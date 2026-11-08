package com.riztechacademy.mandibhav.ui

import com.riztechacademy.mandibhav.data.SamplePrices
import org.junit.Assert.assertEquals
import org.junit.Test

class ShareTextTest {
    @Test
    fun aSharedPriceReadsWithoutTheApp() {
        assertEquals(
            "Onion at Lasalgaon APMC, 7 Nov 2026: ₹2,450 a quintal (modal price). Min ₹1,200, max ₹2,851. From Mandi Bhav.",
            shareText(SamplePrices.lasalgaonOnion),
        )
    }
}
