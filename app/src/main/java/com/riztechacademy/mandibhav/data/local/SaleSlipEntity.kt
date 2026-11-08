package com.riztechacademy.mandibhav.data.local

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Query
import androidx.room3.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * One row of `sale_slips` (schema 6): the photo of the last sale slip for
 * one commodity at one market. The row holds the file's name, not the
 * picture: the picture is a file in the app's private storage.
 */
@Entity(tableName = "sale_slips", primaryKeys = ["market", "commodity"])
data class SaleSlipEntity(
    val market: String,
    val commodity: String,
    val date: LocalDate,
    val fileName: String,
)

@Dao
interface SaleSlipDao {

    @Query("SELECT * FROM sale_slips WHERE market = :market AND commodity = :commodity")
    fun observe(market: String, commodity: String): Flow<SaleSlipEntity?>

    @Query("SELECT * FROM sale_slips WHERE market = :market AND commodity = :commodity")
    suspend fun get(market: String, commodity: String): SaleSlipEntity?

    @Query("SELECT fileName FROM sale_slips")
    suspend fun allFileNames(): List<String>

    @Upsert
    suspend fun upsert(slip: SaleSlipEntity)

    @Query("DELETE FROM sale_slips WHERE market = :market AND commodity = :commodity")
    suspend fun delete(market: String, commodity: String)
}
