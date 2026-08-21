package io.wickkit.flags

import android.content.Context
import java.io.File

internal object SharedPrefsDiscovery {

    private val EXCLUDED_EXACT = setOf(
        "wickkit_flags",
        "gtc",
        "WebViewChromiumPrefs",
    )

    private val EXCLUDED_PREFIXES = listOf(
        "frc_", // Firebase Remote Config local cache
        "com.google.", // Google Play Services / Firebase SDK
        "com.firebase.", // Firebase SDK
        "firebase_", // Firebase SDK
        "androidx.", // AndroidX internal prefs
    )

    fun discoverNames(context: Context): List<String> {
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        return prefsDir.listFiles()
            ?.filter { it.extension == "xml" }
            ?.map { it.nameWithoutExtension }
            ?.filter { name ->
                name !in EXCLUDED_EXACT && EXCLUDED_PREFIXES.none { name.startsWith(it) }
            }
            ?: emptyList()
    }
}
