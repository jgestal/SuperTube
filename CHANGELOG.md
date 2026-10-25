# Changelog

All notable changes to SuperTube. Versions follow the [GitHub releases](https://github.com/jgestal/SuperTube/releases).

## 1.3 — 2026-10-25

### Added
- **Speed button** in the player: tap to switch between 1×, 1.5× and 2×, long press for the full speed list.
- **Media notification and lock screen controls** (previous, play/pause, next, artwork), also for Bluetooth and
  headset buttons, through a `mediaPlayback` foreground service.
- **Pause when headphones are unplugged** or a Bluetooth device disconnects.

### Changed
- Leaving the app with Home keeps the video in **picture-in-picture**, and it keeps playing with the screen off
  (applied once to users who never changed *Background playback*; it can be changed in the player settings).
- Single taps on the video react faster (shorter wait for a possible double tap).
- The temporary 2× of *press and hold* is never saved: if the app is closed while holding, the video reopens at
  its previous speed.
- Settings are saved as soon as the app goes to the background, so changes made right before closing it are kept.

### Fixed
- Returning to the app while the video was in picture-in-picture left it floating in the small window: it now goes
  back to the player, where you left it.
- The Recents button stopped the video and reloaded it on return (HyperOS doesn't report it to apps and refuses
  picture-in-picture from Recents): the video now keeps playing and goes on where it was.
- Playback stalls of 10–25 s followed by a jump back: network errors of the SABR stream (such as
  `ERR_QUIC_PROTOCOL_ERROR`) were ignored, so the player waited until the video was reloaded. They now fail over
  to another media server and, if needed, to another network engine.
- Black flash, stall and lost subtitles, zoom or resize mode when rotating, entering fullscreen or picture-in-picture.
- The seek bar jumped back for a moment after seeking; double tap could seek past the end of the video.
- The play button disappeared while paused and buffering.
- SponsorBlock segments were slightly misaligned with the seek bar.
- The default *Favorites* folder kept the name of the language it was created in; it now follows the app language
  (existing folders are migrated).
- *More* sometimes opened scrolled down, hiding its first sections.
- The notification and lock screen showed no artwork for videos opened from a link.
- The *Subscribe* button truncated the channel name (shorter labels in all languages).
- Misaligned header in empty favorites folders and in the carousel layout.

### Other
- The app language can be chosen per app in the system settings (Android 13+).
- Plural forms for the import message in every language, and no remaining lint warnings in the app module.

### Compatibility
- Runs natively on devices with **16 KB memory pages** (Android 15+): Cronet 143 and Conscrypt 2.8.0, and native
  libraries packaged for 16 KB alignment.

## 1.2 — 2026-10-24
- The whole interface translated into **20 languages**.
- New **About** screen with the authors, credits, links and open source licenses.

## 1.1.1 — 2026-10-24
- **Wi‑Fi only downloads** switch also in *Settings › SuperTube*.

## 1.1 — 2026-10-24
- **Mini player**: back or swipe down collapses the video into a bar while browsing.
- **Sleep timer**: 15 / 30 / 45 / 60 / 90 minutes or at the end of the video.
- **Quality picker** before downloading, with the size of each option.
- **Wi‑Fi only downloads** that wait for Wi‑Fi and resume automatically.
- **Home screen widget** with your favorites.
- **Export / import** of favorites and SuperTube settings.
- **Updates** from the GitHub releases of this repository.
- Fixes: audio downloads without an account, real file sizes, private empty search, the app no longer goes to the
  background after closing a player opened from a link.

## 1.0 — 2026-10-24
- First release: touch-first Material 3 interface for phones and tablets on top of the SmartTube engine, adaptive
  navigation, four feed layouts, touch player with gestures, favorites with folders and a downloads manager.
