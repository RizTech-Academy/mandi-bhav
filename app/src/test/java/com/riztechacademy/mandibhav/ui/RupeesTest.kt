package com.riztechacademy.mandibhav.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class RupeesTest {

    @Test
    fun amountsBelowOneThousandHaveNoSeparator() {
        assertEquals("₹0", formatRupees(0))
        assertEquals("₹999", formatRupees(999))
    }

    @Test
    fun thousandsGroupLikeEverywhereElse() {
        assertEquals("₹1,000", formatRupees(1_000))
        assertEquals("₹2,450", formatRupees(2_450))
        assertEquals("₹99,999", formatRupees(99_999))
    }

    @Test
    fun lakhsAndCroresGroupInTwos() {
        assertEquals("₹1,00,000", formatRupees(1_00_000))
        assertEquals("₹2,45,000", formatRupees(2_45_000))
        assertEquals("₹1,23,45,678", formatRupees(1_23_45_678))
    }

    @Test
    fun negativeAmountsKeepTheSignInFront() {
        assertEquals("-₹2,450", formatRupees(-2_450))
        assertEquals("-₹92,23,37,20,36,85,47,75,808", formatRupees(Long.MIN_VALUE))
    }

    @Test
    fun deviceLocaleDoesNotChangeTheResult() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("mr-IN"))
            assertEquals("₹2,45,000", formatRupees(2_45_000))
            Locale.setDefault(Locale.GERMANY)
            assertEquals("₹2,45,000", formatRupees(2_45_000))
        } finally {
            Locale.setDefault(original)
        }
    }
}
