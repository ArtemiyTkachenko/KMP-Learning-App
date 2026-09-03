package org.artkachenko.kmp_learning_app.guided_learning

import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

/**
 * Chooses the one recent learning context a returning learner can usefully be taken back to.
 *
 * This answers "where was I working, and how do I get back there?", which is a different question
 * from [LearningRecommendationPolicy]'s "what should I work on now?". The two surfaces are allowed
 * to disagree, and neither consults the other: this one reads recency, the policy reads mistakes,
 * weakness, and coverage. Nothing here scores, ranks, or explains a recommendation.
 *
 * ## What it reads
 *
 * Completed assessment history and current curriculum, and nothing else. History supplies stable
 * IDs through the persisted `TestAttempt.config`; current curriculum decides whether those IDs
 * still lead anywhere and supplies every name. No "last studied" value is stored anywhere, which is
 * why a rename, a deprecation, or a newly completed attempt changes this answer with no migration
 * and no extra persistence.
 *
 * ## How the context is chosen
 *
 * Completed history is already newest first, so it is walked in that order and the first entry that
 * resolves to a currently usable context wins. It keeps walking rather than giving up at the newest
 * entry: a single stale attempt at the top of history would otherwise hide a perfectly good context
 * just behind it. Mixed runs are skipped — see [toRecentStudyContext] for why a Mixed attempt may
 * not have a Topic inferred from its Questions — leaving the Interview area reachable where it
 * already is, in the navigation bar.
 *
 * ## Where it sends the learner
 *
 * An untargeted (`ALL`) run returns to the *content* it was configured from, because reopening an
 * assessment is a heavier answer than the learner asked for by tapping a shortcut. A targeted run —
 * unseen, weak areas, or unresolved mistakes — returns to the Practice Builder carrying that intent
 * as a preset, so the learner sees the setup and can edit it before anything starts. Those three
 * sources are derived from history at selection time and will have moved on since the attempt was
 * completed; that is the point of reopening the builder rather than restoring a run, and the
 * builder's normal preflight reports honestly when nothing is left to ask.
 */
internal class ContinueStudyingResolver(
    private val curriculumRepository: CurriculumRepository,
) {
    /**
     * @param completedAttempts completed history, newest first, as `AssessmentHistoryStore` and
     * `AssessmentRepository.getCompletedAttempts` both provide it. This deliberately does not
     * re-sort: attempt ordering is the repository's contract, not this policy's opinion.
     */
    suspend fun resolve(completedAttempts: List<TestAttempt>): ContinueStudyingContext? {
        for (attempt in completedAttempts) {
            val focused = attempt.focusedStudyConfig() ?: continue
            focused.toContinueStudyingContext()?.let { return it }
        }
        return null
    }

    private suspend fun AssessmentConfig.Focused.toContinueStudyingContext():
        ContinueStudyingContext? =
        when (val scope = scope) {
            is AssessmentScope.Topic ->
                activeTopic(scope.topicId)?.let { topic -> topicContext(topic, source) }

            is AssessmentScope.Subtopic -> subtopicContext(scope.subtopicId, source)
        }

    private suspend fun subtopicContext(
        subtopicId: String,
        source: PracticeQuestionSource,
    ): ContinueStudyingContext? {
        // A Subtopic the curriculum no longer knows at all cannot even name its parent, so this
        // history entry is unusable and the next older one is inspected instead. Resolution is not
        // limited to ACTIVE content here on purpose: a deprecated Subtopic still records which
        // Topic the learner was working in.
        val subtopic = curriculumRepository.getSubtopicById(subtopicId) ?: return null
        val parentTopic = activeTopic(subtopic.topicId) ?: return null
        // A deprecated Subtopic under a Topic that is still taught degrades to that Topic rather
        // than being dropped: the learner's context is still reachable, only less specific. The
        // same degrading applies to a practice preset, which keeps its source and widens its scope.
        if (subtopic.status != ContentStatus.ACTIVE) return topicContext(parentTopic, source)
        return ContinueStudyingContext(
            target = continueTarget(
                topicId = parentTopic.id,
                subtopicId = subtopicId,
                source = source,
            ),
            scopeName = subtopic.name,
            parentTopicName = parentTopic.name,
        )
    }

    private fun topicContext(
        topic: Topic,
        source: PracticeQuestionSource,
    ): ContinueStudyingContext =
        ContinueStudyingContext(
            target = continueTarget(topicId = topic.id, subtopicId = null, source = source),
            scopeName = topic.name,
        )

    /** A Topic that is missing or no longer taught is not a place to send anyone back to. */
    private suspend fun activeTopic(topicId: String): Topic? =
        curriculumRepository.getTopicById(topicId)?.takeIf { it.status == ContentStatus.ACTIVE }

    private fun continueTarget(
        topicId: String,
        subtopicId: String?,
        source: PracticeQuestionSource,
    ): ContinueStudyingTarget =
        if (source == PracticeQuestionSource.ALL) {
            ContinueStudyingTarget.Topic(topicId = topicId, subtopicId = subtopicId)
        } else {
            ContinueStudyingTarget.Practice(
                PracticePreset(
                    scope = subtopicId?.let(AssessmentScope::Subtopic)
                        ?: AssessmentScope.Topic(topicId),
                    source = source,
                ),
            )
        }
}

/**
 * The completed Focused configuration this attempt was run from, or `null` when it has none.
 *
 * Routed through [toRecentStudyContext] so Continue Studying and the recommendation policy share
 * one definition of recent study: the `IN_PROGRESS` refusal and the "Mixed identifies no Topic"
 * rule are stated once, there. Only the narrowing back to the persisted configuration happens here,
 * because Continue Studying additionally needs the practice source — which [RecentStudyContext]
 * deliberately does not carry, since the policy's coverage tie-break has no use for it.
 */
private fun TestAttempt.focusedStudyConfig(): AssessmentConfig.Focused? =
    when (toRecentStudyContext()) {
        is RecentStudyContext.Focused -> config as? AssessmentConfig.Focused
        RecentStudyContext.Mixed, null -> null
    }
