package io.wickkit.logs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogFilterTest {

    // region parseLogFilter

    @Test
    fun `empty string returns None`() {
        assertEquals(LogFilter.None, parseLogFilter(""))
    }

    @Test
    fun `blank string returns None`() {
        assertEquals(LogFilter.None, parseLogFilter("   "))
    }

    @Test
    fun `plain text returns ByText`() {
        assertEquals(LogFilter.ByText("retrofit"), parseLogFilter("retrofit"))
    }

    @Test
    fun `tag prefix returns ByTag`() {
        assertEquals(LogFilter.ByTag("OkHttp"), parseLogFilter("tag:OkHttp"))
    }

    @Test
    fun `tag prefix is case insensitive`() {
        assertEquals(LogFilter.ByTag("OkHttp"), parseLogFilter("TAG:OkHttp"))
    }

    @Test
    fun `tag prefix trims spaces after colon`() {
        assertEquals(LogFilter.ByTag("OkHttp"), parseLogFilter("tag: OkHttp"))
    }

    @Test
    fun `tag prefix with no value returns ByTag with empty string`() {
        assertEquals(LogFilter.ByTag(""), parseLogFilter("tag:"))
    }

    // endregion

    // region matches

    @Test
    fun `None matches any entry`() {
        assertTrue(entry(tag = "Tag", message = "Message").matches(LogFilter.None))
    }

    @Test
    fun `ByText matches message case-insensitively`() {
        val logEntry = entry(tag = "Tag", message = "Payment started")
        assertTrue(logEntry.matches(LogFilter.ByText("payment")))
        assertTrue(logEntry.matches(LogFilter.ByText("PAYMENT")))
    }

    @Test
    fun `ByText matches tag case-insensitively`() {
        val logEntry = entry(tag = "OkHttp", message = "message")
        assertTrue(logEntry.matches(LogFilter.ByText("okhttp")))
    }

    @Test
    fun `ByText returns false when neither tag nor message match`() {
        assertFalse(entry(tag = "Tag", message = "Message").matches(LogFilter.ByText("nomatch")))
    }

    @Test
    fun `ByTag matches tag case-insensitively`() {
        assertTrue(entry(tag = "OkHttp", message = "message").matches(LogFilter.ByTag("okhttp")))
    }

    @Test
    fun `ByTag does not match on message`() {
        assertFalse(entry(tag = "Tag", message = "OkHttp").matches(LogFilter.ByTag("okhttp")))
    }

    @Test
    fun `ByTag returns false when tag does not match`() {
        assertFalse(entry(tag = "Retrofit", message = "message").matches(LogFilter.ByTag("okhttp")))
    }

    // endregion

    @Test
    fun `LogLevel entries expose non-empty label and chipLabel`() {
        for (level in LogLevel.entries) {
            assertTrue("${level.name}.label was empty", level.label.isNotEmpty())
            assertTrue("${level.name}.chipLabel was empty", level.chipLabel.isNotEmpty())
        }
    }

    private fun entry(tag: String, message: String) = LogEntry(
        id = 0L,
        level = LogLevel.DEBUG,
        tag = tag,
        message = message,
        time = "00:00:00.000",
    )
}
