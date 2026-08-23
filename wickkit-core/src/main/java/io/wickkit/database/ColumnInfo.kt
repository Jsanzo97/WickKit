package io.wickkit.database

data class ColumnInfo(
    val name: String,
    val type: String,
    val isPrimaryKey: Boolean,
    val notNull: Boolean,
)
