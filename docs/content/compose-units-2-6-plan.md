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

**Authoring status.** E23-02, E23-03, E23-04 and E23-05 shipped Units 2, 3, 4 and 5; all
four are ACTIVE in `learning_curriculum.json`. What that authoring found is recorded in
[Authoring outcomes for Units 2 and 3](#authoring-outcomes-for-units-2-and-3),
[Authoring outcomes for Unit 4](#authoring-outcomes-for-unit-4) and
[Authoring outcomes for Unit 5](#authoring-outcomes-for-unit-5). Unit 6 is still a plan, so
its identities below remain proposals until E23-06 ships them.

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

## Authoring outcomes for Unit 4

Added by E23-04 after the five Lessons were written. Everything below was checked against
the artifacts this repository resolves or executed against them; nothing here is recalled.

### What did not change

Every proposed Unit id, Lesson id, title, authored order and primary/supporting mapping for
Unit 4 in the [identity tables](#identity-conventions-and-proposed-identities) shipped
verbatim. No Lesson boundary moved, no Lesson split or merged, and no blueprint correction
was required. All five Lessons carry Core, Practical and Senior depth. Every
`relatedLessonIds` entry resolves inside the shipped bundle: Unit 4 links backwards to
Units 1–3 and within itself, and the references to the lazy-layout, performance and effects
Units are prose naming the Unit, as the handoff requires.

### Compiler configuration the Lessons assume

Re-read at authoring time rather than taken from this plan's earlier table. Kotlin is
2.4.10, the Compose compiler plugin is versioned with it, Compose Multiplatform is 1.11.1,
and no module declares a `composeCompiler { }` block, so no compiler option is customised
anywhere in the repository. **Strong Skipping is enabled**, and it is no longer a mode that
can be chosen: the Gradle plugin's own `StrongSkipping` feature flag is deprecated at
`ERROR` level with the message "This flag should be enabled by default and will be removed
with Kotlin 2.5.0", read from the `ComposeFeatureFlag` class in the resolved
`compose-compiler-gradle-plugin-2.4.10` artifact. No stability configuration file and no
compiler metrics are configured, and L4.4 and L4.5 say so rather than presenting either as
something a reader can inspect here.

### Claims that were executed rather than reasoned about

A throwaway `runComposeUiTest` probe on the JVM target measured each of these and was then
deleted. Composables recorded their own executions, and identity was measured by giving
each composable a `remember`ed token, so a changed token means a discarded remember slot.

| Claim a Lesson makes | Measured result |
| --- | --- |
| Two `if` branches calling the same composable are two identities | Token 1 in the `then` branch; a new token 10 appeared in the `else` branch after the flag flipped |
| One call site whose modifier changes keeps its identity | Token unchanged across the flip |
| A changed `key(...)` value discards the instance | Token 3 replaced by token 11 |
| Without keys, remembered state stays with the position when a list is reordered | Reversing `[a, b, c]` left tokens 4, 5, 6 in place, so the item labelled `a` occupied the slot that had belonged to `c` |
| With `key(item)`, remembered state travels with the item | Tokens 7, 8, 9 stayed with `a`, `b`, `c` across the same reversal |
| Strong Skipping is on: the same unstable instance passed again is skipped | Parent executed 3 times, child once — under pre–Strong Skipping rules a required unstable parameter made the function unskippable, so it would have run 3 times |
| A fresh but structurally equal unstable instance re-executes the child | Parent 3, child 3 |
| A stable value equal to the previous one is skipped | Parent 3, child 1 |
| A `@Stable` type whose property is backed by `mutableStateOf` still updates its reader when only the property changes | Writing the property left the parent at 1 execution and ran the child a second time; a later unrelated parent invalidation with the same instance skipped the child |
| A type whose `equals` ignores the mutable data the UI displays produces a stale screen | The composable displaying a track count was never re-executed while the backing list grew from one entry to three |
| A truthful stability annotation changes which comparison is generated, so a fresh but equal instance is skipped | `data class MutableThing(var label: String)` re-ran 3 times on fresh-but-equal arguments; the identical type annotated `@Stable` ran once. `javap -c` shows `changedInstance` emitted for the first and `changed` for the second |
| A data class holding an ordinary `List` is *not* statically inferred stable | `javap -c` shows the compiler emitting **both** comparisons for it and deferring to the call site, exactly as it does for a bare `List` parameter — the same runtime resolution described above, not an inference of stability |

### The Strong Skipping comparison rule did not reproduce for collections

This is the most important finding in this issue and it is deliberately recorded in full.

The Strong Skipping documentation states that unstable parameters are compared using
instance equality (`===`) and stable ones with `equals`, and the stability page states that
collections such as `List`, `Set` and `Map` are always considered unstable. Under this
repository's configured toolchain a composable taking a `List<Row>` parameter, whose caller
built a fresh and structurally equal list on every pass, was **not** re-executed — parent 3
executions, child 1 — while a control that changed the list's contents was re-executed
every time. Distinct instances were confirmed by printing identity hash codes, so a genuinely
new object reached the call on each pass. The same experiment against a class with a `var`
property behaved exactly as documented.

`javap -c` over the compiled probe explains it without speculation. For a statically stable
parameter the compiler emits `Composer.changed`; for a statically unstable one it emits
`Composer.changedInstance`; for a parameter typed as the `List` interface it emits **both**
and selects at run time from a bit in the caller-supplied `$changed` flags. In the measured
calls the call site set no bit marking the argument unstable, so the `equals` branch ran.
`GapComposer.kt` in the resolved `androidx.compose.runtime:runtime:1.11.2` sources confirms
the semantics: `changed` compares with `!=` and `changedInstance` with `!==`.

**How L4.4 handles it.** The documented rules are taught as the rules, because they are the
guarantee and they are what an interview asks for. The observation is recorded beside them
as an observation about a configured toolchain, with the conclusion that which comparison a
given compiler picks for a given call is an implementation decision that has already changed
once and must not be designed against in either direction. The two design rules the Lesson
leaves the reader with — hold a stable identity for values passed repeatedly, and make types
genuinely stable so `equals` is meaningful — survive whichever answer a future compiler gives.

### Semantic re-check of the two Unit 4 Questions

Both were re-read against the finished prose rather than against this plan's expectation.

| Question | Reasoning it requires | Does the finished Lesson teach it | Evidence |
| --- | --- | --- | --- |
| `compose_key_identity_lazy_state` | Identity is positional without a key, so remembered state stays with the slot; and Compose never infers identity from contents | **Yes** | `lesson_keys_and_identity_in_lists` Core carries a four-row table of what insertion, deletion and reordering do to positional identity, the measured reversal result, and an explicit statement that the runtime never compares item contents. Its `INTERVIEW_FOCUS` names both distractor shapes — the list being rebuilt, and identity inferred from contents. `lesson_composable_identity` supplies the underlying model |
| `compose_strong_skipping_instance_equality` | Strong Skipping makes restartable composables skippable but compares unstable arguments by instance, so a fresh equal list still re-runs | **Partly, and with a conflict** | `lesson_stability_and_skipping` Practical teaches exactly this rule and the three-row comparison table the Question is built on, so a reader can answer it. Its Senior section then records that the specific scenario in the Question's stem — a rebuilt equal `List` — did not reproduce on this toolchain. A reader who takes the documented rule answers correctly; a reader who takes the measured observation answers `a` and is marked wrong |

**Two findings for E23-07, in priority order.**

1. **Correctness, not level.** `compose_strong_skipping_instance_equality` asserts an
   outcome for a rebuilt equal `List` that this repository's compiler does not produce. The
   Question is faithful to the documentation and was verified against it on 2026-09-08; it
   is the *configured behaviour* that diverges. E23-07 should re-run the measurement before
   deciding anything, and then choose deliberately between re-framing the stem around a type
   the compiler classifies as unstable statically — which would assess the same reasoning
   without depending on interface-typed parameters — and leaving it as an assessment of the
   documented rule with the explanation made explicit about that being what it tests. No
   Question was added, edited, re-levelled or re-mapped in E23-04.
2. **Level, as this plan already predicted.** The same Question is levelled `FOUNDATION`
   while identifying a comparison rule is compiler-behaviour reasoning rather than recall.
   That remains a candidate for review, not a defect.

One further candidate, of the same shape as the one Unit 3 produced.
`compose_key_identity_lazy_state` is levelled `APPLIED` and is the only active Question on
`compose_identity_keys`, so both identity Lessons rest on it. `lesson_composable_identity`
now teaches call-site identity, the recomposition-versus-re-entry distinction, and the
counter-example that a single call site keeps its state however much it moves — none of
which the Question reaches. That is GAP-U4-A, confirmed below rather than closed.

### Assessment gaps confirmed unchanged

All three Unit 4 gaps stand, and teaching a concept does not close an assessment gap.

- **GAP-U4-A stands.** The one active identity Question is entirely lazy-list framed.
  `lesson_composable_identity` maps `compose_identity_keys` as its only primary concept and
  now teaches the branch-versus-call-site distinction and the three-event table separating
  recomposition, leaving the Composition and re-entering it. Nothing active assesses any of
  it outside a list.
- **GAP-U4-B stands, and is now the widest gap in the Unit.** Nothing in the bank assesses
  `@Stable` or `@Immutable` at all, while `lesson_stability_annotations` is the Lesson with
  the highest interview signal in Unit 4 and teaches the annotations as correctness
  contracts with a measured stale-UI failure behind them.
- **GAP-U4-C stands.** `lesson_immutability_vs_stability` maps `compose_stability` as its
  only primary concept and carries `kotlin_data_classes`, `kotlin_equality`,
  `kotlin_collections` and `kotlin_variables` as supporting, so the Kotlin Questions those
  Subtopics carry create no Unit 4 practice coverage.
  `BundledLearningCurriculumTest.kotlinLanguageConceptsStaySupportingRatherThanBecomingUnitPractice`
  now pins that, in the same way Unit 3's performance mapping is pinned, so a later change
  cannot quietly promote one of them and make the gap disappear silently.

### Two corrections found in review

Both were claims the first draft asserted rather than measured, and both were settled by
running them. They are recorded because the shape of the mistake is likely to recur in
Units 5 and 6: a sentence that is a reasonable generalisation of a verified result, but that
was never itself verified.

- **"No stability annotation can help when the caller rebuilds the value" was false.** L4.4's
  Practical section said that a fresh instance defeats skipping whatever the parameter type is
  annotated as, which contradicted the stable-value row in the table immediately above it.
  Stability decides *which comparison is generated*, so a genuinely stable type is compared
  with `equals` and a fresh but equal value is skipped. The Lesson now gives the middle row two
  exits — hold the instance still, or make the type honestly stable — and says that the second
  is available only when the type deserves it, which is what L4.5 then covers.
- **"An immutable data class over read-only collections needs no annotation" was unverified and
  wrong as stated.** The documented rule is that collections are always treated as unstable
  because their immutability cannot be guaranteed, so authoring discipline about never mutating
  a `List` is invisible to the analysis. Measurement shows this toolchain does not conclude
  "unstable" either — it emits both comparisons and defers to the call site — but it certainly
  does not *infer stability*, which is what the sentence claimed. L4.5 now separates domain
  immutability from compiler classification, names the three honest routes if a stable
  comparison is actually needed, and points back at L4.4's instruction not to design against
  the observed behaviour.

Both corrections tightened the same seam: L4.4 tells the reader to design against the
documented rule rather than the measured one, so no later Lesson may quietly lean on the
measured one to make a recommendation sound simpler than it is.

### Editorial decisions worth carrying forward

- **The three questions are the Unit's spine.** Every Lesson keeps "is this the same
  composition identity", "was an observable change recorded" and "can execution be skipped"
  apart, and L4.3 turns them into an explicit diagnostic order. The instruction that
  produced this is worth repeating in Units 5 and 6: do not attribute a stale screen to
  skipping before establishing that an observable write happened at all.
- **Two suppression mechanisms are kept apart.** `mutableStateOf` comparing an assigned
  value with the previous one under `structuralEqualityPolicy` is a *state write* being
  suppressed inside the holder; the compiler-generated parameter comparison is a *call*
  being skipped at the call site. L4.3 states this in a `NOTE` because conflating them is
  what sends people to stability tooling for a bug that is two steps earlier.
- **The annotation example is reported honestly.** Removing `@Immutable` from the failing
  type did not change the outcome on this toolchain, because the compiler could not classify
  the type either and its `equals` still reported the two instances equal. L4.5 says so, and
  frames the annotation's damage as converting a cautious compiler judgement into a
  guarantee the compiler is entitled to rely on — latent and toolchain-dependent rather than
  absent. Manufacturing a cleaner demonstration would have taught something untrue.

---

## Authoring outcomes for Unit 5

Added by E23-05 after the three Lessons were written. Everything below was read from the
artifacts this repository resolves or executed against them; nothing here is recalled.

### What did not change

Every proposed Unit id, Lesson id, title, authored order and primary/supporting mapping for
Unit 5 in the [identity tables](#identity-conventions-and-proposed-identities) shipped
verbatim. No Lesson boundary moved, no Lesson split or merged, and no blueprint *line* was
corrected — two blueprint Notes were added, recording the mutable-key failure for L5.1 and
the capture-and-lifetime material for L5.2. All three Lessons carry Core, Practical and
Senior depth. Every `relatedLessonIds` entry resolves inside the shipped bundle: Unit 5
links backwards into Units 1–4 and within itself, and the Effects, Flow, ViewModel and
Performance Units are named in prose only, as the handoff requires.

All three Lessons map `compose_derived_state` as their only primary concept, so
`PracticeTargetResolver` resolves the Unit to exactly that one Subtopic and Unit practice
runs on its single active Question. That is deliberate — the Unit is one decision model
taught in three passes, and inventing a wider primary mapping would claim practice coverage
the taxonomy does not have.
`BundledLearningCurriculumTest.architectureAndPerformanceConceptsStaySupportingRatherThanBecomingUnitPractice`
pins L5.3's three cross-Topic concepts as supporting, in the same way Unit 3's performance
mapping and Unit 4's Kotlin mappings are pinned.

### Runtime facts read from the resolved artifacts

| Claim a Lesson makes | How it was verified |
| --- | --- |
| `remember(key1)` returns the previous value while `key1` compares equal (`==`) to the value it had in the previous composition | The `remember` KDoc in `Composables.kt` in the sources of the resolved `androidx.compose.runtime:runtime:1.11.2` artifact, whose overloads delegate to `Composer.changed`, documented as `equals`-based |
| The runtime offers exactly two `derivedStateOf` overloads — `(calculation)` and `(policy, calculation)` | `javap` over the resolved `runtime-desktop-1.11.2` artifact: `SnapshotStateKt` declares those two and nothing else |
| A derived state with no policy signals an update on every dependency change, and Compose's recomposition machinery is the observer that filters it — comparing the new result with the one the reading scope last saw, using structural equality when no policy was supplied | The `derivedStateOf(calculation)` KDoc in `DerivedState.kt`, together with `RecomposeScopeImpl.checkDerivedStateChanged` in the same resolved sources, which reads `policy ?: structuralEqualityPolicy()` |

**The `computedStateOf` finding is re-confirmed and stays out of scope.** E23-01 recorded
that the current upstream `DerivedState.kt` KDoc recommends `computedStateOf` for trivial
calculations while the configured runtime does not ship it. Re-checked here against the
resolved artifact rather than the plan: `javap` lists only the two overloads above. The
Lessons teach those two, and the API is not named anywhere in Unit 5, per this issue's
agreed scope.

### Claims that were executed rather than reasoned about

A throwaway `runComposeUiTest` probe on the JVM target measured each of these and was then
deleted. Every case counted the calculation and the enclosing composable execution
separately, so "the body ran and the calculation did not" is an observation.

| Claim a Lesson makes | Measured result |
| --- | --- |
| A derived value whose result changes less often than its input spares its readers | Index driven 0 → 1 → 2 → 3 → 4: the derived calculation ran 5 times, the scope reading it executed 2 times — initial composition and the single false → true flip. The three later index changes invalidated nobody |
| `derivedStateOf` does not reduce how often the calculation runs | Same run: 5 calculations for 4 input changes plus the initial composition, while the consumer ran twice |
| A trivial derived expression removes nothing | A label rebuilt from an index that changed at every step: the consumer executed 5 times both with and without the wrapper, and the wrapper added a derived-state object and a comparison per change |
| A missing key serves a stale result | An unkeyed `remember` ran its calculation once and never again, across every later execution of the body |
| A correctly keyed calculation runs when, and only when, its dependency differs | Keyed on the one value it reads: ran again on that value's change, did not run when an unrelated value changed |
| An irrelevant key recalculates for nothing | Keyed on one real dependency and one unrelated value: ran for both, producing the same result |
| Mutating an object used as a key invalidates nothing | The enclosing body executed twice more after the key object was mutated; the calculation did not run again, and the rendered text kept the pre-mutation value |
| A `derivedStateOf` capturing a non-`State` input goes stale | Threshold captured as an ordinary `Int`, changed from 1 to 5, index then moved to 2: the flag reported `true`, still comparing against the captured 1 |

### The distinction the measurement forced

The single most important correction the probe produced is that **`derivedStateOf` filters
consumers, not calculations**. The first draft of L5.2 was written around the natural
shorthand — that the wrapper stops the derived value being recomputed — which the
measurement contradicts directly: the calculation ran on every dependency change in every
variant tested. The shipped Lesson states the distinction in Core, restates it in the
comparison table's middle column, and makes it one of the three promises the API does not
make. Unit 6 should preserve it: the snapshot lesson explains *why* the consumer is spared
and must not re-teach this decision rule.

A first-draft ordering mistake is worth recording for the same reason E23-04 recorded
its two: the initial probe placed a direct-calculation reader and a derived reader in the
**same** recompose scope, so the direct read invalidated the shared scope and both readers
executed on every change — which reads as "`derivedStateOf` does nothing". It was the
measurement setup that was wrong, not the API. Separating the two readers into their own
scopes produced the result above. Comparative claims about invalidation are only meaningful
when the thing being compared is the only read in its scope.

### Semantic re-check of `compose_derived_state_threshold`

Re-read in full against the finished prose rather than against this plan's expectation.

| Question | Reasoning it requires | Does the finished Lesson teach it | Evidence |
| --- | --- | --- | --- |
| `compose_derived_state_threshold` | That `derivedStateOf` updates consumers only when the derived result changes, and that it is neither a threading nor a persistence API | **Yes, and past what the Question asks** | `lesson_derived_state` Core states that reading a derived value subscribes the scope to the *result* rather than the inputs; the Practical comparison table separates when the calculation runs from when the consumer runs; and the `COMMON_MISTAKE` callout names the three things the API does not do — prevent the calculation, eliminate recomposition, or make expensive computation affordable. The threading distractor is refuted twice over, once here and once in `lesson_work_outside_composition`, which states that neither memoization nor a change of layer moves work to another thread |

This plan asked E23-05 to teach the decision rule rather than the example, because the
Lesson uses the same scroll-threshold case the Question uses. It does: the canonical
snippet appears in Core, and the reasoning that transfers — name the inputs, name the
observed result, compare their change frequencies — is stated separately in Practical
together with the `fullName` counter-case and the measured trivial case, neither of which
the Question touches. A reader who understood the Lesson can answer the Question; a reader
who memorised the Question could not write the Lesson's Practical section.

**One candidate for E23-07, of the shape Units 3 and 4 produced.** The Question is levelled
`APPLIED` and is the only active Question on `compose_derived_state`, so all three Unit 5
Lessons and the whole of the Unit's practice rest on it. The Lessons now teach the
counter-cases, the capture failure, the key-comparison contract and the placement decision,
none of which is assessed anywhere. That is not a defect in the Question, which remains
sound and correctly answered; it is a coverage observation, and it makes GAP-U5-A and
GAP-U5-B more pressing rather than less.

### Assessment gaps confirmed unchanged

Both Unit 5 gaps stand, and teaching a concept does not close an assessment gap.

- **GAP-U5-A stands.** `lesson_remember_key_memoization` now teaches key-based invalidation
  with three measured failures — the missing key, the irrelevant key, and the mutated key
  object — and the `==` comparison contract that separates it from Strong Skipping's rule.
  Nothing active assesses any of it: `compose_derived_state`'s one Question is about
  `derivedStateOf`, and `compose_state`'s is about saved-state lifetimes.
- **GAP-U5-B stands, and is now the wider of the two.** `lesson_work_outside_composition`
  maps `compose_derived_state` as its only primary concept and carries
  `main_thread_performance`, `layered_architecture` and `use_cases` as supporting, so the
  Questions those Subtopics hold create no Unit 5 practice coverage — and none of them
  connects placement of work to composition in any case. The Lesson's most interview-useful
  claim, that moving work to a `ViewModel` moves responsibility rather than threads, is
  unassessed anywhere in the bank.

### Editorial decisions worth carrying forward

- **Two comparison rules are kept apart by name.** A `remember` key is compared with `==`;
  a Strong Skipping parameter comparison for an unstable argument is `===`. L5.1 says so in
  a `NOTE` rather than leaving the reader to notice, because Unit 4 has just spent five
  Lessons on the other rule and the transfer error is the obvious one to make.
- **Illustrative cost is labelled as illustrative.** Unit 5 reports execution counts, which
  are measurements of this project's toolchain, and reasons about cost, which is not
  measured. L5.3 says explicitly that nothing in the Unit claims how many milliseconds any
  of it takes and re-applies Unit 3's symptom / identified work / causal link standard to
  placement decisions.
- **The layering bridge stops at ownership.** L5.3 uses the UI-layer and domain-layer
  guidance to establish that business logic never lives in the UI layer, and immediately
  uses the same documentation's statement that the domain layer is optional and use cases
  should be added only when required to refuse the "therefore write a use case" conclusion.
  The four questions it leaves the reader with — cost, lifetime, reuse, responsibility —
  are the transferable part, and they are what Unit 8 should build on rather than restate.
- **The absolute claim is refused in both directions.** "All calculations must leave
  composition" is named and rejected beside the misplacement failure, because a Lesson that
  argues only one way produces a state holder whose job is string concatenation.


## Assessment gaps for E23-07

Substantive gaps only. There is no per-Lesson or per-level quota, and a gap here is a
candidate for authoring rather than an automatic defect. Each gap names the reasoning that is
missing, not a number.

| Gap | Unit / Lesson | Reasoning no ACTIVE Question assesses | Why it is substantive | Recommended action |
| --- | --- | --- | --- | --- |
| GAP-U2-A | Unit 2 / `lesson_observable_state`, `lesson_remember_composition_memory` | Distinguishing "observable" from "remembered": the four combinations of `remember` and `mutableStateOf` and what each does | This is the central confusion Unit 2 exists to resolve, and `compose_state`'s only active Question is about saved-state lifetimes | Add coverage in E23-07 |
| GAP-U2-B | Unit 2 / `lesson_observable_collections` | That mutating a collection held in state notifies nothing, and what the alternatives cost | No Question in any Topic connects collection mutation to a missing Compose update. `kotlin_readonly_list_not_immutable` and `data_class_copy_is_shallow` are Kotlin-side and supporting-only, so they create no Unit 2 practice | Add coverage in E23-07 |
| GAP-U3-A | Unit 3 / `lesson_recomposition_cost` | That recomposition is the normal operating mode and the cost is the work done during composition | Unit 3's two active Questions cover the definition and the scope rule only. `compose_recomposition_performance_001` sits in the `performance` Topic and is supporting-only here, so it must not be counted as Unit 3 coverage | Add coverage in E23-07 |
| GAP-U4-A | Unit 4 / `lesson_composable_identity` | Call-site identity outside lists — state discarded because the branch holding it stopped being composed, and preserved when one call site merely moves on screen | The one active identity Question is entirely lazy-list framed. Confirmed after authoring: the finished Lesson also teaches the recomposition-versus-re-entry distinction and the single-call-site counter-example, none of which is assessed | Add coverage in E23-07 |
| GAP-U4-B | Unit 4 / `lesson_stability_annotations` | That `@Stable` and `@Immutable` are promises the compiler trusts, and that a false promise is a correctness bug presenting as stale UI | Nothing in the bank assesses the annotations at all, and this is the Lesson with the highest interview signal in Unit 4. Confirmed after authoring as the widest gap in the Unit | Add coverage in E23-07; the strongest candidate of the three |
| GAP-U4-C | Unit 4 / `lesson_immutability_vs_stability` | The Compose consequence of read-only-but-not-immutable data, as opposed to the Kotlin fact — including that an assignment of an equal value records no change | The Kotlin fact is assessed (`kotlin_readonly_list_not_immutable`), but only as supporting context for this Lesson. The four supporting Kotlin Subtopics are pinned as supporting by a test, so they cannot become Unit practice by accident | Add coverage in E23-07, after GAP-U4-A and GAP-U4-B |
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

Read [Authoring outcomes for Units 2 and 3](#authoring-outcomes-for-units-2-and-3) first: it
records the semantic re-check of the two recomposition Questions against the finished prose,
confirms GAP-U2-A, GAP-U2-B and GAP-U3-A unchanged, and adds one new candidate — Applied
coverage of read placement, which `lesson_recomposition_scopes` now teaches well past what
the single FOUNDATION Question assesses.

Then read [Authoring outcomes for Unit 5](#authoring-outcomes-for-unit-5), which re-checks
`compose_derived_state_threshold` against the finished prose, confirms GAP-U5-A and
GAP-U5-B unchanged, and records that all three Unit 5 Lessons and the whole of that Unit's
practice rest on that single `APPLIED` Question.

Then read [Authoring outcomes for Unit 4](#authoring-outcomes-for-unit-4), which carries the
one item in this plan that is a possible Question defect rather than a coverage gap:
`compose_strong_skipping_instance_equality` asserts an outcome for a rebuilt equal `List`
that this repository's configured compiler does not produce. Re-run the measurement recorded
there before deciding anything, and treat its `FOUNDATION` level as a second, separate
question. E23-04 and E23-05 changed no Question.

The nine gap rows are the starting list. Re-read them against the finished prose before
authoring: a gap this plan predicted may have been answered by a Lesson that turned out deeper
than planned, and a new one may have appeared. Regenerating
`docs/content/question-bank-coverage.md` closes the staleness recorded above.

### For E23-08

The five Units read in blueprint order, and the concept boundaries in this document are what
the cross-Unit review should test: no concept is taught twice, each Unit's prerequisites are
satisfied by an earlier Unit or bridged in place, and no Lesson links to material that does not
exist.
