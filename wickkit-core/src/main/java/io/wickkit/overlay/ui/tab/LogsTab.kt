package io.wickkit.overlay.ui.tab

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.wickkit.core.R
import io.wickkit.logs.LogEntry
import io.wickkit.logs.LogLevel
import io.wickkit.logs.LogNoise
import io.wickkit.logs.WickKitLogManager
import io.wickkit.logs.matches
import io.wickkit.logs.parseLogFilter
import io.wickkit.overlay.ui.WickKitTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet

private val ToolbarHeight = 36.dp

private object LogsTabState {
    var search: String = ""
    var showAll: Boolean = false
}

@Composable
internal fun LogsTab() {
    val entries by WickKitLogManager.entries.collectAsState()
    LogsTabContent(entries = entries)
}

@Composable
private fun LogsTabContent(entries: ImmutableList<LogEntry>) {
    var search by remember { mutableStateOf(LogsTabState.search) }
    var showAll by remember { mutableStateOf(LogsTabState.showAll) }
    val selectedLevels by WickKitLogManager.selectedLevels.collectAsState()
    val context = LocalContext.current

    val filtered: ImmutableList<LogEntry> = remember(entries, search, selectedLevels, showAll) {
        val filter = parseLogFilter(search)
        val noisyTags = if (!showAll) LogNoise.noisyTags(entries) else emptySet()
        entries.filter { entry ->
            entry.level in selectedLevels &&
                entry.matches(filter) &&
                (showAll || (!LogNoise.isSystemTag(entry.tag) && entry.tag !in noisyTags))
        }.toImmutableList()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LogsToolbar(
            search = search,
            showAll = showAll,
            onSearch = {
                search = it
                LogsTabState.search = it
            },
            onShowAllChange = {
                showAll = it
                LogsTabState.showAll = it
            },
            onClear = { WickKitLogManager.clear() },
            onShare = { shareLogEntries(context = context, entries = filtered) },
        )
        LevelFilterRow(
            enabledLevels = selectedLevels.toPersistentSet(),
            onToggle = { WickKitLogManager.toggleLevel(it) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        if (filtered.isEmpty()) {
            LogsEmptyState(hasEntries = entries.isNotEmpty())
        } else {
            LogsList(
                entries = filtered,
                onLongClick = { entry -> copyEntryToClipboard(context = context, entry = entry) },
            )
        }
    }
}

@Composable
private fun LogsToolbar(
    search: String,
    showAll: Boolean,
    onSearch: (String) -> Unit,
    onShowAllChange: (Boolean) -> Unit,
    onClear: () -> Unit,
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(ToolbarHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (search.isEmpty()) {
                Text(
                    text = stringResource(R.string.wk_logs_search_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                )
            }
            BasicTextField(
                value = search,
                onValueChange = onSearch,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        AppAllToggle(showAll = showAll, onShowAllChange = onShowAllChange)
        Box(
            modifier = Modifier
                .size(ToolbarHeight)
                .clip(RoundedCornerShape(8.dp))
                .clickable(interactionSource = null, indication = null) { onShare() }
                .padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = stringResource(R.string.wk_cd_share_logs),
                modifier = Modifier.fillMaxSize(),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Box(
            modifier = Modifier
                .height(ToolbarHeight)
                .clip(RoundedCornerShape(8.dp))
                .clickable(interactionSource = null, indication = null) { onClear() }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.wk_clear),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun AppAllToggle(showAll: Boolean, onShowAllChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .height(ToolbarHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            false to stringResource(R.string.wk_logs_filter_app),
            true to stringResource(R.string.wk_logs_filter_all),
        ).forEach { (isAll, label) ->
            val active = showAll == isAll
            Box(
                modifier = Modifier
                    .height(ToolbarHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (active) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        } else {
                            Color.Transparent
                        },
                    )
                    .clickable(interactionSource = null, indication = null) { onShowAllChange(isAll) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    ),
                    color = if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    },
                )
            }
        }
    }
}

@Composable
private fun LevelFilterRow(
    enabledLevels: ImmutableSet<LogLevel>,
    onToggle: (LogLevel) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LogLevel.entries.forEach { level ->
            LevelChip(
                level = level,
                selected = level in enabledLevels,
                onClick = { onToggle(level) },
            )
        }
    }
}

@Composable
private fun LevelChip(
    level: LogLevel,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = level.badgeColor()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) color.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) color.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(interactionSource = null, indication = null) { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = level.chipLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (selected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        )
    }
}

