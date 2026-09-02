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
import io.wickkit.overlay.ui.WickKitTheme
import io.wickkit.threads.ThreadEntry
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun ThreadDetailScreen(entry: ThreadEntry, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        ThreadDetailHeader(entry = entry, onBack = onBack)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        ThreadDetailContent(entry = entry)
    }
}

@Composable
private fun ThreadDetailHeader(entry: ThreadEntry, onBack: () -> Unit) {
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
                text = entry.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.threadGroup.isNotEmpty()) {
                Text(
                    text = entry.threadGroup,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ThreadDetailContent(entry: ThreadEntry) {
    val daemonLabel = if (entry.isDaemon) {
        stringResource(R.string.wk_threads_daemon)
    } else {
        stringResource(R.string.wk_threads_user)
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "section_thread") {
            ThreadDetailSectionHeader(stringResource(R.string.wk_threads_section_info))
        }
        item(key = "state") {
            ThreadDetailInfoRow(
                label = stringResource(R.string.wk_threads_label_state),
                value = entry.state.name,
            )
        }
        item(key = "type") {
            ThreadDetailInfoRow(
                label = stringResource(R.string.wk_threads_label_type),
                value = daemonLabel,
            )
        }
        item(key = "priority") {
            ThreadDetailInfoRow(
                label = stringResource(R.string.wk_threads_label_priority),
                value = "${entry.priority} / 10",
            )
        }
        if (entry.threadGroup.isNotEmpty()) {
            item(key = "group") {
                ThreadDetailInfoRow(
                    label = stringResource(R.string.wk_threads_label_group),
                    value = entry.threadGroup,
                )
            }
        }
        item(key = "stack_depth") {
            ThreadDetailInfoRow(
                label = stringResource(R.string.wk_threads_label_stack_depth),
                value = "${entry.stackTrace.size}",
            )
        }
        item(key = "section_stack") {
            ThreadDetailSectionHeader(stringResource(R.string.wk_threads_section_stack_trace))
        }
        if (entry.stackTrace.isEmpty()) {
            item(key = "no_stack") { ThreadStackEmptyRow() }
        } else {
            itemsIndexed(
                items = entry.stackTrace,
                key = { index, _ -> "frame_$index" },
            ) { _, frame ->
                ThreadStackFrameRow(frame = frame)
            }
        }
    }
}

@Composable
private fun ThreadDetailSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ThreadDetailInfoRow(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(end = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = value,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
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
private fun ThreadStackFrameRow(frame: String) {
    Column {
        Text(
            text = frame,
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
private fun ThreadStackEmptyRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.wk_threads_no_stack_trace),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}

@PreviewLightDark
@Composable
private fun ThreadDetailScreenPreview() {
    WickKitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ThreadDetailScreen(
                entry = ThreadEntry(
                    id = 0,
                    name = "DatabaseWorker-1",
                    state = Thread.State.BLOCKED,
                    isDaemon = true,
                    priority = 5,
                    threadGroup = "main",
                    stackTrace = persistentListOf(
                        "java.lang.Object.wait(Native Method)",
                        "io.wickkit.sample.db.DatabaseHelper.query(DatabaseHelper.kt:88)",
                        "io.wickkit.sample.repo.UserRepository.getUser(UserRepository.kt:34)",
                    ),
                ),
                onBack = {},
            )
        }
    }
}
