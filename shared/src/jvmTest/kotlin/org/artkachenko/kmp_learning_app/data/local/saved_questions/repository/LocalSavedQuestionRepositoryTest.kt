package org.artkachenko.kmp_learning_app.data.local.saved_questions.repository

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.AnswerOptionEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.QuestionCorrectAnswerEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.QuestionEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.SubtopicEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.TopicEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.repository.LocalCurriculumRepository
import org.artkachenko.kmp_learning_app.data.local.saved_questions.savedQuestionDataModule
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestion
import org.artkachenko.kmp_learning_app.saved_questions.repository.SavedQuestionRepository
import org.koin.dsl.koinApplication
import org.koin.dsl.module

internal class LocalSavedQuestionRepositoryTest {
    @Test
    fun savePersistsIdentityAndReportsIndividualState() = runTest {
        withTestDatabase { database ->
            val repository = repository(database, epochMillis = 1_000)

            assertFalse(repository.isSaved("question"))

            repository.save("question")

            assertTrue(repository.isSaved("question"))
            assertFalse(repository.isSaved("other_question"))
            assertEquals(
                listOf(SavedQuestion("question", savedAtEpochMillis = 1_000)),
                repository.getSavedQuestions(),
            )
            assertEquals(1, database.savedQuestionDao().count())
        }
    }

    @Test
    fun repeatedSaveIsANoOpAndPreservesOriginalTimestamp() = runTest {
        withTestDatabase { database ->
            var epochMillis = 1_000L
            val repository = LocalSavedQuestionRepository(
                database = database,
                now = { Instant.fromEpochMilliseconds(epochMillis) },
            )

            repository.save("question")
            epochMillis = 2_000
            repository.save("question")

            assertEquals(
                listOf(SavedQuestion("question", savedAtEpochMillis = 1_000)),
                repository.getSavedQuestions(),
            )
            assertEquals(1, database.savedQuestionDao().count())
        }
    }

    @Test
    fun unsaveRemovesIdentityAndRepeatedUnsaveIsSafe() = runTest {
        withTestDatabase { database ->
            val repository = repository(database)
            repository.save("question")

            repository.unsave("question")
            repository.unsave("question")

            assertFalse(repository.isSaved("question"))
            assertEquals(emptyList(), repository.getSavedQuestions())
        }
    }

    @Test
    fun savingAfterUnsaveCreatesANewTimestamp() = runTest {
        withTestDatabase { database ->
            var epochMillis = 1_000L
            val repository = LocalSavedQuestionRepository(
                database = database,
                now = { Instant.fromEpochMilliseconds(epochMillis) },
            )

            repository.save("question")
            repository.unsave("question")
            epochMillis = 2_000
            repository.save("question")

            assertEquals(
                listOf(SavedQuestion("question", savedAtEpochMillis = 2_000)),
                repository.getSavedQuestions(),
            )
        }
    }

    @Test
    fun savedQuestionsAreDistinctAndDeterministicallyOrdered() = runTest {
        withTestDatabase { database ->
            var epochMillis = 1_000L
            val repository = LocalSavedQuestionRepository(
                database = database,
                now = { Instant.fromEpochMilliseconds(epochMillis) },
            )

            repository.save("question_a")
            epochMillis = 3_000
            repository.save("question_c")
            repository.save("question_b")

            assertEquals(
                listOf("question_b", "question_c", "question_a"),
                repository.getSavedQuestions().map { it.questionId },
            )
            assertEquals(3, database.savedQuestionDao().count())
        }
    }

    @Test
    fun concurrentSavesForOneIdentityCreateOneRow() = runTest {
        withTestDatabase { database ->
            val repository = repository(database)

            coroutineScope {
                List(20) {
                    async { repository.save("question") }
                }.awaitAll()
            }

            assertEquals(1, database.savedQuestionDao().count())
        }
    }

    @Test
    fun savedIdentityResolvesActiveAndDeprecatedContentAndRetainsMissingContent() = runTest {
        withTestDatabase { database ->
            insertLifecycleCurriculum(database)
            val savedRepository = repository(database)
            val curriculumRepository: CurriculumRepository = LocalCurriculumRepository(database)

            savedRepository.save("active_question")
            savedRepository.save("deprecated_question")
            savedRepository.save("missing_question")

            assertEquals(
                setOf("active_question", "deprecated_question", "missing_question"),
                savedRepository.getSavedQuestions().map { it.questionId }.toSet(),
            )
            assertEquals(
                ContentStatus.ACTIVE,
                assertNotNull(curriculumRepository.getQuestionById("active_question")).status,
            )
            assertEquals(
                ContentStatus.DEPRECATED,
                assertNotNull(curriculumRepository.getQuestionById("deprecated_question")).status,
            )
            assertNull(curriculumRepository.getQuestionById("missing_question"))
            assertTrue(savedRepository.isSaved("missing_question"))
        }
    }

    @Test
    fun koinModuleResolvesLocalRepositoryAgainstSuppliedDatabase() = runTest {
        withTestDatabase { database ->
            val app = koinApplication {
                modules(
                    module { single<CurriculumDatabase> { database } },
                    savedQuestionDataModule,
                )
            }

            try {
                val repository = app.koin.get<SavedQuestionRepository>()
                assertIs<LocalSavedQuestionRepository>(repository)

                repository.save("question")

                assertEquals(1, database.savedQuestionDao().count())
            } finally {
                app.close()
            }
        }
    }

    @Test
    fun savedIdentitySurvivesDatabaseAndRepositoryReconstruction() = runTest {
        val directory = Files.createTempDirectory("saved-question-reconstruction-test")
        val databasePath = directory.resolve("curriculum.db")

        try {
            val initialDatabase = openDatabase(databasePath.toString())
            try {
                repository(initialDatabase, epochMillis = 1_000).save("question")
            } finally {
                initialDatabase.close()
            }

            val reconstructedDatabase = openDatabase(databasePath.toString())
            try {
                assertEquals(
                    listOf(SavedQuestion("question", savedAtEpochMillis = 1_000)),
                    repository(reconstructedDatabase).getSavedQuestions(),
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
    ): LocalSavedQuestionRepository =
        LocalSavedQuestionRepository(
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

    private suspend fun insertLifecycleCurriculum(database: CurriculumDatabase) {
        val dao = database.curriculumDao()
        dao.upsertTopics(listOf(TopicEntity("topic", "Topic", "ACTIVE", sortOrder = 0)))
        dao.upsertSubtopics(
            listOf(SubtopicEntity("subtopic", "topic", "Subtopic", "ACTIVE", sortOrder = 0)),
        )
        dao.upsertQuestions(
            listOf(
                QuestionEntity("active_question", "topic", "subtopic", "Active?", "SINGLE", "FOUNDATION", "Active explanation.", "ACTIVE", sortOrder = 0),
                QuestionEntity("deprecated_question", "topic", "subtopic", "Deprecated?", "SINGLE", "FOUNDATION", "Deprecated explanation.", "DEPRECATED", sortOrder = 1),
            ),
        )
        dao.upsertAnswerOptions(
            listOf(
                AnswerOptionEntity("active_question", "active_answer", "Active answer", sortOrder = 0),
                AnswerOptionEntity("deprecated_question", "deprecated_answer", "Deprecated answer", sortOrder = 0),
            ),
        )
        dao.upsertCorrectAnswers(
            listOf(
                QuestionCorrectAnswerEntity("active_question", "active_answer"),
                QuestionCorrectAnswerEntity("deprecated_question", "deprecated_answer"),
            ),
        )
    }
}
