package io.wickkit.logs

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

internal object WickKitLogStore {

    private const val MAX_ENTRIES = 500
    private val idCounter = AtomicLong(0)

    private val _entries = MutableStateFlow<PersistentList<LogEntry>>(persistentListOf())
    val entries: StateFlow<ImmutableList<LogEntry>> = _entries.asStateFlow()

    private val _selectedLevels = MutableStateFlow(LogLevel.entries.toSet())
    val selectedLevels: StateFlow<Set<LogLevel>> = _selectedLevels.asStateFlow()

    fun add(level: LogLevel, tag: String, message: String, time: String) {
        val entry = LogEntry(
            id = idCounter.getAndIncrement(),
            level = level,
            tag = tag,
            message = message,
            time = time,
        )
        _entries.update { current ->
            val base = if (current.size >= MAX_ENTRIES) current.removeAt(0) else current
            base.add(entry)
        }
    }

    fun toggleLevel(level: LogLevel) {
        _selectedLevels.update { current ->
            if (level in current) current - level else current + level
        }
    }

    fun clear() {
        _entries.value = persistentListOf()
    }
}
