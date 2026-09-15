package io.wickkit.performance

import io.wickkit.compose.ComposableEntry
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@androidx.compose.runtime.Immutable
internal data class RuntimeStats(
    val recompositions: Long,
    val threads: Int,
    val jvmUsedMb: Long,
    val jvmMaxMb: Long,
    val nativeHeapMb: Long,
    val composableEntries: ImmutableList<ComposableEntry> = persistentListOf(),
    val composableTrackingActive: Boolean = false,
)
