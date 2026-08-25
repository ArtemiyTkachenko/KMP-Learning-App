package org.artkachenko.kmp_learning_app.curriculum

import kotlinx.serialization.Serializable

@Serializable
internal data class Curriculum(
    val topics: List<Topic>,
    val subtopics: List<Subtopic>,
    val questions: List<Question>,
)
