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
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
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

    /** Set to fail the next fetches as a phone in airplane mode would. */
    var offline = false

    override suspend fun fetchLatestPrices(): List<CommodityPrice> {
        calls++
        if (offline) throw IOException("No network connection")
        return latest
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
    private val repository = RoomPriceRepository(database.priceDao(), network, clock)

    @Before
    fun seedTheMorningPrices() = runTest {
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

        val afterRestart = RoomPriceRepository(database.priceDao(), FakePriceNetworkDataSource(), clock)
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

        assertTrue(failure is IOException)
        assertEquals(2_525L, repository.price(SamplePrices.lasalgaonOnion.id).first()!!.modalPrice)
        assertEquals(fourOFive, repository.lastUpdated().first())
    }

    @Test
    fun aRefreshThatChangesNoPriceStillCountsNone() = runTest {
        // Every row gets a new fetchedAt; only a different price is a change.
        repository.refresh()
        val later = RoomPriceRepository(database.priceDao(), network, Clock.offset(clock, java.time.Duration.ofHours(1)))

        assertEquals(0, later.refresh())
        assertEquals(fourOFive.plusSeconds(3_600), later.lastUpdated().first())
    }
}
