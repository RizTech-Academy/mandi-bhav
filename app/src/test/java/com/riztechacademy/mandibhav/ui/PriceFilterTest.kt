package com.riztechacademy.mandibhav.ui

import com.riztechacademy.mandibhav.data.SamplePrices
import org.junit.Assert.assertEquals
import org.junit.Test

class PriceFilterTest {

    private val prices = SamplePrices.all

    @Test
    fun commoditiesAreDistinctInFirstSeenOrder() {
        assertEquals(
            listOf("Onion", "Tomato", "Potato", "Wheat", "Soybean", "Tur dal"),
            commoditiesIn(prices),
        )
    }

    @Test
    fun emptyQueryAndNoCommodityShowsEverything() {
        assertEquals(prices, filterPrices(prices, query = "", commodity = null))
    }

    @Test
    fun commodityChipKeepsOnlyThatCommodity() {
        val onions = filterPrices(prices, query = "", commodity = "Onion")
        assertEquals(3, onions.size)
        assertEquals(setOf("Onion"), onions.map { it.commodity }.toSet())
    }

    @Test
    fun queryMatchesMarketIgnoringCaseAndSpaces() {
        val lasalgaon = filterPrices(prices, query = "  lasalgaon ", commodity = null)
        assertEquals(listOf(SamplePrices.lasalgaonOnion, SamplePrices.lasalgaonSoybean), lasalgaon)
    }

    @Test
    fun queryAndCommodityCombine() {
        assertEquals(
            listOf(SamplePrices.puneTomato),
            filterPrices(prices, query = "pune", commodity = "Tomato"),
        )
        assertEquals(emptyList<Any>(), filterPrices(prices, query = "vashi", commodity = "Onion"))
    }

    @Test
    fun theHomeMarketComesFirstAndNothingElseMoves() {
        val ordered = homeMarketFirst(prices, homeMarket = "Vashi APMC, Navi Mumbai")
        assertEquals(listOf(SamplePrices.vashiPotato, SamplePrices.vashiWheat), ordered.take(2))
        assertEquals(prices - setOf(SamplePrices.vashiPotato, SamplePrices.vashiWheat), ordered.drop(2))

        assertEquals(prices, homeMarketFirst(prices, homeMarket = null))
        assertEquals(prices, homeMarketFirst(prices, homeMarket = "A market with no prices today"))
    }
}
