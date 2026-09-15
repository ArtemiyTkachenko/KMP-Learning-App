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

**No Unit has been authored yet.** E26-02 is the first to append here.
