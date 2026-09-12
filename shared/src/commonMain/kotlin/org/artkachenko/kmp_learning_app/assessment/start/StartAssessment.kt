package org.artkachenko.kmp_learning_app.assessment.start

import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentStartResult

/** Creates the durable identity that every assessment-taking destination requires. */
internal class StartAssessment(
    private val assessmentEngine: AssessmentEngine,
    private val assessmentRepository: AssessmentRepository,
) {
    suspend operator fun invoke(config: AssessmentConfig): StartAssessmentResult =
        when (val result = assessmentEngine.start(config)) {
            AssessmentStartResult.NoEligibleQuestions -> StartAssessmentResult.NoEligibleQuestions
            is AssessmentStartResult.Started -> {
                assessmentRepository.save(result.session.attempt)
                StartAssessmentResult.Created(result.session.attempt.id)
            }
        }
}

internal sealed interface StartAssessmentResult {
    data class Created(val attemptId: String) : StartAssessmentResult
    data object NoEligibleQuestions : StartAssessmentResult
}
