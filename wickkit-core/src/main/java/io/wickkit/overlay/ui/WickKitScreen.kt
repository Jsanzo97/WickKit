package io.wickkit.overlay.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.wickkit.overlay.ui.tab.DatabaseTab
import io.wickkit.overlay.ui.tab.DeviceTab
import io.wickkit.overlay.ui.tab.FlagsTab
import io.wickkit.overlay.ui.tab.LogsTab
import io.wickkit.overlay.ui.tab.MemoryLeaksTab
import io.wickkit.overlay.ui.tab.NetworkTab
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private enum class WickKitTab(val label: String) {
    Logs("Logs"),
    Network("Network"),
    Database("Database"),
    Flags("Flags"),
    Leaks("Leaks"),
    Device("Device"),
}

private const val SCRIM_ALPHA = 0.65f
private const val PANEL_HEIGHT_FRACTION = 0.88f
private const val OPEN_MS = 280
private const val CLOSE_MS = 220L

@Composable
internal fun WickKitScreen(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(WickKitTab.Logs) }

    LaunchedEffect(Unit) { visible = true }

    fun animateClose() {
        scope.launch {
            visible = false
            delay(CLOSE_MS.milliseconds)
            onClose()
        }
    }

    BackHandler(onBack = ::animateClose)

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(OPEN_MS)),
            exit = fadeOut(animationSpec = tween(CLOSE_MS.toInt())),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                    .clickable(interactionSource = null, indication = null) { animateClose() },
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(CLOSE_MS.toInt()),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            DebugPanel(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onClose = ::animateClose,
            )
        }
    }
}

@Composable
private fun DebugPanel(
    selectedTab: WickKitTab,
    onTabSelected: (WickKitTab) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(PANEL_HEIGHT_FRACTION)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(MaterialTheme.colorScheme.background)
            .clickable(interactionSource = null, indication = null) { }
            .navigationBarsPadding(),
    ) {
        PanelHandle()
        PanelHeader(onClose = onClose)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        PanelTabs(selected = selectedTab, onSelect = onTabSelected)
        when (selectedTab) {
            WickKitTab.Logs -> LogsTab()
            WickKitTab.Network -> NetworkTab()
            WickKitTab.Database -> DatabaseTab()
            WickKitTab.Flags -> FlagsTab()
            WickKitTab.Leaks -> MemoryLeaksTab()
            WickKitTab.Device -> DeviceTab()
        }
    }
}

@Composable
private fun PanelHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
        )
    }
}

@Composable
private fun PanelHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "WickKit",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Debug Panel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun PanelTabs(selected: WickKitTab, onSelect: (WickKitTab) -> Unit) {
    val indicatorColor = MaterialTheme.colorScheme.primary
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            WickKitTab.entries.forEach { tab ->
                val isSelected = selected == tab
                Box(
                    modifier = Modifier
                        .clickable { onSelect(tab) }
                        .height(48.dp)
                        .drawBehind {
                            if (isSelected) {
                                drawRect(
                                    color = indicatorColor,
                                    topLeft = Offset(0f, size.height - 2.dp.toPx()),
                                    size = Size(size.width, 2.dp.toPx()),
                                )
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = tab.label,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) selectedColor else unselectedColor,
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

@PreviewLightDark
@Composable
private fun DebugPanelLogsPreview() {
    WickKitTheme {
        DebugPanel(
            selectedTab = WickKitTab.Logs,
            onTabSelected = {},
            onClose = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun DebugPanelDevicePreview() {
    WickKitTheme {
        DebugPanel(
            selectedTab = WickKitTab.Device,
            onTabSelected = {},
            onClose = {},
        )
    }
}
