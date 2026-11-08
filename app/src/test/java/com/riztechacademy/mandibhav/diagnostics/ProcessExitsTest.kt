package com.riztechacademy.mandibhav.diagnostics

import android.app.ApplicationExitInfo
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessExitsTest {

    private val ist = ZoneId.of("Asia/Kolkata")
    private val at = Instant.parse("2026-11-08T21:29:33Z")

    /** Shortened from a real ANR dump on the course emulator. */
    private val anrDump = """
        Subject: Input dispatching timed out (MainActivity is not responding. Waited 5001ms for MotionEvent).
        RssHwmKb: 173564

        "Signal Catcher" daemon prio=10 tid=6 Runnable
          at art.Thread.dump(Native method)

        "main" prio=5 tid=1 Sleeping
          | group="main" sCount=1 ucsCount=0 flags=1
          at java.lang.Thread.sleep(Native method)
          at java.lang.Thread.sleep(Thread.java:356)
          at mc2.l(r8-map-id-e2ec9451:19)

        "OkHttp TaskRunner" daemon prio=5 tid=20 TimedWaiting
          at java.lang.Object.wait(Native method)
    """.trimIndent()

    @Test
    fun aCrashIsOneLineBecauseAndroidKeepsNoStackForIt() {
        val crash = ProcessExit(ApplicationExitInfo.REASON_CRASH, at, "crash", trace = null)

        assertTrue(isProblem(crash))
        assertEquals(listOf("Last process ended: crash at 9 Nov 02:59:33 (crash)"), describe(crash, ist))
    }

    @Test
    fun anAnrShowsItsSubjectAndOnlyTheMainThread() {
        val anr = ProcessExit(ApplicationExitInfo.REASON_ANR, at, null, anrDump)

        val lines = describe(anr, ist)

        assertEquals("Last process ended: ANR at 9 Nov 02:59:33", lines[0])
        assertTrue(lines[1].startsWith("Subject: Input dispatching timed out"))
        assertEquals("\"main\" prio=5 tid=1 Sleeping", lines[2])
        assertEquals("  at mc2.l(r8-map-id-e2ec9451:19)", lines.last())
        assertFalse(lines.any { "OkHttp" in it || "Signal Catcher" in it })
    }

    @Test
    fun anAnrDumpCountsEvenWhenTheProcessEndedSomeOtherWay() {
        // Seen on the emulator: the app recovered from the ANR, was stopped
        // later, and Android kept the ANR's dump on the "stopped" record.
        val stopped = ProcessExit(ApplicationExitInfo.REASON_USER_REQUESTED, at, "[FORCE STOP]", anrDump)

        assertTrue(isProblem(stopped))
    }

    @Test
    fun normalEndsAreNotReported() {
        val updated = ProcessExit(ApplicationExitInfo.REASON_PACKAGE_UPDATED, at, "stop due to installPackageLI", null)
        val swiped = ProcessExit(ApplicationExitInfo.REASON_USER_REQUESTED, at, "[FORCE STOP]", null)

        assertFalse(isProblem(updated))
        assertFalse(isProblem(swiped))
        assertEquals("app updated", reasonName(updated.reason))
    }

    @Test
    fun aDumpWithNoMainThreadGivesNoLines() {
        assertEquals(emptyList<String>(), mainThreadOf("Subject: something\n\n\"Binder:1\" prio=5\n"))
    }
}
