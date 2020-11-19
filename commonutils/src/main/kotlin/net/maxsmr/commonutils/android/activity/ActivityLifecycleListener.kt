package net.maxsmr.commonutils.android.activity

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * Слушатель жизненного цикла активити
 */
open class ActivityLifecycleListener(
        private val onActivityCreated: ((activity: Activity?, savedInstanceState: Bundle?) -> Unit)? = null,
        private val onActivityStarted: ((activity: Activity?) -> Unit)? = null,
        private val onActivityResumed: ((activity: Activity?) -> Unit)? = null,
        private val onActivityPaused: ((activity: Activity?) -> Unit)? = null,
        private val onActivityStopped: ((activity: Activity?) -> Unit)? = null,
        private val onActivityDestroyed: ((activity: Activity?) -> Unit)? = null,
        private val onActivitySaveInstanceState: ((activity: Activity?, savedInstanceState: Bundle?) -> Unit)? = null
) : Application.ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        onActivityCreated?.invoke(activity, savedInstanceState)
    }

    override fun onActivityStarted(activity: Activity) {
        onActivityStarted?.invoke(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        onActivityResumed?.invoke(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        onActivityPaused?.invoke(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        onActivityStopped?.invoke(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        onActivityDestroyed?.invoke(activity)
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        onActivitySaveInstanceState?.invoke(activity, outState)
    }
}