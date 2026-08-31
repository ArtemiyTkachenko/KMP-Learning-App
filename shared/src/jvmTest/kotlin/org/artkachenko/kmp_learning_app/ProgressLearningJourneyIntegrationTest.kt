package org.artkachenko.kmp_learning_app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
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
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.curriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImportResult
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import org.artkachenko.kmp_learning_app.data.local.curriculum.repository.LocalCurriculumRepository
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.artkachenko.kmp_learning_app.learning_progress.SubtopicPerformance
import org.artkachenko.kmp_learning_app.learning_progress.TopicPerformance
import org.artkachenko.kmp_learning_app.learning_progress.WeakArea
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewService
import org.artkachenko.kmp_learning_app.progress.progressHistoryCardTag
import org.artkachenko.kmp_learning_app.progress.progressTopicCardTag
import org.artkachenko.kmp_learning_app.topic_study.topicStudyPresentationModule
import org.koin.compose.KoinApplication
import org.koin.core.context.stopKoin
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
internal class ProgressLearningJourneyIntegrationTest {
    @Test
    fun realHistoryDrivesProgressNavigationAndMistakeReview() {
        synchronized(appIntegrationMainDispatcherLock) {
            stopKoin()
            Dispatchers.setMain(Dispatchers.Unconfined)
            var database: CurriculumDatabase? = null
            try {
                runComposeUiTest {
                    val components = testComponents()
                    database = components.database
                    assertDomainCheckpoint(components)

                    setContent {
                        CompositionLocalProvider(LocalUriHandler provides components.uriHandler) {
                            KoinApplication(
                                configuration = koinConfiguration {
                                    modules(components.modules)
                                },
                            ) {
                                App()
                            }
                        }
                    }

                    waitForText("Topics")
                    onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.PROGRESS))
                        .performClick()
                    waitForText("Completed assessments")
                    // The derived values, not just their labels.
                    onNode(hasText("Questions answered") and hasText("7")).assertIsDisplayed()
                    onNode(hasText("Correct answers") and hasText("3")).assertIsDisplayed()
                    onNodeWithText("accuracy overall").assertIsDisplayed()
                    assertTrue(
                        onAllNodesWithText("42.9%").fetchSemanticsNodes().isNotEmpty(),
                    )

                    scrollToText("Weak areas")
                    assertTrue(onAllNodesWithText("Android").fetchSemanticsNodes().isNotEmpty())
                    assertTrue(onAllNodesWithText("Lifecycle").fetchSemanticsNodes().isNotEmpty())
                    // Scroll first: the navigation bar shortens the list, so a row further down
                    // is no longer composed just because the section header is visible.
                    scrollToText("State")
                    assertTrue(onAllNodesWithText("State").fetchSemanticsNodes().isNotEmpty())

                    onNode(hasScrollAction()).performScrollToNode(
                        hasText("Topic performance"),
                    )
                    onNodeWithText("Legacy Android").performScrollTo().assertIsDisplayed()
                    onNodeWithText("0 / 1 correct").assertIsDisplayed()
                    assertTrue(onAllNodesWithText("0%").fetchSemanticsNodes().isNotEmpty())
                    scrollToTag(progressTopicCardTag(AndroidTopicId))
                    onNodeWithTag(progressTopicCardTag(AndroidTopicId)).performClick()
                    waitForText("Subtopics")
                    onNodeWithText("Android").assertIsDisplayed()
                    onNodeWithText("2 / 5 correct").assertIsDisplayed()
                    onNodeWithText("40%").assertIsDisplayed()
                    onNodeWithText("Lifecycle").assertIsDisplayed()
                    onNodeWithText("1 / 3 correct").assertIsDisplayed()
                    onNodeWithText("33.3%").assertIsDisplayed()
                    onNodeWithText("State").assertIsDisplayed()
                    onNodeWithText("1 / 2 correct").assertIsDisplayed()
                    onNodeWithText("50%").assertIsDisplayed()
                    onNodeWithText("Never observed").assertDoesNotExist()
                    assertTrue(onAllNodesWithText("Weak area").fetchSemanticsNodes().size >= 3)
                    onNodeWithContentDescription("Back").performClick()
                    waitForText("Progress")

                    scrollToTag(progressHistoryCardTag(MixedAttemptId))
                    onNodeWithTag(progressHistoryCardTag(MixedAttemptId)).performClick()
                    waitForText("Score: 2 / 4")
                    onNodeWithText("Score: 2 / 4").assertIsDisplayed()
                    scrollToText("Lifecycle question")
                    onNodeWithText("Lifecycle question").assertIsDisplayed()
                    scrollToText("Question question_missing is no longer available.")
                    onNodeWithText("Question question_missing is no longer available.")
                        .assertIsDisplayed()
                    onNodeWithContentDescription("Back").performClick()
                    waitForText("Progress")

                    scrollToTag(progressHistoryCardTag(FocusedSubtopicAttemptId))
                    onNodeWithTag(progressHistoryCardTag(FocusedSubtopicAttemptId)).performClick()
                    waitForText("Score: 1 / 1")
                    onNodeWithText("Score: 1 / 1").assertIsDisplayed()
                    onNodeWithText("Lifecycle question").performScrollTo().assertIsDisplayed()
                    onNodeWithContentDescription("Back").performClick()
                    waitForText("Progress")

                    // The dashboard reports the size of the queue; opening it is the Mistakes
                    // navigation item's job, and that item carries the same count as a badge.
                    scrollToTextStartingWith("unresolved mistakes to review")
                    onNodeWithText("2 unresolved mistakes to review").assertIsDisplayed()
                    onNodeWithText("2", useUnmergedTree = true).assertIsDisplayed()
                    onNodeWithTag(
                        appNavigationBarItemTag(AppTopLevelDestination.MISTAKES),
                    ).performClick()
                    waitForText("Lifecycle question")
                    onNodeWithText("Lifecycle question").assertIsDisplayed()
                    onNodeWithText("Newest lifecycle selection").performScrollTo().assertIsDisplayed()
                    onNodeWithText("State question").assertDoesNotExist()
                    scrollToText("Legacy question")
                    onNodeWithText("Legacy question").assertIsDisplayed()
                    onNodeWithText("Selected legacy answer").performScrollTo().assertIsDisplayed()
                    assertTrue(onAllNodesWithText("Your answer").fetchSemanticsNodes().isNotEmpty())
                    onNodeWithText("Correct legacy answer").performScrollTo().assertIsDisplayed()
                    assertTrue(onAllNodesWithText("Correct answer").fetchSemanticsNodes().isNotEmpty())
                    onNodeWithText("Legacy explanation").performScrollTo().assertIsDisplayed()
                    scrollToText("Source: Lifecycle docs")
                    onNodeWithText("Source: Lifecycle docs").performClick()
                    assertEquals(listOf("https://example.com/lifecycle"), components.uriHandler.openedUris)
                }
            } finally {
                stopKoin()
                database?.close()
                Dispatchers.resetMain()
            }
        }
    }

    @Test
    fun realHistoryMakesAMistakeResolveAndReappearFromItsLatestOccurrence() = runTest {
        val database = createImportedDatabase()
        try {
            val curriculumRepository = LocalCurriculumRepository(database)
            val repository = LocalAssessmentRepository(AssessmentAttemptStore(database))
            val service = MistakeReviewService(
                repository,
                AssessmentReviewLoader(curriculumRepository),
            )

            repository.save(
                singleQuestionAttempt("attempt_wrong_1", 1_000, false, LifecycleOldSelectionId),
            )
            assertEquals(listOf(LifecycleQuestionId), service.load().map { it.questionId })

            repository.save(
                singleQuestionAttempt("attempt_correct", 2_000, true, LifecycleCorrectId),
            )
            assertEquals(emptyList(), service.load())

            repository.save(
                singleQuestionAttempt("attempt_wrong_2", 3_000, false, LifecycleNewestSelectionId),
            )
            val mistake = service.load().single()
            assertEquals("attempt_wrong_2", mistake.sourceAttemptId)
            val available = assertIs<ReviewQuestionItem.Available>(mistake.reviewItem)
            assertEquals(
                listOf(LifecycleNewestSelectionId),
                available.question.answers.filter { it.wasSelected }.map { it.id },
            )
        } finally {
            database.close()
        }
    }

    private suspend fun testComponents(): TestComponents {
        val database = createImportedDatabase()
        val localCurriculumRepository = LocalCurriculumRepository(database)
        val curriculumRepository = CurriculumRepositoryWithMissingQuestion(
            delegate = localCurriculumRepository,
            missingQuestionId = MissingQuestionId,
        )
        val assessmentRepository = LocalAssessmentRepository(AssessmentAttemptStore(database))
        historyFixture().forEach { assessmentRepository.save(it) }
        val progressService = LearningProgressService(assessmentRepository, curriculumRepository)
        val reviewLoader = AssessmentReviewLoader(curriculumRepository)
        val mistakeService = MistakeReviewService(assessmentRepository, reviewLoader)

        return TestComponents(
            database = database,
            repository = assessmentRepository,
            progressService = progressService,
            mistakeService = mistakeService,
            uriHandler = RecordingUriHandler(),
            modules = listOf(
                curriculumDataModule,
                assessmentDataModule,
                topicStudyPresentationModule,
                module {
                    single<CurriculumDatabase> { database }
                    single<CurriculumRepository> { curriculumRepository }
                    single<AssessmentRepository> { assessmentRepository }
                    single { progressService }
                    single { reviewLoader }
                    single { mistakeService }
                },
            ),
        )
    }

    private suspend fun assertDomainCheckpoint(components: TestComponents) {
        val completed = components.repository.getCompletedAttempts()
        assertEquals(
            listOf(MixedAttemptId, FocusedSubtopicAttemptId, FocusedTopicAttemptId),
            completed.map { it.id },
        )
        assertEquals(
            AssessmentStatus.IN_PROGRESS,
            components.repository.getById(InProgressAttemptId)?.status,
        )

        val snapshot = components.progressService.load()
        assertEquals(3, snapshot.completedAttemptCount)
        assertEquals(7, snapshot.answeredQuestionCount)
        assertEquals(3, snapshot.correctAnswerCount)
        assertEquals(3.0 / 7.0 * 100.0, snapshot.percentage, absoluteTolerance = 0.000_001)
        assertEquals(6, snapshot.topics.sumOf { it.answeredCount })

        assertPerformance(
            snapshot.topics.single { it.topicId == AndroidTopicId },
            5,
            2,
            40.0,
            true,
        )
        assertPerformance(
            snapshot.topics.single { it.topicId == LegacyTopicId },
            1,
            0,
            0.0,
            false,
        )
        assertPerformance(
            snapshot.subtopics.single { it.subtopicId == LifecycleSubtopicId },
            3,
            1,
            100.0 / 3.0,
            true,
        )
        assertPerformance(
            snapshot.subtopics.single { it.subtopicId == StateSubtopicId },
            2,
            1,
            50.0,
            true,
        )
        assertPerformance(
            snapshot.subtopics.single { it.subtopicId == LegacySubtopicId },
            1,
            0,
            0.0,
            false,
        )
        assertFalse(snapshot.subtopics.any { it.subtopicId == UnobservedSubtopicId })
        assertEquals(
            listOf(LifecycleSubtopicId, AndroidTopicId, StateSubtopicId),
            snapshot.weakAreas.map {
                when (it) {
                    is WeakArea.Topic -> it.performance.topicId
                    is WeakArea.Subtopic -> it.performance.subtopicId
                }
            },
        )

        val mistakes = components.mistakeService.load()
        assertEquals(listOf(LifecycleQuestionId, LegacyQuestionId), mistakes.map { it.questionId })
        val lifecycle = assertIs<ReviewQuestionItem.Available>(mistakes.first().reviewItem)
        assertEquals(
            listOf(LifecycleNewestSelectionId),
            lifecycle.question.answers.filter { it.wasSelected }.map { it.id },
        )
    }

    private fun assertPerformance(
        performance: TopicPerformance,
        answered: Int,
        correct: Int,
        percentage: Double,
        weak: Boolean,
    ) {
        assertEquals(answered, performance.answeredCount)
        assertEquals(correct, performance.correctCount)
        assertEquals(percentage, performance.percentage, absoluteTolerance = 0.000_001)
        assertEquals(weak, performance.isWeak)
    }

    private fun assertPerformance(
        performance: SubtopicPerformance,
        answered: Int,
        correct: Int,
        percentage: Double,
        weak: Boolean,
    ) {
        assertEquals(answered, performance.answeredCount)
        assertEquals(correct, performance.correctCount)
        assertEquals(percentage, performance.percentage, absoluteTolerance = 0.000_001)
        assertEquals(weak, performance.isWeak)
    }

    private fun ComposeUiTest.waitForText(text: String) {
        waitUntil(timeoutMillis = IntegrationWaitTimeoutMillis) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun ComposeUiTest.scrollToText(text: String) {
        waitForScrollableContent()
        onNode(hasScrollAction()).performScrollToNode(hasText(text))
    }

    /** For rows whose label carries a leading or trailing count. */
    private fun ComposeUiTest.scrollToTextStartingWith(text: String) {
        waitForScrollableContent()
        onNode(hasScrollAction()).performScrollToNode(hasText(text, substring = true))
    }

    private fun ComposeUiTest.scrollToTag(tag: String) {
        waitForScrollableContent()
        onNode(hasScrollAction()).performScrollToNode(
            androidx.compose.ui.test.hasTestTag(tag),
        )
    }

    private fun ComposeUiTest.waitForScrollableContent() {
        waitUntil(timeoutMillis = IntegrationWaitTimeoutMillis) {
            onAllNodes(hasScrollAction()).fetchSemanticsNodes().size == 1
        }
    }

    private data class TestComponents(
        val database: CurriculumDatabase,
        val repository: AssessmentRepository,
        val progressService: LearningProgressService,
        val mistakeService: MistakeReviewService,
        val uriHandler: RecordingUriHandler,
        val modules: List<org.koin.core.module.Module>,
    )

    private class RecordingUriHandler : UriHandler {
        val openedUris = mutableListOf<String>()

        override fun openUri(uri: String) {
            openedUris += uri
        }
    }

    /**
     * Schema v3 prevents persisting an orphan question ID. Import the row so repository.save()
     * crosses the real FK boundary, then model later lookup loss at the repository contract that
     * progress/review consumers are required to tolerate.
     */
    private class CurriculumRepositoryWithMissingQuestion(
        private val delegate: CurriculumRepository,
        private val missingQuestionId: String,
    ) : CurriculumRepository by delegate {
        override suspend fun getQuestionById(questionId: String): Question? =
            if (questionId == missingQuestionId) null else delegate.getQuestionById(questionId)
    }
}

