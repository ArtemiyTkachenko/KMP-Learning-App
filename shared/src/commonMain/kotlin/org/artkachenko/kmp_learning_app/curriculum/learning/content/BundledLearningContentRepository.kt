package org.artkachenko.kmp_learning_app.curriculum.learning.content

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningCurriculum
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningLesson
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository

/**
 * Serves the bundled learning document from memory.
 *
 * The document is static publisher-owned content, so it is decoded and validated once per
 * repository instance and queried from immutable indexes afterwards rather than re-read on
 * every call. The double-checked mutex follows `CurriculumDataInitializer`: concurrent
 * first calls wait for the same load, and the cached field is only assigned once the whole
 * document has passed validation, so a failed load leaves no partial indexes behind and
 * cannot be observed as an empty curriculum.
 */
internal class BundledLearningContentRepository(
    private val loader: LearningContentLoader = LearningContentLoader(),
) : LearningContentRepository {
    private val mutex = Mutex()
    private var content: LoadedLearningContent? = null

    override suspend fun getActiveUnitsByTopic(topicId: String): List<LearningUnit> =
        content().activeUnitsByTopicId[topicId].orEmpty()

    override suspend fun getUnitById(unitId: String): LearningUnit? =
        content().unitsById[unitId]

    override suspend fun getLessonById(lessonId: String): LearningLesson? =
        content().lessonsById[lessonId]

    private suspend fun content(): LoadedLearningContent =
        content ?: mutex.withLock {
            content ?: LoadedLearningContent(loader.load()).also { content = it }
        }
}

/**
 * Query indexes over one validated document.
 *
 * The indexes reference the same domain objects the document holds rather than copies, and
 * `groupBy` keeps authored order inside each Topic — list position is the ordering contract
 * for learning content, so nothing here sorts.
 */
private class LoadedLearningContent(
    learningCurriculum: LearningCurriculum,
) {
    val activeUnitsByTopicId: Map<String, List<LearningUnit>> =
        learningCurriculum.units
            .filter { it.status == ContentStatus.ACTIVE }
            .groupBy { it.topicId }

    val unitsById: Map<String, LearningUnit> =
        learningCurriculum.units.associateBy { it.id }

    val lessonsById: Map<String, LearningLesson> =
        learningCurriculum.units
            .flatMap { it.lessons }
            .associateBy { it.id }
}
