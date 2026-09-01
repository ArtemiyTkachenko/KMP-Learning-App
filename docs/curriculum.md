# MVP Android Interview Curriculum

## Purpose and Scope

This document defines the MVP curriculum taxonomy for modern senior Android
interview preparation. It is a human-readable curriculum map for focused
practice, mixed interview selection, progress aggregation, and future question
bank expansion.

The conceptual hierarchy is:

1. Topic
2. Subtopic
3. Question

The implemented `Question` model links to its Topic and Subtopic by stable IDs
and contains text, answer options, authored `selectionMode`, required authored
`level`, correct-answer IDs, explanation, sources, and lifecycle status.
`QuestionLevel` represents interview depth with the exact values `FOUNDATION`,
`APPLIED`, and `ADVANCED`. It describes the reasoning required by the question,
not learner performance, and is not yet used for assessment filtering.

Not every subtopic listed here must have an MVP question immediately. The
taxonomy represents the intended curriculum map so the question bank can grow
incrementally without changing existing content identity.

## Stable IDs

- Every Topic and Subtopic has an explicit stable ID.
- IDs use lowercase snake_case.
- IDs represent identity and are not derived dynamically from display text.
- Renaming display text must not require changing the stable ID.
- Topic IDs are globally unique.
- Subtopic IDs are also globally unique for simplicity.
- Numeric database IDs are intentionally not used.

## MVP Boundaries

In scope:

- Theory and engineering interview preparation for senior Android roles.
- Modern Android, Kotlin, Jetpack Compose, architecture, testing, build, and
  Kotlin Multiplatform topics relevant to real interviews.
- Legacy Android concepts when they still appear in practical interviews.

Out of scope:

- Algorithm and data-structure exercises.
- Behavioral interview questions.
- Backend implementation or backend interview curriculum.
- Treating obsolete Android APIs as recommended modern implementation
  approaches.

## Topic Overview

| Topic ID | Display Name |
| --- | --- |
| `android_platform` | Android Platform & Application Model |
| `lifecycle_navigation` | Lifecycle, State & Navigation |
| `android_ui` | UI — Views & Jetpack Compose |
| `kotlin_language` | Kotlin Language & JVM Fundamentals |
| `async_reactive` | Coroutines, Flow & Reactive Programming |
| `architecture` | Application Architecture & Design Principles |
| `dependency_injection` | Dependency Injection |
| `local_data` | Local Persistence & Offline Data |
| `networking` | Networking & Serialization |
| `background_work` | Background Work & OS Constraints |
| `notifications` | Notifications & Push Messaging |
| `testing` | Testing & Testability |
| `performance` | Performance, Memory & Debugging |
| `security` | Security, Privacy & Permissions |
| `build_delivery` | Build System, Modularization & Delivery |
| `mobile_system_design` | Mobile System Design |
| `kmp` | Kotlin Multiplatform & Compose Multiplatform |

## Android Platform & Application Model

Stable topic ID: `android_platform`

Scope: Android runtime fundamentals, app components, configuration, resources,
main-thread behavior, and platform communication concepts.

| Subtopic ID | Display Name |
| --- | --- |
| `android_process_model` | Application and process model |
| `android_components` | Android application components |
| `android_context` | Context |
| `android_intents` | Intents |
| `android_manifest` | Manifest and application configuration |
| `android_resources` | Android resources and qualifiers |
| `android_main_thread` | Main thread, Looper, and Handler fundamentals |
| `android_ipc` | Android IPC fundamentals |

## Lifecycle, State & Navigation

Stable topic ID: `lifecycle_navigation`

Scope: Android lifecycle behavior, state restoration, navigation models, deep
links, and back handling.

