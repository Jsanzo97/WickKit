package io.wickkit.overlay.ui.tab

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.wickkit.network.MockRule
import io.wickkit.network.MockRuleManager
import io.wickkit.network.NetworkEntry
import io.wickkit.network.WickKitNetworkManager
import io.wickkit.overlay.ui.WickKitTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private val ToolbarHeight = 36.dp

private val HTTP_METHODS = listOf("GET", "POST", "PUT", "DELETE", "PATCH")

private sealed interface NetworkScreen {
    data object Requests : NetworkScreen
    data class Detail(val entry: NetworkEntry) : NetworkScreen
    data object Mocks : NetworkScreen
    data class MockEditor(
        val rule: MockRule?,
        val prefillFrom: NetworkEntry?,
        val returnTo: NetworkScreen,
    ) : NetworkScreen
}

@Composable
internal fun NetworkTab() {
    val entries by WickKitNetworkManager.entries.collectAsState()
    val rules by MockRuleManager.rules.collectAsState()
    var screen: NetworkScreen by remember { mutableStateOf(NetworkScreen.Requests) }

    BackHandler(enabled = screen !is NetworkScreen.Requests) {
        screen = when (val s = screen) {
            is NetworkScreen.Detail -> NetworkScreen.Requests
            NetworkScreen.Mocks -> NetworkScreen.Requests
            is NetworkScreen.MockEditor -> s.returnTo
            NetworkScreen.Requests -> NetworkScreen.Requests
        }
    }

    when (val s = screen) {
        NetworkScreen.Requests -> NetworkRequestsScreen(
            entries = entries,
            activeRulesCount = rules.count { it.isEnabled },
            onEntryClick = { screen = NetworkScreen.Detail(it) },
            onEntryLongClick = {
                screen = NetworkScreen.MockEditor(
                    rule = null,
                    prefillFrom = it,
                    returnTo = NetworkScreen.Requests,
                )
            },
            onMocksClick = { screen = NetworkScreen.Mocks },
        )

        is NetworkScreen.Detail -> NetworkDetailScreen(
            entry = s.entry,
            onBack = { screen = NetworkScreen.Requests },
            onMockRequest = {
                screen = NetworkScreen.MockEditor(
                    rule = null,
                    prefillFrom = it,
                    returnTo = NetworkScreen.Detail(s.entry),
                )
            },
        )

        NetworkScreen.Mocks -> MocksScreen(
            rules = rules,
            onBack = { screen = NetworkScreen.Requests },
            onToggle = { MockRuleManager.toggle(it) },
            onDelete = { MockRuleManager.remove(it) },
            onEdit = {
                screen = NetworkScreen.MockEditor(
                    rule = it,
                    prefillFrom = null,
                    returnTo = NetworkScreen.Mocks,
                )
            },
            onAddNew = {
                screen = NetworkScreen.MockEditor(
                    rule = null,
                    prefillFrom = null,
                    returnTo = NetworkScreen.Mocks,
                )
            },
        )

        is NetworkScreen.MockEditor -> MockRuleEditor(
            rule = s.rule,
            prefillFrom = s.prefillFrom,
            onBack = { screen = s.returnTo },
            onSave = { rule ->
                if (s.rule == null) MockRuleManager.add(rule) else MockRuleManager.update(rule)
                screen = NetworkScreen.Mocks
            },
        )
    }
}

// ─── Requests screen ─────────────────────────────────────────────────────────

@Composable
private fun NetworkRequestsScreen(
    entries: ImmutableList<NetworkEntry>,
    activeRulesCount: Int,
    onEntryClick: (NetworkEntry) -> Unit,
    onEntryLongClick: (NetworkEntry) -> Unit,
    onMocksClick: () -> Unit,
) {
    var search by remember { mutableStateOf("") }
    var methodFilter by remember { mutableStateOf<String?>(null) }

    val filtered = remember(entries, search, methodFilter) {
        entries.filter { entry ->
            (methodFilter == null || entry.method.equals(methodFilter, ignoreCase = true)) &&
                (search.isBlank() || entry.url.contains(search, ignoreCase = true))
        }.toImmutableList()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        RequestsToolbar(
            search = search,
            activeRulesCount = activeRulesCount,
            onSearch = { search = it },
            onMocksClick = onMocksClick,
            onClear = { WickKitNetworkManager.clear() },
        )
        MethodFilterRow(selected = methodFilter, onSelect = { methodFilter = it })
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        if (filtered.isEmpty()) {
            NetworkEmptyState(hasEntries = entries.isNotEmpty())
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = filtered, key = { it.id }) { entry ->
                    NetworkEntryRow(
                        entry = entry,
                        onClick = { onEntryClick(entry) },
                        onLongClick = { onEntryLongClick(entry) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                }
            }
        }
    }
}

