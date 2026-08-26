package org.artkachenko.kmp_learning_app.assessment

internal sealed interface AssessmentConfig {
    val questionCount: Int

    data class Focused(
        val scope: AssessmentScope,
        override val questionCount: Int,
    ) : AssessmentConfig {
        init {
            requirePositiveQuestionCount(questionCount)
        }
    }

    data class Mixed(
        override val questionCount: Int,
    ) : AssessmentConfig {
        init {
            requirePositiveQuestionCount(questionCount)
        }
    }
}

private fun requirePositiveQuestionCount(questionCount: Int) {
    require(questionCount > 0) {
        "questionCount must be greater than zero."
    }
}
