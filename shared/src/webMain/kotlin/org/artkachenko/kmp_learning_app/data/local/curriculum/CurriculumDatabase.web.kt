package org.artkachenko.kmp_learning_app.data.local.curriculum

import androidx.room3.Room
import org.artkachenko.kmp_learning_app.sqlite_worker.createSQLiteWasmWorker

private const val CurriculumDatabaseName = "curriculum.db"

internal fun createWebCurriculumDatabase(): CurriculumDatabase =
    Room.databaseBuilder<CurriculumDatabase>(name = CurriculumDatabaseName)
        .setDriver(createSQLiteWasmWorker())
        .addMigrations(*curriculumDatabaseMigrations.toTypedArray())
        .build()
