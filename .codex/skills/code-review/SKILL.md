---
name: code-review
description: Perform a project-specific review of a change, diff, or branch. Use when asked to review code, find bugs, assess implementation quality, or review before PR; do not automatically implement findings.
---

# Code Review

## Use When

The user asks to review a diff, branch, implementation, or code quality before a PR.

## Do Not Use When

The user asked for implementation. Do not implement fixes unless they explicitly ask for
that after the review.

## Workflow

1. Inspect the relevant diff and the touched files.
2. If a backlog key is referenced, read it in `.github/project/backlog.yml`.
3. Check the affected module and source-set boundaries.
4. Check tests and the validation evidence actually presented.
5. Report findings first, ordered by severity.

## Project References

- [Code review](../../../docs/workflows/code-review.md) — what to prioritize, what to omit,
  and the finding format.
- [KMP boundaries](../../../docs/development/kmp.md) and
  [Kotlin style](../../../docs/development/kotlin.md) — the rules most findings cite.
- [Architecture](../../../docs/architecture/overview.md) — check a change against what the
  code already does before calling it wrong.

## Output

Findings ordered by severity, each with a severity label, a concrete file and line where
possible, the problem, why it matters, and the smallest appropriate correction. Do not
manufacture findings; if nothing substantive is found, say so and name the residual test or
validation risk.
