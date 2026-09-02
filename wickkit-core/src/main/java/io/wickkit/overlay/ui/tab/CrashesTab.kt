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
import io.wickkit.crashes.CrashEntry
import io.wickkit.crashes.WickKitCrashManager
import io.wickkit.overlay.ui.WickKitTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CrashBadgeColor = Color(0xFFEF5350)
private val AnrBadgeColor = Color(0xFFFF9800)

internal enum class CrashFilter(val label: String) {
    ALL("All"),
    CRASH("Crash"),
    ANR("ANR"),
    ;

    fun matches(entry: CrashEntry): Boolean = when (this) {
        ALL -> true
        CRASH -> entry is CrashEntry.Crash
        ANR -> entry is CrashEntry.Anr
    }
}

@Composable
internal fun CrashesTab() {
    val entries by WickKitCrashManager.entries.collectAsState()
    var selectedEntry: CrashEntry? by remember { mutableStateOf(null) }

    BackHandler(enabled = selectedEntry != null) {
        selectedEntry = null
    }

    when (val entry = selectedEntry) {
        null -> CrashesTabContent(
            entries = entries,
            onEntryClick = { selectedEntry = it },
        )

        else -> CrashDetailScreen(
            entry = entry,
            onBack = { selectedEntry = null },
        )
    }
}

@Composable
private fun CrashesTabContent(
    entries: ImmutableList<CrashEntry>,
    onEntryClick: (CrashEntry) -> Unit,
) {
    var filter by remember { mutableStateOf(CrashFilter.ALL) }
    val filtered = remember(entries, filter) {
        if (filter == CrashFilter.ALL) {
            entries
        } else {
            entries.filter { filter.matches(it) }.toImmutableList()
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        CrashesToolbar(entries = entries)
        CrashesFilterBar(entries = entries, selected = filter, onSelect = { filter = it })
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        if (filtered.isEmpty()) {
            CrashesEmptyState()
        } else {
            CrashesList(entries = filtered, onEntryClick = onEntryClick)
        }
    }
}

@Composable
private fun CrashesToolbar(entries: ImmutableList<CrashEntry>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${entries.size} ${stringResource(R.string.wk_crashes_events)}",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CrashesFilterBar(
    entries: ImmutableList<CrashEntry>,
    selected: CrashFilter,
    onSelect: (CrashFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CrashFilter.entries.forEach { crashFilter ->
            val count = entries.count { crashFilter.matches(it) }
            val color = when (crashFilter) {
                CrashFilter.ALL -> MaterialTheme.colorScheme.onSurface
                CrashFilter.CRASH -> CrashBadgeColor
                CrashFilter.ANR -> AnrBadgeColor
            }
            CrashFilterChip(
                label = "${crashFilter.label} ($count)",
                isSelected = crashFilter == selected,
                color = color,
                onClick = { onSelect(crashFilter) },
            )
        }
    }
}

@Composable
private fun CrashFilterChip(
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
private fun CrashesList(
    entries: ImmutableList<CrashEntry>,
    onEntryClick: (CrashEntry) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = entries, key = { it.id }) { entry ->
            CrashEntryRow(entry = entry, onClick = { onEntryClick(entry) })
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        }
    }
}

@Composable
private fun CrashEntryRow(entry: CrashEntry, onClick: () -> Unit) {
    val anrLabel = stringResource(R.string.wk_crashes_badge_anr)
    val title = when (entry) {
        is CrashEntry.Crash -> entry.exceptionType.substringAfterLast('.')
        is CrashEntry.Anr -> anrLabel
    }
    val subtitle = when (entry) {
        is CrashEntry.Crash -> entry.message.ifEmpty { entry.exceptionType }
        is CrashEntry.Anr -> entry.description.ifEmpty { entry.processName }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null) { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CrashTypeBadge(entry = entry)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatTimestamp(entry.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
        )
    }
}

@Composable
private fun CrashTypeBadge(entry: CrashEntry) {
    val crashLabel = stringResource(R.string.wk_crashes_badge_crash)
    val anrLabel = stringResource(R.string.wk_crashes_badge_anr)
    val label = when (entry) {
        is CrashEntry.Crash -> crashLabel
        is CrashEntry.Anr -> anrLabel
    }
    val color = when (entry) {
        is CrashEntry.Crash -> CrashBadgeColor
        is CrashEntry.Anr -> AnrBadgeColor
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
        )
    }
}

@Composable
private fun CrashesEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.wk_crashes_empty_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Text(
                text = stringResource(R.string.wk_crashes_empty_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String = SimpleDateFormat(
    "MM-dd HH:mm",
    Locale.getDefault(),
).format(Date(timestamp))

private fun sampleEntries(): ImmutableList<CrashEntry> = persistentListOf(
    CrashEntry.Crash(
        id = 0L,
        timestamp = System.currentTimeMillis() - 60_000L,
        exceptionType = "java.lang.NullPointerException",
        message = "Attempt to invoke virtual method on a null object reference",
        stackTrace = persistentListOf(
            "io.wickkit.sample.MainActivity.onCreate(MainActivity.kt:42)",
            "android.app.Activity.performCreate(Activity.java:8050)",
        ),
        threadName = "main",
        appVersion = "1.3.2",
    ),
    CrashEntry.Anr(
        id = 1L,
        timestamp = System.currentTimeMillis() - 3_600_000L,
        description = "Input dispatching timed out (io.wickkit.sample/MainActivity)",
        trace = persistentListOf(
            "\"main\" prio=5 tid=1 Sleeping",
            "  at java.lang.Thread.sleep(Native Method)",
        ),
        processName = "io.wickkit.sample",
    ),
)

@PreviewLightDark
@Composable
private fun CrashesTabPreview() {
    WickKitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CrashesTabContent(entries = sampleEntries(), onEntryClick = {})
        }
    }
}

@PreviewLightDark
@Composable
private fun CrashesTabEmptyPreview() {
    WickKitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CrashesTabContent(entries = persistentListOf(), onEntryClick = {})
        }
    }
}
