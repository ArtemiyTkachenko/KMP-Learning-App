# Android Platform & Application Model — Initial Learning Blueprint

## Purpose

This document defines the initial learning program for the `android_platform` Topic
(**Android Platform & Application Model**) before any production Lessons are authored.

It follows `docs/content/learning-content-authoring.md`: map the complete interview-relevant
subject first, establish dependencies and ownership, classify material as Teach / Bridge /
Reference / Exclude, review the existing assessment bank semantically, and only then write
learner-facing content.

This is a **plan, not production lesson content**.

Home Topic: `android_platform`.

Initial scope:

- 3 Learning Units
- 7 planned Lessons
- all 8 existing ACTIVE Android Platform Subtopics reviewed
- all 16 existing ACTIVE Android Platform Questions reviewed semantically
- explicit boundaries against Lifecycle/Navigation, Background Work, Security, Performance,
  Coroutines/Flow, Local Data, and Build/Delivery
- Teach / Bridge / Reference / Exclude decisions
- taxonomy gaps recorded rather than hidden
- assessment gaps recorded for a later question-authoring pass

The initial course is intentionally written for an experienced Android engineer preparing
for Senior Android interviews. It is not an Android API catalogue and it is not a beginner
"what is an Activity?" course.

---

# The Learner This Subject Is Written For

The learner already builds Android applications and recognizes Activities, Intents,
Context, resources, and the main thread. The gap this Topic closes is the **system model
under those APIs**.

The learner should finish the initial path able to reason through questions such as:

- What exactly disappears when Android kills the app process?
- Why can the system start a component even when no Activity in the app is currently open?
- How does a component affect the importance of the process that hosts it?
- What does declaring a component in the manifest actually tell the system?
- Why is an exported component a cross-application boundary?
- Why is retaining an Activity Context dangerous while retaining Application Context may
  be appropriate?
- Why does the same resource ID resolve to different values on different devices or after a
  configuration change?
- What does an implicit Intent actually ask the package manager to resolve?
- Why does posting to a main-thread Handler not move work off the main thread?
- Why does a remote Binder method run under a different threading model from a normal
  in-process method call?
- What data crosses a process boundary, and what remains only an object in the caller's
  address space?

The course should make those answers follow from a model of Android as a **component-based,
process-managed operating-system environment**, rather than from memorized API trivia.

---

# Editorial Position

## The central model

The subject can be organized around four recurring questions:

1. **Who owns the process?**
   The system starts and reclaims application processes according to active components,
   dependencies, and resource pressure. App code does not own an immortal process.

2. **How does the system enter the app?**
   Android components are operating-system-visible entry points. Their declarations and
   exposure rules are part of the app's public surface.

3. **What environment is this code running in?**
   Context, resources, configuration, theme, thread, and process are observable parts of an
   Android call's environment.

4. **Did this call cross a process boundary?**
   Local object calls share memory. Binder IPC crosses address spaces and therefore changes
   data representation, threading, failure, and security assumptions.

Every Lesson should strengthen one or more of these questions.

---

# What This Topic Owns

This Topic owns the Android runtime/platform contracts behind:

- application processes and process importance;
- Android's four core application component types;
- component declaration and exposure in the manifest;
- Context as access to application/environment services and resources;
- explicit/implicit Intents and intent-filter resolution;
- resource IDs, alternative resources, and qualifier selection;
- the main thread, Looper, MessageQueue, and Handler model;
- Binder IPC fundamentals;
- AIDL as one mechanism for a typed cross-process Binder interface;
- ContentProvider as a system-facing data component and URI-based access boundary at a
  platform-fundamentals level.

---

# What This Topic Does Not Own

Several neighboring Topics intentionally go deeper.

## Lifecycle, State & Navigation

Owns Activity/Fragment lifecycles, configuration recreation, process-death restoration,
ViewModel lifetime, navigation/task-history reasoning, deep links as navigation, and Back.
This Platform Topic may explain why process death removes memory and how Activities are
system components, but it must not re-teach state restoration or navigation.

## Background Work & OS Constraints

Owns background execution restrictions, started/bound service API selection in depth,
foreground services, WorkManager, Doze/App Standby, AlarmManager, and long-running work.
This Topic teaches what a Service component is and how a bound remote service can be an IPC
endpoint. It does not teach which background-work API a product requirement should use.

## Security, Privacy & Permissions

Owns permission model, exported-component security, intent security, PendingIntent
security, ContentProvider security, caller validation, and sandbox/security boundaries.
This Topic teaches that exported/IPC surfaces are external boundaries and shows the
mechanics required by existing questions. It does not become a security curriculum.

