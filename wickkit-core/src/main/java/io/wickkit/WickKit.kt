package io.wickkit

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import io.wickkit.overlay.WickKitActivity
import io.wickkit.overlay.WickKitNotification
import io.wickkit.overlay.WickKitPermissionActivity
import java.lang.ref.WeakReference

object WickKit {

    internal var isVisible = false
    private var currentActivity: WeakReference<Activity>? = null
    private var notificationSetUp = false

    internal fun init(context: Context) {
        val app = context.applicationContext as? Application ?: return
        app.registerActivityLifecycleCallbacks(activityTracker())
    }

    fun open(context: Context) {
        if (isVisible) return
        context.startActivity(
            Intent(context, WickKitActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
        )
    }

    private fun setupNotification(activity: Activity) {
        if (notificationSetUp) return
        notificationSetUp = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) {
                WickKitNotification.show(activity.applicationContext)
            } else {
                activity.startActivity(Intent(activity, WickKitPermissionActivity::class.java))
            }
        } else {
            WickKitNotification.show(activity.applicationContext)
        }
    }

    private fun activityTracker() = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityResumed(activity: Activity) {
            when (activity) {
                is WickKitActivity -> isVisible = true

                is WickKitPermissionActivity -> Unit

                else -> {
                    currentActivity = WeakReference(activity)
                    setupNotification(activity)
                }
            }
        }
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) {
            if (currentActivity?.get() === activity) currentActivity = null
        }
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) {
            if (activity is WickKitActivity) isVisible = false
        }
    }
}
