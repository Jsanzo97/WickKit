package io.wickkit.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WickKitNetworkManagerTest {

    @Before
    fun setUp() {
        WickKitNetworkManager.clear()
    }

    // region add / entries

    @Test
    fun `starts empty after clear`() {
        assertTrue(WickKitNetworkManager.entries.value.isEmpty())
    }

    @Test
    fun `add stores entry with correct fields`() {
        val entry = entry(id = 0L, method = "POST", url = "https://api.example.com/v1/users")
        WickKitNetworkManager.add(entry)
        assertEquals(entry, WickKitNetworkManager.entries.value.single())
    }

    @Test
    fun `add preserves insertion order`() {
        WickKitNetworkManager.add(entry(id = 0L, url = "https://example.com/first"))
        WickKitNetworkManager.add(entry(id = 1L, url = "https://example.com/second"))
        val entries = WickKitNetworkManager.entries.value
        assertEquals("https://example.com/first", entries[0].url)
        assertEquals("https://example.com/second", entries[1].url)
    }

    @Test
    fun `clear removes all entries`() {
        WickKitNetworkManager.add(entry(id = 0L))
        WickKitNetworkManager.clear()
        assertTrue(WickKitNetworkManager.entries.value.isEmpty())
    }

    @Test
    fun `entries are capped at 500`() {
        repeat(501) { i -> WickKitNetworkManager.add(entry(id = i.toLong())) }
        assertEquals(500, WickKitNetworkManager.entries.value.size)
    }

    @Test
    fun `oldest entry is dropped when cap is exceeded`() {
        repeat(500) { i -> WickKitNetworkManager.add(entry(id = i.toLong(), url = "https://example.com/$i")) }
        WickKitNetworkManager.add(entry(id = 500L, url = "https://example.com/newest"))
        val entries = WickKitNetworkManager.entries.value
        assertEquals(500, entries.size)
        assertEquals("https://example.com/1", entries.first().url)
        assertEquals("https://example.com/newest", entries.last().url)
    }

    @Test
    fun `isMocked flag is preserved`() {
        WickKitNetworkManager.add(entry(id = 0L, isMocked = true))
        assertTrue(WickKitNetworkManager.entries.value.single().isMocked)
    }

    @Test
    fun `error field is preserved`() {
        WickKitNetworkManager.add(entry(id = 0L, error = "Connection refused"))
        assertEquals("Connection refused", WickKitNetworkManager.entries.value.single().error)
    }

    @Test
    fun `null statusCode is preserved for error entries`() {
        WickKitNetworkManager.add(entry(id = 0L, statusCode = null, error = "Timeout"))
        assertFalse(WickKitNetworkManager.entries.value.single().statusCode != null)
    }

    // endregion

    @Suppress("LongParameterList")
    private fun entry(
        id: Long = 0L,
        method: String = "GET",
        url: String = "https://example.com",
        statusCode: Int? = 200,
        error: String? = null,
        isMocked: Boolean = false,
    ) = NetworkEntry(
        id = id,
        method = method,
        url = url,
        requestHeaders = emptyMap(),
        requestBody = null,
        statusCode = statusCode,
        responseHeaders = emptyMap(),
        responseBody = null,
        durationMs = 100L,
        time = "10:00:00.000",
        error = error,
        isMocked = isMocked,
    )
}
