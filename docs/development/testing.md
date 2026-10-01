# Testing

How this repository's code is tested and where tests belong. For the commands themselves
see [validation](validation.md).

## Test Source Sets

| Source set | Purpose | Has sources today |
| --- | --- | --- |
| `shared/src/commonTest` | Genuinely shared behavior | yes |
| `shared/src/jvmTest` | JVM/desktop behavior, Room testing, Compose UI tests | yes |
| `shared/src/androidHostTest` | Android host-side behavior | no |
| `shared/src/iosTest` | iOS behavior | no |
| `shared/src/webTest` | Web behavior, in a real browser for JS and Wasm | yes |

`androidApp` and `desktopApp` have no meaningful test sources.

The test library available in shared code is `kotlin.test`, with
`kotlinx-coroutines-test` in `commonTest` and Room testing plus Compose UI test artifacts
in `jvmTest`.

`webTest` runs in headless Chrome through Karma, once per browser target. Use it for behaviour
that only a browser can show — the SQLite worker, OPFS, browser APIs behind a `js()` call — not
for logic `commonTest` already covers. `shared/karma.config.d` serves the test page with the same
COOP/COEP headers as the development server, so the real database worker can open OPFS, and
raises Mocha's per-test timeout to fit a worker start. A test that waits on real browser events
must not use a virtual-time `withTimeout` inside `runTest`.

## Test Design

- Add or update tests for meaningful behavior changes.
- Test observable behavior, not implementation details.
- Put a test in `commonTest` only when the behavior is genuinely shared; use a platform
  test source set when behavior depends on platform APIs or on a platform `actual`.
- Prefer simple fakes and real values over mocks when they are clearer.
- Do not add mocking or test dependencies to increase test count, and do not add
  tautological tests that only assert language or library behavior.
- Cover meaningful boundary and failure cases; for a bug fix, add regression coverage
  where practical.
- Test classes and helpers should usually be `internal` or `private`. After reducing
  visibility, run the relevant test task to confirm discovery still works.
