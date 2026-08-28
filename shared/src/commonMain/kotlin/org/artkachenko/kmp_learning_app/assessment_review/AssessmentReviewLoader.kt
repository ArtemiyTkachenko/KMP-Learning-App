package org.artkachenko.kmp_learning_app.assessment_review

import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

internal class AssessmentReviewLoader(
    private val curriculumRepository: CurriculumRepository,
) {
    suspend fun loadQuestions(attempt: TestAttempt): List<ReviewQuestionItem> =
        attempt.questionAttempts.map { questionAttempt ->
            val question = curriculumRepository.getQuestionById(questionAttempt.questionId)
                ?: return@map ReviewQuestionItem.Missing(questionAttempt.questionId)
            val answerState = questionAttempt.answerState as? QuestionAnswerState.Answered
                ?: error("Completed attempt contains an unanswered question.")

            ReviewQuestionItem.Available(
                ReviewQuestionUiModel(
                    questionId = question.id,
                    topicId = question.topicId,
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
