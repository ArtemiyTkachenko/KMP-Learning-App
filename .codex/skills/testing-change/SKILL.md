---
name: testing-change
description: Design and verify tests for behavior changes in this KMP project. Use when adding behavior, fixing bugs, changing domain/data/presentation logic, or modifying automated tests; not for test-count padding or unrelated documentation-only edits.
---

# Testing Change

Use this skill when adding meaningful behavior, fixing bugs, changing domain/data/presentation logic, adding/modifying tests, or reviewing a known test coverage gap.

Do not use it to add tautological tests or mocks solely to increase coverage.

## Current Test Setup

- Test library currently available in shared code: `kotlin.test`.
- `shared/src/commonTest` is for genuinely shared behavior.
- `shared/src/androidHostTest` is for Android host-side behavior.
- `shared/src/jvmTest` is for JVM/desktop behavior.
- `shared/src/iosTest` is for iOS behavior.
- `shared/src/webTest` exists for web-related shared tests.
- `androidApp` and `desktopApp` currently have no meaningful test sources.

## Test Design

- Test observable behavior, not implementation details.
- Put tests in `commonTest` when the behavior is truly shared.
- Use platform test source sets when behavior depends on platform APIs or platform-specific actual implementations.
- Prefer simple fakes and real values over mocks when they are clearer.
- Do not add mocking/test dependencies without a concrete need.
- Include meaningful boundary and failure cases where relevant.
- For bug fixes, add regression coverage where practical.
- Avoid tests that only assert language/library behavior.

## Verification Commands

Use focused commands where possible:

```sh
./gradlew :shared:jvmTest
./gradlew :shared:testAndroidHostTest
./gradlew :shared:iosSimulatorArm64Test
./gradlew :shared:jsTest
./gradlew :shared:wasmJsTest
./gradlew :shared:allTests
```

For Android app changes:

```sh
./gradlew :androidApp:testDebugUnitTest
./gradlew :androidApp:assembleDebug
```

For broader confidence:

```sh
./gradlew check
```

Always report the exact commands run and whether any expected validation was skipped or unavailable.
