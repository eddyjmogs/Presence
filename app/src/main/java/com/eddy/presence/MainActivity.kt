package com.eddy.presence

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.eddy.presence.service.PresenceForegroundService
import com.eddy.presence.ui.home.HomeScreen
import com.eddy.presence.ui.settings.SettingsActivity
import com.eddy.presence.ui.theme.PresenceTheme

class MainActivity : ComponentActivity() {

    private var pendingTask by mutableStateOf<String?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingTask?.let { task ->
                PresenceForegroundService.startDeepWork(this, task)
            }
        }
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
                        onStartFocusMode = { /* Step 10: wire Focus Mode */ },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    private fun startDeepWork(task: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingTask = task
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            PresenceForegroundService.startDeepWork(this, task)
        }
    }
}
