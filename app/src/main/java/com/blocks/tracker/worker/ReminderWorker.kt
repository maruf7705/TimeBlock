package com.blocks.tracker.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.blocks.tracker.BlocksApplication
import com.blocks.tracker.data.BlocksRepository

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as BlocksApplication
        val repository = BlocksRepository(app.database.entryDao())
        val untitledList = repository.getUntitledEntries()

        if (untitledList.isNotEmpty()) {
            val count = untitledList.size
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = NotificationCompat.Builder(applicationContext, BlocksApplication.CHANNEL_ID_REMINDER)
                .setSmallIcon(android.R.drawable.ic_menu_agenda)
                .setContentTitle("Focus")
                .setContentText("You have $count untitled time block${if (count > 1) "s" else ""}.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            manager.notify(1001, notification)
        }
        return Result.success()
    }
}
