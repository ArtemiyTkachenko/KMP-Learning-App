package org.artkachenko.kmp_learning_app.data.local.curriculum.importer

import org.artkachenko.kmp_learning_app.curriculum.validation.CurriculumValidationError

internal sealed interface CurriculumImportResult {
    data object Imported : CurriculumImportResult

    data class Rejected(
        val errors: List<CurriculumValidationError>,
    ) : CurriculumImportResult
}
