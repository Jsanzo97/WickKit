package io.wickkit.overlay.ui.tab

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.wickkit.database.ColumnInfo
import io.wickkit.database.DatabaseDiscovery
import io.wickkit.database.DatabaseEntry
import io.wickkit.database.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

internal const val POLL_INTERVAL_MS = 2_000L

internal sealed interface DbScreen {
    data object DatabaseList : DbScreen
    data class TableList(val database: DatabaseEntry) : DbScreen
    data class TableData(val database: DatabaseEntry, val table: String) : DbScreen
}

internal data class TableUiState(
    val columns: List<ColumnInfo> = emptyList(),
    val rows: List<List<Any?>> = emptyList(),
    val editedRowKeys: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

internal typealias CellKey = Pair<Int, String>

internal fun TableUiState.showsReadOnlyBanner(hasPk: Boolean) = !hasPk && !isLoading && error == null

internal fun rowKey(columns: List<ColumnInfo>, row: List<Any?>): String = columns.filter {
    it.isPrimaryKey
}.joinToString("|") { col ->
    row.getOrNull(columns.indexOf(col))?.toString() ?: "null"
}

private fun mergeWithEdits(
    freshColumns: List<ColumnInfo>,
    freshRows: List<List<Any?>>,
    uiState: TableUiState,
): Pair<List<List<Any?>>, Set<String>> {
    if (uiState.editedRowKeys.isEmpty()) return freshRows to emptySet()
    val survivingKeys = mutableSetOf<String>()
    val mergedRows = freshRows.map { freshRow ->
        val key = rowKey(freshColumns, freshRow)
        if (key !in uiState.editedRowKeys) return@map freshRow
        val editedRow = uiState.rows.find { row -> rowKey(uiState.columns, row) == key }
        if (editedRow != null && editedRow == freshRow) {
            survivingKeys += key
            editedRow
        } else {
            freshRow
        }
    }
    return mergedRows to survivingKeys
}

private suspend fun persistEdit(
    cell: CellKey,
    uiState: TableUiState,
    newText: String,
    databasePath: String,
    table: String,
): Pair<String, List<List<Any?>>>? = withContext(Dispatchers.IO) {
    val (rowIndex, colName) = cell
    val originalRow = uiState.rows.getOrNull(rowIndex) ?: return@withContext null
    val original = originalRow.getOrNull(
        uiState.columns.indexOfFirst { it.name == colName },
    )?.toString() ?: ""
    if (newText == original) return@withContext null
    runCatching {
        DatabaseManager(databasePath).use { manager ->
            manager.updateRow(
                table = table,
                columns = uiState.columns,
                originalRow = originalRow,
                edits = mapOf(colName to newText),
            )
            rowKey(columns = uiState.columns, row = originalRow) to manager.getRows(table)
        }
    }.getOrNull()
}

internal class DatabaseTabViewModel(application: Application) : AndroidViewModel(application) {

    private val _screen = MutableStateFlow<DbScreen>(DbScreen.DatabaseList)
    val screen: StateFlow<DbScreen> = _screen.asStateFlow()

    private val _databases = MutableStateFlow<List<DatabaseEntry>?>(null)
    val databases: StateFlow<List<DatabaseEntry>?> = _databases.asStateFlow()

    private val _tables = MutableStateFlow<List<Pair<String, Long>>?>(null)
    val tables: StateFlow<List<Pair<String, Long>>?> = _tables.asStateFlow()

    private val _tableUiState = MutableStateFlow(TableUiState())
    val tableUiState: StateFlow<TableUiState> = _tableUiState.asStateFlow()

    private val _editingCell = MutableStateFlow<CellKey?>(null)
    val editingCell: StateFlow<CellKey?> = _editingCell.asStateFlow()

    private var pollJob: Job? = null

    init {
        startPolling()
    }

    fun navigateTo(newScreen: DbScreen) {
        _screen.value = newScreen
        when (newScreen) {
            DbScreen.DatabaseList -> Unit

            is DbScreen.TableList -> _tables.value = null

            is DbScreen.TableData -> {
                _tableUiState.value = TableUiState()
                _editingCell.value = null
            }
        }
        startPolling()
    }

    fun navigateBack() {
        navigateTo(
            when (val s = _screen.value) {
                is DbScreen.TableData -> DbScreen.TableList(s.database)
                is DbScreen.TableList -> DbScreen.DatabaseList
                DbScreen.DatabaseList -> DbScreen.DatabaseList
            },
        )
    }

    fun onEditingStarted(rowIndex: Int, colName: String) {
        _editingCell.value = rowIndex to colName
    }

    fun cancelEdit() {
        _editingCell.value = null
    }

    fun onEditingCommitted(newText: String) {
        val cell = _editingCell.value ?: return
        val s = _screen.value as? DbScreen.TableData ?: return
        _editingCell.value = null
        viewModelScope.launch {
            val result = persistEdit(cell, _tableUiState.value, newText, s.database.path, s.table)
            if (result != null) {
                val (key, refreshed) = result
                _tableUiState.value = _tableUiState.value.copy(
                    rows = refreshed,
                    editedRowKeys = _tableUiState.value.editedRowKeys + key,
                )
            }
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            pollOnce()
            while (true) {
                delay(POLL_INTERVAL_MS.milliseconds)
                pollOnce()
            }
        }
    }

    private suspend fun pollOnce() {
        when (val s = _screen.value) {
            DbScreen.DatabaseList -> {
                _databases.value = withContext(Dispatchers.IO) {
                    DatabaseDiscovery.findDatabases(getApplication())
                }
            }

            is DbScreen.TableList -> {
                _tables.value = withContext(Dispatchers.IO) {
                    DatabaseManager(s.database.path).use { manager ->
                        manager.listTables().map { it to manager.getRowCount(it) }
                    }
                }
            }

            is DbScreen.TableData -> pollTableData(s)
        }
    }

    private suspend fun pollTableData(screen: DbScreen.TableData) {
        if (_editingCell.value != null) return
        val result = withContext(Dispatchers.IO) {
            runCatching {
                DatabaseManager(screen.database.path).use { inspector ->
                    inspector.getColumns(screen.table) to inspector.getRows(screen.table)
                }
            }
        }
        val current = _tableUiState.value
        if (current.isLoading) {
            _tableUiState.value = result.fold(
                onSuccess = { (cols, rows) -> TableUiState(columns = cols, rows = rows, isLoading = false) },
                onFailure = { TableUiState(isLoading = false, error = "Unable to read table data") },
            )
        } else {
            result.getOrNull()?.let { (freshColumns, freshRows) ->
                val (mergedRows, survivingKeys) = mergeWithEdits(freshColumns, freshRows, current)
                _tableUiState.value = current.copy(
                    columns = freshColumns,
                    rows = mergedRows,
                    editedRowKeys = survivingKeys,
                )
            }
        }
    }
}
