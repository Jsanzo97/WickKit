package io.wickkit.overlay.ui.tab

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.wickkit.network.NetworkEntry
import io.wickkit.overlay.ui.WickKitTheme
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toPersistentMap

@Composable
internal fun NetworkDetailScreen(
    entry: NetworkEntry,
    onBack: () -> Unit,
    onMockRequest: (NetworkEntry) -> Unit,
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        DetailToolbar(method = entry.method, url = entry.url, onBack = onBack)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusRow(entry = entry)

            if (entry.requestHeaders.isNotEmpty()) {
                HeadersSection(
                    title = "Request Headers",
                    headers = entry.requestHeaders.toPersistentMap(),
                )
            }
            if (!entry.requestBody.isNullOrBlank()) {
                BodySection(title = "Request Body", body = entry.requestBody, context = context)
            }

            if (entry.error != null) {
                ErrorSection(error = entry.error)
            } else {
                if (entry.responseHeaders.isNotEmpty()) {
                    HeadersSection(
                        title = "Response Headers",
                        headers = entry.responseHeaders.toPersistentMap(),
                    )
                }
                if (!entry.responseBody.isNullOrBlank()) {
                    BodySection(title = "Response Body", body = entry.responseBody, context = context)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = null, indication = null) { onMockRequest(entry) }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Mock this request",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DetailToolbar(method: String, url: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        MethodBadge(method = method)
        Text(
            text = url,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatusRow(entry: NetworkEntry) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when {
            entry.isMocked -> {
                StatusCodeBadge(code = entry.statusCode ?: 0)
                MockedBadge()
            }

            entry.error != null -> {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF9E9E9E).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "ERR",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF9E9E9E),
                    )
                }
            }

            else -> StatusCodeBadge(code = entry.statusCode ?: 0)
        }
        Text(
            text = "${formatDuration(entry.durationMs)} · ${entry.time}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun MockedBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF9C27B0).copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "MOCKED",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF9C27B0),
        )
    }
}

@Composable
private fun HeadersSection(
    title: String,
    headers: ImmutableMap<String, String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = title)
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                headers.entries.forEach { (key, value) ->
                    Row {
                        Text(
                            text = "$key: ",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BodySection(
    title: String,
    body: String,
    context: Context,
) {
    val formatted = prettyJson(body)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionHeader(title = title, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(interactionSource = null, indication = null) {
                        copyToClipboard(context, body)
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Copy",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp),
        ) {
            SelectionContainer {
                Text(
                    text = formatted,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                )
            }
        }
    }
}

@Composable
private fun ErrorSection(error: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = "Error")
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFEF5350),
        )
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing,
        ),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = modifier,
    )
}

private fun prettyJson(body: String): String = try {
    when {
        body.trimStart().startsWith("{") -> org.json.JSONObject(body).toString(2)
        body.trimStart().startsWith("[") -> org.json.JSONArray(body).toString(2)
        else -> body
    }
} catch (_: Exception) {
    body
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("body", text))
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

@PreviewLightDark
@Composable
private fun NetworkDetailPreview() {
    WickKitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            NetworkDetailScreen(
                entry = NetworkEntry(
                    id = 0,
                    method = "POST",
                    url = "https://api.example.com/v1/users/create",
                    requestHeaders = mapOf(
                        "Content-Type" to "application/json",
                        "Authorization" to "Bearer eyJhbGciOiJSUzI1NiJ9...",
                    ),
                    requestBody = """{"name":"Jorge","email":"jorge@example.com"}""",
                    statusCode = 201,
                    responseHeaders = mapOf("Content-Type" to "application/json"),
                    responseBody = """{"id":42,"name":"Jorge","createdAt":"2026-01-01"}""",
                    durationMs = 340,
                    time = "10:23:45",
                    error = null,
                ),
                onBack = {},
                onMockRequest = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun NetworkDetailMockedPreview() {
    WickKitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            NetworkDetailScreen(
                entry = NetworkEntry(
                    id = 1,
                    method = "GET",
                    url = "https://api.example.com/v1/products",
                    requestHeaders = emptyMap(),
                    requestBody = null,
                    statusCode = 200,
                    responseHeaders = emptyMap(),
                    responseBody = """{"items":[]}""",
                    durationMs = 0,
                    time = "10:23:46",
                    error = null,
                    isMocked = true,
                ),
                onBack = {},
                onMockRequest = {},
            )
        }
    }
}
