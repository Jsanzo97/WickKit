package io.wickkit.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WickKitComposeTrackerTest {

    @Before
    fun setUp() {
        WickKitComposeTracker.reset()
        // reset() intentionally does not clear pluginActive; reflection ensures each test starts clean
        val field = WickKitComposeTracker::class.java.getDeclaredField("pluginActive")
        field.isAccessible = true
        field.setBoolean(WickKitComposeTracker, false)
    }

    @Test
    fun `initial state returns empty list`() {
        val entries = WickKitComposeTracker.computeEntries(nowMs = 1_000L)
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `below threshold composable is not emitted`() {
        WickKitComposeTracker.onRecompose("SlowComposable")
        WickKitComposeTracker.computeEntries(nowMs = 0L)
        repeat(4) { WickKitComposeTracker.onRecompose("SlowComposable") }
        // 4 recompositions over 2 000 ms = 2.0 /s — below YELLOW threshold of 5
        val entries = WickKitComposeTracker.computeEntries(nowMs = 2_000L)
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `composable above yellow threshold emits with YELLOW severity`() {
        WickKitComposeTracker.computeEntries(nowMs = 0L)
        repeat(12) { WickKitComposeTracker.onRecompose("TitleBar") }
        // 12 recompositions over 1 000 ms = 12.0 /s — YELLOW (>5, ≤25)
        val entries = WickKitComposeTracker.computeEntries(nowMs = 1_000L)
        assertEquals(1, entries.size)
        assertEquals("TitleBar", entries[0].name)
        assertEquals(RecomposeSeverity.YELLOW, entries[0].severity)
        assertEquals(12L, entries[0].totalCount)
    }

    @Test
    fun `composable above orange threshold emits with ORANGE severity`() {
        WickKitComposeTracker.computeEntries(nowMs = 0L)
        repeat(30) { WickKitComposeTracker.onRecompose("ProductCard") }
        // 30 /s — ORANGE (>25, ≤50)
        val entries = WickKitComposeTracker.computeEntries(nowMs = 1_000L)
        assertEquals(1, entries.size)
        assertEquals(RecomposeSeverity.ORANGE, entries[0].severity)
    }

    @Test
    fun `composable above red threshold emits with RED severity`() {
        WickKitComposeTracker.computeEntries(nowMs = 0L)
        repeat(60) { WickKitComposeTracker.onRecompose("CheckoutButton") }
        // 60 /s — RED (>50)
        val entries = WickKitComposeTracker.computeEntries(nowMs = 1_000L)
        assertEquals(1, entries.size)
        assertEquals(RecomposeSeverity.RED, entries[0].severity)
    }

    @Test
    fun `entries are sorted by rate descending`() {
        WickKitComposeTracker.computeEntries(nowMs = 0L)
        repeat(60) { WickKitComposeTracker.onRecompose("A") }
        repeat(30) { WickKitComposeTracker.onRecompose("B") }
        repeat(10) { WickKitComposeTracker.onRecompose("C") }
        val entries = WickKitComposeTracker.computeEntries(nowMs = 1_000L)
        assertEquals(listOf("A", "B", "C"), entries.map { it.name })
    }

    @Test
    fun `peak rate is tracked across polls`() {
        WickKitComposeTracker.computeEntries(nowMs = 0L)
        repeat(60) { WickKitComposeTracker.onRecompose("Spike") }
        WickKitComposeTracker.computeEntries(nowMs = 1_000L) // high rate poll
        // Next poll has lower rate
        repeat(10) { WickKitComposeTracker.onRecompose("Spike") }
        val entries = WickKitComposeTracker.computeEntries(nowMs = 2_000L)
        assertEquals(1, entries.size)
        // peak should still reflect the earlier spike (60 /s)
        assertTrue(
            "Peak ${entries[0].peakRatePerSecond} should be >= current ${entries[0].ratePerSecond}",
            entries[0].peakRatePerSecond >= entries[0].ratePerSecond,
        )
        assertTrue("Peak should be close to 60", entries[0].peakRatePerSecond >= 55f)
    }

    @Test
    fun `reset clears all counts and state`() {
        WickKitComposeTracker.computeEntries(nowMs = 0L)
        repeat(60) { WickKitComposeTracker.onRecompose("Foo") }
        WickKitComposeTracker.computeEntries(nowMs = 1_000L)
        WickKitComposeTracker.reset()
        // After reset, first poll has no elapsed time so rate is 0
        val entries = WickKitComposeTracker.computeEntries(nowMs = 2_000L)
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `multiple composables are tracked independently`() {
        WickKitComposeTracker.computeEntries(nowMs = 0L)
        repeat(60) { WickKitComposeTracker.onRecompose("Alpha") }
        repeat(60) { WickKitComposeTracker.onRecompose("Beta") }
        val entries = WickKitComposeTracker.computeEntries(nowMs = 1_000L)
        val names = entries.map { it.name }.toSet()
        assertTrue(names.contains("Alpha"))
        assertTrue(names.contains("Beta"))
    }

    @Test
    fun `rate is zero on first poll after reset`() {
        WickKitComposeTracker.reset()
        repeat(60) { WickKitComposeTracker.onRecompose("Foo") }
        // First poll after reset: lastPollTimeMs == 0, elapsedMs == 0, rate == 0
        val entries = WickKitComposeTracker.computeEntries(nowMs = 1_000L)
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `total count accumulates across polls`() {
        WickKitComposeTracker.computeEntries(nowMs = 0L)
        repeat(60) { WickKitComposeTracker.onRecompose("Counter") }
        WickKitComposeTracker.computeEntries(nowMs = 1_000L)
        repeat(60) { WickKitComposeTracker.onRecompose("Counter") }
        val entries = WickKitComposeTracker.computeEntries(nowMs = 2_000L)
        assertEquals(1, entries.size)
        assertEquals(120L, entries[0].totalCount)
    }

    @Test
    fun `isPluginActive is false before any recompose call`() {
        assertFalse(WickKitComposeTracker.isPluginActive())
    }

    @Test
    fun `isPluginActive becomes true after first onRecompose`() {
        WickKitComposeTracker.onRecompose("SomeComposable")
        assertTrue(WickKitComposeTracker.isPluginActive())
    }

    @Test
    fun `isPluginActive remains true after reset`() {
        WickKitComposeTracker.onRecompose("SomeComposable")
        WickKitComposeTracker.reset()
        assertTrue(WickKitComposeTracker.isPluginActive())
    }
}
