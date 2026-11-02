package com.blocks.tracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.blocks.tracker.data.FocusStateStore

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            CheckInNotificationHelper.createNotificationChannels(context)
            val store = FocusStateStore(context)
            if (store.checkInNotificationsEnabled) {
                CheckInScheduler.scheduleFirstCheckIn(context)
            }
        }
    }
}
