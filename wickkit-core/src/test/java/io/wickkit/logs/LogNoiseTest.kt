package io.wickkit.logs

import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogNoiseTest {

    // region isSystemTag

    @Test
    fun `known system tags are recognized`() {
        assertTrue(LogNoise.isSystemTag("Choreographer"))
        assertTrue(LogNoise.isSystemTag("OpenGLRenderer"))
        assertTrue(LogNoise.isSystemTag("art"))
        assertTrue(LogNoise.isSystemTag("zygote"))
        assertTrue(LogNoise.isSystemTag("chatty"))
    }

    @Test
    fun `tags starting with com_android are system tags`() {
        assertTrue(LogNoise.isSystemTag("com.android.internal.Something"))
    }

    @Test
    fun `tags starting with android_hardware are system tags`() {
        assertTrue(LogNoise.isSystemTag("android.hardware.camera2.Foo"))
    }

    @Test
    fun `tags starting with android_os are system tags`() {
        assertTrue(LogNoise.isSystemTag("android.os.Handler"))
    }

    @Test
    fun `app tags are not system tags`() {
        assertFalse(LogNoise.isSystemTag("MainActivity"))
        assertFalse(LogNoise.isSystemTag("com.myapp.Feature"))
    }

    @Test
    fun `android prefix without known sub-namespace is not a system tag`() {
        assertFalse(LogNoise.isSystemTag("android.util.Log"))
    }

    // endregion

    // region noisyTags

    @Test
    fun `fewer than 20 entries returns empty set`() {
        val entries = List(19) { entry("NoiseTag") }.toImmutableList()
        assertTrue(LogNoise.noisyTags(entries).isEmpty())
    }

    @Test
    fun `20 entries with all different tags returns empty set`() {
        val entries = (1..20).map { entry("tag$it") }.toImmutableList()
        assertTrue(LogNoise.noisyTags(entries).isEmpty())
    }

    @Test
    fun `tag appearing in exactly 30 percent of window is noisy`() {
        // limit = max(100 * 0.3, 10).toInt() = 30; count 30 >= 30 → noisy
        val entries = (List(30) { entry("NoiseTag") } + List(70) { entry("OtherTag") })
            .toImmutableList()
        assertTrue("NoiseTag" in LogNoise.noisyTags(entries))
    }

    @Test
    fun `tag appearing in fewer than 30 percent of window is not noisy`() {
        // limit = 30; count 29 < 30 → not noisy
        val entries = (List(29) { entry("NoiseTag") } + List(71) { entry("OtherTag") })
            .toImmutableList()
        assertFalse("NoiseTag" in LogNoise.noisyTags(entries))
    }

    @Test
    fun `only the last windowSize entries are considered`() {
        // first 100 all "NoiseTag", last 100 diverse → window sees no NoiseTag
        val noisy = List(100) { entry("NoiseTag") }
        val diverse = (1..100).map { entry("unique$it") }
        val entries = (noisy + diverse).toImmutableList()
        assertFalse("NoiseTag" in LogNoise.noisyTags(entries, windowSize = 100))
    }

    @Test
    fun `custom threshold is respected`() {
        // 20 out of 100 entries → noisy at threshold 0.2, not noisy at 0.21
        val entries = (List(20) { entry("NoiseTag") } + List(80) { entry("OtherTag") })
            .toImmutableList()
        assertTrue("NoiseTag" in LogNoise.noisyTags(entries, threshold = 0.2f))
        assertFalse("NoiseTag" in LogNoise.noisyTags(entries, threshold = 0.21f))
    }

    // endregion

    private fun entry(tag: String) = LogEntry(0L, LogLevel.DEBUG, tag, "message", "00:00:00.000")
}
