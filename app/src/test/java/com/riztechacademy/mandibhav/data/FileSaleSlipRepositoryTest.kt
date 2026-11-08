package com.riztechacademy.mandibhav.data

import android.content.Context
import android.net.Uri
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.riztechacademy.mandibhav.data.local.MandiBhavDatabase
import java.io.File
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/** Slips over a real in-memory database and the real file system, under Robolectric. */
@RunWith(AndroidJUnit4::class)
class FileSaleSlipRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database = Room.inMemoryDatabaseBuilder<MandiBhavDatabase>(context)
        .setDriver(AndroidSQLiteDriver())
        .build()
    private val slips = FileSaleSlipRepository(context, database.saleSlipDao())
    private val day = LocalDate.of(2026, 11, 8)

    @After
    fun close() {
        database.close()
        File(context.filesDir, "slips").deleteRecursively()
    }

    /** A picture somewhere else, as the photo picker or the camera app would hand over. */
    private fun picture(bytes: String): Uri {
        val file = File.createTempFile("picked", ".jpg", context.cacheDir)
        file.writeText(bytes)
        return Uri.fromFile(file)
    }

    @Test
    fun attachingCopiesThePictureIntoTheAppsOwnFiles() = runTest {
        val source = picture("slip one")

        slips.attach("Lasalgaon APMC", "Onion", day, source)

        val slip = slips.slip("Lasalgaon APMC", "Onion").first()!!
        assertEquals(day, slip.date)
        assertEquals(File(context.filesDir, "slips"), slip.file.parentFile)
        assertEquals("slip one", slip.file.readText())
        // A copy: the original going away does not take the slip with it.
        File(source.path!!).delete()
        assertTrue(slip.file.exists())
    }

    @Test
    fun aNewSlipReplacesTheOldAndDeletesItsFile() = runTest {
        slips.attach("Lasalgaon APMC", "Onion", day, picture("old"))
        val old = slips.slip("Lasalgaon APMC", "Onion").first()!!.file

        slips.attach("Lasalgaon APMC", "Onion", day.plusDays(1), picture("new"))

        val new = slips.slip("Lasalgaon APMC", "Onion").first()!!
        assertNotEquals(old, new.file)
        assertFalse(old.exists())
        assertEquals("new", new.file.readText())
        assertEquals(1, File(context.filesDir, "slips").listFiles()!!.size)
    }

    @Test
    fun removingDeletesTheRowAndTheFile() = runTest {
        slips.attach("Lasalgaon APMC", "Onion", day, picture("slip"))
        val file = slips.slip("Lasalgaon APMC", "Onion").first()!!.file

        slips.remove("Lasalgaon APMC", "Onion")

        assertNull(slips.slip("Lasalgaon APMC", "Onion").first())
        assertFalse(file.exists())
    }

    @Test
    fun aPictureThatCannotBeReadLeavesTheSlipAsItWas() = runTest {
        slips.attach("Lasalgaon APMC", "Onion", day, picture("kept"))

        try {
            slips.attach("Lasalgaon APMC", "Onion", day, Uri.fromFile(File(context.cacheDir, "gone.jpg")))
            fail("expected an IOException")
        } catch (e: IOException) {
            // expected
        }

        assertEquals("kept", slips.slip("Lasalgaon APMC", "Onion").first()!!.file.readText())
        assertEquals(1, File(context.filesDir, "slips").listFiles()!!.size)
    }

    @Test
    fun eachCommodityAndMarketHasItsOwnSlip() = runTest {
        slips.attach("Lasalgaon APMC", "Onion", day, picture("onion"))

        assertNull(slips.slip("Lasalgaon APMC", "Soybean").first())
        assertNull(slips.slip("Nashik APMC", "Onion").first())
    }
}
