# Lifecycle, State & Navigation — Initial Learning Blueprint

## Purpose

This document defines the initial learning program for the `lifecycle_navigation` Topic
(**Lifecycle, State & Navigation**) before any production Lessons are authored.

It follows `docs/content/learning-content-authoring.md`: map the interview-relevant subject
first, establish conceptual dependencies and ownership, classify material as Teach /
Bridge / Reference / Exclude, review the existing assessment bank semantically, and only
then write learner-facing content.

This is a **plan, not production lesson content**.

Home Topic: `lifecycle_navigation`.

Initial scope:

- 4 Learning Units
- 10 planned Lessons
- all 11 existing ACTIVE Lifecycle / Navigation Subtopics reviewed
- all 23 existing ACTIVE Lifecycle / Navigation Questions reviewed semantically
- explicit boundaries against Compose, Coroutines / Flow, Architecture, Android Platform,
  and KMP learning material
- explicit Reference and Exclude decisions
- taxonomy gaps recorded rather than hidden
- assessment gaps recorded for a later question-authoring pass

The course is written for an experienced Android engineer preparing for Senior Android
interviews. It assumes ordinary Android and Kotlin familiarity. It is not a callback
memorization course.

---

# The Learner This Subject Is Written For

The learner has seen lifecycle callbacks, ViewModels, saved state, and Navigation APIs in
real Android code. They can usually make a screen work. The remaining gap is explaining
**why a particular object still exists, why another one was recreated, who owns a piece of
state, and what Back or process recreation actually does to it**.

A strong Senior answer in this subject should be able to reason through questions such as:

- Is this Activity visible, interactive, stopped, or being destroyed?
- Which resource should follow visibility, and which should follow focus?
- Does this Fragment still exist even though its View has already been destroyed?
- Which `LifecycleOwner` should this UI observation follow?
- Is this recreation a configuration change, process death, or permanent dismissal?
- Which values survive because memory survived, which survive because state was serialized,
  and which must be reloaded from durable storage?
- What exactly owns this ViewModel, and what event clears it?
- What is a navigation back-stack entry besides "a route on a list"?
- Is the app's navigation stack managed by a controller, or is the stack itself app-owned
  state?
- What history should exist when the app is entered through a deep link?
- Is Back being handled by changing navigation state, or intercepted as a custom gesture?
- Can the system preview the destination of a predictive-back gesture before commit?

The course should make these questions answerable from a lifetime model, not from a
memorized callback sequence.

---

# Editorial Position

## The central model

The subject can be reduced to four recurring questions:

1. **What is the owner?**
   Activity, Fragment, Fragment View, navigation entry, graph entry, task, or app-owned
   navigation state.

2. **What ends that owner's lifetime?**
   Temporary loss of focus, invisibility, configuration recreation, stack pop, task
   dismissal, or process death are not equivalent events.

3. **Where does the state actually live?**
   Ordinary memory, a retained owner such as a ViewModelStore, saved state held for
   reconstruction, or durable storage.

4. **What history should Back expose?**
   Back behavior follows navigation/task state. A custom Back callback is not a replacement
   for modeling the stack correctly.

Every Lesson should strengthen one or more of those questions.

## What this course owns

This Topic owns Android's **lifetime and restoration mechanics**:

- Activity and Fragment lifecycle behavior;
- the separate Fragment View lifecycle;
- `LifecycleOwner` and lifecycle-aware work;
- configuration recreation;
- process death and recreation;
- ViewModel lifetime and `ViewModelStoreOwner`;
- saved-state mechanisms and limits;
- navigation back-stack and entry lifetime;
- Navigation 2 versus Navigation 3 ownership models;
- deep-link entry into navigation;
- Back and predictive-back behavior.

## What this course deliberately does not own

Several adjacent Topics already teach related decisions:

- **Architecture** owns what a screen state holder is responsible for, state ownership as an
  architectural decision, MVVM/MVI, repository boundaries, and business-state design.
  This Topic teaches *how long Android owners live*.
- **Compose** owns `remember`, `rememberSaveable`, Compose state ownership, side effects,
  and lifecycle-aware Compose collection. This Topic explains the Android lifetime those
  APIs integrate with.
- **Coroutines / Flow** owns structured concurrency, cancellation, Flow, `StateFlow`, and
  collection mechanics. This Topic explains when Android lifecycle APIs start and stop that
  work.
- **Android Platform** owns processes, Intents, manifest behavior, components, and IPC as
  platform subjects. This Topic bridges only the parts needed to reason about Activity
  tasks, launch modes, and external navigation.
- **KMP** owns how lifecycle and ViewModel semantics map across Android, iOS, desktop, and
  other hosts. This core Topic remains Android-specific.

Do not duplicate those curricula simply because their APIs appear in examples.

---

# Unit Order

1. **Component Lifetimes and Visibility**
2. **Recreation, Retention and Restoration**
3. **Navigation State and Destination Lifetime**
4. **External Entry and Back Navigation**

The order is deliberate:

- **Component lifetime before state survival.** "Does this state survive?" is meaningless
  until the learner can name what is being destroyed.
- **Fragment View lifetime after Activity lifetime.** The second lifecycle is easiest to
  understand once a single component lifecycle is already secure.
- **Lifecycle-aware APIs after both owners exist.** The choice of owner is the point; an API
  name without that distinction becomes a recipe.
- **Recreation before ViewModel.** A ViewModel's value is that its owner/store survives a
  particular kind of recreation. Teaching ViewModel first encourages the false model
  "ViewModel persists state".
- **ViewModel before saved state.** The learner should first see in-memory retention and
  only then the stronger reconstruction guarantee of saved state.
- **Restoration before navigation.** Every navigation entry is itself an owner with
  lifecycle and saved-state implications.
- **Navigation fundamentals before Navigation 2 versus 3.** The libraries are two
  implementations of a navigation-state problem. The stack model should exist before
  either API is compared.
- **Internal navigation before deep links.** A deep link is an alternate entry path into a
  navigation model, not a separate routing universe.
