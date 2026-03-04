# Muzic - Android Offline Audio Architecture

Muzic is a lightweight, ad-free offline audio player for Android, engineered for high-fidelity playback with a responsive, modern application architecture.

## Overview

Designed with Android's Jetpack Compose and Media3, Muzic offers a robust audio playback experience. Features include complete offline capabilities, support for lossless audio formats, and standard media session integration for system-wide controls.

## Key Features

- **Offline Playback:** Operates entirely without network requirements.
- **High-Fidelity Audio Support:** Compatible with FLAC, WAV, ALAC, OPUS, OGG, AAC, and MP3.
- **Modern User Interface:** Built using Jetpack Compose with a persistent bottom navigation paradigm, a global mini-player, and a dark-themed aesthetic.
- **Media Library Management:** Direct fetching and categorization of local device media into Songs, Albums, Artists, Playlists, and Folders.
- **System Integrations:** Full support for Android MediaSession, allowing playback control via the notification drawer, lock screen, and Bluetooth peripherals.
- **Privacy-Centric:** No internet permissions requested, zero telemetry, and zero advertisements.

## Technology Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Audio Engine:** AndroidX Media3 / ExoPlayer
- **Local Persistence:** Room Database
- **Dependency Injection:** Hilt
- **Image Loading:** Coil
- **Asynchrony:** Kotlin Coroutines & Flow

## Architecture

The project strictly follows the MVVM (Model-View-ViewModel) architectural pattern combined with a local Repository data layer.

```text
com.muzic.player/
├── data/          # Entity models, Room DAOs, Repositories, MediaStore logic
├── di/            # Hilt module bindings
├── player/        # Media3 ExoPlayer instance, MediaSession service
├── ui/            # Compose screens, components, theme tokens, navigation graph
└── util/          # Extension functions, permission helpers, constants
```

## Build Instructions

1. Clone the repository.
2. Open the project in Android Studio (Iguana 2023.2.1 or newer recommended).
3. Synchronize Gradle files.
4. Execute via standard procedures or build directly via the Gradle Wrapper:

```bash
./gradlew assembleDebug
```

The resulting debug APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

## Requirements

- Minimum OS: Android 10 (API Level 29)
- Target SDK: API 36
- Storage Permissions for local media access

## Roadmap

- [x] Integrate Media3 audio engine and playback service
- [x] Implement MediaStore library scanning
- [x] Construct Jetpack Compose UI with persistent navigation
- [x] Global playback state handling (MiniPlayer)
- [x] Implement Playlists and Favorites
- [ ] Implement System Audio Effects (Equalizer, Bass Boost, Virtualizer interfaces)
- [ ] Smart Playlists functionality
- [ ] Sleep Timer implementation

## License

All rights reserved. Muzic is currently maintained for personal internal use.
