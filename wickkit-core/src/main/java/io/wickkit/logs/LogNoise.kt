package io.wickkit.logs

import kotlinx.collections.immutable.ImmutableList

internal object LogNoise {

    private val systemTags = setOf(
        "Choreographer", "OpenGLRenderer", "EGL_emulation", "libEGL",
        "Gralloc", "SurfaceFlinger", "BufferQueueProducer", "HardwareRenderer",
        "RenderThread", "InputReader", "InputDispatcher", "InputMethodManager",
        "ViewRootImpl", "DecorView", "art", "dalvikvm", "zygote",
        "chatty", "libc", "malloc",
    )

    private val systemPrefixes = listOf(
        "com.android.",
        "android.hardware.",
        "android.os.",
    )

    fun isSystemTag(tag: String): Boolean = tag in systemTags || systemPrefixes.any { tag.startsWith(it) }

    fun noisyTags(
        entries: ImmutableList<LogEntry>,
        windowSize: Int = 100,
        threshold: Float = 0.3f,
    ): Set<String> {
        if (entries.size < 20) return emptySet()
        val window = entries.takeLast(windowSize)
        val limit = maxOf((window.size * threshold).toInt(), 10)
        return window.groupingBy { it.tag }.eachCount()
            .filterValues { it >= limit }
            .keys
    }
}
