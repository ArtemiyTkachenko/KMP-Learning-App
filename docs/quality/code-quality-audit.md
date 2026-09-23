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
| `CQ-STATE-001` | Saved questions / concurrency | Medium | High | `saved_questions/SavedQuestionStateHolder.kt`, `SavedQuestionStateHolderTest.kt` | Concurrent mutations for different Question IDs can leave the in-memory saved list older than the repository. | Per-ID pending state correctly keeps other rows interactive, so two writes may run together. Each coroutine then reads the whole repository and independently replaces `savedQuestions`; an older read can settle after a newer read and restore a stale list even though persistence is correct. Existing tests gate both writes together and do not force out-of-order read settlement. | Serialize mutation read-back against every other read of the table so an older snapshot cannot replace a newer one, and protect it with deterministic out-of-order regression tests. Part 2D resolution: the read-back now takes the same `reading` Mutex the refresh holds, matching the pattern Part 2B established for `StudyProgressStateHolder`; the writes stay outside the lock, so one Question's mutation still never waits on another's. Two deterministic tests — an older refresh settling after a write, and a save's read-back sampled before a different Question's removal — fail on the previous implementation and pass on this one. | Fixed |
| `CQ-BUG-002` | App startup cancellation | Low | High | `AppRoot.kt`, `AppRootTest.kt` | Startup converted coroutine cancellation into the normal initialization-error state. | `runCatching` caught `CancellationException` from the host initializer and assigned `Error` before the cancelled effect returned. Host disposal normally made that state unobservable, but the startup wrapper still violated structured cancellation and could represent owner cancellation as an operational failure. | Rethrow cancellation before mapping ordinary initialization exceptions to `Error`; keep the state `Loading` when its owner cancels. | Fixed |
| `CQ-STATE-002` | App shell badge cancellation | Low | High | `AppShellViewModel.kt` | The unresolved-mistake fallback could convert cancellation from its suspend service boundary into a zero badge. | `runCatching(...).getOrDefault(0)` caught every throwable. The current supplied-history path is an in-memory derivation with no suspension, so cancellation is not presently raised inside it and impact is limited; the suspend contract nevertheless allowed future or explicit cancellation to become plausible data instead of terminating collection. | Preserve the intentional zero fallback for ordinary service exceptions and rethrow `CancellationException`. | Fixed |
| `CQ-STATE-003` | Topic browser cancellation | Low | High | `TopicBrowserViewModel.kt`, `TopicBrowserViewModelTest.kt` | Catalogue and optional-enrichment fallbacks converted coroutine cancellation into ordinary screen state. | `runCatching` around `readCatalog`, learning-content enrichment, and `derivedOrNull` caught `CancellationException`. A cancelled catalogue load could publish `Error`; cancelled history or learning-content work could continue and publish absent enrichment. ViewModel clearing normally makes that state unobservable, but the owner still violated structured cancellation and could finish a partial snapshot after cancellation. | Rethrow cancellation at every suspend fallback boundary while retaining `Error` or `null` for ordinary exceptions; protect primary-load and atomic history-enrichment behavior with owner-level tests. | Fixed |
| `CQ-STATE-004` | Topic detail cancellation | Low | High | `TopicDetailViewModel.kt`, `TopicDetailViewModelTest.kt` | Primary, learning-content, and history fallbacks converted coroutine cancellation into ordinary Topic state. | Broad `runCatching` blocks could turn cancellation into screen `Error`, unavailable learning content, or absent analytics and then render it. Generation checks protected retry ordering but did not distinguish cancellation from operational failure. | Rethrow cancellation before applying the existing primary or enrichment fallback; retain graceful degradation for ordinary exceptions. | Fixed |
| `CQ-STATE-005` | Study progress cancellation | Low | High | `StudyProgressStateHolder.kt`, `StudyProgressStateHolderTest.kt` | App-scoped study reads and mutations treated owner cancellation as normal repository failure. | Both suspend `runCatching` blocks caught `CancellationException`: a cancelled first refresh could publish `Error`, and a cancelled mutation could clear its pending marker as though persistence had failed normally. The manual refresh mutex did unlock in `finally`, so no lock leak was present. | Rethrow cancellation, keep ordinary read/mutation failure behavior unchanged, and verify cancellation while repository work is suspended. | Fixed |
| `CQ-BUG-003` | Assessment creation / event lifecycle | High | High | `AssessmentLaunchViewModel.kt`, `FocusedResultViewModel.kt`, `MixedInterviewResultViewModel.kt`, their event collectors and state types, and direct tests | Launch and retake owners permitted another durable attempt after persistence completed but before the buffered navigation event was consumed. | All three owners synchronously guarded work while it was running, but success restored `Idle` before `Channel.send`. A second owner call in that real post-persistence window created another independently identified attempt. The UI normally navigated quickly, but neither the owner boundary nor buffered event delivery made consumption atomic with reopening the action. | Hold the created attempt identity in a terminal pending-navigation state, reject re-entry there, and return to `Idle` only after the single collector successfully invokes the navigation callback for that same identity. | Fixed |
| `CQ-STATE-006` | Assessment lifecycle cancellation | Low | High | `AssessmentTakingViewModel.kt`, `AssessmentHistoryStore.kt`, both result ViewModels, `MistakeReviewStateHolder.kt`, and direct tests | Broad suspend fallbacks converted cancellation into ordinary assessment failure state. | Submission, completion, attempt loading, result loading, retake creation, completed-history reads, mistake derivation, and optional study-link enrichment all used `runCatching` without distinguishing `CancellationException`. Depending on the owner, cancellation could publish a retryable failure/Error, turn history cancellation into `AssessmentHistoryUnavailableException`, or silently remove study links. These paths normally cancel only with their ViewModel or app owner, limiting visible impact. | Rethrow `CancellationException` at each suspend fallback while preserving the established ordinary-error or optional-enrichment behavior. | Fixed |
| `CQ-STATE-007` | Assessment loading / retry concurrency | Medium | High | `AssessmentTakingViewModel.kt`, `FocusedResultViewModel.kt`, `MixedInterviewResultViewModel.kt`, and direct tests | Retry could start overlapping loads before the UI recomposed out of Error. | Each Retry button existed only in settled Error, but each public owner method unconditionally called its loader. Two callbacks dispatched before recomposition therefore launched two repository/review reads, and whichever completed last could overwrite the other result. | Accept retry only while the owner's current state is Error; publish Loading synchronously before launching so later callbacks are ignored. Protect each owner with a controlled pending-load test. | Fixed |

| `CQ-STATE-008` | Progress and mistake queue / retry | Medium | High | `ProgressStateHolder.kt`, `ProgressViewModel.kt`, `MistakeReviewStateHolder.kt`, `MistakeReviewViewModel.kt`, `ProgressViewModelTest.kt`, `MistakeReviewViewModelTest.kt` | Retry could not recover a failed derivation, only a failed history read. | Both dashboards derive from `AssessmentHistoryStore.history`, and both Retry actions invalidated that store. A re-read of unchanged history produces an equal `AssessmentHistory.Loaded`, and a `StateFlow` does not emit an equal value again, so when the read succeeded and the *derivation* over it failed — an unreadable curriculum while the attempt table was fine — nothing downstream re-ran. The surface stayed in `Error` for the rest of the session with a Retry button that did nothing. Existing tests covered only the failed-read path, where `Failed` -> `Loaded` is a real change. | Keep invalidation for the read failure and add an explicit re-derivation request the holder combines into its own upstream, so the retry is an emission whether or not history changed. Protect both owners with a test that fails the derivation while history reads successfully. | Fixed |
| `CQ-STATE-009` | Saved questions / stale resolution | Medium | High | `SavedQuestionsViewModel.kt`, `SavedQuestionsViewModelTest.kt` | A superseded content resolution reported its own cancellation as a curriculum failure. | `resolve` cancels the previous job and starts a new one whenever the saved list changes, which ordinary use reaches: removing one Question and then another produces a saved-list change while the first list is still resolving. The replaced job's `runCatching` caught the `CancellationException` it was cancelled with and published `SavedQuestionsUiState.Error`. A recorded emission list proves the unfixed owner publishes `Error` between two valid `Content` states. Whether that `Error` is also the last word depends on dispatcher ordering — under the single-threaded test dispatcher the replacement wins — but the holder does not re-emit an equal value, so a resolution settling after its replacement would leave a readable list showing a full-screen error until Retry. | Rethrow cancellation so a replaced resolution publishes nothing at all, and assert no `Error` is ever published across a supersession. | Fixed |
| `CQ-STATE-010` | Progress and saved-question cancellation | Low | High | `ProgressStateHolder.kt`, `SavedQuestionStateHolder.kt`, `SavedQuestionStateHolderTest.kt` | The remaining Part-2 suspend fallbacks converted owner cancellation into ordinary state. | Three boundaries caught `CancellationException` alongside operational failure: the progress derivation over loaded history, the saved-state refresh read, and the saved-state mutation with its read-back. A cancelled derivation could publish `ProgressUiState.Error`, a cancelled first read `SavedQuestionsState.Error`, and a cancelled mutation could clear its pending marker as though persistence had failed. All three owners are app-scoped, so cancellation means the process scope is ending and impact is limited — the same reasoning as `CQ-STATE-002`. | Rethrow `CancellationException` before each established fallback, keeping ordinary-failure behavior unchanged. | Fixed |
| `CQ-STATE-011` | Progress dashboard vs Topic detail | Observation | High | `ProgressStateHolder.kt`, `ProgressTopicViewModel.kt` | Progress Topic detail derives from its own history read rather than the shared cache. | The dashboard derives from the `AssessmentHistoryStore` snapshot, while `ProgressTopicViewModel` calls `LearningProgressService.load()` with no attempts, which falls through to `AssessmentRepository.getCompletedAttempts()`. Opening a Topic detail therefore issues a second full history query and can, during the store's documented stale-while-refresh window, show figures derived from a newer snapshot than the dashboard behind it. The window closes as soon as the store's re-read settles, and both surfaces converge; no contradiction survives. | Leave as-is. Both numbers are correct for the snapshot each read, the divergence is transient by construction, and centralizing every progress representation into one app-scoped model is the broader shared-state redesign this audit defers. Recorded so Part 3C/Part 5 can weigh the duplicate query against that cost. | Accepted as-is |
| `CQ-BUG-004` | Curriculum import / reconciliation | High | High | `CurriculumImporter.kt`, `CurriculumImporterTest.kt`, `docs/architecture/persistence.md` | Re-homing a Subtopic to another Topic aborted the whole import on an existing installation. | `question` holds a composite foreign key onto `subtopic(topic_id, id)`, and the exported schema declares it immediate rather than `DEFERRABLE INITIALLY DEFERRED`, so SQLite checks it after every statement. `upsertSubtopics` runs before `upsertQuestions`, so updating `subtopic.topic_id` orphaned the persisted Question rows that still named the old pair, even though the very next statement moved those same Questions and the incoming curriculum passed validation. A fresh install accepted the identical bundle. A JVM probe against the real importer reproduced `SQLiteException: FOREIGN KEY constraint failed` on the second import; because the failure happens inside the startup importer, an upgrading user would have reached an unrecoverable startup error whose Retry could never succeed. | Open the import transaction with `PRAGMA defer_foreign_keys = ON` so the finished graph is checked once at `COMMIT` instead of after each statement, and keep every other write, ordering, and guard unchanged. Atomicity is preserved: a graph that is still inconsistent at `COMMIT` fails the same constraint, rolls back, and leaves the previous curriculum importable, which a second regression test pins. | Fixed |
| `CQ-DATA-001` | Curriculum persistence / documentation | Low | High | `CurriculumDao.kt` | The delete-stale-options contract documented behavior the importer had stopped having. | `deleteAnswerOptionsForQuestionExcept` still stated that a historically referenced option "can still appear in a new assessment for that question", which was true before the answer-option status column existed. The importer now follows that delete with `deprecateAnswerOptionsForQuestionExcept`, and every active query reads through `getActiveAnswerOptionsForQuestions`, so the retained option is excluded from new assessments; `retiredAnswerOptionLeavesActiveQuestionsButStaysReviewable` already proves it. Runtime behavior was correct, only its documentation was not. | Correct the comment to describe the deprecate-and-filter behavior that exists, and open no functional finding. | Fixed |
| `CQ-DATA-002` | Curriculum validation / authored sources | Medium | High | `CurriculumValidator.kt`, `CurriculumValidationErrorCode.kt`, `CurriculumValidatorTest.kt`, `docs/content/content-authoring.md` | A Question citing one source URL twice silently shipped one citation short. | `question_source` is keyed by `(question_id, url)`, but validation checked only presence, blankness, scheme and placeholder hosts. A DAO probe confirmed that upserting two authored sources with the same URL leaves exactly one row, carrying the second title and the second sort order; nothing reported the loss at decode, validation, import or read time. The bundled bank currently contains no such duplicate, so this is a latent authoring trap rather than a live defect. Duplicate answer text was already rejected for the same reason, so the gap was inconsistent as well as silent. | Reject a repeated source URL within a Question with a dedicated `DUPLICATE_SOURCE_URL` code, the way duplicate answer text is already rejected, and leave `LearningCurriculumValidator` alone: Lesson sources are never persisted relationally, so a repeated URL there is a visible duplicate rather than a silent loss. | Fixed |
| `CQ-DATA-003` | Cross-document status consistency | Low | Medium | `LearningCurriculumValidator.kt`, `CurriculumValidator.kt` | Nothing rejects ACTIVE learning content homed on, or teaching, retired assessment taxonomy. | `LearningCurriculumValidator` checks that a Unit's `topicId` and a Lesson's primary and supporting Subtopic IDs *exist* in the assessment curriculum, never that they are ACTIVE. The assessment repository hides descendants of a deprecated parent by joining parent statuses, so a deprecated Topic is genuinely unreachable there; the learning repository filters on Unit and Lesson status only, so an ACTIVE Unit whose home Topic was retired still appears in `getActiveUnits()` and can still be offered by Continue Learning. The bundled content has no deprecated Topic, Subtopic or Unit today, so no instance exists. | Decide the authoring contract before writing a rule: whether retiring a Topic is meant to retire the Units homed on it, or whether such a Unit is deliberately still readable. Only then add the validator rule, because the two answers produce opposite rules and neither is currently stated anywhere. | Deferred |
| `CQ-DATA-004` | Learning-content cache publication | Observation | High | `BundledLearningContentRepository.kt` | The cached document is published through a non-volatile field read outside the mutex. | The double-checked `content ?: mutex.withLock { content ?: ... }` is the standard shape, and a reader that observes the reference can only observe a fully constructed `LoadedLearningContent`: every one of its properties is a `val`, so JVM final-field semantics freeze them and everything reachable from them at the end of construction, and the reference is assigned only after the constructor returns. JS and Wasm are single-threaded, and Kotlin/Native's memory model follows the JVM's. No concurrency defect was found; the safety argument is simply not visible from the code. | Leave as-is. Record why the pattern is safe so a later reader does not "fix" it, and revisit only if `LoadedLearningContent` ever gains a mutable property, which would end the final-field guarantee. | Accepted as-is |
| `CQ-DATA-005` | Validator ownership | Observation | High | `CurriculumValidator.kt`, `LearningCurriculumValidator.kt`, `tools/learning_question_coverage.py` | Kotlin and Python enforce a small overlapping set of authored-content rules. | The coverage tool independently rejects a duplicate Question ID, an unrecognised status or level, an unknown Unit home Topic and an unknown Lesson Subtopic. The boundaries differ legitimately: Python guards the authored repository files in CI before anything is built, the Kotlin validators guard the runtime import and load boundary on a device that may be running an older bundle. The overlapping rules agree today and were checked against each other during this pass. | Keep both. Record the overlap so a future change to one is checked against the other; neither should be deleted for overlapping, because they protect different moments. | Accepted as-is |
| `CQ-BUG-005` | Assessment session / domain invariants | Medium | High | `AssessmentEngine.kt`, `AssessmentEngineTest.kt` | `submitAnswer` did not enforce the authored answer arity, so a SINGLE Question could record several selected answers. | The engine validated status, Question membership, non-emptiness, and that every selected ID belongs to the Question, but never compared the submission against `Question.selectionMode`. `CurriculumValidator` already rejects a SINGLE Question with several correct answers, and `AssessmentTakingViewModel` replaces rather than adds the pending ID for SINGLE, so the rule existed on both sides of the engine and not inside it. A non-UI caller could therefore persist an occurrence recording a choice the interaction never offered, and review, scoring and mistake derivation would read it as genuine. | Enforce `SINGLE` -> exactly one selected ID in `submitAnswer`, after the membership check so unknown IDs keep failing for their own reason. Leave MULTIPLE unconstrained beyond non-emptiness. | Fixed |
| `CQ-DATA-006` | Assessment persistence / contract | Low | High | `AssessmentRepository.kt`, `docs/architecture/persistence.md` | The repository interface stated no contract, so its durability, snapshot, ordering and failure semantics were discoverable only by reading the Room implementation. | Three undocumented suspend functions. `save` is whole-snapshot replacement inside one write transaction and is used for both creation and update; `getById` returns `null` only for an absent attempt and raises for a corrupt one; `getCompletedAttempts` is completed-only, newest-first, and fails as a whole on a corrupt row. None of that was written down, and a caller could reasonably have read `save` as an incremental update or `null` as "read failed". | Document each method's semantics on the interface, and record the save/read contract, the one-writer expectation and the corruption policy in the persistence architecture document. | Fixed |
| `CQ-DATA-007` | Assessment persistence / redundant state | Observation | High | `TestAttempt.kt`, `AssessmentAttemptMapper.kt` | Aggregate score and per-occurrence correctness are stored twice and the schema permits them to disagree. | `test_attempt.score_correct_answers` and `question_attempt.is_correct` express the same fact. `TestAttempt` validates `score.totalQuestions == questionAttempts.size` but not `score.correctAnswers == questionAttempts.count { it is Answered && it.isCorrect }`, so a hand-edited or corrupted database could reconstruct a COMPLETED attempt whose result screen and progress dashboard disagree. No production path can create it: `AssessmentEngine.complete` derives both from the same occurrences, and `save` writes both from one aggregate in one transaction. | Leave as-is. Adding the constructor invariant would churn roughly seventy COMPLETED fixtures across thirty-eight test files to catch a state only external corruption can produce; the summary row is worth keeping because it is what history ordering and the result screen read. | Accepted as-is |
| `CQ-DATA-008` | Assessment persistence / timestamps | Observation | High | `AssessmentAttemptMapper.kt` | Attempt timestamps round-trip at millisecond resolution, so a reloaded attempt is not `==` to the in-memory one when the clock is finer. | `startedAt` and `completedAt` are stored as epoch milliseconds and rebuilt with `Instant.fromEpochMilliseconds`, while `Clock.System.now()` carries sub-millisecond precision on the JVM. Both values are absolute instants with no local-time interpretation, truncation is monotone so `completedAt >= startedAt` and newest-first ordering both survive it, and no consumer compares a saved aggregate with its reloaded form outside tests that use millisecond-aligned fixtures. | Leave as-is. Millisecond resolution is sufficient for history ordering and the tie-breakers behind it, and widening the columns would be a migration for no observable behavior. | Accepted as-is |
| `CQ-DATA-009` | Learning progress / documentation | Low | High | `docs/architecture/progress.md`, `LearningProgressPolicy.kt` | The progress architecture document stated weak-area evidence thresholds the code had stopped using. | The document said "A Topic is weak after at least 3 observations below 70% accuracy, and a Subtopic after at least 2", while `LearningProgressPolicy` has used a single `WeakAreaMinimumAnswered = 5` for both since commit `5f95fd3`, which is what `weakPolicyRequiresEvidenceAndTreatsExactlySeventyPercentAsNotWeak` pins. The stale numbers are the only written statement of a product threshold that decides which weak area a recommendation targets, so a later reader reconciling policy against documentation would have reached the wrong figure. | Restate the documented threshold as the single value of 5 the code applies to both scopes, with the reason the constant already records. The product threshold itself is not a code-quality question and was not changed. | Fixed |
| `CQ-DATA-010` | Historical metadata resolution / query cost | Medium | Medium | `LearningPerformanceDerivation.kt`, `LearningProgressService.kt`, `MistakeReviewService.kt`, `AssessmentReviewLoader.kt` | Historical Question metadata is resolved one stable ID at a time even where the whole ACTIVE bank has already been read in the same call. | `LearningPerformanceDerivation` correctly caches per stable ID, so the per-*occurrence* N+1 does not exist — `historicalLookupsCacheResolvedAndMissingIdentitiesOncePerLoad` pins one lookup per distinct ID. What remains is one `getQuestionById` per distinct historical Question, and `LocalCurriculumRepository` answers each inside its own `withReadTransaction` with several statements. `LearningProgressService.load()` already holds every ACTIVE `Question` from `getActiveQuestions()` in the same call, and `ProgressStateHolder`, `TopicBrowserViewModel` and `TopicDetailViewModel` each issue their own `load()` per history emission. `MistakeReviewService.load` resolves one Question per unresolved mistake for the same reason, though it genuinely needs the full content. No measurement of the cost on any host was taken. | Measure a full-bank history on the JVM and web hosts first. If it matters, the local change is to seed the derivation's cache from the ACTIVE questions the service has already loaded, leaving individual lookups only for identities that ACTIVE content cannot explain; a batched `getQuestionsByIds` on `CurriculumRepository` is the wider alternative and would also serve mistake review. Neither was done without evidence. | Needs measurement |
| `CQ-DATA-011` | Learning progress / documentation | Low | High | `docs/architecture/progress.md` | Nothing said that grouped Topic and Subtopic answered counts may sum to less than the overall answered count. | Overall totals sum persisted `AssessmentScore` values, while `LearningPerformanceDerivation` can only place an occurrence whose Question still resolves through `getQuestionById` and skips the rest with `continue`. `missingQuestionKeepsPersistedOverallWhileDeprecatedAndMissingMetadataRemainScoped` proves the resulting divergence is intended, and Part 3A's never-delete import contract makes it unreachable through ordinary publishing, but the document described the two sources without stating that they can disagree. | Document the divergence and its one cause beside the existing coverage explanation, including that DEPRECATED content does not cause it because `getQuestionById` is the historical resolver. | Fixed |
| `CQ-DI-001` | Common Koin graph / startup timing | Low | High | `settings/AppearanceStateHolder.kt`, `ui/theme/AppearanceTheme.kt`, `AppRoot.kt`, `docs/architecture/overview.md` | Four written statements claimed the appearance preference is read while the host builds its Koin graph, which lazy `single` semantics make false. | Koin 4.2.2 declares `single(createdAtStart: Boolean = false)` and only definitions in `Module.eagerInstances` are instantiated by `createEagerInstances()`; no definition in this repository passes `createdAtStart`, and no host calls `koin.get<AppearanceStateHolder>()` during startup — the four `start*LocalDataGraph` functions resolve `CurriculumDataInitializer` and nothing else. The holder's first resolution is therefore `AppearanceTheme`'s own `remember { KoinPlatform.getKoinOrNull()?.getOrNull<AppearanceStateHolder>() }`, which makes `AppearanceTheme`'s "No storage I/O happens here" the exact opposite of what that composable does. The guarantee the comments were defending is unaffected: the read is synchronous inside `remember`, so it completes within the first composition and before the first frame, and there is still no light-to-dark flash and no startup step awaiting storage. | Restate all four to describe lazy first-resolution and say that *synchronous* rather than *early* is what prevents the flash. The graph is correct as it stands; only the explanation was wrong, and `StudyProgressStateHolder` and `SavedQuestionStateHolder` already describe their own laziness accurately. | Fixed |
| `CQ-DI-002` | Common Koin graph / test coverage | Low | High | `SharedHostStartupTest.kt` | The graph test asserted singleton identity for three app-scoped holders but not for `AssessmentHistoryStore`, and never resolved two of the fifteen ViewModel bindings. | `sharedHostModulesResolveTheWholeProductGraph` already pinned one instance each of `SavedQuestionStateHolder`, `StudyProgressStateHolder` and `AppearanceStateHolder`, but not the completed-history cache that eleven consumers share and that the Part 2 and Part 3 conclusions rest on — so `single` becoming `factory` there would have left every history-derived surface with its own cache and no test would have failed. Switching the definition to `factory` was confirmed to leave the suite green before the assertion was added, and to fail at the new assertion afterwards. `AppShellViewModel` and `InterviewStartViewModel` were also the only two bindings no graph-level test resolved. | Assert one `AssessmentHistoryStore` beside the three existing identity assertions and resolve the two remaining ViewModels, in the one test that installs the real `assessmentDataModule`. No new test class; the existing graph check is the right owner. | Fixed |
| `CQ-DI-003` | Navigation 3 / ViewModel store ownership | Low | High | `AppNavigator.kt`, `App.kt` | Two back-stack entries with an equal route would silently share one `ViewModelStore`, and `push` does not prevent one. | `ViewModelStoreNavEntryDecorator` scopes each store by `NavEntry.contentKey`, which `defaultContentKey` derives as `key.toString()`; `AppRoute` is a sealed interface of `data class`/`data object`, so two equal routes produce one key and, as `NavEntry`'s own documentation states, "NavEntries that share the same contentKey will be handled as sharing the same content and/or NavEntryDecorator state". `AppNavigator.push` appends unconditionally. Tracing every `navigator.push` call site in `App.kt` shows the hazard is currently unreachable: within one area, no destination reachable from a route can push that same route again, every attempt and result route carries a freshly generated `attemptId`, and the Practice Builder and Lesson reader are terminal with respect to the routes above them (`onNavigateLesson` replaces rather than pushes). The four areas hold independent stacks, so switching areas cannot collide either. | Leave as-is. The invariant holds today through the shape of the navigation graph rather than through a guard, and adding a duplicate check to `push` would be navigation work this chunk does not own. Recorded so that a future route addition — in particular any new path back to a Topic, Unit or Practice Builder route already on the stack — is understood to be a ViewModel-ownership change and not only a navigation one. | Accepted as-is |
## Audit Pass Log

