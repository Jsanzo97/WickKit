package io.wickkit.overlay.ui.tab

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.wickkit.database.ColumnInfo
import io.wickkit.database.DatabaseDiscovery
import io.wickkit.database.DatabaseEntry
import io.wickkit.database.DatabaseManager
import io.wickkit.database.DatabaseStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val COLUMN_WIDTH: Dp = 140.dp
private val CELL_HEIGHT: Dp = 44.dp
private const val EDITED_BORDER_WIDTH = 3
private const val EDITED_BG_ALPHA = 0.07f

private sealed interface DbScreen {
    data object DatabaseList : DbScreen
    data class TableList(val db: DatabaseEntry) : DbScreen
    data class TableData(val db: DatabaseEntry, val table: String) : DbScreen
}

private data class TableUiState(
    val columns: List<ColumnInfo> = emptyList(),
    val rows: List<List<Any?>> = emptyList(),
    val editedRowKeys: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

private fun TableUiState.showsReadOnlyBanner(hasPk: Boolean) = !hasPk && !isLoading && error == null

// rowIndex to column name
private typealias CellKey = Pair<Int, String>

// ─── Entry point ──────────────────────────────────────────────────────────────

@Composable
internal fun DatabaseTab() {
    val context = LocalContext.current
    var screen: DbScreen by remember { mutableStateOf(DbScreen.DatabaseList) }

    BackHandler(enabled = screen !is DbScreen.DatabaseList) {
        screen = when (val s = screen) {
            is DbScreen.TableData -> DbScreen.TableList(s.db)
            is DbScreen.TableList -> DbScreen.DatabaseList
            DbScreen.DatabaseList -> DbScreen.DatabaseList
        }
    }

    when (val s = screen) {
        DbScreen.DatabaseList -> DatabaseListScreen(
            onSelect = { db -> screen = DbScreen.TableList(db) },
        )

        is DbScreen.TableList -> TableListScreen(
            db = s.db,
            onBack = { screen = DbScreen.DatabaseList },
            onSelect = { table -> screen = DbScreen.TableData(s.db, table) },
        )

        is DbScreen.TableData -> TableDataScreen(
            db = s.db,
            table = s.table,
            onBack = { screen = DbScreen.TableList(s.db) },
        )
    }
}

// ─── Screen 1: database list ─────────────────────────────────────────────────

@Composable
private fun DatabaseListScreen(onSelect: (DatabaseEntry) -> Unit) {
    val context = LocalContext.current
    var databases by remember { mutableStateOf<List<DatabaseEntry>?>(null) }

    LaunchedEffect(Unit) {
        databases = withContext(Dispatchers.IO) { DatabaseDiscovery.findDatabases(context) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTitle("Databases")
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        when {
            databases == null -> LoadingState()

            databases!!.isEmpty() -> EmptyState("No databases found")

            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(databases!!.size) { i ->
                    DatabaseRow(db = databases!![i], onSelect = onSelect)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
private fun DatabaseRow(db: DatabaseEntry, onSelect: (DatabaseEntry) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = db.status == DatabaseStatus.Ok) { onSelect(db) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = db.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (db.status == DatabaseStatus.Ok) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                },
            )
            Text(
                text = formatSize(db.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when (db.status) {
            DatabaseStatus.Ok -> Unit
            DatabaseStatus.Encrypted -> StatusBadge("Encrypted", MaterialTheme.colorScheme.error)
            DatabaseStatus.Unsupported -> StatusBadge("Unsupported", MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─── Screen 2: table list ─────────────────────────────────────────────────────

@Composable
private fun TableListScreen(
    db: DatabaseEntry,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var tables by remember { mutableStateOf<List<Pair<String, Long>>?>(null) }

    LaunchedEffect(db.path) {
        tables = withContext(Dispatchers.IO) {
            DatabaseManager(db.path).use { inspector ->
                inspector.listTables().map { it to inspector.getRowCount(it) }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenToolbar(title = db.name, onBack = onBack)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        when {
            tables == null -> LoadingState()

            tables!!.isEmpty() -> EmptyState("No tables found")

            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(tables!!.size) { i ->
                    val (name, count) = tables!![i]
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

private suspend fun persistEdit(
    cell: CellKey,
    uiState: TableUiState,
    newText: String,
    dbPath: String,
    table: String,
): Pair<String, List<List<Any?>>>? = withContext(Dispatchers.IO) {
    val (rowIdx, colName) = cell
    val originalRow = uiState.rows.getOrNull(rowIdx) ?: return@withContext null
    val original = originalRow.getOrNull(
        uiState.columns.indexOfFirst { it.name == colName },
    )?.toString() ?: ""
    if (newText == original) return@withContext null
    runCatching {
        DatabaseManager(dbPath).use { manager ->
            manager.updateRow(table, uiState.columns, originalRow, mapOf(colName to newText))
            rowKey(uiState.columns, originalRow) to manager.getRows(table)
        }
    }.getOrNull()
}

@Composable
private fun TableDataScreen(db: DatabaseEntry, table: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var uiState by remember(table) { mutableStateOf(TableUiState()) }
    var editingCell by remember { mutableStateOf<CellKey?>(null) }
    var editValue by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(table) {
        uiState = withContext(Dispatchers.IO) {
            runCatching {
                DatabaseManager(db.path).use { inspector ->
                    TableUiState(
                        columns = inspector.getColumns(table),
                        rows = inspector.getRows(table),
                        isLoading = false,
                    )
                }
            }.getOrElse { e -> TableUiState(isLoading = false, error = e.message ?: "Unknown error") }
        }
    }

    fun commitEdit() {
        val cell = editingCell ?: return
        editingCell = null
        val snapshot = editValue.text
        scope.launch {
            val result = persistEdit(cell, uiState, snapshot, db.path, table)
            if (result != null) {
                val (key, refreshed) = result
                uiState = uiState.copy(rows = refreshed, editedRowKeys = uiState.editedRowKeys + key)
            }
        }
    }

    val hasPk = uiState.columns.any { it.isPrimaryKey }
    val hScroll = rememberScrollState()
    val editedBorderColor = MaterialTheme.colorScheme.primary
    val editedBgColor = MaterialTheme.colorScheme.primary.copy(alpha = EDITED_BG_ALPHA)

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenToolbar(title = table, onBack = onBack)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        if (uiState.showsReadOnlyBanner(hasPk)) ReadOnlyBanner()
        when {
            uiState.isLoading -> LoadingState()

            uiState.error != null -> EmptyState("Error: ${uiState.error}")

            uiState.rows.isEmpty() -> EmptyState("Table is empty")

            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                stickyHeader {
                    GridHeader(columns = uiState.columns, hScroll = hScroll)
                }
                itemsIndexed(uiState.rows) { rowIdx, row ->
                    val key = rowKey(uiState.columns, row)
                    val isEdited = key in uiState.editedRowKeys
                    GridRow(
                        row = row,
                        columns = uiState.columns,
                        hScroll = hScroll,
                        isEdited = isEdited,
                        editedBorderColor = editedBorderColor,
                        editedBgColor = editedBgColor,
                        hasPk = hasPk,
                        editingCell = editingCell,
                        editValue = editValue,
                        rowIndex = rowIdx,
                        onEditValueChange = { editValue = it },
                        onCellClick = { colName ->
                            if (editingCell != null) commitEdit()
                            val value = row.getOrNull(uiState.columns.indexOfFirst { it.name == colName })
                            val text = value?.toString() ?: ""
                            editingCell = rowIdx to colName
                            editValue = TextFieldValue(text = text, selection = TextRange(text.length))
                        },
                        onCommitEdit = ::commitEdit,
                    )
                }
            }
        }
    }
}

// ─── Grid components ──────────────────────────────────────────────────────────

@Composable
private fun GridHeader(
    columns: List<ColumnInfo>,
    hScroll: androidx.compose.foundation.ScrollState,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .padding(start = EDITED_BORDER_WIDTH.dp) // align with data rows
                .horizontalScroll(hScroll),
        ) {
            columns.forEach { col ->
                HeaderCell(name = col.name)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun GridRow(
    row: List<Any?>,
    columns: List<ColumnInfo>,
    hScroll: androidx.compose.foundation.ScrollState,
    isEdited: Boolean,
    editedBorderColor: Color,
    editedBgColor: Color,
    hasPk: Boolean,
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
                .horizontalScroll(hScroll)
                .background(if (isEdited) editedBgColor else Color.Transparent),
        ) {
            row.forEachIndexed { colIdx, value ->
                val col = columns.getOrNull(colIdx) ?: return@forEachIndexed
                val isActiveEdit = editingCell?.first == rowIndex && editingCell.second == col.name
                val editable = hasPk && !col.isPrimaryKey
                DataCell(
                    text = if (isActiveEdit) editValue.text else value?.toString() ?: "null",
                    isPrimaryKey = col.isPrimaryKey,
                    isNull = value == null && !isActiveEdit,
                    isEditing = isActiveEdit,
                    editValue = editValue,
                    onEditValueChange = onEditValueChange,
                    onCommitEdit = onCommitEdit,
                    modifier = if (editable && !isActiveEdit) {
                        Modifier.clickable { onCellClick(col.name) }
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
private fun ScreenTitle(title: String) {
    Text(
        text = title,
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
                contentDescription = "Back",
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
private fun ReadOnlyBanner() {
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer) {
        Text(
            text = "Read-only — table has no primary key",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
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

private fun rowKey(
    columns: List<ColumnInfo>,
    row: List<Any?>,
): String = columns.filter { it.isPrimaryKey }.joinToString("|") { col ->
    row.getOrNull(columns.indexOf(col))?.toString() ?: "null"
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1_024 -> "$bytes B"
    bytes < 1_024 * 1_024 -> "${bytes / 1_024} KB"
    else -> "${"%.1f".format(bytes / (1_024.0 * 1_024.0))} MB"
}
