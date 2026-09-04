# Question Bank Coverage Snapshot

## Purpose

This document records **what the interview question bank currently covers**, so
that planning the next expansion does not require re-reading all 399 questions.
A full coverage review is expensive; this is the checkpoint that replaces it.

`docs/content/content-authoring.md` is the editorial contract and
`docs/content/question-authoring-playbook.md` is the method. This document is neither —
it is the *state*. Read it first when the task is "add questions", "find gaps",
or "is X already covered?".

### How to use it

1. Read **Headline numbers** and **Topic coverage** to see where the bank is
   thin.
2. Read **Empty subtopics** — the triage there already separates real gaps from
   subtopics that are empty on purpose. Do not treat a zero as an automatic
   instruction to author five questions.
3. Read **Concept coverage** for the high-frequency interview families; it says
   which concepts are covered, which are thin, and which are absent, at a
   finer grain than the subtopic taxonomy.
4. Use the **Subtopic index** to check for duplication. Question IDs are
   semantic, so the index doubles as a concept list — scanning the target
   subtopic's IDs is usually enough to tell whether an idea is already taken.
5. Only fall back to reading `initial_curriculum.json` when the index leaves a
   genuine doubt about a specific question's angle.

### Staleness

The reproducible baseline is this document together with
`shared/src/commonMain/composeResources/files/curriculum/initial_curriculum.json`
from the same commit. Anything that adds, deprecates, or re-homes a question
invalidates the numbers.
**Regenerate it in the same PR that changes the bank** — see below. The prose
sections (triage, concept coverage, notes) are human judgment and need editing
by hand; the tables are generated.

### Regenerating the tables

Run from the repository root. The first block prints the headline numbers and
the topic table, the second prints the full subtopic index.

```python
import json
from collections import Counter

PATH = 'shared/src/commonMain/composeResources/files/curriculum/initial_curriculum.json'
d = json.load(open(PATH, encoding='utf-8'))
act = [q for q in d['questions'] if q['status'] == 'ACTIVE']
bysub = Counter(q['subtopicId'] for q in act)
subs = [s['id'] for s in d['subtopics']]

print(f"total {len(d['questions'])}  active {len(act)}  "
      f"deprecated {len(d['questions']) - len(act)}")
print('mode', Counter(q['selectionMode'] for q in d['questions']))
print('MULTIPLE with one correct answer',
      sum(1 for q in d['questions']
          if q['selectionMode'] == 'MULTIPLE' and len(q['correctAnswerIds']) == 1))
print('subtopic histogram', dict(sorted(Counter(bysub.get(s, 0) for s in subs).items())))

print('\n| Topic | `topicId` | Active | Subtopics | Covered | Empty | Density |')
print('|---|---|---:|---:|---:|---:|---:|')
rows = []
for t in d['topics']:
    ts = [s for s in d['subtopics'] if s['topicId'] == t['id']]
    n = sum(bysub.get(s['id'], 0) for s in ts)
    cov = sum(1 for s in ts if bysub.get(s['id'], 0))
    rows.append((n / len(ts), t['name'], t['id'], n, len(ts), cov, len(ts) - cov))
for dens, name, tid, n, ns, cov, emp in sorted(rows, reverse=True):
    print(f'| {name} | `{tid}` | {n} | {ns} | {cov} | {emp} | {dens:.2f} |')
```

```python
import json

PATH = 'shared/src/commonMain/composeResources/files/curriculum/initial_curriculum.json'
d = json.load(open(PATH, encoding='utf-8'))
active, deprecated = {}, {}
for q in d['questions']:
    (active if q['status'] == 'ACTIVE' else deprecated).setdefault(q['subtopicId'], []).append(q['id'])

for t in d['topics']:
    ts = [s for s in d['subtopics'] if s['topicId'] == t['id']]
    n = sum(len(active.get(s['id'], [])) for s in ts)
    empty = sum(1 for s in ts if not active.get(s['id']))
    print(f"### {t['name']}\n")
    print(f"`{t['id']}` — **{n} active** across {len(ts)} subtopics "
          f"({len(ts) - empty} covered, {empty} empty)\n")
    print('| Subtopic | n | Question IDs |\n|---|---:|---|')
    for s in ts:
        ids, dep = active.get(s['id'], []), deprecated.get(s['id'], [])
        cell = ', '.join(f'`{i}`' for i in ids) if ids else '—'
        if dep:
            cell += (' ' if ids else '') + '_(deprecated: ' + ', '.join(f'`{i}`' for i in dep) + ')_'
        print(f"| `{s['id']}` — {s['name']} | {len(ids)} | {cell} |")
    print()
```

The anti-cue audit script lives in `docs/content/question-authoring-playbook.md` Part 3;
its current output is reproduced under **Audit baselines** below.

## Headline numbers

| Metric | Value |
|---|---:|
| Total questions | 399 |
| ACTIVE | 360 |
| DEPRECATED | 39 |
| Topics | 17 |
| Subtopics | 361 |
| Subtopics with ≥1 active question | 283 (78%) |
| Subtopics with 0 active questions | 78 |
| SINGLE | 353 |
| MULTIPLE | 46 |
| — of which exactly one correct answer | 3 |
| Answer options | 1602 (393 questions with 4 options, 6 with 5) |
| Source references | 454 across 279 unique URLs |

Subtopic depth distribution: **78** subtopics have 0 questions, **220** have 1,
**52** have 2, **8** have 3, **3** have 4.

The bank averages one question per subtopic. That is the number to keep in mind:
the taxonomy is deliberately wider than the content, so most subtopics being at
1 is the designed steady state, not a deficiency.

## Topic coverage

Sorted by density (active questions ÷ subtopics). The bottom of this table is
where the next expansion should look first.

| Topic | `topicId` | Active | Subtopics | Covered | Empty | Density |
|---|---|---:|---:|---:|---:|---:|
| Lifecycle, State & Navigation | `lifecycle_navigation` | 23 | 11 | 11 | 0 | 2.09 |
| Android Platform & Application Model | `android_platform` | 16 | 8 | 8 | 0 | 2.00 |
| Coroutines, Flow & Reactive Programming | `async_reactive` | 38 | 26 | 24 | 2 | 1.46 |
| Application Architecture & Design Principles | `architecture` | 22 | 18 | 16 | 2 | 1.22 |
| Local Persistence & Offline Data | `local_data` | 21 | 19 | 17 | 2 | 1.11 |
| Kotlin Language & JVM Fundamentals | `kotlin_language` | 25 | 23 | 19 | 4 | 1.09 |
| UI — Views & Jetpack Compose | `android_ui` | 26 | 24 | 20 | 4 | 1.08 |
| Testing & Testability | `testing` | 22 | 22 | 19 | 3 | 1.00 |
| Performance, Memory & Debugging | `performance` | 22 | 23 | 18 | 5 | 0.96 |
| Background Work & OS Constraints | `background_work` | 19 | 20 | 17 | 3 | 0.95 |
| Dependency Injection | `dependency_injection` | 23 | 25 | 20 | 5 | 0.92 |
| Networking & Serialization | `networking` | 24 | 28 | 21 | 7 | 0.86 |
| Security, Privacy & Permissions | `security` | 17 | 20 | 16 | 4 | 0.85 |
| Mobile System Design | `mobile_system_design` | 17 | 24 | 16 | 8 | 0.71 |
| Build System, Modularization & Delivery | `build_delivery` | 17 | 25 | 15 | 10 | 0.68 |
| Notifications & Push Messaging | `notifications` | 12 | 18 | 11 | 7 | 0.67 |
| Kotlin Multiplatform & Compose Multiplatform | `kmp` | 16 | 27 | 15 | 12 | 0.59 |
| **Total** | | **360** | **361** | **283** | **78** | **1.00** |

Two caveats before acting on this table:

- **Density is a prompt, not a verdict.** `notifications` and `kmp` sit at the
  bottom partly because their taxonomies are unusually fine-grained (18 and 27
  subtopics for areas that carry fewer distinct interview concepts than, say,
  Compose).
- **Density hides depth.** `async_reactive` looks comfortable at 1.46, but its
  38 questions are spread across 24 subtopics, so most individual concepts still
  have exactly one question. Use the Concept coverage section, not the density,
  to judge whether a concept is genuinely tested.

## Audit baselines

Current output of the `docs/content/question-authoring-playbook.md` Part 3 script over
the whole bank:

