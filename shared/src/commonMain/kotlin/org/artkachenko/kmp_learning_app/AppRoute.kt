package org.artkachenko.kmp_learning_app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface AppRoute : NavKey {
    @Serializable
    data object PlaceholderStart : AppRoute

    @Serializable
    data class PlaceholderDetail(val itemId: String) : AppRoute
}