| Pass | Area | Status | Commit reviewed | Files reviewed | Findings | Fixes | Validation | Notes |
| ---- | ---- | ------ | --------------- | -------------: | -------: | ----: | ---------- | ----- |
| Part 0 | Baseline and inventory | Complete | `75e7b30c9b08a5ea1cf275c1c7793bfa4ab4c96f` | 409 Kotlin paths inventoried plus build, CI, and architecture documentation | 0 | 0 | Four Gradle baseline checks; `git diff --check`; documentation-only final status | No production quality review performed. |
| Part 1A | App shell, navigation, shared UI, and settings | Complete | `c025e027b077eb5e9600ee26e9283180486b4635` | 41 assigned production Kotlin files, 12 direct dependencies/callers, and 20 relevant tests | 3 | 1 | Targeted `AppRootTest`; `:shared:jvmTest`; `:androidApp:assembleDebug`; `git diff --check` | High 0, Medium 0, Low 3; two deferred. No recomposition, expensive-composition, state-read-scope, stability, remember, `derivedStateOf`, effect-key, lazy-identity, side-effect-during-composition, or accessibility defect was verified. See review record below. |
| Part 1B | Topic discovery and lesson reading | Complete | `753ab8d8f1db93ea1b2f90b399e8bd28629f65b8` | 25 assigned production Kotlin files, 7 shared/model dependencies, and 16 relevant test files | 4 | 4 | Targeted `TopicBrowserScreenTest` and `TopicDetailScreenTest`; `:shared:jvmTest`; `:androidApp:assembleDebug`; `git diff --check` | High 0, Medium 1, Low 3. One lazy-state identity bug, two accessibility defects, and one duplicated stable concept were fixed. No qualifying recomposition/performance concern was found. See review record below. |
| Part 1C | Assessment launch, taking, review, and practice builder | Complete | `a37b5a7385572e2b727b6580a62b165a46e51a64` | 23 assigned production Kotlin files, 10 shared dependencies/callers, and 16 relevant test files | 3 | 3 | Targeted `AssessmentTakingScreenTest` and `PracticeBuilderScreenTest`; `:shared:jvmTest`; `:androidApp:assembleDebug`; `git diff --check` | High 0, Medium 1, Low 2. One question-local lazy identity bug and two accessibility defects were fixed. No qualifying recomposition/performance concern or new Part-2 deferral was found. See review record below. |
| Part 1D | Results, history, progress, mistakes, and saved questions | Complete | `e499932b8168e2d76661f17eeb39ab511514bc48` | 43 assigned production Kotlin files, 9 shared UI/review dependencies, and 32 relevant tests | 6 | 5 | Six targeted Compose suites; `:shared:jvmTest`; `:androidApp:assembleDebug`; `:shared:check`; `git diff --check` | High 0, Medium 3, Low 3. `CQ-UI-002` and four new UI findings were fixed; one saved-state race was deferred to Part 2D. See review record below. |
| Part 2A | Shell, appearance, and application state | Complete | `34bb8fae8c35e47b924be3ccc5caad8f7bd59b53` | 24 production/state files and 10 relevant test files | 3 | 3 | Targeted startup, appearance, navigator, and host/initializer suites; `:shared:jvmTest`; `:androidApp:assembleDebug`; `:shared:check`; `git diff --check` | High 0, Medium 0, Low 3. `CQ-BUG-001` and two cancellation findings were fixed. No Part 4 finding was opened. See review record below. |
| Part 2B | Learning and practice-builder state | Complete | `89420828d45fa7ecb61358990ec499f9a0d7955e` | 31 production/state/dependency files and 15 relevant test files | 3 | 3 | Six targeted owner suites; `:shared:jvmTest`; `git diff --check` | High 0, Medium 0, Low 3. Cancellation now propagates from Topic Browser, Topic Detail, and study-progress work; all stale-result, runtime-parameter, event, and read-back mechanisms were accepted. See review record below. |
| Part 2C | Assessment, mixed interview, results, and mistakes | Complete | `42e49a5e32a339df77e19082003fbfcc3bfc22d4` | 32 owner/state files, 8 supporting contracts/integration boundaries, and 23 relevant test files | 3 | 3 | Six targeted owner commands; `:shared:jvmTest`; `:shared:check`; `git diff --check` | High 1, Medium 1, Low 1. Durable creation stays locked through navigation-event consumption, retries cannot overlap, and every broad Part-2C suspend fallback now preserves cancellation. Part 2D is next. See review record below. |
| Part 2D | Progress and saved-question state | Complete | `7b67932de7ede0c8f9a8c293b53fb3a24c255e39` | 12 owner/state files, 9 supporting contracts, and 12 relevant test files | 4 | 4 | Seven targeted owner/journey commands; `:shared:jvmTest`; `:shared:check`; `git diff --check` | High 0, Medium 2, Low 1, Observation 1. `CQ-STATE-001` is resolved, both history-derived dashboards can now recover a failed derivation, and every remaining Part-2 suspend fallback preserves cancellation. Part 2 is complete; Part 3A is next. See review record below. |
| Part 3A | Curriculum and bundled learning content | Complete | `0675d0dfbac6de69c4545fd6b6240d66646641a6` | 40 assigned production Kotlin files, 6 supporting contracts, and 14 relevant test files | 6 | 3 | Targeted importer, validator, repository, codec and learning-content commands; `:shared:jvmTest`; `:shared:check`; both Python content validators; `git diff --check` | High 1, Medium 1, Low 2, Observation 2. `CQ-BUG-004` is a verified upgrade-only import abort and is fixed with no schema change; one silent authored-source loss and one misleading data contract are fixed; one cross-document status rule is deferred pending a content decision. Part 3B is next. See review record below. |
| Part 3B | Assessment sessions, attempts and history persistence | Complete | `c0dafad5468b6c0ba36628bbe0f924db554309e9` | 22 assigned production Kotlin files, 6 supporting contracts, 3 entities plus the attempt DAO, 2 assessment migrations, and 12 relevant test files | 4 | 2 | Four targeted engine/store/migration/integration commands; `:shared:jvmTest`; `:shared:check`; `git diff --check` | High 0, Medium 1, Low 1, Observation 2. `CQ-BUG-005` closes the SINGLE-arity gap between authoring validation and the UI; `CQ-DATA-006` writes down the repository's save, read, corruption and one-writer contract. Aggregate save is atomic and stale-child free, config and scope round-trip exactly, historical correctness is persisted rather than recomputed, and completed history is three queries with deterministic ordering. No schema change. Part 3C is next. See review record below. |
| Part 3C | Study progress, recommendations and saved questions | Complete | `ef291a087206777d2d06c90bd13648e677a4f288` | 24 assigned production Kotlin files, 5 supporting contracts, 2 learner-owned entities plus their DAOs, 2 learner-state migrations, and 14 relevant test files | 3 | 2 | Two targeted repository commands including a deliberate REPLACE falsification run; six targeted derivation/policy commands; `:shared:jvmTest`; `:shared:check`; `git diff --check` | High 0, Medium 1, Low 2, Observation 0. Both learner-owned repositories satisfy their documented idempotency, re-add, ordering and orphan contracts; insert-ignore plus a stable-ID tie-break makes them correct under concurrent duplicate writes, which two new regressions now pin. The derivation layer keeps historical evidence and current content strictly apart and shares one exposure, weak-area and mistake definition per concept. `CQ-DATA-009` and `CQ-DATA-011` correct and complete the progress architecture document; `CQ-DATA-010` records per-stable-ID historical metadata resolution as needing measurement. No schema change. Part 3D is next. See review record below. |
| Part 3D | Preference store contract | Complete | `a77156d64e56969bf769c2d26e40a10724ced283` | 5 assigned production Kotlin files, 3 supporting call sites, 4 platform implementations read for contract only, and 5 relevant test classes | 0 | 0 | One targeted appearance/preference command (22 tests, 5 classes); `git status --short` | High 0, Medium 0, Low 0, Observation 0 — **no material issue found**. The two-method storage contract, its no-throw and absence-is-null promises, the unrecognised-token degradation and the clear-on-System encoding all hold and are pinned by tests at the boundary every host implements. One key and two tokens are declared once in common code. Nothing changed. Part 3 is complete; Part 4A is next. See review record below. |
| Part 4A | Common Koin graph and lifetimes | Complete | `a77156d64e56969bf769c2d26e40a10724ced283` | Seven common modules and all 45 definitions, 20 `koinViewModel` sites, 17 Compose resolution files, 4 host startup bridges read for contract only, 30 singleton classes read for fields, and 6 relevant test files | 3 | 2 | Six targeted graph/DI commands; a deliberate `factory` falsification run; `:shared:jvmTest`; `:shared:check`; `git diff --check` | High 0, Medium 0, Low 3, Observation 0. Every lifetime matches required ownership and no binding changed: 30 singles, 0 factories, 15 ViewModels (8 parameterized), 45 distinct binding keys and 45 distinct concrete constructions, no duplicates, no qualifiers, no cycles, no layer inversion, and no domain or data class reaching the container. `CQ-DI-001` corrects four statements that claimed a graph-build-time preference read that lazy `single` semantics make impossible; `CQ-DI-002` pins the shared history cache's singleton identity and the two ViewModel bindings no graph test resolved; `CQ-DI-003` records the duplicate-route ViewModelStore hazard as currently unreachable. Part 3 and Part 4A are complete; Part 4B is next. See review record below. |

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

## Part 2B Review Record

- **Owner boundary:** `TopicBrowserViewModel`, `TopicDetailViewModel`, `LearningUnitViewModel`,
  `LearningLessonViewModel`, `PracticeBuilderViewModel`, and `StudyProgressStateHolder`, together
  with their UI states, events, runtime targets, route mappings, pure study derivation and continue
  policy, destinations, Navigation 3 routes, and Koin parameter call sites.
- **Dependencies inspected:** the curriculum, learning-content, lesson-study, assessment-history,
  question-selector, learning-progress, recommendation, and continue-studying contracts were read
  only far enough to establish suspension, ordering, caching, and selection semantics. The bundled
  learning repository and `AppCoroutineScope` were inspected only for cancellation cooperation and
  lifetime. Repository implementations, Room, persistence, and graph quality remain Parts 3 and 4.
- **Tests inspected (15 files):** the six direct owner suites; `PracticeTargetResolverTest`;
  `PracticeBuilderRouteMappingTest`; and the focused learning, learning navigation, reader, Unit
  practice, Topic discovery, targeted-practice lifecycle, and guided-preset journeys. Part 1's
  destination/effect findings were reused rather than re-auditing Compose rendering.
- **Cancellation audit and fixes:** `CQ-STATE-003`, `CQ-STATE-004`, and `CQ-STATE-005` are fixed.
  Topic Browser's three broad fallback boundaries, Topic Detail's four, and the holder's two now
  rethrow `CancellationException` and retain their established fallback only for ordinary
  exceptions. `LearningUnitViewModel`, `LearningLessonViewModel`, `PracticeBuilderViewModel`, and
  `PracticeTargetResolver` already rethrew cancellation before broad fallback. The source-opening
  `runCatching` in `LearningLessonDestination` wraps a synchronous URI action rather than suspend
  work and is outside state ownership. No other `runCatching`, `getOrElse`, `fold`, or broad catch
  around suspend work remains in this Part 2B boundary without explicit cancellation propagation.
- **Topic Browser:** `derivedOrNull` remains the optional-enrichment boundary, now cancellation
  preserving. Ordinary progress, recommendation, continue-studying, and learning-content failures
  still cost only their decoration and never become catalogue errors. Retry increments the
  generation before clearing the old catalogue's count and Unit data; checks after the catalogue
  read and the full learning-content read reject every stale publisher result. Query is independent,
  survives retry, and always filters the latest loaded catalogue.
- **Topic Browser snapshot and work:** progress, recommendation, and continue-studying are derived
  sequentially from one history emission and assigned only after every derivation finishes. An
  unrelated render during suspension therefore sees the complete previous enrichment snapshot,
  never new progress beside old cards. `collect` prevents an older history emission overtaking a
  newer one. One `LearningProgressService.load` result feeds both rows and recommendation; no
  duplicate progress derivation or credible measurement-level performance issue was found.
- **Topic Detail:** the fixed `topicId` is validated, missing or retired content has a controlled
  state, curriculum generations reject stale primary and learning-content results, and retry clears
  the previous Unit list before loading. History analytics and unresolved-mistake IDs come from one
  sequential collector; there is no suspension between their private-field assignments, so render
  cannot observe a half-written history snapshot. Study progress is derived from the same retained
  ACTIVE Unit list and shared studied-ID snapshot used by its rows. Loading or unavailable Units can
  coexist with a derived empty internal progress value, but the study page consumes progress only
  for `Available` Units; no user-visible false zero or impossible action was found.
- **Learning Unit and Lesson:** both cancel a prior retry job, rethrow cancellation, retain the
  resolved publisher object, and derive every row/aggregate studied value from one holder emission.
  Missing, deprecated, or mismatched IDs become `NotFound`; repository failures remain retryable
  `Error`. Production learning-content lookup uses a cancellable mutex on first load and immutable
  cached indexes afterwards, so cancellation is sufficient for retry replacement without a second
  generation mechanism. Opening or navigating Lessons performs no study write.
- **Runtime parameters and identity:** Topic, Unit, Lesson, and builder routes are data-class
  Navigation 3 keys with per-entry ViewModel stores. Destination `parametersOf`, Koin definitions,
  constructors, and integration helpers agree on parameter order. The same-type Lesson pair is
  consistently `unitId` then `lessonId`, uses explicit indexed Koin reads, and containment tests
  reject a valid Lesson paired with the wrong Unit. No runtime-ID mix-up or reused-owner mismatch was
  found; new inline ID types are not justified in this pass.
- **Practice Builder target and availability races:** replacement cancels the previous resolver or
  selector job, both production paths use cooperative suspend contracts, and every broad fallback
  rethrows cancellation. A resolved scope is published before its eligibility job starts; a changed
  level/source publishes `Checking` synchronously, so the brief count/options update before
  `Available` cannot enable Start against old availability. No stale target or availability result
  can overwrite the latest UI configuration under the current contracts.
