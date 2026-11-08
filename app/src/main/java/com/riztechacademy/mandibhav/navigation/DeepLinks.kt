package com.riztechacademy.mandibhav.navigation

import androidx.navigation3.runtime.NavKey
import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Mandi Bhav's deep links. A custom scheme, not an https App Link: an App
 * Link needs a website whose owner publishes a file vouching for the app,
 * and this app has no website. The cost is that any app may claim the same
 * scheme, so a link here must never carry anything private, and the app
 * must treat whatever arrives as written by a stranger.
 *
 * mandibhav://price?market=Lasalgaon%20APMC&commodity=Onion   today's price
 * mandibhav://alerts                                          the Alerts tab
 */
const val DEEP_LINK_SCHEME = "mandibhav"

private const val PRICE_HOST = "price"
private const val ALERTS_HOST = "alerts"

/** No real market or commodity name is longer; anything longer is not one. */
private const val MAX_NAME_LENGTH = 80

/** The link that opens today's price for [commodity] at [market]. */
fun latestPriceLink(market: String, commodity: String): String =
    "$DEEP_LINK_SCHEME://$PRICE_HOST?market=${encode(market)}&commodity=${encode(commodity)}"

/** The link that opens the Alerts tab. */
fun alertsLink(): String = "$DEEP_LINK_SCHEME://$ALERTS_HOST"

/**
 * Where [link] asks to go, or null if it is not a link this app makes.
 * Called with whatever another app, a web page or adb sent, so it checks
 * everything and trusts nothing: the scheme, the host, that there is no path,
 * and that each name is present, of a sane length, and free of control
 * characters. A name that passes can still be one no market has; the screen
 * it opens says so, as it does for a price that has gone.
 */
fun parseDeepLink(link: String): NavKey? {
    val uri = try {
        URI(link)
    } catch (e: URISyntaxException) {
        return null
    }
    if (!uri.scheme.equals(DEEP_LINK_SCHEME, ignoreCase = true)) return null
    if (!uri.rawPath.isNullOrEmpty() && uri.rawPath != "/") return null
    return when (uri.host?.lowercase()) {
        PRICE_HOST -> {
            val query = queryOf(uri.rawQuery) ?: return null
            val market = query["market"]?.takeIf(::isSaneName) ?: return null
            val commodity = query["commodity"]?.takeIf(::isSaneName) ?: return null
            LatestPrice(market = market, commodity = commodity)
        }
        ALERTS_HOST -> Alerts
        else -> null
    }
}

/** The first value of each parameter, decoded; null if the query cannot be decoded. */
private fun queryOf(rawQuery: String?): Map<String, String>? {
    if (rawQuery.isNullOrEmpty()) return emptyMap()
    return try {
        rawQuery.split('&')
            .map { it.split('=', limit = 2) }
            .filter { it.size == 2 }
            .map { (key, value) -> decode(key) to decode(value) }
            .distinctBy { it.first }
            .toMap()
    } catch (e: IllegalArgumentException) {
        // A broken %-escape, such as "%zz".
        null
    }
}

private fun isSaneName(name: String): Boolean =
    name.isNotBlank() && name.length <= MAX_NAME_LENGTH && name.none { it.isISOControl() } && name.trim() == name

// The String overloads: the Charset ones arrived in Android 13 (API 33).
private fun encode(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

private fun decode(value: String) = URLDecoder.decode(value, "UTF-8")
