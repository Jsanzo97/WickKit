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
        val e = entry(tag = "Tag", message = "Payment started")
        assertTrue(e.matches(LogFilter.ByText("payment")))
        assertTrue(e.matches(LogFilter.ByText("PAYMENT")))
    }

    @Test
    fun `ByText matches tag case-insensitively`() {
        val e = entry(tag = "OkHttp", message = "message")
        assertTrue(e.matches(LogFilter.ByText("okhttp")))
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

    private fun entry(tag: String, message: String) = LogEntry(0L, LogLevel.DEBUG, tag, message, "00:00:00.000")
}
