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
    entities = [PriceEntity::class, PriceHistoryEntity::class, MarketEntity::class],
    version = 4,
    autoMigrations = [
        // 2 to 3 adds one nullable column, prices.fetchedAt: Room can work
        // that out from the two exported schemas.
        AutoMigration(from = 2, to = 3),
        // 3 to 4 adds two more, the commodity's picture URLs (module 9).
        AutoMigration(from = 3, to = 4),
    ],
)
@ColumnTypeConverters(Converters::class)
abstract class MandiBhavDatabase : RoomDatabase() {
    abstract fun priceDao(): PriceDao
}
