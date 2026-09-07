package io.wickkit.flags

class WickKitRemoteConfig private constructor(private val delegate: Any) {

    companion object {
        fun wrap(firebaseRc: Any): WickKitRemoteConfig {
            WickKitFlagsManager.notifyWrapRegistered()
            return WickKitRemoteConfig(delegate = firebaseRc)
        }
    }

    fun getBoolean(key: String): Boolean = WickKitFlagsManager
        .getActiveRcOverride(key)
        ?.equals("true", ignoreCase = true)
        ?: invokeOn("getBoolean", key) as? Boolean ?: false

    fun getString(key: String): String = WickKitFlagsManager.getActiveRcOverride(key)
        ?: invokeOn("getString", key) as? String ?: ""

    fun getLong(key: String): Long = WickKitFlagsManager.getActiveRcOverride(key)?.toLongOrNull()
        ?: invokeOn("getLong", key) as? Long ?: 0L

    fun getDouble(key: String): Double = WickKitFlagsManager.getActiveRcOverride(key)?.toDoubleOrNull()
        ?: invokeOn("getDouble", key) as? Double ?: 0.0

    private fun invokeOn(method: String, key: String): Any? = runCatching {
        delegate.javaClass.getMethod(method, String::class.java).invoke(delegate, key)
    }.getOrNull()
}
