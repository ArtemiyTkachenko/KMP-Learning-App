# Progress, Coverage, And Learning Context

How completed assessment history becomes learner-facing statistics, and how those statistics reach the study surfaces. See [assessment](assessment.md) for where that history comes from, and [study progress](study-progress.md) for learner-owned Lesson study state, which is a separate responsibility and is not derived from assessments.

## Derived Learning Progress

Completed history feeds the shared `LearningProgressService`, which derives a
`LearningProgressSnapshot` entirely in memory. Overall totals sum persisted
`AssessmentScore` values, while Topic and Subtopic observations use persisted
`QuestionAnswerState.Answered.isCorrect` values plus stable historical Question,
Topic, and Subtopic lookup. Every completed occurrence counts equally,
including focused, mixed, and retake attempts; derived statistics are not
persisted. A Topic or a Subtopic is weak after at least 5 observations below
70% accuracy — one threshold for both, because a recommendation should rest on a
pattern rather than on one unlucky Question.

The two sources answer slightly different questions, and are allowed to. Overall
totals come from the persisted score of each completed attempt and therefore
count every occurrence the learner answered, while the Topic and Subtopic
breakdown can only place an occurrence whose Question still resolves through
`CurriculumRepository.getQuestionsByIds`. A historical Question that no longer
resolves at all — which the never-delete import contract makes unreachable
through ordinary publishing — keeps its persisted occurrence in the overall
figures and drops out of the grouped ones, so the grouped answered counts may
legitimately sum to less than the overall answered count. Deprecation does not
cause this: `getQuestionsByIds` is the historical resolver and reads a DEPRECATED
Question, which keeps its Topic and Subtopic accuracy while leaving current
coverage.

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
result destinations; no progress snapshot or history summary is persisted. A row's
completion time travels as the domain `Instant` and is phrased at the point of
display — see *Dates* below — so no pre-formatted timestamp reaches the state.

The unresolved mistake count is both a report and a route. Progress exists to answer
two questions — how am I doing, and what should I work on next — and the most concrete
answer it holds to the second is a queue of questions the learner has already got
wrong, so the row opens Mistake Review by selecting that area exactly as the
navigation bar does. It remains a route and never a practice preset: the count spans
the whole curriculum while focused practice has to name a Topic or Subtopic, and
picking one on the learner's behalf would be an unexplained recommendation. Scoped
mistake practice is offered where a scope is actually known, on a queue entry. A
resolved queue keeps no action, because an empty destination is not worth a tap.

The dashboard presents the snapshot's three signals with an explicit hierarchy,
because they answer three different questions, are routinely different numbers,
and are not equally important to a learner opening the screen.

All-time accuracy leads as `ProgressHero`, the screen's one brand-gradient
surface. It is labelled as all-time rather than "overall", since an unqualified
accuracy figure beside a recent one is ambiguous, and it keeps `accuracyColor`,
because diagnosis is what this screen is for. The gradient is the second and
last call site of that token — see `AppSemanticColors` — and the argument for it
is the neighbour count: this page runs six or more containers down its length,
and the shared `AccuracyHeroCard`'s single surface-ramp step reads against that
many siblings as "the first card" rather than as the screen's answer. The two
gradient surfaces stay distinguishable by motion rather than by palette. The
completion hero counts a score out over the app's one celebratory duration; this
one settles into place over the ordinary content-reveal duration, because a
learner opening Progress to check on themselves is not being congratulated. The
hero has no section heading over it: it sat directly under a `TopAppBar` already
reading "Progress", introducing a surface that states its own subject in the
largest type on the screen.

Curriculum coverage sits inside that hero rather than in a card of its own. It
was one of three similarly sized summaries each leading with a percentage, which
is how a dashboard ends up with three headlines and therefore none; it is
context for the accuracy above it, and it now reads as context. Nothing it
states changed: it always prints its raw attempted/total counts beside the
percentage, because coverage and accuracy differ substantially for normal
learners and the denominator is what explains why; its meter uses the exact count
ratio rather than the rounded display percentage; a `null` percentage (an empty
ACTIVE bank) reports that there is no curriculum to cover instead of drawing 0%;
and the meter stays in the primary brand family rather than taking
`accuracyColor`, because colouring 30% coverage red would read as a bad score
when it only means most of the bank is still ahead of the learner.

