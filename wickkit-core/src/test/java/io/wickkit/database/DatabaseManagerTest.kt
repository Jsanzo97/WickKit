package io.wickkit.database

import android.database.sqlite.SQLiteDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseManagerTest {

    private lateinit var dbFile: File
    private lateinit var inspector: DatabaseManager

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        dbFile = context.getDatabasePath("inspector_test.db").also { it.parentFile?.mkdirs() }

        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { sqliteDatabase ->
            sqliteDatabase.execSQL(
                "CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, score REAL)",
            )
            sqliteDatabase.execSQL("CREATE TABLE products (id INTEGER PRIMARY KEY, label TEXT)")
            sqliteDatabase.execSQL("INSERT INTO users (name, score) VALUES ('Alice', 9.5)")
            sqliteDatabase.execSQL("INSERT INTO users (name, score) VALUES ('Bob', 7.0)")
            sqliteDatabase.execSQL("INSERT INTO products (id, label) VALUES (1, 'Widget')")
        }
        inspector = DatabaseManager(dbFile.absolutePath)
    }

    @After
    fun tearDown() {
        if (::inspector.isInitialized) inspector.close()
        dbFile.delete()
    }

    // region init — registered connection path

    @Test
    fun `uses registered database connection instead of opening a new one`() {
        val context = RuntimeEnvironment.getApplication()
        val file = context.getDatabasePath("registered_test.db").also { it.parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { setup ->
            setup.execSQL("CREATE TABLE items (id INTEGER PRIMARY KEY)")
        }
        val registeredDb = SQLiteDatabase.openOrCreateDatabase(file, null)
        WickKitDatabaseRegistry.register(registeredDb)
        try {
            val manager = DatabaseManager(file.absolutePath)
            assertTrue(manager.listTables().contains("items"))
            manager.close()
            assertTrue(registeredDb.isOpen)
        } finally {
            registeredDb.close()
            file.delete()
            val field = WickKitDatabaseRegistry::class.java.getDeclaredField("connections")
            field.isAccessible = true
            (field.get(WickKitDatabaseRegistry) as MutableList<*>).clear()
        }
    }

    // endregion

    // region listTables

    @Test
    fun `listTables excludes android underscore tables`() {
        assertFalse(inspector.listTables().any { it.startsWith("android_") })
    }

    @Test
    fun `listTables excludes sqlite underscore tables`() {
        assertFalse(inspector.listTables().any { it.startsWith("sqlite_") })
    }

    @Test
    fun `listTables returns user-defined tables`() {
        val tables = inspector.listTables()
        assertTrue(tables.contains("users"))
        assertTrue(tables.contains("products"))
    }

    // endregion

    // region getRowCount

    @Test
    fun `getRowCount returns correct count for each table`() {
        assertEquals(2L, inspector.getRowCount("users"))
        assertEquals(1L, inspector.getRowCount("products"))
    }

    @Test
    fun `getRowCount returns zero for empty table`() {
        val context = RuntimeEnvironment.getApplication()
        val file = context.getDatabasePath("empty_count.db").also { it.parentFile?.mkdirs() }
        try {
            SQLiteDatabase.openOrCreateDatabase(file, null).use { sqliteDatabase ->
                sqliteDatabase.execSQL("CREATE TABLE empty_t (id INTEGER PRIMARY KEY)")
            }
            DatabaseManager(file.absolutePath).use { manager ->
                assertEquals(0L, manager.getRowCount("empty_t"))
            }
        } finally {
            file.delete()
        }
    }

    // endregion

    // region getColumns

    @Test
    fun `getColumns marks integer primary key`() {
        val id = inspector.getColumns("users").first { it.name == "id" }
        assertTrue(id.isPrimaryKey)
        assertEquals("INTEGER", id.type)
    }

    @Test
    fun `getColumns marks non-pk text column with not-null constraint`() {
        val name = inspector.getColumns("users").first { it.name == "name" }
        assertFalse(name.isPrimaryKey)
        assertEquals("TEXT", name.type)
        assertTrue(name.notNull)
    }

    @Test
    fun `getColumns reflects nullable real column`() {
        val score = inspector.getColumns("users").first { it.name == "score" }
        assertFalse(score.notNull)
        assertEquals("REAL", score.type)
    }

    @Test
    fun `getColumns defaults empty affinity to TEXT`() {
        val context = RuntimeEnvironment.getApplication()
        val file = context.getDatabasePath("typeless.db").also { it.parentFile?.mkdirs() }
        try {
            SQLiteDatabase.openOrCreateDatabase(file, null).use { sqliteDatabase ->
                sqliteDatabase.execSQL("CREATE TABLE t (id INTEGER PRIMARY KEY, value)")
            }
            DatabaseManager(file.absolutePath).use { manager ->
                val column = manager.getColumns("t").first { it.name == "value" }
                assertEquals("TEXT", column.type)
            }
        } finally {
            file.delete()
        }
    }

    // endregion

    // region getRows

    @Test
    fun `getRows maps INTEGER column to Long`() {
        val alice = inspector.getRows("users").first { it[1] == "Alice" }
        assertTrue("expected Long but was ${alice[0]?.javaClass}", alice[0] is Long)
    }

    @Test
    fun `getRows maps REAL column to Double`() {
        val alice = inspector.getRows("users").first { it[1] == "Alice" }
        assertEquals(9.5, alice[2] as Double, 0.001)
    }

    @Test
    fun `getRows maps absent value to null`() {
        val context = RuntimeEnvironment.getApplication()
        val file = context.getDatabasePath("null_test.db").also { it.parentFile?.mkdirs() }
        try {
            SQLiteDatabase.openOrCreateDatabase(file, null).use { sqliteDatabase ->
                sqliteDatabase.execSQL("CREATE TABLE t (id INTEGER PRIMARY KEY, val TEXT)")
                sqliteDatabase.execSQL("INSERT INTO t (id) VALUES (1)")
            }
            DatabaseManager(file.absolutePath).use { manager ->
                val row = manager.getRows("t").single()
                assertNull(row[1])
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun `getRows maps BLOB column to placeholder string`() {
        val context = RuntimeEnvironment.getApplication()
        val file = context.getDatabasePath("blob_test.db").also { it.parentFile?.mkdirs() }
        try {
            SQLiteDatabase.openOrCreateDatabase(file, null).use { sqliteDatabase ->
                sqliteDatabase.execSQL("CREATE TABLE t (id INTEGER PRIMARY KEY, data BLOB)")
                sqliteDatabase.execSQL("INSERT INTO t (id, data) VALUES (1, X'DEADBEEF')")
            }
            DatabaseManager(file.absolutePath).use { manager ->
                val row = manager.getRows("t").single()
                assertEquals("[BLOB]", row[1])
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun `getRows respects limit`() {
        assertEquals(1, inspector.getRows(table = "users", limit = 1).size)
    }

    @Test
    fun `getRows respects offset`() {
        val all = inspector.getRows("users")
        val secondPage = inspector.getRows(
            table = "users",
            limit = 1,
            offset = 1,
        )
        assertEquals(all[1][0], secondPage.single()[0])
    }

    // endregion

    // region updateRow

    @Test
    fun `updateRow modifies only the target row`() {
        val cols = inspector.getColumns("users")
        val aliceRow = inspector.getRows("users").first { it[1] == "Alice" }

        inspector.updateRow(
            table = "users",
            columns = cols,
            originalRow = aliceRow,
            edits = mapOf("name" to "Alicia"),
        )

        val updated = inspector.getRows("users")
        assertTrue(updated.any { it[1] == "Alicia" })
        assertFalse(updated.any { it[1] == "Alice" })
        assertTrue(updated.any { it[1] == "Bob" })
    }

    @Test
    fun `updateRow edits no-pk table via synthetic rowid`() {
        val context = RuntimeEnvironment.getApplication()
        val file = context.getDatabasePath("no_pk.db").also { it.parentFile?.mkdirs() }
        try {
            SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
                db.execSQL("CREATE TABLE data (value TEXT)")
                db.execSQL("INSERT INTO data VALUES ('original')")
                db.execSQL("INSERT INTO data VALUES ('other')")
            }
            DatabaseManager(file.absolutePath).use { manager ->
                val cols = manager.getColumns("data")
                val rows = manager.getRows("data")
                val targetRow = rows.first { row -> row.any { it == "original" } }
                manager.updateRow(
                    table = "data",
                    columns = cols,
                    originalRow = targetRow,
                    edits = mapOf("value" to "updated"),
                )
                val result = manager.getRows("data")
                assertTrue(result.any { row -> row.any { it == "updated" } })
                assertFalse(result.any { row -> row.any { it == "original" } })
                assertTrue(result.any { row -> row.any { it == "other" } })
            }
        } finally {
            file.delete()
        }
    }

    // endregion
}
