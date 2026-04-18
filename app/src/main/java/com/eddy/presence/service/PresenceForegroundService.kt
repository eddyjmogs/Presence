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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.eddy.presence.MainActivity
import com.eddy.presence.R
import com.eddy.presence.alarm.AlarmScheduler
import com.eddy.presence.intervalToMs
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
                val now = System.currentTimeMillis()
                val store = SessionStateStore(this)
                store.deepWorkActive = true
                store.currentTask = task
                store.timerStartTime = now
                store.timerExpired = false
                store.pendingAcknowledgement = false
                val endMs = now + intervalToMs(store.intervalMinutes)
                AlarmScheduler.schedule(this, endMs)
                startForeground(NOTIFICATION_ID, buildDeepWorkNotification(task, endMs))
            }
            ACTION_UPDATE_DEEP_WORK_NOTIFICATION -> {
                val store = SessionStateStore(this)
                if (store.deepWorkActive) {
                    val endMs = store.timerStartTime + intervalToMs(store.intervalMinutes)
                    notificationManager.notify(
                        NOTIFICATION_ID,
                        buildDeepWorkNotification(store.currentTask, endMs),
                    )
                }
            }
            ACTION_ALARM_FIRED -> {
                startAlarmRingtone()
            }
            ACTION_STOP_ALARM -> {
                stopAlarmRingtone()
                cancelVibration()
                TorchController.turnOff(this)
            }
            ACTION_START_FOCUS_MODE -> {
                val contextName = intent.getStringExtra(EXTRA_CONTEXT) ?: ""
                val store = SessionStateStore(this)
                store.focusModeActive = true
                store.focusModeContext = contextName
                store.focusModeAllowedPackage = ""
                startForeground(NOTIFICATION_ID, buildFocusModeNotification(contextName))
            }
            ACTION_STOP_FOCUS_MODE -> {
                val store = SessionStateStore(this)
                store.focusModeActive = false
                store.focusModeAllowedPackage = ""
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_STOP -> {
                stopAlarmRingtone()
                cancelVibration()
                TorchController.turnOff(this)
                val store = SessionStateStore(this)
                store.clearSession()
                store.focusModeActive = false
                store.focusModeAllowedPackage = ""
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
        ringtone.play()
        activeRingtone = ringtone
        Handler(Looper.getMainLooper()).postDelayed({ stopAlarmRingtone() }, 3_000)
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

    private fun buildFocusModeNotification(contextName: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Focus Mode")
            .setContentText("Context: $contextName")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun buildDeepWorkNotification(task: String, intervalEndMs: Long = 0L): Notification {
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
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(intervalEndMs.takeIf { it > 0L } ?: System.currentTimeMillis())
            .build()
    }

    companion object {
        const val ACTION_START_DEEP_WORK = "com.eddy.presence.START_DEEP_WORK"
        const val ACTION_START_FOCUS_MODE = "com.eddy.presence.START_FOCUS_MODE"
        const val ACTION_STOP_FOCUS_MODE = "com.eddy.presence.STOP_FOCUS_MODE"
        const val ACTION_ALARM_FIRED = "com.eddy.presence.ALARM_FIRED"
        const val ACTION_STOP_ALARM = "com.eddy.presence.STOP_ALARM"
        const val ACTION_STOP = "com.eddy.presence.STOP"
        const val ACTION_UPDATE_DEEP_WORK_NOTIFICATION = "com.eddy.presence.UPDATE_DW_NOTIFICATION"
        const val EXTRA_TASK = "extra_task"
        const val EXTRA_CONTEXT = "extra_context"

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

        fun startFocusMode(context: Context, contextName: String) {
            context.startForegroundService(
                Intent(context, PresenceForegroundService::class.java)
                    .apply { action = ACTION_START_FOCUS_MODE; putExtra(EXTRA_CONTEXT, contextName) }
            )
        }

        fun stopFocusMode(context: Context) {
            context.startService(
                Intent(context, PresenceForegroundService::class.java)
                    .apply { action = ACTION_STOP_FOCUS_MODE }
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, PresenceForegroundService::class.java)
                    .apply { action = ACTION_STOP }
            )
        }

        fun updateDeepWorkNotification(context: Context) {
            context.startService(
                Intent(context, PresenceForegroundService::class.java)
                    .apply { action = ACTION_UPDATE_DEEP_WORK_NOTIFICATION }
            )
        }
    }
}
