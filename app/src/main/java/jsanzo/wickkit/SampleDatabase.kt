package jsanzo.wickkit

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SampleDatabase(context: Context) : SQLiteOpenHelper(context, "wickkit_sample.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
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
        db.execSQL(
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
        seedUsers(db)
        seedProducts(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun reseed() {
        writableDatabase.run {
            delete("users", null, null)
            delete("products", null, null)
            seedUsers(this)
            seedProducts(this)
        }
    }

    private fun seedUsers(db: SQLiteDatabase) {
        val rows = listOf(
            row("name" to "Alice Martín", "email" to "alice@example.com", "age" to 29, "role" to "admin"),
            row("name" to "Bob García", "email" to "bob@example.com", "age" to 34, "role" to "user"),
            row("name" to "Carlos López", "email" to "carlos@example.com", "age" to 27, "role" to "user"),
            row("name" to "Diana Ruiz", "email" to "diana@example.com", "age" to 42, "role" to "moderator"),
            row("name" to "Elena Pérez", "email" to "elena@example.com", "age" to 31, "role" to "user"),
        )
        rows.forEach { db.insert("users", null, it) }
    }

    private fun seedProducts(db: SQLiteDatabase) {
        val rows = listOf(
            row("name" to "Wireless Keyboard", "price" to 49.99, "category" to "Electronics", "in_stock" to 1),
            row("name" to "USB-C Hub", "price" to 34.95, "category" to "Electronics", "in_stock" to 1),
            row("name" to "Mechanical Pencil", "price" to 8.50, "category" to "Stationery", "in_stock" to 1),
            row("name" to "Notebook A5", "price" to 12.00, "category" to "Stationery", "in_stock" to 0),
            row("name" to "Standing Desk Mat", "price" to 79.00, "category" to "Office", "in_stock" to 1),
            row("name" to "Cable Organiser", "price" to 15.99, "category" to "Office", "in_stock" to 0),
        )
        rows.forEach { db.insert("products", null, it) }
    }

    private fun row(vararg pairs: Pair<String, Any?>): ContentValues = ContentValues().apply {
        pairs.forEach { (k, v) ->
            when (v) {
                is String -> put(k, v)
                is Int -> put(k, v)
                is Long -> put(k, v)
                is Double -> put(k, v)
                is Float -> put(k, v)
                null -> putNull(k)
            }
        }
    }
}
