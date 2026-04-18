package com.eddy.presence

import android.app.Application
import com.eddy.presence.data.db.PresenceDatabase
import com.eddy.presence.data.repository.LogRepository

class PresenceApplication : Application() {
    val database by lazy { PresenceDatabase.getInstance(this) }
    val logRepository by lazy { LogRepository(database.logEntryDao()) }
}
