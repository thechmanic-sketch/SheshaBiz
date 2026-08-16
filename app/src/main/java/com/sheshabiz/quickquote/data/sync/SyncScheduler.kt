package com.sheshabiz.quickquote.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules the periodic background cross-device sync and fires one-off passes on the
 * triggers that most matter for freshness: connectivity regained, app foreground, and right
 * after a successful login. Mirrors [com.sheshabiz.quickquote.domain.ReminderScheduler]'s
 * shape. [SyncManager.sync] itself is a no-op for anyone not logged in, so every method here
 * is safe to call unconditionally, offline user or not.
 */
class SyncScheduler(private val context: Context) {

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    /** Starts the periodic pass and starts watching for connectivity regained. Idempotent —
     * safe to call every time the app starts. */
    fun start() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        registerConnectivityCallback()
    }

    fun stop() {
        WorkManager.getInstance(context).cancelUniqueWork(SyncWorker.WORK_NAME)
        unregisterConnectivityCallback()
    }

    /** Fires a single sync pass right away — called after a successful login, on app
     * foreground (see [com.sheshabiz.quickquote.MainActivity]), and whenever connectivity
     * comes back. Replaces any not-yet-run pending one-off, rather than queueing another. */
    fun syncNow() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            SyncWorker.ONE_OFF_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun registerConnectivityCallback() {
        if (networkCallback != null) return
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                syncNow()
            }
        }
        // registerNetworkCallback(NetworkRequest, NetworkCallback) — not
        // registerDefaultNetworkCallback, which needs API 24 — since minSdk here is 21.
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching { manager.registerNetworkCallback(request, callback) }
            .onSuccess { networkCallback = callback }
    }

    private fun unregisterConnectivityCallback() {
        val callback = networkCallback ?: return
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        runCatching { manager?.unregisterNetworkCallback(callback) }
        networkCallback = null
    }
}
