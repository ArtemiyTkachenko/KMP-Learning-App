---
name: kmp-boundary-review
description: Review or guide Kotlin Multiplatform boundaries in this project. Use when code moves between common/platform source sets, expect/actual APIs are introduced, or module/source-set dependencies change; not for ordinary Kotlin edits with no KMP boundary impact.
---

# KMP Boundary Review

Use this skill when a task adds or moves shared code, changes source sets, introduces platform-specific implementations, adds `expect`/`actual`, or changes dependencies between shared and platform modules.

Do not use it for pure `.github` automation work or ordinary Kotlin changes that do not affect KMP boundaries.

## Current Structure

- Shared module: `:shared`
- Platform/application shells: `:androidApp`, `:desktopApp`, `:webApp`, `iosApp`
- Shared source sets include `commonMain`, `androidMain`, `iosMain`, `jvmMain`, `jsMain`, `wasmJsMain`
- Test source sets include `commonTest`, `androidHostTest`, `jvmTest`, `iosTest`, `webTest`

## Review Questions

- Does this responsibility conceptually belong in shared code?
- Is any Android, UIKit, JVM, browser, or Wasm API leaking into `commonMain`?
- Is platform-specific behavior being forced into common code to maximize sharing?
- Would small platform duplication be clearer than a shared abstraction?
- Is `expect`/`actual` justified by a useful common contract?
- Are actual implementations present for all configured targets that need them?
- Is dependency direction still platform/application -> `:shared`?
- Are source-set dependencies declared in the correct source set?
- Are tests placed in `commonTest` only for genuinely shared behavior?

## Output

For reviews, separate blocking correctness issues from maintainability concerns and optional suggestions. Recommend the smallest correction that preserves the intended boundary.

Do not propose broad architecture rewrites merely because another KMP structure is theoretically possible.
