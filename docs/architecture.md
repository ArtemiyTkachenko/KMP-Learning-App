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
after randomized encounter ordering. Targeted practice adds level and question
source to that configuration without adding a second engine; see "Targeted
practice selection" below.
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

The snapshot also carries recent performance, which answers "how have I been
performing lately?" while the all-time figures continue to answer "how have I
performed across my complete history?". All-time accuracy is correct but slow:
after substantial history it can still read 58% for a learner who now scores
80-90%, so `RecentPerformancePolicy` defines a second, separate signal instead of
reweighting the first. Recent means the latest **five completed assessments** —
a count window rather than a date window, because a date window shows an
intensive user dozens of observations from one evening and an occasional user an
empty dashboard despite real history. Five is responsive to a change in
performance, resistant to one bad evening, explainable in a sentence, and a
natural size for one compact series; it is a product policy and is deliberately
not configurable. Attempts are ordered `completedAt DESC, startedAt DESC,
id ASC` — the same ordering `AssessmentAttemptDao` queries with — by the policy
itself rather than trusted from the caller, since the history may arrive from the
repository, the shared cache, or a test fake. Every completed attempt
participates on identical terms, focused, mixed and retake alike, because a
retake is simply another completed occurrence; IN_PROGRESS attempts never do.

Recent accuracy is question-weighted: correct answers over answered questions
across the whole window, never the mean of the attempt percentages, since a 1/1
attempt and a 10/20 attempt make 11/21 rather than 75%. It is `null` rather than
0.0 when there is no recent evidence, because a learner who has completed nothing
has not scored 0%. Both series are exposed oldest -> newest so a chart reads past
-> present without presentation reversing domain data, and the attempt series
carries raw percentages only — no direction, momentum, or velocity score is
derived, and there is no time decay, so every answer inside the window has equal
weight and recency is expressed solely by the bounded window. A trend is marked
available at three attempts, below which one observation or a single change is
not a trajectory worth presenting. The per-answer series is capped at 50 outcomes
and keeps the most recent ones; the summary is derived from the attempt series
precisely so that the cap can never silently narrow it. `QuestionAttempt` stores
no answer timestamp, so that series is ordered by attempt completion time and
then by stored assessment sequence — a sequence, not a wall-clock record of when
each answer was given. As with all-time performance, persisted
`QuestionAnswerState.Answered.isCorrect` is authoritative and is never recompared
against the current `Question.correctAnswerIds`: an answer key can be corrected
later, and history must not change retrospectively. That also means recent
performance issues no curriculum query of its own. Nothing about it is persisted.

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

The dashboard presents the snapshot's three signals as three separate surfaces,
because they answer three different questions and are routinely different
numbers. All-time accuracy keeps the primary summary card and is now labelled as
all-time rather than "overall", since an unqualified accuracy figure beside a
recent one is ambiguous. Curriculum coverage and recent performance follow it as
quieter tonal cards: important, but not three competing headlines. Coverage always
prints its raw attempted/total counts beside the percentage, because coverage and
accuracy differ substantially for normal learners and the denominator is what
explains why; its meter uses the exact count ratio rather than the rounded display
percentage, and a `null` percentage (an empty ACTIVE bank) reports that there is no
curriculum to cover instead of drawing 0%. Recent performance prints the domain's
question-weighted window accuracy — never the mean of the plotted attempts — with
a grammatically singular or plural window label. The new-user Empty state is
unchanged: a dashboard of zeroes is not a substitute for guidance.

Recent performance carries the app's only trend visualization, a small Compose
`Canvas` line chart in `RecentTrendChart`; no charting dependency was added for
five points, and the drawing stays in `commonMain` like the rest of the UI. Two of
its properties are load-bearing enough to be pinned by tests over the pure
`trendPoints` helper rather than by inspecting a rendered chart. The vertical scale
is fixed to 0-100% and never fitted to the observed values, because a 72/74/76
series auto-scaled to its own range draws as a dramatic climb when nothing much
happened. Horizontal spacing is uniform, because the recent window is defined by a
count of assessments rather than by elapsed time, so the gaps carry no duration
meaning. The chart appears only at the domain's `RecentTrendAvailability.Available`
and the shorter cases say plainly that a trend appears after three assessments,
rather than hiding the summary that one or two completed assessments legitimately
support. It plots `attemptSeries` and not `answerSeries` — one visualization is the
budget — draws no direction colouring or "improving"/"declining" label, since the
domain deliberately exposes raw observations, and carries a semantic description
listing every plotted percentage oldest-first so the drawing is never the only
representation.