- **Back last.** Correct Back behavior is synthesis: owner lifetime, current stack,
  destination history, external entry, and system gesture behavior all meet there.

---

# Unit 1 — Component Lifetimes and Visibility

**Purpose:** establish the Android owners whose lifetimes later state, work, and navigation
attach to.

**Prerequisites:** basic Android component familiarity. No lifecycle callback sequence is
assumed beyond recognition of Activity and Fragment.

## L1.1 — Activity Lifecycle: Visible, Interactive and Gone

**Stable Lesson ID:** `lesson_activity_lifecycle_visibility_and_work`

**Objective:** choose lifecycle boundaries from what the user can see and interact with,
and explain what work belongs around those boundaries.

### Core

- `onCreate`, `onStart`, `onResume`, `onPause`, `onStop`, and `onDestroy` as transitions,
  not six independent places to put arbitrary work.
- CREATED / STARTED / RESUMED as useful state concepts.
- `onStart` / `onStop` track visibility more closely.
- `onResume` / `onPause` track foreground interaction/focus more closely.
- A partially covered Activity can be paused while still visible.
- Lifecycle callbacks run on the main thread.
- `onPause` must stay short; it is not a durable-work boundary.
- `onDestroy` is not a guaranteed "final cleanup before the process disappears" callback.

### Practical

- Camera/video/location-style resources whose acquisition should follow actual visibility or
  interaction requirements.
- Why a dialog-themed or translucent Activity can pause another Activity without stopping
  it.
- Registration/unregistration symmetry based on the resource's required lifetime.
- Avoiding database/network work in `onPause`.
- Distinguishing "Activity instance destroyed" from "process killed".
- Brief bridge to `LifecycleOwner` rather than implementing every callback manually.

### Senior

- Choose the narrowest lifecycle state that still satisfies the feature requirement:
  RESUMED for genuinely interactive/exclusive work; STARTED for work that should continue
  while visible.
- Callback order is less important than naming the invariant each resource requires.
- Cleanup cannot depend on `onDestroy` for correctness because process termination can end
  memory without giving application code a final callback.

**Primary Subtopics**

- `activity_lifecycle`

**Supporting Subtopics**

- `lifecycle_aware_apis`
- `configuration_changes`
- `process_death`
- `android_main_thread` (`android_platform`)

**Existing assessment reviewed**

- `activity_lifecycle_001`
- `activity_on_pause_vs_on_stop_visibility`

**Assessment gap**

Current coverage is Foundation-only. A later Applied question should require selecting a
visibility/focus boundary for a concrete resource rather than recalling callback names.

---

## L1.2 — A Fragment Has an Instance Lifecycle and a View Lifecycle

**Stable Lesson ID:** `lesson_fragment_and_view_lifecycles`

**Objective:** reason separately about the lifetime of a Fragment object and the lifetime
of the View hierarchy it currently owns.

### Core

- A Fragment is a `LifecycleOwner`.
- A Fragment can outlive its current View hierarchy.
- Creating and destroying the Fragment View are separate from creating and destroying the
  Fragment instance.
- A Fragment on the back stack can keep its instance while `onDestroyView()` has already
  torn down the View tree.
- `viewLifecycleOwner` represents the current View lifecycle.
- View-bound references must not outlive the View.

### Practical

- Clearing ViewBinding references in `onDestroyView`.
- Binding UI observation to `viewLifecycleOwner`, not the Fragment instance, when the
  observer touches Views.
- A Fragment-scoped ViewModel can outlive the current Fragment View.
- Why callbacks arriving after `onDestroyView()` are dangerous when they retain or mutate
  old Views.
- Recreating the View when navigating back to a Fragment whose instance remained.

### Senior

- The "Fragment exists" fact is not sufficient to justify touching its View.
- Lifetime mismatch is the root cause behind classic Fragment binding leaks and stale UI
  updates.
- Max-lifecycle and FragmentManager details may alter current lifecycle state, but the
  instance/View distinction remains the essential reasoning model.

**Primary Subtopics**

- `fragment_lifecycle`

**Supporting Subtopics**

- `lifecycle_aware_apis`
- `viewmodel_lifecycle`
- `navigation_fundamentals`

**Existing assessment reviewed**

- `fragment_view_lifecycle_collection`
- `fragment_back_stack_view_destroyed_binding`

**Assessment gap**

Add an Applied failure scenario where asynchronous or observable work completes after
`onDestroyView()` and the candidate must choose the correct owner/cleanup boundary.

---

## L1.3 — Lifecycle-Aware Work: Follow the Owner, Not the Screen Name

**Stable Lesson ID:** `lesson_lifecycle_aware_work_and_collection`

**Objective:** choose the correct `LifecycleOwner` and lifecycle state for work that should
start, stop, cancel, or restart with UI availability.

### Core

- `Lifecycle`, `LifecycleOwner`, and lifecycle states as a reusable abstraction over
  component callbacks.
- `lifecycleScope` is cancelled when its owner is destroyed.
- `repeatOnLifecycle` starts the supplied block when the owner reaches the requested state,
  cancels that block when it falls below the state, and starts it again when the state is
  reached again.
- For Fragment View updates, use the View's owner.
- Restarting a collection is not the same as keeping one coroutine suspended forever.

### Practical

- Collecting a Flow while a Fragment View is at least STARTED.
- Choosing STARTED versus RESUMED from feature requirements rather than habit.
- Multiple Flow collections inside `repeatOnLifecycle` need independent child coroutines
  when they must run concurrently.
- `flowWithLifecycle` as a convenient single-Flow alternative.
- Compose's `collectAsStateWithLifecycle` is a Bridge to existing Compose material, not a
  second Compose lesson.

### Senior

- Lifetime-aware APIs solve *when work should be active*. They do not decide whether the
  underlying data source itself should be hot, cold, cached, shared, or durable.
- Correctness depends first on selecting the right owner, then the right minimum active
  state.
- A Fragment lifecycle owner and its View lifecycle owner can yield observably different
  cancellation points.

**Primary Subtopics**

- `lifecycle_aware_apis`

**Supporting Subtopics**

