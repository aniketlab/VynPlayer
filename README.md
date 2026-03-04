# 🎵 Muzic — Android Offline Music Player

A lightweight, ad-free offline music player focused on high-quality audio playback with a clean Material Design 3 UI.

## ✨ Features

- 🎶 **Offline Playback** — No internet required, ever
- 🔊 **High Quality Audio** — FLAC, WAV, ALAC, OPUS, OGG, AAC, MP3 support
- 🎨 **Material Design 3** — Beautiful dark theme with smooth animations
- 📂 **Multiple Views** — Songs, Albums, Artists, Playlists, Folders
- ❤️ **Favorites** — Quick access to your loved tracks
- 📝 **Playlists** — Create and manage custom playlists
- 🔀 **Shuffle & Repeat** — All playback modes supported
- 🔔 **Notification Controls** — Play/pause from notification & lock screen
- 🎧 **Bluetooth Support** — Full media session integration
- 🚫 **No Ads, No Tracking** — Pure music experience

## 📱 Screenshots

*Coming soon*

## 🛠️ Tech Stack

| Technology | Usage |
|------------|-------|
| Kotlin | 100% |
| Jetpack Compose | UI Framework |
| Media3 / ExoPlayer | Audio Engine |
| Room | Local Database |
| Hilt | Dependency Injection |
| Coil | Image Loading |
| Coroutines + Flow | Async Operations |

## 🏗️ Architecture

MVVM + Repository Pattern

```
com.muzic.player/
├── data/          # Models, Room DB, Repositories, MediaStore Scanner
├── di/            # Hilt Modules
├── player/        # ExoPlayer, MediaSession Service, Queue Manager
├── ui/            # Compose Screens, Components, Theme, Navigation
└── util/          # Constants, Helpers
```

## 📦 Build

1. Clone the repository
2. Open in Android Studio (Panda 2 or later)
3. Sync Gradle
4. Run on device/emulator (API 29+)

```bash
./gradlew assembleDebug
```

APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

## 📋 Requirements

- Android 10 (API 29) or later
- Storage permission for music access

## 🗺️ Roadmap

- [x] Audio engine + playback service
- [x] Music library scanning
- [x] Material Design 3 UI
- [x] Playlists & Favorites
- [ ] 10-band Equalizer
- [ ] Bass Boost & Virtualizer
- [ ] Smart Playlists
- [ ] Sleep Timer
- [ ] Gesture Navigation

## 📄 License

This project is for personal use. All rights reserved.

---

**Muzic** — *Pure Sound. No Noise.* 🎵
