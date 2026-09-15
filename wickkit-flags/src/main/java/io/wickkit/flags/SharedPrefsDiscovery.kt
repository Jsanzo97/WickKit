package io.wickkit.flags

import java.io.File

internal object SharedPrefsDiscovery {

    private val EXCLUDED_EXACT = setOf(
        "wickkit_flags",
        "gtc",
        "WebViewChromiumPrefs",
    )

    private val EXCLUDED_SIMPLE_PREFIXES = listOf(
        "frc_",
        "firebase_",
        "gtm_",
        "androidx_",
    )

    private val EXCLUDED_SUFFIXES = listOf(
        "_secure_prefs",
    )

    private val KNOWN_SDK_PREFIXES = listOf(
        "com.google.",
        "com.facebook.",
        "com.crashlytics.",
        "com.appsflyer.",
        "com.onesignal.",
        "io.sentry.",
        "io.embrace.",
        "io.intercom.",
    )

    fun discoverNames(prefsDir: File, appPackage: String? = null): List<String> = prefsDir.listFiles()
        ?.filter { it.extension == "xml" }
        ?.map { it.nameWithoutExtension }
        ?.filter { name ->
            name !in EXCLUDED_EXACT &&
                EXCLUDED_SIMPLE_PREFIXES.none { name.startsWith(it) } &&
                EXCLUDED_SUFFIXES.none { name.endsWith(it) } &&
                !isThirdPartySdkFile(name, appPackage)
        }
        ?: emptyList()

    private fun isThirdPartySdkFile(name: String, appPackage: String?): Boolean {
        if (appPackage != null && name.startsWith(appPackage)) return false
        return KNOWN_SDK_PREFIXES.any { name.startsWith(it) }
    }
}
