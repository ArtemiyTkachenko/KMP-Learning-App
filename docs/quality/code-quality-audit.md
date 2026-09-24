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
| `CQ-DATA-004` | Learning-content cache publication | Observation | High | `BundledLearningContentRepository.kt` | The cached document is published through a non-volatile field read outside the mutex. | The double-checked `content ?: mutex.withLock { content ?: ... }` is the standard shape, and a reader that observes the reference can only observe a fully constructed `LoadedLearningContent`: every one of its properties is a `val`, so JVM final-field semantics freeze them and everything reachable from them at the end of construction, and the reference is assigned only after the constructor returns. JS and Wasm are single-threaded, and Kotlin/Native's memory model follows the JVM's. No concurrency defect was found; the safety argument is simply not visible from the code. | Leave as-is. Record why the pattern is safe so a later reader does not "fix" it, and revisit only if `LoadedLearningContent` ever gains a mutable property, which would end the final-field guarantee. **Re-confirmed in the Stage 4D fix pass**, which independently re-derived this as an unsafe-publication defect, added `@Volatile`, and then reverted it: the final-field argument above is correct, and the annotation would have added a barrier per read for no correctness. The safety argument is now recorded in the repository's own KDoc rather than only here, which is what this disposition asked for and what its absence from the source made easy to miss. | Accepted as-is |
| `CQ-DATA-005` | Validator ownership | Observation | High | `CurriculumValidator.kt`, `LearningCurriculumValidator.kt`, `tools/learning_question_coverage.py` | Kotlin and Python enforce a small overlapping set of authored-content rules. | The coverage tool independently rejects a duplicate Question ID, an unrecognised status or level, an unknown Unit home Topic and an unknown Lesson Subtopic. The boundaries differ legitimately: Python guards the authored repository files in CI before anything is built, the Kotlin validators guard the runtime import and load boundary on a device that may be running an older bundle. The overlapping rules agree today and were checked against each other during this pass. | Keep both. Record the overlap so a future change to one is checked against the other; neither should be deleted for overlapping, because they protect different moments. | Accepted as-is |
| `CQ-BUG-005` | Assessment session / domain invariants | Medium | High | `AssessmentEngine.kt`, `AssessmentEngineTest.kt` | `submitAnswer` did not enforce the authored answer arity, so a SINGLE Question could record several selected answers. | The engine validated status, Question membership, non-emptiness, and that every selected ID belongs to the Question, but never compared the submission against `Question.selectionMode`. `CurriculumValidator` already rejects a SINGLE Question with several correct answers, and `AssessmentTakingViewModel` replaces rather than adds the pending ID for SINGLE, so the rule existed on both sides of the engine and not inside it. A non-UI caller could therefore persist an occurrence recording a choice the interaction never offered, and review, scoring and mistake derivation would read it as genuine. | Enforce `SINGLE` -> exactly one selected ID in `submitAnswer`, after the membership check so unknown IDs keep failing for their own reason. Leave MULTIPLE unconstrained beyond non-emptiness. | Fixed |
| `CQ-DATA-006` | Assessment persistence / contract | Low | High | `AssessmentRepository.kt`, `docs/architecture/persistence.md` | The repository interface stated no contract, so its durability, snapshot, ordering and failure semantics were discoverable only by reading the Room implementation. | Three undocumented suspend functions. `save` is whole-snapshot replacement inside one write transaction and is used for both creation and update; `getById` returns `null` only for an absent attempt and raises for a corrupt one; `getCompletedAttempts` is completed-only, newest-first, and fails as a whole on a corrupt row. None of that was written down, and a caller could reasonably have read `save` as an incremental update or `null` as "read failed". | Document each method's semantics on the interface, and record the save/read contract, the one-writer expectation and the corruption policy in the persistence architecture document. | Fixed |
| `CQ-DATA-007` | Assessment persistence / redundant state | Observation | High | `TestAttempt.kt`, `AssessmentAttemptMapper.kt` | Aggregate score and per-occurrence correctness are stored twice and the schema permits them to disagree. | `test_attempt.score_correct_answers` and `question_attempt.is_correct` express the same fact. `TestAttempt` validates `score.totalQuestions == questionAttempts.size` but not `score.correctAnswers == questionAttempts.count { it is Answered && it.isCorrect }`, so a hand-edited or corrupted database could reconstruct a COMPLETED attempt whose result screen and progress dashboard disagree. No production path can create it: `AssessmentEngine.complete` derives both from the same occurrences, and `save` writes both from one aggregate in one transaction. | Leave as-is. Adding the constructor invariant would churn roughly seventy COMPLETED fixtures across thirty-eight test files to catch a state only external corruption can produce; the summary row is worth keeping because it is what history ordering and the result screen read. **Re-confirmed in the Stage 4D fix pass**, which reached the same finding and the same deferral from the concurrency side, and adds one detail worth recording: the two sources are read *inconsistently within one snapshot* — `LearningProgressService.load` takes the headline accuracy from the stored aggregate while its own `recentPerformance`, and `LearningPerformanceDerivation`'s Topic breakdown and weak areas, count the occurrence rows. That is not a defect while the invariant holds, but it means a divergence would show as one dashboard disagreeing with itself rather than as a single wrong number. | Accepted as-is |
| `CQ-DATA-008` | Assessment persistence / timestamps | Observation | High | `AssessmentAttemptMapper.kt` | Attempt timestamps round-trip at millisecond resolution, so a reloaded attempt is not `==` to the in-memory one when the clock is finer. | `startedAt` and `completedAt` are stored as epoch milliseconds and rebuilt with `Instant.fromEpochMilliseconds`, while `Clock.System.now()` carries sub-millisecond precision on the JVM. Both values are absolute instants with no local-time interpretation, truncation is monotone so `completedAt >= startedAt` and newest-first ordering both survive it, and no consumer compares a saved aggregate with its reloaded form outside tests that use millisecond-aligned fixtures. | Leave as-is. Millisecond resolution is sufficient for history ordering and the tie-breakers behind it, and widening the columns would be a migration for no observable behavior. | Accepted as-is |
| `CQ-DATA-009` | Learning progress / documentation | Low | High | `docs/architecture/progress.md`, `LearningProgressPolicy.kt` | The progress architecture document stated weak-area evidence thresholds the code had stopped using. | The document said "A Topic is weak after at least 3 observations below 70% accuracy, and a Subtopic after at least 2", while `LearningProgressPolicy` has used a single `WeakAreaMinimumAnswered = 5` for both since commit `5f95fd3`, which is what `weakPolicyRequiresEvidenceAndTreatsExactlySeventyPercentAsNotWeak` pins. The stale numbers are the only written statement of a product threshold that decides which weak area a recommendation targets, so a later reader reconciling policy against documentation would have reached the wrong figure. | Restate the documented threshold as the single value of 5 the code applies to both scopes, with the reason the constant already records. The product threshold itself is not a code-quality question and was not changed. | Fixed |
| `CQ-DATA-010` | Historical metadata resolution / query cost | Medium | Medium | `LearningPerformanceDerivation.kt`, `LearningProgressService.kt`, `MistakeReviewService.kt`, `AssessmentReviewLoader.kt` | Historical Question metadata is resolved one stable ID at a time even where the whole ACTIVE bank has already been read in the same call. | `LearningPerformanceDerivation` correctly caches per stable ID, so the per-*occurrence* N+1 does not exist — `historicalLookupsCacheResolvedAndMissingIdentitiesOncePerLoad` pins one lookup per distinct ID. What remains is one `getQuestionById` per distinct historical Question, and `LocalCurriculumRepository` answers each inside its own `withReadTransaction` with several statements. `LearningProgressService.load()` already holds every ACTIVE `Question` from `getActiveQuestions()` in the same call, and `ProgressStateHolder`, `TopicBrowserViewModel` and `TopicDetailViewModel` each issue their own `load()` per history emission. `MistakeReviewService.load` resolves one Question per unresolved mistake for the same reason, though it genuinely needs the full content. No measurement of the cost on any host was taken. | Measure a full-bank history on the JVM and web hosts first. If it matters, the local change is to seed the derivation's cache from the ACTIVE questions the service has already loaded, leaving individual lookups only for identities that ACTIVE content cannot explain; a batched `getQuestionsByIds` on `CurriculumRepository` is the wider alternative and would also serve mistake review. Neither was done without evidence. **Measured and fixed in the Stage 4E fix pass.** The trigger analysis is what the earlier pass lacked: `ProgressStateHolder` and `MistakeReviewStateHolder` are `SharingStarted.Eagerly` app-scoped singletons, so both re-derive on *every* `invalidate()` whether or not their screens are open, and Topic Browser and Topic Detail each add their own `load()` on the same emission — up to four derivations per completed assessment. Worse, `PracticeBuilderViewModel.refreshAvailability()` runs on every level-chip tap and every source change, and for `WEAK_AREAS` that reaches `performanceDerivation.derive`, so a UI tap could trigger the whole N+1. At full-bank history (478 Questions) one derivation was 478 read transactions and ~1,912 statements. The resolution is now `getQuestionsByIds`, a single read transaction and four statements for the whole set, adopted by all five per-ID loops — the derivation, `AssessmentReviewLoader`, `MistakeReviewService`, `SavedQuestionContentResolver` and `AssessmentSessionLoader`. The seeding alternative recorded above was not taken: it would have covered ACTIVE identities only, left DEPRECATED ones reading one at a time, and done nothing for review, the saved list or resume. `getQuestionById` was *replaced* rather than kept beside the batched form, because keeping both was demonstrated to be unsafe during the pass: `CurriculumRepository by VmCurriculumRepository` delegates an interface default body, so a decorator overriding only the per-ID form was silently bypassed and a cancellation test stopped exercising cancellation. Tests that legitimately read one identity use an extension in the test source set, which cannot be overridden or delegated around. The Topic and Subtopic name loops stay per-ID and memoised: both are bounded by what the curriculum authors (17 Topics, 361 Subtopics) rather than by history, and each is one statement against one table rather than a read transaction. | Fixed |
| `CQ-DATA-011` | Learning progress / documentation | Low | High | `docs/architecture/progress.md` | Nothing said that grouped Topic and Subtopic answered counts may sum to less than the overall answered count. | Overall totals sum persisted `AssessmentScore` values, while `LearningPerformanceDerivation` can only place an occurrence whose Question still resolves through `getQuestionById` and skips the rest with `continue`. `missingQuestionKeepsPersistedOverallWhileDeprecatedAndMissingMetadataRemainScoped` proves the resulting divergence is intended, and Part 3A's never-delete import contract makes it unreachable through ordinary publishing, but the document described the two sources without stating that they can disagree. | Document the divergence and its one cause beside the existing coverage explanation, including that DEPRECATED content does not cause it because `getQuestionById` is the historical resolver. | Fixed |
| `CQ-DI-001` | Common Koin graph / startup timing | Low | High | `settings/AppearanceStateHolder.kt`, `ui/theme/AppearanceTheme.kt`, `AppRoot.kt`, `docs/architecture/overview.md` | Four written statements claimed the appearance preference is read while the host builds its Koin graph, which lazy `single` semantics make false. | Koin 4.2.2 declares `single(createdAtStart: Boolean = false)` and only definitions in `Module.eagerInstances` are instantiated by `createEagerInstances()`; no definition in this repository passes `createdAtStart`, and no host calls `koin.get<AppearanceStateHolder>()` during startup — the four `start*LocalDataGraph` functions resolve `CurriculumDataInitializer` and nothing else. The holder's first resolution is therefore `AppearanceTheme`'s own `remember { KoinPlatform.getKoinOrNull()?.getOrNull<AppearanceStateHolder>() }`, which makes `AppearanceTheme`'s "No storage I/O happens here" the exact opposite of what that composable does. The guarantee the comments were defending is unaffected: the read is synchronous inside `remember`, so it completes within the first composition and before the first frame, and there is still no light-to-dark flash and no startup step awaiting storage. | Restate all four to describe lazy first-resolution and say that *synchronous* rather than *early* is what prevents the flash. The graph is correct as it stands; only the explanation was wrong, and `StudyProgressStateHolder` and `SavedQuestionStateHolder` already describe their own laziness accurately. | Fixed |
| `CQ-DI-002` | Common Koin graph / test coverage | Low | High | `SharedHostStartupTest.kt` | The graph test asserted singleton identity for three app-scoped holders but not for `AssessmentHistoryStore`, and never resolved two of the fifteen ViewModel bindings. | `sharedHostModulesResolveTheWholeProductGraph` already pinned one instance each of `SavedQuestionStateHolder`, `StudyProgressStateHolder` and `AppearanceStateHolder`, but not the completed-history cache that eleven consumers share and that the Part 2 and Part 3 conclusions rest on — so `single` becoming `factory` there would have left every history-derived surface with its own cache and no test would have failed. Switching the definition to `factory` was confirmed to leave the suite green before the assertion was added, and to fail at the new assertion afterwards. `AppShellViewModel` and `InterviewStartViewModel` were also the only two bindings no graph-level test resolved. | Assert one `AssessmentHistoryStore` beside the three existing identity assertions and resolve the two remaining ViewModels, in the one test that installs the real `assessmentDataModule`. No new test class; the existing graph check is the right owner. | Fixed |
| `CQ-DI-003` | Navigation 3 / ViewModel store ownership | Low | High | `AppNavigator.kt`, `App.kt` | Two back-stack entries with an equal route would silently share one `ViewModelStore`, and `push` does not prevent one. | `ViewModelStoreNavEntryDecorator` scopes each store by `NavEntry.contentKey`, which `defaultContentKey` derives as `key.toString()`; `AppRoute` is a sealed interface of `data class`/`data object`, so two equal routes produce one key and, as `NavEntry`'s own documentation states, "NavEntries that share the same contentKey will be handled as sharing the same content and/or NavEntryDecorator state". `AppNavigator.push` appends unconditionally. Tracing every `navigator.push` call site in `App.kt` shows the hazard is currently unreachable: within one area, no destination reachable from a route can push that same route again, every attempt and result route carries a freshly generated `attemptId`, and the Practice Builder and Lesson reader are terminal with respect to the routes above them (`onNavigateLesson` replaces rather than pushes). The four areas hold independent stacks, so switching areas cannot collide either. | Leave as-is. The invariant holds today through the shape of the navigation graph rather than through a guard, and adding a duplicate check to `push` would be navigation work this chunk does not own. Recorded so that a future route addition — in particular any new path back to a Topic, Unit or Practice Builder route already on the stack — is understood to be a ViewModel-ownership change and not only a navigation one. | Accepted as-is |
| `CQ-DATA-012` | Bundled content / coverage governance | Medium | High | `docs/content/question-bank-coverage.md`, `tools/learning_question_coverage.py`, `.github/workflows/main.yml` | The question-bank coverage snapshot is stale and nothing gates it. | The document headlines 442 Questions, 401 ACTIVE, 1 774 answer options, 563 sources and 78 empty Subtopics; the bundle holds 478, 437, 1 918, 635 and 69 (71 without an ACTIVE Question). Its generator is a fenced Python block a human pastes into a shell, while the sibling learning snapshot is generated by `tools/` and CI-gated with `--check`, and is current. | Move the generator into `tools/` with `--write`/`--check` and add it to the CI step that already runs its sibling. | Open |
| `CQ-DATA-013` | Curriculum validation / diagnostics | Medium | High | `InitialCurriculumSmokeTest.kt`, `CurriculumDataInitializer.kt`, `AppRoot.kt` | The precise validation errors are discarded at every point where they would be read. | `CurriculumValidator` produces an entity-identified error per defect; the initializer joins them into one exception, `AppStartupStateHolder` catches it as `catch (_: Exception)` and shows a generic string, and `commonMain` has no logging. The gate that actually fires is `assertTrue(validator.validate(...).isEmpty())` with no message, so CI reports `Expected value to be true.` and names no Question. | Assert on the rendered error list rather than on `isEmpty()`. Runtime reporting of a rejected bundle belongs to Part 5. | Open |
| `CQ-DATA-014` | Authored content / identity stability | Medium | High | `CurriculumValidator.kt`, `CurriculumImporter.kt`, `AssessmentReviewLoader.kt`, `docs/content/content-authoring.md` | Nothing compares one bundle revision to the next, so every identity-stability rule is convention-only. | The authoring contract requires a new `Question.id` or `AnswerOption.id` on a material change; the validator sees one document and the importer upserts by primary key without diffing. `AssessmentReviewLoader` reads `isCorrect` from the attempt and `isCorrectAnswer` from the current key, so a key changed under a stable ID makes review contradict itself. Four revisions of history show the convention kept — 0 Questions removed, 0 keys changed, 3 option texts refined under stable IDs. | Diff the bundle against its previous released revision at build time — ID sets, correct-answer sets, option-ID sets, `selectionMode` — and report a violation as an authoring error. | Open |
| `CQ-DATA-015` | Assessment review / content evolution | Low | High | `assessment/session/AnswerOrder.kt`, `AssessmentReviewLoader.kt`, `SavedQuestionContentResolver.kt` | Review fidelity degrades silently when a Question's option set changes. | `withAnswersOrderedFor` shuffles the *current* option list from an `(attemptId, questionId)` seed while the KDoc claims the arrangement the learner actually answered; `getQuestionById` returns retired options too. Adding an option changes both set and order, so an older attempt shows an option the learner never saw, marked unselected. Unreachable on a fresh install. | Bound the KDoc claim to a stable option set, and decide whether the saved-question surface should exclude retired options. | Open |
| `CQ-DATA-016` | Curriculum validation / status semantics | Low | High | `CurriculumValidator.kt` | `TOPIC_WITHOUT_QUESTIONS` is status-blind, so it cannot catch the case it exists to prevent. | `validateMinimumCoverage` builds `questionTopicIds` from every Question regardless of `status`, so a Topic whose Questions are all DEPRECATED passes. The bank shows the shape one level down: `notification_channels` and `dependency_configurations` are ACTIVE Subtopics whose only Questions are DEPRECATED. `LearningCurriculumValidator` makes the equivalent rules ACTIVE-only. | Count ACTIVE Questions in that one rule. Do not add a Subtopic equivalent — an empty ACTIVE Subtopic is recorded product state. | Open |
| `CQ-DATA-017` | Topic discovery / content coverage | Low | High | `TopicBrowserViewModel.kt`, `TopicDetailViewModel.kt`, `TopicDetailScreen.kt` | Subtopic search surfaces Subtopics that Topic detail deliberately hides. | `readCatalog` indexes every ACTIVE Subtopic; `readCurriculum` keeps only Subtopics with an ACTIVE Question. 71 of 361 have none, so about a fifth of Subtopic hits push a route whose `targetIndex` resolves to `-1` and leaves the learner on the Subtopics tab with no row and no explanation. It degrades safely. | Index only practicable Subtopics, or say on the result row that the Subtopic has no questions yet. | Open |
| `CQ-TEST-001` | Bundled-content tests / coupling | Low | High | `InitialCurriculumSmokeTest.kt`, `CurriculumLocalDataPathTest.kt`, `CurriculumImporterTest.kt` | Snapshot assertions mix content policy with incidental derived totals. | Three files independently pin bank-wide counts; adding one Question means editing about nine literals. Per-Topic ACTIVE targets, the SINGLE/MULTIPLE split and the first-row IDs are policy; `1_918`, `528`, `635` and `bundledQuestionsHaveReviewedE1503LevelDistribution` freeze consequence. | Keep the policy assertions. Re-express the derived totals as invariants against the authored collections instead of frozen numbers. | Open |
| `CQ-DATA-018` | Content pipelines / failure modelling | Observation | High | `CurriculumImporter.kt`, `LearningContentLoader.kt` | The assessment pipeline models validation failure as data but lets decode failure escape untyped. | `LearningContentLoader` translates `SerializationException` into a typed `Decode` failure and explains why; the importer returns `Rejected(errors)` for validation only, and `CurriculumLocalDataPathTest` pins the raw `SerializationException` escaping. Both end at the same generic startup screen today. | Record only. Revisit with startup error reporting in Part 5. | Open |
| `CQ-DATA-019` | Authored content / ordering contract | Observation | High | `curriculum/Curriculum.kt`, `CurriculumPersistenceMapper.kt`, `docs/content/question-audit-log.yml` | Array position is an ordering contract the assessment model never states. | `LearningCurriculum` documents it; `Curriculum` and its types do not, and `sortOrder` appears only as `mapIndexed`. Across four revisions 79 Questions were appended with zero index changes, which is what keeps `sort_order` stable on installed devices and the audit log's `n:` index valid. A mid-array insert would renumber every later row with no test failing. | Write the rule into `Curriculum`'s KDoc and `content-authoring.md`. | Open |
| `CQ-DATA-020` | Learning content / source governance | Observation | High | `LearningCurriculumValidator.kt`, `InitialCurriculumContentQualityTest.kt`, `learning_curriculum.json` | Learning sources are ungated by design, and secondary sources are no longer exceptional. | The question bank has a 16-host allowlist; the learning document deliberately has none. Of 443 learning sources, 17 cite `martinfowler.com`, 7 cite `raw.githubusercontent.com` at the moving `androidx-main` branch, 2 `staltz.com`, 2 `blog.ploeh.dk`. Several are plainly primary for the claim. | Record the position deliberately; pin the raw GitHub citations to a tag or commit so the cited text cannot move. | Open |
| `CQ-DATA-021` | Shared history cache / generation atomicity | Medium | High | `AssessmentHistoryStore.kt` | The failed-read retry bumped the refresh generation with a read-modify-write that `invalidate()` could overwrite. | `invalidate()` is atomic (`reloads.update { it + 1 }`) and deliberately takes no lock, but `generationForOneShotRead()` read `reloads.value`, then assigned `currentGeneration + 1` — an *absolute* value. The `failedReadRetry` mutex serialises one-shot readers against each other and not against `invalidate()`, so two invalidations landing between that read and that write were both lost, and because the write was absolute rather than an increment the generation could move *backwards*. A later `completedAttempts()` requiring generation *n* could then be satisfied by a `Settled(n)` produced by a read that started before its own call, which is precisely the stale-history answer the generation exists to prevent. Reaching it needs a failed read plus two concurrent invalidations, so it is an edge case rather than a live defect. | Bump with `reloads.updateAndGet { it + 1 }`, which is atomic against `invalidate()` and monotonic whatever else is incrementing. No deterministic regression test is possible: on a single-threaded test dispatcher nothing can interleave between two non-suspending `MutableStateFlow.value` accesses, and the brief forbids producing the race with timing. The two existing coalescing and retry regressions pin that behaviour is unchanged. | Fixed |
| `CQ-DATA-022` | Assessment completion / operation boundary | Medium | High | `CompleteAssessment.kt`, `AssessmentTakingViewModel.kt`, `AssessmentDataModule.kt`, `CompleteAssessmentTest.kt` | Persisting a completed attempt and marking the shared history cache stale were two statements in a ViewModel, and the second was both cancellable and forgettable. | `assessmentRepository.save(attempt)` is one atomic write transaction, but `historyStore.invalidate()` sat after it as a separate step reached by *resuming a continuation* — and a cancelled job throws at that resumption. The learner leaving the taking destination as the transaction commits was enough to leave the attempt `COMPLETED` in SQLite while the app-scoped cache still held the list from before it, so Progress, the mistake queue, the Mistakes badge, the interview record, Topic learning context and unseen-practice selection all silently omitted a finished assessment for the rest of the process, recoverable only by a manual Retry or a restart. The structural half matters more than the window: nothing in `AssessmentRepository.save`'s signature says a completed write obliges a second call, which is the exact shape of `CQ-STATE-012` and `CQ-STATE-013` — both of which became real bugs because a caller forgot. | Introduce `CompleteAssessment` beside the existing `StartAssessment`, owning scoring, the write and the invalidation as one operation, with the invalidation in `finally`. Unconditional invalidation is correct because the costs are asymmetric: a needless one costs a single re-read that returns the cached attempts, which `history` documents as a normal emission, while a missing one costs correctness. No `NonCancellable` is needed — `invalidate()` does not suspend. The repository cannot own the call itself, because `AssessmentHistoryStore` is built on the repository and the dependency would be a cycle. | Fixed |
| `CQ-DI-004` | Common Koin graph / override safety | Medium | High | `SharedHostStartupTest.kt`, `AndroidLocalData.kt`, `IosLocalData.kt`, `DesktopLocalData.kt`, `WebLocalData.kt` | Nothing can detect a duplicate or overriding definition, and the assertions that claim to cannot. | `koin-core` 4.2.2 `KoinApplication` declares `private var allowOverride = true`; `strictOverride()` flips it and no host and no test calls it, so `InstanceRegistry.saveMapping` replaces an existing index silently and logs only `warn("(+) override index ...")` — invisible, because no host installs a logger. A throwaway probe confirmed it: two modules each declaring `single { Probe(...) }` resolved through `koinApplication { }` yield one instance, the later definition winning. `SharedHostStartupTest`'s four identity assertions state the opposite mechanism ("a second binding would give them separately-cached histories that drift apart", "a duplicate definition would otherwise pass", "a second binding would silently break that", "a second instance would mean the startup screens and the shell could disagree"); a duplicate produces one winner, so `assertEquals(get(), get())` passes either way. What those assertions really pin is `single` not becoming `factory`, which is what the Part 4A falsification run demonstrated. | Call `strictOverride()` in the four `start*LocalDataGraph` functions and in the graph test so an unintended duplicate fails at startup, and correct the four assertion comments to say they pin scope rather than uniqueness. | Open |
| `CQ-DI-005` | Common Koin graph / container boundary | Low | High | `ui/theme/AppearanceTheme.kt`, six journey integration tests | The appearance preference is the one dependency resolved from the global container rather than from the composition's Koin. | `AppearanceTheme` uses `KoinPlatform.getKoinOrNull()`, which is `KoinPlatformTools.defaultContext().getOrNull()` — the `GlobalContext`. Every other common-code resolution goes through `koinViewModel`/`koinInject`, which read `LocalKoinScopeContext`, the composition local that `KoinApplication { }` and `KoinContext { }` override. Six tests compose the real `App()` under `KoinApplication { ... }` — `FocusedLearningJourney`, `LearningProductionContentJourney`, `LearningReaderJourney`, `MixedInterviewJourney`, `ProgressLearningJourney`, `TopicDiscovery` — and all six omit `appearanceModule`; installing it would change nothing, because the holder would live in the composition's Koin while the lookup reads the global one those tests `stopKoin()`. `AppearanceThemeTest`'s helper states the coupling, so it is known rather than hidden. | Resolve the holder through the composition's scope with the same optional guard — preview-safety is orthogonal to which container is consulted — so a host that used `KoinApplication { }` instead of `startKoin` would not silently lose dark mode. | Open |
| `CQ-DI-006` | Graph test / host contract documentation | Low | High | `SharedHostStartupTest.kt` | The graph test describes a three-module, one-platform-binding graph that has not existed for several epics. | The class KDoc reads "Every runtime host installs the same three shared modules plus exactly one platform `CurriculumDatabase` module"; hosts install seven common modules and two platform modules, and the test body installs all nine nine lines below the comment. The inline comment "The only binding a platform host adds on top of the shared modules" is contradicted three lines later by `jvmAppearanceModule`, which supplies `AppPreferenceStorage`. This is the one test whose stated job is to pin the host contract, and it misstates both dimensions a new host can get wrong. | Restate both comments from the module list the test already builds: seven common modules, and exactly two platform bindings, naming `CurriculumDatabase` and `AppPreferenceStorage`. | Open |
| `CQ-DI-007` | Architecture documentation / DI narrative | Low | High | `docs/architecture/overview.md` | The DI narrative contradicts the same document's Runtime Host Coverage table. | Lines 17-23 still say Koin "is started by the Android `Application`, combines the shared curriculum module with the Android database module" and that "Compose injection, and ViewModel DSLs are deferred until a real requirement appears". Ninety lines later the host table correctly lists four hosts and all seven common modules plus two platform modules. Three claims are false: four hosts start Koin, the graph is seven common modules, and both deferred techniques are in use — `org.koin.core.module.dsl.viewModel` for fifteen definitions and Compose injection at twenty `koinViewModel` sites plus one `koinInject`. It reads as current restraint rather than as an E07-era note. | Rewrite the paragraph to match the host table, and restate the deferral sentence as what is still deferred: annotations, the compiler plugin, `singleOf`/`viewModelOf`, qualifiers and scopes. | Open |
| `CQ-DI-008` | App scope / platform dispatcher semantics | Observation | High | `assessment/history/AppCoroutineScope.kt`, `ProgressStateHolder.kt`, `MistakeReviewStateHolder.kt` | `Dispatchers.Default` is a background pool on three targets and the browser's main thread on two. | `AppCoroutineScope` is `CoroutineScope(SupervisorJob() + Dispatchers.Default)`. On Kotlin/JS and Kotlin/Wasm that is the single-threaded event loop. On each `invalidate()` — every completed attempt — `ProgressStateHolder` re-derives the dashboard, `MistakeReviewStateHolder` rebuilds the queue, and `MistakeReviewService.load` issues one `getQuestionById` per unresolved mistake through `AssessmentReviewLoader`, each in its own read transaction. The holders are lazy `single`s, so this starts on the first visit to those areas and then continues for the process whether or not the screens are shown again. No measurement was taken on any host. | Record only; `CQ-DATA-010` owns the per-ID query cost it compounds. Revisit with Part 5 cross-cutting performance. **Partly relieved by the Stage 4E fix pass**: the per-ID query cost this compounds is gone — one derivation and one queue rebuild are now one batched read each instead of one read transaction per identity. What this finding records is unchanged and still open: the derivations themselves still run on every `invalidate()` for the life of the process once their areas have been visited, on the browser's main thread on two targets. The remedy for that is a subscription policy, not a query shape. | Open |
| `CQ-DI-009` | Constructor defaults / graph opt-out | Observation | High | `AssessmentQuestionSelector.kt`, `LearningProgressService.kt`, `MistakeReviewStateHolder.kt` | Three constructors can supply a dependency the graph also owns. | `AssessmentQuestionSelector` and `LearningProgressService` default `performanceDerivation` to `LearningPerformanceDerivation(curriculumRepository)`; `MistakeReviewStateHolder` defaults `learningContentRepository` to `null`. All three are supplied explicitly by their modules, so production shares one stateless derivation and the mistake queue does get its study links. The null is the one that is not harmless in kind: it removes the study-Lesson link silently rather than failing, and six test call sites construct the holder without it. | Record only. Noted because a default that builds or omits a graph-owned dependency is how a future call site would silently opt out of the graph, invisibly at the call site. | Open |
| `CQ-STATE-012` | Learn surfaces / history recovery | High | High | `TopicBrowserViewModel.kt`, `TopicDetailViewModel.kt`, `TopicBrowserViewModelTest.kt`, `TopicDetailViewModelTest.kt` | Neither Learn ViewModel's Retry could recover any state derived from the shared assessment history. | Both observe `AssessmentHistoryStore.history` and derive optional enrichment from it — learning context, Recommended Next and Continue Studying on the browser; learning context and the unresolved-mistake count on Topic Detail. `retry()` reloaded only the curriculum and the study projection. It called neither `historyStore.invalidate()`, which is the only thing that re-reads an attempt table that failed, nor anything that re-runs a derivation over history that read successfully — a re-read of unchanged history is an equal `AssessmentHistory.Loaded` that a `StateFlow` does not emit again. A transient database failure at startup therefore removed every guided surface for the rest of the session, and the Retry button that exists for exactly that restored the catalogue and left the rest gone. `ProgressViewModel.refresh()` and `MistakeReviewViewModel.refresh()` already document and implement both halves (`CQ-STATE-008`); these two never received them. | Mirror the existing pattern in both ViewModels: a `derivations` counter combined into the history collection, and a `retry()` that calls `historyStore.invalidate()` and bumps it. Four jvm regressions pin both failure domains, each confirmed to fail against the previous code. | Fixed |
| `CQ-STATE-013` | Shared history / retryable derivation | Medium | High | `ProgressStateHolder.kt`, `MistakeReviewStateHolder.kt`, `TopicBrowserViewModel.kt`, `TopicDetailViewModel.kt`, `AssessmentHistoryStore.kt` | Four owners now keep a private `derivations` counter to make a retry over unchanged history observable. | The counter exists because `AssessmentHistoryStore.history` is a `StateFlow` and an equal `Loaded` is not re-emitted. Each consumer solves that for itself with the same `MutableStateFlow(0)` plus `combine(history, derivations)` pair, which is why `CQ-STATE-012` was possible at all: the pattern is copied rather than owned. Giving the store's own emissions an identity — a generation on `AssessmentHistory.Loaded`, or an explicit re-derivation signal beside `invalidate()` — would let all four copies and both `retryDerivation()` methods be deleted. | Defer. It is a deliberate consolidation across the store and four consumers with its own test surface, not a bug fix, and it depends on `CQ-STATE-012` having landed. Re-confirmed as the top Stage 4C candidate during the Stage 4B fix pass, which deliberately kept it out of scope to avoid one patch spanning two unrelated themes. Fixed in the Stage 4C fix pass: `history` became a `SharedFlow` with `replay = 1` whose contract is one emission per settled refresh, and all four counters plus both `retryDerivation()` methods are deleted. | Fixed |
| `CQ-STATE-014` | Attempt creation / event versus state | Low | Medium | `AssessmentLaunchViewModel.kt`, `FocusedResultViewModel.kt`, `MixedInterviewResultViewModel.kt`, `AssessmentLaunchCoordinator.kt` | Attempt creation is published as durable state and as a one-shot `Channel` event describing the same fact, reconciled by a handled-callback. | Each of the three sets `Created(attemptId)` on its `MutableStateFlow` and sends the same identity through a `Channel(BUFFERED)` consumed by a `LaunchedEffect` in the composable. `receiveAsFlow` is single-consumer and that collection is cancelled when the destination leaves composition, so an event lost in flight would strand the state on `Created`, where the `start`/`repeat` guards refuse to launch again — the action is then dead for the life of that ViewModel. The window is one dispatch wide and no failure has been observed. | Defer. The remedy is a decision about event delivery shared by all three result surfaces, which is wider than a local fix and belongs with the Part 2 owners. | Deferred |
| `CQ-KMP-001` | Android preference storage / contract | Medium | High | `settings/AndroidAppearanceModule.kt`, `settings/AppPreferenceStorage.kt` | The Android store was the one implementation that could throw out of a contract that forbids it. | `AppPreferenceStorage` states that implementations must not throw and that an unreadable store reports absence, "because failing to remember a preference is not a reason to fail to start". The JVM, iOS and web implementations all guard; Android called `getSharedPreferences` and `getString` unguarded. `getSharedPreferences` throws when the preferences directory is unavailable — the normal state of a credential-protected context before first unlock — and `getString` throws `ClassCastException` if the key holds another type. `AppearanceStateHolder` performs that read synchronously in its constructor, which Koin resolves lazily inside `AppearanceTheme`'s `remember`, i.e. during `AppRoot`'s first composition and above the startup error screen, so a throw is an unrecoverable first-frame crash rather than a forgotten preference. Carried over as the open handoff from Part 3D. | Guard both methods with `runCatching`, using the idiom `JvmAppPreferenceStorage` already uses, and resolve the `SharedPreferences` instance lazily so its own failure is absence too. Not covered by a test: `shared` has no populated Android host-test source set and exercising `SharedPreferences` off-device would mean adding Robolectric. | Fixed || `CQ-CROSS-001` | Attempt result surfaces / retake orchestration | Medium | High | `topic_study/focused_result/FocusedResultViewModel.kt`, `mixed_interview/MixedInterviewResultViewModel.kt`, both UI state files, both screens, both destinations, `assessment/retake/AssessmentRetakeController.kt` | One retake state machine existed twice, under two names. | `RepeatPracticeState` and `RepeatInterviewState` were structurally identical six-case sealed interfaces, driven by identical `repeatPractice`/`repeatInterview`, `onRetakeEventHandled` and `setRepeatState` bodies over the same `AssessmentRetakeService.createRetake`, and reported through two one-case event hierarchies (`FocusedResultEvent`, `MixedInterviewResultEvent`) collected by two identical `LaunchedEffect` blocks. `CQ-BUG-003` — the post-persistence re-entry window — had to be found and fixed in both copies independently, which is the concrete cost this records. Neither copy could be tested without a full result ViewModel and its four fakes. | Extract `AssessmentRetakeController` in `assessment/retake/`, owning `AssessmentRetakeState`, the buffered `AssessmentRetakeCreated` event, the re-entry guard and the identity-matched release. Both ViewModels delegate; each keeps only whether there is a loaded result to repeat, and each screen keeps its own wording. | Fixed |
| `CQ-CROSS-002` | Attempt result surfaces / state ownership | Low | High | `FocusedResultUiState.kt`, `MixedInterviewResultUiState.kt`, both screens | Retake state was stored inside the result content it is not part of. | `Content.repeatPracticeState` / `Content.repeatInterviewState` put a running action inside a record of a settled fact: the score and the transcript of a completed attempt cannot change while the screen is open, and the retake state changes on every press. The conflation forced every transition through a `setRepeatState` that cast to `Content` and silently dropped the write otherwise — unreachable in practice, but a transition that can disappear is not a state machine anyone can reason about. | Publish retake state as its own `StateFlow` beside `uiState`, and pass it to the screen as its own parameter. The "only retake a loaded result" rule becomes an explicit guard in the ViewModel rather than an implicit consequence of a cast. | Fixed |
| `CQ-CROSS-003` | Mixed interview result / derivation placement | Low | High | `mixed_interview/MixedInterviewResultViewModel.kt`, `mixed_interview/TopicAnswerCounts.kt` | Per-Topic aggregation was a suspending ViewModel method built on a mutable counter class. | `loadTopicPerformance` grouped the transcript by `topicId`, counted with a `private class TopicCounts(var questionCount, var correctCount)`, computed percentages, and resolved Topic names, all in one method. The counting is a deterministic transformation with no I/O in it, but it could only be exercised through the ViewModel, a fake curriculum repository and a fake review loader; the mutable holder existed only because the derivation was written imperatively in the wrong layer. | Extract the pure `List<ReviewQuestionItem>.topicAnswerCounts()` and an immutable `TopicAnswerCounts` that requires a positive denominator. The ViewModel keeps the name resolution, which is the only part that needs a repository. | Fixed |
| `CQ-CROSS-004` | Review surfaces / mutation boundary | Low | High | `FocusedResultViewModel.kt`, `MixedInterviewResultViewModel.kt`, `MistakeReviewViewModel.kt`, `assessment_review/AssessmentReviewModels.kt` | One mutation-boundary rule, written three times over three traversals. | "A review surface may only toggle a Question it currently shows as `Available`" was implemented independently in all three `toggleSaved` methods, twice over `content.questions` and once over `content.mistakes.reviewItem`, and documented three times. The three surfaces share one saved-state holder, so accepting a save on one that the others would refuse is a real inconsistency the duplication makes possible. | One `ReviewQuestionItem.isAvailableFor(questionId)` predicate in `assessment_review`, called by all three. | Fixed |

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
| Part 3A addendum | Bundled curriculum as production data | Complete | `6e198c5dc14140f80736fe7685e4334f9baef8be` | Both bundled JSON documents audited in full (478 Questions, 135 Lessons), 14 content-boundary production files re-read, 6 consumers, 8 tests and 5 authoring contracts | 10 | 0 | Whole-dataset programmatic checks over identity, references, answer semantics, status, ordering, content shape and four bundle revisions; `git status --short` clean; no Gradle task required because nothing executable changed | High 0, Medium 3, Low 4, Observations 3. Audit only: no production Kotlin, bundled JSON, test or content document was changed. Closes the content exclusion the Part 3A code review recorded. |
| Part 4A addendum | Common Koin graph and lifetimes | Complete | `8b2be06abc375fa930172a03a474280e55cb3445` | Seven common modules plus eight platform modules re-counted mechanically, 20 `koinViewModel` sites across 15 files, 4 host bridges and 4 platform roots, 30 singleton classes re-read for fields, 8 app-scoped holders, 12 Koin-touching test files, and 5 third-party libraries read from source | 6 | 0 | One throwaway duplicate-definition probe (`:shared:jvmTest`, passed, then deleted); `git status --short` clean; no other Gradle task, because no executable file changed | High 0, Medium 1, Low 3, Observations 2. Audit only: no Koin definition, module, host bridge, comment or test was changed. Adds the enforcement, implicit-contract and test-coverage layer the Part 4A record did not reach. |
| Stage 4A fix pass | Cross-cutting: Learn-surface history recovery and the Android preference store | Complete | `b8a214a` plus the working tree | 2 ViewModels, 4 state holders, 1 shared store, 1 platform store, and the 2 corresponding jvm test files, re-read in full | 4 (1 High, 2 Medium, 1 Low) | 2 fixed, 2 deferred | `:shared:jvmTest`, `:shared:allTests`, `:androidApp:assembleDebug`, `:androidApp:lintDebug` | Not a planned chunk. A targeted re-audit of state ownership, coroutine lifecycle, cancellation and event-versus-state across the current tree, taken outside the Part sequence; **Part 4B remains the next planned chunk and was not started**. |
| Stage 4B fix pass | Attempt result ownership: the retake state machine, result derivation, and the review mutation boundary | Complete | `e398dc7` plus the working tree | 2 result ViewModels, 2 UI state files, 2 screens, 2 destinations, 1 further review ViewModel, 1 shared review model file, plus the 4 corresponding test files, re-read in full; 23 further presentation/domain files read for comparison | 9 (4 fixed, 1 deferred, 3 recorded, 1 not a defect) | 4 fixed | `:shared:compileKotlinJvm`; `:shared:jvmTest` (1545 tests); `:shared:check`; `:androidApp:assembleDebug`; `git diff --check`; `git status --short` | Not a planned chunk. An architecture-focused pass over ownership, duplication and testability in the two attempt-result surfaces, taken outside the Part sequence. `CQ-CROSS-001`–`004` are fixed; `CQ-STATE-013` is re-confirmed as deferred and is the leading Stage 4C candidate. **The planned Part 4B — host composition roots — is unrelated to this pass, remains the next planned chunk, and was not started.** |
| Stage 4C fix pass | Shared assessment-history refresh contract: state/event semantics and the store's own type surface | Complete | `6324153` plus the working tree | 1 store, 1 interface, 6 consumers and 1 test file read in full; 5 further state holders and UI-state files read for comparison | 6 (2 fixed, 1 deferred, 2 not a defect, 1 accepted as-is) | 2 fixed | `:shared:compileKotlinJvm`; `:shared:jvmTest` (1549 tests); `:shared:check`; `:androidApp:assembleDebug`; `git status --short` | Not a planned chunk. A state-model, API-contract and boundary-correctness pass bounded to one cluster: the shared history store and everything that derives from it. `CQ-STATE-013` and `CQ-STATE-015` are fixed; `CQ-STATE-014` and `CQ-TYPE-001` remain deferred. **The planned Part 4B — host composition roots — is unrelated to this pass, remains the next planned chunk, and was not started.** |
| Stage 4E fix pass | Historical curriculum resolution: one batched read instead of one round trip per stable ID | Complete | `272a509` plus the working tree | 1 repository interface, 1 DAO, 1 Room repository, 5 per-ID call sites, and the 4 app-scoped/ViewModel consumers that drive them; 31 test doubles and 10 integration tests updated | 6 (1 high, 1 medium, 4 low) | 1 cluster (`CQ-DATA-010`) | `:shared:jvmTest` (1559 tests), `:shared:check`, `:androidApp:assembleDebug`, `git diff --check` | High 0 new; the pass converted `CQ-DATA-010` from *Needs measurement* to *Fixed* by supplying the trigger analysis it lacked, and relieved half of `CQ-DI-008`. Five deferred findings recorded for 4F. See review record below. |
| Stage 4F fix pass | Regression protection: superseded, cancelled and unread asynchronous results must not reach the learner as an answer | Complete | `cbd7d5c` plus the working tree | All 131 test files inventoried by name; every production file without a same-named test checked for indirect coverage; every broad `catch` in `commonMain` and the platform source sets; 1 new test file, 4 test files extended, 3 test doubles gated | 5 (2 high, 2 medium, 1 low) | 1 cluster (`CQ-TEST-001`, `CQ-TEST-002`, `CQ-TEST-003`) | `:shared:jvmTest` (1569 tests), `:shared:check`, `:androidApp:assembleDebug`, `git diff --check` | No production-code change. Ten tests added, each verified against a deliberately broken production tree and restored. Two findings deferred to 4G. See review record below. |

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

