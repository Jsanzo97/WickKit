package io.wickkit.flags

import android.content.Context
import org.junit.Assert.assertFalse
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
class SharedPrefsDiscoveryTest {

    private lateinit var context: Context
    private lateinit var prefsDir: File

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        prefsDir.mkdirs()
        prefsDir.listFiles()?.forEach { it.delete() }
    }

    @Test
    fun `discoverNames excludes wickkit_flags`() {
        File(prefsDir, "wickkit_flags.xml").createNewFile()
        File(prefsDir, "user_settings.xml").createNewFile()

        val names = SharedPrefsDiscovery.discoverNames(context)

        assertFalse("wickkit_flags" in names)
        assertTrue("user_settings" in names)
    }

    @Test
    fun `discoverNames returns only xml files without extension`() {
        File(prefsDir, "app_prefs.xml").createNewFile()
        File(prefsDir, "readme.txt").createNewFile()

        val names = SharedPrefsDiscovery.discoverNames(context)

        assertTrue("app_prefs" in names)
        assertFalse("readme" in names)
        assertFalse("readme.txt" in names)
    }

    @Test
    fun `discoverNames returns empty list when directory is empty`() {
        val names = SharedPrefsDiscovery.discoverNames(context)

        assertTrue(names.isEmpty())
    }
}
