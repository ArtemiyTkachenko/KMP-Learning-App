---
name: pr-readiness
description: Determine whether completed work is ready for review, PR, or merge. Use when asked if work is complete against an issue or ready for PR; verify acceptance criteria, diff, tests, and repository checks without claiming CI or merge status unless observed.
---

# PR Readiness

Use this skill when the user asks whether work is ready for PR, ready for review, ready to merge, or complete against a backlog issue.

Do not merge changes automatically. Do not claim CI passed unless CI results were actually observed.

## Workflow

1. Determine the relevant backlog issue when applicable.
2. Read `.github/project/backlog.yml` for the issue's acceptance criteria.
3. Inspect the complete relevant diff.
4. Check each acceptance criterion against the actual implementation.
5. Look for unrelated changes.
6. Search touched areas for accidental TODOs, placeholders, logging, or debug code.
7. Check whether documentation or backlog updates are required by the change.
8. Review meaningful test coverage.
9. Run relevant build/test/lint/static-analysis commands that exist in this repository.
10. Report failed, skipped, or unavailable validation explicitly.
11. Give a concise readiness verdict.

## Validation Commands

Use focused checks first:

```sh
./gradlew :shared:check
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:lintDebug
```

Use the full current repository check when appropriate:

```sh
./gradlew check
```

Use `./gradlew build` only when full assembly across Android release, web production output, iOS frameworks, and tests is needed.

For backlog-only changes, validate with:

```sh
python .github/project/validate_backlog.py .github/project/backlog.yml
```

If local dependencies such as `PyYAML` are missing, say so and do not report the validation as passed.

## Verdict Rules

- Do not say tests passed unless they were executed.
- Do not say CI passed unless actual CI results were checked.
- Do not say an issue is merged if it is not merged.
- Do not call the issue `Done` before the project's Done conditions are met.
- Prefer statuses such as "ready for review", "ready for PR", "blocked by validation", or "not ready".
