package io.wickkit.flags

import android.content.Context

class WickKitRemoteConfig private constructor(private val delegate: Any) {

    companion object {
        @Suppress("UnusedParameter")
        fun wrap(context: Context, firebaseRc: Any): WickKitRemoteConfig = WickKitRemoteConfig(firebaseRc)
    }

    fun getBoolean(key: String): Boolean = invoke("getBoolean", key) as? Boolean ?: false
    fun getString(key: String): String = invoke("getString", key) as? String ?: ""
    fun getLong(key: String): Long = invoke("getLong", key) as? Long ?: 0L
    fun getDouble(key: String): Double = invoke("getDouble", key) as? Double ?: 0.0

    private fun invoke(method: String, key: String): Any? = runCatching {
        delegate.javaClass.getMethod(method, String::class.java).invoke(delegate, key)
    }.getOrNull()
}
