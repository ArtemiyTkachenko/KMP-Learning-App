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
| `CQ-UI-001` | Shared UI / startup | Low | High | `AppRoot.kt`, `ui/ScreenStatus.kt` | The startup root duplicated the shared loading and error status concept. | `AppRoot` had private centred loading and retry layouts with their own literal spacing even though `ScreenLoading` and `ScreenError` already own the same semantics, layout, progress indication, and retry action for the rest of the product. The duplicate also omitted the shared status container's edge padding. | Render startup `Loading` and `Error` through the existing shared status primitives while keeping the startup state machine, strings, loading tag, and retry behavior unchanged. | Fixed |
| `CQ-BUG-001` | App shell lifecycle | Low | High | `AppRoot.kt`, four runtime roots/startup bridges, `MainActivity.kt`, `CurriculumDataInitializer.kt`, `AppRootTest.kt` | Host recreation reset the startup state and launched initialization again. | `AppRoot` held completion only in plain `remember`, although the app-scoped initializer already had the required lifetime: it survives host reconstruction and is rebuilt after a fresh process. The initializer now implements the explicit `AppStartupInitializer` host contract and exposes thread-safe in-process completion; a reconstructed root seeds `Ready` from that same owner, while a fresh owner seeds `Loading`. No completion value is saved durably. | Keep durable import completion on the app-scoped initializer and transient loading/error/retry presentation in `AppStartupStateHolder`; protect same-owner reconstruction and fresh-owner behavior at that boundary. | Fixed |
| `CQ-UI-002` | Shared UI API | Low | High | `ui/PerformanceCard.kt`; callers in `progress/**` and `mixed_interview/**` | `PerformanceCard` exposed independent navigation and percentage-visibility controls that permitted incoherent combinations. | All callers established two stable rules: every chevron card was also clickable, and every hidden percentage represented unavailable evidence. The old API could still render an inert chevron, require an unused percentage, or combine whole-card navigation with a nested action; caller-supplied `Modifier.clickable` also exposed no button role. `isSummary`, weak styling/label, subtitle, caption, and the optional embedded action each have independent real callers and remain coherent. | Encode navigation as nullable `onClick`, derive the chevron and `Role.Button` from it, reject a simultaneous nested action, and encode a hidden percentage as `null`. Keep the remaining presentation controls rather than introducing a mode hierarchy. | Fixed |
| `CQ-UI-003` | Topic discovery / state identity | Medium | High | `topic_study/topics/TopicBrowserScreen.kt`, `TopicBrowserScreenTest.kt` | Every search query shared one `LazyListState`, so a new result set could open at the previous query's scroll position. | Browse and search correctly had separate states, but the results state survived all non-blank query changes. After scrolling a long result set to item 25, replacing the query with another 30-item result set kept that index instead of showing its first match. | Reset only the results list to item zero when the query changes; retain the independent browse-list state. | Fixed |
| `CQ-UI-004` | Topic detail / accessibility | Low | High | `topic_detail/TopicStudyPage.kt`, `TopicSubtopicsPage.kt`, `TopicDetailScreenTest.kt` | Two custom clickable discovery rows exposed an action but no control role. | `LearningUnitRow` and `SubtopicRow` used foundation `Modifier.clickable` directly. Unlike Material button/card overloads, no semantic role was supplied, so assistive technology could discover activation without identifying the rows as buttons. | Set `Role.Button` on both conditional row click targets and assert the role in their existing interaction tests. | Fixed |
| `CQ-UI-005` | Topic discovery / accessibility | Low | High | `topic_study/topics/TopicBrowserScreen.kt`, `TopicBrowserScreenTest.kt` | The browser's visible screen title was not exposed as a semantic heading. | This is the only reviewed top-level surface whose title is content rather than `AppTopBar` chrome. Its `headlineMedium` styling conveyed hierarchy visually, but the node had no `heading` semantic for non-visual navigation. | Add heading semantics to the title and protect it with a Compose semantics assertion. | Fixed |
| `CQ-UI-006` | Lesson reader / duplication | Low | High | `learning_lesson/LearningLessonBlocks.kt`, `LearningLessonScreen.kt` | The lesson body and lesson outline maintained separate exhaustive mappings from `LearningDepth` to the same localized labels. | Both private extensions switched over every depth and returned the identical three resources. A new or renamed depth therefore required two presentation mappings in one reader to remain aligned. | Keep one package-internal depth-label mapping and use it for both rendered depth headings and outline entries. | Fixed |
| `CQ-UI-007` | Assessment taking / state identity | Medium | High | `assessment_taking/AssessmentTakingScreen.kt`, `AssessmentTakingScreenTest.kt` | Consecutive questions shared the same remembered `LazyListState`, so a new question could open at the previous question's deep scroll position. | `QuestionContent` remained at one composition position while only its state changed. A Compose interaction test scrolled Question A to answer 25, replaced it with Question B, and reproduced B's heading remaining off-screen until the content was keyed by question ID. | Give each question its own composition identity so its lazy list starts at the top, while selection and feedback remain owned by `AssessmentTakingUiState`. | Fixed |
| `CQ-UI-008` | Practice builder / accessibility | Low | High | `practice_builder/PracticeBuilderScreen.kt`, `PracticeBuilderScreenTest.kt` | The question-count and source controls exposed checkbox roles even though both sets are single-select. | Material `FilterChip` supplies `Role.Checkbox` by default. The builder used it for question count and source radio groups; a nested visual `RadioButton(onClick = null)` did not change the parent chip's role. | Override those two chip groups to `Role.RadioButton`, retain checkbox semantics for levels, and assert all three roles in the merged semantics tree. | Fixed |
| `CQ-UI-009` | Assessment taking / accessibility | Low | High | `assessment_taking/AssessmentTakingScreen.kt`, `AssessmentTakingScreenTest.kt` | The current question was visually styled as the content heading but was not exposed as one semantically. | The top app bar names the assessment while the `headlineSmall` question text is the primary heading of the changing content. Its semantics previously exposed only text. | Mark the question text as a heading and protect the hierarchy with a Compose semantics assertion. | Fixed |
| `CQ-UI-010` | Mixed interview / accessibility | Low | High | `mixed_interview/InterviewStartScreen.kt`, `InterviewStartScreenTest.kt` | The interview landing page's visible title was not exposed as a semantic heading. | This top-level surface intentionally has no `AppTopBar`; its `headlineMedium` title was therefore the page heading visually but only ordinary text in the semantics tree. | Add heading semantics to the title and assert them in the existing screen test. | Fixed |
| `CQ-UI-011` | Mistake review / composition work | Medium | High | `mistake_review/MistakeReviewScreen.kt` | The screen rebuilt its practiceable-Question list and Subtopic set on unrelated recompositions. | `MistakeReviewContent` traversed the full unresolved queue twice to derive the practice-all target. Per-Question saved-state changes and source-open failures recompose this content without changing `state.mistakes`, so a long queue repeated O(n) work and allocations for unrelated UI state. | Derive the count and Subtopic set in one pass and remember the result by the queue identity. Keep the state owner unchanged until Part 2C. | Fixed |
| `CQ-UI-012` | Assessment results / duplication | Low | High | `assessment_review/AssessmentResultLayout.kt`, `focused_result/FocusedResultScreen.kt`, `mixed_interview/MixedInterviewResultScreen.kt` | Focused and mixed results duplicated the same adaptive one-scroll/two-pane layout behavior. | Both screens had identical pane construction, padding, spacing, weights, compact ordering, and expanded behavior; only their summary and review sections differed. Maintaining two copies could let one result lose content order or pane behavior independently. | Share the stable adaptive assessment-result shell with summary/review `LazyListScope` content, while leaving state handling and feature-specific sections in each result screen. | Fixed |
| `CQ-UI-013` | Assessment results / effects | Medium | High | `focused_result/FocusedResultDestination.kt`, `mixed_interview/MixedInterviewResultDestination.kt` | Long-lived retake event collectors captured the initial navigation callback. | Each `LaunchedEffect` was keyed only to its ViewModel but invoked `onRetakeCreated` directly. If the callback changed while the same ViewModel remained composed, a later retake event navigated through the stale callback; assessment taking already uses the current-callback pattern for the same boundary. | Read the latest callback through `rememberUpdatedState` while keeping the collector keyed to the ViewModel. | Fixed |
| `CQ-STATE-001` | Saved questions / concurrency | Medium | High | `saved_questions/SavedQuestionStateHolder.kt`, `SavedQuestionStateHolderTest.kt` | Concurrent mutations for different Question IDs can leave the in-memory saved list older than the repository. | Per-ID pending state correctly keeps other rows interactive, so two writes may run together. Each coroutine then reads the whole repository and independently replaces `savedQuestions`; an older read can settle after a newer read and restore a stale list even though persistence is correct. Existing tests gate both writes together and do not force out-of-order read settlement. | In Part 2D, serialize mutation/readback settlement or version results so an older snapshot cannot replace a newer one; add a deterministic different-ID out-of-order regression test with the fix. | Deferred |
| `CQ-BUG-002` | App startup cancellation | Low | High | `AppRoot.kt`, `AppRootTest.kt` | Startup converted coroutine cancellation into the normal initialization-error state. | `runCatching` caught `CancellationException` from the host initializer and assigned `Error` before the cancelled effect returned. Host disposal normally made that state unobservable, but the startup wrapper still violated structured cancellation and could represent owner cancellation as an operational failure. | Rethrow cancellation before mapping ordinary initialization exceptions to `Error`; keep the state `Loading` when its owner cancels. | Fixed |
| `CQ-STATE-002` | App shell badge cancellation | Low | High | `AppShellViewModel.kt` | The unresolved-mistake fallback could convert cancellation from its suspend service boundary into a zero badge. | `runCatching(...).getOrDefault(0)` caught every throwable. The current supplied-history path is an in-memory derivation with no suspension, so cancellation is not presently raised inside it and impact is limited; the suspend contract nevertheless allowed future or explicit cancellation to become plausible data instead of terminating collection. | Preserve the intentional zero fallback for ordinary service exceptions and rethrow `CancellationException`. | Fixed |

