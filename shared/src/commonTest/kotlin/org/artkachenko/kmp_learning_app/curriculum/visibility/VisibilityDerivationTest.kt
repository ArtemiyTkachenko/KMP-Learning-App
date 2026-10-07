package org.artkachenko.kmp_learning_app.curriculum.visibility

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.assessment.history.VisibleAssessmentHistory
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentSelectionResult
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.curriculum.visibility.VisibilityFixture.android
import org.artkachenko.kmp_learning_app.curriculum.visibility.VisibilityFixture.androidLifecycle
import org.artkachenko.kmp_learning_app.curriculum.visibility.VisibilityFixture.composeState
import org.artkachenko.kmp_learning_app.curriculum.visibility.VisibilityFixture.kmp
import org.artkachenko.kmp_learning_app.curriculum.visibility.VisibilityFixture.kmpExpectActual
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.artkachenko.kmp_learning_app.learning_progress.WeakArea
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewService

/**
 * The domain derivations over the application's visibility seams — the visible curriculum and the
 * visible history projection — with no visibility logic of their own.
 *
 * One representative history, newest first:
 *
 * | Attempt   | Answers                                      | Raw  | KMP hidden      |
 * |-----------|----------------------------------------------|------|-----------------|
 * | mixed     | a1 ✓, k1 ✗, c1 ✓                             | 2/3  | 2/2 (k1 hidden) |
 * | kmpFocus  | k1–k6 ✗                                      | 0/6  | dropped         |
 * | core      | a1 ✗, a2 ✓, a3 ✓, a4 ✓, a5 ✓, c2 ✗            | 4/6  | 4/6             |
 *
 * With KMP hidden: 2 attempts, 8 answers, 6 correct (75%), no weak area, coverage 7/9, two
 * unresolved mistakes (a1, c2). With KMP shown: 3 attempts, 15 answers, 6 correct (40%), one weak
 * area (the `kmp_expect_actual` Subtopic), coverage 13/16, eight unresolved mistakes (a1, c2,
 * k1–k6). `a1`'s correct answer in `mixed` came minutes after its mistake, before the review was
 * due, so it does not move `a1` off the review schedule.
 */
internal class VisibilityDerivationTest {

    private val history = listOf(
        completedAttempt("mixed", 500, "a1" to true, "k1" to false, "c1" to true),
        completedAttempt(
            "kmpFocus", 400,
            "k1" to false, "k2" to false, "k3" to false, "k4" to false, "k5" to false, "k6" to false,
            config = focusedOn(kmp, 6),
        ),
        completedAttempt(
            "core", 300,
            "a1" to false, "a2" to true, "a3" to true, "a4" to true, "a5" to true, "c2" to false,
        ),
    )

    // --- Progress ---------------------------------------------------------------------------

    @Test
    fun hiddenKmpContributesNothingToProgress() = runTest {
        val harness = harness(includeKmpContent = false)

        val snapshot = harness.progress.load()

        assertEquals(2, snapshot.completedAttemptCount)
        assertEquals(8, snapshot.answeredQuestionCount)
        assertEquals(6, snapshot.correctAnswerCount)
        assertEquals(75.0, snapshot.percentage)
        assertEquals(emptyList(), snapshot.weakAreas)
        assertEquals(listOf("android", "compose"), snapshot.topics.map { it.topicId }.sorted())
        assertTrue(snapshot.subtopics.none { it.topicId == kmp.id })
        assertEquals(7, snapshot.coverage.attemptedQuestionCount)
        assertEquals(9, snapshot.coverage.totalQuestionCount)
        assertTrue(snapshot.topicCoverage.none { it.topicId == kmp.id })
        assertEquals(2, snapshot.recentPerformance.attemptSeries.size)
        assertEquals(8, snapshot.recentPerformance.answeredQuestionCount)
        assertTrue(snapshot.recentPerformance.answerSeries.none { it.questionId.startsWith("k") })
    }

