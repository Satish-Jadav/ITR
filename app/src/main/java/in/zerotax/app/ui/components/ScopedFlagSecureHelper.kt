package `in`.zerotax.app.ui.components

import android.app.Activity
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object ScopedFlagSecureHelper {
    private var restoreJob: Job? = null

    /**
     * Temporarily relaxes FLAG_SECURE for up to 5 minutes to permit the user
     * or OS print/share spooler to generate and save the official ITR-V PDF receipt.
     */
    fun temporarilyRelaxFlagSecure(activity: Activity, durationMs: Long = 5 * 60 * 1000L) {
        restoreJob?.cancel()
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

        restoreJob = CoroutineScope(Dispatchers.Main).launch {
            delay(durationMs)
            reEnableFlagSecure(activity)
        }
    }

    fun reEnableFlagSecure(activity: Activity) {
        restoreJob?.cancel()
        restoreJob = null
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
