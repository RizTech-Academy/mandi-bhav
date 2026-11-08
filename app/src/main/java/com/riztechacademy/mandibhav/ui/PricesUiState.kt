package com.riztechacademy.mandibhav.ui

import com.riztechacademy.mandibhav.model.CommodityPrice

/**
 * Everything the prices screen shows, at one moment, in one object. The
 * screen draws it; it never works anything out for itself.
 */
data class PricesUiState(
    /** True until the repository has answered for the first time. */
    val isLoading: Boolean = false,
    /** What the user has typed in the search field. */
    val query: String = "",
    /** The commodity chip that is selected, or null for "All". */
    val selectedCommodity: String? = null,
    /** One chip per commodity, in the order it first appears. */
    val commodities: List<String> = emptyList(),
    /** The prices to show, already filtered. */
    val prices: List<CommodityPrice> = emptyList(),
    /** The best market for the chosen commodity; null when "All" is selected. */
    val bestMarket: CommodityPrice? = null,
    /** True while a refresh is running. */
    val isRefreshing: Boolean = false,
    /** A message to show once, such as "3 prices updated.", until the screen says it has. */
    val userMessage: String? = null,
) {
    /** The filters match nothing. Not while loading: nothing has been matched yet. */
    val noResults: Boolean get() = !isLoading && prices.isEmpty()
}
