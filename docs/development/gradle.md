# Gradle And Build Configuration

Rules for changing build files, version catalogs, targets, plugins, and source-set
dependencies. For module and source-set semantics see [KMP](kmp.md); for the commands see
[validation](validation.md).

## Build Layout

- Modules are declared in `settings.gradle.kts`: `:androidApp`, `:desktopApp`, `:shared`,
  `:sqliteWasmWorker`, `:webApp`.
- The root `build.gradle.kts` only centralizes plugin aliases with `apply false`.
- Dependency and plugin versions live in `gradle/libs.versions.toml`.
- `:shared` applies Kotlin Multiplatform, the Android KMP library plugin, Room, Compose
  Multiplatform, the Compose compiler, KSP, and kotlinx.serialization. Room schemas are
  written to `shared/schemas`, and the Room compiler is registered per KSP target.
- `:androidApp` applies the Android application plugin and the Compose compiler.
- Gradle properties enable the configuration cache and build cache.

## Rules

- Keep versions centralized in `gradle/libs.versions.toml`. Module build files consume
  catalog aliases such as `libs.compose.ui` or `libs.plugins.kotlinMultiplatform`, never
  literal versions.
- Add a dependency only to the module and source set that actually needs it, and keep
  platform-specific dependencies out of `commonMain`.
- Verify that a source-set dependency matches the target that consumes it.
- Use Gradle project references for module dependencies, for example
  `implementation(project(":shared"))`.
- Avoid unrelated plugin, Kotlin, AGP, Gradle, or dependency upgrades.
- Do not introduce convention plugins or build abstractions until repeated build logic
  makes them worthwhile.
- Introduce a dependency only for a concrete requirement, and explain the material
  tradeoffs.

## Adding A Dependency

1. Add or reuse the version and library alias in `gradle/libs.versions.toml`.
2. Add it to the narrowest source set that needs it.
3. Explain why the module, source set, and library were chosen.
4. Validate with the narrowest relevant task, then widen if build behavior changed across
   modules.