## Audit Pass Log

| Pass | Area | Status | Commit reviewed | Files reviewed | Findings | Fixes | Validation | Notes |
| ---- | ---- | ------ | --------------- | -------------: | -------: | ----: | ---------- | ----- |
| Part 0 | Baseline and inventory | Complete | `75e7b30c9b08a5ea1cf275c1c7793bfa4ab4c96f` | 409 Kotlin paths inventoried plus build, CI, and architecture documentation | 0 | 0 | Four Gradle baseline checks; `git diff --check`; documentation-only final status | No production quality review performed. |
| Part 1A | App shell, navigation, shared UI, and settings | Complete | `c025e027b077eb5e9600ee26e9283180486b4635` | 41 assigned production Kotlin files, 12 direct dependencies/callers, and 20 relevant tests | 3 | 1 | Targeted `AppRootTest`; `:shared:jvmTest`; `:androidApp:assembleDebug`; `git diff --check` | High 0, Medium 0, Low 3; two deferred. No recomposition, expensive-composition, state-read-scope, stability, remember, `derivedStateOf`, effect-key, lazy-identity, side-effect-during-composition, or accessibility defect was verified. See review record below. |
| Part 1B | Topic discovery and lesson reading | Complete | `753ab8d8f1db93ea1b2f90b399e8bd28629f65b8` | 25 assigned production Kotlin files, 7 shared/model dependencies, and 16 relevant test files | 4 | 4 | Targeted `TopicBrowserScreenTest` and `TopicDetailScreenTest`; `:shared:jvmTest`; `:androidApp:assembleDebug`; `git diff --check` | High 0, Medium 1, Low 3. One lazy-state identity bug, two accessibility defects, and one duplicated stable concept were fixed. No qualifying recomposition/performance concern was found. See review record below. |
| Part 1C | Assessment launch, taking, review, and practice builder | Complete | `a37b5a7385572e2b727b6580a62b165a46e51a64` | 23 assigned production Kotlin files, 10 shared dependencies/callers, and 16 relevant test files | 3 | 3 | Targeted `AssessmentTakingScreenTest` and `PracticeBuilderScreenTest`; `:shared:jvmTest`; `:androidApp:assembleDebug`; `git diff --check` | High 0, Medium 1, Low 2. One question-local lazy identity bug and two accessibility defects were fixed. No qualifying recomposition/performance concern or new Part-2 deferral was found. See review record below. |
| Part 1D | Results, history, progress, mistakes, and saved questions | Complete | `e499932b8168e2d76661f17eeb39ab511514bc48` | 43 assigned production Kotlin files, 9 shared UI/review dependencies, and 32 relevant tests | 6 | 5 | Six targeted Compose suites; `:shared:jvmTest`; `:androidApp:assembleDebug`; `:shared:check`; `git diff --check` | High 0, Medium 3, Low 3. `CQ-UI-002` and four new UI findings were fixed; one saved-state race was deferred to Part 2D. See review record below. |
| Part 2A | Shell, appearance, and application state | Complete | `34bb8fae8c35e47b924be3ccc5caad8f7bd59b53` | 24 production/state files and 10 relevant test files | 3 | 3 | Targeted startup, appearance, navigator, and host/initializer suites; `:shared:jvmTest`; `:androidApp:assembleDebug`; `:shared:check`; `git diff --check` | High 0, Medium 0, Low 3. `CQ-BUG-001` and two cancellation findings were fixed. No Part 4 finding was opened. See review record below. |

