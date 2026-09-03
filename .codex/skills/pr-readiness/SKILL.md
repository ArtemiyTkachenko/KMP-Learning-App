---
name: pr-readiness
description: Determine whether completed work is ready for review, PR, or merge. Use when asked if work is complete against an issue or ready for PR; verify acceptance criteria, diff, tests, and repository checks without claiming CI or merge status unless observed.
---

# PR Readiness

## Use When

The user asks whether work is ready for PR, ready for review, ready to merge, or complete
against a backlog issue.

## Do Not Use When

The user asked for implementation or a line-by-line review. Never merge changes
automatically.

## Workflow

1. Determine the relevant backlog issue and read its acceptance criteria in
   `.github/project/backlog.yml`.
2. Inspect the complete relevant diff.
3. Check each acceptance criterion against the actual implementation.
4. Look for unrelated changes, and for accidental TODOs, placeholders, logging, or debug
   code in the touched areas.
5. Check whether the change requires documentation or backlog updates.
6. Review meaningful test coverage.
7. Run the relevant build/test/lint checks, and report anything failed, skipped, or
   unavailable.

## Project References

- [Backlog workflow](../../../docs/workflows/backlog.md) — acceptance criteria and the
  status language to use.
- [Validation](../../../docs/development/validation.md) — every check that exists here,
  including backlog validation.
- [CI](../../../docs/workflows/ci.md) — what the pipeline covers, and what it does not
  (there is no iOS signal).

## Output

A concise readiness verdict such as "ready for review", "ready for PR", "blocked by
validation", or "not ready".

Do not say tests passed unless they were executed, do not say CI passed unless CI results
were checked, do not say an issue is merged if it is not, and do not call an issue `Done`
before the project's Done conditions are met.
