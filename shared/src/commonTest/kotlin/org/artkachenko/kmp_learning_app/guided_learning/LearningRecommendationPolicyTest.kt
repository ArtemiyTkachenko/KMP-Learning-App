package org.artkachenko.kmp_learning_app.guided_learning

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.learning_progress.SubtopicCoverage
import org.artkachenko.kmp_learning_app.learning_progress.SubtopicPerformance
import org.artkachenko.kmp_learning_app.learning_progress.TopicCoverage
import org.artkachenko.kmp_learning_app.learning_progress.TopicPerformance
import org.artkachenko.kmp_learning_app.learning_progress.WeakArea

internal class LearningRecommendationPolicyTest {
    @Test
    fun newUserWithActiveCurriculumStartsByBrowsingTopics() {
        val recommendation = recommend(
            inputs(
                completedAttemptCount = 0,
                topicCoverage = listOf(coverage("topic_a", attempted = 0, total = 4)),
            ),
        )

        assertEquals(LearningRecommendationTarget.Topics, recommendation.target)
        assertEquals(LearningRecommendationRationale.NewUser, recommendation.rationale)
    }

    @Test
    fun noActiveCurriculumProducesNoRecommendation() {
        val recommendation = LearningRecommendationPolicy.recommend(
            inputs(
                unresolvedMistakeCount = 2,
                weakAreas = listOf(weakTopic("retired_topic")),
                topicCoverage = emptyList(),
            ),
        )

        assertNull(recommendation)
    }

    @Test
    fun unresolvedMistakesOutrankWeakAreasAndCoverage() {
        val recommendation = recommend(
            inputs(
                unresolvedMistakeCount = 3,
                weakAreas = listOf(weakTopic("topic_a")),
                topicCoverage = listOf(coverage("topic_a", attempted = 1, total = 5)),
            ),
        )

        assertEquals(LearningRecommendationTarget.MistakeReview, recommendation.target)
        assertEquals(
            LearningRecommendationRationale.UnresolvedMistakes(count = 3),
            recommendation.rationale,
        )
    }

    @Test
    fun usableWeakAreaOutranksIncompleteCoverage() {
        val recommendation = recommend(
            inputs(
                weakAreas = listOf(weakTopic("topic_a", name = "Kotlin")),
                topicCoverage = listOf(coverage("topic_a", attempted = 0, total = 5)),
            ),
        )

        val target = assertIs<LearningRecommendationTarget.Practice>(recommendation.target)
        assertEquals(
            PracticePreset(
                scope = AssessmentScope.Topic("topic_a"),
                source = PracticeQuestionSource.WEAK_AREAS,
            ),
            target.preset,
        )
        assertEquals(
            LearningRecommendationRationale.WeakArea(
                scope = AssessmentScope.Topic("topic_a"),
                areaName = "Kotlin",
            ),
            recommendation.rationale,
        )
    }

    @Test
    fun incompleteCoverageIsTheFallbackRecommendation() {
        val recommendation = recommend(
            inputs(
                topicCoverage = listOf(coverage("topic_a", attempted = 2, total = 5)),
            ),
        )

        assertUnseenRecommendation(recommendation, topicId = "topic_a", unseenCount = 3)
    }

    @Test
    fun returningLearnerWithFullCoverageAndNoOtherSignalHasNoRecommendation() {
        val recommendation = LearningRecommendationPolicy.recommend(
            inputs(
                topicCoverage = listOf(coverage("topic_a", attempted = 5, total = 5)),
            ),
        )

        assertNull(recommendation)
    }

    @Test
    fun weakTopicMapsToTopicScopedWeakAreaPreset() {
        val recommendation = recommend(
            inputs(
                weakAreas = listOf(weakTopic("topic_a")),
                topicCoverage = listOf(coverage("topic_a", attempted = 4, total = 4)),
            ),
        )

        val target = assertIs<LearningRecommendationTarget.Practice>(recommendation.target)
        assertEquals(AssessmentScope.Topic("topic_a"), target.preset.scope)
        assertEquals(PracticeQuestionSource.WEAK_AREAS, target.preset.source)
    }

    @Test
    fun weakSubtopicMapsToSubtopicScopedWeakAreaPreset() {
        val recommendation = recommend(
            inputs(
                weakAreas = listOf(weakSubtopic("topic_a", "subtopic_a")),
                topicCoverage = listOf(coverage("topic_a", attempted = 4, total = 4)),
                subtopicCoverage = listOf(
                    SubtopicCoverage("topic_a", "subtopic_a", 2, 2),
                ),
            ),
        )

        val target = assertIs<LearningRecommendationTarget.Practice>(recommendation.target)
        assertEquals(AssessmentScope.Subtopic("subtopic_a"), target.preset.scope)
        assertEquals(PracticeQuestionSource.WEAK_AREAS, target.preset.source)
    }

