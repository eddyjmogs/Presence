package com.eddy.presence.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.eddy.presence.data.model.LogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface LogEntryDao {

    @Insert
    suspend fun insert(entry: LogEntry)

    @Query("SELECT * FROM log_entries WHERE timestamp >= :startOfDay ORDER BY timestamp DESC LIMIT 20")
    fun getEntriesForDay(startOfDay: Long): Flow<List<LogEntry>>

    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC LIMIT 200")
    fun getAllEntries(): Flow<List<LogEntry>>
}
