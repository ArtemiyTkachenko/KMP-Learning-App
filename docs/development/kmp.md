# Kotlin Multiplatform Boundaries

What may live in shared code, what must stay platform-specific, and how the modules
depend on each other. This is the canonical source for KMP rules; `shared/AGENTS.md`
and the `kmp-boundary-review` skill point here rather than restating them.

## Modules

| Module | Role |
| --- | --- |
| `:shared` | The Kotlin Multiplatform module: shared Compose UI, domain, data, and `expect`/`actual` implementations. |
| `:androidApp` | Android application shell. |
| `:desktopApp` | Compose Desktop application shell. |
| `:webApp` | Compose web shell for the JS and Wasm browser targets. |
| `:sqliteWasmWorker` | Packages the SQLite WASM worker consumed by the web Room driver. |
| `iosApp` | Xcode shell embedding the framework produced by `:shared`. |

Dependency direction is one-way: platform application shells depend on `:shared`, and
`:shared` never depends on an application module.

## Shared Targets And Source Sets

`:shared` configures `android`, `jvm`, `js(browser)`, `wasmJs(browser)`, `iosArm64`, and
`iosSimulatorArm64`.

Source sets that currently hold code:

| Source set | Contents |
| --- | --- |
| `commonMain` | Platform-independent shared code, including shared Compose UI. |
| `androidMain` | Android platform implementations and Android-only dependencies. |
| `iosMain` | iOS platform implementations. |
| `jvmMain` | JVM/desktop platform implementations. |
| `webMain` | Code shared by both browser targets, including the web Room builder and Koin module. |
| `commonTest`, `jvmTest` | See [testing](testing.md). |

`webMain` is the intermediate source set for JS and Wasm. Per-target `jsMain` and
`wasmJsMain` exist in the hierarchy but currently hold no `:shared` sources — only
`:sqliteWasmWorker` splits code that way, because only worker construction genuinely
differs between the two browser targets.

## Rules

- Put code in `commonMain` only when the concept is genuinely platform-independent.
- Keep Android, UIKit, JVM, browser, and Wasm APIs out of common source sets.
- Do not optimize for maximum shared-code percentage. Prefer limited duplication over a
  bad shared abstraction.
- Use `expect`/`actual` only when the common contract is useful to shared code and
  platform implementations are genuinely required. Provide actuals for every configured
  target that needs them.
- Declare a dependency in the narrowest source set that consumes it. See
  [gradle](gradle.md).
- Abstract platform capabilities behind a shared interface owned by shared code, the way
  the database builder sits below `CurriculumRepository`. See
  [architecture overview](../architecture/overview.md) and
  [persistence](../architecture/persistence.md) for the existing boundaries.

## Boundary Review Questions

When a change moves code across a boundary, ask:

- Does this responsibility conceptually belong in shared code?
- Is a platform API leaking into `commonMain`?
- Is platform-specific behavior being forced into common code to maximize sharing?
- Would small platform duplication be clearer than a shared abstraction?
- Is dependency direction still application shell -> `:shared`?
- Are source-set dependencies declared in the source set that consumes them?
- Are tests in `commonTest` only for genuinely shared behavior?

Recommend the smallest correction that preserves the intended boundary. Do not propose a
broad architecture rewrite merely because another KMP structure is theoretically
possible.
