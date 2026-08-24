package io.wickkit.database

internal data class ColumnInfo(
    val name: String,
    val type: String,
    val isPrimaryKey: Boolean,
    val notNull: Boolean,
)