@Composable
private fun LogsList(
    entries: ImmutableList<LogEntry>,
    onLongClick: (LogEntry) -> Unit,
) {
    val listState = rememberLazyListState()

    val isAtBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            val total = listState.layoutInfo.totalItemsCount
            total == 0 || lastVisible == total - 1
        }
    }

    LaunchedEffect(entries.size) {
        if (isAtBottom && entries.isNotEmpty()) {
            listState.scrollToItem(entries.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items = entries, key = { it.id }) { entry ->
            LogEntryRow(
                entry = entry,
                onLongClick = { onLongClick(entry) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        }
    }
}

@Composable
private fun LogEntryRow(entry: LogEntry, onLongClick: () -> Unit) {
    val color = entry.level.badgeColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = null,
                indication = null,
                onClick = {},
                onLongClick = onLongClick,
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = entry.level.label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = color,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.tag,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = entry.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun LogsEmptyState(hasEntries: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (hasEntries) {
                    stringResource(R.string.wk_logs_empty_filtered)
                } else {
                    stringResource(R.string.wk_logs_empty_title)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            if (!hasEntries) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.wk_logs_empty_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
        }
    }
}

private fun shareLogEntries(context: Context, entries: ImmutableList<LogEntry>) {
    val text = buildString {
        entries.forEach { entry ->
            append("${entry.time} ${entry.level.label}/${entry.tag}: ${entry.message}\n")
        }
    }
    val chooser = Intent.createChooser(
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        },
        null,
    )
    context.startActivity(chooser)
}

private fun copyEntryToClipboard(context: Context, entry: LogEntry) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val text = "${entry.time} ${entry.level.label}/${entry.tag}: ${entry.message}"
    clipboardManager.setPrimaryClip(ClipData.newPlainText("log", text))
    Toast.makeText(context, context.getString(R.string.wk_copied_to_clipboard), Toast.LENGTH_SHORT).show()
}

@Composable
private fun LogLevel.badgeColor(): Color = when (this) {
    LogLevel.VERBOSE -> Color(0xFF9E9E9E)
    LogLevel.DEBUG -> MaterialTheme.colorScheme.primary
    LogLevel.INFO -> Color(0xFF4FC3F7)
    LogLevel.WARN -> Color(0xFFFFB74D)
    LogLevel.ERROR -> Color(0xFFEF5350)
}

private fun sampleEntries(): ImmutableList<LogEntry> = persistentListOf(
    LogEntry(
        id = 0,
        level = LogLevel.DEBUG,
        tag = "MainActivity",
        message = "onCreate called",
        time = "10:23:44.001",
    ),
    LogEntry(
        id = 1,
        level = LogLevel.INFO,
        tag = "WickKit",
        message = "SDK initialized successfully",
        time = "10:23:44.120",
    ),
    LogEntry(
        id = 2,
        level = LogLevel.VERBOSE,
        tag = "ViewModelProvider",
        message = "Creating ViewModel: MainViewModel",
        time = "10:23:44.350",
    ),
    LogEntry(
        id = 3,
        level = LogLevel.WARN,
        tag = "OkHttp",
        message = "No network cache configured",
        time = "10:23:44.512",
    ),
    LogEntry(
        id = 4,
        level = LogLevel.ERROR,
        tag = "Retrofit",
        message = "HTTP 404 Not Found: GET /api/users/42",
        time = "10:23:45.001",
    ),
    LogEntry(
        id = 5,
        level = LogLevel.DEBUG,
        tag = "Navigation",
        message = "Navigating to HomeFragment",
        time = "10:23:45.230",
    ),
    LogEntry(
        id = 6,
        level = LogLevel.INFO,
        tag = "Database",
        message = "Room database opened successfully",
        time = "10:23:45.450",
    ),
)

@PreviewLightDark
@Composable
private fun LogsTabPreview() {
    WickKitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LogsTabContent(entries = sampleEntries())
        }
    }
}

@PreviewLightDark
@Composable
private fun LogsTabEmptyPreview() {
    WickKitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LogsTabContent(entries = persistentListOf())
        }
    }
}
