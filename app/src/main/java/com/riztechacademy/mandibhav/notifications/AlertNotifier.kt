package com.riztechacademy.mandibhav.notifications

import com.riztechacademy.mandibhav.domain.AlertCrossing

/**
 * Tells the user that an alert's level has been crossed. An interface, so
 * the code that decides *when* to tell can be tested without Android's
 * notification system; [SystemAlertNotifier] is the real one.
 */
interface AlertNotifier {
    /** Shows a notification for each of [crossings]. Does nothing if notifications are off. */
    fun notify(crossings: List<AlertCrossing>)
}
