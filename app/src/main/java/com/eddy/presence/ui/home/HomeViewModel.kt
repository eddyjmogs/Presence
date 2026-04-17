package com.eddy.presence.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LogEntry(
    val time: String,
    val summary: String,
    val mode: LogMode,
)

enum class LogMode { DEEP_WORK, FOCUS }

data class HomeUiState(
    val taskText: String = "",
    val focusModeExpanded: Boolean = false,
    val logEntries: List<LogEntry> = emptyList(),
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

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
