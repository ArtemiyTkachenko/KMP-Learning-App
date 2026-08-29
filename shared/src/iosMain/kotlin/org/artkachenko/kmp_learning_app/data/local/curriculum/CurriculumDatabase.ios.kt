package org.artkachenko.kmp_learning_app.data.local.curriculum

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

private const val CurriculumDatabaseName = "curriculum.db"

internal fun createIosCurriculumDatabase(): CurriculumDatabase {
    val databasePath = "${documentDirectory()}/$CurriculumDatabaseName"

    return Room.databaseBuilder<CurriculumDatabase>(name = databasePath)
        .setDriver(BundledSQLiteDriver())
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val directory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(directory?.path) {
        "Could not resolve the iOS application Documents directory."
    }
}
