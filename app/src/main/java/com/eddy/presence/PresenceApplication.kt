package com.eddy.presence

import android.app.Application
import com.eddy.presence.data.db.PresenceDatabase
import com.eddy.presence.data.repository.FocusContextRepository
import com.eddy.presence.data.repository.LogRepository
import com.eddy.presence.data.repository.WhitelistRepository

class PresenceApplication : Application() {
    val database by lazy { PresenceDatabase.getInstance(this) }
    val logRepository by lazy { LogRepository(database.logEntryDao()) }
    val whitelistRepository by lazy { WhitelistRepository(database.whitelistDao()) }
    val focusContextRepository by lazy { FocusContextRepository(database.focusContextDao()) }
}