### Part 1A Review Record

- **Assigned production boundary (41 files):** every package-root `App*.kt`, every Kotlin file
  under `shared/commonMain/.../ui/**`, and every Kotlin file under
  `shared/commonMain/.../settings/**`. The boundary includes `AppShellViewModel` only as the public
  interface consumed by the shell; its state/coroutine implementation remains owned by Part 2A.
- **Direct dependencies and callers inspected (12 files):** Android `MainActivity`,
  `CurriculumDataInitializer`, the four platform `UtcOffset` actuals, `LearningLessonDestination`,
  `LearningLessonScreen`, `ProgressComponents`, `ProgressScreen`, `MixedInterviewResultScreen`, and
  `InterviewStartScreen`. These were read only far enough to establish startup recreation, shell
  chrome callbacks, timestamp cost, and `PerformanceCard` API use; their owning future chunks were
  not audited.
- **Relevant tests reviewed (20 files):** `AppearancePreferenceTest`, `TopicVisualIdentityTest`,
  `CopyReportingTextToolbarTest`, `LocalTimestampTest`, `AppNavigationBarTest`, `AppNavigationTest`,
  `AppNavigationTransitionsTest`, `AppNavigatorRestorationTest`, `AppNavigatorTest`, `AppRootTest`,
  `AppearanceThemeTest`, `SettingsNavigationIntegrationTest`, `SharedHostStartupTest`,
  `SettingsScreenTest`, `AdaptiveLayoutTest`, `ContentHierarchyTest`, `LargeFontScaleTest`,
  `MetricComponentsTest`, `TopicVisualIdentityCurriculumTest`, and `SelectionCopyDesktopTest`.
- **Fixed:** `CQ-UI-001`; `AppRoot` now uses `ScreenLoading` and `ScreenError`. `AppRootTest` now
  interacts with the visible Retry label rather than a production test tag. No shared-component API
  was widened for test-only access.
- **Deferred:** `CQ-BUG-001` to Part 2A / 4B and `CQ-UI-002` to Part 1D. No architecture, Gradle,
  Koin, persistence, schema, curriculum, or dependency change was made.