- `activity_lifecycle`
- `fragment_lifecycle`
- `coroutine_scope` (`async_reactive`)
- `flow_collection` (`async_reactive`)

**Existing assessment reviewed**

- `lifecycle_repeat_on_lifecycle`

**Assessment gap**

The current single Applied question is useful but narrow. Future coverage can test owner
selection or STARTED-versus-RESUMED behavior without duplicating coroutine/Flow questions.

---

# Unit 2 — Recreation, Retention and Restoration

**Purpose:** separate four mechanisms that developers often collapse into one word:
recreation, retained in-memory state, saved reconstruction state, and durable persistence.

**Prerequisites:** Unit 1.

## L2.1 — Configuration Change, Process Death and Permanent Dismissal

**Stable Lesson ID:** `lesson_recreation_configuration_process_and_dismissal`

**Objective:** predict what survives each major destruction path and choose a test that
actually reproduces the failure being investigated.

### Core

Distinguish at least these cases:

1. **Configuration recreation**
   - Activity instance and Views are recreated by default.
   - The app process normally remains.
   - In-memory process objects can still exist.
   - retained owner state such as a ViewModelStore can be handed to the replacement owner.

2. **System-initiated process death with task restoration**
   - every in-process object disappears;
   - a later recreation constructs fresh objects;
   - saved-state records and durable data may reconstruct the visible state.

3. **Permanent screen/task dismissal**
   - finishing/popping an owner ends its retained owner state;
   - saved state associated with a removed task/owner is not durable application storage.

- Handling a configuration change manually via `android:configChanges` changes which
  callbacks/recreation occur; it should not be presented as a default state-management
  strategy.
- Process death does not require a clean lifecycle teardown callback first.

### Practical

- Rotation as a configuration-recreation example.
- Why "Don't keep activities" stresses Activity recreation but does not reproduce loss of
  process-wide singletons and static caches.
- Testing actual process recreation while preserving the task, such as terminating the app
  process and returning through Recents using supported tooling.
- Recognizing bugs that pass rotation tests because a singleton survived but fail after
  real process death.
- Separating "restore UI position" from "persist business data".

### Senior

- A test is only useful if it destroys the same owner/state layers as the production
  failure.
- The system may preserve a task's reconstruction information while killing the process
  that produced it.
- Configuration and process recreation can produce visually similar screens through
  entirely different state mechanisms.

**Primary Subtopics**

- `configuration_changes`
- `process_death`

**Supporting Subtopics**

- `activity_lifecycle`
- `viewmodel_lifecycle`
- `saved_state`
- `android_process_model` (`android_platform`)

**Existing assessment reviewed**

- `configuration_changes_001`
- `configuration_change_vs_process_recreation`
- `process_death_reproducing_restoration`

**Assessment gap**

This area is suitable for an eventual Advanced scenario only if the stem genuinely
requires tracing several state layers (for example ViewModel + saved state + singleton +
database) across a destruction path. Do not label a rotation question Advanced merely
because it mentions process death.

---

## L2.2 — ViewModel Lifetime Is the Lifetime of Its Owner

**Stable Lesson ID:** `lesson_viewmodel_owner_and_scope`

**Objective:** predict when a ViewModel is reused or cleared by identifying its
`ViewModelStoreOwner`.

### Core

- A ViewModel is stored in a `ViewModelStore` owned by a `ViewModelStoreOwner`.
- Common owners include Activity, Fragment, and navigation back-stack/graph entries.
- Configuration recreation can retain the owner's ViewModelStore and return the same
  ViewModel instance to the replacement UI owner.
- Permanent destruction of the owner clears its store and triggers ViewModel cleanup.
- Process death removes the ViewModel instance; ViewModel is not persistence.
- Scope determines which screens receive the same instance.

### Practical

- Activity-scoped versus Fragment-scoped state.
- Destination-scoped ViewModels for independent screen state.
- Navigation-graph-scoped ViewModels for temporary state shared by several destinations.
- Why application-wide scope is wrong for state whose requirement ends with a checkout or
  destination.
- Avoid references from ViewModel to View/Activity/lifecycle objects whose lifetime is
  shorter than the ViewModel.
- `SavedStateHandle` is introduced only as the reconstruction mechanism for small state;
  full treatment is L2.3.

### Senior

- "Same ViewModel class" does not imply "same ViewModel instance"; owner identity does.
- A scope should be derived from the lifetime the state/work requirement needs.
- This Lesson teaches the Android lifetime mechanism. Architecture retains ownership of
  what responsibilities belong in a ViewModel.

**Primary Subtopics**

- `viewmodel_lifecycle`

**Supporting Subtopics**

- `configuration_changes`
- `process_death`
- `navigation_fundamentals`
- `saved_state`
- `state_ownership` (`architecture`)

**Existing assessment reviewed**

- `viewmodel_destination_scope`
- `viewmodel_store_configuration_retention`
- `viewmodel_clear_owner_finish`
- `navigation_graph_viewmodel_shared_scope` as supporting navigation coverage

**Assessment position**

Existing coverage is strong across Foundation and Applied. Future questions should focus on
owner identity and competing scopes, not more "ViewModel survives rotation" recall.

---

## L2.3 — Saved State Is a Reconstruction Hint, Not a Database

**Stable Lesson ID:** `lesson_saved_state_and_reconstruction`

**Objective:** choose saved state for small reconstruction inputs and explain exactly what
guarantee it provides compared with ViewModel memory and durable storage.

### Core

- Saved instance state is for reconstructing UI after system-driven recreation.
- `onSaveInstanceState`, `SavedStateRegistry`, `SavedStateHandle`, and Compose saved-state
  APIs participate in the same broad reconstruction problem at different boundaries.
- Saved state survives configuration recreation and system-initiated process death when
  the associated task/state is restored.
- Saved state does not survive every form of user dismissal and is not durable application
  persistence.
- Store the minimum small/simple information needed to reconstruct state, such as a query,
  selected ID, or UI element state.
- Large loaded data should be reloaded from durable storage/data sources.

### Practical

