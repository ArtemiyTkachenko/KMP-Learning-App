package org.artkachenko.kmp_learning_app.topic_study

import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningLesson
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningSection
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository

/**
 * Fixtures shared by the Learning Unit and Lesson presentation tests.
 *
 * Both destinations read the same document through the same contract, so building it twice would
 * let the two suites drift apart on what a deprecated Unit or a foreign Lesson looks like.
 */
internal fun testLearningUnit(
    id: String,
    topicId: String = "topic_a",
    lessons: List<LearningLesson> = emptyList(),
    status: ContentStatus = ContentStatus.ACTIVE,
): LearningUnit =
    LearningUnit(
        id = id,
        topicId = topicId,
        title = "Title of $id",
        summary = "Summary of $id",
        lessons = lessons,
        status = status,
    )

internal fun testLearningLesson(
    id: String,
    status: ContentStatus = ContentStatus.ACTIVE,
    sections: List<LearningSection> = emptyList(),
    sources: List<SourceReference> = emptyList(),
    primarySubtopicIds: List<String> = emptyList(),
    supportingSubtopicIds: List<String> = emptyList(),
): LearningLesson =
    LearningLesson(
        id = id,
        title = "Title of $id",
        summary = "Summary of $id",
        primarySubtopicIds = primarySubtopicIds,
        supportingSubtopicIds = supportingSubtopicIds,
        sections = sections,
        relatedLessonIds = emptyList(),
        sources = sources,
        status = status,
    )

/**
 * A learning document held in memory, indexed exactly as `BundledLearningContentRepository` does.
 *
 * [getUnitById] and [getLessonById] resolve whatever the status is, because that is the real
 * contract the presentation layer has to filter on top of — a fake that hid deprecated content
 * would make the ACTIVE checks under test unobservable. [getLessonById] resolves globally for the
 * same reason: the parent-membership rule is only meaningful if a foreign Lesson really is
 * resolvable by ID.
 *
 * [failuresRemaining] fails that many calls before answering, so a Retry can be observed
 * recovering rather than only failing.
 */
internal class FakeLearningContentRepository(
    units: List<LearningUnit> = emptyList(),
    private var failuresRemaining: Int = 0,
) : LearningContentRepository {
    private val allUnits = units
    private val unitsById = units.associateBy(LearningUnit::id)
    private val lessonsById = units.flatMap(LearningUnit::lessons).associateBy(LearningLesson::id)

    var unitReadIds = mutableListOf<String>()
        private set

    var lessonReadIds = mutableListOf<String>()
        private set

    override suspend fun getActiveUnits(): List<LearningUnit> {
        failIfRequested()
        return allUnits.filter { it.status == ContentStatus.ACTIVE }
    }

    override suspend fun getActiveUnitsByTopic(topicId: String): List<LearningUnit> {
        failIfRequested()
        return allUnits.filter { it.topicId == topicId && it.status == ContentStatus.ACTIVE }
    }

    override suspend fun getUnitById(unitId: String): LearningUnit? {
        failIfRequested()
        unitReadIds += unitId
        return unitsById[unitId]
    }

    override suspend fun getLessonById(lessonId: String): LearningLesson? {
        failIfRequested()
        lessonReadIds += lessonId
        return lessonsById[lessonId]
    }

    private fun failIfRequested() {
        if (failuresRemaining > 0) {
            failuresRemaining -= 1
            error("learning content unavailable")
        }
    }
}
