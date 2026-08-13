package com.sheshabiz.quickquote.domain

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sheshabiz.quickquote.R
import com.sheshabiz.quickquote.appContainer
import com.sheshabiz.quickquote.data.db.entity.InvoiceStatus

class OverdueInvoiceCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer()
        if (!container.preferences.overdueRemindersEnabled.value) return Result.success()

        val now = System.currentTimeMillis()
        val overdueCount = container.invoiceRepository.getAllOnce()
            .count { it.status == InvoiceStatus.UNPAID && it.dueDate < now }

        if (overdueCount > 0) {
            notify(overdueCount)
        }
        return Result.success()
    }

    private fun notify(count: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Overdue invoices",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Alerts when unpaid invoices pass their due date." }
            manager.createNotificationChannel(channel)
        }

        val title = if (count == 1) "1 invoice is overdue" else "$count invoices are overdue"
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("Follow up to get paid — check the Invoices tab.")
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "overdue_invoices"
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "overdue_invoice_check"
    }
}
