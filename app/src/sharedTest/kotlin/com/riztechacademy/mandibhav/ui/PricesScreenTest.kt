package com.riztechacademy.mandibhav.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.core.layout.WindowSizeClass
import com.riztechacademy.mandibhav.data.RefreshFailure
import com.riztechacademy.mandibhav.data.SamplePrices
import com.riztechacademy.mandibhav.ui.theme.MandiBhavTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The prices screen on its own, handed a state and checked for what a
 * farmer would see and be able to do. No ViewModel and no repository: the
 * screen is stateless, so a test gives it any state directly.
 *
 * These tests run twice over: on the JVM with Robolectric
 * (./gradlew testDebugUnitTest) and on an emulator or phone
 * (./gradlew connectedDebugAndroidTest).
 */
@RunWith(AndroidJUnit4::class)
class PricesScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var clickedPriceId: String? = null
    private var refreshes = 0
    private var filtersCleared = 0

    private fun showScreen(state: PricesUiState) {
        compose.setContent {
            MandiBhavTheme {
                PricesScreen(
                    uiState = state,
                    onQueryChange = {},
                    onCommodityClick = {},
                    onClearFilters = { filtersCleared++ },
                    onRefresh = { refreshes++ },
                    onUserMessageShown = {},
                    onPriceClick = { clickedPriceId = it },
                    // A phone-sized window, whatever the test device is.
                    windowSizeClass = WindowSizeClass(minWidthDp = 400, minHeightDp = 800),
                )
            }
        }
    }

    private val everyPrice = PricesUiState(
        commodities = commoditiesIn(SamplePrices.all),
        prices = SamplePrices.all,
    )

    @Test
    fun aCardIsFoundByTheSentenceTalkBackReads() {
        showScreen(everyPrice)

        // The card replaces its texts with one description, so there is no
        // node with the text "Lasalgaon APMC" to find.
        compose.onNodeWithContentDescription(priceCardDescription(SamplePrices.lasalgaonOnion))
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun tappingACardOpensThatPrice() {
        showScreen(everyPrice)

        val card = priceCardDescription(SamplePrices.puneOnion)
        // Scroll first: on a small screen, such as Robolectric's default
        // 320 x 470 dp, the second card is not composed until it is needed.
        compose.onNodeWithTag(PRICES_GRID_TAG).performScrollToNode(hasContentDescription(card))
        compose.onNodeWithContentDescription(card).performClick()

        assertEquals(SamplePrices.puneOnion.id, clickedPriceId)
    }

    @Test
    fun aFilteredStateShowsOnlyItsCards() {
        // What module 5 promised: hand the screen a filtered list and check
        // the cards, without typing anything.
        showScreen(
            everyPrice.copy(
                query = "pune",
                selectedCommodity = "Tomato",
                prices = listOf(SamplePrices.puneTomato),
                bestMarket = SamplePrices.puneTomato,
            ),
        )

        compose.onNodeWithContentDescription(priceCardDescription(SamplePrices.puneTomato)).assertIsDisplayed()
        compose.onNodeWithContentDescription(priceCardDescription(SamplePrices.nashikTomato)).assertDoesNotExist()
    }

    @Test
    fun theLastCardIsThereOnceTheGridScrollsToIt() {
        showScreen(everyPrice)
        val lastCard = priceCardDescription(SamplePrices.all.last())

        // Off screen, a lazy grid has not composed it at all.
        compose.onNodeWithContentDescription(lastCard).assertDoesNotExist()

        compose.onNodeWithTag(PRICES_GRID_TAG).performScrollToNode(hasContentDescription(lastCard))
        compose.onNodeWithContentDescription(lastCard).assertIsDisplayed()
    }

    @Test
    fun refreshIsOffWhileARefreshRuns() {
        showScreen(everyPrice.copy(isRefreshing = true))

        compose.onNodeWithText("Refresh").assertIsNotEnabled()
    }

    @Test
    fun noMatchesOffersToClearTheFilters() {
        showScreen(everyPrice.copy(query = "vashi", selectedCommodity = "Onion", prices = emptyList()))

        compose.onNodeWithText("No prices match \"vashi\".").assertIsDisplayed()
        compose.onNodeWithText("Clear filters").performClick()

        assertEquals(1, filtersCleared)
    }

    @Test
    fun aFailedRefreshOffersRetryInTheSnackbar() {
        showScreen(
            everyPrice.copy(
                userMessage = "No connection. Showing the prices saved on this phone.",
                refreshFailure = RefreshFailure.NoConnection,
            ),
        )

        compose.onNodeWithText("No connection. Showing the prices saved on this phone.").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()

        assertEquals(1, refreshes)
    }
}
