package com.riztechacademy.mandibhav.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Every question the app asks the prices tables, as SQL that Room checks
 * when the app is built. A function returning a [Flow] emits again whenever
 * a table it reads changes; a `suspend` function runs once.
 */
@Dao
interface PriceDao {

    @Query("SELECT * FROM prices ORDER BY commodity, modalPrice DESC")
    fun observeAll(): Flow<List<PriceEntity>>

    @Query("SELECT * FROM prices WHERE id = :id")
    fun observePrice(id: String): Flow<PriceEntity?>

    @Query(
        """
        SELECT * FROM price_history
        WHERE market = :market AND commodity = :commodity AND date BETWEEN :from AND :to
        ORDER BY date
        """
    )
    fun observeHistory(market: String, commodity: String, from: LocalDate, to: LocalDate): Flow<List<PriceHistoryEntity>>

    /** Every market, each with its prices today, by name. */
    @Transaction
    @Query("SELECT * FROM markets ORDER BY name")
    fun observeMarketsWithPrices(): Flow<List<MarketWithPrices>>

    /** One market and its prices today, or null if there is no such market. */
    @Transaction
    @Query("SELECT * FROM markets WHERE name = :name")
    fun observeMarket(name: String): Flow<MarketWithPrices?>

    /** When the newest price was fetched; null if no refresh has ever succeeded. */
    @Query("SELECT MAX(fetchedAt) FROM prices")
    fun observeLastFetched(): Flow<Instant?>

    @Query("SELECT * FROM prices")
    suspend fun getAll(): List<PriceEntity>

    /** The date of the newest price saved; null when the phone has none. */
    @Query("SELECT MAX(date) FROM prices")
    suspend fun newestDate(): LocalDate?

    /** Adds a row for each market not already in the table; leaves the rest alone. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMarketsIfMissing(markets: List<MarketEntity>)

    /** Writes each market, replacing the district of one already saved. */
    @Upsert
    suspend fun upsertMarkets(markets: List<MarketEntity>)

    /** Inserts each price, or replaces the row that already has its id. */
    @Upsert
    suspend fun upsertPrices(prices: List<PriceEntity>)

    @Upsert
    suspend fun upsertHistory(history: List<PriceHistoryEntity>)

    @Query("DELETE FROM prices WHERE id IN (:ids)")
    suspend fun deletePrices(ids: List<String>)

    /**
     * Saves one download in one transaction and returns how many prices
     * changed.
     *
     * `prices` holds one row per commodity and market: today's. When a
     * newer day's price arrives, the row it replaces has a different id
     * (the id carries the date), so upserting alone would keep both and the
     * list would show yesterday's onion beside today's. Instead the old row's
     * modal price moves into `price_history`, and the row is deleted.
     * [history] from the server is written after that, so where both have
     * a day, the server's closing price wins over what the phone last saw.
     */
    @Transaction
    suspend fun saveDownload(
        prices: List<PriceEntity>,
        markets: List<MarketEntity> = emptyList(),
        history: List<PriceHistoryEntity> = emptyList(),
    ): Int {
        val before = getAll()
        upsertMarkets(markets)
        // A price whose market has no row would never appear on the Markets
        // tab, because the relation starts from the markets table.
        insertMarketsIfMissing(prices.map { it.market }.distinct().map { MarketEntity(name = it, district = "") })

        val incoming = prices.associateBy { it.market to it.commodity }
        val replaced = before.filter { old ->
            val new = incoming[old.market to old.commodity]
            new != null && new.date > old.date
        }
        upsertHistory(replaced.map { PriceHistoryEntity(it.market, it.commodity, it.date, it.modalPrice) })
        deletePrices(replaced.map { it.id })
        upsertHistory(history)
        upsertPrices(prices)

        val beforeById = before.associateBy { it.id }
        // A price is changed if anything but the time it was fetched differs.
        return prices.count { beforeById[it.id]?.copy(fetchedAt = it.fetchedAt) != it }
    }
}
