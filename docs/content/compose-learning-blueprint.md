# Compose Learning Blueprint

## Purpose

This is the complete learning map for Jetpack Compose, produced under
`docs/content/learning-content-authoring.md` before any production Compose lesson is
authored. It is the first Topic mapped under that contract and doubles as the worked
example of what a blueprint looks like.

It is a **plan, not content**. No lesson text is authored here, and nothing in this file is
a runtime artifact. Authoring proceeds incrementally, Unit by Unit, against this map.

Home Topic: `android_ui` (UI — Views & Jetpack Compose).

Scope: 18 Learning Units, 63 planned Lessons, plus explicit Reference and Exclude
decisions and a record of concepts the current assessment taxonomy cannot express.

Units 7–12 were **renumbered and rewritten** by E25-01, which reconciled the former Units 7
and 8 against the shipped Units 1–6, the shipped Coroutines and Flow curriculum, and the
current epic boundaries; the former Units 9–14 are now 13–18. The reconciliation, the
evidence behind it, and the confirmed authoring plan for Units 7–12 are in
[`compose-units-7-12-plan.md`](compose-units-7-12-plan.md).

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
7. Production Screen State and Unidirectional Data Flow
8. Observable State Collection and Lifecycle
9. Effect Lifecycle and `LaunchedEffect`
10. Latest-Value Effects and Event-Driven Coroutine Work
11. Cleanup, External Synchronization and State Producers
12. Production UI Effects and Mechanism Selection
13. Modifiers and Layout
14. Lazy Layouts
15. CompositionLocal, Theme and Ambient Dependencies
16. Accessibility and Semantics
17. Compose Performance Mental Model
18. Views and Compose Interoperability

The order encodes conceptual dependencies, not convenience:

- **Stability (4) after recomposition (3).** Skipping cannot be explained before there is
  something to skip. Stability introduced first is just annotation folklore.
- **Derived state (5) after identity and stability (4).** `remember(key)` and
  `derivedStateOf` are answers to "this recomputes more often than it needs to", which
  requires knowing when and why recomposition happens.
- **Snapshots (6) after state (2), recomposition (3) and derived state (5).** The snapshot
  system explains *why* everything in Units 2–5 behaves as it does. Placed first it is
  abstract theory; placed here it is the unifying mechanism.
- **Screen state (7) after state ownership (2).** Unit 2 decides where one piece of state
  lives; Unit 7 composes a whole screen from several classes of state at once and names the
  owner that sits outside the Composition. Taught the other way round, a learner concludes
  that "state goes in the ViewModel", which is the wrong mental model.
- **Collection (8) after screen state (7), snapshots (6) and the Flow curriculum.**
  Converting a stream into Compose `State` is only meaningful once the learner knows what a
  Composition can observe and what a `StateFlow` already guarantees.
- **Effects (9) after execution semantics (1), recomposition (3) and identity (4).**
  `LaunchedEffect` only makes sense once the learner knows a composable body may run
  repeatedly, may be skipped and may be abandoned, and is not a one-time imperative
  lifecycle callback. Taught earlier, effects become memorized incantations. Effect keys
  additionally require `remember(key)` from Unit 5 and skipping's instance comparison from
  Unit 4, because the Lesson's whole point is the contrast between the two comparisons.
- **Latest values and event-driven work (10) after effects (9).** Both Lessons are defined
  against `LaunchedEffect`: one keeps its lifetime while changing what it reads, the other
  takes the trigger away from composition entirely.
- **Cleanup and producers (11) after 9 and 10.** Every API here is a variation on an
  ownership decision the earlier two Units established.
- **Mechanism selection (12) last of the six.** It is synthesis, and it is worthless before
  every mechanism it chooses between exists.
- **Layout (13) after the state model.** Modifiers and layout are largely independent of
  the state story, so they come after it rather than interleaved; deferred reads in
  Unit 17 then have both halves available.
- **Lazy layouts (14) after identity and keys (4)** and after layout (13). Lazy list keys
  are a direct application of composition identity.
- **Performance (17) last but one.** By that point every mechanism it depends on —
  recomposition, stability, derived state, layout phases, lazy content — has been taught,
  so performance is synthesis rather than a random list of optimization tricks.
- **Interop (18) last.** It is a migration concern that assumes both models are understood.

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
  View story is Unit 18 and the Views half of `android_ui`. Do **not** turn this into an
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
  reproduce the lifecycle and process-death curriculum here. The documented saved-state
  guarantee is Android's; Compose Multiplatform documents no equivalent for desktop, iOS or
  web, so teach the lifetime ladder — recomposition, composition removal, process
  recreation — and say plainly that host restoration beyond Android is not a documented
  guarantee.

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
- **Primary:** `compose_state_hoisting`
- **Supporting:** `compose_udf`, `state_ownership` (architecture)
- **Notes:** The `architecture` Topic owns state ownership as a general principle —
  **Bridge**. `ViewModel`-owned screen state is introduced by name here and taken up as a
  bounded boundary in Unit 7; the architecture curriculum owns the rest of it.
  `compose_udf` is **supporting**, not primary: L1.3 owns unidirectional data flow
  and this lesson uses the direction to reason about ownership rather than teaching it.
  Two lessons claiming the same primary concept would also make Unit 2 practice assess a
  concept Unit 1 is responsible for.

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
- **Notes:** Name composition, layout and drawing as distinct phases at **Bridge** depth —
  without that distinction "recomposition redraws the screen" cannot be refuted. Per-phase
  state reads and deferred reads stay in Unit 17.

#### L3.2 — Recomposition Scopes and Selective Execution

- **Objective:** predict which composables re-execute for a given state change.
- **Core:** a recomposition scope is the restartable region that read the state; only
  invalidated scopes re-execute; where you read state determines what recomposes.
