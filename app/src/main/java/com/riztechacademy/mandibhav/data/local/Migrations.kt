package com.riztechacademy.mandibhav.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Version 1 to 2: markets get a table of their own, with the district each
 * is in. Every market that already has prices gets a row, so a farmer who
 * updates the app keeps everything the old version saved.
 *
 * Written out in full SQL, never built from the app's constants: this code
 * must do exactly what it did on the day version 2 shipped, for as long as
 * any phone still has a version 1 database.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `markets` (`name` TEXT NOT NULL, `district` TEXT NOT NULL, PRIMARY KEY(`name`))"
        )
        connection.execSQL("INSERT INTO markets (name, district) SELECT DISTINCT market, '' FROM prices")
        connection.execSQL(
            "UPDATE markets SET district = 'Nashik' WHERE name IN ('Lasalgaon APMC', 'Nashik APMC')"
        )
        connection.execSQL("UPDATE markets SET district = 'Pune' WHERE name = 'Pune (Gultekdi) APMC'")
        connection.execSQL("UPDATE markets SET district = 'Thane' WHERE name = 'Vashi APMC, Navi Mumbai'")
    }
}
