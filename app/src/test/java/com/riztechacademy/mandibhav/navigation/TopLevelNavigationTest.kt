package com.riztechacademy.mandibhav.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopLevelNavigationTest {

    /** A fresh navigation state, as the app starts: on Prices, every stack at its root. */
    private fun navigation() = TopLevelNavigation(
        startDestination = Prices,
        currentTopLevel = mutableStateOf<NavKey>(Prices),
        backStacks = listOf(Prices, Markets, Alerts).associateWith { NavBackStack(it) },
    )

    @Test
    fun aScreenIsPushedOntoTheCurrentStack() {
        val nav = navigation()
        nav.navigate(PriceDetail("pune-onion-2026-11-07"))

        assertEquals(listOf(Prices, PriceDetail("pune-onion-2026-11-07")), nav.currentStack.toList())
    }

    @Test
    fun switchingTabsKeepsEachStack() {
        val nav = navigation()
        nav.navigate(PriceDetail("pune-onion-2026-11-07"))
        nav.navigate(Markets)
        nav.navigate(Market("Nashik APMC"))
        nav.navigate(Prices)

        assertEquals(Prices, nav.currentTopLevel)
        assertEquals(listOf(Prices, PriceDetail("pune-onion-2026-11-07")), nav.currentStack.toList())
        assertEquals(listOf(Markets, Market("Nashik APMC")), nav.backStacks.getValue(Markets).toList())
    }

    @Test
    fun tappingTheCurrentTabAgainReturnsToItsFirstScreen() {
        val nav = navigation()
        nav.navigate(Markets)
        nav.navigate(Market("Nashik APMC"))
        nav.navigate(PriceDetail("nashik-onion-2026-11-07"))

        nav.navigate(Markets)

        assertEquals(listOf<NavKey>(Markets), nav.currentStack.toList())
    }

    @Test
    fun backPopsThenReturnsToPricesThenHasNowhereToGo() {
        val nav = navigation()
        nav.navigate(Markets)
        nav.navigate(Market("Nashik APMC"))

        assertTrue(nav.goBack())
        assertEquals(listOf<NavKey>(Markets), nav.currentStack.toList())

        assertTrue(nav.goBack())
        assertEquals(Prices, nav.currentTopLevel)

        assertFalse(nav.goBack())
    }

    @Test
    fun theStartTabIsAlwaysUnderneath() {
        val nav = navigation()
        assertEquals(listOf<NavKey>(Prices), nav.stacksInUse())

        nav.navigate(Alerts)
        assertEquals(listOf(Prices, Alerts), nav.stacksInUse())
    }
}
