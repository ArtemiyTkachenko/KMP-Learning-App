# Assessment Architecture

How an assessment is configured, run, scored, and persisted. See [overview](overview.md) for app composition and [progress](progress.md) for the statistics derived from completed attempts.

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

## Assessment Graph And Flows

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

The Mixed Android Interview product has its own top-level area, and the asymmetry with
Practice is intentional rather than an omission: **Practice is the learner choosing what to
work on, Interview is the app testing them.** There is no interview builder and no
configurable length, so the landing screen's job is to state what the session is before the
learner commits to twenty questions — its size, its scope, and the one rule that actually
differs from Practice: answers are reviewed when the interview is complete, not question by
question. That rule is the same one `AssessmentTakingViewModel` enforces by withholding
`PracticeFeedback` for a `Mixed` config, so the copy describes behaviour rather than
promising it.

Once the learner has finished an interview, `InterviewStartScreen` shows their record
through `InterviewStartViewModel`; each row opens the result it came from. The most recent
interview leads and carries its date, because "how did I do last time, and how long ago was
that?" is the question a returning learner has. A personal best is kept and kept *second*:
it is genuine information the data model already supports, but promoting it above the latest
result would turn a preparation tool into a high-score table, and the app has no streak,
points, or leaderboard anywhere else. The best row is omitted entirely when it is the same
attempt as the latest.

A first visit is a state rather than a gap. `InterviewHistoryUiState` keeps `Loading` and
`Empty` distinct so the record area shows a spinner while the read is in flight and a short
"No interviews yet" note afterwards, explaining what will appear there. No empty table and no
zeroed score: a `0 of 20` would be a result the learner never got.

Starting an interview navigates with `MixedInterview(questionCount)` primitive
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

`AssessmentConfig.Focused` can scope a run to one Topic, one Subtopic, or a
non-empty set of Subtopics. All three travel through the same selection, engine,
persistence, result, and retake path; only eligibility and the order questions
are drawn in differ, and those are described in
[practice selection](practice-selection.md). The multi-Subtopic scope is a
generic assessment-domain capability with no learning-content dependency: it
holds stable Subtopic IDs and nothing that names what grouped them, so a stored
attempt keeps its original meaning even if that grouping is later re-authored.

Multi-Subtopic practice is entered from a Learning Unit (E21-06), through
`AppRoute.PracticeBuilderLearningUnit`, which carries the stable Unit ID and
leaves the derivation to the builder — see
[the Practice Builder](practice-builder.md). That entry is addressed by Unit, not
by scope: the scope-addressed route helpers still refuse a multi-Subtopic scope
explicitly rather than coercing it into a narrower or broader one, because a
derived set of concepts cannot be reversed into the Unit it came from. Once
configured, such a run starts, persists, resumes by attempt ID, and retakes
through the existing lifecycle, and its stored config names only Subtopics.

## Saved Questions On Review Surfaces

Saving is learner-owned state layered onto review content, never part of it.
`AssessmentReviewLoader` still means "historical `QuestionAttempt` + curriculum resolution",
`ReviewQuestionUiModel` still carries authored content only, and neither knows about saving.

`SavedQuestionStateHolder` is the app-scoped projection of the E18-01 `SavedQuestionRepository`,
registered once in `topicStudyPresentationModule` on `AppCoroutineScope`. It exposes
`SavedQuestionsState`, which keeps the repository's ordered `List<SavedQuestion>` as the canonical
value and derives an ID set for per-card membership, plus the `pendingQuestionIds` whose mutation is
in flight. The repository stays the source of truth: a mutation persists first and the visible state
is then re-read from it, so a card never shows a saved state that was not written, and a failed
write leaves the previous one standing.

Focused results, Mixed results, and Mistake Review each observe that one holder as a second,
independent state stream beside their own content, so a saved-state failure is never a screen-level
error and result loading never waits on saved state. Each ViewModel's `toggleSaved(questionId)`
ignores an ID that is not a `ReviewQuestionItem.Available` in its current state, which is why a
`ReviewQuestionItem.Missing` placeholder cannot be saved even though its stable ID is known.
Presentation is the shared `ReviewQuestionCard`'s optional `ReviewSaveAction`: a text
Save/Unsave control beside the question heading, with no affordance at all while saved state is
`Loading` or `Error`, because "not known to be saved" is not "unsaved".

