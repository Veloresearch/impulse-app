<p align="center">
  <img src="docs/mark.svg" width="96" alt="Impulse" />
</p>

<h1 align="center">Impulse for Android</h1>

<p align="center">
  Your own AI, in your pocket. The model runs on your computer; this app is how you reach it.
</p>

---

Impulse runs language models on your own PC. This app connects your phone to it, so you can chat
with your models, use your contexts and change a few settings from anywhere: the same Wi-Fi,
your Tailscale network, or a Cloudflare Tunnel.

Nothing goes through our servers. Your chats stay on your phone and your computer.

## What it does

- **Chat** with the model loaded in Impulse, with streamed answers and the model's thinking folded
  above each reply.
- **Contexts**: pick one of the document collections you built in Impulse and every answer can use it.
- **Home**: is your computer reachable, which model is loaded, your recent chats.
- **Settings**: accent colour, reduced motion, the computer you are connected to.

## Getting connected

You need Impulse Preview 4.2 or newer on your computer.

1. On the computer, open **Settings › Phone** in Impulse.
2. Choose how the phone should reach it: **Local network**, **Tailscale** or **Cloudflare**.
3. In the app, tap **Scan code** and point the camera at the code Impulse shows.
4. Pick your profile and type its password.

| Route | When to use it | What you need |
| --- | --- | --- |
| Local network | Phone and PC on the same Wi-Fi | Nothing else |
| Tailscale | From anywhere, privately | Tailscale on both devices, same account |
| Cloudflare Tunnel | From anywhere, through a public address | A Cloudflare account for your own tunnel; none for a quick link |

A Cloudflare Tunnel can sit behind Cloudflare Zero Trust. The code Impulse shows then carries an
Access service token, and the app sends it with every request.

You can also type the address by hand: `192.168.1.20`, `pc.tailnet.ts.net` or a full `https://` URL.

## Build

Requirements: JDK 17 or newer, the Android SDK (platform 35).

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`. Install it with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Project layout

```
app/src/main/java/com/velocity/impulse/
  data/            Impulse API client, connection parsing, chat storage, app state
  ui/theme/        colours, type, radii, motion curves, the continuous-corner shape
  ui/components/   buttons, toggles, segmented control, lists, sheet, markdown, the mark
  ui/screens/      connect, sign in, home, chat, contexts, settings, QR scanner
  ui/App.kt        the opening, tab bar and page transitions
```

## Design

The app follows the Impulse desktop design: solid surfaces separated by tone rather than lines,
continuous corners, one accent colour, and motion that explains where things came from. Every
press compresses and springs back; lists unfold in place; the tab highlight travels. Turning on
**Reduce motion** replaces movement with cross-fades.

## Privacy

The app talks only to the address you give it. It stores that address, your session, your
settings and your chats on the phone. The camera is used only while you scan a code, and nothing
is recorded.

## License

Apache License 2.0. See [LICENSE](LICENSE).
