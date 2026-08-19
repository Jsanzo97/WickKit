package io.wickkit.overlay.ui.tab

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.wickkit.overlay.ui.WickKitTheme

@Composable
internal fun LogsTab() {
    PlaceholderTab(
        title = "Logs",
        description = "Captures Logcat output and auto-plants a Timber tree. " +
            "Filter by level, tag, or search across history.",
        features = listOf(
            "Real-time Logcat stream",
            "Timber auto-plant via WickKit.init()",
            "Filter by level: V / D / I / W / E",
            "Full-text search across log history",
            "Copy and share log entries",
        ),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun LogsTabPreview() {
    WickKitTheme {
        LogsTab()
    }
}
