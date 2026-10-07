# Lifecycle, State & Navigation learning implementation

Content baseline: `629a390638f15fd7fc35fbb383a3d66b9c0e6efa`. The authoritative blueprint is retained in [lifecycle-state-navigation-learning-blueprint.md](lifecycle-state-navigation-learning-blueprint.md).

## Learning program

Four Android core Units and ten Lessons use the blueprint’s proposed stable identities. They follow the Kotlin Units and precede the optional KMP tail. Original content and relative order are preserved.

| Unit ID | Title | Lessons |
| --- | --- | ---: |
| `unit_component_lifetimes_and_visibility` | Component Lifetimes and Visibility | 3 |
| `unit_recreation_retention_and_restoration` | Recreation, Retention and Restoration | 3 |
| `unit_navigation_state_and_destination_lifetime` | Navigation State and Destination Lifetime | 2 |
| `unit_external_entry_and_back_navigation` | External Entry and Back Navigation | 2 |

| Lesson ID | Title | Primary Subtopics |
| --- | --- | --- |
| `lesson_activity_lifecycle_visibility_and_work` | Activity Lifecycle: Visible, Interactive and Gone | `activity_lifecycle` |
| `lesson_fragment_and_view_lifecycles` | A Fragment Has an Instance Lifecycle and a View Lifecycle | `fragment_lifecycle` |
| `lesson_lifecycle_aware_work_and_collection` | Lifecycle-Aware Work: Follow the Owner, Not the Screen Name | `lifecycle_aware_apis` |
| `lesson_recreation_configuration_process_and_dismissal` | Configuration Change, Process Death and Permanent Dismissal | `configuration_changes`, `process_death` |
| `lesson_viewmodel_owner_and_scope` | ViewModel Lifetime Is the Lifetime of Its Owner | `viewmodel_lifecycle` |
| `lesson_saved_state_and_reconstruction` | Saved State Is a Reconstruction Hint, Not a Database | `saved_state` |
| `lesson_navigation_back_stacks_tasks_and_entry_lifetimes` | Back Stacks, Tasks and Entry Lifetimes | `navigation_fundamentals` |
| `lesson_navigation_2_vs_3_state_ownership` | Navigation 2 and Navigation 3: Who Owns the Stack? | `navigation_2_vs_3` |
| `lesson_deep_links_app_links_and_reconstruction` | Deep Links and App Links: Entering the Right State | `deep_links` |
| `lesson_back_navigation_and_predictive_back` | Back Is a Navigation Transition, Including Predictive Back | `back_handling` |

## Blueprint interpretation

The blueprint’s `local_data` and `security` bridge labels identify Topics, not existing Subtopics. The bounded storage and input-trust explanations remain in their Lessons; the labels are omitted from Supporting metadata rather than inventing taxonomy. No other Unit/Lesson identity or conceptual deviation was needed.

## Independent review and corrections

The author, fresh read-only reviewer and correction editor are separate for each sequential batch. Ordinary findings are accepted; accepted prose is retained unless a substantive correction is needed.

- Batch 1: reviewer accepted all three Lessons and confirmed five primary questions are taught. Coordinating review moved the unchanged short/main-thread onPause paragraph into Core, matching the blueprint’s Foundation depth. Other text remained unchanged; the editor verified the exact final version.
- Batch 2: recreation and ViewModel Lessons accepted unchanged. Saved-state corrections qualified CreationExtras/createSavedStateHandle as the current new-factory path rather than the sole possible mechanism, and stated attached-Fragment/main-thread use, including main-thread updates while stopped. The editor rechecked current official APIs and all ten primary questions plus the supporting graph-scope question.
- Batch 3: both Lessons accepted. The editor replaced a remote “This” antecedent with explicit singleTask wording; all other text was preserved. Navigation 3 1.2.0 and Lifecycle 2.11.0 source revisions were independently verified against stable release endpoints.
- Batch 4: predictive Back accepted. The editor corrected the restricted link parser to use literal encoded authority and complete encoded-path matching, aligned the policy explanation, and added the official Uri reference. Normal and adversarial requests were source-traced; no Android execution is claimed.
- Integrated review accepted the full Topic after one source-anchor correction. All 23 existing ACTIVE questions have actual teaching support. The separate editor independently verified the corrected Compose section and proved all remaining learning content unchanged.

The assessment review rejected two candidates: verified-link input/access trust is already assessed by `exported_component_external_input_validation` under Security, and Activity resource pairing repeats existing visibility coverage. Four distinct questions remain. No extra owner/threshold, recreation, saved-state, producer/collector, notification-history or stack-policy variants were added.

The final editor moved two existing blocks unchanged into Core: the Navigation 3 keys/saveable-values/ViewModel-owner paragraph and the predictive cancellation warning. This supports the new Foundation questions without expanding the Lessons. The exact integrated content is frozen at SHA-256 `2f6758040bca395507cd396cfdb22b438a21fa06f515152cdd23f832e8ceef33`.

