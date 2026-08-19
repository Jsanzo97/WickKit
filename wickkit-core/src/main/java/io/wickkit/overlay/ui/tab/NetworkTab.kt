package io.wickkit.overlay.ui.tab

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.wickkit.overlay.ui.WickKitTheme

@Composable
internal fun NetworkTab() {
    PlaceholderTab(
        title = "Network",
        description = "Intercepts OkHttp traffic via bytecode injection — no setup needed. " +
            "Inspect requests, responses, and mock endpoints.",
        features = listOf(
            "Request/response inspector (headers, body, timing)",
            "Zero-setup OkHttp interception via Gradle plugin",
            "Response mocking by URL pattern or regex",
            "Status code and latency badges",
            "Export traffic as HAR file",
        ),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun NetworkTabPreview() {
    WickKitTheme {
        NetworkTab()
    }
}
