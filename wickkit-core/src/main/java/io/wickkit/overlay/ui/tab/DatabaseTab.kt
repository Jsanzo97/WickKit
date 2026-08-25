package io.wickkit.overlay.ui.tab

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.wickkit.core.R
import io.wickkit.database.ColumnInfo
import io.wickkit.database.DatabaseEntry
import io.wickkit.database.DatabaseStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

private val COLUMN_WIDTH: Dp = 140.dp
private val CELL_HEIGHT: Dp = 44.dp
private const val EDITED_BORDER_WIDTH = 3
private const val EDITED_BG_ALPHA = 0.07f

// ─── Entry point ──────────────────────────────────────────────────────────────

@Composable
internal fun DatabaseTab() {
    val viewModel: DatabaseTabViewModel = viewModel()
    val screen by viewModel.screen.collectAsState()
    val databases by viewModel.databases.collectAsState()
    val tables by viewModel.tables.collectAsState()
    val tableUiState by viewModel.tableUiState.collectAsState()
    val editingCell by viewModel.editingCell.collectAsState()

    BackHandler(enabled = screen !is DbScreen.DatabaseList) { viewModel.navigateBack() }

    when (val s = screen) {
        DbScreen.DatabaseList -> DatabaseListScreen(
            databases = databases,
            onSelect = { db -> viewModel.navigateTo(DbScreen.TableList(db)) },
        )

        is DbScreen.TableList -> TableListScreen(
            database = s.database,
            tables = tables,
            onBack = viewModel::navigateBack,
            onSelect = { table -> viewModel.navigateTo(DbScreen.TableData(s.database, table)) },
        )

        is DbScreen.TableData -> {
            val focusManager = LocalFocusManager.current
            var editValue by remember { mutableStateOf(TextFieldValue("")) }
            TableDataBody(
                table = s.table,
                uiState = tableUiState,
                editingCell = editingCell,
                editValue = editValue,
                onBack = viewModel::navigateBack,
                onFocusClear = {
                    focusManager.clearFocus()
                    viewModel.cancelEdit()
                },
                onEditValueChange = { editValue = it },
                onCellClick = { rowIndex, colName, text ->
                    if (editingCell != null) viewModel.onEditingCommitted(editValue.text)
                    editValue = TextFieldValue(text = text, selection = TextRange(text.length))
                    viewModel.onEditingStarted(rowIndex, colName)
                },
                onCommitEdit = { viewModel.onEditingCommitted(editValue.text) },
            )
        }
    }
}

// ─── Screen 1: database list ─────────────────────────────────────────────────