@Composable
private fun RequestsToolbar(
    search: String,
    activeRulesCount: Int,
    onSearch: (String) -> Unit,
    onMocksClick: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
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
            if (search.isEmpty()) {
                Text(
                    text = "Search by URL…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                )
            }
            BasicTextField(
                value = search,
                onValueChange = onSearch,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val mocksLabel = if (activeRulesCount > 0) "Mocks ($activeRulesCount)" else "Mocks"
        Box(
            modifier = Modifier
                .height(ToolbarHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (activeRulesCount > 0) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    } else {
                        Color.Transparent
                    },
                )
                .clickable(interactionSource = null, indication = null) { onMocksClick() }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = mocksLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (activeRulesCount > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
        Box(
            modifier = Modifier
                .height(ToolbarHeight)
                .clip(RoundedCornerShape(8.dp))
                .clickable(interactionSource = null, indication = null) { onClear() }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Clear",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun MethodFilterRow(selected: String?, onSelect: (String?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MethodChip(
            label = "ALL",
            isSelected = selected == null,
            color = MaterialTheme.colorScheme.onSurface,
            onClick = { onSelect(null) },
        )
        HTTP_METHODS.forEach { method ->
            MethodChip(
                label = method,
                isSelected = selected == method,
                color = methodColor(method),
                onClick = { onSelect(if (selected == method) null else method) },
            )
        }
    }
}

@Composable
private fun MethodChip(label: String, isSelected: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (isSelected) color.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(interactionSource = null, indication = null) { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            ),
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        )
    }
}

@Composable
private fun NetworkEntryRow(entry: NetworkEntry, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = null,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.width(IntrinsicSize.Max),
        ) {
            MethodBadge(method = entry.method, modifier = Modifier.fillMaxWidth())
            StatusBadge(entry = entry)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatDuration(entry.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
                if (entry.error != null) {
                    Text(
                        text = entry.error,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFEF5350),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        }
        Text(
            text = entry.time,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
    }
}

@Composable
internal fun MethodBadge(method: String, modifier: Modifier = Modifier) {
    val color = methodColor(method)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = method,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
        )
    }
}

@Composable
private fun StatusBadge(entry: NetworkEntry) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val (text, color) = when {
            entry.error != null -> "ERR" to Color(0xFF9E9E9E)
            else -> (entry.statusCode?.toString() ?: "?") to statusColor(entry.statusCode)
        }
        StatusLabel(text = text, color = color, modifier = Modifier.fillMaxWidth())
        if (entry.isMocked) {
            StatusLabel(text = "MOCK", color = Color(0xFF9C27B0), modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun StatusCodeBadge(code: Int) {
    StatusLabel(text = code.toString(), color = statusColor(code))
}

@Composable
private fun StatusLabel(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
        )
    }
}

@Composable
private fun NetworkEmptyState(hasEntries: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (hasEntries) "No requests match the filter" else "No requests captured yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            if (!hasEntries) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Add WickKitNetworkInterceptor to your OkHttpClient",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
        }
    }
}

// ─── Mocks screen ─────────────────────────────────────────────────────────────

@Composable
private fun MocksScreen(
    rules: ImmutableList<MockRule>,
    onBack: () -> Unit,
    onToggle: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onEdit: (MockRule) -> Unit,
    onAddNew: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MocksToolbar(onBack = onBack, onAddNew = onAddNew)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        if (rules.isEmpty()) {
            MocksEmptyState()
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = rules, key = { it.id }) { rule ->
                    MockRuleRow(
                        rule = rule,
                        onToggle = { onToggle(rule.id) },
                        onDelete = { onDelete(rule.id) },
                        onEdit = { onEdit(rule) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                }
            }
        }
    }
}

@Composable
private fun MocksToolbar(onBack: () -> Unit, onAddNew: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
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
            text = "Mock Rules",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .height(ToolbarHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .clickable(interactionSource = null, indication = null) { onAddNew() }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+ Add Rule",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun MockRuleRow(
    rule: MockRule,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null) { onEdit() }
            .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MethodBadge(method = rule.method ?: "ALL")
                StatusCodeBadge(code = rule.statusCode)
            }
            Text(
                text = rule.urlPattern,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (rule.delayMs > 0) {
                Text(
                    text = "Delay: ${formatDuration(rule.delayMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
        CompactToggle(checked = rule.isEnabled, onToggle = onToggle)
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete rule",
                tint = Color(0xFFEF5350),
            )
        }
    }
}

