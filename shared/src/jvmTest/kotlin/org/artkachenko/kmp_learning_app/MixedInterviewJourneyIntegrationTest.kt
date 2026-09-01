package org.artkachenko.kmp_learning_app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
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
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeService
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentSessionLoader
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingFinishTag
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingSubmitTag
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
import org.artkachenko.kmp_learning_app.data.local.assessment.AssessmentAttemptStore
import org.artkachenko.kmp_learning_app.data.local.assessment.assessmentDataModule
import org.artkachenko.kmp_learning_app.data.local.assessment.repository.LocalAssessmentRepository
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.curriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImportResult
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import org.artkachenko.kmp_learning_app.data.local.curriculum.repository.LocalCurriculumRepository
import org.artkachenko.kmp_learning_app.mixed_interview.InterviewStartButtonTag
import org.artkachenko.kmp_learning_app.mixed_interview.MixedInterviewDefaults
import org.artkachenko.kmp_learning_app.mixed_interview.MixedResultPracticeAgainTag
import org.artkachenko.kmp_learning_app.topic_study.topicStudyPresentationModule
import org.koin.compose.KoinApplication
import org.koin.core.context.stopKoin
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
internal class MixedInterviewJourneyIntegrationTest {
    @Test
    fun appCompletesBalancedMixedInterviewReviewsResultAndReopensDurableRetake() {
        synchronized(appIntegrationMainDispatcherLock) {
            stopKoin()
            Dispatchers.setMain(Dispatchers.Unconfined)
            lateinit var components: TestComponents
            var database: CurriculumDatabase? = null
            try {
                runComposeUiTest {
                    components = testComponents()
                    database = components.database
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

                    waitUntil(timeoutMillis = 5_000) {
                        onAllNodesWithText("Topics").fetchSemanticsNodes().isNotEmpty()
                    }
                    // The interview has its own destination now, reached from the navigation bar.
                    onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.INTERVIEW))
                        .performClick()
                    waitForText("Start Mixed Interview")
                    onNodeWithText("Mixed Android Interview").assertIsDisplayed()
                    onNodeWithText("20-question interview").assertIsDisplayed()
                    assertEquals(20, MixedInterviewDefaults.QuestionCount)
                    onNodeWithTag(InterviewStartButtonTag).performClick()

                    waitForText("Kotlin single question")
                    onNodeWithText("Kotlin single question").assertIsDisplayed()
                    onNodeWithText("Question 1 of 4").assertIsDisplayed()
                    val initial = assertNotNull(components.repository.getById(OriginalAttemptId))
                    assertEquals(AssessmentConfig.Mixed(questionCount = 20), initial.config)
                    assertEquals(
                        listOf(KotlinSingleId, ComposeMultipleId, CoroutinesSingleId, KotlinSecondId),
                        initial.questionAttempts.map { it.questionId },
                    )
                    assertEquals(AssessmentStatus.IN_PROGRESS, initial.status)
                    assertNull(initial.score)
                    assertNull(initial.completedAt)
                    assertTrue(initial.questionAttempts.all {
                        it.answerState == QuestionAnswerState.Unanswered
                    })

                    answer(listOf("A"))
                    waitForText("Compose multiple question")
                    onNodeWithText("Compose multiple question").assertIsDisplayed()
                    onNodeWithText("Question 2 of 4").assertIsDisplayed()
                    val afterSingle = assertNotNull(components.repository.getById(OriginalAttemptId))
                    assertEquals(
                        QuestionAnswerState.Answered(setOf("A"), isCorrect = true),
                        afterSingle.questionAttempts[0].answerState,
                    )
                    assertTrue(afterSingle.questionAttempts.drop(1).all {
                        it.answerState == QuestionAnswerState.Unanswered
                    })

                    answer(listOf("A", "B"))
                    waitForText("Coroutines single question")
                    onNodeWithText("Coroutines single question").assertIsDisplayed()
                    val afterMultiple = assertNotNull(components.repository.getById(OriginalAttemptId))
                    assertEquals(
                        QuestionAnswerState.Answered(setOf("A", "B"), isCorrect = false),
                        afterMultiple.questionAttempts[1].answerState,
                    )

                    answer(listOf("A"))
                    waitForText("Kotlin second question")
                    onNodeWithText("Kotlin second question").assertIsDisplayed()
                    answer(listOf("B"))

                    waitForText("All questions answered. Ready to finish.")
                    onNodeWithText("All questions answered. Ready to finish.").assertIsDisplayed()
                    val ready = assertNotNull(components.repository.getById(OriginalAttemptId))
                    assertEquals(AssessmentStatus.IN_PROGRESS, ready.status)
                    assertTrue(ready.questionAttempts.all {
                        it.answerState is QuestionAnswerState.Answered
                    })
                    assertNull(ready.score)
                    assertNull(ready.completedAt)
                    onNodeWithTag(AssessmentTakingFinishTag).performClick()

                    waitForText("Score: 2 / 4")
                    onNodeWithText("Score: 2 / 4").assertIsDisplayed()
                    assertTrue(
                        onAllNodesWithText("50", substring = true)
                            .fetchSemanticsNodes()
                            .isNotEmpty(),
                    )
                    val reconstructedRepository = LocalAssessmentRepository(
                        AssessmentAttemptStore(components.database),
                    )
                    val completed = assertNotNull(
                        reconstructedRepository.getById(OriginalAttemptId),
                    )
                    assertEquals(AssessmentStatus.COMPLETED, completed.status)
                    assertEquals(AssessmentScore(totalQuestions = 4, correctAnswers = 2), completed.score)
                    assertEquals(50.0, completed.score?.percentage)
                    assertNotNull(completed.completedAt)

                    onNodeWithText("Kotlin").performScrollTo().assertIsDisplayed()
                    onNodeWithText("1 / 2 correct").performScrollTo().assertIsDisplayed()
                    onNodeWithText("Compose").performScrollTo().assertIsDisplayed()
                    onNodeWithText("0 / 1 correct").performScrollTo().assertIsDisplayed()
                    assertTrue(
                        onAllNodesWithText("0%", substring = true)
                            .fetchSemanticsNodes()
                            .isNotEmpty(),
                    )
                    onNodeWithText("Coroutines").performScrollTo().assertIsDisplayed()
                    onNodeWithText("1 / 1 correct").performScrollTo().assertIsDisplayed()
                    assertTrue(
                        onAllNodesWithText("100%", substring = true)
                            .fetchSemanticsNodes()
                            .isNotEmpty(),
                    )

                    scrollToText("Compose multiple question")
                    onNodeWithText("Compose multiple question").assertIsDisplayed()
                    assertTrue(onAllNodesWithText("Incorrect").fetchSemanticsNodes().isNotEmpty())
                    assertTrue(onAllNodesWithText("Your answer").fetchSemanticsNodes().isNotEmpty())
                    assertTrue(onAllNodesWithText("Correct answer").fetchSemanticsNodes().isNotEmpty())
                    scrollToText("Compose exact-set explanation")
                    onNodeWithText("Compose exact-set explanation").assertIsDisplayed()
                    scrollToText("Source: Compose docs")
                    onNodeWithText("Source: Compose docs").performClick()
                    assertEquals(listOf("https://example.com/compose"), components.uriHandler.openedUris)

                    val sourceBeforeRetake = assertNotNull(
                        components.repository.getById(OriginalAttemptId),
                    )
                    scrollToText("Practice Again")
                    onNodeWithTag(MixedResultPracticeAgainTag).performClick()
                    waitForText("Kotlin single question")
                    onNodeWithText("Kotlin single question").assertIsDisplayed()
                    onNodeWithText("Question 1 of 4").assertIsDisplayed()

                    val retake = assertNotNull(components.repository.getById(RetakeAttemptId))
                    assertNotEquals(sourceBeforeRetake.id, retake.id)
                    assertEquals(sourceBeforeRetake.config, retake.config)
                    assertEquals(AssessmentStatus.IN_PROGRESS, retake.status)
                    assertNull(retake.score)
                    assertNull(retake.completedAt)
                    assertTrue(retake.questionAttempts.all {
                        it.answerState == QuestionAnswerState.Unanswered
                    })
                    assertEquals(listOf(OriginalAttemptId, RetakeAttemptId), components.createdAttemptIds)
                    assertEquals(sourceBeforeRetake, components.repository.getById(OriginalAttemptId))

                    onNodeWithContentDescription("Back").performClick()
                    waitForText("Score: 2 / 4")
                    onNodeWithText("Score: 2 / 4").assertIsDisplayed()
                    scrollToText("Compose exact-set explanation")
                    onNodeWithText("Compose exact-set explanation").assertIsDisplayed()
                    scrollToText("Practice Again")
                    onNodeWithTag(MixedResultPracticeAgainTag)
                        .assertIsEnabled()
                }
            } finally {
                stopKoin()
                database?.close()
                Dispatchers.resetMain()
            }
        }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.answer(answerIds: List<String>) {
        answerIds.forEach { answerId -> onNodeWithText(answerId).performClick() }
        onNodeWithTag(AssessmentTakingSubmitTag).performClick()
    }

    private fun androidx.compose.ui.test.ComposeUiTest.waitForText(text: String) {
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.scrollToText(text: String) {
        onNode(hasScrollAction()).performScrollToNode(hasText(text))
    }

    private suspend fun testComponents(): TestComponents {
        val database = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        assertIs<CurriculumImportResult.Imported>(
            CurriculumImporter(database, loadCurriculum = ::fixture).importCurriculum(),
        )
        val curriculumRepository = LocalCurriculumRepository(database)
        val assessmentRepository = LocalAssessmentRepository(AssessmentAttemptStore(database))
        val selector = AssessmentQuestionSelector(
            curriculumRepository = curriculumRepository,
            completedHistory = { assessmentRepository.getCompletedAttempts() },
            randomize = { it },
        )
        val availableIds = listOf(OriginalAttemptId, RetakeAttemptId).iterator()
        val createdAttemptIds = mutableListOf<String>()
        val engine = AssessmentEngine(
            questionSelector = selector,
            generateAttemptId = {
                check(availableIds.hasNext()) { "Unexpected additional assessment creation." }
                availableIds.next().also(createdAttemptIds::add)
            },
            now = { Instant.fromEpochMilliseconds(1_000) },
        )
        return TestComponents(
            database = database,
            repository = assessmentRepository,
            uriHandler = RecordingUriHandler(),
            createdAttemptIds = createdAttemptIds,
            modules = listOf(
                curriculumDataModule,
                assessmentDataModule,
                topicStudyPresentationModule,
                module {
                    single<CurriculumDatabase> { database }
                    single<CurriculumRepository> { curriculumRepository }
                    single<AssessmentRepository> { assessmentRepository }
                    single { selector }
                    single { engine }
                    single { AssessmentSessionLoader(get(), get()) }
                    single { AssessmentRetakeService(get(), get()) }
                },
            ),
        )
    }

    private data class TestComponents(
        val database: CurriculumDatabase,
        val repository: AssessmentRepository,
        val uriHandler: RecordingUriHandler,
        val createdAttemptIds: List<String>,
        val modules: List<org.koin.core.module.Module>,
    )

    private class RecordingUriHandler : UriHandler {
        val openedUris = mutableListOf<String>()

        override fun openUri(uri: String) {
            openedUris += uri
        }
    }

    private fun fixture() = Curriculum(
        topics = listOf(
            Topic(KotlinTopicId, "Kotlin"),
            Topic(ComposeTopicId, "Compose"),
            Topic(CoroutinesTopicId, "Coroutines"),
        ),
        subtopics = listOf(
            Subtopic("kotlin_core", KotlinTopicId, "Kotlin core"),
            Subtopic("compose_core", ComposeTopicId, "Compose core"),
            Subtopic("coroutines_core", CoroutinesTopicId, "Coroutines core"),
        ),
        questions = listOf(
            question(
                id = KotlinSingleId,
                topicId = KotlinTopicId,
                subtopicId = "kotlin_core",
                text = "Kotlin single question",
                selectionMode = AnswerSelectionMode.SINGLE,
                correctAnswerIds = listOf("A"),
                explanation = "Kotlin single explanation",
                sourceTitle = "Kotlin docs",
                sourceUrl = "https://example.com/kotlin",
            ),
            question(
                id = KotlinSecondId,
                topicId = KotlinTopicId,
                subtopicId = "kotlin_core",
                text = "Kotlin second question",
                selectionMode = AnswerSelectionMode.SINGLE,
                correctAnswerIds = listOf("A"),
                explanation = "Kotlin second explanation",
                sourceTitle = "Kotlin language docs",
                sourceUrl = "https://example.com/kotlin-second",
            ),
            question(
                id = ComposeMultipleId,
                topicId = ComposeTopicId,
                subtopicId = "compose_core",
                text = "Compose multiple question",
                selectionMode = AnswerSelectionMode.MULTIPLE,
                correctAnswerIds = listOf("A", "C"),
                explanation = "Compose exact-set explanation",
                sourceTitle = "Compose docs",
                sourceUrl = "https://example.com/compose",
            ),
            question(
                id = CoroutinesSingleId,
                topicId = CoroutinesTopicId,
                subtopicId = "coroutines_core",
                text = "Coroutines single question",
                selectionMode = AnswerSelectionMode.SINGLE,
                correctAnswerIds = listOf("A"),
                explanation = "Coroutines single explanation",
                sourceTitle = "Coroutines docs",
                sourceUrl = "https://example.com/coroutines",
            ),
        ),
    )

    private fun question(
        id: String,
        topicId: String,
        subtopicId: String,
        text: String,
        selectionMode: AnswerSelectionMode,
        correctAnswerIds: List<String>,
        explanation: String,
        sourceTitle: String,
        sourceUrl: String,
    ) = Question(
        id = id,
        topicId = topicId,
        subtopicId = subtopicId,
        text = text,
        answers = listOf(
            AnswerOption("A", "A"),
            AnswerOption("B", "B"),
            AnswerOption("C", "C"),
        ),
        selectionMode = selectionMode,
        level = QuestionLevel.FOUNDATION,
        correctAnswerIds = correctAnswerIds,
        explanation = explanation,
        sources = listOf(SourceReference(sourceTitle, sourceUrl)),
        status = ContentStatus.ACTIVE,
    )
}

private const val OriginalAttemptId = "mixed-original"
private const val RetakeAttemptId = "mixed-retake"
private const val KotlinTopicId = "topic_kotlin"
private const val ComposeTopicId = "topic_compose"
private const val CoroutinesTopicId = "topic_coroutines"
private const val KotlinSingleId = "kotlin_single"
private const val KotlinSecondId = "kotlin_second"
private const val ComposeMultipleId = "compose_multiple"
private const val CoroutinesSingleId = "coroutines_single"
