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
 *
 * ## Why the plain cached field is safe to read outside the mutex
 *
 * This is a `single`, and its callers are not on one thread: `MistakeReviewStateHolder` reads it
 * from `AppCoroutineScope`, which is `Dispatchers.Default`, while the Learn ViewModels read it from
 * `viewModelScope`, which is `Dispatchers.Main.immediate`. A caller that finds the field already set
 * returns on the fast path without taking the mutex, so that read genuinely races the write — and it
 * is nonetheless safe, because every property of [LoadedLearningContent] is a `val`, which compiles
 * to a `final` field. JLS 17.5 freezes final fields at the end of the constructor and guarantees a
 * thread that obtains the reference only after construction — even through a race — sees their
 * initialised values and everything reachable from them. The reference is assigned after the
 * constructor returns, so there is no partially built document to observe. Kotlin/Native follows the
 * same model, and both web targets are single-threaded.
 *
 * `@Volatile` would therefore add a barrier to every read and buy no correctness. This is recorded
 * rather than left implicit because the shape looks like unsafe publication and invites a fix it
 * does not need; the guarantee ends the moment [LoadedLearningContent] gains a mutable property,
 * which is the one change that should revisit this. `CurriculumDataInitializer` reaches the same
 * safety differently — its flag is published through a `MutableStateFlow` — so the two are not the
 * same argument even though the double-checked shape is.
 */
internal class BundledLearningContentRepository(
    private val loader: LearningContentLoader = LearningContentLoader(),
) : LearningContentRepository {
    private val mutex = Mutex()
    private var content: LoadedLearningContent? = null

    override suspend fun getActiveUnits(): List<LearningUnit> = content().activeUnits

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
    /**
     * The document's own Unit order, ACTIVE only. [activeUnitsByTopicId] is derived from this rather
     * than filtered again, so the two can never disagree about which Units are ACTIVE, and the
     * global list stays the one place authored sequence survives.
     */
    val activeUnits: List<LearningUnit> =
        learningCurriculum.units.filter { it.status == ContentStatus.ACTIVE }

    val activeUnitsByTopicId: Map<String, List<LearningUnit>> =
        activeUnits.groupBy { it.topicId }

    val unitsById: Map<String, LearningUnit> =
        learningCurriculum.units.associateBy { it.id }

    val lessonsById: Map<String, LearningLesson> =
        learningCurriculum.units
            .flatMap { it.lessons }
            .associateBy { it.id }
}
