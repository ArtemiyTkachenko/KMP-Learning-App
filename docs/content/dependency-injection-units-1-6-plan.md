# Dependency Injection Units 1–6 Authoring and Assessment Plan

## Purpose

`docs/content/dependency-injection-learning-blueprint.md` maps the whole dependency-injection
and object-graph subject. This document is the confirmed authoring plan for the six Units
that epic E27 delivers — **6 Units, 34 Lessons** — reviewed against the taxonomy, the shipped
architecture Units, the current ACTIVE and DEPRECATED question bank, this repository's own
Koin graph, the configured libraries, and current authoritative Dagger, Hilt and Koin
documentation.

It exists so that E27-02 through E27-07 can start authoring without re-deciding identity,
scope, or concept ownership, and so that E27-08 has a traceable list of assessment gaps.

It is **not** a second blueprint. Objectives, depth layers, misconception targets,
terminology and Teach/Bridge/Reference/Exclude decisions stay in the blueprint; this
document records the decisions the blueprint left open and the review findings that would
otherwise have to be re-derived.

**Review date: 2026-09-18.** Everything in
[configured versions](#configured-versions-this-plan-assumes),
[repository evidence](#part-7--this-repositorys-own-koin-graph-as-evidence) and
[framework findings](#part-8--dagger-hilt-and-koin-source-sensitive-findings) was checked on
that date against the versions this repository is configured with and against the
documentation each framework publishes today.

**This issue is planning only.** It introduces no production Unit, Lesson, Question or
taxonomy entry, changes no production curriculum JSON, changes nothing in this repository's
Koin graph, and adds no Dagger or Hilt dependency anywhere. Every identity proposed here
lives in documentation until the authoring issue that ships it.

## Scope confirmation

The merged epic assumes six instructional Units. **The six-Unit structure is unchanged.**

The test applied is the one E24, E25 and E26 established: *does crossing this Unit boundary
require a genuinely different mental model?* Taxonomy shape and Question availability were
deliberately not used as the test — the `dependency_injection` Topic has 25 Subtopics and the
curriculum has 6 Units, and nothing about that ratio is a design input.

| Boundary | Different mental model? | Why |
| --- | --- | --- |
| 1 → 2 | Yes | Unit 1 reasons about one object and where its collaborators come from. Unit 2 reasons about a structure — shared nodes, reuse, lifetime, ambiguity — none of which is visible in a single constructor. A reader can hold Unit 1 completely and still not know what a scope is a rule about. |
| 2 → 3 | Yes | Units 1–2 decide; Unit 3 encodes. Reading a generated graph, choosing between two ways of declaring a binding, and reasoning about what a compiler checked are activities, not further decisions. |
| 3 → 4 | Yes | Unit 3's reader designs a component structure. Unit 4's reader receives one and has to decide whether their lifetimes fit it. Accepting somebody else's structure is a different skill from designing one, and it is the one an Android interview asks for. |
| 4 → 5 | Yes | Unit 4's lifetimes are handed over by a platform's own component tree. Unit 5 removes the platform: the same lifetime questions have to be answered with nothing to inherit them from, and the graph has to be declared in code that compiles for four targets. |
| 5 → 6 | Yes | Units 1–5 answer "how would this be expressed". Unit 6 answers "should this project express it at all, and in which of four notations", which starts from requirements rather than from a graph. |

**The Lesson count is 34**, distributed 6/6/7/6/5/4. The counts are not quotas; each was
derived from the number of distinct mental models the Unit carries, and the deliberate
unevenness is recorded per Unit:

- **Unit 1 carries six** because "where can a collaborator come from", "what a constructor
  parameter makes true", "injection is not inversion", "who assembles the graph", "what a
  lookup hides" and "when hand-wiring is sufficient" are six separately learnable ideas, and
  E27-02's acceptance criteria name all six. Merging the inversion Lesson in particular would
  bury the misconception the Unit most exists to correct, and merging the sufficiency Lesson
  would turn manual wiring back into a stepping stone.
- **Unit 2 carries six** because the epic's own acceptance criteria name six distinct
  outcomes — trace a graph, choose reuse from a requirement, separate scope from lifetime and
  owner, separate runtime input from a dependency, read same-type ambiguity as a design
  question, and reason about failure timing. Three of them are the terminology the three
  framework Units rest on, and none survives being folded into a neighbour.
- **Unit 3 carries seven**, one more than the blueprint's usual ceiling, and the extra one is
  deliberate. Six Lessons cover the mechanisms; the seventh covers what compile-time
  validation does and does not prove, which is a reasoning responsibility rather than a
  mechanism, is named separately in E27-04's acceptance criteria, and is the epic's
  misconception 14. Folded into the component Lesson it becomes a paragraph about error
  messages. Qualifiers and multibindings were merged into one Lesson for the opposite reason:
  both are about the binding key being insufficient as a selector, which is one model.
- **Unit 4 carries six** because Hilt's contribution is exactly six decisions — what it
  decides for you, which component owns a lifetime, what happens when the platform owns
  construction, how ViewModels and late-arriving values work, what installation decides, and
  whether to accept the convention at all. The ViewModel Lesson cannot merge with the
  component Lesson because the retained-against-ViewModel distinction is the Unit's subtlest
  point and needs both to exist first.
- **Unit 5 carries five** because the Koin surface this curriculum teaches is container,
  definitions, scopes, ViewModel resolution and the multiplatform split. A sixth Lesson on
  Koin's current declaration mechanisms was considered and rejected: its objective would have
  been a capability list, which Rule 5 of the authoring contract forbids, so the material is a
  bounded section of L5.1 and an axis in L6.2 instead.
- **Unit 6 carries four** because it is a synthesis Unit and adds no new mechanism: the axes,
  the detection-time axis worked in full because it is the one most commonly stated wrongly,
  three scenarios, and proportionality. A fifth Lesson would have to invent material.

## How the authoring issues use this document

| Issue | Reads |
| --- | --- |
| E27-02 | Unit 1 identities, prerequisites and boundaries; the Unit 1 rows of the semantic review; GAP-U1-A…E; the source policy; the E26 handoff ledger for what `lesson_dependency_inversion_in_practice` already settles |
| E27-03 | Unit 2 identities and boundaries; the Unit 2 rows; GAP-U2-A…F; **[the terminology contract](dependency-injection-learning-blueprint.md#terminology-this-curriculum-fixes) in full**, since this Unit fixes the vocabulary the next three depend on |
| E27-04 | Unit 3 identities and boundaries; the Unit 3 rows; GAP-U3-A…B; **[the Dagger findings](#dagger-findings) in full** |
| E27-05 | Unit 4 identities and boundaries; the Unit 4 rows; GAP-U4-A…C; **[the Hilt findings](#hilt-findings) in full**, including the two-source divergence that decides which components a Lesson may name |
| E27-06 | Unit 5 identities and boundaries; the Unit 5 rows; GAP-U5-A…E; **[Part 7](#part-7--this-repositorys-own-koin-graph-as-evidence) in full**, which is the Unit's case study; **[the Koin findings](#koin-findings) in full** |
| E27-07 | Unit 6 identities and boundaries; the Unit 6 rows; GAP-U6-A…C; the framework findings for the accuracy of every comparative claim |
| E27-08 | [Assessment gaps](#assessment-gaps-for-e27-08) in full, re-checked against the finished Lessons, plus the [mapping, level and correction candidates](#mapping-level-and-correction-candidates) and the [practice routing model](#part-6--unit-practice-routing-modelled-now) |
| E27-09 | [Handoff](#handoff), the [E26 ledger](#part-3--the-e26--e27-handoff-ledger) for the deferral-by-deferral check its acceptance criteria require, and the recorded limitations |

Each authoring issue also appends its outcomes to
[Authoring outcomes](#authoring-outcomes), so a finding is re-checked rather than re-derived
by the issue that follows it.

## Configured versions this plan assumes

Read from `gradle/libs.versions.toml` on 2026-09-18, on `task/backlog-update-e27` at
`a876522`.

| Component | Version | Relevance to E27 |
| --- | --- | --- |
| **Koin** | **4.2.2** | The only dependency-injection library in this repository. Unit 5's case study and every Koin behaviour claim rest on it |
| `koin-core`, `koin-compose`, `koin-compose-viewmodel`, `koin-android` | 4.2.2 | The four artifacts declared; `koin-android` is what supplies `androidContext()` |
| Kotlin | 2.4.10 | Language level for every example |
| Compose Multiplatform | 1.11.1 | Supplies the common `ViewModelStoreOwner` the ViewModel resolution in L5.4 depends on |
| `androidx-lifecycle` (through `org.jetbrains.androidx.lifecycle`) | 2.11.0-beta01 | `ViewModel` and its store — applied by L4.4 and L5.4, taught by neither |
| Room | 3.0.1 | Behind a repository binding in the case study; never taught |
| **Dagger** | **not present** | Curriculum subject only. No module declares it and none may |
| **Hilt** | **not present** | Curriculum subject only. No module declares it and none may |

**Documentation versions this plan is written against.** Koin's documentation labels itself
**4.2**, which is the closest current documentation to the configured `4.2.2` and is what
every Koin claim below rests on. Dagger and Hilt documentation is unversioned on the pages
used; the retrieval date of 2026-09-18 is therefore the freshness marker, and each authoring
issue re-verifies rather than trusting this table.

---

## Part 1 — Complete dependency-injection taxonomy inventory

Every Subtopic in the `dependency_injection` Topic, read from
`shared/src/commonMain/composeResources/files/curriculum/initial_curriculum.json` on
2026-09-18. **There are 25, and all 25 are `ACTIVE`.** The Topic holds **27 Questions: 23
ACTIVE and 4 DEPRECATED**. Twenty-one Subtopics have at least one ACTIVE Question;
`dagger_modules`, `koin_fundamentals`, `koin_scopes`, `koin_viewmodels` and
`di_framework_tradeoffs` have none — five empty Subtopics, four of them the ones this epic
most needs.

The last column is this epic's decision under Rule 4 of the authoring contract. "Teach"
means the Subtopic is **primary** in at least one Lesson.

| Subtopic | Name | Status | ACTIVE | DEPRECATED | Conceptual responsibility | E27 decision |
| --- | --- | --- | --- | --- | --- | --- |
| `di_fundamentals` | Dependency injection fundamentals | ACTIVE | 1 | 0 | What injection is; the three ways a class gets a collaborator; injection against inversion | **Teach** — primary in L1.1 and L1.3 |
| `manual_di` | Manual dependency injection | ACTIVE | 1 | 0 | Hand-written wiring as a technique, its costs and its conditions of sufficiency | **Teach** — primary in L1.6 and L6.4 |
| `constructor_injection` | Constructor injection | ACTIVE | 1 | 1 | Requirements visible in the type; fully-formed objects | **Teach** — primary in L1.2 |
| `composition_root` | Composition root | ACTIVE | 1 | 0 | The responsibility of assembling the graph, and where it belongs | **Teach** — primary in L1.4 |
| `dependency_graphs` | Dependency graphs | ACTIVE | 1 | 1 | Transitive graphs, roots, assembly, runtime input, key ambiguity, failure timing | **Teach** — primary in L2.1, L2.4, L2.5 and L2.6 |
| `di_scopes` | Scopes and lifetimes | ACTIVE | 1 | 0 | Reuse policy; the separation of lifetime requirement, owner and scope | **Teach** — primary in L2.2 and L2.3 |
| `service_locator_vs_di` | Service locator vs dependency injection | ACTIVE | 1 | 0 | Dependency visibility and resolution responsibility; where resolution is acceptable | **Teach** — primary in L1.5 |
| `dagger_fundamentals` | Dagger fundamentals | ACTIVE | 1 | 0 | Constructor bindings, generated construction, and what the compiler checks | **Teach** — primary in L3.1 and L3.7 |
| `dagger_modules` | Dagger modules | ACTIVE | 0 | 1 | A grouping of binding declarations that owns no graph | **Teach** — primary in L3.2. **Its only Question is DEPRECATED** |
| `dagger_bindings` | @Provides and @Binds | ACTIVE | 1 | 0 | Choosing a binding declaration from what each can express | **Teach** — primary in L3.2 |
| `dagger_components` | Components and subcomponents | ACTIVE | 3 | 0 | The graph root and its API; nested against independent graphs | **Teach** — primary in L3.3 and L3.4 |
| `dagger_scopes` | Dagger scopes | ACTIVE | 1 | 0 | Scoped reuse tied to a component instance | **Teach** — primary in L3.5 |
| `dagger_qualifiers` | Qualifiers | ACTIVE | 1 | 0 | Making two bindings of one type into two keys | **Teach** — primary in L3.6 |
| `dagger_multibindings` | Multibindings | ACTIVE | 1 | 0 | One key several independent declarations contribute to | **Teach** — primary in L3.6 |
| `hilt_fundamentals` | Hilt fundamentals | ACTIVE | 1 | 0 | Hilt as generated convention over Dagger; framework-owned construction; entry points | **Teach** — primary in L4.1 and L4.3 |
| `hilt_components` | Hilt components and lifecycle scopes | ACTIVE | 1 | 0 | The generated hierarchy, its lifetimes and its scope annotations | **Teach** — primary in L4.2 |
| `hilt_viewmodels` | Hilt ViewModels | ACTIVE | 2 | 0 | The ViewModel component, its scope, and values that arrive at navigation time | **Teach** — primary in L4.4 |
| `hilt_modules` | Hilt modules and InstallIn | ACTIVE | 1 | 0 | Which graph a binding joins, as distinct from how long it lives | **Teach** — primary in L4.5 |
| `hilt_vs_dagger` | Hilt vs raw Dagger | ACTIVE | 1 | 0 | Accepting a component structure against designing one | **Teach** — primary in L4.6 |
| `koin_fundamentals` | Koin fundamentals | ACTIVE | **0** | 0 | The container, modules, startup as composition root, declaration mechanisms | **Teach** — primary in L5.1 |
| `koin_definitions` | single/factory definitions | ACTIVE | 1 | 0 | Reuse policy expressed as a definition; interface binding | **Teach** — primary in L5.2 |
| `koin_scopes` | Koin scopes | ACTIVE | **0** | 0 | A bounded sub-container and the owner that must keep it alive | **Teach** — primary in L5.3 |
| `koin_viewmodels` | Koin ViewModel integration | ACTIVE | **0** | 0 | Definition, resolution at a boundary, runtime parameters, and who owns the store | **Teach** — primary in L5.4 |
| `koin_multiplatform` | Koin and Kotlin Multiplatform | ACTIVE | 1 | 0 | Shared definitions plus per-platform bindings, and per-host composition roots | **Teach** — primary in L5.5 |
| `di_framework_tradeoffs` | DI framework trade-offs | ACTIVE | **0** | 1 | Choosing a strategy from stated requirements | **Teach** — primary in L6.1, L6.2, L6.3 and L6.4 |

**Nothing in the Topic is left unmapped, and nothing is Bridge, Reference or Exclude.** All
25 are primary in at least one Lesson. This is a different outcome from E26, where `solid`
was deliberately supporting-only, and the difference is not an oversight: the
`dependency_injection` taxonomy was built for exactly this subject, so every Subtopic in it
names something a Unit here has to teach. Three Subtopics are nevertheless taught at
**Reference depth inside a Teach Lesson** — `dagger_multibindings` (the set form only),
`koin_scopes` (only as far as the lifetime model needs) and the entry-point half of
`hilt_fundamentals` — and the blueprint's Notes record each boundary.

### Concepts this epic needs for which the taxonomy has no exact Subtopic

Recorded in the blueprint's
[taxonomy gaps](dependency-injection-learning-blueprint.md#taxonomy-gaps-concepts-with-no-exact-assessment-subtopic)
with the mapping used instead. The four that matter most to E27-08 are **injection against
inversion** (mapped to `di_fundamentals`), **runtime input against graph dependency** (mapped
to `dependency_graphs`), **generic same-type ambiguity** (also `dependency_graphs`) and **the
integration boundary at which framework resolution is acceptable** (mapped to
`service_locator_vs_di`). The first three are why `dependency_graphs` is primary in four
Lessons and `di_fundamentals` in two.

**No taxonomy ID is invented by this issue, and no taxonomy change is recommended.** Two
observations are recorded for a later question-bank decision and are explicitly *not*
proposed as E27 work:

1. **`koin_kmp` exists in the `kmp` Topic** with the same meaning as `koin_multiplatform` in
   this Topic, and holds no Questions. E27 uses `koin_multiplatform` because its Units home
   in `dependency_injection`. This is a duplication to guard against in E27-08, not an
   impossibility.
2. **`di_scopes` has to carry three distinguishable ideas** — the lifetime requirement, the
   owner, and the scope rule — and a single Subtopic cannot express which of them a Question
   is about. This constrains how precisely E27-08 can route the scope Questions, and is
   recorded in [routing limitations](#handoff) rather than solved by a split.

**Neither observation is a demonstrated semantic impossibility.** An empty Subtopic is not
evidence of one, and neither is a routing overlap; both would need a distinction the existing
IDs cannot honestly express, and this review found none.

---

## Identity conventions and proposed identities

The shipped Compose, Coroutines and Architecture content establishes the convention and this
plan follows it rather than inventing one:

- **Unit id** — `unit_` plus the Unit title in snake case, shortened where the full title
  would be unreadable.
- **Lesson id** — `lesson_` plus the concept the Lesson owns, in snake case, short enough to
  read in a reference. Lesson ids are **not** numbered: the `L1.1` labels in the blueprint
  are positional and would be wrong the moment a Lesson moves.
- Ids are stable once shipped. Renaming one breaks learner study-progress records, which are
  keyed by Lesson id (see `docs/architecture/study-progress.md`).
- Lesson ids must be unique across **all** Units, not just within one: `relatedLessonIds`
  names a Lesson without naming its Unit, and `LearningCurriculumValidator` rejects a
  duplicate (`DUPLICATE_LESSON_ID`).

**Three Unit titles differ from the backlog's issue titles**, and each change is a
pedagogical one rather than a shortening:

- Unit 3 is **"Dagger: Compile-Time Object Graphs"** rather than "Dagger Compile-Time Object
  Graphs" — punctuation only, matching the shipped convention of naming the framework and
  then what it is.
- Unit 4 is **"Hilt: Android Lifecycle-Aware Dagger"** rather than "Hilt Android Lifecycle
  Integration". The title states the Unit's thesis, which is that Hilt *is* Dagger with the
  Android decisions made, and the Unit's first misconception is that it is not. A title that
  says "integration" invites exactly the separate-system reading L4.1 exists to defeat.
- Unit 6 is **"Choosing a Dependency Injection Strategy"** rather than "Dependency Injection
  Strategy and Framework Trade-offs". The Unit's subject is the decision, not the table, and
  a title naming trade-offs first sets up the scorecard the epic forbids.

Units 1, 2 and 5 keep the backlog's wording exactly.

| Blueprint | Unit id | Unit title |
| --- | --- | --- |
| Unit 1 | `unit_dependency_injection_as_object_construction` | Dependency Injection as Object Construction |
| Unit 2 | `unit_object_graphs_lifetimes_and_scopes` | Object Graphs, Lifetimes and Scopes |
| Unit 3 | `unit_dagger_compile_time_object_graphs` | Dagger: Compile-Time Object Graphs |
| Unit 4 | `unit_hilt_android_lifecycle_integration` | Hilt: Android Lifecycle-Aware Dagger |
| Unit 5 | `unit_koin_and_dependency_injection_in_kmp` | Koin and Dependency Injection in KMP |
| Unit 6 | `unit_choosing_a_dependency_injection_strategy` | Choosing a Dependency Injection Strategy |

**All six Units take `dependency_injection` as their home Topic.** The Topic exists, holds 25
Subtopics and 23 ACTIVE Questions, and no shipped Learning Unit homes there, so E27 is the
first content in it.

| Blueprint | Lesson id | Lesson title | Primary | Supporting |
| --- | --- | --- | --- | --- |
| L1.1 | `lesson_who_constructs_this_object` | Who Constructs This Object? | `di_fundamentals` | `constructor_injection`, `manual_di`, `service_locator_vs_di`, `separation_of_concerns` |
| L1.2 | `lesson_a_dependency_should_be_visible` | A Dependency Should Be Visible | `constructor_injection` | `di_fundamentals`, `interface_boundaries`, `dependency_direction`, `test_doubles` |
| L1.3 | `lesson_injected_inverted_or_both` | Injected, Inverted, or Both? | `di_fundamentals` | `dependency_direction`, `interface_boundaries`, `solid`, `constructor_injection` |
| L1.4 | `lesson_one_place_that_knows_how_to_build` | One Place That Knows How to Build | `composition_root` | `manual_di`, `dependency_graphs`, `service_locator_vs_di`, `separation_of_concerns`, `layered_architecture` |
| L1.5 | `lesson_asking_for_it_or_being_given_it` | Asking For It, or Being Given It | `service_locator_vs_di` | `di_fundamentals`, `composition_root`, `constructor_injection`, `test_di` |
| L1.6 | `lesson_when_wiring_it_yourself_is_enough` | When Wiring It Yourself Is the Right Answer | `manual_di` | `composition_root`, `dependency_graphs`, `di_framework_tradeoffs`, `architecture_tradeoffs` |
| L2.1 | `lesson_from_one_dependency_to_a_graph` | From One Dependency to a Graph | `dependency_graphs` | `composition_root`, `manual_di`, `constructor_injection`, `layered_architecture` |
| L2.2 | `lesson_one_instance_or_a_new_one` | One Instance, or a New One Each Time? | `di_scopes` | `dependency_graphs`, `state_ownership`, `architecture_tradeoffs` |
| L2.3 | `lesson_scope_is_a_rule_owner_is_a_lifetime` | A Scope Is a Rule; an Owner Is a Lifetime | `di_scopes` | `dependency_graphs`, `state_ownership`, `android_process_model`, `viewmodel_lifecycle` |
| L2.4 | `lesson_runtime_input_is_not_a_dependency` | A Value the Graph Cannot Know | `dependency_graphs` | `di_scopes`, `constructor_injection`, `navigation_fundamentals`, `state_ownership` |
| L2.5 | `lesson_two_dependencies_of_the_same_type` | Two Dependencies of the Same Type | `dependency_graphs` | `dagger_qualifiers`, `interface_boundaries`, `constructor_injection` |
| L2.6 | `lesson_when_a_broken_graph_tells_you` | When Does a Broken Graph Tell You? | `dependency_graphs` | `di_framework_tradeoffs`, `dagger_fundamentals`, `kotlin_gradle_plugin` |
| L3.1 | `lesson_dagger_constructs_what_it_can_see` | What Dagger Can Construct on Its Own | `dagger_fundamentals` | `constructor_injection`, `manual_di`, `composition_root`, `dependency_graphs` |
| L3.2 | `lesson_declaring_the_rest_of_the_graph` | Declaring the Rest of the Graph | `dagger_modules`, `dagger_bindings` | `dagger_fundamentals`, `interface_boundaries`, `dependency_direction` |
| L3.3 | `lesson_which_graph_owns_this_binding` | Which Graph Owns This Binding? | `dagger_components` | `dagger_modules`, `composition_root`, `dependency_graphs`, `dagger_bindings` |
| L3.4 | `lesson_child_graph_or_separate_graph` | A Child Graph, or a Separate Graph? | `dagger_components` | `dagger_scopes`, `dependency_graphs`, `layered_architecture`, `module_dependency_direction` |
| L3.5 | `lesson_dagger_scopes_and_component_instances` | A Scope Is a Promise the Component Keeps | `dagger_scopes` | `di_scopes`, `dagger_components`, `state_ownership` |
| L3.6 | `lesson_when_the_type_is_not_the_key` | When the Type Is Not the Key | `dagger_qualifiers`, `dagger_multibindings` | `dagger_bindings`, `dependency_graphs`, `dependency_direction`, `feature_modularization` |
| L3.7 | `lesson_what_the_dagger_compiler_checked` | What the Dagger Compiler Actually Checked | `dagger_fundamentals` | `dependency_graphs`, `di_framework_tradeoffs`, `dagger_components`, `kotlin_gradle_plugin` |
| L4.1 | `lesson_hilt_is_dagger_with_decisions_made` | Hilt Is Dagger With the Decisions Already Made | `hilt_fundamentals` | `hilt_vs_dagger`, `dagger_components`, `dagger_fundamentals`, `activity_lifecycle` |
| L4.2 | `lesson_which_android_component_owns_this` | Which Android Component Owns This? | `hilt_components` | `di_scopes`, `dagger_scopes`, `activity_lifecycle`, `configuration_changes`, `android_process_model` |
| L4.3 | `lesson_when_android_owns_construction` | When Android Owns Construction | `hilt_fundamentals` | `constructor_injection`, `service_locator_vs_di`, `activity_lifecycle`, `hilt_components` |
| L4.4 | `lesson_hilt_viewmodels_and_runtime_input` | ViewModels, Their Component, and the Values That Arrive Late | `hilt_viewmodels` | `hilt_components`, `viewmodel_lifecycle`, `saved_state`, `state_ownership`, `dependency_graphs` |
| L4.5 | `lesson_which_graph_does_this_binding_join` | Which Graph Does This Binding Join? | `hilt_modules` | `dagger_modules`, `hilt_components`, `dagger_scopes`, `di_scopes` |
| L4.6 | `lesson_hilt_or_hand_written_dagger` | Hilt, or Components You Write Yourself? | `hilt_vs_dagger` | `hilt_components`, `dagger_components`, `di_framework_tradeoffs`, `architecture_tradeoffs` |
| L5.1 | `lesson_the_koin_container_and_its_modules` | The Container, and the Modules That Fill It | `koin_fundamentals` | `composition_root`, `dependency_graphs`, `koin_definitions`, `di_framework_tradeoffs`, `kmp_architecture` |
| L5.2 | `lesson_koin_definitions_and_reuse` | Definitions, and the Reuse Requirement Behind Them | `koin_definitions` | `koin_fundamentals`, `di_scopes`, `constructor_injection`, `interface_boundaries`, `service_locator_vs_di` |
| L5.3 | `lesson_koin_scopes_and_their_owners` | Scopes, and the Owner That Has to Stay Alive | `koin_scopes` | `di_scopes`, `koin_definitions`, `koin_fundamentals`, `state_ownership` |
| L5.4 | `lesson_resolving_viewmodels_at_the_boundary` | Resolving a ViewModel at the Boundary | `koin_viewmodels` | `koin_definitions`, `service_locator_vs_di`, `kmp_lifecycle_viewmodel`, `viewmodel_lifecycle`, `state_ownership` |
| L5.5 | `lesson_one_graph_across_platforms` | One Graph, Several Platforms | `koin_multiplatform` | `koin_fundamentals`, `expect_actual`, `platform_implementations`, `kmp_architecture`, `composition_root`, `interface_boundaries` |
| L6.1 | `lesson_what_a_container_actually_buys` | What a Container Actually Buys | `di_framework_tradeoffs` | `manual_di`, `dagger_fundamentals`, `hilt_fundamentals`, `koin_fundamentals`, `architecture_tradeoffs` |
| L6.2 | `lesson_when_should_a_graph_error_surface` | When Should a Graph Error Surface? | `di_framework_tradeoffs` | `dagger_fundamentals`, `koin_fundamentals`, `dependency_graphs`, `manual_di`, `kotlin_gradle_plugin` |
| L6.3 | `lesson_three_projects_three_answers` | Three Projects, Three Answers | `di_framework_tradeoffs` | `manual_di`, `hilt_vs_dagger`, `koin_multiplatform`, `dagger_components`, `kmp_architecture` |
| L6.4 | `lesson_the_smallest_sufficient_strategy` | The Smallest Sufficient Strategy | `manual_di`, `di_framework_tradeoffs` | `composition_root`, `architecture_tradeoffs`, `di_fundamentals`, `constructor_injection` |

### Identity and mapping checks performed

Checked against the bundled production JSON on 2026-09-18, by script rather than by reading.

- **Uniqueness.** None of the 6 proposed Unit ids and 34 proposed Lesson ids collides with
  the **24 shipped Unit ids or the 101 shipped Lesson ids** in `learning_curriculum.json`, or
  with each other. The longest proposed Unit id is 48 characters and the longest Lesson id is
  44, both within the range the shipped content already uses.
- **Mapping validity.** All **47 distinct Subtopic ids** used above exist in
  `initial_curriculum.json` and are `ACTIVE`. Twenty-five are `dependency_injection` — every
  Subtopic in the Topic. The other twenty-two bridge to `architecture` (7),
  `lifecycle_navigation` (5), `kmp` (4), `build_delivery` (3), `testing` (2) and
  `android_platform` (1) — the cross-Topic bridging Rule 3 of the authoring contract expects.
- **Primary validity.** Every primary concept is a `dependency_injection` Subtopic. No Lesson
  claims another Topic's Subtopic as primary, which is what keeps E27 from altering any other
  Topic's assessment coverage.
- **Overlap.** No Lesson lists the same Subtopic as both primary and supporting, which the
  validator rejects (`PRIMARY_SUPPORTING_SUBTOPIC_OVERLAP`).
- **Shared primaries.** Six Subtopics are primary in more than one Lesson:
  `dependency_graphs` (4), `di_framework_tradeoffs` (4), `di_fundamentals` (2), `di_scopes` (2),
  `dagger_fundamentals` (2), `dagger_components` (2), `hilt_fundamentals` (2) and `manual_di`
  (2). This is the contract's expected shape and matches the shipped Units. The consequences
  for practice are modelled in [Part 6](#part-6--unit-practice-routing-modelled-now) rather
  than left to be discovered.
- **No shipped Unit's practice changes.** **No shipped Lesson takes any
  `dependency_injection` Subtopic as a primary concept**; exactly one takes one as supporting
  (`lesson_dependency_inversion_in_practice`, supporting `service_locator_vs_di`). E27 is
  therefore the first content to claim any of them, and shipping it cannot alter the practice
  pool of any existing Unit.
- **Forward references.** No `relatedLessonIds` value proposed here names a Lesson that does
  not yet ship. See [cross-linking rules](#cross-linking-rules-and-the-intended-link-graph).

---

## Lesson prerequisites, demonstrable reasoning and boundaries

The blueprint holds objectives and depth layers. This section holds what the review had to
settle: the reasoning a learner must be able to demonstrate, where each prerequisite is
already taught, and the boundary that keeps each Lesson from absorbing its neighbour. Every
prerequisite named is a **shipped** Lesson id or an earlier E27 Lesson.

### Unit 1 — `unit_dependency_injection_as_object_construction`

**Prerequisites:** `lesson_dependency_direction_and_boundaries`,
`lesson_when_an_interface_is_a_boundary` and `lesson_dependency_inversion_in_practice`
(shipped, E26). All three are anchors rather than background: L1.2 relies on the boundary
test, L1.3 relies on inversion being already taught in full, and L1.6 relies on
proportionality from `lesson_smallest_sufficient_architecture`.

| Lesson | Demonstrable reasoning | Boundary |
| --- | --- | --- |
| L1.1 | Given a class that needs a collaborator, write it three ways and name what each version makes true of the class | Names no framework beyond one forward sentence. Graphs, lifetimes and containers are Unit 2's and later |
| L1.2 | Given a class with a required collaborator, say what a constructor parameter guarantees that a later assignment does not; then inject a concrete type and say why it is still injection | Stops at visibility and formedness. **Whether an abstraction earns its existence is `lesson_when_an_interface_is_a_boundary`'s** and must be cited, not re-argued. Field injection is L4.3's |
| L1.3 | For one graph, say separately whether the dependency was injected and whether it was inverted, and produce an example of each without the other | Applies inversion; does not teach it. `lesson_dependency_inversion_in_practice` already states that a container changes no import, and this Lesson cites that sentence |
| L1.4 | For a four-object graph, write the assembly at an entry point and identify a feature class that has become a second composition root | Teaches a responsibility and a location. **A container is not a composition root**, and containers are Unit 5's |
| L1.5 | Given a class that looks up a collaborator, name what its signature no longer says; then judge a resolution call at a UI boundary and say why it is a different design | Both halves are required. Taught as "lookup is bad", it makes L4.3 and L5.4 read as defects |
| L1.6 | For a described project, decide whether hand-written wiring is sufficient and name the change that would end that | Judges one project. **The four-way comparison is Unit 6's** and depends on Units 3–5 |

### Unit 2 — `unit_object_graphs_lifetimes_and_scopes`

**Prerequisites:** Unit 1 entire; `lesson_choosing_the_owner_by_lifetime` and
`lesson_viewmodel_lifetime_and_persistence` (shipped, E26). L2.3 additionally assumes
`lesson_state_holder_responsibility` for what an owner is.

| Lesson | Demonstrable reasoning | Boundary |
| --- | --- | --- |
| L2.1 | Given a requested object, draw the transitive graph, name the construction responsibility at each edge and identify the root | Generic vocabulary only. Every framework's graph word is Units 3–5 |
| L2.2 | For each node of a graph, choose reuse or per-request construction and state the requirement that decided it | The decision, not the notation. No scope annotation or DSL keyword appears |
| L2.3 | Given a lifetime requirement, name the owner that must exist for that long and say why declaring a scope creates nothing | **Acceptance-critical.** Applies `lesson_choosing_the_owner_by_lifetime` rather than re-deriving it; adds only what a container changes |
| L2.4 | Given a class needing two graph dependencies and one navigation-time value, route each correctly and say why the third must not be a binding | Names the general factory shape. Assisted injection, saved state and Koin parameters are Units 4 and 5 |
| L2.5 | Given two same-type collaborators, say what the assembler cannot decide and name the two honest ways out | The design question. Qualifiers and named definitions are Units 3 and 5 |
| L2.6 | For a graph with a missing edge, say when the failure becomes observable under each regime and what a successful check did not establish | Generic timing. Compile-time validation is L3.7's instance of it, and build tooling is E29's |

### Unit 3 — `unit_dagger_compile_time_object_graphs`

**Prerequisites:** Units 1 and 2 entire. No Dagger API is introduced before the Unit 2
question it answers.

| Lesson | Demonstrable reasoning | Boundary |
| --- | --- | --- |
| L3.1 | Say what an annotated constructor causes the build to produce, and why no runtime reflection is involved | Generated construction. **Build configuration is E29's** and this Lesson says only that a build step exists |
| L3.2 | For three types Dagger cannot construct directly, choose a declaration form for each and justify it from what each can express | The selection rule. Not a syntax tour, and never "two ways to do the same thing" |
| L3.3 | Read a component and say what it can satisfy, what it cannot, and where the program crosses into the graph | The component as root and API. Android components are Unit 4's |
| L3.4 | Given a feature needing part of another graph, choose nesting or dependency and justify it by what each can see | Visibility. **A component boundary is not a build module boundary**, stated once |
| L3.5 | Count the instances an annotated binding produces across two components, and name the object whose lifetime bounds each | Component-instance reuse, consistent with L2.3. Android lifetimes are Unit 4's |
| L3.6 | Encode L2.5's ambiguity, then invert the dependency between a consumer and several contributors | Key mechanics. The full multibinding surface is Reference |
| L3.7 | State what the build verified, then show a graph that passes and is architecturally wrong | **The misconception 14 Lesson.** Build performance and processor configuration are E29's |

### Unit 4 — `unit_hilt_android_lifecycle_integration`

**Prerequisites:** Unit 3 entire; `lesson_viewmodel_lifetime_and_persistence` and
`lesson_choosing_the_owner_by_lifetime` (shipped, E26) for what a ViewModel's owner decides.

| Lesson | Demonstrable reasoning | Boundary |
| --- | --- | --- |
| L4.1 | Given a Hilt setup, separate what is Dagger from what is Hilt's convention | Does not re-teach Dagger. Setup and plugins are E29's |
| L4.2 | Given a lifetime requirement in Android terms, name the component and scope that satisfy it and the cost of a wider one | **Source-sensitive.** Only the components a requirement needs; the lifecycle curriculum owns Android lifecycle |
| L4.3 | Explain why a platform-created class cannot use constructor injection, and when reaching the graph from unmanaged code is legitimate | Field injection as a consequence, never as a preference. Platform-component mechanics stay out |
| L4.4 | Distinguish a retained-scoped dependency from a ViewModel-scoped one by what a second ViewModel receives, and route a navigation-time value | **Acceptance-critical.** Applies the shipped ViewModel lifetime material; the saved-state API surface is the lifecycle curriculum's |
| L4.5 | Move a module between components and state the visibility change; then add a scope and state that visibility did not change | Two orthogonal decisions. Multi-module installation strategy is E29's |
| L4.6 | For two described projects, choose between the convention and a hand-written structure and name what each costs | No verdict. **The four-way comparison is Unit 6's** |

### Unit 5 — `unit_koin_and_dependency_injection_in_kmp`

**Prerequisites:** Units 1–2 entire; Unit 3 for binding and graph vocabulary;
`kmp_shared_viewmodel_owner_platform`'s conclusion as taught by the shipped architecture
material — that the owner which creates and clears a ViewModel is platform-supplied.

| Lesson | Demonstrable reasoning | Boundary |
| --- | --- | --- |
| L5.1 | Read a startup function and a set of modules and describe the graph and the composition root they define | Container, module, startup. **The compiler plugin is a bounded Reference note**, and its setup is E29's |
| L5.2 | Encode L2.2's reuse decisions as definitions and correct the three misreadings of the retained keyword | Definitions from requirements. The full DSL surface is Reference |
| L5.3 | Decide whether a requirement needs a bounded scope and name the owner that must create and close it | Only as far as the lifetime model needs. The scope API is not toured |
| L5.4 | Say what the container decided and what the host decided when a screen resolves a ViewModel, and pass a runtime value correctly | **Construction is not store ownership.** Navigation and Compose mechanics stay out |
| L5.5 | Split a graph into shared definitions and per-platform bindings, and describe what each host's composition root adds | **E33 owns source-set architecture**; only the platform-binding pattern the graph needs appears |

### Unit 6 — `unit_choosing_a_dependency_injection_strategy`

**Prerequisites:** Units 1–5 entire, and `lesson_smallest_sufficient_architecture` (shipped,
E26) for proportionality, which L6.4 applies rather than re-derives.

| Lesson | Demonstrable reasoning | Boundary |
| --- | --- | --- |
| L6.1 | Name the axes and state each as a requirement making a property valuable at a cost | No scorecard, no ranking, no winner row |
| L6.2 | State when each strategy's configuration is checked, and what earlier checking does not prove | **Source-sensitive**; the claim depends on the mechanism, not the library's name. Setup is E29's |
| L6.3 | For three described projects, choose a strategy and defend it from the requirements | No scenario engineered to produce a predetermined answer |
| L6.4 | Choose the smallest sufficient strategy and name the change that would justify the next one | Applies `lesson_smallest_sufficient_architecture`; organisational change management stays out |

---

## Cross-linking rules and the intended link graph

`LearningCurriculumValidator` rejects a `relatedLessonIds` entry naming an unknown Lesson
(`UNKNOWN_RELATED_LESSON`), and Lesson ids are resolved across the whole document rather than
within a Unit. **Forward links are therefore invalid until their target ships.**

- Each authoring issue may link **backwards** only: to the 101 shipped Lessons and to Lessons
  shipped by an earlier E27 issue.
- Pointing forward is a prose sentence naming the Unit. The issue that ships that Unit may
  then add the reciprocal link from its own Lessons.
- **E27 requires no edit to any shipped Lesson in order to receive a link.** This was
  checked: the three shipped Lessons that point at this curriculum —
  `lesson_state_holder_responsibility` ("the dependency-injection curriculum owns it"),
  `lesson_dependency_inversion_in_practice` ("belong entirely to the dependency-injection
  curriculum") and `lesson_screen_state_owner_boundary` — all name it in prose without
  asserting that it does not exist. **E27-09 should re-read all three** and confirm they still
  read correctly once the Units ship; none is expected to need editing, and adding a backward
  link from them is optional rather than required.

The intended final graph, recorded here so the authoring issues do not have to invent it:

| From | To | Direction | Shipped by |
| --- | --- | --- | --- |
| L1.2 | `lesson_when_an_interface_is_a_boundary` | backward | E27-02 |
| L1.3 | `lesson_dependency_inversion_in_practice`, `lesson_dependency_direction_and_boundaries` | backward | E27-02 |
| L1.4 | `lesson_what_architecture_decides` | backward | E27-02 |
| L1.6 | `lesson_smallest_sufficient_architecture`, `lesson_layers_and_their_cost` | backward | E27-02 |
| L2.1 | Unit 1 L1.4 | backward, within E27 | E27-03 |
| L2.2, L2.3 | `lesson_choosing_the_owner_by_lifetime`, `lesson_viewmodel_lifetime_and_persistence`, `lesson_state_holder_responsibility` | backward | E27-03 |
| L2.4 | Unit 1 L1.2 | backward, within E27 | E27-03 |
| L3.1 | Unit 1 L1.4; Unit 2 L2.1 | backward, within E27 | E27-04 |
| L3.5 | Unit 2 L2.3 | backward, within E27 | E27-04 |
| L3.6 | Unit 2 L2.5 | backward, within E27 | E27-04 |
| L3.7 | Unit 2 L2.6 | backward, within E27 | E27-04 |
| L4.2 | Unit 2 L2.3; Unit 3 L3.5 | backward, within E27 | E27-05 |
| L4.3 | Unit 1 L1.2, L1.5 | backward, within E27 | E27-05 |
| L4.4 | `lesson_viewmodel_lifetime_and_persistence`; Unit 2 L2.4 | backward | E27-05 |
| L5.1 | Unit 1 L1.4 | backward, within E27 | E27-06 |
| L5.2 | Unit 2 L2.2; Unit 1 L1.5 | backward, within E27 | E27-06 |
| L5.3 | Unit 2 L2.3 | backward, within E27 | E27-06 |
| L5.4 | `lesson_state_holder_responsibility`; Unit 1 L1.5; Unit 2 L2.4 | backward | E27-06 |
| L5.5 | Unit 1 L1.4; `lesson_dependency_inversion_in_practice` | backward | E27-06 |
| L6.1–L6.3 | at least one Lesson from each of Units 1–5 | backward, within E27 | E27-07 |
| L6.4 | `lesson_smallest_sufficient_architecture`; Unit 1 L1.6 | backward | E27-07 |

---

## Part 3 — The E26 → E27 handoff ledger

E26 recorded its deferrals to this epic in `docs/content/architecture-units-1-6-plan.md` and
in `docs/content/architecture-learning-blueprint.md`, and E26-09's closure review confirmed
each one held in the shipped prose. **Every deferral is accounted for below, concept by
concept.** This table is what E27-09's acceptance criterion "each construction responsibility
the architecture epic deferred to this epic is answered by a shipped Lesson or recorded as
still deferred with its reason" is checked against.

| Deferred by E26 | Where it was deferred | Owned in E27 by |
| --- | --- | --- |
| How a screen-level owner is **constructed** | `lesson_state_holder_responsibility`, which states that construction "by a factory, by a container, by the caller — is a separate decision with its own trade-offs, and the dependency-injection curriculum owns it" | **L1.1** and **L1.4** generically; **L4.4** and **L5.4** for the two framework encodings |
| How a component's dependencies **reach** it | E26's Unit 2 and Unit 4 Exclude notes | **L1.2** (the constructor), **L1.4** (assembly), **L4.3** (when the platform owns construction) |
| Dependency injection as distinct from dependency inversion, from the injection side | `lesson_dependency_inversion_in_practice` teaches the distinction from the inversion side and stops: "which container, how it is configured, what a module or a binding or a scope or a qualifier is, and how a graph is assembled belong entirely to the dependency-injection curriculum" | **L1.3**, which cites that Lesson and supplies the injection-side examples — injection without inversion, and inversion without a container |
| Containers, modules, bindings, graph assembly, scopes and qualifiers | E26 blueprint's Excluded Material, and L4.4's Notes | **Units 2–5 entire** |
| Dagger | E26 Part 14 | **Unit 3** |
| Hilt | E26 Part 14 | **Unit 4** |
| Koin, and the fact that this repository uses the classic DSL with ViewModels resolved at destinations | E26 Part 7's evidence row, "named once in L2.1 as *how this app supplies an owner*, and otherwise E27's" | **Unit 5 entire**, with the graph as the worked case study |
| Injected construction of any owner | E26 Part 14's boundary row | **L4.4** (Hilt) and **L5.4** (Koin) |
| Multiplatform ViewModel construction: the finding that on non-JVM targets `viewModel()` cannot be called without parameters in common code and an initializer must be supplied | E26 Part 8, Finding 5, recorded verbatim as "a **construction** detail, which is E27's territory" | **Bridged in L5.4, not taught.** The Lesson says what the resolution call obtains and who owns the store; it does not teach the multiplatform ViewModel factory API, which stays with E33 and the lifecycle curriculum. Recorded as a deliberate partial answer |
| That the owner which creates and clears a ViewModel is platform-supplied | E26 Part 8, Findings 1–4, and `kmp_shared_viewmodel_owner_platform` | **Applied in L5.4**, cited rather than re-derived |
| A Question on inversion against injection, which E26-05 recommended leaving to E27 and E26-08 did not author | E26-08's source-verification note | **GAP-U1-A**, which is E27-08's to dispose of |
| `service_locator_vs_di` as a **primary** concept — supporting-only throughout E26 | E26's mapping decision | **L1.5 claims it as primary**, which gives it Unit practice for the first time |

**Three E26 conclusions E27 must carry forward without weakening:**

1. **A container changes no import, so it inverts nothing.**
   `lesson_dependency_inversion_in_practice` states it and
   `interface_with_one_implementation_is_not_a_boundary` assesses it. L1.3 applies it and must
   cite rather than re-derive.
2. **An owner's lifetime decides what survives, and lifetime is not persistence.**
   `lesson_viewmodel_lifetime_and_persistence` and `lesson_choosing_the_owner_by_lifetime`
   establish it. L2.3, L4.2 and L4.4 all depend on it and none may reintroduce "the singleton
   survives" as shorthand.
3. **Structure earns its cost or it is removed.** `lesson_smallest_sufficient_architecture` is
   the closing argument of E26 and L6.4 is the closing argument of E27; the second is the same
   reasoning applied to wiring, and must be recognisable as such.

**What E27 does not inherit.** E26 deferred queueing architecture, the `SavedStateHandle`
mechanism and build-module structure to other curricula, not to this one. None of them appears
here, and E27-09 should confirm that none has drifted in.

---

## Part 4 — Recurring worked examples

Three examples carry across the six Units. They exist so the reader watches one graph grow
rather than meeting thirty unrelated toy classes, and so Unit 6's comparison has concrete
projects to compare. **Every example is framed as an example**, per Rule 11 of the authoring
contract: type names are used freely, and no Lesson makes this application the subject.

### Example A — a small graph that can genuinely be wired by hand

**Used by:** Units 1 and 2. **Introduced:** L1.1. **Grown:** L1.4, L2.1.

```text
StudySessionController
    ├── QuestionRepository
    └── Clock
```

Then, as Unit 1 proceeds: `QuestionRepository` acquires a `QuestionDataSource`, the data
source acquires a configuration value, and an `AttemptRecorder` joins in L2.1 so the graph has
a shared node. Constraints on the example:

- It starts with **concrete** types, and an interface appears only in L1.3 where a real
  boundary is being demonstrated. This is what makes "injection does not require interfaces"
  demonstrable rather than asserted.
- It is assembled explicitly, in an entry point, and the assembly code is shown growing.
- It is small enough that hand-wiring is genuinely the right answer, which is what L1.6 needs.
- It never requires a framework, and no framework name appears in it.

The service-locator contrast in L1.5 is the **same** graph with one class fetching its
repository from a registry, so the reader compares two versions of something they already
understand.

### Example B — a growing Android-only graph

**Used by:** Units 2, 3, 4 and 6. **Introduced:** L2.2.

A reading application with an application-lifetime authentication session and HTTP client, a
screen-level state owner per destination, a per-screen object that must reset when the screen
does, a dependency shared by a screen and everything under it across a rotation, two
configured instances of one client type, and several independently developed features each
contributing a startup initialiser. Constraints:

- Every element exists because a Unit needs it, not because an annotation needs demonstrating.
  The two clients exist for L2.5 and L3.6; the initialisers exist for L3.6; the rotation
  requirement exists for L4.2.
- It is not inflated to make hand-wiring look impossible. L1.6's judgement that a small graph
  needs no container has to survive this example's existence, and Unit 6 states plainly that
  this one grew past that point for reasons a reader can name.
- It is Android-only on purpose, so Unit 5's multiplatform constraints are a genuine change of
  problem rather than a variation.

### Example C — a multiplatform application's own graph

**Used by:** Units 5 and 6. **Source:** this repository, read as evidence.

A Kotlin Multiplatform application with shared repositories, shared state holders and shared
ViewModels, whose four hosts each start the graph with the shared modules plus their own
platform modules. The complete evidence matrix — which file establishes which fact, and what
each does and does not demonstrate — is
[Part 7](#part-7--this-repositorys-own-koin-graph-as-evidence).

**Framing rule for Example C**, following the precedent E26 set in
`lesson_dependency_inversion_in_practice`: the Lesson writes "consider a multiplatform
application that…" and then uses the real structure, the real module names and the real
startup functions. It never writes "this app", "this repository" or "the current
implementation". Naming a real arrangement is what makes the example teachable; claiming it as
this product's own is what Rule 11 forbids.

---

## Part 5 — Semantic assessment review

Every ACTIVE Question in the `dependency_injection` Topic was read in full — stem, options,
correct set, explanation and Sources — and independently solved, not counted. There are **23**,
spread over 18 of the Topic's 25 Subtopics. All **4** DEPRECATED Questions were read as
context, because a deprecated Question still occupies its concept.

**Nothing below claims that an unpublished Lesson makes a Question answerable.** "Planned"
means this plan places the required reasoning in a named Lesson; whether the finished prose
delivers it is a judgement E27-02…E27-07 make at authoring time and E27-08 re-checks.

**Every one of the 23 is technically sound.** No Question was found with a wrong key, and the
review produced no defect that makes a Question unanswerable. What it produced is four kinds
of finding: two mappings that route a framework Question into a generic Unit, one option
whose wording will contradict what L2.3 teaches, two explanations whose framework claims have
been overtaken by current documentation, and a large number of Questions that are definitional
where the curriculum will teach a decision.

### Generic and manual dependency injection

| Question | Level | Subtopic | Lesson(s) | Reasoning the Question requires | Finding | Disposition candidate |
| --- | --- | --- | --- | --- | --- | --- |
| `di_constructor_injection_testability` | Foundation | `di_fundamentals` | L1.2 | That constructor parameters make requirements visible, produce a fully-formed object, and permit substitution — and that they tell a container nothing about how to build anything | **The best generic Question in the bank**, and the only one that separates what constructor injection does from what a framework still needs. Its fourth option is the sharpest thing in the Topic. **Mapping mismatch:** the reasoning is `constructor_injection`'s, and the Subtopic is `di_fundamentals`, so it reaches Unit 1 through L1.1/L1.3 rather than through L1.2, which is the Lesson that teaches it | Keep; **re-map candidate** to `constructor_injection`, paired with GAP-U1-E |
| `composition_root_001` | Foundation | `composition_root` | L1.4 | That assembly is centralised near the entry point, that a queried registry is a service locator instead, and that caching is a separate concern | Sound and correctly mapped. **Definitional** — it asks what a composition root is, never where assembly belongs for a stated program or whether a given class has become a second one | Keep; see GAP-U1-C |
| `service_locator_vs_di_001` | Foundation | `service_locator_vs_di` | L1.5 | That reaching into a registry removes the dependency from the signature, hiding it from callers and tests, and moves failure to run time | Sound, correctly mapped, and its explanation already contains the visibility argument L1.5 teaches. **Definitional and one-sided** — it assesses the downside of a locator and never poses the qualification the curriculum needs, which is that resolution at an integration boundary is a different design | Keep; see GAP-U1-D |
| `manual_di_graph_growth_cost` | Applied | `manual_di` | L1.6, L2.1 | That the cost which grows is clerical — threading a parameter through every construction site — and that manual wiring loses no capability: scoping, sharing and constructor injection all survive | **Excellent, and the single most important Question in the Topic for this epic's thesis.** Its explanation already refuses three of the misconceptions Unit 1 exists to correct, in its own words | Keep; L1.6 must teach enough reasoning to make it answerable without quoting it. **Routing consequence:** `manual_di` is primary in L6.4 too, so it is also Unit 6 practice |
| `di_scopes_001` | Foundation | `di_scopes` | L2.2, L2.3 | That a scope decides how long provided instances live and who shares them, and that mis-scoping leaks in one direction and rebuilds in the other | Sound as far as it goes, and its explanation's two-directional framing is exactly L2.2's. **Two problems.** It is a statement-recognition Question rather than a decision. And its keyed option reads "A scope controls the lifetime and sharing boundary of provided instances", which is the wording L2.3 exists to correct: the owner decides the lifetime and the scope is a reuse rule within it. A learner who has read L2.3 could reasonably hesitate over the keyed option | Keep the Question; **wording-correction candidate**, and see GAP-U2-A and GAP-U2-B |
| `hilt_field_injection_framework_classes` | Foundation | `constructor_injection` | L4.3 | That the platform instantiates an Activity through a no-argument constructor, so Hilt populates annotated fields at a documented lifecycle point, and that constructor injection stays the default where the app owns construction | **Sound and well written** — its last sentence is L4.3's thesis. **Premature routing, and the clearest in the Topic.** Its stem, options and explanation are entirely about Hilt, and its Subtopic is a generic Unit 1 primary, so a reader who has finished Unit 1 meets a Hilt Question three Units early, while Unit 4 — whose L4.3 teaches exactly this — never reaches it | Keep; **strongest re-map candidate in the Topic**, to `hilt_fundamentals` |

### Dagger

| Question | Level | Subtopic | Lesson(s) | Reasoning the Question requires | Finding | Disposition candidate |
| --- | --- | --- | --- | --- | --- | --- |
| `dagger_generated_factory_no_reflection` | Foundation | `dagger_fundamentals` | L3.1 | That an annotated constructor produces a generated factory at build time, that the component implementation wires factories together, and that there is neither runtime annotation reading nor bytecode rewriting | Sound, correctly mapped, and unusually honest: its explanation refuses "zero overhead" as well as "reflection", which is the distinction L3.1 needs. Verified against the Dagger basic-usage page's compile-time code-generation section | Keep |
| `dagger_inject_provides_binds_selection` | Foundation | `dagger_bindings` | L3.2 | That an annotated constructor binds a type you own, that an abstract binding maps an abstraction to an already-injectable implementation, and that a provider method handles construction logic and third-party types | Sound and precisely matched to L3.2's selection rule. Its two distractors are the two real errors. **Level looks low** for a four-option multiple-selection Question requiring three separate constraint judgements | Keep; **level-review candidate** |
| `dagger_component_graph_root` | Foundation | `dagger_components` | L3.3 | That a component connects bindings into a graph and exposes entry points, while a module only contributes declarations | Sound and correctly mapped. **Definitional** — a role-attribution Question rather than a reading of a specific graph | Keep; see GAP-U3-A |
| `dagger_subcomponent_parent_binding_inheritance` | Foundation | `dagger_components` | L3.4 | That a subcomponent sees every parent binding, may add its own and a narrower scope, and that visibility does not flow upward | Sound and precisely matched. Verified against the Dagger subcomponents page, which states that a bound object "can depend on any object that is bound in its parent component or any ancestor component" and that the reverse does not hold | Keep |
| `dagger_component_dependency_vs_subcomponent` | Applied | `dagger_components` | L3.4 | That a subcomponent inherits the whole parent graph while a component dependency can use only the types the other component publishes on its interface | **The best Dagger Question in the bank**, and exactly L3.4's contract, including the cost it names. The claim about provision methods is supported by the basic-usage page's list of what a component can provide | Keep |
| `dagger_scope_component_instance_lifetime` | Foundation | `dagger_scopes` | L3.5 | That the owning component instance caches the scoped result, so a second component produces a second instance and releasing the component releases them | **Excellent, and the closest existing match to any E27 Lesson's contract.** It is misconception 9 assessed directly, and its distractor set names the three wrong owners. Verified against the basic-usage statement that Dagger "associates scoped instances in the graph with instances of component implementations". **Level looks low** for reasoning that is the Unit's hardest idea | Keep; **level-review candidate**, and cite rather than re-derive in L3.5 |
| `dagger_qualifier_same_type_bindings` | Applied | `dagger_qualifiers` | L3.6, L2.5 | That a key is a type plus a qualifier, so distinct qualifiers disambiguate and each consumer must state which it needs; and that scopes control reuse rather than selection | Sound and precisely matched to L3.6. Its distractors kill declaration order and module order, which is L2.5's non-answer list | Keep |
| `dagger_multibinding_into_set` | Applied | `dagger_multibindings` | L3.6 | That independent contributors each add to one collection the consumer injects without naming any of them, and that this inverts the dependency | Sound, precisely matched, and its explanation already states the dependency-direction point L3.6 teaches. Verified against the Dagger multibindings page | Keep |
| `dagger_compile_time_graph_validation` | Foundation | `dependency_graphs` | L3.7, L2.6 | That Dagger resolves every reachable key while compiling a component, so a missing binding and an ambiguous key are both compile diagnostics | Sound, and verified against the basic-usage statement that "all validation of the relationship between bindings happens at the `@Component` level". **Premature routing.** Its stem and every option are Dagger-specific, and `dependency_graphs` is a generic Unit 2 primary in four Lessons, so it is the **only Question Unit 2 practice reaches besides `di_scopes_001`** — and it arrives before Dagger exists | Keep; **re-map candidate** to `dagger_fundamentals`. See [the routing discussion](#part-6--unit-practice-routing-modelled-now) |

### Hilt

| Question | Level | Subtopic | Lesson(s) | Reasoning the Question requires | Finding | Disposition candidate |
| --- | --- | --- | --- | --- | --- | --- |
| `hilt_entry_point_manual_access` | Applied | `hilt_fundamentals` | L4.3 | That the supported-class list is fixed and excludes content providers, that an entry-point interface plus an accessor reaches the graph from unmanaged code, and that Hilt creates no component for the provider | Sound and correctly mapped, and its reasoning is L4.3's escape-hatch half. **Source-sensitivity:** the supported-class list it rests on is the one that has changed most on the Android page — see [the Hilt findings](#hilt-findings) — although the content-provider exclusion it turns on is unchanged | Keep; re-verify sources in E27-08 |
| `hilt_activity_retained_component_lifetime` | Applied | `hilt_components` | L4.2 | That the retained component spans configuration recreation while the activity component does not, and that the singleton component would work but widens the dependency to the whole process | Sound, well matched to L4.2, and its explanation already contains the "bindings flow down, never up" rule. **Source-sensitivity:** its stem is framed around an Activity "and its Fragments" and one distractor names `FragmentComponent`, which the Android page no longer documents though the Hilt library still defines it. The Question remains correct; the framing now needs a source that says so | Keep; **source-freshness candidate** — the Sources should name the library documentation, not only the Android page |
| `di_hilt_viewmodel_scope` | Foundation | `hilt_viewmodels` | L4.4 | That the annotation governs how the ViewModel is constructed and retained, while the lifetime of what it receives is decided by that binding's own scope | **Excellent, and exactly the L4.4 confusion**, stated in its explanation as "conflating the two is the usual source of surprise". The best-matched Question in Unit 4 | Keep |
| `dagger_assisted_injection_viewmodel` | Applied | `hilt_viewmodels` | L4.4, L2.4 | That some constructor parameters belong to the graph and some are known only at the call site, so a factory takes exactly the assisted ones and the graph supplies the rest — and that a per-instance runtime value does not go into the graph | Sound and precisely matched to L4.4's second half; the last sentence is L2.4's thesis. Verified against the Dagger assisted-injection page. The `dagger_` prefix against a Hilt Subtopic is cosmetic and not a defect | Keep |
| `hilt_install_in_binding_visibility` | Foundation | `hilt_modules` | L4.5 | That installation decides which component receives the bindings and therefore where they are injectable, that lifetime is a separate decision expressed by a scope, and that the hierarchy is fixed | **Excellent, and exactly L4.5's contract** — it is the visibility-against-lifetime orthogonality assessed directly. **Source-sensitivity:** its explanation says bindings in the activity component are injectable into "an Activity and the Fragments and Views under it", which the Android page no longer supports | Keep; **source-freshness candidate** |
| `hilt_vs_dagger_convention_tradeoff` | Applied | `hilt_vs_dagger` | L4.6, L4.1 | That Hilt supplies a standard Android-lifetime hierarchy and removes the ability to define your own, and that underneath it is still compile-time Dagger with modules, scopes and constructor injection | Sound, precisely matched, and it refuses the "Hilt is runtime" distractor, which is L4.1's misconception. **Source-sensitivity:** its explanation names a hierarchy "from SingletonComponent down to FragmentComponent and ViewComponent" | Keep; **source-freshness candidate** |

### Koin

| Question | Level | Subtopic | Lesson(s) | Reasoning the Question requires | Finding | Disposition candidate |
| --- | --- | --- | --- | --- | --- | --- |
| `di_koin_factory_vs_single` | Foundation | `koin_definitions` | L5.2, L2.2 | That one keyword keeps a single instance for the container's lifetime and the other constructs per resolution, and that choosing the shared one for per-screen state is how unintended sharing appears | Sound, correctly mapped, and its last sentence is L2.2's failure mode. **Two findings.** It is definitional — it asks what the difference is, never which a stated requirement needs. And its explanation asserts that "both resolve at runtime, since Koin builds its graph without code generation", which is accurate for the classic DSL and **no longer accurate as a statement about Koin**, whose current compiler plugin generates the DSL and verifies the configuration at compile time | Keep; **correction candidate** for the explanation, and see GAP-U5-D |
| `koin_multiplatform_common_module` | Applied | `koin_multiplatform` | L5.5, L6.3 | That annotation processors emitting JVM code cannot process `commonMain` for a native target and that Hilt is additionally tied to Android classes, while a plain Kotlin DSL compiles wherever Kotlin does | **Excellent, and exactly L5.5's Senior point.** Its distractors kill reflection, per-platform generation and the `Context` requirement, which are the three things people assume. **One finding:** its explanation states the trade-off as "a missing binding surfaces as a runtime failure rather than a compile error", which is true of the mechanism this repository uses and is now a mechanism-scoped rather than a framework-scoped claim | Keep; **correction candidate** — the claim needs qualifying, not replacing |

### The four DEPRECATED dependency-injection Questions, read as context

All four are DEPRECATED in the bundled curriculum and each occupies a concept this epic
teaches. **None should be restored as written**, and the reasoning each held is accounted for.

| Question | Subtopic | Responsibility it held | Why it is not needed as written | Does the concept still need assessing? |
| --- | --- | --- | --- | --- |
| `constructor_injection_001` | `constructor_injection` | Why constructor injection is preferred for required dependencies: visibility at construction time, immutable properties, a fully-formed object, and the contrast with internal resolution | **Superseded.** `di_constructor_injection_testability` assesses the same four properties, as a multiple-selection Question with a sharper distractor set, and adds the "a framework still needs a binding declaration" point this one lacked | Yes, and it already is — but through a Question mapped to `di_fundamentals`. This is the pairing behind the re-map candidate and GAP-U1-E |
| `di_framework_tradeoff_compile_vs_runtime` | `di_framework_tradeoffs` | That generating a graph at build time turns a missing binding into a build failure rather than a first-resolution failure, at the cost of build work; and that both styles express scopes and neither resolves a cycle | **The concept is central to L6.2 and the wording is now unsafe.** Its framing of "a reflection-free, compile-time DI framework" against "a runtime service-registry one" is the framework-level dichotomy the epic explicitly forbids, and current Koin makes it inaccurate. Its subsidiary points — both styles express scopes, neither fixes a cycle — remain true and useful | **Yes, and this is the epic's largest gap.** `di_framework_tradeoffs` holds no ACTIVE Question. E27-08 should author the reasoning fresh, framed by mechanism rather than by library. See GAP-U6-A |
| `dagger_module_binding_declarations` | `dagger_modules` | That a module declares binding methods while a component owns the graph instance, exposes entry points and retains scoped objects | **Not superseded.** `dagger_component_graph_root` assesses the component side of the same contrast, so a reader can reach the distinction, but nothing assesses the module's own role. Its wording is sound and its Source is current | Yes, weakly. `dagger_modules` is the only Subtopic that is primary in a Lesson and holds no ACTIVE Question. See GAP-U3-A |
| `dagger_graph_assembly_generated_component` | `dependency_graphs` | How Dagger assembles a graph into usable code: resolving requested keys through constructor and module bindings and emitting a component implementation, rather than reflecting at run time | **Superseded.** `dagger_generated_factory_no_reflection` assesses the same reasoning with a better explanation, and `dagger_compile_time_graph_validation` covers the resolution half. Its Subtopic is also the generic `dependency_graphs`, which is the routing problem this plan is trying to avoid | No. The concept is covered twice under `dagger_fundamentals` |

**Do not restore any of the four.** Where the reasoning is still needed it is recorded as a
gap, so E27-08 authors it against the finished Lessons rather than reviving retired wording.

---

## Assessment gaps for E27-08

Substantive gaps only. There is no per-Lesson or per-level quota, and a gap here is a
**candidate for authoring rather than an automatic defect**. Each gap names the reasoning that
is missing, not a number. Gap ids are stable and E27-08 disposes of each one explicitly.

| Gap | Unit / Lesson | Reasoning no ACTIVE Question assesses | Why it is substantive | Recommended action |
| --- | --- | --- | --- | --- |
| GAP-U1-A | 1 / `lesson_injected_inverted_or_both` | Distinguishing an injected dependency from an inverted one **in the same graph** — identifying a constructor-injected dependency whose source direction is still wrong, and an inverted one with no container anywhere | Nothing in any Topic poses it. `architecture_solid_dependency_substitution` and `interface_with_one_implementation_is_not_a_boundary` both assert the distinction inside an explanation and assess something else. **E26-05 recommended leaving this to E27 and E26-08 deliberately did not author it**, so it arrives here as an open item rather than a new discovery | Add coverage in E27-08; **the epic's clearest single gap in Unit 1** |
| GAP-U1-B | 1 / `lesson_who_constructs_this_object` | Choosing between constructing a collaborator, fetching it and receiving it, for a stated class, and naming what each choice makes true | `di_fundamentals`'s only Question assesses the consequences of constructor injection once the choice has been made. The choice itself — and with it misconceptions 1 and 2 — is unassessed | Add coverage in E27-08 |
| GAP-U1-C | 1 / `lesson_one_place_that_knows_how_to_build` | Recognising construction knowledge that has leaked into a feature class, and saying where it belonged | `composition_root_001` is definitional. Nothing makes the reader locate the responsibility in a specific program, which is the only form in which "a composition root is a class called AppContainer" is corrected | Add coverage in E27-08 |
| GAP-U1-D | 1 / `lesson_asking_for_it_or_being_given_it` | Judging two resolution calls — one in a domain class, one at a UI or platform integration boundary — and saying why only one is the service-locator shape | `service_locator_vs_di_001` assesses the downside of a locator and stops. Without the qualification, Unit 4's field injection and Unit 5's Compose resolution both read as defects, which is a contradiction the curriculum cannot ship with | Add coverage in E27-08; **strong candidate**, and the one most specific to this curriculum's argument |
| GAP-U1-E | 1 / `lesson_a_dependency_should_be_visible` | Generic constructor injection assessed on its own terms, reachable from the Lesson that teaches it | **`constructor_injection`'s one ACTIVE Question is entirely about Hilt field injection**, and the generic Question that assesses this reasoning sits under `di_fundamentals`. Whichever way E27-08 resolves the two re-map candidates, one of the two Subtopics ends up needing a Question | **Decide the two re-maps first**, then add coverage only for what they leave open |
| GAP-U2-A | 2 / `lesson_one_instance_or_a_new_one` | Choosing an instance-reuse policy from a stated lifetime or sharing requirement, for a specific dependency | `di_scopes_001` states four facts about scopes and asks the reader to recognise two of them. Nothing makes the reader decide, which is the whole of L2.2 | Add coverage in E27-08 |
| GAP-U2-B | 2 / `lesson_scope_is_a_rule_owner_is_a_lifetime` | That a scope declaration creates no owner — naming the object that must exist for the required lifetime, generically | `dagger_scope_component_instance_lifetime` assesses exactly this reasoning **in Dagger terms** and routes to Unit 3. A Unit 2 reader meets no Question on the epic's most acceptance-critical idea | Add coverage in E27-08; **the widest gap in Unit 2** |
| GAP-U2-C | 2 / `lesson_runtime_input_is_not_a_dependency` | Separating a runtime input from a graph dependency, generically, and naming what goes wrong when the first becomes a binding | `dagger_assisted_injection_viewmodel` assesses it in Dagger and Hilt terms and routes to Unit 4. The distinction recurs in three framework Units and is assessed only inside one of them | Add coverage in E27-08; **strong candidate** |
| GAP-U2-D | 2 / `lesson_two_dependencies_of_the_same_type` | Reading same-type ambiguity as a design question and choosing between distinct types and a distinguished key, before any mechanism | `dagger_qualifier_same_type_bindings` is the Dagger mechanism and routes to Unit 3 | Add coverage in E27-08 if capacity allows; lower priority, since a reader who can answer the Dagger Question is close |
| GAP-U2-E | 2 / `lesson_when_a_broken_graph_tells_you` | When a broken graph becomes observable, as a property of how the graph is described rather than of a named library | `dependency_graphs`'s one ACTIVE Question is Dagger-specific, which is also the premature-routing finding. Reframed generically this is also Unit 6's axis, so one Question could serve both — which is a reason to write it carefully, not to write two | Add coverage in E27-08; **coordinate with GAP-U6-A** |
| GAP-U2-F | 2 / `lesson_from_one_dependency_to_a_graph` | Tracing a transitive graph from a root and naming the construction responsibility at each edge | Nothing in any Topic poses it. Lower priority than the gaps above, because a reader who can do L2.2 through L2.6 has demonstrated it implicitly | Add coverage in E27-08 only if it can pose a reading task rather than a definition |
| GAP-U3-A | 3 / `lesson_declaring_the_rest_of_the_graph` | What a module contributes, as distinct from what a component assembles and owns | **`dagger_modules` is the only Subtopic that is primary in a Lesson and holds no ACTIVE Question**, and its one Question is DEPRECATED. `dagger_component_graph_root` assesses the contrast from the component side only | Add coverage in E27-08. **Must not re-ask the retired wording** |
| GAP-U3-B | 3 / `lesson_what_the_dagger_compiler_checked` | That a graph which compiles can still be architecturally wrong — naming what the check established and what it did not | `dagger_compile_time_graph_validation` assesses **when** errors are reported and stops. Misconception 14 is the Unit's closing argument and is unassessed | Add coverage in E27-08; **strong candidate** |
| GAP-U4-A | 4 / `lesson_hilt_viewmodels_and_runtime_input` | That a ViewModel-scoped dependency is per-ViewModel while a retained-scoped one is shared across ViewModels, and choosing between them from a sharing requirement | `hilt_activity_retained_component_lifetime` compares the retained component against the activity and singleton components, never against the ViewModel component. `di_hilt_viewmodel_scope` assesses that the binding's own scope decides, without making the reader choose one. Misconception 23 is unassessed | Add coverage in E27-08; **the widest gap in Unit 4** |
| GAP-U4-B | 4 / `lesson_when_android_owns_construction` | That field injection exists because construction responsibility differs, and is not a convenience — with constructor injection reaffirmed wherever the application owns construction | The reasoning **is** assessed, by `hilt_field_injection_framework_classes`, which routes to Unit 1. If the re-map is made, this gap closes with no authoring; if it is not, Unit 4 has no field-injection coverage at all | **Decide the re-map first.** Author only if it is declined |
| GAP-U4-C | 4 / `lesson_which_android_component_owns_this` | That an application-lifetime binding is bounded by the process and is not durable state | Misconception 22 is explicitly named in E27-05's acceptance criteria and is unassessed here. `android_process_model_001` assesses the process fact in another Topic, from the storage side | Add coverage in E27-08, from the component-lifetime side rather than the storage side |
| GAP-U5-A | 5 / `lesson_the_koin_container_and_its_modules` | What the container is, what a module groups, and where startup belongs — read as the composition root of L1.4 | **`koin_fundamentals` holds zero ACTIVE Questions.** The Unit's opening Lesson is reached by nothing | Add coverage in E27-08 |
| GAP-U5-B | 5 / `lesson_koin_scopes_and_their_owners` | Choosing a bounded scope over a container-wide or per-request definition from a stated reuse requirement, and naming the owner that must close it | **`koin_scopes` holds zero ACTIVE Questions** | Add coverage in E27-08 |
| GAP-U5-C | 5 / `lesson_resolving_viewmodels_at_the_boundary` | That the container constructs the ViewModel while the host's store owner decides its lifetime, and that a runtime value is passed at the resolution site | **`koin_viewmodels` holds zero ACTIVE Questions**, and misconception 24 is unassessed anywhere. The repository's own graph makes this the most concrete reasoning in Unit 5 | Add coverage in E27-08; **the widest gap in Unit 5** |
| GAP-U5-D | 5 / `lesson_koin_definitions_and_reuse` | Choosing a definition from a stated reuse requirement, rather than stating the difference between two keywords | `di_koin_factory_vs_single` is definitional, and separately carries a claim that current documentation has overtaken. The correction and the gap should be decided together | Add coverage in E27-08, paired with the correction candidate |
| GAP-U5-E | 5 / `lesson_one_graph_across_platforms` | Deciding **which** bindings must be supplied per platform and which belong in shared code, for a stated graph | `koin_multiplatform_common_module` assesses why Koin is a candidate at all, which is a different question from how to split a graph | Add coverage in E27-08 |
| GAP-U6-A | 6 / `lesson_when_should_a_graph_error_surface` | When a project's graph configuration is checked, stated as a property of the mechanism it uses rather than of the library's name | **`di_framework_tradeoffs` holds zero ACTIVE Questions.** The DEPRECATED Question held this axis and stated it in a way current tooling has made misleading. Overlaps GAP-U2-E, and the two should be written as one generic and one comparative Question or as one Question only | Add coverage in E27-08; **the widest gap in the epic** |
| GAP-U6-B | 6 / `lesson_three_projects_three_answers` | Choosing a strategy for a described project from its requirements, including a project whose answer is to add no container | Nothing in any Topic poses it, and E27-07's acceptance criteria require the learner to be "assessed by a decision-from-requirement scenario rather than by a ranking". Pairs with GAP-U6-C | Add coverage in E27-08; **strong candidate, and a plausible ADVANCED** |
| GAP-U6-C | 6 / `lesson_the_smallest_sufficient_strategy` | Choosing the smallest sufficient strategy and naming the observable change that would justify the next one | `manual_di_graph_growth_cost` assesses what grows, which is one input. The decision itself is unassessed, and it is the Lesson the epic ends on | Add coverage in E27-08 |

### Mapping, level and correction candidates

These are candidate Question edits, not new Questions. **None of them makes a Question wrong.**
E27-08 should decide on each deliberately — a re-map changes which Unit's practice a Question
appears in, and preserving Question and AnswerOption identity is a lifecycle requirement.

| Question | Current Subtopic | Reasoning it actually assesses | Suggested change | Consequence of leaving it |
| --- | --- | --- | --- | --- |
| `hilt_field_injection_framework_classes` | `constructor_injection` | That Android instantiates an Activity itself, so Hilt injects annotated fields at a documented lifecycle point, and constructor injection stays the default elsewhere | **Re-map to `hilt_fundamentals`** | **The most consequential mismatch found.** Unit 1 practice contains a Hilt Question three Units before Hilt exists, and Unit 4 — whose L4.3 teaches exactly this — never reaches it. Moving it also empties `constructor_injection`, which makes GAP-U1-E obligatory rather than optional, so the two decisions must be taken together |
| `dagger_compile_time_graph_validation` | `dependency_graphs` | That Dagger resolves every reachable key at compile time, so a missing binding and an ambiguous key are both build errors | **Re-map to `dagger_fundamentals`** | Unit 2 practice contains a Dagger Question before Dagger exists, and it is one of only two Questions Unit 2 reaches at all. Moving it leaves `dependency_graphs` — a four-Lesson primary — with no ACTIVE Question, which makes GAP-U2-E obligatory. Not moving it leaves an honest mapping with a premature reader experience, which is a defensible outcome and must then be recorded rather than hidden |
| `di_constructor_injection_testability` | `di_fundamentals` | The four properties that follow from taking required collaborators as constructor parameters, and the one that does not | **Re-map candidate to `constructor_injection`**, decided together with the row above | Leaving it keeps a strong Question reachable from Unit 1 either way — both Subtopics are Unit 1 primaries — so the routing consequence is small. What it changes is which Lesson's reasoning the Question evidences, and whether `constructor_injection` has any generic ACTIVE coverage at all |
| `di_scopes_001` | `di_scopes` | That a scope decides lifetime and sharing, and that mis-scoping fails in both directions | **Wording correction candidate.** Its keyed option reads "A scope controls the lifetime and sharing boundary of provided instances" | L2.3 teaches that the **owner** decides the lifetime and the scope is a reuse rule within it, so the Question's own wording will read as a contradiction to a learner who understood the Lesson. This is the clearest example of a Question that is correct against its Sources and incompatible with the curriculum's terminology contract |
| `di_koin_factory_vs_single` | `koin_definitions` | The difference between a retained and a per-resolution definition | **Explanation correction candidate.** "Both resolve at runtime, since Koin builds its graph without code generation" | Accurate for the classic DSL; inaccurate as a statement about Koin, whose current compiler plugin generates the DSL and verifies configuration at compile time. Left unchanged, it teaches misconception 25 — which Unit 5 and Unit 6 both exist to correct |
| `koin_multiplatform_common_module` | `koin_multiplatform` | Why an annotation processor emitting JVM code cannot serve `commonMain` for a native target while a plain Kotlin DSL can | **Explanation qualification candidate.** "The trade-off being that a missing binding surfaces as a runtime failure rather than a compile error" | Same issue, smaller: the claim is true of the mechanism this repository uses and needs scoping to it. The Question's keyed answer and every distractor remain correct |
| `hilt_activity_retained_component_lifetime`, `hilt_install_in_binding_visibility`, `hilt_vs_dagger_convention_tradeoff` | `hilt_components`, `hilt_modules`, `hilt_vs_dagger` | Component lifetimes, installation visibility, and what Hilt decides | **Source-freshness review.** All three reference Fragment or View components, which the Hilt library still defines and the Android documentation no longer lists | No answer becomes wrong. What changes is that the cited Android page no longer supports the wording, so the Sources should name the library documentation for those claims. See [the Hilt findings](#hilt-findings) |
| `dagger_inject_provides_binds_selection`, `dagger_scope_component_instance_lifetime` | `dagger_bindings`, `dagger_scopes` | Binding selection under three constraints; scoped lifetime following the component instance | **Level review** | Both are `FOUNDATION` for reasoning that is a judgement rather than recall, and the second is the Unit's hardest idea. A level change is a candidate, not a defect |

**Two observations that are not gaps and must not be treated as quotas:**

- **The `dependency_injection` Topic holds zero ACTIVE `ADVANCED` Questions** — 14 `FOUNDATION`
  and 9 `APPLIED` across 23. This plan proposes no quota. It records that the finished
  curriculum will teach four pieces of reasoning that would justify one on merit: resolving a
  lifetime conflict across several owners (L2.3, L4.2, L4.4); distinguishing injection from
  inversion in a realistic graph (L1.3); choosing a strategy for a constrained project
  (L6.3, L6.4); and identifying why a graph that assembles correctly still violates a lifetime
  requirement (L2.3, L3.7). E27-08 decides levels after the Lessons exist.
- **The bank is weighted towards Dagger and Hilt: 9 and 6 ACTIVE Questions against 2 for Koin
  and 5 for the generic material.** This is not evidence that Dagger deserves more Lessons; it
  is evidence about who wrote the bank. The Lesson distribution above was derived from mental
  models and happens to land at 6/6/7/6/5/4, which is close to even — and Unit 5's two
  Questions against five Lessons is the sharpest coverage imbalance in the epic.

---

## Part 6 — Unit practice routing, modelled now

Unit practice is resolved from a Unit's Lessons' **primary** concepts only; supporting concepts
never broaden practice eligibility. `PracticeTargetResolver` reads an ACTIVE Unit, collects the
ACTIVE primary Subtopic ids of its ACTIVE Lessons, and hands them to the assessment scope. The
pools below were computed by script from the proposed mappings against the ACTIVE Questions in
the bundled JSON on 2026-09-18, so that the routing shape is a finding rather than something
E27-08 discovers.

| Unit | Primary concepts | Pool | Levels | The Questions |
| --- | --- | --- | --- | --- |
| 1 | `di_fundamentals`, `constructor_injection`, `composition_root`, `service_locator_vs_di`, `manual_di` | **5** | 4 F, 1 A | `di_constructor_injection_testability`, `hilt_field_injection_framework_classes`, `composition_root_001`, `service_locator_vs_di_001`, `manual_di_graph_growth_cost` |
| 2 | `dependency_graphs`, `di_scopes` | **2** | 2 F | `dagger_compile_time_graph_validation`, `di_scopes_001` |
| 3 | `dagger_fundamentals`, `dagger_modules`, `dagger_bindings`, `dagger_components`, `dagger_scopes`, `dagger_qualifiers`, `dagger_multibindings` | **8** | 5 F, 3 A | `dagger_generated_factory_no_reflection`, `dagger_inject_provides_binds_selection`, `dagger_component_graph_root`, `dagger_subcomponent_parent_binding_inheritance`, `dagger_component_dependency_vs_subcomponent`, `dagger_scope_component_instance_lifetime`, `dagger_qualifier_same_type_bindings`, `dagger_multibinding_into_set` |
| 4 | `hilt_fundamentals`, `hilt_components`, `hilt_viewmodels`, `hilt_modules`, `hilt_vs_dagger` | **6** | 2 F, 4 A | `hilt_entry_point_manual_access`, `hilt_activity_retained_component_lifetime`, `di_hilt_viewmodel_scope`, `dagger_assisted_injection_viewmodel`, `hilt_install_in_binding_visibility`, `hilt_vs_dagger_convention_tradeoff` |
| 5 | `koin_fundamentals`, `koin_definitions`, `koin_scopes`, `koin_viewmodels`, `koin_multiplatform` | **2** | 1 F, 1 A | `di_koin_factory_vs_single`, `koin_multiplatform_common_module` |
| 6 | `di_framework_tradeoffs`, `manual_di` | **1** | 1 A | `manual_di_graph_growth_cost` |

**Union: all 23 ACTIVE dependency-injection Questions.** No Question falls outside every pool,
which is the difference from E26, where `solid` was supporting-only by design.

**Primary concepts with no ACTIVE coverage — five, in three Units:** `dagger_modules` (Unit 3),
`koin_fundamentals`, `koin_scopes`, `koin_viewmodels` (Unit 5), `di_framework_tradeoffs`
(Unit 6). Unit 5 has three of the five, which is why three of its five Lessons are reached by
nothing.

### Structural overlaps and premature routing, calculated rather than discovered

**Exactly one Question appears in two Units' pools.** `manual_di_graph_growth_cost` is in
Unit 1 and Unit 6, because `manual_di` is primary in L1.6 and L6.4. This is intended and not a
defect: L1.6 asks whether hand-wiring is sufficient for one project and L6.4 asks it as the
final decision among four strategies, and the Question — what grows fastest as a hand-wired
graph gets larger — is a fair input to both. **The alternative, demoting `manual_di` to
supporting in one of them, was rejected**, because both Lessons genuinely teach it.

**Two Questions are semantically premature in the Unit they reach**, and both are recorded as
re-map candidates rather than repaired by distorting a mapping:

1. **`hilt_field_injection_framework_classes` reaches Unit 1** through `constructor_injection`.
   Its stem names an Activity and Hilt. A Unit 1 reader has met no framework at all. The
   mapping is not dishonest — the Question genuinely is about why constructor injection is
   unavailable — but the reader experience is wrong, and the Lesson that teaches the reasoning
   is three Units later.
2. **`dagger_compile_time_graph_validation` reaches Unit 2** through `dependency_graphs`. Its
   stem names a component and every option names Dagger behaviour. The mapping is honest,
   because compile-time validation genuinely is a fact about a dependency graph, and it is one
   of only two Questions Unit 2 reaches, so removing it without authoring GAP-U2-E would leave
   the Unit with one.

**The deliberate decision recorded here is that neither was avoided by mapping a Lesson
dishonestly.** Unit 2's Lessons genuinely teach `dependency_graphs`, and Unit 1's L1.2 genuinely
teaches `constructor_injection`. Correct taxonomy produces unavoidable premature practice, and
the remedy is a Question re-map in E27-08, not a mapping the curriculum does not believe.

**Questions structurally reached but semantically weak for the Unit that reaches them.**
Three, all of them "correct but definitional where the Lesson teaches a decision":
`composition_root_001` and `service_locator_vs_di_001` in Unit 1, and `di_koin_factory_vs_single`
in Unit 5. Each is recorded as a gap rather than as a fault in the Question.

**No shipped Unit's practice pool changes.** No shipped Lesson takes a `dependency_injection`
Subtopic as primary, so nothing E27 maps can reach an existing Unit.

**E27-08 must re-compute these pools through the production resolver** rather than trusting
this table, exactly as E26-08 did, because the mappings may have moved during authoring and
because the resolver — not this script — is what the learner meets.

### Cross-topic duplication guard

Questions outside `dependency_injection` that already assess reasoning this epic touches.
**None should be re-mapped into `dependency_injection`**, and E27-08 must not author a
duplicate of any of them.

| Question | Topic / Subtopic | What it already assesses | What E27 must therefore not re-author |
| --- | --- | --- | --- |
| `interface_with_one_implementation_is_not_a_boundary` | architecture / `interface_boundaries` | That a contract in the implementation's own vocabulary is not a boundary — and, in its explanation, that "injection decides which implementation arrives at run time and changes no source dependency, so a codebase can inject everywhere and have inverted nothing" | The **whether-an-abstraction-earns-its-existence** half of L1.2. GAP-U1-A must be written from the injection side, about one graph, not as another interface-value Question |
| `architecture_solid_dependency_substitution` | architecture / `solid` | That depending on a caller-owned abstraction is dependency inversion and enables open/closed extension | The inversion half of L1.3. E27 assesses the **distinction**, not inversion itself |
| `dependency_direction_domain_framework_types` | architecture / `dependency_direction` | Framework coupling recognised from a signature, with a distractor that explicitly denies "the use case cannot be injected" | Any Question whose real subject is dependency direction |
| `test_dependency_substitution_constructor` | testing / `test_di` | That a class reaching a global singleton for its repository can only be substituted by changing the global, which leaks between tests, while a constructor parameter keeps cases independent | **The testing form of L1.5.** GAP-U1-D must be about design visibility and the integration boundary, not about test substitution — which is E31's |
| `kmp_expect_actual_vs_interface` | kmp / `kmp_dependency_inversion` | When an interface with injected implementations is preferable to `expect`/`actual`, decided by substitutability | A large part of L5.5's platform-binding reasoning. GAP-U5-E must be about **which bindings the graph splits**, not about choosing between the two mechanisms |
| `expect_actual_001` | kmp / `expect_actual` | What `expect`/`actual` solves, noting that "where a plain interface plus constructor injection suffices, that is usually the simpler option" | Any `expect`/`actual` Question. E33 owns the mechanism |
| `kmp_shared_viewmodel_owner_platform` | kmp / `kmp_lifecycle_viewmodel` | That the owner which creates and clears a shared ViewModel is platform-supplied | The **ownership** half of L5.4 and L4.4. GAP-U5-C must be about what the container decided, given that the host decides the lifetime |
| `viewmodel_activity_reference_lifetime`, `architecture_state_holder_taxonomy`, `owner_chosen_from_the_required_lifetime` | architecture / `state_ownership` | Owner selection from a required lifetime, and what an owner outliving a UI instance costs | The generic **owner** reasoning. GAP-U2-A and GAP-U2-B must be about reuse policy and about the scope-creates-no-owner correction, not about choosing an owner |
| `android_process_model_001` | android_platform / `android_process_model` | That in-memory singletons do not survive the process being killed | The **storage** half of misconception 11. GAP-U4-C must come from the component-lifetime side |
| `ksp_vs_kapt_build_cost` | build_delivery / `kotlin_gradle_plugin` | Why moving Room and Dagger annotation processing to KSP shortens builds | Any build-cost Question. E29 owns it, and L3.7 and L6.1 name the cost without analysing it |
| `modularization_large_app_module_cost` | build_delivery / `android_modules` | The cost of a large app module, including wiring | The build-module half of L3.4 |
| *(none)* | kmp / `koin_kmp` | Nothing — the Subtopic is ACTIVE and empty | **A guard rather than a duplicate.** E27-08 must not author a Koin-multiplatform Question here as well as under `koin_multiplatform`; a Question here would reach no E27 Unit at all |

---

## Part 7 — This repository's own Koin graph, as evidence

Read from `shared/src/` on 2026-09-18. **This is evidence for examples, not a model to imitate
and not a target to refactor.** The application is small, entirely local-first, has no network
layer, ships one Gradle module for its shared code, and was built under constraints a Lesson
must state whenever it uses the code as an illustration. E27 proposes **no change to the
production Koin graph**, and no authoring issue may make one.

### The complete graph, as configured

**Seven `commonMain` modules**, each a `module { }` value:

| Module | File | Definitions | What it shows |
| --- | --- | --- | --- |
| `curriculumDataModule` | `data/local/curriculum/CurriculumDataModule.kt` | `CurriculumImporter`, `CurriculumRepository` → `LocalCurriculumRepository`, `CurriculumDataInitializer` | A concrete definition, an interface binding, and a startup collaborator in one module |
| `learningContentModule` | `curriculum/learning/content/LearningContentModule.kt` | `LearningContentRepository` → `BundledLearningContentRepository` | An interface binding whose KDoc states **why** it is retained: "its loaded document is meant to be shared" |
| `assessmentDataModule` | `data/local/assessment/AssessmentDataModule.kt` | 11 definitions including `AppCoroutineScope`, `AssessmentHistoryStore`, `AssessmentQuestionSelector`, `AssessmentEngine`, `LearningProgressService` | The largest single module, and the clearest example of a graph deep enough that the assembly order stopped being obvious |
| `savedQuestionDataModule` | `data/local/saved_questions/SavedQuestionDataModule.kt` | `SavedQuestionRepository` → `LocalSavedQuestionRepository` | A one-definition module, which is evidence that module size is an organisational choice |
| `lessonStudyDataModule` | `data/local/lesson_study/LessonStudyDataModule.kt` | `LessonStudyRepository` → `LocalLessonStudyRepository` | Same, with a KDoc stating the boundary it was split along |
| `topicStudyPresentationModule` | `topic_study/TopicStudyPresentationModule.kt` | 26 definitions: 11 retained and 15 ViewModel, 8 of the latter taking runtime parameters | **The richest single file of evidence in the repository** for Unit 5 |
| `appearanceModule` | `settings/AppearanceModule.kt` | `ThemePreferenceStore`, `AppearanceStateHolder` | The shared half of the platform-binding example |

**Eight platform modules**, two per host: `androidCurriculumDataModule` /
`androidAppearanceModule`, `jvmCurriculumDataModule` / `jvmAppearanceModule`,
`iosCurriculumDataModule` / `iosAppearanceModule`, `webCurriculumDataModule` /
`webAppearanceModule`.

**Four host composition roots**, one per target:

| Host | Function | File |
| --- | --- | --- |
| Android | `startAndroidLocalDataGraph(application)` | `androidMain/.../AndroidLocalData.kt` |
| Desktop (JVM) | `startDesktopLocalDataGraph()` | `jvmMain/.../DesktopLocalData.kt` |
| iOS | `startIosLocalDataGraph()` | `iosMain/.../IosLocalData.kt` |
| Web | `startWebLocalDataGraph()` | `webMain/.../WebLocalData.kt` |

All four call `startKoin` with the **same seven shared modules in the same order**, then add
their own two. Only the Android one takes an argument and calls `androidContext(...)`, because
only Android's storage and database need one. Each guards against a second start. Each host
application calls its own function once — `KmpLearningApplication.onCreate()`,
`desktopApp`'s `main()`, `webApp`'s `main()`, and `MainViewController` on iOS.

### The evidence matrix

| Claim a Lesson may make | Where it is settled | What it demonstrates | What it does **not** demonstrate |
| --- | --- | --- | --- |
| Each host has its own composition root, and they differ only in what the platform must supply | The four `start*LocalDataGraph` functions | That a multiplatform application's shared graph plus per-platform bindings is a workable arrangement, and that "one place that knows how to build" survives having four entry points | That four near-identical startup functions are the right factoring. They share seven module references by repetition, and the repository has not needed to change that |
| Only one host passes a platform object into the graph | `androidContext(application.applicationContext)` in `AndroidLocalData.kt` | That platform coupling is confined to the module that needs it | That a platform context is generally required. Three hosts need none |
| A binding can be a concrete type or an interface mapped to an implementation | `single { CurriculumImporter(database = get()) }` beside `single<CurriculumRepository> { LocalCurriculumRepository(database = get()) }` in one file | Both definition shapes side by side, with the interface binding an instance of the inversion E26 already taught | That every repository needs an interface. `CurriculumImporter` and `AssessmentAttemptStore` have none and are resolved by their concrete types |
| A retained definition can correspond to an application-lifetime requirement | `AppearanceStateHolder`, with the reason in the source: "App-scoped because the theme is applied above the navigation shell: the preference has to outlive the Settings entry that changes it" | **The best single piece of evidence in the repository for L2.2 and L5.2** — a reuse decision with its lifetime requirement written next to it | That state holders should be retained. The reason is specific and the Lesson must quote the reason, not the keyword |
| An application-lifetime coroutine scope can be an ordinary graph dependency | `single { AppCoroutineScope() }`, requested six times across two modules as `get<AppCoroutineScope>()` — by one history store and five state holders | That a lifetime an application needs can be expressed as a named type in the graph, and that naming the type is what makes the injection unambiguous | Anything about coroutine scope design, which E24 and E26 own |
| Retained definitions are used for shared caches with stated reasons | `StudyProgressStateHolder` ("Topic Detail, the Unit overview, and the Lesson reader are alive at the same time and show the same learner-owned truth"), `SavedQuestionStateHolder` ("the three review surfaces present the same saved identities") | That the reuse requirement in this graph is genuinely **shared identity**, which is L2.2's first requirement | That app-wide retention is a default. Both KDocs argue the case, which is the point |
| A ViewModel is a definition like any other, constructed from graph dependencies | 14 `viewModel { }` definitions in `topicStudyPresentationModule` | That the container constructs the ViewModel from ordinary injected collaborators | That the container owns the ViewModel's lifetime. It does not — see the row below |
| A runtime value reaches a ViewModel as a parameter rather than as a binding | `viewModel { parameters -> TopicDetailViewModel(topicId = parameters.get(), …) }`, and seven more like it | **The clearest available evidence for L2.4 and L5.4** — the repository distinguishes graph dependencies from route-supplied identifiers in every parameterised definition | That every navigation argument should travel this way. The saved-state route exists and this application does not use it |
| Two runtime parameters of the same type must be read positionally | `LearningLessonViewModel(unitId = parameters.get(0), lessonId = parameters.get(1), …)`, with the source comment "both identities are Strings, so a type-based lookup cannot say which is which" | **L2.5's same-type ambiguity, in production, with the consequence recorded**: the author notes that resolving both by type "would make every Lesson unavailable" | That positional parameters are a good general design. It is the ambiguity Unit 2 warns about, arriving in a place with no qualifier mechanism |
| ViewModels are resolved at destination boundaries | 20 `koinViewModel()` call sites across 15 files, 8 of them passing runtime parameters | That resolution happens at the integration edge, which is L1.5's acceptable boundary | That resolving from a composable is generally acceptable anywhere. Every one of the 20 sits in the application shell or in a destination-level composable |
| The container is resolved directly in exactly two non-startup places | `koinInject()` for `AppearanceStateHolder` in `SettingsDestination.kt`; `KoinPlatform.getKoinOrNull()?.getOrNull<AppearanceStateHolder>()` in `AppearanceTheme.kt` | **The material for L5.4's boundary judgement.** The first is a composable default parameter at a destination — an integration boundary. The second is infrastructure whose source explains why it tolerates an absent graph: "a preview or a unit test that composes a screen directly has no graph" | A service locator in a business class. **No domain, data or presentation class in this repository resolves from the container** — verified by search. Every non-startup access is at a composition boundary |
| Startup code may reach into the container | `GlobalContext.get().get<CurriculumDataInitializer>()` on Android and Desktop; `KoinPlatform.getKoin().get<…>()` on iOS and Web | That infrastructure at the composition root legitimately touches the container | That this is a pattern for application code. It is four lines, all in the startup files |
| A shared abstraction can be bound to a different implementation per platform | `AppPreferenceStorage` declared in `commonMain`; `AndroidAppPreferenceStorage` (SharedPreferences), `IosAppPreferenceStorage` (NSUserDefaults), the JVM properties-file storage and the web local-storage implementation, each bound in its own platform module | **The best multiplatform evidence in the repository, and better than an invented `expect`/`actual` example** — the shared code depends on an interface it owns, and the graph supplies the platform detail | Multiplatform architecture generally. It is one preference with four small implementations, and E33 owns the subject |
| The same split works for a platform-constructed database | `createCurriculumDatabase(context)`, `createJvmCurriculumDatabase()`, `createIosCurriculumDatabase()`, `createWebCurriculumDatabase()`, one per platform module | That the pattern generalises past the toy case | Room, drivers or persistence design, which the persistence curriculum owns |

### Observations recorded, not acted on

Per E27-06's acceptance criterion, any observation that the production graph could be improved
is recorded here and **not** proposed as work. None of these is a defect.

1. **All seven shared modules are listed by hand in all four hosts**, so adding a shared module
   edits four files. Koin's module-inclusion feature would express this differently. The
   repetition is currently harmless and no issue proposes changing it.
2. **`topicStudyPresentationModule` holds both presentation definitions and ten app-scoped
   state holders**, several of which are not presentation. The file's own KDoc comments explain
   each placement. It is an organisational observation, not an architectural one.
3. **`LearningLessonViewModel` reads two same-typed parameters positionally**, which the source
   already flags. Unit 5 uses it as a teaching example of the ambiguity; it is not a bug.

### How a Lesson may and may not use this

- It **may** describe the arrangement and name the real types, framed as an example per
  [Part 4](#part-4--recurring-worked-examples).
- It **may** quote a definition's stated reason, because the reason is what makes the example
  teach something.
- It **must not** convert a local choice into a general recommendation. "This graph uses a
  retained definition for its repositories, therefore repositories should be retained" and
  "this graph has one module per concern, therefore modules should match architecture layers"
  are both forbidden, explicitly, by E27-06's acceptance criteria.
- It **must not** write "this app", "this repository" or "the current implementation", per
  Rule 11.
- It **must not** propose a refactor, and must re-verify every claim above against the code as
  it stands at authoring time and again at closure.

---

## Part 8 — Dagger, Hilt and Koin source-sensitive findings

**This is the acceptance-critical technical section of E27-01.** Every claim below was settled
against the documentation each project publishes, read on **2026-09-18**, rather than from
recalled behaviour. Framework APIs move; the generic reasoning of Units 1 and 2 does not.

### Dagger findings

Read from `dagger.dev` on 2026-09-18. The dev-guide index currently lists Basic Usage,
Subcomponents, Multibindings, Assisted Injection, Dagger SPI, Testing, Compiler Options,
Versions and KSP Support.

| Claim a Lesson will make | Settled by | Exact basis |
| --- | --- | --- |
| Dagger generates the wiring rather than reflecting at run time | Dagger overview | "Dagger 2 is the first to implement the full stack with generated code", with the guiding principle "to generate code that mimics the code that a user might have hand-written" |
| An annotated constructor produces generated factory code | Basic Usage, *Compile-time Code Generation* | The processor "may also generate source files with names like `CoffeeMaker_Factory.java` or `CoffeeMaker_MembersInjector.java`", described as implementation details |
| An abstract binding is preferred where it applies, for a stated reason | Basic Usage, *Satisfying Dependencies* | "Using `@Binds` is the preferred way to declare this kind of binding because Dagger only needs the module at compile time, and can avoid class loading the module at runtime" |
| A component is a graph with declared roots | Basic Usage, *Building the Graph* | The component defines "a well-defined set of roots" through "an interface with methods that have no arguments and return the desired type" |
| A component dependency exposes only what the other component publishes | Basic Usage, list of available bindings | "The component provision methods of the component dependencies" appear in the list of what a component can provide |
| **A scoped instance's lifetime is the component instance's** | Basic Usage, *Singletons and Scoped Bindings* | "Dagger 2 associates scoped instances in the graph with instances of component implementations", which is why "the components themselves need to declare which scope they intend to represent" — **the single most important Dagger sentence for this curriculum**, because it is misconception 9's refutation |
| Validation happens at the component level, not per module | Basic Usage, *Compile-time Validation* | "While `@Inject`, `@Module` and `@Provides` annotations are validated individually, all validation of the relationship between bindings happens at the `@Component` level" |
| A missing binding is a compiler error | Basic Usage, *Compile-time Validation* | The processor "is strict and will cause a compiler error if any bindings are invalid or incomplete", with the quoted `javac` rejection |
| A subcomponent sees every ancestor binding and nothing sees a child's | Subcomponents | "An object bound in a subcomponent can depend on any object that is bound in its parent component or any ancestor component"; and "objects bound in parent components can't depend on those bound in subcomponents; nor can objects bound in one subcomponent depend on objects bound in sibling subcomponents" |
| A subcomponent may not repeat an ancestor's scope | Subcomponents | "No subcomponent may be associated with the same scope as any ancestor component" |
| Multibindings let independent modules contribute to one collection | Multibindings | "Dagger allows you to bind several objects into a collection even when the objects are bound in different modules using multibindings", and the plugin example where "application code can inject it without depending directly on the individual bindings" |
| Assisted injection separates graph parameters from call-site parameters | Assisted Injection | "some parameters may be provided by the DI framework and others must be passed in at creation time"; and the two constraints — an assisted type "cannot be injected directly, only the `@AssistedFactory` type can be", and assisted types "cannot be scoped" |

**One Reference-only finding.** The reuse-without-identity scope is documented with an explicit
warning: it is "not associated with any single component", "there is no guarantee that the
component will call the binding only once", and applying it "to bindings that return mutable
objects, or objects where it's important to refer to the same instance, is dangerous". L3.5 may
name it in one paragraph; it must not be presented as a third ordinary option.

**What E27-04 must not do.** No Lesson may state build-tool behaviour, processor configuration
or build timings. The KSP and Compiler Options pages exist and are **E29's**; E27 states only
that a build step exists.

### Hilt findings

**This is the most source-sensitive material in the epic, and the two official sources
currently differ.** Both were read on 2026-09-18.

**Finding 1 — the Android documentation now documents five components; the Hilt library
documents eight.** The Android Developers Hilt page lists exactly `SingletonComponent`,
`ActivityRetainedComponent`, `ViewModelComponent`, `ActivityComponent` and `ServiceComponent`,
and the strings `FragmentComponent`, `ViewComponent` and `ViewWithFragmentComponent` do not
appear on it at all. The Hilt documentation at `dagger.dev/hilt/components` lists all eight,
with `FragmentComponent` (`@FragmentScoped`, `Fragment#onAttach()` to `Fragment#onDestroy()`),
`ViewComponent` and `ViewWithFragmentComponent` (both `@ViewScoped`, `View#super()` to view
destruction). The Android page's supported-class list is correspondingly `Application`,
`ViewModel`, `Activity`, `Service` and `BroadcastReceiver`, with the note that in Compose "you
don't need to annotate individual composables" and that annotating the root activity "serves as
the single DI entry point for your entire UI hierarchy".

**Neither source is wrong, and E27-05 must not pick one silently.** The library defines eight
components; the platform's current recommended practice documents five. The rule for authoring:
a Lesson teaches the components its reasoning needs — which for this curriculum is the
singleton, retained, ViewModel and activity components — and where it names a Fragment or View
component it cites the library documentation and says that the current Android guidance is
written around Compose. Three existing ACTIVE Questions reference the fragment and view
components; none becomes wrong, and all three are recorded as source-freshness candidates.

**Finding 2 — the component lifetimes, from the Android page's own table.**

| Generated component | Created at | Destroyed at | Scope |
| --- | --- | --- | --- |
| `SingletonComponent` | `Application#onCreate()` | `Application` destroyed | `@Singleton` |
| `ActivityRetainedComponent` | `Activity#onCreate()` | `Activity#onDestroy()` | `@ActivityRetainedScoped` |
| `ViewModelComponent` | `ViewModel` created | `ViewModel` destroyed | `@ViewModelScoped` |
| `ActivityComponent` | `Activity#onCreate()` | `Activity#onDestroy()` | `@ActivityScoped` |
| `ServiceComponent` | `Service#onCreate()` | `Service#onDestroy()` | `@ServiceScoped` |

With the note that decides L4.2's worked requirement: "`ActivityRetainedComponent` lives across
configuration changes, so it is created at the first `Activity#onCreate()` and destroyed at the
last `Activity#onDestroy()`." The hierarchy is `SingletonComponent` above
`ActivityRetainedComponent` and `ServiceComponent`, with `ActivityComponent` and
`ViewModelComponent` below the retained one.

**Finding 3 — installation decides visibility, and the documentation says so directly.**
"Installing a module into a component allows its bindings to be accessed as a dependency of
other bindings in that component or in any child component below it in the component
hierarchy." Separately, "all bindings in Hilt are unscoped" by default, and a scoped binding is
created "once per instance of the component that the binding is scoped to". These two sentences
are L4.5's entire content, and together they are the direct refutation of reading `@InstallIn`
as a lifetime declaration.

**Finding 4 — the retained and ViewModel scopes are different owners, verbatim.** From the Hilt
and Jetpack integrations documentation: "All Hilt ViewModels are provided by the
`ViewModelComponent` which follows the same lifecycle as a `ViewModel`"; "a `@ViewModelScoped`
type will make it so that a single instance of the scoped type is provided across all
dependencies injected into the `ViewModel`. Other instances of a ViewModel that request the
scoped instance will receive a different instance"; and "if a single instance needs to be shared
across various ViewModels, then it should be scoped using either `@ActivityRetainedScoped` or
`@Singleton`." **This settles misconception 23 and is the source L4.4 rests on.**

**Finding 5 — field injection's constraint is documented and is the reason it is visible.**
"Fields injected by Hilt cannot be private. Attempting to inject a private field with Hilt
results in a compilation error." L4.3 uses this as the concrete consequence of the framework
having to assign the field.

**Finding 6 — entry points are defined as a boundary, not as a lookup convenience.** "An entry
point is the boundary between code that is managed by Hilt and code that is not. It is the
point where code first enters into the graph of objects that Hilt manages." The content-provider
example remains the documented case, and `hilt_entry_point_manual_access` rests on it correctly.

**Finding 7 — the default bindings table includes `SavedStateHandle` in the ViewModel
component**, which is the documented route for a navigation-time value and is what L4.4 compares
assisted injection against. The Lesson names it and teaches no saved-state API.

**What E27-05 must re-verify.** Every row of Finding 2, the supported-class list in Finding 1,
and whether the two sources still differ. This page has changed materially since the existing
Questions were written, which is exactly why the re-verification is an acceptance criterion
rather than a courtesy.

### Koin findings

Read from `insert-koin.io` on 2026-09-18. **Every page consulted is labelled version 4.2**,
which is the closest current documentation to this repository's configured **4.2.2**.

**Finding 1 — Koin now documents three ways to declare definitions, and one of them is checked
at compile time.** The definitions, ViewModel and annotations pages each present a **Compiler
Plugin DSL**, an **annotations** form and the **classic DSL** side by side. The annotations page
states that "the Koin Compiler Plugin processes these annotations and generates all underlying
Koin DSL for you at compile time" and that "the Compiler Plugin verifies your Koin configuration
at compile time, checking that all dependencies are declared and accessible". It also states
that "the legacy KSP processor (`koin-ksp-compiler`) is **deprecated** in favor of the **Koin
Compiler Plugin**".

**This is the finding that makes misconception 25 a live one rather than a straw man**, and it
directly affects two existing ACTIVE Questions whose explanations assert that Koin resolves at
run time because it generates no code. **E27-06 and E27-07 must not write "Dagger is
compile-time, Koin is runtime" as a fundamental comparison.** The accurate formulation is that
detection time follows the mechanism a project chooses, and that this repository's graph uses
the classic DSL, whose configuration is checked when a definition is resolved.

**What E27 must not do with this finding.** Teach the compiler plugin's setup, its annotations
or its build configuration. Unit 5 carries a bounded note; Unit 6 carries the axis; E29 owns
everything else. The deprecated KSP processor must not be taught as current architecture.

**Finding 2 — the definition vocabulary, as currently documented.** `single` is "one instance
for app lifetime" that is "reused throughout the app"; `factory` produces "a new instance each
time"; `scoped` is "one instance per scope". Interface binding is documented in all three
declaration forms. A module is "a logical container for grouping related definitions", and the
modules page recommends `includes()` as "the recommended way to organize your modules" over
listing modules directly at startup.

**The wording "app lifetime" is a summary, not a guarantee**, and L5.2 must not repeat it
unqualified: it is the *container's* lifetime, and the container is started by a host. This is
the same correction L2.3 makes generically, and it is why the Lesson teaches the container
lifetime before the keyword.

**Finding 3 — scopes are documented as lifecycle containers with an explicit close.** A scope is
created with an identity, instances declared inside it are reused within it, and when a scope
closes "all scoped instances are released", close callbacks run, and the scope "becomes
unusable" — accessing an instance afterwards throws. The Android helpers "automatically create
and destroy Scope based on Activity Lifecycle". L5.3 teaches the generic form and names the
helpers as Reference.

**Finding 4 — the Compose integration, and what it does and does not decide.** `koinInject()`
injects "any Koin-managed dependency" and the documentation recommends injecting "as default
parameter" for testability — which is exactly the shape this repository's `SettingsDestination`
uses. `koinViewModel()` is "the primary API for injecting ViewModels in Compose" and supports
runtime parameters through `parametersOf`. Compose Multiplatform support is documented as full
for iOS and desktop and experimental for web, with `koin-compose` and `koin-compose-viewmodel`
usable from `commonMain`.

**The documentation does not state how the `ViewModelStoreOwner` is selected**, which is
significant for L5.4: the ownership half of that Lesson rests on the shipped multiplatform
ViewModel material and on this repository's own navigation-entry decorators, not on Koin's
documentation. The Lesson must say what each source settles.

**Finding 5 — the multiplatform guidance matches this repository's arrangement.** The
multiplatform page documents shared modules plus platform modules, the `expect val
platformModule: Module` pattern, and starting from shared code with
`startKoin { modules(commonModules() + platformModule) }`. **This repository does it
differently** — each host names the shared modules and its own explicitly, with no `expect`
declaration — and that difference is worth one sentence in L5.5, stated as two workable
arrangements rather than as a deviation.

**What E27-06 must re-verify.** The version label on every page, the three declaration forms and
their verification behaviour, the scope close semantics, and whether the configured library is
still 4.2.2.

---

## Part 9 — Source policy in practice

The blueprint records the
[source families and the hierarchy](dependency-injection-learning-blueprint.md#authoritative-source-families).
This section records what was actually opened during this review.

### Pages opened, 2026-09-18

| Page | Settles |
| --- | --- |
| `developer.android.com/training/dependency-injection` | The three ways a class gets a dependency and that the third "is dependency injection"; constructor against field injection, and that framework classes "are instantiated by the system, so constructor injection is not possible"; the reflection-against-generation split; the service-locator comparison and its three named downsides; and the benefits list |
| `developer.android.com/training/dependency-injection/manual` | The dependencies-container shape, the application-level container, that "you write a lot of boilerplate code (such as factories), which can be error-prone", that you must "manage the scope and lifecycle of the containers yourself", and the recommendation to prefer Hilt "when possible" |
| `developer.android.com/training/dependency-injection/hilt-android` | [Hilt findings](#hilt-findings) 1, 2, 3, 5, 6 and 7 |
| `developer.android.com/training/dependency-injection/hilt-jetpack` | [Hilt finding](#hilt-findings) 4 — the `@ViewModelScoped` against `@ActivityRetainedScoped` distinction, verbatim |
| `dagger.dev/hilt/components` | The library's full eight-component list with scopes and lifetimes, which is the other half of Hilt finding 1 |
| `dagger.dev/dev-guide/` | The generated-code principle |
| `dagger.dev/dev-guide/basic-usage` | Most of the [Dagger findings](#dagger-findings): generated factories, `@Binds` preference, component roots, component-dependency provision methods, scoped instances associated with component instances, the reusable-scope warning, and compile-time validation at the component level |
| `dagger.dev/dev-guide/subcomponents` | Parent-binding inheritance, one-way visibility, and the ancestor-scope rule |
| `dagger.dev/dev-guide/multibindings` | What multibindings are for and why independent modules can contribute |
| `dagger.dev/dev-guide/assisted-injection` | Assisted parameters, the generated factory, and the two constraints |
| `insert-koin.io/docs/reference/koin-core/definitions` | `single`, `factory`, `scoped`, interface binding, and the three declaration forms (v4.2) |
| `insert-koin.io/docs/reference/koin-core/modules` | What a module is, `includes()`, and the organisation guidance (v4.2) |
| `insert-koin.io/docs/reference/koin-core/scopes` | Scope creation, close semantics and the Android helpers (v4.2) |
| `insert-koin.io/docs/reference/koin-annotations/start` | The Compiler Plugin, its compile-time verification, and the deprecation of the KSP processor (v4.2) |
| `insert-koin.io/docs/reference/koin-compose/compose` | `koinInject()`, the default-parameter recommendation, and Compose Multiplatform support (v4.2) |
| `insert-koin.io/docs/reference/koin-compose/compose-viewmodel` | `koinViewModel()`, ViewModel definitions, runtime parameters, and the absence of a `ViewModelStoreOwner` statement (v4.2) |
| `insert-koin.io/docs/reference/koin-mp/kmp` | Shared and platform modules, the `expect val platformModule` pattern, and starting from shared code (v4.2) |

### Three findings that change how a Lesson must be phrased

1. **"Recommended" is not "required", and the Android manual-DI page is explicit about it.** It
   recommends Hilt "when possible" and, in the same breath, describes manual DI as a complete
   technique with named costs. **L1.6 must report both**, or it will manufacture the "manual DI
   is for toy applications" misconception the Unit exists to correct. The honest reading is that
   the page recommends a library for the projects it is written about, and names the conditions
   that make the recommendation apply.
2. **The compile-time-against-runtime dichotomy is no longer a framework-level fact.** Koin's
   current documentation describes a compiler plugin that verifies configuration at compile
   time. L6.2 must be written around **mechanism**, and two existing Questions carry
   explanations that this finding overtakes.
3. **The two official Hilt sources currently differ on the component set.** A Lesson that names
   a component must know which source it is resting on, and E27-05's acceptance criterion that
   the hierarchy is "verified against current official documentation" is satisfied only by
   naming which one.

---

## Part 13 — Boundaries with the shipped curricula

Stated concept by concept so that E27 applies these mechanics rather than reteaching them.
E27-09's acceptance criteria check exactly this.

### E26 owns application architecture

E27 does **not** teach: what a responsibility is, dependency direction, dependency inversion,
when an abstraction is a boundary, layers and their cost, Clean Architecture, repositories,
use cases, state holders, UI state modelling, unidirectional data flow, owner selection by
lifetime, or architecture proportionality.

E27 **applies** them: L1.2 cites the boundary test rather than re-arguing whether an interface
earns its existence; L1.3 cites `lesson_dependency_inversion_in_practice` and adds only the
injection-side examples; L2.3 and L4.4 apply owner selection; L6.4 applies proportionality.
The complete deferral ledger is
[Part 3](#part-3--the-e26--e27-handoff-ledger).

### E23, E24 and E25 own Compose and coroutines

E27 does **not** teach: Compose state, recomposition, effects, `collectAsState`, coroutine
scopes, structured concurrency, cancellation, dispatchers, Flow, `StateFlow` or `SharedFlow`.

E27 **touches** them in three bounded places: L5.4 shows a resolution call inside a destination
composable and teaches nothing about Compose; Part 7's evidence includes an injected
application-lifetime coroutine scope, used only as an example of a lifetime expressed as a
named graph type; and the blueprint's terminology register records that "scope" means something
different here and in E24, so the two are never used unqualified in one sentence.

---

## Part 14 — Boundaries with later and adjacent curricula

| Curriculum | E27 owns | It owns | Where the boundary is stated |
| --- | --- | --- | --- |
| **E29 — Gradle, modularization, build** | That code generation exists, that Dagger and Hilt require a build step, that a compiler plugin exists for Koin, and that early checking costs build work | KSP and kapt setup, annotation-processor configuration, compiler-plugin Gradle setup, convention plugins, generated-source directories, build variants, build-performance measurement, Gradle module structure | L3.1, L3.7, L5.1 and L6.2, each in one bounded statement. **No Gradle snippet appears in any E27 Lesson**, however a framework's own documentation opens |
| **E31 — Testing** | That explicit dependencies permit substitution, that hidden resolution makes isolation harder, and that graph choice changes the testing surface — all as consequences | Mocks, fakes, stubs, MockK, Mockito, Hilt test modules and test component replacement, Koin test APIs, test-double strategy, unit-test design | L1.2 and L1.5. **No E27 Lesson shows a test**, exactly as no E26 Lesson did, and `test_dependency_substitution_constructor` already assesses the testing form of L1.5's reasoning |
| **E33 — Kotlin Multiplatform** | Shared definitions, a platform-specific implementation supplied to one graph, one multiplatform composition-root pattern, and why an annotation processor emitting JVM code cannot serve `commonMain` for a native target while a Kotlin DSL can | Source-set architecture, `expect`/`actual` mechanics, platform abstraction generally, native interop, iOS integration architecture, sharing policy | L5.5, which uses exactly one platform-binding example and names E33 for the rest |
| **Lifecycle and navigation** | The conclusion that an owner's lifetime bounds what a graph retains | Activity and Fragment lifecycle, navigation APIs, back-stack mechanics, the `SavedStateHandle` API surface, navigation arguments | L2.4, L4.2 and L4.4, which use lifetime facts and teach no lifecycle or navigation API |
| **Persistence** | That memory is not durability | Room, SQL, migrations, DataStore, storage design | L2.3 and L4.2, in one sentence each |
| **Architecture (E26)** | Applying direction, ownership and proportionality to construction | Everything in [Part 13](#part-13--boundaries-with-the-shipped-curricula) | Throughout; the ledger is Part 3 |

---

## Handoff

**Sequencing.** E27-02 → E27-03 → E27-04 → E27-05 → E27-06 → E27-07 → E27-08 → E27-09, in Unit
order. The order is a real dependency chain: Unit 2's vocabulary is what Units 3–5 encode,
Unit 4 assumes Unit 3's Dagger, Unit 5 assumes Units 1–3, and Unit 6 compares four things that
must all exist first.

**What each authoring issue must not redesign.** Unit identity, Unit title, Lesson identities,
Lesson order, primary and supporting mappings, and the Teach/Bridge/Reference/Exclude decisions
are settled here and in the blueprint. An authoring issue that finds one of them wrong updates
**both documents in the same change** and records why, as E24-07 did for two Lesson titles.

**What E27-09 should verify beyond its own acceptance criteria:**

- Terminology is consistent across the six Units **and with the shipped Architecture and
  Coroutines Units**, against the blueprint's
  [terminology register](dependency-injection-learning-blueprint.md#terminology-this-curriculum-fixes).
  "Scope", "module", "owner" and "resolution" are the four to read hardest: the first two
  collide with vocabulary other curricula and Gradle already own, and the last two are where
  Units 4 and 5 are most likely to drift.
- No E27 Lesson re-derives a shipped E26 argument. L1.2 against
  `lesson_when_an_interface_is_a_boundary`, L1.3 against
  `lesson_dependency_inversion_in_practice`, and L2.3 against
  `lesson_choosing_the_owner_by_lifetime` are the three boundaries to read hardest.
- The [E26 ledger](#part-3--the-e26--e27-handoff-ledger) is walked deferral by deferral, and
  each row is marked answered by a shipped Lesson or still deferred with its reason. One row is
  expected to come back a **partial** answer by design: the multiplatform ViewModel construction
  detail E26 Part 8 recorded is bridged in L5.4 and not taught.
- The three shipped Lessons that point at this curriculum —
  `lesson_state_holder_responsibility`, `lesson_dependency_inversion_in_practice` and
  `lesson_screen_state_owner_boundary` — are re-read and confirmed to still read correctly.
- Every claim in [Part 7](#part-7--this-repositorys-own-koin-graph-as-evidence) is re-checked
  against the code as it stands, and every claim in
  [Part 8](#part-8--dagger-hilt-and-koin-source-sensitive-findings) against the documentation as
  it stands.
- **No module declares Dagger or Hilt**, verified by search rather than by assumption.
- Unit practice for each of the six Units is re-computed through the production resolver with
  the routing findings in [Part 6](#part-6--unit-practice-routing-modelled-now) in mind.

**Limitations carried forward, none of which is a defect to fix during authoring:**

1. **Two Questions are semantically premature in the Unit they reach** —
   `hilt_field_injection_framework_classes` in Unit 1 and `dagger_compile_time_graph_validation`
   in Unit 2. Both mappings are honest and both are recorded; the remedy is an E27-08 re-map,
   not a mapping the curriculum does not believe.
2. **Unit 2 reaches only two ACTIVE Questions, and one of them is the premature Dagger one.**
   Six Lessons of acceptance-critical reasoning are assessed by `di_scopes_001` alone.
3. **Unit 5 reaches only two ACTIVE Questions across five Lessons**, and three of its five
   primary concepts hold none at all. It is the epic's worst-covered Unit.
4. **Unit 6 reaches one Question, shared with Unit 1**, and `di_framework_tradeoffs` holds no
   ACTIVE Question, so the Unit the epic ends on is effectively unassessed.
5. **`di_scopes` must carry three distinguishable ideas** — lifetime requirement, owner, scope
   rule — and one Subtopic cannot express which a Question is about. This limits how precisely
   E27-08 can route GAP-U2-A and GAP-U2-B, and only a taxonomy split would change it. E27 does
   not make one.
6. **`koin_kmp` exists in the `kmp` Topic, ACTIVE and empty**, duplicating `koin_multiplatform`.
   Recorded as a guard; not resolved.
7. **The two official Hilt sources currently document different component sets**, and three
   ACTIVE Questions rest on the wider one. Recorded in
   [the Hilt findings](#hilt-findings); no Question becomes wrong.
8. **Two ACTIVE Koin Question explanations are overtaken by current Koin documentation** on the
   compile-time-against-runtime point. Both are correction candidates, not defects.
9. **The `dependency_injection` Topic holds no ACTIVE `ADVANCED` Question.** Not a quota to
   fill; recorded because four pieces of the finished curriculum's reasoning would justify one.
10. **This repository's Koin graph is small, local-first and single-module.** It cannot
    illustrate a large graph, a second implementation of anything, scoped Koin definitions, or
    module inclusion, so L5.1, L5.3 and Unit 6's growth scenarios must use Examples A and B
    rather than stretching Example C.
11. **`gh` is not installed in this environment**, so issue #385 was read through
    `.github/project/backlog.yml`, which is the synchronised source of the issue text.
12. **`iosArm64` is not compiled locally or on CI.** Inherited from E25 and E26 and unchanged;
    it bounds what may be claimed about the iOS host's startup path beyond reading its source.

---

## Authoring outcomes

Each authoring issue appends its outcomes here, following the precedent of
`docs/content/architecture-units-1-6-plan.md`: what did not change, corrections made during
review, claims that were verified rather than reasoned about, source decisions, the semantic
review of the Questions the Unit now reaches, and the cross-links and validation performed.

### E27-02 authoring outcomes

**Issue #386, `task/E27-02`. Authored 2026-09-18.** Unit 1 ships; Units 2–6 remain unauthored.

#### What shipped

**One Unit, `unit_dependency_injection_as_object_construction`, titled "Dependency Injection as
Object Construction", home Topic `dependency_injection`.** It is appended after the six
architecture Units, which keeps the document's Topic grouping and matches the Topic order in
`initial_curriculum.json`. Read from production after the change: **25 active Units and 107
active Lessons**, up from 24 and 101, and the `dependency_injection` Topic now exposes exactly
one Learning Unit with six Lessons.

| # | Lesson id | Title | Primary | Supporting |
| --- | --- | --- | --- | --- |
| L1.1 | `lesson_who_constructs_this_object` | Who Constructs This Object? | `di_fundamentals` | `constructor_injection`, `manual_di`, `service_locator_vs_di`, `separation_of_concerns` |
| L1.2 | `lesson_a_dependency_should_be_visible` | A Dependency Should Be Visible | `constructor_injection` | `di_fundamentals`, `interface_boundaries`, `dependency_direction`, `test_doubles` |
| L1.3 | `lesson_injected_inverted_or_both` | Injected, Inverted, or Both? | `di_fundamentals` | `dependency_direction`, `interface_boundaries`, `solid`, `constructor_injection` |
| L1.4 | `lesson_one_place_that_knows_how_to_build` | One Place That Knows How to Build | `composition_root` | `manual_di`, `dependency_graphs`, `service_locator_vs_di`, `separation_of_concerns`, `layered_architecture` |
| L1.5 | `lesson_asking_for_it_or_being_given_it` | Asking For It, or Being Given It | `service_locator_vs_di` | `di_fundamentals`, `composition_root`, `constructor_injection`, `test_di` |
| L1.6 | `lesson_when_wiring_it_yourself_is_enough` | When Wiring It Yourself Is the Right Answer | `manual_di` | `composition_root`, `dependency_graphs`, `di_framework_tradeoffs`, `architecture_tradeoffs` |

**Every identity is the planned one**, unchanged in id, title, order, primary concept and
supporting set. No Lesson was added, removed, split or merged, and no supporting concept was
promoted to primary.

#### Deviations from the plan

Two, both small and both additive.

1. **Example A's clock is authored as a concrete `SystemClock`** rather than as the plan
   diagram's generic `Clock` node. The concrete name is what makes "injection does not require
   interfaces" demonstrable instead of asserted — L1.2 injects it by class, with no abstraction
   anywhere — and the type is defined in the snippet that introduces it, so nothing depends on a
   platform type of the same name.
2. **Three backward links were added beyond the four the plan's link table requires**, plus the
   intra-Unit links. The plan's table names L1.2, L1.3, L1.4 and L1.6 links into shipped
   architecture Lessons; all four ship as specified. The additions are L1.1 →
   `lesson_state_holder_responsibility`, which is the shipped Lesson whose deferral of
   construction the E26 handoff ledger assigns to L1.1, and one intra-Unit link per Lesson from
   L1.2 onwards to the Lesson whose conclusion it consumes. Every one is backward and resolvable;
   nothing points forward.

Nothing else departed from the plan. No Lesson boundary was found to be wrong, so neither this
document's identities nor the blueprint's Unit map needed editing.

#### Content structure

Every Lesson carries CORE, PRACTICAL and SENIOR sections, 20–26 blocks and roughly
1,350–1,750 words, which sits inside the range the shipped E23–E26 Lessons occupy and inside
Rule 8's 5–10 minute target. Each carries two to three Sources, a `KEY_TAKEAWAY` in CORE, at
least one `COMMON_MISTAKE` in PRACTICAL and an `INTERVIEW_FOCUS` closing SENIOR. Comparison
tables are used five times, each for a comparison that is genuinely clearer in a table: the
three construction options, the injection-against-inversion decisions, the four
injected/inverted combinations, and the located-against-injected class.

#### Example A, as actually authored

One graph, evolved across the six Lessons rather than restated:

```text
main
 └─ StudySessionController
     ├─ QuestionRepository   (concrete class in L1.1–L1.2; an interface from L1.3)
     │    └─ LocalQuestionRepository → BundledQuestionDataSource → QuestionCatalogConfig
     └─ SystemClock          (concrete throughout)
```

- **L1.1** introduces `StudySessionController`, `QuestionRepository` and `SystemClock`, all
  concrete, and writes the controller three ways.
- **L1.2** injects `SystemClock` by class, then shows the same controller with nine constructor
  parameters as a design signal.
- **L1.3** is where `QuestionRepository` becomes an interface, at the one point a boundary is
  earned, with `LocalQuestionRepository` and `BundledQuestionDataSource` appearing beneath it.
  `SystemClock` deliberately stays concrete, which is the comparison the Lesson needs.
- **L1.4** assembles the graph in `main`, then grows it with `QuestionCatalogConfig` two levels
  down, then shows a `ReviewScreenPresenter` that rebuilds the same chain internally.
- **L1.5** replaces the controller's constructor with `ServiceRegistry.resolve(...)` calls
  against the same two collaborators.
- **L1.6** adds `QuestionBrowser` and a per-use `SessionSummaryBuilder` so that sharing and
  fresh-per-use construction are both visible, then judges this graph.

A consistency pass was run over every snippet: type names, constructor shapes, parameter order,
`suspend` placement, visibility modifiers and the point at which the interface appears all agree
across the six Lessons. The only secondary micro-example is L1.3's
`LoadQuestions`/`SqlQuestionDatabase` pair, which exists because the "injected but not inverted"
shape needs a consumer whose source names a storage package; its packages
(`com.example.study.session`, `com.example.study.data.sql`) were chosen not to collide with the
architecture curriculum's own worked example.

#### Misconception coverage

Each is defeated by reasoning in place rather than collected into a myths list.

| Misconception | Where it is defeated |
| --- | --- |
| Dependency injection requires a framework | L1.1 CORE definition and its `COMMON_MISTAKE`; reinforced by L1.6's whole argument |
| Dependency injection requires interfaces | L1.2's concrete `SystemClock` injection and its `COMMON_MISTAKE` |
| Constructor injection is a Dagger feature | L1.2 SENIOR, "Why the technique is framework-independent" |
| Injection and inversion are the same thing | L1.3 entire, with an example of each without the other |
| A DI framework fixes dependency direction | L1.3's `COMMON_MISTAKE`, citing `lesson_dependency_inversion_in_practice` |
| Every class should resolve from a container | L1.5's four costs, and L1.4's warning about a centralised graph classes reach into |
| Service locator and injection are equivalent because both return objects | L1.5 CORE opens on exactly that and moves the comparison off the result |
| More interfaces automatically improve testability | L1.2 SENIOR: substitution rests on the dependency being explicit, not on interface count |
| Hiding a large constructor behind a container fixes an over-responsible class | L1.2's second `COMMON_MISTAKE` |
| Manual DI is only for toy projects | L1.6's `COMMON_MISTAKE` and its recommendation-versus-rule reading |

**Two defects found in review and corrected.** Both were in L1.3 and L1.6, and both were real
rather than stylistic.

1. **The four-combination table's *not injected, inverted* row refuted itself.** It was authored as
   "the consumer owns the contract and the class still constructs the implementation itself", and
   its own explanation admitted that constructing the implementation requires importing it, which
   restores the arrow the interface removed. By the Lesson's own import test that arrangement is
   therefore **not** inverted, so the row claimed a combination it did not demonstrate. It is now
   the consumer-owned contract with the implementation **fetched from a registry** — version B of
   L1.1 behind an owned interface. The consumer names its own contract and the registry and never
   the implementation package, so the source arrow holds; nothing supplied the collaborator, so
   nothing was injected. The paragraph after the table now also says what occupies that row, which
   strengthens rather than weakens the warning it already carried.
2. **L1.3's closing callout said "give one example of each without the other".** The second example
   it then gave — a consumer-owned interface constructed in `main` — is inverted **and** injected by
   hand, which is the very reading §22 of the issue forbids. The callout now asks the learner to
   show the two decisions coming apart, and labels that example as inverted and hand-injected,
   making the point it was always meant to make: inversion needs no container, not no injection.
3. **L1.6 claimed manual wiring defers graph errors to run time.** One cost-list item read
   "Validating the graph before the program runs becomes valuable enough to pay for, because a
   missing or ambiguous edge is currently found by running the program." That is false for the
   hand-written wiring this Unit teaches and contradicts both L1.1's and L1.5's comparison rows,
   which correctly say a missing constructor argument is a compile error at the construction site —
   and it would have handed a learner a false reason to adopt a container. It is replaced by a
   hazard hand-wiring genuinely has: several values of the same type circulating in one assembly,
   where passing the wrong one type-checks. The replacement restates the compile-time guarantee
   explicitly, so the item now reinforces the Unit's thesis instead of undercutting it.

Two further corrections the issue asked for are handled as wording rather than as misconceptions.
**Injection is defined by who supplies the dependency, not by the constructor signature**: L1.1
SENIOR states the preferred form and then explicitly refuses the narrow definition, so Unit 4's
field injection will not contradict Unit 1. And **the four-combination table cannot be read as
"framework-free means not injected"**: the paragraph after it says so directly, and places the
inverted-without-a-container shape in the *injected and inverted* row.

#### Source decisions

Every source was re-opened on 2026-09-18 and the specific claim re-read; none was carried over
from E27-01 on trust. Sources are attached per claim rather than one general DI page per Lesson.

| Source | Claims it settles | Used by |
| --- | --- | --- |
| [Dependency injection in Android](https://developer.android.com/training/dependency-injection) | The three ways a class gets an object and "The third option is dependency injection"; constructor against field injection and that framework classes are instantiated by the system; the service-locator comparison, including that dependencies "are encoded in the class implementation, not in the API surface" | L1.1, L1.2, L1.5, L1.6 |
| [Manual dependency injection](https://developer.android.com/training/dependency-injection/manual) | The container placed at the application entry point; the named costs — "a lot of boilerplate code (such as factories), which can be error-prone" and "manage the scope and lifecycle of the containers yourself"; the recommendation "When possible, it's recommended to use Hilt rather than manual dependency injection" | L1.4, L1.6 |
| [Inversion of Control Containers and the Dependency Injection pattern](https://martinfowler.com/articles/injection.html) | The name of the pattern and its three forms, which is what supports constructor injection being framework-independent; "the configuration of services is separated from their use"; the assembler; and the service-locator comparison, verbatim on searching the source for calls to the locator | L1.1, L1.2, L1.3, L1.4, L1.5, L1.6 |
| [Composition Root](https://blog.ploeh.dk/2011/07/28/CompositionRoot/) | "a (preferably) unique location in an application where modules are composed together", "as close as possible to the application's entry point", and that a DI container should only be referenced from it — which is the container-is-not-the-root distinction | L1.4 |
| [The Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html) | The dependency rule, reused from E26 rather than re-argued | L1.3 |
| [Kotlin properties: late-initialized properties](https://kotlinlang.org/docs/properties.html) | That a `lateinit` property accessed before initialisation throws `UninitializedPropertyAccessException`, which is the formedness failure L1.2 makes concrete | L1.2 |

**Three claims were narrowed rather than sourced as planned.**

1. **The constructor-injection chronology claim was dropped.** The blueprint's L1.2 Senior layer
   proposed that constructor injection "predates every library in this curriculum and is
   specified by an independent standard the frameworks adopt rather than define". Establishing
   either half cleanly would need historical or specification sourcing that does not carry its
   weight for the pedagogical point, so L1.2 makes the stronger and simpler claim instead:
   constructor injection is a framework-independent technique that the frameworks **encode or
   automate rather than define**, supported by Fowler's description of it as one of the pattern's
   three forms and by the observable fact that swapping frameworks leaves constructors untouched.
   No history is taught.
2. **The Android recommendation is reported with its condition and its publisher.** L1.6 quotes
   "When possible" and says who publishes both the page and the library it recommends, then
   reads it as strong evidence about a trade-off rather than as a rule. The official guidance is
   neither contradicted nor converted into a requirement.
3. **The service-locator failure-timing claim is scoped to the demonstrated design.** L1.5 states
   explicitly that a registry validating registrations at start-up narrows the window and one
   resolving lazily widens it, and carries forward only the general claim that a requirement
   absent from the signature cannot be checked by the compiler at the construction site.

No framework-marketing page, tutorial aggregator or forum answer was consulted or cited, and no
Koin, Dagger or Hilt documentation was needed, because Unit 1 makes no framework claim.

#### Cross-links

All backward, all resolving, no forward link anywhere — the five later Units do not exist, so a
forward `relatedLessonIds` entry would fail `UNKNOWN_RELATED_LESSON`. Forward pointing is done in
prose, naming the Unit.

| Lesson | Links to |
| --- | --- |
| L1.1 | `lesson_state_holder_responsibility` |
| L1.2 | `lesson_when_an_interface_is_a_boundary`, L1.1 |
| L1.3 | `lesson_dependency_inversion_in_practice`, `lesson_dependency_direction_and_boundaries`, L1.2 |
| L1.4 | `lesson_what_architecture_decides`, L1.1 |
| L1.5 | L1.1, L1.2, L1.4 |
| L1.6 | `lesson_smallest_sufficient_architecture`, `lesson_layers_and_their_cost`, L1.4 |

**No shipped Lesson was edited.** No reciprocal link was added for symmetry, and the three
shipped Lessons that point at this curriculum in prose were left alone; E27-09 re-reads them.
**No contradiction with any E26 Lesson was found.** The two E26 conclusions this Unit carries
forward — that a container changes no import, and that structure earns its cost — are cited and
applied rather than re-derived, and L1.2 cites the interface boundary test instead of re-arguing
it.

#### Practice pool, resolved through production

`LearningUnitPracticeIntegrationTest.theDependencyInjectionFoundationsUnitPractisesOnlyItsFivePrimaryConcepts`
drives the real Practice Builder, the real resolver and the real selector against the imported
bank. **Expected five, resolved five**, and the scope is exactly the five primary concepts:

| Question | Subtopic | Level |
| --- | --- | --- |
| `di_constructor_injection_testability` | `di_fundamentals` | FOUNDATION |
| `hilt_field_injection_framework_classes` | `constructor_injection` | FOUNDATION |
| `composition_root_001` | `composition_root` | FOUNDATION |
| `service_locator_vs_di_001` | `service_locator_vs_di` | FOUNDATION |
| `manual_di_graph_growth_cost` | `manual_di` | APPLIED |

**4 FOUNDATION, 1 APPLIED, 0 ADVANCED**, matching the plan's model exactly. No supporting concept
contributes — the test asserts that `test_doubles`, `test_di` and the six architecture bridges
reach nothing — and the intersection with the architecture foundations Unit's pool is empty, which
is the plan's prediction that no shipped Unit's practice changes.

#### Semantic review of the five Questions the Unit now reaches

Each was re-read in full against the **finished** prose, not against the mappings.

- **`di_constructor_injection_testability` — answerable.** Its three keyed options map onto
  L1.2's four guarantees: visibility in the type, full formedness with no init call, and
  substitution by a caller supplying something else. Its sharpest distractor — that a framework
  could build the class with nothing declaring how the collaborators are provided — is
  refused by L1.2 SENIOR, which says a framework arranges for the arguments to be present and
  does not define the technique, and by L1.4's container-is-not-the-root distinction. Its other
  distractor, that collaborators become optional, is refused by the "cannot be skipped" guarantee
  and by L1.2's refusal to make a required dependency nullable. Immutability is taught as the
  fourth guarantee and again in the `lateinit` contrast. **Routing note, unchanged:** the Question
  sits on `di_fundamentals`, so it is reached through L1.1 and L1.3 rather than through L1.2,
  which is the Lesson that teaches it.
- **`composition_root_001` — answerable.** L1.4 CORE gives the keyed answer in its own words
  (assembly centralised near the entry point) and kills all three distractors explicitly: the
  queried registry is named as the opposite design in the same Lesson's `COMMON_MISTAKE` and
  taught in full by L1.5, scoping and lifetime are named as a separate decision deferred to
  Unit 2, and caching never appears as part of the definition. The Question remains definitional
  where the Lesson teaches a judgement; it was not rewritten and the Lesson was not narrowed to
  match it.
- **`service_locator_vs_di_001` — answerable.** Its keyed option is L1.5's first and third
  named cost verbatim in substance. The Lesson deliberately goes further, adding the
  integration-boundary qualification the Question does not pose, and was **not** weakened to
  match: a reader who has understood L1.5 answers this Question easily, and also knows the
  qualification the Question omits.
- **`manual_di_graph_growth_cost` — answerable from first principles.** Its keyed option is the
  first item in L1.6's cost list, worked through concretely with three entry points and an
  intermediate helper. All three distractors are refused by the Lesson's opening argument that
  manual wiring loses no capability: scoping and sharing without a generator, constructor
  injection that does not give way, and a shared instance between two consumers shown as one
  `val`. L1.6 makes the argument in its own vocabulary and does not reuse the Question's wording.
- **`hilt_field_injection_framework_classes` — structurally reachable, not taught by this Unit.**
  Its stem, options and explanation are entirely about Hilt and `@AndroidEntryPoint`. Unit 1
  names no framework API at all, so a reader who has finished it cannot reason to the keyed
  answer from what was taught. What Unit 1 does give is the adjacent generic reasoning — L1.1
  SENIOR's statement that injection is about who supplies rather than which member it lands on,
  and L1.5's integration-boundary qualification — which is a bridge toward the Question, not
  coverage of it. **Unit 1 practice is therefore not semantically clean, and this outcome does
  not claim it is.**

#### Known premature Hilt routing

`hilt_field_injection_framework_classes` maps to `constructor_injection`, which is L1.2's primary
concept, so it enters Unit 1 practice three Units before Hilt exists. This is E27-01's recorded
finding and it was **not** repaired here: L1.2's primary mapping is correct and unchanged, Unit
practice was not removed, the Question was not re-mapped or altered, and Hilt was not taught
prematurely to close the gap. The limitation is pinned by an assertion in the practice test, so
E27-08's re-map to `hilt_fundamentals` — which must be decided together with GAP-U1-E — has to
re-state it rather than silently repair it.

#### GAP-U1-A … GAP-U1-E after authoring

E27-08 owns disposition. Recorded here is only what finished authoring changed about each.

| Gap | Status after authoring |
| --- | --- |
| GAP-U1-A — injected against inverted in one graph | **Still open as planned, and instructionally sharper.** L1.3 ships both shapes explicitly and adds a four-combination table, so a Question can now be written against a specific taught arrangement rather than against a definition. The one authoring addition E27-08 should exploit: the Lesson warns against reading "framework-free" as "not injected", which is a plausible distractor axis |
| GAP-U1-B — choosing construct / fetch / receive from a stated requirement | **Still open as planned.** L1.1's comparison table gives seven properties that distinguish the three, which is a ready-made basis for a decision Question; nothing about authoring reduced the gap |
| GAP-U1-C — recognising construction knowledge leaking outside the composition root | **Still open as planned, and now has a worked referent.** L1.4 ships the leaking `ReviewScreenPresenter` and the diagnosis sentence, so the gap can be closed with a locate-the-responsibility Question rather than another definition |
| GAP-U1-D — hidden lookup against legitimate integration-boundary resolution | **Still open as planned, and it is the gap authoring most strengthened.** L1.5 now teaches the deciding question and four acceptability criteria, so a two-call judgement Question has explicit taught criteria to assess. It remains the gap most specific to this curriculum's argument |
| GAP-U1-E — generic constructor injection reachable from the Lesson that teaches it | **Still open, and unchanged in shape.** L1.2 genuinely teaches the reasoning `di_constructor_injection_testability` assesses, while that Question sits on `di_fundamentals` and `constructor_injection`'s only ACTIVE Question is the Hilt one. Authoring confirms the mismatch is real rather than theoretical; the two re-maps still have to be decided together |

**No Question was authored, changed, re-mapped, re-levelled or deprecated, and no AnswerOption was
touched.** The `dependency_injection` Topic still holds 27 Questions — 23 ACTIVE (14 FOUNDATION,
9 APPLIED, 0 ADVANCED) and 4 DEPRECATED — and no taxonomy entry was added, removed or edited.

#### Tests changed, and why

| File | Change | Why production data made it necessary |
| --- | --- | --- |
| `BundledLearningCurriculumTest` | Unit id, title and home-Topic lists extended by one; Lesson id/title order and primary mappings added for the new Unit; three new tests — `dependencyInjectionFoundationsUnitKeepsItsPlannedBridgesOutOfPrimaryPractice`, `dependencyInjectionFoundationsUnitLinksBackwardsOnlyToShippedArchitectureAnchors` and `dependencyInjectionFoundationsUnitTeachesEveryConclusionWithoutAFramework` | The document now spans four home Topics. The supporting mappings are where `test_doubles` and `test_di` would silently become practice, and the framework-free test is the acceptance criterion about syntax expressed as a check |
| `LearningUnitPracticeIntegrationTest` | `existingLearnerTraversesTheExpansionWithLiveParentProgressAndDurableIdentities` extended to walk the dependency-injection Unit before Continue Learning may report Complete, and its final studied-record count raised from 100 to 106; new test `theDependencyInjectionFoundationsUnitPractisesOnlyItsFivePrimaryConcepts` | Continue Learning walks the whole document, so a fourth home Topic changes the traversal. The Unit is deliberately **not** in the shared expectation table, because that table's loop cannot state which of the five resolved Questions is semantically premature — the bespoke test pins the exact five ids and that limitation instead |

The framework-free test inspects **authored code blocks only**. Policing prose would flag the
forward sentences the plan explicitly permits, which is the brittleness E27-02's brief warned
against. No test was added that only restates schema validation `LearningCurriculumValidatorTest`
already performs, and the data-driven suites — the reader journey over every shipped Unit, Topic
Detail's Unit rows, the end-to-end repository path — needed no edit because they read the
document rather than list it.

#### Generated coverage

`python3 tools/learning_question_coverage.py --write` then `--check`; the snapshot reports
**25 active units, 107 active lessons**, the new Unit's pool as 4 FOUNDATION and 1 APPLIED, and
**0 primary subtopics with no active question** — all five of this Unit's primaries hold one. The
generated per-Lesson tables expose the premature routing plainly: L1.2's only primary Question is
`hilt_field_injection_framework_classes`, and `di_constructor_injection_testability` appears
under L1.1 and L1.3 instead. Nothing in the file was edited by hand.

#### Validation performed

| Command | Result |
| --- | --- |
| `python3` structural pre-check over both bundled JSON documents | Unit and Lesson ids unique across the whole document, every primary and supporting id an ACTIVE Subtopic, no primary/supporting overlap, every `relatedLessonIds` target resolvable, non-self and backward in document order, no forward link. No defect |
| `python3` framework-syntax scan over every block of the new Unit | Zero occurrences of Dagger, Hilt or Koin syntax in code, prose, bullets or table cells. Framework **names** occur in seven paragraphs in total, all of them forward-reference prose: exactly one bounded statement in L1.1, two in L1.2, one each in L1.4 and L1.5, and two in L1.6 — one of which is the quoted Android recommendation. L1.3 names no framework at all |
| `./gradlew :shared:jvmTest --tests "*BundledLearningCurriculumTest*"` | 45 tests, 0 failures |
| `./gradlew :shared:jvmTest --tests "*LearningUnitPracticeIntegrationTest*"` | 15 tests, 0 failures |
| `./gradlew :shared:jvmTest --tests "*LearningProductionContentJourneyTest*" --tests "*LearningContentEndToEndTest*" --tests "*LearningCurriculumValidatorTest*"` | Passed, including the journey suite that renders every authored block of the new Unit in the real reader, checks the reading column never widens, and opens authored Source links through the app's own URI boundary |
| `./gradlew :shared:jvmTest --rerun-tasks` | **1,461 tests, 0 failures, 0 skipped** |
| `python3 tools/learning_question_coverage.py --write` then `--check` | Snapshot regenerated and reported current |
| `cd tools && python3 -m unittest test_learning_question_coverage.py` | 21 tests, OK |
| `./gradlew :shared:check` | **BUILD SUCCESSFUL** — `jvmTest`, `testAndroidHostTest`, `jsTest`/`jsBrowserTest`, `wasmJsTest`/`wasmJsBrowserTest` and `allTests` |
| `./gradlew :shared:iosSimulatorArm64Test --rerun-tasks` | **BUILD SUCCESSFUL.** Re-run explicitly because the task reported UP-TO-DATE inside `:shared:check` against results predating this change |
| `./gradlew :androidApp:assembleDebug` | **BUILD SUCCESSFUL** |
| `git diff --check` and `git status --short` | Four files changed — the bundled learning document, two jvm test files and the generated coverage snapshot, plus this plan — with no build or cache output and no whitespace defects |

#### Not validated

- **`iosArm64` is not compiled locally or on CI**, unchanged from E23–E26. `iosSimulatorArm64Test`
  was run and passed; the device target was not. Unit 1 is bundled content with no
  target-specific code and makes no platform claim, so nothing in its prose depends on that
  target, but the limitation is reported because it remains true of the repository.
- **No platform-specific behaviour is claimed from compilation.** The JS, Wasm and Android host
  results above prove the document decodes and the suites pass on those targets; they are not
  evidence about rendering on any device, and the Unit was not read on a running Android, iOS or
  web host.
- **No CI run is claimed.** Nothing here was observed on GitHub Actions.
- **Backlog validation was not run**: `PyYAML` is unavailable in this environment, so
  `.github/project/backlog.yml` was read as text rather than parsed. **`gh` is not installed**, so
  issue #386 was read from the backlog entry — whose `issue`, `approach` and twelve
  `acceptance_criteria` are the same text — rather than from GitHub directly.
- **The prose itself is editorial.** No automated check can confirm that a Lesson teaches what it
  claims; the semantic review above is a judgement, as Rule 10 of the authoring contract requires.

#### Production code untouched

**No production dependency-injection change of any kind.** No Koin module, host `startKoin`
function, `koinViewModel()` call site, service construction or state-holder lifetime was edited;
the Koin version in `gradle/libs.versions.toml` is unchanged at 4.2.2; and no Dagger, Hilt or
other dependency-injection dependency was added to any module. No UI, navigation, progress,
assessment, theme, icon or application-identity code was touched — the change is bundled content,
two test files and documentation.

---
