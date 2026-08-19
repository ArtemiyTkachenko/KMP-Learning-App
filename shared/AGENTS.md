# Shared Module Guide

`shared` is the primary Kotlin Multiplatform module. It currently contains shared Compose UI, small shared logic, platform `expect`/`actual` examples, Compose resources, and multiplatform tests.

- Keep `commonMain` platform-independent.
- Use `androidMain`, `iosMain`, `jvmMain`, `jsMain`, or `wasmJsMain` when APIs or behavior are platform-specific.
- Do not introduce Android, UIKit, JVM, browser, or Wasm APIs into common source sets.
- Use `expect`/`actual` only when a common contract is needed by shared code.
- Prefer `commonTest` for shared behavior tests and platform test source sets for platform-specific behavior.
- Validate shared changes with the narrowest relevant task, commonly `./gradlew :shared:allTests` or `./gradlew :shared:check`.
- Watch for generated Kotlin/JS files after web-related Gradle tasks; generated stores should not be committed unless intentionally introduced by a build-system task.
