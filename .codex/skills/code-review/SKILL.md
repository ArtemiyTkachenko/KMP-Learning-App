---
name: code-review
description: Perform a project-specific review of a change, diff, or branch. Use when asked to review code, find bugs, assess implementation quality, or review before PR; do not automatically implement findings.
---

# Code Review

Use this skill when the user asks to review a diff, branch, implementation, or code quality before PR.

Do not use this skill to implement fixes unless the user explicitly asks for that after the review.

## Review Focus

Inspect for:

- Correctness bugs and behavioral regressions.
- Acceptance-criteria violations for referenced `E##-##` issues.
- KMP boundary violations.
- Class visibility correctness
- Android lifecycle, coroutine, or platform issues where applicable.
- Compose issues where applicable to current code.
- Unnecessary complexity, speculative abstraction, or unrelated refactors.
- Duplication that creates maintenance risk.
- Dependency/build concerns.
- Insufficient or misplaced tests.
- Dead, unreachable, placeholder, or debug code.
- Accidental unrelated changes.

## Process

1. Inspect the relevant diff and touched files.
2. If a backlog key is referenced, read `.github/project/backlog.yml`.
3. Check affected module/source-set boundaries.
4. Check tests and validation evidence.
5. Report findings first, ordered by severity.

## Finding Format

For each substantive finding include:

- Severity: blocking, important, or optional.
- Concrete file and line where possible.
- Problem.
- Why it matters.
- Recommended correction.

Do not manufacture findings to make the review longer. If no issues are found, say that clearly and mention residual test or validation risk.
