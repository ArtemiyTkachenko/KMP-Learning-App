# Shared Module Guide

`:shared` is the Kotlin Multiplatform module: shared Compose UI, domain and data code,
platform `expect`/`actual` implementations, Compose resources, and multiplatform tests.

Canonical rules for this module — source sets, what may live in `commonMain`, dependency
direction, `expect`/`actual` — are in
[KMP boundaries](../docs/development/kmp.md). Test placement is in
[testing](../docs/development/testing.md).

Local reminders that are easy to get wrong here:

- Keep Android, UIKit, JVM, browser, and Wasm APIs out of `commonMain`.
- Web code shared by both browser targets belongs in `webMain`, not in per-target source
  sets; `:shared` currently has no `jsMain` or `wasmJsMain` sources.
- Validate with the narrowest relevant task, commonly `./gradlew :shared:jvmTest`, then
  `./gradlew :shared:allTests` or `./gradlew :shared:check`. See
  [validation](../docs/development/validation.md).
- Web-related Gradle tasks can leave generated Kotlin/JS files behind. Do not commit
  generated stores unless a build-system task intentionally introduced them.

What the code in this module currently does is documented in
[`docs/architecture/`](../docs/architecture/overview.md).
