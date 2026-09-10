package org.artkachenko.kmp_learning_app.curriculum.learning.content

import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.content.BundledCurriculumSource
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit
import org.artkachenko.kmp_learning_app.curriculum.learning.validation.LearningCurriculumValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Exercises the shipped resource rather than a fixture, so the packaged bundle itself is
 * proven to decode and to be coherent against the taxonomy it was authored against.
 *
 * These tests assert structure and stable identity — Unit and Lesson ids, home Topic,
 * authored order, and the primary/supporting mappings later work will consume. Editorial
 * wording is reviewed by a person and deliberately not pinned here, because a test that
 * asserts prose only makes rewriting the prose expensive.
 *
 * The subject here is the shipped document itself. What the loader and the repository make
 * of it is `LearningContentEndToEndTest`'s subject.
 */
internal class BundledLearningCurriculumTest {
    private suspend fun units(): List<LearningUnit> = BundledLearningCurriculumSource.load().units

    private suspend fun unit(id: String): LearningUnit = units().single { it.id == id }

    @Test
    fun bundledLearningCurriculumShipsTheActiveUnitsInBlueprintOrder() = runTest {
        // List position is the ordering contract for a publisher-owned document, so this is
        // asserted unsorted: the state model is taught before the recomposition it drives,
        // and the coroutines path follows the Compose path it was authored after.
        assertEquals(
            listOf(
                "unit_thinking_in_compose",
                "unit_state_and_state_ownership",
                "unit_recomposition",
                "unit_identity_keys_and_stability",
                "unit_derived_state_and_expensive_work",
                "unit_snapshot_fundamentals",
                "unit_coroutines_and_structured_concurrency",
                "unit_context_dispatchers_and_concurrency",
                "unit_cancellation_failure_and_coordination",
                "unit_flow_fundamentals",
                "unit_flow_composition_timing_and_failure",
            ),
            units().map { it.id },
        )

        assertEquals(
            listOf(
                "Thinking in Compose",
                "State and State Ownership",
                "Recomposition",
                "Identity, Keys, Stability and Immutability",
                "Derived State and Expensive Work",
                "Snapshot Fundamentals",
                "Coroutine Fundamentals and Structured Concurrency",
                "Coroutine Context, Dispatchers and Concurrent Work",
                "Cancellation, Failure and Coordination",
                "Flow Fundamentals",
                "Flow Composition, Timing and Failure",
            ),
            units().map { it.title },
        )

        // A Unit's home Topic decides where it is browsed, so it is asserted per Unit
        // rather than as one value: the document now spans two home Topics.
        assertEquals(
            listOf(
                "android_ui",
                "android_ui",
                "android_ui",
                "android_ui",
                "android_ui",
                "android_ui",
                "async_reactive",
                "async_reactive",
                "async_reactive",
                "async_reactive",
                "async_reactive",
            ),
            units().map { it.topicId },
        )

        units().forEach { unit ->
            assertEquals(ContentStatus.ACTIVE, unit.status, unit.id)
        }
    }

