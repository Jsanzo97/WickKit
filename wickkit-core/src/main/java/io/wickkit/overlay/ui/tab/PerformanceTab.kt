package io.wickkit.overlay.ui.tab

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.wickkit.overlay.ui.WickKitTheme
import io.wickkit.performance.PerformanceSnapshot
import io.wickkit.performance.WickKitPerformanceManager
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

private val FpsGoodColor = Color(0xFF4CAF50)
private val FpsWarnColor = Color(0xFFFF9800)
private val FpsBadColor = Color(0xFFEF5350)

private data class PerfSection(val title: String, val items: List<PerfSectionItem>)

private data class PerfSectionItem(
    val label: String,
    val value: String,
    val valueColor: Color? = null,
)

@Composable
internal fun PerformanceTab() {
    val snapshot by WickKitPerformanceManager.snapshot.collectAsState()
    val sections = remember(snapshot) { buildPerformanceInfo(snapshot) }
    PerformanceTabContent(sections = sections.toPersistentList())
}

@Composable
private fun PerformanceTabContent(sections: ImmutableList<PerfSection>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        sections.forEach { section ->
            item(key = section.title) { PerfSectionHeader(title = section.title) }
            items(section.items, key = { "${section.title}/${it.label}" }) { item ->
                PerfInfoRow(item = item)
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
)

@PreviewLightDark
@Composable
private fun PerformanceTabPreview() {
    WickKitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            PerformanceTabContent(sections = buildPerformanceInfo(sampleSnapshot()).toPersistentList())
        }
    }
}

@PreviewLightDark
@Composable
private fun PerformanceTabEmptyPreview() {
    WickKitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            PerformanceTabContent(sections = buildPerformanceInfo(PerformanceSnapshot()).toPersistentList())
        }
    }
}
