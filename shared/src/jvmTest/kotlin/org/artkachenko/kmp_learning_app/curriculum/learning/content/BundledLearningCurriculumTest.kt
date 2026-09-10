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
