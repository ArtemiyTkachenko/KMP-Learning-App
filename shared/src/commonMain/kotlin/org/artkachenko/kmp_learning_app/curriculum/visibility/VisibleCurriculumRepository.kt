package org.artkachenko.kmp_learning_app.curriculum.visibility

import kotlinx.coroutines.flow.StateFlow
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

/**
 * The application's [CurriculumRepository]: the stored curriculum as the learner's current
 * [CurriculumVisibility] allows them to see it.
 *
 * It applies the repository's own split between eligibility and identity:
 *
 * - Every `getActive*` read drops content whose Topic is hidden, so selection, coverage and
 *   browsing see a smaller curriculum and need no visibility logic of their own.
 * - [getTopicById], [getSubtopicById] and both stable-ID Question reads — the historical
 *   [getQuestionsByIds] and [getQuestionsByIdsForCurrentContent] — pass through unchanged. They
 *   resolve stored references — an attempt's Questions, a saved Question, a focused scope — and a
 *   hidden Topic must stay readable there, or hiding content would make history unreadable rather
 *   than hidden. Whether a destination may *show* a hidden identity is a route decision, not this
 *   one.
 *
 * Every function is written out rather than delegated with `by`: a delegated member added to the
 * interface later would silently bypass the filter, which is the failure the interface's own
 * documentation warns about.
 *
 * The visibility is read per call, not captured, so a change applies to the next read.
 */
internal class VisibleCurriculumRepository(
    private val delegate: CurriculumRepository,
    private val visibility: StateFlow<CurriculumVisibility>,
) : CurriculumRepository {

    override suspend fun getActiveTopics(): List<Topic> {
        val current = visibility.value
        return delegate.getActiveTopics().filter { current.isTopicVisible(it.id) }
    }

    override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> {
        val current = visibility.value
        if (!current.isTopicVisible(topicId)) return emptyList()
        return delegate.getActiveSubtopics(topicId).filter { current.isTopicVisible(it.topicId) }
    }

    override suspend fun getActiveQuestions(): List<Question> =
        visibleQuestions { delegate.getActiveQuestions() }

    override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> =
        visibleQuestions { delegate.getActiveQuestionsByTopic(topicId) }

    override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
        visibleQuestions { delegate.getActiveQuestionsBySubtopic(subtopicId) }

    override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> =
        visibleQuestions { delegate.getActiveQuestionsByLevels(levels) }

    override suspend fun getActiveQuestionsByTopicAndLevels(
        topicId: String,
        levels: Set<QuestionLevel>,
    ): List<Question> =
        visibleQuestions { delegate.getActiveQuestionsByTopicAndLevels(topicId, levels) }

    override suspend fun getActiveQuestionsBySubtopicAndLevels(
        subtopicId: String,
        levels: Set<QuestionLevel>,
    ): List<Question> =
        visibleQuestions { delegate.getActiveQuestionsBySubtopicAndLevels(subtopicId, levels) }

    override suspend fun getTopicById(topicId: String): Topic? =
        delegate.getTopicById(topicId)

    override suspend fun getSubtopicById(subtopicId: String): Subtopic? =
        delegate.getSubtopicById(subtopicId)

    override suspend fun getQuestionsByIds(questionIds: Collection<String>): Map<String, Question> =
        delegate.getQuestionsByIds(questionIds)

    override suspend fun getQuestionsByIdsForCurrentContent(
        questionIds: Collection<String>,
    ): Map<String, Question> =
        delegate.getQuestionsByIdsForCurrentContent(questionIds)

    /**
     * One rule for every Question-returning eligibility read: a Question is visible when its Topic
     * is. A Subtopic-scoped read needs no separate check, because a Question's Subtopic belongs to
     * its Topic — `CurriculumValidator` rejects a curriculum where it does not.
     */
    private inline fun visibleQuestions(read: () -> List<Question>): List<Question> {
        val current = visibility.value
        return read().filter { current.isTopicVisible(it.topicId) }
    }
}
