package com.riztechacademy.mandibhav

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.riztechacademy.mandibhav.data.SamplePrices
import com.riztechacademy.mandibhav.ui.priceCardDescription
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The whole app on a device, from MainActivity, with Hilt's real graph
 * except the price repository (TestPriceRepositoryModule). Slower than the
 * JVM tests and needs an emulator or phone; it is the only test here that
 * proves the real activity, navigation and dependency graph fit together.
 */
@HiltAndroidTest
class MainActivityTest {

    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun inject() {
        hilt.inject()
    }

    @Test
    fun aCardOpensItsPriceAndBackReturnsToTheList() {
        val card = priceCardDescription(SamplePrices.lasalgaonOnion)
        compose.onNodeWithContentDescription(card).performClick()

        compose.onNodeWithText("Share").assertIsDisplayed()

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithContentDescription(card).assertIsDisplayed()
    }

    @Test
    fun theMarketsTabListsEveryMarket() {
        compose.onNodeWithText("Markets").performClick()

        compose.onNodeWithText("Lasalgaon APMC").assertIsDisplayed()
    }
}