- **Practical:** reading a state value high in the tree and passing it down vs. reading it at
  the leaf; lambda parameters that keep a scope from being invalidated; how a single
  misplaced read widens the recomposing region.
- **Senior:** why the read location, not the write location, defines the scope — the
  reasoning deferred reads in Unit 17 build on.
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
  otherwise distorts Units 4, 5 and 17. Measurement and tooling are **Bridge** to Unit 17.

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
  Unit 14, which cross-references this lesson.

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
  **Exclude**. Compiler metrics and reports are **Reference**, in Unit 17.

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
- **Notes:** a third key failure belongs beside the two above and was added during
  authoring: mutating an object used as a key invalidates nothing, because the stored key
  and the new key are the same object and the comparison is between an object and itself.
  The key comparison is `==`; it must be kept apart from Strong Skipping's `===` rule for
  unstable parameters, which Unit 4 teaches.

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
- **Notes:** the derived `State` object's lifetime and what its calculation *captures* were
  added to Senior during authoring, because a captured non-`State` input is frozen at
  creation and produces a wrong answer rather than a slow one. The runtime this repository
  resolves offers `derivedStateOf(calculation)` and `derivedStateOf(policy, calculation)`
  and nothing else; upstream guidance naming a further API is **Exclude** here, since that
  API is absent from the configured artifact.

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
  apply/merge internals are **Exclude** — deep internals are **Reference** at most. Two
  additions from authoring: the Core line's "reading state inside a composable registers a
  dependency" is generalised in the shipped Lesson to *any* observing context — an
  executing composable, a `derivedStateOf` calculation, a `snapshotFlow` block — because
  the composable-only framing makes L6.2 look like a special case rather than the same
  mechanism. And the Practical line's "the answer is almost always 'nothing observable was
  written'" is shipped with its limits attached: it diagnoses the missing-write family, and
  the Lesson says explicitly that identity, captured values, equality and skipping are
  separate failures taught in Units 4 and 5.

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
  then a pointer. Collection inside `LaunchedEffect` is taught in Unit 9; this lesson
  precedes it deliberately so the effect lesson has a concrete use. While Unit 9 is
  unauthored, that pointer names the subject in prose and cites external documentation:
  `relatedLessonIds` cannot reference a Lesson that does not exist. The Flow
  curriculum now has a map — `docs/content/coroutines-flow-learning-blueprint.md`, delivered
  by E24-01 — and its `lesson_cold_flows` and `lesson_flow_collection_lifetime` are the
  canonical treatment of the four facts this Lesson bridges. The shipped Lesson's sentence
  saying the app "does not teach yet" must be corrected, and the reciprocal
  `relatedLessonIds` added, by the issue that ships that Unit; see
  `docs/content/coroutines-flow-units-1-6-plan.md`. Authoring added one
  concrete failure the blueprint did not plan: a block returning a `SnapshotStateList`
  itself records no element read *and* produces a value equal to the previous one, so it
  emits once and never again. The runtime's own `SnapshotStateList.toList()` documentation
  recommends `toList()` for exactly this case, and the shipped Lesson teaches it.

---

## Unit 7 — Production Screen State and Unidirectional Data Flow

**Purpose:** decide who owns each piece of state on a complete production screen, before any
effect API exists. Units 2 and 4 answered the question one piece of state at a time; this Unit
answers it for a screen that carries several classes of state at once and has an owner outside
the Composition.
**Prerequisites:** Units 1–4. Confirmed authoring plan:
[`compose-units-7-12-plan.md`](compose-units-7-12-plan.md).

#### L7.1 — Three Classes of State on One Screen

- **Objective:** place every piece of state on a realistic screen, and justify each placement.
- **Core:** local UI-element state, state hoisted into a subtree, and screen-level state owned
  outside the Composition, compared on one screen; the three ownership tests restated in one
  sentence and applied rather than re-derived.
- **Practical:** at least one piece of state that correctly stays local; the screen whose local
  state has all been pushed to the top, and the parameter list and state type that result; why
  "all production state belongs in a state holder" is wrong.
- **Senior:** lifetime and ownership as separate axes when three classes coexist.
- **Primary:** `compose_state_hoisting`
- **Supporting:** `compose_state`, `state_ownership` (architecture), `viewmodel_lifecycle`
  (lifecycle_navigation)
- **Notes:** **Must not** re-derive L2.4. The three "at least" rules, the overload pair and the
  over-hoisting argument are taught there and are linked, not repeated.

#### L7.2 — The Stateless Content Boundary

- **Objective:** split a screen into a stateful screen composable and a stateless content
  composable, and justify the split by what it makes possible.
- **Core:** the two-composable screen shape; the content composable receives a state value and
  callbacks and owns nothing.
- **Practical:** what the boundary buys — reuse, preview, one place where wiring lives; what a
  content composable that reaches for its own owner loses.
- **Primary:** `compose_udf`
- **Supporting:** `compose_state_hoisting`, `compose_previews`, `compose_fundamentals`
- **Notes:** `compose_previews` is deliberately question-empty and is used here as vocabulary,
  not as claimed practice coverage. Compose UI testing is **Exclude** — the testing curriculum
  owns it.

#### L7.3 — One Screen State Value, Events Back Up

- **Objective:** model what the screen renders as one immutable current value and what the user
  does as callbacks describing intent.
- **Core:** one state value per screen; callbacks as intent rather than as assignment.
- **Practical:** a content composable handed a `MutableState` or a holder reference, and the
  specific problems that creates — a second write path, no preview, no reuse; the rewritten
  signature.
- **Senior:** why immutability of the state value is what makes equality-based skipping work —
  the payoff from Unit 4.
- **Primary:** `compose_udf`
- **Supporting:** `compose_state`, `unidirectional_data_flow` (architecture),
  `kotlin_data_classes` (kotlin_language), `compose_stability`
