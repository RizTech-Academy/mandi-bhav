package com.riztechacademy.mandibhav.model

import java.time.LocalDate

/**
 * One day's price for one commodity at one market.
 *
 * Prices are whole rupees per quintal (100 kg), the unit APMC markets report in.
 * They are Long, never Double: a price is money, and money has to add up exactly.
 */
data class CommodityPrice(
    val commodity: String,
    val market: String,
    val date: LocalDate,
    val minPrice: Long,
    val maxPrice: Long,
    val modalPrice: Long,
)
