# Code Review

Automated and manual reviews focus on substantive engineering issues rather than stylistic
noise. A review reports findings; it does not implement them unless the user asks for that
afterwards.

## Prioritize

- Correctness bugs and behavioral regressions.
- Violations of the acceptance criteria of a referenced `E##-##` issue.
- Incorrect Kotlin Multiplatform boundaries, accidental platform dependencies in shared
  code, and wrong dependency direction. See [KMP](../development/kmp.md).
- Visibility wider than the declaration needs. See [Kotlin style](../development/kotlin.md).
- Android lifecycle or coroutine issues where relevant.
- Compose state, side-effect, or lifecycle issues where relevant.
- Meaningful missing or misplaced automated tests.
- Gradle/build configuration problems, unnecessary dependencies, premature abstractions.
- Duplication that creates maintenance risk.
- Dead, unreachable, placeholder, or debug code.
- Accidental unrelated changes.
- Security or reliability problems where relevant.

## De-Emphasize Or Omit

- Formatting-only comments.
- Subjective naming preferences with no meaningful maintainability impact.
- Stylistic suggestions already handled by tooling.
- Speculative architecture rewrites unrelated to the change.
- Comments made only to produce review output.

## Process

1. Inspect the relevant diff and the touched files.
2. If a backlog key is referenced, read it in `.github/project/backlog.yml`.
3. Check the affected module and source-set boundaries.
4. Check tests and the validation evidence actually presented.
5. Report findings first, ordered by severity.

## Finding Format

For each substantive finding: severity (blocking, important, optional), a concrete file
and line where possible, the problem, why it matters, and the smallest appropriate
correction.

Do not manufacture findings when the implementation is sound. If nothing substantive is
found, say so clearly and name the residual test or validation risk.
