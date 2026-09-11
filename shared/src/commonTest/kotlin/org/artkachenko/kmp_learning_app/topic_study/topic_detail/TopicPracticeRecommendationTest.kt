package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.ui.LearningContextUiModel

/**
 * Which practice a Topic's evidence promotes, and — just as importantly — what it refuses to
 * promote. The order is fixed and deterministic, so every case here is the rule rather than a
 * sample of it.
 */
internal class TopicPracticeRecommendationTest {

    @Test
    fun aCredibleWeakAreaIsPromotedAboveEverythingElse() {
        val recommendation = topicPracticeRecommendation(
            context = context(attempted = 12, total = 28, accuracy = 41.0, isWeak = true),
            unresolvedMistakeCount = 6,
        )

        assertEquals(TopicPracticeRecommendation.WeakAreas, recommendation)
        assertEquals(PracticeQuestionSource.WEAK_AREAS, recommendation.source)
    }

    /**
     * The weak verdict is the domain's and is never re-derived from the percentage on screen: a low
     * accuracy the policy did not call weak has not met its evidence threshold, so it must not
     * produce a weak recommendation here either.
     */
    @Test
    fun aLowPercentageTheDomainDidNotCallWeakDoesNotPromoteWeakPractice() {
        val recommendation = topicPracticeRecommendation(
            context = context(attempted = 2, total = 28, accuracy = 22.0, isWeak = false),
            unresolvedMistakeCount = 0,
        )

        assertEquals(
            TopicPracticeRecommendation.Unseen(26),
            recommendation,
        )
    }

    @Test
    fun unresolvedMistakesComeNextAndCarryTheirCount() {
        val recommendation = topicPracticeRecommendation(
            context = context(attempted = 28, total = 28, accuracy = 88.0, isWeak = false),
            unresolvedMistakeCount = 4,
        )

        assertEquals(TopicPracticeRecommendation.Mistakes(4), recommendation)
        assertEquals(PracticeQuestionSource.UNRESOLVED_MISTAKES, recommendation.source)
    }

    @Test
    fun unseenQuestionsAreTheWeakestClaimAndCarryTheRemainder() {
        val recommendation = topicPracticeRecommendation(
            context = context(attempted = 12, total = 28, accuracy = 88.0, isWeak = false),
            unresolvedMistakeCount = 0,
        )

        assertEquals(TopicPracticeRecommendation.Unseen(16), recommendation)
        assertEquals(PracticeQuestionSource.UNSEEN, recommendation.source)
    }

    /**
     * On a Topic nobody has touched, "unseen" is true of the whole bank, so an unseen run and an
     * ordinary run would draw from one identical pool. Two labels for one outcome is not a
     * recommendation.
     */
    @Test
    fun anUntouchedTopicPromotesNothingRatherThanOfferingTheWholeBankTwice() {
        val recommendation = topicPracticeRecommendation(
            context = context(attempted = 0, total = 28, accuracy = null, isWeak = false),
            unresolvedMistakeCount = 0,
        )

        assertEquals(TopicPracticeRecommendation.Everything, recommendation)
        assertEquals(PracticeQuestionSource.ALL, recommendation.source)
    }

    @Test
    fun aFullyCoveredTopicWithNothingWrongPromotesNothing() {
        val recommendation = topicPracticeRecommendation(
            context = context(attempted = 28, total = 28, accuracy = 92.0, isWeak = false),
            unresolvedMistakeCount = 0,
        )

        assertEquals(TopicPracticeRecommendation.Everything, recommendation)
    }

    /**
     * Unknown is not empty. Analytics that have not loaded must not read as "not weak, nothing left
     * to see", and unknown history must not read as "no mistakes" — but neither may an absent
     * signal suppress one that *was* read, which is why they are asked separately.
     */
    @Test
    fun unknownSignalsNeitherProduceNorSuppressARecommendation() {
        assertEquals(
            TopicPracticeRecommendation.Everything,
            topicPracticeRecommendation(context = null, unresolvedMistakeCount = null),
        )

        // History unknown, analytics read: the weak verdict still speaks.
        assertEquals(
            TopicPracticeRecommendation.WeakAreas,
            topicPracticeRecommendation(
                context = context(attempted = 10, total = 10, accuracy = 40.0, isWeak = true),
                unresolvedMistakeCount = null,
            ),
        )

        // Analytics unknown, history read: the mistake count still speaks.
        assertEquals(
            TopicPracticeRecommendation.Mistakes(3),
            topicPracticeRecommendation(context = null, unresolvedMistakeCount = 3),
        )
    }

    private fun context(
        attempted: Int,
        total: Int,
        accuracy: Double?,
        isWeak: Boolean,
    ): LearningContextUiModel =
        LearningContextUiModel(
            attemptedQuestionCount = attempted,
            totalQuestionCount = total,
            coveragePercentage = if (total == 0) null else attempted.toDouble() / total * 100,
            accuracyPercentage = accuracy,
            isWeak = isWeak,
        )
}
