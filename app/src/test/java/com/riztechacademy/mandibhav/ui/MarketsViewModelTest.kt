package com.riztechacademy.mandibhav.ui

import com.riztechacademy.mandibhav.MainDispatcherRule
import com.riztechacademy.mandibhav.data.FakeLocationSource
import com.riztechacademy.mandibhav.data.FakePriceRepository
import com.riztechacademy.mandibhav.data.LocationResult
import com.riztechacademy.mandibhav.model.GeoPoint
import kotlinx.coroutines.CompletableDeferred
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
        val viewModel = MarketsViewModel(repository, FakeLocationSource(), FakeUserPreferencesRepository())
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

    /** Niphad, between Lasalgaon and Nashik: 15 km from Lasalgaon and 34 km from Nashik in a straight line. */
    private val niphad = GeoPoint(20.0800, 74.1100)

    @Test
    fun aFixSortsTheMarketsNearestFirstAndNamesTheNearest() = runTest {
        val location = FakeLocationSource(LocationResult.Found(niphad, accuracyMetres = 2_000f))
        val viewModel = MarketsViewModel(FakePriceRepository(), location, FakeUserPreferencesRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertEquals(NearestMarket.NotAsked, viewModel.uiState.value.nearest)
        assertEquals(0, location.calls)

        viewModel.findNearest()

        val state = viewModel.uiState.value
        assertEquals(NearestMarket.Found("Lasalgaon APMC", 15), state.nearest)
        assertEquals(
            listOf("Lasalgaon APMC", "Nashik APMC", "Vashi APMC, Navi Mumbai", "Pune (Gultekdi) APMC"),
            state.markets.map { it.name },
        )
        assertEquals(listOf(15, 34, 160, 179), state.markets.map { it.distanceKm })
    }

    @Test
    fun whileFindingTheCardSaysSoAndASecondTapDoesNotAskAgain() = runTest {
        val location = FakeLocationSource().apply { pending = CompletableDeferred() }
        val viewModel = MarketsViewModel(FakePriceRepository(), location, FakeUserPreferencesRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.findNearest()
        viewModel.findNearest()
        assertEquals(NearestMarket.Finding, viewModel.uiState.value.nearest)
        assertEquals(1, location.calls)

        location.pending!!.complete(LocationResult.NoFix)
        assertEquals(NearestMarket.NoFix, viewModel.uiState.value.nearest)
        // No fix, no distances: the list stays in name order.
        assertEquals("Lasalgaon APMC", viewModel.uiState.value.markets.first().name)
        assertNull(viewModel.uiState.value.markets.first().distanceKm)
    }

    @Test
    fun locationOffAndNoPermissionAreReportedAsThemselves() = runTest {
        val location = FakeLocationSource(LocationResult.LocationOff)
        val viewModel = MarketsViewModel(FakePriceRepository(), location, FakeUserPreferencesRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.findNearest()
        assertEquals(NearestMarket.LocationOff, viewModel.uiState.value.nearest)

        location.result = LocationResult.NoPermission
        viewModel.findNearest()
        assertEquals(NearestMarket.NoPermission, viewModel.uiState.value.nearest)
    }

    @Test
    fun aFixWithNoMarketLocationsSaysTheLocationsAreMissingNotTheFix() {
        val markets = listOf(com.riztechacademy.mandibhav.model.Market("Lasalgaon APMC", "Nashik", emptyList(), location = null))

        val state = marketsUiState(markets, LocationResult.Found(niphad, null), isFinding = false)

        assertEquals(NearestMarket.NoMarketLocations, state.nearest)
    }

    @Test
    fun aMarketWithNoLocationGoesLastWithNoDistance() {
        val markets = listOf(
            com.riztechacademy.mandibhav.model.Market("Unknown APMC", "", emptyList(), location = null),
            com.riztechacademy.mandibhav.model.Market("Lasalgaon APMC", "Nashik", emptyList(), SamplePrices.locations["Lasalgaon APMC"]),
        )

        val state = marketsUiState(markets, LocationResult.Found(niphad, null), isFinding = false)

        assertEquals(listOf("Lasalgaon APMC", "Unknown APMC"), state.markets.map { it.name })
        assertEquals(listOf(15, null), state.markets.map { it.distanceKm })
    }

    @Test
    fun aRefusalIsRememberedUntilThePermissionIsGranted() = runTest {
        val preferences = FakeUserPreferencesRepository()
        val viewModel = MarketsViewModel(FakePriceRepository(), FakeLocationSource(), preferences)
        assertFalse(viewModel.locationRefusedBefore.value)

        viewModel.setLocationRefused(true)
        assertTrue(viewModel.locationRefusedBefore.value)

        viewModel.setLocationRefused(false)
        assertFalse(viewModel.locationRefusedBefore.value)
    }
}
