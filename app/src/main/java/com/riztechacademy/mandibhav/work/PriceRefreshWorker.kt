package com.riztechacademy.mandibhav.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.riztechacademy.mandibhav.data.RefreshException
import com.riztechacademy.mandibhav.data.isWorthRetryingLater
import com.riztechacademy.mandibhav.domain.RefreshPricesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * The daily refresh, run by WorkManager whether or not the app is open.
 *
 * It downloads nothing itself: it calls the same [RefreshPricesUseCase] as
 * the Refresh button, so the same single-flight download, retries and
 * transaction save the prices, and the same check announces any alert that
 * has crossed. If the Prices screen is refreshing at the same moment, the
 * two share one download.
 *
 * A `CoroutineWorker`: [doWork] is a suspend function on
 * `Dispatchers.Default`, and if WorkManager stops the work (the network
 * constraint is lost, or the system needs the time back) the coroutine is
 * cancelled, and the work is run again later.
 */
@HiltWorker
class PriceRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val refreshPrices: RefreshPricesUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "Daily refresh, attempt ${runAttemptCount + 1}")
        return try {
            val changed = refreshPrices()
            Log.i(TAG, "Daily refresh saved the prices: $changed changed")
            Result.success(workDataOf(KEY_CHANGED to changed))
        } catch (e: RefreshException) {
            // Only the data layer's own failure; a cancellation goes on up,
            // and WorkManager reschedules stopped work by itself.
            val again = e.failure.isWorthRetryingLater && runAttemptCount < MAX_ATTEMPTS - 1
            Log.i(TAG, "Daily refresh failed: ${e.failure}; ${if (again) "retrying" else "giving up until tomorrow"}")
            if (again) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val TAG = "PriceRefreshWorker"

        /** The output key for how many prices changed. */
        const val KEY_CHANGED = "changed"

        /** At most four tries a day: the first, then three retries with backoff. */
        const val MAX_ATTEMPTS = 4
    }
}
