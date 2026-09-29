package org.artkachenko.kmp_learning_app.curriculum.visibility

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.curriculum.learning.content.BundledLearningContentRepository

/**
 * The learning-content decorator over the shipped document, so the two real KMP Units — not a
 * fixture's — are the ones hidden and restored.
 */
internal class VisibleLearningContentRepositoryTest {

    private val bundled = BundledLearningContentRepository()
    private val visibility = VisibilityFixture.visibility(includeKmpContent = false)
    private val repository = VisibleLearningContentRepository(bundled, visibility)

    private val kmpUnitIds = listOf(
        "unit_kmp_shared_viewmodels_and_host_lifecycles",
        "unit_koin_and_dependency_injection_in_kmp",
    )

    @Test
    fun hiddenKmpUnitsLeaveTheActiveReadsAndTheRestKeepAuthoredOrder() = runTest {
        val all = bundled.getActiveUnits()
        assertEquals(kmpUnitIds, all.filter { it.topicId == "kmp" }.map { it.id })

        val visible = repository.getActiveUnits()

        assertTrue(visible.none { it.topicId == "kmp" })
        assertEquals(all.filterNot { it.topicId == "kmp" }, visible)
        assertEquals(emptyList(), repository.getActiveUnitsByTopic("kmp"))
        assertEquals(bundled.getActiveUnitsByTopic("android_ui"), repository.getActiveUnitsByTopic("android_ui"))
    }

    @Test
    fun shownKmpUnitsReturnInAuthoredOrder() = runTest {
        visibility.value = CurriculumVisibility.from(includeKmpContent = true)

        assertEquals(bundled.getActiveUnits(), repository.getActiveUnits())
        assertEquals(kmpUnitIds, repository.getActiveUnitsByTopic("kmp").map { it.id })
    }

    /** A stored study record or a restored route still names something real while hidden. */
    @Test
    fun hiddenUnitsAndLessonsStillResolveByIdentity() = runTest {
        kmpUnitIds.forEach { unitId ->
            val unit = assertNotNull(repository.getUnitById(unitId))
            assertEquals("kmp", unit.topicId)
            unit.lessons.forEach { lesson ->
                assertEquals(lesson, repository.getLessonById(lesson.id))
            }
        }
    }
}
