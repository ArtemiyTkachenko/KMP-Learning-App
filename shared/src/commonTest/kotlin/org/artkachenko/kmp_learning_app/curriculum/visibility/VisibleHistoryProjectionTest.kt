package org.artkachenko.kmp_learning_app.curriculum.visibility

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.history.VisibleHistoryProjection
import org.artkachenko.kmp_learning_app.curriculum.visibility.VisibilityFixture.android
import org.artkachenko.kmp_learning_app.curriculum.visibility.VisibilityFixture.kmp

internal class VisibleHistoryProjectionTest {

    private val curriculum = FixtureCurriculumRepository()
    private val hidden = CurriculumVisibility.from(includeKmpContent = false)
    private val shown = CurriculumVisibility.from(includeKmpContent = true)

    private suspend fun project(attempts: List<TestAttempt>, visibility: CurriculumVisibility) =
        VisibleHistoryProjection.visibleAttempts(attempts, visibility, curriculum)

    @Test
    fun aCoreOnlyAttemptIsTheSameAttemptEitherWay() = runTest {
        val core = completedAttempt("core", 100, "a1" to true, "c1" to false)

        assertSame(core, project(listOf(core), hidden).single())
        assertSame(core, project(listOf(core), shown).single())
    }

    @Test
    fun aKmpOnlyAttemptIsDroppedWhenHiddenAndReturnsWhenShown() = runTest {
        val kmpOnly = completedAttempt("kmp_only", 100, "k1" to false, "k2" to true, config = focusedOn(kmp, 2))

        assertEquals(emptyList(), project(listOf(kmpOnly), hidden))
        assertSame(kmpOnly, project(listOf(kmpOnly), shown).single())
    }

    /** Audit decision D-2 at the data level: Android correct + KMP wrong is 1/2, or 1/1 when hidden. */
    @Test
    fun aMixedAttemptIsTrimmedToItsVisibleAnswersWithARecomputedScore() = runTest {
        val mixed = completedAttempt("mixed", 100, "a1" to true, "k1" to false)
        assertEquals(50.0, mixed.score!!.percentage)

        val visible = project(listOf(mixed), hidden).single()

        assertEquals(listOf(mixed.questionAttempts.first()), visible.questionAttempts)
        assertEquals(AssessmentScore(totalQuestions = 1, correctAnswers = 1), visible.score)
        assertEquals(100.0, visible.score!!.percentage)
        assertEquals(mixed.id, visible.id)
        assertEquals(mixed.config, visible.config)
        assertEquals(mixed.status, visible.status)
        assertEquals(mixed.startedAt, visible.startedAt)
        assertEquals(mixed.completedAt, visible.completedAt)

        assertSame(mixed, project(listOf(mixed), shown).single())
    }

    @Test
    fun trimmingKeepsTheRemainingAnswersInTheirOriginalOrder() = runTest {
        val mixed = completedAttempt(
            "mixed", 100,
            "a3" to false, "k1" to true, "c1" to true, "k2" to false, "a1" to true,
        )

        val visible = project(listOf(mixed), hidden).single()

        assertEquals(listOf("a3", "c1", "a1"), visible.questionAttempts.map { it.questionId })
        assertEquals(AssessmentScore(totalQuestions = 3, correctAnswers = 2), visible.score)
    }

    @Test
    fun droppingAnAttemptLeavesTheOthersNewestFirst() = runTest {
        val history = listOf(
            completedAttempt("newest", 400, "a1" to true),
            completedAttempt("kmp_only", 300, "k1" to false),
            completedAttempt("mixed", 200, "k2" to false, "c1" to true),
            completedAttempt("oldest", 100, "a2" to false),
        )

        assertEquals(listOf("newest", "mixed", "oldest"), project(history, hidden).map { it.id })
        assertEquals(history, project(history, shown))
    }

    /**
     * The score counts persisted correctness. `k1`'s answer key says `k1_a` is right, but the
     * stored answer chose `k1_a` and was recorded incorrect — as if the key has since been
     * corrected — and the projection must keep the recorded outcome.
     */
    @Test
    fun theRecomputedScoreUsesPersistedCorrectnessNotTheCurrentAnswerKey() = runTest {
        val stored = completedAttempt("mixed", 100, "a1" to true, "a2" to true, "k1" to true)
        val a2RecordedWrongDespiteMatchingTheKey = stored.questionAttempts[1].copy(
            answerState = org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState.Answered(
                selectedAnswerIds = setOf("a2_a"),
                isCorrect = false,
            ),
        )
        val attempt = stored.copy(
            questionAttempts = listOf(stored.questionAttempts[0], a2RecordedWrongDespiteMatchingTheKey, stored.questionAttempts[2]),
            score = AssessmentScore(totalQuestions = 3, correctAnswers = 2),
        )

        val visible = project(listOf(attempt), hidden).single()

        assertEquals(AssessmentScore(totalQuestions = 2, correctAnswers = 1), visible.score)
    }

    @Test
    fun theWholeHistoryIsResolvedInOneBatchedRead() = runTest {
        val history = listOf(
            completedAttempt("one", 300, "a1" to true, "k1" to false, "c1" to true),
            completedAttempt("two", 200, "a1" to false, "k2" to true),
            completedAttempt("three", 100, "a2" to true, "k3" to true, "k4" to false),
        )

        project(history, hidden)

        assertEquals(1, curriculum.questionsByIdsCalls.size)
        assertEquals(setOf("a1", "a2", "c1", "k1", "k2", "k3", "k4"), curriculum.questionsByIdsCalls.single())
    }

    @Test
    fun nothingIsResolvedWhenNothingIsHidden() = runTest {
        project(listOf(completedAttempt("mixed", 100, "a1" to true, "k1" to false)), shown)

        assertTrue(curriculum.questionsByIdsCalls.isEmpty())
    }

    /**
     * An ID the curriculum no longer resolves is not known to be hidden, so it stays: visibility
     * must not turn a metadata gap into lost history. The attempt is otherwise trimmed as usual.
     */
    @Test
    fun anUnresolvableQuestionIsKeptRatherThanTreatedAsHidden() = runTest {
        val attempt = completedAttempt("mixed", 100, "retired_q" to false, "k1" to true, "a1" to true)

        val visible = project(listOf(attempt), hidden).single()

        assertEquals(listOf("retired_q", "a1"), visible.questionAttempts.map { it.questionId })
        assertEquals(AssessmentScore(totalQuestions = 2, correctAnswers = 1), visible.score)
        assertTrue(project(listOf(completedAttempt("gone", 100, "retired_q" to true)), hidden).isNotEmpty())
    }

    /** Classification is by the Question's *current* Topic, the attribution progress uses. */
    @Test
    fun aQuestionIsClassifiedByTheTopicItLivesInNow() = runTest {
        val rehomed = FixtureCurriculumRepository(
            questions = VisibilityFixture.questions.map {
                if (it.id == "a1") it.copy(topicId = kmp.id, subtopicId = VisibilityFixture.kmpExpectActual.id) else it
            },
        )
        val attempt = completedAttempt("mixed", 100, "a1" to true, "a2" to true)

        val visible = VisibleHistoryProjection.visibleAttempts(listOf(attempt), hidden, rehomed).single()

        assertEquals(listOf("a2"), visible.questionAttempts.map { it.questionId })
        assertTrue(hidden.isTopicVisible(android.id))
    }
}
