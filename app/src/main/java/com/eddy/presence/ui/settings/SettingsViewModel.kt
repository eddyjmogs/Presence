package com.eddy.presence.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    val notifyAlarm: Boolean = true,
    val notifyVibration: Boolean = false,
    val notifyFlashlight: Boolean = false,
    val notifySilent: Boolean = false,
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
            notifyAlarm = store.notifyAlarm,
            notifyVibration = store.notifyVibration,
            notifyFlashlight = store.notifyFlashlight,
            notifySilent = store.notifySilent,
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

    fun setNotifyAlarm(enabled: Boolean) {
        store.notifyAlarm = enabled
        _uiState.update { it.copy(notifyAlarm = enabled) }
    }

    fun setNotifyVibration(enabled: Boolean) {
        store.notifyVibration = enabled
        _uiState.update { it.copy(notifyVibration = enabled) }
    }

    fun setNotifyFlashlight(enabled: Boolean) {
        store.notifyFlashlight = enabled
        _uiState.update { it.copy(notifyFlashlight = enabled) }
    }

    fun setNotifySilent(enabled: Boolean) {
        store.notifySilent = enabled
        _uiState.update { it.copy(notifySilent = enabled) }
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
