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
                val now = System.currentTimeMillis()
                val store = SessionStateStore(this)
                store.deepWorkActive = true
                store.timerStartTime = now
                store.timerExpired = false
                store.pendingAcknowledgement = false
                val endMs = now + intervalToMs(store.intervalMinutes)
                AlarmScheduler.schedule(this, endMs)
                startForeground(NOTIFICATION_ID, buildDeepWorkNotification(endMs))
            }
            ACTION_UPDATE_DEEP_WORK_NOTIFICATION -> {
                val store = SessionStateStore(this)
                if (store.deepWorkActive) {
                    val endMs = store.timerStartTime + intervalToMs(store.intervalMinutes)
                    notificationManager.notify(NOTIFICATION_ID, buildDeepWorkNotification(endMs))
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
            ACTION_STOP -> {
                stopAlarmRingtone()
                cancelVibration()
                TorchController.turnOff(this)
                val store = SessionStateStore(this)
                store.clearSession()
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

    private fun buildDeepWorkNotification(intervalEndMs: Long = 0L): Notification {
        val store = SessionStateStore(this)
        val isPending = store.pendingAcknowledgement
        val now = System.currentTimeMillis()

        val contentPendingIntent: PendingIntent
        val actionLabel: String
        val actionPendingIntent: PendingIntent

        if (isPending) {
            val elapsedMinutes = if (store.timerStartTime > 0L)
                ((now - store.timerStartTime) / 60_000L).toInt().coerceAtLeast(1) else 1
            val overlayIntent = com.eddy.presence.ui.overlay.DeepWorkOverlayActivity.buildIntent(
                this, com.eddy.presence.ui.overlay.OverlayScenario.OverTime,
                store.intervalMinutes, elapsedMinutes,
            )
            contentPendingIntent = PendingIntent.getActivity(
                this, 2, overlayIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            actionLabel = "Check In Now"
            actionPendingIntent = PendingIntent.getActivity(
                this, 3, overlayIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        } else {
            val openAppIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            contentPendingIntent = PendingIntent.getActivity(
                this, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val openSessionIntent = Intent(this, com.eddy.presence.ui.session.DeepWorkSessionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            actionLabel = "Open Session"
            actionPendingIntent = PendingIntent.getActivity(
                this, 1, openSessionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Deep Work")
            .setContentText(if (isPending) "Check-in pending" else "Session in progress")
            .setContentIntent(contentPendingIntent)
            .addAction(0, actionLabel, actionPendingIntent)
            .setOngoing(true)
            .setShowWhen(true)
            .setUsesChronometer(!isPending)
            .setChronometerCountDown(true)
            .setWhen(intervalEndMs.takeIf { it > 0L } ?: now)
            .build()
    }

    companion object {
        const val ACTION_START_DEEP_WORK = "com.eddy.presence.START_DEEP_WORK"
        const val ACTION_ALARM_FIRED = "com.eddy.presence.ALARM_FIRED"
        const val ACTION_STOP_ALARM = "com.eddy.presence.STOP_ALARM"
        const val ACTION_STOP = "com.eddy.presence.STOP"
        const val ACTION_UPDATE_DEEP_WORK_NOTIFICATION = "com.eddy.presence.UPDATE_DW_NOTIFICATION"

        private const val CHANNEL_ID = "presence_session"
        private const val NOTIFICATION_ID = 1

        fun startDeepWork(context: Context) {
            context.startForegroundService(
                Intent(context, PresenceForegroundService::class.java)
                    .apply { action = ACTION_START_DEEP_WORK }
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

        fun updateDeepWorkNotification(context: Context) {
            context.startService(
                Intent(context, PresenceForegroundService::class.java)
                    .apply { action = ACTION_UPDATE_DEEP_WORK_NOTIFICATION }
            )
        }
    }
}
