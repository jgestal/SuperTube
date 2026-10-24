<p align="center"><img src="images/supertube_icon.png" width="128" alt="SuperTube icon"></p>

# SuperTube

**SuperTube** is a phone and tablet focused fork of [SmartTube](https://github.com/yuliskov/SmartTube),
the ad-free YouTube client for Android TV by [yuliskov](https://github.com/yuliskov).

SmartTube is designed for TV remotes (D-pad navigation, a fixed 1080p "10-foot" UI).
SuperTube keeps all of its engine and features (no ads, SponsorBlock, DeArrow, background playback,
quality selection, accounts, history, playlists...) and replaces the interface with a touch-first one:

- **Jetpack Compose + Material 3** UI with dynamic colors and light/dark themes.
- **Adaptive layouts** (following the official [Android adaptive skill](https://github.com/android/skills)):
  bottom navigation bar on phones, navigation rail on tablets and foldables, grids that add columns on wider screens.
- **Native screen density** instead of the emulated TV density, edge-to-edge drawing.
- **Touch player**: tap to show controls, double tap to seek, seek bar with SponsorBlock segments,
  rotate (or tap) for fullscreen, picture-in-picture, like/share/save/comments/speed chips.
- **Menus and settings as bottom sheets** (centered dialogs on large screens), TV-only settings hidden.
- No updates from the original project are offered (this is a separate app: `app.supertube`).

## Project layout

| Module | Description |
| --- | --- |
| `supertube` | The SuperTube app (Kotlin, Compose). One activity per shared `View` contract. |
| `common` | Shared business logic from SmartTube: presenters, player engine, settings. |
| `smarttubetv` | The original SmartTube TV app (kept to ease merging upstream changes). |
| `MediaServiceCore`, `SharedModules` | SmartTube git submodules (YouTube API, utilities). |

The mobile app implements the same `View` interfaces (`BrowseView`, `PlaybackView`, `SearchView`,
`AppDialogView`...) as the TV app, so every presenter in `common` works unchanged.

## Building

```bash
git clone --recurse-submodules https://github.com/jgestal/SuperTube.git
cd SuperTube
# Gradle 7.5 needs JDK 11-17
JAVA_HOME=/path/to/jdk-17 ./gradlew :supertube:assembleStbetaDebug
```

The APK is generated in `supertube/build/outputs/apk/stbeta/debug/`.

## Credits and license

SuperTube is based on [SmartTube](https://github.com/yuliskov/SmartTube) by yuliskov and its modules
[MediaServiceCore](https://github.com/yuliskov/MediaServiceCore) and [SharedModules](https://github.com/yuliskov/SharedModules),
all released under the MIT License. All credit for the YouTube engine and the original features goes to them.
The original README is kept in [README_SmartTube.md](README_SmartTube.md).

Released under the [MIT License](LICENSE). ExoPlayer (`exoplayer-amzn-2.10.6`) is licensed under Apache 2.0.

SuperTube is not affiliated with, endorsed by or connected to YouTube, Google or the SmartTube project.
