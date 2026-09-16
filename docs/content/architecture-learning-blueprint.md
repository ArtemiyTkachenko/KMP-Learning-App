# Application Architecture Learning Blueprint

## Purpose

This is the complete learning map for application architecture and state ownership,
produced under `docs/content/learning-content-authoring.md` before any production Lesson
for the subject is authored. It is the third Topic mapped under that contract, after
`docs/content/compose-learning-blueprint.md` and
`docs/content/coroutines-flow-learning-blueprint.md`.

It is a **plan, not content**. No Lesson text is authored here, and nothing in this file is
a runtime artifact. Authoring proceeds Unit by Unit against this map, through E26-02 to
E26-07.

Home Topic: `architecture` (Application Architecture & Design Principles).

Scope: 6 Learning Units, 29 planned Lessons, plus explicit Reference and Exclude decisions,
a misconception matrix, a terminology register, and a record of concepts the current
assessment taxonomy cannot express.

The confirmed authoring plan — stable identities, the taxonomy inventory, prerequisites,
the semantic Question review, the repository and multiplatform findings, and the practice
routing model — is in
[`architecture-units-1-6-plan.md`](architecture-units-1-6-plan.md). This blueprint holds
objectives, depth layers and editorial decisions; that document holds what the review had
to settle.

## The Learner This Subject Is Written For

The reader has already worked through the shipped Compose and Coroutines and Flow Units.
They can hoist state, describe recomposition, name what a `CoroutineScope` owns, choose a
stream abstraction from its delivery contract, and split a screen into a stateful screen
composable and a stateless content composable. The Compose curriculum has taken them to a
screen-level owner and then deliberately refused to design it.

What they cannot yet do — and what an interview asks them to do — is justify a boundary.
They can name MVVM but not say where state lives in it; they can add a repository but not
say what would be lost without one; they can recite that Clean Architecture has three
layers, which is not what Clean Architecture says. The failure mode is not ignorance of
patterns. It is that every structural decision arrives as a convention rather than as an
answer to a question about ownership, dependency and lifetime.

## How to Read This Blueprint

Every planned Lesson records:

- **Objective** — what the learner should be able to do afterwards.
- **Core / Practical / Senior** — the concepts belonging to each depth layer, per Rule 6 of
  the authoring contract. A missing Senior line means deeper material would be artificial.
- **Primary** — Subtopic IDs the Lesson is responsible for teaching thoroughly.
- **Supporting** — Subtopic IDs the Lesson explains only far enough to stay understandable,
  including cross-Topic bridges. Cross-Topic IDs are annotated with their owning Topic.
- **Notes** — Teach/Bridge/Reference/Exclude decisions, prerequisites, and pointers to
  deeper content.

