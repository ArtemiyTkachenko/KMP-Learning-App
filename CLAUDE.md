# KMP Learning App — Claude Code Guide

The authoritative agent guide for this repository is `AGENTS.md`. It applies in full to
Claude Code. Read and follow it.

@AGENTS.md

Project skills live in `.claude/skills/`. Each one delegates to the matching file in
`.codex/skills/`, which holds the actual rules shared by both Claude Code and Codex. Do
not restate or reinterpret those rules — read the `.codex` file when a skill fires.
