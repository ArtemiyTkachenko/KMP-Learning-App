package org.artkachenko.kmp_learning_app

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
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
import org.artkachenko.kmp_learning_app.curriculum.learning.content.learningContentModule
import org.artkachenko.kmp_learning_app.curriculum.visibility.curriculumVisibilityModule
import org.artkachenko.kmp_learning_app.curriculum.visibility.kmpContentPreferenceTestModule
import org.artkachenko.kmp_learning_app.data.local.assessment.AssessmentAttemptStore
import org.artkachenko.kmp_learning_app.data.local.assessment.assessmentDataModule
import org.artkachenko.kmp_learning_app.data.local.assessment.repository.LocalAssessmentRepository
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.curriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImportResult
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import org.artkachenko.kmp_learning_app.data.local.curriculum.repository.LocalCurriculumRepository
import org.artkachenko.kmp_learning_app.data.local.lesson_study.lessonStudyDataModule
import org.artkachenko.kmp_learning_app.data.local.saved_questions.savedQuestionDataModule
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.learning_progress.ReadinessGap
import org.artkachenko.kmp_learning_app.learning_progress.ReadinessTarget
import org.artkachenko.kmp_learning_app.progress.ProgressContentTag
import org.artkachenko.kmp_learning_app.progress.ProgressReadinessChooserTag
import org.artkachenko.kmp_learning_app.progress.ProgressTopicReadinessTag
import org.artkachenko.kmp_learning_app.progress.progressReadinessGapActionTag
import org.artkachenko.kmp_learning_app.progress.progressReadinessRowTag
import org.artkachenko.kmp_learning_app.progress.progressReadinessTargetTag
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderStartButtonTag
import org.artkachenko.kmp_learning_app.topic_study.topicStudyPresentationModule
import org.koin.compose.KoinApplication
import org.koin.core.context.stopKoin
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module