private suspend fun createImportedDatabase(): CurriculumDatabase {
    val database = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
    assertIs<CurriculumImportResult.Imported>(
        CurriculumImporter(database, loadCurriculum = ::progressFixture).importCurriculum(),
    )
    return database
}

private fun historyFixture(): List<TestAttempt> = listOf(
    completedAttempt(
        id = FocusedTopicAttemptId,
        config = AssessmentConfig.Focused(AssessmentScope.Topic(AndroidTopicId), 10),
        completedAtMillis = 1_000,
        answers = listOf(
            answered(LifecycleQuestionId, LifecycleOldSelectionId, false),
            answered(StateQuestionId, StateWrongId, false),
        ),
    ),
    completedAttempt(
        id = FocusedSubtopicAttemptId,
        config = AssessmentConfig.Focused(AssessmentScope.Subtopic(LifecycleSubtopicId), 10),
        completedAtMillis = 2_000,
        answers = listOf(answered(LifecycleQuestionId, LifecycleCorrectId, true)),
    ),
    completedAttempt(
        id = MixedAttemptId,
        config = AssessmentConfig.Mixed(20),
        completedAtMillis = 3_000,
        answers = listOf(
            answered(LifecycleQuestionId, LifecycleNewestSelectionId, false),
            answered(StateQuestionId, StateCorrectId, true),
            answered(LegacyQuestionId, LegacyWrongId, false),
            answered(MissingQuestionId, MissingAnswerId, true),
        ),
    ),
    TestAttempt(
        id = InProgressAttemptId,
        config = AssessmentConfig.Mixed(20),
        questionAttempts = listOf(QuestionAttempt(LifecycleQuestionId)),
        status = AssessmentStatus.IN_PROGRESS,
        startedAt = Instant.fromEpochMilliseconds(4_000),
    ),
)

