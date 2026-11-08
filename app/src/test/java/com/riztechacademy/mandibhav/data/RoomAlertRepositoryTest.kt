package com.riztechacademy.mandibhav.data

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.riztechacademy.mandibhav.data.local.MandiBhavDatabase
import com.riztechacademy.mandibhav.data.local.asEntity
import com.riztechacademy.mandibhav.domain.RefreshPricesUseCase
import com.riztechacademy.mandibhav.model.AlertDirection
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The alerts over a real in-memory database, and the check against real saved prices. */
@RunWith(AndroidJUnit4::class)
class RoomAlertRepositoryTest {

    private val database = Room.inMemoryDatabaseBuilder<MandiBhavDatabase>(
        ApplicationProvider.getApplicationContext<Context>(),
    ).setDriver(AndroidSQLiteDriver()).build()
    private val clock = Clock.fixed(Instant.parse("2026-11-07T10:35:00Z"), ZoneId.of("Asia/Kolkata"))
    private val alerts = RoomAlertRepository(database.alertDao(), database.priceDao(), clock)

    @Before
    fun saveTheMorningPrices() = runTest {
        database.priceDao().saveDownload(SamplePrices.all.map { it.asEntity() })
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun anAlertIsSavedAndListed() = runTest {
        val id = alerts.add("Onion", "Lasalgaon APMC", AlertDirection.Above, 2_500)

        val saved = alerts.alerts().first().single()
        assertEquals(id, saved.id)
        assertEquals(AlertDirection.Above, saved.direction)
        assertEquals(2_500L, saved.level)
        // Lasalgaon onion is ₹2,450 this morning: not past ₹2,500 yet.
        assertFalse(saved.isCrossed)
    }

    @Test
    fun anAlertAlreadyPastItsLevelStartsCrossed() = runTest {
        // Lasalgaon onion is ₹2,450: already above ₹2,400.
        alerts.add("Onion", "Lasalgaon APMC", AlertDirection.Above, 2_400)

        assertTrue(alerts.allAlerts().single().isCrossed)
    }

    @Test
    fun aDeletedAlertIsGone() = runTest {
        val keep = alerts.add("Tomato", "Nashik APMC", AlertDirection.Below, 1_000)
        val delete = alerts.add("Onion", "Lasalgaon APMC", AlertDirection.Above, 2_500)

        alerts.delete(delete)

        assertEquals(listOf(keep), alerts.allAlerts().map { it.id })
    }

    @Test
    fun theCrossingIsSavedSoTheNextCheckSaysNothing() = runTest {
        // The real repository, a fake network bringing the 4 pm closing prices.
        val testScope = TestScope()
        val appScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScope.testScheduler))
        val prices = RoomPriceRepository(database.priceDao(), FakePriceNetworkDataSource(), clock, appScope)
        val notifier = FakeAlertNotifier()
        val refreshPrices = RefreshPricesUseCase(prices, alerts, notifier)
        alerts.add("Onion", "Lasalgaon APMC", AlertDirection.Above, 2_500)

        testScope.runTest {
            refreshPrices() // ₹2,525 at 4 pm
            refreshPrices() // still ₹2,525
        }
        appScope.cancel()

        assertEquals(1, notifier.shown.size)
        assertTrue(alerts.allAlerts().single().isCrossed)
    }
}
