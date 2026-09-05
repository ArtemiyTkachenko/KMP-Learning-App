# Compose Learning Blueprint

## Purpose

This is the complete learning map for Jetpack Compose, produced under
`docs/content/learning-content-authoring.md` before any production Compose lesson is
authored. It is the first Topic mapped under that contract and doubles as the worked
example of what a blueprint looks like.

It is a **plan, not content**. No lesson text is authored here, and nothing in this file is
a runtime artifact. Authoring proceeds incrementally, Unit by Unit, against this map.

Home Topic: `android_ui` (UI — Views & Jetpack Compose).

Scope: 14 Learning Units, 50 planned Lessons, plus explicit Reference and Exclude
decisions and a record of concepts the current assessment taxonomy cannot express.

## How to Read This Blueprint

Every planned Lesson records:

- **Objective** — what the learner should be able to do afterwards.
- **Core / Practical / Senior** — the concepts belonging to each depth layer, per Rule 6 of
  the authoring contract. A missing Senior line means deeper material would be artificial.
- **Primary** — Subtopic IDs the Lesson is responsible for teaching thoroughly.
- **Supporting** — Subtopic IDs the Lesson explains only far enough to stay understandable,
  including cross-Topic bridges. Cross-Topic IDs are annotated with their owning Topic.
- **Notes** — Teach/Bridge/Reference/Exclude decisions, prerequisites, and pointers to
  deeper future content.

