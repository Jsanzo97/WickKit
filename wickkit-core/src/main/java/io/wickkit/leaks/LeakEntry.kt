package io.wickkit.leaks

import androidx.compose.runtime.Immutable

@Immutable
internal data class LeakEntry(
    val id: Long,
    val className: String,
    val detectedAt: String,
    val instanceCount: Int,
    val firstDetectedAt: String = detectedAt,
)
