package io.wickkit.logs

import android.os.Process
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal object WickKitLogcat {

    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var readJob: Job? = null

    @Volatile private var activeProcess: java.lang.Process? = null

    // threadtime format: MM-DD HH:MM:SS.mmm  PID  TID LEVEL TAG: message
    private val lineRegex = Regex(
        """^\d{2}-\d{2} (\d{2}:\d{2}:\d{2}\.\d{3})\s+\d+\s+\d+\s+([VDIWEF])\s+(.*?):\s*(.*)$""",
    )

    private val levelMap = mapOf(
        'V' to LogLevel.VERBOSE,
        'D' to LogLevel.DEBUG,
        'I' to LogLevel.INFO,
        'W' to LogLevel.WARN,
        'E' to LogLevel.ERROR,
        'F' to LogLevel.ERROR,
    )

    fun start() {
        val pid = Process.myPid()
        readJob = scope.launch {
            var backoffMs = 1_000L
            while (true) {
                runCatching { readProcess(pid) }
                    .onSuccess { backoffMs = 1_000L }
                    .onFailure { Log.e("WickKit", "Logcat reader crashed", it) }
                delay(backoffMs.milliseconds)
                backoffMs = (backoffMs * 2).coerceAtMost(30_000L)
            }
        }
    }

    fun stop() {
        activeProcess?.destroy()
        activeProcess = null
        readJob?.cancel()
        readJob = null
        scope.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    private fun readProcess(pid: Int) {
        val process = ProcessBuilder("logcat", "--pid=$pid", "-v", "threadtime")
            .redirectErrorStream(true)
            .start()
        activeProcess = process
        try {
            process.inputStream.bufferedReader().lineSequence().forEach { line ->
                parseLine(line)?.let { (level, tag, message, time) ->
                    WickKitLogManager.add(
                        level = level,
                        tag = tag,
                        message = message,
                        time = time,
                    )
                }
            }
        } finally {
            process.destroy()
            activeProcess = null
        }
    }

    private fun parseLine(line: String): ParsedLine? {
        val match = lineRegex.find(line) ?: return null
        val time = match.groupValues[1]
        val level = levelMap[match.groupValues[2].firstOrNull()] ?: return null
        val tag = match.groupValues[3].trim()
        val message = match.groupValues[4]
        return ParsedLine(
            level = level,
            tag = tag,
            message = message,
            time = time,
        )
    }

    private data class ParsedLine(
        val level: LogLevel,
        val tag: String,
        val message: String,
        val time: String,
    )
}
