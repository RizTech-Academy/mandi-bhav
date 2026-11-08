package com.riztechacademy.mandibhav.domain

import com.riztechacademy.mandibhav.model.CommodityPrice

/**
 * The market paying the most for [commodity]: the highest modal price on the
 * latest day that commodity was reported, and on a tie the higher minimum
 * price, the better floor for a farmer. Null if no market reported it.
 *
 * Only the latest day counts, so a market that has not reported today
 * cannot win on an older, higher price.
 */
fun bestMarketFor(prices: List<CommodityPrice>, commodity: String): CommodityPrice? {
    val reported = prices.filter { it.commodity == commodity }
    val latestDay = reported.maxOfOrNull { it.date } ?: return null
    return reported
        .filter { it.date == latestDay }
        .maxWithOrNull(compareBy<CommodityPrice> { it.modalPrice }.thenBy { it.minPrice })
}
