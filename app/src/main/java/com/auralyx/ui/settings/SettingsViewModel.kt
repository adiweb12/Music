package com.auralyx.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralyx.domain.repository.MediaRepository
import com.auralyx.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val darkTheme: Boolean        = true,
    val defaultVideoMode: Boolean = false,
    val scanOnLaunch: Boolean     = true,
    val isScanning: Boolean       = false,
    val scanMessage: String?      = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val repository: MediaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(prefs.isDarkTheme, prefs.isDefaultVideoMode, prefs.scanOnLaunch) { dark, video, scan ->
                SettingsUiState(darkTheme = dark, defaultVideoMode = video, scanOnLaunch = scan)
            }.collect { _state.value = it }
        }
    }

    fun toggleDarkTheme(enabled: Boolean) = viewModelScope.launch { prefs.setDarkTheme(enabled) }
    fun toggleDefaultVideoMode(enabled: Boolean) = viewModelScope.launch { prefs.setDefaultVideoMode(enabled) }
    fun toggleScanOnLaunch(enabled: Boolean) = viewModelScope.launch { prefs.setScanOnLaunch(enabled) }

    fun scanStorage() {
        viewModelScope.launch {
            _state.update { it.copy(isScanning = true, scanMessage = null) }
            try {
                repository.scanStorage()
                val count = repository.getSongCount()
                _state.update { it.copy(isScanning = false, scanMessage = "Found $count items") }
            } catch (e: Exception) {
                _state.update { it.copy(isScanning = false, scanMessage = "Scan failed: ${e.message}") }
            }
        }
    }
}