private fun singleQuestionAttempt(
    id: String,
    completedAtMillis: Long,
    correct: Boolean,
    selectedAnswerId: String,
): TestAttempt = completedAttempt(
    id = id,
    config = AssessmentConfig.Mixed(1),
    completedAtMillis = completedAtMillis,
    answers = listOf(answered(LifecycleQuestionId, selectedAnswerId, correct)),
)

private fun completedAttempt(
    id: String,
    config: AssessmentConfig,
    completedAtMillis: Long,
    answers: List<QuestionAttempt>,
): TestAttempt = TestAttempt(
    id = id,
    config = config,
    questionAttempts = answers,
    status = AssessmentStatus.COMPLETED,
    startedAt = Instant.fromEpochMilliseconds(completedAtMillis - 100),
    completedAt = Instant.fromEpochMilliseconds(completedAtMillis),
    score = AssessmentScore(
        totalQuestions = answers.size,
        correctAnswers = answers.count {
            (it.answerState as QuestionAnswerState.Answered).isCorrect
        },
    ),
)

private fun answered(
    questionId: String,
    selectedAnswerId: String,
    correct: Boolean,
): QuestionAttempt = QuestionAttempt(
    questionId,
    QuestionAnswerState.Answered(setOf(selectedAnswerId), correct),
)

