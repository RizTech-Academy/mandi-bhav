package com.riztechacademy.mandibhav.work

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * The daily price refresh: what it needs, when it runs, and how it is
 * scheduled. One unique piece of periodic work, named [WORK_NAME].
 */
object DailyRefresh {

    /** The name the work is saved under. Never change it: installed phones have it saved. */
    const val WORK_NAME = "daily-price-refresh"

    /**
     * After the markets have closed and the day's prices are published,
     * so the farmer has them before she leaves in the morning.
     */
    val EVENING: LocalTime = LocalTime.of(19, 0)

    /**
     * Only with a working connection. Any network will do: a refresh is
     * about 3.5 KB, less than one commodity picture, and waiting for Wi-Fi
     * would mean never, for a farmer whose only connection is mobile data.
     */
    private val needsNetwork = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Once a day, from the next 7 pm; if it fails, again after 10 minutes, then 20, then 40. */
    fun request(now: ZonedDateTime): PeriodicWorkRequest =
        PeriodicWorkRequestBuilder<PriceRefreshWorker>(1, TimeUnit.DAYS)
            .setConstraints(needsNetwork)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .setInitialDelay(delayUntilNext(EVENING, now))
            .build()

    /**
     * The same worker once, after [delay], as soon as the network allows. Debug builds use
     * it to run the refresh on demand: WorkManager will not run periodic
     * work before its time, even when JobScheduler is told to.
     */
    fun runOnceRequest(delay: Duration = Duration.ZERO): OneTimeWorkRequest =
        OneTimeWorkRequestBuilder<PriceRefreshWorker>()
            .setInitialDelay(delay)
            .setConstraints(needsNetwork)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

    /**
     * Schedules the daily refresh unless it is already scheduled. KEEP, so
     * calling this on every start changes nothing once it exists: replacing
     * it would restart the 24 hours each time the app opened, and a farmer
     * who opens the app every afternoon would never get her evening refresh.
     */
    fun schedule(workManager: WorkManager, now: ZonedDateTime) {
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request(now))
    }
}

/** How long from [now] until [time] next comes round: later today, or tomorrow. */
fun delayUntilNext(time: LocalTime, now: ZonedDateTime): Duration {
    val today = now.with(time)
    val next = if (today.isAfter(now)) today else today.plusDays(1)
    return Duration.between(now, next)
}
