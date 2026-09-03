# Targeted Practice Selection

How a practice run is narrowed by scope, interview level, and question source. See [practice builder](practice-builder.md) for the surface that configures it and [recommendations](recommendations.md) for the policy that suggests one.

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
