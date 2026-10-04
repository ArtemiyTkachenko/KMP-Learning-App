# CI

## Build And Test (`.github/workflows/main.yml`)

The workflow is triggered by pull requests to `main`, pushes to `main`, and manual dispatch,
but the runner job executes only when `github.actor` is `ArtemiyTkachenko`. Events triggered
by other accounts leave the job skipped, so public contributors cannot consume this
repository's hosted-runner minutes. When it runs, it uses Ubuntu and Temurin JDK 21. The
job is a fast data gate, then one Gradle invocation, then two verification steps over what
that invocation produced.

### 1. Data gates

Cheap Python steps ahead of the Gradle job, all failing in seconds:

```sh
python3 -m unittest discover -s tools -p 'test_*.py'
python3 tools/question_bank_coverage.py --check
python3 tools/learning_question_coverage.py --check
python3 tools/question_bank_identity.py --against "$baseline"
python3 .github/project/validate_backlog.py .github/project/backlog.yml
```

The two `--check` commands gate two committed content snapshots with different scopes:

- `question_bank_coverage.py` compares the marker-fenced structural regions of
  `docs/content/question-bank-coverage.md` — headline counts, Topic density, deprecated
  Questions, the empty-Subtopic count and the Subtopic index — against
  `initial_curriculum.json`. Adding, deprecating or re-homing a Question, or changing the
  taxonomy, fails the build until the regions are regenerated. The editorial prose around
  them (triage, concept coverage, expansion notes) is human-owned and **not** checked.
- `learning_question_coverage.py` compares the whole of
  `docs/content/learning-question-coverage.md`, which relates learning Lessons to ACTIVE
  Questions, against both authored curricula, so a learning-mapping or Question change
  that leaves it stale fails the build.

CI never regenerates either snapshot; `--write` stays an authoring step.

`question_bank_identity.py` is a cross-revision gate rather than a snapshot. Persisted
attempts reference Questions and AnswerOptions by ID, so it fails when, against the revision
being replaced, a Question disappeared or an existing Question's `selectionMode`,
AnswerOption-ID set or correct-answer set changed. Status, level, Topic/Subtopic placement
and all wording may change. The baseline is the pull request's base SHA, the push's
`github.event.before`, or `main`'s parent commit on manual dispatch. There is no tag or
release to compare against (see [versioning](../development/versioning.md)), and comparing
every accepted revision is the stronger guarantee anyway. The checkout is shallow, so the
step fetches that one commit before comparing; an unknown event, an all-zero `before`, an
unfetchable commit or a baseline without the curriculum fails the step instead of skipping
it. The rules themselves are in
[content authoring](../content/content-authoring.md#enforced-identity-rules).

The backlog validator is the same script `sync-backlog.yml` runs. That workflow is manual
dispatch only, so before this gate existed a malformed `backlog.yml` merged freely and was
discovered only when somebody next ran the sync — with the breakage already on `main`.
PyYAML is installed here, pinned to the version the sync workflow installs, into a virtualenv
under `RUNNER_TEMP` — the runner's system Python is externally managed, so a plain
`pip install` is refused, and a virtualenv inside the workspace would be flagged by the
uncommitted-output check below.

### 2. Build and test

```sh
./gradlew --no-daemon \
  :androidApp:assembleDebug :androidApp:lintDebug \
  :desktopApp:assemble :webApp:assemble :shared:check
```

A single invocation is deliberate: one configuration phase and one Kotlin/Native + JS/Wasm
toolchain setup. `org.gradle.parallel` is not enabled, so the tasks run sequentially in
command-line order. Cheap Android and desktop JVM compilation runs first so a plain compile
break fails fast; `:shared:check` runs last because it is by far the longest, and it is
what runs the JVM, Android host, JS and Wasm test targets.

`:webApp:assemble` and `:shared:check` resolve npm dependencies through Kotlin's Yarn tasks,
which consume the committed `kotlin-js-store/yarn.lock` and `kotlin-js-store/wasm/yarn.lock`.
The root build script makes `kotlinStoreYarnLock` and `kotlinWasmStoreYarnLock` fail the run when
resolution disagrees with a committed lockfile or a lockfile is missing; there is no separate
lockfile step. See [validation](../development/validation.md#jswasm-dependency-lock).

`:androidApp:lintDebug` sits beside the Android build because it reuses that compilation
and costs seconds. **Read its scope precisely:** it analyses the Android shell — two Kotlin
files, the manifest and the Android resources — and nothing in `:shared`. The
`com.android.kotlin.multiplatform.library` plugin exposes no main-variant lint task for
that module, and `checkDependencies` does not reach it; this was confirmed by planting a
`NewApi` violation in `shared/src/androidMain` and an unremembered-state violation in
`shared/src/commonMain`, neither of which lint reported, while the same `NewApi` violation
in `androidApp/src/main` aborted the build. Android Lint is therefore a gate on the shell,
not static analysis of the application. `androidApp/build.gradle.kts` disables the three
detectors that report newer dependency releases, because they contact the dependency
repositories on every run and so change their output when somebody else publishes.

### 3. Verification of what the build produced

```sh
git status --porcelain   # must be empty
```

The build writes into the source tree: Room exports its schemas to `shared/schemas`, which
is tracked on purpose. Without this step, a `CurriculumDatabase` version bump whose schema
JSON was never committed produces a green run — CI regenerates the file, the migration test
passes against it, and `main` ends up with a schema version that exists on no machine but
the runner. Generated build output is gitignored, so a clean tree is the normal case; a
failure here means a build artifact belongs in the commit.
The same check protects the tracked Yarn lockfiles: a build that rewrote either of them
would leave the tree dirty and fail here.

Test reports for every target `:shared:check` ran are uploaded as `shared-test-reports`.
The Android host, JS and Wasm reports matter most: those are the failures least
reproducible on a developer's macOS machine.

**What CI does not cover:** Kotlin/Native iOS compilations are disabled on a Linux runner,
so this job gives no iOS signal at all. iOS framework linking and simulator runtime
verification are local macOS checks. Do not describe a green CI run as iOS validation.
There is also no static analysis of `:shared` — no Detekt, no ktlint, and no Android Lint
reach, as described above — no instrumentation or browser end-to-end tests, and no line
coverage.

## Backlog Sync (`.github/workflows/sync-backlog.yml`)

Manual dispatch only, with the job restricted to `ArtemiyTkachenko`. Installs PyYAML, validates `.github/project/backlog.yml`, verifies
the runner's GitHub CLI supports parent/sub-issue functionality, then synchronizes the
backlog to the GitHub Project. See [backlog](backlog.md) and `.github/AGENTS.md`.

## Reporting CI Status

Never claim CI passed unless actual CI results were observed. Local `./gradlew check`
success is not a CI result.
