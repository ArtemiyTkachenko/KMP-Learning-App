package org.artkachenko.kmp_learning_app.learning_progress

import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.history.QuestionExposure
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

internal class LearningProgressService(
    private val assessmentRepository: AssessmentRepository,
    private val curriculumRepository: CurriculumRepository,
    private val performanceDerivation: LearningPerformanceDerivation =
        LearningPerformanceDerivation(curriculumRepository),
) {
    /**
     * [completedAttempts] lets a caller that already holds newest-first completed history reuse it,
     * as [MistakeReviewService] does, so the shared cache is read once per derivation rather than
     * once per consumer.
     */
    suspend fun load(completedAttempts: List<TestAttempt>? = null): LearningProgressSnapshot {
        val completedAttempts =
            (completedAttempts ?: assessmentRepository.getCompletedAttempts())
                .filter { it.status == AssessmentStatus.COMPLETED }
        val answeredQuestionCount = completedAttempts.sumOf { requireNotNull(it.score).totalQuestions }
        val correctAnswerCount = completedAttempts.sumOf { requireNotNull(it.score).correctAnswers }
        val performance = performanceDerivation.derive(completedAttempts)
        // Exposure comes from the shared policy, and from the raw historical IDs it reads before
        // any metadata resolution: a Question that no longer resolves simply fails the ACTIVE
        // intersection below rather than being silently dropped. Deriving it there rather than in
        // the loop is what keeps coverage and unseen practice from disagreeing about what "seen"
        // means.
        val attemptedQuestionIds = QuestionExposure.observedQuestionIds(completedAttempts)

        // Coverage answers "how much of the CURRENT curriculum have I encountered?", so its
        // denominator is the ACTIVE question bank and never anything reachable from history. This
        // is the one ACTIVE read per derivation; the grouping below is entirely in memory, so no
        // per-Topic or per-Subtopic query is issued.
        val activeQuestions = curriculumRepository.getActiveQuestions()
        val activeQuestionIds = activeQuestions.mapTo(mutableSetOf(), Question::id)
        // Stable IDs deduplicate exposure: repeated occurrences of one Question collapse here,
        // while the occurrence-based accuracy above deliberately counts every one of them.
        val attemptedActiveQuestionIds = attemptedQuestionIds intersect activeQuestionIds

        return LearningProgressSnapshot(
            completedAttemptCount = completedAttempts.size,
            answeredQuestionCount = answeredQuestionCount,
            correctAnswerCount = correctAnswerCount,
            percentage = percentage(correctAnswerCount, answeredQuestionCount),
            topics = performance.topics,
            subtopics = performance.subtopics,
            weakAreas = performance.weakAreas,
            coverage = CurriculumCoverage(
                attemptedQuestionCount = attemptedActiveQuestionIds.size,
                totalQuestionCount = activeQuestions.size,
            ),
            topicCoverage = topicCoverage(activeQuestions, attemptedActiveQuestionIds),
            subtopicCoverage = subtopicCoverage(activeQuestions, attemptedActiveQuestionIds),
            // Derived from the same history, and only from it: recent performance needs persisted
            // correctness and timestamps, so it issues no curriculum query of its own.
            recentPerformance = recentPerformance(completedAttempts),
        )
    }
}

/**
 * Recent performance over the latest completed attempts, with every figure taken from persisted
 * per-question correctness.
 *
 * Persisted correctness is authoritative rather than a fresh comparison against
 * `Question.correctAnswerIds`: a Question's answer key can be corrected after an attempt was
 * answered, and history must not change retrospectively because of it. That also keeps this
 * derivation free of curriculum reads.
 *
 * Every completed attempt participates on the same terms — focused, mixed, and retake alike, since a
 * retake is simply another completed occurrence — and IN_PROGRESS attempts do not, having been
 * filtered out by the caller: recent performance describes completed evidence only.
 */
private fun recentPerformance(completedAttempts: List<TestAttempt>): RecentPerformance {
    val window = RecentPerformancePolicy.recentWindow(completedAttempts)

    return RecentPerformance(
        attemptSeries = window.map { attempt ->
            RecentAttemptPerformance(
                attemptId = attempt.id,
                completedAt = requireNotNull(attempt.completedAt),
                answeredQuestionCount = attempt.questionAttempts.size,
                correctAnswerCount = attempt.questionAttempts.count(::answeredCorrectly),
            )
        },
        answerSeries = window
            .flatMap { attempt ->
                attempt.questionAttempts.map { questionAttempt ->
                    RecentAnswerOutcome(
                        attemptId = attempt.id,
                        questionId = questionAttempt.questionId,
                        isCorrect = answeredCorrectly(questionAttempt),
                    )
                }
            }
            // Keeps the most recent outcomes when the window holds more than the cap, without
            // disturbing their chronological order. The summary above is unaffected: it comes from
            // the attempt series, which covers every answer in the window.
            .takeLast(RecentPerformancePolicy.MaxRecentAnswerOutcomes),
    )
}

private fun answeredCorrectly(questionAttempt: QuestionAttempt): Boolean =
    (questionAttempt.answerState as QuestionAnswerState.Answered).isCorrect

/**
 * Coverage groups are derived from the ACTIVE questions rather than from the attempted IDs, so a
 * Topic the learner has never opened is still present as 0/N instead of being missing. Ordering is
 * by stable ID so the domain output is deterministic without depending on repository row order or
 * on how a screen wants to sort them.
 */
private fun topicCoverage(
    activeQuestions: List<Question>,
    attemptedActiveQuestionIds: Set<String>,
): List<TopicCoverage> =
    activeQuestions
        .groupBy(Question::topicId)
        .entries
        .sortedBy { it.key }
        .map { (topicId, questions) ->
            TopicCoverage(
                topicId = topicId,
                attemptedQuestionCount = questions.count { it.id in attemptedActiveQuestionIds },
                totalQuestionCount = questions.size,
            )
        }

private fun subtopicCoverage(
    activeQuestions: List<Question>,
    attemptedActiveQuestionIds: Set<String>,
): List<SubtopicCoverage> =
    activeQuestions
        .groupBy { SubtopicKey(topicId = it.topicId, subtopicId = it.subtopicId) }
        .entries
        .sortedWith(compareBy({ it.key.topicId }, { it.key.subtopicId }))
        .map { (key, questions) ->
            SubtopicCoverage(
                topicId = key.topicId,
                subtopicId = key.subtopicId,
                attemptedQuestionCount = questions.count { it.id in attemptedActiveQuestionIds },
                totalQuestionCount = questions.size,
            )
        }

private data class SubtopicKey(
    val topicId: String,
    val subtopicId: String,
)

private fun percentage(
    correctCount: Int,
    answeredCount: Int,
): Double =
    if (answeredCount == 0) {
        0.0
    } else {
        correctCount.toDouble() / answeredCount * 100.0
    }
