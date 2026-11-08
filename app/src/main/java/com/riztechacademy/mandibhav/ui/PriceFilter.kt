package com.riztechacademy.mandibhav.ui

import com.riztechacademy.mandibhav.model.CommodityPrice

/** Each commodity once, in the order it first appears: the filter chips. */
fun commoditiesIn(prices: List<CommodityPrice>): List<String> =
    prices.map { it.commodity }.distinct()

/**
 * [prices] with the home market's first, each group in its original order.
 * With no home market, or one with no prices today, nothing moves.
 */
fun homeMarketFirst(prices: List<CommodityPrice>, homeMarket: String?): List<CommodityPrice> {
    if (homeMarket == null) return prices
    val (home, others) = prices.partition { it.market == homeMarket }
    return home + others
}

/**
 * The prices to show for a search query and an optional chosen commodity.
 * The query matches commodity or market, ignoring case and surrounding spaces;
 * a null commodity means "all".
 */
fun filterPrices(
    prices: List<CommodityPrice>,
    query: String,
    commodity: String?,
): List<CommodityPrice> {
    val q = query.trim()
    return prices.filter { price ->
        (commodity == null || price.commodity == commodity) &&
            (q.isEmpty() ||
                price.commodity.contains(q, ignoreCase = true) ||
                price.market.contains(q, ignoreCase = true))
    }
}
