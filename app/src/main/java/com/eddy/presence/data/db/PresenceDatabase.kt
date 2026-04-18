package com.eddy.presence.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.eddy.presence.data.model.FocusContext
import com.eddy.presence.data.model.LogEntry
import com.eddy.presence.data.model.WhitelistEntry

@Database(
    entities = [LogEntry::class, WhitelistEntry::class, FocusContext::class],
    version = 3,
    exportSchema = false,
)
abstract class PresenceDatabase : RoomDatabase() {

    abstract fun logEntryDao(): LogEntryDao
    abstract fun whitelistDao(): WhitelistDao
    abstract fun focusContextDao(): FocusContextDao

    companion object {
        @Volatile private var instance: PresenceDatabase? = null

        fun getInstance(context: Context): PresenceDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PresenceDatabase::class.java,
                    "presence.db",
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { instance = it }
            }
    }
}
