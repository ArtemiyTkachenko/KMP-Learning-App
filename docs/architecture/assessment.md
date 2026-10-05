# Assessment Architecture

How an assessment is configured, run, scored, and persisted. See [overview](overview.md) for app composition, [progress](progress.md) for the statistics derived from completed attempts, and [curriculum visibility](curriculum-visibility.md) for how hidden optional content affects new, in-progress, and completed attempts.

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
`StartAssessment` coordinates the engine plus initial persistence.
`AssessmentRetakeService` reuses that same boundary after resolving a completed
source attempt. These are lazy capabilities rather than startup initializers;
Android still awaits curriculum import before entering `App()`.
E09 and E10 should depend on `CurriculumRepository`, `AssessmentRepository`,
`AssessmentEngine`, `StartAssessment`, and `AssessmentRetakeService`, not Room
DAOs or entities.

Assessment taking is shared by focused practice and mixed interviews through
`AssessmentTakingViewModel -> AssessmentEngine -> AssessmentSession`.
Each destination that can create an assessment owns an `AssessmentLaunchCoordinator`.
The coordinator resolves `AssessmentLaunchViewModel`, accepts either assessment
configuration, and calls `StartAssessment` while the originating screen remains
visible behind a modal loading state. Only after the initial `TestAttempt` is saved
does the destination report its attempt ID to the shell, whose only responsibility
is pushing the appropriate route. Startup or no-content failures therefore remain
on the originating screen and can be retried without placing a transient configuration
in the saved back stack or teaching the root composable feature launch protocols.
`AssessmentTakingViewModel` accepts only that stable
attempt ID and reconstructs the runtime session through `AssessmentSessionLoader`,
with the persisted `TestAttempt.config` remaining authoritative. Product wrappers
provide titles and navigation while reusing the same question, submission,
progress, and explicit-completion state machine. The
taking screen pins a linear meter under its top bar, driven by the same
`questionNumber`/`totalQuestions` as the counter, so how far through the assessment
the learner is stays answerable while they read a long question.

The progress meter is pinned above the scrolling pane rather than placed in it, so how far
through the assessment the learner is stays answerable while they read a long question. It is
given the content column's width explicitly, because "outside the pane" and "the width of the
pane" are only the same thing at a compact width: past `AppContentWidth.Standard`'s cap the
question column is centred and a full-bleed meter measured a column it no longer lined up with.
It draws with the product's shared `ProgressMeter` — not a second `LinearProgressIndicator` with
Material's track gap, stop indicator and default height, which is what it was.

The commit action is separated from the options by a section break rather than by the same gap
that separates the options from each other: choosing and committing are different acts, and on a
touch screen a button one option-gap below the last option is a mis-tap. Finishing follows the
same busy-control rule as every other action in the app — the button keeps its place, its size and
its emphasis, and states its condition as a word beside an 18dp spinner rather than replacing its
label with Material's 40dp standalone indicator.

The Mixed Android Interview product has its own top-level area, and the asymmetry with
Practice is intentional rather than an omission: **Practice is the learner choosing what to
work on, Interview is the app testing them.** There is no interview builder and no
configurable length, so the landing screen's job is to state what the session is before the
learner commits to twenty questions — its size, its scope, and the one rule that actually
differs from Practice: answers are reviewed when the interview is complete, not question by
question. That rule is the same one `AssessmentTakingViewModel` enforces by withholding
`PracticeFeedback` for a `Mixed` config, so the copy describes behaviour rather than
promising it.

When a `Focused` run does reveal an answer, it reveals it **on the options themselves**. The
practice screen and the results screen share one outcome vocabulary, declared in
`assessment_review/QuestionContentComponents.kt`: `AnswerOutcome` for a single option
(correctly selected, incorrectly selected, missed, or unremarkable), `QuestionOutcome` for
the question as a whole, and one derivation and one set of colours for both. That code used
to be private to the review card, because results were the only place a learner met it;
formative practice shows the same thing seconds after an answer instead of minutes, so the
boundary moved rather than being copied. `AssessmentTakingScreen` owns no colour rule of its
own, and the two surfaces cannot drift into teaching two visual languages for one fact.