- **Practice Builder selection and events:** question count does not refresh eligibility because it
  narrows a pool whose size depends only on scope, levels, source, content, and history. StateFlow
  updates preserve at least one level; unsupported initial and later sources are rejected, and ALL
  is currently supported. `currentConfig` receives the explicit preflight count at its only call
  site and snapshots the remaining state once. Repeated builder Start events are harmless at the
  full destination boundary: the single collector forwards them sequentially to
  `AssessmentLaunchViewModel`, whose synchronous Idle-to-Launching guard accepts only the first.
  The buffered Channel appropriately survives a brief collector gap, is owned by the destination's
  ViewModel, and is discarded with that navigation entry.
- **Study-progress refresh and mutation ordering:** `tryLock` drops overlapping destination-open
  refreshes, which is safe for current ownership because every in-process write goes through this
  holder and performs its own serialized read-back; Part 3C still owns repository/external-writer
  assumptions. A refresh that sampled before a write publishes before that mutation's locked
  read-back. Different-Lesson writes may overlap, but every read-back takes the same mutex: a read
  that misses a still-running write is followed by that write's own read-back, while any read after
  both writes sees both. Existing deterministic tests cover an older refresh settling after a write
  and concurrent different-Lesson use; no state regression remains.
- **Pending and read-back failure:** pending IDs affect only their exact Lesson and are never counted
  as studied. Success publishes repository read-back; write or read-back failure clears only that
  pending ID and retains the last persisted snapshot, allowing immediate retry or later refresh.
  The brief chance to request the opposite operation after a successful write whose read-back failed
  follows from deliberately refusing to guess persisted truth and was accepted as-is. Cancellation
  now leaves owner state untouched instead of simulating an operational failure. Manual `tryLock`
  always unlocks in `finally`, and `withLock` is cancellation safe.
- **Ownership and collectors:** application scope is appropriate for one study projection shared by
  retained parent Learn destinations. It is a Koin singleton using the process-lifetime
  `AppCoroutineScope` (`SupervisorJob + Dispatchers.Default`) and is used for app-scoped state-holder
  work. Part 4A retains the full Koin/container teardown audit; no Part 4 finding was opened here.
  Topic Browser and Topic Detail each collect the cached history once for different outputs; all four
  Learn owners collect the cached study state once. The small retained-destination duplication did
  not justify a global presentation cache or measurement finding.
- **State and abstraction conclusion:** unknown, unavailable, loaded-empty, not-found, and primary
  error states remain distinct. Search, publisher content, assessment history, and learner study
  state are independent inputs only where the UI can usefully degrade. Manual `render()` aggregation
  provides synchronous input updates and explicit atomic publication; no lost concurrent write or
  reachable contradictory action was found. Similar load/render patterns differ in primary versus
  optional failure and retry behavior, so no Part 5 base-ViewModel or generic state-machine finding
  was opened.
- **Handoffs:** Part 3C retains lesson-study repository atomicity, query snapshot, and external-writer
  assumptions, plus selector/history repository internals. Part 4A retains full graph ownership and
  process-scope teardown. Part 5 may reassess repetition only with wider cross-feature evidence; no
  `CQ-CROSS` finding was justified here.
- **Validation:** the six targeted owner suites passed, including five new cancellation regressions;
  `./gradlew :shared:jvmTest` passed. `:shared:check` was not run because the common production edit
  only changes exception branching around APIs already compiled for all targets and the focused plus
  full JVM suites exercised every changed owner. Android assembly, lint, device UI, and browser UI
  checks were not run because no platform or Compose code changed. `git diff --check` passed.

## Part 2C Review Record

- **Owner/file boundary (32 production files):** assessment launch (`AssessmentLaunchCoordinator`,
  `AssessmentLaunchDialog`, `AssessmentLaunchViewModel`, `StartAssessment`); assessment taking
  (`AssessmentQuestionUiModel`, `AssessmentTakingDestination`, `AssessmentTakingScreen`,
  `AssessmentTakingUiState`, `AssessmentTakingViewModel`); focused result
  (`FocusedResultDestination`, `FocusedResultScreen`, `FocusedResultUiState`,
  `FocusedResultViewModel`); mixed interview (`InterviewHistoryStateHolder`,
  `InterviewStartDestination`, `InterviewStartScreen`, `InterviewStartViewModel`,
  `MixedInterviewDestination`, `MixedInterviewResultDestination`, `MixedInterviewResultScreen`,
  `MixedInterviewResultUiState`, `MixedInterviewResultViewModel`); mistake review
  (`MistakeReviewDestination`, `MistakeReviewModels`, `MistakeReviewScreen`, `MistakeReviewService`,
  `MistakeReviewStateHolder`, `MistakeReviewUiState`, `MistakeReviewViewModel`); and shared history
  (`AppCoroutineScope`, `AssessmentHistoryStore`, `CompletedAssessmentHistory`).
- **Supporting contracts and integration boundaries inspected (8):** `AssessmentEngine`,
  `AssessmentSession`, `AssessmentSessionLoader`, `AssessmentRepository`, `AssessmentRetakeService`,
  `AssessmentReviewLoader`, the focused/mixed Navigation 3 route entries in `App`, and the
  presentation/data Koin definitions only far enough to establish parameters, event collectors,
  cache fan-out, and owner lifetime. `LocalAssessmentRepository`, Room, persistence atomicity, and
  graph teardown were not audited.
- **Tests inspected (23 files):** the direct launch dialog/ViewModel, taking screen/ViewModel,
  focused screen/ViewModel, interview-start screen/ViewModel, mixed-result screen/ViewModel and
  integration, mistake destination/screen/ViewModel/study-link, and history-store suites; supporting
  engine, session-loader, retake, review-loader, and mistake-service suites; plus the targeted-
  practice and saved-question lifecycle journeys. This was risk-directed inspection, not Part 6.
- **Existing findings:** no ledger finding was deferred to Part 2C, so none required disposition.
  `CQ-STATE-001` remains deferred to Part 2D and was not re-audited. New `CQ-BUG-003`,
  `CQ-STATE-006`, and `CQ-STATE-007` are fixed. Counts for this pass: Critical 0, High 1, Medium 1,
  Low 1, Observation findings 0; Fixed 3, Deferred 0, Needs measurement 0, Accepted as-is 0, and Not a
  defect 0.
- **Cancellation sweep:** ten broad suspend fallback boundaries were verified and fixed: taking
  submission, completion, and load; focused result load and retake; mixed result load and retake;
  history read; mistake queue derivation; and optional Lesson enrichment. Each now rethrows
  `CancellationException` before its ordinary fallback. Launch already used explicit
  `try/catch` with cancellation rethrow. The three destination `runCatching` calls wrap synchronous
  `openUri`, and the taking `getOrNull` calls are bounds checks, not exception fallbacks.
- **Launch and event lifecycle:** the synchronous `Idle` -> `Launching` transition prevents two
  initial calls from launching. `CQ-BUG-003` covered the distinct post-persistence window: success
  now publishes `Created(attemptId)`, sends one buffered event, and accepts no new start until the
  coordinator has invoked navigation and acknowledged the same identity. Failure dismissal still
  returns to `Idle`; retry retains the original config. The one producer/one collector buffered
  Channel is appropriate across brief collector gaps and is discarded with its Navigation entry.
- **Launch partial success:** `AssessmentEngine.start` has no externally meaningful mutation;
  `StartAssessment` emits `Created` only after `save` returns. Whether a repository write can commit
  and then throw or be cancelled is a Part 3B durability/idempotency handoff, not a presentation fix.
- **Taking submission:** `isSubmitting` is published before the coroutine starts, so duplicate calls
  cannot both save; a deterministic gated-save test now proves it. Selection rejects IDs outside
  the current Question and enforces SINGLE replacement versus MULTIPLE toggle. Selected IDs and the
  current Question remain stable while submitting because selection, next, completion, and UI retry
  are all state-gated; the engine also copies the selected set before the only suspension. No extra
  snapshot or generation was justified. Nothing throwing runs after a successful in-progress save,
  so the UI cannot report submission failure after that save returns normally.
- **Feedback and question identity:** formative feedback reads the answered occurrence at the same
  guarded index and retains the submitted IDs. Selection is blocked while feedback exists;
  `nextQuestion` requires feedback and synchronously leaves `Content`, so repeated Next cannot skip.
  On the final focused Question it enters `ReadyToComplete` and immediately uses the shared guarded
  completion path. Mixed/interview taking instead remains explicitly ready for Finish.
- **Completion:** `isCompleting` is published synchronously and a gated-save test proves repeated
  Finish/completion calls persist once. Completion saves the completed attempt, then synchronously
  invalidates history, then publishes success; `invalidate` and state construction do not throw or
  suspend, so no post-save operational-failure window exists after a normally returning save.
  In-progress saves do not invalidate. A save that commits before throwing/cancelling remains a Part
  3B repository-contract handoff because presentation cannot infer that partial outcome.
- **Attempt loading, retry, and resume:** `CQ-STATE-007` is fixed. A completed/non-in-progress
  attempt goes directly to result navigation state; missing attempts or Questions become retryable
  Error. Retry is exposed only from settled Error and each owner now verifies that state before
  synchronously publishing Loading, so repeated callbacks cannot overlap loads or let an older
  retry overwrite a newer one. Controlled pending-load tests protect taking and both result owners.
  Resume selects the first unanswered occurrence; the engine produces contiguous answered prefixes,
  while enforcement for malformed persisted gaps is retained for Part 3B. All-answered IN_PROGRESS
  attempts correctly become `ReadyToComplete`.
- **History store:** eager app-scoped state and ordinary sequential `map` are retained. During an
  in-flight invalidation, observers may briefly keep/publish the previous generation as documented,
  while one-shot callers wait for the required generation. A controlled two-generation test proves
  the old generation cannot satisfy the one-shot read and the newer generation ultimately replaces
  it. `generation >= requiredGeneration` correctly lets a later settled generation satisfy an
  earlier requirement. Concurrent callers after failure are Mutex-coalesced onto one retry; failed
  refresh retains prior loaded observer data but is never served to one-shot selection. Cancellation
  no longer becomes `Failed` or `AssessmentHistoryUnavailableException`. `mapLatest` would weaken
  the intentional sequential-query contract and was not adopted.
- **Results and retakes:** both result loads rethrow cancellation and ordinary failures remain
  retryable. Retry is accepted only from a settled error and synchronously leaves it. Focused and
  mixed retakes now share the identity-bearing pending-navigation guard and identity-checked event
  acknowledgement from their single collectors; deterministic tests cover re-entry after durable
  creation but before consumption. Ordinary retake failures retain result content and allow retry.
  Focused's captured content previously had no concurrent same-stream writer; success now uses the
  current state through `setRepeatState` as part of the common fix. Saved-question state remains a
  separate app-scoped stream and only available review IDs can be toggled.
- **Result correctness:** mixed results explicitly reject non-Mixed attempts. Focused results are
  reached only from the typed focused completion/retake routes, so an additional config check would
  duplicate that integration invariant without a reachable mismatch. Blank IDs are rejected in
  both result constructors; taking delegates the same check to `AssessmentSessionLoader`. Topic
  performance excludes missing Questions, retains first-encounter Topic order, permits missing Topic
  names, and cannot divide by zero because a bucket is created only while incrementing its count.
- **Mistake state:** the app-scoped holder remains the single queue owner and the ViewModel delegates
  it without copying state. Full derivation failure remains Error; ordinary optional learning-content
  failure still publishes the queue without study links. Cancellation propagates from both paths.
  Sequential history mapping can briefly publish an older queue before a newer generation, matching
  the history store's documented stale-while-refresh observer contract; results cannot invert.
  Refresh invalidates the shared history source and refreshes saved state independently, and save
  toggles reject unavailable review content.
- **Ownership and fan-out:** one completion invalidation reaches the eagerly derived mistake queue,
  interview history, progress state, shell badge, Topic learning context/recommendations, and future
  one-shot selection through the one shared history store. App-scoped history and mistake ownership
  prevents tab switches from recreating Loading. Full Koin lifetime and shutdown ownership remains
  Part 4A; no Part 4 finding was opened. No duplicated presentation state or Part 5 base-ViewModel
  abstraction was justified; focused/mixed repetition remains local and explicit.
- **Handoffs:** Part 3B retains ambiguous commit-then-throw/cancel outcomes for start, retake,
  submission, and completion; repository idempotency/transaction guarantees; and malformed
  IN_PROGRESS answer-prefix invariants. Part 4A retains app-scope teardown. Part 5 may reassess the
  parallel result owners only with wider evidence; no `CQ-CROSS` finding was opened.
- **Tests and validation:** changed six direct test files. Added deterministic post-persistence
  launch/retake re-entry tests; gated duplicate submission/completion tests; cancellation regressions
  for taking, result loads/retakes, history, mistake derivation, and study enrichment; coalesced retry
  and two-generation history tests; pending-load duplicate-retry tests for all three owners; and
  ordinary optional-enrichment degradation coverage. The six
  requested targeted Gradle commands passed after launch expectations and focused/mixed terminal-
  state fixtures were updated for the new owner contracts; `./gradlew :shared:jvmTest` passed.
  `./gradlew :shared:check` and `git diff --check` passed. The existing expect/actual Beta warning
  appeared during compilation.
  Android assembly, lint, and device/browser UI checks were not run because no platform source,
  navigation route, layout, theme token, or visual design changed. The result screens reuse the
  existing Creating text/spinner/button-disabled presentation for the brief Created state, so no
  pixel verification or Material deviation was needed.
- **Next:** Part 2D — Progress and saved-question state. It was not started in this pass.

## Part 2D Review Record

- **Owner/file boundary (12 production files):** `progress`: `ProgressStateHolder`,
  `ProgressViewModel`, `ProgressTopicViewModel`, `ProgressUiState`, `ProgressTopicUiState`,
  `ProgressDestination`, `ProgressTopicDestination`; `saved_questions`:
  `SavedQuestionStateHolder`, `SavedQuestionsViewModel`, `SavedQuestionsState`,
  `SavedQuestionsUiState`, `SavedQuestion`. `MistakeReviewStateHolder` and `MistakeReviewViewModel`
  were edited for `CQ-STATE-008` only; see the handoff note at the end of this record.
- **Supporting contracts inspected (9):** `AssessmentHistoryStore` (as the contract Part 2C
  established, not re-audited), `LearningProgressService`, `MistakeReviewService`,
  `SavedQuestionContentResolver`, `SavedQuestionRepository`, `CurriculumRepository`, `TestAttempt`,
  `TopicStudyPresentationModule`, and `StudyProgressStateHolder` as the Part-2B reference pattern.
- **Tests inspected (12 files):** `SavedQuestionStateHolderTest`, `SavedQuestionsViewModelTest`,
  `SavedQuestionContentResolverTest`, `SavedQuestionLifecycleIntegrationTest`,
  `SavedQuestionCaptureIntegrationTest`, `SavedQuestionsScreenTest`, `ProgressViewModelTest`,
  `ProgressTopicViewModelTest`, `ProgressScreenTest`, `ProgressTopicScreenTest`,
  `ProgressLearningJourneyIntegrationTest`, and `StudyProgressStateHolderTest`. There is no separate
  `ProgressStateHolderTest`; `ProgressViewModelTest` constructs the holder directly and is where the
  dashboard contract lives. Part 6A retains the coverage assessment.
- **Existing findings:** `CQ-STATE-001`, the only finding deferred to Part 2D, is fixed. No other
  finding was deferred here, so none remains carrying a Part-2D disposition.
- **Progress ownership:** app-scoped ownership is correct and unchanged. The Progress tab's
  navigation entry destroys `ProgressViewModel`, the dashboard must not flash Loading over figures
  already loaded, and the same completed-history snapshot feeds the mistake queue, interview record
  and shell badge. `ProgressViewModel` stays thin: it republishes the holder's `StateFlow` and owns
  no derivation or local copy.
- **Progress cancellation and failure domains:** `CQ-STATE-010` makes the derivation rethrow
  `CancellationException` rather than publishing `ProgressUiState.Error`. The remaining fallback is
  deliberately whole-dashboard: `LearningProgressService.load` produces every headline figure, so
  its failure leaves nothing to show. Degrading the unresolved mistake count separately was
  considered and rejected — with history supplied, `MistakeReviewService.countUnresolved` is a pure
  in-memory derivation that issues no read, so it cannot realistically fail on its own, and
  inventing an optional-enrichment path for it would add a state distinction no failure produces.
- **Progress stale history and derivation latency:** no second cache layer exists. The holder maps
  the history store's emissions and adds nothing of its own. `historyStore.history` is a
  `StateFlow`, so a derivation running while a newer history arrives resumes on the latest value and
  an older derivation cannot publish after a newer one; ordinary `map` serializes derivations, which
  is sequential consistency rather than a race, and `mapLatest` was not adopted for the same reason
  Part 2C rejected it. The worst-case stale period is one derivation — two curriculum reads and
  in-memory grouping — which is not a user-facing stale state.
- **Progress snapshot consistency and duplicate reads:** completed-attempt count, answered/correct
  counts, percentage, curriculum coverage, recent performance, unresolved mistake count, weak areas,
  Topic performance and the history cards all derive from the single `history.attempts` list the
  store supplied. Both `LearningProgressService.load(completedAttempts)` and
  `MistakeReviewService.countUnresolved(completedAttempts)` consume that snapshot and issue no
  history read of their own; `mapHistory` reuses it too. The one repository read inside a derivation
  is the ACTIVE question bank, which is the coverage denominator and not history.
- **Historical metadata and completed-attempt invariants:** `mapHistory` tolerates a missing Topic
  or Subtopic as a null label and never fabricates one, so a retired ID keeps its score row and a
  multi-Subtopic focused scope keeps no subtitle. `getOrLoad` caches null through `containsKey`, so
  a repeated missing ID is queried once. The `requireNotNull(attempt.score)` and
  `requireNotNull(attempt.completedAt)` calls are not defensive gaps: `TestAttempt.init` already
  requires both for `COMPLETED`, so a malformed row cannot reach presentation as a completed
  attempt — it fails at construction, which is Part 3B's boundary. No finding.
- **`ProgressTopicViewModel`:** accepted unchanged. `topicId` is required non-blank at
  construction, retry cancels the previous job before publishing Loading, and the existing explicit
  `catch (cancellation: CancellationException) { throw cancellation }` is already correct. Stale
  results cannot publish: the ViewModel runs on the main dispatcher, so a replaced job is either
  never started or suspended inside a cooperative `LearningProgressService.load`, and resumes by
  throwing. No generation token is justified. A Topic with no observations is `Empty` rather than
  `Error`, and coverage stays `null` when the Topic has no ACTIVE questions rather than collapsing
  to 0%, keeping observed history and current coverage as the two separate figures they are.
- **Dashboard versus Topic detail:** recorded as `CQ-STATE-011` and accepted. The detail screen
  reads history independently through `LearningProgressService.load()`; the divergence is bounded by
  the store's documented stale-while-refresh window and both surfaces converge. No global progress
  cache was introduced.
- **`SavedQuestionStateHolder` ownership and concurrency:** app-scoped ownership is correct and was
  not moved — Focused Result, Mixed Result, Mistake Review and Saved Questions must observe one set
  of saved identities, and all four consume `savedQuestionStateHolder.state` directly with no local
  copy. `CQ-STATE-001` is fixed by the Part-2B pattern rather than a new mechanism: the mutation's
  read-back now takes the same `reading` Mutex that `refresh` holds. Persistence-first visibility is
  preserved; no optimistic mutation was introduced. Two deterministic tests with snapshot-on-entry
  repository gates prove both orderings, and both fail against the previous implementation.
- **Saved refresh deduplication and pending markers:** `tryLock` still drops an overlapping refresh,
  which remains safe for the same reason Part 2B accepted it for study progress — every in-process
  write performs its own read-back under the same lock, so a dropped refresh is always followed by a
  read that observes the write. The same-Question guard is adequate because `toggleSaved` is reached
  only from serialized UI event dispatch, and the realistic case is two different Questions, which
  the read-back ordering now covers. `settle` transforms current state, so publishing one mutation's
  read-back preserves another's pending marker; pending-marker correctness and data-snapshot
  correctness were checked separately and only the latter was defective.
