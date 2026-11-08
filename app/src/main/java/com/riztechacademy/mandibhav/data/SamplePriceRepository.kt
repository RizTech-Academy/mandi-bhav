package com.riztechacademy.mandibhav.data

import com.riztechacademy.mandibhav.model.CommodityPrice
import com.riztechacademy.mandibhav.model.DayPrice
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * The prices from [SamplePrices], held in memory. The data is fake; the
 * contract is the real one, so a database (module 8) and the network
 * (module 9) can replace this class without any screen changing.
 *
 * One instance is shared by the whole app: it is the single source of truth.
 * Every flow below is derived from [prices], so a refresh reaches every
 * screen that is watching.
 */
class SamplePriceRepository(
    initialPrices: List<CommodityPrice> = SamplePrices.all,
    private val weekOfModalPrices: Map<String, List<Long>> = SamplePrices.weekOfModalPrices,
    private val closingPrices: List<CommodityPrice> = SamplePrices.closingPrices,
    private val networkDelayMillis: Long = 1_000,
) : PriceRepository {

    /** The constructor Hilt uses: the sample data, with every default. */
    @Inject
    constructor() : this(initialPrices = SamplePrices.all)

    private val prices = MutableStateFlow(initialPrices)

    override fun todaysPrices(): Flow<List<CommodityPrice>> = prices.asStateFlow()

    override fun price(id: String): Flow<CommodityPrice?> =
        prices.map { all -> all.firstOrNull { it.id == id } }.distinctUntilChanged()

    override fun pricesAt(market: String): Flow<List<CommodityPrice>> =
        prices.map { all -> all.filter { it.market == market } }.distinctUntilChanged()

    override fun week(id: String): Flow<List<DayPrice>> =
        price(id).map { price -> price?.let { SamplePrices.weekOf(it, weekOfModalPrices) }.orEmpty() }

    /**
     * Applies the markets' closing prices, after a pause that stands in for
     * the network until module 9. A second refresh finds nothing new.
     */
    override suspend fun refresh(): Int {
        delay(networkDelayMillis)
        val closing = closingPrices.associateBy { it.id }
        var changed = 0
        prices.update { current ->
            val updated = current.map { closing[it.id] ?: it }
            changed = updated.indices.count { updated[it] != current[it] }
            updated
        }
        return changed
    }
}
