package com.eddy.presence.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "log_entries")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val mode: String = "DEEP_WORK",       // "DEEP_WORK" | "FOCUS" — Focus entries added in Step 10+
    val scenario: String = "OnTime",      // "OnTime" | "OverTime" | "Away"
    val taskName: String = "",
    val didText: String = "",
    val nextFocusText: String = "",
    val intervalMinutes: Int = 0,
    val notifyAlarm: Boolean = false,
    val notifyVibration: Boolean = false,
    val notifyFlashlight: Boolean = false,
    val notifySilent: Boolean = false,
    val notes: String = "",
)
