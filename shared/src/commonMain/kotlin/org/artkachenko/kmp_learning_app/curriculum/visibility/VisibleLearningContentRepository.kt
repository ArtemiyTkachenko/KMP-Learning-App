package org.artkachenko.kmp_learning_app.curriculum.visibility

import kotlinx.coroutines.flow.StateFlow
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningLesson
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository

/**
 * The application's [LearningContentRepository]: bundled learning content as the learner's current
 * [CurriculumVisibility] allows them to see it.
 *
 * The same split as [VisibleCurriculumRepository]. The two ACTIVE reads drop Units whose home Topic
 * is hidden, keeping authored order, so Continue Learning, availability badges and the mistake →
 * Lesson link never offer hidden material. [getUnitById] and [getLessonById] resolve identity and
 * pass through, so a stored study record or a restored route still names something real.
 *
 * Filtering by the Unit's home Topic is enough: the content-boundary invariants guarantee that a
 * visible Unit's Lessons never practise or link to a hidden Topic's material.
 */
internal class VisibleLearningContentRepository(
    private val delegate: LearningContentRepository,
    private val visibility: StateFlow<CurriculumVisibility>,
) : LearningContentRepository {

    override suspend fun getActiveUnits(): List<LearningUnit> {
        val current = visibility.value
        return delegate.getActiveUnits().filter { current.isTopicVisible(it.topicId) }
    }

    override suspend fun getActiveUnitsByTopic(topicId: String): List<LearningUnit> {
        val current = visibility.value
        if (!current.isTopicVisible(topicId)) return emptyList()
        return delegate.getActiveUnitsByTopic(topicId).filter { current.isTopicVisible(it.topicId) }
    }

    override suspend fun getUnitById(unitId: String): LearningUnit? =
        delegate.getUnitById(unitId)

    override suspend fun getLessonById(lessonId: String): LearningLesson? =
        delegate.getLessonById(lessonId)
}
