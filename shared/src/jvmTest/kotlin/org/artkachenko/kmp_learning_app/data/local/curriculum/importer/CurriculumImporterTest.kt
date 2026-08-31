package org.artkachenko.kmp_learning_app.data.local.curriculum.importer

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Curriculum
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.content.BundledCurriculumSource
import org.artkachenko.kmp_learning_app.curriculum.serialization.CurriculumJsonCodec
import org.artkachenko.kmp_learning_app.data.local.assessment.AssessmentAttemptStore
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDao
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.repository.LocalCurriculumRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

internal class CurriculumImporterTest {
    @Test
    fun realBundledCurriculumPopulatesFreshDatabase() = runTest {
        withTestDatabase { database ->
            val expectedCurriculum = BundledCurriculumSource.load()

            val result = CurriculumImporter(database).importCurriculum()

            assertEquals(CurriculumImportResult.Imported, result)
            val dao = database.curriculumDao()
            assertEquals(17, dao.countTopics())
            assertEquals(361, dao.countSubtopics())
            assertEquals(309, dao.countQuestions())

            val singleAnswerQuestion = expectedCurriculum.questions.first {
                it.selectionMode == AnswerSelectionMode.SINGLE
            }
            val multipleAnswerQuestion = expectedCurriculum.questions.first {
                it.selectionMode == AnswerSelectionMode.MULTIPLE
            }

            assertPersistedQuestionMatches(singleAnswerQuestion, dao)
            assertPersistedQuestionMatches(multipleAnswerQuestion, dao)
        }
    }

    @Test
    fun realBundledCurriculumImportIsIdempotent() = runTest {
        withTestDatabase { database ->
            val importer = CurriculumImporter(database)

            assertEquals(CurriculumImportResult.Imported, importer.importCurriculum())
            val firstCounts = database.curriculumDao().countRows()
            val firstQuestion = database.curriculumDao().getQuestionById("activity_lifecycle_001")

            assertEquals(CurriculumImportResult.Imported, importer.importCurriculum())

            assertEquals(firstCounts, database.curriculumDao().countRows())
            assertEquals(firstQuestion, database.curriculumDao().getQuestionById("activity_lifecycle_001"))
        }
    }

    @Test
    fun invalidCurriculumIsRejectedBeforeWritingToFreshDatabase() = runTest {
        withTestDatabase { database ->
            val result = CurriculumImporter(
                database = database,
                loadCurriculum = { validCurriculum("topic_a").withUnknownCorrectAnswer() },
            ).importCurriculum()

            assertIs<CurriculumImportResult.Rejected>(result)
            assertEquals(emptyCounts, database.curriculumDao().countRows())
        }
    }