## Performance, Memory & Debugging

Owns ANRs in depth, rendering/jank, startup performance, memory leaks, profiling, and
StrictMode. This Topic teaches why the main thread must stay responsive and why Context
lifetime matters. It does not become a profiling/performance course.

## Coroutines / Flow

Owns dispatchers, suspension, structured concurrency, and coroutine thread switching.
This Topic teaches the platform event loop that `Dispatchers.Main` ultimately targets.

## Local Data

Owns storage mechanisms and databases. This Topic mentions ContentProvider as a platform
component and URI namespace, not as a replacement for Room/database design.

## Build System, Modularization & Delivery

Owns manifest merging, build variants, source sets, packaging, and resource shrinking.
This Topic teaches the runtime meaning of manifest/resource declarations, not Gradle merge
mechanics.

---

# Initial Curriculum

## Unit 1 — Processes, Components and Android Environment

**Purpose:** establish the system-managed runtime model before teaching messages, resource
selection, or IPC.

### L1.1 — Your App Is Not Its Process

**Stable Lesson ID:** `lesson_android_process_model_and_importance`

**Objective:** explain what an Android application process represents, why it can disappear,
and how active components influence process importance.

**Core**

- An Android app normally runs its components in an application process created by the
  system when needed.
- The process owns the app's heap, static fields, ordinary singletons, threads, and other
  in-memory objects.
- Process lifetime is not application-data lifetime.
- Android can reclaim a process when its current importance is low enough and the system
  needs resources.
- Component state contributes to process importance.
- A process may remain cached after its components stop being active; cached is not a
  durability guarantee.
- When a process is removed, every ordinary in-memory object in it disappears together.
- A later component launch can create a fresh process and fresh application objects.

**Practical**

- Why a singleton/cache cannot be the only copy of user progress that must survive.
- Distinguish "component destroyed" from "process removed".
- Foreground/visible/service/cached importance as a practical hierarchy rather than a list
  to memorize.
- A currently executing BroadcastReceiver can temporarily make its process important.
- A foreground service raises process importance, but Background Work owns foreground
  service policy.
- A bound dependency can influence how the system ranks the server process.
- `Application` is one process-scoped object, not durable storage.

**Senior**

- Process importance follows the most important active component/dependency, not which
  object graph contains the most references.
- Garbage collection and process reclamation solve different problems.
- Optional multi-process component configuration exists, but separate app processes should
  be treated as an explicit architectural boundary rather than a free optimization.

**Primary Subtopics**

- `android_process_model`

**Supporting Subtopics**

- `android_components`
- `process_death`
- `background_process_death`
- `android_memory_model`

**Existing assessment reviewed**

- `android_process_model_001`
- `process_importance_reclaim_order`
- `android_component_process_lifetime` as supporting coverage

**Assessment gap**

Current coverage establishes reclaim and importance well at Foundation depth. A later
Applied scenario can trace several in-memory assumptions across a process restart without
re-teaching saved-state restoration.

---

### L1.2 — Android Components and the Manifest Form the System Entry Surface

**Stable Lesson ID:** `lesson_android_components_and_manifest_contract`

**Objective:** distinguish the four core component types, explain how the system discovers
them, and reason about component exposure from manifest declarations.

**Core**

Android's four core component types:

- **Activity** — user-facing entry point.
- **Service** — component for work/API interaction without its own UI; Background Work owns
  service selection/restrictions in depth.
- **BroadcastReceiver** — entry point for receiving broadcast events; callback work is
  bounded/short.
- **ContentProvider** — URI-addressed data/component boundary accessed through
  `ContentResolver`.

The manifest:

- declares system-known components;
- records component metadata and capabilities;
- declares intent filters;
- controls whether many components are externally reachable;
- tells Android/build tooling about other app/runtime requirements.

Important distinctions:

- a class existing in source code does not automatically make it a system-launchable
  component;
- the system can instantiate component entry points without first navigating through the
  app's normal UI;
- components and the process hosting them have related but different lifetimes;
- runtime-registered BroadcastReceivers are an exception to "all receivers must be
  manifest-declared".

**Practical**

- `android:exported=false` for a component that should not be started by other apps.
- Android 12+ explicit `android:exported` requirement for relevant filtered components.
- Intent filters describe capabilities; they are not a replacement for an explicit
  exported/security decision.
- ContentProvider authorities name provider namespaces.
- Temporary URI permission grants can expose one specific content URI without opening the
  entire provider.
- `FileProvider` as the common file-sharing bridge.
- Bound service and provider calls may become IPC depending on process placement.
- Permissions/features/minSdk may be mentioned as manifest responsibilities, while their
  full semantics remain in Security/Build topics.

