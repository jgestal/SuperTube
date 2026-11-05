<p align="right"><b>English</b> · <a href="README.es.md">Español</a></p>

<p align="center">
  <img src="images/supertube_icon.png" width="112" alt="SuperTube icon">
</p>

<h1 align="center">SuperTube</h1>

<p align="center">
  <b>An ad-free YouTube client for Android phones and tablets, with a touch-first Material&nbsp;3 interface.</b><br>
  Also for Android TV. Fork of <a href="https://github.com/yuliskov/SmartTube">SmartTube</a> by yuliskov.<br>
  <a href="https://gestal.es/supertube/">gestal.es/supertube</a>
</p>

<p align="center">
  <a href="https://github.com/jgestal/SuperTube/releases/latest"><img alt="Latest release" src="https://img.shields.io/github/v/release/jgestal/SuperTube?label=download&style=for-the-badge&color=E53935"></a>
  <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white">
  <a href="LICENSE"><img alt="License: MIT" src="https://img.shields.io/badge/license-MIT-blue?style=for-the-badge"></a>
</p>

<p align="center">
  <img src="docs/screenshots/hero.webp" width="820" alt="SuperTube: home feed with the mini player, search and favorites">
</p>

## Contents

- [Why SuperTube?](#why-supertube)
- [Features](#features)
- [SuperTube for Android TV](#supertube-for-android-tv)
- [Installation](#installation)
- [FAQ](#faq)
- [Privacy](#privacy)
- [Building from source](#building-from-source)
- [Architecture](#architecture)
- [Contributing](#contributing)
- [Legal notice](#legal-notice)
- [Credits and license](#credits-and-license)

## Why SuperTube?

[SmartTube](https://github.com/yuliskov/SmartTube) is an excellent YouTube client, but it is built for TV remotes: a
fixed 1080p "10-foot" interface, D-pad focus navigation and Leanback screens. On a phone it looks tiny and is awkward to
use with your fingers.

**SuperTube keeps SmartTube's engine** (no ads, SponsorBlock, DeArrow, accounts, history, playlists, quality and codec
selection) and **replaces the whole interface** with one designed for touch screens, following the
[Android adaptive design guidelines](https://developer.android.com/develop/ui/compose/layouts/adaptive).

## Features

### A real mobile interface

Jetpack Compose and Material 3, with light and dark themes and dynamic colors. Navigation adapts to the window: a bottom
bar on phones, and a navigation rail in landscape, on tablets and on foldables. Videos can be shown as a **list**, a
**compact list**, a **grid** or **horizontal shelves**, and the app remembers your choice.

<p align="center">
  <img src="docs/screenshots/layout_menu.webp" width="250" alt="Feed layout menu">
  <img src="docs/screenshots/grid.webp" width="250" alt="Grid layout">
</p>
<p align="center">
  <img src="docs/screenshots/landscape_rail.webp" width="640" alt="Navigation rail in landscape">
</p>

### Touch player

| Gesture or control | Action |
| --- | --- |
| Double tap left / right | Seek −10 / +10 s (consecutive taps add up) |
| Press and hold | Temporary 2× speed |
| Speed button | 1× → 1.5× → 2×; long press for the full list |
| Swipe up / down | Fullscreen / collapse into the mini player (vertical videos stay in portrait) |
| Swipe in fullscreen | Brightness (left side) and volume (right side) |

The seek bar shows the **SponsorBlock** segments. Back or a swipe down collapses the video into a **mini player** that
keeps playing while you browse. Leaving the app keeps the video in **picture-in-picture**, and audio keeps playing with
the screen off, with **lock screen and notification controls**. There is also a **sleep timer**, and playback pauses
when headphones are unplugged.

<p align="center">
  <img src="docs/screenshots/player.webp" width="250" alt="Player controls with the speed button">
  <img src="docs/screenshots/hold.webp" width="250" alt="Press and hold for 2x speed">
</p>
<p align="center">
  <img src="docs/screenshots/fullscreen.webp" width="640" alt="Fullscreen player">
</p>
<p align="center">
  <img src="docs/screenshots/media_controls.webp" width="380" alt="Media controls in the notification shade">
</p>

### Favorites with folders

The ♡ on every video saves it to *Favorites* or to your own folders. Folders can be searched, renamed, and played in
order or shuffled. A **home screen widget** shows them, and everything can be **exported and imported** as a JSON file.
Favorites are stored only on your device.

<p align="center">
  <img src="docs/screenshots/favorites.webp" width="250" alt="A favorites folder">
  <img src="docs/screenshots/save_to.webp" width="250" alt="Save to folder sheet">
  <img src="docs/screenshots/widget.png" width="250" alt="Home screen widget">
</p>

### Downloads manager

Download the **audio** or the **video** (up to 1080p H.264, joined on the device without re-encoding) after choosing the
quality and seeing its size. Downloads can be **paused and resumed**, even after closing the app, and expired links are
refreshed automatically. There is an optional **Wi‑Fi only** mode, video and audio filters, and an offline player. Files
are saved to `Movies/SuperTube` and `Music/SuperTube`.

<p align="center">
  <img src="docs/screenshots/download_sheet.webp" width="250" alt="Download quality picker">
  <img src="docs/screenshots/search_mini.webp" width="250" alt="Search with the mini player">
</p>

> [!NOTE]
> Without a YouTube account, YouTube only offers single-file streams (usually 360p). Sign in to download up to 1080p.

### Also included

- **Search** with local recent searches, voice search and filters.
- **Reorderable sections** in *More*, settings as bottom sheets, and TV-only settings hidden.
- **Opens YouTube links** shared or tapped in other apps. On Android 12+ you have to allow this once in
  *Settings › SuperTube › Open YouTube links*, which explains the steps. If the YouTube app is installed, it keeps the
  links until you turn off its *Open supported links* setting.
- **Updates** from the releases of this repository (*Settings › SuperTube › Check for updates*).
- **20 languages**, selectable per app in the Android 13+ system settings.
- Everything from SmartTube: no ads, SponsorBlock, DeArrow, sign-in (subscriptions, history, playlists), 4K/8K, HDR,
  60 fps, codec selection, subtitles, live chat and comments.

## SuperTube for Android TV

The TV app (`smarttubetv` module) keeps SmartTube's remote-friendly Leanback interface and adds:

- **Backdrops**: the focused video fills the background, Netflix-style, faded so the text stays readable. Without a
  thumbnail, the home screen shows a SuperTube ambient image.
- **Lite mode for slow TVs and TV boxes** (*Settings › User interface*): no background images, card previews, scrolling
  titles or seek thumbnails, and a smaller video buffer.
- SuperTube name, icons, launcher banner and home screen channel, and updates from this repository
  (*Settings › About › Check for updates*).

<p align="center">
  <img src="docs/screenshots/tv_backdrop.webp" width="640" alt="SuperTube for Android TV: the focused video fills the background">
</p>
<p align="center">
  <img src="docs/screenshots/tv_settings.webp" width="316" alt="TV user interface settings with lite mode">
  <img src="docs/screenshots/tv_lite_mode.webp" width="316" alt="TV home screen in lite mode">
</p>

## Installation

Download the APK from the **[latest release](https://github.com/jgestal/SuperTube/releases/latest)** or from
[gestal.es/supertube](https://gestal.es/supertube/). On a TV, open **gestal.es/supertube/tv** in the browser or a
downloader app.

| Device | File | Package |
| --- | --- | --- |
| Phone or tablet | `SuperTube-<version>.apk` | `es.gestal.supertube` |
| Android TV (any) | `SuperTubeTV_stable_<version>_universal.apk` | `es.gestal.supertube.tv` |
| Android TV (smaller download) | `…_arm64-v8a.apk` for recent TVs, `…_armeabi-v7a.apk` for older ones, `…_x86.apk` | `es.gestal.supertube.tv` |

**Requirements:** Android 8.0 or newer. Ready for devices with 16 KB memory pages. SuperTube is a separate app and can
be installed next to SmartTube.

**Updates:** once installed, new versions are installed from *Check for updates* in the app settings. See the
[changelog](CHANGELOG.md) for what changed in each version.

### Install warnings

Android warns about every app that doesn't come from Google Play. These are the usual messages and what to tap:

| Where | Message | What to do |
| --- | --- | --- |
| Chrome | "This type of file can harm your device" | Tap **Download anyway** |
| Android | "Your phone isn't allowed to install unknown apps from this source" | Tap **Settings**, turn on **Allow from this source** and go back |
| Play Protect | "Unsafe app blocked" or "Unknown app" | Tap **More details**, then **Install anyway** (on a TV, the same with the remote) |
| Play Protect | Later turns the app off or removes it | Play Store › your profile › **Play Protect** › settings › turn off **Scan apps with Play Protect**, then install again |

### Verify the APK

Every release is signed with the same certificate and scanned with VirusTotal (the report is in the release notes).
To check that an APK is the original, compare its signing certificate:

```bash
apksigner verify --print-certs SuperTube-<version>.apk   # from the Android SDK build-tools
```

The SHA-256 digest must be:

```
f6:fb:28:69:7f:24:d1:bf:21:ff:12:b8:20:e2:64:ca:4a:94:2f:3e:e8:4c:75:f2:01:6d:eb:3c:e6:07:70:41
```

(`apksigner` prints it without colons.)

## FAQ

<details>
<summary><b>Is it free?</b></summary>

Yes. No ads, no subscriptions and no in-app purchases. The source code is in this repository under the MIT license.
</details>

<details>
<summary><b>Is it an official YouTube app?</b></summary>

No. SuperTube is an independent project, not affiliated with Google or YouTube. It is built on SmartTube, an
open-source client.
</details>

<details>
<summary><b>Why isn't it on Google Play?</b></summary>

Google Play doesn't accept apps like this one, so it is published as an APK in the GitHub releases and on the website,
with the signing certificate listed above.
</details>

<details>
<summary><b>Do I need an account?</b></summary>

No. Sign in only if you want your subscriptions, history and playlists, or downloads above 360p. The session is stored
on your device.
</details>

<details>
<summary><b>Does it work with Chromecast or Android Auto?</b></summary>

No. SuperTube doesn't include Google Play Services, which those features need.
</details>

<details>
<summary><b>Is there an iPhone version?</b></summary>

No. SuperTube is for Android 8.0 or newer, on phones, tablets and Android TV.
</details>

<details>
<summary><b>Can I use the phone APK on a TV, or the other way round?</b></summary>

It installs, but the phone APK isn't made for a remote, and the TV one isn't made for touch screens.
</details>

More answers on the [website FAQ](https://gestal.es/supertube/#faq).

## Privacy

SuperTube doesn't collect any personal data for the developer: no SuperTube accounts, no analytics, no advertising and
no crash reports, and no server of its own. Settings, favorites, recent searches, downloads and the optional Google
session stay on your device. The app connects directly to YouTube/Google, SponsorBlock, DeArrow, Return YouTube Dislike
and GitHub (update check) to work.

Read the full [privacy policy](https://gestal.es/supertube/privacy/). (`PRIVACY.md` in this repository is the original
policy of SmartTube's F-Droid build.)

## Building from source

**Requirements:** JDK 17 (Gradle 7.5 runs on JDK 11–17), the Android SDK, and the git submodules.

```bash
git clone --recurse-submodules https://github.com/jgestal/SuperTube.git
cd SuperTube

# Phone and tablet app → supertube/build/outputs/apk/stbeta/debug/
JAVA_HOME=/path/to/jdk-17 ./gradlew :supertube:assembleStbetaDebug

# Android TV app → smarttubetv/build/outputs/apk/ststable/debug/
JAVA_HOME=/path/to/jdk-17 ./gradlew :smarttubetv:assembleStstableDebug
```

If you cloned without `--recurse-submodules`, run `git submodule update --init --recursive`.

### Tests

```bash
./gradlew :supertube:testStbetaDebugUnitTest :supertube:lintStbetaDebug   # unit tests and lint
supertube/tools/rotation_e2e.sh path/to/SuperTube.apk                      # fullscreen and rotation, on an emulator
```

Every push to `main` builds the APKs on GitHub Actions (lint, build and VirusTotal scan); the nightly APKs are available
as workflow artifacts.

### Signed builds and releases

For signed builds, create `supertube/keystore.properties` (phone app) and `keystore.properties` in the repository root
(TV app), both with `storeFile`, `storePassword`, `keyAlias` and `keyPassword`. Then run
`./gradlew :supertube:assembleStbetaRelease`.

To publish a version, set the same `versionName` in both apps, add a dated section to the [changelog](CHANGELOG.md) and
run `tools/release/release.sh`. It runs the tests, builds and checks both signed apps, writes the TV update manifest,
creates the GitHub release with every APK and regenerates the website. `--dry-run` stops after the build.

The signed APKs stay in `dist/` (not in git), and the website hosts the newest ones in `landing/apk/`. After every
release, upload `landing/` and rename `_htaccess` to `.htaccess` on the server (see
[landing/server/](landing/server/README.md) for Apache and nginx).

### Website and promotional images

The website (`landing/`, 6 languages) and the social network images (`promo/`) are generated with:

```bash
python3 tools/landing/gen_landing.py
python3 tools/landing/promo.py
```

## Architecture

SuperTube reuses SmartTube's MVP architecture: every presenter in the `common` module talks to a `View` interface
(`BrowseView`, `PlaybackView`, `SearchView`, `AppDialogView`…). The TV app implements those views with Leanback
fragments; **SuperTube implements them with Compose activities**, so the business logic is shared unchanged.

| Module | Description |
| --- | --- |
| `supertube` | The mobile app: Compose UI, playback service, favorites, downloads, widget, updater |
| `common` | SmartTube business logic: presenters, player engine (ExoPlayer), settings |
| `smarttubetv` | The Android TV app (Leanback UI, backdrops, lite mode); module name kept to merge upstream changes |
| `MediaServiceCore`, `SharedModules` | SmartTube submodules (YouTube API, utilities) |
| `landing`, `promo`, `tools` | Website, promotional images, and release and generator scripts |

Notable mobile-only pieces in `supertube/src/main/java/.../mobile/`:

| Path | Purpose |
| --- | --- |
| `ui/playback/` | Player screen, controls, gestures and mini player |
| `playback/` | Media session, notification and `mediaPlayback` foreground service |
| `ui/feed/` | Feed layouts shared by every screen |
| `download/` | Resumable downloads, track joining and Wi‑Fi only mode |
| `prefs/`, `widget/`, `update/` | Favorites and settings, home screen widget, updater |

## Contributing

- **Bugs and ideas:** open an [issue](https://github.com/jgestal/SuperTube/issues/new/choose) with the bug report or
  feature request template. Check the [FAQ](https://gestal.es/supertube/#faq) and the existing issues first, and
  include the app version, the device and the Android version.
- **Pull requests:** keep them focused on one change, and run the unit tests and lint before opening them. Problems in
  the YouTube engine (videos that don't load, sign-in) usually belong upstream in
  [SmartTube](https://github.com/yuliskov/SmartTube) or [MediaServiceCore](https://github.com/yuliskov/MediaServiceCore).
- **Translations:** every new string needs to be added to all 20 `strings.xml` files.

## Legal notice

YouTube's Terms of Service don't allow third-party clients or ad blocking. Downloading content may infringe copyright
unless you have the rights to it or your local law allows it. The download feature is meant for **personal use** of
content you are allowed to save, such as the Creative Commons films by the Blender Foundation shown in the screenshots.
Do not redistribute copyrighted content. How you use the app is your decision and your responsibility.

SuperTube is not affiliated with, endorsed by or connected to YouTube, Google or the SmartTube project. YouTube, Android
and Google TV are trademarks of Google LLC.

## Credits and license

SuperTube is based on **[SmartTube](https://github.com/yuliskov/SmartTube)** by **yuliskov** and its modules
[MediaServiceCore](https://github.com/yuliskov/MediaServiceCore) and [SharedModules](https://github.com/yuliskov/SharedModules),
released under the MIT License. All the credit for the YouTube engine and the original features goes to them. The
original README is kept in [README_SmartTube.md](README_SmartTube.md).

SuperTube is released under the [MIT License](LICENSE) © 2026 Juan Gestal. ExoPlayer (`exoplayer-amzn-2.10.6`) is
licensed under Apache 2.0. The YouTube engine in `MediaServiceCore` uses
[Java Regex](https://github.com/florianingerl/com.florianingerl.util.regex) by Florian Ingerl, licensed under GPL 2.0;
its source is available at that link. Screenshots show Creative Commons films by the Blender Foundation, without a
signed-in account.

Contact: [juan@gestal.es](mailto:juan@gestal.es)
