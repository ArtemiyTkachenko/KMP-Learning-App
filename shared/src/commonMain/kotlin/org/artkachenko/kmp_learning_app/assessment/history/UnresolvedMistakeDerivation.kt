package org.artkachenko.kmp_learning_app.assessment.history

import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt

/**
 * The single definition of whether a Question is currently an unresolved mistake.
 *
 * Attempts are consumed in their persisted newest-first order. The first occurrence of each stable
 * Question ID is therefore authoritative: persisted incorrect means unresolved, while persisted
 * correct means resolved and makes every older occurrence irrelevant. Current curriculum content
 * is deliberately absent from this derivation because historical state must survive answer-key
 * changes, deprecation, and missing content.
 */
internal object UnresolvedMistakeDerivation {
    fun derive(attempts: List<TestAttempt>): List<UnresolvedMistakeOccurrence> {
        val seenQuestionIds = mutableSetOf<String>()
        val unresolved = mutableListOf<UnresolvedMistakeOccurrence>()

        for (attempt in attempts) {
            if (attempt.status != AssessmentStatus.COMPLETED) continue
            for (questionAttempt in attempt.questionAttempts) {
                if (!seenQuestionIds.add(questionAttempt.questionId)) continue

                val answered = questionAttempt.answerState as QuestionAnswerState.Answered
                if (!answered.isCorrect) {
                    unresolved += UnresolvedMistakeOccurrence(
                        questionId = questionAttempt.questionId,
                        sourceAttemptId = attempt.id,
                        questionAttempt = questionAttempt,
                    )
                }
            }
        }

        return unresolved
    }
}

/** The latest persisted incorrect occurrence of one stable Question ID. */
internal data class UnresolvedMistakeOccurrence(
    val questionId: String,
    val sourceAttemptId: String,
    val questionAttempt: QuestionAttempt,
)