- **Notes:** **Exclude** `UiState` modelling strategy — sealed hierarchy against nullable
  fields, partial states, error representation. That is the architecture curriculum's, and it is
  the material the former Unit 8 wrongly claimed.

#### L7.4 — The Screen-Level Owner as a Bounded Bridge

- **Objective:** describe the boundary between the Composition and the thing that owns screen
  state, and know where the curriculum hands over.
- **Core:** what "outside the Composition" buys — surviving recomposition, surviving the
  composable leaving composition, and surviving UI recreation; the composition talks to it
  through a state value down and callbacks up.
- **Practical:** that a plain remembered state holder is often the right answer and a
  screen-level owner is not a default; what changes about a piece of state when its owner moves
  out, and what does not.
- **Primary:** `compose_state_hoisting`
- **Supporting:** `state_ownership` (architecture), `viewmodel_lifecycle`,
  `configuration_changes` (lifecycle_navigation), `kmp_lifecycle_viewmodel` (kmp)
- **Notes:** **Bridge only.** MVVM, MVI, layering, repositories, use cases, `UiState` design and
  dependency injection are **Exclude** here and belong to the architecture and
  dependency-injection curricula. This Lesson keeps the promise that L2.3 and L2.4 currently
  make to a "ViewModel unit"; both of those sentences are corrected when this Unit ships.

---

## Unit 8 — Observable State Collection and Lifecycle

**Purpose:** turn the observable streams the coroutines and Flow curriculum teaches into
something a Composition can render, and be honest about what the collection costs.
**Prerequisites:** Units 2, 3, 6, 7, and the coroutines and Flow Units 4 and 6.

#### L8.1 — What a Composable Can and Cannot Observe

- **Objective:** explain why a `Flow` or `StateFlow` cannot drive a composable directly.
- **Core:** a Composition observes snapshot-state reads and nothing else; a stream is not
  snapshot state; conversion is therefore a real operation with a contract.
- **Practical:** `someStateFlow.value` read in a composable body, and what the screen does when
  the flow changes.
- **Primary:** `compose_state`
- **Supporting:** `stateflow`, `hot_vs_cold_streams` (async_reactive), `compose_snapshot_system`
- **Notes:** Problem-first anchor; no API is named until L8.2.

#### L8.2 — `collectAsState`: Converting a Stream into Compose State

- **Objective:** use the state-conversion API correctly and know exactly what it does.
- **Core:** both overloads; a `StateFlow`'s current value as the initial value; the mandatory
  `initial` for a plain `Flow`; a collected value arriving is an ordinary state write.
- **Practical:** the conversion is itself keyed on the flow instance, so a flow constructed
  fresh on every recomposition restarts collection; why manual collection inside a launched
  effect is **not** the default way to render ongoing state, and what would make it right.
- **Primary:** `compose_state`
- **Supporting:** `flow_collection`, `stateflow` (async_reactive), `compose_recomposition`
- **Notes:** **Bridge** to the Flow curriculum for operators, `flowOn` and buffering. The
  distinction between converting state and performing an effect returns in Unit 12.

#### L8.3 — Collection Has a Lifetime and a Cost

- **Objective:** reason about how long a collection runs and what that costs.
- **Core:** the collection's lifetime is the call site's composition.
- **Practical:** what "still collecting" costs — an active collector, an active upstream, work
  applied to a UI nobody is looking at.
- **Senior:** stopping the UI collector does not by itself stop upstream production; the
  upstream's owner and sharing policy decide that.
- **Primary:** `compose_state`
- **Supporting:** `flow_collection`, `flow_sharing`, `coroutine_cancellation`,
  `lifecycle_coroutines` (async_reactive)
- **Notes:** **Bridge** to `stateIn`/`shareIn` in exactly one paragraph, then a link. Sharing
  policies and their timeouts are **not** retaught.

#### L8.4 — Lifecycle-Aware Collection and the Lifecycle a Screen Actually Has

- **Objective:** decide between plain and lifecycle-aware collection from the problem, and know
  what "lifecycle" means on each target this project builds.
- **Core:** the problem lifecycle-aware collection solves; `collectAsStateWithLifecycle` and its
  `minActiveState` default of `STARTED`; that it is built from a state producer and
  `repeatOnLifecycle` and inherits both contracts.
- **Practical:** what actually moves the lifecycle on Android, desktop, iOS and web, and the
  user action that stops collection on each; the platform limits the multiplatform lifecycle
  documents.
- **Primary:** `compose_state`
- **Supporting:** `lifecycle_aware_apis` (lifecycle_navigation), `flow_sharing`
  (async_reactive), `kmp_lifecycle_viewmodel`, `compose_multiplatform` (kmp)
- **Notes:** The lifecycle-aware API **is** available to this project's `commonMain` on every
  target it builds, and is already used there; the evidence is recorded in
  [`compose-units-7-12-plan.md`](compose-units-7-12-plan.md). An earlier version of this
  blueprint said the opposite, and that claim was wrong. Android-only guidance must not be
  presented as multiplatform behaviour, and a dependency in `commonMain` must not be presented
  as identical behaviour everywhere. Fragment collection, lifecycle architecture and `LiveData`
  are **Exclude** beyond one comparison clause.

---

## Unit 9 — Effect Lifecycle and `LaunchedEffect`

**Purpose:** teach effects problem-first, per Rule 5 of the authoring contract, and give effect
keys the treatment they need.
**Prerequisites:** Units 1, 3, 4, 5, and the coroutines and Flow Units 1 and 3.

#### L9.1 — Why Compose Needs an Effect API

- **Objective:** state the ownership problem the effect family solves.
- **Core:** a composable body is a description with no lifetime of its own, so work started
  from it has no answer to how often it starts, who stops it, or what cleans it up.
- **Practical:** the four problems the family solves, named before any API — lifetime-bound
  suspend work, registration with cleanup, event-driven launching, and a current value inside
  long-lived work.
