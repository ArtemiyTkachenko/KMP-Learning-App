package org.artkachenko.kmp_learning_app.curriculum.learning.validation

/**
 * One authoring defect found in the learning document.
 *
 * [entityId] is the most specific identity the defect can be attributed to — the Lesson id
 * for anything inside a Lesson, the Unit id for Unit-level defects — so an author can find
 * the content without reading the message. It is null when the defective content has no
 * usable identity, which in practice means a blank id.
 */
internal data class LearningCurriculumValidationError(
    val code: LearningCurriculumValidationErrorCode,
    val entityId: String?,
    val message: String,
)
