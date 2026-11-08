package com.riztechacademy.mandibhav.ui

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
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PriceDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModelFor(id: String, repository: FakePriceRepository = FakePriceRepository()) =
        PriceDetailViewModel(id, repository, FindBestMarketUseCase(repository))

    /** Collects uiState as the screen would, so WhileSubscribed starts it. */
    private fun TestScope.collectUiState(viewModel: PriceDetailViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
    }

    private fun PriceDetailViewModel.found() = uiState.value as PriceDetailUiState.Found

    @Test
    fun isLoadingUntilSomebodyCollects() {
        val viewModel = viewModelFor(SamplePrices.vashiWheat.id)

        assertEquals(PriceDetailUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun loadsThePriceAndItsWeekFromTheId() = runTest {
        val id = SamplePrices.vashiWheat.id
        val repository = FakePriceRepository(weeks = mapOf(id to SamplePrices.weekOf(SamplePrices.vashiWheat)))
        val viewModel = viewModelFor(id, repository)
        collectUiState(viewModel)

        val detail = viewModel.found().detail
        assertEquals(SamplePrices.vashiWheat, detail.price)
        assertEquals(7, detail.week.size)
        assertEquals(0L, detail.changeSinceYesterday)
    }

    @Test
    fun anIdThatNoLongerExistsIsNotFound() = runTest {
        // What a back stack restored after process death can hold, once the
        // data has changed underneath it.
        val viewModel = viewModelFor("lasalgaon-onion-2026-10-01")
        collectUiState(viewModel)

        assertEquals(PriceDetailUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun namesTheBestMarketForTheCommodity() = runTest {
        val viewModel = viewModelFor(SamplePrices.puneOnion.id)
        collectUiState(viewModel)

        assertEquals(SamplePrices.lasalgaonOnion, viewModel.found().bestMarket)
    }

    @Test
    fun anOpenScreenFollowsTheRepository() = runTest {
        // Kavita has Lasalgaon onion open when the 4 pm prices arrive.
        val repository = FakePriceRepository()
        val viewModel = viewModelFor(SamplePrices.lasalgaonOnion.id, repository)
        collectUiState(viewModel)
        assertEquals(2_450L, viewModel.found().detail.price.modalPrice)

        repository.prices = SamplePrices.all.map { price ->
            SamplePrices.closingPrices.firstOrNull { it.id == price.id } ?: price
        }

        assertEquals(2_525L, viewModel.found().detail.price.modalPrice)
        assertEquals(SamplePrices.closingPrices[1], viewModel.found().bestMarket) // Nashik, 2,560
    }

    @Test
    fun aPriceThatDisappearsWhileOpenBecomesNotFound() = runTest {
        val repository = FakePriceRepository()
        val viewModel = viewModelFor(SamplePrices.puneTur.id, repository)
        collectUiState(viewModel)

        repository.prices = SamplePrices.all - SamplePrices.puneTur

        assertEquals(PriceDetailUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun aNotificationsPriceFollowsTheNewDay() = runTest {
        // Posted for 7 November's onion; tapped after 8 November's arrived.
        val repository = FakePriceRepository()
        val viewModel = LatestPriceViewModel("Lasalgaon APMC", "Onion", repository, FindBestMarketUseCase(repository))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertEquals(SamplePrices.lasalgaonOnion, (viewModel.uiState.value as PriceDetailUiState.Found).detail.price)

        val nextDay = SamplePrices.lasalgaonOnion.copy(
            id = "lasalgaon-onion-2026-11-08",
            date = SamplePrices.lasalgaonOnion.date.plusDays(1),
            modalPrice = 2_580,
        )
        repository.prices = SamplePrices.all.map { if (it.id == SamplePrices.lasalgaonOnion.id) nextDay else it }

        assertEquals(nextDay, (viewModel.uiState.value as PriceDetailUiState.Found).detail.price)
    }
}
