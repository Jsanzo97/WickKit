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
        val ctx = RuntimeEnvironment.getApplication()
        dbFile = ctx.getDatabasePath("inspector_test.db").also { it.parentFile?.mkdirs() }

        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
            db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, score REAL)")
            db.execSQL("CREATE TABLE products (id INTEGER PRIMARY KEY, label TEXT)")
            db.execSQL("INSERT INTO users (name, score) VALUES ('Alice', 9.5)")
            db.execSQL("INSERT INTO users (name, score) VALUES ('Bob', 7.0)")
            db.execSQL("INSERT INTO products (id, label) VALUES (1, 'Widget')")
        }
        inspector = DatabaseManager(dbFile.absolutePath)
    }

    @After
    fun tearDown() {
        if (::inspector.isInitialized) inspector.close()
        dbFile.delete()
    }

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
        val ctx = RuntimeEnvironment.getApplication()
        val file = ctx.getDatabasePath("empty_count.db").also { it.parentFile?.mkdirs() }
        try {
            SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
                db.execSQL("CREATE TABLE empty_t (id INTEGER PRIMARY KEY)")
            }
            DatabaseManager(file.absolutePath).use { insp ->
                assertEquals(0L, insp.getRowCount("empty_t"))
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
        val ctx = RuntimeEnvironment.getApplication()
        val file = ctx.getDatabasePath("typeless.db").also { it.parentFile?.mkdirs() }
        try {
            SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
                db.execSQL("CREATE TABLE t (id INTEGER PRIMARY KEY, value)")
            }
            DatabaseManager(file.absolutePath).use { insp ->
                val col = insp.getColumns("t").first { it.name == "value" }
                assertEquals("TEXT", col.type)
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
        val ctx = RuntimeEnvironment.getApplication()
        val file = ctx.getDatabasePath("null_test.db").also { it.parentFile?.mkdirs() }
        try {
            SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
                db.execSQL("CREATE TABLE t (id INTEGER PRIMARY KEY, val TEXT)")
                db.execSQL("INSERT INTO t (id) VALUES (1)")
            }
            DatabaseManager(file.absolutePath).use { insp ->
                val row = insp.getRows("t").single()
                assertNull(row[1])
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun `getRows maps BLOB column to placeholder string`() {
        val ctx = RuntimeEnvironment.getApplication()
        val file = ctx.getDatabasePath("blob_test.db").also { it.parentFile?.mkdirs() }
        try {
            SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
                db.execSQL("CREATE TABLE t (id INTEGER PRIMARY KEY, data BLOB)")
                db.execSQL("INSERT INTO t (id, data) VALUES (1, X'DEADBEEF')")
            }
            DatabaseManager(file.absolutePath).use { insp ->
                val row = insp.getRows("t").single()
                assertEquals("[BLOB]", row[1])
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun `getRows respects limit`() {
        assertEquals(1, inspector.getRows("users", limit = 1).size)
    }

    @Test
    fun `getRows respects offset`() {
        val all = inspector.getRows("users")
        val secondPage = inspector.getRows("users", limit = 1, offset = 1)
        assertEquals(all[1][0], secondPage.single()[0])
    }

    // endregion

    // region updateRow

    @Test
    fun `updateRow modifies only the target row`() {
        val cols = inspector.getColumns("users")
        val aliceRow = inspector.getRows("users").first { it[1] == "Alice" }

        inspector.updateRow("users", cols, aliceRow, mapOf("name" to "Alicia"))

        val updated = inspector.getRows("users")
        assertTrue(updated.any { it[1] == "Alicia" })
        assertFalse(updated.any { it[1] == "Alice" })
        assertTrue(updated.any { it[1] == "Bob" })
    }

    @Test(expected = IllegalStateException::class)
    fun `updateRow throws when table has no primary key`() {
        val ctx = RuntimeEnvironment.getApplication()
        val file = ctx.getDatabasePath("no_pk.db").also { it.parentFile?.mkdirs() }
        try {
            SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
                db.execSQL("CREATE TABLE data (value TEXT)")
                db.execSQL("INSERT INTO data VALUES ('test')")
            }
            DatabaseManager(file.absolutePath).use { insp ->
                val cols = insp.getColumns("data")
                val rows = insp.getRows("data")
                insp.updateRow("data", cols, rows[0], mapOf("value" to "changed"))
            }
        } finally {
            file.delete()
        }
    }

    // endregion
}
