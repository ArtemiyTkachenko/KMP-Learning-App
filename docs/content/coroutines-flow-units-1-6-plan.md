# Coroutines and Flow Units 1–6 Authoring and Assessment Plan

## Purpose

`docs/content/coroutines-flow-learning-blueprint.md` maps the whole coroutines and Flow
subject. This document is the confirmed authoring plan for the six Units that epic E24
delivers — **6 Units, 29 Lessons** — reviewed against the taxonomy, the shipped Compose
Units, the current ACTIVE question bank, and current authoritative sources.

It exists so that E24-02 through E24-07 can start authoring without re-deciding identity,
scope, or concept ownership, and so that E24-08 has a traceable list of assessment gaps.

It is **not** a second blueprint. Objectives, depth layers and
Teach/Bridge/Reference/Exclude decisions stay in the blueprint; this document records the
decisions the blueprint left open and the review findings that would otherwise have to be
re-derived.

**Review date: 2026-09-10.** Everything in
[Source freshness](#source-freshness-and-technical-assumptions) was checked on that date
against the versions this repository is configured with.

## Scope confirmation

The merged epic assumes six instructional Units. **The six-Unit structure is unchanged.**
The audit found no pedagogical or taxonomy reason to split, merge, add or drop a Unit: the
26 `async_reactive` Subtopics divide cleanly along the six boundaries the epic describes,
and each boundary is a genuine change of mental model rather than a change of API family.

The **Lesson count is 29**, above the 24–25 the epic's planning discussion estimated. That
estimate was not a quota and the difference is deliberate:

- Unit 1 carries five Lessons because `Job`-as-handle and `CoroutineScope`-as-owner are
  related but not the same model, and a merged Lesson would have to cover `Job` states,
  `join`, the parent-child link, scope construction, the cancellation point, ad-hoc-scope
  failures and lifecycle scopes at once. Both halves are also separately required by
  E24-02's acceptance criteria.
- Unit 4 carries five because Flow builders and callback adaptation (`callbackFlow`,
  `awaitClose`) is a distinct mental model — making a hot external source look cold — and
  folding it into the cold-Flow Lesson would bury the leak that the builder exists to
  prevent.
- Unit 5 carries five because the epic's own framing asks for one Lesson per decision, and
  there are five decisions: filter by value or by time, combine current values or pair
  emissions, what happens to in-flight work, what may be lost when the collector is slow,
  and where a failure can be seen.
- Unit 6 carries five because `stateIn`/`shareIn` and the final abstraction choice are the
  two Lessons the epic exists to make possible, and neither can be folded into the
  `StateFlow` or `SharedFlow` Lesson without shrinking it to a slogan.

This issue is planning only. It introduces no production Lesson and no Question. Every
identity proposed here lives in documentation until the authoring issue that ships it.

## How the authoring issues use this document

| Issue | Reads |
| --- | --- |
| E24-02 | Unit 1 identities, objectives and boundaries; the Unit 1 rows of the semantic review; the suspension, structured-concurrency and scope entries in source freshness |
| E24-03 | Unit 2 identities and boundaries; the context, dispatcher and concurrency rows; the `Dispatchers.IO`, `Dispatchers.Main` and KMP entries in source freshness |
| E24-04 | Unit 3 identities and boundaries; the cancellation, exception and supervision rows; the `CoroutineExceptionHandler`, `NonCancellable`, `runInterruptible` and atomics entries in source freshness; **and the [timeout measurement](#what-withtimeout-does-to-non-cooperative-work)**, which corrects the natural way to phrase L3.2's Senior point |
| E24-05 | Unit 4 identities and boundaries; the Flow-fundamentals rows; the context-preservation and `callbackFlow` entries; **and [Reconciling the shipped `snapshotFlow` Lesson](#reconciling-the-shipped-snapshotflow-lesson) in full** |
| E24-06 | Unit 5 identities and boundaries; the operator, buffering and failure rows; the flattening, experimental-annotation and `catch` entries in source freshness |
| E24-07 | Unit 6 identities and boundaries; the hot-stream rows; the `StateFlow` conflation, `SharedFlow` buffering and `SharingStarted` entries in source freshness |
| E24-08 | [Assessment gaps](#assessment-gaps-for-e24-08) in full, re-checked against the finished Lessons, plus the [mapping corrections](#mapping-corrections-rather-than-gaps) and the semantic reviews each authoring issue recorded in its own outcomes section |
| E24-09 | [Handoff](#handoff) — sequencing, cross-Unit links, and the limitations recorded here |

Each authoring issue also reads the outcomes the previous ones recorded, so a finding is
re-checked rather than re-derived: [Authoring outcomes for Unit 1](#authoring-outcomes-for-unit-1)
is the first of those and is required reading for E24-03 and E24-08.

**Authoring status.** Unit 1 is authored and shipped by E24-02; see
[Authoring outcomes for Unit 1](#authoring-outcomes-for-unit-1). Unit 2 is authored and
shipped by E24-03; see [Authoring outcomes for Unit 2](#authoring-outcomes-for-unit-2).
Unit 3 is authored and shipped by E24-04; see
[Authoring outcomes for Unit 3](#authoring-outcomes-for-unit-3). Unit 4 is authored and
shipped by E24-05; see [Authoring outcomes for Unit 4](#authoring-outcomes-for-unit-4).
Unit 5 is authored and shipped by E24-06; see
[Authoring outcomes for Unit 5](#authoring-outcomes-for-unit-5). Unit 6 is authored in
production format by E24-07, pending review and merge; see
[Authoring outcomes for Unit 6](#authoring-outcomes-for-unit-6). **Every Unit this epic
plans is now authored**, so the next issue is E24-08.

**Two Lesson titles changed and are recorded here rather than left to drift.** L6.4 ships as
"Sharing Cold Flows with `stateIn` and `shareIn`" and L6.5 as "Choosing a Stream Abstraction
by Delivery Guarantees". Both identities, both positions and every mapping are unchanged; the
titles in the tables above and in the blueprint were updated in the same change. The reason is
editorial: the planned titles listed APIs where the Lessons are organised by the decision they
teach, which is the ordering Rule 5 of the authoring contract asks for.

---

## Identity conventions and proposed identities

The shipped Compose content establishes the convention and this plan follows it rather than
inventing one:

- **Unit id** — `unit_` plus the Unit title in snake case.
- **Lesson id** — `lesson_` plus the concept the Lesson owns, in snake case, short enough to
  read in a reference. Lesson ids are **not** numbered: the blueprint's `L1.1` labels are
  positional and would be wrong the moment a Lesson moves.
- Ids are stable once shipped. Renaming one breaks learner study-progress records, which are
  keyed by Lesson id (see `docs/architecture/study-progress.md`).
- Lesson ids must be unique across **all** Units, not just within one: `relatedLessonIds`
  names a Lesson without naming its Unit, and `LearningCurriculumValidator` rejects a
  duplicate (`DUPLICATE_LESSON_ID`).

Two deliberate departures, both following the precedent set by
`unit_identity_keys_and_stability`, whose title in full snake case was unreadable:

- Unit 1's title in snake case would be `unit_coroutine_fundamentals_and_structured_concurrency`
  (53 characters). The id drops "coroutine fundamentals" to `unit_coroutines_and_structured_concurrency`.
- Unit 2's would be `unit_coroutine_context_dispatchers_and_concurrent_work` (53). The id
  shortens to `unit_context_dispatchers_and_concurrency`.

Both Unit titles are unchanged, and both dropped terms are still taught in the Unit.

| Blueprint | Unit id | Unit title |
| --- | --- | --- |
| Unit 1 | `unit_coroutines_and_structured_concurrency` | Coroutine Fundamentals and Structured Concurrency |
| Unit 2 | `unit_context_dispatchers_and_concurrency` | Coroutine Context, Dispatchers and Concurrent Work |
| Unit 3 | `unit_cancellation_failure_and_coordination` | Cancellation, Failure and Coordination |
| Unit 4 | `unit_flow_fundamentals` | Flow Fundamentals |
| Unit 5 | `unit_flow_composition_timing_and_failure` | Flow Composition, Timing and Failure |
| Unit 6 | `unit_stateflow_sharedflow_and_hot_streams` | StateFlow, SharedFlow and Hot Streams |

All six Units take `async_reactive` as their home Topic.

| Blueprint | Lesson id | Lesson title | Primary | Supporting |
| --- | --- | --- | --- | --- |
| L1.1 | `lesson_suspension_and_blocking` | Suspension Is Not Blocking | `coroutine_fundamentals` | `coroutine_dispatchers`, `jvm_fundamentals`, `android_main_thread` |
| L1.2 | `lesson_coroutine_builders` | Starting Coroutines: `launch`, `async` and `runBlocking` | `coroutine_builders` | `coroutine_fundamentals`, `coroutine_scope`, `coroutine_parallelism`, `kotlin_lambdas` |
| L1.3 | `lesson_job_and_parent_child` | `Job`: the Handle That Carries Lifetime | `coroutine_jobs` | `coroutine_scope`, `coroutine_context`, `structured_concurrency` |
| L1.4 | `lesson_coroutine_scope_ownership` | `CoroutineScope` and Who Owns a Coroutine's Lifetime | `coroutine_scope` | `coroutine_jobs`, `lifecycle_coroutines`, `coroutine_leaks`, `viewmodel_lifecycle` |
| L1.5 | `lesson_structured_concurrency` | Structured Concurrency and What It Guarantees | `structured_concurrency` | `coroutine_jobs`, `coroutine_scope`, `coroutine_builders`, `coroutine_exceptions` |
| L2.1 | `lesson_coroutine_context` | `CoroutineContext` and What Children Inherit | `coroutine_context` | `coroutine_jobs`, `coroutine_dispatchers`, `structured_concurrency` |
| L2.2 | `lesson_dispatchers` | Dispatchers and Where Code Actually Runs | `coroutine_dispatchers` | `coroutine_context`, `android_main_thread`, `main_thread_performance`, `anr` |
| L2.3 | `lesson_with_context_and_main_safety` | `withContext` and Main-Safety | `coroutine_context_switching` | `coroutine_dispatchers`, `coroutine_fundamentals`, `structured_concurrency`, `repository_pattern` |
| L2.4 | `lesson_sequential_and_concurrent_work` | Sequential by Default, Concurrent on Purpose | `coroutine_parallelism` | `coroutine_builders`, `structured_concurrency`, `coroutine_dispatchers` |
| L3.1 | `lesson_cooperative_cancellation` | Cancellation Is Cooperative | `coroutine_cancellation` | `coroutine_jobs`, `structured_concurrency`, `kotlin_exceptions` |
| L3.2 | `lesson_cancellation_cleanup_and_timeouts` | Cleanup, `NonCancellable` and Timeouts | `coroutine_cancellation` | `kotlin_exceptions`, `coroutine_context_switching`, `coroutine_jobs` |
| L3.3 | `lesson_exception_propagation` | How a Coroutine Failure Travels | `coroutine_exceptions` | `coroutine_builders`, `coroutine_jobs`, `coroutine_cancellation`, `error_modeling` |
| L3.4 | `lesson_supervision_and_failure_isolation` | `SupervisorJob`, `supervisorScope` and the Limits of Isolation | `coroutine_supervision` | `coroutine_exceptions`, `coroutine_context`, `structured_concurrency`, `coroutine_jobs` |
| L3.5 | `lesson_shared_state_and_coordination` | Shared Mutable State and Choosing a Coordination Mechanism | `coroutine_parallelism` | `hot_vs_cold_streams`, `jvm_fundamentals`, `android_memory_model`, `coroutine_dispatchers` |
| L4.1 | `lesson_why_flow` | One Value or Many: Why `Flow` Exists | `flow_fundamentals` | `coroutine_fundamentals`, `hot_vs_cold_streams`, `repository_pattern`, `single_source_of_truth` |
| L4.2 | `lesson_cold_flows` | Cold Flows: Producer, Collector and Operators | `flow_fundamentals` | `flow_collection`, `flow_operators`, `kotlin_sequences` |
| L4.3 | `lesson_flow_collection_lifetime` | Collection Lifetime and Flow Cancellation | `flow_collection` | `coroutine_cancellation`, `coroutine_scope`, `lifecycle_coroutines`, `coroutine_jobs` |
| L4.4 | `lesson_flow_context_and_flow_on` | Context Preservation and `flowOn` | `flow_context` | `coroutine_context`, `coroutine_dispatchers`, `coroutine_context_switching`, `flow_errors` |
| L4.5 | `lesson_flow_builders_and_callback_adapters` | Flow Builders and Adapting Callback APIs | `flow_fundamentals` | `flow_context`, `coroutine_cancellation`, `flow_buffering`, `memory_leaks` |
| L5.1 | `lesson_transforming_and_filtering_flows` | Transforming and Filtering: by Value and by Time | `flow_operators` | `flow_fundamentals`, `kotlin_lambdas`, `kotlin_equality` |
| L5.2 | `lesson_combining_flows` | `combine` and `zip`: Current Values or Paired Emissions | `flow_operators` | `flow_fundamentals`, `stateflow`, `flow_collection` |
| L5.3 | `lesson_flattening_flows` | Flattening: Should New Input Cancel Old Work? | `flow_operators` | `coroutine_cancellation`, `coroutine_parallelism`, `flow_collection` |
| L5.4 | `lesson_flow_buffering_and_conflation` | When the Collector Cannot Keep Up | `flow_buffering` | `flow_collection`, `flow_context`, `coroutine_cancellation`, `flow_operators` |
| L5.5 | `lesson_flow_failure_and_completion` | `catch`, `retry` and `onCompletion` | `flow_errors` | `kotlin_exceptions`, `coroutine_cancellation`, `flow_operators`, `error_modeling` |
| L6.1 | `lesson_hot_and_cold_streams` | Hot and Cold: When Production Happens | `hot_vs_cold_streams` | `flow_fundamentals`, `flow_collection`, `stateflow`, `sharedflow` |
| L6.2 | `lesson_state_flow` | `StateFlow`: One Current Value | `stateflow` | `hot_vs_cold_streams`, `flow_collection`, `kotlin_equality`, `state_ownership` |
| L6.3 | `lesson_shared_flow` | `SharedFlow`: Replay, Buffering and Subscribers | `sharedflow` | `hot_vs_cold_streams`, `flow_buffering`, `stateflow`, `flow_collection` |
| L6.4 | `lesson_sharing_cold_flows` | Sharing Cold Flows with `stateIn` and `shareIn` | `flow_sharing` | `stateflow`, `sharedflow`, `coroutine_scope`, `lifecycle_coroutines` |
| L6.5 | `lesson_choosing_a_stream_abstraction` | Choosing a Stream Abstraction by Delivery Guarantees | `hot_vs_cold_streams` | `stateflow`, `sharedflow`, `flow_sharing`, `state_ownership` |

### Identity and mapping checks performed

Checked against the bundled production JSON on 2026-09-10, not from memory.

- **Uniqueness.** None of the 6 proposed Unit ids and 29 proposed Lesson ids collides with
  the 6 shipped Unit ids or the 21 shipped Lesson ids in `learning_curriculum.json`, or with
  each other.
- **Mapping validity.** All 39 distinct Subtopic ids used above exist in
  `initial_curriculum.json` and are `ACTIVE`. Twenty-three are `async_reactive` — every one
  in the Topic except `rxjava_fundamentals`, `flow_vs_rxjava` and `livedata`, all three
  deliberately unmapped. The other sixteen
  bridge to `kotlin_language` (`jvm_fundamentals`, `kotlin_lambdas`, `kotlin_exceptions`,
  `kotlin_sequences`, `kotlin_equality`), `performance` (`coroutine_leaks`,
  `main_thread_performance`, `anr`, `memory_leaks`, `android_memory_model`),
  `android_platform` (`android_main_thread`), `architecture` (`repository_pattern`,
  `error_modeling`, `single_source_of_truth`, `state_ownership`) and `lifecycle_navigation`
  (`viewmodel_lifecycle`) — the cross-Topic bridging Rule 3 of the authoring contract
  expects.
- **Overlap.** No Lesson lists the same Subtopic as both primary and supporting, which the
  validator rejects (`PRIMARY_SUPPORTING_SUBTOPIC_OVERLAP`).
- **Shared primaries.** Five Subtopics are primary in more than one Lesson:
  `coroutine_cancellation` (L3.1, L3.2), `coroutine_parallelism` (L2.4, L3.5),
  `flow_fundamentals` (L4.1, L4.2, L4.5), `flow_operators` (L5.1, L5.2, L5.3) and
  `hot_vs_cold_streams` (L6.1, L6.5). This is the contract's expected shape —
  "a single Subtopic is routinely touched by more than one Lesson at different depths" — and
  matches the shipped Compose Units, where `compose_state` is primary in three Lessons.
- **Forward references.** No `relatedLessonIds` value proposed here names a Lesson that does
  not yet ship. See [Cross-linking rules](#cross-linking-rules).
- **Taxonomy gaps.** Six concepts have no exact Subtopic and are recorded in the blueprint
  rather than given invented IDs. None of them blocks authoring; each is either semantically
  covered by a broader Subtopic that the bank already uses the same way, or stays supporting
  prose.

---

## Lesson objectives, prerequisites and boundaries

Objectives here restate the blueprint's in one line and add what the review had to settle:
the reasoning a learner must be able to demonstrate, where each prerequisite is taught or
bridged, and the boundary that keeps the Lesson from absorbing its neighbour.

### Unit 1 — Coroutine Fundamentals and Structured Concurrency (`unit_coroutines_and_structured_concurrency`)

**Prerequisites:** none inside this subject. Kotlin lambdas and higher-order functions are
bridged in place; no learning Unit teaches Kotlin and this epic does not start one.

#### `lesson_suspension_and_blocking` (L1.1)

- **Objective:** distinguish suspension from blocking, and from threading.
- **Demonstrable reasoning:** given a suspending function that calls a blocking API, say
  whether calling it from the main thread is safe, and why the `suspend` keyword does not
  answer that question.
- **Prerequisites:** the idea of a thread, bridged in two sentences. No coroutine
  prerequisite — this is the entry point of the path.
- **Boundary:** dispatchers are named as "the thing that decides where it runs" and deferred
  whole to L2.2. The compiler transform is excluded; "a suspended coroutine is a stored
  continuation" is the only mechanism sentence allowed.

#### `lesson_coroutine_builders` (L1.2)

- **Objective:** choose a builder from the intent, and say what it returns.
- **Demonstrable reasoning:** given `async { save() }` whose result is never consumed,
  explain why `launch` states the intent better; and given `runBlocking` on the main thread,
  predict the consequence.
- **Prerequisites:** L1.1. Kotlin lambdas, bridged where the trailing-lambda shape needs it.
- **Boundary:** `async` appears only as the builder that returns a value. Intentional
  concurrency is L2.4 and `async` failure semantics are L3.3, both named and deferred.
  `withContext` is deliberately absent from the builder list.

#### `lesson_job_and_parent_child` (L1.3)

- **Objective:** use a `Job` to reason about a coroutine's progress and its relatives.
- **Demonstrable reasoning:** given a parent whose own body has finished while a child is
  still running, say whether the parent is complete, and name the state it is in.
- **Prerequisites:** L1.2 — the builder is where a `Job` comes from.
- **Boundary:** cancellation is named as something a `Job` carries and is deferred **whole**
  to Unit 3. This Lesson must not begin teaching cooperative cancellation, which is the
  natural drift and would leave L3.1 with nothing to own.

#### `lesson_coroutine_scope_ownership` (L1.4)

- **Objective:** name the owner of any coroutine, and identify code where nobody owns it.
- **Demonstrable reasoning:** given a class that builds its own `CoroutineScope` and never
  cancels it, describe the failure concretely — work continuing for a screen that is gone,
  captured references retained, failures with no owner — rather than calling it bad style.
- **Prerequisites:** L1.3; Android lifecycle ownership (`viewmodel_lifecycle`), bridged in
  place because no learning Unit teaches it and this Lesson must not reproduce that
  curriculum.
- **Boundary:** `viewModelScope` is an example of an owned scope, not a ViewModel-architecture
  lesson. `SupervisorJob` appears in the scope-construction idiom and is explicitly deferred
  to L3.4 rather than half-explained. WorkManager is one sentence.

#### `lesson_structured_concurrency` (L1.5)

- **Objective:** state what the model guarantees and what it does not.
- **Demonstrable reasoning:** given a suspending function that starts work and returns before
  it finishes, explain what the caller has lost, and rewrite it with `coroutineScope`.
- **Prerequisites:** L1.3 and L1.4.
- **Boundary:** failure propagation is stated as a fact and owned by L3.3; supervision is
  named as the exception and owned by L3.4. `supervisorScope` is not introduced in this Unit.
  The `coroutineScope`-versus-`withContext` distinction is deferred to L2.3.

### Unit 2 — Coroutine Context, Dispatchers and Concurrent Work (`unit_context_dispatchers_and_concurrency`)

**Prerequisite:** Unit 1, which E24-02 ships first.

#### `lesson_coroutine_context` (L2.1)

- **Objective:** read a coroutine's effective context off the code that launched it.
- **Demonstrable reasoning:** for `launch(Dispatchers.IO) { }` inside a scope on `Main`, name
  where each element of the resulting context came from, and say which element is never
  simply inherited.
- **Prerequisites:** L1.3 — the `Job` is the element that carries the structure.
- **Boundary:** `CoroutineExceptionHandler` is named as a context element and deferred to
  L3.3; `SupervisorJob` to L3.4. The `launch(SupervisorJob())` trap is **split on purpose**:
  the re-parenting half is a context fact taught here, the supervision half is L3.4. Neither
  Lesson teaches the whole of it alone, and both must point at the other.

#### `lesson_dispatchers` (L2.2)

- **Objective:** choose a dispatcher from the shape of the work.
- **Demonstrable reasoning:** given a blocking file read, justify `Dispatchers.IO` over
  `Dispatchers.Default` in terms of pool sizing rather than priority; and given
  `withContext(Dispatchers.IO)` inside code already on `Default`, say what actually happens.
- **Prerequisites:** L2.1; main-thread and ANR concepts, bridged.
- **Boundary:** injection is introduced as a design point and its testing motivation is named
  and deferred to E31. `Dispatchers.Unconfined` and custom dispatchers are Reference.
  **KMP caveat is mandatory** — see [Dispatchers and KMP](#dispatchers-and-kotlin-multiplatform).

#### `lesson_with_context_and_main_safety` (L2.3)

- **Objective:** decide where a context switch belongs.
- **Demonstrable reasoning:** given a suspending network call wrapped in
  `withContext(Dispatchers.IO)` by its caller, decide whether the wrapper does anything, and
  say who is responsible for main-safety.
- **Prerequisites:** L2.2; L1.5, because `withContext` is contrasted with `coroutineScope`.
- **Boundary:** `withContext` creates a lexical child scope but does not make the caller
  proceed concurrently, unlike `launch`/`async` — that contrast with L1.2 is the Lesson's
  job. Layered architecture is bridged, not taught.

#### `lesson_sequential_and_concurrent_work` (L2.4)

- **Objective:** decide whether two operations should overlap, and write the version that does.
- **Demonstrable reasoning:** given `async { a() }.await()` then `async { b() }.await()`,
  predict that the calls run sequentially and explain what to change; then say whether the
  overlap is worth it for the problem at hand.
- **Prerequisites:** L1.2, L1.5.
- **Boundary:** `async` failure semantics are named — a failing `async` child still cancels
  its parent whether or not anyone awaits it — and owned by L3.3. `select` is excluded.

### Unit 3 — Cancellation, Failure and Coordination (`unit_cancellation_failure_and_coordination`)

**Prerequisites:** Units 1 and 2.

#### `lesson_cooperative_cancellation` (L3.1)

- **Objective:** predict whether a cancelled coroutine will actually stop.
- **Demonstrable reasoning:** given a CPU-bound loop with no suspension points, say what
  happens on cancellation and name the three ways to make it cooperative; and given a broad
  `catch (e: Exception)`, say what it does to cancellation.
- **Prerequisites:** L1.3 and L1.5; Kotlin exceptions, bridged.
- **Boundary:** cleanup, `NonCancellable` and timeouts are L3.2. The cancellation-versus-
  failure distinction is one sentence here and owned by L3.3.

#### `lesson_cancellation_cleanup_and_timeouts` (L3.2)

- **Objective:** release resources correctly on cancellation and express a deadline through
  the same model.
- **Demonstrable reasoning:** given a suspending `close()` in a `finally` block that silently
  does nothing, explain why and fix it; and given `withTimeout` around non-cooperative code,
  say what the deadline actually bounds — which is neither the work nor the caller's wait.
- **Prerequisites:** L3.1.
- **Boundary:** `runInterruptible` and thread interruption are **JVM-only** and must be
  labelled as such — this is a Kotlin Multiplatform repository. `NonCancellable` must be
  taught with its danger attached rather than as a tool for finishing cancelled work.
- **Accuracy requirement:** do not write that `withTimeout` abandons non-cooperative work and
  lets the caller proceed. It does not, and the measurement in
  [What `withTimeout` does to non-cooperative work](#what-withtimeout-does-to-non-cooperative-work)
  shows why, including the case where the block returns a **successful result** long after its
  deadline.

#### `lesson_exception_propagation` (L3.3)

- **Objective:** given a failing coroutine, name everything else that fails.
- **Demonstrable reasoning:** decide, for `launch` and for `async`, where the exception can be
  observed; and say why a `CoroutineExceptionHandler` installed on a child does nothing.
- **Prerequisites:** L1.5, L2.1, L3.1.
- **Boundary:** supervision is the exception to every rule stated here and is owned by L3.4.
  Result-type error modelling is one bridging pointer to `architecture`.

#### `lesson_supervision_and_failure_isolation` (L3.4)

- **Objective:** isolate independent failures without hiding them.
- **Demonstrable reasoning:** given a nested `launch` inside a `supervisorScope` whose inner
  child fails, predict which coroutines are cancelled; and explain why
  `launch(SupervisorJob())` does not supervise anything.
- **Prerequisites:** L2.1 (context re-parenting), L3.3.
- **Boundary:** completes the trap begun in L2.1. Must not present supervision as error
  suppression — the failure mode is a reader who adds `SupervisorJob` to silence a crash.

#### `lesson_shared_state_and_coordination` (L3.5)

- **Objective:** identify concurrently accessed state and choose a mechanism deliberately.
- **Demonstrable reasoning:** given a counter incremented from many coroutines on a
  multi-threaded dispatcher, explain the wrong total; then justify a choice among an atomic,
  confinement and a `Mutex` for a given shape of state.
- **Prerequisites:** L2.2 (a multi-threaded dispatcher is what makes the race possible), L2.4.
- **Boundary:** this is the Unit's **bounded** coordination treatment and must stay a
  decision Lesson. `Semaphore`, `actor` and the full `Channel` API are Reference; the
  `Channel` contrast is one paragraph and the real comparison is L6.5. Multiplatform atomics
  availability is a source-sensitive point — see
  [Atomics in common code](#atomics-in-common-code).

### Unit 4 — Flow Fundamentals (`unit_flow_fundamentals`)

**Prerequisites:** Units 1–3. Collection lifetime is scope lifetime, Flow cancellation is
coroutine cancellation, and exception transparency is a constraint on the exception model —
all three are why this Unit is fourth rather than first.

#### `lesson_why_flow` (L4.1)

- **Objective:** decide whether an API should return one value or produce values over time.
- **Demonstrable reasoning:** given a screen that must reflect later local writes, explain
  why a one-shot suspending call cannot do it, and what a Flow-returning API costs the caller.
- **Prerequisites:** L1.1; repository API shape, bridged to `architecture`.
- **Boundary:** hot streams are named as something that exists and deferred whole to Unit 6.
  `LiveData`, RxJava and callbacks get one sentence as the alternatives Flow replaced.

#### `lesson_cold_flows` (L4.2)

- **Objective:** say exactly what runs, and when, for a given chain.
- **Demonstrable reasoning:** given a `flow { }` with logging and no terminal operator,
  predict that nothing prints; and predict what two collectors of the same flow each receive.
- **Prerequisites:** L4.1; Kotlin sequences, bridged as the analogy and its limit.
- **Boundary:** operators appear only as "intermediate versus terminal". `flowOn` is L4.4.
  **This is the canonical treatment of coldness** that the shipped `lesson_snapshot_flow`
  currently bridges in four bullets — see
  [Reconciling the shipped `snapshotFlow` Lesson](#reconciling-the-shipped-snapshotflow-lesson).

#### `lesson_flow_collection_lifetime` (L4.3)

- **Objective:** name the coroutine a collection runs in, and what will end it.
- **Demonstrable reasoning:** given a cold Flow wrapping a long paged read whose collector is
  cancelled, say what happens to the producer and when; and say what
  `flow.onEach { }.launchIn(scope)` actually does.
- **Prerequisites:** L1.4, L3.1, L4.2.
- **Boundary:** lifecycle-aware collection (`repeatOnLifecycle`, `flowWithLifecycle`) and
  Compose collection (`collectAsStateWithLifecycle`) are one bridging sentence each and are
  owned by E25 and the Compose blueprint's Unit 8, *Observable State Collection and
  Lifecycle* (renumbered by E25-01; see `compose-units-7-12-plan.md`).

#### `lesson_flow_context_and_flow_on` (L4.4)

- **Objective:** say which part of a chain runs in which context.
- **Demonstrable reasoning:** given a `flow { }` whose body wraps itself in
  `withContext(Dispatchers.IO)`, predict the runtime failure and give the correct form;
  and say what `flowOn` does and does not affect.
- **Prerequisites:** L2.1, L2.3, L4.2.
- **Boundary:** exception transparency is introduced here as the same principle applied to
  failures; the operators that implement it are L5.5. `buffer`'s interaction with `flowOn` is
  named and deferred to L5.4.

#### `lesson_flow_builders_and_callback_adapters` (L4.5)

- **Objective:** turn an existing producer into a Flow without leaking it.
- **Demonstrable reasoning:** given a `callbackFlow` whose block returns after registering a
  listener, say what the builder does about it and why that is better than the alternative;
  and choose between `flow`, `channelFlow` and `callbackFlow` for a given producer.
- **Prerequisites:** L4.3, L4.4.
- **Boundary:** the `Channel` behind `channelFlow` is Reference — one sentence explaining why
  emission from several coroutines is legal here. `produceIn` is excluded.

### Unit 5 — Flow Composition, Timing and Failure (`unit_flow_composition_timing_and_failure`)

**Prerequisite:** Unit 4 in full. Every Lesson here assumes coldness, collection lifetime and
context preservation are secure.

#### `lesson_transforming_and_filtering_flows` (L5.1)

- **Objective:** choose between filtering on value and filtering on time.
- **Demonstrable reasoning:** given a search field, explain what `debounce(300)` and
  `distinctUntilChanged()` each do and why both are frequently wanted.
- **Prerequisites:** L4.2.
- **Boundary:** the operator list stops at the five that carry a decision. `take`, `drop`,
  `onStart`, `scan` and `fold` are a short Reference table. No operator whose behaviour is
  predictable from its name earns a section.

#### `lesson_combining_flows` (L5.2)

- **Objective:** decide between latest-of-each and matched pairs.
- **Demonstrable reasoning:** given a user flow and a settings flow feeding one screen state,
  choose `combine` and say what `zip` would do instead; and predict what happens before both
  sources have emitted once.
- **Prerequisites:** L5.1.
- **Boundary:** `combineTransform` and the higher-arity overloads are Reference.

#### `lesson_flattening_flows` (L5.3)

- **Objective:** choose a flattening strategy from what should happen to in-flight work.
- **Demonstrable reasoning:** given search-as-you-type, choose `flatMapLatest` and say what it
  does to the request already running; then say when `flatMapConcat` or `flatMapMerge` would
  be right instead.
- **Prerequisites:** L3.1 (cancelling an inner flow is coroutine cancellation), L5.1.
- **Boundary:** all three carry `@ExperimentalCoroutinesApi` in the configured version and the
  Lesson must say so — see [Experimental operator status](#experimental-operator-status).

#### `lesson_flow_buffering_and_conflation` (L5.4)

- **Objective:** decide what may be lost when the producer outpaces the collector.
- **Demonstrable reasoning:** given a fast producer and a slow collector, predict which values
  `conflate` drops and what `collectLatest` abandons, and say which is safe for a given
  side effect.
- **Prerequisites:** L4.3, L4.4.
- **Boundary:** the default is backpressure, and that must be established before any operator
  is introduced — otherwise buffering reads as a performance trick. `BufferOverflow` modes
  beyond conflation are Reference.

#### `lesson_flow_failure_and_completion` (L5.5)

- **Objective:** place failure handling where it can see the failure.
- **Demonstrable reasoning:** given `.catch { }.collect { }` where the `collect` block throws,
  say whether `catch` fires and give both fixes; and say what `onCompletion` observes.
- **Prerequisites:** L3.1, L3.3, L4.4.
- **Boundary:** `retry` with backoff is a worked example, not a section on backoff policy.
  Representing an expected failure as a value in the stream is one bridging pointer.

### Unit 6 — StateFlow, SharedFlow and Hot Streams (`unit_stateflow_sharedflow_and_hot_streams`)

**Prerequisites:** Units 4 and 5. Unit 1's ownership model is assumed throughout, because
sharing moves an upstream's lifetime onto a scope.

#### `lesson_hot_and_cold_streams` (L6.1)

- **Objective:** classify a stream by when it produces and what a late subscriber gets.
- **Demonstrable reasoning:** given the same source expressed both ways, say what a second,
  later collector receives from each, and what happens to values emitted while nobody was
  subscribed.
- **Prerequisites:** L4.2, L4.3.
- **Boundary:** `StateFlow` and `SharedFlow` are named as the two implementations and taught
  in L6.2 and L6.3. Links backwards to `lesson_cold_flows` and to the shipped
  `lesson_snapshot_flow`, whose state-versus-events argument this Lesson reuses rather than
  restates.
- **Accuracy requirement:** this Lesson owns **production lifetime only**. Retention is a
  separate axis owned by L6.2 and L6.3, and start/stop policy is a third owned by L6.4.
  Neither "a hot flow discards values emitted with no subscribers" nor "a hot producer keeps
  its upstream open regardless" is true in general — the first holds only at `replay = 0`,
  and the second is contradicted by `WhileSubscribed`. Stating either as a property of
  hotness would be corrected by each of the next three Lessons in turn.

#### `lesson_state_flow` (L6.2)

- **Objective:** predict exactly which values a `StateFlow` collector observes.
- **Demonstrable reasoning:** given an assignment of a value equal to the current one, say
  that nothing is emitted and why; and given a slow collector under fast updates, say what it
  sees.
- **Prerequisites:** L6.1; Kotlin equality, bridged, because conflation is `Any.equals`.
- **Boundary:** screen-state modelling, `UiState` hierarchies and ViewModel design are owned
  by the architecture curriculum; E25-01 moved `UiState` modelling and ViewModel design out
  of the Compose blueprint, which now bounds its screen-state material to Unit 7. `LiveData`
  is one Reference row.

#### `lesson_shared_flow` (L6.3)

- **Objective:** state what a `SharedFlow` guarantees a subscriber will receive.
- **Demonstrable reasoning:** given `replay = 0` and a subscriber that arrives late, say what
  it receives; and given an unbuffered shared flow, say what `emit` does with and without
  subscribers.
- **Prerequisites:** L6.1, L6.2, L5.4.
- **Boundary:** must not reduce to "SharedFlow is for events". The delivery consequence of
  each configuration is the Lesson.

#### `lesson_sharing_cold_flows` (L6.4)

- **Objective:** convert a cold upstream into a shared one and say what starts and stops it.
- **Demonstrable reasoning:** given `stateIn(scope, WhileSubscribed(5_000), initial)`, say
  what the 5,000 controls and why that figure is chosen; and choose between `Eagerly` and
  `WhileSubscribed` for a stated resource constraint.
- **Prerequisites:** L1.4 (the scope is what the upstream's lifetime moves to), L6.2, L6.3.
- **Boundary:** the 5-second figure is a configuration-change heuristic on Android, not a
  rule. This repository's own state holders use `Eagerly`, which is a legitimate app-lifetime
  choice and a useful contrast case. Custom `SharingStarted` is Reference.

#### `lesson_choosing_a_stream_abstraction` (L6.5)

- **Objective:** pick the abstraction from the delivery guarantee the problem needs.
- **Demonstrable reasoning:** decide, for a stated scenario, among a plain value, a cold
  Flow, a `StateFlow`, a `SharedFlow` and a `Channel`, and name the property that decides it.
- **Prerequisites:** every Lesson of Units 4 and 6.
- **Boundary:** this is the epic's bounded `Channel` treatment. A full Channel curriculum
  stays excluded. One-off UI events in a Compose screen are E25's and the architecture
  curriculum's decision; this Lesson supplies the stream semantics and stops.

---

## Authoring outcomes for Unit 1

Added by E24-02 after the five Lessons were written. Everything below was checked against the
artifacts this repository resolves or executed against them; nothing here is recalled.

### What did not change

Every proposed Unit id, Lesson id, title, authored order and primary/supporting mapping for
Unit 1 in the [identity tables](#identity-conventions-and-proposed-identities) shipped
verbatim. No Lesson boundary moved, no Lesson split or merged, and no blueprint correction was
required. All five Lessons carry Core, Practical and Senior depth, and each runs 1,188–1,426
words, inside the range the 21 shipped Compose Lessons occupy.

The Unit is appended after `unit_snapshot_fundamentals`, so the authored order of the whole
document is the six Compose Units followed by this one. List position is the ordering contract
for a publisher-owned document, and it is also what `ContinueLearningPolicy` walks, so the
coroutines path now follows the Compose path for a learner who studies straight through.

### Forward references

No Lesson links forward. `relatedLessonIds` are backward-only and stay inside the Unit —
`lesson_suspension_and_blocking` has none, and each later Lesson names only Lessons above it.
Nothing links into the shipped Compose Units: the plan's useful backward links
(`lesson_snapshot_flow`, `lesson_work_outside_composition`) belong to Units 2 and 4, and
inventing one here would have been a link the material does not earn.

Everything the Unit defers is prose naming the Unit that owns it, never an id: dispatchers and
`withContext` to Unit 2, cancellation and supervision and `async` failure semantics to Unit 3.
`supervisorScope` is never named. `SupervisorJob()` appears once, inside the scope-construction
idiom in `lesson_coroutine_scope_ownership`, and is explicitly set aside there as failure
propagation the failure Unit teaches.

### Corrections made during review

Three claims in the first draft were over-strong and were corrected before the Unit shipped. They
are recorded because each is a claim a later Unit could easily reintroduce.

| Draft claim | Why it was wrong | What ships |
| --- | --- | --- |
| L1.5: "Every coroutine has a parent" | A scope carrying no `Job` starts **root** coroutines, and a builder's context can re-parent one, so universal parenthood is not a structured-concurrency guarantee. It also contradicted L1.4's own `GlobalScope` material | The guarantee is stated as "a coroutine launched in a scope becomes a child of that scope's job", followed by a paragraph naming the root-coroutine exception, quoting the `GlobalScope` KDoc, and pointing re-parenting at Unit 2 |
| L1.1: a coroutine that never suspends "never reaches a moment at which anything about it can be acted on from outside" | Cancellation is observed at a cancellable suspension point **or** at an explicit check, so suspension is not the only route. The cancellation page says exactly that | The paragraph now names cancellation as cooperative and says a coroutine that neither suspends nor looks runs to the end. The check APIs are still not named — that boundary is unchanged |
| L1.5: "If a scope was cancelled, nothing it started is still running" | `cancel()` requests cancellation and returns; children may still be finishing. The stopped-work guarantee belongs to completion, not to the request | The sentence separates the two: the request reaches every descendant, and the scope's job completing is the moment nothing is running |
| L1.3: the parent-child link "is not something you configure ... it happens automatically, every time" | A builder given an explicit `Job` re-parents that coroutine out of the launching scope — the behaviour the ACTIVE `coroutine_supervisor_job_child_context_noop` documents. Parentage is context inheritance by default, not a law | The rule is stated as inheritance from the starting context, with one clause naming the override and deferring it to Unit 2. The trap itself stays split across L2.1 and L3.4 as this plan assigns it |
| L1.5 (introduced by the first correction above): root coroutines have "none of the four guarantees" | An overcorrection. A root has no parent, but it is still the top of a structure: it waits for its own children, cancelling it cancels them, and their failures reach it. Only attachment to an owner and upward propagation are absent | The paragraph now separates the two halves explicitly — the last three guarantees hold inside a root's subtree, the first one and everything downstream of it does not |
| L1.4: a coroutine's lifetime "is the lifetime of the scope it was launched from", and the owner is "whoever calls `cancel()`" | A child normally completes long before its scope does, so the scope **bounds** the lifetime rather than equalling it; and a lexical scope ends by completing, with nobody calling `cancel()` | The sentence says bounded, notes that most work finishes sooner, and gives the owner two forms: whoever cancels a constructed scope, or the end of the block for a lexical one |
| L1.1: "The keyword is the promise" of main-safety | Read as written it reinstates the misconception the Lesson exists to remove, and it contradicts the Lesson's own `readSettings` example two paragraphs above. Nothing enforces main-safety; Android's guidance is a convention about whose job it is, not a semantic property of `suspend` | The paragraph names it as a convention, says explicitly that `readSettings` breaks it and compiles, and closes on the reading habit: a signature says what the author was supposed to do, never what they did |
| L1.4: "a coroutine's ancestry is the only 'somebody' the runtime knows about" for a failure | False as a causal model, and reporting is not L1.4's to teach. `async` retains a failure in its `Deferred`; a root or supervised `launch` can report through a handler installed in its own context or through the platform's uncaught path; an owned `viewModelScope` coroutine can still surface an uncaught failure | The paragraph is reduced to a diagnosability claim — an un-owned failure surfaces detached from any lifecycle — and explicitly defers where a failure travels, what can intercept it, and supervision to Unit 3. The matching Practical bullet and interview callout were reworded the same way |

One smaller correction in the same pass: L1.4 said "a scope is a `CoroutineContext`". `CoroutineScope`
is an interface declaring one property, `coroutineContext`, and no functions, so the Lesson now says a
scope *holds* a context rather than being one. `GlobalScope`'s KDoc was added to L1.5's Sources for the
quotation it now carries.

The root-coroutine claims were executed rather than reasoned about, in the same style as the table
below. Against a jobless scope standing in for `GlobalScope`: the launched job's `parent` was `null`;
while its child ran it reported `StandaloneCoroutine{Completing}` with one child and `join()` waited
198 ms for that child; and cancelling the root left the child's completion flag `false`. Separately,
`coroutineScope`'s job **is** a child of the calling coroutine's job — which is why the Lesson now
disambiguates the two senses of "root" the documentation uses.

None of these changed an identity, a mapping, an order, or a Lesson boundary.

### Claims that were executed rather than reasoned about

A throwaway JVM test probed each of these against the resolved `kotlinx-coroutines-core:1.11.0`
and was then deleted. The numbers are quoted in the Lessons as measurements, not as guarantees.

| Claim a Lesson makes | Measured result |
| --- | --- |
| A parent whose body has finished is not complete while a child runs (L1.3) | At the 100 ms mark: `isActive` true, `isCompleted` false, one child, and the job printed as `StandaloneCoroutine{Completing}`; after `join()`, `isCompleted` true |
| `Deferred` is a `Job` (L1.3) | `d is Job` returned true for an `async` result |
| A blocking call inside a coroutine holds its thread (L1.1) | On a single-thread dispatcher, two coroutines running `Thread.sleep(300)` took 607 ms; two running `delay(300)` took 310 ms |
| A coroutine that never suspends monopolises its dispatcher thread (L1.1) | On a single-thread dispatcher, a 250 ms busy loop ran to completion before a second coroutine launched onto the same dispatcher started at all |
| A detached `launch` returns to the caller before the work finishes (L1.4, L1.5) | The suspending function returned after 5 ms for 300 ms of work, and the work completed normally afterwards; the `coroutineScope` version of the same body returned after 306 ms |
| `runBlocking` blocks its caller (L1.2) | `runBlocking { delay(300) }` returned to its caller after 306 ms |
| A scope's `Job` is the parent of what it launches (L1.4) | `scope.coroutineContext[Job] === childJob.parent` held for a scope built with the `CoroutineScope(...)` constructor |

### Source decisions

The [restructured guide](#the-coroutines-documentation-has-been-restructured) warning was
honoured: `coroutines-basics.html` was re-read rather than recalled, and it is still dated
07 September 2026. Every claim the Unit makes about suspension, builders, `runBlocking`, the
parent-child hierarchy and `coroutineScope` is quoted from it or from the API reference, and
no secondary source is cited anywhere in the Unit.

Two pages were added to the plan's verified list because Unit 1 depends on detail the guide
does not carry:

| Claim | Source |
| --- | --- |
| A `Job` is "a cancellable thing with a lifecycle that concludes in its completion"; the six-state table; `join()` "suspends the coroutine until this job is complete" and "resumes normally ... for any reason"; the completing state "waits ... for all its children to complete" | <https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-job/> |
| `Deferred` is "a non-blocking cancellable future" with "the same state machine as `Job`" | <https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-deferred/> |
| `delay` delays "without blocking a thread" | <https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/delay.html> |
| A scope "allows managing the lifecycles of several coroutines simultaneously"; the `coroutineContext[Job] === childJob.parent` convention; "if a scope is cancelled, all coroutines in it are cancelled too"; the constructor is "for entity lifecycles" *with required `cancel()` call*; `GlobalScope` has "the lifetime of the whole application" and "it is easy to misuse it" | <https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-scope/> |
| `coroutineScope`'s job "completes when both the block and all the coroutines launched in the scope complete. Only then can the `coroutineScope` call return a value" | <https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/coroutine-scope.html> |
| `runBlocking` is "designed to bridge regular blocking code ... in `main` functions, in tests, and in non-`suspend` callbacks"; calling it from a suspending function is "redundant and blocks the thread, avoid this" | <https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/run-blocking.html> |

`viewModelScope`'s cancellation contract is quoted from
<https://developer.android.com/topic/libraries/architecture/coroutines>, which is the page that
states it; the best-practices page is used for guidance — main-safety as the callee's
obligation, and injecting a longer-lived scope rather than using `GlobalScope` — and for
nothing semantic.

**KMP honesty.** `Thread.sleep` and the thread-memory comparison are the only JVM-specific
material in the Unit and both are labelled as such where they appear. No example names a
dispatcher constant, so nothing in the Unit assumes a platform that supplies one.

### Semantic review of the Questions this Unit now reaches

All seven ACTIVE Questions reachable through Unit 1's five primary concepts were re-read in
full against the finished prose, not against the mappings.

| Question | Verdict against the shipped Lessons |
| --- | --- |
| `coroutine_fundamentals_001` | Answerable from L1.1's Core. Its key — suspend without blocking the thread and resume later — and its three distractors (a dedicated pool, resuming on the same thread, callable from ordinary code) are each addressed explicitly |
| `coroutine_run_blocking_main_thread` | Answerable from L1.2's Practical, which predicts the frozen main thread from the definition rather than asserting a rule. The mapping drift the plan recorded is unchanged: the Subtopic reaches L1.1 while the reasoning lives in L1.2, and both are in this Unit, so Unit practice is unaffected |
| `launch_vs_async_unawaited_result` | Answerable from L1.2's Practical, which is built on the same `async { save() }` case. The Lesson argues from intent rather than restating the option wording |
| `coroutine_scope_job_ownership` | Answerable from L1.4's Core, which establishes the scope's `Job` as the parent of what it launches and the owner as whoever cancels it |
| `structured_concurrency_001` | Answerable from L1.5. Its three distractors — shared dispatcher, serialised children, restarted child — are each rejected in the Senior comparison, argued from the lifetime/scheduling distinction rather than echoed |
| `parent_cancellation_propagates_children` | **Still not answerable from this Unit, by design.** Its distractors need `cancelAndJoin` and `NonCancellable`, which are Unit 3 material and stay out of L1.3. The Lesson names cancellation as something the structure carries and stops there. This is the mismatch the plan recorded, unchanged and untouched — see [mapping corrections](#mapping-corrections-rather-than-gaps) |
| `coroutine_async_exception_surfaces_at_await` | **Still not answerable from this Unit, by design.** It needs failure propagation from a dropped `Deferred` and how supervision changes it. L1.2 says one sentence — `async` stores its outcome, including a failure, in the `Deferred` — and defers the rest. Recorded, not fixed |

Neither mismatch was fixed here and no Lesson was bent to make either answerable. E24-08 owns
both re-maps.

### The known gaps were not filled

GAP-U1-A, GAP-U1-B and GAP-U1-C are unchanged: E24-02 authored no Question. What did change is
that the reasoning each gap describes is now taught, so E24-08 has something to assess against.

| Gap | Where the reasoning now lives |
| --- | --- |
| GAP-U1-A — applying suspension-versus-blocking to real code | L1.1's Practical: the paired `readSettings`/`fetchArticles` example, the three-questions list, and the single-thread measurement |
| GAP-U1-B — the concrete failure of an un-owned scope | L1.4's Practical: the `SearchController` that never cancels, the five named consequences, and the 5 ms-versus-306 ms measurement |
| GAP-U1-C — `Job` as a handle | L1.3: `join`/`joinAll`, the measured completing state, the state table, and `Deferred` as a `Job` with a result |

### Unresolved questions, unchanged

None of the plan's four [unresolved questions](#unresolved-questions) is about Unit 1, and none
was answered here. The one page Unit 1 depends on that could be rewritten during the epic's
authoring window is `coroutines-basics.html`; it was current on 2026-09-10.

---

## Authoring outcomes for Unit 2

E24-03 authors `unit_context_dispatchers_and_concurrency` immediately after Unit 1 in
`learning_curriculum.json`. The four Lessons retain their confirmed IDs, titles, order
and exact primary/supporting mappings: context, dispatchers, withContext/main-safety,
then sequential/concurrent work. Each has an objective in its summary, Core, Practical
and Senior sections, examples, takeaways and opened authoritative Sources. Existing
Units and Lessons are unchanged. No Question or taxonomy record changed.

### Corrections and boundaries carried forward

L2.1 builds on E24-02's corrected model: a scope holds a context; builders combine the
receiver scope's context with arguments; the new coroutine has its own Job attached to
the selected parent. It distinguishes `scope.launch` from an unqualified nested `launch`,
and explains root coroutines without denying that roots can own children. A scope bounds
ordinary child lifetimes; it need not have the same lifetime as a child.

The diagnostic `launch(SupervisorJob())` example traces P, S and C explicitly: fresh S
replaces P as C's parent, rather than being inserted under P. It is labelled an
anti-pattern, not a supervision recipe. The 1.11.0 `launch` API now explicitly documents
Job override as unsupported and provides a deprecated Job overload. Supervision behavior
and cancellation/failure details remain Unit 3 prose pointers. There is no claim that a
cancellation request means work has already stopped.

The blueprint received precision corrections, not a curriculum redesign:

- `Main` is declared in common code; usable implementation depends on platform/runtime.
- `withContext` creates a lexical child scope. It does not return a handle and let the
  caller proceed concurrently in the way `launch` and `async` do.
- Redundant wrappers may add dispatch overhead, not necessarily a dispatch for every
  nested block. An unchanged dispatcher can avoid dispatching.
- Default `async` schedules work eagerly; it does not guarantee immediate body execution.
- Inheritance language now preserves the explicit-parent caveat from Unit 1.

No identity, mapping, objective, Lesson boundary or Teach/Bridge/Reference/Exclude
decision changed. E24-04 was not started.

### Dispatcher and source freshness decisions

Rechecked on 2026-09-10 against Kotlin 2.4 API documentation and kotlinx.coroutines 1.11.0
API documentation. The guide's context and composition pages still have their legacy
structure. API references, not remembered guide sections, establish the precise contracts:

| Claim | Opened authority |
| --- | --- |
| Context keys, composition, child Job and explicit parent override | [CoroutineContext](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.coroutines/-coroutine-context/), [plus](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.coroutines/-coroutine-context/plus.html), [launch](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/launch.html) |
| CPU policy, default fallback and JVM/Native core-based sizing | [Default](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-default.html) |
| JVM IO/Default resource sharing, default parallelism, elasticity | [IO](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-i-o.html) |
| Per-view execution limit is not a count of suspended operations | [limitedParallelism](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-dispatcher/limited-parallelism.html) |
| Common declaration and platform implementations | [Main](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-main.html), [JS/Wasm source at 1.11.0](https://raw.githubusercontent.com/Kotlin/kotlinx.coroutines/1.11.0/kotlinx-coroutines-core/jsAndWasmShared/src/Dispatchers.kt), [Native source at 1.11.0](https://raw.githubusercontent.com/Kotlin/kotlinx.coroutines/1.11.0/kotlinx-coroutines-core/native/src/Dispatchers.kt) |
| Lexical scope, changed/unchanged dispatcher behavior | [withContext](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/with-context.html), [coroutineScope](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/coroutine-scope.html) |
| Scheduling, sequential composition, concurrency and parallelism | [async](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/async.html), [composition guide](https://kotlinlang.org/docs/composing-suspending-functions.html), [basics](https://kotlinlang.org/docs/coroutines-basics.html) |
| Main-safety as implementation guidance | [Android best practices](https://developer.android.com/kotlin/coroutines/coroutines-best-practices), [Android performance guidance](https://developer.android.com/kotlin/coroutines/coroutines-adv) |

L2.2 explicitly confines IO/Default sharing and the configurable larger-of-64-or-core-count
IO limit to JVM. It separates executing blocking tasks from exact pool size and explains
elastic views without promising unlimited useful capacity. IO-to-Default and Default-to-IO
do not guarantee a physical thread switch; Default-to-IO typically retains the worker.
Native IO's implementation differs. JS/Wasm Main delegates to Default with immediate support;
Default does not create browser CPU workers. The desktop shell's Swing dependency was read,
not inferred from its presence in the catalog. Examples naming IO are labelled Android/JVM;
shared examples accept a dispatcher or use common APIs.

L2.3 distinguishes blocking work owned by a repository from an explicitly main-safe,
asynchronous client. The latter needs no ritual IO wrapper. A suspend signature does not
prove main-safety, and expensive processing after a network wait needs its own decision.
The examples do not claim that all methods in any named client library share one contract.

L2.4 compares one account-overview problem in three forms: direct sequential calls,
structured children created before awaiting, and immediate-await serialization. The
400/250/650 ms values are illustrative arithmetic, not benchmarks. Single-thread concurrent
waiting is distinct from parallel CPU execution; independence, conditional need,
coordination, resource pressure and failure complexity all affect whether overlap helps.

### Semantic assessment review against finished prose

All five ACTIVE Questions reached through Unit 2's primary mappings were read in full,
including their answers and explanations, then checked against the completed Lessons.

| Question | Outcome |
| --- | --- |
| `coroutine_supervisor_job_child_context_noop` | L2.1 teaches the parent replacement needed by the key, but the full supervision comparison and distractor reasoning intentionally remain L3.4. Not fully answerable from Unit 2 alone; retain the planned split. |
| `coroutine_io_dispatcher_blocking_calls` | L2.2 establishes occupied computation workers versus blocking-work capacity. IO mitigates that starvation mechanism; it is not a promise of unlimited resources. |
| `suspending_api_dispatcher_assumption` | L2.3's explicitly asynchronous client and blocking-store contrast establish the reasoning. Its dispatcher mapping reaches L2.2 structurally, but Unit practice includes both Lessons. |
| `coroutine_context_switching_001` | L2.3 explains scoped execution, caller waiting and restoration of the caller context, including why the scope's other coroutines are not modified. |
| `coroutine_async_await_sequential` | L2.4 traces task creation and the first await, including the case where await returns immediately. It teaches the reasoning without promising a measured speedup. |

`coroutine_async_exception_surfaces_at_await` was also re-read as required Unit 1 context.
Its dropped-Deferred and supervision reasoning still belongs to Unit 3; the known mapping
review remains E24-08 work. No Lesson was expanded to make it answerable early.

GAP-U2-A/B/C remain **E24-08 assessment work**. Their reasoning is now taught in L2.1's
composition/Job predictions, L2.2's thread-sharing diagnosis, and L2.4's concurrency
judgment. Generated structural coverage does not mean these semantic gaps are closed.

### Backward relationships and validation

- L2.1: `lesson_job_and_parent_child`, `lesson_coroutine_scope_ownership`,
  `lesson_structured_concurrency`.
- L2.2: `lesson_suspension_and_blocking`.
- L2.3: `lesson_structured_concurrency`, `lesson_coroutine_builders`,
  `lesson_work_outside_composition`.
- L2.4: `lesson_coroutine_builders`, `lesson_structured_concurrency`.

All relationships point to previously shipped Lessons. Unit 3 is named only in prose.
The bundle tests now assert the new Unit and Lesson order, titles and exact primary and
supporting mappings. The existing validator covers schema coherence, stable ID uniqueness,
taxonomy references, primary/supporting overlap and related IDs. Learning/question coverage
was regenerated; the unchanged question-bank snapshot needs no regeneration.

## Authoring outcomes for Unit 3

E24-04 authors `unit_cancellation_failure_and_coordination` immediately after Unit 2 in
`learning_curriculum.json`. Everything below was executed, opened or read during that work;
nothing here is recalled from the E24-01 review.

### What did not change

All five proposed Lesson ids, titles, authored order and exact primary/supporting mappings
shipped verbatim, as did the Unit id, title and `async_reactive` home Topic. No Lesson was
renamed, reordered, split or merged, and no boundary in the E24-01 plan was found to be
wrong, so no canonical planning correction was required. Each Lesson carries Core, Practical
and Senior depth and runs 1,062–1,152 words, at the lower end of the range the shipped
Compose and E24 Lessons occupy. No Question, taxonomy record or earlier Unit changed.

`coroutine_cancellation` is deliberately primary in both L3.1 and L3.2, and
`coroutine_parallelism` is primary in both L2.4 and L3.5. Both were confirmed as the
contract's expected shape rather than duplication: the request and the unwind are separate
mental models, and Unit practice counts a shared Subtopic's Questions exactly once.

### Corrections inherited and not reintroduced

The six incorrect simplifications Units 1 and 2 corrected are each contradicted explicitly
somewhere in the finished prose rather than merely avoided:

| Simplification | Where Unit 3 contradicts it |
| --- | --- |
| "`cancel()` means the coroutine has already stopped" | L3.1's Core separates the request from completion, and its Practical shows `cancel()` then `join()` as two steps with a `COMMON_MISTAKE` callout on reading a cancelled scope as a stopped one |
| "Every coroutine always has its launching coroutine as parent" | L3.4's Senior traces `launch(SupervisorJob())` and states that the supplied Job replaced the scope's Job as the parent |
| "`async` only matters when `await()` is called" | L3.3's Practical runs the dropped-`Deferred` case and separates propagation from observation |
| "A suspending function is automatically main-safe" | Not restated anywhere; L3.1 makes the parallel point that `suspend` alone inserts no cancellation check either |
| "A scope and every child have the same lifetime" | L3.1 says cancelling a parent asks every descendant to stop rather than asserting they have |
| "`launch(SupervisorJob())` inserts supervision into the existing hierarchy" | L3.4's Senior completes the trap and calls the pattern an anti-pattern rather than a recipe |

### Corrections made during review

Three claims in the first draft were wrong or under-scoped and were corrected before the Unit
shipped. Each is recorded because each is a claim a later Unit could easily reintroduce, and
because two of the three were teaching a remedy that did not cover the case it was attached to.

| Draft claim | Why it was wrong | What ships |
| --- | --- | --- |
| L3.2: after prompt cancellation discards a value, "keeping the reference somewhere the `finally` block can see is what makes cleanup reliable" | It prescribes the fix for one situation while describing another. If the call site is written `val r = withContext(IO) { open() }`, the assignment never happens, so widening the variable's scope changes nothing. And when *you* write the suspending producer, the caller cannot close a resource it never received at all | The paragraph now says the capture must happen **inside** the block, into a variable `finally` already sees, with the guide's `BufferedReader` shape as the example. A second paragraph adds the producer side: the release belongs at the point of resumption, using the `CancellableContinuation.resume` overload whose handler runs "if and only if the value was not successfully used to resume the continuation". That KDoc is now one of L3.2's Sources |
| L3.5: confinement is "a single-threaded context, or in an application more usually a single component that owns the data and exposes suspending operations" | The second half is not confinement. A class that holds the fields and exposes `suspend` functions serialises nothing: two callers enter those functions concurrently, on two threads, and race exactly as if they had touched the fields directly. The draft was teaching encapsulation as though it were a coordination mechanism | Confinement is now defined as **serialised execution, not encapsulation** — a single-threaded context or a dispatcher view limited to one — and the encapsulation-is-not-enough case is stated explicitly as the thing that does not work |
| L3.3: "later exceptions are attached to it as suppressed exceptions", with the reader directed to the suppressed list | True on the JVM only. The exception-handling guide's own note says the mechanism "currently only works on Java version 1.7+" and that "The JS and Native restrictions are temporary". Stated unconditionally in a multiplatform curriculum it teaches a portability guarantee three of the five target families do not provide | The rule is split: first exception wins everywhere, and the suppression half is scoped to the JVM, with the other targets named as not guaranteeing it. The takeaway bullet carries the same scoping |

The confinement correction exposed a second, related accuracy point in L3.5 that the draft had missed:
a serialised owner serialises *execution*, not a logical operation. The `limitedParallelism`
KDoc says so directly — the limit is on coroutines executing at the same time, not on how many
are inside the block, and it recommends a `Mutex` or `Semaphore` when what is wanted is mutual
exclusion. That is now the bridge from confinement to `Mutex`, and it replaces a comparison-table
cell that had claimed confinement "works but may be heavier than needed" for a multi-step
invariant, which is wrong once any step suspends. The KDoc is now one of L3.5's Sources.

None of the three changed an identity, a mapping, an order, or a Lesson boundary.

### The timeout measurement, re-run

The E24-01 measurement was recreated on this project's JVM target against the resolved
`kotlinx-coroutines-core-jvm:1.11.0` — the artifact path was printed from the running
classpath rather than assumed — and the throwaway test was then deleted. Each case used a
100 ms deadline and `withTimeoutOrNull`. Two runs; the ranges below are across both.

| Case | Body | Returned | Elapsed |
| --- | --- | --- | --- |
| A | `withContext(Dispatchers.IO) { Thread.sleep(1500) }` | `null` | 1682–1730 ms |
| B | a CPU loop running ~1500 ms with no suspension point or cancellation check | **the computed value**, not `null` | 1504–1507 ms |
| C | `join()` on a job launched into a separate, non-child scope | `null`; the job was still active afterwards | 203–229 ms |
| D | the same blocking call as A wrapped in `runInterruptible` | `null` | 199–208 ms |

**One correction to how E24-01's numbers should be read.** This run added two calibration
cases E24-01 did not have: a cooperative `delay(1500)` under the same 100 ms deadline
returned in 171–212 ms, and under a 250 ms deadline in 343–355 ms. There is therefore a
roughly 100 ms fixed overhead in this harness on this machine, so case C's 203 ms and case
D's 199 ms are "at the deadline", not "at twice the deadline". E24-01's 108 ms for case C is
consistent with the same behaviour measured with less overhead. **The conceptual findings are
unchanged and were reproduced exactly:** A and B both overran the deadline by roughly fifteen
times with the caller waiting the whole time, B returned a successful computed value, and C
is the only case where the caller moved on early — because the awaited work was never a
child. L3.2 presents the table with the calibration stated and labels the figures
measurements rather than API guarantees.

Case D is new to E24-04 and was added because the `runInterruptible` claim deserved data
rather than only prose: the same blocking call that overran by 1.6 s in case A stopped at the
deadline when wrapped. The `InterruptedException` was not observed outside the
`runInterruptible` block, which matches the guide's example — the interruption is raised
inside the block and reaches the coroutine as a `CancellationException`.

### The existing Question explanation defect, confirmed and not fixed here

`coroutine_run_interruptible_blocking_call`'s explanation ends with the clause "withTimeout
resumes the coroutine while leaving the blocked thread occupied". Case A is that exact
scenario and contradicts it: the coroutine was not resumed until the blocking call returned
1.7 s later. The Question's key and all four options remain correct — its `withTimeout`
distractor claims the call "abandons the blocking method", which is genuinely false — so this
is a wrong rationale attached to a right answer.

**It was not corrected in E24-04.** The repository's correctness rules require that a shipped
Question's answer key be right, and it is; nothing about the defect makes the Question
unanswerable or teaches a wrong key. Correcting it would also touch a Question record in an
issue whose scope is Lesson authoring, and E24-08 already owns both this clause and the
Question's Subtopic review, so splitting them across two issues would make the audit trail
worse. L3.2 teaches the correct model, which is what E24-04 owes the learner.

### Atomics in common code, resolved

E24-01's first [unresolved question](#unresolved-questions) is answered. Read from the
resolved `kotlin-stdlib:2.4.10` sources artifact and confirmed by compiling against this
repository's own common source set:

- **Available from common code.** `kotlin.concurrent.atomics.AtomicInt`, `AtomicLong`,
  `AtomicBoolean` and `AtomicReference` are `expect class` declarations in the stdlib's
  `commonMain`, `@SinceKotlin("2.1")`. A probe file using `AtomicInt` and `incrementAndFetch`
  in `shared/src/commonMain` compiled successfully under
  `./gradlew :shared:compileCommonMainKotlinMetadata`, so the API is genuinely available for
  every target this repository declares.
- **Still experimental, at error level.** Every declaration carries `@ExperimentalAtomicApi`,
  which is `@RequiresOptIn(level = RequiresOptIn.Level.ERROR)`. Removing `@OptIn` from the
  same probe failed the metadata compilation with four opt-in errors, so the annotation is a
  real requirement rather than a warning.
- **Not thread-safe everywhere.** The KDoc states that "For JS and Wasm [AtomicInt] is
  implemented trivially and is not thread-safe since these platforms do not support
  multi-threading", and that the `update` family invokes its transform exactly once there.

**Decision.** The opt-in is one annotation, so it does not make the example distracting, and
the common API is the accurate answer for a multiplatform curriculum. L3.5 therefore shows
`kotlin.concurrent.atomics` in common code with the `@OptIn(ExperimentalAtomicApi::class)`
line visible, states the experimental status in prose, and states the JS/Wasm caveat next to
it. `java.util.concurrent.atomic` appears nowhere in the Unit. Note that the Kotlin guide's
own shared-state page still uses `AtomicInteger`, which is JVM-only — that is one of the
places where following the guide literally would have produced an inaccurate multiplatform
Lesson.

### JVM and multiplatform decisions

- `runInterruptible` and thread interruption are labelled JVM-only in L3.2's Senior section,
  in the prose and in a comment inside the example. The Lesson states that no dispatcher
  interrupts threads by itself and that other targets have no equivalent.
- `Thread.sleep` appears only inside the measurement table, as the body of a labelled JVM
  case.
- `Dispatchers.IO` appears twice, both times inside an example explicitly marked Android/JVM.
- L3.5 states that genuine multi-threaded execution is a platform property: the race is real
  on JVM, Android and Native, and the same code cannot interleave that way on JS or Wasm.
- The platform-specific last-resort handling of an unpropagated exception is named in one
  clause in L3.3 as differing per target and is not enumerated. The blueprint marks that
  material **Exclude**, and enumerating four platform behaviours would have turned a boundary
  note into a section.

### Source decisions and freshness

Every page was opened on 2026-09-10 rather than recalled, and two of the four legacy pages
E24-01 listed have moved.

| Page | State on 2026-09-10 | Consequence |
| --- | --- | --- |
| `coroutines-cancellation.html` | "Cancellation and timeouts", dated **27 July 2026**. Sections: cancel, cancellation propagation, reacting to cancellation, suspension points, `yield()`, explicit checks, interrupting blocking code, **handling values safely when canceling**, non-cancelable blocks, timeout | Structure matches what E24-01 recorded, plus a **new section on prompt cancellation** that E24-01 did not have. L3.2's Core teaches it |
| `exception-handling.html` | "Coroutine exceptions handling", dated **20 July 2026**. Legacy section structure unchanged: propagation, `CoroutineExceptionHandler`, cancellation and exceptions, aggregation, supervision | Still the guide-level authority for L3.3 and L3.4, as E24-01 expected |
| `shared-mutable-state-and-concurrency.html` | Legacy structure, dated **27 September 2024** | Still current and still the best guide source for L3.5's three-option structure, but its atomic example is JVM-only |
| `channels.html` | Legacy structure, unchanged | Cited once by L3.5 for the bounded contrast |

Three findings that changed how a Lesson is written:

1. **Prompt cancellation.** The cancellation page now states that a cancelled suspended
   coroutine "resumes with a `CancellationException` instead of returning any values, even if
   those values are already available", and gives the `BufferedReader` example where a
   resource is acquired but the value never reaches the caller. This is a real cleanup hazard
   E24-01's plan did not name, and L3.2's Core now teaches it before `finally`.
2. **`withTimeout` is no longer on the guide page.** The rewritten Timeout section documents
   only `withTimeoutOrNull` with a `Duration`. `withTimeout`'s semantics and
   `TimeoutCancellationException` come from the API reference, which is what L3.2 cites. That
   KDoc is also where the two sentences the Lesson leans on live: `withTimeout` "behaves like
   `coroutineScope`, as it, too, creates a new lexically scoped child coroutine", and
   "Cancellation on timeout runs concurrently the code running in the block and may happen at
   any time, even after the block finishes executing but before the caller gets resumed with
   the result."
3. **`CoroutineExceptionHandler`'s KDoc has a better model than the guide.** The 1.11.0
   reference frames the handler as an element "to handle coroutine exceptions without a clear
   propagation path" and enumerates the four recognised paths, where the guide still says
   "root". The two agree — a `launch` on a scope with no `Job`, or a direct child of a
   supervisor, is exactly a case with no path — but the path model is the one a reader can
   apply to an arbitrary tree, so L3.3 teaches it and the guide's wording is the supporting
   citation.

`NonCancellable`'s KDoc supplied one framing E24-01 did not have: `launch(NonCancellable)`
"severs" the parent-child relation entirely, and code after a `withContext(NonCancellable)`
block still runs in a cancelled coroutine. Both are in L3.2's Practical. `Mutex`'s KDoc
supplied non-reentrancy, which L3.5 does not teach but which was checked before writing that
`withLock` is the structured form.

### The exception-handler boundary as taught

E24-04's acceptance criteria require this to be precise, so it is recorded exactly. L3.3's
Senior section lists the four propagation paths the library recognises — a `try`/`catch`
inside the coroutine, a lexically scoped builder rethrowing to its caller, structured transfer
to a parent that processes child failures, and a builder whose return value allows querying
the result — and then states that the handler is invoked only when none of them applies. Two
worked negatives follow: a handler on an ordinary child inside `coroutineScope`, which never
runs because the failure does have a path, and a handler on `async`, which is redundant
because the caller of `await()` owns that failure. The Lesson also states that the handler
cannot recover, because it runs after the coroutine has already completed with the exception.
No sentence in the Unit says "catches errors in the scope", "global try/catch", or "handles
all uncaught child exceptions".

### Semantic review of the Questions this Unit now reaches

All seven ACTIVE Questions reachable through Unit 3's four primary concepts were read in full
— stem, options, key, explanation and Sources — against the finished prose.

| Question | Verdict against the shipped Lessons |
| --- | --- |
| `coroutine_cancellation_001` | Answerable from L3.1's Core and Practical. Its three distractors — thread interruption, finishing the current block, taking effect at a dispatcher switch — are each contradicted directly, the first by L3.2's JVM-only framing of interruption |
| `cancellation_exception_rethrow` | Answerable from L3.1's Senior, which argues from what the handler consumes rather than restating a rule, and shows the `ensureActive()` form the API reference recommends |
| `coroutine_run_interruptible_blocking_call` | Answerable from L3.2's Senior. The key's mechanism, the `Dispatchers.IO` distractor and the pre-call `ensureActive()` distractor are each addressed. Its explanation defect is recorded above and left to E24-08 |
| `coroutine_exceptions_001` | Answerable from L3.3's Core, which walks the three-child tree the Question describes |
| `coroutine_exception_handler_root_boundary` | Answerable from L3.3's Senior. Its key is phrased as "uncaught root-coroutine exceptions"; the Lesson teaches the propagation-path model, which subsumes that phrasing rather than contradicting it, and teaches the `async`/`await` half explicitly |
| `coroutine_supervisor_scope_direct_children` | Answerable from L3.4's Core, which uses the same nested shape and asks the reader to predict it before giving the answer |
| `coroutine_async_await_sequential` | Owned by L2.4 and unchanged. It reaches Unit 3 because `coroutine_parallelism` is L3.5's primary concept; L3.5 does not teach `await` sequencing and does not need to, since both Lessons ship and the reasoning is taught in Unit 2 |

Two Questions outside this Unit's mappings were re-read because the issue requires it:

- `parent_cancellation_propagates_children` is **now answerable**, but from Unit 3, not from
  the Unit its Subtopic reaches. Its three distractors need cooperative resumption, that
  `cancel()` returns at once, and that a suspending call in `finally` needs `NonCancellable` —
  L3.1 and L3.2 teach all three. This strengthens rather than changes the recorded re-map
  candidate: the reasoning now demonstrably lives in Unit 3 while `coroutine_jobs` keeps it in
  Unit 1 practice. Neither Unit was bent to accommodate the mapping.
- `coroutine_async_exception_surfaces_at_await` is **now answerable** from L3.3's Practical,
  which runs the dropped-`Deferred` case, and L3.4, which supplies the `supervisorScope`
  clause its explanation depends on. Its Subtopic still reaches Unit 1. Recorded, not fixed.

`coroutine_supervisor_job_child_context_noop` was also re-read as required Unit 2 context. Its
reasoning is now complete across L2.1 and L3.4 as this plan intended, which was the split
E24-03 recorded as deliberate.

### The known gaps were not filled

E24-04 authored no Question. GAP-U3-A and GAP-U3-B are unchanged; what changed is that the
reasoning each describes is now taught, so E24-08 has something to assess against.

| Gap | Where the reasoning now lives |
| --- | --- |
| GAP-U3-A — cleanup and timeouts | L3.2 in full: prompt cancellation, the broken and fixed suspending `close()`, `NonCancellable`'s three boundaries, timeout as cancellation with a clock, and the measured cases where a deadline bounds nothing |
| GAP-U3-B — races and coordination | L3.5 in full: the lost-update example, the volatile dead end, the three mechanisms, the shape-of-state comparison table, and the one-paragraph `Channel` bridge |

The Unit's supporting concepts are asserted in a new bundle test so that a later change cannot
quietly promote `jvm_fundamentals`, `android_memory_model` or `hot_vs_cold_streams` to primary
and claim Unit practice that no `async_reactive` Question provides — which would hide GAP-U3-B
rather than close it.

### Cross-links and validation

Backward-only, and every target already ships:

- L3.1: `lesson_job_and_parent_child`, `lesson_structured_concurrency`.
- L3.2: `lesson_cooperative_cancellation`, `lesson_with_context_and_main_safety`.
- L3.3: `lesson_coroutine_builders`, `lesson_structured_concurrency`,
  `lesson_cooperative_cancellation`.
- L3.4: `lesson_coroutine_context`, `lesson_exception_propagation`.
- L3.5: `lesson_dispatchers`, `lesson_sequential_and_concurrent_work`.

Nothing links into an unshipped Flow Unit. The forward references the Unit does make are prose:
L3.5 points the full Channel-versus-stream comparison at "the hot and cold streams material
later in this curriculum" without naming an id, and L3.1 defers the cancellation-versus-failure
asymmetry to L3.3 by describing it rather than linking.

E24-05 was not started, and no Flow, testing, Compose-effect or architecture material was
authored.

## Authoring outcomes for Unit 4

E24-05 authors `unit_flow_fundamentals` immediately after Unit 3 in
`learning_curriculum.json`, and performs the `snapshotFlow` reconciliation this plan
assigned to it. Everything below was executed, opened or read during that work on
2026-09-10; nothing here is recalled from the E24-01 review.

### What did not change

All five proposed Lesson ids, titles, authored order and exact primary/supporting mappings
shipped verbatim, as did the Unit id, title and `async_reactive` home Topic. No Lesson was
renamed, reordered, split or merged, and no boundary in the E24-01 plan was found to be
wrong, so no canonical planning correction was required beyond the status and outcomes
recorded here. Each Lesson carries Core, Practical and Senior depth and runs 1,063–1,194
words, inside the range the shipped Compose and E24 Lessons occupy. No Question, taxonomy
record or earlier E24 Unit changed.

`flow_fundamentals` is deliberately primary in L4.1, L4.2 and L4.5, which the plan's
[shared-primaries](#identity-and-mapping-checks-performed) note anticipated. Unit practice
therefore reaches **three** concepts — `flow_fundamentals`, `flow_collection`,
`flow_context` — and **five** ACTIVE Questions, each counted once. That is asserted in the
integration test rather than left to the generated snapshot.

### Boundaries held, and how

Every boundary the epic draws around this Unit was tested against the finished prose rather
than assumed from the outline.

| Boundary | What ships |
| --- | --- |
| Unit 5 owns operators | L4.2 introduces intermediate against terminal only, using the operators page's own definitions, and states outright that which operator to reach for is the composition unit's subject. `map` and `filter` appear as syntax in one illustrative chain. No operator's decision model is taught anywhere |
| Unit 5 owns failure | `catch`, `retry`, `retryWhen` and `onCompletion` are never named. L4.4's Senior section spends one paragraph saying that context preservation is one instance of a transparency rule whose failure half exists, and defers it by prose |
| Unit 6 owns hot streams | `StateFlow` and `SharedFlow` are never named in the Unit. Hot streams are described twice — in L4.1 as a third API shape that exists, and in L4.2 as the limit of the word *cold* — both times without an id and both times pointing at "the hot streams unit later in this curriculum" |
| E25 owns Compose and lifecycle integration | `LaunchedEffect`, `collectAsState`, `collectAsStateWithLifecycle`, `repeatOnLifecycle` and `flowWithLifecycle` are never named. L4.3 gives lifecycle-bound collection one sentence and defers the mechanisms |
| Channels stay Reference | `channelFlow`'s channel is explained as far as "emission from more than one coroutine needs a thread-safe, context-preserving channel", and L4.5's Senior section states explicitly that channels as a coordination primitive — capacities, fan-out, pipelines — are a subject this unit leaves alone. `produceIn` is absent |
| Architecture stays supporting | L4.1 uses a repository-shaped example because it makes the decision concrete, and holds the single-source-of-truth argument to one sentence that names the architecture curriculum as its owner. No MVVM, MVI, ViewModel or state-ownership material appears |

### How Unit 3's cancellation model was carried forward

This was the largest accuracy risk in the Unit, because the shortest true sentence —
"cancelling the collector cancels the upstream flow" — is also the one that reintroduces the
misconception Unit 3 removed. L4.3's Senior section is built around not doing that.

- The rule is stated as a request observed cooperatively, in the same words Unit 3 used, and
  the Lesson says explicitly that Flow introduces no second cancellation mechanism.
- The over-strong reading is contradicted with data rather than with a caveat. Measurement B
  below is a producer that was cancelled and finished anyway, and the Lesson quotes it.
- The well-behaved case is given equal weight, because leaving a reader believing
  cancellation usually does not work would be its own error: `emit` is a cooperative
  suspension point, so an ordinary producer stops promptly, and measurement C is that case.
- The closing paragraph names what is *absent* from the model — no Flow-specific kill
  switch, no separate collection lifecycle, no Job the flow owns — because the wrong mental
  model is what produces the wrong predictions, not a missing fact.

`runInterruptible`, `NonCancellable` and timeouts are not re-taught. L4.5 links back to
`lesson_cancellation_cleanup_and_timeouts` for the cleanup shape `awaitClose` reproduces,
which is the backward link the plan's cross-linking rules anticipated.

### Corrections made during review

Two claims in the shipped draft were over-strong and were corrected during review. Both are
recorded because both are claims a later Flow Unit could easily reintroduce.

| Draft claim | Why it was wrong | What ships |
| --- | --- | --- |
| L4.3: a cold flow "has no coroutine of its own. It does not schedule anything, it does not own a `Job`" | True of a `flow { }` builder collected directly, and false as a general rule — which **this Unit's own later Lessons contradict**. L4.4 states that `flowOn` runs the upstream in a separate coroutine, and L4.5 shows `channelFlow` launching children. Coldness governs *when* production starts and *per whom*, not whether the implementation creates coroutines | The paragraph now scopes the strong form to the simplest pipeline, names the two operators in this Unit that do introduce a coroutine, and moves the invariant to the place it actually holds: any coroutine a flow creates is a **child of the collecting coroutine**, so nothing is scheduled behind the collector's back or outlives it. L4.3's summary and its Senior "what is absent" paragraph were reworded to match — the Senior line now says no Job the flow owns *independently of its collector* |
| L4.2: naming eight terminal operators and then "everything else in an ordinary chain is intermediate and therefore inert" | The list was read off the operators page, which offers those names as examples and nowhere claims to be exhaustive. `count`, `single` and `last` are terminal too, so the closing clause told a learner that calls which collect immediately are inert intermediate operators | The list is presented as examples — grouped as `collect`/`collectLatest`, the reducing operators, and `launchIn` — and the exhaustive clause is replaced by the test that actually decides it: an operator returning another `Flow` is intermediate and has started nothing; one that suspends to produce a result, or launches a coroutine to produce it, has collected |

A second review pass found three more, of which two were accuracy defects in the prose:

| Draft claim | Why it was wrong | What ships |
| --- | --- | --- |
| L4.1: the Flow signature means "the caller is told the first value and every later one", and a cost bullet reading "The value arrives more than once" | `Flow<T>` describes **zero or more** values; a flow may emit nothing, or emit once and complete. Stated as written, the Lesson taught a reader to infer cardinality and freshness from the return type, when those are the API's documented contract. `observeTheme()` emits repeatedly because of what it is for, not because `Flow` compels it | The code comment now reads "This API's contract: the current theme, and every later one". A new Core paragraph states that the type settles only that values arrive over time, and that cardinality and freshness are the author's to document. The cost bullet reads "Values **may** arrive more than once — how many and for how long is this API's contract rather than a guarantee of `Flow<T>`". The interview callout carries the same qualification |
| L4.5: the `callbackFlow` adapter called bare `trySend(value)` | `trySend` cannot suspend, so it cannot apply back pressure; it returns a `ChannelResult` that fails when the buffer is full or collection has ended. The example therefore dropped values silently, and the Senior section acknowledged that failure is possible without the Practical example giving the reader any policy — so a learner copying the adapter got neither delivery nor an explicit drop | The example inspects the result — `trySend(value).onFailure { cause -> onLocationDropped(value, cause) }` — with a comment saying dropping is a decision rather than a default, mirroring the shape the `callbackFlow` KDoc's own example uses. A new Practical paragraph explains why `trySend` cannot suspend, when it fails, that dropping is sometimes the right policy for a superseded position, and that it should be stated rather than fallen into. Buffering policy itself stays deferred to Unit 5 |
| This document: the source-freshness note said the rewritten *Flows* page "no longer has" the `#flow-cancellation` anchor | Contradicted two other passages in the same change, both of which record that the anchor was checked live and still resolves. As the source-verification handoff for E24-08, an unreconciled status leaves it unclear whether a Question Source needs repair | The note now states that the anchor and all three URLs resolve, that the pages still support every claim attached to them, and that the stale item is the citation *title* rather than a broken link — so nothing obliges E24-08 to repair a Source |

None of the five corrections changed an identity, a mapping, an order, or a Lesson boundary,
and none changed a measurement.

### Claims that were executed rather than reasoned about

A throwaway JVM test probed each of these against the resolved
`kotlinx-coroutines-core:1.11.0` and was then deleted. The numbers are quoted in the Lessons
as measurements, not as guarantees.

| Claim a Lesson makes | Measured result |
| --- | --- |
| Constructing a cold flow runs nothing (L4.2) | After constructing a `flow { }` with a side-effecting first statement, the producer had run **0** times |
| Each collector starts a new execution (L4.2) | The same two-emission flow collected twice ran its producer **2** times and delivered `[1, 2, 1, 2]` |
| Building an intermediate chain still starts nothing (L4.2) | A `flow { }.map { }` chain with no terminal operator had run its producer **0** times; after one `collect`, **1** |
| A. `withContext` around `emit` inside `flow { }` fails, and how (L4.4) | `IllegalStateException`, message quoted verbatim in the Lesson: `Flow invariant is violated: / Flow was collected in [… BlockingEventLoop…], / but emission happened in [… Dispatchers.IO]. / Please refer to 'flow' documentation or use 'flowOn' instead` |
| B. Cancellation does not interrupt non-cooperative producer work (L4.3) | A producer whose first statement was a 600 ms `Thread.sleep`, cancelled after 100 ms: the blocking call **completed**, and `join()` on the cancelled coroutine returned **694 ms** after the start |
| C. A cooperative producer stops promptly and runs its cleanup (L4.3) | An emitting loop with `delay` between values, cancelled mid-flight: the producer's `finally` ran once and the collecting Job completed |
| C2. `emit` alone is enough to observe cancellation (L4.3) | A producer whose **only** cooperative point was `emit` — a tight loop with no `delay`, no `yield` and no other suspending call — stopped when its collector was cancelled: the loop never exited normally and the coroutine completed. This is the `flow` builder being "cancellable by default", checked rather than assumed |
| D. `flowOn` moves the upstream into another coroutine, and asking for `IO` does not produce a thread from a separate pool (L4.4) | Upstream moved with `.flowOn(Dispatchers.IO)`, collected from `Dispatchers.Default`: producer and upstream `map` on `DefaultDispatcher-worker-3 @coroutine#4`; downstream `map` and collector on `DefaultDispatcher-worker-1 @coroutine#1`. Different coroutine, and both threads from the **Default pool**, which is what the `Dispatchers.IO` KDoc's shared-threads clause predicts |
| E. `launchIn` returns a `Job` and the scope owns the collection (L4.3) | `launchIn` returned a `StandaloneCoroutine`, `is Job` true; cancelling the scope ended both collections |
| F. `callbackFlow` without `awaitClose` fails, and how (L4.5) | `IllegalStateException`, message quoted verbatim in the Lesson: `'awaitClose { yourCallbackOrListener.cancel() }' should be used in the end of callbackFlow block. Otherwise, a callback/listener may leak in case of external cancellation. See callbackFlow API documentation for the details.` |
| G. A cold adapter registers once per collection, and `awaitClose` unregisters each (L4.5) | One `callbackFlow` collected by two coroutines: **2** registrations, **0** unregistrations while both ran; after cancelling the scope, **2** unregistrations |
| H. `channelFlow` children produce concurrently and out of order (L4.5) | Two child coroutines, one delayed: values arrived `[2, 1]` |

Measurement D is the one worth keeping. It is simultaneously the evidence that `flowOn` runs
the upstream in a *different coroutine* — which is what makes the invariant satisfiable — and
the evidence that Unit 2's "context is not thread" precision survives into Flow. Note what it
does and does not show: the two coroutines ran on two different worker threads, so this is not
a demonstration that a context switch never moves threads. What it demonstrates is that both
threads came from the `Default` pool even though the upstream asked for `IO`, which is the
shared-threads clause of the `Dispatchers.IO` KDoc observed rather than quoted. L4.4 is worded
to claim exactly that and no more.

### Source freshness, re-checked on 2026-09-10

Every page was opened rather than recalled. Two dates are new information relative to what
E24-01 recorded, and neither changes a claim.

| Page | State on 2026-09-10 | Consequence |
| --- | --- | --- |
| `coroutines-flow.html` | "Flows", dated **13 July 2026**. Two top-level sections, Cold flows and Hot flows, exactly the structure E24-01 recorded but with a date E24-01 did not capture — and, as finding 1 below records, a good deal more underneath them than that structure implies | Still the guide-level authority for coldness, per-collector execution, collection lifetime and the context default. Every one of those four claims was re-read on the page, as were the intermediate-operator and sequences clauses L4.2 now quotes |
| `coroutines-flow-operators.html` | "Flow operators", dated **28 July 2026** | Supplies the intermediate/terminal definitions L4.2 quotes. **Its terminal-operator names are examples, not an inventory** — `count`, `single` and `last` are terminal and are not among them — so a Lesson must not treat the page's list as exhaustive; see [corrections made during review](#corrections-made-during-review) |
| `flow` KDoc | Current | Carries the context-preservation requirement *and the `withContext(Dispatcher.IO) { emit(2) }` example* as its own illustration of the `IllegalStateException`. This is the single best source for L4.4 and is cited there |
| `flowOn`, `launchIn`, `channelFlow`, `callbackFlow`, `awaitClose` KDocs | Current | The five API pages the Unit leans on. Each supports the specific clause attached to it |
| `sequences.html` | Current | Used only for the laziness bridge in L4.2's Senior section. Confirmed that the page does not discuss coroutines, which is the boundary the Lesson states |
| Android coroutines best practices | Current | Quoted once, in L4.1, for the suspend-for-one-shot / Flow-for-changes division. Used for guidance only; every semantic claim in the Unit is sourced to Kotlin's own documentation |

Three findings that changed how a Lesson is written:

1. **The rewritten *Flows* page carries more than its two top-level headings suggest, and
   reading only the headings is a trap.** Its table of contents shows just "Cold flows" and
   "Hot flows", which is what E24-01 recorded — but under them the page has anchored
   subsections including `#intermediate-flow-operators`, `#call-suspending-functions-inside-a-flow-builder`,
   `#change-the-coroutine-context-of-a-cold-flow-with-flowon` and `#flow-cancellation`. Two of
   those changed how L4.2 is sourced. The page states that intermediate operators "are cold, so
   the returned flow doesn't start processing values until it's collected", which is the exact
   claim the Lesson makes and which a first draft argued from the operators page's definitions
   plus measurement instead; and it states "Unlike in sequences, you can call suspending
   functions inside a `flow()` builder function", which is the sequence analogy's limit in the
   documentation's own words. Both are now quoted rather than inferred. **E24-06 and E24-07
   should read the page body rather than its heading list**, because the operator, exception and
   hot-stream subsections are there too.
2. **`callbackFlow`'s KDoc does not print its own exception message.** It states that the
   builder "throws `IllegalStateException` if block returns, but the channel is not closed
   yet" without quoting the text. L4.5 quotes the runtime's actual message because measuring
   it was the only way to have it, and the message is more instructive than the sentence.
3. **The `flow` builder's cancellability is documented on `cancellable`, not on `flow`.** The
   first draft of L4.3 asserted that `emit` is a cooperative suspension point, which is a
   reasonable inference but was not sourced. The `cancellable` KDoc is where the statement
   actually lives — the `flow` builder and all `SharedFlow` implementations are cancellable by
   default, and `cancellable()` "checks cancellation status on each emission" — so the Lesson
   now says what the documentation says and backs it with measurement C2 rather than with an
   inference. That KDoc was added to L4.3's Sources.
4. **`flowOn` is not purely a context operator.** Its KDoc states that a dispatcher change
   puts emission and collection in two coroutines with "a channel with a default buffer size
   … used internally between the two". L4.4's Senior section teaches that, because it is both
   the mechanism that makes context preservation possible and a buffering side effect a
   reader will otherwise meet as a surprise in Unit 5.

### The context invariant as taught

GAP-U4-B is the reason to record this precisely. L4.4 does not present the invariant as a
rule with an exception list. The order is: the default (a cold flow runs in the collector's
context), then why that default is worth having (a `collect` call is locally readable — the
context you are looking at is the context the chain runs in), then the wrong code, then the
runtime's own message, then the reason the rule exists stated as a consequence — if a
producer could switch its own emission context, no reader could know their collection's
context without tracing every operator to the source. `flowOn` arrives last, as the answer
the runtime message itself names. A reader who follows that order can reconstruct why the
invariant exists rather than remembering that it does.

The Lesson also refuses the two shortest wrong summaries explicitly: that a flow runs in the
background because it is a flow (the `suspend` misconception in new clothing, and named as
such), and that `flowOn(ioDispatcher)` means "this flow runs on IO" (a `COMMON_MISTAKE`
callout, because a reader with the direction backwards also places the operator wrongly).

### The builder decision as taught

GAP-U4-C asks for the choice among `flow`, `channelFlow` and `callbackFlow` to be teachable.
L4.5 teaches it from the producer's shape rather than as a lookup table: the `flow` builder's
limit is stated first — emission must happen in the builder's own context, from its own
coroutine — and the other two builders are then introduced as the two ordinary producers that
do not fit inside it. The comparison table in the Senior section carries a "why that one"
column rather than a feature list, and one paragraph states why `channelFlow` and
`callbackFlow` are not interchangeable despite sharing channel-backed mechanics:
`channelFlow` answers who may produce, `callbackFlow` answers what tears a subscription down.

`awaitClose` is taught as a lifetime rather than as a required incantation. The block is read
top to bottom as register / bridge values in / hold the collection open / unregister; the
runtime's message is quoted; the leak is named concretely — listener still registered, source
retaining it and what it captured, values delivered to a consumer that is gone; and the
`awaitClose` KDoc's "executed unconditionally before this function returns" is connected back
to Unit 3's `finally` shape. Measurement G supplies the ownership point the plan asked for:
each cold collection registers its own subscription.

### Semantic review of the Questions this Unit now reaches

All five ACTIVE Questions reachable through Unit 4's three primary concepts were read in full
— stem, options, key, explanation and Sources — against the finished prose.

| Question | Verdict against the shipped Lessons |
| --- | --- |
| `flow_fundamentals_001` | Answerable from L4.2's Core and Practical. Its key is the builder running only on collection, and its three distractors — replaying the most recent value, sharing one producer among collectors, emitting after the last collector stops — are each properties of hot streams, which L4.2's Senior section names as the limit of the word *cold* without teaching them. The Lesson argues from the recipe model rather than echoing the option wording |
| `callback_flow_await_close_registration` | Answerable from L4.5's Practical, which is built on the same registration-and-return shape. All three distractors are addressed: buffering is named as the builder's separate channel capacity, coldness and re-collectability are established in the Core section and measured in the Senior one, and the key's two halves — keeping the flow open and giving unregistration one place — are the paragraph the Lesson is organised around. The plan's note that its `FOUNDATION` level looks low is unchanged and untouched |
| `flow_launch_in_on_each_scope` | Answerable from L4.3's Practical, which quotes the KDoc's `scope.launch { flow.collect() }` equivalence, states that the returned value is a `Job`, and rejects the hot-flow distractor explicitly with measurement E behind it |
| `flow_collection_cancels_cold_producer` | Answerable from L4.3. Its key requires both halves the Lesson teaches — the producer runs inside the collecting coroutine, and it stops at a cancellable suspension point — and the "cancellation interrupts the running thread" distractor is contradicted directly by measurement B. This is the Question the Unit is best matched to |
| `flow_context_001` | Answerable from L4.4's Practical. Its key is upstream-only, its three distractors each move the collector, and the Lesson's `COMMON_MISTAKE` callout is precisely that error. Note that the Question's own explanation uses the phrase "context-transparent" where the current documentation says "context-preserving"; the Lesson uses the documentation's term. This is a wording difference, not a defect, and no Question was changed |

One Source detail is worth flagging to E24-08 without acting on it here.
`flow_fundamentals_001`, `flow_context_001` and `flow_collection_cancels_cold_producer` all
cite `coroutines-flow.html` under its **old title**, "Asynchronous Flow", which the page has
not carried since the rewrite. The third also cites the `#flow-cancellation` anchor, and that
anchor was checked against the live page and **still exists**. So all three URLs resolve, the
anchor resolves, and the page still supports every claim attached to it: this is a stale
citation *label*, not a broken Source. It is recorded rather than fixed, because E24-05 changes
no Question.

The three Compose `snapshotFlow` Questions (`compose_snapshot_flow_state`,
`compose_snapshot_flow_read_inside_block`, `compose_snapshot_flow_conflated_state`) were
re-read to confirm the new material neither duplicates nor contradicts them. It does not: no
E24 Lesson mentions `snapshotFlow`, the snapshot system, or Compose state observation, and
`compose_snapshot_system` remains reachable only through the Compose Unit. They create no
Unit 4 practice and are not counted as any.

### The known gaps were not filled

E24-05 authored no Question. GAP-U4-A, GAP-U4-B and GAP-U4-C are unchanged as gaps; what
changed is that the reasoning each describes is now taught, so E24-08 has something to assess
against.

| Gap | Where the reasoning now lives |
| --- | --- |
| GAP-U4-A — one suspending result against an observable Flow | L4.1 in full: the paired signatures, the concrete inability of a one-shot call to report a later change, the repository carrying both shapes, the five-item list of what a Flow-returning API costs the caller, and the `COMMON_MISTAKE` callout against "if it can change, return a Flow" |
| GAP-U4-B — the reason for the context invariant | L4.4's Practical and Senior: the KDoc's own wrong example, the runtime's verbatim message, the local-readability argument for why the rule exists, and measurement D on what `flowOn` actually arranges |
| GAP-U4-C — choosing among the three builders | L4.5 in full: the `flow` builder's limit stated first, the two producers that do not fit it, the comparison table with a reason column, and the paragraph on why the last two are not interchangeable |

The Unit's supporting concepts are asserted in a new bundle test so that a later change cannot
quietly promote `repository_pattern`, `single_source_of_truth`, `kotlin_sequences`,
`lifecycle_coroutines` or `memory_leaks` to primary and claim Unit practice that no
`async_reactive` Question provides — which would hide GAP-U4-A in particular, whose whole
point is that the closest existing assessment sits in the `architecture` Topic.

### The `snapshotFlow` reconciliation, exactly as performed

Two edits to `lesson_snapshot_flow`, and nothing else in that Lesson or its Unit.

1. **One paragraph replaced.** The sentence "The full subject — operators, hot streams and
   sharing, back pressure, structured cancellation — belongs to the coroutines and Flow
   curriculum, **which this app does not teach yet**; Kotlin's own documentation is where to
   go for it in the meantime" became a pointer to the shipped Unit: the full subject is taught
   by the Flow Fundamentals unit, the first two of the Lesson's four facts are its treatment
   of cold flows and the last two its treatment of collection lifetime and cancellation, both
   linked below, with operators, hot streams and sharing, and back pressure in the units after
   it. The four facts themselves are **unchanged**, because E24-01 verified them and Unit 4
   agrees with all four.
2. **Two `relatedLessonIds` appended:** `lesson_cold_flows` and
   `lesson_flow_collection_lifetime`, in that order, after the three existing Compose links.
   Both targets now ship, so neither is a forward reference.

Nothing else moved. No snapshot-system semantics, no snapshot read observation, no
`snapshotFlow` behaviour and no Compose guidance was copied, rewritten or relocated, and no
E24 Lesson takes a Compose Subtopic as primary or mentions `snapshotFlow` at all. `Flow` and
`snapshotFlow` are related in one direction only: the Compose Lesson points at the Flow Unit
for the general model, and the Flow Unit's two backward links let a reader who follows them
recognise the same cold mechanics in a Compose setting without meeting duplicate material.

The second, weaker pointer the plan identified — `lesson_work_outside_composition`'s
"belong to the effects and coroutine material later in this path" — was re-read and **not**
edited. It remains half-true in the same way it was before Unit 4: the coroutine half now
ships and the Compose-effects half does not. It stays E24-09's candidate, as the plan
assigned.

### Cross-links and validation

Backward-only, and every target already ships:

- L4.1: `lesson_suspension_and_blocking`.
- L4.2: `lesson_why_flow`, `lesson_snapshot_flow`.
- L4.3: `lesson_cold_flows`, `lesson_coroutine_scope_ownership`,
  `lesson_cooperative_cancellation`, `lesson_snapshot_flow`.
- L4.4: `lesson_cold_flows`, `lesson_coroutine_context`,
  `lesson_with_context_and_main_safety`.
- L4.5: `lesson_cold_flows`, `lesson_flow_collection_lifetime`,
  `lesson_flow_context_and_flow_on`, `lesson_cancellation_cleanup_and_timeouts`.

Both Compose links the plan recommended were taken (`lesson_snapshot_flow` from
`lesson_cold_flows` and from `lesson_flow_collection_lifetime`); the third,
from `lesson_hot_and_cold_streams`, belongs to E24-07. Nothing links into an unshipped Unit.
Every forward reference the Unit makes is prose naming a unit rather than an id: operators,
buffering and failure to "the flow composition unit later in this curriculum", hot streams and
sharing to "the hot streams unit later in this curriculum", lifecycle and Compose collection
to "the effects and lifecycle integration material later in this curriculum".

E24-06 was not started, and no Unit 5 operator, timing or failure material, no Unit 6
hot-stream material, no Compose-effect material and no Question was authored.

## Authoring outcomes for Unit 5

E24-06 authors `unit_flow_composition_timing_and_failure` immediately after Unit 4 in
`learning_curriculum.json`. Everything below was executed, opened or read during that work
on 2026-09-10; nothing here is recalled from the E24-01 review, and every annotation and
default was read from this repository's **resolved** `kotlinx-coroutines-core:1.11.0`
sources jar rather than from a published API page.

### What did not change

All five proposed Lesson ids, titles, authored order and exact primary/supporting mappings
shipped verbatim, as did the Unit id, title and `async_reactive` home Topic. No Lesson was
renamed, reordered, split or merged. No operator was added to or dropped from the set the
plan named, so the acceptance criterion requiring a documented justification for such a
change does not apply. No Question, taxonomy record, earlier E24 Unit or Compose Unit
changed — the diff outside this Unit is the two tests that pin Unit and Lesson counts and
the generated coverage snapshot.

`flow_operators` is deliberately primary in L5.1, L5.2 and L5.3, which the plan's
[shared-primaries](#identity-and-mapping-checks-performed) note anticipated. Unit practice
therefore reaches **three** concepts — `flow_operators`, `flow_buffering`, `flow_errors` —
and **seven** ACTIVE Questions, each counted once. That is asserted in the integration test
rather than left to the generated snapshot. Each Lesson carries Core, Practical and Senior
depth and runs 1,155–1,235 words, just above the 1,063–1,194 range Unit 4 occupies and
inside the range the shipped Compose Units span.

### The two E24-05 corrections, carried forward

Both were live risks in this Unit rather than theoretical ones, because Unit 5 is where the
internal coroutines actually appear.

- **Coldness is about when production starts, not about how many coroutines an
  implementation uses.** L5.4 states outright that it is not true that a Flow has no
  buffer, and names the three places in the curriculum where one exists: `flowOn` across a
  dispatcher change, `channelFlow` by construction, and the buffering operators themselves.
  L5.3 describes `flatMapMerge` collecting several inner flows at once without ever
  suggesting that this is a second lifetime — the inner flows are cancelled through the
  ordinary hierarchy, which is what makes `flatMapLatest`'s cancellation ordinary
  cancellation. Neither Lesson repeats the over-broad claim Unit 4's review removed.
- **Intermediate against terminal is semantic, not a memorised list.** No Lesson here
  enumerates terminal operators. L5.1 relies on the distinction only to say that `onEach`
  alone starts nothing, and L5.4 uses `collectLatest` as a terminal operation without
  needing a list to justify it.

### Boundaries held, and how

Every boundary was tested against the finished prose rather than assumed from the outline.

| Boundary | What ships |
| --- | --- |
| Unit 6 owns hot streams and state | `StateFlow`, `MutableStateFlow`, `SharedFlow`, `replay`, `stateIn`, `shareIn` and `SharingStarted` are never named. `stateflow` is supporting in L5.2 only, and the Lesson's one forward gesture is a prose paragraph saying that a stream which always has a current value is a different kind of stream, owned by "the hot streams and state unit later in this curriculum". L5.4 makes one bounded factual reference — `conflate` has no effect on a stream that already conflates by construction — without naming or teaching the type |
| E25 owns Compose collection | `collectAsState`, `collectAsStateWithLifecycle`, `LaunchedEffect`, `rememberCoroutineScope`, `produceState`, `repeatOnLifecycle` and `flowWithLifecycle` are never named. Search-as-you-type is used as a domain example in L5.1 and L5.3 and no Compose mechanics appear around it |
| Architecture stays supporting | `error_modeling` earns exactly one bounded bridge in L5.5: `catch` may emit a fallback, that changes the downstream contract, and whether an expected failure is better modelled as a value than an exception is named as the architecture curriculum's question. No `Result` type, sealed UI state, MVI, MVVM or repository error policy appears |
| E31 owns testing | Turbine, `runTest`, virtual time and `TestDispatcher` are never named, even though every timing operator in L5.1 and L5.4 invites a test example. The deterministic probes used to verify the published outputs were throwaway authoring validation and were deleted |
| Kotlin stays supporting | `kotlin_lambdas` is present only as the fact that `map`, `filter` and `transform` take suspending lambdas; `kotlin_equality` only as the fact that `distinctUntilChanged` compares with `equals`. Neither becomes a lesson on lambdas or on equality — the full equality argument belongs to Unit 6's `StateFlow` conflation |
| KMP accuracy | Every code example is common Kotlin. No JVM-only API, no Android lifecycle assumption and no thread claim appears. L5.3 states explicitly that the concurrency `flatMapMerge` provides is the coroutine kind Unit 2 established and not a promise of parallel execution, which is the one place the Unit 2 distinction was at risk |

### The transform-and-filter decision model as taught

L5.1 is organised around one question — what does this operator need in order to decide? —
rather than around the six operators. `map`, `filter` and `transform` need only the value
and differ only in how many outputs one input may produce. `distinctUntilChanged` needs the
value and the one immediately before it. `debounce` and `sample` need the clock. The Lesson
states that an operator can only be correct for a problem whose decision uses the same
inputs, which is what makes the search-field pairing — `debounce` first for timing noise,
`distinctUntilChanged` second for the repeated query — a semantic argument rather than a
recipe. `take`, `drop`, `onStart`, `onEach` and `scan` are one Reference paragraph in the
Senior section, as the plan's boundary required, and no operator has a section merely
because it exists.

### The `DEFAULT_CONCURRENCY` decision

E24-01 deliberately left the numeric value unestablished and told L5.3 either to read it or
to name it symbolically. It was read. In the resolved artifact:

```kotlin
public const val DEFAULT_CONCURRENCY_PROPERTY_NAME: String = "kotlinx.coroutines.flow.defaultConcurrency"

@FlowPreview
public val DEFAULT_CONCURRENCY: Int = systemProp(DEFAULT_CONCURRENCY_PROPERTY_NAME, 16, 1, Int.MAX_VALUE)
```

The value is **16**, and the declaration's own KDoc adds the qualification that makes it
safe to teach: it can be changed on the JVM through that system property. L5.3 states the
number with the qualification attached, because the number is not the point — the
*existence of a bound* is, and a reader who knows only that the parameter defaults to
`DEFAULT_CONCURRENCY` cannot reason about what an unbounded fan-out would cost. It was
verified against the declaration and by reading it back at runtime, not remembered. Note
that the constant carries `@FlowPreview`, so it is one of the three declarations in this
Unit with the stronger opt-in.

### Claims that were executed rather than reasoned about

Throwaway JVM probes ran each of these against the resolved `kotlinx-coroutines-core:1.11.0`
and were then deleted. Every number a Lesson quotes is from this table and is presented in
the prose as a measurement rather than as an API guarantee.

| Claim a Lesson makes | Measured result |
| --- | --- |
| `distinctUntilChanged` compares only with the immediately previous value (L5.1) | `flowOf("A","A","B","A").distinctUntilChanged()` produced **A, B, A** — the final A survived |
| `debounce` and `sample` are different contracts, on one identical timeline (L5.1) | The typing timeline in the Lesson, run six times with identical results: `debounce(300)` produced **kot, kotlin**; `sample(300)` produced **kot, kotl, kotlin**. The extra value is a string the user never paused at, which is the whole distinction. An earlier timeline put the last keystroke roughly sixty milliseconds before a sampling tick and was rejected for that reason — the published one leaves margins of over a hundred milliseconds on both sides, and the Lesson states that the exact middle value is a property of where emissions fall relative to the ticks rather than a guarantee |
| `debounce` cannot see a repeated query, `distinctUntilChanged` cannot see time (L5.1) | A user who typed `kot`, paused, added and deleted a character, and paused again: `debounce(300)` alone emitted **kot twice**; `debounce(300).distinctUntilChanged()` emitted it **once** |
| `transform` may emit zero, one or many outputs per input (L5.1) | `flowOf(1,2,3).transform { if (it % 2 == 0) { emit(…); emit(…) } }` produced **two** values from **three** inputs |
| A suspending lambda inside `map` is ordinary (L5.1) | `flowOf(1,2).map { delay(10); it * 10 }` produced **10, 20** |
| `combine`'s startup rule and steady-state rule (L5.2) | The spaced timeline in the Lesson produced **(U1,S1) at ~200 ms, (U1,S2) at ~400 ms, (U2,S2) at ~600 ms** across three runs — nothing before the second source's first value, then one result per emission |
| `combine` emits nothing if a source never emits (L5.2) | A working source combined with one that completed without emitting produced an **empty** list, not a partial one |
| `zip` pairs and completes when either side runs out (L5.2) | Three values zipped against two produced **(A1,B1), (A2,B2)**; a four-value source zipped against a two-value one emitted **two pairs** while the faster upstream had produced **three** values — one was produced, found no partner and was discarded |
| `merge` interleaves without combining (L5.2) | The two sources above merged produced **U1, S1, S2, U2** |
| The three flattening strategies on one timeline (L5.3) | `flatMapConcat` → **A-1, A-2, B-1, B-2**; `flatMapMerge` → **B-1, A-1, B-2, A-2**; `flatMapLatest` → **B-1, B-2**. Same source, same inner flows, three orders |
| `flatMapLatest` cancels an inner flow that has already done work (L5.3) | Counting inner flows on the same timeline: `concat` started **2** and completed **2**; `merge` started **2** and completed **2**; `latest` started **2** and completed **1** — the cancelled inner flow had already executed its first statement |
| `DEFAULT_CONCURRENCY` (L5.3) | **16**, read back at runtime, matching the declaration |
| The default is backpressure, not loss (L5.4) | The Lesson's producer and collector, with no operator: **1, 2, 3, 4, 5** in **2,676 ms** — the collector's pace, not the producer's |
| `buffer` decouples the two ends and keeps every value (L5.4) | Same pair with `.buffer()`: **1, 2, 3, 4, 5** in **1,870 ms** |
| `conflate` drops unread values and protects work in progress (L5.4) | Same pair with `.conflate()`: **1, 3, 5** in **1,101 ms** — 2 and 4 were produced and never seen; the block processing 1 was not disturbed |
| `collectLatest` cancels the block rather than dropping values (L5.4) | Same pair with `collectLatest`: **all five started**, **only 5 finished**, in **1,124 ms** |
| `catch` does not see a failure thrown by `collect` (L5.5) | The `catch` block received **nothing** and the exception propagated out to the caller of `collect` |
| `retry` re-collects the upstream and re-runs a cold producer (L5.5) | A producer failing its first two attempts under `retry(2)`: the producer ran **3** times and the collector received **1, 2, 3** — including the values emitted by the two attempts that then failed |
| `retryWhen`'s attempt index is zero-based (L5.5) | A flow that failed every time, giving up at attempt 2, saw the indexes **0, 1, 2** |
| `onCompletion`'s cause across four endings (L5.5) | Normal completion → **null**; producer threw → **IllegalStateException**; `collect` block threw → **IllegalStateException** (a downstream failure, which `catch` would not have seen); collecting coroutine cancelled → **JobCancellationException** |

### How `combine` was verified, and the one honest caveat

The `combine` timeline in L5.2 was not written from the mental model and then checked. It
was run first, and the first version of it was wrong.

With the sources' emissions two hundred milliseconds apart, three runs produced all three
combined values in the order the model predicts, and that is the timeline the Lesson
publishes. With the same emissions compressed to a hundred milliseconds, two of three runs
produced only **two** values — the intermediate combination was skipped rather than delayed.
The documentation's own worked example on the `combine` KDoc, whose sources are ten and
fifteen milliseconds apart, is annotated as printing `1a 2a 2b 2c`; it printed `1a 2b 2c`
here, consistently, across three runs.

Reading `combineInternal` in the resolved artifact explains it exactly. Updates are received
in batches, and a batch bails out only "as soon as we encountered two values from the same
source" — so updates from *different* sources that are already pending are folded into a
single combined result carrying the latest of each. A follow-up probe confirmed the
complement: three values bursting from **one** source against a stable other source produced
**three** results in all three runs, because the per-source collector yields after each send.

The Lesson teaches the reliable timeline in Core and puts the qualification in Senior, framed
as the operator's actual contract rather than as a defect: `combine` is specified in terms of
the latest value of each source, not as one output per upstream emission, and that is the
property that makes it right for screen state and wrong for anything that must observe every
change. This is deliberately **not** written up as a documentation error — the KDoc example's
ten- and fifteen-millisecond delays are inside JVM timer granularity, so the difference is
explained by scheduling rather than by the page being wrong — but the L5.2 prose does not
repeat the guide page's "emits a new value when any upstream flow emits a value" as an
unqualified guarantee.

### The conflation capacity discrepancy, recorded rather than resolved

The `conflate()` contract was checked in three places and two of them disagree on one number.

| Source | What it says |
| --- | --- |
| `conflate()` in the resolved artifact | `buffer(CONFLATED)`, and `buffer` desugars `CONFLATED` to `capacity = 0, onBufferOverflow = DROP_OLDEST` |
| The `buffer` KDoc in the same artifact | `CONFLATED` "is a shortcut to `buffer(capacity = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST)`", and a non-`SUSPEND` overflow strategy "implicitly creates a channel with at least one buffered element" |
| `coroutines-flow-operators.html`, live on 2026-09-10 | "the `.conflate()` operator, which is a shorthand for `buffer(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)`" |

The two are reconcilable — a capacity of zero with a drop-oldest strategy implicitly gets one
buffered element — but they are not the same sentence, and quoting either number invites a
reader to contradict it with the other. **L5.4 therefore states the semantics and not the
number**: `conflate` is `buffer` with a drop-oldest overflow strategy and the smallest buffer
that strategy needs. Everything the Lesson actually teaches about `conflate` — never suspend
the emitter, replace a pending older value with a newer one, do not cancel processing already
started — is stated identically by all three sources and was measured besides. E24-07 and
E24-08 should not "fix" this into a number without re-reading both.

### Source freshness, re-checked on 2026-09-10

Every page below was opened rather than recalled, and every source URL cited by a Unit 5
Lesson — twenty-two distinct URLs — was requested and returned 200.

| Page | State on 2026-09-10 | Consequence |
| --- | --- | --- |
| `coroutines-flow-operators.html` | Title still "Flow operators". Structure unchanged from what E24-01 and E24-05 recorded | Supplies the default-backpressure claim L5.4 opens with, the "doesn't cancel processing that has already started" clause that separates `conflate` from `collectLatest`, and the `zip` completion clause. All three re-read on the page. Its `conflate` capacity figure is the one discrepancy above |
| `coroutines-flow.html` | Title still "Flows" | Not cited by Unit 5; the operator-level material this Unit needs is all on the operators page or in KDoc |
| `exception-handling.html` | Title still "Coroutine exceptions handling" | Cited by L5.5 for the cancellation-is-not-failure model Unit 3 established, which L5.5 preserves rather than restates |
| `coroutines-cancellation.html` | Title still "Cancellation and timeouts" | Cited by L5.3 for the cooperative-cancellation model the side-effect warning depends on |
| `debounce`, `sample`, `distinctUntilChanged`, `transform`, `combine`, `zip`, `merge`, `flatMapConcat`, `flatMapMerge`, `flatMapLatest`, `DEFAULT_CONCURRENCY`, `buffer`, `conflate`, `collectLatest`, `flowOn`, `catch`, `retry`, `retryWhen`, `onCompletion` KDoc | All current, all 200 | E24-01's instruction that the API reference is the authority for the operators the rewritten guide pages no longer cover held throughout. `debounce`, `sample`, the flattening operators, `retry`, `retryWhen` and `onCompletion` are cited to KDoc for exactly that reason |

No page was found rewritten since E24-05 recorded its state, so open question 4 in
[Unresolved questions](#unresolved-questions) is unchanged.

### The library's own discouragement, taught rather than suppressed

Both `flatMapConcat` and `flatMapMerge` carry an unusual KDoc note: their usage is
"discouraged in a regular application-specific flows", because "most likely, suspending
operation in `map` operator will be sufficient and linear transformations are much easier to
reason about". Repeating a Lesson's operators while suppressing their own documentation's
reservation would be dishonest, so L5.3 states it and draws the design consequence: if each
value needs one asynchronous result rather than a stream of them, a suspending `map` is the
simpler answer and no flattening is required at all. The note does not apply to
`flatMapLatest`, which has no equivalent clause, and the Lesson does not extend it there.

### The latest-value side-effect warning, exactly as taught

This is the acceptance-critical part of L5.3 and it is built on a measurement rather than on
a caution. Two inner flows started, one was cancelled, and both had already executed their
first statement before the cancellation arrived. The Lesson's rule follows from that:

> Cancellation stops work from continuing. It does not undo work already performed.

So the question the learner is taught to ask is not whether newer input supersedes older
input, but whether the work started for the older input is safe to abandon halfway through —
with the two lists the issue named, and the closing statement that making an operation
disposable is a design problem the operator does not solve. L5.4 makes the same argument for
`collectLatest` from the other direction: the collector block may be cancelled between any
two suspension points, so rendering a preview is safe and a durable write is not.

Neither Lesson implies that cancellation rolls anything back, and neither presents
`flatMapLatest` as the default UI operator.

### The `catch` boundary and `onCompletion`, as taught

L5.5 is organised around position in the chain rather than around three APIs. The
`catch`-does-not-see-`collect` case is shown as an annotated chain with the boundary drawn
between the lines, then measured, then explained by exception transparency in the KDoc's own
words. Both fixes the plan required are given, and the Lesson explicitly warns against the
third, wrong one — relocating arbitrary collector work upstream merely so `catch` can see it,
which changes what the stream is responsible for and puts side effects into a chain that may
later be retried.

`retry` is taught as re-collection of the upstream rather than as re-running a line, and the
measurement carries two consequences the plan asked for: the producer's side effect ran three
times, and the values emitted by the failed attempts had already been delivered downstream,
so retrying does not un-emit them. `retry()` with no argument is called out as an unbounded
loop, since its default is `Long.MAX_VALUE`. Backoff appears as one worked `retryWhen`
example and does not become a resilience section.

`onCompletion` gets the four-ending table above and the analytics anti-example the issue
named, with the fix stated as the KDoc's own idiom — test the cause for `null`. Cancellation
gets its own paragraph saying that neither `catch` nor `retry` may consume it, which preserves
Unit 3's model unchanged. The Unit closes on the exception-transparency-as-context-transparency
bridge Unit 4 left half-finished, explicitly labelled as a mental bridge rather than a formal
equivalence, with local readability named as the property both restrictions protect.

### Semantic review of the Questions this Unit now reaches

All seven ACTIVE Questions reachable through Unit 5's three primary concepts were read in
full — stem, options, key, explanation and Sources — against the finished prose. No Question
was changed; E24-08 owns assessment.

| Question | Verdict against the shipped Lessons |
| --- | --- |
| `flow_debounce_vs_distinct_until_changed` | Answerable from L5.1's Core and Practical. Its key is exactly the Lesson's decision-inputs framing, and each distractor is addressed by name: the swapped definition, comparison by identity against `equals`, and the fixed-window behaviour the Lesson attributes to `sample`. The Lesson argues from the measured timeline rather than echoing the option wording. Note that the question's own explanation uses `sample` only as a distractor gloss, which is what GAP-U5-C below records |
| `flow_combine_vs_zip_emission_rule` | Answerable from L5.2 in full. Its key carries both halves of `combine`'s rule — after both flows emit once, latest values when either emits — and both are the Lesson's Core. Its user-and-settings framing is the same domain the Lesson uses, which was checked for wording overlap rather than assumed: the Lesson derives the choice from the state-against-correspondence question and never restates an option. One option is worth flagging: distractor D's second clause, that `zip` cancels the remaining flow when one completes, is **true** — it is the `zip` KDoc's own wording — but its first clause is false for both operators, so the option is correctly wrong and the key is unaffected |
| `flow_flat_map_latest_search_cancellation` | Answerable from L5.3's Practical. Its key and its three distractors are exactly the four behaviours the Lesson's axis table distinguishes, including the `conflate` distractor, which L5.4 then separates from cancellation properly. This is the Question the Unit is best matched to, and it is also the one that establishes GAP-U5-A: `flatMapConcat` and `flatMapMerge` appear only as distractors, so answering it correctly does not demonstrate a choice between them |
| `flow_conflate_vs_collect_latest` | Answerable from L5.4's Practical and Senior. Its key is the pair the Lesson's strongest comparison is built on, and its explanation's closing sentence — whether a partially processed value can safely be abandoned — is the Senior section's organising question, reached independently. The measured `1, 3, 5` and `started all five, finished one` outputs make both halves concrete in a way the Question's prose does not |
| `flow_buffer_producer_consumer_concurrency` | Answerable from L5.4's Practical. Its key names the trade the Lesson states — concurrent progress up to capacity, memory for decoupling — and its three distractors are the other three operators in the Lesson, each taught separately. Note that answering it requires knowing what the *default* is, which the Lesson establishes before any operator; that ordering was a plan boundary and it is what makes this Question answerable rather than guessable |
| `flow_catch_upstream_only` | Answerable from L5.5's Core. Its key is the Lesson's boundary rule and its explanation names both fixes, which the Lesson gives independently and then extends with the ownership caveat. Its single Source cites `coroutines-flow.html` under the old title "Asynchronous Flow"; the URL still resolves and the page still supports the claim, so this is the stale citation *label* E24-05 already recorded, not a new defect |
| `flow_retry_when_conditional_attempts` | Answerable from L5.5's Practical. Its key is the cause-and-attempt predicate, which the Lesson teaches with the measured zero-based indexes, and its `catch` distractor is the distinction the Lesson draws between terminating an error and resubscribing. What the Question does **not** assess is what resubscription costs — that a cold producer runs again — which is GAP-U5-D below |

### The known gaps were not filled

E24-06 authored no Question. GAP-U5-A and GAP-U5-B are unchanged as gaps; what changed is
that the reasoning each describes is now taught, so E24-08 has something to assess against.

| Gap | Where the reasoning now lives |
| --- | --- |
| GAP-U5-A — `flatMapConcat` against `flatMapMerge` on ordering and overlap | L5.3 in full: the three-axis table in Core, the single measured timeline that gives all three operators a different output, the two scenarios that make sequential right and interleaved right, the explicit refusal to call merge "the fast one", and the bounded-concurrency paragraph that gives the overlap axis a cost |
| GAP-U5-B — what `onCompletion` observes, and why completion is not success | L5.5's Senior section in full: the four-ending measured table, the analytics anti-example identified as a defect rather than an idiom, the KDoc's null-cause idiom as the fix, and the cancellation paragraph that explains why the third and fourth endings exist at all |

### New candidate gaps for E24-08

Two further gaps surfaced while authoring. Both are candidates for E24-08 to decide on
deliberately, and neither is a defect in an existing Question.

| Gap | Unit / Lesson | Reasoning no ACTIVE Question assesses | Why it is substantive | Recommended action |
| --- | --- | --- | --- | --- |
| GAP-U5-C | Unit 5 / `lesson_transforming_and_filtering_flows` | Choosing between `debounce` and `sample` — quiet period against fixed cadence — and predicting what each produces from one timeline | `flow_debounce_vs_distinct_until_changed` uses `sample` only as a one-clause distractor gloss. The two timing operators are the Lesson's central decision and the pair a reader is most likely to conflate, and nothing in the bank makes anyone choose between them. The measured timeline in L5.1, where `sample` emits a value the user never paused at, is the kind of case a question could be built on | Add coverage in E24-08 |
| GAP-U5-D | Unit 5 / `lesson_flow_failure_and_completion` | That `retry` re-collects the upstream, so a cold producer's side effects run again and values already emitted by a failed attempt have already been delivered downstream | `flow_retry_when_conditional_attempts` assesses which operator expresses a bounded conditional retry, which is the API choice. What retrying *costs* — the reason retry is only appropriate where repeating the upstream operation is safe — is unassessed, and it is the half that decides whether the operator may be used at all | Add coverage in E24-08 |

One observation that is **not** a gap. `async_reactive`'s Flow half still holds no ACTIVE
`ADVANCED` Question, and Unit 5's seven are four Foundation and three Applied. E24-01 recorded
that as an observation rather than a quota and this issue treated it as one: no Lesson's scope
was widened or narrowed to justify a level, and E24-08 should decide from the reasoning
complexity of whatever it writes. What is now true that was not before is that the material an
Advanced Unit 5 Question would need exists — `combine`'s state-not-events contract, the
cancellation-does-not-undo argument, the `conflate`-against-`collectLatest` decision, and the
four endings of `onCompletion` are all taught at Senior depth.

### Cross-links and validation

Backward-only, and every target already ships:

- L5.1: `lesson_cold_flows`.
- L5.2: `lesson_cold_flows`, `lesson_transforming_and_filtering_flows`.
- L5.3: `lesson_cooperative_cancellation`, `lesson_sequential_and_concurrent_work`,
  `lesson_flow_collection_lifetime`, `lesson_transforming_and_filtering_flows`.
- L5.4: `lesson_flow_collection_lifetime`, `lesson_flow_context_and_flow_on`,
  `lesson_cooperative_cancellation`, `lesson_flattening_flows`.
- L5.5: `lesson_exception_propagation`, `lesson_cooperative_cancellation`,
  `lesson_flow_context_and_flow_on`, `lesson_flow_buffering_and_conflation`.

Every candidate link the issue proposed was taken. The four within-Unit links are the
progression itself — filtering into combining, filtering into flattening, flattening into
buffering, buffering into failure — and each one is where a reader who followed it would find
the concept the prose just leaned on. No link points into an unshipped Unit, and no shipped
Lesson was edited to receive one. Every forward reference is prose naming a unit: hot streams
and current-value state to "the hot streams and state unit later in this curriculum", and
error modelling to "the architecture curriculum".

Validation run, all passing: `python3 tools/learning_question_coverage.py --check`;
`./gradlew :shared:jvmTest --tests "*BundledLearningCurriculumTest*" --tests
"*LearningContentEndToEndTest*" --tests "*LearningCurriculumJsonCodecTest*"`;
`./gradlew :shared:jvmTest --tests "*LearningUnitPracticeIntegrationTest*" --tests
"*LearningProductionContentJourneyTest*" --tests "*LearningReaderJourneyIntegrationTest*"
--tests "*ProgressLearningJourneyIntegrationTest*"`; and `./gradlew :shared:allTests`.
`LearningCurriculumValidator` caught one real defect during authoring — a comparison block
in L5.3 with a blank first header — which is the machine-checkable half of the authoring
contract doing its job.

E24-07 was not started, and no Unit 6 hot-stream material, no Compose-effect material and no
Question was authored.

## Authoring outcomes for Unit 6

E24-07 authors `unit_stateflow_sharedflow_and_hot_streams` immediately after Unit 5 in
`learning_curriculum.json`, closing the authoring half of this epic. Everything below was
executed, opened or read during that work on 2026-09-10. Every API contract quoted was read
from the declaration in this repository's **resolved** `kotlinx-coroutines-core:1.11.0`
sources jar, and every measured claim was produced by a throwaway JVM probe against that same
artifact and then deleted.

### What did not change

The Unit id, title and `async_reactive` home Topic, all five Lesson ids, their authored order
and their exact primary and supporting mappings shipped verbatim from the identity tables. No
Lesson was reordered, split or merged, and no Lesson from an earlier Unit, no Compose Lesson,
no Question and no taxonomy record was touched. Outside this Unit the diff is the two tests
that pin Unit and Lesson counts and reach, the generated coverage snapshot, this document plus
the blueprint's status and two of its headings, and one production fix that the added Lessons
surfaced — see [the study-state race](#the-study-state-race-this-unit-surfaced).

`hot_vs_cold_streams` is deliberately primary in both L6.1 and L6.5, which the plan's
[shared-primaries](#identity-and-mapping-checks-performed) note anticipated. Unit practice
therefore reaches **four** concepts — `hot_vs_cold_streams`, `stateflow`, `sharedflow`,
`flow_sharing` — and **six** ACTIVE Questions, each counted once. That is asserted in
`LearningUnitPracticeIntegrationTest` rather than left to the generated snapshot. Each Lesson
carries Core, Practical and Senior depth and runs 1,236–1,626 words after the review
corrections below, which added measured material to L6.3, L6.4 and L6.5. L6.3 is the longest: it
carries delivery, the whole buffering configuration, and the `replay = 1` against `StateFlow`
distinction that round four added. L6.4 is next, with sharing, three policies, two timers and
upstream completion. That is the load the plan's five-Lesson justification for this Unit already
anticipated, and neither was split — splitting L6.4 would separate the policy from the scope
that gives it meaning. Both sit inside the range the shipped document already spans, where six
Lessons are longer and the maximum is 2,169 words.

### The two title corrections, and why they are not scope changes

L6.4 shipped as "Sharing Cold Flows with `stateIn` and `shareIn`" rather than "`stateIn`,
`shareIn` and `SharingStarted`", and L6.5 as "Choosing a Stream Abstraction by Delivery
Guarantees" rather than "Choosing Between a Value, a Flow, a State Holder and a Channel".
Neither Lesson's content, objective, mapping or boundary moved: L6.4 still teaches all three
`SharingStarted` policies at the depth the plan specified, and L6.5 still decides among
exactly the five abstractions the old title listed. The planned titles were API inventories,
and Rule 5 of the authoring contract asks for the decision to lead. The identity tables above
and the blueprint headings were updated in the same change, so nothing drifts.

### The three axes, kept apart deliberately

L6.1's accuracy requirement — that hotness, retention and start/stop policy are three
independent axes — turned out to be the right organising principle for the whole Unit rather
than one Lesson's caveat, and it is what most of the authoring decisions below defend.

- **L6.1 owns production lifetime only.** It states the definition in the KDoc's own words —
  a flow is hot because its active instance exists independently of the presence of
  collectors — then names the other two axes and hands them to the Lessons that own them. It
  carries a three-row table doing exactly that.
- **Retention is L6.2's and L6.3's.** L6.1 answers "what does a late subscriber receive?"
  with "that is not decided by hotness", shows all three outcomes for the same timeline, and
  refuses to generalise.
- **Start and stop policy is L6.4's.** L6.1 states that a shared stream can stop its upstream
  while remaining hot, and points forward in prose.

The over-general sentences the plan warned about are absent, and their absence is enforced by
prose that names them as mistakes: "a hot flow throws away anything emitted while nobody is
listening" and "a hot flow replays what you missed" appear together in a `COMMON_MISTAKE`
callout as two retention configurations mislabelled as definitions of hotness.

### The guide page's hot-flow framing is looser than the KDoc

A source finding worth recording, because it would have produced exactly the collapse the
plan warned against. The rewritten `coroutines-flow.html` "Hot flows" section says hot flows
"keep emitting values even when no collector is active". Taken as a definition that is false
for `shareIn(scope, WhileSubscribed())`, whose upstream is stopped precisely when no
subscriber is active — measured below. The Lessons therefore take the definition from the
`StateFlow` and `SharedFlow` KDocs, which say only that the active instance exists
independently of the presence of collectors, and cite the guide page for the cold/hot split
and the `SharedFlow` and `StateFlow` usage material it does cover well. **Do not paraphrase
the guide's hot-flow sentence.**

### Claims that were executed rather than reasoned about

Every number below was measured on this project's JVM target against the resolved
`kotlinx-coroutines-core:1.11.0`, with probes that were deleted afterwards. Wall-clock
figures are evidence for a semantic claim, not published guarantees.

| Probe | Result | What it settles |
| --- | --- | --- |
| Collector attached to a `MutableStateFlow` whose value had already moved on | Received the current value immediately, matching `value` | L6.2's current-value contract |
| `data class` state, three spaced assignments of an equal value | **One** delivery — the initial one | Equality-based conflation as behaviour |
| Same shape with a class inheriting identity equality | **Four** deliveries — the initial value plus one per assignment | That the state type's `equals` is what decides |
| Object mutated in place 1 → 99, then an equal instance assigned | Collector saw the value **once**, at subscription; `value.count` read 99 afterwards | The mutable-state failure mode, stated without inventing a guarantee |
| Value assigned 1…10 at 20 ms with a 200 ms collector | Collector observed **0, 7, 10** | Slow-collector conflation, ending on the latest |
| 8 coroutines × 2,000 increments through `value = value + 1` | **5,998** of 16,000 | Why read-modify-write is not atomic |
| The same load through `update { }` | **16,000** exactly | `update`'s compare-and-set contract |
| `replay = 0`: emit A with nobody subscribed, subscribe, emit B | Subscriber received **B only** | Replay-zero retention |
| `replay = 1`: same sequence | Subscriber received **A then B** | Replay as the only thing a late subscriber can see |
| Unbuffered `emit` × 3 with one 300 ms subscriber | Returned at ≈109, 414, 718 ms | That `emit` waits for subscribers to take the value |
| Unbuffered `emit` × 1,000 with **no** subscribers | ≈1 ms, replay cache empty | That absent subscribers mean no backpressure and total loss |
| Six values, 20 ms apart, one 300 ms subscriber, default `SUSPEND` | All six delivered | The baseline the dropping case is measured against |
| The same with `extraBufferCapacity = 1` and `DROP_OLDEST` | **1 and 6** delivered, subscriber present throughout | That broadcast is fan-out, not a delivery guarantee |
| Consecutive `tryEmit` successes against a present 400 ms subscriber, over seven `replay`/`extraBufferCapacity` pairs | **`replay + extraBufferCapacity`** every time, including 1 slot at `replay = 1, extra = 0` | That `replay` is also buffer for a present slow subscriber |
| Five queued `Channel` elements, one worker cancelled part-way through the third | Worker took 1–3 and finished 1–2; a replacement receiver drained 4 and 5; **3 was never redelivered** | That a `Channel` gives single-receiver delivery, not handling |
| Fresh `MutableSharedFlow(replay = 1)` against fresh `MutableStateFlow(initial)`, subscriber arriving before any emission | **Nothing** against the **initial value**; `resetReplayCache()` empties it again | That `replay = 1` is not a current-value contract |
| `stateIn(scope, Lazily, -1)` with no subscriber yet | Object present and `value` readable; upstream started **zero** times until the first subscriber arrived | That a hot stream can exist with production not started |
| Returning subscriber after a `WhileSubscribed()` stop, upstream needing 250 ms to produce | `stateIn` delivered the retained value at **4 ms**; `shareIn(replay = 0)` delivered **nothing until 258 ms** | That "served immediately from what is retained" is `stateIn`-only |
| Six values emitted with no subscriber at `replay = 0, extraBufferCapacity = 3` | Late subscriber received **nothing** | That extra capacity retains nothing for an absent subscriber |
| `replay = 2`, ten emissions, no subscribers | Replay cache held the last two | That overflow strategy has no effect with no subscribers |
| `tryEmit` on an unbuffered flow with no subscribers | `true`, replay cache still empty | That `true` is not evidence of delivery |
| `tryEmit` on the same flow with one slow subscriber | `false` | The documented `false` condition |
| `tryEmit` × 5 on `replay = 1` with a slow subscriber | `true`, then four `false` | Buffer capacity, not delivery |
| Three subscribers to a cold upstream shared with `shareIn` | Upstream started **once** | What sharing buys |
| Two direct collections of the same cold flow | Builder ran **twice** | The baseline sharing removes |
| `Eagerly` + `shareIn(replay = 0)`, subscriber joins after six values | First value seen was the **seventh** | The KDoc's "immediately discarded" clause |
| The same at `replay = 2` | Subscriber received the two most recent, then live values | Replay against eager production |
| Upstream that emits once and completes, under `WhileSubscribed()` | Collector still active afterwards; state still read that value | "Normal completion has no effect on subscribers" |
| Subscriber returning after a `WhileSubscribed()` stop | Received the **retained** value first, then the restarted upstream's first value | That a stop does not send a returning subscriber back to `initialValue` |

### The `SharingStarted` probe, in full

One cold source counting its own starts and stops, one subscriber that arrives and later
leaves, five configurations. This table is the evidence behind L6.4's Practical section.

| Policy | Before any subscriber | While subscribed | 150 ms after the last subscriber left | 750 ms after | On re-subscribe |
| --- | --- | --- | --- | --- | --- |
| `Eagerly` | started, already producing | running | running, nothing stopped | running | joins the run in progress |
| `Lazily` | not started | running | running, nothing stopped | running | joins the run in progress |
| `WhileSubscribed()` | not started | running | **stopped**; last value retained | stopped; value retained | **restarted from the beginning** |
| `WhileSubscribed(500)` | not started | running | still running; value advanced | **stopped** | restarted |
| `WhileSubscribed(0, replayExpirationMillis = 200)` | not started | running | stopped; last value retained | stopped; **value reset to `initialValue`** | restarted |

Three conclusions the Lesson teaches from it. `Eagerly` and `Lazily` differ only in when they
start — neither ever stops, so "lazy" does not imply the symmetric stop its name suggests.
`stopTimeoutMillis` moves the stop and nothing else. And `replayExpirationMillis` runs on its
own clock *after* the stop: the last row stopped at the same moment as the third and reset its
value 500 ms later, which is the cleanest available demonstration that the two timers are not
one.

### The defaults, read from the declaration

Both are surprising and both are stated in L6.4 as defaults rather than as advice.

- `WhileSubscribed(stopTimeoutMillis = 0, replayExpirationMillis = Long.MAX_VALUE)`. The
  documented gloss for the second is "keep replay cache forever, never reset buffer", and the
  measured behaviour matches: with a plain `WhileSubscribed()` the upstream stops immediately
  and the retained value survives indefinitely.
- The five-second figure is therefore **not** an API default and is taught as an application
  choice sized from how long the collector gaps actually are. `flow_state_in_while_subscribed`
  assesses what the parameter means; L6.4 adds why a particular number would be chosen.

### `Eagerly` was examined, not corrected

The repository's four `stateIn(scope, SharingStarted.Eagerly, ...)` state holders —
`ProgressStateHolder`, `InterviewHistoryStateHolder`, `MistakeReviewStateHolder`,
`AssessmentHistoryStore` — plus `AppShellViewModel`'s `viewModelScope` holder were read
before L6.4 was written. The four application-scoped ones run on `AppCoroutineScope`, which
is `SupervisorJob() + Dispatchers.Default` and lives as long as the process, and
`ProgressStateHolder`'s own documentation states the reason: the navigation entry destroys
the ViewModel on a tab switch, so the dashboard was rebuilt from nothing on every visit.
The first draft of L6.4 justified the choice by the scope's lifetime, and that was wrong twice
over — see [corrections made during review of Unit 6](#corrections-made-during-review-of-unit-6). L6.4 ships the
justification the code actually supports: the upstream is a derivation several screens read, so
keeping it running means the figures are computed before the first open and stay current across
gaps, at the cost of running while nobody looks. L6.4 uses this as its contrast case, teaches
`Eagerly` through its trade rather than as a mistake. **No state holder and no sharing policy in
this repository was changed**; the one production change in this issue's diff is unrelated to
`Eagerly` and is recorded below.

### The `SharedFlow` no-subscriber behaviour, as taught

This is the Unit's most counter-intuitive contract and the plan asked for it not to be
inferred. What the resolved KDoc says, and what L6.3 teaches:

- `emit` on an unbuffered shared flow "suspends until all subscribers receive the emitted
  value and returns immediately if there are no subscribers".
- `MutableSharedFlow.emit`'s own KDoc is more precise still: suspension happens only when the
  overflow strategy is `SUSPEND` **and** there are subscribers; with no subscribers "the
  buffer is not used" and the value is stored into the replay cache if one exists, or dropped.
- Buffer overflow "can happen only when there is at least one subscriber that is not ready to
  accept the new value"; with none, only the most recent `replay` values are stored and the
  overflow strategy has no effect at all.
- `tryEmit` returns `false` only under the same `SUSPEND`-plus-subscribers condition, so on an
  unbuffered flow `true` is exactly the no-subscriber case — which the class KDoc states
  outright, adding that the value is then "immediately lost".

L6.3 states all four, measures the first three, and attaches a `COMMON_MISTAKE` callout to the
`tryEmit` result specifically. It does not hide any of it behind an Android lifecycle rule.

### The `StateFlow` equality contract, as taught

L6.2 quotes the KDoc's own heading — strong equality-based conflation — and its statement that
values are conflated using `Any.equals`, then makes it behavioural with the three measured
cases in the table above. Three further clauses were read from the declaration and used:

- "State flow behavior with classes that violate the contract for `Any.equals` is
  unspecified." The Lesson cites this rather than inventing a guarantee about mutation, which
  the plan explicitly warned against.
- `compareAndSet`'s KDoc notes that when both the expected and the new value equal the current
  one it returns `true` **without** storing the new reference. This is in L6.2's Senior
  section as the identity caveat.
- `update`'s KDoc warns that the function "may be evaluated multiple times, if `value` is
  being concurrently updated", which is why the Lesson requires the lambda to be pure.

Completion is taught as the KDoc states it: state flow never completes, cannot be closed, can
never represent a failure, and errors must be materialised — so a failed load is a value in
the state type. Operator fusion is one sentence listing the no-ops, not a section.

### The `Channel` boundary held

L6.5's channel material is three paragraphs: the `BlockingQueue`-like model with suspending
`send`/`receive`, fan-out as "multiple coroutines may receive from the same channel,
distributing work between themselves", and the consequence that a queued element goes to one
receiver rather than to all subscribers. Pipelines, fan-in, `produce`, `select` and the
capacity taxonomy are named as excluded in one sentence. That is the same bounded treatment
the plan authorised and the contrast `flow_vs_channel_delivery_model` already assesses.

### The E25 and architecture boundaries held

No E24 Lesson names `collectAsState`, `collectAsStateWithLifecycle`, `LaunchedEffect`,
`rememberCoroutineScope`, `produceState` or `repeatOnLifecycle`. L6.2 says that `StateFlow` is
a Kotlin Multiplatform type that knows nothing about a UI, a lifecycle or a ViewModel, and
defers how a Compose screen collects one. L6.5 says that once the delivery guarantee is
chosen, later architecture material decides how a UI models and consumes it, and stops. The
private-mutable/public-immutable pair appears in L6.2 as API encapsulation with an explicit
sentence saying that where state should live is an architecture question this Unit leaves
alone. `state_ownership` and `lifecycle_coroutines` remain supporting-only, which
`BundledLearningCurriculumTest` now pins.

### Semantic review of the Questions this Unit now reaches

All six ACTIVE Questions reachable through Unit 6's four primary concepts were read in full —
stem, options, key, explanation and Sources — against the finished prose. No Question was
changed; E24-08 owns assessment.

| Question | Verdict against the shipped Lessons |
| --- | --- |
| `stateflow_001` | Answerable from L6.2's Core. Its key — the UI needs the latest value and updates when it changes — is the current-value contract, and its three distractors are each addressed by name: one-time events (L6.2's countable-things argument and L6.5's honest version), delivery of every intermediate value (the measured `0, 7, 10`), and starting without an initial value (stated as a consequence of the contract). The Question is **descriptive**, though: answering it demonstrates knowing what `StateFlow` is for, not predicting what a collector sees. That is GAP-U6-A |
| `stateflow_vs_sharedflow_current_value` | Answerable from L6.2 and L6.3 together. Both correct options are taught directly, and both distractors are contradicted explicitly — L6.2 states that a state flow does not buffer beyond one value, and L6.3 opens by defining shared flow as hot. Also descriptive on both correct options, which is the other half of GAP-U6-A |
| `shared_flow_replay_late_subscriber` | Answerable from L6.3's Practical, which measures exactly this scenario. Its key and all three distractors map onto sentences the Lesson derives independently: that the replay cache is the only thing a later subscriber sees, that always retaining the latest is `StateFlow`'s behaviour, and that a shared flow with no collectors neither queues nor refuses emissions. The best-matched Question in the Unit |
| `flow_share_in_vs_state_in` | Answerable from L6.4's Core, which draws the distinction as current-value against configurable replay rather than by return-type name. Its three distractors are all addressed: per-collector re-running is what sharing removes, the stop is decided by `SharingStarted`, and synchronous readability is `stateIn`'s side. The **mapping drift** E24-01 recorded is confirmed and unchanged: the Question sits on `sharedflow` while its reasoning is L6.4's `flow_sharing`. Both are Unit 6 concepts, so Unit practice is unaffected — but a `sharedflow`-scoped practice run gets a sharing question |
| `flow_state_in_while_subscribed` | Answerable from L6.4's Practical. Its key is the grace-period reading, which the Lesson states, measures and attaches a `COMMON_MISTAKE` callout to; its "polling interval" and "delays delivery" distractors are the two misreadings the callout names. What it does not assess is the choice between policies or the second timer — GAP-U6-B |
| `flow_vs_channel_delivery_model` | Answerable from L6.5's Practical. Its key is the one-receiver-per-element property, and its three distractors are each contradicted by a row of the Lesson's requirement table. Note that this is the **only** ACTIVE Question on `hot_vs_cold_streams`, so L6.1's own reasoning — production lifetime, late subscribers, values emitted with nobody subscribed — reaches no Question at all. That is GAP-U6-C |

`live_data_vs_state_flow_ui_state` was read as supporting context only, as the issue asked.
Its third statement remains the clearest existing description of `StateFlow` conflation in the
bank, and L6.2 teaches that behaviour far past it. The Question sits on `livedata`, which E24
leaves unmapped by design, so it creates no Unit 6 practice and must not be counted as
covering GAP-U6-A. `sharedflow_001` is DEPRECATED and was re-read so that E24-08 does not
re-ask what it already asked: it tested that `SharedFlow` is hot and that replay is
configurable, which is L6.3's Core.

### GAP-U6-A, GAP-U6-B and GAP-U6-C all still exist

E24-07 authored no Question. What changed is that the reasoning each gap names is now taught,
so E24-08 has something to assess against and measured material to build scenarios from.

| Gap | Status | Where the reasoning now lives |
| --- | --- | --- |
| GAP-U6-A — equality-based conflation as behaviour | **Still open.** Both `stateflow` Questions remain descriptive | L6.2's Practical in full: the KDoc's own heading quoted, the three-row equality table, the measured one-against-three delivery counts for `data class` against identity equality, the mutate-then-assign-equal case where the collector never learns of either change, and the measured `0, 7, 10` slow-collector run. The strongest candidate in the epic, and the material now supports a code-tracing question rather than a definition |
| GAP-U6-B — the sharing-policy decision | **Still open.** `flow_state_in_while_subscribed` still assesses one parameter of one policy | L6.4 in full: the five-configuration measured table, `Eagerly`'s discard clause, `Lazily`'s no-stop contract, the two separately drawn timers, both defaults read from the declaration, the five-second figure framed as an application choice, and this repository's own `Eagerly` holders as a defensible case |
| GAP-U6-C — hot against cold posed directly | **Still open.** `hot_vs_cold_streams`' single Question is the `Channel` comparison, which belongs to L6.5 | L6.1 in full: the definition from the KDoc, the late-subscriber timeline with all three retention outcomes, the measured cold-runs-twice against shared-runs-once pair, the measured eager-with-no-replay case where the first value a subscriber sees is the seventh, and the callout naming the two false generalisations |

### New candidate gaps for E24-08

Three surfaced while authoring. None is a defect in an existing Question, and each is a
decision for E24-08 to take deliberately.

| Gap | Unit / Lesson | Reasoning no ACTIVE Question assesses | Why it is substantive | Recommended action |
| --- | --- | --- | --- | --- |
| GAP-U6-D | Unit 6 / `lesson_shared_flow` | What an unbuffered `SharedFlow`'s `emit` does with and without subscribers, and that `tryEmit() == true` is not evidence anyone received the value | `shared_flow_replay_late_subscriber` assesses the late-subscriber half of delivery. The emitter's half is unassessed, and it is where "SharedFlow is for events" actually fails: the measured 1,000 emissions into an empty subscriber set completed in about a millisecond and were all lost. A strong ADVANCED candidate, in the half of the Topic that has none | Add coverage in E24-08 |
| GAP-U6-E | Unit 6 / `lesson_sharing_cold_flows` | That `stopTimeoutMillis` and `replayExpirationMillis` are two timers on two clocks, and that the second defaults to never | `flow_state_in_while_subscribed` teaches the first and does not mention the second. The measured pair of runs — same stop moment, reset 500 ms apart — is a ready-made tracing scenario. Overlaps GAP-U6-B and could be folded into it or split off | Add coverage in E24-08, possibly as part of GAP-U6-B |
| GAP-U6-F | Unit 6 / `lesson_choosing_a_stream_abstraction` | That a must-not-be-lost occurrence is not satisfied by any hot Flow configuration, so the answer is durable state, a queue or acknowledgement | `flow_vs_channel_delivery_model` assesses the single-receiver contrast, which is a different property. `durable_state_vs_one_off_event` in the `architecture` Topic is the closest and is supporting-only here, so it creates no Unit 6 practice. This is the reasoning the epic exists to replace the slogan with, and nothing in `async_reactive` assesses it | Add coverage in E24-08 |

### Corrections made during review of Unit 6

Sixteen defects were found by review of the shipped prose, across five rounds, and every one was
re-measured before being corrected. Nearly all are the same failure in different clothes: a rule
stated without the qualification that the Unit's own material supplies two paragraphs later.

#### Round one

**1. The identity-equality delivery count was wrong.** L6.2 said that three spaced assignments
of an equal value delivered one value for a `data class` and "three" for a class with identity
equality. Three is the count for *two* assignments; the probe behind the sentence made two, and
the prose said three. Re-measured at both sizes: two assignments give 1 against 3, three
assignments give **1 against 4** — the initial value plus one per assignment, since no two
instances compare equal. The Lesson now states the three-assignment pair and says outright that
it is the same code with a different `equals`.

**2. "Every current subscriber gets every emitted value" is not true under a dropping
strategy.** The `SharedFlow` KDoc's own sentence says all collectors get all emitted values, and
L6.3 repeated it as a `KEY_TAKEAWAY` — three paragraphs above teaching `onBufferOverflow`, whose
whole purpose is to drop values rather than suspend the emitter. Buffer overflow requires a
subscriber that is not ready, so the subscriber that loses the value is a *present* one.
Measured: six values 20 ms apart with one 300 ms subscriber delivered all six under the default
`SUSPEND` and **1 and 6** with `extraBufferCapacity = 1, DROP_OLDEST`, with the subscriber
present for both runs. L6.3 now teaches broadcast as **fan-out** — the value is offered to every
subscriber rather than consumed by one — and treats "does this subscriber receive every value?"
as the separate question the configuration answers. The measurement is in the Lesson.

**3. "Must the value survive having no subscribers?" was the wrong question.** L6.3's decision
table answered it "no shared flow configuration provides this", which contradicts `replay = 1`,
the Lesson's own measurement of it, and the paragraph directly beneath the table. Survival is
exactly what replay provides. The requirement no configuration meets is the stronger one, so the
row now asks whether the occurrence must eventually be **handled by somebody** even if nobody is
subscribed when it happens, which is the requirement L6.5 then sends to durable state, a queue
or acknowledgement.

**4. `Eagerly` was justified from the scope's lifetime, which is the axis error this Unit
exists to prevent.** L6.4 said `WhileSubscribed` "would buy nothing" against this repository's
process-lifetime state holders and "would reintroduce the spinner". Both halves are false, and
both collapse axes the Unit separates:

- *Scope lifetime is not start/stop policy.* A process-lifetime scope sets the **maximum**
  lifetime. `WhileSubscribed` on that same scope still stops the upstream every time the
  subscriber count reaches zero, so it buys exactly what it always buys.
- *Stopping is not resetting.* With the default `replayExpirationMillis` of `Long.MAX_VALUE`,
  `stateIn` keeps its last value while stopped. Measured with a plain `WhileSubscribed()`: after
  the last subscriber left the upstream stopped and `value` still read the last real value, and
  a returning subscriber received **that retained value first**, before the restarted upstream's
  own first value. A returning screen therefore never sees `initialValue`, so no spinner is
  reintroduced.

L6.4 now justifies `Eagerly` from what the upstream is: a derivation over shared history that
several screens read, kept running so the figures are computed before the first open and stay
current across gaps, paid for by running while nobody is looking. The Lesson says explicitly
that the scope's lifetime is a different question, and the corrected retained-value measurement
is in its Senior section. **No production code changed**, and the conclusion about the
repository's four holders is unchanged — only the reasoning offered for it.

#### Round two

Four more, and **three of them were round one's own corrections left unpropagated**. Round one
fixed the sentences that had been pointed at instead of sweeping the Unit for the claim shape,
so the identical over-general statement survived in a neighbouring Lesson. That is the process
failure worth recording, more than the individual defects.

**5. `replay` is also buffer for a present slow subscriber.** This one is a genuine technical
error rather than a stray copy, and it was the most damaging in the Unit, because it teaches
the wrong overflow behaviour. L6.3 said replay "is about a subscriber that is not there yet"
and extra buffer "is about a subscriber that is there and behind", as though they served
disjoint purposes. The `SharedFlow` KDoc says otherwise: the replay cache "also provides buffer
for emissions to the shared flow, allowing slow subscribers to get values from the buffer
without suspending emitters", and `extraBufferCapacity` reserves capacity "beyond replay".
Measured across seven configurations, the number of consecutive `tryEmit` calls that succeed
against a present 400-millisecond subscriber is **`replay + extraBufferCapacity`** every time —
`replay = 1, extraBufferCapacity = 0` gives one slot, from replay alone. The Unit's own earlier
measurement had already shown this (`tryEmit` × 5 at `replay = 1` returned `true` then four
`false`) and the prose contradicted it. The other half holds: measured at
`replay = 0, extraBufferCapacity = 3`, six values emitted with nobody subscribed left a late
subscriber **nothing**. L6.3 now teaches `replay`'s dual role explicitly, states the total as
`replay + extraBufferCapacity`, and carries a four-row measured table with one column for a
present slow subscriber and one for a late one.

**6. "Collects the upstream once" is false under `WhileSubscribed`.** L6.4's Core said the
sharing coroutine "collects the upstream once", four paragraphs above its own measured table
showing `WhileSubscribed()` stopping and restarting that collection. What sharing removes is
one collection **per collector** — one upstream collection serves however many subscribers are
present — and how many times it is started over the stream's lifetime is the policy's business.
L6.4 now says exactly that and points forward to the restart in the same sentence. The code
comment beside it now reads "three concurrent subscribers … exactly once between them".

**7. The unconditional broadcast claim survived into L6.5.** Round one corrected L6.3's Core
gloss and `KEY_TAKEAWAY` to fan-out, and left L6.5's decision table asserting that a
`SharedFlow` means "all current subscribers receive each value" — the exact guarantee the
previous Lesson now spends a measured paragraph refuting. The row now reads that each value is
fanned out to current subscribers and that what each one receives depends on the buffering
configuration.

**8. The survival-is-impossible claim survived into L6.5.** Same shape. Round one corrected
L6.3's decision-table row to ask about eventual handling rather than survival; L6.5's
`COMMON_MISTAKE` still said that an occurrence surviving with no collector is something "no
amount of configuration" provides, which `replay = 1` plainly does and which the Unit measures
twice. The callout now scopes the `replay = 0` case accurately, notes that a replay window does
retain across a gap, and names the unmeetable requirement as eventual **handling** —
nothing records whether a subscriber consumed the value and nothing waits for one to appear.

**What the sweep covered.** After these fixes the whole Unit was searched for the five claim
shapes involved — unconditional receipt by all subscribers, survival-with-no-subscribers stated
as impossible, "once" applied to upstream collection, "no configuration provides", and replay
described as ordinary buffering. Five matches remain and all five are correct in context: the
`SharedFlow` KDoc quotation in L6.3's Core, which the next clause qualifies; the unbuffered
`emit` contract, which genuinely does wait for all subscribers under the default suspending
strategy; the two `once` matches, which describe a probe's upstream rather than the sharing
model; and the two "no configuration" statements, both now scoped to guaranteed handling.

#### Round three

Two more reported, both in L6.5, and both the same claim shape yet again — which is what finally
forced a sweep method that works.

**9. "A shared flow hands the same value to every subscriber."** The Channel comparison restored
the unconditional delivery guarantee that rounds one and two had removed from L6.3 and from
L6.5's own table. The round-two sweep missed it because that regex anchored on receive-side
verbs near a universal quantifier, and this sentence puts the verb first and uses "hands". L6.5
now draws the contrast as the **shape** of delivery rather than its reliability: a shared flow
offers the same value to all of its subscribers, a channel gives each element to one receiver.

**10. A `Channel` was offered as the answer to "each element handled by exactly one receiver".**
Receiving an element removes it, and nothing tracks what happens next, so a channel provides
competing-receiver *delivery* and not handling. Measured on this project's JVM target with five
queued elements and one worker: the worker took 1, 2 and 3, finished 1 and 2, and was cancelled
part-way through 3; a replacement receiver drained 4 and 5, and **3 was never redelivered and
never finished by anyone**. This mattered more than a wording slip, because the row was offering
`Channel` as the answer to the requirement L6.5's own Senior section sends to durable state,
acknowledgement or a queue. The row now reads "each element taken by exactly one receiver rather
than by all of them" with the property "queued delivery to a single receiver, not broadcast";
the measurement is in the Lesson; the table's limits paragraph now states that every row is
about delivery and none is a guarantee of handling; and the Senior section says outright that a
`Channel` does not rescue the requirement either. The Unit's closing argument is stronger for
it — **no** primitive here guarantees eventual handling, not `SharedFlow` and not `Channel`.

**A sweep method that actually works.** Three rounds of regex sweeps each missed the next
instance, because each was written to match the wording of the defect that had just been
reported. The replacement is mechanical rather than clever: split every block in the Unit into
sentences, select the ones pairing a universal or negative quantifier (`all`, `every`, `each`,
`any`, `no`, `never`, `always`, `only`, `exactly`) with an audience noun (`subscriber`,
`collector`, `receiver`, `worker`, `observer`, `consumer`), and read all of them. That produced
63 sentences — small enough to check by hand, and it does not depend on predicting how the next
over-general claim will be phrased.

It found **two further instances that were not reported**, both now fixed: L6.4's Core said the
sharing coroutine "broadcasts what it produces to every subscriber", which is now "fans out …
to the subscribers"; and L6.4's `Lazily` paragraph quoted the KDoc's guarantee that the first
subscriber gets every emitted value without noting that it holds for `shareIn`'s default
buffering, which a fused `conflate()` would change. The other 61 were checked and are correct in
context — the `SharedFlow` KDoc quotation that the following clause qualifies, the unbuffered
`emit` contract, `StateFlow`'s genuinely unconditional replay-one-to-every-new-subscriber rule,
and the measured statements.

#### Round four

One more, and it is the sharpest of the twelve because the sentence contradicted the contract
this Unit's second Lesson is built on.

**13. `replay = 1` was offered as an alternative to `StateFlow` for current-value semantics.**
L6.3's decision table answered "must an observer arriving at any moment know the current truth?"
with "this is state — `StateFlow`, or `replay = 1` at minimum". The second half is false, and
L6.2 already says why: a `StateFlow` requires an initial value, so there is never a moment when
it has nothing to hand over, whereas `replay = 1` retains the most recent value **if there has
been one**. Measured on this project's JVM target:

| Case | What a subscriber arriving now receives |
| --- | --- |
| Fresh `MutableSharedFlow(replay = 1)`, nothing emitted yet | **Nothing** — it waits; the replay cache is empty |
| The same flow after one emission | That value |
| The same flow after `resetReplayCache()` | **Nothing** again |
| Fresh `MutableStateFlow("INITIAL")` | **`INITIAL`**, immediately |
| `replay = 1` + `DROP_OLDEST`, seeded with `tryEmit`, read through `distinctUntilChanged` | `INITIAL`, immediately |

The last row is the `StateFlow` KDoc's own recipe for making a shared flow behave like a state
flow, and it is the clearest available proof that `replay = 1` is one ingredient of three rather
than an equivalent. The table row now reads "`StateFlow` — no `SharedFlow` configuration gives a
value that is always already there", and a new paragraph beneath the table carries the
measurement and the recipe. This also gives the Unit its sharpest `StateFlow`-against-`SharedFlow`
distinction, which had previously been left implicit in L6.2's "there is always a current value".

**Sweep for this shape.** The quantifier sweep does not catch a *substitution* claim, so the Unit
was searched separately for every mention of `replay = 1`, "replay window", "at minimum", and
phrasings of the form "like a `StateFlow`" or "instead of a `StateFlow`". Nine matches, of which
this was the only substitution claim; the rest describe retention across a gap and are supported
by the Unit's own measurements. L6.5's worked scenario already names `StateFlow` for current
authentication state without offering an alternative, so nothing else needed changing.

#### Round five

Two reported, one found alongside them, and between them they close the last of the three axes.

**14. L6.1 implied that a hot stream's production is already running.** Its Core said a
collector "does not start it — it joins something that is already there". The first half is the
definition; the second quietly asserts that production is under way, which `Lazily` and
`WhileSubscribed` contradict, as L6.4's own measured table shows four Lessons later. Measured on
this project's JVM target with `stateIn(scope, SharingStarted.Lazily, -1)`: before any subscriber
the object existed and its `value` read `-1`, while the upstream had started **zero** times; the
first subscriber's arrival started it. L6.1 now says the instance exists — it is there before
anyone subscribes, outlives them all leaving, and subscribing joins that one instance rather than
making a private copy — and then separates that explicitly from whether anything is being
produced into it, with the measurement and a forward pointer to L6.4. The closing line is now
"'a subscriber never starts anything' is not part of the definition; what is, is that production
is not per-collector."

**15. The same claim in miniature, two paragraphs later, not reported.** The paragraph
introducing the word *subscriber* said it "is a useful reminder that subscribing is joining
rather than starting". Found while re-reading L6.1's Core after fixing the sentence above. It now
reads that what a subscriber joins already exists as a stream, "even when, as above, its arrival
is what sets production going".

**16. "A new subscriber is served immediately from what is retained" is `stateIn`-only.** L6.4's
`WhileSubscribed(5_000)` callout said it while the surrounding passage covers both operators, and
`shareIn(..., replay = 0)` retains nothing, so a returning subscriber simply waits for the
upstream. Measured on this project's JVM target with an upstream that takes 250 ms to produce
anything, after a `WhileSubscribed()` stop:

| Returning subscriber | At 120 ms | First value |
| --- | --- | --- |
| `stateIn(..., initialValue)` | the retained value | at **4 ms**, then the restarted upstream's values |
| `shareIn(..., replay = 0)` | **nothing** | at **258 ms**, when the restart produced one |

The callout now says that nothing in the timeout delays a subscriber or re-runs the upstream on a
timer, and that what a subscriber has to show for joining at once is whatever was retained — the
current value for `stateIn`, the replay cache for `shareIn`, nothing at `replay = 0`. The Senior
paragraph on replay expiration carries the paired measurement.

**Sweep for these shapes.** Two more passes, neither of which the earlier sweeps would have
caught. One for already-running language (`already there`, `already running`, `in progress`,
`joins`): five further matches, all correct — two describe `StateFlow`'s current value, and three
are L6.4 policy-table cells for `Eagerly` and `Lazily`, where the upstream genuinely is already
started and a later subscriber genuinely does join a run in progress. One for claims spanning
both sharing operators, and for "a new subscriber … immediately": seven matches, all correct —
`StateFlow`'s unconditional replay to a new collector, the `emit`-returns-immediately contract,
a requirement phrased as a question, and three statements true of both operators.

**The wider lesson for E24-09.** Almost every one of these sixteen was a general rule stated
without the qualification the same Lesson supplies, and most of rounds two to five existed only
because the previous round patched the flagged sentence rather than the claim. That is the
specific failure mode of a Unit whose subject is separating axes. Four things follow: the other
five Units are worth re-reading for the same shape; a correction to a general claim must be
applied by searching the whole document for the claim rather than editing the reported sentence;
the search should be the mechanical quantifier-plus-audience sweep described above, because three
successive hand-written regexes each missed the next instance by matching the wording of the last
one; and that sweep catches over-general *delivery* claims but not *substitution* claims — "X at
minimum" offered in place of Y — which need their own pass, as round four showed.

### The study-state race this Unit surfaced

Adding five Lessons made `LearningUnitPracticeIntegrationTest`'s
`existingLearnerTraversesTheExpansionWithLiveParentProgressAndDurableIdentities` fail
intermittently with a ten-second timeout, waiting for a Lesson it had just marked to read back as
studied and not pending. The cause is a real defect in `StudyProgressStateHolder`, not in the
content and not in the test.

**The race.** Opening a Learn destination constructs a `LearningLessonViewModel`, whose `init`
calls `StudyProgressStateHolder.refresh()`, which launches a `getStudiedLessons()` read. If the
learner then marks that Lesson studied, `toggleStudied` writes and reads back on a separate
coroutine. The `reading` mutex serialised refreshes against each other but not against that
read-back, so a refresh read that reached the database **before** the write could return
**after** it and publish its older snapshot over the persisted result. The holder then showed the
Lesson as unstudied while the database held it as studied — the exact fabrication its own
documentation forbids, arriving from the read side rather than the write side.

**Why it surfaced as a flake rather than a visible bug.** The correct state does exist, for an
instant, between the write settling and the stale read landing. `StateFlow` conflates, so a
collector not scheduled inside that window never observes it — which is Unit 6's own
slow-collector contract, and the reason the test waited for a state that had already come and
gone. The test traverses every authored Lesson, so five more Lessons meant five more chances per
run.

**Reproduced deterministically** with a repository whose read snapshots on entry and returns on
command: the database ended holding `lesson_a` while the holder published `studied=[]`, and the
observed sequence was `[]`, `[] pending=[a]`, `[a] pending=[]`, `[]`. No existing test could
express it, because the fake's read gate snapshots *after* the gate opens.

**The fix** takes the same `reading` mutex around the mutation's read-back, so a read can never
publish a snapshot older than one already published. The write itself stays outside the lock,
which is what keeps a mutation on one Lesson from delaying a mutation on another — the behaviour
`aSecondTapOnThePendingLessonIsIgnoredWhileOtherLessonsStayUsable` pins, and which serialising the
whole mutation would have broken. `StudyProgressStateHolderTest` gains
`aReadIssuedBeforeAWriteDoesNotOverwriteItAfterwards`, confirmed to fail against the unfixed
holder and pass against the fixed one, and `FakeLessonStudyRepository` gains a `staleReadGate`
that can express a read older than a write.

After the fix the reported test passed **100 out of 100** direct runs, and its whole class 60 out
of 60. It had already passed 100 out of 100 before the fix when run in isolation, which is why the
reproduction was built rather than the flake chased.

**This is production code and outside E24-07's authoring scope.** It is in this diff because the
Unit's own Lessons surfaced it and leaving a known-failing test would be worse. It is
self-contained — one `withLock`, its documentation, one test and one test-support capability — and
can be split into its own change without touching any content.

### Cross-links and validation

Backward-only, and every target already ships:

- L6.1: `lesson_cold_flows`, `lesson_flow_collection_lifetime`, `lesson_snapshot_flow`.
- L6.2: `lesson_hot_and_cold_streams`, `lesson_flow_buffering_and_conflation`,
  `lesson_shared_state_and_coordination`.
- L6.3: `lesson_hot_and_cold_streams`, `lesson_state_flow`,
  `lesson_flow_buffering_and_conflation`.
- L6.4: `lesson_coroutine_scope_ownership`, `lesson_cold_flows`, `lesson_state_flow`,
  `lesson_shared_flow`.
- L6.5: `lesson_why_flow`, `lesson_hot_and_cold_streams`, `lesson_state_flow`,
  `lesson_shared_flow`, `lesson_sharing_cold_flows`.

Every candidate link the issue proposed was taken and none was added beyond them, which is
the discipline the issue asked for now that no future E24 id has to be avoided. The
`lesson_snapshot_flow` link from L6.1 is the one that carries an argument rather than a
definition: L6.1's Senior section reuses that Lesson's state-as-lossy-compression framing to
explain why retention differs between state and occurrences, and **does not restate any
Compose snapshot material**. `lesson_snapshot_flow` was re-read and needed no edit — the
sentence E24-05 corrected is already gone, and nothing Unit 6 says contradicts its four-fact
bridge. Forward references are prose: the Compose collection site and one-off UI effects to
"later in this path", and acknowledgement and queueing to "outside this unit".

Validation run, all passing: `python3 tools/learning_question_coverage.py --write` then
`--check`; `./gradlew :shared:jvmTest --tests "*BundledLearningCurriculumTest*" --tests
"*LearningContentEndToEndTest*" --tests "*LearningCurriculumJsonCodecTest*"`;
`./gradlew :shared:jvmTest --tests "*LearningUnitPracticeIntegrationTest*" --tests
"*LearningProductionContentJourneyTest*" --tests "*LearningReaderJourneyIntegrationTest*"
--tests "*ProgressLearningJourneyIntegrationTest*"`; `./gradlew :shared:jvmTest`;
`./gradlew :shared:allTests`; `./gradlew :shared:check`; and
`python3 -m unittest discover -s tools -p "test_*.py"` for the coverage generator itself.
`./gradlew :shared:iosSimulatorArm64Test` reported UP-TO-DATE against the changed bundle, so
its results directory was removed and the task re-run: 427 tests, no failures.
`./gradlew :shared:jvmTest --tests "*StudyProgressStateHolderTest*"` covers the production fix
above, and the reported integration test was additionally run 100 times directly against the JVM
test classpath. `LearningCurriculumValidator` reported no error against the shipped bundle. Unlike Unit 5, it
caught nothing during authoring; the one defect that did surface — a `SharedFlow` snippet that
declared the same `val` twice — was found by reading the rendered Lesson end to end, which is a
useful reminder of where the machine-checkable half of the authoring contract stops.

E24-08 was not started: no Question was added, changed, re-mapped or deprecated, and
`initial_curriculum.json` is untouched by this change.

## Cross-linking rules

`LearningCurriculumValidator` rejects a `relatedLessonIds` entry naming an unknown Lesson
(`UNKNOWN_RELATED_LESSON`), and lesson ids are resolved across the whole document rather than
within a Unit. **Forward links are therefore invalid until their target ships.**

- Each authoring issue may link **backwards** only: to the 21 shipped Compose Lessons and to
  Lessons shipped by an earlier E24 issue.
- Pointing forward is a prose sentence naming the Unit. The issue that ships that Unit may
  then add the reciprocal link from its own Lessons.
- The **one edit to existing content** this epic requires is E24-05's reconciliation of
  `lesson_snapshot_flow`, below. No other shipped Lesson needs editing to receive a link.
- Useful backward links into shipped Compose content: `lesson_snapshot_flow` from
  `lesson_cold_flows`, `lesson_flow_collection_lifetime` and `lesson_hot_and_cold_streams`;
  `lesson_work_outside_composition` from `lesson_with_context_and_main_safety`.

## Reconciling the shipped `snapshotFlow` Lesson

`lesson_snapshot_flow` (in `unit_snapshot_fundamentals`, shipped by E23-06) is the only
published bridge into this subject. E24-05 owns the reconciliation. What it already teaches,
what it deferred, and what must change:

**What it teaches about Flow, and E24 must not contradict:**

- Four facts, presented as "enough for this lesson to stand on its own": a cold flow produces
  nothing until collected and each collector starts an independent execution; `collect` is
  suspending and needs a coroutine; the collector's lifetime is the coroutine's lifetime;
  stopping collection means cancelling that coroutine. All four are exactly what
  `lesson_cold_flows` and `lesson_flow_collection_lifetime` own, at greater depth. **They are
  correct and must be repeated as consistent, not corrected.**
- The state-versus-events argument in its Senior section — observable state is a lossy
  compression of the events that produced it, so an observer of state may arrive late and
  still be correct. `lesson_hot_and_cold_streams` should cite this rather than re-derive it.

**What it explicitly deferred:** "operators, hot streams and sharing, back pressure,
structured cancellation" — which is Units 3, 5 and 6 of this blueprint.

**What must change, in E24-05:**

1. Its Core section says the full subject "belongs to the coroutines and Flow curriculum,
   **which this app does not teach yet**; Kotlin's own documentation is where to go for it in
   the meantime." That sentence becomes false the moment Unit 4 ships. E24-05 must replace it
   with a pointer to the shipped Unit, and may then add `lesson_cold_flows` and
   `lesson_flow_collection_lifetime` to its `relatedLessonIds`.
2. Nothing else in the Lesson needs editing. In particular, E24 must **not** duplicate any
   snapshot-system teaching: `snapshotFlow` remains owned by `compose_snapshot_system` and
   Unit 6 of the Compose blueprint, and no E24 Lesson takes a Compose Subtopic as primary.

A second, weaker pointer exists: `lesson_work_outside_composition` says dispatcher and
builder choice "belong to the effects and coroutine material later in this path". That
becomes half-true once Units 1–3 ship. E24-09 should re-read it; it is a candidate for a
one-sentence edit, not a defect.

**Performed by E24-05 on 2026-09-10.** Both required changes shipped and nothing else in the
Lesson or its Unit moved; `lesson_work_outside_composition` was re-read and deliberately left
alone. The exact edits are recorded in
[the `snapshotFlow` reconciliation, exactly as performed](#the-snapshotflow-reconciliation-exactly-as-performed).

---

## Semantic assessment review

Every ACTIVE Question in the `async_reactive` Topic was read in full — stem, options, correct
set, explanation and Sources — not counted. There are **38**, spread over 24 of the Topic's 26
Subtopics; `rxjava_fundamentals` and `flow_vs_rxjava` hold none. Five DEPRECATED Questions on
the same Subtopics were read as context, because a deprecated Question still occupies its
concept.

**Nothing below claims that an unpublished Lesson makes a Question answerable.** "Planned"
means this plan places the required reasoning in a named Lesson; whether the finished prose
delivers it is a judgement E24-02…E24-07 make at authoring time and E24-08 re-checks.

| Question | Level | Subtopic | Lesson(s) | Reasoning the Question requires | Finding | Action |
| --- | --- | --- | --- | --- | --- | --- |
| `coroutine_fundamentals_001` | Foundation | `coroutine_fundamentals` | L1.1 | That suspension releases the thread, implies no pool, and may resume elsewhere | Sound, and the closest match in the bank to a Lesson's contract | Retain |
| `coroutine_run_blocking_main_thread` | Applied | `coroutine_fundamentals` | L1.2 | That `runBlocking` blocks the calling thread until its coroutine finishes | Sound as a Question. **Mapping mismatch**: the reasoning is builder choice, which L1.2 owns through `coroutine_builders`, but the Subtopic reaches L1.1 | Retain; re-map candidate — see [mapping corrections](#mapping-corrections-rather-than-gaps) |
| `coroutine_async_exception_surfaces_at_await` | Advanced | `coroutine_builders` | L3.3 | That a failing `async` child cancels its scope even when the `Deferred` is dropped, and that `supervisorScope` changes this | Technically excellent and verified against the exception-handling documentation. **Mapping mismatch**: it is failure-propagation reasoning sitting on the builders Subtopic, so it becomes Unit 1 practice for what Unit 3 teaches | Retain; re-map candidate |
| `launch_vs_async_unawaited_result` | Applied | `coroutine_builders` | L1.2 | That `launch` communicates resultless work and `async` produces a value someone must consume | Sound and well matched | Retain |
| `coroutine_scope_job_ownership` | Applied | `coroutine_scope` | L1.4 | That the scope's `Job` defines the lifetime of what it launches, so ownership must be explicit | Sound; the best match in the bank for Unit 1's central argument | Retain |
| `coroutine_supervisor_job_child_context_noop` | Advanced | `coroutine_context` | L2.1, L3.4 | That a `Job` passed to `launch` re-parents that coroutine out of the scope rather than supervising it | Sound and verified. Its reasoning is deliberately split across two Lessons by this plan. **Consequence:** it is the *only* active Question on `coroutine_context`, so L2.1's own reasoning is unassessed | Retain; see GAP-U2-A |
| `coroutine_io_dispatcher_blocking_calls` | Applied | `coroutine_dispatchers` | L2.2 | That `Default` is sized around the core count and blocking work starves it, while `IO` expands | Sound and verified against the `Dispatchers.IO` KDoc | Retain; L2.2 must add that `IO` and `Default` share threads |
| `suspending_api_dispatcher_assumption` | Applied | `coroutine_dispatchers` | L2.3 | That `suspend` selects no dispatcher and an asynchronous client needs no `withContext` wrapper | Sound. Minor mapping drift — the reasoning is `withContext` ritual, which L2.3 owns — but both Lessons are in the same Unit, so Unit practice is unaffected | Retain |
| `parent_cancellation_propagates_children` | Foundation | `coroutine_jobs` | L1.3, L3.1, L3.2 | That cancellation propagates and is observed at cooperative points, that `cancel()` returns at once, and that a suspending call in `finally` needs `NonCancellable` | Technically correct, but its three distractors require `cancelAndJoin` and `NonCancellable` — Unit 3 material — while its Subtopic reaches only Unit 1. **The most consequential mismatch found**, and its `FOUNDATION` level does not match the reasoning required | Retain; re-map and level review are both candidates for E24-08 |
| `structured_concurrency_001` | Foundation | `structured_concurrency` | L1.5 | That structured concurrency is about lifetime and ownership, not scheduling | Sound and precisely matched to L1.5's contract, including the distractor that children run one at a time | Retain |
| `coroutine_cancellation_001` | Foundation | `coroutine_cancellation` | L3.1 | That cancellation is cooperative and busy code stops only at a check | Sound | Retain |
| `cancellation_exception_rethrow` | Applied | `coroutine_cancellation` | L3.1, L3.3 | That `CancellationException` signals cooperative cancellation and swallowing it lets work continue | Sound | Retain |
| `coroutine_run_interruptible_blocking_call` | Applied | `coroutine_cancellation` | L3.2 | That cancellation reaches neither a suspension point nor a check inside a blocking JVM call, and `runInterruptible` bridges the two models | Key and options sound and verified. **Two caveats.** Platform: thread interruption is a JVM concept and this is a KMP repository, so L3.2 must scope the claim. Accuracy: one clause of its explanation — "withTimeout resumes the coroutine while leaving the blocked thread occupied" — is contradicted by [measurement A](#what-withtimeout-does-to-non-cooperative-work) | Retain; explanation correction is a candidate for E24-08 |
| `coroutine_exceptions_001` | Foundation | `coroutine_exceptions` | L3.3 | That a regular `Job` parent is cancelled by a child failure, taking its siblings with it | Sound | Retain |
| `coroutine_exception_handler_root_boundary` | Advanced | `coroutine_exceptions` | L3.3 | That the handler observes uncaught root-coroutine exceptions and that `async` exposes failure at `await` instead | Sound and verified word for word against the exception-handling page, including that `async` has no handler | Retain |
| `coroutine_supervisor_scope_direct_children` | Advanced | `coroutine_supervision` | L3.4 | That supervision reaches only direct children, so a nested `launch`'s failure cancels its own parent | Sound and verified; the sharpest Question in the Topic | Retain |
| `coroutine_context_switching_001` | Foundation | `coroutine_context_switching` | L2.3 | That `withContext` switches context for a scoped block and starts nothing concurrent | Sound | Retain |
| `coroutine_async_await_sequential` | Applied | `coroutine_parallelism` | L2.4 | That awaiting on the same line serialises work that looks parallel | Sound; exactly L2.4's central example | Retain |
| `viewmodel_scope_cleared_cancellation` | Foundation | `lifecycle_coroutines` | — | That `viewModelScope` is cancelled when the ViewModel is cleared | Sound, but `lifecycle_coroutines` is **supporting-only** everywhere in E24 by design, so this creates no Unit practice | Retain; no E24 action |
| `flow_fundamentals_001` | Foundation | `flow_fundamentals` | L4.2 | That a cold flow's producer runs per collection and holds no value | Sound | Retain |
| `callback_flow_await_close_registration` | Foundation | `flow_fundamentals` | L4.5 | That the builder block returns after registering, so `awaitClose` is what keeps the subscription alive and gives unregistration one place | Sound and verified. Level looks low — reasoning about a builder contract and a thrown `IllegalStateException` is not recall | Retain; level review is a candidate, not a defect |
| `flow_flat_map_latest_search_cancellation` | Applied | `flow_operators` | L5.3 | That `flatMapLatest` cancels the inner flow for the previous value, and that `conflate` does not cancel started work | Sound; the best-matched operator Question in the bank | Retain |
| `flow_debounce_vs_distinct_until_changed` | Foundation | `flow_operators` | L5.1 | That `debounce` is time-based and `distinctUntilChanged` compares with the immediately previous value | Sound; exactly L5.1's decision | Retain |
| `flow_combine_vs_zip_emission_rule` | Foundation | `flow_operators` | L5.2 | That `combine` re-emits from latest values after every source has emitted once, and `zip` pairs unused values | Sound and verified against both KDocs. Level looks low for a rule with a startup precondition | Retain; level review is a candidate |
| `flow_launch_in_on_each_scope` | Foundation | `flow_collection` | L4.3 | That `launchIn` is `scope.launch { collect() }` and returns a `Job`, and that it does not make the flow hot | Sound | Retain |
| `flow_collection_cancels_cold_producer` | Foundation | `flow_collection` | L4.3 | That the producer runs in the collecting coroutine, so cancelling the collector stops production cooperatively | Sound and verified; L4.3's central claim | Retain |
| `flow_catch_upstream_only` | Foundation | `flow_errors` | L5.5 | That `catch` sees only what is declared upstream of it, and that the terminal collector is downstream | Sound and verified against the `catch` KDoc | Retain |
| `flow_retry_when_conditional_attempts` | Applied | `flow_errors` | L5.5 | That `retryWhen`'s predicate receives cause and attempt index, and that `catch` terminates rather than resubscribes | Sound | Retain |
| `flow_context_001` | Foundation | `flow_context` | L4.4 | That `flowOn` affects upstream operators only | Sound and verified | Retain |
| `flow_conflate_vs_collect_latest` | Foundation | `flow_buffering` | L5.4 | That `conflate` drops values and lets the block finish, while `collectLatest` cancels the block | Sound and verified against both KDocs | Retain |
| `flow_buffer_producer_consumer_concurrency` | Applied | `flow_buffering` | L5.4 | That `buffer` decouples producer and collector up to a capacity, trading memory for throughput | Sound | Retain |
| `stateflow_001` | Foundation | `stateflow` | L6.2 | That `StateFlow` holds a current value, requires an initial one, conflates, and is a poor fit for one-time events | Sound | Retain |
| `stateflow_vs_sharedflow_current_value` | Foundation | `stateflow` | L6.2, L6.3 | That `StateFlow` replays its latest value and `SharedFlow` has configurable replay without a current-value contract | Sound and verified. Both correct options are descriptive rather than behavioural | Retain; see GAP-U6-A |
| `flow_share_in_vs_state_in` | Applied | `sharedflow` | L6.4 | That both convert a cold upstream, that `stateIn` requires an initial value, and that `SharingStarted` decides when the upstream stops | Sound and verified. **Mapping drift:** the reasoning is sharing, which L6.4 owns through `flow_sharing`; the Subtopic reaches L6.3. Same Unit, so Unit practice is unaffected | Retain |
| `shared_flow_replay_late_subscriber` | Foundation | `sharedflow` | L6.3 | That a value reaches whoever is subscribed at emission, and the replay cache is all a later subscriber can see | Sound and verified against the `SharedFlow` KDoc | Retain |
| `flow_vs_channel_delivery_model` | Applied | `hot_vs_cold_streams` | L6.5 | That a `Channel` coordinates queued values to one receiver per element, which no Flow shape does | Sound. It is also the precedent that makes mapping bounded `Channel` material to `hot_vs_cold_streams` correct rather than a stretch | Retain |
| `flow_state_in_while_subscribed` | Foundation | `flow_sharing` | L6.4 | That `WhileSubscribed`'s timeout is the grace period after the last collector leaves, not a poll interval or a delivery delay | Sound and verified against the `SharingStarted.WhileSubscribed` KDoc | Retain |
| `live_data_vs_state_flow_ui_state` | Foundation | `livedata` | — | That `StateFlow` needs an initial value, is not lifecycle-aware, and conflates equal values while `LiveData` dispatches every set | Sound. `livedata` is unmapped by E24 by design, so this creates no Unit practice; its third claim is nevertheless the clearest existing statement of `StateFlow` conflation in the bank | Retain; no E24 action |

### Deprecated Questions read as context

`coroutine_vs_thread_suspension` (suspension releases the thread), `coroutine_builders_001`
(when `async` beats `launch`), `cpu_loop_cooperative_cancellation` (the CPU-loop checks),
`coroutine_scope_vs_supervisor_scope_failure` (`supervisorScope` for independent siblings) and
`sharedflow_001` (SharedFlow properties) are DEPRECATED. Two consequences for E24-08: the
concept space around suspension, builders and supervision is more crowded than the active
counts suggest, and a new Question must not re-ask what a deprecated one already asked in
different wording.

### Questions outside `async_reactive` that touch this subject

Read so that E24-08 does not duplicate them and so no Lesson claims coverage it does not
create. **None of these is reachable through an E24 primary mapping**, because no E24 Lesson
takes a non-`async_reactive` Subtopic as primary — so none of them becomes Unit practice for
a new Unit.

| Question | Topic / Subtopic | Relevance |
| --- | --- | --- |
| `performance_coroutine_scope_leak` | performance / `coroutine_leaks` | The `GlobalScope` failure L1.4 teaches, assessed from the leak side; supporting-only here |
| `retrofit_coroutines_001`, `coroutine_http_call_cancellation` | networking / `retrofit_coroutines` | Suspending client integration and cancellation reaching the HTTP layer; supporting context for L2.3 and L3.1 |
| `android_main_thread_001` | android_platform / `android_main_thread` | Main-thread work; bridged by L1.1 and L2.2 |
| `compose_snapshot_flow_state`, `compose_snapshot_flow_read_inside_block`, `compose_snapshot_flow_conflated_state` | android_ui / `compose_snapshot_system` | The published Compose bridge; owned by E23, not to be re-asked by E24 |
| `durable_state_vs_one_off_event`, `repository_observable_api_shape` | architecture / `state_ownership`, `repository_pattern` | The closest existing assessment of L4.1's and L6.5's decisions, from the architecture side |
| `kmp_swift_interop_suspend_flow` | kmp / `shared_vs_platform_code` | Suspend and Flow across the Swift boundary; out of E24 scope |
| `test_main_dispatcher_replacement`, `run_test_advance_until_idle_pending_work`, `coroutine_virtual_time_delay_skipping`, `flow_testing_turbine_bounded_collection`, `flow_test_background_collector_statein` | testing / various | Owned by E31; E24 must not teach or assess this material |

---

## Assessment gaps for E24-08

Substantive gaps only. There is no per-Lesson or per-level quota, and a gap here is a
candidate for authoring rather than an automatic defect. Each gap names the reasoning that is
missing, not a number.

| Gap | Unit / Lesson | Reasoning no ACTIVE Question assesses | Why it is substantive | Recommended action |
| --- | --- | --- | --- | --- |
| GAP-U1-A | Unit 1 / `lesson_suspension_and_blocking` | Predicting, for a specific call, whether it suspends or blocks — a `suspend` function that calls a blocking API inside it, or `Thread.sleep` inside a coroutine | `coroutine_fundamentals_001` establishes the definition; nothing makes the reader apply it to code, which is where the misconception actually lives | Add coverage in E24-08 |
| GAP-U1-B | Unit 1 / `lesson_coroutine_scope_ownership` | The concrete failure of an un-owned or process-lifetime scope, from the coroutine side | `coroutine_scope_job_ownership` assesses why ownership should be explicit, not what goes wrong without it. `performance_coroutine_scope_leak` is in the `performance` Topic and supporting-only, so it creates no Unit 1 practice | Add coverage in E24-08 |
| GAP-U1-C | Unit 1 / `lesson_job_and_parent_child` | `Job` as a handle: waiting with `join`, the parent that has finished its own body but is not complete, and `Deferred` as a `Job` with a value | `coroutine_jobs`' only active Question is a cancellation question (see the mapping note), so the Lesson's own reasoning is unassessed | Add coverage in E24-08 |
| GAP-U2-A | Unit 2 / `lesson_coroutine_context` | Context as an inherited, composable set — what a child inherits, what it overrides, and that the `Job` is the one element never simply inherited | `coroutine_context`'s only active Question is an ADVANCED re-parenting trap. The Unit's foundational reasoning has no assessment | Add coverage in E24-08 |
| GAP-U2-B | Unit 2 / `lesson_dispatchers` | That `Dispatchers.IO` and `Dispatchers.Default` share threads, so switching between them is frequently not a thread switch | Documented behaviour that directly contradicts common folklore, and the kind of claim a senior candidate is expected to get right. Nothing in the bank goes near it | Add coverage in E24-08; the strongest ADVANCED candidate in Units 1–3 |
| GAP-U2-C | Unit 2 / `lesson_sequential_and_concurrent_work` | Concurrency against parallelism, and the case where adding concurrency does not pay | `coroutine_async_await_sequential` assesses the mechanical trap only; the judgement half of the Lesson is unassessed | Add coverage in E24-08 |
| GAP-U3-A | Unit 3 / `lesson_cancellation_cleanup_and_timeouts` | What may and may not be done inside a cancelled coroutine — that a suspending call in `finally` does nothing without `NonCancellable` — and that a timeout is cancellation with a clock | Present only as a distractor and an explanation aside in `parent_cancellation_propagates_children`. No Question makes it the thing being tested, and it is where real cleanup bugs come from | Add coverage in E24-08 |
| GAP-U3-B | Unit 3 / `lesson_shared_state_and_coordination` | Races between coroutines, and choosing among an atomic, confinement and a `Mutex` | **Nothing in any Topic assesses this.** The whole Lesson is unassessed, and it is the widest gap in the epic | Add coverage in E24-08 |
| GAP-U4-A | Unit 4 / `lesson_why_flow` | Choosing between a one-shot suspending function and an observable Flow for a given repository API | `repository_observable_api_shape` is the closest, sits in `architecture`, and is supporting-only here, so it creates no Unit 4 practice | Add coverage in E24-08 |
| GAP-U4-B | Unit 4 / `lesson_flow_context_and_flow_on` | Exception transparency's context half — that an emitter may not change its own context, and what the runtime does about it | `flow_context_001` assesses what `flowOn` affects. The invariant violation, which is the reason `flowOn` exists, is unassessed | Add coverage in E24-08; strong ADVANCED candidate |
| GAP-U4-C | Unit 4 / `lesson_flow_builders_and_callback_adapters` | Choosing among `flow`, `channelFlow` and `callbackFlow` for a given producer | `callback_flow_await_close_registration` assesses one builder's contract well; the choice between the three is not assessed. Lower priority than the gaps above | Add coverage in E24-08 if capacity allows |
| GAP-U5-A | Unit 5 / `lesson_flattening_flows` | `flatMapConcat` against `flatMapMerge` on ordering and overlap | `flow_flat_map_latest_search_cancellation` assesses the cancelling strategy and uses the other two only as distractors, which does not establish that a reader can choose between them | Add coverage in E24-08 |
| GAP-U5-B | Unit 5 / `lesson_flow_failure_and_completion` | What `onCompletion` observes — success, failure and cancellation alike — and why that makes it a poor place for a success side effect | Both `flow_errors` Questions are about `catch` and `retryWhen`. Completion is unassessed | Add coverage in E24-08 |
| GAP-U5-C | Unit 5 / `lesson_transforming_and_filtering_flows` | Choosing between `debounce` and `sample` — quiet period against fixed cadence — and predicting what each produces from one timeline | **Added by E24-06.** `flow_debounce_vs_distinct_until_changed` uses `sample` only as a one-clause distractor gloss, so nothing makes a reader choose between the two timing operators they are most likely to conflate | Add coverage in E24-08 |
| GAP-U5-D | Unit 5 / `lesson_flow_failure_and_completion` | That `retry` re-collects the upstream, so a cold producer's side effects run again and a failed attempt's values have already reached the collector | **Added by E24-06.** `flow_retry_when_conditional_attempts` assesses the API choice; what retrying costs — the half that decides whether the operator may be used at all — is unassessed | Add coverage in E24-08 |
| GAP-U6-A | Unit 6 / `lesson_state_flow` | `StateFlow`'s equality-based conflation as behaviour: that assigning an equal value emits nothing, and that a state type's `equals` therefore decides what the UI sees | Both `stateflow` Questions are descriptive — what `StateFlow` is for, and how it compares with `SharedFlow`. The one behavioural statement in the bank is a distractor-adjacent claim in `live_data_vs_state_flow_ui_state`, which sits on `livedata` and is unmapped by E24 | Add coverage in E24-08; the strongest candidate in Unit 6 |
| GAP-U6-B | Unit 6 / `lesson_sharing_cold_flows` | `Eagerly` against `WhileSubscribed` as a resource and correctness tradeoff, and what `replayExpirationMillis` does | `flow_state_in_while_subscribed` assesses one parameter of one policy. The choice between policies — the actual decision — is unassessed | Add coverage in E24-08 |
| GAP-U6-C | Unit 6 / `lesson_hot_and_cold_streams` | The hot/cold distinction posed directly: what a late subscriber receives from each, and what happens to values emitted with no subscribers | `hot_vs_cold_streams`' single active Question is the `Channel` comparison, which belongs to L6.5. `flow_fundamentals_001` assesses coldness from the cold side only | Add coverage in E24-08 |
| GAP-U6-D | Unit 6 / `lesson_shared_flow` | What an unbuffered `SharedFlow`'s `emit` does with and without subscribers, and that `tryEmit() == true` is not evidence anyone received the value | **Added by E24-07.** `shared_flow_replay_late_subscriber` assesses the late-subscriber half of delivery; the emitter's half is unassessed, and it is where the "SharedFlow is for events" heuristic actually fails | Add coverage in E24-08; the strongest ADVANCED candidate in the Flow half |
| GAP-U6-E | Unit 6 / `lesson_sharing_cold_flows` | That `stopTimeoutMillis` and `replayExpirationMillis` are two timers on two clocks, and that the second defaults to never resetting | **Added by E24-07.** `flow_state_in_while_subscribed` teaches the first parameter and does not mention the second. Overlaps GAP-U6-B and may be folded into it | Add coverage in E24-08 |
| GAP-U6-F | Unit 6 / `lesson_choosing_a_stream_abstraction` | That a must-not-be-lost occurrence is satisfied by no hot Flow configuration, so the answer is durable state, a queue or acknowledgement | **Added by E24-07.** `flow_vs_channel_delivery_model` assesses the single-receiver contrast, a different property; `durable_state_vs_one_off_event` sits in `architecture` and is supporting-only here. This is the reasoning the epic exists to put in place of the slogan | Add coverage in E24-08 |

### Mapping corrections rather than gaps

These are candidate Question edits, not new Questions. Each is a Subtopic mapping that
misdirects practice; none of them makes a Question wrong. **E24-08 should decide on each
deliberately** — a re-map changes which Unit's practice a Question appears in, and preserving
Question identity is a lifecycle requirement.

| Question | Current Subtopic | Reasoning it actually assesses | Suggested Subtopic | Consequence of leaving it |
| --- | --- | --- | --- | --- |
| `parent_cancellation_propagates_children` | `coroutine_jobs` | Cancellation propagation, `cancelAndJoin`, and `NonCancellable` | `coroutine_cancellation` | Unit 1 practice contains a Question a Unit 1 reader cannot answer, and Unit 3 loses a Question it earns. Its `FOUNDATION` level is a separate question |
| `coroutine_async_exception_surfaces_at_await` | `coroutine_builders` | Failure propagation from a dropped `Deferred`, and how `supervisorScope` changes it | `coroutine_exceptions` | An ADVANCED Unit 3 item becomes Unit 1 practice |
| `coroutine_run_blocking_main_thread` | `coroutine_fundamentals` | `runBlocking` as a bridge that blocks its caller | `coroutine_builders` | Minor; both Lessons are in Unit 1, so Unit practice is unaffected. Lowest priority of the three |

**One explanation correction, which is a different kind of item.**
`coroutine_run_interruptible_blocking_call`'s explanation says "withTimeout resumes the
coroutine while leaving the blocked thread occupied". Measurement A in
[What `withTimeout` does to non-cooperative work](#what-withtimeout-does-to-non-cooperative-work)
is that scenario and shows the coroutine is **not** resumed until the blocking call returns.
The Question's key and all four options stay correct, so this is a wrong rationale attached to
a right answer — the kind of defect that teaches a misconception to whoever reads the
explanation after answering. E24-04 should re-run the measurement while authoring L3.2, and
E24-08 should then correct the clause rather than write a new Question about it.

Two observations that are **not** gaps and must not be treated as quotas:

- `async_reactive` holds **four** ACTIVE `ADVANCED` Questions across 38, all four in the
  coroutine half (`coroutine_builders`, `coroutine_context`, `coroutine_exceptions`,
  `coroutine_supervision`). The Flow half — Units 4, 5 and 6 — currently has none, while
  Units 5 and 6 carry the epic's subtlest reasoning. If E24-08 authors Advanced material
  anywhere, that is where the reasoning would justify it, on merit rather than to fill a
  level.
- All 29 Lessons map to at least one Subtopic that has at least one active Question, which is
  exactly why the review above was read rather than counted. By reasoning rather than
  structure, **20 of 29** have an active Question whose reasoning this plan places in them.
  For a Topic averaging roughly 1.5 Questions per Subtopic that is the expected shape, not a
  coverage failure.

---

## Assessment outcomes for E24-08

Added by E24-08 after the gap table above was re-read against the finished Lessons. The table
stays as the record of what was found; this section is the record of what was done about it.
Every measurement below was executed on this repository's toolchain against the resolved
`kotlinx-coroutines-core:1.11.0`, and every source was opened rather than recalled.

### Scope

A semantic assessment review of the **primary** concepts of Units 1–6 and the ACTIVE Questions
reachable through them — **not** a bank-wide audit. That is 21 primary Subtopics and the 38
ACTIVE `async_reactive` Questions reachable through them, plus the five DEPRECATED Questions on
the same Subtopics and the supporting-only Questions in `performance`, `architecture`,
`android_ui`, `networking`, `testing`, `kmp` and `lifecycle_coroutines` that the earlier review
listed. Those last two groups were read for duplication only; none of them closes an E24 gap and
none was re-mapped. Nineteen Questions were authored, three were re-mapped, one had its level
raised and one had an explanation clause corrected; the other 407 questions in
`initial_curriculum.json` were not re-reviewed and their prior verdicts stand.
`docs/content/question-audit-log.yml` records the review under that scope.

### Final disposition of the 21 gap candidates

Every gap was re-read against the finished prose and against the Questions already on its
Subtopic before anything was written. **None was found already closed.** Twenty are addressed,
one is deferred.

| Gap | Disposition | Question ID |
| --- | --- | --- |
| GAP-U1-A | Addressed | `coroutine_suspend_does_not_move_blocking_work` |
| GAP-U1-B | Addressed | `coroutine_scope_outlives_its_consumer` |
| GAP-U1-C | Addressed | `coroutine_parent_job_waits_for_children` |
| GAP-U2-A | Addressed | `coroutine_child_context_inherits_and_overrides` |
| GAP-U2-B | Addressed | `coroutine_io_and_default_share_threads` |
| GAP-U2-C | Addressed | `coroutine_concurrency_needs_independence` |
| GAP-U3-A | Addressed | `coroutine_timeout_cleanup_needs_non_cancellable` |
| GAP-U3-B | Addressed in part; the mechanism-choice half deferred | `coroutine_shared_counter_lost_update` |
| GAP-U4-A | Addressed | `flow_one_shot_result_vs_observable_stream` |
| GAP-U4-B | Addressed | `flow_emit_must_keep_the_collector_context` |
| GAP-U4-C | **Deferred** | — |
| GAP-U5-A | Addressed | `flow_flat_map_concat_preserves_grouping` |
| GAP-U5-B | Addressed | `flow_on_completion_observes_every_ending` |
| GAP-U5-C | Addressed | `flow_sample_cadence_vs_debounce_quiet_period` |
| GAP-U5-D | Addressed | `flow_retry_re_collects_the_upstream` |
| GAP-U6-A | Addressed | `state_flow_equal_value_is_not_a_new_state` |
| GAP-U6-B | Addressed, combined with GAP-U6-E | `flow_sharing_policy_and_replay_expiration` |
| GAP-U6-C | Addressed | `hot_sharing_changes_production_not_retention` |
| GAP-U6-D | Addressed | `shared_flow_try_emit_true_is_not_delivery` |
| GAP-U6-E | Addressed, combined with GAP-U6-B | `flow_sharing_policy_and_replay_expiration` |
| GAP-U6-F | Addressed | `stream_choice_cannot_supply_a_delivery_guarantee` |

**One combination.** GAP-U6-B and GAP-U6-E are one Question because one scenario requires both
and neither half is answerable without the other: three stated requirements select the policy,
and only the two timers read separately satisfy all three. Splitting them would have produced a
policy Question whose keyed configuration a reader could reach without knowing what its second
argument does, and a timer Question with no reason to care.

**No gap was split.** GAP-U3-B was the only candidate for it, and it went the other way — see
below.

**One deferral, GAP-U4-C.** The gap asks a reader to choose among `flow`, `channelFlow` and
`callbackFlow` by producer shape. The rule that decides that choice is the `flow` builder's
single-coroutine emission limit, and `flow_emit_must_keep_the_collector_context` now assesses
exactly that limit from the invariant side, while `callback_flow_await_close_registration`
already assesses the callback adapter's own contract. A third Question on `flow_fundamentals`
would mostly re-ask the invariant in builder clothing, which is the duplication `Q20` exists to
prevent. The canonical plan already ranked this gap lowest, and the reasoning it names is taught
in L4.5 in full. Recorded as open rather than closed.

**One half-deferral, GAP-U3-B.** The gap names two separable things: recognising a race over
shared mutable state, and choosing among an atomic, confinement and a `Mutex`.
`coroutine_shared_counter_lost_update` takes the first together with the smallest sufficient
mechanism for a plain counter, which is an atomic; the `@Volatile` dead end is its strongest
distractor. The mechanism-selection half — an invariant spanning more than one variable, where
per-field atomics are insufficient and a `Mutex` or confinement is required — is **deferred**,
for a structural reason rather than an editorial one. `coroutine_parallelism` is the primary
concept of both L2.4 and L3.5, so every Question mapped to it enters **Unit 2's** practice as
well as Unit 3's, and Unit 2 teaches neither `Mutex` nor confinement. One such Question is a
tolerable overlap and two would not be. Nothing in any Topic assesses that reasoning yet, which
makes it the largest remaining hole in the epic.

### The nineteen new Questions

| Question | Subtopic | Level | Gap |
| --- | --- | --- | --- |
| `coroutine_suspend_does_not_move_blocking_work` | `coroutine_fundamentals` | Applied | GAP-U1-A |
| `coroutine_scope_outlives_its_consumer` | `coroutine_scope` | Applied | GAP-U1-B |
| `coroutine_parent_job_waits_for_children` | `coroutine_jobs` | Applied | GAP-U1-C |
| `coroutine_child_context_inherits_and_overrides` | `coroutine_context` | Applied | GAP-U2-A |
| `coroutine_io_and_default_share_threads` | `coroutine_dispatchers` | **Advanced** | GAP-U2-B |
| `coroutine_concurrency_needs_independence` | `coroutine_parallelism` | Applied | GAP-U2-C |
| `coroutine_timeout_cleanup_needs_non_cancellable` | `coroutine_cancellation` | **Advanced** | GAP-U3-A |
| `coroutine_shared_counter_lost_update` | `coroutine_parallelism` | Applied | GAP-U3-B |
| `flow_one_shot_result_vs_observable_stream` | `flow_fundamentals` | Applied | GAP-U4-A |
| `flow_emit_must_keep_the_collector_context` | `flow_context` | Applied | GAP-U4-B |
| `flow_flat_map_concat_preserves_grouping` | `flow_operators` | Applied | GAP-U5-A |
| `flow_on_completion_observes_every_ending` | `flow_errors` | Applied | GAP-U5-B |
| `flow_sample_cadence_vs_debounce_quiet_period` | `flow_operators` | Applied | GAP-U5-C |
| `flow_retry_re_collects_the_upstream` | `flow_errors` | Applied | GAP-U5-D |
| `state_flow_equal_value_is_not_a_new_state` | `stateflow` | **Advanced** | GAP-U6-A |
| `flow_sharing_policy_and_replay_expiration` | `flow_sharing` | **Advanced** | GAP-U6-B + GAP-U6-E |
| `hot_sharing_changes_production_not_retention` | `hot_vs_cold_streams` | Applied | GAP-U6-C |
| `shared_flow_try_emit_true_is_not_delivery` | `sharedflow` | **Advanced** | GAP-U6-D |
| `stream_choice_cannot_supply_a_delivery_guarantee` | `hot_vs_cold_streams` | **Advanced** | GAP-U6-F |

Each was solved from its stem and options alone before `correctAnswerIds` was consulted, every
option was tested for defensibility under the stem as written, and every cited page was opened
and the supporting sentence located. Four defects were found and fixed by that pass, all of them
in the stem rather than the key — and a fifth of the same shape was found afterwards by PR
review, recorded below the four:

- **GAP-U2-C had a speculative-prefetch escape.** The first draft made the second call *needed*
  only for premium accounts, which leaves starting it early genuinely faster on the premium path
  — so the keyed "overlapping cannot shorten the wait" was defensibly wrong. The stem now makes
  the second call take an id carried by the first call's response, which is a data dependency
  rather than a conditional one.
- **GAP-U5-A's `flatMapLatest` distractor was defensible.** "One at a time, in order, never
  interleaved" is all true of `flatMapLatest`; what it breaks is completeness. The stem now
  requires every file's updates from first to last.
- **GAP-U4-A had two soft distractors** that argued from style rather than fact. Both were
  replaced with claims that are checkable and false: that a value the server can change must be
  exposed as a stream, and that a cold Flow delivers one value per collection.
- **GAP-U6-B's "within a few seconds" was not a discriminator.** The stem now says five seconds,
  which makes the thirty-second stop distractor definitively wrong rather than arguably slow.

A fifth was caught before authoring. A draft of GAP-U6-A used a `data class` holding a
`MutableList` mutated in place, which lands exactly on the `StateFlow` KDoc's "behavior with
classes that violate the contract for `Any.equals` is unspecified" clause. The shipped stem
assigns a **newly built, equal** instance of a well-behaved type instead, which is specified
behaviour and makes the same point more sharply.

A sixth was found by PR review, and it is the same failure as the first four — an unstated
premise the key depended on. **GAP-U3-A's stem said only that `close()` is a suspending
function**, and `suspend` is a capability rather than a promise that a call reaches a
cancellable suspension point: a `close()` that flushes synchronously, suspends
non-cancellably, or handles the cancellation itself completes normally in a cancelled
coroutine. A reader who knew that could reject all four options. The stem now states that the
flush is awaited at an ordinary cancellable suspension point, and the explanation says what
follows when it is not — which turns the hole into the Unit's own point, that cancellation is
observed only where the code lets it be. Question and AnswerOption ids, the key, the level and
the Subtopic are unchanged: the assessed claim did not move, the stem stopped depending on an
assumption it never made.

**All nineteen are `SINGLE`.** Each asks for one prediction or one decision, and a `MULTIPLE`
question about, say, three true properties of `SharedFlow` is the descriptive shape these gaps
exist because of. **None is `FOUNDATION`**, which is a consequence rather than a policy: every
gap in the table names reasoning that the descriptive layer already has a Question for, which is
why it was a gap.

Answer positions were redistributed across the batch after authoring — nineteen Questions all
keying their first option is a position cue, and answer identity is by ID, so reordering costs
nothing. The bank's position distribution is unchanged at 28/27/26/19/1.

### The six Advanced levels, justified one at a time

The plan's observation that the Flow half held no `ADVANCED` Question was **not** used as a
reason. Each level below was decided from the contract's minimum-sufficient-reasoning test, and
thirteen of the nineteen came out `APPLIED`.

| Question | Why the reasoning is Advanced |
| --- | --- |
| `coroutine_io_and_default_share_threads` | Four mechanisms combine: that `IO` and `Default` share threads, that the switch is still a real dispatcher change, that `IO`'s parallelism is separate and elastic, and that what moves is the accounting rather than the thread. The strongest distractor is the correct over-correction |
| `coroutine_timeout_cleanup_needs_non_cancellable` | A timeout is cancellation, cancellation unwinds through `finally`, a suspension point inside a cancelled coroutine resumes with the cancellation, and `NonCancellable` is the bounded remedy. Four documented contracts, traced in order, and two distractors that each get one of them right |
| `state_flow_equal_value_is_not_a_new_state` | Equality-based conflation, a data class's generated `equals`, and the inference that a new instance is not a new state — plus separating equality conflation from slow-collector conflation, which is the distractor a descriptive reading picks |
| `flow_sharing_policy_and_replay_expiration` | Three explicit requirements against four viable-looking configurations, two independent timers, and two surprising defaults. This is the contract's "explicit requirements rather than a universal best practice determine the correct decision" |
| `shared_flow_try_emit_true_is_not_delivery` | The unbuffered `emit` contract, the no-subscriber path, what `tryEmit`'s boolean actually reports, and replay semantics — and the counter-intuitive direction of the answer, where success is the failure |
| `stream_choice_cannot_supply_a_delivery_guarantee` | Three retention configurations traced against a requirement that includes absent consumers and a process restart, ending in a decision that none of the offered mechanisms is the right kind of thing |

`flow_emit_must_keep_the_collector_context` was considered for `ADVANCED` — the plan named it a
strong candidate — and shipped `APPLIED`. One documented invariant supplies the key, and the
distractors test understanding of that one invariant rather than an interaction between several.
Calling it Advanced would have been levelling the subject rather than the reasoning.

### The three mapping corrections, all three taken

| Question | From | To | Why |
| --- | --- | --- | --- |
| `parent_cancellation_propagates_children` | `coroutine_jobs` | `coroutine_cancellation` | Its key is cooperative resumption and its three distractors need `cancelAndJoin`, `NonCancellable` and the difference between requesting and observing cancellation. E24-04 confirmed the Question became answerable only once Unit 3 shipped, while the Subtopic kept it in Unit 1's practice |
| `coroutine_async_exception_surfaces_at_await` | `coroutine_builders` | `coroutine_exceptions` | Its reasoning is failure propagation from a dropped `Deferred` and how `supervisorScope` changes it, which is L3.3 and L3.4. E24-04 confirmed it is answerable from Unit 3 and E24-02 confirmed it is not answerable from Unit 1 |
| `coroutine_run_blocking_main_thread` | `coroutine_fundamentals` | `coroutine_builders` | `runBlocking` is a builder that bridges blocking code, which L1.2 owns. Both Lessons are in Unit 1, so Unit practice is unaffected; what changes is that a `coroutine_builders`-scoped practice run now gets a builder Question, and `coroutine_fundamentals` keeps the suspension pair |

All three preserved `Question.id` and every `AnswerOption.id`: a Subtopic is where a Question is
filed, not what it asserts, so no historical attempt changes meaning. The first two are visible
as routing, and `unitPracticeRoutesReMappedQuestionsToTheUnitThatTeachesThem` in
`LearningUnitPracticeIntegrationTest` asserts that routing rather than the metadata, so a
re-mapping that drifted back would fail a test rather than pass quietly.

**One level change, decided separately from its mapping.**
`parent_cancellation_propagates_children` moves from `FOUNDATION` to `APPLIED`. Eliminating its
three distractors requires four separate cancellation contracts — that cancellation is observed
at cooperative points, that it does not preempt, that `cancel()` returns at once, and that a
suspending call in `finally` needs `NonCancellable` — which is past "one primary documented
concept determines the answer". It is not `ADVANCED`: each of the four is a direct documented
contract and none of them interacts subtly with another.

**Two levels reviewed and deliberately retained.** `callback_flow_await_close_registration` stays
`FOUNDATION`: one builder contract supplies the key and all three distractors fall to the same
contract, which is the rubric's own definition of the level, and the depth E24-05 was reaching
for is better served by a Question that applies the rule than by re-levelling one that
recognises it. `flow_combine_vs_zip_emission_rule` stays `FOUNDATION` for the same reason — it
distinguishes two documented operator contracts, and a startup precondition is part of one
contract rather than a second mechanism.

**One mapping drift deliberately retained.** `flow_share_in_vs_state_in` still sits on
`sharedflow` while its reasoning is L6.4's `flow_sharing`. It is outside the three corrections
this issue names, both Subtopics are Unit 6 concepts so Unit practice is unaffected, and moving
it would take `sharedflow` to two and `flow_sharing` to three rather than three and two. Recorded
again so a later reviewer does not have to re-derive the judgement.

### The explanation correction

`coroutine_run_interruptible_blocking_call`'s explanation ended with "withTimeout resumes the
coroutine while leaving the blocked thread occupied". The measurement was re-run for this issue
rather than inherited: `withTimeoutOrNull(100)` around
`withContext(Dispatchers.IO) { Thread.sleep(1500) }` returned `null` after **1,542 ms and
1,505 ms** across two runs, against a cooperative `delay(1500)` calibration under the same
deadline that returned after **103 ms**. The caller is not resumed at the deadline; it waits for
the blocking call. The clause now reads that a timeout does not abandon the call either — the
deadline cancels the block, but the coroutine is not resumed until the blocking method returns,
so the deadline bounds nothing here.

`Question.id`, all four `AnswerOption.id`s, the key, the level, the Subtopic and the assessed
claim are unchanged. This is an editorial correction to the same assessment responsibility, which
the contract keeps under the existing id, and no replacement Question was created.

### What the new Questions were built on

Every behavioural claim was executed before it was written down. A throwaway JVM probe, deleted
afterwards, measured all of the following against the resolved `kotlinx-coroutines-core:1.11.0`.

| Claim a Question depends on | Measured result |
| --- | --- |
| A timeout does not resume the caller around a blocking call | `null` after 1,542 / 1,505 ms for a 100 ms deadline; cooperative calibration 103 ms |
| A suspending cleanup in `finally` under a timeout does not complete | Completed **false** without `NonCancellable`, raising `TimeoutCancellationException`; **true** with it |
| Assigning an equal but newly built `data class` value emits nothing | **1** delivery for three assignments; the identical code with identity equality delivered **4** |
| `tryEmit` on an unbuffered `SharedFlow` with no subscriber | Returned **true**, `replayCache` empty |
| `extraBufferCapacity` retains nothing for an absent subscriber | Six emissions at `extraBufferCapacity = 64` with no subscriber; the later subscriber received **nothing** |
| `flow { withContext(IO) { emit(v) } }` | `IllegalStateException`, "Flow invariant is violated", naming both contexts |
| `retry` re-collects a cold upstream | Producer ran **3** times; collector received **Sending, Sending, Sending, Sent** |
| Concurrent `total += 1` is not atomic | **14,729** of 16,000; the same load through `AtomicInt` gave **16,000** |
| `debounce` emits nothing against a source faster than its timeout | `debounce(1000)` over 1.2 s of 50 ms readings produced **nothing**; `sample(300)` produced **3** values |
| `flatMapMerge` does not preserve the order inner flows were started in | `flatMapConcat` → `[A-1, A-2, B-1, B-2]`; `flatMapMerge` → `[B-1, B-2, A-1, A-2]`, A started first |
| A parent whose body has returned is completing, not complete | `isActive` true, `isCompleted` false, one child, printed `StandaloneCoroutine{Completing}`; `join()` resumed 237 ms later |
| A builder argument overrides the dispatcher and never the `Job` | Name `repo` retained, dispatcher `Dispatchers.IO`; child `Job` was not the scope's `Job` and its `parent` was |
| `replayExpirationMillis` runs on its own clock after the stop | At `WhileSubscribed(0, replayExpirationMillis = 200)` the value survived 100 ms after the last subscriber and was back at `initialValue` by 500 ms; with the default it still held the value at 600 ms |

Two Questions rest on documentation alone, because their claims are contractual rather than
observable: `coroutine_io_and_default_share_threads` (the `Dispatchers.IO` KDoc states the
shared-threads and elasticity clauses outright, and E24-05's measurement D already observed a
`flowOn(IO)` upstream running on a `Default` worker) and
`stream_choice_cannot_supply_a_delivery_guarantee`, whose keyed answer is about what survives a
process restart.

### Sources

Thirty-two distinct URLs are cited by the new and changed Questions, and **every one was opened
during this issue** and the supporting sentence located. All are `kotlinlang.org` except the
Android coroutines best-practices page, which supplies the main-safety convention and the
one-shot-versus-Flow division and nothing semantic.

The restructuring warning was honoured: no section was cited from memory. Three pages the epic
depends on were re-read and are unchanged from what E24-04 and E24-06 recorded —
`coroutines-cancellation.html` ("Cancellation and timeouts", 27 July 2026),
`coroutines-basics.html` (07 September 2026) and `shared-mutable-state-and-concurrency.html`
(27 September 2024). Where the rewritten guides no longer carry the detail, the 1.11.0 API
reference is cited instead: `withTimeout`, `debounce`, `sample`, `retry`, `onCompletion`,
`flattenConcat`, `merge`, `flowOn`, `flow`, `StateFlow`, `SharedFlow`,
`MutableSharedFlow.tryEmit`, `SharingStarted.WhileSubscribed`, `stateIn`, `shareIn`, `Job`,
`Deferred`, `launch`, `async`, `Dispatchers.IO`, `Dispatchers.Default` and `CoroutineContext`.

**The hot-flow warning was honoured too.** `hot_sharing_changes_production_not_retention` takes
its definition from the `SharedFlow` KDoc — the active instance exists independently of the
presence of collectors — and makes the guide page's looser "keep emitting values even when no
collector is active" one of its distractors, false precisely because `WhileSubscribed` stops the
upstream when the last subscriber leaves. That sentence is now assessed as a misconception rather
than quoted as a definition.

All three of the playbook's Part 7 source scripts were run over the whole bank after the batch
landed: 317 unique URLs all returned HTTP 200, all 285 distinct pages behind them rendered a
non-empty body, and all 48 `#fragment` citations resolved to a real anchor.

### Final Unit practice reach

Primary concepts only. Supporting mappings contribute nothing, which
`expandedUnitsConfigureOnlyTheirPrimaryConceptsAndDeduplicateProductionQuestions` asserts by
subtracting each Unit's supporting Subtopics from its scope and requiring the intersection to be
empty.

| Unit | Primary concepts | Before | After |
| --- | --- | ---: | ---: |
| Unit 1 — Coroutine Fundamentals and Structured Concurrency | `coroutine_fundamentals`, `coroutine_builders`, `coroutine_jobs`, `coroutine_scope`, `structured_concurrency` | 7 | **8** |
| Unit 2 — Coroutine Context, Dispatchers and Concurrent Work | `coroutine_context`, `coroutine_dispatchers`, `coroutine_context_switching`, `coroutine_parallelism` | 5 | **9** |
| Unit 3 — Cancellation, Failure and Coordination | `coroutine_cancellation`, `coroutine_exceptions`, `coroutine_supervision`, `coroutine_parallelism` | 7 | **12** |
| Unit 4 — Flow Fundamentals | `flow_fundamentals`, `flow_collection`, `flow_context` | 5 | **7** |
| Unit 5 — Flow Composition, Timing and Failure | `flow_operators`, `flow_buffering`, `flow_errors` | 7 | **11** |
| Unit 6 — StateFlow, SharedFlow and Hot Streams | `hot_vs_cold_streams`, `stateflow`, `sharedflow`, `flow_sharing` | 6 | **11** |

Unit 1's count moves by one rather than by three because two Questions left it in the same
change. That is the intended outcome: the Unit now practises five concepts it teaches instead of
seven Questions two of which a Unit 1 reader could not answer. The bank goes from 370 to 389
ACTIVE, 41 DEPRECATED is unchanged, and `async_reactive` goes from 38 to 57 ACTIVE — 2.19 per
Subtopic, the densest Topic in the bank and deliberately so, because six learning Units practise
it.

Two Questions that discuss this subject remain outside every Unit's practice by design and were
re-checked rather than assumed: `viewmodel_scope_cleared_cancellation` on `lifecycle_coroutines`
and `live_data_vs_state_flow_ui_state` on `livedata`. So do `performance_coroutine_scope_leak`,
`repository_observable_api_shape` and `durable_state_vs_one_off_event`, each of which is the
closest existing assessment of an E24 decision from another Topic's side. All five were read for
duplication; the new Questions were written to a different assessment responsibility in each
case — ownership of coroutine lifetime rather than retention, the one-shot half of the API
choice rather than the observable half, and whether any transient hot flow supplies a delivery
guarantee rather than how a UI models state against events.

### Remaining limitations for E24-09

- **The coordination-mechanism half of GAP-U3-B is unassessed anywhere in the bank.** Choosing
  among an atomic, confinement and a `Mutex` when an invariant spans more than one variable is
  taught in L3.5 and asked nowhere. The structural reason is recorded above: `coroutine_parallelism`
  is shared by L2.4 and L3.5, so the Question would enter Unit 2's practice as well.
- **GAP-U4-C is open by decision**, not by oversight.
- **`coroutine_parallelism`'s three Questions reach Unit 2 as well as Unit 3.** Two of them are
  Unit 2 material; `coroutine_shared_counter_lost_update` is not, and a Unit 2 learner meeting it
  will be reasoning past what Unit 2 taught. This is the price of the shared primary concept the
  plan assigned, and it is the one place where a Unit's practice is not fully bounded by what the
  Unit teaches.
- **`coroutine_jobs`, `structured_concurrency`, `coroutine_supervision` and
  `coroutine_context_switching` hold one Question each.** That is the bank's ordinary density and
  not a gap, but it means a short Unit 1 or Unit 3 run can miss a concept entirely.
- **Nothing in the assessment layer knows about Units.** `PracticeTargetResolver` derives a scope
  from primary concepts every time, so a later Lesson re-mapping silently changes practice. The
  two integration tests named above are the only thing that would notice.

---

## Verification outcomes for E24-09

Verified on 2026-09-11 against the final production bundles after E24-01 through E24-08.

### Production sequence and editorial review

The `async_reactive` Topic contains **6 Units / 29 Lessons** in this production order:

1. `unit_coroutines_and_structured_concurrency` — 5 Lessons
2. `unit_context_dispatchers_and_concurrency` — 4 Lessons
3. `unit_cancellation_failure_and_coordination` — 5 Lessons
4. `unit_flow_fundamentals` — 5 Lessons
5. `unit_flow_composition_timing_and_failure` — 5 Lessons
6. `unit_stateflow_sharedflow_and_hot_streams` — 5 Lessons

The stable Unit and Lesson identities match this plan. Reading all 29 Lessons in that order found
the intended prerequisite progression at every Unit boundary: Unit 2 refines Unit 1's Job and
ownership model with context and concurrency; Unit 3 completes the deferred cancellation,
failure, supervision and coordination model; Unit 4 applies coroutine lifetime and cancellation
to cold Flow collection; Unit 5 builds composition and delivery transformations on that model;
and Unit 6 keeps hotness, retained/replayed values and sharing policy separate. The paired
`launch(SupervisorJob())` discussions form one argument and do not imply universal parentage.

The terminology pass found no remaining contradiction or accidental duplicate curriculum.
Cancellation remains distinct from ordinary failure, concurrency from parallelism, buffering
from replay, conflation from latest cancellation, and hotness from retention and sharing policy.
`lesson_snapshot_flow` is consistent with Units 3–6: Snapshot observation remains Compose-owned,
its returned Flow is cold, collection owns its lifetime, state/event guidance agrees with Unit 6,
and both related Lesson IDs resolve. All 75 E24 related-Lesson links resolve; no E24 explanation
requires a future Lesson to be intelligible.

All 96 Kotlin code blocks were reviewed for plausible syntax, API names and agreement with their
surrounding claims. One concrete defect was corrected in L3.1: a broad-catch example claimed that
`ensureActive()` rethrows a received `CancellationException`, although an otherwise-active Job
would not do so. It now catches `Exception` and rethrows `CancellationException` unchanged before
handling ordinary failures. No new Lesson, section, example or Question was added.

### Reader, progress and practice verification

The production reader journey now derives every active Unit from the bundle rather than only the
Compose Topic. In a 400 x 900 phone-shaped window it exercised all 12 shipped Units / 50 Lessons,
including every E24 paragraph, bullet list, code block, comparison and callout. Reading-column
containment held, genuinely wide code/comparisons scrolled internally, and every one of E24's 129
Sources rendered as an operable title and passed its exact authored URL to the app URI boundary.
Source URL formatting and content rules passed repository validation. The URLs were not fetched
again because this issue changed no Source and E24-08's same-day freshness pass already verified
317 unique production URLs, 285 response bodies and 48 anchors.

Using the real study repository and production content, the journeys verified mark and unmark at
Lesson, Unit and `async_reactive` Topic level, including return from 29/29 to 28/29 and from 5/5 to
4/5. Continue Learning followed every production E24 Lesson identity in order, advanced within a
Unit, crossed every Unit boundary (including Unit 1 to Unit 2), returned to an unmarked earlier
Lesson, and produced `Complete` only after the whole authored learning document was exhausted.
That is the current product contract; E24 does not wrap or complete while unrelated authored
Lessons remain.

Production practice resolution remains primary-concept-only, excludes supporting mappings and
deduplicates repeated Questions. Final reach is **8, 9, 12, 7, 11 and 11** for Units 1–6. All six
Unit actions enter the existing Practice Builder with the derived availability/count.
`parent_cancellation_propagates_children` and `coroutine_async_exception_surfaces_at_await` reach
Unit 3 rather than Unit 1; `coroutine_run_blocking_main_thread` remains in Unit 1 through
`coroutine_builders`; supporting-only Questions do not leak into practice. The final bank contains
**389 ACTIVE / 41 DEPRECATED** Questions.

### Validation and disposition

The following checks passed:

- `python3 -m unittest discover -s tools -p 'test_*.py'` — 21 tool tests.
- `python3 tools/learning_question_coverage.py --check` after regenerating with `--write` — current.
- Targeted `:shared:jvmTest` runs for bundled curricula, codecs, end-to-end content, production
  reader/progress journeys, Continue Learning and Unit practice.
- `./gradlew --no-daemon :androidApp:assembleDebug :desktopApp:assemble :webApp:assemble :shared:check`
  — Android, JVM/Desktop, JS browser, Wasm/JS browser and iOS simulator tasks passed. Webpack's
  existing bundle-size warnings and Kotlin/Native metadata warnings remain non-failing.

`.github/project/validate_backlog.py` could not run locally because the environment has no
`PyYAML` module (`ModuleNotFoundError: No module named 'yaml'`); the backlog file was not changed.
The curriculum schema/identity/taxonomy/mapping/status/content/source checks, generated coverage
freshness and all platform checks above passed. The Question audit remains consistent with the
unchanged 389 ACTIVE / 41 DEPRECATED production bank.

Remaining non-blocking limitations are classified as follows:

- **Deferred future work:** the coordination-mechanism selection half of GAP-U3-B remains
  unassessed, and GAP-U4-C remains deliberately deferred.
- **Accepted structural limitation:** shared-primary `coroutine_parallelism` makes Unit 2 reach
  `coroutine_shared_counter_lost_update`, and mapping-derived routing means later primary mapping
  changes can alter Unit practice reach.
- **Intentional boundary:** several primary concepts have one Question; `lifecycle_coroutines`
  and `livedata` Questions remain outside E24 Unit practice.

No item is an unresolved defect or release blocker. E24-09 satisfies its acceptance criteria and
**E24 is ready to close** after review and merge.

---

## Source freshness and technical assumptions

### Configured versions this plan assumes

Read from `gradle/libs.versions.toml` and the module build files; the runtime version was
resolved, not assumed.

| Item | Value | How it was established |
| --- | --- | --- |
| Kotlin | 2.4.10 | `gradle/libs.versions.toml` |
| `kotlinx-coroutines` (catalog) | 1.11.0 | `gradle/libs.versions.toml` |
| Resolved `kotlinx-coroutines-core` | 1.11.0 | `./gradlew :shared:dependencyInsight --configuration jvmRuntimeClasspath --dependency org.jetbrains.kotlinx:kotlinx-coroutines-core` — selected by constraint over transitive requests for 1.9.0 (Koin) and 1.8.0 (skiko) |
| `kotlinx-coroutines-swing` | 1.11.0 | Catalog; desktop shell only |
| `kotlinx-coroutines-test` | 1.11.0 | Catalog; test source sets only |
| Compose Multiplatform | 1.11.1 | `gradle/libs.versions.toml` |
| Targets | Android, JVM/desktop, iOS (arm64, simulator arm64), JS browser, Wasm/JS browser | `shared/build.gradle.kts` |

Two consequences that examples depend on:

- **This is a Kotlin Multiplatform repository with five target families.** Every example must
  compile in principle in `commonMain`, or be labelled as platform-specific. That constrains
  `Dispatchers.Main`, `runInterruptible`, thread interruption and atomics — see below.
- **The repository's own coroutine usage is a legitimate grounding source.** It uses exactly
  one explicit dispatcher (`CoroutineScope(SupervisorJob() + Dispatchers.Default)` in
  `AppCoroutineScope`) and four `stateIn(..., SharingStarted.Eagerly, ...)` state holders. The
  `Eagerly` choice is a useful, honest contrast case for L6.4. **Corrected by E24-07:** this row
  originally read "an app-lifetime holder where `WhileSubscribed` would buy nothing", which is
  false. A process-lifetime scope sets the maximum lifetime only; `WhileSubscribed` on that same
  scope would still stop the upstream whenever the subscriber count reached zero. The defensible
  justification is the cost of keeping the derivation running against the cost of restarting it
  — see [corrections made during review of Unit 6](#corrections-made-during-review-of-unit-6).

### The coroutines documentation has been restructured

**This is the most important finding for authors, and it is not visible from a search result.**
Between the E23 review and this one, `kotlinlang.org` rewrote part of the coroutines guide.
Checked 2026-09-10:

| Page | State on 2026-09-10 |
| --- | --- |
| `coroutines-basics.html` | **Rewritten** ("Coroutines basics", dated 07 September 2026). Now frames builders as `CoroutineScope.launch()` extensions and covers suspension, scope, structured concurrency and `Job` |
| `coroutines-cancellation.html` | **Rewritten** and now titled **"Cancellation and timeouts"**, with sections for cooperative cancellation, `yield()`, explicit checks, interrupting blocking code, `NonCancellable` and timeout. Dated 27 July 2026; E24-04 also found a **prompt-cancellation section** and that the page no longer documents `withTimeout` itself |
| `coroutines-flow.html` | **Rewritten** and now titled **"Flows"**, with only two top-level sections — Cold flows and Hot flows |
| `coroutines-flow-operators.html` | **New page**, "Flow operators": intermediate/terminal, transforming, filtering, concurrent processing, combining, lifecycle, terminal |
| `exception-handling.html` | **Legacy guide structure**, still the authority for propagation, `CoroutineExceptionHandler` and supervision. Dated 20 July 2026 when E24-04 re-read it; the 1.11.0 `CoroutineExceptionHandler` KDoc carries a more precise "propagation path" model than the page's "root" wording |
| `coroutine-context-and-dispatchers.html` | **Legacy guide structure**; covers context, `Job` in the context, children, combining elements. **Does not cover `Dispatchers.IO` or `Dispatchers.Main`** |
| `shared-mutable-state-and-concurrency.html`, `channels.html`, `composing-suspending-functions.html` | **Legacy guide structure**, still current and still the best guide-level sources for their subjects. The shared-state page is dated 27 September 2024 and its atomic example is `AtomicInteger`, which is JVM-only |

Three instructions follow for E24-05 and E24-06 in particular:

1. **The rewritten Flow pages no longer cover several operators E24 teaches.** `conflate`,
   `collectLatest`, `combine` and `zip` are on the new operators page; `flatMapConcat`,
   `flatMapMerge`, `flatMapLatest`, `debounce`, `sample`, `retry`, `retryWhen` and
   `onCompletion` are **not** on either Flow page. For those, the `kotlinx.coroutines` API
   reference KDoc is the authority and is what a Lesson should cite.
2. **Do not cite a guide page from memory of its old contents.** Several sections that
   existed on the old "Asynchronous Flow" page — buffering, flow exceptions, exception
   transparency, composing, flattening — are no longer where they were.
3. Existing Question Sources still resolve: `coroutines-cancellation.html` and
   `coroutines-flow.html` both return 200 and both still support the claims attached to them,
   although both now do so from rewritten pages. No Question source needs changing for this
   reason alone.

**Re-checked by E24-07 on 2026-09-10, with one finding that constrains Unit 6.**
`coroutines-flow.html` still returns 200, is still dated 13 July 2026, and its "Hot flows"
section is a good source for the cold/hot split, for `MutableSharedFlow` with a backing
property, for the replay parameter, and for converting a cold flow with `shareIn`. It is
**not** a safe source for the definition of hotness: it says hot flows "keep emitting values
even when no collector is active", which is false for `shareIn(scope, WhileSubscribed())`,
whose upstream is stopped exactly when no subscriber is active — measured in
[Authoring outcomes for Unit 6](#authoring-outcomes-for-unit-6). Take the definition from the
`StateFlow` and `SharedFlow` KDocs, both of which say only that the active instance exists
independently of the presence of collectors. The page also does not document the
`SharingStarted` policies beyond naming `Eagerly` in one sentence, so the policy contracts,
`stopTimeoutMillis` and `replayExpirationMillis` come from the KDoc.

**Dates captured by E24-05 on 2026-09-10**, which E24-01 did not record: `coroutines-flow.html`
is dated 13 July 2026 and `coroutines-flow-operators.html` 28 July 2026. Their top-level section
structure is unchanged, but the *Flows* page carries anchored subsections beneath it that its
heading list does not show — see finding 1 in
[Authoring outcomes for Unit 4](#authoring-outcomes-for-unit-4). Three ACTIVE Question Sources
cite `coroutines-flow.html` under its old title "Asynchronous Flow", and one of those also cites
the `#flow-cancellation` anchor. **That anchor was checked against the live page and still
resolves**, as does every one of the three URLs, and the page still supports every claim
attached to them. The stale item is the citation *title*, not a broken link, so nothing here
obliges E24-08 to repair a Source.

### Verified claims

Reviewed 2026-09-10. Each row is a claim the Lessons depend on, with the page that supports
it. Every row was read on the page, not recalled.

| Claim the Lessons rely on | Source | Note for the author |
| --- | --- | --- |
| "Coroutines can suspend their execution instead of blocking a thread", and a suspending function "allows a running operation to pause and resume later"; a suspending function can only be called from another suspending function | <https://kotlinlang.org/docs/coroutines-basics.html> | L1.1's Core. Note what the page does *not* say: nothing about which thread |
| `runBlocking` "creates a coroutine scope and blocks the current thread until the coroutines launched in that scope finish"; use it "only when there is no other option" | <https://kotlinlang.org/docs/coroutines-basics.html> | L1.2 |
| Coroutines "form a tree hierarchy of parent and child tasks with linked lifecycles"; "A parent coroutine waits for its children to complete before it finishes. If the parent coroutine fails or gets canceled, all its child coroutines are recursively canceled too" | <https://kotlinlang.org/docs/coroutines-basics.html> | L1.5's three guarantees, in the source's own words |
| "coroutine cancellation is cooperative. Coroutines react to cancellation only when they cooperate by suspending or checking for cancellation explicitly"; `isActive`, `ensureActive()` and `yield()` are the three checks; "If a coroutine doesn't suspend for a long time, it also doesn't stop when it's canceled" | <https://kotlinlang.org/docs/coroutines-cancellation.html> | L3.1 |
| `NonCancellable` "is useful when you need to ensure that certain operations, such as closing resources with a suspending `close()` function, complete even if the coroutine is canceled"; `withTimeoutOrNull` returns `null` on timeout | <https://kotlinlang.org/docs/coroutines-cancellation.html> | L3.2. The page's own framing puts timeouts inside the cancellation model, which is the framing L3.2 adopts |
| Builders "come in two flavors: propagating exceptions automatically (`launch`) or exposing them to users (`async` and `produce`)" | <https://kotlinlang.org/docs/exception-handling.html> | L3.3's Core |
| "`CoroutineExceptionHandler` is invoked only on uncaught exceptions… all children coroutines delegate handling of their exceptions to their parent coroutine… until the root"; and "`async` builder always catches all exceptions and represents them in the resulting `Deferred` object, so its `CoroutineExceptionHandler` has no effect either" | <https://kotlinlang.org/docs/exception-handling.html> | The exact boundary E24-04's acceptance criteria require. Quote it rather than paraphrase |
| `CancellationException`s "are ignored by all handlers, so they should be used only as the source of additional debug information" | <https://kotlinlang.org/docs/exception-handling.html> | L3.1 and L3.3 — the cancellation-is-not-failure distinction |
| `SupervisorJob` is "similar to a regular Job with the only exception that a failure or cancellation of a child does not propagate to the supervisor job or its other children"; `supervisorScope` "propagates the cancellation in one direction only"; supervised children "do use the `CoroutineExceptionHandler` that is installed in their scope in the same way as root coroutines do" | <https://kotlinlang.org/docs/exception-handling.html> | L3.4. The third clause is the one that makes "each supervised child must handle its own failure" concrete |
| "The coroutine context is a set of various elements. The main elements are the `Job`… and its dispatcher"; a child "inherits its context… and the `Job` of the new coroutine becomes a child of the parent coroutine's job"; elements combine with `+` | <https://kotlinlang.org/docs/coroutine-context-and-dispatchers.html> | L2.1. The `Job` exception to inheritance is the Lesson's Senior point |
| `withContext` "may suspend the current coroutine and switch to a new context—provided the new context differs from the existing one… extra dispatches are required" | <https://kotlinlang.org/docs/coroutine-context-and-dispatchers.html> | L2.3, including the cost of a redundant switch |
| `Dispatchers.IO`'s parallelism "defaults to the limit of 64 threads or the number of cores (whichever is larger)"; "This dispatcher and its views share threads with the `Default` dispatcher, so using `withContext(Dispatchers.IO) { }` when already running on the `Default` dispatcher typically does not lead to an actual switching to another thread"; `limitedParallelism` views are not restricted by that limit | <https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-i-o.html> | L2.2's Senior material, and the basis of GAP-U2-B. Note the KDoc's own caveat that the limit is not strict |
| The three answers to shared mutable state are a thread-safe or atomic structure, thread confinement (fine- or coarse-grained), and `Mutex`; "`Mutex.lock()` is a suspending function. It does not block a thread"; `withLock` is the `lock`/`try`/`finally` shorthand; fine-grained confinement "works very slowly" | <https://kotlinlang.org/docs/shared-mutable-state-and-concurrency.html> | L3.5's whole structure, including the ordering of the three options |
| "cold flows are lazy. The code block of a cold flow builder doesn't run until a collector collects it. Each new collector starts a new execution of the flow"; "A cold flow is like a recipe" | <https://kotlinlang.org/docs/coroutines-flow.html> | L4.2's Core |
| "Flow collection is tied to the coroutine that calls the `collect()` function. When that coroutine is canceled, the collection stops, and the upstream flow is canceled too" | <https://kotlinlang.org/docs/coroutines-flow.html> | L4.3's Core |
| "By default, a cold flow runs in the same coroutine context as the collector"; `flowOn` "is context-preserving. It changes only the coroutine context of the upstream flow while keeping the downstream flow in the caller's context" | <https://kotlinlang.org/docs/coroutines-flow.html> | L4.4 |
| "By default, the collector applies backpressure to the upstream flow… the upstream flow suspends when the buffer is full"; `conflate()` "is a shorthand for `buffer(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)`"; conflate "doesn't cancel processing that has already started. To do that, use `collectLatest()` instead" | <https://kotlinlang.org/docs/coroutines-flow-operators.html> | L5.4. Establishing the default before the operators is what the page itself does |
| `combine` "emits a new value when any upstream flow emits a value, using the latest value from each upstream flow"; `zip` "combines the first value from each flow, then the second value from each flow… completes as soon as one of the upstream flows completes" | <https://kotlinlang.org/docs/coroutines-flow-operators.html> | L5.2 |
| `catch` "is _transparent_ to exceptions that occur in downstream flow and does not catch exceptions that are thrown to cancel the flow"; conceptually it wraps the upstream in a `try`/`catch` | <https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/catch.html> | L5.5. The KDoc's worked example, showing which operators are in and out of scope, is the one to adapt |
| `flatMapLatest` "switches to a new flow… every time the original flow emits a value. When the original flow emits a new value, the previous flow produced by `transform` block is cancelled" | <https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/flat-map-latest.html> | L5.3 |
| `flatMapMerge`'s `concurrency` "controls the number of in-flight flows, at most `concurrency` flows are collected at the same time" and defaults to `DEFAULT_CONCURRENCY`; it is a shortcut for `map(transform).flattenMerge(concurrency)` | <https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/flat-map-merge.html> | L5.3 |
| `StateFlow` "is a _hot_ flow because its active instance exists independently of the presence of collectors"; it "always has an initial value, replays one most recent value to new subscribers, does not buffer any more values"; "**Strong equality-based conflation**: Values in state flow are conflated using `Any.equals` comparison in a similar way to `distinctUntilChanged`"; "a slow collector skips fast updates, but always collects the most recently emitted value"; "State flow never completes" | <https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/> | L6.2 in full, and the basis of GAP-U6-A. "Strong equality-based conflation" is the KDoc's own heading and worth quoting |
| `SharedFlow` "shares emitted values among all its collectors in a broadcast fashion"; "Every new subscriber first gets the values from the replay cache and then gets new emitted values"; an unbuffered shared flow's "`emit` call… suspends until all subscribers receive the emitted value and returns immediately if there are no subscribers"; `extraBufferCapacity` and `onBufferOverflow` configure the rest; "Shared flow never completes" | <https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-shared-flow/> | L6.3 in full. The "returns immediately if there are no subscribers" clause is the honest version of "SharedFlow is for events" |
| `stateIn` converts a cold flow into a hot `StateFlow` started in a given scope; "Normal completion of the upstream flow has no effect on subscribers"; the suspending overload without an initial value "suspends until the first value is emitted" and throws `NoSuchElementException` if none is; applying `conflate`, `distinctUntilChanged` or `flowOn` to a `StateFlow` has no effect | <https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/state-in.html> | L6.4 |
| `SharingStarted` supplies START / STOP / STOP_AND_RESET_REPLAY_CACHE commands to the sharing coroutine; `WhileSubscribed`'s `stopTimeoutMillis` is "a delay… between the disappearance of the last subscriber and the stopping of the sharing coroutine. It defaults to zero"; `replayExpirationMillis` is the delay before resetting the replay cache and "defaults to `Long.MAX_VALUE` (keep replay cache forever, never reset buffer)" | <https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-sharing-started/> and <https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-sharing-started/-companion/-while-subscribed.html> | L6.4, and the basis of GAP-U6-B. Both defaults are surprising and both are worth stating |
| A `Channel` is "conceptually very similar to `BlockingQueue`" with suspending `send`/`receive`; unbuffered channels are rendezvous; fan-out means "Multiple coroutines may receive from the same channel, distributing work between themselves" | <https://kotlinlang.org/docs/channels.html> | L6.5's bounded comparison. Note the page does **not** compare Channels with Flows; that contrast is E24's own argument to make |
| Android guidance: inject dispatchers rather than hard-coding them; suspending functions should be main-safe and that is the callee's responsibility; the ViewModel should create coroutines and expose immutable state; the data layer should expose suspend functions for one-shot calls and Flows for changes; avoid `GlobalScope`; make coroutines cancellable | <https://developer.android.com/kotlin/coroutines/coroutines-best-practices> | L1.4, L2.2, L2.3, L4.1. Use it for the *guidance*, not for semantics — every semantic claim above is sourced to Kotlin's own documentation |

### Version-sensitive findings that constrain authoring

#### What `withTimeout` does to non-cooperative work

Measured on this project's JVM target against the resolved `kotlinx-coroutines-core:1.11.0`,
because the natural way to phrase L3.2's Senior point turned out to be wrong and the
correction is worth having as data rather than as reasoning. Each case used a 100 ms deadline
and `withTimeoutOrNull`:

| Case | Body | Returned | Elapsed |
| --- | --- | --- | --- |
| A | `withContext(Dispatchers.IO) { Thread.sleep(1500) }` | `null` | **1523 ms** |
| B | a CPU loop running ~1500 ms with no suspension point or cancellation check | **the computed value**, not `null` | **1501 ms** |
| C | `join()` on a job launched into a **separate, non-child** scope | `null`; the job was still active afterwards | 108 ms |

What this establishes, and what L3.2 must teach:

- **`withTimeout` bounds nothing by itself.** Its body is a child scope, so structured
  concurrency prevents it from returning until that body finishes. A and B both overran the
  deadline by roughly fifteen times, and the *caller* waited the whole time.
- **B is the sharper case.** The block not only outlived its deadline, it **succeeded** — the
  coroutine never reached a cancellation check, so the timeout's cancellation was never
  observed and the value was returned as though nothing had happened. A reader who believes a
  timeout is a hard deadline will not predict this.
- **C is the only case where the caller moves on early**, and it does so precisely because the
  awaited work was never a child. That is L1.4's ownership argument arriving from a different
  direction, and it is the correct place to make the connection.

**Re-run by E24-04 on 2026-09-10.** All three cases reproduced against the same resolved
artifact, with one correction to how the numbers should be read: this harness carries roughly
100 ms of fixed overhead, established with calibration cases E24-01 did not have. The
conceptual findings are unchanged. See
[the timeout measurement, re-run](#the-timeout-measurement-re-run).

**Consequence for the question bank.** The ACTIVE Question
`coroutine_run_interruptible_blocking_call` explains its `withTimeout` distractor with the
clause "withTimeout resumes the coroutine while leaving the blocked thread occupied."
Measurement A is that exact scenario and contradicts it: the coroutine was not resumed until
the blocking call returned. **The Question's key and its four options remain correct** — the
distractor claims `withTimeout` "abandons the blocking method", which is genuinely false — so
this is an inaccuracy in one explanatory clause, not a wrong answer. It is recorded as a
correction candidate for E24-08 rather than a defect to fix here, and E24-04 should re-run
the measurement before anything is changed. See
[mapping corrections](#mapping-corrections-rather-than-gaps).

#### Experimental operator status

`flatMapLatest` and `flatMapMerge` are annotated `@ExperimentalCoroutinesApi` in the API
reference for the configured version. L5.3 must state this rather than present them as
settled API, and must not present the annotation as a reason to avoid them — they are the
standard answers to the problems they solve, and the annotation is a stability contract, not
a warning. Re-check the annotation at authoring time; `debounce` and `sample` should be
re-checked at the same moment, as their status has moved historically.

**Re-checked by E24-06 on 2026-09-10, and one half of the note above was wrong.** Every
annotation below was read from the declaration in this repository's *resolved*
`kotlinx-coroutines-core:1.11.0` sources jar rather than from the published API reference,
so it is the status of the artifact this project actually compiles against.

| Operator | Annotation in the resolved 1.11.0 artifact |
| --- | --- |
| `map`, `filter`, `transform`, `distinctUntilChanged` | none — stable |
| `onStart`, `onEach`, `scan`, `take`, `drop` | none — stable |
| `combine`, `combineTransform`, `zip` | none — stable |
| `merge` (`Iterable<Flow<T>>.merge`, `merge(vararg)`) | none — stable |
| `buffer`, `conflate`, `flowOn`, `collectLatest` | none — stable |
| `catch`, `retry`, `retryWhen`, `onCompletion` | none — stable |
| `flatMapConcat`, `flatMapMerge`, `flatMapLatest`, `transformLatest`, `mapLatest`, `flattenConcat`, `flattenMerge` | `@ExperimentalCoroutinesApi` |
| **`debounce`, `sample`, `DEFAULT_CONCURRENCY`** | **`@FlowPreview`** |

E24-01 implied that `debounce` and `sample` might carry the same annotation as the
flattening operators. They do not: they carry `@FlowPreview`, which is a **different and
stronger** opt-in. `@ExperimentalCoroutinesApi` says the semantics may change in a way that
breaks some code; `@FlowPreview`'s own KDoc says the declaration has *no* backward
compatibility guarantees, binary or source, and that its API and semantics "can and will be
changed in next releases". Both are declared `@RequiresOptIn(level = WARNING)`, so both cost
an `@OptIn` and nothing else. L5.1 states the `@FlowPreview` status and the distinction;
L5.3 states the `@ExperimentalCoroutinesApi` status and that `merge` carries no annotation
at all. Neither Lesson presents an annotation as a reason to avoid an operator.

#### Dispatchers and Kotlin Multiplatform

`coroutine-context-and-dispatchers.html` documents `Dispatchers.Default` and
`Dispatchers.Unconfined` and does **not** document `Dispatchers.IO` or `Dispatchers.Main` —
those live in the API reference. Two consequences for L2.2:

- E24-03 precision correction: `Dispatchers.Main` is declared in `commonMain`, but a usable
  implementation depends on platform/runtime. JVM requires an integration artifact; Darwin
  uses its main queue; JS/Wasm delegates to Default with immediate support in 1.11.0.
  Shared code can accept a dispatcher without assuming Android's realization everywhere.
- `Dispatchers.IO` is a JVM/Android and Native concept. Examples that name it should be
  framed as Android or JVM examples, not as `commonMain` code.

The `kotlinx-coroutines-swing` dependency in the catalog exists to supply the desktop main
dispatcher, which is concrete in-repository evidence that `Main` is platform-supplied rather
than free.

#### Atomics in common code

L3.5's atomic option cannot be demonstrated with `java.util.concurrent.atomic` in a
Kotlin Multiplatform lesson. Kotlin 2.1 introduced common `kotlin.concurrent.atomics`, and
this repository is on Kotlin 2.4.10, so the common API is expected to be available — but its
experimental status was **not verified** in this review and must be checked at authoring
time. If it is still opt-in, L3.5 should teach the *decision* (an atomic fits one independent
variable) and show the platform-specific code as a JVM example rather than presenting an
unavailable common API.

**Resolved by E24-04 on 2026-09-10.** The API *is* available from `commonMain` for every
target this repository declares, and it *is* still `@ExperimentalAtomicApi` at
`RequiresOptIn.Level.ERROR`; on JS and Wasm the implementation is trivial and not
thread-safe. Both halves were checked against the resolved stdlib artifact and by compiling
a probe in this repository's own common source set. L3.5 ships the common API with the
`@OptIn` line visible and the caveats stated — see
[Atomics in common code, resolved](#atomics-in-common-code-resolved).

#### `runInterruptible` is JVM-only

Thread interruption is a JVM mechanism. The existing Question
`coroutine_run_interruptible_blocking_call` is correct and its Sources are sound, but L3.2
must scope the claim to the JVM rather than presenting interruption as part of the coroutine
model everywhere.

**E24-04 measured what it changes.** The same blocking call that overran a 100 ms deadline by
1.6 s unwrapped stopped at the deadline once wrapped in `runInterruptible`, and the
`InterruptedException` was not visible outside the block — it reaches the coroutine as a
`CancellationException`. Case D in
[the timeout measurement](#the-timeout-measurement-re-run).

### Unresolved questions

1. ~~Whether `kotlin.concurrent.atomics` is still experimental on Kotlin 2.4.10.~~
   **Answered by E24-04:** available from common code, and still experimental at opt-in
   error level. See [Atomics in common code](#atomics-in-common-code).
2. ~~Whether `flatMapLatest`, `flatMapMerge`, `debounce` and `sample` still carry
   `@ExperimentalCoroutinesApi` when E24-06 authors Unit 5.~~ **Answered by E24-06:** the
   flattening operators do; `debounce` and `sample` carry `@FlowPreview` instead, which is
   the stronger opt-in. Every operator Unit 5 teaches was read from the resolved artifact.
   See [Experimental operator status](#experimental-operator-status).
3. ~~`DEFAULT_CONCURRENCY`'s numeric value was not read from the resolved artifact.~~
   **Answered by E24-06:** it is **16**, read from the declaration, and on the JVM it is
   overridable through the `kotlinx.coroutines.flow.defaultConcurrency` system property.
   L5.3 teaches the number with that qualification. See
   [the `DEFAULT_CONCURRENCY` decision](#the-default_concurrency-decision).
4. Whether the remaining legacy guide pages — exception handling, context and dispatchers,
   shared mutable state, channels — will be rewritten during E24's authoring window. They
   were current on 2026-09-10, and E24-04 re-checked exception handling, shared mutable state
   and channels on the same date and found them unchanged in structure. Any authoring issue
   that finds a rewritten page should record it here rather than silently citing the new one.

---

## Boundaries with later curriculum

Stated concept by concept so a later authoring agent cannot absorb them by accident.

### E25 owns Compose coroutine and effect integration

E24 does **not** teach `LaunchedEffect`, `DisposableEffect`, `SideEffect`,
`rememberCoroutineScope`, `produceState`, `collectAsState` / `collectAsStateWithLifecycle`,
`repeatOnLifecycle` / `flowWithLifecycle`, Compose lifecycle collection, production effect
selection, or production Compose screen-state patterns. Where a Lesson needs a collection
site — L4.3 in particular — it names the API in one sentence and points at the Unit that owns
it. `snapshotFlow` itself stays owned by the shipped `lesson_snapshot_flow` and the Compose
blueprint's Unit 6; E24 links to it and does not restate it.

### The architecture curriculum owns application state architecture

E24 does **not** teach MVVM, MVI, ViewModel architecture, repository architecture,
state-holder architecture, `UiState` modelling, or application-level state ownership. These
appear only as bounded supporting context: `repository_pattern` in L2.3 and L4.1,
`state_ownership` in L6.2 and L6.5, `error_modeling` in L3.3 and L5.5,
`single_source_of_truth` in L4.1. None of them is primary anywhere in E24, so none becomes
Unit practice.

### E31 owns coroutine and Flow testing

E24 does **not** teach `runTest`, test schedulers, virtual time, `TestDispatcher`,
`Dispatchers.setMain`, Turbine, or Flow testing generally. Dispatcher injection is taught in
L2.2 and L2.3 as a **design** point; its testing motivation is named in one sentence and
deferred. The five testing-Topic Questions listed in the review are E31's, not E24-08's.

### Other boundaries

- **RxJava migration is out of scope.** `rxjava_fundamentals` and `flow_vs_rxjava` are
  unmapped by any Lesson and hold no active Questions.
- **Coroutine internals are excluded** beyond one sentence — "a suspended coroutine is a
  stored continuation" — which L1.1 needs for its cost argument.
- **Channel treatment is bounded and justified.** One paragraph in L3.5 (communicate rather
  than share) and one Lesson section in L6.5 (single-receiver delivery as the property no
  Flow shape offers). The justification is that L6.5's decision is incomplete without it and
  that the bank already assesses exactly this contrast in
  `flow_vs_channel_delivery_model`. A full Channel curriculum — pipelines, fan-in, fan-out,
  ticker channels, `produce` — stays excluded.
- **`LiveData` is Reference only**, one table row in L6.2.

---

## Documentation changes made by this review

Three, each in the same change as this document. None changes production content.

1. **`docs/content/coroutines-flow-learning-blueprint.md` added** — the subject blueprint
   this plan confirms, following the precedent of the Compose blueprint.
2. **`AGENTS.md` documentation map updated** — both new documents are registered under
   "Content — learning", which is where E23's two planning documents are listed.
3. **`docs/content/compose-learning-blueprint.md` corrected, bounded.** Its Unit 6 note and
   its then-Unit 8 notes both point at "the Flow curriculum" as an abstract future subject,
   written when none existed. Two pointers now name the blueprint that does. Nothing else in
   that file changes, and no Compose Lesson boundary moves: the Compose Units keep every Flow
   concept as supporting context exactly as before.

No correction to the Compose blueprint's **content** was needed. Its treatment of Flow is
already correctly bounded — the then-Unit 8's L8.2 explicitly bridges sharing strategies,
operators, buffering and cancellation to the Flow curriculum rather than teaching them —
which is what made the E24 boundary easy to draw. E25-01 later rewrote that Unit and kept the
same bridge depth; see `compose-units-7-12-plan.md`.

---

## Handoff

### For E24-02 through E24-07

- Take Unit and Lesson ids from the
  [identity tables](#identity-conventions-and-proposed-identities) verbatim. They are checked
  unique against production content; changing one after it ships costs a learner's
  study-progress record.
- Primary and supporting mappings are settled and valid. Keep primary mappings to the
  concepts the Lesson genuinely owns — they are what Unit practice selects on. In particular,
  **no E24 Lesson takes a non-`async_reactive` Subtopic as primary**, which is what keeps the
  architecture, performance, Compose and testing boundaries from leaking into Unit practice.
- **Forward links are invalid until their target ships.** See
  [Cross-linking rules](#cross-linking-rules). Each issue may link backwards only; pointing
  forward is prose naming the Unit.
- Prerequisites outside this subject — Kotlin lambdas, exceptions, equality, collections and
  sequences; threads; Android lifecycle and main-thread concepts; repository and error
  modelling — have no authored learning Unit and must be bridged in place to the depth the
  Lesson needs, then pointed at their Topic. **No Lesson in Units 1–6 depends on an
  unpublished Lesson except in the epic's own order.**
- Read [source freshness](#source-freshness-and-technical-assumptions) before searching for
  anything. The coroutines guide has been restructured and several operator subjects are no
  longer on the pages that used to hold them.
- E24-05 additionally owns the `snapshotFlow` reconciliation, which is the epic's only edit to
  already-shipped content.
- Regenerate `docs/content/learning-question-coverage.md` in the same change that adds
  Lessons: `python3 tools/learning_question_coverage.py --write`.
- Record what authoring found in this document, following the precedent of the Compose plan's
  per-Unit "Authoring outcomes" sections, so the next issue re-checks findings rather than
  re-deriving them.

### For E24-08

**Every Unit this epic plans is now authored**, so E24-08 begins against finished prose
rather than against predictions. The gap rows are the starting list — sixteen at review time,
eighteen after E24-06 added GAP-U5-C and GAP-U5-D, twenty-one after E24-07 added GAP-U6-D,
GAP-U6-E and GAP-U6-F. The three mapping corrections and the one explanation correction are
separate, smaller decisions.

Each authoring issue recorded, in its own outcomes section, whether the gaps it inherited
survived contact with the finished Lessons and where the reasoning for each now lives. **None
of the sixteen original gaps was closed by authoring**, which is the expected outcome: a
Lesson teaches reasoning, and only a Question assesses it. Read those sections rather than
re-auditing what the Lessons teach.

For Unit 6 specifically, [Authoring outcomes for Unit 6](#authoring-outcomes-for-unit-6)
answers the questions E24-08 would otherwise have to re-derive: GAP-U6-A, GAP-U6-B and
GAP-U6-C are all still open and each row names exactly which measured material a Question
could be built on; no existing Question became semantically insufficient, and none is wrong;
the one mapping drift on `flow_share_in_vs_state_in` is confirmed and unchanged; no Source or
explanation defect was found in the six Questions the Unit reaches; and `sharedflow_001` is
DEPRECATED on ground L6.3 now teaches, so a new Question must not re-ask it. One source
finding constrains authoring there: the rewritten `coroutines-flow.html` "Hot flows" section
says hot flows keep emitting when no collector is active, which is false as a general claim
and is contradicted by this epic's own measurement of `WhileSubscribed`. Cite the `StateFlow`
and `SharedFlow` KDocs for that definition instead.

The explanation correction is the only item here that is a possible **defect** rather than a
coverage gap: `coroutine_run_interruptible_blocking_call` attaches a rationale to its
`withTimeout` distractor that this review measured and contradicted. Re-run the measurement
recorded in [the timeout section](#what-withtimeout-does-to-non-cooperative-work) before
changing anything, and correct the clause rather than adding a Question.

Three things to weigh that are not in the table:

- GAP-U3-B (races and coordination) is the only gap where **nothing in any Topic** assesses
  the reasoning. It is the widest.
- GAP-U2-B (`IO` and `Default` share threads) and GAP-U4-B (the context invariant) are the two
  places where the bank's silence coincides with folklore being wrong, which is where a
  Question earns its `ADVANCED` level on merit.
- The Flow half of the Topic has no `ADVANCED` Question at all while carrying the epic's
  subtlest reasoning. That is an observation, not a quota.

`docs/content/question-bank-coverage.md` was **not** regenerated by this issue, because no
Question changed. Its counts were last regenerated by E23-07 at 370 ACTIVE / 41 DEPRECATED,
which matches the current bundle exactly — verified during this review rather than assumed.
E24-08 regenerates it and `learning-question-coverage.md` together.

**E24-08 is done.** What it decided about each of the twenty-one gaps, the three mapping
corrections and the explanation clause is recorded in
[Assessment outcomes for E24-08](#assessment-outcomes-for-e24-08). Read that section rather
than re-deriving anything from the paragraphs above, which are the record of what was handed
over and not of what happened.

### For E24-09

The six Units read in blueprint order, and the concept boundaries in this document are what
the cross-Unit review should test:

- No concept is taught twice. The deliberate exceptions are the two split traps —
  `launch(SupervisorJob())` across L2.1 and L3.4, and cancellation named in L1.3 but owned by
  L3.1 — both of which should read as one argument across two Lessons rather than two
  attempts at the same one.
- Each Unit's prerequisites are satisfied by an earlier Unit or bridged in place.
- No Lesson links to material that does not exist.
- Terminology is consistent across the six Units **and with the shipped Compose Units**:
  "suspend", "scope", "context", "cancellation", "cold", "hot", "conflated", "collector".
  `lesson_snapshot_flow` already uses several of these, and its four-fact bridge is the
  reference point they must not contradict.
- The `snapshotFlow` reconciliation landed: no sentence in the shipped Compose content still
  says the coroutines and Flow curriculum does not exist.

One limitation to carry forward: `lifecycle_coroutines` and `livedata` each hold an active
Question that no E24 Unit reaches through a primary mapping. That is intended — lifecycle
integration is E25's and the architecture curriculum's subject — but it means those two
Questions remain reachable only through the general assessment flow, not through any new
Unit's practice.

E24-08 added four more, listed in full under
[Remaining limitations for E24-09](#remaining-limitations-for-e24-09): the
coordination-mechanism half of GAP-U3-B and GAP-U4-C are open by decision rather than by
oversight, `coroutine_parallelism`'s Questions reach Unit 2 as well as Unit 3 because both
Units take it as a primary concept, and four Subtopics hold one Question each. None of them is
a defect to fix in E24-09; they are the shape of the practice a cross-Unit review should read
before judging it.
