package io.wickkit.overlay.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private val Background = Color(0xFF0F1117)
private val SurfaceColor = Color(0xFF181B22)
private val SurfaceVariant = Color(0xFF1F232C)
private val Primary = Color(0xFF7C6AF7)
private val PrimaryContainer = Color(0xFF201C45)
private val OnPrimary = Color(0xFFFFFFFF)
private val OnSurface = Color(0xFFE2E4EC)
private val OnSurfaceVariant = Color(0xFF8C93A8)
private val Outline = Color(0xFF2C3040)
private val Error = Color(0xFFEF5656)

private val WickKitColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = Primary,
    background = Background,
    onBackground = OnSurface,
    surface = SurfaceColor,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    error = Error,
)

@Composable
internal fun WickKitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WickKitColorScheme,
        content = content,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun WickKitThemePreview() {
    WickKitTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "WickKit",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Debug Panel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Secondary text",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
