package io.wickkit.overlay.ui.tab

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.wickkit.compose.ComposableEntry
import io.wickkit.compose.RecomposeSeverity
import io.wickkit.overlay.ui.WickKitTheme
import io.wickkit.performance.PerformanceSnapshot
import io.wickkit.performance.WickKitPerformanceManager
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

private val FpsGoodColor = Color(0xFF4CAF50)
private val FpsWarnColor = Color(0xFFFF9800)
private val FpsBadColor = Color(0xFFEF5350)
private val ComposableYellowColor = Color(0xFFFFC107)

private data class PerfSection(val title: String, val items: List<PerfSectionItem>, val isCompose: Boolean = false)

private data class PerfSectionItem(
    val label: String,
    val value: String,
    val valueColor: Color? = null,
)

@Composable
internal fun PerformanceTab() {
    val snapshot by WickKitPerformanceManager.snapshot.collectAsState()
    val sections = remember(snapshot) { buildPerformanceInfo(snapshot) }
    var sortByPeak by remember { mutableStateOf(false) }
    val composableEntries = remember(snapshot.composableEntries, sortByPeak) {
        if (sortByPeak) {
            snapshot.composableEntries.sortedByDescending { it.peakRatePerSecond }.toPersistentList()
        } else {
            snapshot.composableEntries
        }
    }
    PerformanceTabContent(
        sections = sections.toPersistentList(),
        composableEntries = composableEntries,
        composableTrackingActive = snapshot.composableTrackingActive,
        sortByPeak = sortByPeak,
        onToggleSort = { sortByPeak = !sortByPeak },
    )
}

@Composable
private fun PerformanceTabContent(
    sections: ImmutableList<PerfSection>,
    composableEntries: ImmutableList<ComposableEntry>,
    composableTrackingActive: Boolean,
    sortByPeak: Boolean,
    onToggleSort: () -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        sections.forEach { section ->
            item(key = section.title) {
                if (section.isCompose) {
                    ComposeSectionHeader(sortByPeak = sortByPeak, onToggleSort = onToggleSort)
                } else {
                    PerfSectionHeader(title = section.title)
                }
            }
            items(section.items, key = { "${section.title}/${it.label}" }) { item ->
                PerfInfoRow(item = item)
            }
            if (section.isCompose) {
                when {
                    !composableTrackingActive -> item(key = "composables_inactive") { ComposablesInactiveRow() }
                    composableEntries.isEmpty() -> item(key = "composables_empty") { ComposablesEmptyRow() }
                    else -> items(composableEntries, key = { it.name }) { entry -> ComposableEntryRow(entry = entry) }
                }
            }
        }
    }
}

