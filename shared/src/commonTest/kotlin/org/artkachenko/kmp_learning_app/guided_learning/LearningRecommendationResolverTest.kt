package org.artkachenko.kmp_learning_app.guided_learning

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.learning_progress.CurriculumCoverage
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressSnapshot
import org.artkachenko.kmp_learning_app.learning_progress.RecentPerformance
import org.artkachenko.kmp_learning_app.learning_progress.SubtopicCoverage
import org.artkachenko.kmp_learning_app.learning_progress.SubtopicPerformance
import org.artkachenko.kmp_learning_app.learning_progress.TopicCoverage
import org.artkachenko.kmp_learning_app.learning_progress.TopicPerformance
import org.artkachenko.kmp_learning_app.learning_progress.WeakArea
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewService

/**
 * The integration around [LearningRecommendationPolicy], not the policy itself.
 *
 * `LearningRecommendationPolicyTest` already pins precedence, weak-area ordering, and coverage
 * tie-breaking, so none of that is retested here. What these tests establish is that the resolver
 * supplies the *established* facts: the caller's progress snapshot rather than a second derivation,
 * the shared unresolved-mistake semantics rather than a count of its own, and the shared
 * recent-study definition rather than Continue Studying's navigation answer.
 */
internal class LearningRecommendationResolverTest {
    @Test
    fun aLearnerWithNoCompletedHistoryIsSentToBrowseTopics() = runTest {
        val recommendation = resolve(
            completedAttempts = emptyList(),
            progress = snapshot(
                completedAttemptCount = 0,
                topicCoverage = listOf(TopicCoverage("kotlin", 0, 10)),
            ),
        )

        // Browse Topics, not a Topic: the deterministic starting point is the list itself, and
        // choosing one for the learner here would be a recommendation rule nobody wrote down.
        assertEquals(LearningRecommendationTarget.Topics, recommendation?.target)
        assertEquals(LearningRecommendationRationale.NewUser, recommendation?.rationale)
    }

    @Test
    fun noUsableActiveCurriculumJustifiesNothing() = runTest {
        assertNull(
            resolve(
                completedAttempts = emptyList(),
                progress = snapshot(
                    completedAttemptCount = 0,
                    topicCoverage = listOf(TopicCoverage("kotlin", 0, 0)),
                ),
            ),
        )
    }

    @Test
    fun theUnresolvedCountComesFromTheSharedMistakeSemantics() = runTest {
        // q_one was answered incorrectly and then correctly, so it is resolved; q_two's latest
        // completed occurrence is still incorrect. The count the recommendation reports has to be
        // the mistake queue's own answer, not a tally of wrong answers in history.
        val history = listOf(
            completedAttempt("newer", "q_one" to true),
            completedAttempt("older", "q_one" to false, "q_two" to false),
        )
        val recommendation = resolve(
            completedAttempts = history,
            progress = snapshot(
                completedAttemptCount = 2,
                topicCoverage = listOf(TopicCoverage("kotlin", 2, 10)),
                // Present and usable, and still outranked: mistakes come first.
                weakAreas = listOf(weakTopic("kotlin")),
            ),
            unresolvedMistakeCounter = SharedMistakeSemantics,
        )

        assertEquals(LearningRecommendationTarget.MistakeReview, recommendation?.target)
        assertEquals(
            LearningRecommendationRationale.UnresolvedMistakes(count = 1),
            recommendation?.rationale,
        )
    }

    @Test
    fun aUsableWeakAreaBecomesAnEditableWeakAreaPreset() = runTest {
        val recommendation = resolve(
            completedAttempts = listOf(completedAttempt("attempt", "q_one" to true)),
            progress = snapshot(
                completedAttemptCount = 1,
                topicCoverage = listOf(TopicCoverage("kotlin", 4, 10)),
                subtopicCoverage = listOf(SubtopicCoverage("kotlin", "coroutines", 4, 6)),
                weakAreas = listOf(weakSubtopic(topicId = "kotlin", subtopicId = "coroutines")),
            ),
            unresolvedMistakeCounter = SharedMistakeSemantics,
        )

        assertEquals(
            LearningRecommendationTarget.Practice(
                PracticePreset(
                    scope = AssessmentScope.Subtopic("coroutines"),
                    source = PracticeQuestionSource.WEAK_AREAS,
                ),
            ),
            recommendation?.target,
        )
        assertEquals(
            LearningRecommendationRationale.WeakArea(
                scope = AssessmentScope.Subtopic("coroutines"),
                areaName = "Coroutines",
            ),
            recommendation?.rationale,
        )
    }

    @Test
    fun remainingUnseenContentBecomesATopicScopedUnseenPreset() = runTest {
        val recommendation = resolve(
            completedAttempts = listOf(completedAttempt("attempt", "q_one" to true)),
            progress = snapshot(
                completedAttemptCount = 1,
                topicCoverage = listOf(
                    TopicCoverage("kotlin", 9, 10),
                    TopicCoverage("compose", 2, 10),
                ),
            ),
            unresolvedMistakeCounter = SharedMistakeSemantics,
        )

        assertEquals(
            LearningRecommendationTarget.Practice(
                PracticePreset(
                    scope = AssessmentScope.Topic("compose"),
                    source = PracticeQuestionSource.UNSEEN,
                ),
            ),
            recommendation?.target,
        )
        assertEquals(
            LearningRecommendationRationale.UnseenCoverage(
                topicId = "compose",
                unseenQuestionCount = 8,
            ),
            recommendation?.rationale,
        )
    }

