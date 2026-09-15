package io.wickkit.crashes

import android.content.Context
import kotlinx.collections.immutable.persistentListOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WickKitCrashManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        crashFile().delete()
        WickKitCrashManager.clear()
    }

    @After
    fun tearDown() {
        crashFile().delete()
        WickKitCrashManager.clear()
    }

    @Test
    fun `loadPersistedCrashes returns empty list when no file exists`() {
        assertTrue(WickKitCrashManager.loadPersistedCrashes(context).isEmpty())
    }

    @Test
    fun `loadPersistedCrashes returns empty list for malformed JSON`() {
        crashFile().writeText("not valid json {{")
        assertTrue(WickKitCrashManager.loadPersistedCrashes(context).isEmpty())
    }

    @Test
    fun `saveCrash writes crash to file`() {
        WickKitCrashManager.saveCrash(
            context = context,
            thread = Thread.currentThread(),
            throwable = RuntimeException("test message"),
        )
        val crashes = WickKitCrashManager.loadPersistedCrashes(context)
        assertEquals(1, crashes.size)
    }

    @Test
    fun `saveCrash stores correct exception type`() {
        WickKitCrashManager.saveCrash(
            context = context,
            thread = Thread.currentThread(),
            throwable = NullPointerException("npe"),
        )
        val crash = WickKitCrashManager.loadPersistedCrashes(context).first()
        assertEquals("java.lang.NullPointerException", crash.exceptionType)
    }

    @Test
    fun `saveCrash stores correct message`() {
        WickKitCrashManager.saveCrash(
            context = context,
            thread = Thread.currentThread(),
            throwable = RuntimeException("expected message"),
        )
        val crash = WickKitCrashManager.loadPersistedCrashes(context).first()
        assertEquals("expected message", crash.message)
    }

    @Test
    fun `saveCrash stores correct thread name`() {
        val thread = Thread.currentThread()
        WickKitCrashManager.saveCrash(
            context = context,
            thread = thread,
            throwable = RuntimeException("crash"),
        )
        val crash = WickKitCrashManager.loadPersistedCrashes(context).first()
        assertEquals(thread.name, crash.threadName)
    }

    @Test
    fun `saveCrash stores stack trace`() {
        WickKitCrashManager.saveCrash(
            context = context,
            thread = Thread.currentThread(),
            throwable = RuntimeException("with stack"),
        )
        val crash = WickKitCrashManager.loadPersistedCrashes(context).first()
        assertTrue(crash.stackTrace.isNotEmpty())
    }

    @Test
    fun `saveCrash handles throwable with null message`() {
        WickKitCrashManager.saveCrash(
            context = context,
            thread = Thread.currentThread(),
            throwable = NullPointerException(),
        )
        val crashes = WickKitCrashManager.loadPersistedCrashes(context)
        assertEquals(1, crashes.size)
        assertEquals("", crashes.first().message)
    }

    @Test
    fun `saveCrash keeps at most 3 crashes`() {
        repeat(5) { i ->
            WickKitCrashManager.saveCrash(
                context = context,
                thread = Thread.currentThread(),
                throwable = RuntimeException("crash $i"),
            )
        }
        val crashes = WickKitCrashManager.loadPersistedCrashes(context)
        assertEquals(3, crashes.size)
        assertEquals("crash 4", crashes[0].message)
        assertEquals("crash 3", crashes[1].message)
        assertEquals("crash 2", crashes[2].message)
    }

    @Test
    fun `loadPersistedCrashes migrates single-object format`() {
        val json = """
            |{"exceptionType":"java.lang.RuntimeException",
            |"message":"legacy","threadName":"main",
            |"appVersion":"1.0","timestamp":1000,"stackTrace":[]}
        """.trimMargin().replace("\n", "")
        crashFile().writeText(json)
        val crashes = WickKitCrashManager.loadPersistedCrashes(context)
        assertEquals(1, crashes.size)
        assertEquals("legacy", crashes.first().message)
    }

    @Test
    fun `buildEntries returns empty list when no crash file`() {
        assertTrue(WickKitCrashManager.buildEntries(context).isEmpty())
    }

    @Test
    fun `buildEntries returns crash entry when file exists`() {
        WickKitCrashManager.saveCrash(
            context = context,
            thread = Thread.currentThread(),
            throwable = RuntimeException("crash"),
        )
        val entries = WickKitCrashManager.buildEntries(context)
        assertEquals(1, entries.size)
        assertTrue(entries[0] is CrashEntry.Crash)
    }

    @Test
    fun `sortAndAssignIds returns empty list for empty input`() {
        val result = WickKitCrashManager.sortAndAssignIds(
            crashes = emptyList(),
            anrs = emptyList(),
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `sortAndAssignIds assigns sequential ids starting from zero`() {
        val crash = CrashEntry.Crash(
            id = 99L,
            timestamp = 1000L,
            exceptionType = "Exception",
            message = "",
            stackTrace = persistentListOf(),
            threadName = "main",
            appVersion = "1.0",
        )
        val anr = CrashEntry.Anr(
            id = 99L,
            timestamp = 2000L,
            description = "ANR",
            trace = persistentListOf(),
            processName = "com.example",
        )
        val result = WickKitCrashManager.sortAndAssignIds(
            crashes = listOf(crash),
            anrs = listOf(anr),
        )
        assertEquals(0L, result[0].id)
        assertEquals(1L, result[1].id)
    }

    @Test
    fun `sortAndAssignIds orders entries by timestamp descending`() {
        val older = CrashEntry.Crash(
            id = 0L,
            timestamp = 1000L,
            exceptionType = "Exception",
            message = "",
            stackTrace = persistentListOf(),
            threadName = "main",
            appVersion = "",
        )
        val newer = CrashEntry.Anr(
            id = 0L,
            timestamp = 2000L,
            description = "",
            trace = persistentListOf(),
            processName = "",
        )
        val result = WickKitCrashManager.sortAndAssignIds(
            crashes = listOf(older),
            anrs = listOf(newer),
        )
        assertEquals(2000L, result[0].timestamp)
        assertEquals(1000L, result[1].timestamp)
    }

    @Test
    fun `sortAndAssignIds preserves crash type after sort`() {
        val crash = CrashEntry.Crash(
            id = 0L,
            timestamp = 1000L,
            exceptionType = "Exception",
            message = "",
            stackTrace = persistentListOf(),
            threadName = "main",
            appVersion = "",
        )
        val result = WickKitCrashManager.sortAndAssignIds(
            crashes = listOf(crash),
            anrs = emptyList(),
        )
        assertTrue(result[0] is CrashEntry.Crash)
    }

    @Test
    fun `clear resets entries to empty`() {
        WickKitCrashManager.clear()
        assertTrue(WickKitCrashManager.entries.value.isEmpty())
    }

    @Test
    fun `clear deletes crash file from disk when context is initialized`() {
        WickKitCrashManager.init(context)
        WickKitCrashManager.saveCrash(
            context = context,
            thread = Thread.currentThread(),
            throwable = RuntimeException("crash"),
        )
        val file = crashFile()
        assertTrue(file.exists())

        WickKitCrashManager.clear()

        assertFalse(file.exists())
    }

    private fun crashFile(): File = File(context.filesDir, "wickkit_crashes.json")
}
