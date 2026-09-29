# KMP Content-Separation Audit

Audit and plan for making Kotlin Multiplatform / Compose Multiplatform material an optional,
separately hideable part of the curriculum. This document is the plan: no curriculum JSON,
application code, schema or UI was changed by it. Progress against the
[Migration Order](#migration-order) is recorded under
[Implementation status](#implementation-status).

- **Baseline:** local `HEAD` = `origin/main` at `6cc8452` (Quality improvements #436).
- **Inputs read:** `initial_curriculum.json` (17 Topics, 361 Subtopics, 478 Questions: 437
  ACTIVE, 41 DEPRECATED), `learning_curriculum.json` (30 Units, 135 Lessons), and the
  `commonMain` sources named under [Runtime Visibility Surfaces](#runtime-visibility-surfaces).
- **Method:** every ACTIVE Question and every Lesson block was scanned for KMP vocabulary
  (Multiplatform, KMP, `commonMain`, `expect`/`actual`, Kotlin/Native, Swift, iOS, desktop,
  web, browser, Wasm, JS, SQLDelight, Ktor, Koin, source set, host, shared code). Every hit
  was then read in context and classified by the **engineering concept tested or taught**,
  not by the string. Source URLs were considered only where they changed learner-visible
  meaning.

## Executive Summary

**`topicId == "kmp"` is sufficient as the single visibility boundary.** No second
classification dimension (`isKmp`) is needed. Every genuinely KMP-specific Question fits an
existing `kmp` Subtopic, the existing `SUBTOPIC_TOPIC_MISMATCH` validation already forces a
Question's Subtopic to share its Topic, and a Learning Unit's home Topic decides where it
is browsed. The rule becomes complete once the content is moved and three authoring
invariants are added: a KMP-home Lesson has only `kmp` primary Subtopics, a core Lesson
references no `kmp` Subtopic, and a core Lesson does not relate to a KMP Lesson. See
[Content-boundary invariants](#content-boundary-invariants).

Verified baseline:

- Topic `kmp` holds **16 ACTIVE and 2 DEPRECATED** Questions, as the brief stated.
- Topic `kmp` has **27** Subtopics, not 26. The brief's list omits `kmp_dependency_hierarchy`.
- A duplicate KMP Subtopic lives outside the Topic: `dependency_injection/koin_multiplatform`
  ("Koin and Kotlin Multiplatform") duplicates `kmp/koin_kmp` ("Koin in KMP").
- **No Learning Unit has home Topic `kmp` today.** The one KMP-titled Unit,
  `unit_koin_and_dependency_injection_in_kmp`, lives under `dependency_injection`. Only **1 of
  its 5 Lessons** is KMP-specific. The other four are the app's only teaching of core Koin,
  which Android projects use too.

### Question counts (ACTIVE Questions reviewed: 51)

| Classification | Count |
| --- | --- |
| KMP — already correctly classified | **16** |
| MOVE TO KMP (misplaced KMP Questions) | **5** |
| SPLIT (core Question kept + a new KMP Question) | **2** |
| REWRITE CORE | **4** |
| KEEP | **24** |

Core Questions needing a rewrite or split: **6** (4 REWRITE CORE + 2 SPLIT). After migration,
Topic `kmp` holds **23** ACTIVE Questions (16 + 5 moved + 2 new from splits).

Differences from the brief's "clearly KMP" list of seven:

- Five move as the brief expected.
- `compose_lifecycle_collection_stops_the_collector_not_the_producer` is a **SPLIT**. The
  collector-vs-producer lifetime it tests is core Android Compose knowledge, and only its
  desktop framing is KMP.
- `koin_container_startup_composition_boundary` is a **REWRITE CORE**. It tests Koin
  composition-root placement, and it is the only Question in `koin_fundamentals`.

### Learning counts (30 Units and 135 Lessons scanned; 28 Lessons and 2 Units classified)

| Classification | Count |
| --- | --- |
| KMP-specific Learning Units today | **1** (KMP-titled; only 1 of its 5 Lessons is KMP) |
| MOVE UNIT | **1** — `unit_koin_and_dependency_injection_in_kmp`, after its four core Lessons are re-homed |
| MOVE LESSON | **1** — `lesson_one_graph_across_platforms` |
| EXTRACT KMP SECTION (core Lessons containing KMP sections) | **10** |
| REWRITE CORE (incidental core KMP mentions) | **13 Lessons + 1 Unit summary** |
| KEEP (reviewed, no KMP teaching) | **5** |
| New KMP Lessons (assembled from extracted material) | **3** |

About 20 further Lessons match only the phrase "Measured on a JVM target". That phrase
reports where an observation was measured. It is not KMP teaching, and those Lessons need no
change.

### Other findings

- **No architectural blocker.** One design constraint matters. Topic Browser, Topic Detail
  and the app-scoped history consumers currently read curriculum once and cache it. The
  visibility setting therefore has to be observable, and history-derived surfaces have to
  re-derive when it changes. See [Preference Design](#preference-design).
- **No database migration or reset is required.** The importer upserts the whole bundle on
  every launch, which already handles re-homing Questions and Subtopics. Attempts store only
  `questionId`, study records only `lessonId`, and saved Questions only `questionId`. The
  preference goes in the existing key-value `AppPreferenceStorage`, not Room.

## Question Migration Table

### KMP — already correctly classified (16)

All sixteen test KMP concepts, and none needs wording changes. The two DEPRECATED
Questions (`shared_vs_platform_code_001`, `kmp_shared_ui_platform_experience_tradeoff`) are
hidden with the Topic and need no action.

| Question ID | Current Topic / Subtopic | Classification | Proposed | Wording change | Rationale |
| --- | --- | --- | --- | --- | --- |
| `kmp_swift_interop_suspend_flow` | kmp / shared_vs_platform_code | KMP — correct | unchanged | none | Swift export of `suspend`/`Flow`. |
| `expect_actual_001` | kmp / expect_actual | KMP — correct | unchanged | none | `expect`/`actual` purpose. |
| `kmp_source_set_hierarchy_resolution` | kmp / kmp_source_sets | KMP — correct | unchanged | none | `iosMain` intermediate source set. |
| `kmp_expect_actual_vs_interface` | kmp / kmp_dependency_inversion | KMP — correct | unchanged | none | Interface vs `expect`/`actual`. |
| `kmp_android_library_not_multiplatform` | kmp / kmp_library_selection | KMP — correct | unchanged | none | Android-only libraries in `commonMain`. |
| `kmp_shared_layer_selection` | kmp / kmp_architecture | KMP — correct | unchanged | none | Which layers to share. |
| `kmp_compose_multiplatform_vs_jetpack` | kmp / compose_multiplatform | KMP — correct | unchanged | none | What stays platform-specific with CMP. |
| `kmp_tradeoff_when_not_to_share` | kmp / kmp_tradeoffs | KMP — correct | unchanged | none | Duplication vs shared abstraction. |
| `kmp_vs_compose_multiplatform_scope` | kmp / kmp_fundamentals | KMP — correct | unchanged | none | KMP vs CMP scope. |
| `kmp_native_memory_model` | kmp / kotlin_native | KMP — correct | unchanged | none | Kotlin/Native memory model. |
| `kmp_common_vs_platform_test_placement` | kmp / kmp_source_sets | KMP — correct | unchanged | none | Common vs platform tests. |
| `kmp_ios_target_framework_output` | kmp / kmp_targets | KMP — correct | unchanged | none | iOS framework output. |
| `commonmain_platform_api_availability` | kmp / commonmain | KMP — correct | unchanged | none | API availability in `commonMain`. |
| `kmp_shared_viewmodel_owner_platform` | kmp / kmp_lifecycle_viewmodel | KMP — correct | unchanged | none | Shared ViewModel, platform owner. |
| `kmp_actual_typealias_platform_type` | kmp / kmp_platform_apis | KMP — correct | unchanged | none | `actual typealias`. |
| `compose_multiplatform_generated_resources` | kmp / compose_multiplatform_resources | KMP — correct | unchanged | none | CMP `Res` resources. |

### MOVE TO KMP (5)

| Question ID | Current Topic / Subtopic | Classification | Proposed Topic / Subtopic | Wording change | Rationale |
| --- | --- | --- | --- | --- | --- |
| `retrofit_vs_ktor_client_multiplatform` | networking / retrofit_vs_ktor | MOVE TO KMP | kmp / `ktor_kmp` | none | The deciding fact is that `commonMain` cannot call a JVM-only client. The stem is already a KMP scenario. |
| `koin_multiplatform_common_module` | dependency_injection / koin_multiplatform | MOVE TO KMP | kmp / `koin_kmp` | none | Why Koin, and not Dagger or Hilt, can describe one shared `commonMain` graph for iOS. |
| `koin_shared_and_platform_binding_split` | dependency_injection / koin_multiplatform | MOVE TO KMP | kmp / `koin_kmp` | none | Splitting a graph into shared definitions and per-host bindings. |
| `room_vs_sqldelight_generation_direction` | local_data / room_vs_sqldelight | MOVE TO KMP | kmp / `sqldelight` | none | The stem is a shared KMP data layer on Android and iOS. `sqldelight` is the distinguishing technology. `room_kmp` is an acceptable alternative. |
| `di_strategy_smallest_sufficient_choice` | dependency_injection / di_framework_tradeoffs | MOVE TO KMP | kmp / `kmp_library_selection` | none | Every discriminating fact is KMP: a shared graph across Android, iOS, desktop and web; Hilt and Dagger being unusable as one graph on Native, JS or Wasm. The Subtopic matches the proposed KMP strategy Lesson, so a Mistake Review link lands on the Lesson that teaches it. |

After these moves:

- `retrofit_vs_ktor` and `room_vs_sqldelight` become ACTIVE Subtopics with no ACTIVE
  Questions. That is normal here: 71 ACTIVE Subtopics already have none. Keep both, because
  their names describe comparisons that are legitimate on Android alone.
- `koin_multiplatform` should be **DEPRECATED**. Its name is KMP-specific and it duplicates
  `koin_kmp`.

### SPLIT (2)

| Question ID | Current Topic / Subtopic | Classification | Proposed Topic / Subtopic | Wording change | Rationale |
| --- | --- | --- | --- | --- | --- |
| `compose_lifecycle_collection_stops_the_collector_not_the_producer` | android_ui / compose_state | SPLIT | Core ID **stays** android_ui / compose_state. New KMP Question → kmp / `compose_multiplatform`. | **Core stem:** replace "A Compose Multiplatform desktop screen … The learner minimises the window" with an Android screen and "The user presses Home and the app moves to the background". **Distractor B:** replace the focus-loss wording with the Android analogue, e.g. "a backgrounded Activity is still at the default threshold; only finishing it drops below". **Explanation:** Home → `ON_STOP` → `CREATED`, below `STARTED`. Losing focus to a multi-window sibling or a translucent Activity is `ON_PAUSE` → `STARTED`, which still collects. **Sources:** swap the kotlinlang `compose-lifecycle` page for the Android lifecycle and `collectAsStateWithLifecycle` documentation. Keep `FlowExt.kt` and `SharingStarted.Eagerly`. **New KMP Question** (proposed ID `kmp_desktop_window_lifecycle_collection_threshold`): tests the desktop host mapping (minimise = `ON_STOP`, focus loss = `ON_PAUSE`) that the current explanation already contains. | The core concept — the collector stops while the producer lives under its own scope and sharing policy — is ADVANCED Android material with no other core Question covering it. Only the host is KMP. A new KMP Question is justified: after extraction, the host-mapping teaching becomes KMP learning content that no current Question covers. |
| `kotlin_no_checked_exceptions_interop` | kotlin_language / kotlin_exceptions | SPLIT | Core ID **stays** kotlin_language / kotlin_exceptions. New KMP Question → kmp / `kotlin_native`. | **Core:** stem becomes "…called from Java?". Correct answer: "Java callers get no compiler-enforced signal, so `@Throws` is needed to put the exception in the signature." **Explanation:** Java's catch-or-declare rule, and the fact that without `@Throws` a Java caller cannot even name a checked exception in a `catch` (javac rejects catching a checked exception that the `try` body is not declared to throw). Remove the Kotlin/Native and Swift sentences and the Objective-C interop source. **New KMP Question** (proposed ID `kmp_swift_undeclared_exception_terminates`): an exception not declared with `@Throws` crossing into Swift terminates the program, while a declared one surfaces as a Swift `throws` / `NSError`. | Java interoperability is core Kotlin-on-Android material. The Swift behaviour is a separate, interview-relevant KMP fact. The existing `kmp_swift_interop_suspend_flow` covers `suspend` and `Flow` export, not exceptions, so the new Question adds coverage rather than duplicating it. |

### REWRITE CORE (4)

| Question ID | Current Topic / Subtopic | Classification | Proposed | Wording change | Rationale |
| --- | --- | --- | --- | --- | --- |
| `kotlinx_serialization_001` | networking / kotlinx_serialization | REWRITE CORE | unchanged | **Correct answer:** "It uses compiler-assisted serializers and supports Kotlin Multiplatform." → "It generates serializers at compile time instead of depending on runtime reflection." **Explanation:** delete "it works on Kotlin/Native and Kotlin/JS as well as the JVM". Keep the compile-time serializer generation and the earlier detection of a missing serializer, re-validating that claim against the current documentation. | This preserves the intended question. The tested contrast is compiler-assisted vs reflection-heavy, which is distractor A's opposite. KMP support was a second clause that made the key a compound claim. A separate KMP question is not needed: `kmp_android_library_not_multiplatform` already tests why JVM-only JSON libraries fail in `commonMain`. |
| `koin_container_startup_composition_boundary` | dependency_injection / koin_fundamentals | REWRITE CORE | unchanged | **Stem:** "A KMP application has common repository definitions and one platform storage module per host" → an Android application with repository definitions across Koin modules plus one module binding its Room database and DataStore. **Correct answer:** "In `Application.onCreate` — the composition root — loading every module once before any feature resolves." Distractors keep their shape. **Explanation:** replace "host … shared graph and its platform bindings" with the Application composition root. **Sources:** replace the `koin-mp/kmp` link with Koin's Android start documentation. | The tested concept — startup belongs at the composition root, and a module is not a running container — is core Koin. It is the only Question in `koin_fundamentals`. Moving it would leave core Koin fundamentals untested, while `koin_kmp` already gains two Questions on the shared/platform split. |
| `clean_architecture_dependency_rule_tradeoff` | architecture / clean_architecture | REWRITE CORE | unchanged | Explanation: delete "— and in a multiplatform project it is exactly the layer that can be shared". | Incidental aside; the Question is otherwise Android-core. |
| `dependency_direction_domain_framework_types` | architecture / dependency_direction | REWRITE CORE | unchanged | Explanation: "cannot be shared unchanged with a non-Android target" → "cannot live in a plain Kotlin/JVM module". The correct answer's "portability" stays, because it is a general term. | Incidental KMP hint; the concept is the dependency rule. |

### KEEP (24)

| Question ID | Current Topic / Subtopic | Classification | Rationale |
| --- | --- | --- | --- |
| `kotlin_internal_module_visibility` | kotlin_language / kotlin_visibility | KEEP | "One Gradle source set" is an example of a Kotlin compilation module; core Kotlin. |
| `build_variants_flavor_vs_type` | build_delivery / build_variants | KEEP | Android variant source sets, not KMP source sets. |
| `gradle_variant_source_set_override` | build_delivery / source_sets | KEEP | Same as above. |
| `network_security_config_cleartext_scope` | security / network_security | KEEP | Debug variant source set on Android. |
| `compose_unremembered_observable_state` | android_ui / compose_state | KEEP | KMP-looking text appears only in an AndroidX source URL (`…/commonMain/…`). |
| `compose_equal_value_write_records_no_change` | android_ui / compose_stability | KEEP | Source URL only. |
| `compose_snapshot_read_records_dependency` | android_ui / compose_snapshot_system | KEEP | Source URL only. |
| `compose_snapshot_flow_read_inside_block` | android_ui / compose_snapshot_system | KEEP | Source URL only. |
| `compose_snapshot_flow_conflated_state` | android_ui / compose_snapshot_system | KEEP | Source URL only. |
| `compose_state_flow_value_read_is_not_observation` | android_ui / compose_state | KEEP | Source URL only. |
| `compose_effect_key_equality_decides_restart` | android_ui / compose_side_effects | KEEP | Source URL only. |
| `compose_constant_effect_key_is_a_lifetime_claim` | android_ui / compose_side_effects | KEEP | Source URL only. |
| `compose_current_callback_without_restarting_the_effect` | android_ui / compose_side_effects | KEEP | Source URL only. |
| `compose_missing_on_dispose_accumulates_listeners` | android_ui / compose_side_effects | KEEP | Source URL only. |
| `compose_produce_state_key_change_keeps_the_last_value` | android_ui / compose_side_effects | KEEP | Source URL only. |
| `compose_required_lifetime_exceeds_the_composition` | android_ui / compose_side_effects | KEEP | Source URL only. |
| `di_koin_factory_vs_single` | dependency_injection / koin_definitions | KEEP | Koin is an Android DI option; no KMP content. |
| `koin_definition_from_reuse_requirement` | dependency_injection / koin_definitions | KEEP | Same. |
| `koin_interview_scope_owner` | dependency_injection / koin_scopes | KEEP | Same. |
| `koin_viewmodel_construction_vs_ownership` | dependency_injection / koin_viewmodels | KEEP | "Platform ViewModelStore owner" means the Android owner. |
| `integration_boundary_resolution_vs_service_locator` | dependency_injection / service_locator_vs_di | KEEP | `koinViewModel()` at a Compose destination; core. |
| `scope_rule_requires_lived_owner` | dependency_injection / di_scopes | KEEP | Koin appears only in a source URL. |
| `runtime_input_stays_out_of_graph` | dependency_injection / dependency_graphs | KEEP | Koin appears only in a source URL. |
| `di_graph_check_timing_by_mechanism` | dependency_injection / di_framework_tradeoffs | KEEP | Dagger/Hilt vs Koin compiler safety; no multiplatform claim. It becomes the only ACTIVE Question in `di_framework_tradeoffs`. |

## Learning Migration Table

Mapping key: **REMOVE** means the KMP Subtopic is dropped from the core Lesson because its
KMP teaching leaves; **MOVE** means the mapping goes with the material to a KMP Lesson.

| Unit / Lesson ID | Current home Topic | Current KMP mappings | Classification | Proposed destination | Core rewrite required | Rationale |
| --- | --- | --- | --- | --- | --- | --- |
| `unit_koin_and_dependency_injection_in_kmp` (Unit) | dependency_injection | — | **MOVE UNIT** (after re-homing four core Lessons) | Home Topic → `kmp`; keeps ID and title. Contains `lesson_one_graph_across_platforms` plus the new `lesson_choosing_di_for_a_shared_graph`. | Summary rewritten to describe only the KMP Lessons. | The ID and title describe KMP. Four of its five Lessons are core Koin, and moving them would strip Koin learning from Android mode. It would also break Mistake Review links for the core Koin Questions, which resolve through `primarySubtopicIds` over visible Units. |
| `lesson_the_koin_container_and_its_modules` | dependency_injection | S `kmp_architecture` → REMOVE | **EXTRACT KMP SECTION** | Re-home into the new core Unit `unit_koin_containers_definitions_and_scopes`. The four-host startup example is already covered by `lesson_one_graph_across_platforms` PRACTICAL (deduplicate there). | Yes | Composition-root teaching is core. The four-host example (CORE code, PRACTICAL#1.0) and "shared and platform definitions" are KMP. |
| `lesson_koin_definitions_and_reuse` | dependency_injection | — | KEEP | Re-home to the new core Unit | No | No KMP teaching. |
| `lesson_koin_scopes_and_their_owners` | dependency_injection | — | KEEP | Re-home to the new core Unit | No | No KMP teaching. |
| `lesson_resolving_viewmodels_at_the_boundary` | dependency_injection | S `kmp_lifecycle_viewmodel` → REMOVE | KEEP (mapping fix only) | Re-home to the new core Unit | No | The owners it names are Activity, Fragment and navigation entry; it makes no KMP claim. |
| `lesson_one_graph_across_platforms` | dependency_injection | P `koin_multiplatform` → replace with `koin_kmp`; S `expect_actual`, `platform_implementations`, `kmp_architecture` → KEEP | **MOVE LESSON** | Stays in `unit_koin_and_dependency_injection_in_kmp`, now home `kmp`. Absorbs the multi-host startup example and one sentence from `lesson_policy_and_framework_detail` SENIOR#2.1. | n/a | Wholly KMP: 17 of 17 blocks. |
| `lesson_what_a_container_actually_buys` | dependency_injection | — | **EXTRACT KMP SECTION** | The KMP capability facts (Koin "composed in common KMP code"; the "Target platforms" row) go to `lesson_choosing_di_for_a_shared_graph`. | Yes | Core strategy comparison with KMP capability claims threaded through it. |
| `lesson_three_projects_three_answers` | dependency_injection | S `koin_multiplatform`, `kmp_architecture` → MOVE | **EXTRACT KMP SECTION** | Project C (PRACTICAL#1.6–1.8), the "KMP, so Koin" half of #1.9, and SENIOR#2.1–2.2 → `lesson_choosing_di_for_a_shared_graph`. Remove `relatedLessonIds` → `lesson_one_graph_across_platforms`. | Yes — a replacement Project C (see Core Rewrite Plan) | The method is core. One of its three worked projects is KMP. |
| `lesson_the_smallest_sufficient_strategy` | dependency_injection | — | REWRITE CORE | — | Yes | Four KMP asides (CORE#0.3, PRACTICAL#1.5 row, SENIOR#2.3, SENIOR#2.5 checklist line). The KMP decision is already captured by the extracted Project C. |
| `lesson_remember_saveable` | android_ui | — | **EXTRACT KMP SECTION** | CORE#0.7 callout → `lesson_kmp_viewmodel_owners_across_hosts` (saved state per host). | Yes | The saved-state ladder is core. The callout on what non-Android CMP hosts restore is KMP. |
| `lesson_screen_state_owner_boundary` | android_ui | S `kmp_lifecycle_viewmodel` → REMOVE | **EXTRACT KMP SECTION** | PRACTICAL#1.3 → `lesson_kmp_viewmodel_owners_across_hosts`. | Yes (PRACTICAL#1.0 and the Lesson summary) | Navigation 3 entry-scoped ViewModels are valid on Android. Only the "Compose Multiplatform application" framing and the host paragraph are KMP. |
| `lesson_lifecycle_aware_collection` | android_ui | S `kmp_lifecycle_viewmodel`, `compose_multiplatform` → MOVE | **EXTRACT KMP SECTION** | PRACTICAL#1.0–1.3 and the host part of SENIOR#2.2 → `lesson_kmp_lifecycle_collection_across_hosts`. | Yes — a replacement Android mapping section | The two-gate model is core. The four-host mapping table and `commonMain` dependency are KMP. |
| `unit_observable_state_collection` (Unit summary) | android_ui | — | REWRITE CORE | — | Yes | The summary says "as each Android, desktop, iOS or web host changes activity". |
| `lesson_stability_and_skipping` | android_ui | — | REWRITE CORE | — | Yes | The toolchain disclosure names "Compose Multiplatform 1.11.1". State the Compose compiler and runtime versions instead. |
| `lesson_launched_effect` | android_ui | — | REWRITE CORE | — | Yes | SENIOR#2.0's host/dispatcher caveat can be stated precisely for Android (see plan). |
| `lesson_flow_adapter_or_compose_producer` | android_ui | — | REWRITE CORE | — | Yes | CORE#0.3: delete "available in the common source set of the Compose runtime, so it is not an Android-only mechanism". |
| `lesson_dispatchers` | async_reactive | — | **EXTRACT KMP SECTION** | SENIOR#2.2–2.4 (per-target realization table) → `lesson_kmp_viewmodel_owners_across_hosts` SENIOR (Main per host). | Yes | Dispatchers are core. The five-target realization table is KMP. |
| `lesson_with_context_and_main_safety` | async_reactive | — | REWRITE CORE | — | Yes | PRACTICAL#1.0 rationale "does not assume Dispatchers.IO exists on every target"; SENIOR#2.4 browser sentence. |
| `lesson_sequential_and_concurrent_work` | async_reactive | — | REWRITE CORE | — | Yes | SENIOR#2.3 "single browser execution thread" example → the Android main thread. |
| `lesson_exception_propagation` | async_reactive | — | REWRITE CORE | — | Yes | PRACTICAL#1.5, SENIOR#2.2, SENIOR#2.8: JS/Native caveats → JVM/Android statements. |
| `lesson_shared_state_and_coordination` | async_reactive | — | REWRITE CORE | — | Yes | CORE#0.3 target threading list; PRACTICAL#1.0 `commonMain` and JS/Wasm atomics. The browser threading fact is already in the extracted dispatcher table. |
| `lesson_state_flow` | async_reactive | — | REWRITE CORE | — | Yes | SENIOR#2.5 "plain Kotlin Multiplatform type" → "plain kotlinx.coroutines type". |
| `lesson_state_holder_responsibility` | architecture | S `kmp_lifecycle_viewmodel` → REMOVE | REWRITE CORE | — | Yes | PRACTICAL#1.3 "a plain class in shared code" → "a plain class". The Lesson never teaches the KMP Subtopic it claims. |
| `lesson_viewmodel_lifetime_and_persistence` | architecture | S `kmp_lifecycle_viewmodel` → MOVE | **EXTRACT KMP SECTION** | PRACTICAL#1.3, #1.4 (Desktop, iOS, "Navigation 3 in common code"), #1.6 → `lesson_kmp_viewmodel_owners_across_hosts`. | Yes — a replacement Android owner list | The owner-lifetime ladder is core. The owners-per-host material is the heart of `kmp_lifecycle_viewmodel`. |
| `lesson_owner_scoped_work` | architecture | — | **EXTRACT KMP SECTION** | PRACTICAL#1.0–1.1 `commonMain`, Native/Swing fallback and `kotlinx-coroutines-swing` → `lesson_kmp_viewmodel_owners_across_hosts`. | Yes | `viewModelScope` ownership is core. The per-target dispatcher fallback is KMP. |
| `lesson_when_a_domain_layer_earns_its_place` | architecture | — | REWRITE CORE | — | Yes | SENIOR#2.1: delete "code shared across platforms in a multiplatform project is shared code rather than automatically a domain layer". |
| `lesson_policy_and_framework_detail` | architecture | S `kmp_architecture` → REMOVE | **EXTRACT KMP SECTION** | SENIOR#2.1 (sharing is a consequence of the policy/detail split) → one sentence merged into `lesson_one_graph_across_platforms` CORE. | Yes | The fragment is one paragraph, too small for its own KMP Lesson. The DI KMP Lesson already opens with the same rule. |
| `lesson_mvvm_observed_state` | architecture | — | REWRITE CORE | — | Yes | SENIOR#2.2: delete "and on a multiplatform target by whichever host supplies that holder". |
| `lesson_choosing_the_owner_by_lifetime` | architecture | — | REWRITE CORE | — | Yes | SENIOR#2.1 "true of some hosts … host by host" → "some owners … owner by owner", matching the rewritten ViewModel-lifetime Lesson. |
| `lesson_mvp_view_contract` | architecture | — | KEEP | — | No | "Differ between hosts" means Activity vs Fragment and the like; generic. |
| `lesson_when_an_interface_is_a_boundary` | architecture | — | KEEP | — | No | "Web Server" is inside a Clean Architecture quotation. |

## Proposed KMP Learning Structure

**Two KMP Units and four Lessons**, one fewer Unit than the candidate structure in the brief.
The candidate "KMP Architecture and Platform Boundaries" Unit is **not justified from
existing material**. The only extractable architecture fragment is one paragraph
(`lesson_policy_and_framework_detail` SENIOR#2.1), and `expect`/`actual`, `shared_domain`,
`shared_data` and `platform_implementations` are not taught anywhere in the learning
content today. Building that Unit would be new curriculum, not migration. Record it as
future work.

Both KMP Units go **after all Android Units** in `learning_curriculum.json`. Continue
Learning follows global authored order, so with KMP ON it reaches KMP material only after the
Android sequence.

### Unit K1 — `unit_kmp_shared_viewmodels_and_host_lifecycles` (new ID)

Title: **Shared ViewModels and Host Lifecycles** · home Topic `kmp`.

1. **`lesson_kmp_viewmodel_owners_across_hosts`** (new ID) — *Shared ViewModels,
   Host-Owned Lifetimes.* Primary `kmp_lifecycle_viewmodel`. Supporting
   `viewmodel_lifecycle`, `saved_state`, `coroutine_dispatchers`.
   - CORE: `lesson_viewmodel_lifetime_and_persistence` PRACTICAL#1.3, #1.4 and #1.6;
     `lesson_screen_state_owner_boundary` PRACTICAL#1.3.
   - PRACTICAL: the Navigation 3 common-code rule from #1.4; `lesson_owner_scoped_work`
     PRACTICAL#1.0–1.1 (`viewModelScope` Main fallback, `kotlinx-coroutines-swing`);
     `lesson_remember_saveable` CORE#0.7.
   - SENIOR: `lesson_dispatchers` SENIOR#2.2–2.4 (Main per target).
   - Covered by `kmp_shared_viewmodel_owner_platform`.
2. **`lesson_kmp_lifecycle_collection_across_hosts`** (new ID) — *Lifecycle-Aware
   Collection on Each Host.* Primary `compose_multiplatform`. Supporting
   `kmp_lifecycle_viewmodel`, `lifecycle_aware_apis`, `flow_sharing`.
   - Built from `lesson_lifecycle_aware_collection` PRACTICAL#1.0–1.3 and the host
     sentence of SENIOR#2.2.
   - Covered by the new split Question `kmp_desktop_window_lifecycle_collection_threshold`
     and by `kmp_compose_multiplatform_vs_jetpack`.

### Unit K2 — `unit_koin_and_dependency_injection_in_kmp` (existing ID, re-homed)

Title unchanged: **Koin and Dependency Injection in KMP** · home Topic `kmp`.

1. **`lesson_one_graph_across_platforms`** (existing ID, moved). Primary `koin_kmp`,
   replacing the deprecated `koin_multiplatform`. Existing supporting Subtopics kept. It
   absorbs the multi-host startup example from `lesson_the_koin_container_and_its_modules`
   and one sentence of the policy argument. Covered by `koin_multiplatform_common_module`
   and `koin_shared_and_platform_binding_split`.
2. **`lesson_choosing_di_for_a_shared_graph`** (new ID) — *Choosing a Strategy for a
   Shared Graph.* Primary `kmp_library_selection`. Supporting `koin_kmp`,
   `di_framework_tradeoffs`, `manual_di`, `hilt_vs_dagger`.
   - Built from `lesson_three_projects_three_answers` Project C and SENIOR#2.1–2.2,
     `lesson_what_a_container_actually_buys`' KMP capability facts, and
     `lesson_the_smallest_sufficient_strategy` SENIOR#2.3.
   - Covered by `di_strategy_smallest_sufficient_choice` and
     `kmp_android_library_not_multiplatform`, which tests the same "is it usable from
     `commonMain`" filter.

### The core Koin Unit — `unit_koin_containers_definitions_and_scopes` (new ID)

Title: **Koin: Containers, Definitions and Scopes** · home `dependency_injection`, in the
current Koin Unit's position.

- Lessons, keeping their stable IDs so study records survive:
  `lesson_the_koin_container_and_its_modules`, `lesson_koin_definitions_and_reuse`,
  `lesson_koin_scopes_and_their_owners`, `lesson_resolving_viewmodels_at_the_boundary`.
- The summary describes Koin on Android: definitions in Kotlin, a composition root in
  `Application`, scopes bound to owners.

### ID handling

Study records are keyed by `lessonId` only (`StudiedLesson`), so moving whole Lessons
between Units preserves them. Unit IDs appear in routes (`AppRoute.LearningUnit`,
`LearningLesson`, `PracticeBuilderLearningUnit`) and Continue Learning targets. The KMP Unit
therefore keeps the old ID, because it matches the title, and the core Unit takes the new
one. A stale route to the old ID then lands on KMP content and is guarded like any other
hidden route.

No IDs are created in production by this audit.

## Core Rewrite Plan

Each rewrite must leave a complete Android Lesson or Question. Claims marked **(verify)**
are the proposed replacement facts. Under the question- and learning-validation standards
they must be checked against primary sources before authoring.

### Questions

See the REWRITE CORE and SPLIT rows above. Summary of the exact edits:

- `kotlinx_serialization_001`: correct-answer text and one explanation sentence.
- `kotlin_no_checked_exceptions_interop`: the stem (Java only), correct-answer text,
  explanation, and one source. The catch-compile-error fact is **(verify)**.
- `compose_lifecycle_collection_stops_the_collector_not_the_producer`: stem, distractor B,
  explanation and sources, re-framed to Android Home/background. The multi-window/translucent
  `ON_PAUSE` contrast is **(verify)**.
- `koin_container_startup_composition_boundary`: stem, correct answer, explanation and
  source, re-framed to `Application.onCreate`.
- `clean_architecture_dependency_rule_tradeoff`, `dependency_direction_domain_framework_types`:
  one explanation clause each.

### Lessons with an extracted KMP section — what replaces it

| Lesson | Remove | Replace with (so the core Lesson stays complete) |
| --- | --- | --- |
| `lesson_remember_saveable` | CORE#0.7 callout | Nothing, or one sentence: Android specifies the saved-state guarantee (`SavedStateRegistry` through the Activity/entry owner). The ladder itself stays. |
| `lesson_screen_state_owner_boundary` | PRACTICAL#1.3; "Compose Multiplatform application whose shared" in #1.0; "multiplatform" in the summary | #1.0 → "Consider an application whose `NavDisplay` installs…" (Navigation 3 is an AndroidX library). #1.3 → one Android sentence: an Activity-scoped owner is retained across configuration recreation, while an entry-scoped owner ends when the entry is popped. |
| `lesson_lifecycle_aware_collection` | PRACTICAL#1.0–1.3; CMP hosts in SENIOR#2.2; "Given a target" in SENIOR#2.3; "window, scene or page" in CORE#0.0; the summary | A new PRACTICAL comparison of **Android** events → state → default-`STARTED` result: app to background or Home (`CREATED`, stops); multi-window focus lost or a translucent Activity on top (`STARTED`, keeps collecting); configuration change (owner recreated); navigation entry covered or popped (entry lifecycle) **(verify)**. Keep the portable lesson: "inspect the owner's mapping before assuming the API failed". SENIOR#2.2 keeps only the lifecycle-runtime-compose and Compose runtime versions. |
| `lesson_dispatchers` | SENIOR#2.2–2.4; "and Native" in CORE#0.3 | One Android paragraph: `Main` is the Android main-thread dispatcher from `kotlinx-coroutines-android`; `IO` and `Default` behave as the JVM section describes; injecting a dispatcher keeps the choice replaceable in tests. |
| `lesson_viewmodel_lifetime_and_persistence` | PRACTICAL#1.3 (`org.jetbrains` artifacts, `commonMain`), #1.4 bullets other than Android, #1.6 multi-target wording | #1.3 → "Which owner exists is a property of the host, not of the class." #1.4 → Android owners: Activity (retains its store through configuration recreation), Fragment, and navigation back-stack entry once the decorator is installed. #1.5 stays. #1.6 → "is a sentence about one owner repeated as a sentence about a class; name the owner, then answer". |
| `lesson_owner_scoped_work` | PRACTICAL#1.0 `commonMain`/`org.jetbrains` wording; #1.1 Native/Swing fallback; "on every target" in SENIOR#2.5 | The AndroidX `viewModelScope` facts: `SupervisorJob`, `Dispatchers.Main.immediate` on Android, closed when the ViewModel is cleared. A fallback sentence relevant to Android: in a plain JVM unit test with no Main dispatcher it falls back to an empty context unless the test installs `Dispatchers.setMain` **(verify against the lifecycle source)**. |
| `lesson_policy_and_framework_detail` | SENIOR#2.1; "desktop or iOS target" in PRACTICAL#1.3 | #1.3 → "this operation cannot be compiled in a plain Kotlin/JVM module, or tested without Android infrastructure". SENIOR#2.1 → drop it, or replace it with the reuse argument: framework-independent policy is what a feature module, a test, or a second screen can reuse. |
| `lesson_the_koin_container_and_its_modules` | CORE code with four platform modules; "shared and platform definitions" in CORE#0.2; PRACTICAL#1.0 four hosts; "source set" in #1.1 | An Android startup: `class App : Application() { onCreate { startKoin { androidContext(this); modules(dataModule, featureModules, databaseModule) } } }`. The Application is the composition root. #1.1 "source set" → "Gradle module". |
| `lesson_what_a_container_actually_buys` | "a graph that can be composed in common KMP code" in CORE#0.2; "usable from shared KMP graphs" and the Native/JS/Wasm target row in PRACTICAL#1.1; "KMP target support" in SENIOR#2.2 | The target row → Android facts only: Hilt requires an Android application; Dagger and Koin also work in plain JVM modules; manual DI works anywhere. SENIOR#2.2 → "platform support". |
| `lesson_three_projects_three_answers` | Project C (PRACTICAL#1.6–1.8); "'KMP, so Koin'" in #1.9; SENIOR#2.1–2.2; KMP in the summary | **Replacement Project C:** an Android app whose team already runs a working Koin graph. Definitions are spread across feature modules, and some are loaded or unloaded at runtime for optional features. There is no need for Hilt's predefined component hierarchy, and migration cost is high. Plausible choice: keep Koin with its compiler-safety checks. This keeps "three projects, three answers" (manual, Hilt, Koin) entirely inside Android. SENIOR#2.1–2.2 → "Construct a second version": an Android app whose Koin graph has six stable objects, where manual wiring is already trivial. |

### Lessons with incidental mentions (REWRITE CORE)

| Lesson | Exact edit |
| --- | --- |
| `lesson_stability_and_skipping` | PRACTICAL#1.0: "Compose Multiplatform 1.11.1" → the Compose runtime and compiler versions actually used. |
| `lesson_launched_effect` | SENIOR#2.0: replace the CMP caveat with the Android statement: the effect's context comes from the composition's applying context; on Android that is the window Recomposer's `AndroidUiDispatcher`, so effect code starts on the main thread unless it switches with `withContext` **(verify)**. |
| `lesson_flow_adapter_or_compose_producer` | CORE#0.3: delete the "common source set … not an Android-only mechanism" clause. |
| `lesson_with_context_and_main_safety` | PRACTICAL#1.0: "so the shared declaration does not assume Dispatchers.IO exists on every target" → "so tests can substitute it". SENIOR#2.4: delete the browser sentence, or replace it with "supplying a dispatcher does not make a blocking call non-blocking". |
| `lesson_sequential_and_concurrent_work` | SENIOR#2.3: "a single browser execution thread" → "the Android main thread, a single-threaded dispatcher". |
| `lesson_exception_propagation` | PRACTICAL#1.5 → JVM/Android: later failures arrive as suppressed exceptions on the first. SENIOR#2.2 → with no handler, the exception reaches the thread's uncaught-exception handler, which crashes an Android app **(verify)**. SENIOR#2.8 → drop "which JS and Native do not currently guarantee". |
| `lesson_shared_state_and_coordination` | CORE#0.3 → "On Android and the JVM, `Dispatchers.Default` runs coroutines on several threads at once". Remove the JS/Wasm sentence. PRACTICAL#1.0 → atomics: `kotlin.concurrent.atomics` (experimental opt-in) or `java.util.concurrent.atomic`; remove `commonMain` and JS/Wasm. |
| `lesson_state_flow` | SENIOR#2.5: "a plain Kotlin Multiplatform type" → "a plain kotlinx.coroutines type". |
| `lesson_state_holder_responsibility` | PRACTICAL#1.3: "a plain class in shared code" → "a plain class". Remove S `kmp_lifecycle_viewmodel`. |
| `lesson_when_a_domain_layer_earns_its_place` | SENIOR#2.1: delete the multiplatform clause. |
| `lesson_mvvm_observed_state` | SENIOR#2.2: delete "and on a multiplatform target by whichever host supplies that holder". |
| `lesson_choosing_the_owner_by_lifetime` | SENIOR#2.1: "hosts" → "owners". |
| `lesson_the_smallest_sufficient_strategy` | CORE#0.3: "does not make Hilt a commonMain graph for iOS" → "does not make Hilt fit a custom session owner". PRACTICAL#1.5: delete the "shared KMP hosts" row. SENIOR#2.3: delete the KMP-team sentence and "shared Native/JS capability". SENIOR#2.5: delete "Shared KMP graph required:". |
| `unit_observable_state_collection` (summary) | "as each Android, desktop, iOS or web host changes activity" → "as the screen's lifecycle owner changes state". |

The rewrites also require documentation updates. `docs/content/curriculum.md`,
`question-bank-coverage.md` and `learning-question-coverage.md` must reflect the moved
Questions and Units. The DI blueprint and Units 1–6 plan describe the Koin Unit as it is
today. The `InitialCurriculumSmokeTest` per-Topic counts (`"kmp" to 16`, the level
distribution) and the Unit list in `BundledLearningCurriculumTest` pin the current shape.

### Content-boundary invariants

Add these to `LearningCurriculumValidator` or as a bundled-content test, so the boundary
cannot erode:

1. A Lesson in a Unit with home Topic `kmp` has only `kmp` Subtopics as **primary**.
2. A Lesson in a non-`kmp` Unit references **no** `kmp` Subtopic, primary or supporting.
3. A non-`kmp` Lesson's `relatedLessonIds` contain no Lesson of a `kmp` Unit.
4. A bundled-content test scans the learner-visible text of non-`kmp` Questions and Lessons
   for `Multiplatform|KMP|commonMain|Kotlin/Native|expect/actual|iosMain`, with an explicit
   allowlist, so new KMP asides fail the build.

Invariant 1 also keeps Mistake Review's Lesson mapping stable when the setting changes. That
mapping requires exactly one primary Lesson per Subtopic over visible Units, and with these
invariants no core Subtopic ever gains or loses a primary Lesson when KMP visibility flips.

## Runtime Visibility Surfaces

### Where filtering should live

Keep one definition of "visible curriculum", applied **below** every selection and
derivation, never in composables:

1. **`CurriculumVisibility`** (new, `curriculum/visibility/`): a value holding
   `hiddenTopicIds`, with `isTopicVisible(topicId)`. `CurriculumVisibility.from(includeKmp)`
   is the only place that knows `"kmp"`, as a named constant.
2. **`VisibleCurriculumRepository : CurriculumRepository`**, a decorator bound in
   `curriculumDataModule` around `LocalCurriculumRepository`.
   - Filters every `getActive*` read by `topicId`.
   - Passes the identity and historical resolvers (`getTopicById`, `getSubtopicById`,
     `getQuestionsByIds`) through unchanged, which keeps the repository's existing split
     between eligibility and history.
   - Implement the interface explicitly, not with `by`, as the interface's own KDoc warns.
3. **`VisibleLearningContentRepository : LearningContentRepository`**, a decorator filtering
   `getActiveUnits` and `getActiveUnitsByTopic` by `unit.topicId`. `getUnitById` and
   `getLessonById` pass through.
4. **A visible history projection**: an app-scoped `CompletedAssessmentHistory` that
   combines `AssessmentHistoryStore.history` with the visibility state. For each attempt it
   drops `questionAttempts` whose Question resolves to a hidden Topic, through one batched
   `getQuestionsByIds`. It drops attempts left empty and recomputes `score` from per-question
   correctness. Every history consumer reads this instead of the raw store.
5. **Route guards**: identity reads that open a destination check `isTopicVisible` and
   return the screen's existing `NotFound`.

Because hiding happens at these seams, selection (`AssessmentQuestionSelector`), coverage
(`LearningProgressService`), performance and weak areas (`LearningPerformanceDerivation`),
mistakes (`UnresolvedMistakeDerivation`), Continue Studying and recommendations need **no
KMP-specific code**. They see a smaller curriculum and a projected history.

### Surface-by-surface

| Surface | Current read path | How it respects visibility | Special treatment |
| --- | --- | --- | --- |
| Topic Browser list | `getActiveTopics`, `getActiveSubtopics` | Decorator | Must **re-read on visibility change**. The ViewModel reads the catalogue once in `init` and outlives a Settings round-trip. It also needs sectioning (see [Main-page structure](#main-page-structure)). |
| Topic Browser search | Filters the loaded catalogue in memory | Inherits the decorator | None beyond the re-read. |
| Learning availability badges | `getActiveUnitsByTopic` | Learning decorator | None. |
| Continue Studying card | `ContinueStudyingResolver` over history | Projected history | A KMP-scoped focused attempt projects to empty and the resolver falls through to the next attempt, which it already does. |
| Recommended Next | `LearningRecommendationResolver` + progress | Projected history + filtered coverage | None. Unseen-coverage Topics come from filtered ACTIVE Questions. |
| Continue Learning | `ContinueLearningPolicy` over `getActiveUnits` | Learning decorator | KMP Units are skipped. "Complete" means Android-complete when OFF. |
| Topic Detail | `getActiveTopics().firstOrNull`, `getActiveQuestionsByTopic`, `getActiveUnitsByTopic` | Decorator: hidden Topic → existing `TopicCurriculum.NotFound` | Stale route handled automatically. |
| Learning Unit / Lesson screens | `getUnitById` (identity) | **Guard:** a hidden `unit.topicId` → existing `NotFound` | `relatedLessonIds` rendering must drop hidden Lessons (defensive, given invariant 3). |
| Practice Builder (Topic / Subtopic / Unit) | `PracticeTargetResolver`: `getTopicById`, `getSubtopicById`, `getUnitById` | **Guard:** hidden target → unavailable | Question counts come from filtered reads. |
| Focused practice (start / retake) | `AssessmentQuestionSelector` → `getActive*ByTopic/Subtopic(AndLevels)` | Decorator | A retake of a hidden-scope attempt yields `NoEligibleQuestions` today. Prefer an explicit guard on the route. |
| WEAK_AREAS / UNRESOLVED_MISTAKES sources | Selector's `CompletedAssessmentHistory` + `LearningPerformanceDerivation` | Inject the projected `CompletedAssessmentHistory` | Ranking must not see KMP weak areas. |
| Mixed interview | `selectMixedQuestions` → `getActiveQuestions` (round-robin by Topic) | Decorator | With KMP ON, `kmp` joins the round-robin as one more Topic. |
| In-progress attempt / attempt routes | `AssessmentSessionLoader`, `AssessmentReviewLoader` → `getQuestionsByIds` (historical) | **Guard** on focused attempts whose scope is hidden; mixed review drops hidden items | Routes carry `attemptId`, so a restored back stack can point at a hidden attempt. |
| Interview history / results | `InterviewHistoryStateHolder`, `MixedInterviewResultViewModel` | Projected history | See decision D-2. |
| Saved Questions | `SavedQuestionContentResolver` → `getQuestionsByIds` | Filter resolved items by `isTopicVisible` | Rows stay stored. The saved count and entry badge use the filtered list. |
| Mistake Review + nav badge | `MistakeReviewService`, `AppShellViewModel` over `historyStore.history` | Projected history | Remove the services' default `assessmentRepository.getCompletedAttempts()` fallback, or project it too. Otherwise a caller can bypass the projection. |
| Mistake → Lesson link | `withStudyLessons(getActiveUnits())` | Learning decorator + invariant 1 | None. |
| Progress dashboard / Topic progress | `ProgressStateHolder`, `ProgressTopicViewModel`, `LearningProgressService` | Projected history + filtered ACTIVE denominator | **Guard** `ProgressTopic` for a hidden Topic. Overall accuracy currently sums `score.totalQuestions`/`correctAnswers`, so it needs the recomputed projected score. |
| Study progress (Unit / Topic) | `StudyProgressService` over Units passed in | Callers pass decorator output | Studied KMP Lessons stay in `lesson_study` and simply fall out of visible summaries. |
| Back stack after toggling OFF | `AppNavigator` | Prune entries whose route targets hidden content, or return to the top-level root | Needed because Settings is a pushed route above live destinations. |

## Main-page structure

The Topic Browser should become **explicitly sectioned in the ViewModel/UI model**, not
partitioned inside the composable. Deciding which Topic belongs to which section is
curriculum knowledge, and composables should only render what they receive.

- Replace `TopicBrowserUiState.Content.topics` with `sections: List<TopicBrowserSection>`.
  A section is `TopicBrowserSection(kind: CurriculumSection, topics:
  List<TopicBrowserItemUiModel>)`, with `CurriculumSection { AndroidEngineering,
  KotlinMultiplatform }`.
- `CurriculumVisibility.sectionOf(topicId)` supplies the section, so the one `"kmp"`
  constant stays in one place.
- `toContent` groups visible Topics by section in a fixed order (Android Engineering first)
  and **omits empty sections**. When KMP is OFF the decorator has already removed `kmp`, so
  the Kotlin Multiplatform section disappears without any `includeKmp` check in the
  ViewModel or the UI.
- `catalogueSection` renders one heading plus rows per section, and replaces the single
  "Topics" heading with the section name. Two new string resources are needed.
- Search stays flat (`topicMatches`, `subtopicMatches`) over visible content.
- The guidance pane is unaffected.

## Preference Design

Follow the appearance-preference pattern. A second preference does not justify a settings
framework, and the brief does not need one.

- **`KmpContentPreferenceStore(storage: AppPreferenceStorage)`** in `settings/`.
  - Key `content.include_kmp`, tokens `"on"` / `"off"`.
  - An absent or unrecognised value reads as **OFF**, which gives the fresh-install default.
  - Unlike the theme store, write **both** tokens explicitly. A future change to the product
    default must not flip learners who deliberately chose OFF. The theme's "absence means
    System" rule has no equivalent here.
- **`CurriculumVisibilityStateHolder`**, a Koin `single`. It owns
  `includeKmpContent: StateFlow<Boolean>` and `visibility: StateFlow<CurriculumVisibility>`,
  plus `setIncludeKmpContent(Boolean)`. Like `AppearanceStateHolder`, it reads synchronously
  in the constructor, so the first frame never flashes KMP content. It writes synchronously
  and updates state first.
- **Ownership: application lifetime.** The holder is consumed by both repository decorators,
  the history projection, the Topic Browser and the navigator. It must outlive every
  destination, as the appearance holder does.
- **Module:** add both bindings in a shared module (for example a
  `curriculumVisibilityModule`). Each host already binds `AppPreferenceStorage`, so there is
  no new platform code.
- **Settings:** `SettingsDestination` injects the holder. `SettingsScreen` gains a second
  switch row, "Include Kotlin Multiplatform content", with one line of supporting text. No
  ViewModel, for the same reason the appearance switch has none.
- **Change propagation:** on a change, the holder emits and the projection re-derives
  because it `combine`s the visibility state. The holder also triggers
  `AssessmentHistoryStore.invalidate()` and `StudyProgressStateHolder.refresh()`, so every
  history- and study-derived surface recomputes. Screen ViewModels that cached a catalogue
  (Topic Browser, Topic Detail) observe `visibility` and reload. *(Superseded in part: the
  raw history is not invalidated, and the study refresh is requested by the two reloading
  ViewModels rather than the holder — see [Step 6](#step-6-settings-switch-sections-and-live-propagation).)*

## History and Metrics Semantics

### Development migration

The developer database needs **no reset and no Room migration**:

- The curriculum importer upserts Topics, Subtopics and Questions on every launch, deferring
  foreign keys so re-homing works. Moving a Question's `topicId` is simply re-imported.
- Attempts persist only `questionId` per answer. Performance derivation re-resolves each
  Question's **current** Topic, so past answers to moved Questions are attributed to `kmp`
  automatically.
- Study records key on `lessonId`. Whole-Lesson moves keep them. New split Lessons start
  unstudied, which is acceptable on a disposable database.
- Old focused attempts scoped to the deprecated `koin_multiplatform` Subtopic resolve as
  deprecated, so Continue Studying skips them.

A reset remains allowed if an implementation step finds it simpler, but none is needed.

### Final toggle behaviour

- **Turning OFF hides; it never deletes.** No write touches `test_attempt`, question-attempt,
  saved-question or lesson-study tables. Only the preference key changes.
- **Turning ON restores.** Projections recompute from unchanged raw records. Mistake
  resolution is derived from the full per-question answer sequence, so KMP mistakes come
  back in exactly the state they would have had.
- **Aggregates follow the visible curriculum.** With KMP OFF, hidden records do not
  participate in:
  - coverage percentages (ACTIVE denominator filtered);
  - accuracy, weak areas, recent performance and recommendations (projected attempts);
  - question totals and badges;
  - Continue Studying, Continue Learning, and the mistake and saved counts.

Services needing special treatment:

- **`LearningProgressService`**: overall accuracy sums `score.totalQuestions` and
  `score.correctAnswers`, which are attempt-level. The projection must **recompute the
  score** of any attempt it trims.
- **`LearningPerformanceDerivation`**: resolves history through the historical resolver,
  which is deliberately unfiltered. It must receive projected attempts. Do not filter the
  resolver itself, because review and saved screens rely on it.
- **`MistakeReviewService` / `LearningProgressService`**: both fall back to
  `assessmentRepository.getCompletedAttempts()` when called without attempts. That path
  must go through the projection or be removed.
- **`AssessmentQuestionSelector`**: takes `CompletedAssessmentHistory`. Bind the projected
  implementation.

### Open decisions (recommendations given)

- **D-1: Mixed interview with KMP ON.** Treat `kmp` as one more Topic in the round-robin.
  *Recommended:* yes, with no weighting change.
- **D-2: Historical mixed attempts that included KMP, viewed with KMP OFF.**
  *Recommended:* one rule everywhere — every surface reads projected history. The interview
  list shows the visible-question score, and the attempt review omits hidden items with a
  short "N Kotlin Multiplatform questions hidden" note. The case exists only for learners
  who turned KMP ON, took mixed interviews, then turned it OFF.
- **D-3: Topic Browser headings when only one section is visible.** *Recommended:* still
  show "Android Engineering", so the section label does not change meaning with a setting.

## Migration Order

Content first: the setting must never ship while KMP material still leaks into core
content. The runtime work is independent of the content work, but it is only meaningful
after it.

1. **Question-bank migration** (PR 1).
   - Move 5 Questions, deprecate `koin_multiplatform`, rewrite 4 core Questions, split 2
     with 2 new KMP Questions.
   - Update `InitialCurriculumSmokeTest` counts, `question-bank-coverage.md` and
     `curriculum.md`. Record each Question in the audit log per the validation standard.
2. **Learning migration, DI part** (PR 2).
   - Create `unit_koin_containers_definitions_and_scopes`, re-home four Lessons, rewrite the
     container Lesson, re-home the KMP Unit, add `lesson_choosing_di_for_a_shared_graph`,
     rewrite the three strategy Lessons.
3. **Learning migration, Compose / Coroutines / Architecture part** (PR 3).
   - Create Unit K1 with its two Lessons, apply the extractions and incidental rewrites, fix
     the mappings, update `learning-question-coverage.md`.
4. **Content-boundary invariants and leak test** (PR 3 or 4). Added once the content passes
   them.
5. **Visibility core** (PR 4): `CurriculumVisibility`, the preference store and holder, both
   repository decorators, the projected history, and service fallbacks routed through it.
   Domain tests included; the Settings switch is not exposed yet.
6. **Topic Browser sectioning + Settings switch + change propagation** (PR 5).
7. **Route guards and back-stack pruning**, plus saved, review and mistake filtering (PR 6).
8. **Architecture documentation.** Document the visibility boundary in
   `docs/architecture/overview.md` (next to the appearance preference) and cross-reference
   it from `progress.md` and `recommendations.md`.

This differs from the brief's sequence in two ways. The brief's "downstream filtering" and
"metrics projection" steps collapse into step 5, because the decorators and the projection
*are* the downstream filtering. Tests ship with each PR instead of at the end.

### Implementation status

| Step | State |
| --- | --- |
| 1. Question-bank migration | Done (#438). |
| 2. Learning migration, DI part | Done (#439). |
| 3. Learning migration, Compose / Coroutines / Architecture part | Done (#440) — see below. |
| 4. Content-boundary invariants and leak test | Done — see [Step 4](#step-4-where-the-invariants-live). |
| 5. Visibility core | Done — see [Step 5](#step-5-visibility-core). |
| 6. Topic Browser sectioning + Settings switch + change propagation | Done — see [Step 6](#step-6-settings-switch-sections-and-live-propagation). |
| 7. Route guards and back-stack pruning, plus saved, review and mistake filtering | Done — see [Step 7](#step-7-route-guards-back-stack-pruning-and-result-projection). |
| 8. Architecture documentation | Not started. |

Step 3 shipped Unit K1 as planned, with its two Lessons. The curriculum now has **32 Units
and 138 Lessons**; the two `kmp` Units (K1, then K2) hold 4 Lessons and follow every core
Unit. The proposed third KMP Unit was not created. Every extraction and incidental rewrite in
the Learning Migration Table was applied, plus one incidental mention the table missed:
`lesson_state_holder_responsibility` listed `UIViewController` among host objects, and now
lists `Fragment`. `lesson_lifecycle_aware_collection` gained supporting `activity_lifecycle`,
because its replacement section teaches Activity lifecycle transitions.

Claims marked **(verify)** that primary sources did not support as proposed:

- **Multi-window focus loss.** The proposed row "multi-window focus lost → `STARTED`" holds
  only on Android 9 and lower. Android 10 and higher keep every visible multi-window Activity
  `RESUMED` (multi-resume) and report focus through `onTopResumedActivityChanged`. The Lesson
  states both. Collection continues either way, so the core Question's answer is unaffected.
- **`viewModelScope` fallback in a JVM unit test.** Not stated. The Android testing
  documentation says local unit tests replace `Main` with `Dispatchers.setMain`, so the
  Lesson states that instead of claiming what the lifecycle source falls back to on Android.
- **Navigation-entry lifecycle.** Stated from the Navigation 3 destination-lifecycle
  documentation: an entry is capped at `STARTED` mid-transition or under an overlay, and at
  `CREATED` once popped and animating out.
- **Desktop `ViewModelStoreOwner`.** The source Lesson said Compose Multiplatform supplies "a
  common `ViewModelStoreOwner` implementation" on desktop. The JetBrains ViewModel page does
  not say so, and the lifecycle source resolves it through a host-default key. The KMP
  Lesson narrows this to "whatever the desktop host provides, or one the application or a
  navigation library installs".
- Supported as proposed: `LaunchedEffect` runs on `AndroidUiDispatcher.CurrentThread` from
  the window Recomposer; the uncaught-exception path goes through `ServiceLoader` handlers
  and then the thread's handler, and crashes an Android app; suppressed exceptions (the
  current coroutines guide no longer carries a JDK 7 caveat); `AtomicInt` is experimental and
  represented on the JVM by `AtomicInteger`.

### Step 4: where the invariants live

All four [content-boundary invariants](#content-boundary-invariants) are bundled-content
tests in `shared/src/jvmTest`, not `LearningCurriculumValidator` rules: "Topic `kmp` is
optional content" is this product's authoring policy, not a property of any learning
curriculum, and an authoring-time guarantee should cost nothing at runtime. Every rule
derives its KMP Units, Lessons and Subtopics from Topic ownership, through one test constant
`KMP_TOPIC_ID`, so a future KMP Unit is covered without being named. Each failure lists every
violation with its Unit, Lesson, Question and block, so an author can fix them in one pass.

| Invariant | Test |
| --- | --- |
| 1. KMP-Unit Lessons have only `kmp` primary Subtopics | `BundledLearningCurriculumTest.kmpUnitLessonsPractiseOnlyKmpOwnedSubtopics` |
| 2. Core Lessons map no `kmp` Subtopic, primary or supporting | `BundledLearningCurriculumTest.coreLessonsMapNoKmpSubtopics` |
| 3. Core Lessons link to no Lesson of a `kmp` Unit | `BundledLearningCurriculumTest.coreLessonsLinkToNoKmpLesson` |
| 4. Lexical leak scan of core Questions and Lessons | `KmpContentLeakTest`, scanner `KmpVocabulary` in `KmpContentBoundary.kt`, scanner tests `KmpVocabularyTest` (all in `curriculum/content`) |

- **Consolidated.** The generic rules replace the two narrow tests the migrations left
  behind: the DI-only test and the test over the 19 Lessons the lifecycle Unit was extracted
  from, plus the per-Unit primary-ownership assertions. The DI test also excluded the
  deprecated DI Subtopic `koin_multiplatform`, which Topic ownership cannot see; the new
  `lessonsMapNoDeprecatedSubtopics` keeps that coverage generically.
- **Vocabulary**, case-insensitive: `Multiplatform` (so also "Kotlin Multiplatform" and
  "Compose Multiplatform"), `KMP` as a word, `commonMain`, `iosMain`, `Kotlin/Native` (spaces
  around the slash allowed), and `expect/actual` (also `expect / actual` and
  `` `expect`/`actual` ``). Host, platform, shared, source set and iOS are deliberately not in
  it.
- **Learner-visible text scanned:** every non-`kmp` Question's text, answer options and
  explanation, deprecated Questions included because attempt history still shows them; every
  non-`kmp` Unit's title and summary, and its Lessons' titles, summaries, section titles and
  every block kind (paragraph, bullet list, code, comparison, callout). Sources — titles and
  URLs — and IDs are not scanned; the AndroidX `.../commonMain/...` source paths this audit
  identified are therefore not leaks, and `KmpVocabularyTest` pins that.
- **No allowlist.** The first run found no learner-visible leak and no structural violation,
  so no content was changed; a future hit is fixed in the content.
- **Questions reuse the generic validator.** `CurriculumValidator` reports
  `SUBTOPIC_TOPIC_MISMATCH` for any Question whose Subtopic belongs to another Topic, and
  `InitialCurriculumSmokeTest` asserts the bundled curriculum validates cleanly. That is why
  Question visibility can use `question.topicId` alone; no KMP-specific Question rule exists.

### Step 5: visibility core

The runtime boundary now exists and every domain derivation reads through it. The Settings switch
is not exposed yet, so with the production default the app runs with Kotlin Multiplatform content
hidden and no control to show it until Step 6.

| Piece | Name and location (`shared/src/commonMain/.../kmp_learning_app/`) |
| --- | --- |
| Visibility value | `CurriculumVisibility` in `curriculum/visibility/`. `hiddenTopicIds`, `isTopicVisible(topicId)`, and `from(includeKmpContent)`, the only production code that names `"kmp"`. |
| Preference store | `KmpContentPreferenceStore` in `settings/`, over the existing `AppPreferenceStorage`. Key `content.include_kmp`; writes `"on"` and `"off"` explicitly; absent or unrecognised reads as OFF. |
| State holder | `CurriculumVisibilityStateHolder` in `curriculum/visibility/`. `includeKmpContent` and `visibility` StateFlows, `setIncludeKmpContent`; reads synchronously in the constructor and writes synchronously on every call. |
| Curriculum decorator | `VisibleCurriculumRepository` in `curriculum/visibility/`. Filters every `getActive*` read by the Question's or Subtopic's Topic, short-circuits a hidden Topic's Subtopic read, and passes `getTopicById`, `getSubtopicById` and `getQuestionsByIds` through. Explicit overrides, no `by`. |
| Learning decorator | `VisibleLearningContentRepository` in `curriculum/visibility/`. Filters `getActiveUnits` and `getActiveUnitsByTopic` by `unit.topicId`, keeping authored order; `getUnitById` and `getLessonById` pass through. |
| History projection | `VisibleAssessmentHistory` in `assessment/history/`, downstream of the unchanged `AssessmentHistoryStore`. The rule itself is `VisibleHistoryProjection.visibleAttempts`, which both the observable `history` and the one-shot `completedAttempts()` call. |
| DI | `curriculumVisibilityModule` in `curriculum/visibility/`, installed by all four hosts next to `appearanceModule`. |
| Service fallbacks | `LearningProgressService` and `MistakeReviewService` now take `CompletedAssessmentHistory` instead of `AssessmentRepository`, which neither uses any more. The application binds both to the projection. |

**DI shape.** `curriculumDataModule` binds `LocalCurriculumRepository` and `learningContentModule`
binds `BundledLearningContentRepository` by their concrete types only. `curriculumVisibilityModule`
binds the `CurriculumRepository` and `LearningContentRepository` interfaces to the decorators
wrapping them, plus the store, the holder and `VisibleAssessmentHistory`. This differs from the
plan above, which bound the decorator inside `curriculumDataModule`. Binding the interfaces in one
place means a graph without the visibility module fails to resolve them, instead of silently
handing out unfiltered eligibility. It also keeps the data module free of the preference.
`AssessmentQuestionSelector` and `LearningProgressService` receive `VisibleAssessmentHistory` in
`assessmentDataModule`; `MistakeReviewService` receives it in `topicStudyPresentationModule`.
`CurriculumImporter` is unchanged.

**History readers moved to the projection:** `ProgressStateHolder`, `MistakeReviewStateHolder`,
`InterviewHistoryStateHolder`, `AppShellViewModel`, and two readers the plan did not list,
`TopicBrowserViewModel` and `TopicDetailViewModel` (learning context, Recommended Next and Continue
Studying). The last two also retry through the projection. `VisibleAssessmentHistory.invalidate()`
only invalidates the raw store, so neither class can reach the raw `history`. `ProgressViewModel`,
`MistakeReviewViewModel` and `CompleteAssessment` still hold the raw store, only to invalidate it.

**Projection semantics**, decided where the plan was silent:

- An attempt with nothing hidden is returned as the same instance. An attempt with nothing
  visible is dropped. A trimmed attempt keeps its ID, configuration, status, timestamps and
  answer order. Its score is recomputed from persisted `Answered.isCorrect`, never from the
  current answer key.
- Classification uses the Question's *current* Topic, resolved in one batched
  `getQuestionsByIds` for the whole history. Nothing is resolved while nothing is hidden.
- **A Question ID that no longer resolves is kept.** Visibility hides content *known* to belong
  to a hidden Topic; missing metadata is not that evidence, and dropping it would turn a
  curriculum gap into lost history.
- If Question metadata cannot be read, the observable projection publishes
  `AssessmentHistory.Failed`. Publishing the unprojected attempts could show hidden content, and
  keeping an older projection could belong to another visibility. Invalidating the raw store is
  the retry. The one-shot read throws.
- The observable projection is `combine(raw history, visibility)` shared with `replay = 1`, not a
  `StateFlow`. It re-emits on a visibility change and on every settled raw refresh, including an
  unchanged one, so the store's "a settled refresh is an event" contract survives.

**Change propagation.** A visibility change re-projects history without invalidating the raw
store, so every history-derived app-scoped surface updates on its own. The plan's other
propagation steps are Step 6: refreshing `StudyProgressStateHolder` and re-reading catalogues
cached by the Topic Browser and Topic Detail ViewModels. Fresh reads already see filtered Units
and Questions.

**Tests.**

- `commonTest`: `KmpContentPreferenceTest` (store and holder), `VisibleCurriculumRepositoryTest`
  (every ACTIVE method family, both states, identity pass-through, order and instance identity),
  `VisibleHistoryProjectionTest` (core, KMP-only, mixed, ordering, persisted correctness,
  batching, unresolved IDs, re-homed Questions) and `VisibilityDerivationTest`. The last covers
  progress, both service fallbacks, selection by every source, D-1 round-robin and a no-write
  round trip.
- `jvmTest`: `VisibleAssessmentHistoryTest` (reactivity to both inputs, unchanged-refresh
  re-announcement, failures) and `VisibleLearningContentRepositoryTest` over the shipped document.
  Also `CurriculumVisibilityIntegrationTest` (production graph over the bundled curriculum:
  default-hidden mixed selection, D-1, and a round trip that updates Progress and the interview
  record while attempt, saved and study records stay equal), a badge test in
  `AppShellViewModelTest`, and a shared-file test in `JvmAppPreferenceStorageTest`.
- `DesktopLocalDataPathTest` now pins the production default: 16 of 17 Topics visible, `kmp`
  still resolvable by ID.
- Existing integration graphs install `curriculumVisibilityModule` with an in-memory preference
  set to OFF. The exceptions are `LearningUnitPracticeIntegrationTest` and
  `LearningProductionContentJourneyTest`, which pin every shipped Unit, KMP ones included, and
  run with it ON.

### Step 6: Settings switch, sections and live propagation

The learner can now change the preference, and the screens that cache ACTIVE reads follow it
live. Learning Unit, Lesson, Practice Builder, Topic progress and result screens are unchanged;
their hidden-content guards are Step 7.

| Piece | Name and location (`shared/src/commonMain/.../kmp_learning_app/`) |
| --- | --- |
| Section classification | `CurriculumSection { AndroidEngineering, KotlinMultiplatform }` in `curriculum/visibility/CurriculumSection.kt`; `CurriculumVisibility.sectionOf(topicId)` in the same companion as `from`, reusing its private `"kmp"` constant. Declaration order is presentation order. |
| Sectioned UI state | `TopicBrowserUiState.Content.sections: List<TopicBrowserSection>` replaces `topics`. `TopicBrowserSection(kind, topics)` and the grouping function `List<TopicBrowserItemUiModel>.toBrowserSections()` live in `topic_study/topics/TopicBrowserUiState.kt`: section order, repository order inside a section, empty sections omitted. Search (`topicMatches`, `subtopicMatches`) stays flat. |
| Headings | `catalogueSection` renders one `SectionHeading` per section: `topic_browser_section_android_engineering` ("Android Engineering"), `topic_browser_section_kotlin_multiplatform` ("Kotlin Multiplatform"). Decision D-3 adopted: the Android heading shows even when it is the only section. "Topics" / "Subtopics" remain search-result headings only. |
| Settings switch | `SettingsDestination` resolves `AppearanceStateHolder` and `CurriculumVisibilityStateHolder` (no ViewModel). `SettingsScreen` has three sections — Appearance, Learning content, About. The new row is "Include Kotlin Multiplatform content" / "Show Kotlin Multiplatform topics, lessons, and practice questions.", tag `SettingsKmpContentSwitchTag`, and calls `setIncludeKmpContent` directly. Both switches share a private `SettingsSwitchRow` (whole-row `toggleable(role = Role.Switch)`, `Switch(onCheckedChange = null)`). |
| Topic Browser observation | `TopicBrowserViewModel` takes `visibilityStateHolder`. `observeVisibility()` collects `visibility`; a value different from `catalogVisibility` (recorded by every `loadCatalog()`) calls `StudyProgressStateHolder.refresh()` and `loadCatalog()`. |
| Topic Detail observation | `TopicDetailViewModel` takes `visibilityStateHolder`; the same shape against `loadVisibility` and `loadTopic()`. A hidden Topic becomes the existing `TopicDetailUiState.NotFound` and returns to `Content` when shown again. |

**Reloading.** Both screens re-read through the visible repositories rather than filtering
their cached lists, so Topic rows, searchable Subtopics, Unit counts, the authored Unit
sequence behind Continue Learning, and Topic Detail's Questions and Units all come from the
new visibility together. The reload is the existing load path, so the existing
`catalogGeneration` / `loadGeneration` contract discards a slower read made under the old
visibility. The query is ViewModel state outside the catalogue, so it survives the reload
and is matched against the newly visible catalogue.

**No duplicate startup read.** A `StateFlow` hands a new collector its current value. The
ViewModels compare each emission with the visibility their newest load was requested under,
instead of `drop(1)`, so the replayed value is ignored without risking a missed change that
lands before the collector starts.

**History is not invalidated.** `VisibleAssessmentHistory` already combines raw history
with visibility and re-emits on a change, so Progress, the mistake badge and Mistake Review,
interview history, and the history-derived Topic Browser and Topic Detail enrichment update
from that emission. The database has not changed, so Step 6 does **not** call
`AssessmentHistoryStore.invalidate()`; the existing calls for assessment completion and
retry are unchanged. Only cached ACTIVE curriculum and learning reads needed an explicit
reload.

**Guidance from the old visibility is withheld.** The catalogue reload and the history
re-projection finish in no fixed order. Hiding KMP makes the projection read Question
metadata, so the Android-only catalogue can land first while Recommended Next, Continue
Studying and the row learning context still describe KMP-visible history — for example, a
Continue Studying shortcut into `kmp`. `VisibleAssessmentHistory.snapshots` therefore
publishes each projection with the visibility it was made under (`VisibleHistorySnapshot`;
`history` is the same flow with the visibility dropped, so the projection still runs once).
`TopicBrowserViewModel` records that visibility with its enrichment and renders the
enrichment only while it equals the catalogue's visibility, treating a mismatch as history
not yet arrived. The fields are not cleared when the reload starts, because a projection for
the new visibility may already have been derived by then. Topic Detail needs no gate: its
enrichment is scoped to its own Topic, which is either unaffected or `NotFound`.

**Study state.** Each reload asks the shared `StudyProgressStateHolder` to `refresh()`. It
reads the full persisted record; nothing is filtered, cleared or unmarked. The visible Units
decide what participates in a derivation, so a KMP Lesson studied while shown is still
studied after a hide-and-show round trip. Concurrent refreshes from both screens are
serialised by the holder's read mutex.

**Deviations from the plan.**

- The plan had the holder invalidate raw history. Step 5's combined projection made that
  redundant, and doing it would re-read the attempt table for no change.
- The plan had the holder refresh study state. The two reloading ViewModels request it
  instead, so `curriculum/visibility/` does not depend on the Learn presentation layer.
- A visibility reload reuses the load path, so it passes through `Loading` briefly. The
  screen is beneath Settings when that happens, so the learner does not see it.

**Tests (all `jvmTest`).**

- `TopicBrowserVisibilityTest`: section order and repository order, no empty KMP section,
  OFF → ON and ON → OFF on one ViewModel, query preservation, a single startup read,
  Continue Learning over the visible sequence, a studied KMP Lesson surviving a round
  trip, no raw history read on a change, the old-visibility race, and guidance from the old
  projection withheld while the new one is still resolving.
- `TopicBrowserVisibilityIntegrationTest`: the production graph over the bundled
  curriculum, one live ViewModel — KMP section, search and Unit count appear and disappear,
  and Continue Learning reaches KMP only after every core Lesson.
- `TopicDetailVisibilityTest`: a single startup load, a core Topic re-read on each change,
  a KMP Topic going `NotFound` and back, no raw history read, and the race.
- `TopicBrowserSectionsScreenTest`: headings, heading semantics, order, D-3, clickable rows,
  flat search groups, guidance above the catalogue, and a long KMP name on a compact width.
- `SettingsScreenTest`: the Learning content section and row, switch semantics and touch
  target, both states and both directions, independent callbacks, and the three-section
  guard against placeholder sections.
- `SettingsNavigationIntegrationTest`: the destination over in-memory storage (OFF by
  default, `"on"` / `"off"` persisted, the theme switch independent), and the live
  Learn → Settings → Back round trip. That test types a KMP-only query before opening
  Settings. The query is still in the field on return, which a new Topic Browser could not
  show, and its KMP result appears (then disappears after the second round trip).
- Existing tests take the sectioned state through the test helper `browsingContent(...)`
  and read rows through `allTopics` (`TopicBrowserTestStates.kt`).

### Step 7: route guards, back-stack pruning and result projection

No user-visible path shows Kotlin Multiplatform content while the setting is off. Two layers do
this. **Destination guards** are the correctness boundary: every destination that resolves identity
or historical content refuses known-hidden content itself, so a restored, direct or not-yet-pruned
route never shows it, even for a frame. **Back-stack pruning** is navigation cleanup on top, so Back
and area switching do not lead into content that is now hidden. Visibility stays Topic-based
throughout (`CurriculumVisibility.isTopicVisible`); no route, attempt or saved row carries a KMP flag,
and nothing outside `CurriculumVisibility` names `"kmp"`.

| Piece | Name and location (`shared/src/commonMain/.../kmp_learning_app/`) |
| --- | --- |
| Route classification | `AppRouteVisibilityResolver` and `enum AppRouteVisibility { Visible, KnownHidden, Unknown }` in `AppRouteVisibility.kt`, bound as a `single` in `topicStudyPresentationModule`. `classify(route, visibility)` reads nothing while nothing is hidden, and a failed read is `Unknown`. |
| Pruning pass | `pruneRoutesHiddenBy(visibility, navigator, resolver)` in the same file: `collectLatest` over `CurriculumVisibilityStateHolder.visibility`, classifying `navigator.detailRoutes()` and passing the `KnownHidden` set to `navigator.pruneFrom`. Started from `AppShell` in `App.kt` by a `LaunchedEffect(navigator)`. |
| Navigator operations | `AppNavigator.detailRoutes()` (every area's non-root entries) and `AppNavigator.pruneFrom(invalid)`. Structural only: no repository or visibility reaches the navigator. |
| Unit / Lesson guards | `LearningUnitViewModel`, `LearningLessonViewModel`: an ACTIVE Unit whose `topicId` is hidden is the existing `NotFound`. The Lesson is decided by its owning Unit. |
| Practice target guard | `PracticeTargetResolver` takes `visibility: StateFlow<CurriculumVisibility>`. Topic, Subtopic (parent Topic) and Unit (home Topic, checked before deriving concepts) targets that resolve hidden are `Unavailable`. `PracticeBuilderViewModel` observes the holder and re-resolves the original target. |
| Progress Topic guard | New `ProgressTopicUiState.Unavailable`, rendered as the existing "Topic unavailable" (`progress_topic_unavailable`). `Empty` keeps meaning a visible Topic with no observations. |
| Saved Questions | `SavedQuestionContentResolver.resolve(savedQuestions, visibility)` omits resolved hidden Questions. `SavedQuestionsViewModel` keys resolved content on the saved list and the visibility. |
| Results | `FocusedResultViewModel` and `MixedInterviewResultViewModel` project their one stored attempt through `VisibleHistoryProjection.visibleAttempts`. New `Unavailable` states and `Content.hiddenQuestionCount`. |
| Hidden-question notice | `HiddenReviewQuestionsNotice` in `assessment_review/AssessmentReviewComponents.kt`, placed by `AssessmentResultOutcome` directly under the completion hero. Plural `assessment_review_hidden_questions`. |
| In-progress attempts | `AssessmentSessionLoader` takes `visibility` and returns the new `AssessmentSessionLoadResult.ContentUnavailable`. `AssessmentTakingViewModel` maps it to the new `AssessmentTakingUiState.Unavailable`. |

**Route classification.** Ownership comes from current content, never from how an ID is spelled.

| Routes | Rule |
| --- | --- |
| `Topics`, `Interview`, `Progress`, `MistakeReview`, `SavedQuestions`, `Settings` | Always `Visible`. |
| `Topic`, `ProgressTopic`, `PracticeBuilderTopic` | The route's Topic ID. |
| `PracticeBuilderSubtopic` | The resolved Subtopic's `topicId`. |
| `LearningUnit`, `LearningLesson`, `PracticeBuilderLearningUnit` | The resolved Unit's `topicId`. A Lesson route uses the Unit it carries. |
| `FocusedPracticeResult`, `MixedInterviewResult` | `KnownHidden` only when `VisibleHistoryProjection` leaves nothing of the completed attempt. A partial projection stays, and an unresolved Question keeps a result visible, as in history. |
| `FocusedPracticeAttempt`, `MixedInterviewAttempt` | `AssessmentSessionLoader.load(attemptId, visibility)`: `ContentUnavailable` is `KnownHidden`. An attempt route whose attempt has completed is judged by the result rule, because it hands over to its result. |

A missing ID, a failed read, an unresolved Question, or a result route whose attempt is not yet
completed is `Unknown`. It stays on the stack and reaches its destination's existing
NotFound/Error handling. Only `KnownHidden` is pruned.

**Pruning semantics.**

- Every area's stack is validated, not only the current one. A KMP Progress drill-down parked while
  the learner changes Settings from Learn is gone when they return to Progress.
- Each stack is cut from its **first** `KnownHidden` entry through its top. Entries above it were
  reached through it, so their path no longer exists. Roots always stay, and nothing is
  reconstructed.
- **Settings preservation.** If the top of a pruned stack is `Settings`, it stays open, rebased
  directly on the root. The learner is not thrown out of Settings by the switch they just changed,
  and Back returns to the Learn root. The navigator applies this to any stack whose top is
  `Settings`, not only the current one. Settings only ever sits on the Learn stack.
- The pass runs for the initial (possibly restored) stacks under the current visibility and again on
  every change. `collectLatest` cancels a pass still classifying, so a result computed under OFF
  cannot prune a stack after visibility has changed again.
- Turning KMP on does not recreate pruned routes.
- Pruning is navigation state only. It never cancels or deletes an attempt, unsaves a Question,
  unmarks a Lesson, or rewrites a configuration or result.

**Destination guards and live observation.** Every guarded ViewModel observes
`CurriculumVisibilityStateHolder.visibility` while alive: the Unit, the Lesson, the Practice Builder,
Progress Topic, Saved Questions, both results and Assessment Taking. Each compares an emission with
the visibility its newest load was requested under, the Step 6 pattern, so the StateFlow's replayed
value never triggers a second startup read. Each reload goes through the screen's existing
load path and job cancellation, so a read made under the old visibility cannot land last. A
destination that is still alive and has not been pruned reloads when content is shown again.

**Saved Questions.** `SavedQuestionStateHolder` still holds every raw saved identity. Nothing is
filtered, deleted or unsaved, and the result and Mistake cards keep using it to show whether the
Question on screen is saved. A resolved Question of a hidden Topic is omitted. An unresolved ID
stays `Missing`, because its Topic cannot be classified. A DEPRECATED Question of a visible Topic
stays, since status plays no part. A visibility change re-resolves the same saved list with no
saved-table read. Content resolved under another visibility gives way to `Loading` rather than
staying on screen, because it could hold now-hidden Questions. If every saved Question is hidden,
the screen is `Empty`. The count on screen counts visible items only.

**Result projection.** Both result ViewModels load the raw attempt, validate completion as before,
and then project it. The projected score comes from persisted `Answered.isCorrect`. The review
lists visible Questions in stored order. The Mixed Topic breakdown is counted from the visible
review items, so a hidden Topic has no row. The attempt ID, timestamps and configuration are
unchanged. Nothing visible means `Unavailable`, not `AttemptNotFound`. `AssessmentReviewLoader` is
still historical and unfiltered: each caller hands it what is currently visible. Retake is
unchanged and selects through the visible repositories, so it picks only eligible Questions. An
unavailable result offers no retake.

**D-2 notice.** When `hiddenQuestionCount > 0`, the result summary shows "N Kotlin Multiplatform
question(s) is/are hidden by your learning-content setting." directly under the score, before the
unresolved-content and mistake notes. It uses `onSurfaceVariant`, as a neutral note rather than a
warning. The result is valid and simply projected. The Focused result uses the same notice for a
historical attempt split across the boundary.

**In-progress attempts are never partially filtered.** The loader checks visibility after its one
batched Question read. If any Question that resolved belongs to a hidden Topic, the result is
`ContentUnavailable`. That check runs before the missing-Question walk, so an attempt holding both
a hidden and a missing Question is unavailable. An ID that does not resolve at all is still
`MissingQuestion`. The stored attempt is untouched and resumes unchanged once visible.
`AssessmentTakingViewModel` withdraws a session already on screen as soon as one of its Questions
becomes hidden. It judges from the Questions it already holds, without a read. It ignores the result
of an answer or completion write that was in flight when the session was withdrawn. A core session
is not reloaded, so an unsubmitted selection survives. `Unavailable` has no Retry.

**Surfaces that needed no code, because Step 5 covered them.** Mistake Review and its badge derive
from `VisibleAssessmentHistory`, and the study link maps through ACTIVE visible Units. The new
regression test confirms that a KMP mistake and its KMP study link disappear while hidden and
return when shown. No filter was added to `MistakeReviewService`, `UnresolvedMistakeDerivation`,
`MistakeReviewStateHolder` or `AssessmentReviewLoader`. Interview history, Progress, Continue
Studying and recommendations were already projected. Topic Detail already used the visible
repositories.

**Deviations from the brief.**

- Topic-owned routes (`Topic`, `ProgressTopic`, `PracticeBuilderTopic`) and `ProgressTopicViewModel`
  check the route's Topic ID directly instead of resolving the Topic first. The ID *is* the Topic's
  identity, so a lookup could only add a failure mode (`Unknown`) without changing any answer.
- `PracticeTargetResolver` and `AssessmentSessionLoader` read visibility from an injected
  `StateFlow`, as the repository decorators do. `SavedQuestionContentResolver` takes it as a
  parameter instead, because its caller keys cached content on it. `AssessmentSessionLoader` also
  exposes `load(attemptId, visibility)`, so route classification applies the destination's exact
  rule under the visibility being validated.
- `practice_builder_target_unavailable` used to read "This learning unit is no longer available for
  practice.", which was wrong for a hidden Topic or Subtopic target. It now reads "This content is
  not available for practice."
- Assessment Taking also observes visibility live, which the brief did not require. An in-progress
  attempt route can be parked in the Mistakes stack while Settings changes, and the guard should not
  depend on pruning.
- The hidden-question plural names Kotlin Multiplatform because `kmp` is the only Topic visibility
  can hide. The count itself is Topic-agnostic.

**Tests (all `jvmTest`).**

- `AppRouteVisibilityTest`: classification of every route kind, both states. `Unknown` for missing
  owners and failed reads. Result projection and in-progress rules, including unresolved Questions.
  No reads while nothing is hidden. Pruning: the Learn flow back to `Topics`, a parked Progress
  stack, builder routes, a KMP-only result removed while a partial Mixed result stays, `Unknown` kept,
  Settings rebased, no recreation on ON, a superseded pass pruning nothing, and no attempt writes.
- `AppNavigatorTest`: `pruneFrom` cuts from the first invalid entry, reaches every area, never
  removes a root, and rebases Settings. `detailRoutes` excludes roots.
- `AppNavigatorRestorationTest`: a stack saved with KMP routes in three areas, restored and
  validated under OFF. Roots and visible routes stay, and hidden detail routes go.
- `DestinationVisibilityGuardTest`: Unit and Lesson ON → Content, OFF → NotFound, and back on one
  ViewModel. A single startup read. Practice targets by Topic, Subtopic and Unit, with a missing
  label kept. A live builder going `TargetUnavailable` and back. Progress Topic `Unavailable`, not
  `Empty`.
- `ResultVisibilityTest`: the D-2 example (1/1 hidden, 1/2 shown), order and persisted
  correctness, a breakdown with visible Topics only, Mixed and Focused `Unavailable` with no retake,
  a partial historical Focused result, a retake selecting visible Questions only, a single startup
  read, and the stored attempt unchanged.
- `SavedAndSessionVisibilityTest`: resolver omission versus `Missing` versus DEPRECATED. The OFF →
  ON → OFF round trip with no saved-table read or mutation. All-hidden → `Empty`. The pending
  removal of a visible item. Loader rules (core, KMP-only, mixed never shortened, `MissingQuestion`
  kept). Taking `Unavailable` without Retry, resuming when shown, and a live session withdrawn and
  restored. A core session unaffected.
- `CurriculumVisibilityIntegrationTest.hiddenKmpContentLeavesEveryIdentityResolvedSurfaceAndReturnsUnchanged`:
  the production graph over the bundled curriculum. A saved KMP Question, a Mixed result with a KMP
  mistake, a studied KMP Lesson, and the Mistake Review queue and its KMP study link all follow OFF →
  ON → OFF on live ViewModels, while attempt, saved and study records stay equal.
- Screen tests: the hidden-question notice (singular, plural, absent), and each new `Unavailable`
  state in the Mixed and Focused results, Assessment Taking and Progress Topic, without Retry.

## Validation Plan

Tests the eventual implementation must add (domain tests in `commonTest`; screen and
integration tests in `jvmTest`, following the existing `TopicBrowserViewModelTest`,
`TopicDiscoveryIntegrationTest` and `AppearancePreferenceTest` patterns):

**Preference**

- A fresh install (empty `InMemoryAppPreferenceStorage`) reads **OFF**.
- `setIncludeKmpContent(true)` persists `"on"`; a new holder over the same storage reads ON
  (persists across restart).
- An unrecognised stored token reads OFF.
- `JvmAppPreferenceStorage` round-trips the new key.

**Curriculum visibility**

- OFF: `getActiveTopics` excludes `kmp`; every `getActive*Question*` variant excludes
  `kmp` Questions.
- OFF: `getActiveUnits` and `getActiveUnitsByTopic` exclude KMP-home Units.
- ON: all of the above are restored.
- OFF: identity and historical reads (`getTopicById("kmp")`, `getQuestionsByIds`,
  `getUnitById`) still resolve.

**Selection**

- OFF: focused practice on any visible scope never includes a `kmp` Question.
- OFF: mixed-interview selection over the bundled curriculum contains no `kmp` Question.
  With ON, `kmp` participates.
- OFF: WEAK_AREAS and UNRESOLVED_MISTAKES sources never rank or select KMP evidence.

**History projection and metrics**

- A toggle round-trip leaves attempt, saved and study tables byte-identical (no deletes).
- A mixed attempt with KMP and Android answers: OFF → the score is recomputed over Android
  answers only; ON → the original.
- OFF: a KMP-only focused attempt affects no accuracy, coverage, weak area, recent
  performance, recommendation or Continue Studying result. ON restores its influence.
- OFF: the coverage denominator equals the Android ACTIVE Question count.
- `LearningProgressService` / `MistakeReviewService` called without attempts still respect
  the projection.

**Surfaces**

- Topic Browser OFF: no Kotlin Multiplatform section. ON: the section appears after Android
  Engineering. It toggles without recreating the ViewModel.
- Search OFF: no KMP Topic, Subtopic (e.g. "SQLDelight", "Koin in KMP") or Unit result.
- Saved Questions and Mistake Review OFF: KMP items are absent and counts exclude them. ON:
  they return with prior state.
- Stale or direct navigation OFF to a KMP Topic, Topic progress, Unit, Lesson, Practice
  Builder target, focused attempt or retake: each fails safely to its existing
  `NotFound`/unavailable state without crashing.
- Continue Learning OFF never targets a KMP Lesson.

**Content boundary**

- The invariants above: KMP-home Lessons have only `kmp` primaries; core Lessons reference
  no `kmp` Subtopic or KMP Lesson.
- The leak scan passes over core learner-visible text.
- The updated `InitialCurriculumSmokeTest` counts: `kmp` = 23 ACTIVE.
