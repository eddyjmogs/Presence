package com.eddy.presence.ui.overlay

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.eddy.presence.FocusRating
import com.eddy.presence.NotifyType
import com.eddy.presence.PresenceApplication
import com.eddy.presence.intervalLabel
import com.eddy.presence.intervalToMs
import com.eddy.presence.alarm.AlarmScheduler
import com.eddy.presence.data.model.LogEntry
import com.eddy.presence.service.PresenceForegroundService
import com.eddy.presence.state.SessionStateStore
import com.eddy.presence.ui.theme.PresenceTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeepWorkOverlayActivity : ComponentActivity() {

    private val viewModel: OverlayViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen and turn screen on when launched
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val scenario = intent.getStringExtra(EXTRA_SCENARIO)
            ?.let { OverlayScenario.valueOf(it) }
            ?: OverlayScenario.OnTime
        val setMinutes = intent.getIntExtra(EXTRA_SET_MINUTES, 0)
        val elapsedMinutes = intent.getIntExtra(EXTRA_ELAPSED_MINUTES, 0)
        val store = com.eddy.presence.state.SessionStateStore(this)
        viewModel.init(
            scenario = scenario,
            setMinutes = setMinutes,
            elapsedMinutes = elapsedMinutes,
            notifyType = store.notifyType,
            didText = store.currentDidText,
            nextFocusText = store.currentNextFocusText,
        )

        setContent {
            PresenceTheme {
                val uiState by viewModel.uiState.collectAsState()
                OverlayScreen(
                    uiState = uiState,
                    onDidTextChange = viewModel::onDidTextChange,
                    onNextFocusChange = viewModel::onNextFocusChange,
                    onIntervalChange = viewModel::onIntervalChange,
                    onNotifyTypeChange = viewModel::onNotifyTypeChange,
                    onFocusRatingChange = viewModel::onFocusRatingChange,
                    onConfirm = {
                        val state = viewModel.uiState.value
                        val now = System.currentTimeMillis()
                        val store = SessionStateStore(this@DeepWorkOverlayActivity)
                        val intervalStartTime = store.timerStartTime
                        store.intervalMinutes = state.intervalMinutes
                        store.timerStartTime = now
                        store.notifyAlarm = state.notifyAlarm
                        store.notifyVibration = state.notifyVibration
                        store.notifyFlashlight = state.notifyFlashlight
                        store.notifySilent = state.notifySilent
                        store.timerExpired = false
                        store.pendingAcknowledgement = false
                        store.currentDidText = ""
                        store.currentNextFocusText = ""
                        // Always stop alarm sound, vibration, and torch regardless of
                        // which notification type was active — torch in particular must
                        // never be left on.
                        PresenceForegroundService.stopAlarm(this@DeepWorkOverlayActivity)
                        PresenceForegroundService.updateDeepWorkNotification(this@DeepWorkOverlayActivity)
                        AlarmScheduler.schedule(
                            this@DeepWorkOverlayActivity,
                            now + intervalToMs(state.intervalMinutes),
                        )
                        lifecycleScope.launch(Dispatchers.IO) {
                            val repo = (application as PresenceApplication).logRepository
                            repo.insert(LogEntry(
                                timestamp = now,
                                mode = "DEEP_WORK",
                                scenario = state.scenario.name,
                                taskName = "",
                                didText = state.didText,
                                nextFocusText = state.nextFocusText,
                                intervalMinutes = state.intervalMinutes,
                                notifyAlarm = state.notifyAlarm,
                                notifyVibration = state.notifyVibration,
                                notifyFlashlight = state.notifyFlashlight,
                                notifySilent = state.notifySilent,
                                focusRating = state.focusRating?.name ?: "",
                                sessionStartTime = intervalStartTime,
                            ))
                        }
                        finish()
                    },
                )
            }
        }
    }

    // Prevent back-press dismissal — user must fill required fields and tap Confirm
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() = Unit

    // Fires when the user presses Home or switches apps. Immediately relaunch so
    // the overlay stays on top until the check-in is confirmed.
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (SessionStateStore(this).pendingAcknowledgement) {
            startActivity(
                Intent(this, DeepWorkOverlayActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            )
        }
    }

    companion object {
        private const val EXTRA_SCENARIO = "extra_scenario"
        private const val EXTRA_SET_MINUTES = "extra_set_minutes"
        private const val EXTRA_ELAPSED_MINUTES = "extra_elapsed_minutes"

        fun buildIntent(
            context: Context,
            scenario: OverlayScenario = OverlayScenario.OnTime,
            setMinutes: Int = 0,
            elapsedMinutes: Int = 0,
        ): Intent = Intent(context, DeepWorkOverlayActivity::class.java).apply {
            putExtra(EXTRA_SCENARIO, scenario.name)
            putExtra(EXTRA_SET_MINUTES, setMinutes)
            putExtra(EXTRA_ELAPSED_MINUTES, elapsedMinutes)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }

        fun launch(
            context: Context,
            scenario: OverlayScenario = OverlayScenario.OnTime,
            setMinutes: Int = 0,
            elapsedMinutes: Int = 0,
        ) {
            context.startActivity(buildIntent(context, scenario, setMinutes, elapsedMinutes))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OverlayScreen(
    uiState: OverlayUiState,
    onDidTextChange: (String) -> Unit,
    onNextFocusChange: (String) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onNotifyTypeChange: (NotifyType) -> Unit,
    onFocusRatingChange: (FocusRating) -> Unit,
    onConfirm: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
        ) {
            ScenarioHeading(uiState)

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = uiState.didText,
                onValueChange = onDidTextChange,
                label = { Text(uiState.didLabel) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.nextFocusText,
                onValueChange = onNextFocusChange,
                label = { Text("What's your focus for the next interval?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Next check-in in:",
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            IntervalPicker(
                selectedMinutes = uiState.intervalMinutes,
                onSelect = onIntervalChange,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Notify me via (pick any):",
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            NotificationTypeRow(
                selected = uiState.notifyType,
                onSelect = onNotifyTypeChange,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "How focused were you?",
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FocusRatingRow(
                selected = uiState.focusRating,
                onSelect = onFocusRatingChange,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onConfirm,
                enabled = uiState.canConfirm,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Confirm")
            }
        }
    }
}

@Composable
private fun ScenarioHeading(uiState: OverlayUiState) {
    when (uiState.scenario) {
        OverlayScenario.OnTime -> {
            Text(
                text = "Check in",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        OverlayScenario.OverTime -> {
            Text(
                text = "You set ${formatDuration(uiState.setMinutes)} — it's been ${formatDuration(uiState.elapsedMinutes)}.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
        }
        OverlayScenario.Away -> {
            Text(
                text = "You were away for ${formatDuration(uiState.elapsedMinutes)}.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun formatDuration(minutes: Int): String {
    if (minutes < 60) return "$minutes min"
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (remainder == 0) "$hours hr" else "$hours hr $remainder min"
}

@Composable
private fun FocusRatingRow(
    selected: FocusRating?,
    onSelect: (FocusRating) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FocusRating.entries.forEach { rating ->
            FilterChip(
                selected = selected == rating,
                onClick = { onSelect(rating) },
                label = { Text(rating.name) },
            )
        }
    }
}

private val INTERVAL_PRESETS = listOf(-10, 1, 2, 3, 5, 7, 10, 15, 25, 30, 45, 60, 90)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntervalPicker(
    selectedMinutes: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        INTERVAL_PRESETS.forEach { minutes ->
            FilterChip(
                selected = selectedMinutes == minutes,
                onClick = { onSelect(minutes) },
                label = { Text(intervalLabel(minutes)) },
            )
        }
    }
}

@Composable
private fun NotificationTypeRow(
    selected: NotifyType,
    onSelect: (NotifyType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        NotifyType.entries.forEach { type ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                RadioButton(
                    selected = selected == type,
                    onClick = { onSelect(type) },
                )
                Text(type.name, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
