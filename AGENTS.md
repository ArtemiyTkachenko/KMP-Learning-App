# KMP Learning App Agent Guide

This repository is an Android / Kotlin Multiplatform learning and portfolio project for senior-level Android/KMP preparation. It focuses on modern Android, Jetpack Compose, Kotlin Multiplatform boundaries, pragmatic architecture, meaningful automated testing, CI/CD, and engineering workflow practice. Generated changes should support learning: keep decisions visible, reviewable, and explainable.

## Scope Boundaries

- Do not introduce a custom backend or analytics unless project scope explicitly changes.
- Do not build native SwiftUI features; the iOS app currently exists to host shared Compose UI.
- Do not expand classic Android Views or Fragments into a large legacy architecture. Use them only for narrowly scoped refresher/interoperability work when requested.
- Avoid unrelated infrastructure, frameworks, or libraries added only for architectural purity.
- Do not silently broaden a backlog issue. Implement the smallest change that satisfies the requested scope.

## Engineering Principles

- Inspect the relevant existing code and build files before changing them.
- Prefer small, understandable, idiomatic Kotlin and Compose over clever abstractions.
- Avoid premature abstraction and unrelated refactors.
- Follow established repository conventions unless the task explicitly changes them.
- Introduce dependencies only for concrete requirements, and explain material tradeoffs.
- Preserve generated/project files unless the task specifically requires changing them.

## Kotlin Multiplatform Boundaries

- The current modules are `:shared`, `:androidApp`, `:desktopApp`, and `:webApp`, plus the `iosApp` Xcode shell.
- Put code in `shared/src/commonMain` only when the concept genuinely belongs in platform-independent shared code.
- Do not optimize for maximum shared-code percentage. Prefer limited duplication over a bad shared abstraction.
- Keep platform APIs out of shared common code. Use platform source sets such as `androidMain`, `iosMain`, `jvmMain`, `jsMain`, and `wasmJsMain` when the responsibility is platform-specific.
- Preserve dependency direction: platform application shells depend on `:shared`; shared code must not depend on app modules.
- Use `expect`/`actual` only when the common contract is useful and platform implementations are genuinely required.

## Gradle And Build Rules

- Keep dependency versions centralized in `gradle/libs.versions.toml`.
- Do not scatter literal dependency versions through module build files.
- Avoid unrelated plugin, Kotlin, AGP, Gradle, or dependency upgrades.
- Do not introduce convention plugins or build abstractions until repeated build logic makes them worthwhile.
- Explain material build-system changes, especially target/source-set/dependency changes.

## Testing And Verification

- Add or update tests for meaningful behavior changes.
- Prefer tests of observable behavior over implementation details.
- Prefer `commonTest` for genuinely shared behavior; use platform test source sets for platform-specific behavior.
- Do not add mocking or test dependencies simply to increase test count.
- Run the narrowest relevant checks first, then a broader check when the change warrants it.
- Useful current commands:
  - `./gradlew :androidApp:assembleDebug`
  - `./gradlew :shared:allTests`
  - `./gradlew :shared:check`
  - `./gradlew :androidApp:lintDebug`
  - `./gradlew check`
  - `./gradlew build`
- Avoid running heavy Gradle/KMP checks in parallel; Kotlin/Native and JS/Wasm setup can contend for shared toolchain resources.

## Backlog And Issue Workflow

- Backlog definitions live in `.github/project/backlog.yml`.
- Stable child issue keys use the `E##-##` format, for example `E02-01`.
- When work references a backlog key, locate it in `backlog.yml` and use its Issue, Approach, Acceptance Criteria, Priority, and Size as the task definition.
- Do not invent missing requirements or mark a GitHub issue `Done` because code was generated.
- Use precise status language such as "implementation complete", "ready for review", or "ready for PR".
- The project's final Done state requires the agreed automated checks to pass and the work to be merged.

## Learning Handoff

For non-trivial generated changes, explain the important Android/KMP/Compose/Gradle choices, files worth reviewing, meaningful alternatives or tradeoffs, tests run, and anything not validated. Do not explain trivial Kotlin syntax unless asked.
