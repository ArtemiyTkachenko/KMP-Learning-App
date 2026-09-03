# KMP Learning App — Claude Code Guide

`AGENTS.md` is the authoritative agent guide for this repository and applies in full to
Claude Code. It is imported below, so it is already in context — do not read it again.

@AGENTS.md

## Skills In Claude Code

`.claude/skills/` holds thin delegates. Each one names the matching file in
`.codex/skills/`, which holds the actual rules shared with Codex. When a skill fires,
read that `.codex` file and follow it — do not restate or reinterpret its rules.

The repository review skill is `kmp-code-review` because Claude Code ships a built-in
`/code-review`. The rename keeps both available: use `kmp-code-review` when the review
should follow this repository's rules, and the built-in `/code-review` for its own effort
levels and multi-agent `ultra` mode.

## Context Efficiency

`AGENTS.md` is the only project documentation loaded automatically. Everything under
`docs/` is opt-in — load a file when the task touches its subject, using the documentation
map in `AGENTS.md`, and stop there rather than reading the neighbouring documents.

- Search by symbol or path before exploring a directory; `docs/architecture/` names most
  of the types worth grepping for.
- Read the relevant range of a large file. `docs/architecture/persistence.md` and the
  files under `docs/content/` are long enough that reading them whole is usually waste.
- Prefer targeted Gradle tasks over `./gradlew check`, and read the specific failure
  rather than the whole log.
- Use a subagent only for a genuinely broad, independent investigation — a coverage
  survey, a repository-wide audit. A single search or a known-file read is cheaper
  inline.
- Start a fresh context for an unrelated task rather than carrying an old one forward.

## Repository Behavior

- Never run `git commit`, push, or open a PR without explicit approval, on any branch.
- Do not modify `.github/` workflows or backlog sync behavior while implementing product
  code unless the issue explicitly asks for workflow work.
- Expensive analysis worth keeping — a coverage review, an audit, a survey — belongs in a
  file under `docs/` so a later session reads it instead of re-deriving it.
