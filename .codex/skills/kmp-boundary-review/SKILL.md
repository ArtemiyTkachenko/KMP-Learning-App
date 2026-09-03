---
name: kmp-boundary-review
description: Review or guide Kotlin Multiplatform boundaries in this project. Use when code moves between common/platform source sets, expect/actual APIs are introduced, or module/source-set dependencies change; not for ordinary Kotlin edits with no KMP boundary impact.
---

# KMP Boundary Review

## Use When

A task adds or moves shared code, changes source sets, introduces a platform-specific
implementation, adds `expect`/`actual`, or changes dependencies between shared and
platform modules.

## Do Not Use When

The work is `.github` automation, or ordinary Kotlin changes with no boundary impact.

## Workflow

1. Identify which source sets and modules the change touches.
2. Walk the boundary review questions in
   [KMP boundaries](../../../docs/development/kmp.md#boundary-review-questions).
3. Confirm dependency direction and that source-set dependencies are declared where they
   are consumed.
4. Check that tests landed in the source set matching the behavior's scope.

## Project References

- [KMP boundaries](../../../docs/development/kmp.md) — modules, targets, source sets, and
  the rules being reviewed against.
- [Persistence](../../../docs/architecture/persistence.md) — the existing platform
  boundary for the database, a useful reference for new platform abstractions.
- [Testing](../../../docs/development/testing.md) — test source-set placement.

## Output

Separate blocking correctness issues from maintainability concerns and optional
suggestions. Recommend the smallest correction that preserves the intended boundary. Do
not propose a broad architecture rewrite merely because another KMP structure is
theoretically possible.
