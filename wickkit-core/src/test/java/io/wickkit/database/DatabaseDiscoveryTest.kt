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

    private lateinit var databasesDirectory: File

    private val context get() = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        databasesDirectory = context.getDatabasePath("_").parentFile!!.also { it.mkdirs() }
        databasesDirectory.listFiles()?.forEach { it.delete() }
    }

    @After
    fun tearDown() {
        databasesDirectory.listFiles()?.forEach { it.delete() }
    }

    @Test
    fun `findDatabases returns empty list when directory is empty`() {
        assertTrue(DatabaseDiscovery.findDatabases(context).isEmpty())
    }

    @Test
    fun `findDatabases filters out wal shm and journal auxiliary files`() {
        SQLiteDatabase.openOrCreateDatabase(File(databasesDirectory, "main.db"), null).close()
        File(databasesDirectory, "main.db-wal").createNewFile()
        File(databasesDirectory, "main.db-shm").createNewFile()
        File(databasesDirectory, "main.db-journal").createNewFile()

        val result = DatabaseDiscovery.findDatabases(context)

        assertEquals(1, result.size)
        assertEquals("main.db", result.single().name)
    }

    @Test
    fun `findDatabases marks valid SQLite file as Ok`() {
        SQLiteDatabase.openOrCreateDatabase(File(databasesDirectory, "valid.db"), null).close()

        val entry = DatabaseDiscovery.findDatabases(context).single()

        assertEquals(DatabaseStatus.Ok, entry.status)
    }

    @Test
    fun `findDatabases ignores subdirectories inside the databases folder`() {
        File(databasesDirectory, "subdir").mkdir()

        assertTrue(DatabaseDiscovery.findDatabases(context).isEmpty())
    }

    @Test
    fun `findDatabases returns entries sorted by name`() {
        listOf("zebra.db", "apple.db", "mango.db").forEach { name ->
            SQLiteDatabase.openOrCreateDatabase(File(databasesDirectory, name), null).close()
        }

        val names = DatabaseDiscovery.findDatabases(context).map { it.name }

        assertEquals(listOf("apple.db", "mango.db", "zebra.db"), names)
    }

    @Test
    fun `findDatabases filters out files matching excluded simple prefixes`() {
        File(databasesDirectory, "firebase_analytics.db").createNewFile()
        File(databasesDirectory, "google_app_measurement.db").createNewFile()
        File(databasesDirectory, "gtm_session.db").createNewFile()
        SQLiteDatabase.openOrCreateDatabase(File(databasesDirectory, "app.db"), null).close()

        val names = DatabaseDiscovery.findDatabases(context).map { it.name }

        assertEquals(listOf("app.db"), names)
    }

    @Test
    fun `findDatabases records the correct file size`() {
        val file = File(databasesDirectory, "sized.db")
        SQLiteDatabase.openOrCreateDatabase(file, null).use { sqliteDatabase ->
            sqliteDatabase.execSQL("CREATE TABLE t (id INTEGER PRIMARY KEY)")
        }

        val entry = DatabaseDiscovery.findDatabases(context).single()

        assertEquals(file.length(), entry.sizeBytes)
    }
}
