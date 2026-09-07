package io.wickkit.crashes

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

internal sealed class CrashEntry {
    abstract val id: Long
    abstract val timestamp: Long

    @Immutable
    data class Crash(
        override val id: Long,
        override val timestamp: Long,
        val exceptionType: String,
        val message: String,
        val stackTrace: ImmutableList<String>,
        val threadName: String,
        val appVersion: String,
    ) : CrashEntry()

    @Immutable
    data class Anr(
        override val id: Long,
        override val timestamp: Long,
        val description: String,
        val trace: ImmutableList<String>,
        val processName: String,
    ) : CrashEntry()
}
