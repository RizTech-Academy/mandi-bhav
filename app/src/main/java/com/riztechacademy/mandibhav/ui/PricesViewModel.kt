package com.riztechacademy.mandibhav.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riztechacademy.mandibhav.data.SamplePrices
import com.riztechacademy.mandibhav.model.CommodityPrice
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Owns the prices screen's filter: the search query and the chosen commodity.
 *
 * The ViewModel outlives a rotation; the two filter values are also kept in
 * [savedStateHandle], so they come back after Android kills the process.
 * The price list itself is not saved: it is data, and is loaded again.
 */
class PricesViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Sample data for now; module 7 replaces this with a repository.
    private val prices: List<CommodityPrice> = SamplePrices.all

    /** One chip per commodity, in the order it first appears. */
    val commodities: List<String> = commoditiesIn(prices)

    /** What the user has typed in the search field. */
    val query: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")

    /** The commodity chip that is selected, or null for "All". */
    val selectedCommodity: StateFlow<String?> =
        savedStateHandle.getStateFlow(KEY_COMMODITY, null)

    /** The prices the screen shows: [prices] filtered by the two values above. */
    val visiblePrices: StateFlow<List<CommodityPrice>> =
        combine(query, selectedCommodity) { query, commodity ->
            filterPrices(prices, query, commodity)
        }.stateIn(
            scope = viewModelScope,
            // Keep filtering for 5 s after the screen stops collecting, so a
            // rotation (a second or less) does not restart the work.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = filterPrices(prices, query.value, selectedCommodity.value),
        )

    fun onQueryChange(newQuery: String) {
        savedStateHandle[KEY_QUERY] = newQuery
    }

    /** A tap on a chip: null is "All"; tapping the selected chip again clears it. */
    fun onCommodityClick(commodity: String?) {
        savedStateHandle[KEY_COMMODITY] =
            if (commodity == selectedCommodity.value) null else commodity
    }

    fun clearFilters() {
        savedStateHandle[KEY_QUERY] = ""
        savedStateHandle[KEY_COMMODITY] = null
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val KEY_COMMODITY = "commodity"
    }
}
