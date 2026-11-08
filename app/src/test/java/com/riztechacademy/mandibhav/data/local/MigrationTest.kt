package com.riztechacademy.mandibhav.data.local

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Builds a database exactly as an old version of the app left it, from the
 * schema JSON exported at the time, runs the migrations, and checks both
 * the new schema and that nothing the user had saved was lost.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = instrumentation,
        databaseClass = MandiBhavDatabase::class,
        driver = AndroidSQLiteDriver(),
        file = instrumentation.targetContext.getDatabasePath("migration-test.db"),
    )

    @Test
    fun version1To2KeepsThePricesAndGivesEachMarketItsDistrict() = runTest {
        // Version 1, with the 4 pm Lasalgaon price a farmer refreshed before updating.
        helper.createDatabase(1).apply {
            execSQL(
                "INSERT INTO prices (id, commodity, market, date, minPrice, maxPrice, modalPrice) " +
                    "VALUES ('lasalgaon-onion-2026-11-07', 'Onion', 'Lasalgaon APMC', 20399, 1200, 2900, 2525)"
            )
            execSQL(
                "INSERT INTO prices (id, commodity, market, date, minPrice, maxPrice, modalPrice) " +
                    "VALUES ('vashi-wheat-2026-11-07', 'Wheat', 'Vashi APMC, Navi Mumbai', 20399, 2700, 3300, 3000)"
            )
            close()
        }

        // Fails here if the migrated schema differs from version 2's JSON.
        val migrated = helper.runMigrationsAndValidate(2, listOf(MIGRATION_1_2))

        val modal = migrated.prepare("SELECT modalPrice FROM prices WHERE id = 'lasalgaon-onion-2026-11-07'").use {
            it.step()
            it.getLong(0)
        }
        assertEquals(2_525L, modal)

        val markets = migrated.prepare("SELECT name, district FROM markets ORDER BY name").use {
            buildList {
                while (it.step()) add(it.getText(0) to it.getText(1))
            }
        }
        assertEquals(listOf("Lasalgaon APMC" to "Nashik", "Vashi APMC, Navi Mumbai" to "Thane"), markets)
        migrated.close()
    }

    @Test
    fun version2To3AddsAnEmptyFetchedAtAndKeepsTheRows() = runTest {
        helper.createDatabase(2).apply {
            execSQL("INSERT INTO markets (name, district) VALUES ('Nashik APMC', 'Nashik')")
            execSQL(
                "INSERT INTO prices (id, commodity, market, date, minPrice, maxPrice, modalPrice) " +
                    "VALUES ('nashik-onion-2026-11-07', 'Onion', 'Nashik APMC', 20399, 1100, 2700, 2560)"
            )
            close()
        }

        // No Migration object: the AutoMigration declared on the database is used.
        val migrated = helper.runMigrationsAndValidate(3, emptyList())

        migrated.prepare("SELECT modalPrice, fetchedAt FROM prices").use {
            it.step()
            assertEquals(2_560L, it.getLong(0))
            assertEquals(true, it.isNull(1))
        }
        migrated.close()
    }

    @Test
    fun version3To4AddsEmptyPictureUrlsAndKeepsTheRows() = runTest {
        helper.createDatabase(3).apply {
            execSQL(
                "INSERT INTO prices (id, commodity, market, date, minPrice, maxPrice, modalPrice, fetchedAt) " +
                    "VALUES ('nashik-onion-2026-11-07', 'Onion', 'Nashik APMC', 20399, 1100, 2700, 2560, 1794053700000)"
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(4, emptyList())

        migrated.prepare("SELECT modalPrice, fetchedAt, thumbnailUrl, photoUrl FROM prices").use {
            it.step()
            assertEquals(2_560L, it.getLong(0))
            assertEquals(1_794_053_700_000L, it.getLong(1))
            assertEquals(true, it.isNull(2))
            assertEquals(true, it.isNull(3))
        }
        migrated.close()
    }

    @Test
    fun aVersion1PhoneReachesVersion4InOneOpen() = runTest {
        helper.createDatabase(1).close()

        helper.runMigrationsAndValidate(4, listOf(MIGRATION_1_2)).close()
    }
}
