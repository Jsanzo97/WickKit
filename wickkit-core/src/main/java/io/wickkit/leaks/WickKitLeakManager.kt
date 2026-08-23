package io.wickkit.leaks

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

internal object WickKitLeakManager {

    private const val MAX_ENTRIES = 500
    private val idCounter = AtomicLong(0)

    val entries: StateFlow<ImmutableList<LeakEntry>>
        field = MutableStateFlow<PersistentList<LeakEntry>>(persistentListOf())

    fun add(className: String, detectedAt: String) {
        entries.update { current ->
            val existingIndex = current.indexOfFirst { it.className == className }
            if (existingIndex >= 0) {
                val existing = current[existingIndex]
                current.replacingAt(
                    existingIndex,
                    existing.copy(
                        instanceCount = existing.instanceCount + 1,
                        detectedAt = detectedAt,
                    ),
                )
            } else {
                (if (current.size >= MAX_ENTRIES) current.removingAt(0) else current).adding(
                    LeakEntry(
                        id = idCounter.getAndIncrement(),
                        className = className,
                        detectedAt = detectedAt,
                        instanceCount = 1,
                    ),
                )
            }
        }
    }

    fun clear() {
        entries.value = persistentListOf()
    }
}
