# KMP-Learning-App

An app for preparing for Android/KMP/Compose interviews, built as a Kotlin Multiplatform
learning and portfolio project.

Android, Desktop, iOS, and both web targets all run the same shared Compose application.
Each host starts the shared Koin graph, initializes the bundled curriculum through its
platform Room database, and then composes the shared `AppRoot`.

## Modules

| Module | Role |
| --- | --- |
| `:shared` | Kotlin Multiplatform shared code and shared Compose UI. |
| `:androidApp` | Android application entry point. |
| `:desktopApp` | Compose Desktop application entry point. |
| `:webApp` | Compose web entry point for the JS and Wasm browser targets. |
| `:sqliteWasmWorker` | SQLite WASM worker packaged for the web Room driver. |

The iOS application lives in `iosApp` and embeds the shared Compose UI through the
framework produced by `:shared`.

The web host must be served from a secure, cross-origin-isolated context. Its development
server supplies the required `Cross-Origin-Opener-Policy` and
`Cross-Origin-Embedder-Policy` headers, and production hosting must preserve them for the
SQLite WASM worker.

## Building And Testing

```sh
./gradlew :androidApp:assembleDebug   # Android
./gradlew :shared:allTests            # shared tests, all targets
./gradlew :shared:check               # broadest shared check
./gradlew check                       # repository-wide
```

CI runs the Android, desktop, and web builds together with the shared checks. Kotlin/Native
iOS compilation is disabled on the Linux runner, so iOS remains a local macOS check.

The full command reference is in [docs/development/validation.md](docs/development/validation.md).

## Documentation

`AGENTS.md` is the entry point for coding agents and links every document below.

- **Architecture** — [overview](docs/architecture/overview.md),
  [assessment](docs/architecture/assessment.md),
  [progress](docs/architecture/progress.md),
  [practice selection](docs/architecture/practice-selection.md),
  [recommendations](docs/architecture/recommendations.md),
  [practice builder](docs/architecture/practice-builder.md),
  [persistence](docs/architecture/persistence.md).
- **Development** — [Kotlin style](docs/development/kotlin.md),
  [KMP boundaries](docs/development/kmp.md), [Gradle](docs/development/gradle.md),
  [testing](docs/development/testing.md), [validation](docs/development/validation.md).
- **Workflows** — [backlog](docs/workflows/backlog.md),
  [code review](docs/workflows/code-review.md), [CI](docs/workflows/ci.md).
- **Content** — [curriculum](docs/content/curriculum.md),
  [authoring contract](docs/content/content-authoring.md),
  [authoring playbook](docs/content/question-authoring-playbook.md),
  [coverage snapshot](docs/content/question-bank-coverage.md).
