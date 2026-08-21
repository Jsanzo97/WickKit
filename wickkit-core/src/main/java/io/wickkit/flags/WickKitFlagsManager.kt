package io.wickkit.flags

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object WickKitFlagsManager {

    private const val WICKKIT_PREFS = "wickkit_flags"
    private const val SP_BACKUP_PREFIX = "sp.backup."
    private const val SP_OVERRIDE_PREFIX = "sp.override."
    private const val SP_ENABLED_PREFIX = "sp.enabled."
    private const val RC_OVERRIDE_PREFIX = "rc.override."
    private const val RC_ENABLED_PREFIX = "rc.enabled."

    @Volatile private var appContext: Context? = null

    internal val sharedPreferencesFiles: StateFlow<ImmutableList<SharedPreferencesFileState>>
        field = MutableStateFlow<ImmutableList<SharedPreferencesFileState>>(persistentListOf())

    internal val remoteConfigEntries: StateFlow<ImmutableList<RemoteConfigEntry>>
        field = MutableStateFlow<ImmutableList<RemoteConfigEntry>>(persistentListOf())

    val isRemoteConfigAvailable: Boolean get() = RemoteConfigBridge.isAvailable()

    internal fun init(context: Context) {
        appContext = context.applicationContext
        reload()
    }

    private fun reload() {
        val ctx = appContext ?: return
        loadSpFiles(ctx)
        if (isRemoteConfigAvailable) loadRcEntries(ctx)
    }

    // ── SharedPreferences ────────────────────────────────────────────────────────

    private fun loadSpFiles(context: Context) {
        val wickkitPrefs = wickkitPrefs(context)
        sharedPreferencesFiles.value = SharedPrefsDiscovery.discoverNames(context)
            .map { name -> buildSharedPreferencesFileState(context, wickkitPrefs, name) }
            .toImmutableList()
    }

    private fun buildSharedPreferencesFileState(
        context: Context,
        wickkitPrefs: SharedPreferences,
        name: String,
    ): SharedPreferencesFileState {
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val entries = prefs.all.entries
            .sortedBy { it.key }
            .mapNotNull { (key, value) -> buildSharedPreferencesEntry(wickkitPrefs, name, key, value) }
            .toImmutableList()
        return SharedPreferencesFileState(name = name, entries = entries)
    }

    private fun buildSharedPreferencesEntry(
        wickkitPrefs: SharedPreferences,
        prefsName: String,
        key: String,
        value: Any?,
    ): SharedPreferencesEntry? {
        val type = typeOf(value) ?: return null
        val hasOverride = wickkitPrefs.contains(spBackupKey(prefsName, key))
        val overrideEncoded = wickkitPrefs.getString(spOverrideKey(prefsName, key), null)
        val backupEncoded = wickkitPrefs.getString(spBackupKey(prefsName, key), null)
        val isEnabled = wickkitPrefs.getString(spEnabledKey(prefsName, key), null) == "true"
        return SharedPreferencesEntry(
            key = key,
            currentValue = value.toString(),
            type = type,
            hasOverride = hasOverride,
            isOverrideEnabled = hasOverride && isEnabled,
            overrideValue = overrideEncoded?.let { decode(it).second } ?: "",
            backupValue = backupEncoded?.let { decode(it).second } ?: "",
        )
    }

    internal fun setSpOverride(prefsName: String, key: String, type: FlagType, value: String) {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val wickkitPrefs = wickkitPrefs(ctx)
        if (!wickkitPrefs.contains(spBackupKey(prefsName, key))) {
            val original = prefs.all[key]
            val originalType = typeOf(original) ?: type
            wickkitPrefs.edit {
                putString(
                    spBackupKey(prefsName, key),
                    encode(originalType, original?.toString() ?: ""),
                )
            }
        }
        wickkitPrefs.edit()
            .putString(spOverrideKey(prefsName, key), encode(type, value))
            .putString(spEnabledKey(prefsName, key), "true")
            .apply()
        prefs.edit().also { writeTyped(it, key, value, type) }.apply()
        loadSpFiles(ctx)
    }

    internal fun toggleSpOverride(prefsName: String, key: String) {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val wickkitPrefs = wickkitPrefs(ctx)
        val isEnabled = wickkitPrefs.getString(spEnabledKey(prefsName, key), null) == "true"
        if (isEnabled) {
            val backupEncoded = wickkitPrefs.getString(spBackupKey(prefsName, key), null) ?: return
            val (backupType, backupValue) = decode(backupEncoded)
            prefs.edit().also { writeTyped(it, key, backupValue, backupType) }.apply()
            wickkitPrefs.edit { putString(spEnabledKey(prefsName, key), "false") }
        } else {
            val overrideEncoded = wickkitPrefs.getString(spOverrideKey(prefsName, key), null) ?: return
            val (overrideType, overrideValue) = decode(overrideEncoded)
            prefs.edit().also { writeTyped(it, key, overrideValue, overrideType) }.apply()
            wickkitPrefs.edit { putString(spEnabledKey(prefsName, key), "true") }
        }
        loadSpFiles(ctx)
    }

    internal fun clearSpOverride(prefsName: String, key: String) {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val wickkitPrefs = wickkitPrefs(ctx)
        val isEnabled = wickkitPrefs.getString(spEnabledKey(prefsName, key), null) == "true"
        if (isEnabled) {
            val backupEncoded = wickkitPrefs.getString(spBackupKey(prefsName, key), null)
            if (backupEncoded != null) {
                val (backupType, backupValue) = decode(backupEncoded)
                prefs.edit().also { writeTyped(it, key, backupValue, backupType) }.apply()
            }
        }
        wickkitPrefs.edit {
            remove(spBackupKey(prefsName, key))
                .remove(spOverrideKey(prefsName, key))
                .remove(spEnabledKey(prefsName, key))
        }
        loadSpFiles(ctx)
    }

    // ── Remote Config ─────────────────────────────────────────────────────────────

    private fun loadRcEntries(context: Context) {
        val wickkitPrefs = wickkitPrefs(context)
        remoteConfigEntries.value = RemoteConfigBridge.getAll()
            .entries
            .sortedBy { it.key }
            .map { (key, remoteValue) ->
                val overrideValue = wickkitPrefs.getString(RC_OVERRIDE_PREFIX + key, null)
                val isEnabled = wickkitPrefs.getString(RC_ENABLED_PREFIX + key, null) == "true"
                RemoteConfigEntry(
                    key = key,
                    remoteValue = remoteValue,
                    overrideValue = overrideValue ?: "",
                    isOverrideEnabled = overrideValue != null && isEnabled,
                )
            }
            .toImmutableList()
    }

    internal fun setRcOverride(key: String, value: String) {
        val ctx = appContext ?: return
        wickkitPrefs(ctx).edit {
            putString(RC_OVERRIDE_PREFIX + key, value)
                .putString(RC_ENABLED_PREFIX + key, "true")
        }
        loadRcEntries(ctx)
    }

    internal fun toggleRcOverride(key: String) {
        val ctx = appContext ?: return
        val prefs = wickkitPrefs(ctx)
        val isEnabled = prefs.getString(RC_ENABLED_PREFIX + key, null) == "true"
        prefs.edit {
            putString(RC_ENABLED_PREFIX + key, if (isEnabled) "false" else "true")
        }
        loadRcEntries(ctx)
    }

    internal fun clearRcOverride(key: String) {
        val ctx = appContext ?: return
        wickkitPrefs(ctx).edit {
            remove(RC_OVERRIDE_PREFIX + key)
                .remove(RC_ENABLED_PREFIX + key)
        }
        loadRcEntries(ctx)
    }

    // ── Public API for app code ───────────────────────────────────────────────────

    fun getBoolean(
        key: String,
        remoteValue: Boolean,
    ): Boolean = activeRcOverride(key)?.lowercase()?.let { it == "true" } ?: remoteValue

    fun getString(
        key: String,
        remoteValue: String,
    ): String = activeRcOverride(key) ?: remoteValue

    fun getLong(
        key: String,
        remoteValue: Long,
    ): Long = activeRcOverride(key)?.toLongOrNull() ?: remoteValue

    fun getDouble(
        key: String,
        remoteValue: Double,
    ): Double = activeRcOverride(key)?.toDoubleOrNull() ?: remoteValue

    private fun activeRcOverride(key: String): String? {
        val ctx = appContext ?: return null
        val prefs = wickkitPrefs(ctx)
        val isEnabled = prefs.getString(RC_ENABLED_PREFIX + key, null) == "true"
        return if (isEnabled) prefs.getString(RC_OVERRIDE_PREFIX + key, null) else null
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private fun wickkitPrefs(
        context: Context,
    ): SharedPreferences = context.getSharedPreferences(WICKKIT_PREFS, Context.MODE_PRIVATE)

    private fun spBackupKey(prefsName: String, key: String) = "$SP_BACKUP_PREFIX$prefsName.$key"
    private fun spOverrideKey(prefsName: String, key: String) = "$SP_OVERRIDE_PREFIX$prefsName.$key"
    private fun spEnabledKey(prefsName: String, key: String) = "$SP_ENABLED_PREFIX$prefsName.$key"

    private fun typeOf(value: Any?): FlagType? = when (value) {
        is Boolean -> FlagType.BOOLEAN
        is String -> FlagType.STRING
        is Int -> FlagType.INT
        is Long -> FlagType.LONG
        is Float -> FlagType.FLOAT
        else -> null
    }

    private fun encode(type: FlagType, value: String) = "${type.name}:$value"

    private fun decode(encoded: String): Pair<FlagType, String> {
        val colon = encoded.indexOf(':')
        return FlagType.valueOf(encoded.substring(0, colon)) to encoded.substring(colon + 1)
    }

    private fun writeTyped(editor: SharedPreferences.Editor, key: String, value: String, type: FlagType) {
        when (type) {
            FlagType.BOOLEAN -> editor.putBoolean(key, value.lowercase() == "true")
            FlagType.STRING -> editor.putString(key, value)
            FlagType.INT -> editor.putInt(key, value.toIntOrNull() ?: 0)
            FlagType.LONG -> editor.putLong(key, value.toLongOrNull() ?: 0L)
            FlagType.FLOAT -> editor.putFloat(key, value.toFloatOrNull() ?: 0f)
        }
    }
}
