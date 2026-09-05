package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.curriculum.learning.content.BundledLearningContentRepository
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository

/**
 * The one step E21-02 owns end to end: shipped learning content -> the rows Topic Detail displays.
 *
 * Nothing is faked and nothing is hardcoded — the Unit and its Lesson count are read from the
 * bundled document through the same repository contract the ViewModel uses, so authoring a second
 * Unit for `android_ui` or retiring a Lesson changes this assertion, which is the point.
 *
 * What the document itself says, how it decodes, how it validates, and how the repository caches it
 * are covered by the EPIC-20 suites and are deliberately not repeated here.
 */
internal class TopicDetailLearningContentTest {
    @Test
    fun theProductionAndroidUiTopicExposesThinkingInCompose() = runTest {
        val repository: LearningContentRepository = BundledLearningContentRepository()

        val items = repository.getActiveUnitsByTopic("android_ui").toLearningUnitItems()

        assertEquals(listOf("unit_thinking_in_compose"), items.map { it.unitId })
        val unit = items.single()
        assertEquals("Thinking in Compose", unit.title)
        assertEquals(3, unit.activeLessonCount)
        // A discovery row without prose would be a title and a number, so the summary has to survive
        // the mapping rather than merely being present in the document.
        assertTrue(unit.summary.isNotBlank())
    }

    @Test
    fun aTopicWithNoAuthoredLearningContentMapsToAnEmptyStudySection() = runTest {
        val repository: LearningContentRepository = BundledLearningContentRepository()

        // Successfully empty, which is a different answer from unreadable and must stay that way:
        // most Topics have no authored Units yet and are still perfectly ordinary Topics.
        assertEquals(
            emptyList(),
            repository.getActiveUnitsByTopic("kotlin_language").toLearningUnitItems(),
        )
    }
}
