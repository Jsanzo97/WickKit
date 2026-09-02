package io.wickkit.flags

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

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

    internal val isRemoteConfigAvailable: Boolean get() = RemoteConfigBridge.isAvailable()

    internal fun init(context: Context) {
        appContext = context.applicationContext
        reload()
    }

    private fun reload() {
        val context = appContext ?: return
        loadSpFiles(context)
        if (isRemoteConfigAvailable) loadRcEntries(context)
    }

    // ── SharedPreferences ────────────────────────────────────────────────────────

    private fun loadSpFiles(context: Context) {
        val wickkitPrefs = wickkitPrefs(context)
        sharedPreferencesFiles.value = SharedPrefsDiscovery.discoverNames(
            File(context.applicationInfo.dataDir, "shared_prefs"),
        )
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
            .mapNotNull { (key, value) ->
                buildSharedPreferencesEntry(
                    wickkitPrefs = wickkitPrefs,
                    prefsName = name,
                    key = key,
                    value = value,
                )
            }
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
        val hasOverride = wickkitPrefs.contains(spBackupKey(prefsName = prefsName, key = key))
        val overrideEncoded = wickkitPrefs.getString(spOverrideKey(prefsName = prefsName, key = key), null)
        val backupEncoded = wickkitPrefs.getString(spBackupKey(prefsName = prefsName, key = key), null)
        val isEnabled = wickkitPrefs.getString(spEnabledKey(prefsName = prefsName, key = key), null) == "true"
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

    internal fun setSpOverride(
        prefsName: String,
        key: String,
        type: FlagType,
        value: String,
    ) {
        val context = appContext ?: return
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val wickkitPrefs = wickkitPrefs(context)
        if (!wickkitPrefs.contains(spBackupKey(prefsName = prefsName, key = key))) {
            val original = prefs.all[key]
            val originalType = typeOf(original) ?: type
            wickkitPrefs.edit {
                putString(
                    spBackupKey(prefsName = prefsName, key = key),
                    encode(type = originalType, value = original?.toString() ?: ""),
                )
            }
        }
        wickkitPrefs.edit()
            .putString(spOverrideKey(prefsName = prefsName, key = key), encode(type = type, value = value))
            .putString(spEnabledKey(prefsName = prefsName, key = key), "true")
            .apply()
        prefs.edit().also { editor ->
            writeTyped(
                editor = editor,
                key = key,
                value = value,
                type = type,
            )
        }.apply()
        loadSpFiles(context)
    }

    internal fun toggleSpOverride(prefsName: String, key: String) {
        val context = appContext ?: return
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val wickkitPrefs = wickkitPrefs(context)
        val isEnabled = wickkitPrefs.getString(spEnabledKey(prefsName = prefsName, key = key), null) == "true"
        if (isEnabled) {
            val backupEncoded = wickkitPrefs.getString(spBackupKey(prefsName = prefsName, key = key), null) ?: return
            val (backupType, backupValue) = decode(backupEncoded)
            prefs.edit().also { editor ->
                writeTyped(
                    editor = editor,
                    key = key,
                    value = backupValue,
                    type = backupType,
                )
            }.apply()
            wickkitPrefs.edit { putString(spEnabledKey(prefsName = prefsName, key = key), "false") }
        } else {
            val overrideEncoded = wickkitPrefs.getString(
                spOverrideKey(prefsName = prefsName, key = key),
                null,
            ) ?: return
            val (overrideType, overrideValue) = decode(overrideEncoded)
            prefs.edit().also { editor ->
                writeTyped(
                    editor = editor,
                    key = key,
                    value = overrideValue,
                    type = overrideType,
                )
            }.apply()
            wickkitPrefs.edit { putString(spEnabledKey(prefsName = prefsName, key = key), "true") }
        }
        loadSpFiles(context)
    }

    internal fun clearSpOverride(prefsName: String, key: String) {
        val context = appContext ?: return
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val wickkitPrefs = wickkitPrefs(context)
        val isEnabled = wickkitPrefs.getString(spEnabledKey(prefsName = prefsName, key = key), null) == "true"
        if (isEnabled) {
            val backupEncoded = wickkitPrefs.getString(spBackupKey(prefsName = prefsName, key = key), null)
            if (backupEncoded != null) {
                val (backupType, backupValue) = decode(backupEncoded)
                prefs.edit().also { editor ->
                    writeTyped(
                        editor = editor,
                        key = key,
                        value = backupValue,
                        type = backupType,
                    )
                }.apply()
            }
        }
        wickkitPrefs.edit {
            remove(spBackupKey(prefsName = prefsName, key = key))
                .remove(spOverrideKey(prefsName = prefsName, key = key))
                .remove(spEnabledKey(prefsName = prefsName, key = key))
        }
        loadSpFiles(context)
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
        val context = appContext ?: return
        wickkitPrefs(context).edit {
            putString(RC_OVERRIDE_PREFIX + key, value)
                .putString(RC_ENABLED_PREFIX + key, "true")
        }
        loadRcEntries(context)
    }

    internal fun toggleRcOverride(key: String) {
        val context = appContext ?: return
        val prefs = wickkitPrefs(context)
        val isEnabled = prefs.getString(RC_ENABLED_PREFIX + key, null) == "true"
        prefs.edit {
            putString(RC_ENABLED_PREFIX + key, if (isEnabled) "false" else "true")
        }
        loadRcEntries(context)
    }

    internal fun clearRcOverride(key: String) {
        val context = appContext ?: return
        wickkitPrefs(context).edit {
            remove(RC_OVERRIDE_PREFIX + key)
                .remove(RC_ENABLED_PREFIX + key)
        }
        loadRcEntries(context)
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
        val context = appContext ?: return null
        val prefs = wickkitPrefs(context)
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
        val separatorIndex = encoded.indexOf(':')
        return FlagType.valueOf(encoded.substring(0, separatorIndex)) to encoded.substring(separatorIndex + 1)
    }

    private fun writeTyped(
        editor: SharedPreferences.Editor,
        key: String,
        value: String,
        type: FlagType,
    ) {
        when (type) {
            FlagType.BOOLEAN -> editor.putBoolean(key, value.lowercase() == "true")
            FlagType.STRING -> editor.putString(key, value)
            FlagType.INT -> editor.putInt(key, value.toIntOrNull() ?: 0)
            FlagType.LONG -> editor.putLong(key, value.toLongOrNull() ?: 0L)
            FlagType.FLOAT -> editor.putFloat(key, value.toFloatOrNull() ?: 0f)
        }
    }
}
