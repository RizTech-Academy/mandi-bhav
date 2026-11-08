package com.riztechacademy.mandibhav.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// One key per destination. A key is what goes on the back stack, and the
// back stack is saved, so every key is @Serializable and holds only small
// values: an id or a name, never the data the screen shows.

/** Top-level: today's prices, searchable. The start destination. */
@Serializable
data object Prices : NavKey

/** Top-level: the markets that reported today. */
@Serializable
data object Markets : NavKey

/** Top-level: the user's price alerts. */
@Serializable
data object Alerts : NavKey

/** One price in detail, by its id. */
@Serializable
data class PriceDetail(val priceId: String) : NavKey

/**
 * Today's price for one commodity at one market, whatever its date. What a
 * price-alert notification opens: an id would name the day it was posted.
 */
@Serializable
data class LatestPrice(val market: String, val commodity: String) : NavKey

/** One market's prices, by the market's name. */
@Serializable
data class Market(val name: String) : NavKey