The partial case is derived at presentation from `selectedAnswerIds` against
`correctAnswerIds` and never reaches scoring. `PracticeFeedback` still carries only
`isCorrect`, and a partially correct answer is recorded exactly as incorrect as it always
was; what changed is that the learner is told which kind of wrong it was.

The verdict and explanation open beneath the options, which on a long question is below the
fold. When feedback first arrives for a question, `QuestionContent` scrolls the least distance
that shows them: nothing if they already fit, their bottom to the viewport's bottom if they fit
but are cut off, and the verdict to the top if the reveal is taller than the viewport. The
distance comes from the reveal's final measured height and runs on `AppMotion.spatialSpec()`
alongside the expansion, not after it. It happens once per question — a `rememberSaveable` flag
inside `key(question.id)`, seeded as done when the screen appears with feedback already shown —
and a learner's own drag or fling cancels it.

## Completion

Both result screens are the same three shared pieces. `AssessmentResultLayout` is the adaptive
shell — one `LazyColumn` at compact and medium widths, two independently scrolling panes at
expanded, with the outcome pane declared first so traversal order matches the single-column
reading order. `AssessmentResultOutcome` is the summary block: the hero, the caveats about the
transcript, and the actions. `AssessmentCompletionHero` is the figure. Only three things differ
between the products — the completion title, the five retake strings, and the test tags — and the
Mixed result appends its per-Topic breakdown to the outcome pane after the shared block. There is
no `isInterview` flag anywhere in the path.

That breakdown is one `ContentGroup` rather than a card per Topic. Its rows are the most
homogeneous thing on the screen — a Topic, a score, a rate, every one of them — they navigate
nowhere, and there are at most as many as the interview drew Topics from, so a card each spent an
edge saying what the heading above already said. It did that directly above a transcript of
question cards that genuinely are separate surfaces, which left the two halves of the summary
reading as one undifferentiated column of boxes. Every row is inert and therefore chevron-free: a
result is a record of what happened and there is no per-Topic destination to reach from one.

`AssessmentCompletionHero` is the product's one use of the hero gradient. It states the score as
the display figure (`8 / 10`) with the percentage beneath it (`80% correct`), a determinate ring
beside it, and nothing else; below five questions the percentage and the ring are both withheld,
because four questions can only produce 0, 25, 50, 75, or 100 and reporting a percentage from one
of them claims an accuracy the run never measured. One `Animatable`, held in a `rememberSaveable`
flag so it runs once per visit rather than once per composition, counts the numerator and sweeps the
ring as a single movement. The figure's node overrides its own `text` with the settled value, so the
score is readable — to a test and to a screen reader — on the first frame, and the ring's
`progressBarRangeInfo` is cleared rather than described, because the number is already written twice
beside it.

Performance emphasis on that card is `ResultEmphasis`, not `accuracyColor`: a run below the
domain's weakness threshold resolves to `partiallyCorrect`, never to `incorrect`. The error role
belongs to a wrong answer and a failed operation, not to a verdict on the learner, and the figure
itself stays `onPrimaryContainer` at every band so the brand remains the dominant colour.

Both of those lists, and the Progress dashboard's per-Topic table, draw the shared `AccuracyRow`
rather than writing the row out again: three surfaces reached the same shape within two changes of
each other, and three copies are three chances to disagree about the type scale of a percentage. It
is deliberately not `PerformanceCard` with a flag. The card carries an accent border, a comparison
meter, and a low-emphasis action line, all of which are container features a grouped row cannot
hold, so the two stay separate components over one shared reading. The row merges its descendants
whether or not it is navigable — a `clickable` merges on its own, but an inert row inside a group
has no card edge left to imply that its name, score, and rate belong together, and unmerged it
would be announced as three unrelated fragments.

`AssessmentResultOutcome` owns the action hierarchy, which depends on what the run produced rather
than on which product it was. With unresolved mistakes, practising them is the primary action and
the retake is the outlined alternative; with nothing left to fix there is no remediation to offer, so
the retake takes the filled weight. `AssessmentActionEmphasis` is how `AssessmentRetakeAction`
receives that, and a busy retake keeps its enabled colours in either weight because its spinner and
changed label already say it is working.

