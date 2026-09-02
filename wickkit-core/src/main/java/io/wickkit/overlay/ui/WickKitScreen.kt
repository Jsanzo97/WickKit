package io.wickkit.overlay.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.wickkit.core.R
import io.wickkit.overlay.ui.tab.CrashesTab
import io.wickkit.overlay.ui.tab.DatabaseTab
import io.wickkit.overlay.ui.tab.DeviceTab
import io.wickkit.overlay.ui.tab.LogsTab
import io.wickkit.overlay.ui.tab.MemoryLeaksTab
import io.wickkit.overlay.ui.tab.NetworkTab
import io.wickkit.overlay.ui.tab.PerformanceTab
import io.wickkit.overlay.ui.tab.ThreadsTab
import io.wickkit.overlay.ui.tab.WickKitFlagsTabSlot
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private object WickKitOverlayState {
    var lastTab: WickKitTab = WickKitTab.Logs
}

private enum class WickKitTab(@StringRes val labelRes: Int) {
    Logs(R.string.wk_tab_logs),
    Network(R.string.wk_tab_network),
    Database(R.string.wk_tab_database),
    Flags(R.string.wk_tab_flags),
    Leaks(R.string.wk_tab_leaks),
    Crashes(R.string.wk_tab_crashes),
    Performance(R.string.wk_tab_performance),
    Threads(R.string.wk_tab_threads),
    Device(R.string.wk_tab_device),
}

private const val SCRIM_ALPHA = 0.65f
private const val PANEL_HEIGHT_FRACTION = 0.88f
private const val OPEN_MS = 280
private const val CLOSE_MS = 220L
private const val DISMISS_VELOCITY_THRESHOLD = 1_500f
private const val DISMISS_FRACTION_THRESHOLD = 0.4f

@Composable
internal fun WickKitScreen(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val scrimVisible = remember { MutableTransitionState(false).also { it.targetState = true } }
    val panelEntryState = remember { MutableTransitionState(false).also { it.targetState = true } }
    val panelOffsetY = remember { Animatable(0f) }
    var panelHeightPx by remember { mutableStateOf(0f) }
    var selectedTab by remember { mutableStateOf(WickKitOverlayState.lastTab) }

    suspend fun close() {
        scrimVisible.targetState = false
        panelOffsetY.animateTo(
            targetValue = panelHeightPx,
            animationSpec = tween(CLOSE_MS.toInt()),
        )
        onClose()
    }

    fun animateClose() {
        scope.launch { close() }
    }

    BackHandler(onBack = ::animateClose)

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visibleState = scrimVisible,
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
            visibleState = panelEntryState,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
            ),
            exit = ExitTransition.None,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { panelHeightPx = it.height.toFloat() }
                .offset { IntOffset(x = 0, y = panelOffsetY.value.roundToInt()) }
                .draggable(
                    orientation = Orientation.Vertical,
                    enabled = panelEntryState.isIdle,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            panelOffsetY.snapTo(
                                targetValue = (panelOffsetY.value + delta).coerceAtLeast(0f),
                            )
                        }
                    },
                    onDragStopped = { velocity ->
                        scope.launch {
                            if (
                                velocity > DISMISS_VELOCITY_THRESHOLD ||
                                panelOffsetY.value > panelHeightPx * DISMISS_FRACTION_THRESHOLD
                            ) {
                                close()
                            } else {
                                panelOffsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.8f,
                                        stiffness = Spring.StiffnessMedium,
                                    ),
                                )
                            }
                        }
                    },
                ),
        ) {
            DebugPanel(
                selectedTab = selectedTab,
                onTabSelected = {
                    selectedTab = it
                    WickKitOverlayState.lastTab = it
                },
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

            WickKitTab.Flags -> WickKitFlagsTabSlot.content?.invoke() ?: Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.wk_flags_module_inactive),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            WickKitTab.Leaks -> MemoryLeaksTab()

            WickKitTab.Crashes -> CrashesTab()

            WickKitTab.Performance -> PerformanceTab()

            WickKitTab.Threads -> ThreadsTab()

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
                text = stringResource(R.string.wk_debug_panel_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.wk_cd_close),
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
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            WickKitTab.entries.chunked(2).forEach { pair ->
                Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                    pair.forEach { tab ->
                        val isSelected = selected == tab
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clickable { onSelect(tab) }
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
                                text = stringResource(tab.labelRes),
                                modifier = Modifier.padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) selectedColor else unselectedColor,
                                maxLines = 1,
                            )
                        }
                    }
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
