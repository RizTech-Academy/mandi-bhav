package com.riztechacademy.mandibhav.data.network

import com.riztechacademy.mandibhav.data.LatestPrices
import com.riztechacademy.mandibhav.data.NetworkMonitor
import com.riztechacademy.mandibhav.data.PastPrice
import com.riztechacademy.mandibhav.data.PriceNetworkDataSource
import com.riztechacademy.mandibhav.data.RefreshException
import com.riztechacademy.mandibhav.data.RefreshFailure
import java.io.IOException
import java.io.InterruptedIOException
import java.time.DateTimeException
import javax.inject.Inject
import kotlinx.serialization.SerializationException
import okhttp3.HttpUrl
import retrofit2.HttpException

/**
 * Prices from the server, through Retrofit. Every failure leaves as a
 * [RefreshException] saying why, in the app's terms.
 */
class RetrofitPriceNetworkDataSource @Inject constructor(
    private val api: PricesApi,
    private val baseUrl: HttpUrl,
    private val networkMonitor: NetworkMonitor,
) : PriceNetworkDataSource {

    override suspend fun fetchLatestPrices(): LatestPrices = request { api.latestPrices().asLatestPrices(baseUrl) }

    override suspend fun fetchHistory(): List<PastPrice> = request { api.history().asPastPrices() }

    /**
     * Runs one request and translates what can go wrong. The order matters:
     * a timeout is an [InterruptedIOException], which is an [IOException],
     * so it is caught first. A coroutine's cancellation is none of these
     * types and goes straight through, as it must.
     */
    private suspend fun <T> request(block: suspend () -> T): T = try {
        block()
    } catch (e: HttpException) {
        throw RefreshException(RefreshFailure.Server(e.code()), e)
    } catch (e: SerializationException) {
        throw RefreshException(RefreshFailure.BadData, e)
    } catch (e: DateTimeException) {
        throw RefreshException(RefreshFailure.BadData, e)
    } catch (e: InterruptedIOException) {
        throw RefreshException(RefreshFailure.Timeout, e)
    } catch (e: IOException) {
        // The same exception for airplane mode and for a server that is
        // down; only the phone's own view of its connection tells them apart.
        val failure = if (networkMonitor.isOnlineNow()) RefreshFailure.Unreachable else RefreshFailure.NoConnection
        throw RefreshException(failure, e)
    }
}
