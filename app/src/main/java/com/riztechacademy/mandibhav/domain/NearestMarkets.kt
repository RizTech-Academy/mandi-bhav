package com.riztechacademy.mandibhav.domain

import com.riztechacademy.mandibhav.model.GeoPoint
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** The Earth's mean radius, in kilometres. */
private const val EARTH_RADIUS_KM = 6_371.0

/**
 * The straight-line distance between two places, in kilometres, by the
 * haversine formula. Not the road distance: a market 30 km away across a
 * river may be 50 km by road. Good to well under 1 % at these distances,
 * which is far better than an approximate location is.
 */
fun distanceKm(from: GeoPoint, to: GeoPoint): Double {
    val lat1 = Math.toRadians(from.latitude)
    val lat2 = Math.toRadians(to.latitude)
    val dLat = lat2 - lat1
    val dLon = Math.toRadians(to.longitude - from.longitude)
    val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
    return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
}

/** One market and how far it is from the user. */
data class MarketDistance(val market: String, val km: Double)

/**
 * The markets with a known location, nearest to [from] first. A market the
 * server gave no location for is left out rather than guessed.
 */
fun nearestMarkets(from: GeoPoint, markets: Map<String, GeoPoint?>): List<MarketDistance> =
    markets.mapNotNull { (name, location) -> location?.let { MarketDistance(name, distanceKm(from, it)) } }
        .sortedBy { it.km }
