package io.wickkit.compose

import androidx.compose.runtime.Immutable

enum class RecomposeSeverity { YELLOW, ORANGE, RED }

@Immutable
data class ComposableEntry(
    val name: String,
    val totalCount: Long,
    val ratePerSecond: Float,
    val peakRatePerSecond: Float,
    val severity: RecomposeSeverity,
)
