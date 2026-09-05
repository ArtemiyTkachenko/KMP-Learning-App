package org.artkachenko.kmp_learning_app.curriculum.learning.repository

import org.artkachenko.kmp_learning_app.curriculum.learning.LearningLesson
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit

/**
 * Reads over the validated learning document.
 *
 * The split mirrors `CurriculumRepository`: [getActiveUnitsByTopic] is the browsing
 * eligibility surface and returns only ACTIVE content, while [getUnitById] and
 * [getLessonById] resolve stable identity regardless of status. A deprecated Unit or
 * Lesson therefore still resolves by id, so a stored reference to retired material stays
 * readable instead of silently becoming nothing.
 *
 * Lessons resolve globally rather than within a Unit because learning-content validation
 * guarantees Lesson ids are unique across the whole document.
 *
 * Every function may throw
 * [org.artkachenko.kmp_learning_app.curriculum.learning.content.LearningContentLoadException]
 * when the bundled document cannot be loaded, which is a build-time authoring fault rather
 * than a runtime condition a caller can recover from.
 */
internal interface LearningContentRepository {
    /** ACTIVE Units whose home Topic is [topicId], in authored order. */
    suspend fun getActiveUnitsByTopic(topicId: String): List<LearningUnit>

    suspend fun getUnitById(unitId: String): LearningUnit?

    suspend fun getLessonById(lessonId: String): LearningLesson?
}