**Senior**

- Treat each exported component/provider surface as an external API boundary.
- Component entry points must not assume another component already initialized transient UI
  state.
- Manifest declaration is part of runtime behavior, not merely build metadata.

**Primary Subtopics**

- `android_components`
- `android_manifest`

**Supporting Subtopics**

- `android_intents`
- `android_ipc`
- `exported_components`
- `content_provider_security`
- `services`
- `bound_services`

**Existing assessment reviewed**

- `android_component_process_lifetime`
- `content_provider_uri_permission_grant`
- `android_manifest_component_exported`

**Assessment gap**

A later Applied question can combine component declaration, exposure, and one narrow
external capability without becoming a permissions/security quiz.

---

### L1.3 — Context Is an Environment Handle, Not a Lifetime-Free Utility Object

**Stable Lesson ID:** `lesson_android_context_lifetime_theme_and_services`

**Objective:** choose a Context according to lifetime and environment requirements, and
explain why two Context objects can provide observably different behavior.

**Core**

- `Context` is the Android abstraction through which code accesses resources, package
  information, files, component operations, and system services.
- A Context carries an environment, not just a method bag.
- `Application`, `Activity`, `Service`, and Context wrappers provide Context instances with
  different lifetime/theme/configuration relationships.
- Application Context normally lives for the current application process.
- Activity Context is tied to the Activity and carries its current theme/window-related
  environment.
- A View's Context commonly inherits the themed UI Context that created it.
- Retaining a short-lived Context from a longer-lived object can retain the short-lived
  owner.

**Practical**

- Long-lived repository needing app resources/system service: Application Context when a
  UI/themed Context is not required.
- Inflating a themed View: current themed UI Context, not Application Context by default.
- `ContextWrapper` does not magically make a wrapped Activity safe to retain.
- System services are retrieved through Context but may have independent system/process
  lifetimes.
- Resource lookup through Context is configuration/theme-sensitive.
- `createConfigurationContext()` is a Reference example of Context representing an
  environment.

**Senior**

- "Use Application Context everywhere" is as wrong as retaining Activity Context
  everywhere; the correct Context follows the capability/environment needed.
- A Context leak is fundamentally a lifetime-ownership problem.
- Theme/configuration-sensitive operations can be semantically wrong even when they do not
  leak memory.

**Primary Subtopics**

- `android_context`

**Supporting Subtopics**

- `android_resources`
- `context_leaks`
- `activity_lifecycle`

**Existing assessment reviewed**

- `android_context_001`
- `android_context_theme_inflation`

**Assessment position**

Existing Applied coverage is strong. No expansion is required unless a genuinely different
configuration/service-lifetime scenario appears.

---

## Unit 2 — Runtime Selection and Dispatch

**Purpose:** teach how Android selects targets/resources and schedules work inside one
process.

### L2.1 — Intents Describe Work; Resolution Chooses a Component

**Stable Lesson ID:** `lesson_android_intents_filters_and_resolution`

**Objective:** predict how explicit and implicit Intents reach components and explain
intent-filter matching as system resolution rather than as application routing syntax.

**Core**

- An Intent is a message/request used to activate supported component types or communicate
  an action/data request.
- **Explicit Intent** identifies a target component.
- **Implicit Intent** describes action/data/categories and asks the system to find a
  compatible component.
- Intent filters declare capabilities of components for implicit resolution.
- Resolution tests action, category, and data compatibility.
- For activity resolution, `CATEGORY_DEFAULT` matters for normal implicit
  `startActivity()` matching.
- Multiple compatible activities can lead to user/system choice.
- Extras carry additional data but do not define filter matching the same way.

**Practical**

- Share/view/open-style implicit Intents.
- Explicit Intents for known internal components.
- Explicit service targeting; implicit service startup/binding is not the modern safe model.
- Data URI/mime-type matching.
- Difference between "the filter matched" and "the receiver may trust the supplied data".
- PackageManager query/resolve APIs as Reference.
- Activity Result APIs may be mentioned as a caller-result mechanism, but are not the
  teaching center.
- Deep-link navigation remains owned by Lifecycle/Navigation.

**Senior**

- Intent resolution is capability discovery, not authorization.
- External Intent fields are untrusted input at exported boundaries.
- An explicit Intent can still cross app boundaries if it names an exported component in
  another package.
- Package visibility is Reference unless a concrete scenario needs it.

**Primary Subtopics**

- `android_intents`

**Supporting Subtopics**

- `android_manifest`
- `android_components`
- `intent_security`
- `deep_links`

