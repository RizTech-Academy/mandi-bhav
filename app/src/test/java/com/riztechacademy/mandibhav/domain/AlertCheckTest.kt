package com.riztechacademy.mandibhav.domain

import com.riztechacademy.mandibhav.data.SamplePrices
import com.riztechacademy.mandibhav.model.AlertDirection
import com.riztechacademy.mandibhav.model.PriceAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertCheckTest {

    /** Onion at Lasalgaon above ₹2,500; the sample price is ₹2,450. */
    private val onionAbove2500 = PriceAlert(1, "Onion", "Lasalgaon APMC", AlertDirection.Above, 2_500)
    private fun lasalgaonOnionAt(modal: Long) = listOf(SamplePrices.lasalgaonOnion.copy(modalPrice = modal))

    @Test
    fun aPriceThatCrossesTheLevelIsANewCrossing() {
        val check = checkAlerts(listOf(onionAbove2500), lasalgaonOnionAt(2_580))

        assertEquals(listOf(onionAbove2500), check.newlyCrossed.map { it.alert })
        assertEquals(2_580L, check.newlyCrossed.single().price.modalPrice)
        assertEquals(mapOf(1L to true), check.crossedChanges)
    }

    @Test
    fun theSameCrossingTheNextDayIsNotAnnouncedAgain() {
        val alreadyCrossed = onionAbove2500.copy(isCrossed = true)

        val check = checkAlerts(listOf(alreadyCrossed), lasalgaonOnionAt(2_610))

        assertTrue(check.newlyCrossed.isEmpty())
        assertTrue(check.crossedChanges.isEmpty())
    }

    @Test
    fun comingBackReArmsTheAlertWithoutAnnouncingIt() {
        val alreadyCrossed = onionAbove2500.copy(isCrossed = true)

        val check = checkAlerts(listOf(alreadyCrossed), lasalgaonOnionAt(2_420))

        assertTrue(check.newlyCrossed.isEmpty())
        assertEquals(mapOf(1L to false), check.crossedChanges)
    }

    @Test
    fun exactlyAtTheLevelIsNotAbove() {
        val check = checkAlerts(listOf(onionAbove2500), lasalgaonOnionAt(2_500))

        assertTrue(check.newlyCrossed.isEmpty())
    }

    @Test
    fun belowWorksTheOtherWayRound() {
        val tomatoBelow1300 = PriceAlert(2, "Tomato", "Nashik APMC", AlertDirection.Below, 1_300)

        val check = checkAlerts(listOf(tomatoBelow1300), SamplePrices.all) // Nashik tomato ₹1,200

        assertEquals(listOf(SamplePrices.nashikTomato), check.newlyCrossed.map { it.price })
    }

    @Test
    fun aMarketWithNoPriceTodayChangesNothing() {
        val solapur = onionAbove2500.copy(market = "Solapur APMC", isCrossed = true)

        val check = checkAlerts(listOf(solapur), SamplePrices.all)

        assertTrue(check.newlyCrossed.isEmpty())
        assertTrue(check.crossedChanges.isEmpty())
    }

    @Test
    fun anotherMarketsPriceDoesNotCount() {
        // Pune onion at ₹2,600 says nothing about Lasalgaon.
        val prices = listOf(SamplePrices.puneOnion.copy(modalPrice = 2_600), SamplePrices.lasalgaonOnion)

        assertTrue(checkAlerts(listOf(onionAbove2500), prices).newlyCrossed.isEmpty())
    }
}
