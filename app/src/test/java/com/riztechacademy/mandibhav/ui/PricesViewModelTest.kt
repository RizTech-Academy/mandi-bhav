package com.riztechacademy.mandibhav.ui

import androidx.lifecycle.SavedStateHandle
import com.riztechacademy.mandibhav.MainDispatcherRule
import com.riztechacademy.mandibhav.data.FakePriceRepository
import com.riztechacademy.mandibhav.data.SamplePrices
import com.riztechacademy.mandibhav.domain.FindBestMarketUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PricesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun newViewModel(
        handle: SavedStateHandle = SavedStateHandle(),
        repository: FakePriceRepository = FakePriceRepository(),
    ) = PricesViewModel(handle, repository, FindBestMarketUseCase(repository))

    /**
     * uiState only updates while someone collects it
     * (SharingStarted.WhileSubscribed), so each test collects it the way the
     * screen does, for as long as the test runs.
     */
    private fun TestScope.collectUiState(viewModel: PricesViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
    }

    @Test
    fun startsWithNoFilterAndEveryPrice() = runTest {
        val viewModel = newViewModel()
        collectUiState(viewModel)

        val state = viewModel.uiState.value
        assertEquals("", state.query)
        assertNull(state.selectedCommodity)
        assertEquals(SamplePrices.all, state.prices)
        assertEquals(listOf("Onion", "Tomato", "Potato", "Wheat", "Soybean", "Tur dal"), state.commodities)
        assertNull(state.bestMarket)
    }

    @Test
    fun queryAndChipFilterTogether() = runTest {
        val viewModel = newViewModel()
        collectUiState(viewModel)

        viewModel.onQueryChange("pune")
        viewModel.onCommodityClick("Tomato")

        assertEquals(listOf(SamplePrices.puneTomato), viewModel.uiState.value.prices)
    }

    @Test
    fun tappingTheSelectedChipAgainClearsIt() = runTest {
        val viewModel = newViewModel()
        collectUiState(viewModel)

        viewModel.onCommodityClick("Onion")
        assertEquals("Onion", viewModel.uiState.value.selectedCommodity)
        assertEquals(3, viewModel.uiState.value.prices.size)

        viewModel.onCommodityClick("Onion")
        assertNull(viewModel.uiState.value.selectedCommodity)
        assertEquals(SamplePrices.all, viewModel.uiState.value.prices)
    }

    @Test
    fun clearFiltersShowsEveryPriceAgain() = runTest {
        val viewModel = newViewModel()
        collectUiState(viewModel)
        viewModel.onQueryChange("vashi")
        viewModel.onCommodityClick("Onion")
        assertEquals(true, viewModel.uiState.value.noResults)

        viewModel.clearFilters()

        assertEquals("", viewModel.uiState.value.query)
        assertNull(viewModel.uiState.value.selectedCommodity)
        assertEquals(SamplePrices.all, viewModel.uiState.value.prices)
    }

    @Test
    fun choosingACommodityNamesItsBestMarket() = runTest {
        val viewModel = newViewModel()
        collectUiState(viewModel)

        viewModel.onCommodityClick("Onion")

        assertEquals(SamplePrices.lasalgaonOnion, viewModel.uiState.value.bestMarket)
    }

    @Test
    fun filterIsWrittenToSavedState() {
        val handle = SavedStateHandle()
        val viewModel = newViewModel(handle)

        viewModel.onQueryChange("lasalgaon")
        viewModel.onCommodityClick("Onion")

        assertEquals("lasalgaon", handle.get<String>("query"))
        assertEquals("Onion", handle.get<String>("commodity"))
    }

    @Test
    fun newViewModelRestoresTheFilterFromSavedState() = runTest {
        // What Android does after process death: a brand-new ViewModel,
        // handed the values that were saved before the process was killed.
        val handle = SavedStateHandle(mapOf("query" to "lasalgaon", "commodity" to "Onion"))
        val restored = newViewModel(handle)
        collectUiState(restored)

        assertEquals("lasalgaon", restored.uiState.value.query)
        assertEquals("Onion", restored.uiState.value.selectedCommodity)
        assertEquals(listOf(SamplePrices.lasalgaonOnion), restored.uiState.value.prices)
    }

    @Test
    fun refreshShowsTheNewPricesAndTheNewBestMarket() = runTest {
        val repository = FakePriceRepository(
            pricesAfterRefresh = SamplePrices.all.map {
                if (it.id == SamplePrices.nashikOnion.id) it.copy(modalPrice = 2_560) else it
            },
        )
        val viewModel = newViewModel(repository = repository)
        collectUiState(viewModel)
        viewModel.onCommodityClick("Onion")

        viewModel.refresh()

        val state = viewModel.uiState.value
        assertEquals(2_560L, state.prices.first { it.market == "Nashik APMC" }.modalPrice)
        assertEquals("Nashik APMC", state.bestMarket?.market)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun refreshLeavesAMessageUntilTheScreenHasShownIt() = runTest {
        val repository = FakePriceRepository(
            pricesAfterRefresh = SamplePrices.all.map { it.copy(modalPrice = it.modalPrice + 10) },
        )
        val viewModel = newViewModel(repository = repository)
        collectUiState(viewModel)

        viewModel.refresh()
        assertEquals("9 prices updated.", viewModel.uiState.value.userMessage)

        viewModel.userMessageShown()
        assertNull(viewModel.uiState.value.userMessage)
    }

    @Test
    fun aRefreshThatChangesNothingSaysSo() = runTest {
        val viewModel = newViewModel()
        collectUiState(viewModel)

        viewModel.refresh()

        assertEquals("Prices are up to date.", viewModel.uiState.value.userMessage)
    }

    @Test
    fun whileLoadingTheFilterIsKnownButNothingIsMissing() {
        // Before anyone collects: the repository has not answered yet.
        val handle = SavedStateHandle(mapOf("query" to "pune"))
        val state = newViewModel(handle).uiState.value

        assertEquals(true, state.isLoading)
        assertEquals("pune", state.query)
        assertFalse(state.noResults)
    }

    @Test
    fun theListFollowsTheRepositoryWithoutARefresh() = runTest {
        val repository = FakePriceRepository()
        val viewModel = newViewModel(repository = repository)
        collectUiState(viewModel)
        viewModel.onCommodityClick("Wheat")

        repository.prices = SamplePrices.all.map {
            if (it.id == SamplePrices.vashiWheat.id) it.copy(modalPrice = 3_050) else it
        }

        assertEquals(3_050L, viewModel.uiState.value.prices.single().modalPrice)
        assertEquals(3_050L, viewModel.uiState.value.bestMarket?.modalPrice)
    }
}
