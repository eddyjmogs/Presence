package com.eddy.presence.ui.reminder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eddy.presence.state.SessionStateStore
import com.eddy.presence.ui.theme.PresenceTheme

class FocusSoftReminderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contextName = intent.getStringExtra(EXTRA_CONTEXT_NAME) ?: ""
        val nonWhitelistedPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val store = SessionStateStore(this)
        val intention = store.currentTask

        setContent {
            PresenceTheme {
                ReminderScreen(
                    contextName = contextName,
                    intention = intention,
                    onGoBack = {
                        // Navigate away from the non-whitelisted app to the home screen
                        startActivity(
                            Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                        finish()
                    },
                    onContinueAnyway = {
                        // Allow this specific package for the remainder of this Focus Mode session
                        // in the current app. Cleared automatically when user opens a different app.
                        store.focusModeAllowedPackage = nonWhitelistedPackage
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_CONTEXT_NAME = "extra_context_name"
        private const val EXTRA_PACKAGE_NAME = "extra_package_name"

        fun launch(context: Context, contextName: String, packageName: String) {
            context.startActivity(
                Intent(context, FocusSoftReminderActivity::class.java)
                    .putExtra(EXTRA_CONTEXT_NAME, contextName)
                    .putExtra(EXTRA_PACKAGE_NAME, packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            )
        }
    }
}

@Composable
private fun ReminderScreen(
    contextName: String,
    intention: String,
    onGoBack: () -> Unit,
    onContinueAnyway: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 64.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Focus mode is on. You're ${contextName.lowercase()}.",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )

                if (intention.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = intention,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Button(
                    onClick = onGoBack,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Go Back")
                }
                OutlinedButton(
                    onClick = onContinueAnyway,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Continue Anyway")
                }
            }
        }
    }
}
