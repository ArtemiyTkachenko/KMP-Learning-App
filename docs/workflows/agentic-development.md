# Agentic Development Pipeline

`.github/workflows/agentic-task.yml` is an opt-in workflow for implementing one canonical
`E##-##` backlog item with independent planning, implementation, deterministic validation,
review, correction, and optional merge.

It is deliberately separate from E03-08. E03-08 covers automatic semantic review of normal
pull requests; this workflow owns the lifecycle of PRs it generates itself.

## Roles

- **Claude planner**: reads the canonical backlog entry and relevant repository context and
  produces a schema-validated implementation contract. It has no write tools.
- **Codex implementer**: edits a clean checkout using the planner contract. It has workspace
  write access but no persisted GitHub credentials.
- **Deterministic validation**: runs the same authored-content checks and Gradle tasks as the
  normal CI workflow.
- **Claude reviewer**: independently reviews the task, complete diff, touched files, and
  validation evidence. It has read-only tools and returns schema-validated findings.
- **Codex fixer**: receives the task, plan, previous review, and failed validation evidence.
  It edits the branch on a fresh runner. Up to two correction cycles are attempted.
- **Trusted GitHub steps**: apply the generated patch, commit/push it, create the PR, dispatch
  fresh repository CI, and optionally squash-merge. The LLM steps do not own write credentials.

## Why Codex Runs In Fresh Jobs

`openai/codex-action` defaults to dropping sudo privileges and recommends a fresh job for a
later Codex invocation. Keeping implementation and each fix cycle in separate jobs also
means every correction starts from the exact pushed branch state rather than hidden agent
session state.

## Trigger And Inputs

The first version is manual-only:

```text
Actions -> Agentic backlog task -> Run workflow
```

Inputs:

- `issue_key`: canonical backlog key such as `E27-08`.
- `merge_when_clean`: when `true`, the final trusted step dispatches fresh repository CI and
  squash-merges only after it is green. When `false`, it leaves a ready PR open.

Manual dispatch is intentional for the first version. A later issue-label trigger can reuse
the same jobs after the workflow is proven stable.

## Required Secrets

- `OPENAI_API_KEY` for `openai/codex-action`.
- `ANTHROPIC_API_KEY` for `anthropics/claude-code-action`.

Claude Code also supports other authentication modes, but this workflow uses the direct API
key path so its trust model is explicit. Do not expose either secret to repository code or
prompts.

## Gate Rules

A cycle is clean only when both are true:

1. deterministic validation succeeded; and
2. the independent review returned `PASS` with no `BLOCKER` or `IMPORTANT` findings.

If the first pass is not clean, the fixer gets the validation log and review JSON. The
workflow then re-runs deterministic validation and a fresh review. It permits at most two
fix cycles; if the last cycle is still not clean, no PR is created and no merge is attempted.

After a clean agent cycle, the workflow creates a PR and explicitly dispatches the repository's
normal `main.yml` CI on the final branch. A remote CI failure leaves the PR open and fails the
workflow. The agent pipeline never treats its own earlier Gradle result as proof that GitHub CI
passed.

## Trust Boundaries

- Agent checkouts use `persist-credentials: false`.
- The Codex job has `contents: read`; it emits a binary-safe patch artifact instead of pushing.
- A separate non-agent job with `contents: write` applies that patch and pushes the branch.
- Claude planner/reviewer receive read tools only.
- Commit, push, PR creation, CI watching, and merge happen in ordinary shell steps after
  the agent process exits.
- The workflow is manual-dispatch only, so untrusted public issue/comment text cannot invoke
  provider credentials in v1.

## Validation

The deterministic validation job intentionally mirrors `.github/workflows/main.yml`:

```sh
python3 -m unittest discover -s tools -p 'test_*.py'
python3 tools/learning_question_coverage.py --check
./gradlew --no-daemon \
  :androidApp:assembleDebug \
  :desktopApp:assemble \
  :webApp:assemble \
  :shared:check
```

As with normal CI, this does not provide iOS validation on the Linux runner.
