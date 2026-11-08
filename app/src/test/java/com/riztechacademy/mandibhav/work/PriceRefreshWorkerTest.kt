package com.riztechacademy.mandibhav.work

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.riztechacademy.mandibhav.data.FakeAlertNotifier
import com.riztechacademy.mandibhav.data.FakeAlertRepository
import com.riztechacademy.mandibhav.data.FakePriceRepository
import com.riztechacademy.mandibhav.data.RefreshFailure
import com.riztechacademy.mandibhav.data.SamplePrices
import com.riztechacademy.mandibhav.domain.RefreshPricesUseCase
import com.riztechacademy.mandibhav.model.AlertDirection
import com.riztechacademy.mandibhav.model.PriceAlert
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The worker and its schedule, with WorkManager's test library: the worker
 * is built and run directly, and the schedule goes through a real
 * WorkManager whose constraints and delays the test meets by hand.
 */
@RunWith(AndroidJUnit4::class)
class PriceRefreshWorkerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val prices = FakePriceRepository(pricesAfterRefresh = SamplePrices.latest)
    private val alerts = FakeAlertRepository(
        listOf(PriceAlert(1, "Onion", "Lasalgaon APMC", AlertDirection.Above, 2_500)),
    )
    private val notifier = FakeAlertNotifier()

    /** What Hilt's factory does in the app: builds the worker with its dependency. */
    private val factory = object : WorkerFactory() {
        override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker =
            PriceRefreshWorker(appContext, workerParameters, RefreshPricesUseCase(prices, alerts, notifier))
    }

    private fun worker(runAttemptCount: Int = 0) =
        TestListenableWorkerBuilder<PriceRefreshWorker>(context)
            .setWorkerFactory(factory)
            .setRunAttemptCount(runAttemptCount)
            .build()

    @Test
    fun aSuccessfulRefreshSucceedsAndSaysHowManyChanged() = runTest {
        val result = worker().doWork()

        assertEquals(ListenableWorker.Result.success(workDataOf(PriceRefreshWorker.KEY_CHANGED to 3)), result)
        assertEquals(1, prices.refreshCount)
    }

    @Test
    fun theWorkerChecksTheAlertsLikeTheRefreshButton() = runTest {
        // The 4 pm closing prices put Lasalgaon onion at ₹2,525, above ₹2,500.
        worker().doWork()

        assertEquals("lasalgaon-onion-2026-11-07", notifier.shown.single().price.id)
    }

    @Test
    fun noConnectionIsRetried() = runTest {
        prices.failure = RefreshFailure.NoConnection

        assertEquals(ListenableWorker.Result.retry(), worker().doWork())
    }

    @Test
    fun aTimeoutIsRetriedLaterThoughNotAtOnce() = runTest {
        prices.failure = RefreshFailure.Timeout

        assertEquals(ListenableWorker.Result.retry(), worker().doWork())
    }

    @Test
    fun aNotFoundIsNotRetried() = runTest {
        prices.failure = RefreshFailure.Server(404)

        assertEquals(ListenableWorker.Result.failure(), worker().doWork())
    }

    @Test
    fun theFourthTryGivesUpUntilTomorrow() = runTest {
        prices.failure = RefreshFailure.Server(503)

        assertEquals(ListenableWorker.Result.retry(), worker(runAttemptCount = 2).doWork())
        assertEquals(ListenableWorker.Result.failure(), worker(runAttemptCount = 3).doWork())
    }

    private fun testWorkManager(): WorkManager {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .setExecutor(SynchronousExecutor())
                .setWorkerFactory(factory)
                .build(),
        )
        return WorkManager.getInstance(context)
    }

    /** 3 pm in Pune on 7 November 2026. */
    private val threePm = ZonedDateTime.of(2026, 11, 7, 15, 0, 0, 0, ZoneId.of("Asia/Kolkata"))

    @Test
    fun theScheduleIsDailyNeedsANetworkAndStartsAtSevenInTheEvening() {
        val workManager = testWorkManager()

        DailyRefresh.schedule(workManager, threePm)

        val info = workManager.getWorkInfosForUniqueWork(DailyRefresh.WORK_NAME).get().single()
        assertEquals(WorkInfo.State.ENQUEUED, info.state)
        assertEquals(TimeUnit.DAYS.toMillis(1), info.periodicityInfo!!.repeatIntervalMillis)
        assertEquals(NetworkType.CONNECTED, info.constraints.requiredNetworkType)
        assertEquals(Duration.ofHours(4).toMillis(), info.initialDelayMillis)
    }

    @Test
    fun schedulingAgainKeepsTheWorkAlreadyScheduled() {
        val workManager = testWorkManager()

        DailyRefresh.schedule(workManager, threePm)
        val first = workManager.getWorkInfosForUniqueWork(DailyRefresh.WORK_NAME).get().single().id
        DailyRefresh.schedule(workManager, threePm.plusHours(2))
        val after = workManager.getWorkInfosForUniqueWork(DailyRefresh.WORK_NAME).get()

        assertEquals(listOf(first), after.map { it.id })
    }

    @Test
    fun itRunsOnlyOnceTheNetworkAndTheDelayAreMet() {
        val workManager = testWorkManager()
        val driver = WorkManagerTestInitHelper.getTestDriver(context)!!
        DailyRefresh.schedule(workManager, threePm)
        val id = workManager.getWorkInfosForUniqueWork(DailyRefresh.WORK_NAME).get().single().id

        driver.setInitialDelayMet(id)
        assertEquals(0, prices.refreshCount) // no network yet

        driver.setAllConstraintsMet(id)
        val deadline = System.currentTimeMillis() + 5_000
        while (prices.refreshCount == 0 && System.currentTimeMillis() < deadline) Thread.sleep(10)

        assertEquals(1, prices.refreshCount)
        // Periodic work goes back to waiting for tomorrow; it never "succeeds" for good.
        assertTrue(workManager.getWorkInfoById(id).get()!!.state == WorkInfo.State.ENQUEUED)
    }

    @Test
    fun theDelayIsUntilSevenTodayOrTomorrow() {
        assertEquals(Duration.ofHours(4), delayUntilNext(DailyRefresh.EVENING, threePm))
        assertEquals(Duration.ofHours(23), delayUntilNext(DailyRefresh.EVENING, threePm.withHour(20)))
        assertEquals(Duration.ofDays(1), delayUntilNext(DailyRefresh.EVENING, threePm.withHour(19)))
    }
}
