package io.wickkit.overlay.ui.tab

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.wickkit.overlay.ui.WickKitTheme

@Composable
internal fun FlagsTab() {
    PlaceholderTab(
        title = "Feature Flags",
        description = "Toggle flags at runtime without rebuilding. Overrides propagate immediately across the app.",
        features = listOf(
            "Boolean, string, and integer flag types",
            "Override remote flags locally for testing",
            "Persistent overrides across app restarts",
            "Flag history and audit trail",
            "Export/import flag state as JSON",
        ),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun FlagsTabPreview() {
    WickKitTheme {
        FlagsTab()
    }
}
