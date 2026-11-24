# Cinny Native Android (Rust Core + Jetpack Compose)

Inspired by [cinnyapp/cinny](https://github.com/cinnyapp/cinny), this project implements a native Android Matrix client where Matrix protocol synchronization, SQLite encrypted persistence, Space DAG resolution, timeline & thread diffing, encrypted media transfer, UnifiedPush decryption, and Olm/Megolm/4S E2EE run in **Rust (`matrix-sdk 0.16` + `matrix-sdk-ui 0.16`)**, bridged to **Jetpack Compose Material 3** via **UniFFI 0.29**.

## Architecture

- `rust-core/` (`cinny_core` v0.3.0):
  - `client.rs`: `SyncService` + `RoomListService` (with `sync_stream` fallback), Matrix SSO/OIDC `login_with_sso_token`, `CinnyAuthSession` restore/export, isolated per-session encrypted SQLite store.
  - `spaces.rs`: Nested Space DAG hierarchy, `browse_space_hierarchy` (discover & join unjoined Space rooms), room creation with recommended Megolm E2EE, alias/ID join, invites, leave, and member listing.
  - `timeline.rs`: `matrix_sdk_ui::Timeline` + `TimelineFocus::Thread` threads, `send_reply`, `edit`, `redact`, `paginate_backwards`, `mark_as_read`, and local E2EE message search.
  - `media.rs`: E2EE AES-CTR attachment encryption (`send_media_attachment`) & `mxc://` local disk decryption cache (`download_media_to_cache`).
  - `crypto.rs`: Bidirectional SAS emoji verification (`confirm`/`cancel`/device list) plus 4S Secret Storage recovery & Megolm room key backup (`recover_with_security_key`, `enable_recovery_and_backup`).
  - `push.rs`: UnifiedPush / Sygnal HTTP pusher registration (`set_pusher`) & background encrypted event decryption (`process_push_event`).
- `android/`: Native Kotlin + Jetpack Compose Material 3 Android application (`in.cinny.android`):
  - `CinnyApplication` + `CinnyViewModel` + `SecureSessionStore` (Android Keystore `EncryptedSharedPreferences` + 256-bit SQLite passphrase + `ProcessLifecycleOwner`).
  - `MainActivity` (`cinny://sso` Custom Tabs deep link, `POST_NOTIFICATIONS`, push notification tap routing, `BackHandler`).
  - `push/`: `CinnyUnifiedPushReceiver`, `PushSyncWorker` (WorkManager background Megolm decryption), `PushNotificationHelper`.
  - `ui/screens/`: `LoginScreen` (Password & SSO/OIDC), `HomeSpaceScreen` (Space Rail, nested sub-spaces, Space discovery, Create/Join/Invites), `RoomTimelineScreen` (Thread modal, encrypted media upload & preview, room search, members/invite), `CryptoVerifyDialog` (SAS + 4S Backup), `PushSettingsDialog`.
- `scripts/build-android-ndk.sh`: Generates UniFFI Kotlin bindings from the unstripped host library first, compiles Android `.so` targets via `cargo-ndk`, and strips debug symbols (`--strip-debug`) without removing `.dynsym`.

## Prerequisites

1. Install Rust toolchain & Android targets:
   ```bash
   rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
   cargo install cargo-ndk
   ```
2. Install Android SDK & NDK (r26+) and set `ANDROID_NDK_HOME`.

## Build & Test

```bash
# Run Rust unit tests
cd rust-core && cargo test

# Build NDK shared libraries & UniFFI Kotlin bindings
./scripts/build-android-ndk.sh

# Run Android unit tests & assemble debug APK
cd android
./gradlew testDebugUnitTest assembleDebug
```

## GitHub Actions CI

`.github/workflows/main.yml` automatically sets up JDK 17, Android SDK 35 + NDK, Rust targets + `cargo-ndk`, builds UniFFI Kotlin bindings and Android `.so` libraries, runs unit tests, builds the debug APK, and uploads `cinny-android-debug-apk` as a workflow artifact.

