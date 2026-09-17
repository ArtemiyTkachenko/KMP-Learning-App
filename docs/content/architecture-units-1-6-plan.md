# Architecture Units 1–6 Authoring and Assessment Plan

## Purpose

`docs/content/architecture-learning-blueprint.md` maps the whole application-architecture
subject. This document is the confirmed authoring plan for the six Units that epic E26
delivers — **6 Units, 29 Lessons** — reviewed against the taxonomy, the shipped Compose and
Coroutines Units, the current ACTIVE and DEPRECATED question bank, this repository's own
code, the configured multiplatform libraries, and current authoritative sources.

It exists so that E26-02 through E26-07 can start authoring without re-deciding identity,
scope, or concept ownership, and so that E26-08 has a traceable list of assessment gaps.

It is **not** a second blueprint. Objectives, depth layers, misconception targets,
terminology and Teach/Bridge/Reference/Exclude decisions stay in the blueprint; this
document records the decisions the blueprint left open and the review findings that would
otherwise have to be re-derived.

**Review date: 2026-09-15.** Everything in
[configured versions](#configured-versions-this-plan-assumes),
[repository findings](#part-7--this-repositorys-own-architecture-as-evidence) and
[multiplatform findings](#part-8--kotlin-multiplatform-viewmodel-and-lifecycle-findings)
was checked on that date against the versions this repository is configured with.

**This issue is planning only.** It introduces no production Unit, Lesson, Question or
taxonomy entry, changes no production curriculum JSON, and proposes no change to this
repository's own architecture. Every identity proposed here lives in documentation until
the authoring issue that ships it.

## Scope confirmation

The merged epic assumes six instructional Units. **The six-Unit structure is unchanged.**

The test applied is the one E24 and E25 established: *does crossing this Unit boundary
require a genuinely different mental model?* Taxonomy shape and Question availability were
deliberately not used as the test — the `architecture` Topic has 18 Subtopics and the
curriculum has 6 Units, and nothing about that ratio is a design input.

| Boundary | Different mental model? | Why |
| --- | --- | --- |
| 1 → 2 | Yes | Unit 1 reasons about boundaries in the abstract, against a requirement change. Unit 2 designs one specific component and its contract. A reader can hold Unit 1 fully and still not know what a screen owner owns. |
| 2 → 3 | Yes | Unit 2 is presentation state, whose question is *who may change this and for how long*. Unit 3 is application data, whose question is *which copy is correct*. Authority and lifetime are different questions with different failure modes. |
| 3 → 4 | Yes | Unit 3 places data behind a boundary that already exists. Unit 4 asks whether to create another one, which is a cost decision rather than an ownership one, and adds the direction-of-dependency argument Unit 1 opened and deferred. |
| 4 → 5 | Yes | Units 1–4 reason from first principles. Unit 5 reads three competing vocabularies over the same reasoning and teaches classification — a genuinely different activity, and the one the learner's interview actually asks for. |
| 5 → 6 | Yes | Unit 5 classifies a design that already exists. Unit 6 starts from a requirement with nothing built and decides what must be state, who owns it, how long it lives and how much structure it earns. |

**The Lesson count is 29**, distributed 5/5/5/5/5/4. The counts are not quotas; each was
derived from the number of distinct mental models the Unit carries:

- **Unit 1 carries five** because "what architecture decides", "what a responsibility is",
  "which way a dependency may point", "when an abstraction is a boundary" and "what a layer
  costs" are five separately learnable ideas, and the epic's acceptance criteria name all
  five. Merging the interface Lesson into the dependency Lesson in particular would bury
  the misconception the Unit most exists to correct.
- **Units 2 and 3 carry five** because each has one Lesson the reader could otherwise skip
  and still feel complete: Unit 2's UI-state modelling Lesson and Unit 3's observable-API
  Lesson are the two places where the epic's acceptance criteria demand a comparison rather
  than a definition, and neither survives being folded into a neighbour.
- **Unit 4 carries five** because the layer decision, the use-case decision, the
  policy/detail split, inversion and Clean Architecture's rule are distinguishable, and
  collapsing them is exactly how the "Clean Architecture means three layers plus a use case
  per method" misconception is manufactured.
- **Unit 5 carries five** because the comparison frame has to be established before the
  names, each of the three patterns is one Lesson against the same screen, and the
  classification Lesson is where the four pattern misconceptions are corrected. It is the
  Unit's whole point and cannot be an appendix to the MVI Lesson.
- **Unit 6 carries four** because it is a synthesis Unit and adds no new mechanism: the
  state/occurrence distinction, the delivery-guarantee question, owner selection by lifetime
  and proportionality are four decisions, and a fifth Lesson would have to invent material.

## How the authoring issues use this document

| Issue | Reads |
| --- | --- |
| E26-02 | Unit 1 identities, prerequisites and boundaries; the Unit 1 rows of the semantic review; GAP-U1-A…E; the source policy and the Clean Architecture primary-source note |
| E26-03 | Unit 2 identities and boundaries; the Unit 2 rows; GAP-U2-A…D; **[Part 8](#part-8--kotlin-multiplatform-viewmodel-and-lifecycle-findings) in full**, which settles every ViewModel lifetime and `viewModelScope` claim the Unit may make; the state-holder evidence in [Part 7](#part-7--this-repositorys-own-architecture-as-evidence) |
| E26-04 | Unit 3 identities and boundaries; the Unit 3 rows; GAP-U3-A…C; the repository evidence in Part 7, including the one-shot-API counterexample |
| E26-05 | Unit 4 identities and boundaries; the Unit 4 rows; GAP-U4-A…B; the Clean Architecture source findings; the "no named use cases in this repository" finding in Part 7 |
| E26-06 | Unit 5 identities and boundaries; the Unit 5 rows; GAP-U5-A…D; **[MVP, MVVM and MVI terminology variance](#mvp-mvvm-and-mvi-terminology-variance) in full**, which is the Unit's source contract |
| E26-07 | Unit 6 identities and boundaries; the Unit 6 rows; GAP-U6-A…C; **[Part 3](#part-3--the-e25--e26-handoff-ledger) in full**, since Unit 6 is where the largest E25 deferrals land |
| E26-08 | [Assessment gaps](#assessment-gaps-for-e26-08) in full, re-checked against the finished Lessons, plus the [mapping, level and defect candidates](#mapping-level-and-defect-candidates) and the [practice routing model](#part-6--unit-practice-routing-modelled-now) |
| E26-09 | [Handoff](#handoff), the [E25 ledger](#part-3--the-e25--e26-handoff-ledger) for the deferral-by-deferral check its acceptance criteria require, and the recorded limitations |

Each authoring issue also appends its outcomes to
[Authoring outcomes](#authoring-outcomes), so a finding is re-checked rather than
re-derived by the issue that follows it.

## Configured versions this plan assumes

Read from `gradle/libs.versions.toml` and `shared/build.gradle.kts` on 2026-09-15, on
`main` at `4dba29c`. They are unchanged from the versions E25 recorded, which was verified
rather than assumed.

| Component | Version | Relevance to E26 |
| --- | --- | --- |
| Kotlin | 2.4.10 | Language level for every example |
| Compose Multiplatform | 1.11.1 | Supplies the common `LifecycleOwner` and `ViewModelStoreOwner` implementations |
| `androidx-lifecycle` (used through `org.jetbrains.androidx.lifecycle`) | 2.11.0-beta01 | `ViewModel`, `viewModelScope`, `ViewModelStore`, the Navigation 3 entry decorator |
| kotlinx.coroutines | 1.11.0 | `viewModelScope`'s dispatcher and `SupervisorJob` |
| Koin | 4.2.2 | How this repository supplies owners and repositories — evidence only, never taught |
| Navigation 3 (`org.jetbrains.androidx.navigation3`) | see catalog | Entry-scoped `ViewModelStore` ownership |
| Room | 3.0.1 | Below the repository boundary; never taught here |

**`2.11.0-beta01` is a beta.** Every authoring issue must re-verify the contracts in
[Part 8](#part-8--kotlin-multiplatform-viewmodel-and-lifecycle-findings) against the
version then configured rather than trusting this table.

---

## Part 1 — Complete architecture taxonomy inventory

Every Subtopic in the `architecture` Topic, read from
`shared/src/commonMain/composeResources/files/curriculum/initial_curriculum.json` on
2026-09-15. **There are 18, and all 18 are `ACTIVE`.** The Topic holds **26 Questions: 22
ACTIVE and 4 DEPRECATED**. Sixteen Subtopics have at least one ACTIVE Question; `mvc` and
`architecture_tradeoffs` have none.

The last column is this epic's decision under Rule 4 of the authoring contract. "Teach"
means the Subtopic is **primary** in at least one Lesson.

| Subtopic | Name | Status | ACTIVE | DEPRECATED | Conceptual responsibility | E26 decision |
| --- | --- | --- | --- | --- | --- | --- |
| `separation_of_concerns` | Separation of concerns | ACTIVE | 1 | 0 | Why responsibilities are divided at all; cohesion and coupling | **Teach** — primary in L1.1 and L1.2 |
| `layered_architecture` | Layered architecture | ACTIVE | 2 | 0 | Layers as a grouping with a dependency rule; what crosses a layer boundary | **Teach** — primary in L1.5 and L3.5 |
| `mvc` | MVC | ACTIVE | 0 | 0 | The origin of the Model/View/x vocabulary and why it is contested | **Teach** — primary in L5.1, at the depth needed to explain what it meant and why the name is unreliable |
| `mvp` | MVP | ACTIVE | 1 | 0 | Presenter, explicit view contract, push updates, attach/detach lifetime | **Teach** — primary in L5.2 |
| `mvvm` | MVVM | ACTIVE | 1 | 0 | Observed state, an owner that holds no view reference | **Teach** — primary in L5.3 |
| `mvi` | MVI | ACTIVE | 1 | 0 | Intent, one current state, explicit transition, reduction | **Teach** — primary in L5.4 |
| `mvvm_vs_mvi` | MVVM vs MVI | ACTIVE | 1 | 0 | Classifying a real design against contested labels | **Teach** — primary in L5.5 |
| `unidirectional_data_flow` | Unidirectional data flow | ACTIVE | 1 | 0 | State outward, intentions inward, one write path | **Teach** — primary in L2.4 |
| `repository_pattern` | Repository pattern | ACTIVE | 1 | 2 | What a repository owns; repository against data source; API shape | **Teach** — primary in L3.1, L3.2 and L3.4 |
| `use_cases` | Use cases and when to introduce them | ACTIVE | 2 | 0 | One business operation; when a domain layer earns its place | **Teach** — primary in L4.1 and L4.2 |
| `single_source_of_truth` | Single source of truth | ACTIVE | 1 | 0 | Which source is authoritative for a fact | **Teach** — primary in L3.3 |
| `state_ownership` | State ownership | ACTIVE | 4 | 0 | Who owns state, what shape it takes, how long it lives | **Teach** — primary in L2.1, L2.2, L2.3, L2.5, L6.1, L6.2 and L6.3 |
| `clean_architecture` | Clean Architecture | ACTIVE | 1 | 0 | The dependency rule and its Android reading | **Teach** — primary in L4.5 |
| `solid` | SOLID principles | ACTIVE | 1 | 0 | Five named design principles | **Reference** — supporting only in L1.2, L1.4 and L4.4. See [why](#why-solid-is-supporting-only) |
| `dependency_direction` | Dependency direction and inversion | ACTIVE | 1 | 1 | Which component may name which; who defines the abstraction | **Teach** — primary in L1.3, L4.3 and L4.4 |
| `interface_boundaries` | Interface boundaries | ACTIVE | 1 | 0 | When an abstraction is a real boundary | **Teach** — primary in L1.4 and L4.4 |
| `error_modeling` | Error representation/modeling | ACTIVE | 2 | 0 | How failure is represented as it crosses a boundary | **Teach** — primary in L3.5; **Bridge** in L2.3, where error is only something the screen renders |
| `architecture_tradeoffs` | Architecture trade-offs and avoiding over-engineering | ACTIVE | 0 | 1 | Whether a structure earns its cost; proportionality | **Teach** — primary in L1.5 and L6.4 |

**Nothing in the Topic is left unmapped.** Every one of the 18 is either primary in a
Lesson or, in `solid`'s single case, deliberately supporting-only.

### Why `solid` is supporting-only

SOLID is a vocabulary rather than a decision. The two principles that carry reasoning this
subject needs — dependency inversion and open/closed — are taught as reasoning by L1.4 and
L4.4 under `interface_boundaries` and `dependency_direction`, which is where an engineer
actually meets them. A Lesson that walked all five in order would be an acronym tour, which
Rule 5 of the authoring contract exists to prevent.

The consequence is recorded rather than hidden: `architecture_solid_dependency_substitution`
is the **one ACTIVE architecture Question no E26 Unit's practice reaches**, and that is
intended. It is the same shape of decision E24 made for `lifecycle_coroutines`.

### Concepts this epic needs for which the taxonomy has no exact Subtopic

Recorded in the blueprint's
[taxonomy gaps](architecture-learning-blueprint.md#taxonomy-gaps-concepts-with-no-exact-assessment-subtopic)
with the mapping used instead. The four that matter most to E26-08 are **UI state
modelling**, **state-holder responsibility as distinct from the `ViewModel` class**,
**occurrence acknowledgement**, and **architecture proportionality**. The first three all
map to `state_ownership`, which is why that Subtopic is primary in seven Lessons across two
Units; the fourth maps to `architecture_tradeoffs`, which currently holds no ACTIVE
Question at all.

**No taxonomy ID is invented by this issue.** Whether `state_ownership` should be split — a
change that would also relieve the Unit 2 / Unit 6 practice overlap recorded in
[Part 6](#part-6--unit-practice-routing-modelled-now) — is a question-bank-change decision
for a later issue, not an E26 one.

---

## Identity conventions and proposed identities

The shipped Compose and Coroutines content establishes the convention and this plan follows
it rather than inventing one:

- **Unit id** — `unit_` plus the Unit title in snake case, shortened where the full title
  would be unreadable.
- **Lesson id** — `lesson_` plus the concept the Lesson owns, in snake case, short enough
  to read in a reference. Lesson ids are **not** numbered: the `L1.1` labels in the
  blueprint are positional and would be wrong the moment a Lesson moves.
- Ids are stable once shipped. Renaming one breaks learner study-progress records, which
  are keyed by Lesson id (see `docs/architecture/study-progress.md`).
- Lesson ids must be unique across **all** Units, not just within one: `relatedLessonIds`
  names a Lesson without naming its Unit, and `LearningCurriculumValidator` rejects a
  duplicate (`DUPLICATE_LESSON_ID`).

Three deliberate shortenings, all following the precedent of
`unit_identity_keys_and_stability` and `unit_context_dispatchers_and_concurrency`:

- Unit 2's title in full snake case would be
  `unit_screen_state_holders_viewmodel_and_ui_state`; the id drops "viewmodel", which the
  Unit still teaches.
- Unit 3's would be `unit_repositories_data_ownership_and_single_source_of_truth` (55
  characters); the id shortens to `unit_repositories_and_data_ownership`.
- Unit 6's would be `unit_state_events_lifetime_and_architecture_selection`; the id
  shortens to `unit_state_events_lifetime_and_selection`.

All six Unit titles are unchanged from the backlog, and every dropped term is still taught.

| Blueprint | Unit id | Unit title |
| --- | --- | --- |
| Unit 1 | `unit_architecture_responsibilities_and_boundaries` | Architecture as Responsibilities and Boundaries |
| Unit 2 | `unit_screen_state_holders_and_ui_state` | Screen State Holders, ViewModel and UI State |
| Unit 3 | `unit_repositories_and_data_ownership` | Repositories, Data Ownership and Single Source of Truth |
| Unit 4 | `unit_domain_logic_and_dependency_direction` | Domain Logic, Use Cases and Dependency Direction |
| Unit 5 | `unit_responsibility_models_mvp_mvvm_mvi` | MVP, MVVM and MVI Responsibility Models |
| Unit 6 | `unit_state_events_lifetime_and_selection` | State, Events, Lifetime and Architecture Selection |

**All six Units take `architecture` as their home Topic.** No current repository evidence
suggests otherwise: the subject's own Topic exists, holds 18 Subtopics and 22 ACTIVE
Questions, and no shipped Learning Unit homes there yet, so E26 is the first content in it.

| Blueprint | Lesson id | Lesson title | Primary | Supporting |
| --- | --- | --- | --- | --- |
| L1.1 | `lesson_what_architecture_decides` | What Architecture Actually Decides | `separation_of_concerns` | `architecture_tradeoffs`, `layered_architecture`, `android_modules` |
| L1.2 | `lesson_responsibility_and_change` | Responsibility, Cohesion and What Changes Together | `separation_of_concerns` | `solid`, `architecture_tradeoffs`, `interface_boundaries` |
| L1.3 | `lesson_dependency_direction_and_boundaries` | Which Way May This Dependency Point? | `dependency_direction` | `interface_boundaries`, `solid`, `layered_architecture`, `module_dependency_direction` |
| L1.4 | `lesson_when_an_interface_is_a_boundary` | When an Interface Is a Boundary, and When It Is Only Indirection | `interface_boundaries` | `dependency_direction`, `solid`, `architecture_tradeoffs`, `test_doubles` |
| L1.5 | `lesson_layers_and_their_cost` | Layers as One Answer, and What They Cost | `layered_architecture`, `architecture_tradeoffs` | `separation_of_concerns`, `android_modules`, `modularization_tradeoffs` |
| L2.1 | `lesson_state_holder_responsibility` | What a Screen State Holder Is Responsible For | `state_ownership` | `compose_state_hoisting`, `viewmodel_lifecycle`, `kmp_lifecycle_viewmodel`, `separation_of_concerns` |
| L2.2 | `lesson_viewmodel_lifetime_and_persistence` | The ViewModel Owner: Lifetime Is Not Persistence | `state_ownership` | `viewmodel_lifecycle`, `configuration_changes`, `process_death`, `saved_state`, `kmp_lifecycle_viewmodel` |
| L2.3 | `lesson_modelling_ui_state` | Modelling the Current UI State | `state_ownership` | `kotlin_sealed_types`, `error_modeling`, `compose_stability`, `compose_state` |
| L2.4 | `lesson_state_out_intentions_in` | State Out, Intentions In | `unidirectional_data_flow` | `state_ownership`, `compose_udf`, `stateflow` |
| L2.5 | `lesson_owner_scoped_work` | Work Whose Lifetime Is the Owner's | `state_ownership` | `lifecycle_coroutines`, `coroutine_scope`, `viewmodel_lifecycle`, `background_api_selection` |
| L3.1 | `lesson_what_a_repository_owns` | What a Repository Is Responsible For | `repository_pattern` | `room_dao`, `retrofit`, `separation_of_concerns`, `architecture_tradeoffs` |
| L3.2 | `lesson_coordinating_sources` | Coordinating Local and Remote Sources | `repository_pattern` | `offline_first`, `cache_invalidation`, `caching` |
| L3.3 | `lesson_single_source_of_truth` | Which Source Is Authoritative? | `single_source_of_truth` | `offline_first`, `cache_invalidation`, `state_ownership` |
| L3.4 | `lesson_observable_or_one_shot_api` | An Observable API, or a One-Shot Read? | `repository_pattern` | `flow_fundamentals`, `flow_collection`, `stateflow`, `single_source_of_truth` |
| L3.5 | `lesson_model_and_error_boundaries` | Model and Error Boundaries: What May Cross | `layered_architecture`, `error_modeling` | `repository_pattern`, `kotlin_sealed_types`, `room_dao`, `retrofit` |
| L4.1 | `lesson_when_a_domain_layer_earns_its_place` | When Does Another Layer Earn Its Existence? | `use_cases` | `layered_architecture`, `architecture_tradeoffs`, `separation_of_concerns` |
| L4.2 | `lesson_use_cases_and_pass_through_cost` | Use Cases That Earn Their Place, and Pass-Through Cost | `use_cases` | `repository_pattern`, `architecture_tradeoffs`, `state_ownership` |
| L4.3 | `lesson_policy_and_framework_detail` | Policy, Framework and Detail | `dependency_direction` | `clean_architecture`, `layered_architecture`, `kmp_architecture` |
| L4.4 | `lesson_dependency_inversion_in_practice` | Who Defines the Abstraction? | `dependency_direction`, `interface_boundaries` | `solid`, `clean_architecture`, `repository_pattern`, `service_locator_vs_di` |
| L4.5 | `lesson_clean_architecture_intent` | Clean Architecture: the Dependency Rule, Not the Diagram | `clean_architecture` | `layered_architecture`, `use_cases`, `dependency_direction`, `android_modules` |
| L5.1 | `lesson_one_screen_five_questions` | One Screen, Five Questions | `mvc` | `mvp`, `mvvm`, `mvi`, `state_ownership` |
| L5.2 | `lesson_mvp_view_contract` | MVP: an Explicit View Contract | `mvp` | `mvc`, `interface_boundaries`, `state_ownership` |
| L5.3 | `lesson_mvvm_observed_state` | MVVM: a UI That Observes State | `mvvm` | `state_ownership`, `unidirectional_data_flow`, `stateflow`, `viewmodel_lifecycle` |
| L5.4 | `lesson_mvi_intent_and_reduction` | MVI: Intent, Reduction and One Current State | `mvi` | `unidirectional_data_flow`, `state_ownership`, `kotlin_sealed_types` |
| L5.5 | `lesson_classifying_a_real_architecture` | Classifying What a Real Codebase Actually Does | `mvvm_vs_mvi` | `mvvm`, `mvi`, `mvp`, `architecture_tradeoffs` |
| L6.1 | `lesson_state_or_occurrence` | Is This State, or Is It Something That Happened? | `state_ownership` | `unidirectional_data_flow`, `stateflow`, `sharedflow`, `single_source_of_truth` |
| L6.2 | `lesson_delivery_guarantees` | What Guarantee Does This Occurrence Need? | `state_ownership` | `sharedflow`, `hot_vs_cold_streams`, `process_death`, `single_source_of_truth` |
| L6.3 | `lesson_choosing_the_owner_by_lifetime` | Choosing an Owner From the Lifetime the Requirement Needs | `state_ownership` | `lifecycle_coroutines`, `coroutine_scope`, `viewmodel_lifecycle`, `background_api_selection` |
| L6.4 | `lesson_smallest_sufficient_architecture` | How Much Architecture Does This Feature Need? | `architecture_tradeoffs` | `use_cases`, `repository_pattern`, `layered_architecture`, `clean_architecture` |

### Identity and mapping checks performed

Checked against the bundled production JSON on 2026-09-15, by script rather than by
reading.

- **Uniqueness.** None of the 6 proposed Unit ids and 29 proposed Lesson ids collides with
  the **18 shipped Unit ids or the 72 shipped Lesson ids** in `learning_curriculum.json`, or
  with each other.
- **Mapping validity.** All **47 distinct Subtopic ids** used above exist in
  `initial_curriculum.json` and are `ACTIVE`. Eighteen are `architecture` — every Subtopic in
  the Topic. The other twenty-nine bridge to `async_reactive` (7), `android_ui` (4),
  `lifecycle_navigation` (4), `local_data` (4), `build_delivery` (3), `kmp` (2), and one each
  to `kotlin_language`, `testing`, `dependency_injection`, `background_work` and
  `networking` — the cross-Topic bridging Rule 3 of the authoring contract expects.
- **Overlap.** No Lesson lists the same Subtopic as both primary and supporting, which the
  validator rejects (`PRIMARY_SUPPORTING_SUBTOPIC_OVERLAP`).
- **Shared primaries.** Eight Subtopics are primary in more than one Lesson:
  `state_ownership` (7), `dependency_direction` (3), `repository_pattern` (3),
  `separation_of_concerns` (2), `interface_boundaries` (2), `layered_architecture` (2),
  `architecture_tradeoffs` (2) and `use_cases` (2). This is the contract's expected shape —
  "a single Subtopic is routinely touched by more than one Lesson at different depths" — and
  matches the shipped Units, where `compose_state` is primary in eight Lessons and
  `compose_side_effects` in fourteen. The consequences for practice are modelled in
  [Part 6](#part-6--unit-practice-routing-modelled-now) rather than left to be discovered.
- **No shipped Unit's practice changes.** **No shipped Lesson takes any `architecture`
  Subtopic as a primary concept** — eight of them appear as supporting only
  (`state_ownership`, `unidirectional_data_flow`, `repository_pattern`,
  `single_source_of_truth`, `layered_architecture`, `separation_of_concerns`, `use_cases`,
  `error_modeling`). E26 is therefore the first content to claim any of them, and shipping
  it cannot alter the practice pool of any existing Unit.
- **Forward references.** No `relatedLessonIds` value proposed here names a Lesson that does
  not yet ship. See [cross-linking rules](#cross-linking-rules-and-the-intended-link-graph).

---

## Lesson prerequisites, demonstrable reasoning and boundaries

The blueprint holds objectives and depth layers. This section holds what the review had to
settle: the reasoning a learner must be able to demonstrate, where each prerequisite is
already taught, and the boundary that keeps each Lesson from absorbing its neighbour. Every
prerequisite named is a **shipped** Lesson id or an earlier E26 Lesson.

### Unit 1 — `unit_architecture_responsibilities_and_boundaries`

**Prerequisites:** none inside this subject. `lesson_state_hoisting` (shipped, E23) is the
one useful backward anchor: it already teaches the reader/writer/lifetime ownership test at
composable scale, and Unit 1 generalises it rather than re-deriving it.

| Lesson | Demonstrable reasoning | Boundary |
| --- | --- | --- |
| L1.1 | Given two package trees with identical dependencies, say whether the architecture differs, and name the change that would make it differ | Names no component type. Repositories, state holders and use cases are Units 2–4 and are not previewed here |
| L1.2 | Given a class doing three things, say which pair genuinely changes for different reasons and which split would be arbitrary | Stops at the idea of a responsibility. Which responsibilities an Android app actually has is Units 2–4 |
| L1.3 | For a named pair of components, say which may depend on the other and what the reversed arrow would cost | Direction only. **Inversion — who defines the abstraction — is deferred whole to L4.4** and must be named, not taught |
| L1.4 | Given an interface with one implementation edited in the same commits, say what it bought; then name the condition that would make it a boundary | Says what makes an abstraction a boundary. Where a boundary belongs in a layered application is L4.4 |
| L1.5 | For a proposed layer, list its concrete costs and name the independent change it would isolate; then say whether it pays | Teaches the cost model. The data layer's specific model boundary is L3.5; module boundaries are E29 |

### Unit 2 — `unit_screen_state_holders_and_ui_state`

**Prerequisites:** `lesson_state_hoisting`, `lesson_immutability_vs_stability` and
`lesson_remember_saveable` (E23); `lesson_classes_of_screen_state`,
`lesson_stateless_screen_content`, `lesson_screen_state_and_ui_events` and
`lesson_screen_state_owner_boundary` (E25); Unit 1 of this subject. L2.5 additionally
assumes `lesson_coroutine_scope_ownership` (E24).

| Lesson | Demonstrable reasoning | Boundary |
| --- | --- | --- |
| L2.1 | Given a screen with four pieces of state, choose an owner for each and name the lifetime requirement that decided it; identify one that must not move to a screen-level owner | Describes the responsibility. **Construction and injection are E27**; an example may construct an owner and must not explain the construction |
| L2.2 | For each of recomposition, leaving composition, configuration recreation, forward navigation, pop and process death, say whether the state survives and name the owner fact that decided it | Lifetime and persistence only. Lifecycle callbacks, navigation APIs and the `SavedStateHandle` API surface stay with the lifecycle and navigation curriculum |
| L2.3 | Given one screen requirement, model it as a data class and as a sealed hierarchy, name one combination each makes impossible, and name what each makes awkward | Models the value. It does not decide where that value is produced (L2.1) or how it is exposed (L2.4) |
| L2.4 | Given a holder exposing its mutable state container, name the three concrete failures and rewrite the surface | Application boundary only. The composable-tree version is shipped in `lesson_state_down_events_up` and is linked, not repeated. Reducers are L5.4 |
| L2.5 | Given two operations on one screen, say which belongs in an owner-scoped scope and what the other one needs instead | Applies E24's scope model. It names the requirement for a longer-lived owner and **defers the answer to L6.3**; background APIs are E28's subject |

### Unit 3 — `unit_repositories_and_data_ownership`

**Prerequisites:** Units 1 and 2. L3.4 additionally assumes `lesson_why_flow`,
`lesson_state_flow` and `lesson_choosing_a_stream_abstraction` (E24).

| Lesson | Demonstrable reasoning | Boundary |
| --- | --- | --- |
| L3.1 | Given a feature, say which of a repository's responsibilities it actually needs; given a trivial single-source feature, say why adding one buys nothing yet | Contracts and ownership. Room, DAOs, HTTP clients and serialization are named as data sources and taught nowhere |
| L3.2 | Trace the read path and the write path for a two-source feature, and say what the consumer is and is not told about origin, freshness and failure | Coordination as a repository decision. Storage mechanics, eviction and sync scheduling are other curricula's |
| L3.3 | For three facts in one application, name the authoritative source for each and the requirement that made it authoritative | The decision per fact. Conflict-resolution algorithms are excluded; the rule is named as a requirement, not designed |
| L3.4 | Given two requirements over the same data, choose the API shape for each and say what the other would cost the caller | The API-shape decision. Stream mechanics, sharing and collection are E24's and are linked |
| L3.5 | Given one concept with three representations, say what the mapping costs; given one shared type, say what the coupling costs; then choose for a stated feature | What may cross and how failure is represented. Serialization, entity design and exception mechanics are excluded |

### Unit 4 — `unit_domain_logic_and_dependency_direction`

**Prerequisites:** Units 1 and 3. Unit 1's boundary test is the instrument; Unit 3 supplies
the repository the question is asked about.

| Lesson | Demonstrable reasoning | Boundary |
| --- | --- | --- |
| L4.1 | Given two features in one application, say which earns a domain layer and name the condition present in it and absent in the other | The layer decision. DDD as a subject is excluded |
| L4.2 | Given a pass-through use case, name its cost; given one that coordinates, enforces a rule or is reused, name what it holds | The class decision, one level below L4.1. Use-case idioms and base classes are Reference at most |
| L4.3 | Given a domain operation naming framework types, name the three consequences and the fix | Policy and detail. Multiplatform sharing is one sentence and is E33's subject |
| L4.4 | Given three types, trace the compile-time dependency, say who owns the abstraction, and say whether adding the interface changed the direction | Inversion. **Dagger, Hilt, Koin, modules, bindings, scopes and qualifiers are E27's and appear nowhere** |
| L4.5 | State the dependency rule; then say how many layers a named feature needs and why the number is not part of the rule | Intent before diagram, from the primary source. No prescribed project structure, no mandatory module |

### Unit 5 — `unit_responsibility_models_mvp_mvvm_mvi`

**Prerequisites:** Units 1, 2 and 4. **One hypothetical practice-configuration screen runs
through L5.2, L5.3 and L5.4 unchanged**, so every difference the reader sees is a difference
in the pattern rather than in the example.

| Lesson | Demonstrable reasoning | Boundary |
| --- | --- | --- |
| L5.1 | Describe a screen's architecture by answering the five questions, using no pattern name; then say why the same description fits more than one name | Establishes the frame and teaches MVC only as far as explaining why the vocabulary is contested |
| L5.2 | Given the screen in MVP, name what the view contract enumerates, why attach and detach exist, and what the arrangement bought in the View era | A working responsibility model, not a historical curiosity. Fragment and Activity lifecycle mechanics are excluded |
| L5.3 | Given the screen in MVVM, say what disappeared relative to MVP and which single property caused it | Names what Unit 2 already built. It must not re-derive state-holder design |
| L5.4 | Given the screen in MVI, write the transition for one intent and say what the ceremony costs | The transition model conceptually. No named library; the **delivery** design for non-state outputs is Unit 6's |
| L5.5 | Given a real screen borrowing from more than one tradition, classify it by responsibilities and answer "MVVM or MVI?" without picking a side | Classification. No recommendation that one pattern is correct, and no performance claim |

### Unit 6 — `unit_state_events_lifetime_and_selection`

**Prerequisites:** Units 1–5; `lesson_shared_flow` and `lesson_choosing_a_stream_abstraction`
(E24); `lesson_transient_ui_effects` and `lesson_transient_effect_delivery` (E25).

| Lesson | Demonstrable reasoning | Boundary |
| --- | --- | --- |
| L6.1 | Given one fact and three different requirements, model it as current state, durable state and an occurrence, and say what each choice costs | Classification. Event sourcing is excluded |
| L6.2 | For an occurrence, answer the four guarantee questions, then say whether any in-memory mechanism satisfies them and what would | **Continues `lesson_transient_effect_delivery` and must not restate its Compose-side argument.** Event buses, queues and messaging infrastructure are excluded |
| L6.3 | Place four requirements on the lifetime ladder, including one where the screen-level owner is wrong in each direction | Owner selection. The mechanism for surviving the process is named once and handed to the background-work curriculum |
| L6.4 | Design two features in one application end to end, one minimal and one elaborate, tracing every structural decision to a requirement | Closes the subject. No decision table, no rubric |

---

## Cross-linking rules and the intended link graph

`LearningCurriculumValidator` rejects a `relatedLessonIds` entry naming an unknown Lesson
(`UNKNOWN_RELATED_LESSON`), and Lesson ids are resolved across the whole document rather
than within a Unit. **Forward links are therefore invalid until their target ships.**

- Each authoring issue may link **backwards** only: to the 72 shipped Lessons and to Lessons
  shipped by an earlier E26 issue.
- Pointing forward is a prose sentence naming the Unit. The issue that ships that Unit may
  then add the reciprocal link from its own Lessons.
- E26 requires **no edit to any shipped Lesson** in order to receive a link. This was
  checked: the two shipped Lessons that point at this curriculum —
  `lesson_screen_state_owner_boundary` and `lesson_transient_effect_delivery` — both name
  "the architecture curriculum" in prose without asserting that it does not exist, unlike
  the sentence E24-05 had to repair in `lesson_snapshot_flow`. **E26-09 should re-read both
  and confirm they still read correctly once the Units ship**; neither is expected to need
  editing, and adding a backward link from them is optional rather than required.

The intended final graph, recorded here so the authoring issues do not have to invent it:

| From | To | Direction | Shipped by |
| --- | --- | --- | --- |
| L2.1 | `lesson_screen_state_owner_boundary`, `lesson_classes_of_screen_state`, `lesson_state_hoisting` | backward | E26-03 |
| L2.2 | `lesson_remember_saveable`, `lesson_screen_state_owner_boundary` | backward | E26-03 |
| L2.3 | `lesson_immutability_vs_stability`, `lesson_screen_state_and_ui_events` | backward | E26-03 |
| L2.4 | `lesson_state_down_events_up`, `lesson_screen_state_and_ui_events` | backward | E26-03 |
| L2.5 | `lesson_coroutine_scope_ownership`, `lesson_remember_coroutine_scope`, `lesson_who_owns_the_trigger` | backward | E26-03 |
| L3.4 | `lesson_why_flow`, `lesson_state_flow`, `lesson_choosing_a_stream_abstraction` | backward | E26-04 |
| L4.x | Unit 1 and Unit 3 Lessons | backward, within E26 | E26-05 |
| L5.3 | Unit 2 Lessons; `lesson_state_flow` | backward, within E26 and to E24 | E26-06 |
| L6.1, L6.2 | `lesson_transient_ui_effects`, `lesson_transient_effect_delivery`, `lesson_shared_flow`, `lesson_choosing_a_stream_abstraction` | backward | E26-07 |
| L6.3 | Unit 2 L2.5; `lesson_coroutine_scope_ownership` | backward | E26-07 |
| L6.4 | at least one Lesson from each of Units 1–5 | backward, within E26 | E26-07 |

---

## Part 3 — The E25 → E26 handoff ledger

E25 recorded its deferrals in `docs/content/compose-units-7-12-plan.md`, both in
[E26 owns application architecture](compose-units-7-12-plan.md#e26-owns-application-architecture)
and in the Unit-level exclusions, and E25-09's closure review confirmed each one held in the
shipped prose. **Every deferral is accounted for below, concept by concept.** This table is
what E26-09's acceptance criterion "each responsibility the Compose effects epic deferred to
this epic is answered by a shipped Lesson or recorded as still deferred with its reason"
is checked against.

| Deferred by E25 | Where it was deferred | Owned in E26 by |
| --- | --- | --- |
| The production screen pipeline behind the owner — repository, owner, UI state, stream, Compose — as an architecture rather than as a Compose boundary | Former blueprint L8.1, **moved to E26** | Unit 2 (the owner), Unit 3 (the data side), Unit 6 L6.4 (the whole pipeline chosen proportionally) |
| Why a screen-level owner exists at all, beyond "the composition talks to it" | L7.4's stated stop | **L2.1** |
| What the owner owns, what it receives, what it must not know | L7.4 Exclude | **L2.1**, **L2.4** |
| How the owner is constructed, injected, layered | L7.4 Exclude | Layering: **Unit 1** and **L4.1**. **Construction and injection are E27's and remain out of E26** |
| `UiState` modelling — sealed hierarchy against nullable-field data class, partial states, error representation | Former L8.3, **moved to E26**; L7.3 Exclude | **L2.3** |
| Application-level unidirectional data flow, distinct from the composable tree | L7.3's boundary | **L2.4** |
| Controlled write paths and read-only exposure at the application boundary | implied by L7.3 | **L2.4** |
| Detailed state-holder design | E26-owns list | **Unit 2** entire |
| `viewModelScope` as the answer to "where does work that outlives the composition go" | L7.3/L10 narrowing, explicitly "the `viewModelScope` answer is E26's" | **L2.5**, with the lifetime-selection half in **L6.3** |
| Work whose required lifetime exceeds the Composition, and what owner it needs | L10.3 and L12.3 reach the negative result | **L6.3** |
| The consumable-event-channel against acknowledged-state trade-off | Former L8.4, split three ways, this third **to E26** | **L6.2** |
| Application-level event modelling | L12.3 Exclude | **L6.1**, **L6.2** |
| Durable against transient event architecture | L12.3 Exclude | **L6.1**, **L6.2** |
| `Channel` against `SharedFlow` as a ViewModel event design decision | L12.3 Exclude, explicitly **E26** | **L6.2** — reframed as the guarantee question, and deliberately **not** answered as a type comparison |
| Acknowledgement and queueing architecture | E26-owns list | **L6.2** for acknowledgement. **Queueing architecture stays excluded** — the blueprint records it as an infrastructure subject this curriculum does not teach |
| `SavedStateHandle` as a state-production mechanism | E26-owns list | **Bounded supporting context in L2.2 only.** The mechanism belongs to the lifecycle and navigation curriculum; E26 uses it to make "lifetime is not persistence" concrete and teaches no API |
| MVC, MVP, MVVM, MVI, MVVM-against-MVI; reducers | E26-owns list | **Unit 5** entire |
| Layered architecture | E26-owns list | **L1.5**, **L3.5**, **L4.5** |
| The repository pattern | E26-owns list | **Unit 3** |
| Use cases | E26-owns list | **Unit 4** |
| Single source of truth as an architecture | E26-owns list | **L3.3** |
| Clean Architecture | E26-owns list | **L4.5** |
| `state_ownership`, `unidirectional_data_flow`, `mvi`, `mvvm`, `repository_pattern`, `layered_architecture` and `single_source_of_truth` as **primary** concepts — supporting-only throughout E25 | E25's mapping decision | E26 claims all seven as primary. This is the change that gives them Unit practice for the first time |
| GAP-U7-C — what changes, and what does not, when a piece of state's owner moves outside the Composition | E25-08 left it open, noting it "overlaps E26's territory and may be better left to it" | **L2.2** teaches exactly this reasoning. E26-08 should decide whether to author it under `state_ownership`; recorded as [GAP-U2-A](#assessment-gaps-for-e26-08) |

**Two E25 conclusions E26 must carry forward without weakening:**

1. **Moving state outside the Composition changes its owner and grants no persistence.**
   `lesson_screen_state_owner_boundary` teaches this and L2.2 extends it. E26 must not
   reintroduce "the ViewModel survives the screen" as shorthand.
2. **A successful emission is not a delivery, and retention is not exactly-once.**
   `lesson_transient_effect_delivery` and E24's
   `stream_choice_cannot_supply_a_delivery_guarantee` establish it. L6.2 applies it to an
   application-level design and must link rather than re-derive.

---

## Part 5 — Semantic assessment review

Every ACTIVE Question in the `architecture` Topic was read in full — stem, options, correct
set, explanation and Sources — not counted. There are **22**, spread over 16 of the Topic's
18 Subtopics; `mvc` and `architecture_tradeoffs` hold none. All **4** DEPRECATED Questions
on the same Subtopics were read as context, because a deprecated Question still occupies its
concept.

**Nothing below claims that an unpublished Lesson makes a Question answerable.** "Planned"
means this plan places the required reasoning in a named Lesson; whether the finished prose
delivers it is a judgement E26-02…E26-07 make at authoring time and E26-08 re-checks.

| Question | Level | Subtopic | Lesson(s) | Reasoning the Question requires | Finding | Action |
| --- | --- | --- | --- | --- | --- | --- |
| `separation_of_concerns_001` | Foundation | `separation_of_concerns` | L1.1, L1.2 | That focused responsibilities narrow the blast radius of a change, and that dependencies point toward policy rather than upward to the UI | Sound, and the only Question in the bank on Unit 1's opening idea. Definitional rather than applied — it asks what separation is good for, not whether a given split is justified | Retain; see GAP-U1-B |
| `architecture_paging_ownership` | Applied | `layered_architecture` | L1.5, L3.2 | That deciding when a page is fetched, caching it and recording where to resume are data-layer responsibilities | Sound and well matched to Unit 3. **Routing consequence:** `layered_architecture` is primary in L1.5 as well, so it also becomes Unit 1 practice, where the data-layer vocabulary has not been introduced | Retain; recorded in [Part 6](#part-6--unit-practice-routing-modelled-now) |
| `dto_entity_domain_model_boundary` | Applied | `layered_architecture` | L3.5, L1.5 | That layer-specific models let network, storage and domain evolve independently at the cost of mapping | Sound, and the closest existing match to L3.5's contract. Same Unit 1 routing consequence as above | Retain |
| `mvp_vs_mvvm_view_contract` | Foundation | `mvp` | L5.2, L5.3 | That a presenter holds a view interface and pushes updates while a state owner publishes state and knows nothing about the view | Sound, precisely matched, and the sharpest Question in the pattern half. Its explanation already carries the attach/detach cost L5.2 teaches | Retain |
| `viewmodel_vs_repository_responsibility` | Applied | `mvvm` | L2.1, L3.1 | That the repository owns the data contract and cache policy while the state holder derives screen state, and that holding a `Context` to format state retains an object that outlives the UI | Sound as a Question and one of the best in the Topic. **Mapping mismatch:** the reasoning is the ViewModel/repository responsibility split, which L2.1 and L3.1 own; its Subtopic is `mvvm`, so it becomes Unit 5 practice. **Consequence:** MVVM as a responsibility model is then unassessed | Retain; re-map candidate — see [mapping candidates](#mapping-level-and-defect-candidates) and GAP-U5-B |
| `architecture_mvi_single_state` | Applied | `mvvm_vs_mvi` | L5.5, L2.3 | That one immutable state value prevents rendering combinations the owner never intended, and that this is a correctness property rather than a performance one | Excellent, and it already denies the "MVI means fewer recompositions" claim in a distractor and in its explanation. The best-matched Question in Unit 5 | Retain; L5.5 should cite rather than re-derive its argument |
| `architecture_ui_event_consumption` | Applied | `mvi` | L6.2 | That a replay cache hands a late collector the event again because the stream has no notion of the event having been acted on, and that handling once requires the consumption to be recorded | **Sound, and it is the single best existing Question for the reasoning L6.2 owns.** Its Subtopic is `mvi`, so it becomes Unit 5 practice and never reaches Unit 6 | Retain; **strongest re-map candidate in the Topic** |
| `repository_observable_api_shape` | Applied | `repository_pattern` | L3.4 | That a requirement to reflect writes the screen did not make needs the repository to be able to push, and that a one-shot read is stale the instant it returns | Sound and precisely matched to L3.4's contract | Retain |
| `architecture_use_case_reuse` | Applied | `use_cases` | L4.2 | That extracting multi-repository orchestration gives it one home that both callers share and that can be exercised without a ViewModel | Sound, and its final sentence already states L4.2's condition — when only one caller exists and there is no logic, the layer is indirection | Retain |
| `domain_layer_passthrough_cost` | Applied | `use_cases` | L4.2 | That a pass-through class adds navigation overhead without owning a behaviour | Sound and directly matched. Together with the Question above, L4.2 is the best-assessed Lesson in the epic | Retain |
| `single_source_of_truth_001` | Foundation | `single_source_of_truth` | L3.3 | That one owner is authoritative and consumers read from there, so there is nothing to reconcile | Sound. Definitional — it establishes what the term means and never makes the reader choose an owner for a stated requirement | Retain; see GAP-U3-C |
| `state_ownership_001` | Foundation | `state_ownership` | L2.1, L2.4 | That one owner decides how state changes and publishes it read-only, and that ownership is separate from durability | Sound, and its explanation already contains the lifetime/persistence separation L2.2 teaches — as an aside rather than as the thing assessed | Retain; see GAP-U2-A |
| `architecture_state_holder_taxonomy` | Applied | `state_ownership` | L2.1 | That a plain remembered holder owns one component's logic while a ViewModel is a screen-level holder, and that a ViewModel would give a picker a lifetime and a dependency surface it never asked for | **Excellent, and the closest existing match to any E26 Lesson's contract.** It is the state-holder-is-not-a-ViewModel distinction assessed directly | Retain; L2.1 must teach enough reasoning to make it answerable without quoting it |
| `durable_state_vs_one_off_event` | Applied | `state_ownership` | L6.1 | That a payment result belongs in durable state derivable after recreation while a snackbar is consumable behaviour, and that replaying transient behaviour as durable state duplicates the effect | Sound and precisely matched to L6.1. **Routing consequence:** `state_ownership` is primary across Units 2 and 6, so this Unit 6 reasoning is also Unit 2 practice | Retain; recorded in Part 6 |
| `viewmodel_activity_reference_lifetime` | Foundation | `state_ownership` | L2.1, L2.2 | That a ViewModel can outlive the UI instance, so retaining one retains a destroyed owner and its Views | Sound. Android-specific by construction, which L2.2 must handle rather than generalise — the claim "a ViewModel commonly survives configuration recreation" is true of the Android host and not of every host this repository builds for | Retain; L2.2 must scope the claim, not contradict it |
| `unidirectional_data_flow_001` | Foundation | `unidirectional_data_flow` | L2.4 | That state flows down to consumers and events flow up to the owner, and that components do not synchronise behind the owner's back | Sound. Definitional, and phrased generically enough to serve either the composable-tree or the application boundary | Retain; see GAP-U2-C |
| `clean_architecture_dependency_rule_tradeoff` | Applied | `clean_architecture` | L4.5, L4.3 | That an inward-pointing domain names no framework type, is unit-testable and shareable, and that mapping appears wherever an annotated type would otherwise have been reused | Sound and carefully written; its explanation already refuses the build-speed claim. It assesses the **consequences** of following the rule and not the rule's own shape | Retain; see GAP-U4-B |
| `architecture_solid_dependency_substitution` | Foundation | `solid` | — | That depending on an abstraction the caller owns is dependency inversion and enables open/closed extension, and that interface segregation points the other way | Sound and well written. `solid` is **supporting-only** throughout E26 by design, so this creates no Unit practice | Retain; no E26 action |
| `dependency_direction_domain_framework_types` | Foundation | `dependency_direction` | L4.3 | That accepting a platform URI and returning a transport type couples the domain to Android and the HTTP client, losing framework independence and portability | Sound, and the explanation's refusal of "coupling means untestability" is exactly L1.4's correction. **Level looks low** — recognising the design consequence from a signature is not recall. **Routing consequence:** `dependency_direction` is primary in L1.3 too, so it is Unit 1 practice for reasoning L4.3 completes | Retain; level review is a candidate, not a defect |
| `architecture_interface_boundary_ownership` | Applied | `interface_boundaries` | L4.4 | That the abstraction belongs to the side that consumes it, and that placing it beside the implementation leaves the arrow pointing outward | Sound and precisely matched to L4.4. **Routing consequence:** `interface_boundaries` is primary in L1.4, so a Unit 1 reader meets a Question whose answer L4.4 supplies | Retain; recorded in Part 6 |
| `architecture_error_mapping_boundary` | Applied | `error_modeling` | L3.5 | That catching the HTTP client's exception types in a state holder makes the presentation layer know which client the data layer uses | Sound and precisely matched | Retain |
| `architecture_error_modeling_result_type` | Applied | `error_modeling` | L3.5 | That a sealed result makes expected failure part of the signature the caller must handle | Sound and precisely matched | Retain |

### The four DEPRECATED architecture Questions, read as context

All four are recorded in `docs/content/question-audit-log.yml` as already DEPRECATED at the
time of the 2026-09-04 bank review, which reviewed and deliberately did not edit them; the
log does not record the reason for the original deprecation, and this plan does not invent
one.

| Question | Subtopic | What it occupied | Consequence for E26 |
| --- | --- | --- | --- |
| `repository_pattern_001` | `repository_pattern` | The repository's role — coordinating access to one or more data sources behind a focused API, keeping persistence types behind the boundary | **The most consequential of the four.** With both this and the Question below deprecated, the ACTIVE bank's only repository Question is about API *shape*, so the repository's **responsibility** — L3.1's entire contract — is unassessed. See GAP-U3-A |
| `repository_vs_data_source_responsibility` | `repository_pattern` | What belongs to the repository rather than to either data source: coordination, source selection, refresh, caching and conflict policy | Same gap, from the coordination side. It is close to L3.2's contract as well. E26-08 must not simply re-ask either in the old wording |
| `dependency_direction_001` | `dependency_direction` | What inversion solves, and that placing the interface beside its implementation defeats it | Superseded: `architecture_interface_boundary_ownership` asks the same question better and is ACTIVE. No restoration needed |
| `architecture_tradeoffs_001` | `architecture_tradeoffs` | When a use-case layer is most defensible — real rules, orchestration, or behaviour several callers would duplicate | Its concept is exactly L4.1's, and its Subtopic now holds **zero** ACTIVE Questions while being primary in L1.5 and L6.4. The reasoning is worth restoring at the *layer* level rather than the use-case level, where `architecture_use_case_reuse` already covers it. See GAP-U1-E and GAP-U6-C |

### Questions outside `architecture` that touch this subject

Read so that E26-08 does not duplicate them and so no Lesson claims coverage it does not
create. **None of these is reachable through an E26 primary mapping**, because no E26 Lesson
takes a non-`architecture` Subtopic as primary — so none of them becomes Unit practice for a
new Unit, and none should be remapped into `architecture` merely because E26 teaches
adjacent reasoning.

| Question | Topic / Subtopic | Relevance, and the duplication it guards against |
| --- | --- | --- |
| `viewmodel_store_configuration_retention`, `viewmodel_clear_owner_finish`, `viewmodel_destination_scope` | lifecycle_navigation / `viewmodel_lifecycle` | Retention across configuration recreation, clearing when the owner is permanently destroyed, and destination-scoped lifetime. **Together these already assess most of L2.2's ladder** from the lifecycle side; L2.2 must teach the reasoning and E26-08 must not duplicate them under `state_ownership` without a distinct angle |
| `navigation_back_stack_entry_lifetime`, `navigation_graph_viewmodel_shared_scope` | lifecycle_navigation / `navigation_fundamentals` | Entry-scoped owners and choosing a shared graph scope for a flow. The owner-selection half of L6.3, from the navigation side |
| `configuration_change_vs_process_recreation` | lifecycle_navigation / `configuration_changes` | The distinction L2.2 depends on. Bridged, not re-assessed |
| `remember_saveable_vs_viewmodel_ownership`, `saved_state_transient_inputs`, `savedstatehandle_process_recreation`, `saved_state_binder_transaction_limit` | lifecycle_navigation / `saved_state` | The persistence half of "lifetime is not persistence". L2.2 keeps saved state as bounded supporting context precisely because this curriculum owns it |
| `process_death_001`, `saved_state_001` | lifecycle_navigation (DEPRECATED) | Both occupied "a ViewModel is in-memory, so process death clears it". The concept is taken even though the Questions are retired |
| `viewmodel_scope_cleared_cancellation` | async_reactive / `lifecycle_coroutines` | **Exactly L2.5's reasoning**, including the conclusion that an upload the user expects to finish belongs elsewhere. E26 must teach the ownership argument and should not re-ask this |
| `coroutine_scope_outlives_its_consumer`, `coroutine_scope_job_ownership` | async_reactive / `coroutine_scope` | Ownership mismatch and its concrete failure. L2.5 and L6.3 apply this; E24 owns it |
| `stream_choice_cannot_supply_a_delivery_guarantee`, `shared_flow_try_emit_true_is_not_delivery` | async_reactive / `hot_vs_cold_streams`, `sharedflow` | **The delivery argument L6.2 continues.** Both are ADVANCED and both already reach the conclusion that the fact must be written where it outlives the emitter. L6.2 adds the application-side design question — who owns that record, and what acknowledgement means — and must not re-ask the stream half |
| `stateflow_001`, `stateflow_vs_sharedflow_current_value`, `state_flow_equal_value_is_not_a_new_state`, `flow_sharing_policy_and_replay_expiration` | async_reactive / `stateflow`, `sharedflow`, `flow_sharing` | Stream contracts. E26 cites them and adds no claim about them |
| `flow_one_shot_result_vs_observable_stream` | async_reactive / `flow_fundamentals` | **Nearly L3.4's decision**, posed from the stream side with a repository example. L3.4 must teach the decision and E26-08 should weigh whether an architecture-side duplicate adds anything |
| `compose_screen_state_lowest_sensible_owner`, `compose_over_hoisted_ui_element_state_cost` | android_ui / `compose_state_hoisting` | Owner placement and over-hoisting cost at composable scale. **These are why L2.1 applies rather than re-derives the ownership test** |
| `compose_state_flow_value_read_is_not_observation`, `compose_udf_event_direction`, `compose_work_placement_responsibility_vs_thread` | android_ui / `compose_state`, `compose_udf`, `compose_derived_state` | The Compose-side boundary. The third already separates *responsibility placement* from *threading*, which L2.1 should cite rather than re-argue |
| `kmp_shared_viewmodel_owner_platform` | kmp / `kmp_lifecycle_viewmodel` | That the ViewModel class, `viewModelScope` and clearing are usable from common code while the **owner** is platform-supplied. Directly supports L2.2's multiplatform caution |
| `kmp_shared_layer_selection` | kmp / `kmp_architecture` | That domain rules, models and repository coordination are the strongest sharing candidates. The consequence of L4.3's policy/detail split, assessed from the KMP side |
| `offline_first_001`, `offline_first_local_write_then_sync`, `cache_invalidation_staleness_policy`, `cache_memory_and_disk_levels` | local_data / `offline_first`, `cache_invalidation`, `caching` | Source-of-truth and freshness from the persistence side. **`offline_first_001` in particular already states the recommendation L3.3 must not turn into a definition** |
| `room_dao_001` | local_data / `room_dao` | That translating rows into domain models is normally the repository's job. Supports L3.1's boundary from below |
| `realtime_messages_persist_then_render`, `system_design_pagination_state_ownership` | mobile_system_design | Committing occurrences to durable storage and placing paging state. Both are L6.2/L6.3 reasoning under a system-design framing |
| `service_locator_vs_di_001`, `di_hilt_viewmodel_scope` | dependency_injection | Hidden dependencies and DI-scoped lifetimes. **E27's**; L4.4 names the distinction between inversion and wiring and teaches no DI |
| `over_modularization_tiny_module_cost`, `feature_vs_layer_module_tradeoff`, `feature_siblings_shared_contract_module`, `modularization_large_app_module_cost` | build_delivery | Module boundaries and their cost. **E29's**; L1.5's "a logical layer is not a Gradle module" boundary exists so this material is not duplicated |
| `fakes_vs_mocks_interaction_coupling`, `test_dependency_substitution_constructor`, `repository_test_cache_policy_fakes`, `viewmodel_testing_001` | testing | Substitution and boundary testing. **E31's**; L1.4 uses testability as a consequence and shows no test |
| `kotlin_sealed_types_001` | kotlin_language / `kotlin_sealed_types` | Closed sets and exhaustive `when`. The mechanism behind L2.3's sealed option; the language curriculum owns it |
| `background_api_selection_criteria` | background_work / `background_api_selection` | Which characteristics point to a scheduler rather than an owner-scoped coroutine. **The mechanism half of L6.3's conclusion**, and the reason L6.3 stops where it does |
| `retrofit_vs_okhttp_responsibilities` | networking / `retrofit` | A responsibility split inside the transport stack. Named only as an example of a data source |

---

## Assessment gaps for E26-08

Substantive gaps only. There is no per-Lesson or per-level quota, and a gap here is a
**candidate for authoring rather than an automatic defect**. Each gap names the reasoning
that is missing, not a number. Gap ids are stable and E26-08 disposes of each one
explicitly.

| Gap | Unit / Lesson | Reasoning no ACTIVE Question assesses | Why it is substantive | Recommended action |
| --- | --- | --- | --- | --- |
| GAP-U1-A | 1 / `lesson_what_architecture_decides` | That architecture is the dependency and ownership structure rather than the package tree or the library set — shown by two arrangements that differ in structure and not in architecture | Nothing in any Topic poses it. It is the misconception the whole epic is built to correct and the one an interview opens with | Add coverage in E26-08 |
| GAP-U1-B | 1 / `lesson_responsibility_and_change` | Deciding whether two pieces of behaviour genuinely change for different reasons, applied to a concrete class | `separation_of_concerns_001` establishes why separation is good; nothing makes the reader judge a specific split, which is where the reasoning actually lives | Add coverage in E26-08 |
| GAP-U1-C | 1 / `lesson_dependency_direction_and_boundaries` | Choosing a dependency direction for two ordinary components, and naming what reversing it would cost | `dependency_direction`'s only ACTIVE Question is the domain/framework-types case, which is L4.3's. Unit 1's own reasoning is unassessed | Add coverage in E26-08 |
| GAP-U1-D | 1 / `lesson_when_an_interface_is_a_boundary` | That an interface with one implementation that changes with it has decoupled nothing, and the condition that would make it a boundary | `architecture_interface_boundary_ownership` assesses **where** an abstraction belongs, never **whether** it earns existence. "Interfaces decouple" is one of the most durable misconceptions in the subject | Add coverage in E26-08; **strong candidate** |
| GAP-U1-E | 1 / `lesson_layers_and_their_cost` | That an added layer must isolate an independent change to be worth its mapping, forwarding and navigation cost — the "more layers is better" correction | **`architecture_tradeoffs` holds zero ACTIVE Questions**, and the DEPRECATED `architecture_tradeoffs_001` addressed the use-case level only. The whole Lesson is unassessed | Add coverage in E26-08; **the widest gap in Unit 1** |
| GAP-U2-A | 2 / `lesson_viewmodel_lifetime_and_persistence` | That an owner's lifetime, not the move out of the Composition, decides what survives — and that surviving recreation is not persistence | The reasoning is assessed four times in `lifecycle_navigation` and once in `kmp`, none of it reachable from an E26 primary; `state_ownership_001` carries it only as an explanation aside. **This is also E25's open GAP-U7-C**, which E25-08 explicitly left to this epic | Add coverage in E26-08, from the ownership side rather than the lifecycle side |
| GAP-U2-B | 2 / `lesson_modelling_ui_state` | Choosing between a single data class and a sealed hierarchy for a stated screen requirement, and naming what each makes impossible and what each makes awkward | **Nothing in any Topic assesses it.** `architecture_mvi_single_state` assesses one value against several streams, a different axis; `kotlin_sealed_types_001` assesses the language mechanism. The epic's acceptance criteria name this comparison explicitly | Add coverage in E26-08; **the widest gap in the epic** |
| GAP-U2-C | 2 / `lesson_state_out_intentions_in` | The concrete failures of exposing a mutable state holder across the application boundary — a second write path, a breakable invariant, no single place to look | `unidirectional_data_flow_001` assesses the direction definitionally and `compose_udf_event_direction` assesses the composable-tree version in another Topic. The application-boundary failure is unassessed | Add coverage in E26-08 |
| GAP-U2-D | 2 / `lesson_owner_scoped_work` | Deciding whether a piece of work belongs in an owner-scoped scope, from what must happen when the user leaves | `viewmodel_scope_cleared_cancellation` assesses exactly this and sits in `async_reactive` as supporting-only, so it creates no Unit 2 practice. **Lower priority than the gaps above** precisely because the reasoning is assessed somewhere; E26-08 should weigh duplication against routing | Add coverage in E26-08 only if it can pose the *ownership* decision rather than the cancellation fact |
| GAP-U3-A | 3 / `lesson_what_a_repository_owns` | What a repository is responsible for, and recognising a feature where adding one buys nothing | **Both Questions that held this concept are DEPRECATED**, leaving the ACTIVE bank with one repository Question about API shape. L3.1's entire contract is unassessed, and the misconception it corrects — "a repository is a wrapper around the API or the DAO" — is the Unit's reason to exist | Add coverage in E26-08; **the widest gap in Unit 3**. Must not re-ask the retired wording |
| GAP-U3-B | 3 / `lesson_coordinating_sources` | What the consumer is and is not told about origin, freshness and failure when a repository coordinates two sources | The persistence Topic assesses staleness policy and offline-first from the storage side; no Question poses it as the repository's own decision about its contract | Add coverage in E26-08 |
| GAP-U3-C | 3 / `lesson_single_source_of_truth` | Choosing the authoritative source for a specific fact from a stated requirement, including a case where it is not the database | `single_source_of_truth_001` is definitional and `offline_first_001` states the offline-first recommendation. Nothing makes the reader decide, which is the only form in which the misconception "source of truth means database" is actually corrected | Add coverage in E26-08; **strong candidate** |
| GAP-U4-A | 4 / `lesson_when_a_domain_layer_earns_its_place` | Whether a feature earns a domain **layer**, as distinct from whether one operation earns a use-case class | The two ACTIVE `use_cases` Questions both work at the class level and are well matched to L4.2. The DEPRECATED `architecture_tradeoffs_001` was the closest at the layer level. Lower priority, since a reader who can answer both class-level Questions is close | Add coverage in E26-08 if capacity allows |
| GAP-U4-B | 4 / `lesson_clean_architecture_intent` | That the dependency rule fixes direction and not a layer count, and that "Clean Architecture means three layers" is a template rather than the rule | `clean_architecture_dependency_rule_tradeoff` assesses the consequences of following the rule and takes the layer structure as given. The misconception the epic's own issue text names is unassessed | Add coverage in E26-08; **strong candidate** |
| GAP-U5-A | 5 / `lesson_one_screen_five_questions` | Describing a screen's architecture by ownership, mutation path, view knowledge, input and output, without using a pattern name | **`mvc` holds zero ACTIVE Questions.** The classification skill is the Unit's whole contribution and is unassessed | Add coverage in E26-08 |
| GAP-U5-B | 5 / `lesson_mvvm_observed_state` | What makes an arrangement MVVM — an observing UI and an owner holding no view reference — as opposed to a class named `ViewModel` | `mvvm`'s only ACTIVE Question assesses the ViewModel/repository responsibility split, which is Unit 2 and Unit 3 reasoning. **MVVM itself is unassessed**, and the folder-structure misconception is untouched | Add coverage in E26-08. Pairs with the re-map candidate below — if `viewmodel_vs_repository_responsibility` moves, `mvvm` becomes empty and this gap becomes obligatory |
| GAP-U5-C | 5 / `lesson_mvi_intent_and_reduction` | MVI's transition model — intent as a value, one current state, a reduction from state and intent to the next state — and its ceremony | `mvi`'s only ACTIVE Question is a delivery-and-consumption Question (see the re-map candidate). Nothing assesses reduction, and "MVI is MVVM plus sealed intents" cannot be corrected without it | Add coverage in E26-08; **strong candidate** |
| GAP-U5-D | 5 / `lesson_classifying_a_real_architecture` | Classifying a hybrid implementation that borrows ownership from one tradition and state modelling from another | `architecture_mvi_single_state` assesses the single-state property well and stops there. The classification of a real, unlabelled design is unassessed | Add coverage in E26-08 |
| GAP-U6-A | 6 / `lesson_delivery_guarantees` | Answering the four guarantee questions for a stated requirement, and concluding that acknowledgement or durability is needed | The reasoning is assessed twice in `async_reactive` (`stream_choice_cannot_supply_a_delivery_guarantee`, `shared_flow_try_emit_true_is_not_delivery`) and once under `mvi` (`architecture_ui_event_consumption`) — **none of it reachable from a Unit 6 primary**. The re-map below would close most of this gap without authoring anything | **Decide the re-map first**, then add coverage only for what it leaves open |
| GAP-U6-B | 6 / `lesson_choosing_the_owner_by_lifetime` | Selecting an owner by matching the lifetime a requirement needs, including the case where the screen-level owner is wrong in each direction | `architecture_state_holder_taxonomy` assesses one rung of the ladder. The selection itself, and the "even the screen owner is too short-lived" case, are assessed only in `background_work` and `async_reactive` | Add coverage in E26-08 |
| GAP-U6-C | 6 / `lesson_smallest_sufficient_architecture` | Choosing the smallest structure that satisfies stated requirements, and justifying a larger one when warranted | **`architecture_tradeoffs` holds zero ACTIVE Questions**, and this is the Lesson the epic exists to end on. Pairs with GAP-U1-E; one Question cannot serve both, because Unit 1 asks about one boundary and Unit 6 about a whole feature | Add coverage in E26-08; **the widest gap in Unit 6** |

### Mapping, level and defect candidates

These are candidate Question edits, not new Questions. Each is a mapping or level that
misdirects practice; **none of them makes a Question wrong**. E26-08 should decide on each
deliberately — a re-map changes which Unit's practice a Question appears in, and preserving
Question identity is a lifecycle requirement.

| Question | Current Subtopic | Reasoning it actually assesses | Suggested Subtopic | Consequence of leaving it |
| --- | --- | --- | --- | --- |
| `architecture_ui_event_consumption` | `mvi` | That a replay cache re-delivers to a late collector because the stream records no consumption, so handling once requires the consumption to be recorded somewhere that survives the collector | `state_ownership` | **The most consequential mismatch found.** Unit 5 practice contains the Unit 6 delivery Question, Unit 6 practice contains none, and GAP-U6-A stays wide. Moving it also leaves `mvi` empty, which GAP-U5-C would fill with the reasoning the Lesson actually teaches |
| `viewmodel_vs_repository_responsibility` | `mvvm` | The ViewModel/repository responsibility split, cache policy placement, and why a state holder must not hold a `Context` to format state | `state_ownership` or `repository_pattern` | Unit 5 practice contains a Unit 2/3 Question, and MVVM as a responsibility model stays unassessed. Moving it makes GAP-U5-B obligatory rather than optional, so the two decisions should be taken together |
| `architecture_paging_ownership` | `layered_architecture` | Which layer decides when a page is fetched, caches it and records where to resume | keep | Fair and well matched in Unit 3. In Unit 1 it asks a reader who has not met the data layer to name it. Lowest priority of the three; a taxonomy split would be the real fix and E26 does not make one |
| `dependency_direction_domain_framework_types` | `dependency_direction` | Recognising framework coupling from a signature and naming its consequence | keep; **level review** | `FOUNDATION` for reasoning that is a design judgement rather than recall. A level change is a candidate, not a defect |
| `mvp_vs_mvvm_view_contract` | `mvp` | Presenter-holds-view against owner-publishes-state | keep | No action. Recorded only because it is the one Question spanning two Unit 5 Lessons, which is fine |

**Two observations that are not gaps and must not be treated as quotas:**

- **The `architecture` Topic holds zero ACTIVE `ADVANCED` Questions** — 14 `APPLIED` and 8
  `FOUNDATION` across 22. Every other major Topic this curriculum has planned against has
  some. Units 5 and 6 carry the epic's subtlest reasoning — classification under contested
  names, and the delivery-guarantee argument — and if E26-08 authors Advanced material
  anywhere, that is where the reasoning would justify it, **on merit rather than to fill a
  level**.
- **Sixteen of the 29 Lessons have an ACTIVE Question that both reaches them through a
  primary mapping and assesses reasoning this plan places in them.** The other thirteen —
  L1.3, L1.4, L2.3, L2.5, L3.1, L3.2, L4.1, L5.1, L5.3, L5.4, L6.2, L6.3 and L6.4 — are
  exactly the gap list above, and their reasons differ: two Subtopics are empty, two
  Subtopics hold a Question about something else (see the re-map candidates), and the rest
  are Lessons whose reasoning the bank has simply never posed. For a Topic averaging 1.2
  ACTIVE Questions per Subtopic that is the expected shape, not a coverage failure, and it
  is exactly why the review above was read rather than counted.

---

## Part 6 — Unit practice routing, modelled now

Unit practice is resolved from a Unit's Lessons' **primary** concepts only; supporting
concepts never broaden practice eligibility. The pools below were computed by script from
the proposed mappings against the ACTIVE Questions in the bundled JSON on 2026-09-15, so
that the routing shape is a finding rather than something E26-08 discovers.

| Unit | Primary concepts | Pool | The Questions |
| --- | --- | --- | --- |
| 1 | `separation_of_concerns`, `dependency_direction`, `interface_boundaries`, `layered_architecture`, `architecture_tradeoffs` | **5** | `separation_of_concerns_001`, `dependency_direction_domain_framework_types`, `architecture_interface_boundary_ownership`, `architecture_paging_ownership`, `dto_entity_domain_model_boundary` |
| 2 | `state_ownership`, `unidirectional_data_flow` | **5** | `state_ownership_001`, `architecture_state_holder_taxonomy`, `durable_state_vs_one_off_event`, `viewmodel_activity_reference_lifetime`, `unidirectional_data_flow_001` |
| 3 | `repository_pattern`, `single_source_of_truth`, `layered_architecture`, `error_modeling` | **6** | `repository_observable_api_shape`, `single_source_of_truth_001`, `architecture_paging_ownership`, `dto_entity_domain_model_boundary`, `architecture_error_mapping_boundary`, `architecture_error_modeling_result_type` |
| 4 | `use_cases`, `clean_architecture`, `dependency_direction`, `interface_boundaries` | **5** | `architecture_use_case_reuse`, `domain_layer_passthrough_cost`, `clean_architecture_dependency_rule_tradeoff`, `dependency_direction_domain_framework_types`, `architecture_interface_boundary_ownership` |
| 5 | `mvc`, `mvp`, `mvvm`, `mvi`, `mvvm_vs_mvi` | **4** | `mvp_vs_mvvm_view_contract`, `viewmodel_vs_repository_responsibility`, `architecture_ui_event_consumption`, `architecture_mvi_single_state` |
| 6 | `state_ownership`, `architecture_tradeoffs` | **4** | `state_ownership_001`, `architecture_state_holder_taxonomy`, `durable_state_vs_one_off_event`, `viewmodel_activity_reference_lifetime` |

**Union: 21 of the 22 ACTIVE architecture Questions.** The one outside every pool is
`architecture_solid_dependency_substitution`, because `solid` is supporting-only by design.

### Structural overlaps, calculated rather than discovered

Four overlaps follow from the mappings. None is distortion of a mapping to separate
practice — the issue's own instruction is to map honestly and record the consequence — and
each is recorded so a reviewer who finds an earlier Unit's Question in a later Unit's
practice knows it was intended.

1. **Unit 6's pool is a strict subset of Unit 2's.** Both take `state_ownership` as a
   primary concept, Unit 2 through four Lessons and Unit 6 through three, so all four
   `state_ownership` Questions appear in both. A learner finishing Unit 2 therefore practises
   `durable_state_vs_one_off_event`, whose reasoning Unit 6 L6.1 teaches. **Will a learner
   finishing an earlier Unit receive a Question that requires a later Unit? Yes, once.**
   The mapping is nevertheless correct in both places: `state_ownership` is genuinely what
   both Units are about, at different depths, and the taxonomy has no finer concept for
   "UI state modelling" or "occurrence acknowledgement". Recorded as a taxonomy limitation
   rather than remedied; a split of `state_ownership` would be a question-bank change.
2. **Units 1 and 4 share `dependency_direction` and `interface_boundaries`**, and therefore
   share two Questions. Both assess reasoning L4.3 and L4.4 complete: one names a domain use
   case accepting framework types, the other asks which module an interface belongs in. A
   Unit 1 reader can make progress on both from direction and ownership alone, but neither
   is fully Unit 1's. Recorded; **the alternative — demoting `dependency_direction` or
   `interface_boundaries` to supporting in Unit 1 — was rejected**, because L1.3 and L1.4
   genuinely teach those concepts and a Lesson that teaches a concept thoroughly declares it
   primary.
3. **Units 1 and 3 share `layered_architecture`**, and therefore `architecture_paging_ownership`
   and `dto_entity_domain_model_boundary`. Both are data-layer Questions and both are well
   matched to Unit 3. In Unit 1 they ask a reader who has not yet met the data layer to name
   its responsibilities. `dto_entity_domain_model_boundary` is defensible there, since L1.5
   uses model mapping as its central cost example; `architecture_paging_ownership` is the
   weaker fit and is recorded as the lowest-priority mapping candidate.
4. **Units 1 and 6 share `architecture_tradeoffs`**, which currently holds **no ACTIVE
   Question**, so the overlap is presently free. It stops being free the moment E26-08
   authors GAP-U1-E or GAP-U6-C: one Question would enter both Units' practice. The two gaps
   are deliberately written as different questions — one boundary against a whole feature —
   so that two Questions can exist without one being a rephrasing of the other.

**No shipped Unit's practice pool changes.** No shipped Lesson takes an `architecture`
Subtopic as primary, so nothing E26 maps can reach an existing Unit. This is the reverse of
the situation E25 recorded, where new Questions entered a shipped E23 Unit.

**E26-08 must re-compute these pools through the production resolver** rather than trusting
this table, exactly as E25-08 did, because the mappings may have moved during authoring and
because the resolver — not this script — is what the learner meets.

---

## Part 7 — This repository's own architecture, as evidence

Read from `shared/src/commonMain/kotlin` on 2026-09-15. **This is evidence for examples and
counterexamples, not a model to imitate and not a target to refactor.** The application is
small, entirely local-first, has no network layer at all, and was built under constraints
that a Lesson must state whenever it uses the code as an illustration. E26 proposes **no
change to production architecture code**, and no authoring issue may.

### What is actually there

| Finding | Detail | What it is evidence of |
| --- | --- | --- |
| **15 `ViewModel` classes, all in `commonMain`, all `internal`** | `AppShellViewModel`, `TopicBrowserViewModel`, `TopicDetailViewModel`, `LearningUnitViewModel`, `LearningLessonViewModel`, `PracticeBuilderViewModel`, `AssessmentTakingViewModel`, `AssessmentLaunchViewModel`, `FocusedResultViewModel`, `ProgressViewModel`, `ProgressTopicViewModel`, `MistakeReviewViewModel`, `SavedQuestionsViewModel`, `InterviewStartViewModel`, `MixedInterviewResultViewModel` | That a screen-level owner can be shared multiplatform code. **L2.1, L2.2** |
| **5 state holders that are not ViewModels** | `StudyProgressStateHolder`, `ProgressStateHolder`, `MistakeReviewStateHolder`, `InterviewHistoryStateHolder`, `SavedQuestionStateHolder` — Koin `single`s taking an injected `AppCoroutineScope` | **The single best example in the repository for L2.1**: the same state-holder responsibility with a different lifetime and no framework base class |
| **An explicit application-lifetime scope** | `AppCoroutineScope(CoroutineScope(SupervisorJob() + Dispatchers.Default))`, a distinct type "so injecting it is unambiguous", documented as existing because the caches on it "are shared by several screens and survive a navigation entry being destroyed, so they cannot belong to a `viewModelScope`" | **L2.5 and L6.3.** The production code already states the lifetime argument those Lessons teach, in its own words |
| **5 repository interfaces owned by their consumers** | `AssessmentRepository`, `CurriculumRepository`, `LessonStudyRepository`, `SavedQuestionRepository` in feature packages; `LearningContentRepository` in `curriculum/learning/repository` | **L4.4.** The abstraction sits with the consumer and the implementation depends inward |
| **Implementations in a separate package tree** | `LocalAssessmentRepository`, `LocalCurriculumRepository`, `LocalLessonStudyRepository`, `LocalSavedQuestionRepository` under `data/local/**`; `BundledLearningContentRepository` under `curriculum/learning/content` | **L1.3, L4.4.** A worked dependency direction in one module, which is also the cleanest available refutation of "a layer needs a module" |
| **Entity-to-domain mapping at the repository boundary** | `LocalLessonStudyRepository` maps `StudiedLessonEntity` to `StudiedLesson`; no Room type is visible above `data/local` | **L3.5.** A real, small model boundary with its mapping cost visible |
| **No DAO is used outside `data/local`** | Verified by search | **L3.1.** The data layer's entry point really is the repository |
| **Every repository method is a one-shot `suspend` function; no repository returns a `Flow`** | Verified by search across every interface and implementation | **L3.4's counterexample, and the most instructive finding here.** The app needs observable study state and gets it from a *state holder* that re-reads after every mutation and republishes a `StateFlow`, not from an observable repository. It is a legitimate design with a stated reason, and it is exactly the trade-off L3.4 asks the reader to weigh |
| **One source of truth, stated as such** | `StudyProgressStateHolder`'s KDoc: "[repository] remains the source of truth. Nothing is stored here that the database does not already hold: this is the in-memory projection the UI observes, read back from the repository after every mutation rather than assembled independently" | **L3.3.** An authoritative source with an in-memory projection, named correctly and distinguished from a cache |
| **No class named `UseCase` or `Interactor` anywhere** | Instead: `StudyProgressService`, `MistakeReviewService`, `LearningProgressService`, `AssessmentRetakeService`, plus pure `object` policies (`ContinueLearningPolicy`, `LearningRecommendationPolicy`, `LearningProgressPolicy`, `RecentPerformancePolicy`) and derivations (`StudyProgressDerivation`, `UnresolvedMistakeDerivation`, `LearningPerformanceDerivation`) | **L4.1 and L4.2.** The repository has domain logic with no domain *layer* and no use-case naming convention — orchestration lives in services and policy in pure functions. It is the concrete form of "the responsibility earns the class, the name does not" |
| **UI state is modelled both ways** | 16 UI state types: mostly sealed interfaces (`LearningLessonUiState`, `TopicDetailUiState`, `ProgressUiState`, …) and one data class (`PracticeBuilderUiState`), whose KDoc explains the choice — "Everything the Practice Builder renders, and nothing it would have to derive" | **L2.3's central comparison, available in one codebase.** A configuration screen whose fields are independent is a data class; screens with mutually exclusive outcomes are sealed |
| **A partial state nested rather than promoted** | `LearningLessonUiState.Content.studyState: StudyProgressUiState<LessonStudyUiModel>`, with the reason recorded: the study record and the document "fail independently … Promoting it would turn a missing indicator into a page the learner cannot read at all" | **L2.3.** The partial-state case with a real justification, which is more useful than an invented example |
| **A real reducer, not called one loudly** | `LessonScrollStateReducer` turns raw scroll positions into `LessonScrollUiState` | **L5.4.** A transition function in a codebase nobody would call MVI |
| **`SavedStateHandle` is used nowhere in production code** | Verified by search; `rememberSaveable` is used for navigation area state and small UI flags | **A limit on this repository as evidence.** L2.2's saved-state material must come from documentation, and the Lesson must not imply this app demonstrates it |
| **Koin classic DSL, ViewModels resolved at the destination boundary** | `topicStudyPresentationModule` and the `data/local/**` modules; `koinViewModel()` at each destination | Named once in L2.1 as *how this app supplies an owner*, and otherwise **E27's**. No Lesson teaches Koin |
| **Navigation 3 entry-scoped ownership is explicit** | `App.kt` installs `rememberSaveableStateHolderNavEntryDecorator()` then `rememberViewModelStoreNavEntryDecorator()`, so each back-stack entry owns its `ViewModelStore` | **L2.2.** The owner whose lifetime decides what survives, in code, and it matches the JetBrains documentation's statement that this scoping is not automatic |

### How a Lesson may and may not use this

- It **may** say "in this application, X is done this way, for this stated reason".
- It **may** use a finding as a counterexample — the one-shot repository API is the clearest.
- It **must not** convert a local design into a general recommendation. This app has no
  network layer, so it has never had to resolve a conflict between two sources; its
  source-of-truth reasoning is therefore simpler than the reasoning L3.3 teaches, and the
  Lesson must say so rather than presenting the app as the worked example.
- It **must not** propose a refactor. E26 is learning-content work; if authoring surfaces a
  genuine production defect, it is recorded for a separate issue and not fixed here.

---

## Part 8 — Kotlin Multiplatform ViewModel and lifecycle findings

**This is the acceptance-critical technical section of E26-01**, and every claim below was
settled against the resolved artifact sources and this repository's configuration rather
than inferred from Android-only documentation. Checked 2026-09-15 against
`androidx.lifecycle:lifecycle-viewmodel:2.11.0-beta01`, resolved from the Gradle cache as
the implementation behind the `org.jetbrains.androidx.lifecycle` artifacts
`shared/build.gradle.kts` declares.

**Finding 1 — `ViewModel` is common in this project's stack.** The resolved sources jar
declares `public expect abstract class ViewModel` in
`commonMain/androidx/lifecycle/ViewModel.kt`, with `ViewModel.jvmAndAndroid.kt` and
`ViewModel.nonJvm.kt` actuals, and `ViewModelStore`, `ViewModelStoreOwner`,
`ViewModelProvider` and `CreationExtras` alongside it in `commonMain`. This repository
declares `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose` in `commonMain`, and
its 15 ViewModels are all in `commonMain`. **A Lesson may say the class is available in
shared code here and must name the artifact and the version.**

**Finding 2 — clearing is what ends the ViewModel, and the store is what clears it.**
`ViewModelStore.clear()` calls `ViewModel.clear()` on each stored instance;
`ViewModelImpl.clear()` closes every registered `AutoCloseable` and then `onCleared()` runs.
`ViewModelStore`'s own KDoc states the contract directly: the store "must be retained across
configuration changes", and when the owner is destroyed permanently it "should call `clear`".
**The owner decides the lifetime; the ViewModel has none of its own.** This is the
source-level basis for L2.2's central correction.

**Finding 3 — `viewModelScope` is common, and its dispatcher is not the same on every
target.** `viewModelScope` is a `commonMain` extension property on `ViewModel`. It lazily
creates a scope, registers it as a keyed closeable, and returns the same instance
afterwards. From `createViewModelScope()` in
`commonMain/androidx/lifecycle/viewmodel/internal/CloseableCoroutineScope.kt`:

- the context is `Dispatchers.Main.immediate + SupervisorJob()`;
- where `Dispatchers.Main` is unavailable the implementation catches `NotImplementedError`
  (named in the source as Native environments such as Linux) and `IllegalStateException`
  (named as "JVM Desktop environments where `Dispatchers.Main` might not exist (e.g.,
  Swing)") and falls back to **`EmptyCoroutineContext`**, so the coroutine then runs in the
  caller's context;
- `close()` on that scope cancels its context, and because the scope is registered under a
  key it is cancelled by `clear()`.

JetBrains' multiplatform ViewModel documentation states the same constraint in words —
`ViewModel.viewModelScope` "is tied to `Dispatchers.Main.immediate`, which might be
unavailable on desktop by default" — and recommends `kotlinx-coroutines-swing`. **This
repository declares `libs.kotlinx.coroutinesSwing` in `desktopApp/build.gradle.kts`**, so
the condition is satisfied here. L2.5 may teach `viewModelScope` as a scope whose lifetime
is the owner's and **must state the dispatcher caveat rather than claiming a uniform main
dispatcher**.

**Finding 4 — the owner is platform-supplied, and Navigation 3 scoping is not automatic.**
JetBrains' documentation states that Jetpack Compose finds the Activity-provided
`ViewModelStoreOwner` on Android, that desktop uses Compose Multiplatform's common
implementation, that on iOS "there is no built-in `ViewModelStoreOwner`, so the ViewModel's
lifecycle must be tied to SwiftUI manually", and that "when using ViewModels with
Navigation 3 in common code, ViewModels are not automatically scoped to navigation entries
by default" — explicit `rememberViewModelStoreNavEntryDecorator()` is required. **This
repository installs exactly that decorator** (`App.kt`), which is why each back-stack entry
owns its store here. The existing Question `kmp_shared_viewmodel_owner_platform` assesses
the same conclusion.

**Finding 5 — one further common-code constraint worth knowing and not teaching.** The same
documentation notes that on non-JVM platforms type reflection is unavailable, so
`viewModel()` cannot be called without parameters in common code and an initializer must be
supplied. This is a **construction** detail, which is E27's territory; it is recorded so an
authoring issue does not discover it mid-Lesson and mistake it for architecture.

**What E26-03 may and may not say**

- It **may** say the ViewModel class, `viewModelScope` and clearing are usable from
  `commonMain` here, naming the artifact and the version.
- It **must not** say "a ViewModel survives configuration changes" without naming the host —
  that guarantee belongs to the Android `ViewModelStoreOwner` and does not follow from the
  class being shared. `viewmodel_activity_reference_lifetime` states the Android case
  correctly and L2.2 must scope rather than contradict it.
- It **must not** say `viewModelScope` always runs on the main dispatcher.
- It **must not** claim that this repository demonstrates `SavedStateHandle`; it does not
  use it.
- It **must** re-verify against the version then configured, because `2.11.0-beta01` is a
  beta.

**Platform limitation carried forward from E25 and unchanged:** the `iosArm64` device target
is not compiled locally and is not built on CI. Multiplatform claims rest on
`iosSimulatorArm64` plus the published artifact for `iosArm64`.

---

## Part 9 — Source policy in practice

The blueprint records the [source families and the hierarchy](architecture-learning-blueprint.md#authoritative-source-families).
This section records what was actually opened during this review, and the findings that
constrain authoring.

### Pages opened and quoted, 2026-09-15

| Page | Settles |
| --- | --- |
| `developer.android.com/topic/architecture/ui-layer/events` | The UI/user/ViewModel event classification; that ViewModel events "should always result in a UI state update"; and the delivery statement L6.2 is built on — when the producer outlives the consumer, these solutions "don't guarantee the delivery and processing of those events" |
| `developer.android.com/topic/architecture/ui-layer/stateholders` | The screen-level against UI-element state-holder distinction; that the business-logic holder is "typically implemented with a `ViewModel`" and the UI-logic holder "with a plain class"; and the placement rule — "produce UI state using state holders closest to where it is consumed" |
| `developer.android.com/topic/architecture/data-layer` | The five repository responsibilities; that "other layers … should never access data sources directly"; that "each repository defines a single source of truth"; that a local source is the **recommended** source of truth *for offline-first support*; and the `suspend`-for-one-shot / `Flow`-for-changes split |
| `developer.android.com/topic/architecture/domain-layer` | That the domain layer "is an _optional_ layer"; use-case naming and lifecycle; and the warning that the pattern "forces you to add use cases even when they are just simple function calls to the data layer, which can add complexity for little benefit" |
| `blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html` | The Dependency Rule verbatim — "source code dependencies can only point inwards" — and, decisively for L4.5, that "the circles are schematic … There's no rule that says you must always have just these four" |
| `martinfowler.com/eaaDev/uiArchs.html` | That "MVC is one of the most misunderstood architectural patterns around"; that different readers take different ideas from it; that MVP's own originators' descriptions "don't entirely mesh"; that many designs "use 'controller' as a synonym for presenter"; and Presentation Model as MVVM's ancestor |
| `kotlinlang.org/docs/multiplatform/compose-viewmodel.html` | The multiplatform ViewModel findings in [Part 8](#part-8--kotlin-multiplatform-viewmodel-and-lifecycle-findings) |
| `kotlinlang.org/docs/multiplatform/compose-lifecycle.html` | The desktop `Dispatchers.Main` caveat and the web lifecycle limits |

`developer.android.com/topic/architecture/recommendations` is already cited by
`viewmodel_vs_repository_responsibility` and is a source family for Units 2–4; it was not
re-opened during this review and an authoring issue should open it rather than trust this
note.

### Two findings that change how a Lesson must be phrased

1. **"Recommended" is not "required", and the data-layer page is precise about it.** The
   local-source-of-truth statement is scoped to offline-first support. L3.3 must teach the
   recommendation together with the condition that produces it, or it will manufacture the
   "single source of truth means database" misconception it exists to correct.
2. **The events page's conclusion is stronger than the slogan it replaces.** It does not say
   "use `SharedFlow` for events"; it says to handle such events immediately and **reduce them
   to UI state**, and gives the reason — UI state "gives you more delivery and processing
   guarantees". L6.2 should teach that conclusion and its reason, which is also why the Unit
   does not answer the question as a comparison between stream types.

### MVP, MVVM and MVI terminology variance

**Acceptance-critical, and the source contract for E26-06.** There is no single normative
specification governing modern usage of these names. The curriculum therefore records what is
stable enough to teach, what varies, and what it will rely on instead.

| Aspect | Stable enough to teach | Varies between sources |
| --- | --- | --- |
| **MVC** | That it names a three-part separation originating in Smalltalk, and that the "controller" role in particular is understood differently by different communities | Almost everything else. Fowler states directly that MVC "is one of the most misunderstood architectural patterns around" and that systems using the name "display a range of important differences". **The curriculum teaches MVC as the origin of the vocabulary and as the reason the vocabulary is unreliable, and asserts no canonical definition** |
| **MVP** | That the presenter holds a view abstraction and pushes updates into it, that the view is passive, and that attach/detach bookkeeping follows from the presenter holding the view | The boundary between "supervising controller" and "passive view"; whether the presenter may contain formatting; and the word itself — Fowler notes that "a lot of designs will follow the MVP style but use 'controller' as a synonym for presenter", and that the Potel and Dolphin descriptions "don't entirely mesh" |
| **MVVM** | That the UI observes state the owner publishes and the owner holds no reference to the view | Whether MVVM requires data binding; whether a class must be named `ViewModel`; whether a screen may expose more than one observable. The name descends from Fowler's Presentation Model, and **Android's own architecture guidance describes the arrangement without using the acronym at all** — which is itself the strongest evidence that the label is optional |
| **MVI** | That input is modelled as an intent value, that one current state describes the screen, and that a transition from state and intent to the next state is explicit | Almost every implementation detail. MVI originates in a fully reactive JavaScript framework (Cycle.js, André Staltz) rather than in Android, and the Android adaptations differ from it and from each other on whether reduction must be pure, whether a store abstraction exists, how side outputs are modelled, and what "effect" names |
| **"Effect"** | Nothing — the word is used for two unrelated abstractions in material this curriculum already teaches | MVI literature uses "effect" for a one-off output; Compose uses it for `LaunchedEffect`, `DisposableEffect` and `SideEffect`. **The blueprint's terminology register forbids the unqualified word**, and L5.4 must state the collision explicitly |
| **"Event"** | Nothing unqualified — it means user input, a domain occurrence, a transient UI output, or a stream emission depending on the sentence | Android's own events page distinguishes user events, UI events and ViewModel events. The curriculum uses "intention" for user input and "occurrence" where the delivery question is live, and qualifies "event" everywhere else |

**The rule E26-06 works under:** where sources disagree, teach dependency, ownership,
mutation path, state flow, view knowledge and lifetime, and say that the naming varies.
**Do not resolve a disagreement by adopting one author's class diagram**, and do not present
any blog as normative. A widely cited author may be quoted as *evidence of how practitioners
use a term*, labelled as such.

---

## Part 13 — Boundaries with the shipped curricula

Stated concept by concept so that E26 applies these mechanics rather than reteaching them.
E26-09's acceptance criteria check exactly this.

### E23 owns Compose state and execution

E26 does **not** teach: `mutableStateOf`, `remember`, `rememberSaveable` as a mechanism,
recomposition, recomposition scopes, composable identity, `key`, stability, `@Stable` and
`@Immutable`, `derivedStateOf`, or the snapshot system.

E26 **uses** them: L2.3 relies on immutability and equality-based skipping being already
understood, and cites `lesson_immutability_vs_stability` rather than re-arguing it; L2.1
applies the reader/writer/lifetime test from `lesson_state_hoisting` at application scale
and says so.

### E24 owns coroutines and Flow

E26 does **not** teach: `CoroutineScope` construction, `Job`, structured concurrency,
cancellation, dispatchers, `withContext`, Flow builders, operators, `flowOn`, buffering,
`StateFlow` and `SharedFlow` mechanics, `stateIn`/`shareIn`, or `SharingStarted`.

E26 **asks a different question about them**: which architectural owner should hold that
scope (L2.5, L6.3), what shape a data API should take for a stated consumer requirement
(L3.4), and what guarantee a requirement needs before any stream type is considered (L6.2).
The delivery conclusions of `lesson_choosing_a_stream_abstraction` and
`shared_flow_try_emit_true_is_not_delivery` are **cited, not re-derived**.

### E25 owns the Compose-side production boundary

E26 does **not** teach: the stateless content boundary, `collectAsState`,
`collectAsStateWithLifecycle`, `LaunchedEffect`, `DisposableEffect`, `SideEffect`,
`produceState`, `rememberCoroutineScope`, `rememberUpdatedState`, effect keys, or the
composable-tree form of state-down/events-up.

E26 **starts behind that boundary**, at exactly the sentence
`lesson_screen_state_owner_boundary` stops on. The complete deferral ledger is
[Part 3](#part-3--the-e25--e26-handoff-ledger).

---

## Part 14 — Boundaries with later and adjacent curricula

| Curriculum | E26 owns | It owns | Where the boundary is stated |
| --- | --- | --- | --- |
| **E27 — Dependency injection** | Which dependency a component should have, who should own the abstraction, why a boundary exists | Dagger, Hilt, Koin, modules, bindings, graph assembly, scopes, qualifiers, injected construction of any owner | L2.1 and L4.4. Constructor parameters may appear in E26 examples; DI-framework mechanics may not |
| **E29 — Gradle, modularization, build** | Logical layers and responsibility boundaries | Module boundaries, build-level dependency graphs, Gradle configuration, convention plugins, build performance | L1.5, in one bounded statement: a logical layer is not a build unit |
| **E31 — Testing** | That a boundary makes substitution possible, as a *consequence* | Test implementation, fakes and mocks, architecture testing strategy, coroutine and Flow testing, integration tests | L1.4. No E26 Lesson shows a test |
| **E33 — Kotlin Multiplatform** | Shared ViewModels, repositories and domain logic as examples; what the policy/detail split makes shareable | `expect`/`actual`, source sets, platform services, iOS integration, platform abstraction mechanics | L4.3, one sentence; and Part 8's claims, which are scoped to what E26 asserts about ownership |
| **Lifecycle and navigation** | Owner and lifetime *conclusions* | Activity and Fragment lifecycle, navigation APIs, back-stack mechanics, predictive back, the `SavedStateHandle` API surface | L2.2 and L6.3, which use lifetime facts and teach no lifecycle API |
| **Persistence** | Authority, source of truth, cache, durability, model boundaries | Room, SQL, migrations, DataStore, paging implementation, eviction | L3.1–L3.5 |
| **Networking** | Remote sources as a concept, transfer-model boundaries, remote/local coordination | Retrofit, Ktor, OkHttp, HTTP semantics, serialization | L3.1, L3.2, L3.5 |
| **Background work** | The conclusion that an operation needs an owner longer-lived than the screen, or than the process | WorkManager, foreground services, OS scheduling constraints, retry and backoff policy | L2.5 names it once; L6.3 reaches the conclusion and stops |

---

## Handoff

**Sequencing.** E26-02 → E26-03 → E26-04 → E26-05 → E26-06 → E26-07 → E26-08 → E26-09, in
Unit order. The order is a real dependency chain: Unit 2 applies Unit 1's boundary test to a
specific component, Unit 4 asks its question about the repository Unit 3 designed, Unit 5
classifies what Units 1–4 built, and Unit 6 synthesises all five.

**What each authoring issue must not redesign.** Unit identity, Unit title, Lesson
identities, Lesson order, primary and supporting mappings, and the Teach/Bridge/Reference/
Exclude decisions are settled here and in the blueprint. An authoring issue that finds one
of them wrong updates **both documents in the same change** and records why, as E24-07 did
for two Lesson titles.

**What E26-09 should verify beyond its own acceptance criteria:**

- Terminology is consistent across the six Units **and with the shipped Compose and
  Coroutines Units**, against the blueprint's
  [terminology register](architecture-learning-blueprint.md#terminology-this-curriculum-fixes).
  "Effect" and "event" are the two to read hardest, because both collide with vocabulary the
  shipped Units already own.
- No E26 Lesson re-derives a shipped E23, E24 or E25 argument. L2.1 against
  `lesson_state_hoisting` and `lesson_classes_of_screen_state`, and L6.2 against
  `lesson_transient_effect_delivery`, are the two boundaries to read hardest.
- The [E25 ledger](#part-3--the-e25--e26-handoff-ledger) is walked deferral by deferral, and
  each row is marked answered by a shipped Lesson or still deferred with its reason. Three
  rows are expected to come back "still deferred by design": construction and injection
  (E27), queueing architecture (excluded), and the `SavedStateHandle` mechanism (lifecycle
  curriculum).
- Both shipped Lessons that point at this curriculum —
  `lesson_screen_state_owner_boundary` and `lesson_transient_effect_delivery` — are re-read
  and confirmed to still read correctly.
- Unit practice for each of the six Units is re-computed through the production resolver with
  the four overlaps in [Part 6](#part-6--unit-practice-routing-modelled-now) in mind, and the
  result recorded rather than assumed.

**Limitations carried forward, none of which is a defect to fix during authoring:**

1. **Unit 6's practice pool is a strict subset of Unit 2's**, through `state_ownership`.
   Only a taxonomy split would change it, and E26 does not make one.
2. **Units 1 and 4 share two Questions, and Units 1 and 3 share two**, for the reasons in
   Part 6. All four mappings are honest and all four are recorded.
3. **`architecture_tradeoffs` holds no ACTIVE Question**, so L6.4 — whose only primary
   concept it is — is reached by nothing, and L1.5's proportionality half is reached only
   through `layered_architecture`, whose two Questions are about the data layer. This is the
   epic's clearest assessment gap and is deliberately not solved by remapping.
4. **`architecture_solid_dependency_substitution` reaches no E26 Unit**, by design.
5. **The `architecture` Topic holds no ACTIVE `ADVANCED` Question.** Not a quota to fill;
   recorded because Units 5 and 6 are where the reasoning would justify one.
6. **This repository has no network layer and no `SavedStateHandle` usage**, so it cannot
   illustrate multi-source conflict resolution or saved state. Lessons touching those must
   say so rather than stretching the example.
7. **`lifecycle` remains at `2.11.0-beta01`** and the multiplatform ViewModel contracts in
   Part 8 must be re-verified rather than preserved by assumption.
8. **`iosArm64` is not compiled locally or on CI.** Inherited from E25 and unchanged.
9. **`gh` is not installed in this environment**, so issue #361 was read through
   `.github/project/backlog.yml`, which is the synchronised source of the issue text.

---

## Authoring outcomes

Each authoring issue appends its outcomes here, following the precedent of
`docs/content/coroutines-flow-units-1-6-plan.md` and
`docs/content/compose-units-7-12-plan.md`: what did not change, corrections made during
review, claims that were executed rather than reasoned about, source decisions, the semantic
review of the Questions the Unit now reaches, and the cross-links and validation performed.

E26-02 is the first to append here.

---

## Authoring outcomes for Unit 1

Added by E26-02 after the five Lessons were written. Everything below was checked against
the artifacts this repository resolves, or executed against them; nothing here is recalled.

### What did not change

Every proposed Unit id, Lesson id, title, authored order and primary/supporting mapping for
Unit 1 in the [identity tables](#identity-conventions-and-proposed-identities) shipped
verbatim. **No Lesson boundary moved, none was split or merged, and neither planning document
needed a correction.** The blueprint's `L1.1`–`L1.5` ordering, its Teach/Bridge/Reference/
Exclude decisions and its misconception targets were followed as written, so the plan and the
production content say the same thing.

| Shipped identity | Title | Primary | Supporting |
| --- | --- | --- | --- |
| `lesson_what_architecture_decides` | What Architecture Actually Decides | `separation_of_concerns` | `architecture_tradeoffs`, `layered_architecture`, `android_modules` |
| `lesson_responsibility_and_change` | Responsibility, Cohesion and What Changes Together | `separation_of_concerns` | `solid`, `architecture_tradeoffs`, `interface_boundaries` |
| `lesson_dependency_direction_and_boundaries` | Which Way May This Dependency Point? | `dependency_direction` | `interface_boundaries`, `solid`, `layered_architecture`, `module_dependency_direction` |
| `lesson_when_an_interface_is_a_boundary` | When an Interface Is a Boundary, and When It Is Only Indirection | `interface_boundaries` | `dependency_direction`, `solid`, `architecture_tradeoffs`, `test_doubles` |
| `lesson_layers_and_their_cost` | Layers as One Answer, and What They Cost | `layered_architecture`, `architecture_tradeoffs` | `separation_of_concerns`, `android_modules`, `modularization_tradeoffs` |

The Unit is `unit_architecture_responsibilities_and_boundaries`, titled **Architecture as
Responsibilities and Boundaries**, homed in `architecture`, and **appended after
`unit_stateflow_sharedflow_and_hot_streams`** — position 19 of 19. List position is the
ordering contract for a publisher-owned document and is what `ContinueLearningPolicy` walks,
so a learner studying straight through now reaches the architecture path after the Compose
path and the coroutines path, which is the same precedent E24-02 set. No existing Unit moved
and no shipped Lesson was edited.

All five Lessons carry Core, Practical and Senior depth and run 1,584–1,797 words including
code, inside the 552–2,307 range the 72 previously shipped Lessons occupy.

### Editorial decisions worth recording

1. **One argument, one worked example.** The Unit is built around a single small feature — a
   list of borrowed items with a due date, an overdue rule and a renewal write — and one
   recurring requirement change, a three-day grace period. Every Lesson re-poses the same
   question against it, so the five Lessons read as one argument rather than five articles.
   The example deliberately uses neutral component names (renderer, policy, formatter, store,
   loader, gateway), because Unit 1 precedes every named component this curriculum teaches.
2. **The Unit opens on a changing requirement, and no Lesson opens on a diagram.** No layer
   stack, no `UI → Domain → Data`, no Clean Architecture and no pattern name appears before
   L1.5, which is the acceptance-critical ordering decision.
3. **Backward-only links, used sparingly.** `lesson_state_hoisting` is linked once, from
   L1.2, where the reader/writer/lifetime ownership test is actually generalised, rather than
   from all five Lessons. Forward material is prose naming the Unit that owns it.
4. **Later components are named only to defer them.** `LessonStudyRepository` is named twice
   as repository *evidence* — once in L1.4 as a one-implementation interface that is still a
   boundary, once in L1.5 as a logical boundary inside one Gradle module — and both times the
   Lesson says the repository's own responsibility is a later Unit's subject. No Lesson
   previews state holders, `ViewModel`, use cases, domain layers, single source of truth,
   MVP/MVVM/MVI, DI frameworks, testing mechanics or KMP source sets.
5. **SOLID is named once, in L1.2, as vocabulary**, with an explicit statement that the
   principles carrying real reasoning are met in L1.4 and in Unit 4. No Lesson walks the five
   principles.
6. **Cost is always stated as things, never as "complexity".** L1.4 and L1.5 each list the
   concrete costs — a name, a file, a navigation hop, a contract to keep in sync, a second
   representation, mapping code, more change sites — because "abstractions add complexity"
   was the sentence the blueprint set out to replace.

### Sources used, and the claims they settle

Four sources across the Unit, all primary architectural writing or official Android
documentation. Every quotation below was read on the page during authoring rather than
recalled, and each is attached to the specific claim it supports.

| Source | Used by | Claim it settles |
| --- | --- | --- |
| [Guide to app architecture](https://developer.android.com/topic/architecture) | L1.1, L1.2, L1.5 | Separation of concerns as "separating your app into methods, classes, files, packages, modules and layers that have clearly defined responsibilities and boundaries"; at least two layers, UI and data, with the domain layer described as an addition rather than a default |
| [Architecture recommendations](https://developer.android.com/topic/architecture/recommendations) | L1.1, L1.5 | "Treat the recommendations in the document as recommendations and not strict requirements"; UI-layer components "don't interact directly with a data source"; the domain layer "Recommended in big apps" with its stated conditions; small apps may place data-layer types in "a `data` package or module" |
| [The Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html) | L1.1, L1.3, L1.4, L1.5 | "Independent of Frameworks … does not depend on the existence of some library of feature laden software"; the flow of control against source dependencies in *Crossing boundaries*; testability as a property that follows from the arrangement; and, decisively for L1.5, "The circles are schematic … There's no rule that says you must always have just these four" |
| [The Single Responsibility Principle](https://blog.cleancoder.com/uncle-bob/2014/05/08/SingleReponsibilityPrinciple.html) | L1.2 | "each software module should have one and only one reason to change"; "Gather together the things that change for the same reasons. Separate those things that change for different reasons"; "This principle is about people" |
| [InterfaceImplementationPair](https://martinfowler.com/bliki/InterfaceImplementationPair.html) | L1.4 | "Interfaces should be designed around your clients' needs, often these don't match the implementation"; "Using interfaces when you aren't going to have multiple implementations is extra effort to keep everything in sync"; and the published-library exception, where users "don't get that fast feedback" |

Two source-sensitive decisions follow from that reading and are recorded because a later Unit
could easily contradict them:

- **The Clean Architecture claims Unit 1 makes are bounded to two.** That frameworks are
  tools rather than architecture, and that the four-circle diagram prescribes no layer count.
  The dependency rule itself — "source code dependencies can only point inwards" — is named
  in L1.3 as a rule belonging to a specific architecture and **not taught**; L4.5 owns it, and
  Unit 1's wording is compatible with the primary-source conclusion E26-01 verified.
- **The Android guidance is quoted with its conditions attached.** L1.5 states the domain
  layer's "in big apps" condition and the "recommendations and not strict requirements" note
  in the same paragraph, because dropping the condition is how the guidance turns into "every
  app has three layers" — the misconception the Lesson exists to correct.

No MVP/MVVM/MVC definition is asserted anywhere in the Unit. MVVM appears once, inside a
category-error example ("we use Compose, so our architecture is MVVM"), where the point is
that the framework decides none of the questions the pattern name is supposed to answer. The
terminology variance recorded in [Part 9](#mvp-mvvm-and-mvi-terminology-variance) is therefore
untouched and stays E26-06's to resolve.

### Repository evidence used, and how

Two findings from [Part 7](#part-7--this-repositorys-own-architecture-as-evidence) are used,
both re-verified against the code during authoring rather than taken from the table:

- `LessonStudyRepository` (in the study feature's package) and `LocalLessonStudyRepository`
  (under `data/local`), both `internal`, both in `:shared`, with no Room type visible above
  `data/local`. L1.4 uses it as a one-implementation interface whose contract is still
  consumer-owned; L1.5 uses it as a logical boundary that needed no Gradle module.
- `settings.gradle.kts` includes five modules and the whole shared application is one of
  them, which is what makes the second claim checkable.

Both appearances say what the application does and for what stated reason. Neither presents
the repository as a model to copy, and **no production architecture code was changed or
proposed for change.**

### Semantic review of the Questions this Unit now reaches

All five ACTIVE Questions in the Unit's resolved practice pool were re-read in full and
independently solved against the finished prose. The E26-01 findings in
[Part 5](#part-5--semantic-assessment-review) **all still hold**; nothing below overturns one.

| Question | Level | Resolved by | Re-read verdict against the shipped Lessons |
| --- | --- | --- | --- |
| `separation_of_concerns_001` | FOUNDATION | `separation_of_concerns` (L1.1, L1.2) | Answerable from L1.1's Core alone. Its explanation's second half — that dependencies point toward policy rather than upward to the UI — is also taught, in L1.3. Still definitional: it asks what separation is good for and never makes the reader judge a split, which is GAP-U1-B |
| `dependency_direction_domain_framework_types` | FOUNDATION | `dependency_direction` (L1.3) | Sound and unchanged. L1.3 teaches enough to answer it — a component inherits the reasons to change of what it names, and the shared-payload example is the same failure one step earlier — but the Question's own framing is a domain use case accepting `android.net.Uri` and returning a Retrofit `Response`, which is L4.3's material. **Level not changed here**, as the issue requires; the FOUNDATION-for-a-design-judgement observation stands as an E26-08 candidate |
| `architecture_interface_boundary_ownership` | APPLIED | `interface_boundaries` (L1.4) | Sound. L1.4 teaches the property the Question turns on — the consumer owns the abstraction it depends on — but the Question asks **which module** the interface belongs in, and module placement is deliberately L4.4's. A Unit 1 reader can reach the right answer from the ownership property; the full justification arrives in Unit 4. Recorded, not repaired |
| `architecture_paging_ownership` | APPLIED | `layered_architecture` (L1.5) | Sound, and the weakest fit in the pool. It asks which layer decides when a page is fetched, in data-layer vocabulary (`PagingSource`, `RemoteMediator`, caching) that Unit 1 never introduces. L1.5 gives the reasoning shape — responsibilities group by level and dependency rule — but not the vocabulary. This is exactly the consequence E26-01 predicted and left in place rather than remapping |
| `dto_entity_domain_model_boundary` | APPLIED | `layered_architecture` (L1.5) | Sound, and the best of the two `layered_architecture` matches. Its argument — layer-specific models let network, storage and domain evolve independently **at the cost of mapping** — is L1.5's central comparison, and the Lesson's diverged `StoredLoan`/`LoanSummary` example teaches both halves. The full model-boundary decision is still L3.5's |

`architecture_solid_dependency_substitution` was re-read as context. It remains sound, it
remains outside every E26 pool because `solid` is supporting-only, and L1.2's one-paragraph
SOLID treatment does not attempt to make it answerable. The four DEPRECATED architecture
Questions were re-read and **not edited**; `architecture_tradeoffs_001` is the one whose
concept L1.5 now teaches, which strengthens rather than changes the GAP-U1-E recommendation.

**No Question was created, edited, re-mapped, re-levelled or re-statused by this issue.**

### GAP-U1-A to GAP-U1-E after authoring

All five gaps survive the finished prose unchanged; authoring did not prove any gap
definition wrong, and E26-08 still owns all five.

| Gap | Status after E26-02 | What the finished Lesson changes about it |
| --- | --- | --- |
| GAP-U1-A | **Open, unchanged** | L1.1 now teaches the reasoning — two package trees with identical dependencies, `ui`/`domain`/`data` packages with a screen that constructs a store, and two framework category errors — so the gap is now a gap in *assessment* of material that ships, which is the strongest case an E26-08 Question can have |
| GAP-U1-B | **Open, unchanged** | L1.2's requirement-change table is precisely the reasoning no ACTIVE Question poses: given three behaviours, decide which change independently. `separation_of_concerns_001` remains definitional |
| GAP-U1-C | **Open, unchanged** | L1.3 teaches direction for two ordinary components and states the cost of reversing it in both directions. The only ACTIVE `dependency_direction` Question remains the domain/framework-types case |
| GAP-U1-D | **Open, unchanged; still the strongest candidate** | L1.4 ships the exact scenario the gap describes — an interface whose contract mirrors its single implementation, both edited together — together with the condition that would make it a boundary. Nothing assesses it |
| GAP-U1-E | **Open, unchanged; still the widest gap in the Unit** | L1.5 ships the before/after comparison the gap asks for, and `architecture_tradeoffs` still holds **zero** ACTIVE Questions, so half of L1.5's primary mapping reaches nothing at all |

**One new observation, not a new gap.** L1.3's distinction between source dependency, call
direction and data flow — the callback that moves control and data without moving the
dependency — is taught in the Unit and is assessed nowhere in any Topic. It is recorded here
as a candidate for E26-08 to weigh beside GAP-U1-C rather than as a sixth gap, because it is
the same Lesson's reasoning at a different angle and a single Question may well cover both.

### Actual practice reach, resolved through the production resolver

Recomputed by running the shipped Unit through `PracticeBuilderViewModel` and the real
selection path in `LearningUnitPracticeIntegrationTest`, not by reading mappings. The Unit
configures `AssessmentScope.Subtopics` of exactly its five primary concepts and resolves
**five** Questions:

| Resolved Question | Level | Reached through | Semantically belongs mainly to |
| --- | --- | --- | --- |
| `separation_of_concerns_001` | FOUNDATION | `separation_of_concerns` | Unit 1 |
| `dependency_direction_domain_framework_types` | FOUNDATION | `dependency_direction` | **Unit 4** (L4.3's policy/framework reasoning) |
| `architecture_interface_boundary_ownership` | APPLIED | `interface_boundaries` | **Unit 4** (L4.4's inversion and placement reasoning) |
| `architecture_paging_ownership` | APPLIED | `layered_architecture` | **Unit 3** (data-layer responsibilities) |
| `dto_entity_domain_model_boundary` | APPLIED | `layered_architecture` | **Unit 3**, with its mapping-cost half genuinely useful in L1.5 |

This matches [Part 6](#part-6--unit-practice-routing-modelled-now)'s modelled pool exactly, so
no mapping moved during authoring.

Three structural limitations are now facts rather than predictions, and all three are asserted
in the test suite so they cannot drift silently:

1. **`architecture_tradeoffs` contributes nothing.** It is primary in L1.5 and holds no ACTIVE
   Question, so the Unit's proportionality reasoning is reachable only through
   `layered_architecture`, whose two Questions are both data-layer scenarios. The Unit's pool
   is therefore five Questions drawn from four of its five primary concepts.
2. **Four of the five Questions are principally later Units' material**, for the honest-mapping
   reasons above. A learner finishing Unit 1 can make progress on all of them from ownership,
   direction and cost alone, but only `separation_of_concerns_001` is fully Unit 1's.
3. **Supporting concepts broaden nothing.** `solid`, `android_modules`,
   `module_dependency_direction`, `test_doubles` and `modularization_tradeoffs` contribute no
   Question, and `architecture_solid_dependency_substitution` stays outside the pool — verified
   by assertion, not by inspection.

### Cross-links

One backward link leaves the Unit: `lesson_responsibility_and_change` →
`lesson_state_hoisting`. The other links are within-Unit and backward only, following the
shipped precedent: L1.3 → L1.1, L1.2; L1.4 → L1.2, L1.3; L1.5 → L1.1, L1.3, L1.4; L1.1 links
to nothing, because it has no prerequisite. **No shipped Lesson was edited**, no forward link
was invented, and `lesson_screen_state_owner_boundary` and `lesson_transient_effect_delivery`
were re-read and still read correctly — E26-09 confirms that again at the end of the epic.

### Tests changed, and why

| File | Change | Why production data made it necessary |
| --- | --- | --- |
| `BundledLearningCurriculumTest` | Unit id, title and home-Topic lists extended by one; Lesson id/title order and primary mappings for the new Unit added; two new tests — `architectureFoundationsUnitKeepsItsPlannedBridgesOutOfPrimaryPractice` and `architectureFoundationsUnitLinksBackwardsOnlyAndReachesItsOneShippedAnchor` | The document now spans three home Topics, and the supporting mappings are where the `solid` decision would silently erode |
| `LearningUnitPracticeIntegrationTest` | `existingLearnerTraversesTheExpansionWithLiveParentProgressAndDurableIdentities` extended to walk the architecture Unit before expecting Continue Learning to report Complete, and its final studied-record count raised from 71 to 76; new test `theArchitectureFoundationsUnitPractisesItsPrimaryConceptsAndNothingElse` | Continue Learning walks the whole document, so a third home Topic changes the traversal. The new Unit could not join the shared expectation table, because that table asserts that every primary concept yields at least one Question and `architecture_tradeoffs` yields none — the bespoke test asserts the exact five resolved ids and that gap instead |

No test was added that only re-states schema validation already performed by
`LearningCurriculumValidatorTest`, and the existing data-driven suites — the reader journey
over every shipped Unit, Topic Detail's Unit rows, the end-to-end repository path — needed no
edit because they read the document rather than listing it.

### Validation performed

| Command | Result |
| --- | --- |
| `python3 -c` structural pre-check over both bundled JSON documents | Ids unique, every mapping an ACTIVE Subtopic, no primary/supporting overlap, every `relatedLessonIds` target resolvable |
| `./gradlew :shared:jvmTest --tests "*BundledLearningCurriculumTest*" --tests "*LearningUnitPracticeIntegrationTest*" --tests "*LearningContentEndToEndTest*"` | 48 tests, all passing (first run surfaced two real defects: blank leading comparison headers, rejected by `LearningCurriculumValidator`, and the Continue Learning traversal above) |
| `./gradlew :shared:jvmTest` | **1,387 tests, 0 failures**, including `LearningProductionContentJourneyTest`, which renders every authored block of the new Unit in the reader, checks the reading column never widens, and opens every authored Source link |
| `python3 tools/learning_question_coverage.py --write` then `--check` | Snapshot regenerated and reported current |
| `cd tools && python3 -m unittest test_learning_question_coverage.py` | 21 tests, OK |
| `./gradlew :androidApp:assembleDebug` | Passed |
| `./gradlew :shared:check` | Passed |
| `git diff --check` | Clean |

The regenerated `docs/content/learning-question-coverage.md` now reports **19 active Units and
77 active Lessons**, one primary Subtopic with no active Question — `architecture_tradeoffs`,
which is GAP-U1-E — and a Unit pool of five Questions across two levels, with no ADVANCED
Question, matching the Topic-wide observation in Part 5.

### Not validated

- **`iosArm64` is not compiled locally or on CI**, unchanged from E25 and E26-01.
- **No CI run is claimed.** Nothing in this issue was observed on GitHub Actions.
- **Backlog validation could not be run**: `PyYAML` is unavailable in this environment, so
  `.github/project/backlog.yml` was read as text rather than parsed and validated. Issue #362
  was read from that file rather than through `gh`, which is still not installed.
- **The learning content itself is editorial** and no automated check can confirm that a
  Lesson teaches what it claims; the semantic review above is a judgement, as Rule 10 of the
  authoring contract requires.

---

## Authoring outcomes for Unit 2

Added by E26-03 after the five Lessons were written. Every version, source claim and
repository fact below was re-checked or executed during authoring rather than carried over
from E26-01's tables; where a finding is unchanged, that is stated as a re-verification and
not as a copy.

### What did not change

Every proposed Unit id, Lesson id, title, authored order and primary/supporting mapping for
Unit 2 in the [identity tables](#identity-conventions-and-proposed-identities) shipped
verbatim. **No Lesson boundary moved, none was split or merged, and neither planning
document needed a correction.** The blueprint's `L2.1`–`L2.5` ordering, its
Teach/Bridge/Reference/Exclude decisions and its misconception targets were followed as
written. E26-02's Unit 1 outcome held as well: nothing in Unit 2 required a Unit 1 identity,
mapping or boundary to move.

| Shipped identity | Title | Primary | Supporting |
| --- | --- | --- | --- |
| `lesson_state_holder_responsibility` | What a Screen State Holder Is Responsible For | `state_ownership` | `compose_state_hoisting`, `viewmodel_lifecycle`, `kmp_lifecycle_viewmodel`, `separation_of_concerns` |
| `lesson_viewmodel_lifetime_and_persistence` | The ViewModel Owner: Lifetime Is Not Persistence | `state_ownership` | `viewmodel_lifecycle`, `configuration_changes`, `process_death`, `saved_state`, `kmp_lifecycle_viewmodel` |
| `lesson_modelling_ui_state` | Modelling the Current UI State | `state_ownership` | `kotlin_sealed_types`, `error_modeling`, `compose_stability`, `compose_state` |
| `lesson_state_out_intentions_in` | State Out, Intentions In | `unidirectional_data_flow` | `state_ownership`, `compose_udf`, `stateflow` |
| `lesson_owner_scoped_work` | Work Whose Lifetime Is the Owner's | `state_ownership` | `lifecycle_coroutines`, `coroutine_scope`, `viewmodel_lifecycle`, `background_api_selection` |

The Unit is `unit_screen_state_holders_and_ui_state`, titled **Screen State Holders,
ViewModel and UI State**, homed in `architecture`, and inserted **directly after
`unit_architecture_responsibilities_and_boundaries`** — position 20 of 20, and second within
the architecture sequence. No existing Unit moved, no shipped Lesson was edited, and
`state_ownership` is supporting in `lesson_state_out_intentions_in` while being primary in
the other four, which is the one place in the Unit where the two roles meet.

All five Lessons carry Core, Practical and Senior depth and run 1,498–2,052 words including
code, inside the 552–2,307 range the previously shipped Lessons occupy.

### Configured versions, re-verified

Read from `gradle/libs.versions.toml`, `shared/build.gradle.kts` and
`desktopApp/build.gradle.kts` during authoring. **Nothing changed since E26-01.**

| Component | Version at E26-01 | Version now | Changed? |
| --- | --- | --- | --- |
| Kotlin | 2.4.10 | 2.4.10 | No |
| Compose Multiplatform | 1.11.1 | 1.11.1 | No |
| `androidx-lifecycle` (via `org.jetbrains.androidx.lifecycle`) | 2.11.0-beta01 | 2.11.0-beta01 | No |
| kotlinx.coroutines | 1.11.0 | 1.11.0 | No |
| Koin | 4.2.2 | 4.2.2 | No |
| Navigation 3 (`multiplatform-nav3-ui`) | catalog | 1.1.1 | No |
| `kotlinx-coroutines-swing` in `desktopApp` | declared | declared | No |

**The lifecycle dependency is still a beta**, so the contracts below were read from the
resolved sources again rather than preserved by assumption, and E26-04 must do the same.

### KMP ViewModel and lifecycle source verification

Re-read from the resolved `androidx.lifecycle:lifecycle-viewmodel-android:2.11.0-beta01`
sources jar in the Gradle cache — the implementation behind the
`org.jetbrains.androidx.lifecycle` artifacts this project declares — and from the current
JetBrains multiplatform ViewModel documentation. **All five of [Part 8](#part-8--kotlin-multiplatform-viewmodel-and-lifecycle-findings)'s
findings hold unchanged; none was overturned and none needed re-phrasing.**

| Contract | Verified statement | Where L2.2 or L2.5 uses it |
| --- | --- | --- |
| `ViewModel` common availability | `public expect abstract class ViewModel` in `commonMain/androidx/lifecycle/ViewModel.kt`, with `ViewModelStore` and `ViewModelStoreOwner` beside it in `commonMain` | L2.2 names the artifact and the version when saying the class is shared here |
| `ViewModelStore` | Its KDoc still states both halves of the contract: an instance "must be retained across configuration changes", and an owner "being destroyed permanently … should call `clear`" | L2.2 quotes both, as the source of the owner-decides-lifetime claim |
| Clearing semantics | `ViewModelStore.clear()` snapshots the map, clears it and calls `ViewModel.clear()` on each instance; `ViewModelImpl.clear()` closes every registered `AutoCloseable` before `onCleared()` | L2.2's rung 5 and the statement that the class contributes only a hook |
| `viewModelScope` | A `commonMain` extension property that lazily calls `createViewModelScope()`, registers it under `VIEW_MODEL_SCOPE_KEY` via `addCloseable`, and returns the same instance afterwards | L2.5's three properties |
| Scope context and dispatcher | `createViewModelScope()` builds `Dispatchers.Main.immediate + SupervisorJob()`, catching `NotImplementedError` (source comment: Native environments such as Linux) and `IllegalStateException` (source comment: "JVM Desktop environments where `Dispatchers.Main` might not exist (e.g., Swing)") and falling back to `EmptyCoroutineContext` | L2.5 states the fallback explicitly and says the main dispatcher is not guaranteed |
| Scope cancellation | `CloseableCoroutineScope.close()` cancels its context, and the key registration is what makes `clear()` cancel it | L2.5's "cancellation is what being owned means" |
| Host-supplied ownership | The JetBrains page still states that Android's Compose finds the `Activity`-provided owner, that "Compose Multiplatform provides a common `ViewModelStoreOwner` implementation", and that on iOS "there is no built-in `ViewModelStoreOwner`, so the ViewModel's lifecycle must be tied to SwiftUI manually" | L2.2's per-host bullet list |
| Navigation 3 scoping | The same page still states that "when using ViewModels with Navigation 3 in common code, ViewModels are not automatically scoped to navigation entries by default" | L2.2's Navigation 3 paragraph |
| Reflection limit | The page still states that `viewModel()` cannot be called without parameters in common code on non-JVM platforms | **Not used.** It is a construction detail and E27's, exactly as Finding 5 records |

Two phrasing decisions follow, and both are acceptance-critical:

- **L2.2 never says "a ViewModel survives configuration changes" unqualified.** The claim
  appears only as rung 3 of the ladder, attributed to the Android host retaining the store,
  and the per-host list immediately afterwards says the same shared class gets a different
  answer on each target. This scopes `viewmodel_activity_reference_lifetime` rather than
  contradicting it.
- **L2.5 never says `viewModelScope` runs on the main dispatcher.** It states the attempt,
  the two caught exceptions, the empty-context fallback, and the fact that this repository
  supplies `kotlinx-coroutines-swing` to its desktop shell — then says explicitly that the
  dispatcher is not the lesson.

### Repository evidence used, and its limits

Six findings from [Part 7](#part-7--this-repositorys-own-architecture-as-evidence) were
re-verified against current code before use, and all six were unchanged.

| Finding | Re-verified as | Used by |
| --- | --- | --- |
| 15 `ViewModel` classes, all `internal`, all in `commonMain` | 15, unchanged | L2.2, as the reason the shared-class-different-lifetime point matters here |
| 5 non-ViewModel state holders | `StudyProgressStateHolder`, `ProgressStateHolder`, `MistakeReviewStateHolder`, `InterviewHistoryStateHolder`, `SavedQuestionStateHolder`, unchanged | **L2.1's acceptance-critical example.** Named as plain classes holding the same responsibility with a different lifetime |
| `AppCoroutineScope` | Unchanged, including the KDoc sentence that its caches "are shared by several screens and survive a navigation entry being destroyed, so they cannot belong to a `viewModelScope`" | L2.5's Senior section, as a lifetime requirement that produced a type |
| `PracticeBuilderUiState` as a data class | Unchanged, with its "everything the Practice Builder renders, and nothing it would have to derive" KDoc | L2.3, as one codebase choosing differently per screen |
| `LearningLessonUiState.Content.studyState` nested rather than promoted | Unchanged, including the recorded reason that promoting it "would turn a missing indicator into a page the learner cannot read at all" | **L2.3's partial-state example** |
| `rememberViewModelStoreNavEntryDecorator()` installed in `App.kt` after `rememberSaveableStateHolderNavEntryDecorator()` | Unchanged | L2.2, as why forward navigation and pop differ here |
| `SavedStateHandle` unused in production Kotlin | Re-verified: it appears only inside the two bundled curriculum JSON documents, never in `shared/src/**/*.kt` | L2.2 says so explicitly rather than implying the app demonstrates saved state |

Three limits were observed. No Lesson presents this application as a model to copy; every
appearance says what the app does and for what stated reason. **Koin is named nowhere in the
Unit** — L2.1 says only that construction is a separate decision with its own curriculum,
without naming a container or a DSL. And **no production architecture code was changed or
proposed for change**; nothing in authoring surfaced a product defect worth recording.

### Editorial decisions worth recording

1. **The Unit continues Unit 1's worked feature rather than opening a new one.** The
   borrowed-items screen — loans, due dates, an overdue rule, a filter, a renewal — is the
   same feature Unit 1 used, now given a screen. This keeps the epic reading as one argument
   and, more importantly, **leaves the practice-configuration screen free for Unit 5**,
   which the plan reserves for it.
2. **The responsibility is named before any class, in every Lesson that could name one.**
   L2.1's Core lists five responsibilities and only then says `ViewModel` is one
   implementation; the phrase "state holder = ViewModel" is rejected explicitly. Unit 1's
   decision model — responsibility, ownership, dependency, lifetime, cost — is applied in
   the opening paragraph and nowhere re-taught.
3. **Four ownership levels on one screen, with one value correctly staying local.** L2.1's
   table places a help-panel expansion (local), a search draft (local, with the requirement
   that would move it), the filter and loan rows (screen-level) and a borrowed-items badge
   (longer-lived shared owner). "All production state belongs in the ViewModel" is rejected
   against the first row and the fourth in opposite directions.
4. **L2.2 re-asks E25's ladder from the store's side, deliberately.**
   `lesson_screen_state_owner_boundary` already answers "does the value survive?" from the
   Composition's edge. L2.2 says so and then asks each rung as two questions — which owner,
   and is it still holding its store — so the Lesson adds the mechanism rather than
   repeating the conclusion. The table's columns differ from the shipped one's for the same
   reason.
5. **The three concepts are separated in a table, not a sentence.** Ownership, lifetime and
   persistence each get a question and an answer for the same screen, and the
   lifetime-is-not-persistence correction is then made twice: once at rung 6 of the ladder
   and once in the Senior section, which is what the issue asks for.
6. **L2.3 grew a field in the sealed model rather than pretending the comparison was even.**
   `Content.refreshFailed` exists because a failed refresh over populated content is not a
   fourth top-level state, and the Lesson says that the field appearing *is* the trade-off
   arriving. Both models are then compared against six requirements, and the impossible
   combination is shown as a literal constructor call.
7. **L2.4 names three failures as consequences, never as "encapsulation".** A second write
   path that races the first, an invariant no type enforces, and the loss of one place to
   look are each stated with the failure they produce. The intention-vs-setter argument is
   carried by a requirement change (selecting a filter must also clear the search box and be
   remembered) rather than asserted, and setter-shaped APIs are explicitly not banned.
8. **Forward material is prose that names a Unit, never a link.** L2.1 defers the lifetime
   ladder to "the closing unit of this curriculum", L2.4 defers intents and reduction to "the
   unit … that compares responsibility models", and L2.5 defers owner selection and
   background mechanisms the same way. No Unit 3–6 Lesson id appears anywhere, because none
   of them exists yet.
9. **MVVM, MVI and MVP are named nowhere in the Unit.** A ViewModel is never presented as
   implying MVVM, and no example is labelled with a pattern name. Unit 5 still has its
   subject.

### Sources used, and the claims they settle

Eight distinct pages across the five Lessons, each attached to a specific claim, each read
during authoring.

| Source | Used by | Claim it settles |
| --- | --- | --- |
| [State holders and UI state](https://developer.android.com/topic/architecture/ui-layer/stateholders) | L2.1, L2.3, L2.4 | The business-logic against UI-logic state-holder split; that the first is "typically implemented with a `ViewModel`" and the second "with a plain class"; and the placement rule, "you should produce UI state using state holders closest to where it is consumed" |
| [ViewModel overview](https://developer.android.com/topic/libraries/architecture/viewmodel) | L2.1, L2.2, L2.5 | That a ViewModel "remains in memory until the `ViewModelStoreOwner` to which it is scoped disappears"; that `onCleared()` runs when the owner destroys it; and, quoted directly in L2.1, that because they "can potentially live longer than the `ViewModelStoreOwner`", ViewModels "shouldn't hold any references of lifecycle-related APIs such as the `Context` or `Resources` to prevent memory leaks" |
| [`ViewModelStore`](https://developer.android.com/reference/androidx/lifecycle/ViewModelStore) | L2.2 | The store contract quoted in Core: retained across configuration changes, cleared when the owner is destroyed permanently |
| [Compose Multiplatform: ViewModel](https://kotlinlang.org/docs/multiplatform/compose-viewmodel.html) | L2.2, L2.5 | The per-host ownership statements and the Navigation 3 scoping statement, both quoted; and the `Dispatchers.Main.immediate` caveat |
| [Compose Multiplatform: Lifecycle](https://kotlinlang.org/docs/multiplatform/compose-lifecycle.html) | L2.5 | The desktop main-dispatcher condition that makes `kotlinx-coroutines-swing` relevant |
| [UI layer](https://developer.android.com/topic/architecture/ui-layer) and [UI events](https://developer.android.com/topic/architecture/ui-layer/events) | L2.1, L2.3, L2.4 (the UI-layer page); L2.4 (the events page) | UI state as one immutable value the screen renders; unidirectional data flow at the application boundary as state outward and events inward |
| [Sealed classes and interfaces](https://kotlinlang.org/docs/sealed-classes.html) | L2.3 | The language mechanism only, cited so a reader can follow the construct without the Lesson teaching it |

One source-sensitive decision is worth recording because a later Unit could contradict it:
**the Android guidance's state-holder split is used as a recommendation with its condition
attached, not as a taxonomy.** L2.1 teaches the responsibility and then says a `ViewModel`
is chosen when the lifetime its owner supplies is the lifetime required — which is what the
guidance's own reason ("particularly surviving `Activity` recreation") says — rather than
converting "typically implemented with" into a rule.

### UI-state modelling decisions

Recorded because GAP-U2-B is the epic's widest gap and E26-08 will author against what the
Lesson actually teaches.

- **The same screen is modelled twice**, on identical product requirements: a data class of
  four independent fields, and a three-variant sealed hierarchy. Neither is presented as
  correct.
- **Six requirements drive the comparison**: first load, content, full-screen failure, a
  refresh failure over populated content, a filter change during a load, and how much the
  renderer must branch. Two of the six favour each shape, which is the point.
- **The impossible combination is concrete**: a constructor call producing a populated list,
  `isLoading = true` and a non-null error at once. Its cost is stated as three consequences —
  every consumer must decide what it means, two consumers can decide differently, and the
  invariant is written nowhere — and it is shown to be reachable from one ordinary mistake.
- **The cost of eliminating it is stated as specifically**: every change becomes a transition
  rather than a field update, a new mode is a variant every consumer must handle, and partial
  updates become decisions about what to carry across.
- **Partial state is taught as a scoping rule**: a failure is modelled at the level of the
  thing that failed, shown as a nested region state and evidenced by
  `LearningLessonUiState.Content.studyState`.
- **"Make illegal states unrepresentable" is taught as a direction with a price that scales**,
  and the choice is explicitly per screen and from the product.
- **Two boundaries are stated in the Lesson itself**: error representation at the data
  boundary is deferred to the repositories Unit, and Compose stability is deferred to E23.

### UDF and write-path treatment

L2.4 states that the composable-tree half is already shipped, names why the application
boundary is a different argument (the write path crosses out of the UI layer, so what enters
decides what work runs), and then does four things the acceptance criteria name: shows the
broken mutable exposure as code with a legal UI write beside it; names the second write path,
the breakable invariant and the lost single place to look as consequences rather than as
"encapsulation"; shows the corrected surface; and carries the intention-vs-assignment
argument with a requirement change. The form/draft nuance is a two-row table decided by who
else must react before submission, and reducers, intent hierarchies and MVI stores are
deferred in one clause.

### Semantic review of the Questions this Unit now reaches

All five ACTIVE Questions in the resolved pool were re-read in full and independently solved
against the finished prose. **The E26-01 findings in [Part 5](#part-5--semantic-assessment-review)
all still hold**; nothing below overturns one.

| Question | Level | Reached through | Re-read verdict against the shipped Lessons |
| --- | --- | --- | --- |
| `state_ownership_001` | FOUNDATION | `state_ownership` | Answerable from L2.1's Core and L2.4's Core together — one owner, read-only publication, no second writer. Its explanation's aside about durability is now taught properly, by L2.2. Still definitional: it asks why ownership is good rather than making the reader decide an owner |
| `architecture_state_holder_taxonomy` | APPLIED | `state_ownership` | **The closest match in the bank to any Unit 2 Lesson's contract, and it survives the prose test.** L2.1's four-row ownership table teaches exactly the reasoning — a reusable component's internal state has no tie to the screen's lifetime, so a screen-level owner would hand it a lifetime and a dependency surface it never asked for — without using the Question's wording, its component or its distractors |
| `durable_state_vs_one_off_event` | APPLIED | `state_ownership` | Sound, and semantically the synthesis Unit's. Its state-against-occurrence reasoning is **not** pre-taught here: L2.3 models what a screen renders and L2.2 separates lifetime from persistence, neither of which is the durable-against-consumable decision. A Unit 2 reader can reach part of it from the lifetime-is-not-persistence material and not all of it, which is the accepted consequence of the shared primary concept |
| `viewmodel_activity_reference_lifetime` | FOUNDATION | `state_ownership` | Sound, and the one the Unit had to be careful with. L2.1's "what the holder must not know" list and the ViewModel documentation quote teach the failure directly; L2.2's rung 3 states the Android premise the Question rests on **as the Android host's guarantee**, so the Lesson scopes the claim rather than contradicting or generalising it |
| `unidirectional_data_flow_001` | FOUNDATION | `unidirectional_data_flow` | Sound and answerable from L2.4's Core. It remains definitional — direction only — and assesses none of the three concrete failures L2.4 teaches, which is GAP-U2-C unchanged |

Adjacent Questions were read as duplication guards and **none was edited or re-mapped**:
`viewmodel_scope_cleared_cancellation` (`async_reactive`), which assesses the cancellation
fact L2.5 deliberately does not re-derive; `viewmodel_vs_repository_responsibility` (`mvvm`),
whose `Context`-for-formatting half L2.1 now teaches from the ownership side while the
Question stays where it is; and `kmp_shared_viewmodel_owner_platform` (`kmp`), which reaches
the same conclusion as L2.2's per-host list from its own Topic. All three remain outside this
Unit's pool, which is correct.

**No Question was created, edited, re-mapped, re-levelled or re-statused by this issue**, and
no factual defect was found in any Question read.

### GAP-U2-A to GAP-U2-D after authoring

All four gaps survive the finished prose unchanged. Authoring proved no gap definition wrong,
and E26-08 still owns all four.

| Gap | Status after E26-03 | What the finished Lesson changes about it |
| --- | --- | --- |
| GAP-U2-A | **Open, unchanged** | L2.2 now ships the reasoning in full — the owner chain, the six-rung ladder answered by naming the owner, and the ownership/lifetime/persistence table. The architecture-side assessment still does not exist: the reasoning is assessed four times in `lifecycle_navigation` and once in `kmp`, none of it reachable from a Unit 2 primary, and `state_ownership_001` still carries it only as an explanation aside |
| GAP-U2-B | **Open, unchanged; still the widest gap in the epic** | L2.3 ships the same-screen comparison the gap describes, including the impossible combination, the partial-state case and the cost of removing combinations. **Nothing in any Topic assesses the choice**, so the gap is now a gap in assessment of material that ships — the strongest case an E26-08 Question can have |
| GAP-U2-C | **Open, unchanged** | L2.4 ships the three failures as named consequences with a code example of the broken surface. `unidirectional_data_flow_001` remains definitional and `compose_udf_event_direction` still assesses the composable-tree version in another Topic |
| GAP-U2-D | **Open, unchanged; still the lowest priority of the four** | L2.5 ships the ownership decision — two operations, one requirement test, and the conclusion that the second needs a different owner. The reasoning nearest to it, `viewmodel_scope_cleared_cancellation`, is still `async_reactive` supporting-only and still creates no Unit 2 practice, and it still assesses the cancellation fact rather than the ownership decision. E26-08's duplication judgement is unchanged by authoring |

**E25's GAP-U7-C is instructionally closed by this issue and remains open as assessment.**
E25-08 left it to this epic on the grounds that the ownership side of "moving state out of
the Composition changes the owner, and the owner's lifetime decides what survives" was
E26's to teach. L2.2 teaches it: the owner chain, the store, the clearing contract, the
per-host qualification and the separation of lifetime from persistence. What E25 deferred
was the *instruction*; the Question that would assess it is GAP-U2-A, which E26-08 still
owns. No E25 document was edited — the disposition is recorded here, and E26-09 checks the
ledger row.

### Actual practice reach, resolved through the production resolver

Recomputed by running the shipped Unit through `PracticeBuilderViewModel` and the real
selection path in `LearningUnitPracticeIntegrationTest`, not by reading mappings. The Unit
configures `AssessmentScope.Subtopics` of exactly its two primary concepts and resolves
**five** Questions:

| Resolved Question | Level | Reached through | Semantically belongs mainly to |
| --- | --- | --- | --- |
| `state_ownership_001` | FOUNDATION | `state_ownership` | Unit 2 |
| `architecture_state_holder_taxonomy` | APPLIED | `state_ownership` | Unit 2, and it is the Unit's best-matched Question |
| `viewmodel_activity_reference_lifetime` | FOUNDATION | `state_ownership` | Unit 2, scoped to the Android host |
| `unidirectional_data_flow_001` | FOUNDATION | `unidirectional_data_flow` | Unit 2 |
| `durable_state_vs_one_off_event` | APPLIED | `state_ownership` | **Unit 6** (L6.1's state-against-occurrence reasoning) |

This matches [Part 6](#part-6--unit-practice-routing-modelled-now)'s modelled pool exactly, so
no mapping moved during authoring, and it confirms E26-01's prediction that the pool would be
five rather than assuming it.

**The Unit 2 / Unit 6 routing limitation is now a fact rather than a prediction.**
`state_ownership` is primary in four Unit 2 Lessons and, in the plan, in three Unit 6
Lessons, so `durable_state_vs_one_off_event` reaches Unit 2 practice even though its
reasoning is L6.1's. It was **not** fixed by demoting `state_ownership`, re-mapping the
Question or inventing a taxonomy concept, all three of which the issue forbids and the plan
already rejected. Unit 2 does not pre-teach Unit 6 to compensate. The limitation is asserted
in the test suite so a later re-map has to re-state it rather than silently repairing it, and
E26-08 owns the decision.

Two further structural facts, both asserted rather than inspected:

1. **Supporting concepts broaden nothing.** The Unit's sixteen supporting-only concepts —
   including `viewmodel_lifecycle`, `configuration_changes`, `process_death`, `saved_state`
   and `kmp_lifecycle_viewmodel`, every one of which has ACTIVE Questions of its own —
   contribute no Question to the pool.
2. **No shipped Unit's practice changed.** No shipped Lesson takes an `architecture` Subtopic
   as primary, so nothing this Unit maps can reach an existing Unit's pool. Unit 1's pool is
   unchanged at five, and the two architecture Units share no Question, because their primary
   concepts are disjoint.

### Cross-links

Backward only, and every target already shipped. L2.1 → `lesson_state_hoisting`,
`lesson_classes_of_screen_state`, `lesson_screen_state_owner_boundary`; L2.2 →
`lesson_remember_saveable`, `lesson_screen_state_owner_boundary`, L2.1; L2.3 →
`lesson_immutability_vs_stability`, `lesson_screen_state_and_ui_events`, L2.1; L2.4 →
`lesson_state_down_events_up`, `lesson_screen_state_and_ui_events`, L2.3; L2.5 →
`lesson_coroutine_scope_ownership`, `lesson_remember_coroutine_scope`,
`lesson_who_owns_the_trigger`, L2.1.

This is the plan's intended graph plus one within-Unit backward link per Lesson, which
follows the Unit 1 precedent. **No shipped Lesson was edited**, no Unit 1 Lesson received a
reciprocal link, and no forward link was invented — asserted by a test that no other Lesson
in the document names a Unit 2 Lesson.

### Tests changed, and why

| File | Change | Why production data made it necessary |
| --- | --- | --- |
| `BundledLearningCurriculumTest` | Unit id, title and home-Topic lists extended by one; Lesson id/title order and primary mappings for the new Unit added; a stale comment corrected from "two home Topics" to three; two new tests — `stateHolderUnitKeepsItsPlannedBridgesOutOfPrimaryPractice` and `stateHolderUnitLinksBackwardsOnlyToShippedComposeAndCoroutineAnchors` | The document lists Units positionally, and the five lifecycle and Compose bridges are where a promotion to primary would silently claim practice the Unit did not earn. The link test also asserts that no shipped Lesson was edited to point into this Unit |
| `LearningUnitPracticeIntegrationTest` | `existingLearnerTraversesTheExpansionWithLiveParentProgressAndDurableIdentities` now expects two architecture Units, derives the Topic's Lesson total instead of hard-coding five, and raises its final studied-record count from 76 to 81; the shared expectation table gained a Unit 2 row; new test `theStateHolderUnitPractisesItsTwoPrimaryConceptsIncludingOneLaterUnitsQuestion` | Continue Learning walks the whole document, so a second architecture Unit changes the traversal and the Topic's progress denominator. Unlike Unit 1, Unit 2 fits the shared table — both primary concepts hold Questions — so the bespoke test exists only to pin the exact five ids and the Unit 2 / Unit 6 overlap |

No test was added that only re-states schema validation `LearningCurriculumValidatorTest`
already performs, and the data-driven suites — the reader journey over every shipped Unit,
Topic Detail's Unit rows, the end-to-end repository path — needed no edit because they read
the document rather than listing it.

### Validation performed

| Command | Result |
| --- | --- |
| `python3` structural pre-check over both bundled JSON documents | Ids unique, every mapping an ACTIVE Subtopic, no primary/supporting overlap, every `relatedLessonIds` target resolvable, no blank or placeholder text, every comparison row matching its header count |
| `./gradlew :shared:jvmTest --tests "*BundledLearningCurriculumTest*" --tests "*LearningUnitPracticeIntegrationTest*" --tests "*LearningCurriculumValidatorTest*" --tests "*LearningContentEndToEndTest*"` | 95 tests, 0 failures |
| `./gradlew :shared:jvmTest` | **1,390 tests, 0 failures**, including `LearningProductionContentJourneyTest`, which renders every authored block of the new Unit in the reader, checks the reading column never widens, and opens every authored Source link |
| `python3 tools/learning_question_coverage.py --write` then `--check` | Snapshot regenerated and reported current |
| `cd tools && python3 -m unittest test_learning_question_coverage.py` | 21 tests, OK |
| `./gradlew :shared:check` | Passed |
| `./gradlew :androidApp:assembleDebug` | Passed |
| `git status --short` and `git diff --check` | Five files changed, no build or cache output, no whitespace defects |

The regenerated `docs/content/learning-question-coverage.md` now reports **20 active Units
and 82 active Lessons**, the new Unit appearing directly after Unit 1 with five Lessons, a
pool of five Questions across two levels and no ADVANCED Question — which matches the
Topic-wide observation in Part 5 — and one primary Subtopic still with no active Question,
which remains `architecture_tradeoffs` and GAP-U1-E rather than anything Unit 2 introduced.

### Not validated

- **`iosArm64` is not compiled locally or on CI**, unchanged from E25, E26-01 and E26-02.
  The multiplatform claims in L2.2 and L2.5 rest on the resolved common sources, the
  official JetBrains documentation and `iosSimulatorArm64`.
- **No CI run is claimed.** Nothing in this issue was observed on GitHub Actions.
- **Backlog validation could not be run**: `PyYAML` is unavailable in this environment, so
  `.github/project/backlog.yml` was read as text rather than parsed and validated. Issue
  #363 was read from that file rather than through `gh`, which is still not installed.
- **The learning content itself is editorial** and no automated check can confirm that a
  Lesson teaches what it claims; the semantic review above is a judgement, as Rule 10 of the
  authoring contract requires.

---

## Authoring outcomes for Unit 3

Added by E26-04 after the five Lessons were written. Every source quotation, repository
finding and routing figure below was re-opened, re-verified against current code, or
executed during authoring rather than carried over from E26-01's tables; where a finding is
unchanged, that is stated as a re-verification and not as a copy.

### What did not change

Every proposed Unit id, Lesson id, title, authored order and primary/supporting mapping for
Unit 3 in the [identity tables](#identity-conventions-and-proposed-identities) shipped
verbatim. **No Lesson boundary moved, none was split or merged, and neither planning
document needed a correction.** The blueprint's `L3.1`–`L3.5` ordering, its
Teach/Bridge/Reference/Exclude decisions and its misconception targets were followed as
written. E26-02's and E26-03's outcomes held as well: nothing in Unit 3 required a Unit 1 or
Unit 2 identity, mapping or boundary to move.

| Shipped identity | Title | Primary | Supporting |
| --- | --- | --- | --- |
| `lesson_what_a_repository_owns` | What a Repository Is Responsible For | `repository_pattern` | `room_dao`, `retrofit`, `separation_of_concerns`, `architecture_tradeoffs` |
| `lesson_coordinating_sources` | Coordinating Local and Remote Sources | `repository_pattern` | `offline_first`, `cache_invalidation`, `caching` |
| `lesson_single_source_of_truth` | Which Source Is Authoritative? | `single_source_of_truth` | `offline_first`, `cache_invalidation`, `state_ownership` |
| `lesson_observable_or_one_shot_api` | An Observable API, or a One-Shot Read? | `repository_pattern` | `flow_fundamentals`, `flow_collection`, `stateflow`, `single_source_of_truth` |
| `lesson_model_and_error_boundaries` | Model and Error Boundaries: What May Cross | `layered_architecture`, `error_modeling` | `repository_pattern`, `kotlin_sealed_types`, `room_dao`, `retrofit` |

The Unit is `unit_repositories_and_data_ownership`, titled **Repositories, Data Ownership
and Single Source of Truth**, homed in `architecture`, and appended **directly after
`unit_screen_state_holders_and_ui_state`** — position 21 of 21, and third within the
architecture sequence. No existing Unit moved and no shipped Lesson was edited.

All five Lessons carry Core, Practical and Senior depth and run 1,515–2,161 words including
code, inside the 552–2,307 range the 82 previously shipped Lessons occupy.

**Configured versions were re-read** from `gradle/libs.versions.toml` during authoring and
are unchanged from E26-03: Kotlin 2.4.10, `androidx-lifecycle` 2.11.0-beta01, Koin 4.2.2,
Navigation 3 1.1.1, Room 3.0.1. **No [Part 8](#part-8--kotlin-multiplatform-viewmodel-and-lifecycle-findings)
contract is used by this Unit** — Unit 3 makes no claim about `ViewModel`, `viewModelScope`,
clearing or host-supplied owners — so nothing in Part 8 needed re-verification here, and
E26-05 inherits that obligation unchanged.

### Unit purpose, and how it stays separate from Unit 2

Unit 2 asked who owns the state a screen renders and how long that owner lives. Unit 3 asks
who owns the policy for obtaining and changing the application's data, and which copy is
authoritative when two disagree. The two are kept apart deliberately and in the prose:

- **No Lesson turns the state holder into the repository.** L3.3 states explicitly that the
  answer to two screens disagreeing about a fine is *not* to move the fine into a state
  holder, because a state holder owns what a screen is currently rendering rather than what
  is true for the application, and then names the three-part stack — service authoritative,
  repository exposing, state holder rendering.
- **No Lesson turns the repository into a persistence wrapper.** L3.1's whole argument is
  that a repository owning none of the listed decisions has created a file rather than a
  boundary.
- **The Unit's five Lessons form one argument**, in the order the issue requires: what
  responsibility would justify a repository → who coordinates several sources → which copy
  is authoritative → what API shape the consumer needs → which representations and failures
  may cross.

### Editorial decisions worth recording

1. **The Unit continues the borrowed-items feature and gives the library a service.** Units
   1 and 2 used loans with a due date, an overdue rule and a renewal write; Unit 3 keeps the
   same feature and adds a library service so a second source exists. The vocabulary carries
   over — `Loan`, `LoanSource`, `RenewalOutcome` — so the epic still reads as one argument,
   and the **practice-configuration screen stays reserved for Unit 5**.
2. **The hypothetical is labelled the first time it is used.** L3.2 opens by saying in as
   many words that this application has no network layer, so the coordination that follows is
   a design worked through rather than an example lifted from production. L3.3 repeats the
   limit in its Senior section. No invented remote implementation is ever described as this
   repository's production code.
3. **Every policy is stated as a consequence of stated requirements.** L3.2 lists three
   requirements before drawing a read path and then says outright that one changed
   requirement — a balance that must never be shown stale — makes the same path a defect.
   The lesson's Common Mistake is exactly the recital of "read locally, refresh, write to the
   database" as though it were the pattern.
4. **Cost is always stated as things.** Following the Unit 1 precedent, the pass-through
   repository's cost is a name, a file, two forwarding functions and a navigation hop; the
   model split's cost is two mapping functions, three places a field is named and a
   translation decision per field.
5. **No Lesson prescribes one error mechanism.** L3.5 lists a sealed result, an
   application-owned exception and an outcome carried inside returned data as three
   defensible shapes, quotes the guidance offering two of them, and states that choosing
   between them is deliberately not the lesson's argument.
6. **Forward material is prose that names a Unit, never a link.** L3.1 defers "which side
   should define that abstraction" to "the unit after this one" without naming a Lesson id,
   because no Unit 4 Lesson exists.
7. **No pattern name appears anywhere in the Unit.** MVVM, MVI and MVP are not named, so
   Unit 5 still has its subject.

### Sources used, and the claims they settle

Five distinct pages across the five Lessons, each opened during authoring and each attached
to the specific claim it supports.

| Source | Used by | Claim it settles |
| --- | --- | --- |
| [Data layer](https://developer.android.com/topic/architecture/data-layer) | L3.1, L3.2, L3.3, L3.4, L3.5 | The five repository responsibilities, quoted verbatim; "Each data source class should have the responsibility of working with only one source of data, which can be a file, a network source, or a local database"; "Other layers in the hierarchy should never access data sources directly; the entry points to the data layer are always the repository classes"; "It's important that each repository defines a single source of truth" and that the exposed data "should always be the data coming directly from the source of truth"; "In order to provide offline-first support, a local data source—such as a database—is the recommended source of truth"; the one-shot/notification split; the model-separation recommendation and its "At minimum" threshold; and both error representations — custom exceptions and a `Result` class |
| [Architecture recommendations](https://developer.android.com/topic/architecture/recommendations) | L3.1 | "Create repositories even if they contain only a single data source" and "Make sure components in the UI layer such as composables or ViewModels don't interact directly with a data source", both marked **Strongly recommended**, together with the page's own "Treat the recommendations in the document as recommendations and not strict requirements" |
| [Build an offline-first app](https://developer.android.com/topic/architecture/data-layer/offline-first) | L3.2, L3.3 | That the local data source "should be the exclusive source of any data that higher layers of the app read", which "ensures data consistency between connection states"; and, for L3.2's Senior claim, "Conflict resolution often requires versioning. The app needs to do some bookkeeping to keep track of when changes occurred, so it can pass the metadata to the network data source" |
| [Best practices for coroutines in Android](https://developer.android.com/kotlin/coroutines/coroutines-best-practices) | L3.4 | "Classes in those layers should expose suspend functions for one-shot calls and Flow to notify about data changes" — reused from E24's settled sources rather than re-researched |
| [Sealed classes and interfaces](https://kotlinlang.org/docs/sealed-classes.html) | L3.5 | The language mechanism only, cited so a reader can follow a closed error contract without the Lesson teaching sealed types |

Three source-sensitive decisions follow, and all three are acceptance-critical:

- **The offline-first recommendation is never separated from its condition.** L3.3 quotes
  the sentence whole, names the condition in the next sentence, gives the reason from the
  offline-first page, and then shows two facts in one application getting different answers.
  Dropping the condition is precisely how the recommendation becomes "the database is the
  source of truth".
- **"Create repositories even if they contain only a single data source" is taught as a
  uniformity decision, not as a refutation of the no-repository case.** L3.1 quotes it as
  strongly recommended, states its actual benefit — a uniform entry point, and one type to
  change when a responsibility arrives — and then says this is a decision about consistency
  across a codebase rather than a derivation from the feature's requirements, citing the
  page's own "recommendations and not strict requirements" note. The Lesson's claim about
  the pass-through type is narrow and survives the recommendation: it has created no
  *boundary* yet, which is a statement about what it isolates rather than an instruction to
  delete it.
- **The error guidance is used for its plurality.** The data-layer page offers custom
  exceptions *and* a `Result` class, which is what licenses L3.5 to teach the boundary
  without prescribing a mechanism.

### Repository evidence used, and its limits

Four findings from [Part 7](#part-7--this-repositorys-own-architecture-as-evidence) were
re-verified against current code before use. **All four were unchanged.**

| Finding | Re-verified as | Used by |
| --- | --- | --- |
| Five repository interfaces owned by their consumer-side packages | `AssessmentRepository`, `CurriculumRepository`, `LessonStudyRepository`, `SavedQuestionRepository` in feature packages and `LearningContentRepository` in `curriculum/learning/repository`, all `internal`, with implementations under `data/local/**` and `curriculum/learning/content` | L3.1's Senior section, as evidence that the entry-point rule holds here |
| No DAO is used outside `data/local` | Re-verified by search across `shared/src/commonMain/kotlin`: no `…Dao()` call appears above `data/local` | L3.1, as the reason the entry point is a fact rather than an intention |
| **Every repository method is a one-shot `suspend` function; no repository returns a `Flow`** | Re-verified across all five interfaces and their implementations: no `Flow` return type appears in any repository | **L3.4's counterexample**, the Unit's most instructive local finding |
| `LocalLessonStudyRepository` maps `StudiedLessonEntity` to `StudiedLesson`, and no Room type is visible above `data/local` | Unchanged; the mapping is a two-field copy inside `getStudiedLessons()` | **L3.5's model-boundary evidence** |
| `StudyProgressStateHolder`'s source-of-truth KDoc | Unchanged, including "[repository] remains the source of truth. Nothing is stored here that the database does not already hold: this is the in-memory projection the UI observes, read back from the repository after every mutation rather than assembled independently" | L3.3's Senior section and L3.4's counterexample |
| No network layer | Re-verified: `gradle/libs.versions.toml` and `shared/build.gradle.kts` declare no Ktor, Retrofit or OkHttp dependency | Stated as a limit in L3.1, L3.2 and L3.3 |

**What the evidence is used for, and what it is explicitly not used for.** L3.1 says the
application demonstrates the boundary and not the coordination. L3.3 says that with no
network layer no fact here has two candidate authorities, so the codebase shows the
vocabulary being used precisely and is "no evidence whatsoever" for the conflict reasoning.
L3.5 adds the counterpart limit: `LearningContentRepository` returns the learning document's
own types directly, so the entity-to-domain mapping is **not** presented as something every
repository here needs. **No production architecture code was changed or proposed for
change**, and nothing in authoring surfaced a product defect worth recording.

### What each Lesson actually does, against the issue's requirements

**L3.1 — repository responsibility.** The responsibility model is a six-item list framed as
a menu of decisions rather than a checklist, introduced by the ownership question — what
decision does this component own that its callers should not own themselves — and followed
immediately by the guidance's own five. The repository-versus-data-source comparison is a
six-row table on one feature (`LoanRepository` over `LocalLoanSource` and `RemoteLoanSource`)
whose rows are decisions, and the Lesson states that the distinction does not depend on a
class-name suffix. The **no-repository case** is `ReminderPreferenceRepository`, a
two-function pass-through over one trivial source, answered with "none yet" and costed. The
**requirement change that earns it** is a four-item list: a second copy on the service, a
readable-while-slow requirement, a second consumer needing one answer, and a stored
representation that stops matching the application's. The **interface misconception** is
corrected with four conditions that would justify one, and the Lesson says in as many words
that *which side defines the abstraction* belongs to the next Unit.

**L3.2 — coordination.** Four decisions are named as the repository's; the read path is six
steps under three stated requirements, followed by a paragraph that holds the diagram to its
own requirement — a read-triggered refresh satisfies a freshness bound measured from opening
and not one that holds while the screen stays open, so the second trigger is named as a
decision rather than left implicit. The write path is a four-column table comparing
local-first against remote-confirmed on requirement, path, what the reader sees and what a
failure means, and the remote-confirmed row is followed by the outcome the neat version of
that design omits: the service may commit while its answer is lost, so the operation has
three outcomes and "unknown" is one of them. The Lesson draws the consequence as a
requirement rather than a mechanism — the unknown outcome is a state the screen can be in,
and resolving it means the operation has to be safe to repeat. Origin-hiding is taught with its counterweight: four things a consumer
legitimately needs — staleness, refresh in progress, last refresh failed, unsent local
changes — separated from implementation origin by the **application meaning / implementation
origin** distinction, and expressed in a `LoansSnapshot` type that names no transport, entity
or exception. Freshness is Senior depth: it cannot be inferred from the data, so a cache that
recorded nothing cannot be given a staleness policy later.

**L3.3 — authority.** The correction is stated first and the definition is per fact, about
disagreement, and about an owner rather than a technology. The three-answer comparison is a
four-column table: a local store authoritative for an offline draft, the library service
authoritative for a fine, and an in-memory session holder authoritative for a sort order —
with the Lesson saying that the third breaks the reflex because source of truth says nothing
about how long a value lives. The conflict is concrete (local 80 against service 100), the
"which is newer" question is rejected unless the requirement says so, and four resolution
rules are listed without one being made a default. The two-screen failure is diagnosed as
*no authoritative owner* rather than as duplication. The offline-first recommendation is
quoted with its condition and its reason.

**L3.4 — API shape.** Both guidance sentences are quoted, and the decisive word is named as
*notified*. The same-data comparison is `observeLoans(): Flow<List<Loan>>` against
`suspend fun quoteRenewal(id: LoanId): RenewalQuote`, with the one-shot consequence stated as
a contract rather than a defect. The consumer cost of a stream is three obligations —
collection lifetime, later values, a policy for values it must ignore — and the mechanics are
handed to E24 by link. Mutable stream exposure is refused explicitly as the data-boundary
form of Unit 2's second-write-path problem. **Both shapes on one repository** is Senior depth
with three conditions that keep it one truth. The **one-shot counterexample** is this
application, with a two-row buys/costs table naming the obligation it moves onto writers.

**L3.5 — boundaries.** Both slogans are rejected in the first paragraph and replaced with the
independent-change question. The split is shown as three real types whose differences are
constraints rather than spelling, with a four-row buys/costs table. The **leakage example** is
constructed so the coupling is genuinely present — the shared type carries the service's
ISO-8601 string and the *screen* parses it — and the Lesson explicitly warns against
overstating it, noting that a serialization library can map a renamed field without the change
reaching upward. The **small-feature counterexample** is the reminder-time preference. The
**error boundary** contrasts catching a transport library's exception with an
application-owned failure contract, lists three mechanisms, and states that the boundary claim
is narrower and stronger than any of them. The separation from Unit 2's L2.3 is stated in the
Senior section: L2.3 asks how a screen represents an error it must render, L3.5 asks what
representation reaches the state holder in the first place.

### Corrections made during review

Three defects were found by review after the Lessons were first written, all in Unit 3's own
prose and all fixed in this change. They are recorded because two of them are the kind of
overstatement this subject is especially prone to — a design described by the outcomes its
author planned for rather than by the outcomes it has.

| Where | Defect | Correction |
| --- | --- | --- |
| L3.1, Senior | The paragraph claimed all five repository implementations sit "in a separate tree under `data/local`". **False**: four do, and `BundledLearningContentRepository` sits under `curriculum/learning/content` because the document it serves is a bundled file rather than a table. [Part 7](#part-7--this-repositorys-own-architecture-as-evidence) records this correctly and the Lesson did not | The exception is now described, and used: the implementations are grouped by the source they work with, and no consumer is affected by which one that is |
| L3.2, Practical | The read-path diagram showed a single read-triggered refresh while the stated requirement was a freshness bound holding "for as long as the reader is looking". A list left open drifts indefinitely under that path, so the Lesson's own "when a refresh is attempted, and what triggers it" decision was answered only half way | The requirement now says explicitly that the bound holds while the list is open, and a paragraph names the missing trigger — a refresh while the screen is visible, or the service announcing a change — while handing the mechanism to other curricula |
| L3.2, Practical | The remote-confirmed write row said a failure means "the write did not happen" and that "the two copies never diverged". **Too strong**: if the service commits and the response is lost, the write happened and the local copy is stale, and no ordering of two sources across a network makes them change atomically | The row now names three outcomes — accepted, refused, unknown — and two paragraphs draw the consequence as requirements: the unknown outcome is a state the screen can be in, and repeating the confirmation has to be safe. The summary claim is narrowed from "refuses divergence" to "never diverges on purpose" |

None of the three changed an identity, a mapping, a source or a Lesson boundary.

### Semantic review of the Questions this Unit now reaches

All six ACTIVE Questions in the resolved pool were re-read in full and independently solved
against the finished prose. **The E26-01 findings in
[Part 5](#part-5--semantic-assessment-review) all still hold**; nothing below overturns one.
The `architecture` Topic still holds 22 ACTIVE and 4 DEPRECATED Questions over 18 Subtopics,
re-counted from the bundled JSON during authoring.

| Question | Level | Reached through | Re-read verdict against the shipped Lessons |
| --- | --- | --- | --- |
| `repository_observable_api_shape` | APPLIED | `repository_pattern` (L3.4) | **The Unit's best-matched Question, and it survives the prose test.** L3.4 teaches exactly the reasoning it turns on — a requirement to reflect writes the screen did not make needs the repository to be able to push, and a one-shot read is stale the instant it returns — using a different feature, different wording and none of its distractors. The Lesson's one-shot half is taught as a contract rather than as a wrong answer, which is what keeps it from coaching the option |
| `single_source_of_truth_001` | FOUNDATION | `single_source_of_truth` (L3.3) | Sound and answerable from L3.3's Core. It remains definitional — it establishes that one owner is authoritative and consumers read from there — and never makes the reader choose an owner for a stated requirement, which is GAP-U3-C unchanged. Its distractor rejecting last-writer-wins as a definition is consistent with L3.3's treatment of resolution rules as requirements |
| `architecture_paging_ownership` | APPLIED | `layered_architecture` (L3.5) | Sound, and a much better fit here than in Unit 1, exactly as E26-01 predicted. Its argument — deciding when a page is fetched, caching it and recording where to resume are data-layer responsibilities — is L3.1's responsibility list and L3.2's coordination reasoning, and a reader who has finished this Unit has the data-layer vocabulary the Question uses. Paging's own APIs are still taught nowhere and do not need to be |
| `dto_entity_domain_model_boundary` | APPLIED | `layered_architecture` (L3.5) | Sound, and the closest existing match to L3.5's contract. Its correct answer is the Lesson's central trade-off, including the mapping cost, and its explanation's "may be unnecessary for very small features" is the Lesson's small-feature counterexample. L3.5 teaches both halves without reproducing its wording |
| `architecture_error_mapping_boundary` | APPLIED | `error_modeling` (L3.5) | **Confirmed to assess the responsibility L3.5 actually teaches.** The Question is about a state holder catching a transport library's exception types, which is the Lesson's error-boundary example in the same shape; its explanation's conclusion — that translating at the repository boundary keeps the client choice inside the data layer — is the Lesson's claim. Importantly, its correct answer is the *boundary* one and not a Result-versus-exception one, so it does not push L3.5 toward prescribing a mechanism |
| `architecture_error_modeling_result_type` | APPLIED | `error_modeling` (L3.5) | Sound, and the one to watch. It asks why a sealed result rather than throwing, and its correct answer — the failure becomes part of the signature the caller must handle — is a property L3.5 states while listing the sealed result as one of three defensible shapes. **L3.5 was deliberately not written to make this Question's option the lesson's conclusion**: its explanation is itself careful ("Exceptions stay reasonable for genuinely exceptional conditions"), so the Question and the Lesson agree that this is a trade-off. No defect, and no change needed |

**Adjacent Questions read as duplication guards, none edited or re-mapped:**
`flow_one_shot_result_vs_observable_stream` (`async_reactive` / `flow_fundamentals`),
`offline_first_001` and `offline_first_local_write_then_sync` (`local_data` / `offline_first`),
`cache_invalidation_staleness_policy` (`local_data` / `cache_invalidation`), and `room_dao_001`
(`local_data` / `room_dao`). All five reach this Unit only as supporting context and therefore
create no practice here, which is asserted rather than inspected. `offline_first_001` in
particular states the recommendation L3.3 had to qualify, and the Lesson's wording is
compatible with it: the Question's own explanation says the remote source "is still
authoritative for data the device did not create".

**The two relevant DEPRECATED repository Questions were re-read and not edited.**
`repository_pattern_001` occupied the repository's role — coordinating access to one or more
data sources behind a focused API, keeping persistence types behind the boundary — and
`repository_vs_data_source_responsibility` occupied the repository-against-data-source split
on a two-source feature. **Both concepts are now taught in full by L3.1, and neither retired
wording is reproduced.** L3.1's treatment is materially wider than either: it derives the
responsibility from an ownership question, treats the list as a menu rather than a
definition, and adds the case where the type is not worth adding, which neither Question ever
posed. Their existence is why GAP-U3-A is the widest gap in the Unit, and E26-08 owns the
response.

**No Question was created, edited, re-mapped, re-levelled or re-statused by this issue**, and
no factual defect was found in any Question read.

### GAP-U3-A to GAP-U3-C after authoring

All three gaps survive the finished prose unchanged. Authoring proved no gap definition
wrong, and E26-08 still owns all three.

| Gap | Status after E26-04 | What the finished Lesson changes about it |
| --- | --- | --- |
| GAP-U3-A | **Open, unchanged; still the widest gap in the Unit** | L3.1 now ships the whole contract — the responsibility model, the repository-against-data-source comparison on one feature, the no-repository case and the requirement change that earns the type. The ACTIVE bank still holds exactly one `repository_pattern` Question and it is about API *shape*, so the responsibility itself remains unassessed. The gap is now a gap in assessment of material that ships, which is the strongest case an E26-08 Question can have. **Neither retired wording may be re-asked** |
| GAP-U3-B | **Open, unchanged** | L3.2 ships the read path, both write paths, and the origin/freshness/failure contract as the repository's own decision. The persistence Topic still assesses staleness policy and offline-first from the storage side, and nothing poses the *contract* question from above. E26-08 should note that the Lesson's sharpest testable idea is the application-meaning-against-implementation-origin distinction, not the read path |
| GAP-U3-C | **Open, unchanged; still a strong candidate** | L3.3 ships the decision the gap describes, including a correct answer that is not a database — the session-scoped in-memory owner — and the conflicting-copy scenario with a stated rule. `single_source_of_truth_001` is still definitional and `offline_first_001` still states the recommendation from the persistence side, so nothing makes the reader choose |

**The L3.4 duplication guard, recorded for E26-08 rather than raised as a gap.**
`flow_one_shot_result_vs_observable_stream` (`async_reactive` / `flow_fundamentals`, APPLIED)
already assesses very nearly L3.4's decision from the stream side, with a repository example
and a deliberately frozen quote. L3.4 still had to teach the architecture decision, because
Unit 3 needs it and because the Question is unreachable from any E26 primary mapping — which
is asserted in the test suite. **This is not automatically an E26 assessment gap, and no new
GAP id was created for it.** E26-08 must decide deliberately whether an architecture-side
Question adds reasoning the Flow-side one does not: the candidate distinction is that the
Flow Question decides an API shape from a consumer requirement, while the unassessed
architecture reasoning is what a stream commits *every* caller to and why exposing a mutable
stream from the data layer recreates a second write path.

**L3.5's existing coverage is confirmed as meaningful and no new gap was invented.**
`dto_entity_domain_model_boundary`, `architecture_error_mapping_boundary` and
`architecture_error_modeling_result_type` between them assess the mapping trade-off, the
leakage consequence and one error representation, which is more than any other Lesson in the
Unit has. One observation is recorded for E26-08 without being promoted to a gap: **nothing
assesses the choice between separate and shared models in the direction that favours one
type** — every existing Question argues the split — so a reader could pass all three while
still believing the triple is mandatory. That is the same shape of observation E26-02 recorded
about L1.3, and a single Question may well cover it alongside GAP-U3-A.

### Actual practice reach, resolved through the production resolver

Recomputed by running the shipped Unit through `PracticeBuilderViewModel` and the real
selection path in `LearningUnitPracticeIntegrationTest`, not by reading mappings. The Unit
configures `AssessmentScope.Subtopics` of exactly its four primary concepts and resolves
**six** Questions:

| Resolved Question | Level | Reached through | Semantically belongs mainly to |
| --- | --- | --- | --- |
| `repository_observable_api_shape` | APPLIED | `repository_pattern` | Unit 3, and it is the Unit's best-matched Question |
| `single_source_of_truth_001` | FOUNDATION | `single_source_of_truth` | Unit 3 |
| `architecture_error_mapping_boundary` | APPLIED | `error_modeling` | Unit 3 |
| `architecture_error_modeling_result_type` | APPLIED | `error_modeling` | Unit 3 |
| `architecture_paging_ownership` | APPLIED | `layered_architecture` | Unit 3, **shared with Unit 1** |
| `dto_entity_domain_model_boundary` | APPLIED | `layered_architecture` | Unit 3, **shared with Unit 1** |

This matches [Part 6](#part-6--unit-practice-routing-modelled-now)'s modelled pool exactly —
six Questions, the same six ids — so no mapping moved during authoring, and E26-01's
prediction is confirmed rather than assumed. The level split is one FOUNDATION and five
APPLIED, with **no ADVANCED Question**, which matches the Topic-wide observation in Part 5.

**The Unit 1 / Unit 3 overlap is now a fact rather than a prediction.**
`layered_architecture` is primary in Unit 1's closing Lesson and in Unit 3's, so
`architecture_paging_ownership` and `dto_entity_domain_model_boundary` appear in both Units'
practice — asserted directly by computing both pools and intersecting them. Both fit Unit 3
better: one is a data-layer responsibility question in data-layer vocabulary, the other is
L3.5's central trade-off. It was **not** fixed by demoting `layered_architecture` from L3.5 or
from Unit 1, by re-mapping either Question, or by inventing a taxonomy concept, all of which
the issue forbids and the plan already rejected. E26-08 owns the decision; the test asserts
the intersection so a later re-map has to re-state the consequence rather than silently
repairing it.

Two further structural facts, both asserted rather than inspected:

1. **Supporting concepts broaden nothing.** The Unit's twelve supporting-only concepts —
   including `offline_first`, `cache_invalidation`, `caching`, `room_dao`, `retrofit`,
   `flow_fundamentals`, `flow_collection`, `stateflow`, `kotlin_sealed_types`,
   `state_ownership` and `separation_of_concerns`, between them holding twenty-one ACTIVE
   Questions — contribute nothing to the pool. `flow_one_shot_result_vs_observable_stream` is
   asserted by id to stay outside it.
2. **No shipped Unit's practice changed.** No shipped Lesson takes an `architecture` Subtopic
   as primary, so nothing this Unit maps can reach an existing Unit's pool. Unit 2's pool is
   unchanged at five and shares no Question with Unit 3, because their primary concepts are
   disjoint; Unit 1's is unchanged at five and shares the two Questions above.

### Cross-links

Backward only, and every target already shipped. L3.1 → `lesson_when_an_interface_is_a_boundary`,
`lesson_layers_and_their_cost`, `lesson_state_holder_responsibility`; L3.2 → L3.1,
`lesson_layers_and_their_cost`; L3.3 → L3.2, `lesson_state_holder_responsibility`; L3.4 →
`lesson_why_flow`, `lesson_state_flow`, `lesson_choosing_a_stream_abstraction`, L3.3; L3.5 →
`lesson_layers_and_their_cost`, `lesson_modelling_ui_state`, L3.1.

This is the plan's intended E24 graph for L3.4 exactly, plus one within-Unit backward link per
Lesson and the Unit 1/Unit 2 anchors each Lesson actually applies — which follows the Unit 1
and Unit 2 precedent. **No shipped Lesson was edited**, no Unit 1 or Unit 2 Lesson received a
reciprocal link, and no forward link was invented.

One test had to be corrected rather than extended. `stateHolderUnitLinksBackwardsOnlyToShippedComposeAndCoroutineAnchors`
asserted that *no other Lesson in the document* names a Unit 2 Lesson, which was true when
Unit 2 was the last Unit and is not the property the plan actually requires. It was narrowed
to Lessons authored **before** Unit 2, which is what "no shipped Lesson was edited to receive
a reciprocal link" means; a later Unit linking backward into Unit 2 is the intended graph, and
Unit 3 does it twice. The new Unit 3 link test uses the same narrowed form.

### Tests changed, and why

| File | Change | Why production data made it necessary |
| --- | --- | --- |
| `BundledLearningCurriculumTest` | Unit id, title and home-Topic lists extended by one; Lesson id/title order and primary mappings for the new Unit added; the Unit 2 link test narrowed from "no other Unit" to "no earlier Unit"; two new tests — `dataOwnershipUnitKeepsItsPlannedBridgesOutOfPrimaryPractice` and `dataOwnershipUnitLinksBackwardsOnlyToShippedArchitectureAndFlowAnchors` | The document lists Units positionally, and the twelve bridges are where a promotion to primary would silently claim another curriculum's practice — `offline_first` and `room_dao` most of all, since this Unit's whole boundary with the persistence curriculum rests on them staying supporting. The bridge test also pins the two concepts that are primary in one Lesson and supporting in another, which the validator cannot catch across Lessons |
| `LearningUnitPracticeIntegrationTest` | The traversal now expects three architecture Units and `listOf(5, 5, 5)` Lessons, and its final studied-record count rises from 81 to 86; the shared expectation table gained a Unit 3 row; new test `theDataOwnershipUnitPractisesItsPrimaryConceptsIncludingTwoSharedWithUnitOne` | Continue Learning walks the whole document, so a third architecture Unit changes the traversal and the Topic's progress denominator. Unit 3 fits the shared table — all four primary concepts hold Questions — so the bespoke test exists to pin the exact six ids, to compute the Unit 1 intersection rather than assert it from memory, and to assert that the E24 Flow Question stays outside the pool |

No test was added that only re-states schema validation `LearningCurriculumValidatorTest`
already performs, and the data-driven suites — the reader journey over every shipped Unit,
Topic Detail's Unit rows, the end-to-end repository path — needed no edit because they read
the document rather than listing it.

### Validation performed

| Command | Result |
| --- | --- |
| `python3` structural pre-check over both bundled JSON documents | Ids unique, every mapping an ACTIVE Subtopic, no primary/supporting overlap, every `relatedLessonIds` target resolvable, no blank or placeholder text, every comparison row matching its header count, every Source URL well-formed |
| `./gradlew :shared:jvmTest --tests "*BundledLearningCurriculumTest*" --tests "*LearningUnitPracticeIntegrationTest*" --tests "*LearningCurriculumValidatorTest*" --tests "*LearningContentEndToEndTest*"` | 98 tests, 0 failures (the first run surfaced one real defect: the Unit 2 forward-link assertion described above) |
| `./gradlew :shared:jvmTest` | **1,393 tests, 0 failures**, including `LearningProductionContentJourneyTest`, which renders every authored block of the new Unit in the reader, checks the reading column never widens, and opens every authored Source link through the app's own URI boundary |
| `python3 tools/learning_question_coverage.py --write` then `--check` | Snapshot regenerated and reported current |
| `cd tools && python3 -m unittest test_learning_question_coverage.py` | 21 tests, OK |
| `./gradlew :shared:check` | Passed |
| `./gradlew :androidApp:assembleDebug` | Passed |
| `git status --short` and `git diff --check` | Five files changed — the bundled learning document, two jvm test files, this plan and the generated coverage snapshot — with no build or cache output and no whitespace defects |

The regenerated `docs/content/learning-question-coverage.md` now reports **21 active Units and
87 active Lessons**, the new Unit appearing directly after Unit 2 with five Lessons, a pool of
six Questions across two levels and no ADVANCED Question, and one primary Subtopic still with
no active Question — which remains `architecture_tradeoffs` and GAP-U1-E rather than anything
Unit 3 introduced.

### Not validated

- **`iosArm64` is not compiled locally or on CI**, unchanged from E25, E26-01, E26-02 and
  E26-03. Unit 3 makes no multiplatform claim at all, so nothing in its prose depends on that
  target; the limitation is reported because it is still true of the epic.
- **No CI run is claimed.** Nothing in this issue was observed on GitHub Actions.
- **Backlog validation could not be run**: `PyYAML` is unavailable in this environment, so
  `.github/project/backlog.yml` was read as text rather than parsed and validated. Issue #364
  was read from that file rather than through `gh`, which is still not installed.
- **The learning content itself is editorial** and no automated check can confirm that a
  Lesson teaches what it claims; the semantic review above is a judgement, as Rule 10 of the
  authoring contract requires.

---

## Authoring outcomes for Unit 4

Added by E26-05 after the five Lessons were written. Every source quotation, repository
finding and routing figure below was re-opened, re-verified against current code, or
executed during authoring rather than carried over from E26-01's tables; where a finding is
unchanged, that is stated as a re-verification and not as a copy.

### What did not change

Every proposed Unit id, Lesson id, title, authored order and primary/supporting mapping for
Unit 4 in the [identity tables](#identity-conventions-and-proposed-identities) shipped
verbatim. **No Lesson boundary moved, none was split or merged, and neither planning
document needed a correction.** The blueprint's `L4.1`–`L4.5` ordering, its
Teach/Bridge/Reference/Exclude decisions and its misconception targets were followed as
written, and nothing in Unit 4 required a Unit 1, Unit 2 or Unit 3 identity, mapping or
boundary to move.

| Shipped identity | Title | Primary | Supporting |
| --- | --- | --- | --- |
| `lesson_when_a_domain_layer_earns_its_place` | When Does Another Layer Earn Its Existence? | `use_cases` | `layered_architecture`, `architecture_tradeoffs`, `separation_of_concerns` |
| `lesson_use_cases_and_pass_through_cost` | Use Cases That Earn Their Place, and Pass-Through Cost | `use_cases` | `repository_pattern`, `architecture_tradeoffs`, `state_ownership` |
| `lesson_policy_and_framework_detail` | Policy, Framework and Detail | `dependency_direction` | `clean_architecture`, `layered_architecture`, `kmp_architecture` |
| `lesson_dependency_inversion_in_practice` | Who Defines the Abstraction? | `dependency_direction`, `interface_boundaries` | `solid`, `clean_architecture`, `repository_pattern`, `service_locator_vs_di` |
| `lesson_clean_architecture_intent` | Clean Architecture: the Dependency Rule, Not the Diagram | `clean_architecture` | `layered_architecture`, `use_cases`, `dependency_direction`, `android_modules` |

The Unit is `unit_domain_logic_and_dependency_direction`, titled **Domain Logic, Use Cases
and Dependency Direction**, homed in `architecture`, and appended **directly after
`unit_repositories_and_data_ownership`** — position 22 of 22, and fourth within the
architecture sequence. No existing Unit moved and no shipped Lesson was edited. The
architecture sequence is now 5/5/5/5 Lessons across four Units, and the whole document is
**22 active Units and 92 active Lessons**, both derived from production rather than
assumed.

All five Lessons carry Core, Practical and Senior depth. `lesson_when_a_domain_layer_earns_its_place`
is the longest Lesson in the document by roughly four per cent, which was accepted rather
than trimmed further: it is the one Lesson required to carry a definition, four earning
conditions, three rejected claims, three worked features, the actionability argument and
both the module and DDD boundaries, and Rule 8 of the authoring contract prefers a coherent
Lesson to a mechanical limit. The other four sit inside the range the previously shipped
Lessons occupy.

**Configured versions were re-read** from `gradle/libs.versions.toml` during authoring and
are unchanged from E26-04: Kotlin 2.4.10, Compose Multiplatform 1.11.1, `androidx-lifecycle`
2.11.0-beta01, kotlinx.coroutines 1.11.0, Koin 4.2.2, Room 3.0.1. **No
[Part 8](#part-8--kotlin-multiplatform-viewmodel-and-lifecycle-findings) contract is used by
this Unit** — Unit 4 makes no claim about `ViewModel`, `viewModelScope`, clearing or
host-supplied owners, and the one `ViewModel` that appears in its prose is a misplaced-policy
example whose point is where a rule lives rather than what the class guarantees — so nothing
in Part 8 needed re-verification here, and E26-06 inherits that obligation unchanged.

### Unit purpose, and how it stays separate from Units 1 and 3

Unit 3 gave the learner a repository and a data boundary. Unit 4 asks whether a feature
needs another architectural boundary between its screen or state owner and its repositories,
and its central claim is that **a domain layer is optional**: it earns its existence only
when there is enough independent responsibility to put there.

- **The Unit does not teach `UI → Domain → Data` as a mandatory Android template.** The
  three-box diagram appears exactly once, in L4.5, and is labelled there as one possible
  application structure compatible with some Clean Architecture principles rather than as
  Clean Architecture itself.
- **The five Lessons form one argument in the plan's order**: does another layer earn its
  cost, does this individual operation earn a use-case type, what is policy and what is
  detail, who should define the abstraction, and only last what Clean Architecture actually
  requires. Opening on Clean Architecture or on a layer diagram would hand the reader the
  conclusion before the decisions it is the conclusion of, which is why the order is
  asserted in `BundledLearningCurriculumTest` with the reason recorded beside it.
- **Unit 1 and Unit 3 prerequisites are applied rather than re-taught.** L4.1 uses the layer
  cost model from `lesson_layers_and_their_cost` without re-deriving it; L4.4 opens by
  naming the three places the question was deferred — L1.3 taught direction and named
  inversion, L1.4 asked whether an abstraction earns existence and left ownership open, and
  L3.1 deferred "which side should define that abstraction" to this Unit in as many words.
- **No pattern name appears anywhere in the Unit.** MVP, MVVM and MVI are named only in
  L4.5's closing sentence as the next Unit's subject, so Unit 5 still has its material.

### Editorial decisions worth recording

1. **The borrowed-items feature carries forward, and the library gains a renewal
   operation.** Units 1–3 used loans, a due date, an overdue rule, a renewal write and the
   reminder-time preference; Unit 4 keeps all of them and adds `RenewLoan`,
   `RenewalEligibility`, `RenewalOutcome` and `BorrowerRepository`. The
   practice-configuration screen stays reserved for Unit 5.
2. **The simple feature is one the reader has already seen judged.** L4.1's no-layer case is
   the reminder-time preference, which L3.1 already answered "none yet" about at the
   repository level. Re-using it makes the layer-level question visibly a different question
   about the same feature rather than a new example.
3. **Cost is always stated as things.** Following the Unit 1 and Unit 3 precedent, the
   forwarding layer's cost is two types, two files, two names, one navigation hop, a third
   place a change lands and a directory that claims a responsibility the feature does not
   have; the pass-through type's cost is a type, a file, a hop, an abstraction to explain,
   and a precedent priced per repository method.
4. **Every source recommendation is quoted with its condition attached.** The domain layer is
   quoted as "an _optional_ layer" together with "You should only use it when needed"; the
   Android recommendation is quoted with its own "Recommended in big apps" strength and the
   page's "recommendations and not strict requirements" note.
5. **Forward material is prose that names a Unit, never a link.** L4.5 names the pattern Unit
   without naming a Lesson id, because no Unit 5 Lesson exists.
6. **No DI mechanism is named anywhere.** Constructor parameters appear because dependencies
   have to be visible; no container, module, binding, scope, qualifier or graph is described,
   and the one place wiring is discussed is L4.4's explicit separation of injection from
   inversion.
7. **No test code appears in the Unit**, and every testability claim names a dependency
   boundary rather than a layer count.

### Sources used, and the claims they settle

Seven distinct pages across the five Lessons, each opened during authoring and each attached
to the specific claim it supports. The three source families stay separate: the primary
Clean Architecture material settles the dependency rule, Android's guidance settles
Android-specific application-layer recommendations, and neither is used as evidence for the
other's claim.

| Source | Used by | Claim it settles |
| --- | --- | --- |
| [Domain layer](https://developer.android.com/topic/architecture/domain-layer) | L4.1, L4.2, L4.3 | That the domain layer "is an _optional_ layer that sits between the UI layer and the data layer"; that "You should only use it when needed—for example, to handle complexity or favor reusability"; that use cases "don't have their own lifecycle. Instead, they're scoped to the class that uses them"; the `operator fun invoke()` idiom offered as an option rather than a definition; and the data-layer-access-restriction passage in full, including the "**potentially significant disadvantage** … it forces you to add use cases even when they are just simple function calls to the data layer, which can add complexity for little benefit" and the page's own proportional conclusion, "A good approach is to add use cases only when required. If you find that your UI layer is accessing data through use cases almost exclusively, it may make sense to _only_ access data this way." |
| [Architecture recommendations](https://developer.android.com/topic/architecture/recommendations) | L4.1, L4.2, L4.5 | "Use a domain layer", marked **Recommended in big apps**, with its stated condition — "if you need to reuse business logic that interacts with the data layer across multiple ViewModels, or you want to simplify the business logic complexity of a particular ViewModel" — and the page's own "Treat the recommendations in the document as recommendations and not strict requirements." |
| [The Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html) | L4.3, L4.4, L4.5 | The Dependency Rule — "source code dependencies can only point inwards" — together with the statement that nothing declared in an outer circle may be named by code in an inner one; that the circles are schematic and there is no rule requiring exactly the four shown; the entities/use-cases/interface-adapters/frameworks naming; that the data crossing boundaries is "simple data structures" rather than entity objects or database rows; and that testability follows from the arrangement rather than justifying it. |
| [Guide to app architecture](https://developer.android.com/topic/architecture) | L4.5 | That Android recommends designing each application "with at least two layers", UI and data, and describes the domain layer as "an optional layer between the UI and data layers" to be used "only when needed". |
| [Separated Interface](https://martinfowler.com/eaaCatalog/separatedInterface.html) | L4.4 | That the pattern "Defines an interface in a separate package from its implementation", so "a client that needs the dependency to the interface can be completely unaware of the implementation" — the primary statement behind L4.4's placement argument. |
| [Inversion of Control Containers and the Dependency Injection pattern](https://martinfowler.com/articles/injection.html) | L4.4 | That injection is about assembly — "a separate object, an assembler, that populates a field in the lister class with an appropriate implementation" — and the sentence that locates the two decisions relative to each other: "The important issue in all of this is to ensure that the configuration of services is separated from their use. Indeed this is a fundamental design principle that sits with the separation of interfaces from implementation." |
| [Share code on platforms](https://kotlinlang.org/docs/multiplatform/multiplatform-share-on-platforms.html) | L4.3 | That the common source set is for "sharing the common business logic that applies to all platforms" — the single bounded sentence behind L4.3's KMP bridge. |

Three source-sensitive decisions follow, and all three are acceptance-critical:

- **The Android domain-layer wording is unchanged from E26-01's review.** The optionality
  sentence, the reuse-and-complexity condition, the naming convention, the no-own-lifecycle
  statement and the pass-through warning were all re-read on the current page and all still
  read as E26-01 recorded them. No plan outcome needed updating.
- **The primary Clean Architecture source is used only for the rule, and Android's guidance
  only for Android's recommendation.** L4.5 never cites the three-layer recommendation as
  evidence about the dependency rule, and never cites the diagram as evidence that Android
  requires a domain layer. The two are placed side by side and explicitly distinguished.
- **The uniformity convention is taught from the guidance's own two-sided passage.** L4.2
  quotes the advantage, the disadvantage and the proportional conclusion together, which is
  what licenses the Lesson to treat "all access crosses use cases" as a legitimate team
  decision without letting it become an architectural claim.

### Repository evidence used, and its limits

Every Part 7 finding this Unit relies on was re-verified against current code before use.
**All were unchanged.**

| Finding | Re-verified as | Used by |
| --- | --- | --- |
| **No class named `UseCase` or `Interactor` anywhere** | Re-verified by searching every `.kt` file in `shared/`, `androidApp/`, `desktopApp/` and `webApp/` for both names: **zero matches**, and no base class or `invoke` idiom standing in for one | **L4.2's central local evidence** |
| Services that orchestrate | `MistakeReviewService`, `StudyProgressService`, `LearningProgressService`, `AssessmentRetakeService` all still present. `MistakeReviewService` was read in full: it takes `AssessmentRepository` and `AssessmentReviewLoader`, applies `UnresolvedMistakeDerivation`, and is called from `AppShellViewModel`, `ProgressStateHolder`, `MistakeReviewStateHolder`, `LearningRecommendationResolver` and `LearningProgressService` | L4.2, as an operation whose responsibility would earn a type |
| Pure policies | `ContinueLearningPolicy`, `LearningRecommendationPolicy`, `LearningProgressPolicy`, `RecentPerformancePolicy` all still present. `ContinueLearningPolicy` was read in full, including its documented constraint that it uses no clock, no repository, no attempt history and no persistence; `LearningRecommendationPolicy` has three production callers | L4.2 and **L4.3's policy example** |
| Derivations | `StudyProgressDerivation`, `UnresolvedMistakeDerivation`, `LearningPerformanceDerivation` all still present | L4.2 |
| **No package named `domain`, and no domain layer** | Re-verified by listing every package under `shared/src/commonMain/kotlin`: application behaviour sits in feature packages (`lesson_study`, `mistake_review`, `learning_progress`, `guided_learning`, `assessment/*`) with implementations under `data/local` | L4.1's Senior section |
| Five repository interfaces owned in consumer-side packages, implementations in a separate tree | `AssessmentRepository`, `CurriculumRepository`, `LessonStudyRepository`, `SavedQuestionRepository` in feature packages and `LearningContentRepository` in `curriculum/learning/repository`; implementations under `data/local/**` and `curriculum/learning/content` | **L4.4's inversion evidence** |
| **The source dependency actually points inward** | Traced from imports rather than inferred from folder names. `LocalLessonStudyRepository` imports `lesson_study.StudiedLesson` and `lesson_study.repository.LessonStudyRepository`; `LessonStudyRepository` imports only `lesson_study.StudiedLesson` and names no database type. A search for `import org.artkachenko.kmp_learning_app.data.local` anywhere in `commonMain` **outside** `data/local` returns **nothing** | **L4.4**, as the claim that the detail names the policy and the policy names no detail |
| One Gradle module | All of the above compiles inside `:shared` | L4.1 and L4.4, as the refutation of "a layer needs a module" |

**What the evidence is used for, and what it is not.** L4.2 states that the repository's
services, policies and derivations show the responsibility earning the class and the suffix
recording what it was, and then says outright that this is one legitimate arrangement rather
than a universally preferable one, and that a small local-first application is weak evidence
about what a large one needs. It explicitly declines to relabel those types as "actually use
cases": the comparison is drawn on responsibilities and the Lesson says that nothing here is
named a use case and that calling it one would be reading the Lesson back into the codebase.
L4.3 uses `ContinueLearningPolicy` as a framework-independent rule stated in the
application's own vocabulary, and states the limit in the same paragraph — this application's
policy is about a curriculum rather than a business domain, so the renewal scenario is a
worked design and not something lifted from production. L4.4 says that package placement
alone proves nothing and reports the import trace instead. **No production architecture code
was changed or proposed for change**, and nothing in authoring surfaced a product defect
worth recording.

### What each Lesson actually does, against the issue's requirements

**L4.1 — the layer decision.** The domain layer is defined as optional, between UI and data,
with the Android sentence quoted together with its condition. The four earning conditions —
reuse across callers, orchestration, business policy worth isolating, meaningful complexity —
are introduced as evidence rather than a quota, with the explicit statement that one can be
sufficient and that they often arrive together. Three claims are rejected by name: that every
production application needs a domain layer, that Clean Architecture requires one, and that
the presence of business logic anywhere justifies a domain package. The **simple feature** is
the reminder-time preference, shown as a package listing with a forwarding middle layer, and
answered "architecturally, nothing yet" with a five-item cost list. The **complex feature** is
renewals, with four conditions visible at once and the two alternatives named — the rule in a
state holder leaves the second caller reaching into another screen's owner, and the rule in
the repository gives the data layer a policy about borrowers. A five-row comparison table puts
the two features on the same questions. The **honest middle case** is the borrowed-items
attention ordering: one moderately complex rule, one caller, plausible but unrequested reuse,
answered with four pieces of *observable* evidence that would settle it, and the explicit
refusal of speculative future flexibility as justification. Senior depth makes "add it when
needed" actionable by giving both defensible sentences in full, separates the logical layer
from the Gradle module, names DDD as excluded, and distinguishes the layer decision from
L4.2's type decision.

**L4.2 — the use-case decision.** A use case is defined as one application operation, with the
guidance's no-own-lifecycle statement quoted, and "interactor" named once as an equivalent
term with the variance noted and no architectural distinction invented. Four non-definitions
are listed and refused: the suffix, `operator fun invoke`, a base class, and a folder. The
**pass-through example** is `GetLoansUseCase` forwarding `repository.loans()`, costed as five
concrete things including the precedent priced per repository method and the directory whose
name overstates its contents. The **useful use case** is `RenewLoan` in the same shape —
class, constructor parameters, one function — loading two inputs, applying a rule, deciding
the outcome and writing only on the allowed branch, with a four-row table whose rows are
responsibilities rather than aesthetics. Both symmetrical reuse mistakes are refused: that a
use case needs two callers, and that anything appearing twice should be extracted. The
**team-uniformity nuance** is a two-column table separating the architectural justification
from the team convention on four axes, followed by a paragraph giving the convention its four
genuine benefits before naming its four costs, and the closing claim that the failure mode is
not choosing the convention but describing it as architecture. Senior depth is the repository
evidence and the state-holder boundary.

**L4.3 — policy and detail.** Both terms are defined, and the distinction is stated as being
about reasons to change. Two shortcuts are refused explicitly: policy is not a synonym for
pure function, and plain Kotlin is not evidence that a responsibility exists. The
**framework-leak example** is a return-submission operation whose signature takes
`android.net.Uri` and returns the transport library's `Response`, traced to three named
consequences — a transport change becomes a policy change, a platform type restricts where the
rule can live, and the rule is stated in someone else's vocabulary — and then redrawn with
application-owned inputs and outputs and the adapter outside. The **wrong-owner example** is
renewal eligibility computed inside `BorrowedItemsViewModel`, diagnosed not as duplication but
as the rule's responsibility being broader than the component holding it, with an explicit
refusal of "business logic does not belong in a ViewModel" and a four-row table separating
decisions that genuinely are the screen owner's from policy that is not. The **KMP bridge** is
one bounded paragraph: framework-independent policy is the strongest sharing candidate,
sharing is a consequence of the split rather than a reason for it, and the reverse claim that
everything in a domain layer belongs in common code does not follow. The **testability
treatment** names the dependency boundary and states outright that five layers with framework
imports gain nothing while two layers with a correct boundary may have everything.

**L4.4 — inversion.** The Lesson opens on the three deferrals it is answering. The correction
is stated first: an interface does not invert anything by existing, and the question is who
defines the abstraction. The conceptual direction is drawn once, with placement and runtime
supply both explicitly excluded from it. The **two arrangements** use the same three types —
`RenewLoan`, `LoanRepository`, `LocalLoanRepository` — with real import lines in both, so the
source dependency is visible rather than asserted: in A the policy imports the data package
and the contract speaks of rows and epoch milliseconds, in B the data package imports the
consumer and the contract speaks of loans and due dates. A six-row table traces which side
names the other, who the contract was designed for, what a storage change costs, what a
policy change costs, who can vary without the other, and whether anything was inverted. The
**repository evidence** is the import trace described above. Senior depth holds the
consumer-ownership nuance — placement expresses responsibility and a neutral location can
obscure it, with Separated Interface as the source — the **dependency inversion against
dependency injection** distinction as a four-row table plus the consequence that a codebase
can inject everywhere and have inverted nothing, the single naming of SOLID's D, and the
ordering relative to L1.4.

**L4.5 — the dependency rule.** The rule is stated from the primary source in one sentence,
with the schematic-circles finding immediately after it. Four claims are rejected by name:
presentation-domain-data, a fixed layer count, a use case per repository method, and a module
per layer. The **template comparison** shows the three-box diagram and labels it as one
possible structure compatible with some principles, with Android's own more careful wording
quoted beside it. A **two-layer design** satisfying the rule is traced, and a **four-grouping
design** is traced beside it, with a five-row table showing both compliant. **Terminology is
reconciled** without adopting one vocabulary: the Clean Architecture circle names, Android's
layer names and the interactor/application/infrastructure variants are named as differing,
and the curriculum's answer is to teach policy, detail and direction and to ask an interviewer
what they mean by "domain". Senior depth lists what the rule buys as four instances of one
sentence, lists what it does **not** buy — fewer files, faster builds, fewer mappings, simpler
code, fewer modules, better runtime performance — and notes it often produces more of several
of them, carries the testability treatment forward in the same precise form, separates the
architectural boundary from the build unit, and closes the Unit by restating its five
decisions and naming Unit 5 in prose.

### Corrections made during review

Four defects were found by reading the rendered Unit after the Lessons were first written,
all in Unit 4's own prose and all fixed in this change. The first is recorded because it is
the kind of error this subject is most prone to: a diagram that asserts a structure the
surrounding prose denies.

| Where | Defect | Correction |
| --- | --- | --- |
| L4.5, Practical | The four-grouping diagram was drawn as a **vertical chain** — eligibility, then the operation, then the screen owner, then the implementation — which reads as `LocalLoanRepository → BorrowedItemsViewModel → RenewLoan`. That is a dependency the design does not have, and the paragraph directly beneath it says in as many words that the screen owner and the implementation are both outer and neither names the other | Redrawn as a branch: both outer components point inward at `RenewLoan`, and neither points at the other. The picture now shows what the caption claims, which is the whole point of a Lesson arguing that the drawing is not the rule |
| L4.5, Senior | The "what it does not buy" list said the rule frequently produces more files, more mappings **and more modules**. The third is not supportable and contradicts the Lesson's own separation of the architectural boundary from the build unit | Narrowed to files and mappings, which follow directly from the contracts and translations the rule requires |
| L4.4, Practical | "The column that matters is the last row of the middle block" named a structure the six-row table does not have | Replaced with the row's own name — what a storage change costs |
| L4.4, Senior | "manual construction inverts nothing less well" is a double negative that reverses on a careless read | Restated positively: a dependency constructed by hand in one place is inverted just as fully |

None of the four changed an identity, a mapping, a source or a Lesson boundary.

### Semantic review of the Questions this Unit now reaches

All five ACTIVE Questions in the resolved pool were re-read in full and independently solved
against the finished prose. **The E26-01 findings in
[Part 5](#part-5--semantic-assessment-review) all still hold**; nothing below overturns one.
The `architecture` Topic still holds 22 ACTIVE and 4 DEPRECATED Questions over 18 Subtopics,
re-counted from the bundled JSON during authoring.

| Question | Level | Reached through | Re-read verdict against the shipped Lessons |
| --- | --- | --- | --- |
| `architecture_use_case_reuse` | APPLIED | `use_cases` (L4.1, L4.2) | Sound and answerable. Its correct answer — that extracting multi-repository orchestration gives it one home that can be exercised without constructing either ViewModel — is L4.2's `RenewLoan` argument plus L4.3's testability framing, and its final explanation sentence is exactly L4.2's pass-through claim. The Lesson uses a different feature, different wording and none of its distractors, and deliberately teaches the single-caller case the Question does not pose, so it does not coach the option |
| `domain_layer_passthrough_cost` | APPLIED | `use_cases` (L4.1, L4.2) | **The Unit's best-matched Question, and it survives the prose test.** L4.2 teaches the reasoning directly — a class that forwards one repository method with no reuse and no rule centralises nothing and costs indirection — while pricing it as five specific things rather than as the word the Question's correct option uses. Its three distractors (testing, `Context`, blocking) are all claims L4.2 never makes and one L4.3 explicitly refuses |
| `clean_architecture_dependency_rule_tradeoff` | APPLIED | `clean_architecture` (L4.5) | Sound, and confirmed to assess the *consequences* of following the rule rather than the rule's own shape. Both correct options are taught: framework-free policy exercisable without instrumentation is L4.3's and L4.5's testability paragraph, and mapping appearing wherever an annotated type would otherwise have been reused is the cost L4.5 carries forward from Unit 1 and Unit 3. Its refusal of the build-speed claim is the same refusal L4.5's "what it does not buy" list makes. **It does not assess GAP-U4-B**, which is unchanged |
| `dependency_direction_domain_framework_types` | FOUNDATION | `dependency_direction` (L4.3, L4.4) | Sound and precisely matched to L4.3, whose framework-leak example is the same shape — a platform URI in and a transport type out — with a different operation and different consequences prose. The Question's correct answer is L4.3's first two consequences, and its explanation's refusal of "coupling means untestability" is consistent with L4.3's insistence that the useful property is the dependency boundary. **Level observation unchanged and recorded below.** **Routing:** also Unit 1 practice, as predicted |
| `architecture_interface_boundary_ownership` | APPLIED | `interface_boundaries` (L4.4) | Sound and precisely matched to L4.4. All four of its options are reasoning the Lesson teaches independently: the consumer owns the abstraction, placing it beside the implementation leaves the arrow pointing outward, a neutral module removes coupling while scattering the contract, and the presentation layer's binding choice is a wiring decision rather than an ownership one — which is L4.4's injection-against-inversion distinction. The Lesson uses a package-level trace with real imports rather than the Question's module scenario, and reproduces none of its distractor wording |

**`architecture_solid_dependency_substitution` was re-read as required, and is confirmed to
stay outside Unit 4's practice.** It is sound and well written; `solid` is supporting-only in
L4.4 by the epic's design, so the Question reaches no E26 Unit at all. L4.4 names the D of
SOLID once for recognition and teaches the reasoning under `dependency_direction` and
`interface_boundaries`, which is where an engineer makes the decision. The exclusion is now
asserted by id in `LearningUnitPracticeIntegrationTest` rather than left as a claim.

**The DEPRECATED `architecture_tradeoffs_001` was re-read and deliberately not restored.** Its
concept — when a use-case layer is most defensible — is closest to L4.1's, and its correct
answer ("when it holds business rules or orchestration that would otherwise be duplicated")
is two of L4.1's four conditions. Two observations for E26-08 rather than an action here.
First, it is a *use-case-level* Question filed under `architecture_tradeoffs`, so restoring it
unchanged would put a use-case answer into Unit 1's and Unit 6's practice and not into Unit 4's,
which is the opposite of what GAP-U4-A needs. Second, its two weakest distractors are now
things the shipped Lessons address head-on — reading from two repositories is L4.2's
orchestration condition stated too loosely, and "the team wants every repository call wrapped
for the sake of consistency" is precisely the uniformity convention L4.2 treats as a
legitimate team decision rather than as a wrong answer. Restoring that option as a distractor
would now contradict the Unit. E26-08 should treat its reasoning as input to GAP-U4-A and
author at the layer level rather than re-ask it.

**No Question was created, edited, re-mapped, re-levelled or re-statused by this issue**, and
no factual defect was found in any Question read.

### GAP-U4-A and GAP-U4-B after authoring

Both gaps survive the finished prose unchanged, and no new gap was created.

| Gap | Status after E26-05 | What the finished Lesson changes about it |
| --- | --- | --- |
| GAP-U4-A | **Open, unchanged; still lower priority than GAP-U4-B** | L4.1 now ships the whole layer-level decision: the definition, the four conditions, three worked features including an honest middle case, and the evidence that would settle it. The two ACTIVE `use_cases` Questions still both work at the *class* level and are both well matched to L4.2, so a reader who can answer both has still never been asked whether a feature earns a layer. The distinction is now taught explicitly — L4.1's Senior section states it in as many words — which makes the gap sharper rather than smaller. Its priority is unchanged because a reader who holds L4.2's reasoning is close, and `architecture_tradeoffs_001` must not simply be restored (see above) |
| GAP-U4-B | **Open, unchanged; still a strong candidate** | L4.5 now ships the reasoning directly: the dependency rule as a direction, the schematic-circles finding, a two-layer compliant design beside a four-grouping one, and the four template claims rejected by name. `clean_architecture_dependency_rule_tradeoff` still assesses the consequences of following the rule and still takes the layer structure as given, so "Clean Architecture means three layers" remains unassessed. The gap is now a gap in assessment of material that ships, which is the strongest case an E26-08 Question can have |

**No new gap was created for L4.3 or L4.4**, which is the plan's default and was re-checked
rather than assumed. `dependency_direction_domain_framework_types` assesses L4.3's leak in the
same shape, and `architecture_interface_boundary_ownership` assesses L4.4's ownership claim
precisely; neither Lesson introduces reasoning that no existing Question can reach. Two
observations are recorded for E26-08 without being promoted to gaps:

- **Nothing assesses the team-uniformity nuance.** L4.2's sharpest testable idea is arguably
  not the pass-through cost — which `domain_layer_passthrough_cost` covers — but the
  distinction between an architectural justification and a deliberate team convention, and no
  Question in any Topic poses it. It may well belong inside a GAP-U4-A Question rather than
  as one of its own.
- **Nothing assesses dependency inversion against dependency injection.** The distinction is
  L4.4's Senior half and the reason a codebase can inject everywhere and invert nothing.
  `service_locator_vs_di_001` and `di_hilt_viewmodel_scope` sit in `dependency_injection` and
  assess hidden dependencies and DI-scoped lifetimes, which is not the same claim. This is
  E27's territory as much as E26's, and E26-08 should weigh it with E27 rather than author
  into it unilaterally.

**Level observation for `dependency_direction_domain_framework_types`, recorded and not
acted on.** It remains `FOUNDATION` for reasoning that is a design judgement rather than
recall: the reader has to read a signature, recognise two kinds of framework coupling and
name the consequence, which is what L4.3 spends its Practical section teaching. E26-01
recorded this as a level-review candidate; authoring L4.3 strengthens the observation rather
than changing it, because the reasoning the Lesson had to build for it is Practical-depth
reasoning. **No level was changed by this issue**, as the issue requires.

### Actual practice reach, resolved through the production resolver

Recomputed by running the shipped Unit through `PracticeBuilderViewModel` and the real
selection path in `LearningUnitPracticeIntegrationTest`, not by reading mappings. The Unit
configures `AssessmentScope.Subtopics` of exactly its four primary concepts and resolves
**five** Questions:

| Resolved Question | Level | Reached through | Semantically belongs mainly to |
| --- | --- | --- | --- |
| `architecture_use_case_reuse` | APPLIED | `use_cases` | Unit 4 |
| `domain_layer_passthrough_cost` | APPLIED | `use_cases` | Unit 4, and it is the Unit's best-matched Question |
| `clean_architecture_dependency_rule_tradeoff` | APPLIED | `clean_architecture` | Unit 4 |
| `dependency_direction_domain_framework_types` | FOUNDATION | `dependency_direction` | Unit 4, **shared with Unit 1** |
| `architecture_interface_boundary_ownership` | APPLIED | `interface_boundaries` | Unit 4, **shared with Unit 1** |

This matches [Part 6](#part-6--unit-practice-routing-modelled-now)'s modelled pool exactly —
five Questions, the same five ids — so no mapping moved during authoring and E26-01's
prediction is confirmed rather than assumed. The level split is one FOUNDATION and four
APPLIED, with **no ADVANCED Question**, matching the Topic-wide observation in Part 5.

**The Unit 1 / Unit 4 overlap is now a fact rather than a prediction.** Both Units take
`dependency_direction` and `interface_boundaries` as primary concepts, so
`dependency_direction_domain_framework_types` and `architecture_interface_boundary_ownership`
appear in both pools — asserted directly by computing both pools and intersecting them. Both
fit Unit 4 better, because Unit 4 is where their reasoning is completed: L4.3 teaches the
framework-leak consequence in full and L4.4 teaches abstraction ownership in full, while Unit
1 gives a reader direction and the boundary test and stops deliberately. It was **not** fixed
by demoting a concept from L1.3, L1.4 or from this Unit, by re-mapping either Question, or by
inventing a taxonomy concept, all of which the issue forbids and the plan already rejected.
E26-08 owns the decision; the test asserts the intersection so a later re-map has to re-state
the consequence rather than silently repairing it.

Two further structural facts, both asserted rather than inspected:

1. **Supporting concepts broaden nothing.** The Unit's nine supporting-only concepts —
   `solid`, `service_locator_vs_di`, `android_modules`, `kmp_architecture`,
   `repository_pattern`, `layered_architecture`, `architecture_tradeoffs`, `state_ownership`
   and `separation_of_concerns`, between them holding eleven ACTIVE Questions — contribute
   nothing to the pool. `architecture_solid_dependency_substitution` is asserted by id to
   stay outside it, and asserted to stay outside Unit 1's pool as well, which is the
   epic-level claim that `solid` reaches no E26 Unit at all.
2. **No shipped Unit's practice changed.** No shipped Lesson takes an `architecture` Subtopic
   as primary, so nothing this Unit maps can reach an existing Unit's pool. Unit 2's pool is
   unchanged at five and Unit 3's at six, and neither shares a Question with Unit 4 because
   their primary concepts are disjoint from its; Unit 1's is unchanged at five and shares the
   two Questions above.

### Cross-links

Backward only, and every target already shipped. L4.1 → `lesson_layers_and_their_cost`,
`lesson_what_a_repository_owns`; L4.2 → L4.1, `lesson_what_a_repository_owns`,
`lesson_state_holder_responsibility`; L4.3 → `lesson_dependency_direction_and_boundaries`,
`lesson_state_holder_responsibility`, `lesson_model_and_error_boundaries`; L4.4 →
`lesson_dependency_direction_and_boundaries`, `lesson_when_an_interface_is_a_boundary`,
`lesson_what_a_repository_owns`; L4.5 → `lesson_layers_and_their_cost`, L4.1, L4.3.

Each link is an actual semantic dependency rather than one mention per prior Lesson: L4.1
applies Unit 1's layer cost model to the repository Unit 3 designed, L4.2 and L4.3 both lean
on what a state holder owns because both draw a line against it, L4.4 names the three
deferrals it answers, and L4.5 builds on the layer cost, the layer decision and the
policy/detail split. **No shipped Lesson was edited**, no Unit 1, 2 or 3 Lesson received a
reciprocal link, and no forward link into Unit 5 was invented. The link test uses the same
narrowed form E26-04 introduced — no Lesson authored *before* this Unit links into it — which
is the property the plan actually requires.

### Tests changed, and why

| File | Change | Why production data made it necessary |
| --- | --- | --- |
| `BundledLearningCurriculumTest` | Unit id, title and home-Topic lists extended by one; Lesson id/title order and primary mappings for the new Unit added; two new tests — `domainLogicUnitKeepsItsPlannedBridgesOutOfPrimaryPractice` and `domainLogicUnitLinksBackwardsOnlyToShippedArchitectureAnchors` | The document lists Units positionally, so a fourth architecture Unit changes four lists. The bridge test exists because this Unit's nine supporting-only concepts are where a promotion to primary would silently claim another curriculum's practice — `solid`, `service_locator_vs_di`, `android_modules` and `kmp_architecture` most of all, since the Unit's whole boundary with E27, E29 and E33 rests on them staying supporting. It also pins the two concepts that are primary in one Lesson and supporting in another, which the validator checks only within a Lesson |
| `LearningUnitPracticeIntegrationTest` | The traversal now expects four architecture Units and `listOf(5, 5, 5, 5)` Lessons, and its final studied-record count rises from 86 to 91; the shared expectation table gained a Unit 4 row; new test `theDomainLogicUnitPractisesItsPrimaryConceptsIncludingTwoSharedWithUnitOne` | Continue Learning walks the whole document, so a fourth architecture Unit changes the traversal and the Topic's progress denominator. Unit 4 fits the shared table — all four primary concepts hold Questions — so the bespoke test exists to pin the exact five ids, to compute the Unit 1 intersection rather than assert it from memory, and to assert that `architecture_solid_dependency_substitution` reaches neither Unit |

No test was added that only re-states schema validation `LearningCurriculumValidatorTest`
already performs, and the data-driven suites — the reader journey over every shipped Unit,
Topic Detail's Unit rows, the end-to-end repository path — needed no edit because they read
the document rather than listing it. No prose is snapshotted anywhere.

### Validation performed

| Command | Result |
| --- | --- |
| `python3` structural pre-check over both bundled JSON documents | Ids unique across the whole document, every mapping an ACTIVE Subtopic, no primary/supporting overlap within a Lesson, every `relatedLessonIds` target resolvable and non-self, no blank or placeholder text, every comparison row matching its header count, every Source URL well-formed |
| `./gradlew :shared:jvmTest --tests "*BundledLearningCurriculumTest*" --tests "*LearningUnitPracticeIntegrationTest*" --tests "*LearningCurriculumValidatorTest*" --tests "*LearningContentEndToEndTest*"` | 101 tests, 0 failures. The first run surfaced one real defect: three comparison tables shipped with a blank first header, which `LearningCurriculumValidator` rejects as `BLANK_COMPARISON_HEADER`. All three were given real column labels |
| `./gradlew :shared:jvmTest` | **1,396 tests, 0 failures**, including `LearningProductionContentJourneyTest`, which renders every authored block of the new Unit in the reader, checks the reading column never widens, and opens every authored Source link through the app's own URI boundary |
| `python3 tools/learning_question_coverage.py --write` then `--check` | Snapshot regenerated and reported current |
| `cd tools && python3 -m unittest test_learning_question_coverage.py` | 21 tests, OK |
| `./gradlew :shared:check` | Passed |
| `./gradlew :androidApp:assembleDebug` | Passed |
| `git status --short` and `git diff --check` | Five files changed — the bundled learning document, two jvm test files, this plan and the generated coverage snapshot — with no build or cache output and no whitespace defects |

The regenerated `docs/content/learning-question-coverage.md` now reports **22 active Units and
92 active Lessons**, the new Unit appearing directly after Unit 3 with five Lessons, a pool of
five Questions across two levels and no ADVANCED Question, and one primary Subtopic still with
no active Question — which remains `architecture_tradeoffs` and GAP-U1-E rather than anything
Unit 4 introduced.

### Not validated

- **`iosArm64` is not compiled locally or on CI**, unchanged from E25 and from every E26
  issue so far. Unit 4 introduces no target-specific code and makes no source-sensitive
  ViewModel or lifecycle claim, so nothing in its prose depends on that target; the
  limitation is reported because it is still true of the epic.
- **No CI run is claimed.** Nothing in this issue was observed on GitHub Actions.
- **Backlog validation could not be run**: `PyYAML` is unavailable in this environment, so
  `.github/project/backlog.yml` was read as text rather than parsed and validated. Issue #365
  was read from that file rather than through `gh`, which is still not installed.
- **The learning content itself is editorial** and no automated check can confirm that a
  Lesson teaches what it claims; the semantic review above is a judgement, as Rule 10 of the
  authoring contract requires.

## Authoring outcomes for Unit 5

Added by E26-06 after the five Lessons were written. Every source quotation, repository
finding and routing figure below was re-opened, re-verified against current code, or
executed during authoring rather than carried over from E26-01's tables; where a finding is
unchanged, that is stated as a re-verification and not as a copy.

### What did not change

Every proposed Unit id, Lesson id, title, authored order and primary/supporting mapping for
Unit 5 in the [identity tables](#identity-conventions-and-proposed-identities) shipped
verbatim. **No Lesson boundary moved, none was split or merged, and neither planning
document needed a correction.** The blueprint's `L5.1`–`L5.5` ordering, its
Teach/Bridge/Reference/Exclude decisions and its misconception targets were followed as
written, and nothing in Unit 5 required a Unit 1–4 identity, mapping or boundary to move.

| Shipped identity | Title | Primary | Supporting |
| --- | --- | --- | --- |
| `lesson_one_screen_five_questions` | One Screen, Five Questions | `mvc` | `mvp`, `mvvm`, `mvi`, `state_ownership` |
| `lesson_mvp_view_contract` | MVP: an Explicit View Contract | `mvp` | `mvc`, `interface_boundaries`, `state_ownership` |
| `lesson_mvvm_observed_state` | MVVM: a UI That Observes State | `mvvm` | `state_ownership`, `unidirectional_data_flow`, `stateflow`, `viewmodel_lifecycle` |
| `lesson_mvi_intent_and_reduction` | MVI: Intent, Reduction and One Current State | `mvi` | `unidirectional_data_flow`, `state_ownership`, `kotlin_sealed_types` |
| `lesson_classifying_a_real_architecture` | Classifying What a Real Codebase Actually Does | `mvvm_vs_mvi` | `mvvm`, `mvi`, `mvp`, `architecture_tradeoffs` |

The Unit ships directly after `unit_domain_logic_and_dependency_direction`, so the
`architecture` Topic now holds **five Units of five Lessons each**, and the whole document
holds **23 active Units and 97 active Lessons**.

**Configured versions, re-verified.** `gradle/libs.versions.toml` was re-read during
authoring: `androidx-lifecycle` is still `2.11.0-beta01`, Kotlin still `2.4.10`,
kotlinx.coroutines still `1.11.0`, Compose Multiplatform still `1.11.1`. Unchanged from the
table this plan assumes and from what E26-05 recorded.

### Unit purpose, and how it stays separate from Units 1–4

Units 1–4 taught every decision these pattern names are about without using a pattern name
once. Unit 5 adds the vocabulary and immediately says what the vocabulary is worth. **Its
purpose is classification, not prescription.**

- **The Unit adds no new mechanism.** Every arrangement in it is one the reader already has
  the means to reason about; what is new is the naming and the skill of reading a design
  back into responsibility terms.
- **Its five Lessons are one argument.** Establish the measurement instrument with no name
  in it; apply it three times to one unchanged screen, each time as a *single* change to the
  arrangement before it; then remove the training wheels and classify a design nobody
  labelled. Opening on a pattern name would teach the label before the reasoning the label
  summarises, which is the failure the whole Unit order exists to prevent.
- **No recommendation is made.** The Unit does not conclude that MVI suits complex screens,
  that MVVM is the default, or that MVP is legacy. It says what each arrangement buys and
  costs and stops; choosing the smallest sufficient architecture is Unit 6's closing
  decision.
- **Unit 4 → Unit 5 orthogonality is taught explicitly.** L5.5's Senior section states that
  these are presentation responsibility models operating at a different concern from the
  dependency rule, that a codebase can coherently combine Clean-Architecture-style dependency
  direction with MVVM-style presentation and MVI-style transitions, and that this is three
  independent decisions rather than a contradiction.
- **Unit 2 is reused rather than retaught.** L5.3 says in as many words that the reader has
  already built this arrangement and that the Lesson supplies a name for it, not a design.

### The one screen, and how it survives

The blueprint fixes a **practice-configuration screen** and E26-05 recorded that it was held
in reserve through Units 1–4, which used the borrowed-items library feature instead. That
reservation held: the screen appears for the first time in L5.1 and is **unchanged through
L5.2, L5.3 and L5.4.** Its requirements are stated once, in L5.1:

- it loads the available practice options — which topics can be practised and which
  difficulty levels exist;
- it holds several selections: a topic, a difficulty, and how many questions the session
  should contain;
- it validates those selections against each other, because not every combination has
  questions behind it;
- it enables a Start control only when the configuration is valid;
- it can be loading, and it can fail while loading;
- after a failure it offers a retry.

**No requirement was added or removed to suit a pattern**, and the MVP version is not
simplified relative to the MVI version — the MVP Lesson writes out the same cross-validation
(`setStartEnabled(selection.isValid())`) that the MVI Lesson expresses as a derived field of
the next state. Every structural difference between the three Lessons is traceable to one of
the five answers.

### The five-question frame, and the sixth consequence

L5.1 ships the frame as the Unit's measuring instrument, in exactly the order the issue
requires: where the screen's state lives, who is allowed to mutate it, whether the behaviour
owner knows the View or UI instance, how user input reaches the owner, and how output or
state reaches the UI. The **sixth** — what lifetime bookkeeping the arrangement forces on
somebody — is presented as a *consequence* of the five rather than a sixth category, and
L5.1's Senior section reads it in both directions: a held UI reference creates the obligation
to know whether the view is still there, and the absence of one removes that specific
obligation without removing lifetime as a subject. Each of L5.2, L5.3 and L5.4 then opens by
answering the same six lines for its own arrangement, in a fixed-width block, so the reader
can diff them.

L5.1's unlabelled description is worked in full and then answered with "we already know the
important part", before any name is offered. The three-team comparison table shows the same
arrangement called MVVM, MVI-ish MVVM, and UDF/state-holder architecture, with the last
column stating what actually differs — nothing, in two of the three rows. Labels are
explicitly **not** made meaningless: the Lesson states that inside a team with a shared
definition a label is a useful three-syllable summary, and narrows the claim to the one that
holds, that a label cannot replace the description.

### Terminology sources reverified, and the variance treatment

The [terminology variance](#mvp-mvvm-and-mvi-terminology-variance) table is this issue's
source contract and every row of it was re-opened during authoring. **All findings held.**

| Source | Used by | Claim it settles |
| --- | --- | --- |
| [GUI Architectures](https://martinfowler.com/eaaDev/uiArchs.html) | L5.1, L5.2, L5.3, L5.5 | That "MVC is one of the most misunderstood architectural patterns around" and that systems using the name "display a range of important differences"; that "a lot of designs will follow the MVP style but use 'controller' as a synonym for presenter"; that of the two best-known early MVP accounts "the two descriptions don't entirely mesh"; and that the fully passive variant "isn't part of the original descriptions of MVP" |
| [Passive View](https://martinfowler.com/eaaDev/PassiveScreen.html) | L5.2 | The passive-view variant, in which the presenter manipulates every widget and the view holds no behaviour |
| [Supervising Controller](https://martinfowler.com/eaaDev/SupervisingPresenter.html) | L5.2 | The other variant, quoted verbatim: the view "handles simple mapping to the underlying model" and the controller "handles input response and complex view logic" |
| [Presentation Model](https://martinfowler.com/eaaDev/PresentationModel.html) | L5.3 | MVVM's ancestry — "Represent the state and behavior of the presentation independently of the GUI controls used in the interface" — and, decisively for the variance argument, that Fowler describes **both** reference directions, the presentation model referencing the view or the view referencing the presentation model, with different trade-offs |
| [UI layer](https://developer.android.com/topic/architecture/ui-layer) | L5.1, L5.3, L5.4, L5.5 | That UI state is immutable and that "Only sources or owners of data should be responsible for updating the data they expose"; and "The pattern where the state flows down and the events flow up is called a unidirectional data flow (UDF)". **Re-checked during authoring: the words MVVM, MVP and MVI appear nowhere on the page**, which is the evidence L5.3 uses for the label being optional |
| [UI layer: state holders](https://developer.android.com/topic/architecture/ui-layer/stateholders) | L5.3 | That a business-logic state holder is "typically implemented with a `ViewModel`" and a UI-logic state holder "typically implemented with a plain class" — two implementations of one responsibility, chosen by lifetime. **Re-checked: "MVVM" does not appear on this page either** |
| [Guide to app architecture](https://developer.android.com/topic/architecture) | L5.1 | The platform's architecture vocabulary, stated without pattern acronyms |
| [Unidirectional User Interface Architectures](https://staltz.com/unidirectional-user-interface-architectures.html) | L5.4, L5.5 | The MVI lineage from its originating author: "Intent: function from Observable of user events to Observable of 'actions'", "Model: function from Observable of actions to Observable of state", and the cycle in which "the user's rendering reacts to the View's output, which reacts to the Model's output, which reacts to the Intent's output (actions), which reacts to user events" |

Two source decisions are worth recording because both are acceptance-critical:

- **Fowler is cited as evidence of practitioner usage, never as a specification the
  curriculum adopts.** This is the qualified exception the blueprint's source policy allows
  for Unit 5, and it is used exactly as written: the quotations establish *that the
  terminology varies*, not what the terms must mean.
- **The MVI lineage and its Android adaptations are kept separate.** L5.4's Senior section
  names Cycle.js and Staltz's formulation as the origin, states that Android versions are
  adaptations that differ from it and from each other on reducer purity, store abstractions,
  side-output delivery and whether every input value is called an intent, and says outright
  that **no adaptation is canonical**. No MVI library is named anywhere in the Unit.

**MVC variance treatment.** MVC is taught only as the origin of the Model/View/x vocabulary
and as the reason the vocabulary is unreliable, with the "controller" role named as the
especially overloaded part. There is **no fourth implementation of the practice screen**, no
Smalltalk internals, no MVC framework history and no claim that any definition of MVC is
canonical. L5.1's practical conclusion is the interview move: ask what the interviewer means
by the controller's responsibility, then answer in the five questions.

**MVP stable and variable.** Stable and taught: the presenter holds a view abstraction, the
view is comparatively passive, the presenter pushes output by calling view methods, the view
reports input inward, and attach/detach bookkeeping follows from the presenter holding the
view. Variable and stated as variable: passive view against supervising controller, how much
formatting the presenter owns, and whether the role is called presenter or controller.

**MVVM stable and variable.** Stable and taught: the UI observes state the owner publishes,
the owner holds no reference to the view, intentions travel inward and state travels outward.
Variable and stated as variable: whether data binding is required, whether the class must be
called `ViewModel`, how many observable values a screen may expose, and the shape of commands
or events between UI and owner. The Presentation Model ancestry is one paragraph and carries
the two-reference-directions finding, which is the sharpest available evidence that the name
never had one arrangement behind it.

**MVI stable and variable.** Stable and taught: input as an explicit value from a closed set,
one current state describing the screen, an explicit transition, unidirectional state
evolution. Variable and stated as variable: reducer purity, store abstractions, side-output
mechanisms, whether all inputs are called intents, and library machinery. L5.4 states its own
definition's limits explicitly — it does **not** require a pure reducer, a `Store` type, a
separate transition class or any library.

### The two terminology collisions

**"Effect" is never used unqualified.** L5.4 names the collision directly: Compose's
`LaunchedEffect`, `DisposableEffect` and `SideEffect` are a family of APIs for work tied to a
composition's lifetime, and MVI literature's "effect" is a one-off output — unrelated
abstractions. The Lesson uses *MVI-style side output* and *a one-off output sometimes called
an effect in MVI literature*, and says in as many words that the bare word is never used for
either. **Verified across the shipped Unit**: no Lesson uses "effect" in the MVI sense
unqualified, and the only other occurrences are the named Compose API family in that same
paragraph.

**"Event" is avoided where it would be ambiguous.** The Unit uses *intention* or *action* for
user input, *occurrence* where something happened and a delivery question is live, and
qualifies the word elsewhere. The one place the unqualified plural survives is in a direct
quotation of Android's UDF sentence ("the events flow up"), which is quoted as a source and
labelled as such.

### What each Lesson actually does, against the issue's requirements

**L5.1 — the frame.** Opens by stating that there is no normative specification for modern
use of these four names, with Fowler's two sentences as the evidence, and then introduces the
five questions as a bulleted instrument. The sixth is presented as a consequence with its
derivation. The practical half fixes the screen's six requirements, answers the five
questions about one unlabelled design in a fixed-width block, asks "what architecture is
this?" and answers that the important part is already known. The three-team table follows,
then the bounded MVC treatment, then a common-mistake callout aimed at the name-only interview
answer. Senior depth explains *why* the names drifted — different toolkits over decades,
originating accounts that "don't entirely mesh", Presentation Model described with two
possible reference directions — and re-reads the sixth question in both directions so that
"no view reference" is not misread as "lifetime stopped mattering".

**L5.2 — MVP.** The screen is carried over and exactly one answer changes: the owner is given
a view abstraction. The six answers are re-run in the same block format, with question three
marked as the defining property. Both variants are named from their primary sources and the
presenter/controller naming variance is stated. The practical half ships the `PracticeConfigView`
contract, a presenter holding one with `attach`/`detach`, the push-style call sequence, and the
**causal** attach/detach explanation: the presenter holds the reference, the UI instance can be
destroyed while the presenter survives, a reference to a destroyed view is still a valid
reference, therefore something must coordinate when it is usable. No lifecycle callback appears
anywhere. The ordering claim is deliberately narrowed — it is **not** claimed that MVP
implementations necessarily have ordering defects, only that correctness here is a property of
the call sequence rather than of a value. Four strengths are named, including that the contract
is consumer-owned in the foundations Unit's exact sense, which is why substitution is real
rather than asserted; six costs are named as things, never as "boilerplate". Senior depth states
the trade explicitly — enumerability against consistency — lists what MVP does *not* decide
(threading, survival, data layer, view granularity), and closes on the habit of asking whether a
named cost belongs to the pattern or to the environment.

**L5.3 — MVVM.** Opens on the MVP comparison as the plan requires — what happens if the owner
stops calling the view — and answers it by inverting the output direction. The load-bearing
property is stated as a negative and question three is the only answer that changes. What
disappears is enumerated precisely (the held reference, the attach/detach relationship required
by it, the enumerated call surface, the ordering question) and what does **not** disappear is
stated in the same paragraph: the owner's lifetime, the observer's lifetime and the cancellation
of owner-scoped work all remain, and removing one reference repeals none of it. The practical
half opens by telling the reader they have already built this, then ships the three corrections:
a `ViewModel` class does not make an architecture MVVM — with the converse stated as firmly, and
with a `ViewModel` holding a view interface named as "an MVP presenter with an inconvenient
superclass" — a folder layout does not, and the number of observable values does not. The
many-against-one correction states the real trade-off (a complete consistent description against
a consumer observing part-way through a multi-field change) and attributes it to state modelling
rather than to the pattern. The section closes on the strongest available evidence: Android's own
UI-layer page describes the whole arrangement, including UDF by name, without using any of the
three acronyms. Senior depth derives every other MVVM characteristic from the absent view
reference — while bounding how much that inversion forces, since an owner with no view reference
can still publish one-off instructions — carries the Presentation Model variance, and keeps
dependency direction and lifetime separate.

**L5.4 — MVI.** No new requirement is added. The two ideas are introduced separately — input as a
value from a closed set, and one current state with an explicit transition — and the sealed
declaration is shown without the Kotlin mechanism being taught. Reduction is defined
conceptually, with the definition's limits stated immediately. The practical half writes one
transition out in full in the issue's exact shape (current, intent, next), shows where validation
belongs by deriving `canStart` inside the transition, and ships a short `reduce` function. Four
benefits are listed as checkable properties; five things MVI does **not** buy are listed
immediately after, covering correctness, thread safety, persistence, once-only handling and
performance. Five ceremony items are counted as things. The state-versus-side-output question is
named only far enough to explain the cost, with the terminology collision qualified in the same
paragraph and the delivery design handed to Unit 6 by naming what that Unit decides — mechanism,
loss, lateness and acknowledgement — without deciding any of it. Senior depth rejects the
many-against-one distinction and the recompositions claim, gives the Cycle.js origin with
Staltz's own function signatures, and closes on when the modelling is worth its ceremony.

**L5.5 — classification.** Ships the classification procedure as a fixed order: answer the five
questions, name what each answer resembles, state the design in a sentence, then offer a label.
Three hybrids are given as a four-column table — a lifecycle-owned `ViewModel` with one state
value and callbacks and no reducer; the same with action values, an explicit reducer and a
separate side-output mechanism; and a several-observable design otherwise identical to the
first — each with a responsibility reading and the labels different teams would use. The four
misconceptions are then each **falsified against one of those designs** rather than contradicted:
the stream-count claim fails because A has one value and none of MVI's transition model; the
sealed-intents claim fails against a shipped code block that adds an action type to design A and
changes nothing about the transition; the recomposition claim fails because the single large
state value can invalidate a composable for fields it never renders; and the folder claim fails
because an MVP presenter filed under `viewmodel/` still reads as MVVM in the project pane. Senior
depth carries the repository evidence, the two boundaries (presentation-only, and orthogonal to
Clean Architecture), and the interview answer — given in full and immediately qualified as
something to be able to produce rather than to memorise.

### Corrections made during review

Three defects were found by re-reading the finished Unit against its own later claims, and all
three are recorded because they share one failure mode: **an absolute stated where the Unit's own
material supports only a narrower claim.** A unit whose subject is that labels overclaim is
exactly the place for an author to overclaim, and two of the three contradicted content this
repository already ships.

| Where | Defect | Correction |
| --- | --- | --- |
| L5.1, Core and Practical | The five questions were said to produce "a complete description of its architecture", and the practical half claimed that "every question that can be asked about how this screen behaves under a requirement change" is answerable from the six lines — with "where a new rule goes" as one of its examples. **L5.5's Senior section says the opposite in as many words**: these are presentation responsibility models and they decide nothing about the repository, the domain layer, dependency supply, navigation or modularization. The illustrative clause was the clearest error, since where a rule belongs is Unit 4's question | Scoped to what the five questions actually settle — completely describing *how a screen's UI and its behaviour owner relate*, which is the whole of what these four names are about. The practical half now names what the six lines settle, and then names what they deliberately do not: whether there is a repository, which source is authoritative, whether a rule earns a domain layer, how dependencies are supplied. The orthogonality argument now opens the Unit as well as closing it |
| L5.2, Practical | One value describing the whole screen was said to be an arrangement in which an inconsistent combination "cannot be expressed at all". **This contradicts shipped Unit 2 material**: `lesson_modelling_ui_state` teaches that a data class of independent fields can represent combinations the product forbids, and treats it as a real defect. A state value holding the selection beside a separate `canStart` can be copied with one updated and the other stale. It also overstated `architecture_mvi_single_state`, whose own explanation grounds the benefit in a consumer observing part-way through a multi-field change | Replaced with the distinction the sources actually support: one value does not by itself make the bad combination unthinkable, and the representable-but-impossible problem is named and pointed back at Unit 2. What one value changes is that the two facts travel together, so no consumer observes the screen mid-update — and making the combination genuinely impossible is then one further step available to it, by deriving the flag or modelling the pair so it cannot disagree, which the call-sequence arrangement has nowhere to take |
| L5.3, Senior | "Once the owner cannot call the UI, output *must* be something the UI reads, which **forces state rather than instructions**." Removing the reference forces output to be *read* rather than *pushed*; it does not force every output to be current state. **The Unit refutes this itself two Lessons later**, citing `PracticeBuilderViewModel` — an owner with no UI reference publishing a one-off `StartPractice` instruction through a channel — and L5.4 spends a paragraph on side outputs precisely because they are not state | Rewritten as the push-to-read inversion, with the limit stated immediately: consumer-read output becomes the only channel and current state is the overwhelmingly common form of it, but an owner holding no view reference can still publish one-off instructions, which the following two Lessons take up. The dependency chain that follows — the owner need not know whether anyone is looking, therefore it is safe for the UI to be absent — is unchanged, because it rests on the inversion rather than on the output being state |

The parallel overclaim in L5.3's many-against-one paragraph ("forbids combinations the owner
never intended") was narrowed in the same pass, to keep the one-value guarantee stated
identically wherever the Unit makes it. **None of the four changed an identity, a mapping, a
source, a cross-link or a Lesson boundary**, and none changed the resolved practice pool.

### Repository evidence used, and its limits

Every Part 7 finding this Unit relies on was re-verified against current code before use.

| Finding | Re-verified as | Used by |
| --- | --- | --- |
| **`LessonScrollStateReducer`** | Read in full. Still present at `topic_study/learning_lesson/LessonScrollStateReducer.kt`, still turning raw scroll positions into `LessonScrollUiState`. **One correction to the E26-01 note:** it is *not* a pure function of a state and an input — it accumulates `lastPosition` and `accumulatedDistance` in its own fields across calls | **L5.5's strongest local evidence.** Used for two claims: that a transition function can exist in a codebase nobody would call MVI, and that "reducer" itself travels loosely enough that a real one need not be pure. Explicitly **not** presented as evidence that this application "uses MVI" |
| **`PracticeBuilderUiState` and `PracticeBuilderViewModel`** | Both read in full. The state is still a `data class` whose KDoc records the choice ("Everything the Practice Builder renders, and nothing it would have to derive"); `isStartEnabled` is still a derived property of the state; the ViewModel still exposes `MutableStateFlow(...).asStateFlow()`, still takes named callbacks (`selectQuestionCount`, `toggleLevel`, `selectSource`, `retryAvailability`, `startPractice`), still holds no UI reference, and still sends its one outward occurrence through a `Channel<PracticeBuilderEvent>` | **L5.5's production instance of hybrid design A.** Also the reason the hypothetical stays hypothetical: the Lesson cites the production screen as a classified example and never lets it stand in for the worked comparison |
| **UI state is modelled both ways** | Re-counted: 16 UI-state types in `commonMain`, 14 sealed interfaces and 2 data classes (`PracticeBuilderUiState` and `LessonScrollUiState`). E26-01 recorded one data class; the second is the reducer's output type and was counted this time | L5.5, as evidence that state-shape choice is not itself a pattern label |
| **15 `ViewModel` classes, all in `commonMain`, all `internal`** | Re-verified by search: still 15, still all in `commonMain` | L5.3 and L5.5, as evidence that a `ViewModel` implementation participates in whatever responsibility arrangement the surrounding design has, and is not itself the arrangement |
| **No production owner holds a UI reference** | Verified by search across `commonMain` | L5.3's three-second classification test, stated as a method rather than as a claim about this app |

**What the evidence is not used for.** No Lesson claims this repository demonstrates MVP, and no
Lesson claims it "uses MVVM" or "uses MVI" — the two places production code appears, it is
classified by the six answers and the label is left to the reader. **No production architecture
code was changed or proposed for change**, and nothing in authoring surfaced a product defect
worth recording. The one correction above is a correction to this plan's own note about
`LessonScrollStateReducer`, not to the code.

### Source-sensitive ViewModel and lifecycle claims

E26-05 recorded that E26-06 inherits Part 8's verification obligation if it makes ViewModel or
lifetime claims. **The obligation was discharged by not incurring it.** The configured lifecycle
version was re-read and is unchanged at `2.11.0-beta01`, and the Unit deliberately makes no
concrete claim about ViewModel retention, clearing, host ownership, configuration recreation or
multiplatform lifetime. What it says is:

- L5.3 states the dependency direction — the owner holds no reference to the UI — and hands
  lifetime to Unit 2 by linking `lesson_viewmodel_lifetime_and_persistence`, with an explicit
  paragraph warning not to translate "the owner does not reference the view" into a claim about
  what survives what, and naming the multiplatform host dependency in the same sentence.
- L5.2 says that a presenter can be retained by something long-lived and that this *increases*
  attach/detach bookkeeping rather than removing it — a statement about the arrangement, not
  about any platform's retention guarantee.
- **The sentence "ViewModels survive the screen" appears nowhere**, and the E26-03 correction is
  not regressed anywhere in the Unit.

`stateflow` stays supporting-only and no `StateFlow` mechanic is taught: L5.3 states explicitly
that the choice of carrier is not part of the pattern and that E24 owns stream behaviour.

### Semantic review of the Questions this Unit now reaches

All four ACTIVE Questions in the resolved pool were re-read in full and independently solved
against the finished prose. **The E26-01 findings in [Part 5](#part-5--semantic-assessment-review)
all still hold**, including both mismatches. The `architecture` Topic still holds 22 ACTIVE and 4
DEPRECATED Questions over 18 Subtopics, re-counted from the bundled JSON during authoring.

| Question | Level | Reached through | Re-read verdict against the shipped Lessons |
| --- | --- | --- | --- |
| `mvp_vs_mvvm_view_contract` | FOUNDATION | `mvp` (L5.2) | **Sound and precisely matched — the Unit's best-fitting Question, as predicted.** Its correct option is exactly the contrast L5.2 and L5.3 are built on: the presenter holds a view interface and pushes updates in, the state owner exposes state the view observes. Its explanation carries the attach/detach consequence that L5.2 derives causally. The Lessons reproduce none of its wording or options, and the three distractors are claims the Unit refuses independently — L5.2's Senior section states in as many words that MVP dictates nothing about threading, nothing about survival, and does not forbid modelling navigation. It spans two Lessons, which Part 5 already recorded as fine |
| `viewmodel_vs_repository_responsibility` | APPLIED | `mvvm` (L5.3) | **Sound as a Question; mismatched to its Subtopic, exactly as E26-01 found.** Its reasoning is the repository/state-holder split, cache and retry policy placement, and why a state holder must not hold a `Context` to format state — which `lesson_state_holder_responsibility` and `lesson_what_a_repository_owns` own and teach. L5.3 was **not** reshaped to fit it and teaches none of that reasoning; a reader who has only read Unit 5 is not equipped for it, and a reader who reached Unit 5 in order is, because Units 2 and 3 taught it. The consequence stands: MVVM as a responsibility model remains unassessed. See GAP-U5-B |
| `architecture_ui_event_consumption` | APPLIED | `mvi` (L5.4) | **Sound as a Question; mismatched to its Subtopic, and it is the strongest re-map candidate in the Topic.** Its reasoning is that a replay cache re-delivers to a late collector because the stream records no consumption, so handling once requires the consumption to be recorded — Unit 6 L6.2's subject entire. **L5.4 was deliberately not distorted to support it**: the Lesson names the side-output design question only far enough to count it as a cost and hands delivery, loss, lateness and acknowledgement to Unit 6 without designing any of them. The consequence stands: Unit 5 structurally receives a Unit 6 Question while MVI's transition model remains unassessed. See GAP-U5-C |
| `architecture_mvi_single_state` | APPLIED | `mvvm_vs_mvi` (L5.5) | **Sound and well matched to L5.5, and it functions as a semantic guard rather than a target.** Its correct answer — one immutable state prevents the UI rendering combinations the owner never intended — is taught by L5.3's many-against-one paragraph and L5.5's first misconception, and its explicit refusal of the recomposition claim is the same refusal L5.4 and L5.5 make, with L5.5 adding the direction the Question only hints at: a single large value can recompose *more*. **Its scenario is not reproduced** — the Lessons never pose "a screen currently exposes several independent StateFlows" as a migration question, and design C exists precisely to make the several-observable arrangement a legitimate classification rather than a thing to be migrated away from |

Adjacent Questions were inspected as duplication guards: `architecture_state_holder_taxonomy`
and `durable_state_vs_one_off_event` (both `state_ownership`) and `compose_udf_event_direction`
(`compose_udf`). None is reachable from a Unit 5 primary concept, and none assesses reasoning
this Unit claims — which is the intended result of `state_ownership` and
`unidirectional_data_flow` being supporting-only here.

**No Question was created, edited, re-mapped, re-levelled or re-statused by this issue**, no
taxonomy entry was invented, and no factual defect was found in any Question read. The two
mapping mismatches were **not repaired here**; E26-08 owns them.

### GAP-U5-A to GAP-U5-D after authoring

All four gaps survive the finished prose. **None was closed, none was found wrong, and no new
gap was created.** The issue's instruction is honoured in both directions: a gap is not declared
closed because a structurally reachable Question exists, and a gap is not preserved merely
because it was planned.

| Gap | Status after E26-06 | What the finished Lesson changes about it |
| --- | --- | --- |
| GAP-U5-A | **Open, unchanged** | L5.1 now ships the whole classification instrument: five questions, a worked unlabelled description of the screen, and the three-team table showing one arrangement under three names. `mvc` still holds **zero** ACTIVE Questions, so the Unit's central contribution reaches no practice at all. Confirmed by running the production resolver, not by reading the mapping. The mapping was **not** changed to manufacture coverage |
| GAP-U5-B | **Open, unchanged; now the sharper of the two MVVM-side gaps** | L5.3 now ships the reasoning exactly as the gap defines it — an observing UI and an owner that holds no view reference, against the class-name and folder-name definitions, each rejected with a counterexample rather than an assertion. `mvvm`'s only ACTIVE Question still assesses the ViewModel/repository split. The gap is now a gap in assessment of material that ships, which is the strongest case an E26-08 Question can have. If `viewmodel_vs_repository_responsibility` is re-mapped, `mvvm` becomes empty and this gap becomes obligatory |
| GAP-U5-C | **Open, unchanged; still a strong candidate** | L5.4 now ships intent-as-a-value, one current state, an explicit transition written out in full, and five counted ceremony items. `mvi`'s only ACTIVE Question is still a delivery-and-consumption Question. Nothing assesses reduction, and "MVI is MVVM plus sealed intents" — which L5.4 and L5.5 both correct, the latter with a falsifying implementation — still cannot be assessed at all |
| GAP-U5-D | **Open, unchanged** | L5.5 now ships three hybrid designs with their responsibility readings and the labels different teams would attach. `architecture_mvi_single_state` still assesses the one-state property and stops there; classifying an unlabelled hybrid from ownership, view knowledge, mutation path and transition explicitness is still unassessed |

Two observations are recorded for E26-08 without being promoted to gaps:

- **Nothing assesses the pattern-versus-property distinction that L5.5's four corrections rest
  on.** The single most testable idea in the Unit is arguably that adding a closed input type to
  an ordinary state holder changes the spelling of the input and nothing about the transition
  model. It may belong inside a GAP-U5-C or GAP-U5-D Question rather than as one of its own.
- **Nothing assesses the orthogonality claim.** That MVP, MVVM and MVI decide presentation
  responsibilities and do not decide the data layer, the domain layer, dependency supply,
  navigation or modularization is L5.5's Senior half and one of the Unit's most interview-relevant
  conclusions. It spans Units 1–5 rather than sitting inside one, so E26-08 should weigh it
  against GAP-U1-E and GAP-U4-A rather than authoring into Unit 5 alone.

### Actual practice reach, resolved through the production resolver

Recomputed by running the shipped Unit through `PracticeBuilderViewModel` and the real selection
path in `LearningUnitPracticeIntegrationTest`, not by reading mappings. The Unit configures
`AssessmentScope.Subtopics` of exactly its five primary concepts and resolves **four** Questions:

| Resolved Question | Level | Reached through | Semantically belongs mainly to |
| --- | --- | --- | --- |
| `mvp_vs_mvvm_view_contract` | FOUNDATION | `mvp` | Unit 5 |
| `viewmodel_vs_repository_responsibility` | APPLIED | `mvvm` | **Units 2 and 3** — structural reach only |
| `architecture_ui_event_consumption` | APPLIED | `mvi` | **Unit 6** — structural reach only |
| `architecture_mvi_single_state` | APPLIED | `mvvm_vs_mvi` | Unit 5 |

This matches [Part 6](#part-6--unit-practice-routing-modelled-now)'s modelled pool exactly — four
Questions, the same four ids — so E26-01's prediction is confirmed rather than assumed. The level
split is one FOUNDATION and three APPLIED, with **no ADVANCED Question**, matching the Topic-wide
observation in Part 5. **Only two of the four semantically assess what this Unit teaches**, which
is the epic's least favourable ratio and the reason GAP-U5-B and GAP-U5-C matter more than their
peers.

Three further structural facts, all asserted in tests rather than inspected:

1. **`mvc` contributes nothing.** Five primary concepts resolve four Questions, and the Unit's
   opening concept reaches none. Asserted by subtopic id.
2. **Supporting concepts broaden nothing.** The Unit's seven supporting-only concepts —
   `state_ownership`, `unidirectional_data_flow`, `stateflow`, `viewmodel_lifecycle`,
   `kotlin_sealed_types`, `interface_boundaries` and `architecture_tradeoffs`, holding **fourteen**
   ACTIVE Questions between them — contribute nothing to the pool. `state_ownership` is the one
   that matters: it is primary across seven Lessons of two other Units, so promoting it here would
   pull the whole state-holder pool into a Unit that teaches none of it.
3. **This Unit shares no Question with any earlier architecture Unit.** It is the first E26 Unit
   with no overlap, because the five pattern concepts are primary nowhere else in the epic. The
   test computes all four earlier pools and intersects them rather than assuming it. **No shipped
   Unit's practice changed**: Unit 1's pool is unchanged at five, Unit 2's at five, Unit 3's at six
   and Unit 4's at five.

### Cross-links

Backward only, and every target already shipped. L5.1 → `lesson_what_architecture_decides`,
`lesson_state_holder_responsibility`; L5.2 → L5.1, `lesson_when_an_interface_is_a_boundary`;
L5.3 → L5.2, `lesson_state_holder_responsibility`, `lesson_state_out_intentions_in`,
`lesson_viewmodel_lifetime_and_persistence`, `lesson_state_flow`; L5.4 → L5.3,
`lesson_modelling_ui_state`, `lesson_state_out_intentions_in`; L5.5 → L5.1, L5.2, L5.3, L5.4.

The plan's explicit requirement — that L5.3 links to the relevant Unit 2 Lessons and to
`lesson_state_flow` — is met with four of those five links; the fifth is L5.2, which L5.3 opens
by comparing itself against. Every other link is an actual semantic dependency: L5.1 leans on the
architecture-is-not-structure argument and on what a screen owner is responsible for, L5.2 on the
consumer-owned-abstraction test because the view contract is one, L5.4 on the UI-state modelling
Lesson because one current state is its input and on the intentions-in Lesson because reduction is
the write path made explicit, and L5.5 on all four Lessons it synthesises. **No shipped Lesson was
edited**, no earlier Unit received a reciprocal link, and **no forward link into Unit 6 exists** —
L5.4 and L5.5 name the synthesis Unit in prose without naming a Lesson id.

### Tests changed, and why

| File | Change | Why production data made it necessary |
| --- | --- | --- |
| `BundledLearningCurriculumTest` | Unit id, title and home-Topic lists extended by one; Lesson id/title order and primary mappings for the new Unit added; two new tests — `responsibilityModelsUnitKeepsItsPlannedBridgesOutOfPrimaryPractice` and `responsibilityModelsUnitLinksBackwardsOnlyToShippedArchitectureAnchors` | The document lists Units positionally, so a fifth architecture Unit changes four lists. The bridge test exists because this Unit has the epic's densest primary/supporting reuse — every pattern concept is primary in one Lesson and supporting in at least two others — and because promoting `state_ownership`, `stateflow`, `kotlin_sealed_types` or `viewmodel_lifecycle` would silently claim another curriculum's practice. The link test pins the five-link MVVM Lesson the plan explicitly requires |
| `LearningUnitPracticeIntegrationTest` | The traversal now expects five architecture Units and `listOf(5, 5, 5, 5, 5)` Lessons, and its final studied-record count rises from 91 to 96; new test `thePatternUnitPractisesItsPrimaryConceptsIncludingTwoThatBelongElsewhere` | Continue Learning walks the whole document, so a fifth architecture Unit changes the traversal and the Topic's progress denominator. The Unit **cannot** join the shared expectation table, which asserts `concepts == resolved subtopics`: `mvc` holds no ACTIVE Question, so the identity fails exactly as it does for Unit 1. The bespoke test therefore pins the four ids, asserts that `mvc` contributes none, computes the empty intersection with all four earlier Units, and records the two structural mismatches |

**The tests record structural reality and do not endorse it.** Both mismatch assertions are
commented as such: they exist so that if E26-08 re-maps either Question the assertion fails and
the consequence has to be re-stated, rather than the mapping being quietly repaired. No test was
added that only re-states schema validation `LearningCurriculumValidatorTest` already performs,
and the data-driven suites — the reader journey over every shipped Unit, Topic Detail's Unit rows,
the end-to-end repository path — needed no edit because they read the document rather than listing
it. No prose is snapshotted anywhere.

### Validation performed

| Command | Result |
| --- | --- |
| `python3` structural pre-check over both bundled JSON documents | Ids unique across the whole document, every mapping an ACTIVE Subtopic, no primary/supporting overlap within a Lesson, every `relatedLessonIds` target resolvable and non-self, no blank text, every comparison row matching its header count, every Source URL well-formed. No defect in the new Unit |
| `./gradlew :shared:jvmTest --tests "*BundledLearningCurriculumTest*" --tests "*LearningUnitPracticeIntegrationTest*" --tests "*LearningCurriculumValidatorTest*" --tests "*LearningContentEndToEndTest*"` | **104 tests, 0 failures** |
| `./gradlew :shared:jvmTest` | **1,399 tests, 0 failures**, including `LearningProductionContentJourneyTest`, which renders every authored block of the new Unit in the reader, checks the reading column never widens, and opens every authored Source link through the app's own URI boundary |
| `python3 tools/learning_question_coverage.py --write` then `--check` | Snapshot regenerated and reported current |
| `cd tools && python3 -m unittest test_learning_question_coverage.py` | 21 tests, OK |
| `./gradlew :shared:check` | Passed |
| `./gradlew :androidApp:assembleDebug` | Passed |
| `git status --short` and `git diff --check` | Five files changed — the bundled learning document, two jvm test files, this plan and the generated coverage snapshot — with no build or cache output and no whitespace defects |

The regenerated `docs/content/learning-question-coverage.md` now reports **23 active Units and 97
active Lessons**, the new Unit appearing directly after Unit 4 with five Lessons, a pool of four
Questions across two levels and no ADVANCED Question. **Primary Subtopics with no active Question
rises from one to two** — `architecture_tradeoffs` (GAP-U1-E) and now `mvc` (GAP-U5-A) — which is
the generated confirmation that GAP-U5-A is real rather than a prediction.

### Not validated

- **`iosArm64` is not compiled locally or on CI**, unchanged from E25 and from every E26 issue so
  far. Unit 5 introduces no target-specific code and makes no source-sensitive ViewModel or
  lifecycle claim, so nothing in its prose depends on that target; the limitation is reported
  because it is still true of the epic.
- **No CI run is claimed.** Nothing in this issue was observed on GitHub Actions.
- **Backlog validation could not be run**: `PyYAML` is unavailable in this environment, so
  `.github/project/backlog.yml` was read as text rather than parsed and validated. Issue #366 was
  read from that file rather than through `gh`, which is still not installed.
- **The learning content itself is editorial** and no automated check can confirm that a Lesson
  teaches what it claims; the semantic review above is a judgement, as Rule 10 of the authoring
  contract requires.

---

## Authoring outcomes for Unit 6

Added by E26-07 after the four Lessons were written. Unit 6 is the **last instructional Unit
of E26**, so this section also closes the authoring run that E26-02 opened: every source
quotation, repository finding and routing figure below was re-opened, re-read against current
code, or executed during authoring rather than carried over from E26-01's tables, and where a
finding is unchanged that is stated as a re-verification rather than as a copy.

### What did not change

Every proposed Unit id, Lesson id, title, authored order and primary/supporting mapping for
Unit 6 in the [identity tables](#identity-conventions-and-proposed-identities) shipped
verbatim. **No Lesson boundary moved, none was split or merged, and no mapping was altered to
improve practice routing.** The blueprint's `L6.1`–`L6.4` ordering, its
Teach/Bridge/Reference/Exclude decisions and its misconception targets were followed as
written, and nothing in Unit 6 required a Unit 1–5 identity, mapping or boundary to move.

| Shipped identity | Title | Primary | Supporting |
| --- | --- | --- | --- |
| `lesson_state_or_occurrence` | Is This State, or Is It Something That Happened? | `state_ownership` | `unidirectional_data_flow`, `stateflow`, `sharedflow`, `single_source_of_truth` |
| `lesson_delivery_guarantees` | What Guarantee Does This Occurrence Need? | `state_ownership` | `sharedflow`, `hot_vs_cold_streams`, `process_death`, `single_source_of_truth` |
| `lesson_choosing_the_owner_by_lifetime` | Choosing an Owner From the Lifetime the Requirement Needs | `state_ownership` | `lifecycle_coroutines`, `coroutine_scope`, `viewmodel_lifecycle`, `background_api_selection` |
| `lesson_smallest_sufficient_architecture` | How Much Architecture Does This Feature Need? | `architecture_tradeoffs` | `use_cases`, `repository_pattern`, `layered_architecture`, `clean_architecture` |

The Unit ships directly after `unit_responsibility_models_mvp_mvvm_mvi`, so the `architecture`
Topic now holds **six Units and 29 Lessons** in the planned 5/5/5/5/5/4 distribution, and the
whole document holds **24 active Units and 101 active Lessons**.

**One documentation correction outside the outcomes sections.** The blueprint's `Status`
section still said "no Unit has been authored", which was true when E26-01 wrote it and is now
false in the strongest possible way. It was corrected to state that all six Units have shipped
and that no Lesson boundary moved. No objective, depth layer, mapping, misconception row,
terminology entry or Exclude decision in the blueprint was touched.

**Configured versions, re-verified.** `gradle/libs.versions.toml` was re-read during authoring:
`androidx-lifecycle` is still `2.11.0-beta01`, Kotlin still `2.4.10`, kotlinx.coroutines still
`1.11.0`, Compose Multiplatform still `1.11.1`, Koin still `4.2.2`, Room still `3.0.1`.
Unchanged from the table this plan assumes and from what E26-06 recorded. The beta re-check
mattered less here than in Unit 2: Unit 6 makes exactly one source-sensitive ViewModel claim,
recorded below.

### Unit purpose, and how it stays separate from Units 1–5

Units 1–5 all begin with something that exists — a boundary to judge, a component to design, a
repository to shape, a layer to justify, an arrangement to classify. **Unit 6 begins with a
requirement and nothing built**, and that is the whole of what makes it a separate Unit rather
than an appendix.

- **It adds no mechanism.** Nothing in the four Lessons is a new abstraction, type or API. Every
  element is something an earlier Unit or an earlier curriculum already taught, and what is new
  is the order in which the decisions are taken.
- **Its four Lessons are one sequence, and each consumes the previous answer.** What must the
  application represent → what guarantee does that representation need → whose lifetime matches
  the guarantee → how much structure does the whole feature earn. No other order is available:
  L6.2 cannot start until L6.1 has said the thing is an occurrence, and L6.3 exists because L6.2
  ends on "an obligation needs an owner, so which one?".
- **Every Lesson reasons from the requirement towards the architecture.** No Lesson opens on a
  mechanism and looks for a use for it, and the one place where a mechanism comparison would be
  the obvious move — `Channel` against `SharedFlow` — is deliberately never made, for the reason
  recorded under [the stream-slogan correction](#the-stream-slogan-correction-and-the-mechanism-boundary).
- **The earlier Units are applied, not re-taught.** L6.4 in particular re-uses two features the
  shipped Lessons already introduced rather than inventing a third application.

### Terminology discipline, audited across the shipped Unit

E26-06's rules are preserved, and the Unit was scanned for both dangerous words after authoring
rather than trusted.

- **"Occurrence"** is used throughout for something that happened where the delivery question is
  live, **"state"** for something currently true, **"durable state" or "durable fact"** where the
  truth must survive process loss, and **"emission"** for transport mechanics. Every Lesson uses
  them consistently.
- **"Effect" never appears in either of its collided senses.** The only occurrence in the Unit is
  **"side-effect execution"**, in L6.2's exactly-once separation, where it names the third of three
  distinguished claims — whether the thing in the world actually happened once — and is defined in
  the same sentence. It is neither a Compose effect API nor an MVI one-off output, and no bare
  "effect" is used for a transient application output anywhere.
- **"Event" is never used bare for an application concept.** Every occurrence falls into one of
  four audited categories: the lifetime sense the blueprint's own register defines ("what event ends
  it", "the ending event"), which is L6.3's central vocabulary; a direct quotation of Android's
  guidance, labelled as a quotation; a slogan or a mistaken phrasing the Lesson is rejecting, set
  off as such ("something called an event", "this stream type is for state and that one is for
  events", "events are being lost"); and two places where the word names the question as an
  interviewer or a real-world description actually poses it — "a single real event in the world" and
  "asked whether something should be state or an event, refuse the binary". The last is deliberate:
  the interview asks it in exactly those words, and refusing the binary is the answer the Lesson
  teaches.

### L6.1: the same fact under three requirements

The fact is the blueprint's: **the practice session was scored.** It is held fixed and three
requirements are placed on it, each with its consequence spelled out and its failure named.

| Requirement | What the application must represent | What losing it means |
| --- | --- | --- |
| **A — render the finished session.** The result screen shows the score, the pass/fail verdict, the answers and the next actions | Current state: this session currently has a completed result, with current-value semantics so a consumer arriving later reads the correct result immediately | A defect: the screen would show a session with no outcome |
| **B — still know it tomorrow.** After leaving the screen, after process recreation, after restarting the application | The same current state with a retention requirement: it must be reconstructable after every in-memory owner is gone | A defect, and a worse one: the application would have forgotten something the reader was told |
| **C — celebrate on the active result screen.** A short animation, one haptic confirmation, a brief congratulatory message, while the reader is still there | A transient occurrence: performed once, in the presence of the UI it belongs to | Nothing. The requirement was about the moment and the moment has passed |

**The current-versus-durable nuance is taught explicitly rather than left to be inferred.** The
Lesson's Core section refuses the three-mutually-exclusive-types reading in as many words and
replaces it with two dimensions: first, *is this currently true or did it happen* — decided by
the requirement's verb, rendered against executed; second, asked **only** of the first kind,
*how long must that truth remain reconstructable*. Durability is named as a point on the second
dimension rather than a third kind, and the Senior section closes on the quadrant the three-way
comparison hides — a fact that is both current state and durable is the ordinary case, with this
repository's studied-lesson record named as the local example.

**Acceptable loss.** Requirement C is the worked case, and the Lesson states the rule directly:
*loss is a defect only when it violates the requirement*. Two further examples are named as
correctly lossy — a haptic confirmation for a button the reader is pressing, and a request to
scroll the visible list to a newly added item — and a `NOTE` callout makes the correction
explicit: "could this be lost?" is the wrong question, because for anything transient the answer
is almost always yes.

**State that sounds like an occurrence.** Four are given: *there is an unfinished practice
session*, *the last submission failed*, *this borrowing request is awaiting approval*, *the
account needs another confirmation step*. Each became true at a moment and each is current state,
because the requirement is that a screen opening later knows the current position. The Lesson
states the correction in the form that generalises: **the past occurrence is not the important
representation; the current consequence is.**

**The duplicate-execution failure.** The blueprint's `showCongratulations` case ships as authored
Kotlin — a `ResultUiState` with a boolean set when the score is computed — followed by the three
ways a second consumer meets it: an owner surviving a configuration change, a destination still on
the back stack, or the value read back from a durable record written in service of Requirement B.
The diagnosis is deliberately precise and is stated as a refusal of the usual one: the defect is
**not** that a boolean was used, it is that an occurrence was recorded as persistent current truth
**without any rule for what "already handled" means**. The Lesson explicitly declines to solve it
with an occurrence wrapper and hands the decision to L6.2, where the guarantee decides.

**Current state as lossy history.** The Senior section uses `score = 8 / 10` and lists what the
value cannot answer — which answer was changed first, how long question six took, whether the
score was recomputed after a correction, how many intermediate values existed. The conclusion is
that a late consumer can be correct about state and cannot be correct about history, that no
retention setting changes this because retention keeps recent values rather than the sequence, and
that a product needing history has a **new requirement** which must name the occurrences worth
preserving as recorded facts. Event sourcing is refused in the same paragraph, in one clause, as an
architecture with consequences this curriculum does not teach.

### L6.2: the guarantee questions, and what answers them

The four questions ship in the issue's order, as a bulleted instrument, with their independence
stated: may the occurrence be lost if nobody is listening; must a consumer arriving later still act
on it; is repeated handling safe; must the application record that it was handled.

**Process survival is a qualification, not a fifth universal question.** It is asked only when the
second is answered yes — *must that obligation survive the process ending?* — and its role is
stated as eligibility: every coroutine scope, hot stream, state holder and ViewModel is in memory,
so a yes removes in-memory ownership from consideration before any mechanism is compared.

**The backgrounded-consumer scenario.** A reader submits a borrowing request for a title on hold;
the library confirms it a minute later while the app is backgrounded and the screen is gone; the
product promises that the next time they open the application they see the confirmation and that it
is acknowledged once. All five questions are answered for it.

**The acceptable-loss contrast is in the same table**, which is what makes it acceptance-critical
rather than decorative: *scroll the visible list to the newly added loan* is answered on the same
five rows, coming out lost-able, with no later consumer, harmless to repeat and with nothing to
record. The Lesson says in as many words that the transient design the Compose curriculum already
supplies is the correct and complete answer for it, and that giving it a durable record and an
acknowledgement would be over-architecture in the precise sense L6.4 defines.

**Replay and emission are applied, not re-derived.** Replay: a retention window changes what a
later subscriber can recover and changes nothing else — not that anybody handled the value, not
that it was handled once, not that it survived the process, not that the intended consumer received
it; a collector created after recreation is simply a new subscriber. Emission: an emission call
reports what the transport did under its current configuration, and on an unbuffered shared flow
with no subscriber it reports success while the value is gone in the same instant, which is a
property of the transport operation rather than evidence that the requirement was satisfied. Both
conclusions are E24's, cited rather than re-measured, and no buffer mechanics are re-taught.

**The acknowledgement model.** Acknowledgement ships as **state somebody owns**: a value with two
distinguishable positions — outstanding and dealt-with — and a component responsible for moving it.
The authored code block is a two-position enum with three questions beside it (which component is
authoritative, what is its lifetime, who may move it) and an explicit comment that it is *not a
queue and not a delivery mechanism*. The Lesson then states what creates none of that: a stream
having accepted a value, and a retention cache, because "most recent" is not "outstanding". The
architectural question it ends on is **who owns the pending-or-handled truth**, which is why L6.3
follows it immediately.

**Android's state-reduction guidance, verified from the current page.** `developer.android.com/topic/architecture/ui-layer/events`
was re-opened during authoring and its wording is unchanged from what E26-01 recorded on
2026-09-15. Quoted in the Lesson: that for one-off events carried by streams, "when the producer
(the ViewModel) outlives the consumer (Compose UI), these solutions don't guarantee the delivery
and processing of those events"; the instruction to "Handle such events immediately and reduce them
to UI state"; and the reason, that "UI state better represents the UI at a given point in time, it
gives you more delivery and processing guarantees". The page's worked shape — the UI notifying the
owner once the message is shown, "causing another UI state update to clear the `userMessage`
property" — is the same pending-or-handled pair, and the Lesson says so. **The reason is taught,
not just the recommendation:** a current value is readable by a consumer arriving at any moment and
a past emission is not, which is why reduction answers the second and fourth questions together.
The Lesson then bounds it explicitly — this is a conclusion from a requirement rather than a
universal rule, and the acceptable-loss column stays transient.

**Exactly-once caution.** The Senior section separates three claims a single sentence tends to
merge: recording application handling state, transport delivery, and side-effect execution — and
names the window in which they come apart, being explicit about which way round it falls. If the
process ends *after* the external action has been performed and *before* the record of it has been
written, the action happened and the application does not know it did; the obligation still reads as
outstanding, so whatever resumes it performs the action again. The Lesson states that this is the
ordinary reason a retry duplicates a request — a missing record of an execution that succeeded,
rather than a failure to execute. The phrasing the Lesson prefers is stated
outright: *the application can record whether this obligation remains pending, and a consumer can
read that record*. General exactly-once processing is named as outside this curriculum, with a
one-clause pointer to where stronger properties actually come from (an identity the external system
recognises, which is a data-ownership and integration decision).

**Process death.** Named carriers, stated as losing everything with the process: `StateFlow`,
`SharedFlow`, `Channel`, a state holder, a ViewModel. The ViewModel documentation is quoted for the
lifetime claim and for saved state being a *separate* mechanism, and both are handed to the
persistence and lifecycle curricula rather than taught.

### The stream-slogan correction, and the mechanism boundary

No slogan of the form "one stream type is for state and another for events" appears anywhere in the
Unit, and the correction is made structurally rather than by assertion. L6.2's Core section ships
the required order as a fixed-width block — requirement → guarantees → owner/lifetime → mechanism —
beside the rejected one, *something called an event → a stream type*. Its Senior section adds the
general form: a mechanism describes delivery, a requirement describes handling, and no description
of delivery answers a question about handling — **which is why the Unit never compares two stream
types at all**: both lose everything they hold when the process ends, so a yes to the
process-survival question decides the comparison before it begins. A no does **not** make the stream
types interchangeable, and the Lesson says so: the four remaining guarantees are what replay,
buffering, conflation and single-receiver against broadcast delivery actually decide, the streams
curriculum owns which contract provides which, and the mechanism becomes a detail only once every
guarantee that applies is satisfied — the last step of the sequence rather than a way of skipping
it. This is the E25 handoff row "`Channel` against `SharedFlow` as a
ViewModel event design decision" answered as the plan intended — reframed as the guarantee question
and deliberately **not** answered as a type comparison.

**The queue and event-bus boundary is stated inside the Lesson, not only in this plan.** L6.2's
Senior section names what it is refusing to build — an application-wide event bus, a durable queue
with consumers and offsets, a delivery protocol with retries and dead-letter handling, an outbox
tying the record and the external call into one transaction — says each is a real subject with real
trade-offs, and says the decision this Lesson teaches is complete when you can name the missing
guarantee, the fact that would have to exist and who would own it. No infrastructure is taught.

### L6.3: the lifetime ladder

The ladder ships as a fixed-width block with each rung's **ending event** beside it, because the
ending event is what makes a rung right or wrong: a single UI element's owner ends when the element
leaves the UI; a screen or destination owner ends when the destination is permanently gone; an
application-lifetime owner ends when the process ends; something that outlives the process ends when
the work is actually done. The Lesson states in as many words that **moving down the ladder is not
an upgrade**, and that "this matters a lot, so put it higher up" is importance wearing a lifetime's
clothes. The decision rule is the awkward one: *what is the shortest lifetime that still satisfies
the requirement?*

Four requirements from one feature — the borrowed-items screen — are placed on it, and the
screen-level owner is shown to be wrong in **both** directions on that one screen.

| Rung | The requirement used | Why that rung |
| --- | --- | --- |
| A UI element's own owner | Whether the filter sheet is expanded | Meaningful only while the sheet is present, and should reset when it returns. Hoisting it into the screen holder is priced in three concrete ways: it outlives the sheet, the holder's public surface grows, and unrelated parts of the screen gain write access |
| A screen or destination owner | The loaded loans, the current filter selection, an in-flight refresh | Produced for this destination and stops mattering when it is permanently gone. Cancelling the refresh on leaving is the correct outcome rather than a loss |
| An application-lifetime owner | The studied-lesson and progress projections several screens share | Several live destinations render one truth and a change in one must be visible in the others; per-destination copies would show stale screens above a fresh one |
| Something outliving the process | The submitted borrowing request | The reader was told it was submitted, so it must complete whether or not they stay, whether or not the app is foregrounded, and whether or not the process is alive |

**`AppCoroutineScope`, re-verified.** `shared/src/commonMain/kotlin/.../assessment/history/AppCoroutineScope.kt`
was re-read during authoring and is unchanged: a distinct type delegating to
`CoroutineScope(SupervisorJob() + Dispatchers.Default)`, documented as existing because the caches
built on it "are shared by several screens and survive a navigation entry being destroyed, so they
cannot belong to a `viewModelScope`", and named distinctly "so injecting it is unambiguous". The
five non-ViewModel state holders that receive it are unchanged, and `StudyProgressStateHolder`'s
KDoc still states the source-of-truth relationship the Lesson leans on. **The caveat ships with the
evidence**: the Lesson says this application is local-first with no network layer and that these
holders are in-memory projections of a database that remains authoritative, and states plainly that
**application scope is not the generic answer**.

**Application-scope cost** is a five-item list rather than the word "complexity": retention for the
life of the process, staleness with no ending event to invalidate it, hidden coupling between
screens connected only through it, global mutable state acquired one defensible step at a time, and
reset semantics that have to be written because no ending event will do them. A `COMMON_MISTAKE`
callout names the specific failure the rung invites — moving work up because it was being cancelled
— and states that application scope is a deliberate lifetime rather than an escape from
cancellation.

**The background-work handoff** is made from the current source. Android's background-work overview
was re-opened, and two sentences carry the boundary: asynchronous work "is not guaranteed to finish
if the app stops being in a valid lifecycle stage", and the task-scheduling APIs are "a more
flexible option when you need to do tasks that need to continue even if the user leaves the app".
The Lesson concludes that an in-process coroutine owner is not enough and that the requirement
leaves the scoping question entirely, then stops: **no scheduler, no constraints, no retry or
backoff policy and no OS execution rules are taught.**

**Source-sensitive ViewModel claim.** Unit 6 makes exactly one, in L6.3's Senior section, and it is
the Unit 2 model preserved rather than restated: a ViewModel has no lifetime of its own, it has its
`ViewModelStoreOwner`'s. The ViewModel overview was re-opened during authoring and quoted — "A
`ViewModel` remains in memory until the `ViewModelStoreOwner` to which it is scoped disappears" —
together with its three named endings (an activity finishing, a navigation entry removed from the
back stack, a composable leaving the composition). The Lesson explicitly refuses the shorthand "the
ViewModel survives the screen", hands the full ladder of events back to
`lesson_viewmodel_lifetime_and_persistence`, and **makes no `viewModelScope` dispatcher claim at
all**, because that is `lesson_owner_scoped_work`'s and repeating it here would have been a
re-derivation. Navigation ownership is used to describe the destination and flow rungs and no
Navigation API, graph setup, back-stack mechanic or decorator is named.

### L6.4: proportionality, worked twice

The two features are in the same application and **both were already introduced by shipped
Lessons**, which is why L6.4 can apply the earlier Units rather than re-establishing them.

**Feature A — the reminder-time setting**, the feature `lesson_when_a_domain_layer_earns_its_place`
uses as its no-domain-layer case. Its requirements are stated in full and are short: load one
current preference, let the reader change it, store it through one source. The shipped structure is
a screen state holder, a data contract and its implementation — and the Lesson lists what the
feature **deliberately does not have**, with the reason for each: no domain layer (none of the four
conditions is present), no use case forwarding a single repository call, no reducer or action type
(one transition, and it is an assignment), no occurrence mechanism (nothing must be delivered to
anybody), and no second representation of a time of day (both sides have one reason to change
between them). The Core section states before either example that this is a **complete and correct**
architecture rather than a first draft to be grown out of.

**One thing Feature A does take care about is not structure**, and it was added during review: the
preference write has to commit, because the reader was shown the new value, so it belongs to an owner
that will not be cancelled halfway through it — the owner-scoped-work test answered without adding a
single type. The element table's last row says the same thing, that what the write needs is an
uncancelled owner rather than a further rung of the ladder. An earlier draft said instead that
"nothing is promised if it does not" complete, which contradicted the feature's own stated
requirement and would have left the smallest sufficient architecture failing a promise it makes.

**Feature B — the borrowing checkout**, a committed workflow in the same library application. Five
requirements are stated, and each buys exactly one piece of structure: a real eligibility rule with
three callers earns a domain operation; data responsibilities with independent sources and freshness
questions earn more than one repository boundary; invariants that must hold across every change earn
an explicit transition; a submitted fact the reader was told about earns a durable
pending-or-confirmed record (L6.2); and a submission promised to complete regardless of the reader
leaving earns an owner above the screen (L6.3). A three-column table traces each element to the
requirement that earns it **and** to why it is absent from Feature A.

**Over-architecture, counted.** Feature A is given Feature B's structure and the cost is listed as
things rather than as a word: two classes that forward, a mapping between two representations that
change together so every field lands in three places, an action type and a transition for a single
assignment, a delivery mechanism with no delivery requirement, and a directory named for a
responsibility the feature does not have. The Lesson names the last as the only cost that compounds
— structure is a claim about where things are, and a false claim misdirects every later reader.

**Under-architecture, as missing owners.** Feature B is given Feature A's structure and the Lesson
states that what is missing is not classes but owners: the eligibility rule duplicated into three
screen owners so a policy change has three homes; source policy leaking upward into the holder; the
invariants distributed across callbacks so "not submittable until validated" holds only where
somebody remembered; the confirmation as a transient emission although a later consumer was
promised; and the submission in the screen's scope, cancelled on leaving. A `COMMON_MISTAKE`
callout refuses the count-based reading of both failures and notes that a feature can have both at
once.

**Pattern choice at synthesis level.** The Senior section opens by noting which question was never
asked — whether either feature is MVVM or MVI — and states the reason: the decisions actually made
are ownership, mutation path, transition explicitness, lifetime, delivery and durability, and
dependency direction, and the resemblance to a pattern is a *description of the result* rather than
an input to it. Unit 5's classification model is preserved intact.

**The uniformity trade-off is taught fairly and then named.** Four concrete benefits are given —
placement known before reading, one shape for a reviewer to check, fewer local decisions for new
engineers, and conventions tooling can assume — and the Lesson says outright that a team choosing
this is not confused and that the indirection may be worth paying. The senior addition is the
distinction between two sentences: "every feature here has a domain layer because we chose
consistency over per-feature proportionality, and we pay two forwarding classes on the small ones
for it" is an engineering decision stated as a trade; "every feature needs a domain layer" is the
same structure with the reasoning removed. **No uniform architecture is framed as bad.**

**No rubric.** The Lesson refuses one explicitly and gives the reason rather than the rule: any
threshold would have added the mapping layer to the reminder setting and would have been perfectly
happy with the under-architected checkout, because neither mistake is visible in a count.

**Unit 1 closure.** The final section asks the foundations Unit's questions of both designs — what
responsibility exists, who owns it, what may depend on what, what lifetime each part requires, what
each boundary costs and whether it is paid for — and answers all five for each feature. The closing
paragraph states the subject's answer to "what architecture should I use": the smallest arrangement
in which every responsibility has an owner, every dependency points somewhere defensible, every
lifetime matches a requirement, and every boundary was paid for by an independent change it
isolates — and notes that both features satisfy it while looking nothing like each other.

### Semantic review of the Questions this Unit now reaches

All four ACTIVE Questions in the resolved pool were re-read in full and independently solved against
the finished prose, together with the duplication guards listed below. **The E26-01 findings in
[Part 5](#part-5--semantic-assessment-review) all still hold.** The `architecture` Topic still holds
22 ACTIVE and 4 DEPRECATED Questions over 18 Subtopics, re-counted from the bundled JSON during
authoring.

| Question | Level | Reached through | Re-read verdict against the shipped Lessons |
| --- | --- | --- | --- |
| `durable_state_vs_one_off_event` | APPLIED | `state_ownership` (L6.1) | **Sound and precisely matched to L6.1 — the only one of the four that semantically assesses what this Unit teaches.** Its correct option is the state-against-consumable decision L6.1 is built on, and its explanation's warning that replaying transient behaviour as durable state can duplicate the effect is exactly the `showCongratulations` failure. **Its scenario and wording are not reproduced**: the Question is a payment result against a snackbar, and the Lesson uses a scored practice session against a celebration, reaches the duplicate execution through an authored code block rather than through a type comparison, and never poses the four-way mechanism choice its options do. A reader who understood L6.1 can answer it without having met it |
| `state_ownership_001` | FOUNDATION | `state_ownership` (L6.1–L6.3) | Sound, and answerable from this Unit although it is Unit 2's. Its point — one owner decides how state changes and publishes it read-only — is Unit 2's contract; what Unit 6 adds is the aside in its explanation, that ownership is separate from durability, which L6.1's two dimensions teach directly. Structurally reachable here because the Subtopic is shared; semantically it belongs to Unit 2 |
| `architecture_state_holder_taxonomy` | APPLIED | `state_ownership` (L6.1–L6.3) | Sound. **Partly matched, and the part that matches is L6.3's.** Its correct answer — a plain remembered holder for a component's own state, rather than a ViewModel that would give it a lifetime and a dependency surface it never asked for — is the first rung of L6.3's ladder and the over-hoisting failure L6.3 prices in three ways. Its framing is still Unit 2's state-holder taxonomy, and L6.3 was not reshaped towards it: the Lesson reasons from ending events rather than from holder kinds. See GAP-U6-B |
| `viewmodel_activity_reference_lifetime` | FOUNDATION | `state_ownership` (L6.1–L6.3) | Sound, and Unit 2's. It assesses why a ViewModel must not retain a UI instance, which L2.2 owns. L6.3 touches the adjacent fact — that a ViewModel's lifetime is its `ViewModelStoreOwner`'s — and deliberately does not re-derive the retention argument. Structural reach only |

Adjacent and cross-topic Questions were re-read as duplication and mapping guards, and **none was
re-mapped**: `architecture_ui_event_consumption` (`mvi`, see below);
`stream_choice_cannot_supply_a_delivery_guarantee` and `shared_flow_try_emit_true_is_not_delivery`
(`async_reactive`, both ADVANCED) — L6.2 applies both conclusions and re-asks neither, and neither
reaches this Unit; `viewmodel_scope_cleared_cancellation`, `coroutine_scope_outlives_its_consumer`
and `coroutine_scope_job_ownership` (`async_reactive`) — the mechanism half of L6.3's ladder, owned
by E24 and by `lesson_owner_scoped_work`; `background_api_selection_criteria` (`background_work`) —
exactly the decision L6.3 stops before, and the reason it stops there;
`navigation_back_stack_entry_lifetime`, `navigation_graph_viewmodel_shared_scope` and
`viewmodel_destination_scope` (`lifecycle_navigation`) — the destination and flow rungs from the
navigation side, which L6.3 describes and does not assess; and `realtime_messages_persist_then_render`
and `system_design_pagination_state_ownership` (`mobile_system_design`) — L6.2 and L6.3 reasoning
under a system-design framing, left where they are.

**No Question was created, edited, re-mapped, re-levelled or re-statused by this issue**, no taxonomy
entry was invented, and no factual defect was found in any Question read.

### `architecture_ui_event_consumption`: the disposition E26-08 needs

Re-read in full against the finished L6.2, as the issue requires. **The E26-01 and E26-06 conclusion
is confirmed, and the finished Lesson strengthens it rather than merely leaving it standing.**

- **The Question's reasoning is L6.2's, line for line.** A replay cache hands the event to the
  collector created after recreation because "the stream has no notion of an event having been acted
  on"; handling once "requires the consumption to be recorded where it survives the collector"; and
  its explanation names the architecture guidance's conclusion that such events are modelled as UI
  state the UI clears once handled. Those are L6.2's replay conclusion, its acknowledgement model and
  its state-reduction section, in that order.
- **L6.2 was not shaped towards it.** The Lesson's worked scenario is a borrowing confirmation
  arriving while the app is backgrounded, not a navigation event after a configuration change; it
  never poses the Question's four options; and its distinctive content — the five questions, the
  pending-or-handled owner, the exactly-once separation — is broader than the Question asks.
- **A reader who has finished L6.2 can answer it, and a reader who has finished Unit 5 cannot** from
  Unit 5 alone, which is what E26-06 recorded.
- **It still routes to Unit 5.** Its Subtopic is `mvi`, so it is the pattern Unit's practice and
  reaches Unit 6 not at all. **No re-map was performed here**, per the issue's instruction.

**If E26-08 moves it to `state_ownership`:** it would enter Unit 6's practice — and also Unit 2's,
since the two Units share that Subtopic — and `mvi` would become empty, which makes GAP-U5-C
obligatory rather than optional. On GAP-U6-A specifically, the move would close **most but not all**
of it; what would remain is recorded in the gap row below.

### GAP-U6-A to GAP-U6-C after authoring

All three gaps survive the finished prose. **None was closed, none was found wrong, and no new gap
was created.** A gap is not declared closed because a structurally reachable Question exists, and
not preserved merely because it was planned.

| Gap | Status after E26-07 | What the finished Lesson changes about it |
| --- | --- | --- |
| GAP-U6-A | **Open. Substantially a routing problem, exactly as the plan predicted — decide the re-map first** | L6.2 now ships the four questions, the process-survival qualification, the acknowledgement model and the state-reduction conclusion. The reasoning is assessed three times in the bank and **none of it is reachable from a Unit 6 primary**: twice in `async_reactive` (`stream_choice_cannot_supply_a_delivery_guarantee`, `shared_flow_try_emit_true_is_not_delivery`) and once under `mvi` (`architecture_ui_event_consumption`). Moving the last of those to `state_ownership` would close most of the gap. **What it would leave unassessed, stated precisely:** (1) answering the four questions for a requirement whose correct answer is *acceptable loss* — every existing Question poses the must-not-be-lost case, so nothing assesses recognising that a transient occurrence may legitimately disappear and that a durable mechanism would be over-architecture; (2) the process-survival qualification as the *eligibility* test that removes in-memory ownership from consideration, as opposed to the fact that streams die with the process, which the two `async_reactive` Questions already assess; and (3) the exactly-once separation between recorded handling state and side-effect execution. E26-08 should decide the re-map first and then author only for what remains |
| GAP-U6-B | **Open, unchanged; the Unit's clearest candidate for new authoring** | L6.3 now ships the whole ladder with each rung's ending event, one owner wrong in both directions on one screen, and the application-scope cost list. `architecture_state_holder_taxonomy` still assesses only the bottom rung, and it does so as a holder-kind question rather than as a lifetime selection. The "even the screen owner is too short-lived" half is assessed only in `background_work` (`background_api_selection_criteria`) and `async_reactive` (`viewmodel_scope_cleared_cancellation`), neither reachable from a Unit 6 primary. Nothing makes a reader *select* an owner from a stated requirement |
| GAP-U6-C | **Open, unchanged; the widest gap in the Unit and the last one in the epic** | L6.4 now ships the two-feature comparison, both wrong designs with their costs counted, the uniformity trade-off and the closing loop. `architecture_tradeoffs` still holds **zero** ACTIVE Questions, so the Lesson the whole epic ends on contributes nothing to practice — confirmed by running the production resolver, not by reading the mapping, and asserted in a test. The distinction from GAP-U1-E holds after authoring and is now visible in the prose: L1.5 asks whether **one boundary** earns its mapping and forwarding cost, and L6.4 asks whether a **whole feature's** structure is proportionate, with the failure it names being missing owners rather than an excessive layer count. One Question cannot serve both |

One observation is recorded for E26-08 without being promoted to a gap: **nothing assesses the
current-versus-durable dimensional distinction** that L6.1 exists to install — that durability is a
retention requirement placed on state rather than a third kind of thing.
`durable_state_vs_one_off_event` assesses the state-against-occurrence half and takes the durability
of the payment result as given. It may belong inside a GAP-U6-A Question rather than as one of its
own.

### Actual practice reach, resolved through the production resolver

Recomputed by running the shipped Unit through `PracticeBuilderViewModel` and the real selection path
in `LearningUnitPracticeIntegrationTest`, not by reading mappings. The Unit configures
`AssessmentScope.Subtopics` of its two primary concepts and resolves **four** Questions:

| Resolved Question | Level | Reached through | Semantically belongs mainly to |
| --- | --- | --- | --- |
| `state_ownership_001` | FOUNDATION | `state_ownership` | **Unit 2** — structural reach only |
| `architecture_state_holder_taxonomy` | APPLIED | `state_ownership` | **Unit 2**, with one rung of L6.3's ladder inside it |
| `durable_state_vs_one_off_event` | APPLIED | `state_ownership` | Unit 6 (L6.1) |
| `viewmodel_activity_reference_lifetime` | FOUNDATION | `state_ownership` | **Unit 2** — structural reach only |

This matches [Part 6](#part-6--unit-practice-routing-modelled-now)'s modelled pool exactly — four
Questions, the same four ids — so E26-01's prediction is confirmed rather than assumed. The level
split is two FOUNDATION and two APPLIED with **no ADVANCED Question**, matching the Topic-wide
observation in Part 5. **Only one of the four semantically assesses what this Unit teaches**, which is
the epic's least favourable ratio — worse than Unit 5's two of four — and it is the reason GAP-U6-A
and GAP-U6-B matter more than their peers.

Three further structural facts, all asserted in tests rather than inspected:

1. **`architecture_tradeoffs` contributes nothing.** Two primary concepts resolve four Questions, and
   the concept the epic's closing Lesson teaches reaches none. Asserted by subtopic id.
2. **Unit 6's pool is a strict subset of Unit 2's**, and the containment is total: every Question this
   Unit reaches is also in the state-holder Unit's pool, because all four arrive through the one
   Subtopic both Units take as primary. The test computes Unit 2's pool and asserts the intersection
   rather than assuming it. The consequences the plan predicted all hold — Unit 2 meets
   `durable_state_vs_one_off_event` before Unit 6 teaches its full reasoning; Unit 6 receives three
   Questions whose semantic home is Unit 2; and Unit 6 receives no `architecture_tradeoffs` Question.
   **No mapping was changed to relieve this**, which would require splitting `state_ownership` and is a
   question-bank decision.
3. **Supporting concepts broaden nothing.** The Unit's fourteen supporting-only concepts contribute
   nothing to the pool. The stream and lifetime bridges matter most — `sharedflow`,
   `hot_vs_cold_streams`, `stateflow`, `process_death`, `viewmodel_lifecycle`, `lifecycle_coroutines`,
   `coroutine_scope` and `background_api_selection` — because this Unit applies their conclusions and
   teaches none of their mechanisms, so a promotion would claim coverage for material it deliberately
   does not carry. The four structural concepts L6.4 names without teaching — `use_cases`,
   `repository_pattern`, `layered_architecture`, `clean_architecture` — are the other half of the same
   discipline: the closing Lesson asks whether a whole feature's structure is proportionate, not
   whether any one of those structures is correct.

**No shipped Unit's practice changed.** Unit 1's pool is unchanged at five, Unit 2's at five, Unit 3's
at six, Unit 4's at five and Unit 5's at four; no shipped Lesson takes an `architecture` Subtopic as
primary, so nothing this Unit maps can reach an earlier curriculum's Unit.

### Cross-links

Backward only, and every target already shipped. L6.1 → `lesson_transient_ui_effects`,
`lesson_transient_effect_delivery`, `lesson_state_flow`, `lesson_state_holder_responsibility`,
`lesson_single_source_of_truth`; L6.2 → L6.1, `lesson_transient_effect_delivery`,
`lesson_shared_flow`, `lesson_choosing_a_stream_abstraction`, `lesson_single_source_of_truth`;
L6.3 → L6.2, `lesson_observable_or_one_shot_api`, `lesson_owner_scoped_work`,
`lesson_viewmodel_lifetime_and_persistence`, `lesson_coroutine_scope_ownership`; L6.4 → `lesson_layers_and_their_cost`,
`lesson_state_holder_responsibility`, `lesson_what_a_repository_owns`,
`lesson_when_a_domain_layer_earns_its_place`, `lesson_classifying_a_real_architecture`, L6.2 and L6.3.

The plan's two explicit requirements for this Unit are met. L6.1 and L6.2 between them link to all
four named E24/E25 anchors, and L6.3 links to Unit 2's owner-scoped-work Lesson and to E24's scope
ownership Lesson. **L6.4 links backward to at least one Lesson from each of Units 1–5**, and the links
chosen are the ones the two-feature design actually uses rather than a complete set: the layers Lesson
because over-architecture is priced the way that Lesson prices a boundary, the state-holder Lesson
because both features have one, the repository Lesson because Feature B's several boundaries are its
decision applied more than once, the domain-layer Lesson because Feature A is its own worked
counterexample and Feature B meets its four conditions, and the classification Lesson because the
Senior section's refusal to start from a pattern name is what preserves it. That claim is asserted in
a test by resolving each target to its owning Unit rather than by listing ids. **No shipped Lesson was
edited**, no earlier Unit received a reciprocal link, and — this being the last instructional Unit —
**no forward link exists anywhere in the document**.

### E25 handoff rows Unit 6 owns, re-checked

Re-read against the shipped prose. E26-09 owns the complete deferral-by-deferral check; this is the
subset [Part 3](#part-3--the-e25--e26-handoff-ledger) assigns to Unit 6.

| Deferred by E25 | Status after E26-07 |
| --- | --- |
| Work whose required lifetime exceeds the Composition, and what owner it needs | **Answered instructionally by L6.3.** The ladder places the requirement, names the rung, and hands the process-surviving mechanism to the background-work curriculum |
| Application-level occurrence modelling | **Answered by L6.1 and L6.2.** L6.1 classifies; L6.2 states the guarantee and names the owner of the record |
| Durable against transient architecture | **Answered by L6.1 and L6.2**, and made two-dimensional rather than two-valued: durability is a retention requirement on state, and the transient case is preserved as legitimate |
| `Channel` against `SharedFlow` as a ViewModel event design decision | **Answered as the plan intended: reframed as the guarantee question and deliberately not answered as a type comparison.** L6.2 states why the comparison is decided before it begins |
| The consumable-event-channel against acknowledged-state trade-off; acknowledgement and consumption | **Answered by L6.2** as a pending-or-handled fact with a named owner, together with the exactly-once caution that keeps the claim honest |
| The full production pipeline chosen proportionally | **Answered by L6.4**, end to end, on two features in one application, with both wrong designs priced |
| Queueing architecture | **Still deferred, and excluded by design.** L6.2 names the machinery it is refusing to build and says why. This is not a gap |
| Dependency construction and injection | **Still deferred. E27's**, untouched by this Unit: no Lesson names a DI framework, a module or a graph |
| `SavedStateHandle` as a state-production mechanism | **Still deferred.** L6.2 names saved state once, quoting the ViewModel documentation, as evidence that durability is a *separate* mechanism from retained lifetime. **No API is taught**, and this Unit does not claim to teach it |

### Tests changed, and why

| File | Change | Why production data made it necessary |
| --- | --- | --- |
| `BundledLearningCurriculumTest` | Unit id, title and home-Topic lists extended by one; the four-Lesson id/title order and the Unit's primary mappings added; two new tests — `theSynthesisUnitKeepsItsPlannedBridgesOutOfPrimaryPractice` and `theSynthesisUnitLinksBackwardsAndClosesTheLoopOverEveryEarlierArchitectureUnit` | The document lists Units positionally, so a sixth architecture Unit changes four lists. The bridge test exists because every one of this Unit's fourteen supporting concepts is owned and assessed by another curriculum, so a promotion would be invisible and would claim that curriculum's practice. The link test pins the plan's one explicit link requirement by resolving each of the closing Lesson's targets to its owning Unit, and asserts backwardness positionally rather than against a hand-written anchor list |
| `LearningUnitPracticeIntegrationTest` | The traversal now expects six architecture Units and `listOf(5, 5, 5, 5, 5, 4)` Lessons, and its final studied-record count rises from 96 to 100; new test `theSynthesisUnitPractisesASubsetOfTheStateHolderUnitAndNothingForItsClosingConcept` | Continue Learning walks the whole document, so a sixth architecture Unit changes the traversal and the Topic's progress denominator. The Unit **cannot** join the shared expectation table, which asserts `concepts == resolved subtopics`: `architecture_tradeoffs` holds no ACTIVE Question, so the identity fails exactly as it does for Units 1 and 5. The bespoke test therefore pins the four ids, asserts that `architecture_tradeoffs` contributes none, computes the containment in Unit 2's pool rather than assuming it, and asserts that the delivery Question L6.2 actually teaches is not reachable here |

**The tests record structural reality and do not endorse it.** The containment assertion, the empty
`architecture_tradeoffs` assertion and the `architecture_ui_event_consumption` exclusion are all
commented as such: they exist so that if E26-08 re-maps a Question or splits a Subtopic the assertion
fails and the consequence has to be re-stated, rather than the mapping being quietly repaired. No test
was added that only re-states schema validation `LearningCurriculumValidatorTest` already performs, and
the data-driven suites — the reader journey over every shipped Unit, Topic Detail's Unit rows, the
end-to-end repository path — needed no edit because they read the document rather than listing it. No
prose is snapshotted anywhere.

### Validation performed

| Command | Result |
| --- | --- |
| `python3` structural pre-check over both bundled JSON documents | Ids unique across the whole document, every mapping an ACTIVE Subtopic, no primary/supporting overlap within a Lesson, every `relatedLessonIds` target resolvable and non-self, no blank or placeholder text, every comparison row matching its header count, every Source URL well-formed, no new widest table and no code line over 86 characters. No defect in the new Unit |
| `./gradlew :shared:jvmTest --tests "*BundledLearningCurriculumTest*" --tests "*LearningUnitPracticeIntegrationTest*" --tests "*LearningCurriculumValidatorTest*" --tests "*LearningContentEndToEndTest*"` | **107 tests, 0 failures** |
| `./gradlew :shared:jvmTest` | **1,402 tests, 0 failures**, including `LearningProductionContentJourneyTest`, which renders every authored block of the new Unit in the reader, checks the reading column never widens, and opens every authored Source link through the app's own URI boundary |
| `python3 tools/learning_question_coverage.py --write` then `--check` | Snapshot regenerated and reported current |
| `cd tools && python3 -m unittest test_learning_question_coverage.py` | 21 tests, OK |
| `./gradlew :shared:check` | Passed — jvm, Android host, JS and wasmJs test targets included |
| `./gradlew :androidApp:assembleDebug` | Passed |
| `git status --short` and `git diff --check` | Six files changed — the bundled learning document, two jvm test files, this plan, the generated coverage snapshot and the blueprint's one-paragraph status correction — with no build or cache output and no whitespace defects |

The regenerated `docs/content/learning-question-coverage.md` now reports **24 active Units and 101
active Lessons**, the new Unit appearing directly after Unit 5 with four Lessons, a pool of four
Questions across two levels and no ADVANCED Question. **Primary Subtopics with no active Question
remains two** — `architecture_tradeoffs` (GAP-U1-E and GAP-U6-C) and `mvc` (GAP-U5-A) — which is the
generated confirmation that the closing Lesson of the epic reaches no practice at all.

### Not validated

- **`iosArm64` is not compiled locally or on CI**, unchanged from E25 and from every E26 issue. Unit 6
  introduces no target-specific code, and its one source-sensitive ViewModel claim is scoped to the
  `ViewModelStoreOwner` contract rather than to a target; the limitation is reported because it is
  still true of the epic.
- **No CI run is claimed.** Nothing in this issue was observed on GitHub Actions, and no merge is
  claimed.
- **Backlog validation could not be run**: `PyYAML` is unavailable in this environment, so
  `.github/project/backlog.yml` was read as text rather than parsed and validated. Issue #367 was read
  from that file rather than through `gh`, which is still not installed.
- **The learning content itself is editorial** and no automated check can confirm that a Lesson teaches
  what it claims; the semantic review above is a judgement, as Rule 10 of the authoring contract
  requires.

---

## Assessment outcomes for E26-08

The epic's semantic assessment pass, run after all six instructional Units shipped. It repeats
[Part 5](#part-5--semantic-assessment-review)'s review against the finished 29 Lessons rather than
against the plan, decides the mapping and level candidates, disposes of all 21 gap candidates, and
records the practice shape the learner actually meets. It changed the question bank, this document,
the audit log, the generated coverage snapshot and two test files. **It changed no Lesson, no Unit
identity, no Lesson mapping and no taxonomy entry** — no Lesson factual defect was found while
validating against the shipped prose, and the routing limitations this issue ran into are recorded
below rather than repaired with a Subtopic split this epic was not authorised to make.

### Scope, and the baseline it started from

Re-derived from the bundled JSON rather than trusted to the prompt or to Part 5.

| Measure | Before E26-08 | After E26-08 |
| --- | ---: | ---: |
| Questions in the bank | 442 | 460 |
| ACTIVE | 401 | 419 |
| DEPRECATED | 41 | 41 |
| `architecture` Questions | 26 | 44 |
| `architecture` ACTIVE | 22 | 40 |
| `architecture` DEPRECATED | 4 | 4 |
| `architecture` Subtopics | 18 | 18 |
| `architecture` ACTIVE by level | 8 F / 14 A / 0 Adv | 7 F / 31 A / 2 Adv |
| `architecture` ACTIVE Subtopics with no Question | 2 (`mvc`, `architecture_tradeoffs`) | **0** |
| Unique Questions reachable through the six E26 Units | 21 | 39 |

Eighteen Questions were authored, two were re-mapped, one was re-levelled. **No Question was edited
for content, deprecated, restored or had an `AnswerOption` id reissued.** Every changed Question kept
its `Question.id`, all of its `AnswerOption` ids, its key, its status and its explanation.

### The mapping and level candidates, decided first

Decided before anything was authored, because a re-map changes which gaps remain. That ordering is
what turned GAP-U6-A from an authoring problem into mostly a routing one, and what made GAP-U5-B and
GAP-U5-C obligatory rather than optional.

| Question | Before | After | Reason | Practice consequence |
| --- | --- | --- | --- | --- |
| `architecture_ui_event_consumption` | `mvi` | **`state_ownership`** | Its reasoning is replay, consumption and acknowledgement — that a replay cache re-delivers to a late collector because the stream records no consumption, so handling once requires the consumption to be recorded where it survives the collector. That is L6.2 line for line, and it is not MVI's transition model. Three independent reviews (E26-01, E26-06, E26-07) reached the same conclusion, the last of them against the finished prose | Leaves Unit 5 and enters Unit 6 — and **also Unit 2**, because the two Units share `state_ownership` as a primary concept. `mvi` became empty, which made GAP-U5-C obligatory |
| `viewmodel_vs_repository_responsibility` | `mvvm` | **`repository_pattern`** | Three of its four options turn on what the data layer owns — no stored entity may cross the boundary, cache and retry policy belongs inside — which L3.1 and L3.2 own; L5.3 teaches none of it. `repository_pattern` was chosen over the `state_ownership` alternative because it keeps the Question out of Unit 6, which teaches no repository material, and puts it in a Unit whose readers have met the state-holder half already, where a Unit 2 reader would not yet have met the repository half | Leaves Unit 5 and enters **Unit 3 only**. `mvvm` became empty, which made GAP-U5-B obligatory |
| `architecture_paging_ownership` | `layered_architecture` | **keep** | Re-checked against the finished L1.5 and L3.5 and the recommendation stands. It is a strong fit in Unit 3 and a weak one in Unit 1, and no honest alternative mapping is better: the reasoning genuinely is which layer owns pagination. The real fix is a taxonomy split, which this epic does not make | Unchanged: still reaches Units 1 and 3. The Unit 1 weakness is an accepted structural limitation, recorded since E26-01 and re-recorded here |
| `dependency_direction_domain_framework_types` | `FOUNDATION` | **`APPLIED`** | Set from the difficulty rubric, not from the Topic's level shape. The reader must read a signature, recognise two different kinds of framework coupling in it and name the consequence, while eliminating three plausible mechanical consequences that are each false for a different reason. That is known behaviour applied to a realistic scenario rather than one documented contract recalled. **Not ADVANCED**: no two mechanisms interact and no trade-off is weighed | None. Its Subtopic is unchanged, so it still reaches Units 1 and 4; only the level a learner sees changed |

### Final disposition of the 21 gap candidates

Eighteen Questions close nineteen gaps; two gaps are closed without one. **No gap disappeared
silently.**

| Gap | Disposition | Question(s), or the reason there is none |
| --- | --- | --- |
| GAP-U1-A | **Closed by new Question** | `architecture_package_move_changes_nothing` (APPLIED, `separation_of_concerns`). A package reorganisation that changes no import, and the reader has to say what it changed. Deliberately a concrete change rather than "which of these is architecture?" |
| GAP-U1-B | **Closed by new Question** | `separation_of_concerns_reason_to_change_test` (APPLIED, `separation_of_concerns`). A proposed split that is **not** justified, keyed on one reason to change reaching both halves. Kept distinct from U1-D and U1-E by scale: this is cohesion inside one component, not what an abstraction buys or what a layer costs |
| GAP-U1-C | **Closed by new Question** | `dependency_direction_callback_does_not_reverse_it` (APPLIED, `dependency_direction`). Keyed on what a source dependency is and what the depending side inherits, using L1.3's callback case. Deliberately **not** an inversion Question, and deliberately not a second framework-leak Question, so it does not duplicate `dependency_direction_domain_framework_types` |
| GAP-U1-D | **Closed by new Question** | `interface_with_one_implementation_is_not_a_boundary` (APPLIED, `interface_boundaries`). Asks *whether* the abstraction earns existence; `architecture_interface_boundary_ownership` asks *where* it belongs. The two responsibilities are kept apart, as the gap required |
| GAP-U1-E | **Closed by new Question** | `added_layer_must_isolate_an_independent_change` (APPLIED, `architecture_tradeoffs`). Evaluates **one boundary**. Kept distinct from GAP-U6-C, which evaluates a whole feature; one Question was deliberately not stretched across both |
| GAP-U2-A | **Covered sufficiently elsewhere; no new Question** | The lifetime-against-persistence fact is assessed four times in `lifecycle_navigation` and once in `kmp`, and its architecture-side form — that moving state to a longer-lived owner changes the owner and grants no durability — is now assessed by `owner_chosen_from_the_required_lifetime`, whose third requirement names an ending no in-memory owner has. A further architecture Question would have duplicated five existing ones and added a sixth Unit-6-flavoured Question to Unit 2's pool. **E25's GAP-U7-C, handed forward to this epic, is closed here on the same reasoning** |
| GAP-U2-B | **Closed by new Question** | `ui_state_shape_from_the_screens_requirements` (APPLIED, `state_ownership`). Two stated constraints decide it — a region that fails independently, and a combination the product forbids — so the reader chooses a shape from requirements rather than preferring a construct. The epic's widest gap, and nothing in any Topic assessed it |
| GAP-U2-C | **Closed by new Question** | `exposed_mutable_state_costs_a_second_write_path` (APPLIED, `unidirectional_data_flow`). Keyed on the three architectural costs, with the `MutableStateFlow` thread-safety claim as a distractor precisely so the Question is not about stream-type trivia. Unit 2 only, so it adds nothing to the Unit 6 overlap |
| GAP-U2-D | **Covered sufficiently elsewhere; no new Question** | `viewmodel_scope_cleared_cancellation` in `async_reactive` assesses exactly this reasoning including the upload conclusion, and the ownership decision the gap wanted — *does this work belong to this owner?* — is now posed by `owner_chosen_from_the_required_lifetime`'s third requirement. E26-01 and E26-03 both recorded this as the lowest-priority gap in the Unit, and the duplication would have been paid for twice: once in the bank and once in Unit 2's pool |
| GAP-U3-A | **Closed by new Question** | `repository_boundary_needs_a_decision_to_own` (APPLIED, `repository_pattern`). Asks which requirement would give a forwarding repository a decision of its own, with the uniform-entry-point recommendation as a distractor that is real but is not a decision. **Neither retired wording is reproduced**, and the retired Questions' framing — what a repository *is* — is deliberately not the question asked |
| GAP-U3-B | **Closed by new Question** | `repository_contract_carries_meaning_not_origin` (APPLIED, `repository_pattern`). Keyed on the application-meaning against implementation-origin distinction that E26-04 named as L3.2's sharpest testable idea. Not combined with U3-A: one asks whether the boundary owns anything, the other asks what its contract may expose |
| GAP-U3-C | **Closed by new Question** | `authoritative_owner_is_chosen_per_fact` (APPLIED, `single_source_of_truth`). Three facts, three different owners, one of which is not a database and one of which is not stored at all. The offline-first recommendation appears as a distractor quoted without its condition, which is how the misconception is actually formed |
| GAP-U4-A | **Closed by new Question** | `domain_layer_is_earned_by_the_feature` (APPLIED, `use_cases`). Posed at the **layer** level, where the two existing `use_cases` Questions work at the class level. E26-05's team-uniformity observation is carried in the explanation rather than as a second decision, and `architecture_tradeoffs_001` was used as input rather than restored. **Not** folded into GAP-U6-C: that Question is `architecture_tradeoffs` and would reach Units 1 and 6, never Unit 4, so folding would have left Unit 4 with nothing at the layer level |
| GAP-U4-B | **Closed by new Question** | `dependency_rule_constrains_direction_not_layer_count` (APPLIED, `clean_architecture`). Three designs — two layers compliant, four groupings compliant, three layers non-compliant — exactly as the gap specified, and not a terminology quiz |
| GAP-U5-A | **Closed by new Question, shared with GAP-U5-D** | `classify_a_screen_by_its_responsibilities` (APPLIED, `mvc`). Fills the Subtopic that held zero ACTIVE Questions through six issues. The assessed skill is responsibility-based classification, and no pattern definition is asserted, so its correctness does not depend on one author's taxonomy |
| GAP-U5-B | **Closed by new Question** | `observed_state_arrangement_is_not_a_class_or_a_folder` (APPLIED, `mvvm`). Obligatory once `viewmodel_vs_repository_responsibility` moved. Keyed on the change that actually establishes the arrangement, with the class-name and folder-name definitions as distractors; the arrangement is stated in the stem rather than deduced from the label |
| GAP-U5-C | **Closed by new Question** | `explicit_transition_is_not_the_input_spelling` (APPLIED, `mvi`). Obligatory once `architecture_ui_event_consumption` moved. Assesses the transition model with the "MVI is MVVM plus sealed intents" misconception as the central discrimination and the purity over-claim as a fourth option. **Nothing about `SharedFlow`, `Channel` or occurrence delivery**, which was the mapping error being corrected |
| GAP-U5-D | **Closed by the GAP-U5-A Question** | The design `classify_a_screen_by_its_responsibilities` presents is a hybrid — a lifecycle-owned holder from one tradition, one immutable state value from another, callbacks from a third — and its three distractors each differ on exactly one property, which is the classification skill the gap describes. A separate hybrid Question would have given Unit 5 one Question per Lesson, which is the quota this issue was told not to author |
| GAP-U6-A | **Mostly closed by the re-map; the remainder closed by one new Question** | The re-map delivers `architecture_ui_event_consumption` to Unit 6. `occurrence_guarantee_before_mechanism` (ADVANCED, `state_ownership`) closes what E26-07 said the re-map would leave: an occurrence whose correct answer is **acceptable loss**, where a durable mechanism would be over-architecture, with process survival as the eligibility test that removes in-memory owners and the recorded-handling against side-effect-execution distinction carried by the fourth option. Its requirement is stated as a readable pending-or-acknowledged record rather than as exactly-once handling, which is the limit L6.2 says an architecture can actually promise. **No second replay Question was written**, as the issue required |
| GAP-U6-B | **Closed by new Question** | `owner_chosen_from_the_required_lifetime` (APPLIED, `state_ownership`). Three rungs of the ladder from three stated ending events, with the application-scoped owner as the tempting longer-lived answer that fails on process death. Also carries the architecture-side half of GAP-U2-A and GAP-U2-D |
| GAP-U6-C | **Closed by new Question** | `smallest_structure_that_satisfies_the_requirements` (ADVANCED, `architecture_tradeoffs`). The epic's synthesis Question: one feature's requirements, four designs, with under-architecture, over-architecture and pattern-first as the three wrong answers. Its key names work that outlives the process rather than L6.4's shorthand "an owner above the screen", so that it agrees with `owner_chosen_from_the_required_lifetime` and with L6.3's rung. Distinct from GAP-U1-E, whose Question judges one boundary |

**No gap was left open**, and no new gap was created. One observation E26-04 recorded was weighed and
deliberately not promoted: *nothing assesses the model-separation trade-off in the direction that
favours one shared type.* It is not authored, because `dto_entity_domain_model_boundary`'s own key
names the mapping cost and its explanation says the split "may be unnecessary for very small
features", and because a new `layered_architecture` Question would also enter Unit 1, where the data
layer is not yet taught. Two further observations — E26-05's dependency-inversion-against-injection
distinction, and E26-06's orthogonality claim — are left to E27 and to whichever issue owns the
cross-epic view, exactly as those issues recommended; both are now touched by distractors in
`interface_with_one_implementation_is_not_a_boundary` and `classify_a_screen_by_its_responsibilities`
respectively, which is coverage of the misconception without a Question claiming the whole idea.

### The two ADVANCED levels, justified one at a time

The Topic held **zero** ACTIVE ADVANCED Questions before this issue and now holds two. Neither was
levelled to fix that, and sixteen of the eighteen new Questions are APPLIED.

- **`occurrence_guarantee_before_mechanism`.** Two occurrences have to be answered against five
  independent guarantee questions in the right order; the process-survival answer eliminates a whole
  class of owners before any mechanism is considered; the correct answer requires recognising that a
  durable design is *wrong* for one of them; and the last distractor turns on *when* the record is
  written rather than on whether one exists. Several constraints interact and a superficially safer
  answer is invalidated by one of them.
- **`smallest_structure_that_satisfies_the_requirements`.** Six stated requirements each earn or fail
  to earn a structural element, and the three wrong answers fail in three different directions —
  missing owners, boundaries with no independent change behind them, and a mechanism chosen before the
  questions were asked. Judging it needs every condition the previous five Units established, applied
  at once.

Both are long, and length is not why they are ADVANCED: `classify_a_screen_by_its_responsibilities`
has the longest options in the batch and is APPLIED, because its four readings are settled by reading
the described design carefully rather than by combining anything.

### Practice routing after every change, through the production resolver

Recomputed by running each shipped Unit through `PracticeBuilderViewModel` and the real selection path
in `LearningUnitPracticeIntegrationTest`, not derived from Subtopic membership. Every pool below is
asserted by id in that test.

| Unit | Pool | Levels | Questions |
| --- | ---: | --- | --- |
| 1 | **11** | 1 F / 9 A / 1 Adv | `separation_of_concerns_001`, `architecture_package_move_changes_nothing`, `separation_of_concerns_reason_to_change_test`, `dependency_direction_domain_framework_types`, `dependency_direction_callback_does_not_reverse_it`, `architecture_interface_boundary_ownership`, `interface_with_one_implementation_is_not_a_boundary`, `architecture_paging_ownership`, `dto_entity_domain_model_boundary`, `added_layer_must_isolate_an_independent_change`, `smallest_structure_that_satisfies_the_requirements` |
| 2 | **10** | 3 F / 6 A / 1 Adv | `state_ownership_001`, `architecture_state_holder_taxonomy`, `durable_state_vs_one_off_event`, `viewmodel_activity_reference_lifetime`, `ui_state_shape_from_the_screens_requirements`, `architecture_ui_event_consumption`, `occurrence_guarantee_before_mechanism`, `owner_chosen_from_the_required_lifetime`, `unidirectional_data_flow_001`, `exposed_mutable_state_costs_a_second_write_path` |
| 3 | **10** | 1 F / 9 A / 0 Adv | `repository_observable_api_shape`, `repository_boundary_needs_a_decision_to_own`, `repository_contract_carries_meaning_not_origin`, `viewmodel_vs_repository_responsibility`, `single_source_of_truth_001`, `authoritative_owner_is_chosen_per_fact`, `architecture_paging_ownership`, `dto_entity_domain_model_boundary`, `architecture_error_mapping_boundary`, `architecture_error_modeling_result_type` |
| 4 | **9** | 0 F / 9 A / 0 Adv | `architecture_use_case_reuse`, `domain_layer_passthrough_cost`, `domain_layer_is_earned_by_the_feature`, `clean_architecture_dependency_rule_tradeoff`, `dependency_rule_constrains_direction_not_layer_count`, `dependency_direction_domain_framework_types`, `dependency_direction_callback_does_not_reverse_it`, `architecture_interface_boundary_ownership`, `interface_with_one_implementation_is_not_a_boundary` |
| 5 | **5** | 1 F / 4 A / 0 Adv | `classify_a_screen_by_its_responsibilities`, `mvp_vs_mvvm_view_contract`, `observed_state_arrangement_is_not_a_class_or_a_folder`, `explicit_transition_is_not_the_input_spelling`, `architecture_mvi_single_state` |
| 6 | **10** | 2 F / 6 A / 2 Adv | `state_ownership_001`, `architecture_state_holder_taxonomy`, `durable_state_vs_one_off_event`, `viewmodel_activity_reference_lifetime`, `ui_state_shape_from_the_screens_requirements`, `architecture_ui_event_consumption`, `occurrence_guarantee_before_mechanism`, `owner_chosen_from_the_required_lifetime`, `added_layer_must_isolate_an_independent_change`, `smallest_structure_that_satisfies_the_requirements` |

**Union: 39 of the 40 ACTIVE architecture Questions.** The one outside every pool is still
`architecture_solid_dependency_substitution`, because `solid` is supporting-only by design, and the
exclusion is asserted by id. **Every primary concept of every Unit now reaches at least one Question**,
so the identity between a Unit's concepts and its resolved Subtopics holds for all six Units for the
first time, and the three tests that previously asserted an *absence* now assert that identity.

### Routing consequences, recorded rather than repaired

| Overlap | Size | Verdict |
| --- | ---: | --- |
| Unit 1 ∩ Unit 4 | **4** | `dependency_direction_domain_framework_types` and `architecture_interface_boundary_ownership` complete their reasoning in Unit 4 and were already shared. The two new ones, `dependency_direction_callback_does_not_reverse_it` and `interface_with_one_implementation_is_not_a_boundary`, are Unit 1's own reasoning and were written deliberately simpler than the inversion Questions, so the shared pool is now fair in **both** directions rather than only one. This is an improvement on the pre-E26-08 shape and is the reason neither concept was demoted |
| Unit 1 ∩ Unit 3 | **2** | Unchanged: `architecture_paging_ownership` and `dto_entity_domain_model_boundary` through `layered_architecture`. No new Question was authored under that shared concept, precisely to avoid widening it |
| Unit 1 ∩ Unit 6 | **2** | New, and the predicted cost of filling `architecture_tradeoffs`. `added_layer_must_isolate_an_independent_change` is fair after Unit 1 and useful to Unit 6 as prerequisite reasoning. `smallest_structure_that_satisfies_the_requirements` is **not fair after Unit 1** — it is the epic's synthesis Question and needs all six Units. The two gaps were kept as two Questions rather than forced into one, which is what the plan required; the premature Unit 1 reach is an accepted structural limitation that only splitting `architecture_tradeoffs` would remove |
| Unit 2 ∩ Unit 6 | **8** | The epic's largest limitation, and it grew. All eight `state_ownership` Questions reach both Units, and **four of them require Unit 6 material a Unit 2 reader has not met**: `durable_state_vs_one_off_event` (already recorded), the re-mapped `architecture_ui_event_consumption`, and the two new GAP-U6-A and GAP-U6-B Questions. That was accepted deliberately: the alternative was leaving the Unit the epic closes on with one semantically matched Question, and the issue's instruction is not to weaken Unit 6 so that Unit 2 stays pure. It is also why GAP-U2-A and GAP-U2-D were **not** given duplicate Questions — each would have added a ninth and tenth to this overlap for reasoning already assessed elsewhere |
| Unit 5 ∩ anything | **0** | Still zero, and now for a better reason. Before this issue Unit 5's five concepts resolved four Questions of which two belonged elsewhere; now they resolve five, one per concept, every one of them assessing what the Unit teaches |

**No shipped Unit outside this epic changed.** No non-`architecture` Subtopic was touched, no Lesson
mapping moved, and the eighteen new Questions are all in the `architecture` Topic, so the Compose and
Coroutines Units' pools are byte-identical.

### Cross-topic duplication review

Every adjacent Question E26-01 named was re-read before authoring, and none was re-mapped into
`architecture`. The guards that actually constrained authoring:

- **`viewmodel_scope_cleared_cancellation`, `coroutine_scope_outlives_its_consumer`,
  `coroutine_scope_job_ownership`** (`async_reactive`) — the reason GAP-U2-D received no Question, and
  the reason `owner_chosen_from_the_required_lifetime` is posed as an owner-selection decision across
  three rungs rather than as a fact about when a scope is cancelled.
- **`stream_choice_cannot_supply_a_delivery_guarantee`, `shared_flow_try_emit_true_is_not_delivery`**
  (`async_reactive`, both ADVANCED) — the reason `occurrence_guarantee_before_mechanism` names no
  stream type at all and keys on the acceptable-loss case, which neither of them poses.
- **`viewmodel_store_configuration_retention`, `viewmodel_clear_owner_finish`,
  `viewmodel_destination_scope`, `configuration_change_vs_process_recreation`, the four `saved_state`
  Questions, `kmp_shared_viewmodel_owner_platform`** — together the reason GAP-U2-A received no
  Question.
- **`flow_one_shot_result_vs_observable_stream`** (`async_reactive`) — re-weighed as E26-04 asked. No
  architecture-side duplicate was authored: `repository_observable_api_shape` already assesses the
  API-shape decision from this side, and the residual idea, that exposing a mutable stream from the
  data layer hands every caller a second write path, is assessed by
  `exposed_mutable_state_costs_a_second_write_path` at the owner boundary where L2.4 teaches it.
- **`compose_screen_state_lowest_sensible_owner`, `compose_over_hoisted_ui_element_state_cost`,
  `compose_udf_event_direction`** (`android_ui`) — the reason the new UDF and owner-selection Questions
  are posed at the application boundary, with a repository, a process and a scheduler in scope, rather
  than inside a composable tree.
- **`offline_first_001`, `cache_invalidation_staleness_policy`, `room_dao_001`** (`local_data`) — the
  reason `authoritative_owner_is_chosen_per_fact` makes the learner *choose* owners for three facts
  instead of restating the offline-first recommendation, which `offline_first_001` already covers.
- **`background_api_selection_criteria`** (`background_work`) — the reason
  `owner_chosen_from_the_required_lifetime`'s fourth rung is "a mechanism whose contract outlives the
  process" rather than a named scheduler. The Question stops exactly where L6.3 stops.
- **`kotlin_sealed_types_001`** (`kotlin_language`) — the reason
  `ui_state_shape_from_the_screens_requirements` asks about representable combinations and never about
  the language mechanism.
- **`over_modularization_tiny_module_cost` and the `build_delivery` module Questions** — the reason the
  Gradle-module option in `added_layer_must_isolate_an_independent_change` is a distractor about
  enforcement rather than a claim about modularization, which E29 owns.
- **`service_locator_vs_di_001`, `di_hilt_viewmodel_scope`** (`dependency_injection`) — the reason
  injection appears only as a distractor in `interface_with_one_implementation_is_not_a_boundary` and
  no Question was authored on inversion against injection, which E26-05 recommended leaving to E27.

### Source verification

Fifteen pages were fetched and the supporting sentence located in the rendered text; none was trusted
to an HTTP 200. `developer.android.com` supplied the architecture overview, recommendations, UI layer,
state holders, UI events, data layer, offline-first, domain layer, ViewModel and background-work pages;
`kotlinlang.org` supplied the `StateFlow` thread-safety contract and the visibility-modifier rule that
makes the package-boundary distractor false; `blog.cleancoder.com` supplied the Dependency Rule's exact
wording and the schematic-circles statement, and the single-responsibility restatement.

**`blog.cleancoder.com` was added to `APPROVED_SOURCE_HOSTS`** in
`InitialCurriculumContentQualityTest`, which is the first non-vendor host in that list. It is the
primary source for the two claims it is cited for, no secondary page is authoritative for either, and
this plan's own Part 9 and the shipped Lessons already cite the same two articles for the same claims.
The test carries a comment saying it is not a precedent for citing practitioner blogs generally.

Seventeen of the eighteen new Questions are `VERIFIED`. **`explicit_transition_is_not_the_input_spelling`
is `PARTIALLY_VERIFIED`**, recorded honestly rather than rounded up: the UI-layer page settles the UDF
cycle the Question is built on, but no vendor page states the transition-explicitness property its key
turns on, and no source is normative for MVI. The stem therefore defines the property it is asking
about rather than resting on a pattern definition, which is the epic's own source contract applied
rather than a workaround for a missing citation.

### Independent solving

Every one of the eighteen new Questions was solved from its stem and options alone before
`correctAnswerIds` was read, then every option was tested for defensibility under the stem as written.
That pass changed two Questions before they shipped, both in a distractor rather than in a key:

- **`dependency_direction_callback_does_not_reverse_it`** — the "a dependency cycle does not build"
  distractor is false inside one compilation unit and true across build modules, and the first stem
  named no module, so the option was defensible under one reasonable reading. The shipped stem states
  that both classes are ordinary classes in the same module.
- **`occurrence_guarantee_before_mechanism`** — a draft distractor named the same structure as the key
  and differed only in what it claimed about it, so two options described one design. It was replaced
  with a genuinely different design — a durable record the screen writes *after* acting — which is
  wrong for a concrete reason L6.2 teaches.

**Review found two more, and both were the same defect: a stem demanding a guarantee no option could
deliver.** They are recorded because the pattern is worth carrying forward — in both, the key was
fine as a design and the *requirement* was overstated.

- **`occurrence_guarantee_before_mechanism`** — the stem asked that the confirmation be shown
  "once". No architecture supplies exactly-once execution, and this Question's own explanation said
  so, so the key did not satisfy its own stem and the fourth option described the key's structure
  with a different claim attached. The shipped stem asks for what L6.2 says a design can actually
  promise: that the next screen shows the confirmation, and that the application can tell at any
  moment whether it has been acknowledged. The distractor now fails on the stated requirement,
  because a record written only after acting is missing for something already acted on.
- **`smallest_structure_that_satisfies_the_requirements`** — the same over-claim, plus a key that
  named "an owner above the screen" for a requirement that outlives the process. That is exactly what
  `owner_chosen_from_the_required_lifetime` keys on being insufficient, **in this same batch**, so the
  two Questions contradicted each other and a reader who answered the first correctly had grounds to
  reject the second. The key now names work that outlives the process. L6.4 uses the shorthand "an
  owner above the screen" for this element and the Question deliberately does not reproduce it,
  because L6.3 is the authority on the rung and is explicit that surviving the process is not a
  scoping decision at all.

Both Questions were still unshipped when they were changed, so no `AnswerOption` id was reissued for
the one option whose claim narrowed and no historical attempt can reference it.

The three re-mapped and re-levelled Questions were re-solved unchanged and needed no edit; the other
nineteen ACTIVE architecture Questions were re-read whole and re-solved against the shipped prose, and
none was found defective. The playbook's length audit was run over the batch, two keys were trimmed and
one distractor lengthened so no key exceeds its longest distractor by more than 10%, and the eighteen
keys land on four different option positions.

### Tests changed, and why

| File | Change |
| --- | --- |
| `InitialCurriculumSmokeTest` | The pinned bank shape: 460 questions, 419 ACTIVE, 413 SINGLE, the global and per-Topic level distributions, and `architecture` from 22 to 40 ACTIVE. These are the counts a batch must not change silently |
| `InitialCurriculumContentQualityTest` | `blog.cleancoder.com` added to `APPROVED_SOURCE_HOSTS`, with the reason and the limit of the precedent in a comment. The duplicate-stem, length, absolute-word and `MULTIPLE`-prompt gates are unchanged and pass over the new Questions |
| `LearningUnitPracticeIntegrationTest` | All six architecture Unit pools re-pinned by exact id, the three counts in the shared expectation table updated, and the three tests that asserted an *absence* — `mvc` empty, `architecture_tradeoffs` empty in two Units — rewritten to assert the concepts-to-Subtopics identity instead. The Unit 1 ∩ Unit 4, Unit 1 ∩ Unit 6 and Unit 2 ∩ Unit 6 overlaps are asserted as sets, and both re-mapped Questions are asserted present in their new Units and absent from Unit 5 |
| `CurriculumLocalDataPathTest` and `CurriculumImporterTest` | The imported row counts, which are a second independent pin on the same bundle: 460 questions, 1,846 answer options, 510 correct answers and 604 question sources. They are listed separately because they are what proves the new Questions survive the import path rather than only the parser |

No test was written whose only purpose is to preserve something this issue fixed, and the assertions
that recorded the old mismatches were deleted rather than inverted-and-kept: a future re-map still
fails a pinned pool and still has to re-state its consequence.

### Validation performed

| Command | Result |
| --- | --- |
| `./gradlew :shared:jvmTest --tests '*InitialCurriculum*' --tests '*CurriculumValidator*'` | Passed — structural validation and every bundled-bank content gate |
| `./gradlew :shared:jvmTest --tests '*LearningUnitPracticeIntegrationTest*' --tests '*LearningCurriculumValidatorTest*' --tests '*BundledLearningCurriculumTest*'` | Passed |
| `python3 tools/learning_question_coverage.py --write` | Rewrote `docs/content/learning-question-coverage.md`, 2126 lines |
| `python3 tools/learning_question_coverage.py --check` | Snapshot current |
| `cd tools && python3 -m unittest test_learning_question_coverage` | 21 tests, OK |
| `./gradlew :shared:jvmTest` | Passed |
| `./gradlew :shared:check` | Passed — jvm, Android host, JS and wasmJs test targets included |
| `./gradlew :androidApp:assembleDebug` | Passed |
| `git status --short` and `git diff --check` | Nine files changed — the bundled question bank, five jvm test files, this plan, the audit log and the generated coverage snapshot — with no build or cache output and no whitespace defects |

The regenerated `docs/content/learning-question-coverage.md` reports **24 active Units, 101 active
Lessons, 419 ACTIVE questions and 41 DEPRECATED**, with **130 unique active questions reachable
through primary mappings** and — for the first time in the project — **zero primary Subtopics with no
active question**, across the whole learning curriculum rather than only this epic.

### Not validated

- **`iosArm64` is not compiled locally or on CI**, unchanged from E25 and from every E26 issue. This
  issue changes shared JSON data and JVM test sources only and introduces no target-specific code, so
  no platform-specific claim is made about it; the limitation is reported because it is still true of
  the epic.
- **No CI run is claimed.** Nothing here was observed on GitHub Actions, and no merge is claimed.
- **Backlog validation could not be run**: `PyYAML` is unavailable in this environment, so
  `.github/project/backlog.yml` and `docs/content/question-audit-log.yml` were read and edited as text
  rather than parsed. The audit log's structure was checked by inspection against the eight existing
  entries, and no historical entry was rewritten. Issue #368 was read through the GitHub REST API with
  `curl`, because `gh` is not installed.
- **Source liveness was verified by fetching and reading each cited page during this session**, which
  is stronger than the playbook's liveness loop, but the loop itself was not re-run over the whole
  bank and no fragment-anchor sweep was performed — none of the eighteen new citations uses a
  `#fragment`.
- **Whether each Question is genuinely answerable from the shipped prose is a judgement**, not an
  automated result. The semantic review above records it as one, and the routing tests prove only
  which Questions a learner reaches, never that a Lesson teaches enough to answer them.

### What E26-09 inherits

An assessment layer that is finished. Every Unit has meaningful practice, every primary concept
reaches a Question, and the three limitations that remain are structural rather than editorial: the
Unit 2 ∩ Unit 6 containment, the premature Unit 1 reach of the synthesis Question, and
`architecture_paging_ownership` in Unit 1. All three would be removed by splitting `state_ownership`
and `architecture_tradeoffs`, which is a question-bank taxonomy change and belongs to whichever issue
is authorised to make one. **E26-09 is not started by this issue.**

## Closure outcomes for E26-09

Verified on 2026-09-17 against the production bundles after E26-01 through E26-08. Every figure below
was derived from `learning_curriculum.json`, `initial_curriculum.json`, the production resolvers and
the repository's own source, rather than carried forward from the sections above. Where a claim was
only inspected rather than executed, this section says so.

### Final curriculum shape

The `architecture` Topic contains **6 Units / 29 Lessons**, all ACTIVE, in this production order:

1. `unit_architecture_responsibilities_and_boundaries` — Architecture as Responsibilities and Boundaries — 5 Lessons
2. `unit_screen_state_holders_and_ui_state` — Screen State Holders, ViewModel and UI State — 5 Lessons
3. `unit_repositories_and_data_ownership` — Repositories, Data Ownership and Single Source of Truth — 5 Lessons
4. `unit_domain_logic_and_dependency_direction` — Domain Logic, Use Cases and Dependency Direction — 5 Lessons
5. `unit_responsibility_models_mvp_mvvm_mvi` — MVP, MVVM and MVI Responsibility Models — 5 Lessons
6. `unit_state_events_lifetime_and_selection` — State, Events, Lifetime and Architecture Selection — 4 Lessons

The whole learning document ships **24 active Units / 101 active Lessons** — 12 Units / 43 Lessons in
`android_ui`, 6 / 29 in `async_reactive`, 6 / 29 in `architecture`. All 101 Lesson ids are unique, every
Unit and Lesson is ACTIVE, and every Unit and Lesson identity matches the one this plan reserved. The
issue's expected numbers were derived rather than trusted and **no discrepancy was found**. The
authored order is pinned by exact Lesson id *and* exact Lesson title for all six Units in
`BundledLearningCurriculumTest`, and independently by the six Unit ids and the `5, 5, 5, 5, 5, 4`
Lesson counts in `LearningUnitPracticeIntegrationTest`.

E26 authored **680 blocks** — 457 paragraphs, 84 callouts, 67 code blocks, 40 bullet lists and 32
comparisons — carrying **79 Source references over 28 distinct URLs**.

### Cross-Unit semantic review

All 29 Lessons were read in final learner order as one continuous curriculum rather than as six
batches. The sequence is one argument and each transition was checked for available prerequisites,
compatible terminology, contradiction, and re-teaching rather than application.

- **Unit 1 → Unit 2.** L2.1 opens by naming the previous unit's test and applying it — "Ask the
  question that unit built: what is that thing responsible for, what does it own, how long must it
  live, and what does having it cost?" — and derives five responsibilities *before* naming a class.
  Responsibility and boundary reasoning leads into choosing a screen-state owner rather than into a
  framework type: "the name for it is a **screen-level state holder**", and `ViewModel` arrives as
  "one platform-provided implementation" chosen by lifetime. Nothing in Unit 2 re-derives cohesion,
  dependency direction or boundary cost.
- **Unit 2 → Unit 3.** Kept distinct, and stated in both directions rather than once. L2.1's Senior
  section closes the Unit's scope explicitly — "this responsibility is about *presentation state*,
  not about what is true for the application … A state holder that quietly acquired a caching policy
  has not become more powerful; it has acquired a second reason to change." L3.3 completes the pair
  and names both violations: "A repository that starts deciding what the fine's label says when it is
  zero has taken on a rendering decision … a state holder that starts deciding when the cached fine
  is too old to show has taken on a source policy that the next screen will implement differently."
  Screen-state ownership and authority over application data are never conflated.
- **Unit 3 → Unit 4.** The domain boundary grows from responsibilities and stays optional. L4.1 opens
  from the state Unit 3 left the feature in — "That is two owners with one boundary between them" —
  and asks "does a third owner belong in the middle?", calling it "the first one in this subject whose
  honest answer is often no". The four earning conditions are evidence rather than a quota, and the
  Lesson works a feature with none of them, a feature with all four, and an honest middle case whose
  verdict is "not yet" with the observable evidence that would change it.
- **Unit 4 → Unit 5.** Orthogonal, and said so twice. L4.5 closes the Unit by handing over without
  collapsing the axes — "MVP, MVVM and MVI describe the same reasoning in three competing
  vocabularies". L5.5's Senior section states the separation directly: the patterns "are **not
  alternatives to Clean Architecture** … They answer different questions at different scales, so a
  codebase can perfectly coherently have inward-pointing dependencies in the Clean Architecture
  sense, an MVVM-style presentation arrangement, and MVI-style explicit transitions inside some of
  its state owners." Dependency direction is never presented as a pattern choice, and no pattern is
  presented as deciding a layer.
- **Unit 5 → Unit 6.** Unit 6 stops reasoning from labels and starts from the requirement. L6.1 opens
  "with a product requirement and nothing built" and runs the four-step order — represent, guarantee,
  lifetime, proportion. L6.4's Senior section makes the break explicit: "Notice which question was
  never asked: whether either feature is MVVM or MVI. That is deliberate … the resemblance is a
  **description of the result** rather than an input to it." L6.2's whole content is the ordering
  requirement → guarantees → owner → mechanism, stated against the inversion it replaces
  ("something called an event → a stream type").

**Refinements are marked as refinements.** L2.4 re-states unidirectional data flow at the application
boundary and says why rather than assuming: "This is the same rule one level out … and the reasons are
not identical, which is why it is worth stating again rather than assuming." L5.3 tells the reader they
have already built the arrangement — "the state-holder unit designed an owner that … did so without
using the word MVVM once" — so the Lesson adds vocabulary rather than design. L6.1 applies the Compose
state/occurrence contrast explicitly "rather than re-deriving it". No later Lesson contradicts an
earlier one.

### Terminology audit

Audited across all 29 Lessons together, by reading and by phrase sweep over the 951 text units
(summaries, paragraphs, callouts, bullets, code, table rows and headers). Every term the issue names is
used consistently.

- **Responsibility** stays "something a component has a reason to own and change for". L1.2 fixes it
  as "a statement about why the code would have to change", and it is never reduced to a class, a
  package or a framework type — L4.2 rejects all four of the usual proxies in one list (a suffix,
  `operator fun invoke`, a base class, a folder), and L1.1 and L5.3 each refute the folder reading with
  two trees and one architecture.
- **Ownership** is used in four contexts and every use names what is owned: screen state (L2.1, L2.4),
  authority over application data (L3.3), the owner of work and its lifetime (L2.5, L6.3), and the
  owner of a pending-or-acknowledged fact (L6.2). L3.3 and L2.1 each state the boundary between the
  first two explicitly, so no use is ambiguous about its object.
- **Lifetime** means the actual ending condition of the owner. L2.2 refuses the shorthand by name —
  "the sentence that causes the trouble is 'the ViewModel survives the screen', and the reason it causes
  trouble is that it names the wrong subject" — and grounds every answer in `ViewModelStore` /
  `ViewModelStoreOwner`. L6.3 repeats the correction rather than relaxing it: a ViewModel "has the
  lifetime of its `ViewModelStoreOwner`". **No Lesson regresses to "the ViewModel survives the
  screen".**
- **Persistence** stays separate from lifetime everywhere. L2.2: "**In-memory retention across a
  host's recreation is not persistence, and the two are separated by exactly one event.**" L6.2 lists
  every in-memory carrier that dies with the process — `StateFlow`, `SharedFlow`, `Channel`, a state
  holder, a ViewModel — and L6.3 states that an application scope "still ends with the process". No
  Lesson implies any of them is durable for outliving a UI owner.
- **State** consistently means something currently true, and durability is a retention dimension
  placed on it rather than a third kind. L6.1 states it as a rule — "**Durable is a point on the second
  dimension, not a third kind**" — and its Requirements A and B are the same fact at two retentions.
- **Intention / action** is used for input toward an owner. L2.4 separates it from an instruction:
  "'The reader chose the overdue filter' is a report; 'set the filter field to Overdue' is an
  instruction."
- **Occurrence** is used where delivery and handling requirements are live, and L6.1 preserves the
  distinction from current consequence: "the past occurrence is not the important representation; the
  **current consequence** is."
- **Delivery** is kept separate from handling throughout L6.2: "A mechanism describes *delivery*; a
  requirement describes *handling*; and no description of delivery answers a question about handling."
- Also checked and consistent: boundary, source of truth, repository, data source, state holder,
  ViewModel, use case, policy, detail, architecture, layer, pattern, and acknowledgement. Two are worth
  naming because a sloppier curriculum would blur them: **layer against module** is separated in L1.5,
  L4.1, L4.5 and L4.4 as logical against physical, and **source of truth against cache** is fixed in
  L3.3 ("when a cache disagrees with the source of truth, the cache is what is wrong").

### The event / effect collision

Swept mechanically as well as by reading. **"Effect" occurs five times in all 680 blocks** and not once
as a bare word for an MVI side output or an application occurrence: twice stating the collision and the
qualification rule in L5.4, once as "side effects" in the purity sense in L4.3, once as the labelled
"**Side-effect execution**" claim in L6.2, and once in the L5.4 interview callout. L5.4 states the
discipline itself — "That word is already taken in this curriculum … Whenever the MVI sense is meant
here it is qualified — an *MVI-style side output*, or *a one-off output sometimes called an effect in
MVI literature* — and the bare word is never used for either" — and the sweep confirms the rule holds
across the whole epic, including the two Units authored after it.

**"Event" occurs 25 times**, and every occurrence is one of four legitimate uses: a lifecycle or ending
event (L2.1, L2.2, L2.5, L6.3 — "what event is allowed to end it"), a verbatim quotation from a cited
source (the Android UI-events page in L6.2, Staltz in L5.4, the UDF sentence in L5.3), an explicit
rejection of the ambiguous universal category (L6.2's "something called an event → a stream type", and
its callout on slogans "of the form 'this stream type is for state and that one is for events'"), or
"a single real event in the world" in L6.1. **"Event" is nowhere introduced as a universal architecture
category**, and Compose effect APIs, MVI-style side outputs, application occurrences and
lifetime-ending events are never conflated. No wording needed correcting.

### Architectural misconception sweep

Every claim on the issue's list was searched for as a direct statement and as wording that could imply
it. All are rejected or properly qualified, and each rejection is a named correction rather than an
omission.

| Misconception | Where E26 rejects it |
| --- | --- |
| Architecture = folders / packages | L1.1 (two trees, one architecture; the `ui`/`domain`/`data` project whose imports say otherwise), L5.3, L5.5 |
| Every interface creates decoupling | L1.4 ("An interface is a language feature. A boundary is a property of an arrangement"), L4.4 |
| Every feature needs a repository | L3.1 — the reminder preference's boundary is "**none yet**", and the recommendation is quoted with its own "recommendations and not strict requirements" caveat |
| Every repository needs an interface | L3.1's four conditions, "Where none of them holds, the interface is one more file that changes in the same commit as its only implementation" |
| The database is always the source of truth | L3.3 — three facts, three owners, one of them in memory; the offline-first recommendation quoted with its condition |
| Repositories should always expose Flow | L3.4 — "'Modern repositories should expose Flow' replaces a decision with a default, and the tell is that it can be said without knowing the requirement" |
| Every feature needs a domain layer | L4.1 — optional, four earning conditions, a worked "not yet" |
| Every repository method needs a use case | L4.2 — named as "a convention presented as a rule", with the team-uniformity option priced honestly |
| Clean Architecture means three layers | L4.5 and L1.5 — the article's own "The circles are schematic … There's no rule that says you must always have just these four" |
| ViewModel means MVVM | L5.3 — "A `ViewModel` that holds a view interface is a presenter" |
| MVVM means many streams / MVI means one | L5.3 and L5.5 — falsified against designs A and C rather than asserted |
| MVI is MVVM plus sealed intents | L5.4's common-mistake callout and L5.5's worked `onAction` counterexample |
| MVI reduces recompositions | L5.4 and L5.5 — and the honest reverse: one large value "can cause a composable that reads the whole value to re-execute for changes it does not care about" |
| ViewModel provides persistence | L2.2, L6.2, L6.3 |
| Production state belongs in the ViewModel | L2.1 — "A screen with nothing left local is usually a screen that stopped asking the question and started following a convention"; L6.3's filter sheet is wrong in the other direction |
| App scope is safer because it lives longer | L2.5, L6.3 — five named costs, and "Application scope is a deliberate lifetime, not an escape from cancellation" |
| SharedFlow is for events | L6.2 — answered as a guarantee question and deliberately not as a type comparison |
| Successful emission means delivery | L6.2 — "on an unbuffered shared flow with no subscriber it reports success and the value is gone in the same instant" |
| Replay means handled once | L6.2 — "a retention window changes what a subscriber arriving later can recover, and that is all it changes" |
| A durable record guarantees exactly-once side effects | L6.2 — the three senses of "exactly once", and the window where the action happened and the record did not |
| More architecture is safer or more senior | L1.5, L4.1, L6.4 — "Over-architecture is boundaries with no independent change behind them" |

The phrase sweep found 198 text units containing an absolute ("always", "never", "every", "all
state"), and reading them confirms the pattern E25-09 recorded: each is either a refutation or a
precisely scoped claim. The single occurrence of "safer" in the epic is the refutation itself — "a
longer-lived owner is a decision with a cost rather than a safer default".

### E23 / E24 / E25 integration: applying rather than re-teaching

- **E23 — Compose state and execution.** E26 relies on state ownership, hoisting, the immutable screen
  value and UDF, and re-teaches none of the mechanics. `remember`, `rememberSaveable`, recomposition
  mechanics, snapshot mechanics and stability inference appear only as named prerequisites with a link
  back: L1.2 cites `lesson_state_hoisting` for "the full ownership argument" at a smaller scale; L2.3
  closes by stating that why an immutable value integrates cleanly with Compose "is already taught in
  the Compose curriculum; this lesson takes it as given"; L2.2 names `rememberSaveable` as the
  Composition-side saved-state mechanism and teaches no API. "Recomposition" occurs five times in the
  whole epic, and every occurrence is either a correct negative (L2.2: "Recomposition is not a lifetime
  event for anything outside the Composition") or the refutation of the MVI performance claim.
- **E24 — Coroutines and Flow.** E26 applies scope ownership, cancellation, `StateFlow`, `SharedFlow`,
  replay and the delivery limits, and re-teaches no operator, no stream mechanics, no exception
  mechanics, no dispatcher mechanics and no buffering algorithm. The deferrals are explicit: L2.4 —
  "the coroutines curriculum owns what those stream types are and how they behave"; L2.5 — "How scopes,
  jobs, structured concurrency and cancellation actually work is taught in the coroutines curriculum
  and applied here rather than re-derived"; L3.4 — "Cold and hot streams, collection, conflation, and
  the operators that turn one into the other are the coroutines and Flow curriculum's"; L6.2 — the
  replay and emission conclusions "apply immediately and are not re-derived here". What E26 adds is
  the ownership and lifetime decision those contracts feed.
- **E25 — Compose effects and production screen state.** The most important integration, and the one
  checked against E25's original wording rather than against E26's summary of it. E25's own
  "E26 owns application architecture" section and its L7.3, L7.4, L8.1, L8.3, L8.4 and L12.3
  exclusions were re-read in `docs/content/compose-units-7-12-plan.md`, and every item in them is
  disposed of in the ledger below. E25's two load-bearing conclusions are carried forward without
  weakening: `lesson_screen_state_owner_boundary` already says "Saying 'the ViewModel survives' without
  naming the event and the owner is incomplete", which L2.2 extends rather than relaxes; and
  `lesson_transient_effect_delivery`'s negative result is where L6.2 begins.

### The complete E25 → E26 handoff ledger, disposed of

Checked against E25's original text, not against Part 3's summary of it. Seventeen rows; **fourteen
fully answered at E26 scope, three still deferred with a named owner.**

| E25 deferral, as E25 worded it | Answered by | Complete at E26 scope? |
| --- | --- | --- |
| The production screen pipeline behind the owner (former L8.1) | Unit 2 (the owner), Unit 3 (the data side), **L6.4** (the whole pipeline chosen proportionally) | Yes |
| Why a screen-level owner exists at all (L7.4's stated stop) | **L2.1** — five responsibilities derived before a class is named | Yes |
| What the owner owns, receives, and must not know (L7.4 Exclude) | **L2.1** (owns, receives, must-not-know list) and **L2.4** (the public surface that makes ownership true) | Yes |
| How the owner is layered (L7.4 Exclude) | **Unit 1** and **L4.1** | Yes |
| How the owner is constructed and injected (L7.4 Exclude) | Named and deferred in L2.1 ("the dependency-injection curriculum owns it") and L4.4 | **No — E27** |
| `UiState` modelling: sealed against nullable-field data class, partial states, error representation (former L8.3, L7.3 Exclude) | **L2.3** — both shapes on one screen, the six-row requirement comparison, the nested region for partial failure, and the screen-side error | Yes |
| Application-level unidirectional data flow, distinct from the composable tree (L7.3's boundary) | **L2.4** | Yes |
| Controlled write paths and read-only exposure at the application boundary | **L2.4** — three named costs of exposing the mutable container | Yes |
| Detailed state-holder design (E26-owns list) | **Unit 2** entire | Yes |
| `viewModelScope` as the answer to "where does work that outlives the composition go" (L7.3 narrowing, "the `viewModelScope` answer is E26's") | **L2.5** — what it actually is, read from the resolved 2.11.0-beta01 sources — with the lifetime-selection half in **L6.3** | Yes |
| Work whose required lifetime exceeds the Composition, and what owner it needs (L10.3, L12.3 negative result) | **L6.3**'s four-rung ladder, and **L2.5**'s owner test | Yes at E26 scope; **background APIs remain deferred** |
| The consumable-event-channel against acknowledged-state trade-off (former L8.4) | **L6.2** — acknowledgement as state somebody owns | Yes |
| Application-level occurrence modelling (L12.3 Exclude) | **L6.1** and **L6.2** | Yes |
| Durable against transient occurrence architecture (L12.3 Exclude) | **L6.1** (retention as a dimension) and **L6.2** (the fifth question) | Yes |
| `Channel` against `SharedFlow` as a ViewModel event design (L12.3 Exclude, "**E26**") | **L6.2** — reframed as requirement, guarantee and owner first, and **deliberately not answered as a stream-type comparison**: "the argument in this lesson never needed to compare two stream types, and why comparing them is the wrong first move" | Yes, by reframing, as planned |
| Acknowledgement and queueing architecture (E26-owns list) | **L6.2** for acknowledgement | Split: acknowledgement yes; **queueing excluded, not missing** |
| `SavedStateHandle` as a state-production mechanism (E26-owns list) | Bounded context in **L2.2** only, which names it and stops | **No — lifecycle and navigation curriculum** |
| MVC, MVP, MVVM, MVI, MVVM-against-MVI, reducers | **Unit 5** entire | Yes |
| Layered architecture | **L1.5**, **L3.5**, **L4.5** | Yes |
| The repository pattern | **Unit 3** | Yes |
| Use cases | **Unit 4** (L4.1 the layer, L4.2 the individual operation) | Yes |
| Single source of truth as an architecture | **L3.3** | Yes |
| Clean Architecture | **L4.5** | Yes |
| The seven concepts as **primary** rather than supporting-only | All seven are primary in E26 and now carry Unit practice | Yes |
| GAP-U7-C — what changes when a piece of state's owner moves outside the Composition | **L2.2** teaches exactly this; E26-08 carried its architecture-side assessment on `owner_chosen_from_the_required_lifetime` | Yes |

**Reason for each remaining deferral.** Construction and injection are E27's subject and E26 teaches
inversion and ownership rather than graph assembly, so answering it here would be the scope creep the
epic goal excludes. `SavedStateHandle` is an API whose behaviour belongs with process-state
restoration; E26 needs only the architectural consequence that persistence is a separate
responsibility with a separate owner, and L2.2 is explicit that this repository "does not use
`SavedStateHandle` in production code at all, so nothing here demonstrates saved state for a screen
owner". Queue and delivery infrastructure is named and refused in L6.2 as "a different project", which
is a bounded conclusion rather than an unfinished one.

**E26 does not promise a universal stream answer anywhere else.** L3.4 refuses the same default from
the data side, and its Senior section shows this application's entirely one-shot repositories as the
counterexample.

### Cross-links

All **91** E26 `relatedLessonIds` resolve: **67 internal to E26, 15 into `android_ui` (E23 and E25),
9 into `async_reactive` (E24)**. No E26 Lesson links forward to a later Lesson anywhere in the
document. The intended graph in
[Cross-linking rules](#cross-linking-rules-and-the-intended-link-graph) is realised exactly, and each
of the six Units has its own link assertion in `BundledLearningCurriculumTest`.

The sixteen distinct shipped anchors E26 reaches are, by epic: **E23** —
`lesson_state_down_events_up`, `lesson_state_hoisting`, `lesson_remember_saveable`,
`lesson_immutability_vs_stability`; **E25** — `lesson_screen_state_owner_boundary`,
`lesson_classes_of_screen_state`, `lesson_screen_state_and_ui_events`,
`lesson_remember_coroutine_scope`, `lesson_who_owns_the_trigger`, `lesson_transient_ui_effects`,
`lesson_transient_effect_delivery`; **E24** — `lesson_coroutine_scope_ownership`, `lesson_why_flow`,
`lesson_state_flow`, `lesson_shared_flow`, `lesson_choosing_a_stream_abstraction`.

**Both directions, as issue #369 asks.** No shipped E23, E24 or E25 Lesson carries a `relatedLessonId`
into `architecture`, and that is the recorded decision rather than an omission: a forward link would
not have resolved when those Lessons shipped, so the earlier curricula point forward **in prose**, and
`BundledLearningCurriculumTest` already asserts that no Lesson authored before a given architecture
Unit was edited to receive a reciprocal link. The forward direction was therefore verified as prose
rather than as links, and **seventeen forward pointers into "the architecture curriculum" were found
and each one is now fulfilled** — among them `lesson_screen_state_and_ui_events` ("Detailed state
modelling belongs to later architecture curriculum" → L2.3), `lesson_who_owns_the_trigger` ("Choosing
and designing that owner belongs to later architecture curriculum" → L2.5 and L6.3),
`lesson_transient_ui_effects` (whether an occurrence is "carried in the screen's state, recorded
somewhere as handled, or delivered in some other way" → exactly the three options L6.1 and L6.2
supply), `lesson_transient_effect_delivery` ("which owner that is, how the occurrence is recorded, how
handling is marked and how the UI consumes it" → L6.2 and L6.3), `lesson_coroutine_scope_ownership`
("what a view model should contain" → L2.1), and `lesson_exception_propagation` together with
`lesson_flow_failure_and_completion` (an expected failure as a value rather than an exception → L3.5's
three shapes). **The two Lessons this plan told E26-09 to re-read —
`lesson_screen_state_owner_boundary` and `lesson_transient_effect_delivery` — were re-read in full and
both still read correctly now that the Units ship. Neither needed an edit, and no backward link was
added to either**, because their prose already resolves and adding one would have been the
graph-completeness work the issue rules out.

**Link duplication.** Each cross-link was checked for whether E26 applies the referenced concept or
repeats it. Every one applies: L1.2 cites `lesson_state_hoisting` and scales its writer test up
rather than restating it; L2.3 takes Compose integration "as given"; L2.5 asks one architectural
question "on top of" E24's scope mechanics; L3.4 links the three stream Lessons and teaches none of
their mechanics; L6.2 cites `lesson_transient_effect_delivery`'s negative result and starts from it.
**No content was trimmed**, because no substantial repetition was found, and the bridge context each
Lesson keeps is what makes it readable on its own.

### Code-block review

All **67** authored code blocks were read against their surrounding prose — **43 Kotlin and 24 `text`
diagrams**. Syntax is plausible throughout, names are consistent with the prose that discusses them,
the code and the explanation agree, and no snippet claims a stronger guarantee than it implements. The
E26-08 standard for overstated guarantees was applied to every snippet and **no code defect was
found**.

Two specific checks, because they are the ones a careless epic fails. **No platform-only type is
presented as common KMP architecture:** a mechanical scan for `android.`, `androidx.`, `java.`,
`UIView`, `NSObject`, `@Composable`, `Activity`, `Context` and `Fragment` inside code found exactly two
hits, and both are correct — `Activity` appears in L2.2's ownership diagram as one example of a
`ViewModelStoreOwner`, and `android.net.Uri` appears in L4.3 as the **deliberate counterexample** the
Lesson diagnoses ("A platform type restricts where the rule can live"). And **no snippet hides an
undeclared requirement behind pseudocode:** the bodies that are elided carry a comment saying what
they would do, and L6.2's `enum class ConfirmationState` is explicitly labelled "Not a queue and not a
delivery mechanism: a fact with an owner" so it cannot be read as an implementation.

**Snippets were not compiled.** The repository has no snippet-compilation tooling for learning content
and building one was not justified by any concrete gap, so the review is a reading against the prose
and against the production types the snippets refer to, and it is reported as that rather than as a
compiler result.

### Claims about this repository, re-checked against the source

E26 uses this codebase as evidence in twelve places. Every claim was verified against the current
source rather than trusted, because a stale claim is exactly the defect a closure pass exists to catch.

| Claim | Verified |
| --- | --- |
| Five repository interfaces: `AssessmentRepository`, `CurriculumRepository`, `LessonStudyRepository`, `SavedQuestionRepository`, `LearningContentRepository` (L3.1, L4.4, L4.5) | Exactly those five, each declared in its consumer's package |
| Five plain-class state holders: `StudyProgressStateHolder`, `ProgressStateHolder`, `MistakeReviewStateHolder`, `InterviewHistoryStateHolder`, `SavedQuestionStateHolder` (L2.1, L2.5, L6.3) | Exactly those five |
| Fifteen ViewModels, all in shared code (L2.2) | Fifteen files declare `: ViewModel()`, all under `commonMain` |
| Sixteen UI-state types (L2.3) | Sixteen `*UiState` declarations in `commonMain` |
| **Every repository method is a one-shot `suspend` function; none returns `Flow`** (L3.4, L6.3) | 26 `suspend fun` declarations across the five interfaces, **zero** occurrences of `Flow` |
| No class named `UseCase` or `Interactor`, and no `domain` package (L4.2, L4.1) | Zero occurrences of either identifier anywhere in the repository; zero directories named `domain` |
| The named services, policies and derivations (L4.2) | All eleven types exist at the stated responsibilities |
| No Room type and no DAO used above `data/local` (L3.1, L3.5) | No `androidx.room` import outside `data/local`; the only `Dao` mention above it is a comment in `RecentPerformancePolicy` citing the persistence ordering, not a usage |
| `App.kt` installs `rememberSaveableStateHolderNavEntryDecorator()` then `rememberViewModelStoreNavEntryDecorator()` (L2.2) | Both, in that order, at `App.kt:123–124` |
| `AppCoroutineScope`'s documented reason, quoted (L2.5, L6.3) | Verbatim |
| `StudyProgressStateHolder`'s "[repository] remains the source of truth" passage (L3.3) | Verbatim |
| `LearningLessonUiState.Content`'s nested `studyState` and its "missing indicator … a page the learner cannot read at all" comment (L2.3) | Verbatim |
| `LessonScrollStateReducer` is a reducer that is not a pure function of state and input (L5.5) | Exists, and accumulates scroll distance in its own fields as described |
| `PracticeBuilderViewModel` — one immutable state, read-only `StateFlow`, named callbacks, a `Channel` for its one outward occurrence, `isStartEnabled` derived (L5.5) | All five, and the four callbacks are `selectQuestionCount`, `toggleLevel`, `selectSource`, `retryAvailability` |
| `LessonStudyRepository`'s contract in the study feature's vocabulary with no database type (L1.4, L4.4) | Four operations, exactly as described, no database type |
| Configured versions: Kotlin 2.4.10, Compose Multiplatform 1.11.1, lifecycle 2.11.0-beta01, coroutines 1.11.0; `kotlinx-coroutines-swing` supplied to desktop (L2.2, L2.5) | All match `gradle/libs.versions.toml` |

**Two claims did not survive the check, and both are fixed.** They are the only content changes in
this issue and each is recorded with the defect that justified it below.

### Source review

All **79** Source references over **28 distinct URLs** were inspected, and every one was **exercised
through the production reader and the app's own URI boundary**: the existing data-driven journey
`LearningProductionContentJourneyTest.everyShippedUnitsAuthoredBlocksRenderInTheReader` reads every
shipped Unit, clicks every authored Source, and asserts the exact authored URL reaches the host's
`UriHandler`. That is stronger than a regex and it is where the verification comes from.

Hosts: `developer.android.com` 13, `martinfowler.com` 7, `kotlinlang.org` 5, `blog.cleancoder.com` 2,
`staltz.com` 1. Every choice remains appropriate to what it is cited for, and the review confirmed the
four distinctions the epic depends on. The **primary Clean Architecture sources are still used only for
the claims they support** — the Dependency Rule's own wording, the framework-independence property,
the flow-of-control-against-source-dependencies observation, and decisively the "circles are schematic"
statement L1.5 and L4.5 are built on. **Practitioner terminology sources are presented as evidence of
practitioner usage rather than as specifications**: L5.1 opens by stating that "there is no single
normative specification governing modern use of these four names" and cites Fowler for the variance
itself, and L5.4 labels Cycle.js as the origin while stating that "**No adaptation is canonical**".
**Android-specific guidance is not silently generalised to KMP**: L3.3 quotes the offline-first
recommendation with its condition attached, L4.1 and L4.5 quote the "recommendations and not strict
requirements" caveat, and L1.5 quotes the domain layer's optionality with the conditions that earn it.
**KMP ViewModel and lifecycle claims remain accurately qualified**: L2.2 names the host per target,
quotes JetBrains on iOS having "no built-in `ViewModelStoreOwner`" and on Navigation 3 entries not
being scoped by default, and then says which of those this application installs.

`blog.cleancoder.com` remains narrowly justified. It is cited twice, for the two claims for which no
vendor page is authoritative, and `InitialCurriculumContentQualityTest`'s `APPROVED_SOURCE_HOSTS`
comment already records the limit of the precedent. **No approved host was broadened**, because no
source failed.

**The 28 URLs were not re-fetched.** The configured versions are unchanged since E26-08's same-day
sweep, E26-09 changed no Source, and closure found no reason to suspect drift — which is the condition
the issue set for reusing that verification. The liveness of the pages is therefore carried from
E26-08 and is not claimed as observed here; what is claimed, and was observed, is that every authored
URL reaches the app's URI boundary unchanged.

One convention was observed rather than corrected: two URLs carry more than one title across the epic
(`.../ui-layer` as "UI layer", "UI layer: define UI state" and "UI layer: unidirectional data flow",
and `.../ui-layer/stateholders` as "State holders and UI state" and "UI layer: state holders"). This is
a document-wide, pre-existing device for pointing a reader at the relevant section of a long page —
fourteen URLs across all three curricula do it — and the authoring contract requires only a non-blank,
non-placeholder title. It is recorded here so a later session does not read it as drift, and it was
deliberately not "fixed", which would have been a document-wide stylistic change outside this issue.

### Reader journey

Exercised through the real reader rather than inspected. The existing production journey derives every
active Unit from the bundle, so it already traverses E26: in a 400 × 900 phone-shaped window it reads
all 24 shipped Units and 101 Lessons, including **every one of E26's 680 authored blocks**, asserting
that each one renders, that the reading column stays inside the window, that genuinely wide code
scrolls inside its own box, and that each Source is operable and emits its authored URL. Every block
type E26 uses is covered, because that suite's `when` over the sealed `LearningBlock` is exhaustive and
the block count is asserted against the bundle.

**Tables.** E26 authors 32 comparisons — 21 with three columns, 9 with four, 2 with two. At 400dp every
comparison renders as the compact stack, and the table renderer is covered separately by
`theWidestAuthoredComparisonStaysInsideTheReadingColumnAsATable`, which derives the document's widest
comparison and reads it at 1100 × 1000, asserting it is the table form rather than the stack, that its
content scrolls inside its own box, and that the reading column does not widen. The widest authored
comparison is still E25's six-column `lesson_choosing_a_compose_mechanism` table, so E26's widest
four-column tables are strictly narrower than the case already proven contained. **No new rendering
test was added for tables**, because no table defect was found and adding one would have duplicated a
derived check.

**Related-Lesson navigation could not be exercised, and this is a product fact rather than a gap.**
`relatedLessonIds` is authoring and validation metadata: it is consumed by
`LearningCurriculumValidator` and by nothing in the UI. `LearningLessonUiState.Content` carries
`previousLesson`, `nextLesson`, `sections`, `sources` and `studyState`, and no related-Lesson
affordance exists in the reader. Resolution is therefore verified — all 91 links resolve, globally by
the validator and per Unit by six assertions — and *navigation* has nothing to exercise. Adjacent
Lesson navigation forwards and back **is** exercised over every shipped Unit by the production journey.

### Progress journey

Exercised through the production reader, the production ViewModels and the real study repository, not
by inspecting database logic. `LearningUnitPracticeIntegrationTest.existingLearnerTraversesTheExpansionWithLiveParentProgressAndDurableIdentities`
walks the whole document with the parent ViewModels held alive throughout: for each of the six
architecture Units it opens every Lesson in the real reader, toggles it studied, and waits for the Unit
counter to reach `index + 1 of n` and the `architecture` Topic counter to reach the new total.

- **Unit progress.** Marking one Lesson increments its Unit; marking more advances it; completion state
  is reached at `n of n` for all six Units with their real counts 5, 5, 5, 5, 5, 4.
- **Topic progress.** The `architecture` Topic denominator is read from the bundle rather than written
  down, and it is **29**. The Topic advances **0 → 29** one Lesson at a time and reaches **29/29**.
- **Unmark and re-mark.** Reversal is exercised at the same 29-Lesson scale through the same
  ViewModels on the `async_reactive` Topic — 29 → 28 → 29, with its Unit counter moving 5/5 → 4/5 → 5/5
  and Continue Learning returning to the unmarked Lesson — and again on an earlier `android_ui` Lesson.
  **The architecture Topic itself is walked up to 29/29 and not back down**, and no
  architecture-specific reversal assertion was added: the derivation is Topic-agnostic, it is already
  reversed at the same scale through the same production path in the same test, and
  `StudyProgressDerivationTest` covers reversal directly. Duplicating it would be the padding this
  epic's test philosophy rules out. This is recorded as a deliberate choice rather than as coverage.
- The `android_ui` Topic's own progress is asserted unaffected by the architecture Units, which is what
  keeps Topic progress per Topic rather than global.

### Continue Learning journey

Verified against production order through the production resolver and the running shell, in the same
traversal. Continue Learning advances within each architecture Unit and crosses **all five** internal
E26 Unit boundaries, because `awaitNext(unit.id, lesson.id)` is asserted before every single one of the
29 Lessons is opened. It also crosses **`async_reactive` → E26 Unit 1**, from the last coroutines
Lesson into `lesson_what_architecture_decides`, which is what proves E26 extends the path rather than
replacing it.

E26 is **not** the end of the document's completion contract by being the current epic; it is the end
because the resolver walks the whole authored document in authored order and the architecture Units are
last in it. `ContinueLearningUiModel.Complete` is produced only once **all 101** authored Lessons are
studied — the test reaches `Complete` after the 29th architecture Lesson and not before — and
unstudying any earlier Lesson, in any Topic, returns Continue Learning to it. The whole-document
contract E24-09 established and E25-09 re-verified is unchanged, and **completion was not redefined**.

### Topic Detail

The `architecture` Topic Detail was driven through the real `TopicDetailViewModel` across the whole
traversal: six Units resolved from the bundle in exact authored order with Lesson counts
`5, 5, 5, 5, 5, 4`, progress labels moving with every mark, and completed and incomplete states
reached. The practice entry is exercised from both handoffs — the Unit overview's
`LearningUnitPracticeButtonTag` and the reader's `LearningLessonPracticeButtonTag` — and the longer
Unit titles caused no layout regression: the production journey asserts reading-column containment for
every Lesson of every Unit at 400dp, and the Unit rows are reached by scrolling the real lazy list to
each card. **No UI functionality was added.** One coverage boundary is recorded honestly:
`TopicDetailLearningContentTest`'s row-mapping assertion (`unitId`, `title`, `activeLessonCount`,
non-blank summary) covers `android_ui` only, and that has been true since E21-02 — `async_reactive` has
no equivalent either, and E24-09 and E25-09 both closed without adding one. The mapping is
Topic-agnostic and the architecture Units' order and counts are pinned twice elsewhere, so no copy was
added.

### Unit practice journey

Resolved through the production `PracticeTargetResolver` and the production Practice Builder, never
compared against generated documentation. The final reach, with levels:

| Unit | Primary concepts | Pool | F / A / Adv |
| --- | --- | --- | --- |
| 1 | `separation_of_concerns`, `dependency_direction`, `interface_boundaries`, `layered_architecture`, `architecture_tradeoffs` | 11 | 1 / 9 / 1 |
| 2 | `state_ownership`, `unidirectional_data_flow` | 10 | 3 / 6 / 1 |
| 3 | `repository_pattern`, `single_source_of_truth`, `layered_architecture`, `error_modeling` | 10 | 1 / 9 / 0 |
| 4 | `use_cases`, `clean_architecture`, `dependency_direction`, `interface_boundaries` | 9 | 0 / 9 / 0 |
| 5 | `mvc`, `mvp`, `mvvm`, `mvi`, `mvvm_vs_mvi` | 5 | 1 / 4 / 0 |
| 6 | `state_ownership`, `architecture_tradeoffs` | 10 | 2 / 6 / 2 |

Every pool matches the issue's expectation exactly, by set of ids as well as by count, and all six are
pinned by exact id in `LearningUnitPracticeIntegrationTest`. The nine verification points the issue
lists all hold: Topic Detail and the reader both start practice for the intended Unit; the existing
production assessment route is used; only primary mappings configure the scope; supporting concepts
contribute nothing — **no non-architecture Question enters any E26 pool**, and nine supporting-only
concepts that hold ACTIVE Questions of their own broaden nothing; DEPRECATED Questions are excluded, so
none of the four retired architecture Questions reaches a pool; and **no architecture-specific hack
exists** — the builder, the resolver, the taking engine and the result flow are the shipped generic
ones. **Assessment UX was not changed.**

Questions rendering and the answer flow were the one thing no suite had ever done over shipped content,
and they are now exercised end to end — see *Tests added* below. The run answers Unit 6's whole
ten-Question pool through the running assessment UI, takes the incorrect branch once and the correct
branch nine times, and completes into the existing Results screen scored 9 / 10.

### Practice semantic fairness

The six pools were re-read in learner order against what a learner knows by the Unit where each
Question appears. Every Question is fair at its Unit except the three cases the shared taxonomy
forces, all three already recorded by E26-08 and re-confirmed here rather than rediscovered. **No
additional unexpectedly premature Question was found.** Unit 5's pool was checked specifically, as the
issue asks: after E26-08 moved `viewmodel_vs_repository_responsibility` and
`architecture_ui_event_consumption` out, all five remaining Questions assess what Unit 5 teaches —
classifying by responsibilities, the MVP view contract, the observed-state arrangement against a class
or folder, the transition against the input spelling, and MVI's single state — which is true
semantically as well as structurally.

### Known structural routing limitations, re-confirmed

Documented rather than repaired, as the issue requires. **No Subtopic split, Lesson re-map, Question
re-map, Question deletion or new Question was attempted**, and no taxonomy change of any kind was made.

1. **Unit 2 ∩ Unit 6 — 8 Questions**, all through the shared `state_ownership` concept:
   `state_ownership_001`, `architecture_state_holder_taxonomy`, `durable_state_vs_one_off_event`,
   `viewmodel_activity_reference_lifetime`, `ui_state_shape_from_the_screens_requirements`,
   `architecture_ui_event_consumption`, `occurrence_guarantee_before_mechanism`,
   `owner_chosen_from_the_required_lifetime`. Four of those are Unit-6-oriented and are therefore
   premature for a learner who has just finished Unit 2 —
   `durable_state_vs_one_off_event`, `architecture_ui_event_consumption`,
   `occurrence_guarantee_before_mechanism` and `owner_chosen_from_the_required_lifetime`. The real fix
   is splitting the broad Subtopic, which E26-09 is not authorised to do.
2. **Unit 1 ∩ Unit 6 — 2 Questions**, both through the shared `architecture_tradeoffs` concept:
   `smallest_structure_that_satisfies_the_requirements` and
   `added_layer_must_isolate_an_independent_change`. The synthesis Question therefore appears in Unit 1
   before the learner has completed Units 2–6. It was **not weakened or removed** to purify Unit 1,
   because the assessment it carries is the epic's closing reasoning and the taxonomy is what cannot
   express the distinction.
3. **`architecture_paging_ownership` still reaches Unit 1** through `layered_architecture`, although
   its strongest semantic home is Unit 3, which it also reaches. E26-08 found no honest alternative
   mapping that was better, and closure confirmed that: the Question is about which layer owns
   pagination, so `layered_architecture` is a true mapping even where it is not the most useful one.

All three are asserted as sets in `LearningUnitPracticeIntegrationTest`, so none can drift silently.

### Assessment state, re-derived from production

E26-08's final state is intact and unchanged. Every figure below was derived from
`initial_curriculum.json` and the production resolver in this session.

- **460** total Questions; **419 ACTIVE**, **41 DEPRECATED**.
- **44** architecture Questions: **40 ACTIVE**, **4 DEPRECATED** (`repository_pattern_001`,
  `dependency_direction_001`, `architecture_tradeoffs_001`, `repository_vs_data_source_responsibility`,
  all still DEPRECATED).
- Architecture ACTIVE levels: **7 FOUNDATION / 31 APPLIED / 2 ADVANCED**.
- **0** architecture primary Subtopics without an ACTIVE Question — all 18 architecture Subtopics hold
  at least one.
- **39 of 40** ACTIVE architecture Questions are reachable through E26 Unit practice. The one outside
  every pool is `architecture_solid_dependency_substitution`, because `solid` is supporting-only across
  the whole epic by design and dependency inversion is assessed under the concept where the decision is
  actually made. **The exclusion is intentional and was preserved.**

**The two E26-08 re-maps are preserved**, because closure uncovered no factual defect in either:
`architecture_ui_event_consumption` remains `state_ownership` (delivery and acknowledgement, Unit 6
L6.2, not MVI transition modelling), and `viewmodel_vs_repository_responsibility` remains
`repository_pattern` (repository and data responsibility, Unit 3, not MVVM pattern definition).
`architecture_paging_ownership` remains `layered_architecture`, and
`dependency_direction_domain_framework_types` remains **APPLIED** — no rubric defect was found, so
neither decision was reopened.

`explicit_transition_is_not_the_input_spelling` remains **PARTIALLY_VERIFIED**. No new authoritative
evidence appeared: no vendor or primary source normatively defines MVI, so the stem still defines the
transition property it asks about. **It was deliberately not upgraded to clean up the report.**

**Audit-log integrity.** `docs/content/question-audit-log.yml` holds nine reviews, newest first, with
E26-08 prepended. Compared against the commit that last touched it before E26-08, the file has **322
insertions and 0 deletions**: no historical entry was overwritten, reformatted or reordered. All
eighteen new Question ids are present in the bank and reach the pools E26-08 recorded; both re-mapped
Questions retain their ids and their AnswerOption ids; the four DEPRECATED Questions remain DEPRECATED.
**No history was altered for stylistic consistency.**

**Generated coverage.** `docs/content/learning-question-coverage.md` was regenerated because the two
content corrections changed the learning document's fingerprint, and the **only** change in the 2,126
line file is that one SHA-256 — the mappings, counts and tables are byte-identical.
`python3 tools/learning_question_coverage.py --check` then reports the snapshot current, and the 21
coverage-tool unit tests pass.

### Final semantic pass

Each of the issue's twelve questions, answered from the shipped prose and the shipped practice.

| Can the learner… | Yes / no | Where it is taught and assessed |
| --- | --- | --- |
| 1. Explain architecture before naming patterns? | Yes | Units 1–4 make every decision without a pattern name; L5.1 says so explicitly. Assessed by `architecture_package_move_changes_nothing`, `separation_of_concerns_reason_to_change_test` |
| 2. Distinguish screen-state ownership from data authority? | Yes | L2.1's scope statement and L3.3's two-directional boundary. Assessed by `viewmodel_vs_repository_responsibility`, `authoritative_owner_is_chosen_per_fact` |
| 3. Explain ViewModel lifetime without calling it persistence? | Yes | L2.2's owner chain and six-event table; the three-idea table separating ownership, lifetime and persistence. Assessed by `viewmodel_activity_reference_lifetime`, `durable_state_vs_one_off_event` |
| 4. Justify or reject a repository? | Yes | L3.1's six decisions, and the reminder preference whose boundary is "none yet". Assessed by `repository_boundary_needs_a_decision_to_own` |
| 5. Choose a source of truth from requirements? | Yes | L3.3's three facts with three different owners, one of them in memory. Assessed by `authoritative_owner_is_chosen_per_fact`, `single_source_of_truth_001` |
| 6. Justify or reject a domain or use-case layer? | Yes | L4.1's four conditions and its honest middle case; L4.2's twin types. Assessed by `domain_layer_is_earned_by_the_feature`, `domain_layer_passthrough_cost`, `architecture_use_case_reuse` |
| 7. Trace dependency direction and inversion separately? | Yes | L1.3 separates source dependency, call direction and data flow; L4.4 asks who defines the abstraction and traces two import lists. Assessed by `dependency_direction_callback_does_not_reverse_it`, `architecture_interface_boundary_ownership`, `dependency_rule_constrains_direction_not_layer_count` |
| 8. Classify MVP, MVVM and MVI by responsibilities rather than folder names? | Yes | L5.1's five questions, L5.5's three unlabelled designs and four falsified claims. Assessed by `classify_a_screen_by_its_responsibilities`, `observed_state_arrangement_is_not_a_class_or_a_folder` |
| 9. Distinguish current state from an occurrence? | Yes | L6.1's two dimensions, and the facts that became true at a moment and are still state. Assessed by `durable_state_vs_one_off_event` |
| 10. Reason about delivery before choosing a stream mechanism? | Yes | L6.2's five questions and its requirement → guarantee → owner → mechanism order. Assessed by `occurrence_guarantee_before_mechanism` (ADVANCED) |
| 11. Choose an owner from the required lifetime? | Yes | L6.3's four-rung ladder, with one screen owner wrong in both directions. Assessed by `owner_chosen_from_the_required_lifetime` |
| 12. Choose the smallest sufficient architecture for a feature? | Yes | L6.4's two features designed end to end, then designed wrongly by swapping their structures. Assessed by `smallest_structure_that_satisfies_the_requirements` (ADVANCED) |

**No answer is "no", and no content was added**, because no phrasing improvement was mistaken for a
missing Lesson.

### Remaining deferrals

Recorded explicitly so E26 does not appear to promise material owned elsewhere. Each is named inside
the curriculum at the point a reader would otherwise expect it.

- **E27 — dependency injection.** Dagger, Hilt, Koin, graph construction, binding, scopes and
  qualifiers, service locator against DI mechanics, and runtime wiring. E26 teaches dependency
  inversion and ownership and stops: L4.4 separates the two decisions in a four-row table and says the
  container "decides which object is handed over; it does not change a single import". L2.1 defers
  construction by name.
- **E29 — build and modularization.** Gradle module-graph design, convention plugins, build-speed
  trade-offs, physical feature or layer modules, and enforcement through modules. E26 teaches logical
  dependency boundaries and never equates them with Gradle modules: L1.3, L1.5, L4.1, L4.4 and L4.5
  each draw the logical-against-physical line, and L4.4 and L4.5 use this repository's single module as
  the refutation that inverting a dependency needs a separate build unit.
- **E31 — testing.** Architecture tests, unit-test strategy, mocks, fakes, test doubles and testing
  frameworks. E26 mentions testability only as a consequence of a boundary and shows no test: L1.4
  ("which is why this lesson deliberately shows no test"), L1.5, L4.3 and L4.5 each state the
  ordering — the boundary is the cause, easier exercise is the consequence, and no layer count
  substitutes for it.
- **KMP curriculum / E33.** Source-set design, `expect`/`actual`, platform service abstractions,
  detailed iOS ownership integration and sharing-policy implementation. E26 uses KMP constraints to
  keep its claims honest — L2.2's per-host ownership, L4.3's one bounded sentence on shared policy —
  and teaches no implementation.
- **Lifecycle, navigation and persistence.** The `SavedStateHandle` API, detailed navigation ownership
  APIs, process-state restoration mechanics and persistence implementation. L2.2 names the mechanisms
  and stops; L6.3 states that how a back stack is built and how entries are scoped "belong to the
  lifecycle and navigation curriculum"; L3.2 hands eviction, scheduling and merge algorithms to the
  persistence curriculum.
- **Background work.** WorkManager and API selection, scheduler configuration, constraints, retries and
  OS background-execution mechanics. E26 decides when an in-process owner is insufficient and stops:
  L2.5, L6.2 and L6.3 each reach that conclusion and name the curriculum that owns the mechanism.
- **Queue and delivery infrastructure.** Durable queues, event buses, outboxes, distributed delivery
  protocols and general exactly-once processing. L6.2 names all five and refuses them: "The decision
  this lesson exists to teach is complete when you can say which guarantee is missing, which fact would
  have to exist, and who would own it. Building a messaging infrastructure is a different project."

### Content corrections made in E26-09

Two, and both are factual claims about this repository that did not survive being checked. Nothing else
in the 680 blocks was edited: no prose was polished, no wording was improved for style, and no
structural or editorial change was made.

| Lesson | The defect | The correction |
| --- | --- | --- |
| `lesson_dependency_inversion_in_practice` (L4.4) | The prose claimed that "Searching the shared module for imports of `data.local` from anywhere outside `data/local` returns nothing at all", introduced with "it is worth checking rather than assuming". Checking it finds **24 such imports** across four per-platform composition roots — `AndroidLocalData.kt`, `DesktopLocalData.kt`, `IosLocalData.kt` and `WebLocalData.kt` — each importing the Koin data modules and `CurriculumDataInitializer` in order to register the implementations. The architectural claim is sound; the absolute form of it is false, which matters most in the one Lesson that tells the reader to verify rather than trust | Narrowed to what is true and turned into evidence for the Lesson's own argument: no *feature* imports anything from `data.local`, and the only code outside it that names it is each platform's composition root — wiring rather than a source dependency from policy to detail, which is exactly the inversion-against-injection distinction the same Lesson's Senior section draws |
| `lesson_modelling_ui_state` (L2.3) | The prose attributed a claim to a source that does not make it: "`PracticeBuilderUiState` is a data class, and **its own documentation says why**: it is a configuration screen whose fields … are simultaneously true and independently editable". The field list and the simultaneity are true of the type, but the KDoc says something different — that the screen "never decides whether a level may be deselected, whether a source may be chosen, or whether Start is allowed", because "an invariant a Composable enforces is one that a second Composable can break". This is the same standard E26-08 applied to overstated Question guarantees: the cited source has to support the claim | Attributed correctly — reading the *type* says why the shape is a data class — and the documentation's actual point is quoted for what it does support, which is the invariant-ownership argument the Lesson's next section is built on |

Both corrections change the learning document's fingerprint, which is why the generated coverage
snapshot was regenerated. **No `relatedLessonId`, Source URL, Lesson id, Unit id, mapping, Question,
level or taxonomy entry was changed**, and no rendering defect, progress defect, Continue Learning
defect or practice-selection bug was found to fix.

### Tests added

One, for the one verification the issue asks for that no existing suite performed.

| Test | The gap it closes |
| --- | --- |
| `LearningProductionContentJourneyTest.shippedArchitectureQuestionsAreAnsweredThroughTheRunningAssessmentUi` | **No authored Question had ever been rendered through the screen a learner answers it on.** `LearningUnitPracticeIntegrationTest` resolves pools and drives runs through the `AssessmentTakingViewModel`, which proves routing and persistence and renders nothing; `FocusedLearningJourneyIntegrationTest` drives the assessment UI over a fixture catalogue of short invented Questions; and the builder journey reaches an enabled Start button and deliberately never presses it. The failures that hid there are real: an authored stem or option long enough to clip or widen the page, or an explanation that never reaches the screen after submission |

The test starts Unit 6's practice from the Unit overview in the running shell and answers the Unit's
**whole ten-Question pool** — chosen because it carries both of the architecture bank's ADVANCED
Questions, `occurrence_guarantee_before_mechanism` and
`smallest_structure_that_satisfies_the_requirements`, which have the Topic's longest stems and options.
For each Question it asserts the pinned progress meter, the stem, and **every authored option** as
displayed, enabled and contained inside a 400dp window; it selects, submits, and asserts the authored
explanation reaches the screen on **both** the incorrect and the correct branch, with the authored key
shown on the incorrect one; and it asserts the pool was covered by set of ids rather than by count, so
it cannot pass having answered one Question. It then asserts the product's actual completion contract:
practice completes itself after its final feedback rather than showing the finish step Interview uses,
so the learner arrives at the existing Results screen, scored 9 / 10 over the authored keys. The
candidates are derived from the Unit's own primary concepts the way the resolver derives them and the
Question on screen is recognised by its authored stem, so a re-map changes what the journey reads
rather than breaking it. The harness's postcondition that reading creates no attempt became a
parameter, so every reading journey still asserts zero and only this one states the single attempt it
means to create.

**The first version of this test was green locally and failed on CI**, and the correction is recorded
here because the failure was instructive rather than incidental. It drove its two navigation taps with
a coordinate `performClick()` after scrolling the control into a lazy list, which is exactly the
hazard `openShippedUnit` already documents and already guards against — a late progress refresh can
replace the list and move what was just scrolled to, so the tap lands somewhere that is no longer the
control and silently does nothing. The journey now invokes the click contract as a semantics action,
re-scrolls and re-taps up to the suite's existing attempt limit, and only taps while the node actually
carries a click action, so a control that is still disabled is waited through instead of being tapped
into nothing. Submit is deliberately *not* retried: after feedback the same control becomes Next, so a
retry there would skip a Question rather than recover one, and it instead waits for the control to
become clickable and taps once.

Two honest limits on that diagnosis. **The original CI failure was not reproduced locally**, so the
fix is justified by the hazard the suite already documents and by the corrected journey passing
repeatedly, not by a reproduced red-to-green. And one candidate cause was **checked and rejected**
rather than assumed: the practice control was measured at the phone-shaped window and sits flush above
the floating navigation bar — its bottom edge and the bar's top edge are both at 824dp — but its
centre, where a coordinate tap lands, is 20dp clear of it, so interception by the navigation bar is
not what happened and is not claimed.

**Tests deliberately not added, because existing coverage already proves the criterion:**

- **The full E26 authored-order assertion** — `BundledLearningCurriculumTest` already pins all six
  Units' Lesson ids *and* Lesson titles, plus their primary mappings, and
  `LearningUnitPracticeIntegrationTest` pins the Unit ids and the `5, 5, 5, 5, 5, 4` counts.
- **E25 → E26 cross-link resolution** — six per-Unit link tests already assert that every E26 link
  resolves to a shipped anchor and that no link points forward, and `LearningCurriculumValidatorTest`
  validates related-Lesson resolution globally.
- **Architecture Topic progress 0 → 29 → 28 → 29** — 0 → 29 is already walked through the production
  reader and ViewModels; the reversal is already exercised at the same 29-Lesson scale through the same
  path on `async_reactive`, and directly by `StudyProgressDerivationTest`.
- **Continue Learning traversal across all six Units** — already asserted before each of the 29
  Lessons, including the `async_reactive` → E26 handover and whole-document `Complete`.
- **The post-E26-08 exact practice pools** — already pinned by exact id for all six Units, with the
  three overlaps asserted as sets.
- **Representative reader rendering for architecture tables, code and Sources** — already covered by
  two data-driven journeys that read every authored block of every shipped Unit and derive the
  document's widest comparison.

### Validation performed

Run sequentially rather than as one parallel invocation.

| Command | Result |
| --- | --- |
| `./gradlew :shared:jvmTest --rerun-tasks --tests '*LearningCurriculumValidatorTest' --tests '*BundledLearningCurriculumTest' --tests '*LearningContentEndToEndTest' --tests '*LearningUnitPracticeIntegrationTest' --tests '*InitialCurriculumContentQualityTest' --tests '*InitialCurriculumSmokeTest'` | Passed — 118 tests, 0 failures |
| `./gradlew :shared:jvmTest --rerun-tasks --tests '*LearningProductionContentJourneyTest' --tests '*ProgressLearningJourneyIntegrationTest' --tests '*LearningReaderJourneyIntegrationTest' --tests '*ContinueLearningPolicyTest' --tests '*ContinueStudyingResolverTest' --tests '*StudyProgress*' --tests '*TopicDetailLearningContentTest' --tests '*LearningNavigationIntegrationTest'` | Passed — 79 tests, 0 failures |
| `python3 tools/learning_question_coverage.py --check` (before the corrections) | Reported stale, correctly — the fingerprint had changed |
| `python3 tools/learning_question_coverage.py --write` | Rewrote `docs/content/learning-question-coverage.md`, 2,126 lines; the only diff is the learning fingerprint |
| `python3 tools/learning_question_coverage.py --check` | Snapshot current |
| `python3 -m unittest discover -s tools -p 'test_*.py'` | 21 tests, OK |
| `./gradlew :shared:jvmTest --rerun-tasks` | Passed — **1,403 tests, 0 failures, 0 skipped**, 114 classes |
| `./gradlew :shared:check` | Passed |
| `./gradlew :shared:iosSimulatorArm64Test --rerun-tasks` | Passed — 448 tests, 0 failures |
| `./gradlew :androidApp:assembleDebug :desktopApp:assemble` | Passed |
| `./gradlew :webApp:assemble` | Passed — webpack's two pre-existing bundle-size warnings remain |
| `git diff --check` | Clean |
| `git status --short` and a full diff review | Three files changed, no generated build or cache output in the diff |

Per-target results inside `:shared:check`, taken from the freshly written result files:
`testAndroidHostTest` 448 tests / 0 failures, `jsBrowserTest` 448 / 0, `wasmJsBrowserTest` 448 / 0,
and `iosSimulatorArm64Test` 448 / 0 from its own forced run. The pre-existing `runSkikoComposeUiTest`
deprecation warnings remain and are unrelated to this issue.

### Platform limitations, stated precisely

- **Validated locally:** JVM, Android host, JS, WasmJS, `iosSimulatorArm64`, the Android debug
  assembly, and the desktop and web distributions.
- **`iosArm64` was not compiled locally and is not compiled on CI.** The epic has recorded this
  consistently and it is still true. This issue changes shared JSON data and one JVM test source and
  introduces no target-specific code, so no target-specific claim rests on it — the limitation is
  reported because it remains true of the epic.
- **No iOS device validation is claimed.**
- **No passing CI run is claimed, and no merge is claimed.** One CI failure was reported back
  during this issue — the first version of the new assessment journey, whose correction is
  recorded under *Tests added* — and it was relayed rather than observed here: no GitHub
  Actions run was viewed from this session, and no green CI result is asserted.
- **Backlog validation could not be run**: `PyYAML` is unavailable in this environment. Neither
  `.github/project/backlog.yml` nor `docs/content/question-audit-log.yml` was modified by this issue;
  both were read as text, and the audit log's integrity was established with `git diff --numstat`
  against the commit before E26-08 rather than by parsing.
- **The 28 Source URLs were not re-fetched**, for the reasons given above. Their liveness is carried
  from E26-08's same-day sweep and is not claimed as observed here.
- **Code snippets were not compiled.** No snippet-compilation tooling exists for learning content and
  no concrete gap justified building one.
- **Whether a Lesson teaches enough to answer the Questions routed to it is a judgement**, recorded as
  one above. The routing tests prove which Questions a learner reaches, never that the prose suffices.

### Closure verdict

**E26 is closed from the curriculum's perspective.** E26-01 planned the curriculum; E26-02 through
E26-07 authored all six Units; E26-08 completed assessment coverage; E26-09 completed integration
verification. The six Units read as one continuous argument from responsibility and boundary through
screen-state ownership, data authority, optional domain policy and dependency direction, and
presentation-pattern classification, to requirement → guarantee → lifetime → proportional
architecture, with no conceptual jump that depends on material not yet taught. Terminology is
consistent across all 29 Lessons and with the three shipped curricula; every responsibility E25
deferred is either answered by a shipped Lesson or recorded above as still deferred with its owner and
its reason; the curriculum applies rather than re-teaches Compose, coroutine, Flow and effect
mechanics; and study progress, Continue Learning, Topic Detail and Unit practice all integrate through
the existing product flows with **no unrelated product behaviour changed**. Two factual claims about
this repository were corrected, one genuine test gap was closed, and the three structural routing
limitations that remain are taxonomy limits recorded and asserted rather than editorial debts. **E27 is
not started by this issue.**
