package com.eddy.presence.ui.overlay

import androidx.lifecycle.ViewModel
import com.eddy.presence.FocusRating
import com.eddy.presence.NotifyType
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
    val setMinutes: Int = 0,
    val elapsedMinutes: Int = 0,
    val didText: String = "",
    val nextFocusText: String = "",
    val intervalMinutes: Int = 25,
    val notifyType: NotifyType = NotifyType.Silent,
    val notes: String = "",
    val focusRating: FocusRating? = null,
) {
    val canConfirm: Boolean get() = didText.isNotBlank() && nextFocusText.isNotBlank() && focusRating != null

    val didLabel: String get() = when (scenario) {
        OverlayScenario.OverTime -> "What happened / what did you work on?"
        OverlayScenario.Away -> "What did you do during that time?"
        OverlayScenario.OnTime -> "What did you work on?"
    }

    // Kept for onConfirm store writes and LogEntry insert
    val notifyAlarm get() = notifyType == NotifyType.Alarm
    val notifyVibration get() = notifyType == NotifyType.Vibration
    val notifyFlashlight get() = notifyType == NotifyType.Flashlight
    val notifySilent get() = notifyType == NotifyType.Silent
}

class OverlayViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OverlayUiState())
    val uiState: StateFlow<OverlayUiState> = _uiState.asStateFlow()

    fun init(
        scenario: OverlayScenario,
        setMinutes: Int,
        elapsedMinutes: Int,
        notifyType: NotifyType,
        didText: String = "",
        nextFocusText: String = "",
        notes: String = "",
    ) {
        _uiState.update {
            it.copy(
                scenario = scenario,
                setMinutes = setMinutes,
                elapsedMinutes = elapsedMinutes,
                notifyType = notifyType,
                didText = didText,
                nextFocusText = nextFocusText,
                notes = notes,
            )
        }
    }

    fun onDidTextChange(text: String) = _uiState.update { it.copy(didText = text) }
    fun onNextFocusChange(text: String) = _uiState.update { it.copy(nextFocusText = text) }
    fun onIntervalChange(minutes: Int) = _uiState.update { it.copy(intervalMinutes = minutes) }
    fun onNotesChange(text: String) = _uiState.update { it.copy(notes = text) }
    fun onNotifyTypeChange(type: NotifyType) = _uiState.update { it.copy(notifyType = type) }
    fun onFocusRatingChange(rating: FocusRating) = _uiState.update { it.copy(focusRating = rating) }
}