- `SavedStateHandle` in a ViewModel for a small screen parameter.
- `rememberSaveable` as a Compose bridge for local UI-element state.
- ID/query in saved state, actual entity list in a database/repository.
- Large Bundles can trigger transaction/serialization problems including
  `TransactionTooLargeException`.
- Serialization/deserialization work is not free and occurs on lifecycle-sensitive paths.
- Saved-state writes are committed at the owner's saving/stopping boundary; updates made
  while stopped are not automatically durable until the owner runs through the relevant
  lifecycle again.
- State must be representable by the saving mechanism; custom objects may require an
  explicit saver/serializer/provider.

### Senior

- State restoration should reconstruct from stable inputs rather than serialize an entire
  object graph.
- Saved state, ViewModel memory, and durable storage form different lifetime guarantees;
  using all three in one screen is normal when each owns a different part of the problem.
- The correct question is "what is the smallest information that lets me recreate this UI
  state?" rather than "how can I save this object?"

**Primary Subtopics**

- `saved_state`

**Supporting Subtopics**

- `process_death`
- `configuration_changes`
- `viewmodel_lifecycle`
- `compose_state` (`android_ui`)
- `local_data` concepts as Bridge only

**Existing assessment reviewed**

- `saved_state_binder_transaction_limit`
- `saved_state_transient_inputs`
- `savedstatehandle_process_recreation`
- `remember_saveable_vs_viewmodel_ownership`

**Assessment position**

Coverage is already strong. Prefer later Applied questions that compare reconstruction
strategies under explicit constraints instead of adding more storage-size trivia.

---

# Unit 3 — Navigation State and Destination Lifetime

**Purpose:** treat navigation as state with lifetime and ownership, rather than as a series
of "go to screen" calls.

**Prerequisites:** Units 1–2.

## L3.1 — Back Stacks, Tasks and Entry Lifetimes

**Stable Lesson ID:** `lesson_navigation_back_stacks_tasks_and_entry_lifetimes`

**Objective:** distinguish navigation history, navigation declarations, Android Activity
tasks, and the lifetime of one navigation entry.

### Core

- A navigation back stack is ordered history; the top entry is current.
- A graph declares reachable destinations/relationships; it is not itself the runtime
  history.
- Pushing creates/adds a new entry; popping permanently removes an entry.
- A back-stack entry can be a lifecycle owner, saved-state owner, and ViewModelStore owner.
- State scoped to a popped entry is cleared with that owner; navigating to the same route
  later creates a new entry.
- A navigation-library back stack is not identical to Android's Activity task stack.

### Practical

- Per-destination ViewModel lifetime.
- Graph-entry ViewModel scope for a multi-screen flow.
- Why a checkout flow can share state and then clear it when the graph/flow is popped.
- Multiple instances of the same route can be distinct entries with distinct scoped state.
- Activity launch modes affect the Activity task stack, not an arbitrary Compose/Fragment
  navigation stack.
- Bridge the important `singleTask` behavior because it is already assessed:
  reusing an existing task instance, delivering a new Intent, and clearing task activities
  above it. Do not expand into an exhaustive launch-mode/Intent-flag matrix.

### Senior

- Route identity, entry identity, and task identity are separate concepts.
- Scoping state to "the screen type" is imprecise; state is scoped to a concrete owner/entry
  instance.
- A navigation bug often comes from confusing declared destinations with actual history.

**Primary Subtopics**

- `navigation_fundamentals`

**Supporting Subtopics**

- `viewmodel_lifecycle`
- `saved_state`
- `back_handling`
- `android_intents` (`android_platform`)
- `android_components` (`android_platform`)

**Existing assessment reviewed**

- `navigation_fundamentals_001`
- `activity_launch_mode_task_stack`
- `navigation_graph_viewmodel_shared_scope`
- `navigation_back_stack_entry_lifetime`

**Assessment position**

Coverage is good. Future expansion should avoid duplicating "what is a back stack?" and
instead test entry identity, nested/flow scope, or task-vs-navigation-stack reasoning.

---

## L3.2 — Navigation 2 and Navigation 3: Who Owns the Stack?

**Stable Lesson ID:** `lesson_navigation_2_vs_3_state_ownership`

**Objective:** compare Navigation 2 and Navigation 3 by where navigation state lives and
how destination lifetime is exposed, rather than by memorizing API-name replacements.

### Current platform position

As of the initial blueprint in October 2026, Navigation 3 is a stable Compose-first
AndroidX navigation library. The stable 1.2.0 line was released in September 2026. The
authoring pass must re-check current stable documentation and not describe Navigation 3 as
experimental.

### Core

**Navigation 2**

- `NavController` manages navigation operations and its internal back stack.
- `NavHost` renders destinations from a navigation graph.
- `NavBackStackEntry` exposes destination-scoped lifecycle, saved state, and ViewModel
  ownership.
- app code observes/commands controller-managed navigation state.

**Navigation 3**

- the app owns explicit navigation state, typically keys in one or more back-stack
  collections;
- adding/removing keys changes navigation;
- `NavDisplay` renders entries derived from that state;
- entry-scoped lifetime/state is provided around displayed navigation entries;
- app ownership makes custom stack models, adaptive scenes, and multiple-stack strategies
  more explicit.

### Practical

- Simple typed route/key examples in both models.
- `navigate` / `popBackStack` mental model versus mutating app-owned navigation state.
- `rememberNavBackStack` when a Navigation 3 stack must be saveable through recreation,
  with the required key constraints.
- Destination lifecycle:
  Navigation 2 exposes `NavBackStackEntry` lifecycle directly;
  Navigation 3 supplies entry-scoped lifecycle to destination content.
- Migration should be framed as an ownership-model change, not a mechanical rename list.
- Current result/deep-link APIs may be named where useful but are not the core of this
  Lesson.

### Senior

- Navigation 3 makes navigation state ordinary application state, which improves
  testability/flexibility but also makes the application responsible for modeling that
  state correctly.
- Navigation 2's controller abstraction is not "wrong"; it centralizes behavior many apps
  still depend on.