```
correct-longest 151/356 (42%), mean ratio 1.03, over 10% limit: 0
absolutes: distractors 0.22/opt, correct 0.11/opt
position: {0: 26%, 1: 27%, 2: 26%, 3: 20%, 4: 1%}
```

All 291 unique source URLs returned HTTP 200 at the time of this snapshot, every one
rendered a non-empty body, and every `#fragment` among them resolved to a real anchor.

These are the numbers a new batch must not degrade. In particular: **zero
questions exceed the 10% correct-answer length limit**, and correct answers do
use absolute words (0.12/opt against 0.22/opt in distractors), so "the option
with 'only' in it is wrong" is not a working strategy. Both properties are easy
to break by accident and are the reason the audit exists.

The length and absolutes audits are now also enforced by
`InitialCurriculumContentQualityTest`, so a batch that degrades either fails the build
rather than only the snapshot above.

**HTTP 200 does not mean the snapshot is clean.** The first-100 review of 2026-09-04 found
`kotlinlang.org/docs/cancellation-and-timeouts.html` returning 200 while rendering nothing,
and `kotlinlang.org/docs/coroutines-flow.html` returning 200 after the sections it was cited
for had been removed. Both were replaced, along with every other decayed citation the review
found: all 14 `lysine.dev` mirror links now point at Square's own repositories, the gutted
`dagger.dev/dev-guide/` links moved to `/dev-guide/basic-usage`, and Kotlin's relocated
multiplatform pages moved under `/docs/multiplatform/`.

**A 200 proves almost nothing on its own.** The full review found three independent ways a
citation can be dead behind a 200: a page whose section was renamed (the anchor check), a
page whose content was removed (the body-length check), and a page that was replaced by a
redirect shell (both). All three scripts are now in the playbook's Part 7 and all three pass
on the whole bank. They are worth running on a schedule rather than only when questions
change — vendor documentation decayed faster than the questions did.

## Deprecated questions

39 questions are `DEPRECATED`. They are retained for stable identity and
historical attempts, and are excluded from active selection.

**Why this matters when authoring:** a deprecated question still occupies its
concept. If a subtopic shows 1 active question plus a deprecated one, the
concept space is more crowded than the count suggests — check what the
deprecated question tested before writing something adjacent to it. Several
subtopics owe their current count entirely to a replacement that landed
elsewhere in the taxonomy.

| Question | Topic / Subtopic |
|---|---|
| `android_components_001` | `android_platform` / `android_components` |
| `fragment_lifecycle_001` | `lifecycle_navigation` / `fragment_lifecycle` |
| `process_death_001` | `lifecycle_navigation` / `process_death` |
| `saved_state_001` | `lifecycle_navigation` / `saved_state` |
| `back_handling_001` | `lifecycle_navigation` / `back_handling` |
| `view_rendering_001` | `android_ui` / `view_rendering` |
| `compose_state_001` | `android_ui` / `compose_state` |
| `compose_recomposition_001` | `android_ui` / `compose_recomposition` |
| `compose_lazy_layouts_001` | `android_ui` / `compose_lazy_layouts` |
| `compose_skipping_stable_parameter_contract` | `android_ui` / `compose_stability` |
| `kotlin_generics_001` | `kotlin_language` / `kotlin_generics` |
| `kotlin_sequences_001` | `kotlin_language` / `kotlin_sequences` |
| `coroutine_builders_001` | `async_reactive` / `coroutine_builders` |
| `sharedflow_001` | `async_reactive` / `sharedflow` |
| `coroutine_vs_thread_suspension` | `async_reactive` / `coroutine_fundamentals` |
| `coroutine_scope_vs_supervisor_scope_failure` | `async_reactive` / `coroutine_supervision` |
| `cpu_loop_cooperative_cancellation` | `async_reactive` / `coroutine_cancellation` |
| `repository_pattern_001` | `architecture` / `repository_pattern` |
| `repository_vs_data_source_responsibility` | `architecture` / `repository_pattern` |
| `dependency_direction_001` | `architecture` / `dependency_direction` |
| `architecture_tradeoffs_001` | `architecture` / `architecture_tradeoffs` |
| `constructor_injection_001` | `dependency_injection` / `constructor_injection` |
| `di_framework_tradeoff_compile_vs_runtime` | `dependency_injection` / `di_framework_tradeoffs` |
| `dagger_module_binding_declarations` | `dependency_injection` / `dagger_modules` |
| `dagger_graph_assembly_generated_component` | `dependency_injection` / `dependency_graphs` |
| `room_flow_001` | `local_data` / `room_flow` |
| `foreground_services_001` | `background_work` / `foreground_services` |
| `notification_channels_001` | `notifications` / `notification_channels` |
| `fcm_data_messages_001` | `notifications` / `fcm_data_messages` |
| `coroutine_testing_001` | `testing` / `coroutine_testing` |
| `compose_ui_testing_001` | `testing` / `compose_ui_testing` |
| `anr_001` | `performance` / `anr` |
| `memory_leaks_001` | `performance` / `memory_leaks` |
| `runtime_permissions_001` | `security` / `runtime_permissions` |
| `dependency_configurations_001` | `build_delivery` / `dependency_configurations` |
| `modularization_dependency_direction` | `build_delivery` / `module_dependency_direction` |
| `offline_design_001` | `mobile_system_design` / `offline_design` |
| `shared_vs_platform_code_001` | `kmp` / `shared_vs_platform_code` |
| `kmp_shared_ui_platform_experience_tradeoff` | `kmp` / `shared_vs_platform_code` |

Five subtopics currently hold **only** a deprecated question and no active one:
`architecture_tradeoffs`, `dagger_modules`, `dependency_configurations`,
`di_framework_tradeoffs`, and `notification_channels`. Each is a real gap where
the replacement question was deliberately filed under a different subtopic; they
are listed again in the triage below.

## Empty subtopics

78 subtopics have no active question. This triage is the part of the snapshot
that a script cannot regenerate — it records which zeros are gaps and which are
intentional.

### Worth filling (ranked)

**Tier 1 — genuine senior-interview concepts with no coverage anywhere**

| Subtopic | Topic | Note |
|---|---|---|
| `certificate_pinning` | security | Only the networking-side trade-off question exists; the mechanism and rotation story are untested |
| `secure_storage` | local/security | EncryptedFile / key-wrapping choices |
| `compose_performance` | performance | Only `compose_recomposition_performance` exists; stability tooling, `@Immutable`, deferred reads at the performance layer |
| `architecture_tradeoffs` | architecture | Deprecated-only; over-engineering / when not to layer |
| `di_framework_tradeoffs` | dependency_injection | Deprecated-only |
| `dagger_modules` | dependency_injection | Deprecated-only; `@Module` responsibilities distinct from `@Component` |
| `room_entities`, `schema_migrations` | local_data | Migrations are covered from the failure side only |
| `ktor_client` | networking | Directly relevant to this repo's KMP direction |
| `test_determinism` | testing | Adjacent to `flaky_tests`, but the ordering/isolation angle is untested |
| `build_types`, `product_flavors`, `dependency_configurations` | build_delivery | `dependency_configurations` is deprecated-only |
| `system_caching`, `client_scalability`, `mobile_resource_constraints` | mobile_system_design | |
| `shared_domain`, `shared_data`, `shared_presentation` | kmp | The "what to actually share" layer questions |
| `ktor_kmp`, `room_kmp`, `sqldelight`, `koin_kmp` | kmp | Concrete library choices; `sqldelight` is only touched via the `room_vs_sqldelight` comparison |
| `fcm_priority`, `push_delivery_constraints`, `notification_channels` | notifications | `notification_channels` is deprecated-only |

**Tier 2 — useful, lower frequency**

`content_provider_security`, `encryption_fundamentals`, `android_profiler`,
`network_inspection`, `layout_inspector`, `lazy_initialization`,
`app_standby`, `battery_restrictions`, `background_process_death`,
`notification_grouping`, `fcm_notification_messages`, `fcm_token_refresh`,
`push_system_architecture`, `rest`, `json_fundamentals`,
`serialization_strategies`, `streaming`, `koin_fundamentals`, `koin_scopes`,
`koin_viewmodels`, `release_builds`, `resource_shrinking`,
`android_gradle_plugin`, `ci_fundamentals`, `cd_fundamentals`,
`system_data_flow`, `backward_compatibility`, `system_security`,
`system_testing_strategy`, `large_scale_modularization`,
`kmp_dependency_hierarchy`, `platform_implementations`, `kmp_navigation`,
`platform_ui_interop`, `kotlinx_serialization_kmp`, `mvc`, `junit`, `espresso`.

