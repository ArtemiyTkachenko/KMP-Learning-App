package org.artkachenko.kmp_learning_app.data.local.curriculum

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.artkachenko.kmp_learning_app.AppStartupInitializer
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImportResult
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter

internal class CurriculumDataInitializer(
    private val importer: CurriculumImporter,
) : AppStartupInitializer {
    private val mutex = Mutex()
    private val initializationComplete = MutableStateFlow(false)

    override val isInitialized: Boolean
        get() = initializationComplete.value

    override suspend fun initialize() {
        if (isInitialized) return

        mutex.withLock {
            if (isInitialized) return

            when (val result = importer.importCurriculum()) {
                CurriculumImportResult.Imported -> {
                    initializationComplete.value = true
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
