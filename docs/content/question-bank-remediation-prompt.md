# Question Bank Remediation — Reusable Prompt

This file is a **prompt**, not documentation. Paste the section below (from
`BEGIN PROMPT` to `END PROMPT`) into a new session to carry out remediation of
the Android interview question bank, in whole or in phases.

It is self-contained: it carries the findings of the 2026-08-30 audit inline, so
no prior session context or scratchpad file is needed.

**How to scope a run.** The prompt defaults to one phase at a time. Replace the
`SCOPE` block with the phases you want, e.g. `PHASE 1` only, or `PHASE 1-3`, or
`ALL PHASES`. Phases are ordered by value per unit of effort and are independent
unless noted.

---

BEGIN PROMPT

======================================================================
TASK
====

Remediate defects in the Android interview question bank identified by the
full-bank audit of 2026-08-30 (270 questions, commit 571f748).

Repository:

```
ArtemiyTkachenko/KMP-Learning-App
```

Primary curriculum file:

```
shared/src/commonMain/composeResources/files/curriculum/initial_curriculum.json
```

This is an IMPLEMENTATION task. The audit is already done. Do not re-audit the
bank. Act on the findings recorded below.

======================================================================
SCOPE
=====

```
PHASE 1
```

Run only the phase(s) named above. If a phase is not listed, do not begin it,
even if you notice its defects while working.

======================================================================
READ FIRST
==========

Before editing anything, read:

```
docs/content/content-authoring.md            (the editorial contract — it wins on conflict)
docs/content/question-authoring-playbook.md  (the method, and the anti-cue audit script)
```

The contract governs. Where this prompt and the contract disagree, follow the
contract and say so.

======================================================================
GROUND RULES
============

1. CORRECT ANSWERS ARE NOT IN SCOPE.

Every one of the 270 answer keys was independently verified correct. No fix in
this prompt requires changing a correct answer's claim. If you believe a key is
wrong, STOP and report it rather than editing it.

2. IDENTITY SEMANTICS.

```
Question.id       kept for wording, clarity, distractor, explanation, and
                  source changes. A new id + deprecation of the old question is
                  only for a changed concept or changed correct answer.

AnswerOption.id   KEPT when only the wording changes and the claim is the same.
                  NEW when the claim changes. Replacing a distractor with a
                  different misconception is a NEW claim, even in the same slot.
```

Historical `QuestionAttempt` rows store selected answer IDs. Reusing an ID for a
different assertion silently corrupts past attempts.

3. RETIRED ANSWER IDS ARE A DATA MIGRATION, NOT AN EDIT.

When a change retires answer IDs, verify that the import marks them
`DEPRECATED`, that active curriculum queries exclude them, and that
`getQuestionById` still returns them so past attempts stay reviewable. A prior
change got this wrong and retired options reappeared as extra choices for
upgrading users. Check this explicitly whenever answer IDs are retired at scale.

4. NEVER TRUST A REMEMBERED URL.

Every URL you write must be fetched and confirmed before it goes in the file.
Confirm it returns 200, that the destination is the intended document, and that
the page actually supports the claim. Use a default user agent — a browser user
agent triggers an auto-sign-in redirect on developer.android.com that produces
false failures:

```bash
curl -s -o /dev/null -w '%{http_code} %{url_effective}\n' -L --max-time 30 "$URL"
```

5. DO NOT COMMIT, PUSH, OR OPEN A PR WITHOUT EXPLICIT APPROVAL.

Make the edits, run the checks, report what changed, and stop with the work in
the working tree. Approving a plan is not approving a commit.

6. STAY INSIDE THE CURRICULUM FILE.

Unless a phase says otherwise, the only file you change is
`initial_curriculum.json`. Do not modify the Question model, Room schema,
migrations, `selectionMode`, `AssessmentEngine`, or the UI. If a phase forces a
count change that a pinned test asserts, update that test deliberately and say
so.

======================================================================
PHASE 1 — P1: REPOINT TRANSFERRED OKHTTP / RETROFIT SOURCE URLS
===============================================================

Ten questions cite `github.com/square/okhttp` and `github.com/square/retrofit`.
Those repositories have been transferred out of the `square` organisation and
now 301-redirect to `github.com/lysine-dev/...`. Verified 2026-08-30:

```
curl -I https://github.com/square/okhttp
  -> 301 Location: https://github.com/lysine-dev/okhttp

api.github.com/repos/lysine-dev/okhttp
  -> full_name lysine-dev/okhttp, 47k stars, pushed_at 2026-08-29, fork: false
```

The links resolve today only because GitHub preserves transfer redirects. They
misrepresent the project's home and break if `square/okhttp` is ever recreated.
The `master` branch segment also redirects to `main`.

Affected questions:

```
network_timeouts_001
okhttp_interceptors_001
retrofit_coroutines_001
okhttp_authenticator_token_refresh
http_cache_control_revalidation
network_error_transport_vs_http
retrofit_vs_okhttp_responsibilities
okhttp_application_vs_network_interceptor
connect_read_write_timeout_distinction
coroutine_http_call_cancellation
```