| Subtopic ID | Display Name |
| --- | --- |
| `activity_lifecycle` | Activity lifecycle |
| `fragment_lifecycle` | Fragment and Fragment view lifecycle |
| `configuration_changes` | Configuration changes |
| `process_death` | Process death and recreation |
| `viewmodel_lifecycle` | ViewModel lifecycle |
| `saved_state` | Saved-state mechanisms |
| `lifecycle_aware_apis` | Lifecycle-aware APIs |
| `navigation_fundamentals` | Navigation and back-stack fundamentals |
| `navigation_2_vs_3` | Navigation 2 vs Navigation 3 |
| `deep_links` | Deep links |
| `back_handling` | Back handling and predictive back |

## UI — Views & Jetpack Compose

Stable topic ID: `android_ui`

Scope: Classic Android Views, Jetpack Compose, rendering, state, side effects,
accessibility, theming, and interop.

### Views

| Subtopic ID | Display Name |
| --- | --- |
| `views_fundamentals` | View and ViewGroup fundamentals |
| `xml_layouts` | XML layouts |
| `view_rendering` | Measure, layout, and draw |
| `recyclerview` | RecyclerView |
| `custom_views` | Custom Views |
| `view_events` | View event handling |
| `view_binding` | ViewBinding and DataBinding awareness |

### Compose

| Subtopic ID | Display Name |
| --- | --- |
| `compose_fundamentals` | Compose fundamentals |
| `compose_recomposition` | Composition and recomposition |
| `compose_state` | Compose state |
| `compose_state_hoisting` | State hoisting |
| `compose_udf` | Unidirectional data flow in Compose |
| `compose_side_effects` | Compose side-effect APIs |
| `compose_derived_state` | derivedStateOf and derived state |
| `compose_snapshot_system` | Compose snapshot-system fundamentals |
| `compose_layouts_modifiers` | Layouts and Modifiers |
| `compose_lazy_layouts` | Lazy layouts |
| `compose_identity_keys` | Identity and keys |
| `compose_stability` | Stability and skippability |
| `composition_local` | CompositionLocal |
| `compose_theming` | Material 3 and theming |
| `compose_accessibility` | Semantics and accessibility |
| `compose_previews` | Compose previews |
| `views_compose_interop` | Views and Compose interoperability |

## Kotlin Language & JVM Fundamentals

Stable topic ID: `kotlin_language`

Scope: Kotlin language features, JVM concepts relevant to Android, and Java
interoperability expected in Android interviews.

| Subtopic ID | Display Name |
| --- | --- |
| `kotlin_variables` | val, var, and basic declarations |
| `kotlin_nullability` | Nullability |
| `kotlin_functions` | Functions and arguments |
| `kotlin_extension_functions` | Extension functions |
| `kotlin_classes` | Kotlin classes |
| `kotlin_data_classes` | Data classes |
| `kotlin_objects` | object and companion object |
| `kotlin_sealed_types` | Sealed classes and interfaces |
| `kotlin_interfaces_inheritance` | Interfaces and inheritance |
| `kotlin_visibility` | Visibility modifiers |
| `kotlin_properties` | Properties, accessors, and backing fields |
| `kotlin_delegation` | Delegation and delegated properties |
| `kotlin_generics` | Generics and variance |
| `kotlin_lambdas` | Lambdas and higher-order functions |
| `kotlin_inline_functions` | inline, noinline, and crossinline |
| `kotlin_reified_types` | Reified type parameters |
| `kotlin_collections` | Collections |
| `kotlin_sequences` | Sequences and lazy collection processing |
| `kotlin_equality` | Structural vs referential equality |
| `kotlin_exceptions` | Exception handling |
| `kotlin_scope_functions` | let/run/apply/also/with |
| `kotlin_java_interop` | Java interoperability |
| `jvm_fundamentals` | JVM fundamentals relevant to Android |

## Coroutines, Flow & Reactive Programming

Stable topic ID: `async_reactive`

Scope: Kotlin coroutines, Flow, lifecycle-aware asynchronous work, and reactive
programming concepts still relevant to Android teams.

### Coroutines

