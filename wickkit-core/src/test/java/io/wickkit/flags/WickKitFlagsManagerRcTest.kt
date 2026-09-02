package io.wickkit.flags

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WickKitFlagsManagerRcTest {

    private val wickkitPrefs = FakeSharedPreferences()
    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        wickkitPrefs.data.clear()
        every { context.applicationContext } returns context
        every { context.getSharedPreferences(any(), any()) } returns wickkitPrefs
        WickKitFlagsManager.init(context)
    }

    // region Remote Config public API

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

    // endregion

    // region Remote Config entry building

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

    // endregion

    // region Early-return guards (appContext null paths)

    @Test
    fun `setRcOverride is no-op when not initialized`() {
        clearAppContext()
        WickKitFlagsManager.setRcOverride(key = "key", value = "value")

        assertFalse(wickkitPrefs.contains("rc.override.key"))
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

    // endregion

    private fun clearAppContext() {
        val field = WickKitFlagsManager::class.java.getDeclaredField("appContext")
        field.isAccessible = true
        field.set(WickKitFlagsManager, null)
    }
}
