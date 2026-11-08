package com.riztechacademy.mandibhav.data

import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SamplePriceRepositoryTest {

    private val repository = SamplePriceRepository()

    @Test
    fun findsAPriceByItsId() = runTest {
        assertEquals(SamplePrices.nashikTomato, repository.price(SamplePrices.nashikTomato.id).first())
    }

    @Test
    fun anUnknownIdIsNullNotACrash() = runTest {
        assertNull(repository.price("no-such-price").first())
        assertTrue(repository.week("no-such-price").first().isEmpty())
    }

    @Test
    fun weekIsSevenDaysOldestFirstEndingOnThePrice() = runTest {
        val price = SamplePrices.lasalgaonOnion
        val week = repository.week(price.id).first()

        assertEquals(7, week.size)
        assertEquals(LocalDate.of(2026, 11, 1), week.first().date)
        assertEquals(price.date, week.last().date)
        assertEquals(price.modalPrice, week.last().modalPrice)
    }

    @Test
    fun everySamplePriceHasAWeekEndingOnItsOwnModalPrice() = runTest {
        for (price in SamplePrices.all) {
            assertEquals(price.commodity, price.modalPrice, repository.week(price.id).first().last().modalPrice)
        }
    }

    @Test
    fun pricesAtAMarketAreOnlyThatMarkets() = runTest {
        assertEquals(
            listOf(SamplePrices.puneOnion, SamplePrices.puneTomato, SamplePrices.puneTur),
            repository.pricesAt("Pune (Gultekdi) APMC").first(),
        )
    }

    @Test
    fun refreshAppliesTheClosingPricesOnce() = runTest {
        // runTest skips the delay that stands in for the network.
        assertEquals(3, repository.refresh())
        assertEquals(2_525L, repository.price(SamplePrices.lasalgaonOnion.id).first()!!.modalPrice)
        assertEquals(SamplePrices.all.size, repository.todaysPrices().first().size)

        assertEquals(0, repository.refresh())
    }

    @Test
    fun afterARefreshTheWeekEndsOnTheNewPrice() = runTest {
        repository.refresh()

        val week = repository.week(SamplePrices.lasalgaonOnion.id).first()
        assertEquals(2_525L, week.last().modalPrice)
        assertEquals(2_420L, week[week.size - 2].modalPrice)
    }

    @Test
    fun aPriceTheRefreshDidNotChangeEmitsNothingNew() = runTest {
        // Everything each flow emits while a test "screen" watches it.
        val lasalgaon = mutableListOf<Long?>()
        val pune = mutableListOf<Long?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.price(SamplePrices.lasalgaonOnion.id).collect { lasalgaon += it?.modalPrice }
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.price(SamplePrices.puneOnion.id).collect { pune += it?.modalPrice }
        }

        repository.refresh() // three prices change; Pune onion is not one of them

        assertEquals(listOf<Long?>(2_450, 2_525), lasalgaon)
        assertEquals(listOf<Long?>(2_200), pune)
    }
}
