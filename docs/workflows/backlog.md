# Backlog And Issue Workflow

How a repository backlog key drives a task. For the rules governing the backlog *data* and
its GitHub Project synchronization, see `.github/AGENTS.md`.

## Backlog Keys

Backlog definitions live in `.github/project/backlog.yml`. Stable child issue keys use the
`E##-##` format, for example `E02-01`.

When work references a key, locate it in `backlog.yml` and use its `issue`, `approach`,
`acceptance_criteria`, `priority`, `size`, and `initial_status` as the task definition,
together with the parent epic entry. If the key is not in `backlog.yml`, stop and report
that it does not exist rather than inventing requirements.

## Implementing An Issue

1. Resolve the key and read the epic plus the issue entry.
2. Check prerequisites stated in the backlog or observable in current code. Do not invent
   dependencies that are neither stated nor observable.
3. Inspect the relevant existing code before editing — at minimum the affected module
   build files, source sets, and tests, plus `settings.gradle.kts`,
   `gradle/libs.versions.toml`, and the root `build.gradle.kts` when the change touches the
   build.
4. Implement the smallest change that satisfies the acceptance criteria. Do not silently
   broaden the issue's scope.
5. Add or update meaningful tests for behavior changes. See
   [testing](../development/testing.md).
6. Validate from the narrowest relevant check outward. See
   [validation](../development/validation.md).
7. Inspect the final diff for unrelated changes.
8. Report each acceptance criterion as satisfied, not satisfied, or not validated, and
   name the exact commands run.

## Status Language

Do not mark a GitHub issue `Done` because code was generated. Use precise language:
"implementation complete", "ready for review", "ready for PR", "blocked by validation",
"not ready".

The project's Done state requires the agreed automated checks to pass **and** the work to
be merged. Do not claim tests passed unless they were executed, do not claim CI passed
unless CI results were observed, and do not describe unmerged work as merged.
