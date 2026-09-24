package org.artkachenko.kmp_learning_app.assessment_review

import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.session.withAnswersOrderedFor
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

/**
 * One persisted occurrence to reconstruct: the attempt it belongs to, and the answer it recorded.
 *
 * [attemptId] is what orders the answers exactly as they were ordered while the attempt was being
 * taken, so review shows the learner the options in the arrangement they actually answered. It is
 * carried per occurrence rather than per batch because the mistake queue reviews one Question from
 * each of many attempts.
 */
internal data class ReviewOccurrence(
    val attemptId: String,
    val questionAttempt: QuestionAttempt,
)

internal class AssessmentReviewLoader(
    private val curriculumRepository: CurriculumRepository,
) {
    suspend fun loadQuestions(attempt: TestAttempt): List<ReviewQuestionItem> =
        loadQuestions(
            attempt.questionAttempts.map { ReviewOccurrence(attempt.id, it) },
        )

    /**
     * Maps persisted occurrences, in the order given, resolving every stable ID they reference in
     * one historical read.
     *
     * Callers that review one Question across attempts, rather than a whole attempt, use this
     * directly so historical mapping lives in one place. The read is hoisted out of the mapping
     * because resolving per occurrence costs a read transaction each, and an attempt's worth of
     * questions — or the whole unresolved mistake queue — is a list of ids known in full before any
     * of it is read. A Question the curriculum no longer holds is absent from the result and maps
     * to [ReviewQuestionItem.Missing], exactly as the per-id `null` did.
     */
    suspend fun loadQuestions(occurrences: List<ReviewOccurrence>): List<ReviewQuestionItem> {
        // An empty mistake queue is the ordinary state of a learner with nothing unresolved, and it
        // resolves to nothing without reading the curriculum.
        if (occurrences.isEmpty()) return emptyList()
        val questionsById = curriculumRepository.getQuestionsByIds(
            occurrences.mapTo(mutableSetOf()) { it.questionAttempt.questionId },
        )
        return occurrences.map { occurrence ->
            reviewItem(occurrence, questionsById[occurrence.questionAttempt.questionId])
        }
    }

    private fun reviewItem(
        occurrence: ReviewOccurrence,
        resolved: Question?,
    ): ReviewQuestionItem {
        val questionAttempt = occurrence.questionAttempt
        val question = resolved?.withAnswersOrderedFor(occurrence.attemptId)
            ?: return ReviewQuestionItem.Missing(questionAttempt.questionId)
        val answerState = questionAttempt.answerState as? QuestionAnswerState.Answered
            ?: error("Completed attempt contains an unanswered question.")

        return ReviewQuestionItem.Available(
            ReviewQuestionUiModel(
                questionId = question.id,
                topicId = question.topicId,
                subtopicId = question.subtopicId,
                text = question.text,
                isCorrect = answerState.isCorrect,
                answers = question.answers.map { answer ->
                    ReviewAnswerUiModel(
                        id = answer.id,
                        text = answer.text,
                        wasSelected = answer.id in answerState.selectedAnswerIds,
                        isCorrectAnswer = answer.id in question.correctAnswerIds,
                    )
                },
                explanation = question.explanation,
                sources = question.sources.map { source ->
                    ReviewSourceUiModel(source.title, source.url)
                },
            ),
        )
    }
}