**Existing assessment reviewed**

- `android_intents_001`
- `android_intent_filter_matching`

**Assessment gap**

Add at most one Applied resolution scenario where action/data/category constraints
interact, or where explicit versus implicit follows from a concrete requirement.

---

### L2.2 — Resources Are Selected From the Current Configuration

**Stable Lesson ID:** `lesson_android_resources_qualifiers_and_selection`

**Objective:** reason from a resource reference plus current configuration to which
resource Android returns, without treating qualifiers as hard-coded branches.

**Core**

- Android compiles application resources into resource identities referenced through `R`.
- The same resource ID can resolve to different concrete values/assets under different
  configurations.
- Default resources provide fallback values.
- Alternative resource directories use qualifiers such as locale, night mode, orientation,
  smallest width/width, density, and layout direction.
- Android chooses the best matching alternative according to qualifier-selection rules.
- Qualifiers are ordered/configuration constraints, not arbitrary suffix strings.
- Resource selection is distinct from whether an Activity is recreated when configuration
  changes.

**Practical**

- `values/` plus locale-specific strings.
- `values-night/` for theme-mode alternatives.
- layout/dimension choices for device-size/configuration adaptation.
- Density-specific drawables versus density-independent dimensions.
- Why referenced resources need appropriate defaults.
- Resources obtained through a Context reflect that Context's configuration/environment.
- Compose may consume Android resources, but adaptive layout strategy stays in the UI
  curriculum.
- Localization/plurals/string formatting are Reference examples, not a complete
  localization course.

**Senior**

- Resource qualifiers encode declarative configuration selection and are often more robust
  than scattered runtime device-condition branches.
- Configuration changes can cause both resource re-resolution and UI recreation; Lifecycle
  owns recreation/restoration.
- Runtime resource selection and Gradle source-set/resource merging are separate systems.

**Primary Subtopics**

- `android_resources`

**Supporting Subtopics**

- `android_context`
- `configuration_changes`
- `build_variants`
- `source_sets`

**Existing assessment reviewed**

- `android_resources_001`

**Assessment gap**

High priority. Add an Applied resource-selection scenario with competing
alternatives/default fallback, without turning it into obscure precedence trivia.

---

### L2.3 — The Main Thread Is a Looper Processing a Queue

**Stable Lesson ID:** `lesson_android_main_thread_looper_and_handler`

**Objective:** explain Android's main-thread event loop and predict whether posted work
blocks, queues, or changes thread.

**Core**

- Android creates a main/UI thread for an application process.
- The main thread services lifecycle/component callbacks, input/UI work, and queued
  messages/callbacks.
- A `Looper` repeatedly processes work from its associated `MessageQueue` on one thread.
- A `Handler` posts messages/Runnables to a particular Looper's queue.
- Posting schedules work; it does not inherently move work to another thread.
- A Handler associated with the main Looper runs callbacks on the main thread.
- Long work on the main thread delays later messages/input/render work and can contribute
  to ANR/jank.
- Android Views follow UI-thread affinity.

**Practical**

- `Handler(Looper.getMainLooper()).post { ... }` as a main-thread handoff.
- Why posting an expensive database operation to that Handler remains wrong.
- Delayed posting changes eligibility time, not execution thread.
- Background-thread result -> main-thread UI update.
- Coroutines bridge: `Dispatchers.Main` targets main-thread execution; suspending is not
  equivalent to blocking.
- StrictMode and ANR diagnostics are Reference bridges to Performance.
- `HandlerThread` is Reference, not a thread-API history lesson.

**Senior**

- A responsive main thread is fundamentally a queue-latency requirement.
- An async abstraction can still block the main thread if its actual body executes there.
- Thread affinity follows the called API contract; Binder callbacks, executors, and
  coroutine dispatchers can enter code on different threads.

**Primary Subtopics**

- `android_main_thread`

**Supporting Subtopics**

- `coroutine_dispatchers`
- `main_thread_performance`
- `anr`
- `strictmode`

**Existing assessment reviewed**

- `android_main_thread_001`
- `android_looper_message_queue`
- `handler_looper_message_queue_roles`

**Assessment gap**

Add one Applied queue/thread scenario rather than another Handler/Looper definition.

---

## Unit 3 — Cross-Process Boundaries

**Purpose:** show what changes when Android communication crosses process/address-space
boundaries.

### L3.1 — Binder IPC: Remote Calls Are Not Local Object Calls

**Stable Lesson ID:** `lesson_android_binder_ipc_and_remote_calls`

**Objective:** explain Binder as Android's IPC/RPC foundation and predict the data,
threading, and ownership consequences of crossing a process boundary.

