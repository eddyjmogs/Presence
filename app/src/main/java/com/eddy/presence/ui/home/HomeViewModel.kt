package com.eddy.presence.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eddy.presence.PresenceApplication
import com.eddy.presence.data.model.LogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class HomeUiState(
    val taskText: String = "",
    val focusModeExpanded: Boolean = false,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as PresenceApplication).logRepository

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val todayEntries: StateFlow<List<LogEntry>> = repository
        .getEntriesForToday()
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
}
