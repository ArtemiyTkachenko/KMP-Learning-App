package org.artkachenko.kmp_learning_app.curriculum.learning

import kotlinx.serialization.Serializable

/**
 * What a callout means, not how it looks. The set is deliberately small — it covers the
 * emphasis the authoring contract actually asks for, and presentation choices such as
 * colour or icon belong to the renderer.
 */
@Serializable
internal enum class LearningCalloutKind {
    NOTE,
    KEY_TAKEAWAY,
    INTERVIEW_FOCUS,
    COMMON_MISTAKE,
}