### Intentionally empty — do not fill without a reason

These were considered and rejected during the last expansion. They are low
senior-interview value, or the concept is adequately reached through a
neighbouring subtopic.

| Subtopic | Why it stays empty |
|---|---|
| `kotlin_variables`, `kotlin_functions`, `kotlin_classes`, `kotlin_interfaces_inheritance` | Basic syntax; the bank targets mid-to-senior and tests these implicitly through harder questions |
| `xml_layouts`, `view_binding`, `compose_previews`, `compose_theming` | Tooling and legacy layout mechanics with little interview signal |
| `gson`, `moshi` | Superseded by kotlinx.serialization, which is covered; a comparison question would be trivia |
| `rxjava_fundamentals`, `flow_vs_rxjava` | Legacy; would date quickly. Revisit only if RxJava re-enters the target job profile |
| `gradle_wrapper`, `app_versioning` | Mechanical facts with no engineering judgment to test |

## Concept coverage

Subtopic counts are coarse. This section tracks the concept families that
actually get asked in interviews, at the grain the questions are written.

### Coroutines and Flow

**Covered with a dedicated question:** suspension vs blocking · `runBlocking` on
the main thread · `CoroutineScope` ownership · Job parent/child cancellation ·
structured concurrency · cooperative cancellation · `CancellationException`
rethrow · `runInterruptible` · `supervisorScope` nesting · `SupervisorJob` in a
child context · `launch` vs `async` · unawaited `Deferred` · exception surfacing
at `await` · `CoroutineExceptionHandler` · `withContext` · `Dispatchers.IO` vs
`Default` · dispatcher assumptions in suspending libraries · sequential
`async().await()` · Flow coldness · `flowOn` · `catch` upstream-only ·
`retryWhen` · `buffer` · `conflate` vs `collectLatest` · `callbackFlow` /
`awaitClose` · `flatMapLatest` · `debounce` vs `distinctUntilChanged` · `combine`
vs `zip` · `launchIn` · producer runs in the collector's coroutine · `StateFlow`
· `SharedFlow` replay · `stateIn` vs `shareIn` · `SharingStarted.WhileSubscribed`
· Channel vs Flow · `repeatOnLifecycle` · `viewModelScope` cancellation ·
LiveData vs StateFlow.

**Thin:** `CoroutineContext` (one question, and it is really about
`SupervisorJob`) — context element inheritance and `+` composition are untested.
Coroutine-vs-thread cost was deprecated and never replaced.

**Absent by choice:** RxJava, Flow vs RxJava.

This family is now the best-covered in the bank. Treat it as saturated unless a
specific concept above is listed as thin.

### Lifecycle and state

Covered: Activity `onPause` vs `onStop` · Fragment view lifecycle and back-stack
instance survival · configuration change vs process recreation · `ViewModelStore`
retention · `onCleared` · `SavedStateHandle` · `TransactionTooLargeException` ·
`rememberSaveable` vs ViewModel ownership · reproducing process death · launch
modes · back stack · Navigation 3 state ownership · `popUpTo` · predictive back ·
App Links.

Thin: deep links has one question; Navigation 2 vs 3 has one.

### Compose and Views

Covered: declarative vs retained model · composition vs recomposition · state
reads and recomposition scope · `remember` vs `rememberSaveable` · state hoisting
· UDF event direction · `LaunchedEffect` keys · `derivedStateOf` · `snapshotFlow`
· modifier order · deferred reads (`Modifier.offset {}`) · lazy `contentType` ·
keys and item identity · stability and strong skipping · `CompositionLocal` ·
semantics · `AndroidView` interop and disposal · measure/layout/draw ·
`invalidate` vs `requestLayout` · touch interception · RecyclerView recycling and
DiffUtil.

Thin: theming, previews, and ViewBinding are untested (deliberately).

### Kotlin and JVM

Covered: nullability · extension resolution is static · data class generation and
shallow `copy` · `object` vs companion · sealed exhaustiveness and its evolution
cost · custom getters · `by lazy` thread safety · star projection · variance ·
`crossinline` / `noinline` · `reified` · read-only vs immutable collections ·
sequence constrained-once and allocation trade-off · `==` vs `===` · scope
function overuse · platform types · type erasure · `@Throws` interop · receiver
lambdas · `internal` visibility.

Thin: `kotlin_collections` and `kotlin_exceptions` have one question each.

### Architecture and DI

Covered: separation of concerns · layered ownership · DTO/entity/domain
boundaries · MVVM responsibilities · MVI single state · one-off events vs durable
state · UDF · use-case reuse and pass-through cost · SSOT · state ownership
taxonomy · SOLID substitution · interface boundary ownership · error mapping and
sealed results · MVP vs MVVM · repository API shape · Clean Architecture
dependency rule · framework types leaking into the domain · constructor injection
· composition root · Dagger compile-time validation, generated factories,
components vs subcomponents, scopes, qualifiers, multibindings · Hilt entry
points, component lifetimes, `@InstallIn`, Hilt vs Dagger · assisted injection ·
manual DI cost · service locator · Koin definitions and KMP.

Thin: Koin beyond definitions and KMP. Absent: `mvc`, `architecture_tradeoffs`,
`di_framework_tradeoffs`.

### Data, networking, background, platform

Covered in depth: Room DAOs/relations/converters/transactions/migrations/Flow
invalidation and compile-time query verification · DataStore vs SharedPreferences
and Preferences vs Proto · `apply` vs `commit` · storage selection · offline-first
and cache invalidation · two-level caching · `filesDir` vs `cacheDir` ·
Room vs SQLDelight · HTTP idempotency, methods, status classification, retry
backoff and jitter, multiplexing, timeouts, caching and revalidation ·
OkHttp interceptors, client reuse, `Authenticator` · Retrofit/coroutines and
cancellation · kotlinx.serialization unknown keys and class discriminators ·
Retrofit vs Ktor · WebSocket liveness · token lifetimes · WorkManager
constraints/chaining/retries/expedited/long-running/reboot persistence · Services,
foreground services, bound services, Doze, AlarmManager vs WorkManager ·
notification channels, importance, permission, trampolines, back stack, actions ·
FCM tokens, priority, app-state behaviour, delivery guarantees, handling window ·
main thread and Looper/Handler/MessageQueue · Binder IPC · Context selection ·
intent filters · process importance · sandbox and permission model · Keystore and
auth-bound keys · PendingIntent mutability · WebView JS bridge · network security
config · secret recoverability · root detection · GC reachability, leaks, heap
limits, ANR, jank, startup, R8, Baseline Profiles, StrictMode, profiling modes ·
Gradle configuration cache, convention plugins, version catalogs, variants,
source sets, dependency resolution, `api` vs `implementation`, modularization
trade-offs, APK vs AAB, shrinking, app signing, KSP vs kapt · system design for
offline, sync, conflicts, pagination, push, sessions, observability, API
evolution, rollout, requirements, non-functional budgets, poison messages, rule
placement, feature flags · KMP source sets, `expect`/`actual`, `actual typealias`,
targets, commonMain API availability, Compose Multiplatform and its resources,
multiplatform ViewModel, library compatibility, sharing trade-offs.

## Notes for the next expansion

- **The bank is no longer coverage-starved; it is depth-starved in places.** The
  cheap wins from filling empty subtopics are largely spent. The next batch will
  produce more value by adding a second, genuinely different question to
  high-frequency subtopics that currently have one than by chasing the remaining
  78 zeros.
- **Check the deprecated table before authoring.** Three subtopics are
  deprecated-only and several others have a retired predecessor whose concept is
  still taken.
- **Author 15–25% of a new batch as MULTIPLE.** The bank sits at 11.5%
  (46/399) because the earliest content used fewer; the last batch ran at 15.6%,
  which is the band to aim for. Only three questions in the whole bank are
  MULTIPLE with a single correct answer — keep authoring some that way, or
  `selectionMode` stays inferable from the answer key.
- **Every question in the bank has 4 options except 6 with 5.** Stay at 4 unless
  there is a specific reason.
- **Source hosts, for reference:** developer.android.com 303 · kotlinlang.org 96
  · github.com 18 (kotlinx.serialization, OkHttp, Retrofit and SQLDelight —
  `square.github.io` returns 404, so each project's own repository is the primary
  source) · dagger.dev 12 · firebase.google.com 12 · rfc-editor.org 8 ·
  docs.gradle.org 6 · and single-digit
  counts for insert-koin.io, ktor.io, sqldelight.github.io, jetbrains.com,
  source.android.com, sqlite.org, google.aip.dev, docs.cloud.google.com.