    @Test
    fun nothingLeftToPractiseIsReportedAsNoRecommendation() = runTest {
        // No unresolved mistakes, no weak area, and every current Question already seen. Rather
        // than manufacturing an action, the policy declines and the surface shows nothing.
        assertNull(
            resolve(
                completedAttempts = listOf(completedAttempt("attempt", "q_one" to true)),
                progress = snapshot(
                    completedAttemptCount = 1,
                    topicCoverage = listOf(TopicCoverage("kotlin", 10, 10)),
                ),
                unresolvedMistakeCounter = SharedMistakeSemantics,
            ),
        )
    }

    @Test
    fun theNewestCompletedContextBreaksAnOtherwiseTiedCoverageDecision() = runTest {
        val progress = snapshot(
            completedAttemptCount = 2,
            // Identical ratios and identical unseen counts, so only the recent context separates
            // them; without it the stable ID ordering would choose "compose".
            topicCoverage = listOf(
                TopicCoverage("compose", 2, 10),
                TopicCoverage("kotlin", 2, 10),
            ),
        )

        val withoutContext = resolve(
            completedAttempts = listOf(completedAttempt("mixed", "q_one" to true)),
            progress = progress,
            unresolvedMistakeCounter = SharedMistakeSemantics,
        )
        assertEquals(
            LearningRecommendationRationale.UnseenCoverage("compose", 8),
            withoutContext?.rationale,
        )

        val withContext = resolve(
            completedAttempts = listOf(
                completedFocusedAttempt("newer", AssessmentScope.Topic("kotlin")),
                completedAttempt("older", "q_one" to true),
            ),
            progress = progress,
            unresolvedMistakeCounter = SharedMistakeSemantics,
        )
        assertEquals(
            LearningRecommendationRationale.UnseenCoverage("kotlin", 8),
            withContext?.rationale,
        )
    }

    @Test
    fun aMixedNewestAttemptIdentifiesNoTopicToPrefer() = runTest {
        // Real recent activity that names no scope. It must not have a Topic inferred from the
        // Questions it happened to ask, so the tie falls back to the established ordering.
        val recommendation = resolve(
            completedAttempts = listOf(completedAttempt("mixed", "q_kotlin" to true)),
            progress = snapshot(
                completedAttemptCount = 1,
                topicCoverage = listOf(
                    TopicCoverage("compose", 2, 10),
                    TopicCoverage("kotlin", 2, 10),
                ),
            ),
            unresolvedMistakeCounter = SharedMistakeSemantics,
        )

        assertEquals(
            LearningRecommendationRationale.UnseenCoverage("compose", 8),
            recommendation?.rationale,
        )
    }

    @Test
    fun anInProgressAttemptNeitherSuppliesNorErasesRecentContext() = runTest {
        // Completed history is the normal input, so this is defensive: an unfinished attempt at the
        // top must not become the recent context, and must not hide the completed one behind it.
        val recommendation = resolve(
            completedAttempts = listOf(
                inProgressFocusedAttempt("unfinished", AssessmentScope.Topic("compose")),
                completedFocusedAttempt("finished", AssessmentScope.Topic("kotlin")),
            ),
            progress = snapshot(
                completedAttemptCount = 1,
                topicCoverage = listOf(
                    TopicCoverage("compose", 2, 10),
                    TopicCoverage("kotlin", 2, 10),
                ),
            ),
            unresolvedMistakeCounter = SharedMistakeSemantics,
        )

        assertEquals(
            LearningRecommendationRationale.UnseenCoverage("kotlin", 8),
            recommendation?.rationale,
        )
    }

    @Test
    fun anUnknownUnresolvedCountFailsRatherThanReadingAsZero() = runTest {
        // Zero is a decision the policy acts on: it falls through to weak areas and then coverage.
        // An unknown count must therefore fail the derivation, leaving the surface with nothing,
        // rather than quietly recommending practice on evidence nobody established.
        assertFailsWith<IllegalStateException> {
            resolve(
                completedAttempts = listOf(completedAttempt("attempt", "q_one" to false)),
                progress = snapshot(
                    completedAttemptCount = 1,
                    topicCoverage = listOf(TopicCoverage("kotlin", 2, 10)),
                    weakAreas = listOf(weakTopic("kotlin")),
                ),
                unresolvedMistakeCounter = { error("Unresolved mistakes unavailable") },
            )
        }
    }

    private suspend fun resolve(
        completedAttempts: List<TestAttempt>,
        progress: LearningProgressSnapshot,
        unresolvedMistakeCounter: UnresolvedMistakeCounter = UnresolvedMistakeCounter { 0 },
    ): LearningRecommendation? =
        LearningRecommendationResolver(unresolvedMistakeCounter)
            .resolve(completedAttempts = completedAttempts, progress = progress)

