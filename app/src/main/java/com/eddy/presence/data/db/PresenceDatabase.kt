package com.eddy.presence.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.eddy.presence.data.model.LogEntry

@Database(entities = [LogEntry::class], version = 1, exportSchema = false)
abstract class PresenceDatabase : RoomDatabase() {

    abstract fun logEntryDao(): LogEntryDao

    companion object {
        @Volatile private var instance: PresenceDatabase? = null

        fun getInstance(context: Context): PresenceDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PresenceDatabase::class.java,
                    "presence.db",
                ).build().also { instance = it }
            }
    }
}
