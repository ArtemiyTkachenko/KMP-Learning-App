# CI

## Build And Test (`.github/workflows/main.yml`)

Runs on pull requests to `main`, pushes to `main`, and manual dispatch. Ubuntu runner,
Temurin JDK 21, one Gradle invocation:

```sh
./gradlew --no-daemon :androidApp:assembleDebug :desktopApp:assemble :webApp:assemble :shared:check
```

A single invocation is deliberate: one configuration phase and one Kotlin/Native + JS/Wasm
toolchain setup. `org.gradle.parallel` is not enabled, so the tasks run sequentially in
command-line order. Cheap Android and desktop JVM compilation runs first so a plain
compile break fails fast; `:shared:check` runs last because it is by far the longest. The
JVM test report is uploaded as an artifact.

**What CI does not cover:** Kotlin/Native iOS compilations are disabled on a Linux runner,
so this job gives no iOS signal at all. iOS framework linking and simulator runtime
verification are local macOS checks. Do not describe a green CI run as iOS validation.

## Backlog Sync (`.github/workflows/sync-backlog.yml`)

Manual dispatch only. Installs PyYAML, validates `.github/project/backlog.yml`, verifies
the runner's GitHub CLI supports parent/sub-issue functionality, then synchronizes the
backlog to the GitHub Project. See [backlog](backlog.md) and `.github/AGENTS.md`.

## Reporting CI Status

Never claim CI passed unless actual CI results were observed. Local `./gradlew check`
success is not a CI result.
