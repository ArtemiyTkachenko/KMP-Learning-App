package org.artkachenko.kmp_learning_app.data.local.lesson_study.repository

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.lesson_study.lessonStudyDataModule
import org.artkachenko.kmp_learning_app.lesson_study.StudiedLesson
import org.artkachenko.kmp_learning_app.lesson_study.repository.LessonStudyRepository
import org.koin.dsl.koinApplication
import org.koin.dsl.module

internal class LocalLessonStudyRepositoryTest {
    @Test
    fun markingPersistsIdentityAndReportsIndividualState() = runTest {
        withTestDatabase { database ->
            val repository = repository(database, epochMillis = 1_000)

            assertFalse(repository.isStudied("lesson_a"))

            repository.markStudied("lesson_a")

            assertTrue(repository.isStudied("lesson_a"))
            assertFalse(repository.isStudied("lesson_b"))
            assertEquals(
                listOf(StudiedLesson("lesson_a", studiedAtEpochMillis = 1_000)),
                repository.getStudiedLessons(),
            )
            assertEquals(1, database.studiedLessonDao().count())
        }
    }

    @Test
    fun repeatedMarkIsANoOpAndPreservesTheOriginalRecordedTime() = runTest {
        withTestDatabase { database ->
            var epochMillis = 1_000L
            val repository = LocalLessonStudyRepository(
                database = database,
                now = { Instant.fromEpochMilliseconds(epochMillis) },
            )

            repository.markStudied("lesson_a")
            epochMillis = 2_000
            repository.markStudied("lesson_a")

            // The first explicit mark is when the current studied fact began, so a later
            // mark of an already-studied Lesson must not move the recorded time forward.
            assertEquals(
                listOf(StudiedLesson("lesson_a", studiedAtEpochMillis = 1_000)),
                repository.getStudiedLessons(),
            )
            assertEquals(1, database.studiedLessonDao().count())
        }
    }

    @Test
    fun unmarkRemovesTheRecordAndRepeatedUnmarkIsSafe() = runTest {
        withTestDatabase { database ->
            val repository = repository(database)
            repository.markStudied("lesson_a")

            repository.unmarkStudied("lesson_a")
            repository.unmarkStudied("lesson_a")

            assertFalse(repository.isStudied("lesson_a"))
            assertEquals(emptyList(), repository.getStudiedLessons())
        }
    }

    @Test
    fun markingAfterUnmarkRecordsANewTime() = runTest {
        withTestDatabase { database ->
            var epochMillis = 1_000L
            val repository = LocalLessonStudyRepository(
                database = database,
                now = { Instant.fromEpochMilliseconds(epochMillis) },
            )

            repository.markStudied("lesson_a")
            epochMillis = 2_000
            repository.markStudied("lesson_a")
            repository.unmarkStudied("lesson_a")
            epochMillis = 3_000
            repository.markStudied("lesson_a")

            // Unmarking removed the previous claim outright, so the next mark is a new fact
            // rather than a repeat of the one idempotency preserved above.
            assertEquals(
                listOf(StudiedLesson("lesson_a", studiedAtEpochMillis = 3_000)),
                repository.getStudiedLessons(),
            )
        }
    }

    @Test
    fun studiedLessonsAreDistinctAndDeterministicallyOrdered() = runTest {
        withTestDatabase { database ->
            var epochMillis = 1_000L
            val repository = LocalLessonStudyRepository(
                database = database,
                now = { Instant.fromEpochMilliseconds(epochMillis) },
            )

            repository.markStudied("lesson_a")
            epochMillis = 3_000
            repository.markStudied("lesson_c")
            repository.markStudied("lesson_b")

            assertEquals(
                listOf("lesson_b", "lesson_c", "lesson_a"),
                repository.getStudiedLessons().map { it.lessonId },
            )
            assertEquals(3, database.studiedLessonDao().count())
        }
    }

