# Matrix3 Mobile

## Goal

Ship Matrix3 as one Android APK that runs the revision-830 login server, game server, cache, and client locally on the device. After first-time cache installation, opening the app should start the local stack automatically and present the playable client without Termux, Windows, cloud hosting, or a second app.

## Current phase

`ACTIVE` — Phase 1: native Android host + local server runtime + 830 client port boundary.

## Phase 1 checklist

- [x] Add standalone `Mobile/` Android application project.
- [x] Keep Android Java source compatibility at Java 8.
- [x] Compile the unchanged Matrix3 server as Java 8 bytecode and package it into the APK.
- [x] Produce a CI-built debug APK containing the Matrix3 server stack.
- [x] Run login and game launchers in separate Android processes so shared Matrix3 static state is isolated.
- [x] Install bundled Matrix3 server data into app-private storage.
- [x] Add first-run revision-830 cache folder import into `data/cache/`.
- [x] Auto-start login server, wait for local readiness, then start game server and wait for game-port readiness.
- [x] Add Android `SurfaceView` host accepting ARGB client frames.
- [x] Add touch/key input bridge state for the client port.
- [x] Identify the first desktop client-host compatibility boundary without renaming obfuscated code.
- [x] Add a build-time bytecode remapper that preserves Matrix3 class/method/field names while redirecting desktop Java UI API references into the Android compatibility namespace.
- [ ] Package the remapped revision-830 client classes into the Android APK.
- [ ] Implement the exact `java.applet`, AWT/Swing, and ImageIO compatibility types required by the remapped client.
- [ ] Classify and replace native/desktop-only client libraries such as `jaclib.ping` where required.
- [ ] Feed the client software framebuffer into `MatrixClientSurfaceView.presentFrame(...)`.
- [ ] Route Android touch/key/IME input into the existing client input consumers.
- [ ] Start the ported 830 client automatically after both local servers report ready.
- [ ] Install a client-enabled debug APK on a real Android ARM64 device.
- [ ] Verify offline login and gameplay against `127.0.0.1`.

## Verified static findings

- `Server/src/main/java/com/rs/LoginLauncher.java` is the Matrix3 login entry point and accepts local/debug defaults.
- `Server/src/main/java/com/rs/GameLauncher.java` is the Matrix3 game entry point and accepts local/debug defaults.
- Local Matrix3 settings use loopback addresses; login server port is 7777 and game port is 43593.
- Matrix3 server source contains Windows-1252-era identifiers/strings. Mobile Linux CI preserves that source encoding instead of rewriting legacy identifiers.
- Android source compilation of the complete server exposed dormant AWT tooling, so the mobile build now compiles the unchanged server through its normal Java toolchain first and feeds Java 8 bytecode to Android/D8. This path has produced a successful debug APK in CI.
- `Client/src/main/java/game/RS3Applet.java` is the desktop client host and already targets `127.0.0.1` in local mode.
- `RS3Applet.startClient()` only creates `game.client`, calls `supplyApplet(...)`, then `init()` and `start()`. The Swing `JFrame` is desktop host code and is not required as the Android launch surface.
- `Client/src/main/java/game/client.java` directly imports desktop-only AWT/image classes and `jaclib.ping`; the complete client cannot run on Android unchanged.
- `Client/src/main/java/game/Class584.java` owns the desktop `Applet`/`Frame`/`Canvas` lifecycle used by the client game loop.
- `Class584.supplyApplet(Applet)` stores the supplied host in the client static applet slot; this is the direct seam for an Android parameter/container host after bytecode remapping.
- `Client/src/main/java/game/Class591_Sub5.java#method8786` selects the current AWT `Container` from fullscreen frame, normal frame, or applet.
- `Client/src/main/java/game/Canvas_Sub1.java` is a thin AWT `Canvas` wrapper that delegates `paint`/`update` to its backing component.

## Architecture

```text
Matrix3 Mobile APK
|
+-- Android MainActivity
|   +-- MatrixClientSurfaceView
|   +-- touch/key bridge
|   +-- Android applet/container compatibility host
|
+-- remapped Matrix3 revision-830 client bytecode
|   +-- original game/* class names preserved
|   +-- desktop API refs -> com.matrix3.mobile.compat/*
|
+-- :login Android process
|   +-- com.rs.LoginLauncher
|
+-- :game Android process
|   +-- com.rs.GameLauncher
|
+-- app-private Matrix3 server home
    +-- data/server_data
    +-- data/accounts_data
    +-- data/cache        <- imported revision-830 cache
```

The existing Matrix3 `Client/` and `Server/` remain the source of truth. Mobile compatibility belongs in `Mobile/` or the smallest explicitly required adapter; do not fork gameplay/server behavior into an Android-only implementation.

## Resume Here

Run the client bytecode remapper in CI and capture its exact mapped desktop type set. Implement only the compatibility classes required by that report, beginning with the verified host chain `RS3Applet/startClient -> game.client -> Class584.supplyApplet -> Class591_Sub5.method8786 -> Canvas_Sub1`. Then package the remapped client JAR into the APK and add the Android client bootstrap. Do not touch combat, networking, cache semantics, or unrelated renderer logic until D8/runtime evidence requires it.