@Composable
private fun PerfSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun PerfInfoRow(item: PerfSectionItem) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = item.value,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = item.valueColor ?: MaterialTheme.colorScheme.onSurface,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun ComposeSectionHeader(sortByPeak: Boolean, onToggleSort: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "COMPOSE",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(interactionSource = null, indication = null, onClick = onToggleSort)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = if (sortByPeak) "Peak ↓" else "Rate ↓",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ComposablesEmptyRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        Text(
            text = "No problematic composables detected",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ComposablesInactiveRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        Text(
            text = "Per-composable tracking inactive — apply the WickKit Gradle plugin to enable",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ComposableEntryRow(entry: ComposableEntry) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = entry.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "%.1f /s".format(entry.ratePerSecond),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = severityColor(entry.severity),
                )
                if (entry.peakRatePerSecond > entry.ratePerSecond) {
                    Spacer(modifier = Modifier.width(0.dp))
                    Text(
                        text = "↑ %.0f pk".format(entry.peakRatePerSecond),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = "%,d×".format(entry.totalCount),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
        )
    }
}

private fun severityColor(severity: RecomposeSeverity): Color = when (severity) {
    RecomposeSeverity.YELLOW -> ComposableYellowColor
    RecomposeSeverity.ORANGE -> FpsWarnColor
    RecomposeSeverity.RED -> FpsBadColor
}

private fun buildPerformanceInfo(s: PerformanceSnapshot): List<PerfSection> = listOf(
    buildFramesSection(s),
    buildComposeSection(s),
    buildMemorySection(s),
    buildRuntimeSection(s),
)

private fun buildFramesSection(s: PerformanceSnapshot): PerfSection {
    val hasData = s.totalFrames > 0
    val dash = "—"
    return PerfSection(
        title = "Frames",
        items = listOf(
            PerfSectionItem(
                label = "Activity",
                value = s.activityName.ifEmpty { dash },
            ),
            PerfSectionItem(
                label = "Avg FPS",
                value = s.fps?.let { "%.1f".format(it) } ?: dash,
                valueColor = s.fps?.let { fpsColor(it) },
            ),
            PerfSectionItem(
                label = "Total frames",
                value = if (hasData) "%,d".format(s.totalFrames) else dash,
            ),
            PerfSectionItem(
                label = "Slow (>16 ms)",
                value = if (hasData) {
                    "${"%,d".format(s.slowFrames)} (${"%.1f".format(s.jankRate ?: 0f)}%)"
                } else {
                    dash
                },
                valueColor = s.jankRate?.let { jankColor(it) },
            ),
            PerfSectionItem(
                label = "Frozen (>700 ms)",
                value = if (hasData) "${s.frozenFrames}" else dash,
                valueColor = if (s.frozenFrames > 0) FpsBadColor else null,
            ),
            PerfSectionItem(
                label = "Render P50",
                value = s.p50Ms?.let { "%.0f ms".format(it) } ?: dash,
            ),
            PerfSectionItem(
                label = "Render P90",
                value = s.p90Ms?.let { "%.0f ms".format(it) } ?: dash,
            ),
        ),
    )
}

private fun buildComposeSection(s: PerformanceSnapshot): PerfSection = PerfSection(
    title = "Compose",
    isCompose = true,
    items = listOf(
        PerfSectionItem(
            label = "Recompositions",
            value = "%,d".format(s.recompositionCount),
        ),
        PerfSectionItem(
            label = "Per second",
            value = if (s.recompositionsPerSecond > 0f) {
                "%.1f /s".format(s.recompositionsPerSecond)
            } else {
                "0 /s"
            },
        ),
    ),
)

private fun buildMemorySection(s: PerformanceSnapshot): PerfSection = PerfSection(
    title = "Memory (live)",
    items = listOf(
        PerfSectionItem(label = "JVM used", value = "${s.jvmUsedMb} MB"),
        PerfSectionItem(label = "JVM max", value = "${s.jvmMaxMb} MB"),
        PerfSectionItem(label = "Native heap", value = "${s.nativeHeapMb} MB"),
    ),
)

private fun buildRuntimeSection(s: PerformanceSnapshot): PerfSection = PerfSection(
    title = "Runtime (live)",
    items = listOf(
        PerfSectionItem(label = "Active threads", value = "${s.threadCount}"),
    ),
)

private fun fpsColor(fps: Float): Color = when {
    fps >= 55f -> FpsGoodColor
    fps >= 30f -> FpsWarnColor
    else -> FpsBadColor
}

private fun jankColor(rate: Float): Color? = when {
    rate >= 10f -> FpsBadColor
    rate >= 5f -> FpsWarnColor
    else -> null
}

private fun sampleSnapshot() = PerformanceSnapshot(
    activityName = "MainActivity",
    fps = 58.3f,
    slowFrames = 12,
    frozenFrames = 0,
    jankRate = 3.2f,
    p50Ms = 8f,
    p90Ms = 14f,
    totalFrames = 375,
    recompositionCount = 1_203L,
    recompositionsPerSecond = 12.4f,
    threadCount = 24,
    jvmUsedMb = 42L,
    jvmMaxMb = 256L,
    nativeHeapMb = 18L,
    composableEntries = persistentListOf(
        ComposableEntry("CheckoutButton", 1_204L, 62.0f, 87.3f, RecomposeSeverity.RED),
        ComposableEntry("ProductCard", 312L, 18.2f, 23.1f, RecomposeSeverity.ORANGE),
        ComposableEntry("SearchBar", 84L, 7.0f, 12.4f, RecomposeSeverity.YELLOW),
    ),
)

@PreviewLightDark
@Composable
private fun PerformanceTabPreview() {
    WickKitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            PerformanceTabContent(
                sections = buildPerformanceInfo(sampleSnapshot()).toPersistentList(),
                composableEntries = sampleSnapshot().composableEntries,
                composableTrackingActive = true,
                sortByPeak = false,
                onToggleSort = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PerformanceTabEmptyPreview() {
    WickKitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            PerformanceTabContent(
                sections = buildPerformanceInfo(PerformanceSnapshot()).toPersistentList(),
                composableEntries = persistentListOf(),
                composableTrackingActive = false,
                sortByPeak = false,
                onToggleSort = {},
            )
        }
    }
}
