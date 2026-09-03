# Progress, Coverage, And Learning Context

How completed assessment history becomes learner-facing statistics, and how those statistics reach the study surfaces. See [assessment](assessment.md) for where that history comes from.

## Derived Learning Progress

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

Mistake Review also presents the shared Saved Questions state described in
[assessment](assessment.md), through the same `ReviewQuestionCard` the result screens use. The two
are independent: saving or unsaving an entry never resolves it, and only a later correct answer
takes it out of the queue. The E17-04 scoped practice shortcut is unchanged and stays a separate
action on the entry.
