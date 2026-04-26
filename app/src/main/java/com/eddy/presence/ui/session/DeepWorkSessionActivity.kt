package com.eddy.presence.ui.session

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eddy.presence.FocusRating
import com.eddy.presence.PresenceApplication
import com.eddy.presence.alarm.AlarmScheduler
import com.eddy.presence.data.model.LogEntry
import com.eddy.presence.intervalLabel
import com.eddy.presence.intervalToMs
import com.eddy.presence.service.PresenceForegroundService
import com.eddy.presence.state.SessionStateStore
import com.eddy.presence.ui.overlay.DeepWorkOverlayActivity
import com.eddy.presence.ui.overlay.OverlayScenario
import com.eddy.presence.ui.theme.PresenceTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeepWorkSessionActivity : ComponentActivity() {

    private val viewModel: DeepWorkSessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PresenceTheme {
                val uiState by viewModel.uiState.collectAsState()
                val countdownSeconds by viewModel.countdownSeconds.collectAsState()
                SessionScreen(
                    uiState = uiState,
                    countdownSeconds = countdownSeconds,
                    onBack = ::finish,
                    onDidTextChange = viewModel::onDidTextChange,
                    onNextFocusChange = viewModel::onNextFocusChange,
                    onStop = { rating ->
                        val store = SessionStateStore(this)
                        val now = System.currentTimeMillis()
                        val didText = store.currentDidText
                        val nextFocusText = store.currentNextFocusText
                        val intervalMinutes = store.intervalMinutes
                        val sessionStartTime = store.timerStartTime
                        val notifyAlarm = store.notifyAlarm
                        val notifyVibration = store.notifyVibration
                        val notifyFlashlight = store.notifyFlashlight
                        val notifySilent = store.notifySilent
                        if (didText.isNotBlank() || nextFocusText.isNotBlank()) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                (application as PresenceApplication).logRepository.insert(
                                    LogEntry(
                                        timestamp = now,
                                        mode = "DEEP_WORK",
                                        scenario = "Stopped",
                                        taskName = "",
                                        didText = didText,
                                        nextFocusText = nextFocusText,
                                        intervalMinutes = intervalMinutes,
                                        notifyAlarm = notifyAlarm,
                                        notifyVibration = notifyVibration,
                                        notifyFlashlight = notifyFlashlight,
                                        notifySilent = notifySilent,
                                        focusRating = rating.name,
                                        sessionStartTime = sessionStartTime,
                                    )
                                )
                            }
                        }
                        store.clearSession()
                        AlarmScheduler.cancel(this)
                        PresenceForegroundService.stop(this)
                        finish()
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val store = SessionStateStore(this)
        if (!store.deepWorkActive) {
            finish()
            return
        }
        val now = System.currentTimeMillis()
        val endMs = store.timerStartTime + intervalToMs(store.intervalMinutes)
        val alreadyExpired = store.pendingAcknowledgement
        val silentlyMissed = !alreadyExpired && store.timerStartTime > 0L && now > endMs
        if (alreadyExpired || silentlyMissed) {
            if (silentlyMissed) {
                store.timerExpired = true
                store.pendingAcknowledgement = true
            }
            val elapsedMinutes = ((now - store.timerStartTime) / 60_000L).toInt().coerceAtLeast(1)
            DeepWorkOverlayActivity.launch(this, OverlayScenario.OverTime, store.intervalMinutes, elapsedMinutes)
            return
        }
        viewModel.refresh()
    }

    companion object {
        fun launch(context: Context) {
            context.startActivity(
                Intent(context, DeepWorkSessionActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SessionScreen(
    uiState: SessionUiState,
    countdownSeconds: Long,
    onBack: () -> Unit,
    onDidTextChange: (String) -> Unit,
    onNextFocusChange: (String) -> Unit,
    onStop: (FocusRating) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deep Work") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            val isPending = uiState.timerExpired || countdownSeconds == 0L
            val countdownText = when {
                countdownSeconds < 0L -> ""
                isPending -> "Check-in pending"
                else -> {
                    val m = countdownSeconds / 60
                    val s = countdownSeconds % 60
                    "Next check-in in %d:%02d".format(m, s)
                }
            }
            if (countdownText.isNotBlank()) {
                Text(
                    text = countdownText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isPending) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                )
            }
            if (uiState.intervalMinutes != 0) {
                Text(
                    text = "Interval: ${intervalLabel(uiState.intervalMinutes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = uiState.didText,
                onValueChange = onDidTextChange,
                label = { Text("What are you working on?") },
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

            Spacer(modifier = Modifier.height(40.dp))

            var showConfirm by remember { mutableStateOf(false) }
            var selectedRating by remember { mutableStateOf<FocusRating?>(null) }

            Button(
                onClick = { showConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Text("Stop Session")
            }

            if (showConfirm) {
                AlertDialog(
                    onDismissRequest = { showConfirm = false; selectedRating = null },
                    title = { Text("End session?") },
                    text = {
                        Column {
                            Text("How focused were you?")
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FocusRating.entries.forEach { rating ->
                                    FilterChip(
                                        selected = selectedRating == rating,
                                        onClick = { selectedRating = rating },
                                        label = { Text(rating.name) },
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showConfirm = false; onStop(selectedRating!!) },
                            enabled = selectedRating != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        ) { Text("End Session") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirm = false; selectedRating = null }) { Text("Cancel") }
                    },
                )
            }
        }
    }
}
