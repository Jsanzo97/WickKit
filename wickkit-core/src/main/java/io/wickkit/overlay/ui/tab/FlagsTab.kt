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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.wickkit.core.R
import io.wickkit.flags.FlagType
import io.wickkit.flags.RemoteConfigEntry
import io.wickkit.flags.SharedPreferencesEntry
import io.wickkit.flags.SharedPreferencesFileState
import io.wickkit.flags.WickKitFlagsManager
import io.wickkit.overlay.ui.WickKitTheme

private val ToolbarHeight = 36.dp

private object FlagsTabState {
    var search: String = ""
}

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
    val expandedFilesState = remember { mutableStateOf(emptySet<String>()) }
    var search by remember { mutableStateOf(FlagsTabState.search) }
    val isSearching = search.isNotEmpty()
    val sharedPreferencesOverrideCount = sharedPreferencesFiles.flatMap { it.entries }.count { it.isOverrideEnabled }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            sharedPreferencesExpanded = true
            remoteConfigExpanded = true
            expandedFilesState.value = sharedPreferencesFiles.map { it.name }.toSet()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        FlagsSearchBar(query = search, onQueryChange = {
            search = it
            FlagsTabState.search = it
        })
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            item {
                FlagsSectionHeader(
                    title = stringResource(R.string.wk_flags_section_sp),
                    overrideCount = sharedPreferencesOverrideCount,
                    isExpanded = sharedPreferencesExpanded,
                    onToggle = { sharedPreferencesExpanded = !sharedPreferencesExpanded },
                )
            }
            if (sharedPreferencesExpanded) {
                sharedPreferencesSection(
                    files = sharedPreferencesFiles,
                    search = search,
                    expandedFilesState = expandedFilesState,
                    onSetValue = onSetSpOverride,
                    onToggleOverride = onToggleSpOverride,
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
            remoteConfigSection(
                entries = if (isSearching) {
                    remoteConfigEntries.filter { it.key.contains(search, ignoreCase = true) }
                } else {
                    remoteConfigEntries
                },
                remoteConfigExpanded = remoteConfigExpanded,
                onToggleRemoteConfigSection = { remoteConfigExpanded = !remoteConfigExpanded },
                onSetValue = onSetRcOverride,
                onToggleOverride = onToggleRcOverride,
            )
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

private fun LazyListScope.sharedPreferencesSection(
    files: List<SharedPreferencesFileState>,
    search: String,
    expandedFilesState: MutableState<Set<String>>,
    onSetValue: (prefsName: String, key: String, type: FlagType, value: String) -> Unit,
    onToggleOverride: (prefsName: String, entry: SharedPreferencesEntry) -> Unit,
) {
    val isSearching = search.isNotEmpty()
    if (files.isEmpty()) {
        item { FlagsEmptyRow(stringResource(R.string.wk_flags_empty_sp)) }
        return
    }
    files.forEach { file ->
        val fileEntries: List<SharedPreferencesEntry> = if (isSearching) {
            file.entries.filter { it.key.contains(search, ignoreCase = true) }
        } else {
            file.entries
        }
        if (isSearching && fileEntries.isEmpty()) return@forEach
        val expandedFiles = expandedFilesState.value
        val isFileExpanded = file.name in expandedFiles
        item(key = "file_${file.name}") {
            SharedPreferencesFileHeader(
                name = file.name,
                entryCount = file.entries.size,
                overriddenCount = file.entries.count { it.isOverrideEnabled },
                isExpanded = isFileExpanded,
                onToggle = {
                    val next = if (isFileExpanded) expandedFiles - file.name else expandedFiles + file.name
                    expandedFilesState.value = next
                },
            )
        }
        if (isFileExpanded) {
            sharedPreferencesEntries(
                file = file,
                entries = fileEntries,
                onSetValue = { key, type, value -> onSetValue(file.name, key, type, value) },
                onToggleOverride = { entry -> onToggleOverride(file.name, entry) },
            )
        }
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
    entries: List<SharedPreferencesEntry>,
    onSetValue: (key: String, type: FlagType, value: String) -> Unit,
    onToggleOverride: (entry: SharedPreferencesEntry) -> Unit,
) {
    if (entries.isEmpty()) {
        item(key = "empty_${file.name}") { FlagsEmptyRow(stringResource(R.string.wk_flags_empty_sp_file)) }
    } else {
        items(count = entries.size, key = { "sp_${file.name}_${entries[it].key}" }) { index ->
            val entry = entries[index]
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
            title = stringResource(R.string.wk_flags_section_rc),
            overrideCount = remoteConfigOverrideCount,
            available = isAvailable,
            isExpanded = remoteConfigExpanded,
            onToggle = onToggleRemoteConfigSection,
        )
    }
    if (!remoteConfigExpanded) return
    if (!isAvailable) {
        item { FlagsEmptyRow(stringResource(R.string.wk_flags_rc_unavailable)) }
        return
    }
    if (entries.isEmpty()) {
        item { FlagsEmptyRow(stringResource(R.string.wk_flags_rc_empty)) }
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
private fun FlagsSearchBar(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
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
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.wk_flags_search_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(ToolbarHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(interactionSource = null, indication = null) { onQueryChange("") }
                    .padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun FlagsScreenTitle() {
    Text(
        text = stringResource(R.string.wk_flags_title),
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
            FlagsBadge(
                text = stringResource(R.string.wk_flags_not_available),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
