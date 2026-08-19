# KMP-Learning-App

App for preparing for Android/KMP Compose interviews.

## Project Foundation

This repository is bootstrapped as a Kotlin Multiplatform project with thin
platform application shells and a shared Compose Multiplatform module.

Current Gradle modules:

- `:shared` - Kotlin Multiplatform shared code and shared Compose UI.
- `:androidApp` - Android application entry point that hosts shared UI.
- `:desktopApp` - Compose Desktop application entry point.
- `:webApp` - Compose web entry point for JS and Wasm browser targets.

The iOS application lives in `iosApp` and embeds the shared Compose UI through
the framework produced by `:shared`.

## Shared Source Sets

The shared module currently defines these platform targets:

- Android
- JVM
- JavaScript browser
- Wasm browser
- iOS ARM64
- iOS simulator ARM64

Shared code belongs in `shared/src/commonMain` only when it is genuinely
platform-independent. Platform-specific behavior belongs in the appropriate
platform source set.

## Verification

Useful foundation checks:

```sh
./gradlew :androidApp:assembleDebug
./gradlew :shared:check
./gradlew check
```

Use `./gradlew build` when a full local build is needed across Android,
desktop, web, shared metadata, and native framework outputs.
