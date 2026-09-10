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
| E24-08 | [Assessment gaps](#assessment-gaps-for-e24-08) in full, re-checked against the finished Lessons, plus the [mapping corrections](#mapping-corrections-rather-than-gaps) |
| E24-09 | [Handoff](#handoff) — sequencing, cross-Unit links, and the limitations recorded here |

Each authoring issue also reads the outcomes the previous ones recorded, so a finding is
re-checked rather than re-derived: [Authoring outcomes for Unit 1](#authoring-outcomes-for-unit-1)
is the first of those and is required reading for E24-03 and E24-08.

**Authoring status.** Unit 1 is authored and shipped by E24-02; see
[Authoring outcomes for Unit 1](#authoring-outcomes-for-unit-1). Unit 2 is authored and
shipped by E24-03; see [Authoring outcomes for Unit 2](#authoring-outcomes-for-unit-2).
Unit 3 is authored and shipped by E24-04; see
[Authoring outcomes for Unit 3](#authoring-outcomes-for-unit-3). Unit 4 is authored in
production format by E24-05, pending review and merge; see
[Authoring outcomes for Unit 4](#authoring-outcomes-for-unit-4). Units 5 and 6 remain
proposed.

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
| L6.4 | `lesson_sharing_cold_flows` | `stateIn`, `shareIn` and `SharingStarted` | `flow_sharing` | `stateflow`, `sharedflow`, `coroutine_scope`, `lifecycle_coroutines` |
| L6.5 | `lesson_choosing_a_stream_abstraction` | Choosing Between a Value, a Flow, a State Holder and a Channel | `hot_vs_cold_streams` | `stateflow`, `sharedflow`, `flow_sharing`, `state_ownership` |

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
  owned by E25 and the Compose blueprint's Unit 8.

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
  by the Compose blueprint's Unit 8 and the architecture curriculum. `LiveData` is one
  Reference row.

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
| `coroutines-flow-operators.html` | "Flow operators", dated **28 July 2026** | Supplies the intermediate/terminal definitions L4.2 quotes and the list of terminal operators it names |
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
| GAP-U6-A | Unit 6 / `lesson_state_flow` | `StateFlow`'s equality-based conflation as behaviour: that assigning an equal value emits nothing, and that a state type's `equals` therefore decides what the UI sees | Both `stateflow` Questions are descriptive — what `StateFlow` is for, and how it compares with `SharedFlow`. The one behavioural statement in the bank is a distractor-adjacent claim in `live_data_vs_state_flow_ui_state`, which sits on `livedata` and is unmapped by E24 | Add coverage in E24-08; the strongest candidate in Unit 6 |
| GAP-U6-B | Unit 6 / `lesson_sharing_cold_flows` | `Eagerly` against `WhileSubscribed` as a resource and correctness tradeoff, and what `replayExpirationMillis` does | `flow_state_in_while_subscribed` assesses one parameter of one policy. The choice between policies — the actual decision — is unassessed | Add coverage in E24-08 |
| GAP-U6-C | Unit 6 / `lesson_hot_and_cold_streams` | The hot/cold distinction posed directly: what a late subscriber receives from each, and what happens to values emitted with no subscribers | `hot_vs_cold_streams`' single active Question is the `Channel` comparison, which belongs to L6.5. `flow_fundamentals_001` assesses coldness from the cold side only | Add coverage in E24-08 |

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
  `Eagerly` choice is a useful, honest contrast case for L6.4 — an app-lifetime holder where
  `WhileSubscribed` would buy nothing.

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

**Dates captured by E24-05 on 2026-09-10**, which E24-01 did not record: `coroutines-flow.html`
is dated 13 July 2026 and `coroutines-flow-operators.html` 28 July 2026, both with the section
structure above unchanged. E24-05 also confirmed that the intermediate-against-terminal
distinction lives **only** on the operators page, and that three ACTIVE Question Sources cite
`coroutines-flow.html` under its old title "Asynchronous Flow" — one of them with a
`#flow-cancellation` anchor the rewritten page no longer has. All three resolve and all three
still support their claims; see
[Authoring outcomes for Unit 4](#authoring-outcomes-for-unit-4).

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
2. Whether `flatMapLatest`, `flatMapMerge`, `debounce` and `sample` still carry
   `@ExperimentalCoroutinesApi` when E24-06 authors Unit 5. Verified experimental for the
   first two on 2026-09-10; the others were not individually checked.
3. `DEFAULT_CONCURRENCY`'s numeric value was not read from the resolved artifact. L5.3 should
   either read it or describe it by name rather than asserting a number.
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
   its Unit 8 notes both point at "the Flow curriculum" as an abstract future subject,
   written when none existed. Two pointers now name the blueprint that does. Nothing else in
   that file changes, and no Compose Lesson boundary moves: the Compose Units keep every Flow
   concept as supporting context exactly as before.

No correction to the Compose blueprint's **content** was needed. Its treatment of Flow is
already correctly bounded — Unit 8's L8.2 explicitly bridges sharing strategies, operators,
buffering and cancellation to the Flow curriculum rather than teaching them — which is what
made the E24 boundary easy to draw.

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

The sixteen gap rows are the starting list. The three mapping corrections and the one
explanation correction are separate, smaller decisions. Re-read all of them against the
finished prose before authoring: a gap this plan predicted may have been closed by a Lesson
that turned out deeper than planned, and new ones will have appeared.

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
