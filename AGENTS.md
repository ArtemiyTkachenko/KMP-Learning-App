# KMP Learning App — Agent Guide

An Android interview preparation app built as a Kotlin Multiplatform learning and
portfolio project: shared Compose Multiplatform UI, Koin, Room, and Navigation 3 in
`:shared`, hosted by thin Android, Desktop, iOS, and web shells. Generated changes should
support learning — keep decisions visible, reviewable, and explainable.

This file is an index. Load a linked document only when the task touches its subject.

## Repository Map

| Path | Contents |
| --- | --- |
| `shared/` | The KMP module: shared Compose UI, domain, data. Has its own `AGENTS.md`. |
| `androidApp/`, `desktopApp/`, `webApp/` | Platform application shells. `androidApp/` has its own `AGENTS.md`. |
| `sqliteWasmWorker/` | SQLite WASM worker packaged for the web Room driver. |
| `iosApp/` | Xcode shell embedding the `:shared` framework. |
| `.github/` | CI workflows and backlog automation. Has its own `AGENTS.md`. |
| `docs/` | Canonical project documentation (see below). |
| `tools/` | Repository authoring tooling. Not part of the application build. |
| `.codex/skills/`, `.claude/skills/` | Repository skills. |

## Critical Rules

- **Smallest sufficient change.** No unrelated refactors, no silently broadened backlog
  scope, no dependencies or infrastructure added for architectural purity alone.
- **Inspect before changing.** Read the existing code and build files, and follow the
  pattern a comparable existing feature already uses.
- **Keep `commonMain` platform-independent.** Application shells depend on `:shared`,
  never the reverse. See [KMP boundaries](docs/development/kmp.md).
- **Versions live in `gradle/libs.versions.toml`.** No literal versions in module build
  files. See [Gradle](docs/development/gradle.md).
- **Narrowest visibility** for new and touched Kotlin declarations. See
  [Kotlin style](docs/development/kotlin.md).
- **Test meaningful behavior changes**, in the source set the behavior belongs to. See
  [testing](docs/development/testing.md).
- **Validate every question you write or change.** Generation is not validation: solve the
  finished question yourself before trusting its answer key, and open every source rather
  than trusting the URL. See
  [question validation](docs/content/question-validation.md).
- **Report validation honestly.** Name the commands run; never claim a check, CI run, or
  merge that was not observed.
- **Check `git status --short` before reporting completion** and keep generated build or
  cache output out of the diff.

## Workflow

Read the task -> read the nearest `AGENTS.md` -> inspect only what is relevant -> follow
an existing pattern -> make the smallest coherent change -> validate narrowly, then
broadly -> review the diff -> report files changed, decisions, validation, and open
concerns.

Full loop, exploration efficiency, decision documentation, and learning-handoff
expectations: [agent workflow](docs/ai/agent-workflow.md).

## Validation

Run the narrowest check that proves the change, then widen. Do not run heavy Gradle/KMP
commands in parallel.

```sh
./gradlew :shared:jvmTest            # fastest shared signal
./gradlew :shared:allTests           # all shared targets
./gradlew :androidApp:assembleDebug  # Android compile
./gradlew :shared:check              # broadest shared check, slowest
./gradlew check                      # repository-wide
```

Full command reference: [validation](docs/development/validation.md). What CI does and
does not cover: [CI](docs/workflows/ci.md).

## Documentation

**Architecture** — what the code currently does:

- [Overview](docs/architecture/overview.md): app composition, Koin, Navigation 3, runtime hosts, curriculum content model.
- [Assessment](docs/architecture/assessment.md): assessment configuration, selection, taking, scoring, retakes.
- [Progress](docs/architecture/progress.md): derived accuracy, coverage, recent performance, learning context on study surfaces.
- [Practice selection](docs/architecture/practice-selection.md): level and question-source policies.
- [Recommendations](docs/architecture/recommendations.md): guided learning policy and Continue Studying.
- [Practice builder](docs/architecture/practice-builder.md): building and persisting a practice configuration.
- [Persistence](docs/architecture/persistence.md): Room schema, migrations, import policy, platform storage.

**Development** — how to write code here:
[Kotlin style](docs/development/kotlin.md) ·
[KMP boundaries](docs/development/kmp.md) ·
[Gradle](docs/development/gradle.md) ·
[testing](docs/development/testing.md) ·
[validation](docs/development/validation.md)

**Workflows**:
[backlog and issues](docs/workflows/backlog.md) ·
[code review](docs/workflows/code-review.md) ·
[CI](docs/workflows/ci.md)

**Content — assessment** — the interview question bank, not application code:
[curriculum](docs/content/curriculum.md) ·
[authoring contract](docs/content/content-authoring.md) ·
[authoring playbook](docs/content/question-authoring-playbook.md) ·
[validation standard](docs/content/question-validation.md) ·
[coverage snapshot](docs/content/question-bank-coverage.md)

**Content — learning** — the explanatory study material, not the question bank:
[learning-content authoring contract](docs/content/learning-content-authoring.md) ·
[Compose learning blueprint](docs/content/compose-learning-blueprint.md) ·
[learning-to-question coverage](docs/content/learning-question-coverage.md)

## Skills

Task-triggered workflows live in `.codex/skills/`, which holds the rules shared by Codex
and Claude Code. `.claude/skills/` contains thin delegates so the same skill is available
in both tools.

| Skill | Use when |
| --- | --- |
| `implement-backlog-issue` | Implementing, continuing, or fixing an `E##-##` backlog issue. |
| `question-bank-change` | Adding, editing, auditing, or fixing questions in the bundled question bank. |
| `gradle-kmp-change` | Changing build files, version catalog, targets, plugins, or source-set dependencies. |
| `kmp-boundary-review` | Code moves between common and platform source sets, or `expect`/`actual` changes. |
| `testing-change` | Designing or verifying tests for a behavior change. |
| `code-review` (`kmp-code-review` in Claude Code) | Reviewing a diff, branch, or implementation. |
| `pr-readiness` | Deciding whether work is ready for review, PR, or merge. |
| `learning-handoff` | Explaining substantial generated changes for learning review. |