Topic performance rows open `AppRoute.ProgressTopic(topicId)`, carrying only
stable topic identity. `ProgressTopicViewModel` selects that Topic and its
observed Subtopics out of the same derived snapshot, so the drill-down never
recalculates statistics or issues curriculum queries of its own. Subtopics
without completed observations are absent rather than fabricated, and weak
Subtopics are flagged from the snapshot's existing policy result. Current
coverage joins onto those rows by stable ID as a caption under the correct/answered
line, so the drill-down states the same two concepts the study surfaces do without
growing a second card per scope; a scope with no ACTIVE questions reports no
coverage at all rather than `0/0`, and zero coverage never removes an accuracy the
learner earned on questions that have since been retired. The screen stays
analytics-focused: unseen Subtopics are still not listed here, because browsing the
whole curriculum is Topic Detail's job.

## Learning context on the study surfaces

The Topics list, Topic Detail, and Subtopic rows present the same derived
snapshot through one small presentation model, `LearningContextUiModel`, built by
`LearningContextIndex` — a single derivation indexed by stable ID so a list of
seventeen Topics costs one snapshot and no repository read per card. Three states
have to stay apart, and the nullability is how they do it: a `null`
`accuracyPercentage` means loaded history holds no answer for that scope, an
absent `LearningContextUiModel` means analytics have not loaded or could not be
derived, and only the combination of no accuracy and no attempted questions
justifies saying "Not studied yet". Coverage and accuracy are never combined into
a single score, because a scope can hold real historical accuracy beside zero
current coverage — which is exactly what a retired question looks like from here.

`TopicBrowserViewModel` and `TopicDetailViewModel` therefore hold curriculum,
query, and learning context as three separate inputs and re-render from all three,
rather than awaiting a combined load. Curriculum is the primary capability and the
only input that can produce Loading, Empty, or Error: browsing, searching, and
starting practice keep working when history is unavailable, and an optional
statistic is never allowed to take down the study flow. The query lives outside
both loads, so a history refresh rebuilds the rows underneath an active search
without disturbing what was typed. Learning context follows the app-scoped
`AssessmentHistoryStore` rather than reading completed attempts again, so a newly
completed assessment refreshes these screens through the same invalidation every
other consumer uses — no restart, no manual retry, and no second history cache.
No app-wide analytics state holder was introduced: the store plus the service
already are the shared source, and each feature only maps them.

Search matching is unchanged by any of this. It still reads Topic and Subtopic
names only, in memory, against the catalog already loaded, so learning context is
display metadata that no query can match and typing still issues no repository
read. Topic search results reuse the enriched Topic row rather than deriving a
second one; Subtopic results stay compact and parent-contextual, with their full
learning context living on Topic Detail.

Presentation keeps the two figures visibly different concepts. Coverage is always
a count ("12 of 28 explored"), never a bare percentage that could be mistaken for
accuracy, and stays in neutral theme colours throughout: a learner at 10% coverage
has not done anything wrong, so the correct/incorrect palette would misread as a
bad score. Accuracy keeps `accuracyColor` and carries its own label. The weak badge
is driven only by `TopicPerformance.isWeak` / `SubtopicPerformance.isWeak`; a row
can render as low accuracy without being weak, because the policy's evidence
threshold has not been met, and accuracy colour is never treated as the weak-state
source of truth.

