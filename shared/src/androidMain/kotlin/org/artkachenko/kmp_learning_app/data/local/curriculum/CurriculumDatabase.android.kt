package org.artkachenko.kmp_learning_app.data.local.curriculum

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

private const val CurriculumDatabaseName = "curriculum.db"

internal fun createCurriculumDatabase(context: Context): CurriculumDatabase {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath(CurriculumDatabaseName)

    return Room.databaseBuilder<CurriculumDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
        .build()
}