Every Topic and Subtopic ID below is a real ID from the bundled curriculum
(`shared/src/commonMain/composeResources/files/curriculum/initial_curriculum.json`) and was
checked `ACTIVE` on 2026-09-15. Concepts with no exact Subtopic are recorded in
[Taxonomy gaps](#taxonomy-gaps-concepts-with-no-exact-assessment-subtopic) rather than
given invented IDs. **This blueprint does not change the question taxonomy.**

The `L1.1` style labels are positional reading aids for this document and its companion
plan. They are **not** identities; the stable ids are in the plan's identity tables and are
never numbered by position.

## The Central Question

The subject is not "which architecture pattern should I use". Pattern names are answers,
and a learner who collects answers without the question cannot defend any of them. Every
Unit below is built so the reader can work through one question about code in front of
them:

> **Who owns this responsibility, what may depend on what, how long must it live, and what
> boundary earns its cost?**

Its four parts are the recurring shape of the whole subject:

- **Ownership.** Which component is responsible for this state, this decision, this data?
  Who is allowed to change it, and through what path?
- **Dependency.** Which component may know that another exists? Which direction does the
  source dependency point, and which side is allowed to change without the other noticing?
- **Lifetime.** How long must this thing live, and what ends it? What must survive
  recomposition, navigation, configuration recreation, process death — and what must
  survive none of them?
- **Cost.** What does this boundary buy, what does it cost in indirection, mapping, naming
  and navigation, and is the exchange worth making for *this* feature?

Architecture is not the number of folders or classes in a project. Layers, repositories,
ViewModels and use cases are candidate answers to those four questions, and each of them
has to earn its place against them.

A second question runs underneath the first and closes the subject in Unit 6:

> **How much architecture does this feature actually need?**

## Unit Order and Why It Matters

1. Architecture as Responsibilities and Boundaries
2. Screen State Holders, ViewModel and UI State
3. Repositories, Data Ownership and Single Source of Truth
4. Domain Logic, Use Cases and Dependency Direction
5. MVP, MVVM and MVI Responsibility Models
6. State, Events, Lifetime and Architecture Selection

The order encodes conceptual dependencies, not convenience:

- **Boundaries (1) before any named component.** Every later Unit judges a specific
  structure — a state holder, a repository, a use case, a pattern — against the same test.
  Introduced after those structures, the test becomes a retrospective justification for
  choices the learner has already been taught to make by default.
- **The screen owner (2) before the data layer (3).** The reader arrives from the Compose
  curriculum standing exactly at the screen boundary, with an owner they can name and
  cannot describe. Continuing from where they are keeps the whole subject anchored to a
  screen they already understand; opening with the data layer starts the subject at the
  far end of a pipeline the learner has never traced.
- **Data ownership (3) before domain logic (4).** "Does this feature need a domain layer?"
  cannot be asked before there is a data layer for the domain layer to sit above, and the
  best argument against a pass-through use case is a repository whose API the reader has
  just seen designed.
- **All four structural Units (1–4) before the patterns (5).** This is the ordering
  decision the Unit exists for. A learner who meets MVP, MVVM and MVI before they can
  reason about ownership and dependency learns three diagrams; a learner who meets them
  afterwards recognises three different answers to questions they already hold. The Unit
  then adds nothing structurally new — it re-reads what they know under three vocabularies
  and shows that real codebases borrow from all three.
- **Selection (6) last.** It is the Unit where the subject's real decision — what must be
  state, who owns it, how long must it survive, how much structure does this need — can be
  posed with every input available. Placed earlier it degrades into a checklist.

The sequence deliberately does **not** open with a layer diagram, and deliberately does not
open with `ViewModel`. Both are where most readers first meet the subject, and both teach
the conclusion while hiding the reasoning.

---

## Unit 1 — Architecture as Responsibilities and Boundaries

**Purpose:** establish what application architecture actually decides, and the test a
boundary has to pass, before any named component or layer stack exists.
**Prerequisites:** none inside this subject. The shipped Compose Lesson
`lesson_state_hoisting` supplies the reader/writer/lifetime ownership test at composable
scale; this Unit generalises it and says so rather than re-deriving it.

The Unit opens on a **changing requirement**, not on a diagram: a feature has to change,
and the question is which parts should have to change with it. Every concept below is
introduced as something that answers that question.

#### L1.1 — What Architecture Actually Decides

- **Objective:** say what application architecture decides, and distinguish it from package
  structure and from library choice.
- **Core:** architecture is the assignment of responsibility, ownership and dependency
  direction across a codebase; the questions it answers are who owns this, who may know
  about this, how long does this live, what changes when a requirement changes; a
  requirement change is the concrete event that makes any of it observable.
- **Practical:** the same feature expressed as two package trees with identical
  dependencies — the trees differ and the architecture does not; a project with `ui`,
  `domain` and `data` packages in which the UI constructs a DAO, and why the folder names
  are then decoration; "we use Koin, so our architecture is dependency injection" and
  "we use Compose, so our architecture is MVVM" as category errors.
- **Senior:** why architecture is judged by the cost of change rather than by structure —
  two codebases with identical class diagrams can differ completely in how many files a
  new requirement touches, and that number is the thing being optimised.
- **Primary:** `separation_of_concerns`
- **Supporting:** `architecture_tradeoffs`, `layered_architecture`,
  `android_modules` (build_delivery)
- **Notes:** **Teach** the definition and the distinction from structure and from
  framework. **Exclude** architecture history, the C4 model, and any taxonomy of
  architectural styles — none of it changes a decision an Android engineer makes.
  Modularization is named in one sentence and **deferred whole** to the build and
  modularization curriculum.

#### L1.2 — Responsibility, Cohesion and What Changes Together

- **Objective:** decide whether two pieces of behaviour genuinely change for different
  reasons, and therefore whether they belong apart.
- **Core:** a responsibility is a reason to change; cohesion is how much of one thing a
  component does; coupling is how much one component must know about another; splitting
  two responsibilities that always change together produces two files and one concept.
- **Practical:** a class that formats a date, decides whether an item is overdue, and
  writes it to storage, split by asking which requirement changes which part; the reverse
  failure, where a "utils" package collects unrelated functions that share nothing but a
  location; how ownership of a value — who may write it — is the sharpest form of the
  question.
- **Senior:** why "one class, one responsibility" is unfalsifiable as usually stated, and
  what makes it usable: a responsibility is only identifiable relative to a set of
  plausible future requirements, so the split is a bet about which changes are coming, not
  a fact about the code.
- **Primary:** `separation_of_concerns`
- **Supporting:** `solid`, `architecture_tradeoffs`, `interface_boundaries`
- **Notes:** **Reference** for SOLID as a named set: the single-responsibility and
  dependency-inversion ideas are taught here and in L1.4/L4.4 as reasoning, and the
  acronym is named once so the reader recognises it in an interview. **Exclude** a Lesson
  that walks all five principles in order — it is an API catalogue in disguise.

#### L1.3 — Which Way May This Dependency Point?

- **Objective:** for a given pair of components, say which may know about the other, and
  what changes when the arrow is reversed.
- **Core:** a dependency is one component naming another; direction is a design decision
  and not a consequence of call order; a component that depends on another inherits its
  reasons to change; stable things may be depended on, volatile things should not be.
- **Practical:** a data class shared between a screen and a network client, and what
  happens to the screen when the wire format changes; a "helper" that the UI and the data
  layer both call, and which one it should belong to; the difference between *calling*
  something and *depending* on it — a callback lets data flow back without the dependency
  arrow turning around.
- **Senior:** why cycles are the observable symptom rather than the disease, and why
  direction matters most exactly where the two sides change at different rates.
- **Primary:** `dependency_direction`
- **Supporting:** `interface_boundaries`, `solid`,
  `layered_architecture`, `module_dependency_direction` (build_delivery)
- **Notes:** **Teach** direction as a decision. Inversion — who *defines* the abstraction,
  and what it costs — is named here and **owned by L4.4**, so this Lesson must not turn
  into a dependency-inversion Lesson. Build-level module dependency is **Bridge** only,
  one sentence, deferred to the build curriculum.

#### L1.4 — When an Interface Is a Boundary, and When It Is Only Indirection

- **Objective:** state the condition under which an abstraction is a real boundary, and
  recognise one that is not.
- **Core:** an interface does not decouple anything by itself; what decouples is the
  dependency pointing at an abstraction that the *consumer* owns, so the volatile side can
  change without the stable side noticing; an interface with exactly one implementation
  that changes whenever that implementation changes has inverted nothing.
- **Practical:** `interface UserRepository` with one implementation, both edited in the
  same commit every time, and what it actually bought; the same interface where a second
  implementation, a different source policy or a genuinely separate change rate exists;
  the honest version of the testing argument — a fake is easier to write against an
  interface, which is a consequence of a boundary rather than a reason to create one.
- **Senior:** the cost side stated concretely — an extra name to learn, an extra file, one
  more navigation hop in an IDE, and a contract that now has to be designed rather than
  discovered; and the case for adding the interface later, when the second implementation
  is real, rather than in anticipation.
- **Primary:** `interface_boundaries`
- **Supporting:** `dependency_direction`, `solid`, `architecture_tradeoffs`,
  `test_doubles` (testing)
- **Notes:** **Teach** the misconception correction directly — "interfaces decouple" and
  "interfaces are for testing" are both addressed and both replaced with the ownership and
  volatility test. Test implementation is **Exclude**; the testing curriculum owns fakes,
  mocks and test strategy, and this Lesson shows no test.

#### L1.5 — Layers as One Answer, and What They Cost

- **Objective:** evaluate whether a proposed layer earns its cost for a given feature.
- **Core:** a layer is a group of components that share a level of responsibility and a
  dependency rule; layering is one possible answer to the boundary question and not the
  definition of architecture; more layers is not better architecture, because every layer
  is a boundary that must earn what it costs.
- **Practical:** the cost of a layer made concrete — an extra model and its mapping, a
  forwarding class, another name for the same concept, one more file to open when reading
  the feature end to end, and one more place a change lands; then the same layer on a
  feature where it pays, because the two sides genuinely change independently; testability
  as a consequence of the boundary rather than as its justification.
- **Senior:** why a layer that only forwards is worse than no layer — it adds the cost of a
  boundary while providing none of the isolation, and it teaches the next engineer that the
  boundary is a formality; and why a logical layer is not a Gradle module.
- **Primary:** `layered_architecture`, `architecture_tradeoffs`
- **Supporting:** `separation_of_concerns`, `android_modules` (build_delivery),
  `modularization_tradeoffs` (build_delivery)
- **Notes:** **Teach** layering as one answer with stated fit conditions. **Bridge**, in
  one bounded statement: a logical layer is a responsibility boundary and a Gradle module
  is a build boundary, and the two are chosen for different reasons. Module boundaries,
  build graphs, convention plugins and build performance are **Exclude** — E29's subject.
  The specific data-layer model boundary (transfer, storage, domain) is named here as the
  clearest example of mapping cost and is **owned by L3.5**.

---

## Unit 2 — Screen State Holders, ViewModel and UI State

**Purpose:** design the owner the Compose curriculum stopped at — what it owns, what it
must not know, how its state is modelled, and how long it lives.
**Prerequisites:** E23 `lesson_state_hoisting` and `lesson_immutability_vs_stability`;
E25 `lesson_classes_of_screen_state`, `lesson_stateless_screen_content`,
`lesson_screen_state_and_ui_events` and `lesson_screen_state_owner_boundary`; Unit 1 of
this subject.

This Unit picks up exactly where `lesson_screen_state_owner_boundary` stops. That Lesson
establishes that the Composition talks to an owner outside it, that moving state across
that boundary changes the owner and grants no persistence, and that what survives is
decided by the owner's own lifetime. It then says in as many words that constructing the
owner, dividing responsibilities, modelling complete UI states and selecting a pattern are
architecture decisions it does not make. **They are this Unit's and Unit 5's.**

#### L2.1 — What a Screen State Holder Is Responsible For

- **Objective:** state the responsibility of a screen-level state holder without naming a
  framework class, and identify a state holder that is not a ViewModel.
- **Core:** a state holder is a responsibility — own the state a screen renders, accept the
  intentions the UI reports, turn data and domain results into that state, and be the one
  place the state changes; `ViewModel` is one platform-provided implementation of that
  responsibility, chosen when the lifetime it offers is the lifetime required; a plain
  class can hold the same responsibility.
- **Practical:** a state holder that is deliberately not a `ViewModel` — an app-scoped
  holder shared by several destinations, and a UI-element holder created with `remember` —
  beside a screen-level `ViewModel`, with the lifetime requirement that decided each; the
  claim that all production state belongs in a ViewModel, checked against a screen where
  one value correctly stays local; what the holder must *not* know — no UI instance, no
  rendering decision, no framework `Context` held for formatting.
- **Senior:** why the responsibility is worth naming separately from the class: once the
  responsibility is the unit of thought, "which holder" becomes a lifetime question with
  several legitimate answers, and a team that has only the word `ViewModel` has only one.
- **Primary:** `state_ownership`
- **Supporting:** `compose_state_hoisting` (android_ui), `viewmodel_lifecycle`
  (lifecycle_navigation), `kmp_lifecycle_viewmodel` (kmp), `separation_of_concerns`
- **Notes:** **Bridge** to the shipped Compose material rather than repeating it: hoisting
  and the three classes of screen state are already taught and are *applied* here.
  **Exclude** construction and injection — E27 owns them; an example may construct an
  owner however is clearest and must not present the construction as the lesson.

#### L2.2 — The ViewModel Owner: Lifetime Is Not Persistence

- **Objective:** for a named event, say whether the owner and its state survive, and name
  the fact that decided it.
- **Core:** a `ViewModel` does not survive anything on its own — its `ViewModelStore` owner
  does, and the ViewModel survives exactly what that owner survives; clearing is what ends
  it; persistence is a separate mechanism that reconstructs a value after the owner is
  gone.
- **Practical:** the lifetime ladder walked once, for one screen — recomposition, the
  composable leaving composition, configuration recreation, navigating forward, popping the
  destination, process death — with the owner named at each rung; the three concepts kept
  apart, since ownership says who may change a value, lifetime says when the owner
  disappears, and persistence says how the value comes back afterwards; saved-state
  mechanisms named as the answer to the last rung and bounded there.
- **Senior:** why "the ViewModel survives the screen" is an unsafe sentence in a
  multiplatform codebase: the guarantee belongs to whatever host supplies the owner, and
  the same shared class gets a different lifetime on each target.
- **Primary:** `state_ownership`
- **Supporting:** `viewmodel_lifecycle`, `configuration_changes`, `process_death`,
  `saved_state` (all lifecycle_navigation), `kmp_lifecycle_viewmodel` (kmp)
- **Notes:** **Teach** the correction that a ViewModel is not a persistence mechanism.
  **Bridge** saved state and configuration changes as bounded supporting context only —
  Activity and Fragment lifecycle, navigation APIs, back-stack mechanics and
  `SavedStateHandle`'s API surface belong to the lifecycle and navigation curriculum.
  **Exclude** predictive back and any lifecycle-callback table.

#### L2.3 — Modelling the Current UI State

- **Objective:** choose a shape for a screen's state from what the screen must be able to
  represent, and say what each shape costs.
- **Core:** the screen renders one immutable current value; how that value is modelled is a
  design decision with real trade-offs, and there is no universal shape; the decisive
  question is which combinations must be representable and which must not.
- **Practical:** the same screen requirement modelled twice — one data class with
  independent fields, one sealed hierarchy of mutually exclusive states — compared on
  loading, content and error, partial content, stale data shown beside a failure,
  incremental loading, and how much a renderer has to branch; a state region that fails
  independently of the screen, modelled as a nested value rather than promoted to a fourth
  top-level state, and the reason; representable-but-impossible combinations — `isLoading`
  true beside a populated list and a non-null error — shown as a real defect, with the cost
  of eliminating them acknowledged.
- **Senior:** why "make illegal states unrepresentable" is a direction rather than a rule:
  every impossible combination removed adds a transition to express, and a hierarchy that
  perfectly encodes exclusivity can make an ordinary partial update awkward. The decision
  is made per screen, from the product requirement, not from a preference for a Kotlin
  construct.
- **Primary:** `state_ownership`
- **Supporting:** `kotlin_sealed_types` (kotlin_language), `error_modeling`,
  `compose_stability` (android_ui), `compose_state` (android_ui)
- **Notes:** **Teach** the comparison. **Exclude** the claim that sealed hierarchies are
  "more MVI" — pattern vocabulary is Unit 5's and asserting it here would teach the label
  instead of the trade-off. Error representation *at the data boundary* is **owned by
  L3.5**; this Lesson models error only as something the screen must render.

#### L2.4 — State Out, Intentions In

- **Objective:** design the holder's public surface so that state leaves it read-only and
  every change enters through one named path.
- **Core:** the current state is exposed as an immutable, read-only value; the UI reports
  what the user intended, not what the state should become; the holder decides what an
  intention means; this is unidirectional data flow one level above the composable tree.
- **Practical:** a holder exposing its mutable state container directly, and the three
  concrete failures — a second write path, an invariant that any caller can break, and a
  change with no single place to look for it; the same holder with a read-only exposure and
  named intention functions; why an intention is named for what the user did rather than
  for the assignment it causes, and what that buys when the requirement changes.
- **Senior:** where the boundary is genuinely debatable — a form screen whose every
  keystroke becomes an intention, against one that owns its draft locally and reports only
  submission — and how to decide from who else must react to the value.
- **Primary:** `unidirectional_data_flow`
- **Supporting:** `state_ownership`, `compose_udf` (android_ui), `stateflow`
  (async_reactive)
- **Notes:** **Bridge** to the shipped `lesson_state_down_events_up` and
  `lesson_screen_state_and_ui_events`, which own the composable-tree half. This Lesson must
  not duplicate them; it applies the same rule at the application boundary, where the
  reasons differ — the write path crosses out of the UI layer here. **Exclude** reducers
  and intent-as-a-type: named in one clause and owned by L5.4.

#### L2.5 — Work Whose Lifetime Is the Owner's

- **Objective:** decide whether a piece of work belongs in a scope that ends with the
  screen's owner.
- **Core:** an owner that starts work owns a scope whose lifetime is its own; work launched
  there is cancelled when the owner is cleared; that is correct exactly when the work's only
  consumer is the screen.
- **Practical:** `viewModelScope` as the concrete instance of that shape — a
  `SupervisorJob` on the main dispatcher, cancelled when the ViewModel is cleared — applied
  rather than re-derived from the coroutine curriculum; a load whose result only this screen
  renders, against an upload the user expects to finish after they navigate away, and why
  the second one having the same owner is the defect; the honest conclusion that the second
  needs an owner with a different lifetime, and that naming which one is Unit 6's decision.
- **Senior:** why a scope injected from outside is a different design than a scope the owner
  creates — the injected one makes the lifetime a decision someone made, and the created one
  makes it an accident of where the code was written.
- **Primary:** `state_ownership`
- **Supporting:** `lifecycle_coroutines` (async_reactive), `coroutine_scope`
  (async_reactive), `viewmodel_lifecycle` (lifecycle_navigation),
  `background_api_selection` (background_work)
- **Notes:** **Bridge** to E24's `lesson_coroutine_scope_ownership`, which owns scopes,
  jobs and cancellation; this Lesson applies that model to one owner. **Exclude** coroutine
  builders, cancellation mechanics, dispatchers and every background-scheduling API —
  WorkManager is named once as "not this" and deferred.

---

## Unit 3 — Repositories, Data Ownership and Single Source of Truth

**Purpose:** decide who is authoritative for application data, what consumers may know
about its origin, and which types are allowed to cross the boundary.
**Prerequisites:** Units 1 and 2; E24 `lesson_why_flow`, `lesson_state_flow` and
`lesson_choosing_a_stream_abstraction` for the observable-API Lesson.

#### L3.1 — What a Repository Is Responsible For

- **Objective:** say what a repository owns, and recognise a feature where adding one buys
  nothing.
- **Core:** a repository exposes application data to the rest of the app, centralises
  changes to it, resolves conflicts between sources, and hides where the data came from; a
  data source works with exactly one source; the entry point to the data layer is the
  repository, so nothing above it depends on a data source directly.
- **Practical:** the reflex definition — "a repository is an interface around the API or the
  DAO" — held against a feature with two sources, a refresh policy and a conflict rule, and
  shown to describe none of the responsibilities that matter; then the case the reflex gets
  right by accident, where one trivial source and a pass-through repository add a file and
  no boundary, and what would have to become true for it to be worth adding.
- **Senior:** why "abstracting the source" is the weakest of the five responsibilities and
  ownership is the strongest — the interesting repositories are the ones that decide
  something, and a repository that decides nothing is a naming convention.
- **Primary:** `repository_pattern`
- **Supporting:** `room_dao` (local_data), `retrofit` (networking),
  `separation_of_concerns`, `architecture_tradeoffs`
- **Notes:** **Teach** the responsibility list and the misconception correction. **Bridge**
  DAOs and HTTP clients as named examples of a data source, one sentence each. **Exclude**
  Room, SQL, Ktor, Retrofit, OkHttp, serialization and paging implementations — their own
  curricula own them, and this Unit teaches contracts and coordination only.

#### L3.2 — Coordinating Local and Remote Sources

- **Objective:** design the read and write paths for a feature with two sources, and say
  what the consumer is and is not told.
- **Core:** a repository with more than one source decides which one answers a read, when a
  refresh happens, where a write lands first, and what a failure means; a consumer asks for
  data and is not told which source produced it.
- **Practical:** a read path worked through — local answers immediately, a refresh runs, the
  local store is updated, the consumer sees the change without asking again; a write path
  worked through — written locally and marked for synchronisation, or attempted remotely
  first, with the user-visible difference stated; what the consumer legitimately does need
  to know — that content is stale, that a refresh failed — modelled deliberately rather than
  leaked as an exception type from whichever library failed.
- **Senior:** why freshness cannot be inferred from the data and has to be recorded: a cache
  that does not know when it was filled cannot answer whether it may still be shown, so the
  staleness decision is a stored fact rather than a policy someone can add later.
- **Primary:** `repository_pattern`
- **Supporting:** `offline_first` (local_data), `cache_invalidation` (local_data),
  `caching` (local_data)
- **Notes:** **Teach** coordination as the repository's decision. **Bridge** offline-first
  and caching to the persistence curriculum, which owns storage mechanics, eviction and
  migration. **Exclude** sync frameworks, conflict-resolution algorithms and background
  scheduling.

#### L3.3 — Which Source Is Authoritative?

- **Objective:** choose the source of truth for a specific fact, and justify it from the
  requirement rather than from a technology.
- **Core:** a single source of truth is the one owner that is authoritative for a fact when
  copies disagree; every consumer reads it from there, so there is nothing to reconcile; it
  is a decision per fact, not a property of a database.
- **Practical:** the same application with three different answers — a local store
  authoritative for a draft the user is editing offline, the server authoritative for an
  account balance where a stale local copy is unacceptable, and an in-memory holder
  authoritative for a session-scoped selection that is meant to disappear; a local database
  recommended as the source of truth for offline-first support, taught as a
  requirement-driven recommendation with its condition stated rather than as the definition;
  the failure this prevents, shown as two screens rendering different values because each
  kept its own copy.
- **Senior:** when the honest answer is that two sources disagree legitimately and the
  application must define a resolution rule — at which point "single source of truth" has
  not been abandoned, it has been located one level up in whatever owns that rule.
- **Primary:** `single_source_of_truth`
- **Supporting:** `offline_first` (local_data), `cache_invalidation` (local_data),
  `state_ownership`
- **Notes:** **Teach** the misconception correction directly: source of truth is not a
  synonym for database. **Exclude** distributed consistency models, CRDTs and
  last-writer-wins algorithms — a resolution rule is named as a requirement, not designed.

#### L3.4 — An Observable API, or a One-Shot Read?

- **Objective:** choose the shape of a data API from what its consumer must observe.
- **Core:** expose a suspending function for a one-shot operation and an observable stream
  when the consumer must be notified of later changes; the decision comes from the
  consumer's requirement, not from whether the underlying data can change.
- **Practical:** a screen that must reflect writes made elsewhere in the app, against a
  quote captured at the moment the user pressed a button and deliberately frozen afterwards;
  what each shape costs the caller — a one-shot read is correct at the instant it returns and
  stale after, and a stream commits every caller to a collection lifetime and to a policy for
  values it is obliged to ignore; why a repository exposing mutable state hands every caller
  a second write path.
- **Senior:** the case where one repository legitimately offers both shapes for the same
  data, and what has to be true for that not to be two sources of truth.
- **Primary:** `repository_pattern`
- **Supporting:** `flow_fundamentals`, `flow_collection`, `stateflow` (all async_reactive),
  `single_source_of_truth`
- **Notes:** **Bridge** to E24 rather than reteaching: cold and hot streams, collection
  lifetime, sharing and conflation are already taught, and this Lesson links to them. It
  teaches the *API-shape decision* only. **Exclude** operators, `stateIn`/`shareIn`
  configuration and collection mechanics.

#### L3.5 — Model and Error Boundaries: What May Cross

- **Objective:** decide which types may cross the data boundary, and how failure is
  represented when it does.
- **Core:** transfer, storage and domain representations of the same concept may be the same
  type or different types, and that is a trade-off rather than a rule; a shared type couples
  everything that uses it to whatever defines it; failure crossing the boundary is modelled
  deliberately, in the application's own terms.
- **Practical:** one concept with three representations and the mapping code that costs;
  the same concept with one shared type and the coupling that costs — a wire-format rename or
  a schema annotation reaching the screen; the case for a small feature where one type is
  correct; failure represented as a sealed result or a domain exception, against a caller
  catching the HTTP client's own exception types and thereby learning which client the data
  layer uses.
- **Senior:** why "every model needs four copies" and "one model is simpler" are both
  answers to a question nobody asked — the boundary earns its mapping exactly where the two
  sides change at different rates, which is why the same codebase can correctly do both.
- **Primary:** `layered_architecture`, `error_modeling`
- **Supporting:** `repository_pattern`, `kotlin_sealed_types` (kotlin_language),
  `room_dao` (local_data), `retrofit` (networking)
- **Notes:** **Teach** the trade-off and the error boundary. **Exclude** serialization
  configuration, Room entity design, migrations, and exception-handling mechanics — named
  and deferred to their curricula. The Kotlin sealed-type mechanism is **Bridge**; the
  `kotlin_language` curriculum owns it.

---

## Unit 4 — Domain Logic, Use Cases and Dependency Direction

**Purpose:** decide when another layer earns its existence, and make dependency inversion a
decision about who defines the abstraction rather than a reflex about interfaces.
**Prerequisites:** Units 1 and 3. Unit 1's boundary test is the instrument this Unit
applies; Unit 3 supplies the repository the question is asked about.

#### L4.1 — When Does Another Layer Earn Its Existence?

- **Objective:** decide whether a feature needs a domain layer, and say what would change
  the answer.
- **Core:** the domain layer is optional; it sits between the UI layer and the data layer
  and exists to hold complexity or to be reused; the conditions that earn it are reuse
  across callers, orchestration across sources, and business policy worth isolating from
  both the UI and the data layer.
- **Practical:** a feature with one screen, one repository and no rule, where the layer adds
  files and nothing else; the same application's checkout, where three callers share one
  multi-repository operation with a rule attached, and the layer pays; the honest middle
  case, and what evidence would settle it.
- **Senior:** why "add it when you need it" is only actionable if you can say what *need*
  looks like — and the three conditions above are that definition; and why introducing the
  layer later is cheap exactly when the repository API was designed for its consumers.
- **Primary:** `use_cases`
- **Supporting:** `layered_architecture`, `architecture_tradeoffs`, `separation_of_concerns`
- **Notes:** **Teach** optionality with stated conditions. **Exclude** DDD as a subject —
  aggregates, entities, value objects and bounded contexts are a different curriculum and
  would double this Unit without improving an Android interview answer.

#### L4.2 — Use Cases That Earn Their Place, and Pass-Through Cost

- **Objective:** distinguish a use case that holds something from one that forwards, and
  state the cost of the second.
- **Core:** a use case is one business operation, named for the action it performs; it
  typically depends on repositories and may depend on other use cases; it has no lifecycle
  of its own; a use case that forwards a single repository call holds nothing.
- **Practical:** `GetUserUseCase` calling `repository.getUser()` and nothing else, beside a
  use case that coordinates two repositories, enforces a rule and is called from two
  screens — the same shape, a different amount of content; the cost of the first made
  concrete, as one more hop when reading the feature, one more file per data operation, and
  a layer the next engineer has to keep uniform; the rule that when a codebase reaches data
  almost exclusively through use cases, uniformity can itself be the reason, which is a
  team decision rather than an architectural one.
- **Senior:** why the "every repository method needs a use case" convention survives — it
  removes a judgement call, and teams pay indirection to avoid arguing — and what it costs
  when the domain layer becomes a directory of forwarding functions nobody reads.
- **Primary:** `use_cases`
- **Supporting:** `repository_pattern`, `architecture_tradeoffs`, `state_ownership`
- **Notes:** **Teach** the misconception correction without prohibiting simple use cases
  categorically. **Exclude** naming-convention debate beyond one line, and any use-case
  base class or `invoke`-operator style discussion — an implementation idiom, not a
  decision.

#### L4.3 — Policy, Framework and Detail

- **Objective:** identify business policy that has leaked into the wrong place, and say what
  it costs.
- **Core:** policy is the rule the business would still have if the app were rewritten;
  detail is how this app happens to implement it — a framework, a client, a database, a UI
  toolkit; keeping policy free of framework types is what lets it be reasoned about,
  reused and shared.
- **Practical:** a domain operation that accepts a platform URI and returns a transport
  library's response type, and the three consequences — it changes when the transport
  changes, it cannot move to a target without that library, and its rule is now stated in
  someone else's vocabulary; a rule implemented inside a screen's state holder, discovered
  when a second screen needs it; the same rule in a plain Kotlin type with the framework on
  either side of it.
- **Senior:** why framework independence is not purity but optionality, and why in a
  multiplatform codebase the boundary is unusually visible — the policy is exactly what can
  be shared, and the detail is exactly what cannot.
- **Primary:** `dependency_direction`
- **Supporting:** `clean_architecture`, `layered_architecture`, `kmp_architecture` (kmp)
- **Notes:** **Bridge** to the Kotlin Multiplatform curriculum in one sentence: what is
  shareable is a *consequence* of the policy/detail split, and `expect`/`actual`, source
  sets and platform integration are E33's. **Exclude** any claim that a domain layer must
  be a separate module.

#### L4.4 — Who Defines the Abstraction?

- **Objective:** trace a real dependency direction, and say whether adding an interface
  changed it.
- **Core:** inversion is not the existence of an abstraction, it is which side owns it — the
  consumer declares what it needs, and the implementation depends on that declaration;
  placing the interface beside its implementation leaves the arrow pointing outward and
  inverts nothing.
- **Practical:** the same three types arranged twice — the abstraction owned by the data
  side, and owned by the consuming side — with the compile-time dependency traced in each
  and the question "which side can change without the other" answered differently; why a
  neutral shared location removes the coupling and scatters the contract away from the code
  whose needs define it; how this connects to L1.4 — inversion is the specific case where an
  interface *does* earn its place.
- **Senior:** why this is an architecture decision rather than a dependency-injection one:
  the container decides which implementation is supplied at runtime and changes no source
  dependency, so a codebase can use a DI framework everywhere and have inverted nothing.
- **Primary:** `dependency_direction`, `interface_boundaries`
- **Supporting:** `solid`, `clean_architecture`, `repository_pattern`,
  `service_locator_vs_di` (dependency_injection)
- **Notes:** **Teach** ownership of the abstraction. **Exclude** Dagger, Hilt, Koin,
  modules, bindings, graph assembly, scopes and qualifiers — E27's subject entirely.
  Constructor parameters may appear; DI-framework mechanics may not.

#### L4.5 — Clean Architecture: the Dependency Rule, Not the Diagram

- **Objective:** state what Clean Architecture actually requires, and separate it from the
  Android layer template it inspired.
- **Core:** the rule is that source dependencies point inward and nothing in an inner circle
  knows anything about an outer one; the number of circles is schematic and there is no rule
  that there must be four; as you move inward, abstraction increases and policy replaces
  detail.
- **Practical:** the familiar Android three-layer template shown as *one instance* of the
  rule rather than as the rule; the vocabulary reconciled — "entity", "interactor" and "use
  case" mean different things in different sources, and the platform's own architecture
  guidance names layers differently again; a codebase satisfying the dependency rule with
  two layers, and another with four.
- **Senior:** what the rule buys and what it does not — it makes policy immune to a change of
  detail, and it says nothing about build speed, module count, or how many types a feature
  needs; and why the diagram is the most-copied and least-useful part of the idea.
- **Primary:** `clean_architecture`
- **Supporting:** `layered_architecture`, `use_cases`, `dependency_direction`,
  `android_modules` (build_delivery)
- **Notes:** **Teach** intent before diagram, from the primary source rather than from
  derivative templates. **Exclude** the screaming-architecture and hexagonal/ports-and-
  adapters treatments as subjects of their own — hexagonal may be named in one clause as
  the same dependency idea under a different name. **Exclude** any prescribed Android
  project structure or mandatory domain module.

---

## Unit 5 — MVP, MVVM and MVI Responsibility Models

**Purpose:** classify what a codebase actually does, in a subject where the names are
contested, by reading responsibilities rather than labels.
**Prerequisites:** Units 1, 2 and 4 — the Unit compares nothing the reader has not already
learned to reason about; it adds vocabulary and history, not mechanism.

**One hypothetical screen runs through the whole Unit.** A practice-configuration screen —
it loads options, holds several user selections, validates them against each other, enables
a Start control only when the selection is valid, and reports a failure it can retry. It is
rich enough to expose every trade-off and small enough that the pattern stays visible. The
screen does **not** change between Lessons, so every difference the reader sees is a
difference in the pattern.

#### L5.1 — One Screen, Five Questions

- **Objective:** describe any screen's architecture by answering five questions, before any
  pattern name is used.
- **Core:** the five questions — where does the state live, who is allowed to mutate it, does
  the behaviour owner know about the view, how does user input reach the owner, how does
  output reach the UI — plus a sixth consequence, what lifetime bookkeeping each arrangement
  forces on someone; MVC named as the origin of the vocabulary and as the reason the
  vocabulary is unreliable.
- **Practical:** the practice-configuration screen answered against the five questions with
  no pattern named, producing a complete description of its architecture; the same
  description shown to be compatible with more than one pattern name, which is the Unit's
  thesis; why "MVC" in an Android interview needs the candidate to ask what the interviewer
  means before answering.
- **Senior:** why these names drifted — they were coined for different UI toolkits over
  thirty years, the original sources do not entirely agree with each other, and a term like
  "controller" is used as a synonym for "presenter" in a great many designs. The candidate
  who can say that and then describe responsibilities is stronger than the one who picks a
  definition and defends it.
- **Primary:** `mvc`
- **Supporting:** `mvp`, `mvvm`, `mvi`, `state_ownership`
- **Notes:** **Teach** the comparison frame and enough of MVC for the reader to explain what
  it meant and why the name is contested. **Exclude** a history lesson, Smalltalk-80
  internals, and any attempt to declare one definition of MVC correct.

#### L5.2 — MVP: an Explicit View Contract

- **Objective:** describe MVP through its view contract and its lifetime bookkeeping, and
  say what it does well.
- **Core:** the presenter holds a reference to a view abstraction and pushes updates into it
  by calling methods; the view is passive and reports user actions to the presenter; attach
  and detach exist because the presenter must not call a view that is gone.
- **Practical:** the practice-configuration screen in MVP — the `View` interface enumerating
  everything the screen can be told to do, the presenter calling it, and the attach/detach
  bookkeeping; what it genuinely bought in the View era, where there was no observable state
  mechanism and the alternative was logic inside an Activity; the costs — a contract that
  grows with every UI affordance, a presenter that must track whether the view is attached,
  and updates that are a sequence of calls rather than a value.
- **Senior:** why MVP is not obsolete as *reasoning*: an explicit contract makes what a
  screen can do enumerable and testable, and a state-based design gives that up in exchange
  for consistency. Knowing what was traded is what makes the modern choice defensible.
- **Primary:** `mvp`
- **Supporting:** `mvc`, `interface_boundaries`, `state_ownership`
- **Notes:** **Teach** MVP as a working responsibility model. **Exclude** dismissing it, and
  exclude Fragment/Activity lifecycle mechanics — the attach/detach *requirement* is the
  point, not the callbacks that implement it.

#### L5.3 — MVVM: a UI That Observes State

- **Objective:** describe MVVM by its dependency direction, and reject the structural
  definitions of it.
- **Core:** the UI observes state the owner publishes; the owner holds no reference to the
  view; intentions travel inward and state travels outward; the owner therefore does not
  need to know whether anyone is currently looking.
- **Practical:** the same screen in MVVM, with the view contract from L5.2 replaced by one
  observable value; what disappears — attach and detach, the enumerated call surface, and
  the ordering questions that come with pushing; what MVVM is *not* — a class named
  `ViewModel`, a folder named `viewmodel`, or a count of state streams; the note that the
  name descends from Presentation Model and that the platform's own guidance describes the
  arrangement without using the acronym at all.
- **Senior:** why the absence of a view reference is the load-bearing property: it is what
  makes the owner survivable independently of the UI, testable without one, and safe when
  the UI is absent — and every other MVVM characteristic follows from it.
- **Primary:** `mvvm`
- **Supporting:** `state_ownership`, `unidirectional_data_flow`, `stateflow`
  (async_reactive), `viewmodel_lifecycle` (lifecycle_navigation)
- **Notes:** **Bridge** to Unit 2, which already designed this owner — L5.3 names what Unit 2
  built and must not re-derive it. **Exclude** data binding and any XML-era MVVM tooling.

#### L5.4 — MVI: Intent, Reduction and One Current State

- **Objective:** describe MVI through its transition model, and state its ceremony
  honestly.
- **Core:** user input becomes an intent, an explicit value; one current state describes the
  whole screen; a transition is a function from the current state and an intent to the next
  state, which is what "reduce" names; occurrences that are not state transitions need
  somewhere else to go.
- **Practical:** the same screen in MVI — intents as a closed set, one state value, a
  transition function that can be read in one place and tested without a screen; what that
  buys, as a complete and inspectable history of how the screen reached its current state;
  the ceremony it costs, counted rather than asserted — a type per intent, a transition for
  every change including trivial ones, and a second mechanism for the things that are not
  state; the origin noted, since MVI comes from a fully reactive JavaScript framework and
  the Android versions are adaptations that differ from it and from each other.
- **Senior:** where the "effect" vocabulary becomes dangerous — MVI literature calls a one-off
  output an *effect*, and Compose calls `LaunchedEffect` and `DisposableEffect` effects, and
  they are unrelated abstractions. Whenever this curriculum uses the MVI sense, it says so.
- **Primary:** `mvi`
- **Supporting:** `unidirectional_data_flow`, `state_ownership`, `kotlin_sealed_types`
  (kotlin_language)
- **Notes:** **Teach** the transition model conceptually. **Exclude** any specific MVI
  library, any claim that one implementation is standard, and the state/effect *delivery*
  design — the requirement-driven version of that decision is Unit 6's.

#### L5.5 — Classifying What a Real Codebase Actually Does

- **Objective:** given a real screen, describe its architecture in responsibility terms and
  say which traditions it borrows from.
- **Core:** modern Android screens routinely combine a lifecycle-owned state holder, one
  immutable state value, callbacks for input and an explicit transition function, which
  belong to different naming traditions; the responsibilities are still classifiable even
  when the label is contested.
- **Practical:** three real-looking screens classified against the five questions, none of
  which is a textbook instance of anything; the four claims that do not survive
  classification — that MVVM means many state streams and MVI means one, that MVI is MVVM
  plus sealed intent classes, that MVI reduces recompositions, and that a folder layout
  identifies a pattern; how to answer "do you use MVVM or MVI?" in an interview by
  describing ownership, mutation path, input shape and transition explicitness, and then
  naming whichever label the team uses.
- **Senior:** why the pattern name is a poor predictor of anything measurable — it says
  nothing about invalidation, allocation or frame time, and performance claims attached to
  it are claims about a particular implementation.
- **Primary:** `mvvm_vs_mvi`
- **Supporting:** `mvvm`, `mvi`, `mvp`, `architecture_tradeoffs`
- **Notes:** **Teach** classification as the skill. **Exclude** any recommendation that one
  pattern is correct for Android, and exclude performance claims about patterns.

---

## Unit 6 — State, Events, Lifetime and Architecture Selection

**Purpose:** synthesis. Start from the guarantee a requirement needs, decide what must be
state, who owns it, how long it must survive, and how much structure the feature earns.
**Prerequisites:** Units 1–5; E24 `lesson_shared_flow` and
`lesson_choosing_a_stream_abstraction`; E25 `lesson_transient_ui_effects` and
`lesson_transient_effect_delivery`.

This Unit continues an argument the Compose curriculum deliberately left at a negative
result. `lesson_transient_effect_delivery` establishes that a requirement naming
persistence, acknowledgement or guaranteed later handling has left the Compose boundary,
and says outright that designing the owner on the other side is this curriculum's work.

#### L6.1 — Is This State, or Is It Something That Happened?

- **Objective:** classify a fact as current state, durable state, or a transient occurrence,
  from what the application must guarantee about it.
- **Core:** current state is what is true now and is rendered; durable state is what must
  still be true after the process is gone; a transient occurrence is something that happened
  and is executed rather than rendered; the same fact can be any of the three depending on
  the requirement.
- **Practical:** one fact — "the practice session was scored" — modelled three ways for three
  requirements, with the consequence of each spelled out; the class that is most often
  misfiled, where "there is an unfinished session" or "the last submission failed" sound like
  events because they became true at a moment, and the requirement is that a screen opening
  later knows the current position, which makes them state; why a fact modelled as an
  occurrence and then shown as if it were state fires twice.
- **Senior:** why current state is a lossy compression of the occurrences that produced it —
  which is precisely why an observer arriving late can be correct about state and cannot be
  correct about history, and why a requirement that needs the history has to say so.
- **Primary:** `state_ownership`
- **Supporting:** `unidirectional_data_flow`, `stateflow` (async_reactive), `sharedflow`
  (async_reactive), `single_source_of_truth`
- **Notes:** **Bridge** to E24 and E25, which own the stream contracts and the Compose-side
  distinction. **Exclude** event sourcing as an architecture.

#### L6.2 — What Guarantee Does This Occurrence Need?

- **Objective:** state the guarantee a requirement needs before choosing any mechanism, and
  decide whether a transient mechanism can supply it.
- **Core:** four independent questions decide it — may it be lost if nobody is listening,
  may a consumer arriving later still act on it, may it be seen twice, and must something
  record that it was handled; a mechanism has to satisfy all four that apply, not the one
  that made it look suitable.
- **Practical:** the platform's own statement of the limit — when the producer outlives the
  consumer, in-memory stream solutions do not guarantee the delivery and processing of those
  events — applied to a completion that happens while the app is backgrounded; a replay
  window examined honestly, as something that changes what a later subscriber can recover
  and neither makes the occurrence delivered nor makes it delivered once; acknowledgement
  modelled as a fact somebody owns, so that "handled" is readable rather than implied by a
  stream having accepted a value; the guidance's own conclusion, that such events are
  usually best reduced to UI state the consumer clears once handled.
- **Senior:** why the slogans are the wrong shape of answer. "`StateFlow` is state,
  `SharedFlow` is events, `Channel` is events" answers with a type where the question was
  about a guarantee, and every one of those types loses everything it holds when the process
  ends. A stream describes delivery; a requirement describes handling; no description of
  delivery answers a question about handling.
- **Primary:** `state_ownership`
- **Supporting:** `sharedflow` (async_reactive), `hot_vs_cold_streams` (async_reactive),
  `process_death` (lifecycle_navigation), `single_source_of_truth`
- **Notes:** **Teach** the four questions and the reduction-to-state conclusion.
  **Exclude** event buses, durable messaging infrastructure, queueing systems and delivery
  protocols — the Unit reasons about a requirement and stops before building an
  infrastructure. Stream mechanics are **Bridge** to E24 and are not re-derived.

#### L6.3 — Choosing an Owner From the Lifetime the Requirement Needs

- **Objective:** select an owner for a piece of work or state by matching its lifetime to
  what the requirement demands.
- **Core:** the owner is chosen from the lifetime the requirement needs, not from where the
  code was convenient to write; the ladder of available lifetimes runs from a composable,
  through a screen-level owner, through an application-scoped owner, to something that
  outlives the process; each rung answers a different question.
- **Practical:** four requirements placed on that ladder, including one where the
  screen-level owner is the wrong lifetime in each direction — a value that should have
  stayed inside a composable, and an operation that must finish whether or not the user
  stays; the app-scoped holder as a legitimate rung, with its cost — it never goes away, so
  what it holds never does either; the conclusion for work that must survive the process
  stated plainly and handed to the background-work curriculum, which owns the mechanism.
- **Senior:** why an application-scoped owner is a decision rather than a default: it is the
  correct answer when several screens genuinely share one truth, and the beginning of a
  global-state problem when it is chosen to avoid thinking about lifetime.
- **Primary:** `state_ownership`
- **Supporting:** `lifecycle_coroutines` (async_reactive), `coroutine_scope`
  (async_reactive), `viewmodel_lifecycle` (lifecycle_navigation),
  `background_api_selection` (background_work)
- **Notes:** **Teach** owner selection. **Exclude** WorkManager, foreground services, OS
  scheduling constraints and every background API — named once as where the answer
  continues and deferred. Navigation scoping is **Bridge** only.

#### L6.4 — How Much Architecture Does This Feature Need?

- **Objective:** choose the smallest structure that satisfies a feature's stated
  requirements, and justify a larger one when it is warranted.
- **Core:** the amount of architecture is chosen from the complexity actually present;
  a screen state holder, one repository, no domain layer, no reducer and no event
  abstraction can be the correct architecture for a whole feature.
- **Practical:** two features designed end to end in the same application — a settings
  screen that needs almost nothing, and a checkout flow that earns a domain layer, an
  explicit transition function and a durable record of what was submitted — with each
  decision traced to a requirement rather than to a convention; the same two designed
  wrongly, by applying the larger structure to the first and the smaller to the second, and
  what each mistake costs; a closing pass over the whole subject, where the four questions
  from Unit 1 are asked of both.
- **Senior:** why proportionality is the hardest thing to hold in a team — uniformity is
  genuinely valuable, so a team may knowingly pay indirection to make every feature look
  the same, and the senior position is to name that as a trade-off that was chosen rather
  than to pretend it was derived.
- **Primary:** `architecture_tradeoffs`
- **Supporting:** `use_cases`, `repository_pattern`, `layered_architecture`,
  `clean_architecture`
- **Notes:** **Teach** proportionality as the closing principle. **Exclude** any scoring
  rubric or decision tree that turns the judgement into a lookup — the Unit teaches a
  question, not a table.

---

## Misconception and Reasoning Targets

Recorded as reasoning a learner must be able to demonstrate, not as slogans to recite. Each
entry names the Lesson responsible for correcting it. Correcting a misconception means
replacing it with a decision the learner can make, so each row says what the learner does
instead.

| Misconception | Corrected in | The reasoning that replaces it |
| --- | --- | --- |
| "Architecture is the folder structure." | L1.1 | Describe two package trees with identical dependencies and say why the architecture is the same; then name the dependency change that would make it different |
| "Architecture is which libraries we use." | L1.1 | Separate the decision the library implements from the decision the team made; name a design that is unchanged by swapping the library |
| "More layers means better architecture." | L1.5 | Name what changes independently, identify what the boundary would isolate, and weigh that against the mapping, forwarding and navigation cost |
| "An interface creates decoupling." | L1.4 | Trace the actual source dependency and ask which side may change without the other; say who owns the abstraction |
| "Interfaces are for testing." | L1.4 | Treat easier substitution as a consequence of a boundary that exists for a change-rate reason, and name that reason before adding the interface |
| "A module is a layer." | L1.5 | Distinguish a logical responsibility boundary from a physical build boundary, and say what each one is chosen for |
| "All production state belongs in a ViewModel." | L2.1 | Choose an owner from readers, writers and required lifetime; keep at least one production value local and justify it |
| "A state holder means a ViewModel." | L2.1 | Name the responsibility first, then choose an implementation whose lifetime matches; show a holder that is not a ViewModel |
| "A ViewModel persists state." | L2.2 | Separate retained lifetime from saved and persistent reconstruction; for a named event, say whether the owner survives and why |
| "The ViewModel survives the screen." | L2.2 | Name the owner and the event before answering, and say which part of the answer is platform-specific |
| "There is one correct UI state shape." | L2.3 | Compare a data class and a sealed hierarchy on the same screen requirement, naming what each makes impossible and what each makes awkward |
| "Sealed hierarchies are more type-safe, so use them." | L2.3 | Ask which combinations must be unrepresentable for this product, and accept the transition cost that removing them creates |
| "Exposing the mutable state holder is fine inside the module." | L2.4 | Name the second write path, the breakable invariant and the lost single place to look |
| "Work started by the screen's owner is safe for anything." | L2.5, L6.3 | Ask what must happen if the user leaves, and choose an owner whose lifetime matches that answer |
| "A repository is a wrapper around an API or a DAO." | L3.1 | Name the coordination, source policy, conflict resolution or domain-facing contract it owns; if there is none, say so and leave it out |
| "Every repository needs an interface." | L3.1, L4.4 | Name the volatility, second implementation or dependency-direction requirement that would justify the abstraction |
| "Single source of truth means the database." | L3.3 | For one fact, name the owner that is authoritative when copies disagree, and say what made it authoritative |
| "Anything the server can change must be exposed as a stream." | L3.4 | Decide from what the consumer must observe, and name what a stream commits every caller to |
| "Every model needs a transfer, storage and domain copy." | L3.5 | Compare the coupling a shared type creates with the mapping a split costs, for this feature's actual change rates |
| "Every feature needs a domain layer." | L4.1 | Compare a feature with no rule against one with reuse, orchestration or policy, and name which condition is present |
| "Every repository method needs a use case." | L4.2 | Identify the policy, orchestration or reuse that earns the class; if none is present, name the cost of adding it anyway |
| "Adding an interface inverts the dependency." | L4.4 | Say who defines the abstraction and which side depends on it; trace the compile-time arrow before and after |
| "Clean Architecture means three layers." | L4.5 | State the dependency rule, then derive the number of layers from the responsibilities actually present |
| "MVC/MVP/MVVM/MVI each have one correct definition." | L5.1 | Answer the five responsibility questions first and treat the label as a naming decision that varies by source |
| "MVP is obsolete, so it is irrelevant." | L5.2 | Describe the explicit view contract, push-style updates and attach/detach lifetime cost, and say what the modern design traded away |
| "MVVM is a folder structure or a `ViewModel` class." | L5.3 | Classify by ownership, dependency direction and state flow; show a `ViewModel` class in an arrangement that is not MVVM |
| "MVVM means many state streams and MVI means one." | L5.5 | Compare transition responsibility and input modelling; note that one current value is characteristic of MVI and available to any of them |
| "MVI is MVVM plus sealed intent classes." | L5.5 | Compare where a transition is expressed and who is responsible for it, not which types exist |
| "MVI reduces recompositions." | L5.5 | Say that invalidation is decided by state reads and stability, and that no pattern name predicts it |
| "`StateFlow` is for state, `SharedFlow` or `Channel` is for events." | L6.2 | Begin with the guarantee — loss, lateness, repetition, acknowledgement — and note that no in-memory stream survives the process |
| "Replay makes an event delivered." | L6.2 | Distinguish must-not-be-lost from must-happen-once, and say what would have to record that handling occurred |
| "Every one-off behaviour needs an event mechanism." | L6.1, L6.2 | Ask whether the requirement is that a later screen knows the current position; if so, model it as state |
| "Losing a transient occurrence is always a bug." | L6.2 | Say explicitly when loss is acceptable, and treat that as a legitimate design outcome |
| "A real app needs all the layers." | L6.4 | Choose the smallest structure that satisfies the stated requirements, and name the requirement that would make you add the next piece |

## Terminology This Curriculum Fixes

Several of these words are overloaded, and two of them are actively dangerous because
another curriculum in this app already uses them for something else. Every Lesson uses them
as defined here.

| Term | How this curriculum uses it |
| --- | --- |
| **Responsibility** | A reason to change. Not a class, not a file, not a layer. |
| **Owner** | The component responsible for a piece of state or work — the one that may change it and whose lifetime bounds it. |
| **Ownership** | Who may change a value, and through what path. Separate from lifetime and from persistence. |
| **Lifetime** | How long a component or value exists, and what event ends it. |
| **Persistence** | The mechanism by which a value can be reconstructed after its owner is gone. Never implied by lifetime. |
| **Authority / authoritative** | Which source is correct when copies of a fact disagree. |
| **Source of truth** | The authoritative owner of a particular fact. A decision per fact, not a technology. |
| **Cache** | A copy kept for speed or availability, which is not authoritative. |
| **Boundary** | A place where one side may change without the other noticing. An interface is a boundary only when that is true. |
| **Dependency** | One component naming another. |
| **Dependency direction** | Which of two components names the other, at compile time. |
| **Inversion** | The case where the consumer defines the abstraction and the implementation depends on it. |
| **Layer** | A group of components sharing a level of responsibility and a dependency rule. Logical. |
| **Module** | A build unit. Physical. Never used as a synonym for layer. |
| **State holder** | The responsibility of owning state and the logic that produces it. |
| **ViewModel** | One platform-provided implementation of a screen-level state holder, with a lifetime supplied by a host. Always capitalised as the class when the class is meant. |
| **UI state** | The immutable current value a screen renders. Written "UI state" in prose and `UiState` only when a type is meant. |
| **Domain data / domain model** | The application's own representation of a concept, independent of transport and storage. |
| **Intention** | What the user did, reported inward to the owner. Preferred over "event" for user input throughout. |
| **Intent** | Used only in Unit 5 for MVI's intent value, and never for the Android `Intent` class, which this curriculum does not discuss. |
| **Occurrence** | Something that happened and is executed rather than rendered. Preferred over "event" wherever the delivery question is live. |
| **Event** | Used only with a qualifier — "user event", "domain occurrence", "UI event" — because the unqualified word means four different things across the sources this subject cites. |
| **Effect** | **Never used unqualified.** Compose's effect APIs are "Compose effects" and MVI's one-off outputs are "MVI effects", and the two are unrelated abstractions. Unit 5 says so explicitly. |
| **Repository** | The component that exposes application data, centralises changes to it, and coordinates its sources. |
| **Data source** | A component working with exactly one source of data. |
| **Use case / interactor** | One business operation, named for the action. The two words are treated as synonyms with the variance noted. |
| **Policy** | A rule the business would still have if the app were rewritten. |
| **Detail / framework** | How this application happens to implement something. |
| **Reducer / reduction** | A transition from current state and an intent to the next state. Unit 5 only. |
| **Architecture** | The assignment of responsibility, ownership and dependency direction. Never a count of folders or classes. |

## Reference Material

Concise treatment is worthwhile, but none of this belongs on the main learning path and none
of it is a prerequisite for anything above.

| Area | Why Reference rather than Teach | Where it sits |
| --- | --- | --- |
| SOLID as a named set of five | Two of the five carry the reasoning this subject needs; the acronym matters only for recognition | Named once in L1.2, applied in L1.4 and L4.4 |
| Hexagonal / ports and adapters | The same dependency idea under another name | One clause in L4.5 |
| "Screaming architecture" | A packaging opinion downstream of decisions this subject already teaches | One clause in L4.5, or omitted |
| Use-case naming conventions and `invoke` idiom | An implementation idiom, not a decision | One line in L4.2 |
| MVC's Smalltalk origins | Useful for recognising why the term is contested | Bounded in L5.1 |
| Presentation Model as MVVM's ancestor | Explains where the name came from and why sources disagree | One line in L5.3 |
| Named MVI libraries | Implementations, not the model | Named in L5.4 |
| Application-scoped state holders | A legitimate rung on the lifetime ladder, not a subject | Bounded in L2.1 and L6.3 |

## Excluded Material

Accurate, but it does not improve interview readiness and would add noise. Each exclusion is
revisitable if the target job profile changes — by editing this blueprint, not by quietly
adding a Lesson.

| Area | Why excluded |
| --- | --- |
| Domain-driven design as a subject — aggregates, entities, value objects, bounded contexts | A curriculum of its own; it would double Unit 4 without improving an Android interview answer |
| Architectural style taxonomies, C4, architecture decision records | Documentation and classification practices, not decisions about a codebase |
| Event sourcing and CQRS | Rarely present in Android client code, and would pull Unit 6 into infrastructure design |
| Event buses, durable messaging, queueing systems, delivery protocols | Unit 6 reasons about the guarantee a requirement needs and stops before building the mechanism |
| Dependency-injection frameworks, containers, graph assembly, scopes and qualifiers | E27's subject; constructor parameters may appear in examples, DI mechanics may not |
| Gradle modules, build graphs, convention plugins, build performance | E29's subject; a logical layer is not a build unit and this curriculum says so once |
| Test implementation — fakes, mocks, architecture testing, coroutine testing | E31's subject; testability is taught as a consequence of a boundary and no test is shown |
| `expect`/`actual`, source sets, platform integration, iOS interop | E33's subject; multiplatform appears only where it changes an ownership claim |
| Activity and Fragment lifecycle callbacks, navigation APIs, back-stack mechanics, predictive back | Owned by the lifecycle and navigation curriculum; this subject uses their conclusions about lifetime |
| Room, SQL, migrations, DataStore, paging implementation | Owned by the persistence curriculum; this subject discusses authority, not storage |
| Retrofit, Ktor, OkHttp, HTTP semantics, serialization | Owned by the networking curriculum; this subject discusses boundaries, not transport |
| WorkManager, foreground services, OS background constraints | Owned by the background-work curriculum; Unit 6 names where the answer continues and stops |
| Compose state mechanics, effect APIs, lifecycle-aware collection | Owned by E23 and E25; this subject applies them and links to them |
| Coroutine builders, cancellation mechanics, operators, sharing configuration | Owned by E24; this subject asks which owner holds a scope, not how scopes work |
| Performance claims attributed to architecture patterns | Not supportable; invalidation and frame cost are decided by other things entirely |
| Refactoring this repository's own architecture | E26 is learning-content work; the codebase is evidence, not a target |

## Taxonomy Gaps: Concepts With No Exact Assessment Subtopic

Recorded rather than given invented IDs. **E26-01 does not change the question taxonomy**;
whether any of these should become a Subtopic is a separate decision for a future
question-bank change. Each row names the mapping the curriculum uses instead, so the gap is
visible to E26-08 and to that later decision without blocking authoring.

| Concept | Nearest existing Subtopic | Decision |
| --- | --- | --- |
| UI state modelling — data class against sealed hierarchy, partial states, representable-but-impossible combinations | `state_ownership` | The broadest honest mapping, and already the bank's own choice for `architecture_state_holder_taxonomy`. Recorded because "state ownership" reads narrower than modelling the state's shape, and because L2.3 is the Lesson in Unit 2 whose reasoning the existing pool assesses least. |
| Screen state-holder responsibility as distinct from the `ViewModel` class | `state_ownership` | Semantically correct — the responsibility *is* ownership of screen state — and the closest existing Question (`architecture_state_holder_taxonomy`) already assesses exactly this distinction under that Subtopic. |
| Transient occurrence acknowledgement and consumption | `state_ownership` | `durable_state_vs_one_off_event` already sits there and assesses the durable-against-consumable decision, so the mapping is the bank's own. No occurrence, event or delivery Subtopic exists in the `architecture` Topic. |
| Architecture proportionality — how much structure a feature earns | `architecture_tradeoffs` | Exactly what the Subtopic is named for ("Architecture trade-offs and avoiding over-engineering"), and it currently holds no ACTIVE Question, which is why L1.5 and L6.4 are the epic's clearest assessment gaps. |
| Cohesion and coupling as named concepts | `separation_of_concerns` | The Subtopic's own definition covers them; they are the mechanism behind separating concerns rather than a separate idea. |
| Application-level unidirectional data flow, distinct from the composable-tree kind | `unidirectional_data_flow` | Correct, and the distinction from `compose_udf` (android_ui) is already expressed by the two Subtopics existing in two Topics. |
| Freshness and staleness as a recorded fact | `cache_invalidation` (local_data) | Supporting-only here. The persistence curriculum owns it, and L3.2 uses it as a bounded consequence of a repository decision. |
| Architecture of a multiplatform codebase — what the policy/detail split makes shareable | `kmp_architecture` (kmp) | Supporting-only and deliberately bounded to one sentence in L4.3; E33 owns the subject. |

One Subtopic in the home Topic is mapped **supporting-only** across the whole epic:

| Subtopic | Active questions | Consequence |
| --- | --- | --- |
| `solid` | 1 | SOLID is a vocabulary this curriculum recognises rather than a Unit's subject: its dependency-inversion and open/closed reasoning is taught by L1.4 and L4.4 under `interface_boundaries` and `dependency_direction`. Its one ACTIVE Question therefore creates no Unit practice for any E26 Unit, which is intended. |

Every other `architecture` Subtopic — all seventeen — is primary in at least one Lesson. The
complete inventory, with statuses and Question counts, is in the plan's
[taxonomy inventory](architecture-units-1-6-plan.md#part-1--complete-architecture-taxonomy-inventory).

## Authoritative Source Families

Identified so that a Lesson author does not start from a blank search. Individual Lessons
still cite the specific page supporting each claim, per Rule 9. Architecture is less
source-deterministic than the Compose runtime, so the hierarchy below is part of the
editorial standard rather than a convenience.

**Tier 1 — normative for platform recommendations.** Official Android architecture
guidance: the architecture guide and its recommendations page, the UI layer and its state
holders and events pages, the data layer page, the domain layer page, and the ViewModel
documentation. These are authoritative for *what Android recommends and why*. They are not
authoritative for what every application must do, and where they say "recommended" the
Lesson teaches the reason and the condition rather than converting it into a requirement.

**Tier 1 — normative for multiplatform behaviour.** Official JetBrains Compose
Multiplatform documentation for the common ViewModel and lifecycle, together with the
resolved `androidx.lifecycle` / `org.jetbrains.androidx.lifecycle` sources and this
repository's own configuration. **Android-only documentation is not evidence about
multiplatform behaviour**, and a claim about a target this project builds is checked against
the artifact rather than inferred.

**Tier 1 — primary for a named idea.** Where an idea has an identifiable originating
statement, that statement is the source: Robert C. Martin's Clean Architecture article for
the dependency rule, Martin Fowler's GUI Architectures for MVC, MVP and Presentation Model,
and the Cycle.js and André Staltz material for Model-View-Intent. Using the primary source is
what makes it possible to say honestly that the number of layers is schematic and that the
pattern names are contested.

**Tier 2 — reused settled sources.** Coroutine and Flow delivery contracts are settled by
E24's sources and are not re-researched; Compose state and effect contracts are settled by
E23's and E25's. E26 cites them and adds no new claim about them.

**Not authoritative.** Interview-question collections, SEO architecture blogs, and anonymous
forum answers, exactly as `docs/content/content-authoring.md` already requires. One
qualified exception applies to Unit 5: where the subject is *what a term means to
practitioners*, a widely cited named author is admissible **as evidence of usage**, labelled
as such, and never as a definition the curriculum adopts.

**Where sources disagree — Unit 5's standing rule.** MVP, MVVM and especially MVI have no
single normative specification governing modern usage. The curriculum therefore does not
resolve disagreement by choosing one author. It teaches the responsibility characteristics
that are stable across sources — where state lives, who may mutate it, whether the behaviour
owner knows the view, how input arrives, how output returns, and what lifetime bookkeeping
follows — and says explicitly where the naming varies. The plan records the specific
variance findings in
[terminology variance](architecture-units-1-6-plan.md#mvp-mvvm-and-mvi-terminology-variance).

**Freshness.** Architecture principles are among the most stable material this app teaches;
what dates is Android's *recommended* application architecture, the multiplatform lifecycle
and ViewModel behaviour, and the artifact versions behind both. Re-check those two families
on any material edit; the dependency rule and the pattern history do not move.

## Status

This blueprint is complete as a map, and **all six Units have now been authored** — E26-07
shipped Unit 6, the last instructional Unit, so the 29 planned Lessons are in production.
E26-01 produced this map together with
[`architecture-units-1-6-plan.md`](architecture-units-1-6-plan.md), which holds the taxonomy
inventory, the semantic Question review, the stable identities, the practice-routing model,
the repository and multiplatform findings, the authoring handoffs, and a per-Unit record of
what each authoring issue actually shipped. Authoring ran E26-02 → E26-07 in Unit order and
moved no Lesson boundary; E26-08 addresses assessment gaps and E26-09 verifies and closes
the epic.

The six-Unit structure the merged backlog assumes is **unchanged**, for the reasons recorded
in the plan's [scope confirmation](architecture-units-1-6-plan.md#scope-confirmation).

When authoring reveals that a Lesson boundary was wrong, update this file in the same change
rather than letting the map and the material drift.
