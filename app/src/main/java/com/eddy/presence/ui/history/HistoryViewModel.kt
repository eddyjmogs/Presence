package com.eddy.presence.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eddy.presence.PresenceApplication
import com.eddy.presence.data.model.LogEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayGroup(val label: String, val entries: List<LogEntry>)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as PresenceApplication).logRepository

    val history: StateFlow<List<DayGroup>> = repo.getAllEntries()
        .map { entries -> groupByDay(entries) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun groupByDay(entries: List<LogEntry>): List<DayGroup> {
        val today = startOfDay(System.currentTimeMillis())
        val yesterday = today - 86_400_000L
        val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

        return entries
            .groupBy { startOfDay(it.timestamp) }
            .entries
            .sortedByDescending { it.key }
            .map { (dayStart, dayEntries) ->
                val label = when (dayStart) {
                    today -> "Today"
                    yesterday -> "Yesterday"
                    else -> dateFormat.format(Date(dayStart))
                }
                DayGroup(label = label, entries = dayEntries)
            }
    }

    private fun startOfDay(timestamp: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
