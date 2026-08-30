package org.artkachenko.kmp_learning_app

import androidx.compose.material3.MaterialTheme
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
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
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
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Curriculum
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.data.local.assessment.assessmentDataModule
import org.artkachenko.kmp_learning_app.data.local.assessment.repository.LocalAssessmentRepository
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.curriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import org.artkachenko.kmp_learning_app.data.local.curriculum.repository.LocalCurriculumRepository
import org.artkachenko.kmp_learning_app.progress.progressTopicCardTag
import org.artkachenko.kmp_learning_app.topic_study.focused_result.FocusedResultPracticeAgainTag
import org.artkachenko.kmp_learning_app.topic_study.topicStudyPresentationModule
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.SubtopicPracticeButtonTag
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicPracticeButtonTag
import org.koin.compose.KoinApplication
import org.koin.core.context.stopKoin
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
internal class FocusedLearningJourneyIntegrationTest {
    @Test
    fun appDrivesTopicPracticeCompletionAndOneDurableRetake() {
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
                MaterialTheme {
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
                onAllNodesWithText("Android").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Android").assertIsDisplayed().performClick()
            onNodeWithText("Core").assertIsDisplayed()
            onNodeWithTag(TopicPracticeButtonTag).assertIsDisplayed().performClick()

            onNodeWithText("Single question").assertIsDisplayed()
            onNodeWithText("A").performClick()
            onNodeWithTag(AssessmentTakingSubmitTag).performClick()
            onNodeWithText("Multiple question").assertIsDisplayed()
            onNodeWithText("A").performClick()
            onNodeWithText("B").performClick()
            onNodeWithTag(AssessmentTakingSubmitTag).performClick()
            onNodeWithText("All questions answered. Ready to finish.").assertIsDisplayed()
            onNodeWithTag(AssessmentTakingFinishTag).performClick()

            onNodeWithText("Score: 1 / 2").assertIsDisplayed()
            onNodeWithText("Single explanation").assertIsDisplayed()
            onNodeWithText("Source: Single source").assertIsDisplayed()
            assertEquals(AssessmentStatus.COMPLETED, components.repository.getById("attempt-original")?.status)
            assertEquals(1, components.repository.getById("attempt-original")?.score?.correctAnswers)

            onNodeWithTag(FocusedResultPracticeAgainTag).performClick()
            onNodeWithText("Single question").assertIsDisplayed()
            assertEquals(AssessmentStatus.IN_PROGRESS, components.repository.getById("attempt-retake")?.status)
            assertTrue(components.repository.getById("attempt-retake")?.questionAttempts?.all {
                it.answerState == QuestionAnswerState.Unanswered
            } == true)
            assertEquals(null, components.repository.getById("attempt-retake-2"))

            onNodeWithContentDescription("Back").performClick()
            onNodeWithText("Score: 1 / 2").assertIsDisplayed()

            onNodeWithContentDescription("Back").performClick()
            onNodeWithContentDescription("Back").performClick()

            onNodeWithText("View progress").assertIsDisplayed().performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Completed assessments").fetchSemanticsNodes().isNotEmpty()
            }
            // The derived values, not just their labels: this is the end-to-end check that Room
            // history reaches the dashboard with the right numbers.
            onNode(hasText("Questions answered") and hasText("2")).assertIsDisplayed()
            onNode(hasText("Correct answers") and hasText("1")).assertIsDisplayed()
            // 50% is the overall headline here and also the topic and weak-area figures.
            onNodeWithText("accuracy overall").assertIsDisplayed()
            assertTrue(onAllNodesWithText("50%").fetchSemanticsNodes().isNotEmpty())
            onNode(hasScrollAction()).performScrollToNode(hasText("Focused practice"))
            onNodeWithText("Focused practice").performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Score: 1 / 2").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithContentDescription("Back").performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Completed assessments").fetchSemanticsNodes().isNotEmpty()
            }

            // Mistake review: the multiple-choice question was answered incorrectly and the single
            // question correctly, so only the former is unresolved.
            onNode(hasScrollAction()).performScrollToNode(
                hasText("Review mistakes", substring = true),
            )
            onNodeWithText("Review mistakes", substring = true).performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Multiple question").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Multiple explanation").performScrollTo().assertIsDisplayed()
            onNodeWithText("Single question").assertDoesNotExist()
            onNodeWithContentDescription("Back").performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Completed assessments").fetchSemanticsNodes().isNotEmpty()
            }

            // Topic drill-down. "Android" also labels the weak-area parent and the focused history
            // row, so target the card by its stable tag rather than by text.
            onNode(hasScrollAction()).performScrollToNode(hasText("Topic performance"))
            onNodeWithTag(progressTopicCardTag("topic_android")).performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Core").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Weak area").assertIsDisplayed()
            onNodeWithContentDescription("Back").performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Completed assessments").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithContentDescription("Back").performClick()

            onNodeWithText("Android").assertIsDisplayed().performClick()
            onNodeWithTag(SubtopicPracticeButtonTag).performClick()
            onNodeWithText("Single question").assertIsDisplayed()
            assertEquals(
                AssessmentConfig.Focused(AssessmentScope.Subtopic("subtopic_core"), 10),
                components.repository.getById("attempt-subtopic")?.config,
            )
                }
            } finally {
                stopKoin()
                database?.close()
                Dispatchers.resetMain()
            }
        }
    }

    private suspend fun testComponents(ids: List<String> = listOf("attempt-original", "attempt-retake", "attempt-subtopic")): TestComponents {
        val db = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        assertIs<org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImportResult.Imported>(
            CurriculumImporter(db, loadCurriculum = { fixture() }).importCurriculum(),
        )
        val curriculum = LocalCurriculumRepository(db)
        val repository = LocalAssessmentRepository(
            org.artkachenko.kmp_learning_app.data.local.assessment.AssessmentAttemptStore(db),
        )
        val selector = AssessmentQuestionSelector(curriculum, randomize = { it })
        val engine = AssessmentEngine(
            selector,
            generateAttemptId = ids.iterator()::next,
            now = { Instant.fromEpochMilliseconds(1_000) },
        )
        return TestComponents(
            database = db,
            repository = repository,
            modules = listOf(
                curriculumDataModule,
                assessmentDataModule,
                topicStudyPresentationModule,
                module {
                    single<CurriculumDatabase> { db }
                    single<CurriculumRepository> { curriculum }
                    single<AssessmentRepository> { repository }
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
        val modules: List<org.koin.core.module.Module>,
    )

    private fun fixture() = Curriculum(
        topics = listOf(Topic("topic_android", "Android")),
        subtopics = listOf(
            Subtopic("subtopic_core", "topic_android", "Core"),
            Subtopic("subtopic_empty", "topic_android", "Empty"),
        ),
        questions = listOf(
            question("question_single", "Single question", listOf("A"), "Single explanation", "Single source"),
            question("question_multiple", "Multiple question", listOf("A", "C"), "Multiple explanation", "Multiple source"),
        ),
    )

    private fun question(id: String, text: String, correct: List<String>, explanation: String, sourceTitle: String) = Question(
        id = id,
        topicId = "topic_android",
        subtopicId = "subtopic_core",
        text = text,
        answers = listOf(AnswerOption("A", "A"), AnswerOption("B", "B"), AnswerOption("C", "C")),
        correctAnswerIds = correct,
        explanation = explanation,
        sources = listOf(SourceReference(sourceTitle, "https://example.com/$id")),
        status = ContentStatus.ACTIVE,
    )
}

internal val appIntegrationMainDispatcherLock = Any()
