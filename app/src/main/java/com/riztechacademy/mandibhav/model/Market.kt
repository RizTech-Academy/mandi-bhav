package com.riztechacademy.mandibhav.model

/** An APMC market, the district it is in, and the prices it reported today. */
data class Market(
    val name: String,
    val district: String,
    val prices: List<CommodityPrice>,
)
