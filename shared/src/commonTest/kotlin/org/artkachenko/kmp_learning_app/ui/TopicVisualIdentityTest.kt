package org.artkachenko.kmp_learning_app.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal class TopicVisualIdentityTest {
    @Test
    fun mappingIsKeyedByStableTopicIdNotDisplayName() {
        assertTrue(hasExplicitTopicVisualIdentity("networking"))
        assertEquals(AppIcons.Globe, topicVisualIdentity("networking").icon)

        // The display name is content that can be reworded, so it must not resolve anything.
        assertFalse(hasExplicitTopicVisualIdentity("Networking & Serialization"))
        assertEquals(
            AppIcons.Topics,
            topicVisualIdentity("Networking & Serialization").icon,
        )
    }

    @Test
    fun unknownTopicIdFallsBackToTheGenericMarker() {
        assertFalse(hasExplicitTopicVisualIdentity("future_topic_from_a_later_curriculum"))
        assertEquals(
            AppIcons.Topics,
            topicVisualIdentity("future_topic_from_a_later_curriculum").icon,
        )
    }

    @Test
    fun blankTopicIdResolvesSafely() {
        assertEquals(AppIcons.Topics, topicVisualIdentity("").icon)
    }

    @Test
    fun sameTopicIdAlwaysResolvesToTheSameIdentity() {
        assertEquals(topicVisualIdentity("kmp"), topicVisualIdentity("kmp"))
        assertEquals(topicVisualIdentity("security"), topicVisualIdentity("security"))
        assertNotEquals(topicVisualIdentity("kmp"), topicVisualIdentity("security"))
    }

    @Test
    fun markerTagIsDerivedFromTheStableTopicId() {
        assertEquals("topic_marker_kmp", topicVisualMarkerTag("kmp"))
    }
}
