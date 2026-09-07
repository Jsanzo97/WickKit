package io.wickkit.database

import android.database.sqlite.SQLiteDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WickKitDatabaseRegistryTest {

    @get:Rule val tempDir = TemporaryFolder()

    private val openedDatabases = mutableListOf<SQLiteDatabase>()

    @Before
    fun setUp() = clearRegistry()

    @After
    fun tearDown() {
        openedDatabases.forEach { runCatching { it.close() } }
        openedDatabases.clear()
        clearRegistry()
    }

    private fun clearRegistry() {
        val field = WickKitDatabaseRegistry::class.java.getDeclaredField("connections")
        field.isAccessible = true
        (field.get(WickKitDatabaseRegistry) as MutableList<*>).clear()
    }

    private fun openDb(name: String): SQLiteDatabase {
        val file = File(tempDir.root, name)
        return SQLiteDatabase.openOrCreateDatabase(file, null).also { openedDatabases.add(it) }
    }

    @Test
    fun `find returns null when no database is registered`() {
        assertNull(WickKitDatabaseRegistry.find("/any/path.db"))
    }

    @Test
    fun `find returns the registered database for the matching path`() {
        val db = openDb("registry_find.db")
        WickKitDatabaseRegistry.register(db)
        assertSame(db, WickKitDatabaseRegistry.find(db.path))
    }

    @Test
    fun `find returns null when path does not match any registered database`() {
        val db = openDb("registry_no_match.db")
        WickKitDatabaseRegistry.register(db)
        assertNull(WickKitDatabaseRegistry.find("/other/path.db"))
    }

    @Test
    fun `register does not add the same database instance twice`() {
        val db = openDb("registry_dedup.db")
        WickKitDatabaseRegistry.register(db)
        WickKitDatabaseRegistry.register(db)
        val field = WickKitDatabaseRegistry::class.java.getDeclaredField("connections")
        field.isAccessible = true
        assertEquals(1, (field.get(WickKitDatabaseRegistry) as List<*>).size)
    }

    @Test
    fun `find returns null after registered database is closed`() {
        val db = openDb("registry_closed.db")
        val path = db.path
        WickKitDatabaseRegistry.register(db)
        db.close()
        assertNull(WickKitDatabaseRegistry.find(path))
    }

    @Test
    fun `multiple databases can be registered and found independently`() {
        val dbA = openDb("registry_multi_a.db")
        val dbB = openDb("registry_multi_b.db")
        WickKitDatabaseRegistry.register(dbA)
        WickKitDatabaseRegistry.register(dbB)
        assertSame(dbA, WickKitDatabaseRegistry.find(dbA.path))
        assertSame(dbB, WickKitDatabaseRegistry.find(dbB.path))
    }
}