**Core**

- Different processes have separate address spaces and heaps.
- Ordinary object references cannot simply be shared across an app-process boundary.
- Binder provides Android's core IPC/RPC mechanism.
- A remote Binder call conceptually represents/marshals arguments, sends a transaction,
  dispatches work in the remote process, and returns represented results/errors.
- AIDL defines a typed Binder interface where generated client/server stubs are useful.
- A same-process Binder/local binding can behave more like an ordinary local call; a remote
  Binder call has IPC behavior.
- Incoming remote Binder calls are dispatched on Binder-managed threads in the receiving
  process, not automatically on its main/UI thread.
- Multiple remote clients can invoke service methods concurrently.
- UI changes resulting from a Binder callback require handoff to the main thread.

**Practical**

- Bound service in another process using AIDL.
- `Parcelable` / supported AIDL representations as value transport, not shared mutable
  object identity.
- Binder transaction payload size is bounded; keep transactions reasonably small.
- ContentProvider/ContentResolver hides Binder/IPC mechanics behind a data-oriented API.
- Temporary content-URI grants are capability delegation over a provider boundary;
  Security owns authorization details.
- Distinguish a remote service callback from a normal method call in the same object graph.
- Remote process loss can make calls fail in ways local calls cannot; death-recipient and
  reconnection strategies are Reference.

**Senior**

- Remote-call APIs should be designed as process-boundary contracts, not ordinary object
  interfaces that happen to use AIDL.
- Thread safety is part of the server contract because Binder can dispatch concurrently.
- Fine-grained chatty remote APIs are costly/fragile compared with coarse operations that
  respect the IPC boundary.
- Caller identity/permission checks matter at IPC boundaries, but Security owns the full
  authorization model.

**Primary Subtopics**

- `android_ipc`

**Supporting Subtopics**

- `android_components`
- `android_main_thread`
- `bound_services`
- `content_provider_security`

**Existing assessment reviewed**

- `android_ipc_binder_contract`
- `binder_ipc_marshalling_boundary`
- `binder_thread_pool_ui_handoff`
- `content_provider_uri_permission_grant` as supporting coverage

**Assessment gap**

Current IPC coverage is comparatively strong. A future Applied scenario could distinguish
same-process versus remote binding, or coarse versus chatty API design. Do not add Binder
internals trivia.

---

# Unit Order and Why It Matters

1. **Processes, Components and Android Environment**
2. **Runtime Selection and Dispatch**
3. **Cross-Process Boundaries**

- Process before components: component lifetime/importance only makes sense once the
  process is established as system-managed and disposable.
- Components before Intents: Intents act on a component-based runtime; teaching Intent
  syntax first encourages a mere-navigation mental model.
- Components and manifest together: a standalone manifest Lesson would become an XML
  catalogue; its important runtime meaning is declaring system entry points/exposure.
- Context before resources: resource/theme lookup is part of the environment represented
  by a Context.
- Intents before IPC: Intent resolution and Binder RPC are different communication models.
- Main event loop before Binder threading: "remote Binder calls are not UI-thread calls"
  matters only once the UI thread model is clear.
- IPC last: Binder synthesizes processes, components, service/provider boundaries, and
  threading.

---

# Existing Taxonomy — Editorial Decision

| Subtopic | Decision | Home |
| --- | --- | --- |
| `android_process_model` | Teach | L1.1 |
| `android_components` | Teach | L1.2; reinforced in L3.1 |
| `android_context` | Teach | L1.3 |
| `android_intents` | Teach | L2.1 |
| `android_manifest` | Teach | L1.2 |
| `android_resources` | Teach | L2.2 |
| `android_main_thread` | Teach | L2.3 |
| `android_ipc` | Teach | L3.1 |

Every existing ACTIVE Platform Subtopic has one clear home.

---

# Complete Subject Map Beyond the Existing Taxonomy

## Teach in the initial 7 Lessons

- process importance hierarchy at practical reasoning depth;
- `Application` as a process-scoped object/Context;
- system activation of components independently of UI flow;
- component exposure as a runtime/public boundary;
- ContentProvider authority/content-URI concept;
- temporary URI-grant mechanics at platform depth;
- Context lifetime versus themed/configured environment;
- system service lookup through Context;
- Intent action/data/category matching;
- resource defaults and best-match selection;
- density/locale/night/size qualifiers;
- event-loop/queue latency model;
- Binder thread-pool dispatch;
- local versus remote Binder behavior;
- AIDL as typed Binder contract tooling;
- Parcelable/value representation across IPC.

## Bridge

