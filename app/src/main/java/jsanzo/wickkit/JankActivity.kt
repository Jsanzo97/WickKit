package jsanzo.wickkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.wickkit.WickKit
import io.wickkit.compose.WickKitComposeTracker
import jsanzo.wickkit.ui.theme.WickKitTheme
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

private const val RECOMPOSE_INTERVAL_MS = 16L
private const val JANK_BLOCK_MS = 300L
private const val CANVAS_SPOKES = 360

internal class JankActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WickKitTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    JankContent(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
private fun JankContent(modifier: Modifier = Modifier) {
    // Manual tracker call — simulates what the WickKit Gradle plugin injects at compile time.
    WickKitComposeTracker.onRecompose("JankContent")
    val context = LocalContext.current
    var recomposeCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(RECOMPOSE_INTERVAL_MS.milliseconds)
            recomposeCount++
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Performance Simulator")
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Local recompositions: $recomposeCount",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(24.dp))
        SpinningCanvas(tick = recomposeCount)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                // Intentionally blocks the main thread to produce slow frames — demo only.
                Thread.sleep(JANK_BLOCK_MS)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Force Jank Frame (${JANK_BLOCK_MS.toInt()} ms block)")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { WickKit.open(context) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open WickKit Overlay")
        }
    }
}

@Composable
private fun SpinningCanvas(tick: Int) {
    // Manual tracker call — simulates what the WickKit Gradle plugin injects at compile time.
    WickKitComposeTracker.onRecompose("SpinningCanvas")
    Canvas(modifier = Modifier.size(160.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 2f - 8.dp.toPx()
        val baseAngle = tick % CANVAS_SPOKES * (Math.PI / 180.0)
        repeat(CANVAS_SPOKES) { i ->
            val angle = baseAngle + i * (Math.PI / (CANVAS_SPOKES / 2.0))
            val alpha = (i.toFloat() / CANVAS_SPOKES).coerceIn(0f, 1f)
            drawLine(
                color = Color(0xFF6200EE).copy(alpha = alpha * 0.6f),
                start = center,
                end = Offset(
                    x = center.x + (cos(angle) * radius).toFloat(),
                    y = center.y + (sin(angle) * radius).toFloat(),
                ),
                strokeWidth = 1.5f,
            )
        }
    }
}
