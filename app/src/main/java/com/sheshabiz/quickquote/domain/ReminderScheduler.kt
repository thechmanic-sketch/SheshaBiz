package com.sheshabiz.quickquote.domain

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Schedules/cancels the periodic background check for overdue invoices. */
class ReminderScheduler(private val context: Context) {

    fun start() {
        val request = PeriodicWorkRequestBuilder<OverdueInvoiceCheckWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            OverdueInvoiceCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun stop() {
        WorkManager.getInstance(context).cancelUniqueWork(OverdueInvoiceCheckWorker.WORK_NAME)
    }
}
