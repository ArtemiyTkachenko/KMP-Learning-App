package org.artkachenko.kmp_learning_app.assessment

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeResult
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeService
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentStartResult
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Curriculum
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.data.local.assessment.AssessmentAttemptStore
import org.artkachenko.kmp_learning_app.data.local.assessment.assessmentDataModule
import org.artkachenko.kmp_learning_app.data.local.assessment.repository.LocalAssessmentRepository
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDataInitializer
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.curriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImportResult
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import org.artkachenko.kmp_learning_app.data.local.curriculum.repository.LocalCurriculumRepository
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.koin.dsl.koinApplication
import org.koin.dsl.module

internal class AssessmentEngineIntegrationTest {
    @Test
    fun realBundledFocusedAssessmentStartsFromPersistedActiveCurriculum() = runTest {
        withTestDatabase { database ->
            assertEquals(CurriculumImportResult.Imported, CurriculumImporter(database).importCurriculum())

            val curriculumRepository = LocalCurriculumRepository(database)
            val topicId = firstTopicWithQuestions(curriculumRepository)
            val engine = assessmentEngine(
                curriculumRepository = curriculumRepository,
                generateAttemptId = { "attempt_real_focused" },
            )

            val result = engine.start(
                AssessmentConfig.Focused(
                    scope = AssessmentScope.Topic(topicId),
                    questionCount = 5,
                ),
            )

            val session = assertIs<AssessmentStartResult.Started>(result).session
            assertTrue(session.questions.isNotEmpty())
            assertTrue(session.questions.size <= 5)
            assertEquals(session.questions.size, session.questions.map { it.id }.toSet().size)
            assertEquals(setOf(topicId), session.questions.map { it.topicId }.toSet())
            assertEquals(AssessmentStatus.IN_PROGRESS, session.attempt.status)
            assertTrue(session.attempt.questionAttempts.all { it.answerState == QuestionAnswerState.Unanswered })
        }
    }

    @Test
    fun realBundledMixedAssessmentStartsThroughSameEngine() = runTest {
        withTestDatabase { database ->
            assertEquals(CurriculumImportResult.Imported, CurriculumImporter(database).importCurriculum())

            val curriculumRepository = LocalCurriculumRepository(database)
            val engine = assessmentEngine(
                curriculumRepository = curriculumRepository,
                generateAttemptId = { "attempt_real_mixed" },
            )

            val result = engine.start(AssessmentConfig.Mixed(questionCount = 10))

            val session = assertIs<AssessmentStartResult.Started>(result).session
            assertTrue(session.questions.isNotEmpty())
            assertTrue(session.questions.size <= 10)
            assertEquals(session.questions.size, session.questions.map { it.id }.toSet().size)
            assertTrue(session.questions.all { it.status == ContentStatus.ACTIVE })
            val firstRoundSize = minOf(
                session.questions.size,
                curriculumRepository.getActiveQuestions()
                    .map { it.topicId }
                    .distinct()
                    .size,
            )
            assertEquals(
                firstRoundSize,
                session.questions.take(firstRoundSize)
                    .map { it.topicId }
                    .distinct()
                    .size,
            )
            assertEquals(AssessmentStatus.IN_PROGRESS, session.attempt.status)
        }
    }