A rendered documentation site is live again — this was NOT true when the
playbook was written, which recorded `square.github.io/okhttp/` as a 404 (still
404 today). Verified 200 on 2026-08-30:

```
https://lysine.dev/okhttp/
https://lysine.dev/okhttp/features/interceptors/
https://lysine.dev/okhttp/features/caching/
https://lysine.dev/okhttp/recipes/
https://lysine.dev/retrofit/
```

Prefer these. Fall back to `github.com/lysine-dev/<repo>/blob/main/...` only
where the doc site has no matching page.

RE-VERIFY EVERY URL AT THE TIME YOU RUN THIS. This project's ownership has
already moved once; do not assume the above still holds.

Two of these questions also have a weak-source finding to fix in the same pass:

```
retrofit_coroutines_001         cites the Retrofit CHANGELOG — evidence the
                                feature exists, not an explanation. Add a
                                conceptual source.

network_error_transport_vs_http cites the repository landing page; cite the
                                Response / HttpException documentation instead.
```

======================================================================
PHASE 2 — P2: BREAK THE MULTI-ANSWER POSITION CUE
=================================================

Of the 32 questions marked "Select all that apply", the first listed option is a
correct answer in 31. Ticking option A on every multi-answer question is right
about A 97% of the time with no Android knowledge.

Position combination frequencies as authored:

```
A,B    17
A,C     7
A,B,D   4
A,B,C   2
A,D     1
B,D     1
```

The playbook's audit script pools every correct answer's position across all 270
questions, so this shows as a healthy 30/32/21/17% spread and is never
surfaced.

WORK:

1. Reorder the `answers` arrays of multi-answer questions until option A is a
   correct answer in roughly half of them, and the remaining combinations are
   spread across positions.

Reordering is free and safe: answer identity is by `AnswerOption.id`, never by
list position. Do NOT rename any ID to match a new order.

2. Extend the playbook's audit script with a multi-answer position check so this
   cannot silently return, and update `docs/content/question-authoring-playbook.md`
   Part 3 to include it. Suggested check:

```python
multi = [q for q in qs if len(q['correctAnswerIds']) > 1]
first = sum(1 for q in multi
            if q['answers'][0]['id'] in q['correctAnswerIds'])
print(f'multi-select: option A correct in {first}/{len(multi)}')
combos = Counter(tuple(sorted('ABCD'[[a['id'] for a in q['answers']].index(c)]
                              for c in q['correctAnswerIds'])) for q in multi)
print('combos:', combos.most_common())
```

Target: option A correct in roughly half of multi-answer questions, no single
combination dominating.

======================================================================
PHASE 3 — P2: REBUILD THE WORST DISTRACTOR SETS
===============================================

The distractor failure is concentrated in the newest authoring cohort (commit
b087173): 33 of 43 distractor findings, against 6 and 4 in the two earlier
cohorts. The pattern is not the off-topic filler a previous review removed — it
is IMPOSSIBLE PLATFORM BEHAVIOUR, which is dismissible on sight and turns a
four-option question into a two-option question.

Every replacement distractor MUST:

```
be a belief a competent Android developer could actually hold;
name only types, functions, and permissions that exist — check each one;
answer the same KIND of question as the other options (category parity);
match the other options in grammatical form, register, and specificity;
be defensibly WRONG under the stem as written.
```

Issue a NEW `AnswerOption.id` for every replaced distractor. The claim changes,
so the ID must.

Update each question's explanation to disarm its new strongest distractor. An
explanation that still argues against a removed option is a defect.

QUESTIONS AND THE OPTIONS TO REPLACE:

```
room_flow_invalidation_requery
  bad: "Flow polls every entity table at a fixed interval selected by
        Dispatchers.IO"; "Each entity keeps a SharedFlow field that Room
        serializes into the database row"; "SQLite pushes complete Kotlin
        objects directly into the collector through a Binder callback"
  use: query re-runs only when the queried table changes; the Flow emits per
       changed row; observation requires the query to sit in a @Transaction

savedstatehandle_process_recreation
  bad: "deserialized from the dead process's heap snapshot"; "every
        ViewModelStore is persisted in the system process"; "the Activity field
        is copied directly into the new process"
  use: restoration through SavedStateRegistry from the onSaveInstanceState
       Bundle; restoration from the Navigation back-stack entry's arguments;
       re-fetch by the repository

compose_semantics_merged_tree
  bad: "The SQLite query plan, because merged semantics are loaded from the
        test database"; "The Gradle dependency graph"
  use: printToLog() of the merged tree; useUnmergedTree = false with
       onNodeWithContentDescription; adding a testTag to the parent

noinline_vs_crossinline_lambda
  bad: "Mark both lambdas reified"; "Mark both lambdas tailrec"
       (reified is a type-parameter modifier, tailrec a function modifier;
        neither is legal on a lambda parameter)
  use: mark the stored lambda noinline and leave the other unmodified; mark
       both noinline; make the whole function non-inline

apk_embedded_api_secret_recoverable
  bad: "Android automatically uploads every string constant to Play Console";
       "The Keystore exports APK constants to other apps"; "TLS sends all
        compiled constants with each request"
  use: R8 obfuscation renames the constant so it cannot be recovered; putting
       it in NDK/C++ code puts it out of reach; splitting the string across
       resources and reassembling at runtime is sufficient

realtime_messages_persist_then_render
  bad: "a SharedFlow ... because replay persists values across app processes";
       "notification extras, because the system reconstructs full conversation
        history from them"; "saved instance state, which supports an unbounded
        message history"
  use: an in-memory MutableStateFlow in a singleton repository; a Room table
       keyed by a client-generated id (wrong for deduplication); writing on the
       socket callback thread without a transaction

parent_cancellation_propagates_children
  bad: "children detach and continue"; "only the oldest child is cancelled";
       "their dispatchers switch to Dispatchers.Default automatically"
  use: cancellation is immediate/preemptive; a cancelled parent still runs
       suspending finally blocks (needs NonCancellable); cancel() waits for
       children (it does not — cancelAndJoin() does)

composition_vs_recomposition
  bad: "recomposition recreates the Activity"; "initial composition runs only
        previews"; "initial composition saves state to disk"
  use: recomposition vs remeasure/redraw phases; recomposition skipped for
       unchanged inputs; composition running off the main thread;
       recomposition running in any order or being cancelled and restarted

idempotent_worker_retry_design
  bad: "Idempotency makes the Worker exempt from Doze"; "...serialize the entire
        server response into its input Data"
  use: Result.retry() only re-runs work that never reached the network;
       WorkManager deduplicates identical requests by tag; a unique work name
       already guarantees at-most-once execution

retrofit_vs_okhttp_responsibilities
  bad: "Retrofit persists response entities; OkHttp maps database rows into
        endpoint parameters"; "Retrofit schedules WorkManager retries; OkHttp
        recreates Activities after configuration changes"
  use: Retrofit owning the cache (OkHttp's Cache does); OkHttp parsing JSON (a
       converter factory's job); Retrofit owning connection pooling

modularization_large_app_module_cost
  bad: "Every internal type becomes a stable Play Store API"; "Gradle disables
        incremental compilation because application modules cannot compile
        incrementally"; "Android prevents teams from adding unit tests"
  use: real positions engineers argue for — see the note below

over_modularization_tiny_module_cost
  bad: "Android library modules cannot contain resources"; "Gradle merges all
        tiny modules before configuration"; "module boundaries remove dependency
        direction"
  use: real positions engineers argue for — see the note below

viewmodel_vs_repository_responsibility
  bad: "The repository owns the Activity; the ViewModel stores Context for all
        data-source operations" and the other role swaps
  use: repository exposing Room entities straight to the UI; ViewModel owning
       the OkHttp cache policy; ViewModel holding a Context for resource lookup

sequence_intermediate_allocation_tradeoff
  bad: "allocate one operating-system thread for each intermediate operation";
       "require JVM reflection to discover every element's runtime generic
        type"; "eagerly copy the full list before each intermediate operator"
  use: sequences short-circuit so terminal-free chains do no work; asSequence()
       on an already-materialised list still copies; sequences are
       single-pass / constrained-once

jank_vs_anr_failure_scale
  bad: "A missed deadline always terminates the process"; "Jank is caused only
        by network threads; ANRs only by GPU rendering on RenderThread";
       "identical labels selected randomly by the device's current refresh rate"
  use: an ANR fires at a fixed timeout for input dispatch regardless of frame
       timing; jank on the RenderThread cannot cause an ANR; a frozen frame
       (>700 ms) is reported as an ANR by Play vitals
```

For the two modularization questions, draw distractors from positions engineers
genuinely argue for:

```
"one module per layer scales better because technical concerns are shared"
"a :core:common module avoids duplication"
"modules should mirror the package structure"
"build speed always improves with more modules"   (widely held, and wrong)
```

NOTE ON A LINKED CUE. Wherever distractors are impossible, the correct answer
becomes by construction the only balanced, qualified sentence in the set — the
old length cue wearing different clothes. Fixing the distractors fixes the cue.
Do not touch the keys.

======================================================================
PHASE 4 — P2: DIFFERENTIATE DUPLICATE CLUSTERS
==============================================

39 clusters were found. The recurring mechanism is not shared wording: question
A's EXPLANATION states question B's correct answer verbatim, and explanations
render in the review screen, so seeing one hands over the other.

Work the ten tightest first. For each, keep the stronger question and re-point
the other at a concept the bank does not cover (see PHASE 8 for the gap list) —
re-pointing is more valuable than deletion.

Re-pointing a question at a different concept means a NEW `Question.id` and
deprecation of the old one. Weigh that cost; where the concept is close enough,
prefer sharpening the stem and options under the existing ID.

