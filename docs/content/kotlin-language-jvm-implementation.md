# Kotlin/JVM learning implementation

Authored content baseline: `c4dc91325ce15a9a27b508b4b27a4d676c932083`. Before final integration, the branch was brought forward to `140bcb5503eb4416cd6df64dea330683a1c0e626`, preserving two nonoverlapping upstream UI quality changes.

## Learning program

Four Units and nine Lessons implement the attached blueprint without a conceptual or identity deviation. The Units follow the existing core Units and precede the optional KMP tail; all original Units and Lessons retain their relative order and content.

| Unit ID | Title |
| --- | --- |
| `unit_kotlin_types_and_callables` | Types and Callable Semantics |
| `unit_kotlin_objects_and_state` | Objects and State Modeling |
| `unit_kotlin_generic_api_mechanics` | Generic APIs and Compiler-Assisted Mechanics |
| `unit_kotlin_java_jvm_boundary` | Kotlin at the Java/JVM Boundary |

| Lesson ID | Title |
| --- | --- |
| `lesson_kotlin_nullability_and_smart_casts` | Nullability, Smart Casts and Type Refinement |
| `lesson_kotlin_functions_lambdas_and_extensions` | Functions, Lambdas, Receivers and Extensions |
| `lesson_kotlin_classes_interfaces_and_objects` | Classes, Interfaces, Visibility and Objects |
| `lesson_kotlin_properties_and_delegation` | Properties, Accessors and Delegation |
| `lesson_kotlin_data_equality_and_sealed_models` | Data Classes, Equality and Sealed Models |
| `lesson_kotlin_generics_and_variance` | Generics, Variance and Type Erasure |
| `lesson_kotlin_inline_and_reified_types` | Inline Functions, Lambda Control Flow and Reified Types |
| `lesson_kotlin_collections_and_sequences` | Collections, Mutability and Sequences |
| `lesson_kotlin_java_jvm_boundary` | Kotlin at the Java/JVM Boundary |

The authoritative blueprint is retained in [kotlin-language-jvm-learning-blueprint.md](kotlin-language-jvm-learning-blueprint.md). Exact Primary and Supporting mappings are protected by the bundled learning tests. No taxonomy, schema or application behavior changed.

## Independent review and editorial disposition

- Batch 1: moved the callable receiver/extension explanation needed for Foundation reasoning into Core; replaced two interview-checklist conclusions with direct technical conclusions.
- Batch 2: qualified the public-signature restriction by effective external visibility, since a public member inside an internal class does not expose its type outside the module. Moved the successful default synchronized-lazy guarantee beside its Core definition. The data/equality/sealed Lesson was accepted unchanged.
- Batch 3: all three Lessons were accepted unchanged by the independent reviewer and separate editor.
- Batch 4: split one dense paragraph about erasure, internal visibility and companion members into three, and made the Java/reflection access-barrier statement explicit.
- Fresh integrated review: accepted all four summaries and nine Lessons as written, finding no required cross-Lesson correction. The coordinating editor accepted that disposition.

No reviewer recommendation was rejected. The author, independent reviewer and correction roles were distinct; reviewers did not edit production content.

## Assessment expansion

Eight ACTIVE SINGLE questions were appended. All are APPLIED by the minimum-sufficient-reasoning test; there was no level or count target. Kotlin now has 33 ACTIVE questions: 23 FOUNDATION, 10 APPLIED, 0 ADVANCED. The full bank has 488 questions (447 ACTIVE, 41 DEPRECATED).

| Question ID | Subtopic | Level | Coverage gap / minimum reasoning |
| --- | --- | --- | --- |
| `kotlin_smart_cast_single_getter_read` | `kotlin_nullability` | APPLIED | Apply custom-getter instability and Elvis/local-value handling to satisfy a one-read formatting contract. |
| `kotlin_callback_registration_snapshot` | `kotlin_functions` | APPLIED | Apply capture semantics at two different times to preserve the registration selection while deferring the side effect. |
| `kotlin_final_entry_open_hook` | `kotlin_classes` | APPLIED | Combine final-entry enforcement with an abstract polymorphic hook to preserve validation while allowing subclass behavior. |
| `kotlin_optional_replaceable_property` | `kotlin_properties` | APPLIED | Apply initialization and mutability contracts to valid initial absence, replacement, and sign-out removal. |
| `kotlin_projected_array_transfer` | `kotlin_generics` | APPLIED | Derive read/write projections and array substitutions from the source subtype and destination supertypes. |
| `kotlin_historical_element_snapshot` | `kotlin_collections` | APPLIED | Choose eager immutable element conversion to preserve membership and historical values rather than an alias, shallow membership copy, or delayed observation. |
| `kotlin_java_companion_default_factory` | `kotlin_java_interop` | APPLIED | Combine Java static placement and generated default-argument arity to provide the stated Java call forms. |
| `kotlin_platform_absence_adapter` | `kotlin_java_interop` | APPLIED | Apply nullable boundary modeling to preserve valid absence as distinct from an empty name and require consumer handling. |