- Compare the model against requirements such as adaptive layouts, multiple top-level
  stacks, custom state ownership, and migration cost.

**Primary Subtopics**

- `navigation_2_vs_3`

**Supporting Subtopics**

- `navigation_fundamentals`
- `viewmodel_lifecycle`
- `saved_state`
- `compose_state` (`android_ui`)

**Existing assessment reviewed**

- `navigation3_back_stack_ownership`

**Assessment gap**

The current single Applied question checks the central ownership shift but not its
consequences. A later scenario can ask the candidate to choose where custom multi-stack or
saveable navigation state should live, or to reason about destination lifecycle during a
migration.

---

# Unit 4 — External Entry and Back Navigation

**Purpose:** teach how navigation behaves when the user enters from outside the app and how
the system expects the app to expose the reverse path.

**Prerequisites:** Unit 3.

## L4.1 — Deep Links and App Links: Entering the Right State

**Stable Lesson ID:** `lesson_deep_links_app_links_and_reconstruction`

**Objective:** treat a deep link as an external request to construct valid navigation state,
with security and history considerations, rather than as "open this composable".

### Core

- A deep link routes an external Intent/URI to specific app content.
- Custom schemes and ordinary web deep links do not by themselves prove domain ownership.
- Android App Links use HTTPS plus verified association between a domain and the app.
- `assetlinks.json` and the app's verified link declaration establish that association.
- A deep-linked destination may be reached without the internal navigation history that
  would exist after ordinary in-app navigation.
- The app must construct an appropriate stack/state for the requested destination.

### Practical

- Verified App Link opening product/detail content directly.
- Handling IDs/path/query parameters as external input that still requires validation.
- Authentication gating: an incoming link can identify desired content while the app first
  establishes sign-in, then reconstructs the intended navigation state.
- Browser/website fallback when the app is unavailable or a link is not verified.
- Deep-link testing should cover both routing and the resulting Back history.
- Android 15+ Dynamic App Links are **Reference**: server-side refinements can change
  matching rules without an app update, but they do not replace the app's navigation/state
  handling.
- Navigation 3's current deep-link API may be referenced, but Android App Links and intent
  routing remain the platform foundation.

### Senior

- Verification establishes domain/app association, not trust in every path parameter or
  authorization to every resource.
- Deep links should map external requests into normal navigation/state mechanisms so Back,
  state restoration, and ownership continue to behave consistently.
- Decide intentionally whether Back from a deep-linked destination should expose synthetic
  parent history, prior app history, or leave the task, depending on product requirements.

**Primary Subtopics**

- `deep_links`

**Supporting Subtopics**

- `navigation_fundamentals`
- `android_intents` (`android_platform`)
- `security` concepts as Bridge only
- `saved_state`

**Existing assessment reviewed**

- `deep_link_app_link_verification`

**Assessment gap**

High priority. Add an Applied question about building valid navigation history from a deep
link, validating external route input, or handling authentication before the requested
destination. Do not add another definition-only `assetlinks.json` question.

---

## L4.2 — Back Is a Navigation Transition, Including Predictive Back

**Stable Lesson ID:** `lesson_back_navigation_and_predictive_back`

**Objective:** model Back as a navigation-state transition the system can reason about and
preview, and use custom interception only when the default stack transition is not enough.

### Core

- System Back normally moves to the previous history entry or exits the current task when
  appropriate.
- Navigation libraries should normally express Back by popping/mutating navigation state.
- `OnBackPressedDispatcher` / AndroidX Back APIs replace legacy interception through
  `onBackPressed()` for modern app handling.
- Compose `BackHandler` handles ordinary custom Back interception.
- Predictive Back requires the system/app to know ahead of commit what Back would do.
- `PredictiveBackHandler` exposes gesture progress for custom in-app predictive
  transitions; cancellation must restore any transient visual state.
- A Back callback is not the right fix for an incorrectly constructed stack.

### Practical

- Sign-in -> home: remove sign-in from future history through stack mutation rather than
  intercepting every Back press on home.
- `popUpTo(..., inclusive = true)` as Navigation 2 stack surgery during forward navigation.
- Navigation 3 equivalent reasoning: mutate app-owned state so the undesired entry no
  longer exists.
- `launchSingleTop` solves duplicate top entries, not arbitrary history removal.
- Legacy `onBackPressed()` interception prevents correct predictive behavior because the
  decision arrives too late for preview.
- Gesture cancellation versus gesture commit when driving a custom animation.

### Senior

- Predictive Back makes Back a two-phase interaction: preview/progress can occur before the
  transition commits.
- Correct stack modeling improves both ordinary and predictive Back because the destination
  already exists as explicit state.
- Custom Back handling should be narrowly scoped and lifecycle-aware; broad interception
  creates hidden navigation policy outside the stack model.
- "Up" navigation and system Back can produce different product behavior; treat this as
  Reference unless a concrete app requirement needs the distinction.

**Primary Subtopics**

- `back_handling`

**Supporting Subtopics**

- `navigation_fundamentals`
- `lifecycle_aware_apis`
- `coroutine_cancellation` (`async_reactive`) only where predictive gesture progress is
  collected

**Existing assessment reviewed**

- `predictive_back_handler_registration`
- `navigation_pop_up_to_inclusive`

**Assessment gap**

Current coverage is useful. A future Applied question could test predictive gesture
cancellation/progress or distinguish fixing the stack from installing a Back callback.

---

# Existing Taxonomy — Editorial Decision for Every Subtopic

| Subtopic | Decision | Home / treatment |
| --- | --- | --- |
| `activity_lifecycle` | Teach | L1.1 |
| `fragment_lifecycle` | Teach | L1.2 |
| `configuration_changes` | Teach | L2.1 |
| `process_death` | Teach | L2.1 |
| `viewmodel_lifecycle` | Teach | L2.2 |
| `saved_state` | Teach | L2.3 |
| `lifecycle_aware_apis` | Teach | L1.3 |
| `navigation_fundamentals` | Teach | L3.1 |
| `navigation_2_vs_3` | Teach | L3.2 |
| `deep_links` | Teach | L4.1 |
| `back_handling` | Teach | L4.2 |

