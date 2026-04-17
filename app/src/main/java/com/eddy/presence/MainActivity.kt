package com.eddy.presence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.eddy.presence.ui.home.HomeScreen
import com.eddy.presence.ui.theme.PresenceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PresenceTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(
                        onStartDeepWork = { /* Step 2: wire ForegroundService */ },
                        onStartFocusMode = { /* Step 10: wire Focus Mode */ },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