```
1  modularization_dependency_direction  /  feature_siblings_shared_contract_module
   Closest pair in the bank: same subtopic, near-identical stem, identical
   answer, same first distractor.
   Re-point one at: breaking a :feature:a <-> :feature:b cycle, or how a feature
   exposes navigation without exposing its screens (api + impl modules).

2  repository_pattern_001 / repository_vs_data_source_responsibility /
   viewmodel_vs_repository_responsibility
   Three-way; all resolve to "the repository coordinates sources behind an
   app-facing contract".
   Keep viewmodel_vs_repository_responsibility. Re-point the others at
   offline-first refresh policy, or whether a repository exposes Flow or suspend.

3  shared_vs_platform_code_001 / kmp_shared_layer_selection /
   kmp_tradeoff_when_not_to_share / kmp_shared_ui_platform_experience_tradeoff
   Four-way; all resolve to "share what is platform-independent".
   Keep two. Re-point the rest at Swift/Objective-C interop, the Kotlin/Native
   memory model, or XCFramework / CocoaPods integration.

4  dagger_module_binding_declarations  /  dagger_component_graph_root
   Mirror-image stems; each explanation states the other's answer in full.
   Re-point one at @Binds vs @Provides, @InstallIn, or component dependencies
   vs subcomponents (currently uncovered).

5  dagger_graph_assembly_generated_component / dagger_compile_time_graph_validation
   / di_framework_tradeoff_compile_vs_runtime
   All three answer "at compile time" — picking whichever option says "compile
   time" answers all three without Dagger knowledge.
   Keep the validation question. Re-point the others at @Component.Factory /
   @BindsInstance, or kapt vs KSP build cost.

6  coroutine_cancellation_001  /  cpu_loop_cooperative_cancellation
   A's explanation names B's key verbatim ("ensureActive(), yield(), isActive").
   Re-point one at runInterruptible for blocking JVM calls, withTimeout vs
   withTimeoutOrNull, or NonCancellable cleanup.

7  coroutine_exceptions_001  /  coroutine_scope_vs_supervisor_scope_failure
   A's explanation says supervisorScope is the isolation mechanism, which is B's
   whole answer. coroutine_supervisor_scope_direct_children covers this too and
   is the strongest of the three — keep it.
   Re-point B at: a SupervisorJob passed as a child context to launch has no
   supervising effect (a common real bug).

8  memory_leaks_001  /  gc_does_not_prevent_reachable_leaks
   Same subtopic, same mechanism, same answer.
   Keep gc_does_not_prevent_reachable_leaks. Re-point the other at an
   inner-class Handler with a delayed message, an unregistered listener, or a
   view binding held past onDestroyView.

9  coroutine_fundamentals_001 / coroutine_vs_thread_suspension
   and coroutine_builders_001 / launch_vs_async_unawaited_result
   Re-point at runBlocking on the main thread, Thread.sleep in a coroutine,
   async exception timing, or CoroutineStart.LAZY.

10 dependency_configurations_001 / gradle_api_vs_implementation_leak /
   gradle_implementation_compile_avoidance
   A's explanation teaches both of the others.
   Keep the two concrete ones. Re-point A at compileOnly / runtimeOnly / ksp.
```

Remaining clusters, lower priority, same treatment:

```
sharedflow_001 / stateflow_vs_sharedflow_current_value
compose_stability_001 / compose_skipping_stable_parameter_contract   (see PHASE 5)
compose_recomposition_001 / compose_state_read_recomposition_scope
compose_lazy_layouts_001 / compose_key_identity_lazy_state
compose_state_001 / compose_vs_view_ui_model
kotlin_sequences_001 / sequence_intermediate_allocation_tradeoff
kotlin_generics_001 / generic_in_out_variance_tradeoff
fragment_lifecycle_001 / fragment_view_lifecycle_collection
saved_state_001 / saved_state_transient_inputs
process_death_001 / configuration_change_vs_process_recreation
fcm_data_messages_001 / fcm_app_state_delivery_behavior
notification_channels_001 / notification_channel_user_control
coroutine_testing_001 / coroutine_virtual_time_delay_skipping
room_flow_001 / room_flow_invalidation_requery
offline_design_001 / system_design_source_of_truth_boundary  (+ offline_first_001)
architecture_tradeoffs_001 / domain_layer_passthrough_cost
dependency_direction_001 / architecture_interface_boundary_ownership
constructor_injection_001 / di_constructor_injection_testability
foreground_services_001 / workmanager_vs_foreground_service_constraint
compose_ui_testing_001 / compose_accessibility_001        (cross-topic)
anr_001 / android_main_thread_001                          (cross-topic)
```

======================================================================
PHASE 5 — P2: SOURCES THAT DO NOT SUPPORT THEIR CLAIM
=====================================================

All 173 unique URLs in the bank return HTTP 200 — nothing is broken. These ten
are official documentation that never discusses the thing being tested, which
the contract classifies as INVALID for the question.

