package com.eddy.presence.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eddy.presence.PresenceApplication
import com.eddy.presence.intervalToMs
import com.eddy.presence.data.model.LogEntry
import com.eddy.presence.state.SessionStateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActiveSession(
    val deepWorkActive: Boolean = false,
    val focusModeActive: Boolean = false,
    val focusModeContext: String = "",
    val currentTask: String = "",
    val timerStartTime: Long = 0L,
    val intervalMinutes: Int = 0,
    val timerExpired: Boolean = false,
) {
    val isActive get() = deepWorkActive || focusModeActive
}

data class HomeUiState(
    val taskText: String = "",
    val focusModeExpanded: Boolean = false,
    val showCreateContextDialog: Boolean = false,
    val session: ActiveSession = ActiveSession(),
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PresenceApplication
    private val logRepo = app.logRepository
    private val contextRepo = app.focusContextRepository

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val todayEntries: StateFlow<List<LogEntry>> = logRepo
        .getEntriesForToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val customContextNames: StateFlow<List<String>> = contextRepo
        .getAll()
        .map { list -> list.map { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun tickerFlow(periodMs: Long) = flow<Unit> {
        while (true) { emit(Unit); delay(periodMs) }
    }

    val countdownSeconds: StateFlow<Long> = combine(_uiState, tickerFlow(1_000)) { state, _ ->
        val s = state.session
        if (!s.deepWorkActive || s.timerStartTime == 0L) -1L
        else {
            val endTime = s.timerStartTime + intervalToMs(s.intervalMinutes)
            maxOf(0L, (endTime - System.currentTimeMillis()) / 1_000L)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), -1L)

    fun refreshSessionState(store: SessionStateStore) {
        _uiState.update {
            it.copy(
                session = ActiveSession(
                    deepWorkActive = store.deepWorkActive,
                    focusModeActive = store.focusModeActive,
                    focusModeContext = store.focusModeContext,
                    currentTask = store.currentTask,
                    timerStartTime = store.timerStartTime,
                    intervalMinutes = store.intervalMinutes,
                    timerExpired = store.timerExpired,
                )
            )
        }
    }

    fun onTaskTextChange(text: String) = _uiState.update { it.copy(taskText = text) }

    fun toggleFocusModeExpanded() = _uiState.update { it.copy(focusModeExpanded = !it.focusModeExpanded) }

    fun collapseFocusMode() = _uiState.update { it.copy(focusModeExpanded = false) }

    fun showCreateContextDialog() = _uiState.update { it.copy(showCreateContextDialog = true) }

    fun dismissCreateContextDialog() = _uiState.update { it.copy(showCreateContextDialog = false) }

    fun createContext(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) { contextRepo.create(trimmed) }
        dismissCreateContextDialog()
    }
}