    @Test
    fun concurrentMarksForOneLessonCreateOneRow() = runTest {
        withTestDatabase { database ->
            val repository = repository(database)

            coroutineScope {
                List(20) {
                    async { repository.markStudied("lesson_a") }
                }.awaitAll()
            }

            assertEquals(1, database.studiedLessonDao().count())
        }
    }

    @Test
    fun aRecordForALessonNoLongerInTheBundleIsRetainedReadableAndRemovable() = runTest {
        withTestDatabase { database ->
            // Deliberately no LearningContentRepository here: persistence never asks whether a
            // Lesson currently exists, which is what lets a learner's claim outlive the material.
            val repository = repository(database, epochMillis = 1_000)

            repository.markStudied("lesson_that_no_longer_exists")

            assertTrue(repository.isStudied("lesson_that_no_longer_exists"))
            assertEquals(
                listOf(
                    StudiedLesson("lesson_that_no_longer_exists", studiedAtEpochMillis = 1_000),
                ),
                repository.getStudiedLessons(),
            )

            repository.unmarkStudied("lesson_that_no_longer_exists")

            assertFalse(repository.isStudied("lesson_that_no_longer_exists"))
            assertEquals(emptyList(), repository.getStudiedLessons())
        }
    }

    @Test
    fun koinModuleResolvesLocalRepositoryAgainstSuppliedDatabase() = runTest {
        withTestDatabase { database ->
            val app = koinApplication {
                modules(
                    module { single<CurriculumDatabase> { database } },
                    lessonStudyDataModule,
                )
            }

            try {
                val repository = app.koin.get<LessonStudyRepository>()
                assertIs<LocalLessonStudyRepository>(repository)

                repository.markStudied("lesson_a")

                assertEquals(1, database.studiedLessonDao().count())
            } finally {
                app.close()
            }
        }
    }

    @Test
    fun studiedStateSurvivesRepositoryReconstructionOverTheSameDatabase() = runTest {
        withTestDatabase { database ->
            repository(database, epochMillis = 1_000).markStudied("lesson_a")

            // A second repository holds no state of its own; the database is the record.
            val reconstructedRepository = repository(database, epochMillis = 5_000)

            assertTrue(reconstructedRepository.isStudied("lesson_a"))
            assertEquals(
                listOf(StudiedLesson("lesson_a", studiedAtEpochMillis = 1_000)),
                reconstructedRepository.getStudiedLessons(),
            )
        }
    }

    @Test
    fun studiedStateSurvivesDatabaseAndRepositoryReconstruction() = runTest {
        // A file-backed database, closed and reopened: an in-memory database that is never
        // closed would prove nothing about durability.
        val directory = Files.createTempDirectory("lesson-study-reconstruction-test")
        val databasePath = directory.resolve("curriculum.db")

        try {
            val initialDatabase = openDatabase(databasePath.toString())
            try {
                repository(initialDatabase, epochMillis = 1_000).markStudied("lesson_a")
            } finally {
                initialDatabase.close()
            }

            val reconstructedDatabase = openDatabase(databasePath.toString())
            try {
                assertEquals(
                    listOf(StudiedLesson("lesson_a", studiedAtEpochMillis = 1_000)),
                    repository(reconstructedDatabase).getStudiedLessons(),
                )
            } finally {
                reconstructedDatabase.close()
            }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private fun repository(
        database: CurriculumDatabase,
        epochMillis: Long = 1_000,
    ): LocalLessonStudyRepository =
        LocalLessonStudyRepository(
            database = database,
            now = { Instant.fromEpochMilliseconds(epochMillis) },
        )

    private suspend fun withTestDatabase(
        block: suspend (CurriculumDatabase) -> Unit,
    ) {
        val database = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        try {
            block(database)
        } finally {
            database.close()
        }
    }

    private fun openDatabase(path: String): CurriculumDatabase =
        Room.databaseBuilder<CurriculumDatabase>(name = path)
            .setDriver(BundledSQLiteDriver())
            .build()
}
