# VYN PLAYER

VYN PLAYER is a modern offline music player for Android built with Kotlin, Jetpack Compose, and AndroidX Media3. It is designed for users who want a fast, elegant, local-first listening experience without ads, accounts, or streaming dependencies.

## Overview

VYN PLAYER focuses on smooth local music playback, a polished modern interface, and maintainable Android architecture. The app scans on-device audio, organizes it into a structured library, and delivers a responsive playback experience with queue handling, playlist support, favorites, history-driven features, and deep Android media integration.

## Key Features

- **Offline-first playback** for locally stored music
- **Modern Compose UI** with a clean dark design language
- **Media3 / ExoPlayer engine** for robust playback handling
- **Local library browsing** across songs, albums, artists, playlists, and folders
- **Queue, shuffle, and repeat** playback controls
- **Mini player + full now-playing screen**
- **Favorites and playlists** stored locally
- **Recent listening and smart mix support**
- **System media integration** for notifications, lock screen, Bluetooth, and headset controls
- **Privacy-friendly approach** with no ads and no mandatory online services

## Supported Audio Formats

VYN PLAYER is intended for common local audio playback workflows and supports formats typically handled through Android media playback, including:

- MP3
- AAC
- WAV
- FLAC
- OGG
- OPUS

## Technology Stack

| Category | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Playback | AndroidX Media3 / ExoPlayer |
| Architecture | MVVM + Repository Pattern |
| Dependency Injection | Hilt |
| Persistence | Room Database |
| Async / State | Kotlin Coroutines, Flow |
| Image Loading | Coil |
| Navigation | Navigation Compose |

## Architecture

The application follows a clean MVVM-oriented structure with repositories handling data access and a dedicated playback layer for queue and session management.

```text
app/src/main/java/com/vyn/player/
├── data/
│   ├── local/          # Room database, DAOs, entities, converters
│   ├── model/          # Domain models such as Song, Album, Artist, Playlist
│   ├── preferences/    # Persistent user preferences
│   └── repository/     # Library, playlists, artwork, history, and smart mix logic
├── di/                 # Hilt dependency injection modules
├── player/             # PlaybackManager, QueueManager, MediaSession service
├── ui/
│   ├── components/     # Shared Compose UI components
│   ├── navigation/     # Routes and nav graph
│   ├── screens/        # Screen UIs and view models
│   └── theme/          # Colors, type, spacing, and theme configuration
└── util/               # Helpers, metadata cleanup, permissions, constants, formatting
```

## Project Goals

VYN PLAYER is built around three core goals:

1. **Local-first listening** — keep the music experience focused on files already on the device.
2. **Smooth UX** — deliver fast rendering, fluid scrolling, and responsive playback actions.
3. **Professional codebase** — maintain a structure that is easy to extend and maintain over time.

## Build Instructions

### Prerequisites

- Android Studio Iguana or newer recommended
- Android SDK properly configured
- Java environment compatible with the project toolchain

### Build Debug APK

**Windows**

```bash
.\gradlew.bat assembleDebug
```

**macOS / Linux**

```bash
./gradlew assembleDebug
```

### Output

The generated debug APK is available at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Requirements

- **Min SDK:** 29
- **Target SDK:** 36
- **Device Access:** Local media/storage permission required for library access

## Current Functional Areas

- Local library scanning
- Song playback and queue handling
- Playlists and favorites
- Recently played / top songs support
- Smart mix generation
- Mini player and now-playing UI
- MediaSession-backed background playback

## Roadmap

- [x] Offline playback engine integration
- [x] MediaStore-based library support
- [x] Compose-based modern UI
- [x] Playlists and favorites
- [x] Queue and smart playback features
- [ ] Equalizer and advanced audio effects
- [ ] Sleep timer
- [ ] Expanded discovery and detail experiences
- [ ] Additional playback personalization options

## Development Notes

- The project may show Android Gradle Plugin warnings for `compileSdk = 36` when using AGP `8.3.0`; these are currently non-blocking.
- Newer Java versions may show native-access warnings during Gradle execution; these are cosmetic unless the toolchain policy changes.
- Debug builds are intentionally larger than optimized release builds.

## Repository Usage

- This repository contains the main source code for VYN PLAYER.
- Debug APKs can be generated locally through Gradle.
- Releases can be distributed separately through GitHub Releases or a dedicated release repository.

## License

Unless otherwise specified by the repository owner, this project is maintained under a private/internal usage model.
