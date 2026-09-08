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
`LessonStudyRepository`, described in [persistence](persistence.md) — and left the
semantics below unchanged. Derivation, presentation, and Continue Learning remain
unimplemented, so this document still names no service or Compose control.

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
is already a function of content and study records. E22-05 may add the smallest additive
ordered read `LearningContentRepository` does not yet expose in order to walk global
authored Unit order.

Three outcomes are distinguishable, and the last two must not collapse into each other:

| Outcome | Meaning | Carries |
| --- | --- | --- |
| Next | At least one current ACTIVE Lesson is unstudied | Stable Unit ID and Lesson ID |
| Complete | Current ACTIVE Lessons exist and all are studied | Nothing to open |
| Empty | The curriculum contains no ACTIVE Lesson eligible for study | Nothing to open |

"You have studied everything" and "there is nothing here to study" are different
statements about the learner and about the content, and rendering one as the other would
either congratulate a learner who has done nothing or hide a finished course. Exact type
names belong to E22-05.

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

## Non-goals and future boundaries

E22-01 introduces no Kotlin type. Prose specifies this contract completely, and every
candidate type — a studied fact, a Unit progress result, a Continue Learning outcome —
would be a decision E22-02, E22-03, or E22-05 is better placed to make once it knows the
shape of its own layer. A speculative type here would have to be edited by the issue that
actually uses it, which is churn rather than a contract.

Also deliberately absent from this document, and owned elsewhere: the Room entity, DAO,
migration, schema version, repository implementation, and Koin binding, which E22-02 has
since added and [persistence](persistence.md) describes; Unit and Topic derivation code
(E22-03); ViewModel, Compose UI, or a mark/unmark control (E22-04); Continue Learning
resolver or navigation route (E22-05).

Excluded from the epic entirely rather than deferred: content hashes or Lesson version
stamps, last-read or resume state, a study-history or orphan-record screen, additional
authored Learning Units, and any assessment question change.