    @Test
    fun controlledAssessmentCanBeAnsweredCompletedPersistedAndReconstructed() = runTest {
        withTestDatabase { database ->
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(database, loadCurriculum = { controlledCurriculum() }).importCurriculum(),
            )
            val components = assessmentComponents(
                database = database,
                generateAttemptId = { "attempt_controlled" },
                now = sequenceClock(StartedAt, CompletedAt),
            )

            val started = assertIs<AssessmentStartResult.Started>(
                components.engine.start(
                    AssessmentConfig.Focused(
                        scope = AssessmentScope.Topic(TopicId),
                        questionCount = 10,
                    ),
                ),
            )

            val answeredSingle = components.engine.submitAnswer(
                session = started.session,
                questionId = SingleQuestionId,
                selectedAnswerIds = listOf(SingleAnswerA),
            )
            val answeredAll = components.engine.submitAnswer(
                session = answeredSingle,
                questionId = MultiQuestionId,
                selectedAnswerIds = listOf(MultiAnswerC, MultiAnswerA),
            )

            assertTrue(components.engine.canComplete(answeredAll))
            val completed = components.engine.complete(answeredAll)
            components.assessmentRepository.save(completed.attempt)

            val restored = components.assessmentRepository.getById("attempt_controlled")
            assertEquals(completed.attempt, restored)
            assertEquals(AssessmentStatus.COMPLETED, restored?.status)
            assertEquals(AssessmentScore(totalQuestions = 2, correctAnswers = 2), restored?.score)
            assertEquals(StartedAt, restored?.startedAt)
            assertEquals(CompletedAt, restored?.completedAt)
            assertAnswered(
                questionAttempt = restored?.questionAttempts?.first { it.questionId == SingleQuestionId },
                expectedAnswerIds = setOf(SingleAnswerA),
                expectedIsCorrect = true,
            )
            assertAnswered(
                questionAttempt = restored?.questionAttempts?.first { it.questionId == MultiQuestionId },
                expectedAnswerIds = setOf(MultiAnswerA, MultiAnswerC),
                expectedIsCorrect = true,
            )
        }
    }

    @Test
    fun retakeCreatesSeparatePersistedAttemptAndPreservesOriginal() = runTest {
        withTestDatabase { database ->
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(database, loadCurriculum = { controlledCurriculum() }).importCurriculum(),
            )
            val components = assessmentComponents(
                database = database,
                generateAttemptId = sequenceIds("attempt_source", "attempt_retake"),
                now = sequenceClock(StartedAt, CompletedAt, RetakeStartedAt),
            )
            val source = completeAndSaveControlledAttempt(
                components = components,
                config = AssessmentConfig.Mixed(questionCount = 2),
            )
            val sourceBeforeRetake = components.assessmentRepository.getById(source.id)

            val retake = components.retakeService.createRetake(source.id)

            val retakeSession = assertIs<AssessmentRetakeResult.Created>(retake).session
            assertEquals("attempt_retake", retakeSession.attempt.id)
            assertNotEquals(source.id, retakeSession.attempt.id)
            assertEquals(source.config, retakeSession.attempt.config)
            assertEquals(AssessmentStatus.IN_PROGRESS, retakeSession.attempt.status)
            assertEquals(null, retakeSession.attempt.score)
            assertEquals(null, retakeSession.attempt.completedAt)
            assertTrue(retakeSession.attempt.questionAttempts.all { it.answerState == QuestionAnswerState.Unanswered })

            assertEquals(retakeSession.attempt, components.assessmentRepository.getById("attempt_retake"))
            assertEquals(sourceBeforeRetake, components.assessmentRepository.getById(source.id))
        }
    }

    @Test
    fun deprecatedQuestionsAreExcludedFromSelectionButResolvableForHistory() = runTest {
        withTestDatabase { database ->
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(database, loadCurriculum = { controlledCurriculum() }).importCurriculum(),
            )
            val activeComponents = assessmentComponents(
                database = database,
                generateAttemptId = { "attempt_historical" },
                now = sequenceClock(StartedAt, CompletedAt),
            )
            val historicalAttempt = completeAndSaveControlledAttempt(
                components = activeComponents,
                config = AssessmentConfig.Focused(
                    scope = AssessmentScope.Subtopic(SubtopicId),
                    questionCount = 1,
                ),
            )
            assertEquals(listOf(SingleQuestionId), historicalAttempt.questionAttempts.map { it.questionId })

            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(
                    database = database,
                    loadCurriculum = {
                        controlledCurriculum(singleQuestionStatus = ContentStatus.DEPRECATED)
                    },
                ).importCurriculum(),
            )

            val repository = LocalCurriculumRepository(database)
            val engine = assessmentEngine(
                curriculumRepository = repository,
                generateAttemptId = { "attempt_after_deprecation" },
                now = { RetakeStartedAt },
            )
            val afterDeprecation = assertIs<AssessmentStartResult.Started>(
                engine.start(
                    AssessmentConfig.Focused(
                        scope = AssessmentScope.Subtopic(SubtopicId),
                        questionCount = 10,
                    ),
                ),
            ).session

            assertEquals(listOf(MultiQuestionId), afterDeprecation.questions.map { it.id })
            assertEquals(
                historicalAttempt,
                LocalAssessmentRepository(AssessmentAttemptStore(database)).getById("attempt_historical"),
            )
            val deprecatedQuestion = repository.getQuestionById(SingleQuestionId)
            assertNotNull(deprecatedQuestion)
            assertEquals(ContentStatus.DEPRECATED, deprecatedQuestion.status)
        }
    }

    @Test
    fun koinGraphResolvesCompletedAssessmentDependenciesWhenDatabaseIsSupplied() = runTest {
        withTestDatabase { database ->
            val app = koinApplication {
                modules(
                    module {
                        single<CurriculumDatabase> { database }
                    },
                    curriculumDataModule,
                    assessmentDataModule,
                )
            }

            try {
                val koin = app.koin

                assertIs<CurriculumRepository>(koin.get<CurriculumRepository>())
                assertIs<CurriculumDataInitializer>(koin.get<CurriculumDataInitializer>())
                assertIs<AssessmentQuestionSelector>(koin.get<AssessmentQuestionSelector>())
                assertIs<AssessmentEngine>(koin.get<AssessmentEngine>())
                assertIs<AssessmentRepository>(koin.get<AssessmentRepository>())
                assertIs<AssessmentRetakeService>(koin.get<AssessmentRetakeService>())
                assertIs<LearningProgressService>(koin.get<LearningProgressService>())
            } finally {
                app.close()
            }
        }
    }

    private suspend fun firstTopicWithQuestions(repository: CurriculumRepository): String {
        for (topic in repository.getActiveTopics()) {
            if (repository.getActiveQuestionsByTopic(topic.id).isNotEmpty()) {
                return topic.id
            }
        }
        error("Expected bundled curriculum to contain at least one active topic with questions.")
    }

    private fun assessmentComponents(
        database: CurriculumDatabase,
        generateAttemptId: () -> String,
        now: () -> Instant,
    ): AssessmentComponents {
        val curriculumRepository = LocalCurriculumRepository(database)
        val selector = AssessmentQuestionSelector(
            curriculumRepository = curriculumRepository,
            randomize = { it },
        )
        val engine = AssessmentEngine(
            questionSelector = selector,
            generateAttemptId = generateAttemptId,
            now = now,
        )
        val assessmentRepository = LocalAssessmentRepository(
            store = AssessmentAttemptStore(database),
        )

        return AssessmentComponents(
            engine = engine,
            assessmentRepository = assessmentRepository,
            retakeService = AssessmentRetakeService(
                assessmentRepository = assessmentRepository,
                assessmentEngine = engine,
            ),
        )
    }

    private fun assessmentEngine(
        curriculumRepository: CurriculumRepository,
        generateAttemptId: () -> String,
        now: () -> Instant = { StartedAt },
    ): AssessmentEngine =
        AssessmentEngine(
            questionSelector = AssessmentQuestionSelector(
                curriculumRepository = curriculumRepository,
                randomize = { it },
            ),
            generateAttemptId = generateAttemptId,
            now = now,
        )

    private suspend fun completeAndSaveControlledAttempt(
        components: AssessmentComponents,
        config: AssessmentConfig,
    ): TestAttempt {
        val started = assertIs<AssessmentStartResult.Started>(
            components.engine.start(config),
        ).session
        val answeredSingle = components.engine.submitAnswer(
            session = started,
            questionId = SingleQuestionId,
            selectedAnswerIds = listOf(SingleAnswerA),
        )
        val answeredAll =
            if (answeredSingle.questions.any { it.id == MultiQuestionId }) {
                components.engine.submitAnswer(
                    session = answeredSingle,
                    questionId = MultiQuestionId,
                    selectedAnswerIds = listOf(MultiAnswerA, MultiAnswerC),
                )
            } else {
                answeredSingle
            }
        val completed = components.engine.complete(answeredAll)
        components.assessmentRepository.save(completed.attempt)
        return completed.attempt
    }

    private fun assertAnswered(
        questionAttempt: QuestionAttempt?,
        expectedAnswerIds: Set<String>,
        expectedIsCorrect: Boolean,
    ) {
        assertNotNull(questionAttempt)
        val answerState = assertIs<QuestionAnswerState.Answered>(questionAttempt.answerState)
        assertEquals(expectedAnswerIds, answerState.selectedAnswerIds)
        assertEquals(expectedIsCorrect, answerState.isCorrect)
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

    private fun controlledCurriculum(
        singleQuestionStatus: ContentStatus = ContentStatus.ACTIVE,
    ): Curriculum =
        Curriculum(
            topics = listOf(
                Topic(
                    id = TopicId,
                    name = "Topic A",
                ),
            ),
            subtopics = listOf(
                Subtopic(
                    id = SubtopicId,
                    topicId = TopicId,
                    name = "Subtopic A",
                ),
            ),
            questions = listOf(
                singleAnswerQuestion(status = singleQuestionStatus),
                multipleAnswerQuestion(),
            ),
        )

    private fun singleAnswerQuestion(
        status: ContentStatus = ContentStatus.ACTIVE,
    ): Question =
        Question(
            id = SingleQuestionId,
            topicId = TopicId,
            subtopicId = SubtopicId,
            text = "Which answer is correct?",
            answers = listOf(
                AnswerOption(SingleAnswerA, "A"),
                AnswerOption(SingleAnswerB, "B"),
                AnswerOption(SingleAnswerC, "C"),
            ),
            correctAnswerIds = listOf(SingleAnswerA),
            explanation = "A is the correct answer.",
            sources = listOf(
                SourceReference("Single source", "https://example.com/single"),
            ),
            status = status,
        )

    private fun multipleAnswerQuestion(): Question =
        Question(
            id = MultiQuestionId,
            topicId = TopicId,
            subtopicId = SubtopicId,
            text = "Which answers are correct?",
            answers = listOf(
                AnswerOption(MultiAnswerA, "A"),
                AnswerOption(MultiAnswerB, "B"),
                AnswerOption(MultiAnswerC, "C"),
            ),
            correctAnswerIds = listOf(MultiAnswerA, MultiAnswerC),
            explanation = "A and C are correct.",
            sources = listOf(
                SourceReference("Multiple source", "https://example.com/multiple"),
            ),
        )

    private fun sequenceIds(vararg ids: String): () -> String {
        val remaining = ids.toMutableList()
        return {
            check(remaining.isNotEmpty()) {
                "No attempt IDs remain."
            }
            remaining.removeAt(0)
        }
    }

    private fun sequenceClock(vararg instants: Instant): () -> Instant {
        val remaining = instants.toMutableList()
        return {
            check(remaining.isNotEmpty()) {
                "No instants remain."
            }
            remaining.removeAt(0)
        }
    }

    private data class AssessmentComponents(
        val engine: AssessmentEngine,
        val assessmentRepository: AssessmentRepository,
        val retakeService: AssessmentRetakeService,
    )

    private companion object {
        const val TopicId = "topic_a"
        const val SubtopicId = "subtopic_a"
        const val SingleQuestionId = "single_question"
        const val SingleAnswerA = "single_a"
        const val SingleAnswerB = "single_b"
        const val SingleAnswerC = "single_c"
        const val MultiQuestionId = "multi_question"
        const val MultiAnswerA = "multi_a"
        const val MultiAnswerB = "multi_b"
        const val MultiAnswerC = "multi_c"

        val StartedAt = Instant.fromEpochMilliseconds(1_700_000_000_000)
        val CompletedAt = Instant.fromEpochMilliseconds(1_700_000_060_000)
        val RetakeStartedAt = Instant.fromEpochMilliseconds(1_700_000_120_000)
    }
}
