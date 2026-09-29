package org.artkachenko.kmp_learning_app.assessment.history

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibility

/**
 * Completed assessment history as the learner's current [CurriculumVisibility] allows them to see
 * it — the history every progress, mistake, selection, recommendation and interview derivation reads.
 *
 * It sits downstream of [AssessmentHistoryStore], which stays the authority for reading, caching,
 * invalidating and failing. Nothing here is written back: hiding a Topic removes its answers from
 * this projection only, and making the Topic visible again brings them back from the unchanged
 * stored attempts. See [VisibleHistoryProjection] for the rule itself.
 *
 * The two views — [history] for app-scoped observers and [completedAttempts] for one-shot domain
 * reads — both run [VisibleHistoryProjection.visibleAttempts], so they cannot disagree.
 *
 * @param curriculumRepository used only for its historical resolver,
 *   [CurriculumRepository.getQuestionsByIds], which is deliberately unfiltered: a hidden Question
 *   must still resolve for the projection to know that it is hidden.
 */
internal class VisibleAssessmentHistory(
    private val rawHistory: AssessmentHistoryStore,
    private val curriculumRepository: CurriculumRepository,
    private val visibility: StateFlow<CurriculumVisibility>,
    scope: CoroutineScope,
) : CompletedAssessmentHistory {

    /**
     * The projected history, re-derived whenever the raw history settles a refresh *or* the
     * visibility changes.
     *
     * It keeps [AssessmentHistoryStore.history]'s contract: every settled refresh is re-announced
     * even when it equals the previous one, because consumers derive from this over a curriculum
     * that can fail independently, and a retry must be able to make them derive again. So this is a
     * replaying [SharedFlow], not a `StateFlow` that would swallow an equal emission, and `combine`
     * forwards every upstream emission rather than only distinct ones.
     *
     * A projection that cannot resolve Question metadata publishes [AssessmentHistory.Failed]
     * rather than the unprojected attempts, which could show hidden content, or an older
     * projection, which could belong to a different visibility. Invalidating the raw store is the
     * retry, exactly as it is for an unreadable attempt table.
     */
    val history: SharedFlow<AssessmentHistory> = combine(rawHistory.history, visibility, ::Pair)
        .map { (raw, current) -> project(raw, current) }
        .shareIn(scope, SharingStarted.Eagerly, replay = 1)

    /**
     * The projected completed attempts, newest first, under the visibility current at the call.
     *
     * Waits for and fails with the raw store exactly as [AssessmentHistoryStore.completedAttempts]
     * does, and a failure to resolve Question metadata propagates too: unreadable history is not
     * empty history.
     */
    override suspend fun completedAttempts(): List<TestAttempt> =
        VisibleHistoryProjection.visibleAttempts(
            attempts = rawHistory.completedAttempts(),
            visibility = visibility.value,
            curriculumRepository = curriculumRepository,
        )

    /**
     * Retries the history this projection is derived from. Only the raw store is invalidated; its
     * re-announced refresh is what makes the projection, and everything observing it, derive again.
     */
    fun invalidate() {
        rawHistory.invalidate()
    }

    private suspend fun project(
        raw: AssessmentHistory,
        current: CurriculumVisibility,
    ): AssessmentHistory =
        when (raw) {
            AssessmentHistory.Loading, AssessmentHistory.Failed -> raw
            is AssessmentHistory.Loaded -> try {
                AssessmentHistory.Loaded(
                    VisibleHistoryProjection.visibleAttempts(raw.attempts, current, curriculumRepository),
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                AssessmentHistory.Failed
            }
        }
}

/**
 * The one rule that turns stored completed attempts into visible ones.
 *
 * For each attempt, the Question answers whose Question belongs to a hidden Topic are removed:
 *
 * - an attempt with nothing hidden is returned as the same instance;
 * - an attempt left with no answers is dropped, since a `TestAttempt` cannot be empty and there is
 *   nothing visible to show of it;
 * - an attempt left with some answers keeps its ID, configuration, timestamps, status and the order
 *   of its remaining answers, and receives a score recomputed over them — `TestAttempt` requires
 *   the score to count exactly its answers, and every aggregate sums that score.
 *
 * Correctness is the persisted [QuestionAnswerState.Answered.isCorrect], never a fresh comparison
 * with the Question's current answer key, for the reason progress uses it: a corrected key must not
 * rewrite history.
 *
 * A Question ID that no longer resolves is **kept**. Visibility hides content *known* to belong to a
 * hidden Topic; missing metadata is not evidence of that, and treating it as hidden would turn a
 * curriculum gap into lost history. Downstream derivations already handle unresolved IDs as they
 * always have.
 *
 * The Topic is the Question's *current* one, resolved through the historical resolver in one
 * batched read for the whole history, so a Question re-homed into or out of a hidden Topic is
 * classified by where it lives now — the same attribution performance derivation uses.
 */
internal object VisibleHistoryProjection {

    suspend fun visibleAttempts(
        attempts: List<TestAttempt>,
        visibility: CurriculumVisibility,
        curriculumRepository: CurriculumRepository,
    ): List<TestAttempt> {
        if (visibility.hidesNothing || attempts.isEmpty()) return attempts
        val questionIds = attempts.flatMapTo(mutableSetOf()) { attempt ->
            attempt.questionAttempts.map { it.questionId }
        }
        val questions = curriculumRepository.getQuestionsByIds(questionIds)
        return attempts.mapNotNull { it.visibleOrNull(visibility, questions) }
    }

    private fun TestAttempt.visibleOrNull(
        visibility: CurriculumVisibility,
        questions: Map<String, Question>,
    ): TestAttempt? {
        val visibleAnswers = questionAttempts.filter { questionAttempt ->
            val question = questions[questionAttempt.questionId] ?: return@filter true
            visibility.isTopicVisible(question.topicId)
        }
        return when (visibleAnswers.size) {
            questionAttempts.size -> this
            0 -> null
            else -> copy(
                questionAttempts = visibleAnswers,
                score = score?.let {
                    AssessmentScore(
                        totalQuestions = visibleAnswers.size,
                        correctAnswers = visibleAnswers.count { answer ->
                            (answer.answerState as? QuestionAnswerState.Answered)?.isCorrect == true
                        },
                    )
                },
            )
        }
    }
}
