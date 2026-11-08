package com.riztechacademy.mandibhav.data.network

import retrofit2.http.GET

/**
 * The prices server's endpoints, as Retrofit sees them. Retrofit writes the
 * class that makes the requests; each `suspend` function sends one GET and
 * returns the parsed body, or throws.
 *
 * The paths have no leading slash: they are relative to the base URL,
 * which ends in `api/v1/`. With a leading slash, `/prices.json` would
 * replace the whole path and ask for the host's root instead.
 */
interface PricesApi {

    /** Today's prices at every market. */
    @GET("prices.json")
    suspend fun latestPrices(): NetworkPrices

    /** Modal prices for the days before today. Downloaded on first launch, or after a gap. */
    @GET("history.json")
    suspend fun history(): NetworkHistory
}
