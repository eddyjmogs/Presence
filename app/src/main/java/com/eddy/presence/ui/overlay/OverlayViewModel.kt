package com.eddy.presence.ui.overlay

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class OverlayScenario {
    OnTime,   // picked up within interval
    OverTime, // picked up after interval expired
    Away,     // returned after idle with no active timer
}

data class OverlayUiState(
    val scenario: OverlayScenario = OverlayScenario.OnTime,
    val setMinutes: Int = 0,      // how long the interval was set for (0 = unknown on first check-in)
    val elapsedMinutes: Int = 0,  // how long it's actually been
    val didText: String = "",
    val nextFocusText: String = "",
    val intervalMinutes: Int = 25,
    val notifyAlarm: Boolean = true,
    val notifyVibration: Boolean = false,
    val notifyFlashlight: Boolean = false,
    val notifySilent: Boolean = false,
    val notes: String = "",
) {
    val canConfirm: Boolean get() = didText.isNotBlank() && nextFocusText.isNotBlank()

    val didLabel: String get() = when (scenario) {
        OverlayScenario.OverTime -> "What happened / what did you work on?"
        OverlayScenario.Away -> "What did you do during that time?"
        OverlayScenario.OnTime -> "What did you work on?"
    }
}

class OverlayViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OverlayUiState())
    val uiState: StateFlow<OverlayUiState> = _uiState.asStateFlow()

    fun init(scenario: OverlayScenario, setMinutes: Int, elapsedMinutes: Int) {
        _uiState.update {
            it.copy(
                scenario = scenario,
                setMinutes = setMinutes,
                elapsedMinutes = elapsedMinutes,
            )
        }
    }

    fun onDidTextChange(text: String) = _uiState.update { it.copy(didText = text) }
    fun onNextFocusChange(text: String) = _uiState.update { it.copy(nextFocusText = text) }
    fun onIntervalChange(minutes: Int) = _uiState.update { it.copy(intervalMinutes = minutes) }
    fun onNotesChange(text: String) = _uiState.update { it.copy(notes = text) }

    fun onNotifyAlarmToggle() = _uiState.update { it.copy(notifyAlarm = !it.notifyAlarm, notifySilent = false) }
    fun onNotifyVibrationToggle() = _uiState.update { it.copy(notifyVibration = !it.notifyVibration, notifySilent = false) }
    fun onNotifyFlashlightToggle() = _uiState.update { it.copy(notifyFlashlight = !it.notifyFlashlight, notifySilent = false) }
    fun onNotifySilentToggle() = _uiState.update {
        val nowSilent = !it.notifySilent
        // Silent is exclusive — checking it clears the others
        it.copy(
            notifySilent = nowSilent,
            notifyAlarm = if (nowSilent) false else it.notifyAlarm,
            notifyVibration = if (nowSilent) false else it.notifyVibration,
            notifyFlashlight = if (nowSilent) false else it.notifyFlashlight,
        )
    }
}
