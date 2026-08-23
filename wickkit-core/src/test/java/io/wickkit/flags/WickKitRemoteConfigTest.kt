package io.wickkit.flags

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WickKitRemoteConfigTest {

    // In-memory SharedPreferences — avoids Robolectric while still exercising real SP reads/writes.
    private val wickkitPrefs = MapSharedPreferences()

    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        wickkitPrefs.data.clear()
        every { context.applicationContext } returns context
        every { context.getSharedPreferences(any(), any()) } returns wickkitPrefs
        WickKitFlagsManager.init(context)
    }

    // region delegate passthrough (no override set)

    @Test
    fun `getBoolean returns delegate value when no override`() {
        val rc = WickKitRemoteConfig.wrap(context = context, firebaseRc = FakeRc(booleans = mapOf("flag" to true)))
        assertEquals(true, rc.getBoolean("flag"))
    }

    @Test
    fun `getString returns delegate value when no override`() {
        val rc = WickKitRemoteConfig.wrap(context = context, firebaseRc = FakeRc(strings = mapOf("theme" to "light")))
        assertEquals("light", rc.getString("theme"))
    }

    @Test
    fun `getLong returns delegate value when no override`() {
        val rc = WickKitRemoteConfig.wrap(context = context, firebaseRc = FakeRc(longs = mapOf("timeout" to 30L)))
        assertEquals(30L, rc.getLong("timeout"))
    }

    @Test
    fun `getDouble returns delegate value when no override`() {
        val rc = WickKitRemoteConfig.wrap(context = context, firebaseRc = FakeRc(doubles = mapOf("rate" to 1.5)))
        assertEquals(1.5, rc.getDouble("rate"), 0.001)
    }

    // endregion

    // region override applied

    @Test
    fun `getBoolean applies rc override when enabled`() {
        WickKitFlagsManager.setRcOverride(key = "new_checkout", value = "true")
        val rc = WickKitRemoteConfig.wrap(
            context = context,
            firebaseRc = FakeRc(booleans = mapOf("new_checkout" to false)),
        )
        assertEquals(true, rc.getBoolean("new_checkout"))
    }

    @Test
    fun `getString applies rc override when enabled`() {
        WickKitFlagsManager.setRcOverride(key = "theme", value = "dark")
        val rc = WickKitRemoteConfig.wrap(context = context, firebaseRc = FakeRc(strings = mapOf("theme" to "light")))
        assertEquals("dark", rc.getString("theme"))
    }

    @Test
    fun `getLong applies rc override when enabled`() {
        WickKitFlagsManager.setRcOverride(key = "max_items", value = "99")
        val rc = WickKitRemoteConfig.wrap(context = context, firebaseRc = FakeRc(longs = mapOf("max_items" to 20L)))
        assertEquals(99L, rc.getLong("max_items"))
    }

    @Test
    fun `getDouble applies rc override when enabled`() {
        WickKitFlagsManager.setRcOverride(key = "rate", value = "9.99")
        val rc = WickKitRemoteConfig.wrap(context = context, firebaseRc = FakeRc(doubles = mapOf("rate" to 1.0)))
        assertEquals(9.99, rc.getDouble("rate"), 0.001)
    }

    // endregion

    // region override lifecycle

    @Test
    fun `getBoolean returns delegate value after override is cleared`() {
        WickKitFlagsManager.setRcOverride(key = "flag", value = "true")
        WickKitFlagsManager.clearRcOverride("flag")
        val rc = WickKitRemoteConfig.wrap(context = context, firebaseRc = FakeRc(booleans = mapOf("flag" to false)))
        assertEquals(false, rc.getBoolean("flag"))
    }

    @Test
    fun `getBoolean returns delegate value after override is toggled off`() {
        WickKitFlagsManager.setRcOverride(key = "flag", value = "true")
        WickKitFlagsManager.toggleRcOverride("flag")
        val rc = WickKitRemoteConfig.wrap(context = context, firebaseRc = FakeRc(booleans = mapOf("flag" to false)))
        assertEquals(false, rc.getBoolean("flag"))
    }

    @Test
    fun `getBoolean applies override again after toggle on`() {
        WickKitFlagsManager.setRcOverride(key = "flag", value = "true")
        WickKitFlagsManager.toggleRcOverride("flag") // off
        WickKitFlagsManager.toggleRcOverride("flag") // on
        val rc = WickKitRemoteConfig.wrap(context = context, firebaseRc = FakeRc(booleans = mapOf("flag" to false)))
        assertEquals(true, rc.getBoolean("flag"))
    }

    // endregion

    // region invalid delegate

    @Test
    fun `getBoolean returns false when delegate has no matching method`() {
        val rc = WickKitRemoteConfig.wrap(context = context, firebaseRc = object {})
        assertEquals(false, rc.getBoolean("any"))
    }

    @Test
    fun `getString returns empty string when delegate has no matching method`() {
        val rc = WickKitRemoteConfig.wrap(context = context, firebaseRc = object {})
        assertEquals("", rc.getString("any"))
    }

    @Test
    fun `getLong returns zero when delegate has no matching method`() {
        val rc = WickKitRemoteConfig.wrap(context = context, firebaseRc = object {})
        assertEquals(0L, rc.getLong("any"))
    }

    @Test
    fun `getDouble returns zero when delegate has no matching method`() {
        val rc = WickKitRemoteConfig.wrap(context = context, firebaseRc = object {})
        assertEquals(0.0, rc.getDouble("any"), 0.0)
    }

    // endregion

    private class FakeRc(
        private val booleans: Map<String, Boolean> = emptyMap(),
        private val strings: Map<String, String> = emptyMap(),
        private val longs: Map<String, Long> = emptyMap(),
        private val doubles: Map<String, Double> = emptyMap(),
    ) {
        fun getBoolean(key: String): Boolean = booleans[key] ?: false
        fun getString(key: String): String = strings[key] ?: ""
        fun getLong(key: String): Long = longs[key] ?: 0L
        fun getDouble(key: String): Double = doubles[key] ?: 0.0
    }
}

