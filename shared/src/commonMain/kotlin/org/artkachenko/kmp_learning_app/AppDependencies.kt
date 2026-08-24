package org.artkachenko.kmp_learning_app

internal data class AppDependencies(
    val createPlaceholderDetailViewModel: (itemId: String) -> PlaceholderDetailViewModel =
        ::PlaceholderDetailViewModel,
)
