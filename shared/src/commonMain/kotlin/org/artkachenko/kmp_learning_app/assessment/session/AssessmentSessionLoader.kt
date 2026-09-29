package org.artkachenko.kmp_learning_app.assessment.session

import kotlinx.coroutines.flow.StateFlow
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibility

/**
 * Rebuilds an in-progress attempt into a session that can be taken.
 *
 * Questions are resolved through the historical resolver, which deliberately returns hidden content,
 * so this is also where an in-progress attempt is checked against the learner's
 * [CurriculumVisibility]. An attempt is never partially filtered: its persisted Question list is part
 * of its state machine — numbering, the first unanswered position, completion and the persisted score
 * all count it — so removing a hidden Question for presentation would make the session disagree with
 * storage. An attempt that contains any Question known to belong to a hidden Topic is
 * [AssessmentSessionLoadResult.ContentUnavailable] instead, and stays stored unchanged so it can be
 * resumed once that content is visible again.
 */
internal class AssessmentSessionLoader(
    private val assessmentRepository: AssessmentRepository,
    private val curriculumRepository: CurriculumRepository,
    private val visibility: StateFlow<CurriculumVisibility>,
) {
    suspend fun load(attemptId: String): AssessmentSessionLoadResult = load(attemptId, visibility.value)

    /**
     * Loads [attemptId] as [visibility] allows it to be shown. Public beside [load] so route
     * classification judges a parked attempt route under the visibility it is validating, by
     * exactly the rule this destination applies.
     */
    suspend fun load(
        attemptId: String,
        visibility: CurriculumVisibility,
    ): AssessmentSessionLoadResult {
        require(attemptId.isNotBlank()) { "attemptId must not be blank." }
        val attempt = assessmentRepository.getById(attemptId)
            ?: return AssessmentSessionLoadResult.AttemptNotFound
        if (attempt.status != AssessmentStatus.IN_PROGRESS) {
            return AssessmentSessionLoadResult.NotInProgress
        }

        // Resolved together, then walked in the attempt's own order: an attempt holds every stable
        // ID it needs, and resuming should not cost a read transaction per question.
        val questionsById = curriculumRepository.getQuestionsByIds(
            attempt.questionAttempts.mapTo(mutableSetOf(), QuestionAttempt::questionId),
        )
        // Only a Question that resolved can be known to be hidden. An ID that resolves to nothing is
        // not evidence of a hidden Topic, so it still reaches MissingQuestion below.
        if (questionsById.values.any { !visibility.isTopicVisible(it.topicId) }) {
            return AssessmentSessionLoadResult.ContentUnavailable
        }
        val questions = buildList {
            attempt.questionAttempts.forEach { questionAttempt ->
                // Still the first missing question in attempt order, which is what the result
                // names: the read above changes when the rows are fetched, not which id fails.
                val question = questionsById[questionAttempt.questionId]
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

    /**
     * The attempt exists and is in progress, but contains a Question of a hidden Topic. A settled
     * answer about current visibility, not a failure: nothing was changed, and the same attempt
     * loads normally once that Topic is visible again.
     */
    data object ContentUnavailable : AssessmentSessionLoadResult
}
