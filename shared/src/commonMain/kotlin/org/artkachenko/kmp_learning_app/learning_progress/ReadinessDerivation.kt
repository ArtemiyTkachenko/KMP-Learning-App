package org.artkachenko.kmp_learning_app.learning_progress

import kotlin.time.Instant
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.history.MistakeScheduleDerivation
import org.artkachenko.kmp_learning_app.assessment.history.QuestionExposure
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel

/**
 * The single definition of a Topic's readiness for a [ReadinessTarget].
 *
 * For each Topic with ACTIVE Questions at the target's levels:
 *
 * - **coverage** is the share of those Questions the learner has seen, each once, correct or not —
 *   [QuestionExposure], the definition Progress coverage and unseen practice already share;
 * - **recent accuracy** reads the newest [ReadinessPolicy.RecentWindow] answers in the Topic at the
 *   target levels, from persisted correctness;
 * - **due mistakes** are [MistakeScheduleDerivation] entries in the Topic at the target levels that
 *   are due at `now`.
 *
 * Below [ReadinessPolicy.MinimumAnsweredForVerdict] target-level answers a Topic is
 * [ReadinessStatus.NOT_ENOUGH_EVIDENCE]; otherwise it is [ReadinessStatus.READY] only when all three
 * conditions hold, and [ReadinessStatus.GETTING_THERE] when any does not. A Topic with no ACTIVE
 * Question at the target's levels is omitted: there is nothing to be ready for.
 *
 * Pure: the output depends only on the arguments, and the current time enters only as `now`.
 * Neither input knows about curriculum visibility, and neither does this: the caller passes the
 * visible history projection and the visible ACTIVE bank, so a hidden Topic has no Questions to
 * measure and no answers to count. Nothing is persisted.
 */
internal object ReadinessDerivation {
    /**
     * @param completedAttempts completed history in any order; non-completed attempts are ignored.
     * @param activeQuestions the current ACTIVE bank, every level, which decides which Topics are
     *   measured and what coverage is out of.
     * @param historicalQuestions history's Question IDs resolved through the historical resolver,
     *   so a DEPRECATED Question keeps attributing its answers to its Topic and level. An ID absent
     *   from it is skipped, exactly as performance derivation skips it.
     */
    fun derive(
        completedAttempts: List<TestAttempt>,
        activeQuestions: List<Question>,
        historicalQuestions: Map<String, Question>,
        target: ReadinessTarget,
        now: Instant,
    ): ReadinessSnapshot {
        val completed = completedAttempts.filter { it.status == AssessmentStatus.COMPLETED }
        val exposure = QuestionExposure.observedQuestionIds(completed)
        val answersByTopic = targetLevelAnswersNewestFirst(completed, historicalQuestions, target)
        val dueMistakesByTopic = MistakeScheduleDerivation.derive(completed)
            .filter { it.isDue(now) }
            .mapNotNull { historicalQuestions[it.questionId] }
            .filter { it.level in target.levels }
            .groupingBy(Question::topicId)
            .eachCount()

        val topics = activeQuestions
            .groupBy(Question::topicId)
            .entries
            // Stable-ID order, as Progress's Topic table and coverage use, so the domain output is
            // deterministic without depending on repository row order.
            .sortedBy { it.key }
            .mapNotNull { (topicId, topicQuestions) ->
                val targetQuestions = topicQuestions.filter { it.level in target.levels }
                if (targetQuestions.isEmpty()) return@mapNotNull null
                topicReadiness(
                    topicId = topicId,
                    targetQuestions = targetQuestions,
                    advancedUnavailable = QuestionLevel.ADVANCED in target.levels &&
                        topicQuestions.none { it.level == QuestionLevel.ADVANCED },
                    exposure = exposure,
                    answersNewestFirst = answersByTopic[topicId].orEmpty(),
                    dueMistakeCount = dueMistakesByTopic[topicId] ?: 0,
                )
            }

        return ReadinessSnapshot(target = target, topics = topics)
    }

