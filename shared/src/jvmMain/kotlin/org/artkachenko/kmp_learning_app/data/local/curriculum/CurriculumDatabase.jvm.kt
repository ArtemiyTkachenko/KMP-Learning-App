package org.artkachenko.kmp_learning_app.data.local.curriculum

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

private const val AppDirectoryName = ".kmp-learning-app"
private const val CurriculumDatabaseName = "curriculum.db"

internal fun createJvmCurriculumDatabase(): CurriculumDatabase {
    val appDirectory = File(
        System.getProperty("user.home"),
        AppDirectoryName,
    )
    appDirectory.mkdirs()

    val databaseFile = File(
        appDirectory,
        CurriculumDatabaseName,
    )

    return Room.databaseBuilder<CurriculumDatabase>(
        name = databaseFile.absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
        .addMigrations(MIGRATION_1_2)
        .build()
}
