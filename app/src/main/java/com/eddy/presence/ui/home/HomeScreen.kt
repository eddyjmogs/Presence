package com.eddy.presence.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eddy.presence.data.model.LogEntry
import com.eddy.presence.ui.history.HistoryActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onStartDeepWork: () -> Unit,
    onStopSession: () -> Unit,
    onViewSession: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val todayEntries by viewModel.todayEntries.collectAsState()
    val countdownSeconds by viewModel.countdownSeconds.collectAsState()
    val ctx = LocalContext.current

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = "Presence",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.session.deepWorkActive) {
                SessionBanner(
                    session = uiState.session,
                    countdownSeconds = countdownSeconds,
                    onViewSession = onViewSession,
                    onStop = onStopSession,
                )
            } else {
                Button(
                    onClick = onStartDeepWork,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Start Deep Work Session")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Today's Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = { HistoryActivity.launch(ctx) }) {
                    Text("All History →")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (todayEntries.isEmpty()) {
                Text(
                    text = "No entries yet. Start a session to begin logging.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                todayEntries.forEach { entry ->
                    LogEntryRow(entry = entry)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun SessionBanner(
    session: ActiveSession,
    countdownSeconds: Long,
    onViewSession: () -> Unit,
    onStop: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = "● Deep Work active",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        val isPending = session.timerExpired || countdownSeconds == 0L
        val countdownText = when {
            countdownSeconds < 0L -> null
            isPending -> "Check-in pending"
            else -> "Next check-in in %d:%02d".format(countdownSeconds / 60, countdownSeconds % 60)
        }
        if (countdownText != null) {
            Text(
                text = countdownText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isPending) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        var showConfirm by remember { mutableStateOf(false) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onViewSession,
                modifier = Modifier.weight(1f),
            ) {
                Text("Open Session")
            }
            Button(
                onClick = { showConfirm = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Text("Stop")
            }
        }

        if (showConfirm) {
            AlertDialog(
                onDismissRequest = { showConfirm = false },
                title = { Text("End session?") },
                text = { Text("Your current interval won't be logged.") },
                confirmButton = {
                    Button(
                        onClick = { showConfirm = false; onStop() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) { Text("End Session") }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
                },
            )
        }
    }
}

private val timeFormatter = SimpleDateFormat("h:mma", Locale.getDefault())

@Composable
private fun LogEntryRow(
    entry: LogEntry,
    modifier: Modifier = Modifier,
) {
    val time = timeFormatter.format(Date(entry.timestamp)).lowercase()
    val summary = buildString {
        if (entry.didText.isNotBlank()) append("Did: ${entry.didText}")
        if (entry.nextFocusText.isNotBlank()) {
            if (isNotEmpty()) append(" | ")
            append("Next: ${entry.nextFocusText}")
        }
    }

    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            text = time,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
