package com.riztechacademy.mandibhav.ui

import com.riztechacademy.mandibhav.data.SamplePrices
import org.junit.Assert.assertEquals
import org.junit.Test

class MarketSummariesTest {

    @Test
    fun eachMarketOnceWithItsCountInFirstSeenOrder() {
        assertEquals(
            listOf(
                MarketSummary("Lasalgaon APMC", 2),
                MarketSummary("Pune (Gultekdi) APMC", 3),
                MarketSummary("Nashik APMC", 2),
                MarketSummary("Vashi APMC, Navi Mumbai", 2),
            ),
            marketSummaries(SamplePrices.all),
        )
    }
}