- **Save/unsave cancellation and partial success:** `CQ-STATE-010` stops a cancelled mutation from
  clearing its pending marker as though persistence had failed. Genuine partial success — write
  committed, read-back failed — still keeps the last state actually read rather than guessing, which
  Part 2B accepted for the identical study-progress case and which leaves a brief window where the
  learner could request the opposite operation. Saving is idempotent by primary key; an unsave of an
  already-removed identity and a re-save of a still-saved one are both repository-level questions
  recorded for Part 3C. Saved ordering is the repository's own and is never re-sorted here.
- **`SavedQuestionsViewModel`:** the separation is correct and preserved — the holder owns identity,
  ordering and pending state, the ViewModel owns only content resolution. `CQ-STATE-009` fixes the
  one defect: a superseded resolution no longer publishes its own cancellation as `Error`.
  `resolvedFor` is sound: `SavedQuestion` equality is identity plus saved timestamp, the repository
  contract preserves that timestamp across a repeated save, and curriculum content is
  publisher-owned and static for the process, so an equal saved list cannot represent changed
  content. Pending-only emissions update actions without re-resolving; any emission that changes the
  saved list re-resolves, so a completed resolution always carries the pending IDs of the emission
  it was started from. Empty cancels resolution and sets `resolvedFor = emptyList()`, and with
  cancellation now silent a late job cannot repopulate Content. Retry deliberately re-runs
  resolution as well as refreshing the holder, for the `StateFlow` equality reason its comment
  states; that explicit re-run was kept.
- **Error versus existing content:** keeping stale Content when a new saved list fails to resolve
  was considered and rejected. Saved identity is this screen's primary data, so showing items
  resolved from a list the learner no longer has would offer Unsave against identities the screen
  can no longer describe. `Error` with Retry remains correct.
- **Cross-screen and refresh fan-out:** saving on a result surface reaches Mixed Result, Mistake
  Review and the Saved Questions list through the one holder, and the existing
  `SavedQuestionLifecycleIntegrationTest` and `SavedQuestionCaptureIntegrationTest` journeys prove
  it; `aQuestionSavedOnAnotherSurfaceAppearsAtItsRepositoryPosition` covers the live case. Progress
  Retry invalidating shared history is intended, because Retry means the shared read may have
  failed; saved Retry refreshing global saved state is intended for the same reason. Completing an
  assessment updates the dashboard without leaving and re-entering Progress, which
  `ProgressLearningJourneyIntegrationTest` exercises.
- **Duplicate and retained state:** no competing mutable cache of saved IDs, pending IDs, progress
  snapshots or unresolved counts exists; every consumer republishes a shared `StateFlow`. Neither
  app-scoped holder retains screen-specific ephemeral state — no scroll position, expansion or
  dialog state — so no finding was opened. `SharingStarted.Eagerly` was kept on both history-derived
  holders: the upstream is an invalidation signal rather than a live subscription, so eager sharing
  costs a derivation per completed assessment and buys warm state on every return.
- **Repeated derivation:** `ProgressStateHolder`, `MistakeReviewStateHolder`, `InterviewHistoryStateHolder`
  and the shell badge each derive from the same history emission. Their outputs differ materially —
  occurrence-based aggregation, latest-occurrence resolution, interview records, a count — and
  merging them would produce exactly the broad shared learner-state model this audit defers. No
  measurement-level cost was established; recorded as a Part-5 observation only.
- **Cancellation sweep:** four boundaries in this scope wrapped suspend work in `runCatching`,
  `fold`, `getOrElse` or a broad catch. Three are fixed by `CQ-STATE-010` and one by `CQ-STATE-009`.
  `ProgressTopicViewModel` already rethrew cancellation and was left alone. The `runCatching` in
  `SavedQuestionsDestination` wraps a synchronous `openUri` and is outside state ownership, matching
  the Part-2B and Part-2C disposition. `ProgressTopicViewModel`, `LearningUnitViewModel` and
  `LearningLessonViewModel` catch `Throwable` rather than `Exception` after rethrowing cancellation;
  Part 2B reviewed the latter two and accepted them, so no finding was opened here for the breadth
  difference alone.
- **Fixes and tests:** fixed `CQ-STATE-001`, `CQ-STATE-008`, `CQ-STATE-009` and `CQ-STATE-010` across
  six production files. Five test files changed. Added: two deterministic saved-state ordering tests
  (an older refresh settling after a write; a save read-back sampled before a different Question's
  removal); two saved-state cancellation regressions; a superseded-resolution test that records
  every published state and asserts no `Error` appears; and a retry-after-derivation-failure test
  for each of Progress and Mistake Review. `FakeSavedQuestionRepository` gained the `readGate` and
  snapshot-on-entry `staleReadGate` already established by `FakeLessonStudyRepository`, and the
  saved-questions ViewModel test curriculum gained a lookup gate. Every new test was confirmed to
  fail against the unfixed production code and pass with the fix.
- **Part-3 handoffs:** Part 3C retains `SavedQuestionRepository` idempotency for the write the
  learner may repeat after a failed read-back, saved-order stability under concurrent writes, and
  transaction boundaries around save/unsave. Part 3B retains the persisted-attempt invariants that
  `TestAttempt.init` currently enforces at construction. `CQ-STATE-011`'s duplicate history query is
  recorded for Part 3C.
- **Part-4 handoffs:** none opened. Both holders are Koin singles on the process-lifetime
  `AppCoroutineScope`; Part 4A retains full graph and teardown ownership, and no Koin definition was
  changed.
- **Part-5 handoffs:** repeated app-scoped derivation from one history emission across four
  consumers, and the structural similarity between `StudyProgressStateHolder` and
  `SavedQuestionStateHolder`. Deliberately not abstracted here: the two holders now share a
  refresh/mutation/read-back shape, but their keys, mutation semantics, ordering contracts and
  failure behavior differ, and a `GenericRepositoryStateHolder<T>` would encode none of that. Part 5
  may reassess now that both are fully understood.
- **Cross-chunk note on `CQ-STATE-008`:** the defect was found in `ProgressStateHolder`, which is
  Part 2D's, and the identical defect exists in `MistakeReviewStateHolder`, which Part 2C reviewed
  before the pattern was understood. Both were fixed together rather than leaving two sibling
  holders inconsistent with a known Medium defect open in one of them. Part 2C was not re-audited;
  only this one defect was applied to its files. The root cause is a consumer-side consequence of
  `AssessmentHistoryStore`'s value-equality emission contract, which was deliberately left
  unchanged, so each consumer owns its own re-derivation trigger.
- **Validation:** the five requested targeted commands passed, plus
  `*MistakeReviewViewModelTest*`, `*SavedQuestionLifecycleIntegrationTest*` and
  `*ProgressLearningJourneyIntegrationTest*`. `./gradlew :shared:jvmTest` passed.
  `./gradlew :shared:check` passed as the Part-2 closure check, running the JVM, JS browser, Wasm
  browser and iOS simulator test tasks. `git diff --check` passed. The documented Kotlin
  expect/actual Beta warning and the existing Compose `runComposeUiTest` deprecation warnings
  appeared and are unchanged. Android assembly, lint and device/browser UI checks were not run
  because no platform source, Compose code, navigation route, layout or theme token changed.

## Part 2 Presentation/State Synthesis

- **Findings:** 13 unique findings across Parts 2A-2D, all fixed, plus one observation accepted
  as-is. Severity totals are Critical 0, High 1, Medium 3, Low 9, Observation 1. Nothing remains
  Deferred, Needs measurement or Not a defect.
- **Cancellation:** this was the single largest defect class in Part 2. Broad suspend `runCatching`
  /`fold`/`getOrElse` misuse was systematic rather than incidental: it appeared in startup, the
  shell badge, Topic Browser, Topic Detail, study progress, assessment taking, both result owners,
  retakes, the history store, the mistake queue, optional Lesson enrichment, the progress
  derivation, saved-state reads and mutations, and saved-content resolution. `CQ-BUG-002`,
  `CQ-STATE-002`, `CQ-STATE-003`, `CQ-STATE-004`, `CQ-STATE-005`, `CQ-STATE-006`, `CQ-STATE-009` and
  `CQ-STATE-010` corrected it. The resulting convention: a suspend fallback rethrows
  `CancellationException` before applying its ordinary-failure behavior, whether that behavior is a
  retryable `Error`, a degraded optional enrichment, a zero fallback, or leaving prior state alone.
  Cancellation means the owner is going away; it is never evidence about data. Synchronous
  `runCatching` around a non-suspending call such as `openUri` is outside this rule and was left
  alone. `CQ-STATE-009` is the one place where the misuse was not merely contract-level: a replaced
  content resolution published a full-screen `Error` over a readable list.
- **Stale results and races:** four verified problems. `CQ-STATE-007` — retry could start
  overlapping loads in three owners before the UI recomposed out of `Error`. `CQ-STATE-001` — a
  saved-question mutation's read-back was unordered against every other read, so an older snapshot
  could restore a Question another mutation had removed. `CQ-STATE-008` — a retry over unchanged
  history could not re-run a failed derivation, because a `StateFlow` does not re-emit an equal
  value. `CQ-STATE-009` — a superseded resolution published over the one that replaced it.
  Reviewed and found correct: generation checks in Topic Browser and Topic Detail; Practice Builder
  target/availability replacement; the history store's two-generation one-shot read; sequential
  `map` derivation in all three history-derived holders; and `ProgressTopicViewModel`'s
  cancel-and-rethrow retry, where cancellation alone is sufficient and a generation token is not.
- **One-shot events:** `CQ-BUG-003` was the pass's only High finding. Launch and both retake owners
  restored `Idle` before the buffered navigation event was consumed, leaving a real window in which
  a second durable attempt could be created. Creation now stays locked in an identity-bearing
  pending-navigation state until the single collector acknowledges that same identity. Buffered
  one-producer/one-collector Channels owned by a navigation entry were otherwise found appropriate.
- **State ownership:** the pass moved ownership exactly once, in `CQ-BUG-001`, from a remembered
  composition value to the app-scoped initializer that already had the required lifetime. Every
  other ownership question resolved to "already correct". App-scoped holders are justified where
  several surfaces present one learner-owned truth or where a destroyed navigation entry would
  otherwise reload it: completed history, study progress, the mistake queue, the progress dashboard,
  saved identities, and appearance. Screen-specific state stayed in ViewModels, and no app-scoped
  holder was found retaining ephemeral screen state.
- **Runtime parameters:** no ID mix-up was found. Routes are data-class Navigation 3 keys with
  per-entry ViewModel stores; `parametersOf`, Koin definitions and constructors agree on order; the
  same-type Lesson pair uses explicit indexed reads with containment tests. Blank IDs are rejected at
  construction. No inline ID wrapper types were justified.
- **Duplicate state:** none found. Every shared projection is republished from one `StateFlow`
  rather than copied — saved identities across four surfaces, study progress across four Learn
  owners, history across five consumers.
- **Deferred to persistence (Parts 3B/3C):** commit-then-throw and commit-then-cancel outcomes for
  start, retake, submission and completion; repository idempotency and transaction boundaries for
  assessments and saved questions; malformed IN_PROGRESS answer-prefix invariants; lesson-study
  atomicity and external-writer assumptions; saved-order stability; and the duplicate history query
  behind `CQ-STATE-011`.
- **Deferred to DI lifetime (Part 4A):** full Koin graph ownership and process-scope teardown for
  the app-scoped holders and `AppCoroutineScope`. No Part-4 finding was opened in any Part-2 chunk
  and no Koin definition was changed.
- **Deferred cross-cutting (Part 5):** repeated derivation from one history emission across four
  app-scoped consumers; the structural similarity between `StudyProgressStateHolder` and
  `SavedQuestionStateHolder`; and the parallel focused/mixed result owners. No `CQ-CROSS` finding was
  justified: in each case the shared shape covers materially different semantics, and abstracting it
  now would trade explicit correct code for a generic holder that encodes none of the differences.

### Final state-ownership conclusions

| State | Owner | Lifetime |
| --- | --- | --- |
| Startup/application readiness | `CurriculumDataInitializer` for durable completion; `AppStartupStateHolder` for transient loading/error/retry | Application graph; composition |
| Appearance preference | `AppearanceStateHolder` | Application (Koin single) |
| Navigation state | `AppNavigator` | Compose saveable |
| Completed assessment history | `AssessmentHistoryStore` | Application (`AppCoroutineScope`) |
| Study progress | `StudyProgressStateHolder` | Application (`AppCoroutineScope`) |
| Mistake queue | `MistakeReviewStateHolder` | Application (`AppCoroutineScope`) |
| Progress dashboard | `ProgressStateHolder` | Application (`AppCoroutineScope`) |
| Saved-question identities | `SavedQuestionStateHolder` | Application (`AppCoroutineScope`) |
| Screen-specific state | The feature ViewModel | Navigation entry |

## Part 3A Review Record

- **Assigned production boundary (40 files):** every Kotlin file under
  `curriculum/**` — the ten assessment domain models plus `content/BundledCurriculumSource`,
  `repository/CurriculumRepository`, `serialization/CurriculumJsonCodec`, the four files under
  `validation/`, the nine learning models under `learning/`, and the seven learning
  content/repository/serialization/validation files — together with every Kotlin file under
  `data/local/curriculum/**`: `CurriculumDao`, `CurriculumDatabase`, `CurriculumDataInitializer`,
  `CurriculumDataModule`, `CurriculumMigrations`, the six entities, the four importer files, and
  `LocalCurriculumRepository` with `CurriculumEntityMapper`. The two bundled JSON resources and the
  exported Room schemas were read as data, not audited as content.
- **Supporting contracts inspected (6):** the exported schema `8.json` for the real foreign-key
  declarations, `.github/workflows/main.yml` for what CI actually enforces,
  `tools/learning_question_coverage.py` for the authoring-time guarantee, the four platform
  `CurriculumDatabase` actuals only far enough to establish that every host uses a bundled SQLite
  build, and the `correctAnswerIds` consumers in `AssessmentEngine`, `AssessmentReviewLoader` and
  `SavedQuestionContentResolver` to establish that the answer key is read as a set. Platform
  database builders remain Part 4C; attempt persistence remains Part 3B.
- **Relevant tests reviewed (14 files):** `CurriculumImporterTest`, `LocalCurriculumRepositoryTest`,
  `CurriculumDatabaseTest`, `CurriculumDatabaseMigrationTest`, `CurriculumLocalDataPathTest`,
  `QuestionLevelEndToEndTest`, `CurriculumValidatorTest`, `CurriculumJsonCodecTest`,
  `CurriculumModelTest`, `InitialCurriculumSmokeTest`, `InitialCurriculumContentQualityTest`,
  `LearningContentLoaderTest`, `BundledLearningContentRepositoryTest`,
  `LearningCurriculumValidatorTest`, plus `LearningCurriculumJsonCodecTest`,
  `LearningContentEndToEndTest` and `BundledLearningCurriculumTest` for the bundled-document
  guarantees.

### Curriculum evolution policy

The policy is documented rather than inferred, and the two pipelines deliberately differ, so
**Option C** is the answer to the omitted-versus-DEPRECATED question.

For the assessment curriculum, `docs/architecture/persistence.md` states that "absence from a later
bundle is not a deletion signal" and that "curriculum retirement must be explicit through
`ContentStatus.DEPRECATED`", `docs/content/content-authoring.md` requires a materially rewritten
Question to keep the old one as `DEPRECATED` under its old ID, `CurriculumImporterTest`
pins it in `absenceFromLaterCurriculumIsNotADeletionSignal`, and the shipped bank practises it:
41 of its 478 Questions are authored `DEPRECATED`. Stale Topic, Subtopic and Question rows are
therefore intentional, not a reconciliation bug. The one operation that *is* reconciled is the
answer-option set of an incoming Question, because an option is the only curriculum row a
historical attempt points a foreign key at.

For learning content, `docs/architecture/study-progress.md` states the opposite and states it
explicitly: a studied-Lesson record whose Lesson "no longer resolves at all" is retained, excluded
from progress, and ignored without error. Nothing persists the learning document, so omission
costs only resolvability. That asymmetry is a consequence of storage, not an inconsistency.

### Import and reconciliation findings

`CQ-BUG-004` is the pass's one High finding and the only reconciliation defect found. Immediate
foreign keys plus a fixed write order meant that re-homing a Subtopic to a different Topic — a
taxonomy edit the validator accepts, that a fresh install imports without complaint, and that moves
its Questions in the same bundle — aborted the entire import on any existing installation, leaving
an upgrading user on a startup error screen whose Retry could never succeed. It is fixed inside the
existing schema with `PRAGMA defer_foreign_keys = ON`, and two regression tests cover both
directions: the valid re-home now imports, and an import that would genuinely orphan a persisted
row still fails whole and leaves the previous curriculum importable.

Everything else in the reconciliation surface was verified correct:

| Scenario | Result |
| --- | --- |
| New Topic / Subtopic / Question | Inserted; unrelated rows untouched |
| Renamed Topic / Subtopic / Question under a stable ID | Updated in place, no duplicate identity |
| Status changed ACTIVE -> DEPRECATED | Updated; row retained and still resolvable by ID |
| Topic / Subtopic / Question omitted from a later bundle | Row retained, status untouched — the documented contract, not a defect |
| Subtopic re-homed to another Topic | **Was a whole-import abort on upgrade; fixed by `CQ-BUG-004`** |
| AnswerOption removed, never selected | Deleted |
| AnswerOption removed, historically selected | Retained, marked `DEPRECATED`, excluded from active queries, still returned by `getQuestionById` |
| AnswerOption re-authored after retirement | Reactivated, because the mapper upserts every authored option as ACTIVE |
| Correct-answer set changed | Rows for incoming Questions deleted and rewritten in the same transaction, after the new options exist and before stale options are removed |
| Source list changed | Rows for incoming Questions deleted and rewritten; authored order preserved through `sort_order` |

Transactionality is sound: decode, validate and snapshot mapping all complete before
`withWriteTransaction` opens, and every write and every reconciliation statement is inside it. No
database mutation happens before validation finishes. Import is idempotent —
`realBundledCurriculumImportIsIdempotent` compares all six row counts and a specific Question
across two runs — and a failed import leaves the previous curriculum readable, which
`invalidCurriculumDoesNotMutateExistingData`,
`malformedSerializedContentPropagatesAndDoesNotMutateDatabase` and the new commit-time test each
prove from a different failure point.

The `if (incomingQuestionIds.isNotEmpty())` guard is genuinely dead: `CurriculumValidator` emits
`EMPTY_QUESTIONS` and returns before the importer could ever be reached with an empty list. It is
left alone as a cheap local invariant rather than flagged.

`CurriculumImportResult.Rejected` for validation errors while resource, decode and database
failures throw is a coherent split and is left as-is: `CurriculumDataInitializer` is the only
caller, a rejected bundle is an authoring fault it turns into a startup error with the individual
messages attached, and a decode or write failure is not something a caller could branch on
differently. Nothing needs a wider sealed type.

`CurriculumDataInitializer`'s data contract was checked without redoing Part 2A's lifecycle work:
`initialized` becomes true only after `Imported`, a `Rejected` or thrown import leaves it false, a
retry therefore runs the full import again, and that repeat is safe because the import is
idempotent.

### Historical-content findings

Historical review resolves **current authored content attached to historical answer state**, and
that is deliberate. `docs/architecture/persistence.md` states that attempts "reference stable
curriculum IDs rather than copying question text, answer text, explanations, or sources", and
`LearningProgressService` documents the same for a corrected answer key. A publisher may therefore
change a Question's text, explanation, sources or correct-answer designation under a stable ID and
a past attempt will be reviewed against the new content; the authoring contract answers that by
requiring a new ID whenever the change is material. No snapshotting architecture was designed in
this pass, and none is needed to make the current behavior correct — only documented, which it is.