- **Pinned count tests to update** whenever the bank changes:
  `InitialCurriculumSmokeTest` (totals, status split, selection-mode split, and
  the per-topic map), `CurriculumImporterTest` (`countQuestions`), and
  `CurriculumLocalDataPathTest` (`countQuestions`, the `lifecycle_navigation`
  count, and `RowCounts`). Update the exact numbers; do not relax them into
  inequalities.

## Subtopic index

Every subtopic, its active question count, and the question IDs it holds.
Question IDs are semantic, so this table is also the concept index — check the
target subtopic here before authoring to avoid a near-duplicate.

### Android Platform & Application Model

`android_platform` — **16 active** across 8 subtopics (8 covered, 0 empty)

| Subtopic | n | Question IDs |
|---|---:|---|
| `android_process_model` — Application and process model | 2 | `android_process_model_001`, `process_importance_reclaim_order` |
| `android_components` — Android application components | 2 | `content_provider_uri_permission_grant`, `android_component_process_lifetime` _(deprecated: `android_components_001`)_ |
| `android_context` — Context | 2 | `android_context_001`, `android_context_theme_inflation` |
| `android_intents` — Intents | 2 | `android_intents_001`, `android_intent_filter_matching` |
| `android_manifest` — Manifest and application configuration | 1 | `android_manifest_component_exported` |
| `android_resources` — Android resources and qualifiers | 1 | `android_resources_001` |
| `android_main_thread` — Main thread, Looper, and Handler fundamentals | 3 | `android_main_thread_001`, `android_looper_message_queue`, `handler_looper_message_queue_roles` |
| `android_ipc` — Android IPC fundamentals | 3 | `android_ipc_binder_contract`, `binder_ipc_marshalling_boundary`, `binder_thread_pool_ui_handoff` |

### Lifecycle, State & Navigation

`lifecycle_navigation` — **23 active** across 11 subtopics (11 covered, 0 empty)

| Subtopic | n | Question IDs |
|---|---:|---|
| `activity_lifecycle` — Activity lifecycle | 2 | `activity_lifecycle_001`, `activity_on_pause_vs_on_stop_visibility` |
| `fragment_lifecycle` — Fragment and Fragment view lifecycle | 2 | `fragment_view_lifecycle_collection`, `fragment_back_stack_view_destroyed_binding` _(deprecated: `fragment_lifecycle_001`)_ |
| `configuration_changes` — Configuration changes | 2 | `configuration_changes_001`, `configuration_change_vs_process_recreation` |
| `process_death` — Process death and recreation | 1 | `process_death_reproducing_restoration` _(deprecated: `process_death_001`)_ |
| `viewmodel_lifecycle` — ViewModel lifecycle | 3 | `viewmodel_destination_scope`, `viewmodel_store_configuration_retention`, `viewmodel_clear_owner_finish` |
| `saved_state` — Saved-state mechanisms | 4 | `saved_state_binder_transaction_limit`, `saved_state_transient_inputs`, `savedstatehandle_process_recreation`, `remember_saveable_vs_viewmodel_ownership` _(deprecated: `saved_state_001`)_ |
| `lifecycle_aware_apis` — Lifecycle-aware APIs | 1 | `lifecycle_repeat_on_lifecycle` |
| `navigation_fundamentals` — Navigation and back-stack fundamentals | 4 | `activity_launch_mode_task_stack`, `navigation_fundamentals_001`, `navigation_graph_viewmodel_shared_scope`, `navigation_back_stack_entry_lifetime` |
| `navigation_2_vs_3` — Navigation 2 vs Navigation 3 | 1 | `navigation3_back_stack_ownership` |
| `deep_links` — Deep links | 1 | `deep_link_app_link_verification` |
| `back_handling` — Back handling and predictive back | 2 | `predictive_back_handler_registration`, `navigation_pop_up_to_inclusive` _(deprecated: `back_handling_001`)_ |

### UI — Views & Jetpack Compose

`android_ui` — **26 active** across 24 subtopics (20 covered, 4 empty)

| Subtopic | n | Question IDs |
|---|---:|---|
| `views_fundamentals` — View and ViewGroup fundamentals | 2 | `views_fundamentals_001`, `compose_vs_view_ui_model` |
| `xml_layouts` — XML layouts | 0 | — |
| `view_rendering` — Measure, layout, and draw | 1 | `view_nested_weight_double_measure` _(deprecated: `view_rendering_001`)_ |
| `recyclerview` — RecyclerView | 1 | `recyclerview_001` |
| `custom_views` — Custom Views | 1 | `custom_view_invalidate_vs_request_layout` |
| `view_events` — View event handling | 1 | `view_touch_event_intercept_gesture` |
| `view_binding` — ViewBinding and DataBinding awareness | 0 | — |
| `compose_fundamentals` — Compose fundamentals | 1 | `compose_phases_deferred_state_read` |
| `compose_recomposition` — Composition and recomposition | 2 | `composition_vs_recomposition`, `compose_state_read_recomposition_scope` _(deprecated: `compose_recomposition_001`)_ |
| `compose_state` — Compose state | 1 | `remember_vs_remember_saveable` _(deprecated: `compose_state_001`)_ |
| `compose_state_hoisting` — State hoisting | 1 | `compose_state_hoisting_001` |
| `compose_udf` — Unidirectional data flow in Compose | 1 | `compose_udf_event_direction` |
| `compose_side_effects` — Compose side-effect APIs | 2 | `compose_side_effects_001`, `compose_launched_effect_key_restart` |
| `compose_derived_state` — derivedStateOf and derived state | 1 | `compose_derived_state_threshold` |
| `compose_snapshot_system` — Compose snapshot-system fundamentals | 1 | `compose_snapshot_flow_state` |
| `compose_layouts_modifiers` — Layouts and Modifiers | 2 | `compose_edge_to_edge_insets`, `compose_modifier_order_padding_click` |
| `compose_lazy_layouts` — Lazy layouts | 1 | `compose_lazy_content_type` _(deprecated: `compose_lazy_layouts_001`)_ |
| `compose_identity_keys` — Identity and keys | 1 | `compose_key_identity_lazy_state` |
| `compose_stability` — Stability and skippability | 2 | `compose_stability_001`, `compose_strong_skipping_instance_equality` _(deprecated: `compose_skipping_stable_parameter_contract`)_ |
| `composition_local` — CompositionLocal | 1 | `compose_composition_local_tradeoff` |
| `compose_theming` — Material 3 and theming | 0 | — |
| `compose_accessibility` — Semantics and accessibility | 1 | `compose_accessibility_001` |
| `compose_previews` — Compose previews | 0 | — |
| `views_compose_interop` — Views and Compose interoperability | 2 | `compose_view_interop_disposal`, `compose_view_disposal_strategy_lifecycle` |

### Kotlin Language & JVM Fundamentals

`kotlin_language` — **25 active** across 23 subtopics (19 covered, 4 empty)

| Subtopic | n | Question IDs |
|---|---:|---|
| `kotlin_variables` — val, var, and basic declarations | 0 | — |
| `kotlin_nullability` — Nullability | 1 | `kotlin_nullability_001` |
| `kotlin_functions` — Functions and arguments | 0 | — |
| `kotlin_extension_functions` — Extension functions | 1 | `kotlin_extension_resolution_static` |
| `kotlin_classes` — Kotlin classes | 0 | — |
| `kotlin_data_classes` — Data classes | 2 | `kotlin_data_classes_001`, `data_class_copy_is_shallow` |
| `kotlin_objects` — object and companion object | 2 | `kotlin_objects_001`, `object_vs_companion_object_members` |
| `kotlin_sealed_types` — Sealed classes and interfaces | 2 | `kotlin_sealed_types_001`, `sealed_when_exhaustive_evolution` |
| `kotlin_interfaces_inheritance` — Interfaces and inheritance | 0 | — |
| `kotlin_visibility` — Visibility modifiers | 1 | `kotlin_internal_module_visibility` |
| `kotlin_properties` — Properties, accessors, and backing fields | 1 | `kotlin_custom_getter_recomputes` |
| `kotlin_delegation` — Delegation and delegated properties | 1 | `kotlin_lazy_thread_safety_mode` |
| `kotlin_generics` — Generics and variance | 2 | `kotlin_star_projection_use_site`, `generic_in_out_variance_tradeoff` _(deprecated: `kotlin_generics_001`)_ |
| `kotlin_lambdas` — Lambdas and higher-order functions | 1 | `kotlin_lambda_with_receiver_dsl` |
| `kotlin_inline_functions` — inline, noinline, and crossinline | 2 | `kotlin_crossinline_non_local_return`, `noinline_vs_crossinline_lambda` |
| `kotlin_reified_types` — Reified type parameters | 1 | `kotlin_reified_types_001` |
| `kotlin_collections` — Collections | 1 | `kotlin_readonly_list_not_immutable` |
| `kotlin_sequences` — Sequences and lazy collection processing | 2 | `kotlin_sequence_constrained_once`, `sequence_intermediate_allocation_tradeoff` _(deprecated: `kotlin_sequences_001`)_ |
| `kotlin_equality` — Structural vs referential equality | 1 | `kotlin_equality_001` |
| `kotlin_exceptions` — Exception handling | 1 | `kotlin_no_checked_exceptions_interop` |
| `kotlin_scope_functions` — let/run/apply/also/with | 1 | `kotlin_scope_functions_001` |
| `kotlin_java_interop` — Java interoperability | 1 | `kotlin_platform_type_null_check` |
| `jvm_fundamentals` — JVM fundamentals relevant to Android | 1 | `jvm_fundamentals_001` |

