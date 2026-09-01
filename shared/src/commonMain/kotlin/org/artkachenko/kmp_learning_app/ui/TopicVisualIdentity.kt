package org.artkachenko.kmp_learning_app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * The presentation-only visual identity of a curriculum Topic.
 *
 * It holds a glyph and nothing else on purpose. There is no per-Topic colour: seventeen tinted
 * categories would be decoration the product cannot keep coherent as the curriculum grows, so
 * every marker uses the same theme roles and only the glyph varies.
 */
internal data class TopicVisualIdentity(
    val icon: ImageVector,
)

/**
 * Keyed by the stable Topic ID rather than the display name, because names are user-facing content
 * that can be reworded at any time while IDs are the curriculum's identity boundary.
 *
 * This deliberately lives in shared UI rather than in [org.artkachenko.kmp_learning_app.curriculum.Topic]
 * or the bundled curriculum JSON: an icon is presentation, and putting it in the model would drag
 * it through serialization and the Room schema for no behavioural gain. Keeping one mapping here
 * also means Topics, search, and the later Topic-performance surfaces cannot drift apart.
 */
private val TopicVisualIdentities: Map<String, TopicVisualIdentity> = mapOf(
    "android_platform" to TopicVisualIdentity(AppIcons.Smartphone),
    "lifecycle_navigation" to TopicVisualIdentity(AppIcons.Route),
    "android_ui" to TopicVisualIdentity(AppIcons.Layout),
    "kotlin_language" to TopicVisualIdentity(AppIcons.Code),
    "async_reactive" to TopicVisualIdentity(AppIcons.Branch),
    "architecture" to TopicVisualIdentity(AppIcons.AccountTree),
    "dependency_injection" to TopicVisualIdentity(AppIcons.Hub),
    "local_data" to TopicVisualIdentity(AppIcons.Database),
    "networking" to TopicVisualIdentity(AppIcons.Globe),
    "background_work" to TopicVisualIdentity(AppIcons.Schedule),
    "notifications" to TopicVisualIdentity(AppIcons.Notifications),
    "testing" to TopicVisualIdentity(AppIcons.Science),
    "performance" to TopicVisualIdentity(AppIcons.Speed),
    "security" to TopicVisualIdentity(AppIcons.Shield),
    "build_delivery" to TopicVisualIdentity(AppIcons.Package),
    "mobile_system_design" to TopicVisualIdentity(AppIcons.Schema),
    "kmp" to TopicVisualIdentity(AppIcons.Devices),
)

/** Neutral marker for a Topic that has no authored glyph yet. */
private val FallbackTopicVisualIdentity = TopicVisualIdentity(AppIcons.Topics)

/**
 * Resolves the marker for [topicId], falling back to a generic Topic glyph.
 *
 * A future curriculum can introduce Topics this build has never seen, so an unknown ID must render
 * rather than crash. `TopicVisualIdentityCurriculumTest` keeps that fallback from quietly covering
 * for a bundled Topic nobody authored a glyph for.
 */
internal fun topicVisualIdentity(topicId: String): TopicVisualIdentity =
    TopicVisualIdentities[topicId] ?: FallbackTopicVisualIdentity

/** Whether [topicId] has an authored mapping rather than the generic fallback. */
internal fun hasExplicitTopicVisualIdentity(topicId: String): Boolean =
    topicId in TopicVisualIdentities

/**
 * Test handle for a rendered marker.
 *
 * The icon is decorative to accessibility services, so it has no content description for a test to
 * hang off; a tag keeps the test API out of what a screen reader announces.
 */
internal fun topicVisualMarkerTag(topicId: String): String = "topic_marker_$topicId"

private val TopicMarkerSize = 36.dp
private val TopicMarkerIconSize = 20.dp

/**
 * The Topic marker: a tonal container with the Topic's glyph.
 *
 * Sized to stay secondary to the Topic name, which remains the authoritative identity. The icon
 * carries no content description because the name is always rendered beside it — describing it
 * would make a screen reader announce the same Topic twice.
 */
@Composable
internal fun TopicVisualMarker(
    topicId: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(TopicMarkerSize).testTag(topicVisualMarkerTag(topicId)),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = topicVisualIdentity(topicId).icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(TopicMarkerIconSize),
            )
        }
    }
}
