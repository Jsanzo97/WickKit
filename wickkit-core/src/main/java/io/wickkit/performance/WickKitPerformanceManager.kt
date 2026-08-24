package io.wickkit.performance

import android.app.Activity
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.Recomposer
import io.wickkit.compose.WickKitComposeTracker
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val POLL_INTERVAL_MS = 2_000L
private const val MAX_FRAME_SAMPLES = 600

internal object WickKitPerformanceManager {

    val snapshot: StateFlow<PerformanceSnapshot>
        field = MutableStateFlow(PerformanceSnapshot())

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastRecompositionCount = 0L
    private var lastPollTimeMs = 0L

    // Choreographer tracks vsync intervals on the main thread; frameDurations is guarded by frameLock.
    @Volatile private var isTracking = false

    @Volatile private var appInForeground = false
    private val frameDurations = ArrayDeque<Long>(MAX_FRAME_SAMPLES)
    private val frameLock = Any()
    private var lastFrameTimeNs = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (lastFrameTimeNs > 0L) {
                val durationMs = (frameTimeNanos - lastFrameTimeNs) / 1_000_000L
                synchronized(frameLock) {
                    if (frameDurations.size >= MAX_FRAME_SAMPLES) frameDurations.removeFirst()
                    frameDurations.addLast(durationMs)
                }
            }
            lastFrameTimeNs = frameTimeNanos
            if (isTracking) Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun start() {
        scope.launch {
            while (true) {
                delay(POLL_INTERVAL_MS.milliseconds)
                if (appInForeground) {
                    collectAndUpdateRuntimeStats()
                    if (isTracking) collectLiveFrameStats()
                }
            }
        }
    }

    fun onActivityStarted() {
        appInForeground = true
    }

    fun onActivityStopped() {
        isTracking = false
        appInForeground = false
    }

    fun onActivityResumed(activity: Activity) {
        synchronized(frameLock) { frameDurations.clear() }
        lastFrameTimeNs = 0L
        isTracking = true
        snapshot.update {
            it.copy(
                activityName = activity.javaClass.simpleName,
                fps = null,
                slowFrames = 0,
                frozenFrames = 0,
                jankRate = null,
                p50Ms = null,
                p90Ms = null,
                totalFrames = 0,
            )
        }
        Handler(Looper.getMainLooper()).post {
            // Remove before posting to prevent duplicates if overlay was open and Choreographer was already running.
            Choreographer.getInstance().removeFrameCallback(frameCallback)
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    fun onActivityPaused() {
        // isTracking stays true — Choreographer keeps running so the overlay shows live frame updates.
        val frameData = synchronized(frameLock) {
            frameDurations.groupBy { it.toInt() }.map { (durationMs, frames) -> durationMs to frames.size }
        }
        updateFrameStats(parseFrameData(frameData))
    }

    internal fun updateFrameStats(stats: FrameStats?) {
        snapshot.update { current ->
            if (stats == null) {
                current.copy(
                    fps = null,
                    slowFrames = 0,
                    frozenFrames = 0,
                    jankRate = null,
                    p50Ms = null,
                    p90Ms = null,
                    totalFrames = 0,
                )
            } else {
                current.copy(
                    fps = stats.fps,
                    slowFrames = stats.slowFrames,
                    frozenFrames = stats.frozenFrames,
                    jankRate = stats.jankRate,
                    p50Ms = stats.p50Ms,
                    p90Ms = stats.p90Ms,
                    totalFrames = stats.totalFrames,
                )
            }
        }
    }

    internal fun updateRuntimeStats(stats: RuntimeStats) {
        val now = System.currentTimeMillis()
        val elapsed = now - lastPollTimeMs
        val delta = stats.recompositions - lastRecompositionCount
        val perSecond = if (lastPollTimeMs > 0L && elapsed > 0L) {
            (delta * 1_000f / elapsed).coerceAtLeast(0f)
        } else {
            0f
        }
        lastRecompositionCount = stats.recompositions
        lastPollTimeMs = now

        snapshot.update { current ->
            current.copy(
                recompositionCount = stats.recompositions,
                recompositionsPerSecond = perSecond,
                threadCount = stats.threads,
                jvmUsedMb = stats.jvmUsedMb,
                jvmMaxMb = stats.jvmMaxMb,
                nativeHeapMb = stats.nativeHeapMb,
                composableEntries = stats.composableEntries,
                composableTrackingActive = stats.composableTrackingActive,
            )
        }
    }

    fun reset() {
        isTracking = false
        appInForeground = false
        synchronized(frameLock) { frameDurations.clear() }
        lastFrameTimeNs = 0L
        lastRecompositionCount = 0L
        lastPollTimeMs = 0L
        snapshot.value = PerformanceSnapshot()
    }

    @OptIn(InternalComposeApi::class)
    private fun collectAndUpdateRuntimeStats() {
        val recompositions = runCatching {
            Recomposer.runningRecomposers.value.sumOf { it.changeCount }
        }.getOrElse { 0L }
        val runtime = Runtime.getRuntime()
        val jvmUsedMb = (runtime.totalMemory() - runtime.freeMemory()) / 1024L / 1024L
        val jvmMaxMb = runtime.maxMemory() / 1024L / 1024L
        val nativeHeapMb = Debug.getNativeHeapAllocatedSize() / 1024L / 1024L
        val composableEntries = runCatching {
            WickKitComposeTracker.computeEntries()
        }.getOrElse {
            persistentListOf()
        }
        updateRuntimeStats(
            RuntimeStats(
                recompositions = recompositions,
                threads = Thread.activeCount(),
                jvmUsedMb = jvmUsedMb,
                jvmMaxMb = jvmMaxMb,
                nativeHeapMb = nativeHeapMb,
                composableEntries = composableEntries,
                composableTrackingActive = WickKitComposeTracker.isPluginActive(),
            ),
        )
    }

    private fun collectLiveFrameStats() {
        val frameData = synchronized(frameLock) {
            frameDurations.groupBy { it.toInt() }.map { (durationMs, frames) -> durationMs to frames.size }
        }
        if (frameData.isNotEmpty()) updateFrameStats(parseFrameData(frameData))
    }
}
