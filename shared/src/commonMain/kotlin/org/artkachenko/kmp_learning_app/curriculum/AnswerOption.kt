package org.artkachenko.kmp_learning_app.curriculum

import kotlinx.serialization.Serializable

@Serializable
internal data class AnswerOption(
    val id: String,
    val text: String,
)
