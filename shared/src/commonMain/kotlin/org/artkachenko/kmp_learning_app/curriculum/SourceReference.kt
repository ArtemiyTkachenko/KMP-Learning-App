package org.artkachenko.kmp_learning_app.curriculum

import kotlinx.serialization.Serializable

@Serializable
internal data class SourceReference(
    val title: String,
    val url: String,
)
