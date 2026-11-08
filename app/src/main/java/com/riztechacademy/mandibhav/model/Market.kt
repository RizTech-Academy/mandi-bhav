package com.riztechacademy.mandibhav.model

/**
 * An APMC market, the district it is in, and the prices it reported today.
 * [location] is roughly where the market yard is; null when the server has
 * not said, as older downloads did not.
 */
data class Market(
    val name: String,
    val district: String,
    val prices: List<CommodityPrice>,
    val location: GeoPoint? = null,
)