## Part 3A Content Addendum — Bundled Curriculum as Production Data

The Part 3A record above audited the curriculum **code** and states explicitly that "the two
bundled JSON resources and the exported Room schemas were read as data, not audited as content."
This addendum closes that exclusion: it audits the authored curriculum itself as production data
and asks what stops invalid or internally inconsistent content reaching a learner. It does not
revisit, renumber or restate any Part 3A conclusion; where the two meet — import reconciliation,
answer-option retirement, `CQ-BUG-004` — the earlier record stands and is cited rather than redone.

- **Commit reviewed:** `6e198c5dc14140f80736fe7685e4334f9baef8be`, working tree clean at start.
- **Authored data inspected in full:** `initial_curriculum.json` (17 Topics, 361 Subtopics, 478
  Questions, 1 918 answer options, 528 correct-answer entries, 635 sources over 335 distinct URLs)
  and `learning_curriculum.json` (30 Units, 135 Lessons, 402 Sections, 2 844 blocks, 443 sources).
  Every check below was run programmatically over the whole document, not over a sample.
- **Production files re-read for the content boundary (14):** `Curriculum`, `Topic`, `Subtopic`,
  `Question`, `AnswerOption`, `SourceReference`, `ContentStatus`, `AnswerSelectionMode`,
  `QuestionLevel`, `CurriculumJsonCodec`, `BundledCurriculumSource`, `CurriculumValidator`,
  `AuthoredContentChecks`, `CurriculumValidationErrorCode`; plus `CurriculumImporter`,
  `CurriculumPersistenceMapper`, `CurriculumDao`, `LocalCurriculumRepository` and
  `CurriculumEntityMapper` for the invariants they assume, and `LearningCurriculumValidator`,
  `LearningContentLoader` and `BundledLearningCurriculumSource` for the second pipeline.
