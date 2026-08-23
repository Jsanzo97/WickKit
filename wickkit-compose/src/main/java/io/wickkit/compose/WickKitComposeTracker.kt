package io.wickkit.compose

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private const val YELLOW_THRESHOLD = 5f
private const val ORANGE_THRESHOLD = 25f
private const val RED_THRESHOLD = 50f

object WickKitComposeTracker {

    private val counts = ConcurrentHashMap<String, AtomicLong>()
    private val peakRates = HashMap<String, Float>()
    private val lastPollCounts = HashMap<String, Long>()
    private var lastPollTimeMs = -1L

    @Volatile private var pluginActive = false

    fun onRecompose(name: String) {
        if (!pluginActive) pluginActive = true
        counts.getOrPut(name) { AtomicLong() }.incrementAndGet()
    }

    fun isPluginActive(): Boolean = pluginActive

    fun reset() {
        counts.clear()
        synchronized(this) {
            peakRates.clear()
            lastPollCounts.clear()
            lastPollTimeMs = -1L
        }
        // pluginActive intentionally not reset — the plugin is still applied after an activity change
    }

    fun computeEntries(): ImmutableList<ComposableEntry> = computeEntries(System.currentTimeMillis())

    internal fun computeEntries(nowMs: Long): ImmutableList<ComposableEntry> = synchronized(this) {
        val elapsedMs = if (lastPollTimeMs < 0L) 0L else nowMs - lastPollTimeMs
        lastPollTimeMs = nowMs

        counts.entries.mapNotNull { (name, countRef) ->
            val total = countRef.get()
            val last = lastPollCounts[name] ?: 0L
            lastPollCounts[name] = total
            val rate = if (elapsedMs > 0L) (total - last) * 1_000f / elapsedMs else 0f
            val peak = maxOf(peakRates[name] ?: 0f, rate)
            peakRates[name] = peak
            severityFor(rate)?.let { severity ->
                ComposableEntry(
                    name = name,
                    totalCount = total,
                    ratePerSecond = rate,
                    peakRatePerSecond = peak,
                    severity = severity,
                )
            }
        }.sortedByDescending { it.ratePerSecond }.toPersistentList()
    }

    private fun severityFor(rate: Float): RecomposeSeverity? = when {
        rate > RED_THRESHOLD -> RecomposeSeverity.RED
        rate > ORANGE_THRESHOLD -> RecomposeSeverity.ORANGE
        rate > YELLOW_THRESHOLD -> RecomposeSeverity.YELLOW
        else -> null
    }
}