    private companion object {
        /**
         * The real mistake queue, counting from caller-supplied history.
         *
         * Its repository throws and its review loader reads a curriculum that answers nothing,
         * which is the point: counting unresolved mistakes for history the caller already holds
         * must not read the shared cache again or reconstruct any review content.
         */
        val SharedMistakeSemantics = UnresolvedMistakeCounter { completedAttempts ->
            MistakeReviewService(
                assessmentRepository = UnreadableAssessmentRepository,
                assessmentReviewLoader = AssessmentReviewLoader(UnusedCurriculumRepository),
            ).countUnresolved(completedAttempts)
        }

        fun snapshot(
            completedAttemptCount: Int,
            topicCoverage: List<TopicCoverage>,
            subtopicCoverage: List<SubtopicCoverage> = emptyList(),
            weakAreas: List<WeakArea> = emptyList(),
        ): LearningProgressSnapshot =
            LearningProgressSnapshot(
                completedAttemptCount = completedAttemptCount,
                answeredQuestionCount = 0,
                correctAnswerCount = 0,
                percentage = 0.0,
                topics = emptyList(),
                subtopics = emptyList(),
                weakAreas = weakAreas,
                coverage = CurriculumCoverage(
                    attemptedQuestionCount = topicCoverage.sumOf { it.attemptedQuestionCount },
                    totalQuestionCount = topicCoverage.sumOf { it.totalQuestionCount },
                ),
                topicCoverage = topicCoverage,
                subtopicCoverage = subtopicCoverage,
                recentPerformance = RecentPerformance(emptyList(), emptyList()),
            )

        fun weakTopic(topicId: String): WeakArea.Topic =
            WeakArea.Topic(
                TopicPerformance(
                    topicId = topicId,
                    topicName = topicId.replaceFirstChar(Char::uppercase),
                    answeredCount = 10,
                    correctCount = 3,
                    percentage = 30.0,
                    isWeak = true,
                ),
            )

        fun weakSubtopic(topicId: String, subtopicId: String): WeakArea.Subtopic =
            WeakArea.Subtopic(
                SubtopicPerformance(
                    subtopicId = subtopicId,
                    subtopicName = subtopicId.replaceFirstChar(Char::uppercase),
                    topicId = topicId,
                    topicName = topicId.replaceFirstChar(Char::uppercase),
                    answeredCount = 10,
                    correctCount = 3,
                    percentage = 30.0,
                    isWeak = true,
                ),
            )

        fun completedAttempt(
            id: String,
            vararg answers: Pair<String, Boolean>,
        ): TestAttempt =
            TestAttempt(
                id = id,
                config = AssessmentConfig.Mixed(answers.size),
                questionAttempts = answers.map { (questionId, isCorrect) ->
                    QuestionAttempt(
                        questionId,
                        QuestionAnswerState.Answered(setOf("${questionId}_a"), isCorrect),
                    )
                },
                status = AssessmentStatus.COMPLETED,
                startedAt = Instant.parse("2026-08-29T00:00:00Z"),
                completedAt = Instant.parse("2026-08-29T00:15:00Z"),
                score = AssessmentScore(answers.size, answers.count { it.second }),
            )

        fun completedFocusedAttempt(id: String, scope: AssessmentScope): TestAttempt =
            completedAttempt(id, "q_$id" to true)
                .copy(config = AssessmentConfig.Focused(scope = scope, questionCount = 1))

        fun inProgressFocusedAttempt(id: String, scope: AssessmentScope): TestAttempt =
            TestAttempt(
                id = id,
                config = AssessmentConfig.Focused(scope = scope, questionCount = 1),
                questionAttempts = listOf(
                    QuestionAttempt("q_$id", QuestionAnswerState.Unanswered),
                ),
                status = AssessmentStatus.IN_PROGRESS,
                startedAt = Instant.parse("2026-08-30T00:00:00Z"),
            )
    }

    private object UnreadableAssessmentRepository : AssessmentRepository {
        override suspend fun save(attempt: TestAttempt) = Unit
        override suspend fun getById(attemptId: String): TestAttempt? = null
        override suspend fun getCompletedAttempts(): List<TestAttempt> =
            error("Completed history is supplied by the caller and must not be read again.")
    }

    private object UnusedCurriculumRepository : CurriculumRepository {
        override suspend fun getActiveTopics(): List<Topic> = unused()

        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> = unused()

        override suspend fun getActiveQuestions(): List<Question> = unused()

        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> = unused()

        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
            unused()

        override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> =
            unused()

        override suspend fun getActiveQuestionsByTopicAndLevels(
            topicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = unused()

        override suspend fun getActiveQuestionsBySubtopicAndLevels(
            subtopicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = unused()

        override suspend fun getTopicById(topicId: String): Topic? = unused()

        override suspend fun getSubtopicById(subtopicId: String): Subtopic? = unused()

        override suspend fun getQuestionById(questionId: String): Question? = unused()

        private fun unused(): Nothing =
            error("Counting unresolved mistakes must not read curriculum content.")
    }
}
