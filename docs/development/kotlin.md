# Kotlin Code Style

Repository-specific Kotlin conventions. Anything not stated here follows ordinary
idiomatic Kotlin.

## Visibility

Use the narrowest practical visibility for new declarations and for declarations changed
during a task.

- `private` for file-local helpers and implementation details.
- `internal` for module/source-set implementation details, including most test classes.
- Public only for intentional module APIs, framework entry points, serialization or
  reflection requirements, Compose previews that need it, and platform lifecycle APIs.

After reducing the visibility of a test class, run the relevant test task to confirm test
discovery still works. See [testing](testing.md).

## Abstraction

- Prefer small, understandable, idiomatic Kotlin and Compose over clever abstractions.
- Avoid premature abstraction and unrelated refactors.
- Follow the pattern an existing comparable feature already uses rather than introducing a
  second way to do the same thing.

## Comments

Comments explain **why** a decision or constraint exists, not what the code does. If a
future maintainer would reasonably ask "why was this done this way?", the reasoning
belongs somewhere durable — see [agent workflow](../ai/agent-workflow.md#decision-documentation).
