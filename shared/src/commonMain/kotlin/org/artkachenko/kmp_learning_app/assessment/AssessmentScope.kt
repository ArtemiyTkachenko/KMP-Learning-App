package org.artkachenko.kmp_learning_app.assessment

internal sealed interface AssessmentScope {
    data class Topic(
        val topicId: String,
    ) : AssessmentScope {
        init {
            require(topicId.isNotBlank()) {
                "topicId must not be blank."
            }
        }
    }

    data class Subtopic(
        val subtopicId: String,
    ) : AssessmentScope {
        init {
            require(subtopicId.isNotBlank()) {
                "subtopicId must not be blank."
            }
        }
    }
}
