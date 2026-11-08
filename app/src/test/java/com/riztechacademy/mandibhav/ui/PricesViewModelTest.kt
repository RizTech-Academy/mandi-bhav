package com.riztechacademy.mandibhav.ui

import androidx.lifecycle.SavedStateHandle
import com.riztechacademy.mandibhav.MainDispatcherRule
import com.riztechacademy.mandibhav.data.FakeNetworkMonitor
import com.riztechacademy.mandibhav.data.FakePriceRepository
import com.riztechacademy.mandibhav.data.RefreshFailure
import com.riztechacademy.mandibhav.data.FakeUserPreferencesRepository
import com.riztechacademy.mandibhav.data.SamplePrices
import com.riztechacademy.mandibhav.data.FakeAlertNotifier
import com.riztechacademy.mandibhav.data.FakeAlertRepository
import com.riztechacademy.mandibhav.domain.FindBestMarketUseCase
import com.riztechacademy.mandibhav.domain.RefreshPricesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PricesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun newViewModel(
        handle: SavedStateHandle = SavedStateHandle(),
        repository: FakePriceRepository = FakePriceRepository(),
        preferences: FakeUserPreferencesRepository = FakeUserPreferencesRepository(),
        networkMonitor: FakeNetworkMonitor = FakeNetworkMonitor(),
    ) = PricesViewModel(
        handle,
        repository,
        FindBestMarketUseCase(repository),
        RefreshPricesUseCase(repository, FakeAlertRepository(), FakeAlertNotifier()),
        preferences,
        networkMonitor,
    )

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

    @Test
    fun theHomeMarketsPricesComeFirstAndFollowThePreference() = runTest {
        val preferences = FakeUserPreferencesRepository()
        val viewModel = newViewModel(preferences = preferences)
        collectUiState(viewModel)
        viewModel.onCommodityClick("Onion")
        assertEquals("Lasalgaon APMC", viewModel.uiState.value.prices.first().market)

        preferences.setHomeMarket("Pune (Gultekdi) APMC")

        assertEquals("Pune (Gultekdi) APMC", viewModel.uiState.value.homeMarket)
        assertEquals("Pune (Gultekdi) APMC", viewModel.uiState.value.prices.first().market)
        // The chips keep their order: the home market moves prices, not commodities.
        assertEquals("Onion", viewModel.uiState.value.commodities.first())
    }

    @Test
    fun withNoConnectionTheSavedPricesStayAndTheMessageSaysWhy() = runTest {
        val repository = FakePriceRepository().apply { offline = true }
        val viewModel = newViewModel(repository = repository)
        collectUiState(viewModel)

        viewModel.refresh()

        val state = viewModel.uiState.value
        assertEquals("No connection. Showing the prices saved on this phone.", state.userMessage)
        assertEquals(SamplePrices.all, state.prices)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun anEmptyPhoneIsNotTheSameAsNoMatches() = runTest {
        val viewModel = newViewModel(repository = FakePriceRepository(prices = emptyList()))
        collectUiState(viewModel)

        assertEquals(true, viewModel.uiState.value.noSavedPrices)
        assertFalse(viewModel.uiState.value.noResults)
    }

    @Test
    fun anEmptyPhoneDownloadsTodaysPricesWithoutATap() = runTest {
        val repository = FakePriceRepository(prices = emptyList(), pricesAfterRefresh = SamplePrices.all)
        val viewModel = newViewModel(repository = repository)
        collectUiState(viewModel)

        assertEquals(1, repository.refreshCount)
        assertEquals(SamplePrices.all, viewModel.uiState.value.prices)
        assertFalse(viewModel.uiState.value.noSavedPrices)
    }

    @Test
    fun aPhoneWithSavedPricesDoesNotDownloadOnOpen() = runTest {
        val repository = FakePriceRepository()
        val viewModel = newViewModel(repository = repository)
        collectUiState(viewModel)

        assertEquals(0, repository.refreshCount)
    }

    @Test
    fun aFailedFirstDownloadIsShownInTheBodyNotASnackbar() = runTest {
        val repository = FakePriceRepository(prices = emptyList()).apply { failure = RefreshFailure.Server(503) }
        val viewModel = newViewModel(repository = repository)
        collectUiState(viewModel)

        val state = viewModel.uiState.value
        assertTrue(state.noSavedPrices)
        assertEquals(RefreshFailure.Server(503), state.refreshFailure)
        assertNull(state.userMessage)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun aServerErrorOverSavedPricesSaysWhatHappened() = runTest {
        val repository = FakePriceRepository().apply { failure = RefreshFailure.Server(503) }
        val viewModel = newViewModel(repository = repository)
        collectUiState(viewModel)

        viewModel.refresh()

        assertEquals(
            "The prices server has a problem (error 503). Try again later.",
            viewModel.uiState.value.userMessage,
        )
        assertEquals(SamplePrices.all, viewModel.uiState.value.prices)
    }

    @Test
    fun aSuccessfulRefreshClearsTheLastFailure() = runTest {
        val repository = FakePriceRepository().apply { failure = RefreshFailure.Timeout }
        val viewModel = newViewModel(repository = repository)
        collectUiState(viewModel)
        viewModel.refresh()
        assertEquals(RefreshFailure.Timeout, viewModel.uiState.value.refreshFailure)

        repository.failure = null
        viewModel.refresh()

        assertNull(viewModel.uiState.value.refreshFailure)
    }

    @Test
    fun backOnlineAfterAFailedRefreshItTriesAgainByItself() = runTest {
        val network = FakeNetworkMonitor(online = false)
        val repository = FakePriceRepository(prices = emptyList(), pricesAfterRefresh = SamplePrices.all)
            .apply { offline = true }
        val viewModel = newViewModel(repository = repository, networkMonitor = network)
        collectUiState(viewModel)
        assertEquals(RefreshFailure.NoConnection, viewModel.uiState.value.refreshFailure)
        assertFalse(viewModel.uiState.value.isOnline)

        repository.offline = false
        network.online = true

        assertEquals(2, repository.refreshCount)
        assertEquals(SamplePrices.all, viewModel.uiState.value.prices)
        assertNull(viewModel.uiState.value.refreshFailure)
    }

    @Test
    fun backOnlineWithNothingFailedDoesNotRefresh() = runTest {
        val network = FakeNetworkMonitor(online = false)
        val repository = FakePriceRepository()
        val viewModel = newViewModel(repository = repository, networkMonitor = network)
        collectUiState(viewModel)

        network.online = true

        assertEquals(0, repository.refreshCount)
    }

    @Test
    fun aSecondTapWhileRefreshingIsIgnored() = runTest {
        // The fake's refresh finishes instantly unless it is held, and an
        // instant refresh is over before the second tap: the guard in
        // refresh() would never be tested.
        val repository = FakePriceRepository().apply { holdRefreshes() }
        val viewModel = newViewModel(repository = repository)
        collectUiState(viewModel)

        viewModel.refresh()
        assertTrue(viewModel.uiState.value.isRefreshing)
        viewModel.refresh()
        repository.releaseRefreshes()

        assertEquals(1, repository.refreshCount)
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertEquals("Prices are up to date.", viewModel.uiState.value.userMessage)
    }

    @Test
    fun aRefreshIsShownStartingAndFinishing() = runTest {
        val repository = FakePriceRepository().apply { holdRefreshes() }
        val viewModel = newViewModel(repository = repository)
        val seen = mutableListOf<Boolean>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.map { it.isRefreshing }.distinctUntilChanged().toList(seen)
        }

        viewModel.refresh()
        repository.releaseRefreshes()

        assertEquals(listOf(false, true, false), seen)
    }

    @Test
    fun theStateKeepsWorkingForFiveSecondsAfterTheScreenStops() = runTest {
        // WhileSubscribed(5_000): a rotation stops collecting for well under
        // a second, so the work carries on; leaving the screen stops it.
        // runTest's clock is virtual: these five seconds take no time.
        val repository = FakePriceRepository()
        val viewModel = newViewModel(repository = repository)
        val screen = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        assertEquals(1, repository.todaysPricesSubscribers)

        screen.cancel()
        advanceTimeBy(4_999)
        assertEquals(1, repository.todaysPricesSubscribers)

        advanceTimeBy(2)
        assertEquals(0, repository.todaysPricesSubscribers)
    }

    @Test
    fun nobodyWatchingNobodySyncs() = runTest {
        // The connection comes back while no screen collects the state:
        // nothing is watching the network, so nothing refreshes.
        val network = FakeNetworkMonitor(online = false)
        val repository = FakePriceRepository().apply { offline = true }
        val viewModel = newViewModel(repository = repository, networkMonitor = network)
        viewModel.refresh()
        assertEquals(1, repository.refreshCount)

        repository.offline = false
        network.online = true

        assertEquals(1, repository.refreshCount)
    }
}
