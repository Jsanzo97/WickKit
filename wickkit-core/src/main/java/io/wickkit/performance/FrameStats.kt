package io.wickkit.performance

internal data class FrameStats(
    val fps: Float,
    val slowFrames: Int,
    val frozenFrames: Int,
    val jankRate: Float,
    val p50Ms: Float,
    val p90Ms: Float,
    val totalFrames: Int,
)