- process-death restoration -> Lifecycle/Navigation
- service/foreground-service policy -> Background Work
- exported-component authorization -> Security
- provider permissions -> Security
- Intent input validation -> Security
- ANR/jank diagnosis -> Performance
- Context leak diagnosis -> Performance
- coroutine dispatchers/suspension -> Coroutines
- database/storage design -> Local Data
- manifest/resource merge mechanics -> Build/Delivery
- deep-link navigation -> Lifecycle/Navigation

## Reference / Future Expansion

1. PackageManager and package visibility
2. Application startup ordering / App Startup
3. Multi-process app architecture
4. Ordered broadcasts / broadcast restrictions
5. Activity Result APIs
6. PendingIntent
7. ContentResolver observers
8. Binder death recipients / reconnect strategies
9. Messenger
10. Direct Boot / device-protected storage
11. Configuration Contexts
12. Resource overlays

## Exclude

- exhaustive manifest attributes;
- every Intent flag;
- complete Activity launch-mode/task rules;
- full background-work/service API selection;
- foreground-service matrices;
- complete broadcast restriction history;
- permission UX;
- Binder kernel internals;
- Parcel binary-format internals;
- AIDL syntax catalogues;
- ContentProvider CRUD tutorials;
- database design;
- qualifier-precedence trivia without engineering consequence;
- Gradle manifest/resource merge mechanics;
- OEM process-killer folklore presented as Android contract;
- general Java concurrency instruction.

---

# Taxonomy Gaps

Do not invent IDs during authoring.

## Potential gap — Component exposure / system entry boundary

Current home: `android_components` + `android_manifest`.

If future assessment grows around exported entry points, URI capabilities, and system
activation, a more specific Subtopic may become useful. No immediate change required.

## Potential gap — Process importance

Current home: `android_process_model`.

Broad enough initially. Split only if creation/reclaim and importance/dependency reasoning
grow into separate substantial question groups.

## Potential gap — Resource selection

Current home: `android_resources`.

No new Subtopic needed yet, though future questions may cluster around qualifier selection,
localization, and density/size adaptation.

## Potential gap — Binder threading versus Binder contract

Current home: `android_ipc`.

Keep together initially.

---

# Semantic Review of the Existing 16 ACTIVE Questions

| Existing Question | Best home | Review |
| --- | --- | --- |
| `android_process_model_001` | L1.1 | Strong correction to "singleton = persistence". |
| `process_importance_reclaim_order` | L1.1 | Good process-importance reasoning; dense but useful. |
| `android_component_process_lifetime` | L1.1 / L1.2 | Good component-versus-process distinction. |
| `content_provider_uri_permission_grant` | L1.2 | Good Applied capability-sharing scenario; bridge Security. |
| `android_manifest_component_exported` | L1.2 | Necessary component-boundary Foundation check. |
| `android_context_001` | L1.3 | Strong Applied lifetime/retention Context question. |
| `android_context_theme_inflation` | L1.3 | Strong counterweight showing Application Context is not universal. |
| `android_intents_001` | L2.1 | Necessary explicit-vs-implicit Foundation anchor. |
| `android_intent_filter_matching` | L2.1 | Good filter contract; teach DEFAULT/category/data carefully. |
| `android_resources_001` | L2.2 | Good qualifier-purpose check but insufficient for actual selection reasoning. |
| `android_main_thread_001` | L2.3 | Good responsiveness/ANR anchor. |
| `android_looper_message_queue` | L2.3 | Useful correction to "Handler means background thread". |
| `handler_looper_message_queue_roles` | L2.3 | Good event-loop mechanics; partial overlap but distinct angle. |
| `android_ipc_binder_contract` | L3.1 | Good AIDL/Binder contract anchor. |
| `binder_ipc_marshalling_boundary` | L3.1 | Good address-space distinction. |
| `binder_thread_pool_ui_handoff` | L3.1 | Strong Applied IPC/thread-affinity question. |

---

# Assessment State

Current Android Platform bank:

- 16 ACTIVE questions
- 12 `FOUNDATION`
- 4 `APPLIED`
- 0 `ADVANCED`

The distribution does not need normalization.

The main issue is uneven scenario depth.

## Highest-priority gaps

### Process model
Potential Applied scenario tracing in-memory assumptions across process restart, without
duplicating Lifecycle restoration.

### Components + manifest
Potential Applied scenario combining system entry, declaration, and one external capability.

### Context
Already strong; no routine expansion needed.

### Intents
Current coverage is Foundation-only. Add one Applied resolution/design scenario.

### Resources
Largest Platform gap. Add one Applied best-match/default-fallback scenario.

