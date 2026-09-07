package io.wickkit.flags

import android.content.SharedPreferences

internal class FakeSharedPreferences : SharedPreferences {
    val data = mutableMapOf<String, String?>()

    override fun getString(key: String, defValue: String?): String? = data.getOrDefault(key, defValue)
    override fun edit(): SharedPreferences.Editor = FakeEditor(data)
    override fun contains(key: String): Boolean = data.containsKey(key)
    override fun getAll(): Map<String, *> = data
    override fun getInt(key: String, defValue: Int): Int = data[key]?.toIntOrNull() ?: defValue
    override fun getLong(key: String, defValue: Long): Long = data[key]?.toLongOrNull() ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = data[key]?.toFloatOrNull() ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = data[key]?.toBoolean() ?: defValue
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = defValues
    override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(
        l: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit
}

internal class FakeEditor(private val data: MutableMap<String, String?>) : SharedPreferences.Editor {
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
        pending.clear()
        toRemove.clear()
    }

    override fun putInt(key: String, value: Int): SharedPreferences.Editor = this
    override fun putLong(key: String, value: Long): SharedPreferences.Editor = this
    override fun putFloat(key: String, value: Float): SharedPreferences.Editor = this
    override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = this
    override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = this
}
