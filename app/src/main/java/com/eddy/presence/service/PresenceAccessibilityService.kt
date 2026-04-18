package com.eddy.presence.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.eddy.presence.PresenceApplication
import com.eddy.presence.state.SessionStateStore
import com.eddy.presence.ui.overlay.DeepWorkOverlayActivity
import com.eddy.presence.ui.reminder.FocusSoftReminderActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PresenceAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Cached whitelist for the currently observed context
    private var observedContext: String = ""
    private var whitelistJob: Job? = null
    @Volatile private var cachedWhitelist: Set<String> = emptySet()

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        // Don't react to our own package — would cause an infinite re-launch loop
        if (packageName == applicationContext.packageName) return

        val store = SessionStateStore(applicationContext)

        if (store.deepWorkActive && store.pendingAcknowledgement) {
            DeepWorkOverlayActivity.launch(applicationContext)
            return
        }

        if (store.focusModeActive) {
            val contextName = store.focusModeContext
            updateWhitelistObserver(contextName)
            if (packageName !in cachedWhitelist) {
                FocusSoftReminderActivity.launch(applicationContext, contextName)
            }
        }
    }

    // Starts (or re-starts) a coroutine collecting the whitelist for the given context.
    // No-op if the context hasn't changed.
    private fun updateWhitelistObserver(contextName: String) {
        if (contextName == observedContext) return
        observedContext = contextName
        whitelistJob?.cancel()
        cachedWhitelist = emptySet()
        if (contextName.isBlank()) return

        val repo = (application as PresenceApplication).whitelistRepository
        whitelistJob = scope.launch {
            repo.getPackagesForContext(contextName).collect { packages ->
                cachedWhitelist = packages.toHashSet()
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
