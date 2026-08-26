package org.artkachenko.kmp_learning_app.assessment

internal data class AssessmentScore(
    val totalQuestions: Int,
    val correctAnswers: Int,
) {
    init {
        require(totalQuestions > 0) {
            "totalQuestions must be greater than zero."
        }
        require(correctAnswers in 0..totalQuestions) {
            "correctAnswers must be between zero and totalQuestions."
        }
    }

    val percentage: Double
        get() =
            correctAnswers.toDouble() /
                totalQuestions.toDouble() *
                100.0
}