Historical foreign-key integrity was traced far enough to reason about reconciliation and no
further. `question_attempt` references `question(id)` and `question_attempt_selected_answer`
references `answer_option(question_id, id)`, both `NO ACTION`. Because the importer never deletes a
Topic, Subtopic or Question, the only deletion that can collide with history is a stale answer
option, and `deleteAnswerOptionsForQuestionExcept` guards it with a `NOT EXISTS` subquery over the
selected-answer table. `test_attempt` stores focused scope IDs as plain columns with no foreign
key, so a historical Focused scope stays labellable for exactly as long as its Topic or Subtopic
row survives — which, under the never-delete policy, is indefinitely.

### Repository-query findings

No N+1 behavior exists. `LocalCurriculumRepository.toDomainQuestions` issues exactly three batch
queries for any result set — options, correct answers, sources — inside one read transaction, and
groups all three by question ID. Composite answer identity is respected everywhere: the hydration
groups by `questionId` rather than `associateBy { it.id }`, the correct-answer join carries the
owning question ID, and the stale-option delete is scoped per question for the same reason. No
accidental global answer map was found.

ACTIVE filtering is correct on every variant, not just one. All six active-question queries join
both `topic` and `subtopic` and require all three statuses, and `getActiveSubtopicsForTopic` joins
its parent Topic; `LocalCurriculumRepositoryTest` exercises the full matrix including
level-filtered variants. The empty level set is resolved in Kotlin before it can reach `IN ()`,
which is a SQLite extension rather than portable SQL, and is tested on every scope. The five
historical resolvers — `getTopicById`, `getSubtopicById`, `getQuestionById`, `getUnitById`,
`getLessonById` — correctly ignore status, and `getQuestionById` is the only read that hydrates
retired answer options.

Ordering is intentional and protected throughout: Topics, Subtopics and Questions by the
`sort_order` the mapper derives from authored list position, answer options and sources by their
own `sort_order`, learning Units and Lessons by list position with nothing sorting them. Because
`sort_order` is an index, ties and negative values are impossible by construction, so no uniqueness
rule is warranted. Correct-answer IDs come back ordered by answer ID rather than authored order,
which is deterministic and harmless: every consumer reads the answer key as a set.

The repository API exposes no storage detail — no entity, no status string, no DAO type crosses
it — and the several level and scope combinations are justified, because each pushes its filter
into SQL instead of loading the whole active bank to narrow it in memory.

### Validation findings

`CurriculumValidator` covers every invariant the schema, the queries, selection, grading and review
actually need: unique Topic, Subtopic and Question IDs, answer IDs unique within a Question, the
Topic/Subtopic hierarchy and its cross-check, correct answers that exist and are not duplicated,
`SINGLE` not carrying several correct answers, a minimum of two answers, non-empty required fields,
sources with a syntactically valid URL, and no authoring placeholder anywhere. `CQ-DATA-002` closed
the one gap found — a repeated source URL within a Question, which the `(question_id, url)` primary
key silently collapsed. Parent/child status combinations are deliberately permitted and harmless
inside the assessment pipeline, because active queries join parent statuses; `CQ-DATA-003` records
the one place that reasoning does not carry across to learning content.

`LearningCurriculumValidator` validates identity, hierarchy, references and content shape,
including the two rules that encode product decisions — cross-Topic concepts are valid by design,
and minimum-content requirements apply to ACTIVE content only. Lesson IDs are compared across every
Unit, which is what makes `LearningContentRepository.getLessonById` a legal global lookup. Error
messages on both validators name the rule, the entity and enough context to fix the content; none
was rewritten for style.

Cross-document references are validated at runtime, in the one place that has both documents:
`LearningContentLoader` validates the learning document against the bundled `Curriculum` rather
than the imported Room copy, so an unknown home Topic or Subtopic fails the load loudly.
`learning_question_coverage.py` re-checks the same references in CI before anything is built, and
its `--check` mode also fails a stale coverage snapshot. `CQ-DATA-005` records that overlap as
intentional rather than duplicated.

### Serialization findings

Both codecs use strict `Json` defaults, so an unknown field, a missing required field, a malformed
document or an unrecognised enum constant all fail decoding rather than producing a quietly wrong
object; `CurriculumJsonCodecTest` covers each of those cases explicitly. That strictness is the
right choice for content shipped in the same binary as its reader, and was not changed.
`LearningCurriculumJsonCodec` additionally pins `type` as the block discriminator so the authored
JSON never depends on generated class names.

Enums are persisted and serialized as `.name`, which makes the database and the authored JSON
readable and is documented as deliberate. The consequence is that renaming a constant is a data
migration: `CurriculumEntityMapper` uses `valueOf` and would throw on an unrecognised persisted
value. `MIGRATION_3_4` and `MIGRATION_4_5` already show the pattern for introducing one. No format
was redesigned.

A missing or unreadable bundled resource is deliberately *not* translated into content failure:
`BundledCurriculumSource` lets it propagate, and `LearningContentLoader` catches only
`SerializationException` so a packaging fault does not send whoever reads the failure to the wrong
file. Neither pipeline can degrade an invalid bundle into an empty curriculum.

### Learning-content pipeline findings

The cache is a per-instance double-checked `Mutex` over one validated document, and the repository
is a Koin `single`, so the document is decoded and validated once per process. Concurrent first
callers wait on the same load and receive the same document; the cached field is assigned only
after `LoadedLearningContent` has been constructed from a fully validated curriculum, so no partial
index can be observed, and `theDocumentIsLoadedOnceAcrossRepeatedQueries` pins the single load. A
failed load caches nothing and every later call retries, which
`aFailedLoadStaysAFailureAndCachesNothing` proves across all four query functions. Cancellation
stays cancellation: nothing in the loader, the repository or the importer catches broadly,
`withLock` releases on cancellation, and a cancelled first load leaves the cache empty for the
next caller. `CQ-DATA-004` records why the
non-volatile field read is nevertheless safe. No invalidation mechanism is warranted, because the
bundled bytes cannot change while the process lives.

### Duplication findings

The two pipelines share the shape *load bundled resource, decode, validate, expose typed failure*,
and share their primitive authoring checks through `AuthoredContentChecks`, which already extracts
the one genuinely common concept. They are not otherwise duplicated: one persists into a relational
schema with reconciliation and a historical resolver, the other keeps an immutable in-memory
document; one reports rejection as a result type because its caller is a startup initializer, the
other throws because its callers return domain models. The mapping code is likewise single-purpose
— `CurriculumPersistenceMapper` writes, `CurriculumEntityMapper` reads, and they map different
shapes rather than two copies of one. No `CQ-CROSS` finding is justified, and no generic content
framework is proposed. Part 5 may revisit the shared load/decode/validate skeleton with the rest of
the repository in view.

### Fixes

- `CQ-BUG-004`: `CurriculumImporter` defers foreign keys to `COMMIT`; two regression tests in
  `CurriculumImporterTest`; the behavior is documented in `docs/architecture/persistence.md`.
- `CQ-DATA-002`: `DUPLICATE_SOURCE_URL` added to `CurriculumValidationErrorCode` and enforced in
  `CurriculumValidator`; two tests in `CurriculumValidatorTest`; the rule is listed in
  `docs/content/content-authoring.md`.
- `CQ-DATA-001`: the stale `CurriculumDao` contract comment now describes the deprecate-and-filter
  behavior that exists. No production behavior changed.

No Room schema change was made, no migration was added, no bundled curriculum or learning content
was edited, and no Koin, platform or attempt-persistence code was touched.

### Handoffs

- **Part 3B — assessment sessions, attempts and history persistence:** `CQ-BUG-004` changed when
  foreign keys are checked inside the *import* transaction only. Attempt-writing transactions were
  not inspected and keep per-statement enforcement; whether any of them rewrites a graph with the
  same intermediate-inconsistency shape is a Part-3B question. The `NO ACTION` references from
  `question_attempt` and `question_attempt_selected_answer` onto curriculum rows were traced only
  as deletion restrictions on the importer.
- **Part 3C — learner-owned state:** `saved_question` and `studied_lesson` intentionally hold no
  foreign key to publisher content, and both survive a Question or Lesson that stops resolving.
  This pass confirmed the curriculum side of that assumption — rows are never deleted, so a saved
  Question always resolves; a removed Lesson does not, and the study-progress contract already
  says it is ignored. Whether an accumulating unresolvable record deserves any surfaced treatment
  is a Part-3C question.
- **Part 4C — platform capability implementations:** every host builds on a bundled SQLite
  distribution, so the `IN (:ids)` batches this pipeline issues — 478 bound question IDs today —
  sit far below the 32 766 variable ceiling of modern SQLite. Whether that holds for the web
  worker build is Part 4C's to confirm.
- **Part 5 — cross-cutting:** the shared *load, decode, validate, expose typed failure* skeleton
  across the two content pipelines, recorded above as not worth extracting on its own evidence.
- **Content decision, feeding `CQ-DATA-003`:** whether retiring an assessment Topic or Subtopic is
  meant to retire the learning Units and Lessons that name it.

### Validation

- `./gradlew :shared:jvmTest --tests '*CurriculumImporterTest*'` — 22 tests, all passing, including
  the two new regressions.
- `./gradlew :shared:jvmTest --tests '*CurriculumValidatorTest*'` — passing with the two new
  source-duplication tests.
- `./gradlew :shared:jvmTest` — passing.
- `./gradlew :shared:check` — passing. JVM, Android host, JS, Wasm and iOS simulator test tasks all
  executed; no asset-size, bundle-ID or duplicate KLIB warning was observed.
- `python3 -m unittest discover -s tools -p 'test_*.py'` — 21 tests, passing.
- `python3 tools/learning_question_coverage.py --check` — snapshot current.
- `git diff --check` — clean.
- Room schema: **unchanged**. `shared/schemas` is untouched, no migration was added, and no
  migration test was required; `CurriculumDatabaseMigrationTest` was read but not modified.
- Not run: Android lint and the application-shell assemble tasks, because no Android-specific or
  host source changed and `:shared:check` already compiled every target.

## Part 3B Review Record

- **Assigned production boundary (22 files):** the eight assessment domain models
  (`TestAttempt`, `QuestionAttempt`, `QuestionAnswerState`, `AssessmentScore`, `AssessmentStatus`,
  `AssessmentConfig`, `AssessmentScope`, `PracticeQuestionSource`); the session package
  (`AssessmentEngine`, `AssessmentSession`, `AssessmentSessionLoader`, `AssessmentStartResult`,
  `AnswerOrder`); `AssessmentRepository`; `StartAssessment`; `AssessmentRetakeService` with
  `AssessmentRetakeResult`; `AssessmentReviewLoader`; and every file under
  `data/local/assessment/**` — `AssessmentAttemptDao`, `AssessmentAttemptMapper`,
  `AssessmentAttemptStore`, `AssessmentDataModule`, `LocalAssessmentRepository`, and the three
  entities. `CurriculumMigrations` and the exported schema `8.json` were read for the assessment
  tables only.
- **Supporting contracts inspected (6):** `AnswerSelectionMode` and `CurriculumValidator`'s
  selection-mode rule to establish the authored arity contract; `AssessmentTakingViewModel`,
  `AssessmentHistoryStore`, `MistakeReviewService` and `LearningProgressService` only far enough to
  enumerate writers and history consumers. ViewModel concurrency was not re-reviewed; Koin
  lifetime, platform database builders and learner-owned persistence were not audited.
- **Database entities and DAOs reviewed:** `TestAttemptEntity`, `QuestionAttemptEntity`,
  `QuestionAttemptSelectedAnswerEntity`, and all thirteen `AssessmentAttemptDao` methods.
- **Migrations reviewed:** `MIGRATION_1_2` (creates the three attempt tables) and `MIGRATION_5_6`
  (adds `practice_levels` and `practice_source`). `MIGRATION_2_3` was read only for its effect on
  the answer options historical attempts point at. Curriculum-only migrations were not re-audited.
- **Tests reviewed (12 files):** `AssessmentAttemptStoreTest`, `LocalAssessmentRepositoryTest`,
  `CurriculumDatabaseMigrationTest`, `AssessmentEngineTest`, `AssessmentEngineIntegrationTest`,
  `AssessmentSessionLoaderTest`, `AssessmentModelTest`, `AnswerOrderTest`,
  `AssessmentRetakeServiceTest`, `AssessmentReviewLoaderTest`,
  `LearningAnalyticsCurriculumEvolutionIntegrationTest`, and the answer-option retirement tests in
  `CurriculumImporterTest`.
- **Existing findings:** no ledger finding was deferred to Part 3B, so none required disposition.
  Part 3A's foreign-key handoff is resolved below. New `CQ-BUG-005` and `CQ-DATA-006` are fixed;
  `CQ-DATA-007` and `CQ-DATA-008` are accepted as-is. Counts for this pass: Critical 0, High 0,
  Medium 1, Low 1, Observation 2; Fixed 2, Deferred 0, Needs measurement 0, Accepted as-is 2, Not a
  defect 0.

### Aggregate persistence

`TestAttempt` round-trips as one aggregate, not as individually valid rows. Every stored dimension
survives: identity, config, ordered occurrences, per-occurrence answer state and selected IDs,
status, both timestamps and the score. Reconstruction runs through the same domain constructors an
in-memory attempt does, so a row set that cannot form a valid aggregate cannot become one.

### Save transactionality and repeated saves

`AssessmentAttemptStore.save` maps the whole aggregate to a snapshot *before* opening a transaction,
then inside one `withWriteTransaction` upserts the attempt row, deletes that attempt's selected
answers, deletes its question occurrences, and writes the snapshot's occurrences and selected
answers. Delete-then-insert rather than upsert-only is the important detail: a child collection that
shrinks or changes leaves nothing stale behind, and the delete order satisfies the immediate foreign
keys, so Part 3A's deferred-foreign-key handoff does not apply here — the attempt write sequence
never presents an intermediate inconsistency to `COMMIT`. Saving the same aggregate twice is
idempotent. A new regression test,
`aFailedSaveLeavesThePreviouslyCommittedSnapshotIntact`, drives a save that fails on its last
statement and proves the previously committed aggregate and its children are untouched.

Repeated saves of one attempt were already covered by
`savingUpdatedAttemptReplacesAttemptOwnedSnapshotOnly`, which replaces a selected set
(`{question_a_b}` -> `{question_a_a}`) and asserts the row counts as well as the reconstructed
aggregate. `save` therefore means authoritative-snapshot replacement, never monotonic append, and
that is now stated on the interface rather than inferred. Question membership is reconciled by the
same mechanism even though no production path changes it after creation.

### Identity, ordering and answer order

`(test_attempt_id, question_id)` is the occurrence key, which matches the domain rule that a
Question appears at most once in an attempt; no redesign is warranted. Question order is semantic
and explicit — `sort_order` is written from list position and every read orders by it — while
selected answers and the config sets are reconstructed as `Set`s with no order dependence. Answer
order is deliberately not persisted: `withAnswersOrderedFor` derives it from the attempt and
Question IDs, so resume and review agree by construction, which
`resumingAnAttemptAndReviewingItShowTheSameAnswerOrder` pins. Persisting positions would be a
migration for a value that can be recomputed.

### Config and scope round-trip

`Mixed`, `Focused` with `Topic`, `Subtopic` and `Subtopics` scopes, narrowed and full level sets,
and every `PracticeQuestionSource` all round-trip exactly. `Subtopics` sorts its IDs before JSON
encoding so equal scopes store identically, and levels are written in authored enum order through
`inAuthoredOrder()` for the same reason; both are canonicalisation, not semantic order. An empty
level set round-trips as the empty string and back, and selection refuses such a request before any
attempt is created, so it is representable but unreachable in stored history. Retake reads the
reconstructed config, which is why this fidelity matters, and
`retakeCreatesSeparatePersistedAttemptAndPreservesOriginal` asserts config equality across the
retake.

### Enums, discriminators and corruption

`AssessmentStatus`, `PracticeQuestionSource` and `QuestionLevel` persist as `.name`; the config and
scope discriminators are local constants; the completed-history query receives
`AssessmentStatus.COMPLETED.name` rather than a duplicated `"COMPLETED"` literal. Every unknown
value fails loudly — `valueOf` raises, unknown discriminators `error(...)`, a malformed
multi-Subtopic payload is rethrown as an explicit `IllegalStateException` — and the mapper's
`require` calls reject score fields, timestamps and selected-answer presence that contradict the
stored status. That is the intended policy and it is now documented: a corrupt row is not an absent
row, one corrupt attempt fails `getCompletedAttempts()` as a whole, and nothing is silently skipped
or repaired. Downgrade compatibility is deliberately unsupported.

### Historical correctness

Occurrence data is persisted, never recomputed. `question_attempt.is_correct` holds the correctness
recorded when the learner answered, the score columns hold the completed result, and
`AssessmentReviewLoader` combines those with current authored content resolved by stable ID.
`correctingTheCurrentAnswerKeyLeavesHistoricalCorrectnessAlone` already proved a re-authored answer
key does not rewrite history. A new end-to-end test,
`anAnswerOptionRetiredByALaterBundleStaysSelectedInHistoricalReview`, closes the 3A/3B seam: an
attempt selects an option, a later bundle replaces it, review still renders the retired option as
the learner's selection with the correctness they earned, and a new assessment is offered only the
current options.

`AssessmentSessionLoader` resumes an IN_PROGRESS attempt against unrestricted historical lookups, so
a DEPRECATED Question still resolves while being excluded from new selection, and a Question that no
longer resolves at all returns `MissingQuestion` rather than a silently shortened session. Both are
the intended policies and both are pinned by
`missingQuestionIsExplicitAndDeprecatedQuestionLoads`. Under Part 3A's never-delete import contract,
the removed-Question case is unreachable through ordinary publishing; the explicit failure state
exists for a database that lost the row some other way.

### Completed-history queries and read consistency

`getCompletedAttempts()` filters on status in SQL and orders by `completed_at DESC, started_at DESC,
id ASC`, so ties from a coarse clock stay deterministic — `completedHistoryUsesStartedTimeThenStableIdToBreakCompletionTies`
covers it. Hydration is three queries regardless of history size: the attempt rows, then occurrences
and selected answers batched by attempt ID and grouped in memory by
`(testAttemptId, questionId)` rather than by answer ID alone. There is no N+1 and no measurement is
needed. The `IN (:attemptIds)` batches sit far below the bundled SQLite variable ceiling for any
realistic accumulation, matching Part 3A's conclusion for the curriculum batches.

Both reads run inside `withReadTransaction`, so a concurrent save cannot produce a new parent beside
old children. The store contains no `catch` at all, so cancellation propagates rather than becoming
"not found" or "empty history".

### Writers and stale-overwrite risk

`AssessmentRepository.save` has exactly three production call sites: `StartAssessment` (once, under
a freshly generated UUID) and `AssessmentTakingViewModel` at lines 80 and 147 (submission and
completion, both on the attempt that screen owns). No attempt has two live owners, Part 2C already
proved submission and completion cannot overlap within that owner, and `AssessmentEngine` refuses to
submit to or re-complete a COMPLETED session. A status downgrade would therefore require a writer
the architecture does not have. The one-writer expectation is now written down instead of assumed,
and no optimistic locking or database guard was added for a race with no path to it.

### Domain session invariants

`AssessmentSession` enforces `questions.map { id } == attempt.questionAttempts.map { questionId }` in
the same order, so the engine's index-based occurrence update cannot answer the wrong Question; both
construction paths — `start` and the loader — satisfy it, and `sessionRequiresQuestionAndAttemptOrderAlignment`
pins it. MULTIPLE submissions normalise duplicates through `Set` conversion and require exact-set
equality for correctness, which was already correct. `CQ-BUG-005` is the one gap: SINGLE arity was
enforced by the Practice UI and by authoring validation but not by the engine between them, and is
now enforced in `submitAnswer`.

### Migrations

`MIGRATION_1_2` creates the three attempt tables with the identity and foreign keys the entities
declare. `MIGRATION_5_6` is a pure add of two nullable columns, deliberately not backfilled: a
pre-v6 FOCUSED row genuinely was an all-levels `ALL` run, and the mapper reconstructs exactly that,
so migrated history keeps the behavior it originally represented rather than claiming a selection
the learner was never offered. No schema change was needed in this pass. A new test,
`migratedAttemptsReconstructAsValidAggregatesThroughTheAssessmentStore`, takes a v5 database through
to v8 and then reads it through `AssessmentAttemptStore`, asserting the reconstructed aggregates for
both a completed attempt and an unfinished one — an app that upgrades mid-assessment must still be
able to resume it, and that case had only been covered at the row level.

