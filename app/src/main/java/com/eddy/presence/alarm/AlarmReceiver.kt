package com.eddy.presence.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.eddy.presence.state.SessionStateStore

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val store = SessionStateStore(context)

        // Mark timer as expired and overlay as pending before triggering notifications.
        // Step 5 adds alarm sound / vibration / flashlight here.
        store.timerExpired = true
        store.pendingAcknowledgement = true
    }
}
