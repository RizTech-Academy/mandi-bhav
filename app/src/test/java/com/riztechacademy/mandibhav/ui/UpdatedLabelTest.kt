package com.riztechacademy.mandibhav.ui

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdatedLabelTest {

    private val pune = ZoneId.of("Asia/Kolkata")
    private val fourOFive = Instant.parse("2026-11-07T10:35:00Z") // 4:05 pm in Pune

    @Test
    fun anUnknownTimeIsSaidPlainlyAndCountsAsStale() {
        assertEquals(
            UpdatedLabel("Not known when these prices were fetched. Tap Refresh.", isStale = true),
            updatedLabel(null, fourOFive, pune, Locale.US),
        )
    }

    @Test
    fun theSameDayGivesTheTimeInThePhonesZone() {
        val sixPm = fourOFive.plusSeconds(115 * 60)
        assertEquals(UpdatedLabel("Updated today at 4:05 PM", isStale = false), updatedLabel(fourOFive, sixPm, pune, Locale.US))
    }

    @Test
    fun nextMorningGivesTheDateAndIsStale() {
        val nextMorning = Instant.parse("2026-11-08T03:30:00Z") // 9 am in Pune, 17 h later
        assertEquals(UpdatedLabel("Updated 7 Nov at 4:05 PM", isStale = true), updatedLabel(fourOFive, nextMorning, pune, Locale.US))
    }

    @Test
    fun indianEnglishWritesTheTimeItsOwnWay() {
        val label = updatedLabel(fourOFive, fourOFive, pune, Locale.forLanguageTag("en-IN"))
        assertEquals("Updated today at 4:05 pm", label.text)
    }
}
