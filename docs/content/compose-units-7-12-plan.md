# Compose Units 7–12 Authoring and Assessment Plan

## Purpose

`docs/content/compose-learning-blueprint.md` maps the whole Jetpack Compose subject. This
document is the confirmed authoring plan for the six Units that epic **E25 — Compose
Effects and Production Screen State** delivers — **6 Units, 22 Lessons** — reviewed against
the taxonomy, the shipped Compose Units (E23), the shipped Coroutines and Flow Units (E24),
the current ACTIVE question bank, this repository's configured libraries, and current
authoritative documentation.

It exists so that E25-02 through E25-07 can start authoring without re-deciding identity,
scope, or concept ownership, and so that E25-08 has a traceable list of assessment gaps.

It is **not** a second blueprint. Objectives, depth layers and
Teach/Bridge/Reference/Exclude decisions stay in the blueprint; this document records the
decisions the blueprint left open, the reconciliation of the blueprint's former Units 7 and
8, and the review findings that would otherwise have to be re-derived.

**This is a reconciliation, not a greenfield plan.** The blueprint already mapped this
territory as Unit 7 (Effects and Composable Lifecycle) and Unit 8 (ViewModel, Flow and
Production Screen State). Both predate the shipped E23 Units, the whole of E24, and the
current epic boundaries. [Reconciling blueprint Unit 7](#reconciling-blueprint-unit-7) and
[Reconciling blueprint Unit 8](#reconciling-blueprint-unit-8) record what survived, what was
absorbed, what moved, and what was handed to the architecture curriculum.

**Review date: 2026-09-11.** Everything in
[Source-sensitive and platform-sensitive claims](#source-sensitive-and-platform-sensitive-claims)
was checked on that date against the versions this repository is configured with, by reading
the resolved artifacts' sources and by running throwaway probes on this project's JVM target.

## Scope confirmation

The merged epic assumes six instructional Units. **The six-Unit structure is unchanged.**

The audit found no pedagogical reason to split, merge, add or drop a Unit. Each boundary is a
genuine change of mental model rather than a change of API family, which is the test the E24
plan established:

1. **Unit 7** — who owns a piece of state on a complete screen.
2. **Unit 8** — how something the composition cannot observe becomes something it can.
3. **Unit 9** — what gives work started by a composition a defined lifetime.
4. **Unit 10** — who owns the *trigger*, and how a long-lived effect reads a current value.
5. **Unit 11** — how the composition owns the external world it touches.
6. **Unit 12** — choosing the smallest sufficient mechanism, and what a transient occurrence
   actually requires.

The **Lesson count is 22**, against the 9 the former blueprint Units 7 and 8 planned for the
same territory. The difference is the epic's whole point: the old Unit 7 taught five APIs in
five Lessons, and the old Unit 8 spent more than half its material on application
architecture that E26 owns. Per-Unit reasons for the counts are in
[Unit and Lesson structure](#unit-and-lesson-structure).

One structural risk was found and is **not** solved by this plan; it is recorded so that
E25-08 is not surprised by it. Four of the six Units take `compose_side_effects` as their
only primary concept, so they receive an identical practice pool. See
[The `compose_side_effects` overlap](#the-compose_side_effects-overlap).

This issue is planning only. It introduces no production Unit, Lesson or Question, and
changes no production curriculum JSON. Every identity proposed here lives in documentation
until the authoring issue that ships it.

## How the authoring issues use this document

| Issue | Reads |
| --- | --- |
| E25-02 | Unit 7 identities, objectives and boundaries; [What E23 already teaches](#what-e23-already-teaches-and-e25-must-not-repeat) in full, because Unit 7 is the Unit most at risk of duplicating shipped material; the Unit 7 rows of the semantic review; the two shipped-Lesson edits in [Cross-linking and shipped-content edits](#cross-linking-and-shipped-content-edits) |
| E25-03 | Unit 8 identities and boundaries; [Lifecycle-aware collection in this repository](#lifecycle-aware-collection-in-this-repository) **in full** — it is the acceptance-critical finding of this issue; the `collectAsState` and `produceState` entries in the source section |
| E25-04 | Unit 9 identities and boundaries; [Effect keys: what the comparison actually is](#effect-keys-what-the-comparison-actually-is) **in full**, including the measured lambda cases; the `LaunchedEffect` entry in the source section |
| E25-05 | Unit 10 identities and boundaries; the `rememberUpdatedState` and `rememberCoroutineScope` entries in the source section, including the measured latest-value probe |
| E25-06 | Unit 11 identities and boundaries; the `DisposableEffect`, `SideEffect`, `produceState` and `awaitDispose` entries in the source section, including the measured `produceState` key-change result |
| E25-07 | Unit 12 identities and boundaries; the [misconception targets](#misconception-and-reasoning-targets) for L12.2 and L12.3; E24's `lesson_choosing_a_stream_abstraction` and `shared_flow_try_emit_true_is_not_delivery`, which it must extend rather than restate |
| E25-08 | [Assessment gaps for E25-08](#assessment-gaps-for-e25-08) in full, re-checked against the finished Lessons, plus [Mapping, level and defect candidates](#mapping-level-and-defect-candidates) and [The `compose_side_effects` overlap](#the-compose_side_effects-overlap) |
| E25-09 | [Handoff](#handoff) — sequencing, cross-links, and the limitations recorded here |

Each authoring issue also records its outcomes in this document, as the E23 and E24 plans do,
so a later issue re-checks a finding rather than re-deriving it.

---

## Configured versions

Read from `gradle/libs.versions.toml`, `shared/build.gradle.kts` and the resolved
`jvmCompileClasspath` on 2026-09-11, not from memory. The JetBrains-published Compose and
lifecycle artifacts are thin aliases over AndroidX artifacts at their own versions, and the
AndroidX version is the one whose source settles behaviour.

| Component | Declared | Actually resolved |
| --- | --- | --- |
| Kotlin | `kotlin = "2.4.10"` | 2.4.10 |
| Compose Multiplatform | `composeMultiplatform = "1.11.1"` | `org.jetbrains.compose.runtime:runtime:1.11.1` → **`androidx.compose.runtime:runtime:1.11.2`** |
| Compose Material 3 | `material3 = "1.11.0-alpha07"` | as declared |
| Lifecycle | `androidx-lifecycle = "2.11.0-beta01"` | `org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.11.0-beta01` → **`androidx.lifecycle:lifecycle-runtime-compose:2.11.0-beta01`** |
| kotlinx.coroutines | `kotlinx-coroutines = "1.11.0"` | 1.11.0 (Compose's own 1.9.0 constraint is upgraded to it) |
| AGP | `agp = "9.0.1"` | 9.0.1 |

**Lifecycle dependencies and their source-set placement.** `shared/build.gradle.kts` declares
all three in `commonMain.dependencies`, not in any platform source set:

```kotlin
implementation(libs.androidx.lifecycle.viewmodelCompose)      // lifecycle-viewmodel-compose
implementation(libs.androidx.lifecycle.runtimeCompose)        // lifecycle-runtime-compose
implementation(libs.androidx.lifecycle.viewmodelNavigation3)  // lifecycle-viewmodel-navigation3
```

`androidx-lifecycle-runtimeKtx` exists in the catalogue but is declared by no module; it is
the Android-only artifact and is not on the shared classpath.

**Targets.** `:shared` builds `androidTarget`, `jvm`, `js(browser)`, `wasmJs(browser)`,
`iosArm64` and `iosSimulatorArm64`. CI (`.github/workflows/main.yml`) builds Android, desktop
and web plus `:shared:check`; **iOS is not built on CI** and remains a local macOS check.

This matters to E25 more than to any earlier Compose epic: the question "is lifecycle-aware
collection available here?" is not answered by Android documentation, and the answer this
repository needs is not the answer an Android-only course would give. See
[Lifecycle-aware collection in this repository](#lifecycle-aware-collection-in-this-repository).

---

## What E23 already teaches, and E25 must not repeat

Every shipped Compose Lesson was read in full before this plan proposed a Unit. The six
shipped Units are `unit_thinking_in_compose`, `unit_state_and_state_ownership`,
`unit_recomposition`, `unit_identity_keys_and_stability`,
`unit_derived_state_and_expensive_work` and `unit_snapshot_fundamentals` — 21 Lessons.

The Lessons that overlap E25's territory, and the exact line E25 must respect:

| Shipped Lesson | Already teaches | E25's remaining responsibility |
| --- | --- | --- |
| `lesson_composable_execution` | That the body may run repeatedly, be skipped, run in an order you did not write, or be discarded; that the body is not an event handler; a list of lines that should make you look twice; the forward-compatibility framing, with the explicit correction that Compose does **not** currently run composables in parallel | Unit 9 L9.1 turns this contract into an **ownership problem** — work started from the body has no owner and no lifetime — and answers it with an API family. It must not re-derive the contract |
| `lesson_state_down_events_up` | The direction; callbacks as intent; the ViewModel-injected child anti-pattern *by name*, including a `collectAsStateWithLifecycle` code sample; the "UDF means everything goes in a ViewModel" misconception | Unit 7 L7.2 and L7.3 apply the direction to a **whole screen** — one immutable state value, a stateless content composable, and what goes wrong when writable state travels down. The direction itself is settled |
| `lesson_state_hoisting` | Stateful/stateless; the overload pair; the three documented "at least" rules; **the over-hoisting cost**; the plain state-holder class remembered in the Composition; **the three ownership tests (who reads, who writes, how long must it live)**; UI-element state against screen UI state; "hoist it" ≠ "put it in the ViewModel" | This is the single biggest duplication risk in E25. See [How Unit 7 earns its place](#how-unit-7-earns-its-place-beside-lesson_state_hoisting) |
| `lesson_remember_composition_memory`, `lesson_remember_key_memoization` | `remember`; that a key list is a dependency list; both failure directions of a wrong key list; that mutating an object used as a key invalidates nothing | Unit 9 L9.3 and L9.4 apply the *same* comparison model to effect **lifetime** rather than to a cached value, and are explicit that this is the same mechanism. They must cross-link, not re-derive |
| `lesson_composition_and_recomposition`, `lesson_recomposition_scopes`, `lesson_recomposition_cost` | What recomposition is, what it invalidates, and that it is not the problem | Unit 8 needs only "a collected value changing is a state write like any other"; Unit 9 needs only "the body may re-execute" |
| `lesson_composable_identity`, `lesson_keys_and_identity_in_lists` | Call-site identity; what leaving composition destroys | Unit 9 L9.2 uses call-site identity to explain *effect* lifetime. Supporting, not primary |
| `lesson_stability_and_skipping`, `lesson_immutability_vs_stability` | Strong skipping; that unstable parameters are compared by instance (`===`) and stable ones with `equals()` | Unit 9 L9.3 needs exactly this contrast to say what an effect key comparison is **not**. It is the cleanest available cross-link in the epic |
| `lesson_remember_key_memoization` | `remember(key)` equality semantics | Unit 9 L9.3's primary source of the "same comparison as a remembered calculation" claim |
| `lesson_snapshot_flow` | `snapshotFlow`; that it is collected inside a `LaunchedEffect`; four bounded facts about Flow; that snapshot changes are conflated | Unit 8 must not re-teach `snapshotFlow`; Unit 12 may use it as one reachable outcome. The Lesson already says the effect family "is a later unit's subject" — Unit 9 fulfils that pointer |
| `lesson_work_outside_composition` | Placement as a responsibility question; that moving work to a ViewModel is not moving it off the main thread | Unit 7 L7.4 continues this into *who owns screen state*; Unit 9 continues it into *which mechanism carries the work* |

### How Unit 7 earns its place beside `lesson_state_hoisting`

The epic's acceptance criteria for E25-02 read, at a glance, like a restatement of the
shipped hoisting Lesson. They are not, but the difference has to be stated precisely or the
Unit will be written as a duplicate.

**What `lesson_state_hoisting` decides:** where *one* piece of state should live, asked one
piece at a time. Its worked example is a single switch on a settings screen, and its answer
is the lowest sensible owner.

**What Unit 7 decides:** how a *whole screen* is composed when several classes of state
coexist and one of the owners sits outside the Composition. That is a different question with
different failure modes:

- A real screen carries local element state, subtree-hoisted state and screen-level state at
  the same time, and the interesting decisions are the ones where two of them are plausible.
  The shipped Lesson never puts them on one screen together.
- A screen-level owner produces **one current state value** that the content renders. The
  shipped Lesson never models screen state as a value; it reasons about individual holders.
- The **stateless content boundary** — a stateful screen composable that owns the wiring and
  a stateless content composable that renders a state value and reports intent — is named
  nowhere in shipped content. `lesson_state_down_events_up` shows the anti-pattern (a child
  holding a ViewModel) but not the boundary that replaces it.
- **Passing writable state downward** is treated in shipped content only through the
  ViewModel-injection example. A `MutableState`, a mutable holder, or a `var` setter handed
  to a child is the more common production version and is unaddressed.
- The shipped Lesson's own forward pointer — "that is where a screen-level owner takes over,
  and the `ViewModel` unit later in this path covers it" — is an unfulfilled promise that
  Unit 7 L7.4 exists to keep, in bounded form.

**The rule for E25-02:** Unit 7 may restate the three ownership tests in one sentence and
must link to `lesson_state_hoisting` for them. It must not re-derive the three "at least"
rules, must not re-explain the overload pair, and must not repeat the over-hoisting argument
as if it were new — it should *apply* it to a screen that has over-hoisted three pieces of
state and show the resulting parameter list. If a draft of any Unit 7 Lesson reads like a
better version of `lesson_state_hoisting`, that Lesson is wrong.

## What E24 already teaches, and E25 must not repeat

The six shipped `async_reactive` Units are `unit_coroutines_and_structured_concurrency`,
`unit_context_dispatchers_and_concurrency`, `unit_cancellation_failure_and_coordination`,
`unit_flow_fundamentals`, `unit_flow_composition_timing_and_failure` and
`unit_stateflow_sharedflow_and_hot_streams` — 29 Lessons.

E24's own plan already fixed this boundary from its side: no E24 Lesson names
`collectAsState`, `collectAsStateWithLifecycle`, `LaunchedEffect`, `rememberCoroutineScope`,
`produceState` or `repeatOnLifecycle`. E25 holds the other side of the same line.

| Shipped E24 Lesson | E24 answers | E25 asks instead |
| --- | --- | --- |
| `lesson_coroutine_scope_ownership` | Who owns a coroutine's lifetime, and what an un-owned scope costs | Which *Compose* boundary owns this coroutine — a composition, a remembered scope, or neither (Units 9, 10) |
| `lesson_cooperative_cancellation`, `lesson_cancellation_cleanup_and_timeouts` | That cancellation is cooperative and what may run after it | What leaving composition cancels, and what happens to in-flight effect work (Unit 9) |
| `lesson_flow_collection_lifetime` | That the collector's lifetime is the coroutine's lifetime | Where that coroutine comes from at the Compose boundary, and what the lifecycle adds to it (Unit 8) |
| `lesson_state_flow` | `StateFlow`'s current-value contract and equality-based conflation | Why a `StateFlow` is not observable to a composition until it is converted (Unit 8) |
| `lesson_shared_flow` | Replay, buffering, subscribers, and that `tryEmit() == true` is not delivery | Whether that guarantee satisfies *this* transient UI requirement (Unit 12) |
| `lesson_sharing_cold_flows` | `stateIn`, `shareIn`, `SharingStarted`, `stopTimeoutMillis` | Only that stopping the UI collector does not by itself stop the upstream — one bridge paragraph in Unit 8 L8.3, then a link (Unit 8) |
| `lesson_choosing_a_stream_abstraction` | That a must-not-be-lost occurrence is satisfied by no hot Flow configuration | What the *Compose side* does with a transient occurrence, and when the answer is "this is not a Compose problem" (Unit 12) |
| `lesson_flow_builders_and_callback_adapters` | `callbackFlow` and `awaitClose` | When a Compose-local producer with `awaitDispose` is the right owner instead, decided from reuse (Unit 11 L11.4) |

`lesson_choosing_a_stream_abstraction` already contains the forward pointer E25 fulfils: "how
a screen collects a stream safely, and how a one-off effect should be modelled and consumed
in a UI — belongs to the effects and architecture material later in this path."

---

## Reconciling blueprint Unit 7

The former Unit 7, *Effects and Composable Lifecycle*, planned five Lessons. Every one was
reviewed against the shipped E23 and E24 content and against the current epic boundary.

| Former Lesson | Decision | Where it goes |
| --- | --- | --- |
| **L7.1 — Why Side Effects Need Controlled APIs** | **Kept, narrowed.** Its Core is now partly shipped: `lesson_composable_execution` already teaches that the body may run repeatedly, be skipped and be abandoned, and already lists the lines that should make a reader look twice. What survives is the *consequence* the shipped Lesson deliberately stops before — work with no owner and no lifetime — and the four problems the API family solves | Unit 9, `lesson_why_effects_are_controlled` |
| **L7.2 — `LaunchedEffect` and Effect Keys** | **Split into three.** The former Lesson carried three mental models at once: composition-owned coroutine lifetime, what a key list *claims*, and the two opposite ways of getting a key list wrong. The epic asks for keys to receive substantial treatment, and Rule 8 of the authoring contract says the number of distinct ideas is the signal to split on | Unit 9, `lesson_launched_effect`, `lesson_effect_keys_as_dependencies`, `lesson_effect_key_failures` |
| **L7.3 — `rememberCoroutineScope`: Launching From Events** | **Kept, promoted into its own Unit with a partner.** Its Senior line — "why this scope is the wrong place for work that must outlive the screen, and what belongs in the `ViewModel` scope instead" — is **narrowed**: E25 identifies that the work belongs elsewhere and stops. The `viewModelScope` answer is E26's, and E24's `lesson_coroutine_scope_ownership` and `viewmodel_scope_cleared_cancellation` already carry the mechanics | Unit 10, `lesson_remember_coroutine_scope` and `lesson_who_owns_the_trigger` |
| **L7.4 — `DisposableEffect` and Cleanup** | **Kept essentially as planned.** The only change is framing: the epic requires that `DisposableEffect` be distinguished by *what it is for* rather than described as a launched effect with cleanup bolted on | Unit 11, `lesson_disposable_effect` |
| **L7.5 — `rememberUpdatedState` and `SideEffect`** | **Split, and the two halves separated by two Units.** Pairing them was an artefact of "the two remaining cases the earlier APIs do not cover", which is an API-inventory reason rather than a conceptual one. `rememberUpdatedState` belongs with event-driven work because both are about who owns a trigger and which value is current; `SideEffect` belongs with `DisposableEffect` and `produceState` because all three are about the external world | Unit 10 `lesson_remember_updated_state`; Unit 11 `lesson_side_effect_publication` |

**Added, with no predecessor in the former Unit 7:** `produceState` and `awaitDispose` (Unit
11), the Flow-adapter-or-Compose-producer decision (Unit 11), and the whole of Unit 12. The
former blueprint named `produceState` nowhere on the main path; it appears only as a
distractor in a shipped Question.

**The former five-Lesson shape is not preserved.** Keeping it would have produced one
API-per-Lesson Unit, which Rule 5 of the authoring contract explicitly names as the failure
mode to avoid.

## Reconciling blueprint Unit 8

The former Unit 8, *ViewModel, Flow and Production Screen State*, is the Unit this epic
dissolves. Its purpose line — "connect Compose to a real application... explicitly
bridge-heavy" — is exactly the material the current roadmap assigns to E26.

| Former Lesson | Decision |
| --- | --- |
| **L8.1 — The Production Screen Pipeline** (Repository → ViewModel → `UiState` → `StateFlow` → Compose; primary `compose_udf`; supporting `repository_pattern`, `mvvm`, `layered_architecture`, `single_source_of_truth`) | **Moved to E26**, except one bounded fragment. Layering, the repository hop, MVVM, and "why the ViewModel boundary exists at all" are application architecture. What E25 keeps is the *screen-level owner as a boundary the composition talks to*: a thing outside the Composition that produces the screen's state and receives its events, with everything about how that thing is structured deferred. That fragment is Unit 7 `lesson_screen_state_owner_boundary` |
| **L8.2 — Collecting Flow and StateFlow in Compose** | **Kept and expanded into the whole of Unit 8.** One Lesson could not carry the state-conversion argument, `collectAsState`'s two overloads, collection lifetime and cost, and lifecycle-aware collection with a defensible multiplatform answer. Its blueprint text also contains a **factual error** that this reconciliation corrects; see below |
| **L8.3 — Modelling `UiState`** (sealed hierarchy against nullable-field data class, partial states, error representation) | **Moved to E26.** This is `UiState` modelling strategy, which the epic explicitly places outside E25. What E25 keeps is one sentence's worth: the screen renders **one immutable current state value**, and why immutability is what makes equality-based skipping work. That lives in Unit 7 `lesson_screen_state_and_ui_events`, which must not turn into a sealed-versus-nullable comparison |
| **L8.4 — State vs. Events, and What Belongs Where** | **Split three ways.** The local-state-that-has-no-business-in-a-ViewModel half goes to Unit 7 `lesson_classes_of_screen_state`. The Compose-side half of "a transient occurrence is not state" goes to Unit 12 `lesson_transient_ui_effects` and `lesson_transient_effect_delivery`. The consumable-event-channel-versus-acknowledged-state trade-off, `SavedStateHandle`'s interaction with it, and application-level event modelling go to **E26** |

### The factual error in the former L8.2

The blueprint's L8.2 Practical line reads:

> "...the platform-neutral option in shared Compose Multiplatform code, where the Android
> lifecycle-aware variant is not available on every target."

**This is false for this repository, and was probably false when it was written.** It is
corrected in the blueprint in the same change as this document. The evidence is in
[Lifecycle-aware collection in this repository](#lifecycle-aware-collection-in-this-repository).
It is recorded here rather than quietly edited because it is precisely the kind of
Android-only inference the epic's acceptance criteria forbid, and because Unit 8 exists partly
to stop a learner from drawing it.

### Two shipped Lessons point at a Unit that no longer exists

Dissolving Unit 8 leaves two shipped E23 Lessons pointing at a "ViewModel unit":

- `lesson_state_hoisting`: "...that is where a screen-level owner takes over, and the
  `ViewModel` unit later in this path covers it."
- `lesson_remember_saveable`: "How screen-level state is owned and produced is the subject of
  the ViewModel unit later in this path."

Neither sentence is wrong about the concept; both name a Unit that will never ship under that
name, and both promise more than E25 delivers, because the architecture half is E26's. **E25-02
owns both edits**, in the same change that ships Unit 7. See
[Cross-linking and shipped-content edits](#cross-linking-and-shipped-content-edits).

## Blueprint renumbering

Six Units replace two, so the blueprint's later Units shift. The decision and its checks:

- **Shipped Units 1–6 are untouched** — same numbers, same identities, same pedagogical
  responsibility.
- **E25 occupies Units 7–12.**
- **Former Units 9–14 become 13–18.** All six are blueprint-only: nothing in
  `learning_curriculum.json` corresponds to them, no production Unit or Lesson id encodes a
  number, and no learner progress record can reference them. The numbers are positional
  labels in one document.
- **Planned totals become 18 Units and 63 Lessons** (was 14 and 50; 50 − 9 + 22 = 63).

No production stable identity depends on blueprint numbering. Production Unit ids are
semantic (`unit_snapshot_fundamentals`), Lesson ids are semantic and deliberately unnumbered,
and `docs/architecture/study-progress.md` keys learner progress by Lesson id.

Every in-document cross-reference was located and updated: the Unit Order list, the ordering
rationale (which referenced Units 8 and 13), the prerequisite lines of Units 9–14, the
Reference and Exclude tables (Units 9, 10, 13), the Authoritative Source Families list (Units
8, 12, 13), and the Status section. Two other canonical documents refer to "the Compose
blueprint's Unit 8" and were updated in the same change:
`docs/content/coroutines-flow-learning-blueprint.md` (3 references) and
`docs/content/coroutines-flow-units-1-6-plan.md` (4 references).

---

## Identity conventions and proposed identities

The shipped content establishes the convention and this plan follows it:

- **Unit id** — `unit_` plus the Unit title in snake case, shortened where the full title
  would be unreadable (the precedent set by `unit_identity_keys_and_stability` and
  `unit_context_dispatchers_and_concurrency`).
- **Lesson id** — `lesson_` plus the concept the Lesson owns, in snake case. Lesson ids are
  **not** numbered: the `L7.1` labels below are positional and would be wrong the moment a
  Lesson moves.
- Ids are stable once shipped. Renaming one breaks learner study-progress records.
- Lesson ids are unique across **all** Units; `LearningCurriculumValidator` rejects a
  duplicate (`DUPLICATE_LESSON_ID`).
- An id is named for the decision the Lesson teaches wherever the Lesson is decision-oriented,
  and for the API only where the API *is* the subject. `lesson_who_owns_the_trigger` and
  `lesson_flow_adapter_or_compose_producer` are deliberately not API-named;
  `lesson_launched_effect` and `lesson_produce_state` deliberately are.

Three shortened Unit ids, each recorded so a later issue does not "fix" them:

| Title in full snake case | Chosen id | Reason |
| --- | --- | --- |
| `unit_observable_state_collection_and_lifecycle` | `unit_observable_state_collection` | 46 → 32 characters; "and lifecycle" is still taught in the Unit |
| `unit_cleanup_external_synchronization_and_state_producers` | `unit_cleanup_synchronization_and_producers` | 57 → 42 |
| `unit_production_ui_effects_and_mechanism_selection` | `unit_production_ui_effects_and_selection` | 50 → 40 |

All six Units take **`android_ui`** as their home Topic, consistent with the shipped Compose
Units and with the blueprint. Nothing in the evidence argues for moving this curriculum to
`architecture` or `async_reactive`: the Units are about what a Compose screen does, and their
cross-Topic reach is handled by supporting mappings, which is what Rule 3 of the authoring
contract is for.

| Blueprint | Unit id | Unit title |
| --- | --- | --- |
| Unit 7 | `unit_production_screen_state_and_udf` | Production Screen State and Unidirectional Data Flow |
| Unit 8 | `unit_observable_state_collection` | Observable State Collection and Lifecycle |
| Unit 9 | `unit_effect_lifecycle_and_launched_effect` | Effect Lifecycle and `LaunchedEffect` |
| Unit 10 | `unit_latest_values_and_event_driven_work` | Latest-Value Effects and Event-Driven Coroutine Work |
| Unit 11 | `unit_cleanup_synchronization_and_producers` | Cleanup, External Synchronization and State Producers |
| Unit 12 | `unit_production_ui_effects_and_selection` | Production UI Effects and Mechanism Selection |

| Blueprint | Lesson id | Lesson title | Primary | Supporting |
| --- | --- | --- | --- | --- |
| L7.1 | `lesson_classes_of_screen_state` | Three Classes of State on One Screen | `compose_state_hoisting` | `compose_state`, `state_ownership`, `viewmodel_lifecycle` |
| L7.2 | `lesson_stateless_screen_content` | The Stateless Content Boundary | `compose_udf` | `compose_state_hoisting`, `compose_previews`, `compose_fundamentals` |
| L7.3 | `lesson_screen_state_and_ui_events` | One Screen State Value, Events Back Up | `compose_udf` | `compose_state`, `unidirectional_data_flow`, `kotlin_data_classes`, `compose_stability` |
| L7.4 | `lesson_screen_state_owner_boundary` | The Screen-Level Owner as a Bounded Bridge | `compose_state_hoisting` | `state_ownership`, `viewmodel_lifecycle`, `kmp_lifecycle_viewmodel`, `configuration_changes` |
| L8.1 | `lesson_external_state_in_compose` | What a Composable Can and Cannot Observe | `compose_state` | `stateflow`, `hot_vs_cold_streams`, `compose_snapshot_system` |
| L8.2 | `lesson_collect_as_state` | `collectAsState`: Converting a Stream into Compose State | `compose_state` | `flow_collection`, `stateflow`, `compose_recomposition` |
| L8.3 | `lesson_collection_lifetime_and_cost` | Collection Has a Lifetime and a Cost | `compose_state` | `flow_collection`, `flow_sharing`, `coroutine_cancellation`, `lifecycle_coroutines` |
| L8.4 | `lesson_lifecycle_aware_collection` | Lifecycle-Aware Collection and the Lifecycle a Screen Actually Has | `compose_state` | `lifecycle_aware_apis`, `kmp_lifecycle_viewmodel`, `compose_multiplatform`, `flow_sharing` |
| L9.1 | `lesson_why_effects_are_controlled` | Why Compose Needs an Effect API | `compose_side_effects` | `compose_fundamentals`, `compose_recomposition` |
| L9.2 | `lesson_launched_effect` | `LaunchedEffect`: Work a Composition Owns | `compose_side_effects` | `coroutine_scope`, `coroutine_cancellation`, `structured_concurrency`, `compose_identity_keys` |
| L9.3 | `lesson_effect_keys_as_dependencies` | What an Effect's Keys Declare | `compose_side_effects` | `compose_derived_state`, `kotlin_equality`, `compose_stability` |
| L9.4 | `lesson_effect_key_failures` | Two Ways to Get Effect Keys Wrong | `compose_side_effects` | `compose_identity_keys`, `coroutine_cancellation`, `kotlin_lambdas` |
| L10.1 | `lesson_remember_updated_state` | Reading the Current Value Without Restarting | `compose_side_effects` | `compose_state`, `compose_recomposition`, `kotlin_lambdas` |
| L10.2 | `lesson_remember_coroutine_scope` | `rememberCoroutineScope`: Launching From an Event | `compose_side_effects` | `coroutine_scope`, `coroutine_builders`, `structured_concurrency` |
| L10.3 | `lesson_who_owns_the_trigger` | Composition-Driven or Event-Driven? | `compose_side_effects` | `coroutine_scope`, `viewmodel_lifecycle`, `lifecycle_coroutines`, `coroutine_cancellation` |
| L11.1 | `lesson_disposable_effect` | Registration and Release as One Decision | `compose_side_effects` | `lifecycle_aware_apis`, `memory_leaks`, `lifecycle_leaks` |
| L11.2 | `lesson_side_effect_publication` | `SideEffect`: Publishing to Non-Compose Code | `compose_side_effects` | `compose_recomposition`, `compose_fundamentals`, `compose_state` |
| L11.3 | `lesson_produce_state` | `produceState`: a Composition-Scoped Producer | `compose_side_effects` | `compose_state`, `flow_collection`, `coroutine_cancellation` |
| L11.4 | `lesson_flow_adapter_or_compose_producer` | A Flow Below the UI, or a Producer at the Boundary? | `compose_side_effects` | `flow_fundamentals`, `hot_vs_cold_streams`, `separation_of_concerns` |
| L12.1 | `lesson_choosing_a_compose_mechanism` | Choosing the Smallest Sufficient Mechanism | `compose_side_effects` | `compose_state`, `compose_state_hoisting`, `compose_udf` |
| L12.2 | `lesson_transient_ui_effects` | Rendering State and Running a Transient Effect | `compose_side_effects` | `coroutine_scope`, `compose_state`, `sharedflow` |
| L12.3 | `lesson_transient_effect_delivery` | What Delivery Guarantee Does This Occurrence Need? | `compose_side_effects` | `sharedflow`, `hot_vs_cold_streams`, `state_ownership`, `stateflow` |

### Identity and mapping checks performed

Checked against the bundled production JSON on 2026-09-11 by script, not from memory.

- **Uniqueness.** None of the 6 proposed Unit ids and 22 proposed Lesson ids collides with
  the 12 shipped Unit ids or the 50 shipped Lesson ids in `learning_curriculum.json`, or with
  each other.
- **Mapping validity.** All **35** distinct Subtopic ids used above exist in
  `initial_curriculum.json` and are `ACTIVE`. Eleven are `android_ui`, eleven
  `async_reactive`, three `architecture`, three `kotlin_language`, three
  `lifecycle_navigation`, two `performance`, two `kmp`.
- **Overlap.** No Lesson lists the same Subtopic as both primary and supporting, which the
  validator rejects (`PRIMARY_SUPPORTING_SUBTOPIC_OVERLAP`).
- **Shared primaries.** Four Subtopics are primary in more than one Lesson:
  `compose_state_hoisting` (L7.1, L7.4), `compose_udf` (L7.2, L7.3), `compose_state`
  (L8.1–L8.4) and `compose_side_effects` (all fourteen Lessons of Units 9–12). The contract
  expects this shape; the last one has a consequence that
  [needs its own section](#the-compose_side_effects-overlap).
- **Primaries shared with shipped Units.** `compose_state_hoisting` and `compose_udf` are
  already primary in `lesson_state_hoisting` and `lesson_state_down_events_up`;
  `compose_state` is already primary in three Lessons of `unit_state_and_state_ownership`.
  Unit 7's and Unit 8's practice therefore overlaps the practice of E23 Units 1 and 2. That
  is expected and acceptable — but it is also why Unit 8's current practice pool teaches none
  of what Unit 8 teaches. See GAP-U8-A.
- **Forward references.** No `relatedLessonIds` value proposed here names a Lesson that does
  not yet ship at the time its own Unit ships. See
  [Cross-linking and shipped-content edits](#cross-linking-and-shipped-content-edits).
- **Taxonomy gaps.** Five concepts have no exact Subtopic and are recorded in the blueprint
  rather than given invented ids. See [Taxonomy gaps](#taxonomy-gaps).

---

## Unit and Lesson structure

Objectives restate the blueprint's in one line and add what this review had to settle: the
reasoning a learner must be able to demonstrate, where each prerequisite is taught or
bridged, and the boundary that keeps a Lesson from absorbing its neighbour.

### Unit 7 — Production Screen State and Unidirectional Data Flow

**Purpose:** decide who owns each piece of state on a complete screen, before any effect API
exists. **Prerequisites:** E23 Units 1–4, especially `lesson_state_down_events_up`,
`lesson_state_hoisting`, `lesson_remember_saveable` and `lesson_stability_and_skipping`. No
E24 prerequisite. **Four Lessons** because ownership, the content boundary, the state value
and the outside-the-Composition owner are four decisions, and the epic's acceptance criteria
name all four.

**L7.1 `lesson_classes_of_screen_state` — Three Classes of State on One Screen**

- **Objective:** given a screen with several pieces of state, place each one.
- **Demonstrable reasoning:** predict what breaks when a given piece is placed one level too
  low or two levels too high; name which of the three ownership tests decided it.
- **Teach:** local UI-element state; subtree-hoisted state; screen-level state; the three
  classes compared on one screen, including at least one piece that must stay local.
- **Bridge:** the three ownership tests (one sentence, link to `lesson_state_hoisting`);
  what a screen-level owner survives (`viewmodel_lifecycle`, one sentence).
- **Exclude:** ViewModel construction, injection, `SavedStateHandle`, state-holder patterns.
- **Boundary:** L7.1 places state. L7.4 says what the out-of-Composition owner is.

**L7.2 `lesson_stateless_screen_content` — The Stateless Content Boundary**

- **Objective:** split a screen into a stateful screen composable and a stateless content
  composable, and justify the split by what it makes possible.
- **Demonstrable reasoning:** say what a given content composable can and cannot be previewed
  or tested with, and why.
- **Teach:** the two-composable screen shape; what the stateless half gains (reuse, preview,
  test, one place to look for wiring); how the split survives the state owner changing.
- **Bridge:** previews as a motivation only (`compose_previews` holds no Questions and is used
  as vocabulary, not as claimed coverage).
- **Exclude:** Compose UI testing (E31), preview tooling mechanics.

**L7.3 `lesson_screen_state_and_ui_events` — One Screen State Value, Events Back Up**

- **Objective:** model what the screen renders as one immutable current value, and what the
  user does as callbacks describing intent.
- **Demonstrable reasoning:** given a content composable that receives a `MutableState` or a
  holder reference, name the specific problems that creates and rewrite the signature.
- **Teach:** one immutable state value per screen; callbacks as intent rather than as
  assignment; why writable state travelling downward removes the single write path; why
  immutability is what lets equality-based skipping work.
- **Bridge:** `unidirectional_data_flow` as the architecture-side name (one sentence);
  `compose_stability` for the equality argument (link to `lesson_stability_and_skipping`).
- **Exclude:** sealed-versus-nullable `UiState` modelling, partial states, error
  representation, intent-as-a-type and reducers — **all E26**.
- **Boundary:** this Lesson says the screen renders one value. It does not say how that value
  is designed.

**L7.4 `lesson_screen_state_owner_boundary` — The Screen-Level Owner as a Bounded Bridge**

- **Objective:** describe the boundary between the composition and the thing that owns screen
  state, and know where the curriculum hands over.
- **Demonstrable reasoning:** say what changes about a piece of state when its owner moves
  outside the Composition, and what does not; for a given action — recomposing, rotating,
  navigating forward, popping the destination — say whether the screen-level state survives,
  and name the lifetime that decided it.
- **Teach:** what "outside the Composition" changes — the state stops being tied to the
  composable's own composition, so recomposition, the composable leaving composition and UI
  recreation no longer decide its fate; that the composition talks to it through a state value
  down and callbacks up, which is L7.3 applied one level higher; that a plain remembered state
  holder is often the right answer and a screen-level owner is not a default.
- **Teach, and this is the correction the Lesson exists to make:** moving state out of the
  Composition does not by itself make it survive anything. **What it survives is decided by the
  owner's own lifetime**, which a platform host controls. In this repository each Navigation 3
  back-stack entry owns its own `ViewModelStore` and the ViewModel is cleared when that entry is
  removed (`docs/architecture/overview.md`), so a popped destination's screen-level state is
  gone even though it lived outside the Composition. "Outside the Composition" is a change of
  owner, not a promise of persistence.
- **Bridge:** `viewmodel_lifecycle` and `kmp_lifecycle_viewmodel` — in this repository the
  owner's *lifetime* is supplied by a platform host, which is why the boundary is worth naming
  (see `kmp_shared_viewmodel_owner_platform`); `configuration_changes` for the Android case.
- **Exclude:** MVVM, MVI, layering, repositories, use cases, `UiState` design, DI, and
  `viewModelScope` — E26 and E27. This Lesson must contain no architecture diagram.

### Unit 8 — Observable State Collection and Lifecycle

**Purpose:** turn E24's streams into something a composition can render, and be honest about
what the collection costs. **Prerequisites:** Unit 7; E23 `lesson_observable_state`,
`lesson_snapshot_observation`, `lesson_composition_and_recomposition`; E24
`lesson_flow_collection_lifetime`, `lesson_state_flow`, `lesson_sharing_cold_flows`. **Four
Lessons** because the conversion argument, the API, the lifetime/cost argument and the
lifecycle answer are four ideas, and folding the lifecycle answer into the API Lesson is what
produces "use the one with the longer name".

**L8.1 `lesson_external_state_in_compose` — What a Composable Can and Cannot Observe**

- **Objective:** explain why a `Flow` or `StateFlow` cannot drive a composable directly.
- **Demonstrable reasoning:** given `val x = someStateFlow.value` read in a composable body,
  predict what the screen does when the flow changes, and why.
- **Teach:** composition observes *snapshot state* reads and nothing else; a stream is not
  snapshot state; conversion is therefore a real operation with a contract, not ceremony.
- **Bridge:** `stateflow`'s current-value contract (one sentence, link to `lesson_state_flow`).
- **Boundary:** no API is named until L8.2. This is the problem-first anchor.

**L8.2 `lesson_collect_as_state` — `collectAsState`: Converting a Stream into Compose State**

- **Objective:** use the state-conversion API correctly and know exactly what it does.
- **Demonstrable reasoning:** say what the returned `State` holds at first composition for a
  `StateFlow` and for a plain `Flow`, and predict what happens when the flow instance passed in
  is constructed fresh on every recomposition.
- **Teach:** both overloads; `StateFlow.value` as the initial value; the mandatory `initial`
  for a plain `Flow`; that a collected value arriving is an ordinary state write and drives
  ordinary recomposition; that the conversion itself is keyed, so a new flow instance restarts
  it.
- **Teach, explicitly:** that **manual collection inside `LaunchedEffect` is not the default
  way to render ongoing state.** Where a Lesson shows manual collection, it must say why — the
  collection is performing an effect rather than producing state. This distinction returns in
  Unit 12.
- **Bridge:** the `context` parameter, named and bounded.
- **Exclude:** Flow operators, `flowOn`, buffering — E24.

**L8.3 `lesson_collection_lifetime_and_cost` — Collection Has a Lifetime and a Cost**

- **Objective:** reason about how long a collection runs and what that costs.
- **Demonstrable reasoning:** for a screen that is navigated away from, say what is still
  running and what decides it.
- **Teach:** the collection's lifetime is the call site's composition; what "still collecting"
  actually costs (an active collector, an active upstream, work applied to a UI nobody sees);
  that **stopping the UI collector does not by itself stop upstream production**.
- **Bridge:** `flow_sharing` — exactly one paragraph saying the upstream's owner and sharing
  policy decide whether production stops, then a link to `lesson_sharing_cold_flows`. Sharing
  policies, `stopTimeoutMillis` and `replayExpirationMillis` are **not** retaught.

**L8.4 `lesson_lifecycle_aware_collection` — Lifecycle-Aware Collection and the Lifecycle a Screen Actually Has**

- **Objective:** decide between plain and lifecycle-aware collection from the problem, and
  know what "lifecycle" means on each target this repository builds.
- **Demonstrable reasoning:** given a target and a user action (minimising a window, switching
  browser tabs, backgrounding an app), predict whether collection stops.
- **Teach:** the problem lifecycle-aware collection solves; `collectAsStateWithLifecycle` and
  its `minActiveState` default of `STARTED`; that `INITIALIZED` is rejected at runtime; that
  the API is a composition of `produceState` and `repeatOnLifecycle` and therefore inherits
  both contracts.
- **Teach, and this is the Unit's acceptance-critical claim:** the API **is** available to
  this repository's `commonMain`, what supplies the `LifecycleOwner` on each target, and how
  the lifecycle states are driven per target. Everything in
  [Lifecycle-aware collection in this repository](#lifecycle-aware-collection-in-this-repository)
  is authoring input for this Lesson.
- **Exclude:** Fragment collection, `repeatOnLifecycle`/`flowWithLifecycle` in a View host
  beyond one comparison sentence, `LiveData` beyond a comparison clause, lifecycle
  architecture.

### Unit 9 — Effect Lifecycle and `LaunchedEffect`

**Purpose:** answer "why does Compose need an effect API at all?" from the execution
contract, then give keys the treatment the epic asks for. **Prerequisites:** E23
`lesson_composable_execution`, `lesson_composition_and_recomposition`,
`lesson_composable_identity`, `lesson_remember_key_memoization`,
`lesson_stability_and_skipping`; E24 `lesson_coroutine_scope_ownership`,
`lesson_cooperative_cancellation`, `lesson_structured_concurrency`. **Four Lessons** because
the ownership problem, the API's lifetime contract, what a key list claims, and the two
failure directions are four ideas; the epic requires substantial key treatment and Rule 8
says to split on idea count rather than length.

**L9.1 `lesson_why_effects_are_controlled` — Why Compose Needs an Effect API**

- **Objective:** state the ownership problem that the effect family solves.
- **Demonstrable reasoning:** given work started in a composable body, name the three specific
  things that have no answer — how often it starts, who stops it, and what cleans it up.
- **Teach:** the body as a description with no lifetime of its own; the four problems the
  family solves, named before any API — lifetime-bound suspend work, registration with
  cleanup, event-driven launching, and a current value inside long-lived work.
- **Bridge:** the execution contract itself, in one paragraph, linking to
  `lesson_composable_execution`. E25-04 must not re-derive it.

**L9.2 `lesson_launched_effect` — `LaunchedEffect`: Work a Composition Owns**

- **Objective:** describe the coroutine lifetime `LaunchedEffect` creates.
- **Demonstrable reasoning:** trace one effect through entering, recomposing, a key change and
  leaving composition, saying what happens to in-flight work at each point.
- **Teach:** launch on entering; cancellation on leaving; cancel-and-relaunch on key change;
  the effect's identity is its call site's, so a call site that stops being composed takes its
  effect with it; the context the coroutine runs in comes from the composition.
- **Bridge:** cancellation is cooperative — one sentence, link to
  `lesson_cooperative_cancellation`. **The Lesson must not say cancellation preempts arbitrary
  code.**
- **Boundary:** keys are *named* here and *explained* in L9.3.

**L9.3 `lesson_effect_keys_as_dependencies` — What an Effect's Keys Declare**

- **Objective:** read a key list as a claim about what the effect's lifetime depends on.
- **Demonstrable reasoning:** for a given effect, decide whether a value belongs in the key
  list by asking whether a change to it should end the current work — not by asking whether
  the effect reads it.
- **Teach:** the key list as a dependency declaration with lifetime meaning; the exact
  comparison contract and its relationship to `remember(key)`; the contrast with the instance
  comparison that decides skipping; **the constant key as the degenerate claim** — "for this
  call site's whole composition lifetime" — which is sometimes exactly right and is often an
  imperative "run once" in disguise.
- **Source-sensitive:** everything in
  [Effect keys: what the comparison actually is](#effect-keys-what-the-comparison-actually-is).
- **Bridge:** `lesson_remember_key_memoization` for the memoization half;
  `lesson_stability_and_skipping` for the skipping contrast. Both are links, not restatements.
- **Note for E25-04:** the Android documentation's rule of thumb — "mutable and immutable
  variables used in the effect block of code should be added as parameters to the effect
  composable" — is a starting heuristic, not the model. The epic requires keys to be taught as
  ownership and lifetime. The Lesson should quote the rule of thumb, say what it is good for,
  and then say why a long-lived effect that must read a changing value is exactly the case it
  gets wrong — which is the handoff into Unit 10.

**L9.4 `lesson_effect_key_failures` — Two Ways to Get Effect Keys Wrong**

- **Objective:** diagnose both failure directions from a symptom.
- **Demonstrable reasoning:** given "the screen shows the previous item's data" or "the request
  is sent on every keystroke", name which direction of key error produced it.
- **Teach:** too few keys — stale ids in use, old work continuing, an effect that never
  restarts when its lifetime dependency changed; keys that needlessly stop comparing equal — a
  freshly allocated value or lambda supplied on every pass, cancelling and restarting work that
  was valid.
- **Teach, precisely:** *a new object is not automatically a changed key.* A freshly
  constructed value that compares equal does not restart the effect; a lambda usually does,
  but **not always**, and the measured cases are in the source section.

### Unit 10 — Latest-Value Effects and Event-Driven Coroutine Work

**Purpose:** separate two problems that look alike — an effect whose lifetime should not
change but whose value must, and work that composition should not start at all.
**Prerequisites:** Unit 9; E24 `lesson_coroutine_builders`, `lesson_coroutine_scope_ownership`.
**Three Lessons** because there are two problems and one decision.

**L10.1 `lesson_remember_updated_state` — Reading the Current Value Without Restarting**

- **Objective:** give a long-lived effect access to a current value without ending it.
- **Demonstrable reasoning:** compare the two available shapes — keying the effect on the
  changing value, and preserving the effect while reading through `rememberUpdatedState` — and
  say what each costs.
- **Teach:** what it is (a remembered `MutableState` reassigned on every recomposition); that
  the effect reads the current value at the moment it reads it; that it does **not** restart
  the effect; that it is **not** a way to prevent or reduce recomposition; that the classic
  case is a callback captured at launch.
- **Measured evidence:** the probe in the source section.

**L10.2 `lesson_remember_coroutine_scope` — `rememberCoroutineScope`: Launching From an Event**

- **Objective:** launch suspend work from a callback, with a lifetime the composition owns.
- **Demonstrable reasoning:** say when the scope is cancelled and what that does to work in
  flight.
- **Teach:** it returns a scope and launches nothing; the caller decides when work starts; the
  scope is cancelled when the call leaves composition; its context comes from the composition's
  applying dispatcher; it may not be given a parent `Job`.
- **Bridge:** `coroutine_builders` — one sentence, link to E24.

**L10.3 `lesson_who_owns_the_trigger` — Composition-Driven or Event-Driven?**

- **Objective:** choose between the two from who owns the trigger, not from API names.
- **Demonstrable reasoning:** given one screen written both ways, say which is correct and
  what the wrong one does on a recomposition or configuration change.
- **Teach:** the worked contrast on one screen — a snackbar or a scroll driven by composition
  state (wrong, fires again when it should not) against the same behaviour driven by the click
  (right); the rule stated as ownership: composition state owns the trigger, or the event does.
- **Teach, and then stop:** work that must continue after the composition is gone belongs to
  **neither** mechanism. Name that, name the symptom, and hand off. The owner is E26's subject
  and `viewmodel_scope_cleared_cancellation` already assesses the coroutine half.
- **Exclude:** `viewModelScope` as a taught mechanism, WorkManager, background work.

### Unit 11 — Cleanup, External Synchronization and State Producers

**Purpose:** own the external world a screen touches. **Prerequisites:** Units 9 and 10; E24
`lesson_flow_builders_and_callback_adapters`, `lesson_flow_collection_lifetime`. **Four
Lessons**, organised by problem rather than by API: release what you registered, publish
outward, produce inward, and decide where the adapter lives.

**L11.1 `lesson_disposable_effect` — Registration and Release as One Decision**

- **Objective:** treat registration and release as one symmetric decision the composition owns.
- **Demonstrable reasoning:** given a registration with no matching release, say concretely what
  accumulates and when.
- **Teach:** setup on entering; `onDispose` on leaving **and** on a key change; one disposal per
  setup; why setup and cleanup necessarily share a key set, and what an asymmetric one would
  break; listener, observer and external-resource scenarios.
- **Teach, as the epic requires:** `DisposableEffect` is distinguished by *what it is for* —
  owning an external registration — not as "a launched effect with cleanup".
- **Bridge:** `memory_leaks`/`lifecycle_leaks` for the consequence, linking to the performance
  Topic rather than teaching leak diagnosis.

**L11.2 `lesson_side_effect_publication` — `SideEffect`: Publishing to Non-Compose Code**

- **Objective:** publish a successfully composed value to something Compose does not manage.
- **Demonstrable reasoning:** say what an abandoned or skipped composition means for the
  publication, and why writing the same call in the body is a different operation.
- **Teach:** it runs after every successful composition that reached it; it is not a coroutine
  API, not a general event handler, and not a place for expensive work; why the same mutation
  written directly in the body can publish a value from a composition that was never applied.
- **Measured evidence:** the probe in the source section counted one `SideEffect` run per
  composition pass.

**L11.3 `lesson_produce_state` — `produceState`: a Composition-Scoped Producer**

- **Objective:** adapt an asynchronous or external source into Compose `State`.
- **Demonstrable reasoning:** predict what the returned `State` holds immediately after a key
  change but before the new producer has written anything.
- **Teach:** the initial value; the producer launched on entering and cancelled on leaving;
  restart keys; **conflation — an equal write is not a change**; the relationship to a launched
  effect writing remembered state, which is what it is built from.
- **Teach the measured trap:** on a key change the producer restarts but **the `State` is not
  reset to `initialValue`** — it keeps the last produced value until the new producer writes.
  This is a real production defect (the previous item's data shown under the new item's key)
  and it is not stated on the documentation page.
- **Boundary:** the internal implementation is useful as a *confirmation* of the contract, not
  as the primary mental model.

**L11.4 `lesson_flow_adapter_or_compose_producer` — A Flow Below the UI, or a Producer at the Boundary?**

- **Objective:** decide where an adapter for a callback-based source belongs.
- **Demonstrable reasoning:** given a source and its intended reuse, choose between a Flow below
  the UI and a Compose-local producer, and justify it from ownership and reuse rather than from
  API preference.
- **Teach:** `awaitDispose` and where it is genuinely needed — a non-suspending subscription
  inside a producer; the decision itself: reusable or non-Compose consumers, or a lifetime that
  is specifically a composition's.
- **Bridge:** `callbackFlow`/`awaitClose` named once as the other side of the comparison, with a
  link to `lesson_flow_builders_and_callback_adapters`. **This Lesson is not another Flow-builder
  Lesson.**

### Unit 12 — Production UI Effects and Mechanism Selection

**Purpose:** synthesis. No new API family. **Prerequisites:** Units 7–11; E24
`lesson_shared_flow` and `lesson_choosing_a_stream_abstraction`. **Three Lessons**: the
decision, the transient-effect mechanics, and the delivery question that the mechanics cannot
answer.

**L12.1 `lesson_choosing_a_compose_mechanism` — Choosing the Smallest Sufficient Mechanism**

- **Objective:** decide, from a stated requirement, which mechanism a screen needs.
- **Demonstrable reasoning:** for a scenario, name the mechanism *and* the four facts that
  chose it — who owns the state, who owns the trigger, what lifetime is required, what cleanup
  is required.
- **Teach:** the decision worked through realistic scenarios. Every outcome from the epic must
  be reachable: plain rendering; local state; hoisted state; collected external state;
  lifecycle-aware collected state; a composition-triggered coroutine; an event-triggered
  coroutine; a long-lived effect reading a current value; a registration with cleanup;
  post-composition publication; an external source adapted into `State`; and "none of these —
  this work has the wrong owner".
- **Boundary:** scenarios, not a table of API definitions.

**L12.2 `lesson_transient_ui_effects` — Rendering State and Running a Transient Effect**

- **Objective:** separate "what the screen is" from "something that should happen once".
- **Demonstrable reasoning:** given a snackbar, a focus request, a scroll or a navigation
  request, say which mechanism executes it and why the same thing modelled as state fires
  twice.
- **Teach:** the Compose side of a transient occurrence — which coroutine or effect lifetime
  runs it, why rendering and executing are different operations, and the concrete
  fires-again-after-recreation failure.
- **Exclude:** the producer architecture on the other side of the boundary.

**L12.3 `lesson_transient_effect_delivery` — What Delivery Guarantee Does This Occurrence Need?**

- **Objective:** ask the delivery question before choosing a mechanism.
- **Demonstrable reasoning:** for an occurrence, state what must be true when the UI is absent,
  and conclude whether any UI-side mechanism can satisfy it.
- **Teach:** the question — including when the UI is absent; that a successful emission is not
  a delivery; that if the answer needs persistence, acknowledgement, queueing or durable state,
  the problem has left the Compose boundary.
- **Teach, as a correction:** the slogan "`SharedFlow` is for events" is incomplete, and
  replacing it with "`Channel` is for events" is the same error. E24's
  `lesson_choosing_a_stream_abstraction` and `shared_flow_try_emit_true_is_not_delivery`
  already established this; Unit 12 applies it, links to it, and does not re-derive it.
- **Exclude:** application-level event architecture, durable-versus-transient event modelling,
  `Channel`-versus-`SharedFlow` as a ViewModel design decision — **E26**.

---

## Misconception and reasoning targets

Recorded as reasoning a learner must be able to demonstrate, not as slogans to recite. Each
entry names the Lesson responsible.

| Misconception | Corrected in | The reasoning that replaces it |
| --- | --- | --- |
| "All production Compose state belongs in a state holder or ViewModel." | L7.1 | Place three pieces of state on one screen and justify each from reads, writes and required lifetime; at least one correctly stays local |
| "Hoist state as high as possible." | L7.1 | Predict the parameter list, the state type and the invalidation cost of a screen whose local state has been pushed to the top |
| "A child may hold writable state or a state-holder reference." | L7.3 | Name the specific problems — a second write path, lost previewability, lost reuse — and rewrite the signature |
| "The screen state type is where the interesting design is." | L7.3, and deliberately left to E26 | Say what E25 settles (one immutable current value) and what it does not (how that value is modelled) |
| "Moving state outside the Composition makes it survive." | L7.4 | For a named action — recomposing, rotating, navigating forward, popping the destination — say whether the screen-level state survives, and name the owner lifetime that decided it rather than the move itself |
| "A `StateFlow` can drive a composable directly." | L8.1 | Predict what `flow.value` read in a body does when the flow changes, and explain it from what composition observes |
| "`collectAsState` and `collectAsStateWithLifecycle` have the same lifetime and cost." | L8.3, L8.4 | For a named target and a named user action, say whether collection stops |
| "`collectAsStateWithLifecycle` is Android-only." | L8.4 | Name the artifact, the source set, the package, and what supplies the `LifecycleOwner` on each target this project builds |
| "Stopping the UI collector stops the upstream." | L8.3 | Say what actually decides it, and where that decision is made |
| "Rendering ongoing state means launching a `LaunchedEffect` and collecting by hand." | L8.2 | Distinguish converting state from performing an effect, and say which one the requirement is |
| "A composable runs once." | L9.1 | Already corrected in E23; L9.1 extends it to *what that means for work you start* |
| "It is safe to start work in a composable body." | L9.1 | Name the three unanswered questions: how often, who stops it, what cleans it up |
| "Recomposition means re-run this side effect." | L9.2 | Distinguish what re-executes (the description) from what restarts (an effect whose keys changed) |
| "`LaunchedEffect(Unit)` means run once." | L9.3 | Restate it as a lifetime claim and test the claim against the requirement |
| "Every value the effect reads must be a key." | L9.3 | Decide by asking whether a change should end the current work |
| "A newly allocated key always causes a restart." | L9.4 | Predict correctly for a fresh-but-equal data class, a non-capturing lambda, a lambda capturing an unchanged value, and a lambda capturing a changing one |
| "Effect keys and skipping use the same comparison." | L9.3 | Name both comparisons and say which decides what |
| "`rememberUpdatedState` prevents recomposition." | L10.1 | Say what it actually does and what it costs, and contrast it with keying the effect |
| "`rememberCoroutineScope` starts a coroutine." | L10.2 | Say what it returns and who decides when work begins |
| "`rememberCoroutineScope` gives work a lifetime beyond the screen." | L10.2, L10.3 | Say when the scope is cancelled and where work that must outlive the screen belongs |
| "`DisposableEffect` is `LaunchedEffect` with cleanup." | L11.1 | Say what each is *for*, and why a key change disposes as well as restarts |
| "`SideEffect` is a general event callback." | L11.2 | Say when it runs relative to a successful composition, and what an abandoned composition means |
| "`produceState` replaces ordinary stream collection." | L11.3, L11.4 | Decide from ownership and reuse which side of the UI boundary the adapter belongs on |
| "`produceState` resets to its initial value when a key changes." | L11.3 | Predict the value held immediately after a key change — a measured result, not folklore |
| "`SharedFlow` is the standard answer for one-off UI events." | L12.3 | Ask what guarantee the occurrence needs, including when the UI is absent |
| "A successful emission means the effect was delivered." | L12.3 | Say what `emit` and `tryEmit` actually promise, and what would have to exist for delivery to be guaranteed |

---

## Source-sensitive and platform-sensitive claims

Every claim below was settled on 2026-09-11 against this repository's resolved artifacts or
against current authoritative documentation, and the two are distinguished wherever they
differ. Where a claim was **measured**, the probe is described and its result recorded;
contractual API behaviour and measured behaviour are labelled separately, following the
precedent E23 and E24 set.

**Probes.** Three throwaway `runComposeUiTest` classes were added under
`shared/src/jvmTest/`, run with
`./gradlew :shared:jvmTest --tests "org.artkachenko.kmp_learning_app.E25ProbeTemp*"`, and
**deleted**. They measure this project's JVM target on the resolved
`androidx.compose.runtime:runtime:1.11.2`. They are authoring validation, not curriculum, and
not tests this repository keeps.

### Lifecycle-aware collection in this repository

**This is the acceptance-critical finding of E25-01.**

**Finding: `collectAsStateWithLifecycle` is available to this repository's `commonMain`, on
every target it builds, and it is already used there in production code.**

Evidence, in order of strength:

1. **Declaration site.** The resolved
   `androidx.lifecycle:lifecycle-runtime-compose:2.11.0-beta01` sources jar declares all four
   `collectAsStateWithLifecycle` overloads in
   `commonMain/androidx/lifecycle/compose/FlowExt.kt`, package **`androidx.lifecycle.compose`**.
   There is no `androidMain` variant and no `expect`/`actual`. The JetBrains artifact
   `org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose` that `shared/build.gradle.kts`
   declares is a thin alias whose own jar contains only a `kotlin_module`; it depends on the
   AndroidX artifact above, which carries the implementation.
2. **Production usage.** `collectAsStateWithLifecycle` is imported from
   `androidx.lifecycle.compose` and used in **14 files, all under `shared/src/commonMain`** —
   every destination composable in the app.
3. **Compilation, verified per target on 2026-09-11.** `:shared:compileAndroidMain`,
   `:shared:compileKotlinJs`, `:shared:compileKotlinWasmJs` and
   `:shared:compileKotlinIosSimulatorArm64` all succeeded; the JVM target is covered by
   `:shared:jvmTest`. No new probe code was needed — the existing `commonMain` usage is the
   probe. `iosArm64` was not compiled locally and is not built on CI; it shares `iosMain` with
   `iosSimulatorArm64` and the dependency is published for it, so availability is established
   by the published artifact rather than by a local compile. **E25-03 must not claim an
   `iosArm64` compile that was not run.**

**Contract, from the resolved source.**

- Four overloads: `StateFlow<T>.collectAsStateWithLifecycle(lifecycleOwner, minActiveState,
  context)`, the same taking a `Lifecycle`, and two `Flow<T>` overloads that additionally take
  `initialValue`.
- `lifecycleOwner` defaults to `LocalLifecycleOwner.current`.
- `minActiveState` defaults to **`Lifecycle.State.STARTED`**.
- The KDoc states: "Warning: `Lifecycle.State.INITIALIZED` is not allowed in this API. Passing
  it as a parameter will throw an `IllegalArgumentException`."
- The `StateFlow` overloads use `this.value` as the initial value.
- The implementation is `produceState(initialValue, this, lifecycle, minActiveState, context) {
  lifecycle.repeatOnLifecycle(minActiveState) { collect { value = it } } }`. It therefore
  inherits `produceState`'s conflation and its keys, and `repeatOnLifecycle`'s start/stop
  behaviour. **Both inherited contracts are teachable in L8.4 and both are verifiable from the
  source rather than inferred.**

**What supplies the `LifecycleOwner`.** `androidx.lifecycle.compose.LocalLifecycleOwner` is
`expect` in `commonMain`; the `nonAndroidMain` actual is a `staticCompositionLocalOf` that
**errors when it has not been provided**. It is provided by Compose Multiplatform itself:
`ui-desktop` / `ui-iosarm64` / `ui-wasm-js` 1.11.1 `skikoMain/.../CompositionLocals.skiko.kt`
provides it from a `PlatformArchitectureComponentsOwner`, whose default implementation owns a
`LifecycleRegistry`. JetBrains' documentation states the same thing in words: "Compose
Multiplatform provides a common `LifecycleOwner` implementation, which extends the original
Jetpack Compose functionality to other platforms."

**How the lifecycle state is actually driven, per target.** Read from the Compose Multiplatform
1.11.1 sources and cross-checked against JetBrains' lifecycle documentation, which describes the
same mappings.

| Target | What moves the lifecycle | Consequence for `minActiveState = STARTED` |
| --- | --- | --- |
| Android | The host `LifecycleOwner` — Activity, Fragment, or navigation back-stack entry | The familiar behaviour; collection stops when the host stops |
| Desktop (JVM/Swing) | `ComposeContainer.updateLifecycleState()`: disposed → `DESTROYED`; detached **or minimized** → `CREATED`; attached, not minimized **and focused** → `RESUMED`; otherwise → `STARTED` | A visible but **unfocused** window is `STARTED`, so collection continues. **Minimizing** drops below `STARTED` and collection stops. Focus alone does not stop it |
| iOS | `ComposeContainerLifecycleDelegate.updateLifecycleState()`: disposed → `DESTROYED`; view appeared + scene foreground + scene active → `RESUMED`; view appeared + foreground + inactive → `STARTED`; otherwise → `CREATED` | Backgrounding the app drops below `STARTED`; an inactive-but-foreground scene stays `STARTED` |
| Web (js / wasmJs) | `ComposeWindow` maps DOM events: `focus` → `ON_RESUME`, `blur` → `ON_PAUSE`, `visibilitychange` → `ON_START`/`ON_STOP`; a touch forces `RESUMED` because iOS Safari does not fire focus | A **hidden tab** stops collection; a merely blurred but visible page stays `STARTED`. JetBrains documents two further limits: web lifecycles **skip `CREATED`** and **never reach `DESTROYED`** |

**A desktop-specific dependency note.** JetBrains' documentation states that
`Lifecycle.coroutineScope` is tied to `Dispatchers.Main.immediate`, "which might be unavailable
on desktop targets by default" without `kotlinx-coroutines-swing`. This repository declares
`libs.kotlinx.coroutinesSwing` in `desktopApp/build.gradle.kts`, so the condition is satisfied
here. L8.4 should mention this as a platform note rather than as a Compose API property.

**What E25-03 may and may not say.**

- It **may** say the API is available in shared code here, name the package and the artifact,
  and give the per-target table above.
- It **must not** say "`collectAsStateWithLifecycle` is Android-only" — false.
- It **must not** say "the dependency is in `commonMain`, so the behaviour is identical
  everywhere" — also false, as the table shows.
- It **must** state the version it verified against, because `2.11.0-beta01` is a beta and the
  JetBrains documentation page currently names `2.11.0`.

### Effect keys: what the comparison actually is

The epic requires E25 to state that effect keys are compared in the same general way as a
remembered calculation's keys, and to distinguish that from skipping. Both halves were verified
against the resolved runtime rather than recalled.

**Contractual, from `androidx.compose.runtime:runtime:1.11.2` sources:**

- `LaunchedEffect(key1) { ... }` is implemented as
  `remember(key1) { LaunchedEffectImpl(applyContext, block) }`. `DisposableEffect(key1) { ... }`
  is `remember(key1) { DisposableEffectImpl(effect) }`. The effect key list **is** a
  `remember` key list; this is not an analogy.
- `remember(key1, calculation)` is `currentComposer.cache(currentComposer.changed(key1),
  calculation)`, and its KDoc says the value is returned again "if `key1` compares equal (`==`)
  to the value it had in the previous composition".
- `Composer.changed(value)` is documented as returning "`true` if the value if `equals` of the
  previous value returns `false`". `Composer.changedInstance(value)` is the separate API that
  compares "using `===` instead of `==` equality... for values that use value equality but, for
  correct behavior, the composer needs reference equality" — this is the comparison behind
  skipping decisions for unstable parameters, which E23's
  `compose_strong_skipping_instance_equality` already assesses.
- `LaunchedEffectImpl.onForgotten()` cancels the job with an internal
  `LeftCompositionCancellationException`, and `onRemembered()` launches it. Cancellation is
  ordinary coroutine cancellation, with all of E24's cooperative-cancellation caveats intact.
- The `LaunchedEffect` KDoc itself contains the pointer Unit 10 needs: "This function should
  **not** be used to (re-)launch ongoing tasks in response to callback events... Instead, see
  `rememberCoroutineScope`."

**Measured (PROBE-A and PROBE-E), JVM target, four composition passes each:**

| Key supplied on every pass | Effect starts | Reading |
| --- | --- | --- |
| `ItemKey("same")` — a fresh `data class` instance, structurally equal | **1** | A new object is **not** a changed key. `equals` decides |
| `{ }` — a lambda capturing nothing | **1** | Non-capturing lambdas are singletons |
| `{ println(fixed) }` — a lambda capturing an unchanging local | **1** | The Compose compiler memoised it, so the same instance is supplied each pass |
| `remember { { println(fixed) } }` — an explicitly remembered lambda | **1** | Same instance every pass |
| `{ println(t) }` — a lambda capturing a value that changes | **4** | A genuinely new instance every pass, so the effect restarts every pass |
| `Unit` | **1** | The constant-key baseline |

`DisposableEffect(ItemKey("same"))` produced **0** disposals across the same four passes,
confirming the same comparison. `SideEffect` ran **4** times — once per composition pass.

**What E25-04 must take from this.** "A freshly created lambda restarts the effect" is true
often enough to be useful and wrong often enough to be dangerous: what restarts an effect is a
key that stops comparing equal, and under strong skipping the compiler memoises lambdas whose
captures have not changed. The Lesson should teach the comparison and derive the lambda cases
from it, not teach the lambda cases as a rule. This is measured behaviour of this project's
toolchain layered on a contractual comparison; both labels belong in the Lesson.

### `collectAsState`

From `androidx.compose.runtime:runtime:1.11.2` `SnapshotFlow.kt`:

- `StateFlow<T>.collectAsState(context)` delegates to `collectAsState(value, context)`, so the
  `StateFlow`'s current value is the initial value.
- `Flow<T>.collectAsState(initial, context)` is
  `produceState(initial, this, context) { collect { value = it } }`, or the same inside
  `withContext(context)` when a context is supplied. **The keys are `initial`, the flow
  instance and the context** — a new flow instance on each recomposition restarts the
  collection.
- Because it is `produceState`, the returned `State` conflates: an equal value is not a change.

**Measured (PROBE-D):** at first composition a `MutableStateFlow("sf-initial")` was already
observed as `sf-initial`, and a cold `flow { emit("cold-first") }` was observed as its supplied
`given-initial`. This is the behavioural half of the "a plain `Flow` has no current value"
argument L8.2 needs.

### `produceState` and `awaitDispose`

From `ProduceState.kt` in the same artifact:

- `produceState(initialValue, key1) { ... }` is
  `remember { mutableStateOf(initialValue) }` plus `LaunchedEffect(key1) { ... }`.
- The KDoc states: "The returned `State` conflates values; no change will be observable if
  `ProduceStateScope.value` is used to set a value that is equal to its old value, and
  observers may only see the latest value if several values are set in rapid succession."
- `ProduceStateScope.awaitDispose(onDispose)` returns `Nothing` and is implemented as
  `suspendCancellableCoroutine<Nothing> {}` inside a `try`/`finally` that always runs
  `onDispose`. Its documented purpose is "configuring callback-based state producers that do
  not suspend". It is available in `commonMain` of the Compose runtime, so it is available on
  every target here.

**Measured (PROBE-F):** a producer keyed on `key` restarted when the key changed (2 starts),
and the returned `State` **kept the value produced under the old key** rather than reverting to
`"INITIAL"`. This follows directly from the unkeyed `remember { mutableStateOf(initialValue) }`
in the implementation, and it is **not stated on the Android documentation page**. It is the
single most useful Senior-depth fact in Unit 11 and it must be labelled as read from the
resolved source and confirmed by measurement.

**Measured (PROBE-G):** three consecutive writes of the same value produced one observable
change; two composition passes read the state in total.

### `rememberUpdatedState`

From `SnapshotState.kt`: `rememberUpdatedState(newValue)` is
`remember { mutableStateOf(newValue) }.apply { value = newValue }`. Its KDoc is explicit that
it exists so that "recomposition will update the resulting `State` without recreating the
long-lived lambda or object, allowing that object to persist without cancelling and
resubscribing, or relaunching a long-lived operation".

**Measured (PROBE-B):** a `LaunchedEffect(Unit)` started **once** and, read three times as the
wrapped value changed `0 → 1 → 2`, saw `[0, 1, 2]`. The effect's lifetime and the value's
currency are independent, which is the Lesson's whole point.

**The correction E25-05 must make:** because the underlying `MutableState` uses structural
equality, a *changed* value written by `rememberUpdatedState` invalidates whatever read it
during composition. It does not reduce recomposition, and nothing about it is an optimisation.
What makes the effect immune is that the effect reads `.value` inside a coroutine rather than
during composition.

### `rememberCoroutineScope`

From `Effects.kt`: `rememberCoroutineScope` is `remember { createCompositionCoroutineScope(...) }`.
The returned `RememberedCoroutineScope` is a `RememberObserver` whose `onForgotten`/`onAbandoned`
cancel it with a `ForgottenCoroutineScopeException`. The KDoc states the two facts Unit 10
needs: "This scope will be cancelled when this call leaves the composition", and "Jobs should
never be launched into **any** coroutine scope as a side effect of composition itself." Supplying
a context containing a `Job` does not throw; it returns a scope whose `Job` has already failed
with `IllegalArgumentException`, so nothing can be launched into it.

### `SideEffect` and `DisposableEffect`

`SideEffect` is `currentComposer.recordSideEffect(effect)`. Its KDoc: effects "will always be
run on the composition's apply dispatcher", are "always run after `RememberObserver` event
callbacks", and "A `SideEffect` runs after **every** recomposition." The Android documentation
adds the contrast Unit 11 needs: "it is incorrect to perform an effect before a successful
recomposition is guaranteed, which is the case when writing the effect directly in a
composable."

`DisposableEffect`'s KDoc states: "There is guaranteed to be one call to `onDispose` for every
call to `effect`", and calling it without a key is a `DeprecationLevel.ERROR` overload.

### Documentation that is authoritative, and where it stops

| Source | Used for | Limitation found |
| --- | --- | --- |
| [Side-effects in Compose](https://developer.android.com/develop/ui/compose/side-effects) | Every effect API's stated purpose; the restart-keys section; the constant-key "think twice" framing; `produceState`'s conflation | Does **not** state the key comparison contract, and does **not** state that `produceState` keeps its last value across a key change. Both come from the runtime source |
| Compose runtime sources (`androidx.compose.runtime:runtime:1.11.2`) | The comparison contract, cancellation exceptions, the implementations of `collectAsState`, `produceState`, `rememberUpdatedState`, `rememberCoroutineScope` | The resolved artifact is 1.11.2 while the catalogue declares Compose Multiplatform 1.11.1. Cite what is resolved |
| [Lifecycle in Compose Multiplatform](https://kotlinlang.org/docs/multiplatform/compose-lifecycle.html) (JetBrains) | The common `LifecycleOwner`; per-platform state mappings; the web `CREATED`/`DESTROYED` limits; the desktop `Dispatchers.Main.immediate` note | Does **not** mention `collectAsStateWithLifecycle` at all, and names artifact version `2.11.0` where this repository resolves `2.11.0-beta01` |
| `androidx.lifecycle:lifecycle-runtime-compose:2.11.0-beta01` sources | The declaration site, package, overloads, `minActiveState` default, the `INITIALIZED` rejection, the `repeatOnLifecycle` implementation | This is the decisive source for the availability question |
| Compose Multiplatform `ui` sources 1.11.1 (`skikoMain`, `desktopMain`, `iosMain`, `webMain`) | Who provides `LocalLifecycleOwner` and what drives the lifecycle per target | Internal API; cite the documented mapping for the Lesson and use the source as the verification |
| [Kotlin flows on Android / Compose collection](https://developer.android.com/topic/libraries/architecture/coroutines) | The current recommendation to use `collectAsStateWithLifecycle`, and the `STARTED` default | Android-framed throughout; it never addresses non-Android targets, which is exactly the gap this plan fills |

**Freshness.** Compose and lifecycle guidance is on the question bank's freshness watch list,
and `androidx-lifecycle = "2.11.0-beta01"` is a beta. Every authoring issue should re-check the
pages it cites and record any change here rather than silently citing a rewritten page.

---

## Taxonomy audit

No taxonomy entry is created by this issue. Every id below is a real, `ACTIVE` id read from
`initial_curriculum.json`.

### Primary against supporting

E25 crosses more Topics than any previous Compose epic, so the rule matters more here: a
concept a Lesson *mentions* is supporting, and only a concept the Lesson is responsible for
teaching thoroughly is primary. In particular:

- **`stateflow`, `sharedflow`, `flow_collection`, `flow_sharing`, `hot_vs_cold_streams`,
  `flow_fundamentals` are supporting everywhere in E25.** Making any of them primary would put
  E24's Questions — which assess E24's reasoning — into an E25 Unit's practice. They are
  bridges, not responsibilities.
- **`coroutine_scope`, `coroutine_cancellation`, `coroutine_builders`,
  `structured_concurrency`, `lifecycle_coroutines` are supporting everywhere.** Same reason.
- **`state_ownership`, `unidirectional_data_flow`, `separation_of_concerns` are supporting
  everywhere.** These are the architecture Topic's, and E26 will need them.
- **`viewmodel_lifecycle`, `saved_state`, `configuration_changes`, `lifecycle_aware_apis` are
  supporting everywhere.** `lifecycle_aware_apis`'s only ACTIVE Question,
  `lifecycle_repeat_on_lifecycle`, is a Fragment question; it must not become Unit 8 practice.
- **`kmp_lifecycle_viewmodel` and `compose_multiplatform` are supporting.**
  `kmp_shared_viewmodel_owner_platform` is excellent context for L7.4 and is the KMP Topic's to
  assess.

### The `compose_side_effects` overlap

**`compose_side_effects` is the only primary concept of Units 9, 10, 11 and 12.** Because
`PracticeTargetResolver` resolves a Unit's practice scope as the deduplicated
`primarySubtopicIds` of its ACTIVE Lessons, **all four Units receive an identical practice
pool.** This is the same structural issue E24 hit with `coroutine_parallelism`, and it is
recorded here so E25-08 meets it in planning rather than in authoring.

**Current state of that pool (2 ACTIVE Questions):**

| Question | Level | Reasoning | Which Unit actually teaches it |
| --- | --- | --- | --- |
| `compose_side_effects_001` | FOUNDATION, MULTIPLE | Four separate claims: `LaunchedEffect` is composition-scoped and keyed; `DisposableEffect` cleans up on leaving or key change; `SideEffect` runs after, not before, a successful composition; `rememberCoroutineScope` does not launch | Units **9, 10 and 11** between them |
| `compose_launched_effect_key_restart` | FOUNDATION | A key change cancels and relaunches | Unit **9** |

**The concrete consequence.** A learner who finishes Unit 9 and presses "Practice this
material" can be asked `compose_side_effects_001`, two of whose four claims belong to Units 10
and 11. That is practice containing reasoning the Unit has not taught. It is not a defect in
the Question — it is a correct Question about the API family — and it cannot be fixed by
authoring more Questions on the same Subtopic, because every one of them lands in all four
pools too.

**What was considered and rejected.**

- *Merging Units 9–12 into fewer Units* so the pools stop overlapping. Rejected: taxonomy
  granularity must not drive pedagogy, and the authoring contract says so explicitly.
- *Giving Units 10, 11 and 12 a different primary concept* — `compose_state` for
  `rememberUpdatedState`, `compose_recomposition` for `SideEffect`. Rejected as dishonest: none
  of those Units is responsible for teaching those concepts, and each would import a pool of
  E23 Questions that is worse than the overlap it removes.
- *Inventing finer-grained Subtopics* (`compose_effect_keys`, `compose_state_producers`, …).
  Rejected: E25-01 does not change the taxonomy, and inventing ids would be exactly the
  fictional precision the authoring contract forbids. It is recorded as a taxonomy gap below.

**What E25-08 should do instead.**

1. Prefer Questions answerable from the **earliest** Unit that teaches the reasoning, so a pool
   that reaches four Units is mostly answerable in the first of them.
2. Where a Question genuinely requires later material — mechanism selection across the whole
   family is the obvious case — record it, because that is Unit 12's reasoning appearing in
   Unit 9's practice by construction.
3. Consider whether `compose_side_effects_001` should be superseded. It is a four-claim
   recognition Question spanning three Units, which is exactly the shape that makes the overlap
   visible. Superseding it means deprecating and adding, which changes identity; **E25-08 owns
   that decision deliberately**, and "leave it" is a defensible outcome.
4. Do **not** treat the overlap as a bug to be engineered away in E25.

### Taxonomy gaps

Recorded, not invented. These are added to the blueprint's taxonomy-gap table in the same
change.

| Concept | Nearest existing Subtopic | Note |
| --- | --- | --- |
| Converting an external observable stream into Compose `State` | `compose_state` | The nearest fit and the one this plan uses, but `compose_state`'s ACTIVE Questions are all E23's. This is why Unit 8's practice pool assesses none of what Unit 8 teaches — see GAP-U8-A |
| Effect lifetime and effect-key semantics | `compose_side_effects` | Distinct reasoning from the API inventory the Subtopic name suggests, and the reason four Units share one pool |
| Composition-scoped state producers (`produceState`, `awaitDispose`) | `compose_side_effects` | Same Subtopic, different mental model |
| Mechanism selection across the effect family | `compose_side_effects` | A synthesis concept with no Subtopic of its own anywhere |
| Compose-side handling of a transient UI occurrence | `compose_side_effects`, with `state_ownership` and `sharedflow` adjacent | The delivery half is assessed in `async_reactive` and `architecture`; the Compose-side half has no home |

**Whether any of these should become a Subtopic is a separate decision for a future
question-bank change**, and a strong candidate given that four Units now share one concept.
E25 does not make it.

---

## Semantic assessment review

Every ACTIVE Question on the Subtopics this plan maps as primary was read in full — stem,
options, correct set, explanation and Sources — and so were the cross-Topic Questions that
assess adjacent reasoning. **Reading, not counting.** DEPRECATED Questions on the same
Subtopics were read as context, because a deprecated Question still occupies its concept.

Nothing below claims that an unpublished Lesson makes a Question answerable.

**Primary-concept pools as they stand:**

| Unit | Primary concepts | ACTIVE Questions reached |
| --- | --- | --- |
| 7 | `compose_state_hoisting`, `compose_udf` | `compose_state_hoisting_001`, `compose_udf_event_direction` |
| 8 | `compose_state` | `remember_vs_remember_saveable`, `compose_unremembered_observable_state`, `compose_state_collection_mutation` |
| 9–12 | `compose_side_effects` | `compose_side_effects_001`, `compose_launched_effect_key_restart` |

**Verdicts:**

| Question | Verdict against the planned Lessons |
| --- | --- |
| `compose_state_hoisting_001` (FOUNDATION) | Correctly mapped and correctly levelled. Answerable from the **shipped** `lesson_state_hoisting`, not from Unit 7 — its key is the definition of hoisting and its strongest distractor is the ViewModel misconception E23 already corrects. It gives Unit 7 a practice item, but it assesses Unit 2's reasoning. Unit 7's own reasoning is unassessed: GAP-U7-A |
| `compose_udf_event_direction` (APPLIED) | Correctly mapped and genuinely useful to Unit 7 L7.3: the key is a callback parameter and the distractors are a shared mutable state object and a ViewModel reference — exactly the "writable state travelling down" failure L7.3 owns. The closest thing to real Unit 7 coverage that exists |
| `remember_vs_remember_saveable` (FOUNDATION) | E23 Unit 2's reasoning. Reaches Unit 8 through `compose_state` and assesses nothing Unit 8 teaches |
| `compose_unremembered_observable_state` (APPLIED) | E23 Unit 2's reasoning. Same |
| `compose_state_collection_mutation` (APPLIED) | E23 Unit 2's reasoning. Same. **Unit 8 has no Question that assesses conversion, collection lifetime or lifecycle awareness** — the widest single gap in the epic |
| `compose_side_effects_001` (FOUNDATION, MULTIPLE) | Correct, and correctly mapped, but it is a four-claim recognition Question spanning Units 9, 10 and 11. See [the overlap](#the-compose_side_effects-overlap) |
| `compose_launched_effect_key_restart` (FOUNDATION) | Correct, correctly mapped, correctly levelled; Unit 9 L9.2's reasoning exactly. Assesses the *restart* direction of key errors only, which leaves L9.4's other direction unassessed |

**Cross-Topic Questions read for duplication and context, none of which is E25 Unit practice:**

| Question | Topic / Subtopic | Why it matters to E25 |
| --- | --- | --- |
| `compose_work_placement_responsibility_vs_thread` | android_ui / `compose_derived_state` | Assesses placement as responsibility and the "a ViewModel moves work off the main thread" misconception. It is E23 Unit 5's and stays there; Unit 7 L7.4 and Unit 9 L9.1 should link to it rather than reassess it |
| `compose_snapshot_flow_state`, `compose_snapshot_flow_read_inside_block`, `compose_snapshot_flow_conflated_state` | android_ui / `compose_snapshot_system` | `snapshotFlow` is E23's. The first names `produceState` as a distractor and the second uses `LaunchedEffect` as scenery; neither is E25 practice and neither should be re-mapped |
| `compose_strong_skipping_instance_equality` | android_ui / `compose_stability` | The instance-comparison half of L9.3's contrast, already assessed. L9.3 links rather than reassesses |
| `compose_remember_key_invalidation` | android_ui / `compose_derived_state` | The `remember(key)` half of the same contrast, already assessed at APPLIED depth. **This is the single best reason not to reassess key equality generically in E25-08** — what is missing is the *effect* consequence, not the comparison |
| `compose_call_site_identity_branches` | android_ui / `compose_identity_keys` | Call-site identity and what leaving composition destroys — the basis of L9.2's effect-lifetime argument |
| `architecture_state_holder_taxonomy`, `durable_state_vs_one_off_event`, `viewmodel_activity_reference_lifetime` | architecture / `state_ownership` | Screen-level against UI-element holders, and durable state against consumable behaviour. Supporting-only in E25, so no Unit practice. L7.1, L7.4 and L12.3 should link to them and must not restate them as Compose reasoning |
| `architecture_ui_event_consumption` | architecture / `mvi` | A replayed one-time navigation event firing twice after a configuration change — the exact failure L12.2 demonstrates, assessed from the architecture side. Unit 12 must link, not duplicate |
| `stream_choice_cannot_supply_a_delivery_guarantee`, `shared_flow_try_emit_true_is_not_delivery`, `shared_flow_replay_late_subscriber` | async_reactive / `hot_vs_cold_streams`, `sharedflow` | E24 already assesses the delivery argument thoroughly. L12.3 applies it at the Compose boundary and must not re-derive it |
| `lifecycle_repeat_on_lifecycle`, `fragment_view_lifecycle_collection`, `live_data_vs_state_flow_ui_state` | lifecycle_navigation, async_reactive | The View-host half of lifecycle-aware collection. Accurate, and **out of E25's scope**: E25 teaches the Compose boundary, not Fragment collection. Supporting-only; must not be treated as Unit 8 practice |
| `viewmodel_scope_cleared_cancellation`, `coroutine_scope_outlives_its_consumer` | async_reactive | Already assess "work outlives its consumer" from the coroutine side, which is where L10.3 hands off |
| `callback_flow_await_close_registration` | async_reactive / `flow_fundamentals` | The `callbackFlow`/`awaitClose` half of L11.4's comparison, already assessed. L11.4 assesses the *placement decision*, not the builder |
| `lifecycle_leak_listener_unregistration`, `context_leak_retained_activity_references`, `gc_does_not_prevent_reachable_leaks` | performance | The consequence of a missing unregistration, assessed from the performance side and in Fragment/View terms. L11.1 needs the Compose-side version; see GAP-U11-A |
| `kmp_shared_viewmodel_owner_platform` | kmp / `kmp_lifecycle_viewmodel` | That the ViewModel class is multiplatform but the *owner* is a platform host — precisely L7.4's boundary, assessed by the KMP Topic. Link |

**DEPRECATED Questions inspected before declaring a gap:** `compose_state_001`,
`compose_recomposition_001`, `compose_stability_001`,
`compose_skipping_stable_parameter_contract`, `compose_lazy_layouts_001`. None of them assesses
any effect, collection or screen-state reasoning. **No deprecated Question occupies an E25
concept**, so none of the gaps below is a revival candidate.

---

## Assessment gaps for E25-08

Substantive gaps only. There is no per-Lesson or per-level quota, and a gap here is a candidate
for authoring rather than an automatic defect. Each names the reasoning that is missing.

Every gap on Units 9–12 inherits the overlap described above: a Question written for any of them
appears in all four pools.

| Gap | Unit / Lesson | Reasoning no ACTIVE Question assesses | Why it is substantive | Recommended action |
| --- | --- | --- | --- | --- |
| GAP-U7-A | 7 / `lesson_classes_of_screen_state` | Placing several pieces of state on **one** screen and justifying each — including one that correctly stays local | `compose_state_hoisting_001` assesses the definition of hoisting, one piece at a time. The screen-level decision Unit 7 exists for is unassessed | Add coverage in E25-08 |
| GAP-U7-B | 7 / `lesson_classes_of_screen_state` | The cost of over-hoisting, made concrete | `compose_recomposition_performance_001` uses "hoist everything to the top" as a distractor in the `performance` Topic, which is supporting-only here and creates no Unit 7 practice | Add coverage in E25-08 |
| GAP-U7-C | 7 / `lesson_screen_state_owner_boundary` | What changes, and what does not, when a piece of state's owner moves outside the Composition | `architecture_state_holder_taxonomy` assesses the choice from the architecture side; nothing assesses the Compose-side boundary | Add coverage in E25-08 if capacity allows; overlaps E26's territory and may be better left to it |
| **GAP-U8-A** | 8 / `lesson_external_state_in_compose`, `lesson_collect_as_state` | That an external stream must become Compose `State` before a composable can render it, and what the conversion produces at first composition | **Nothing anywhere in the bank assesses this.** `compose_state`'s three Questions are all E23's, so Unit 8 currently has a practice pool that teaches none of its own material. **The widest gap in the epic** | Add coverage in E25-08; highest priority |
| GAP-U8-B | 8 / `lesson_lifecycle_aware_collection` | Choosing between plain and lifecycle-aware collection from a stated requirement, and predicting when collection stops | `lifecycle_repeat_on_lifecycle` assesses the Fragment equivalent in another Topic. The Compose decision is unassessed, and the multiplatform half is unassessed anywhere | Add coverage in E25-08; strong candidate, and the natural home for the per-target behaviour this plan established |
| GAP-U8-C | 8 / `lesson_collection_lifetime_and_cost` | That stopping the UI collector does not by itself stop upstream production | `flow_state_in_while_subscribed` assesses the timeout parameter from the sharing side; the consequence at the collection site is unassessed | Add coverage in E25-08 |
| GAP-U9-A | 9 / `lesson_why_effects_are_controlled` | The concrete failure of work started directly in a composable body | `compose_work_placement_responsibility_vs_thread` is the closest and is mapped to `compose_derived_state`, so it creates no Unit 9 practice | Add coverage in E25-08 |
| GAP-U9-B | 9 / `lesson_effect_key_failures` | That a freshly allocated but structurally equal key does **not** restart an effect, while a lambda that stops comparing equal does | `compose_remember_key_invalidation` assesses the comparison for a remembered calculation; nothing assesses the effect consequence, which is where the restart-storm bug lives | Add coverage in E25-08; the strongest APPLIED candidate in Unit 9, and directly supported by the measurements in this plan |
| GAP-U9-C | 9 / `lesson_effect_key_failures` | Too few keys — stale ids and work that continues under the wrong identity | `compose_launched_effect_key_restart` assesses the correct-restart direction only | Add coverage in E25-08 |
| GAP-U9-D | 9 / `lesson_effect_keys_as_dependencies` | A constant key read as a lifetime claim rather than as "run once" | Unassessed. It is the single most common effect misuse | Add coverage in E25-08 |
| GAP-U10-A | 10 / `lesson_remember_updated_state` | `rememberUpdatedState` at all — the stale-callback failure and the keyed alternative's restart cost | **Nothing in the bank names it**, including as a distractor | Add coverage in E25-08; highest priority in Unit 10 |
| GAP-U10-B | 10 / `lesson_who_owns_the_trigger` | Choosing between a composition-triggered effect and a remembered scope from who owns the trigger | `compose_side_effects_001` has one clause saying `rememberCoroutineScope` does not launch. Recognition, not a decision | Add coverage in E25-08 |
| GAP-U10-C | 10 / `lesson_remember_coroutine_scope` | The lifetime of work launched from a remembered scope, and that work which must outlive the screen belongs elsewhere | `coroutine_scope_outlives_its_consumer` and `viewmodel_scope_cleared_cancellation` assess the coroutine side in `async_reactive`; the Compose-scope version is unassessed | Add coverage in E25-08 |
| GAP-U11-A | 11 / `lesson_disposable_effect` | A missing `onDispose` in a Compose screen, and what accumulates | `lifecycle_leak_listener_unregistration` is Fragment-based and sits in `performance`, so it creates no Unit 11 practice | Add coverage in E25-08 |
| GAP-U11-B | 11 / `lesson_side_effect_publication` | `SideEffect`'s timing as a decision — what an abandoned or skipped composition means for a publication | Present only as a one-clause distractor in `compose_side_effects_001` | Add coverage in E25-08 |
| GAP-U11-C | 11 / `lesson_produce_state` | `produceState` at all: restart keys, conflation, and that the `State` is **not** reset on a key change | **Unassessed**; `produceState` appears only as a distractor in `compose_snapshot_flow_state`. The key-change behaviour is a measured, documented-nowhere fact | Add coverage in E25-08; the strongest ADVANCED candidate in the epic |
| GAP-U11-D | 11 / `lesson_flow_adapter_or_compose_producer` | Choosing between a Flow below the UI and a Compose-local producer from ownership and reuse | `callback_flow_await_close_registration` assesses the builder contract in `async_reactive`; the placement decision is unassessed | Add coverage in E25-08 if capacity allows |
| GAP-U12-A | 12 / `lesson_choosing_a_compose_mechanism` | Choosing a mechanism from a stated requirement across the whole family | Unassessed. It is the reasoning the epic exists to establish | Add coverage in E25-08; highest priority in Unit 12 |
| GAP-U12-B | 12 / `lesson_transient_ui_effects` | The Compose-side distinction between rendering current state and executing a transient effect | `durable_state_vs_one_off_event` and `architecture_ui_event_consumption` assess the architecture side and are supporting-only here | Add coverage in E25-08 |
| GAP-U12-C | 12 / `lesson_transient_effect_delivery` | What a transient occurrence requires **when the UI is absent**, posed from the Compose side | E24 assesses the emitter's half thoroughly (`shared_flow_try_emit_true_is_not_delivery`, `stream_choice_cannot_supply_a_delivery_guarantee`). The consumer-side question is unassessed, and answering it must not duplicate E24 | Add coverage in E25-08; write it as a *decision*, not as a restatement |

**Two observations that are not gaps and must not be treated as quotas.**

- `android_ui` currently holds **one** ACTIVE `ADVANCED` Question
  (`compose_snapshot_flow_conflated_state`). E25 carries several claims that would justify
  Advanced material on merit — `produceState`'s key-change value, the effect-key comparison,
  and the delivery question — but the level must follow the reasoning, not the count.
- Every planned Lesson maps to a Subtopic that has at least one ACTIVE Question, which is
  exactly why the review above was read rather than counted. By reasoning rather than
  structure, **3 of 22** planned Lessons have an ACTIVE Question that assesses what this plan
  places in them. That is a much thinner starting position than E24's, and it is the honest
  measure of how little of this territory the bank currently covers.

### Mapping, level and defect candidates

Candidate edits, not new Questions. **E25-08 decides on each deliberately**; a re-map changes
which Unit's practice a Question appears in, and preserving Question identity is a lifecycle
requirement. No Question is changed by E25-01.

| Item | Current | Finding | Recommendation |
| --- | --- | --- | --- |
| `compose_side_effects_001` | `compose_side_effects`, FOUNDATION, MULTIPLE | Correct and well-sourced, but it spans three Units and lands in four practice pools. Its `rememberCoroutineScope` and `SideEffect` clauses are unanswerable after Unit 9 alone | Consider superseding it with narrower Questions in E25-08. Leaving it is defensible; doing so silently is not |
| `compose_work_placement_responsibility_vs_thread` | `compose_derived_state` | Assesses placement-as-responsibility, which is closer to `compose_state_hoisting`/`state_ownership` than to derived state. **Considered and not recommended for re-mapping**: it is the Question that closes E23 `lesson_work_outside_composition`, and moving it would take practice away from a shipped Lesson that earns it | Leave. Recorded so a later reviewer does not re-open it |
| `lifecycle_repeat_on_lifecycle` | `lifecycle_aware_apis`, APPLIED | Correct and correctly mapped. Its scenario is a Fragment, which E25 does not teach. Supporting-only here, so it creates no Unit 8 practice | Leave. Record in E25-09 that Unit 8's lifecycle reasoning has no ACTIVE Question until GAP-U8-B is filled |
| `live_data_vs_state_flow_ui_state` | `livedata`, FOUNDATION | States that "an Activity or Fragment has to collect it inside `repeatOnLifecycle` or `flowWithLifecycle`". Accurate for a View host and not a claim about Compose | Leave. Unit 8 must not contradict it, and should not cite it |
| `compose_state_hoisting_001` | `compose_state_hoisting`, FOUNDATION | Correctly levelled. Its explanation is the one place in the bank that states "Hoisting is not the same as pushing all state into a ViewModel", which Unit 7 must stay consistent with | Leave |
| `compose_launched_effect_key_restart` | `compose_side_effects`, FOUNDATION | Correct; FOUNDATION is right for the restart direction | Leave |
| No Question anywhere | — | **Searched and not found:** no ACTIVE or DEPRECATED Question claims that `collectAsStateWithLifecycle` is Android-only, or makes any platform-availability claim about it | No action. Recorded because the epic asked for platform-assumption defects to be found, and the bank has none |

**One documentation defect was found and is fixed by this issue, not deferred:** the Compose
blueprint's former L8.2 claimed the lifecycle-aware variant "is not available on every target".
See [The factual error in the former L8.2](#the-factual-error-in-the-former-l82).

---

## Cross-linking and shipped-content edits

`LearningCurriculumValidator` rejects a `relatedLessonIds` entry naming an unknown Lesson
(`UNKNOWN_RELATED_LESSON`), and ids are resolved across the whole document. **Forward links are
invalid until their target ships.**

- Each authoring issue may link **backwards** only: to the 50 shipped Lessons and to Lessons
  shipped by an earlier E25 issue.
- Pointing forward is a prose sentence naming the Unit. The issue that ships that Unit may then
  add the reciprocal link from its own Lessons.

**The intended final graph**, recorded now so the authoring order does not lose it:

| From | To | Why |
| --- | --- | --- |
| L7.1, L7.4 | `lesson_state_hoisting`, `lesson_state_down_events_up` | The ownership model this Unit applies |
| L7.3 | `lesson_stability_and_skipping` | Why immutability of the state value matters |
| L7.4 | `lesson_remember_saveable`, `lesson_work_outside_composition` | The lifetime ladder and the placement argument |
| L8.1, L8.2 | `lesson_observable_state`, `lesson_snapshot_observation`, `lesson_state_flow` | What composition observes, and what a `StateFlow` is |
| L8.3 | `lesson_flow_collection_lifetime`, `lesson_sharing_cold_flows` | Collection lifetime and who decides whether the upstream stops |
| L8.4 | `lesson_state_flow` | The current-value contract the initial value comes from |
| L9.1 | `lesson_composable_execution`, `lesson_composition_and_recomposition` | The execution contract this Unit turns into an ownership problem |
| L9.2 | `lesson_composable_identity`, `lesson_coroutine_scope_ownership`, `lesson_cooperative_cancellation` | Call-site identity and coroutine lifetime |
| L9.3 | `lesson_remember_key_memoization`, `lesson_stability_and_skipping` | The two comparisons |
| L9.4 | `lesson_remember_key_memoization` | Both failure directions, already taught for a cached value |
| L10.1 | `lesson_launched_effect`, `lesson_effect_keys_as_dependencies` | The alternative it is compared against |
| L10.3 | `lesson_coroutine_scope_ownership`, `lesson_structured_concurrency` | Where work that outlives the screen belongs |
| L11.1 | `lesson_cancellation_cleanup_and_timeouts`, `lesson_flow_collection_lifetime` | Cancellation as the other shape of cleanup, and what a cancelled collector releases |
| L11.3, L11.4 | `lesson_flow_builders_and_callback_adapters`, `lesson_snapshot_flow` | The two adapters on the other side of the boundary |
| L12.1 | Every earlier E25 Lesson, and `lesson_work_outside_composition` | It is the synthesis |
| L12.3 | `lesson_choosing_a_stream_abstraction`, `lesson_shared_flow` | The delivery argument it applies |

**Backward links into E25 from shipped Lessons — two required edits, both owned by E25-02.**

Both are shipped E23 Lessons that promise a "ViewModel unit" which this reconciliation has
dissolved:

1. `lesson_state_hoisting`, Practical: "...that is where a screen-level owner takes over, and
   the `ViewModel` unit later in this path covers it." The clause must name the screen-state
   Unit instead, and must not promise architecture that E26 owns.
2. `lesson_remember_saveable`, Core: "How screen-level state is owned and produced is the
   subject of the ViewModel unit later in this path."

E25-02 may add `lesson_screen_state_owner_boundary` to their `relatedLessonIds` in the same
change, because the target ships in that change. **No other shipped Lesson needs editing for
E25**, and three further forward pointers are satisfied without any edit because they name a
subject rather than a Unit title:

- `lesson_composable_execution` — "Those APIs have their own unit later in this path."
- `lesson_composition_and_recomposition` — "...Compose's effect APIs, which give it a defined
  lifetime, and which have their own unit later in this path."
- `lesson_snapshot_flow` — "...the effect APIs as a family, and how to choose between them, are
  a later unit's subject."

E25-04 and E25-07 may add backlinks to these three from their own Lessons; E25-09 should
re-read all five and confirm nothing still reads as a promise about a Unit that does not exist.

---

## Boundaries with adjacent curriculum

Stated concept by concept so a later authoring agent cannot absorb them by accident.

### E23 owns the Compose state and execution foundation

E25 does **not** teach: declarative UI; what a composable is; the execution contract;
`mutableStateOf` and `State<T>`; `remember`; `rememberSaveable`; state-hoisting fundamentals
and the three "at least" rules; observable collections; composition and recomposition;
recomposition scopes; recomposition cost; call-site identity; `key` and list keys;
immutability against stability; strong skipping; `@Stable`/`@Immutable`; `remember(key)` as
memoization; `derivedStateOf`; keeping work out of composition; the snapshot system;
`snapshotFlow`.

E25 **applies** all of them. Where a Lesson needs one, it gets one bounded paragraph and a
`relatedLessonIds` link.

### E24 owns coroutines and Flow

E25 does **not** teach: suspension; builders; `Job`; scope construction; structured
concurrency; context and dispatchers; `withContext`; concurrency against parallelism;
cooperative cancellation; `NonCancellable`; timeouts; exception propagation; supervision;
shared-state coordination; Flow fundamentals; cold flows; collection lifetime as a coroutine
property; `flowOn`; flow builders and `callbackFlow`; operators; flattening; buffering and
conflation; `catch`/`retry`/`onCompletion`; hot against cold; `StateFlow`; `SharedFlow`;
`stateIn`/`shareIn`/`SharingStarted`; stream-abstraction selection.

E25 **applies** these at the Compose boundary. Three bridges are authorised and bounded:
`lesson_state_flow`'s current-value contract (Unit 8), `lesson_sharing_cold_flows`' "the
upstream's owner decides" (one paragraph, Unit 8 L8.3), and `lesson_choosing_a_stream_abstraction`'s
delivery argument (Unit 12 L12.3).

### E26 owns application architecture

E25 does **not** teach: MVC, MVP, MVVM, MVI, or MVVM-against-MVI; reducers; layered
architecture; the repository pattern; use cases; single source of truth as an architecture;
Clean Architecture; detailed state-holder architecture; `UiState` modelling strategies (sealed
hierarchy against nullable fields, partial states, error representation); application-level
event modelling; durable-against-transient event architecture; choosing `Channel` against
`SharedFlow` as a ViewModel event design; acknowledgement and queueing architecture;
`SavedStateHandle` as a state-production mechanism.

**This is the boundary the former blueprint Unit 8 crossed**, and it is the one most likely to
be crossed again by accident. Three rules for the authoring issues:

1. Unit 7 L7.4 may say a screen-level owner exists, what it survives, and how the composition
   talks to it. It may not say how it is built, injected, layered or tested.
2. Unit 7 L7.3 may say the screen renders one immutable current value. It may not compare ways
   of modelling that value.
3. Unit 12 L12.3 may say that a guaranteed-delivery requirement leaves the Compose boundary. It
   may not design what is on the other side.

`state_ownership`, `unidirectional_data_flow`, `mvi`, `mvvm`, `repository_pattern`,
`layered_architecture` and `single_source_of_truth` are supporting-only or unmapped throughout
E25, so none of them becomes Unit practice.

### E27 owns dependency injection

E25 does **not** teach: Hilt, Koin, constructor injection, ViewModel injection, DI scopes, or
graph assembly — even though a screen-level owner appears in Unit 7's examples and this
repository wires its own screens with Koin. An example may construct an owner however is
clearest; it must not present the construction as the lesson.

### E31 owns testing

E25 does **not** teach: Compose UI testing, effect testing, lifecycle-collection testing,
Turbine, `runTest`, virtual time, test dispatchers, or Flow testing. Unit 7 L7.2 may say that
a stateless content composable is testable and previewable as part of *why the boundary
exists*; it may not show a test.

The `runComposeUiTest` probes behind this plan's measurements are **authoring validation, not
curriculum**, and were deleted.

### Other exclusions

- **Navigation.** Navigation remains owned by `lifecycle_navigation`. Unit 12 may use "a
  navigation request" as one transient occurrence among several; it must not teach navigation
  APIs, back-stack behaviour, or Navigation 3.
- **RxJava migration.** Out of scope entirely. `rxjava_fundamentals` and `flow_vs_rxjava` are
  unmapped by E25.
- **`LiveData`.** Reference only — at most a comparison clause in Unit 8 L8.4.
- **Fragment Flow collection.** Supporting context at most, as the contrast that explains why
  the Compose API exists.
- **WorkManager and background work.** Named once in Unit 10 L10.3 as "not this", then
  deferred.
- **New learning UI, database changes, and broad production refactoring.** Outside the epic.
- **Compose runtime internals.** The implementations quoted in this plan are *verification* of
  contracts, not a mental model to teach. A Lesson may say "`LaunchedEffect` is a `remember`
  with a launched job" because it makes the key comparison obvious; it must not become a tour
  of `RememberObserver`.

---

## Handoff

**Sequencing.** E25-02 → E25-03 → E25-04 → E25-05 → E25-06 → E25-07 → E25-08 → E25-09, in Unit
order. The order is a real dependency chain, not a convenience: Unit 8 assumes Unit 7's screen
shape, Units 10 and 11 assume Unit 9's key model, and Unit 12 is a synthesis of all five.

**What E25-09 should verify beyond the issue's own criteria:**

- Terminology is consistent across the six Units **and with the shipped Compose and
  Coroutines Units**: "owner", "lifetime", "trigger", "composition", "leaves composition",
  "key", "restart", "collect", "conversion", "conflated", "transient", "delivery".
- No E25 Lesson re-derives a shipped E23 or E24 argument. Unit 7 against `lesson_state_hoisting`
  is the case to read hardest.
- The five forward pointers listed in
  [Cross-linking and shipped-content edits](#cross-linking-and-shipped-content-edits) all read
  correctly after the two required edits.
- Unit practice for each of Units 9–12 was checked with the overlap in mind, and the result was
  recorded rather than assumed.

**Limitations carried forward, none of which is a defect to fix in E25-09:**

1. **Four Units share one practice pool** through `compose_side_effects`, and no amount of
   question authoring changes that. Only a taxonomy change would, and E25 does not make one.
2. **Unit 8's practice pool assesses none of Unit 8's material** until GAP-U8-A and GAP-U8-B are
   filled, and even then it will continue to include three E23 Questions through `compose_state`.
3. **Unit 7's practice overlaps E23 Units 1 and 2** through `compose_udf` and
   `compose_state_hoisting`. Intended, and worth stating to a reviewer who finds a shipped Unit's
   Question in a new Unit's practice.
4. **`iosArm64` was not compiled locally** and is not built on CI. The lifecycle-aware collection
   claim rests on `iosSimulatorArm64` plus the published artifact for `iosArm64`.
5. **The lifecycle artifact is a beta** (`2.11.0-beta01`) and the resolved Compose runtime
   (1.11.2) is ahead of the declared Compose Multiplatform version (1.11.1). Every authoring
   issue should re-verify rather than trust this document's version table.
6. **`compose_previews`, and the KMP and performance Subtopics used as supporting concepts,
   hold no practice claim.** They are vocabulary, exactly as the blueprint uses them elsewhere.

---

## Authoring outcomes

Each authoring issue appends its outcomes here, following the precedent of
`compose-units-2-6-plan.md` and `coroutines-flow-units-1-6-plan.md`: what it kept, what it
changed, what it measured, and what the next issue must know. E25-01 was planning only; the
production outcomes begin below.

### E25-02 — Unit 7

`unit_production_screen_state_and_udf` shipped under `android_ui` with all four planned
identities, titles, mappings and authored order unchanged:
`lesson_classes_of_screen_state`, `lesson_stateless_screen_content`,
`lesson_screen_state_and_ui_events`, and `lesson_screen_state_owner_boundary`. The four use
one question-library screen whose difficulty-menu expansion stays local, whose selected
difficulty is coordinated by a Compose parent, and whose loaded questions, saved markers and
load failure come from a screen-level owner. No Question, taxonomy entry, effect API, observable
state collection, or E26 architecture material was added.

The shipped argument preserves the E23 boundary. L7.1 applies the three existing ownership
tests to several values on one screen and makes over-hoisting concrete through an inflated
screen value, local-interaction callbacks, enlarged parameter lists, implementation coupling,
a wider update surface, and unnecessarily extended lifetime. L7.2 establishes the wiring-screen
against stateless-content boundary through reuse, previews, isolated verification and one place
to inspect wiring. L7.3 replaces writable state and owner references with one immutable current
screen value and intent callbacks, while stating explicitly that local UI-element state can still
coexist behind that boundary. L7.4 makes owner lifetime — not merely placement outside the
Composition — decide survival.

The repository-specific lifetime example was rechecked against `App.kt`,
`docs/architecture/overview.md`, and the current official Navigation 3 state and entry-decorator
documentation. The configured `NavDisplay` still installs saveable-state and ViewModel-store
entry decorators; a relevant back-stack entry retains its own `ViewModelStore`, and removing the
entry clears it. Android configuration recreation is labelled as Android-specific, while the
general multiplatform claim remains that the host supplies the owner's lifetime. Current Android
state-hoisting and Compose architecture guidance still supports keeping simple UI-element state
local, exposing immutable state with events, and using the lowest common owner. Current Compose
Multiplatform ViewModel guidance still makes host ownership, rather than the class name alone,
the relevant lifetime boundary.

The intended backward graph shipped. L7.1 and L7.4 link to `lesson_state_hoisting` and
`lesson_state_down_events_up`; L7.3 links to `lesson_stability_and_skipping`; L7.4 also links to
`lesson_remember_saveable` and `lesson_work_outside_composition`. The shipped
`lesson_state_hoisting` and `lesson_remember_saveable` pointers now name this Unit's bounded
Compose-side screen-owner treatment, explicitly leave detailed state-holder architecture to later
curriculum, and reciprocally link to `lesson_screen_state_owner_boundary`. No other shipped Lesson
was edited.

The semantic Question verdicts are unchanged after reading the finished Lessons.
`compose_state_hoisting_001` remains correct and eligible but assesses E23's one-value hoisting
foundation rather than Unit 7's whole-screen reasoning. `compose_udf_event_direction` directly
supports L7.3 because it requires a callback instead of shared writable state or a ViewModel
reference. GAP-U7-A, GAP-U7-B and GAP-U7-C therefore remain open for E25-08: the Lessons now teach
all three targets, but teaching does not create assessment coverage. Generated coverage reports
four Lessons, two distinct primary concepts, and exactly the two eligible Questions above;
supporting concepts remain excluded from Unit practice. Related-Lesson links are not part of the
coverage report format and are instead validated by the bundled-content tests.

Validation run during authoring: `python3 tools/learning_question_coverage.py --write` regenerated
the snapshot; `python3 tools/learning_question_coverage.py --check` reported it current;
`cd tools && python3 -m unittest test_learning_question_coverage.py` passed 21 tests;
`./gradlew :shared:jvmTest` passed 1,372 tests after the production catalogue and journey
expectations were expanded from 21 to 25 `android_ui` Lessons; and
`./gradlew :shared:allTests` passed Android host, JVM, iOS simulator, JS and Wasm targets;
`./gradlew :shared:check` also completed successfully. The existing learner's three published
Lesson identities remained unchanged. E25-03 may assume the screen/content split and immutable
value-plus-callback contract, but still owns every mechanism that converts an outside observable
value into Compose state.

### E25-03 — Unit 8

`unit_observable_state_collection` shipped under `android_ui` with all four planned identities,
titles, mappings and authored order unchanged: `lesson_external_state_in_compose`,
`lesson_collect_as_state`, `lesson_collection_lifetime_and_cost`, and
`lesson_lifecycle_aware_collection`. Every Lesson keeps `compose_state` as its sole primary
Subtopic, so Unit practice is derived from that concept only. Flow, coroutine, lifecycle and KMP
concepts remain supporting context. No Question, taxonomy entry, effect curriculum, Fragment
collection guidance, Flow testing material, or E26 state-holder architecture was added.

The shipped progression keeps the four planned questions separate. L8.1 shows that reading
`StateFlow.value` does not connect a later stream update to Compose invalidation, while preserving
the possibility that an unrelated recomposition reads the newer value. L8.2 presents collection as
the bridge into snapshot-observed `State`, contrasts the current value supplied by `StateFlow` with
the explicit initial value required by plain `Flow`, and treats stream identity and `context` only
as far as the API contract requires. L8.3 makes composition-owned collection, cancellation cost,
and the independent lifetime of a shared producer concrete. L8.4 then adds lifecycle activity as a
second gate instead of presenting `collectAsStateWithLifecycle` as an unconditional upgrade.
Manual `LaunchedEffect` collection is named only as the wrong default abstraction for continuous
renderable state; E25-04 still owns effect mechanics.

The freshness-sensitive API evidence was rechecked before authoring. Compose Multiplatform remains
declared at 1.11.1 and resolves AndroidX Compose runtime 1.11.2; the resolved `collectAsState`
source still gives the `StateFlow` overload its initial `value`, requires `initial` for a plain
`Flow`, keys the producer by the stream and coroutine context, and applies a non-empty `context` to
collection. JetBrains lifecycle remains `2.11.0-beta01` in `commonMain`; its resolved source still
provides the four planned `StateFlow`/`Flow` plus `LifecycleOwner`/`Lifecycle` overloads, defaults to
`LocalLifecycleOwner.current` and `STARTED`, rejects `INITIALIZED`, and composes `produceState` with
`repeatOnLifecycle`. Production still calls `collectAsStateWithLifecycle` from `commonMain`.

The platform reasoning was also rechecked against the current configured source and official
documentation. Android uses its relevant host owner. Desktop maps visible focused windows to
`RESUMED`, visible unfocused windows to `STARTED`, and minimized or detached windows below the
default threshold; its shell still supplies `kotlinx-coroutines-swing`. iOS keeps an
inactive-but-foreground scene at `STARTED` and drops a background or non-appeared scene below it.
On web, blur alone pauses to `STARTED`, while hiding the page drops below the threshold; the Lesson
also records the documented normal-host limitations around `CREATED` and `DESTROYED`. These are
presented as target mappings to one common threshold, not as identical platform lifecycles.

The semantic Question verdicts remain unchanged after reading the finished Lessons.
`remember_vs_remember_saveable`, `compose_unremembered_observable_state`, and
`compose_state_collection_mutation` form the three-Question `compose_state` practice pool, but all
assess E23 material. GAP-U8-A, GAP-U8-B and GAP-U8-C therefore remain open for E25-08: conversion
and initial state, the plain-versus-lifecycle-aware lifetime decision, and the distinction between
stopping a UI collector and stopping upstream production are now taught but still unassessed.
`lifecycle_repeat_on_lifecycle` remains supporting context and was not made Unit practice.

Validation run during authoring: `jq empty` accepted the production document;
`python3 tools/learning_question_coverage.py --write` regenerated the snapshot;
`python3 tools/learning_question_coverage.py --check` reported it current;
`cd tools && python3 -m unittest test_learning_question_coverage.py` passed 21 tests;
`./gradlew :shared:jvmTest` passed 1,373 tests after the production catalogue, practice and journey
expectations were expanded from 25 to 29 `android_ui` Lessons; `./gradlew :shared:allTests` passed
the configured Android host, JVM, iOS simulator, JS and Wasm targets; and
`./gradlew :shared:check` completed successfully. `iosArm64` was not compiled locally, so no device
target compile is claimed. E25-04 may assume the external-stream-to-Compose-state and two-gate
lifetime model, but still owns side effects, `LaunchedEffect`, and effect keys in full.

### E25-04 — Unit 9

`unit_effect_lifecycle_and_launched_effect` shipped under `android_ui` immediately after
`unit_observable_state_collection`, with all four planned identities, titles, mappings and authored
order unchanged: `lesson_why_effects_are_controlled`, `lesson_launched_effect`,
`lesson_effect_keys_as_dependencies`, and `lesson_effect_key_failures`. Every Lesson keeps
`compose_side_effects` as its sole primary Subtopic, so Unit practice is based only on that concept.
The planned Compose, Kotlin and coroutine concepts remain supporting. No Question, taxonomy entry,
later effect API, transient-event architecture, state-holder architecture, or E25-05 material was
added.

The shipped progression is one ownership-and-lifetime argument. L9.1 starts with analytics and
question work launched directly from a composable body, then makes repeated execution, skipping and
an abandoned composition expose three different failures in treating body execution as an event or
lifecycle callback. It corrects "a composable runs once" without replacing it with "a composable
always runs repeatedly": the runtime owns whether and when the body executes. L9.2 gives
composition-owned suspend work a precise lifetime and traces entry, ordinary recomposition with
equal keys, key change, leaving and re-entry. It states that ordinary recomposition does not restart
the effect, and that key change cancels in-flight old work before launching its replacement.

Cancellation is kept at E24 bridge depth. Compose establishes the owner and lifetime; ordinary
coroutine cancellation supplies the mechanism. Leaving composition or replacing an effect requests
cooperative cancellation, which cannot forcibly preempt arbitrary blocking or non-cooperative code.
Cancellation bounds continued work and does not roll back an external mutation that already
completed. The context statement is likewise bounded to the resolved public contract: the effect
uses the Composition's applying coroutine context, with no universal Android-main-thread claim.

The source-sensitive findings were rechecked on 2026-09-14. Compose Multiplatform remains declared
at 1.11.1, Kotlin and the Compose compiler remain 2.4.10, and Gradle dependency insight still
resolves `androidx.compose.runtime:runtime:1.11.2` on the JVM. The resolved 1.11.2 `Effects.kt`
still implements `LaunchedEffect(key1)` as `remember(key1) { LaunchedEffectImpl(...) }`; its KDoc
still specifies launch on entering Composition, cancellation and relaunch for a different key,
cancellation on leaving, and points callback-triggered ongoing work to a later mechanism. The
implementation launches on `onRemembered` and cancels on `onForgotten` and `onAbandoned` through
ordinary coroutine cancellation.

The exact key conclusion is unchanged. Resolved `Composables.kt` says `remember(key)` retains its
value when the key compares equal with `==`, and delegates the decision to `Composer.changed`;
`Composer` documents `changed` in terms of `equals` and exposes `changedInstance` as the separate
`===` operation. Therefore effect keys use remember-key equality. Strong-skipping parameter
comparison is a different decision: current official guidance states that stable parameters use
object equality while unstable parameters use instance equality. The Lessons teach the two
decisions, not runtime implementation classes.

A bounded throwaway JVM Compose probe was rerun against the configured compiler and resolved
runtime and then deleted. Across four composition passes it observed starts of **1** for a fresh
structurally equal data-class key, **4** for a fresh default/identity-equality key, **1** for a
non-capturing lambda, **1** for a lambda capturing unchanged data, **1** for an explicitly
remembered lambda, **4** for a lambda capturing changing data, and **1** for `Unit`. Equality is the
API/runtime contract. The exact lambda results are measured current-toolchain behavior explained by
compiler memoization, not a timeless promise. The two side-by-side object examples make the other
conclusion explicit: fresh allocation does not restart an effect unless the supplied key stops
comparing equal.

L9.3 turns keys into the question "what change should end this lifetime?" rather than a list of
everything the block reads. It preserves the official used-values heuristic as a useful way to
find missed invalidating inputs, then establishes its boundary: some long-lived work should remain
alive while a value becomes current. It names that unresolved requirement without teaching the
solution E25-05 owns. `LaunchedEffect(Unit)` is taught as the declaration that only this call site
leaving Composition ends the effect, not as "run once". L9.4 diagnoses both false declarations: a
constant-key hint sequence remains tied to a stale `questionId`, while a freshly recreated
identity-equality wrapper repeatedly cancels and replaces still-valid work. It makes discarded
progress, reset timers, repeated requests and non-rollback of completed external effects concrete.

The intended backward graph shipped unchanged. L9.1 links to `lesson_composable_execution` and
`lesson_composition_and_recomposition`; L9.2 links to `lesson_composable_identity`,
`lesson_coroutine_scope_ownership` and `lesson_cooperative_cancellation`; L9.3 links to
`lesson_remember_key_memoization` and `lesson_stability_and_skipping`; and L9.4 links to
`lesson_remember_key_memoization`. No forward `relatedLessonIds` were added.

The two ACTIVE Questions were re-solved and checked against their options, explanations and current
official side-effects source; neither was edited. `compose_launched_effect_key_restart` remains
correct, correctly mapped and correctly levelled FOUNDATION coverage for the cancel-and-relaunch
slice of Unit 9. `compose_side_effects_001` remains correct and well-sourced, but its
`rememberCoroutineScope`, `DisposableEffect` and `SideEffect` reasoning spans Units 10 and 11.
Generated coverage now reports four Lessons, one distinct primary concept, and two reachable
FOUNDATION Questions. That is structural reach, not semantic adequacy: Unit 9 learners can still
receive later material because Units 9–12 share the `compose_side_effects` pool. The limitation is
accepted for this issue and remains an explicit E25-08 decision rather than being hidden by a false
mapping or invented taxonomy id.

GAP-U9-A through GAP-U9-D all remain open for E25-08. The finished Lessons now teach uncontrolled
body work, equality plus needless restart behavior, too-few-key stale work, and the constant key as
a lifetime claim respectively; shipping teaching does not create assessment coverage. No new
distinct Unit 9 gap was found. E25-05 may assume that learners can distinguish recomposition from
effect restart and can decide whether a value should end an effect lifetime. It inherits the clean
problem that a long-lived effect may need a changing value currently without a restart; E25-05
still owns the mechanism and event-triggered work in full.

Validation run during authoring: `jq empty` accepted the production document;
`./gradlew :shared:dependencyInsight --configuration jvmRuntimeClasspath --dependency
androidx.compose.runtime:runtime` resolved 1.11.2; the throwaway probe passed with
`./gradlew :shared:jvmTest --tests 'org.artkachenko.kmp_learning_app.E25KeyProbeTempTest'` and was
removed; the four focused production suites passed together; `python3
tools/learning_question_coverage.py --write` regenerated the snapshot; its `--check` mode reported
the snapshot current; `cd tools && python3 -m unittest test_learning_question_coverage.py` passed
21 tests; `./gradlew :shared:jvmTest` passed the common/JVM suite after the Android UI journey grew
from 29 to 33 Lessons; and `./gradlew :shared:allTests` passed Android host, JVM, iOS simulator, JS
and Wasm targets. `./gradlew :shared:check` and `git diff --check` were also run successfully after
the final authoring change. No `iosArm64` device-target test or external CI run is claimed.

### E25-05 — Unit 10

`unit_latest_values_and_event_driven_work`, **Latest-Value Effects and Event-Driven Coroutine
Work**, shipped under `android_ui` immediately after `unit_effect_lifecycle_and_launched_effect`.
Its three planned Lessons shipped in their original order and with unchanged identities:
`lesson_remember_updated_state`, **Reading the Current Value Without Restarting**;
`lesson_remember_coroutine_scope`, **`rememberCoroutineScope`: Launching From an Event**; and
`lesson_who_owns_the_trigger`, **Composition-Driven or Event-Driven?** No Question, taxonomy entry,
Unit 11 API, transient-delivery architecture, state-holder architecture, or E25-06 content was
added.

All three Lessons keep `compose_side_effects` as their sole primary Subtopic. L10.1 supports it with
`compose_state`, `compose_recomposition`, and `kotlin_lambdas`; L10.2 with `coroutine_scope`,
`coroutine_builders`, and `structured_concurrency`; and L10.3 with `coroutine_scope`,
`viewmodel_lifecycle`, `lifecycle_coroutines`, and `coroutine_cancellation`. Those supporting
concepts remain excluded from Unit practice. The mapping is deliberately honest despite the shared
pool limitation.

L10.1 starts from Unit 9's question: should this changing value end the current work? Its timed
question keeps one realistic 30-second composition-lifetime timer while recomposition can supply a
new `onTimeout`. The Lesson compares `LaunchedEffect(onTimeout)` directly with
`rememberUpdatedState(onTimeout)` plus `LaunchedEffect(Unit)`. Keying on the callback is correct when
the old timer is obsolete, but it cancels the in-flight delay and starts the full timer again when
the callback key stops comparing equal. The latest-value shape is correct when the original timer
remains valid and only the callback read at completion must be current. The constant key is applied
as a call-site composition-lifetime claim, not renamed to "run once".

The exact current-value model is explicit. Compose remembers a `State` holder, recomposition updates
its value, and the still-running coroutine reads that holder when execution reaches the read. A
value copied into a local before a delay does not change retroactively. The Lesson also states that
`rememberUpdatedState` neither prevents nor reduces recomposition: reading the returned State in
composable code participates in ordinary state-read invalidation. Here the effect stays alive
because the value is read inside its existing coroutine rather than supplied as a changing key.

The source-sensitive findings were rechecked on 2026-09-14. Kotlin and the Compose compiler remain
2.4.10, Compose Multiplatform remains declared at 1.11.1, and Gradle dependency insight still
resolves `androidx.compose.runtime:runtime:1.11.2` on the JVM. The resolved 1.11.2
`SnapshotState.kt` KDoc still describes updating a value across recompositions without recreating a
long-lived lambda/object or restarting an operation, and its implementation remains
`remember { mutableStateOf(newValue) }.apply { value = newValue }`. The current official side-effect
guide still gives the same long-lived timeout case, current callback, constant-key caution, and
keyed alternative. The public/KDoc purpose is taught as the contract; the implementation confirms
the State model rather than replacing that contract.

No new latest-value probe was run for E25-05. The resolved source and public documentation settle
the authored claims, and E25-01's still-current throwaway JVM measurement already observed one
`LaunchedEffect(Unit)` start while the existing coroutine read `[0, 1, 2]`. That earlier measurement
remains labelled current-toolchain evidence rather than an additional API guarantee, and no probe
file exists in the final diff.

L10.2 uses a Save-answer click that launches only transient snackbar UI work. The sequence is
explicit: composition creates/remembers the scope; the event calls `scope.launch`; only then does a
coroutine start. `rememberCoroutineScope` therefore launches nothing on its own. The caller chooses
whether and when to launch, another callback may launch another child, and a retained `Job` is
mentioned only as an optional handle when real cancellation or coordination is required.

The resolved 1.11.2 `Effects.kt` KDoc and source remain unchanged for the lifetime claims. The same
scope instance is remembered across recompositions; the scope is bound to that composition point;
it is cancelled on forgotten or abandoned; its default dispatcher is the Recomposer's applying
dispatcher when none is supplied; and the supplied context may not contain a parent `Job`. The
Lesson includes the stable restriction and, at Senior depth, the current failure behavior: the
composable call does not throw directly, while the returned scope contains a failed `Job` and
cannot launch children. It also preserves the KDoc's warning that jobs must not be launched from a
composable body as a side effect of composition itself.

Leaving Composition while the snackbar coroutine is suspended cancels the remembered scope and
propagates cancellation to its children. Cancellation is described with E24 precision: it is a
cooperative request observed by cancellation-aware work, not an immediate preemption of arbitrary
blocking code, and it cannot roll back external work that already completed. L10.2 links to
`lesson_coroutine_scope_ownership` and `lesson_coroutine_builders` instead of reteaching their Job
and hierarchy mechanics.

L10.3 states the final decision as ownership. Use `LaunchedEffect` when entering or changing a
particular composition state should start or restart the work: Composition owns both trigger and
lifetime. Use `rememberCoroutineScope` plus `launch` when a click or callback should decide when
work starts: the event owns the trigger while Composition still owns the scope lifetime. A
question-specific hint timer keyed by `questionId` is the valid composition-triggered case.

The same-screen worked comparison uses a Question editor whose requirement is "when the user taps
Save, show confirmation." The incorrect version derives a `LaunchedEffect` from a persistent
`showSavedMessage` rendering condition; leaving and re-entering that branch while the condition
remains true can fire the transient snackbar again. The correct version remembers a scope and
calls `scope.launch` from the Save callback, so that click owns the trigger. The example is kept at
direct Compose/UI-mechanics depth and does not generalize into Unit 12's transient-delivery
architecture.

Work that must continue after the screen leaves Composition is explicitly assigned to neither
mechanism. Committed uploads, durable saves, payment submission, and background synchronization are
used only to reveal the lifetime mismatch; the Lesson stops at "move the work to an owner whose
lifetime matches the requirement." It does not teach `viewModelScope`, WorkManager, repositories,
queues, MVVM, or MVI. L10.3 links backward to `lesson_launched_effect`,
`lesson_coroutine_scope_ownership`, and `lesson_structured_concurrency`. L10.1 links to
`lesson_launched_effect` and `lesson_effect_keys_as_dependencies`. No forward links were added.

The two ACTIVE Questions reachable through `compose_side_effects` were re-solved from their stems
and options and checked against their answer keys, explanations, Sources, and levels; no
DEPRECATED Question is currently mapped to this primary Subtopic. `compose_launched_effect_key_restart`
remains correct, correctly mapped, and correctly levelled FOUNDATION coverage for the Unit 9
restart direction. `compose_side_effects_001` remains correct and well-sourced at FOUNDATION: its
`rememberCoroutineScope` distractor and explanation are now answerable after Unit 10, but the
Question still spans Units 9-11 through its `DisposableEffect` and `SideEffect` clauses. Neither
Question was edited, and no Question wording was copied into a Lesson.

Generated coverage now reports Unit 10 as three Lessons, one distinct primary concept, and two
reachable FOUNDATION Questions. That is structural reach through `compose_side_effects`, not
semantic adequacy. Units 9-12 still share an identical practice pool, so Unit 10 can receive Unit
9/11 material and any future `rememberUpdatedState` Question on this Subtopic would also appear
after Unit 9. No dishonest primary mapping or invented Subtopic was used to hide that limitation;
E25-08 retains the deliberate assessment decision.

GAP-U10-A, GAP-U10-B, and GAP-U10-C all remain open for E25-08. The shipped Lessons now teach the
stale/current callback problem and keyed restart cost, trigger ownership as a decision, and
remembered-scope cancellation plus the longer-lived-work boundary respectively; authored teaching
does not create semantic Question coverage. No additional distinct Unit 10 gap was exposed.
E25-06 may assume that learners can distinguish a composition-owned trigger from an event-owned
trigger and can distinguish restarting an effect from keeping its lifetime while reading a current
value.

Validation run during authoring: `jq empty` accepted the production document;
`./gradlew :shared:dependencyInsight --configuration jvmRuntimeClasspath --dependency
androidx.compose.runtime:runtime` resolved 1.11.2; the initial focused four-suite run found one
legitimate stale lesson-count expectation, and the same command passed after it was updated;
`python3 tools/learning_question_coverage.py --write` regenerated the snapshot and its `--check`
mode reported it current; `cd tools && python3 -m unittest test_learning_question_coverage.py`
passed 21 tests (with the existing sandbox warning from RVM's `ps` call); `./gradlew
:shared:jvmTest` passed the full common/JVM suite after the Android UI journey grew from 33 to 36
Lessons; and `./gradlew :shared:allTests` passed Android host, JVM, iOS simulator, JS, and Wasm
targets. `./gradlew :shared:check` and `git diff --check` also passed after the final authoring
change. No `iosArm64` device-target test, external CI run, or new runtime probe is claimed.

### E25-06 — Unit 11

`unit_cleanup_synchronization_and_producers`, **Cleanup, External Synchronization and State
Producers**, shipped under `android_ui` immediately after
`unit_latest_values_and_event_driven_work`. Its four planned Lessons shipped in the planned
order with unchanged identities and titles: `lesson_disposable_effect`, **Registration and
Release as One Decision**; `lesson_side_effect_publication`, **`SideEffect`: Publishing to
Non-Compose Code**; `lesson_produce_state`, **`produceState`: a Composition-Scoped Producer**;
and `lesson_flow_adapter_or_compose_producer`, **A Flow Below the UI, or a Producer at the
Boundary?** No Question, taxonomy entry, mechanism-selection decision tree, transient-effect
material, Flow or `callbackFlow` curriculum, or E25-07 content was added.

All four Lessons keep `compose_side_effects` as their sole primary Subtopic and the plan's
supporting mappings unchanged: L11.1 with `lifecycle_aware_apis`, `memory_leaks` and
`lifecycle_leaks`; L11.2 with `compose_recomposition`, `compose_fundamentals` and
`compose_state`; L11.3 with `compose_state`, `flow_collection` and `coroutine_cancellation`;
L11.4 with `flow_fundamentals`, `hot_vs_cold_streams` and `separation_of_concerns`. No Lesson
lists a Subtopic as both primary and supporting, and every supporting concept stays out of Unit
practice.

**Versions rechecked on 2026-09-14.** `gradle/libs.versions.toml` still declares Kotlin 2.4.10,
Compose Multiplatform 1.11.1, Material 3 1.11.0-alpha07, lifecycle 2.11.0-beta01,
kotlinx.coroutines 1.11.0 and AGP 9.0.1. `./gradlew :shared:dependencyInsight --configuration
jvmRuntimeClasspath --dependency androidx.compose.runtime:runtime` still resolves
**`androidx.compose.runtime:runtime:1.11.2`** on the JVM, and that artifact's `commonMain`
sources are what every source-sensitive claim below was read from.

**`DisposableEffect` findings.** The resolved 1.11.2 `Effects.kt` implements
`DisposableEffect(key1) { … }` as `remember(key1) { DisposableEffectImpl(effect) }`, where the
impl is a `RememberObserver`: `onRemembered` runs the effect block and keeps the returned
`DisposableEffectResult`, `onForgotten` disposes it, and `onAbandoned` does nothing because
`onRemembered` was never called. The KDoc states that the `onDispose` clause **must** be the
final statement, that there is guaranteed to be one `onDispose` call for every call to the
effect block, and that both run on the composition's apply dispatcher. The no-key overload
remains a `DeprecationLevel.ERROR` shadow. The KDoc also directs work that needs no disposal to
`SideEffect` or `LaunchedEffect` instead, which is the basis for the Lesson's rejection of an
empty `onDispose`. The public documentation page adds the key-change wording used in the
Lesson: dispose the current effect and reset by calling the effect again, performing cleanup for
the old operation before initializing the new.

**`SideEffect` findings.** The resolved 1.11.2 KDoc: the effect is *scheduled* to run when the
current composition completes successfully and applies changes, so objects managed outside the
Composition are not left inconsistent if the composition operation fails; effects always run on
the composition's apply dispatcher, never concurrently with applying changes to the tree, and
always after `RememberObserver` callbacks; and a `SideEffect` runs after every recomposition.
The implementation is `currentComposer.recordSideEffect(effect)`, which the composer records
into the change list to be run when composition changes are applied — the mechanical reason an
abandoned attempt publishes nothing. The documentation page supplies the contrast the Lesson
needs verbatim: it is incorrect to perform an effect before a successful recomposition is
guaranteed, which is the case when writing the effect directly in a composable.

**`produceState` findings.** The resolved 1.11.2 `ProduceState.kt` implements every overload as
`remember { mutableStateOf(initialValue) }` plus `LaunchedEffect(keys) { … }`. The `remember`
carries **no keys**. The KDoc states that the producer is launched on entering the Composition
and cancelled on leaving it, that a key change cancels and re-launches a running producer, and
that the returned `State` conflates values — no change is observable when a value equal to the
old one is set, and observers may only see the latest value if several are set in rapid
succession.

**Exact key-change versus `initialValue` result.** E25-01's PROBE-F was re-run on the current
toolchain with a throwaway JVM Compose probe (`Recomposer`, `BroadcastFrameClock`, a unit
`Applier`, manual frames). Scenario and result: `produceState(initialValue = "INITIAL", key)`
with key `"A"` produced `"A-result"`; the key was changed to `"B"` with the new producer held
before its first write; the producer start log read `[A, B]`, so the producer did restart,
while the returned `State` read `"A-result"` at every observation during that interval — **not**
`"INITIAL"`; releasing the gate produced `"B-result"`. A key change therefore restarts the
producer and does **not** reapply `initialValue`, exactly as E25-01 measured and exactly as the
unkeyed `remember` predicts. This is not stated on the documentation page, and L11.3 teaches it
with the explicit product consequence and the explicit fix — write the loading value as the
first statement of each producer lifetime.

**Conflation wording.** The KDoc settles it, so the authored prose follows the KDoc rather than
a measurement: the returned `State` conflates, an equal write is not an observable change, and
rapid writes may only be seen as the latest value. A confirming probe on the same run had a
producer assign the same loaded value three times; the reading composable executed **twice** in
total — once for `"INITIAL"` and once for `"LOADED"` — reproducing E25-01's PROBE-G. The Lesson
deliberately does not equate this with Flow `conflate()`, `StateFlow`'s contract or
`collectLatest`; it is Compose `State` with current-value, equality-based change semantics.

**Skipping and successful-composition measurements.** The same probe run measured the two
`SideEffect` claims rather than asserting them from inference. A root that recomposed twice
published twice; a child composable whose parameter did not change was skipped on the second
pass and published **once**. L11.2 states only what that supports — a skipped execution
schedules no publication in that pass — and explicitly refuses the broader "SideEffect only runs
when state changes" reading. The abandoned-composition claim is taught from the KDoc and the
`recordSideEffect` mechanism, not from a synthetic abandoned-composition test. The same run also
logged `DisposableEffect` ordering on a key change as `setup:A`, `dispose:A`, `setup:B`, and
`dispose:B` on `composition.dispose()`, confirming that the obsolete registration is released
before the replacement is established.

**`awaitDispose` findings.** `ProduceStateScope.awaitDispose(onDispose)` is declared in the
Compose runtime's `commonMain`, so it is available on every target this project builds. It
returns `Nothing` and is implemented as `suspendCancellableCoroutine<Nothing> {}` inside a
`try`/`finally` that always runs `onDispose`. Its KDoc purpose is awaiting the disposal of the
producer whether it left the composition, the source changed, or an error occurred, and it is
documented for configuring callback-based state producers that do not suspend. L11.4 teaches
that purpose and the four-step subscription shape; it does not teach `suspendCancellableCoroutine`
or `callbackFlow` internals.

**Probes were deleted.** Both probe files lived only under
`shared/src/jvmTest/.../probe/` during authoring and were removed before the final diff;
`git status --short` shows no probe file.

**Flow-versus-Compose-local producer boundary.** L11.4 answers the placement question from
ownership and reuse in a two-column table — more than one consumer, meaning outside Compose,
stream operators as part of the source contract, a lifetime below the UI, or a platform/domain
layer that should expose the observation API argue for a Flow below the UI; an adaptation that
exists for this Compose boundary, a composition-owned lifetime, a required `State` output, or a
reusable stream nothing would reuse argue for the Compose-local producer. `callbackFlow` and
`awaitClose` are named once as the same registration-and-cleanup idea one layer down, with the
link to `lesson_flow_builders_and_callback_adapters`. The Lesson also records the reverse
mistake — routing a single-screen value through layers that add no behaviour — and notes that
choosing the Flow does not remove the Compose-side collection step.

**Distinctions the Unit holds.** `DisposableEffect` is separated from `LaunchedEffect` by what
kind of lifetime the Composition owns — an in-flight suspend operation ended by cooperative
cancellation, against a registration held by an external object and ended by a synchronous
release — not by "one has cleanup"; `lesson_cancellation_cleanup_and_timeouts` keeps coroutine
cleanup. `SideEffect` is taught only as publication after successful composition and explicitly
rejected as an event handler, a coroutine launcher and a place for expensive work, with the
trigger question handed back to Unit 10. `produceState` + `awaitDispose` is separated from
`DisposableEffect` by the output requirement rather than by "callbacks", and L11.4 keeps Unit
10's composition-owned versus event-owned trigger distinction explicit. No mechanism-selection
decision tree was authored: Unit 12 still owns the synthesis, and the only forward reference is
one prose sentence naming a later unit.

**`rememberUpdatedState` interaction.** L11.1's Senior section combines a stable registration
keyed on the external object with a current callback read through `rememberUpdatedState`, and
states the bounded rule — do not tear down and re-register an external observer merely because a
callback changed, but do key on a value when changing it means the registration belongs
elsewhere. Unit 10's mechanics are linked, not retaught.

**Cross-links.** All links are backward. L11.1 →
`lesson_effect_keys_as_dependencies`, `lesson_remember_updated_state`,
`lesson_cancellation_cleanup_and_timeouts`, `lesson_flow_collection_lifetime`. L11.2 →
`lesson_composable_execution`, `lesson_why_effects_are_controlled`,
`lesson_who_owns_the_trigger`. L11.3 → `lesson_launched_effect`, `lesson_collect_as_state`,
`lesson_snapshot_flow`. L11.4 → `lesson_flow_builders_and_callback_adapters`,
`lesson_collect_as_state`, `lesson_snapshot_flow`. One deviation from the intended-graph table
is recorded deliberately: that table pairs L11.3 and L11.4 jointly with
`lesson_flow_builders_and_callback_adapters` and `lesson_snapshot_flow`, and the callback-adapter
link was placed only on L11.4, because L11.4 is the Lesson whose prose names `callbackFlow` and
L11.3 does not. No shipped Lesson was edited, and no forward link to Unit 12 was added.

**Semantic Question review.** The two ACTIVE Questions reachable through `compose_side_effects`
were re-read in full and re-solved against the finished Lessons; there is still no DEPRECATED
Question on this Subtopic, so the pool is unchanged from E25-05 in membership.
`compose_side_effects_001` (FOUNDATION, MULTIPLE) remains correct, correctly mapped and
correctly levelled: the key set is the `LaunchedEffect` composition-scope clause and the
`DisposableEffect` cleanup clause, and the distractors — `SideEffect` running before the
composition is applied, and `rememberCoroutineScope` launching on entry — are both false against
the resolved 1.11.2 source. After Unit 11 its `DisposableEffect` and `SideEffect` clauses are
answerable from taught material for the first time, so the Question is now answerable end to
end by a learner who has finished Units 9, 10 and 11. `compose_launched_effect_key_restart`
remains correct, correctly mapped and correctly levelled; its reasoning is still Unit 9's, and
Unit 11 receives it only structurally through the shared primary concept. Neither Question was
edited and no Question wording was copied into a Lesson.

**Shared practice-pool limitation, unchanged and still recorded.** `compose_side_effects` is the
sole primary concept of Units 9, 10, 11 and future 12, so all of them receive one identical
pool. Unit 11 therefore receives Unit 9's restart Question, Units 9 and 10 receive the
`DisposableEffect` and `SideEffect` clauses Unit 11 now teaches, and any future Unit 11-specific
Question written on this Subtopic will appear in Unit 9's practice too. No dishonest primary
mapping and no invented Subtopic was used to hide this; E25-08 still owns the deliberate
assessment response, and E25-01's four recommendations stand.

**GAP-U11-* status.** All four remain **open** for E25-08, and authored teaching does not create
semantic Question coverage. GAP-U11-A (a missing `onDispose` in a Compose screen and what
accumulates) — L11.1 now teaches the concrete failure, three listeners on one session delivering
every result three times plus the retained references, and nothing assesses it. GAP-U11-B
(`SideEffect` timing as a decision, and what an abandoned or skipped composition means) — L11.2
now teaches it; it is still present in the bank only as a one-clause distractor. GAP-U11-C
(`produceState` at all: restart keys, conflation, and the State not resetting on a key change) —
L11.3 now teaches it with a measured result; still unassessed, and still the strongest ADVANCED
candidate in the epic. GAP-U11-D (choosing between a Flow below the UI and a Compose-local
producer) — L11.4 now teaches the decision; `callback_flow_await_close_registration` still
assesses only the builder contract in `async_reactive`. **No new gap id was created.** Finished
authoring exposed no distinct unrecorded reasoning gap: the `awaitDispose` subscription shape
belongs to GAP-U11-D's Lesson and the `DisposableEffect`-versus-producer choice is the same
placement reasoning, so adding an id for either would be duplication rather than a finding.

**Generated coverage.** `python3 tools/learning_question_coverage.py --write` reports 17 active
Units and 69 active Lessons, and lists Unit 11 as four Lessons, one distinct primary concept and
two reachable FOUNDATION Questions with no APPLIED or ADVANCED coverage. That is structural
reach through `compose_side_effects` only. Semantically, neither reachable Question assesses
registration ownership, cleanup on a key change, publication timing, producer lifetime, the
key-change value, conflation, `awaitDispose` or the placement decision, which is what the
GAP-U11-* entries above record.

**Validation run during authoring.** `python3 -c "import json…"` round-tripped the production
document and confirmed the change is a pure insertion — one added Unit, zero existing Units
altered; `./gradlew :shared:dependencyInsight --configuration jvmRuntimeClasspath --dependency
androidx.compose.runtime:runtime` resolved 1.11.2; the two throwaway JVM Compose probes ran
under `./gradlew :shared:jvmTest --tests …` and were then deleted; the focused suite
`./gradlew :shared:jvmTest --tests "…curriculum.learning.content.*" --tests
"…TopicDetailLearningContentTest" --tests "…LearningUnitPracticeIntegrationTest"` passed after
the count expectations were updated rather than weakened; `python3
tools/learning_question_coverage.py --write` then `--check` reported the snapshot current;
`cd tools && python3 -m unittest test_learning_question_coverage.py` passed 21 tests;
`./gradlew :shared:jvmTest` passed the full common/JVM suite with the `android_ui` journey grown
from 36 to 40 Lessons; `./gradlew :shared:allTests` passed Android host, JVM, iOS simulator, JS
and Wasm targets; and `./gradlew :shared:check` plus `git diff --check` passed on the final
diff. No `iosArm64` device-target run and no external CI run is claimed.

**What E25-07 and Unit 12 may assume.** The learner has now met every E25 mechanism
individually: hoisted screen state and a stateless content boundary (Unit 7), conversion of an
observable stream into Compose `State` and its lifetime (Unit 8), composition-owned suspend work
and effect keys (Unit 9), a current value read without restarting and an event-owned trigger
(Unit 10), and a registration with paired release, an outward publication tied to successful
composition, a composition-scoped state producer and a callback subscription inside one (Unit
11). Every outcome Unit 12's decision Lesson must reach therefore has a Lesson that owns it, and
Unit 12 may link backwards to all of them. Unit 12 still owns: the mechanism-selection decision
itself, transient UI effects, and the delivery question. Two facts Unit 12 should not re-derive
are the `produceState` key-change value and the `SideEffect` successful-composition contract;
both are established here and should be cited rather than restated.

### E25-07 — Unit 12

`unit_production_ui_effects_and_selection`, **Production UI Effects and Mechanism Selection**,
shipped under `android_ui` immediately after `unit_cleanup_synchronization_and_producers`, which
completes this epic's six-Unit instructional sequence. Its three planned Lessons shipped in the
planned order with unchanged identities and titles: `lesson_choosing_a_compose_mechanism`,
**Choosing the Smallest Sufficient Mechanism**; `lesson_transient_ui_effects`, **Rendering State
and Running a Transient Effect**; and `lesson_transient_effect_delivery`, **What Delivery
Guarantee Does This Occurrence Need?** No Question, taxonomy entry, new effect API, Flow or
`Channel` curriculum, event-wrapper or ViewModel event pattern, navigation API, DI or testing
material was added, and no E25-08 work was begun.

**Mappings.** All three Lessons keep `compose_side_effects` as their sole primary Subtopic, with
the plan's supporting mappings unchanged: L12.1 with `compose_state`, `compose_state_hoisting`
and `compose_udf`; L12.2 with `coroutine_scope`, `compose_state` and `sharedflow`; L12.3 with
`sharedflow`, `hot_vs_cold_streams`, `state_ownership` and `stateflow`. No Lesson lists a
Subtopic as both primary and supporting, and every E24 and architecture concept stays supporting
so that no E25 Unit imports another Topic's practice.

**Versions rechecked on 2026-09-15.** `gradle/libs.versions.toml` still declares Kotlin 2.4.10,
Compose Multiplatform 1.11.1, Material 3 1.11.0-alpha07, lifecycle 2.11.0-beta01,
kotlinx.coroutines 1.11.0 and AGP 9.0.1, and `./gradlew :shared:dependencyInsight --configuration
jvmRuntimeClasspath --dependency androidx.compose.runtime:runtime` still resolves
**`androidx.compose.runtime:runtime:1.11.2`**. Nothing changed since E25-06, so no dependency
investigation was repeated.

**Sources.** Unit 12 required far less new research than Units 8–11 because almost every claim is
inherited. The two external pages the wording actually depends on were re-read on 2026-09-15.
*Side-effects in Compose* supplies the over-use caution L12.1's Senior section quotes — effects
are easily overused, and the work done in one should be UI-related and should not break
unidirectional data flow — and its framing of a one-off event such as showing a snackbar or
navigating given a state condition. *UI events* supplies the one delivery claim L12.3 takes from
Android guidance rather than from E24: when the producer outlives the consumer, these solutions
do not guarantee the delivery and processing of those events. The `SharedFlow` and
`MutableSharedFlow.tryEmit` reference pages are cited for the `tryEmit` result contract. **No
runtime probe was run**, because the Unit introduces no new behavioural claim: the `SideEffect`
successful-composition contract and the `produceState` key-change value are cited from E25-06,
and the `tryEmit`-with-no-subscriber and no-replay results are cited from E24's measurements
rather than re-measured.

**L12.1 — the decision.** The Lesson is built from requirements rather than from an API
catalogue. Core states the four decision facts — who owns the state, who owns the trigger, what
lifetime is required, what has to be released — and then an ordered sequence of eight questions
stopped at the first one a requirement answers: is there state to render and who owns it; is an
external observable value driving rendering; what starts imperative or suspend work; does
long-lived work need a current value without a new lifetime; does the Composition acquire
something that must be given back; does non-Compose code need the successfully composed value;
does an external producer have to become Compose `State`; and must the work outlive this
Composition. Practical works three screens from this app's own practice flow in full prose with
code and all four facts each — an explanation panel that needs only local `remember` and a
directly rendered parameter, a countdown that needs `LaunchedEffect(questionId)` *and*
`rememberUpdatedState` for two separate reasons, and a Show-hint tap that needs only a remembered
scope — and then a thirteen-row table whose columns are Requirement, State owner, Trigger owner,
Required lifetime, What is released, Smallest sufficient mechanism.

**Every required outcome is reachable, and each is a table row plus a fact-by-fact derivation:**
ordinary rendering (row 1), local state (row 2), hoisted state (row 3), `collectAsState` (row 4),
`collectAsStateWithLifecycle` (row 5), `LaunchedEffect` (row 6), `rememberUpdatedState` (row 7),
`rememberCoroutineScope` (row 8), `DisposableEffect` (row 9), `SideEffect` (row 10), `produceState`
with `awaitDispose` (row 11), a reusable Flow below the UI followed by ordinary collection (row
12), and "none of these — the owner is outside the Composition" (row 13). Rows 6 and 7 are
deliberately the same screen split across two facts, and row 12 is row 11 with the ownership fact
changed, which is the Lesson's argument that the table is output rather than a lookup.

**Smallest-sufficient treatment.** The principle is stated in Core ("an unnecessary effect is not
free"), demonstrated in Practical by two screens whose correct answer contains no effect at all,
and enumerated in Senior as six real over-reaches: writing an already-available value into state
from an effect; introducing a screen-level owner for a value one composable owns; collecting a
stream by hand in a `LaunchedEffect`; turning an event into persistent state so an effect can
watch it; reaching for `produceState` because a callback is involved when the registration
produces no `State`; and adding a key to keep a value current. A `COMMON_MISTAKE` callout rejects
the power-ranking reading of the mechanisms outright.

**The negative result.** Senior treats "must the work outlive the screen" as a correct outcome
rather than a dead end, names the committed-submission, durable-save, in-flight-upload and
payment cases, and explains it from the mechanism contracts — a remembered scope is cancelled
when the call leaves the Composition and an effect when its position does, which is exactly wrong
for work that must not stop. It then stops: the owner's identity, construction and placement are
named as the architecture curriculum's decision.

**L12.2 — state against occurrence.** Core separates a condition, which is rendered and repeats
harmlessly, from an occurrence, which is executed and does not; lists current-state examples
(selected question, loading, a validation message that should stay visible, the active filter)
and occurrence examples (snackbar, focus, scroll or animation, a navigation request); and refuses
to classify from widget type, using a dialog that is a condition in one requirement and an
occurrence in another. **The duplicate-fire scenario** is the issue's shape rather than Unit 10's:
`EditorScreenState(draft, saved)` with `if (state.saved) { LaunchedEffect(state.saved) { … } }`.
The concrete behaviour taught is that the branch entering the Composition with `saved == true`
starts an effect lifetime, and every later entry with the same value — a rebuilt screen collecting
the current state again, a return while the flag still stands, the branch leaving and re-entering
— starts another one and runs the snackbar again, although the learner saved once. The Lesson
states explicitly that this is not a defect in the API or the key: the value answers "is a
successful save currently recorded?" while the requirement asked "did a save just happen?", and no
key list expresses a difference the value does not carry. A three-row table contrasts repeating
`isSaving`, repeating `errorMessage` and repeating `saved`.

**Transient mechanisms and why.** L12.2 refuses both slogans: a `COMMON_MISTAKE` callout rejects
"transient effects use `LaunchedEffect`" and "transient effects use `rememberCoroutineScope`"
together, and the trigger question from Unit 10 remains the decider — a tap that scrolls, focuses
or confirms is event-owned and launched from a remembered scope, while a composition state that
genuinely means "this screen now represents something that should be announced" is
composition-owned and keyed. **Navigation** appears as exactly one paragraph: a navigation request
is one more piece of UI work that happens once, whose duplication is visible as a duplicated
destination, and destinations, routes and the back stack are named as the navigation curriculum's
subject with nothing in the reasoning depending on them.

**No overcorrection, and the handoff.** Senior rejects "one-off things must never be state" using
a completed payment that must render correctly after the app is reopened, and identifies the
editor screen's actual defect as a stored fact wired to a mechanism that executes. It then hands
off: before turning an occurrence into transient UI execution, ask what has to be true if nobody
is there to execute it.

**Architecture boundary in L12.2.** `architecture_ui_event_consumption` was read in full before
authoring. L12.2 demonstrates the same class of failure from the Compose consumer side and
deliberately does not reproduce that Question's modelling solution: it names the requirement
("something has to decide what the occurrence requires") without prescribing event-as-UI-state,
consumption marking, or any holder. The ownership boundary is stated in prose — whether the
occurrence is carried in screen state, recorded as handled, or delivered another way is an
application-architecture decision the architecture curriculum owns.

**L12.3 — the delivery question.** Core insists on the order (requirement first, mechanism
second), makes the absence concrete with a backgrounded practice session whose result is produced
while no screen is composed, and asks the six requirement questions: must it still happen, may it
be discarded, may a later screen observe it, may more than one observer act on it, is repeating it
harmful, and does anything have to record that it was handled. It notes that "events are being
lost, so add replay" answers only the third and quietly changes the answer to the fifth.

**Exact E24 facts applied without reteaching.** Two, both cited rather than re-derived. First,
`tryEmit` returning `true` on an unbuffered shared flow with no subscriber means the flow accepted
the call and the value is gone at once — E24 measured it on this project's toolchain, and L12.3
adds only the Compose-side consequence that a UI effect cannot execute what the UI never received,
so a log line written from a `true` result is evidence about the producer and not about the
screen. Second, retention: replay changes what a later subscriber can recover and does not make an
occurrence delivered or delivered once, because a recreated screen is a new collector that reads
what was retained. **Replay is explicitly not taught as the fix for a lost event**; a
`COMMON_MISTAKE` callout names the trade (a missed confirmation becomes a repeated one) and says a
repeated charge or destination is worse than a missed toast. No API matrix, no buffer taxonomy and
no second `SharedFlow` lesson was written.

**Three outcome classes.** A table maps the UI-absent requirement to what the occurrence is and
where the answer lives, followed by one paragraph each. *Ephemeral UI-only* — a score animation, a
non-essential confirmation, a focus request — where "nothing needs to happen" is a decision, and
the previous Lesson's mechanisms suffice. *Current truth* — "there is an unfinished practice
session", "the last submission failed" — reclassified as state, with the stream curriculum's
current-value reasoning named and **no holder prescribed**. *Must survive absence* — persistence,
acknowledgement, queueing, retries, a durable record or guaranteed later handling — declared
outside the Compose boundary, supported by the Android UI-events statement that solutions of this
shape do not guarantee delivery and processing when the producer outlives the consumer, and then
stopped.

**Slogan corrections.** Senior retires "one stream type is for state and another for events" by
saying which half is right and why the other half promises a guarantee no broadcast stream
provides, retires "a channel is for events" as the same mistake with a different type, and rejects
treating a broadcast stream as the standard one-off-event mechanism as the same claim with the
reasoning removed. It then states the general form — a stream type describes delivery, a
requirement describes handling, and no description of delivery answers a question about handling —
and lists the deciding dimensions as E24's, not this Unit's. **The consumer-side distinction is
explicit**: the stream Lessons ask what an abstraction delivers, and L12.3 asks whether that
delivery satisfies the requirement given that this UI exists for only part of the time.

**E26 endpoint.** L12.3 ends on a sentence rather than an architecture — choose an owner whose
lifetime and delivery guarantees actually match the requirement — and names which owner, how the
occurrence is recorded, how handling is marked and how the UI consumes it as the architecture
curriculum's questions. No event wrapper, consumable-event class, MVI effect channel, ViewModel
`Channel` pattern, event bus, persistent queue, repository event store, acknowledgement design,
durable event schema or reducer appears anywhere in the Unit.

**Cross-links.** All links are backward. **L12.1 links to all nineteen earlier E25 Lessons** in
production order — the four of Unit 7, the four of Unit 8, the four of Unit 9, the three of Unit
10 and the four of Unit 11 — plus `lesson_work_outside_composition`, which the intended-graph
table requires. One addition beyond that table is recorded deliberately: `lesson_snapshot_flow` is
appended as the twenty-first link, because that shipped Lesson's forward pointer says the effect
APIs as a family and how to choose between them are a later unit's subject, and L12.1 is the
Lesson that fulfils it. `lesson_snapshot_flow` itself was **not** edited, as E25-01 directed. L12.2
links to `lesson_screen_state_and_ui_events`, `lesson_launched_effect`,
`lesson_remember_coroutine_scope` and `lesson_who_owns_the_trigger`. L12.3 links to
`lesson_transient_ui_effects`, `lesson_lifecycle_aware_collection`, `lesson_shared_flow` and
`lesson_choosing_a_stream_abstraction` — the two E24 arguments the plan requires, plus the two E25
Lessons whose reasoning the delivery question actually uses, and nothing more, so
`relatedLessonIds` does not become a bibliography. No shipped Lesson was edited by this issue.

**Semantic Question review.** The two ACTIVE Questions reachable through `compose_side_effects`
were re-read in full and re-solved against the finished Lessons, and the pool is still exactly
`compose_side_effects_001` and `compose_launched_effect_key_restart` — current `main` added
nothing to this Subtopic and there is still no DEPRECATED Question on it.
`compose_side_effects_001` (FOUNDATION, MULTIPLE) remains correct, correctly mapped and correctly
levelled; Unit 12 teaches none of its four clauses as new material and receives it structurally
through the shared primary concept. `compose_launched_effect_key_restart` (FOUNDATION) remains
correct and is still Unit 9's reasoning. **Neither assesses anything Unit 12 teaches**: neither
requires choosing between mechanisms, distinguishing a rendered condition from an executed
occurrence, or reasoning about delivery when the UI is absent. The five adjacent Questions the
issue named were also re-read and none is Unit 12 practice — `durable_state_vs_one_off_event`
(`architecture`/`state_ownership`) owns the durable-versus-consumable modelling decision;
`architecture_ui_event_consumption` (`architecture`/`mvi`) owns the replayed-navigation
consumption failure; `shared_flow_try_emit_true_is_not_delivery` and
`shared_flow_replay_late_subscriber` (`async_reactive`/`sharedflow`) own the emitter-side
`tryEmit` and replay contracts; and `stream_choice_cannot_supply_a_delivery_guarantee`
(`async_reactive`/`hot_vs_cold_streams`) owns the "write it where it outlives the process"
conclusion. Unit 12 bridges to all five and duplicates none of their reasoning; **no Question was
edited**, and no Question's wording was copied into a Lesson.

**GAP-U12-* status. All three remain open for E25-08**, because teaching a concept does not create
assessment. GAP-U12-A (choosing a mechanism from a stated requirement across the whole family) —
L12.1 now teaches it with thirteen reachable outcomes and the four decision facts, and nothing
assesses it; still the highest-priority Unit 12 gap. GAP-U12-B (the Compose-side distinction
between rendering current state and executing a transient effect) — L12.2 now teaches it with a
concrete duplicate-fire behaviour, and the adjacent architecture Questions remain supporting-only,
so they create no Unit practice. GAP-U12-C (what a transient occurrence requires when the UI is
absent, posed from the Compose side) — L12.3 now teaches it as a decision, and E24 still assesses
only the emitter's half. **No new gap id was created.** Finished authoring exposed no distinct
unrecorded reasoning: the smallest-sufficient principle and the "no composition-owned mechanism"
outcome are both part of GAP-U12-A's decision, and the emission-is-not-delivery and replay
corrections are both part of GAP-U12-C's, so adding ids for them would be duplication rather than
a finding.

**Final Units 9–12 shared-pool shape, now fully realised.** `compose_side_effects` is the sole
primary concept of fifteen Lessons across four Units — 4 in Unit 9, 3 in Unit 10, 4 in Unit 11 and
3 in Unit 12 — so **all four Units receive one identical two-Question practice pool**, and any
Question written on this Subtopic for any of them lands in all four. Unit 12's own reasoning is
the sharpest case in the epic, because mechanism selection across the family is by construction
unanswerable from Unit 9 alone and yet appears in Unit 9's practice. **This is not a defect
E25-07 may fix**: no taxonomy id was invented, L12.3 was not re-mapped primarily to `sharedflow`,
L12.1 was not re-mapped to an earlier Compose primary, and no Question was moved for routing.
E25-01's four recommendations stand unchanged and E25-08 owns the deliberate response.

**E25 instructional total verified against production, not against this plan.** The shipped
document now holds **18 active Units and 72 active Lessons**, of which E25's six Units hold
**22 Lessons**: Unit 7 four, Unit 8 four, Unit 9 four, Unit 10 three, Unit 11 four, Unit 12 three.
That matches the count E25-01 fixed. The `android_ui` production order is Units 1–6 unchanged,
then `unit_production_screen_state_and_udf`, `unit_observable_state_collection`,
`unit_effect_lifecycle_and_launched_effect`, `unit_latest_values_and_event_driven_work`,
`unit_cleanup_synchronization_and_producers`, `unit_production_ui_effects_and_selection`. No
earlier Unit was reordered, no Lesson id is duplicated, and the JSON change is a pure insertion:
one added Unit, zero existing Units altered, verified by comparing the parsed `units` list before
and after with the new entry removed.

**Generated coverage.** `python3 tools/learning_question_coverage.py --write` reports **18 active
Units and 72 active Lessons**, and lists Unit 12 as three Lessons, one distinct primary concept,
and two reachable FOUNDATION Questions with no APPLIED or ADVANCED coverage. The full E25 sequence
appears in the report in production order. That is structural reach through `compose_side_effects`
only; semantically neither reachable Question assesses mechanism selection, the state-versus-
occurrence distinction, or the delivery requirement, which is what the GAP-U12-* entries record.
Nothing in the snapshot was hand-edited.

**Validation run during authoring.** A `python3` round-trip of the production document confirmed
the pure insertion described above; `./gradlew :shared:dependencyInsight --configuration
jvmRuntimeClasspath --dependency androidx.compose.runtime:runtime` resolved 1.11.2; the focused
suite `./gradlew :shared:jvmTest --tests "…curriculum.learning.content.*" --tests
"…TopicDetailLearningContentTest" --tests "…LearningUnitPracticeIntegrationTest"` passed after the
count expectations were updated rather than weakened; `python3 tools/learning_question_coverage.py
--write` then `--check` reported the snapshot current; `cd tools && python3 -m unittest
test_learning_question_coverage.py` passed 21 tests; `./gradlew :shared:jvmTest` passed 1382 tests
with the `android_ui` journey grown from 40 to 43 Lessons and the learner-journey totals from 68
to 71; `./gradlew :shared:allTests` passed Android host, JVM, iOS simulator, JS and Wasm targets
(448 tests on each non-JVM target, with `iosSimulatorArm64Test` re-run explicitly rather than
accepted as up to date); and `./gradlew :shared:check` plus `git diff --check` passed on the final
diff. **No `iosArm64` device-target run and no external CI run is claimed.**

**What E25-08 inherits.** Every E25 Lesson now ships, so the assessment pass can be read against
finished prose rather than against a plan. The open gaps are GAP-U7-A/B/C, GAP-U8-A/B/C,
GAP-U9-A/B/C/D, GAP-U10-A/B/C, GAP-U11-A/B/C/D and GAP-U12-A/B/C, and the structural constraint
that shapes all of the Units 9–12 entries is the single shared `compose_side_effects` pool
described above. E25-09 still owns the integration review, the terminology sweep across the six
Units, and the re-reading of the five shipped forward pointers — of which the `lesson_snapshot_flow`
one is now fulfilled by L12.1's backward link.
