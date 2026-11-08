package com.riztechacademy.mandibhav.data

import com.riztechacademy.mandibhav.model.CommodityPrice
import com.riztechacademy.mandibhav.model.DayPrice
import java.time.LocalDate

/**
 * Hard-coded prices for building the screen. Realistic, not real: the app
 * fetches real prices in the networking module.
 */
object SamplePrices {
    private val day = LocalDate.of(2026, 11, 7)

    val lasalgaonOnion = CommodityPrice(
        id = "lasalgaon-onion-2026-11-07",
        commodity = "Onion",
        market = "Lasalgaon APMC",
        date = day,
        minPrice = 1_200,
        maxPrice = 2_851,
        modalPrice = 2_450,
    )

    val puneOnion = CommodityPrice(
        id = "pune-onion-2026-11-07",
        commodity = "Onion",
        market = "Pune (Gultekdi) APMC",
        date = day,
        minPrice = 1_000,
        maxPrice = 2_800,
        modalPrice = 2_200,
    )

    val nashikOnion = CommodityPrice(
        id = "nashik-onion-2026-11-07",
        commodity = "Onion",
        market = "Nashik APMC",
        date = day,
        minPrice = 1_100,
        maxPrice = 2_700,
        modalPrice = 2_300,
    )

    val puneTomato = CommodityPrice(
        id = "pune-tomato-2026-11-07",
        commodity = "Tomato",
        market = "Pune (Gultekdi) APMC",
        date = day,
        minPrice = 800,
        maxPrice = 2_000,
        modalPrice = 1_500,
    )

    val nashikTomato = CommodityPrice(
        id = "nashik-tomato-2026-11-07",
        commodity = "Tomato",
        market = "Nashik APMC",
        date = day,
        minPrice = 600,
        maxPrice = 1_800,
        modalPrice = 1_200,
    )

    val vashiPotato = CommodityPrice(
        id = "vashi-potato-2026-11-07",
        commodity = "Potato",
        market = "Vashi APMC, Navi Mumbai",
        date = day,
        minPrice = 1_600,
        maxPrice = 2_600,
        modalPrice = 2_100,
    )

    val vashiWheat = CommodityPrice(
        id = "vashi-wheat-2026-11-07",
        commodity = "Wheat",
        market = "Vashi APMC, Navi Mumbai",
        date = day,
        minPrice = 2_700,
        maxPrice = 3_300,
        modalPrice = 3_000,
    )

    val lasalgaonSoybean = CommodityPrice(
        id = "lasalgaon-soybean-2026-11-07",
        commodity = "Soybean",
        market = "Lasalgaon APMC",
        date = day,
        minPrice = 4_200,
        maxPrice = 4_800,
        modalPrice = 4_550,
    )

    val puneTur = CommodityPrice(
        id = "pune-tur-dal-2026-11-07",
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

    /**
     * The 4 pm closing prices: what a refresh brings in. Three prices change
     * under the same ids, and Nashik overtakes Lasalgaon as the best market
     * for onion.
     */
    val closingPrices = listOf(
        lasalgaonOnion.copy(maxPrice = 2_900, modalPrice = 2_525),
        nashikOnion.copy(modalPrice = 2_560),
        puneTomato.copy(modalPrice = 1_450),
    )

    /**
     * The modal price over the last seven days for each price above, oldest
     * first, keyed by price id. The last value is that price's modal price.
     */
    val weekOfModalPrices: Map<String, List<Long>> = mapOf(
        lasalgaonOnion.id to listOf(2_310, 2_280, 2_350, 2_400, 2_380, 2_420, 2_450),
        puneOnion.id to listOf(2_100, 2_150, 2_120, 2_180, 2_250, 2_230, 2_200),
        nashikOnion.id to listOf(2_150, 2_200, 2_180, 2_260, 2_240, 2_280, 2_300),
        puneTomato.id to listOf(1_800, 1_750, 1_700, 1_650, 1_600, 1_550, 1_500),
        nashikTomato.id to listOf(1_350, 1_300, 1_320, 1_250, 1_280, 1_220, 1_200),
        vashiPotato.id to listOf(2_050, 2_080, 2_060, 2_100, 2_090, 2_110, 2_100),
        vashiWheat.id to listOf(2_960, 2_970, 2_980, 2_980, 2_990, 3_000, 3_000),
        lasalgaonSoybean.id to listOf(4_480, 4_500, 4_520, 4_510, 4_540, 4_530, 4_550),
        puneTur.id to listOf(7_100, 7_150, 7_200, 7_180, 7_250, 7_280, 7_300),
    )

    /**
     * The week for [price] from [weekOfModalPrices], oldest day first. The
     * history ends on the day of the price, so its last value is the price's
     * own modal price, whatever a refresh has set it to. Empty without a history.
     */
    fun weekOf(
        price: CommodityPrice,
        history: Map<String, List<Long>> = weekOfModalPrices,
    ): List<DayPrice> {
        val modals = history[price.id]?.dropLast(1)?.plus(price.modalPrice) ?: return emptyList()
        return modals.mapIndexed { index, modal ->
            DayPrice(
                date = price.date.minusDays((modals.size - 1 - index).toLong()),
                modalPrice = modal,
            )
        }
    }
}
