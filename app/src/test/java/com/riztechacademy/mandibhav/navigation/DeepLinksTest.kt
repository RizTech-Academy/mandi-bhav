package com.riztechacademy.mandibhav.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Every link arrives from outside the app, so most of these are links an attacker or a typo could send. */
class DeepLinksTest {

    @Test
    fun aPriceLinkRoundTripsEvenWithSpacesCommasAndBrackets() {
        for (market in listOf("Lasalgaon APMC", "Vashi APMC, Navi Mumbai", "Pune (Gultekdi) APMC")) {
            assertEquals(LatestPrice(market, "Tur dal"), parseDeepLink(latestPriceLink(market, "Tur dal")))
        }
    }

    @Test
    fun theLinkIsWhatTheLessonPrints() {
        assertEquals(
            "mandibhav://price?market=Lasalgaon%20APMC&commodity=Onion",
            latestPriceLink("Lasalgaon APMC", "Onion"),
        )
        assertEquals(Alerts, parseDeepLink(alertsLink()))
    }

    @Test
    fun aPlusIsASpaceAsBrowsersWriteIt() {
        assertEquals(LatestPrice("Lasalgaon APMC", "Onion"), parseDeepLink("mandibhav://price?market=Lasalgaon+APMC&commodity=Onion"))
    }

    @Test
    fun theFirstOfARepeatedParameterWins() {
        assertEquals(
            LatestPrice("Nashik APMC", "Onion"),
            parseDeepLink("mandibhav://price?market=Nashik%20APMC&market=Lasalgaon%20APMC&commodity=Onion"),
        )
    }

    @Test
    fun linksThatAreNotOursAreIgnored() {
        val notOurs = listOf(
            "https://price?market=Nashik%20APMC&commodity=Onion",
            "mandibhav://prices?market=Nashik%20APMC&commodity=Onion",
            "mandibhav://price/extra?market=Nashik%20APMC&commodity=Onion",
            "mandibhav://settings",
            "mandibhav:price",
            "not a uri at all",
            "",
        )
        for (link in notOurs) assertNull(link, parseDeepLink(link))
    }

    @Test
    fun missingBlankOverlongOrControlCharacterNamesAreRefused() {
        val bad = listOf(
            "mandibhav://price?market=Nashik%20APMC",
            "mandibhav://price?commodity=Onion",
            "mandibhav://price",
            "mandibhav://price?market=&commodity=Onion",
            "mandibhav://price?market=%20%20&commodity=Onion",
            "mandibhav://price?market=Nashik%0AAPMC&commodity=Onion",
            "mandibhav://price?market=%20Nashik&commodity=Onion",
            "mandibhav://price?market=${"A".repeat(81)}&commodity=Onion",
            "mandibhav://price?market=%zz&commodity=Onion",
        )
        for (link in bad) assertNull(link, parseDeepLink(link))
    }

    @Test
    fun aNameNoMarketHasStillParses() {
        // It is well formed; the screen it opens says the price is not available.
        assertEquals(LatestPrice("Atlantis APMC", "Onion"), parseDeepLink("mandibhav://price?market=Atlantis%20APMC&commodity=Onion"))
    }
}
