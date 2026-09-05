package org.artkachenko.kmp_learning_app.curriculum.learning

import kotlinx.serialization.Serializable

/**
 * Depth layers of one Lesson, read from top to bottom. These are layers of the same
 * material rather than difficulty settings or separate Lesson variants, and a Lesson is
 * not required to carry all three: [SENIOR] is omitted where deeper material would be
 * artificial.
 */
@Serializable
internal enum class LearningDepth {
    CORE,
    PRACTICAL,
    SENIOR,
}
