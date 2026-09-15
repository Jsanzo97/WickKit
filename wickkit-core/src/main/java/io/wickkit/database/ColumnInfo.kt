package io.wickkit.database

@androidx.compose.runtime.Immutable
internal data class ColumnInfo(
    val name: String,
    val type: String,
    val isPrimaryKey: Boolean,
    val notNull: Boolean,
    val isRowId: Boolean = false,
)
