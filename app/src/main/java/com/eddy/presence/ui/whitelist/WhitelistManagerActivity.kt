package com.eddy.presence.ui.whitelist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eddy.presence.ui.theme.PresenceTheme

class WhitelistManagerActivity : ComponentActivity() {

    private val viewModel: WhitelistManagerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contextName = intent.getStringExtra(EXTRA_CONTEXT_NAME) ?: return finish()
        viewModel.init(contextName)

        setContent {
            PresenceTheme {
                val uiState by viewModel.uiState.collectAsState()
                WhitelistManagerScreen(
                    uiState = uiState,
                    onToggle = viewModel::toggle,
                )
            }
        }
    }

    companion object {
        private const val EXTRA_CONTEXT_NAME = "extra_context_name"

        fun launch(context: Context, contextName: String) {
            context.startActivity(
                Intent(context, WhitelistManagerActivity::class.java)
                    .putExtra(EXTRA_CONTEXT_NAME, contextName)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WhitelistManagerScreen(
    uiState: WhitelistUiState,
    onToggle: (packageName: String, whitelisted: Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("${uiState.contextName} — Allowed Apps") })
        },
    ) { innerPadding ->
        if (uiState.loading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            ) {
                items(uiState.apps, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        checked = app.packageName in uiState.whitelistedPackages,
                        onCheckedChange = { checked -> onToggle(app.packageName, checked) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    app: AppInfo,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = app.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
