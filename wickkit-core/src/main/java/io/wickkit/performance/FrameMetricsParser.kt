package io.wickkit.performance

private const val SLOW_FRAME_THRESHOLD_MS = 16
private const val FROZEN_FRAME_THRESHOLD_MS = 700
private const val PERCENT = 100

internal fun parseFrameData(entries: List<Pair<Int, Int>>): FrameStats? {
    val sorted = entries.filter { (durationMs, count) -> durationMs > 0 && count > 0 }.sortedBy { it.first }
    if (sorted.isEmpty()) return null

    var totalFrames = 0
    var totalDurationMs = 0L
    var slowFrames = 0
    var frozenFrames = 0

    for ((durationMs, count) in sorted) {
        totalFrames += count
        totalDurationMs += durationMs.toLong() * count
        if (durationMs > SLOW_FRAME_THRESHOLD_MS) slowFrames += count
        if (durationMs > FROZEN_FRAME_THRESHOLD_MS) frozenFrames += count
    }

    if (totalFrames == 0) return null

    val avgDurationMs = totalDurationMs.toFloat() / totalFrames
    val fps = if (avgDurationMs > 0f) 1000f / avgDurationMs else 0f
    val jankRate = slowFrames.toFloat() / totalFrames * PERCENT

    return FrameStats(
        fps = fps,
        slowFrames = slowFrames,
        frozenFrames = frozenFrames,
        jankRate = jankRate,
        p50Ms = percentile(
            sorted = sorted,
            totalFrames = totalFrames,
            targetPercentile = 50,
        ).toFloat(),
        p90Ms = percentile(
            sorted = sorted,
            totalFrames = totalFrames,
            targetPercentile = 90,
        ).toFloat(),
        totalFrames = totalFrames,
    )
}

private fun percentile(sorted: List<Pair<Int, Int>>, totalFrames: Int, targetPercentile: Int): Int {
    val target = (totalFrames * targetPercentile) / PERCENT
    var cumulative = 0
    for ((durationMs, count) in sorted) {
        cumulative += count
        if (cumulative >= target) return durationMs
    }
    return sorted.last().first
}
