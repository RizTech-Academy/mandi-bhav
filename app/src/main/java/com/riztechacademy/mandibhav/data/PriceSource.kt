package com.riztechacademy.mandibhav.data

import com.riztechacademy.mandibhav.model.CommodityPrice
import com.riztechacademy.mandibhav.model.DayPrice

/**
 * Where the screens get prices from: for now, the sample data in memory.
 *
 * Every screen asks this for what it shows, by id, rather than being handed
 * a copy by the screen before it. Module 7 replaces it with a repository,
 * and the screens keep asking the same questions.
 */
class PriceSource(
    private val prices: List<CommodityPrice> = SamplePrices.all,
    private val weekOfModalPrices: Map<String, List<Long>> = SamplePrices.weekOfModalPrices,
) {
    /** Today's prices, every commodity at every market. */
    fun todaysPrices(): List<CommodityPrice> = prices

    /** One price by its id, or null if there is no such price (any more). */
    fun price(id: String): CommodityPrice? = prices.firstOrNull { it.id == id }

    /** Today's prices at one market. */
    fun pricesAt(market: String): List<CommodityPrice> = prices.filter { it.market == market }

    /**
     * The last seven days of modal prices for the price with [id], oldest
     * first and ending on that price's date; empty if there is no history.
     */
    fun week(id: String): List<DayPrice> {
        val price = price(id) ?: return emptyList()
        val modals = weekOfModalPrices[id] ?: return emptyList()
        return modals.mapIndexed { index, modal ->
            DayPrice(
                date = price.date.minusDays((modals.size - 1 - index).toLong()),
                modalPrice = modal,
            )
        }
    }
}
