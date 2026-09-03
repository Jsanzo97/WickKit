package io.wickkit.flags

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    // region Remote Config active override

    @Test
    fun `getActiveRcOverride returns null when no override`() {
        assertNull(WickKitFlagsManager.getActiveRcOverride("flag"))
    }

    @Test
    fun `getActiveRcOverride returns override value when active`() {
        WickKitFlagsManager.setRcOverride(key = "flag", value = "true")

        assertEquals("true", WickKitFlagsManager.getActiveRcOverride("flag"))
    }

    @Test
    fun `getActiveRcOverride returns null when override is disabled`() {
        WickKitFlagsManager.setRcOverride(key = "flag", value = "true")
        WickKitFlagsManager.toggleRcOverride("flag")

        assertNull(WickKitFlagsManager.getActiveRcOverride("flag"))
    }

    @Test
    fun `getActiveRcOverride returns string override when active`() {
        WickKitFlagsManager.setRcOverride(key = "title", value = "hello")

        assertEquals("hello", WickKitFlagsManager.getActiveRcOverride("title"))
    }

    @Test
    fun `getActiveRcOverride returns numeric string override when active`() {
        WickKitFlagsManager.setRcOverride(key = "timeout", value = "5000")

        assertEquals("5000", WickKitFlagsManager.getActiveRcOverride("timeout"))
    }

    @Test
    fun `getActiveRcOverride returns decimal string override when active`() {
        WickKitFlagsManager.setRcOverride(key = "ratio", value = "0.75")

        assertEquals("0.75", WickKitFlagsManager.getActiveRcOverride("ratio"))
    }

    @Test
    fun `clearRcOverride removes active override`() {
        WickKitFlagsManager.setRcOverride(key = "flag", value = "true")
        WickKitFlagsManager.clearRcOverride("flag")

        assertNull(WickKitFlagsManager.getActiveRcOverride("flag"))
    }

    @Test
    fun `toggleRcOverride re-enables a disabled override`() {
        WickKitFlagsManager.setRcOverride(key = "flag", value = "true")
        WickKitFlagsManager.toggleRcOverride("flag") // disable
        WickKitFlagsManager.toggleRcOverride("flag") // re-enable

        assertEquals("true", WickKitFlagsManager.getActiveRcOverride("flag"))
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
    fun `getActiveRcOverride returns null when not initialized`() {
        clearAppContext()

        assertNull(WickKitFlagsManager.getActiveRcOverride("flag"))
    }

    // endregion

    private fun clearAppContext() {
        val field = WickKitFlagsManager::class.java.getDeclaredField("appContext")
        field.isAccessible = true
        field.set(WickKitFlagsManager, null)
    }
}
