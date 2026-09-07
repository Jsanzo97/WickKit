package io.wickkit.flags

class WickKitRemoteConfig private constructor(private val delegate: Any) {

    companion object {
        fun wrap(firebaseRc: Any): WickKitRemoteConfig = WickKitRemoteConfig(firebaseRc)
    }

    fun getBoolean(key: String): Boolean = invoke(method = "getBoolean", key = key) as? Boolean ?: false
    fun getString(key: String): String = invoke(method = "getString", key = key) as? String ?: ""
    fun getLong(key: String): Long = invoke(method = "getLong", key = key) as? Long ?: 0L
    fun getDouble(key: String): Double = invoke(method = "getDouble", key = key) as? Double ?: 0.0

    private fun invoke(method: String, key: String): Any? = runCatching {
        delegate.javaClass.getMethod(method, String::class.java).invoke(delegate, key)
    }.getOrNull()
}
