package io.wickkit.threads

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal object WickKitThreadManager {

    private const val POLL_INTERVAL_MS = 2_000L

    val entries: StateFlow<ImmutableList<ThreadEntry>>
        field = MutableStateFlow<PersistentList<ThreadEntry>>(persistentListOf())

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            while (true) {
                refresh()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun refresh() {
        entries.value = buildEntries(Thread.getAllStackTraces())
    }

    fun clear() {
        entries.value = persistentListOf()
    }

    internal fun buildEntries(
        rawData: Map<Thread, Array<StackTraceElement>>,
    ): PersistentList<ThreadEntry> = rawData.entries
        .mapNotNull { (thread, stackTrace) ->
            val state = thread.state
            if (state == Thread.State.TERMINATED) {
                null
            } else {
                ThreadEntry(
                    id = 0L,
                    name = thread.name.orEmpty(),
                    state = state,
                    isDaemon = thread.isDaemon,
                    priority = thread.priority,
                    threadGroup = thread.threadGroup?.name.orEmpty(),
                    stackTrace = stackTrace.map { it.toString() }.toPersistentList(),
                )
            }
        }
        .sortedWith(compareBy({ statePriority(it.state) }, { it.name }))
        .mapIndexed { index, entry -> entry.copy(id = index.toLong()) }
        .toPersistentList()

    private fun statePriority(state: Thread.State): Int = when (state) {
        Thread.State.BLOCKED -> 0
        Thread.State.RUNNABLE -> 1
        Thread.State.WAITING -> 2
        Thread.State.TIMED_WAITING -> 3
        Thread.State.NEW -> 4
        Thread.State.TERMINATED -> 5
    }
}