    @Test
    fun shownKmpRestoresTheFullHistoryResults() = runTest {
        val harness = harness(includeKmpContent = true)

        val snapshot = harness.progress.load()

        assertEquals(3, snapshot.completedAttemptCount)
        assertEquals(15, snapshot.answeredQuestionCount)
        assertEquals(6, snapshot.correctAnswerCount)
        assertEquals(40.0, snapshot.percentage)
        // The KMP Subtopic is weak, and a weak Subtopic is reported in place of its Topic.
        assertEquals(
            listOf(kmpExpectActual.id),
            snapshot.weakAreas.map { assertIs<WeakArea.Subtopic>(it).performance.subtopicId },
        )
        assertTrue(snapshot.topics.any { it.topicId == kmp.id })
        assertEquals(13, snapshot.coverage.attemptedQuestionCount)
        assertEquals(16, snapshot.coverage.totalQuestionCount)
        assertEquals(3, snapshot.recentPerformance.attemptSeries.size)
        assertEquals(15, snapshot.recentPerformance.answeredQuestionCount)
    }

    /** The mixed attempt is scored over its visible answers, and restored when shown. */
    @Test
    fun theMixedAttemptsRecentScoreIsRecalculatedOverItsVisibleAnswers() = runTest {
        val hidden = harness(includeKmpContent = false).progress.load().recentPerformance.attemptSeries
        val shown = harness(includeKmpContent = true).progress.load().recentPerformance.attemptSeries

        val hiddenMixed = hidden.single { it.attemptId == "mixed" }
        assertEquals(2, hiddenMixed.answeredQuestionCount)
        assertEquals(2, hiddenMixed.correctAnswerCount)
        val shownMixed = shown.single { it.attemptId == "mixed" }
        assertEquals(3, shownMixed.answeredQuestionCount)
        assertEquals(2, shownMixed.correctAnswerCount)
    }

    // --- Service fallbacks ------------------------------------------------------------------

    /**
     * Called without attempts, both services read the projected history they were given — never
     * the raw repository, which would bring every hidden answer back.
     */
    @Test
    fun servicesCalledWithoutAttemptsReadTheProjectedHistory() = runTest {
        val harness = harness(includeKmpContent = false)

        assertEquals(8, harness.progress.load().answeredQuestionCount)
        assertEquals(2, harness.mistakes.countUnresolved())
        assertEquals(listOf("a1", "c2"), harness.mistakes.load().map { it.questionId })
        // One read of the attempt table, by the cache the projection sits on.
        assertEquals(1, harness.raw.completedReads)
    }

    @Test
    fun shownKmpMistakesReturnInTheStateTheirHistoryLeavesThem() = runTest {
        val harness = harness(includeKmpContent = true)

        assertEquals(8, harness.mistakes.countUnresolved())
        assertEquals(
            setOf("a1", "c2", "k1", "k2", "k3", "k4", "k5", "k6"),
            harness.mistakes.load().mapTo(mutableSetOf()) { it.questionId },
        )
    }

    /** A toggle re-projects the cached history: nothing is re-read, and nothing is written. */
    @Test
    fun aVisibilityRoundTripRederivesWithoutReadingOrWritingAttempts() = runTest {
        val harness = harness(includeKmpContent = false)
        assertEquals(2, harness.mistakes.countUnresolved())

        harness.visibility.value = CurriculumVisibility.from(includeKmpContent = true)
        assertEquals(8, harness.mistakes.countUnresolved())
        assertEquals(15, harness.progress.load().answeredQuestionCount)

        harness.visibility.value = CurriculumVisibility.from(includeKmpContent = false)
        assertEquals(2, harness.mistakes.countUnresolved())
        assertEquals(8, harness.progress.load().answeredQuestionCount)

        assertEquals(1, harness.raw.completedReads)
        assertEquals(0, harness.raw.writes)
        assertEquals(history, harness.raw.getCompletedAttempts())
    }

    // --- Selection --------------------------------------------------------------------------

    @Test
    fun hiddenKmpNeverEntersAMixedAssessment() = runTest {
        val selector = harness(includeKmpContent = false).selector

        val all = assertIs<AssessmentSelectionResult.Selected>(selector.select(AssessmentConfig.Mixed(questionCount = 50)))
        assertTrue(all.questions.none { it.topicId == kmp.id })
        assertEquals(9, all.questions.size)

        val round = assertIs<AssessmentSelectionResult.Selected>(selector.select(AssessmentConfig.Mixed(questionCount = 3)))
        assertEquals(listOf("a1", "c1", "a2"), round.questions.map { it.id })
    }

    /** Audit decision D-1: shown, `kmp` is one more Topic in the round-robin, with no weighting. */
    @Test
    fun shownKmpJoinsTheMixedRoundRobinAsOneMoreTopic() = runTest {
        val selector = harness(includeKmpContent = true).selector

        val round = assertIs<AssessmentSelectionResult.Selected>(selector.select(AssessmentConfig.Mixed(questionCount = 3)))

        assertEquals(listOf("android", "kmp", "compose"), round.questions.map { it.topicId })
    }

