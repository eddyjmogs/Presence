package com.eddy.presence.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.eddy.presence.alarm.AlarmScheduler
import com.eddy.presence.intervalToMs
import com.eddy.presence.service.PresenceForegroundService
import com.eddy.presence.state.SessionStateStore

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val store = SessionStateStore(context)
        if (!store.deepWorkActive || store.timerStartTime == 0L) return

        val endMs = store.timerStartTime + intervalToMs(store.intervalMinutes)
        val now = System.currentTimeMillis()

        if (endMs > now) {
            // Interval hasn't elapsed yet — reschedule the remaining time
            AlarmScheduler.schedule(context, endMs)
        } else {
            // Interval already passed while the phone was off — mark as expired
            store.timerExpired = true
            store.pendingAcknowledgement = true
        }

        // Restart the foreground service so the notification and screen receiver come back
        PresenceForegroundService.startDeepWork(context)
    }
}
