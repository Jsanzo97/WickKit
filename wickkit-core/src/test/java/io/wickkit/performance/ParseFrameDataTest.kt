package io.wickkit.performance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ParseFrameDataTest {

    // region null / empty input

    @Test
    fun `returns null for empty list`() {
        assertNull(parseFrameData(emptyList()))
    }

    @Test
    fun `returns null when all entries have zero duration`() {
        assertNull(parseFrameData(listOf(0 to 5, 0 to 3)))
    }

    @Test
    fun `returns null when all entries have zero count`() {
        assertNull(parseFrameData(listOf(8 to 0, 16 to 0)))
    }

    @Test
    fun `returns null when all entries have zero duration or zero count`() {
        assertNull(parseFrameData(listOf(0 to 3, 8 to 0)))
    }

    // endregion

    // region totalFrames

    @Test
    fun `computes total frames as sum of all counts`() {
        val stats = parseFrameData(listOf(8 to 100, 16 to 50, 20 to 10))
        assertNotNull(stats)
        assertEquals(160, stats!!.totalFrames)
    }

    @Test
    fun `ignores zero-duration entries when computing total frames`() {
        val stats = parseFrameData(listOf(0 to 99, 8 to 100))
        assertNotNull(stats)
        assertEquals(100, stats!!.totalFrames)
    }

    // endregion

    // region fps

    @Test
    fun `computes fps as 1000 divided by average duration`() {
        // all frames at 16ms → avg = 16ms → fps = 62.5
        val stats = parseFrameData(listOf(16 to 100))
        assertNotNull(stats)
        assertEquals(62.5f, stats!!.fps, 0.01f)
    }

    @Test
    fun `computes fps from mixed durations`() {
        // 50 frames at 8ms + 50 frames at 24ms → avg = 16ms → fps = 62.5
        val stats = parseFrameData(listOf(8 to 50, 24 to 50))
        assertNotNull(stats)
        assertEquals(62.5f, stats!!.fps, 0.01f)
    }

    // endregion

    // region slow frames

    @Test
    fun `counts frames strictly above 16ms as slow`() {
        val stats = parseFrameData(listOf(16 to 80, 17 to 20))
        assertNotNull(stats)
        assertEquals(20, stats!!.slowFrames)
    }

    @Test
    fun `does not count frame at exactly 16ms as slow`() {
        val stats = parseFrameData(listOf(16 to 100))
        assertNotNull(stats)
        assertEquals(0, stats!!.slowFrames)
    }

    @Test
    fun `counts all frames above 16ms as slow`() {
        val stats = parseFrameData(listOf(17 to 30, 33 to 10, 800 to 5))
        assertNotNull(stats)
        assertEquals(45, stats!!.slowFrames)
    }

    // endregion

    // region frozen frames

    @Test
    fun `counts frames strictly above 700ms as frozen`() {
        val stats = parseFrameData(listOf(16 to 90, 700 to 5, 701 to 3, 1000 to 2))
        assertNotNull(stats)
        assertEquals(5, stats!!.frozenFrames)
    }

    @Test
    fun `does not count frame at exactly 700ms as frozen`() {
        val stats = parseFrameData(listOf(700 to 10))
        assertNotNull(stats)
        assertEquals(0, stats!!.frozenFrames)
    }

    // endregion

    // region jank rate

    @Test
    fun `computes jank rate as percentage of slow frames`() {
        // 10 slow out of 100 total → 10%
        val stats = parseFrameData(listOf(8 to 90, 20 to 10))
        assertNotNull(stats)
        assertEquals(10f, stats!!.jankRate, 0.01f)
    }

    @Test
    fun `jank rate is zero when no slow frames`() {
        val stats = parseFrameData(listOf(8 to 100, 16 to 50))
        assertNotNull(stats)
        assertEquals(0f, stats!!.jankRate, 0.01f)
    }

    @Test
    fun `jank rate is 100 percent when all frames are slow`() {
        val stats = parseFrameData(listOf(17 to 100))
        assertNotNull(stats)
        assertEquals(100f, stats!!.jankRate, 0.01f)
    }

    // endregion

    // region percentiles

    @Test
    fun `p50 returns median frame duration`() {
        // 50 frames at 8ms, 50 frames at 16ms → median is 8ms (50th percentile at frame 50)
        val stats = parseFrameData(listOf(8 to 50, 16 to 50))
        assertNotNull(stats)
        assertEquals(8f, stats!!.p50Ms, 0.01f)
    }

    @Test
    fun `p90 returns 90th percentile frame duration`() {
        // 90 frames at 8ms, 10 frames at 40ms → p90 = 8ms (cumulative hits 90 at 8ms bucket)
        val stats = parseFrameData(listOf(8 to 90, 40 to 10))
        assertNotNull(stats)
        assertEquals(8f, stats!!.p90Ms, 0.01f)
    }

    @Test
    fun `p90 captures slowest bucket when most frames are slow`() {
        // 5 frames at 8ms, 95 frames at 50ms → p90 is 50ms
        val stats = parseFrameData(listOf(8 to 5, 50 to 95))
        assertNotNull(stats)
        assertEquals(50f, stats!!.p90Ms, 0.01f)
    }

    // endregion

    // region single entry

    @Test
    fun `handles single entry correctly`() {
        val stats = parseFrameData(listOf(10 to 1))
        assertNotNull(stats)
        assertEquals(1, stats!!.totalFrames)
        assertEquals(100f, stats.fps, 0.01f)
        assertEquals(0, stats.slowFrames)
        assertEquals(0, stats.frozenFrames)
    }

    // endregion
}
