package com.vyn.player.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyn.player.BuildConfig
import com.vyn.player.data.model.GithubRelease
import com.vyn.player.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class SettingsUiState(
    val isScanning: Boolean = false,
    val scanMessage: String = "",
    val isCheckingForUpdates: Boolean = false,
    val latestRelease: GithubRelease? = null,
    val isUpdateAvailable: Boolean = false,
    val updateErrorMessage: String? = null,
    val showUpdateDialog: Boolean = false,
    val showRetryDialog: Boolean = false,
    val infoMessage: String? = null
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

    fun checkForUpdates(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCheckingForUpdates = true,
                updateErrorMessage = null,
                showRetryDialog = false,
                infoMessage = null
            )

            when (val result = fetchLatestRelease(forceRefresh)) {
                is UpdateCheckResult.Success -> {
                    val release = result.release
                    val updateAvailable = isUpdateAvailable(BuildConfig.VERSION_NAME, release.tag_name)
                    _uiState.value = _uiState.value.copy(
                        isCheckingForUpdates = false,
                        latestRelease = release,
                        isUpdateAvailable = updateAvailable,
                        showUpdateDialog = updateAvailable,
                        showRetryDialog = false,
                        infoMessage = if (updateAvailable) null else "You're on latest version"
                    )
                }

                UpdateCheckResult.NoReleasesYet -> {
                    _uiState.value = _uiState.value.copy(
                        isCheckingForUpdates = false,
                        latestRelease = null,
                        isUpdateAvailable = false,
                        showUpdateDialog = false,
                        showRetryDialog = false,
                        infoMessage = "No updates available yet"
                    )
                }

                UpdateCheckResult.NoInternet -> {
                    _uiState.value = _uiState.value.copy(
                        isCheckingForUpdates = false,
                        updateErrorMessage = "No internet",
                        showRetryDialog = true
                    )
                }

                is UpdateCheckResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isCheckingForUpdates = false,
                        updateErrorMessage = result.message,
                        showRetryDialog = true
                    )
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        _uiState.value = _uiState.value.copy(showUpdateDialog = false)
    }

    fun dismissRetryDialog() {
        _uiState.value = _uiState.value.copy(showRetryDialog = false)
    }

    fun clearInfoMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }

    private fun isUpdateAvailable(currentVersion: String, latestVersion: String): Boolean {
        return normalizeVersion(currentVersion) != normalizeVersion(latestVersion)
    }

    private fun normalizeVersion(version: String): String {
        return version.trim().removePrefix("v").removePrefix("V")
    }
}

sealed interface UpdateCheckResult {
    data class Success(val release: GithubRelease) : UpdateCheckResult
    data object NoReleasesYet : UpdateCheckResult
    data object NoInternet : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

private val githubReleaseHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

suspend fun fetchLatestRelease(forceRefresh: Boolean = false): UpdateCheckResult {
    val cached = if (!forceRefresh) getValidCachedRelease() else null
    if (cached != null) return UpdateCheckResult.Success(cached)

    return withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(LATEST_RELEASE_URL)
                .get()
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .build()

            githubReleaseHttpClient.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> {
                        val body = response.body?.string().orEmpty()
                        if (body.isBlank()) {
                            return@use UpdateCheckResult.Error("Unable to check for updates. Please try again.")
                        }

                        val json = JSONObject(body)
                        val tagName = json.optString("tag_name").trim()
                        val changelog = json.optString("body").trim()
                        val assets = json.optJSONArray("assets")
                        val asset = if (assets != null && assets.length() > 0) assets.optJSONObject(0) else null
                        val apkUrl = asset?.optString("browser_download_url").orEmpty().trim()

                        if (tagName.isBlank() || apkUrl.isBlank() || !apkUrl.startsWith("https://")) {
                            return@use UpdateCheckResult.Error("Invalid release data received from server.")
                        }

                        val release = GithubRelease(
                            tag_name = tagName,
                            body = changelog,
                            apkUrl = apkUrl
                        )
                        cacheRelease(release)
                        UpdateCheckResult.Success(release)
                    }

                    404 -> UpdateCheckResult.NoReleasesYet
                    else -> UpdateCheckResult.Error("GitHub API error: ${response.code}")
                }
            }

        } catch (_: IOException) {
            UpdateCheckResult.NoInternet
        } catch (exception: Exception) {
            UpdateCheckResult.Error(exception.message ?: "Unable to check for updates. Please try again.")
        }
    }
}

private fun getValidCachedRelease(): GithubRelease? {
    val now = System.currentTimeMillis()
    return if (cachedRelease != null && now - lastFetchedAt <= CACHE_DURATION_MS) cachedRelease else null
}

private fun cacheRelease(release: GithubRelease) {
    cachedRelease = release
    lastFetchedAt = System.currentTimeMillis()
}

private const val LATEST_RELEASE_URL = "https://api.github.com/repos/aniketlab/VynPlayer-Releases/releases/latest"
private const val CACHE_DURATION_MS = 5 * 60 * 1000L

@Volatile
private var cachedRelease: GithubRelease? = null

@Volatile
private var lastFetchedAt: Long = 0L
