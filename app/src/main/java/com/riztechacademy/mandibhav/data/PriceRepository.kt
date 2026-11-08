package com.riztechacademy.mandibhav.data

import com.riztechacademy.mandibhav.model.CommodityPrice
import com.riztechacademy.mandibhav.model.DayPrice
import kotlinx.coroutines.flow.Flow

/**
 * The one place the app gets prices from. Screens ask it questions; they
 * never know whether the answer came from memory, a database or the network.
 *
 * Every answer is a [Flow]: it gives the current value when collected, and
 * a new value whenever the data changes, for as long as the screen watches.
 */
interface PriceRepository {

    /** Today's prices, every commodity at every market. */
    fun todaysPrices(): Flow<List<CommodityPrice>>

    /** One price by its id; null if there is no such price (any more). */
    fun price(id: String): Flow<CommodityPrice?>

    /** Today's prices at one market. */
    fun pricesAt(market: String): Flow<List<CommodityPrice>>

    /**
     * The last seven days of modal prices for the price with [id], oldest
     * first and ending on that price's date; empty if there is no history.
     */
    fun week(id: String): Flow<List<DayPrice>>

    /** Fetches the latest prices. Returns how many prices changed. */
    suspend fun refresh(): Int
}
