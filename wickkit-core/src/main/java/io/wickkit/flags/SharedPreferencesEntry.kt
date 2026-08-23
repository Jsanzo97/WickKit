package io.wickkit.flags

import kotlinx.collections.immutable.ImmutableList

internal data class SharedPreferencesFileState(
    val name: String,
    val entries: ImmutableList<SharedPreferencesEntry>,
)

internal data class SharedPreferencesEntry(
    val key: String,
    val currentValue: String,
    val type: FlagType,
    val hasOverride: Boolean,
    val isOverrideEnabled: Boolean,
    val overrideValue: String,
    val backupValue: String,
)
