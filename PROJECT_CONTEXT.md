# 🎵 Muzic — Project Context & Status

> **This file helps AI assistants understand the project quickly.**
> When starting a new chat, say: "Muzic project continue karna hai" and point to this file.

---

## Project Overview

**Muzic** is an ad-free, offline Android music player built with Kotlin + Jetpack Compose.
- **Developer:** officialtechrom
- **Package:** `com.muzic.player`
- **Min SDK:** 29 (Android 10)
- **Target SDK:** 36

## Repository Structure

| Repo | Visibility | Purpose |
|------|-----------|---------|
| [Muzic](https://github.com/officialtechrom/Muzic) | 🔒 Private | Source code (development) |
| [Muzic-Releases](https://github.com/officialtechrom/Muzic-Releases) | 🌍 Public | APK downloads & release updates for users |

## Local Paths

- **Project:** `c:\Users\sharm\Downloads\Programs\Antigravity\Muzic\`
- **APK Output:** `app\build\outputs\apk\debug\app-debug.apk`
- **Desktop APK Copy:** `Desktop\Muzic.apk`
- **Android SDK:** `C:\Users\sharm\AppData\Local\Android\Sdk`

## Tech Stack

| Technology | Version | Usage |
|------------|---------|-------|
| Kotlin | 2.0.0 | Language |
| Jetpack Compose | BOM 2024.02.02 | UI |
| Media3/ExoPlayer | 1.2.1 | Audio engine |
| Room | 2.6.1 | Database (playlists, favorites) |
| Hilt | 2.50 | Dependency injection |
| Coil | 2.5.0 | Album art loading |
| Navigation Compose | 2.7.7 | Screen navigation |
| AGP | 8.3.0 | Android Gradle Plugin |
| Gradle | 8.4 | Build system |
| Java | 24 | Runtime |

## Architecture

MVVM + Repository Pattern

```
com.muzic.player/
├── MuzicApp.kt              # @HiltAndroidApp
├── MainActivity.kt          # Single Activity + Splash
├── data/
│   ├── local/               # Room DB, DAOs, Entities
│   ├── model/               # Song, Album, Artist, Playlist, Folder
│   └── repository/          # MusicRepository, PlaylistRepository, MediaStoreScanner
├── di/                      # AppModule, DatabaseModule, PlayerModule
├── player/
│   ├── PlaybackManager.kt   # ExoPlayer wrapper (core engine)
│   ├── QueueManager.kt      # Queue/shuffle logic
│   └── MuzicPlaybackService.kt  # MediaSessionService (background)
├── ui/
│   ├── theme/               # Color, Type, Theme (Material 3 dark)
│   ├── navigation/          # Screen routes, NavGraph
│   ├── components/          # SongItem, AlbumCard, ArtistCard, MiniPlayer, SeekBar, PlaybackControls
│   └── screens/
│       ├── splash/          # Animated splash
│       ├── library/         # 5-tab pager (Songs, Albums, Artists, Playlists, Folders)
│       ├── nowplaying/      # Full player with album art
│       └── settings/        # Settings + V2 placeholders
└── util/                    # Constants, TimeUtils, PermissionHelper
```

## Current Status (v1.0.0) — Released 2026-03-04

### ✅ Completed (MVP)
- ExoPlayer audio engine with gapless playback
- MediaSession (notification, lock screen, Bluetooth)
- MediaStore scanner (songs, albums, artists, folders)
- Room database (playlists, favorites)
- Material Design 3 dark theme (Deep Indigo #1A237E + Electric Purple #7C4DFF)
- 5-tab library with HorizontalPager
- Now Playing screen with gradient + album art
- Mini player bar
- Queue management + shuffle
- Repeat modes (off/one/all)
- Permission handling (API 29+)
- Adaptive launcher icon
- APK built and released on GitHub

### ⬜ Planned (V2)
- 10-band equalizer
- Bass boost + virtualizer
- Smart playlists (recently/most played)
- Sleep timer
- Album/Artist/Folder detail screens
- Gesture navigation in Now Playing
- Search functionality in library
- Release build (ProGuard shrinking → ~30 MB)

## Build Commands

```bash
# Build debug APK
.\gradlew.bat assembleDebug

# APK location after build
app\build\outputs\apk\debug\app-debug.apk

# Create new GitHub release
gh release create v1.x.0 --repo officialtechrom/Muzic-Releases --title "Muzic v1.x.0" --notes "notes" app-debug.apk#Muzic-v1.x.0.apk

# Push source code changes
git add . && git commit -m "message" && git push
```

## Known Issues / Notes

- AGP 8.3.0 shows warning for compileSdk=36 (safe to ignore)
- Java 24 shows native access warnings (cosmetic, no impact)
- Debug APK is ~63 MB; release build will be ~30 MB with ProGuard
- `Icons.Rounded.ArrowBack` and `QueueMusic` show deprecation warnings (use AutoMirrored versions)

## Design System

- **Primary:** Deep Indigo `#1A237E`
- **Accent:** Electric Purple `#7C4DFF`
- **Background:** Dark Charcoal `#121212`
- **Surface:** `#1E1E1E`
- **Text:** Soft White `#E0E0E0`
- **Typography:** System Sans Serif (Roboto)
- **Icons:** Material Icons Rounded
