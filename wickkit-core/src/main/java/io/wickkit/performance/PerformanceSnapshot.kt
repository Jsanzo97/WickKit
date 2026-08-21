package io.wickkit.performance

import androidx.compose.runtime.Immutable

@Immutable
internal data class PerformanceSnapshot(
    val activityName: String = "",
    val fps: Float? = null,
    val slowFrames: Int = 0,
    val frozenFrames: Int = 0,
    val jankRate: Float? = null,
    val p50Ms: Float? = null,
    val p90Ms: Float? = null,
    val totalFrames: Int = 0,
    val recompositionCount: Long = 0L,
    val recompositionsPerSecond: Float = 0f,
    val threadCount: Int = 0,
    val jvmUsedMb: Long = 0L,
    val jvmMaxMb: Long = 0L,
    val nativeHeapMb: Long = 0L,
)
