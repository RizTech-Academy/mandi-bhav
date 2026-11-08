package com.riztechacademy.mandibhav.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.riztechacademy.mandibhav.model.GeoPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** What asking for the phone's location can come back with. */
sealed interface LocationResult {
    /** Where the phone is, to within [accuracyMetres] (null if the provider did not say). */
    data class Found(val point: GeoPoint, val accuracyMetres: Float?) : LocationResult

    /** The app has no location permission (it was refused, or revoked in Settings). */
    data object NoPermission : LocationResult

    /** Location is switched off for the whole phone, in quick settings or Settings. */
    data object LocationOff : LocationResult

    /** Location is on and allowed, but no fix came in time: indoors, or no signal. */
    data object NoFix : LocationResult
}

/** The phone's location, asked for once, when the user asks for the nearest market. */
interface LocationSource {
    suspend fun currentLocation(): LocationResult
}

/**
 * The framework's [LocationManager], through core-ktx's [LocationManagerCompat]:
 * no Google Play services dependency, so it works on every phone, including
 * those sold without Google's apps.
 *
 * It asks for one fix, never for updates. Not getLastKnownLocation: that is
 * whatever fix the phone last cached, from wherever it last was, and on the
 * course emulator it answered from 13,389 km away. getCurrentLocation already
 * returns a cached fix when one is recent enough, and a fresh one otherwise.
 */
class AndroidLocationSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : LocationSource {

    private val manager = context.getSystemService(LocationManager::class.java)

    override suspend fun currentLocation(): LocationResult {
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (coarse != PackageManager.PERMISSION_GRANTED) return LocationResult.NoPermission
        if (!LocationManagerCompat.isLocationEnabled(manager)) return LocationResult.LocationOff

        val location = try {
            currentFix()
        } catch (e: SecurityException) {
            // Revoked between the check and the call.
            return LocationResult.NoPermission
        }
        return location?.let { LocationResult.Found(GeoPoint(it.latitude, it.longitude), it.accuracyOrNull()) }
            ?: LocationResult.NoFix
    }

    /**
     * One fresh fix: first from the fused (or network) provider, which uses
     * Wi-Fi and mobile towers and costs little; if that finds nothing, from
     * GPS. On Android 12 and later an app with only approximate location may
     * use GPS, and gets the fix blurred to about 2 km, which is all it asked
     * for. On the course emulator, which has no Wi-Fi or tower location, the
     * fused provider gave nothing in its 10 seconds and GPS answered in 4.
     */
    @Throws(SecurityException::class)
    private suspend fun currentFix(): Location? {
        for ((provider, timeout) in providers()) {
            fixFrom(provider, timeout)?.let { return it }
        }
        return null
    }

    @Throws(SecurityException::class)
    private suspend fun fixFrom(provider: String, timeoutMillis: Long): Location? =
        withTimeoutOrNull(timeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                LocationManagerCompat.getCurrentLocation(
                    manager,
                    provider,
                    signal,
                    ContextCompat.getMainExecutor(context),
                ) { location -> continuation.resume(location) }
            }
        }

    /** The providers to try, in order, each with how long to wait for it. */
    private fun providers(): List<Pair<String, Long>> {
        val enabled = manager.getProviders(true)
        val cheap = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && LocationManager.FUSED_PROVIDER in enabled ->
                LocationManager.FUSED_PROVIDER
            LocationManager.NETWORK_PROVIDER in enabled -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        // Before Android 12, GPS needs precise location, which this app does not ask for.
        val gps = LocationManager.GPS_PROVIDER.takeIf {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && it in enabled
        }
        return listOfNotNull(cheap?.let { it to CHEAP_TIMEOUT_MILLIS }, gps?.let { it to GPS_TIMEOUT_MILLIS })
    }

    private fun Location.accuracyOrNull(): Float? = if (hasAccuracy()) accuracy else null

    private companion object {
        const val CHEAP_TIMEOUT_MILLIS = 10_000L
        const val GPS_TIMEOUT_MILLIS = 20_000L
    }
}
