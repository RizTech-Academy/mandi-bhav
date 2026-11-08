package com.riztechacademy.mandibhav.ui

import com.riztechacademy.mandibhav.MainDispatcherRule
import com.riztechacademy.mandibhav.data.FakePriceRepository
import com.riztechacademy.mandibhav.data.FakeUserPreferencesRepository
import com.riztechacademy.mandibhav.data.SamplePrices
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MarketsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun listsEachMarketThatReportedAndFollowsTheRepository() = runTest {
        val repository = FakePriceRepository(prices = listOf(SamplePrices.vashiWheat))
        val viewModel = MarketsViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf(MarketSummary("Vashi APMC, Navi Mumbai", "Thane", 1)), viewModel.uiState.value.markets)

        repository.prices = listOf(SamplePrices.vashiWheat, SamplePrices.puneTur)

        assertEquals(
            listOf(MarketSummary("Pune (Gultekdi) APMC", "Pune", 1), MarketSummary("Vashi APMC, Navi Mumbai", "Thane", 1)),
            viewModel.uiState.value.markets,
        )
    }

    @Test
    fun oneMarketShowsOnlyItsOwnPrices() = runTest {
        val viewModel = MarketViewModel("Nashik APMC", FakePriceRepository(), FakeUserPreferencesRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(listOf(SamplePrices.nashikOnion, SamplePrices.nashikTomato), viewModel.uiState.value.prices)
    }

    @Test
    fun theChipMakesThisTheHomeMarketAndATapClearsIt() = runTest {
        val preferences = FakeUserPreferencesRepository(homeMarket = "Lasalgaon APMC")
        val viewModel = MarketViewModel("Nashik APMC", FakePriceRepository(), preferences)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertFalse(viewModel.uiState.value.isHomeMarket)

        viewModel.toggleHomeMarket()
        assertTrue(viewModel.uiState.value.isHomeMarket)
        assertEquals("Nashik APMC", preferences.homeMarket.first())

        viewModel.toggleHomeMarket()
        assertFalse(viewModel.uiState.value.isHomeMarket)
        assertNull(preferences.homeMarket.first())
    }
}
