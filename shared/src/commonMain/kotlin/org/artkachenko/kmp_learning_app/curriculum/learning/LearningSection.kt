package org.artkachenko.kmp_learning_app.curriculum.learning

import kotlinx.serialization.Serializable

/**
 * One depth layer of a Lesson, in authored order.
 *
 * A Lesson may carry more than one Section at the same [depth], so [title] exists for the
 * authored subheading that tells them apart. It is optional because a Lesson whose depth
 * layers are each a single Section needs no heading beyond the depth itself.
 */
@Serializable
internal data class LearningSection(
    val depth: LearningDepth,
    val blocks: List<LearningBlock>,
    val title: String? = null,
)
