package com.eddy.presence.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eddy.presence.data.model.LogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onStartDeepWork: (task: String) -> Unit,
    onStartFocusMode: (context: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val todayEntries by viewModel.todayEntries.collectAsState()

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

            OutlinedTextField(
                value = uiState.taskText,
                onValueChange = viewModel::onTaskTextChange,
                label = { Text("Current task") },
                placeholder = { Text("What are you working on?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onStartDeepWork(uiState.taskText) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.taskText.isNotBlank(),
            ) {
                Text("Start Deep Work Session")
            }

            Spacer(modifier = Modifier.height(12.dp))

            FocusModeSelector(
                expanded = uiState.focusModeExpanded,
                onToggle = viewModel::toggleFocusModeExpanded,
                onContextSelected = { context ->
                    viewModel.collapseFocusMode()
                    onStartFocusMode(context)
                },
            )

            Spacer(modifier = Modifier.height(32.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Today's Log",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

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
private fun FocusModeSelector(
    expanded: Boolean,
    onToggle: () -> Unit,
    onContextSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        OutlinedButton(
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Enable Focus Mode")
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp)) {
                FocusModeOption(label = "🚇 Commute", onClick = { onContextSelected("Commute") })
                FocusModeOption(label = "🚽 Bathroom", onClick = { onContextSelected("Bathroom") })
                FocusModeOption(label = "+ Custom...", onClick = { onContextSelected("Custom") })
            }
        }
    }
}

@Composable
private fun FocusModeOption(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    )
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
