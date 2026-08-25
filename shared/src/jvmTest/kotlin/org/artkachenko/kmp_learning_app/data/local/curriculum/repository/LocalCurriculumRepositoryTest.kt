package org.artkachenko.kmp_learning_app.data.local.curriculum.repository

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Curriculum
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.content.BundledCurriculumSource
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImportResult
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class LocalCurriculumRepositoryTest {
    @Test
    fun getActiveTopicsReturnsOnlyActiveTopicsInSortOrder() = runTest {
        withRepository(
            curriculumOf(
                graph("topic_c", topicName = "Topic C"),
                graph("topic_b", topicName = "Topic B", status = ContentStatus.DEPRECATED),
                graph("topic_a", topicName = "Topic A"),
            ),
        ) { repository ->
            assertEquals(
                listOf("topic_c", "topic_a"),
                repository.getActiveTopics().map { it.id },
            )
        }
    }

    @Test
    fun getActiveSubtopicsRequiresActiveSubtopicAndActiveParentTopic() = runTest {
        withRepository(activeSubtopicFixture()) { repository ->
            assertEquals(
                listOf("active_topic_sub_b", "active_topic_sub_a"),
                repository.getActiveSubtopics("active_topic").map { it.id },
            )
            assertEquals(emptyList(), repository.getActiveSubtopics("deprecated_topic"))
        }
    }

    @Test
    fun getActiveQuestionsByTopicRequiresActiveQuestionSubtopicAndTopic() = runTest {
        withRepository(activeQuestionFixture()) { repository ->
            val questions = repository.getActiveQuestionsByTopic("active_topic")

            assertEquals(listOf("question_z", "question_a"), questions.map { it.id })
            assertCompleteQuestion(
                question = questions.first(),
                expectedAnswerIds = listOf("question_z_answer_b", "question_z_answer_a"),
                expectedCorrectAnswerIds = listOf("question_z_answer_a", "question_z_answer_b"),
                expectedSourceUrls = listOf("https://example.com/question-z/source-b", "https://example.com/question-z/source-a"),
            )
            assertEquals(emptyList(), repository.getActiveQuestionsByTopic("deprecated_topic"))
        }
    }

    @Test
    fun getActiveQuestionsBySubtopicRequiresActiveQuestionSubtopicAndTopic() = runTest {
        withRepository(activeQuestionFixture()) { repository ->
            assertEquals(
                listOf("question_z", "question_a"),
                repository.getActiveQuestionsBySubtopic("active_subtopic").map { it.id },
            )
            assertEquals(emptyList(), repository.getActiveQuestionsBySubtopic("deprecated_subtopic"))
            assertEquals(emptyList(), repository.getActiveQuestionsBySubtopic("deprecated_parent_subtopic"))
        }
    }

    @Test
    fun getQuestionByIdReturnsDeprecatedQuestionForHistoricalLookup() = runTest {
        val deprecatedQuestion = question(
            id = "deprecated_question",
            topicId = "deprecated_topic",
            subtopicId = "deprecated_subtopic",
            status = ContentStatus.DEPRECATED,
            correctAnswerIds = listOf("deprecated_question_answer_a", "deprecated_question_answer_b"),
        )
        withRepository(
            Curriculum(
                topics = listOf(
                    Topic("deprecated_topic", "Deprecated topic", ContentStatus.DEPRECATED),
                ),
                subtopics = listOf(
                    Subtopic("deprecated_subtopic", "deprecated_topic", "Deprecated subtopic", ContentStatus.DEPRECATED),
                ),
                questions = listOf(deprecatedQuestion),
            ),
        ) { repository ->
            val question = repository.getQuestionById("deprecated_question")

            assertNotNull(question)
            assertEquals(ContentStatus.DEPRECATED, question.status)
            assertEquals(listOf("deprecated_question_answer_b", "deprecated_question_answer_a"), question.answers.map { it.id })
            assertEquals(listOf("deprecated_question_answer_a", "deprecated_question_answer_b"), question.correctAnswerIds)
            assertEquals(listOf("https://example.com/deprecated-question/source-b", "https://example.com/deprecated-question/source-a"), question.sources.map { it.url })
        }
    }

    @Test
    fun getQuestionByIdReturnsNullForUnknownQuestion() = runTest {
        withRepository(curriculumOf(graph("topic_a"))) { repository ->
            assertNull(repository.getQuestionById("missing_question"))
        }
    }

    @Test
    fun realBundledCurriculumCanBeReadThroughRepositoryAfterImport() = runTest {
        withTestDatabase { database ->
            val expectedCurriculum = BundledCurriculumSource.load()
            assertEquals(CurriculumImportResult.Imported, CurriculumImporter(database).importCurriculum())

            val repository = LocalCurriculumRepository(database)
            assertEquals(
                expectedCurriculum.topics.filter { it.status == ContentStatus.ACTIVE }.map { it.id },
                repository.getActiveTopics().map { it.id },
            )

            val knownQuestion = repository.getQuestionById("activity_lifecycle_001")
            assertNotNull(knownQuestion)
            assertEquals("activity_lifecycle_001", knownQuestion.id)
            assertEquals(expectedCurriculum.questions.first { it.id == "activity_lifecycle_001" }.answers.map { it.id }, knownQuestion.answers.map { it.id })
            assertEquals(expectedCurriculum.questions.first { it.id == "activity_lifecycle_001" }.sources.map { it.url }, knownQuestion.sources.map { it.url })
        }
    }

    private suspend fun withRepository(
        curriculum: Curriculum,
        block: suspend (CurriculumRepository) -> Unit,
    ) {
        withTestDatabase { database ->
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(
                    database = database,
                    loadCurriculum = { curriculum },
                ).importCurriculum(),
            )
            block(LocalCurriculumRepository(database))
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

    private fun activeSubtopicFixture(): Curriculum =
        Curriculum(
            topics = listOf(
                Topic("active_topic", "Active topic"),
                Topic("deprecated_topic", "Deprecated topic", ContentStatus.DEPRECATED),
                Topic("other_topic", "Other topic"),
            ),
            subtopics = listOf(
                Subtopic("other_subtopic", "other_topic", "Other subtopic"),
                Subtopic("active_topic_sub_b", "active_topic", "Active subtopic B"),
                Subtopic("deprecated_active_subtopic", "deprecated_topic", "Deprecated topic child"),
                Subtopic("active_topic_deprecated_sub", "active_topic", "Deprecated subtopic", ContentStatus.DEPRECATED),
                Subtopic("active_topic_sub_a", "active_topic", "Active subtopic A"),
            ),
            questions = listOf(
                question("other_question", "other_topic", "other_subtopic"),
                question("included_question_b", "active_topic", "active_topic_sub_b"),
                question("deprecated_parent_question", "deprecated_topic", "deprecated_active_subtopic"),
                question("deprecated_subtopic_question", "active_topic", "active_topic_deprecated_sub"),
                question("included_question_a", "active_topic", "active_topic_sub_a"),
            ),
        )

    private fun activeQuestionFixture(): Curriculum =
        Curriculum(
            topics = listOf(
                Topic("active_topic", "Active topic"),
                Topic("deprecated_topic", "Deprecated topic", ContentStatus.DEPRECATED),
                Topic("other_topic", "Other topic"),
            ),
            subtopics = listOf(
                Subtopic("active_subtopic", "active_topic", "Active subtopic"),
                Subtopic("deprecated_subtopic", "active_topic", "Deprecated subtopic", ContentStatus.DEPRECATED),
                Subtopic("deprecated_parent_subtopic", "deprecated_topic", "Deprecated parent subtopic"),
                Subtopic("other_subtopic", "other_topic", "Other subtopic"),
            ),
            questions = listOf(
                question(
                    id = "question_z",
                    topicId = "active_topic",
                    subtopicId = "active_subtopic",
                    answers = listOf(
                        AnswerOption("question_z_answer_b", "Answer B"),
                        AnswerOption("question_z_answer_a", "Answer A"),
                    ),
                    correctAnswerIds = listOf("question_z_answer_a", "question_z_answer_b"),
                    sources = listOf(
                        SourceReference("Source B", "https://example.com/question-z/source-b"),
                        SourceReference("Source A", "https://example.com/question-z/source-a"),
                    ),
                ),
                question("deprecated_question", "active_topic", "active_subtopic", ContentStatus.DEPRECATED),
                question("deprecated_subtopic_question", "active_topic", "deprecated_subtopic"),
                question("deprecated_parent_question", "deprecated_topic", "deprecated_parent_subtopic"),
                question("other_question", "other_topic", "other_subtopic"),
                question("question_a", "active_topic", "active_subtopic"),
            ),
        )

    private fun curriculumOf(vararg graphs: CurriculumGraph): Curriculum =
        Curriculum(
            topics = graphs.map { it.topic },
            subtopics = graphs.map { it.subtopic },
            questions = graphs.map { it.question },
        )

    private fun graph(
        id: String,
        topicName: String = "$id topic",
        status: ContentStatus = ContentStatus.ACTIVE,
    ): CurriculumGraph =
        CurriculumGraph(
            topic = Topic(
                id = id,
                name = topicName,
                status = status,
            ),
            subtopic = Subtopic(
                id = "${id}_subtopic",
                topicId = id,
                name = "$id subtopic",
                status = status,
            ),
            question = question(
                id = "${id}_question",
                topicId = id,
                subtopicId = "${id}_subtopic",
                status = status,
            ),
        )

    private fun question(
        id: String,
        topicId: String,
        subtopicId: String,
        status: ContentStatus = ContentStatus.ACTIVE,
        answers: List<AnswerOption> = listOf(
            AnswerOption("${id}_answer_b", "Answer B"),
            AnswerOption("${id}_answer_a", "Answer A"),
        ),
        correctAnswerIds: List<String> = listOf("${id}_answer_a"),
        sources: List<SourceReference> = listOf(
            SourceReference("Source B", "https://example.com/${id.dashCase()}/source-b"),
            SourceReference("Source A", "https://example.com/${id.dashCase()}/source-a"),
        ),
    ): Question =
        Question(
            id = id,
            topicId = topicId,
            subtopicId = subtopicId,
            text = "$id?",
            answers = answers,
            correctAnswerIds = correctAnswerIds,
            explanation = "$id explanation.",
            sources = sources,
            status = status,
        )

    private fun assertCompleteQuestion(
        question: Question,
        expectedAnswerIds: List<String>,
        expectedCorrectAnswerIds: List<String>,
        expectedSourceUrls: List<String>,
    ) {
        assertEquals(expectedAnswerIds, question.answers.map { it.id })
        assertEquals(expectedCorrectAnswerIds, question.correctAnswerIds)
        assertEquals(expectedSourceUrls, question.sources.map { it.url })
    }

    private fun String.dashCase(): String =
        replace('_', '-')

    private data class CurriculumGraph(
        val topic: Topic,
        val subtopic: Subtopic,
        val question: Question,
    )
}