- **Primary:** `compose_side_effects`
- **Supporting:** `compose_fundamentals`, `compose_recomposition`
- **Notes:** L1.2 already teaches the execution contract and the lines that should make a reader
  look twice. This Lesson **applies** it and must not re-derive it.

#### L9.2 — `LaunchedEffect`: Work a Composition Owns

- **Objective:** describe the coroutine lifetime `LaunchedEffect` creates.
- **Core:** launched on entering composition, cancelled on leaving, cancelled and relaunched
  when a key changes; the coroutine runs in the composition's context.
- **Practical:** one effect traced through entering, recomposing, a key change and leaving, with
  what happens to in-flight work at each point; the effect's identity is its call site's, so a
  call site that stops being composed takes its effect with it.
- **Senior:** what the cancellation guarantee does and does not promise about in-flight work.
- **Primary:** `compose_side_effects`
- **Supporting:** `coroutine_scope`, `coroutine_cancellation`, `structured_concurrency`
  (async_reactive), `compose_identity_keys`
- **Notes:** **Bridge** to the coroutines curriculum: cancellation is cooperative, in one
  sentence and a link. Do **not** say cancellation preempts arbitrary code. Keys are named here
  and explained in L9.3.

#### L9.3 — What an Effect's Keys Declare

- **Objective:** read a key list as a claim about what the effect's lifetime depends on.
- **Core:** the key list is a dependency declaration with lifetime meaning, not a list of every
  value the effect reads; the decision is whether a change should *end the current work*.
- **Practical:** the exact comparison — effect keys are compared the way a remembered
  calculation's keys are — and the contrast with the instance comparison that decides skipping;
  a constant key read as the degenerate claim "for this call site's whole composition lifetime",
  which is sometimes right and is often an imperative "run once" in disguise.
- **Primary:** `compose_side_effects`
- **Supporting:** `compose_derived_state`, `compose_stability`, `kotlin_equality`
  (kotlin_language)
- **Notes:** L5.1 and L4.4 already teach both comparisons. This Lesson links to them and adds
  only the effect consequence.

#### L9.4 — Two Ways to Get Effect Keys Wrong

- **Objective:** diagnose both failure directions from a symptom.
- **Core:** too few keys — a stale id still in use, old work continuing, an effect that never
  restarts when its lifetime dependency changed.
- **Practical:** keys that needlessly stop comparing equal — a freshly allocated value or lambda
  supplied on every pass — and the valid in-flight work that is discarded; that a new object is
  **not** automatically a changed key, with the cases that surprise people.
- **Primary:** `compose_side_effects`
- **Supporting:** `compose_identity_keys`, `coroutine_cancellation` (async_reactive),
  `kotlin_lambdas` (kotlin_language)

---

## Unit 10 — Latest-Value Effects and Event-Driven Coroutine Work

**Purpose:** separate two problems that look alike — an effect whose lifetime should not change
but whose value must, and work that composition should not start at all.
**Prerequisites:** Unit 9, and the coroutines and Flow Unit 1.

#### L10.1 — Reading the Current Value Without Restarting

- **Objective:** give a long-lived effect access to a current value without ending it.
- **Core:** `rememberUpdatedState` is a remembered state holder reassigned on every
  recomposition; the effect reads the current value at the moment it reads it.
- **Practical:** the stale-callback failure; the alternative of keying the effect on the
  changing value, and the restart it costs; that it is **not** a way to prevent or reduce
  recomposition, and not a performance optimisation.
- **Primary:** `compose_side_effects`
- **Supporting:** `compose_state`, `compose_recomposition`, `kotlin_lambdas` (kotlin_language)

#### L10.2 — `rememberCoroutineScope`: Launching From an Event

- **Objective:** launch suspend work from a callback, with a lifetime the composition owns.
- **Core:** it returns a scope and launches nothing; the caller decides when work starts; the
  scope is cancelled when the call leaves composition.
- **Practical:** scrolling, showing a snackbar, animating on a gesture; where the scope's
  context comes from.
- **Primary:** `compose_side_effects`
- **Supporting:** `coroutine_scope`, `coroutine_builders`, `structured_concurrency`
  (async_reactive)

#### L10.3 — Composition-Driven or Event-Driven?

- **Objective:** choose between the two from who owns the trigger, not from API names.
- **Core:** composition state owns the trigger, or the event does.
- **Practical:** one screen written both ways — a snackbar or scroll driven by composition state
  against the same behaviour driven by the click — and what the wrong one does on a
  recomposition or configuration change.
- **Senior:** work that must continue after the composition is gone belongs to neither
  mechanism; name the symptom and stop.
- **Primary:** `compose_side_effects`
- **Supporting:** `coroutine_scope`, `coroutine_cancellation`, `lifecycle_coroutines`
  (async_reactive), `viewmodel_lifecycle` (lifecycle_navigation)
- **Notes:** **Exclude** `viewModelScope` as a taught mechanism, WorkManager and background
  work. The longer-lived owner is the architecture and background-work curricula's.

---

## Unit 11 — Cleanup, External Synchronization and State Producers

**Purpose:** own the external world a screen touches, organised by problem rather than by API.
**Prerequisites:** Units 9, 10, and the coroutines and Flow Unit 4.

#### L11.1 — Registration and Release as One Decision

- **Objective:** treat registration and release as one symmetric decision the composition owns.
- **Core:** `DisposableEffect(key) { ... onDispose { ... } }`; setup on entering, disposal on
  leaving **and** on a key change; one disposal per setup.
- **Practical:** listeners, observers, lifecycle observers, sensor callbacks and third-party
  handles; what accumulates when the release is missing.
- **Senior:** why setup and cleanup necessarily share a key set, and what an asymmetric one
  breaks.
