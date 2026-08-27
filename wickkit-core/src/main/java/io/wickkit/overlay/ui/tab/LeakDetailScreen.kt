package io.wickkit.overlay.ui.tab

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.wickkit.core.R
import io.wickkit.leaks.LeakEntry
import io.wickkit.overlay.ui.WickKitTheme

private val LeakDetailColor = Color(0xFFEF5350)

@Composable
internal fun LeakDetailScreen(entry: LeakEntry, onBack: () -> Unit) {
    val simpleClass = entry.className.substringAfterLast('.').ifEmpty { entry.className }
    val packagePath = entry.className.substringBeforeLast('.', "")
    val objectType = when {
        simpleClass.endsWith("Activity") -> "Activity"
        simpleClass.endsWith("Fragment") -> "Fragment"
        else -> "Object"
    }

    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        LeakDetailHeader(
            simpleClass = simpleClass,
            packagePath = packagePath,
            onBack = onBack,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { LeakSectionHeader(stringResource(R.string.wk_leak_section_object)) }
            item { LeakInfoRow(label = stringResource(R.string.wk_leak_label_class), value = simpleClass) }
            if (packagePath.isNotEmpty()) {
                item { LeakInfoRow(label = stringResource(R.string.wk_leak_label_package), value = packagePath) }
            }
            item { LeakInfoRow(label = stringResource(R.string.wk_leak_label_type), value = objectType) }

            item { LeakSectionHeader(stringResource(R.string.wk_leak_section_detection)) }
            item {
                LeakInfoRow(
                    label = stringResource(R.string.wk_leak_label_instances),
                    value = "${entry.instanceCount}",
                )
            }
            item {
                LeakInfoRow(
                    label = stringResource(R.string.wk_leak_label_first_detected),
                    value = entry.firstDetectedAt,
                )
            }
            if (entry.instanceCount > 1) {
                item {
                    LeakInfoRow(
                        label = stringResource(R.string.wk_leak_label_latest_detected),
                        value = entry.detectedAt,
                    )
                }
            }

            item { LeakSectionHeader(stringResource(R.string.wk_leak_section_causes)) }
            items(leakHints(context, objectType)) { hint ->
                LeakHintRow(hint = hint)
            }
        }
    }
}

@Composable
private fun LeakDetailHeader(
    simpleClass: String,
    packagePath: String,
    onBack: () -> Unit,
) {
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
                text = simpleClass,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = LeakDetailColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (packagePath.isNotEmpty()) {
                Text(
                    text = packagePath,
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
private fun LeakSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun LeakInfoRow(label: String, value: String) {
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
private fun LeakHintRow(hint: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "•",
            modifier = Modifier.padding(end = 8.dp, top = 1.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        )
    }
}

private fun leakHints(context: Context, objectType: String): List<String> = when (objectType) {
    "Activity" -> listOf(
        context.getString(R.string.wk_leak_hint_activity_1),
        context.getString(R.string.wk_leak_hint_activity_2),
        context.getString(R.string.wk_leak_hint_activity_3),
        context.getString(R.string.wk_leak_hint_activity_4),
    )

    "Fragment" -> listOf(
        context.getString(R.string.wk_leak_hint_fragment_1),
        context.getString(R.string.wk_leak_hint_fragment_2),
        context.getString(R.string.wk_leak_hint_fragment_3),
        context.getString(R.string.wk_leak_hint_fragment_4),
    )

    else -> listOf(
        context.getString(R.string.wk_leak_hint_object_1),
        context.getString(R.string.wk_leak_hint_object_2),
        context.getString(R.string.wk_leak_hint_object_3),
    )
}

@PreviewLightDark
@Composable
private fun LeakDetailScreenPreview() {
    WickKitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LeakDetailScreen(
                entry = LeakEntry(
                    id = 0,
                    className = "io.wickkit.sample.HomeFragment",
                    detectedAt = "10:23:54.022",
                    instanceCount = 3,
                    firstDetectedAt = "10:23:44.001",
                ),
                onBack = {},
            )
        }
    }
}