### Part-2C handoff closure

The durability boundaries Part 2C deferred are now settled and documented. `save` commits or rolls
back as a whole, so there is no commit-then-throw state for start, submission or completion to
reason about: when the call returns normally the aggregate is durable, and everything after it —
history invalidation, navigation — is application synchronisation rather than persistence. Repeated
saves of one aggregate are idempotent, which makes a retry after an ambiguous failure safe.
Malformed IN_PROGRESS answer prefixes are not representable: the mapper rejects an occurrence whose
`is_correct` and selected-answer rows disagree, and resume simply selects the first unanswered
occurrence in persisted order.

### Fixes

- `CQ-BUG-005`: `AssessmentEngine.submitAnswer` enforces the SINGLE arity after its membership
  check; `severalAnswersForASingleSelectionQuestionAreRejected` and
  `repeatedIdsStillCountAsOneSelectionForASingleSelectionQuestion` replace the legacy
  `extraAnswerForSingleAnswerQuestionIsIncorrect`, whose exact-set semantics
  `multipleSelectionWithOneCorrectAnswerStillUsesExactSetScoring` already covers for MULTIPLE.
- `CQ-DATA-006`: `AssessmentRepository` now documents all three methods, and
  `docs/architecture/persistence.md` gains an "Attempt Save and Read Semantics" section covering
  transactional snapshot replacement, the one-writer expectation, read consistency, the corruption
  policy and persisted occurrence correctness. No production behavior changed.

No Room schema change was made, no migration was added, no entity or DAO was altered, no Koin or
platform code was touched, and no learner-owned persistence was modified.

### Handoffs

- **Part 3C — study progress, recommendations, saved questions:** `saved_question` and
  `studied_lesson` were not opened. The only cross-reference is that assessment history is the
  input to the recommendation and mistake surfaces, and this pass confirms that input is a single
  coherent newest-first snapshot per read.
- **Part 4C — platform capability implementations:** whether `withReadTransaction` and
  `withWriteTransaction` give the same isolation on the web worker driver as on the bundled SQLite
  hosts. The common code depends on it; only Part 4C can confirm the web build.
- **Part 5 — cross-cutting:** none opened. `AssessmentAttemptMapper` is the single canonical
  encoder and decoder for status, config, scope, levels and source, with no duplicated encoding
  anywhere, so there is nothing to generalise.

### Validation

- `./gradlew :shared:jvmTest --tests '*AssessmentEngineTest*'` — passing with the two replacement
  SINGLE-arity tests.
- `./gradlew :shared:jvmTest --tests '*AssessmentAttemptStoreTest*'` — passing with the new
  rollback regression.
- `./gradlew :shared:jvmTest --tests '*CurriculumDatabaseMigrationTest*'` — passing with the new
  migrated-aggregate round-trip.
- `./gradlew :shared:jvmTest --tests '*AssessmentEngineIntegrationTest*'` — passing with the new
  retired-answer review integration test.
- `./gradlew :shared:jvmTest` — passing.
- `./gradlew :shared:check` — passing. JVM, Android host, JS, Wasm and iOS simulator test tasks all
  executed; only the pre-existing expect/actual Beta warning appeared.
- `git diff --check` — clean.
- Room schema: **unchanged**. `shared/schemas` is untouched, no migration was added, and no
  migration was required.
- Not run: Android lint and the application-shell assemble tasks, because no platform source, Compose
  UI, navigation route, theme value or resource changed and `:shared:check` already compiled every
  target.

## Part 3C Review Record

- **Assigned production boundary (24 files):** the two learner-owned persistence stacks —
  `LessonStudyRepository`, `LocalLessonStudyRepository`, `StudiedLessonDao`, `StudiedLessonEntity`,
  `StudiedLesson`, `LessonStudyDataModule`, and the identical `SavedQuestionRepository`,
  `LocalSavedQuestionRepository`, `SavedQuestionDao`, `SavedQuestionEntity`, `SavedQuestion`,
  `SavedQuestionDataModule` — plus the derivation layer: `LearningProgressService`,
  `LearningPerformanceDerivation`, `LearningProgressPolicy`, `LearningProgressModels`,
  `RecentPerformancePolicy`, `QuestionExposure`, `StudyProgressService`, `StudyProgressDerivation`,
  `StudyProgressModels`, `ContinueLearningPolicy`, `ContinueLearningModels`, and the guided-learning
  set `LearningRecommendationResolver`, `LearningRecommendationPolicy`, `LearningRecommendationModels`,
  `ContinueStudyingResolver`, `ContinueStudyingModels`, `RecentStudyContextDerivation`.
- **Supporting contracts inspected, not re-audited (5):** `CurriculumRepository` and
  `AssessmentRepository` for their documented resolution and ordering guarantees, `MistakeReviewService`
  with `UnresolvedMistakeDerivation` as the shared latest-occurrence owner, `AssessmentReviewLoader`
  for what mistake review costs per unresolved candidate, and `TestAttempt` for the completed-attempt
  invariants every derivation here relies on.
- **Database objects reviewed:** `saved_question` and `studied_lesson` entities, `SavedQuestionDao`
  and `StudiedLessonDao`, exported schemas 7 and 8, and migrations `MIGRATION_6_7` and
  `MIGRATION_7_8`.
- **Tests inspected (14):** `LocalLessonStudyRepositoryTest`, `LocalSavedQuestionRepositoryTest`,
  `CurriculumDatabaseMigrationTest`, `LearningProgressServiceTest`,
  `LearningAnalyticsCurriculumEvolutionIntegrationTest`, `StudyProgressDerivationTest`,
  `StudyProgressServiceTest`, `StudyProgressStateHolderTest`, `ContinueLearningPolicyTest`,
  `ContinueStudyingResolverTest`, `LearningRecommendationPolicyTest`,
  `LearningRecommendationResolverTest`, `MistakeReviewServiceTest`, and
  `GuidedLearningPracticePresetIntegrationTest`.

### Learner-owned persistence

Both repositories are the same small shape and both satisfy the contract written on their interface.
`markStudied` and `save` are a single `@Insert(onConflict = IGNORE)` of an entity whose primary key is
the stable ID, so a repeated write is structurally a no-op and — the part that matters for ordering —
cannot move the stored timestamp. `unmarkStudied` and `unsave` are a delete by that ID, so a
re-creation afterwards is a genuinely new learner action and takes the clock's current value.
`isStudied` and `isSaved` are single-row primary-key lookups rather than a table scan, and
`getStudiedLessons` and `getSavedQuestions` order in SQL by `<timestamp> DESC, <stable id> ASC`, so
equal timestamps — which deterministic tests, a coarse clock and batch writes all produce — still
give one deterministic order rather than SQLite row order. Time is taken from an injected
`now: () -> Instant` defaulting to `Clock.System.now()`, stored as absolute epoch milliseconds with no
local-time interpretation, which is what lets every ordering and idempotency test above run on a
controlled clock without a sleep.

Neither repository wraps its work in a transaction, and neither needs one: every operation is a
single statement. Neither contains a `catch`, so an operational database failure propagates as a
failure instead of being reported as "not saved" or "not studied", and coroutine cancellation
propagates for the same reason. Neither imports `CurriculumRepository` or `LearningContentRepository`,
so the documented separation — persistence stores identity, derivation decides what current content
makes of it — is a property of the dependency graph rather than a rule to remember.

### Idempotency, ordering and duplicate writes

The existing suites already covered the sequential contract on both sides: repeated write preserving
the original timestamp, delete then re-add taking a new one, and an equal-timestamp pair ordered by
stable ID. What they did not cover is the same question under concurrency: `concurrentSavesForOneIdentityCreateOneRow`
and `concurrentMarksForOneLessonCreateOneRow` prove one row survives twenty overlapping writes, but
they run on a fixed clock and so say nothing about the timestamp. Two regressions were added —
`aConcurrentDuplicateSaveCannotReplaceTheOriginalTimestamp` and
`aConcurrentRepeatedMarkCannotReplaceTheOriginalRecordedTime` — which write once at one instant, then
race twenty duplicate writes at a later instant, and assert both the single row and the original
timestamp. Both were confirmed to fail when `OnConflictStrategy.IGNORE` is temporarily changed to
`REPLACE` in each DAO, alongside the two sequential tests, and to pass against the real
implementation; the DAOs were restored before any other validation was run. No application-level lock
was added: the primary key plus insert-ignore already enforces the documented behavior.

### Orphan identity and content evolution

Neither table carries a foreign key to publisher content, which `PRAGMA foreign_key_list` assertions in
the migration tests pin and the exported schemas confirm. A saved Question therefore behaves
identically whether its content is ACTIVE, DEPRECATED, or absent — the repository never looks — and
`savedIdentityResolvesActiveAndDeprecatedContentAndRetainsMissingContent` exercises all three. The
studied side has the same property and `aRecordForALessonNoLongerInTheBundleIsRetainedReadableAndRemovable`
covers it, including removal by stable ID alone with no `LearningContentRepository` present at all. No
repository-level pruning of unresolvable identities exists anywhere. Part 3A's open question — whether
an accumulating unresolvable record deserves surfaced treatment — remains a product question and not a
persistence defect; the current answer, that derivation ignores it and the learner can still remove it,
is coherent.

### Migrations

`MIGRATION_6_7` and `MIGRATION_7_8` are pure additive `CREATE TABLE IF NOT EXISTS` statements with no
backfill, which is right: absence of a row already means unsaved and unstudied, so an empty new table
is correct rather than incomplete, and seeding rows from current content would invent learner claims.
Nothing destructive appears on either path. `migrationFromSixToSevenAddsEmptySavedIdentityTableAndPreservesExistingRows`,
`migrationFromSevenToEightAddsEmptyStudiedLessonTableAndPreservesExistingRows` and the full
`migrationFromOneToEightPreservesCurriculumAndHistoricalAssessmentRows` together prove existing rows
survive, the new tables are usable immediately, and no foreign key was introduced.

### Historical evidence versus current content

The distinction the pass exists to protect holds throughout. `LearningProgressService` sums
`AssessmentScore` for overall totals and reads `QuestionAnswerState.Answered.isCorrect` for every
per-occurrence figure; nothing anywhere recomputes historical correctness from
`Question.correctAnswerIds`, and `persistedCorrectnessIsUsedEvenWhenAuthoredAnswersChanged` plus the
Part-3B review tests pin it. Coverage takes its denominator from `CurriculumRepository.getActiveQuestions()`
alone, so a retired Question keeps the accuracy the learner earned and leaves current coverage, which
`curriculumRetirementRemovesQuestionFromCoverageButNotFromAccuracy` and
`deprecatedAndMissingHistoricalQuestionsStayOutOfCurrentCoverage` both cover. The two aggregations are
deliberately different in shape — performance counts every occurrence, coverage counts each stable ID
once — and `repeatedQuestionCoversOnceButKeepsAccuracyOccurrenceBased` exists precisely so neither is
"simplified" into the other.

Completed-only semantics are enforced at each boundary rather than trusted from the caller:
`LearningProgressService` filters, `LearningPerformanceDerivation` filters again, `QuestionExposure`
filters, `UnresolvedMistakeDerivation` filters, and `toRecentStudyContext` refuses an IN_PROGRESS
attempt. Because `TestAttempt.init` requires every question attempt of a COMPLETED attempt to be
`Answered`, the `as QuestionAnswerState.Answered` casts in the derivations are reading an invariant the
aggregate already guarantees rather than assuming one, which is why no malformed-data branch was added.

`CQ-DATA-011` records the one thing that was true of the code but unwritten: an occurrence whose
Question no longer resolves at all stays in the overall totals and drops out of the Topic and Subtopic
breakdown, so grouped answered counts can sum to less than the overall count.
`missingQuestionKeepsPersistedOverallWhileDeprecatedAndMissingMetadataRemainScoped` already pinned the
behavior; `docs/architecture/progress.md` now states it, along with the fact that deprecation does not
cause it because `getQuestionById` is the historical resolver.

### Exposure, weak areas and recent performance

`QuestionExposure` is a genuine single definition: coverage reads it inside `LearningProgressService`
and unseen practice reads it in selection, so "Progress says seen, practice says unseen" is not
expressible. It is keyed by stable ID, set-valued, correctness-independent and completed-only, and it
deliberately asks nothing about whether an ID still exists — callers intersect with whatever current
content they care about.

`LearningPerformanceDerivation` is likewise shared: `LearningProgressService` and
`AssessmentQuestionSelector` both derive weak areas from it rather than each applying its own
threshold. Weak-area output prefers an actionable weak Subtopic over its parent Topic, retains a Topic
only when no child qualifies, and sorts by accuracy, then descending evidence, then a stable composite
key — no map iteration order reaches the result.
`weakAreasSortByAccuracyThenEvidenceThenStableIdentity` covers it. The thresholds themselves were not
touched; `CQ-DATA-009` only corrects the architecture document, which still claimed the pre-`5f95fd3`
values of 3 and 2 rather than the single value of 5 the code applies to both scopes.

`RecentPerformancePolicy` sorts the supplied history with the same comparator
`AssessmentAttemptDao` orders by rather than trusting the caller, takes the newest five, and reverses
to oldest-first so a chart reads past to present without presentation reversing domain data. The
per-answer series is capped with `takeLast`, which keeps the most recent outcomes and leaves the
summary — derived from the attempt series — unaffected;
`answerSeriesKeepsTheMostRecentOutcomesWhenTheWindowExceedsTheCap` and
`answerSeriesCapDoesNotChangeTheRecentSummary` cover both halves, and
`attemptsCompletedAtTheSameInstantBreakTiesByStartThenIdentity` covers the tie behavior Part 3B
defined. No derivation re-sorts completed history under a different rule.

### Study progress and Continue Learning

`StudyProgressService` reads study state once per snapshot and collapses it to a `Set` of IDs, so a
Unit and its Topic cannot disagree by sampling at different moments and neither duplicates nor row
order can reach the derivation. `StudyProgressDerivation` takes its denominator from the current
ACTIVE Lessons and never from the studied set, excludes DEPRECATED Lessons from both sides of every
fraction, and makes an orphan identity a non-event rather than a special case. `StudyProgressSummary`
keeps "no ACTIVE Lesson in scope" as `Empty` rather than a zero denominator, and completion is a
derived property of `Progress` so no caller can assemble a contradictory result.

`ContinueLearningPolicy` walks Units then Lessons in the order given, skipping DEPRECATED at both
levels, returning the first ACTIVE Lesson absent from the studied set, and distinguishing `Complete`
from `Empty` with a flag it only reads once the walk has run to the end. Nothing sorts — authored list
position is the pedagogical sequence — and the policy is pure over content plus identities with no
clock, repository or assessment history reachable from its package.
`ContinueLearningPolicyTest` covers authored order across Units, deprecated Units and Lessons, orphan
identities, `Complete`, `Empty`, and determinism.

### Continue Studying and recommendations

`ContinueStudyingResolver` walks completed history in the order it was given, never re-sorting, and
keeps walking past an unusable newer entry instead of giving up at the top. It skips Mixed attempts
through the shared `toRecentStudyContext`, skips a Topic that is missing or no longer ACTIVE, degrades
a DEPRECATED Subtopic to its still-ACTIVE parent Topic — widening a practice preset's scope while
keeping its source — and skips a Subtopic whose parent cannot be recovered at all. A persisted
`AssessmentScope.Subtopics` is skipped rather than having one of its members chosen. An `ALL` run
returns to content while a targeted run reopens the builder on a preset carrying the original source,
and `ContinueStudyingResolverTest` covers every one of these cases including the source round-trip.

`LearningRecommendationResolver` gathers facts and decides nothing. It reads the progress snapshot the
caller already derived rather than loading another, asks for the unresolved-mistake count with that
same completed history, and takes recent context from the same list, so all four inputs describe one
history snapshot. `TopicBrowserViewModel` supplies that single snapshot and its comment says why. A
failing count propagates out of `resolve` and the screen loses the card, which
`anUnknownUnresolvedCountFailsRatherThanReadingAsZero` pins — a fabricated zero would fall through to
weak areas and recommend practice on a fact nobody established. Loaded-empty history stays distinct
from unreadable history: the former reaches the policy as `completedAttemptCount == 0` and yields the
new-user recommendation, the latter never reaches the policy at all. An empty or unusable ACTIVE
curriculum returns no recommendation rather than a fabricated destination.

`LearningRecommendationPolicy` is an ordered decision tree with no score, and its coverage tie-break is
resolved by exact integer cross-multiplication rather than floating-point ratios, then by a matching
recent context, then by more unseen Questions, then by stable Topic ID. Saved Questions appear nowhere
in it, and Continue Learning appears nowhere in it either; the three state models stay separate.

### Mistake resolution

`UnresolvedMistakeDerivation` consumes newest-first history as given, treats the first occurrence of
each stable Question ID as authoritative, and therefore resolves a Question the moment a newer correct
occurrence exists and reopens it when a newer incorrect one does.
`MistakeReviewServiceTest` covers incorrect-then-correct, correct-then-incorrect, the three-occurrence
case, cross-configuration resolution, retakes, and `historyOrderIsConsumedAsGivenWithoutReSorting`.
Identity is the stable Question ID throughout — never text or list index — and current content is
absent from the derivation, so a historical mistake whose Question is now DEPRECATED or missing stays
unresolved while review content reports `ReviewQuestionItem.Missing`. Historical unresolvedness and
current practiceability are separate concepts and remain so.

### Persistence versus derivation

Nothing derived is persisted. The full schema at version 8 holds publisher content, assessment
occurrences, and two learner-owned identity tables of a stable ID and a timestamp; there is no stored
progress percentage, weak-area classification, recommendation or continue-learning target, and no
invalidation problem to go with one. No new persisted derived state was added in this pass.

### Write validation

Neither repository validates a blank ID, and none was added. The only production writers are
`StudyProgressStateHolder` and `SavedQuestionStateHolder`, both acting on an ID taken from content the
learner is looking at, and `CurriculumValidator` and `LearningCurriculumValidator` already reject blank
Topic, Subtopic, Question, Unit and Lesson IDs at the authoring boundary that produces them. Adding a
`require` at the repository would duplicate a guarantee that is made where content enters the system,
which is the boundary that can actually be crossed.

### Part-2 closures

Part 2B's outstanding assumption — that `StudyProgressStateHolder`'s `tryLock` refresh strategy is
safe because every read-back returns authoritative persisted truth in a deterministic order — is
confirmed: the repositories hold no cache, read straight through the DAO, and order in SQL. Part 2D's
matching assumption for `SavedQuestionStateHolder` is confirmed on the same evidence, as are the three
questions it deferred: a repeated save is idempotent and timestamp-preserving, an unsave of an already
removed identity is a no-op delete, and single-statement operations need no transaction boundary. All
three Part-2 holder findings are closed as stated rather than reopened, and `CQ-STATE-011`'s duplicate
history read remains a presentation-level observation rather than a data defect, since the derivation
it feeds is per-call and consistent.

### Fixes

- `CQ-DATA-009`: `docs/architecture/progress.md` now states the single weak-area threshold of 5
  observations the code has applied to both Topics and Subtopics since commit `5f95fd3`.
- `CQ-DATA-011`: the same document now explains that overall totals and the Topic/Subtopic breakdown
  come from different sources and may legitimately disagree when a historical Question no longer
  resolves, and that deprecation does not cause it.
- Two concurrency regressions added to the learner-owned repository suites, both confirmed to fail
  against a `REPLACE` conflict strategy.

No production Kotlin changed, no schema changed, no migration was added, and no Koin, platform,
ViewModel or content file was touched.

### Handoffs

- **Part 3D — preference store contract:** none opened. Neither learner-owned repository reaches a
  generic preference or key-value abstraction; both go straight to Room through `CurriculumDatabase`,
  so nothing here constrains how preferences are stored.
- **Part 4 — DI, lifecycle and platform:** `CQ-DATA-010`'s measurement is per-host. The per-stable-ID
  read transaction that looks modest on the bundled SQLite hosts is the same call on the web worker
  driver, where Part 4C already owns the transaction and isolation question.