Once the learner has finished an interview, `InterviewStartScreen` shows their record
through `InterviewStartViewModel` as one `ContentGroup`; each row opens the result it came from.
At most two rows ever reach it and they answer the same question about the same thing, so the pair
is one record with two lines rather than two unrelated results stacked under a heading. The most recent
interview leads and carries its date, because "how did I do last time, and how long ago was
that?" is the question a returning learner has. A personal best is kept and kept *second*:
it is genuine information the data model already supports, but promoting it above the latest
result would turn a preparation tool into a high-score table, and the app has no streak,
points, or leaderboard anywhere else. The best row is omitted entirely when it is the same
attempt as the latest.

A first visit is a state rather than a gap. `InterviewHistoryUiState` keeps `Loading` and
`Empty` distinct so the record area shows a spinner while the read is in flight and a short
"No interviews yet" note afterwards, explaining what will appear there. No empty table and no
zeroed score: a `0 of 20` would be a result the learner never got. History that could not be
read or projected is `Unavailable`, which omits the record area entirely: it is unknown rather
than empty, so it neither shows "No interviews yet" nor blocks starting an interview.

Starting an interview passes `AssessmentConfig.Mixed` to the Interview destination's
launch coordinator. Balanced selection and initial persistence finish before
`MixedInterviewAttempt(attemptId)` is pushed, so saved navigation contains only
durable attempt identity and restoration cannot start the assessment again.
Focused destinations use the same coordinator and push
`FocusedPracticeAttempt(attemptId)` after persistence.
Completion replaces the attempt entry with `MixedInterviewResult(attemptId)`;
the result loads the persisted attempt from `AssessmentRepository`, projects it to
the learner's current [curriculum visibility](#curriculum-visibility), and uses
`AssessmentReviewLoader` with `CurriculumRepository.getQuestionsByIds` for ordered
historical review of the visible answers. The score shown is the projected attempt's:
the durable `AssessmentScore` when nothing is hidden, and a score recomputed from
persisted correctness over the visible answers when something is. Resolved review
Questions are grouped by `topicId` in attempt encounter order by
`topicAnswerCounts()`, a pure derivation over the visible review items alone;
`CurriculumRepository.getTopicById` then resolves historical names without ACTIVE
filtering, once per distinct Topic. A review item whose Question the curriculum no
longer holds has no Topic to attribute it to and is counted in no Topic, while the
score above the breakdown still counts it — the projection keeps unresolved answers.
Topic performance is derived in memory and is not persisted.

Historical review resolves through `getQuestionsByIds` because it must keep stored answer IDs
readable: an AnswerOption a completed attempt selected and a later bundle retired is kept in Room as
DEPRECATED and is still returned there, with its text. Each review card orders its options with
`withAnswersOrderedFor(attemptId)`, which is deterministic for an `(attemptId, questionId)` pair and
answer set, so it reproduces the arrangement the learner answered whenever the Question's
AnswerOption set is unchanged. The question-bank identity gate guarantees that for every revision
accepted under a stable Question ID. An attempt from before that gate may resolve against a
changed set, so its exact original arrangement — and agreement between the current answer key and
what it was scored against — is not promised. Its selected options stay readable, and the persisted
`Answered.isCorrect` remains the attempt's verdict; review never re-scores against the current key.

Mixed interview repeats follow the same persisted-retake boundary as focused
practice. The Mixed result delegates creation to `AssessmentRetakeService`,
keeps the completed source result in the back stack, and pushes
`MixedInterviewAttempt(retakeAttemptId)` only after the new attempt has been
saved.

Both result surfaces drive that boundary through one owner,
`AssessmentRetakeController`, rather than a copy each. It holds
`AssessmentRetakeState` — `Idle`, `Creating`, `Created(attemptId)`,
`SourceAttemptNotFound`, `NoEligibleQuestions`, `Error` — beside the result
content rather than inside it, and publishes the created identity once through a
buffered channel. `Created` is terminal until the destination confirms that the
same identity reached navigation, which is what stops a second durable attempt
being created in the window between persistence completing and the buffered
event being consumed. The three failure states do not block re-entry, because
nothing durable was created. Each result ViewModel keeps only what differs:
whether there is a loaded result to repeat at all, and the wording its screen
puts on each state. That route reopens the persisted session through
`AssessmentSessionLoader`, so balanced selection and `AssessmentEngine.start()`
occur once during retake creation rather than again when the assessment screen
opens.

