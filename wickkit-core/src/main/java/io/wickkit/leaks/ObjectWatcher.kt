package io.wickkit.leaks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

private const val WATCH_DELAY_MS = 5_000L
private const val GC_SETTLE_MS = 200L

internal object ObjectWatcher {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pendingRefs = ConcurrentLinkedQueue<Pair<String, WeakReference<Any>>>()
    private val gcScheduled = AtomicBoolean(false)

    fun watch(target: Any) {
        pendingRefs.add(target.javaClass.name to WeakReference(target))
        if (gcScheduled.compareAndSet(false, true)) {
            scope.launch {
                delay(WATCH_DELAY_MS.milliseconds)
                Runtime.getRuntime().gc()
                delay(GC_SETTLE_MS.milliseconds)
                val detectedAt = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                generateSequence { pendingRefs.poll() }.forEach { (className, ref) ->
                    if (ref.get() != null) {
                        WickKitLeakManager.add(className = className, detectedAt = detectedAt)
                    }
                }
                gcScheduled.set(false)
            }
        }
    }
}
