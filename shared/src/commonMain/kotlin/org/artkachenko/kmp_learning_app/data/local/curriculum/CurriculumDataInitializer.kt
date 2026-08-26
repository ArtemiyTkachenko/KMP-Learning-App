package org.artkachenko.kmp_learning_app.data.local.curriculum

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImportResult
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter

internal class CurriculumDataInitializer(
    private val importer: CurriculumImporter,
) {
    private val mutex = Mutex()
    private var initialized = false

    suspend fun initialize() {
        if (initialized) return

        mutex.withLock {
            if (initialized) return

            when (val result = importer.importCurriculum()) {
                CurriculumImportResult.Imported -> {
                    initialized = true
                }
                is CurriculumImportResult.Rejected -> {
                    throw IllegalStateException(
                        "Bundled curriculum failed validation: ${result.errors.joinToString { it.message }}",
                    )
                }
            }
        }
    }
}
