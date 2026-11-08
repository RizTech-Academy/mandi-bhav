package com.riztechacademy.mandibhav.data.local

import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteConnection
import com.riztechacademy.mandibhav.data.SamplePrices

/**
 * Fills a brand-new database with the morning's sample prices and their
 * six days of history, so the first launch has something to show. It runs
 * once, when Room creates the file, inside the same transaction.
 *
 * The seed stands in for a first download; module 9 replaces it with one.
 */
object SeedDatabase : RoomDatabase.Callback() {

    override suspend fun onCreate(connection: SQLiteConnection) {
        connection.prepare("INSERT INTO markets (name, district) VALUES (?, ?)").use { statement ->
            for ((market, district) in SamplePrices.districts) {
                statement.bindText(1, market)
                statement.bindText(2, district)
                statement.step()
                statement.reset()
            }
        }
        connection.prepare(
            "INSERT INTO prices (id, commodity, market, date, minPrice, maxPrice, modalPrice) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)"
        ).use { statement ->
            for (price in SamplePrices.all) {
                statement.bindText(1, price.id)
                statement.bindText(2, price.commodity)
                statement.bindText(3, price.market)
                statement.bindLong(4, price.date.toEpochDay())
                statement.bindLong(5, price.minPrice)
                statement.bindLong(6, price.maxPrice)
                statement.bindLong(7, price.modalPrice)
                statement.step()
                statement.reset()
            }
        }
        connection.prepare(
            "INSERT INTO price_history (market, commodity, date, modalPrice) VALUES (?, ?, ?, ?)"
        ).use { statement ->
            for (price in SamplePrices.all) {
                // The week's last day is today's price itself, so the
                // history holds the six days before it.
                for (day in SamplePrices.weekOf(price).dropLast(1)) {
                    statement.bindText(1, price.market)
                    statement.bindText(2, price.commodity)
                    statement.bindLong(3, day.date.toEpochDay())
                    statement.bindLong(4, day.modalPrice)
                    statement.step()
                    statement.reset()
                }
            }
        }
    }
}