### Coroutines, Flow & Reactive Programming

`async_reactive` — **38 active** across 26 subtopics (24 covered, 2 empty)

| Subtopic | n | Question IDs |
|---|---:|---|
| `coroutine_fundamentals` — Coroutine and suspend fundamentals | 2 | `coroutine_fundamentals_001`, `coroutine_run_blocking_main_thread` _(deprecated: `coroutine_vs_thread_suspension`)_ |
| `coroutine_builders` — launch and async | 2 | `coroutine_async_exception_surfaces_at_await`, `launch_vs_async_unawaited_result` _(deprecated: `coroutine_builders_001`)_ |
| `coroutine_scope` — CoroutineScope | 1 | `coroutine_scope_job_ownership` |
| `coroutine_context` — CoroutineContext | 1 | `coroutine_supervisor_job_child_context_noop` |
| `coroutine_dispatchers` — Dispatchers | 2 | `coroutine_io_dispatcher_blocking_calls`, `suspending_api_dispatcher_assumption` |
| `coroutine_jobs` — Job and parent-child relationships | 1 | `parent_cancellation_propagates_children` |
| `structured_concurrency` — Structured concurrency | 1 | `structured_concurrency_001` |
| `coroutine_cancellation` — Cancellation | 3 | `coroutine_cancellation_001`, `cancellation_exception_rethrow`, `coroutine_run_interruptible_blocking_call` _(deprecated: `cpu_loop_cooperative_cancellation`)_ |
| `coroutine_exceptions` — Exception propagation and handling | 2 | `coroutine_exceptions_001`, `coroutine_exception_handler_root_boundary` |
| `coroutine_supervision` — SupervisorJob and supervisorScope | 1 | `coroutine_supervisor_scope_direct_children` _(deprecated: `coroutine_scope_vs_supervisor_scope_failure`)_ |
| `coroutine_context_switching` — withContext | 1 | `coroutine_context_switching_001` |
| `coroutine_parallelism` — Concurrency and async/await | 1 | `coroutine_async_await_sequential` |
| `lifecycle_coroutines` — Lifecycle-aware coroutine scopes | 1 | `viewmodel_scope_cleared_cancellation` |
| `flow_fundamentals` — Flow fundamentals | 2 | `flow_fundamentals_001`, `callback_flow_await_close_registration` |
| `flow_operators` — Flow operators | 3 | `flow_flat_map_latest_search_cancellation`, `flow_debounce_vs_distinct_until_changed`, `flow_combine_vs_zip_emission_rule` |
| `flow_collection` — Flow collection | 2 | `flow_launch_in_on_each_scope`, `flow_collection_cancels_cold_producer` |
| `flow_errors` — Flow exception handling | 2 | `flow_catch_upstream_only`, `flow_retry_when_conditional_attempts` |
| `flow_context` — flowOn and execution context | 1 | `flow_context_001` |
| `flow_buffering` — Buffering and conflation | 2 | `flow_conflate_vs_collect_latest`, `flow_buffer_producer_consumer_concurrency` |
| `stateflow` — StateFlow | 2 | `stateflow_001`, `stateflow_vs_sharedflow_current_value` |
| `sharedflow` — SharedFlow | 2 | `flow_share_in_vs_state_in`, `shared_flow_replay_late_subscriber` _(deprecated: `sharedflow_001`)_ |
| `hot_vs_cold_streams` — Hot vs cold streams | 1 | `flow_vs_channel_delivery_model` |
| `flow_sharing` — stateIn, shareIn, and sharing policies | 1 | `flow_state_in_while_subscribed` |
| `livedata` — LiveData | 1 | `live_data_vs_state_flow_ui_state` |
| `rxjava_fundamentals` — RxJava/RxKotlin fundamentals | 0 | — |
| `flow_vs_rxjava` — Flow vs RxJava | 0 | — |

### Application Architecture & Design Principles

`architecture` — **22 active** across 18 subtopics (16 covered, 2 empty)

| Subtopic | n | Question IDs |
|---|---:|---|
| `separation_of_concerns` — Separation of concerns | 1 | `separation_of_concerns_001` |
| `layered_architecture` — Layered architecture | 2 | `architecture_paging_ownership`, `dto_entity_domain_model_boundary` |
| `mvc` — MVC | 0 | — |
| `mvp` — MVP | 1 | `mvp_vs_mvvm_view_contract` |
| `mvvm` — MVVM | 1 | `viewmodel_vs_repository_responsibility` |
| `mvi` — MVI | 1 | `architecture_ui_event_consumption` |
| `mvvm_vs_mvi` — MVVM vs MVI | 1 | `architecture_mvi_single_state` |
| `unidirectional_data_flow` — Unidirectional data flow | 1 | `unidirectional_data_flow_001` |
| `repository_pattern` — Repository pattern | 1 | `repository_observable_api_shape` _(deprecated: `repository_pattern_001`, `repository_vs_data_source_responsibility`)_ |
| `use_cases` — Use cases and when to introduce them | 2 | `architecture_use_case_reuse`, `domain_layer_passthrough_cost` |
| `single_source_of_truth` — Single source of truth | 1 | `single_source_of_truth_001` |
| `state_ownership` — State ownership | 4 | `state_ownership_001`, `architecture_state_holder_taxonomy`, `durable_state_vs_one_off_event`, `viewmodel_activity_reference_lifetime` |
| `clean_architecture` — Clean Architecture | 1 | `clean_architecture_dependency_rule_tradeoff` |
| `solid` — SOLID principles | 1 | `architecture_solid_dependency_substitution` |
| `dependency_direction` — Dependency direction and inversion | 1 | `dependency_direction_domain_framework_types` _(deprecated: `dependency_direction_001`)_ |
| `interface_boundaries` — Interface boundaries | 1 | `architecture_interface_boundary_ownership` |
| `error_modeling` — Error representation/modeling | 2 | `architecture_error_mapping_boundary`, `architecture_error_modeling_result_type` |
| `architecture_tradeoffs` — Architecture trade-offs and avoiding over-engineering | 0 | —_(deprecated: `architecture_tradeoffs_001`)_ |

### Dependency Injection

`dependency_injection` — **23 active** across 25 subtopics (20 covered, 5 empty)

