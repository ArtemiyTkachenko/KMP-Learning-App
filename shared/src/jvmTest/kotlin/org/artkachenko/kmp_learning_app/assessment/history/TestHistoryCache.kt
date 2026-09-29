package org.artkachenko.kmp_learning_app.assessment.history

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibility

/**
 * A stand-in for the app scope the caches run on in production.
 *
 * Bound to `Dispatchers.Main`, which these tests replace with a test dispatcher, so the eagerly
 * shared flows are driven by `advanceUntilIdle()` like the rest of the test body.
 */
internal fun testCacheScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

internal fun testHistoryStore(
    repository: AssessmentRepository,
    scope: CoroutineScope,
): AssessmentHistoryStore = AssessmentHistoryStore(repository, scope)

/**
 * The visible projection over [this] store, on the same [scope], as the application graph builds it.
 *
 * Everything is visible unless a test passes another [visibility], and with nothing hidden the
 * projection never reads [curriculumRepository] — so a screen test that is not about visibility
 * observes exactly the raw store's emissions, one hop further downstream. The default repository
 * fails on any read, which makes that a checked assumption rather than a hope.
 */
internal fun AssessmentHistoryStore.visibleHistory(
    scope: CoroutineScope,
    curriculumRepository: CurriculumRepository = UnreadHistoryCurriculum,
    visibility: StateFlow<CurriculumVisibility> =
        MutableStateFlow(CurriculumVisibility.from(includeKmpContent = true)),
): VisibleAssessmentHistory =
    VisibleAssessmentHistory(
        rawHistory = this,
        curriculumRepository = curriculumRepository,
        visibility = visibility,
        scope = scope,
    )

/** Retained so call sites read the same; the caches share eagerly and need no collector. */
internal fun keepSubscribed(@Suppress("UNUSED_PARAMETER") flow: StateFlow<*>) = Unit

/** A curriculum the projection must not read: nothing is hidden, so nothing needs classifying. */
private object UnreadHistoryCurriculum : CurriculumRepository {
    private fun unread(): Nothing = error("An all-visible history projection must not read the curriculum.")

    override suspend fun getActiveTopics() = unread()
    override suspend fun getActiveSubtopics(topicId: String) = unread()
    override suspend fun getActiveQuestions() = unread()
    override suspend fun getActiveQuestionsByTopic(topicId: String) = unread()
    override suspend fun getActiveQuestionsBySubtopic(subtopicId: String) = unread()
    override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>) = unread()
    override suspend fun getActiveQuestionsByTopicAndLevels(topicId: String, levels: Set<QuestionLevel>) = unread()
    override suspend fun getActiveQuestionsBySubtopicAndLevels(subtopicId: String, levels: Set<QuestionLevel>) = unread()
    override suspend fun getTopicById(topicId: String) = unread()
    override suspend fun getSubtopicById(subtopicId: String) = unread()
    override suspend fun getQuestionsByIds(questionIds: Collection<String>) = unread()
}
