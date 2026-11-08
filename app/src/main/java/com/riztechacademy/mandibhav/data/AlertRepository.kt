package com.riztechacademy.mandibhav.data

import com.riztechacademy.mandibhav.data.local.AlertDao
import com.riztechacademy.mandibhav.data.local.AlertEntity
import com.riztechacademy.mandibhav.data.local.PriceDao
import com.riztechacademy.mandibhav.data.local.asModel
import com.riztechacademy.mandibhav.model.AlertDirection
import com.riztechacademy.mandibhav.model.PriceAlert
import com.riztechacademy.mandibhav.model.isMetBy
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The user's price alerts. They live only on the phone: Mandi Bhav reads
 * prices from a server but sends nothing back.
 */
interface AlertRepository {

    /** Every alert, by commodity, market and level. */
    fun alerts(): Flow<List<PriceAlert>>

    /** Every alert, once. */
    suspend fun allAlerts(): List<PriceAlert>

    /**
     * Saves a new alert and returns its id. If today's saved price is already
     * past the level, the alert starts crossed: the user can see that price on
     * the screen in front of them, and does not need a notification to say so.
     */
    suspend fun add(commodity: String, market: String, direction: AlertDirection, level: Long): Long

    suspend fun delete(id: Long)

    /** Records which alerts are past their level now, by id. */
    suspend fun setCrossed(changes: Map<Long, Boolean>)
}

class RoomAlertRepository @Inject constructor(
    private val alertDao: AlertDao,
    private val priceDao: PriceDao,
    private val clock: Clock,
) : AlertRepository {

    override fun alerts(): Flow<List<PriceAlert>> =
        alertDao.observeAll().map { rows -> rows.map { it.asModel() } }

    override suspend fun allAlerts(): List<PriceAlert> = alertDao.getAll().map { it.asModel() }

    override suspend fun add(commodity: String, market: String, direction: AlertDirection, level: Long): Long {
        val today = priceDao.getAll().firstOrNull { it.commodity == commodity && it.market == market }
        val alert = PriceAlert(id = 0, commodity = commodity, market = market, direction = direction, level = level)
        return alertDao.insert(
            AlertEntity(
                commodity = commodity,
                market = market,
                direction = direction,
                level = level,
                isCrossed = today != null && alert.isMetBy(today.modalPrice),
                createdAt = clock.instant(),
            ),
        )
    }

    override suspend fun delete(id: Long) = alertDao.delete(id)

    override suspend fun setCrossed(changes: Map<Long, Boolean>) {
        if (changes.isNotEmpty()) alertDao.setCrossed(changes)
    }
}