- **Primary:** `compose_side_effects`
- **Supporting:** `lifecycle_aware_apis` (lifecycle_navigation), `memory_leaks`,
  `lifecycle_leaks` (performance)
- **Notes:** Distinguished by *what it is for*, not described as a launched effect with cleanup
  added. Leak diagnosis is **Bridge** to the performance Topic.

#### L11.2 — `SideEffect`: Publishing to Non-Compose Code

- **Objective:** publish a successfully composed value to something Compose does not manage.
- **Core:** it runs after every successful composition that reached it.
- **Practical:** updating an analytics or legacy object; why writing the same call in the body
  is a different operation, because it can publish a value from a composition that was never
  applied.
- **Primary:** `compose_side_effects`
- **Supporting:** `compose_recomposition`, `compose_fundamentals`, `compose_state`
- **Notes:** **Exclude** presenting it as a general event handler, a coroutine API, or a place
  for expensive work.

#### L11.3 — `produceState`: a Composition-Scoped Producer

- **Objective:** adapt an asynchronous or external source into Compose `State`.
- **Core:** an initial value, a producer launched on entering and cancelled on leaving, and
  restart keys.
- **Practical:** the returned state conflates, so an equal write is not a change; what the state
  holds immediately after a key change but before the new producer has written.
- **Senior:** its relationship to a launched effect writing remembered state, which is what it
  is built from — as confirmation of the contract, not as the mental model.
- **Primary:** `compose_side_effects`
- **Supporting:** `compose_state`, `flow_collection`, `coroutine_cancellation` (async_reactive)

#### L11.4 — A Flow Below the UI, or a Producer at the Boundary?

- **Objective:** decide where an adapter for a callback-based source belongs.
- **Core:** `awaitDispose` and the one case it is for — a non-suspending subscription inside a
  producer.
- **Practical:** the decision itself, answered from ownership and reuse: a reusable or
  non-Compose-consumed source becomes a Flow below the UI; a source whose lifetime is
  specifically a composition's can stay at the boundary.
- **Primary:** `compose_side_effects`
- **Supporting:** `flow_fundamentals`, `hot_vs_cold_streams` (async_reactive),
  `separation_of_concerns` (architecture)
- **Notes:** **Bridge** to `callbackFlow`/`awaitClose`, named once with a link. This is not
  another Flow-builder Lesson.

---

## Unit 12 — Production UI Effects and Mechanism Selection

**Purpose:** synthesis. No new API family; the Unit exists because engineers who know each API
individually still choose the wrong one under production pressure.
**Prerequisites:** Units 7–11, and the coroutines and Flow Unit 6.

#### L12.1 — Choosing the Smallest Sufficient Mechanism

- **Objective:** decide, from a stated requirement, which mechanism a screen needs.
- **Core:** the four facts that decide it — who owns the state, who owns the trigger, what
  lifetime is required, what cleanup is required.
- **Practical:** realistic screen scenarios in which every outcome is reachable: plain
  rendering, local state, hoisted state, collected state, lifecycle-aware collected state, a
  composition-triggered coroutine, an event-triggered coroutine, a long-lived effect reading a
  current value, a registration with cleanup, post-composition publication, an external source
  adapted into state, and "none of these — this work has the wrong owner".
- **Primary:** `compose_side_effects`
- **Supporting:** `compose_state`, `compose_state_hoisting`, `compose_udf`
- **Notes:** Scenarios, not a table of API definitions.

#### L12.2 — Rendering State and Running a Transient Effect

- **Objective:** separate what the screen *is* from something that should happen once.
- **Core:** rendering current state and executing a transient effect are different operations.
- **Practical:** snackbars, a one-time focus request, scrolling, animation and a navigation
  request; which effect or scope lifetime executes each; the one-off modelled as state that
  fires again after recreation.
- **Primary:** `compose_side_effects`
- **Supporting:** `coroutine_scope`, `sharedflow` (async_reactive), `compose_state`
- **Notes:** Navigation is used as one occurrence among several; navigation APIs remain owned by
  `lifecycle_navigation` and are **Exclude**.

#### L12.3 — What Delivery Guarantee Does This Occurrence Need?

- **Objective:** ask the delivery question before choosing a mechanism.
- **Core:** what must be true when the UI is absent; a successful emission is not a delivery.
- **Practical:** why "one stream type is for state and another for events" is not the answer,
  and why replacing it with a different stream type is the same mistake; when the requirement
  needs persistence, acknowledgement or queueing, and has therefore left the Compose boundary.
- **Primary:** `compose_side_effects`
- **Supporting:** `stateflow`, `sharedflow`, `hot_vs_cold_streams` (async_reactive),
  `state_ownership` (architecture)
- **Notes:** The coroutines and Flow Unit 6 already establishes the delivery argument; this
  Lesson applies it at the Compose boundary and links rather than re-deriving. Application-level
  event architecture — durable against transient modelling, `Channel` against `SharedFlow` as a
  design, acknowledgement and queueing — is **Exclude** and belongs to the architecture
  curriculum.

---

## Unit 13 — Modifiers and Layout

**Purpose:** teach how Compose sizes and positions things.
**Prerequisites:** Unit 1. Independent of Units 2–12.

#### L13.1 — What a Modifier Is and Why Order Matters

- **Objective:** stop treating modifiers as a bag of unordered attributes.
- **Core:** a `Modifier` is an ordered, immutable chain of decorations applied outside-in;
  each element wraps the next; order changes behavior, not just appearance.
- **Practical:** `padding().background()` vs. `background().padding()`;
  `clickable().padding()` vs. `padding().clickable()` and the resulting touch target; size
  modifiers interacting with padding.
- **Primary:** `compose_layouts_modifiers`
- **Supporting:** `compose_fundamentals`

#### L13.2 — Designing Composable APIs With Modifiers

