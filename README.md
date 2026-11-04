<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="docs/header-dark.png">
    <img src="docs/header-light.png" width="720" alt="Impulse App by Velocity">
  </picture>
</p>

<h3 align="center">Your own AI, in your pocket.</h3>

<p align="center">
  The models run on your computer, in <a href="https://github.com/Veloresearch/impulse">Impulse</a>.<br>
  This app puts them in your hand: chat, search the web, read your files, talk, from anywhere.
</p>

<p align="center">Made by Velocity, an independent AI company.</p>

<p align="center"><b>Preview.</b> This is an early version of the app: everything works, and it is still changing fast.<br>It updates itself as new previews come out.</p>

<p align="center">
  <a href="https://github.com/Veloresearch/impulse-app/releases/latest/download/Impulse-arm64-v8a.apk"><img alt="Android ARM64, most phones" src="https://img.shields.io/badge/android-ARM64%20%C2%B7%20most%20phones-8a7aa8?style=for-the-badge&labelColor=2d2b32"></a>
  <a href="https://github.com/Veloresearch/impulse-app/releases/latest/download/Impulse-armeabi-v7a.apk"><img alt="Android ARMv7, older phones" src="https://img.shields.io/badge/ARMv7-older%20phones-6b5d85?style=for-the-badge&labelColor=2d2b32"></a>
  <a href="https://github.com/Veloresearch/impulse-app/releases/latest/download/Impulse-x86_64.apk"><img alt="x86_64, Chromebooks and emulators" src="https://img.shields.io/badge/x86__64-chromebooks-6b5d85?style=for-the-badge&labelColor=2d2b32"></a>
  <a href="https://github.com/Veloresearch/impulse-app/releases/latest/download/Impulse-universal.apk"><img alt="Universal APK" src="https://img.shields.io/badge/universal-any%20device-6b5d85?style=for-the-badge&labelColor=2d2b32"></a>
</p>

<p align="center">
  <img alt="Latest release" src="https://img.shields.io/github/v/release/Veloresearch/impulse-app?include_prereleases&label=latest&color=8a7aa8&labelColor=2d2b32">
  <img alt="Downloads" src="https://img.shields.io/github/downloads/Veloresearch/impulse-app/total?color=8a7aa8&labelColor=2d2b32">
  <img alt="Android 8 and newer" src="https://img.shields.io/badge/android-8%2B-8a7aa8?labelColor=2d2b32">
  <img alt="License" src="https://img.shields.io/badge/license-Apache%202.0-8a7aa8?labelColor=2d2b32">
  <img alt="Open source" src="https://img.shields.io/badge/source-open-8a7aa8?labelColor=2d2b32">
</p>

<p align="center">
  <a href="https://github.com/Veloresearch/impulse-app/releases">Release notes</a> ·
  <a href="https://veloresearch.com">veloresearch.com</a> ·
  <a href="https://github.com/Veloresearch/impulse">Impulse for your computer</a>
</p>

<!-- SCREENS -->

## What it does

- **Chat with the models on your computer.** Answers stream in as they are written, the model's thinking folds above each reply, and every chat syncs with Impulse on the desktop both ways.
- **The web and your plugins.** The model searches the web, reads pages and calls the MCP plugins you set up in Impulse, and shows where its answer came from.
- **Pictures and files.** Take a photo, pick from your gallery or add a document: PDF, Word, Excel, PowerPoint, text and code. Save what you see to your phone.
- **Momentum.** Each chat keeps its own memory across the conversation, and you can pick one of your contexts for every answer to draw on. The chat's details show how much it holds and what each answer recalled.
- **Your models, from your phone.** See what is loaded, switch models, set how hard they think, and download new ones to your computer.
- **Talk.** Dictate a question, or switch to voice and just talk.
- **Made to be held.** Dark, light and AMOLED themes, one accent colour, haptics and motion that follow your finger.

## Get started

1. **Install Impulse on your computer** from [its releases](https://github.com/Veloresearch/impulse/releases/latest). It is where the models run.
2. **Install this app.** Download the APK for your phone from the buttons above (ARM64 fits almost every phone from the last eight years) and open it. Android asks once to allow installs from your browser or file manager.
3. **Connect.** Open the app: computers running Impulse on the same Wi-Fi show up on their own. Tap yours, or scan the code from **Impulse › Settings › Phone**.
4. **Sign in** with your Impulse profile's password. That's it.

The app updates itself: when a new version is out it tells you, downloads it and hands it to Android to install.

### From anywhere

| Route | When | What you need |
| --- | --- | --- |
| Same Wi-Fi | At home or in the office | Nothing; the app finds Impulse |
| Tailscale | Anywhere, privately | Tailscale on the computer and the phone, same account |
| Cloudflare Tunnel | Anywhere, through a public address | A quick link with no account, or your own tunnel behind Zero Trust |

Choose the route in **Impulse › Settings › Phone** and scan the code it shows; the code carries everything the app needs.

## Privacy

The app talks only to the computer you connect it to. Your chats live on that computer and sync to the phone; nothing goes through our servers, because there are none in the middle. The camera is used only while you scan a code or take a photo for a question. Your session is kept in the phone's secure storage.

## Build it yourself

You need JDK 17 or newer and the Android SDK (platform 35).

```bash
./gradlew assembleDebug
```

The APKs land in `app/build/outputs/apk/`. Install one with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Every release on GitHub also carries the full source code as a zip.

## Project layout

```
app/src/main/java/com/velocity/impulse/
  data/            the Impulse API, chats and sync, tools, attachments, settings
  ui/theme/        colours, type, motion, the continuous-corner shape
  ui/components/   controls, morphs, glass, haptics, markdown, avatars, model faces
  ui/screens/      chat, drawer, details, models, contexts, settings, connect, voice
  ui/App.kt        the shell: one conversation on floating islands
```

## License

Apache License 2.0. See [LICENSE](LICENSE). Inter and JetBrains Mono are under the SIL Open Font License, the Phosphor icons under MIT; see [docs/licenses](docs/licenses).