Every existing ACTIVE Subtopic has one clear teaching home.

---

# Complete Subject Map Beyond the Existing Taxonomy

## Teach inside the initial 10 Lessons

These concepts are required even though some do not have dedicated Subtopics:

- Fragment instance lifetime versus Fragment View lifetime.
- `LifecycleOwner` selection.
- STARTED versus RESUMED as requirement-driven active states.
- lifecycle-aware collection restart semantics.
- the destruction/recreation matrix: configuration recreation, process death, permanent
  dismissal.
- owner identity as the basis of ViewModel identity.
- navigation entry as lifecycle / saved-state / ViewModel owner.
- route identity versus entry identity.
- Activity task stack versus navigation-library stack.
- saved-state reconstruction limits and size/cost trade-offs.
- Navigation 3 app-owned navigation state.
- deep-link construction of a valid navigation history.
- predictive Back preview/progress/cancellation.

## Bridge

Concepts owned by another Topic but necessary here:

- coroutine cancellation and child scopes;
- Flow collection;
- Compose `rememberSaveable`, `collectAsStateWithLifecycle`, and `BackHandler`;
- state-holder responsibility;
- Android process model;
- Intents and manifest filters;
- durable persistence.

Teach only enough locally for the lifetime/navigation argument to stand on its own.

## Reference / Future Expansion Candidates

These are useful but do not deserve a standalone initial Lesson:

1. **Multiple top-level back stacks**
   - common bottom-navigation requirement;
   - especially relevant to Navigation 3 recipes;
   - can become a future Applied navigation Lesson if the Topic expands.

2. **Adaptive / multi-pane Navigation 3 scenes**
   - important to foldables/tablets;
   - better added after basic app-owned stack state is secure.

3. **Navigation result passing**
   - Navigation 2 saved-state-based patterns and Navigation 3 Result API;
   - useful but not part of the first ownership/lifetime path.

4. **FragmentManager transaction mechanics**
   - `add`, `replace`, `addToBackStack`, reordering details;
   - retain only the Fragment View-lifetime consequence initially.

5. **Activity launch modes and Intent flags in depth**
   - `singleTop`, `singleTask`, task affinity, document modes;
   - bridge only the task-history behavior needed by existing questions.

6. **SavedStateRegistry custom providers**
   - useful for reusable infrastructure;
   - keep as advanced reference under saved state.

7. **Dynamic App Links**
   - Android 15+ server-controlled matching refinements;
   - reference under deep links.

8. **Up versus Back**
   - product/navigation distinction worth recognizing;
   - initial treatment can remain a short comparison.

9. **Lifecycle `DefaultLifecycleObserver` / event observers**
   - useful API forms;
   - mental model matters more than callback interface inventory.

10. **Navigation animations and transitions**
    - predictive integration is relevant;
    - general animation authoring belongs elsewhere.

## Exclude from the initial learning program

- Memorizing complete callback-order charts without a scenario.
- Exhaustive every-configuration-qualifier handling.
- Treating `android:configChanges` as the normal state-management solution.
- Deprecated retained Fragments as a recommended retention mechanism.
- Loader-era architecture.
- Exhaustive FragmentManager internal state-machine details.
- Exhaustive Intent-flag/task-affinity combinations.
- Navigation XML syntax tutorials.
- Every Navigation 2 DSL overload.
- Every Navigation 3 decorator/scene API.
- OEM/process-killer folklore presented as a stable platform contract.
- Saved-state serialization internals that do not change an engineering decision.
- KMP host lifecycle mapping; that stays in the KMP Topic.

---

# Taxonomy Gaps

The blueprint must not invent IDs. The following concepts could justify future taxonomy
expansion if assessment coverage grows.

## High-value gap — Navigation entry lifetime / ownership

Current nearest home: `navigation_fundamentals`.

Why it may eventually deserve its own Subtopic:

- `NavBackStackEntry` / entry ownership combines lifecycle, ViewModelStore, saved state,
  and entry identity;
- it is rich enough for several Applied questions without being the same as generic
  "navigation fundamentals".

No immediate taxonomy change is required.

## High-value gap — Lifecycle owner selection

Current nearest home: `lifecycle_aware_apis`.

Potential future questions may distinguish:

- Activity owner;
- Fragment owner;
- Fragment View owner;
- destination/entry owner.

If the question bank grows, a more specific identity may help coverage reporting.

## Possible future gap — Task stack versus in-app navigation stack

Current coverage is awkwardly split between `navigation_fundamentals` and Android Platform
concepts.

Keep the current taxonomy for the initial program, but avoid pretending the two stacks are
the same abstraction.

## No immediate new Subtopic needed for

- predictive Back: already `back_handling`;
- App Links: already `deep_links`;
- Navigation 3 state ownership: already `navigation_2_vs_3`;
- `SavedStateHandle`: already `saved_state`;
- process-death testing: already `process_death`.

---

# Semantic Review of the Existing 23 ACTIVE Questions

This is a review snapshot for authoring. Production Lesson mappings remain through stable
Subtopic IDs rather than individual Question IDs.