    @Test
    fun staleWeakAreaIsSkippedForTheNextUsableAreaInEstablishedOrder() {
        val recommendation = recommend(
            inputs(
                weakAreas = listOf(
                    weakTopic("retired_topic"),
                    weakSubtopic("topic_a", "subtopic_a"),
                ),
                topicCoverage = listOf(coverage("topic_a", attempted = 3, total = 3)),
                subtopicCoverage = listOf(
                    SubtopicCoverage("topic_a", "subtopic_a", 3, 3),
                ),
            ),
        )

        val target = assertIs<LearningRecommendationTarget.Practice>(recommendation.target)
        assertEquals(AssessmentScope.Subtopic("subtopic_a"), target.preset.scope)
        assertEquals(PracticeQuestionSource.WEAK_AREAS, target.preset.source)
    }

    @Test
    fun staleWeakAreaFallsThroughToCoverageWhenNoUsableWeakAreaExists() {
        val recommendation = recommend(
            inputs(
                weakAreas = listOf(weakTopic("retired_topic")),
                topicCoverage = listOf(coverage("topic_a", attempted = 2, total = 5)),
            ),
        )

        assertUnseenRecommendation(recommendation, topicId = "topic_a", unseenCount = 3)
    }

    @Test
    fun lowestCoverageRatioSelectsTheTopic() {
        val recommendation = recommend(
            inputs(
                topicCoverage = listOf(
                    coverage("topic_more_covered", attempted = 3, total = 5),
                    coverage("topic_less_covered", attempted = 1, total = 5),
                    coverage("topic_complete", attempted = 6, total = 6),
                ),
            ),
        )

        assertUnseenRecommendation(
            recommendation,
            topicId = "topic_less_covered",
            unseenCount = 4,
        )
    }

    @Test
    fun equalCoverageRatioPrefersMoreUnseenQuestions() {
        val recommendation = recommend(
            inputs(
                topicCoverage = listOf(
                    coverage("topic_small", attempted = 1, total = 2),
                    coverage("topic_large", attempted = 2, total = 4),
                ),
            ),
        )

        assertUnseenRecommendation(recommendation, topicId = "topic_large", unseenCount = 2)
    }

    @Test
    fun completeCoverageTieUsesStableTopicId() {
        val recommendation = recommend(
            inputs(
                topicCoverage = listOf(
                    coverage("topic_z", attempted = 1, total = 2),
                    coverage("topic_a", attempted = 1, total = 2),
                ),
            ),
        )

        assertUnseenRecommendation(recommendation, topicId = "topic_a", unseenCount = 1)
    }

    @Test
    fun matchingRecentFocusedContextBreaksLowestCoverageTieBeforeUnseenCount() {
        val recommendation = recommend(
            inputs(
                topicCoverage = listOf(
                    coverage("topic_recent", attempted = 1, total = 2),
                    coverage("topic_larger", attempted = 2, total = 4),
                ),
                recentStudyContext = RecentStudyContext.Focused(
                    AssessmentScope.Topic("topic_recent"),
                ),
            ),
        )

        assertUnseenRecommendation(recommendation, topicId = "topic_recent", unseenCount = 1)
    }

    @Test
    fun recentFocusedSubtopicContextResolvesThroughCurrentCoverage() {
        val recommendation = recommend(
            inputs(
                topicCoverage = listOf(
                    coverage("topic_a", attempted = 1, total = 2),
                    coverage("topic_b", attempted = 1, total = 2),
                ),
                subtopicCoverage = listOf(
                    SubtopicCoverage("topic_b", "subtopic_recent", 1, 2),
                ),
                recentStudyContext = RecentStudyContext.Focused(
                    AssessmentScope.Subtopic("subtopic_recent"),
                ),
            ),
        )

        assertUnseenRecommendation(recommendation, topicId = "topic_b", unseenCount = 1)
    }

    @Test
    fun missingStaleMixedAndUnrelatedRecentContextDoNotChangeNormalTieBreak() {
        val ignoredContexts = listOf(
            null,
            RecentStudyContext.Mixed,
            RecentStudyContext.Focused(AssessmentScope.Topic("removed_topic")),
            RecentStudyContext.Focused(AssessmentScope.Subtopic("removed_subtopic")),
            RecentStudyContext.Focused(AssessmentScope.Topic("topic_complete")),
        )

        ignoredContexts.forEach { context ->
            val recommendation = recommend(
                inputs(
                    topicCoverage = listOf(
                        coverage("topic_small", attempted = 1, total = 2),
                        coverage("topic_large", attempted = 2, total = 4),
                        coverage("topic_complete", attempted = 1, total = 1),
                    ),
                    recentStudyContext = context,
                ),
            )

            assertUnseenRecommendation(recommendation, topicId = "topic_large", unseenCount = 2)
        }
    }

