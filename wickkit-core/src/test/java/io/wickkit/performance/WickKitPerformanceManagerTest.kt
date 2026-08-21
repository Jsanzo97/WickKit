package io.wickkit.performance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        val s = WickKitPerformanceManager.snapshot.value
        assertEquals("", s.activityName)
        assertNull(s.fps)
        assertEquals(0, s.totalFrames)
        assertEquals(0L, s.recompositionCount)
        assertEquals(0, s.threadCount)
        assertEquals(0L, s.jvmUsedMb)
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
        val s = WickKitPerformanceManager.snapshot.value
        assertEquals(5, s.slowFrames)
        assertEquals(1, s.frozenFrames)
    }

    @Test
    fun `updateFrameStats with stats stores jank rate`() {
        WickKitPerformanceManager.updateFrameStats(sampleFrameStats())
        assertEquals(5f, WickKitPerformanceManager.snapshot.value.jankRate)
    }

    @Test
    fun `updateFrameStats with stats stores percentiles`() {
        WickKitPerformanceManager.updateFrameStats(sampleFrameStats())
        val s = WickKitPerformanceManager.snapshot.value
        assertEquals(8f, s.p50Ms)
        assertEquals(15f, s.p90Ms)
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
        val s = WickKitPerformanceManager.snapshot.value
        assertNull(s.fps)
        assertNull(s.jankRate)
        assertNull(s.p50Ms)
        assertNull(s.p90Ms)
        assertEquals(0, s.slowFrames)
        assertEquals(0, s.frozenFrames)
        assertEquals(0, s.totalFrames)
    }

    @Test
    fun `updateFrameStats with null preserves runtime fields`() {
        WickKitPerformanceManager.updateRuntimeStats(
            recompositions = 50L,
            threads = 10,
            jvmUsedMb = 32L,
            jvmMaxMb = 256L,
            nativeHeapMb = 12L,
        )
        WickKitPerformanceManager.updateFrameStats(null)
        val s = WickKitPerformanceManager.snapshot.value
        assertEquals(50L, s.recompositionCount)
        assertEquals(10, s.threadCount)
    }

    // endregion

    // region updateRuntimeStats

    @Test
    fun `updateRuntimeStats stores recomposition count`() {
        WickKitPerformanceManager.updateRuntimeStats(
            recompositions = 200L,
            threads = 5,
            jvmUsedMb = 0L,
            jvmMaxMb = 0L,
            nativeHeapMb = 0L,
        )
        assertEquals(200L, WickKitPerformanceManager.snapshot.value.recompositionCount)
    }

    @Test
    fun `updateRuntimeStats stores thread count`() {
        WickKitPerformanceManager.updateRuntimeStats(
            recompositions = 0L,
            threads = 18,
            jvmUsedMb = 0L,
            jvmMaxMb = 0L,
            nativeHeapMb = 0L,
        )
        assertEquals(18, WickKitPerformanceManager.snapshot.value.threadCount)
    }

    @Test
    fun `updateRuntimeStats stores jvm memory`() {
        WickKitPerformanceManager.updateRuntimeStats(
            recompositions = 0L,
            threads = 0,
            jvmUsedMb = 42L,
            jvmMaxMb = 256L,
            nativeHeapMb = 0L,
        )
        val s = WickKitPerformanceManager.snapshot.value
        assertEquals(42L, s.jvmUsedMb)
        assertEquals(256L, s.jvmMaxMb)
    }

    @Test
    fun `updateRuntimeStats stores native heap`() {
        WickKitPerformanceManager.updateRuntimeStats(
            recompositions = 0L,
            threads = 0,
            jvmUsedMb = 0L,
            jvmMaxMb = 0L,
            nativeHeapMb = 18L,
        )
        assertEquals(18L, WickKitPerformanceManager.snapshot.value.nativeHeapMb)
    }

    @Test
    fun `recompositionsPerSecond is zero on first call`() {
        WickKitPerformanceManager.updateRuntimeStats(
            recompositions = 100L,
            threads = 0,
            jvmUsedMb = 0L,
            jvmMaxMb = 0L,
            nativeHeapMb = 0L,
        )
        assertEquals(0f, WickKitPerformanceManager.snapshot.value.recompositionsPerSecond, 0.01f)
    }

    @Test
    fun `recompositionsPerSecond is non-negative on subsequent calls`() {
        WickKitPerformanceManager.updateRuntimeStats(
            recompositions = 100L,
            threads = 0,
            jvmUsedMb = 0L,
            jvmMaxMb = 0L,
            nativeHeapMb = 0L,
        )
        WickKitPerformanceManager.updateRuntimeStats(
            recompositions = 150L,
            threads = 0,
            jvmUsedMb = 0L,
            jvmMaxMb = 0L,
            nativeHeapMb = 0L,
        )
        val rate = WickKitPerformanceManager.snapshot.value.recompositionsPerSecond
        assert(rate >= 0f) { "Expected non-negative rate, got $rate" }
    }

    @Test
    fun `updateRuntimeStats preserves frame stats`() {
        WickKitPerformanceManager.updateFrameStats(sampleFrameStats())
        WickKitPerformanceManager.updateRuntimeStats(
            recompositions = 10L,
            threads = 5,
            jvmUsedMb = 20L,
            jvmMaxMb = 128L,
            nativeHeapMb = 8L,
        )
        val s = WickKitPerformanceManager.snapshot.value
        assertEquals(60f, s.fps)
        assertEquals(100, s.totalFrames)
    }

    // endregion

    // region reset

    @Test
    fun `reset clears frame stats`() {
        WickKitPerformanceManager.updateFrameStats(sampleFrameStats())
        WickKitPerformanceManager.reset()
        val s = WickKitPerformanceManager.snapshot.value
        assertNull(s.fps)
        assertEquals(0, s.totalFrames)
    }

    @Test
    fun `reset clears runtime stats`() {
        WickKitPerformanceManager.updateRuntimeStats(
            recompositions = 99L,
            threads = 12,
            jvmUsedMb = 50L,
            jvmMaxMb = 200L,
            nativeHeapMb = 10L,
        )
        WickKitPerformanceManager.reset()
        val s = WickKitPerformanceManager.snapshot.value
        assertEquals(0L, s.recompositionCount)
        assertEquals(0, s.threadCount)
        assertEquals(0L, s.jvmUsedMb)
    }

    @Test
    fun `reset clears activity name`() {
        WickKitPerformanceManager.updateFrameStats(sampleFrameStats())
        WickKitPerformanceManager.reset()
        assertEquals("", WickKitPerformanceManager.snapshot.value.activityName)
    }

    // endregion

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
