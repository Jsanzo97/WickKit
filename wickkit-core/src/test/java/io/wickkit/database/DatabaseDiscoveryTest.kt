package io.wickkit.database

import android.database.sqlite.SQLiteDatabase
import org.junit.After
import org.junit.Assert.assertEquals
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
class DatabaseDiscoveryTest {

    private lateinit var dbsDir: File

    private val ctx get() = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        dbsDir = ctx.getDatabasePath("_").parentFile!!.also { it.mkdirs() }
        dbsDir.listFiles()?.forEach { it.delete() }
    }

    @After
    fun tearDown() {
        dbsDir.listFiles()?.forEach { it.delete() }
    }

    @Test
    fun `findDatabases returns empty list when directory is empty`() {
        assertTrue(DatabaseDiscovery.findDatabases(ctx).isEmpty())
    }

    @Test
    fun `findDatabases filters out wal shm and journal auxiliary files`() {
        SQLiteDatabase.openOrCreateDatabase(File(dbsDir, "main.db"), null).close()
        File(dbsDir, "main.db-wal").createNewFile()
        File(dbsDir, "main.db-shm").createNewFile()
        File(dbsDir, "main.db-journal").createNewFile()

        val result = DatabaseDiscovery.findDatabases(ctx)

        assertEquals(1, result.size)
        assertEquals("main.db", result.single().name)
    }

    @Test
    fun `findDatabases marks valid SQLite file as Ok`() {
        SQLiteDatabase.openOrCreateDatabase(File(dbsDir, "valid.db"), null).close()

        val entry = DatabaseDiscovery.findDatabases(ctx).single()

        assertEquals(DatabaseStatus.Ok, entry.status)
    }

    @Test
    fun `findDatabases ignores subdirectories inside the databases folder`() {
        File(dbsDir, "subdir").mkdir()

        assertTrue(DatabaseDiscovery.findDatabases(ctx).isEmpty())
    }

    @Test
    fun `findDatabases returns entries sorted by name`() {
        listOf("zebra.db", "apple.db", "mango.db").forEach { name ->
            SQLiteDatabase.openOrCreateDatabase(File(dbsDir, name), null).close()
        }

        val names = DatabaseDiscovery.findDatabases(ctx).map { it.name }

        assertEquals(listOf("apple.db", "mango.db", "zebra.db"), names)
    }

    @Test
    fun `findDatabases records the correct file size`() {
        val file = File(dbsDir, "sized.db")
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE t (id INTEGER PRIMARY KEY)")
        }

        val entry = DatabaseDiscovery.findDatabases(ctx).single()

        assertEquals(file.length(), entry.sizeBytes)
    }
}
