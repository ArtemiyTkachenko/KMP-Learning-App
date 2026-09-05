package org.artkachenko.kmp_learning_app.curriculum.learning.content

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

/**
 * The fixture is authored, not alphabetical: Units are named so that authored order and
 * alphabetical order disagree, which is what makes the ordering assertions meaningful.
 */
internal class BundledLearningContentRepositoryTest {
    @Test
    fun activeUnitsForATopicKeepAuthoredOrderAndExcludeDeprecatedUnits() = runTest {
        val units = repository().getActiveUnitsByTopic("android_ui")

        assertEquals(listOf("unit_b", "unit_a"), units.map { it.id })
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
        val repository = repository(onLoad = { loads++ })

        repository.getActiveUnitsByTopic("android_ui")
        repository.getUnitById("unit_a")
        repository.getLessonById("lesson_side_effects")

        assertEquals(1, loads)
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
        assertFailsWith<LearningContentLoadException> { repository.getActiveUnitsByTopic("unknown_topic") }
        assertFailsWith<LearningContentLoadException> { repository.getUnitById("unit_a") }
        assertFailsWith<LearningContentLoadException> { repository.getLessonById("lesson_side_effects") }
    }

    private fun repository(onLoad: () -> Unit = {}): LearningContentRepository =
        BundledLearningContentRepository(
            loader = LearningContentLoader(
                loadLearningCurriculum = {
                    onLoad()
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
