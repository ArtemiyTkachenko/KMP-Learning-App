---
name: learning-handoff
description: Explain substantial agent-generated changes for learning and portfolio review. Use after non-trivial implementation/setup work or when requested; distinguish architectural choices and tradeoffs from trivial syntax.
---

# Learning Handoff

## Use When

Substantial generated implementation or setup work has just finished, or the user asks for
a learning-oriented explanation.

## Do Not Use When

The edit was small and mechanical.

## Workflow

1. Review the final diff.
2. Identify the decisions a reader could not infer from the code alone.
3. Write the handoff, covering the relevant subset of the content listed in
   [agent workflow](../../../docs/ai/agent-workflow.md#learning-handoff).
4. Where a decision is likely to matter later, persist the reasoning in repository
   documentation rather than only in the handoff.

## Project References

- [Agent workflow](../../../docs/ai/agent-workflow.md) — the handoff content list and the
  decision-documentation preference order.
- [Architecture](../../../docs/architecture/overview.md) — where a durable architectural
  decision belongs.

## Repository Context To Include When Relevant

- Whether code belongs in `commonMain` or a platform source set, and why.
- Why a dependency belongs in a specific source set or module.
- Which command validated the change.
- How the change maps to a backlog issue's acceptance criteria.

## Output

A concise explanation. Separate facts from inferences, do not present uncertain
architectural claims as established project decisions, and do not explain trivial Kotlin
syntax unless it matters to a larger concept. Close with a few review questions the
developer should be able to answer after reading the diff.
