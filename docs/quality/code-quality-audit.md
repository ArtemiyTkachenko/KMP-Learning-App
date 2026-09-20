# Code Quality Audit

## Purpose

This ledger is the durable state for a repository-wide code-quality audit of
`ArtemiyTkachenko/KMP-Learning-App`. The audit is intentionally split into bounded passes so
later sessions can review deeply without duplicating work. Part 0 records the baseline and
inventory only; it makes no substantive production findings.

## Baseline

| Field | Value |
| --- | --- |
| Audit start date | 2026-09-20 |
| Baseline commit | `75e7b30c9b08a5ea1cf275c1c7793bfa4ab4c96f` (merge of PR #403, E27-09) |
| Baseline branch | `task/code-quality-audit-1`, exactly aligned with `origin/main` at audit start |
| Local `main` note | Local `main` was stale at `86afc9b`; it was not used as the baseline |
| Initial working tree | Clean: `git status --short` produced no output |
| Repository state | E27 complete; unmerged PR #402 is not present in the baseline and is excluded |

The baseline is a commit identity, not a promise that later passes will run against an unchanged
branch. Every pass must record the commit it actually reviews and account for changes since this
baseline.

## Audit Principles

### No issue quota

An audited area may legitimately produce **No material issue found**. Do not invent refactors to
prove that a review happened.

### Evidence over preference

A finding must be supported by concrete code behavior, duplication, lifecycle semantics, test
risk, API misuse, or maintainability cost. “I prefer another style” is not a finding.

### Recomposition is not inherently a defect

Report recomposition only when there is a concrete reason it is unnecessarily broad, repeated,
expensive, or behaviorally dangerous. “A composable recomposes” is not equivalent to “performance
bug.”

### Abstraction is not inherently improvement

Extract similar code only when it represents the same stable concept or behavior. Do not promote
premature generic abstractions.

### Tests are evaluated by behavioral confidence

Line coverage percentage is not the primary quality metric. Ask: **Which important behaviors could
regress, and what test proves they still work?**

### Compose review rule

Part 1 investigates state-read and recomposition scope, stability, expensive composition work,
`remember`, `derivedStateOf`, effects and keys, collection and lazy-list identity, meaningful
lambda/object allocation, layout/modifier work, lifecycle-aware collection, accessibility, and
duplicated UI. It does not flag normal recomposition, harmless object creation, or unstable
parameters without evidence of meaningful repeated work or incorrect behavior.

### Duplicated UI extraction rule

Similar UI is not automatically one abstraction. Extract only the same stable UI concept. Do not
create a shared component dominated by flags such as `isCompact`, `showSubtitle`,
`useLargePadding`, `enableTrailingIcon`, `alternateLayout`, and screen-specific exceptions merely
to remove repeated lines. Similar composables may legitimately remain separate.

### Bug-review rule

Potential bugs include stale async results, races, cancellation errors, duplicated initialization,
inconsistent or impossible state, invalid empty/error assumptions, wrong IDs, ordering problems,
persistence inconsistencies, lifecycle ownership errors, process/recreation assumptions,
incorrect platform divergence, and swallowed failures, as well as crashes.

### API/design-friction rule

API/design friction is a formal audit category. Later passes inspect unrelated parameter bundles,
boolean-state explosions, impossible combinations, broad visibility, forwarding-only abstractions,
implementation concepts leaking through repository APIs, UI concerns in data/domain models,
demonstrably misusable string identifiers, and APIs callers can easily invoke incorrectly. Record
evidence and cost; do not automatically redesign the API.

### Test-review rule

**Test quality is evaluated by behavioral risk, not by maximizing test count.** Part 6 classifies
important behavior as well protected, partially protected, high-risk and untested, medium-risk and
untested, not worth isolated unit testing, better protected by integration test, or better
protected by UI test. It does not demand a test for every class or function.

## Severity

| Severity | Definition |
| --- | --- |
| Critical | Likely data loss, corruption, security issue, severe crash loop, or fundamental correctness failure. |
| High | Real bug or design flaw likely to produce incorrect user-visible behavior, lifecycle failure, major race, serious state inconsistency, or a high-risk maintenance problem. |
| Medium | Meaningful maintainability, performance, duplication, lifecycle, API-design, or coverage issue worth fixing but not immediately dangerous. |
| Low | Localized smell, small duplication, naming/API friction, or inexpensive cleanup with limited effect. |
| Observation | Worth recording, but current evidence does not establish a defect or justify a change. |

## Confidence

Confidence is tracked separately from severity: **High**, **Medium**, or **Low**. For example, High
severity / Medium confidence differs materially from Medium severity / High confidence. Fix
prioritization should generally favor high-confidence findings, not dramatic possibilities.

## Finding Statuses

- `Open`
- `Fixed`
- `Deferred`
- `Accepted as-is`
- `Needs measurement`
- `Not a defect`

Never delete a rejected or resolved finding. Update its status and rationale to preserve the audit
trail. Finding IDs are stable, grouped by area, never renumbered, and never reused:
`CQ-UI-###`, `CQ-STATE-###`, `CQ-DATA-###`, `CQ-DI-###`, `CQ-KMP-###`, `CQ-TEST-###`,
`CQ-CROSS-###`, and `CQ-BUG-###`.

## Finding Ledger

| ID | Area | Severity | Confidence | File(s) | Finding | Evidence | Recommendation | Status |
| -- | ---- | -------- | ---------- | ------- | ------- | -------- | -------------- | ------ |
| — | — | — | — | — | No findings recorded yet — inventory pass only. | — | — | — |

## Audit Pass Log

| Pass | Area | Status | Commit reviewed | Files reviewed | Findings | Fixes | Validation | Notes |
| ---- | ---- | ------ | --------------- | -------------: | -------: | ----: | ---------- | ----- |
| Part 0 | Baseline and inventory | Complete | `75e7b30c9b08a5ea1cf275c1c7793bfa4ab4c96f` | 409 Kotlin paths inventoried plus build, CI, and architecture documentation | 0 | 0 | Four Gradle baseline checks; `git diff --check`; documentation-only final status | No production quality review performed. |

## Baseline Health

| Check | Result | Failures/warnings | Notes |
| ----- | ------ | ----------------- | ----- |
| `./gradlew :shared:jvmTest` | PASS | None observed | Build successful; test task was up-to-date. |
| `./gradlew :androidApp:testDebugUnitTest` | PASS | None observed | Build successful; `:androidApp:testDebugUnitTest` was `NO-SOURCE`, so this proves task/configuration health, not Android unit-test execution. |
| `./gradlew :androidApp:lintDebug` | PASS | None reported | Android lint completed and wrote its HTML report under generated build output. |
| `./gradlew :shared:check` | PASS | None observed | Build successful. JVM, Android host, JS, Wasm, and iOS simulator task graph completed; many tasks were up-to-date. |

No Compose/Web asset-size, Kotlin/Native bundle-ID, or duplicate KLIB `unique_name` warning was
observed in these commands. The repository documents those signals as potentially non-fatal; a
future pass should record them if observed, without creating a finding unless behavior differs
materially from the documented signal.

## Repository Architecture Inventory

### Gradle and application modules

The real Gradle module set comes from `settings.gradle.kts`:

| Module/host | Responsibility |
| --- | --- |
| `:shared` | KMP domain, data, Room persistence, presentation/state, shared Compose UI, DI, and platform implementations. |
| `:androidApp` | Thin Android application, `Application`, `Activity`, manifest, and launcher resources. |
| `:desktopApp` | Thin Compose Desktop JVM entry point and packaging. |
| `:webApp` | Thin executable JS/Wasm browser entry point. |
| `:sqliteWasmWorker` | JS/Wasm-specific packaging of the SQLite browser worker. |
| `iosApp` | Xcode host embedding the `:shared` framework; it is not a Gradle module. |

### Production source sets

| Source set | Kotlin files | Role | Primary pass |
| --- | ---: | --- | --- |
| `shared/commonMain` | 247 | Substantial shared product logic and nearly all Compose UI. | Parts 1–5 by area |
| `shared/androidMain` | 6 | Android graph startup, Room builder/module, preferences, copy reporting, and time-zone adapter. | Part 4 |
| `shared/iosMain` | 8 | iOS roots/graph startup plus Room, preferences, copy reporting, and time-zone adapters. | Part 4 |
| `shared/jvmMain` | 7 | Desktop roots/graph startup plus Room, preferences, copy reporting, and time-zone adapters. | Part 4 |
| `shared/webMain` | 7 | Browser roots/graph startup plus Room, preferences, copy reporting, and time-zone adapters shared by JS/Wasm. | Part 4 |
| `androidApp/main` | 2 | Android host entry points. | Part 4 |
| `desktopApp/main` | 1 | Desktop host entry point. | Part 4 |
| `webApp/webMain` | 1 | JS/Wasm web host entry point. | Part 4 |
| `sqliteWasmWorker/commonMain`, `jsMain`, `wasmJsMain` | 3 | Worker contract and per-browser-target worker construction. | Part 4 |

Configured `:shared` targets are Android, JVM, JS browser, WasmJS browser, iOS Arm64, and iOS
Simulator Arm64. `webMain` is the shared browser implementation source set; `:shared` has no
checked-in `jsMain` or `wasmJsMain` Kotlin sources. The worker module is the intentional exception
with target-specific JS and Wasm source files.

### Test source sets

| Source set | Kotlin files | Current role |
| --- | ---: | --- |
| `shared/commonTest` | 33 | Shared domain, policy, serialization, validation, state-holder, and utility tests. |
| `shared/jvmTest` | 94 | JVM/Room tests, ViewModel tests, Compose Desktop UI tests, navigation, and integration journeys. |
| `shared/androidHostTest` | 0 | Configured by the Android KMP target; no checked-in sources. |
| `shared/iosTest` | 0 | No checked-in platform-specific sources; shared tests are compiled/run for the simulator target. |
| `shared/webTest`, `jsTest`, `wasmJsTest` | 0 | No checked-in target-specific sources; shared tests feed JS/Wasm test tasks. |
| Application modules | 0 | No Kotlin test source directories in Android, Desktop, or Web hosts. |

Approximate repository totals: **282 production Kotlin files** and **127 test Kotlin files**.

## Production Code Inventory

### Architectural package map

| Area | Current locations and responsibility |
| --- | --- |
| App shell/navigation | Package root: `App`, `AppRoot`, `AppNavigation`, `AppNavigator`, routes, transitions, top-level destinations, and route mappings. |
| Curriculum/domain | `curriculum/`: assessment curriculum models, serialization/validation, and learning curriculum/content repository. |
| Assessment/practice | `assessment/`, `assessment_taking/`, `assessment_review/`, `topic_study/practice_builder/`, `focused_practice/`, and `focused_result/`. |
| Learning/topic presentation | `topic_study/topics/`, `topic_detail/`, `learning_unit/`, and `learning_lesson/`. |
| Progress/recommendations | `lesson_study/`, `learning_progress/`, `guided_learning/`, and `progress/`. |
| Mistakes/history/results | `mistake_review/`, `mixed_interview/`, and `assessment/history/`. |
| Saved questions | `saved_questions/` plus its local Room package. |
| Settings/appearance | `settings/` common preference/state/theme contract plus platform storage modules. |
| Persistence/data | `data/local/assessment`, `curriculum`, `lesson_study`, and `saved_questions`. |
| Shared UI/design system | `ui/`, including adaptive panes, bars/snackbars, content hierarchy, metrics, selection, theme/layout/motion/colors, and time formatting. |
| Platform implementations | `shared/{androidMain,iosMain,jvmMain,webMain}` and thin application hosts. |

### Compose inventory

- 60 `commonMain` Kotlin files contain `@Composable`; 67 production files do so when host/platform
  code is included.
- Approximately 30 top-level destination/screen entry points exist: 16 `*Destination` and 14
  `*Screen` declarations, plus three feature pages, `App`, `AppRoot`, and an assessment-launch
  dialog. Counts are planning aids because some destinations delegate directly and some files hold
  multiple composables.
- 15 Compose files live in the core `ui/` component/design-system area. Feature-local reusable
  components also live beside assessment review, progress, topic detail, and learning lesson UI.
- App shell/navigation chrome is in the package root and `ui/`; settings is in `settings/`.
- Learning UI is in `topic_study/topics`, `topic_detail`, `learning_unit`, and `learning_lesson`.
- Practice/assessment UI is in `assessment/start`, `assessment_taking`, `assessment_review`,
  `practice_builder`, and `focused_practice`.
- Results/progress UI is in `focused_result`, `mixed_interview`, `mistake_review`, `progress`, and
  `saved_questions`.

This is a surface inventory only. No recomposition, accessibility, or duplication assessment was
performed.

### ViewModels and state owners

There are **15 ViewModel classes**: `AppShellViewModel`, `AssessmentLaunchViewModel`,
`AssessmentTakingViewModel`, `MistakeReviewViewModel`, `InterviewStartViewModel`,
`MixedInterviewResultViewModel`, `ProgressViewModel`, `ProgressTopicViewModel`,
`SavedQuestionsViewModel`, `FocusedResultViewModel`, `LearningLessonViewModel`,
`LearningUnitViewModel`, `PracticeBuilderViewModel`, `TopicDetailViewModel`, and
`TopicBrowserViewModel`.

Six named state holders own shared/application or feature state: `StudyProgressStateHolder`,
`MistakeReviewStateHolder`, `InterviewHistoryStateHolder`, `ProgressStateHolder`,
`SavedQuestionStateHolder`, and `AppearanceStateHolder`. `AssessmentHistoryStore` and the
ViewModels are additional Flow/StateFlow-backed presentation owners. Parameterized ViewModels
cover attempt IDs, topic IDs, unit/lesson IDs, and practice targets.

### Data and persistence

| Category | Current implementation families |
| --- | --- |
| Repository contracts | Curriculum, learning content, assessment, lesson study, and saved questions. |
| Implementations | `LocalCurriculumRepository`, `BundledLearningContentRepository`, `LocalAssessmentRepository`, `LocalLessonStudyRepository`, and `LocalSavedQuestionRepository`. |
| Room database/access | `CurriculumDatabase`; curriculum, assessment-attempt, studied-lesson, and saved-question DAOs/entities. |
| Import/load | `CurriculumImporter`, `CurriculumDataInitializer`, bundled curriculum source/codec/validator, and learning content loader/source/codec/validator. |
| Stores/adapters | `AssessmentAttemptStore`, `AssessmentHistoryStore`, `ThemePreferenceStore`, and platform `AppPreferenceStorage`. |
| Mapping/migrations | Local curriculum/assessment mappers and `CurriculumMigrations`; exported Room schemas are under `shared/schemas`. |
| Bundled resources | Assessment and learning JSON in `shared/commonMain/composeResources/files/curriculum`. |

### DI inventory

- Koin version: **4.2.2**.
- Common modules: `LearningContentModule`, `AssessmentDataModule`, `CurriculumDataModule`,
  `LessonStudyDataModule`, `SavedQuestionDataModule`, `AppearanceModule`, and
  `TopicStudyPresentationModule`.
- Platform modules: one curriculum-data module and one appearance module for each of Android,
  iOS, JVM, and Web.
- Host graph starts: `startAndroidLocalDataGraph`, `startIosLocalDataGraph`,
  `startDesktopLocalDataGraph`, and `startWebLocalDataGraph`; paired initialize functions resolve
  and run `CurriculumDataInitializer`.
- Definitions: approximately **38 `single`**, **0 `factory`**, and **15 `viewModel`** definitions.
  Common code owns 30 singles and all ViewModels; each platform contributes two singles.
- Custom Koin scopes: **none found**.
- Direct resolution categories: 20 Compose `koinViewModel` call sites at app/destination
  boundaries; one `koinInject` in settings; low-level `GlobalContext`/`KoinPlatform` lookups in host
  startup/initialization; and a tolerant Koin lookup in `AppearanceTheme` for preview/test use.

These are inventory facts, not lifetime or service-locator findings.

### Platform inventory

| Platform | Host and implementation areas |
| --- | --- |
| Android | `KmpLearningApplication`, `MainActivity`, Android graph startup, Room database builder, preference storage, selection-copy report, and UTC offset. |
| iOS | Xcode `iosApp`, `IosAppRoot`, `MainViewController`, iOS graph startup, Room builder, NSUserDefaults-backed preferences, selection-copy report, and UTC offset. |
| JVM/Desktop | Desktop `main`, `DesktopAppRoot`, graph startup, Room builder, file-backed preferences, desktop selection-copy integration, and UTC offset. |
| Web JS/Wasm | Web `main`, `WebAppRoot`, graph startup, browser Room builder, local-storage preferences, selection-copy report, UTC offset, and JS/Wasm SQLite worker implementations. |

Explicit common-to-platform families are the Room `CurriculumDatabaseConstructor`, selection-copy
reporting, and local UTC offset. Database builders, preference bindings, app roots, graph startup,
and worker creation are also platform families without implying that their structural similarity is
defective duplication.

## Test Inventory

### Test architecture and families

| Production area | Existing test families | Apparent style |
| --- | --- | --- |
| App shell/navigation | Navigator, restoration, transitions, navigation bar, app root, settings and journey tests | JVM state plus Compose integration |
| Topic/learning | Topic browser/detail, learning unit/lesson, learning content, reader/navigation journeys | ViewModel, Compose UI, repository/content, integration |
| Assessment/practice | Engine, selector, session loader, launch, taking, review, builder, focused/mixed results | Shared unit, ViewModel, Compose UI, integration |
| Progress/recommendations | Study progress, learning progress, continue/recommendation policy, progress screens/topics | Shared unit, ViewModel, Compose UI, integration |
| Persistence | Room database/migration, importer, assessment store, and local repositories | JVM integration with Room plus repository behavior |
| Saved questions/mistakes | State/service/ViewModel/screen and capture/lifecycle journeys | Shared unit, JVM UI, integration |
| Settings/platform utilities | Preference/theme, JVM storage, timestamp/copy reporting | Shared unit, JVM platform/UI |
| Bundled content | Curriculum smoke/quality, learning curriculum, end-to-end loading | Resource/content integration |

Planning counts include 14 `*ViewModelTest`, two `*StateHolderTest`, five
`*RepositoryTest`, two `*StoreTest`, database and migration tests, 14 `*ScreenTest`, five
navigation-named tests, and 15 `*IntegrationTest` files. These counts do not establish adequacy.

### UI-test presence

| Test type | State | Evidence |
| --- | --- | --- |
| Android Compose UI tests | Absent | No Android instrumentation/device test sources; shared JVM UI tests are not Android device tests. |
| Android instrumentation tests | Absent | No `androidTest`/instrumented source directory with tests. |
| Desktop Compose UI tests | Present | 39 `jvmTest` files import Compose UI test APIs, covering screens, components, navigation, layout, and journeys. |
| Screenshot/golden tests | Absent | No screenshot framework/configuration or golden assets found. |
| Web UI tests | Absent | No target-specific browser UI test sources found. |
| End-to-end browser tests | Absent | No Playwright/Cypress/Selenium-style suite or CI step found. |
| iOS target-specific tests | Absent | No checked-in `iosTest` sources; common tests run through the simulator target. |
| JS/Wasm target-specific tests | Absent | No checked-in `jsTest`/`wasmJsTest` sources; common tests feed those tasks. |

Part 6 decides whether any absent test type is justified by behavior risk.

### Expensive or integration-oriented boundaries

Room and SQLite drivers, schema migrations, bundled Compose resources, platform preferences,
selection/copy integration, local time-zone behavior, Navigation 3/lifecycle restoration, Compose
semantics/layout, iOS framework hosting, and browser SQLite/storage are boundaries where integration
or platform tests may be more informative than isolated unit tests.

## Static Analysis / CI Inventory

### Configured quality tooling

- Android lint exists through the Android Gradle plugin; no custom lint configuration or baseline
  was found.
- Gradle/Kotlin compilation and KMP dependency/configuration checks run as part of normal tasks.
- Room/KSP provides compile-time query/schema generation; Room schema/migration tests exist.
- Python validators cover authored content and the learning-to-question snapshot.
- No Detekt, ktlint, dependency-analysis plugin, binary-compatibility validator, or explicit
  warnings-as-errors policy was found.
- No JaCoCo, Kover, or other Kotlin line/code-coverage system was found.
- `tools/learning_question_coverage.py` measures curriculum Question coverage only; it is not Kotlin
  code coverage.

### Compose compiler state

The Compose compiler plugin is applied where Compose is compiled, but no stability configuration,
metrics destination, reports destination, strong-skipping feature override, or other explicit
Compose compiler diagnostic configuration was found. Part 0 does not enable any of them.

### CI checks actually configured

The build-and-test workflow runs on pull requests/pushes to `main` and manual dispatch. It runs:

```text
python3 -m unittest discover -s tools -p 'test_*.py'
python3 tools/learning_question_coverage.py --check
./gradlew --no-daemon :androidApp:assembleDebug :desktopApp:assemble :webApp:assemble :shared:check
```

It uploads the JVM test report. It does not run `:androidApp:lintDebug`, Android instrumentation/UI
tests, browser end-to-end tests, line coverage, Detekt/ktlint, or iOS compilation/runtime checks on
the Ubuntu runner. The manual backlog-sync workflow validates and synchronizes backlog data; it is
not part of application CI.

## Audit Ownership Map

| Code area | Primary audit pass | Secondary concern |
| --- | --- | --- |
| App shell, navigation composables, shared UI primitives, theme/layout | Part 1 | Parts 4, 5, and 6 |
| Topic/learning, settings, practice, result, progress, and saved-question Compose UI | Part 1 | Parts 2 and 6 |
| ViewModels and Flow/state holders | Part 2 | Part 6 |
| Assessment sessions/history presentation ownership | Part 2 | Parts 3 and 6 |
| Curriculum and learning-content repositories/loaders | Part 3 | Parts 5 and 6 |
| Room database, DAOs, entities, migrations, importers, and local repositories | Part 3 | Parts 4 and 6 |
| Common Koin modules and definition lifetimes | Part 4 | Part 2 |
| Host composition roots and graph-start functions | Part 4 | Part 3 |
| Android/iOS/JVM/Web implementations and expect/actual families | Part 4 | Parts 3 and 5 |
| Domain models, utilities, error handling, visibility, dead code, and cross-feature APIs | Part 5 | Relevant functional pass |
| Tests and behavioral protection | Part 6 | Relevant functional pass |

Ownership is primary review responsibility, not exclusive access. A ViewModel may be reviewed in
Part 2 for state/coroutine behavior and Part 6 for coverage; a screen may be reviewed in Part 1 for
Compose behavior and Part 6 for UI coverage. Reuse the existing finding ID when the same issue is
seen again; do not create duplicate findings.

## Planned Audit Passes

### Part 0 — Baseline and inventory

Current pass. Establishes the commit, repository health, inventories, ownership, protocol, and
future chunks. It performs no production quality review.

### Part 1 — Compose UI and shared UI architecture

Scope: recomposition, state-read placement, stability, `remember`/`derivedStateOf`, effect keys,
lazy identity, duplicated stable UI concepts, shared primitives, accessibility, and UI-specific
bugs. Execute in these chunks:

| Chunk | Included surface | Approximate size | Size | Rationale |
| --- | --- | ---: | --- | --- |
| 1A — App shell, navigation, shared UI, settings | Package-root `App*` files, `ui/**`, `settings/**` | 21 Compose files / 41 Kotlin files | Large | One shell/design-system/theme/navigation context. |
| 1B — Topic discovery and lesson reading | `topic_study/{topics,topic_detail,learning_unit,learning_lesson}` | 13 Compose / 25 Kotlin | Large | One Learn browsing-to-reader journey with shared topic/lesson concepts. |
| 1C — Assessment launch, taking, review, and practice builder | `assessment/start`, `assessment_taking`, `assessment_review`, `practice_builder`, `focused_practice` | 9 Compose / 23 Kotlin | Medium | One assessment construction and taking flow. |
| 1D — Results, history, progress, mistakes, and saved questions | `focused_result`, `mixed_interview`, `mistake_review`, `progress`, `saved_questions` | 17 Compose / 43 Kotlin | Large | Post-attempt and learner-history surfaces share review/progress context. |

### Part 2 — Presentation, ViewModels and state

Scope: state ownership, Flow collection, events, cancellation/concurrency, stale results, runtime
parameters, impossible/duplicate state, navigation, and lifecycle behavior.

| Chunk | Included owners | Size | Rationale |
| --- | --- | --- | --- |
| 2A — Shell, appearance, and application state | `AppShellViewModel`, `AppearanceStateHolder`, app-level navigation interaction | Small | Application/root lifetime and startup state. |
| 2B — Learning and practice-builder state | Topic browser/detail, learning unit/lesson, practice builder ViewModels; study-progress state | Large | One discovery-to-study/practice pipeline with parameterized owners. |
| 2C — Assessment, mixed interview, results, and mistakes | Launch/taking/mistake/interview/result ViewModels; assessment/interview/mistake state holders | Large | Shared assessment lifecycle, attempt identity, and async work. |
| 2D — Progress and saved-question state | Progress ViewModels/state holder; saved-question ViewModel/state holder; relevant history feeds | Medium | Cross-screen learner-owned state and derived presentation. |

### Part 3 — Domain, repositories, persistence and data

Scope: repositories, importers/stores, database access, consistency, transactions, serialization,
error handling, query/work efficiency, duplication, and boundary violations.

| Chunk | Included subsystems | Size | Rationale |
| --- | --- | --- | --- |
| 3A — Curriculum and bundled learning content | `curriculum/**`, local curriculum importer/repository/initializer/mappers, serialization and validation | Large | Publisher-owned content enters through two related but distinct pipelines. |
| 3B — Assessment sessions, attempts, and history persistence | `assessment/**` domain/services/repository and `data/local/assessment/**` | Large | Attempt creation, scoring, persistence, review, and history form one consistency boundary. |
| 3C — Study progress, recommendations, saved questions | `lesson_study/**`, `guided_learning/**`, `learning_progress/**`, saved-question domain/repository, their local Room packages | Large | Learner-owned state and derived recommendations depend on related history/persistence. |
| 3D — Preference store contract | Common settings storage/store contract, excluding platform implementations | Small | Small non-Room persistence boundary; platform bindings remain Part 4. |

### Part 4 — DI, lifecycle ownership, KMP and platform code

Scope: Koin definitions and lifetimes, resolution boundaries, host composition, common/platform
boundaries, intentional versus problematic platform duplication, cleanup, and divergence.

| Chunk | Included surface | Size | Rationale |
| --- | --- | --- | --- |
| 4A — Common Koin graph and lifetimes | Seven common modules, 30 singles, 15 ViewModels, Compose resolution sites | Large | One graph/lifetime/ownership review. |
| 4B — Host composition roots | Android/iOS/Desktop/Web app roots, start/initialize functions, thin application modules | Medium | Host startup and graph ownership must be traced end to end. |
| 4C — Platform capability implementations | Platform Room/preferences, selection copy, UTC offset, and SQLite JS/Wasm worker families | Medium | Compare contracts and divergence without assuming platform similarity is duplication. |

### Part 5 — Cross-cutting production quality

One **Large** pass is currently appropriate after Parts 1–4 have narrowed the residual surface. It
will inspect utility duplication, dead code, visibility, model/API design friction, magic
constants, swallowed errors, repeated algorithms, forwarding abstractions, cross-feature coupling,
and justified non-Compose performance concerns. Split only if the residual file set proves too
large; do not repeat functional reviews already owned by earlier passes.

### Part 6 — Test architecture and final synthesis

Scope: behavioral coverage, brittle/redundant/implementation-coupled tests, critical untested
flows, residual findings, and audit completeness. Follow production boundaries:

| Chunk | Production-aligned test surface | Size | Rationale |
| --- | --- | --- | --- |
| 6A — Presentation/state behavior coverage | Tests for Part 2 owners and coroutine/state behavior | Large | Compare each owner’s risks with its direct and journey tests. |
| 6B — Repository/persistence/content coverage | Room, repositories, migrations, import/load, serialization, validation | Large | Integration boundaries and failure/consistency behavior belong together. |
| 6C — Compose UI/navigation/accessibility coverage | 39 JVM Compose-test files, navigation/layout/semantics, absent device/browser suites | Large | Decide test-type gaps from observed UI risks, not platform quotas. |
| 6D — Platform/integration coverage and final synthesis | Host/platform boundaries, cross-feature journeys, residual ledger review | Medium | Close coverage classifications and audit completeness without duplicating 6A–6C. |

## Future Pass Protocol

Each future audit chunk must:

1. Read this ledger.
2. Read only its assigned production area plus necessary dependencies.
3. Read tests for that area.
4. Understand behavior before criticizing implementation.
5. Record high-confidence findings; “No material issue found” is valid.
6. Independently verify potential bugs.
7. Make only permitted, local fixes.
8. Add or update tests for fixes where valuable.
9. Update finding statuses without deleting or renumbering findings.
10. Append or update exactly one audit-pass-log entry.
11. Run targeted validation, widening only when the change/risk warrants it.
12. Stop at the chunk boundary.

Full repository validation is not required after every chunk. Record the exact commit, files,
commands, results, and anything not validated.

## Fix vs Defer Policy

### Permitted local fixes

Later passes may directly fix confirmed bugs; incorrect lifecycle/state ownership; clear
race/cancellation problems; obvious unnecessary expensive Compose work; straightforward extraction
of a duplicated stable UI concept; dead/unreachable code; repeated logic with one clear canonical
abstraction; local API misuse; incorrect visibility; error-handling defects; and tests specifically
needed for a corrected defect.

### Normally deferred

Record rather than implement broad architecture rewrites; MVVM/state-management replacement;
repository-wide naming changes; DI framework migration or production Koin redesign; module
extraction/new Gradle architecture; large shared-component redesign; test-framework migration;
large public API redesign; speculative micro-optimization; and changes requiring product/design
decisions. A justified deferred item may become a separate future task.

## Current Audit Status

Part 0 is complete. There are no substantive findings and no “candidates noticed during inventory”
section because no unverified observation needed preservation. Production, test, Gradle, DI, CI,
and curriculum files remain unchanged. The next recommended chunk is **Part 1A — App shell,
navigation, shared UI, and settings**.
