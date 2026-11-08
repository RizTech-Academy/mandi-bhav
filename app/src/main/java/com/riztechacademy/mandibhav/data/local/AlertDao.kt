package com.riztechacademy.mandibhav.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

/** The questions the app asks the `price_alerts` table. */
@Dao
interface AlertDao {

    @Query("SELECT * FROM price_alerts ORDER BY commodity, market, level")
    fun observeAll(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM price_alerts")
    suspend fun getAll(): List<AlertEntity>

    /** Saves a new alert and returns the id Room gave it. */
    @Insert
    suspend fun insert(alert: AlertEntity): Long

    @Query("DELETE FROM price_alerts WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE price_alerts SET isCrossed = :crossed WHERE id = :id")
    suspend fun setCrossed(id: Long, crossed: Boolean)

    /** Saves every changed crossed state at once, so a check is never half recorded. */
    @Transaction
    suspend fun setCrossed(changes: Map<Long, Boolean>) {
        for ((id, crossed) in changes) setCrossed(id, crossed)
    }
}
