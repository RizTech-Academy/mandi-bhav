package com.riztechacademy.mandibhav.data

import com.riztechacademy.mandibhav.model.CommodityPrice
import com.riztechacademy.mandibhav.model.DayPrice
import com.riztechacademy.mandibhav.model.Market
import java.time.Instant
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

    /** Every market, by name, each with its prices today. */
    fun markets(): Flow<List<Market>>

    /** One market and its prices today; null if there is no such market. */
    fun market(name: String): Flow<Market?>

    /**
     * The last seven days of modal prices for the price with [id], oldest
     * first and ending on that price's date; empty if there is no history.
     */
    fun week(id: String): Flow<List<DayPrice>>

    /** When prices were last fetched successfully; null if never. */
    fun lastUpdated(): Flow<Instant?>

    /**
     * Fetches the latest prices. Returns how many prices changed. Throws an
     * [java.io.IOException] when there is no connection; the saved prices
     * are left as they were.
     */
    suspend fun refresh(): Int
}
