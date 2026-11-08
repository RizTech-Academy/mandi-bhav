package com.riztechacademy.mandibhav.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import com.riztechacademy.mandibhav.model.CommodityPrice
import com.riztechacademy.mandibhav.model.GeoPoint
import java.time.format.DateTimeFormatter
import java.util.Locale

private val shareDateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/**
 * The text a shared price carries, written to be read in a chat by someone
 * who does not have the app:
 *
 * Onion at Lasalgaon APMC, 8 Nov 2026: ₹2,580 a quintal (modal price). Min ₹1,250, max ₹2,950. From Mandi Bhav.
 */
fun shareText(price: CommodityPrice): String =
    "${price.commodity} at ${price.market}, ${shareDateFormat.format(price.date)}: " +
        "${formatRupees(price.modalPrice)} a quintal (modal price). " +
        "Min ${formatRupees(price.minPrice)}, max ${formatRupees(price.maxPrice)}. From Mandi Bhav."

/**
 * The system share sheet, with [text]. An implicit intent: the app says what
 * it wants done (send this text), and the user chooses which app does it.
 */
fun Context.shareText(text: String, title: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, text)
        .putExtra(Intent.EXTRA_TITLE, title)
    startActivity(Intent.createChooser(send, null))
}

/**
 * [name] on a map, in whichever maps app the phone has. Not checked first
 * with resolveActivity: since Android 11 that answers null for apps this
 * app cannot see, even when one is installed. Trying, and catching the
 * failure, is right on every version. Returns false if nothing could open it.
 */
fun Context.openOnMap(name: String, at: GeoPoint): Boolean {
    val uri = "geo:${at.latitude},${at.longitude}?q=${at.latitude},${at.longitude}(${Uri.encode(name)})".toUri()
    return try {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
        true
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "No maps app on this phone can show $name.", Toast.LENGTH_LONG).show()
        false
    }
}