| Existing Question | Best learning home | Review |
| --- | --- | --- |
| `activity_lifecycle_001` | L1.1 | Good Foundation check for why `onPause` must stay short. |
| `activity_on_pause_vs_on_stop_visibility` | L1.1 | Strong visibility-vs-focus scenario; important Core acceptance check. |
| `fragment_view_lifecycle_collection` | L1.2 / L1.3 | Good owner-selection Foundation question. |
| `fragment_back_stack_view_destroyed_binding` | L1.2 | Strong two-lifecycle question; should anchor ViewBinding cleanup explanation. |
| `lifecycle_repeat_on_lifecycle` | L1.3 | Good Applied behavior check; requires restart/cancel semantics. |
| `configuration_changes_001` | L2.1 | Good default recreation Foundation anchor. |
| `configuration_change_vs_process_recreation` | L2.1 | Good distinction question; should remain a matrix, not callback trivia. |
| `process_death_reproducing_restoration` | L2.1 | Strong Applied testing question; prevents "Don't keep activities = process death" misconception. |
| `viewmodel_destination_scope` | L2.2 | Good scope-to-owner Applied question. |
| `viewmodel_store_configuration_retention` | L2.2 | Necessary Foundation lifetime anchor. |
| `viewmodel_clear_owner_finish` | L2.2 | Good counterpart showing permanent destruction. |
| `saved_state_binder_transaction_limit` | L2.3 | Useful practical size/cost question; avoid teaching an exact safe-size number. |
| `saved_state_transient_inputs` | L2.3 | Good selection of reconstruction hint versus durable data. |
| `savedstatehandle_process_recreation` | L2.3 | Strong Foundation mechanism check. |
| `remember_saveable_vs_viewmodel_ownership` | L2.3 + Compose bridge | Good Applied split; ensure Lifecycle lesson does not re-teach Compose state ownership. |
| `navigation_fundamentals_001` | L3.1 | Necessary Foundation distinction between runtime history and graph declaration. |
| `activity_launch_mode_task_stack` | L3.1 | Accurate but cross-topic; teach the task-stack consequence without expanding into a launch-mode catalogue. |
| `navigation_graph_viewmodel_shared_scope` | L3.1 / L2.2 | Good Applied graph-owner lifetime scenario. |
| `navigation_back_stack_entry_lifetime` | L3.1 | Strong owner-lifetime question. |
| `navigation3_back_stack_ownership` | L3.2 | Correct central difference between Navigation 2 and 3. |
| `deep_link_app_link_verification` | L4.1 | Good Foundation verification question but too narrow to establish deep-link navigation reasoning alone. |
| `predictive_back_handler_registration` | L4.2 | Good modern Back-contract Foundation question. |
| `navigation_pop_up_to_inclusive` | L4.2 | Good Applied reminder that future Back behavior is often fixed by changing the stack now. |

---

# Assessment State

The current Lifecycle / Navigation bank contains:

- 23 ACTIVE questions
- 15 `FOUNDATION`
- 8 `APPLIED`
- 0 `ADVANCED`

This distribution is not itself a defect.

The next assessment pass should search for realistic deeper reasoning, but it must not
upgrade questions merely to create an Advanced category.

## Highest-priority future assessment gaps

### Activity lifecycle

Add one Applied resource-lifetime scenario:
- visible-but-not-focused versus fully hidden;
- choose START/STOP or RESUME/PAUSE based on the requirement.

### Fragment lifecycle

Add one Applied stale-view scenario:
- Fragment survives;
- View is destroyed;
- callback/collector retains binding or attempts a View update.

### Lifecycle-aware APIs

Consider an owner-selection scenario:
- Fragment instance versus Fragment View;
- or STARTED versus RESUMED based on product behavior.

### Recreation / process death

Potential high-signal synthesis:
- ViewModel survives rotation;
- singleton survives rotation but not process death;
- SavedStateHandle restores a key;
- repository reloads durable data.

This can become Advanced only if all mechanisms are necessary to solve the stem.

### ViewModel / saved state

Coverage is already good. Do not add questions simply because the Lessons are important.

### Navigation 2 versus 3

Add a scenario about:
- custom/multiple app-owned back stacks;
- saveable stack ownership;
- destination lifecycle migration.

### Deep links

Highest content-to-assessment gap.

Add Applied coverage for:
- reconstructing appropriate history from a deep link;
- auth-gating while preserving intended destination;
- treating route parameters as untrusted/external input.

### Back / predictive back

Possible Applied scenario:
- gesture progress starts an animation;
- gesture cancellation must restore visual state;
- committed gesture performs navigation.

Or test whether a stack bug should be fixed with stack mutation rather than Back interception.

---

# Stable Identities for the Authoring Run

## Units

| Order | Stable Unit ID | Title |
| ---: | --- | --- |
| 1 | `unit_component_lifetimes_and_visibility` | Component Lifetimes and Visibility |
| 2 | `unit_recreation_retention_and_restoration` | Recreation, Retention and Restoration |
| 3 | `unit_navigation_state_and_destination_lifetime` | Navigation State and Destination Lifetime |
| 4 | `unit_external_entry_and_back_navigation` | External Entry and Back Navigation |

## Lessons

| Order | Stable Lesson ID | Title |
| ---: | --- | --- |
| 1 | `lesson_activity_lifecycle_visibility_and_work` | Activity Lifecycle: Visible, Interactive and Gone |
| 2 | `lesson_fragment_and_view_lifecycles` | A Fragment Has an Instance Lifecycle and a View Lifecycle |
| 3 | `lesson_lifecycle_aware_work_and_collection` | Lifecycle-Aware Work: Follow the Owner, Not the Screen Name |
| 4 | `lesson_recreation_configuration_process_and_dismissal` | Configuration Change, Process Death and Permanent Dismissal |
| 5 | `lesson_viewmodel_owner_and_scope` | ViewModel Lifetime Is the Lifetime of Its Owner |
| 6 | `lesson_saved_state_and_reconstruction` | Saved State Is a Reconstruction Hint, Not a Database |
| 7 | `lesson_navigation_back_stacks_tasks_and_entry_lifetimes` | Back Stacks, Tasks and Entry Lifetimes |
| 8 | `lesson_navigation_2_vs_3_state_ownership` | Navigation 2 and Navigation 3: Who Owns the Stack? |
| 9 | `lesson_deep_links_app_links_and_reconstruction` | Deep Links and App Links: Entering the Right State |
| 10 | `lesson_back_navigation_and_predictive_back` | Back Is a Navigation Transition, Including Predictive Back |

These are the initial stable identity proposals. If the repository contains an actual ID
collision at authoring time, fix the collision before production content is authored.
Otherwise preserve these identities through the author/reviewer/editor workflow.

---

# Authoritative Source Families for the Authoring Run

Prefer current official Android / AndroidX documentation.

Likely source families:

