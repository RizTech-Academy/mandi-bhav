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

/** Top-level: price alerts, a placeholder until the notifications module. */
@Serializable
data object Alerts : NavKey

/** One price in detail, by its id. */
@Serializable
data class PriceDetail(val priceId: String) : NavKey

/** One market's prices, by the market's name. */
@Serializable
data class Market(val name: String) : NavKey
