package io.wickkit.logs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WickKitLogManagerTest {

    @Before
    fun setUp() {
        WickKitLogManager.clear()
        LogLevel.entries.forEach { level ->
            if (level !in WickKitLogManager.selectedLevels.value) {
                WickKitLogManager.toggleLevel(level)
            }
        }
    }

    // region entries

    @Test
    fun `starts empty after clear`() {
        assertTrue(WickKitLogManager.entries.value.isEmpty())
    }

    @Test
    fun `add stores entry with correct fields`() {
        WickKitLogManager.add(LogLevel.DEBUG, "Tag", "Message", "10:00:00.000")
        val entry = WickKitLogManager.entries.value.single()
        assertEquals(LogLevel.DEBUG, entry.level)
        assertEquals("Tag", entry.tag)
        assertEquals("Message", entry.message)
        assertEquals("10:00:00.000", entry.time)
    }

    @Test
    fun `add assigns unique ids`() {
        WickKitLogManager.add(LogLevel.DEBUG, "Tag", "first", "00:00:00.000")
        WickKitLogManager.add(LogLevel.DEBUG, "Tag", "second", "00:00:00.001")
        val entries = WickKitLogManager.entries.value
        assertNotEquals(entries[0].id, entries[1].id)
    }

    @Test
    fun `add preserves insertion order`() {
        WickKitLogManager.add(LogLevel.DEBUG, "Tag", "first", "00:00:00.000")
        WickKitLogManager.add(LogLevel.INFO, "Tag", "second", "00:00:00.001")
        val entries = WickKitLogManager.entries.value
        assertEquals("first", entries[0].message)
        assertEquals("second", entries[1].message)
    }

    @Test
    fun `clear removes all entries`() {
        WickKitLogManager.add(LogLevel.INFO, "Tag", "Msg", "00:00:00.000")
        WickKitLogManager.clear()
        assertTrue(WickKitLogManager.entries.value.isEmpty())
    }

    @Test
    fun `entries are capped at 500`() {
        repeat(501) { i -> WickKitLogManager.add(LogLevel.DEBUG, "Tag", "msg$i", "00:00:00.000") }
        assertEquals(500, WickKitLogManager.entries.value.size)
    }

    @Test
    fun `oldest entry is dropped when cap is exceeded`() {
        repeat(500) { i -> WickKitLogManager.add(LogLevel.DEBUG, "Tag", "msg$i", "00:00:00.000") }
        WickKitLogManager.add(LogLevel.DEBUG, "Tag", "newest", "00:00:00.000")
        val entries = WickKitLogManager.entries.value
        assertEquals(500, entries.size)
        assertEquals("msg1", entries.first().message)
        assertEquals("newest", entries.last().message)
    }

    // endregion

    // region selectedLevels

    @Test
    fun `all levels are enabled by default`() {
        val levels = WickKitLogManager.selectedLevels.value
        LogLevel.entries.forEach { assertTrue("$it should be enabled", it in levels) }
    }

    @Test
    fun `toggleLevel disables an enabled level`() {
        WickKitLogManager.toggleLevel(LogLevel.DEBUG)
        assertFalse(LogLevel.DEBUG in WickKitLogManager.selectedLevels.value)
    }

    @Test
    fun `toggleLevel re-enables a disabled level`() {
        WickKitLogManager.toggleLevel(LogLevel.DEBUG)
        WickKitLogManager.toggleLevel(LogLevel.DEBUG)
        assertTrue(LogLevel.DEBUG in WickKitLogManager.selectedLevels.value)
    }

    @Test
    fun `toggling one level does not affect others`() {
        WickKitLogManager.toggleLevel(LogLevel.DEBUG)
        val levels = WickKitLogManager.selectedLevels.value
        LogLevel.entries.filter { it != LogLevel.DEBUG }.forEach {
            assertTrue("$it should remain enabled", it in levels)
        }
    }

    // endregion
}
