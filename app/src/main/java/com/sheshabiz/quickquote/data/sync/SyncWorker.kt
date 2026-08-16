package com.sheshabiz.quickquote.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sheshabiz.quickquote.appContainer

/** Runs one [SyncManager.sync] pass in the background — mirrors
 * [com.sheshabiz.quickquote.domain.OverdueInvoiceCheckWorker]'s shape exactly. A no-op (fast
 * success) for anyone not logged in, since [SyncManager.sync] itself no-ops in that case. */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer()
        return runCatching { container.syncManager.sync() }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }

    companion object {
        const val WORK_NAME = "cross_device_sync"
        const val ONE_OFF_WORK_NAME = "cross_device_sync_one_off"
    }
}
