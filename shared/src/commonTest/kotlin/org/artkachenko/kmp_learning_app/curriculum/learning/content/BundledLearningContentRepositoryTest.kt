package org.artkachenko.kmp_learning_app.curriculum.learning.content

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Curriculum
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningBlock
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningCurriculum
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningDepth
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningLesson
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningSection
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The fixture is authored, not alphabetical: Units are named so that authored order and
 * alphabetical order disagree, which is what makes the ordering assertions meaningful.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class BundledLearningContentRepositoryTest {
    @Test
    fun activeUnitsForATopicKeepAuthoredOrderAndExcludeDeprecatedUnits() = runTest {
        val units = repository().getActiveUnitsByTopic("android_ui")

        assertEquals(listOf("unit_b", "unit_a"), units.map { it.id })
    }

    /**
     * The fixture interleaves Topics — Android UI, coroutines, then Android UI again — so a result
     * that grouped Units by Topic, or sorted them by ID, would differ from the authored sequence.
     * That is the property Continue Learning walks, so it is asserted rather than assumed.
     */
    @Test
    fun everyActiveUnitIsReturnedInGlobalAuthoredOrderAcrossTopics() = runTest {
        val units = repository().getActiveUnits()

        assertEquals(listOf("unit_b", "unit_c", "unit_a"), units.map { it.id })
    }

    @Test
    fun theGlobalActiveUnitListExcludesDeprecatedUnits() = runTest {
        val units = repository().getActiveUnits()

        assertEquals(emptyList(), units.filter { it.status != ContentStatus.ACTIVE })
        assertEquals(null, units.firstOrNull { it.id == "unit_d" })
    }

    @Test
    fun activeUnitsAreScopedToTheirHomeTopic() = runTest {
        val repository = repository()

        assertEquals(listOf("unit_c"), repository.getActiveUnitsByTopic("kotlin_coroutines").map { it.id })
        assertEquals(emptyList(), repository.getActiveUnitsByTopic("unknown_topic").map { it.id })
    }

    @Test
    fun aUnitResolvesByStableIdEvenWhenItIsDeprecated() = runTest {
        val unit = repository().getUnitById("unit_d")

        assertEquals(ContentStatus.DEPRECATED, unit?.status)
    }

    @Test
    fun aLessonResolvesByStableIdFromAnyUnit() = runTest {
        val repository = repository()

        assertEquals("lesson_side_effects", repository.getLessonById("lesson_side_effects")?.id)
        assertEquals("lesson_retired_state", repository.getLessonById("lesson_retired_state")?.id)
    }

    @Test
    fun unknownIdsResolveToNull() = runTest {
        val repository = repository()

        assertNull(repository.getUnitById("unit_missing"))
        assertNull(repository.getLessonById("lesson_missing"))
    }

    @Test
    fun aLessonMayTeachConceptsOwnedByAnotherTopic() = runTest {
        val lesson = repository().getLessonById("lesson_side_effects")

        // The Unit is browsed under Android UI while the Lesson leans on a coroutines
        // concept. Validation accepts that by design, and the repository must not
        // reintroduce a home-Topic restriction of its own.
        assertEquals(listOf("compose_state"), lesson?.primarySubtopicIds)
        assertEquals(listOf("coroutine_scope"), lesson?.supportingSubtopicIds)
    }

    @Test
    fun theDocumentIsLoadedOnceAcrossRepeatedQueries() = runTest {
        var loads = 0
        val repository = repository(
            onLoad = {
                loads++
                null
            },
        )

        repository.getActiveUnits()
        repository.getActiveUnitsByTopic("android_ui")
        repository.getUnitById("unit_a")
        repository.getLessonById("lesson_side_effects")

        assertEquals(1, loads)
    }

    /**
     * The other half of the caching contract, and the one repeated sequential queries cannot show:
     * callers that arrive *before* the first load finishes share it rather than each starting one.
     *
     * This matters because the repository is a `single` whose callers are not on one thread —
     * `MistakeReviewStateHolder` reads it from `AppCoroutineScope` (`Dispatchers.Default`) while the
     * Learn ViewModels read it from `viewModelScope` (`Dispatchers.Main.immediate`) — so two first
     * calls overlapping is a normal thing for this object rather than a contrived one, and the mutex
     * is what makes it one load. Both callers must come back with the same indexes; that they may
     * then read the cached field without the mutex is safe for a separate reason the repository's own
     * documentation gives.
     */
    @Test
    fun concurrentFirstCallsShareOneLoadAndSeeTheSameDocument() = runTest {
        var loads = 0
        val loading = CompletableDeferred<Unit>()
        val repository = repository(
            onLoad = {
                loads++
                // Suspends the first load so the second caller genuinely arrives while it is in
                // flight, rather than after it. No delay is involved: the gate is released below.
                loading
            },
        )

        val first = async { repository.getActiveUnits() }
        val second = async { repository.getUnitById("unit_a") }
        runCurrent()
        assertTrue(first.isActive && second.isActive, "Neither caller should complete before the load does.")

        loading.complete(Unit)

        val units = first.await()
        assertEquals("unit_a", requireNotNull(second.await()).id)
        assertEquals(1, loads, "Overlapping first calls must share one load.")
        // The same index instance, not an equal copy: a second load would have built a second one.
        assertSame(units, repository.getActiveUnits())
    }

    @Test
    fun aFailedLoadStaysAFailureAndCachesNothing() = runTest {
        val repository = BundledLearningContentRepository(
            loader = LearningContentLoader(
                loadLearningCurriculum = {
                    LearningCurriculum(units = listOf(unit(id = "unit_a", topicId = "unknown_topic")))
                },
                loadCurriculum = { baseCurriculum },
            ),
        )

        val failure = assertFailsWith<LearningContentLoadException> {
            repository.getActiveUnitsByTopic("android_ui")
        }
        assertIs<LearningContentLoadFailure.Validation>(failure.failure)

        // No query may quietly succeed with partial content once loading has failed.
        assertFailsWith<LearningContentLoadException> { repository.getActiveUnits() }
        assertFailsWith<LearningContentLoadException> { repository.getActiveUnitsByTopic("unknown_topic") }
        assertFailsWith<LearningContentLoadException> { repository.getUnitById("unit_a") }
        assertFailsWith<LearningContentLoadException> { repository.getLessonById("lesson_side_effects") }
    }

    /**
     * [onLoad] may return a [CompletableDeferred] to hold the load open, which is how the concurrent
     * test above arranges an overlap without a timing assumption.
     */
    private fun repository(
        onLoad: () -> CompletableDeferred<Unit>? = { null },
    ): LearningContentRepository =
        BundledLearningContentRepository(
            loader = LearningContentLoader(
                loadLearningCurriculum = {
                    onLoad()?.await()
                    learningCurriculum
                },
                loadCurriculum = { baseCurriculum },
            ),
        )

    private fun unit(
        id: String,
        topicId: String,
        lessons: List<LearningLesson> = listOf(lesson(id = "${id}_lesson")),
        status: ContentStatus = ContentStatus.ACTIVE,
    ) = LearningUnit(
        id = id,
        topicId = topicId,
        title = "Unit $id",
        summary = "What the learner takes away from unit $id.",
        lessons = lessons,
        status = status,
    )

    private fun lesson(
        id: String,
        primarySubtopicIds: List<String> = listOf("compose_recomposition"),
        supportingSubtopicIds: List<String> = emptyList(),
        status: ContentStatus = ContentStatus.ACTIVE,
    ) = LearningLesson(
        id = id,
        title = "Lesson $id",
        summary = "What the learner takes away from lesson $id.",
        primarySubtopicIds = primarySubtopicIds,
        supportingSubtopicIds = supportingSubtopicIds,
        sections = listOf(
            LearningSection(
                depth = LearningDepth.CORE,
                blocks = listOf(
                    LearningBlock.Paragraph(
                        text = "Compose describes the UI for the current state rather than mutating a view tree.",
                    ),
                ),
            ),
        ),
        relatedLessonIds = emptyList(),
        sources = listOf(
            SourceReference(
                title = "Thinking in Compose",
                url = "https://developer.android.com/develop/ui/compose/mental-model",
            ),
        ),
        status = status,
    )

    private val learningCurriculum = LearningCurriculum(
        units = listOf(
            unit(
                id = "unit_b",
                topicId = "android_ui",
                lessons = listOf(
                    lesson(
                        id = "lesson_side_effects",
                        primarySubtopicIds = listOf("compose_state"),
                        supportingSubtopicIds = listOf("coroutine_scope"),
                    ),
                ),
            ),
            unit(id = "unit_c", topicId = "kotlin_coroutines"),
            unit(
                id = "unit_d",
                topicId = "android_ui",
                lessons = listOf(lesson(id = "lesson_retired_state", status = ContentStatus.DEPRECATED)),
                status = ContentStatus.DEPRECATED,
            ),
            unit(id = "unit_a", topicId = "android_ui"),
        ),
    )

    private val baseCurriculum = Curriculum(
        topics = listOf(
            Topic(id = "android_ui", name = "Android UI"),
            Topic(id = "kotlin_coroutines", name = "Kotlin coroutines"),
        ),
        subtopics = listOf(
            Subtopic(id = "compose_recomposition", topicId = "android_ui", name = "Recomposition"),
            Subtopic(id = "compose_state", topicId = "android_ui", name = "Compose state"),
            Subtopic(id = "coroutine_scope", topicId = "kotlin_coroutines", name = "Coroutine scope"),
        ),
        questions = emptyList(),
    )
}
