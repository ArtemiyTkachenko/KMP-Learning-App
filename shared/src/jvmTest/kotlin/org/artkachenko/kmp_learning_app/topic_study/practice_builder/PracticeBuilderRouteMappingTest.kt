package org.artkachenko.kmp_learning_app.topic_study.practice_builder

import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.AppRoute
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset
import org.artkachenko.kmp_learning_app.topic_study.focused_practice.toAssessmentConfig

internal class PracticeBuilderRouteMappingTest {
    @Test
    fun topicScopeOpensTheBuilderWithOnlyItsStableId() {
        assertEquals(
            AppRoute.PracticeBuilderTopic("topic_a"),
            AssessmentScope.Topic("topic_a").toPracticeBuilderRoute(),
        )
        assertEquals(
            AssessmentScope.Topic("topic_a"),
            AppRoute.PracticeBuilderTopic("topic_a").toAssessmentScope(),
        )
    }

    @Test
    fun subtopicScopeOpensTheBuilderWithOnlyItsStableId() {
        assertEquals(
            AppRoute.PracticeBuilderSubtopic("subtopic_a"),
            AssessmentScope.Subtopic("subtopic_a").toPracticeBuilderRoute(),
        )
        assertEquals(
            AssessmentScope.Subtopic("subtopic_a"),
            AppRoute.PracticeBuilderSubtopic("subtopic_a").toAssessmentScope(),
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
        assertEquals(AssessmentScope.Topic("topic_a"), route.toAssessmentScope())
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
        assertEquals(AssessmentScope.Subtopic("subtopic_a"), route.toAssessmentScope())
    }

    /**
     * The whole point of the wider route: a configured run has to survive navigation intact, or
     * assessment taking would silently start something the learner did not configure.
     */
    @Test
    fun aTopicConfigurationSurvivesTheRoundTrip() {
        val config = AssessmentConfig.Focused(
            scope = AssessmentScope.Topic("topic_a"),
            questionCount = 15,
            levels = setOf(QuestionLevel.ADVANCED, QuestionLevel.FOUNDATION),
            source = PracticeQuestionSource.ALL,
        )

        val route = assertIsTopicRoute(config.toPracticeRoute())

        assertEquals("topic_a", route.topicId)
        assertEquals(15, route.questionCount)
        assertEquals(
            listOf(QuestionLevel.FOUNDATION, QuestionLevel.ADVANCED),
            route.levels,
        )
        assertEquals(PracticeQuestionSource.ALL, route.source)
        assertEquals(config, route.toAssessmentConfig())
    }

    @Test
    fun aSubtopicConfigurationSurvivesTheRoundTrip() {
        val config = AssessmentConfig.Focused(
            scope = AssessmentScope.Subtopic("subtopic_a"),
            questionCount = 5,
            levels = setOf(QuestionLevel.APPLIED),
            source = PracticeQuestionSource.ALL,
        )

        val route = assertIsSubtopicRoute(config.toPracticeRoute())

        assertEquals("subtopic_a", route.subtopicId)
        assertEquals(5, route.questionCount)
        assertEquals(listOf(QuestionLevel.APPLIED), route.levels)
        assertEquals(PracticeQuestionSource.ALL, route.source)
        assertEquals(config, route.toAssessmentConfig())
    }

    /** Selection order must not change the route, so an equal setup produces an equal entry. */
    @Test
    fun levelOrderIsNormalisedSoAnEqualSetupIsAnEqualRoute() {
        fun route(levels: Set<QuestionLevel>) =
            AssessmentConfig.Focused(
                scope = AssessmentScope.Topic("topic_a"),
                questionCount = 10,
                levels = levels,
                source = PracticeQuestionSource.ALL,
            ).toPracticeRoute()

        assertEquals(
            route(setOf(QuestionLevel.FOUNDATION, QuestionLevel.ADVANCED)),
            route(setOf(QuestionLevel.ADVANCED, QuestionLevel.FOUNDATION)),
        )
    }

    private fun assertIsTopicBuilderRoute(route: AppRoute): AppRoute.PracticeBuilderTopic =
        route as? AppRoute.PracticeBuilderTopic
            ?: error("Expected a topic practice builder route but was $route.")

    private fun assertIsSubtopicBuilderRoute(route: AppRoute): AppRoute.PracticeBuilderSubtopic =
        route as? AppRoute.PracticeBuilderSubtopic
            ?: error("Expected a subtopic practice builder route but was $route.")

    private fun assertIsTopicRoute(route: AppRoute): AppRoute.FocusedTopicPractice =
        route as? AppRoute.FocusedTopicPractice
            ?: error("Expected a topic practice route but was $route.")

    private fun assertIsSubtopicRoute(route: AppRoute): AppRoute.FocusedSubtopicPractice =
        route as? AppRoute.FocusedSubtopicPractice
            ?: error("Expected a subtopic practice route but was $route.")
}