    @Test
    fun invalidCurriculumDoesNotMutateExistingData() = runTest {
        withTestDatabase { database ->
            val initialCurriculum = validCurriculum("topic_a")
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(database, loadCurriculum = { initialCurriculum }).importCurriculum(),
            )
            val beforeCounts = database.curriculumDao().countRows()
            val beforeQuestion = database.curriculumDao().getQuestionById("topic_a_question")

            val result = CurriculumImporter(
                database = database,
                loadCurriculum = { validCurriculum("topic_b").withUnknownCorrectAnswer() },
            ).importCurriculum()

            assertIs<CurriculumImportResult.Rejected>(result)
            assertEquals(beforeCounts, database.curriculumDao().countRows())
            assertEquals(beforeQuestion, database.curriculumDao().getQuestionById("topic_a_question"))
        }
    }

    @Test
    fun malformedSerializedContentPropagatesAndDoesNotMutateDatabase() = runTest {
        withTestDatabase { database ->
            val initialCurriculum = validCurriculum("topic_a")
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(database, loadCurriculum = { initialCurriculum }).importCurriculum(),
            )
            val beforeCounts = database.curriculumDao().countRows()

            assertFailsWith<SerializationException> {
                CurriculumImporter(
                    database = database,
                    loadCurriculum = {
                        CurriculumJsonCodec.decode("{ malformed json")
                    },
                ).importCurriculum()
            }

            assertEquals(beforeCounts, database.curriculumDao().countRows())
        }
    }

    @Test
    fun stableIdentitiesAreUpdatedWithoutCreatingDuplicates() = runTest {
        withTestDatabase { database ->
            val versionA = curriculumOf(
                graph("topic_a", topicName = "Topic A", subtopicName = "Subtopic A", questionText = "Question A?"),
                graph("topic_b", topicName = "Topic B", subtopicName = "Subtopic B", questionText = "Question B?"),
            )
            val versionB = curriculumOf(
                graph("topic_b", topicName = "Topic B updated", subtopicName = "Subtopic B updated", questionText = "Question B updated?"),
                graph(
                    "topic_a",
                    topicName = "Topic A updated",
                    subtopicName = "Subtopic A updated",
                    questionText = "Question A updated?",
                    explanation = "Updated explanation.",
                    status = ContentStatus.DEPRECATED,
                    answers = listOf(
                        AnswerOption("topic_a_answer_a", "Answer A updated"),
                        AnswerOption("topic_a_answer_b", "Answer B"),
                    ),
                    sources = listOf(
                        SourceReference("Source A updated", "https://example.com/topic_a/source-a"),
                    ),
                ),
            )

            val importerA = CurriculumImporter(database, loadCurriculum = { versionA })
            val importerB = CurriculumImporter(database, loadCurriculum = { versionB })
            assertEquals(CurriculumImportResult.Imported, importerA.importCurriculum())
            val beforeCounts = database.curriculumDao().countRows()

            assertEquals(CurriculumImportResult.Imported, importerB.importCurriculum())

            val dao = database.curriculumDao()
            assertEquals(beforeCounts, dao.countRows())
            assertEquals("Topic A updated", dao.getTopicById("topic_a")?.name)
            assertEquals("DEPRECATED", dao.getTopicById("topic_a")?.status)
            assertEquals(1, dao.getTopicById("topic_a")?.sortOrder)
            assertEquals("Subtopic A updated", dao.getSubtopicById("topic_a_subtopic")?.name)
            assertEquals("Question A updated?", dao.getQuestionById("topic_a_question")?.text)
            assertEquals("Updated explanation.", dao.getQuestionById("topic_a_question")?.explanation)
            assertEquals("Answer A updated", dao.getAnswerOptionsForQuestion("topic_a_question").first().text)
            assertEquals("Source A updated", dao.getSourcesForQuestion("topic_a_question").single().title)
        }
    }

    @Test
    fun newContentIsInsertedWithoutResettingExistingData() = runTest {
        withTestDatabase { database ->
            val versionA = validCurriculum("topic_a")
            val versionB = curriculumOf(
                graph("topic_a"),
                graph("topic_b"),
            )

            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(database, loadCurriculum = { versionA }).importCurriculum(),
            )
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(database, loadCurriculum = { versionB }).importCurriculum(),
            )

            val dao = database.curriculumDao()
            assertEquals(2, dao.countTopics())
            assertEquals(2, dao.countSubtopics())
            assertEquals(2, dao.countQuestions())
            assertEquals("topic_a_question", dao.getQuestionById("topic_a_question")?.id)
            assertEquals("topic_b_question", dao.getQuestionById("topic_b_question")?.id)
        }
    }

    @Test
    fun unrelatedExistingContentIsRetainedDuringImport() = runTest {
        withTestDatabase { database ->
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(database, loadCurriculum = { validCurriculum("legacy") }).importCurriculum(),
            )
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(database, loadCurriculum = { validCurriculum("incoming") }).importCurriculum(),
            )

            assertEquals("legacy_question", database.curriculumDao().getQuestionById("legacy_question")?.id)
            assertEquals("incoming_question", database.curriculumDao().getQuestionById("incoming_question")?.id)
        }
    }

    @Test
    fun deprecatedContentIsUpdatedAndRetained() = runTest {
        withTestDatabase { database ->
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(database, loadCurriculum = { validCurriculum("topic_a") }).importCurriculum(),
            )
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(
                    database,
                    loadCurriculum = {
                        curriculumOf(graph("topic_a", status = ContentStatus.DEPRECATED))
                    },
                ).importCurriculum(),
            )

            val dao = database.curriculumDao()
            assertEquals("DEPRECATED", dao.getTopicById("topic_a")?.status)
            assertEquals("DEPRECATED", dao.getSubtopicById("topic_a_subtopic")?.status)
            assertEquals("DEPRECATED", dao.getQuestionById("topic_a_question")?.status)
            assertEquals(1, dao.countTopics())
            assertEquals(1, dao.countSubtopics())
            assertEquals(1, dao.countQuestions())
        }
    }

    @Test
    fun absenceFromLaterCurriculumIsNotADeletionSignal() = runTest {
        withTestDatabase { database ->
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(
                    database,
                    loadCurriculum = {
                        curriculumOf(
                            graph("topic_a"),
                            graph("topic_b"),
                        )
                    },
                ).importCurriculum(),
            )
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(database, loadCurriculum = { validCurriculum("topic_a") }).importCurriculum(),
            )

            val dao = database.curriculumDao()
            assertEquals(2, dao.countTopics())
            assertEquals(2, dao.countSubtopics())
            assertEquals(2, dao.countQuestions())
            assertEquals("topic_b_question", dao.getQuestionById("topic_b_question")?.id)
            assertEquals("ACTIVE", dao.getQuestionById("topic_b_question")?.status)
        }
    }

    @Test
    fun questionSourcesAreSynchronizedOnlyForIncomingQuestionIds() = runTest {
        withTestDatabase { database ->
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(
                    database,
                    loadCurriculum = {
                        curriculumOf(
                            graph(
                                "topic_a",
                                sources = listOf(
                                    SourceReference("Source A", "https://example.com/source-a"),
                                    SourceReference("Source B", "https://example.com/source-b"),
                                ),
                            ),
                            graph("unrelated"),
                        )
                    },
                ).importCurriculum(),
            )
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(
                    database,
                    loadCurriculum = {
                        curriculumOf(
                            graph(
                                "topic_a",
                                sources = listOf(
                                    SourceReference("Source B updated", "https://example.com/source-b"),
                                    SourceReference("Source C", "https://example.com/source-c"),
                                ),
                            ),
                        )
                    },
                ).importCurriculum(),
            )

            val dao = database.curriculumDao()
            assertEquals(
                listOf("https://example.com/source-b", "https://example.com/source-c"),
                dao.getSourcesForQuestion("topic_a_question").map { it.url },
            )
            assertEquals(
                listOf("https://example.com/unrelated/source-a"),
                dao.getSourcesForQuestion("unrelated_question").map { it.url },
            )
        }
    }

    @Test
    fun correctAnswerRelationsAreSynchronizedOnlyForIncomingQuestionIds() = runTest {
        withTestDatabase { database ->
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(
                    database,
                    loadCurriculum = {
                        curriculumOf(
                            graph(
                                "topic_a",
                                answers = listOf(
                                    AnswerOption("topic_a_answer_a", "Answer A"),
                                    AnswerOption("topic_a_answer_b", "Answer B"),
                                ),
                                selectionMode = AnswerSelectionMode.MULTIPLE,
                                correctAnswerIds = listOf("topic_a_answer_a", "topic_a_answer_b"),
                            ),
                            graph("unrelated"),
                        )
                    },
                ).importCurriculum(),
            )
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(
                    database,
                    loadCurriculum = {
                        curriculumOf(
                            graph(
                                "topic_a",
                                answers = listOf(
                                    AnswerOption("topic_a_answer_a", "Answer A"),
                                    AnswerOption("topic_a_answer_b", "Answer B"),
                                ),
                                correctAnswerIds = listOf("topic_a_answer_b"),
                            ),
                        )
                    },
                ).importCurriculum(),
            )

            val dao = database.curriculumDao()
            assertEquals(listOf("topic_a_answer_b"), dao.getCorrectAnswerIdsForQuestion("topic_a_question"))
            assertEquals(listOf("unrelated_answer_a"), dao.getCorrectAnswerIdsForQuestion("unrelated_question"))
        }
    }

    @Test
    fun authoredSelectionModeSurvivesImportRepositoryReadAndReimport() = runTest {
        withTestDatabase { database ->
            val single = validCurriculum("topic_a")
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(database, loadCurriculum = { single }).importCurriculum(),
            )
            assertEquals(
                AnswerSelectionMode.SINGLE,
                LocalCurriculumRepository(database).getQuestionById("topic_a_question")?.selectionMode,
            )

            val multipleWithOneCorrect = curriculumOf(
                graph(
                    id = "topic_a",
                    selectionMode = AnswerSelectionMode.MULTIPLE,
                    correctAnswerIds = listOf("topic_a_answer_a"),
                ),
            )
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(database, loadCurriculum = { multipleWithOneCorrect }).importCurriculum(),
            )

            val entity = database.curriculumDao().getQuestionById("topic_a_question")
            assertEquals("MULTIPLE", entity?.selectionMode)
            val restored = LocalCurriculumRepository(database).getQuestionById("topic_a_question")
            assertEquals(AnswerSelectionMode.MULTIPLE, restored?.selectionMode)
            assertEquals(listOf("topic_a_answer_a"), restored?.correctAnswerIds)
        }
    }

    @Test
    fun answerOptionsRemovedFromCurriculumAreDeletedForIncomingQuestionsOnly() = runTest {
        withTestDatabase { database ->
            val threeAnswers = listOf(
                AnswerOption("topic_a_answer_a", "topic_a answer A"),
                AnswerOption("topic_a_answer_b", "topic_a answer B"),
                AnswerOption("topic_a_answer_c", "topic_a answer C"),
            )
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(
                    database,
                    loadCurriculum = {
                        curriculumOf(graph("topic_a", answers = threeAnswers), graph("unrelated"))
                    },
                ).importCurriculum(),
            )

            val result = CurriculumImporter(
                database,
                loadCurriculum = {
                    curriculumOf(graph("topic_a", answers = threeAnswers.take(2)))
                },
            ).importCurriculum()

            assertEquals(CurriculumImportResult.Imported, result)
            val dao = database.curriculumDao()
            assertEquals(
                listOf("topic_a_answer_a", "topic_a_answer_b"),
                dao.getAnswerOptionsForQuestion("topic_a_question").map { it.id },
            )
            assertEquals(
                listOf("unrelated_answer_a", "unrelated_answer_b"),
                dao.getAnswerOptionsForQuestion("unrelated_question").map { it.id },
            )
        }
    }

    @Test
    fun answerOptionThatWasPreviouslyCorrectCanBeRemoved() = runTest {
        withTestDatabase { database ->
            val originalAnswers = listOf(
                AnswerOption("topic_a_answer_a", "topic_a answer A"),
                AnswerOption("topic_a_answer_b", "topic_a answer B"),
            )
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(
                    database,
                    loadCurriculum = {
                        curriculumOf(
                            graph(
                                "topic_a",
                                answers = originalAnswers,
                                correctAnswerIds = listOf("topic_a_answer_a"),
                            ),
                        )
                    },
                ).importCurriculum(),
            )

            // topic_a_answer_a is dropped while it is still the persisted correct answer.
            // question_correct_answer has a foreign key onto answer_option, so this fails
            // unless stale options are deleted after correct answers are replaced.
            val result = CurriculumImporter(
                database,
                loadCurriculum = {
                    curriculumOf(
                        graph(
                            "topic_a",
                            answers = listOf(
                                AnswerOption("topic_a_answer_b", "topic_a answer B"),
                                AnswerOption("topic_a_answer_c", "topic_a answer C"),
                            ),
                            correctAnswerIds = listOf("topic_a_answer_b"),
                        ),
                    )
                },
            ).importCurriculum()

            assertEquals(CurriculumImportResult.Imported, result)
            val dao = database.curriculumDao()
            assertEquals(
                listOf("topic_a_answer_b", "topic_a_answer_c"),
                dao.getAnswerOptionsForQuestion("topic_a_question").map { it.id },
            )
            assertEquals(
                listOf("topic_a_answer_b"),
                dao.getCorrectAnswerIdsForQuestion("topic_a_question"),
            )
        }
    }

    @Test
    fun answerOptionSelectedByHistoricalAttemptIsRetainedOnReimport() = runTest {
        withTestDatabase { database ->
            val threeAnswers = listOf(
                AnswerOption("topic_a_answer_a", "topic_a answer A"),
                AnswerOption("topic_a_answer_b", "topic_a answer B"),
                AnswerOption("topic_a_answer_c", "topic_a answer C"),
            )
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(
                    database,
                    loadCurriculum = { curriculumOf(graph("topic_a", answers = threeAnswers)) },
                ).importCurriculum(),
            )
            AssessmentAttemptStore(database).save(
                TestAttempt(
                    id = "historical_attempt",
                    config = AssessmentConfig.Focused(AssessmentScope.Topic("topic_a"), 1),
                    questionAttempts = listOf(
                        QuestionAttempt(
                            questionId = "topic_a_question",
                            answerState = QuestionAnswerState.Answered(
                                selectedAnswerIds = setOf("topic_a_answer_c"),
                                isCorrect = false,
                            ),
                        ),
                    ),
                    status = AssessmentStatus.COMPLETED,
                    startedAt = Instant.fromEpochMilliseconds(1),
                    completedAt = Instant.fromEpochMilliseconds(2),
                    score = AssessmentScore(totalQuestions = 1, correctAnswers = 0),
                ),
            )

            val result = CurriculumImporter(
                database,
                loadCurriculum = {
                    curriculumOf(graph("topic_a", answers = threeAnswers.take(2)))
                },
            ).importCurriculum()

            assertEquals(CurriculumImportResult.Imported, result)
            assertEquals(
                listOf("topic_a_answer_a", "topic_a_answer_b", "topic_a_answer_c"),
                database.curriculumDao()
                    .getAnswerOptionsForQuestion("topic_a_question")
                    .map { it.id },
            )
            assertEquals(
                ContentStatus.DEPRECATED.name,
                database.curriculumDao()
                    .getAnswerOptionsForQuestion("topic_a_question")
                    .single { it.id == "topic_a_answer_c" }
                    .status,
            )
        }
    }

    @Test
    fun retiredAnswerOptionLeavesActiveQuestionsButStaysReviewable() = runTest {
        withTestDatabase { database ->
            // Reproduces the upgrade path: a user answered with an option that a later
            // bundle renames. The row survives for historical review, so it must not come
            // back as an extra choice in new assessments.
            val originalAnswers = listOf(
                AnswerOption("topic_a_answer_a", "topic_a answer A"),
                AnswerOption("topic_a_answer_b", "topic_a answer B"),
                AnswerOption("topic_a_answer_c", "topic_a implausible filler"),
            )
            CurriculumImporter(
                database,
                loadCurriculum = { curriculumOf(graph("topic_a", answers = originalAnswers)) },
            ).importCurriculum()
            AssessmentAttemptStore(database).save(
                TestAttempt(
                    id = "historical_attempt",
                    config = AssessmentConfig.Focused(AssessmentScope.Topic("topic_a"), 1),
                    questionAttempts = listOf(
                        QuestionAttempt(
                            questionId = "topic_a_question",
                            answerState = QuestionAnswerState.Answered(
                                selectedAnswerIds = setOf("topic_a_answer_c"),
                                isCorrect = false,
                            ),
                        ),
                    ),
                    status = AssessmentStatus.COMPLETED,
                    startedAt = Instant.fromEpochMilliseconds(1),
                    completedAt = Instant.fromEpochMilliseconds(2),
                    score = AssessmentScore(totalQuestions = 1, correctAnswers = 0),
                ),
            )

            // The filler is replaced by a differently-identified distractor.
            val renamedAnswers = listOf(
                AnswerOption("topic_a_answer_a", "topic_a answer A"),
                AnswerOption("topic_a_answer_b", "topic_a answer B"),
                AnswerOption("topic_a_answer_e", "topic_a plausible distractor"),
            )
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(
                    database,
                    loadCurriculum = { curriculumOf(graph("topic_a", answers = renamedAnswers)) },
                ).importCurriculum(),
            )

            val repository = LocalCurriculumRepository(database)
            assertEquals(
                listOf("topic_a_answer_a", "topic_a_answer_b", "topic_a_answer_e"),
                repository.getActiveQuestionsByTopic("topic_a").single().answers.map { it.id },
            )
            assertEquals(
                listOf("topic_a_answer_a", "topic_a_answer_b", "topic_a_answer_c", "topic_a_answer_e"),
                repository.getQuestionById("topic_a_question")?.answers?.map { it.id }?.sorted(),
            )
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

    private suspend fun assertPersistedQuestionMatches(
        question: Question,
        dao: CurriculumDao,
    ) {
        assertEquals(question.text, dao.getQuestionById(question.id)?.text)
        assertEquals(question.selectionMode.name, dao.getQuestionById(question.id)?.selectionMode)
        assertEquals(question.status.name, dao.getQuestionById(question.id)?.status)
        assertEquals(question.answers.map { it.id }, dao.getAnswerOptionsForQuestion(question.id).map { it.id })
        assertEquals(question.correctAnswerIds.sorted(), dao.getCorrectAnswerIdsForQuestion(question.id))
        assertEquals(question.sources.map { it.url }, dao.getSourcesForQuestion(question.id).map { it.url })
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

    private fun validCurriculum(prefix: String): Curriculum =
        curriculumOf(graph(prefix))

    private fun Curriculum.withUnknownCorrectAnswer(): Curriculum =
        copy(
            questions = questions.mapIndexed { index, question ->
                if (index == 0) {
                    question.copy(correctAnswerIds = listOf("missing_answer"))
                } else {
                    question
                }
            },
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
        subtopicName: String = "$id subtopic",
        questionText: String = "$id question?",
        explanation: String = "$id explanation.",
        status: ContentStatus = ContentStatus.ACTIVE,
        answers: List<AnswerOption> = listOf(
            AnswerOption("${id}_answer_a", "$id answer A"),
            AnswerOption("${id}_answer_b", "$id answer B"),
        ),
        selectionMode: AnswerSelectionMode = AnswerSelectionMode.SINGLE,
        correctAnswerIds: List<String> = listOf("${id}_answer_a"),
        sources: List<SourceReference> = listOf(
            SourceReference("$id source A", "https://example.com/$id/source-a"),
        ),
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
                name = subtopicName,
                status = status,
            ),
            question = Question(
                id = "${id}_question",
                topicId = id,
                subtopicId = "${id}_subtopic",
                text = questionText,
                answers = answers,
                selectionMode = selectionMode,
                correctAnswerIds = correctAnswerIds,
                explanation = explanation,
                sources = sources,
                status = status,
            ),
        )

    private data class CurriculumGraph(
        val topic: Topic,
        val subtopic: Subtopic,
        val question: Question,
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
