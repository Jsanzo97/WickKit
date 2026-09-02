package io.wickkit.flags

class WickKitRemoteConfig private constructor(private val delegate: Any) {

    companion object {
        fun wrap(firebaseRc: Any): WickKitRemoteConfig = WickKitRemoteConfig(delegate = firebaseRc)
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
