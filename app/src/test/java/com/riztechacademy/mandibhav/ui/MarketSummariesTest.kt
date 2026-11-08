package com.riztechacademy.mandibhav.ui

import com.riztechacademy.mandibhav.data.SamplePrices
import com.riztechacademy.mandibhav.model.Market
import org.junit.Assert.assertEquals
import org.junit.Test

class MarketSummariesTest {

    @Test
    fun aSummaryCountsTheMarketsPricesAndNamesItsDistrict() {
        val pune = Market(
            name = "Pune (Gultekdi) APMC",
            district = "Pune",
            prices = listOf(SamplePrices.puneOnion, SamplePrices.puneTomato, SamplePrices.puneTur),
        )

        assertEquals(MarketSummary("Pune (Gultekdi) APMC", "Pune", 3), pune.summary())
        assertEquals("Pune district, 3 prices today", pune.summary().description())
    }

    @Test
    fun anUnknownDistrictIsLeftOutAndOnePriceIsSingular() {
        assertEquals("1 price today", MarketSummary("Lasalgaon APMC", "", 1).description())
    }
}
