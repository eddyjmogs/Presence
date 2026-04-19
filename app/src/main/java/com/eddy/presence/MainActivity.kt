package com.eddy.presence

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.eddy.presence.FocusRating
import com.eddy.presence.data.model.LogEntry
import com.eddy.presence.intervalToMs
import com.eddy.presence.service.PresenceForegroundService
import com.eddy.presence.state.SessionStateStore
import com.eddy.presence.ui.home.HomeScreen
import com.eddy.presence.ui.home.HomeViewModel
import com.eddy.presence.ui.overlay.DeepWorkOverlayActivity
import com.eddy.presence.ui.overlay.OverlayScenario
import com.eddy.presence.ui.session.DeepWorkSessionActivity
import com.eddy.presence.ui.settings.SettingsActivity
import com.eddy.presence.ui.theme.PresenceTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()

    private var showOnboarding by mutableStateOf(false)
    private var showOverlayPermissionDialog by mutableStateOf(false)
    private var showExactAlarmDialog by mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchDeepWork()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PresenceTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {},
                            actions = {
                                IconButton(onClick = { SettingsActivity.launch(this@MainActivity) }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                                }
                            },
                        )
                    },
                ) { innerPadding ->
                    HomeScreen(
                        onStartDeepWork = { startDeepWork() },
                        onStopSession = { rating -> stopCurrentSession(rating) },
                        onViewSession = { DeepWorkSessionActivity.launch(this@MainActivity) },
                        modifier = Modifier.padding(innerPadding),
                        viewModel = homeViewModel,
                    )
                }

                if (showOnboarding) {
                    OnboardingDialog(
                        onDismiss = {
                            showOnboarding = false
                            SessionStateStore(this@MainActivity).onboardingDone = true
                            requestBatteryOptimizationExemption()
                        }
                    )
                }

                if (showOverlayPermissionDialog) {
                    PermissionExplanationDialog(
                        title = "Overlay Permission Required",
                        body = "Presence needs 'Draw over other apps' to show the mandatory Deep Work check-in overlay. Please grant it in the next screen.",
                        onConfirm = {
                            showOverlayPermissionDialog = false
                            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")))
                        },
                        onDismiss = { showOverlayPermissionDialog = false },
                    )
                }

                if (showExactAlarmDialog) {
                    PermissionExplanationDialog(
                        title = "Exact Alarms Required",
                        body = "Presence uses exact alarms to fire your check-in timer at the precise time you set. Please grant this in the next screen.",
                        onConfirm = {
                            showExactAlarmDialog = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:$packageName")))
                            }
                        },
                        onDismiss = { showExactAlarmDialog = false },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val store = SessionStateStore(this)
        homeViewModel.refreshSessionState(store)
        if (!store.onboardingDone) showOnboarding = true
        maybeShowPendingOverlay(store)
    }

    private fun maybeShowPendingOverlay(store: SessionStateStore) {
        if (!store.deepWorkActive || store.timerStartTime == 0L) return
        val now = System.currentTimeMillis()
        val endMs = store.timerStartTime + intervalToMs(store.intervalMinutes)
        val alreadyExpired = store.pendingAcknowledgement
        val silentlyMissed = !alreadyExpired && now > endMs
        if (!alreadyExpired && !silentlyMissed) return
        if (silentlyMissed) {
            store.timerExpired = true
            store.pendingAcknowledgement = true
        }
        val elapsedMinutes = ((now - store.timerStartTime) / 60_000L).toInt().coerceAtLeast(1)
        DeepWorkOverlayActivity.launch(this, OverlayScenario.OverTime, store.intervalMinutes, elapsedMinutes)
    }

    private fun startDeepWork() {
        if (!Settings.canDrawOverlays(this)) {
            showOverlayPermissionDialog = true
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !(getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
        ) {
            showExactAlarmDialog = true
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        launchDeepWork()
    }

    private fun launchDeepWork() {
        PresenceForegroundService.startDeepWork(this)
        val now = System.currentTimeMillis()
        val store = SessionStateStore(this)
        store.deepWorkActive = true
        store.timerStartTime = now
        store.timerExpired = false
        store.pendingAcknowledgement = false
        homeViewModel.refreshSessionState(store)
        DeepWorkSessionActivity.launch(this)
    }

    private fun stopCurrentSession(rating: FocusRating) {
        val store = SessionStateStore(this)
        val now = System.currentTimeMillis()
        val didText = store.currentDidText
        val nextFocusText = store.currentNextFocusText
        val notes = store.currentNotes
        val intervalMinutes = store.intervalMinutes
        val sessionStartTime = store.timerStartTime
        val notifyAlarm = store.notifyAlarm
        val notifyVibration = store.notifyVibration
        val notifyFlashlight = store.notifyFlashlight
        val notifySilent = store.notifySilent
        if (didText.isNotBlank() || nextFocusText.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
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
                        notes = notes,
                        focusRating = rating.name,
                        sessionStartTime = sessionStartTime,
                    )
                )
            }
        }
        store.clearSession()
        PresenceForegroundService.stop(this)
        homeViewModel.refreshSessionState(store)
    }

    private fun requestBatteryOptimizationExemption() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            runCatching {
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")))
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun OnboardingDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { androidx.compose.material3.Text("Welcome to Presence") },
        text = {
            androidx.compose.material3.Text(
                "Presence keeps you intentional.\n\n" +
                "• Deep Work — mandatory check-in overlays on a timer\n\n" +
                "You'll be asked to grant a few permissions:\n" +
                "Overlay, Exact Alarms, and Battery Optimization.\n\n" +
                "Grant them in Settings → Permissions whenever you're ready."
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) { androidx.compose.material3.Text("Get Started") }
        },
    )
}

@androidx.compose.runtime.Composable
private fun PermissionExplanationDialog(
    title: String,
    body: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { androidx.compose.material3.Text(title) },
        text = { androidx.compose.material3.Text(body) },
        confirmButton = {
            Button(onClick = onConfirm) { androidx.compose.material3.Text("Open Settings") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { androidx.compose.material3.Text("Cancel") }
        },
    )
}