    @Test
    fun focusedPracticeSeesOnlyItsVisibleScope() = runTest {
        val hidden = harness(includeKmpContent = false).selector
        val core = assertIs<AssessmentSelectionResult.Selected>(hidden.select(focusedOn(android, 10)))
        assertTrue(core.questions.all { it.topicId == android.id })
        assertEquals(
            AssessmentSelectionResult.NoContent.NoEligibleQuestions,
            hidden.select(focusedOn(kmp, 10)),
        )

        val shown = harness(includeKmpContent = true).selector
        val kmpRun = assertIs<AssessmentSelectionResult.Selected>(shown.select(focusedOn(kmp, 10)))
        assertTrue(kmpRun.questions.all { it.topicId == kmp.id })
    }

    /**
     * A scope spanning both sides of the boundary, as a Learning Unit's can: the only weakness in
     * this history is KMP, so hidden there is nothing weak to practise, and shown it is exactly the
     * KMP Subtopic's Questions.
     */
    @Test
    fun weakAreaPracticeCannotSelectAHiddenWeakness() = runTest {
        val config = crossBoundary(PracticeQuestionSource.WEAK_AREAS, androidLifecycle.id, kmpExpectActual.id)

        assertEquals(
            AssessmentSelectionResult.NoContent.NoEligibleQuestions,
            harness(includeKmpContent = false).selector.select(config),
        )
        val shown = assertIs<AssessmentSelectionResult.Selected>(harness(includeKmpContent = true).selector.select(config))
        assertEquals(setOf("k1", "k2", "k3", "k4", "k5", "k6"), shown.questions.mapTo(mutableSetOf()) { it.id })
    }

    @Test
    fun mistakePracticeCannotSelectAHiddenMistake() = runTest {
        val config = crossBoundary(
            PracticeQuestionSource.UNRESOLVED_MISTAKES,
            androidLifecycle.id, composeState.id, kmpExpectActual.id,
        )

        val hidden = assertIs<AssessmentSelectionResult.Selected>(harness(includeKmpContent = false).selector.select(config))
        assertEquals(listOf("a1", "c2"), hidden.questions.map { it.id })
        val shown = assertIs<AssessmentSelectionResult.Selected>(harness(includeKmpContent = true).selector.select(config))
        assertEquals(setOf("a1", "c2", "k1", "k2", "k3", "k4", "k5", "k6"), shown.questions.mapTo(mutableSetOf()) { it.id })
    }

    // --- Harness ----------------------------------------------------------------------------

    private fun crossBoundary(source: PracticeQuestionSource, vararg subtopicIds: String) =
        AssessmentConfig.Focused(
            scope = AssessmentScope.Subtopics(subtopicIds.toSet()),
            questionCount = 20,
            source = source,
        )

    private fun TestScope.harness(includeKmpContent: Boolean) =
        Harness(backgroundScope, history, includeKmpContent)

    /** The application's wiring, over in-memory sources: the same seams, the same services. */
    private class Harness(scope: CoroutineScope, attempts: List<TestAttempt>, includeKmpContent: Boolean) {
        val raw = RecordingAssessmentRepository(attempts)
        val visibility = VisibilityFixture.visibility(includeKmpContent)
        private val curriculum = VisibleCurriculumRepository(FixtureCurriculumRepository(), visibility)
        private val completedHistory = VisibleAssessmentHistory(
            rawHistory = AssessmentHistoryStore(raw, scope),
            curriculumRepository = curriculum,
            visibility = visibility,
            scope = scope,
        )
        val progress = LearningProgressService(completedHistory, curriculum)
        val mistakes = MistakeReviewService(completedHistory, AssessmentReviewLoader(curriculum))
        val selector = AssessmentQuestionSelector(curriculum, completedHistory, randomize = { it })
    }

    private class RecordingAssessmentRepository(private val attempts: List<TestAttempt>) : AssessmentRepository {
        var completedReads = 0
        var writes = 0

        override suspend fun save(attempt: TestAttempt) {
            writes++
        }

        override suspend fun getById(attemptId: String): TestAttempt? = attempts.firstOrNull { it.id == attemptId }

        override suspend fun getCompletedAttempts(): List<TestAttempt> {
            completedReads++
            return attempts
        }
    }
}
