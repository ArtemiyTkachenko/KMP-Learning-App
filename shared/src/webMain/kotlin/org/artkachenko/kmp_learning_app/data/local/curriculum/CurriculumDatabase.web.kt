package org.artkachenko.kmp_learning_app.data.local.curriculum

import androidx.room3.Room
import org.artkachenko.kmp_learning_app.sqlite_worker.createSQLiteWasmWorker

private const val CurriculumDatabaseName = "curriculum.db"

internal fun createWebCurriculumDatabase(): CurriculumDatabase =
    Room.databaseBuilder<CurriculumDatabase>(name = CurriculumDatabaseName)
        .setDriver(createSQLiteWasmWorker())
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
        .build()
