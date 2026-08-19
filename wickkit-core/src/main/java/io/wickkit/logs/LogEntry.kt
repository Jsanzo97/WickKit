package io.wickkit.logs

import androidx.compose.runtime.Immutable

@Immutable
internal data class LogEntry(
    val id: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val time: String,
)