## Assessment result

| Question ID | Primary Subtopic | Level | Distinct reasoning |
| --- | --- | --- | --- |
| `fragment_captured_binding_callback_cleanup` | `fragment_lifecycle` | Applied | Clearing a field does not invalidate a separately captured binding; end registration and invalidate queued old-View deliveries. |
| `navigation3_keys_and_entry_state_scopes` | `navigation_2_vs_3` | Foundation | Saved history keys, entry saveable UI values and entry ViewModel stores are separate responsibilities. |
| `external_target_account_history_after_sign_in` | `deep_links` | Applied | Preserve the intended target through sign-in, check the new account, and construct the required return history. |
| `predictive_back_cancel_preserves_history` | `back_handling` | Foundation | Cancelled progress resets the preview and preserves history; normal completion commits. |

Three existing records received bounded corrections:

- `predictive_back_handler_registration`: callback participation is separate from preview rendering. Its existing options/key remain fixed; their preexisting distractor weakness is recorded rather than claimed to have been newly repaired.
- `navigation_pop_up_to_inclusive`: inclusive removal can retain entries below SignIn; minimum sufficient classification is Foundation.
- `process_death_reproducing_restoration`: finishing one Activity need not end its task. The stem now explicitly defines a verified task/Recents-preserving background process kill, excluding force-stop; additional task/developer-option sources support the details. Minimum sufficient classification is Foundation.

Every shipped answer text, answer ID, key and selection mode is unchanged. Of the original 489 Question objects, 486 are unchanged; only these three scoped records differ. Four new SINGLE questions bring the bank to 493 total / 452 ACTIVE / 41 DEPRECATED. Lifecycle has 27 ACTIVE questions (19 Foundation, 8 Applied, 0 Advanced); its four Unit practice pools contain 6, 10, 6 and 5 questions. Supporting bridges do not expand those practice pools. Existing security coverage is semantic cross-bank support, not an extra Lifecycle practice question.

## Validation and review limits

Publication base: `4f9cf13649a5bb7cb71e763dc90478b743cf039c`, after a clean rebase over two unrelated main commits. Both curriculum baselines were verified unchanged by those commits before integration.

- Actual production Kotlin 2.4.10 models/codecs/validators and both encode/decode round trips: PASS, zero errors, 40 Units / 157 Lessons and 493 Questions. The standalone runner uses production sources and exact serialization 1.11.0; it is not a substitute for Gradle/JVM/UI execution.
- `python3 tools/learning_question_coverage.py --write` and `python3 tools/question_bank_coverage.py --write`: generated snapshots updated; both `--check` commands PASS. Human coverage prose was reconciled with current counts and scoped outcomes.
- `python3 tools/question_bank_identity.py --against 4f9cf13649a5bb7cb71e763dc90478b743cf039c`: PASS.
- `python3 -m unittest discover -s tools -p 'test_*.py'`: 111 tests PASS on the final bundles.
- Whole-bank mechanical anti-cue check: 188/449 single-key questions nominally longest, mean ratio 1.03, zero above the 10% limit; absolute-word counts occur in keyed and incorrect answers. This is not a semantic audit of unrelated questions.
- `git diff --check`: PASS. Exact guards preserve every original learning-curriculum byte outside the insertion, all old learning objects, and all original assessment objects except the three explicitly allowed records. Application code, dependencies, schemas, taxonomy and unrelated content are unchanged.
- 66 distinct final citations have successful URL/body/anchor checks across the author, reviewer, editor and coordinating stages. Decisive claims were read independently; HTTP 200 was not treated as semantic proof. Four coordinating API/guide fetches timed out transiently; separate successful independent retrievals supplied actual contracts. Those failed observations were retained rather than relabelled successful. Full-SHA AndroidX source references were verified against stable release endpoints (Navigation 3 1.2.0, Lifecycle 2.11.0, Activity 1.13.0), and the bundled-source guard now also covers Gitiles links.

Required local Gradle command attempted:

```sh
./gradlew :shared:jvmTest --tests '*InitialCurriculum*' --tests '*CurriculumValidator*' --tests '*BundledLearningCurriculum*' --tests '*LearningCurriculumValidator*' --tests '*LearningUnitPracticeIntegration*' --tests '*TopicDetailLearningContent*' --tests '*TopicBrowserViewModel*'
```

It was blocked before test execution by the Gradle 9.1.0 wrapper download: `java.net.SocketException: Network is unreachable`. No failed test is claimed and the unchanged prerequisite was not retried. Exact-head CI evidence is recorded in the draft pull request after publication; baseline main CI is not used as this branch's validation. No Android device/emulator execution of teaching examples, live App Link association or gesture behavior is claimed, and no iOS validation is inferred from CI.

