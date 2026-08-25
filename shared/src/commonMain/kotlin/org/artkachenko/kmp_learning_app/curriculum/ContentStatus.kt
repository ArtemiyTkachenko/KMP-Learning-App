package org.artkachenko.kmp_learning_app.curriculum

import kotlinx.serialization.Serializable

@Serializable
internal enum class ContentStatus {
    ACTIVE,
    DEPRECATED,
}
