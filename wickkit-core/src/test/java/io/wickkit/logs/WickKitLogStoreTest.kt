package io.wickkit.logs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WickKitLogStoreTest {

    @Before
    fun setUp() {
        WickKitLogStore.clear()
        LogLevel.entries.forEach { level ->
            if (level !in WickKitLogStore.selectedLevels.value) {
                WickKitLogStore.toggleLevel(level)
            }
        }
    }

    // region entries

    @Test
    fun `starts empty after clear`() {
        assertTrue(WickKitLogStore.entries.value.isEmpty())
    }

    @Test
    fun `add stores entry with correct fields`() {
        WickKitLogStore.add(LogLevel.DEBUG, "Tag", "Message", "10:00:00.000")
        val entry = WickKitLogStore.entries.value.single()
        assertEquals(LogLevel.DEBUG, entry.level)
        assertEquals("Tag", entry.tag)
        assertEquals("Message", entry.message)
        assertEquals("10:00:00.000", entry.time)
    }

    @Test
    fun `add assigns unique ids`() {
        WickKitLogStore.add(LogLevel.DEBUG, "Tag", "first", "00:00:00.000")
        WickKitLogStore.add(LogLevel.DEBUG, "Tag", "second", "00:00:00.001")
        val entries = WickKitLogStore.entries.value
        assertNotEquals(entries[0].id, entries[1].id)
    }

    @Test
    fun `add preserves insertion order`() {
        WickKitLogStore.add(LogLevel.DEBUG, "Tag", "first", "00:00:00.000")
        WickKitLogStore.add(LogLevel.INFO, "Tag", "second", "00:00:00.001")
        val entries = WickKitLogStore.entries.value
        assertEquals("first", entries[0].message)
        assertEquals("second", entries[1].message)
    }

    @Test
    fun `clear removes all entries`() {
        WickKitLogStore.add(LogLevel.INFO, "Tag", "Msg", "00:00:00.000")
        WickKitLogStore.clear()
        assertTrue(WickKitLogStore.entries.value.isEmpty())
    }

    @Test
    fun `entries are capped at 500`() {
        repeat(501) { i -> WickKitLogStore.add(LogLevel.DEBUG, "Tag", "msg$i", "00:00:00.000") }
        assertEquals(500, WickKitLogStore.entries.value.size)
    }

    @Test
    fun `oldest entry is dropped when cap is exceeded`() {
        repeat(500) { i -> WickKitLogStore.add(LogLevel.DEBUG, "Tag", "msg$i", "00:00:00.000") }
        WickKitLogStore.add(LogLevel.DEBUG, "Tag", "newest", "00:00:00.000")
        val entries = WickKitLogStore.entries.value
        assertEquals(500, entries.size)
        assertEquals("msg1", entries.first().message)
        assertEquals("newest", entries.last().message)
    }

    // endregion

    // region selectedLevels

    @Test
    fun `all levels are enabled by default`() {
        val levels = WickKitLogStore.selectedLevels.value
        LogLevel.entries.forEach { assertTrue("$it should be enabled", it in levels) }
    }

    @Test
    fun `toggleLevel disables an enabled level`() {
        WickKitLogStore.toggleLevel(LogLevel.DEBUG)
        assertFalse(LogLevel.DEBUG in WickKitLogStore.selectedLevels.value)
    }

    @Test
    fun `toggleLevel re-enables a disabled level`() {
        WickKitLogStore.toggleLevel(LogLevel.DEBUG)
        WickKitLogStore.toggleLevel(LogLevel.DEBUG)
        assertTrue(LogLevel.DEBUG in WickKitLogStore.selectedLevels.value)
    }

    @Test
    fun `toggling one level does not affect others`() {
        WickKitLogStore.toggleLevel(LogLevel.DEBUG)
        val levels = WickKitLogStore.selectedLevels.value
        LogLevel.entries.filter { it != LogLevel.DEBUG }.forEach {
            assertTrue("$it should remain enabled", it in levels)
        }
    }

    // endregion
}