- **Validation:** `./gradlew :shared:jvmTest --tests org.artkachenko.kmp_learning_app.AppRootTest`
  passed; `./gradlew :shared:jvmTest` passed; `./gradlew :androidApp:assembleDebug` passed. The
  existing Kotlin expect/actual Beta warning appeared during compilation and is unrelated to this
  change. Android lint was not run because no Android-specific source changed; JS/Wasm and iOS were
  not run because the fix only substitutes common primitives already compiled for every target by
  the Part 0 `:shared:check` baseline. No screenshot/golden infrastructure exists, so the changed
  startup status was interaction-verified by the JVM Compose test rather than pixel-verified.

### Part 1B Review Record

- **Assigned production boundary (25 files):** every Kotlin file under
  `topic_study/topics`, `topic_study/topic_detail`, `topic_study/learning_unit`, and
  `topic_study/learning_lesson`. The four ViewModels were read only at their public state/action
  interfaces and far enough to confirm UI invariants; their coroutine and ownership behavior remains
  owned by Part 2B.
- **Shared/model dependencies inspected (7 files):** `ScreenStatus.kt`, `ContentHierarchy.kt`,
  `MetricComponents.kt`, `AdaptivePanes.kt`, `LearningContext.kt`, and the study-progress UI/domain
  models. Part 1A's `AppTopBar`, adaptive pane, layout, and theme contracts were used as established
  dependencies rather than re-audited.
- **Relevant tests inspected (16 files):** the four feature `*ViewModelTest` files; the four direct
  screen/content suites (`TopicBrowserScreenTest`, `TopicDetailScreenTest`,
  `TopicDetailLearningContentTest`, `LearningUnitScreenTest`); `LearningLessonScreenTest`;
  `LessonScrollStateReducerTest`; `TopicPracticeRecommendationTest`; and the five focused Learn
  journey/integration suites (`FocusedLearningJourneyIntegrationTest`,
  `LearningNavigationIntegrationTest`, `LearningReaderJourneyIntegrationTest`,
  `LearningUnitPracticeIntegrationTest`, and `TopicDiscoveryIntegrationTest`). These were used to
  establish intended UI behavior and protect fixes, not as a Part 6 coverage audit.
- **Fixed:** `CQ-UI-003` resets search results to their first item for each changed query without
  disturbing the catalogue's independent scroll position. `CQ-UI-004` gives the two foundation
  clickable rows `Role.Button`. `CQ-UI-005` exposes the browser title as a heading. `CQ-UI-006`
  makes the Lesson depth-label resource mapping the one shared concept used by body and outline.
- **Recomposition/performance disposition:** no finding. State is lifecycle-collected at destination
  boundaries; meaningful collection transforms live in ViewModels; lazy rows use stable entity keys;
  Lesson scroll sampling keeps per-pixel reads out of the body composition; outline derivation is
  remembered by sections and localized labels; and the small per-Unit progress joins did not justify
  speculative caching. No `derivedStateOf`, stability/skipping, state-read-scope, or expensive-
  composition change was made.
- **Effects and identity:** the reader's scroll reducer and `ScrollState` are keyed to Lesson
  identity, its visibility callback uses `rememberUpdatedState`, and disposal restores shell chrome.
  Source-open failure state was not changed: adjacent-Lesson navigation replaces the Navigation 3
  entry, disposing the destination that owns the state. No navigation or other side effect was found
  executing directly during composition.
- **Reuse decisions:** the Part 1A status, hierarchy, metric, adaptive-pane, and top-bar primitives
  already represent the concepts used here and were reused. Topic- and Unit-level study summaries
  remain separate because their state handling and list placement differ; Topic, Subtopic, Unit, and
  Lesson rows retain distinct containers/content contracts rather than gaining screen-mode flags.
- **Validation:** the targeted `TopicBrowserScreenTest` and `TopicDetailScreenTest` run passed;
  `./gradlew :shared:jvmTest` and `./gradlew :androidApp:assembleDebug` passed. The documented Kotlin
  expect/actual Beta warning appeared and was unchanged. Android lint was not run because no
  Android-specific source changed; JS/Wasm/iOS and UI device tests were not run because these local
  common-Compose changes use existing multiplatform APIs and the repository has no device UI suite.
  No visual verification was required because the fixes alter scroll identity or semantics only;
  `CQ-UI-006` is a rendering-preserving mapping consolidation.

### Part 1C Review Record

- **Assigned production boundary (23 files):** `assessment/start`:
  `AssessmentLaunchCoordinator.kt`, `AssessmentLaunchDialog.kt`, `AssessmentLaunchViewModel.kt`,
  `StartAssessment.kt`; `assessment_taking`: `AssessmentQuestionUiModel.kt`,
  `AssessmentTakingDestination.kt`, `AssessmentTakingScreen.kt`, `AssessmentTakingUiState.kt`,
  `AssessmentTakingViewModel.kt`; `assessment_review`: `AssessmentReviewComponents.kt`,
  `AssessmentReviewLoader.kt`, `AssessmentReviewModels.kt`, `QuestionContentComponents.kt`,
  `ReviewSaveAction.kt`; `topic_study/practice_builder`: `PracticeBuilderDefaults.kt`,
  `PracticeBuilderDestination.kt`, `PracticeBuilderRouteMapping.kt`, `PracticeBuilderScreen.kt`,
  `PracticeBuilderTarget.kt`, `PracticeBuilderUiState.kt`, `PracticeBuilderViewModel.kt`,
  `PracticeTargetResolver.kt`; and `topic_study/focused_practice/FocusedPracticeDestination.kt`.
