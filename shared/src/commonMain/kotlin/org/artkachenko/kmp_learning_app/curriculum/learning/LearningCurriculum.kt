package org.artkachenko.kmp_learning_app.curriculum.learning

import kotlinx.serialization.Serializable

/**
 * The authored learning document: explanatory study material, kept separate from the
 * assessment [org.artkachenko.kmp_learning_app.curriculum.Curriculum] because the two
 * hierarchies are deliberately not the same shape.
 *
 * Unit order is the authored order. Learning content is a publisher-owned document, so
 * list position is the ordering contract and no sort-order field is stored.
 */
@Serializable
internal data class LearningCurriculum(
    val units: List<LearningUnit>,
)
