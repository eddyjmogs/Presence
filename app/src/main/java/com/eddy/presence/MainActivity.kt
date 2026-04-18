package com.eddy.presence

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import com.eddy.presence.service.PresenceForegroundService
import com.eddy.presence.state.SessionStateStore
import com.eddy.presence.ui.home.HomeScreen
import com.eddy.presence.ui.home.HomeViewModel
import com.eddy.presence.ui.settings.SettingsActivity
import com.eddy.presence.ui.theme.PresenceTheme

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()

    private var showOnboarding by mutableStateOf(false)
    private var showOverlayPermissionDialog by mutableStateOf(false)
    private var showAccessibilityDialog by mutableStateOf(false)
    private var showExactAlarmDialog by mutableStateOf(false)
    private var pendingTask by mutableStateOf<String?>(null)
    private var pendingFocusContext by mutableStateOf<String?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingTask?.let { launchDeepWork(it) }
        pendingTask = null
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
                        onStartDeepWork = { task -> startDeepWork(task) },
                        onStartFocusMode = { context -> startFocusMode(context) },
                        onStopSession = { stopCurrentSession() },
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

                if (showAccessibilityDialog) {
                    PermissionExplanationDialog(
                        title = "Accessibility Service Required",
                        body = "Presence needs the Accessibility Service to detect when you switch apps during Focus Mode. Please enable 'Presence' in Accessibility Settings.",
                        onConfirm = {
                            showAccessibilityDialog = false
                            val ctx = pendingFocusContext
                            pendingFocusContext = null
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            // If they come back and it's enabled, they can tap Start again
                            ctx?.let { homeViewModel.collapseFocusMode() }
                        },
                        onDismiss = {
                            showAccessibilityDialog = false
                            pendingFocusContext = null
                        },
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
    }

    private fun startDeepWork(task: String) {
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
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingTask = task
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        launchDeepWork(task)
    }

    private fun launchDeepWork(task: String) {
        PresenceForegroundService.startDeepWork(this, task)
        homeViewModel.refreshSessionState(SessionStateStore(this).also { it.deepWorkActive = true; it.currentTask = task })
    }

    private fun startFocusMode(contextName: String) {
        if (!isAccessibilityEnabled()) {
            pendingFocusContext = contextName
            showAccessibilityDialog = true
            return
        }
        PresenceForegroundService.startFocusMode(this, contextName)
        homeViewModel.collapseFocusMode()
        homeViewModel.refreshSessionState(SessionStateStore(this).also {
            it.focusModeActive = true
            it.focusModeContext = contextName
        })
    }

    private fun stopCurrentSession() {
        val store = SessionStateStore(this)
        when {
            store.deepWorkActive -> {
                store.clearSession()
                PresenceForegroundService.stop(this)
            }
            store.focusModeActive -> {
                store.focusModeActive = false
                store.focusModeAllowedPackage = ""
                PresenceForegroundService.stopFocusMode(this)
            }
        }
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

    private fun isAccessibilityEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return flat.contains("$packageName/.service.PresenceAccessibilityService", ignoreCase = true)
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
                "• Deep Work — mandatory check-in overlays on a timer\n" +
                "• Focus Mode — soft reminders when you open off-whitelist apps\n\n" +
                "You'll be asked to grant a few permissions:\n" +
                "Overlay, Accessibility, Exact Alarms, and Battery Optimization.\n\n" +
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
