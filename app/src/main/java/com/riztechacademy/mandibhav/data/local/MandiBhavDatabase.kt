package com.riztechacademy.mandibhav.data.local

import androidx.room3.AutoMigration
import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase

/**
 * The app's database: which tables it has, at which schema version, and the
 * DAOs that reach them. Room generates the implementation when the app builds.
 */
@Database(
    entities = [PriceEntity::class, PriceHistoryEntity::class, MarketEntity::class, AlertEntity::class],
    version = 5,
    autoMigrations = [
        // 2 to 3 adds one nullable column, prices.fetchedAt: Room can work
        // that out from the two exported schemas.
        AutoMigration(from = 2, to = 3),
        // 3 to 4 adds two more, the commodity's picture URLs (module 9).
        AutoMigration(from = 3, to = 4),
        // 4 to 5 adds a table, price_alerts (module 10). A new table is
        // something Room can always work out for itself.
        AutoMigration(from = 4, to = 5),
    ],
)
@ColumnTypeConverters(Converters::class)
abstract class MandiBhavDatabase : RoomDatabase() {
    abstract fun priceDao(): PriceDao
    abstract fun alertDao(): AlertDao
}