The fresh reviewer saved all nine independent solutions before opening keys or author metadata; all matched. Every retained question received individual option analysis and final Q1–Q20 PASS judgments with HIGH confidence. The separate editor revalidated the exact final text. No retained question remains REVIEW_REQUIRED or REPLACE.

- Rejected proposal: `kotlin_interface_default_composition`. The technically correct Foundation item reduced to qualified-super syntax and closely reproduced the Lesson example; it did not add the intended behavioral-design reasoning. It was dropped rather than padded or classified higher.
- Corrected `kotlin_historical_element_snapshot`: removed the true but uncited concurrency sentence, which was unnecessary under the stated single-thread premise, and named `toList()` precisely. Its key, options, level and sources remain unchanged.
- Seven retained proposals were accepted unchanged. No reviewer recommendation was rejected.

All 17 accepted-question citation records (15 distinct URLs) are VERIFIED against opened official documentation. Unchanged claims retain the independent reviewer’s verification; the editor independently reopened the three snapshot sources after correction:

| Citation URL | Final status |
| --- | --- |
| https://kotlinlang.org/docs/classes.html#abstract-classes | VERIFIED |
| https://kotlinlang.org/docs/collection-transformations.html#map | VERIFIED |
| https://kotlinlang.org/docs/constructing-collections.html#copy | VERIFIED |
| https://kotlinlang.org/docs/delegated-properties.html#lazy-properties | VERIFIED |
| https://kotlinlang.org/docs/generics.html#use-site-variance-type-projections | VERIFIED |
| https://kotlinlang.org/docs/inheritance.html#overriding-methods | VERIFIED |
| https://kotlinlang.org/docs/java-interop.html#null-safety-and-platform-types | VERIFIED |
| https://kotlinlang.org/docs/java-to-kotlin-interop.html#handling-signature-clashes-with-jvmname | VERIFIED |
| https://kotlinlang.org/docs/java-to-kotlin-interop.html#overloads-generation | VERIFIED |
| https://kotlinlang.org/docs/java-to-kotlin-interop.html#static-methods | VERIFIED |
| https://kotlinlang.org/docs/lambdas.html#closures | VERIFIED |
| https://kotlinlang.org/docs/null-safety.html | VERIFIED |
| https://kotlinlang.org/docs/properties.html#late-initialized-properties-and-variables | VERIFIED |
| https://kotlinlang.org/docs/sequences.html | VERIFIED |
| https://kotlinlang.org/docs/typecasts.html#smart-cast-prerequisites | VERIFIED |

## Validation

Final integrated checks:

| Command / check | Result |
| --- | --- |
| `python3 tools/question_bank_coverage.py --write` and `python3 tools/learning_question_coverage.py --write` | Both committed coverage documents regenerated; human Kotlin coverage/empty-Subtopic notes reconciled |
| Both coverage tools with `--check` | PASS |
| `python3 tools/question_bank_identity.py --against 140bcb5503eb4416cd6df64dea330683a1c0e626` | PASS; shipped question identities, answer/key sets and modes unchanged |
| `python3 -m unittest discover -s tools -p 'test_*.py'` | All 89 tests PASS |
| Actual production Kotlin codecs/validators and round trips, scratch standalone runner | PASS: 17 Topics, 361 Subtopics, 488 questions / 447 ACTIVE, 36 Units, 147 Lessons; zero errors |
| Original-record comparison and focused insertion checks | All original 138 Lessons and 480 questions, taxonomy, metadata and relative order preserved; existing curriculum text retained outside insertion chunks |
| Playbook Part 3 whole-bank anti-cue audit | 187/444 keyed-longest (42%), mean ratio 1.03; zero over 10%; absolutes 0.23/distractor and 0.13/keyed; positions 27/28/26/19/1 percent |
| Same audit, accepted eight-question subset | 4/8 keyed-longest, mean ratio 1.04; zero over 10%; absolutes 0.04/distractor and 0.00/keyed; positions 38/25/25/12 percent |
| Same audit, all-status Kotlin subset | 12/34 keyed-longest, mean ratio 1.01; zero over 10%; absolutes 0.16/distractor and 0.17/keyed; positions 22/22/33/22 percent |
| Playbook Part 9 neighbor scan against original content baseline | One pair inspected: optional-property and platform-absence questions. Consistent null modeling; distinct lifecycle versus Java-boundary decisions, no contradiction or duplicate reasoning |
| Source checks, accepted question subset | Every retained citation covered by passing URL/fragment/body records; 13 added exact URLs/fragments plus two newly covered base pages |
| `git diff --check` | PASS |

