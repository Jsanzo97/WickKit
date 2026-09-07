package io.wickkit.overlay.ui.tab

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.wickkit.core.R
import io.wickkit.crashes.CrashEntry
import io.wickkit.overlay.ui.WickKitTheme
import kotlinx.collections.immutable.persistentListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun CrashDetailScreen(entry: CrashEntry, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        CrashDetailHeader(entry = entry, onBack = onBack)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        when (entry) {
            is CrashEntry.Crash -> CrashDetailContent(entry = entry)
            is CrashEntry.Anr -> AnrDetailContent(entry = entry)
        }
    }
}

@Composable
private fun CrashDetailHeader(entry: CrashEntry, onBack: () -> Unit) {
    val crashLabel = stringResource(R.string.wk_crashes_badge_crash)
    val anrLabel = stringResource(R.string.wk_crashes_badge_anr)
    val title = when (entry) {
        is CrashEntry.Crash -> entry.exceptionType.substringAfterLast('.')
        is CrashEntry.Anr -> anrLabel
    }
    val subtitle = when (entry) {
        is CrashEntry.Crash -> "$crashLabel · ${entry.threadName} · ${entry.appVersion}"
        is CrashEntry.Anr -> entry.processName
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.wk_cd_back),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
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
    }
}

@Composable
private fun CrashDetailContent(entry: CrashEntry.Crash) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "section_info") {
            CrashDetailSectionHeader(stringResource(R.string.wk_crashes_section_info))
        }
        item(key = "exception") {
            CrashDetailInfoRow(
                label = stringResource(R.string.wk_crashes_label_exception),
                value = entry.exceptionType,
            )
        }
        if (entry.message.isNotEmpty()) {
            item(key = "message") {
                CrashDetailInfoRow(
                    label = stringResource(R.string.wk_crashes_label_message),
                    value = entry.message,
                )
            }
        }
        item(key = "thread") {
            CrashDetailInfoRow(
                label = stringResource(R.string.wk_crashes_label_thread),
                value = entry.threadName,
            )
        }
        item(key = "version") {
            CrashDetailInfoRow(
                label = stringResource(R.string.wk_crashes_label_version),
                value = entry.appVersion,
            )
        }
        item(key = "time") {
            CrashDetailInfoRow(
                label = stringResource(R.string.wk_crashes_label_time),
                value = formatDetailTimestamp(entry.timestamp),
            )
        }
        item(key = "section_stack") {
            CrashDetailSectionHeader(stringResource(R.string.wk_crashes_section_stack_trace))
        }
        if (entry.stackTrace.isEmpty()) {
            item(key = "no_stack") { CrashNoTraceRow() }
        } else {
            itemsIndexed(
                items = entry.stackTrace,
                key = { index, _ -> "frame_$index" },
            ) { _, frame ->
                CrashTraceLineRow(line = frame)
            }
        }
    }
}

@Composable
private fun AnrDetailContent(entry: CrashEntry.Anr) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "section_info") {
            CrashDetailSectionHeader(stringResource(R.string.wk_crashes_section_info))
        }
        if (entry.description.isNotEmpty()) {
            item(key = "description") {
                CrashDetailInfoRow(
                    label = stringResource(R.string.wk_crashes_label_description),
                    value = entry.description,
                )
            }
        }
        item(key = "process") {
            CrashDetailInfoRow(
                label = stringResource(R.string.wk_crashes_label_process),
                value = entry.processName,
            )
        }
        item(key = "time") {
            CrashDetailInfoRow(
                label = stringResource(R.string.wk_crashes_label_time),
                value = formatDetailTimestamp(entry.timestamp),
            )
        }
        item(key = "section_trace") {
            CrashDetailSectionHeader(stringResource(R.string.wk_crashes_section_trace))
        }
        if (entry.trace.isEmpty()) {
            item(key = "no_trace") { CrashNoTraceRow() }
        } else {
            itemsIndexed(
                items = entry.trace,
                key = { index, _ -> "line_$index" },
            ) { _, line ->
                CrashTraceLineRow(line = line)
            }
        }
    }
}

@Composable
private fun CrashDetailSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun CrashDetailInfoRow(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(end = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = value,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun CrashTraceLineRow(line: String) {
    Column {
        Text(
            text = line,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
        )
    }
}

@Composable
private fun CrashNoTraceRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.wk_crashes_no_trace),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}

private fun formatDetailTimestamp(timestamp: Long): String = SimpleDateFormat(
    "yyyy-MM-dd HH:mm:ss",
    Locale.getDefault(),
).format(Date(timestamp))

@PreviewLightDark
@Composable
private fun CrashDetailScreenPreview() {
    WickKitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CrashDetailScreen(
                entry = CrashEntry.Crash(
                    id = 0L,
                    timestamp = System.currentTimeMillis() - 60_000L,
                    exceptionType = "java.lang.NullPointerException",
                    message = "Attempt to invoke virtual method on a null object reference",
                    stackTrace = persistentListOf(
                        "io.wickkit.sample.MainActivity.onCreate(MainActivity.kt:42)",
                        "android.app.Activity.performCreate(Activity.java:8050)",
                        "android.app.ActivityThread.performLaunchActivity(ActivityThread.java:3825)",
                    ),
                    threadName = "main",
                    appVersion = "1.3.2",
                ),
                onBack = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun AnrDetailScreenPreview() {
    WickKitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CrashDetailScreen(
                entry = CrashEntry.Anr(
                    id = 1L,
                    timestamp = System.currentTimeMillis() - 3_600_000L,
                    description = "Input dispatching timed out (io.wickkit.sample/MainActivity)",
                    trace = persistentListOf(
                        "\"main\" prio=5 tid=1 Sleeping",
                        "  | group=\"main\" sCount=1 ucsCount=0 flags=1 obj=0x71c7b880 self=0xb400007528ecfbd0",
                        "  at java.lang.Thread.sleep(Native Method)",
                    ),
                    processName = "io.wickkit.sample",
                ),
                onBack = {},
            )
        }
    }
}
