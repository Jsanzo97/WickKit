package io.wickkit.overlay.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp

// ── Dark palette ─────────────────────────────────────────────────────────────

private val DarkPrimary = Color(0xFF4E7CF6)
private val DarkPrimaryContainer = Color(0xFF152244)
private val DarkBackground = Color(0xFF0F1117)
private val DarkSurface = Color(0xFF181B22)
private val DarkSurfaceVariant = Color(0xFF1F232C)
private val DarkOnSurface = Color(0xFFE2E4EC)
private val DarkOnSurfaceVariant = Color(0xFF8C93A8)
private val DarkOutline = Color(0xFF2C3040)
private val DarkError = Color(0xFFEF5656)

private val WickKitDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color.White,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkPrimary,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = DarkError,
)

// ── Light palette ─────────────────────────────────────────────────────────────

private val LightPrimary = Color(0xFF1A5CC8)
private val LightPrimaryContainer = Color(0xFFD8E6FF)
private val LightBackground = Color(0xFFF3F5FB)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFEBEDF5)
private val LightOnSurface = Color(0xFF1A1C23)
private val LightOnSurfaceVariant = Color(0xFF5C6380)
private val LightOutline = Color(0xFFCDD0DA)
private val LightError = Color(0xFFD32F2F)

private val WickKitLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightPrimary,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = LightError,
)

// ── Theme ─────────────────────────────────────────────────────────────────────

@Composable
internal fun WickKitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) WickKitDarkColorScheme else WickKitLightColorScheme,
        content = content,
    )
}

// ── Preview ───────────────────────────────────────────────────────────────────

@PreviewLightDark
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
                    text = "WickKit",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Debug Panel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Secondary text",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
