# Matrix3 Mobile

## Goal

Ship Matrix3 as one Android APK that runs the revision-830 login server, game server, cache, and client locally on the device. After first-time cache installation, opening the app should start the local stack automatically and present the playable client without Termux, Windows, cloud hosting, or a second app.

## Current phase

`ACTIVE` — Phase 1: native Android host + local server runtime + 830 client port boundary.

## Phase 1 checklist

- [x] Add standalone `Mobile/` Android application project.
- [x] Keep Android Java source compatibility at Java 8.
- [x] Compile/package Matrix3 server source and existing server JAR dependencies through the mobile module.
- [x] Run login and game launchers in separate Android processes so shared Matrix3 static state is isolated.
- [x] Install bundled Matrix3 server data into app-private storage.
- [x] Add first-run revision-830 cache folder import into `data/cache/`.
- [x] Auto-start login server, wait for local readiness, then start game server and wait for game-port readiness.
- [x] Add Android `SurfaceView` host accepting ARGB client frames.
- [x] Add touch/key input bridge state for the client port.
- [x] Identify the first desktop client-host compatibility boundary without renaming obfuscated code.
- [ ] Make the existing revision-830 client source set compile for Android without renaming obfuscated classes/methods/fields.
- [ ] Replace/isolate `java.applet`, AWT/Swing, `ImageIO`, and desktop-only client host dependencies.
- [ ] Classify and replace native/desktop-only client libraries such as `jaclib.ping` where required.
- [ ] Feed the client software framebuffer into `MatrixClientSurfaceView.presentFrame(...)`.
- [ ] Route Android touch/key/IME input into the existing client input consumers.
- [ ] Start the ported 830 client automatically after both local servers report ready.
- [ ] Produce and install a debug APK on a real Android ARM64 device.
- [ ] Verify offline login and gameplay against `127.0.0.1`.

## Verified static findings

- `Server/src/main/java/com/rs/LoginLauncher.java` is the Matrix3 login entry point and accepts local/debug defaults.
- `Server/src/main/java/com/rs/GameLauncher.java` is the Matrix3 game entry point and accepts local/debug defaults.
- Local Matrix3 settings use loopback addresses; login server port is 7777 and game port is 43593.
- Matrix3 server source contains Windows-1252-era identifiers/strings. Mobile Linux CI must preserve that encoding instead of rewriting legacy identifiers.
- `Client/src/main/java/game/RS3Applet.java` is the desktop client host and already targets `127.0.0.1` in local mode.
- `Client/src/main/java/game/client.java` directly imports desktop-only AWT/image classes and `jaclib.ping`; the complete client source cannot be compiled by Android unchanged.
- `Client/src/main/java/game/Class584.java` owns the desktop `Applet`/`Frame`/`Canvas` lifecycle used by the client game loop.
- `Client/src/main/java/game/Class591_Sub5.java#method8786` selects the current AWT `Container` from fullscreen frame, normal frame, or applet.
- `Client/src/main/java/game/Canvas_Sub1.java` is a thin AWT `Canvas` wrapper that delegates `paint`/`update` to its backing component.

## Architecture

```text
Matrix3 Mobile APK
|
+-- Android MainActivity
|   +-- MatrixClientSurfaceView
|   +-- touch/key bridge
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

The existing Matrix3 `Client/` and `Server/` remain the source of truth. Mobile compatibility belongs in `Mobile/` or the smallest explicitly required client adapter; do not fork gameplay/server behavior into an Android-only implementation.

## Resume Here

Implement the first client-host compatibility slice around `Class584`, `Class591_Sub5.method8786`, and `Canvas_Sub1`: separate desktop AWT/Applet container ownership from the existing game-loop lifecycle and route the Android build to `MatrixClientSurfaceView` without renaming original classes/methods/fields. Do not touch combat, networking, cache semantics, or unrelated rendering systems. After the host compiles, let Android javac/D8 identify the next exact client-only desktop/native dependency.