- **Dependencies/callers inspected (10 files):** `App.kt`, `ui/AppTopBar.kt`,
  `ui/ScreenStatus.kt`, `ui/ContentHierarchy.kt`, `ui/AdaptivePanes.kt`,
  `assessment/AssessmentConfig.kt`, `assessment/session/AssessmentEngine.kt`,
  `assessment/session/AssessmentSession.kt`, `focused_result/FocusedResultScreen.kt`, and
  `mixed_interview/MixedInterviewResultScreen.kt`. Result callers were read only far enough to
  establish review-component reuse; Part 1D was not audited.
- **State interfaces inspected:** `AssessmentLaunchViewModel`, `AssessmentTakingViewModel`, and
  `PracticeBuilderViewModel` were read through their public state/action transitions and immediate
  guards. Coroutine cancellation, event ownership, persistence sequencing, and stale-result analysis
  remain owned by Parts 2B/2C and 3; no new verified concern required deferral from this pass.
- **Relevant tests inspected (16 files):** `AssessmentLaunchDialogTest`,
  `AssessmentLaunchViewModelTest`, `AssessmentTakingScreenTest`, `AssessmentTakingViewModelTest`,
  `AssessmentReviewLoaderTest`, `AssessmentReviewComponentsTest`, `AssessmentSessionLoaderTest`,
  `PracticeBuilderRouteMappingTest`, `PracticeBuilderScreenTest`, `PracticeBuilderViewModelTest`,
  `PracticeTargetResolverTest`, `FocusedLearningJourneyIntegrationTest`,
  `LearningUnitPracticeIntegrationTest`, `TargetedPracticeLifecycleIntegrationTest`,
  `MixedInterviewJourneyIntegrationTest`, and `GuidedLearningPracticePresetIntegrationTest`.
  They established intended behavior and protected fixes; Part 6's adequacy audit was not performed.
- **Fixed:** `CQ-UI-007` keys taking content to the semantic Question ID so a changed Question starts
  at the top; `CQ-UI-008` exposes question-count and source choices as radio buttons while levels
  remain checkboxes; `CQ-UI-009` exposes the current Question as the content heading. The two screen
  suites gained interaction and semantics regressions.
- **Recomposition/performance disposition:** no finding. Expected selection, submission, feedback,
  progress, and animation state changes recompose their owning content; answer collections are
  rendered lazily with stable answer-ID keys; review transforms are small or loader-owned; and no
  expensive work, overly broad state read, `derivedStateOf` opportunity, or measurable static
  performance concern justified a change.
- **Effects, transitions, and identity:** coordinator event collectors are keyed to their ViewModels
  and use current callbacks; completion navigation is keyed to the successful attempt ID. Submit,
  next, finish, launch, and retry paths have synchronous state guards before asynchronous work. The
  verified identity defect was confined to the taking list and fixed by Question ID; selection,
  correctness, review, and lazy-row identity already use answer/Question IDs rather than positions.
- **Reuse and adaptive-layout decisions:** taking and review rows remain separate because one is an
  interactive radio/checkbox control and the other is a read-only outcome explanation; their merger
  would require mode flags. Review already shares authored-content primitives across result and saved
  surfaces. Focused practice cleanly delegates to `AssessmentTakingDestination`. Builder state and
  callbacks stay outside compact/expanded branches, while shared list sections preserve content
  order; no extraction or adaptive-state change was needed. Existing `ScreenLoading`, `ScreenError`,
  `ScreenMessage`, `ScreenStatus`, `AppTopBar`, `SectionHeading`, and `AppTwoPaneRow` are reused.
- **Accessibility disposition:** answer rows already expose one coherent radio/checkbox target with
  non-clickable visual controls, feedback states correctness in text, and progress exposes range
  semantics. `CQ-UI-008` and `CQ-UI-009` address the two verified gaps. Launch dialogs, standard
  buttons, disabled states, review outcome labels, and save state expose sufficient Material or
  explicit semantics; no additional annotation was added.
- **Validation:** the targeted `AssessmentTakingScreenTest` and `PracticeBuilderScreenTest` run
  passed; `./gradlew :shared:jvmTest` and `./gradlew :androidApp:assembleDebug` passed. The existing
  Kotlin expect/actual Beta warning was unchanged. Android lint was not run because no Android source
  changed; JS/Wasm/iOS and device UI tests were not run because the changes use existing common
  Compose APIs and the repository has no device/browser UI suite. No visual verification was needed
  because all changes affect state identity or semantics without changing pixels.

### Part 1D Review Record