| Subtopic ID | Display Name |
| --- | --- |
| `coroutine_fundamentals` | Coroutine and suspend fundamentals |
| `coroutine_builders` | launch and async |
| `coroutine_scope` | CoroutineScope |
| `coroutine_context` | CoroutineContext |
| `coroutine_dispatchers` | Dispatchers |
| `coroutine_jobs` | Job and parent-child relationships |
| `structured_concurrency` | Structured concurrency |
| `coroutine_cancellation` | Cancellation |
| `coroutine_exceptions` | Exception propagation and handling |
| `coroutine_supervision` | SupervisorJob and supervisorScope |
| `coroutine_context_switching` | withContext |
| `coroutine_parallelism` | Concurrency and async/await |
| `lifecycle_coroutines` | Lifecycle-aware coroutine scopes |

### Flow

| Subtopic ID | Display Name |
| --- | --- |
| `flow_fundamentals` | Flow fundamentals |
| `flow_operators` | Flow operators |
| `flow_collection` | Flow collection |
| `flow_errors` | Flow exception handling |
| `flow_context` | flowOn and execution context |
| `flow_buffering` | Buffering and conflation |
| `stateflow` | StateFlow |
| `sharedflow` | SharedFlow |
| `hot_vs_cold_streams` | Hot vs cold streams |
| `flow_sharing` | stateIn, shareIn, and sharing policies |

### Reactive

| Subtopic ID | Display Name |
| --- | --- |
| `livedata` | LiveData |
| `rxjava_fundamentals` | RxJava/RxKotlin fundamentals |
| `flow_vs_rxjava` | Flow vs RxJava |

## Application Architecture & Design Principles

Stable topic ID: `architecture`

Scope: Architectural patterns, dependency direction, state ownership,
boundaries, and trade-offs for maintainable Android applications.

| Subtopic ID | Display Name |
| --- | --- |
| `separation_of_concerns` | Separation of concerns |
| `layered_architecture` | Layered architecture |
| `mvc` | MVC |
| `mvp` | MVP |
| `mvvm` | MVVM |
| `mvi` | MVI |
| `mvvm_vs_mvi` | MVVM vs MVI |
| `unidirectional_data_flow` | Unidirectional data flow |
| `repository_pattern` | Repository pattern |
| `use_cases` | Use cases and when to introduce them |
| `single_source_of_truth` | Single source of truth |
| `state_ownership` | State ownership |
| `clean_architecture` | Clean Architecture |
| `solid` | SOLID principles |
| `dependency_direction` | Dependency direction and inversion |
| `interface_boundaries` | Interface boundaries |
| `error_modeling` | Error representation/modeling |
| `architecture_tradeoffs` | Architecture trade-offs and avoiding over-engineering |

## Dependency Injection

Stable topic ID: `dependency_injection`

Scope: Dependency injection concepts, manual wiring, scopes, and practical
trade-offs across Dagger, Hilt, and Koin.

### Fundamentals

| Subtopic ID | Display Name |
| --- | --- |
| `di_fundamentals` | Dependency injection fundamentals |
| `manual_di` | Manual dependency injection |
| `constructor_injection` | Constructor injection |
| `composition_root` | Composition root |
| `dependency_graphs` | Dependency graphs |
| `di_scopes` | Scopes and lifetimes |
| `service_locator_vs_di` | Service locator vs dependency injection |

### Dagger

| Subtopic ID | Display Name |
| --- | --- |
| `dagger_fundamentals` | Dagger fundamentals |
| `dagger_modules` | Dagger modules |
| `dagger_bindings` | @Provides and @Binds |
| `dagger_components` | Components and subcomponents |
| `dagger_scopes` | Dagger scopes |
| `dagger_qualifiers` | Qualifiers |
| `dagger_multibindings` | Multibindings |

### Hilt

