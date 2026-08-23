package io.wickkit.leaks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WickKitLeakManagerTest {

    @Before
    fun setUp() {
        WickKitLeakManager.clear()
    }

    // region add / entries

    @Test
    fun `starts empty after clear`() {
        assertTrue(WickKitLeakManager.entries.value.isEmpty())
    }

    @Test
    fun `add stores entry with correct fields`() {
        WickKitLeakManager.add(className = "com.example.MainActivity", detectedAt = "10:00:00.000")
        val entry = WickKitLeakManager.entries.value.single()
        assertEquals("com.example.MainActivity", entry.className)
        assertEquals("10:00:00.000", entry.detectedAt)
        assertEquals(1, entry.instanceCount)
    }

    @Test
    fun `add creates new entry for unknown class`() {
        WickKitLeakManager.add(className = "com.example.ActivityA", detectedAt = "10:00:00.000")
        WickKitLeakManager.add(className = "com.example.ActivityB", detectedAt = "10:00:01.000")
        assertEquals(2, WickKitLeakManager.entries.value.size)
    }

    @Test
    fun `add preserves insertion order for different classes`() {
        WickKitLeakManager.add(className = "com.example.FirstActivity", detectedAt = "10:00:00.000")
        WickKitLeakManager.add(className = "com.example.SecondActivity", detectedAt = "10:00:01.000")
        val entries = WickKitLeakManager.entries.value
        assertEquals("com.example.FirstActivity", entries[0].className)
        assertEquals("com.example.SecondActivity", entries[1].className)
    }

    @Test
    fun `add increments instance count for existing class`() {
        WickKitLeakManager.add(className = "com.example.MainActivity", detectedAt = "10:00:00.000")
        WickKitLeakManager.add(className = "com.example.MainActivity", detectedAt = "10:00:05.000")
        val entries = WickKitLeakManager.entries.value
        assertEquals(1, entries.size)
        assertEquals(2, entries.single().instanceCount)
    }

    @Test
    fun `add updates detectedAt when class already has entry`() {
        WickKitLeakManager.add(className = "com.example.MainActivity", detectedAt = "10:00:00.000")
        WickKitLeakManager.add(className = "com.example.MainActivity", detectedAt = "10:00:05.000")
        assertEquals("10:00:05.000", WickKitLeakManager.entries.value.single().detectedAt)
    }

    @Test
    fun `add sets firstDetectedAt equal to detectedAt on first detection`() {
        WickKitLeakManager.add(className = "com.example.MainActivity", detectedAt = "10:00:00.000")
        val entry = WickKitLeakManager.entries.value.single()
        assertEquals(entry.detectedAt, entry.firstDetectedAt)
    }

    @Test
    fun `add preserves firstDetectedAt when class already has entry`() {
        WickKitLeakManager.add(className = "com.example.MainActivity", detectedAt = "10:00:00.000")
        WickKitLeakManager.add(className = "com.example.MainActivity", detectedAt = "10:00:05.000")
        assertEquals("10:00:00.000", WickKitLeakManager.entries.value.single().firstDetectedAt)
    }

    @Test
    fun `add does not create duplicate entries for the same class`() {
        repeat(5) { WickKitLeakManager.add(className = "com.example.MainActivity", detectedAt = "10:00:00.000") }
        assertEquals(1, WickKitLeakManager.entries.value.size)
        assertEquals(5, WickKitLeakManager.entries.value.single().instanceCount)
    }

    @Test
    fun `add preserves entry id when updating instance count`() {
        WickKitLeakManager.add(className = "com.example.MainActivity", detectedAt = "10:00:00.000")
        val originalId = WickKitLeakManager.entries.value.single().id
        WickKitLeakManager.add(className = "com.example.MainActivity", detectedAt = "10:00:05.000")
        assertEquals(originalId, WickKitLeakManager.entries.value.single().id)
    }

    @Test
    fun `add keeps entry at its original index when updating instance count`() {
        WickKitLeakManager.add(className = "com.example.ActivityA", detectedAt = "10:00:00.000")
        WickKitLeakManager.add(className = "com.example.ActivityB", detectedAt = "10:00:01.000")
        WickKitLeakManager.add(className = "com.example.ActivityA", detectedAt = "10:00:02.000")
        val entries = WickKitLeakManager.entries.value
        assertEquals("com.example.ActivityA", entries[0].className)
        assertEquals("com.example.ActivityB", entries[1].className)
    }

    @Test
    fun `updating existing entry at cap does not evict any entry`() {
        repeat(500) { index ->
            WickKitLeakManager.add(className = "com.example.Activity$index", detectedAt = "10:00:00.000")
        }
        WickKitLeakManager.add(className = "com.example.Activity0", detectedAt = "10:00:01.000")
        val entries = WickKitLeakManager.entries.value
        assertEquals(500, entries.size)
        assertEquals("com.example.Activity0", entries.first().className)
        assertEquals(2, entries.first().instanceCount)
    }

    @Test
    fun `clear removes all entries`() {
        WickKitLeakManager.add(className = "com.example.MainActivity", detectedAt = "10:00:00.000")
        WickKitLeakManager.clear()
        assertTrue(WickKitLeakManager.entries.value.isEmpty())
    }

    @Test
    fun `entries are capped at 500`() {
        repeat(501) { index ->
            WickKitLeakManager.add(className = "com.example.Activity$index", detectedAt = "10:00:00.000")
        }
        assertEquals(500, WickKitLeakManager.entries.value.size)
    }

    @Test
    fun `oldest entry is dropped when cap is exceeded`() {
        repeat(500) { index ->
            WickKitLeakManager.add(className = "com.example.Activity$index", detectedAt = "10:00:00.000")
        }
        WickKitLeakManager.add(className = "com.example.NewestActivity", detectedAt = "10:00:00.000")
        val entries = WickKitLeakManager.entries.value
        assertEquals(500, entries.size)
        assertEquals("com.example.Activity1", entries.first().className)
        assertEquals("com.example.NewestActivity", entries.last().className)
    }

    // endregion
}
