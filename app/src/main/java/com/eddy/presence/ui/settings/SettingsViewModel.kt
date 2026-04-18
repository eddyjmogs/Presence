package com.eddy.presence.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eddy.presence.NotifyType
import com.eddy.presence.PresenceApplication
import com.eddy.presence.state.SessionStateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val customContextNames: List<String> = emptyList(),
    val defaultIntervalMinutes: Int = 25,
    val notifyType: NotifyType = NotifyType.Silent,
    val showCreateContextDialog: Boolean = false,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PresenceApplication
    private val contextRepo = app.focusContextRepository
    private val whitelistRepo = app.whitelistRepository
    private val store = SessionStateStore(application)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            defaultIntervalMinutes = store.intervalMinutes,
            notifyType = store.notifyType,
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val customContextNames: StateFlow<List<String>> = contextRepo.getAll()
        .map { list -> list.map { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setDefaultInterval(minutes: Int) {
        store.intervalMinutes = minutes
        _uiState.update { it.copy(defaultIntervalMinutes = minutes) }
    }

    fun setNotifyType(type: NotifyType) {
        store.notifyType = type
        _uiState.update { it.copy(notifyType = type) }
    }

    fun showCreateContextDialog() = _uiState.update { it.copy(showCreateContextDialog = true) }
    fun dismissCreateContextDialog() = _uiState.update { it.copy(showCreateContextDialog = false) }

    fun createContext(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) { contextRepo.create(trimmed) }
        dismissCreateContextDialog()
    }

    fun deleteContext(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            whitelistRepo.deleteAllForContext(name)
            contextRepo.delete(name)
        }
    }
}
