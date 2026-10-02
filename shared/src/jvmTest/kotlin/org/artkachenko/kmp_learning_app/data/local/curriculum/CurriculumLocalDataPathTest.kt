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
import org.artkachenko.kmp_learning_app.curriculum.content.BundledCurriculumSource
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
import org.artkachenko.kmp_learning_app.getQuestionById
import kotlin.test.assertNull

internal class CurriculumLocalDataPathTest {
    @Test
    fun realBundledCurriculumInitializesAndCanBeQueriedThroughRepository() = runTest {
        withTestDatabase { database ->
            val initializer = CurriculumDataInitializer(
                importer = CurriculumImporter(database),
            )

            initializer.initialize()

            // Counts and orders are derived from the bundle that was just imported, so this path
            // test does not re-pin the bank's shape — InitialCurriculumSmokeTest owns that. The
            // named first rows are the representative fixture this test reads end to end.
            val authored = BundledCurriculumSource.load()
            val repository: CurriculumRepository = LocalCurriculumRepository(database)
            val activeTopics = repository.getActiveTopics()
            assertEquals(
                authored.topics.filter { it.status == ContentStatus.ACTIVE }.map { it.id },
                activeTopics.map { it.id },
            )
            assertEquals("android_platform", activeTopics.first().id)

            val lifecycleSubtopics = repository.getActiveSubtopics("lifecycle_navigation")
            assertEquals("activity_lifecycle", lifecycleSubtopics.first().id)

            val lifecycleQuestions = repository.getActiveQuestionsByTopic("lifecycle_navigation")
            val activeSubtopicIds = authored.subtopics
                .filter { it.status == ContentStatus.ACTIVE }
                .mapTo(mutableSetOf()) { it.id }
            assertEquals(
                authored.questions
                    .filter {
                        it.topicId == "lifecycle_navigation" &&
                            it.status == ContentStatus.ACTIVE &&
                            it.subtopicId in activeSubtopicIds
                    }
                    .map { it.id },
                lifecycleQuestions.map { it.id },
            )
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
            assertEquals(authored.questions.size, database.curriculumDao().countQuestions())
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
            assertEquals(BundledCurriculumSource.load().authoredRowCounts(), firstCounts)
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

            // Two racing initializers persist one dataset: exactly the rows the bundle authors.
            assertEquals(
                BundledCurriculumSource.load().authoredRowCounts(),
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
                assertIs<LocalCurriculumRepository>(koin.get<LocalCurriculumRepository>())
                // The data module binds the raw repository by its concrete type only. The
                // interface application code reads is bound by `curriculumVisibilityModule`, to
                // the visibility decorator, so this module alone cannot hand out unfiltered
                // eligibility reads.
                assertNull(koin.getOrNull<CurriculumRepository>())
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

    /** The row counts an exact import of this document produces: one row per authored value. */
    private fun Curriculum.authoredRowCounts(): RowCounts =
        RowCounts(
            topics = topics.size,
            subtopics = subtopics.size,
            questions = questions.size,
            answerOptions = questions.sumOf { it.answers.size },
            correctAnswers = questions.sumOf { it.correctAnswerIds.size },
            questionSources = questions.sumOf { it.sources.size },
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
