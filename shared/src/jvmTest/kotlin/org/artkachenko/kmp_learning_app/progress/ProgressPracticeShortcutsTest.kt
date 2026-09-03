package org.artkachenko.kmp_learning_app.progress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.AppRoute
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.toPracticeBuilderRoute

/**
 * The whole of the weak-area shortcut's decision-making, which is why it can be tested without a
 * screen: a row becomes a scope and the one source it already stands for, and the existing preset
 * mapping turns that into the existing builder route.
 */
internal class ProgressPracticeShortcutsTest {
    @Test
    fun aWeakTopicRowBecomesWeakAreaPracticeForThatExactTopic() {
        val preset = weakArea(WeakAreaType.TOPIC, "topic_a").toPracticePreset()

        assertEquals(
            PracticePreset(
                scope = AssessmentScope.Topic("topic_a"),
                source = PracticeQuestionSource.WEAK_AREAS,
            ),
            preset,
        )
        assertEquals(
            AppRoute.PracticeBuilderTopic("topic_a", PracticeQuestionSource.WEAK_AREAS),
            preset.toPracticeBuilderRoute(),
        )
    }

    @Test
    fun aWeakSubtopicRowBecomesWeakAreaPracticeForThatExactSubtopic() {
        val preset = weakArea(WeakAreaType.SUBTOPIC, "subtopic_b").toPracticePreset()

        assertEquals(
            PracticePreset(
                scope = AssessmentScope.Subtopic("subtopic_b"),
                source = PracticeQuestionSource.WEAK_AREAS,
            ),
            preset,
        )
        assertEquals(
            AppRoute.PracticeBuilderSubtopic("subtopic_b", PracticeQuestionSource.WEAK_AREAS),
            preset.toPracticeBuilderRoute(),
        )
    }

    /**
     * The row's displayed accuracy plays no part: the row is in the weak-area list, which is the
     * domain's verdict, so a percentage that happens to look healthy changes nothing.
     */
    @Test
    fun theRowsOwnPercentageNeverChangesTheSource() {
        val presets = listOf(0.0, 41.0, 99.9).map { percentage ->
            weakArea(WeakAreaType.TOPIC, "topic_a", percentage = percentage).toPracticePreset()
        }

        assertEquals(1, presets.distinct().size)
        assertEquals(PracticeQuestionSource.WEAK_AREAS, presets.first().source)
    }

    /** A shortcut opens a setup screen. Anything that runs, scores, or reviews is not reachable. */
    @Test
    fun theShortcutLandsInTheBuilderAndCarriesNoQuestions() {
        val route = weakArea(WeakAreaType.SUBTOPIC, "subtopic_b").toPracticePreset()
            .toPracticeBuilderRoute()

        assertTrue(
            route is AppRoute.PracticeBuilderSubtopic,
            "A contextual shortcut must open the Practice Builder, but was $route.",
        )
        // The route's whole payload is the scope and the source. Nothing about which Questions the
        // run will draw travels with it, so no candidate list can go stale in navigation.
        assertEquals("subtopic_b", route.subtopicId)
        assertEquals(PracticeQuestionSource.WEAK_AREAS, route.source)
    }

    private fun weakArea(
        type: WeakAreaType,
        stableId: String,
        percentage: Double = 41.0,
    ): WeakAreaUiModel =
        WeakAreaUiModel(
            type = type,
            stableId = stableId,
            title = "Displayed name",
            subtitle = null,
            answeredCount = 5,
            correctCount = 2,
            percentage = percentage,
        )
}
