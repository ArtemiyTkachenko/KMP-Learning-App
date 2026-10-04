package org.artkachenko.kmp_learning_app.curriculum.validation

internal data class CurriculumValidationError(
    val code: CurriculumValidationErrorCode,
    val entityId: String?,
    val message: String,
)

/**
 * A developer-facing report of [this] list: the count, then one `CODE [entityId] message` line per
 * error. The CI validation gate and the runtime startup diagnostic share it, so both name the rule
 * and the entity the same way.
 */
internal fun List<CurriculumValidationError>.renderForDiagnostics(): String =
    "$size error(s):\n" + joinToString("\n") { "${it.code} [${it.entityId ?: "curriculum"}] ${it.message}" }
