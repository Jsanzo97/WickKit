package io.wickkit.flags

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SharedPrefsDiscoveryTest {

    @get:Rule val tempDir = TemporaryFolder()

    private lateinit var prefsDir: File

    @Before
    fun setUp() {
        prefsDir = tempDir.newFolder("shared_prefs")
    }

    @After
    fun tearDown() {
        prefsDir.listFiles()?.forEach { it.delete() }
    }

    @Test
    fun `discoverNames excludes wickkit_flags`() {
        File(prefsDir, "wickkit_flags.xml").createNewFile()
        File(prefsDir, "user_settings.xml").createNewFile()

        val names = SharedPrefsDiscovery.discoverNames(prefsDir)

        assertFalse("wickkit_flags" in names)
        assertTrue("user_settings" in names)
    }

    @Test
    fun `discoverNames returns only xml files without extension`() {
        File(prefsDir, "app_prefs.xml").createNewFile()
        File(prefsDir, "readme.txt").createNewFile()

        val names = SharedPrefsDiscovery.discoverNames(prefsDir)

        assertTrue("app_prefs" in names)
        assertFalse("readme" in names)
        assertFalse("readme.txt" in names)
    }

    @Test
    fun `discoverNames returns empty list when directory is empty`() {
        val names = SharedPrefsDiscovery.discoverNames(prefsDir)

        assertTrue(names.isEmpty())
    }
}
