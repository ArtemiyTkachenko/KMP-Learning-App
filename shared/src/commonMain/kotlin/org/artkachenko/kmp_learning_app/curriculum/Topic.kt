package org.artkachenko.kmp_learning_app.curriculum

import kotlinx.serialization.Serializable

@Serializable
internal data class Topic(
    val id: String,
    val name: String,
    val status: ContentStatus = ContentStatus.ACTIVE,
)
