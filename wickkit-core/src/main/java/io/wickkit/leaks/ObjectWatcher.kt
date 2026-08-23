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
import kotlin.time.Duration.Companion.milliseconds

private const val WATCH_DELAY_MS = 5_000L
private const val GC_SETTLE_MS = 200L

internal object ObjectWatcher {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun watch(target: Any) {
        val className = target.javaClass.name
        val ref = WeakReference(target)
        scope.launch {
            delay(WATCH_DELAY_MS.milliseconds)
            Runtime.getRuntime().gc()
            delay(GC_SETTLE_MS.milliseconds)
            if (ref.get() != null) {
                val detectedAt = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                WickKitLeakManager.add(className = className, detectedAt = detectedAt)
            }
        }
    }
}