```
architecture_solid_dependency_substitution
  cites /topic/architecture — the page never mentions SOLID
  fix: cite the principles' own definition (Robert C. Martin, "Design Principles
       and Design Patterns"), or drop the SOLID framing and ask the same
       question in Android terms

composition_root_001
  cites /topic/architecture — never mentions a composition root
  fix: /training/dependency-injection/manual (its "application container"
       section is exactly this concept)

system_design_feature_flag_rollout
  cites /topic/architecture — no feature-flag or rollout content
  fix: Firebase Remote Config docs, and/or Play Console staged-rollout docs

system_design_observability_signals
  cites /topic/architecture — no logging / crash / metrics content
  fix: Firebase Crashlytics documentation

system_design_api_backward_compatibility
  cites /topic/architecture/data-layer/offline-first — no API/field versioning
  fix: an API-versioning primary source (e.g. the Google API design guide)

performance_r8_minify_reflection
  cites /topic/performance — no R8, keep rules, or reflection content
  fix: /build/shrink-code#keep-code — NOTE /build/shrink-code itself now
       redirects to /topic/performance/app-optimization/enable-app-optimization,
       so confirm which page carries the keep-rule content before citing

performance_strictmode_role
  cites /topic/performance — StrictMode is not described there
  fix: https://developer.android.com/reference/android/os/StrictMode

binder_ipc_marshalling_boundary
  cites source.android.com/docs/core/architecture/hidl/binder-ipc — the HIDL
  vendor-HAL chapter; HIDL is deprecated in favour of stable AIDL since
  Android 13. Wrong context for an app-level question.
  fix: https://developer.android.com/reference/android/os/Binder or
       /develop/background-work/services/aidl

flaky_test_shared_state_and_time
contract_focused_tests_resist_refactor
  both cite /training/testing/fundamentals, which does not establish their
  claims. Either find a source that does, or reword the questions to claims the
  cited page supports.
```

ALSO IN THIS PHASE — the generic landing-page cluster. The playbook's own rule
("a landing page is not enough when a precise page exists") is violated
systematically: `developer.android.com/topic/architecture` is cited by 12
questions and `/training/testing/fundamentals` by 6.

```
separation_of_concerns_001, unidirectional_data_flow_001,
single_source_of_truth_001, state_ownership_001, dependency_direction_001,
architecture_tradeoffs_001, architecture_interface_boundary_ownership
  -> /topic/architecture/ui-layer, /ui-layer/state-holders, /domain-layer,
     /data-layer, /recommendations

test_doubles_001        -> /training/testing/fundamentals/test-doubles     (200 verified)
testing_strategy_001    -> /training/testing/fundamentals/what-to-test     (200 verified)
local_vs_instrumented_test_placement
                        -> /training/testing/local-tests +
                           /training/testing/instrumented-tests            (200 verified)
viewmodel_testing_001   -> ViewModel / state-holder testing guidance
android_manifest_component_exported
                        -> /guide/topics/manifest/activity-element#exported
sqlite_index_read_write_tradeoff
                        -> sqlite.org/queryplanner.html, or the androidx.room.Index reference
security_scoped_storage_media_access
                        -> /training/data-storage/shared/photopicker
pagination_keyset_vs_offset
                        -> PagingSource reference (key-based), or a database-vendor
                           primary source for keyset pagination
test_dependency_substitution_constructor
                        -> /training/dependency-injection (manual DI + testing),
                           currently cites the Hilt testing guide for a
                           hand-rolled-singleton question
service_locator_vs_di_001
                        -> drop the /topic/architecture entry; the manual-DI
                           source already carries the claim
http_idempotency_001    -> deep-link the section:
                           rfc-editor.org/rfc/rfc9110.html#name-idempotent-methods
```

TWO TITLE/URL MISMATCHES — the source title claims one document and the URL
points at another:

```
navigation_graph_viewmodel_shared_scope
  title "Scope a ViewModel to the Navigation Graph"
  url   /guide/fragments/communicate#viewmodel   (a Fragment page, for a
        destination-framed question)

workmanager_vs_foreground_service_constraint
  title "Choose the Right Background API"
  url   /develop/background-work/background-tasks/persistent/getting-started/
        define-work#persistent   (the "Define work requests" page)
  fix   /develop/background-work/background-tasks (the actual chooser)
```

======================================================================
PHASE 6 — P2: VERSION ASSUMPTIONS AND DEFENSIBLE DISTRACTORS
============================================================

FRESHNESS

```
compose_stability_001
compose_skipping_stable_parameter_contract
  Both describe pre-strong-skipping semantics with no version stated. Strong
  skipping has been ENABLED BY DEFAULT since the Compose compiler shipped with
  Kotlin 2.0.20: all restartable composables become skippable, unstable
  parameters are compared with instance equality (===) rather than blocking
  skipping outright, and lambdas are auto-memoized.
  Evidence:
  https://developer.android.com/develop/ui/compose/performance/stability/strongskipping
  The keys remain defensible under === on a new-but-equal instance, so this is a
  qualification, not a correction.
  Fix: state the assumption, or convert one of the two into a strong-skipping
  question (what === comparison means for a fresh List instance holding equal
  contents). compose_recomposition_performance_001 inherits the same caveat.

navigation3_back_stack_ownership
  Correct for Navigation 3, but the stem never says Nav3 is in play, and a
  candidate who knows only Navigation 2 cannot reason about an app-owned key
  list. Fix: open with the assumption ("An app adopting the Navigation 3
  library..."), and re-verify against the guide as Nav3 stabilises.

foreground_service_types_001
  "For apps targeting modern Android versions" — name the API level (34).
```

