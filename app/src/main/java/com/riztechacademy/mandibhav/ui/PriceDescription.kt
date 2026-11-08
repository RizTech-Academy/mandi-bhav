package com.riztechacademy.mandibhav.ui

import com.riztechacademy.mandibhav.model.CommodityPrice
import java.time.format.DateTimeFormatter
import java.util.Locale

private val spokenDateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)

/** The grouped digits of [formatRupees] without the rupee sign: "2,450". */
private fun groupedDigits(amount: Long): String = formatRupees(amount).replace("₹", "")

/**
 * An amount as a screen reader should say it: "2,450 rupees", not
 * "rupee sign 2,450".
 */
fun spokenRupees(amount: Long): String = "${groupedDigits(amount)} rupees"

/** What TalkBack reads for a whole price card: one sentence, in reading order. */
fun priceCardDescription(price: CommodityPrice): String =
    "${price.commodity} at ${price.market}, " +
        "${spokenRupees(price.modalPrice)} per quintal. " +
        "Range ${groupedDigits(price.minPrice)} to ${spokenRupees(price.maxPrice)}. " +
        spokenDateFormat.format(price.date) + "."
