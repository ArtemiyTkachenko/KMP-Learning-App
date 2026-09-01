# Architecture Notes

## Application Composition

`App()` wraps `AppShell()` in `AppTheme` and takes no dependencies of its own;
everything the shell needs is either navigation state it owns or a ViewModel
resolved from Koin at a destination boundary.

The local curriculum data graph uses Koin because E07 introduced concrete
runtime dependencies that need platform-aware composition: `CurriculumDatabase`,
`CurriculumImporter`, `CurriculumDataInitializer`, and
`CurriculumRepository`. Koin is started by the Android `Application`, combines
the shared curriculum module with the Android database module, and uses the
classic DSL only. Koin annotations, compiler plugins, Compose injection, and
ViewModel DSLs are deferred until a real requirement appears.

Android startup now follows:

```text
Application
  -> start Koin
MainActivity
  -> await local curriculum initialization
  -> App()
```

`CurriculumRepository` is the application-facing data boundary intended for
E08 assessment-engine work. Runtime reads should depend on that interface
rather than on Room entities, DAOs, or the local repository implementation.

The shell exposes four areas — Topics, Interview, Progress, and Mistakes —
through `AppTopLevelDestination`, which maps each to its `AppRoute`.

`AppNavigator` owns navigation state and gives **each area its own back stack**. A
single shared stack meant switching away from a detail discarded it, so returning to
an area dropped the learner back at its root; per-area stacks leave each area exactly
where it was left. Back leaves the current area's detail first, then returns to the
start area (Topics), and only then reports the event unconsumed so the host can close
the app. Re-selecting the area already shown returns it to its root.

Which screens keep the navigation control is decided by `AppRoute.showsAreaNavigation()`:
browsing screens — including the topic and progress-topic details — keep it, because
hiding it on every detail trapped the learner inside an area until they pressed back.
Screens that own the learner's full attention (an assessment in progress, and its
result) hide it and rely on their own back affordance.

`AppNavigationScaffold` places that control adaptively: below
`AppNavigationRailBreakpoint` (600.dp, the Material compact/medium boundary) a
`NavigationBar` runs along the bottom edge; at or above it a `NavigationRail` runs down
the leading edge. The decision is made from the measured window width rather than from
the platform, because the same host can be either size — a desktop or browser window can
be dragged narrow.

`AppShellViewModel` supplies the one piece of state the control itself needs: the
unresolved mistake count, badged onto the Mistakes item. The Progress dashboard reports
the same count as plain text rather than as a second button, so one destination has one
control.

Navigation motion is declared in `AppNavigationTransitions` rather than left to
Navigation 3's defaults, which animate on Android but resolve to `EnterTransition.None`
on desktop, iOS, and web.

Shared presentation ViewModels are resolved from the Koin Compose module at the
Navigation 3 destination boundary. Parameterized destinations pass only stable
route data into their ViewModels, while Navigation 3 entry-scoped ViewModel
ownership remains intact: each back-stack entry receives its own
ViewModelStore, and the ViewModel is cleared when that entry is removed.

All runtime hosts share `AppRoot(initialize)` in shared `commonMain`. It owns the
startup loading, failure, and retry states around the platform initializer and
then enters `App()`. Hosts start Koin before composition and supply only their
own initializer, so a failed initialization cannot leave a host without
content. `App()` keeps its own `MaterialTheme` so it remains usable directly in
tests and previews that bypass `AppRoot`.

### Runtime Host Coverage

`App()` and the common product graph are used by every configured runtime host:

