---
name: gradle-kmp-change
description: Safely modify Gradle and Kotlin Multiplatform build configuration for this repository. Use for build.gradle.kts, settings.gradle.kts, libs.versions.toml, source-set dependencies, targets, plugins, or Gradle properties; not for ordinary Kotlin code changes.
---

# Gradle KMP Change

Use this skill for changes to Gradle build files, settings, version catalogs, plugins, source-set dependency declarations, project/module dependencies, or build properties.

Do not use it merely because ordinary Kotlin code lives in a Gradle project.

## Repository Facts

- Modules are declared in `settings.gradle.kts`: `:androidApp`, `:desktopApp`, `:shared`, `:webApp`.
- Root `build.gradle.kts` only centralizes plugin aliases with `apply false`.
- Dependency and plugin versions live in `gradle/libs.versions.toml`.
- `:shared` uses `org.jetbrains.kotlin.multiplatform`, `com.android.kotlin.multiplatform.library`, Compose Multiplatform, and the Compose compiler plugin.
- `:androidApp` uses `com.android.application` and the Compose compiler plugin.
- Gradle properties enable configuration cache and build cache.

## Rules

- Preserve centralized dependency/version management in `gradle/libs.versions.toml`.
- Avoid scattered literal versions in module build files.
- Explain why any new plugin, target, dependency, or source-set dependency is needed.
- Avoid unrelated version upgrades.
- Do not introduce convention plugins or build abstractions until repeated build logic justifies them.
- Verify that source-set dependencies match the target that consumes them.
- Keep platform application modules depending on `:shared`, not the reverse.

## Validation

Run the narrowest useful validation first:

```sh
./gradlew :shared:check
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:lintDebug
```

Use broader verification when build behavior changes across modules:

```sh
./gradlew check
./gradlew build
```

Do not run heavy Gradle/KMP commands in parallel. Kotlin/Native, JS, and Wasm setup may contend for shared toolchain resources.

Known non-fatal signals to report if observed: Compose/Web asset-size warnings, Kotlin/Native framework bundle ID warnings, and KLIB duplicate `unique_name` warnings.
