package io.wickkit.flags

internal data class RemoteConfigEntry(
    val key: String,
    val remoteValue: String,
    val overrideValue: String,
    val isOverrideEnabled: Boolean,
)
