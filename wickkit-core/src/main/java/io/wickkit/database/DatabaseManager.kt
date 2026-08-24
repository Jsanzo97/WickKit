package io.wickkit.database

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

internal class DatabaseManager(path: String) : AutoCloseable {

    private val database = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE)

    fun listTables(): List<String> = database.rawQuery(
        "SELECT name FROM sqlite_master " +
            "WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' " +
            "ORDER BY name",
        null,
    ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }

    fun getRowCount(table: String): Long = database.rawQuery(
        "SELECT COUNT(*) FROM ${table.q()}",
        null,
    ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }

    fun getColumns(table: String): List<ColumnInfo> = database.rawQuery(
        "PRAGMA table_info(${table.q()})",
        null,
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    ColumnInfo(
                        name = cursor.columnString("name"),
                        type = cursor.columnString("type").uppercase().ifEmpty { "TEXT" },
                        isPrimaryKey = cursor.columnInt("pk") > 0,
                        notNull = cursor.columnInt("notnull") > 0,
                    ),
                )
            }
        }
    }

    fun getRows(table: String, limit: Int = 300, offset: Int = 0): List<List<Any?>> = database.rawQuery(
        "SELECT * FROM ${table.q()} LIMIT $limit OFFSET $offset",
        null,
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    (0 until cursor.columnCount).map { columnIndex ->
                        when (cursor.getType(columnIndex)) {
                            Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(columnIndex)
                            Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(columnIndex)
                            Cursor.FIELD_TYPE_STRING -> cursor.getString(columnIndex)
                            Cursor.FIELD_TYPE_BLOB -> "[BLOB]"
                            else -> null
                        }
                    },
                )
            }
        }
    }

    fun updateRow(
        table: String,
        columns: List<ColumnInfo>,
        originalRow: List<Any?>,
        edits: Map<String, String>,
    ) {
        val primaryKeys = columns.filter { it.isPrimaryKey }
        check(primaryKeys.isNotEmpty()) { "No primary key on table $table" }
        val setClause = edits.keys.joinToString(", ") { "${it.q()} = ?" }
        val whereClause = primaryKeys.joinToString(" AND ") { "${it.name.q()} = ?" }
        val allArgs = buildList<Any?> {
            addAll(edits.values)
            addAll(
                primaryKeys.map { primaryKey ->
                    originalRow.getOrNull(columns.indexOfFirst { it.name == primaryKey.name })?.toString()
                },
            )
        }.toTypedArray()
        database.execSQL("UPDATE ${table.q()} SET $setClause WHERE $whereClause", allArgs)
    }

    override fun close() = database.close()

    private fun String.q() = "\"${replace("\"", "\"\"")}\""
    private fun Cursor.columnString(columnName: String) = getString(getColumnIndexOrThrow(columnName)) ?: ""
    private fun Cursor.columnInt(columnName: String) = getInt(getColumnIndexOrThrow(columnName))
}
