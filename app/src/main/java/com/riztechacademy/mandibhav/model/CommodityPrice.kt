package com.riztechacademy.mandibhav.model

import java.time.LocalDate

/**
 * One day's price for one commodity at one market.
 *
 * Prices are whole rupees per quintal (100 kg), the unit APMC markets report in.
 * They are Long, never Double: a price is money, and money has to add up exactly.
 *
 * [id] identifies this price and nothing else: a list uses it as the item key,
 * and navigation passes it to the detail screen instead of the whole object.
 *
 * [thumbnailUrl] and [photoUrl] are pictures of the commodity, small for a
 * card and large for the detail screen; null when the server sent none.
 */
data class CommodityPrice(
    val id: String,
    val commodity: String,
    val market: String,
    val date: LocalDate,
    val minPrice: Long,
    val maxPrice: Long,
    val modalPrice: Long,
    val thumbnailUrl: String? = null,
    val photoUrl: String? = null,
)