DEFENSIBLE DISTRACTORS — an option marked incorrect that a senior engineer could
reasonably defend. The contract treats this as a top-priority defect. In each
case fix the STEM or the option wording, not the key.

```
architecture_solid_dependency_substitution
  _d "Interface segregation, because the logger exposes fewer methods to
      everyone" — replacing a concrete FirebaseLogger with a consumer-owned
      interface in practice declares only the methods the consumer needs, which
      IS interface segregation. The option is excluded only by its justification
      clause.
  fix: constrain the stem ("the interface declares the same members as the
       concrete class"), or reword _d so it is clearly false.

di_constructor_injection_testability
  _c "The framework can resolve the dependencies without any binding
      declaration" — with Dagger, an @Inject constructor IS the binding, which
      this same topic's dagger_inject_provides_binds_selection asserts as
      correct.
  fix: reword to "...without the class being annotated in any way", or
       "...without a module, component, or annotation of any kind".

kmp_tradeoff_when_not_to_share
  _b "When the shared code would need a dependency published for every target"
     — a real and common reason teams duplicate.
  fix: replace with something clearly false, e.g. "When the shared module would
       need more than one target declared in its Gradle configuration".

views_fundamentals_001
  _g "It defines measurement rules that each child View must implement itself"
     — half-endorsed by the question's own explanation ("each child measures
     itself in response to the constraints its parent passes down").
  fix: reword to a clear falsehood, e.g. "It measures each child directly and
       assigns final sizes, bypassing the child's onMeasure".
```

======================================================================
PHASE 7 — P2/P3: CUES, STRUCTURE, AND EDITORIAL POLISH
======================================================

STEMS THAT TELEGRAPH THEIR ANSWER BY WORD-MATCHING

```
room_transactions_001
  stem "must succeed or fail together" -> key "commit or roll back together"
  fix: reframe as a decision — a DAO method doing delete-then-insert without
       @Transaction, and what an observer of a Flow query can see mid-operation

connect_read_write_timeout_distinction
  stem "stalls while waiting for response bytes" -> key "bounds inactivity while
  reading response data"
  fix: make the scenario require a decision — a large streamed download that
       legitimately takes minutes, and which timeout must be raised
       (callTimeout vs readTimeout)

background_limits_001
  stem "choose background APIs with OS limits in mind" -> key "the OS restricts
  background execution". No Android knowledge is exercised.
  fix: make it concrete — a periodic WorkManager request with a 5-minute
       interval (clamped to 15), or a job that never runs for an app in the
       `rare` standby bucket

sensitive_logging_001
  stem "why exclude secrets from logs" -> key "logs can expose sensitive data
  beyond its intended boundary"
  fix: make it concrete — what an adb bugreport contains; R8 log-stripping rules
       and why they do not help retroactively; which logs a non-privileged app
       can read

android_intent_filter_matching
  key "must satisfy Intent resolution rules" is near-tautological and the only
  option using precise terminology; the three distractors are dismissible.
  fix: real misconceptions — "only the action must match; category and data are
       advisory"; "CATEGORY_DEFAULT is not required for implicit activity
       intents"; "the filter with the highest android:priority always wins"
```

THE "every" CUE

"every" appears in 3 correct options and 74 distractor options; in 67 of the 70
questions containing it, every occurrence is in a wrong answer — a 96%-reliable
elimination signal. Across all tracked absolutes the split is 24 vs 181
(0.08 vs 0.23 per option), so the previous perfect discriminator is gone, but
this one word has drifted back into being a signal.

Apply the playbook's own remedy: use "every" in CORRECT answers wherever it is
literally true. Never add an absolute to a correct answer that is not literally
true. Examples that are true:

```
"every target must supply a matching actual"
"every read executes the getter"
"every insert touching the indexed column updates it too"
```

MULTI-ANSWER QUESTIONS WITH THREE CORRECT OPTIONS OF FOUR

A single distractor means "select everything" scores 75%. Add a fourth option so
two are wrong, or convert to single-answer:

```
di_constructor_injection_testability
background_api_selection_criteria
release_shrinking_responsibilities
background_sync_001
system_design_observability_signals
kmp_shared_layer_selection
```

STALE-BUT-RESOLVING URLS — update the authored address in place:

