package io.wickkit.flags

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WickKitRemoteConfig private constructor(
    private val delegate: Any,
    context: Context,
) {

    init {
        if (!WickKitFlagsManager.isInitialized) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                WickKitFlagsManager.init(context.applicationContext)
            }
        }
    }

    companion object {
        fun wrap(context: Context, firebaseRc: Any): WickKitRemoteConfig = WickKitRemoteConfig(
            delegate = firebaseRc,
            context = context,
        )
    }

    fun getBoolean(key: String): Boolean = WickKitFlagsManager.getBoolean(
        key = key,
        remoteValue = invokeOn(
            target = delegate,
            method = "getBoolean",
            key = key,
        ) as? Boolean ?: false,
    )

    fun getString(key: String): String = WickKitFlagsManager.getString(
        key = key,
        remoteValue = invokeOn(
            target = delegate,
            method = "getString",
            key = key,
        ) as? String ?: "",
    )

    fun getLong(key: String): Long = WickKitFlagsManager.getLong(
        key = key,
        remoteValue = invokeOn(
            target = delegate,
            method = "getLong",
            key = key,
        ) as? Long ?: 0L,
    )

    fun getDouble(key: String): Double = WickKitFlagsManager.getDouble(
        key = key,
        remoteValue = invokeOn(
            target = delegate,
            method = "getDouble",
            key = key,
        ) as? Double ?: 0.0,
    )
}

private fun invokeOn(target: Any, method: String, key: String): Any? = runCatching {
    target.javaClass.getMethod(
        method,
        String::class.java,
    ).invoke(target, key)
}.getOrNull()
