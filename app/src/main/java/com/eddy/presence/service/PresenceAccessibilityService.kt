package com.eddy.presence.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.eddy.presence.state.SessionStateStore
import com.eddy.presence.ui.overlay.DeepWorkOverlayActivity

class PresenceAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        // Don't react to our own overlay — would cause an infinite re-launch loop
        if (packageName == applicationContext.packageName) return

        val store = SessionStateStore(applicationContext)
        if (store.deepWorkActive && store.pendingAcknowledgement) {
            DeepWorkOverlayActivity.launch(applicationContext)
        }
    }

    override fun onInterrupt() = Unit
}
