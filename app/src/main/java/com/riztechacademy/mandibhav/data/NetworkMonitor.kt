package com.riztechacademy.mandibhav.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/** Whether the phone has a working connection, now and as it changes. */
interface NetworkMonitor {
    /** True while the default network can reach the internet. Watches only while collected. */
    val isOnline: Flow<Boolean>

    /** The answer right now, for telling "no connection" from "server down" after a failure. */
    fun isOnlineNow(): Boolean
}

/**
 * [NetworkMonitor] from the system's [ConnectivityManager]. "Online" means
 * the default network has been validated: Android has checked that it
 * really reaches the internet, which a café Wi-Fi with a sign-in page or a
 * train's dead mobile data does not.
 */
class ConnectivityManagerNetworkMonitor @Inject constructor(
    @ApplicationContext context: Context,
) : NetworkMonitor {

    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    override val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.isValidated())
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        trySend(isOnlineNow())
        connectivity.registerDefaultNetworkCallback(callback)
        // Unregistered as soon as nobody collects, so the app does not keep
        // a callback with the system while no screen is looking.
        awaitClose { connectivity.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged().conflate()

    override fun isOnlineNow(): Boolean =
        connectivity.getNetworkCapabilities(connectivity.activeNetwork)?.isValidated() == true

    private fun NetworkCapabilities.isValidated() =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
