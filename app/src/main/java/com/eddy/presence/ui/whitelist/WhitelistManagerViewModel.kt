package com.eddy.presence.ui.whitelist

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eddy.presence.PresenceApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppInfo(val label: String, val packageName: String)

data class WhitelistUiState(
    val contextName: String = "",
    val apps: List<AppInfo> = emptyList(),
    val whitelistedPackages: Set<String> = emptySet(),
    val loading: Boolean = true,
)

class WhitelistManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as PresenceApplication).whitelistRepository
    private val _contextName = MutableStateFlow("")

    private val _uiState = MutableStateFlow(WhitelistUiState())
    val uiState: StateFlow<WhitelistUiState> = _uiState

    fun init(contextName: String) {
        _contextName.value = contextName
        _uiState.update { it.copy(contextName = contextName) }

        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { loadInstalledApps() }
            _uiState.update { it.copy(apps = apps, loading = false) }
        }

        viewModelScope.launch {
            repo.getPackagesForContext(contextName).collect { packages ->
                _uiState.update { it.copy(whitelistedPackages = packages.toSet()) }
            }
        }
    }

    fun toggle(packageName: String, whitelisted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.setWhitelisted(_contextName.value, packageName, whitelisted)
        }
    }

    private fun loadInstalledApps(): List<AppInfo> {
        val pm = getApplication<Application>().packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map { AppInfo(label = it.loadLabel(pm).toString(), packageName = it.packageName) }
            .sortedBy { it.label.lowercase() }
    }
}
