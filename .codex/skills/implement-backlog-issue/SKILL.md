---
name: implement-backlog-issue
description: Implement, continue, fix, or complete work identified by a repository E##-## backlog key. Use this for scoped issue implementation driven by .github/project/backlog.yml; do not use it for general questions or pure code review.
---

# Implement Backlog Issue

Use this skill when the user asks to implement, continue, fix, or complete a specific repository backlog issue such as `E02-01`.

Do not use this skill for conceptual discussion, broad planning without a key, pure code review, or PR readiness checks.

## Workflow

1. Resolve the requested `E##-##` key in `.github/project/backlog.yml`.
2. Read the parent epic entry and the issue's `issue`, `approach`, `acceptance_criteria`, `priority`, `size`, and `initial_status`.
3. If the key is missing, stop and report that it does not exist in the repository backlog.
4. Inspect relevant repository code before editing. At minimum consider:
   - `settings.gradle.kts`
   - root `build.gradle.kts`
   - `gradle/libs.versions.toml`
   - affected module build files
   - affected source sets and tests
5. Check obvious prerequisites from the backlog and current code. Do not invent dependencies that are not stated or observable.
6. For substantial work, share a concise implementation plan before edits.
7. Implement the smallest sufficient change that satisfies the acceptance criteria.
8. Compose with specialized skills when relevant:
   - `gradle-kmp-change` for Gradle, source-set, target, or dependency changes.
   - `kmp-boundary-review` for shared/platform boundary changes.
   - `testing-change` for test design or test setup changes.
9. Add or update meaningful automated tests when behavior changes.
10. Run focused verification first, then broader verification when appropriate.
11. Inspect the final diff for unrelated changes before reporting completion.
12. Report each acceptance criterion as satisfied, not satisfied, or not validated.
13. Report exact commands run and anything that could not be validated.

## Repository Commands

Prefer the narrowest command that proves the change:

```sh
./gradlew :androidApp:assembleDebug
./gradlew :shared:allTests
./gradlew :shared:check
./gradlew :androidApp:lintDebug
./gradlew check
```

Use `./gradlew build` only when broad full-project assembly is needed; it is slower and includes Android release, web production bundles, iOS framework linking, and tests.

Avoid running heavy Gradle/KMP commands in parallel.
