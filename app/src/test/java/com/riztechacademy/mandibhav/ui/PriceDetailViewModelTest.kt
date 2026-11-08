package com.riztechacademy.mandibhav.ui

import com.riztechacademy.mandibhav.data.PriceSource
import com.riztechacademy.mandibhav.data.SamplePrices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PriceDetailViewModelTest {

    @Test
    fun loadsThePriceAndItsWeekFromTheId() {
        val id = SamplePrices.vashiWheat.id
        val viewModel = PriceDetailViewModel(id)

        val detail = viewModel.detail!!
        assertEquals(SamplePrices.vashiWheat, detail.price)
        assertEquals(7, detail.week.size)
        assertEquals(0L, detail.changeSinceYesterday)
    }

    @Test
    fun readsTodaysValueFromTheSourceNotFromWhoeverOpenedIt() {
        // The 4 pm update: the source now has a corrected price under the
        // same id. A screen that was handed the id shows the new value.
        val corrected = SamplePrices.lasalgaonOnion.copy(modalPrice = 2_525)
        val source = PriceSource(prices = listOf(corrected))

        val viewModel = PriceDetailViewModel(corrected.id, source)

        assertEquals(2_525L, viewModel.detail!!.price.modalPrice)
    }

    @Test
    fun anIdThatNoLongerExistsGivesNoDetail() {
        // What a back stack restored after process death can hold, once the
        // data has changed underneath it.
        val viewModel = PriceDetailViewModel("lasalgaon-onion-2026-10-01")
        assertNull(viewModel.detail)
    }
}
