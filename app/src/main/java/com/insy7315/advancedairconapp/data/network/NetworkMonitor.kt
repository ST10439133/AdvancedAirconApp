//Android Developers. 2026. Read network state | Connectivity. [Online]. Available at: https://developer.android.com/develop/connectivity/network-ops/reading-network-state [Accessed: 5 October 2026].
//GitHub. 2026. connectivity_validator/doc/how-it-works.md. [Online]. Available at: https://github.com/sabeelmuttil/connectivity_validator/blob/master/doc/how-it-works.md [Accessed: 5 October 2026].
// Android Open Source. 2025. NetworkStateTracker.kt - WorkManager. [Online]. Available at: https://android.googlesource.com/platform/frameworks/support/+/90e1ce986b2fecb289231b9e4261611b36bafa68/work/work-runtime/src/main/java/androidx/work/impl/constraints/trackers/NetworkStateTracker.kt [Accessed: 5 October 2026].

package com.insy7315.advancedairconapp.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

object NetworkMonitor {

    fun observe(context: Context): Flow<Boolean> = callbackFlow {
        val cm = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        fun currentStatus(): Boolean {
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }

        trySend(currentStatus())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(currentStatus()) }
            override fun onLost(network: Network) { trySend(currentStatus()) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(currentStatus())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, callback)
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    fun isOnline(context: Context): Boolean {
        val cm = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}