- **Objective:** write reusable composables that behave the way callers expect.
- **Core:** accept a `modifier: Modifier = Modifier` parameter, place it first, and apply it
  to the outermost layout node; a composable decorates itself only after the caller's
  modifier.
- **Practical:** a composable that ignores the caller's modifier and cannot be positioned;
  applying the modifier to the wrong internal node; slot APIs and content lambdas as the
  other half of a reusable component's surface.
- **Primary:** `compose_layouts_modifiers`
- **Supporting:** `compose_state_hoisting`

#### L13.3 — The Layout Contract

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

#### L13.4 — `Row`, `Column`, `Box` and `weight`

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

#### L13.5 — Custom `Layout` and Intrinsics

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

## Unit 14 — Lazy Layouts

**Purpose:** teach lists, which is where identity, keys and derived state pay off.
**Prerequisites:** Units 4, 5, 13.

#### L14.1 — Why Lazy Composition Exists

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

#### L14.2 — Item Identity, Keys and `contentType`

- **Objective:** keep item state correct across list mutations.
- **Core:** stable `key` per item; state and animations follow the key rather than the
  index; `contentType` tells the runtime which items can share composition structure.
- **Practical:** an expanded item that jumps to a neighbour after a delete; a checkbox that
  resets on reorder; heterogeneous lists (headers, ads, content) and what `contentType`
  saves.
- **Primary:** `compose_lazy_layouts`
- **Supporting:** `compose_identity_keys`
- **Notes:** Direct application of L4.2 — cross-reference rather than re-teach.

#### L14.3 — `LazyListState` and Observing Scroll

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

## Unit 15 — CompositionLocal, Theme and Ambient Dependencies

**Purpose:** explain implicit tree-scoped values and their cost.
**Prerequisites:** Units 1–3.

#### L15.1 — `CompositionLocal`

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

#### L15.2 — Theme and Design Tokens

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

## Unit 16 — Accessibility and Semantics

**Purpose:** teach accessibility as behavior, which also explains how UI tests see the tree.
**Prerequisites:** Units 1, 13.

#### L16.1 — The Semantics Tree

- **Objective:** explain the parallel tree Compose exposes to accessibility services.
- **Core:** composables emit semantics alongside layout; the semantics tree describes meaning,
  not pixels; semantic hierarchy and visual hierarchy are related but not identical.
- **Practical:** inspecting what a screen actually exposes; a visually obvious grouping that
  the semantics tree does not express.
- **Primary:** `compose_accessibility`
- **Supporting:** `compose_fundamentals`

#### L16.2 — Accessible Behavior, Not Just Labels

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

#### L16.3 — Semantics and Compose UI Testing

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

## Unit 17 — Compose Performance Mental Model

**Purpose:** synthesis. Nothing here is new machinery; it is the machinery of Units 3–6 and 13–14
organized around cost. Its primary mappings deliberately reach into the `performance`
Topic, which is the intended cross-Topic behavior, not a taxonomy error.
**Prerequisites:** Units 3, 4, 5, 13, 14.

#### L17.1 — The Three Phases

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

#### L17.2 — Deferred Reads and Where You Read State

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

#### L17.3 — Putting the Tools Together

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

#### L17.4 — Measure Instead of Guessing

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

## Unit 18 — Views and Compose Interoperability

**Purpose:** teach the migration boundary honestly, including its long-term cost.
**Prerequisites:** Units 1, 2, 9. Familiarity with the Views half of `android_ui` helps but
is not assumed.

#### L18.1 — Compose Inside Views: `ComposeView`

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

#### L18.2 — Views Inside Compose: `AndroidView`

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

#### L18.3 — Migration Strategy and the Cost of Permanent Bridges

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
| Exhaustive `Modifier` API memorization | An API catalogue; the *order and contract* concepts in Unit 13 are what carry interview signal |
| Compose compiler-generated function signatures | Implementation detail of the compiler plugin; no product decision depends on it |
| Internal runtime class-name trivia | Recall of names, not understanding of behavior |
| `SlotTable` implementation detail | The observable consequences are taught in Units 3–4; the data structure adds nothing |
| Exhaustive snapshot MVCC implementation | Same reasoning; the mental model in L6.1 is the useful part |
| Pre–Strong Skipping optimization folklore | Actively wrong now — L4.4 corrects it explicitly rather than repeating it |
| Unmeasured micro-optimization tricks | Contradicts L13.4; advice without measurement is superstition |
| Paging library internals | Its own subject; would unbalance Unit 14 |
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
| Converting an external observable stream into Compose `State` | `compose_state` | The nearest fit and the one Unit 8 uses, but every active `compose_state` Question assesses Unit 2's material, so Unit 8's practice currently assesses none of what it teaches. Recorded by E25-01. |
| Effect lifetime and effect-key semantics | `compose_side_effects` | Distinct reasoning from the API inventory the Subtopic name suggests. Recorded by E25-01. |
| Composition-scoped state producers (`produceState`, `awaitDispose`) | `compose_side_effects` | Same Subtopic, different mental model. Recorded by E25-01. |
| Mechanism selection across the effect family | `compose_side_effects` | A synthesis concept with no Subtopic of its own in any Topic. Recorded by E25-01. |
| Compose-side handling of a transient UI occurrence | `compose_side_effects`; `state_ownership` and `sharedflow` adjacent | The delivery half is assessed in `async_reactive` and `architecture`; the Compose-side half has no home. Recorded by E25-01. |