- **Part 5 — cross-cutting:** `LocalSavedQuestionRepository` and `LocalLessonStudyRepository` are
  structurally identical — stable ID plus timestamp, insert-ignore, delete by ID, ordered read — and a
  generic "timestamped identity set" repository is the obvious extraction. It was deliberately not
  made: the two are separate learner concepts whose contracts are currently identical by coincidence of
  requirements rather than by definition, and the shared documentation each interface carries is worth
  more than the twenty lines an abstraction would remove. Recorded for Part 5 with that reasoning, not
  as a recommendation.

### Validation

- `./gradlew :shared:jvmTest --tests '*LocalLessonStudyRepositoryTest*' --tests '*LocalSavedQuestionRepositoryTest*'`
  — run first with both DAOs temporarily switched to `OnConflictStrategy.REPLACE`: 21 tests, 4 failed,
  the two new concurrency regressions among them. Re-run after restoring the DAOs: passing.
- `./gradlew :shared:jvmTest --tests '*LearningProgressServiceTest*' --tests '*LearningRecommendation*' --tests '*ContinueStudyingResolverTest*' --tests '*ContinueLearningPolicyTest*' --tests '*StudyProgress*' --tests '*MistakeReviewServiceTest*' --tests '*LearningAnalyticsCurriculumEvolutionIntegrationTest*' --tests '*CurriculumDatabaseMigrationTest*'`
  — passing.
- `./gradlew :shared:jvmTest` — passing.
- `./gradlew :shared:check` — passing. JVM, Android host, JS, Wasm and iOS simulator test tasks all
  executed.
- `git diff --check` — clean.
- Room schema: **unchanged**. `shared/schemas` is untouched, no migration was added, and no migration
  was required.
- Not run: Android lint and the application-shell assemble tasks, because no platform source, Compose
  UI, navigation route, theme value or resource changed and `:shared:check` already compiled every
  target.


## Part 3D Review Record

- **Assigned production boundary (5 files):** the common preference contract and its one consumer
  chain — `AppPreferenceStorage`, `ThemePreferenceStore`, `ThemePreference` with
  `resolveDarkTheme`, `AppearanceStateHolder`, and `AppearanceModule`. Platform implementations are
  excluded by the chunk definition and belong to Part 4C; each was read only far enough to confirm
  what the common contract must promise about them.
- **Supporting call sites inspected, not re-audited (3):** `SettingsDestination` as the only writer,
  `AppearanceTheme` as the only non-Settings reader, and `AppRoot` for the startup-timing comment
  that refers to this store. Their Compose and state behaviour is owned by Parts 1A and 2A.
- **Tests inspected (5 classes, 22 tests):** `ThemePreferenceResolutionTest`,
  `ThemePreferenceStoreTest`, `AppearanceStateHolderTest` (all three in the common
  `AppearancePreferenceTest.kt`), plus `JvmAppPreferenceStorageTest` and `AppearanceThemeTest` on
  the JVM.

### The contract

`AppPreferenceStorage` is two methods over a `String?` value, and it is the entire boundary between
the application and five different platform key-value stores. The audit's question for a persistence
contract is whether the written promise and the code agree, and here they do.

Three promises are stated and all three hold. **Absence and unreadability are the same answer:**
`read` returns null for both, so no caller has to distinguish "never stored" from "storage is
blocked", and `ThemePreferenceStore.read` maps both onto `ThemePreference.System`. **An
unrecognised token does not throw:** the `when` in `ThemePreferenceStore.read` has an `else` branch
rather than an exhaustive token match, so a value written by a later version, or a corrupted one,
degrades to the system theme; `anUnrecognisedStoredValueReadsAsNoOverride` pins it. **Following the
system is the absence of a key, not a third token:** `write(System)` passes null and clears it,
which `returningToTheSystemThemeClearsTheStoredKey` pins, and which is what lets an installation
that has never opened Settings behave exactly as it did before the preference existed.

The unrecognised-token path deliberately does not repair storage. Reading a foreign token as
`System` while leaving it in place means a downgrade followed by an upgrade returns the learner to
the choice the later version recorded, whereas rewriting it on read would destroy that. This is the
better behaviour of the two and no change was made.

One key, one encoding, one place. `Key`, `LightToken` and `DarkToken` are `const` in
`ThemePreferenceStore`'s companion, so the five hosts cannot drift; the KDoc correctly records that
renaming `appearance.theme` would silently discard every learner's saved appearance. No other
common code reads or writes `AppPreferenceStorage` — a repository-wide search finds
`ThemePreferenceStore` as the only implementation-independent consumer, and `SettingsDestination`
as the only path that writes.

### Ownership and lifetime

Confirmed unchanged from the Part 2A conclusion, and re-verified against the code rather than
assumed: `AppearanceStateHolder` is a Koin `single` so the preference outlives the Settings entry
that changes it, `MutableStateFlow` is constructed from a synchronous `store.read()`, and
`setDarkTheme` publishes before persisting. Part 2A accepted that order against the non-throwing
contract and nothing here changes that reasoning.

`setDarkTheme`'s equality guard cannot skip a needed write. `System` is never equal to the `Light`
or `Dark` it computes, so the first explicit choice always reaches storage, in both directions and
under either system value — which is exactly what
`turningTheSwitchOffUnderADarkSystemStillPersistsAnOverride` exists to prove.

The guard does have one consequence worth stating, because it is the only observable cost of the
no-throw contract. A storage write that silently fails is indistinguishable from one that
succeeded, so the in-memory value moves to `Dark` and every subsequent identical `setDarkTheme(true)`
returns early without retrying. The learner would have to toggle away and back to produce another
write. This is not a defect to fix at this layer: making it retryable would require `write` to
report success, which would reintroduce into common code exactly the failure the contract was
written to absorb, and the product's stated position is that failing to remember a preference must
not become a visible error. Recorded here rather than opened as a finding.

### Findings

**No material issue found.** The common preference contract is complete, internally consistent,
singly-sourced, and covered by tests at the same boundary every host implements. No production
Kotlin, no test and no documentation changed in this chunk.

### Handoffs

- **Part 4A — common Koin graph:** `appearanceModule` declares `ThemePreferenceStore` and
  `AppearanceStateHolder` as `single` and requires `AppPreferenceStorage` from a platform module.
  One timing claim needs the DI pass to settle it: `AppearanceStateHolder`'s KDoc, `AppearanceTheme`'s
  KDoc and `docs/architecture/overview.md` all state that the holder reads storage "while the host is
  building its graph", which is only true if something resolves the `single` during startup. Koin
  singles are lazy, and the holder's other resolution sites are `AppearanceTheme`'s `remember` block
  and `SettingsDestination`'s `koinInject()`, both inside composition. Part 4A owns resolution timing
  and is the right place to confirm or correct the claim.
- **Part 4C — platform implementations.** The contract this chunk validated is only as good as the
  four implementations that must satisfy it, and they are not uniform. Part 4C must verify:
  - **No-throw parity.** `JvmAppPreferenceStorage` wraps both methods in `runCatching`, and the web
    storage guards inside JavaScript so that a browser with site data blocked returns the empty
    string. `IosAppPreferenceStorage` is safe by construction, because `stringForKey` returns null
    for an absent or non-string value. `AndroidAppPreferenceStorage` is the one with no guard:
    `getSharedPreferences` and `getString` are both called unprotected, so a locked direct-boot
    context or a non-string value stored under `appearance.theme` would throw out of the holder's
    constructor. No production path creates either condition today, which is why this is a
    verification item rather than a finding here.
  - **Durability timing.** Android's `write` uses `Editor.apply()`, which is asynchronous to disk.
    The common KDoc's claim that the choice is durable "by the time the switch has finished moving"
    is exact for the JVM, iOS and web implementations and approximate for Android.
  - **Web empty-string conflation.** `WebAppPreferenceStorage.read` maps an empty stored string onto
    null. Harmless for the only key in use, since neither token is empty, but it is a property of
    that implementation rather than of the contract.

### Validation

- `./gradlew :shared:jvmTest --tests '*AppearancePreferenceTest*' --tests '*ThemePreference*' --tests '*AppearanceStateHolder*' --tests '*JvmAppPreferenceStorageTest*' --tests '*AppearanceThemeTest*'`
  — 22 tests across 5 classes, 0 failures.
- Not run: `:shared:jvmTest` in full, `:shared:check`, and any host assemble task. Nothing changed
  in this chunk — no production file, no test and no document — so there is no change for a broader
  command to prove. `git status --short` is clean for Part 3D.


## Part 4A Review Record

### Inventory of the current common graph

Counted from the working tree at `a77156d`, not carried forward from the Part 0 plan, though the
totals agree with it.

| Module | Path under `shared/src/commonMain/kotlin/org/artkachenko/kmp_learning_app/` | `single` | `factory` | `viewModel` |
| --- | --- | ---: | ---: | ---: |
| `curriculumDataModule` | `data/local/curriculum/CurriculumDataModule.kt` | 3 | 0 | 0 |
| `learningContentModule` | `curriculum/learning/content/LearningContentModule.kt` | 1 | 0 | 0 |
| `assessmentDataModule` | `data/local/assessment/AssessmentDataModule.kt` | 11 | 0 | 0 |
| `savedQuestionDataModule` | `data/local/saved_questions/SavedQuestionDataModule.kt` | 1 | 0 | 0 |
| `lessonStudyDataModule` | `data/local/lesson_study/LessonStudyDataModule.kt` | 1 | 0 | 0 |
| `topicStudyPresentationModule` | `topic_study/TopicStudyPresentationModule.kt` | 11 | 0 | 15 |
| `appearanceModule` | `settings/AppearanceModule.kt` | 2 | 0 | 0 |
| **Total** | **seven modules, exactly the expected set** | **30** | **0** | **15** |

- **Parameterized ViewModels: 8.** `ProgressTopicViewModel`, `TopicDetailViewModel` (`topicId`),
  `LearningUnitViewModel` (`unitId`), `LearningLessonViewModel` (`unitId`, `lessonId`),
  `PracticeBuilderViewModel` (`PracticeBuilderTarget`, optional `PracticeQuestionSource`),
  `AssessmentTakingViewModel`, `FocusedResultViewModel`, `MixedInterviewResultViewModel`
  (`attemptId`). The other seven take no runtime parameter.
- **`koinViewModel` resolution sites: 20**, across 15 files, resolving all 15 definitions. Six of
  them resolve `AssessmentLaunchViewModel`: `AssessmentLaunchCoordinator`'s own default plus the
  five destinations that hoist it and pass it in, so the default is never evaluated in production.
- **`koinInject` sites: 1** — `SettingsDestination`'s `holder: AppearanceStateHolder = koinInject()`.
- **Direct `KoinPlatform`/global-context lookups in common production code: 1** —
  `AppearanceTheme`. The four host `start*LocalDataGraph` functions use `GlobalContext` or
  `KoinPlatform` as well, but those are Part 4B's surface.
- **Injected `CoroutineScope`s: 1 type, 7 injection points.** `AppCoroutineScope` is passed to
  `AssessmentHistoryStore`, `InterviewHistoryStateHolder`, `MistakeReviewStateHolder`,
  `SavedQuestionStateHolder`, `StudyProgressStateHolder` and `ProgressStateHolder`, always written
  as an explicit `get<AppCoroutineScope>()`.
- **Dependencies resolved by concrete type rather than interface: 1** —
  `AssessmentQuestionSelector(completedHistory = get<AssessmentHistoryStore>())`, where the
  parameter's declared type is the narrower `CompletedAssessmentHistory`. Every other `get()` either
  resolves a type that has only a concrete binding or resolves the interface the module binds.
- **Qualifiers: 0.** No `named(...)` or custom qualifier exists anywhere in the module set.
- **Koin: 4.2.2** from the version catalog; Navigation 3 UI 1.1.1 with
  `lifecycle-viewmodel-navigation3` 2.11.0-beta01.

### Lifetime conclusions

The Part-4A question — whether each binding's Koin lifetime matches the lifetime its behaviour
requires — is answered yes for all 45 definitions. No binding was changed.

**Process/application lifetime, correctly `single`.** `AppCoroutineScope`; the six app-scoped state
holders; `AssessmentHistoryStore`; `BundledLearningContentRepository`; `CurriculumDataInitializer`;
`ThemePreferenceStore` and `AppearanceStateHolder`. Three of these need singleton identity for
correctness rather than convenience, and each was checked against its fields rather than its name.
`CurriculumDataInitializer` holds a `Mutex` and a `MutableStateFlow<Boolean>` that coalesce
concurrent in-process startup attempts, so a second instance would re-run the import — the reason
`CQ-BUG-001` moved completion here in Part 2A. `BundledLearningContentRepository` holds a `Mutex`
and a `private var content: LoadedLearningContent?`, the process cache Part 3A established, so a
factory would re-parse and re-validate the bundled document per consumer. `AssessmentHistoryStore`
is the cache eleven consumers share; `CQ-DI-002` now pins its identity.

**Navigation-entry lifetime, correctly `viewModel`.** All fifteen. Each maps to at least one
`koinViewModel` site composed inside `NavDisplay`, so each resolves under the `NavEntry`'s own
`ViewModelStoreOwner`; the sole exception is `AppShellViewModel`, resolved in `App.kt` above
`NavDisplay` and therefore owned by the host's store, which is the shell lifetime it wants.

**Stateless reusable services, acceptably `single`.** `LearningPerformanceDerivation`,
`AssessmentQuestionSelector`, `AssessmentEngine`, `StartAssessment`, `AssessmentRetakeService`,
`AssessmentSessionLoader`, `LearningProgressService`, `AssessmentReviewLoader`,
`MistakeReviewService`, `ContinueStudyingResolver`, `LearningRecommendationResolver`,
`SavedQuestionContentResolver`, `PracticeTargetResolver`, `CurriculumImporter`, and the four
repository implementations. Every one was opened and read for fields: none holds a current request,
attempt, destination ID or selection, and the only `var`s found are locals inside function bodies —
`AssessmentQuestionSelector`'s round counters. Per the chunk's own rule these were not converted to
factories for lacking state.

**Zero factories is not a gap.** Everything in the graph is either app-shared or a navigation
ViewModel, so there is nothing for a factory to express.

### `AppCoroutineScope`

`class AppCoroutineScope(delegate: CoroutineScope = CoroutineScope(SupervisorJob() +
Dispatchers.Default)) : CoroutineScope by delegate`, bound once as a `single`.

Process lifetime is required, not stylistic: the six holders built on it publish `StateFlow`s that
must survive a navigation entry being destroyed, which is the whole reason they are not
`viewModelScope` work. `SupervisorJob` matches that — the six pipelines are independent, and a
failure in the progress derivation must not silently stop the saved-question projection. It is left
alone, as the chunk directs.

`Dispatchers.Default` is appropriate for what actually runs on it. The app-scoped flows do CPU
derivation (`LearningPerformanceDerivation`, the progress and study projections) plus Room *suspend*
reads, which manage their own execution and do not need an IO dispatcher supplied by the caller. No
evidence of a dispatcher problem was found and none is recorded.

Cancellation before process death is not expected and is not needed. `stopKoin` appears nowhere in
production code — every host guards startup with `if (GlobalContext.getOrNull() != null) return`, so
the graph is created at most once per process and is never torn down. The chunk's instruction not to
flag an uncancelled process-global scope therefore applies, and no shutdown hook was designed.

Nothing short-lived injects it: the seven injection points are all app-scoped holders, and no
ViewModel receives a raw `CoroutineScope` alongside its `viewModelScope`.

The distinct wrapper type is doing real work and is kept. It makes `get<AppCoroutineScope>()`
unambiguous without a qualifier, which is why the graph needs no qualifiers at all. Note that the
holders' constructors declare plain `CoroutineScope`; that is deliberate and better, because it lets
every holder test take a `TestScope` while the module stays explicit about which scope production
passes.

### Graph teardown

Within one process there is no production restart path, as above. Tests are the only place the graph
is stopped and started repeatedly, and they already handle it: the eleven Koin-touching JVM tests
serialize on `appIntegrationMainDispatcherLock`, call `stopKoin()` both before and in `finally`, or
use an isolated `koinApplication { }` rather than the global context, and two integration tests go
further and call `app.koin.get<AppCoroutineScope>().cancel()` in teardown.

A leaked `AppCoroutineScope` does survive `stopKoin()` in the tests that do not cancel it, because
stopping Koin closes the container and not the coroutines its instances started. It is inert: the
store's upstream is an invalidation signal that only a now-unreachable consumer could pulse, so the
scope parks after its first read and is released with the JVM. No test-ordering dependency or
cross-test interference was observed, `:shared:jvmTest` and `:shared:check` both pass, and the
fixtures were left alone rather than rewritten for a leak with no symptom.

### Identity conclusions

- **`AssessmentHistoryStore`:** one binding, one instance, shared by all eleven consumers — the
  shell badge (`AppShellViewModel`), `ProgressStateHolder` and `ProgressViewModel`,
  `MistakeReviewStateHolder` and `MistakeReviewViewModel`, `InterviewHistoryStateHolder`,
  `TopicBrowserViewModel`, `TopicDetailViewModel`, `AssessmentTakingViewModel`, and
  `AssessmentQuestionSelector` through the narrower contract. Ten declare the parameter as
  `AssessmentHistoryStore` and receive it through `get()`; the eleventh is the concrete resolution
  discussed below. `CQ-DI-002` now pins this.
- **The other app-scoped holders:** `StudyProgressStateHolder`, `SavedQuestionStateHolder` and
  `AppearanceStateHolder` each have one binding and an existing identity assertion;
  `ProgressStateHolder`, `MistakeReviewStateHolder` and `InterviewHistoryStateHolder` each have one
  binding and one consumer, so identity cannot diverge.
- **Repository identity:** exactly one binding per contract for `CurriculumRepository`,
  `LearningContentRepository`, `AssessmentRepository`, `SavedQuestionRepository` and
  `LessonStudyRepository`, each `single<Interface> { LocalImpl(...) }`. Because Koin binds only the
  declared type, the implementation classes are not separately resolvable, and no feature
  constructs a local repository directly: every `Local*Repository(` and `Bundled*Repository(`
  constructor call in `commonMain` is inside its own module.
- **No duplicates or collisions.** A mechanical pass over all 45 definitions found 45 distinct
  binding keys and 45 distinct concrete constructor calls: no interface bound twice, no concrete
  type constructed in two modules, and no `single<Interface> { Impl() }` paired with a second bare
  `single { Impl() }`. Nothing depends on Koin's override behaviour.

### Interface versus concrete resolution

The single concrete resolution is justified and is kept. `AssessmentQuestionSelector` declares
`CompletedAssessmentHistory`, a one-method `fun interface`, while the module hands it
`get<AssessmentHistoryStore>()`. That is not a shortcut around the interface — it is the only way to
reach the shared cache, because `CompletedAssessmentHistory` has no binding of its own and the point
of the parameter, established in Parts 2 and 3, is that repeated Practice Builder edits must read
the app-scoped cache rather than re-query history. The module comment already says exactly this.

An alias `single<CompletedAssessmentHistory> { get<AssessmentHistoryStore>() }` would resolve the
same instance and would be safe, but it buys nothing today: there is one consumer, the narrow
contract exists for the constructor signature and for `FakeCompletedHistory` in
`AssessmentQuestionSelectorTest`, and the concrete resolution creates no test friction because the
selector is constructed directly in its tests. Not added.

`LearningRecommendationResolver` is the other narrowing and is also kept as it stands: the module
closes a lambda over `get<MistakeReviewService>()` and passes only `countUnresolved`, so the
resolver depends on a count rather than on the whole mistake-queue service. The closure captures a
singleton and nothing from a composition or a destination, which is the distinction that matters.

### Runtime parameters

The DI contract is aligned at every parameterized site, and the ordering-sensitive one is protected
by an existing test.

