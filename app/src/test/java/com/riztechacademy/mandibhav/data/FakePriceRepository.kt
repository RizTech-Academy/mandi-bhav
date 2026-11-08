package com.riztechacademy.mandibhav.data

import com.riztechacademy.mandibhav.model.CommodityPrice
import com.riztechacademy.mandibhav.model.DayPrice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * A [PriceRepository] for tests. A test sets [prices] to whatever the case
 * needs, and every flow follows it, as the real repository's do. A refresh
 * swaps in [pricesAfterRefresh]; the fake counts its refreshes and has no delay.
 */
class FakePriceRepository(
    prices: List<CommodityPrice> = SamplePrices.all,
    var weeks: Map<String, List<DayPrice>> = emptyMap(),
    var pricesAfterRefresh: List<CommodityPrice> = prices,
) : PriceRepository {

    private val current = MutableStateFlow(prices)

    /** The prices the repository holds now. Setting it is "the data changed". */
    var prices: List<CommodityPrice>
        get() = current.value
        set(value) {
            current.value = value
        }

    var refreshCount = 0
        private set

    override fun todaysPrices(): Flow<List<CommodityPrice>> = current

    override fun price(id: String): Flow<CommodityPrice?> =
        current.map { all -> all.firstOrNull { it.id == id } }

    override fun pricesAt(market: String): Flow<List<CommodityPrice>> =
        current.map { all -> all.filter { it.market == market } }

    override fun week(id: String): Flow<List<DayPrice>> = current.map { weeks[id].orEmpty() }

    override suspend fun refresh(): Int {
        refreshCount++
        val changed = pricesAfterRefresh.count { it !in prices }
        prices = pricesAfterRefresh
        return changed
    }
}