- **Consumers read for what they assume about content (6):** `AssessmentReviewLoader`,
  `SavedQuestionContentResolver`, `AnswerOrder`, `AssessmentQuestionSelector`,
  `TopicDetailViewModel`, `TopicBrowserViewModel`, plus `AppRoot`/`AppStartupStateHolder` for what
  a rejected bundle actually shows a user.
- **Tests read (8):** `InitialCurriculumSmokeTest`, `InitialCurriculumContentQualityTest`,
  `CurriculumValidatorTest`, `CurriculumImporterTest`, `CurriculumLocalDataPathTest`,
  `BundledLearningCurriculumTest`, `LearningCurriculumValidatorTest`, and the CI step in
  `.github/workflows/main.yml`.
- **Authoring contracts read for the invariants they claim (5):** `content-authoring.md`,
  `question-validation.md`, `question-bank-coverage.md`, `learning-content-authoring.md`, and
  `docs/architecture/persistence.md` on `sort_order` and curriculum retirement.
- **Content edits made: none.** This pass is an audit. No production Kotlin, no bundled JSON, no
  test and no content document was changed; only this ledger.

### The authored-content model

Authored content reaches a learner through six layers, and each owns a different guarantee.

| Layer | Component | What it guarantees |
| --- | --- | --- |
| Authoring | `initial_curriculum.json`, reviewed against `content-authoring.md` and `question-validation.md` | Editorial meaning: the claim is true, the distractors are plausible, the source supports the claim. Not machine-checked. |
| Serialization | `CurriculumJsonCodec` (`Json { encodeDefaults = true }`) | Shape and vocabulary. Unknown keys are **not** ignored, so a misspelled field fails rather than defaulting silently; a missing required field and an unrecognised enum token both fail. |
| Validation | `CurriculumValidator` | Identity, referential integrity, hierarchy agreement, answer and source shape, placeholder absence. One deterministic pass; every defect reported, nothing repaired. |
| Import | `CurriculumImporter` | Atomic whole-graph upsert with `PRAGMA defer_foreign_keys = ON`, and the one reconciliation the model performs: an incoming Question's answer-option set. |
| Persistence | `topic`/`subtopic`/`question`/`answer_option`/`question_correct_answer`/`question_source` | Stable identity plus `sort_order`, so display order survives without depending on SQLite row order. |
| Reconstruction | `CurriculumDao` + `LocalCurriculumRepository` | Eligibility. Every `getActive*` query joins Topic and Subtopic status, so status is enforced in SQL rather than by each caller. |

Two properties of that chain are worth naming before the findings.

**Status is a query-layer guarantee, not a data-layer one.** Nothing in the JSON or the validator
forbids an ACTIVE Question under a DEPRECATED Subtopic; every active query joins
`t.status = :activeStatus AND s.status = :activeStatus`, so retiring a parent retires everything
under it without a content migration, and `getQuestionById`, `getTopicById` and `getSubtopicById`
deliberately bypass the filter so history stays resolvable. This is the right place for the rule
and it is applied consistently across all seven active-question queries.

**Ordering is authored array position and nothing else.** No model type carries an order field;
`toPersistenceSnapshot` assigns `sortOrder` with `mapIndexed`, and every ordered read has an
explicit `ORDER BY sort_order`. `LearningCurriculum`'s KDoc states this contract for the learning
document — "list position is the ordering contract and no sort-order field is stored" — while
`Curriculum` states it nowhere.

Invariants that exist only by convention, with no owner in code:

- A Question is never *deleted* from the bundle; retirement is `ContentStatus.DEPRECATED`.
  `docs/architecture/persistence.md` says absence is not a deletion signal and
  `CurriculumImporterTest.absenceFromLaterCurriculumIsNotADeletionSignal` pins the importer's half
  of it, but nothing checks the authored side.
- New Questions are *appended*; existing array positions do not move.
- A material change takes a new `Question.id` or `AnswerOption.id`.
- `docs/content/question-bank-coverage.md` is regenerated in the same PR that changes the bank.

### The shipped dataset

Every identity, referential-integrity, answer-semantics, status and ordering check below was run
over all 478 Questions and all 135 Lessons.

**Identity — clean.** No duplicate Topic, Subtopic or Question ID; no duplicate answer ID inside a
Question and none repeated across the whole bank either; no ID blank, whitespace-padded, or outside
`[a-z0-9_]+`; no ID shared between the Topic, Subtopic and Question namespaces. Two ID schemes
coexist — 90 Questions use the original `<subtopic>_NNN` form and the rest use descriptive slugs,
of which 145 happen to begin with their `subtopicId` — but neither scheme is *read* anywhere, so a
re-homed Question keeps a now-misleading prefix without any behavioural consequence. Answer-option
suffix letters are demonstrably *not* positional: 156 Questions carry non-contiguous or unordered
suffixes such as `a, b, e, f`, which is exactly what `content-authoring.md`'s "do not use list
position or index as answer identity" asks for and is evidence the rule has been followed through
several rounds of option replacement.

**Referential integrity — clean.** Every Subtopic resolves to an existing Topic; every Question
resolves to an existing Topic and Subtopic; every Question's `topicId` agrees with the Topic owning
its Subtopic; every one of the 528 correct-answer entries names an option of its own Question;
there are no orphaned entities. The learning document is equally clean: all 30 Unit home Topics,
all primary and supporting Subtopic references and all `relatedLessonIds` resolve, with no
duplicate Unit or Lesson ID.

**Answer semantics — correct, including the case that looks wrong.** All 431 SINGLE Questions
carry exactly one correct answer, which the validator enforces from both sides
(`NO_CORRECT_ANSWERS` and `SELECTION_MODE_CORRECT_ANSWER_MISMATCH`). Of the 47 MULTIPLE Questions,
38 have two correct answers, 6 have three, and 3 have exactly one. That last group is **not** a
defect: `content-authoring.md` states that `selectionMode` "must not be inferred from
`correctAnswerIds.size`: doing so exposes hidden answer-key information through the input
controls. A MULTIPLE question with one correct answer is valid and still uses multi-selection
controls", the PR checklist repeats it, and `question-bank-coverage.md` already counts the three.
All 47 carry the "Select all that apply." prompt, enforced twice over by
`InitialCurriculumSmokeTest.authoredMultipleQuestionsTellReaderToSelectAllThatApply` and
`InitialCurriculumContentQualityTest.bundledMultipleSelectionQuestionsTellTheReaderTheModeExplicitly`.
No Question keys every one of its options.

**Status — internally consistent.** 437 ACTIVE and 41 DEPRECATED Questions; no DEPRECATED Topic or
Subtopic exists at all, so no ACTIVE child currently sits under a retired parent. Two ACTIVE
Subtopics — `notification_channels` and `dependency_configurations` — hold Questions of which every
one is DEPRECATED, which is the shape `CQ-DATA-016` is about one level down.

**Ordering — stable, and demonstrably so.** Comparing the working tree against `581748b`,
`77408e5`, `87bc851` and `20347fb`: **no Question has ever been removed**, and **no surviving
Question has ever changed array index** — 79 were appended over that range and every existing
position held. That is what makes `sort_order` stable across releases on installed devices and what
keeps the `n:` positional index in `docs/content/question-audit-log.yml` meaningful. The Subtopics
array is grouped by Topic in 17 contiguous runs, so the globally-indexed `sortOrder` still produces
the authored order within each Topic. The Questions array is *not* grouped — 478 Questions form 58
Topic runs — but nothing depends on it, because `AssessmentQuestionSelector` shuffles its candidate
pool before narrowing.

