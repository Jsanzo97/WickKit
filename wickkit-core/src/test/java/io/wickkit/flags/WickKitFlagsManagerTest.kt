package io.wickkit.flags

import android.content.Context
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WickKitFlagsManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("wickkit_flags", Context.MODE_PRIVATE).edit().clear().commit()
        WickKitFlagsManager.init(context)
    }

    @After
    fun tearDown() {
        context.getSharedPreferences("wickkit_flags", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    // ── setSpOverride ──────────────────────────────────────────────────────────

    @Test
    fun `setSpOverride writes new value to real SP`() {
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("feature", false).commit()

        WickKitFlagsManager.setSpOverride(
            prefsName = "test_prefs",
            key = "feature",
            type = FlagType.BOOLEAN,
            value = "true",
        )

        assertTrue(prefs.getBoolean("feature", false))
    }

    @Test
    fun `setSpOverride backs up original value`() {
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("name", "original").commit()

        WickKitFlagsManager.setSpOverride(
            prefsName = "test_prefs",
            key = "name",
            type = FlagType.STRING,
            value = "override",
        )

        val wickkitPrefs = context.getSharedPreferences("wickkit_flags", Context.MODE_PRIVATE)
        val backup = wickkitPrefs.getString("sp.backup.test_prefs.name", null)
        assertEquals("STRING:original", backup)
    }

    @Test
    fun `setSpOverride does not overwrite backup on second edit`() {
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("name", "original").commit()

        WickKitFlagsManager.setSpOverride(
            prefsName = "test_prefs",
            key = "name",
            type = FlagType.STRING,
            value = "first_override",
        )
        WickKitFlagsManager.setSpOverride(
            prefsName = "test_prefs",
            key = "name",
            type = FlagType.STRING,
            value = "second_override",
        )

        val wickkitPrefs = context.getSharedPreferences("wickkit_flags", Context.MODE_PRIVATE)
        val backup = wickkitPrefs.getString("sp.backup.test_prefs.name", null)
        assertEquals("STRING:original", backup)
        assertEquals("second_override", prefs.getString("name", null))
    }

    @Test
    fun `setSpOverride handles int type`() {
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("count", 5).commit()

        WickKitFlagsManager.setSpOverride(
            prefsName = "test_prefs",
            key = "count",
            type = FlagType.INT,
            value = "42",
        )

        assertEquals(42, prefs.getInt("count", 0))
    }

    @Test
    fun `setSpOverride handles long type`() {
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("timeout", 1000L).commit()

        WickKitFlagsManager.setSpOverride(
            prefsName = "test_prefs",
            key = "timeout",
            type = FlagType.LONG,
            value = "5000",
        )

        assertEquals(5000L, prefs.getLong("timeout", 0L))
    }

    @Test
    fun `setSpOverride handles float type`() {
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().putFloat("ratio", 0.5f).commit()

        WickKitFlagsManager.setSpOverride(
            prefsName = "test_prefs",
            key = "ratio",
            type = FlagType.FLOAT,
            value = "1.5",
        )

        assertEquals(1.5f, prefs.getFloat("ratio", 0f), 0.001f)
    }

    // ── clearSpOverride ────────────────────────────────────────────────────────

    @Test
    fun `clearSpOverride restores original boolean value`() {
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("feature", false).commit()

        WickKitFlagsManager.setSpOverride(
            prefsName = "test_prefs",
            key = "feature",
            type = FlagType.BOOLEAN,
            value = "true",
        )
        WickKitFlagsManager.clearSpOverride(prefsName = "test_prefs", key = "feature")

        assertFalse(prefs.getBoolean("feature", true))
    }

    @Test
    fun `clearSpOverride removes wickkit metadata`() {
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("key", "value").commit()

        WickKitFlagsManager.setSpOverride(
            prefsName = "test_prefs",
            key = "key",
            type = FlagType.STRING,
            value = "override",
        )
        WickKitFlagsManager.clearSpOverride(prefsName = "test_prefs", key = "key")

        val wickkitPrefs = context.getSharedPreferences("wickkit_flags", Context.MODE_PRIVATE)
        assertFalse(wickkitPrefs.contains("sp.backup.test_prefs.key"))
        assertFalse(wickkitPrefs.contains("sp.override.test_prefs.key"))
        assertFalse(wickkitPrefs.contains("sp.enabled.test_prefs.key"))
    }

    @Test
    fun `clearSpOverride on non-overridden key is a no-op`() {
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("key", "value").commit()

        WickKitFlagsManager.clearSpOverride(prefsName = "test_prefs", key = "key")

        assertEquals("value", prefs.getString("key", null))
    }

    // ── toggleSpOverride ───────────────────────────────────────────────────────

    @Test
    fun `toggleSpOverride disables active override and restores original`() {
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("feature", false).commit()

        WickKitFlagsManager.setSpOverride(
            prefsName = "test_prefs",
            key = "feature",
            type = FlagType.BOOLEAN,
            value = "true",
        )
        WickKitFlagsManager.toggleSpOverride(prefsName = "test_prefs", key = "feature")

        assertFalse(prefs.getBoolean("feature", true))
        val wickkitPrefs = context.getSharedPreferences("wickkit_flags", Context.MODE_PRIVATE)
        assertEquals("false", wickkitPrefs.getString("sp.enabled.test_prefs.feature", null))
    }

    @Test
    fun `toggleSpOverride re-enables disabled override`() {
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("feature", false).commit()

        WickKitFlagsManager.setSpOverride(
            prefsName = "test_prefs",
            key = "feature",
            type = FlagType.BOOLEAN,
            value = "true",
        )
        WickKitFlagsManager.toggleSpOverride(prefsName = "test_prefs", key = "feature")
        WickKitFlagsManager.toggleSpOverride(prefsName = "test_prefs", key = "feature")

        assertTrue(prefs.getBoolean("feature", false))
    }

    @Test
    fun `toggleSpOverride is no-op when enabled but backup key is missing`() {
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("feature", false).commit()
        val wickkitPrefs = context.getSharedPreferences("wickkit_flags", Context.MODE_PRIVATE)
        wickkitPrefs.edit().putString("sp.enabled.test_prefs.feature", "true").commit()

        WickKitFlagsManager.toggleSpOverride(prefsName = "test_prefs", key = "feature")

        assertFalse(prefs.getBoolean("feature", false))
    }

    @Test
    fun `toggleSpOverride is no-op when disabled but override key is missing`() {
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("feature", false).commit()
        val wickkitPrefs = context.getSharedPreferences("wickkit_flags", Context.MODE_PRIVATE)
        wickkitPrefs.edit().putString("sp.enabled.test_prefs.feature", "false").commit()

        WickKitFlagsManager.toggleSpOverride(prefsName = "test_prefs", key = "feature")

        assertFalse(prefs.getBoolean("feature", false))
    }

    // ── Remote Config public API ───────────────────────────────────────────────

    @Test
    fun `getBoolean returns remote value when no override`() {
        assertFalse(WickKitFlagsManager.getBoolean(key = "flag", remoteValue = false))
        assertTrue(WickKitFlagsManager.getBoolean(key = "flag", remoteValue = true))
    }

    @Test
    fun `getBoolean returns override when active`() {
        WickKitFlagsManager.setRcOverride(key = "flag", value = "true")

        assertTrue(WickKitFlagsManager.getBoolean(key = "flag", remoteValue = false))
    }

    @Test
    fun `getBoolean returns remote value when override disabled`() {
        WickKitFlagsManager.setRcOverride(key = "flag", value = "true")
        WickKitFlagsManager.toggleRcOverride("flag")

        assertFalse(WickKitFlagsManager.getBoolean(key = "flag", remoteValue = false))
    }

    @Test
    fun `getString returns remote value when no override`() {
        assertEquals("default", WickKitFlagsManager.getString(key = "title", remoteValue = "default"))
    }

    @Test
    fun `getString returns override when active`() {
        WickKitFlagsManager.setRcOverride(key = "title", value = "hello")

        assertEquals("hello", WickKitFlagsManager.getString(key = "title", remoteValue = "default"))
    }

    @Test
    fun `getLong returns remote value when no override`() {
        assertEquals(3000L, WickKitFlagsManager.getLong(key = "timeout", remoteValue = 3000L))
    }

    @Test
    fun `getLong returns override when active`() {
        WickKitFlagsManager.setRcOverride(key = "timeout", value = "5000")

        assertEquals(5000L, WickKitFlagsManager.getLong(key = "timeout", remoteValue = 0L))
    }

    @Test
    fun `getDouble returns remote value when no override`() {
        assertEquals(0.5, WickKitFlagsManager.getDouble(key = "ratio", remoteValue = 0.5), 0.001)
    }

    @Test
    fun `getDouble returns override when active`() {
        WickKitFlagsManager.setRcOverride(key = "ratio", value = "0.75")

        assertEquals(0.75, WickKitFlagsManager.getDouble(key = "ratio", remoteValue = 0.0), 0.001)
    }

    @Test
    fun `clearRcOverride makes getBoolean return remote value`() {
        WickKitFlagsManager.setRcOverride(key = "flag", value = "true")
        WickKitFlagsManager.clearRcOverride("flag")

        assertFalse(WickKitFlagsManager.getBoolean(key = "flag", remoteValue = false))
    }

    @Test
    fun `toggleRcOverride re-enables a disabled override`() {
        WickKitFlagsManager.setRcOverride(key = "flag", value = "true")
        WickKitFlagsManager.toggleRcOverride("flag") // disable
        WickKitFlagsManager.toggleRcOverride("flag") // re-enable

        assertTrue(WickKitFlagsManager.getBoolean(key = "flag", remoteValue = false))
    }

    // ── Remote Config entry building ───────────────────────────────────────────

    @Test
    fun `remoteConfigEntries builds entry with active override`() {
        mockkObject(RemoteConfigBridge)
        every { RemoteConfigBridge.getAll() } returns mapOf("feature" to "remote_value")

        WickKitFlagsManager.setRcOverride(key = "feature", value = "override_value")

        val entry = WickKitFlagsManager.remoteConfigEntries.value.first()
        assertEquals("remote_value", entry.remoteValue)
        assertEquals("override_value", entry.overrideValue)
        assertTrue(entry.isOverrideEnabled)
        unmockkObject(RemoteConfigBridge)
    }

    @Test
    fun `remoteConfigEntries builds entry with no override`() {
        mockkObject(RemoteConfigBridge)
        every { RemoteConfigBridge.getAll() } returns mapOf("feature" to "remote_value")

        WickKitFlagsManager.clearRcOverride("feature")

        val entry = WickKitFlagsManager.remoteConfigEntries.value.first()
        assertEquals("remote_value", entry.remoteValue)
        assertEquals("", entry.overrideValue)
        assertFalse(entry.isOverrideEnabled)
        unmockkObject(RemoteConfigBridge)
    }

    @Test
    fun `remoteConfigEntries builds entry with disabled override`() {
        mockkObject(RemoteConfigBridge)
        every { RemoteConfigBridge.getAll() } returns mapOf("feature" to "remote_value")

        WickKitFlagsManager.setRcOverride(key = "feature", value = "override_value")
        WickKitFlagsManager.toggleRcOverride("feature")

        val entry = WickKitFlagsManager.remoteConfigEntries.value.first()
        assertEquals("override_value", entry.overrideValue)
        assertFalse(entry.isOverrideEnabled)
        unmockkObject(RemoteConfigBridge)
    }

    // ── reload() isRemoteConfigAvailable true branch ───────────────────────────

    @Test
    fun `reload calls loadRcEntries when remote config is available`() {
        mockkObject(RemoteConfigBridge)
        every { RemoteConfigBridge.isAvailable() } returns true
        every { RemoteConfigBridge.getAll() } returns mapOf("feature" to "remote_value")

        WickKitFlagsManager.init(context)

        assertEquals(1, WickKitFlagsManager.remoteConfigEntries.value.size)
        assertEquals("feature", WickKitFlagsManager.remoteConfigEntries.value.first().key)
        unmockkObject(RemoteConfigBridge)
    }

    // ── StateFlow access ───────────────────────────────────────────────────────

    @Test
    fun `sharedPreferencesFiles reflects override state after setSpOverride`() {
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("key", "original").commit()
        WickKitFlagsManager.init(context)

        WickKitFlagsManager.setSpOverride(
            prefsName = "test_prefs",
            key = "key",
            type = FlagType.STRING,
            value = "override",
        )

        val file = WickKitFlagsManager.sharedPreferencesFiles.value.find { it.name == "test_prefs" }
        assertNotNull(file)
        assertTrue(file!!.entries.first { it.key == "key" }.hasOverride)
    }

    // ── Type edge cases ────────────────────────────────────────────────────────

    @Test
    fun `sharedPreferencesFiles skips StringSet entries`() {
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("tags", setOf("a")).putString("name", "hello").commit()
        WickKitFlagsManager.init(context)

        val file = WickKitFlagsManager.sharedPreferencesFiles.value.find { it.name == "test_prefs" }
        assertNotNull(file)
        assertNull(file!!.entries.find { it.key == "tags" })
    }

    @Test
    fun `setSpOverride on missing key uses parameter type as backup type`() {
        WickKitFlagsManager.setSpOverride(
            prefsName = "test_prefs",
            key = "new_key",
            type = FlagType.STRING,
            value = "hello",
        )

        val wickkitPrefs = context.getSharedPreferences("wickkit_flags", Context.MODE_PRIVATE)
        assertEquals("STRING:", wickkitPrefs.getString("sp.backup.test_prefs.new_key", null))
    }

    @Test
    fun `setSpOverride with invalid int value uses 0 as fallback`() {
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("count", 5).commit()

        WickKitFlagsManager.setSpOverride(
            prefsName = "test_prefs",
            key = "count",
            type = FlagType.INT,
            value = "not_a_number",
        )

        assertEquals(0, prefs.getInt("count", -1))
    }

    @Test
    fun `setSpOverride with invalid long value uses 0 as fallback`() {
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("timeout", 1000L).commit()

        WickKitFlagsManager.setSpOverride(
            prefsName = "test_prefs",
            key = "timeout",
            type = FlagType.LONG,
            value = "not_a_number",
        )

        assertEquals(0L, prefs.getLong("timeout", -1L))
    }

    @Test
    fun `setSpOverride with invalid float value uses 0 as fallback`() {
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().putFloat("ratio", 0.5f).commit()

        WickKitFlagsManager.setSpOverride(
            prefsName = "test_prefs",
            key = "ratio",
            type = FlagType.FLOAT,
            value = "not_a_number",
        )

        assertEquals(0f, prefs.getFloat("ratio", -1f), 0.001f)
    }

    @Test
    fun `clearSpOverride with no backup when enabled does not crash`() {
        val wickkitPrefs = context.getSharedPreferences("wickkit_flags", Context.MODE_PRIVATE)
        wickkitPrefs.edit().putString("sp.enabled.test_prefs.key", "true").commit()

        WickKitFlagsManager.clearSpOverride(prefsName = "test_prefs", key = "key")

        assertFalse(wickkitPrefs.contains("sp.enabled.test_prefs.key"))
    }

    // ── Early-return guards (appContext null paths) ────────────────────────────

    private fun clearAppContext() {
        val field = WickKitFlagsManager::class.java.getDeclaredField("appContext")
        field.isAccessible = true
        field.set(WickKitFlagsManager, null)
    }

    @Test
    fun `setSpOverride is no-op when not initialized`() {
        clearAppContext()
        WickKitFlagsManager.setSpOverride(
            prefsName = "test_prefs",
            key = "key",
            type = FlagType.STRING,
            value = "value",
        )

        val wickkitPrefs = context.getSharedPreferences("wickkit_flags", Context.MODE_PRIVATE)
        assertFalse(wickkitPrefs.contains("sp.backup.test_prefs.key"))
    }

    @Test
    fun `toggleSpOverride is no-op when not initialized`() {
        clearAppContext()
        WickKitFlagsManager.toggleSpOverride(prefsName = "test_prefs", key = "key")
    }

    @Test
    fun `clearSpOverride is no-op when not initialized`() {
        clearAppContext()
        WickKitFlagsManager.clearSpOverride(prefsName = "test_prefs", key = "key")
    }

    @Test
    fun `setRcOverride is no-op when not initialized`() {
        clearAppContext()
        WickKitFlagsManager.setRcOverride(key = "key", value = "value")

        assertFalse(context.getSharedPreferences("wickkit_flags", Context.MODE_PRIVATE).contains("rc.override.key"))
    }

    @Test
    fun `toggleRcOverride is no-op when not initialized`() {
        clearAppContext()
        WickKitFlagsManager.toggleRcOverride("key")
    }

    @Test
    fun `clearRcOverride is no-op when not initialized`() {
        clearAppContext()
        WickKitFlagsManager.clearRcOverride("key")
    }

    @Test
    fun `getBoolean returns remote value when not initialized`() {
        clearAppContext()

        assertFalse(WickKitFlagsManager.getBoolean(key = "flag", remoteValue = false))
        assertTrue(WickKitFlagsManager.getBoolean(key = "flag", remoteValue = true))
    }
}
