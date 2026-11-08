package com.riztechacademy.mandibhav.ui

import com.riztechacademy.mandibhav.data.SamplePrices
import org.junit.Assert.assertEquals
import org.junit.Test

class PriceDescriptionTest {

    @Test
    fun spokenRupeesPutsTheUnitAfterTheNumber() {
        assertEquals("2,450 rupees", spokenRupees(2_450))
        assertEquals("2,45,000 rupees", spokenRupees(2_45_000))
    }

    @Test
    fun cardDescriptionIsOneSentenceInReadingOrder() {
        assertEquals(
            "Onion at Lasalgaon APMC, 2,450 rupees per quintal. " +
                "Range 1,200 to 2,851 rupees. 7 November 2026.",
            priceCardDescription(SamplePrices.lasalgaonOnion),
        )
    }
}
