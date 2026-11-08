package com.riztechacademy.mandibhav.ui

import com.riztechacademy.mandibhav.model.CommodityPrice
import com.riztechacademy.mandibhav.model.DayPrice

/**
 * Everything the detail screen shows for one price: the price itself, its
 * last seven days, and the two numbers worked out from them.
 */
data class PriceWeek(
    val price: CommodityPrice,
    val week: List<DayPrice>,
    /** Today's modal price minus yesterday's; null without a yesterday. */
    val changeSinceYesterday: Long?,
    /** The lowest and highest modal price of the week; null without a history. */
    val weekRange: LongRange?,
)

/** Builds the detail for [price] from its [week], oldest day first. */
fun priceWeek(price: CommodityPrice, week: List<DayPrice>): PriceWeek {
    val modals = week.map { it.modalPrice }
    return PriceWeek(
        price = price,
        week = week,
        changeSinceYesterday = if (modals.size >= 2) modals.last() - modals[modals.size - 2] else null,
        weekRange = if (modals.isEmpty()) null else modals.min()..modals.max(),
    )
}

/** The change since yesterday in words: "₹30 up on yesterday". */
fun describeChange(change: Long): String = when {
    change > 0 -> "${formatRupees(change)} up on yesterday"
    change < 0 -> "${formatRupees(-change)} down on yesterday"
    else -> "Same as yesterday"
}