### Main thread
Add one Applied queue/thread handoff scenario.

### IPC
Already comparatively strong. Add only if a distinct same-process-vs-remote or API-granularity
scenario is justified.

---

# Stable Identities for Run 2

## Units

| Order | Stable Unit ID | Title |
| ---: | --- | --- |
| 1 | `unit_android_processes_components_and_environment` | Processes, Components and Android Environment |
| 2 | `unit_android_runtime_selection_and_dispatch` | Runtime Selection and Dispatch |
| 3 | `unit_android_cross_process_boundaries` | Cross-Process Boundaries |

## Lessons

| Order | Stable Lesson ID | Title |
| ---: | --- | --- |
| 1 | `lesson_android_process_model_and_importance` | Your App Is Not Its Process |
| 2 | `lesson_android_components_and_manifest_contract` | Android Components and the Manifest Form the System Entry Surface |
| 3 | `lesson_android_context_lifetime_theme_and_services` | Context Is an Environment Handle, Not a Lifetime-Free Utility Object |
| 4 | `lesson_android_intents_filters_and_resolution` | Intents Describe Work; Resolution Chooses a Component |
| 5 | `lesson_android_resources_qualifiers_and_selection` | Resources Are Selected From the Current Configuration |
| 6 | `lesson_android_main_thread_looper_and_handler` | The Main Thread Is a Looper Processing a Queue |
| 7 | `lesson_android_binder_ipc_and_remote_calls` | Binder IPC: Remote Calls Are Not Local Object Calls |

---

# Authoritative Source Families for Run 2

Prefer current official Android documentation/API references:

- Application fundamentals / app components
- Processes and threads overview
- App manifest overview
- Activity / Service / BroadcastReceiver / ContentProvider references as needed
- Content provider basics and provider manifest element
- FileProvider / secure file-sharing documentation
- Context API reference
- App resources overview / alternative resources
- localization/resource-selection documentation
- Intents and intent filters
- PackageManager query/resolve APIs where necessary
- Looper / Handler / MessageQueue references
- AIDL documentation
- bound services / Binder docs
- Parcelable / Parcel references where needed

## Freshness requirements

Re-check current documentation during authoring for:

- component-export requirements;
- implicit-service restrictions;
- package visibility if mentioned;
- file-sharing/URI-grant guidance;
- AIDL/Binder guidance;
- any API-level-specific behavior.

---

# Cross-Topic Boundary Register

| Concept | This Topic teaches | Deeper owner |
| --- | --- | --- |
| process death | memory/process removal | Lifecycle/Navigation: restoration |
| services | component role / remote endpoint | Background Work |
| foreground service | process-importance bridge | Background Work |
| exported components | reachability | Security |
| provider URI grants | capability mechanics | Security |
| Intent input | resolution | Security: trust/validation |
| Activity task stack | bounded bridge only | Lifecycle/Navigation |
| deep links | Intent foundation | Lifecycle/Navigation |
| Context leaks | lifetime reasoning | Performance |
| ANR/jank | why queue responsiveness matters | Performance |
| `Dispatchers.Main` | bridge to main thread | Coroutines |
| databases/files | provider may front data | Local Data |
| manifest/resource merging | runtime result only | Build/Delivery |

---

# Run 2 Workflow

Use the same multi-agent **author -> independent reviewer -> editor/correction** process as
the completed Kotlin and Lifecycle programs.

## Suggested batches

### Batch 1
- Process Model and Importance
- Components and Manifest
- Context

### Batch 2
- Intents and Resolution
- Resources and Qualifiers
- Main Thread / Looper / Handler

### Batch 3
- Binder IPC

Review/correct after every batch, then run an integrated Topic review before assessment
expansion.

## Author responsibilities

- read the blueprint and current authoring contract;
- inspect all ACTIVE questions for Primary Subtopics;
- verify current Android behavior against official sources;
- explain runtime models before API names;
- preserve cross-topic boundaries;
- distinguish process from component lifetime;
- distinguish reachability/export from authorization;
- distinguish Context lifetime from Context semantics;
- distinguish Intent resolution from navigation policy;
- distinguish posting from offloading;
- distinguish same-process calls from Binder IPC;
- keep learner-facing prose project-agnostic.

## Reviewer responsibilities

Check:

- process importance/reclamation;
- component discovery/activation;
- manifest/export semantics;
- Context lifetime/theme behavior;
- Intent-filter matching;
- resource-selection semantics;
- Looper/Handler execution thread;
- Binder thread-pool behavior;
- IPC address-space/marshalling claims;
- boundary discipline with Lifecycle, Background Work, Security, Performance, Coroutines,
  Local Data, and Build/Delivery.

