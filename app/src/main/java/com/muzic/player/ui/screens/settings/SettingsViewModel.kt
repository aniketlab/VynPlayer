package com.muzic.player.ui.screens.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muzic.player.data.repository.ArtworkRepository
import com.muzic.player.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val userPrefs: com.muzic.player.data.preferences.UserPreferencesManager,
    private val artworkRepository: ArtworkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    val themeMode: StateFlow<Int> = userPrefs.themeMode
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 0)

    val artworkDownloadMode: StateFlow<Int> = userPrefs.artworkDownloadMode
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 2)

    fun setThemeMode(mode: Int) {
        viewModelScope.launch {
            userPrefs.saveThemeMode(mode)
        }
    }

    fun setArtworkDownloadMode(mode: Int) {
        viewModelScope.launch {
            userPrefs.saveArtworkDownloadMode(mode)
        }
    }

    fun rescanLibrary() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState(isScanning = true, scanMessage = "Scanning...")
            musicRepository.refreshLibrary()
            _uiState.value = SettingsUiState(isScanning = false, scanMessage = "Library refreshed!")
        }
    }

    /**
     * Force triggers the artwork download pipeline from scratch.
     * Clears all previous state (queuedKeys, failedAttempts) and rebuilds.
     */
    fun forceArtworkScan() {
        viewModelScope.launch {
            Log.d("ArtworkDebug", "Force Artwork Scan button pressed!")
            _uiState.value = SettingsUiState(isScanning = true, scanMessage = "Testing iTunes API...")

            try {
                // Direct OkHttp test — no DNS pre-check, no ConnectivityManager
                val testResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val client = okhttp3.OkHttpClient.Builder()
                            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                            .followRedirects(true)
                            .build()
                        val request = okhttp3.Request.Builder()
                            .url("https://itunes.apple.com/search?term=test&limit=1")
                            .header("User-Agent", "MuzicPlayer/2.2.0")
                            .build()
                        Log.d("ArtworkDebug", "Sending OkHttp request to iTunes...")
                        val response = client.newCall(request).execute()
                        val body = response.body?.string() ?: ""
                        Log.d("ArtworkDebug", "Response: HTTP ${response.code}, body=${body.length} chars")
                        "HTTP ${response.code} (${body.length} chars)"
                    } catch (e: Exception) {
                        Log.e("ArtworkDebug", "OkHttp test FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
                        "FAILED: ${e.javaClass.simpleName}: ${e.message}"
                    }
                }

                _uiState.value = SettingsUiState(isScanning = true, scanMessage = "Test: $testResult")
                kotlinx.coroutines.delay(2000)

                // Always proceed — let the actual downloads fail naturally if no internet
                _uiState.value = SettingsUiState(isScanning = true, scanMessage = "Loading songs...")
                val songs = musicRepository.getAllSongs().first()
                Log.d("ArtworkDebug", "Got ${songs.size} songs for force scan")

                _uiState.value = SettingsUiState(isScanning = true, scanMessage = "Queuing ${songs.size} songs...")
                artworkRepository.forceArtworkScan(songs)

                _uiState.value = SettingsUiState(
                    isScanning = false,
                    scanMessage = if (testResult.contains("200")) "✓ Downloading artwork..." else "⚠ $testResult — trying anyway"
                )
            } catch (e: Exception) {
                Log.e("ArtworkDebug", "Force scan error: ${e.message}", e)
                _uiState.value = SettingsUiState(isScanning = false, scanMessage = "Error: ${e.message}")
            }
        }
    }
}
