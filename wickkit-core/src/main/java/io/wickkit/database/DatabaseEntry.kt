package io.wickkit.database

internal enum class DatabaseStatus { Ok, Encrypted, Unsupported }

@androidx.compose.runtime.Immutable
internal data class DatabaseEntry(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val status: DatabaseStatus,
)
