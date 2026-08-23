package io.wickkit.database

enum class DatabaseStatus { Ok, Encrypted, Unsupported }

data class DatabaseEntry(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val status: DatabaseStatus,
)