- **Assigned production boundary (43 files):** `focused_result`: `FocusedResultDestination.kt`,
  `FocusedResultScreen.kt`, `FocusedResultUiState.kt`, `FocusedResultViewModel.kt`;
  `mixed_interview`: `InterviewHistoryStateHolder.kt`, `InterviewStartDestination.kt`,
  `InterviewStartScreen.kt`, `InterviewStartViewModel.kt`, `MixedInterviewDefaults.kt`,
  `MixedInterviewDestination.kt`, `MixedInterviewResultDestination.kt`,
  `MixedInterviewResultScreen.kt`, `MixedInterviewResultUiState.kt`,
  `MixedInterviewResultViewModel.kt`; `mistake_review`: `MistakeReviewDestination.kt`,
  `MistakeReviewModels.kt`, `MistakeReviewScreen.kt`, `MistakeReviewService.kt`,
  `MistakeReviewStateHolder.kt`, `MistakeReviewUiState.kt`, `MistakeReviewViewModel.kt`;
  `progress`: `ProgressComponents.kt`, `ProgressDestination.kt`,
  `ProgressPracticeShortcuts.kt`, `ProgressScreen.kt`, `ProgressStateHolder.kt`,
  `ProgressTopicDestination.kt`, `ProgressTopicScreen.kt`, `ProgressTopicUiState.kt`,
  `ProgressTopicViewModel.kt`, `ProgressUiState.kt`, `ProgressViewModel.kt`,
  `RecentTrendChart.kt`; `saved_questions`: `SavedQuestion.kt`,
  `SavedQuestionContentModels.kt`, `SavedQuestionContentResolver.kt`,
  `SavedQuestionStateHolder.kt`, `SavedQuestionsDestination.kt`, `SavedQuestionsScreen.kt`,
  `SavedQuestionsState.kt`, `SavedQuestionsUiState.kt`, `SavedQuestionsViewModel.kt`, and
  `repository/SavedQuestionRepository.kt`. Nineteen of these files currently contain Compose.
- **Shared review/UI dependencies inspected (9 existing files):** `ui/PerformanceCard.kt`,
  `ui/AdaptivePanes.kt`, `ui/ScreenStatus.kt`, `ui/ContentHierarchy.kt`, and the five
  `assessment_review` files `AssessmentReviewComponents.kt`, `AssessmentReviewLoader.kt`,
  `AssessmentReviewModels.kt`, `QuestionContentComponents.kt`, and `ReviewSaveAction.kt`.
  `AssessmentResultLayout.kt` was added as the one new shared result primitive.
- **State/ViewModel interfaces inspected:** all 11 ViewModels/state holders in the boundary were
  read through the state and action contracts consumed by UI. Repeat concurrency and event
  ownership were inspected only far enough to establish UI invariants; Part 2C retains their full
  state/coroutine audit. `SavedQuestionStateHolder` was traced far enough to verify
  `CQ-STATE-001`, which Part 2D owns.
- **Relevant tests inspected (32 files):** the direct loader, service, holder, ViewModel, screen,
  destination, resolver, shortcut, chart, repository, and result integration suites under
  `assessment_review`, `focused_result`, `mixed_interview`, `mistake_review`, `progress`, and
  `saved_questions`; `FocusedLearningJourneyIntegrationTest`,
  `MixedInterviewJourneyIntegrationTest`, `ProgressLearningJourneyIntegrationTest`, and
  `ui/AdaptiveLayoutTest`. They established intended behavior and protected the fixes; full test
  architecture adequacy remains Part 6.
- **Fixed:** `CQ-UI-002` now binds navigation, chevron, click handling, and `Role.Button` through
  `onClick`, represents an unavailable percentage as `null`, and rejects a nested action on a
  navigable card. Every caller was migrated: interview latest/best, progress Topic/history,
  mixed-result Topic performance, weak areas, and Topic/Subtopic detail summaries. `CQ-UI-010`
  adds the interview heading; `CQ-UI-011` remembers the mistake practice target by queue identity;
  `CQ-UI-012` shares the result pane shell; `CQ-UI-013` keeps retake callbacks current.
- **Deferred:** `CQ-STATE-001` to Part 2D. No Part 2C issue was verified: both repeat actions disable
  while creating and their ViewModels synchronously reject a duplicate launch; attempt identity is
  constructor-owned and error states remain retryable. No separate Part 6 test finding was opened;
  the concurrency regression test belongs with the Part 2D fix.
- **PerformanceCard caller disposition:** inert mixed-result Topic cards show the percentage without
  a chevron; interview-history, progress-Topic, and progress-history cards navigate as whole button
  surfaces and show the chevron; weak-area cards remain reading surfaces with a separate labelled
  action; Topic detail uses summary styling and suppresses percentages only when evidence is below
  policy. Weak badges are omitted only where the section heading already states weakness. The
  remaining independent options therefore encode real caller needs and no sealed mode hierarchy or
  generic configuration object was introduced.
- **Lists and identity:** mistake, weak-area, Topic, history, Subtopic, and saved lists use stable
  domain keys. Saved removal pending state is a `Set<String>` checked per row; the two-row UI test
  confirms one pending removal does not disable another, and `animateItem` follows `questionId`.
  Result transcripts intentionally remain unkeyed: their order and membership do not change while
  the destination lives, save state is external per Question ID, and expansion is itself saveable by
  Question ID. Adding a key would not fix or protect a current behavior.
- **Adaptive layout:** focused and mixed results now call the same `AssessmentResultLayout`; compact
  order remains outcome then transcript, while expanded panes contain the same section lambdas and
  retain independent scroll state. Progress and Mistake Review already declare shared section
  lambdas once for both arrangements. No action, transcript order, or state owner differs by size.
- **Progress and weak-area semantics:** joins, grouping, sorting, history mapping, and weakness
  classification are upstream. UI work is limited to small formatting and a maximum-five-point
  chart. The dashboard continues to distinguish an observed empty weak-area set from no resolvable
  observations by checking whether Topic performance exists; that logic was preserved.
