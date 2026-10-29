# IPTV for Android TV

Netflix-style Android TV app that plays the free, public channels listed by
[iptv-org/iptv](https://github.com/iptv-org/iptv).

## How the data works

The iptv-org/iptv repository does not have a server API. Its M3U playlists are
generated from the public JSON API at **https://iptv-org.github.io/api/**
([iptv-org/api](https://github.com/iptv-org/api)). This app uses that JSON API
directly, because it has much richer data than the M3U files:

| File              | Used for                                         |
|-------------------|--------------------------------------------------|
| `streams.json`    | Stream URL, quality, labels, User-Agent, Referer |
| `channels.json`   | Name, country, categories, NSFW flag, closed     |
| `logos.json`      | Channel logos (best one is picked per channel)   |
| `countries.json`  | Country name and flag                            |
| `categories.json` | Category names                                   |
| `blocklist.json`  | Channels removed for legal reasons (always hidden) |

`CatalogAssembler` joins these files on-device (streamed, item by item, to keep
memory low) and stores the result in Room. The UI only reads from Room.
The catalog refreshes in the background when it is older than 24 hours, or
from **Settings → Update now**.

Rules:
- Only channels with at least one stream are shown.
- Closed and blocklisted channels are hidden.
- Streams that are not linked to a channel (no metadata) are skipped.
- When a stream fails, the player tries the channel's next stream.

## Features

- Home: hero banner of the focused channel + rows ("Live in Indonesia", then categories).
- Left navigation rail: Search, Home, Countries, Categories, Settings, Created By.
- Player: Media3 ExoPlayer (HLS + DASH). ▲/▼ or CH+/CH− switches channel, with a 15-second loading countdown indicator while streams connect.
- Created By: Developer profile with quick links to GitHub ([iptv-android-tv](https://github.com/mrandiw/iptv-android-tv)) and Instagram ([@codewithandiw](https://www.instagram.com/codewithandiw/)).
- Adult (NSFW) channels are visible but locked behind a 4-digit PIN.
  Default PIN is **0000**, change it in Settings. Unlock lasts until the app is closed.

## Architecture

```
app/src/main/java/com/iptvtv/
├── data/
│   ├── remote/          IptvApi (OkHttp + kotlinx.serialization streaming), DTOs
│   ├── local/           Room: entities, ChannelDao, AppDatabase
│   ├── CatalogAssembler join + filter logic (pure Kotlin, unit tested)
│   ├── ChannelRepository single source of truth, sync
│   ├── SettingsStore    DataStore: home country, PIN hash, last sync
│   └── NsfwGate         session unlock state
├── model/               domain models
└── ui/                  Compose for TV (androidx.tv.material3), one ViewModel per screen
```

Single module, MVVM, manual DI (`AppContainer`).

## Build

Requirements: Android Studio (latest stable), JDK 17+ (Android Studio's bundled JDK works), Android SDK 37.

1. Open this folder in Android Studio and let Gradle sync.
2. Create an Android TV emulator (Device Manager → TV → 1080p), or connect a TV with ADB.
3. Run the `app` configuration.

Command line:

```bash
./gradlew testDebugUnitTest assembleDebug
```

## Installing on Android TV via ADB

### 1. Enable Developer Options on TV
1. On your TV, go to **Settings** → **Device Preferences** (or **System**) → **About**.
2. Scroll down to **Android TV OS build** (or **Build**) and press Select/OK **7 times** until you see *"You are now a developer!"*.

### 2. Enable Debugging
1. Go back to **Settings** → **Device Preferences** (or **System**) → **Developer options**.
2. Toggle on **USB debugging** (and **Network debugging** if listed separately).

### 3. Find Your TV's IP Address
1. Ensure your computer and TV are on the same Wi-Fi / local network.
2. Go to **Settings** → **Network & Internet** → select your connected network.
3. Note the **IP address** (e.g. `192.168.1.50`).

### 4. Connect over ADB
From your computer terminal, connect to the TV:

```bash
adb connect <TV_IP_ADDRESS>:5555
# Example: adb connect 192.168.1.50:5555
```

> Look at your TV screen: when prompted with **"Allow USB debugging?"**, check **"Always allow from this computer"** and select **OK**.

Verify the connection:

```bash
adb devices
# Expected output:
# 192.168.1.50:5555    device
```

### 5. Build and Install

1. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```

2. Install to the connected TV:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### Troubleshooting
- **Connection refused / cannot connect:** Make sure TV and PC are on the same subnet, port `5555` is reachable, and the TV hasn't gone into sleep mode. Try running `adb kill-server && adb start-server` then reconnecting.
- **Unauthorized device:** Re-run `adb connect <TV_IP_ADDRESS>:5555` while the TV screen is active, and accept the authorization prompt on screen.
- **Multiple devices connected:** Specify the target TV by adding `-s <TV_IP_ADDRESS>:5555` to ADB commands (e.g., `adb -s 192.168.1.50:5555 install -r ...`).

## Notes

- Package name `com.iptvtv` is a placeholder; change `namespace`/`applicationId` before publishing.
- Many streams are plain HTTP, so cleartext traffic is allowed (`network_security_config.xml`).
- The app hosts no content; availability of each stream depends on its source.
