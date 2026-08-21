package jsanzo.wickkit

import android.content.Context
import androidx.core.content.edit

object SamplePreferences {

    fun seed(context: Context) {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit {
            putBoolean("feature_dark_mode", false)
                .putBoolean("feature_new_checkout", true)
                .putInt("max_retry_count", 3)
                .putString("api_base_url", "https://api.example.com/v2")
        }

        context.getSharedPreferences("network_config", Context.MODE_PRIVATE).edit {
            putLong("request_timeout_ms", 30000L)
                .putInt("max_connections", 5)
                .putFloat("backoff_multiplier", 1.5f)
        }

        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit {
            putString("username", "test_user")
                .putBoolean("onboarding_completed", false)
                .putInt("session_count", 7)
        }
    }
}
