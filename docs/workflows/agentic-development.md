# Adversarial Agentic Development

`.github/workflows/agentic-debate.yml` is an opt-in workflow that lets Codex and Claude
work the same engineering task from deliberately different roles.

## Roles

### Codex

Codex owns two responsibilities:

1. **Task authoring.** It turns a high-level objective (and, optionally, an existing
   `E##-##` backlog key) into the concrete task contract: problem, approach, scope,
   acceptance criteria, repository constraints, and validation expectations.
2. **Independent review.** After Claude implements, Codex reviews the real branch diff.
   It must keep stable finding IDs, respond directly to Claude's challenges, and withdraw
   findings when Claude's counterargument or fix resolves them.

Codex never edits the implementation in this workflow.

### Claude

Claude owns implementation, an internal quality barrier, and rebuttal:

1. **Initial implementation.** It implements the Codex-authored task using repository
   guidance and existing patterns.
2. **Self-review barrier.** A fresh read-only Claude context independently reviews the
   implementation, a separate Claude fixer context addresses its material findings, and
   this review → fix cycle runs exactly twice before Codex review begins. Each self-review
   is posted to the draft PR by a trusted non-agent job.
3. **Challenge / correction.** For each later material Codex finding, Claude explicitly
   accepts, partially accepts, or rejects it. Accepted findings are fixed. Rejected findings
   must be challenged with concrete repository, behavioral, test, documentation, or
   language/framework evidence.

The self-reviewer cannot edit code, and the fixer does not inherit the reviewer's session
state. Claude is not required to implement a review suggestion merely because Codex
proposed it; a self-review finding may be rejected only with concrete evidence.

## Claude Quality Barrier

The draft PR is opened immediately after Claude's initial implementation. Before Codex
reviews the branch, Claude runs two independent quality cycles:

```text
Claude implementation
    ↓
Claude self-review 1 (fresh, read-only context)
    ↓
Claude fix 1 (fresh implementation context)
    ↓
Claude self-review 2 (fresh, read-only context)
    ↓
Claude fix 2
    ↓
Codex review begins
```

Both review results are posted to the draft PR. The second fix is the version Codex sees.
There is intentionally no third Claude self-review: the requested barrier is exactly two
review → fix cycles.

## Negotiation Loop

After the Claude barrier, one Codex negotiation iteration is:

```text
Codex review
    ↓
consensus? ── yes → final CI
    │
    no
    ↓
Claude fix / challenge
```

There are at most five such iterations.

After Claude's fifth response, Codex performs one final **reconciliation check**. This is
not a sixth review round: its purpose is to determine whether Claude's fifth response
resolved the iteration-5 objections. It should not introduce unrelated new findings.

Consensus requires all of the following:

- Codex returns `ACCEPT`;
- Codex returns `consensus: true`;
- no active `BLOCKING` or `IMPORTANT` findings remain.

The workflow calculates that gate itself rather than trusting the boolean alone.

## Human Escalation

If the final reconciliation check still has no consensus, the workflow does **not** merge
and does **not** choose a winner. The PR remains draft.

It posts a human decision packet containing:

- the Codex-authored task;
- Codex's final detailed-but-succinct position;
- Claude's final detailed-but-succinct position;
- each remaining blocking/important finding;
- Codex's requested change and rationale;
- Claude's stance, reasoning, and action where available.

The developer then decides which argument to accept or defines a third resolution.

An API failure, action crash, artifact failure, or other automation problem is classified
separately as a workflow failure. It is never presented as an engineering disagreement.

## Deterministic CI

AI consensus is not a merge gate by itself. Once the two agents reach consensus, the
controller dispatches a fresh run of the repository's normal `main.yml` CI against the
final branch and waits for it to succeed.

Only after that run passes is the draft PR marked ready. If `merge_when_consensus` was
enabled for the manual run, the trusted workflow step then squash-merges it.

The normal PR-triggered CI may also run during the negotiation as Claude pushes revisions.
The CI workflow's concurrency policy cancels stale runs when a newer branch revision
arrives.

## Trust Boundaries

- Codex task/review jobs use read-only repository permissions.
- Claude reviewer model jobs use read-only repository permissions and no editing tools.
- Claude implementer/fixer model jobs use read-only GitHub permissions and a checkout
  without persisted credentials. They can edit only their local workspace.
- Claude emits a binary-safe patch artifact.
- A separate non-agent job gets `contents: write`, applies the patch, commits, and pushes.
- PR creation, CI dispatch, comments, readiness changes, and merge happen in ordinary
  trusted workflow steps.
- The first version is manual-dispatch only; public issue/comment text cannot trigger API
  credentials.

## Inputs

`Agentic Codex-Claude debate` accepts:

- `objective` — a high-level engineering objective for Codex to turn into the task;
- `backlog_key` — optional existing `E##-##` key whose definition constrains the task;
- `merge_when_consensus` — defaults to `false`.

Keeping merge disabled for early runs makes the generated PR the inspection surface while
the negotiation behavior is being calibrated.

## Required Secrets

The repository needs two Actions secrets:

- `OPENAI_API_KEY` — used by Codex task authoring and review jobs.
- `ANTHROPIC_API_KEY` — used by Claude implementation/challenge jobs.

Neither secret should ever be committed to the repository.
