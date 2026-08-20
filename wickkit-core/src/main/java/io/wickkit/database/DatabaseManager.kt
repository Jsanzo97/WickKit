package io.wickkit.database

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

class DatabaseManager(path: String) : AutoCloseable {

    private val db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE)

    fun listTables(): List<String> = db.rawQuery(
        "SELECT name FROM sqlite_master " +
            "WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' " +
            "ORDER BY name",
        null,
    ).use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }

    fun getRowCount(table: String): Long = db.rawQuery(
        "SELECT COUNT(*) FROM ${table.q()}",
        null,
    ).use { c -> if (c.moveToFirst()) c.getLong(0) else 0L }

    fun getColumns(table: String): List<ColumnInfo> = db.rawQuery(
        "PRAGMA table_info(${table.q()})",
        null,
    ).use { c ->
        buildList {
            while (c.moveToNext()) {
                add(
                    ColumnInfo(
                        name = c.str("name"),
                        type = c.str("type").uppercase().ifEmpty { "TEXT" },
                        isPrimaryKey = c.int("pk") > 0,
                        notNull = c.int("notnull") > 0,
                    ),
                )
            }
        }
    }

    fun getRows(table: String, limit: Int = 300, offset: Int = 0): List<List<Any?>> = db.rawQuery(
        "SELECT * FROM ${table.q()} LIMIT $limit OFFSET $offset",
        null,
    ).use { c ->
        buildList {
            while (c.moveToNext()) {
                add(
                    (0 until c.columnCount).map { i ->
                        when (c.getType(i)) {
                            Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
                            Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
                            Cursor.FIELD_TYPE_STRING -> c.getString(i)
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
        val pks = columns.filter { it.isPrimaryKey }
        check(pks.isNotEmpty()) { "No primary key on table $table" }
        val set = edits.keys.joinToString(", ") { "${it.q()} = ?" }
        val where = pks.joinToString(" AND ") { "${it.name.q()} = ?" }
        val allArgs = buildList<Any?> {
            addAll(edits.values)
            addAll(
                pks.map { pk ->
                    originalRow.getOrNull(columns.indexOfFirst { it.name == pk.name })?.toString()
                },
            )
        }.toTypedArray()
        db.execSQL("UPDATE ${table.q()} SET $set WHERE $where", allArgs)
    }

    override fun close() = db.close()

    private fun String.q() = "\"$this\""
    private fun Cursor.str(col: String) = getString(getColumnIndexOrThrow(col)) ?: ""
    private fun Cursor.int(col: String) = getInt(getColumnIndexOrThrow(col))
}