Because Units 9–12 all take `compose_side_effects` as their only primary concept, and Unit
practice is resolved from primary concepts, **all four Units receive an identical practice
pool**. That is a consequence of the four gaps above rather than a defect in the Units, and
it is analysed in
[`compose-units-7-12-plan.md`](compose-units-7-12-plan.md#the-compose_side_effects-overlap).

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
- **Compose Multiplatform and multiplatform lifecycle documentation** (JetBrains), together
  with the resolved `androidx.lifecycle` sources, for Units 7–12 and anything
  shared-source-set specific. Android-only documentation does not settle a multiplatform
  claim, and a dependency declared in `commonMain` does not settle a behavioural one; see
  [`compose-units-7-12-plan.md`](compose-units-7-12-plan.md).
- **Android accessibility documentation** for Unit 16, which is broader than the Compose
  guides alone.
- **Android performance documentation** — Baseline Profiles, Macrobenchmark, R8 — for
  Unit 17.

Compose APIs and recommendations are on the question bank's freshness watch list. Re-check
sources on any material edit; guidance older than roughly two releases is suspect.

## Status

This blueprint is complete as a map. Units 1–6 are authored and ship in
`learning_curriculum.json` as `unit_thinking_in_compose`,
`unit_state_and_state_ownership`, `unit_recomposition`,
`unit_identity_keys_and_stability`, `unit_derived_state_and_expensive_work` and
`unit_snapshot_fundamentals`; Units 7–18 are still plans. When authoring reveals a wrong
Lesson boundary, update this file in the same change.

Units 7–12 have a confirmed authoring plan in
[`compose-units-7-12-plan.md`](compose-units-7-12-plan.md), delivered by E25-01. It records
their Unit and Lesson identities, objectives, prerequisites and boundaries, the semantic
review of the Questions their primary Subtopics reach, the assessment gaps for E25-08, and
the version-sensitive and platform-sensitive claims their Lessons depend on.

**E25-01 reconciled the former Units 7 and 8 rather than appending to them.** What changed,
and why:

- **The former Unit 7 (Effects and Composable Lifecycle, five Lessons) became Units 9, 10 and
  11.** Its `LaunchedEffect` Lesson carried three mental models at once — composition-owned
  coroutine lifetime, what a key list claims, and the two opposite ways of getting a key list
  wrong — and became three Lessons. Its `rememberUpdatedState`/`SideEffect` Lesson was split:
  pairing them was an API-inventory decision rather than a conceptual one. `produceState`,
  `awaitDispose` and the Flow-adapter-or-Compose-producer decision are new; the former Unit
  named none of them on the main path.
- **The former Unit 8 (ViewModel, Flow and Production Screen State, four Lessons) was
  dissolved.** Its collection Lesson became the whole of Unit 8. The bounded screen-state half
  of its pipeline and its state-versus-events Lesson became Unit 7 and Unit 12. Its layering,
  MVVM and `UiState`-modelling material — the production screen pipeline as architecture,
  sealed-against-nullable state design, application-level event modelling, and the consumable
  event channel against acknowledged state — **moved to the architecture curriculum**, which
  is where the current roadmap places it.
- **One factual correction.** The former L8.2 said the lifecycle-aware collection variant "is
  not available on every target". That is false for this repository: the API is declared in
  `commonMain` of the resolved lifecycle artifact, is already used in this project's
  `commonMain`, and compiles on every target the shared module builds. The corrected Unit 8
  L8.4 states what is available, what supplies the `LifecycleOwner` per target, and where the
  behaviour genuinely differs.
- **Two shipped Lessons still promise a "ViewModel unit".** `lesson_state_hoisting` and
  `lesson_remember_saveable` each point at a Unit that no longer exists under that name. The
  issue that ships Unit 7 owns both edits.
- **Numbering.** Six Units replace two, so the former Units 9–14 are now 13–18, and the
  planned totals are 18 Units and 63 Lessons. Shipped Units 1–6 are untouched in number,
  identity and responsibility. No production identity encodes a Unit number: Unit and Lesson
  ids are semantic and unnumbered, and learner progress is keyed by Lesson id.

Units 2–6 have a confirmed authoring plan in
[`compose-units-2-6-plan.md`](compose-units-2-6-plan.md), which records their proposed Unit
and Lesson identities, prerequisites and boundaries, the semantic review of the Questions on
their primary Subtopics, and the version-sensitive claims their Lessons depend on. Four
corrections from that review are already applied above: `compose_udf` demoted to supporting
in L2.4, the phases Note on L3.1, the platform caveat on L2.3, and the pointer constraint on
L6.2.

Authoring Unit 1 kept all three Lesson boundaries and both concept mappings unchanged, and
required one accuracy correction: L1.2's Core line previously said a composable "may
execute in any order or in parallel", which reads as a claim about current runtime
behaviour that the Compose documentation contradicts. The line now describes ordering,
skipping, and discarded optimistic recomposition, and the Notes record why the parallel
claim must not be taught.

Authoring Units 2 and 3 kept every planned Lesson boundary, identity and concept mapping
unchanged; no blueprint correction was required. Four findings are worth recording because
they affect later Units:

- **L2.3's lifetime ladder gained a rung.** The Core line above says "three lifetimes —
  recomposition, configuration change, process death". Authoring found that too coarse: a
  composable leaving the Composition is a fourth, distinct event, and `rememberSaveable`
  does not survive it on its own because it unregisters its value provider as it leaves.
  The shipped Lesson teaches four rungs — re-execution, leaving the Composition, UI
  recreation, process recreation. The Core line here is left as the blueprint's shorthand
  rather than rewritten, but Unit 7 — the screen-state Unit, renumbered by E25-01 — should
  not inherit the three-rung framing.
- **Unit 2's saved-state platform caveat is carried as a Note, not a claim.** L2.3 states
  the Android guarantee precisely and says plainly that Compose Multiplatform documents no
  equivalent for desktop, iOS or web, per the plan's instruction. The unresolved question
  recorded there is unchanged by this authoring.
- **L3.2's inline caveat is now taught, and it was verified rather than assumed.**
  `Column`, `Row` and `Box` are declared `inline` in the Compose Multiplatform 1.11.1
  sources this repository resolves, so their content lambdas are compiled into the calling
  composable and form no recomposition scope of their own. The Lesson teaches this
  explicitly, because "wrap it in a `Column` to narrow recomposition" is a plausible and
  wrong conclusion to draw from the rest of the Lesson.
- **Unit 3 states no execution counts.** Executions can be coalesced, cancelled and
  restarted, or skipped, so the Lessons reason about which scopes a change *can* invalidate
  and deliberately promise no number. Unit 4 should keep that discipline when it introduces
  skipping properly.

Authoring Unit 4 kept every planned Lesson boundary, identity, title and concept mapping
unchanged, and required no blueprint correction. Three findings matter to Units 5–6 and to
E23-07:

- **The identity examples were run, not reasoned about.** L4.1's claim that two `if`
  branches calling the same composable are two identities, and its counter-example that one
  call site whose modifier changes keeps its state, were both measured by giving each
  composable a remembered token and watching whether the token changed. So were L4.2's
  keyed and unkeyed reorder outcomes. The blueprint's L4.1 Practical line — "state that
  unexpectedly resets because a composable moved between branches of an `if`" — is accurate
  as written, but the Lesson deliberately teaches the counter-example beside it, because
  "conditionals destroy state" is the wrong generalisation to leave a learner holding.
- **The documented Strong Skipping comparison rule did not reproduce for collections.**
  Under this repository's toolchain a composable taking a `List` parameter was *not*
  re-executed when its caller supplied a fresh but structurally equal list, although the
  documentation specifies instance comparison for unstable parameters and states that
  collections are always unstable. L4.4 teaches the documented rule as the guarantee and
  records the observation separately. See the plan's authoring outcomes for the evidence;
  this is the clearest case so far of configured behaviour running ahead of the
  documentation, and Units 5 and 6 should expect more of it rather than fewer.
- **Unit 4 states no execution counts**, as Unit 3 asked. Its predictions are framed as
  conditions — the same composition identity, the call actually reached, and no independent
  invalidation of the child — rather than as numbers.

Authoring Unit 5 kept every planned Lesson boundary, identity, title and concept mapping
unchanged. Two blueprint Notes were added above rather than any line being rewritten, and
three findings matter to Unit 6 and to E23-07:

- **The unit's claims were measured, not argued.** A throwaway `runComposeUiTest` probe on
  the JVM target counted calculations and composable executions separately for each case
  the Lessons teach: a `derivedStateOf` whose result changes less often than its input
  (calculation five times, reading scope twice), a trivial one whose result changes as
  often as its input (no consumer executions removed at all), the three key failures, and
  the stale captured input. The probe was deleted; the numbers are recorded in the plan's
  authoring outcomes.
- **`derivedStateOf` filters consumers, not calculations — and it is pulled, not pushed.**
  The measurement makes the first distinction concrete, because "`derivedStateOf` avoids
  recomputation" is the plausible and wrong summary. Review then corrected the second: a
  dependency write *invalidates* the derived value and the calculation re-runs when it is
  next needed, so coalesced writes produce one recalculation and an unread derived state
  produces none. Unit 6 inherits both: the snapshot lesson explains *why* the consumer is
  spared, must not re-teach the decision rule, and must not state any "happens on every
  write" rule that a batching schedule would disprove.
- **Unit 5 states no benchmark results.** Its cost reasoning is explanatory — where work
  sits and how often it repeats — and the execution counts it does report are labelled as
  measurements of this project's toolchain rather than as performance claims. Unit 6 should
  keep that separation.

Authoring Unit 6 kept both planned Lesson boundaries, identities, titles and concept
mappings unchanged. Two blueprint Notes were extended above rather than any line being
rewritten, and four findings matter to the effects Units — 9, 10 and 11 after E25-01's
renumbering — and to E23-07:

- **The Unit's behavioural claims were measured against the resolved runtime.** Two
  throwaway probes on the JVM target — one `runComposeUiTest` probe for L6.1 and one
  `runTest` probe for L6.2 — checked every claim the Lessons make about what is observed
  and what is emitted, rather than reading them off a documentation page. Both probes were
  deleted; the numbers and the conditions they were taken under are recorded in the plan's
  authoring outcomes.
- **`snapshotFlow` was verified against `androidx.compose.runtime:runtime:1.11.2`, not
  against upstream.** The resolved artifact's `SnapshotFlow.kt` and the current
  `androidx-main` file agree word for word on every claim the Lesson makes, which is worth
  recording because Unit 5 found the opposite for `derivedStateOf`. Units 9–11 should keep
  checking rather than assuming the agreement holds.
- **An equal write is not a write at all.** Assigning a `mutableStateOf` the value it
  already holds skips the write entirely — it produced no composable execution, no
  `snapshotFlow` evaluation and, when attempted inside a read-only snapshot, not even the
  `IllegalStateException` an unequal write produces there. Both Unit 6 Lessons lean on
  this, and it is the cleanest available demonstration that a write and a change are
  different things.
- **Snapshot consistency is a per-version read guarantee, not application-level
  atomicity.** Review caught the draft claiming that a block reading several related values
  inside one snapshot "cannot see half an update". It can: two ordinary assignments to two
  holders are two separate changes, and an observer running between them sees a torn pair,
  which was measured. L6.1 now states the limit and points at the in-scope fix — one state
  object for values that form one invariant. Units 9–11 will meet the same boundary from the
  effects side and should not inherit the stronger claim.
- **Unit 6 states evaluation and emission counts, and states their conditions with them.**
  Unit 3 asked for no counts and Unit 5 relaxed that to counts labelled as measurements of
  this project's toolchain. L6.2 needs numbers, because "evaluations, writes and emissions
  are not one-to-one" is not believable as an assertion; every number it reports names the
  collection readiness and settling schedule it was taken under, and the Lesson says
  plainly that the numbers describe an observation schedule rather than the API.
