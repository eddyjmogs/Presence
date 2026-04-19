package com.eddy.presence.ui.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eddy.presence.intervalToMs
import com.eddy.presence.state.SessionStateStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class SessionUiState(
    val didText: String = "",
    val nextFocusText: String = "",
    val notes: String = "",
    val timerStartTime: Long = 0L,
    val intervalMinutes: Int = 0,
    val timerExpired: Boolean = false,
)

class DeepWorkSessionViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SessionStateStore(application)

    private val _uiState = MutableStateFlow(
        SessionUiState(
            didText = store.currentDidText,
            nextFocusText = store.currentNextFocusText,
            notes = store.currentNotes,
            timerStartTime = store.timerStartTime,
            intervalMinutes = store.intervalMinutes,
            timerExpired = store.timerExpired,
        )
    )
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private fun tickerFlow() = flow<Unit> {
        while (true) { emit(Unit); delay(1_000) }
    }

    val countdownSeconds: StateFlow<Long> = combine(_uiState, tickerFlow()) { state, _ ->
        if (state.timerStartTime == 0L) -1L
        else {
            val endTime = state.timerStartTime + intervalToMs(state.intervalMinutes)
            maxOf(0L, (endTime - System.currentTimeMillis()) / 1_000L)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), -1L)

    fun refresh() {
        _uiState.update {
            it.copy(
                didText = store.currentDidText,
                nextFocusText = store.currentNextFocusText,
                notes = store.currentNotes,
                timerStartTime = store.timerStartTime,
                intervalMinutes = store.intervalMinutes,
                timerExpired = store.timerExpired,
            )
        }
    }

    fun onDidTextChange(text: String) {
        store.currentDidText = text
        _uiState.update { it.copy(didText = text) }
    }

    fun onNextFocusChange(text: String) {
        store.currentNextFocusText = text
        _uiState.update { it.copy(nextFocusText = text) }
    }

    fun onNotesChange(text: String) {
        store.currentNotes = text
        _uiState.update { it.copy(notes = text) }
    }
}
