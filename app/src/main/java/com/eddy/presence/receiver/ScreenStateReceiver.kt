package com.eddy.presence.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.eddy.presence.state.SessionStateStore
import com.eddy.presence.ui.overlay.DeepWorkOverlayActivity
import com.eddy.presence.ui.overlay.OverlayScenario

class ScreenStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> onScreenOff(context)
            Intent.ACTION_SCREEN_ON -> onScreenOn(context)
        }
    }

    private fun onScreenOff(context: Context) {
        SessionStateStore(context).screenOffTime = System.currentTimeMillis()
    }

    private fun onScreenOn(context: Context) {
        val store = SessionStateStore(context)
        if (!store.deepWorkActive) return

        val screenOffTime = store.screenOffTime
        if (screenOffTime == 0L) return

        val now = System.currentTimeMillis()
        val screenOffDuration = now - screenOffTime
        val intervalMs = store.intervalMinutes * 60_000L

        when {
            store.timerExpired -> {
                // Scenario B: alarm fired while the screen was off (or user just picked up phone).
                // Elapsed is measured from when the interval started, not just screen-off time,
                // so the heading accurately reflects total overrun.
                val elapsedMinutes = ((now - store.timerStartTime) / 60_000L)
                    .toInt()
                    .coerceAtLeast(1)
                store.pendingAcknowledgement = true
                DeepWorkOverlayActivity.launch(
                    context = context,
                    scenario = OverlayScenario.OverTime,
                    setMinutes = store.intervalMinutes,
                    elapsedMinutes = elapsedMinutes,
                )
            }

            intervalMs > 0 && screenOffDuration >= intervalMs -> {
                // Scenario C: screen was off for at least the full interval with no active timer.
                // Treat as an untracked away period.
                val elapsedMinutes = (screenOffDuration / 60_000L).toInt().coerceAtLeast(1)
                store.pendingAcknowledgement = true
                DeepWorkOverlayActivity.launch(
                    context = context,
                    scenario = OverlayScenario.Away,
                    setMinutes = store.intervalMinutes,
                    elapsedMinutes = elapsedMinutes,
                )
            }

            // Within interval and no alarm fired — user is on track, do nothing
        }
    }
}
