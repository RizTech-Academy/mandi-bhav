package com.riztechacademy.mandibhav.ui

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Saved prices older than this are flagged as possibly out of date. */
val STALE_AFTER: Duration = Duration.ofHours(12)

/** What the line above the prices says about how old they are. */
data class UpdatedLabel(val text: String, val isStale: Boolean)

/**
 * "Updated today at 4:05 PM", "Updated 6 Nov at 4:05 PM", or, when the
 * time is not known, a plain statement saying so. [now],
 * [zone] and [locale] are parameters so that a test can fix them.
 */
fun updatedLabel(
    lastFetched: Instant?,
    now: Instant,
    zone: ZoneId,
    locale: Locale,
): UpdatedLabel {
    if (lastFetched == null) {
        // Null is not "never": rows saved before version 3 have no time either.
        return UpdatedLabel("Not known when these prices were fetched. Tap Refresh.", isStale = true)
    }
    val time = DateTimeFormatter.ofPattern("h:mm a", locale).format(lastFetched.atZone(zone))
    val day = lastFetched.atZone(zone).toLocalDate()
    val text = if (day == now.atZone(zone).toLocalDate()) {
        "Updated today at $time"
    } else {
        "Updated ${DateTimeFormatter.ofPattern("d MMM", locale).format(day)} at $time"
    }
    return UpdatedLabel(text, isStale = Duration.between(lastFetched, now) > STALE_AFTER)
}
