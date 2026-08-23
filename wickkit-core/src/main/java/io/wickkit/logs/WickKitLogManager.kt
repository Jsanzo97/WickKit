package io.wickkit.logs

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

internal object WickKitLogManager {

    private const val MAX_ENTRIES = 500
    private val idCounter = AtomicLong(0)

    val entries: StateFlow<ImmutableList<LogEntry>>
        field = MutableStateFlow<PersistentList<LogEntry>>(persistentListOf())

    val selectedLevels: StateFlow<Set<LogLevel>>
        field = MutableStateFlow(LogLevel.entries.toSet())

    fun add(
        level: LogLevel,
        tag: String,
        message: String,
        time: String,
    ) {
        val entry = LogEntry(
            id = idCounter.getAndIncrement(),
            level = level,
            tag = tag,
            message = message,
            time = time,
        )
        entries.update { current ->
            (if (current.size >= MAX_ENTRIES) current.removingAt(0) else current).adding(entry)
        }
    }

    fun toggleLevel(level: LogLevel) {
        selectedLevels.update { current ->
            if (level in current) current - level else current + level
        }
    }

    fun clear() {
        entries.value = persistentListOf()
    }
}
