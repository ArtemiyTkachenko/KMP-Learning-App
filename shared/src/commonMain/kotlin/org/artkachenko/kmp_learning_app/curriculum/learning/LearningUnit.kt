package org.artkachenko.kmp_learning_app.curriculum.learning

import kotlinx.serialization.Serializable
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus

/**
 * A group of concepts that make sense to learn together.
 *
 * [topicId] is the home Topic, which decides where the Unit is browsed. It deliberately
 * does not constrain the Topics a Lesson may reference: cross-Topic bridging is a core
 * product rule of the learning-content authoring contract.
 */
@Serializable
internal data class LearningUnit(
    val id: String,
    val topicId: String,
    val title: String,
    val summary: String,
    val lessons: List<LearningLesson>,
    val status: ContentStatus = ContentStatus.ACTIVE,
)
