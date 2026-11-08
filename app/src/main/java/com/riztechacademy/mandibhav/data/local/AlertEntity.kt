package com.riztechacademy.mandibhav.data.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.riztechacademy.mandibhav.model.AlertDirection
import com.riztechacademy.mandibhav.model.PriceAlert
import java.time.Instant

/**
 * One row of the `price_alerts` table, added in version 5. The user's own
 * data, unlike the prices: nothing on a server can bring it back, so a
 * migration must never lose it.
 */
@Entity(tableName = "price_alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val commodity: String,
    val market: String,
    /** Saved as its name, "Above" or "Below". */
    val direction: AlertDirection,
    val level: Long,
    /** Whether the last price seen was past the level; see [PriceAlert.isCrossed]. */
    val isCrossed: Boolean,
    val createdAt: Instant,
)

fun AlertEntity.asModel() = PriceAlert(
    id = id,
    commodity = commodity,
    market = market,
    direction = direction,
    level = level,
    isCrossed = isCrossed,
)
