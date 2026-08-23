package jsanzo.wickkit

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import io.wickkit.flags.WickKitRemoteConfig
import timber.log.Timber

object SampleRemoteConfig {

    private val defaults = mapOf(
        "welcome_message" to "Hello from Remote Config!",
        "feature_new_checkout" to false,
        "max_items_per_page" to 20L,
        "api_timeout_seconds" to 30L,
        "enable_analytics" to true,
    )

    private var remoteConfig: WickKitRemoteConfig? = null

    fun init(context: Context) {
        remoteConfig = WickKitRemoteConfig.wrap(context = context, firebaseRc = Firebase.remoteConfig)
        Firebase.remoteConfig
            .setConfigSettingsAsync(remoteConfigSettings { minimumFetchIntervalInSeconds = 0 })
            .addOnCompleteListener {
                Firebase.remoteConfig.setDefaultsAsync(defaults)
                doFetch()
            }
    }

    fun fetch() = doFetch()

    fun getWelcomeMessage(): String = remoteConfig?.getString("welcome_message") ?: "Hello!"
    fun isNewCheckoutEnabled(): Boolean = remoteConfig?.getBoolean("feature_new_checkout") ?: false
    fun getMaxItemsPerPage(): Long = remoteConfig?.getLong("max_items_per_page") ?: 20L
    fun getApiTimeoutSeconds(): Long = remoteConfig?.getLong("api_timeout_seconds") ?: 30L
    fun isAnalyticsEnabled(): Boolean = remoteConfig?.getBoolean("enable_analytics") ?: true

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