Unresolved mistake state is derived once, never persisted:

    AssessmentRepository.getCompletedAttempts()   (newest first)
        -> first occurrence per stable Question ID
        -> that occurrence's persisted correctness
        -> incorrect only
        -> UnresolvedMistakeDerivation
             |-> AssessmentReviewLoader.loadQuestion(...) -> mistake queue
             `-> current ACTIVE scoped/level candidates -> targeted practice

Because completed history is already ordered newest first, the first occurrence
of a Question ID is its latest one, so a later correct answer resolves the
Question automatically and a later incorrect answer reopens it. Both Mistake
Review and unresolved-mistake practice consume `UnresolvedMistakeDerivation`, so
they cannot disagree about that lifecycle. This is
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

## Targeted practice selection

EPIC-16 lets a practice run be narrowed by interview level and by where its
Questions come from. That is one configuration, not one assessment type per
practice kind: `AssessmentConfig.Focused` carries scope, requested question
count, selected `QuestionLevel`s, and a single `PracticeQuestionSource`, and it
still ends at the same `AssessmentEngine`, the same `QuestionAttempt`s keyed by
stable Question ID, and the same scoring. The alternative — an engine per
practice type — would have forked session lifecycle, persistence, and completion
four ways to vary one step of the pipeline.

The source is one typed dimension rather than `onlyUnseen`/`weakOnly`/
`mistakesOnly` booleans, because the four values are mutually exclusive policies
and boolean combinations would invent product states nobody designed. Levels are
a set with inclusive-OR meaning, matching the `CurriculumRepository` level-filter
contract: "any level" is expressed by naming every level, never by an empty set.
Level filtering happens in the repository through the scoped level-aware reads,
so presentation never loads a scope and filters it afterwards. Mixed Android
Interview deliberately stays outside all of this — no scope, no levels, no
source — because coverage-first selection across every ACTIVE Question is its
product behaviour, not a special case of targeted practice.

`AssessmentQuestionSelector` returns `AssessmentSelectionResult` instead of a
bare `List<Question>`. An empty list answered too many questions at once, and
the differences matter: nothing eligible, no level selected, and a future source
whose policy does not exist yet are three different answers. All four current
sources — `ALL`, `UNSEEN`, `WEAK_AREAS`, and `UNRESOLVED_MISTAKES` — are
implemented and never fall back to another source.
Each is a branch of one `when` over the source, so adding a policy is a local
change that leaves the Practice Builder UI, the engine, scoring, and session logic
untouched. `AssessmentEngine` collapses every no-content reason into
`AssessmentStartResult.NoEligibleQuestions`, since assessment taking has one
no-content state and availability is read from selection before starting; what it
guarantees is that a refused request creates and persists no attempt.

### Unseen practice

"Has the learner seen this Question?" is one definition, `QuestionExposure`, and
both the curriculum-coverage statistics and unseen practice read it. They are the
same concept in opposite directions — coverage counts the Questions inside the
exposure set, unseen practice selects the ones outside it — and two folds over
history would agree today and drift the first time one of them changed its mind
about what counts, leaving a Progress percentage that practice contradicts.
Exposure is a set of stable Question IDs taken from completed attempts only, so a
Question asked five times is exposed exactly as much as one asked once, an
IN_PROGRESS attempt makes nothing seen, and correctness never enters into it.

Selection subtracts that set from the ordinary candidate pool — the same scoped,
level-aware ACTIVE read `ALL` uses — rather than starting from history. That
ordering is what keeps historical and current content from contaminating each
other: an exposed ID whose Question has been retired cannot remove anything from
a pool it is no longer in, and a newly authored Question is unseen the moment it
exists, with nothing to backfill. Nothing is persisted for any of this; there is
no `isSeen` column and no stored candidate list, because the answer is derived at
selection time and would otherwise be stale by the next completed attempt. Scope
and levels are never widened to find unseen content: a fully-seen ADVANCED
selection ends at `NoEligibleQuestions`, which the Practice Builder already reads
as "this setup has nothing to ask", instead of quietly practising FOUNDATION.

The selector reads history through `CompletedAssessmentHistory`, a one-shot
suspending read that `AssessmentHistoryStore` implements. It serves the Practice
Builder's per-edit preflight from the app-scoped cache rather than issuing a
query for every chip the learner taps, and it inherits the store's invalidation,
so a just-completed assessment stops being unseen through the same refresh
Progress and the mistake queue use. Screen observers may retain their last loaded
content while that refresh runs, but the one-shot selection read tracks the
invalidation generation and waits for it rather than accepting stale attempts.
It raises `AssessmentHistoryUnavailableException` when that generation fails,
and the next selection retry starts one new repository read. Reporting unread or
stale history as "no completed attempts" would report Questions as unseen — most
likely right after launch or completion — and start a practice run built on it.
A retake still re-runs the persisted configuration rather than replaying stored
Questions, so repeating an unseen run selects against the learner's history as it
stands then, which by definition no longer includes the Questions they just
answered.

### Weak-area practice

Weak-area practice and the Progress dashboard share one
`LearningPerformanceDerivation`. It owns the occurrence aggregation, historical
Question-to-Topic/Subtopic resolution, minimum-evidence rules, accuracy threshold,
and `WeakArea` construction; neither the selector nor presentation reconstructs
those rules. `LearningProgressService` combines that output with coverage and recent
performance, while `AssessmentQuestionSelector` consumes only the weak Topic and
Subtopic identities. This keeps the practice pool and the learner-facing Progress
assessment on the same semantics without making every practice preflight calculate
unrelated coverage and trend data.

The selector derives weak identities from completed history through
`CompletedAssessmentHistory`, then intersects their union with the ordinary scoped,
level-aware ACTIVE candidate read. A weak Topic admits candidates from any of its
children; an independently weak Subtopic admits only that child when its parent is
healthy. Membership is an OR filter, so Topic/Subtopic overlap does not add weight or
duplicate a Question. The configured scope and levels are never widened, deprecated
Questions can contribute historical evidence but cannot enter a new attempt, and an
empty weak set ends at `NoEligibleQuestions`. The existing stable-ID deduplication,
randomization, and requested-count truncation run after this eligibility filter.

### Unresolved-mistake practice

Mistake practice consumes the same `UnresolvedMistakeDerivation` as Mistake
Review. Completed attempts arrive newest first through `CompletedAssessmentHistory`;
the first persisted occurrence of each stable Question ID wins, and its stored
`QuestionAnswerState.Answered.isCorrect` is authoritative. The current authored
answer key is not consulted, so editing curriculum content cannot rewrite history.
IN_PROGRESS attempts are excluded before latest-occurrence state is derived, and
assessment type or retake origin does not partition the history.

The selector intersects those unresolved IDs with its ordinary scoped,
level-aware ACTIVE repository read. History therefore decides whether an ID is
unresolved, while current curriculum decides whether it is askable: missing and
deprecated Questions remain available to historical review but cannot enter a new
assessment. Scope and levels are never widened, and an empty intersection returns
`NoEligibleQuestions`. The resulting pool still passes through the common
stable-ID deduplication, randomization, and requested-count truncation.

Nothing records a mistake as resolved or dismissed. When mistake practice is
completed correctly, the normal completion save creates the newest correct
occurrence and `AssessmentTakingViewModel` invalidates `AssessmentHistoryStore`.
The next Mistake Review or selection derivation therefore excludes that Question;
a later completed incorrect occurrence reopens it through exactly the same path.

## Guided learning recommendation policy

`LearningRecommendationPolicy` is a pure `commonMain` decision over already-derived
facts. It does not read Room or repositories, observe flows, inspect navigation,
start an assessment, or reproduce any learning-signal derivation. Its typed
`LearningRecommendationInputs` have these sources of truth:

| Input | Source |
| --- | --- |
| Completed-attempt count | `LearningProgressSnapshot.completedAttemptCount` |
| Unresolved-mistake count | `MistakeReviewService.countUnresolved`, backed by `UnresolvedMistakeDerivation` |
| Ordered weak areas | `LearningProgressSnapshot.weakAreas`, backed by `LearningPerformanceDerivation` |
| Topic and Subtopic coverage | `LearningProgressSnapshot.topicCoverage` and `subtopicCoverage` |
| Optional recent study context | Scope/configuration kind from the newest completed history entry |

The optional recent context is stable domain data only: either a focused
`AssessmentScope` or Mixed. The caller takes it from completed history, which is
already ordered newest first; `IN_PROGRESS` attempts and presentation navigation
state are not inputs.

Recommendation priority is a deterministic product policy, not a ranking score:

| State | Recommendation |
| --- | --- |
| No usable ACTIVE curriculum | None |
| New user (zero completed attempts) | Browse Topics |
| One or more unresolved mistakes | Open Mistake Review |
| Currently usable weak area | Open weak-area practice preset |
| Remaining unseen ACTIVE Questions | Open Topic-scoped unseen-practice preset |
| Otherwise | None |

Mistakes therefore outrank weakness, and weakness outranks ordinary coverage.
The unresolved count is consumed as one fact; the policy never scans attempts.
Weakness is consumed as the ordered `WeakArea` output and never recalculates the
70% or minimum-evidence rules. That established ordering (accuracy, then evidence,
then stable identity) selects the first weak area that still intersects current
ACTIVE Topic/Subtopic coverage; a historical weak scope with no current content is
skipped. Coverage is consumed as unique-ID counts and never derives exposure.

Unseen coverage candidates are Topics with at least one unseen ACTIVE Question.
They are selected by this exact tie order:

1. Lowest coverage ratio, compared exactly from attempted/total counts.
2. A matching recent focused Topic, or the current parent Topic of a matching
   focused Subtopic.
3. More unseen Questions.
4. Lexicographically smallest stable Topic ID.

Mixed, stale, missing, complete, or unrelated recent context is ignored. Recency
can only resolve a tie inside the already-selected lowest-coverage group, so it
cannot create a competing "continue where you left off" recommendation; that
surface belongs to E17-02.

Every result pairs a semantic `LearningRecommendationTarget` with a typed
`LearningRecommendationRationale`. The rationale carries the deciding count,
scope/name, or unseen count needed for later localized copy; no resource IDs or
hard-coded presentation sentences enter the domain. Practice recommendations
carry only `PracticePreset(scope, source)`. Question count and level selection
remain the Practice Builder's defaults, and no recommendation starts an attempt.
There is no random choice, wall-clock input, persistence, ML/LLM step, streak,
engagement optimization, or blended score.

## Recommended Next

`LearningRecommendationResolver` is the only place the policy's inputs are
assembled, and it makes no decision of its own. It receives the completed history
of one `AssessmentHistoryStore` emission together with the
`LearningProgressSnapshot` the caller has already derived from that same emission,
asks for the unresolved-mistake count with that history, takes the recent context
from the shared `TestAttempt.toRecentStudyContext`, and hands the result to
`LearningRecommendationPolicy`. It does not inject `LearningProgressService`, does
not read `AssessmentRepository`, and holds no cache: one history emission produces
one progress derivation, shared by Topic learning context and the recommendation,
so the two cannot describe the same history differently.

The count arrives through the narrow `UnresolvedMistakeCounter`, wired in DI to
`MistakeReviewService::countUnresolved`. That keeps the recommendation domain from
depending on the Mistake Review feature for one integer, and keeps it unable to
reach review content or the shared cache. A failing count propagates rather than
being read as zero: zero is a fact the policy acts on — it falls through to weak
areas and then to coverage — so substituting it for an unknown would recommend
practice on evidence nobody established.

Only `AssessmentHistory.Loaded` produces a recommendation. Loading and Failed
history are unknown, not empty, and are never translated into zero completed
attempts; the deterministic new-user recommendation therefore belongs to a
genuinely loaded empty history and to nothing else.

Presentation is `TopicBrowserUiState.Content.recommendedNext`, optional enrichment
beside `continueStudying` rather than a screen state, so a failed derivation costs
only the card. `RecommendedNextUiModel` carries the domain's `target` and
`rationale` verbatim and adds one resolved Topic name, because an unseen rationale
identifies its Topic by stable ID and the label has to come from the catalogue the
screen already loaded. The Composable switches on the typed rationale to select
localized copy and re-derives nothing; a missing weak-area name or an unresolvable
Topic name degrades the wording rather than withholding the recommendation. The
card is withheld while a search query is active, exactly as the continue shortcut
is, and no rationale text participates in matching.

`LearningRecommendationRouteMapping` is where Navigation 3 begins.
`LearningRecommendationTarget.Topics` and `MistakeReview` are area roots, so
`AppNavigator.openRecommendation` selects them through the existing
`AppTopLevelDestination.forRoute` rule instead of pushing an area root onto another
area's stack; a practice target is pushed as the shared
`PracticePreset.toPracticeBuilderRoute` detail. A new learner is therefore returned
to the Topic list itself with no Topic chosen for them, a mistake recommendation
reaches the existing Mistake Review capability rather than `UNRESOLVED_MISTAKES`
practice, and a practice recommendation lands in the editable builder. A navigation
test asserts no recommendation target can reach an attempt, a result, or a route
that starts a run.

## Continue Studying

`ContinueStudyingResolver` answers a different question from the recommendation
policy above, and neither calls the other:

| Surface | Question | Input |
| --- | --- | --- |
| Continue Studying | "Where was I working, and how do I get back?" | Recency of completed focused study |
| `LearningRecommendationPolicy` | "What should I work on now?" | Mistakes, weakness, coverage |

The two are allowed to point at different places, and Continue Studying does not
disappear because the recommendation would go elsewhere. Both cards may be shown
at once, in that order: Recommended Next is the one policy-driven primary action,
Continue Studying stays a compact continuity shortcut beneath it, and neither is
deduplicated, suppressed, or altered because of the other. Neither turns the Topic
rows below them into recommendation cards.

Its only inputs are completed assessment history and current curriculum. History
supplies stable IDs through the persisted `TestAttempt.config`; current curriculum
decides whether those IDs still lead anywhere and supplies every label, so a
renamed Topic is renamed on the card with nothing migrated. There is no
`lastStudiedTopicId` column, preference, or resume flag: the information already
exists in completed history, so no Room migration was needed and completion
invalidation refreshes the shortcut through the same path Progress uses.

`TestAttempt.toRecentStudyContext` is the one definition of recent study, shared
with the recommendation policy's tie-break input. It refuses `IN_PROGRESS`
attempts and reports Mixed runs as `RecentStudyContext.Mixed`. Continue Studying
additionally needs the persisted practice source, which `RecentStudyContext`
deliberately does not carry, so the resolver narrows back to the stored
`AssessmentConfig.Focused` after that shared decision rather than duplicating it.

Selection walks completed history in its existing newest-first order and takes the
first entry that resolves to a currently usable context. It keeps walking rather
than stopping at the newest one, so a single stale attempt cannot hide a usable
context just behind it:

| Historical context | Result |
| --- | --- |
| Mixed run | Skipped; no Topic is inferred from its Questions |
| `IN_PROGRESS` attempt | Skipped; this feature never resumes |
| Active Topic | Return to Topic detail |
| Active Subtopic under an active Topic | Return to Topic detail at that Subtopic |
| Deprecated Subtopic, parent Topic still active | Degrade to the parent Topic |
| Subtopic missing entirely, or parent not active | Skipped; inspect older history |
| Missing or deprecated Topic | Skipped; inspect older history |
| Nothing usable left | No card |

Source decides the destination kind. An `ALL` run returns to *content*, because
reopening an assessment is a heavier answer than a shortcut tap asked for. A
targeted run — unseen, weak areas, or unresolved mistakes — returns to the
Practice Builder carrying `PracticePreset(scope, source)`, which the learner can
edit before anything starts. Those sources are history-derived and will have moved
on since the attempt completed; that is exactly why the builder is reopened and
re-preflighted rather than a previous run being restored. No Question IDs are
snapshotted and no eligible pool is carried.

`ContinueStudyingTarget` cannot represent an attempt ID, and
`ContinueStudyingRouteMapping` maps it only to `AppRoute.Topic` or the shared
Practice Builder preset route, so returning to a learning context can never become
resuming an assessment. A navigation test asserts no Continue target reaches
`FocusedPracticeAttempt`, `MixedInterviewAttempt`, or a route that starts a run.

The card lives on the existing Topics surface as optional enrichment on
`TopicBrowserUiState.Content`, never as a screen state of its own: unknown,
failed, or empty history and a failed resolution all leave Topics browsing,
searching, and opening exactly as they were, with the card simply absent.
`TopicBrowserViewModel` derives it from the same `AssessmentHistoryStore` emission
that already produces Topic learning context and the recommendation — one
sequential collector, no second history read — so all three derivations see the
same history and an older one cannot land on top of a newer. They fail
independently: a failed continue resolution cannot erase a valid recommendation,
and a failed recommendation cannot erase the shortcut or the learning context. The card is withheld while a search query is active: a
learner who has started typing has said what they are looking for, and the
shortcut is not a search result.

## The Practice Builder

Choosing a Topic or Subtopic no longer starts an assessment; it opens a builder
scoped to that stable ID, and the builder produces the `AssessmentConfig.Focused`
that assessment taking then runs. The screen is deliberately four decisions —
length, levels, source, and the scope it was opened from — rather than a general
assessment-settings surface, and every one of them is already answered on arrival:
the defaults are the previous one-tap run, so the flow costs a returning learner
one extra tap and no decisions.

`PracticeBuilderViewModel` owns every rule about what a runnable configuration is,
and the Composable owns none of them. The last-level protection is the clearest
case: an empty level set is representable in the domain and explicitly
non-runnable, so a screen that could reach it would strand the learner on a Start
button that can never work. Enforcing it in the state holder means one
implementation rather than one per control that touches levels.

The builder can be opened on an initial source as well as a scope.
`AppRoute.PracticeBuilderTopic` and `PracticeBuilderSubtopic` carry a typed
`PracticeQuestionSource` defaulting to `ALL`, which keeps the Topic Detail entry
semantically unchanged, and `PracticePreset.toPracticeBuilderRoute` is the single
mapping from a semantic practice intent to that route. One preset-capable route
per scope rather than a route per practice kind: a later shortcut surface reuses
this mapping instead of adding a parallel preset system. An arriving source seeds
the initial state and nothing else — count and levels stay the builder's defaults,
an unsupported source falls back to `ALL` exactly as `selectSource` refuses one,
availability is re-checked normally, the learner can still change it, and nothing
starts on arrival.

Source options carry availability as a property of the *policy*, not of the
learner's content. All four current sources are selectable. The builder reads that through
`AssessmentQuestionSelector.isSourceSupported`, which answers without loading any
content; probing by attempting a selection would read content, and completed
history, once per option just to render a screen. A selector test asserts the two
agree for every source, which is what keeps the duplicated `when` honest.

Choosing `UNSEEN`, `WEAK_AREAS`, or `UNRESOLVED_MISTAKES` changes nothing
structurally: it re-runs the same preflight against completed history, so Start is
enabled only when matching content exists. A supported source with nothing left to
ask is not an unavailable source, and the two states stay separate in the UI for
that reason.

Whether the current configuration has any content is a separate question, and it
is answered *before* Start through `AssessmentQuestionSelector.select` — never
through `AssessmentEngine.start`, which persists an attempt. Checking whether
practice is possible must not create practice as a side effect, so the builder is
given the read-only selection boundary and no repository or engine at all.

The configured run reaches assessment taking as typed route fields — scope ID,
count, levels, source — and never as Questions, answers, or curriculum text. Every
dimension travels because the destination rebuilds the config from the route, and
an absent dimension would silently become its default: practising every level when
the learner asked for one. The level list is normalised to authored order so an
identical setup is an identical back-stack entry. The scope's display name is
resolved from its stable ID on arrival rather than carried, so a renamed Topic
cannot appear under a label frozen into the back stack.

## Persisted practice configuration

Practice levels and source are part of the attempt record, as the nullable
`practice_levels` and `practice_source` columns added in schema v6. They were
deliberately left unpersisted while nothing could vary them — every stored FOCUSED
attempt genuinely was an all-levels `ALL` run — but that stopped being true the
moment the Practice Builder could start, say, an ADVANCED-only run: history would
have described an attempt the learner never made, and retake, which re-runs the
reconstructed config, would have widened the repeat back across the whole scope.

`MIGRATION_5_6` is a pure add of two nullable columns. Nothing is backfilled,
because a literal level list on a historical row would claim the learner chose
something they were never offered; instead a null on a FOCUSED row reconstructs as
the all-levels `ALL` semantics it always had, in the mapper, in one place. MIXED
rows keep writing null, since Mixed has no level or source dimension at all.
Levels are stored as comma-separated `QuestionLevel` names in enum order rather
than in a join table: it is a closed three-value set read only alongside its own
attempt, and normalising the order on write is what makes two identical selections
compare equal. `QuestionAttempt` and scoring schema are untouched.
