---
name: kmp-code-review
description: Perform a project-specific review of a change, diff, or branch. Use when asked to review code, find bugs, assess implementation quality, or review before PR; do not automatically implement findings.
---

# KMP Code Review

Read `.codex/skills/code-review/SKILL.md` now and follow it exactly. That file is the
single source of truth for this skill and is shared with Codex — do not restate or
reinterpret its rules here.

The source file is named `code-review` while this skill is named `kmp-code-review`. That
mismatch is intentional, not an error: Claude Code ships a built-in `code-review` skill,
and the rename keeps both available. The built-in `/code-review` is a separate,
general-purpose tool with its own effort levels and multi-agent `ultra` mode; use this
skill when the review should follow this repository's rules.

Also apply the "Code Review Rules" section of `AGENTS.md`.
