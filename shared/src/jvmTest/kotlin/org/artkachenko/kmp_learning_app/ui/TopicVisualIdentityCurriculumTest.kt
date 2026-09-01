package org.artkachenko.kmp_learning_app.ui

import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.content.BundledCurriculumSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The unknown-ID fallback exists so a future curriculum cannot crash the app. It must not also
 * hide a bundled Topic nobody authored a glyph for, which is what these tests are for.
 */
internal class TopicVisualIdentityCurriculumTest {
    @Test
    fun everyActiveBundledTopicHasAnExplicitVisualIdentity() = runTest {
        val activeTopicIds = activeTopicIds()

        assertTrue(activeTopicIds.isNotEmpty())
        val unmapped = activeTopicIds.filterNot(::hasExplicitTopicVisualIdentity)
        assertTrue(
            unmapped.isEmpty(),
            "Bundled Topics falling back to the generic marker: $unmapped",
        )
    }

    @Test
    fun everyActiveBundledTopicHasADistinctGlyph() = runTest {
        val activeTopicIds = activeTopicIds()
        val icons = activeTopicIds.map { topicVisualIdentity(it).icon }

        assertEquals(
            activeTopicIds.size,
            icons.distinct().size,
            "Two Topics share a glyph, which defeats at-a-glance recognition: " +
                activeTopicIds.groupBy { topicVisualIdentity(it).icon.name }
                    .filterValues { it.size > 1 },
        )
    }

    private suspend fun activeTopicIds(): List<String> =
        BundledCurriculumSource.load().topics
            .filter { it.status == ContentStatus.ACTIVE }
            .map { it.id }
}
