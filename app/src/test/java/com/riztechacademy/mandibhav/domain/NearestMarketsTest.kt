package com.riztechacademy.mandibhav.domain

import com.riztechacademy.mandibhav.data.SamplePrices
import com.riztechacademy.mandibhav.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class NearestMarketsTest {

    private val lasalgaon = SamplePrices.locations.getValue("Lasalgaon APMC")
    private val nashik = SamplePrices.locations.getValue("Nashik APMC")

    @Test
    fun distanceIsSymmetricAndZeroToItself() {
        assertEquals(0.0, distanceKm(lasalgaon, lasalgaon), 1e-9)
        assertEquals(distanceKm(lasalgaon, nashik), distanceKm(nashik, lasalgaon), 1e-9)
    }

    @Test
    fun lasalgaonToNashikIsAbout49KmInAStraightLine() {
        assertEquals(48.8, distanceKm(lasalgaon, nashik), 0.1)
    }

    @Test
    fun oneDegreeOfLatitudeIsAbout111Km() {
        assertEquals(111.2, distanceKm(GeoPoint(18.0, 73.0), GeoPoint(19.0, 73.0)), 0.1)
    }

    @Test
    fun nearestFirstAndMarketsWithNoLocationLeftOut() {
        val fromKharadi = GeoPoint(18.5500, 73.9400)
        val nearest = nearestMarkets(fromKharadi, SamplePrices.locations + ("Unknown APMC" to null))

        assertEquals(
            listOf("Pune (Gultekdi) APMC", "Vashi APMC, Navi Mumbai", "Nashik APMC", "Lasalgaon APMC"),
            nearest.map { it.market },
        )
        assertEquals(10.5, nearest.first().km, 0.1)
    }
}