```
/topic/libraries/architecture/workmanager (+3 sub-pages)
                     -> /develop/background-work/background-tasks/persistent/...
/develop/ui/views/notifications/channels
                     -> /develop/ui/compose/notifications/channels
/develop/ui/views/notifications/notification-permission
                     -> /develop/ui/compose/notifications/notification-permission
/build/shrink-code   -> /topic/performance/app-optimization/enable-app-optimization
/topic/performance   -> /topic/performance/overview
/training/app-links/verify-android-applinks
                     -> /training/app-links/verify-applinks
/develop/background-work/services/alarms/schedule
                     -> /develop/background-work/services/alarms
firebase.google.com/docs/cloud-messaging/android/receive
                     -> .../receive-messages
jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform-and-jetpack-compose.html
                     -> kotlinlang.org/docs/multiplatform/compose-multiplatform-and-jetpack-compose.html
rfc-editor.org/rfc/rfc9110
                     -> deep-link the section rather than the info page
```

QUESTIONS TOO BASIC FOR A MID/SENIOR STANDARD — raise or deliberately keep as
floor questions:

```
kotlin_nullability_001   options _a and _c are exact mirrors — a coin flip
                         raise to: platform types, !! vs requireNotNull, or
                         nullability of an unbounded T
kotlin_equality_001      same mirrored shape
                         raise to: the equals/hashCode contract with a mutated
                         key already in a HashSet
android_components_001   bare four-way component recall, no scenario
view_rendering_001       ordering recall with permuted distractors
                         raise to: why a nested-weight LinearLayout measures
                         children twice; why requestLayout() from onDraw() is a bug
back_handling_001        near-definitional
                         raise to: popUpTo/inclusive vs launchSingleTop, or
                         predictive back obligations
runtime_permissions_001  the key is a UX principle while all distractors are
                         platform-false — the option sets are not comparable
                         raise to: shouldShowRequestPermissionRationale() and
                         the real state machine
```

EDITORIAL

```
kotlin_sealed_types_001
  explanation says a sealed type restricts direct subtypes to "the same module".
  Since Kotlin 1.5 the rule is the same module AND the same package.

compose_view_interop_disposal
  the id says "disposal" but the question is entirely about AndroidView factory
  vs update; disposal is the subject of compose_view_disposal_strategy_lifecycle.
  A rename is a concept-level id change and implies deprecating the old id —
  weigh that cost against the maintenance confusion.

kotlinx Flow doc URL is cited in two canonical forms
  kotlinlang.org/docs/coroutines-flow.html   (4 questions)
  kotlinlang.org/docs/flow.html#buffering    (1 question)
  Pick one form, and prefer deep anchors (#flow-context, #buffering,
  #conflation, #exception-transparency).
```

======================================================================
PHASE 8 — COVERAGE GAPS (ONLY IF EXPLICITLY IN SCOPE)
=====================================================

DO NOT add questions unless this phase is named in SCOPE. When PHASE 4 re-points
a duplicate question, prefer a concept from this list.

Structural context: the taxonomy is far ahead of the content — 361 subtopics for
270 questions. 160 subtopics have no question at all, 142 have exactly one, and
only 59 have two or more. Worst: kmp (18 of 27 empty), networking (16 of 28),
build_delivery (15 of 25), mobile_system_design (13 of 24), performance (12 of 23).

Highest-value absences:

```
async_reactive    callbackFlow / awaitClose (entirely absent); shareIn vs
                  stateIn and the SharingStarted variants; StateFlow's
                  equality-based conflation; combine / flatMapLatest / debounce /
                  distinctUntilChanged; Mutex vs synchronized; withTimeout;
                  NonCancellable cleanup; runBlocking; Dispatchers.Main.immediate;
                  limitedParallelism

android_ui        Compose phases and deferred state reads (Modifier.offset {},
                  graphicsLayer); strong skipping and @Immutable vs @Stable;
                  movableContentOf / SubcomposeLayout / Modifier.Node;
                  window insets and edge-to-edge; WindowSizeClass

kmp               Swift/Objective-C interop, suspend functions and Flow as seen
                  from Swift; the Kotlin/Native memory model; XCFramework /
                  CocoaPods / SPM; Ktor, SQLDelight vs Room-KMP, kotlinx-datetime;
                  multiplatform ViewModel, CMP navigation and resources

di                assisted injection (ViewModel + nav args); multibindings;
                  component dependencies vs subcomponents; @AndroidEntryPoint,
                  @InstallIn, EntryPointAccessors; Provider<T>/Lazy<T> to break a
                  cycle; Hilt test rules; kapt vs KSP

local_data        file and media storage entirely — scoped storage, MediaStore,
                  SAF, cacheDir vs getExternalFilesDir; Room migration testing
                  (MigrationTestHelper, exported schemas); @Transaction on DAO
                  methods; Paging 3 with Room; EncryptedSharedPreferences
                  deprecation

lifecycle_nav     predictive back (OnBackPressedDispatcher, BackHandler); launch
                  modes / taskAffinity / the system task stack; the ~1 MB Binder
                  limit and TransactionTooLargeException; type-safe Navigation
                  Compose routes

testing           Turbine (the bank raises the toList() hazard but never names a
                  working approach); Robolectric; screenshot testing;
                  MockWebServer; Espresso idling; Hilt test rules; KMP commonTest

performance       Macrobenchmark / Microbenchmark and Baseline Profile
                  generation; cold/warm/hot start; Perfetto; onTrimMemory and the
                  low-memory killer; overdraw and recomposition counts; LeakCanary

build_delivery    configuration cache and build cache; convention plugins vs
                  buildSrc; signing and Play App Signing; dynamic feature
                  modules; CI sharding; R8 full mode and the keep-rule workflow

networking        ConnectivityManager / NetworkCallback and metered networks;
                  HTTP/2 and connection pooling; WebSocket / SSE; Ktor client;
                  multipart upload; polymorphic serialization and schema evolution

background/notif  Doze and App Standby buckets; expedited work; CoroutineWorker
                  and HiltWorker; work chaining; observing WorkInfo; Android 15
                  FGS timeouts; notification trampolines (banned from Android 12);
                  FCM priority and Doze delivery

security          Network Security Configuration; permission protection levels;
                  biometrics and auth-bound Keystore keys; Play Integrity;
                  package visibility (<queries>); backup and data-extraction
                  rules; WebView security

architecture      UI events guidance; state-holder taxonomy; Paging architecture;
                  "design the X" questions; push vs poll; analytics batching;
                  auth/session design; rate limiting and backoff jitter;
                  multi-device sync beyond last-write-wins
```

