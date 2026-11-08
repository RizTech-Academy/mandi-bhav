package com.riztechacademy.mandibhav.domain

import com.riztechacademy.mandibhav.data.SamplePrices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BestMarketTest {

    @Test
    fun theHighestModalPriceWins() {
        // Onion: Lasalgaon 2,450, Nashik 2,300, Pune 2,200.
        assertEquals(SamplePrices.lasalgaonOnion, bestMarketFor(SamplePrices.all, "Onion"))
    }

    @Test
    fun onATieTheHigherMinimumWins() {
        val nashikMatchesLasalgaon = SamplePrices.nashikOnion.copy(modalPrice = 2_450)
        // Nashik first, so the answer cannot come from the order of the list.
        val prices = listOf(nashikMatchesLasalgaon, SamplePrices.lasalgaonOnion)

        // Same modal price; Lasalgaon's minimum is 1,200, Nashik's 1,100.
        assertEquals(SamplePrices.lasalgaonOnion, bestMarketFor(prices, "Onion"))
    }

    @Test
    fun anOlderHigherPriceDoesNotWin() {
        // Vashi reported onion at 2,900, but last week. It is not on offer today.
        val lastWeekAtVashi = SamplePrices.lasalgaonOnion.copy(
            id = "vashi-onion-2026-10-31",
            market = "Vashi APMC, Navi Mumbai",
            date = SamplePrices.lasalgaonOnion.date.minusDays(7),
            modalPrice = 2_900,
        )
        val prices = SamplePrices.all + lastWeekAtVashi

        assertEquals(SamplePrices.lasalgaonOnion, bestMarketFor(prices, "Onion"))
    }

    @Test
    fun aCommodityNobodyReportedHasNoBestMarket() {
        assertNull(bestMarketFor(SamplePrices.all, "Garlic"))
    }
}
