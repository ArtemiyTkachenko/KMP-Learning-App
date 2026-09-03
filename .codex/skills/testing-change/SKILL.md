---
name: testing-change
description: Design and verify tests for behavior changes in this KMP project. Use when adding behavior, fixing bugs, changing domain/data/presentation logic, or modifying automated tests; not for test-count padding or unrelated documentation-only edits.
---

# Testing Change

## Use When

Adding meaningful behavior, fixing a bug, changing domain/data/presentation logic, adding
or modifying tests, or addressing a known coverage gap.

## Do Not Use When

The goal is to raise a test count with tautological tests or unnecessary mocks, or the
change is documentation only.

## Workflow

1. Decide what observable behavior changed, and which scope owns it.
2. Choose the source set from
   [testing](../../../docs/development/testing.md#test-source-sets) — `commonTest` only
   when the behavior is genuinely shared.
3. Find an existing test for a comparable feature and follow its shape.
4. Write the test against observable behavior, covering meaningful boundary and failure
   cases; for a bug fix, add regression coverage.
5. Run the narrowest test task that exercises it, then widen.

## Project References

- [Testing](../../../docs/development/testing.md) — source sets, available libraries, and
  test design rules.
- [Validation](../../../docs/development/validation.md) — every test command that exists
  here.
- [Kotlin style](../../../docs/development/kotlin.md) — test classes are normally
  `internal`; confirm discovery after narrowing visibility.

## Output

Report the exact commands run and whether any expected validation was skipped or
unavailable.
