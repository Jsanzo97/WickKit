package io.wickkit.crashes

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

internal object WickKitCrashManager {

    private const val CRASH_FILE_NAME = "wickkit_last_crash.json"
    private const val MAX_STACK_TRACE_LINES = 200
    private const val MAX_ANR_ENTRIES = 5
    private const val MAX_TRACE_LINES = 500

    val entries: StateFlow<ImmutableList<CrashEntry>>
        field = MutableStateFlow<PersistentList<CrashEntry>>(persistentListOf())

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context) {
        val applicationContext = context.applicationContext
        installCrashHandler(applicationContext)
        scope.launch {
            entries.value = buildEntries(applicationContext)
        }
    }

    private fun installCrashHandler(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            saveCrash(context, thread, throwable)
            previous?.uncaughtException(thread, throwable)
        }
    }

    internal fun saveCrash(
        context: Context,
        thread: Thread,
        throwable: Throwable,
    ) {
        runCatching {
            val appVersion = runCatching {
                @Suppress("DEPRECATION")
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                packageInfo.versionName.orEmpty()
            }.getOrDefault("")
            val stackTraceArray = JSONArray()
            throwable.stackTrace.take(MAX_STACK_TRACE_LINES).forEach { element ->
                stackTraceArray.put(element.toString())
            }
            val json = JSONObject().apply {
                put("exceptionType", throwable.javaClass.name)
                put("message", throwable.message.orEmpty())
                put("threadName", thread.name)
                put("appVersion", appVersion)
                put("timestamp", System.currentTimeMillis())
                put("stackTrace", stackTraceArray)
            }
            File(context.filesDir, CRASH_FILE_NAME).writeText(json.toString())
        }
    }

    internal fun buildEntries(context: Context): PersistentList<CrashEntry> {
        val crashes = loadPersistedCrash(context)?.let { listOf(it) } ?: emptyList()
        val anrs = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> loadAnrEntries(context)
            else -> emptyList()
        }
        return sortAndAssignIds(crashes = crashes, anrs = anrs)
    }

    internal fun sortAndAssignIds(
        crashes: List<CrashEntry.Crash>,
        anrs: List<CrashEntry.Anr>,
    ): PersistentList<CrashEntry> = (crashes + anrs)
        .sortedByDescending { it.timestamp }
        .mapIndexed { index, entry ->
            when (entry) {
                is CrashEntry.Crash -> entry.copy(id = index.toLong())
                is CrashEntry.Anr -> entry.copy(id = index.toLong())
            }
        }
        .toPersistentList()

    internal fun loadPersistedCrash(context: Context): CrashEntry.Crash? = runCatching {
        val file = File(context.filesDir, CRASH_FILE_NAME)
        if (!file.exists()) {
            return null
        }
        val json = JSONObject(file.readText())
        val stackTraceArray = json.getJSONArray("stackTrace")
        val stackTrace = (0 until stackTraceArray.length())
            .map { stackTraceArray.getString(it) }
            .toPersistentList()
        CrashEntry.Crash(
            id = 0L,
            timestamp = json.getLong("timestamp"),
            exceptionType = json.getString("exceptionType"),
            message = json.getString("message"),
            stackTrace = stackTrace,
            threadName = json.getString("threadName"),
            appVersion = json.getString("appVersion"),
        )
    }.getOrNull()

    @RequiresApi(Build.VERSION_CODES.R)
    private fun loadAnrEntries(context: Context): List<CrashEntry.Anr> = runCatching {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return@runCatching emptyList()
        activityManager
            .getHistoricalProcessExitReasons(context.packageName, 0, MAX_ANR_ENTRIES)
            .filter { it.reason == ApplicationExitInfo.REASON_ANR }
            .map { exitInfo ->
                CrashEntry.Anr(
                    id = 0L,
                    timestamp = exitInfo.timestamp,
                    description = exitInfo.description.orEmpty(),
                    trace = readTrace(exitInfo),
                    processName = exitInfo.processName.orEmpty(),
                )
            }
    }.getOrDefault(emptyList())

    @RequiresApi(Build.VERSION_CODES.R)
    private fun readTrace(exitInfo: ApplicationExitInfo): PersistentList<String> = runCatching {
        exitInfo.traceInputStream?.bufferedReader()?.useLines { lines ->
            lines.take(MAX_TRACE_LINES).toList().toPersistentList()
        } ?: persistentListOf()
    }.getOrDefault(persistentListOf())

    fun clear() {
        entries.value = persistentListOf()
    }
}
