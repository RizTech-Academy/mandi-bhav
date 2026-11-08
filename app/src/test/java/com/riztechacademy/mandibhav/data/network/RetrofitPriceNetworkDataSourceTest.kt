package com.riztechacademy.mandibhav.data.network

import com.riztechacademy.mandibhav.data.FakeNetworkMonitor
import com.riztechacademy.mandibhav.data.RefreshException
import com.riztechacademy.mandibhav.data.RefreshFailure
import com.riztechacademy.mandibhav.di.NetworkModule
import com.riztechacademy.mandibhav.di.pricesHttpClient
import java.io.File
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.time.measureTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The real Retrofit, OkHttp and JSON parsing, against MockWebServer: a
 * real HTTP server on this computer that answers with whatever each test
 * queues, including the failures a real server rarely produces on demand.
 */
class RetrofitPriceNetworkDataSourceTest {

    private val server = MockWebServer().apply { start() }
    private val network = FakeNetworkMonitor(online = true)

    /** Shorter limits than the app's, so the timeout tests take a second, not twenty. */
    private val client = pricesHttpClient(
        connectTimeoutMillis = 1_000,
        readTimeoutMillis = 500,
        callTimeoutMillis = 1_500,
    )
    private val baseUrl = server.url("/api/v1/")
    private val api = NetworkModule.provideRetrofit(baseUrl, { client }, NetworkModule.provideJson())
        .create(PricesApi::class.java)
    private val source = RetrofitPriceNetworkDataSource(api, baseUrl, network)

    /** The JSON committed in this repository's api/ folder: what the release app downloads. */
    private fun committed(name: String) = File("../api/v1/$name").readText()

    @After
    fun stopServer() {
        server.close()
    }

    private fun respond(code: Int = 200, body: String) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    private suspend fun failureOf(block: suspend () -> Unit): RefreshFailure =
        try {
            block()
            throw AssertionError("expected a RefreshException")
        } catch (e: RefreshException) {
            e.failure
        }

    @Test
    fun theCommittedPricesParseAndThePicturesResolveAgainstTheBaseUrl() = runTest {
        respond(body = committed("prices.json"))

        val latest = source.fetchLatestPrices()

        assertEquals("/api/v1/prices.json", server.takeRequest().url.encodedPath)
        assertEquals(9, latest.prices.size)
        assertEquals("Nashik", latest.markets["Lasalgaon APMC"])
        val onion = latest.prices.first { it.id == "lasalgaon-onion-2026-11-08" }
        assertEquals(LocalDate.of(2026, 11, 8), onion.date)
        assertEquals(2_580L, onion.modalPrice)
        assertEquals(server.url("/api/v1/images/thumb/onion.jpg").toString(), onion.thumbnailUrl)
    }

    @Test
    fun theCommittedHistoryParses() = runTest {
        respond(body = committed("history.json"))

        val history = source.fetchHistory()

        assertEquals(63, history.size)
        assertEquals(LocalDate.of(2026, 11, 1), history.minOf { it.date })
    }

    @Test
    fun aFieldTheAppDoesNotKnowIsIgnored() = runTest {
        respond(body = committed("prices.json").replace("\"modal_price\"", "\"arrivals_tonnes\": 120, \"modal_price\""))

        assertEquals(9, source.fetchLatestPrices().prices.size)
    }

    @Test
    fun aServerErrorKeepsItsCode() = runTest {
        respond(code = 503, body = "Service Unavailable")

        assertEquals(RefreshFailure.Server(503), failureOf { source.fetchLatestPrices() })
    }

    @Test
    fun aMissingFileIsA404() = runTest {
        respond(code = 404, body = "404: Not Found")

        assertEquals(RefreshFailure.Server(404), failureOf { source.fetchLatestPrices() })
    }

    @Test
    fun halfAFileIsBadData() = runTest {
        // A connection that dropped mid-file, behind a proxy that still said 200.
        respond(body = committed("prices.json").take(600))

        assertEquals(RefreshFailure.BadData, failureOf { source.fetchLatestPrices() })
    }

    @Test
    fun aMissingFieldIsBadData() = runTest {
        respond(body = committed("prices.json").replace("\"modal_price\"", "\"modal\""))

        assertEquals(RefreshFailure.BadData, failureOf { source.fetchLatestPrices() })
    }

    @Test
    fun aDateInAnotherFormatIsBadData() = runTest {
        respond(body = committed("prices.json").replace("\"date\": \"2026-11-08\"", "\"date\": \"08/11/2026\""))

        assertEquals(RefreshFailure.BadData, failureOf { source.fetchLatestPrices() })
    }

    @Test
    fun aServerThatNeverAnswersTimesOut() = runTest {
        server.enqueue(MockResponse.Builder().body(committed("prices.json")).headersDelay(3, TimeUnit.SECONDS).build())

        assertEquals(RefreshFailure.Timeout, failureOf { source.fetchLatestPrices() })
    }

    @Test
    fun aTrickleThatNeverTripsTheReadTimeoutStillHitsTheCallTimeout() = runTest {
        // 100 bytes every 400 ms: each read is inside the 500 ms read timeout,
        // and the whole file would take about 14 s. Only the call timeout stops it.
        server.enqueue(
            MockResponse.Builder().body(committed("prices.json")).throttleBody(100, 400, TimeUnit.MILLISECONDS).build(),
        )

        var failure: RefreshFailure? = null
        val took = withContext(Dispatchers.Default) {
            measureTime { failure = failureOf { source.fetchLatestPrices() } }
        }

        assertEquals(RefreshFailure.Timeout, failure)
        assertTrue("took $took", took.inWholeMilliseconds in 1_400..3_000)
    }

    @Test
    fun aServerThatIsDownIsUnreachableWhileThePhoneIsOnline() = runTest {
        server.close()

        assertEquals(RefreshFailure.Unreachable, failureOf { source.fetchLatestPrices() })
    }

    @Test
    fun theSameFailureWithThePhoneOfflineIsNoConnection() = runTest {
        server.close()
        network.online = false

        assertEquals(RefreshFailure.NoConnection, failureOf { source.fetchLatestPrices() })
    }

    @Test
    fun cancellingTheCallerIsNotAFailure() = runTest {
        server.enqueue(MockResponse.Builder().body(committed("prices.json")).headersDelay(1, TimeUnit.SECONDS).build())

        val call = async(Dispatchers.Default) { source.fetchLatestPrices() }
        withContext(Dispatchers.Default) { delay(200) }
        call.cancel()

        val thrown = runCatching { call.await() }.exceptionOrNull()
        assertTrue("got $thrown", thrown is CancellationException)
    }
}
