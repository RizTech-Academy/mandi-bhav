package com.riztechacademy.mandibhav.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import java.time.Duration

/**
 * Debug builds only: runs the refresh worker once, from the command line,
 * without waiting for 7 pm.
 *
 * ```
 * adb shell am broadcast -n com.riztechacademy.mandibhav/.work.RunRefreshReceiver
 * ```
 *
 * Add `--el delay 120` to wait two minutes first, long enough to close the
 * app or put the emulator into Doze before the work becomes due.
 *
 * It enqueues one-time work for the same worker, with the same network
 * constraint, so it goes through JobScheduler like the daily run does.
 */
class RunRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "run-refresh-now",
            ExistingWorkPolicy.REPLACE,
            DailyRefresh.runOnceRequest(Duration.ofSeconds(intent.getLongExtra("delay", 0))),
        )
    }
}
