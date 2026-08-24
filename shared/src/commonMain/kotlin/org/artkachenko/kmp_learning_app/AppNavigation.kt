@file:OptIn(ExperimentalSerializationApi::class)

package org.artkachenko.kmp_learning_app

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

internal val appNavigationSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            // The Android-only overload can discover route serializers reflectively, but iOS,
            // JS, and Wasm need an explicit module for NavKey's open polymorphic boundary.
            subclassesOfSealed<AppRoute>()
        }
    }
}