Recent performance keeps its own `SecondarySummaryCard` directly under the hero,
because it is a different window over different evidence and routinely reads a
different number. Its figure dropped from near-hero weight to `MetricFigure` and
moved onto the title's own line, so the card has one thing to read at each level
rather than four at the same one. It prints the domain's question-weighted window
accuracy — never the mean of the plotted attempts — with a grammatically singular
or plural window label, and its counts as one quiet evidence line.

Text on the gradient is either `onPrimaryContainer`, which is the token's
documented contract, or one of the three `accuracyColor` bands and that colour at
`0.8` alpha for supporting lines. Those last two are outside the contract —
`accuracyColor` includes `incorrect`, which the completion hero deliberately
never renders — so `ProgressHeroThemeTest` asserts every one of them, composited,
against both endpoints of both sweeps.

The new-user Empty state is unchanged: a dashboard of zeroes is not a substitute
for guidance.

Below the standing group the hierarchy keeps falling. Weak areas carry the app's
warning tone once, as a small `partiallyCorrect` accent beside their section
heading, and the rows underneath keep the accent border and coloured figure they
already had and still pass no badge: one amber mark introducing a section says
"these need attention" once, where the same mark repeated down six rows stops
being a mark at all. The icon is decorative and announces nothing — the heading's
words are what carry the status, so colour is never the only channel. Topic
performance is one `ContentGroup` rather than a card per Topic: those rows are
the most homogeneous thing on the dashboard — a name, a score, a rate, a chevron,
every one of them — and a card each spent an edge saying what the heading above
already said. The length is bounded by the Topics the learner has answered
anything in, which is what makes an eagerly composed group the right shape for
them. Weak areas stay cards on purpose, because a weak row is singled out by an
accent border and a border is a property of a container; the contrast between the
bordered cards and the quiet table under them is now what separates "these need
attention" from "here is everything". Session history is the most tertiary thing
here and is drawn as a bare row on the page: no container at all, a smaller
shape, a denser inset, a `titleSmall` name, and the score and completion time
folded onto one supporting line. It stays a lazy list of its own rows rather than
a group because its length is unbounded — a group that composed four hundred
attempts to draw one border would be paying for the border with the scroll — and
it clips itself before its `clickable` so hover and press follow the row's
corners instead of drawing a band whose edges land on the text, which is
permanent on a pointer host. Its chevron, `Role.Button`, and stable-attempt-ID
navigation are unchanged.

Those four ranks — the gradient hero, the recent-performance card, the grouped
per-Topic table, and the bare history rows — are the dashboard's whole statement
of what matters more than what, and none of them changes colour to make it.

