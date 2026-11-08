package com.riztechacademy.mandibhav.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceSourceTest {

    private val source = PriceSource()

    @Test
    fun findsAPriceByItsId() {
        assertEquals(SamplePrices.nashikTomato, source.price(SamplePrices.nashikTomato.id))
    }

    @Test
    fun anUnknownIdIsNullNotACrash() {
        assertNull(source.price("no-such-price"))
        assertTrue(source.week("no-such-price").isEmpty())
    }

    @Test
    fun weekIsSevenDaysOldestFirstEndingOnThePrice() {
        val price = SamplePrices.lasalgaonOnion
        val week = source.week(price.id)

        assertEquals(7, week.size)
        assertEquals(LocalDate.of(2026, 11, 1), week.first().date)
        assertEquals(price.date, week.last().date)
        assertEquals(price.modalPrice, week.last().modalPrice)
    }

    @Test
    fun everySamplePriceHasAWeekEndingOnItsOwnModalPrice() {
        for (price in SamplePrices.all) {
            assertEquals(price.commodity, price.modalPrice, source.week(price.id).last().modalPrice)
        }
    }

    @Test
    fun pricesAtAMarketAreOnlyThatMarkets() {
        val pune = source.pricesAt("Pune (Gultekdi) APMC")
        assertEquals(
            listOf(SamplePrices.puneOnion, SamplePrices.puneTomato, SamplePrices.puneTur),
            pune,
        )
    }
}
