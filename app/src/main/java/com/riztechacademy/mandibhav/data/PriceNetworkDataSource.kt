package com.riztechacademy.mandibhav.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.riztechacademy.mandibhav.model.CommodityPrice
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.delay

/** Where the latest prices come from. Module 9 makes this a real server. */
interface PriceNetworkDataSource {
    /** Today's prices as the server has them now. Throws [IOException] without a connection. */
    suspend fun fetchLatestPrices(): List<CommodityPrice>
}

/**
 * Stands in for the network until module 9: after a one-second pause it
 * returns the 4 pm closing prices from [SamplePrices]. It fails, as a real
 * request would, when the phone has no connection, so the app can be tested
 * in airplane mode before there is a server.
 */
class DemoPriceNetworkDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : PriceNetworkDataSource {

    override suspend fun fetchLatestPrices(): List<CommodityPrice> {
        delay(1_000)
        if (!isConnected()) throw IOException("No network connection")
        return SamplePrices.latest
    }

    private fun isConnected(): Boolean {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
