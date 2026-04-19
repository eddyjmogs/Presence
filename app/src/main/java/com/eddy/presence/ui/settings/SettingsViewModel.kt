package com.eddy.presence.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.eddy.presence.NotifyType
import com.eddy.presence.state.SessionStateStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val defaultIntervalMinutes: Int = 25,
    val notifyType: NotifyType = NotifyType.Silent,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SessionStateStore(application)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            defaultIntervalMinutes = store.intervalMinutes,
            notifyType = store.notifyType,
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setDefaultInterval(minutes: Int) {
        store.intervalMinutes = minutes
        _uiState.update { it.copy(defaultIntervalMinutes = minutes) }
    }

    fun setNotifyType(type: NotifyType) {
        store.notifyType = type
        _uiState.update { it.copy(notifyType = type) }
    }
}
