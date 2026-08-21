package io.wickkit.overlay.ui.tab

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.view.WindowManager
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
import java.util.Locale
import java.util.TimeZone

private data class DeviceInfoSection(val title: String, val items: List<Pair<String, String>>)

@Composable
internal fun DeviceTab() {
    val context = LocalContext.current
    val sections = remember(context) { buildDeviceInfo(context) }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        sections.forEach { section ->
            item(key = section.title) { DeviceSectionHeader(title = section.title) }
            items(section.items, key = { "${section.title}/${it.first}" }) { (label, value) ->
                DeviceInfoRow(label = label, value = value)
            }
        }
    }
}

@Composable
private fun DeviceSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun DeviceInfoRow(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
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
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        )
    }
}

private fun buildDeviceInfo(context: Context): List<DeviceInfoSection> = listOf(
    buildAppSection(context),
    buildDeviceSection(),
    buildDisplaySection(context),
    buildMemorySection(context),
    buildStorageSection(),
    buildLocaleSection(),
)

private fun buildAppSection(context: Context): DeviceInfoSection {
    val isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val pi = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
    val version = pi?.let {
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            it.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            it.versionCode.toLong()
        }
        "${it.versionName} ($code)"
    } ?: "N/A"
    return DeviceInfoSection(
        title = "App",
        items = listOf(
            "Package" to context.packageName,
            "Version" to version,
            "Build type" to if (isDebug) "debug" else "release",
        ),
    )
}

private fun buildDeviceSection(): DeviceInfoSection = DeviceInfoSection(
    title = "Device",
    items = listOf(
        "Manufacturer" to Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
        "Model" to Build.MODEL,
        "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "ABI" to (Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"),
        "CPU cores" to "${Runtime.getRuntime().availableProcessors()}",
    ),
)

private fun buildDisplaySection(context: Context): DeviceInfoSection {
    val dm = context.resources.displayMetrics
    val refreshRate = runCatching {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        }
        display?.let { "${it.refreshRate.toInt()} Hz" } ?: "N/A"
    }.getOrElse { "N/A" }
    val fontScale = "×%.1f".format(dm.scaledDensity / dm.density)
    val orientation = if (dm.widthPixels < dm.heightPixels) "portrait" else "landscape"
    return DeviceInfoSection(
        title = "Display",
        items = listOf(
            "Resolution" to "${dm.widthPixels} × ${dm.heightPixels} px",
            "Density" to "${dm.densityDpi} dpi (×${dm.density})",
            "Refresh rate" to refreshRate,
            "Font scale" to fontScale,
            "Orientation" to orientation,
        ),
    )
}

private fun buildMemorySection(context: Context): DeviceInfoSection {
    val memInfo = runCatching {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
    }.getOrNull()
    return DeviceInfoSection(
        title = "Memory",
        items = listOf(
            "Max heap" to "${Runtime.getRuntime().maxMemory() / 1024 / 1024} MB",
            "Total RAM" to (memInfo?.let { "${it.totalMem / 1024 / 1024} MB" } ?: "N/A"),
            "Available RAM" to (memInfo?.let { "${it.availMem / 1024 / 1024} MB" } ?: "N/A"),
        ),
    )
}

private fun buildStorageSection(): DeviceInfoSection {
    val stat = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
    return DeviceInfoSection(
        title = "Storage (internal)",
        items = listOf(
            "Free" to (stat?.let { "${it.availableBytes / 1024 / 1024} MB" } ?: "N/A"),
            "Total" to (stat?.let { "${it.totalBytes / 1024 / 1024} MB" } ?: "N/A"),
        ),
    )
}

private fun buildLocaleSection(): DeviceInfoSection = DeviceInfoSection(
    title = "Locale",
    items = listOf(
        "Language" to Locale.getDefault().toLanguageTag(),
        "Timezone" to TimeZone.getDefault().id,
    ),
)

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun DeviceTabPreview() {
    WickKitTheme {
        DeviceTab()
    }
}
