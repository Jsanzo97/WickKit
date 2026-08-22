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

    private var rc: WickKitRemoteConfig? = null

    fun init(context: Context) {
        rc = WickKitRemoteConfig.wrap(context, Firebase.remoteConfig)
        Firebase.remoteConfig
            .setConfigSettingsAsync(remoteConfigSettings { minimumFetchIntervalInSeconds = 0 })
            .addOnCompleteListener {
                Firebase.remoteConfig.setDefaultsAsync(defaults)
                doFetch()
            }
    }

    fun fetch() = doFetch()

    // ── Accessors — use rc instead of Firebase.remoteConfig directly ─────────────

    fun getWelcomeMessage(): String = rc?.getString("welcome_message") ?: "Hello!"
    fun isNewCheckoutEnabled(): Boolean = rc?.getBoolean("feature_new_checkout") ?: false
    fun getMaxItemsPerPage(): Long = rc?.getLong("max_items_per_page") ?: 20L
    fun getApiTimeoutSeconds(): Long = rc?.getLong("api_timeout_seconds") ?: 30L
    fun isAnalyticsEnabled(): Boolean = rc?.getBoolean("enable_analytics") ?: true

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