| Subtopic | n | Question IDs |
|---|---:|---|
| `di_fundamentals` — Dependency injection fundamentals | 1 | `di_constructor_injection_testability` |
| `manual_di` — Manual dependency injection | 1 | `manual_di_graph_growth_cost` |
| `constructor_injection` — Constructor injection | 1 | `hilt_field_injection_framework_classes` _(deprecated: `constructor_injection_001`)_ |
| `composition_root` — Composition root | 1 | `composition_root_001` |
| `dependency_graphs` — Dependency graphs | 1 | `dagger_compile_time_graph_validation` _(deprecated: `dagger_graph_assembly_generated_component`)_ |
| `di_scopes` — Scopes and lifetimes | 1 | `di_scopes_001` |
| `service_locator_vs_di` — Service locator vs dependency injection | 1 | `service_locator_vs_di_001` |
| `dagger_fundamentals` — Dagger fundamentals | 1 | `dagger_generated_factory_no_reflection` |
| `dagger_modules` — Dagger modules | 0 | —_(deprecated: `dagger_module_binding_declarations`)_ |
| `dagger_bindings` — @Provides and @Binds | 1 | `dagger_inject_provides_binds_selection` |
| `dagger_components` — Components and subcomponents | 3 | `dagger_component_dependency_vs_subcomponent`, `dagger_component_graph_root`, `dagger_subcomponent_parent_binding_inheritance` |
| `dagger_scopes` — Dagger scopes | 1 | `dagger_scope_component_instance_lifetime` |
| `dagger_qualifiers` — Qualifiers | 1 | `dagger_qualifier_same_type_bindings` |
| `dagger_multibindings` — Multibindings | 1 | `dagger_multibinding_into_set` |
| `hilt_fundamentals` — Hilt fundamentals | 1 | `hilt_entry_point_manual_access` |
| `hilt_components` — Hilt components and lifecycle scopes | 1 | `hilt_activity_retained_component_lifetime` |
| `hilt_viewmodels` — Hilt ViewModels | 2 | `di_hilt_viewmodel_scope`, `dagger_assisted_injection_viewmodel` |
| `hilt_modules` — Hilt modules and InstallIn | 1 | `hilt_install_in_binding_visibility` |
| `hilt_vs_dagger` — Hilt vs raw Dagger | 1 | `hilt_vs_dagger_convention_tradeoff` |
| `koin_fundamentals` — Koin fundamentals | 0 | — |
| `koin_definitions` — single/factory definitions | 1 | `di_koin_factory_vs_single` |
| `koin_scopes` — Koin scopes | 0 | — |
| `koin_viewmodels` — Koin ViewModel integration | 0 | — |
| `koin_multiplatform` — Koin and Kotlin Multiplatform | 1 | `koin_multiplatform_common_module` |
| `di_framework_tradeoffs` — DI framework trade-offs | 0 | —_(deprecated: `di_framework_tradeoff_compile_vs_runtime`)_ |

### Local Persistence & Offline Data

`local_data` — **21 active** across 19 subtopics (17 covered, 2 empty)

| Subtopic | n | Question IDs |
|---|---:|---|
| `storage_selection` — Choosing a storage mechanism | 2 | `storage_selection_001`, `datastore_vs_room_data_shape` |
| `shared_preferences` — SharedPreferences | 1 | `shared_preferences_apply_vs_commit` |
| `datastore` — DataStore | 1 | `datastore_preferences_vs_proto` |
| `datastore_vs_sharedpreferences` — DataStore vs SharedPreferences | 1 | `datastore_vs_sharedpreferences_async` |
| `file_storage` — File storage | 1 | `file_storage_cache_dir_vs_files_dir` |
| `sqlite_fundamentals` — SQLite and relational-database fundamentals | 1 | `sqlite_index_read_write_tradeoff` |
| `room_fundamentals` — Room fundamentals | 1 | `room_compile_time_query_verification` |
| `room_entities` — Room entities | 0 | — |
| `room_dao` — Room DAOs and queries | 1 | `room_dao_001` |
| `room_relationships` — Room relationships | 1 | `room_relationship_no_foreign_key_join` |
| `room_type_converters` — Type converters | 1 | `room_type_converter_scope` |
| `room_transactions` — Transactions | 2 | `room_transactions_001`, `room_transaction_dao_method` |
| `room_migrations` — Room migrations | 2 | `room_migration_missing_path`, `room_destructive_migration_tradeoff` |
| `room_flow` — Room and Flow | 1 | `room_flow_invalidation_requery` _(deprecated: `room_flow_001`)_ |
| `caching` — Caching | 1 | `cache_memory_and_disk_levels` |
| `offline_first` — Offline-first architecture | 2 | `offline_first_001`, `offline_first_local_write_then_sync` |
| `cache_invalidation` — Cache invalidation | 1 | `cache_invalidation_staleness_policy` |
| `schema_migrations` — Schema/version migration concepts | 0 | — |
| `room_vs_sqldelight` — Room vs SQLDelight | 1 | `room_vs_sqldelight_generation_direction` |

### Networking & Serialization

`networking` — **24 active** across 28 subtopics (21 covered, 7 empty)

| Subtopic | n | Question IDs |
|---|---:|---|
| `http_fundamentals` — HTTP fundamentals | 1 | `http_connection_reuse_multiplexing` |
| `http_methods` — HTTP methods | 1 | `http_methods_safe_vs_idempotent` |
| `http_status_codes` — HTTP status codes | 1 | `http_status_retry_classification` |
| `rest` — REST concepts | 0 | — |
| `http_idempotency` — Idempotency | 1 | `http_idempotency_001` |
| `pagination` — Pagination | 1 | `pagination_keyset_vs_offset` |
| `network_timeouts` — Timeouts | 2 | `network_timeouts_001`, `connect_read_write_timeout_distinction` |
| `network_retries` — Retries and backoff | 1 | `network_retry_backoff_jitter` |
| `tls_https` — HTTPS/TLS fundamentals | 1 | `tls_certificate_pinning_tradeoff` |
| `okhttp` — OkHttp | 1 | `okhttp_client_instance_reuse_pooling` |
| `okhttp_interceptors` — OkHttp interceptors | 2 | `okhttp_interceptors_001`, `okhttp_application_vs_network_interceptor` |
| `retrofit` — Retrofit | 1 | `retrofit_vs_okhttp_responsibilities` |
| `retrofit_coroutines` — Retrofit and coroutines | 2 | `retrofit_coroutines_001`, `coroutine_http_call_cancellation` |
| `network_error_handling` — Network error handling | 1 | `network_error_transport_vs_http` |
| `ktor_client` — Ktor Client | 0 | — |
| `retrofit_vs_ktor` — Retrofit vs Ktor | 1 | `retrofit_vs_ktor_client_multiplatform` |
| `json_fundamentals` — JSON fundamentals | 0 | — |
| `gson` — Gson | 0 | — |
| `moshi` — Moshi | 0 | — |
| `kotlinx_serialization` — kotlinx.serialization | 1 | `kotlinx_serialization_001` |
| `serialization_strategies` — Reflection vs generated/compiler-assisted serialization | 0 | — |
| `custom_serialization` — Custom serializers/adapters | 1 | `serialization_class_discriminator_mismatch` |
| `json_compatibility` — Missing, unknown, nullable, and default JSON fields | 1 | `json_unknown_keys_compatibility` |
| `network_caching` — HTTP/network caching | 1 | `http_cache_control_revalidation` |
| `websockets` — WebSockets | 1 | `websocket_idle_connection_heartbeat` |
| `streaming` — Streaming concepts | 0 | — |
| `authentication_tokens` — Authentication tokens | 1 | `auth_access_refresh_token_lifetime_split` |
| `token_refresh` — Token refresh flows | 1 | `okhttp_authenticator_token_refresh` |

### Background Work & OS Constraints

`background_work` — **19 active** across 20 subtopics (17 covered, 3 empty)

| Subtopic | n | Question IDs |
|---|---:|---|
| `background_execution` — Android background execution model | 1 | `background_started_service_restriction` |
| `services` — Services | 1 | `service_callbacks_run_on_main_thread` |
| `foreground_services` — Foreground services | 1 | `foreground_service_start_notification_deadline` _(deprecated: `foreground_services_001`)_ |
| `bound_services` — Bound services | 1 | `bound_service_client_binding_lifetime` |
| `workmanager` — WorkManager fundamentals | 1 | `workmanager_expedited_work` |
| `workmanager_constraints` — WorkManager constraints | 2 | `workmanager_constraints_001`, `workmanager_constraints_not_deadline` |
| `workmanager_chaining` — Work chaining | 1 | `workmanager_unique_work_policy` |
| `workmanager_retries` — WorkManager retries | 2 | `workmanager_retry_result_contract`, `idempotent_worker_retry_design` |
| `service_vs_workmanager` — Service vs WorkManager | 1 | `workmanager_vs_foreground_service_constraint` |
| `alarmmanager` — AlarmManager | 1 | `alarm_manager_vs_workmanager_selection` |
| `exact_alarms` — Exact alarms | 1 | `background_exact_alarm_justification` |
| `doze` — Doze | 1 | `doze_maintenance_window_deferral` |
| `app_standby` — App Standby | 0 | — |
| `battery_restrictions` — Battery restrictions | 0 | — |
| `background_limits` — Background execution limits | 1 | `background_limits_001` |
| `background_process_death` — Process-death implications | 0 | — |
| `boot_restart_work` — Work across reboot/restart | 1 | `work_persistence_across_reboot` |
| `long_running_work` — Long-running work | 1 | `long_running_worker_set_foreground` |
| `foreground_service_types` — Foreground-service types and permissions | 1 | `foreground_service_types_001` |
| `background_api_selection` — Choosing the appropriate background API | 1 | `background_api_selection_criteria` |

