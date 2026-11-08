package com.riztechacademy.mandibhav.data.local

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Relation
import com.riztechacademy.mandibhav.model.Market

/** One row of `markets`: an APMC market and the district it is in. */
@Entity(tableName = "markets")
data class MarketEntity(
    @PrimaryKey val name: String,
    val district: String,
)

/**
 * A market and its prices today, read together. Room runs two queries, one
 * for the markets and one for all their prices, inside one transaction, and
 * matches each price's `market` column to a market's `name`.
 */
data class MarketWithPrices(
    @Embedded val market: MarketEntity,
    @Relation(parentColumns = ["name"], entityColumns = ["market"])
    val prices: List<PriceEntity>,
)

fun MarketWithPrices.asModel() = Market(
    name = market.name,
    district = market.district,
    prices = prices.sortedBy { it.commodity }.map { it.asModel() },
)