| Host | Koin graph started by | Database builder | Runnable |
| --- | --- | --- | --- |
| Android | `KmpLearningApplication` -> `startAndroidLocalDataGraph` | `CurriculumDatabase.android.kt` | yes |
| Desktop (JVM) | `desktopApp/main.kt` -> `startDesktopLocalDataGraph` | `CurriculumDatabase.jvm.kt` | yes |
| iOS | `MainViewController` -> `startIosLocalDataGraph` | `CurriculumDatabase.ios.kt`, bundled SQLite | yes |
| Web JS | `webApp/main.kt` -> `startWebLocalDataGraph` | `CurriculumDatabase.web.kt`, SQLite worker/OPFS | yes |
| Web Wasm | `webApp/main.kt` -> `startWebLocalDataGraph` | `CurriculumDatabase.web.kt`, SQLite worker/OPFS | yes |

Each startup function installs `curriculumDataModule`, `assessmentDataModule`,
and `topicStudyPresentationModule` plus exactly one platform database module.
The host then composes its thin platform root, which delegates initialization to
the common `AppRoot` state machine. Database creation and platform storage stay
below the shared repository boundary; `App()` does not start Koin or select a
database.

The JS and Wasm executables share the Room builder and Koin module in `webMain`.
Only worker construction differs in `jsMain` and `wasmJsMain`. The repository's
`sqliteWasmWorker` module packages the official-style SQLite WASM worker used by
`WebWorkerSQLiteDriver`; the worker opens the stable `curriculum.db` in OPFS.
The browser must support OPFS and SharedArrayBuffer in a secure,
cross-origin-isolated context. The development webpack server supplies COOP and
COEP headers; production hosting must do the same.

Kotlin/Native iOS compilations remain disabled on the Linux CI runner. iOS
framework linking and simulator runtime verification are therefore local macOS
checks rather than Linux CI guarantees.

## Curriculum Content Model

The curriculum content contract lives in shared `commonMain` code as immutable
Kotlin models with flat Topic, Subtopic, and Question collections linked by
stable string IDs. The flat shape is intentional: it keeps content identity
independent from display text and avoids coupling the model to a future database
or import format.

Substantive content validation lives in `CurriculumValidator` so validation can
report multiple authoring errors for a complete curriculum instead of failing
object construction on the first malformed item. Serialization and local
persistence are handled by the E07 data path while Room-specific metadata stays
out of the curriculum domain models.

`Question.selectionMode` is authored curriculum data and flows through local
persistence into assessment presentation. It is deliberately independent from
`correctAnswerIds`: interaction controls must not reveal answer-key cardinality,
and scoring continues to compare selected and correct answer-ID sets exactly.

## Assessment Domain

The shared assessment model defines focused and mixed assessment configuration,
question attempts, answer identity state, attempt lifecycle, timestamps, and
score summaries without depending on Room, Koin, Compose, Android, or
`CurriculumRepository`. E08 uses `CurriculumRepository` for question selection
and keeps scoring/session behavior separate from persistence.
Question selection follows `AssessmentConfig -> AssessmentQuestionSelector ->
CurriculumRepository`; Mixed selection uses coverage-first rounds across topics
after randomized encounter ordering.
The runtime `AssessmentSession` keeps the selected `Question` objects for
scoring, while `TestAttempt` remains the stable-ID attempt record persisted by
the local attempt store without embedding curriculum content.

`AssessmentRepository` is the domain-facing boundary for durable
`TestAttempt` snapshots. The local implementation delegates to
`AssessmentAttemptStore`, keeping Room entities and DAOs below the repository
interface. Retake creation is separate orchestration:
`AssessmentRetakeService` loads a completed source attempt, starts a new
assessment with the same `AssessmentConfig`, saves the new in-progress
`TestAttempt`, and leaves the source attempt unchanged. Retakes intentionally
use fresh selection without guaranteeing that questions differ from the
original run.

Completed history feeds the shared `LearningProgressService`, which derives a
`LearningProgressSnapshot` entirely in memory. Overall totals sum persisted
`AssessmentScore` values, while Topic and Subtopic observations use persisted
`QuestionAnswerState.Answered.isCorrect` values plus stable historical Question,
Topic, and Subtopic lookup. Every completed occurrence counts equally,
including focused, mixed, and retake attempts; derived statistics are not
persisted. A Topic is weak after at least 3 observations below 70% accuracy,
and a Subtopic after at least 2 observations below 70% accuracy.