E09-04 completes the retained session through `AssessmentEngine`, persists the
completed attempt before replacing focused-practice navigation with a stable
attempt-ID result route, and loads historical review through
`AssessmentRepository` plus `CurriculumRepository.getQuestionsByIds`. Deprecated
or missing historical questions are represented per review item without
changing the durable score. Retake behavior remains deferred to E09-05.

E09-05 exposes repeat practice from the result screen through
`AssessmentRetakeService`. The service creates and persists a fresh attempt;
`FocusedPracticeAttempt` then carries only its stable ID, and
`AssessmentSessionLoader` reconstructs the runtime session without calling
`AssessmentEngine.start()` a second time. The source result remains below the
retake in the back stack.

The complete E09 focused-learning path is `TopicBrowser -> TopicDetail ->
AssessmentConfig.Focused -> StartAssessment -> FocusedPracticeAttempt`, with
durable `TestAttempt` checkpoints through `AssessmentRepository`, explicit
completion, and `FocusedPracticeResult(attemptId)` historical reconstruction. A
repeat uses `AssessmentRetakeService`, then navigates by the persisted
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
Presentation is the shared `ReviewQuestionCard`'s optional `ReviewSaveAction`, rendered by
`QuestionBookmarkAction` beside the question heading, with no affordance at all while saved state is
`Loading` or `Error`, because "not known to be saved" is not "unsaved". That control is the app's
one statement of saved state: a filled bookmark when saved and an outlined one when not, the word
beside it, and the current value published separately as `stateDescription` and `toggleableState`,
so shape, fill, word, and the accessible reading all change together. It lives in
`QuestionContentComponents.kt` rather than in the review card, because its second caller is the
Saved Questions collection.

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
`CurriculumRepository.getQuestionsByIdsForCurrentContent` — never an ACTIVE listing — into
`SavedQuestionItem.Available` or `SavedQuestionItem.Missing`, preserving the repository's saved
order (`saved_at_epoch_millis DESC, question_id ASC`) exactly. The resolver also applies the
learner's current [curriculum visibility](#curriculum-visibility): a resolved Question of a
hidden Topic is omitted. The holder itself is never filtered. DEPRECATED content of a visible
Topic resolves and renders like any other; a null lookup is a `Missing` placeholder that keeps its position and stays
removable, because the learner still owns that identity; a *failing* lookup is a screen error with
Retry, since a curriculum that cannot be read is not evidence that a Question was retired. Retry
re-runs resolution against the loaded saved list explicitly, because a refresh that re-reads an
equal saved list produces no new `StateFlow` emission to react to.

The current-content read is the historical one minus retired AnswerOptions. Like assessment review
it leaves Question status unfiltered, so a DEPRECATED Question stays saved and reviewable. Unlike
review it returns only the options the curriculum authors now: a saved Question shows current
authored content for a stable identity, not an attempt transcript, so an option retained only
because some past attempt selected it has nothing to explain there.

`SavedQuestionContentUiModel` deliberately is not `ReviewQuestionUiModel`: that model describes one
historical attempt, and a saved Question has none — the learner may have saved it having answered it
either way. Nothing about correctness, selection, or score is fabricated to reuse
`ReviewQuestionCard`. What is shared is the neutral presentation in `QuestionContentComponents.kt`
— the answer-option container and tag, the explanation block, the source links with their
open-failure notice, `QuestionDisclosure`, and `QuestionBookmarkAction` — which both
`ReviewQuestionCard` and the saved-Question card render.

The disclosure is shared and its *default* is not, because the default is a statement about the
surface. A result transcript opens the Questions the learner got wrong, because they came to read it
through. A saved collection opens nothing: the question text is the browsing key and the detail is
on request. The saved card previously had no disclosure at all, so a collection saved over weeks was
that many permanently open blocks of options, explanation and sources, and finding one meant
scrolling past all the others in full. The screen also states its own size, as the Mistakes queue
beside it always has.

