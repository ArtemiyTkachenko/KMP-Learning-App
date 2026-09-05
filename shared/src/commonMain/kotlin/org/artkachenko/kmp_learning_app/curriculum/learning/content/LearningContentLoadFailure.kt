package org.artkachenko.kmp_learning_app.curriculum.learning.content

import org.artkachenko.kmp_learning_app.curriculum.learning.validation.LearningCurriculumValidationError

/**
 * Why bundled learning content could not be made available.
 *
 * The two cases are kept apart because they fail for different reasons and are fixed in
 * different places: [Decode] means the shipped JSON does not match the serialized model at
 * all, while [Validation] means it decoded fine but says something the assessment
 * curriculum does not support.
 *
 * Neither case degrades into an empty document. An invalid bundle and a valid bundle that
 * happens to contain zero Units are different states, and consumers must be able to tell
 * them apart.
 */
internal sealed interface LearningContentLoadFailure {
    data class Decode(
        val cause: Throwable,
    ) : LearningContentLoadFailure

    data class Validation(
        val errors: List<LearningCurriculumValidationError>,
    ) : LearningContentLoadFailure
}

/**
 * Thrown instead of exposing partially usable learning content.
 *
 * Loading sits below `LearningContentRepository`, whose functions return domain models
 * rather than results, so a failure has to travel as an exception. It carries [failure] so
 * a caller can distinguish a malformed bundle from an invalid one without parsing a
 * message, and so the underlying authoring errors survive.
 */
internal class LearningContentLoadException(
    val failure: LearningContentLoadFailure,
) : IllegalStateException(failure.describe())

private fun LearningContentLoadFailure.describe(): String =
    when (this) {
        is LearningContentLoadFailure.Decode ->
            "Bundled learning content could not be decoded: ${cause.message}"
        is LearningContentLoadFailure.Validation ->
            "Bundled learning content failed validation: ${errors.joinToString { it.message }}"
    }
