package org.artkachenko.kmp_learning_app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface AppRoute : NavKey {
    @Serializable
    data object Topics : AppRoute

    @Serializable
    data class Topic(
        val topicId: String,
    ) : AppRoute
}
