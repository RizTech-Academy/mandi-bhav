package com.riztechacademy.mandibhav.diagnostics

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * How the app's previous process ended, as Android recorded it (Android 11
 * and later). Plain values, so what Mandi Bhav says about it can be tested.
 *
 * [trace] is set for ANRs and native crashes only: for an ANR it is the
 * thread dump Android took, the same one Play's Android vitals shows. For
 * a Java or Kotlin crash Android records that it happened, not the stack;
 * that is in logcat on your phone, and in Android vitals from the field.
 */
data class ProcessExit(
    val reason: Int,
    val timestamp: Instant,
    val description: String?,
    val trace: String?,
)

/** A reason code in words. */
@RequiresApi(30)
fun reasonName(reason: Int): String = when (reason) {
    ApplicationExitInfo.REASON_ANR -> "ANR"
    ApplicationExitInfo.REASON_CRASH -> "crash"
    ApplicationExitInfo.REASON_CRASH_NATIVE -> "native crash"
    ApplicationExitInfo.REASON_LOW_MEMORY -> "killed for low memory"
    ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "killed for excessive resource use"
    ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "failed to start"
    ApplicationExitInfo.REASON_USER_REQUESTED -> "stopped by the user or adb"
    ApplicationExitInfo.REASON_PACKAGE_UPDATED -> "app updated"
    ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "permission changed"
    ApplicationExitInfo.REASON_EXIT_SELF -> "exited"
    ApplicationExitInfo.REASON_SIGNALED -> "killed by a signal"
    ApplicationExitInfo.REASON_OTHER -> "other"
    else -> "reason $reason"
}

/** The reasons a developer should look into; the rest are normal ends of a process. */
@RequiresApi(30)
fun isProblem(exit: ProcessExit): Boolean =
    exit.trace != null || exit.reason in setOf(
        ApplicationExitInfo.REASON_ANR,
        ApplicationExitInfo.REASON_CRASH,
        ApplicationExitInfo.REASON_CRASH_NATIVE,
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE,
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE,
    )

/**
 * The main thread's part of an ANR thread dump: from the line starting
 * "main" to the first blank line, at most [maxLines] lines. Empty if the
 * dump has no main thread.
 */
fun mainThreadOf(trace: String, maxLines: Int = 20): List<String> =
    trace.lineSequence()
        .dropWhile { !it.startsWith("\"main\"") }
        .takeWhile { it.isNotBlank() }
        .take(maxLines)
        .toList()

private val timeFormat = DateTimeFormatter.ofPattern("d MMM HH:mm:ss", Locale.ENGLISH)

/**
 * What the debug log says about the last process: one line, then the main
 * thread if there is a dump, and the subject line it starts with.
 */
@RequiresApi(30)
fun describe(exit: ProcessExit, zone: ZoneId): List<String> {
    val first = buildString {
        append("Last process ended: ").append(reasonName(exit.reason))
        append(" at ").append(timeFormat.format(exit.timestamp.atZone(zone)))
        exit.description?.takeIf { it.isNotBlank() }?.let { append(" (").append(it).append(')') }
    }
    val trace = exit.trace ?: return listOf(first)
    val subject = trace.lineSequence().firstOrNull { it.startsWith("Subject:") }
    return listOfNotNull(first, subject) + mainThreadOf(trace)
}

/** The most recent ends of this app's processes, newest first. */
@RequiresApi(30)
fun readProcessExits(context: Context, max: Int = 5): List<ProcessExit> {
    val activityManager = context.getSystemService(ActivityManager::class.java)
    return activityManager.getHistoricalProcessExitReasons(context.packageName, 0, max).map { info ->
        ProcessExit(
            reason = info.reason,
            timestamp = Instant.ofEpochMilli(info.timestamp),
            description = info.description,
            trace = info.traceInputStream?.bufferedReader()?.use { it.readText() },
        )
    }
}
