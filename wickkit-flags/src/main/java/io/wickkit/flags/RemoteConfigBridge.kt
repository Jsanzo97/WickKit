package io.wickkit.flags

internal object RemoteConfigBridge {

    private const val RC_CLASS = "com.google.firebase.remoteconfig.FirebaseRemoteConfig"

    fun isAvailable(): Boolean = try {
        Class.forName(RC_CLASS)
        true
    } catch (_: ClassNotFoundException) {
        false
    }

    @Suppress("UNCHECKED_CAST")
    fun getAll(): Map<String, String> = try {
        val clazz = Class.forName(RC_CLASS)
        val instance = clazz.getMethod("getInstance").invoke(null)
        val values = clazz.getMethod("getAll").invoke(instance) as Map<String, Any>
        values.mapValues { (_, configValue) ->
            configValue.javaClass.getMethod("asString").invoke(configValue) as String
        }
    } catch (_: Exception) {
        emptyMap()
    }
}
