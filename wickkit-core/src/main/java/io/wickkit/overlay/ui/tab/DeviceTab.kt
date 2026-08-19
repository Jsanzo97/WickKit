package io.wickkit.overlay.ui.tab

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.wickkit.overlay.ui.WickKitTheme

@Composable
internal fun DeviceTab() {
    val context = LocalContext.current
    val items = remember(context) { buildDeviceInfo(context) }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items) { (label, value) ->
            DeviceInfoRow(label = label, value = value)
        }
    }
}

@Composable
private fun DeviceInfoRow(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        )
    }
}

private fun buildDeviceInfo(context: Context): List<Pair<String, String>> {
    val dm = context.resources.displayMetrics
    val memInfo = runCatching {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
    }.getOrNull()
    return listOf(
        "Manufacturer" to Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
        "Model" to Build.MODEL,
        "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "Resolution" to "${dm.widthPixels} × ${dm.heightPixels} px",
        "Density" to "${dm.densityDpi} dpi (×${dm.density})",
        "ABI" to (Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"),
        "Max Heap" to "${Runtime.getRuntime().maxMemory() / 1024 / 1024} MB",
        "Total RAM" to (memInfo?.let { "${it.totalMem / 1024 / 1024} MB" } ?: "N/A"),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun DeviceTabPreview() {
    WickKitTheme {
        DeviceTab()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun DeviceInfoRowPreview() {
    WickKitTheme {
        Column {
            DeviceInfoRow(label = "Model", value = "Pixel 8 Pro")
            DeviceInfoRow(label = "Android", value = "15 (API 35)")
            DeviceInfoRow(label = "Max Heap", value = "512 MB")
        }
    }
}