- **Review, saving, missing content, and sources:** result and mistake surfaces use the shared
  attempt-review card; Saved Questions deliberately uses authored content without learner selection
  or correctness claims. Missing result Questions have no save/practice action, while a missing
  saved identity remains removable. Source failure is associated by URL and a successful click
  clears it. Save/unsave actions use exact Question IDs and per-ID pending state.
- **Recomposition/performance disposition:** `CQ-UI-011` is the one verified Part 1D finding: a
  saved-state or source-failure change caused a full mistake-queue target derivation despite an
  unchanged queue. No other unnecessary recomposition, expensive composition work, broad state-read
  scope, stability/skipping issue, or justified `derivedStateOf` use was found. The recent chart's
  collection work is bounded to five values; result notices scan bounded assessment transcripts;
  progress derivation is upstream.
- **Accessibility:** navigable performance cards now publish `Role.Button`, and the interview page
  title is a heading. Score and correctness information remains textual rather than color-only;
  answer tags cover selected-correct, selected-wrong, missed-correct, and neutral combinations;
  saved removal and practice shortcuts are standard labelled buttons. Tests assert the new role and
  heading semantics.
- **Reuse decisions:** `AssessmentResultLayout` is the stable summary/transcript shell shared by
  focused and mixed results. Existing score, review, missing-content, retention, status, section,
  and adaptive-pane primitives remain reused. Feature-specific outcome sections stay separate
  because repeat state and mixed Topic performance differ; mistake review and saved authored-content
  cards remain separate from attempt review because their truth and actions differ.
- **Validation:** the six targeted Compose suites passed; `./gradlew :shared:jvmTest`,
  `./gradlew :androidApp:assembleDebug`, and `./gradlew :shared:check` passed. The broad check ran
  Android host tests, JVM tests, JS browser tests, Wasm browser tests, and iOS simulator tests. The
  existing Kotlin expect/actual Beta warning appeared for JVM, Android, JS, Wasm, and iOS and was
  unchanged. Android lint, device UI tests, and browser end-to-end tests were not run; no Android
  source changed, and those UI infrastructures do not exist here. Rendering was not pixel-checked
  because the UI changes preserve pixels except for semantics and bind already-present chevrons to
  their existing click actions.

## Part 1 Compose/UI Synthesis

- **Findings:** 15 unique findings across Parts 1A-1D: 13 fixed and two deferred. Severity totals
  are High 0, Medium 5, Low 10; no observation-only or needs-measurement finding remains.
- **Bugs fixed:** three user-visible state/effect defects: search-result scroll identity,
  assessment-Question scroll identity, and stale result-navigation callbacks.
- **Accessibility:** six findings affected accessibility: two missing button roles, two missing
  headings, incorrect single-select roles, and navigable performance cards without a button role.
  All were fixed with JVM Compose semantics protection.
- **Duplication/reuse:** three duplicated stable concepts were consolidated: startup status,
  Lesson depth labels, and the adaptive assessment-result shell. Existing assessment review,
  authored Question content, status, hierarchy, metric, and adaptive-pane primitives were reused.
- **Recomposition/performance:** one actual repeated-work finding was fixed by remembering the
  mistake practice target by queue identity. No other unnecessary recomposition, expensive
  composition, state-read-scope, stability/skipping, or measurement-dependent performance issue
  was verified.
- **Deferred ownership:** `CQ-BUG-001` remains for Part 2A/4B and `CQ-STATE-001` for Part 2D. No
  Part 2C concern and no standalone Part 6 test concern was opened.
- **Deliberately not created:** no generic ResultScreen, performance-card mode hierarchy, generic
  Question card, Topic/Unit row merger, taking/review row merger, or compact/expanded state owner.
  Similar UI whose state, meaning, or actions differ remains separate.

## Part 2A Review Record

- **Production/state boundary (24 files):** `AppRoot.kt`, `App.kt`, `AppShellViewModel.kt`,
  `AppNavigator.kt`, `AppNavigation.kt`, the four platform app roots/startup bridges, Android
  `MainActivity`, `CurriculumDataInitializer.kt`, `CurriculumDataModule.kt`,
  `AssessmentHistoryStore.kt`, `MistakeReviewService.kt`, and the six common appearance/theme
  files. Platform preference implementations and `TopicStudyPresentationModule` were read only far
  enough to establish contracts and lifetimes; their implementation/graph audit remains Part 4.
- **Tests inspected (10 files):** `AppRootTest`, `SharedHostStartupTest`,
  `CurriculumLocalDataPathTest`, `DesktopLocalDataPathTest`, `AppNavigatorTest`,
  `AppNavigatorRestorationTest`, `AppNavigationTest`, `AppearancePreferenceTest`,
  `AppearanceThemeTest`, and `SettingsNavigationIntegrationTest`. No direct
  `AppShellViewModelTest` exists; Part 6A retains the broad coverage assessment.
- **Owner/lifetime conclusion:** shell badge data remains in `AppShellViewModel`; saveable selected
  area and per-area stacks remain in `AppNavigator`; `AppearanceStateHolder` remains an app-scoped
  Koin single; successful startup completion now belongs to the app-scoped initializer, while a
  remembered `AppStartupStateHolder` owns only transient loading/error/retry presentation.
