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
 */
internal class BundledLearningCurriculumTest {
    @Test
    fun bundledLearningCurriculumShipsTheActiveThinkingInComposeUnit() = runTest {
        val unit = BundledLearningCurriculumSource.load().units.single()

        assertEquals("unit_thinking_in_compose", unit.id)
        assertEquals("Thinking in Compose", unit.title)
        assertEquals("android_ui", unit.topicId)
        assertEquals(ContentStatus.ACTIVE, unit.status)
    }

    @Test
    fun theUnitCarriesItsThreeLessonsInBlueprintOrder() = runTest {
        val unit = BundledLearningCurriculumSource.load().units.single()

        assertEquals(
            listOf(
                "lesson_declarative_ui",
                "lesson_composable_execution",
                "lesson_state_down_events_up",
            ),
            unit.lessons.map { it.id },
        )
    }

    @Test
    fun everyLessonDeclaresThePrimaryConceptItTeaches() = runTest {
        // The primary mapping is the stable bridge into assessment coverage, so it is a
        // contract rather than editorial detail.
        val unit = BundledLearningCurriculumSource.load().units.single()

        assertEquals(
            listOf(
                listOf("compose_fundamentals"),
                listOf("compose_fundamentals"),
                listOf("compose_udf"),
            ),
            unit.lessons.map { it.primarySubtopicIds },
        )
    }

    @Test
    fun aLessonBridgesToAnotherTopicsSupportingConcept() = runTest {
        // `unidirectional_data_flow` is owned by the architecture Topic while the Unit is
        // browsed under `android_ui`; cross-Topic support is a product rule, not a defect.
        val unit = BundledLearningCurriculumSource.load().units.single()

        assertTrue(
            unit.lessons
                .single { it.id == "lesson_state_down_events_up" }
                .supportingSubtopicIds
                .contains("unidirectional_data_flow"),
        )
    }

    @Test
    fun everyActiveLessonIsStudyableAndSourced() = runTest {
        val unit = BundledLearningCurriculumSource.load().units.single()

        unit.lessons.forEach { lesson ->
            assertEquals(ContentStatus.ACTIVE, lesson.status, lesson.id)
            assertTrue(lesson.sections.isNotEmpty(), lesson.id)
            assertTrue(lesson.sources.isNotEmpty(), lesson.id)
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

    @Test
    fun theProductionLoadPathExposesTheBundledDocument() = runTest {
        // The default loader wires both shipped resources together; this is the path the
        // repository uses at runtime, so a resource-path or packaging mistake fails here.
        val learningCurriculum = LearningContentLoader().load()

        assertEquals(listOf("unit_thinking_in_compose"), learningCurriculum.units.map { it.id })
    }

    @Test
    fun theRepositoryServesTheBundledDocument() = runTest {
        val repository = BundledLearningContentRepository()

        val units: List<LearningUnit> = repository.getActiveUnitsByTopic("android_ui")

        assertEquals(listOf("unit_thinking_in_compose"), units.map { it.id })
    }
}