    @Test
    fun coverageSelectionDoesNotDependOnInputOrdering() {
        val coverage = listOf(
            coverage("topic_z", attempted = 1, total = 2),
            coverage("topic_a", attempted = 1, total = 2),
            coverage("topic_complete", attempted = 3, total = 3),
        )

        val forward = recommend(inputs(topicCoverage = coverage))
        val reversed = recommend(inputs(topicCoverage = coverage.reversed()))

        assertEquals(forward, reversed)
        assertUnseenRecommendation(forward, topicId = "topic_a", unseenCount = 1)
    }

    @Test
    fun everyTargetCarriesRationaleForTheSamePolicyBranch() {
        val recommendations = listOf(
            recommend(
                inputs(
                    completedAttemptCount = 0,
                    topicCoverage = listOf(coverage("topic_a", 0, 1)),
                ),
            ),
            recommend(
                inputs(
                    unresolvedMistakeCount = 1,
                    topicCoverage = listOf(coverage("topic_a", 1, 1)),
                ),
            ),
            recommend(
                inputs(
                    weakAreas = listOf(weakTopic("topic_a")),
                    topicCoverage = listOf(coverage("topic_a", 1, 1)),
                ),
            ),
            recommend(
                inputs(topicCoverage = listOf(coverage("topic_a", 0, 1))),
            ),
        )

        recommendations.forEach { recommendation ->
            when (val target = recommendation.target) {
                LearningRecommendationTarget.Topics ->
                    assertIs<LearningRecommendationRationale.NewUser>(recommendation.rationale)

                LearningRecommendationTarget.MistakeReview ->
                    assertIs<LearningRecommendationRationale.UnresolvedMistakes>(
                        recommendation.rationale,
                    )

                is LearningRecommendationTarget.Practice -> when (target.preset.source) {
                    PracticeQuestionSource.WEAK_AREAS -> {
                        val rationale = assertIs<LearningRecommendationRationale.WeakArea>(
                            recommendation.rationale,
                        )
                        assertEquals(target.preset.scope, rationale.scope)
                    }

                    PracticeQuestionSource.UNSEEN -> {
                        val rationale = assertIs<LearningRecommendationRationale.UnseenCoverage>(
                            recommendation.rationale,
                        )
                        assertEquals(
                            (target.preset.scope as AssessmentScope.Topic).topicId,
                            rationale.topicId,
                        )
                    }

                    PracticeQuestionSource.ALL,
                    PracticeQuestionSource.UNRESOLVED_MISTAKES,
                    -> error("Recommendation policy must not create this practice source.")
                }
            }
        }
    }
}

private fun inputs(
    completedAttemptCount: Int = 1,
    unresolvedMistakeCount: Int = 0,
    weakAreas: List<WeakArea> = emptyList(),
    topicCoverage: List<TopicCoverage> = listOf(coverage("topic_a", 1, 1)),
    subtopicCoverage: List<SubtopicCoverage> = emptyList(),
    recentStudyContext: RecentStudyContext? = null,
): LearningRecommendationInputs =
    LearningRecommendationInputs(
        completedAttemptCount = completedAttemptCount,
        unresolvedMistakeCount = unresolvedMistakeCount,
        weakAreas = weakAreas,
        topicCoverage = topicCoverage,
        subtopicCoverage = subtopicCoverage,
        recentStudyContext = recentStudyContext,
    )

private fun coverage(
    topicId: String,
    attempted: Int,
    total: Int,
): TopicCoverage = TopicCoverage(topicId, attempted, total)

private fun weakTopic(
    topicId: String,
    name: String? = topicId,
): WeakArea =
    WeakArea.Topic(
        TopicPerformance(
            topicId = topicId,
            topicName = name,
            answeredCount = 4,
            correctCount = 1,
            percentage = 25.0,
            isWeak = true,
        ),
    )

private fun weakSubtopic(
    topicId: String,
    subtopicId: String,
): WeakArea =
    WeakArea.Subtopic(
        SubtopicPerformance(
            subtopicId = subtopicId,
            subtopicName = subtopicId,
            topicId = topicId,
            topicName = topicId,
            answeredCount = 3,
            correctCount = 1,
            percentage = 1.0 / 3.0 * 100.0,
            isWeak = true,
        ),
    )

private fun recommend(inputs: LearningRecommendationInputs): LearningRecommendation =
    assertNotNull(LearningRecommendationPolicy.recommend(inputs))

private fun assertUnseenRecommendation(
    recommendation: LearningRecommendation,
    topicId: String,
    unseenCount: Int,
) {
    val target = assertIs<LearningRecommendationTarget.Practice>(recommendation.target)
    assertEquals(
        PracticePreset(
            scope = AssessmentScope.Topic(topicId),
            source = PracticeQuestionSource.UNSEEN,
        ),
        target.preset,
    )
    assertEquals(
        LearningRecommendationRationale.UnseenCoverage(
            topicId = topicId,
            unseenQuestionCount = unseenCount,
        ),
        recommendation.rationale,
    )
}