Saved state is stated the same way here as on the review surfaces, because it is the same fact.
Every entry on this screen is saved by construction, so each card renders `QuestionBookmarkAction`
with `isSaved = true` and pressing it performs the removal the screen already had — the control
says *this Question is saved; this toggles that*, not *delete this item*. A `Missing` placeholder
carries the same control for the same reason: the content is gone but the saved identity is not,
and that identity is what a bookmark represents. The surfaces keep separate test handles
(`reviewQuestionSaveTag`, `savedQuestionRemoveTag`) over the one shared control, so neither
surface's tests depend on the other's. Mutation semantics are unchanged: only the affected
Question's control disables while its own write is in flight, and the row leaves through
`animateItem()`. State switching goes through the shared `ScreenStateTransition` keyed on the state
class, so `Loading` becoming a collection fades while removing a Question — which stays inside
`Content` — does not.

Topic detail screens use a Material 3 top app bar for back navigation, with the
navigation icon invoking the existing Navigation 3 back-stack pop. Detail and
practice destinations should keep this phone-style toolbar affordance instead
of rendering a standalone text Back button in page content.

## Curriculum Visibility

The learner can hide the optional Kotlin Multiplatform Topic. Assessment applies that
differently to attempts that do not exist yet, attempts in progress, and completed
attempts. The system-wide model is [curriculum visibility](curriculum-visibility.md).

**Starting.** Selection reads `VisibleCurriculumRepository` and `VisibleAssessmentHistory`,
so a new attempt — focused, Mixed, or a retake — cannot contain a Question from a hidden
Topic. See [practice selection](practice-selection.md#curriculum-visibility).

**In progress: atomic.** `AssessmentSessionLoader` resolves an in-progress attempt's
Questions through the identity read, because the persisted attempt owns its Question
sequence. If any Question that resolved belongs to a hidden Topic, the result is
`AssessmentSessionLoadResult.ContentUnavailable`, and `AssessmentTakingViewModel` shows
`Unavailable` with no Retry. The attempt is **not** partially filtered. Numbering, the first
unanswered position, completion and the stored score all describe one persisted sequence,
and a session with a Question removed for presentation would disagree with storage.
Visibility is checked before the missing-Question walk, so an attempt holding both a hidden
and a missing Question is unavailable. An ID that does not resolve at all is still
`MissingQuestion`. A session already on screen is withdrawn as soon as one of its Questions
becomes hidden, and a core session is unaffected. The stored attempt is never changed, so
it resumes exactly where it was once the content is visible again.

**Completed: projected.** A completed attempt is a historical record, so both result
screens project it through `VisibleHistoryProjection`, the rule every history consumer uses:

- hidden Questions are omitted from the review, and the visible transcript keeps its
  original order;
- the score is recomputed from persisted `Answered.isCorrect` over the visible answers, and
  never from the current answer key;
- the Mixed Topic breakdown is counted from visible review items, so a hidden Topic has no
  row;
- when anything was hidden, the summary states "N Kotlin Multiplatform questions are hidden
  by your learning-content setting." (plural `assessment_review_hidden_questions`) directly
  under the score, as a neutral note rather than a warning. This is audit decision D-2: one rule everywhere, and the interview record shows
  the same projected score;
- a result with no visible Question is `Unavailable`, not `AttemptNotFound`, and offers no
  retake. The attempt still exists and returns unchanged when shown.

The attempt ID, timestamps and configuration are unchanged, and nothing is written back.
`AssessmentReviewLoader` stays historical and unfiltered, and each caller hands it what is
currently visible.

```text
IN_PROGRESS  → atomic: any hidden Question makes the session unavailable
COMPLETED    → historical record: safely projected to its visible Questions
```

The two rules are deliberately different, not two inconsistent implementations. An
in-progress attempt is still a state machine whose next step depends on its full sequence.
A completed attempt is finished, and showing its visible part cannot change what it
records.

**Saved Questions.** `SavedQuestionStateHolder` keeps every raw saved identity, because the
result and Mistake cards need to show whether any Question on screen is saved.
`SavedQuestionContentResolver` omits resolved hidden Questions. An unresolved ID stays
`Missing`, because its ownership is unknown. The collection's count covers visible entries
only, and a visibility change re-resolves the same saved list without reading or writing the
saved table.

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