### Notifications & Push Messaging

`notifications` — **12 active** across 18 subtopics (11 covered, 7 empty)

| Subtopic | n | Question IDs |
|---|---:|---|
| `notification_fundamentals` — Android notification fundamentals | 1 | `notification_channel_required_for_display` |
| `notification_channels` — Notification channels | 0 | —_(deprecated: `notification_channels_001`)_ |
| `notification_importance` — Notification importance | 1 | `notification_channel_user_control` |
| `notification_pending_intents` — PendingIntent in notifications | 1 | `notification_trampoline_restriction` |
| `notification_actions` — Notification actions | 1 | `notification_remote_input_reply_action` |
| `notification_grouping` — Notification grouping | 0 | — |
| `notification_permission` — Runtime notification permission | 2 | `notification_permission_001`, `notification_channel_vs_runtime_permission` |
| `fcm_fundamentals` — Firebase Cloud Messaging fundamentals | 1 | `fcm_delivery_best_effort_signal` |
| `fcm_tokens` — FCM registration tokens | 1 | `fcm_token_rotation_server_mapping` |
| `fcm_notification_messages` — Notification messages | 0 | — |
| `fcm_data_messages` — Data messages | 1 | `fcm_message_priority_doze` _(deprecated: `fcm_data_messages_001`)_ |
| `fcm_app_state_behavior` — Foreground/background behavior | 1 | `fcm_app_state_delivery_behavior` |
| `fcm_token_refresh` — Token refresh | 0 | — |
| `fcm_message_handling` — Message handling | 1 | `fcm_message_handling_work_budget` |
| `notification_deep_links` — Deep linking from notifications | 1 | `notification_pending_intent_back_stack` |
| `push_delivery_constraints` — Push delivery limitations | 0 | — |
| `fcm_priority` — FCM message priority | 0 | — |
| `push_system_architecture` — Backend-to-FCM-to-device architecture | 0 | — |

### Testing & Testability

`testing` — **22 active** across 22 subtopics (19 covered, 3 empty)

| Subtopic | n | Question IDs |
|---|---:|---|
| `testing_strategy` — Test strategy | 1 | `testing_strategy_001` |
| `unit_testing` — Unit tests | 1 | `unit_test_android_stub_not_mocked_error` |
| `integration_testing` — Integration tests | 1 | `robolectric_vs_instrumented` |
| `ui_testing` — UI tests | 1 | `compose_ui_test_clock_manual_advance` |
| `local_vs_instrumented_tests` — Local vs instrumented tests | 1 | `local_vs_instrumented_test_placement` |
| `junit` — JUnit | 0 | — |
| `test_doubles` — Test doubles | 1 | `test_doubles_001` |
| `fakes_vs_mocks` — Fakes, mocks, and stubs | 1 | `fakes_vs_mocks_interaction_coupling` |
| `viewmodel_testing` — ViewModel testing | 1 | `viewmodel_testing_001` |
| `coroutine_testing` — Coroutine testing | 1 | `test_main_dispatcher_replacement` _(deprecated: `coroutine_testing_001`)_ |
| `flow_testing` — Flow testing | 3 | `flow_testing_turbine_bounded_collection`, `flow_testing_hot_flow_never_completes`, `flow_test_background_collector_statein` |
| `run_test` — runTest | 1 | `run_test_advance_until_idle_pending_work` |
| `virtual_time` — Virtual time | 1 | `coroutine_virtual_time_delay_skipping` |
| `repository_testing` — Repository testing | 1 | `repository_test_cache_policy_fakes` |
| `room_testing` — Room/database testing | 2 | `room_testing_in_memory_database`, `room_migration_test_preserves_data` |
| `compose_ui_testing` — Compose UI testing | 1 | `compose_semantics_merged_tree` _(deprecated: `compose_ui_testing_001`)_ |
| `espresso` — Espresso awareness | 0 | — |
| `test_di` — Dependency replacement/injection in tests | 1 | `test_dependency_substitution_constructor` |
| `test_determinism` — Deterministic tests | 0 | — |
| `architecture_for_testability` — Architecture for testability | 1 | `testability_injected_time_source_seam` |
| `testing_boundaries` — What to test and where | 1 | `contract_focused_tests_resist_refactor` |
| `flaky_tests` — Flaky-test prevention/debugging | 1 | `flaky_test_shared_state_and_time` |

### Performance, Memory & Debugging

`performance` — **22 active** across 23 subtopics (18 covered, 5 empty)

| Subtopic | n | Question IDs |
|---|---:|---|
| `android_memory_model` — Android/JVM memory fundamentals | 1 | `android_heap_limit_oom_large_allocation` |
| `memory_leaks` — Memory leaks | 1 | `gc_does_not_prevent_reachable_leaks` _(deprecated: `memory_leaks_001`)_ |
| `context_leaks` — Context leaks | 1 | `context_leak_retained_activity_references` |
| `lifecycle_leaks` — Activity/Fragment/listener leaks | 1 | `lifecycle_leak_listener_unregistration` |
| `coroutine_leaks` — Coroutine lifetime leaks | 1 | `performance_coroutine_scope_leak` |
| `garbage_collection` — Garbage collection fundamentals | 3 | `art_gc_reachability`, `art_generational_gc_short_lived_objects`, `system_gc_not_memory_fix` |
| `anr` — ANRs | 1 | `anr_broadcast_receiver_work_budget` _(deprecated: `anr_001`)_ |
| `main_thread_performance` — Main-thread performance | 1 | `main_thread_ui_toolkit_single_thread` |
| `rendering_jank` — Rendering and jank | 2 | `performance_rendering_jank_causes`, `jank_vs_anr_failure_scale` |
| `startup_performance` — App startup | 2 | `performance_cold_warm_hot_start`, `startup_performance_001` |
| `lazy_initialization` — Lazy initialization | 0 | — |
| `recyclerview_performance` — RecyclerView performance | 1 | `recyclerview_diffutil_minimal_updates` |
| `compose_performance` — Compose performance | 0 | — |
| `compose_recomposition_performance` — Avoiding unnecessary recomposition | 1 | `compose_recomposition_performance_001` |
| `android_profiler` — Android Studio profiling fundamentals | 0 | — |
| `cpu_profiling` — CPU profiling | 1 | `cpu_profiling_sampling_vs_instrumented` |
| `memory_profiling` — Memory profiling | 1 | `heap_dump_path_to_gc_root` |
| `network_inspection` — Network inspection | 0 | — |
| `layout_inspector` — Layout Inspector / Compose inspection | 0 | — |
| `strictmode` — StrictMode | 1 | `performance_strictmode_role` |
| `r8` — R8 | 1 | `performance_r8_minify_reflection` |
| `baseline_profiles` — Baseline Profiles | 1 | `performance_baseline_profile_purpose` |
| `macrobenchmark` — Macrobenchmark awareness | 1 | `performance_baseline_profile_generation` |

### Security, Privacy & Permissions

`security` — **17 active** across 20 subtopics (16 covered, 4 empty)

| Subtopic | n | Question IDs |
|---|---:|---|
| `android_sandbox` — Android application sandbox | 1 | `android_sandbox_app_uid_isolation` |
| `android_permissions` — Permission model | 1 | `permission_protection_level_prompt` |
| `runtime_permissions` — Runtime permissions | 1 | `permission_rationale_state_machine` _(deprecated: `runtime_permissions_001`)_ |
| `permission_ux` — Permission-request UX | 1 | `security_permission_denied_forever` |
| `exported_components` — Exported components | 1 | `exported_components_001` |
| `intent_security` — Intent security | 1 | `exported_component_external_input_validation` |
| `pending_intent_security` — PendingIntent security | 1 | `security_pending_intent_mutability` |
| `content_provider_security` — ContentProvider security | 0 | — |
| `secure_storage` — Secure local storage | 0 | — |
| `android_keystore` — Android Keystore | 2 | `security_keystore_key_extraction`, `keystore_key_not_general_secret_storage` |
| `encryption_fundamentals` — Encryption fundamentals | 0 | — |
| `secret_management` — API keys and secret management | 1 | `apk_embedded_api_secret_recoverable` |
| `network_security` — Network security | 1 | `network_security_config_cleartext_scope` |
| `certificate_pinning` — Certificate pinning and trade-offs | 0 | — |
| `auth_token_security` — Authentication-token security | 1 | `auth_token_device_storage_risk` |
| `biometric_auth` — Biometric authentication awareness | 1 | `biometric_prompt_keystore_binding` |
| `webview_security` — WebView security | 1 | `webview_javascript_interface_exposure` |
| `sensitive_logging` — Sensitive-data logging | 1 | `sensitive_logging_001` |
| `compromised_devices` — Rooted/compromised-device limitations | 1 | `root_detection_client_trust_boundary` |
| `privacy_principles` — Privacy and data-minimization principles | 1 | `security_scoped_storage_media_access` |

