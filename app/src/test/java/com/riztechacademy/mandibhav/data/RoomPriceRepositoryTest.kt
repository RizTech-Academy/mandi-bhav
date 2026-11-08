package com.riztechacademy.mandibhav.data

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.riztechacademy.mandibhav.data.local.MandiBhavDatabase
import com.riztechacademy.mandibhav.data.local.MarketEntity
import com.riztechacademy.mandibhav.data.local.PriceHistoryEntity
import com.riztechacademy.mandibhav.data.local.asEntity
import com.riztechacademy.mandibhav.model.CommodityPrice
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** A network that returns whatever the test says, at once. */
class FakePriceNetworkDataSource(var latest: List<CommodityPrice> = SamplePrices.latest) : PriceNetworkDataSource {
    var calls = 0
        private set
    var historyCalls = 0
        private set

    /** What `history.json` holds. */
    var history: List<PastPrice> = emptyList()

    /** Failures for the next fetches of the latest prices, in order; then success. */
    val failures = ArrayDeque<RefreshFailure>()

    /** Set to fail the next fetches as a phone in airplane mode would. */
    var offline = false

    /** Set to make each fetch take this long, in virtual time. */
    var delayMillis = 0L

    override suspend fun fetchLatestPrices(): LatestPrices {
        calls++
        if (delayMillis > 0) delay(delayMillis)
        if (offline) throw RefreshException(RefreshFailure.NoConnection)
        failures.removeFirstOrNull()?.let { throw RefreshException(it) }
        return LatestPrices(markets = SamplePrices.districts, prices = latest)
    }

    override suspend fun fetchHistory(): List<PastPrice> {
        historyCalls++
        return history
    }
}

/**
 * The real repository over a real in-memory database. Only the network is
 * fake, so these check the SQL, the mapping and the refresh together.
 */
@RunWith(AndroidJUnit4::class)
class RoomPriceRepositoryTest {

    private val database = Room.inMemoryDatabaseBuilder<MandiBhavDatabase>(
        ApplicationProvider.getApplicationContext<Context>(),
    ).setDriver(AndroidSQLiteDriver()).build()

    private val network = FakePriceNetworkDataSource()
    /** 4:05 pm in Pune on 7 November 2026. */
    private val fourOFive = Instant.parse("2026-11-07T10:35:00Z")
    private val clock = Clock.fixed(fourOFive, ZoneId.of("Asia/Kolkata"))

    /** One test scope per test; its virtual clock makes the retry waits instant. */
    private val testScope = TestScope()

