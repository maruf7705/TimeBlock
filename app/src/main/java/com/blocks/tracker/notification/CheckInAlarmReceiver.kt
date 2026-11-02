package com.blocks.tracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.blocks.tracker.data.BlocksDatabase
import com.blocks.tracker.data.FocusStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class CheckInAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val store = FocusStateStore(context)
                if (!store.checkInNotificationsEnabled) {
                    return@launch
                }

                // 1. Quiet hours (11 PM - 6 AM): No notifications (bypassed in debug mode for testing)
                if (!store.checkInDebugMode && CheckInScheduler.isQuietHours()) {
                    CheckInScheduler.scheduleForSixAm(context)
                    return@launch
                }

                // 2. Active sleeping block: No notifications (bypassed in debug mode for testing)
                if (!store.checkInDebugMode && CheckInScheduler.isSleepingBlockActive(context)) {
                    CheckInScheduler.scheduleFollowup(context)
                    return@launch
                }

                val currentStep = store.checkInStep // 0 = first alert, 1 = 1st follow-up, 2 = 2nd follow-up, 3 = final wrap-up
                if (currentStep >= 4) {
                    // All 4 alerts have been delivered, stop completely!
                    return@launch
                }

                val thisAlertStep = currentStep + 1 // 1, 2, 3, or 4

                // 3. Query DB for context-aware messages
                val today = LocalDate.now().toString()
                val dao = BlocksDatabase.getDatabase(context).entryDao()
                val entries = dao.getEntriesForDateSync(today)

                val hasUntitledBlocks = entries.any { it.title.isBlank() }
                val nowM = LocalTime.now().let { it.hour * 60 + it.minute }
                val lastEntryEnd = if (entries.isNotEmpty()) entries.last().endMin else 0
                val noLogOverTwoHours = (nowM - lastEntryEnd) >= 120
                val isAfterNinePm = LocalTime.now().hour in 21..22

                val isFirst = (thisAlertStep == 1)
                val isFinal = (thisAlertStep == 4)

                val message = if (isFinal) {
                    // Step 4: Special final wrap-up notification
                    CheckInMessageManager.specialOverTwoHours
                } else {
                    CheckInMessageManager.getMessage(
                        isFirst = isFirst,
                        hasUntitledBlocks = hasUntitledBlocks,
                        noLogOverTwoHours = noLogOverTwoHours,
                        isAfterNinePm = isAfterNinePm,
                        lastTitle = store.lastMessageTitle
                    )
                }

                // 4. Show Heads-up Notification
                CheckInNotificationHelper.showNotification(context, message.title, message.body)
                store.lastMessageTitle = message.title
                store.checkInStep = thisAlertStep

                val now = System.currentTimeMillis()
                if (!store.firstCheckInShown) {
                    store.firstCheckInShown = true
                    store.firstCheckInTimestamp = now
                }

                // 5. Schedule next alert according to the cadence:
                // If just delivered step 1 -> schedule step 2 (+15m)
                // If just delivered step 2 -> schedule step 3 (+15m)
                // If just delivered step 3 -> schedule step 4 (+45m)
                // If just delivered step 4 -> stop (do not schedule anything)
                if (thisAlertStep < 4) {
                    CheckInScheduler.scheduleNextStep(context, thisAlertStep + 1)
                }

            } catch (e: Exception) {
                // Fail-safe
            } finally {
                pendingResult.finish()
            }
        }
    }
}
