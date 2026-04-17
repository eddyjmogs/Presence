package com.eddy.presence.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.eddy.presence.MainActivity
import com.eddy.presence.R
import com.eddy.presence.alarm.TorchController
import com.eddy.presence.receiver.ScreenStateReceiver
import com.eddy.presence.state.SessionStateStore

class PresenceForegroundService : Service() {

    private lateinit var notificationManager: NotificationManager
    private var activeRingtone: Ringtone? = null
    private val screenStateReceiver = ScreenStateReceiver()

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        // ACTION_SCREEN_ON/OFF cannot be received by a static manifest receiver —
        // must be registered dynamically here while the service is alive.
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DEEP_WORK -> {
                val task = intent.getStringExtra(EXTRA_TASK) ?: ""
                val store = SessionStateStore(this)
                store.deepWorkActive = true
                store.currentTask = task
                startForeground(NOTIFICATION_ID, buildDeepWorkNotification(task))
            }
            ACTION_ALARM_FIRED -> {
                startAlarmRingtone()
            }
            ACTION_STOP_ALARM -> {
                stopAlarmRingtone()
                cancelVibration()
                TorchController.turnOff(this)
            }
            ACTION_STOP -> {
                stopAlarmRingtone()
                cancelVibration()
                TorchController.turnOff(this)
                SessionStateStore(this).clearSession()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterReceiver(screenStateReceiver)
        stopAlarmRingtone()
        super.onDestroy()
    }

    private fun startAlarmRingtone() {
        stopAlarmRingtone() // ensure no double-play
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(this, uri) ?: return
        ringtone.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ringtone.isLooping = true
        }
        ringtone.play()
        activeRingtone = ringtone
    }

    private fun stopAlarmRingtone() {
        activeRingtone?.stop()
        activeRingtone = null
    }

    private fun cancelVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator.cancel()
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Presence Session",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Active Deep Work and Focus Mode sessions"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildDeepWorkNotification(task: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val contentText = if (task.isNotBlank()) "Working on: $task" else "Deep Work in progress"

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Deep Work")
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .build()
    }

    companion object {
        const val ACTION_START_DEEP_WORK = "com.eddy.presence.START_DEEP_WORK"
        const val ACTION_ALARM_FIRED = "com.eddy.presence.ALARM_FIRED"
        const val ACTION_STOP_ALARM = "com.eddy.presence.STOP_ALARM"
        const val ACTION_STOP = "com.eddy.presence.STOP"
        const val EXTRA_TASK = "extra_task"

        private const val CHANNEL_ID = "presence_session"
        private const val NOTIFICATION_ID = 1

        fun startDeepWork(context: Context, task: String) {
            context.startForegroundService(
                Intent(context, PresenceForegroundService::class.java)
                    .apply { action = ACTION_START_DEEP_WORK; putExtra(EXTRA_TASK, task) }
            )
        }

        fun fireAlarm(context: Context) {
            context.startService(
                Intent(context, PresenceForegroundService::class.java)
                    .apply { action = ACTION_ALARM_FIRED }
            )
        }

        fun stopAlarm(context: Context) {
            context.startService(
                Intent(context, PresenceForegroundService::class.java)
                    .apply { action = ACTION_STOP_ALARM }
            )
        }

        fun launchOverlay(context: Context) {
            com.eddy.presence.ui.overlay.DeepWorkOverlayActivity.launch(context)
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, PresenceForegroundService::class.java)
                    .apply { action = ACTION_STOP }
            )
        }
    }
}
