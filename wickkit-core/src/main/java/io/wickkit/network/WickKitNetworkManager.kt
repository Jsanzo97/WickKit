package io.wickkit.network

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

object WickKitNetworkManager {

    private const val MAX_ENTRIES = 500

    internal val entries: StateFlow<ImmutableList<NetworkEntry>>
        field = MutableStateFlow<PersistentList<NetworkEntry>>(persistentListOf())

    fun add(entry: NetworkEntry) {
        entries.update { current ->
            val base = if (current.size >= MAX_ENTRIES) current.removingAt(0) else current
            base.adding(entry)
        }
    }

    internal fun clear() {
        entries.value = persistentListOf()
    }
}