Saving is orthogonal to everything derived from history. It does not change scoring, coverage,
weak areas, recommendations, or unresolved-mistake state, and no history invalidation follows it.

## Browsing Saved Questions

`AppRoute.SavedQuestions` is a detail of the Topics area, reached from a static entry in the Topic
Browser that is present during normal browsing and withheld from search results, exactly as the two
guided-learning cards are. The entry carries no count and reads no saved state, so the Topic Browser
gains no dependency on saved-Question persistence and the destination stays reachable when nothing
has been saved — which is the case its empty state exists for. Area navigation stays visible, since
browsing saved content is review rather than an assessment in progress.

`SavedQuestionsViewModel` observes the same app-scoped `SavedQuestionStateHolder` the three review
surfaces observe; it never reads `SavedQuestionRepository` itself, which is what makes a Question
saved on a result screen appear here, and one removed here disappear there. It adds exactly one
thing: `SavedQuestionContentResolver` maps each saved identity through
`CurriculumRepository.getQuestionById` — the historical resolver, never an ACTIVE listing — into
`SavedQuestionItem.Available` or `SavedQuestionItem.Missing`, preserving the repository's saved
order (`saved_at_epoch_millis DESC, question_id ASC`) exactly. DEPRECATED content resolves and
renders like any other; a null lookup is a `Missing` placeholder that keeps its position and stays
removable, because the learner still owns that identity; a *failing* lookup is a screen error with
Retry, since a curriculum that cannot be read is not evidence that a Question was retired. Retry
re-runs resolution against the loaded saved list explicitly, because a refresh that re-reads an
equal saved list produces no new `StateFlow` emission to react to.

`SavedQuestionContentUiModel` deliberately is not `ReviewQuestionUiModel`: that model describes one
historical attempt, and a saved Question has none — the learner may have saved it having answered it
either way. Nothing about correctness, selection, or score is fabricated to reuse
`ReviewQuestionCard`. What is shared is the neutral presentation in `QuestionContentComponents.kt`
— the answer-option container and tag, the explanation block, and the source links with their
open-failure notice — which both `ReviewQuestionCard` and the saved-Question card render.

Topic detail screens use a Material 3 top app bar for back navigation, with the
navigation icon invoking the existing Navigation 3 back-stack pop. Detail and
practice destinations should keep this phone-style toolbar affordance instead
of rendering a standalone text Back button in page content.

## Interview Simulation: What Was Considered And Not Built

The Interview is deliberately the least configurable thing in the app: twenty mixed
questions, no per-question verdict until the end, one result, one repeat. P2 asked whether
richer simulation features belong in the product. They were evaluated against two tests —
does the feature fit the model already built, and is it low-risk — and none of them passed
both. This records the reasoning so the question is not re-opened from scratch.

| Considered | Why it is not here |
| --- | --- |
| A countdown timer | Time pressure is a second scoring dimension. Every accuracy figure in the app — all-time, recent, per-Topic, weak-area — is answers over attempts, and a timed run either leaves those untouched, in which case the timer changes nothing and is decoration, or feeds them, in which case every derived metric changes meaning. It also needs an answer for what happens to an expired attempt, which the persistence model has no state for. |
| Recording elapsed time without a limit | Cheaper, but it is a schema change for a figure with no consumer: nothing in Progress, Recommendations, or the selection policies would read it, so it would be a column that exists to be shown on one card. |
| Skip and return to a question | The taking engine advances one question at a time and records each answer as it is given, which is what makes an interrupted attempt resumable and a mistake queue derivable. Deferred answers would need an "unanswered but visited" state threaded through the engine, the attempt entity, and scoring. |
| Choosing interview length or difficulty | The Practice Builder is where a learner configures a run, and the Interview's whole identity is that it is the one they do not configure. Adding options here would make the two surfaces near-duplicates and remove the reason the Interview exists. |
| A per-question confidence rating | Plausible and genuinely used in interview prep, but it is new learner-authored data with its own persistence, its own empty states, and no existing policy that would consume it. That is a feature, not a refinement. |

What P2 did change is compositional only, and is covered in
[adaptive layout](adaptive-layout.md): on an expanded window the landing page puts the
invitation and the learner's record side by side rather than stacking them, and the result
screen keeps the score, the notices, and the repeat action in a pane beside the transcript
instead of at the top of it. Neither adds a concept. The P1 no-history state is unchanged.
