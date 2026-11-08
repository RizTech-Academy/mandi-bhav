package com.riztechacademy.mandibhav.ui.permissions

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Each row is a step measured on the emulator (Android 16): what
 * checkSelfPermission and shouldShowRequestPermissionRationale returned,
 * and whether the app had seen a refusal before.
 */
class RuntimePermissionTest {

    @Test
    fun freshInstallIsNotAsked() {
        assertEquals(PermissionStatus.NotAsked, permissionStatus(granted = false, showRationale = false, refusedBefore = false))
    }

    @Test
    fun oneRefusalShowsARationale() {
        assertEquals(PermissionStatus.Refused, permissionStatus(granted = false, showRationale = true, refusedBefore = false))
        assertEquals(PermissionStatus.Refused, permissionStatus(granted = false, showRationale = true, refusedBefore = true))
    }

    @Test
    fun aSecondRefusalLooksLikeAFreshInstallUnlessTheAppRemembered() {
        // The trap: the system's two facts are the same as on a fresh install.
        assertEquals(PermissionStatus.Blocked, permissionStatus(granted = false, showRationale = false, refusedBefore = true))
    }

    @Test
    fun dismissingTheFirstDialogIsNotARefusal() {
        // Back on the first dialog: not granted, no rationale, and nothing to remember.
        assertEquals(PermissionStatus.NotAsked, permissionStatus(granted = false, showRationale = false, refusedBefore = false))
    }

    @Test
    fun grantedWinsWhateverCameBefore() {
        assertEquals(PermissionStatus.Granted, permissionStatus(granted = true, showRationale = false, refusedBefore = true))
    }

    @Test
    fun eachStatusHasItsAction() {
        assertEquals(PermissionAction.Proceed, actionFor(PermissionStatus.Granted))
        assertEquals(PermissionAction.Ask, actionFor(PermissionStatus.NotAsked))
        assertEquals(PermissionAction.ExplainFirst, actionFor(PermissionStatus.Refused))
        assertEquals(PermissionAction.OpenSettings, actionFor(PermissionStatus.Blocked))
    }
}
