package com.riztechacademy.mandibhav.ui

import com.riztechacademy.mandibhav.data.RefreshFailure
import com.riztechacademy.mandibhav.model.CommodityPrice
import java.time.Instant

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
    /** The market the user sells at, whose prices come first; null until chosen. */
    val homeMarket: String? = null,
    /** The best market for the chosen commodity; null when "All" is selected. */
    val bestMarket: CommodityPrice? = null,
    /** When the prices on this phone were last fetched; null if never. */
    val lastUpdated: Instant? = null,
    /** The phone has no prices saved at all, as opposed to none that match. */
    val noSavedPrices: Boolean = false,
    /** True while a refresh is running. */
    val isRefreshing: Boolean = false,
    /** A message to show once, such as "3 prices updated.", until the screen says it has. */
    val userMessage: String? = null,
    /** Why the last refresh failed; null if it succeeded or none has run. */
    val refreshFailure: RefreshFailure? = null,
    /** Whether the phone has a working connection. */
    val isOnline: Boolean = true,
) {
    /** A phone with nothing saved is downloading for the first time: the screen says so in full. */
    val isFirstDownload: Boolean get() = noSavedPrices && isRefreshing

    /**
     * The filters match nothing. Not while loading, when nothing has been
     * matched yet, and not when there is nothing saved to match against.
     */
    val noResults: Boolean get() = !isLoading && !noSavedPrices && prices.isEmpty()
}
