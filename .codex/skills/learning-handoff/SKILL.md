---
name: learning-handoff
description: Explain substantial agent-generated changes for learning and portfolio review. Use after non-trivial implementation/setup work or when requested; distinguish architectural choices and tradeoffs from trivial syntax.
---

# Learning Handoff

Use this skill after substantial generated implementation/setup work, or whenever the user explicitly asks for a learning-oriented explanation.

Do not use it for every tiny mechanical edit, and do not explain trivial Kotlin syntax unless it matters to a larger concept.

## Handoff Content

Cover the relevant subset:

1. What changed.
2. Why this approach was chosen.
3. Android, KMP, Compose, Gradle, or testing concepts involved.
4. Files the developer should review first.
5. Non-obvious configuration.
6. Architectural decisions and tradeoffs.
7. Alternatives considered.
8. Tests/checks executed and what they demonstrate.
9. Anything that was not validated.
10. A few review questions the developer should be able to answer after reading the diff.

Keep it concise. Separate facts from inferences, and do not present uncertain architectural claims as established project decisions.

## Repository Context To Include When Relevant

- Whether code belongs in `commonMain` or a platform source set.
- Why a dependency belongs in a specific source set or module.
- Which Gradle command validated the change.
- How the change maps to a backlog issue's acceptance criteria.
