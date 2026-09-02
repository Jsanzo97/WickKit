package io.wickkit.flags

import java.io.File

internal object SharedPrefsDiscovery {

    private val EXCLUDED_EXACT = setOf(
        "wickkit_flags",
        "gtc",
        "WebViewChromiumPrefs",
    )

    private val REVERSE_DOMAIN_PREFIXES = listOf(
        "com.", "org.", "io.", "net.", "de.", "me.", "co.", "uk.", "fr.", "es.",
    )

    private val EXCLUDED_SIMPLE_PREFIXES = listOf(
        "frc_", // Firebase Remote Config
        "firebase_", // Firebase SDK
        "gtm_", // Google Tag Manager
        "androidx_", // AndroidX internal (underscore variant)
    )

    private val EXCLUDED_SUFFIXES = listOf(
        "_secure_prefs", // EncryptedSharedPreferences internal file
    )

    fun discoverNames(prefsDir: File): List<String> = prefsDir.listFiles()
        ?.filter { it.extension == "xml" }
        ?.map { it.nameWithoutExtension }
        ?.filter { name ->
            name !in EXCLUDED_EXACT &&
                REVERSE_DOMAIN_PREFIXES.none { name.startsWith(it) } &&
                EXCLUDED_SIMPLE_PREFIXES.none { name.startsWith(it) } &&
                EXCLUDED_SUFFIXES.none { name.endsWith(it) }
        }
        ?: emptyList()
}
