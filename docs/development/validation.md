# Validation Commands

The canonical list of checks that exist in this repository. Other documents and skills
link here instead of repeating command lists.

## The Ladder

Run the narrowest check that can prove the change, then widen only when the change
warrants it:

```text
targeted test or compile task
        -> module-level check
        -> repository-wide check
```

Do not run repository-wide validation after every small edit, and do not run heavy
Gradle/KMP commands in parallel — Kotlin/Native, JS, and Wasm toolchain setup contend for
shared resources. When a check fails, read the specific failure rather than pasting the
whole Gradle log.

## Shared Module

```sh
./gradlew :shared:jvmTest
./gradlew :shared:testAndroidHostTest
./gradlew :shared:iosSimulatorArm64Test
./gradlew :shared:jsTest
./gradlew :shared:wasmJsTest
./gradlew :shared:allTests
./gradlew :shared:check
```

`:shared:allTests` runs every target's tests and aggregates the report. `:shared:check`
is the broadest shared check and is by far the longest-running task in the repository.

## Application Modules

```sh
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:testDebugUnitTest
./gradlew :androidApp:lintDebug
./gradlew :desktopApp:assemble
./gradlew :webApp:assemble
```

## Repository-Wide

```sh
./gradlew check
./gradlew build
```

Use `./gradlew build` only when full assembly is genuinely needed; it includes the Android
release build, web production bundles, iOS framework linking, and tests.

## Backlog Data

```sh
python .github/project/validate_backlog.py .github/project/backlog.yml
```

If `PyYAML` is not installed locally, report that instead of reporting the validation as
passed.

## Known Non-Fatal Signals

Report these if observed, but they are not failures: Compose/Web asset-size warnings,
Kotlin/Native framework bundle ID warnings, and KLIB duplicate `unique_name` warnings.

## Reporting

Always name the exact commands run and state explicitly what was skipped, unavailable, or
failed. Never report a check as passing unless it was executed. See
[CI](../workflows/ci.md) for what the pipeline does and does not cover.
