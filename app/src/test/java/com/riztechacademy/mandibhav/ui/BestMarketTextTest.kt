package com.riztechacademy.mandibhav.ui

import com.riztechacademy.mandibhav.data.SamplePrices
import org.junit.Assert.assertEquals
import org.junit.Test

class BestMarketTextTest {

    @Test
    fun namesTheBestMarketAndItsPrice() {
        assertEquals(
            "Best today: Lasalgaon APMC, ₹2,450",
            describeBestMarket(SamplePrices.lasalgaonOnion, here = SamplePrices.puneOnion),
        )
    }

    @Test
    fun saysSoWhenThisIsTheBestMarket() {
        assertEquals(
            "The best price for onion today",
            describeBestMarket(SamplePrices.lasalgaonOnion, here = SamplePrices.lasalgaonOnion),
        )
    }
}
