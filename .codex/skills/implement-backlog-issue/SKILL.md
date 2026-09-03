---
name: implement-backlog-issue
description: Implement, continue, fix, or complete work identified by a repository E##-## backlog key. Use this for scoped issue implementation driven by .github/project/backlog.yml; do not use it for general questions or pure code review.
---

# Implement Backlog Issue

## Use When

The user asks to implement, continue, fix, or complete a specific backlog issue such as
`E02-01`.

## Do Not Use When

The request is conceptual discussion, broad planning with no key, a pure code review, or a
PR readiness check.

## Workflow

1. Resolve the key in `.github/project/backlog.yml` and read the parent epic plus the
   issue's `issue`, `approach`, `acceptance_criteria`, `priority`, `size`, and
   `initial_status`. If the key is missing, stop and report that.
2. Check prerequisites stated in the backlog or observable in current code.
3. Inspect the relevant code and build files before editing.
4. For substantial work, share a concise implementation plan before editing.
5. Implement the smallest change that satisfies the acceptance criteria.
6. Compose with the specialized skills when relevant: `gradle-kmp-change` for build
   changes, `kmp-boundary-review` for shared/platform boundary changes, `testing-change`
   for test design.
7. Add or update meaningful tests when behavior changes.
8. Validate narrowly, then more broadly when appropriate.
9. Inspect the final diff for unrelated changes.

## Project References

- [Backlog workflow](../../../docs/workflows/backlog.md) — the canonical version of this
  loop, including status language.
- [Agent workflow](../../../docs/ai/agent-workflow.md) — exploration, scope discipline,
  decision documentation.
- [Validation](../../../docs/development/validation.md) — every check that exists here.

## Output

Report each acceptance criterion as satisfied, not satisfied, or not validated; the exact
commands run; and anything that could not be validated.
