package com.blocks.tracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.blocks.tracker.data.BlocksDatabase
import com.blocks.tracker.data.EntryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class CheckInActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = LocalDate.now().toString()
                val dao = BlocksDatabase.getDatabase(context).entryDao()
                val entries = dao.getEntriesForDateSync(today).sortedBy { it.startMin }

                val nowTime = LocalTime.now()
                val nowM = nowTime.hour * 60 + nowTime.minute

                if (entries.isNotEmpty()) {
                    val lastEntry = entries.last()
                    if (nowM > lastEntry.startMin) {
                        val updated = lastEntry.copy(
                            endMin = nowM,
                            updatedAt = System.currentTimeMillis()
                        )
                        dao.insertEntry(updated)
                    }
                } else {
                    if (nowM > 0) {
                        val newBlock = EntryEntity(
                            id = UUID.randomUUID().toString(),
                            date = today,
                            startMin = 0,
                            endMin = nowM,
                            title = "কাজ চলছে",
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        dao.insertEntry(newBlock)
                    }
                }

                // 2. Clear notification and reset timer
                CheckInNotificationHelper.cancelNotification(context)
                CheckInScheduler.resetTimer(context)

            } catch (e: Exception) {
                // Fail-safe
            } finally {
                pendingResult.finish()
            }
        }
    }
}
