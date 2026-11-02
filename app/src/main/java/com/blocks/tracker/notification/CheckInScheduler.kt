package com.blocks.tracker.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.blocks.tracker.data.BlocksDatabase
import com.blocks.tracker.data.FocusStateStore
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar

object CheckInScheduler {

    private const val REQUEST_CODE_ALARM = 3001

    fun getAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, CheckInAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun isQuietHours(): Boolean {
        val hour = LocalTime.now().hour
        return hour >= 23 || hour < 6
    }

    suspend fun isSleepingBlockActive(context: Context): Boolean {
        val today = LocalDate.now().toString()
        val dao = BlocksDatabase.getDatabase(context).entryDao()
        val entries = dao.getEntriesForDateSync(today)
        if (entries.isEmpty()) return false

        val lastEntry = entries.last()
        val titleLower = lastEntry.title.lowercase()
        val isSleepTitle = titleLower.contains("sleep") || titleLower.contains("ঘুম")
        if (!isSleepTitle) return false

        val nowM = LocalTime.now().let { it.hour * 60 + it.minute }
        // If current time is within or shortly after the sleep block
        return nowM in (lastEntry.startMin..lastEntry.endMin + 30)
    }

    fun resetTimer(context: Context) {
        val store = FocusStateStore(context)
        cancelAllReminders(context)

        store.firstCheckInShown = false
        store.firstCheckInTimestamp = 0L
        store.checkInStep = 0
        store.lastActivityTimestamp = System.currentTimeMillis()

        if (store.checkInNotificationsEnabled) {
            scheduleFirstCheckIn(context)
        }
    }

    fun scheduleFirstCheckIn(context: Context) {
        val store = FocusStateStore(context)
        if (!store.checkInNotificationsEnabled) return

        val delayMs = if (store.checkInDebugMode) {
            60_000L // 1 minute in debug mode
        } else {
            45 * 60_000L // 45 minutes in production
        }

        scheduleAlarmAt(context, System.currentTimeMillis() + delayMs)
    }

    /**
     * Schedules next notification in the exact 4-stage cadence:
     * - After Step 1 (first alert shown): wait 15m for Step 2
     * - After Step 2 (1st follow-up shown): wait 15m for Step 3
     * - After Step 3 (2nd follow-up shown): wait 45m for Step 4 (final wrap-up)
     * - After Step 4 (final alert shown): stop completely
     */
    fun scheduleNextStep(context: Context, nextStep: Int) {
        val store = FocusStateStore(context)
        if (!store.checkInNotificationsEnabled) return
        if (nextStep > 4) return // All 4 stages finished, STOP

        val isDebug = store.checkInDebugMode
        val delayMs = when (nextStep) {
            2 -> if (isDebug) 20_000L else 15 * 60_000L // 15m later (Follow-up 1)
            3 -> if (isDebug) 20_000L else 15 * 60_000L // 15m later (Follow-up 2)
            4 -> if (isDebug) 60_000L else 45 * 60_000L // 45m later (Final Wrap-up)
            else -> return
        }

        scheduleAlarmAt(context, System.currentTimeMillis() + delayMs)
    }

    fun scheduleFollowup(context: Context) {
        val store = FocusStateStore(context)
        scheduleNextStep(context, store.checkInStep + 1)
    }

    fun scheduleForSixAm(context: Context) {
        val calendar = Calendar.getInstance().apply {
            if (get(Calendar.HOUR_OF_DAY) >= 23) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        scheduleAlarmAt(context, calendar.timeInMillis)
    }

    private fun scheduleAlarmAt(context: Context, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = getAlarmPendingIntent(context)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    fun cancelAllReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = getAlarmPendingIntent(context)
        alarmManager.cancel(pendingIntent)
        CheckInNotificationHelper.cancelNotification(context)
    }
}
