# E15-03 Question Level Classification Report

This report records the complete-bank editorial review performed for E15-03 on
2026-09-01. The review covered all 399 bundled questions: 360 `ACTIVE` and 39
`DEPRECATED`. Each stem, answer option, and correct-answer set was reviewed
against the Question Interview Level contract in `docs/content/content-authoring.md`.

The counts include both active and deprecated bundled questions. They describe
the bank as classified; no target distribution or quota was used.

## Overall Distribution

| Level | Questions |
| --- | ---: |
| `FOUNDATION` | 238 |
| `APPLIED` | 147 |
| `ADVANCED` | 14 |
| **Total** | **399** |

For audit completeness, the active bank contains 205 Foundation, 141 Applied,
and 14 Advanced questions. The deprecated bank contains 33 Foundation and 6
Applied questions.

## Distribution by Topic

| Topic | `FOUNDATION` | `APPLIED` | `ADVANCED` | Total |
| --- | ---: | ---: | ---: | ---: |
| Android Platform & Application Model | 13 | 4 | 0 | 17 |
| Lifecycle, State & Navigation | 19 | 8 | 0 | 27 |
| UI - Views & Jetpack Compose | 23 | 8 | 0 | 31 |
| Kotlin Language & JVM Fundamentals | 25 | 2 | 0 | 27 |
| Coroutines, Flow & Reactive Programming | 24 | 15 | 4 | 43 |
| Application Architecture & Design Principles | 11 | 15 | 0 | 26 |
| Dependency Injection | 18 | 9 | 0 | 27 |
| Local Persistence & Offline Data | 17 | 4 | 1 | 22 |
| Networking & Serialization | 12 | 11 | 1 | 24 |
| Background Work & OS Constraints | 14 | 5 | 1 | 20 |
| Notifications & Push Messaging | 7 | 7 | 0 | 14 |
| Testing & Testability | 8 | 16 | 0 | 24 |
| Performance, Memory & Debugging | 14 | 10 | 0 | 24 |
| Security, Privacy & Permissions | 9 | 6 | 3 | 18 |
| Build System, Modularization & Delivery | 12 | 7 | 0 | 19 |
| Mobile System Design | 1 | 13 | 4 | 18 |
| Kotlin Multiplatform & Compose Multiplatform | 11 | 7 | 0 | 18 |

## Ambiguous Boundary Decisions

| Question ID | Assigned | Competing | Boundary decision |
| --- | --- | --- | --- |
| `workmanager_constraints_001` | `FOUNDATION` | `APPLIED` | The scenario names process death and connectivity, but one direct WorkManager persistence-and-constraints contract is sufficient to eliminate every distractor. |
| `compose_phases_deferred_state_read` | `APPLIED` | `FOUNDATION` | The lambda overload's contract is documented, but the question requires applying its phase-specific state read to a concrete animation-performance symptom. |
| `architecture_ui_event_consumption` | `APPLIED` | `ADVANCED` | Configuration recreation and event delivery interact, but identifying replay as the cause is a direct diagnosis rather than a multi-mechanism event architecture exercise. |
| `http_cache_control_revalidation` | `ADVANCED` | `APPLIED` | The result depends on combining freshness directives, mandatory revalidation, and ETag validator behavior rather than selecting one isolated HTTP feature. |
| `biometric_prompt_keystore_binding` | `ADVANCED` | `APPLIED` | The answer requires comparing an app-observed callback with authentication enforced at the cryptographic-key boundary and tracing the resulting trust difference. |
| `system_design_conflict_resolution_policy` | `APPLIED` | `ADVANCED` | Although the scenario is architectural, the actual options require one direct conflict-policy choice; they do not require designing merge, ordering, and recovery together. |

These decisions use the rubric's lower-level preference where deeper reasoning
would be educational but is not necessary to identify and justify the authored
answer set.