The same snapshot carries curriculum coverage, which answers a different
question: not "how accurately did I answer what I saw?" but "how much of the
current curriculum have I seen at all?". Coverage is the intersection of the
stable Question IDs appearing in completed history with the current ACTIVE
question bank, read once per derivation through
`CurriculumRepository.getActiveQuestions()` and grouped in memory. It is
therefore deliberately unlike performance in three ways: each stable Question ID
counts at most once no matter how often it was answered, correctness is
irrelevant because an incorrect answer is still exposure, and the denominator is
the current ACTIVE bank rather than anything reachable from history. A
DEPRECATED or unresolvable historical Question keeps its historical accuracy but
leaves current coverage, and publishing new questions legitimately lowers the
coverage percentage while leaving accuracy untouched. Coverage groups are built
from the ACTIVE questions rather than from the attempted IDs, so a Topic or
Subtopic with no history at all is present as `0/N` instead of missing — "never
attempted" must stay distinguishable from "attempted poorly", and 0/0 reports a
`null` percentage because an empty denominator is not 0% coverage. Coverage is
derived state like everything else here; nothing about it is persisted.

The Progress dashboard is a shared presentation destination reached through
the argument-free `AppRoute.Progress` route. `ProgressViewModel` maps the
derived snapshot and newest-first completed history into display models,
resolving focused scope labels through stable historical Topic/Subtopic lookup.
The destination refreshes on lifecycle resume so retained navigation entries
show attempts completed while another result or retake destination was open.
History rows navigate by stable attempt ID to the existing focused or mixed
result destinations; no progress snapshot or history summary is persisted. The
dashboard reports the unresolved mistake count as plain text — the Mistakes
navigation item, badged with the same count, owns opening the queue.

Topic performance rows open `AppRoute.ProgressTopic(topicId)`, carrying only
stable topic identity. `ProgressTopicViewModel` selects that Topic and its
observed Subtopics out of the same derived snapshot, so the drill-down never
recalculates statistics or issues curriculum queries of its own. Subtopics
without completed observations are absent rather than fabricated, and weak
Subtopics are flagged from the snapshot's existing policy result.

Mistake review is derived, never persisted:

    AssessmentRepository.getCompletedAttempts()   (newest first)
        -> first occurrence per stable Question ID
        -> that occurrence's persisted correctness
        -> incorrect only
        -> AssessmentReviewLoader.loadQuestion(...)
        -> unresolved mistake queue

Because completed history is already ordered newest first, the first occurrence
of a Question ID is its latest one, so a later correct answer resolves the
Question automatically and a later incorrect answer reopens it. This is
deliberately narrower than `LearningProgressService`, which stays
occurrence-based and counts every completed answer. Review content is
reconstructed only for unresolved candidates, and a Question whose content no
longer resolves stays in the queue as `ReviewQuestionItem.Missing`. No mistake,
resolved, or dismissed state is stored; Room remains assessment-history
persistence only.

The completed E08 graph is composed with the same classic Koin DSL as the
curriculum graph: `AssessmentQuestionSelector` depends on `CurriculumRepository`,
`AssessmentEngine` depends on the selector, `AssessmentRepository` persists
`TestAttempt` snapshots through `AssessmentAttemptStore`, and
`AssessmentRetakeService` coordinates the repository plus engine. There is no
separate assessment startup initializer because assessment services are lazy
capabilities; Android still awaits curriculum import before entering `App()`.
E09 and E10 should depend on `CurriculumRepository`, `AssessmentRepository`,
`AssessmentEngine`, and `AssessmentRetakeService`, not Room DAOs or entities.

