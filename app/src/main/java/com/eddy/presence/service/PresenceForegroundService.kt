package com.eddy.presence.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.eddy.presence.MainActivity
import com.eddy.presence.R
import com.eddy.presence.state.SessionStateStore

class PresenceForegroundService : Service() {

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
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
            ACTION_STOP -> {
                SessionStateStore(this).clearSession()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

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
        const val ACTION_STOP = "com.eddy.presence.STOP"
        const val EXTRA_TASK = "extra_task"

        private const val CHANNEL_ID = "presence_session"
        private const val NOTIFICATION_ID = 1

        fun startDeepWork(context: Context, task: String) {
            val intent = Intent(context, PresenceForegroundService::class.java).apply {
                action = ACTION_START_DEEP_WORK
                putExtra(EXTRA_TASK, task)
            }
            context.startForegroundService(intent)
        }

        fun launchOverlay(context: Context) {
            com.eddy.presence.ui.overlay.DeepWorkOverlayActivity.launch(context)
        }

        fun stop(context: Context) {
            val intent = Intent(context, PresenceForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
