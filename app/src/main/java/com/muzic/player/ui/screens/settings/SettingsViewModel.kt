package com.muzic.player.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muzic.player.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isScanning: Boolean = false,
    val scanMessage: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun rescanLibrary() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState(isScanning = true, scanMessage = "Scanning...")
            musicRepository.refreshLibrary()
            _uiState.value = SettingsUiState(isScanning = false, scanMessage = "Library refreshed!")
        }
    }
}
