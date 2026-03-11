package com.muzic.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.muzic.player.data.repository.ArtworkRepository
import com.muzic.player.ui.MuzicAppContent
import com.muzic.player.ui.components.LocalArtworkRepository
import com.muzic.player.ui.theme.MuzicTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import com.muzic.player.data.preferences.UserPreferencesManager

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesManager: UserPreferencesManager

    @Inject
    lateinit var artworkRepository: ArtworkRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by userPreferencesManager.themeMode.collectAsState(initial = 0)
            val isDarkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            MuzicTheme(darkTheme = isDarkTheme) {
                CompositionLocalProvider(LocalArtworkRepository provides artworkRepository) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        MuzicAppContent()
                    }
                }
            }
        }
    }
}
