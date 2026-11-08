package com.riztechacademy.mandibhav.data.local

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.riztechacademy.mandibhav.data.SamplePrices
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the DAO against a real SQLite database, in memory, on the JVM:
 * Robolectric provides the Android SQLite classes, so no emulator is needed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PriceDaoTest {

    private val database = Room.inMemoryDatabaseBuilder<MandiBhavDatabase>(
        ApplicationProvider.getApplicationContext<Context>(),
    ).setDriver(AndroidSQLiteDriver()).build()

    private val dao = database.priceDao()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun aPriceComesBackAsItWentIn() = runTest {
        dao.upsertPrices(listOf(SamplePrices.lasalgaonOnion.asEntity()))

        assertEquals(SamplePrices.lasalgaonOnion, dao.observePrice(SamplePrices.lasalgaonOnion.id).first()?.asModel())
        assertNull(dao.observePrice("no-such-price").first())
    }

    @Test
    fun upsertReplacesTheRowWithTheSameId() = runTest {
        dao.upsertPrices(SamplePrices.all.map { it.asEntity() })

        dao.upsertPrices(listOf(SamplePrices.lasalgaonOnion.copy(modalPrice = 2_525).asEntity()))

        assertEquals(SamplePrices.all.size, dao.getAll().size)
        assertEquals(2_525L, dao.observePrice(SamplePrices.lasalgaonOnion.id).first()?.modalPrice)
    }

    @Test
    fun pricesComeOutByCommodityThenHighestModalPriceFirst() = runTest {
        dao.upsertPrices(SamplePrices.all.map { it.asEntity() })

        val onion = dao.observeAll().first().filter { it.commodity == "Onion" }.map { it.market }
        assertEquals(listOf("Lasalgaon APMC", "Nashik APMC", "Pune (Gultekdi) APMC"), onion)
    }

    @Test
    fun countsOnlyThePricesThatChanged() = runTest {
        dao.upsertPrices(SamplePrices.all.map { it.asEntity() })

        assertEquals(3, dao.saveDownload(SamplePrices.latest.map { it.asEntity() }))
        assertEquals(0, dao.saveDownload(SamplePrices.latest.map { it.asEntity() }))
    }

    @Test
    fun historyIsTheDaysAskedForOldestFirst() = runTest {
        val price = SamplePrices.lasalgaonOnion
        dao.upsertHistory(
            SamplePrices.weekOf(price).map { PriceHistoryEntity(price.market, price.commodity, it.date, it.modalPrice) },
        )

        val history = dao.observeHistory(price.market, price.commodity, price.date.minusDays(6), price.date.minusDays(1)).first()

        assertEquals(6, history.size)
        assertEquals(price.date.minusDays(6), history.first().date)
        assertEquals(listOf<Long>(2_310, 2_280, 2_350, 2_400, 2_380, 2_420), history.map { it.modalPrice })
    }

    @Test
    fun anOpenQueryEmitsAgainWhenTheTableChanges() = runTest {
        dao.upsertPrices(listOf(SamplePrices.lasalgaonOnion.asEntity()))
        val seen = mutableListOf<Long?>()
        val watching = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            dao.observePrice(SamplePrices.lasalgaonOnion.id).collect { seen += it?.modalPrice }
        }
        // Room's queries run on its own threads, so wait for the first answer.
        while (seen.isEmpty()) yield()

        dao.upsertPrices(listOf(SamplePrices.lasalgaonOnion.copy(modalPrice = 2_525).asEntity()))
        while (seen.size < 2) yield()

        assertEquals(listOf<Long?>(2_450, 2_525), seen)
        watching.cancel()
    }

    @Test
    fun aQueryRunsAgainWhenAnyRowOfItsTableChanges() = runTest {
        // Room watches tables, not rows: a change to Pune onion re-runs the
        // Lasalgaon query, which answers the same thing again. This is why
        // the repository adds distinctUntilChanged.
        dao.upsertPrices(listOf(SamplePrices.lasalgaonOnion.asEntity(), SamplePrices.puneOnion.asEntity()))
        val seen = mutableListOf<Long?>()
        val watching = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            dao.observePrice(SamplePrices.lasalgaonOnion.id).collect { seen += it?.modalPrice }
        }
        while (seen.isEmpty()) yield()

        dao.upsertPrices(listOf(SamplePrices.puneOnion.copy(modalPrice = 2_250).asEntity()))
        while (seen.size < 2) yield()

        assertEquals(listOf<Long?>(2_450, 2_450), seen)
        watching.cancel()
    }

    @Test
    fun aNewDayMovesYesterdaysPriceIntoTheHistoryInsteadOfKeepingBoth() = runTest {
        dao.saveDownload(listOf(SamplePrices.lasalgaonOnion.asEntity()))
        val tomorrow = SamplePrices.lasalgaonOnion.copy(
            id = "lasalgaon-onion-2026-11-08",
            date = SamplePrices.lasalgaonOnion.date.plusDays(1),
            modalPrice = 2_580,
        )

        dao.saveDownload(listOf(tomorrow.asEntity()))

        assertEquals(listOf(tomorrow.id), dao.getAll().map { it.id })
        val history = dao.observeHistory(
            market = "Lasalgaon APMC",
            commodity = "Onion",
            from = SamplePrices.lasalgaonOnion.date,
            to = SamplePrices.lasalgaonOnion.date,
        ).first()
        assertEquals(listOf(2_450L), history.map { it.modalPrice })
    }

    @Test
    fun theServersHistoryWinsOverThePriceThePhoneLastSaw() = runTest {
        // The phone saved the morning's 2,450; the day closed at 2,525.
        dao.saveDownload(listOf(SamplePrices.lasalgaonOnion.asEntity()))
        val tomorrow = SamplePrices.lasalgaonOnion.copy(id = "lasalgaon-onion-2026-11-08", date = SamplePrices.lasalgaonOnion.date.plusDays(1))
        val closing = PriceHistoryEntity("Lasalgaon APMC", "Onion", SamplePrices.lasalgaonOnion.date, 2_525)

        dao.saveDownload(listOf(tomorrow.asEntity()), history = listOf(closing))

        val day = SamplePrices.lasalgaonOnion.date
        assertEquals(listOf(closing), dao.observeHistory("Lasalgaon APMC", "Onion", day, day).first())
    }
}
