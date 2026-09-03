package org.artkachenko.kmp_learning_app.assessment_review

import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.session.withAnswersOrderedFor
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

internal class AssessmentReviewLoader(
    private val curriculumRepository: CurriculumRepository,
) {
    suspend fun loadQuestions(attempt: TestAttempt): List<ReviewQuestionItem> =
        attempt.questionAttempts.map { questionAttempt ->
            loadQuestion(attempt.id, questionAttempt)
        }

    /**
     * Maps a single persisted occurrence. Callers that review one Question across attempts, rather
     * than a whole attempt, use this directly so historical mapping lives in one place.
     *
     * [attemptId] is the attempt the occurrence belongs to. It orders the answers exactly as they
     * were ordered while the attempt was being taken, so review shows the learner the options in
     * the arrangement they actually answered.
     */
    suspend fun loadQuestion(
        attemptId: String,
        questionAttempt: QuestionAttempt,
    ): ReviewQuestionItem {
        val question = curriculumRepository.getQuestionById(questionAttempt.questionId)
            ?.withAnswersOrderedFor(attemptId)
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
