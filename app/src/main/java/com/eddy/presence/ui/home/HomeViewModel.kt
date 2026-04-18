package com.eddy.presence.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eddy.presence.PresenceApplication
import com.eddy.presence.data.model.LogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val taskText: String = "",
    val focusModeExpanded: Boolean = false,
    val showCreateContextDialog: Boolean = false,
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

    fun onTaskTextChange(text: String) {
        _uiState.update { it.copy(taskText = text) }
    }

    fun toggleFocusModeExpanded() {
        _uiState.update { it.copy(focusModeExpanded = !it.focusModeExpanded) }
    }

    fun collapseFocusMode() {
        _uiState.update { it.copy(focusModeExpanded = false) }
    }

    fun showCreateContextDialog() {
        _uiState.update { it.copy(showCreateContextDialog = true) }
    }

    fun dismissCreateContextDialog() {
        _uiState.update { it.copy(showCreateContextDialog = false) }
    }

    fun createContext(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            contextRepo.create(trimmed)
        }
        dismissCreateContextDialog()
    }
}