| Subtopic ID | Display Name |
| --- | --- |
| `hilt_fundamentals` | Hilt fundamentals |
| `hilt_components` | Hilt components and lifecycle scopes |
| `hilt_viewmodels` | Hilt ViewModels |
| `hilt_modules` | Hilt modules and InstallIn |
| `hilt_vs_dagger` | Hilt vs raw Dagger |

### Koin

| Subtopic ID | Display Name |
| --- | --- |
| `koin_fundamentals` | Koin fundamentals |
| `koin_definitions` | single/factory definitions |
| `koin_scopes` | Koin scopes |
| `koin_viewmodels` | Koin ViewModel integration |
| `koin_multiplatform` | Koin and Kotlin Multiplatform |

### Comparison

| Subtopic ID | Display Name |
| --- | --- |
| `di_framework_tradeoffs` | DI framework trade-offs |

## Local Persistence & Offline Data

Stable topic ID: `local_data`

Scope: Android storage options, database concepts, Room, caching, offline-first
design, and migration concerns.

| Subtopic ID | Display Name |
| --- | --- |
| `storage_selection` | Choosing a storage mechanism |
| `shared_preferences` | SharedPreferences |
| `datastore` | DataStore |
| `datastore_vs_sharedpreferences` | DataStore vs SharedPreferences |
| `file_storage` | File storage |
| `sqlite_fundamentals` | SQLite and relational-database fundamentals |
| `room_fundamentals` | Room fundamentals |
| `room_entities` | Room entities |
| `room_dao` | Room DAOs and queries |
| `room_relationships` | Room relationships |
| `room_type_converters` | Type converters |
| `room_transactions` | Transactions |
| `room_migrations` | Room migrations |
| `room_flow` | Room and Flow |
| `caching` | Caching |
| `offline_first` | Offline-first architecture |
| `cache_invalidation` | Cache invalidation |
| `schema_migrations` | Schema/version migration concepts |
| `room_vs_sqldelight` | Room vs SQLDelight |

## Networking & Serialization

Stable topic ID: `networking`

Scope: HTTP, Android networking libraries, error handling, serialization, and
network-adjacent application concerns.

### HTTP

| Subtopic ID | Display Name |
| --- | --- |
| `http_fundamentals` | HTTP fundamentals |
| `http_methods` | HTTP methods |
| `http_status_codes` | HTTP status codes |
| `rest` | REST concepts |
| `http_idempotency` | Idempotency |
| `pagination` | Pagination |
| `network_timeouts` | Timeouts |
| `network_retries` | Retries and backoff |
| `tls_https` | HTTPS/TLS fundamentals |

### Networking Libraries

| Subtopic ID | Display Name |
| --- | --- |
| `okhttp` | OkHttp |
| `okhttp_interceptors` | OkHttp interceptors |
| `retrofit` | Retrofit |
| `retrofit_coroutines` | Retrofit and coroutines |
| `network_error_handling` | Network error handling |
| `ktor_client` | Ktor Client |
| `retrofit_vs_ktor` | Retrofit vs Ktor |

### Serialization

| Subtopic ID | Display Name |
| --- | --- |
| `json_fundamentals` | JSON fundamentals |
| `gson` | Gson |
| `moshi` | Moshi |
| `kotlinx_serialization` | kotlinx.serialization |
| `serialization_strategies` | Reflection vs generated/compiler-assisted serialization |
| `custom_serialization` | Custom serializers/adapters |
| `json_compatibility` | Missing, unknown, nullable, and default JSON fields |

### Advanced

| Subtopic ID | Display Name |
| --- | --- |
| `network_caching` | HTTP/network caching |
| `websockets` | WebSockets |
| `streaming` | Streaming concepts |
| `authentication_tokens` | Authentication tokens |
| `token_refresh` | Token refresh flows |

## Background Work & OS Constraints

Stable topic ID: `background_work`

Scope: Android background execution limits, services, WorkManager, alarms,
battery constraints, and choosing appropriate background APIs.