======================================================================
VERIFICATION — RUN AFTER EVERY PHASE
====================================

1. STRUCTURAL

```bash
./gradlew :shared:jvmTest --rerun-tasks
```

Baseline as of the audit: 58 suites, 399 tests, 0 failures, 0 errors, 0 skipped,
including CurriculumValidatorTest (14), InitialCurriculumSmokeTest (5),
CurriculumJsonCodecTest (9), CurriculumImporterTest (16),
CurriculumDatabaseMigrationTest (2). Any regression is a blocker.

2. ANTI-CUE AUDIT

Run the script in `docs/content/question-authoring-playbook.md` Part 3, plus the
multi-answer position check added in PHASE 2.

Targets to hold or improve:

```
correct answer >10% longer than every distractor      0        (currently 0)
mean correct / mean distractor length ratio           ~1.0     (currently 1.037)
correct-longest share                                 <=~45%   (currently 43%)
absolutes per option, correct vs distractor           narrow the 0.08 / 0.23 gap
"every" in correct vs distractor options              improve on 3 / 74
multi-answer: option A correct                        ~half    (currently 31/32)
single-answer position spread                         no obvious pattern
                                                      (currently A26 B31 C24 D19)
```

3. SOURCE LIVENESS — every URL you touched, plus a full sweep before merge

```bash
python3 -c "
import json
d = json.load(open('shared/src/commonMain/composeResources/files/curriculum/initial_curriculum.json', encoding='utf-8'))
print('\n'.join(sorted({s['url'] for q in d['questions'] for s in q['sources']})))
" | while read -r u; do
  out=$(curl -s -o /dev/null -w '%{http_code} %{num_redirects} %{url_effective}' -L --max-time 30 "$u")
  echo "$out <= $u"
done
```

Report: total checked, 200s, redirects, failures. Do NOT replace a redirecting
URL automatically — decide case by case whether the redirect is benign or the
authored address has genuinely gone stale.

4. ANSWER-ID INTEGRITY

```
every correctAnswerId resolves to an option of its own question
all question ids unique; all answer ids unique
retired answer ids handled as a migration (see GROUND RULES 3)
no answer id reused for a different claim
```

======================================================================
REPORTING
=========

At the end of the run, report:

```
phases completed
questions changed, by id
answer ids retired and answer ids introduced
sources replaced, with the URL that was verified and its HTTP status
anti-cue metrics before and after
test results
anything found that was NOT in this prompt
anything deliberately left undone, and why
```

State plainly if a phase was only partially completed. Do not describe work as
finished unless it is finished and verified.

======================================================================
GUARDRAILS
==========

```
Do not change any correct answer's claim.
Do not delete or deprecate a question unless the phase says to.
Do not add questions outside PHASE 8.
Do not modify the Question model, Room schema, migrations, selectionMode,
  AssessmentEngine, or the UI.
Do not write a URL you have not fetched in this session.
Do not commit, push, or open a PR without explicit approval.
Do not mark any GitHub issue Done.
```

END PROMPT

---

## Provenance

The findings embedded above come from a full editorial and technical audit of
all 270 questions performed on 2026-08-30 against commit `571f748`:

- 0 P0, 10 P1, 113 P2, 38 P3 findings across 161 of 270 questions.
- All 270 answer keys were independently re-derived and verified correct,
  including all 32 multi-answer keys. No answer-key defect exists.
- All 173 unique source URLs returned HTTP 200; 20 resolved through a redirect.
- `./gradlew :shared:jvmTest` passed with 399 tests and no structural errors.

The three systemic defects, in priority order: the multi-answer position cue
(option A correct in 31 of 32), 39 duplicate concept clusters, and a distractor
quality regression confined almost entirely to the 90 questions added in commit
`b087173` (33 of 43 distractor findings).
