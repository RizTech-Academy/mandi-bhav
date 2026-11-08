package com.riztechacademy.mandibhav.data.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.riztechacademy.mandibhav.model.CommodityPrice
import java.time.Instant
import java.time.LocalDate

/**
 * One row of the `prices` table: today's price for one commodity at one
 * market. It mirrors [CommodityPrice], but belongs to the data layer, so the
 * model the screens use carries no database annotations.
 */
@Entity(tableName = "prices")
data class PriceEntity(
    @PrimaryKey val id: String,
    val commodity: String,
    val market: String,
    val date: LocalDate,
    val minPrice: Long,
    val maxPrice: Long,
    val modalPrice: Long,
    /** When this row was last written by a refresh; null for the seed data. */
    val fetchedAt: Instant? = null,
    /** The commodity's pictures, small and large, as full URLs; null if none. Added in version 4. */
    val thumbnailUrl: String? = null,
    val photoUrl: String? = null,
)

fun PriceEntity.asModel() = CommodityPrice(
    id = id,
    commodity = commodity,
    market = market,
    date = date,
    minPrice = minPrice,
    maxPrice = maxPrice,
    modalPrice = modalPrice,
    thumbnailUrl = thumbnailUrl,
    photoUrl = photoUrl,
)

fun CommodityPrice.asEntity(fetchedAt: Instant? = null) = PriceEntity(
    id = id,
    commodity = commodity,
    market = market,
    date = date,
    minPrice = minPrice,
    maxPrice = maxPrice,
    modalPrice = modalPrice,
    fetchedAt = fetchedAt,
    thumbnailUrl = thumbnailUrl,
    photoUrl = photoUrl,
)