| Subtopic ID | Display Name |
| --- | --- |
| `background_execution` | Android background execution model |
| `services` | Services |
| `foreground_services` | Foreground services |
| `bound_services` | Bound services |
| `workmanager` | WorkManager fundamentals |
| `workmanager_constraints` | WorkManager constraints |
| `workmanager_chaining` | Work chaining |
| `workmanager_retries` | WorkManager retries |
| `service_vs_workmanager` | Service vs WorkManager |
| `alarmmanager` | AlarmManager |
| `exact_alarms` | Exact alarms |
| `doze` | Doze |
| `app_standby` | App Standby |
| `battery_restrictions` | Battery restrictions |
| `background_limits` | Background execution limits |
| `background_process_death` | Process-death implications |
| `boot_restart_work` | Work across reboot/restart |
| `long_running_work` | Long-running work |
| `foreground_service_types` | Foreground-service types and permissions |
| `background_api_selection` | Choosing the appropriate background API |

## Notifications & Push Messaging

Stable topic ID: `notifications`

Scope: Android notifications, notification permissions, PendingIntent behavior,
Firebase Cloud Messaging, and push delivery architecture.

### Notifications

| Subtopic ID | Display Name |
| --- | --- |
| `notification_fundamentals` | Android notification fundamentals |
| `notification_channels` | Notification channels |
| `notification_importance` | Notification importance |
| `notification_pending_intents` | PendingIntent in notifications |
| `notification_actions` | Notification actions |
| `notification_grouping` | Notification grouping |
| `notification_permission` | Runtime notification permission |

### FCM

| Subtopic ID | Display Name |
| --- | --- |
| `fcm_fundamentals` | Firebase Cloud Messaging fundamentals |
| `fcm_tokens` | FCM registration tokens |
| `fcm_notification_messages` | Notification messages |
| `fcm_data_messages` | Data messages |
| `fcm_app_state_behavior` | Foreground/background behavior |
| `fcm_token_refresh` | Token refresh |
| `fcm_message_handling` | Message handling |
| `notification_deep_links` | Deep linking from notifications |
| `push_delivery_constraints` | Push delivery limitations |
| `fcm_priority` | FCM message priority |
| `push_system_architecture` | Backend-to-FCM-to-device architecture |

## Testing & Testability

Stable topic ID: `testing`

Scope: Test strategy, local and instrumented tests, test doubles, coroutine and
Flow testing, UI testing, and architecture for testability.

| Subtopic ID | Display Name |
| --- | --- |
| `testing_strategy` | Test strategy |
| `unit_testing` | Unit tests |
| `integration_testing` | Integration tests |
| `ui_testing` | UI tests |
| `local_vs_instrumented_tests` | Local vs instrumented tests |
| `junit` | JUnit |
| `test_doubles` | Test doubles |
| `fakes_vs_mocks` | Fakes, mocks, and stubs |
| `viewmodel_testing` | ViewModel testing |
| `coroutine_testing` | Coroutine testing |
| `flow_testing` | Flow testing |
| `run_test` | runTest |
| `virtual_time` | Virtual time |
| `repository_testing` | Repository testing |
| `room_testing` | Room/database testing |
| `compose_ui_testing` | Compose UI testing |
| `espresso` | Espresso awareness |
| `test_di` | Dependency replacement/injection in tests |
| `test_determinism` | Deterministic tests |
| `architecture_for_testability` | Architecture for testability |
| `testing_boundaries` | What to test and where |
| `flaky_tests` | Flaky-test prevention/debugging |

## Performance, Memory & Debugging

Stable topic ID: `performance`

Scope: Android memory behavior, leak patterns, runtime performance, rendering,
profiling, debugging tools, and optimization techniques.

### Memory

