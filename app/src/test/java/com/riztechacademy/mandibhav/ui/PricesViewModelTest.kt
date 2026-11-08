package com.riztechacademy.mandibhav.ui

import androidx.lifecycle.SavedStateHandle
import com.riztechacademy.mandibhav.MainDispatcherRule
import com.riztechacademy.mandibhav.data.SamplePrices
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PricesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /**
     * visiblePrices only updates while someone collects it
     * (SharingStarted.WhileSubscribed), so each test collects it the way the
     * screen does, for as long as the test runs.
     */
    private fun TestScope.collectVisiblePrices(viewModel: PricesViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.visiblePrices.collect {}
        }
    }

    @Test
    fun startsWithNoFilterAndEveryPrice() = runTest {
        val viewModel = PricesViewModel(SavedStateHandle())
        collectVisiblePrices(viewModel)

        assertEquals("", viewModel.query.value)
        assertNull(viewModel.selectedCommodity.value)
        assertEquals(SamplePrices.all, viewModel.visiblePrices.value)
    }

    @Test
    fun queryAndChipFilterTogether() = runTest {
        val viewModel = PricesViewModel(SavedStateHandle())
        collectVisiblePrices(viewModel)

        viewModel.onQueryChange("pune")
        viewModel.onCommodityClick("Tomato")

        assertEquals(listOf(SamplePrices.puneTomato), viewModel.visiblePrices.value)
    }

    @Test
    fun tappingTheSelectedChipAgainClearsIt() = runTest {
        val viewModel = PricesViewModel(SavedStateHandle())
        collectVisiblePrices(viewModel)

        viewModel.onCommodityClick("Onion")
        assertEquals("Onion", viewModel.selectedCommodity.value)
        assertEquals(3, viewModel.visiblePrices.value.size)

        viewModel.onCommodityClick("Onion")
        assertNull(viewModel.selectedCommodity.value)
        assertEquals(SamplePrices.all, viewModel.visiblePrices.value)
    }

    @Test
    fun clearFiltersShowsEveryPriceAgain() = runTest {
        val viewModel = PricesViewModel(SavedStateHandle())
        collectVisiblePrices(viewModel)
        viewModel.onQueryChange("vashi")
        viewModel.onCommodityClick("Onion")
        assertEquals(emptyList<Any>(), viewModel.visiblePrices.value)

        viewModel.clearFilters()

        assertEquals("", viewModel.query.value)
        assertNull(viewModel.selectedCommodity.value)
        assertEquals(SamplePrices.all, viewModel.visiblePrices.value)
    }

    @Test
    fun filterIsWrittenToSavedState() {
        val handle = SavedStateHandle()
        val viewModel = PricesViewModel(handle)

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
        val restored = PricesViewModel(handle)
        collectVisiblePrices(restored)

        assertEquals("lasalgaon", restored.query.value)
        assertEquals("Onion", restored.selectedCommodity.value)
        assertEquals(listOf(SamplePrices.lasalgaonOnion), restored.visiblePrices.value)
    }
}
