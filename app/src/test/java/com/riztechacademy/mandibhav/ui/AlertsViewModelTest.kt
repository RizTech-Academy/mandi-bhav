package com.riztechacademy.mandibhav.ui

import androidx.lifecycle.SavedStateHandle
import com.riztechacademy.mandibhav.MainDispatcherRule
import com.riztechacademy.mandibhav.data.FakeAlertRepository
import com.riztechacademy.mandibhav.data.FakePriceRepository
import com.riztechacademy.mandibhav.model.AlertDirection
import com.riztechacademy.mandibhav.model.PriceAlert
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlertsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val alerts = FakeAlertRepository()
    private fun newViewModel(handle: SavedStateHandle = SavedStateHandle()) =
        AlertsViewModel(handle, alerts, FakePriceRepository())

    private fun TestScope.collectUiState(viewModel: AlertsViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun choosingACommodityOffersOnlyTheMarketsThatHaveItToday() = runTest {
        val viewModel = newViewModel()
        collectUiState(viewModel)

        viewModel.onCommodityChosen("Onion")

        assertEquals(listOf("Lasalgaon APMC", "Nashik APMC", "Pune (Gultekdi) APMC"), viewModel.uiState.value.markets)
    }

    @Test
    fun theFormShowsTodaysPriceAsAGuide() = runTest {
        val viewModel = newViewModel()
        collectUiState(viewModel)

        viewModel.onCommodityChosen("Onion")
        viewModel.onMarketChosen("Lasalgaon APMC")

        assertEquals(2_450L, viewModel.uiState.value.todaysPrice)
    }

    @Test
    fun aDifferentCommodityClearsTheMarket() = runTest {
        val viewModel = newViewModel()
        collectUiState(viewModel)
        viewModel.onCommodityChosen("Onion")
        viewModel.onMarketChosen("Lasalgaon APMC")

        viewModel.onCommodityChosen("Wheat")

        assertNull(viewModel.uiState.value.form.market)
    }

    @Test
    fun savingSavesTheAlertAndClearsTheLevel() = runTest {
        val viewModel = newViewModel()
        collectUiState(viewModel)
        viewModel.onCommodityChosen("Onion")
        viewModel.onMarketChosen("Lasalgaon APMC")
        viewModel.onLevelChange("2500")

        assertTrue(viewModel.save())

        val saved = alerts.alerts.single()
        assertEquals(PriceAlert(saved.id, "Onion", "Lasalgaon APMC", AlertDirection.Above, 2_500), saved)
        assertEquals("", viewModel.uiState.value.form.level)
        assertEquals("Above ₹2,500 a quintal. Today ₹2,450.", viewModel.uiState.value.alerts.single().description())
    }

    @Test
    fun anIncompleteFormSaysWhatIsMissing() = runTest {
        val viewModel = newViewModel()
        collectUiState(viewModel)

        assertFalse(viewModel.save())
        assertEquals("Choose a commodity.", viewModel.uiState.value.form.error)

        viewModel.onCommodityChosen("Onion")
        viewModel.onMarketChosen("Lasalgaon APMC")
        assertFalse(viewModel.save())
        assertEquals("Enter the price in whole rupees a quintal, such as 2500.", viewModel.uiState.value.form.error)
        assertTrue(alerts.alerts.isEmpty())
    }

    @Test
    fun theLevelKeepsDigitsOnly() = runTest {
        val viewModel = newViewModel()
        collectUiState(viewModel)

        viewModel.onLevelChange("₹2,500.50")

        assertEquals("250050", viewModel.uiState.value.form.level)
    }

    @Test
    fun aHalfTypedAlertSurvivesProcessDeath() = runTest {
        val handle = SavedStateHandle()
        newViewModel(handle).apply {
            onCommodityChosen("Tomato")
            onMarketChosen("Nashik APMC")
            onDirectionChosen(AlertDirection.Below)
            onLevelChange("1000")
        }

        val restored = newViewModel(SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) }))
        collectUiState(restored)

        assertEquals(
            NewAlertForm("Tomato", "Nashik APMC", AlertDirection.Below, "1000"),
            restored.uiState.value.form,
        )
    }

    @Test
    fun deletingRemovesTheAlert() = runTest {
        val viewModel = newViewModel()
        collectUiState(viewModel)
        val id = alerts.add("Onion", "Lasalgaon APMC", AlertDirection.Above, 2_500)

        viewModel.delete(id)

        assertTrue(viewModel.uiState.value.alerts.isEmpty())
    }

    @Test
    fun aRowSaysWhenThePriceIsPastTheLevel() {
        val row = AlertRow(PriceAlert(1, "Onion", "Lasalgaon APMC", AlertDirection.Above, 2_500, isCrossed = true), 2_580)
        assertEquals("Above ₹2,500 a quintal. Today ₹2,580, past your level.", row.description())

        val noPrice = AlertRow(PriceAlert(2, "Onion", "Solapur APMC", AlertDirection.Below, 2_000), null)
        assertEquals("Below ₹2,000 a quintal. No price today.", noPrice.description())
    }
}