private class MapSharedPreferences : SharedPreferences {
    val data = mutableMapOf<String, String?>()

    override fun getString(key: String, defValue: String?): String? = data.getOrDefault(key, defValue)
    override fun edit(): SharedPreferences.Editor = MapEditor(data)
    override fun contains(key: String): Boolean = data.containsKey(key)
    override fun getAll(): Map<String, *> = data
    override fun getInt(key: String, defValue: Int): Int = data[key]?.toIntOrNull() ?: defValue
    override fun getLong(key: String, defValue: Long): Long = data[key]?.toLongOrNull() ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = data[key]?.toFloatOrNull() ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = data[key]?.toBoolean() ?: defValue
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = defValues
    override fun registerOnSharedPreferenceChangeListener(
        l: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(
        l: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit
}

private class MapEditor(private val data: MutableMap<String, String?>) : SharedPreferences.Editor {
    private val pending = mutableMapOf<String, String?>()
    private val toRemove = mutableSetOf<String>()

    override fun putString(key: String, value: String?): SharedPreferences.Editor = apply { pending[key] = value }
    override fun remove(key: String): SharedPreferences.Editor = apply { toRemove.add(key) }
    override fun clear(): SharedPreferences.Editor = apply { data.clear() }
    override fun commit(): Boolean {
        apply()
        return true
    }
    override fun apply() {
        toRemove.forEach { data.remove(it) }
        data.putAll(pending)
    }

    override fun putInt(key: String, value: Int): SharedPreferences.Editor = this
    override fun putLong(key: String, value: Long): SharedPreferences.Editor = this
    override fun putFloat(key: String, value: Float): SharedPreferences.Editor = this
    override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = this
    override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = this
}
