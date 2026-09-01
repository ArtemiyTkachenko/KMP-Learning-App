package org.artkachenko.kmp_learning_app.data.local.curriculum

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Curriculum
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.curriculum.serialization.CurriculumJsonCodec
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import org.artkachenko.kmp_learning_app.data.local.curriculum.repository.LocalCurriculumRepository
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull

internal class CurriculumLocalDataPathTest {
    @Test
    fun realBundledCurriculumInitializesAndCanBeQueriedThroughRepository() = runTest {
        withTestDatabase { database ->
            val initializer = CurriculumDataInitializer(
                importer = CurriculumImporter(database),
            )

            initializer.initialize()

            val repository: CurriculumRepository = LocalCurriculumRepository(database)
            val activeTopics = repository.getActiveTopics()
            assertEquals(17, activeTopics.size)
            assertEquals("android_platform", activeTopics.first().id)

            val lifecycleSubtopics = repository.getActiveSubtopics("lifecycle_navigation")
            assertEquals("activity_lifecycle", lifecycleSubtopics.first().id)

            val lifecycleQuestions = repository.getActiveQuestionsByTopic("lifecycle_navigation")
            assertEquals(23, lifecycleQuestions.size)
            assertEquals("activity_lifecycle_001", lifecycleQuestions.first().id)

            val question = repository.getQuestionById("activity_lifecycle_001")
            assertNotNull(question)
            assertEquals(ContentStatus.ACTIVE, question.status)
            assertEquals("activity_lifecycle_001", question.id)
            assertEquals("lifecycle_navigation", question.topicId)
            assertEquals("activity_lifecycle", question.subtopicId)
            assertEquals(4, question.answers.size)
            assertEquals(listOf("activity_lifecycle_001_b"), question.correctAnswerIds)
            assertEquals(1, question.sources.size)
            assertEquals("The Activity Lifecycle", question.sources.first().title)
            assertEquals(399, database.curriculumDao().countQuestions())
        }
    }

    @Test
    fun repeatedInitializationIsSafeAndDoesNotDuplicateRows() = runTest {
        withTestDatabase { database ->
            val initializer = CurriculumDataInitializer(
                importer = CurriculumImporter(database),
            )

            initializer.initialize()
            val firstCounts = database.curriculumDao().countRows()

            initializer.initialize()

            assertEquals(firstCounts, database.curriculumDao().countRows())
            assertEquals(17, LocalCurriculumRepository(database).getActiveTopics().size)
        }
    }

    @Test
    fun concurrentInitializationRunsSafelyAndPersistsOneDataset() = runTest {
        withTestDatabase { database ->
            val initializer = CurriculumDataInitializer(
                importer = CurriculumImporter(database),
            )

            coroutineScope {
                awaitAll(
                    async { initializer.initialize() },
                    async { initializer.initialize() },
                )
            }

            assertEquals(
                RowCounts(
                    topics = 17,
                    subtopics = 361,
                    questions = 399,
                    answerOptions = 1_602,
                    correctAnswers = 448,
                    questionSources = 454,
                ),
                database.curriculumDao().countRows(),
            )
        }
    }

    @Test
    fun deprecatedContentIsImportedButExcludedFromActiveQueries() = runTest {
        withTestDatabase { database ->
            val initializer = CurriculumDataInitializer(
                importer = CurriculumImporter(
                    database = database,
                    loadCurriculum = {
                        Curriculum(
                            topics = listOf(Topic("topic", "Topic")),
                            subtopics = listOf(Subtopic("subtopic", "topic", "Subtopic")),
                            questions = listOf(
                                question("active_question", ContentStatus.ACTIVE),
                                question("deprecated_question", ContentStatus.DEPRECATED),
                            ),
                        )
                    },
                ),
            )

            initializer.initialize()

            val repository = LocalCurriculumRepository(database)
            assertEquals(listOf("active_question"), repository.getActiveQuestionsByTopic("topic").map { it.id })

            val deprecatedQuestion = repository.getQuestionById("deprecated_question")
            assertNotNull(deprecatedQuestion)
            assertEquals(ContentStatus.DEPRECATED, deprecatedQuestion.status)
        }
    }

    @Test
    fun malformedSerializedContentFailsInitializationWithoutPersistingRows() = runTest {
        withTestDatabase { database ->
            val initializer = CurriculumDataInitializer(
                importer = CurriculumImporter(
                    database = database,
                    loadCurriculum = {
                        CurriculumJsonCodec.decode("{ malformed json")
                    },
                ),
            )

            assertFailsWith<SerializationException> {
                initializer.initialize()
            }

            assertEquals(emptyCounts, database.curriculumDao().countRows())
        }
    }

    @Test
    fun semanticallyInvalidContentFailsInitializationWithoutPersistingRows() = runTest {
        withTestDatabase { database ->
            val initializer = CurriculumDataInitializer(
                importer = CurriculumImporter(
                    database = database,
                    loadCurriculum = {
                        Curriculum(
                            topics = listOf(Topic("topic", "Topic")),
                            subtopics = listOf(Subtopic("subtopic", "topic", "Subtopic")),
                            questions = listOf(
                                question(
                                    id = "invalid_question",
                                    status = ContentStatus.ACTIVE,
                                    correctAnswerIds = listOf("missing_answer"),
                                ),
                            ),
                        )
                    },
                ),
            )

            assertFailsWith<IllegalStateException> {
                initializer.initialize()
            }

            assertEquals(emptyCounts, database.curriculumDao().countRows())
        }
    }

    @Test
    fun koinGraphResolvesLocalDataDependenciesWhenDatabaseIsSupplied() = runTest {
        withTestDatabase { database ->
            val app = koinApplication {
                modules(
                    module {
                        single<CurriculumDatabase> { database }
                    },
                    curriculumDataModule,
                )
            }

            try {
                val koin = app.koin

                assertIs<CurriculumImporter>(koin.get<CurriculumImporter>())
                assertIs<CurriculumDataInitializer>(koin.get<CurriculumDataInitializer>())
                assertIs<LocalCurriculumRepository>(koin.get<CurriculumRepository>())
            } finally {
                app.close()
            }
        }
    }

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

    private suspend fun CurriculumDao.countRows(): RowCounts =
        RowCounts(
            topics = countTopics(),
            subtopics = countSubtopics(),
            questions = countQuestions(),
            answerOptions = countAnswerOptions(),
            correctAnswers = countCorrectAnswers(),
            questionSources = countQuestionSources(),
        )

    private fun question(
        id: String,
        status: ContentStatus,
        correctAnswerIds: List<String> = listOf("${id}_answer_a"),
    ): Question =
        Question(
            id = id,
            topicId = "topic",
            subtopicId = "subtopic",
            text = "$id?",
            answers = listOf(
                AnswerOption("${id}_answer_a", "Answer A"),
                AnswerOption("${id}_answer_b", "Answer B"),
            ),
            selectionMode = AnswerSelectionMode.SINGLE,
            level = QuestionLevel.FOUNDATION,
            correctAnswerIds = correctAnswerIds,
            explanation = "$id explanation.",
            sources = listOf(
                SourceReference("$id source", "https://example.com/${id.replace('_', '-')}/source"),
            ),
            status = status,
        )

    private data class RowCounts(
        val topics: Int,
        val subtopics: Int,
        val questions: Int,
        val answerOptions: Int,
        val correctAnswers: Int,
        val questionSources: Int,
    )

    private companion object {
        val emptyCounts = RowCounts(
            topics = 0,
            subtopics = 0,
            questions = 0,
            answerOptions = 0,
            correctAnswers = 0,
            questionSources = 0,
        )
    }
}