### Build System, Modularization & Delivery

`build_delivery` — **17 active** across 25 subtopics (15 covered, 10 empty)

| Subtopic | n | Question IDs |
|---|---:|---|
| `gradle_fundamentals` — Gradle fundamentals | 1 | `gradle_configuration_cache_invalidation` |
| `gradle_wrapper` — Gradle Wrapper | 0 | — |
| `gradle_plugins` — Gradle plugins | 1 | `gradle_convention_plugin_build_logic` |
| `dependency_configurations` — Dependency configurations | 0 | —_(deprecated: `dependency_configurations_001`)_ |
| `version_catalogs` — Version catalogs | 1 | `version_catalogs_001` |
| `android_gradle_plugin` — Android Gradle Plugin | 0 | — |
| `kotlin_gradle_plugin` — Kotlin Gradle plugin | 1 | `ksp_vs_kapt_build_cost` |
| `build_variants` — Build variants | 1 | `build_variants_flavor_vs_type` |
| `build_types` — Build types | 0 | — |
| `product_flavors` — Product flavors | 0 | — |
| `source_sets` — Source sets | 1 | `gradle_variant_source_set_override` |
| `dependency_resolution` — Dependency resolution | 1 | `gradle_dependency_conflict_resolution` |
| `android_modules` — Application/library modules | 1 | `modularization_large_app_module_cost` |
| `feature_modularization` — Feature modularization | 1 | `feature_vs_layer_module_tradeoff` |
| `module_dependency_direction` — Module dependency direction | 1 | `feature_siblings_shared_contract_module` _(deprecated: `modularization_dependency_direction`)_ |
| `api_vs_implementation` — api vs implementation | 2 | `gradle_api_vs_implementation_leak`, `gradle_implementation_compile_avoidance` |
| `modularization_tradeoffs` — Benefits and costs of modularization | 2 | `over_modularization_tiny_module_cost`, `modularization_build_speed_not_guaranteed` |
| `apk_vs_aab` — APK vs Android App Bundle | 1 | `apk_vs_aab_001` |
| `app_signing` — Application signing | 1 | `play_app_signing_upload_key_roles` |
| `release_builds` — Debug vs release | 0 | — |
| `code_shrinking` — Code shrinking/obfuscation | 1 | `release_shrinking_responsibilities` |
| `resource_shrinking` — Resource shrinking | 0 | — |
| `app_versioning` — versionCode/versionName | 0 | — |
| `ci_fundamentals` — CI fundamentals | 0 | — |
| `cd_fundamentals` — Delivery/release pipeline fundamentals | 0 | — |

### Mobile System Design

`mobile_system_design` — **17 active** across 24 subtopics (16 covered, 8 empty)

| Subtopic | n | Question IDs |
|---|---:|---|
| `requirements_analysis` — Requirements clarification | 1 | `system_requirements_clarifying_constraint` |
| `nonfunctional_requirements` — Non-functional requirements | 1 | `system_nonfunctional_freshness_budget` |
| `system_decomposition` — Client/system decomposition | 1 | `system_client_server_rule_placement` |
| `system_data_flow` — Data flow | 0 | — |
| `local_remote_sources` — Local vs remote sources of truth | 1 | `system_design_source_of_truth_boundary` |
| `system_caching` — Caching strategy | 0 | — |
| `offline_design` — Offline behavior | 1 | `system_design_push_vs_poll` _(deprecated: `offline_design_001`)_ |
| `data_synchronization` — Data synchronization | 1 | `resilient_write_queue_dependency_order` |
| `conflict_resolution` — Conflict resolution | 1 | `system_design_conflict_resolution_policy` |
| `system_pagination` — Pagination design | 2 | `system_design_pagination_state_ownership`, `paged_cache_remote_key_transaction` |
| `system_retries` — Error and retry design | 1 | `system_retry_queue_poison_write` |
| `background_sync` — Background synchronization | 1 | `background_sync_001` |
| `push_driven_updates` — Push-driven updates | 1 | `realtime_messages_persist_then_render` |
| `session_management` — Authentication/session handling | 1 | `account_data_partition_logout` |
| `large_scale_modularization` — Modularization at scale | 0 | — |
| `client_scalability` — Client-code scalability | 0 | — |
| `mobile_resource_constraints` — Battery/network/device constraints | 0 | — |
| `observability` — Logging/observability considerations | 1 | `system_design_observability_signals` |
| `feature_flags` — Feature flags | 1 | `feature_flag_offline_default_value` |
| `backward_compatibility` — Backward compatibility | 0 | — |
| `api_evolution` — API evolution | 1 | `system_design_api_backward_compatibility` |
| `rollout_strategy` — Rollout and migrations | 1 | `system_design_feature_flag_rollout` |
| `system_security` — Security/privacy in system design | 0 | — |
| `system_testing_strategy` — Testing strategy for designed systems | 0 | — |

### Kotlin Multiplatform & Compose Multiplatform

`kmp` — **16 active** across 27 subtopics (15 covered, 12 empty)

| Subtopic | n | Question IDs |
|---|---:|---|
| `kmp_fundamentals` — Kotlin Multiplatform fundamentals | 1 | `kmp_vs_compose_multiplatform_scope` |
| `kmp_targets` — Targets | 1 | `kmp_ios_target_framework_output` |
| `kmp_source_sets` — Source sets | 2 | `kmp_source_set_hierarchy_resolution`, `kmp_common_vs_platform_test_placement` |
| `commonmain` — commonMain | 1 | `commonmain_platform_api_availability` |
| `kmp_dependency_hierarchy` — Source-set/dependency hierarchy | 0 | — |
| `shared_vs_platform_code` — What to share vs keep platform-specific | 1 | `kmp_swift_interop_suspend_flow` _(deprecated: `shared_vs_platform_code_001`, `kmp_shared_ui_platform_experience_tradeoff`)_ |
| `expect_actual` — expect/actual | 1 | `expect_actual_001` |
| `kmp_platform_apis` — Accessing platform APIs | 1 | `kmp_actual_typealias_platform_type` |
| `kotlin_native` — Kotlin/Native awareness | 1 | `kmp_native_memory_model` |
| `kmp_architecture` — KMP architecture | 1 | `kmp_shared_layer_selection` |
| `shared_domain` — Shared domain logic | 0 | — |
| `shared_data` — Shared data layer | 0 | — |
| `shared_presentation` — Shared presentation logic | 0 | — |
| `kmp_dependency_inversion` — Cross-platform dependency inversion | 1 | `kmp_expect_actual_vs_interface` |
| `platform_implementations` — Platform-specific implementations | 0 | — |
| `compose_multiplatform` — Compose Multiplatform fundamentals | 1 | `kmp_compose_multiplatform_vs_jetpack` |
| `compose_multiplatform_resources` — Compose Multiplatform Resources | 1 | `compose_multiplatform_generated_resources` |
| `kmp_lifecycle_viewmodel` — Multiplatform Lifecycle/ViewModel | 1 | `kmp_shared_viewmodel_owner_platform` |
| `kmp_navigation` — Multiplatform navigation | 0 | — |
| `platform_ui_interop` — Platform UI interoperability | 0 | — |
| `kmp_library_selection` — Evaluating KMP library compatibility | 1 | `kmp_android_library_not_multiplatform` |
| `kotlinx_serialization_kmp` — kotlinx.serialization in KMP | 0 | — |
| `ktor_kmp` — Ktor in KMP | 0 | — |
| `room_kmp` — Room KMP | 0 | — |
| `sqldelight` — SQLDelight | 0 | — |
| `koin_kmp` — Koin in KMP | 0 | — |
| `kmp_tradeoffs` — Practical KMP trade-offs | 1 | `kmp_tradeoff_when_not_to_share` |
