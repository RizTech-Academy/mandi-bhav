package com.riztechacademy.mandibhav.ui.permissions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/** Where a runtime permission stands. */
enum class PermissionStatus {
    /** Granted: go ahead. */
    Granted,

    /** Not granted, and never refused: the system dialog will show. */
    NotAsked,

    /** Refused once: the dialog will show one more time, so explain first. */
    Refused,

    /** Refused twice: the dialog will not show again. Only Settings can grant it. */
    Blocked,
}

/**
 * Where [granted], [showRationale] and [refusedBefore] put a permission.
 *
 * Android gives an app only the first two. `shouldShowRequestPermissionRationale`
 * is true after one refusal and false both before the first request and after
 * the second refusal, so on its own it cannot tell "never asked" from
 * "blocked". The third fact, whether the user has ever refused, is the app's
 * to remember: [refusedBefore].
 *
 * A request dismissed with Back, or by tapping outside the dialog, is not a
 * refusal: Android answers "not granted" with no rationale and counts nothing,
 * and this function, with [refusedBefore] still false, says [PermissionStatus.NotAsked].
 */
fun permissionStatus(granted: Boolean, showRationale: Boolean, refusedBefore: Boolean): PermissionStatus = when {
    granted -> PermissionStatus.Granted
    showRationale -> PermissionStatus.Refused
    refusedBefore -> PermissionStatus.Blocked
    else -> PermissionStatus.NotAsked
}

/** What a tap on the feature does, for each status. */
enum class PermissionAction { Proceed, Ask, ExplainFirst, OpenSettings }

fun actionFor(status: PermissionStatus): PermissionAction = when (status) {
    PermissionStatus.Granted -> PermissionAction.Proceed
    PermissionStatus.NotAsked -> PermissionAction.Ask
    PermissionStatus.Refused -> PermissionAction.ExplainFirst
    PermissionStatus.Blocked -> PermissionAction.OpenSettings
}

/** One runtime permission as a screen sees it, and what its buttons do. */
class RuntimePermissionState(
    val status: PermissionStatus,
    /** The app's own explanation is showing, before the system's dialog. */
    val explaining: Boolean,
    /** The user tapped the feature: proceed, ask, explain first, or open Settings. */
    val onFeatureTap: () -> Unit,
    /** The user read the explanation and chose to continue: ask the system now. */
    val confirmExplanation: () -> Unit,
    val dismissExplanation: () -> Unit,
)

/**
 * Remembers [permission] for this screen.
 *
 * [refusedBefore] and [onRefusedChange] are the app's memory of whether the
 * user has ever refused it, kept somewhere that outlives the process.
 * [onGranted] runs when the user taps the feature with the permission
 * granted, when a request is granted, and when the user comes back from
 * Settings having granted it there.
 *
 * Everything else is read from the system each time it is needed, and again
 * whenever the screen resumes: the user can change the permission in
 * Settings at any moment, and if they revoke it while the app is in the
 * background, Android kills the process.
 */
@Composable
fun rememberRuntimePermission(
    permission: String,
    refusedBefore: Boolean,
    onRefusedChange: (Boolean) -> Unit,
    onGranted: () -> Unit,
): RuntimePermissionState {
    val context = LocalContext.current
    val activity = context.findActivity()
    val currentOnGranted by rememberUpdatedState(onGranted)
    val currentOnRefusedChange by rememberUpdatedState(onRefusedChange)
    val currentRefusedBefore by rememberUpdatedState(refusedBefore)

    var explaining by rememberSaveable(permission) { mutableStateOf(false) }
    // Bumped to read the system again: after an answer, and on every resume.
    var reads by remember { mutableIntStateOf(0) }
    // Whether the user has just been to Settings from the Blocked card.
    var sentToSettings by rememberSaveable(permission) { mutableStateOf(false) }

    fun read(): PermissionStatus {
        val rationale = activity.shouldShowRationale(permission)
        return permissionStatus(context.isGranted(permission), rationale, currentRefusedBefore || rationale)
    }

    /**
     * Writes down what only the system knows right now: that the user has
     * refused (a rationale is due), or that refusals no longer matter (granted).
     * Called after an answer and on resume, never during composition.
     */
    fun recordWhatTheSystemShows() {
        val granted = context.isGranted(permission)
        val rationale = activity.shouldShowRationale(permission)
        if (rationale && !currentRefusedBefore) currentOnRefusedChange(true)
        if (granted && currentRefusedBefore) currentOnRefusedChange(false)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        recordWhatTheSystemShows()
        reads++
        if (isGranted) currentOnGranted()
    }

    LifecycleResumeEffect(permission) {
        recordWhatTheSystemShows()
        reads++
        if (sentToSettings && context.isGranted(permission)) {
            // Back from Settings, where the user turned it on: finish what they asked for.
            sentToSettings = false
            currentOnGranted()
        }
        onPauseOrDispose { }
    }

    val status = remember(reads, refusedBefore) { read() }
    return RuntimePermissionState(
        status = status,
        explaining = explaining,
        onFeatureTap = {
            when (actionFor(read())) {
                PermissionAction.Proceed -> currentOnGranted()
                PermissionAction.Ask -> launcher.launch(permission)
                PermissionAction.ExplainFirst -> explaining = true
                PermissionAction.OpenSettings -> {
                    sentToSettings = true
                    context.openAppSettings()
                }
            }
        },
        confirmExplanation = {
            explaining = false
            launcher.launch(permission)
        },
        dismissExplanation = { explaining = false },
    )
}

private fun Activity.shouldShowRationale(permission: String) =
    ActivityCompat.shouldShowRequestPermissionRationale(this, permission)

/** The activity a composable is in; Compose's context may be a wrapper around it. */
fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    error("No activity for $this")
}

/** The app's own page in Settings, the only place a blocked permission can be granted. */
fun Context.openAppSettings() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
}

fun Context.isGranted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