Every Topic and Subtopic ID below is a real ID from the bundled curriculum
(`shared/src/commonMain/composeResources/files/curriculum/initial_curriculum.json`).
Concepts with no exact Subtopic are recorded in
[Taxonomy gaps](#taxonomy-gaps-concepts-with-no-exact-assessment-subtopic) rather than
given invented IDs. **This blueprint does not change the question taxonomy.**

Existing Compose question coverage was reviewed as an input (see
`docs/content/question-bank-coverage.md`), but the Unit structure is designed for learning
and deliberately does not mirror the Subtopic list one-to-one.

## Unit Order and Why It Matters

1. Thinking in Compose
2. State and State Ownership
3. Recomposition
4. Identity, Keys, Stability and Immutability
5. Derived State and Expensive Work
6. Snapshot Fundamentals
7. Effects and Composable Lifecycle
8. ViewModel, Flow and Production Screen State
9. Modifiers and Layout
10. Lazy Layouts
11. CompositionLocal, Theme and Ambient Dependencies
12. Accessibility and Semantics
13. Compose Performance Mental Model
14. Views and Compose Interoperability

The order encodes conceptual dependencies, not convenience:

- **Effects (7) after execution semantics (1) and recomposition (3).** `LaunchedEffect`
  only makes sense once the learner knows a composable body may run repeatedly and is not
  a one-time imperative lifecycle callback. Taught earlier, effects become memorized
  incantations.
- **Stability (4) after recomposition (3).** Skipping cannot be explained before there is
  something to skip. Stability introduced first is just annotation folklore.
- **Derived state (5) after identity and stability (4).** `remember(key)` and
  `derivedStateOf` are answers to "this recomputes more often than it needs to", which
  requires knowing when and why recomposition happens.
- **Snapshots (6) after state (2), recomposition (3) and derived state (5).** The snapshot
  system explains *why* everything in Units 2–5 behaves as it does. Placed first it is
  abstract theory; placed here it is the unifying mechanism.
- **ViewModel and Flow (8) after state ownership (2).** Integration builds on ownership
  rather than defining it. A learner who meets `ViewModel` first concludes that "state
  goes in the ViewModel", which is the wrong mental model.
- **Layout (9) after the state model.** Modifiers and layout are largely independent of
  the state story, so they come after it rather than interleaved; deferred reads in
  Unit 13 then have both halves available.
- **Lazy layouts (10) after identity and keys (4)** and after layout (9). Lazy list keys
  are a direct application of composition identity.
- **Performance (13) last but one.** By that point every mechanism it depends on —
  recomposition, stability, derived state, layout phases, lazy content — has been taught,
  so performance is synthesis rather than a random list of optimization tricks.
- **Interop (14) last.** It is a migration concern that assumes both models are understood.

---

## Unit 1 — Thinking in Compose

**Purpose:** establish the declarative mental model before any API is introduced.
**Prerequisites:** none. This is the entry point of the Compose path.

#### L1.1 — Declarative UI and Why Compose Exists

- **Objective:** explain what changes when UI is described rather than mutated, and what
  problem that solves in real Android apps.
- **Core:** imperative View trees (inflate, find, mutate) vs. describing UI for a given
  state; the class of bugs that comes from UI state and view state drifting apart; "UI is a
  function of state".
- **Practical:** the same small screen expressed both ways; where the imperative version
  accumulates state-sync bugs; why this is a maintainability argument, not a syntax
  preference.
- **Primary:** `compose_fundamentals`
- **Supporting:** `views_fundamentals`, `view_rendering`
- **Notes:** Views appear here only as the contrast case — **Bridge**, not Teach. The full
  View story is Unit 14 and the Views half of `android_ui`. Do **not** turn this into an
  XML tutorial; `xml_layouts` stays out.

#### L1.2 — What a Composable Is and How It Executes

- **Objective:** describe the execution rules of a composable function accurately enough
  that effects and recomposition later make sense.
- **Core:** a composable is a function that emits UI; it may execute repeatedly; it may be
  skipped; sibling execution order must not be relied on; optimistic recomposition may be
  cancelled and its work discarded; it must not be treated as a one-time lifecycle
  callback; composables should be fast, idempotent, and side-effect free.
- **Practical:** what breaks when a composable body mutates external state, starts work, or
  assumes it runs once; why a counter incremented in a composable body is a bug rather than
  a feature.
- **Senior:** why these constraints exist at all — they are what allow the runtime to skip,
  reorder, and re-execute freely, which is the source of Compose's performance model.
- **Primary:** `compose_fundamentals`
- **Supporting:** `compose_recomposition`
- **Notes:** This is the single most important lesson for everything after it. Recomposition
  appears only as "it can run again" — the mechanism is Unit 3. Compiler-generated
  signatures and runtime internals are **Exclude**. Do **not** teach that composables
  currently run in parallel: the Compose documentation states that they cannot presently be
  run in parallel and asks only that code be written so a future multithreaded runtime
  would still be correct. Teach the contract — the number, timing, and relative order of
  executions are the runtime's decision — rather than a threading claim that is wrong today
  and would date an interview answer.

#### L1.3 — State Down, Events Up

- **Objective:** explain unidirectional data flow in Compose and why the direction matters.
- **Core:** state flows down the tree, events flow up; a composable renders what it is
  given and reports what happened; there is one owner of each piece of state.
- **Practical:** the `value` / `onValueChange` shape; what a bidirectional binding costs;
  recognizing UDF violations in a code review.
- **Primary:** `compose_udf`
- **Supporting:** `unidirectional_data_flow` (architecture), `compose_state_hoisting`
- **Notes:** UDF as an architectural principle is owned by the `architecture` Topic —
  **Bridge** to it and point there for MVI, reducers, and general UDF. Hoisting mechanics
  are Unit 2; this lesson establishes the direction only.

---

## Unit 2 — State and State Ownership

**Purpose:** teach what Compose state is, how long it lives, and who should own it.
**Prerequisites:** Unit 1.

#### L2.1 — Observable State: `mutableStateOf` and `State<T>`

- **Objective:** distinguish ordinary mutation from mutation Compose can observe.
- **Core:** what "state" means for a UI; `State<T>` and `MutableState<T>`; `mutableStateOf`;
  reading `.value` (and the `by` delegate) is what makes a composable depend on that state.
- **Practical:** a plain `var` inside a composable that never updates the UI; the difference
  between "the value changed" and "the UI was told".
- **Primary:** `compose_state`
- **Supporting:** `compose_snapshot_system`, `kotlin_delegation` (kotlin_language)
- **Notes:** Snapshot machinery is **Bridge** here — one sentence that reads and writes are
  tracked, deferred to Unit 6. Property delegation is Kotlin's, explained only as far as
  `by` needs.

#### L2.2 — `remember`: Composition Memory

- **Objective:** separate "this value is observable" from "this value survives
  recomposition", which learners routinely conflate.
- **Core:** `remember` stores a value across recompositions at a composition slot;
  `remember` is not `mutableStateOf` and neither implies the other; `remember(key)`
  discards and recomputes when the key changes.
- **Practical:** the four combinations (`remember` alone, `mutableStateOf` alone, both,
  neither) and what each actually does; why `remember { mutableStateOf(x) }` is the common
  pair; keys that are wrong and the stale values that result.
- **Senior:** `remember` is tied to composition identity, so where it is called matters as
  much as what it stores — the hook into Unit 4.
- **Primary:** `compose_state`
- **Supporting:** `compose_identity_keys`, `compose_recomposition`
- **Notes:** **Teach** `remember(key)` conceptually here; memoization of expensive work is
  Unit 5.

#### L2.3 — `rememberSaveable` and State That Must Survive

- **Objective:** decide which state needs to survive what, and pick the right mechanism.
- **Core:** three lifetimes — recomposition, configuration change, process death; what
  `remember` survives and what it does not; `rememberSaveable` and its saver requirement.
- **Practical:** scroll position, expanded/collapsed, in-progress text input; the size and
  serializability limits of saved state; why "save everything" is the wrong instinct.
- **Senior:** where `rememberSaveable` sits relative to `ViewModel` and `SavedStateHandle`,
  and why they are not alternatives to each other.
- **Primary:** `compose_state`
- **Supporting:** `saved_state`, `configuration_changes`, `process_death`
  (lifecycle_navigation)
- **Notes:** **Bridge** to the lifecycle Topic. Explain process death only as far as
  "the process can be killed and recreated with the user's task intact". Do **not**
  reproduce the lifecycle and process-death curriculum here.

#### L2.4 — State Hoisting and the Lowest Sensible Owner

- **Objective:** decide where a given piece of state should live.
- **Core:** hoisting moves state to a caller; the `value` + `onValueChange` pattern;
  stateless vs. stateful composables; the lowest common owner of everything that reads or
  writes the state.
- **Practical:** hoisting a toggle for reuse and testability; over-hoisting so a whole
  screen recomposes for one checkbox; local UI state that should stay local; plain state
  holder classes as the step between a composable and a `ViewModel`.
- **Senior:** the framing that matters is not "Compose or ViewModel" but **who is the lowest
  sensible owner of this state?** — with the tests being who reads it, who writes it, and
  how long it must live.
- **Primary:** `compose_state_hoisting`, `compose_udf`
- **Supporting:** `state_ownership` (architecture)
- **Notes:** The `architecture` Topic owns state ownership as a general principle —
  **Bridge**. `ViewModel`-owned screen state is introduced by name here and taught in
  Unit 8.

#### L2.5 — Collections and Observable Mutation

- **Objective:** explain why ordinary mutable collections break in Compose and what to use
  instead.
- **Core:** `mutableStateOf(mutableListOf())` does not notify on `add`; the snapshot system
  observes the `State` holder, not arbitrary object mutation; replace rather than mutate.
- **Practical:** immutable update patterns (`+`, `copy`, `toMutableList().also { }.toList()`);
  `mutableStateListOf` / `mutableStateMapOf` and when they earn their place; nested data
  classes where an inner mutation is invisible.
- **Senior:** Kotlin's `List` interface is read-only, not immutable, so a `List` parameter
  guarantees nothing about the underlying object — the bridge into Unit 4's stability
  material.
- **Primary:** `compose_state`
- **Supporting:** `kotlin_collections`, `kotlin_data_classes` (kotlin_language),
  `compose_stability`
- **Notes:** Kotlin collection semantics are **Bridge** — enough to make the Compose
  behavior explicable, then point at the Kotlin Topic.

---

## Unit 3 — Recomposition

**Purpose:** explain what actually happens when state changes, and defuse the belief that
recomposition is a defect.
**Prerequisites:** Units 1–2.

#### L3.1 — Composition and Recomposition

- **Objective:** distinguish the initial composition from recomposition and name what
  triggers each.
- **Core:** initial composition builds the tree; recomposition re-runs parts of it; a state
  write invalidates the composables that read that state; recomposition does not rebuild or
  redraw the whole screen.
- **Practical:** tracing a button click through state write → invalidation → re-execution of
  the reading composable; what does *not* re-execute.
- **Primary:** `compose_recomposition`
- **Supporting:** `compose_fundamentals`, `compose_state`

#### L3.2 — Recomposition Scopes and Selective Execution

- **Objective:** predict which composables re-execute for a given state change.
- **Core:** a recomposition scope is the restartable region that read the state; only
  invalidated scopes re-execute; where you read state determines what recomposes.
- **Practical:** reading a state value high in the tree and passing it down vs. reading it at
  the leaf; lambda parameters that keep a scope from being invalidated; how a single
  misplaced read widens the recomposing region.
- **Senior:** why the read location, not the write location, defines the scope — the
  reasoning deferred reads in Unit 13 build on.
- **Primary:** `compose_recomposition`
- **Supporting:** `compose_snapshot_system`, `compose_state`
- **Notes:** Skipping is named here and mechanized in Unit 4. **Exclude** slot-table
  internals.

#### L3.3 — Recomposition Is Not the Problem

- **Objective:** replace "avoid recomposition" with "avoid unnecessary expensive work".
- **Core:** recomposition is the normal operating mode of the framework; a frequently
  recomposing cheap composable is fine; the cost is work done *during* composition.
- **Practical:** the actual costs — allocation, sorting or filtering a list, parsing, image
  decoding, logging in a composable body; misconceptions worth naming directly ("every
  recomposition redraws the screen", "a recomposition count above zero is a bug").
- **Primary:** `compose_recomposition`
- **Supporting:** `compose_recomposition_performance` (performance)
- **Notes:** Deliberately a short lesson; it exists to prevent a misconception that
  otherwise distorts Units 4, 5 and 13. Measurement and tooling are **Bridge** to Unit 13.

---

## Unit 4 — Identity, Keys, Stability and Immutability

**Purpose:** explain how Compose decides "this is the same composable as before" and "these
inputs did not change".
**Prerequisites:** Units 1–3.

#### L4.1 — Composable Identity

- **Objective:** explain what gives a composable, and its remembered state, an identity.
- **Core:** identity comes from the call site and position in the composition, not from
  parameter values; remembered state is attached to that identity; when identity changes,
  remembered state is discarded.
- **Practical:** state that unexpectedly resets because a composable moved between branches
  of an `if`; two calls to the same composable holding independent `remember` slots.
- **Primary:** `compose_identity_keys`
- **Supporting:** `compose_recomposition`, `compose_state`

#### L4.2 — `key` and Keys in Lazy Lists

- **Objective:** give a composable a stable logical identity when position is not a reliable
  one.
- **Core:** `key(id) { }`; list items whose position changes across recompositions; a stable
  key ties remembered state to the item rather than the slot.
- **Practical:** reordering, insertion and deletion in a list; unkeyed items showing another
  item's expanded state or animation; index used as a key and why it fails exactly when it
  matters.
- **Primary:** `compose_identity_keys`
- **Supporting:** `compose_lazy_layouts`
- **Notes:** The lazy-list *application* is supporting here; `LazyColumn` as a whole is
  Unit 10, which cross-references this lesson.

#### L4.3 — Immutability in Kotlin vs. What Compose Needs

- **Objective:** stop equating `val` with immutability.
- **Core:** `val` fixes the reference, not the object; a `data class` holding a
  `MutableList` is deeply mutable; Kotlin's `List` is a read-only interface, not an
  immutability guarantee.
- **Practical:** an "immutable" UI state whose nested list is mutated in place and never
  triggers an update; modelling screen state so equality is meaningful.
- **Senior:** structural vs. referential equality, and why `equals` correctness is a
  prerequisite for anything Compose infers from "the value did not change".
- **Primary:** `compose_stability`
- **Supporting:** `kotlin_data_classes`, `kotlin_equality`, `kotlin_collections`,
  `kotlin_variables` (kotlin_language)
- **Notes:** Kotlin semantics are **Bridge**. `kotlin_variables` currently has no active
  questions by deliberate policy — it is referenced as vocabulary, not as practice coverage.

#### L4.4 — Stability and Skipping

- **Objective:** explain when Compose can skip a composable and what "stable" means.
- **Core:** skipping means not re-executing a composable whose inputs did not change;
  stability is the compiler's judgement about whether change can be detected reliably;
  stable vs. unstable parameter types.
- **Practical:** modern **Strong Skipping** behavior and what it changed — unstable
  parameters no longer force re-execution the way older guidance assumed; how to check the
  actual behavior instead of assuming it.
- **Senior:** the interaction between equality, stability and skipping, and why an incorrect
  `equals` produces a stale UI rather than a slow one.
- **Primary:** `compose_stability`
- **Supporting:** `compose_recomposition`, `compose_recomposition_performance` (performance)
- **Notes:** **Explicitly correct** the obsolete claim that passing a `List` or any unstable
  parameter always forces recomposition. Pre–Strong Skipping optimization folklore is
  **Exclude**. Compiler metrics and reports are **Reference**, in Unit 13.

#### L4.5 — `@Stable` and `@Immutable` as Contracts

- **Objective:** treat the annotations as promises the author must keep.
- **Core:** `@Immutable` promises the public properties never change after construction;
  `@Stable` promises changes are observable and `equals` is consistent; the compiler trusts
  the promise rather than verifying it.
- **Practical:** where an annotation is genuinely warranted; interface-typed parameters the
  compiler cannot analyze; annotating a type whose backing collection is mutated, and the
  stale UI that follows.
- **Senior:** these are correctness contracts, not performance switches — a false promise is
  a correctness bug that presents as a rendering bug, which is far harder to diagnose than
  a slow screen.
- **Primary:** `compose_stability`
- **Supporting:** `kotlin_equality` (kotlin_language)

---

## Unit 5 — Derived State and Expensive Work

**Purpose:** control when computation happens, without cargo-culting optimization.
**Prerequisites:** Units 2–4.

#### L5.1 — `remember(key)` as Memoization

- **Objective:** cache a computed value correctly across recompositions.
- **Core:** `remember(key1, key2) { expensive() }`; the key list is the dependency list;
  recomputation happens exactly when a key changes.
- **Practical:** filtering or sorting a list in a composable body without memoization;
  under-specified keys producing stale results; over-specified keys defeating the cache.
- **Primary:** `compose_derived_state`
- **Supporting:** `compose_state`, `compose_recomposition`

#### L5.2 — `derivedStateOf`

- **Objective:** know the one situation `derivedStateOf` is actually for.
- **Core:** it exists when inputs change more frequently than the derived result does; the
  derived `State` only invalidates its readers when the *result* changes.
- **Practical:** the canonical threshold case — "show the scroll-to-top button when the
  first visible item index exceeds 0" — where the index changes constantly and the boolean
  rarely does; how much recomposition that removes.
- **Senior:** when `derivedStateOf` adds nothing (result changes as often as its inputs) and
  is pure overhead; how it differs from `remember(key)`, which recomputes on key change
  rather than filtering result changes.
- **Primary:** `compose_derived_state`
- **Supporting:** `compose_snapshot_system`, `compose_lazy_layouts`

#### L5.3 — Keeping Work Out of Composition

- **Objective:** put computation where it belongs instead of optimizing it in place.
- **Core:** composition is for describing UI; business and data computation belongs above
  the UI layer; a composable body is not a safe place for anything expensive or
  order-dependent.
- **Practical:** mapping domain models to display models in the `ViewModel` or a use case;
  the difference between "make this composable cheaper" and "this should not have been in
  composition at all"; not optimizing trivial work.
- **Primary:** `compose_derived_state`
- **Supporting:** `main_thread_performance` (performance), `layered_architecture`,
  `use_cases` (architecture)
- **Notes:** **Bridge** to `architecture` — the layering argument is stated, not taught.

---

## Unit 6 — Snapshot Fundamentals

**Purpose:** supply the mechanism that explains Units 2–5, and stop there.
**Prerequisites:** Units 2, 3, 5.

#### L6.1 — How Compose Observes State

- **Objective:** explain, at mental-model depth, why `mutableStateOf` triggers UI updates
  and an ordinary object does not.
- **Core:** the snapshot system tracks reads and writes of snapshot state; reading state
  inside a composable registers a dependency; writing it invalidates the dependent scopes;
  ordinary mutable objects participate in none of this.
- **Practical:** the whole-Unit-2 behavior re-explained in one consistent model; why the
  answer to "why didn't my UI update?" is almost always "nothing observable was written".
- **Senior:** snapshots give a consistent view of state, which is what makes safe reads
  from a non-UI context possible.
- **Primary:** `compose_snapshot_system`
- **Supporting:** `compose_state`, `compose_recomposition`
- **Notes:** Intentionally bounded. Snapshot MVCC implementation, slot-table layout, and
  apply/merge internals are **Exclude** — deep internals are **Reference** at most.

#### L6.2 — `snapshotFlow` and Crossing Into Flow

- **Objective:** convert observable Compose state into a stream for non-UI consumers.
- **Core:** `snapshotFlow { }` produces a cold `Flow` from snapshot state reads; it emits on
  change and conflates.
- **Practical:** reacting to scroll position, logging analytics on a state transition,
  driving a suspend call from a UI value; where it must be collected from.
- **Primary:** `compose_snapshot_system`
- **Supporting:** `flow_fundamentals`, `hot_vs_cold_streams` (async_reactive),
  `compose_side_effects`
- **Notes:** **Bridge** to the Flow curriculum — cold-stream semantics get one paragraph,
  then a pointer. Collection inside `LaunchedEffect` is taught in Unit 7; this lesson
  precedes it deliberately so the effect lesson has a concrete use.

---

## Unit 7 — Effects and Composable Lifecycle

**Purpose:** teach side effects problem-first, per Rule 5 of the authoring contract.
**Prerequisites:** Units 1–3, 6.

#### L7.1 — Why Side Effects Need Controlled APIs

- **Objective:** explain why a composable body cannot start work directly.
- **Core:** composables may run repeatedly, be skipped, run in any order, and be abandoned;
  work started in the body is therefore started an unpredictable number of times and never
  cleaned up; a composition enters, may recompose many times, and eventually leaves.
- **Practical:** a network call in a composable body firing on every recomposition; the four
  problems the effect APIs solve, stated before any API is named — lifetime-bound suspend
  work, observer registration with cleanup, event-driven launching, and reading the latest
  value inside a long-running effect.
- **Primary:** `compose_side_effects`
- **Supporting:** `compose_fundamentals`, `compose_recomposition`
- **Notes:** The problem-first anchor for the whole Unit. No API is the subject here.

#### L7.2 — `LaunchedEffect` and Effect Keys

- **Objective:** run suspend work tied to composition and key lifetime.
- **Core:** `LaunchedEffect(key)` launches a coroutine when it enters composition, cancels it
  when it leaves, and restarts it when a key changes; the coroutine is scoped to the
  composition.
- **Practical:** loading on first composition with `Unit`; restarting on an id change;
  `LaunchedEffect(true)` used as an unclear "run once"; passing an unstable lambda or a new
  object as a key and restarting on every recomposition; missing a key and never reloading.
- **Senior:** the cancellation guarantee and what it means for in-flight work; why an
  effect's key list is a dependency declaration in the same sense as `remember(key)`.
- **Primary:** `compose_side_effects`
- **Supporting:** `coroutine_fundamentals`, `coroutine_cancellation`,
  `structured_concurrency` (async_reactive)
- **Notes:** **Bridge** to coroutines: cancellation is explained as far as "leaving
  composition cancels the coroutine", then pointed at the coroutines Topic. Do **not**
  reproduce the coroutine curriculum.

#### L7.3 — `rememberCoroutineScope`: Launching From Events

- **Objective:** start work from a callback rather than from composition.
- **Core:** `rememberCoroutineScope()` returns a scope bound to the call site's composition;
  launch from an event handler, not from the composable body.
- **Practical:** scrolling a list on click, showing a snackbar, animating on a gesture; the
  decision rule — composition-driven work is `LaunchedEffect`, event-driven work is the
  remembered scope.
- **Senior:** why this scope is the wrong place for work that must outlive the screen, and
  what belongs in the `ViewModel` scope instead.
- **Primary:** `compose_side_effects`
- **Supporting:** `coroutine_scope`, `coroutine_builders` (async_reactive),
  `viewmodel_lifecycle` (lifecycle_navigation)

#### L7.4 — `DisposableEffect` and Cleanup

- **Objective:** register something external and guarantee it is released.
- **Core:** `DisposableEffect(key) { ... onDispose { ... } }`; the cleanup runs when the
  effect leaves composition or a key changes; every registration needs a matching removal.
- **Practical:** listeners, broadcast receivers, `LifecycleObserver`, sensor callbacks,
  third-party SDK handles; the leak that results from a missing `onDispose`.
- **Senior:** why cleanup is keyed the same way as setup, and what an asymmetric key list
  breaks.
- **Primary:** `compose_side_effects`
- **Supporting:** `lifecycle_aware_apis` (lifecycle_navigation), `lifecycle_leaks`
  (performance)
- **Notes:** Leak diagnosis is **Bridge** to the performance Topic.

#### L7.5 — `rememberUpdatedState` and `SideEffect`

- **Objective:** handle the two remaining cases the earlier APIs do not cover.
- **Core:** `rememberUpdatedState` keeps a long-running effect reading the latest lambda or
  value without restarting it; `SideEffect` publishes composed state to a non-Compose
  object after every successful composition.
- **Practical:** a timeout effect that must call the current `onTimeout` rather than the one
  captured at launch; the alternative — keying the effect on the lambda — and why it
  restarts the work; `SideEffect` used to update an analytics or legacy object.
- **Primary:** `compose_side_effects`
- **Supporting:** `compose_state`, `compose_identity_keys`
- **Notes:** Deliberately last in the Unit; these only make sense once keys and restarts
  from L7.2 are understood.

---

## Unit 8 — ViewModel, Flow and Production Screen State

**Purpose:** connect Compose to a real application. **This Unit is explicitly
bridge-heavy** — most of its supporting concepts are owned by `lifecycle_navigation`,
`async_reactive`, and `architecture`, and it is the blueprint's clearest example of
legitimate cross-Topic learning coverage.
**Prerequisites:** Units 1–3, 7.

#### L8.1 — The Production Screen Pipeline

- **Objective:** describe the standard path from data to pixels and justify each hop.
- **Core:** Repository → ViewModel → `UiState` exposed as `StateFlow` → Compose renders it →
  events go back up to the `ViewModel`; each layer's single responsibility.
- **Practical:** a realistic screen wired end to end; what belongs in each layer; symptoms
  of collapsing two layers into one.
- **Senior:** why the `ViewModel` boundary exists at all — survival across configuration
  change, testability without the UI toolkit, and a single source of truth for screen state.
- **Primary:** `compose_udf`
- **Supporting:** `repository_pattern`, `mvvm`, `layered_architecture`,
  `single_source_of_truth` (architecture), `viewmodel_lifecycle` (lifecycle_navigation)
- **Notes:** **Bridge** to `architecture` — the pattern is applied, not taught. MVVM vs. MVI
  is a pointer, not a section.

#### L8.2 — Collecting Flow and StateFlow in Compose

- **Objective:** get a stream into a composable correctly.
- **Core:** `collectAsState` / `collectAsStateWithLifecycle`; a `StateFlow` always has a
  current value; collection is itself an effect with a lifetime.
- **Practical:** why lifecycle-aware collection matters on Android — a plain
  `collectAsState` keeps collecting while the screen is in the background, wasting work and
  potentially holding upstream resources; the platform-neutral option in shared
  Compose Multiplatform code, where the Android lifecycle-aware variant is not available on
  every target.
- **Senior:** upstream sharing policy (`stateIn` with a started policy) as the actual
  control over whether the producer stops, and why the collection site alone does not
  decide it.
- **Primary:** `compose_state`
- **Supporting:** `stateflow`, `flow_collection`, `flow_sharing`, `lifecycle_coroutines`
  (async_reactive), `kmp_lifecycle_viewmodel`, `compose_multiplatform` (kmp)
- **Notes:** **Bridge** to the Flow curriculum for sharing strategies, operators, buffering
  and cancellation. Explain `stateIn` only as much as the lifecycle argument requires.

#### L8.3 — Modelling `UiState`

- **Objective:** design a screen state type that makes impossible states impossible.
- **Core:** an immutable `UiState` holding everything the screen renders; loading, content
  and error as modelled states rather than loose booleans.
- **Practical:** sealed hierarchy vs. a single data class with nullable fields, and when each
  is right; partial states (content plus a refresh indicator) that a naive sealed hierarchy
  cannot express; error representation the UI can actually render.
- **Senior:** why immutability of `UiState` is what makes skipping and equality-based
  updates work — the payoff from Unit 4.
- **Primary:** `compose_state`
- **Supporting:** `error_modeling`, `single_source_of_truth` (architecture),
  `kotlin_sealed_types`, `kotlin_data_classes` (kotlin_language)

#### L8.4 — State vs. Events, and What Belongs Where

- **Objective:** decide what goes in the `ViewModel` and what stays in the composable.
- **Core:** state is re-rendered whenever it is read; an event must be consumed exactly once;
  the two need different representations.
- **Practical:** the one-off navigation or snackbar delivered as state and fired twice after
  a configuration change; local UI state (a dropdown's expanded flag, a text field's focus)
  that has no business in a `ViewModel`; screen and business state that must not live in
  composition.
- **Senior:** the trade-offs between a consumable event channel and modelling the event as
  state with an explicit acknowledgement, and how `SavedStateHandle` interacts with each.
- **Primary:** `compose_udf`, `compose_state_hoisting`
- **Supporting:** `state_ownership`, `mvi` (architecture), `sharedflow` (async_reactive),
  `saved_state` (lifecycle_navigation)
- **Notes:** Closes the ownership question opened in L2.4 with the production answer.

---

## Unit 9 — Modifiers and Layout

**Purpose:** teach how Compose sizes and positions things.
**Prerequisites:** Unit 1. Independent of Units 2–8.

#### L9.1 — What a Modifier Is and Why Order Matters

- **Objective:** stop treating modifiers as a bag of unordered attributes.
- **Core:** a `Modifier` is an ordered, immutable chain of decorations applied outside-in;
  each element wraps the next; order changes behavior, not just appearance.
- **Practical:** `padding().background()` vs. `background().padding()`;
  `clickable().padding()` vs. `padding().clickable()` and the resulting touch target; size
  modifiers interacting with padding.
- **Primary:** `compose_layouts_modifiers`
- **Supporting:** `compose_fundamentals`

#### L9.2 — Designing Composable APIs With Modifiers

- **Objective:** write reusable composables that behave the way callers expect.
- **Core:** accept a `modifier: Modifier = Modifier` parameter, place it first, and apply it
  to the outermost layout node; a composable decorates itself only after the caller's
  modifier.
- **Practical:** a composable that ignores the caller's modifier and cannot be positioned;
  applying the modifier to the wrong internal node; slot APIs and content lambdas as the
  other half of a reusable component's surface.
- **Primary:** `compose_layouts_modifiers`
- **Supporting:** `compose_state_hoisting`

#### L9.3 — The Layout Contract

- **Objective:** explain the single-pass measurement model.
- **Core:** constraints go down, sizes come up, parents place children; each child is
  measured once; a parent cannot measure a child twice in the normal path.
- **Practical:** reading a real constraint failure — a child that "won't fill" because the
  parent passed a bounded constraint; `fillMaxSize` inside a scrollable container.
- **Senior:** why this contract exists — it is what removes the multi-pass measurement cost
  of nested View hierarchies, and what makes intrinsics an explicit opt-in.
- **Primary:** `compose_layouts_modifiers`
- **Supporting:** `view_rendering` (android_ui, Views)
- **Notes:** **Bridge** to classic View measure/layout/draw for the contrast; the View
  rendering pipeline itself belongs to the Views half of `android_ui`.

#### L9.4 — `Row`, `Column`, `Box` and `weight`

- **Objective:** compose the standard layouts predictably.
- **Core:** `Row` and `Column` main/cross axis, arrangement and alignment; `Box` stacking;
  `weight` distributing remaining space.
- **Practical:** a weighted row that overflows because a child ignores its constraints;
  alignment vs. arrangement confusion; nesting scrollables and the errors it produces.
- **Reference:** window insets and edge-to-edge — where system bars, IME padding and the
  inset modifiers fit. Kept at concise reference depth: it is real interview material but
  is API-shaped and platform-version-sensitive. See
  [Taxonomy gaps](#taxonomy-gaps-concepts-with-no-exact-assessment-subtopic).
- **Primary:** `compose_layouts_modifiers`

#### L9.5 — Custom `Layout` and Intrinsics

- **Objective:** know that the escape hatch exists and when it is justified.
- **Core:** the `Layout` composable — measure children, decide a size, place them; intrinsic
  measurements as an explicit second look at a child.
- **Practical:** the rare cases that genuinely need a custom layout; why a custom layout is
  usually the wrong first answer.
- **Senior:** the cost of intrinsics relative to the single-pass model, and why they are
  opt-in rather than the default.
- **Primary:** `compose_layouts_modifiers`
- **Supporting:** `view_rendering`, `custom_views` (android_ui, Views)
- **Notes:** **Reference** depth. Highly specialized custom measurement and
  `Modifier.Node` authoring are **Exclude** from the main path.

---

## Unit 10 — Lazy Layouts

**Purpose:** teach lists, which is where identity, keys and derived state pay off.
**Prerequisites:** Units 4, 5, 9.

#### L10.1 — Why Lazy Composition Exists

- **Objective:** explain the model rather than the API.
- **Core:** lazy layouts compose only what is visible (plus a small buffer); items are
  composed and discarded as they scroll; `LazyColumn` is not a `Column` inside a scroll
  modifier.
- **Practical:** a `Column` with `verticalScroll` over a thousand items and what it costs;
  the `LazyListScope` DSL — `item`, `items`, `itemsIndexed` — as a description of content,
  not a loop over composables.
- **Senior:** how this compares to `RecyclerView` recycling — the same problem, a different
  solution, and why there is no explicit ViewHolder.
- **Primary:** `compose_lazy_layouts`
- **Supporting:** `recyclerview`, `recyclerview_performance` (android_ui / performance)
- **Notes:** **Bridge** to `RecyclerView` for the comparison only.

#### L10.2 — Item Identity, Keys and `contentType`

- **Objective:** keep item state correct across list mutations.
- **Core:** stable `key` per item; state and animations follow the key rather than the
  index; `contentType` tells the runtime which items can share composition structure.
- **Practical:** an expanded item that jumps to a neighbour after a delete; a checkbox that
  resets on reorder; heterogeneous lists (headers, ads, content) and what `contentType`
  saves.
- **Primary:** `compose_lazy_layouts`
- **Supporting:** `compose_identity_keys`
- **Notes:** Direct application of L4.2 — cross-reference rather than re-teach.

#### L10.3 — `LazyListState` and Observing Scroll

- **Objective:** react to scroll without recomposing the world.
- **Core:** `rememberLazyListState()`; `firstVisibleItemIndex` and friends are snapshot state
  that changes constantly; scroll actions are suspend functions.
- **Practical:** the scroll-to-top button implemented naively and then with `derivedStateOf`;
  `snapshotFlow` over scroll position for analytics or paging triggers; scrolling
  programmatically from a remembered scope.
- **Primary:** `compose_lazy_layouts`
- **Supporting:** `compose_derived_state`, `compose_snapshot_system`, `compose_side_effects`
- **Notes:** Paging is **Exclude** from this Unit — a paging library course is its own
  subject and does not belong inside Compose fundamentals.

---

## Unit 11 — CompositionLocal, Theme and Ambient Dependencies

**Purpose:** explain implicit tree-scoped values and their cost.
**Prerequisites:** Units 1–3.

#### L11.1 — `CompositionLocal`

- **Objective:** decide when an implicit tree-scoped value is justified.
- **Core:** `CompositionLocal` provides a value to a whole subtree without threading it
  through every parameter; `compositionLocalOf` vs. `staticCompositionLocalOf` and the
  difference in invalidation behavior; `CompositionLocalProvider` defines the scope.
- **Practical:** legitimate uses — theme, density, layout direction, platform context;
  illegitimate ones — passing screen data or a repository down the tree.
- **Senior:** the hidden-dependency trade-off. A `CompositionLocal` makes a composable's
  real inputs invisible at the call site, which harms testability, previewability and
  reuse. **Do not use it as a service locator** — dependency injection is a construction
  concern, not a composition concern.
- **Primary:** `composition_local`
- **Supporting:** `service_locator_vs_di`, `di_fundamentals` (dependency_injection)
- **Notes:** **Bridge** to the DI Topic for the service-locator argument.

#### L11.2 — Theme and Design Tokens

- **Objective:** use and extend a theme without memorizing a component catalogue.
- **Core:** `MaterialTheme` as colour, typography and shape delivered through
  `CompositionLocal`; a theme is a set of tokens, not a stylesheet.
- **Practical:** reading theme values instead of hard-coding; extending the theme with custom
  tokens for values Material does not model; light and dark theming at practical depth,
  including dynamic colour as a decision rather than a default.
- **Primary:** `compose_theming`
- **Supporting:** `composition_local`, `android_resources` (android_platform)
- **Notes:** `compose_theming` has **no active questions** and is listed as deliberately
  empty in `docs/content/question-bank-coverage.md`. It is still worth **teaching** — a
  learner needs it to build anything — but the blueprint does not propose changing that
  question policy, and this Lesson should not be judged by question coverage. Detailed
  Material component and theming API catalogues are **Reference** at most; memorizing the
  component set is **Exclude**.

---

## Unit 12 — Accessibility and Semantics

**Purpose:** teach accessibility as behavior, which also explains how UI tests see the tree.
**Prerequisites:** Units 1, 9.

#### L12.1 — The Semantics Tree

- **Objective:** explain the parallel tree Compose exposes to accessibility services.
- **Core:** composables emit semantics alongside layout; the semantics tree describes meaning,
  not pixels; semantic hierarchy and visual hierarchy are related but not identical.
- **Practical:** inspecting what a screen actually exposes; a visually obvious grouping that
  the semantics tree does not express.
- **Primary:** `compose_accessibility`
- **Supporting:** `compose_fundamentals`

#### L12.2 — Accessible Behavior, Not Just Labels

- **Objective:** move past `contentDescription` as the whole of accessibility.
- **Core:** `contentDescription` for meaningful images and null for decorative ones; role,
  state and action semantics; `mergeDescendants` and when merging helps or destroys
  information.
- **Practical:** a custom clickable `Row` that a screen reader cannot announce or activate;
  a toggle that never announces its state; touch target sizing; why "add a content
  description" is not a fix for a component that is not operable.
- **Senior:** semantics as a public contract of a component, and why an accessible component
  is usually a better-designed component.
- **Primary:** `compose_accessibility`
- **Supporting:** `view_events` (android_ui, Views)
- **Notes:** Exhaustive focus and input API surfaces are **Reference**.

#### L12.3 — Semantics and Compose UI Testing

- **Objective:** connect semantics to how tests find and assert on nodes.
- **Core:** the Compose testing APIs query the semantics tree; a node that is inaccessible is
  usually also untestable; test tags as an explicit, last-resort semantic handle.
- **Practical:** a test that cannot find a node because semantics are merged; preferring
  meaningful matchers over test tags.
- **Primary:** `compose_accessibility`
- **Supporting:** `compose_ui_testing`, `ui_testing` (testing)
- **Notes:** **Bridge** to the testing Topic. Synchronization, idling, test rules and the
  rest of the UI testing curriculum stay there — this Lesson only establishes that
  semantics are the shared substrate.

---

## Unit 13 — Compose Performance Mental Model

**Purpose:** synthesis. Nothing here is new machinery; it is the machinery of Units 3–10
organized around cost. Its primary mappings deliberately reach into the `performance`
Topic, which is the intended cross-Topic behavior, not a taxonomy error.
**Prerequisites:** Units 3, 4, 5, 9, 10.

#### L13.1 — The Three Phases

- **Objective:** locate cost in the right phase.
- **Core:** composition, layout, draw; a state change does not necessarily invalidate all
  three; different work belongs to each.
- **Practical:** a frame budget framing — what "jank" actually is; identifying which phase a
  given change invalidates.
- **Primary:** `compose_performance` (performance)
- **Supporting:** `compose_recomposition`, `compose_layouts_modifiers`, `rendering_jank`,
  `main_thread_performance` (performance)
- **Notes:** `compose_performance` currently has **no active questions** and is ranked Tier 1
  in `docs/content/question-bank-coverage.md`. The blueprint records this; it does not
  change it.

#### L13.2 — Deferred Reads and Where You Read State

- **Objective:** apply the read-location rule from L3.2 as a performance technique.
- **Core:** reading state in a lambda passed to a phase-specific modifier defers the read to
  that phase; the classic contrast is `Modifier.offset(x)` invalidating composition versus
  `Modifier.offset { }` invalidating only layout.
- **Practical:** an animated or scroll-driven value read at the top of a screen and the
  recomposition it causes; moving the read down and measuring the difference.
- **Senior:** why this is the same principle as recomposition scope, applied one layer down —
  and why it is a targeted technique rather than a habit to apply everywhere.
- **Primary:** `compose_recomposition_performance` (performance)
- **Supporting:** `compose_layouts_modifiers`, `compose_derived_state`, `compose_recomposition`

#### L13.3 — Putting the Tools Together

- **Objective:** choose the right tool for an observed cost.
- **Core:** a decision path — expensive work in composition → move it out or `remember(key)`;
  result changes less often than inputs → `derivedStateOf`; unnecessary re-execution →
  stability and skipping; long lists → lazy content and keys; frequently changing values →
  deferred reads.
- **Practical:** animation and per-phase considerations at bounded depth — why animating a
  value that is read during composition is the expensive version of the same animation.
- **Senior:** why stability annotations are the last resort rather than the first, and how
  Strong Skipping changed the default advice.
- **Primary:** `compose_performance`, `compose_recomposition_performance` (performance)
- **Supporting:** `compose_stability`, `compose_derived_state`, `compose_lazy_layouts`,
  `compose_recomposition`

#### L13.4 — Measure Instead of Guessing

- **Objective:** insist on evidence before optimization.
- **Core:** measure before and after; **debug-build behavior is not performance evidence** —
  debug builds are unoptimized, run without R8, and can be dramatically slower than release
  in ways that mislead entirely.
- **Practical:** release builds with R8 as the baseline; Baseline Profiles; Macrobenchmark
  and what it measures; recomposition counts and layout inspection as diagnostics rather
  than scores.
- **Senior:** why a recomposition count is a symptom rather than a metric, and what a real
  regression looks like when it is stated in frame terms.
- **Primary:** `compose_performance` (performance)
- **Supporting:** `macrobenchmark`, `baseline_profiles`, `r8`, `layout_inspector`,
  `cpu_profiling` (performance)
- **Notes:** **Bridge** only. The profiling, jank and startup curriculum belongs to the
  `performance` Topic and must not be duplicated here; this Lesson teaches the discipline,
  not the tools.

---

## Unit 14 — Views and Compose Interoperability

**Purpose:** teach the migration boundary honestly, including its long-term cost.
**Prerequisites:** Units 1, 2, 7. Familiarity with the Views half of `android_ui` helps but
is not assumed.

#### L14.1 — Compose Inside Views: `ComposeView`

- **Objective:** add Compose to an existing View-based screen correctly.
- **Core:** `ComposeView` as a `View` hosting a composition; the composition must be disposed
  in step with its host; `ViewCompositionStrategy` selects that policy.
- **Practical:** `ComposeView` in an XML layout or created in code; the Fragment case, where
  the default strategy is the common source of leaks and crashes; why the Fragment *view*
  lifecycle, not the Fragment lifecycle, is the relevant one.
- **Primary:** `views_compose_interop`
- **Supporting:** `fragment_lifecycle`, `activity_lifecycle` (lifecycle_navigation),
  `view_binding` (android_ui, Views)
- **Notes:** **Bridge** to lifecycle for the Fragment view-lifecycle distinction.

#### L14.2 — Views Inside Compose: `AndroidView`

- **Objective:** embed a `View` in a composition without duplicating state.
- **Core:** `AndroidView(factory, update)` — `factory` runs once, `update` runs on
  recomposition when its reads change; `AndroidViewBinding` for existing XML.
- **Practical:** `MapView`, `WebView`, `SurfaceView` and vendor SDK views; the state-ownership
  question at the boundary — Compose state driving the View, or the View owning its own; the
  double-state bug where both do; releasing View resources with `onRelease`/`DisposableEffect`.
- **Senior:** why the boundary costs measurement and invalidation, and why nesting scrollable
  Views inside scrollable Compose is a known trouble spot.
- **Primary:** `views_compose_interop`
- **Supporting:** `views_fundamentals`, `view_rendering` (android_ui, Views)

#### L14.3 — Migration Strategy and the Cost of Permanent Bridges

- **Objective:** reason about interop as an engineering decision, not just an API.
- **Core:** incremental migration — screen by screen, or leaf component first; interop exists
  to make migration possible without a rewrite.
- **Practical:** choosing a migration seam; keeping the shared state model consistent across
  the boundary; the theming and design-system duplication a long migration incurs.
- **Senior:** when a permanent bridge stops being a migration tool and becomes technical debt
  — two design systems, two state models, two testing approaches, and a boundary nobody
  owns. This is the interview-relevant judgement.
- **Primary:** `views_compose_interop`
- **Supporting:** `architecture_tradeoffs` (architecture), `xml_layouts` (android_ui, Views)
- **Notes:** `architecture_tradeoffs` currently has no active questions (deprecated-only) and
  `xml_layouts` is deliberately empty — both are used here as vocabulary, not as claimed
  practice coverage.

---

## Reference Material

Concise treatment is worthwhile, but none of this belongs on the main learning path and
none of it is a prerequisite for anything above.

| Area | Why Reference rather than Teach | Where it sits |
| --- | --- | --- |
| Compose Previews (`compose_previews`) | Tooling; useful daily, almost no interview signal | Short aside; the Subtopic is deliberately question-empty |
| Material component catalogue | Large, changes often, looked up rather than memorized | Mentioned in L11.2 |
| Animation API catalogue | Real subject, but API-shaped; the per-phase cost argument is the interview-relevant part | Cost angle in L13.3; catalogue deferred |
| `Canvas` and custom drawing | Specialized; matters for graphics-heavy roles only | Not planned as a Lesson |
| Advanced gestures and pointer input | Deep API surface; the common cases are covered by `clickable` and friends | Touch target sizing in L12.2 |
| `Modifier.Node` and custom modifiers | Framework-author territory | Named in L9.5 |
| Highly specialized custom measurement | Rare in product work | Bounded in L9.5 |
| Exhaustive focus and input APIs | Large surface, low frequency | Named in L12.2 |
| Window insets and edge-to-edge | Genuinely useful, but API-shaped and version-sensitive | Reference note in L9.4 |
| Deep snapshot internals | Explains nothing the mental model in L6.1 does not already deliver | Bounded in Unit 6 |

## Excluded Material

Accurate, but it does not improve interview readiness and would add noise. Each exclusion is
revisitable if the target job profile changes — by editing this blueprint, not by quietly
adding a lesson.

| Area | Why excluded |
| --- | --- |
| Memorizing the Material component set | Recall, not reasoning; looked up in seconds |
| Exhaustive `Modifier` API memorization | An API catalogue; the *order and contract* concepts in Unit 9 are what carry interview signal |
| Compose compiler-generated function signatures | Implementation detail of the compiler plugin; no product decision depends on it |
| Internal runtime class-name trivia | Recall of names, not understanding of behavior |
| `SlotTable` implementation detail | The observable consequences are taught in Units 3–4; the data structure adds nothing |
| Exhaustive snapshot MVCC implementation | Same reasoning; the mental model in L6.1 is the useful part |
| Pre–Strong Skipping optimization folklore | Actively wrong now — L4.4 corrects it explicitly rather than repeating it |
| Unmeasured micro-optimization tricks | Contradicts L13.4; advice without measurement is superstition |
| Paging library internals | Its own subject; would unbalance Unit 10 |
| Compose-specific navigation APIs | Navigation is owned by `lifecycle_navigation`; a Compose lesson would fragment it |

## Taxonomy Gaps: Concepts With No Exact Assessment Subtopic

These are useful Compose learning concepts for which the current assessment taxonomy has no
exact Subtopic. They are recorded rather than given invented IDs. **E20-01 does not change
the question taxonomy**; whether any of these should become a Subtopic is a separate
decision for a future question-bank change.

| Concept | Nearest existing Subtopic | Note |
| --- | --- | --- |
| Window insets / edge-to-edge | `compose_layouts_modifiers` | No insets Subtopic exists in any Topic. Taught at Reference depth in L9.4. |
| Compose animation | none | No animation Subtopic in any Topic. Only the per-phase cost angle is planned (L13.3). |
| Compose gestures / pointer input | `view_events` (Views-only) | The Views event Subtopic is not a Compose equivalent. |
| Compose `Canvas` / custom drawing | `custom_views` (Views-only) | `custom_views` is explicitly the View-based concept. |
| Compose text and typography | `compose_theming` | Typography tokens are reachable via theming; text layout and field behavior are not. |
| Focus management | none | No focus Subtopic. Referenced only. |
| Custom `Modifier.Node` authoring | `compose_layouts_modifiers` | Excluded from the main path anyway. |
| Compose-specific navigation | `navigation_fundamentals`, `navigation_2_vs_3` (lifecycle_navigation) | Both exist but are not Compose-scoped; navigation stays with its own Topic. |
| Compose runtime and compiler internals | none | Deliberately excluded, so no gap needs filling. |

Separately, four Subtopics this blueprint maps have **no active questions**, which affects
what "Practice this material" can offer for the corresponding Lessons:

| Subtopic | Status | Affected Lessons |
| --- | --- | --- |
| `compose_performance` | Empty; ranked Tier 1 "worth filling" | L13.1, L13.3, L13.4 |
| `compose_theming` | Empty; deliberately so | L11.2 |
| `compose_previews` | Empty; deliberately so | Reference only |
| `architecture_tradeoffs` | Deprecated-only; ranked Tier 1 | L14.3 |

## Authoritative Source Families

Identified so that a Lesson author does not start from a blank search. Individual Lessons
still cite the specific page supporting each claim, per Rule 9.

- **Android Developers — Jetpack Compose** guides: mental model, state, side effects,
  lifecycle, phases, performance, lists, semantics, theming, interop.
- **Compose API reference** (`androidx.compose.*`) for exact contracts, defaults, and
  parameter semantics.
- **Compose release notes and the AndroidX source** for behavior that changed — Strong
  Skipping in particular, where secondary sources are frequently out of date.
- **Kotlin documentation** for collections, delegation, equality, sealed types, coroutines
  and Flow when those appear as bridged supporting concepts.
- **Compose Multiplatform documentation** (JetBrains) for the platform-neutral half of
  Unit 8 and anything shared-source-set specific.
- **Android accessibility documentation** for Unit 12, which is broader than the Compose
  guides alone.
- **Android performance documentation** — Baseline Profiles, Macrobenchmark, R8 — for
  Unit 13.

Compose APIs and recommendations are on the question bank's freshness watch list. Re-check
sources on any material edit; guidance older than roughly two releases is suspect.

## Status

This blueprint is complete as a map. Unit 1 is authored and ships in
`learning_curriculum.json` as `unit_thinking_in_compose`; Units 2–14 are still plans. When
authoring reveals a wrong Lesson boundary, update this file in the same change.

Authoring Unit 1 kept all three Lesson boundaries and both concept mappings unchanged, and
required one accuracy correction: L1.2's Core line previously said a composable "may
execute in any order or in parallel", which reads as a claim about current runtime
behaviour that the Compose documentation contradicts. The line now describes ordering,
skipping, and discarded optimistic recomposition, and the Notes record why the parallel
claim must not be taught.