Reject manifest catalogues, Android-history trivia, vague "the OS handles it" explanations,
unsupported absolutes, and Senior sections based on obscure APIs rather than mechanisms.

---

# Integrated Review

After all seven Lessons are stable, check for:

- inconsistent meaning of application/process/component;
- accidental claim that `Application` is durable app state;
- duplicated component/Intent explanations;
- Context advice reduced to "always Application Context";
- resource/configuration behavior contradicting Lifecycle material;
- main-thread material duplicating Coroutines/Performance;
- Binder described as shared-memory object access;
- incorrect Binder threading claims;
- ContentProvider reduced to "database wrapper";
- security implications stated without authorization caveats.

---

# Assessment Expansion

Only after the seven Lessons are stable should new Platform questions be considered.

Strong candidates:

1. resource qualifier/default selection;
2. Applied Intent resolution;
3. Applied main-thread queue/handoff;
4. component/manifest external entry;
5. optional same-process versus remote Binder scenario.

Do not add questions merely to increase count and do not force an Advanced category.

---

# Completion Criteria

The Topic is complete for the initial phase only when:

1. all 7 planned Lessons are authored;
2. every Lesson receives independent technical/editorial review and correction;
3. all 16 existing ACTIVE questions have semantic teaching support;
4. the integrated Topic review passes;
5. justified assessment gaps are addressed;
6. new questions receive independent review;
7. generated coverage is current;
8. relevant validation/tests pass;
9. adjacent Topics are not silently duplicated;
10. any blueprint deviation is explicitly justified.

---

# Decision Summary

The initial Android Platform & Application Model course should contain **7 Lessons across
3 Units**:

1. process lifetime and importance;
2. system component entry points + manifest contract;
3. Context lifetime/environment;
4. Intent resolution;
5. resource selection;
6. main-thread event loop;
7. Binder IPC.

A separate manifest Lesson would likely become an attribute catalogue, so manifest belongs
with component entry points. Combining Context with resources would lose the
lifetime/theme distinction already tested by the question bank. Combining main-thread
dispatch with IPC would hide the fact that Binder has a different incoming threading model.

The highest-value future assessment gaps are specific scenarios in:

- resource selection;
- Intent resolution;
- main-thread scheduling;
- component/manifest entry boundaries.

IPC and Context already have comparatively useful Applied coverage.

---

# Implementation Record — 2026-10-08

The initial program implements all three Units and seven Lessons above with their
proposed stable identities, titles, Primary/Supporting mappings and ordered
CORE / PRACTICAL / SENIOR sections. No structural or ownership deviation was needed.
The program follows Lifecycle learning and precedes the optional KMP tail.

Three separate author → independent reviewer → correction-editor batches (3+3+1)
were followed by a fresh integrated review. That review confirmed semantic teaching
support for all 16 preexisting ACTIVE Platform questions. The Binder correction
makes each UI attachment/server session own a fresh callback holder: a retired
holder stays inactive, preventing old queued callbacks from becoming valid when a
new attachment starts. No other Topic's lesson content was changed.

Four independently reviewed assessment additions address distinct gaps:

| Question ID | Primary Subtopic | Final level | Reasoning assessed |
| --- | --- | --- | --- |
| `android_resource_partial_translation_fallback` | `android_resources` | FOUNDATION | Predict per-entry fallback with a partial translation |
| `android_intent_resolution_action_category_type` | `android_intents` | FOUNDATION | Select one complete filter matching action, carried categories and type |
| `android_main_worker_result_without_wait` | `android_main_thread` | APPLIED | Repair main-thread waiting while preserving worker execution and UI affinity |
| `android_component_independent_entry_dependency` | `android_components` | APPLIED | Place reloadable initialization where independent external entry can obtain it |

The resource and Intent candidates are classified FOUNDATION because their final
options require direct documented-contract knowledge. They do not fully close the
blueprint's aspirational Applied resource/Intent scenario depth; no extra complexity
or question was added merely to reach that level. Context and IPC retain their
existing stronger coverage, including remote Binder UI handoff; no optional Binder
question was necessary. Existing questions and taxonomy remain unchanged.

Final question review lowered the Intent level and removed an implementation-option
wording cue in the component question. Its final bytes received a separate
revalidation; the scoped audit record and generated coverage track accepted output.
Local Gradle execution was blocked before tests by wrapper-download network
unavailability. Production Kotlin codecs/validators and deterministic tooling were
run locally; exact-head repository CI results are recorded in the draft PR. These
checks do not claim Android device execution of the illustrative lesson snippets.
