package com.blocks.tracker.data

import android.content.Context
import android.content.SharedPreferences

class FocusStateStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("focus_state_prefs", Context.MODE_PRIVATE)

    var currentScreen: String
        get() = prefs.getString("current_screen", "TRACK") ?: "TRACK"
        set(value) = prefs.edit().putString("current_screen", value).apply()

    var selectedDayViewDate: String
        get() = prefs.getString("selected_day_view_date", "") ?: ""
        set(value) = prefs.edit().putString("selected_day_view_date", value).apply()

    var draftTitle: String
        get() = prefs.getString("draft_title", "") ?: ""
        set(value) = prefs.edit().putString("draft_title", value).apply()

    var fromMin: Int
        get() = prefs.getInt("from_min", -1)
        set(value) = prefs.edit().putInt("from_min", value).apply()

    var isLiveClock: Boolean
        get() = prefs.getBoolean("is_live_clock", true)
        set(value) = prefs.edit().putBoolean("is_live_clock", value).apply()

    // Last date on which user logged or interacted in the app (yyyy-MM-dd)
    var lastActiveDate: String
        get() = prefs.getString("last_active_date", "") ?: ""
        set(value) = prefs.edit().putString("last_active_date", value).apply()

    fun clearDraft() {
        prefs.edit().remove("draft_title").apply()
    }

    // Check-in Notification Settings & State
    var checkInNotificationsEnabled: Boolean
        get() = prefs.getBoolean("checkin_enabled", true)
        set(value) = prefs.edit().putBoolean("checkin_enabled", value).apply()

    var checkInDebugMode: Boolean
        get() = prefs.getBoolean("checkin_debug_mode", false)
        set(value) = prefs.edit().putBoolean("checkin_debug_mode", value).apply()

    var hasSeenPermissionPrompt: Boolean
        get() = prefs.getBoolean("has_seen_perm_prompt", false)
        set(value) = prefs.edit().putBoolean("has_seen_perm_prompt", value).apply()

    var lastActivityTimestamp: Long
        get() = prefs.getLong("last_activity_ts", System.currentTimeMillis())
        set(value) = prefs.edit().putLong("last_activity_ts", value).apply()

    var firstCheckInShown: Boolean
        get() = prefs.getBoolean("first_checkin_shown", false)
        set(value) = prefs.edit().putBoolean("first_checkin_shown", value).apply()

    var firstCheckInTimestamp: Long
        get() = prefs.getLong("first_checkin_ts", 0L)
        set(value) = prefs.edit().putLong("first_checkin_ts", value).apply()

    // Step counter for exact 4-stage reminder sequence:
    // 0: waiting for first check-in (+45m)
    // 1: first check-in fired, waiting for 1st follow-up (+15m)
    // 2: 1st follow-up fired, waiting for 2nd follow-up (+15m)
    // 3: 2nd follow-up fired, waiting for final wrap-up (+45m)
    // 4: final wrap-up fired -> stop completely
    var checkInStep: Int
        get() = prefs.getInt("checkin_step", 0)
        set(value) = prefs.edit().putInt("checkin_step", value).apply()

    var lastMessageTitle: String
        get() = prefs.getString("last_msg_title", "") ?: ""
        set(value) = prefs.edit().putString("last_msg_title", value).apply()

    var hasSeenBatteryTip: Boolean
        get() = prefs.getBoolean("has_seen_battery_tip", false)
        set(value) = prefs.edit().putBoolean("has_seen_battery_tip", value).apply()

    // Alarm stream priority alert (ensures vibration during calls and alarm volume priority)
    var useAlarmStream: Boolean
        get() = prefs.getBoolean("use_alarm_stream", true)
        set(value) = prefs.edit().putBoolean("use_alarm_stream", value).apply()
}