private fun progressFixture(): Curriculum = Curriculum(
    topics = listOf(
        Topic(AndroidTopicId, "Android"),
        Topic(LegacyTopicId, "Legacy Android", ContentStatus.DEPRECATED),
    ),
    subtopics = listOf(
        Subtopic(LifecycleSubtopicId, AndroidTopicId, "Lifecycle"),
        Subtopic(StateSubtopicId, AndroidTopicId, "State"),
        Subtopic(UnobservedSubtopicId, AndroidTopicId, "Never observed"),
        Subtopic(LegacySubtopicId, LegacyTopicId, "Legacy APIs", ContentStatus.DEPRECATED),
    ),
    questions = listOf(
        fixtureQuestion(
            LifecycleQuestionId,
            AndroidTopicId,
            LifecycleSubtopicId,
            "Lifecycle question",
            LifecycleCorrectId,
            listOf(
                AnswerOption(LifecycleCorrectId, "Correct lifecycle answer"),
                AnswerOption(LifecycleNewestSelectionId, "Newest lifecycle selection"),
                AnswerOption(LifecycleOldSelectionId, "Older lifecycle selection"),
            ),
            "Lifecycle explanation",
            "Lifecycle docs",
            "https://example.com/lifecycle",
        ),
        fixtureQuestion(
            StateQuestionId,
            AndroidTopicId,
            StateSubtopicId,
            "State question",
            StateCorrectId,
            listOf(
                AnswerOption(StateCorrectId, "Correct state answer"),
                AnswerOption(StateWrongId, "Incorrect state answer"),
            ),
            "State explanation",
            "State docs",
            "https://example.com/state",
        ),
        fixtureQuestion(
            LegacyQuestionId,
            LegacyTopicId,
            LegacySubtopicId,
            "Legacy question",
            LegacyCorrectId,
            listOf(
                AnswerOption(LegacyCorrectId, "Correct legacy answer"),
                AnswerOption(LegacyWrongId, "Selected legacy answer"),
            ),
            "Legacy explanation",
            "Legacy docs",
            "https://example.com/legacy",
            ContentStatus.DEPRECATED,
        ),
        fixtureQuestion(
            MissingQuestionId,
            AndroidTopicId,
            LifecycleSubtopicId,
            "Question that later becomes unavailable",
            MissingAnswerId,
            listOf(
                AnswerOption(MissingAnswerId, "Historical answer"),
                AnswerOption("question_missing_b", "Historical distractor"),
            ),
            "Historical explanation",
            "Historical docs",
            "https://example.com/historical",
        ),
    ),
)

