package com.riztechacademy.mandibhav.domain

import com.riztechacademy.mandibhav.data.FakeAlertNotifier
import com.riztechacademy.mandibhav.data.FakeAlertRepository
import com.riztechacademy.mandibhav.data.FakePriceRepository
import com.riztechacademy.mandibhav.data.RefreshException
import com.riztechacademy.mandibhav.data.RefreshFailure
import com.riztechacademy.mandibhav.data.SamplePrices
import com.riztechacademy.mandibhav.model.AlertDirection
import com.riztechacademy.mandibhav.model.PriceAlert
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshPricesUseCaseTest {

    private val onionAbove2500 = PriceAlert(1, "Onion", "Lasalgaon APMC", AlertDirection.Above, 2_500)

    /** The morning's ₹2,450, then a refresh brings ₹2,580. */
    private val prices = FakePriceRepository(
        pricesAfterRefresh = SamplePrices.all.map {
            if (it.id == SamplePrices.lasalgaonOnion.id) it.copy(modalPrice = 2_580) else it
        },
    )
    private val alerts = FakeAlertRepository(listOf(onionAbove2500))
    private val notifier = FakeAlertNotifier()
    private val refreshPrices = RefreshPricesUseCase(prices, alerts, notifier)

    @Test
    fun aRefreshThatCrossesALevelAnnouncesItOnce() = runTest {
        refreshPrices()
        refreshPrices() // the same prices again: the same crossing

        assertEquals(1, notifier.shown.size)
        assertEquals("Onion", notifier.shown.single().price.commodity)
        assertEquals(2_580L, notifier.shown.single().price.modalPrice)
        assertTrue(alerts.alerts.single().isCrossed)
    }

    @Test
    fun aFailedRefreshChecksNothing() = runTest {
        prices.failure = RefreshFailure.NoConnection

        val failure = runCatching { refreshPrices() }.exceptionOrNull()

        assertEquals(RefreshFailure.NoConnection, (failure as RefreshException).failure)
        assertTrue(notifier.shown.isEmpty())
    }

    @Test
    fun afterThePriceComesBackANewCrossingIsAnnouncedAgain() = runTest {
        refreshPrices() // ₹2,580: announced
        prices.pricesAfterRefresh = SamplePrices.all // back to ₹2,450
        refreshPrices()
        prices.pricesAfterRefresh = SamplePrices.all.map {
            if (it.id == SamplePrices.lasalgaonOnion.id) it.copy(modalPrice = 2_640) else it
        }
        refreshPrices() // ₹2,640: a new crossing

        assertEquals(listOf(2_580L, 2_640L), notifier.shown.map { it.price.modalPrice })
    }
}
