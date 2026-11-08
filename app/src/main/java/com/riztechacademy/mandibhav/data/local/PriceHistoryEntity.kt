package com.riztechacademy.mandibhav.data.local

import androidx.room3.Entity
import com.riztechacademy.mandibhav.model.DayPrice
import java.time.LocalDate

/**
 * One row of `price_history`: the modal price of one commodity at one market
 * on an earlier day. The key is the three columns together, because that is
 * what makes a row unique, and it is also what the week query searches by.
 */
@Entity(tableName = "price_history", primaryKeys = ["market", "commodity", "date"])
data class PriceHistoryEntity(
    val market: String,
    val commodity: String,
    val date: LocalDate,
    val modalPrice: Long,
)

fun PriceHistoryEntity.asDayPrice() = DayPrice(date = date, modalPrice = modalPrice)
