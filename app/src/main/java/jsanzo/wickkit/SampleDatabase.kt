package jsanzo.wickkit

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SampleDatabase(context: Context) : SQLiteOpenHelper(context, "wickkit_sample.db", null, 1) {

    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE users (
                id      INTEGER PRIMARY KEY AUTOINCREMENT,
                name    TEXT    NOT NULL,
                email   TEXT    NOT NULL,
                age     INTEGER,
                role    TEXT    NOT NULL DEFAULT 'user'
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE products (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                name        TEXT    NOT NULL,
                price       REAL    NOT NULL,
                category    TEXT,
                in_stock    INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent(),
        )
        seedUsers(database)
        seedProducts(database)
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun reseed() {
        writableDatabase.run {
            delete("users", null, null)
            delete("products", null, null)
            seedUsers(this)
            seedProducts(this)
        }
    }

    private fun seedUsers(database: SQLiteDatabase) {
        val rows = listOf(
            row("name" to "Alice Martín", "email" to "alice@example.com", "age" to 29, "role" to "admin"),
            row("name" to "Bob García", "email" to "bob@example.com", "age" to 34, "role" to "user"),
            row("name" to "Carlos López", "email" to "carlos@example.com", "age" to 27, "role" to "user"),
            row("name" to "Diana Ruiz", "email" to "diana@example.com", "age" to 42, "role" to "moderator"),
            row("name" to "Elena Pérez", "email" to "elena@example.com", "age" to 31, "role" to "user"),
        )
        rows.forEach { database.insert("users", null, it) }
    }

    private fun seedProducts(database: SQLiteDatabase) {
        val rows = listOf(
            row("name" to "Wireless Keyboard", "price" to 49.99, "category" to "Electronics", "in_stock" to 1),
            row("name" to "USB-C Hub", "price" to 34.95, "category" to "Electronics", "in_stock" to 1),
            row("name" to "Mechanical Pencil", "price" to 8.50, "category" to "Stationery", "in_stock" to 1),
            row("name" to "Notebook A5", "price" to 12.00, "category" to "Stationery", "in_stock" to 0),
            row("name" to "Standing Desk Mat", "price" to 79.00, "category" to "Office", "in_stock" to 1),
            row("name" to "Cable Organiser", "price" to 15.99, "category" to "Office", "in_stock" to 0),
        )
        rows.forEach { database.insert("products", null, it) }
    }

    private fun row(vararg pairs: Pair<String, Any?>): ContentValues = ContentValues().apply {
        pairs.forEach { (key, value) ->
            when (value) {
                is String -> put(key, value)
                is Int -> put(key, value)
                is Long -> put(key, value)
                is Double -> put(key, value)
                is Float -> put(key, value)
                null -> putNull(key)
            }
        }
    }
}
