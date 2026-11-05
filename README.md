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
  <a href="https://github.com/jgestal/SuperTube/releases/latest"><img alt="Download" src="https://img.shields.io/github/v/release/jgestal/SuperTube?label=download&style=for-the-badge&color=E53935"></a>
  <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white">
  <a href="LICENSE"><img alt="License MIT" src="https://img.shields.io/badge/license-MIT-blue?style=for-the-badge"></a>
</p>

<p align="center">
  <img src="docs/screenshots/hero.webp" width="820" alt="SuperTube: home feed with the mini player, search and favorites">
</p>

<p align="center">
  <a href="#features">Features</a> ·
  <a href="#supertube-for-android-tv">Android TV</a> ·
  <a href="#download">Download</a> ·
  <a href="#building">Building</a> ·
  <a href="#architecture">Architecture</a> ·
  <a href="CHANGELOG.md">Changelog</a> ·
  <a href="#en-español">Español</a>
</p>

## Why SuperTube?

[SmartTube](https://github.com/yuliskov/SmartTube) is an excellent YouTube client, but it is built for TV remotes: a
fixed 1080p "10-foot" interface, D-pad focus navigation and Leanback screens. On a phone it looks tiny and is awkward to
use with your fingers.

**SuperTube keeps SmartTube's engine** — no ads, SponsorBlock, DeArrow, accounts, history, playlists, quality and codec
selection — and **replaces the whole interface** with one designed for touch screens, following the
[Android adaptive design guidelines](https://developer.android.com/develop/ui/compose/layouts/adaptive).

## Features

### A real mobile interface

Jetpack Compose and Material 3 with light and dark themes and dynamic colors. Navigation adapts to the window: a bottom
bar on phones and a navigation rail in landscape, on tablets and on foldables. Videos can be shown as a **list**, a
**compact list**, a **grid** or **horizontal shelves**, and the choice is remembered.

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

The ♡ on every video saves it to *Favorites* or to your own folders. Folders can be searched, renamed and played in
order or shuffled. A **home screen widget** shows them, and everything can be **exported and imported** as a JSON file.
Favorites are stored only on your device.

<p align="center">
  <img src="docs/screenshots/favorites.webp" width="250" alt="A favorites folder">
  <img src="docs/screenshots/save_to.webp" width="250" alt="Save to folder sheet">
  <img src="docs/screenshots/widget.png" width="250" alt="Home screen widget">
</p>

### Downloads manager

Download the **audio** or the **video** (up to 1080p H.264, joined on the device without re-encoding) after choosing the
quality and seeing its size. Downloads can be **paused and resumed**, even after closing the app; expired links are
refreshed automatically. Optional **Wi‑Fi only** mode, video and audio filters, an offline player, and files saved to
`Movies/SuperTube` and `Music/SuperTube`.

<p align="center">
  <img src="docs/screenshots/download_sheet.webp" width="250" alt="Download quality picker">
  <img src="docs/screenshots/search_mini.webp" width="250" alt="Search with the mini player">
</p>

> Without a YouTube account, YouTube only offers single-file streams (usually 360p); sign in for up to 1080p.

### Also included

- **Search** with local recent searches, voice search and filters.
- **Reorderable sections** in *More*, settings as bottom sheets, and TV-only settings hidden.
- **Opens YouTube links** shared or tapped in other apps. On Android 12+ it has to be allowed once in
  *Settings › SuperTube › Open YouTube links*, which explains the steps (the YouTube app, if installed, keeps the
  links until its *Open supported links* is turned off).
- **Updates** from the releases of this repository (*Settings › SuperTube › Check for updates*).
- **20 languages**, selectable per app in the Android 13+ system settings.
- Everything from SmartTube: no ads, SponsorBlock, DeArrow, sign-in (subscriptions, history, playlists), 4K/8K, HDR,
  60 fps, codec selection, subtitles, live chat and comments.

## SuperTube for Android TV

The TV app (`smarttubetv` module) keeps SmartTube's remote-friendly Leanback interface and adds:

- **Backdrops**: the focused video fills the background, Netflix-style, faded so the text stays readable. Without a
  thumbnail the home screen shows a SuperTube ambient image.
- **Lite mode for slow TVs and TV boxes** (*Settings › User interface*): no background images, card previews, scrolling
  titles or seek thumbnails, and a smaller video buffer.
- SuperTube name, icons, launcher banner and home screen channel, and updates from this repository.

<p align="center">
  <img src="docs/screenshots/tv_backdrop.webp" width="640" alt="SuperTube for Android TV: the focused video fills the background">
</p>
<p align="center">
  <img src="docs/screenshots/tv_settings.webp" width="316" alt="TV user interface settings with lite mode">
  <img src="docs/screenshots/tv_lite_mode.webp" width="316" alt="TV home screen in lite mode">
</p>

## Download

Get the APK from the **[latest release](https://github.com/jgestal/SuperTube/releases/latest)**. Later versions can be
installed from *Settings › SuperTube › Check for updates*.

- Requires **Android 8.0** or newer (ARM 32/64-bit); ready for devices with 16 KB memory pages.
- SuperTube is a separate app (`es.gestal.supertube`; the TV app is `es.gestal.supertube.tv`) and can be installed
  next to SmartTube. TV APKs are named `SuperTubeTV_stable_<version>_<abi>.apk`.
- Release signing certificate (SHA-256):
  `f6:fb:28:69:7f:24:d1:bf:21:ff:12:b8:20:e2:64:ca:4a:94:2f:3e:e8:4c:75:f2:01:6d:eb:3c:e6:07:70:41`

SuperTube is distributed only through GitHub, so it doesn't include Google Play Services features such as Chromecast or
Android Auto. See the [changelog](CHANGELOG.md) for what changed in each version.

## Building

```bash
git clone --recurse-submodules https://github.com/jgestal/SuperTube.git
cd SuperTube
# Gradle 7.5 runs on JDK 11-17
JAVA_HOME=/path/to/jdk-17 ./gradlew :supertube:assembleStbetaDebug
```

The APK is generated in `supertube/build/outputs/apk/stbeta/debug/`. For signed release builds, create
`supertube/keystore.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`) and run
`./gradlew :supertube:assembleStbetaRelease`.

The TV app:

```bash
JAVA_HOME=/path/to/jdk-17 ./gradlew :smarttubetv:assembleStstableDebug   # es.gestal.supertube.tv
```

Checks before a release:

```bash
./gradlew :supertube:testStbetaDebugUnitTest :supertube:lintStbetaDebug   # unit tests and lint
supertube/tools/rotation_e2e.sh path/to/SuperTube.apk                      # fullscreen and rotation, on an emulator
```

To publish a version (same `versionName` in both apps and a dated CHANGELOG section), `tools/release/release.sh`
runs the tests, builds and checks both signed apps, writes the TV update manifest, creates the GitHub release with
every APK and regenerates the website. `--dry-run` stops after the build. The signed APKs stay in `dist/` (not in
git) and the website hosts the newest ones in `landing/apk/`: upload `landing/` after every release and rename `_htaccess` to `.htaccess` on the server. The TV app is signed with the root
`keystore.properties` (same format as the phone one).

The website (`landing/`) and the social network images (`promo/`) are generated with
`python3 tools/landing/gen_landing.py` and `python3 tools/landing/promo.py`.

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

Notable mobile-only pieces in `supertube/src/main/java/.../mobile/`:

| Path | Purpose |
| --- | --- |
| `ui/playback/` | Player screen, controls, gestures and mini player |
| `playback/` | Media session, notification and `mediaPlayback` foreground service |
| `ui/feed/` | Feed layouts shared by every screen |
| `download/` | Resumable downloads, track joining and Wi‑Fi only mode |
| `prefs/`, `widget/`, `update/` | Favorites and settings, home screen widget, updater |

## Downloads notice

Downloading content may be against YouTube's Terms of Service. The download feature is provided for **personal use** of
content you have the right to save (for example the Creative Commons Blender open movies shown above). Do not
redistribute copyrighted content.

## Credits and license

SuperTube is based on **[SmartTube](https://github.com/yuliskov/SmartTube)** by **yuliskov** and its modules
[MediaServiceCore](https://github.com/yuliskov/MediaServiceCore) and [SharedModules](https://github.com/yuliskov/SharedModules),
released under the MIT License. All the credit for the YouTube engine and the original features goes to them. The
original README is kept in [README_SmartTube.md](README_SmartTube.md).

SuperTube is released under the [MIT License](LICENSE). ExoPlayer (`exoplayer-amzn-2.10.6`) is licensed under Apache 2.0.
The YouTube engine in `MediaServiceCore` uses [Java Regex](https://github.com/florianingerl/com.florianingerl.util.regex)
by Florian Ingerl, licensed under GPL 2.0; its source is available at that link.
Screenshots show Creative Commons films by the Blender Foundation, without a signed-in account.

SuperTube is not affiliated with, endorsed by or connected to YouTube, Google or the SmartTube project.

---

## En español

**SuperTube** es un cliente de YouTube sin anuncios para móviles y tablets Android, basado en SmartTube (diseñado para
TV). Mantiene todo su motor y sustituye la interfaz por una táctil con Material 3. También hay versión para
**Android TV**, con imágenes de fondo al estilo Netflix y un **modo ligero** para teles y TV boxes lentos. Más
información en [gestal.es/supertube](https://gestal.es/supertube/).

- **Navegación adaptativa**: barra inferior o lateral, y cuatro vistas de vídeos (lista, compacta, cuadrícula y carrusel).
- **Reproductor táctil**:
  - Doble toque para avanzar o retroceder, mantener pulsado para 2× y **botón de velocidad** (1× · 1,5× · 2×).
  - Gestos de brillo y volumen, **mini reproductor**, imagen en imagen y **temporizador de apagado**.
  - Reproducción en segundo plano con **controles en la pantalla de bloqueo** y en las notificaciones.
- **Favoritos con carpetas**, búsqueda, reproducción continua o aleatoria, **widget** y exportación/importación.
- **Gestor de descargas** de audio y vídeo con selector de calidad, modo solo Wi‑Fi y pausa/reanudación.
- **20 idiomas** y actualizaciones desde este repositorio.

Descarga la última versión en la [página de Releases](https://github.com/jgestal/SuperTube/releases/latest)
(requiere Android 8.0 o superior). Los cambios de cada versión están en el [CHANGELOG](CHANGELOG.md).
