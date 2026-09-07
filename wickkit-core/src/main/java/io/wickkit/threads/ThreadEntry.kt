package io.wickkit.threads

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class ThreadEntry(
    val id: Long,
    val name: String,
    val state: Thread.State,
    val isDaemon: Boolean,
    val priority: Int,
    val threadGroup: String,
    val stackTrace: ImmutableList<String>,
)
