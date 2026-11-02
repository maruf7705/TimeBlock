package com.blocks.tracker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.blocks.tracker.BlocksApplication
import com.blocks.tracker.data.BlocksRepository
import java.time.LocalDate

class DayAutoCloseWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as BlocksApplication
        val repository = BlocksRepository(app.database.entryDao())
        val yesterday = LocalDate.now().minusDays(1).toString()
        repository.runDayAutoClose(yesterday)
        return Result.success()
    }
}
