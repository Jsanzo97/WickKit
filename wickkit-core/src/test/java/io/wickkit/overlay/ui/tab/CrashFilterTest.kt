package io.wickkit.overlay.ui.tab

import io.wickkit.crashes.CrashEntry
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashFilterTest {

    private val crash = CrashEntry.Crash(
        id = 0L,
        timestamp = 1000L,
        exceptionType = "java.lang.RuntimeException",
        message = "test",
        stackTrace = persistentListOf(),
        threadName = "main",
        appVersion = "1.0",
    )

    private val anr = CrashEntry.Anr(
        id = 1L,
        timestamp = 2000L,
        description = "ANR",
        trace = persistentListOf(),
        processName = "com.example",
    )

    @Test
    fun `ALL matches crash entry`() {
        assertTrue(CrashFilter.ALL.matches(crash))
    }

    @Test
    fun `ALL matches anr entry`() {
        assertTrue(CrashFilter.ALL.matches(anr))
    }

    @Test
    fun `CRASH matches crash entry`() {
        assertTrue(CrashFilter.CRASH.matches(crash))
    }

    @Test
    fun `CRASH does not match anr entry`() {
        assertFalse(CrashFilter.CRASH.matches(anr))
    }

    @Test
    fun `ANR matches anr entry`() {
        assertTrue(CrashFilter.ANR.matches(anr))
    }

    @Test
    fun `ANR does not match crash entry`() {
        assertFalse(CrashFilter.ANR.matches(crash))
    }
}
