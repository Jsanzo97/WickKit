package io.wickkit.compose

enum class RecomposeSeverity { YELLOW, ORANGE, RED }

data class ComposableEntry(
    val name: String,
    val totalCount: Long,
    val ratePerSecond: Float,
    val peakRatePerSecond: Float,
    val severity: RecomposeSeverity,
)
