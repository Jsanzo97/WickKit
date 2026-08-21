package jsanzo.wickkit

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import timber.log.Timber

object SampleRemoteConfig {

    private val defaults = mapOf(
        "welcome_message" to "Hello from Remote Config!",
        "feature_new_checkout" to false,
        "max_items_per_page" to 20L,
        "api_timeout_seconds" to 30L,
        "enable_analytics" to true,
    )

    fun init() {
        Firebase.remoteConfig
            .setConfigSettingsAsync(remoteConfigSettings { minimumFetchIntervalInSeconds = 0 })
            .addOnCompleteListener {
                Firebase.remoteConfig.setDefaultsAsync(defaults)
                doFetch()
            }
    }

    fun fetch() = doFetch()

    private fun doFetch() {
        Firebase.remoteConfig.fetchAndActivate()
            .addOnSuccessListener { updated ->
                Timber.tag("SampleRemoteConfig").d("Fetch OK — updated=$updated")
            }
            .addOnFailureListener { exception ->
                Timber.tag("SampleRemoteConfig").e(exception, "Fetch failed")
            }
    }
}
