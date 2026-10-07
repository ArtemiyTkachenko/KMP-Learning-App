package org.artkachenko.kmp_learning_app.progress_reset

import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistory
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.assessment.history.testCacheScope
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.lesson_study.StudiedLesson
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressState
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressStateHolder
import org.artkachenko.kmp_learning_app.lesson_study.repository.LessonStudyRepository

/**
 * Reset as one operation: the delete and the invalidation of both app-scoped caches derived from
 * what it deletes. Stated over the real [AssessmentHistoryStore] and [StudyProgressStateHolder], so
 * "invalidated" is observed as what it is — the caches reading the emptied tables back.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ResetLearnerProgressTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun resetDeletesProgressAndBothCachesReadItBackEmpty() = runResetTest {
        val tables = FakeProgressTables()
        val (history, study) = loadedCaches(tables)

        resetLearnerProgress(tables, history, study)()
        advanceUntilIdle()

        assertEquals(1, tables.deletes)
        assertEquals(AssessmentHistory.Loaded(emptyList()), history.history.first())
        assertEquals(emptySet(), assertIs<StudyProgressState.Loaded>(study.state.value).studiedLessonIds)
    }

    /** A failed delete propagates for the dialog to report, and the data — and every cache — is what it was. */
    @Test
    fun aFailedDeletePropagatesAndLeavesProgressAsItWas() = runResetTest {
        val tables = FakeProgressTables()
        tables.deleteFailure = IllegalStateException("Database unavailable")
        val (history, study) = loadedCaches(tables)

        assertFailsWith<IllegalStateException> { resetLearnerProgress(tables, history, study)() }
        advanceUntilIdle()

        assertEquals(AssessmentHistory.Loaded(listOf(CompletedAttempt)), history.history.first())
        assertEquals(setOf("lesson_a"), assertIs<StudyProgressState.Loaded>(study.state.value).studiedLessonIds)
    }

    /**
     * A failed delete committed nothing, so it invalidates nothing: a database failing the delete is
     * likely to fail the re-read too, and the study holder answers that with `Error` — which would
     * take every studied mark off the Learn surfaces over data that is intact.
     */
    @Test
    fun aFailedDeleteLeavesStudiedMarksShownEvenWhenTheTablesAreUnreadable() = runResetTest {
        val tables = FakeProgressTables()
        val (history, study) = loadedCaches(tables)
        tables.deleteFailure = IllegalStateException("Database unavailable")
        tables.failReads = true

        assertFailsWith<IllegalStateException> { resetLearnerProgress(tables, history, study)() }
        advanceUntilIdle()

        assertEquals(setOf("lesson_a"), assertIs<StudyProgressState.Loaded>(study.state.value).studiedLessonIds)
        assertEquals(AssessmentHistory.Loaded(listOf(CompletedAttempt)), history.history.first())
    }

    /**
     * The regression `CompleteAssessment` guards against, for reset: the delete commits, and the
     * caller is cancelled at the resumption after it. Without the invalidation in `finally`, every
     * surface would keep showing the deleted history for the rest of the process.
     */
    @Test
    fun aDeleteThatCommitsBeforeCancellationStillInvalidatesBothCaches() = runResetTest {
        val tables = FakeProgressTables()
        val (history, study) = loadedCaches(tables)
        val reset = async {
            tables.afterDelete = {
                requireNotNull(coroutineContext[Job]).cancel()
                coroutineContext.ensureActive()
            }
            resetLearnerProgress(tables, history, study)()
        }
        advanceUntilIdle()

        assertEquals(1, tables.deletes, "The delete is expected to have committed before cancellation.")
        assertFailsWith<CancellationException> { reset.await() }
        assertEquals(AssessmentHistory.Loaded(emptyList()), history.history.first())
        assertEquals(emptySet(), assertIs<StudyProgressState.Loaded>(study.state.value).studiedLessonIds)
    }

    private fun TestScope.loadedCaches(
        tables: FakeProgressTables,
    ): Pair<AssessmentHistoryStore, StudyProgressStateHolder> {
        val scope = testCacheScope()
        val history = AssessmentHistoryStore(tables.attempts, scope)
        val study = StudyProgressStateHolder(tables.studiedLessons, scope)
        study.refresh()
        advanceUntilIdle()
        return history to study
    }

    private fun resetLearnerProgress(
        tables: FakeProgressTables,
        history: AssessmentHistoryStore,
        study: StudyProgressStateHolder,
    ) = ResetLearnerProgress(repository = tables, historyStore = history, studyProgress = study)

    private fun runResetTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }
}

/**
 * The attempt and studied-Lesson tables as one fake, because the reset they share is one
 * transaction: [deleteLearnerProgress] empties both or, on [deleteFailure], neither.
 */
private class FakeProgressTables : ProgressResetRepository {
    private var completed = listOf(CompletedAttempt)
    private var studied = listOf(StudiedLesson("lesson_a", 1_000))

    var deletes = 0
        private set
    var deleteFailure: Throwable? = null
    var failReads = false

    /** Runs after the delete is recorded, standing in for what resuming its continuation observes. */
    var afterDelete: suspend () -> Unit = {}

    override suspend fun deleteLearnerProgress() {
        deleteFailure?.let { throw it }
        completed = emptyList()
        studied = emptyList()
        deletes += 1
        afterDelete()
    }

    val attempts = object : AssessmentRepository {
        override suspend fun save(attempt: TestAttempt) = error("A reset never saves an attempt.")
        override suspend fun getById(attemptId: String): TestAttempt? = completed.firstOrNull { it.id == attemptId }
        override suspend fun getCompletedAttempts(): List<TestAttempt> =
            if (failReads) error("Attempts unavailable.") else completed
    }

    val studiedLessons = object : LessonStudyRepository {
        override suspend fun markStudied(lessonId: String) = error("A reset never marks a Lesson.")
        override suspend fun unmarkStudied(lessonId: String) = error("A reset never unmarks one Lesson.")
        override suspend fun isStudied(lessonId: String): Boolean = studied.any { it.lessonId == lessonId }
        override suspend fun getStudiedLessons(): List<StudiedLesson> =
            if (failReads) error("Study state unavailable.") else studied
    }
}

private val CompletedAttempt = TestAttempt(
    id = "completed",
    config = AssessmentConfig.Mixed(questionCount = 1),
    questionAttempts = listOf(
        QuestionAttempt(
            questionId = "question_a",
            answerState = QuestionAnswerState.Answered(setOf("answer_a"), isCorrect = true),
        ),
    ),
    status = AssessmentStatus.COMPLETED,
    startedAt = Instant.fromEpochMilliseconds(1_000),
    completedAt = Instant.fromEpochMilliseconds(2_000),
    score = AssessmentScore(totalQuestions = 1, correctAnswers = 1),
)