/**
 * Readiness through the real application: the chooser on Progress, the drill-down's action into
 * the Practice Builder, a practice run answered in the UI, and the dashboard row after it — over
 * the production graph, a real Room database, and the completion invalidation every other
 * Progress figure refreshes on.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
internal class ProgressReadinessIntegrationTest {
    @Test
    fun choosingSeniorThenCompletingPracticeMovesTheTopicToReady() {
        synchronized(appIntegrationMainDispatcherLock) {
            stopKoin()
            Dispatchers.setMain(Dispatchers.Unconfined)
            var database: CurriculumDatabase? = null
            try {
                runComposeUiTest {
                    val components = testComponents()
                    database = components.database
                    setContent {
                        MaterialTheme {
                            KoinApplication(configuration = koinConfiguration { modules(components.modules) }) {
                                App()
                            }
                        }
                    }

                    waitUntil(timeoutMillis = WaitMillis) {
                        onAllNodesWithText("Android").fetchSemanticsNodes().isNotEmpty()
                    }
                    onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.PROGRESS)).performClick()
                    waitForTag(ProgressContentTag)

                    // Nothing is judged until the learner says what they are preparing for.
                    scrollToTag(ProgressReadinessChooserTag)
                    onNodeWithText("What are you preparing for?").assertIsDisplayed()
                    onNodeWithTag(progressReadinessTargetTag(ReadinessTarget.SENIOR)).performClick()

                    val row = progressReadinessRowTag(TopicId)
                    waitForTag(row)
                    scrollToTag(row)
                    onNode(
                        hasTestTag(row) and
                            hasText("Not enough evidence yet", substring = true) and
                            hasText("Answer 4 more questions", substring = true) and
                            hasText("No Advanced questions in this topic yet", substring = true),
                    ).assertIsDisplayed()

                    // The row opens the drill-down, whose missing item opens the existing builder.
                    onNodeWithTag(row).performClick()
                    waitForTag(ProgressTopicReadinessTag)
                    onNodeWithText("Readiness for Senior").assertIsDisplayed()
                    onNodeWithTag(progressReadinessGapActionTag(ReadinessGap.MoreAnswersNeeded(4)))
                        .performScrollTo()
                        .performClick()
                    waitUntil(timeoutMillis = WaitMillis) {
                        onAllNodesWithText("5 questions ready").fetchSemanticsNodes().isNotEmpty()
                    }
                    onNodeWithText("Topic: Android").assertIsDisplayed()
                    onNodeWithTag(PracticeBuilderStartButtonTag).performScrollTo().performClick()

                    // Every Foundation and Applied Question, answered correctly.
                    repeat(QuestionCount) { index ->
                        waitForTag(AssessmentTakingSubmitTag)
                        onNodeWithText("Correct").performClick()
                        onNodeWithTag(AssessmentTakingSubmitTag).performClick()
                        val next = if (index == QuestionCount - 1) "Finish and view results" else "Next question"
                        waitUntil(timeoutMillis = WaitMillis) {
                            onAllNodesWithText(next).fetchSemanticsNodes().isNotEmpty()
                        }
                        onNodeWithText(next).performClick()
                    }
                    waitUntil(timeoutMillis = WaitMillis) {
                        onAllNodesWithText("5 / 5").fetchSemanticsNodes().isNotEmpty()
                    }

                    // Back on the dashboard, with no restart: six answers, all five seen, none wrong.
                    onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.PROGRESS)).performClick()
                    waitForTag(ProgressContentTag)
                    waitUntil(timeoutMillis = WaitMillis) {
                        onAllNodes(hasTestTag(row) and hasText("Ready", substring = true))
                            .fetchSemanticsNodes().isNotEmpty()
                    }
                    scrollToTag(row)
                    onNode(hasTestTag(row) and hasText("Ready", substring = true)).assertIsDisplayed()
                    onNodeWithText("1 of 1 topic ready").assertExists()
                }
            } finally {
                stopKoin()
                database?.close()
                Dispatchers.resetMain()
            }
        }
    }

    private fun ComposeUiTest.waitForTag(tag: String) {
        waitUntil(timeoutMillis = WaitMillis) {
            onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun ComposeUiTest.scrollToTag(tag: String) {
        onNode(hasScrollAction()).performScrollToNode(hasTestTag(tag))
    }

    private suspend fun testComponents(): TestComponents {
        val db = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        assertIs<CurriculumImportResult.Imported>(
            CurriculumImporter(db, loadCurriculum = { fixture() }).importCurriculum(),
        )
        val curriculum = LocalCurriculumRepository(db)
        val repository = LocalAssessmentRepository(AssessmentAttemptStore(db))
        // One earlier correct answer, so Progress has a dashboard to show and the Topic starts below
        // the evidence minimum.
        repository.save(seededAttempt())
        val selector = AssessmentQuestionSelector(
            curriculumRepository = curriculum,
            completedHistory = { repository.getCompletedAttempts() },
            randomize = { it },
        )
        val engine = AssessmentEngine(
            selector,
            generateAttemptId = (1..10).map { "attempt-$it" }.iterator()::next,
            now = { Instant.fromEpochMilliseconds(5_000) },
        )
        return TestComponents(
            database = db,
            modules = listOf(
                curriculumDataModule,
                curriculumVisibilityModule,
                kmpContentPreferenceTestModule(includeKmpContent = false),
                learningContentModule,
                assessmentDataModule,
                savedQuestionDataModule,
                lessonStudyDataModule,
                topicStudyPresentationModule,
                module {
                    single<CurriculumDatabase> { db }
                    single<CurriculumRepository> { curriculum }
                    single<AssessmentRepository> { repository }
                    single { selector }
                    single { engine }
                },
            ),
        )
    }

    private class TestComponents(
        val database: CurriculumDatabase,
        val modules: List<org.koin.core.module.Module>,
    )
}

private const val WaitMillis = 5_000L
private const val TopicId = "topic_android"
private const val QuestionCount = 5

/** Three Foundation and two Applied Questions, and no Advanced one, so Senior carries its note. */
private fun fixture() = Curriculum(
    topics = listOf(Topic(TopicId, "Android")),
    subtopics = listOf(Subtopic("subtopic_core", TopicId, "Core")),
    questions = listOf(
        question("q1", QuestionLevel.FOUNDATION),
        question("q2", QuestionLevel.FOUNDATION),
        question("q3", QuestionLevel.FOUNDATION),
        question("q4", QuestionLevel.APPLIED),
        question("q5", QuestionLevel.APPLIED),
    ),
)

private fun question(id: String, level: QuestionLevel) = Question(
    id = id,
    topicId = TopicId,
    subtopicId = "subtopic_core",
    text = "Question $id",
    answers = listOf(AnswerOption("${id}_right", "Correct"), AnswerOption("${id}_wrong", "Wrong")),
    selectionMode = AnswerSelectionMode.SINGLE,
    level = level,
    correctAnswerIds = listOf("${id}_right"),
    explanation = "Explanation $id",
    sources = listOf(SourceReference("Source $id", "https://example.com/$id")),
    status = ContentStatus.ACTIVE,
)

private fun seededAttempt() = TestAttempt(
    id = "seeded",
    config = AssessmentConfig.Focused(AssessmentScope.Topic(TopicId), 1),
    questionAttempts = listOf(
        QuestionAttempt("q1", QuestionAnswerState.Answered(setOf("q1_right"), isCorrect = true)),
    ),
    status = AssessmentStatus.COMPLETED,
    startedAt = Instant.fromEpochMilliseconds(900),
    completedAt = Instant.fromEpochMilliseconds(1_000),
    score = AssessmentScore(totalQuestions = 1, correctAnswers = 1),
)