The fresh final blind gate independently solved all seven final touched questions before opening keys/levels. All derived answers matched, all hard semantic gates passed, and correctness, uniqueness, source support, minimum-sufficient classification and teaching depth were accepted at HIGH confidence. The scoped result is recorded in [question-audit-log.yml](question-audit-log.yml); registration retains its preexisting non-hard Q6 distractor limitation.


## Existing-question teaching support

The fresh integrated reviewer read the complete ten-Lesson program, all 23 existing ACTIVE Lifecycle questions and relevant neighboring material. Lesson positions below follow the blueprint’s Unit/Lesson order. This is semantic teaching support, separate from taxonomy reachability and the scoped assessment corrections.

| Question | Teaching home/depth | Semantic finding |
| --- | --- | --- |
| activity_lifecycle_001 | L1.1 Core/Practical | PASS: main-thread pause, transition delay, short release actions, separate durability. |
| activity_on_pause_vs_on_stop_visibility | L1.1 Core | PASS: separate dialog-themed Activity, partial visibility, pause without stop; no confusing an in-Activity dialog. |
| fragment_view_lifecycle_collection | L1.2 Core/Practical; L1.3 Core | PASS: current View owner ends before retained Fragment instance; observation and field cleanup are separate. |
| fragment_back_stack_view_destroyed_binding | L1.2 Core/Practical | PASS: specific replacement-back-stack trace, detached widgets, retained fields/store, new View on return. |
| lifecycle_repeat_on_lifecycle | L1.3 Core/Practical | PASS: correct View scope plus STARTED threshold; cancel/restart and fresh invocation, not suspended collection. |
| configuration_changes_001 | L2.1 Core/Practical | PASS: default unhandled recreation versus declared manual handling; normally retained process. |
| configuration_change_vs_process_recreation | L2.1 Core/Practical | PASS: new UI with retained store versus wholly new heap and reconstruction. |
| process_death_reproducing_restoration | L2.1 Practical | PASS: genuinely ended background process, preserved task, Recents return, am kill eligibility and verification. Final clarification and explanation correction recorded above. |
| viewmodel_destination_scope | L2.2 Practical | PASS: independent destination owners, wider Activity alternatives, permanent-entry cleanup. |
| viewmodel_store_configuration_retention | L2.2 Core | PASS: retained keyed store, new UI, no serialized ViewModel or retained Activity. |
| viewmodel_clear_owner_finish | L2.2 Core | PASS: normal permanent finish clears store versus configuration retention. |
| saved_state_binder_transaction_limit | L2.3 Core/Practical | PASS: shared limited Binder buffer, size/serialization costs, small reconstruction hints, no exact safe quota. |
| saved_state_transient_inputs | L2.3 Practical | PASS: query/ID versus catalog, audit log and media cache, with required fresh-launch recovery distinguished. |
| savedstatehandle_process_recreation | L2.3 Core/Practical | PASS: registry-connected handle, eligible captured snapshot, new instance; not replayed original arguments or automatic DataStore. |
| remember_saveable_vs_viewmodel_ownership | L2.3 Practical plus bounded Compose context | PASS: local expanded flag versus repository-driven screen holder, without re-teaching Compose state. |
| navigation_fundamentals_001 | L3.1 Core | PASS: history of occurrences versus declaration and Activity task. |
| activity_launch_mode_task_stack | L3.1 Core/Practical | PASS: existing singleTask reuse clears Activities above and receives Intent; inner stack requires separate policy. |
| navigation_graph_viewmodel_shared_scope | L2.2 Practical; L3.1 Practical | PASS: concrete shared checkout entry survives child pop, permanent graph removal starts later checkout clean. |
| navigation_back_stack_entry_lifetime | L3.1 Core/Senior | PASS: permanent entry destruction/clearing, later occurrence new, transition and saved-stack qualifications. |
| navigation3_back_stack_ownership | L3.2 Core/Practical | PASS: explicit app keys outside destinations versus controller-managed history; saving not inferred from ordinary memory alone. |
| deep_link_app_link_verification | L4.1 Core | PASS: autoVerify association and assetlinks package/certificate; user/device conditions and no security shortcut. |
| predictive_back_handler_registration | L4.2 Core/Practical | PASS concept support: advance participation/enabled-state decision versus legacy commit-time interception. Final explanation separates participation from preview rendering. |
| navigation_pop_up_to_inclusive | L4.2 Practical | PASS: named entry plus above removed, lower entries retained, launchSingleTop not history removal. Final explanation preserves lower history entries. |

## Review questions

- Which state survives configuration recreation, and which state must be reconstructed after process loss?
- Why are saved Navigation 3 keys separate from entry saveable state and ViewModel stores?
- What changes during predictive progress, and what can change only after commitment?
