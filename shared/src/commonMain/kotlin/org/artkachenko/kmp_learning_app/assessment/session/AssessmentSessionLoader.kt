package org.artkachenko.kmp_learning_app.assessment.session

import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

internal class AssessmentSessionLoader(
    private val assessmentRepository: AssessmentRepository,
    private val curriculumRepository: CurriculumRepository,
) {
    suspend fun load(attemptId: String): AssessmentSessionLoadResult {
        require(attemptId.isNotBlank()) { "attemptId must not be blank." }
        val attempt = assessmentRepository.getById(attemptId)
            ?: return AssessmentSessionLoadResult.AttemptNotFound
        if (attempt.status != AssessmentStatus.IN_PROGRESS) {
            return AssessmentSessionLoadResult.NotInProgress
        }

        val questions = buildList {
            attempt.questionAttempts.forEach { questionAttempt ->
                val question = curriculumRepository.getQuestionById(questionAttempt.questionId)
                    ?: return AssessmentSessionLoadResult.MissingQuestion(questionAttempt.questionId)
                // Derived from the attempt id, so resuming shows the same answer order the learner
                // was already looking at rather than reshuffling under them.
                add(question.withAnswersOrderedFor(attempt.id))
            }
        }
        return AssessmentSessionLoadResult.Loaded(
            AssessmentSession(attempt = attempt, questions = questions),
        )
    }
}

internal sealed interface AssessmentSessionLoadResult {
    data class Loaded(val session: AssessmentSession) : AssessmentSessionLoadResult
    data object AttemptNotFound : AssessmentSessionLoadResult
    data object NotInProgress : AssessmentSessionLoadResult
    data class MissingQuestion(val questionId: String) : AssessmentSessionLoadResult
}
