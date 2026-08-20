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

## Gradle And Dependency Conventions

Dependency and plugin versions are centralized in `gradle/libs.versions.toml`.
Module build files should consume catalog aliases such as `libs.compose.ui` or
`libs.plugins.kotlinMultiplatform` rather than declaring dependency versions
inline.

When adding a dependency:

1. Add or reuse the version and library alias in `gradle/libs.versions.toml`.
2. Add the dependency only to the module and source set that actually needs it.
3. Keep platform-specific dependencies out of `commonMain`.
4. Prefer existing Gradle/module patterns before introducing new build
   abstractions.

Project dependencies use Gradle project references, for example
`implementation(project(":shared"))`.