- **Startup/recreation disposition:** `CQ-BUG-001` is fixed. Every host supplies the same small
  `AppStartupInitializer` contract. Reconstructing UI with the same application owner seeds `Ready`
  and does not invoke import again; a fresh graph creates an incomplete initializer and therefore
  runs initialization. No saveable or persistent completion flag was introduced.
- **Initializer synchronization:** the existing double-checked `Mutex` still coalesces concurrent
  calls. Completion is now backed by `MutableStateFlow.value`, giving unlocked checks and host reads
  a thread-safe in-process value. It is published only after `Imported`; rejection, exceptions, and
  cancellation leave it false, so later calls can retry. There is no suspension between successful
  import return and completion publication.
- **Cancellation and error handling:** `CQ-BUG-002` makes startup rethrow cancellation rather than
  showing `Error`; ordinary `Exception`s remain retryable. `CQ-STATE-002` makes the shell badge
  rethrow cancellation while retaining zero as the fallback for an ordinary non-critical count
  failure. Fatal `Throwable`s are no longer converted into normal startup or badge state. No other
  broad catch occurs in the Part 2A boundary.
- **Shell Flow conclusion:** unresolved count is derived from the one app-scoped history cache and
  has no competing mutable copy. The service receives already-loaded attempts, so it performs an
  in-memory latest-occurrence derivation and no repository read. `SharingStarted.Eagerly` is kept:
  the shell always needs the badge once ready, upstream changes only on history invalidation, and
  eager derivation keeps the value current across lifecycle subscription gaps. Loading, failed
  history, and ordinary derivation failure intentionally project to zero solely as “hide badge”; no
  domain data is written or declared resolved.
- **Appearance conclusion:** application lifetime is correct because the preference applies above
  navigation and survives Settings removal. `ThemePreference` is the sole mutable preference;
  effective dark/light remains derived from it and the system value. Constructor read and writes
  are synchronous by the non-throwing `AppPreferenceStorage` contract. Current implementations are
  small platform key/value operations and suppress unavailable-storage failures, so the optimistic
  publish-then-write order does not contradict the contract. No duplicate theme source or invalid
  System/Light/Dark combination was found.
- **Navigation conclusion:** Compose saveable state is the appropriate owner for the selected area
  and four independent back stacks. Existing restoration tests cover area/detail reconstruction,
  durable route identity, and per-area behavior. Shell data, appearance, startup, and navigation
  remain separate owners; no duplicated selected-area state or impossible area/active-stack state
  was found.
- **Concurrency/stale-result conclusion:** retry cannot overlap a prior startup attempt because the
  action exists only in settled `Error`; one `LaunchedEffect` owns the active attempt. Concurrent
  initializer callers remain mutex-coalesced. Appearance calls originate from serialized UI event
  dispatch and repeated equal choices are no-ops. No stale asynchronous result or non-atomic
  read-copy-write defect was verified in this slice.
- **Part 4 handoff:** no Part 4B or 4C finding was opened. Part 4B still owns the full host/Koin graph
  audit, and Part 4C still owns platform preference durability, file atomicity, and latency; this
  pass established only the lifetime and non-throwing contracts required by application state.
- **Fixes and tests:** fixed `CQ-BUG-001`, `CQ-BUG-002`, and `CQ-STATE-002`. `AppRootTest` now covers
  same-owner reconstruction, a fresh initializer, failure followed by successful retry, and
  cancellation propagation. Existing initializer concurrency and failure tests and navigation,
  appearance, and host integration suites passed unchanged apart from startup-interface call sites.
- **Validation:** targeted startup, appearance, navigator restoration, and host/initializer suites
  passed; `./gradlew :shared:jvmTest` and `./gradlew :androidApp:assembleDebug` passed; and
  `./gradlew :shared:check` passed across Android host, JVM, JS, Wasm, and iOS simulator targets.
  `git diff --check` passed. The existing Kotlin expect/actual Beta warning was unchanged. Android
  lint and device/browser end-to-end UI tests were not run; no platform implementation changed and
  those UI test infrastructures do not exist here.

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

```text
Part 1A — Complete

High: 0
Medium: 0
Low: 3
Observations: 0

Fixed: 1
Deferred: 2
Needs measurement: 0
Accepted as-is: 0
Not a defect: 0

Part 1B — Complete

High: 0
Medium: 1
Low: 3
Observations: 0

Fixed: 4
Deferred: 0
Needs measurement: 0
Accepted as-is: 0
Not a defect: 0

Part 1C — Complete

High: 0
Medium: 1
Low: 2
Observations: 0

Fixed: 3
Deferred: 0
Needs measurement: 0
Accepted as-is: 0
Not a defect: 0

Part 1D — Complete

High: 0
Medium: 3
Low: 3
Observations: 0

Fixed: 5
Deferred: 1
Needs measurement: 0
Accepted as-is: 0
Not a defect: 0

Part 1 — Complete
Part 2A — Complete

High: 0
Medium: 0
Low: 3
Observations: 0

Fixed: 3
Deferred: 0
Needs measurement: 0
Accepted as-is: 0
Not a defect: 0

Part 2B — Next
```

Part 2A is complete. The exact next chunk is **Part 2B — Learning and practice-builder state**.
Do not begin it automatically.