| Subtopic ID | Display Name |
| --- | --- |
| `android_memory_model` | Android/JVM memory fundamentals |
| `memory_leaks` | Memory leaks |
| `context_leaks` | Context leaks |
| `lifecycle_leaks` | Activity/Fragment/listener leaks |
| `coroutine_leaks` | Coroutine lifetime leaks |
| `garbage_collection` | Garbage collection fundamentals |

### Performance

| Subtopic ID | Display Name |
| --- | --- |
| `anr` | ANRs |
| `main_thread_performance` | Main-thread performance |
| `rendering_jank` | Rendering and jank |
| `startup_performance` | App startup |
| `lazy_initialization` | Lazy initialization |
| `recyclerview_performance` | RecyclerView performance |
| `compose_performance` | Compose performance |
| `compose_recomposition_performance` | Avoiding unnecessary recomposition |

### Tooling

| Subtopic ID | Display Name |
| --- | --- |
| `android_profiler` | Android Studio profiling fundamentals |
| `cpu_profiling` | CPU profiling |
| `memory_profiling` | Memory profiling |
| `network_inspection` | Network inspection |
| `layout_inspector` | Layout Inspector / Compose inspection |
| `strictmode` | StrictMode |

### Optimization

| Subtopic ID | Display Name |
| --- | --- |
| `r8` | R8 |
| `baseline_profiles` | Baseline Profiles |
| `macrobenchmark` | Macrobenchmark awareness |

## Security, Privacy & Permissions

Stable topic ID: `security`

Scope: Android security boundaries, permissions, component exposure, secure
storage, network security, authentication-token handling, and privacy
principles.

| Subtopic ID | Display Name |
| --- | --- |
| `android_sandbox` | Android application sandbox |
| `android_permissions` | Permission model |
| `runtime_permissions` | Runtime permissions |
| `permission_ux` | Permission-request UX |
| `exported_components` | Exported components |
| `intent_security` | Intent security |
| `pending_intent_security` | PendingIntent security |
| `content_provider_security` | ContentProvider security |
| `secure_storage` | Secure local storage |
| `android_keystore` | Android Keystore |
| `encryption_fundamentals` | Encryption fundamentals |
| `secret_management` | API keys and secret management |
| `network_security` | Network security |
| `certificate_pinning` | Certificate pinning and trade-offs |
| `auth_token_security` | Authentication-token security |
| `biometric_auth` | Biometric authentication awareness |
| `webview_security` | WebView security |
| `sensitive_logging` | Sensitive-data logging |
| `compromised_devices` | Rooted/compromised-device limitations |
| `privacy_principles` | Privacy and data-minimization principles |

## Build System, Modularization & Delivery

Stable topic ID: `build_delivery`

Scope: Gradle, Android build concepts, dependency configuration,
modularization, packaging, release mechanics, and delivery pipelines.

### Build

| Subtopic ID | Display Name |
| --- | --- |
| `gradle_fundamentals` | Gradle fundamentals |
| `gradle_wrapper` | Gradle Wrapper |
| `gradle_plugins` | Gradle plugins |
| `dependency_configurations` | Dependency configurations |
| `version_catalogs` | Version catalogs |
| `android_gradle_plugin` | Android Gradle Plugin |
| `kotlin_gradle_plugin` | Kotlin Gradle plugin |
| `build_variants` | Build variants |
| `build_types` | Build types |
| `product_flavors` | Product flavors |
| `source_sets` | Source sets |
| `dependency_resolution` | Dependency resolution |

### Modularization

| Subtopic ID | Display Name |
| --- | --- |
| `android_modules` | Application/library modules |
| `feature_modularization` | Feature modularization |
| `module_dependency_direction` | Module dependency direction |
| `api_vs_implementation` | api vs implementation |
| `modularization_tradeoffs` | Benefits and costs of modularization |

### Delivery

