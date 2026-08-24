package io.wickkit.overlay.ui.tab

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.TypedValue
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
import io.wickkit.core.R
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
    buildDeviceSection(context),
    buildDisplaySection(context),
    buildMemorySection(context),
    buildStorageSection(context),
    buildLocaleSection(context),
)

private fun buildAppSection(context: Context): DeviceInfoSection {
    val isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val packageInfo = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
    val version = packageInfo?.let {
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            it.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            it.versionCode.toLong()
        }
        "${it.versionName} ($code)"
    } ?: "N/A"
    return DeviceInfoSection(
        title = context.getString(R.string.wk_device_section_app),
        items = listOf(
            context.getString(R.string.wk_device_label_package) to context.packageName,
            context.getString(R.string.wk_device_label_version) to version,
            context.getString(R.string.wk_device_label_build_type) to if (isDebug) {
                context.getString(R.string.wk_device_value_debug)
            } else {
                context.getString(R.string.wk_device_value_release)
            },
        ),
    )
}

private fun buildDeviceSection(context: Context): DeviceInfoSection = DeviceInfoSection(
    title = context.getString(R.string.wk_device_section_device),
    items = listOf(
        context.getString(R.string.wk_device_label_manufacturer) to
            Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
        context.getString(R.string.wk_device_label_model) to Build.MODEL,
        context.getString(R.string.wk_device_label_android) to
            "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        context.getString(R.string.wk_device_label_abi) to
            (Build.SUPPORTED_ABIS.firstOrNull() ?: context.getString(R.string.wk_device_value_unknown)),
        context.getString(R.string.wk_device_label_cpu_cores) to "${Runtime.getRuntime().availableProcessors()}",
    ),
)

private fun buildDisplaySection(context: Context): DeviceInfoSection {
    val displayMetrics = context.resources.displayMetrics
    val refreshRate = runCatching {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        }
        display?.let { "${it.refreshRate.toInt()} Hz" } ?: "N/A"
    }.getOrElse { "N/A" }
    val fontScale = "×%.1f".format(
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 1f, displayMetrics) / displayMetrics.density,
    )
    val orientation = if (displayMetrics.widthPixels < displayMetrics.heightPixels) {
        context.getString(R.string.wk_device_value_portrait)
    } else {
        context.getString(R.string.wk_device_value_landscape)
    }
    return DeviceInfoSection(
        title = context.getString(R.string.wk_device_section_display),
        items = listOf(
            context.getString(R.string.wk_device_label_resolution) to
                "${displayMetrics.widthPixels} × ${displayMetrics.heightPixels} px",
            context.getString(R.string.wk_device_label_density) to
                "${displayMetrics.densityDpi} dpi (×${displayMetrics.density})",
            context.getString(R.string.wk_device_label_refresh_rate) to refreshRate,
            context.getString(R.string.wk_device_label_font_scale) to fontScale,
            context.getString(R.string.wk_device_label_orientation) to orientation,
        ),
    )
}

private fun buildMemorySection(context: Context): DeviceInfoSection {
    val memoryInfo = runCatching {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        ActivityManager.MemoryInfo().also { info -> activityManager.getMemoryInfo(info) }
    }.getOrNull()
    return DeviceInfoSection(
        title = context.getString(R.string.wk_device_section_memory),
        items = listOf(
            context.getString(R.string.wk_device_label_max_heap) to
                "${Runtime.getRuntime().maxMemory() / 1024 / 1024} MB",
            context.getString(R.string.wk_device_label_total_ram) to
                (memoryInfo?.let { "${it.totalMem / 1024 / 1024} MB" } ?: "N/A"),
            context.getString(R.string.wk_device_label_available_ram) to
                (memoryInfo?.let { "${it.availMem / 1024 / 1024} MB" } ?: "N/A"),
        ),
    )
}

private fun buildStorageSection(context: Context): DeviceInfoSection {
    val statFs = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
    return DeviceInfoSection(
        title = context.getString(R.string.wk_device_section_storage),
        items = listOf(
            context.getString(R.string.wk_device_label_storage_free) to
                (statFs?.let { "${it.availableBytes / 1024 / 1024} MB" } ?: "N/A"),
            context.getString(R.string.wk_device_label_storage_total) to
                (statFs?.let { "${it.totalBytes / 1024 / 1024} MB" } ?: "N/A"),
        ),
    )
}

private fun buildLocaleSection(context: Context): DeviceInfoSection = DeviceInfoSection(
    title = context.getString(R.string.wk_device_section_locale),
    items = listOf(
        context.getString(R.string.wk_device_label_language) to Locale.getDefault().toLanguageTag(),
        context.getString(R.string.wk_device_label_timezone) to TimeZone.getDefault().id,
    ),
)

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun DeviceTabPreview() {
    WickKitTheme {
        DeviceTab()
    }
}
