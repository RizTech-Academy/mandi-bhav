package com.riztechacademy.mandibhav.notifications

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.riztechacademy.mandibhav.MainActivity
import com.riztechacademy.mandibhav.R
import com.riztechacademy.mandibhav.domain.AlertCrossing
import com.riztechacademy.mandibhav.model.AlertDirection
import com.riztechacademy.mandibhav.ui.formatRupees
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/** The one channel Mandi Bhav posts to. The id is permanent; the name is what Settings shows. */
const val PRICE_ALERTS_CHANNEL_ID = "price_alerts"

/** The extras a notification's tap carries: the market and commodity whose price to open. */
const val EXTRA_MARKET = "com.riztechacademy.mandibhav.MARKET"
const val EXTRA_COMMODITY = "com.riztechacademy.mandibhav.COMMODITY"

/** The extra the summary's tap carries: open the Alerts tab. */
const val EXTRA_OPEN_ALERTS = "com.riztechacademy.mandibhav.OPEN_ALERTS"

/**
 * Creates the app's notification channels. Safe to call on every start:
 * creating a channel that exists changes only its name and description,
 * never the importance or sound the user may have chosen in Settings.
 */
fun createNotificationChannels(context: Context) {
    NotificationManagerCompat.from(context).createNotificationChannel(
        NotificationChannelCompat.Builder(PRICE_ALERTS_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName("Price alerts")
            .setDescription("When a price crosses a level you set on the Alerts tab")
            .build(),
    )
}

/**
 * Whether a notification posted now would be seen: the app is allowed to
 * notify (on Android 13 and later, the POST_NOTIFICATIONS permission) and
 * the user has not turned the price alerts channel off.
 */
fun areAlertNotificationsOn(context: Context): Boolean {
    val manager = NotificationManagerCompat.from(context)
    if (!manager.areNotificationsEnabled()) return false
    val channel = manager.getNotificationChannelCompat(PRICE_ALERTS_CHANNEL_ID)
    return channel == null || channel.importance != NotificationManagerCompat.IMPORTANCE_NONE
}

/** "Onion at Lasalgaon APMC: ₹2,580" */
fun AlertCrossing.notificationTitle(): String =
    "${price.commodity} at ${price.market}: ${formatRupees(price.modalPrice)}"

/** "Above your ₹2,500 a quintal. Modal price on 8 November." */
fun AlertCrossing.notificationText(): String {
    val side = when (alert.direction) {
        AlertDirection.Above -> "Above"
        AlertDirection.Below -> "Below"
    }
    val day = price.date.format(DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH))
    return "$side your ${formatRupees(alert.level)} a quintal. Modal price on $day."
}

/**
 * Posts price-alert notifications through Android's notification system:
 * one per alert, in one group, under a summary.
 *
 * The group matters. With two or more notifications from one app, Android
 * bundles them by itself, and a tap on that bundle opens the app's launcher
 * screen and clears every notification in it. With a group of its own, the
 * app decides what the bundle says and where a tap on it goes: the Alerts tab.
 */
class SystemAlertNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AlertNotifier {

    override fun notify(crossings: List<AlertCrossing>) {
        if (crossings.isEmpty()) return
        // Android 13 and later drop a notification posted without the
        // permission, silently. Checking first says so in the code, and is
        // what lint asks for before notify().
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = NotificationManagerCompat.from(context)
        for (crossing in crossings) {
            // The alert's id as the notification's: a second crossing of the
            // same alert replaces its notification rather than adding another.
            manager.notify(crossing.alert.id.toInt(), crossing.toNotification())
        }
        manager.notify(SUMMARY_ID, summary(crossings))
    }

    private fun AlertCrossing.toNotification(): Notification {
        // Not the price's id: it carries the date, and tomorrow's refresh
        // replaces it. The market and commodity still name today's price.
        val open = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_MARKET, price.market)
            .putExtra(EXTRA_COMMODITY, price.commodity)
        return NotificationCompat.Builder(context, PRICE_ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alerts)
            .setContentTitle(notificationTitle())
            .setContentText(notificationText())
            // One request code per alert. PendingIntents that differ only in
            // their extras are the same PendingIntent to Android, so with one
            // shared code, every alert's tap would open the last one posted.
            .setContentIntent(tapOpening(open, requestCode = alert.id.toInt()))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setGroup(GROUP_KEY)
            .build()
    }

    /** What the bundle shows when collapsed; a tap opens the Alerts tab. */
    private fun summary(crossings: List<AlertCrossing>): Notification {
        val open = Intent(context, MainActivity::class.java).putExtra(EXTRA_OPEN_ALERTS, true)
        val lines = NotificationCompat.InboxStyle()
        crossings.forEach { lines.addLine(it.notificationTitle()) }
        return NotificationCompat.Builder(context, PRICE_ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alerts)
            .setContentTitle("Price alerts")
            .setContentText(crossings.joinToString { it.notificationTitle() })
            .setStyle(lines)
            .setContentIntent(tapOpening(open, requestCode = SUMMARY_ID))
            .setAutoCancel(true)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .build()
    }

    private fun tapOpening(intent: Intent, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private companion object {
        const val GROUP_KEY = "com.riztechacademy.mandibhav.PRICE_ALERTS"

        /** Alert ids start at 1, so 0 is free for the summary, as id and as request code. */
        const val SUMMARY_ID = 0
    }
}