| Subtopic ID | Display Name |
| --- | --- |
| `apk_vs_aab` | APK vs Android App Bundle |
| `app_signing` | Application signing |
| `release_builds` | Debug vs release |
| `code_shrinking` | Code shrinking/obfuscation |
| `resource_shrinking` | Resource shrinking |
| `app_versioning` | versionCode/versionName |
| `ci_fundamentals` | CI fundamentals |
| `cd_fundamentals` | Delivery/release pipeline fundamentals |

## Mobile System Design

Stable topic ID: `mobile_system_design`

Scope: Mobile-focused system design, trade-off analysis, data flow,
offline/sync behavior, resource constraints, rollout, and testing strategy.

| Subtopic ID | Display Name |
| --- | --- |
| `requirements_analysis` | Requirements clarification |
| `nonfunctional_requirements` | Non-functional requirements |
| `system_decomposition` | Client/system decomposition |
| `system_data_flow` | Data flow |
| `local_remote_sources` | Local vs remote sources of truth |
| `system_caching` | Caching strategy |
| `offline_design` | Offline behavior |
| `data_synchronization` | Data synchronization |
| `conflict_resolution` | Conflict resolution |
| `system_pagination` | Pagination design |
| `system_retries` | Error and retry design |
| `background_sync` | Background synchronization |
| `push_driven_updates` | Push-driven updates |
| `session_management` | Authentication/session handling |
| `large_scale_modularization` | Modularization at scale |
| `client_scalability` | Client-code scalability |
| `mobile_resource_constraints` | Battery/network/device constraints |
| `observability` | Logging/observability considerations |
| `feature_flags` | Feature flags |
| `backward_compatibility` | Backward compatibility |
| `api_evolution` | API evolution |
| `rollout_strategy` | Rollout and migrations |
| `system_security` | Security/privacy in system design |
| `system_testing_strategy` | Testing strategy for designed systems |

## Kotlin Multiplatform & Compose Multiplatform

Stable topic ID: `kmp`

Scope: Kotlin Multiplatform and Compose Multiplatform fundamentals,
architecture, shared/platform boundaries, lifecycle/navigation, resources, and
ecosystem trade-offs.

### Fundamentals

| Subtopic ID | Display Name |
| --- | --- |
| `kmp_fundamentals` | Kotlin Multiplatform fundamentals |
| `kmp_targets` | Targets |
| `kmp_source_sets` | Source sets |
| `commonmain` | commonMain |
| `kmp_dependency_hierarchy` | Source-set/dependency hierarchy |
| `shared_vs_platform_code` | What to share vs keep platform-specific |
| `expect_actual` | expect/actual |
| `kmp_platform_apis` | Accessing platform APIs |
| `kotlin_native` | Kotlin/Native awareness |

### Architecture

| Subtopic ID | Display Name |
| --- | --- |
| `kmp_architecture` | KMP architecture |
| `shared_domain` | Shared domain logic |
| `shared_data` | Shared data layer |
| `shared_presentation` | Shared presentation logic |
| `kmp_dependency_inversion` | Cross-platform dependency inversion |
| `platform_implementations` | Platform-specific implementations |

### Compose Multiplatform

| Subtopic ID | Display Name |
| --- | --- |
| `compose_multiplatform` | Compose Multiplatform fundamentals |
| `compose_multiplatform_resources` | Compose Multiplatform Resources |
| `kmp_lifecycle_viewmodel` | Multiplatform Lifecycle/ViewModel |
| `kmp_navigation` | Multiplatform navigation |
| `platform_ui_interop` | Platform UI interoperability |

### Ecosystem

| Subtopic ID | Display Name |
| --- | --- |
| `kmp_library_selection` | Evaluating KMP library compatibility |
| `kotlinx_serialization_kmp` | kotlinx.serialization in KMP |
| `ktor_kmp` | Ktor in KMP |
| `room_kmp` | Room KMP |
| `sqldelight` | SQLDelight |
| `koin_kmp` | Koin in KMP |

### Trade-offs

| Subtopic ID | Display Name |
| --- | --- |
| `kmp_tradeoffs` | Practical KMP trade-offs |
