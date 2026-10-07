package org.artkachenko.kmp_learning_app.learning_progress

import kotlin.time.Clock
import kotlin.time.Instant
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

/**
 * Supplies [ReadinessDerivation] with the curriculum it measures against.
 *
 * History is always the caller's: the progress dashboard hands over the visible history it already
 * holds, so readiness adds no history read of its own. [curriculumRepository] is the visible one,
 * so its ACTIVE bank already excludes hidden Topics; its historical resolver is unfiltered, which is
 * harmless here because the history it resolves is already the visible projection.
 *
 * [now] is injected, as [org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewService] does,
 * so tests decide which mistakes are due.
 */
internal class ReadinessService(
    private val curriculumRepository: CurriculumRepository,
    private val now: () -> Instant = { Clock.System.now() },
) {
    suspend fun load(
        completedAttempts: List<TestAttempt>,
        target: ReadinessTarget,
    ): ReadinessSnapshot {
        val historicalQuestionIds = completedAttempts.flatMapTo(mutableSetOf()) { attempt ->
            attempt.questionAttempts.map { it.questionId }
        }
        return ReadinessDerivation.derive(
            completedAttempts = completedAttempts,
            activeQuestions = curriculumRepository.getActiveQuestions(),
            // One batched read, and none at all for a learner with no history.
            historicalQuestions = if (historicalQuestionIds.isEmpty()) {
                emptyMap()
            } else {
                curriculumRepository.getQuestionsByIds(historicalQuestionIds)
            },
            target = target,
            now = now(),
        )
    }
}
