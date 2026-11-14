<h1 align="center">Spotiboost</h1>

<p align="center">Android client for the Spotify web player, with background playback.</p>

<p align="center"><b>English</b> · <a href="README.es.md">Español</a></p>

<p align="center">
    📘 Also available: <a href='https://github.com/Ufoex/Faceboost'><b>Faceboost</b></a>, the same idea for Facebook.<br>
    📸 And <a href='https://github.com/Ufoex/Instaboost'><b>Instaboost</b></a>, for Instagram.
</p>

<h3 align="center">📥 Download</h3>

<p align="center">
    Get the latest APK from the releases page and install it.<br><br>
    <a href='https://github.com/Ufoex/Spotiboost/releases/latest'><img alt='Download' height='40' src='https://img.shields.io/github/v/release/Ufoex/Spotiboost?style=for-the-badge&logo=github&label=Download&color=1DB954&labelColor=24292F'/></a>
</p>

<h3 align="center">📊 Downloads</h3>

<p align="center">
    Total number of times the app has been downloaded so far.<br><br>
    <a href='https://grev.shehryar.ae/?owner=Ufoex&repo=Spotiboost'><img alt='Downloads' height='40' src='https://img.shields.io/github/downloads/Ufoex/Spotiboost/total?style=for-the-badge&logo=github&label=Downloads&color=1DB954&labelColor=24292F'/></a>
</p>

<h3 align="center">🙋 Problem or suggestion?</h3>

<p align="center">
    Something broken, or an idea? Open an issue on GitHub and tell me what happened or what you would like.<br><br>
    <a href='https://github.com/Ufoex/Spotiboost/issues/new/choose'><img alt='Open an issue' height='40' src='https://img.shields.io/badge/Open%20an%20issue-D73A4A?style=for-the-badge&logo=github&logoColor=white'/></a>
</p>

<h3 align="center">☕ Like my work?</h3>

<p align="center">
    The app is free and has no ads. If you like it, you can support me with a coffee.<br><br>
    <a href='https://buymeacoffee.com/masterllamk'><img alt='Buy me a coffee' height='40' src='https://img.shields.io/badge/Buy%20me%20a%20coffee-FFDD00?style=for-the-badge&logo=buymeacoffee&logoColor=black'/></a>
</p>

Spotiboost is an Android client for the Spotify web player (`open.spotify.com`). It loads the site inside [GeckoView](https://mozilla.github.io/geckoview/) (Firefox's engine) and injects an extension that adapts the interface to a phone.

## Features

- Mobile interface: bottom bar (Home, Search, Library), expanded player, queue, devices and full-screen lyrics.
- Background and screen-off playback: foreground service, notification and lock-screen controls.
- English and Spanish, following the phone's language.
- Warning when battery saver is on or notifications are missing, since either can cut the music.
- Update check: the app looks for new releases here and shows the release notes in English or Spanish.

Not affiliated with Spotify. You need your own account; audio plays through the device's own Widevine DRM.

## Why GeckoView and not the system WebView

In Android's WebView the Spotify web player never gets to request the audio. In GeckoView it does, with three tweaks (DRM permission, autoplay, and Service Workers disabled, which broke the covers); they are commented in `MainActivity.java`.

## Install

Download the APK from the [latest release](https://github.com/Ufoex/Spotiboost/releases/latest) and open it. arm64 only (almost all current phones). Later updates can be installed from inside the app.

## Build

Requires JDK 17 and the Android SDK (platform 36).

```sh
./gradlew assembleDebug     # app-debug.apk, with debugging and diagnostics
./gradlew assembleRelease   # app-release.apk
```

The debug build installs as "Spotiboost Debug" (`com.ufoex.spotiboost.gecko`), so it coexists with the release (`com.ufoex.spotiboost`).

## Publishing a release

1. Add a `## Spotiboost vX.Y.Z` section at the top of `CHANGE.md` with `<!-- lang:en -->` and `<!-- lang:es -->` blocks (these are the notes shown in the release and in the app's update prompt).
2. Create the tag and push it; GitHub Actions runs the tests, builds and publishes the APK to Releases:

```sh
git tag vX.Y.Z && git push origin vX.Y.Z
```

On launch the app checks GitHub for the latest version and, if it is newer than the installed one, offers to download and install it.

To sign with your own key, set the repo secrets `KEYSTORE_BASE64` (`base64 -w0 release.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`. Without them the release is signed with the debug key: it installs, but won't update over another signature.

## Support

The app is free and has no ads. If it is useful to you and you want to say thanks, you can [buy me a coffee](https://buymeacoffee.com/masterllamk).

## License

MIT. GeckoView (MPL 2.0) is downloaded as a dependency and is not included in this repository.

Spotiboost is not affiliated with or endorsed by Spotify. Spotify is a trademark of Spotify AB.
