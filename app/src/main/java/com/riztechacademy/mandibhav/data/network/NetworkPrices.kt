package com.riztechacademy.mandibhav.data.network

import com.riztechacademy.mandibhav.data.LatestPrices
import com.riztechacademy.mandibhav.data.PastPrice
import com.riztechacademy.mandibhav.model.CommodityPrice
import java.time.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl

/*
 * The JSON the prices server sends, field for field, in the server's names.
 * These classes belong to the network layer: nothing outside this package
 * sees them. The mapping functions turn them into the app's own types, so a
 * renamed field on the server is a change here and nowhere else.
 */

/** `prices.json`: one day's prices at every market. */
@Serializable
data class NetworkPrices(
    @SerialName("schema_version") val schemaVersion: Int,
    val date: String,
    val markets: List<NetworkMarket>,
    val prices: List<NetworkPrice>,
)

@Serializable
data class NetworkMarket(
    val name: String,
    val district: String,
)

@Serializable
data class NetworkPrice(
    val id: String,
    val commodity: String,
    val market: String,
    val date: String,
    @SerialName("min_price") val minPrice: Long,
    @SerialName("max_price") val maxPrice: Long,
    @SerialName("modal_price") val modalPrice: Long,
    /** Paths relative to the API's base URL; optional, so an older server still parses. */
    val thumbnail: String? = null,
    val photo: String? = null,
)

/** `history.json`: modal prices for the days before `prices.json`'s. */
@Serializable
data class NetworkHistory(
    @SerialName("schema_version") val schemaVersion: Int,
    val history: List<NetworkPastPrice>,
)

@Serializable
data class NetworkPastPrice(
    val market: String,
    val commodity: String,
    val date: String,
    @SerialName("modal_price") val modalPrice: Long,
)

/**
 * The app's view of a download. Picture paths are resolved against
 * [baseUrl] here, because only the network layer knows where the API lives.
 * A date that is not an ISO date throws `DateTimeParseException`.
 */
fun NetworkPrices.asLatestPrices(baseUrl: HttpUrl) = LatestPrices(
    markets = markets.associate { it.name to it.district },
    prices = prices.map { it.asModel(baseUrl) },
)

fun NetworkPrice.asModel(baseUrl: HttpUrl) = CommodityPrice(
    id = id,
    commodity = commodity,
    market = market,
    date = LocalDate.parse(date),
    minPrice = minPrice,
    maxPrice = maxPrice,
    modalPrice = modalPrice,
    thumbnailUrl = thumbnail?.let { baseUrl.resolve(it)?.toString() },
    photoUrl = photo?.let { baseUrl.resolve(it)?.toString() },
)

fun NetworkHistory.asPastPrices() = history.map {
    PastPrice(market = it.market, commodity = it.commodity, date = LocalDate.parse(it.date), modalPrice = it.modalPrice)
}
