package com.vyn.player.ui.screens.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyn.player.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isScanning: Boolean = false,
    val scanMessage: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val userPrefs: com.vyn.player.data.preferences.UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    val themeMode: StateFlow<Int> = userPrefs.themeMode
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 0)

    fun setThemeMode(mode: Int) {
        viewModelScope.launch {
            userPrefs.saveThemeMode(mode)
        }
    }

    fun rescanLibrary() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState(isScanning = true, scanMessage = "Scanning...")
            musicRepository.refreshLibrary()
            _uiState.value = SettingsUiState(isScanning = false, scanMessage = "Library refreshed!")
        }
    }
}