@Composable
private fun MocksEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No mock rules yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tap a request → \"Mock this request\" to create one",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun CompactToggle(checked: Boolean, onToggle: () -> Unit) {
    val trackColor = if (checked) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }
    Box(
        modifier = Modifier
            .size(width = 32.dp, height = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(trackColor)
            .clickable(interactionSource = null, indication = null) { onToggle() },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .size(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White),
        )
    }
}

// ─── Shared helpers ───────────────────────────────────────────────────────────

internal fun methodColor(method: String): Color = when (method.uppercase()) {
    "GET" -> Color(0xFF4FC3F7)
    "POST" -> Color(0xFF66BB6A)
    "PUT" -> Color(0xFFFFB74D)
    "DELETE" -> Color(0xFFEF5350)
    "PATCH" -> Color(0xFFBA68C8)
    else -> Color(0xFF9E9E9E)
}

internal fun statusColor(code: Int?): Color = when {
    code == null -> Color(0xFF9E9E9E)
    code < 300 -> Color(0xFF66BB6A)
    code < 400 -> Color(0xFF4FC3F7)
    code < 500 -> Color(0xFFFFB74D)
    else -> Color(0xFFEF5350)
}

internal fun formatDuration(ms: Long): String = when {
    ms < 1000 -> "${ms}ms"
    else -> "${"%.1f".format(ms / 1000.0)}s"
}

// ─── Previews ─────────────────────────────────────────────────────────────────

private fun sampleEntries() = persistentListOf(
    NetworkEntry(
        id = 0,
        method = "GET",
        url = "https://api.example.com/v1/users",
        requestHeaders = emptyMap(),
        requestBody = null,
        statusCode = 200,
        responseHeaders = emptyMap(),
        responseBody = null,
        durationMs = 142,
        time = "10:23:44",
        error = null,
    ),
    NetworkEntry(
        id = 1,
        method = "POST",
        url = "https://api.example.com/v1/users",
        requestHeaders = emptyMap(),
        requestBody = "{}",
        statusCode = 201,
        responseHeaders = emptyMap(),
        responseBody = null,
        durationMs = 340,
        time = "10:23:45",
        error = null,
    ),
    NetworkEntry(
        id = 2,
        method = "GET",
        url = "https://api.example.com/v1/products?page=1",
        requestHeaders = emptyMap(),
        requestBody = null,
        statusCode = 404,
        responseHeaders = emptyMap(),
        responseBody = null,
        durationMs = 88,
        time = "10:23:46",
        error = null,
    ),
    NetworkEntry(
        id = 3,
        method = "DELETE",
        url = "https://api.example.com/v1/items/42",
        requestHeaders = emptyMap(),
        requestBody = null,
        statusCode = 500,
        responseHeaders = emptyMap(),
        responseBody = null,
        durationMs = 1230,
        time = "10:23:47",
        error = null,
    ),
    NetworkEntry(
        id = 4,
        method = "GET",
        url = "https://api.example.com/v1/data",
        requestHeaders = emptyMap(),
        requestBody = null,
        statusCode = 200,
        responseHeaders = emptyMap(),
        responseBody = null,
        durationMs = 95,
        time = "10:23:48",
        error = null,
        isMocked = true,
    ),
    NetworkEntry(
        id = 5,
        method = "GET",
        url = "https://api.example.com/v1/timeout",
        requestHeaders = emptyMap(),
        requestBody = null,
        statusCode = null,
        responseHeaders = emptyMap(),
        responseBody = null,
        durationMs = 30000,
        time = "10:23:49",
        error = "connection timeout",
    ),
)

@PreviewLightDark
@Composable
private fun NetworkRequestsPreview() {
    WickKitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            NetworkRequestsScreen(
                entries = sampleEntries(),
                activeRulesCount = 1,
                onEntryClick = {},
                onEntryLongClick = {},
                onMocksClick = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun NetworkRequestsEmptyPreview() {
    WickKitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            NetworkRequestsScreen(
                entries = persistentListOf(),
                activeRulesCount = 0,
                onEntryClick = {},
                onEntryLongClick = {},
                onMocksClick = {},
            )
        }
    }
}

private fun sampleRules() = persistentListOf(
    MockRule(0, "/api/v1/checkout", "POST", 500, """{"error":"Service unavailable"}""", delayMs = 0),
    MockRule(1, "/api/v1/products", null, 200, """{"items":[]}""", delayMs = 2000, isEnabled = false),
)

@PreviewLightDark
@Composable
private fun MocksScreenPreview() {
    WickKitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MocksScreen(
                rules = sampleRules(),
                onBack = {},
                onToggle = {},
                onDelete = {},
                onEdit = {},
                onAddNew = {},
            )
        }
    }
}
