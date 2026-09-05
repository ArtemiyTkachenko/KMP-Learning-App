# The Practice Builder

The screen that turns what the learner chose to practise into a runnable `AssessmentConfig.Focused`, and how that configuration is persisted. See [practice selection](practice-selection.md) for the selection policies it previews.

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

## What the builder is opened on

The builder receives a `PracticeBuilderTarget` — `Topic`, `Subtopic`, or `LearningUnit`
— rather than an `AssessmentScope`. The two were the same thing while every entry was a
Topic or a Subtopic, whose stable ID *is* the scope. A Learning Unit breaks that: what it
practises is the set of concepts its current Lessons are responsible for teaching, which
does not exist until the Unit has been read. Keeping the distinction in the type is what
prevents that derived set from being computed on the screen offering practice and carried
through the back stack, where it would quietly outlive the authoring it came from.

`PracticeTargetResolver` is the one place a target becomes a scope, and it is the only
crossing from learning content into assessment configuration in the app. It runs in one
direction: a Unit is read here and leaves as a plain set of Subtopic IDs, so selection,
the engine, persistence, and retake never learn that Learning Units exist. It lives in
presentation beside the builder rather than in the assessment domain, which is what keeps
that dependency edge from reversing.

Resolution answers one of three things. `Resolved(name, scope)` carries the scope and the
label to show. `Unavailable` means the target names nothing that is current study
material — a stale or deprecated Unit. `NoPracticeableConcepts` means the Unit resolves
and is current but teaches nothing assessable. The last two reach the screen as
`PracticeAvailability.TargetUnavailable` and `NoPracticeableConcepts`, which disable Start
and offer no Retry: both are settled answers about content, unlike a failed read, which
stays `Error` and re-resolves on Retry. A Topic or Subtopic target cannot produce either,
so those flows are unchanged — their scope is known from the ID, and the curriculum is
read only for a display name whose absence has never blocked practice.

A Learning Unit's scope is the deduplicated union of `primarySubtopicIds` across its
ACTIVE Lessons. Four authoring rules are enforced by that one derivation:

- **Deprecated Lessons contribute nothing**, even inside an ACTIVE Unit. Retired material
  must not keep quizzing the learner.
- **`supportingSubtopicIds` never enter practice.** A supporting concept exists so a
  Lesson can explain enough surrounding context to stand on its own, which is a different
  claim from being responsible for teaching it.
- **Cross-Topic primary concepts survive.** `LearningUnit.topicId` is the home Topic that
  decides where the Unit is browsed and never narrows what it may teach; the learning
  curriculum validator already owns structural validity.
- **A concept named by several Lessons contributes once.** The scope is a `Set`, so a
  shared primary concept cannot weight selection.

An empty derived set is refused before `AssessmentScope.Subtopics` is constructed. Its
non-empty requirement is a domain invariant, not a user-facing outcome, so a malformed
Unit reads as unavailable rather than failing a precondition in front of the learner.

`AppRoute.PracticeBuilderLearningUnit` carries the stable Unit ID and nothing else — no
title, no concepts, and no source field, since nothing produces a Learning-Unit practice
intent. Both study surfaces push it: the Unit overview and the Lesson reader each offer
"Practice this unit", and the reader's action uses the *owning* Unit from the route it is
being rendered in, so finishing a Lesson practises the whole Unit rather than that Lesson.
Both are ordinary pushes onto the Learn stack, so Back returns to the surface the learner
left. The reverse mapping does not exist: a derived multi-Subtopic scope cannot be turned
back into a builder route, because several Units can teach the same concepts and the whole
point of resolving on arrival is that the derivation is re-run against current content.

The configured run reaches assessment taking as typed route fields — scope ID,
count, levels, source — and never as Questions, answers, or curriculum text. Every
dimension travels because the destination rebuilds the config from the route, and
an absent dimension would silently become its default: practising every level when
the learner asked for one. The level list is normalised to authored order so an
identical setup is an identical back-stack entry. The scope's display name is
resolved from its stable ID on arrival rather than carried, so a renamed Topic
cannot appear under a label frozen into the back stack.

A Unit run travels as `AppRoute.FocusedSubtopicsPractice`, whose `subtopicIds` are the
derived concepts and not the Unit — by that point the run is an ordinary focused
assessment, and re-deriving it at the assessment would let a mid-run content change alter
what is asked. The IDs are sorted for the same reason the levels are normalised: an
identical configuration has to be an identical back-stack entry.

Learning Unit identity is therefore absent from `AssessmentConfig`, `AssessmentScope`, and
`TestAttempt`. That is deliberate rather than incidental: `TestAttempt.config` is the
authoritative record of what a learner was actually asked, so an attempt started when a
Unit taught `{A, B, C}` stays `{A, B, C}` after the Unit is re-authored to add `D`, and
retake repeats the run that happened rather than the Unit as it reads today. E21-06 added
no schema, column, migration, or persistence field — the multi-Subtopic scope E21-05 made
persistable is what carries it.

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
