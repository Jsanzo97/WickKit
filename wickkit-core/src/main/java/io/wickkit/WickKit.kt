package io.wickkit

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import io.wickkit.compose.WickKitComposeTracker
import io.wickkit.leaks.ObjectWatcher
import io.wickkit.logs.WickKitLogcat
import io.wickkit.overlay.WickKitActivity
import io.wickkit.overlay.WickKitNotification
import io.wickkit.overlay.WickKitPermissionActivity
import io.wickkit.performance.WickKitPerformanceManager
import java.lang.ref.WeakReference

object WickKit {

    internal var isVisible = false
    private var currentActivity: WeakReference<Activity>? = null
    private var notificationSetUp = false

    private val fragmentWatcher = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentDestroyed(fragmentManager: FragmentManager, fragment: Fragment) {
            ObjectWatcher.watch(fragment)
        }
    }

    internal fun init(context: Context) {
        val app = context.applicationContext as? Application ?: return
        WickKitLogcat.start()
        WickKitPerformanceManager.start()
        app.registerActivityLifecycleCallbacks(activityTracker())
    }

    fun open(context: Context) {
        if (!isVisible) {
            context.startActivity(
                Intent(context, WickKitActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
            )
        }
    }

    private fun setupNotification(activity: Activity) {
        if (!notificationSetUp) {
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
    }

    private fun activityTracker() = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            if (activity is FragmentActivity) {
                activity.supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentWatcher, true)
            }
        }
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityResumed(activity: Activity) {
            when (activity) {
                is WickKitActivity -> isVisible = true

                is WickKitPermissionActivity -> Unit

                else -> {
                    currentActivity = WeakReference(activity)
                    setupNotification(activity)
                    WickKitPerformanceManager.onActivityResumed(activity)
                }
            }
        }
        override fun onActivityPaused(activity: Activity) {
            if (activity !is WickKitActivity && activity !is WickKitPermissionActivity) {
                WickKitPerformanceManager.onActivityPaused()
            }
        }
        override fun onActivityStopped(activity: Activity) {
            if (currentActivity?.get() === activity) currentActivity = null
            if (activity !is WickKitActivity && activity !is WickKitPermissionActivity) {
                WickKitComposeTracker.reset()
            }
        }
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) {
            when (activity) {
                is WickKitActivity -> isVisible = false
                is WickKitPermissionActivity -> Unit
                else -> ObjectWatcher.watch(activity)
            }
        }
    }
}
