package com.eddy.presence.data.repository

import com.eddy.presence.data.db.LogEntryDao
import com.eddy.presence.data.model.LogEntry
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class LogRepository(private val dao: LogEntryDao) {

    fun getEntriesForToday(): Flow<List<LogEntry>> {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return dao.getEntriesForDay(startOfDay)
    }

    fun getAllEntries(): Flow<List<LogEntry>> = dao.getAllEntries()

    suspend fun insert(entry: LogEntry) = dao.insert(entry)
}