The eight new questions ask for one decision each, so SINGLE is appropriate. No wording was padded to improve audit metrics. The one absolute word in a new distractor does not create a batch-wide strategy, and the Kotlin/whole-bank audits retain keyed absolute words.

Question authoring included successful Kotlin/Java compiler/runtime probes and six expected-invalid cases (including the subsequently dropped proposal). The independent reviewer separately tested platform-return inference with Kotlin/JVM 2.4.10. These are bounded semantic probes, not Gradle or UI tests.

Learning-stage and environment evidence:

- Actual production Kotlin models, codecs and validators compiled in a scratch JVM runner using repository-matched Kotlin 2.4.10 and serialization 1.11.0: zero assessment/learning errors, both codec round trips passed (36 Units, 147 Lessons; baseline 480 questions). This is not a Gradle or UI test.
- All 36 instructional code blocks were checked with documented surrounding declarations/wrappers where needed; nine expected-invalid compiler cases produced the intended failures. Kotlin/Java boundary examples also checked generated signatures and runtime assertions. No Android execution or benchmark is implied.
- `python3 -m unittest discover -s tools -p 'test_*.py'`: 89 tests passed after regenerating the learning snapshot.
- Targeted `./gradlew :shared:jvmTest --tests '*InitialCurriculum*' --tests '*CurriculumValidator*' --tests '*BundledLearningCurriculum*' --tests '*LearningCurriculumValidator*'`: blocked before tests by wrapper download (`java.net.SocketException: Network is unreachable`). Gradle 9.1.0 and required Azul Java 21 are unavailable locally; only Java 17 is present. Broader Gradle checks were not attempted after this setup failure.
- Baseline mechanical source sweep: all 341 URLs returned HTTP 200 and all 50 fragments passed. Two unrelated Google Cloud pages returned short retrieved bodies; this does not establish that their canonical documentation is empty.
- Additional learning URLs: all 18 returned HTTP 200, 16 passed the rendered-body threshold. Two Oracle pages returned access stubs to the checker, while independent official retrieval exposed the full documentation. The Files.readString raw anchor remains mechanically unverified; its exact method documentation was available through independent retrieval. Lesson authors and independent reviewers separately opened the authoritative claim sources.

The initial Python tooling run detected the expected stale learning coverage snapshot; regeneration resolved it. No validation failure was treated as a successful run.

## CI follow-up

The first published CI run ([37441792665](https://github.com/ArtemiyTkachenko/KMP-Learning-App/actions/runs/37441792665)) compiled the JVM tests and executed 1,896 tests, with five failures in existing journey and Topic tests. Coverage, identity and backlog checks passed. The failures exposed assumptions about Kotlin being unauthored, DI continuing directly to KMP, plural progress text, and stripping literal stars from inline code.

Four test files were corrected to cover the new Kotlin journey and Topic availability, preserve inline-code syntax, use singular text for a one-Lesson Unit, and check absent adjacent navigation. Reader arrival now waits for its screen tag, since a Unit and Lesson may share a title. No production renderer or reviewed curriculum text was changed. The branch’s overlapping Quality improvements commit preserved these fixes and corrected the durable studied-Lesson count to 146. Its configured CI run [37449041785](https://github.com/ArtemiyTkachenko/KMP-Learning-App/actions/runs/37449041785) passed. The additional reader-arrival, absent-navigation and Kotlin Topic coverage assertions run the pipeline again; their result is recorded in the PR checks rather than presumed here. Local Gradle remains blocked as described above.

## Deliberately deferred scope

The blueprint retains `val`/`var` and scope functions as Reference material and does not add value classes, reflection internals, a bytecode tutorial, coroutine/Flow material or KMP runtime differences. Smart-cast/type-refinement reasoning remains mapped to `kotlin_nullability`, because the existing taxonomy has no dedicated smart-cast Subtopic. No new taxonomy was introduced. The blueprint’s interface-conflict behavioral assessment gap remains after rejecting the syntax-recall proposal; the Lesson still teaches the concept.
