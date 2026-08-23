package io.wickkit.performance

import io.wickkit.compose.ComposableEntry
import io.wickkit.compose.RecomposeSeverity
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WickKitPerformanceManagerTest {

    @Before
    fun setUp() {
        WickKitPerformanceManager.reset()
    }

    // region initial state

    @Test
    fun `snapshot starts empty after reset`() {
        val snapshot = WickKitPerformanceManager.snapshot.value
        assertEquals("", snapshot.activityName)
        assertNull(snapshot.fps)
        assertEquals(0, snapshot.totalFrames)
        assertEquals(0L, snapshot.recompositionCount)
        assertEquals(0, snapshot.threadCount)
        assertEquals(0L, snapshot.jvmUsedMb)
    }

    // endregion

    // region updateFrameStats

    @Test
    fun `updateFrameStats with stats stores fps`() {
        WickKitPerformanceManager.updateFrameStats(sampleFrameStats())
        assertEquals(60f, WickKitPerformanceManager.snapshot.value.fps)
    }

    @Test
    fun `updateFrameStats with stats stores slow and frozen frame counts`() {
        WickKitPerformanceManager.updateFrameStats(sampleFrameStats())
        val snapshot = WickKitPerformanceManager.snapshot.value
        assertEquals(5, snapshot.slowFrames)
        assertEquals(1, snapshot.frozenFrames)
    }

    @Test
    fun `updateFrameStats with stats stores jank rate`() {
        WickKitPerformanceManager.updateFrameStats(sampleFrameStats())
        assertEquals(5f, WickKitPerformanceManager.snapshot.value.jankRate)
    }

    @Test
    fun `updateFrameStats with stats stores percentiles`() {
        WickKitPerformanceManager.updateFrameStats(sampleFrameStats())
        val snapshot = WickKitPerformanceManager.snapshot.value
        assertEquals(8f, snapshot.p50Ms)
        assertEquals(15f, snapshot.p90Ms)
    }

    @Test
    fun `updateFrameStats with stats stores total frames`() {
        WickKitPerformanceManager.updateFrameStats(sampleFrameStats())
        assertEquals(100, WickKitPerformanceManager.snapshot.value.totalFrames)
    }

    @Test
    fun `updateFrameStats with null clears frame fields`() {
        WickKitPerformanceManager.updateFrameStats(sampleFrameStats())
        WickKitPerformanceManager.updateFrameStats(null)
        val snapshot = WickKitPerformanceManager.snapshot.value
        assertNull(snapshot.fps)
        assertNull(snapshot.jankRate)
        assertNull(snapshot.p50Ms)
        assertNull(snapshot.p90Ms)
        assertEquals(0, snapshot.slowFrames)
        assertEquals(0, snapshot.frozenFrames)
        assertEquals(0, snapshot.totalFrames)
    }

    @Test
    fun `updateFrameStats with null preserves runtime fields`() {
        WickKitPerformanceManager.updateRuntimeStats(
            RuntimeStats(
                recompositions = 50L,
                threads = 10,
                jvmUsedMb = 32L,
                jvmMaxMb = 256L,
                nativeHeapMb = 12L,
            ),
        )
        WickKitPerformanceManager.updateFrameStats(null)
        val snapshot = WickKitPerformanceManager.snapshot.value
        assertEquals(50L, snapshot.recompositionCount)
        assertEquals(10, snapshot.threadCount)
    }

    // endregion

    // region updateRuntimeStats

    @Test
    fun `updateRuntimeStats stores recomposition count`() {
        WickKitPerformanceManager.updateRuntimeStats(
            RuntimeStats(
                recompositions = 200L,
                threads = 5,
                jvmUsedMb = 0L,
                jvmMaxMb = 0L,
                nativeHeapMb = 0L,
            ),
        )
        assertEquals(200L, WickKitPerformanceManager.snapshot.value.recompositionCount)
    }

    @Test
    fun `updateRuntimeStats stores thread count`() {
        WickKitPerformanceManager.updateRuntimeStats(
            RuntimeStats(
                recompositions = 0L,
                threads = 18,
                jvmUsedMb = 0L,
                jvmMaxMb = 0L,
                nativeHeapMb = 0L,
            ),
        )
        assertEquals(18, WickKitPerformanceManager.snapshot.value.threadCount)
    }

    @Test
    fun `updateRuntimeStats stores jvm memory`() {
        WickKitPerformanceManager.updateRuntimeStats(
            RuntimeStats(
                recompositions = 0L,
                threads = 0,
                jvmUsedMb = 42L,
                jvmMaxMb = 256L,
                nativeHeapMb = 0L,
            ),
        )
        val snapshot = WickKitPerformanceManager.snapshot.value
        assertEquals(42L, snapshot.jvmUsedMb)
        assertEquals(256L, snapshot.jvmMaxMb)
    }

    @Test
    fun `updateRuntimeStats stores native heap`() {
        WickKitPerformanceManager.updateRuntimeStats(
            RuntimeStats(
                recompositions = 0L,
                threads = 0,
                jvmUsedMb = 0L,
                jvmMaxMb = 0L,
                nativeHeapMb = 18L,
            ),
        )
        assertEquals(18L, WickKitPerformanceManager.snapshot.value.nativeHeapMb)
    }

    @Test
    fun `recompositionsPerSecond is zero on first call`() {
        WickKitPerformanceManager.updateRuntimeStats(
            RuntimeStats(
                recompositions = 100L,
                threads = 0,
                jvmUsedMb = 0L,
                jvmMaxMb = 0L,
                nativeHeapMb = 0L,
            ),
        )
        assertEquals(0f, WickKitPerformanceManager.snapshot.value.recompositionsPerSecond, 0.01f)
    }

    @Test
    fun `recompositionsPerSecond is non-negative on subsequent calls`() {
        WickKitPerformanceManager.updateRuntimeStats(
            RuntimeStats(
                recompositions = 100L,
                threads = 0,
                jvmUsedMb = 0L,
                jvmMaxMb = 0L,
                nativeHeapMb = 0L,
            ),
        )
        WickKitPerformanceManager.updateRuntimeStats(
            RuntimeStats(
                recompositions = 150L,
                threads = 0,
                jvmUsedMb = 0L,
                jvmMaxMb = 0L,
                nativeHeapMb = 0L,
            ),
        )
        val rate = WickKitPerformanceManager.snapshot.value.recompositionsPerSecond
        assert(rate >= 0f) { "Expected non-negative rate, got $rate" }
    }

    @Test
    fun `updateRuntimeStats preserves frame stats`() {
        WickKitPerformanceManager.updateFrameStats(sampleFrameStats())
        WickKitPerformanceManager.updateRuntimeStats(
            RuntimeStats(
                recompositions = 10L,
                threads = 5,
                jvmUsedMb = 20L,
                jvmMaxMb = 128L,
                nativeHeapMb = 8L,
            ),
        )
        val snapshot = WickKitPerformanceManager.snapshot.value
        assertEquals(60f, snapshot.fps)
        assertEquals(100, snapshot.totalFrames)
    }

    // endregion

    // region reset

    @Test
    fun `reset clears frame stats`() {
        WickKitPerformanceManager.updateFrameStats(sampleFrameStats())
        WickKitPerformanceManager.reset()
        val snapshot = WickKitPerformanceManager.snapshot.value
        assertNull(snapshot.fps)
        assertEquals(0, snapshot.totalFrames)
    }

    @Test
    fun `reset clears runtime stats`() {
        WickKitPerformanceManager.updateRuntimeStats(
            RuntimeStats(
                recompositions = 99L,
                threads = 12,
                jvmUsedMb = 50L,
                jvmMaxMb = 200L,
                nativeHeapMb = 10L,
            ),
        )
        WickKitPerformanceManager.reset()
        val snapshot = WickKitPerformanceManager.snapshot.value
        assertEquals(0L, snapshot.recompositionCount)
        assertEquals(0, snapshot.threadCount)
        assertEquals(0L, snapshot.jvmUsedMb)
    }

    @Test
    fun `reset clears activity name`() {
        WickKitPerformanceManager.updateFrameStats(sampleFrameStats())
        WickKitPerformanceManager.reset()
        assertEquals("", WickKitPerformanceManager.snapshot.value.activityName)
    }

    // endregion

    // region composableEntries

    @Test
    fun `updateRuntimeStats stores composable entries in snapshot`() {
        val entries = persistentListOf(sampleComposableEntry())
        WickKitPerformanceManager.updateRuntimeStats(
            RuntimeStats(
                recompositions = 0L,
                threads = 0,
                jvmUsedMb = 0L,
                jvmMaxMb = 0L,
                nativeHeapMb = 0L,
                composableEntries = entries,
            ),
        )
        assertEquals(1, WickKitPerformanceManager.snapshot.value.composableEntries.size)
        assertEquals("CheckoutButton", WickKitPerformanceManager.snapshot.value.composableEntries[0].name)
    }

    @Test
    fun `composableEntries default to empty list`() {
        WickKitPerformanceManager.updateRuntimeStats(
            RuntimeStats(
                recompositions = 0L,
                threads = 0,
                jvmUsedMb = 0L,
                jvmMaxMb = 0L,
                nativeHeapMb = 0L,
            ),
        )
        assertTrue(WickKitPerformanceManager.snapshot.value.composableEntries.isEmpty())
    }

    @Test
    fun `reset clears composable entries`() {
        WickKitPerformanceManager.updateRuntimeStats(
            RuntimeStats(
                recompositions = 0L,
                threads = 0,
                jvmUsedMb = 0L,
                jvmMaxMb = 0L,
                nativeHeapMb = 0L,
                composableEntries = persistentListOf(sampleComposableEntry()),
            ),
        )
        WickKitPerformanceManager.reset()
        assertTrue(WickKitPerformanceManager.snapshot.value.composableEntries.isEmpty())
    }

    // endregion

    private fun sampleComposableEntry() = ComposableEntry(
        name = "CheckoutButton",
        totalCount = 1_204L,
        ratePerSecond = 62.0f,
        peakRatePerSecond = 87.3f,
        severity = RecomposeSeverity.RED,
    )

    private fun sampleFrameStats() = FrameStats(
        fps = 60f,
        slowFrames = 5,
        frozenFrames = 1,
        jankRate = 5f,
        p50Ms = 8f,
        p90Ms = 15f,
        totalFrames = 100,
    )
}
