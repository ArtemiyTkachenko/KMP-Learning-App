package org.artkachenko.kmp_learning_app.topic_study.practice_builder

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.artkachenko.kmp_learning_app.AppRoute
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset

internal class PracticeBuilderRouteMappingTest {
    @Test
    fun topicScopeOpensTheBuilderWithOnlyItsStableId() {
        assertEquals(
            AppRoute.PracticeBuilderTopic("topic_a"),
            AssessmentScope.Topic("topic_a").toPracticeBuilderRoute(),
        )
        assertEquals(
            PracticeBuilderTarget.Topic("topic_a"),
            AppRoute.PracticeBuilderTopic("topic_a").toPracticeBuilderTarget(),
        )
    }

    @Test
    fun subtopicScopeOpensTheBuilderWithOnlyItsStableId() {
        assertEquals(
            AppRoute.PracticeBuilderSubtopic("subtopic_a"),
            AssessmentScope.Subtopic("subtopic_a").toPracticeBuilderRoute(),
        )
        assertEquals(
            PracticeBuilderTarget.Subtopic("subtopic_a"),
            AppRoute.PracticeBuilderSubtopic("subtopic_a").toPracticeBuilderTarget(),
        )
    }

    /** Opening from content is unchanged: scope only, and the builder's own ALL default. */
    @Test
    fun openingTheBuilderFromContentStillDefaultsToTheAllSource() {
        assertEquals(
            PracticeQuestionSource.ALL,
            assertIsTopicBuilderRoute(AssessmentScope.Topic("topic_a").toPracticeBuilderRoute())
                .source,
        )
        assertEquals(
            PracticeQuestionSource.ALL,
            assertIsSubtopicBuilderRoute(
                AssessmentScope.Subtopic("subtopic_a").toPracticeBuilderRoute(),
            ).source,
        )
    }

    @Test
    fun aTopicPresetOpensTheBuilderOnItsRememberedSource() {
        val route = assertIsTopicBuilderRoute(
            PracticePreset(
                scope = AssessmentScope.Topic("topic_a"),
                source = PracticeQuestionSource.UNSEEN,
            ).toPracticeBuilderRoute(),
        )

        assertEquals(AppRoute.PracticeBuilderTopic("topic_a", PracticeQuestionSource.UNSEEN), route)
        // Still the builder, never an assessment: a preset is a setup to inspect, not a run.
        assertEquals(PracticeBuilderTarget.Topic("topic_a"), route.toPracticeBuilderTarget())
    }

    @Test
    fun aSubtopicPresetOpensTheBuilderOnItsRememberedSource() {
        val route = assertIsSubtopicBuilderRoute(
            PracticePreset(
                scope = AssessmentScope.Subtopic("subtopic_a"),
                source = PracticeQuestionSource.WEAK_AREAS,
            ).toPracticeBuilderRoute(),
        )

        assertEquals(
            AppRoute.PracticeBuilderSubtopic("subtopic_a", PracticeQuestionSource.WEAK_AREAS),
            route,
        )
        assertEquals(PracticeBuilderTarget.Subtopic("subtopic_a"), route.toPracticeBuilderTarget())
    }

    /**
     * The Unit entry carries identity and nothing derived from it. A route holding the Unit's
     * concepts or its title would be a second copy of authored content living in the back stack,
     * which is exactly what resolving on arrival exists to avoid.
     */
    @Test
    fun aLearningUnitOpensTheBuilderWithOnlyItsStableId() {
        val route = AppRoute.PracticeBuilderLearningUnit("unit_a")

        assertEquals(PracticeBuilderTarget.LearningUnit("unit_a"), route.toPracticeBuilderTarget())
        assertEquals("unit_a", route.unitId)
    }

    /**
     * A derived multi-Subtopic scope has no way back to the Unit it came from, so it deliberately
     * has no scope-addressed builder entry. Failing loudly beats practising the wrong thing.
     */
    @Test
    fun aMultiSubtopicScopeHasNoScopeAddressedBuilderEntry() {
        assertFailsWith<IllegalStateException> {
            AssessmentScope.Subtopics(setOf("subtopic_a", "subtopic_b")).toPracticeBuilderRoute()
        }
        assertFailsWith<IllegalStateException> {
            PracticePreset(
                scope = AssessmentScope.Subtopics(setOf("subtopic_a")),
                source = PracticeQuestionSource.ALL,
            ).toPracticeBuilderRoute()
        }
    }

    private fun assertIsTopicBuilderRoute(route: AppRoute): AppRoute.PracticeBuilderTopic =
        route as? AppRoute.PracticeBuilderTopic
            ?: error("Expected a topic practice builder route but was $route.")

    private fun assertIsSubtopicBuilderRoute(route: AppRoute): AppRoute.PracticeBuilderSubtopic =
        route as? AppRoute.PracticeBuilderSubtopic
            ?: error("Expected a subtopic practice builder route but was $route.")

}