    private fun topicReadiness(
        topicId: String,
        targetQuestions: List<Question>,
        advancedUnavailable: Boolean,
        exposure: Set<String>,
        answersNewestFirst: List<Boolean>,
        dueMistakeCount: Int,
    ): TopicReadiness {
        val covered = targetQuestions.count { it.id in exposure }
        val recent = answersNewestFirst.take(ReadinessPolicy.RecentWindow)
        val recentCorrect = recent.count { it }
        val answered = answersNewestFirst.size
        val hasDueMistakes = dueMistakeCount > ReadinessPolicy.MaximumDueMistakesForReady

        val gaps = buildList {
            if (answered < ReadinessPolicy.MinimumAnsweredForVerdict) {
                add(ReadinessGap.MoreAnswersNeeded(ReadinessPolicy.MinimumAnsweredForVerdict - answered))
                // A due mistake is a fact about a Question, not a judgement on a few answers, so it
                // is worth acting on before the minimum is reached. Accuracy and coverage are not
                // stated below the minimum: that would be a verdict on evidence too thin to carry one.
                if (hasDueMistakes) add(ReadinessGap.DueMistakes(dueMistakeCount))
                return@buildList
            }
            if (hasDueMistakes) add(ReadinessGap.DueMistakes(dueMistakeCount))
            if (!reaches(recentCorrect, recent.size, ReadinessPolicy.ReadyRecentAccuracy)) {
                add(
                    ReadinessGap.RecentAccuracyBelow(
                        percentage = recentCorrect.toDouble() / recent.size * 100.0,
                        required = ReadinessPolicy.ReadyRecentAccuracy,
                    ),
                )
            }
            if (!reaches(covered, targetQuestions.size, ReadinessPolicy.ReadyCoverage)) {
                add(
                    ReadinessGap.CoverageBelow(
                        percentage = covered.toDouble() / targetQuestions.size * 100.0,
                        required = ReadinessPolicy.ReadyCoverage,
                    ),
                )
            }
        }

        return TopicReadiness(
            topicId = topicId,
            status = when {
                answered < ReadinessPolicy.MinimumAnsweredForVerdict -> ReadinessStatus.NOT_ENOUGH_EVIDENCE
                gaps.isEmpty() -> ReadinessStatus.READY
                else -> ReadinessStatus.GETTING_THERE
            },
            answeredCount = answered,
            coveredQuestionCount = covered,
            targetQuestionCount = targetQuestions.size,
            recentAnsweredCount = recent.size,
            recentCorrectCount = recentCorrect,
            dueMistakeCount = dueMistakeCount,
            advancedUnavailable = advancedUnavailable,
            gaps = gaps,
        )
    }

    /**
     * Every target-level answer's persisted correctness, grouped by Topic, newest first.
     *
     * Attempts are ordered by [RecentPerformancePolicy.newestFirst], the persistence order, rather
     * than trusted from the caller. `QuestionAttempt` records no answer time, so within one attempt
     * the later position in the assessment is taken as the newer answer.
     *
     * Topic and level come from the Question's current record through the historical resolver, the
     * attribution performance derivation uses, so a DEPRECATED Question's answers still count here
     * while the Question itself has left coverage.
     */
    private fun targetLevelAnswersNewestFirst(
        completed: List<TestAttempt>,
        historicalQuestions: Map<String, Question>,
        target: ReadinessTarget,
    ): Map<String, List<Boolean>> {
        val answers = mutableMapOf<String, MutableList<Boolean>>()
        for (attempt in completed.sortedWith(RecentPerformancePolicy.newestFirst)) {
            for (questionAttempt in attempt.questionAttempts.asReversed()) {
                val question = historicalQuestions[questionAttempt.questionId] ?: continue
                if (question.level !in target.levels) continue
                val isCorrect = (questionAttempt.answerState as QuestionAnswerState.Answered).isCorrect
                answers.getOrPut(question.topicId, ::mutableListOf).add(isCorrect)
            }
        }
        return answers
    }

    /**
     * Whether [part] of [whole] is at least [requiredPercentage] percent, compared exactly: 3 of 5 is
     * 60% and must reach a 60% threshold, which a floating-point division can miss by one ulp.
     */
    private fun reaches(part: Int, whole: Int, requiredPercentage: Double): Boolean =
        part * 100.0 >= requiredPercentage * whole
}