private fun fixtureQuestion(
    id: String,
    topicId: String,
    subtopicId: String,
    text: String,
    correctAnswerId: String,
    answers: List<AnswerOption>,
    explanation: String,
    sourceTitle: String,
    sourceUrl: String,
    status: ContentStatus = ContentStatus.ACTIVE,
): Question = Question(
    id = id,
    topicId = topicId,
    subtopicId = subtopicId,
    text = text,
    answers = answers,
    selectionMode = AnswerSelectionMode.SINGLE,
    correctAnswerIds = listOf(correctAnswerId),
    explanation = explanation,
    sources = listOf(SourceReference(sourceTitle, sourceUrl)),
    status = status,
)

private const val AndroidTopicId = "topic_android"
private const val LifecycleSubtopicId = "subtopic_lifecycle"
private const val StateSubtopicId = "subtopic_state"
private const val UnobservedSubtopicId = "subtopic_unobserved"
private const val LegacyTopicId = "topic_legacy"
private const val LegacySubtopicId = "subtopic_legacy"
private const val LifecycleQuestionId = "question_lifecycle"
private const val StateQuestionId = "question_state"
private const val LegacyQuestionId = "question_legacy"
private const val MissingQuestionId = "question_missing"
private const val LifecycleCorrectId = "question_lifecycle_a"
private const val LifecycleNewestSelectionId = "question_lifecycle_b"
private const val LifecycleOldSelectionId = "question_lifecycle_c"
private const val StateCorrectId = "question_state_a"
private const val StateWrongId = "question_state_b"
private const val LegacyCorrectId = "question_legacy_a"
private const val LegacyWrongId = "question_legacy_b"
private const val MissingAnswerId = "question_missing_a"
private const val FocusedTopicAttemptId = "attempt_focused_topic"
private const val FocusedSubtopicAttemptId = "attempt_focused_subtopic"
private const val MixedAttemptId = "attempt_mixed"
private const val InProgressAttemptId = "attempt_in_progress"
private const val IntegrationWaitTimeoutMillis = 15_000L
