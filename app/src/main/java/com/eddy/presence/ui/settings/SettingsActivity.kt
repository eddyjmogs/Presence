package com.eddy.presence.ui.settings

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eddy.presence.ui.theme.PresenceTheme
import com.eddy.presence.ui.whitelist.WhitelistManagerActivity

class SettingsActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PresenceTheme {
                val uiState by viewModel.uiState.collectAsState()
                val customContextNames by viewModel.customContextNames.collectAsState()
                SettingsScreen(
                    uiState = uiState.copy(customContextNames = customContextNames),
                    onBack = ::finish,
                    onSetInterval = viewModel::setDefaultInterval,
                    onSetNotifyAlarm = viewModel::setNotifyAlarm,
                    onSetNotifyVibration = viewModel::setNotifyVibration,
                    onSetNotifyFlashlight = viewModel::setNotifyFlashlight,
                    onSetNotifySilent = viewModel::setNotifySilent,
                    onAddContext = viewModel::showCreateContextDialog,
                    onDeleteContext = viewModel::deleteContext,
                    onCreateContext = viewModel::createContext,
                    onDismissCreateDialog = viewModel::dismissCreateContextDialog,
                )
            }
        }
    }

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, SettingsActivity::class.java))
        }
    }
}

private val INTERVAL_PRESETS = listOf(10, 15, 25, 30, 45, 60, 90)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onSetInterval: (Int) -> Unit,
    onSetNotifyAlarm: (Boolean) -> Unit,
    onSetNotifyVibration: (Boolean) -> Unit,
    onSetNotifyFlashlight: (Boolean) -> Unit,
    onSetNotifySilent: (Boolean) -> Unit,
    onAddContext: () -> Unit,
    onDeleteContext: (String) -> Unit,
    onCreateContext: (String) -> Unit,
    onDismissCreateDialog: () -> Unit,
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(innerPadding),
        ) {
            // ── Focus Mode Contexts ────────────────────────────────────────
            SectionHeader("Focus Mode Contexts")

            BuiltInContextRow(label = "🚇 Commute", contextName = "Commute", context = context)
            BuiltInContextRow(label = "🚽 Bathroom", contextName = "Bathroom", context = context)

            uiState.customContextNames.forEach { name ->
                CustomContextRow(
                    name = name,
                    onManage = { WhitelistManagerActivity.launch(context, name) },
                    onDelete = { onDeleteContext(name) },
                )
            }

            TextButton(
                onClick = onAddContext,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text("+ Add Context")
            }

            SectionDivider()

            // ── Deep Work Defaults ─────────────────────────────────────────
            SectionHeader("Deep Work Defaults")

            Text(
                text = "Default interval",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                INTERVAL_PRESETS.forEach { minutes ->
                    FilterChip(
                        selected = uiState.defaultIntervalMinutes == minutes,
                        onClick = { onSetInterval(minutes) },
                        label = { Text("${minutes}m") },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Default notification type",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            CheckRow("Alarm", uiState.notifyAlarm, onSetNotifyAlarm)
            CheckRow("Vibration", uiState.notifyVibration, onSetNotifyVibration)
            CheckRow("Flashlight", uiState.notifyFlashlight, onSetNotifyFlashlight)
            CheckRow("Silent", uiState.notifySilent, onSetNotifySilent)

            SectionDivider()

            // ── Permissions ────────────────────────────────────────────────
            SectionHeader("Permissions")
            PermissionsSection(context)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (uiState.showCreateContextDialog) {
        CreateContextDialog(onConfirm = onCreateContext, onDismiss = onDismissCreateDialog)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun SectionDivider() {
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider()
}

@Composable
private fun BuiltInContextRow(label: String, contextName: String, context: Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = { WhitelistManagerActivity.launch(context, contextName) }) {
            Text("Whitelist")
        }
    }
}

@Composable
private fun CustomContextRow(name: String, onManage: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = onManage) { Text("Whitelist") }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete $name",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PermissionsSection(context: Context) {
    val packageName = context.packageName

    val hasOverlay = Settings.canDrawOverlays(context)
    val hasAccessibility = isAccessibilityEnabled(context)
    val hasExactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    } else true
    val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    } else true
    val hasBatteryExempt = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
        .isIgnoringBatteryOptimizations(packageName)

    PermissionRow(
        label = "Draw over other apps",
        granted = hasOverlay,
        onGrant = {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        },
    )
    PermissionRow(
        label = "Accessibility service",
        granted = hasAccessibility,
        onGrant = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
    )
    PermissionRow(
        label = "Exact alarms",
        granted = hasExactAlarm,
        onGrant = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
                )
            }
        },
    )
    PermissionRow(
        label = "Notifications",
        granted = hasNotifications,
        onGrant = {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            )
        },
    )
    PermissionRow(
        label = "Battery optimization exempt",
        granted = hasBatteryExempt,
        onGrant = {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
            )
        },
    )
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false
    return flat.contains("${context.packageName}/.service.PresenceAccessibilityService", ignoreCase = true)
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onGrant: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (granted) "Granted" else "Not granted",
                style = MaterialTheme.typography.bodySmall,
                color = if (granted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
            )
        }
        if (!granted) {
            OutlinedButton(onClick = onGrant) { Text("Grant") }
        }
    }
}

@Composable
private fun CreateContextDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Focus Context") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Context name") },
                placeholder = { Text("e.g. Reading, Gym...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
