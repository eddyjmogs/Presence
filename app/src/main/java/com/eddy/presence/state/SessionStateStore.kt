package com.eddy.presence.state

import android.content.Context
import android.content.SharedPreferences
import com.eddy.presence.NotifyType

/**
 * SharedPreferences-backed store for session state that must survive process restarts.
 * All reads/writes are synchronous and safe to call from any thread.
 */
class SessionStateStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var deepWorkActive: Boolean
        get() = prefs.getBoolean(KEY_DEEP_WORK_ACTIVE, false)
        set(v) = prefs.edit().putBoolean(KEY_DEEP_WORK_ACTIVE, v).apply()

    var focusModeActive: Boolean
        get() = prefs.getBoolean(KEY_FOCUS_MODE_ACTIVE, false)
        set(v) = prefs.edit().putBoolean(KEY_FOCUS_MODE_ACTIVE, v).apply()

    var focusModeContext: String
        get() = prefs.getString(KEY_FOCUS_CONTEXT, "") ?: ""
        set(v) = prefs.edit().putString(KEY_FOCUS_CONTEXT, v).apply()

    var currentTask: String
        get() = prefs.getString(KEY_CURRENT_TASK, "") ?: ""
        set(v) = prefs.edit().putString(KEY_CURRENT_TASK, v).apply()

    var intervalMinutes: Int
        get() = prefs.getInt(KEY_INTERVAL_MINUTES, 25)
        set(v) = prefs.edit().putInt(KEY_INTERVAL_MINUTES, v).apply()

    var timerStartTime: Long
        get() = prefs.getLong(KEY_TIMER_START_TIME, 0L)
        set(v) = prefs.edit().putLong(KEY_TIMER_START_TIME, v).apply()

    var timerExpired: Boolean
        get() = prefs.getBoolean(KEY_TIMER_EXPIRED, false)
        set(v) = prefs.edit().putBoolean(KEY_TIMER_EXPIRED, v).apply()

    var pendingAcknowledgement: Boolean
        get() = prefs.getBoolean(KEY_PENDING_ACK, false)
        set(v) = prefs.edit().putBoolean(KEY_PENDING_ACK, v).apply()

    var screenOffTime: Long
        get() = prefs.getLong(KEY_SCREEN_OFF_TIME, 0L)
        set(v) = prefs.edit().putLong(KEY_SCREEN_OFF_TIME, v).apply()

    var notifyAlarm: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_ALARM, false)
        set(v) = prefs.edit().putBoolean(KEY_NOTIFY_ALARM, v).apply()

    var notifyVibration: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_VIBRATION, false)
        set(v) = prefs.edit().putBoolean(KEY_NOTIFY_VIBRATION, v).apply()

    var notifyFlashlight: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_FLASHLIGHT, false)
        set(v) = prefs.edit().putBoolean(KEY_NOTIFY_FLASHLIGHT, v).apply()

    var notifySilent: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_SILENT, true)
        set(v) = prefs.edit().putBoolean(KEY_NOTIFY_SILENT, v).apply()

    var currentDidText: String
        get() = prefs.getString(KEY_CURRENT_DID_TEXT, "") ?: ""
        set(v) = prefs.edit().putString(KEY_CURRENT_DID_TEXT, v).apply()

    var currentNextFocusText: String
        get() = prefs.getString(KEY_CURRENT_NEXT_FOCUS, "") ?: ""
        set(v) = prefs.edit().putString(KEY_CURRENT_NEXT_FOCUS, v).apply()

    var currentNotes: String
        get() = prefs.getString(KEY_CURRENT_NOTES, "") ?: ""
        set(v) = prefs.edit().putString(KEY_CURRENT_NOTES, v).apply()

    var notifyType: NotifyType
        get() = when {
            notifyAlarm -> NotifyType.Alarm
            notifyVibration -> NotifyType.Vibration
            notifyFlashlight -> NotifyType.Flashlight
            else -> NotifyType.Silent
        }
        set(v) {
            notifyAlarm = v == NotifyType.Alarm
            notifyVibration = v == NotifyType.Vibration
            notifyFlashlight = v == NotifyType.Flashlight
            notifySilent = v == NotifyType.Silent
        }

    // Package the user explicitly allowed during Focus Mode via "Continue Anyway".
    // Cleared when they open a different app — ensures the reminder re-fires for new apps.
    var focusModeAllowedPackage: String
        get() = prefs.getString(KEY_FOCUS_ALLOWED_PKG, "") ?: ""
        set(v) = prefs.edit().putString(KEY_FOCUS_ALLOWED_PKG, v).apply()

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(v) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, v).apply()

    fun clearSession() {
        prefs.edit()
            .putBoolean(KEY_DEEP_WORK_ACTIVE, false)
            .putBoolean(KEY_TIMER_EXPIRED, false)
            .putBoolean(KEY_PENDING_ACK, false)
            .putLong(KEY_TIMER_START_TIME, 0L)
            .putString(KEY_CURRENT_DID_TEXT, "")
            .putString(KEY_CURRENT_NEXT_FOCUS, "")
            .putString(KEY_CURRENT_NOTES, "")
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "presence_session"
        private const val KEY_DEEP_WORK_ACTIVE = "deep_work_active"
        private const val KEY_FOCUS_MODE_ACTIVE = "focus_mode_active"
        private const val KEY_FOCUS_CONTEXT = "focus_context"
        private const val KEY_CURRENT_TASK = "current_task"
        private const val KEY_INTERVAL_MINUTES = "interval_minutes"
        private const val KEY_TIMER_START_TIME = "timer_start_time"
        private const val KEY_TIMER_EXPIRED = "timer_expired"
        private const val KEY_PENDING_ACK = "pending_ack"
        private const val KEY_SCREEN_OFF_TIME = "screen_off_time"
        private const val KEY_NOTIFY_ALARM = "notify_alarm"
        private const val KEY_NOTIFY_VIBRATION = "notify_vibration"
        private const val KEY_NOTIFY_FLASHLIGHT = "notify_flashlight"
        private const val KEY_NOTIFY_SILENT = "notify_silent"
        private const val KEY_FOCUS_ALLOWED_PKG = "focus_allowed_pkg"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_CURRENT_DID_TEXT = "current_did_text"
        private const val KEY_CURRENT_NEXT_FOCUS = "current_next_focus"
        private const val KEY_CURRENT_NOTES = "current_notes"
    }
}
