package com.riztechacademy.mandibhav.data

import com.riztechacademy.mandibhav.data.local.MarketEntity
import com.riztechacademy.mandibhav.data.local.PriceDao
import com.riztechacademy.mandibhav.data.local.PriceHistoryEntity
import com.riztechacademy.mandibhav.data.local.asDayPrice
import com.riztechacademy.mandibhav.data.local.asEntity
import com.riztechacademy.mandibhav.data.local.asModel
import com.riztechacademy.mandibhav.model.CommodityPrice
import com.riztechacademy.mandibhav.model.DayPrice
import com.riztechacademy.mandibhav.di.ApplicationScope
import com.riztechacademy.mandibhav.model.Market
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Prices from the on-device database. The database is the single source of
 * truth: every answer is a Room query, and a refresh writes to the database
 * rather than to the screens, so whatever is watching sees the new prices.
 * They survive the process being killed and the phone being restarted, and
 * a failed refresh leaves them exactly as they were.
 *
 * A refresh runs in [externalScope], which lives as long as the app, not
 * in the caller's scope: if the screen that asked goes away, its wait is
 * cancelled, but the download it started still finishes and is saved.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RoomPriceRepository @Inject constructor(
    private val priceDao: PriceDao,
    private val network: PriceNetworkDataSource,
    private val clock: Clock,
    @ApplicationScope private val externalScope: CoroutineScope,
) : PriceRepository {

    /** The refresh that is running now, if any; a second caller waits for it. */
    private var running: Deferred<Int>? = null

    override fun todaysPrices(): Flow<List<CommodityPrice>> =
        priceDao.observeAll().map { rows -> rows.map { it.asModel() } }.distinctUntilChanged()

    override fun price(id: String): Flow<CommodityPrice?> =
        priceDao.observePrice(id).map { it?.asModel() }.distinctUntilChanged()

    override fun markets(): Flow<List<Market>> =
        priceDao.observeMarketsWithPrices().map { rows -> rows.map { it.asModel() } }.distinctUntilChanged()

    override fun market(name: String): Flow<Market?> =
        priceDao.observeMarket(name).map { it?.asModel() }.distinctUntilChanged()

    override fun week(id: String): Flow<List<DayPrice>> =
        price(id).flatMapLatest { price ->
            if (price == null) {
                flowOf(emptyList())
            } else {
                priceDao.observeHistory(
                    market = price.market,
                    commodity = price.commodity,
                    from = price.date.minusDays(6),
                    to = price.date.minusDays(1),
                ).map { history ->
                    // No history, no week; otherwise the six days before,
                    // then today's price as the seventh.
                    if (history.isEmpty()) emptyList()
                    else history.map { it.asDayPrice() } + DayPrice(price.date, price.modalPrice)
                }
            }
        }.distinctUntilChanged()

    override fun lastUpdated(): Flow<Instant?> = priceDao.observeLastFetched().distinctUntilChanged()

    /**
     * Fetches the latest prices and saves them, returning how many changed.
     * Network first, then one write: if the fetch fails, nothing has been
     * written, and every screen keeps showing what the database has.
     *
     * Only one refresh runs at a time across the app: a second call while
     * one is running waits for the same result instead of downloading again.
     * Throws [RefreshException]; transient failures are retried first.
     */
    override suspend fun refresh(): Int {
        val download = synchronized(this) {
            running?.takeIf { it.isActive } ?: externalScope.async { download() }.also { running = it }
        }
        return download.await()
    }

    private suspend fun download(): Int {
        val latest = retryingTransientFailures { network.fetchLatestPrices() }
        // History is downloaded when the phone has none to build a week from:
        // on first launch, or when the last saved day is more than a day
        // before today's. Otherwise each new day's arrival moves the day
        // before into the history, in saveDownload.
        val newestSaved = priceDao.newestDate()
        val newestIncoming = latest.prices.maxOfOrNull { it.date }
        val history = if (newestIncoming != null && (newestSaved == null || newestSaved < newestIncoming.minusDays(1))) {
            retryingTransientFailures { network.fetchHistory() }
        } else {
            emptyList()
        }
        val now = clock.instant()
        return priceDao.saveDownload(
            prices = latest.prices.map { it.asEntity(fetchedAt = now) },
            markets = latest.markets.map { (name, district) -> MarketEntity(name, district) },
            history = history.map { PriceHistoryEntity(it.market, it.commodity, it.date, it.modalPrice) },
        )
    }
}
