package io.wickkit.overlay.ui.tab

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.wickkit.core.R
import io.wickkit.overlay.ui.WickKitTheme
import io.wickkit.threads.ThreadEntry
import io.wickkit.threads.WickKitThreadManager
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private enum class ThreadFilter(val label: String) {
    ALL("All"),
    RUNNABLE("Runnable"),
    WAITING("Waiting"),
    TIMED("Timed"),
    BLOCKED("Blocked"),
    NEW("New"),
    ;

    fun matches(state: Thread.State): Boolean = when (this) {
        ALL -> true
        RUNNABLE -> state == Thread.State.RUNNABLE
        WAITING -> state == Thread.State.WAITING
        TIMED -> state == Thread.State.TIMED_WAITING
        BLOCKED -> state == Thread.State.BLOCKED
        NEW -> state == Thread.State.NEW
    }
}

private val ThreadBlockedColor = Color(0xFFEF5350)
private val ThreadRunnableColor = Color(0xFF4CAF50)
private val ThreadWaitingColor = Color(0xFFFF9800)
private val ThreadTimedWaitingColor = Color(0xFFFFC107)
private val ThreadNewColor = Color(0xFF9E9E9E)

@Composable
internal fun ThreadsTab() {
    val entries by WickKitThreadManager.entries.collectAsState()
    var selectedEntry: ThreadEntry? by remember { mutableStateOf(null) }

    BackHandler(enabled = selectedEntry != null) {
        selectedEntry = null
    }

    when (val entry = selectedEntry) {
        null -> ThreadsTabContent(
            entries = entries,
            onEntryClick = { selectedEntry = it },
        )

        else -> ThreadDetailScreen(
            entry = entry,
            onBack = { selectedEntry = null },
        )
    }
}

@Composable
private fun ThreadsTabContent(
    entries: ImmutableList<ThreadEntry>,
    onEntryClick: (ThreadEntry) -> Unit,
) {
    var filter by remember { mutableStateOf(ThreadFilter.ALL) }
    val filtered = remember(entries, filter) {
        if (filter == ThreadFilter.ALL) {
            entries
        } else {
            entries.filter { filter.matches(it.state) }.toImmutableList()
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        ThreadsToolbar(entries = entries)
        ThreadsFilterBar(entries = entries, selected = filter, onSelect = { filter = it })
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        if (filtered.isEmpty()) {
            ThreadsEmptyState()
        } else {
            ThreadsList(entries = filtered, onEntryClick = onEntryClick)
        }
    }
}

@Composable
private fun ThreadsToolbar(entries: ImmutableList<ThreadEntry>) {
    val blockedCount = entries.count { it.state == Thread.State.BLOCKED }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${entries.size} ${stringResource(R.string.wk_threads_active)}",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (blockedCount > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ThreadBlockedColor.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "$blockedCount ${stringResource(R.string.wk_threads_blocked_warning)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ThreadBlockedColor,
                )
            }
        }
    }
}

@Composable
private fun ThreadsFilterBar(
    entries: ImmutableList<ThreadEntry>,
    selected: ThreadFilter,
    onSelect: (ThreadFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThreadFilter.entries.forEach { filter ->
            val count = entries.count { filter.matches(it.state) }
            val color = when (filter) {
                ThreadFilter.ALL -> MaterialTheme.colorScheme.onSurface
                ThreadFilter.RUNNABLE -> ThreadRunnableColor
                ThreadFilter.WAITING -> ThreadWaitingColor
                ThreadFilter.TIMED -> ThreadTimedWaitingColor
                ThreadFilter.BLOCKED -> ThreadBlockedColor
                ThreadFilter.NEW -> ThreadNewColor
            }
            ThreadFilterChip(
                label = "${filter.label} ($count)",
                isSelected = filter == selected,
                color = color,
                onClick = { onSelect(filter) },
            )
        }
    }
}

@Composable
private fun ThreadFilterChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (isSelected) color.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(interactionSource = null, indication = null) { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            ),
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        )
    }
}

@Composable
private fun ThreadsList(
    entries: ImmutableList<ThreadEntry>,
    onEntryClick: (ThreadEntry) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = entries, key = { it.id }) { entry ->
            ThreadEntryRow(entry = entry, onClick = { onEntryClick(entry) })
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        }
    }
}

@Composable
private fun ThreadEntryRow(entry: ThreadEntry, onClick: () -> Unit) {
    val typeLabel = if (entry.isDaemon) {
        stringResource(R.string.wk_threads_daemon)
    } else {
        stringResource(R.string.wk_threads_user)
    }
    val secondaryInfo = buildString {
        if (entry.threadGroup.isNotEmpty()) {
            append(entry.threadGroup)
            append(" · ")
        }
        append(typeLabel)
        append(" · p")
        append(entry.priority)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null) { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThreadStateBadge(state = entry.state)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = secondaryInfo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${entry.stackTrace.size}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
        )
    }
}

@Composable
private fun ThreadStateBadge(state: Thread.State) {
    val color = stateColor(state)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = stateLabel(state),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
        )
    }
}

@Composable
private fun ThreadsEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.wk_threads_empty_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Text(
                text = stringResource(R.string.wk_threads_empty_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
        }
    }
}

private fun stateColor(state: Thread.State): Color = when (state) {
    Thread.State.RUNNABLE -> ThreadRunnableColor
    Thread.State.BLOCKED -> ThreadBlockedColor
    Thread.State.WAITING -> ThreadWaitingColor
    Thread.State.TIMED_WAITING -> ThreadTimedWaitingColor
    Thread.State.NEW, Thread.State.TERMINATED -> ThreadNewColor
}

private fun stateLabel(state: Thread.State): String = when (state) {
    Thread.State.RUNNABLE -> "RUNNABLE"
    Thread.State.BLOCKED -> "BLOCKED"
    Thread.State.WAITING -> "WAITING"
    Thread.State.TIMED_WAITING -> "TIMED"
    Thread.State.NEW -> "NEW"
    Thread.State.TERMINATED -> "DONE"
}

private fun sampleEntries(): ImmutableList<ThreadEntry> = persistentListOf(
    ThreadEntry(
        id = 0,
        name = "main",
        state = Thread.State.RUNNABLE,
        isDaemon = false,
        priority = 5,
        threadGroup = "main",
        stackTrace = persistentListOf(
            "io.wickkit.sample.MainActivity.onCreate(MainActivity.kt:42)",
            "android.app.Activity.performCreate(Activity.java:8050)",
        ),
    ),
    ThreadEntry(
        id = 1,
        name = "DatabaseWorker-1",
        state = Thread.State.BLOCKED,
        isDaemon = true,
        priority = 5,
        threadGroup = "main",
        stackTrace = persistentListOf(
            "java.lang.Object.wait(Native Method)",
            "io.wickkit.sample.db.DatabaseHelper.query(DatabaseHelper.kt:88)",
        ),
    ),
    ThreadEntry(
        id = 2,
        name = "OkHttp ConnectionPool",
        state = Thread.State.TIMED_WAITING,
        isDaemon = true,
        priority = 5,
        threadGroup = "main",
        stackTrace = persistentListOf("java.lang.Thread.sleep(Native Method)"),
    ),
)

@PreviewLightDark
@Composable
private fun ThreadsTabPreview() {
    WickKitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ThreadsTabContent(entries = sampleEntries(), onEntryClick = {})
        }
    }
}

@PreviewLightDark
@Composable
private fun ThreadsTabEmptyPreview() {
    WickKitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ThreadsTabContent(entries = persistentListOf(), onEntryClick = {})
        }
    }
}
