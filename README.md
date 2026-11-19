# Arena Map Lab

Standalone Android tactical map notebook. This project is intentionally offline and does not connect to, inspect, patch, or control Liên Quân Mobile.

## Included in v1.0.0

- No key, account, or login screen.
- Manual tactical map board with three lane paths, jungle regions, bases, and generic objective points.
- Four manual marker types: ping, gank, objective, and danger.
- Undo last marker and clear all markers.
- Three independent countdown timers. Their initial values (60/120/180 seconds) are editable and are only sample presets, not a claim about in-game respawn times.
- Local notes saved on the device.
- Local persistence for map markers and notes.
- No network, overlay, accessibility service, background service, game-memory access, or game-file modification.

## Important scope

This is a self-contained manual planning board. It does **not** remove Liên Quân's HUD, reveal fog-of-war, read live enemy positions, or display information from a running match. Those functions would require integration with or modification of the game, which this independent app deliberately does not do.

## Build an installable debug APK

The current workspace used to create this source package does not contain the Android SDK, Gradle, or the Android build-tools, and outbound downloads are unavailable. Therefore no compiled APK is included in this delivery.

To build the APK on a machine with Android Studio:

1. Install Android Studio and Android SDK Platform 35.
2. Open this `ArenaMapLab` folder as an existing project.
3. Allow Gradle sync. The project uses Android Gradle Plugin `8.7.3`, Java 17 compatibility, `minSdk 23`, and `targetSdk 35`.
4. Choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Android Studio will create a debug-signed APK at `app/build/outputs/apk/debug/app-debug.apk`.
6. Transfer that APK to the Android device and install it. The debug build is for testing; create a release signing key in Android Studio before publishing.

## Package details

- Application ID: `com.example.arenamaplab`
- App name: `Arena Map Lab`
- Version: `1.0.0` (versionCode 1)
- Language: Java
- UI: Android Views + custom Canvas map
- Third-party dependencies: none

## Project tree

```text
ArenaMapLab/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/arenamaplab/MainActivity.java
│       └── res/
│           ├── drawable/ic_launcher.xml
│           └── values/{strings.xml,styles.xml}
├── build.gradle
├── gradle.properties
├── settings.gradle
└── README.md
```
