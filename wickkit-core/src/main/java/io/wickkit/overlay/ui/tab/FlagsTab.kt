package io.wickkit.overlay.ui.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.wickkit.flags.FlagType
import io.wickkit.flags.RemoteConfigEntry
import io.wickkit.flags.SharedPreferencesEntry
import io.wickkit.flags.SharedPreferencesFileState
import io.wickkit.flags.WickKitFlagsManager
import io.wickkit.overlay.ui.WickKitTheme

// ─── Entry point ──────────────────────────────────────────────────────────────

@Composable
internal fun FlagsTab() {
    val viewModel: FlagsTabViewModel = viewModel()
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
    ) {
        FlagsScreenTitle()
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        FlagsContent(
            onSetSpOverride = viewModel::setSpOverride,
            onToggleSpOverride = viewModel::toggleSpOverride,
            onSetRcOverride = viewModel::setRcOverride,
            onToggleRcOverride = viewModel::toggleRcOverride,
        )
    }
}

// ─── Content ──────────────────────────────────────────────────────────────────

@Composable
private fun FlagsContent(
    onSetSpOverride: (prefsName: String, key: String, type: FlagType, value: String) -> Unit,
    onToggleSpOverride: (prefsName: String, entry: SharedPreferencesEntry) -> Unit,
    onSetRcOverride: (key: String, value: String) -> Unit,
    onToggleRcOverride: (RemoteConfigEntry) -> Unit,
) {
    val sharedPreferencesFiles by WickKitFlagsManager.sharedPreferencesFiles.collectAsState()
    val remoteConfigEntries by WickKitFlagsManager.remoteConfigEntries.collectAsState()
    var sharedPreferencesExpanded by remember { mutableStateOf(false) }
    var remoteConfigExpanded by remember { mutableStateOf(false) }
    var expandedFiles by remember { mutableStateOf(emptySet<String>()) }
    val sharedPreferencesOverrideCount = sharedPreferencesFiles.flatMap { it.entries }.count { it.isOverrideEnabled }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            FlagsSectionHeader(
                title = "SharedPreferences",
                overrideCount = sharedPreferencesOverrideCount,
                isExpanded = sharedPreferencesExpanded,
                onToggle = { sharedPreferencesExpanded = !sharedPreferencesExpanded },
            )
        }
        if (sharedPreferencesExpanded) {
            if (sharedPreferencesFiles.isEmpty()) item { FlagsEmptyRow("No SharedPreferences found") }
            sharedPreferencesFiles.forEach { file ->
                val isFileExpanded = file.name in expandedFiles
                item(key = "file_${file.name}") {
                    SharedPreferencesFileHeader(
                        name = file.name,
                        entryCount = file.entries.size,
                        overriddenCount = file.entries.count { it.isOverrideEnabled },
                        isExpanded = isFileExpanded,
                        onToggle = {
                            expandedFiles = if (isFileExpanded) expandedFiles - file.name else expandedFiles + file.name
                        },
                    )
                }
                if (isFileExpanded) {
                    sharedPreferencesEntries(
                        file = file,
                        onSetValue = { key, type, value -> onSetSpOverride(file.name, key, type, value) },
                        onToggleOverride = { entry -> onToggleSpOverride(file.name, entry) },
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
        remoteConfigSection(
            entries = remoteConfigEntries,
            remoteConfigExpanded = remoteConfigExpanded,
            onToggleRemoteConfigSection = { remoteConfigExpanded = !remoteConfigExpanded },
            onSetValue = onSetRcOverride,
            onToggleOverride = onToggleRcOverride,
        )
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ─── SharedPreferences rows ───────────────────────────────────────────────────

@Composable
private fun SharedPreferencesFileHeader(
    name: String,
    entryCount: Int,
    overriddenCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (overriddenCount > 0) {
                FlagsBadge(text = "$overriddenCount", color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = "$entryCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    }
}

@Composable
private fun SharedPreferencesEntryRow(
    entry: SharedPreferencesEntry,
    onSetValue: (String) -> Unit,
    onToggleOverride: () -> Unit,
) {
    val isBoolean = entry.type == FlagType.BOOLEAN
    var isEditing by remember(entry.key) { mutableStateOf(false) }
    var isBoolEditing by remember(entry.key) { mutableStateOf(false) }
    var editValue by remember(entry.key) { mutableStateOf(TextFieldValue("")) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable {
                    if (isBoolean) {
                        isBoolEditing = !isBoolEditing
                    } else if (!isEditing) {
                        isEditing = true
                        editValue = TextFieldValue(
                            entry.currentValue,
                            selection = TextRange(entry.currentValue.length),
                        )
                    }
                },
        ) {
            Text(
                text = entry.key,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            when {
                isEditing -> FlagsInlineEdit(
                    value = editValue,
                    keyboardType = when (entry.type) {
                        FlagType.INT, FlagType.LONG -> KeyboardType.Number
                        FlagType.FLOAT -> KeyboardType.Decimal
                        else -> KeyboardType.Text
                    },
                    onValueChange = { editValue = it },
                    onCommit = {
                        if (isEditing) {
                            isEditing = false
                            onSetValue(editValue.text)
                        }
                    },
                )

                isBoolEditing -> FlagsBoolEditor(
                    current = entry.currentValue,
                    onSelect = { value ->
                        isBoolEditing = false
                        onSetValue(value)
                    },
                )

                else -> SharedPreferencesEntryValueRow(entry = entry)
            }
        }
        CompactToggle(checked = entry.isOverrideEnabled, onToggle = { onToggleOverride() })
    }
}

@Composable
private fun SharedPreferencesEntryValueRow(entry: SharedPreferencesEntry) {
    val overrideColor = MaterialTheme.colorScheme.primary
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val valueColor = if (entry.isOverrideEnabled) overrideColor else MaterialTheme.colorScheme.onSurface
        Text(
            text = entry.currentValue,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        FlagsBadge(text = entry.type.name, color = mutedColor)
        if (entry.hasOverride) {
            val label = if (entry.isOverrideEnabled) {
                "orig: ${entry.backupValue}"
            } else {
                "override: ${entry.overrideValue}"
            }
            Text(
                text = "← $label",
                style = MaterialTheme.typography.labelSmall,
                color = mutedColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ─── SP items section ─────────────────────────────────────────────────────────

private fun LazyListScope.sharedPreferencesEntries(
    file: SharedPreferencesFileState,
    onSetValue: (key: String, type: FlagType, value: String) -> Unit,
    onToggleOverride: (entry: SharedPreferencesEntry) -> Unit,
) {
    if (file.entries.isEmpty()) {
        item(key = "empty_${file.name}") { FlagsEmptyRow("No values") }
    } else {
        items(count = file.entries.size, key = { "sp_${file.name}_${file.entries[it].key}" }) { index ->
            val entry = file.entries[index]
            SharedPreferencesEntryRow(
                entry = entry,
                onSetValue = { value -> onSetValue(entry.key, entry.type, value) },
                onToggleOverride = { onToggleOverride(entry) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        }
    }
}

// ─── RC LazyColumn section ────────────────────────────────────────────────────

private fun LazyListScope.remoteConfigSection(
    entries: List<RemoteConfigEntry>,
    remoteConfigExpanded: Boolean,
    onToggleRemoteConfigSection: () -> Unit,
    onSetValue: (String, String) -> Unit,
    onToggleOverride: (RemoteConfigEntry) -> Unit,
) {
    val isAvailable = WickKitFlagsManager.isRemoteConfigAvailable
    val remoteConfigOverrideCount = if (isAvailable) entries.count { it.isOverrideEnabled } else 0
    item {
        FlagsSectionHeader(
            title = "Firebase Remote Config",
            overrideCount = remoteConfigOverrideCount,
            available = isAvailable,
            isExpanded = remoteConfigExpanded,
            onToggle = onToggleRemoteConfigSection,
        )
    }
    if (!remoteConfigExpanded) return
    if (!isAvailable) {
        item { FlagsEmptyRow("Firebase Remote Config not found in this app") }
        return
    }
    if (entries.isEmpty()) {
        item { FlagsEmptyRow("No active Remote Config parameters") }
        return
    }
    items(count = entries.size, key = { "rc_${entries[it].key}" }) { index ->
        val entry = entries[index]
        RemoteConfigEntryRow(
            entry = entry,
            onSetValue = { value -> onSetValue(entry.key, value) },
            onToggleOverride = { onToggleOverride(entry) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    }
}

// ─── Remote Config rows ───────────────────────────────────────────────────────

@Composable
private fun RemoteConfigEntryRow(
    entry: RemoteConfigEntry,
    onSetValue: (String) -> Unit,
    onToggleOverride: () -> Unit,
) {
    val isBoolean = entry.remoteValue == "true" || entry.remoteValue == "false"
    var isEditing by remember(entry.key) { mutableStateOf(false) }
    var isBoolEditing by remember(entry.key) { mutableStateOf(false) }
    var editValue by remember(entry.key) { mutableStateOf(TextFieldValue("")) }
    val displayValue = if (entry.isOverrideEnabled) entry.overrideValue else entry.remoteValue

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable {
                    if (isBoolean) {
                        isBoolEditing = !isBoolEditing
                    } else if (!isEditing) {
                        isEditing = true
                        val initial = entry.overrideValue.ifEmpty { entry.remoteValue }
                        editValue = TextFieldValue(initial, selection = TextRange(initial.length))
                    }
                },
        ) {
            Text(
                text = entry.key,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            when {
                isEditing -> FlagsInlineEdit(
                    value = editValue,
                    onValueChange = { editValue = it },
                    onCommit = {
                        if (isEditing) {
                            isEditing = false
                            onSetValue(editValue.text)
                        }
                    },
                )

                isBoolEditing -> FlagsBoolEditor(
                    current = displayValue,
                    onSelect = { value ->
                        isBoolEditing = false
                        onSetValue(value)
                    },
                )

                else -> RemoteConfigEntryValueRow(entry = entry)
            }
        }
        CompactToggle(checked = entry.isOverrideEnabled, onToggle = { onToggleOverride() })
    }
}

@Composable
private fun RemoteConfigEntryValueRow(entry: RemoteConfigEntry) {
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val displayValue = if (entry.isOverrideEnabled) entry.overrideValue else entry.remoteValue
        val valueColor = if (entry.isOverrideEnabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
        Text(
            text = displayValue,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (entry.isOverrideEnabled) {
            Text(
                text = "← remote: ${entry.remoteValue}",
                style = MaterialTheme.typography.labelSmall,
                color = mutedColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ─── Inline edit field ────────────────────────────────────────────────────────

@Composable
private fun FlagsInlineEdit(
    value: TextFieldValue,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (TextFieldValue) -> Unit,
    onCommit: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var hasFocused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onCommit() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    if (state.hasFocus) {
                        hasFocused = true
                    } else if (hasFocused) {
                        onCommit()
                    }
                },
        )
    }
}

// ─── Boolean chip selector ────────────────────────────────────────────────────

@Composable
private fun FlagsBoolEditor(current: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("true", "false").forEach { option ->
            val isSelected = current == option
            Box(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = RoundedCornerShape(4.dp),
                    )
                    .background(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(4.dp),
                    )
                    .clickable(interactionSource = null, indication = null) { onSelect(option) }
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    ),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

// ─── Shared components ────────────────────────────────────────────────────────

@Composable
private fun FlagsScreenTitle() {
    Text(
        text = "Flags",
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun FlagsEmptyRow(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FlagsSectionHeader(
    title: String,
    overrideCount: Int = 0,
    available: Boolean = true,
    isExpanded: Boolean = true,
    onToggle: () -> Unit = {},
) {
    val titleColor = if (available) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = titleColor,
        )
        if (!available) {
            FlagsBadge(text = "Not available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (overrideCount > 0) {
            FlagsBadge(text = "$overrideCount active", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun FlagsBadge(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.12f)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun FlagsTabPreview() {
    WickKitTheme {
        FlagsTab()
    }
}
