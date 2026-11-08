package com.riztechacademy.mandibhav.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.riztechacademy.mandibhav.data.FakeAlertNotifier
import com.riztechacademy.mandibhav.data.FakeAlertRepository
import com.riztechacademy.mandibhav.data.FakeNetworkMonitor
import com.riztechacademy.mandibhav.data.FakePriceRepository
import com.riztechacademy.mandibhav.data.FakeUserPreferencesRepository
import com.riztechacademy.mandibhav.data.SamplePrices
import com.riztechacademy.mandibhav.domain.FindBestMarketUseCase
import com.riztechacademy.mandibhav.domain.RefreshPricesUseCase
import com.riztechacademy.mandibhav.ui.theme.MandiBhavTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The screen and its real ViewModel together, over fakes: what a farmer
 * types reaches the ViewModel, and what it decides reaches the screen.
 * JVM only (Robolectric), because the fakes live in the unit-test folder.
 */
@RunWith(AndroidJUnit4::class)
class PricesRouteTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val repository = FakePriceRepository()
    private val handle = SavedStateHandle()

    private fun showRoute() {
        val viewModel = PricesViewModel(
            handle,
            repository,
            FindBestMarketUseCase(repository),
            RefreshPricesUseCase(repository, FakeAlertRepository(), FakeAlertNotifier()),
            FakeUserPreferencesRepository(),
            FakeNetworkMonitor(),
        )
        compose.setContent {
            MandiBhavTheme {
                PricesRoute(onPriceClick = {}, viewModel = viewModel)
            }
        }
    }

    @Test
    fun typingInTheSearchFieldFiltersTheCards() {
        showRoute()

        compose.onNode(hasSetTextAction()).performTextInput("lasalgaon")

        assertEquals("lasalgaon", handle.get<String>("query"))
        compose.onNodeWithContentDescription(priceCardDescription(SamplePrices.lasalgaonOnion)).assertIsDisplayed()
        compose.onNodeWithContentDescription(priceCardDescription(SamplePrices.puneOnion)).assertDoesNotExist()
    }

    @Test
    fun refreshTellsTheFarmerWhatChanged() {
        repository.pricesAfterRefresh = SamplePrices.all.map {
            if (it.id == SamplePrices.lasalgaonOnion.id) it.copy(modalPrice = 2_720) else it
        }
        showRoute()

        compose.onNodeWithText("Refresh").performClick()

        compose.onNodeWithText("1 price updated.").assertIsDisplayed()
        assertEquals(1, repository.refreshCount)
    }
}
