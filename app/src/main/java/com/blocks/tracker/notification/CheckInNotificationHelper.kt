package com.blocks.tracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.blocks.tracker.MainActivity
import com.blocks.tracker.R
import com.blocks.tracker.data.FocusStateStore

object CheckInNotificationHelper {

    const val CHANNEL_ID_ALARM = "focus_checkin_alarm_v2"
    const val CHANNEL_ID_STANDARD = "focus_checkin_standard_v2"
    const val NOTIFICATION_ID_CHECKIN = 2001
    const val ACTION_CONTINUE_CURRENT_BLOCK = "com.blocks.tracker.ACTION_CONTINUE_CURRENT_BLOCK"
    const val EXTRA_FOCUS_TITLE = "EXTRA_FOCUS_TITLE"

    // Strong, prominent vibration: 3 solid 450ms pulses separated by 200ms pauses (~1.7s total)
    val VIBRATION_PATTERN = longArrayOf(0, 450, 200, 450, 200, 450)
    val VIBRATION_AMPLITUDES = intArrayOf(0, 255, 0, 255, 0, 255)

    fun getSoundUri(context: Context): Uri {
        return try {
            val resourceId = R.raw.checkin_45
            Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/$resourceId")
        } catch (e: Exception) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return

            // 1. Alarm Stream Channel (Heads-up banner, vibration; audio handled via alarm stream MediaPlayer)
            val alarmChannel = NotificationChannel(
                CHANNEL_ID_ALARM,
                "Check-in Reminders (Alarm Priority)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority mindful check-in reminders using alarm stream"
                setSound(null, null)
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
            manager.createNotificationChannel(alarmChannel)

            // 2. Standard Notification Channel (Standard audio attributes fallback)
            val soundUri = getSoundUri(context)
            val standardAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val standardChannel = NotificationChannel(
                CHANNEL_ID_STANDARD,
                "Check-in Reminders (Standard)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Standard notification check-in reminders"
                setSound(soundUri, standardAttributes)
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
            manager.createNotificationChannel(standardChannel)
        }
    }

    /**
     * Triggers distinct physical vibration and double-chime audio via the Alarm Stream.
     * - Uses USAGE_ALARM to ensure vibration continues even during active cellular/VoIP calls.
     * - Plays checkin_45.mp3 (double chime with interval) at alarm volume so it cannot be muted by notification mute.
     * - If in an active phone call, suppresses loud speaker chime to protect user's ear while ensuring full vibration.
     */
    fun triggerAlarmStreamAlert(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val isInCall = audioManager.mode == AudioManager.MODE_IN_CALL ||
                audioManager.mode == AudioManager.MODE_IN_COMMUNICATION

        val alarmAudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // 1. Force hardware vibration via USAGE_ALARM (bypasses in-call notification suppression)
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(VIBRATION_PATTERN, VIBRATION_AMPLITUDES, -1),
                    alarmAudioAttributes
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(VIBRATION_PATTERN, -1)
            }
        } catch (e: Exception) {
            // Safe fallback
        }

        // 2. Play double chime via MediaPlayer on USAGE_ALARM (only when not in an active call)
        if (!isInCall) {
            try {
                val soundUri = getSoundUri(context)
                val mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(alarmAudioAttributes)
                    setDataSource(context, soundUri)
                    prepare()
                    setOnCompletionListener { mp ->
                        try {
                            mp.release()
                        } catch (e: Exception) {}
                    }
                }
                mediaPlayer.start()
            } catch (e: Exception) {
                // Safe fallback
            }
        }
    }

    fun showNotification(context: Context, title: String, body: String) {
        createNotificationChannels(context)
        val store = FocusStateStore(context)
        val useAlarm = store.useAlarmStream

        // 1. Content tap intent -> Opens MainActivity with EXTRA_FOCUS_TITLE
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_FOCUS_TITLE, true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            101,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Action button: "একই কাজ চলছে"
        val actionIntent = Intent(context, CheckInActionReceiver::class.java).apply {
            action = ACTION_CONTINUE_CURRENT_BLOCK
        }
        val actionPendingIntent = PendingIntent.getBroadcast(
            context,
            102,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (useAlarm) CHANNEL_ID_ALARM else CHANNEL_ID_STANDARD

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(VIBRATION_PATTERN)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "একই কাজ চলছে", actionPendingIntent)

        if (!useAlarm) {
            builder.setSound(getSoundUri(context))
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_CHECKIN, builder.build())

        // If alarm stream mode is active, trigger the alarm stream vibration and double chime audio
        if (useAlarm) {
            triggerAlarmStreamAlert(context)
        }
    }

    fun cancelNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID_CHECKIN)
    }
}