    @Test
    fun eachUnitCarriesItsLessonsInBlueprintOrder() = runTest {
        assertEquals(
            listOf(
                "lesson_declarative_ui",
                "lesson_composable_execution",
                "lesson_state_down_events_up",
            ),
            unit("unit_thinking_in_compose").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "lesson_observable_state",
                "lesson_remember_composition_memory",
                "lesson_remember_saveable",
                "lesson_state_hoisting",
                "lesson_observable_collections",
            ),
            unit("unit_state_and_state_ownership").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "lesson_composition_and_recomposition",
                "lesson_recomposition_scopes",
                "lesson_recomposition_cost",
            ),
            unit("unit_recomposition").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "lesson_composable_identity",
                "lesson_keys_and_identity_in_lists",
                "lesson_immutability_vs_stability",
                "lesson_stability_and_skipping",
                "lesson_stability_annotations",
            ),
            unit("unit_identity_keys_and_stability").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "lesson_remember_key_memoization",
                "lesson_derived_state",
                "lesson_work_outside_composition",
            ),
            unit("unit_derived_state_and_expensive_work").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "lesson_snapshot_observation",
                "lesson_snapshot_flow",
            ),
            unit("unit_snapshot_fundamentals").lessons.map { it.id },
        )

        // The coroutines Unit reads as one argument, so its order is the argument:
        // suspension, then how work starts, then its lifetime, then its owner, then the
        // guarantees ownership and hierarchy make together.
        assertEquals(
            listOf(
                "lesson_suspension_and_blocking",
                "lesson_coroutine_builders",
                "lesson_job_and_parent_child",
                "lesson_coroutine_scope_ownership",
                "lesson_structured_concurrency",
            ),
            unit("unit_coroutines_and_structured_concurrency").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "lesson_coroutine_context",
                "lesson_dispatchers",
                "lesson_with_context_and_main_safety",
                "lesson_sequential_and_concurrent_work",
            ),
            unit("unit_context_dispatchers_and_concurrency").lessons.map { it.id },
        )

        // Unit 3 escalates one argument: whether the work stops, what happens while it
        // stops, what happens if it fails instead, how supervision changes that, and what
        // concurrent work does to state it shares.
        assertEquals(
            listOf(
                "lesson_cooperative_cancellation",
                "lesson_cancellation_cleanup_and_timeouts",
                "lesson_exception_propagation",
                "lesson_supervision_and_failure_isolation",
                "lesson_shared_state_and_coordination",
            ),
            unit("unit_cancellation_failure_and_coordination").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "Cancellation Is Cooperative",
                "Cleanup, `NonCancellable` and Timeouts",
                "How a Coroutine Failure Travels",
                "`SupervisorJob`, `supervisorScope` and the Limits of Isolation",
                "Shared Mutable State and Choosing a Coordination Mechanism",
            ),
            unit("unit_cancellation_failure_and_coordination").lessons.map { it.title },
        )

        assertEquals(
            listOf(
                "`CoroutineContext` and What Children Inherit",
                "Dispatchers and Where Code Actually Runs",
                "`withContext` and Main-Safety",
                "Sequential by Default, Concurrent on Purpose",
            ),
            unit("unit_context_dispatchers_and_concurrency").lessons.map { it.title },
        )

        // Unit 4 is a single progression rather than five Flow topics: why the shape
        // exists, what cold means, who owns the collection, where it executes, and how an
        // existing producer becomes a flow.
        assertEquals(
            listOf(
                "lesson_why_flow",
                "lesson_cold_flows",
                "lesson_flow_collection_lifetime",
                "lesson_flow_context_and_flow_on",
                "lesson_flow_builders_and_callback_adapters",
            ),
            unit("unit_flow_fundamentals").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "One Value or Many: Why `Flow` Exists",
                "Cold Flows: Producer, Collector and Operators",
                "Collection Lifetime and Flow Cancellation",
                "Context Preservation and `flowOn`",
                "Flow Builders and Adapting Callback APIs",
            ),
            unit("unit_flow_fundamentals").lessons.map { it.title },
        )

        // Unit 5 is a sequence of decisions taken on the pipeline Unit 4 established:
        // transform one stream, join streams, map values into streams of their own,
        // handle a producer faster than its collector, then handle how collection ends.
        assertEquals(
            listOf(
                "lesson_transforming_and_filtering_flows",
                "lesson_combining_flows",
                "lesson_flattening_flows",
                "lesson_flow_buffering_and_conflation",
                "lesson_flow_failure_and_completion",
            ),
            unit("unit_flow_composition_timing_and_failure").lessons.map { it.id },
        )

        assertEquals(
            listOf(
                "Transforming and Filtering: by Value and by Time",
                "`combine` and `zip`: Current Values or Paired Emissions",
                "Flattening: Should New Input Cancel Old Work?",
                "When the Collector Cannot Keep Up",
                "`catch`, `retry` and `onCompletion`",
            ),
            unit("unit_flow_composition_timing_and_failure").lessons.map { it.title },
        )
    }

    @Test
    fun everyLessonDeclaresThePrimaryConceptItTeaches() = runTest {
        // The primary mapping is the stable bridge into assessment coverage, so it is a
        // contract rather than editorial detail.
        assertEquals(
            listOf(
                listOf("compose_fundamentals"),
                listOf("compose_fundamentals"),
                listOf("compose_udf"),
            ),
            unit("unit_thinking_in_compose").lessons.map { it.primarySubtopicIds },
        )

        assertEquals(
            listOf(
                listOf("compose_state"),
                listOf("compose_state"),
                listOf("compose_state"),
                listOf("compose_state_hoisting"),
                listOf("compose_state"),
            ),
            unit("unit_state_and_state_ownership").lessons.map { it.primarySubtopicIds },
        )

        assertEquals(
            listOf(
                listOf("compose_recomposition"),
                listOf("compose_recomposition"),
                listOf("compose_recomposition"),
            ),
            unit("unit_recomposition").lessons.map { it.primarySubtopicIds },
        )

        // Three Lessons share `compose_stability` at different depths, which the authoring
        // contract allows: a Subtopic is touched by more than one Lesson, and the identity
        // pair is a separate concept from the stability pair.
        assertEquals(
            listOf(
                listOf("compose_identity_keys"),
                listOf("compose_identity_keys"),
                listOf("compose_stability"),
                listOf("compose_stability"),
                listOf("compose_stability"),
            ),
            unit("unit_identity_keys_and_stability").lessons.map { it.primarySubtopicIds },
        )

        // All three Unit 5 Lessons own the same concept, so the Unit practises exactly
        // `compose_derived_state`. The Unit is a decision model rather than three separate
        // subjects, and splitting the mapping would claim coverage the taxonomy does not have.
        assertEquals(
            listOf(
                listOf("compose_derived_state"),
                listOf("compose_derived_state"),
                listOf("compose_derived_state"),
            ),
            unit("unit_derived_state_and_expensive_work").lessons.map { it.primarySubtopicIds },
        )

        // Both Unit 6 Lessons own `compose_snapshot_system`: the Unit is one mental model
        // and its second Lesson applies that model to a non-UI consumer rather than
        // teaching a separate concept.
        assertEquals(
            listOf(
                listOf("compose_snapshot_system"),
                listOf("compose_snapshot_system"),
            ),
            unit("unit_snapshot_fundamentals").lessons.map { it.primarySubtopicIds },
        )

        // Unlike the Compose Units, every Lesson here owns a different concept, and each of
        // the five is a distinct `async_reactive` Subtopic. Unit practice is exactly these
        // five: the cross-Topic bridges the Lessons lean on stay supporting.
        assertEquals(
            listOf(
                listOf("coroutine_fundamentals"),
                listOf("coroutine_builders"),
                listOf("coroutine_jobs"),
                listOf("coroutine_scope"),
                listOf("structured_concurrency"),
            ),
            unit("unit_coroutines_and_structured_concurrency").lessons.map { it.primarySubtopicIds },
        )

        assertEquals(
            listOf(
                listOf("coroutine_context"),
                listOf("coroutine_dispatchers"),
                listOf("coroutine_context_switching"),
                listOf("coroutine_parallelism"),
            ),
            unit("unit_context_dispatchers_and_concurrency").lessons.map { it.primarySubtopicIds },
        )

        // `coroutine_cancellation` is primary in two Lessons at different depths, which the
        // authoring contract allows: the request and the unwind are separate mental models.
        assertEquals(
            listOf(
                listOf("coroutine_cancellation"),
                listOf("coroutine_cancellation"),
                listOf("coroutine_exceptions"),
                listOf("coroutine_supervision"),
                listOf("coroutine_parallelism"),
            ),
            unit("unit_cancellation_failure_and_coordination").lessons.map { it.primarySubtopicIds },
        )

        // `flow_fundamentals` is primary in three of the five Lessons — why the shape
        // exists, what cold means, and how a producer becomes one — so Unit practice here
        // is exactly three concepts rather than five.
        assertEquals(
            listOf(
                listOf("flow_fundamentals"),
                listOf("flow_fundamentals"),
                listOf("flow_collection"),
                listOf("flow_context"),
                listOf("flow_fundamentals"),
            ),
            unit("unit_flow_fundamentals").lessons.map { it.primarySubtopicIds },
        )

        // `flow_operators` is primary in the first three Lessons — filtering, combining
        // and flattening are three depths of one concept — so Unit practice here is three
        // concepts rather than five.
        assertEquals(
            listOf(
                listOf("flow_operators"),
                listOf("flow_operators"),
                listOf("flow_operators"),
                listOf("flow_buffering"),
                listOf("flow_errors"),
            ),
            unit("unit_flow_composition_timing_and_failure").lessons.map { it.primarySubtopicIds },
        )
    }

    @Test
    fun contextUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        assertEquals(
            listOf(
                listOf("coroutine_jobs", "coroutine_dispatchers", "structured_concurrency"),
                listOf("coroutine_context", "android_main_thread", "main_thread_performance", "anr"),
                listOf(
                    "coroutine_dispatchers",
                    "coroutine_fundamentals",
                    "structured_concurrency",
                    "repository_pattern",
                ),
                listOf("coroutine_builders", "structured_concurrency", "coroutine_dispatchers"),
            ),
            unit("unit_context_dispatchers_and_concurrency").lessons.map { it.supportingSubtopicIds },
        )
    }

    @Test
    fun cancellationUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        // The Kotlin, JVM, performance and hot-stream concepts this Unit leans on are
        // assessed in their own Topics. Promoting any of them would hand Unit practice
        // questions the Unit does not teach — GAP-U3-B in
        // `docs/content/coroutines-flow-units-1-6-plan.md` depends on that staying true,
        // because nothing in any Topic yet assesses coroutine races over shared state.
        assertEquals(
            listOf(
                listOf("coroutine_jobs", "structured_concurrency", "kotlin_exceptions"),
                listOf("kotlin_exceptions", "coroutine_context_switching", "coroutine_jobs"),
                listOf(
                    "coroutine_builders",
                    "coroutine_jobs",
                    "coroutine_cancellation",
                    "error_modeling",
                ),
                listOf(
                    "coroutine_exceptions",
                    "coroutine_context",
                    "structured_concurrency",
                    "coroutine_jobs",
                ),
                listOf(
                    "hot_vs_cold_streams",
                    "jvm_fundamentals",
                    "android_memory_model",
                    "coroutine_dispatchers",
                ),
            ),
            unit("unit_cancellation_failure_and_coordination").lessons.map { it.supportingSubtopicIds },
        )
    }

    @Test
    fun flowUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        // The architecture, Kotlin, lifecycle and performance concepts Unit 4 bridges are
        // assessed in their own Topics, and none of them is reachable through an E24
        // primary mapping. GAP-U4-A depends on that staying true: the closest existing
        // assessment of the one-shot-versus-stream decision is
        // `repository_observable_api_shape`, which sits in `architecture`, so promoting
        // `repository_pattern` here would claim Unit practice no `async_reactive`
        // Question provides and would hide the gap E24-08 still has to close.
        assertEquals(
            listOf(
                listOf(
                    "coroutine_fundamentals",
                    "hot_vs_cold_streams",
                    "repository_pattern",
                    "single_source_of_truth",
                ),
                listOf("flow_collection", "flow_operators", "kotlin_sequences"),
                listOf(
                    "coroutine_cancellation",
                    "coroutine_scope",
                    "lifecycle_coroutines",
                    "coroutine_jobs",
                ),
                listOf(
                    "coroutine_context",
                    "coroutine_dispatchers",
                    "coroutine_context_switching",
                    "flow_errors",
                ),
                listOf(
                    "flow_context",
                    "coroutine_cancellation",
                    "flow_buffering",
                    "memory_leaks",
                ),
            ),
            unit("unit_flow_fundamentals").lessons.map { it.supportingSubtopicIds },
        )
    }

    @Test
    fun compositionUnitKeepsItsPlannedBridgesOutOfPrimaryPractice() = runTest {
        // `stateflow` is supporting in the combining Lesson because current-value
        // reasoning is relevant there, and `kotlin_equality`, `kotlin_lambdas`,
        // `kotlin_exceptions`, `error_modeling`, `coroutine_cancellation` and
        // `coroutine_parallelism` are all bridges into Topics that assess them
        // themselves. Promoting any of them would hand Unit 5 practice Questions this
        // Unit does not teach — and promoting `stateflow` in particular would claim
        // coverage of material Unit 6 has not shipped yet.
        assertEquals(
            listOf(
                listOf("flow_fundamentals", "kotlin_lambdas", "kotlin_equality"),
                listOf("flow_fundamentals", "stateflow", "flow_collection"),
                listOf("coroutine_cancellation", "coroutine_parallelism", "flow_collection"),
                listOf(
                    "flow_collection",
                    "flow_context",
                    "coroutine_cancellation",
                    "flow_operators",
                ),
                listOf(
                    "kotlin_exceptions",
                    "coroutine_cancellation",
                    "flow_operators",
                    "error_modeling",
                ),
            ),
            unit("unit_flow_composition_timing_and_failure").lessons.map { it.supportingSubtopicIds },
        )
    }

    @Test
    fun aPerformanceConceptStaysSupportingRatherThanBecomingUnitPractice() = runTest {
        // GAP-U3-A in `docs/content/compose-units-2-6-plan.md`: the one active Question on
        // `compose_recomposition_performance` lives in the `performance` Topic and is reached
        // only as supporting context. Promoting it to primary would silently claim Unit 3
        // practice coverage that no Question actually provides, which E23-07 must still see.
        val lesson = unit("unit_recomposition").lessons.single { it.id == "lesson_recomposition_cost" }

        assertTrue(lesson.supportingSubtopicIds.contains("compose_recomposition_performance"))
        assertEquals(listOf("compose_recomposition"), lesson.primarySubtopicIds)
    }

    @Test
    fun kotlinLanguageConceptsStaySupportingRatherThanBecomingUnitPractice() = runTest {
        // GAP-U4-C in `docs/content/compose-units-2-6-plan.md`: the Kotlin facts this Lesson
        // bridges are assessed in the `kotlin_language` Topic, and the Compose consequence of
        // read-only-but-mutable data is not assessed anywhere. Promoting any of these to
        // primary would claim Unit 4 practice coverage that no Question provides.
        val lesson = unit("unit_identity_keys_and_stability").lessons
            .single { it.id == "lesson_immutability_vs_stability" }

        assertEquals(listOf("compose_stability"), lesson.primarySubtopicIds)
        assertTrue(
            lesson.supportingSubtopicIds.containsAll(
                listOf("kotlin_data_classes", "kotlin_equality", "kotlin_collections", "kotlin_variables"),
            ),
        )
    }

    @Test
    fun architectureAndPerformanceConceptsStaySupportingRatherThanBecomingUnitPractice() = runTest {
        // GAP-U5-B in `docs/content/compose-units-2-6-plan.md`: this Lesson argues where work
        // belongs, so it leans on `main_thread_performance`, `layered_architecture` and
        // `use_cases` — all owned by other Topics and all assessed there, if at all, in terms
        // that have nothing to do with composition. Promoting any of them would hand Unit 5
        // practice questions it does not teach and would hide the gap E23-07 has to see.
        val lesson = unit("unit_derived_state_and_expensive_work").lessons
            .single { it.id == "lesson_work_outside_composition" }

        assertEquals(listOf("compose_derived_state"), lesson.primarySubtopicIds)
        assertTrue(
            lesson.supportingSubtopicIds.containsAll(
                listOf("main_thread_performance", "layered_architecture", "use_cases"),
            ),
        )
    }

    @Test
    fun flowConceptsStaySupportingRatherThanBecomingUnitPractice() = runTest {
        // GAP-U6-A in `docs/content/compose-units-2-6-plan.md`: the single active Question on
        // `compose_snapshot_system` identifies an API name, and nothing assesses the observation
        // model this Unit is built on. The Flow concepts the second Lesson bridges are assessed
        // in the `async_reactive` Topic in terms that have nothing to do with Compose, and
        // `compose_side_effects` belongs to the Effects Unit that E23 does not author. Promoting
        // any of them would hand Unit 6 practice it does not teach and would hide the gap that
        // E23-07 still has to close.
        val lesson = unit("unit_snapshot_fundamentals").lessons
            .single { it.id == "lesson_snapshot_flow" }

        assertEquals(listOf("compose_snapshot_system"), lesson.primarySubtopicIds)
        assertTrue(
            lesson.supportingSubtopicIds.containsAll(
                listOf("flow_fundamentals", "hot_vs_cold_streams", "compose_side_effects"),
            ),
        )
    }

    @Test
    fun lifecycleAndPerformanceConceptsStaySupportingInTheCoroutinesUnit() = runTest {
        // `docs/content/coroutines-flow-units-1-6-plan.md`: no E24 Lesson takes a
        // non-`async_reactive` Subtopic as primary, which is what keeps the lifecycle,
        // performance and Android-platform bridges out of this Unit's practice. GAP-U1-B
        // in particular depends on it — `performance_coroutine_scope_leak` assesses the
        // ownership failure from the leak side, and promoting `coroutine_leaks` would
        // claim Unit practice that no `async_reactive` Question provides and would hide
        // the gap E24-08 still has to close.
        val lesson = unit("unit_coroutines_and_structured_concurrency").lessons
            .single { it.id == "lesson_coroutine_scope_ownership" }

        assertEquals(listOf("coroutine_scope"), lesson.primarySubtopicIds)
        assertTrue(
            lesson.supportingSubtopicIds.containsAll(
                listOf("coroutine_jobs", "lifecycle_coroutines", "coroutine_leaks", "viewmodel_lifecycle"),
            ),
        )
    }

    @Test
    fun kotlinAndPlatformConceptsStaySupportingInTheSuspensionLesson() = runTest {
        // The same rule at the entry point of the coroutines path: the thread, JVM and
        // main-thread facts this Lesson bridges are assessed in their own Topics, and
        // dispatchers are named here only to be deferred to the Unit that owns them.
        val lesson = unit("unit_coroutines_and_structured_concurrency").lessons
            .single { it.id == "lesson_suspension_and_blocking" }

        assertEquals(listOf("coroutine_fundamentals"), lesson.primarySubtopicIds)
        assertTrue(
            lesson.supportingSubtopicIds.containsAll(
                listOf("coroutine_dispatchers", "jvm_fundamentals", "android_main_thread"),
            ),
        )
    }

    @Test
    fun aLessonBridgesToAnotherTopicsSupportingConcept() = runTest {
        // `unidirectional_data_flow` is owned by the architecture Topic while the Unit is
        // browsed under `android_ui`; cross-Topic support is a product rule, not a defect.
        assertTrue(
            unit("unit_thinking_in_compose").lessons
                .single { it.id == "lesson_state_down_events_up" }
                .supportingSubtopicIds
                .contains("unidirectional_data_flow"),
        )

        // The same rule, exercised where the bridge spans three Topics at once: saved state
        // and process death are `lifecycle_navigation` concepts a Compose Lesson has to lean
        // on without reproducing that curriculum.
        assertTrue(
            unit("unit_state_and_state_ownership").lessons
                .single { it.id == "lesson_remember_saveable" }
                .supportingSubtopicIds
                .containsAll(listOf("saved_state", "configuration_changes", "process_death")),
        )
    }

    @Test
    fun everyActiveLessonIsStudyableAndSourced() = runTest {
        units().flatMap { it.lessons }.forEach { lesson ->
            assertEquals(ContentStatus.ACTIVE, lesson.status, lesson.id)
            assertTrue(lesson.sections.isNotEmpty(), lesson.id)
            assertTrue(lesson.sources.isNotEmpty(), lesson.id)
        }
    }

    @Test
    fun relatedLessonReferencesResolveWithinTheShippedDocument() = runTest {
        // Forward links are invalid until their target ships, so this is what stops a Lesson
        // pointing at a Unit that is still only planned.
        val lessons = units().flatMap { it.lessons }
        val lessonIds = lessons.map { it.id }.toSet()

        lessons.forEach { lesson ->
            lesson.relatedLessonIds.forEach { relatedId ->
                assertTrue(relatedId in lessonIds, "${lesson.id} -> $relatedId")
            }
        }
    }

    @Test
    fun bundledLearningCurriculumValidatesAgainstTheBundledBaseCurriculum() = runTest {
        val errors = LearningCurriculumValidator().validate(
            learningCurriculum = BundledLearningCurriculumSource.load(),
            curriculum = BundledCurriculumSource.load(),
        )

        assertEquals(emptyList(), errors)
    }
}
