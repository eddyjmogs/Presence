package com.eddy.presence.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.eddy.presence.intervalToMs
import com.eddy.presence.state.SessionStateStore
import com.eddy.presence.ui.overlay.DeepWorkOverlayActivity
import com.eddy.presence.ui.overlay.OverlayScenario

class PresenceAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return // user is in our own app, no action needed

        val store = SessionStateStore(this)
        if (!store.pendingAcknowledgement) return

        val now = System.currentTimeMillis()
        val elapsedMinutes = if (store.timerStartTime > 0L)
            ((now - store.timerStartTime) / 60_000L).toInt().coerceAtLeast(1) else 1
        DeepWorkOverlayActivity.launch(
            context = this,
            scenario = OverlayScenario.OverTime,
            setMinutes = store.intervalMinutes,
            elapsedMinutes = elapsedMinutes,
        )
    }

    override fun onInterrupt() = Unit
}