    /**
     * Stands in for the app's scope, built the same way: a SupervisorJob, so
     * a failed refresh is reported to whoever awaits it and does not fail
     * the scope, on the test's virtual clock.
     */
    private val appScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScope.testScheduler))
    private val repository = RoomPriceRepository(database.priceDao(), network, clock, appScope)

    private fun runTest(body: suspend TestScope.() -> Unit) = testScope.runTest { body() }

    @Before
    fun seedTheMorningPrices() = kotlinx.coroutines.test.runTest {
        val dao = database.priceDao()
        dao.insertMarketsIfMissing(SamplePrices.districts.map { (name, district) -> MarketEntity(name, district) })
        dao.upsertPrices(SamplePrices.all.map { it.asEntity() })
        dao.upsertHistory(
            SamplePrices.all.flatMap { price ->
                SamplePrices.weekOf(price).dropLast(1).map {
                    PriceHistoryEntity(price.market, price.commodity, it.date, it.modalPrice)
                }
            },
        )
    }

    @After
    fun closeDatabase() {
        appScope.cancel()
        database.close()
    }

    @Test
    fun findsAPriceByItsId() = runTest {
        assertEquals(SamplePrices.nashikTomato, repository.price(SamplePrices.nashikTomato.id).first())
    }

    @Test
    fun anUnknownIdIsNullNotACrash() = runTest {
        assertNull(repository.price("no-such-price").first())
        assertTrue(repository.week("no-such-price").first().isEmpty())
    }

    @Test
    fun weekIsSevenDaysOldestFirstEndingOnThePrice() = runTest {
        val price = SamplePrices.lasalgaonOnion
        val week = repository.week(price.id).first()

        assertEquals(7, week.size)
        assertEquals(LocalDate.of(2026, 11, 1), week.first().date)
        assertEquals(price.date, week.last().date)
        assertEquals(SamplePrices.weekOf(price), week)
    }

    @Test
    fun aMarketComesWithItsDistrictAndOnlyItsOwnPrices() = runTest {
        val pune = repository.market("Pune (Gultekdi) APMC").first()!!

        assertEquals("Pune", pune.district)
        assertEquals(listOf(SamplePrices.puneOnion, SamplePrices.puneTomato, SamplePrices.puneTur), pune.prices)
        assertNull(repository.market("No such APMC").first())
    }

    @Test
    fun everyMarketIsListedByNameWithItsPrices() = runTest {
        val markets = repository.markets().first()

        assertEquals(
            listOf("Lasalgaon APMC", "Nashik APMC", "Pune (Gultekdi) APMC", "Vashi APMC, Navi Mumbai"),
            markets.map { it.name },
        )
        assertEquals(SamplePrices.all.size, markets.sumOf { it.prices.size })
    }

    @Test
    fun aPriceFromANewMarketStillReachesTheMarketsTab() = runTest {
        // The relation starts from the markets table, so a price whose
        // market has no row would be invisible there. The refresh adds one.
        val solapur = SamplePrices.puneOnion.copy(id = "solapur-onion-2026-11-07", market = "Solapur APMC")
        network.latest = SamplePrices.latest + solapur

        repository.refresh()

        val market = repository.market("Solapur APMC").first()!!
        assertEquals("", market.district)
        assertEquals(listOf(solapur), market.prices)
    }

    @Test
    fun refreshWritesTheClosingPricesOnce() = runTest {
        assertEquals(3, repository.refresh())
        assertEquals(2_525L, repository.price(SamplePrices.lasalgaonOnion.id).first()!!.modalPrice)
        assertEquals(SamplePrices.all.size, repository.todaysPrices().first().size)

        assertEquals(0, repository.refresh())
        assertEquals(2, network.calls)
    }

    @Test
    fun afterARefreshTheWeekEndsOnTheNewPrice() = runTest {
        repository.refresh()

        val week = repository.week(SamplePrices.lasalgaonOnion.id).first()
        assertEquals(2_525L, week.last().modalPrice)
        assertEquals(2_420L, week[week.size - 2].modalPrice)
    }

    @Test
    fun aSecondRepositoryOnTheSameDatabaseSeesTheRefresh() = runTest {
        // What process death looks like to the data: a brand-new repository,
        // the same database. Memory is gone; the rows are not.
        repository.refresh()

        val afterRestart = RoomPriceRepository(database.priceDao(), FakePriceNetworkDataSource(), clock, appScope)
        assertEquals(2_525L, afterRestart.price(SamplePrices.lasalgaonOnion.id).first()!!.modalPrice)
    }

    @Test
    fun aRefreshRecordsWhenThePricesWereFetched() = runTest {
        assertNull(repository.lastUpdated().first())

        repository.refresh()

        assertEquals(fourOFive, repository.lastUpdated().first())
    }

    @Test
    fun aFailedRefreshLeavesTheSavedPricesAndTheTimeAlone() = runTest {
        repository.refresh()
        network.offline = true
        network.latest = SamplePrices.all.map { it.copy(modalPrice = 1) }

        val failure = runCatching { repository.refresh() }.exceptionOrNull()

        assertEquals(RefreshFailure.NoConnection, (failure as RefreshException).failure)
        assertEquals(2_525L, repository.price(SamplePrices.lasalgaonOnion.id).first()!!.modalPrice)
        assertEquals(fourOFive, repository.lastUpdated().first())
    }

    @Test
    fun aRefreshThatChangesNoPriceStillCountsNone() = runTest {
        // Every row gets a new fetchedAt; only a different price is a change.
        repository.refresh()
        val later = RoomPriceRepository(
            database.priceDao(), network, Clock.offset(clock, java.time.Duration.ofHours(1)), appScope,
        )

        assertEquals(0, later.refresh())
        assertEquals(fourOFive.plusSeconds(3_600), later.lastUpdated().first())
    }

    // A new day, 8 November: every price has a new id, because the id carries the date.
    private val nextDay = SamplePrices.latest.map {
        it.copy(id = it.id.replace("2026-11-07", "2026-11-08"), date = it.date.plusDays(1), modalPrice = it.modalPrice + 50)
    }

    @Test
    fun aNewDaysPriceReplacesYesterdaysAndMovesItIntoTheHistory() = runTest {
        repository.refresh() // 7 November's closing prices: Lasalgaon onion 2,525
        network.latest = nextDay

        repository.refresh()

        // One onion at Lasalgaon, not two: yesterday's row is gone.
        val prices = repository.todaysPrices().first()
        assertEquals(SamplePrices.all.size, prices.size)
        assertTrue(prices.all { it.date == LocalDate.of(2026, 11, 8) })
        assertNull(repository.price(SamplePrices.lasalgaonOnion.id).first())

        // ...and its 2,525 is the sixth day of the new week.
        val week = repository.week("lasalgaon-onion-2026-11-08").first()
        assertEquals(7, week.size)
        assertEquals(2_525L, week[5].modalPrice)
        assertEquals(LocalDate.of(2026, 11, 7), week[5].date)
        assertEquals(2_575L, week[6].modalPrice)
    }

    @Test
    fun theHistoryIsDownloadedOnlyWhenThePhoneHasAGap() = runTest {
        repository.refresh() // 7 November over 7 November: no gap
        assertEquals(0, network.historyCalls)

        network.latest = nextDay
        repository.refresh() // 8 November over 7 November: the move fills the gap
        assertEquals(0, network.historyCalls)

        network.latest = nextDay.map { it.copy(id = it.id.replace("11-08", "11-11"), date = it.date.plusDays(3)) }
        repository.refresh() // 11 November over 8 November: three days missing
        assertEquals(1, network.historyCalls)
    }

    @Test
    fun anEmptyPhoneGetsTheHistoryWithItsFirstDownload() = runTest {
        database.clearAllTables()
        network.history = listOf(PastPrice("Lasalgaon APMC", "Onion", LocalDate.of(2026, 11, 6), 2_420))

        repository.refresh()

        assertEquals(1, network.historyCalls)
        val week = repository.week(SamplePrices.lasalgaonOnion.id).first()
        assertEquals(listOf(2_420L, 2_525L), week.map { it.modalPrice })
    }

    @Test
    fun aServerErrorIsRetriedAndThenSucceeds() = runTest {
        network.failures += listOf(RefreshFailure.Server(503), RefreshFailure.Server(503))

        assertEquals(3, repository.refresh())
        assertEquals(3, network.calls)
        // Waited 1 s and then 2 s, in virtual time.
        assertEquals(3_000L, testScheduler.currentTime)
    }

    @Test
    fun aNotFoundIsNotRetried() = runTest {
        network.failures += RefreshFailure.Server(404)

        val failure = runCatching { repository.refresh() }.exceptionOrNull() as RefreshException

        assertEquals(RefreshFailure.Server(404), failure.failure)
        assertEquals(1, network.calls)
    }

    @Test
    fun afterThreeTriesTheLastFailureIsReported() = runTest {
        network.failures += List(5) { RefreshFailure.Unreachable }

        val failure = runCatching { repository.refresh() }.exceptionOrNull() as RefreshException

        assertEquals(RefreshFailure.Unreachable, failure.failure)
        assertEquals(3, network.calls)
    }

    @Test
    fun twoRefreshesAtOnceShareOneDownload() = runTest {
        // A tap on Refresh while the reconnect sync is already downloading.
        network.delayMillis = 2_000
        val first = async { repository.refresh() }
        val second = async { repository.refresh() }

        assertEquals(3, first.await())
        assertEquals(3, second.await())
        assertEquals(1, network.calls)
    }

    @Test
    fun aDownloadFinishesEvenIfTheScreenThatAskedIsGone() = runTest {
        // The screen's coroutine is cancelled half way through the download.
        network.delayMillis = 2_000
        val screen = launch { repository.refresh() }
        advanceTimeBy(1_000)
        screen.cancel()

        assertTrue(screen.isCancelled)

        // Nobody is waiting any more, and the prices are still saved.
        assertEquals(fourOFive, repository.lastUpdated().first { it != null })
        assertEquals(2_525L, repository.price(SamplePrices.lasalgaonOnion.id).first()!!.modalPrice)
        assertEquals(1, network.calls)
    }
}
