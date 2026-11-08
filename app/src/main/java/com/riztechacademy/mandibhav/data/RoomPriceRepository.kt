package com.riztechacademy.mandibhav.data

import com.riztechacademy.mandibhav.data.local.PriceDao
import com.riztechacademy.mandibhav.data.local.asDayPrice
import com.riztechacademy.mandibhav.data.local.asEntity
import com.riztechacademy.mandibhav.data.local.asModel
import com.riztechacademy.mandibhav.model.CommodityPrice
import com.riztechacademy.mandibhav.model.DayPrice
import com.riztechacademy.mandibhav.model.Market
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RoomPriceRepository @Inject constructor(
    private val priceDao: PriceDao,
    private val network: PriceNetworkDataSource,
    private val clock: Clock,
) : PriceRepository {

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
     * Network first, then one write. If the fetch fails, nothing has been
     * written, and every screen keeps showing what the database has.
     */
    override suspend fun refresh(): Int {
        val latest = network.fetchLatestPrices()
        val now = clock.instant()
        return priceDao.upsertAndCountChanges(latest.map { it.asEntity(fetchedAt = now) })
    }
}
