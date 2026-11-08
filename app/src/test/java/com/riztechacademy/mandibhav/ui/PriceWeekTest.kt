package com.riztechacademy.mandibhav.ui

import com.riztechacademy.mandibhav.data.SamplePrices.weekOf
import com.riztechacademy.mandibhav.data.SamplePrices
import com.riztechacademy.mandibhav.model.DayPrice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PriceWeekTest {

    private val price = SamplePrices.puneTomato
    private val week = weekOf(price)

    @Test
    fun changeIsTodayMinusYesterday() {
        // Pune tomato: 1,550 yesterday, 1,500 today.
        assertEquals(-50L, priceWeek(price, week).changeSinceYesterday)
    }

    @Test
    fun weekRangeIsTheLowestAndHighestModalPrice() {
        assertEquals(1_500L..1_800L, priceWeek(price, week).weekRange)
    }

    @Test
    fun oneDayHasNoChangeAndNoHistoryHasNoRange() {
        val oneDay = listOf(DayPrice(price.date, price.modalPrice))
        assertNull(priceWeek(price, oneDay).changeSinceYesterday)
        assertNull(priceWeek(price, emptyList()).weekRange)
    }

    @Test
    fun describesTheChangeInWords() {
        assertEquals("₹30 up on yesterday", describeChange(30))
        assertEquals("₹50 down on yesterday", describeChange(-50))
        assertEquals("Same as yesterday", describeChange(0))
        assertEquals("₹1,200 up on yesterday", describeChange(1_200))
    }
}