- Android Activity lifecycle
- Fragment lifecycle
- AndroidX Lifecycle and lifecycle-aware coroutines
- ViewModel overview and ViewModel scoping APIs
- Save UI states / SavedStateHandle
- Android process and app lifecycle documentation
- Navigation back stack documentation
- Navigation 2 documentation and typed Compose navigation
- Navigation 3 overview, basics, state-saving, migration guide, and release notes
- Android deep links and App Links
- Predictive Back and AndroidX Back APIs
- Activity launch modes / tasks only for the bounded task-stack bridge
- official API reference when a precise callback or state guarantee is required

## Freshness requirements

This Topic is more version-sensitive than Kotlin language semantics.

At authoring time, re-check:

- Navigation 3 stable version and recommended APIs;
- Navigation 2 -> Navigation 3 migration guidance;
- predictive Back requirements;
- App Links / Dynamic App Links behavior;
- SavedStateHandle current APIs and saving semantics;
- Lifecycle library guidance.

Do not freeze an alpha/beta behavior into learner-facing prose if a stable replacement
exists.

Where AndroidX source is cited directly, follow the repository's immutable-revision source
rules.

---

# Cross-Topic Boundary Register

Authors and reviewers should use this to prevent duplication.

| Concept encountered here | This Topic teaches | Deeper owner |
| --- | --- | --- |
| `rememberSaveable` | what lifetime/recreation problem it participates in | Compose |
| `collectAsStateWithLifecycle` | why lifecycle ownership matters | Compose |
| `repeatOnLifecycle` + Flow | start/stop behavior by lifecycle state | Coroutines / Flow owns Flow mechanics |
| `viewModelScope` | owner lifetime consequence only | Coroutines + Architecture |
| ViewModel responsibilities | lifetime/scoping mechanics | Architecture |
| state ownership | Android owner lifetime constraints | Architecture |
| database/repository persistence | why saved state is not durable data | Local Data / Architecture |
| Android process | what process death destroys | Android Platform |
| Intent / manifest filters | enough for deep-link/task reasoning | Android Platform |
| KMP lifecycle | none beyond saying Android-specific | KMP |
| Compose Navigation 3 UI | navigation-state ownership/lifetime | this Topic; Compose layout/state mechanics stay in Compose |

---

# Authoring Guidance for the Second Run

Use the same multi-agent **author -> independent reviewer -> editor/correction** process as
the completed Kotlin/JVM program.

## Suggested batches

### Batch 1
- Activity Lifecycle
- Fragment and View Lifecycles
- Lifecycle-Aware Work

Review and correct before continuing.

### Batch 2
- Recreation Matrix
- ViewModel Owner and Scope
- Saved State and Reconstruction

Review and correct.

### Batch 3
- Back Stacks / Tasks / Entry Lifetimes
- Navigation 2 versus Navigation 3

Review and correct.

### Batch 4
- Deep Links / App Links
- Back / Predictive Back

Review and correct.

Then perform an integrated Topic review before assessment expansion.

## Author responsibilities

Each author must:

- read this blueprint and the repository authoring contract;
- read all ACTIVE questions for the Lesson's Primary Subtopics;
- verify current Android/AndroidX behavior;
- teach from lifetime/state mental models rather than callback/API inventories;
- use realistic Android examples;
- keep learner-facing prose project-agnostic;
- keep curriculum IDs and planning language out of learner-facing content;
- preserve cross-topic boundaries;
- use Core / Practical / Senior depth honestly;
- avoid inventing a Senior section when the deeper material would be trivia.

## Reviewer responsibilities

Independent reviewers should check:

- technical correctness;
- version freshness;
- whether owner/lifetime distinctions are precise;
- configuration versus process-death correctness;
- ViewModel retention versus persistence wording;
- saved-state limits and dismissal semantics;
- Fragment View lifecycle correctness;
- Navigation 2 / Navigation 3 distinctions;
- predictive Back behavior;
- deep-link security/history reasoning;
- duplication with Compose / Architecture / Coroutines;
- whether existing questions are answerable from real understanding.

Reviewers must not turn the Topic into:

- an Architecture rewrite;
- a Compose lifecycle API catalogue;
- a coroutine tutorial;
- a Navigation API reference;
- a callback-order memorization guide.

---

# Completion Criteria for the Later Authoring Run

The Topic is complete for the initial phase only when:

1. all 10 planned Lessons are authored;
2. every Lesson has independent technical/editorial review and correction;
3. the full Topic passes an integrated read for terminology, prerequisites, and duplication;
4. existing ACTIVE questions are semantically supported by the Lessons;
5. justified assessment gaps are filled under the existing question-authoring contract;
6. generated learning/question coverage is current;
7. validation and relevant tests pass;
8. no KMP-specific host material leaked into the Android core Topic;
9. no learner-facing curriculum/project narration was introduced;
10. any deviation from this blueprint is explicitly justified.

---

# Decision Summary

The initial Lifecycle, State & Navigation program should contain **10 Lessons across 4
Units**.

Ten is justified because this Topic contains several distinct owner/lifetime models that
should not be collapsed:

1. Activity visibility and interaction lifetime
2. Fragment instance versus View lifetime
3. lifecycle-aware work
4. configuration/process/dismissal recreation
5. ViewModel owner lifetime
6. saved-state reconstruction
7. navigation stack/task/entry lifetime
8. Navigation 2 versus Navigation 3 ownership
9. external deep-link entry
10. Back and predictive Back

Fewer Lessons would force unrelated lifetime models together, especially Fragment View
lifetime with generic lifecycle APIs or saved state with ViewModel lifetime. More Lessons
would promote Reference material such as launch-mode matrices, multiple back stacks, or
Dynamic App Links too early.

The main assessment weakness is **not raw question count**. The bank already has useful
coverage. The clearest future gaps are:

- Applied Activity resource lifetime;
- Applied Fragment stale-View lifetime;
- deeper lifecycle-owner selection;
- Navigation 3 ownership consequences;
- deep-link history/auth/input reasoning;
- predictive-Back gesture cancellation or stack-vs-callback reasoning.

Advanced questions should emerge only where several lifetime/state mechanisms genuinely
interact.
