package com.eddy.presence.ui.history

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eddy.presence.data.model.LogEntry
import com.eddy.presence.ui.theme.PresenceTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : ComponentActivity() {

    private val viewModel: HistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PresenceTheme {
                val history by viewModel.history.collectAsState()
                HistoryScreen(
                    history = history,
                    onDelete = viewModel::delete,
                    onBack = ::finish,
                )
            }
        }
    }

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, HistoryActivity::class.java))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreen(
    history: List<DayGroup>,
    onDelete: (LogEntry) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (history.isEmpty()) {
            Text(
                text = "No sessions logged yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(innerPadding).padding(24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            ) {
                history.forEach { group ->
                    item(key = group.label) {
                        DayHeader(label = group.label)
                    }
                    items(group.entries, key = { it.id }) { entry ->
                        EntryRow(entry = entry, onDelete = { onDelete(entry) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(label: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        HorizontalDivider()
    }
}

private val timeFormat = SimpleDateFormat("h:mma", Locale.getDefault())

@Composable
private fun EntryRow(entry: LogEntry, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val endTime = timeFormat.format(Date(entry.timestamp)).lowercase()
    val time = if (entry.sessionStartTime > 0L) {
        val startTime = timeFormat.format(Date(entry.sessionStartTime)).lowercase()
        "$startTime → $endTime"
    } else {
        endTime
    }
    val modeLabel = if (entry.mode == "DEEP_WORK") "Deep Work" else entry.mode
    val detail = buildString {
        if (entry.didText.isNotBlank()) append("Did: ${entry.didText}")
        if (entry.nextFocusText.isNotBlank()) {
            if (isNotEmpty()) append("  ·  ")
            append("Next: ${entry.nextFocusText}")
        }
        if (entry.notes.isNotBlank()) {
            if (isNotEmpty()) append("  ·  ")
            append(entry.notes)
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.Top,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
        Row {
            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$modeLabel · ${entry.intervalMinutes} min",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
            if (entry.focusRating.isNotBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                val (label, color) = when (entry.focusRating) {
                    "Hyper" -> "Hyper" to androidx.compose.ui.graphics.Color(0xFF6A1B9A)
                    "Focused" -> "Focused" to androidx.compose.ui.graphics.Color(0xFF2E7D32)
                    "Partially" -> "Partially" to androidx.compose.ui.graphics.Color(0xFFF57F17)
                    else -> "Distracted" to MaterialTheme.colorScheme.error
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        if (detail.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 64.dp),
            )
        }
        } // end Column
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete entry",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
