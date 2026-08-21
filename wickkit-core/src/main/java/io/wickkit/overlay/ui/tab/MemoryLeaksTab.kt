package io.wickkit.overlay.ui.tab

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.wickkit.leaks.LeakEntry
import io.wickkit.leaks.WickKitLeakManager
import io.wickkit.overlay.ui.WickKitTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private val ToolbarHeight = 36.dp
private val LeakColor = Color(0xFFEF5350)

private sealed interface LeakScreen {
    data object List : LeakScreen
    data class Detail(val entry: LeakEntry) : LeakScreen
}

@Composable
internal fun MemoryLeaksTab() {
    val entries by WickKitLeakManager.entries.collectAsState()
    var screen: LeakScreen by remember { mutableStateOf(LeakScreen.List) }

    BackHandler(enabled = screen is LeakScreen.Detail) {
        screen = LeakScreen.List
    }

    when (val s = screen) {
        LeakScreen.List -> MemoryLeaksTabContent(
            entries = entries,
            onEntryClick = { screen = LeakScreen.Detail(it) },
        )

        is LeakScreen.Detail -> LeakDetailScreen(
            entry = s.entry,
            onBack = { screen = LeakScreen.List },
        )
    }
}

@Composable
private fun MemoryLeaksTabContent(
    entries: ImmutableList<LeakEntry>,
    onEntryClick: (LeakEntry) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LeaksToolbar(
            leakCount = entries.size,
            onClear = { WickKitLeakManager.clear() },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        if (entries.isEmpty()) {
            LeaksEmptyState()
        } else {
            LeaksList(entries = entries, onEntryClick = onEntryClick)
        }
    }
}

@Composable
private fun LeaksToolbar(leakCount: Int, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (leakCount == 0) {
                "No leaks detected"
            } else {
                "$leakCount leak${if (leakCount == 1) "" else "s"} detected"
            },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = if (leakCount > 0) LeakColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Box(
            modifier = Modifier
                .height(ToolbarHeight)
                .clip(RoundedCornerShape(8.dp))
                .clickable(interactionSource = null, indication = null) { onClear() }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Clear",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun LeaksList(entries: ImmutableList<LeakEntry>, onEntryClick: (LeakEntry) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = entries, key = { it.id }) { entry ->
            LeakEntryRow(entry = entry, onClick = { onEntryClick(entry) })
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        }
    }
}

@Composable
private fun LeakEntryRow(entry: LeakEntry, onClick: () -> Unit) {
    val simpleClass = entry.className.substringAfterLast('.').ifEmpty { entry.className }
    val packagePath = entry.className.substringBeforeLast('.', "")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null) { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(LeakColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (entry.instanceCount > 9) "9+" else "${entry.instanceCount}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = LeakColor,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = simpleClass,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = LeakColor.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = entry.detectedAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
            if (packagePath.isNotEmpty()) {
                Text(
                    text = packagePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (entry.instanceCount > 1) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "×${entry.instanceCount} instances leaked",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = LeakColor.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun LeaksEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No leaks detected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Activities and Fragments are monitored automatically",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
        }
    }
}

private fun sampleEntries(): ImmutableList<LeakEntry> = persistentListOf(
    LeakEntry(0, "io.wickkit.sample.MainActivity", "10:23:44.001", 1),
    LeakEntry(1, "io.wickkit.sample.HomeFragment", "10:23:54.022", 3, firstDetectedAt = "10:23:49.500"),
    LeakEntry(2, "io.wickkit.sample.ProfileActivity", "10:24:01.200", 1),
)

@PreviewLightDark
@Composable
private fun MemoryLeaksTabPreview() {
    WickKitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MemoryLeaksTabContent(entries = sampleEntries(), onEntryClick = {})
        }
    }
}

@PreviewLightDark
@Composable
private fun MemoryLeaksTabEmptyPreview() {
    WickKitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MemoryLeaksTabContent(entries = persistentListOf(), onEntryClick = {})
        }
    }
}
