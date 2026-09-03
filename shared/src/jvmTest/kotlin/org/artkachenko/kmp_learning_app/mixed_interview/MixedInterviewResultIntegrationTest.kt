package org.artkachenko.kmp_learning_app.mixed_interview

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeResult
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeService
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentSessionLoadResult
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentSessionLoader
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Curriculum
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.data.local.assessment.AssessmentAttemptStore
import org.artkachenko.kmp_learning_app.data.local.assessment.repository.LocalAssessmentRepository
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImportResult
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import org.artkachenko.kmp_learning_app.data.local.curriculum.repository.LocalCurriculumRepository
import org.artkachenko.kmp_learning_app.data.local.saved_questions.repository.LocalSavedQuestionRepository
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionStateHolder

@OptIn(ExperimentalCoroutinesApi::class)
internal class MixedInterviewResultIntegrationTest {
    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun completedAttemptCanCreateAndReconstructIndependentMixedRetake() = runTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val database = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        val savedQuestionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            assertEquals(
                CurriculumImportResult.Imported,
                CurriculumImporter(database, loadCurriculum = { curriculum() }).importCurriculum(),
            )
            val curriculumRepository = LocalCurriculumRepository(database)
            val assessmentRepository = LocalAssessmentRepository(AssessmentAttemptStore(database))
            val source = completedAttempt()
            assessmentRepository.save(source)
            val retakeService = AssessmentRetakeService(
                assessmentRepository = assessmentRepository,
                assessmentEngine = AssessmentEngine(
                    questionSelector = AssessmentQuestionSelector(
                        curriculumRepository = curriculumRepository,
                        completedHistory = { assessmentRepository.getCompletedAttempts() },
                        randomize = { it },
                    ),
                    generateAttemptId = { "mixed-retake" },
                    now = { Instant.fromEpochMilliseconds(3) },
                ),
            )

            val persisted = assessmentRepository.getById("mixed-result")
            assertEquals(AssessmentScore(2, 1), persisted?.score)

            val viewModel = MixedInterviewResultViewModel(
                attemptId = "mixed-result",
                assessmentRepository = assessmentRepository,
                curriculumRepository = curriculumRepository,
                assessmentReviewLoader = AssessmentReviewLoader(curriculumRepository),
                assessmentRetakeService = retakeService,
                // The production graph's saved-state holder, on the same database as the result.
                savedQuestionStateHolder = SavedQuestionStateHolder(
                    LocalSavedQuestionRepository(database),
                    savedQuestionScope,
                ),
            )
            val state = assertIs<MixedInterviewResultUiState.Content>(
                withContext(Dispatchers.Default) {
                    withTimeout(5_000) {
                        viewModel.uiState.first { it !is MixedInterviewResultUiState.Loading }
                    }
                },
            )
            assertEquals(listOf("Active Topic", "Retired Topic"), state.topicPerformance.map { it.topicName })
            assertEquals(listOf(1, 0), state.topicPerformance.map { it.correctCount })
            assertEquals(2, state.topicPerformance.sumOf { it.questionCount })

            val created = assertIs<AssessmentRetakeResult.Created>(
                retakeService.createRetake(source.id),
            ).session.attempt
            assertNotEquals(source.id, created.id)
            assertEquals(source.config, created.config)
            assertEquals(AssessmentStatus.IN_PROGRESS, created.status)
            assertTrue(created.questionAttempts.all {
                it.answerState == QuestionAnswerState.Unanswered
            })
            assertNull(created.score)
            assertNull(created.completedAt)
            assertEquals(source, assessmentRepository.getById(source.id))
            assertEquals(created, assessmentRepository.getById(created.id))

            val loaded = assertIs<AssessmentSessionLoadResult.Loaded>(
                AssessmentSessionLoader(assessmentRepository, curriculumRepository).load(created.id),
            ).session
            assertEquals(created, loaded.attempt)
            assertEquals(created.questionAttempts.map { it.questionId }, loaded.questions.map { it.id })
        } finally {
            savedQuestionScope.cancel()
            database.close()
        }
    }

    private fun completedAttempt() = TestAttempt(
        id = "mixed-result",
        config = AssessmentConfig.Mixed(2),
        questionAttempts = listOf(
            QuestionAttempt("active-q", QuestionAnswerState.Answered(setOf("a"), true)),
            QuestionAttempt("retired-q", QuestionAnswerState.Answered(setOf("b"), false)),
        ),
        status = AssessmentStatus.COMPLETED,
        startedAt = Instant.fromEpochMilliseconds(1),
        completedAt = Instant.fromEpochMilliseconds(2),
        score = AssessmentScore(2, 1),
    )

    private fun curriculum() = Curriculum(
        topics = listOf(
            Topic("active", "Active Topic"),
            Topic("retired", "Retired Topic", ContentStatus.DEPRECATED),
        ),
        subtopics = listOf(
            Subtopic("active-sub", "active", "Active Subtopic"),
            Subtopic("retired-sub", "retired", "Retired Subtopic", ContentStatus.DEPRECATED),
        ),
        questions = listOf(
            question("active-q", "active", "active-sub"),
            question(
                "retired-q",
                "retired",
                "retired-sub",
                ContentStatus.DEPRECATED,
            ),
        ),
    )

    private fun question(
        id: String,
        topicId: String,
        subtopicId: String,
        status: ContentStatus = ContentStatus.ACTIVE,
    ) = Question(
        id = id,
        topicId = topicId,
        subtopicId = subtopicId,
        text = "Question $id",
        answers = listOf(
            AnswerOption("a", "Answer A"),
            AnswerOption("b", "Answer B"),
        ),
        selectionMode = AnswerSelectionMode.SINGLE,
        level = QuestionLevel.FOUNDATION,
        correctAnswerIds = listOf("a"),
        explanation = "Explanation $id",
        sources = listOf(SourceReference("Source", "https://example.com/$id")),
        status = status,
    )
}
