package com.riztechacademy.mandibhav.data

import com.riztechacademy.mandibhav.model.CommodityPrice
import com.riztechacademy.mandibhav.model.DayPrice
import com.riztechacademy.mandibhav.model.Market
import java.io.IOException
import java.time.Instant
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

    /** When the prices were last fetched; a refresh sets it to [refreshedAt]. */
    val lastUpdatedFlow = MutableStateFlow<Instant?>(null)
    var refreshedAt: Instant = Instant.parse("2026-11-07T10:35:00Z")

    /** Set to make the next refreshes fail as they would with no connection. */
    var offline = false

    override fun todaysPrices(): Flow<List<CommodityPrice>> = current

    override fun price(id: String): Flow<CommodityPrice?> =
        current.map { all -> all.firstOrNull { it.id == id } }

    override fun markets(): Flow<List<Market>> = current.map { all ->
        all.groupBy { it.market }.map { (name, prices) -> Market(name, SamplePrices.districts[name].orEmpty(), prices) }
            .sortedBy { it.name }
    }

    override fun market(name: String): Flow<Market?> = markets().map { markets -> markets.firstOrNull { it.name == name } }

    override fun week(id: String): Flow<List<DayPrice>> = current.map { weeks[id].orEmpty() }

    override fun lastUpdated(): Flow<Instant?> = lastUpdatedFlow

    override suspend fun refresh(): Int {
        refreshCount++
        if (offline) throw IOException("No network connection")
        lastUpdatedFlow.value = refreshedAt
        val changed = pricesAfterRefresh.count { it !in prices }
        prices = pricesAfterRefresh
        return changed
    }
}
