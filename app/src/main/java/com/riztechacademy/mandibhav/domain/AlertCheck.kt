package com.riztechacademy.mandibhav.domain

import com.riztechacademy.mandibhav.model.CommodityPrice
import com.riztechacademy.mandibhav.model.PriceAlert
import com.riztechacademy.mandibhav.model.isMetBy

/** An alert whose level the price has just crossed, and the price that crossed it. */
data class AlertCrossing(val alert: PriceAlert, val price: CommodityPrice)

/**
 * What comparing the alerts with today's prices found: the alerts to tell the
 * user about, and every alert whose crossed state has changed, to save.
 */
data class AlertCheck(
    val newlyCrossed: List<AlertCrossing>,
    val crossedChanges: Map<Long, Boolean>,
)

/**
 * Compares each alert with today's price for its commodity at its market.
 *
 * - Past the level now and not before: a new crossing, to announce.
 * - Past it now and before: the same crossing as yesterday; say nothing.
 * - Back on the other side: no announcement, but the alert is ready again.
 * - No price today for that market: nothing changes. Small markets do not
 *   report every day, and a missing price is not a price that came back.
 */
fun checkAlerts(alerts: List<PriceAlert>, prices: List<CommodityPrice>): AlertCheck {
    val todays = prices.associateBy { it.market to it.commodity }
    val newlyCrossed = mutableListOf<AlertCrossing>()
    val changes = mutableMapOf<Long, Boolean>()
    for (alert in alerts) {
        val price = todays[alert.market to alert.commodity] ?: continue
        val crossedNow = alert.isMetBy(price.modalPrice)
        if (crossedNow && !alert.isCrossed) newlyCrossed += AlertCrossing(alert, price)
        if (crossedNow != alert.isCrossed) changes[alert.id] = crossedNow
    }
    return AlertCheck(newlyCrossed, changes)
}
