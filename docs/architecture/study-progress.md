# Learner Study Progress

What "studied" means, which facts are durable, and how current learning progress is
derived when publisher-owned learning content changes. See [overview](overview.md) for the
Learning Content Model this state sits on top of, [progress](progress.md) for the
separate, assessment-derived statistics, and [persistence](persistence.md) for the
saved-question precedent the storage shape follows.

This document is the contract, written before anything implemented it: E22-02 owns
persistence, E22-03 the derivation, E22-04 the presentation, and E22-05 Continue Learning.
Their job is to satisfy the semantics recorded here rather than to settle them
independently. The persistence half now exists — `studied_lesson`, `MIGRATION_7_8`, and
`LessonStudyRepository`, described in [persistence](persistence.md) — and so does the
derivation, recorded under [What the derivation computes](#what-the-derivation-computes),
and the Learn presentation, recorded under
[Presenting study progress](#presenting-study-progress). All three left the semantics below
unchanged. Continue Learning remains unimplemented.

## Three responsibilities, not one

The application already keeps publisher-owned content apart from learner-owned history.
Study progress is a third responsibility beside them, not a widening of either:

| Responsibility | Answers | Owner | Durable |
| --- | --- | --- | --- |
| Authored learning content | "What material currently exists?" | Publisher | No; bundled and re-read |
| Study progress | "What has the learner intentionally marked studied?" | Learner | Yes |
| Assessment history | "What has the learner practised, and how did they do?" | Learner | Yes, already |

Three chains of reasoning are therefore invalid everywhere in this feature:

```text
Opened     != Studied
Studied    != Practised
Practised  != Mastered
```

A generic "learning progress" bucket that blurs them would make every later question —
what a percentage counts, what a re-authored Lesson does to it, what an assessment result
implies about reading — unanswerable. Keeping them apart is what makes each one derivable.

## What Studied means

A Lesson is studied because the learner explicitly said so, and for no other reason. None
of the following makes a Lesson studied:

- opening it, viewing it, or scrolling it;
- reaching the bottom of it;
- pressing Previous or Next, or navigating away;
- starting Unit practice, or completing Unit practice.

This is a product fact, not an implementation shortcut. Navigation and activity signals
record traffic, not intent: a learner skims a Lesson to find one code sample, opens the
wrong one from a Unit overview, or reaches the end of a short Lesson in a second. Reading
the bottom of a page is evidence that a screen scrolled, and inferring an accomplishment
from it produces progress the learner did not claim and cannot trust. An explicit mark is
the only signal that means "I consider this material studied", so it is the only signal
recorded.

Studied is reversible for the same reason it is explicit — a learner who marked the wrong
Lesson, or who wants to revisit material, must be able to say so:

```text
Unstudied  --explicit mark-->  Studied  --explicit unmark-->  Unstudied
```

Unmarking removes the current studied fact. It does not record a second, historical
"unstudied" event, and this feature has no event log: the model holds the learner's
current claim about each Lesson, not the history of how that claim changed. E22-04 owns
the wording and affordance of the control; this contract owns only the behaviour.

## Persisted identity and facts

The single piece of publisher-owned identity that learner study state may store is the
stable Lesson ID. Conceptually the durable fact is:

```text
lessonId    the stable Lesson identity
studiedAt   when the learner explicitly marked it
```

Nothing else. Copying publisher-owned material into learner-owned storage is forbidden,
including the Lesson title, summary, sections or body; the Unit ID, Unit title, Topic ID,
or Topic title when stored only as duplicated hierarchy; authored order; and primary or
supporting Subtopic mappings. Every one of those is current publisher information and must
be resolved from the current learning document at read time, exactly as the Learn surfaces
already resolve titles from `LearningContentRepository` rather than from the navigation
route. A copy would be a snapshot that quietly disagrees with the Lesson the learner is
looking at.

The corollary is that the storage layer must not require a relational foreign key from a
study record to publisher-owned content. Learning Lessons are bundled publisher documents
validated in memory, not Room-owned rows, so there is nothing to point a constraint at;
more importantly, a learner's claim is allowed to outlive the material it was made about.
This is the same boundary `saved_question` already draws against `question`.

Facts are persisted; state is derived:

| Information | Owner | Persisted |
| --- | --- | --- |
| Lesson title, summary, body, authored order | Publisher learning content | No study-state copy |
| Lesson studied fact | Learner study state | Yes |
| Studied timestamp | Learner study state | Yes |
| Unit studied count | Derived | No |
| Unit ACTIVE Lesson total | Derived from current content | No |
| Unit completion | Derived | No |
| Topic studied count and total | Derived | No |
| Topic completion | Derived | No |
| Practice score, accuracy, weakness | Assessment history | Existing persistence only |
| Continue Learning target | Derived | No |

No aggregate is stored: not `studiedLessonCount`, `unitLessonCount`, `unitPercent`,
`unitComplete`, `topicStudiedCount`, `topicLessonCount`, `topicPercent`, `topicComplete`,
or `continueLearningLessonId`. Every such value is a function of the current publisher
hierarchy and the current set of studied Lesson IDs, so a stored copy could only be a
cache that goes stale the moment content is republished. This mirrors what the repository
already does with assessment statistics, coverage, and Continue Studying, none of which
has a column of its own.

## Time semantics

The recorded time means "when the learner explicitly marked this Lesson studied", and
nothing more:

- the first explicit mark establishes `studiedAt`;
- marking an already-studied Lesson is idempotent and preserves the original `studiedAt`;
- unmarking removes the record;
- a later mark establishes a new `studiedAt`.

There is no `lastOpenedAt`, `lastReadAt`, reading duration, completion history, or content
revision stamp. Each of those would be either an activity signal this contract rejects or
a second source of truth about material the publisher already owns.

## Re-authoring lifecycle

Stable Lesson identity, and only stable Lesson identity, decides whether a learner's claim
survives re-authoring.

**An editorial revision that keeps the Lesson ID keeps the study record.** Improving
wording, correcting an example, adding explanation, reordering sections, updating Sources,
or rewriting code samples all leave the Lesson responsible for the same learning, so the
learner's claim about it remains true. Progress is not reset because the document bytes
changed.

**A change of learning responsibility requires a new stable Lesson ID.** When the material
changes enough that having studied the old Lesson should no longer count as having studied
the new one, the author publishes it under a new ID and, where appropriate, deprecates the
old one. Identity is how a semantic break is communicated, because the author is the only
party that knows one happened.

**Content hashing is rejected.** A changed content hash, changed JSON, changed body text,
changed Source URL, or reordered sections must never invalidate study progress, and no
part of this model compares Lesson bodies or couples a study record to a content version
or hash. Such a mechanism cannot tell a typo fix from a rewrite, so it would either reset
progress on every routine edit or, tuned the other way, miss the rewrite it exists to
catch. Worse, it makes the learner's record a function of publishing activity they never
took part in. The authoring decision is deliberate and belongs to the author; a hash is a
guess about it.

## Deprecated and missing content

A learner-owned study fact may outlive the ACTIVE lifecycle of the Lesson it names. Two
cases follow from that, and both resolve the same way:

| Situation | Record | Current progress |
| --- | --- | --- |
| Lesson still resolves, status DEPRECATED | Retained | Excluded from numerator and denominator |
| Lesson ID no longer resolves at all | Retained | Excluded; ignored without error |

A DEPRECATED Lesson raises neither the numerator nor the denominator of any current
progress figure, and therefore can never block completion of the current material: a Unit
whose ACTIVE Lessons are all studied is complete regardless of how much retired content it
once contained. An unresolvable record is harmless in the same way — derivation ignores
it, it never crashes a read, it never counts toward completion, and it never fabricates a
phantom Lesson on a screen. Publisher content is free to move on without the learner's
history being deleted, and the record stays addressable by its stable ID so persistence
can still read or remove it.

**E22 does not surface historical or orphaned study records to the learner.** Retention
here is data-lifecycle correctness, not a feature: the Learn surfaces describe the current
ACTIVE curriculum, and there is no study-history screen, no "retired material you studied"
list, and no orphan-record management UI in this epic. This is an intentional scope
decision. A later feature may expose study history if a real product need appears, and the
retained records are what would make that possible.

## Current Unit study progress

Unit progress is defined over the Unit's current ACTIVE Lessons only:

```text
studiedActiveLessons
--------------------          per current ACTIVE Unit
totalActiveLessons
```

A persisted study record contributes to the numerator only when its `lessonId` resolves to
a Lesson that is currently ACTIVE and currently belongs to that Unit. The denominator is
read from the current learning document and is never derived from stored study records —
counting the rows a learner happens to have is how a partially studied Unit would report
100%.

The derived result is a studied count, an ACTIVE Lesson total, and a completion state.
Completion is true only when:

```text
totalActiveLessons > 0  AND  studiedActiveLessons == totalActiveLessons
```

An ACTIVE Unit with zero ACTIVE Lessons is **empty**, an explicitly defined result that is
neither 0% nor 100%. The `0 == 0` reading would announce that the learner completed a Unit
containing nothing to study, and 0% would claim work is outstanding that does not exist.
Neither is honest, so the empty case is its own answer. E22-03 chooses the Kotlin result
type; this contract only requires that the case be represented rather than computed by
accident.

## Current Topic study progress

Study progress follows the **learning hierarchy**, not the assessment taxonomy. A Learning
Unit belongs to the Topic named by `LearningUnit.topicId` — its home Topic, the one it is
browsed under — and Topic study progress is derived from the current ACTIVE Units whose
home Topic is that Topic, together with those Units' current ACTIVE Lessons.

A Lesson's `primarySubtopicIds` and `supportingSubtopicIds` may name Subtopics owned by
other Topics; cross-Topic bridging is a core rule of the learning-content authoring
contract. Those mappings do not move study completion anywhere. A Compose Unit under
`android_ui` whose Lesson explains a lifecycle concept does not raise Lifecycle study
progress when it is studied — it raises the Compose Unit's, and `android_ui`'s. Subtopic
mappings continue to serve assessment and content relationships, which is what they were
authored for: they say what a Lesson teaches, not where the learner's browsing progress
lives.

The Topic aggregate is lesson-weighted, computed across Units rather than averaged over
them:

```text
studied ACTIVE Lessons across ACTIVE home Units
----------------------------------------------
    all ACTIVE Lessons across ACTIVE home Units
```

A Unit of two Lessons must not carry the same weight as a Unit of ten simply because each
produces one percentage:

```text
Unit A: 1 / 2 studied
Unit B: 9 / 10 studied

correct:   10 / 12   (83%)
incorrect: (50% + 90%) / 2   (70%)
```

An ACTIVE Unit with no ACTIVE Lessons contributes nothing to either side of that fraction;
it neither dilutes nor inflates the Topic.

A Topic is **empty** — again an explicit result rather than 0% or 100% — when it has no
current ACTIVE Learning Units, or when its ACTIVE Units collectively contain no ACTIVE
Lessons. There is no meaningful denominator in either case, and a Topic that is
practisable but not yet studyable is a normal state of this curriculum rather than a
learner who has fallen behind.

## What the derivation computes

E22-03 implements the two fractions above as `StudyProgressDerivation`, a dependency-free
`object` in `lesson_study` beside the persisted `StudiedLesson`. It is pure: it reads no
repository, no clock, and no assessment history, so the same hierarchy and the same studied
identities always produce the same result.

```text
deriveUnit(unit: LearningUnit, studiedLessonIds: Set<String>): LearningUnitStudyProgress
deriveTopic(topicId: String, units: List<LearningUnit>, studiedLessonIds: Set<String>): TopicStudyProgress
```

Study facts arrive as a `Set` of Lesson IDs rather than as `StudiedLesson` records, which is
what makes three of the rules above fall out of the arithmetic instead of needing special
cases: an orphan identity is simply never encountered in current content, a repeated
identity cannot weight anything, and the repository's newest-first row order — a boundary
convenience, not curriculum order — cannot reach the output. The persisted `studiedAt` is
not read at all, because current completion is a question about identity and weighting it
by recency would make the answer depend on history rather than on the current curriculum.

Three result types carry the answers, all in `StudyProgressModels.kt`:

| Type | Holds |
| --- | --- |
| `LessonStudyProgress` | The stable Lesson ID and whether it is currently studied |
| `LearningUnitStudyProgress` | The Unit ID, its current ACTIVE `LessonStudyProgress` list, and a summary |
| `TopicStudyProgress` | The Topic ID, its current ACTIVE home Units, and a summary |

`LessonStudyProgress` carries no title, summary, body, or Subtopic mapping: every Learn
surface already resolves those from `LearningContentRepository`, and a copy could only
disagree with the Lesson the learner is looking at.

`StudyProgressSummary` is where the empty case becomes explicit rather than accidental:

```kotlin
sealed interface StudyProgressSummary {
    data object Empty
    data class Progress(studiedCount, totalCount) { val isComplete get() = studiedCount == totalCount }
}
```

`Progress` requires `totalCount > 0` and `studiedCount in 0..totalCount`, and completion is
a derived property rather than a third constructor argument, so no caller can assemble a
result claiming that 2 of 5 Lessons are studied and the Unit is finished.
`StudyProgressSummary.of(studiedCount, totalCount)` is the single place a zero total becomes
`Empty`, which is how both empty cases in the contract above are produced rather than
computed by accident.

The Unit numerator is the count of `isStudied` entries in the Unit's current ACTIVE Lessons,
and the denominator is the size of that same list; the studied set is never counted. The
Topic derivation filters the supplied Units to `status == ACTIVE && topicId == topicId`,
derives each, and then counts over the **flattened** Lessons of those Units. Flattening is
what makes the Topic aggregate lesson-weighted rather than an average of per-Unit
percentages, and it is why an empty Unit contributes to neither side. Callers normally pass
`LearningContentRepository.getActiveUnitsByTopic`, which already satisfies both filters; the
derivation applies them anyway because it accepts an arbitrary Unit list, and a DEPRECATED
or foreign Unit reaching a Topic aggregate would be silent rather than obvious. Both
functions filter with `List.filter` and `List.map`, so authored Unit and Lesson order
survives the removal of ineligible items.

`StudyProgressService` is the thin IO half: it takes `LessonStudyRepository`, reads
`getStudiedLessons()` once per snapshot, collapses it to identities, and delegates. It
deliberately does **not** take `LearningContentRepository`. The Learn surfaces already own
their content reads, and the two failures must stay apart: an unreadable learning document
is a content error, while unreadable study state costs only the studied indicator over
content that still reads perfectly well. Its `isLessonStudied(lessonId)` answers
persistence's question — does a stored claim exist for this stable ID — and stays true for a
DEPRECATED or unresolvable Lesson; whether such a Lesson counts toward current progress is
decided by the content-aware derivation, which never rewrites or deletes the stored fact.

Nothing here is registered in Koin yet. The derivation is an `object`, and the service has no
consumer until E22-04, which owns the presentation loading behaviour and is therefore the
issue that knows whether it wants an app-scoped singleton. E22-03 also adds no state holder
and no `StateFlow` for the same reason.

## Studied and practised are independent

Assessment history remains the only source of practice evidence: attempts, correctness,
scores, weak areas, unresolved mistakes, question exposure, assessment-derived coverage,
and the existing recommendations. Study state therefore carries no `practised`,
`practiceScore`, `accuracy`, `mastered`, `weak`, `questionCount`, or `attemptId` field.

The two never write to each other. Marking a Lesson studied creates no `TestAttempt` and
changes no assessment history; completing an assessment creates no studied fact, however
closely the Questions match a Lesson. Study is a claim the learner makes about reading;
practice is measured performance. A screen may show them side by side — that is E22-04's
concern — but each stays independently sourced, so neither can be manufactured from the
other.

This creates a naming hazard worth stating plainly. `LearningProgressService`,
`LearningProgressSnapshot`, and `LearningContextIndex` already exist and answer questions
about completed assessments. They are not to be redefined to also mean Lesson completion
because the English word "progress" overlaps. E22-03 introduces a separately named study
derivation with its own responsibility, and the assessment services are not extended to
carry study state.

## Continue Learning

Continue Learning is a new concept beside the existing Continue Studying card, not a
redefinition of it:

| Surface | Question | Input |
| --- | --- | --- |
| Continue Studying | "Where was I working, and how do I get back?" | Completed assessment history and current curriculum |
| Continue Learning | "What current Lesson should I study next?" | Current ACTIVE learning content and current study records |

The two are separately named, separately derived, and allowed to point at different
places. Continue Studying's semantics, wording, inputs, and destinations are unchanged by
this epic.

The initial policy is deterministic and purely positional:

```text
current ACTIVE Learning Units, in authored LearningCurriculum Unit order
  -> their current ACTIVE Lessons, in authored Lesson order
     -> the first Lesson not currently marked studied
```

DEPRECATED Units and DEPRECATED Lessons are skipped and can never be the answer. Fully
studied Units are skipped as a consequence of the walk rather than by a special case,
because they contain no remaining unstudied Lesson. Authored order is the curriculum's
pedagogical order, which is precisely the sequencing information a "what next" answer
needs; anything cleverer would be a second recommendation policy.

The policy reads no assessment history, no most recent `TestAttempt`, no weak areas,
mistakes, or coverage, no navigation history, and no wall-clock time. It persists nothing:
there is no `lastReadLessonId`, `resumeLessonId`, or `currentLessonId`, because the answer
is already a function of content and study records. E22-05 added the smallest additive
ordered read `LearningContentRepository` did not yet expose in order to walk global
authored Unit order: `getActiveUnits()`, described in [overview](overview.md).

Three outcomes are distinguishable, and the last two must not collapse into each other:

| Outcome | Meaning | Carries |
| --- | --- | --- |
| Next | At least one current ACTIVE Lesson is unstudied | Stable Unit ID and Lesson ID |
| Complete | Current ACTIVE Lessons exist and all are studied | Nothing to open |
| Empty | The curriculum contains no ACTIVE Lesson eligible for study | Nothing to open |

"You have studied everything" and "there is nothing here to study" are different
statements about the learner and about the content, and rendering one as the other would
either congratulate a learner who has done nothing or hide a finished course.

### What E22-05 implemented

`ContinueLearningPolicy.resolve(units, studiedLessonIds)` is the whole decision, and it is
an ordinary nested walk over a list and a set — no repository, no coroutine, no clock. It
returns `ContinueLearningOutcome`: `Next(ContinueLearningTarget(unitId, lessonId))`,
`Complete`, or `Empty`. Fully studied Units, DEPRECATED Units, DEPRECATED Lessons, empty
Units, and orphaned study identities all fall out of the walk rather than needing rules of
their own: retired content is filtered as it is reached, a finished Unit simply contains no
remaining unstudied Lesson, and an identity naming a Lesson that no longer resolves is
never encountered.

It lives in `lesson_study`, beside `StudyProgressDerivation`, rather than in
`guided_learning` beside `ContinueStudyingResolver`. Nothing in `lesson_study` can reach a
`TestAttempt`, so "Continue Learning never reads assessment history" is a property of where
the code sits rather than a rule a future change has to remember. The two features stay
separately named and separately derived, exactly as the table above requires.

There is deliberately no loading or failure case in `ContinueLearningOutcome`. The policy
answers only when both inputs are known; a caller that cannot read one of them has no
outcome, and manufacturing one — an empty studied set, an empty curriculum — is the
fabrication [failure semantics](#failure-semantics) forbids.

### Where it appears

Continue Learning is a third card on the Topic Browser, beside Recommended Next and
Continue Studying, and it is optional enrichment on `TopicBrowserUiState.Content` in
exactly the way those two are. `TopicBrowserViewModel` derives it on every render from two
inputs it already holds:

```text
LearningContentRepository.getActiveUnits()  ->  activeLearningUnits
StudyProgressStateHolder.state              ->  studiedLessonIds
                                            ->  ContinueLearningPolicy.resolve
```

The study half is the app-scoped `StudyProgressStateHolder` the three Learn destinations
already observe, not a read of its own. That is what makes a Lesson marked in the reader
move the card here without this screen being rebuilt — the Topic Browser is usually still
alive underneath Topic Detail, the Unit overview, and the reader — and it is also what
stops Continue Learning from disagreeing with the studied indicators shown on those
screens. The content half rides along with the availability read the browser already
performed, published in the same step, so the markers and the card describe one read of one
document. Nothing about the answer is cached: the walk is a pass over a list already in
memory, and a cached next Lesson would be one more place study state could go stale.

Four deliberate presentation decisions, recorded here so they are choices rather than
omissions:

| Decision | Choice | Why |
| --- | --- | --- |
| Placement | Third card, below Continue Studying and above Saved Questions | The two assessment-derived cards already state a priority between themselves; inserting a card derived from different inputs into that pair would restate it as a three-way ranking nobody decided |
| Search visibility | Withheld while a query is active | The same rule the other two guided cards follow: a learner who has started typing has said what they are looking for, and none of the three is a search result |
| `Complete` | A card with no click action, on a neutral surface rather than an accent container | "You have finished" is worth stating, and a state with nowhere to go must not look like a state with somewhere to go. Compose gives a `Card` without `onClick` no click semantics, so it is announced as content, not as a button |
| `Empty` | No card at all | A learner with nothing to read is not helped by a card telling them so, and the catalogue rows already carry the per-Topic availability marker |

Navigation reuses the existing Lesson destination. `ContinueLearningTarget.toAppRoute()`
maps to `AppRoute.LearningLesson(unitId, lessonId)` — the same route the Unit overview
pushes, carrying the same two stable IDs — and the shell pushes it once rather than
reconstructing a Unit-then-Lesson stack, so Back returns to the screen the shortcut was
tapped on. That mirrors Continue Studying, which likewise pushes one Topic route. No prose
travels: the Lesson is resolved from current content on arrival, and the card's own labels
are read from the same Units the policy walked, so a re-authored Lesson reads correctly
with nothing stored and nothing migrated.

The card is enrichment in the strict sense the [failure semantics](#failure-semantics)
below require. An unreadable study record, an unreadable learning document, or a study
record that is merely still arriving each leave it absent and cost the learner nothing
else: Topic rows, search, Topic access, practice, and both assessment-derived guided
surfaces are exactly as they were. `retry()` re-reads the study record along with the
catalogue, so a card lost to a transient failure comes back without a restart.

### How Continue Learning is verified

| Layer | What it establishes |
| --- | --- |
| `ContinueLearningPolicyTest` | The whole decision: no progress, partial progress, an earlier gap beating a later mark, skipped full Units, global authored order across Topics, deprecated Units and Lessons, orphaned identities, empty Units, Complete versus Empty, determinism |
| `BundledLearningContentRepositoryTest` | That `getActiveUnits()` really returns global authored order across interleaved Topics and excludes DEPRECATED Units |
| `TopicBrowserViewModelTest` | That the screen composes the two inputs correctly: titles from current content, refresh after mark and unmark while the browser stays alive, unknown and unreadable inputs withholding the card, search suppression, and coexistence with an unchanged Continue Studying |
| `TopicBrowserScreenTest` | Rendering and callbacks for an already-derived model: naming, the single click target, `Complete` having no click action, absence from search, and the three guided cards coexisting |
| `AppNavigationTest` | That the target reaches the existing Lesson route and can never reach an assessment |

Policy cases are proved once, in the policy tests, and are not re-proved through Compose.

## Failure semantics

Study state is optional learner-owned enrichment layered over readable publisher content.
Failing to read it must never make valid learning content unreadable.

Two conflations are forbidden:

```text
cannot read study state  !=  nothing studied
failed write             !=  successful mark or unmark
```

An unavailable study state is its own outcome, distinct from an empty studied set — the
same distinction `TopicLearningUnitsUiState` already draws between `Available(emptyList())`
and `Unavailable`, and the same one Topic coverage draws between "never attempted" and
"attempted poorly". Rendering unavailable as "not studied" tells the learner their record
is gone; rendering a failed write as success tells them a claim was saved that was not.

A study-state failure may legitimately cost the learner the studied indicator, the
progress aggregate, the ability to mark or unmark, and the Continue Learning shortcut. It
must not cost them Topic browsing, Unit content, Lesson reading, or any existing practice
capability. E22-04 designs the state hierarchy; this contract fixes what that design has
to preserve.

Content failure and study failure are also different failures. "The learning document
could not be read" is a publisher-content problem and already has its own handling; "study
records could not be read" says nothing about the authored content and must not invalidate
it. Continue Learning needs both inputs and so may be unavailable if either is missing,
but a study-record failure alone leaves the authored curriculum entirely readable.

## Presenting study progress

E22-04 makes the state above visible and changeable on the three Learn destinations that
already exist — the Lesson reader, the Learning Unit overview, and Topic Detail — without
adding a fourth screen, a Room query, or a second progress model.

### One app-scoped projection, not three caches

Navigation 3 keeps the Learn back stack alive, so the realistic flow is

```text
Topic Detail -> Learning Unit -> Lesson -> mark studied -> Back -> Back
```

with both parents still constructed and still observing their own state. Three ViewModels
each reading `studied_lesson` for themselves would mean going back through two screens that
still show the value from before the mark, and "Back probably rebuilds the ViewModel" is
not an invalidation strategy.

`StudyProgressStateHolder` is therefore a single app-scoped projection, built on
`AppCoroutineScope` and following the `SavedQuestionStateHolder` precedent exactly, which is
the established pattern here for one learner-owned truth shown on several live surfaces. It
is registered in `topicStudyPresentationModule`, because it is shared presentation state;
`lessonStudyDataModule` continues to own only the Room-backed repository, and
`learningContentModule` continues to own publisher content alone. All three Learn
ViewModels take it, and `SharedHostStartupTest` pins that exactly one instance resolves.

Its state is `StudyProgressState`:

```kotlin
sealed interface StudyProgressState {
    data object Loading
    data class Loaded(studiedLessons: List<StudiedLesson>, pendingLessonIds: Set<String>) {
        val studiedLessonIds: Set<String>
    }
    data object Error
}
```

The database stays authoritative. Nothing is stored in the holder that persistence does not
already hold, and nothing becomes visible that was not read back from it:

| Event | Result |
| --- | --- |
| `refresh()` succeeds | `Loaded` with what the repository returned — an empty list included |
| First read fails | `Error`, never `Loaded(empty)` |
| A later read fails | The previous `Loaded` stands; a transient failure must not repaint the Learn stack as unstudied |
| Concurrent `refresh()` | A `Mutex.tryLock` collapses them into one query |

Every Learn ViewModel calls `refresh()` when it is constructed and again on `retry()`, which
is what lets a surface recover from an earlier failed read instead of losing the mark
control for the rest of the session.

### Mutation is persist-then-read-back

`toggleStudied(lessonId)` is the single mutation path, and it is ignored while study state
is unknown — a toggle needs a persisted value to reverse — and while that Lesson already has
a write in flight. The Lesson goes into `pendingLessonIds`, the repository is written, the
repository is read again, and only that read becomes visible. Nothing is flipped
optimistically, so a control can never display a state that was not persisted.

| Outcome | What the learner sees |
| --- | --- |
| Write and read-back succeed | The read-back value, and pending cleared |
| Write fails | The previous persisted value, and pending cleared |
| Write succeeds, read-back fails | The previous persisted value, and pending cleared |

The last row is a deliberate choice rather than an oversight. The write may well have
landed, but this process did not observe it, and the contract is that visible state comes
from a read. Inventing the value the write "should" have produced is exactly the
fabrication the model forbids, so the holder keeps the last state it actually read and the
next `refresh()` — the next time any Learn destination opens — discovers the truth. A test
documents both halves of that behaviour.

`pendingLessonIds` is per Lesson, so a write on one Lesson disables that Lesson's control
and nothing else.

### One snapshot, all derived figures

The holder performs the single canonical read; presentation then derives from the identities
it publishes, using the pure `StudyProgressDerivation` directly:

```text
StudyProgressStateHolder.state -> Loaded.studiedLessonIds
  + the ViewModel's current LearningUnit(s)
  -> StudyProgressDerivation.deriveUnit / deriveTopic
```

`StudyProgressService` is deliberately not on this path and remains unregistered. Its
methods read `LessonStudyRepository` themselves, so calling it on every holder emission
would query Room again for every screen and every mark — several independent samples of one
fact, which is how a Unit and its Topic come to disagree. The service stays as the
single-IO boundary for a caller that has no shared projection, which E22-05 may well be; it
was not redesigned or deleted for this issue.

The Unit overview and Topic Detail therefore retain the domain objects they need:
`LearningUnitViewModel` keeps the resolved `LearningUnit`, and `TopicDetailViewModel` keeps
the ACTIVE `List<LearningUnit>` from `getActiveUnitsByTopic`. The row models
(`LearningLessonItemUiModel`, `LearningUnitItemUiModel`) are deliberately not given Lesson
bodies to make the arithmetic possible — a discovery row must not carry the authored
document. A study-state change re-derives from the retained content, so publisher content
is not re-read when only learner state moved.

### Loading, Available, Unavailable

Every Learn surface carries study state as `StudyProgressUiState<T>`:

```kotlin
sealed interface StudyProgressUiState<out T> {
    data object Loading
    data class Available<T>(val value: T)
    data object Unavailable
}
```

`StudyProgressState.toUiState { … }` is the one place `Error` becomes `Unavailable`, so no
destination can decide for itself that an unreadable record means an empty one. The wrapper
is narrow on purpose: it models study state on three screens and is not a general async
result type for the application.

The nesting matters as much as the three cases. Study state lives *inside* the content
state of each screen, never beside it:

| Screen state | Means |
| --- | --- |
| `LearningLessonUiState.Error` | The Lesson document could not be read |
| `LearningLessonUiState.Content(studyState = Unavailable)` | The Lesson reads fine; the learner's record could not be read |
| `TopicLearningUnitsUiState.Unavailable` | The Topic's authored Units could not be read |
| `Content(learningUnits = Available, studyProgress = Unavailable)` | The Units are readable; the study record is not |

A study-state failure can never produce a screen-level `Error`, can never turn
`TopicLearningUnitsUiState.Available` into `Unavailable`, and can never affect practice: no
practice action anywhere reads `StudyProgressState`.

### Lesson reader

`LearningLessonUiState.Content` gains
`studyState: StudyProgressUiState<LessonStudyUiModel>`, where `LessonStudyUiModel` is
`isStudied` plus `isPending` for this Lesson alone.

**State at the top, action at the end.** The reader reports whether the Lesson is studied
directly under the title — so the learner can see it without reading to the end — and the
control that *changes* it lives after the Sources, behind an explicit end-of-lesson prompt.
The two used to sit together under the summary as a badge beside an equally sized
`OutlinedButton`, which meant the third thing a learner met on an unread Lesson was an
invitation to declare they had read it. Completion is now something that happens when the
material is genuinely behind them.

The two states are also deliberately asymmetrical. An unstudied Lesson ends on "You've
reached the end of this lesson" and one filled `Button`; a studied one ends on a statement
with a `TextButton` under it, because "Studied" and "Mark as not studied" drawn as two pill
controls of equal weight made undoing look like half of what the feature was for.

| Study state | Top of page | End of page |
| --- | --- | --- |
| `Loading` | Nothing. A badge before the record is read would be a guess | Nothing |
| `Available(isStudied = false)` | Badge "In progress" | Prompt, and a filled "Complete lesson" |
| `Available(isStudied = true)` | Badge "Studied" with a tick | "Studied", and a text "Mark as not studied" |
| `Available(isPending = true)` | The persisted badge unchanged | The control disabled |
| `Unavailable` | "Study progress unavailable" — never "Not studied", and no control | Nothing |

"In progress" rather than "Not studied": the reader is by definition in the middle of it,
and the negative phrasing described the record rather than the learner.

Accessibility is carried by words in two channels: the button's visible label is the action
it performs, which is what Material's own semantics announce, and the current value is
published as the button's `stateDescription`. No tick, colour, or icon is load-bearing, and
no redundant content description is added on top of what Material already exposes.

**Where the Lesson sits.** `Content` also carries `placement: LessonPlacementUiModel?` — the
Unit's title and the Lesson's 1-based position among that Unit's ACTIVE Lessons. The toolbar
renders the Unit title as its persistent primary line and `Lesson X of Y` as a quieter second
line, including for a one-Lesson Unit. Meaningful downward reading collapses only the position;
meaningful upward reading or returning to the top restores it. The Topic → Unit → Lesson
hierarchy the learner navigated down was otherwise invisible once they arrived. The counts come
from the same ACTIVE list previous/next steps through, so a retired Lesson is neither a waypoint
nor a denominator.

**One primary continuation.** The end of the page ranks its two ways onward rather than
offering both at equal weight. With a successor, the next-Lesson card takes the primary
container and "Practice this unit" steps down to an `OutlinedButton`; on the last Lesson of a
Unit there is nothing left to read, so practising takes the filled button instead. Previous
keeps the quiet container it always had. Both controls emit exactly what they emitted before
the ranking existed.

`onToggleStudied` is the only thing on the page that changes study state. Opening the
Lesson, scrolling it, reaching the bottom, pressing Previous or Next, opening a Source, and
pressing "Practice this unit" all leave the record untouched — asserted at the ViewModel
level against the repository, not only through screen callbacks. The reading-progress meter
under the top bar remains a measure of scroll position through the document and is
unrelated: it is not labelled study progress, does not drive the control, and keeps its
existing non-focusable semantics.

### Learning Unit overview

`LearningUnitUiState.Content` gains
`studyProgress: StudyProgressUiState<LearningUnitStudyProgress>` — the E22-03 result
verbatim, not a second model. The per-Lesson list is joined to the rows by stable Lesson ID.

The aggregate sits under the Unit's summary and above "Practice this unit", so the page's
pedagogical order is unchanged. It renders as the written figure — "1 of 3 lessons studied"
— with a `ProgressMeter` beneath it as a second channel. The ratio is computed at the moment
of drawing from the derived counts; no percentage is added to a domain model or persisted,
and no second progress-bar component was introduced.

`StudyProgressSummary.Empty` renders nothing at all: `0 / 0` reads as complete and 0% claims
outstanding work that does not exist, and the overview's existing "No lessons are currently
available in this unit." is the honest explanation. `Loading` renders nothing either.
`Unavailable` renders "Study progress unavailable" in place of the aggregate.

Each ACTIVE Lesson row states "Studied" or "Not studied" in words when study state is
`Available`, and says nothing in the other two cases — so a row with no marker cannot be
mistaken for one that is merely unstudied. Study state is an annotation and never a
navigation rule: rows keep authored order, unstudied Lessons are not hidden, moved, locked,
or made unclickable, and completion is described as studied rather than mastered.

### Topic Detail

`TopicDetailUiState.Content` gains
`studyProgress: StudyProgressUiState<TopicStudyProgress>` as a fourth independently failing
input, beside the curriculum, the authored Units, and the assessment-derived
`learningContext`. Study progress is not folded into `LearningContextUiModel`: studied
Lessons are a claim about reading and coverage is measured from attempts, and a screen that
merged them could manufacture either from the other.

Per-Unit results are joined to the Unit cards by stable Unit ID. The card's last line is one
line either way — the learner's own progress ("1 of 3 lessons studied") when a record has
been read, and the authored count ("3 lessons") while it is loading, unreadable, or
describes a Unit with no current Lessons. Showing both would state the total twice.

An unreadable record shows one concise message under the Study heading rather than repeating
itself inside every card. Units stay listed, stay in authored order, and stay clickable;
`Start Practice`, Subtopic practice, and the targeted shortcuts are untouched in all three
study states.

No Topic-level study aggregate card was added. The issue asks for progress for a Topic's
Learning Units, Topic Detail already carries an assessment summary card, a coverage meter,
and a Subtopic list, and a second large progress figure would compete with the first while
answering a question the per-Unit lines already answer.

### Topic Browser: deliberately unchanged

**E22-04 does not add aggregate study progress to the Topic Browser.**

The Topic Browser is the catalogue and guidance surface: it already carries search,
Topic and Subtopic discovery, the learning-availability marker, Continue Studying,
Recommended Next, Saved Questions, and assessment-derived learning context. Another
per-Topic figure there would add density without making study state actionable, since the
learner still has to open the Topic to do anything about it. Topic Detail is where
Unit-specific current progress belongs, and E22-05's Continue Learning is the actionable
catalogue-level affordance — a place to go next, rather than one more number. The decision
is recorded here so it is deliberate rather than an omission.

E22-05 has since added that card, and the decision stands: the browser gained one
actionable shortcut into the next Lesson, and still carries no per-Topic or aggregate study
figure. See [Where it appears](#where-it-appears).

## Non-goals and future boundaries

E22-01 introduces no Kotlin type. Prose specifies this contract completely, and every
candidate type — a studied fact, a Unit progress result, a Continue Learning outcome —
would be a decision E22-02, E22-03, or E22-05 is better placed to make once it knows the
shape of its own layer. A speculative type here would have to be edited by the issue that
actually uses it, which is churn rather than a contract.

Also deliberately absent from this document, and owned elsewhere: the Room entity, DAO,
migration, schema version, repository implementation, and Koin binding, which E22-02 has
since added and [persistence](persistence.md) describes; Unit and Topic derivation code
(E22-03); the Learn presentation and the mark/unmark control, which E22-04 has since added
and [Presenting study progress](#presenting-study-progress) describes; the Continue
Learning policy, presentation, and navigation route, which E22-05 has since added and
[What E22-05 implemented](#what-e22-05-implemented) describes. Neither E22-04 nor E22-05
added a schema change: the database remains at version 8, and no aggregate, percentage,
completion flag, last-read Lesson, or resume flag is persisted.

Excluded from the epic entirely rather than deferred: content hashes or Lesson version
stamps, last-read or resume state, a study-history or orphan-record screen, additional
authored Learning Units, and any assessment question change.