Assessment taking is shared by focused practice and mixed interviews through
`AssessmentTakingViewModel -> AssessmentEngine -> AssessmentSession`.
`AssessmentTakingLaunch.New` accepts either assessment configuration and saves
the initial and per-answer `TestAttempt` snapshots through
`AssessmentRepository` before the UI advances. `ExistingAttempt` reconstructs
the runtime-only session from its stable attempt ID through
`AssessmentSessionLoader`, with the persisted `TestAttempt.config` remaining
authoritative. Product wrappers provide titles and navigation while reusing the
same question, submission, progress, and explicit-completion state machine. The
taking screen pins a linear meter under its top bar, driven by the same
`questionNumber`/`totalQuestions` as the counter, so how far through the assessment
the learner is stays answerable while they read a long question.

The Mixed Android Interview product has its own top-level area. `InterviewStartScreen`
leads with the start action and, once the learner has finished an interview, shows their
latest and best results through `InterviewStartViewModel`; each opens the result it came
from. Starting an interview navigates with `MixedInterview(questionCount)` primitive
route data.
The route reconstructs `AssessmentConfig.Mixed` at its destination and delegates
to `AssessmentTakingLaunch.New`, so balanced selection, initial persistence,
answer checkpoints, actual selected-question progress, and explicit completion
stay in the shared assessment path. After the initial attempt is persisted, the
config route is replaced by `MixedInterviewAttempt(attemptId)` so saved
navigation restores the existing session instead of creating another attempt.
Focused starts use the same promotion to `FocusedPracticeAttempt(attemptId)`.
Completion replaces the attempt entry with `MixedInterviewResult(attemptId)`;
the result loads the durable `AssessmentScore` from `AssessmentRepository` and
uses `AssessmentReviewLoader` with `CurriculumRepository.getQuestionById` for
ordered historical review. Resolved review Questions are grouped by `topicId`
in attempt encounter order, and `CurriculumRepository.getTopicById` resolves
historical names without ACTIVE filtering. Topic performance is derived in
memory and is not persisted.

Mixed interview repeats follow the same persisted-retake boundary as focused
practice. The Mixed result delegates creation to `AssessmentRetakeService`,
keeps the completed source result in the back stack, and pushes
`MixedInterviewAttempt(retakeAttemptId)` only after the new attempt has been
saved. That route reopens the persisted session through
`AssessmentTakingLaunch.ExistingAttempt`, so balanced selection and
`AssessmentEngine.start()` occur once during retake creation rather than again
when the assessment screen opens.

E09-04 completes the retained session through `AssessmentEngine`, persists the
completed attempt before replacing focused-practice navigation with a stable
attempt-ID result route, and loads historical review through
`AssessmentRepository` plus `CurriculumRepository.getQuestionById`. Deprecated
or missing historical questions are represented per review item without
changing the durable score. Retake behavior remains deferred to E09-05.

E09-05 exposes repeat practice from the result screen through
`AssessmentRetakeService`. The service creates and persists a fresh attempt;
`FocusedPracticeAttempt` then carries only its stable ID, and
`AssessmentSessionLoader` reconstructs the runtime session without calling
`AssessmentEngine.start()` a second time. The source result remains below the
retake in the back stack.

The complete E09 focused-learning path is `TopicBrowser -> TopicDetail ->
AssessmentConfig.Focused -> FocusedPractice -> AssessmentEngine`, with durable
`TestAttempt` checkpoints through `AssessmentRepository`, explicit completion,
and `FocusedPracticeResult(attemptId)` historical reconstruction. A repeat uses
`AssessmentRetakeService`, then navigates by the persisted
`FocusedPracticeAttempt(attemptId)` so `AssessmentSessionLoader` can restore the
runtime-only session without creating a second attempt. Android and Desktop
share this presentation and domain flow; Room and DAOs remain below the
repository boundaries.

Topic detail screens use a Material 3 top app bar for back navigation, with the
navigation icon invoking the existing Navigation 3 back-stack pop. Detail and
practice destinations should keep this phone-style toolbar affordance instead
of rendering a standalone text Back button in page content.