`LearningLessonViewModel` is the only definition that reads positionally, because both identities
are `String` and a type-based lookup could not tell them apart. The four links agree:
`LearningLessonDestination` calls `parametersOf(unitId, lessonId)`, the module reads
`parameters.get(0)` then `parameters.get(1)`, the constructor declares `unitId` then `lessonId`, and
`App.kt` passes `unitId = route.unitId, lessonId = route.lessonId` from the route being rendered.
`TopicStudyPresentationModuleTest` resolves it through the real module with
`parametersOf("unit_thinking_in_compose", "lesson_declarative_ui")` and asserts both values on the
resulting state, so a transposition fails a test. There is one call site and no competing one, so
no wrapper parameter object and no value class was introduced.

`PracticeBuilderViewModel` reads by type — `parameters.get()` for `PracticeBuilderTarget` and
`parameters.getOrNull()` for `PracticeQuestionSource` — and the two types are unrelated, so an
absent source cannot consume the target. In practice the source is never absent: all three
`PracticeBuilder*` entries call `PracticeBuilderDestination`, whose own `initialSource` parameter
defaults to `PracticeQuestionSource.ALL`, so `parametersOf(target, initialSource)` always carries
both. The module's `?: PracticeQuestionSource.ALL` is therefore a redundant but harmless second
statement of the same default, and `TopicStudyPresentationModuleTest` already covers both the
supplied and the defaulted source.

The three attempt-based ViewModels each take exactly one parameter, and each destination passes
exactly the `attemptId` of the route it renders — `AssessmentTakingDestination`,
`FocusedResultDestination` and `MixedInterviewResultDestination` all read `route.attemptId` in
`App.kt`. No mismatch among taking, focused result and mixed result was found. The single-String
sites (`topicId`, `unitId`) read by type for the same reason, which is safe with one parameter.

### Navigation 3 ownership

`NavDisplay` installs `rememberSaveableStateHolderNavEntryDecorator()` before
`rememberViewModelStoreNavEntryDecorator()`, which is the required order — the ViewModel decorator's
documentation states it needs the saveable decorator so entry-scoped ViewModels can reach a
`SavedStateHandle`. Every destination composes inside `entryProvider`, under both decorators, so
`koinViewModel()` there resolves against that entry's `ViewModelStoreOwner` and not the host's.

`AppShellViewModel` is the one deliberate exception and the `AssessmentHistoryStore` /
`AppShellViewModel` distinction the chunk asks about holds exactly as drawn: the store is a
process-scoped `single` that owns the history, and the shell ViewModel is an entry-independent
ViewModel that only maps `historyStore.history` into a badge count through `viewModelScope`. They
were not collapsed.

**`AssessmentLaunchViewModel` gets one instance per destination, not one shared coordinator.** Five
destinations use `AssessmentLaunchCoordinator`, and each resolves the launch ViewModel itself and
passes it in. Because each of those destinations is a distinct `NavEntry` with its own store, the
five launches are independent — a failed practice launch on the mistake queue cannot surface its
dialog on the focused-result screen. The only way two of them could share state is two back-stack
entries with an equal content key, which `CQ-DI-003` records as currently unreachable.

Entry recreation re-resolves correctly: a new `NavEntry` gets a new store, and the destination's
`koinViewModel { parametersOf(...) }` supplies the route's own identities again, so route parameters
never need to live in app-global DI state. No `SavedStateHandle` was added — the typed routes
already carry every runtime identity, and nothing in the product asks for state restoration beyond
what the saveable decorator gives.

### Module placement

Every module boundary was checked against ownership and left where it is.

`savedQuestionDataModule` and `lessonStudyDataModule` stay separate one-binding modules. Each marks
a learner-owned persistence boundary, and `lessonStudyDataModule`'s separation from
`learningContentModule` is the publisher-versus-learner line Part 3 established; merging either for
file count would erase a distinction the code relies on.

`topicStudyPresentationModule` is the large one — 11 singles and all 15 ViewModels — and size alone
is not the defect. What it mixes is coherent: app-scoped presentation state, the stateless resolvers
that feed it, and the ViewModel declarations that read both. `StudyProgressStateHolder` is correctly
here rather than in `lessonStudyDataModule`, which owns only the Room-backed repository, and
`SavedQuestionStateHolder` correctly sits apart from `SavedQuestionRepository` for the same reason:
the repository owns stored facts and the holder owns the app-scoped projection. Both module comments
already say so and both match the code. The stateless resolvers — `AssessmentReviewLoader`,
`MistakeReviewService`, `ContinueStudyingResolver`, `LearningRecommendationResolver`,
`PracticeTargetResolver`, `SavedQuestionContentResolver` — introduce no hidden or circular
dependency where they are. Discoverability in a module this size is a Part 5 question, not a
lifetime defect.

### Direction and cycles

The graph is a DAG. Tracing all 45 definitions gives one direction — platform database and
preference storage, then repositories, then derivations and domain services, then app-scoped
holders, then ViewModels — with no edge back up. No `A requires B requires A` pair exists, directly
or through a holder, and shared repositories were not mistaken for cycles.

Layer direction is clean and was checked by import rather than by intent: nothing under `data/**` or
in `BundledLearningContentRepository` imports Compose, navigation or `ViewModel`, and all 30
singleton classes were checked individually for `androidx.compose`, `androidx.navigation` and
`androidx.lifecycle.ViewModel` imports with no hit. No singleton retains an Activity, a context, a
navigator, a Compose `State`, a `LocalContext`, a URI handler or a destination callback; the only
lambda built inside a module closes over another singleton.

### Service-locator surface

This is the strongest result of the pass. Exactly 24 files in `commonMain` import anything from
`org.koin`: the seven module files and seventeen Compose files. **No domain, data, repository,
service, state-holder or ViewModel class reaches the container** — every one of them receives its
dependencies through its constructor, which is why the test suite constructs them directly and why
`AssessmentEngine`'s clock and ID generator are constructor defaults overridden per test rather
than a global mutable hook. Module-DSL `get()` is not counted as service-locator use.

`SettingsDestination`'s `koinInject()` default parameter is straightforward and is kept: Part 2A
already concluded no Settings-specific owner is required, the app-scoped holder is exactly what the
screen needs, and the default parameter is also the override seam its tests use. The same pattern in
the fourteen destinations' `koinViewModel()` defaults is the project's standard testability seam,
and the one place it could have caused duplicate resolution — `AssessmentLaunchCoordinator`'s own
default — is avoided because every production caller passes the ViewModel in.

### `AppearanceTheme`'s optional lookup

`remember { KoinPlatform.getKoinOrNull()?.getOrNull<AppearanceStateHolder>() }` is **accepted as a
deliberate infrastructure seam**, not classified as service-locator abuse, and the reasoning is
recorded here because it is the graph's one exception.

Against the chunk's five criteria: composing without a graph is a genuinely supported mode, because
previews and isolated screen tests compose screens directly and `AppTheme` must still resolve; the
fallback is intentional and *tested* — `AppearanceThemeTest.aCompositionWithoutAnApplicationGraphStillThemes`
calls `stopKoin()` first and asserts the composition still lands on one of the app's own schemes
rather than a Material baseline; the lookup cannot hide a broken production graph, because a host
that failed to install `appearanceModule` would lose the preference but every other missing binding
still fails loudly at the first `koinViewModel` call, and `SharedHostStartupTest` asserts the
appearance chain end to end; and `koinInject()` is the alternative that would throw, which would
mean every preview and direct screen test had to start a Koin graph to render. On the fifth — whether
`remember` pins a null holder if Koin starts later in the same composition — the answer is that it
would, but production ordering rules it out: all four hosts call `start*LocalDataGraph()` before any
composition, and `AppRoot` is only reached afterwards. It is a test and preview concern only, and
those cases want the null.

This is where `CQ-DI-001` came from: the seam is right, but the comments defending it described the
wrong mechanism.

### Eager state versus eager object creation

The distinction the chunk asks for is real here and was the source of the only production-comment
defect. Koin 4.2.2's `single` is lazy — `createdAtStart` defaults to `false` and nothing in this
repository sets it — so registration is not construction, and every one of the 30 singles is built
on first resolution.

`SharingStarted.Eagerly` then means "start as soon as the holder exists", which is a later moment
than "at application start". Concretely:

- `AssessmentHistoryStore` is constructed when `AppShellViewModel` is first resolved, which happens
  in `App.kt` as soon as startup reaches `Ready`. Its eager `stateIn` therefore does warm the
  history before any feature screen asks, which is what its documentation claims and what actually
  happens — the store's own KDoc says nothing about graph-build time and needed no correction.
- `ProgressStateHolder`, `MistakeReviewStateHolder` and `InterviewHistoryStateHolder` are
  constructed only when their ViewModels are first resolved, so their eager flows start on the first
  visit to those areas rather than at startup. Nothing claimed otherwise.
- `AppearanceStateHolder` is constructed by `AppearanceTheme`'s `remember`, and four statements
  claimed it happened while the host built its graph. Fixed as `CQ-DI-001`.

`StudyProgressStateHolder` and `SavedQuestionStateHolder` already describe this accurately — "Called
when a Learn destination opens rather than once at startup" — which is what made the appearance
comments identifiable as the outliers.

### Startup initializer contract

`CurriculumDataInitializer`'s in-process `initialized` state is only meaningful if every caller gets
the same instance, and all four hosts satisfy that today: each `start*LocalDataGraph()` installs
`curriculumDataModule` and the host bridge then resolves `GlobalContext.get().get<CurriculumDataInitializer>()`
or `KoinPlatform.getKoin().get<CurriculumDataInitializer>()` rather than constructing one. That is
the contract Part 4B must verify end to end; Part 4A only records it.

### Fixes

- `CQ-DI-001` — four DI-timing statements corrected across `AppearanceStateHolder`,
  `AppearanceTheme`, `AppRoot` and `docs/architecture/overview.md`. Documentation only; no binding,
  no lifetime and no behaviour changed.
- `CQ-DI-002` — `SharedHostStartupTest` now asserts one `AssessmentHistoryStore` and resolves
  `AppShellViewModel` and `InterviewStartViewModel`, so all fifteen ViewModel bindings are covered
  by a graph-level test.
- `CQ-DI-003` — accepted as-is, recorded.

No Koin definition was added, removed, reordered or rebound. No module was created, merged or split.
No alias, qualifier or factory was introduced, no constructor DSL (`singleOf`/`viewModelOf`) was
adopted, and no DI framework change of any kind was made.

### Graph verification

No separate Koin verification test was added, because one already exists in a better form.
`SharedHostStartupTest.sharedHostModulesResolveTheWholeProductGraph` installs all seven common
modules plus the two platform bindings and resolves the real graph — including every parameterized
ViewModel with representative parameters — which is precisely the coverage Koin's `verify()` cannot
give for parameterized and platform-supplied definitions. Its sibling test composes `AppRoot`
through real Navigation 3 decorators to the Topic Browser, so resolution under the intended entry
owner is exercised rather than asserted. `TopicStudyPresentationModuleTest` covers
`AssessmentTakingViewModel`, `AssessmentLaunchViewModel` and the Practice Builder's two source
paths against fakes shaped for them. Adding a mock-everything verification test on top of this would
be brittle and would prove less.

### Handoffs

- **Part 4B — host composition roots.** Required of every host, to be verified there rather than
  here: install all seven common modules exactly once; supply exactly one `CurriculumDatabase`
  binding (required by `CurriculumImporter`, `LocalCurriculumRepository`, `AssessmentAttemptStore`,
  `LocalSavedQuestionRepository` and `LocalLessonStudyRepository`) and exactly one
  `AppPreferenceStorage` binding (required by `ThemePreferenceStore`) — these two are the *entire*
  set of bindings the common graph expects from outside; start the graph before any composition, so
  that `AppearanceTheme`'s optional lookup never legitimately returns null in production; and
  resolve `CurriculumDataInitializer` from the container rather than constructing one, so repeated
  in-process startup attempts share its coalescing state. Also confirm each host's startup guard
  makes `start*LocalDataGraph()` idempotent, since that guard is what makes the absence of graph
  teardown safe.
- **Part 4C — platform capability implementations.** Carried forward from Part 3D unchanged: the
  no-throw `AppPreferenceStorage` contract, where `AndroidAppPreferenceStorage` is the one
  implementation with no guard around `getSharedPreferences`/`getString`; Android's asynchronous
  `Editor.apply()` against the common KDoc's durability wording; the web implementation's
  empty-string-as-absent mapping; and the four `CurriculumDatabase` builders' parity.
- **Part 5 — cross-cutting.** `topicStudyPresentationModule`'s discoverability at 26 definitions in
  one file, if the module keeps growing. The redundant `?: PracticeQuestionSource.ALL` in the
  Practice Builder definition, which restates a default the destination already applies. And
  `CQ-DI-003`'s structural point: `AppNavigator.push` appends unconditionally, and route equality is
  what scopes a `ViewModelStore`.

### Validation

- `./gradlew :shared:jvmTest --tests '*SharedHostStartupTest*' --tests '*TopicStudyPresentationModuleTest*' --tests '*AppearanceThemeTest*' --tests '*AppearancePreferenceTest*' --tests '*ThemePreference*' --tests '*AppearanceStateHolder*'`
  — passing.
- Falsification of the `CQ-DI-002` assertion: with `AssessmentHistoryStore` temporarily switched
  from `single` to `factory`, `sharedHostModulesResolveTheWholeProductGraph` fails at the new
  assertion (`SharedHostStartupTest.kt:131`). The production definition was restored and
  `git diff --stat` on `AssessmentDataModule.kt` confirms it is unchanged.
- `./gradlew :shared:jvmTest` — passing.
- `./gradlew :shared:check` — passing. JVM, Android host, JS, Wasm and iOS simulator test tasks all
  executed, which is the multiplatform compile check the chunk asks for when common wiring is
  touched.
- `git diff --check` — clean.
- Host assembles: **not run, deliberately.** No Koin definition, no host source and no platform file
  changed; the only production edits are KDoc and comment text in three `commonMain` files, and
  `:shared:check` already compiled every target including Android, iOS and both web targets.


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

Part 2B — Complete

High: 0
Medium: 0
Low: 3
Observations: 0

Fixed: 3
Deferred: 0
Needs measurement: 0
Accepted as-is: 0
Not a defect: 0

Part 2C — Complete

High: 1
Medium: 1
Low: 1
Observations: 0

Fixed: 3
Deferred: 0
Needs measurement: 0
Accepted as-is: 0
Not a defect: 0

Part 2D — Complete

High: 0
Medium: 2
Low: 1
Observations: 1

Fixed: 4
Deferred: 0
Needs measurement: 0
Accepted as-is: 1
Not a defect: 0

Part 2 — Complete

Part 3A — Complete

High: 1
Medium: 1
Low: 2
Observations: 2

Fixed: 3
Deferred: 1
Needs measurement: 0
Accepted as-is: 2
Not a defect: 0

Part 3B — Complete

High: 0
Medium: 1
Low: 1
Observations: 2

Fixed: 2
Deferred: 0
Needs measurement: 0
Accepted as-is: 2
Not a defect: 0

Part 3C — Complete

High: 0
Medium: 1
Low: 2
Observations: 0

Fixed: 2
Deferred: 0
Needs measurement: 1
Accepted as-is: 0
Not a defect: 0

Part 3D — Complete

High: 0
Medium: 0
Low: 0
Observations: 0

Fixed: 0
Deferred: 0
Needs measurement: 0
Accepted as-is: 0
Not a defect: 0

Part 3 — Complete

Part 4A — Complete

High: 0
Medium: 0
Low: 3
Observations: 0

Fixed: 2
Deferred: 0
Needs measurement: 0
Accepted as-is: 1
Not a defect: 0

Part 4B — Next
```

Part 4A is complete. The common Koin graph was re-inventoried from the working tree — seven modules,
30 singles, no factories, 15 ViewModels of which 8 take runtime parameters, 20 `koinViewModel` sites,
one `koinInject`, and one direct `KoinPlatform` lookup — and every lifetime was found to match the
ownership its behaviour requires, so no binding, module or alias changed. The graph has 45 distinct
binding keys and 45 distinct concrete constructions, no duplicate definition, no qualifier, no cycle
and no layer inversion; the shared `AssessmentHistoryStore` reaches all eleven of its consumers as one
instance; every screen ViewModel resolves under its own Navigation 3 entry owner, which is what keeps
the five `AssessmentLaunchViewModel` sites independent; and no domain, data or state class reaches the
container at all, which is why the suite can construct them directly. Two Low findings are fixed.
`CQ-DI-001` corrects four statements — in `AppearanceStateHolder`, `AppearanceTheme`, `AppRoot` and
`docs/architecture/overview.md` — that claimed the appearance preference is read while the host builds
its graph; Koin 4.2.2's `single` is lazy, so the read actually happens inside `AppearanceTheme`'s
`remember` on the first composition, and it is the read being *synchronous*, not early, that prevents
a light-to-dark flash. `CQ-DI-002` closes a real coverage gap by pinning the history cache's singleton
identity and resolving the two ViewModel bindings no graph test reached, verified by confirming the
suite stayed green with that definition switched to `factory` beforehand and failed at the new
assertion afterwards. `CQ-DI-003` records that two equal routes on one back stack would share a
`ViewModelStore`, and that this is currently unreachable. Only KDoc and comment text changed in
production. The exact next chunk is **Part 4B — Host composition roots**. Do not begin it
automatically.

Part 3D is complete, and with it Part 3. The common preference contract — `AppPreferenceStorage`,
`ThemePreferenceStore`, `ThemePreference` and the holder above them — holds against every promise
written on it: absence and unreadable storage are the same null, an unrecognised token degrades to the
system theme rather than throwing, following the system is the absence of a key rather than a third
token, and the one storage key and its two tokens are declared once in common code so five hosts
cannot drift. The equality guard in `setDarkTheme` was checked against every direction and system
value and cannot skip a needed write. **No material issue was found and nothing changed.** The pass
carries one handoff worth naming: of the four platform implementations, three guard themselves against
a store that cannot be read and `AndroidAppPreferenceStorage` does not, which Part 4C owns. The exact
next chunk was **Part 4A — Common Koin graph and lifetimes**, now also complete.

Part 3C is complete. The learner-owned persistence layer was found correct against every part of its
written contract: insert-ignore on a stable-ID primary key makes a repeated save or mark a no-op that
preserves the original timestamp even under concurrent duplicate writes, a delete-then-re-add records
a genuinely new time, reads order by timestamp then stable ID in SQL rather than relying on row order,
neither table carries a foreign key to publisher content, and an identity whose content no longer
resolves stays readable and removable by ID alone. Two new concurrency regressions pin the timestamp
half of that, both confirmed to fail against a `REPLACE` conflict strategy. The derivation layer keeps
historical evidence and current content strictly apart — persisted correctness is never recomputed,
coverage's denominator is the current ACTIVE bank, exposure and weak areas and mistake resolution each
have exactly one definition shared by every consumer, and Continue Learning, Continue Studying and the
recommendation policy remain three separate answers derived from one history snapshot. Two
documentation defects in `docs/architecture/progress.md` are fixed (`CQ-DATA-009`, `CQ-DATA-011`), and
`CQ-DATA-010` records per-stable-ID historical metadata resolution as needing measurement before any
change. No production Kotlin changed, no Room schema change was made and no migration was required.
The exact next chunk is **Part 3D — Preference store contract**. Do not begin it automatically.

Part 3B is complete. The assessment persistence layer was found sound: `AssessmentAttemptStore.save`
is an atomic whole-snapshot replacement that cannot leave stale children, reads hydrate inside a
transaction, config and scope round-trip exactly including canonicalised level and multi-Subtopic
sets, historical correctness and selected answers are persisted occurrence data rather than
recomputed from current content, and completed history is three queries with a deterministic
newest-first order. One Medium domain gap (`CQ-BUG-005`) — the engine not enforcing the authored
SINGLE answer arity that both authoring validation and the UI already assume — is fixed, and the
repository's save, read, corruption and one-writer contract (`CQ-DATA-006`) is now written down on
the interface and in the persistence architecture document. `CQ-DATA-007` and `CQ-DATA-008` are
accepted as-is with recorded rationale. No Room schema change was made and no migration was
required. The exact next chunk is **Part 3C — Study progress, recommendations, saved questions**.
Do not begin it automatically.
