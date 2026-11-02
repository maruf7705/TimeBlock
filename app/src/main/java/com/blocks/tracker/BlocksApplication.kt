package com.blocks.tracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.*
import com.blocks.tracker.data.BlocksDatabase
import com.blocks.tracker.data.FocusStateStore
import com.blocks.tracker.worker.DayAutoCloseWorker
import com.blocks.tracker.worker.ReminderWorker
import java.util.concurrent.TimeUnit

class BlocksApplication : Application() {

    val database by lazy { BlocksDatabase.getDatabase(this) }
    val stateStore by lazy { FocusStateStore(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        com.blocks.tracker.notification.CheckInNotificationHelper.createNotificationChannels(this)
        scheduleBackgroundWorkers()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_REMINDER,
                "Untitled Blocks Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to name untitled time blocks"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun scheduleBackgroundWorkers() {
        val workManager = WorkManager.getInstance(this)

        // 1. Day Auto-Close near 23:59 daily
        val autoCloseWork = PeriodicWorkRequestBuilder<DayAutoCloseWorker>(
            1, TimeUnit.DAYS
        ).build()
        workManager.enqueueUniquePeriodicWork(
            "DayAutoCloseWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            autoCloseWork
        )

        // 2. Local notification reminder every 2 hours while untitled entries exist
        val reminderWork = PeriodicWorkRequestBuilder<ReminderWorker>(
            2, TimeUnit.HOURS
        ).build()
        workManager.enqueueUniquePeriodicWork(
            "ReminderWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            reminderWork
        )
    }

    companion object {
        const val CHANNEL_ID_REMINDER = "blocks_reminders"
    }
}
