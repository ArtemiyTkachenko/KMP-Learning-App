# Learning Recommendations

How the app decides what to suggest next, how it returns the learner to recent work, and how a study surface turns a signal it is already showing into practice. See [practice selection](practice-selection.md) for what the resulting practice run then selects.

Three separate concepts share one payload and must not be conflated:

| Concept | Chooses | How many actions |
| --- | --- | --- |
| Recommended Next | The single best action, globally | At most one |
| Continue Studying | Where the learner last worked | At most one |
| Contextual practice shortcut | Nothing; it offers what the visible signal already says | Zero, one, or more per scope |

All three hand the same `PracticePreset(scope, source)` to the same
`toPracticeBuilderRoute()` mapping, and the Practice Builder never learns which
one produced it.

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

## Continue Studying

`ContinueStudyingResolver` answers a different question from the recommendation
policy above, and neither calls the other:

| Surface | Question | Input |
| --- | --- | --- |
| Continue Studying | "Where was I working, and how do I get back?" | Recency of completed focused study |
| `LearningRecommendationPolicy` | "What should I work on now?" | Mistakes, weakness, coverage |

The two are allowed to point at different places, and Continue Studying does not
disappear because the recommendation would go elsewhere.

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
that already produces Topic learning context — one sequential collector, no second
history read — so both derivations see the same history and an older one cannot
land on top of a newer. The card is withheld while a search query is active: a
learner who has started typing has said what they are looking for, and the
shortcut is not a search result.

## Contextual practice shortcuts

A contextual shortcut is a local affordance, not a policy. It fires only where a
study surface is *already displaying* a learning signal that names a Topic or
Subtopic, and it translates that signal into the practice source EPIC-16 already
has for it:

| Surface | Visible signal | Preset |
| --- | --- | --- |
| Progress weak-area row | The row is in `LearningProgressSnapshot.weakAreas` | `WEAK_AREAS` for the row's stable ID |
| Topic detail, Topic | `LearningContextUiModel.isWeak` | `WEAK_AREAS` for the Topic |
| Topic detail, Topic | `LearningContextUiModel.hasUnseenQuestions` | `UNSEEN` for the Topic |
| Topic detail, Subtopic row | The same two facts, per Subtopic | `WEAK_AREAS` / `UNSEEN` for the Subtopic |
| Mistake Review entry | The entry is in the unresolved queue | `UNRESOLVED_MISTAKES` for the Question's Subtopic |

Nothing is re-derived. Weakness is the domain's `isWeak` verdict, copied verbatim,
and is never inferred from the accuracy figure displayed beside it.
`hasUnseenQuestions` only restates the coverage counts already on screen, and is an
observation about whether the action is worth offering — the unseen stable IDs
themselves are still computed by `AssessmentQuestionSelector` from completed
history at the moment practice is configured. The unresolved queue stays
`UnresolvedMistakeDerivation`'s, and the clicked Question supplies its Subtopic as
*context*, never as a candidate list; no Question ID travels in a route.

Two signals can be true for one scope. Both actions are then offered, because the
learner chose to look at that scope; inventing a precedence between them would
turn the feature into a second recommendation policy. Ordinary `ALL` practice is
untouched and remains the primary action on Topic detail.

An aggregate signal gets no shortcut. Curriculum coverage and the global
unresolved-mistake count on Progress identify no Topic or Subtopic, and there is
no global focused scope, so picking one — the first, the weakest, the one holding
the most mistakes — would be a scope chosen behind the learner's back. Mistake
Review remains the global mistake capability.

Absent analytics (`learningContext == null`) mean *unknown*, not "not weak" and
not "nothing seen". No shortcut is inferred from an absent context, and ordinary
practice stays available.

Nothing is preflighted on the originating surface and no candidate count is
cached. Learning state can legitimately change between a shortcut being drawn and
being tapped; the builder is the screen that re-checks against current content and
reports an empty result honestly.
