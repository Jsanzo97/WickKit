package io.wickkit.network

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

object WickKitNetworkManager {

    private const val MAX_ENTRIES = 100

    internal val entries: StateFlow<ImmutableList<NetworkEntry>>
        field = MutableStateFlow<PersistentList<NetworkEntry>>(persistentListOf())

    fun add(entry: NetworkEntry) {
        entries.update { current ->
            (if (current.size >= MAX_ENTRIES) current.removingAt(0) else current).adding(entry)
        }
    }

    internal fun clear() {
        entries.value = persistentListOf()
    }
}
