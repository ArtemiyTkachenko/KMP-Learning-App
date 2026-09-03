# Agent Workflow

The default loop for a coding agent working in this repository, and how to reach the
information a task needs without loading the whole project.

## Default Loop

1. Read the task. If it names an `E##-##` key, follow [backlog](../workflows/backlog.md).
2. Read the nearest applicable `AGENTS.md` for the directory you are changing.
3. Inspect only the relevant code and documentation.
4. Identify an existing implementation of the same shape and use it as the reference.
5. Make the smallest coherent change.
6. Run targeted validation, then broader validation if warranted. See
   [validation](../development/validation.md).
7. Review the resulting diff, and check `git status --short` for stray build or cache
   output.
8. Report: files changed, important decisions, validation performed, unresolved concerns.

Trivial changes do not need a planning phase. Use planning or investigation first for
architecture changes, migrations, broad refactors, and unclear tasks.

## Efficient Exploration

- Search by symbol or path before reading a directory; scanning the repository without a
  reason wastes the context the task actually needs.
- Read the relevant range of a large file rather than the whole file, and do not re-read
  unchanged files.
- Load a documentation file only when the task touches its subject. The
  [documentation map in `AGENTS.md`](../../AGENTS.md#documentation) says which file
  answers which question.
- Avoid commands that produce large output when a narrower one answers the question. When
  a Gradle task fails, read the specific failure rather than the entire log.

## Prefer The Existing Pattern

Prefer an existing repository pattern over a generic framework best practice, unless the
task explicitly changes the project's architecture. Before implementing a feature, find
whether a similar one already exists and follow it. This matters most for Compose screens
and state holders, repositories and their domain boundaries, Koin modules, Room entities
and migrations, Navigation 3 routes, KMP `expect`/`actual` abstractions, tests, and GitHub
Actions workflows.

## Scope Discipline

- Implement the smallest change that satisfies the request; do not broaden a backlog issue.
- No unrelated refactors, and no infrastructure or libraries added only for architectural
  purity.
- Preserve generated and project files unless the task specifically requires changing them.
- Before finishing, verify every changed file is directly related to the requested task.

## Decision Documentation

When a non-obvious implementation or architectural decision is made, preserve the
reasoning, preferring in order:

1. Clear code and naming that make the decision self-explanatory.
2. A concise code comment when the reason cannot be inferred from the code.
3. Existing architecture documentation in [`docs/architecture/`](../architecture/overview.md)
   when the decision affects multiple files or modules.

When choosing between several reasonable approaches and the choice is not already
documented, state the decision and its rationale in the handoff, and persist it in
repository documentation when it is likely to matter later.

## Learning Handoff

This repository is a learning and portfolio project, so non-trivial generated changes come
with an explanation. Cover the relevant subset: what changed and why this approach; the
Android, KMP, Compose, Gradle, or testing concepts involved; which files to review first;
non-obvious configuration; architectural decisions, tradeoffs, and alternatives
considered; the checks executed and what they demonstrate; anything not validated; and a
few review questions the developer should be able to answer after reading the diff.

Keep it concise. Separate facts from inferences, do not present uncertain claims as
established project decisions, and do not explain trivial Kotlin syntax unless it matters
to a larger concept.

## Scope Boundaries Of The Project

- No custom backend or analytics unless the project scope explicitly changes.
- No native SwiftUI features; `iosApp` exists to host shared Compose UI.
- Do not expand classic Android Views or Fragments into a large legacy architecture; use
  them only for narrowly scoped refresher or interoperability work when requested.
