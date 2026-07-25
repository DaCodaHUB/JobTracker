package com.dangle.jobtracker.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitor for network connectivity changes using the Android [ConnectivityManager].
 * 
 * It exposes a cold [Flow] that emits a boolean whenever the device connects to
 * or disconnects from a network with internet capability.
 */
@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext context: Context
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * A Flow representing the current network connection status.
     * Uses [callbackFlow] to wrap the Android [ConnectivityManager.NetworkCallback] system.
     */
    val isConnected: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            /** Triggered when a suitable network becomes available. */
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                trySend(true)
            }

            /** Triggered when the currently active network is lost. */
            override fun onLost(network: Network) {
                super.onLost(network)
                trySend(false)
            }
        }

        // Define which networks we are interested in (those with internet access)
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)

        // Initial state check: Ensure the Flow has an immediate accurate starting value
        val initialNetwork = connectivityManager.activeNetwork
        val initialCapabilities = connectivityManager.getNetworkCapabilities(initialNetwork)
        val initialIsConnected = initialCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        trySend(initialIsConnected)

        // Clean up: Unregister the callback when the collector's scope is cancelled
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged() // Avoid emitting duplicate identical statuses
}
