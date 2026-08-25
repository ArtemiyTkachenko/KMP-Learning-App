package org.artkachenko.kmp_learning_app.curriculum.validation

internal data class CurriculumValidationError(
    val code: CurriculumValidationErrorCode,
    val entityId: String?,
    val message: String,
)
