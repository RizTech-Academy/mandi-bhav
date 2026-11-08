package com.riztechacademy.mandibhav.data

import com.riztechacademy.mandibhav.model.CommodityPrice
import com.riztechacademy.mandibhav.model.GeoPoint
import java.time.LocalDate

/** Where the prices come from: the prices server, through Retrofit. */
interface PriceNetworkDataSource {
    /** Today's prices and the markets they are from. Throws [RefreshException] on failure. */
    suspend fun fetchLatestPrices(): LatestPrices

    /** The modal prices of the last few days. Throws [RefreshException] on failure. */
    suspend fun fetchHistory(): List<PastPrice>
}

/** One download of today's prices, with each market's district and, where known, its location. */
data class LatestPrices(
    val markets: Map<String, String>,
    val prices: List<CommodityPrice>,
    val locations: Map<String, GeoPoint> = emptyMap(),
)

/** One commodity's modal price at one market on an earlier day. */
data class PastPrice(
    val market: String,
    val commodity: String,
    val date: LocalDate,
    val modalPrice: Long,
)
