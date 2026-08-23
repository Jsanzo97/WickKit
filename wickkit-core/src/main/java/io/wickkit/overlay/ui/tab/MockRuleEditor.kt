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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.wickkit.network.MockRule
import io.wickkit.network.NetworkEntry
import io.wickkit.overlay.ui.WickKitTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

private val EDITOR_METHODS = listOf("GET", "POST", "PUT", "DELETE", "PATCH")
private val EditorFieldHeight = 36.dp
private val BodyFieldMinHeight = 128.dp

@Suppress("CyclomaticComplexMethod")
@Composable
internal fun MockRuleEditor(
    rule: MockRule?,
    prefillFrom: NetworkEntry?,
    onBack: () -> Unit,
    onSave: (MockRule) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val isFromEntry = prefillFrom != null
    var urlPattern by remember { mutableStateOf(rule?.urlPattern ?: prefillFrom?.url ?: "") }
    var selectedMethod by remember { mutableStateOf(rule?.method ?: prefillFrom?.method) }
    var statusCode by remember {
        mutableStateOf(rule?.statusCode?.toString() ?: prefillFrom?.statusCode?.toString() ?: "200")
    }
    var responseBody by remember { mutableStateOf(rule?.responseBody ?: prefillFrom?.responseBody ?: "") }
    var delayMs by remember { mutableStateOf(rule?.delayMs?.toString() ?: "0") }
    var jsonError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
    ) {
        EditorToolbar(
            isEditing = rule != null,
            onBack = onBack,
            onSave = {
                if (responseBody.isNotBlank() && !isValidJson(responseBody)) {
                    jsonError = true
                } else {
                    jsonError = false
                    onSave(
                        MockRule(
                            id = rule?.id ?: 0L,
                            urlPattern = urlPattern,
                            method = selectedMethod,
                            statusCode = statusCode.toIntOrNull() ?: 200,
                            responseBody = responseBody.takeIf { it.isNotBlank() },
                            delayMs = delayMs.toLongOrNull() ?: 0L,
                            isEnabled = rule?.isEnabled ?: true,
                        ),
                    )
                }
            },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EditorTextField(
                label = "URL Pattern",
                hint = "e.g. /api/v1/users",
                value = urlPattern,
                onValueChange = { urlPattern = it },
            )

            MethodSelectorRow(
                selected = selectedMethod,
                onSelect = { selectedMethod = it },
                enabled = !isFromEntry,
            )

            EditorTextField(
                label = "Status Code",
                hint = "200",
                value = statusCode,
                onValueChange = { statusCode = it },
                keyboardType = KeyboardType.Number,
            )

            BodyTextField(
                value = responseBody,
                onValueChange = {
                    responseBody = it
                    jsonError = false
                },
                hasError = jsonError,
            )

            EditorTextField(
                label = "Delay (ms)",
                hint = "0",
                value = delayMs,
                onValueChange = { delayMs = it },
                keyboardType = KeyboardType.Number,
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EditorToolbar(
    isEditing: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
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
            text = if (isEditing) "Edit Mock Rule" else "New Mock Rule",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(interactionSource = null, indication = null) { onSave() }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Save",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun MethodSelectorRow(
    selected: String?,
    onSelect: (String?) -> Unit,
    enabled: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FieldLabel(text = "Method")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MethodSelectorChip(
                label = "ALL",
                isSelected = selected == null,
                color = MaterialTheme.colorScheme.onSurface,
                onClick = { onSelect(null) },
                enabled = enabled,
            )
            EDITOR_METHODS.forEach { method ->
                MethodSelectorChip(
                    label = method,
                    isSelected = selected == method,
                    color = methodColor(method),
                    onClick = { onSelect(if (selected == method) null else method) },
                    enabled = enabled,
                )
            }
        }
    }
}

@Composable
private fun MethodSelectorChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .alpha(if (!enabled && !isSelected) 0.35f else 1f)
            .background(if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (isSelected) color.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(enabled = enabled, interactionSource = null, indication = null) { onClick() }
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
private fun EditorTextField(
    label: String,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FieldLabel(text = label)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(EditorFieldHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BodyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hasError: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FieldLabel(text = "Response Body (JSON)")
            if (hasError) {
                Text(
                    text = "Invalid format",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFEF5350),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = BodyFieldMinHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    text = """{"key": "value"}""",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
    )
}

private fun isValidJson(text: String): Boolean = try {
    val element = Json.parseToJsonElement(text.trim())
    element is JsonObject || element is JsonArray
} catch (_: Exception) {
    false
}

@PreviewLightDark
@Composable
private fun MockRuleEditorNewPreview() {
    WickKitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MockRuleEditor(
                rule = null,
                prefillFrom = NetworkEntry(
                    id = 0,
                    method = "GET",
                    url = "https://api.example.com/v1/products",
                    requestHeaders = emptyMap(),
                    requestBody = null,
                    statusCode = 200,
                    responseHeaders = emptyMap(),
                    responseBody = null,
                    durationMs = 0,
                    time = "",
                    error = null,
                ),
                onBack = {},
                onSave = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun MockRuleEditorEditPreview() {
    WickKitTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MockRuleEditor(
                rule = MockRule(
                    id = 0,
                    urlPattern = "/api/v1/checkout",
                    method = "POST",
                    statusCode = 500,
                    responseBody = """{"error":"Service unavailable"}""",
                    delayMs = 1000,
                ),
                prefillFrom = null,
                onBack = {},
                onSave = {},
            )
        }
    }
}
