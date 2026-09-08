# Compose Units 2–6 Authoring and Assessment Plan

## Purpose

`docs/content/compose-learning-blueprint.md` maps the whole Compose subject. This document
is narrower: it is the confirmed authoring plan for the five Units that epic E23 delivers —
blueprint Units 2–6, 18 Lessons — reviewed against the taxonomy, the shipped Unit 1, the
current ACTIVE question bank, and current authoritative sources.

It exists so that E23-02 through E23-06 can start authoring without re-deciding identity,
scope, or concept ownership, and so that E23-07 has a traceable list of assessment gaps.

It is **not** a second blueprint. Objectives, depth layers, and Teach/Bridge/Reference/
Exclude decisions stay in the blueprint; this document records the decisions the blueprint
left open and the review findings that would otherwise have to be re-derived. Where the
review found a blueprint defect, the blueprint was corrected and the correction is recorded
in [Blueprint changes](#blueprint-changes-made-by-this-review).

**Review date: 2026-09-08.** Everything in [Source freshness](#source-freshness-and-technical-assumptions)
was checked on that date against the versions this repository is configured with.

## Scope confirmation

The agreed scope holds: **five Units, 18 Lessons**, exactly as the blueprint plans them. The
review found no conflict that would justify splitting, merging, adding, or dropping a Lesson.
Four bounded corrections were needed and are recorded below; none of them changes the Unit or
Lesson count.

This issue is planning only. It introduces no production Lesson and no Question. Every
identity proposed here lives in documentation until the authoring issue that ships it.

## How the authoring issues use this document

| Issue | Reads |
| --- | --- |
| E23-02 | Unit 2 identities, objectives and boundaries; the Unit 2 rows of the semantic review; the saved-state and observation entries in source freshness |
| E23-03 | Unit 3 identities and boundaries; the recomposition rows of the semantic review; the phases entry in source freshness |
| E23-04 | Unit 4 identities and boundaries; the stability rows; the Strong Skipping and compiler-configuration entries in source freshness |
| E23-05 | Unit 5 identities and boundaries; the derived-state rows; the `derivedStateOf` entry in source freshness |
| E23-06 | Unit 6 identities and boundaries; the snapshot rows; the `snapshotFlow` entry in source freshness |
| E23-07 | [Assessment gaps](#assessment-gaps-for-e23-07) in full, re-checked against the finished Lessons |
| E23-08 | [Handoff](#handoff) — sequencing, cross-Unit links, and the limitations recorded here |

**Authoring status.** E23-02 shipped Unit 2 and E23-03 shipped Unit 3; both are ACTIVE in
`learning_curriculum.json`. What that authoring found is recorded in
[Authoring outcomes](#authoring-outcomes-for-units-2-and-3). Units 4–6 are still plans, so
every identity below remains a proposal until its issue ships it.

---

## Identity conventions and proposed identities

The shipped Unit establishes the convention, and this plan follows it rather than inventing
one:

- **Unit id** — `unit_` plus the Unit title in snake case (`Thinking in Compose` →
  `unit_thinking_in_compose`).
- **Lesson id** — `lesson_` plus the concept the Lesson owns, in snake case, short enough to
  read in a reference (`lesson_declarative_ui`, `lesson_composable_execution`,
  `lesson_state_down_events_up`). Lesson ids are **not** numbered: the blueprint's `L2.1`
  labels are positional and would be wrong the moment a Lesson moves.
- Ids are stable once shipped. Renaming one breaks learner study-progress records, which are
  keyed by Lesson id (see `docs/architecture/study-progress.md`).
- Lesson ids must be unique across **all** Units, not just within one: `relatedLessonIds`
  names a Lesson without naming its Unit, and `LearningCurriculumValidator` rejects a
  duplicate.

One deliberate departure: Unit 4's title in snake case
(`unit_identity_keys_stability_and_immutability`) is long enough to be unreadable in a
reference, so its id drops the last term. Immutability is still taught in the Unit — it is
the route into stability — and the Unit title is unchanged.

| Blueprint | Unit id | Unit title |
| --- | --- | --- |
| Unit 2 | `unit_state_and_state_ownership` | State and State Ownership |
| Unit 3 | `unit_recomposition` | Recomposition |
| Unit 4 | `unit_identity_keys_and_stability` | Identity, Keys, Stability and Immutability |
| Unit 5 | `unit_derived_state_and_expensive_work` | Derived State and Expensive Work |
| Unit 6 | `unit_snapshot_fundamentals` | Snapshot Fundamentals |

| Blueprint | Lesson id | Lesson title | Primary | Supporting |
| --- | --- | --- | --- | --- |
| L2.1 | `lesson_observable_state` | Observable State: `mutableStateOf` and `State<T>` | `compose_state` | `compose_snapshot_system`, `kotlin_delegation` |
| L2.2 | `lesson_remember_composition_memory` | `remember`: Composition Memory | `compose_state` | `compose_identity_keys`, `compose_recomposition` |
| L2.3 | `lesson_remember_saveable` | `rememberSaveable` and State That Must Survive | `compose_state` | `saved_state`, `configuration_changes`, `process_death` |
| L2.4 | `lesson_state_hoisting` | State Hoisting and the Lowest Sensible Owner | `compose_state_hoisting` | `compose_udf`, `state_ownership` |
| L2.5 | `lesson_observable_collections` | Collections and Observable Mutation | `compose_state` | `kotlin_collections`, `kotlin_data_classes`, `compose_stability` |
| L3.1 | `lesson_composition_and_recomposition` | Composition and Recomposition | `compose_recomposition` | `compose_fundamentals`, `compose_state` |
| L3.2 | `lesson_recomposition_scopes` | Recomposition Scopes and Selective Execution | `compose_recomposition` | `compose_snapshot_system`, `compose_state` |
| L3.3 | `lesson_recomposition_cost` | Recomposition Is Not the Problem | `compose_recomposition` | `compose_recomposition_performance` |
| L4.1 | `lesson_composable_identity` | Composable Identity | `compose_identity_keys` | `compose_recomposition`, `compose_state` |
| L4.2 | `lesson_keys_and_identity_in_lists` | `key` and Keys in Lazy Lists | `compose_identity_keys` | `compose_lazy_layouts` |
| L4.3 | `lesson_immutability_vs_stability` | Immutability in Kotlin vs. What Compose Needs | `compose_stability` | `kotlin_data_classes`, `kotlin_equality`, `kotlin_collections`, `kotlin_variables` |
| L4.4 | `lesson_stability_and_skipping` | Stability and Skipping | `compose_stability` | `compose_recomposition`, `compose_recomposition_performance` |
| L4.5 | `lesson_stability_annotations` | `@Stable` and `@Immutable` as Contracts | `compose_stability` | `kotlin_equality` |
| L5.1 | `lesson_remember_key_memoization` | `remember(key)` as Memoization | `compose_derived_state` | `compose_state`, `compose_recomposition` |
| L5.2 | `lesson_derived_state` | `derivedStateOf` | `compose_derived_state` | `compose_snapshot_system`, `compose_lazy_layouts` |
| L5.3 | `lesson_work_outside_composition` | Keeping Work Out of Composition | `compose_derived_state` | `main_thread_performance`, `layered_architecture`, `use_cases` |
| L6.1 | `lesson_snapshot_observation` | How Compose Observes State | `compose_snapshot_system` | `compose_state`, `compose_recomposition` |
| L6.2 | `lesson_snapshot_flow` | `snapshotFlow` and Crossing Into Flow | `compose_snapshot_system` | `flow_fundamentals`, `hot_vs_cold_streams`, `compose_side_effects` |

### Identity and mapping checks performed

- **Uniqueness.** None of the 5 proposed Unit ids and 18 proposed Lesson ids collides with
  the authored content (`unit_thinking_in_compose`; `lesson_declarative_ui`,
  `lesson_composable_execution`, `lesson_state_down_events_up`) or with each other. Checked
  against `learning_curriculum.json`, not from memory.
- **Mapping validity.** All 26 Topic-owned Subtopic ids used by blueprint Units 2–6 exist in
  `initial_curriculum.json` and are `ACTIVE`. Eleven are `android_ui`; the rest bridge to
  `kotlin_language`, `lifecycle_navigation`, `architecture`, `performance` and
  `async_reactive`, which Rule 3 of the authoring contract expects.
- **Taxonomy gaps.** Units 2–6 need no Subtopic that does not exist. The blueprint's recorded
  gaps (insets, animation, gestures, focus, and so on) all sit in Units 9–14. `kotlin_variables`,
  cited by L4.3, has no active question by deliberate policy — it is vocabulary, not practice
  coverage, and that is unchanged here.
- **Overlap.** No Lesson lists the same Subtopic as both primary and supporting, which the
  validator rejects. One cross-Lesson ownership overlap was found and corrected — see
  [Blueprint changes](#blueprint-changes-made-by-this-review).

---

## Lesson objectives, prerequisites and boundaries

Objectives here restate the blueprint's in one line and add what the review had to settle:
the reasoning a learner must be able to demonstrate, where each prerequisite is taught or
bridged, and the boundary that keeps the Lesson from absorbing its neighbour.

### Unit 2 — State and State Ownership (`unit_state_and_state_ownership`)

**Prerequisite:** Unit 1, which is published. No prerequisite of this Unit is unpublished.

#### `lesson_observable_state` (L2.1)

- **Objective:** distinguish ordinary mutation from mutation Compose can observe.
- **Demonstrable reasoning:** given a screen that does not update, decide whether anything
  observable was written, and explain why reading `.value` is what creates the dependency.
- **Prerequisites:** the execution contract from `lesson_composable_execution` (published);
  Kotlin property delegation, bridged in place to the depth `by` needs.
- **Boundary:** names the snapshot system in one sentence and defers the mechanism to
  `lesson_snapshot_observation`. Does not teach `remember` — a value can be observable and
  still be recreated on every execution, which is the next Lesson's point.

#### `lesson_remember_composition_memory` (L2.2)

- **Objective:** separate "this value is observable" from "this value survives recomposition".
- **Demonstrable reasoning:** predict the behaviour of all four combinations of `remember` and
  `mutableStateOf`, and explain why `remember { mutableStateOf(x) }` is the common pair.
- **Prerequisites:** L2.1. Recomposition appears only as "the body can run again", already
  established in Unit 1.
- **Boundary:** `remember(key)` is taught conceptually as invalidation, not as a performance
  tool; memoizing expensive work is `lesson_remember_key_memoization`. Composition identity is
  named as the reason placement matters and is owned by `lesson_composable_identity`.

#### `lesson_remember_saveable` (L2.3)

- **Objective:** decide which state must survive what, and pick the mechanism accordingly.
- **Demonstrable reasoning:** place a given piece of state on the recomposition /
  configuration-change / process-death ladder and justify the choice, including why "save
  everything" is wrong.
- **Prerequisites:** L2.2; the Android lifecycle concepts `configuration_changes`,
  `process_death` and `saved_state`, all bridged in place — no learning Unit teaches them yet
  and this Lesson must not reproduce that curriculum.
- **Boundary:** saved state is small UI state that crosses a Binder transaction, not durable
  storage; `ViewModel` and `SavedStateHandle` are named as complements, not alternatives, and
  their treatment stays with the future ViewModel Unit. Platform caveat is mandatory — see
  [saved state](#saved-state-guarantees-and-platform-differences).

#### `lesson_state_hoisting` (L2.4)

- **Objective:** decide where a given piece of state should live.
- **Demonstrable reasoning:** name the lowest sensible owner for a piece of state by asking
  who reads it, who writes it, and how long it must live, and explain the cost of hoisting too
  far as well as too little.
- **Prerequisites:** unidirectional data flow, taught by the published
  `lesson_state_down_events_up`; general state ownership, bridged to the `architecture` Topic.
- **Boundary:** teaches the mechanics and the ownership decision. The direction of data flow is
  Unit 1's contract and appears here only as supporting context; `ViewModel`-owned screen state
  is named and deferred.

#### `lesson_observable_collections` (L2.5)

- **Objective:** explain why ordinary mutable collections break in Compose and what to use.
- **Demonstrable reasoning:** given a list mutated in place with no UI update, identify that
  nothing observable was written, and choose between replacement and a snapshot-aware
  collection.
- **Prerequisites:** L2.1 and L2.2; Kotlin collection and `data class` semantics, bridged.
- **Boundary:** ends at "a read-only `List` guarantees nothing about the underlying object",
  which is the hand-off into `lesson_immutability_vs_stability`. Stability and skipping are
  named, not taught.

### Unit 3 — Recomposition (`unit_recomposition`)

**Prerequisite:** Units 1–2. Unit 2 ships first in E23-02.

#### `lesson_composition_and_recomposition` (L3.1)

- **Objective:** distinguish initial composition from recomposition and name what triggers each.
- **Demonstrable reasoning:** trace a button click through state write → invalidation →
  re-execution, and say what did *not* re-execute and why.
- **Prerequisites:** Unit 1's execution contract; Unit 2's observable state.
- **Boundary:** names composition, layout and drawing as separate phases so that "recomposition
  redraws the screen" can be refuted, and stops there — per-phase state reads and deferred reads
  are Unit 13. Recomposition scopes are the next Lesson.

#### `lesson_recomposition_scopes` (L3.2)

- **Objective:** predict which composables re-execute for a given state change.
- **Demonstrable reasoning:** given two versions of a tree that read the same state at
  different levels, say which scopes are invalidated, and explain why the read location rather
  than the write location defines the scope.
- **Prerequisites:** L3.1.
- **Boundary:** skipping is named as "an unchanged input can let a scope be skipped" and
  mechanised in `lesson_stability_and_skipping`. Slot-table internals are excluded. Deferred
  reads as a performance technique stay in Unit 13.

#### `lesson_recomposition_cost` (L3.3)

- **Objective:** replace "avoid recomposition" with "avoid unnecessary expensive work".
- **Demonstrable reasoning:** judge whether an observed recomposition is a problem, naming what
  the executing body actually costs rather than counting executions.
- **Prerequisites:** L3.1 and L3.2.
- **Boundary:** deliberately short. It names misconceptions and the real costs; measurement,
  tooling and optimisation belong to Unit 13, and the fixes belong to Units 4 and 5. It must not
  become a performance lesson, and it must not present a recomposition count as evidence of a
  defect on its own.

### Unit 4 — Identity, Keys, Stability and Immutability (`unit_identity_keys_and_stability`)

**Prerequisite:** Units 1–3.

#### `lesson_composable_identity` (L4.1)

- **Objective:** explain what gives a composable, and its remembered state, an identity.
- **Demonstrable reasoning:** explain why state resets when a composable moves between branches
  of an `if`, and why two calls to the same composable hold independent `remember` slots.
- **Prerequisites:** `remember` (L2.2); recomposition (Unit 3).
- **Boundary:** identity from the call site and position, in ordinary composition. Lists are the
  next Lesson; the slot table itself is excluded.

#### `lesson_keys_and_identity_in_lists` (L4.2)

- **Objective:** give a composable a stable logical identity when position is not a reliable one.
- **Demonstrable reasoning:** predict what a user sees when a keyless list is reordered, and
  explain why an index key fails exactly when it matters.
- **Prerequisites:** L4.1.
- **Boundary:** `key` as an identity mechanism, with its limits. `LazyColumn` itself, item reuse
  and `contentType` are Unit 10; this Lesson uses a lazy list as the setting, not the subject.

#### `lesson_immutability_vs_stability` (L4.3)

- **Objective:** stop equating `val` with immutability.
- **Demonstrable reasoning:** given a "immutable" UI state whose nested list is mutated in place,
  explain why no update happens and why `equals` correctness is a precondition for anything the
  runtime infers from "unchanged".
- **Prerequisites:** L2.5; Kotlin equality and `data class` semantics, bridged.
- **Boundary:** Kotlin semantics only as far as the Compose consequence needs. What the compiler
  does with that information is the next Lesson.

#### `lesson_stability_and_skipping` (L4.4)

- **Objective:** explain when Compose can skip a composable and what "stable" means.
- **Demonstrable reasoning:** decide whether a given call is skipped under this repository's
  compiler configuration, and explain why a freshly built but equal list still fails the check.
- **Prerequisites:** L4.3; recomposition scopes (L3.2).
- **Boundary:** must explicitly correct the pre–Strong Skipping claim that an unstable parameter
  always forces re-execution, and must state the Strong Skipping assumption wherever an example
  depends on it. Compiler metrics and reports are Reference material in Unit 13.

#### `lesson_stability_annotations` (L4.5)

- **Objective:** treat `@Stable` and `@Immutable` as promises the author must keep.
- **Demonstrable reasoning:** judge whether an annotation is warranted, and explain why a false
  promise surfaces as a stale UI rather than as a slow one.
- **Prerequisites:** L4.4.
- **Boundary:** contracts and their correctness consequences. Not an optimisation checklist; the
  stability configuration file and kotlinx immutable collections are named at most once as
  further reading.

### Unit 5 — Derived State and Expensive Work (`unit_derived_state_and_expensive_work`)

**Prerequisite:** Units 2–4.

#### `lesson_remember_key_memoization` (L5.1)

- **Objective:** cache a computed value correctly across recompositions.
- **Demonstrable reasoning:** choose a key list for a calculation and explain what
  under-specifying it (stale results) and over-specifying it (no cache) each cost.
- **Prerequisites:** L2.2, which already introduced `remember(key)` as invalidation.
- **Boundary:** the cache and its invalidation. Filtering the *result* of a calculation is the
  next Lesson.

#### `lesson_derived_state` (L5.2)

- **Objective:** know the one situation `derivedStateOf` is actually for.
- **Demonstrable reasoning:** given inputs that change more often than the result, explain what
  `derivedStateOf` removes, and recognise the cases where it adds nothing but overhead.
- **Prerequisites:** L5.1; recomposition scopes (L3.2).
- **Boundary:** must not present it as a wrapper for every state-dependent expression, and must
  compare it against direct calculation and `remember(key)` rather than describing it alone.

#### `lesson_work_outside_composition` (L5.3)

- **Objective:** put computation where it belongs instead of optimising it in place.
- **Demonstrable reasoning:** decide whether the fix is "make this composable cheaper" or "this
  never belonged in composition", and name the layer that should own the work.
- **Prerequisites:** Unit 1's execution contract; L3.3.
- **Boundary:** the layering argument is stated and bridged to the `architecture` Topic.
  Coroutine execution and effect selection are Unit 7, named only.

### Unit 6 — Snapshot Fundamentals (`unit_snapshot_fundamentals`)

**Prerequisite:** Units 2, 3 and 5.

#### `lesson_snapshot_observation` (L6.1)

- **Objective:** explain why `mutableStateOf` triggers UI updates and an ordinary object does not.
- **Demonstrable reasoning:** re-explain the whole of Unit 2 in one consistent model, and answer
  "why didn't my UI update?" with "nothing observable was written".
- **Prerequisites:** Units 2, 3 and 5 — this Lesson unifies them rather than introducing new
  behaviour.
- **Boundary:** mental-model depth only. Snapshot MVCC, apply and merge internals are excluded;
  the consistency property is stated as an observable guarantee, not implemented.

#### `lesson_snapshot_flow` (L6.2)

- **Objective:** convert observable Compose state into a stream for non-UI consumers.
- **Demonstrable reasoning:** decide when crossing into `Flow` is the right move, and state what
  conflation means for the values a collector sees.
- **Prerequisites:** L6.1; cold-stream semantics, bridged in one paragraph — the Flow curriculum
  is **not authored**, so the pointer must name the subject and cite Kotlin's documentation
  rather than promise an in-app Lesson.
- **Boundary:** must not present snapshot observation as a durable event log or as a guarantee
  that every intermediate value is delivered. Collection inside `LaunchedEffect` is shown as the
  usual collection site and attributed to the future Effects Unit, which E23 does not author.

---

## Semantic assessment review

Every ACTIVE Question on the eight primary Subtopics of Units 2–6 was read in full — stem,
options, correct set, explanation and Sources — not counted. There are nine, deduplicated where
two Lessons share a Subtopic. Four DEPRECATED Questions on the same Subtopics were read as
context, because a deprecated Question still occupies its concept.

**Nothing below claims that an unpublished Lesson makes a Question answerable.** "Planned"
means this plan places the required reasoning in a named Lesson; whether the finished prose
delivers it is a judgement E23-02…E23-06 make at authoring time and E23-07 re-checks.

| Question | Level | Subtopic | Lesson(s) | Reasoning the Question requires | Where that reasoning lives | Finding | Action |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `remember_vs_remember_saveable` | Foundation | `compose_state` | `lesson_remember_saveable` | That both retain across recomposition, but only `rememberSaveable` participates in saved-state restoration, and that it is not durable storage | Planned — L2.3 Core is exactly this ladder | Sound. Stem is Android-framed ("rotation"); accurate for Android and the Lesson must add the platform caveat rather than the Question | Retain |
| `compose_state_hoisting_001` | Foundation | `compose_state_hoisting` | `lesson_state_hoisting` | That hoisting moves state to an appropriate owner and passes value down / events up, and that it is not "put everything in a ViewModel" | Planned — L2.4 Core and Senior | Sound and well matched to the Lesson's contract | Retain |
| `compose_udf_event_direction` | Applied | `compose_udf` | `lesson_state_down_events_up` (published) | That a child reports intent through a callback instead of holding a second write path | Taught — Unit 1 ships this | Ownership defect found: the blueprint also made `compose_udf` primary in L2.4, which would have made this Unit 2 practice for a concept Unit 1 owns | Corrected in the blueprint; no Question change |
| `composition_vs_recomposition` | Foundation | `compose_recomposition` | `lesson_composition_and_recomposition` | That initial composition records emitted UI and recomposition updates only affected scopes — and that composition is not layout and drawing | Planned — L3.1, provided the three phases are named | The distractor "remeasures and redraws on every state change" cannot be eliminated without the phase distinction, which the blueprint's L3.1 Core did not mention | Blueprint Note added; revisit during E23-03 authoring |
| `compose_state_read_recomposition_scope` | Foundation | `compose_recomposition` | `lesson_recomposition_scopes` | That reads are recorded by the executing scope, and that moving a read into a smaller child narrows what is invalidated | Planned — L3.2 Core and Practical | Sound; the closest match in the bank to a Lesson's contract | Retain |
| `compose_key_identity_lazy_state` | Applied | `compose_identity_keys` | `lesson_keys_and_identity_in_lists`, `lesson_composable_identity` | That identity is positional without a key, so remembered state stays with the slot, and that Compose never infers identity from contents | Planned — L4.2, with L4.1 supplying the identity model | Sound. Shared by two Lessons; counted once. Its framing is entirely list-based, which leaves L4.1's own reasoning unassessed | Retain; see GAP-U4-A |
| `compose_strong_skipping_instance_equality` | Foundation | `compose_stability` | `lesson_stability_and_skipping` | That Strong Skipping makes restartable composables skippable but compares unstable arguments by instance, so a fresh equal list still re-runs | Planned — L4.4 Practical | Verified correct against the Strong Skipping documentation on 2026-09-08, including the Kotlin 2.0.20 default. Level looks low: identifying the comparison rule is compiler-behaviour reasoning, not recall | Retain; level review is a candidate for E23-07, not a defect |
| `compose_derived_state_threshold` | Applied | `compose_derived_state` | `lesson_derived_state` | That `derivedStateOf` updates consumers only when the derived result changes, and that it is neither a threading nor a persistence API | Planned — L5.2 Core and Practical, using the same scroll-threshold case | Sound. Because the Lesson uses the canonical example the Question uses, E23-05 must teach the decision rule and the cases where it adds nothing, not the example | Retain; revisit during authoring |
| `compose_snapshot_flow_state` | Foundation | `compose_snapshot_system` | `lesson_snapshot_flow` | Recognising `snapshotFlow` as the API that turns `State` into a cold `Flow`, against `rememberSaveable`, `produceState` and `CompositionLocal` | Planned — L6.2 Core | Sound but recall-shaped: it identifies an API rather than exercising reasoning about observation or conflation. It is also the only active Question on this Subtopic, so Unit 6 practice currently rests on it alone | Retain; add coverage in E23-07 — see GAP-U6-A |

### Deprecated Questions read as context

`compose_state_001` (declarative state), `compose_recomposition_001` (what triggers
recomposition), `compose_stability_001` and `compose_skipping_stable_parameter_contract` (both
skipping-contract questions) are DEPRECATED. Two consequences for E23-07: the concept space
around stability is more crowded than the single active Question suggests, and new Questions
must not re-ask what a deprecated Question already asked in a different wording.

### Coverage-report staleness found during the review

`docs/content/question-bank-coverage.md` predates two deprecations and is wrong where this
review reads it: it reports 360 ACTIVE / 39 DEPRECATED where the bank now holds 358 / 41, and
it still lists `compose_stability_001` as active, which inflates `compose_stability` to two
active Questions and `android_ui` to 26. `flow_testing_hot_flow_never_completes` is the other
missing deprecation. Every count in this document was taken from
`initial_curriculum.json` directly, not from that report. Regenerating it is E23-07's job —
that issue already requires the affected coverage reports to be current — and it was
deliberately not regenerated here, where no Question changed.
`docs/content/learning-question-coverage.md` is current and was verified with `--check`.

---

## Authoring outcomes for Units 2 and 3

Added by E23-02 and E23-03 after the Lessons were written, so E23-04 through E23-08 re-check
findings rather than re-deriving them.

### What did not change

Every proposed Unit id, Lesson id, title, authored order, and primary/supporting mapping for
Units 2 and 3 in the [identity tables](#identity-conventions-and-proposed-identities) shipped
verbatim. No Lesson boundary moved, no Lesson split or merged, and no blueprint correction
was needed beyond the four this review had already made. The eight Lessons carry Core and
Practical depth throughout; `lesson_recomposition_cost` deliberately carries no Senior
section, because the plan asked for a short Lesson and manufacturing deeper material would
have pulled Unit 13's content forward.

### Semantic re-check of the two recomposition Questions

Both were re-read against the finished prose rather than against the plan's expectation.

| Question | Reasoning it requires | Does the finished Lesson teach it | Evidence |
| --- | --- | --- | --- |
| `composition_vs_recomposition` | Initial composition records emitted UI; recomposition updates only affected parts; **and** composition is not layout and drawing | **Yes**, and the phase distinction the plan flagged as missing is now explicit | `lesson_composition_and_recomposition` Core names the three phases in a table, states that recomposition re-runs the first only, and refutes the exact distractor wording in a `COMMON_MISTAKE` callout; its `INTERVIEW_FOCUS` restates the phase separation |
| `compose_state_read_recomposition_scope` | Reads are recorded by the executing scope; moving a read into a smaller child narrows invalidation; unrelated scopes are not invalidated | **Yes** | `lesson_recomposition_scopes` Core builds the whole Lesson on two versions of one screen that differ only in read location, and its `INTERVIEW_FOCUS` names both true statements and both distractor shapes |

The blueprint Note added by this review for L3.1 — that the three phases must be named at
Bridge depth — is therefore discharged. It is recorded as taught, not as planned.

One honest mismatch is worth stating rather than smoothing over.
`compose_state_read_recomposition_scope` is levelled FOUNDATION, and the finished Lesson
reaches well past what the Question asks: the inline-lambda caveat and the
value-versus-holder distinction are not assessed by anything in the bank. That is a Lesson
teaching its concept rather than the question, which is the intended direction, but it means
the Question no longer represents the Lesson's depth. E23-07 may want Applied coverage of
read placement; it is a candidate, not a defect.

### Assessment gaps confirmed unchanged

**GAP-U3-A stands exactly as recorded.** `lesson_recomposition_cost` maps
`compose_recomposition` as its only primary concept and carries
`compose_recomposition_performance` as supporting, so the one active Question on that
Subtopic — `compose_recomposition_performance_001`, which lives in the `performance` Topic —
creates no Unit 3 practice coverage. Nothing in Unit 3's practice mappings assesses that
recomposition is the normal operating mode or that cost is the work done during composition.
`BundledLearningCurriculumTest.aPerformanceConceptStaysSupportingRatherThanBecomingUnitPractice`
now pins this, so a later change cannot quietly promote the mapping and make the gap
disappear without the test failing.

GAP-U2-A and GAP-U2-B stand exactly as recorded. Unit 2 teaches both bodies of reasoning —
`lesson_remember_composition_memory` carries the four-combination table GAP-U2-A describes,
and `lesson_observable_collections` carries the unobserved-mutation failure GAP-U2-B
describes — and neither is assessed by any active Question. Teaching a concept does not
close an assessment gap; both remain E23-07's work.

### Technical assumptions the finished Lessons depend on

Verified during authoring against the artifacts this repository actually resolves, not from
memory or from upstream `androidx-main` alone.

| Claim a Lesson makes | How it was verified |
| --- | --- |
| `mutableStateOf` defaults to `structuralEqualityPolicy()`, so assigning a structurally equal value schedules nothing | `SnapshotState.kt` and `SnapshotMutationPolicy.kt` in the sources of the resolved `androidx.compose.runtime:runtime:1.11.2` artifact |
| Reads of `MutableState.value` during a composable's execution subscribe the currently executing recompose scope, and a write schedules recomposition of the subscribed scopes | The `MutableState` KDoc in the same resolved artifact |
| A non-inline `@Composable` lambda argument — such as a `Button`'s `content` — forms its own restart scope, so an unremembered holder read only inside one is not reset by the write the click caused | Executed, not reasoned about: a throwaway `runComposeUiTest` probe composed both variants and clicked them. See [the unremembered-state example](#the-unremembered-state-example) |
| `rememberSaveable` unregisters its value provider when its composable leaves the Composition, so it does not by itself restore across a branch closing and reopening; `SaveableStateHolder` is what saves a subtree before disposing it | `RememberSaveable.kt` (`onForgotten` → `entry?.unregister()`) and the `SaveableStateHolder` KDoc in the resolved `androidx.compose.runtime:runtime-saveable:1.11.2` sources |
| `rememberSaveable` offers both a `saver` overload for a value and a `stateSaver` overload for a `MutableState`, and `listSaver` takes `save`/`restore` | `RememberSaveable.kt` and `ListSaver.kt` in the same resolved sources |
| `Column`, `Row` and `Box` are `inline`, so their content lambdas form no recomposition scope | `Column.kt`, `Row.kt` and `Box.kt` in the sources of the resolved `org.jetbrains.compose.foundation:foundation-layout-desktop:1.11.1` artifact |
| Composable functions are compiled as restartable by default | The `@NonRestartableComposable` KDoc in the resolved runtime: the annotation exists to *prevent* code being generated that allows skipping or restarting |

### The unremembered-state example

`lesson_observable_state` teaches the classic "observable but not remembered" failure. The
first draft wrote it with the state read inside a `Button` content lambda and claimed the
counter stays at zero. That claim is **false**. It was caught in review and then settled by
running it rather than by argument:

| Variant | Behaviour after one click |
| --- | --- |
| Read inside `Button`'s `content` lambda | Displays `Clicked 1 times`, then `Clicked 2 times` — it counts |
| Read in the same body that calls `mutableStateOf` | Displays `Clicked 0 times` — stuck, which is the intended demonstration |

The cause is that a non-inline composable lambda argument is its own restart scope, so the
write invalidates only the lambda, which re-executes against the *same* captured holder. The
enclosing body never re-runs, so `mutableStateOf(0)` is never called again.

The shipped Lesson uses the second variant, and a `NOTE` states the guarantee accurately:
the value is lost whenever the composable that called `mutableStateOf` executes again, while
*when* the reset becomes visible depends on which scope recorded the read. **Units 4–6 should
take the same care.** "Unremembered state resets immediately" and "extracting a function
creates a scope" are both plausible, both wrong, and both easy to write by accident.

### L2.3's lifetime ladder has four rungs, not three

Review also found that the first draft merged "the composable left the Composition" into
"the UI was recreated" and named a saved-state mechanism as the answer to both. Those are
different events with different answers: `rememberSaveable` unregisters itself as its
composable leaves, so it does not survive a branch closing and reopening on its own — that
needs an owner that stays, or a `SaveableStateHolder` such as navigation provides.

The shipped ladder therefore reads re-execution → leaving the Composition → UI recreation →
process recreation, and only the last two are `rememberSaveable`'s. A second defect in the
same draft is fixed with it: the prose had said the process-death rung "is not what
`rememberSaveable` is for", which contradicted both the Lesson's own table and the documented
guarantee. `rememberSaveable` *is* the composable-level answer to system-initiated process
death with the task retained; what it is not is durable storage.

The blueprint's L2.3 line still says "three lifetimes". That shorthand is what this authoring
found too coarse, and Unit 8 should not inherit it.

**Unresolved question 1 is still unresolved.** The plan asked E23-02 to re-check whether
Compose Multiplatform 1.11.x restores `rememberSaveable` state across application restart on
desktop, iOS or web. It was re-checked and nothing authoritative was found: the CMP lifecycle
documentation still covers lifecycle states and events only. `lesson_remember_saveable`
therefore states the Android guarantee precisely and says plainly that no equivalent
guarantee is documented elsewhere, exactly as this plan instructed. It asserts nothing about
desktop, iOS or web behaviour in either direction.

---

## Assessment gaps for E23-07

Substantive gaps only. There is no per-Lesson or per-level quota, and a gap here is a
candidate for authoring rather than an automatic defect. Each gap names the reasoning that is
missing, not a number.

| Gap | Unit / Lesson | Reasoning no ACTIVE Question assesses | Why it is substantive | Recommended action |
| --- | --- | --- | --- | --- |
| GAP-U2-A | Unit 2 / `lesson_observable_state`, `lesson_remember_composition_memory` | Distinguishing "observable" from "remembered": the four combinations of `remember` and `mutableStateOf` and what each does | This is the central confusion Unit 2 exists to resolve, and `compose_state`'s only active Question is about saved-state lifetimes | Add coverage in E23-07 |
| GAP-U2-B | Unit 2 / `lesson_observable_collections` | That mutating a collection held in state notifies nothing, and what the alternatives cost | No Question in any Topic connects collection mutation to a missing Compose update. `kotlin_readonly_list_not_immutable` and `data_class_copy_is_shallow` are Kotlin-side and supporting-only, so they create no Unit 2 practice | Add coverage in E23-07 |
| GAP-U3-A | Unit 3 / `lesson_recomposition_cost` | That recomposition is the normal operating mode and the cost is the work done during composition | Unit 3's two active Questions cover the definition and the scope rule only. `compose_recomposition_performance_001` sits in the `performance` Topic and is supporting-only here, so it must not be counted as Unit 3 coverage | Add coverage in E23-07 |
| GAP-U4-A | Unit 4 / `lesson_composable_identity` | Call-site identity outside lists — state discarded because a composable moved in the tree | The one active identity Question is entirely lazy-list framed, so the Lesson that supplies the underlying model is unassessed | Add coverage in E23-07 |
| GAP-U4-B | Unit 4 / `lesson_stability_annotations` | That `@Stable` and `@Immutable` are promises the compiler trusts, and that a false promise is a correctness bug | Nothing in the bank assesses the annotations at all, and this is the Lesson with the highest interview signal in Unit 4 | Add coverage in E23-07 |
| GAP-U4-C | Unit 4 / `lesson_immutability_vs_stability` | The Compose consequence of read-only-but-not-immutable data, as opposed to the Kotlin fact | The Kotlin fact is assessed (`kotlin_readonly_list_not_immutable`), but only as supporting context for this Lesson | Add coverage in E23-07, after GAP-U4-A and GAP-U4-B |
| GAP-U5-A | Unit 5 / `lesson_remember_key_memoization` | Key-based cache invalidation: stale results from under-specified keys, no cache from over-specified ones | `compose_derived_state` carries one Question and it is about `derivedStateOf`; the memoization half of the Unit is unassessed | Add coverage in E23-07 |
| GAP-U5-B | Unit 5 / `lesson_work_outside_composition` | Deciding that work does not belong in composition at all, and which layer owns it | `main_thread_performance` is View-toolkit framed and supporting-only; no Question connects placement of work to composition | Add coverage in E23-07 |
| GAP-U6-A | Unit 6 / `lesson_snapshot_observation` | The observation model itself: which reads create dependencies and what a write invalidates | The only Question on `compose_snapshot_system` identifies an API. Unit 6's primary Lesson has no assessment of its own reasoning | Add coverage in E23-07 |

Two observations that are **not** gaps and must not be treated as quotas:

- `android_ui` currently holds no `ADVANCED` Question at all, while Units 4–6 carry the epic's
  deepest reasoning. If E23-07 authors Advanced material anywhere, this is where the reasoning
  would justify it — on merit, not to fill a level.
- `lesson_remember_saveable`, `lesson_state_hoisting`, `lesson_composition_and_recomposition`,
  `lesson_recomposition_scopes`, `lesson_keys_and_identity_in_lists`,
  `lesson_stability_and_skipping`, `lesson_derived_state` and `lesson_snapshot_flow` each have
  an active Question whose reasoning this plan places in them. Structurally all eighteen map
  to a Subtopic that has at least one active Question, which is exactly why the review above
  was read rather than counted. Eight of eighteen with matching reasoning is the expected
  shape for a bank averaging one Question per Subtopic — not a coverage failure.

---

## Source freshness and technical assumptions

### Configured versions this plan assumes

Read from `gradle/libs.versions.toml` and the module build files; the runtime version was
resolved, not assumed.

| Item | Value | How it was established |
| --- | --- | --- |
| Kotlin | 2.4.10 | `gradle/libs.versions.toml` |
| Compose compiler plugin | `org.jetbrains.kotlin.plugin.compose`, version = Kotlin version | `libs.versions.toml`; applied by `:shared`, `:androidApp`, `:desktopApp`, `:webApp` |
| Compose Multiplatform | 1.11.1 | `libs.versions.toml` |
| Resolved Compose runtime | `androidx.compose.runtime:runtime:1.11.2` | `./gradlew :shared:dependencyInsight --configuration jvmRuntimeClasspath --dependency androidx.compose.runtime:runtime` |
| Compose compiler options | none — no `composeCompiler { }` block exists in any module | Repository search |
| Targets | Android, JVM/desktop, iOS (arm64, simulator arm64), JS browser, Wasm/JS browser | `shared/build.gradle.kts` |

Two consequences that examples depend on:

- **Strong Skipping is on**, because it is the default from the Compose compiler shipped with
  Kotlin 2.0.20 and this repository sets no compiler options that would disable it. Examples in
  L4.4 and L4.5 must state that assumption where behaviour depends on it.
- **No stability configuration file and no compiler metrics** are configured. L4.4 and L4.5 must
  not present either as something the reader can already inspect in this project.

### Verified claims

Reviewed 2026-09-08. Each row is a claim the Lessons depend on, with the page that supports it.

| Claim the Lessons rely on | Source | Note for the author |
| --- | --- | --- |
| `mutableStateOf` creates an observable `MutableState`; changes to `value` schedule recomposition of readers | <https://developer.android.com/develop/ui/compose/state> | Wording to teach: the *read* creates the dependency |
| `remember` stores a value in the Composition and forgets it when the composable leaves; it does not survive configuration change; `remember(key)` invalidates and recomputes when a key changes | <https://developer.android.com/develop/ui/compose/state> | Supports L2.2 and L5.1 directly |
| Non-observable mutable objects such as `ArrayList` or a mutable data class do not trigger recomposition; prefer `State<List<T>>` with read-only `listOf()` | <https://developer.android.com/develop/ui/compose/state> | Supports L2.5's failure mode |
| Hoisting rules: lowest common parent of readers, highest level of writers, hoist together what changes together | <https://developer.android.com/develop/ui/compose/state> | Supports L2.4's "lowest sensible owner" framing |
| `rememberSaveable` stores state in a `Bundle` through the saved-instance-state mechanism, survives configuration change and system-initiated process death, is size-limited (`TransactionTooLargeException`), and needs a Saver for non-primitive types | <https://developer.android.com/develop/ui/compose/state-saving> | Android-specific; see the platform caveat below |
| A composable's identity is its call site; `key` distinguishes multiple calls from one call site and need only be unique among those calls | <https://developer.android.com/develop/ui/compose/lifecycle> | Supports L4.1 and L4.2 |
| Composition, layout and drawing are distinct phases; state read in layout or draw changes the screen without recomposing | <https://developer.android.com/develop/ui/compose/phases> | Bridge depth only in L3.1 — the phase argument is Unit 13 |
| A type is stable if it is immutable or if Compose can know whether its value changed; collections such as `List`, `Set` and `Map` are always treated as unstable because immutability cannot be guaranteed | <https://developer.android.com/develop/ui/compose/performance/stability> | Supports L4.3 and L4.4 |
| Strong Skipping is enabled by default from Kotlin 2.0.20; all restartable composables become skippable; unstable parameters are compared with `===` and stable ones with `equals`; lambdas are automatically remembered | <https://developer.android.com/develop/ui/compose/performance/stability/strongskipping> | The anchor for L4.4, and for correcting the obsolete "unstable always recomposes" claim |
| The stability configuration file is an opt-in contract with the compiler and does not make a class stable; kotlinx immutable collections are treated as immutable | <https://developer.android.com/develop/ui/compose/performance/stability/fix> | Reference only, in L4.5 |
| `derivedStateOf` is for inputs that change more often than the result needs to be observed; combining two state objects is not a reason to use it; it is expensive relative to trivial calculation | <https://developer.android.com/develop/ui/compose/side-effects> and the `DerivedState.kt` KDoc, <https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/DerivedState.kt> | See the version caveat on `computedStateOf` below |
| `snapshotFlow` runs its block in a read-only snapshot, records the state it reads, emits when the result is not equal to the previous one, and **conflates** — "only the most recent state matters", and the block may run once after many rapid changes | <https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/SnapshotFlow.kt> and <https://developer.android.com/develop/ui/compose/side-effects> | This is what forbids teaching snapshot observation as an event log |
| A Kotlin read-only collection type protects the reference, not the underlying object; write operations remain possible through a mutable reference held elsewhere | <https://kotlinlang.org/docs/collections-overview.html> | The page contrasts read-only and mutable interfaces but does not state "read-only is not immutable" in those words — L4.3 should make the argument from the interface contract and cite the Compose stability page for the consequence |

### Version-sensitive findings that constrain authoring

- **`computedStateOf` must not be taught.** The current upstream `DerivedState.kt` KDoc
  recommends `computedStateOf` for trivial calculations, but that function does not exist in the
  runtime this repository resolves. `javap` over
  `androidx.compose.runtime:runtime:1.11.2` shows only `derivedStateOf(calculation)` and
  `derivedStateOf(policy, calculation)`. This is a concrete case of current upstream guidance
  running ahead of the configured version; L5.2 must teach the two overloads that exist.
- **The lifecycle page still leads with pre–Strong Skipping rules.** Its skipping list gives "a
  required parameter is of a non-stable type" as a disqualifier and adds only that Strong
  Skipping "relaxes the last requirement". Reading that page alone reproduces exactly the
  folklore L4.4 is required to correct; the Strong Skipping page is the authority for behaviour
  under this repository's configuration.
- **`retain` is on the classpath but out of scope.** `androidx.compose.runtime:runtime-retain`
  ships `retain { }` alongside `remember`. The blueprint does not plan it, and E23 should not
  add it: it is adjacent to Unit 2's subject but is a fourth lifetime to hold in mind while the
  first three are still new. If a Lesson names it at all, its experimental/opt-in status must be
  checked at authoring time.

### Saved-state guarantees and platform differences

This is the least settled area in the plan, and L2.3 must be written to survive it.

- The Android guarantee is documented and quoted above: `Bundle`, saved instance state,
  configuration change and system-initiated process death, with a size limit.
- **Compose Multiplatform does not document an equivalent guarantee.** The CMP lifecycle page
  (<https://kotlinlang.org/docs/multiplatform/compose-lifecycle.html>) covers lifecycle states
  and events only and says nothing about saved state; the CMP 1.11.1 and 1.9.x release pages
  (<https://kotlinlang.org/docs/multiplatform/whats-new-compose-111.html>) list the
  `org.jetbrains.androidx.savedstate` dependency without stating what is restored on desktop,
  iOS or web.
- The clearest maintainer statement found is from 2023
  (<https://github.com/JetBrains/compose-multiplatform/issues/3480>): Android "only allows to
  store serializable values" while desktop, web and iOS "by default every object is allowed to
  be saved, because they don't save/restore state anywhere yet". That predates the versions in
  use here by several releases and **must not be quoted as current behaviour**.
- In-repository evidence points the same way: `AppNavigatorRestorationTest` notes that
  `StateRestorationTester` is unimplemented on skiko and drives `LocalSaveableStateRegistry`
  directly, and `AppNavigator` uses `rememberSaveable` with a `Saver` that tolerates a value
  that no longer restores.
- **Instruction for E23-02:** state the Android guarantee precisely, describe
  `rememberSaveable` as participation in a host-provided saved-state mechanism, and say plainly
  that on the other Compose Multiplatform targets what the host restores is not documented as a
  guarantee. Do not assert desktop, iOS or web restoration behaviour either way. The one claim
  that holds everywhere — and the one the interview answer needs — is the lifetime ladder:
  recomposition, then composition removal, then process recreation.

### Unresolved questions

1. Whether Compose Multiplatform 1.11.x restores `rememberSaveable` state across application
   restart on desktop, iOS or web. Not documented; deliberately left as an assumption rather
   than a claim. Re-check at E23-02, and again if the Lesson is ever revised.
2. Whether `retain` is experimental in the resolved runtime. Not needed unless E23-02 names it.
3. Per-function API reference pages on `developer.android.com` render their contracts through
   client-side scripting and could not be read directly. The androidx sources were used instead,
   which are the authoritative statement of the same contract, and the surface was checked
   against the resolved artifact.

---

## Blueprint changes made by this review

Four changes, each recorded in `docs/content/compose-learning-blueprint.md` in the same commit.
None changes the Unit or Lesson count.

1. **L2.4 — `compose_udf` demoted from primary to supporting.** Unit 1's shipped
   `lesson_state_down_events_up` already declares `compose_udf` primary and owns the concept.
   Leaving it primary in L2.4 would have made two Lessons contractually responsible for the same
   concept and would have pulled `compose_udf_event_direction` into Unit 2 practice for reasoning
   Unit 1 teaches. The blueprint's own L1.3 mapping is the mirror image — hoisting is supporting
   there — so this restores the intended symmetry.
2. **L3.1 — Note added that the three phases are named at Bridge depth.** The Lesson's Core did
   not mention layout and drawing, yet `composition_vs_recomposition` cannot be answered without
   distinguishing them, and E23-03 requires it.
3. **L2.3 — Note added recording the platform caveat.** The saved-state guarantee the Lesson
   teaches is Android's; the product ships on desktop, iOS and web, where no equivalent
   guarantee is documented.
4. **L6.2 — Note added that the Effects and Flow pointers must not promise in-app Lessons.**
   Unit 7 and the Flow curriculum are outside E23, and `relatedLessonIds` cannot reference a
   Lesson that does not exist.

---

## Handoff

### For E23-02 through E23-06

- Take Unit and Lesson ids from the [identity tables](#identity-conventions-and-proposed-identities)
  verbatim. They are checked unique against production content; changing one later costs a
  learner's study-progress record.
- Primary and supporting mappings are settled and valid. Keep primary mappings to the concepts
  the Lesson genuinely owns — they are what Unit practice selects on.
- **Forward links are invalid until their target ships.** `LearningCurriculumValidator` rejects
  a `relatedLessonIds` entry that names an unknown Lesson, so each authoring issue may link
  *backwards* to published Lessons only. Pointing forward is a prose sentence naming the Unit,
  and the issue that ships that Unit may then add the reciprocal link. Unit 2 followed this:
  every `relatedLessonIds` entry it ships resolves inside the bundle, and every reference to
  Units 4–6 and to the Effects, ViewModel and Performance Units is prose naming the Unit.
  `BundledLearningCurriculumTest.relatedLessonReferencesResolveWithinTheShippedDocument` now
  enforces it. Unit 3 followed it too. **E23-04 through E23-06 should add the reciprocal
  backward links** from their own Lessons; Units 2 and 3 do not need editing to receive
  them.
- Prerequisites outside the Compose path — lifecycle, Kotlin, architecture, Flow — have no
  authored learning Unit and must be bridged in place to the depth the Lesson needs, then
  pointed at their Topic. No Lesson in Units 2–6 depends on an unpublished Lesson except in the
  epic's own order.
- Regenerate `docs/content/learning-question-coverage.md` in the same change that adds Lessons.

### For E23-07

Read [Authoring outcomes](#authoring-outcomes-for-units-2-and-3) first: it records the
semantic re-check of the two recomposition Questions against the finished prose, confirms
GAP-U2-A, GAP-U2-B and GAP-U3-A unchanged, and adds one new candidate — Applied coverage of
read placement, which `lesson_recomposition_scopes` now teaches well past what the single
FOUNDATION Question assesses.

The nine gap rows are the starting list. Re-read them against the finished prose before
authoring: a gap this plan predicted may have been answered by a Lesson that turned out deeper
than planned, and a new one may have appeared. Regenerating
`docs/content/question-bank-coverage.md` closes the staleness recorded above.

### For E23-08

The five Units read in blueprint order, and the concept boundaries in this document are what
the cross-Unit review should test: no concept is taught twice, each Unit's prerequisites are
satisfied by an earlier Unit or bridged in place, and no Lesson links to material that does not
exist.
