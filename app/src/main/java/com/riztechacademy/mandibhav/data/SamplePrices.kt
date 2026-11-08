package com.riztechacademy.mandibhav.data

import com.riztechacademy.mandibhav.model.CommodityPrice
import java.time.LocalDate

/**
 * Hard-coded prices for building the screen. Realistic, not real: the app
 * fetches real prices in the networking module.
 */
object SamplePrices {
    private val day = LocalDate.of(2026, 11, 7)

    val lasalgaonOnion = CommodityPrice(
        commodity = "Onion",
        market = "Lasalgaon APMC",
        date = day,
        minPrice = 1_200,
        maxPrice = 2_851,
        modalPrice = 2_450,
    )

    val puneOnion = CommodityPrice(
        commodity = "Onion",
        market = "Pune (Gultekdi) APMC",
        date = day,
        minPrice = 1_000,
        maxPrice = 2_800,
        modalPrice = 2_200,
    )

    val nashikOnion = CommodityPrice(
        commodity = "Onion",
        market = "Nashik APMC",
        date = day,
        minPrice = 1_100,
        maxPrice = 2_700,
        modalPrice = 2_300,
    )

    val puneTomato = CommodityPrice(
        commodity = "Tomato",
        market = "Pune (Gultekdi) APMC",
        date = day,
        minPrice = 800,
        maxPrice = 2_000,
        modalPrice = 1_500,
    )

    val nashikTomato = CommodityPrice(
        commodity = "Tomato",
        market = "Nashik APMC",
        date = day,
        minPrice = 600,
        maxPrice = 1_800,
        modalPrice = 1_200,
    )

    val vashiPotato = CommodityPrice(
        commodity = "Potato",
        market = "Vashi APMC, Navi Mumbai",
        date = day,
        minPrice = 1_600,
        maxPrice = 2_600,
        modalPrice = 2_100,
    )

    val vashiWheat = CommodityPrice(
        commodity = "Wheat",
        market = "Vashi APMC, Navi Mumbai",
        date = day,
        minPrice = 2_700,
        maxPrice = 3_300,
        modalPrice = 3_000,
    )

    val lasalgaonSoybean = CommodityPrice(
        commodity = "Soybean",
        market = "Lasalgaon APMC",
        date = day,
        minPrice = 4_200,
        maxPrice = 4_800,
        modalPrice = 4_550,
    )

    val puneTur = CommodityPrice(
        commodity = "Tur dal",
        market = "Pune (Gultekdi) APMC",
        date = day,
        minPrice = 6_800,
        maxPrice = 7_800,
        modalPrice = 7_300,
    )

    val all = listOf(
        lasalgaonOnion,
        puneOnion,
        nashikOnion,
        puneTomato,
        nashikTomato,
        vashiPotato,
        vashiWheat,
        lasalgaonSoybean,
        puneTur,
    )
}