@Composable
private fun DatabaseListScreen(
    databases: List<DatabaseEntry>?,
    onSelect: (DatabaseEntry) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTitle()
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        when {
            databases == null -> LoadingState()

            databases.isEmpty() -> EmptyState(stringResource(R.string.wk_database_empty_databases))

            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(databases.size) { index ->
                    DatabaseRow(databaseEntry = databases[index], onSelect = onSelect)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
private fun DatabaseRow(databaseEntry: DatabaseEntry, onSelect: (DatabaseEntry) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = databaseEntry.status == DatabaseStatus.Ok) { onSelect(databaseEntry) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = databaseEntry.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (databaseEntry.status == DatabaseStatus.Ok) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                },
            )
            Text(
                text = formatSize(databaseEntry.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when (databaseEntry.status) {
            DatabaseStatus.Ok -> Unit

            DatabaseStatus.Encrypted -> StatusBadge(
                stringResource(R.string.wk_database_status_encrypted),
                MaterialTheme.colorScheme.error,
            )

            DatabaseStatus.Unsupported -> StatusBadge(
                stringResource(R.string.wk_database_status_unsupported),
                MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── Screen 2: table list ─────────────────────────────────────────────────────

@Composable
private fun TableListScreen(
    database: DatabaseEntry,
    tables: List<Pair<String, Long>>?,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenToolbar(title = database.name, onBack = onBack)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        when {
            tables == null -> LoadingState()

            tables.isEmpty() -> EmptyState(stringResource(R.string.wk_database_empty_tables))

            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(tables.size) { index ->
                    val (name, count) = tables[index]
                    TableRow(name = name, rowCount = count, onClick = { onSelect(name) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
private fun TableRow(name: String, rowCount: Long, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "$rowCount rows",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─── Screen 3: table data grid ────────────────────────────────────────────────

@Composable
private fun TableDataBody(
    table: String,
    uiState: TableUiState,
    editingCell: CellKey?,
    editValue: TextFieldValue,
    onBack: () -> Unit,
    onFocusClear: () -> Unit,
    onEditValueChange: (TextFieldValue) -> Unit,
    onCellClick: (rowIndex: Int, colName: String, text: String) -> Unit,
    onCommitEdit: () -> Unit,
) {
    val horizontalScroll = rememberScrollState()
    val editedBorderColor = MaterialTheme.colorScheme.primary
    val editedBgColor = MaterialTheme.colorScheme.primary.copy(alpha = EDITED_BG_ALPHA)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { onFocusClear() } },
    ) {
        ScreenToolbar(title = table, onBack = onBack)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        when {
            uiState.isLoading -> LoadingState()

            uiState.error != null -> EmptyState("Error: ${uiState.error}")

            uiState.rows.isEmpty() -> EmptyState(stringResource(R.string.wk_database_empty_table))

            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                stickyHeader {
                    GridHeader(columns = uiState.columns.toPersistentList(), horizontalScroll = horizontalScroll)
                }
                itemsIndexed(uiState.rows) { rowIndex, row ->
                    val key = rowKey(uiState.columns, row)
                    GridRow(
                        row = row.toPersistentList(),
                        columns = uiState.columns.toPersistentList(),
                        horizontalScroll = horizontalScroll,
                        isEdited = key in uiState.editedRowKeys,
                        editedBorderColor = editedBorderColor,
                        editedBgColor = editedBgColor,
                        editingCell = editingCell,
                        editValue = editValue,
                        rowIndex = rowIndex,
                        onEditValueChange = onEditValueChange,
                        onCellClick = { colName ->
                            val value = row.getOrNull(uiState.columns.indexOfFirst { it.name == colName })
                            onCellClick(rowIndex, colName, value?.toString() ?: "")
                        },
                        onCommitEdit = onCommitEdit,
                    )
                }
            }
        }
    }
}

// ─── Grid components ──────────────────────────────────────────────────────────

@Composable
private fun GridHeader(
    columns: ImmutableList<ColumnInfo>,
    horizontalScroll: androidx.compose.foundation.ScrollState,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .padding(start = EDITED_BORDER_WIDTH.dp)
                .horizontalScroll(horizontalScroll),
        ) {
            columns.filter { !it.isRowId }.forEach { col ->
                HeaderCell(name = col.name)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun GridRow(
    row: ImmutableList<Any?>,
    columns: ImmutableList<ColumnInfo>,
    horizontalScroll: androidx.compose.foundation.ScrollState,
    isEdited: Boolean,
    editedBorderColor: Color,
    editedBgColor: Color,
    editingCell: CellKey?,
    editValue: TextFieldValue,
    rowIndex: Int,
    onEditValueChange: (TextFieldValue) -> Unit,
    onCellClick: (String) -> Unit,
    onCommitEdit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (isEdited) {
                    drawRect(
                        color = editedBorderColor,
                        topLeft = Offset.Zero,
                        size = Size(EDITED_BORDER_WIDTH.dp.toPx(), size.height),
                    )
                }
            },
    ) {
        Row(
            modifier = Modifier
                .padding(start = EDITED_BORDER_WIDTH.dp)
                .horizontalScroll(horizontalScroll)
                .background(if (isEdited) editedBgColor else Color.Transparent),
        ) {
            row.forEachIndexed { columnIndex, value ->
                val column = columns.getOrNull(columnIndex) ?: return@forEachIndexed
                if (column.isRowId) return@forEachIndexed
                val isActiveEdit = editingCell?.first == rowIndex && editingCell.second == column.name
                val editable = !column.isPrimaryKey
                DataCell(
                    text = if (isActiveEdit) editValue.text else value?.toString() ?: "null",
                    isPrimaryKey = column.isPrimaryKey,
                    isNull = value == null && !isActiveEdit,
                    isEditing = isActiveEdit,
                    editValue = editValue,
                    onEditValueChange = onEditValueChange,
                    onCommitEdit = onCommitEdit,
                    modifier = if (editable && !isActiveEdit) {
                        Modifier.clickable { onCellClick(column.name) }
                    } else {
                        Modifier
                    },
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    }
}

@Composable
private fun HeaderCell(name: String) {
    Box(
        modifier = Modifier
            .width(COLUMN_WIDTH)
            .height(CELL_HEIGHT)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DataCell(
    text: String,
    isPrimaryKey: Boolean,
    isNull: Boolean,
    isEditing: Boolean,
    editValue: TextFieldValue,
    onEditValueChange: (TextFieldValue) -> Unit,
    onCommitEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor = when {
        isPrimaryKey -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        isNull -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val cursorColor = MaterialTheme.colorScheme.primary
    val textStyle = MaterialTheme.typography.bodySmall.copy(color = textColor)
    val editBorderColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .width(COLUMN_WIDTH)
            .height(CELL_HEIGHT)
            .then(
                if (isEditing) {
                    Modifier.border(1.5.dp, editBorderColor, RoundedCornerShape(3.dp))
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (isEditing) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            BasicTextField(
                value = editValue,
                onValueChange = onEditValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(cursorColor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onCommitEdit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        } else {
            Text(
                text = text,
                style = textStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ─── Shared components ────────────────────────────────────────────────────────

@Composable
private fun ScreenTitle() {
    Text(
        text = stringResource(R.string.wk_database_title),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun ScreenToolbar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.wk_cd_back),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun formatSize(bytes: Long): String = when {
    bytes < 1_024 -> "$bytes B"
    bytes < 1_024 * 1_024 -> "${bytes / 1_024} KB"
    else -> "${"%.1f".format(bytes / (1_024.0 * 1_024.0))} MB"
}
