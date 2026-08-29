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

## Documentation

Project documentation lives in `docs/`:

- `docs/architecture.md` - application composition, navigation, presentation
  conventions, and which platform hosts are runnable.
- `docs/persistence.md` - local data design, Room schema, and the curriculum
  import and update policy.
- `docs/curriculum.md` - the Android interview topic and subtopic taxonomy.
- `docs/content-authoring.md` - the editorial contract for interview content:
  the rules a question must satisfy.
- `docs/question-authoring-playbook.md` - the method for meeting that contract:
  distractor design, anti-cue audits, and pre-merge checks.

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

## Runnable Hosts

Android, Desktop, iOS, and both web targets are runtime hosts for the same
shared application. Each host starts the shared Koin graph before composing
`AppRoot`, then initializes the bundled curriculum through its platform Room
database. Android, Desktop, and iOS use persistent local SQLite files. JS and
Wasm use Room's SQLite web-worker driver with origin-scoped OPFS storage.

The web host must be served from a secure, cross-origin-isolated context. Its
development server supplies the required `Cross-Origin-Opener-Policy` and
`Cross-Origin-Embedder-Policy` headers. Production hosting must preserve those
headers for the SQLite WASM worker. See `docs/architecture.md` ("Runtime Host
Coverage") and `docs/persistence.md` for the exact host and storage boundaries.

## Android And Shared Boundaries

`androidApp` owns Android application concerns such as the Activity entry point,
Android manifest, launcher resources, app id, SDK configuration, and Android
preview setup. It depends on `:shared` and hosts the shared Compose entry point
with `App()`.

`:shared` owns code intended to be reused across configured targets. Common
shared code lives in `shared/src/commonMain`; platform-specific implementations
live in platform source sets such as `shared/src/androidMain`.

When migrating future domain functionality, start from whether the concept is
platform-independent. Domain rules and pure data transformations are candidates
for shared code. Android framework integration, lifecycle entry points,
permissions, resources, and other platform APIs should remain in Android-owned
code or Android-specific source sets.

## Verification

Useful foundation checks:

```sh
./gradlew :androidApp:assembleDebug
./gradlew :shared:check
./gradlew check
```

CI runs the Android, desktop, and web builds together with the shared checks:

```sh
./gradlew :androidApp:assembleDebug :desktopApp:assemble :webApp:assemble :shared:check
```

Use `./gradlew build` when a full local build is needed across Android,
desktop, web, shared metadata, and native framework outputs.

## Shared Testing

Common shared tests live in `shared/src/commonTest` and use `kotlin.test`.
They should cover platform-independent behavior that belongs in shared code.

Run shared tests with:

```sh
./gradlew :shared:allTests
```

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