**Content shape — clean.** No blank Topic, Subtopic, Question, answer, explanation or source text;
no authoring placeholder; every Question has at least two options (472 have four, 6 have five), at
least one correct answer, a non-blank explanation and at least one source; every one of the 635
source URLs is a syntactically valid `https://` URL with a host and a path, and every host is on
the 16-entry allowlist in `InitialCurriculumContentQualityTest`. No duplicated stem, no duplicated
explanation, no two Questions sharing an identical option set. A Jaccard near-duplicate scan over
all 114 003 stem pairs surfaced two above 0.62; both were inspected and both are false positives
(`dagger_module_binding_declarations` versus `dagger_component_graph_root` ask about `@Module`
versus `@Component`; `sharedflow_001` versus `di_scopes_001` share only the "Which statements
about … Select all that apply." frame, and in each pair one member is already DEPRECATED). A
0.9-threshold scan over option texts within each Question surfaced one pair, in
`noinline_vs_crossinline_lambda`, which is a deliberate swapped-term distractor. **No content
defect was found in the shipped dataset.**

### Validator responsibility

`CurriculumValidator` enforces 39 error codes across identity, hierarchy, answer shape,
correct-answer references, selection-mode arity, explanation, sources and placeholders, in one
non-failing-fast deterministic pass, and `CurriculumValidatorTest` covers it with 22 tests
including ordering determinism, multi-error accumulation, the `TODO()`-is-not-a-placeholder
carve-out and the DEPRECATED-is-not-itself-a-defect rule. The importer assumes exactly what the
validator promises and says so: `CurriculumDao.deleteAnswerOptionsForQuestionExcept` documents
that `keepAnswerIds` "is never empty… CurriculumValidator requires at least two answers per
question", and `CurriculumEntityMapper` calls
`AnswerSelectionMode.valueOf`/`QuestionLevel.valueOf` on the strength of the codec having already
rejected an unknown token. Those assumptions hold.

Three responsibility gaps remain, and each is a finding below rather than a restatement here:
coverage counting ignores status (`CQ-DATA-016`), nothing validates one bundle revision against the
previous one (`CQ-DATA-014`), and the precise diagnostics the validator produces are discarded at
every point where a human would read them (`CQ-DATA-013`). A fourth item is a deliberate asymmetry
rather than a gap: the assessment pipeline models a validation failure as data but lets a decode
failure escape untyped, where the learning pipeline models both (`CQ-DATA-018`).

Malformed curricula that pass validation today and would produce wrong behaviour: a Topic all of
whose Questions are DEPRECATED (passes `TOPIC_WITHOUT_QUESTIONS`, then browses as a Topic with
nothing to practise); an ACTIVE Question under a DEPRECATED Subtopic (passes, then becomes
unreachable through every eligibility query while still resolving historically); and a Question
whose entire option set is keyed (passes, and scores correct for a learner who selects everything).
None of the three occurs in the shipped bank.

### Bundled-content tests

The suite divides cleanly, and the division is the point.

**Semantic and invariant tests, all worth keeping.**
`bundledInitialCurriculumPassesStructuralValidation` runs the real validator over the real bundle,
and `bundledInitialQuestionsPreserveE0604ContentShape` asserts per-Question answerability.
`InitialCurriculumContentQualityTest` enforces five editorial invariants no validator could own —
unique stems, the MULTIPLE prompt, the approved-host list, the anti-cue keyed-answer length ratio,
and the absolute-word distribution across keyed answers and distractors — and the last two are
unusually good: they encode *why* the rule exists rather than a literal.
`CurriculumImporterTest`'s 22 cases cover the evolution semantics directly, including re-homing,
absence-is-not-deletion, retained historical options and untouched learner records.
`LearningCurriculumValidatorTest` and `LearningContentLoader`'s all-or-nothing contract do the
same for the second document.

**Snapshot tests, mixed.** Some frozen numbers are content policy and belong in CI: the per-Topic
ACTIVE counts in `bundledInitialQuestionDistributionMatchesCurrentTargets` express the
"no Topic starves" intent, and the SINGLE/MULTIPLE split is a deliberate balance. Others freeze
consequence: `1_918` answer options, `528` correct-answer rows and `635` sources in
`CurriculumLocalDataPathTest` are derived totals that say nothing a per-row invariant would not say
better, and `bundledQuestionsHaveReviewedE1503LevelDistribution` is named after the issue that
produced it. `CQ-TEST-001` separates the two and recommends keeping the first group.

**The gap on the other side.** Nothing automatically protects: that every ACTIVE Topic has at least
one ACTIVE Question; that a Question is never removed between revisions; that a correct-answer key
never changes under a stable ID; that the Questions array stays append-only; or that
`docs/content/question-bank-coverage.md` matches the bank.

### Historical and user-state compatibility

Persisted learner state references curriculum identity in four places: `question_attempt` and
`question_attempt_selected_answer` (foreign-keyed onto `question` and `answer_option`),
`saved_question` and `mistake` resolution (by stable ID, no foreign key), and study progress (by
Lesson ID). The contracts are explicit and mostly safe:

| Content change | What happens | Safe? |
| --- | --- | --- |
| Text or explanation edited, ID kept | Attempt correctness is persisted occurrence data and is never recomputed; review and saved questions render the new text | Yes, and this is the documented minor-edit case |
| `level` changed, ID kept | Persisted correctness unaffected; level-derived figures reclassify. Happened twice in history (`dependency_direction_domain_framework_types`, `parent_cancellation_propagates_children`) | Yes |
| Question re-homed to another Subtopic | Validator accepts it, importer handles it since `CQ-BUG-004`; derived per-Subtopic figures move with the content. Happened eight times | Yes |
| Options added or removed, ID kept | Removed-and-historically-selected options are retained and marked DEPRECATED; `getQuestionById` still returns them, so review keeps the original text. Added options change the derived review order — see `CQ-DATA-015` | Mostly |
| Correct answer changed, ID kept | Review reports the persisted verdict beside the *current* key and can contradict itself — see `CQ-DATA-014`. Forbidden by `content-authoring.md`; never yet done | **No, and unenforced** |
| Question becomes DEPRECATED | Dropped from eligibility, still resolvable by ID; saved questions and review unaffected | Yes |
| Question deleted from the bundle | A fresh install never sees it; an upgraded install keeps the row ACTIVE and keeps serving it. Never yet done | **No, and unenforced** |
| ID reused for different content | Nothing detects it; history silently re-points at the new meaning | **No, and unenforced** |

The three unsafe rows share one cause and one fix direction, recorded once as `CQ-DATA-014`.

### Findings

Ordered by severity, then by confidence.

- **`CQ-DATA-012` — Medium / High — the question-bank coverage snapshot is stale and ungated.**
  `docs/content/question-bank-coverage.md` headlines 442 total Questions, 401 ACTIVE, 1 774 answer
  options, 563 sources over 323 URLs and 78 empty Subtopics; the bundle holds 478, 437, 1 918, 635
  over 335, and 69 Subtopics with no Question at all (71 with no ACTIVE Question, 290 with at least
  one). The drift is exactly the two most recent authoring epics. Its generator is a fenced Python
  block inside the Markdown that a human pastes into a shell, while the sibling snapshot
  `docs/content/learning-question-coverage.md` is produced by `tools/learning_question_coverage.py`
  and gated in CI by `python3 tools/learning_question_coverage.py --check` — and is current.
  `content-authoring.md` instructs "regenerate its tables in the same PR that changes the bank",
  which is a rule with no enforcement. **Failure mode:** the document exists so a later session
  does not re-derive the coverage triage, and its Empty-subtopics section explicitly separates real
  gaps from deliberate ones; planning an expansion against it now plans against a bank 36 Questions
  out of date. **Direction:** move the generator into `tools/` with the same `--write`/`--check`
  pair and add it to the CI step that already runs its sibling.

- **`CQ-DATA-013` — Medium / High — validation diagnostics are discarded everywhere they would be
  read.** `CurriculumValidator` produces a `CurriculumValidationError(code, entityId, message)` per
  defect. `CurriculumDataInitializer` joins every message into one `IllegalStateException`;
  `AppStartupStateHolder.initialize` catches it as `catch (_: Exception)` and renders the generic
  `app_startup_error` string; `commonMain` contains no logging of any kind. The gate that will
  actually fire is the build-time one, and it is
  `assertTrue(CurriculumValidator().validate(initialCurriculum).isEmpty())` —
  `InitialCurriculumSmokeTest`, no message argument. **Failure mode:** an author breaks one of 478
  Questions and CI reports `Expected value to be true.`, naming neither the error code nor the
  Question; the entity-identified error model the validator was built around is never surfaced to
  anyone. **Direction:** assert on the rendered error list rather than on `isEmpty()`, which costs
  one line and makes every one of the 39 codes diagnostic. What a rejected bundle should do at
  runtime is a startup-reporting question and belongs to Part 5.

- **`CQ-DATA-014` — Medium / High — nothing compares one bundle revision to the next, so every
  identity-stability rule is convention-only.** `content-authoring.md` requires a new `Question.id`
  with the old one DEPRECATED when the concept, claim, correct answer or scenario changes, and a
  new `AnswerOption.id` when an option's meaning changes. `CurriculumValidator` validates one
  document in isolation; `CurriculumImporter` upserts by primary key and never diffs against the
  persisted copy. The convention has held — across `581748b` to the working tree no Question was
  removed and no correct-answer key changed; three option texts were rewritten under a stable ID
  (`di_scopes_001_b`, `di_hilt_viewmodel_scope_b`,
  `compose_strong_skipping_instance_equality_b`) and all three are refinements of the same claim —
  but nothing would detect a violation. **Failure mode:** `AssessmentReviewLoader.loadQuestion`
  takes `isCorrect` from the persisted attempt and `isCorrectAnswer` from the *current*
  `question.correctAnswerIds`, so a key changed under a stable ID makes the review screen tell a
  learner they answered correctly while marking the option they chose incorrect. Deleting a
  Question from the JSON is the same class: fresh installs lose it, upgraded installs keep serving
  it ACTIVE, and the two populations diverge permanently. **Direction:** a build-time diff of the
  bundle against its previous released revision — ID sets, correct-answer sets, option-ID sets,
  `selectionMode` — reported as an authoring error. This is the single highest-value missing
  guarantee in the content boundary.

- **`CQ-DATA-015` — Low / High — review fidelity degrades silently when an option set changes.**
  `AnswerOrder.withAnswersOrderedFor` shuffles the *current* option list with a seed derived from
  `(attemptId, questionId)`, and `AssessmentReviewLoader`'s KDoc claims the result orders the
  answers "exactly as they were ordered while the attempt was being taken". `getQuestionById`
  returns ACTIVE and DEPRECATED options together. Adding an option to an existing Question
  therefore changes both the set and the derived order, so review of an older attempt shows an
  option the learner never saw, marked `wasSelected = false` and indistinguishable from one they
  saw and rejected, in an arrangement they never saw. `SavedQuestionContentResolver` has the milder
  form: a saved Question renders retired options as ordinary wrong answers. Unreachable on a fresh
  install, because no shipped option is DEPRECATED. **Direction:** bound the KDoc claim to a stable
  option set, and decide whether the historical resolver should exclude retired options for the
  saved-question surface, which has no attempt whose arrangement it is reproducing.

- **`CQ-DATA-016` — Low / High — the validator's one coverage rule is status-blind.**
`CurriculumValidator.validateMinimumCoverage` builds `questionTopicIds` from
`curriculum.questions` without filtering `status`, so `TOPIC_WITHOUT_QUESTIONS` passes for a Topic
whose every Question is DEPRECATED — precisely the case it exists to prevent. The bank
demonstrates the shape one level down: `notification_channels` and `dependency_configurations` are
ACTIVE Subtopics whose only Questions are all DEPRECATED. Contrast `LearningCurriculumValidator`,
which makes every minimum-content rule ACTIVE-only and says why. **Failure mode:** retiring the
last live Question in a Topic ships a Topic that loads, lists Subtopics and offers nothing to
practise, with no build failure. **Direction:** count ACTIVE Questions in that one rule. Do *not*
add the Subtopic equivalent: an ACTIVE Subtopic with no Question is recorded product state, not a
defect.

- **`CQ-DATA-017` — Low / High — Topic search surfaces Subtopics no screen can show.**
  `TopicBrowserViewModel.readCatalog` indexes every ACTIVE Subtopic for search, while
  `TopicDetailViewModel.readCurriculum` deliberately drops Subtopics with no ACTIVE Question
  ("Only Subtopics that can actually be practised become rows"). 71 of 361 ACTIVE Subtopics
  currently have no ACTIVE Question, so roughly a fifth of Subtopic search hits push
  `AppRoute.Topic(topicId, subtopicId)`, `TopicDetailTabs` resolves `targetIndex` to `-1`, and the
  learner lands on the Subtopics tab at the top with no row and no explanation. The behaviour
  degrades safely — the effect is explicitly written to handle `-1` — so this is a dead-end search
  result, not a fault. The taxonomy being wider than the bank is a recorded product decision, so
  the fix belongs to the search surface. **Direction:** either index only practicable Subtopics or
  say on the result row that the Subtopic has no questions yet.

- **`CQ-TEST-001` — Low / High — bundled-bank snapshot assertions mix content policy with
  incidental totals.** `InitialCurriculumSmokeTest` pins 17/361/478/437/41/431/47, a whole-bank and
  per-status level distribution, and a 17-entry per-Topic ACTIVE map; `CurriculumLocalDataPathTest`
  independently pins 17/361/478, `1_918`, `528`, `635`, `23`, and a first-row ID at each level of
  the hierarchy; `CurriculumImporterTest` pins 361/478 again. Adding one ACTIVE APPLIED Question to
  `testing` requires editing roughly nine literals across three files. Policy worth keeping: the
  per-Topic ACTIVE targets, the SINGLE/MULTIPLE split, and the first-row IDs, which are the only
  thing pinning authored order end to end. Incidental: `1_918`, `528` and `635`, which are derived
  totals, and `bundledQuestionsHaveReviewedE1503LevelDistribution`, which freezes the state one
  issue happened to leave. **Direction:** keep the policy assertions as they are; re-express the
  derived totals as invariants against the authored collections rather than as frozen numbers, so
  ordinary authoring stops paying for them.

- **`CQ-DATA-018` — Observation — the two pipelines model failure asymmetrically.**
  `LearningContentLoader` catches `SerializationException` and rethrows
  `LearningContentLoadException(LearningContentLoadFailure.Decode(cause))`, explaining that a
  packaging fault and a content fault should not send a reader to the same file.
  `CurriculumImporter` returns `CurriculumImportResult.Rejected(errors)` for validation but lets a
  decode failure escape untyped, which `CurriculumLocalDataPathTest` pins as a raw
  `SerializationException`. Both end at the same generic startup screen today, so nothing
  user-visible differs. Recorded, not a defect on current evidence.

- **`CQ-DATA-019` — Observation — array position is an ordering contract the assessment model does
  not state.** `LearningCurriculum` documents it; `Curriculum`, `Topic`, `Subtopic` and `Question`
  do not, and `sortOrder` appears only as `mapIndexed` inside `toPersistenceSnapshot`. The
  convention has held perfectly — 79 Questions appended over four revisions with zero index changes
  — and it is load-bearing twice over: it keeps `sort_order` stable on installed devices, and it
  keeps the `n:` positional index in `docs/content/question-audit-log.yml` valid. A mid-array insert
  would renumber `sort_order` for every later Question on every device and silently invalidate every
  recorded `n`, with no test failing. The natural homes for the rule are `Curriculum`'s KDoc and
  `content-authoring.md`.

- **`CQ-DATA-020` — Observation — learning-content sources are ungated by design, and secondary
  sources are no longer exceptional.** `InitialCurriculumContentQualityTest` enforces a 16-host
  allowlist for the question bank. The learning document has no equivalent, deliberately:
  `LearningCurriculumValidator.validateSources` and Rule 9 of `learning-content-authoring.md` both
  argue that an allowlist would reject valid documentation. The consequence is visible in the data:
  of 443 learning sources, 17 cite `martinfowler.com`, 7 cite `raw.githubusercontent.com` against
  the moving `androidx-main` branch, 2 cite `staltz.com` and 2 cite `blog.ploeh.dk`, against a
  contract that calls secondary sources exceptional. Several are plainly the primary source for the
  claim they support. The branch-pinned raw URLs are the part with a mechanical answer — a tag or
  commit pins the text the Lesson actually cites. Recorded so the position is a decision.

### What is already strong

- **The shipped data is clean on every mechanical axis.** Zero duplicate or malformed IDs at any
  level, zero broken references, zero hierarchy disagreements, zero duplicate stems, explanations or
  option sets, zero placeholders, and 635 well-formed allowlisted source URLs — verified over the
  whole document, not sampled. The same holds for the learning document's 30 Units and 135 Lessons.
- **Status enforcement lives in SQL.** All seven active-question queries and the active-subtopic
  query join Topic and Subtopic status, so retiring a parent retires its children with no content
  migration and no caller able to forget. The three by-ID resolvers bypass it on purpose, and
  say so.
- **Every ordered read orders explicitly.** No query anywhere relies on SQLite row order.
- **Selection-mode semantics are right, including the counter-intuitive case.** SINGLE arity is
  enforced from both directions, and MULTIPLE-with-one-correct is a reasoned product decision about
  not leaking the key through the input controls — documented, counted, and prompt-enforced.
- **The validator reports rather than repairs, exhaustively and deterministically.** One pass, every
  independent defect, no trimming or de-duplication that would hide the defect it found — and 22
  tests holding that shape, including determinism and the `TODO()` carve-out.
- **The learning pipeline validates cross-document references at runtime, all-or-nothing.**
  `LearningContentLoader` checks the learning document against the bundled `Curriculum` it was
  authored against rather than the imported Room copy, and refuses partial success.
- **Strict JSON.** No `ignoreUnknownKeys`, so a misspelled or stray field fails the build instead of
  defaulting silently, and `@SerialName` pins every `LearningBlock` discriminator against class
  renames.
- **Authoring discipline is real and measurable.** Across four bundle revisions: no Question
  deleted, no array position disturbed, no correct-answer key changed under a stable ID, and option
  IDs left alone while option text was refined. The rules in `content-authoring.md` are being
  followed; what is missing is anything that would notice if they stopped being.
- **`CQ-BUG-004`'s fix and the answer-option reconciliation** remain the strongest part of the
  import path and should not be disturbed by anything above.

### Missing invariants

**Enforced in code.** Non-blank and unique Topic/Subtopic/Question IDs; unique answer IDs within a
Question; Subtopic → Topic and Question → Topic/Subtopic resolution; Question `topicId` agreeing
with its Subtopic's owner; at least two answers; no duplicate option text within a Question; at
least one correct answer; every correct-answer ID resolving to an option of that Question; no
duplicate correct-answer ID; SINGLE carrying at most one correct answer; non-blank question text,
explanation, answer text, source title and source URL; syntactically valid `http(s)` source URL; no
unreachable-host URL; no authoring placeholder in any authored text; one Question never citing the
same source URL twice; at least one Topic, Subtopic and Question; every Topic having at least one
Question *of any status*. On the learning side, additionally: cross-document Topic and Subtopic
resolution, `relatedLessonIds` resolution, no self-relation, primary/supporting overlap, comparison
column-count agreement, and status-aware minimum content.

**Enforced only by tests.** Unique question stems; the "Select all that apply." prompt; the
approved source-host allowlist; the keyed-answer length ratio; absolute-word distribution across
keyed answers and distractors; total and per-status Question counts; the per-Topic ACTIVE
distribution and the per-Topic level distribution; total answer-option, correct-answer and source
row counts; the first Topic, Subtopic and Question in authored order; and the learning document's
per-Unit blueprint order and bridge composition.

**Relying on authored-content convention alone.** A Question is never removed from the bundle; new
Questions are appended and existing array positions never move; a material change takes a new
`Question.id` with the old one DEPRECATED; an option whose meaning changes takes a new
`AnswerOption.id`; a correct-answer key never changes under a stable ID; an ID is never reused for
different content; every ACTIVE Topic keeps at least one ACTIVE Question; an ACTIVE Question is not
left under a DEPRECATED Subtopic; no Question keys its entire option set;
`docs/content/question-bank-coverage.md` is regenerated whenever the bank changes; and learning
sources are authoritative.

### Part 3A Content Addendum assessment

The bundled curriculum can be trusted as a production-data boundary **for the content that is in it
today, and not yet for the process that changes it.** Both halves of that sentence are supported by
the same evidence. Every mechanical property that can be checked within a single revision is clean
across all 478 Questions and all 135 Lessons, the validator that checks them is exhaustive,
deterministic and well tested, and status and ordering are enforced in the one layer — SQL — where
no caller can forget them. Nothing in the shipped data is wrong.

What is missing is longitudinal. The contract that actually protects a learner's history is not
"this document is internally consistent" but "this document means the same thing the last one did
where the IDs agree", and that contract has no owner: no revision-to-revision diff, no check that a
Question survived, no check that a key held. The evidence says the convention has been kept
perfectly so far, which is exactly why the gap is easy to miss and cheap to close now.
Alongside it, the two supporting weaknesses are about *knowing*: the coverage snapshot that guides
the next expansion is 36 Questions out of date because its regeneration is manual, and the only
gate that will ever report a content defect throws away the error model built to describe it.

Fix `CQ-DATA-014`, `CQ-DATA-012` and `CQ-DATA-013` and the authored curriculum becomes a boundary
that can be trusted across revisions rather than within one. Until then, its reliability rests on
the author remembering the rules, which the history shows they have — every time so far.

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


## Part 4A Addendum — Common Koin Graph and Lifetimes

The Part 4A record above established that every binding's Koin lifetime matches the lifetime its
behaviour requires, and it remains correct: this addendum re-derived the whole graph from the
working tree independently and reached the same conclusion on all 45 definitions. It does not
restate, renumber or revisit any Part 4A conclusion. What it adds is the layer Part 4A did not
reach — what the graph's *protective machinery* actually protects, which lifetime contracts exist
only as convention, and the two written descriptions of the graph that no longer describe it. Where
the two records meet — `AssessmentHistoryStore` identity, `AppearanceTheme`'s optional lookup,
`CQ-DI-003`'s route-equality hazard — the earlier record stands and is cited rather than redone.

- **Commit reviewed:** `8b2be06abc375fa930172a03a474280e55cb3445`, working tree clean at start and
  at finish.
- **Graph re-derived, not carried forward.** Seven common modules, four platform database modules
  and four platform preference modules read in full; every `single`, `factory` and `viewModel`
  counted mechanically rather than taken from the Part 4A table. The totals agree: **30 `single`,
  0 `factory`, 15 `viewModel` in common**, plus **8 platform `single`s** (one `CurriculumDatabase`
  and one `AppPreferenceStorage` per host), and **20 `koinViewModel` sites across 15 files**, one
  `koinInject` and one `KoinPlatform` lookup.
- **Library behaviour verified against sources, not documentation.** `navigation3-runtime` 1.1.1
  (`NavEntry.kt`, `EntryProvider.kt`, `DecoratedNavEntries.kt`),
  `lifecycle-viewmodel-navigation3` 2.11.0-beta01 (`ViewModelStoreNavEntryDecorator.kt`),
  `koin-core` 4.2.2 (`KoinApplication.kt`, `InstanceRegistry.kt`), `koin-core-viewmodel` 4.2.2
  (`ModuleExt.kt`, `GetViewModel.kt`) and `koin-compose` 4.2.2 (`KoinApplication.kt`,
  `ComposeContextWrapper.kt`) were extracted from the Gradle cache and read.
- **Falsification run.** Koin's duplicate-definition behaviour was confirmed empirically with a
  throwaway JVM test rather than argued from the source: two modules each declaring
  `single { Probe(...) }`, resolved through `koinApplication { }`. The test passed — one instance,
  the *second* definition winning — and was then deleted. `git status --short` is clean apart from
  this document.
- **Production code changed: none.** This pass is an audit. No Koin definition, no module, no host
  bridge, no comment and no test was modified; only this ledger.

### Dependency and lifetime map

| Module | `single` | `viewModel` | Owns |
| --- | ---: | ---: | --- |
| `curriculumDataModule` | 3 | 0 | Publisher curriculum persistence and its startup import |
| `learningContentModule` | 1 | 0 | The bundled publisher learning document |
| `assessmentDataModule` | 11 | 0 | Attempt persistence, the history cache, the app scope, and the assessment/progress services |
| `savedQuestionDataModule` | 1 | 0 | Learner-owned saved-Question persistence |
| `lessonStudyDataModule` | 1 | 0 | Learner-owned studied-Lesson persistence |
| `topicStudyPresentationModule` | 11 | 15 | App-scoped presentation state, its stateless resolvers, and every screen ViewModel |
| `appearanceModule` | 2 | 0 | The appearance preference and its store |
| `{android,ios,jvm,web}CurriculumDataModule` | 1 each | 0 | The one `CurriculumDatabase` binding |
| `{android,ios,jvm,web}AppearanceModule` | 1 each | 0 | The one `AppPreferenceStorage` binding |

Grouped by the lifetime each definition actually needs rather than by the module it sits in:

**App-scoped mutable state (8).** `AppCoroutineScope`; `AssessmentHistoryStore`;
`InterviewHistoryStateHolder`, `MistakeReviewStateHolder`, `ProgressStateHolder`,
`SavedQuestionStateHolder`, `StudyProgressStateHolder`; `AppearanceStateHolder`. Each was opened
and read for fields. None holds a Topic, Unit, Lesson, attempt or destination identity — the only
per-identity state any of them keeps is `pendingQuestionIds` / `pendingLessonIds`, which are
learner-owned rows mid-write, not navigation state. Two more objects are app-scoped *caches* rather
than holders and belong here for the same reason: `CurriculumDataInitializer` (a `Mutex` plus a
`MutableStateFlow<Boolean>` that coalesces in-process startup attempts) and
`BundledLearningContentRepository` (a `Mutex` plus `private var content`, the once-per-process
parse).

**App-scoped stateless shared services (17).** `LearningPerformanceDerivation`,
`AssessmentQuestionSelector`, `AssessmentEngine`, `StartAssessment`, `AssessmentRetakeService`,
`AssessmentSessionLoader`, `LearningProgressService`, `AssessmentReviewLoader`,
`MistakeReviewService`, `ContinueStudyingResolver`, `LearningRecommendationResolver`,
`SavedQuestionContentResolver`, `PracticeTargetResolver`, `CurriculumImporter`, and
`ThemePreferenceStore`, plus the two repositories that hold nothing. Every one was re-read for
fields independently of the Part 4A pass: the only `var`s found anywhere are function locals
(`AssessmentQuestionSelector`'s round counters) and the private `answered`/`correct` accumulator
inside `LearningPerformanceDerivation`'s derivation, which is constructed per call. `single` here is
reuse, not shared state.

**Repositories and data infrastructure (5 common + 8 platform).** `CurriculumRepository`,
`LearningContentRepository`, `AssessmentRepository`, `SavedQuestionRepository`,
`LessonStudyRepository`, each `single<Interface> { LocalImpl(...) }`, plus `AssessmentAttemptStore`;
and per host one `CurriculumDatabase` and one `AppPreferenceStorage`. Koin binds only the declared
type, so the implementation classes are not separately resolvable and no feature can construct one:
every `Local*Repository(` and `Bundled*Repository(` call in `commonMain` is inside its own module.

**ViewModel-scoped state (15).** All fifteen `viewModel` definitions. Fourteen resolve under a
`NavEntry`'s own `ViewModelStoreOwner`; `AppShellViewModel` resolves in `App.kt` above `NavDisplay`
and is therefore owned by the host's store, which is the shell lifetime it wants.

**Platform-provided (2 contracts).** `CurriculumDatabase` and `AppPreferenceStorage` are the
*entire* set of bindings the common graph expects from outside. Nothing else crosses inward.

### What the ViewModel lifetime actually is

Worth stating precisely, because three of this addendum's findings depend on it. In Koin 4.2.2
`Module.viewModel(qualifier, definition)` is literally `return factory(qualifier, definition)` —
**a `viewModel` definition is a Koin factory.** No part of the ViewModel lifetime lives in the Koin
container. It comes entirely from `resolveViewModel`, which builds a `KoinViewModelFactory` and
calls `ViewModelProvider.create(viewModelStore, factory, extras)[vmClass]`, so the store decides
identity and the class name is the key.

The store, in turn, comes from `ViewModelStoreNavEntryDecorator`, which keys stores by
`NavEntry.contentKey` and clears one on `onPop`. `defaultContentKey(key)` is `key.toString()`, and
`AppRoute` is a sealed interface of `data class`/`data object`, so a route's *values* are its store
key. Three consequences follow and all three hold in this graph:

1. **A parameterized ViewModel cannot be re-parameterized in place.** `ViewModelProvider` returns
   the existing instance for a key and ignores the `parametersOf` lambda entirely. This is safe here
   only because a changed route value is a changed `contentKey` and therefore a different store —
   which is exactly what makes `LearningLessonDestination`'s `replaceTop(LearningLesson(unitId,
   lessonId))` re-create the reader's ViewModel with the new Lesson rather than reuse the old one.
   Nothing in the code states this dependency.
2. **Every entry's ViewModels are cleared on an area switch, not only on a pop.**
   `PrepareBackStack` treats any `contentKey` absent from the current entry list as popped, and
   `AppNavigator` hands `NavDisplay` a different one of its four stacks when the area changes. This
   is the mechanism that makes the six app-scoped holders necessary rather than stylistic, and it
   was verified in `DecoratedNavEntries.kt` rather than assumed.
3. **Two `koinViewModel()` calls for the same class inside one entry return one instance.** The
   `AssessmentLaunchViewModel` resolved by `AssessmentLaunchCoordinator`'s default parameter and
   the one each of the five destinations resolves and passes in are the same object, because they
   share a store and a default key. The "the default is never evaluated in production" reasoning in
   the Part 4A record is correct but stronger than it needs to be: even if it were evaluated, it
   could not produce a second launch ViewModel.

### State-sharing boundaries, re-verified

Each boundary was traced from the module to every consuming constructor rather than accepted from
the earlier record.

| Boundary | Shared owner | Consumers | Verdict |
| --- | --- | --- | --- |
| Assessment history | `AssessmentHistoryStore` | `AppShellViewModel`, `ProgressStateHolder`, `ProgressViewModel`, `MistakeReviewStateHolder`, `MistakeReviewViewModel`, `InterviewHistoryStateHolder`, `TopicBrowserViewModel`, `TopicDetailViewModel`, `AssessmentTakingViewModel`, `AssessmentQuestionSelector` (through `CompletedAssessmentHistory`) | Intentionally shared source of truth |
| Saved Questions | `SavedQuestionStateHolder` | `FocusedResultViewModel`, `MixedInterviewResultViewModel`, `MistakeReviewViewModel`, `SavedQuestionsViewModel` | Intentionally shared source of truth |
| Lesson study | `StudyProgressStateHolder` | `TopicBrowserViewModel`, `TopicDetailViewModel`, `LearningUnitViewModel`, `LearningLessonViewModel` | Intentionally shared source of truth |
| Unresolved-mistake rule | `MistakeReviewService` over the shared cache | badge count, `ProgressStateHolder`'s count, `MistakeReviewStateHolder`'s queue | Duplicated *derivation*, one source — and it cannot disagree, because `countUnresolved` and `load` both delegate to the same pure `UnresolvedMistakeDerivation` over the same list |
| Appearance | `AppearanceStateHolder` | `AppearanceTheme` (global lookup), `SettingsDestination` (`koinInject`) | Shared, but through two different containers — see `CQ-DI-005` |

Nothing here is a duplicated cache. The one place two derivations of the same rule run
independently — the Mistakes badge and the Progress dashboard's `unresolvedMistakeCount` — is safe
by construction rather than by coincidence, and is recorded below as a strength.

### Invalidation, traced

Every write into the shared caches was traced to its call site:

- `AssessmentHistoryStore.invalidate()` has exactly three callers:
  `AssessmentTakingViewModel` on completion, and the Retry paths in `ProgressViewModel` and
  `MistakeReviewViewModel`. Nothing outside the app writes the attempt tables, so "an attempt
  completed" is a complete invalidation trigger — today.
- `ProgressStateHolder.retryDerivation()` and `MistakeReviewStateHolder.retryDerivation()` exist
  because a re-read of unchanged history is an equal `StateFlow` value that never re-emits; each
  has exactly one caller, its own ViewModel's `retry`.
- `SavedQuestionStateHolder.refresh()` is called by all four consuming ViewModels on `init` and on
  retry; `StudyProgressStateHolder.refresh()` by all four Learn ViewModels on `init` and on retry.
  Neither holder loads itself. Both start at `Loading` and stay there until some consumer asks.

### Findings

Ordered by severity, then by confidence.

- **`CQ-DI-004` — Medium / High — nothing can detect a duplicate or overriding definition, and the
  assertions that claim to cannot.** `koin-core` 4.2.2 `KoinApplication` declares
  `private var allowOverride = true`; `strictOverride()` flips it and **no host, and no test, calls
  it**. `InstanceRegistry.saveMapping` therefore replaces an existing index silently, logging only
  `warn("(+) override index ...")` — and none of the four hosts installs a logger, so even that is
  invisible. A throwaway probe confirmed the behaviour: two modules each declaring
  `single { Probe(...) }`, resolved through `koinApplication { }`, produce **one** instance and the
  *later* definition wins. **Why it matters:** `SharedHostStartupTest` carries four identity
  assertions whose comments state the opposite mechanism — "a second binding would give them
  separately-cached histories that drift apart", "a duplicate definition would otherwise pass", "a
  second binding would silently break that", "a second instance would mean the startup screens and
  the shell could disagree". A duplicate binding does not create two instances; it creates one
  winner, so `assertEquals(koin.get<T>(), koin.get<T>())` passes either way. What those four
  assertions actually protect is `single` becoming `factory` — which is real, and is exactly what
  the Part 4A falsification run demonstrated — but it is not what they say they protect.
  **Concrete failure mode:** a platform module, or a future feature module, binds
  `AppPreferenceStorage`, `CurriculumDatabase` or any app-scoped holder a second time. Koin resolves
  it, the whole suite stays green, and the host silently runs on the definition that happened to be
  registered last — which for the two platform contracts is the one thing hosts are allowed to
  differ on. **Direction:** call `strictOverride()` in the four `start*LocalDataGraph` functions and
  in the graph test so an unintended duplicate fails at startup instead of winning, and correct the
  four assertion comments to say they pin scope rather than uniqueness.

- **`CQ-DI-005` — Low / High — the appearance preference is the one dependency resolved from the
  global container rather than the composition's.** `AppearanceTheme` does
  `remember { KoinPlatform.getKoinOrNull()?.getOrNull<AppearanceStateHolder>() }`, and
  `KoinPlatform.getKoinOrNull()` is `KoinPlatformTools.defaultContext().getOrNull()` — the
  `GlobalContext`. Every other resolution in common code goes through `koinViewModel`/`koinInject`,
  which read `LocalKoinScopeContext`, a composition local that `KoinApplication { }` and
  `KoinContext { }` override. **Why it matters:** the two are the same object only when the graph
  was started with `startKoin`. **Concrete failure mode, already visible in this repository:** six
  journey tests compose the real `App()` under `KoinApplication { ... }` — `LearningReaderJourney`,
  `FocusedLearningJourney`, `ProgressLearningJourney`, `MixedInterviewJourney`,
  `LearningProductionContentJourney` and `TopicDiscovery` — and every one of them omits
  `appearanceModule` from its module list. It would make no difference if they installed it: the
  holder would be in the composition's Koin and `AppearanceTheme` would still look in the global
  one, which those tests deliberately `stopKoin()`. `AppearanceThemeTest`'s own helper states the
  coupling — "`AppearanceTheme` looks the holder up in the global context, which is where every host
  puts it, so these tests start one" — so this is a known constraint rather than a hidden one, but
  it is a constraint the common graph should not have. A host that adopted the idiomatic Compose
  Multiplatform `KoinApplication { }` composition root instead of `startKoin` would lose dark mode
  with no error and no failing test. **Direction:** resolve the holder through the composition's
  scope with a guard — the optional, preview-safe behaviour the existing seam correctly provides is
  orthogonal to *which container* is consulted — and keep `AppearanceThemeTest`'s no-graph case,
  which would then cover the composition-local path too.

- **`CQ-DI-006` — Low / High — the graph test describes a three-module graph that has not existed
  for several epics.** `SharedHostStartupTest`'s class KDoc reads "Every runtime host installs the
  same **three** shared modules plus exactly one platform `CurriculumDatabase` module, then composes
  `AppRoot`." The hosts install **seven** common modules and **two** platform modules, and the test
  body itself installs all nine, nine lines below the comment. The inline comment on the database
  binding — "The only binding a platform host adds on top of the shared modules" — is contradicted
  three lines later by `jvmAppearanceModule`, which adds `AppPreferenceStorage`. **Why it matters:**
  this is the one test whose stated job is to pin the host contract, and the contract it states is
  wrong in both dimensions a host can get wrong: how many common modules to install, and how many
  bindings to supply. **Concrete failure mode:** a new host is written against the comment, installs
  `curriculumDataModule`/`learningContentModule`/`assessmentDataModule` and one database binding,
  and fails at the first `koinViewModel` call on a surface nobody opened during bring-up.
  **Direction:** restate both comments from the module list the test already builds — seven common
  modules, and exactly two platform bindings, naming `CurriculumDatabase` and
  `AppPreferenceStorage`.

- **`CQ-DI-007` — Low / High — the architecture document's DI narrative contradicts its own host
  table.** `docs/architecture/overview.md:17-23` still reads "The local curriculum data graph uses
  Koin because E07 introduced concrete runtime dependencies... Koin is started by the Android
  `Application`, combines the shared curriculum module with the Android database module, and uses
  the classic DSL only. Koin annotations, compiler plugins, **Compose injection, and ViewModel DSLs
  are deferred until a real requirement appears**." Ninety lines later the same document's Runtime
  Host Coverage table correctly lists four hosts and names all seven common modules plus the two
  platform modules. Three of the paragraph's claims are now false: Koin is started by four hosts,
  not the Android `Application`; the graph is seven common modules, not "the shared curriculum
  module"; and both deferred techniques are in use — `org.koin.core.module.dsl.viewModel` for
  fifteen definitions, and Compose injection at twenty `koinViewModel` sites plus one `koinInject`.
  **Why it matters:** this paragraph is the first description of DI a reader meets in the canonical
  architecture document, and it reads as a deliberate, still-current restraint rather than as an
  E07-era note nobody revisited. **Direction:** rewrite the paragraph to describe the graph the host
  table already describes, and either delete the deferral sentence or restate it as what is still
  deferred — annotations, the compiler plugin, `singleOf`/`viewModelOf`, qualifiers and scopes.

- **`CQ-DI-008` — Observation / High — `AppCoroutineScope`'s dispatcher is the browser's main thread
  on two of the five targets.** `AppCoroutineScope` is
  `CoroutineScope(SupervisorJob() + Dispatchers.Default)`. On JVM, Android and Native that is a
  background pool; on Kotlin/JS and Kotlin/Wasm `Dispatchers.Default` is the single-threaded event
  loop, so every app-scoped derivation runs where the frame does. What runs there is not trivial:
  on each `invalidate()` — that is, on every completed attempt — `ProgressStateHolder` re-derives
  the whole dashboard, `MistakeReviewStateHolder` re-builds the queue, and
  `MistakeReviewService.load` issues one `getQuestionById` per unresolved mistake through
  `AssessmentReviewLoader`, each inside its own read transaction. The holders are lazy `single`s, so
  this begins only after the learner first opens those areas, and then continues for the rest of the
  process whether or not those screens are ever shown again. No measurement was taken on any host
  and none is claimed; `CQ-DATA-010` already owns the per-ID query cost. Recorded here because it is
  a *lifetime* property — the work is permanent by construction — and because the dispatcher choice
  is uniform in the code while its meaning is not uniform across targets.

- **`CQ-DI-009` — Observation / High — three constructors can supply a dependency the graph also
  owns.** `AssessmentQuestionSelector` and `LearningProgressService` both default
  `performanceDerivation` to `LearningPerformanceDerivation(curriculumRepository)`, and
  `MistakeReviewStateHolder` defaults `learningContentRepository` to `null`. All three are supplied
  explicitly by the modules — `performanceDerivation = get()` in both cases,
  `learningContentRepository = get()` in the third — so production shares one derivation instance
  and the mistake queue does get its study links. The derivation is stateless, so a second instance
  would be harmless; the null is not harmless in the same way, because it silently removes the
  study-Lesson link rather than failing, and six test call sites construct the holder without it.
  Nothing is wrong today. Recorded because a constructor default that builds or omits a graph-owned
  dependency is the mechanism by which a future call site would silently opt out of the graph, and
  because the omission is invisible at the call site.

### What is already strong

- **No service-locator leakage whatsoever below the composition.** Twenty-four `commonMain` files
  import anything from `org.koin`: the seven module files and seventeen Compose files. Re-checked
  independently of the Part 4A pass — **no domain, data, repository, service, state-holder or
  ViewModel class touches the container.** Every one takes its dependencies through its constructor,
  which is why the test suite constructs them directly and why `AssessmentEngine`'s clock and ID
  generator are constructor defaults rather than a global hook.
- **No app-scoped object holds navigation identity.** All eight app-scoped mutable objects were
  read for fields. None keeps a Topic, Unit, Lesson, attempt or destination ID. The only
  per-identity state is a set of rows mid-write, which settles in both the success and the failure
  path. This is the single property that makes a graph with thirty singletons safe.
- **The narrowing of `CompletedAssessmentHistory` is real narrowing, not a shortcut.**
  `AssessmentQuestionSelector` declares a one-method `fun interface` and the module hands it
  `get<AssessmentHistoryStore>()`; `LearningRecommendationResolver` receives a lambda closed over
  `MistakeReviewService.countUnresolved` rather than the service. Both closures capture singletons
  and nothing from a composition or a destination, which is the distinction that matters.
- **The unresolved-mistake rule is derived three times and cannot disagree.** The Mistakes badge,
  the Progress dashboard's count and the queue itself all route through `MistakeReviewService`,
  which delegates to the pure `UnresolvedMistakeDerivation` over the same cached list. Duplicated
  derivation over one source is the right trade here, and it is what lets the badge live in a
  ViewModel while the queue lives in an app-scoped holder without the two drifting.
- **The publisher/learner module split is load-bearing and is tested by its own structure.**
  `learningContentModule` owns the authored document; `lessonStudyDataModule` owns what the learner
  did with it; `StudyProgressStateHolder` sits in neither, because it is the app-scoped projection
  rather than either the content or the table. `SavedQuestionStateHolder` and
  `SavedQuestionRepository` are split on exactly the same line. Both module comments state the rule
  and both match the code.
- **Decorator order is correct and deliberate.** `rememberSaveableStateHolderNavEntryDecorator()`
  precedes `rememberViewModelStoreNavEntryDecorator()`, which the latter's own documentation
  requires so entry-scoped ViewModels can reach a `SavedStateHandle`. The comment in `App.kt` says
  why.
- **All four hosts install a byte-for-byte identical common module list, in the same order**, each
  adding exactly its two platform modules, each guarding `startKoin` with a null check, and each
  resolving `CurriculumDataInitializer` from the container rather than constructing one. The
  `GlobalContext` / `KoinPlatform` split between hosts is cosmetic and was verified as such:
  `KoinPlatform.getKoin()` is `KoinPlatformTools.defaultContext().get()`, the same global context
  `GlobalContext.get()` returns.
- **The graph is a DAG with no layer inversion**, re-traced through all 45 definitions: platform
  storage → repositories → derivations and services → app-scoped holders → ViewModels, with no edge
  back up, and nothing under `data/**` importing Compose, navigation or `ViewModel`.

### Lifetime contracts that are implicit

Each of these is relied on by working code and is encoded nowhere — not in a type, not in a guard,
not in an assertion.

1. **A route's values are its `ViewModelStore` key.** `defaultContentKey` is `key.toString()`, so
   adding a field to an `AppRoute` widens store identity and removing one narrows it. The Lesson
   reader's `replaceTop` correctness depends on this entirely. `CQ-DI-003` records the converse
   hazard — two equal routes sharing a store — but the positive direction is equally unstated.
2. **`parametersOf` is ignored on a second resolution.** Every parameterized ViewModel is correct
   only because its route value changes whenever its parameter does. A route that carried a
   parameter the ViewModel reads but the route's `toString` did not distinguish would silently reuse
   the previous instance.
3. **An attempt completing is the only event that invalidates history.** A future writer of the
   attempt tables — a sync, an import, a debug action — must call
   `AssessmentHistoryStore.invalidate()`, and nothing would fail if it did not.
4. **The two learner-owned holders never load themselves.** Both start at `Loading` and are
   populated only because each consuming ViewModel calls `refresh()` in its `init`. A fifth Learn or
   review surface that forgets would render a permanent spinner over state the app already holds.
5. **Exactly two bindings are expected from outside the common graph.** `CurriculumDatabase` and
   `AppPreferenceStorage`. This is stated in the Part 4A handoff and in the architecture document's
   host table, but the graph itself expresses it only by failing to resolve.
6. **Nothing ever tears the graph down.** No production `stopKoin`, no scope cancellation, no
   database close. The absence of teardown is what makes the process-lifetime `SupervisorJob` and
   the never-invalidated in-memory caches safe, and it is guaranteed only by each host's startup
   guard.
7. **`AppearanceTheme` requires the global context specifically.** See `CQ-DI-005`.

### What the graph tests prove, and what they do not

`SharedHostStartupTest.sharedHostModulesResolveTheWholeProductGraph` and
`TopicStudyPresentationModuleTest` are the two graph-level tests, and both resolve through
`koinApplication { }` with `koin.get<T>()`.

**Proven.** Every binding resolves, including all fifteen ViewModel definitions and every
parameterized one with representative parameters. Four singletons are pinned as `single` rather than
`factory`. Parameter *semantics* are pinned where they are ordering-sensitive:
`TopicStudyPresentationModuleTest` resolves `LearningLessonViewModel` with
`parametersOf("unit_thinking_in_compose", "lesson_declarative_ui")` against the real bundled
document and asserts both identities on the resulting state, so a transposition fails. Both Practice
Builder source paths are covered. `SharedHostStartupTest`'s sibling composes `AppRoot` through the
real Navigation 3 decorators to the Topic Browser, so entry-scoped resolution is exercised once.

**Not proven, and not provable this way.** Because `viewModel {}` *is* `factory {}`, every
`koin.get<SomeViewModel>()` in both tests constructs a raw instance with no `ViewModelStore`
involved. Neither test can observe ViewModel scoping at all: not that two entries get different
instances, not that one entry gets the same instance twice, not that a cleared entry clears its
ViewModel, and not that `parametersOf` is ignored on a second resolution of a live store. The
journey integration tests cover the *behavioural* consequence — navigating Topic → Unit → Lesson and
asserting shipped content proves the right parameters reached the right entry — but no test asserts
the lifetime property directly, and the journey tests would also pass if the store were the host's
rather than the entry's. Duplicate definitions are undetectable for the separate reason in
`CQ-DI-004`. And no test installs all seven common modules *and* composes across more than one
navigation entry, so "all hosts install the same common product graph" is proven for module
membership but not for anything that depends on entry ownership.

### Handoffs

- **Part 4B — host composition roots.** `CQ-DI-004` and `CQ-DI-006` both land there as well as
  here: the `strictOverride()` decision belongs in the four `start*LocalDataGraph` functions, and
  the two-binding contract the graph test misstates is the contract 4B must verify per host. The
  Part 4A handoff list stands unchanged otherwise.
- **Part 5 — cross-cutting.** `CQ-DI-008`'s permanent app-scoped derivation cost on the web targets,
  alongside `CQ-DATA-010`'s per-ID query cost, which it compounds. Also `CQ-DI-009`'s constructor
  defaults, which are an API-shape question rather than a graph question.
- **Part 6 — test architecture.** The ViewModel-scoping gap above. If it is worth closing, the
  cheapest honest test is a Compose one that composes two `NavEntry`s and asserts distinct
  instances, not another `koin.get()` assertion.

### Validation

- `./gradlew :shared:jvmTest --tests '*TempOverrideProbeTest*'` — the throwaway duplicate-definition
  probe described above. **Passed**, confirming one instance and last-definition-wins. The file was
  then deleted; `git status --short` shows only this document.
- No other Gradle task was run, and none is warranted: **no executable file changed in this pass.**
  The Part 4A record's own validation — `:shared:jvmTest` and `:shared:check` both passing at
  `a77156d`, with every KMP target compiled — covers the graph as it stands, and `git diff
  a77156d..HEAD` shows the only production changes since were the three `CQ-DI-001` comment edits
  and the `CQ-DI-002` test assertion, neither of which touches a binding.
- `git status --short` — clean apart from `docs/quality/code-quality-audit.md`.

### Part 4A Addendum assessment

The common Koin graph is a reliable composition root, and the reason is structural rather than
incidental: dependency ownership is expressed in constructors everywhere below the composition, so
Koin is a wiring layer and not a runtime the code depends on. Thirty singletons would normally be a
finding in itself; here it is not, because eight of them were read and found to hold nothing a
screen owns, seventeen hold nothing at all, and the remaining five are the repositories. The one
lifetime distinction the product genuinely needs — state that must survive a `NavEntry` being
destroyed on an area switch versus state that must not — is drawn correctly at every one of the five
sharing boundaries, and the module layout matches the ownership it claims, including the
publisher-versus-learner line that two separate one-binding modules exist to keep.

What the graph lacks is not correctness but *enforcement*. Every important property of it — that a
route's fields are a store key, that only two bindings come from outside, that nothing tears the
graph down, that an attempt completing is the sole invalidation trigger, that no definition is
registered twice — is currently true by inspection and false by nothing. Koin's default
`allowOverride = true` means the graph will accept a contradiction rather than report one, and the
four assertions written to defend against exactly that describe a mechanism Koin does not have.
That is the finding worth acting on: `CQ-DI-004` is cheap, and it converts the most load-bearing of
the implicit contracts into a startup failure.

The two documentation findings are small but sit in the two places a reader goes first — the
canonical architecture document and the test that exists to pin the host contract — and both
currently describe a graph from several epics ago. `CQ-DI-005` is the only finding that touches
production behaviour, and it constrains the graph rather than breaking it: the common code can be
started, but not *composed*, with a Koin instance that is not the global one.

Part 4 is not synthesised here, and Part 4B is not begun.

## Stage 4B Fix Pass Review Record

- **Commit reviewed:** `e398dc7` (the tip of `task/code-quality-audit-1`) plus the working tree.
- **Framing:** this is not the planned Part 4B. The planned Part 4B is *host composition roots* and
  is untouched. This pass was commissioned as the architectural successor to the Stage 4A
  correctness/lifecycle work, and asked specifically about domain logic in presentation, repeated
  orchestration across owners, duplicated concepts, leaking models, invalid states, derived values
  being stored, and code that resists unit testing.
- **Boundary read:** the two attempt-result surfaces end to end (ViewModel, UI state, screen,
  destination, tests), plus `MistakeReviewViewModel`, `AssessmentReviewLoader`,
  `AssessmentReviewModels`, `ReviewSaveAction`, `AssessmentResultLayout`, `AssessmentRetakeService`,
  `AssessmentLaunchViewModel`/`Coordinator`/`StartAssessment`, `AssessmentHistoryStore`,
  `ProgressStateHolder`, `MistakeReviewStateHolder`, `TopicBrowserViewModel`,
  `TopicDetailViewModel`, `SavedQuestionStateHolder`, `SavedQuestionContentResolver`,
  `LearningContext`, `PerformanceCard`, the practice-shortcut and practice-recommendation files,
  `AssessmentConfig`, `AssessmentScope`, `PracticeBuilderUiState` and `PracticeBuilderTarget`.

### What was found

Stage 4A had already unified the *UI shell* of the two result surfaces — `AssessmentResultLayout`
and `reviewSaveAction` are shared. What it left duplicated was everything behind that shell. The
Focused practice result and the Mixed interview result held two structurally identical retake state
machines under two names, two one-case event hierarchies carrying the same payload, two identical
`LaunchedEffect` collectors, and two copies of the same mutation-boundary rule that Mistake Review
held a third copy of. The retake state was nested inside the immutable result content, and the
Mixed result's per-Topic aggregation was an imperative suspending method over a mutable counter.

That is one cluster with one theme, and it is `CQ-CROSS-001` through `CQ-CROSS-004`.

### What was deliberately not changed

- **The two `outcomeSection` composables.** Structurally identical, but every string differs by
  product intent — "Practice again" against "Retake interview", "Starting practice" against
  "Starting interview", and three differently worded failure messages. A shared composable would
  take five `StringResource` parameters and two test tags, which is exactly the flag-dominated
  extraction the Duplicated UI extraction rule forbids. The state machine, which is what can drift
  *behaviourally*, is now shared; the copy, which is a product decision, stays local.
- **`CQ-STATE-013`,** the four copies of the `derivations` counter. A genuine finding and the
  leading Stage 4C candidate, but a different theme across five owners; including it would have made
  one patch spanning two unrelated architectural stories.
- **The 51 `CancellationException` sites** across 19 files. Correct at every site inspected.
  Unifying them means a generic helper framework, which buys nothing over the explicit idiom.
- **`hasPartialCoverage` restated in `topicPracticeRecommendation`.** A real Low-severity
  duplication of one coverage rule across two files, recorded here, belonging with a Learn-surface
  pass rather than an assessment-result one.

### Behaviour

No intended user-visible change. Every guard was moved verbatim: the retake button is still disabled
while `Creating` or `Created`, the three failure states still allow an immediate retry, a stale
navigation confirmation still cannot release the guard protecting a newer retake, and cancellation
still never renders as a failed retake. The one semantic difference is unobservable: releasing
`Created` back to `Idle` no longer silently no-ops if the result is not loaded, a branch nothing
could reach, because the retake state no longer lives inside the result content.

### Test surface

`AssessmentRetakeControllerTest` (commonTest, 6 tests) now states the retake rules once, over the
real `AssessmentRetakeService` and fake repositories, instead of each rule being restated through
two ViewModels. `TopicAnswerCountsTest` (commonTest, 5 tests) covers the extracted derivation with
no fakes at all, including the invalid state its `require` now prevents. The two ViewModel suites
keep what is genuinely theirs — delegation and the loaded-result guard — and
`practiseAgainBeforeTheResultLoadsCreatesNothing` pins the rule that moving the state machine out
made explicit.

## Stage 4C Fix Pass Review Record

- **Commit reviewed:** `6324153` (the tip of `task/code-quality-audit-1`) plus the working tree.
- **Framing:** this is not the planned Part 4C. The planned Part 4 sequence still has **Part 4B —
  host composition roots** as its next chunk, untouched. This pass was commissioned as the
  successor to the Stage 4A correctness/lifecycle work and the Stage 4B ownership work, and asked a
  single question: *does the type, API, or state model make incorrect or inconsistent usage
  unnecessarily easy?*
- **Boundary read:** `AssessmentHistoryStore`, `CompletedAssessmentHistory` and all six of the
  store's consumers end to end (`ProgressStateHolder`/`ProgressViewModel`,
  `MistakeReviewStateHolder`/`MistakeReviewViewModel`, `TopicBrowserViewModel`,
  `TopicDetailViewModel`, `InterviewHistoryStateHolder`, `AppShellViewModel`), plus
  `StudyProgressStateHolder`, `SavedQuestionStateHolder`, `AppNavigator`, `PracticeBuilderUiState`
  and `PracticeBuilderViewModel`, `AssessmentQuestionSelector`, and the corresponding jvm test files
  for the store and its four retrying consumers.

### 4C findings

| ID | Area | Category | Severity | Files | Finding | Disposition |
| --- | --- | --- | --- | --- | --- | --- |
| `CQ-STATE-013` | Shared history refresh contract | event/state semantics | Medium | `AssessmentHistoryStore.kt`, `ProgressStateHolder.kt`, `MistakeReviewStateHolder.kt`, `TopicBrowserViewModel.kt`, `TopicDetailViewModel.kt` | Recovering from a failed derivation required two calls that no type related to each other. `history` was a `StateFlow`, which drops an emission equal to its last, so a re-read of an unchanged attempt table reached no consumer. Four owners each answered that with the same private `MutableStateFlow(0)` combined into their own state, and every retry became `invalidate()` *plus* a second call that a caller simply had to know to make. A retry that made only one recovered only half the screens derived from the read — which is what `CQ-STATE-012` (High, fixed in Stage 4A) actually was. | **Fixed** |
| `CQ-STATE-015` | `HistoryRefresh` | type safety | Low | `AssessmentHistoryStore.kt` | `completedAttempts()` waited for a settled generation and then had to handle `HistoryRefresh.Pending` anyway, with `error("Pending refresh cannot satisfy a settled generation.")`. The type permitted a state the caller had already excluded, so the exclusion lived in a comment and a throw rather than in the `when`. | **Fixed** |
| `CQ-STATE-016` | `InterviewHistoryStateHolder` | error modeling | — | `InterviewHistoryStateHolder.kt` | An unreadable history renders as `Empty`, the same shape as "no interviews taken". | **Not a defect.** The collapse is deliberate and documented on the branch: the record is supplementary, the screen's only control does not depend on it, and the alternative blocks the one action on a surface whose purpose is starting an interview. |
| `CQ-STATE-017` | `AppShellViewModel` | error modeling | — | `AppShellViewModel.kt` | Loading and Failed history both badge the Mistakes item with `0`, indistinguishable from "nothing unresolved". | **Not a defect.** A badge is decoration, the KDoc says so, and there is no navigation-level surface on which an error could be reported without interrupting navigation. |
| `CQ-STATE-018` | `AppNavigator.backStack` | mutability | — | `AppNavigator.kt` | The back stack escapes as a `MutableList<NavKey>`, so any caller can bypass `push`/`popBack`/`replaceTop`. | **Accepted as-is.** Navigation 3's `NavDisplay` takes the mutable list itself; narrowing the property would mean exposing a second accessor for the framework, which removes nothing. |
| `CQ-TYPE-001` | Stable identifiers | type safety | Low | repository-wide | `topicId`, `subtopicId`, `questionId`, `attemptId`, `unitId` and `lessonId` are all plain `String`, and `MistakeStudyLesson(unit.id, lesson.id, lesson.title)` is the kind of positional call where two of them could be swapped silently. | **Deferred.** No instance of a mixed identifier was found, and the remedy is the repository-wide value-class migration this audit's scope discipline excludes. Worth revisiting only if a real mix-up appears. |

Findings group into one cluster — the shared assessment-history boundary, where `CQ-STATE-013`,
`CQ-STATE-015`, `CQ-STATE-016` and `CQ-STATE-017` all live — and two isolated observations,
`CQ-STATE-018` and `CQ-TYPE-001`.

### 4C scope

**Implemented:** `CQ-STATE-013` and `CQ-STATE-015`. One file owns the contract, five files consume
it, and no unrelated code is touched.

**Deferred:** `CQ-TYPE-001` (repository-wide typing), `CQ-STATE-014` (attempt creation published as
both durable state and a one-shot `Channel` event across three result surfaces — a decision about
event delivery shared by the Part 2 owners, unchanged from Stage 4A), and `CQ-DI-007` (the DI
narrative in `docs/architecture/overview.md`).

The scope earns its type change because the contract it fixes has already produced a High bug. The
pattern was copied into four owners *because* nothing in the store's type said what a refresh meant,
and `CQ-STATE-012` was the case where one of those four copies was simply missing. Deleting the
copies is secondary; what matters is that there is no longer a second call for a fifth consumer to
forget.

### What changed

`AssessmentHistoryStore.history` is now a `SharedFlow<AssessmentHistory>` with `replay = 1`,
produced by a pure `scan` over settled refreshes rather than by a `MutableStateFlow` mutated inside
a `map`. The contract is stated on the property: **one `invalidate()` is one emission once the
resulting read settles** — whether the attempts changed, came back identical, or could not be read
at all — and `replay = 1` keeps the late-subscriber guarantee the cache exists for. `HistoryRefresh`
gained a `Settled` sub-interface, so `completedAttempts()` waits on a type that cannot be `Pending`
and its `when` has two branches instead of three.

The four `derivations` counters, both `retryDerivation()` methods and all four `combine` calls are
gone. `ProgressViewModel.refresh()`, `MistakeReviewViewModel.refresh()`,
`TopicBrowserViewModel.retry()` and `TopicDetailViewModel.retry()` each lost the second call, and
`ProgressViewModel`/`MistakeReviewViewModel` no longer retain their state holder as a property.

### Behaviour

No intended user-visible change, and the claim is measured rather than asserted: with conflation
restored on the new flow — one `distinctUntilChanged()` inserted before `shareIn` — the four
consumers' retry-recovery regressions fail and the two new store regressions fail, and with it
removed all 1 549 jvm tests pass. That is the same recovery the counters provided, now provided
once.

One unobservable timing difference is worth recording: a derivation retry used to run immediately
on the counter bump *and* again if the re-read changed the history, so a retry could derive twice.
It now derives once, after the read settles. Nothing renders in between either way — the previous
state stays on screen while the derivation suspends — so the visible sequence is unchanged, and the
retry now costs one derivation instead of two.

### Test surface

`AssessmentHistoryStoreTest` gains four regressions over the flow contract rather than over any
consumer: an unchanged re-read still announces itself, a *failed* re-read announces itself while
keeping the attempts already read, a subscriber before the first read settles is told `Loading`
rather than "no history", and a late subscriber replays the cached history without starting a read
of its own. The four consumer retry tests were already written against the public `refresh()`/
`retry()` API, so they needed no change and now pin the single-signal contract directly.


## Stage 4D Fix Pass Review Record

- **Commit reviewed:** `39f5820` (the tip of `task/code-quality-audit-1`) plus the working tree.
- **Framing:** this is not the planned Part 4D. The planned Part 4 sequence still has **Part 4B —
  host composition roots** as its next chunk, untouched. This pass was commissioned as the successor
  to the Stage 4A correctness work, the Stage 4B ownership work and the Stage 4C API-shape work, and
  asked one question: *does the application stay correct when operations overlap, repeat, are
  cancelled, partially fail, or resume after an interrupted lifecycle?*
- **Boundary read:** every persistence entity and its DAO (`saved_question`, `studied_lesson`,
  `test_attempt`/`question_attempt`/`question_attempt_selected_answer`, the curriculum tables), the
  four local repositories, `AssessmentAttemptStore`, `AssessmentAttemptMapper`, `CurriculumImporter`,
  `CurriculumDataInitializer`, `AssessmentHistoryStore`, `AssessmentEngine`, `StartAssessment`,
  `AssessmentRetakeService`/`AssessmentRetakeController`, `AssessmentSessionLoader`,
  `AssessmentTakingViewModel`, `AssessmentLaunchViewModel`, all five app-scoped state holders,
  `LearningProgressService`, `MistakeReviewService`, `UnresolvedMistakeDerivation`,
  `QuestionExposure`, `BundledLearningContentRepository`, `AppRoot`, and the five
  `AppPreferenceStorage` implementations.

### The dispatcher line

Almost every conclusion in this pass turns on one fact that is easy to miss: **`AppCoroutineScope`
is `SupervisorJob() + Dispatchers.Default`, a thread pool on three of the five targets, while every
ViewModel runs on `viewModelScope`, i.e. `Dispatchers.Main.immediate`.** So app-scoped singletons are
genuinely reached from two threads, and ViewModel-confined state is not. That line is what separates
a realistic race from a theoretical one here, and it is why several candidate findings below are
recorded as serialised by design rather than fixed. `CQ-DI-008` already records the *cost* side of
the same fact.

### 4D findings

Two new findings, `CQ-DATA-021` and `CQ-DATA-022`, both fixed. Two findings were re-derived
independently and turned out to be already adjudicated — `CQ-DATA-004` (learning-content cache
publication) and `CQ-DATA-007` (persisted score versus occurrence rows) — and both existing
dispositions were re-confirmed rather than reopened. `CQ-DATA-004` is the more instructive of the
two: this pass reached the opposite conclusion, added `@Volatile`, and then reverted it once the
ledger's final-field argument was checked against JLS 17.5 and found correct. The lesson is recorded
in that row.

### What was examined and found sound

This is the part worth keeping, because it is the expensive half of the pass and a later session
should not re-derive it.

- **Learner-owned state (`saved_question`, `studied_lesson`).** `PRIMARY KEY(question_id)` /
  `PRIMARY KEY(lesson_id)` with `@Insert(onConflict = IGNORE)` and delete-by-key. Uniqueness is
  enforced at the database rather than by caller discipline, and save/unsave/mark/unmark are
  naturally idempotent. `IGNORE` rather than `REPLACE` is deliberate and documented: re-marking must
  not move the recorded timestamp forward.
- **`SavedQuestionStateHolder` and `StudyProgressStateHolder`.** Already hardened. The `reading`
  mutex orders every read *including each mutation's read-back*, so a refresh that reached the
  database before a write can no longer publish after it; visible state changes only after a
  persisted read; a failed re-read keeps the last good snapshot. The `pendingIds` check-then-act is a
  race in the abstract, but every caller is a UI tap on the main dispatcher, so it is serialised by
  design and was deliberately not "fixed".
- **`AssessmentAttemptStore.save`.** Upsert, delete-all-children, reinsert, inside one
  `withWriteTransaction`. Whole-snapshot replacement is what makes a repeated or retried save
  idempotent, and a cancelled save rolls the transaction back. This is why the completion retry in
  `CQ-DATA-022` cannot create a second occurrence.
- **`CurriculumImporter`.** One deferred-foreign-key write transaction, upsert-based, so re-import is
  idempotent. `deleteAnswerOptionsForQuestionExcept` already excludes options a historical attempt
  selected via `NOT EXISTS`, then deprecates the survivors, so the referenced-row abort that
  `CQ-BUG-004` fixed cannot return through the option path either.
- **`CurriculumDataInitializer`.** Correct double-checked locking whose flag is published through a
  `MutableStateFlow`. A rejected or cancelled import leaves it false, so retry re-runs a full
  idempotent import.
- **Ordering.** Every ordered query carries an explicit total order — `completed_at DESC, started_at
  DESC, id ASC`; `saved_at DESC, question_id ASC`; `sort_order` on authored lists — and
  `UnresolvedMistakeDerivation` consumes newest-first as a stated contract rather than an assumption.
- **Derived aggregates.** The score columns of `CQ-DATA-007` are the *only* persisted derived state
  in the application. No counter, percentage, completion count or weak-area flag is stored; progress,
  coverage, weak areas and the mistake queue are recomputed from attempts on every refresh. That is
  the right design for this app and no cache should be added to it.
- **Retry.** No retry infrastructure exists and none is warranted. Every user-facing retry re-enters
  the same idempotent operation, and `AssessmentRetakeController` keeps `Created` terminal precisely
  so that a second press cannot mint a second durable attempt.
- **Deletion and reset.** There is no delete or reset operation anywhere in the application: no
  attempt deletion, no progress reset, no teardown. The orphaned-record, stale-aggregate and
  surviving-dependent-state questions therefore have no surface to apply to.

### Recorded but deliberately not fixed

- **Abandoned `IN_PROGRESS` attempts accumulate.** A launch or retake whose coroutine is cancelled
  after `StartAssessment` persists the attempt but before its identity reaches navigation leaves a
  row nothing will ever open. Nothing *reads* it — `getCompletedTestAttempts` and `QuestionExposure`
  both filter on `COMPLETED` — so this is unbounded row growth with no incorrect behaviour. Pruning
  or a resume-an-assessment surface is a product decision before it is a technical one.
- **Wall-clock ordering of the mistake queue.** `completed_at_epoch_millis` decides which occurrence
  of a Question is authoritative. A backwards clock correction could make an older incorrect
  occurrence win and resurrect a resolved mistake. The remedy is a monotonic sequence, which should
  not be invented before a concrete problem exists. Related to, but narrower than, `CQ-DATA-008`.
- **`JvmAppPreferenceStorage.write` is a truncating read-modify-write over a properties file.** One
  key, one caller, main thread only, and `Properties.load` on a truncated file degrades to "no
  preference stored" rather than throwing. Not worth a change.

### 4D scope

**Implemented:** `CQ-DATA-021` and `CQ-DATA-022` — one cluster, the app-scoped shared state that
outlives every screen: an invalidation that must be atomic against the one-shot read path, and a
completion that must not be able to half-happen.

**Deferred:** the three items recorded above, plus `CQ-DATA-007`'s invariant, which belongs to a
derived-aggregate pass rather than a concurrency one — tightening it churns roughly seventy fixtures
across thirty-eight files, which would have made this patch unreviewable for a state only corruption
can produce.

The boundary: everything implemented is about *two things happening at once, or one thing stopping
halfway*. Everything deferred is about *what the data means*, or needs a product decision first.

### Behaviour

No intended user-visible change. The completion path persists the same attempt with the same score
and navigates to the same result; the only difference is that the shared cache is now marked stale
even when the coroutine that completed the attempt does not survive to the next statement. The
generation bump produces the same reads in every non-racing sequence, which the two existing
coalescing and retry regressions pin.

### Test surface

`CompleteAssessmentTest` (jvmTest, 5 tests) states the completion contract over the real
`AssessmentHistoryStore` and observes invalidation as what it actually is — one more read of the
attempt table. Two of the five were confirmed to fail against the pre-fix two-statement form:
`aWriteThatCommitsBeforeCancellationStillMarksHistoryStale`, whose fake records the write and only
then observes the cancellation its own resumption would have observed, and
`aFailedWriteStillMarksHistoryStale`. The other three cover duplicate invocation, the score itself,
and a session that cannot be completed writing and invalidating nothing.
`BundledLearningContentRepositoryTest` gains `concurrentFirstCallsShareOneLoadAndSeeTheSameDocument`,
the half of the caching contract that repeated *sequential* queries cannot show, gated on a
`CompletableDeferred` rather than on timing. `AssessmentTakingViewModelTest` keeps every rule it
already owned; its six construction sites now build the operation instead of receiving the store.


## Stage 4E Fix Pass Review Record

- **Commit reviewed:** `272a509` (the tip of `task/code-quality-audit-1`) plus the working tree.
- **Framing:** a performance pass, deliberately not a micro-optimisation one. The question asked was
  *is the application doing materially more work than necessary during realistic usage?*, and a
  finding only counted when the thing that repeatedly triggers the work could be named.
- **Boundary read:** every Compose screen and shared component for recomposition, stability, lambda
  identity, lazy-list keys and derived data; every `StateFlow`/`shareIn`/`stateIn` pipeline;
  `CurriculumDao`, `LocalCurriculumRepository` and `CurriculumRepository`; the progress, mistake,
  review, saved-question, session-resume and selection derivations; `BundledLearningContentRepository`
  and the two bundled documents; and the initialisation paths.

### Data scale this pass reasoned from

The bundled bank is **478 Questions (437 ACTIVE, 41 DEPRECATED), 361 Subtopics, 17 Topics**; the
learning document is 1.9 MB of JSON — 30 Units, 135 Lessons, 402 Sections, 2,844 blocks. Those are
the numbers every cost claim below is scaled against, and they are worth keeping: "how big can this
get?" is the first question any later performance finding has to answer.

### 4E findings

Six findings, one fixed. The fixed one is `CQ-DATA-010`, which existed since Part 3 as *Needs
measurement*; this pass supplied the measurement rather than opening a new row. The other five are
recorded in **4F candidates** below and were deliberately not implemented.

The measurement that closed it was not a benchmark — the repository has no benchmarking
infrastructure and the brief forbids inventing one — but a trigger analysis plus call-count tests.
What the earlier pass could not say was *how often* the derivation runs. It runs on every
`AssessmentHistoryStore.invalidate()`, which is every completed assessment, in up to four places at
once, because `ProgressStateHolder` and `MistakeReviewStateHolder` are `SharingStarted.Eagerly`
app-scoped singletons that derive whether or not their screens are open, while `TopicBrowserViewModel`
and `TopicDetailViewModel` each derive again on the same emission. It also runs on **every
level-chip tap** in the Practice Builder when the source is `WEAK_AREAS`, because
`refreshAvailability()` asks the selector for a deliberately large run and the weak-area policy
derives all-time performance to answer. A UI tap could therefore cost 478 read transactions.

### What was examined and found sound

This is the expensive half of the pass and a later session should not re-derive it.

- **Compose.** Kotlin 2.4.10 means strong skipping is on, so unstable parameters are compared by
  instance and composables stay skippable; no stability annotation was warranted anywhere and none
  was added. Lazy lists carry stable keys wherever identity can change, and the two review lists
  without keys are static, which is the case the brief says to leave alone. `rememberLessonOutline`
  keys on a structurally-equal map so the memo survives; `LessonOutline` takes `currentIndex` as a
  lambda so scrolling recomposes the outline and not the prose; `LearningContextIndex` exists
  precisely so a screen joins against one derivation instead of searching per row. No collection
  work of consequence runs in composition. Parts 1A-1D did this job properly and it shows.
- **Flow pipelines.** One shared history upstream, one collector per consumer, no duplicated
  `combine` chain and no second `getCompletedAttempts` read anywhere. `Eagerly` is correct here and
  `WhileSubscribed` would be strictly more work: the upstream is an invalidation signal mapped to a
  read, not a live subscription, so sharing while subscribed would re-read on every tab switch.
- **Aggregate computation.** `QuestionExposure`, `UnresolvedMistakeDerivation`,
  `RecentPerformancePolicy` and the two coverage groupings are already single-pass over history.
- **Resource parsing.** `BundledLearningContentRepository` decodes and validates the 1.9 MB document
  once per instance behind a double-checked mutex and answers every query from prebuilt immutable
  indexes. Nothing repeats, and no cache should be added anywhere near it.
- **Main-thread work and initialisation.** No file I/O, JSON parsing or large transform runs from a
  composable or a ViewModel `init`. No duplicate startup work.

### The API-shape decision, and why it is not incidental

The obvious way to add a batched read is to add `getQuestionsByIds` beside `getQuestionById` with a
default implementation that loops. That was written first and **proved unsafe within the same
session**: `MistakeReviewViewModelTest` has decorators declared as
`CurriculumRepository by VmCurriculumRepository` that override only the per-ID form. Kotlin's `by`
delegation delegates interface members that have default bodies too, so the batched default resolved
against the *delegate*, and the cancellation and partial-content decorators silently stopped
decorating anything. The tests still passed; they were simply no longer testing what they claimed.

So `getQuestionById` was replaced rather than joined. `CurriculumRepository` now has exactly one
historical Question resolver, abstract, with no per-ID sibling an implementation could answer
inconsistently and no default body a decorator could bypass. Tests that legitimately read one
identity use `CurriculumSingleReads.kt` in the test source set — extensions cannot be overridden or
delegated around, so a test reading one identity is provably reading it through the same code
production reads many. That cost 31 mechanical test-double edits, which is the honest price of not
leaving a trap in the contract.

### Recorded but deliberately not fixed

- **Topic and Subtopic name resolution stays per-ID.** Both loops are memoised per derivation and
  bounded by what the curriculum authors — 17 Topics, 361 Subtopics — rather than by how much
  history the learner has, and each is one statement against one table rather than a read
  transaction. Batching them would have doubled the contract change for the smaller half of the
  cost. Recorded as a 4F candidate with the honest arithmetic attached.
- **`CQ-DI-008`'s permanent app-scoped derivation.** Relieved but not closed; see its row.
- The four other 4E findings, all listed under **4F candidates**.

### Behaviour

No intended user-visible change. Every surface resolves the same identities to the same content in
the same order; a missing identity still resolves to `Missing`, a DEPRECATED one still resolves with
its retired answer options, and `AssessmentSessionLoader` still names the *first* missing question in
attempt order. `LocalCurriculumRepositoryTest` asserts the batched read against a real database and
requires many identities in one request to answer exactly as singleton requests do.

One deliberate behavioural tightening came out of the pass rather than going into it. Batching
initially made the derivation read the curriculum even with no completed history, which two tests
caught — `practiceWithAnEmptyHistoryDerivedSourceCreatesNoAttempt` and the Topic-discovery read
count. Rather than relax either assertion, the empty case is now guarded in the three callers where
"nothing to resolve" is an ordinary runtime state: no completed history, an empty mistake queue, an
empty saved list. A learner's first practice run now touches the question tables not at all.

### Test surface

Evidence is expressed as call counts, never as timing; no benchmark or "must finish in X ms"
assertion was added.

- `LearningProgressServiceTest.historicalIdentitiesAreResolvedInOneBatchedReadPerLoad` (renamed from
  `historicalLookupsCacheResolvedAndMissingIdentitiesOncePerLoad`) asserts one batched read holding
  every identity history mentions, resolved and missing alike, and that Topic and Subtopic names
  stay one memoised lookup each. `focusedMixedAndRepeated...` additionally pins that three
  occurrences of one Question across three attempts are asked for once.
- `AssessmentReviewLoaderTest.wholeAttemptIsResolvedInOneHistoricalRead`,
  `MistakeReviewServiceTest.theWholeQueueIsResolvedInOneHistoricalRead` and
  `SavedQuestionContentResolverTest.theWholeSavedListIsResolvedInOneRead` each pin one round trip
  and the unchanged output order.
- `LocalCurriculumRepositoryTest.getQuestionsByIdsAnswersManyIdentitiesExactlyAsSingletonRequestsDo`
  is the semantic anchor, run against a real Room database over the real bundled curriculum.
- No behavioural coverage was weakened. Every assertion that previously counted per-ID reads now
  counts batched reads over the same identities.

### Validation

| Command | Result |
| ------- | ------ |
| `./gradlew :shared:jvmTest` | PASS — 1559 tests, 0 failures |
| `./gradlew :shared:check` | PASS — JVM, Android host, JS, Wasm and iOS simulator task graph |
| `./gradlew :androidApp:assembleDebug` | PASS |
| `git diff --check` | Clean |

No benchmark was run, because the repository has none and this pass did not introduce one.


## Stage 4F Fix Pass Review Record

- **Commit reviewed:** `cbd7d5c` (the tip of `task/code-quality-audit-1`) plus the working tree.
- **Framing:** not the planned Part 4B — host composition roots remain untouched. This pass was
  commissioned as the regression-protection successor to Stages 4A-4E and asked one question: *are
  the important architectural and behavioural guarantees of this application protected by tests that
  would actually catch realistic regressions?* Test count was explicitly not the objective.
- **Boundary read:** all 131 test files in `commonTest` and `jvmTest` (1 559 tests at the start of
  the pass), inventoried by name; every production file with no same-named test, checked for
  indirect coverage; every broad `catch` in `commonMain` and the platform source sets; every
  `runBlocking`, `delay`, `Thread.sleep`, clock, timezone, UUID and `Random` use in production and
  test code.

### What the suite already does well, and should not be re-derived

This is the expensive half of the pass. The suite is in good shape and most of the brief's
checklist has no finding attached to it.

- **Coroutine hygiene.** No `Thread.sleep` anywhere, no `delay` used for synchronisation in any
  test, and no `runBlocking` standing in for `runTest` in a coroutine test — the three surviving
  uses are fixture loading outside a coroutine test and one deliberately documented Unconfined
  bridge. 59 files use `runTest`; ViewModel suites install `StandardTestDispatcher` as Main and
  drive with `advanceUntilIdle()`/`runCurrent()`.
- **Time and randomness are already injected.** `AssessmentEngine`, `LocalSavedQuestionRepository`
  and `LocalLessonStudyRepository` all take `now: () -> Instant`; `AssessmentEngine` takes
  `generateAttemptId`; `AssessmentQuestionSelector` takes `randomize`; `AnswerOrder` seeds `Random`
  from the attempt and question IDs. `LocalTimestamp` is pure and takes its offset and its "now" as
  arguments. No test depends on the wall clock, the machine timezone or the current date.
- **Failure paths are tested, not assumed.** Repository failure, missing records, deprecated and
  missing historical content, empty datasets, duplicate invocation, cancellation, retry and
  repository/database reconstruction each have named tests across the state holders, the four local
  repositories and the integration suites.
- **Persistence guarantees are tested at the layer that provides them.** `AssessmentAttemptStoreTest`,
  `LocalSavedQuestionRepositoryTest`, `LocalLessonStudyRepositoryTest`, `CurriculumImporterTest` and
  `CurriculumDatabaseMigrationTest` all run against a real Room database, including concurrent
  duplicate writes, foreign-key rejection, deterministic ordering and eight migration steps.
- **Assertions are specific.** 467 `assertIs<…>` against 162 `assertNotNull`; the `assertTrue(…
  isNotEmpty())` uses are bundled-content smoke checks and Compose "at least one node matches"
  queries, which is what that assertion is for.
- **Compose tests assert behaviour.** 82 semantic-state assertions (`assertHasClickAction`,
  `assertIsEnabled`, `assertIsToggleable`, `assertIsSelected`) beside the tag lookups, and the
  adaptive suite covers both window classes for the cases where the layout genuinely differs.
- **Files with no same-named test are mostly models and DI modules.** The ones that looked like real
  gaps were each checked and found covered through their consumers: `RecentStudyContextDerivation`
  (`ContinueStudyingResolverTest`, `LearningRecommendationResolverTest`), `AuthoredContentChecks`
  (`CurriculumValidatorTest`), `RecentPerformancePolicy` (`LearningProgressServiceTest`),
  `AssessmentAttemptMapper` (`AssessmentAttemptStoreTest`, against a real database), and the four
  route-mapping files (`AppNavigationTest`, which also pins the "no target may reach an attempt"
  rule the mappings' KDoc states).

### 4F findings

| ID | Area | Existing coverage | Missing contract | Regression not currently caught | Level | Priority | Disposition |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `CQ-TEST-001` | `AppShellViewModel` | `SharedHostStartupTest` asserts Koin resolves it; `ProgressLearningJourneyIntegrationTest` asserts the literal text `"2"` in the unmerged tree of one full-app run | The badge's whole mapping from `AssessmentHistory`: `Loading`/`Failed` badge nothing (the `CQ-STATE-017` adjudication), the count is the mistake queue's rule over the *shared* read, it moves on completion, and a rebuilt shell takes the replayed history | A shell that badges an error indicator, that recounts by reading the attempt table itself on every rebuild, or that stops following `invalidate()`. Only the one integration assertion stands between any of those and shipping, and it survives the first two | ViewModel | **High** | **Fixed** |
| `CQ-TEST-002` | `PracticeBuilderViewModel.refreshAvailability()` | `availabilityRefreshesWhenTheLevelSelectionChanges` (sequential; its two toggles are not separated by a scheduler advance, so the superseded check never starts), `aFailedEligibilityCheckIsAnErrorThatRetryCanRecoverFrom` | The documented supersession rule — *"Held so a superseded eligibility read cannot land after the one that replaced it"* — and the documented cancellation rethrow beside it | Dropping `availabilityJob?.cancel()` leaves the learner looking at the eligible count *and the offered run lengths* of a level selection they have already moved off; folding the cancellation into `Error` puts "could not check" on a screen whose newer check is still running. `refreshAvailability()` runs on every level and source tap, so this is the most frequently exercised cancellation in the app | ViewModel | **High** | **Fixed** |
| `CQ-TEST-003` | `LearningLessonViewModel`, `LearningUnitViewModel`, `ProgressTopicViewModel` | Each has a failure-and-retry test; none has a cancellation test | All three `loadJob?.cancel()`, publish `Loading`, then relaunch, and all three rethrow `CancellationException` so the abandoned load publishes nothing. Eight comparable owners in the repository have this test; these three do not | A double-tapped Retry, or a Lesson opened while the previous document read is outstanding, leaves `Error` on screen over a reload that is going to succeed — and on the Progress drill-down `Error` is the state that offers Retry, so the learner is invited to retry the retry | ViewModel | Medium | **Fixed** |
| `CQ-TEST-004` | `timestampText` | `LocalTimestampTest` covers the pure day-and-offset arithmetic; nothing covers the wording layer, and the one integration assertion deliberately avoids it | The `TODAY`/`YESTERDAY`/dated branch selection and the twelve-entry `shortMonthResource` table | A month mis-mapped in that table dates every Progress history row and every interview record wrongly, silently. A mid-month instant keeps such a test timezone-independent, so it is writable without flakiness | Compose UI | Medium | **Deferred to 4G** |
| `CQ-TEST-005` | `AssessmentLaunchViewModel` | Four tests over success, no-eligible-questions, retry and re-entry | Its `CancellationException` rethrow, which keeps a dismissed launch dialog from settling as `Failed(Unexpected)` | Lower than `CQ-TEST-003`: the state belongs to a dialog that is going away, so a swallowed cancellation is largely unobservable | ViewModel | Low | **Deferred to 4G** |

Two candidates were investigated and **not** raised. `AppShellViewModel`'s `catch (_: Exception) -> 0`
around `countUnresolved` is unreachable from its only call site — the shell always supplies the
attempts, and `UnresolvedMistakeDerivation` over a supplied list is pure and cannot throw — so a test
for it would be an invented failure the API cannot produce. And no test double was found misleading:
the fakes propagate failures, preserve production ordering, and the repository-level guarantees are
tested against real Room rather than restated in Kotlin.

### 4F scope

**Implemented:** `CQ-TEST-001`, `CQ-TEST-002` and `CQ-TEST-003` — one cluster: **an asynchronous
result that was superseded, cancelled, or never read must not reach the learner as an answer.**

That is one rule with four owners that each cancel-and-relaunch (`PracticeBuilderViewModel` twice
over, on the target resolve and the eligibility check; the Lesson reader; the Unit overview; the
Progress drill-down) plus the one consumer of the shared history cache that had no behavioural test
at all. The repository already states this rule elsewhere — `ProgressViewModel`'s
`aStaleReadCompletingLateDoesNotOverwriteTheNewerResult`, `SavedQuestionStateHolder`'s
`aReadIssuedBeforeAMutationDoesNotOverwriteItAfterwards`, `StudyProgressStateHolder`'s equivalent —
so this pass finishes an existing pattern rather than introducing one.

**Deferred:** `CQ-TEST-004` (a different cluster: the presentation of time) and `CQ-TEST-005`.

### Test surface

Ten tests, all `jvmTest`, all deterministic and gated on `CompletableDeferred` rather than on timing.

- **`AppShellViewModelTest`** (new, 5 tests). The count is the mistake queue's rule over the shared
  history; an unsettled history and an unreadable one each badge nothing; a completed assessment
  moves the badge on the same one further read every other screen gets; a rebuilt shell badges the
  cached count without starting a read. Its curriculum repository refuses every call, which is
  itself the assertion that the badge counts occurrences and never reconstructs review content.
- **`PracticeBuilderViewModelTest`** (+2). A superseded eligibility read cannot overwrite the newer
  selection — asserted on both the eligible count and the offered run lengths, because
  `refreshAvailability` writes `questionCountOptions` inside the same `try`. And a cancelled read
  leaves the screen `Checking` rather than `Error`. The first check is for two levels and the second
  for one, so the two answers differ and the assertion can tell them apart.
- **`LearningUnitViewModelTest`**, **`LearningLessonViewModelTest`**, **`ProgressTopicViewModelTest`**
  (+1 each). A superseded load publishes nothing and leaves the screen loading. Two gates per test,
  with the superseded read released *last*, after the replacement has already been observed — which
  is what makes the ordering a fact rather than a hope.

Two test doubles gained a gate rather than a new fake being written: `FakeLearningContentRepository`
(shared by the Unit and Lesson suites) and `ProgressTopicViewModelTest`'s history repository each
took a `beforeRead`/`beforeLoad` hook receiving the read's 1-based number, defaulting to a no-op so
no existing call site changed. `PracticeBuilderViewModelTest`'s private curriculum fake took the
same hook keyed on its existing `selectionCalls` counter.

### Mutation check

Every added test was verified against a deliberately broken production tree, one mutation at a time,
each restored immediately:

| Mutation | Tests that failed | Others affected |
| --- | --- | --- |
| `CancellationException` rethrow removed from the three `load()` methods | the three `aSupersededLoad…` tests | none of the other 45 in those classes |
| `availabilityJob?.cancel()` removed | `aSupersededEligibilityReadCannotOverwriteTheNewerSelection` | none of the other 32 |
| Practice Builder cancellation folded into `PracticeAvailability.Error` | `aCancelledEligibilityReadLeavesTheScreenCheckingRatherThanFailed` | none of the other 32 |
| `Loading, Failed -> 0` changed to `-> 1` | the unsettled-history and unreadable-history tests | none |
| `countUnresolved(history.attempts)` changed to `countUnresolved()` | the completion and rebuilt-shell tests | none |
| `countUnresolved(history.attempts)` changed to `history.attempts.size` | three of the five badge tests | none |

### Behaviour

No production code was changed. The suite went from 1 559 to 1 569 jvm tests.

### Validation

| Command | Result |
| ------- | ------ |
| The five affected test classes, individually | PASS |
| `./gradlew :shared:jvmTest` | PASS — 1569 tests, 0 failures, 0 skipped |
| `./gradlew :shared:check` | PASS — JVM, Android host, JS, Wasm and iOS simulator task graph |
| `./gradlew :androidApp:assembleDebug` | PASS |
| `git diff --check` | Clean |


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

Part 3A Content Addendum — Complete

High: 0
Medium: 3
Low: 4
Observations: 3

Fixed: 0
Deferred: 0
Needs measurement: 0
Accepted as-is: 0
Not a defect: 0

Part 4A Addendum — Complete

High: 0
Medium: 1
Low: 3
Observations: 2

Fixed: 0
Deferred: 0
Needs measurement: 0
Accepted as-is: 0
Not a defect: 0

Stage 4A fix pass — Complete

High: 1
Medium: 2
Low: 1
Observations: 0

Fixed: 2
Deferred: 2
Needs measurement: 0
Accepted as-is: 0
Not a defect: 0

Stage 4C fix pass — Complete

High: 0
Medium: 1
Low: 2
Observations: 0

Fixed: 2
Deferred: 1
Needs measurement: 0
Accepted as-is: 1
Not a defect: 2

Part 4B — Next
```

The Stage 4C fix pass is complete. It is not one of the planned chunks: it is a state-model,
API-contract and boundary-correctness re-audit of the current tree, bounded to the one cluster where
the type system was actually making misuse easy — the shared assessment-history store and the six
surfaces that derive from it. `CQ-STATE-013` (Medium) is fixed, and it is the finding the Stage 4A
pass named as the consolidation that its own High fix made worth doing. `AssessmentHistoryStore
.history` was a `StateFlow`, so a re-read of an attempt table nobody had written produced an equal
`Loaded` that never reached a consumer; four owners each answered that privately with the same
counter, and "retry" became two calls related by nothing but convention. It is now a `SharedFlow`
with `replay = 1`, built from a pure `scan` over settled refreshes, whose stated contract is one
emission per `invalidate()` once the read settles — so one call recovers both an unreadable attempt
table and a derivation that failed over history which read perfectly well. The four `derivations`
counters, both `retryDerivation()` methods and all four `combine` calls are deleted, and no
consumer can now forget the half of a retry that `CQ-STATE-012` was. `CQ-STATE-015` (Low) closes the
store's own unreachable state: `HistoryRefresh` gained a `Settled` sub-interface, so the suspending
read waits on a type that cannot be `Pending` and no longer carries an `error(...)` for a case its
own caller had already excluded. Four regressions in `AssessmentHistoryStoreTest` pin the new flow
contract, and the claim that behaviour is unchanged was measured: restoring conflation with a single
`distinctUntilChanged()` fails all four consumers' retry regressions and two of the new ones.
`CQ-STATE-016`, `CQ-STATE-017` and `CQ-STATE-018` are recorded as deliberate and were not changed;
`CQ-TYPE-001` and `CQ-STATE-014` are deferred. The exact next planned chunk remains **Part 4B —
Host composition roots**, which this pass did not begin.

The stage 4A fix pass is complete. It is not one of the planned chunks: it is a targeted re-audit of
the current tree against state ownership, coroutine and Flow lifecycle, cancellation, event-versus-state
and Compose-effect correctness, run outside the Part sequence and bounded to the two findings where
behaviour was actually wrong rather than merely duplicated. The cancellation discipline the earlier
passes established held everywhere it was re-checked — every `runCatching` and every broad `catch` over
a suspend boundary in the shared module rethrows `CancellationException` before folding a failure into
state, and the generation guards in `TopicBrowserViewModel`, `TopicDetailViewModel` and
`PracticeBuilderViewModel` still make a superseded read unable to land on a newer one. What it found
instead was a recovery path that does not reach the thing that failed. `CQ-STATE-012` (High) is fixed:
neither Learn ViewModel's Retry touched the shared assessment history, so an unreadable attempt table at
startup removed the learning context, Recommended Next, Continue Studying and the unresolved-mistake
count for the rest of the session, and pressing Retry restored the catalogue and silently left the rest
gone. Both now invalidate the shared history and bump a derivation counter, which is the recovery
`ProgressViewModel` and `MistakeReviewViewModel` already documented and implemented; four jvm
regressions pin both failure domains and each was confirmed to fail against the previous code.
`CQ-KMP-001` (Medium) closes the open Part 3D handoff: `AndroidAppPreferenceStorage` was the one
implementation that could throw out of a contract forbidding it, and it is read synchronously above the
startup error screen, so the throw would have been a first-frame crash rather than a forgotten
preference. `CQ-STATE-013` and `CQ-STATE-014` are recorded and deferred — the first is the consolidation
that the fix to `CQ-STATE-012` makes worth doing, the second a decision about event delivery shared by
three result surfaces. The exact next planned chunk remains **Part 4B — Host composition roots**, which
this pass did not begin.

The Part 4A addendum is complete. The common Koin graph was re-derived from the working tree
independently of the Part 4A record and agrees with it on all 45 definitions: seven common modules,
30 singles, no factories, 15 ViewModels, eight platform singles, and exactly two bindings expected
from outside. Every lifetime still matches the ownership its behaviour requires, no app-scoped
object holds navigation identity, no domain, data, service, state-holder or ViewModel class touches
the container, and all four hosts install an identical common module list. What this pass adds is
the layer above correctness. Koin 4.2.2 allows definition override by default and no host calls
`strictOverride()`, so a duplicate binding silently wins rather than failing — confirmed with a
throwaway probe — and the four identity assertions written to defend against exactly that describe a
mechanism Koin does not have; they pin scope, not uniqueness. `AppearanceTheme` is the one
dependency resolved from the global container rather than the composition's Koin, which is why six
journey tests compose the real `App()` without an appearance module and could not use one if they
installed it. The graph test's own KDoc still describes three shared modules and one platform
binding, and the architecture document's DI narrative still describes an Android-only,
curriculum-only graph with Compose injection and ViewModel DSLs deferred — both contradicted by code
the same files sit beside. Six findings are recorded and none fixed, because this pass is an audit:
`CQ-DI-004` (Medium), `CQ-DI-005`, `CQ-DI-006`, `CQ-DI-007` (Low) and `CQ-DI-008`, `CQ-DI-009`
(Observations), together with seven lifetime contracts that working code relies on and nothing
encodes. **No graph defect was found and nothing was changed.** The exact next chunk remains
**Part 4B — Host composition roots**, and the Part 4 synthesis is deliberately not written here.

The Part 3A content addendum is complete. The two bundled JSON documents were audited as production
data rather than as fixtures, in full and programmatically: 17 Topics, 361 Subtopics, 478 Questions,
1 918 answer options, 528 correct-answer entries and 635 sources on the assessment side, and 30
Units, 135 Lessons and 2 844 blocks on the learning side. Every mechanical property checkable within
one revision is clean — no duplicate or malformed identity anywhere, no broken reference, no
hierarchy disagreement, no duplicated stem, explanation or option set, no placeholder, and 635
well-formed allowlisted source URLs — and the near-duplicate scans over stems and options surfaced
only false positives. SINGLE arity is enforced from both directions; the three MULTIPLE Questions
with one correct answer are a documented product decision about not leaking the key through the
input controls, not a defect. Status and ordering are enforced in SQL, where no caller can forget
them. The gap is longitudinal rather than structural: nothing compares one bundle revision to the
next, so the rules that actually protect a learner's history — never delete a Question, never move
an array position, never change a correct-answer key under a stable ID — have no owner in code, even
though four revisions of history show every one of them kept. Ten findings are recorded, none fixed,
because this pass is an audit: `CQ-DATA-012` (the coverage snapshot is 36 Questions stale and
ungated), `CQ-DATA-013` (the only gate that fires throws away the error model built to describe the
defect), `CQ-DATA-014` (no revision-to-revision diff), four Low findings and three Observations.
**No content defect was found in the shipped dataset, and nothing was changed.** The exact next
chunk remains **Part 4B — Host composition roots**, and the Part 3 synthesis is deliberately not
written here.

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