Motion on this screen belongs to the hero and to the chart, and to nothing else.
The hero's ring sweep and its accuracy count are one `Animatable` over the
ordinary content-reveal duration, with the coverage block fading in 100ms behind
them, so the whole entry lands inside 400ms; the chart's own reveal runs
independently in its card. Both are claimed by a `rememberSaveable` flag set
*before* the animation starts, which is what makes them once per visit rather
than once per composition: the dashboard's state is a `StateFlow` that re-emits
on every lifecycle resume and on every settled history refresh, and both surfaces
sit in scrolling lists. Without the flag a learner would watch their lifetime
accuracy count up again every time either happened, which would be the screen
claiming something had changed when nothing had. Weak areas, Topic rows, and
history rows render normally and are not staggered. The flags live in
presentation; no animation state reaches the ViewModel.

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
support. In practice the whole surface is gated harder than that:
`RecentPerformancePolicy.MinimumVisibleAttempts` is five, so the recent card — chart
included — does not appear until the window is full, and a three-point drawing
presented as a trajectory is unreachable. The three fixed guides are labelled with
their percentages, which is what makes the drawing a chart rather than a shape: a
learner can read that a point sits just under half, and the labels state that the
axis is the full 0-100 range rather than fitted to the data. Two of those three
are drawn dashed and faint, because a grid is a reading aid rather than content
and three solid rules across a small card put as much ink on it as the series
they measure; the 0% guide stays solid, since it is not a guide but the axis the
area sits on and the line the series rises from. The line carries a `primary`
area fade beneath it and no longer caps its width: five points across an expanded
pane span a small share of the height, so a bare polyline read as a scratch on
the card, and the previous 420dp cap left the right third of a desktop-width card
visibly empty. It is stroked as one path with round joins rather than as a
segment per pair, which is what stops round-capped segments beading at every
shared endpoint, and it is still the polyline through the observed values — no
curve fitting, because a smoothed line through five discrete session results
would draw values nobody scored. The newest attempt wears a halo rather than a
larger dot, so the one point the learner came for is findable without the series
acquiring two marker sizes, and a faint vertical carries it down to the labelled
scale. A series of one point draws its marker alone: a fill and a stroke across
zero width would be a sliver of colour claiming to be a quantity. On first appearance the whole series rises from the 0%
guide to its values together over the app's ordinary content-reveal duration — one
`Animatable` read in the draw scope, held by a `rememberSaveable` flag so a card
scrolling back into view does not redraw itself and imply something changed. It plots `attemptSeries` and not `answerSeries` — one visualization is the
budget — draws no direction colouring or "improving"/"declining" label, since the
domain deliberately exposes raw observations, and carries a semantic description
listing every plotted percentage oldest-first so the drawing is never the only
representation. It also states no up-or-down trend in text, for the same reason:
the domain publishes the raw series and derives no direction, momentum, or
velocity from it, so turning a comparison of two of those points into "+6%" would
be presentation reaching a verdict the rest of the app declines to reach. The
chart lost its own "Recent session trend" caption in the redesign — the card
above says what window this is and the axis beside it says what the scale is, so
a third label between them named the drawing without telling the learner anything
about it.

The drill-down leads with the same `AccuracyHeroCard`, carrying the Topic name as its
title and the all-time counts as its caption. Its percentage is nullable for the
same reason the card's parameter is: below `WeakAreaMinimumAnswered` there is no
honest figure, so the hero states the counts and lets the "not enough data" line
explain itself rather than drawing a ring at a number the learner never produced.
Weakness remains the domain's verdict and is still suppressed below that minimum,
and coverage stays a caption rather than a second figure, because it counts current
Questions once each while the line above it is all-time and occurrence-based.

The Subtopic rows beneath it are the one list in the app that passes
`PerformanceCard(comparesWithSiblings = true)`. They are parts of one whole measured
on one scale, and the question the learner opened the screen to ask is which of them
is worst; four percentages down the right-hand edge answer that only by being read
one at a time. The meter is drawn from the same `accuracyColor` as the figure and
clears its semantics, so the number is announced once. A row below the evidence
minimum has no percentage and therefore no meter, rather than a bar at a rate that
was never measured.

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

`AssessmentHistoryStore.history` is a `SharedFlow` with `replay = 1` rather than a
`StateFlow`, and that choice is the whole of a consumer's Retry. Every consumer
listed above *derives* from the history — over a curriculum that can be
unavailable while the attempt table reads perfectly well — so the two failures a
screen can show come from different places, and re-reading alone would not reach
the second one: a re-read of an attempt table nobody has written produces attempts
equal to the cached ones, and a `StateFlow` drops an emission equal to its last.
The store instead guarantees that **one `invalidate()` is one emission once the
resulting read settles**, whether the attempts changed, came back identical, or
could not be read at all. A consumer therefore recovers both failures by observing
this flow, with nothing of its own to arrange; `replay = 1` is what still lets a
returning destination render the cached history on its first frame.

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
