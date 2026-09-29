package org.artkachenko.kmp_learning_app.topic_study.topics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumSection
import org.artkachenko.kmp_learning_app.guided_learning.LearningRecommendationRationale
import org.artkachenko.kmp_learning_app.guided_learning.LearningRecommendationTarget

/**
 * The browsing catalogue's section headings, rendered from state that has already been sectioned.
 *
 * The screen never partitions Topics itself, so these tests hand it sections directly and check only
 * how they are presented: which headings appear, in what order, with what semantics, and that the
 * rows beneath them and the search results beside them behave exactly as before.
 */
@OptIn(ExperimentalTestApi::class)
internal class TopicBrowserSectionsScreenTest {

    @Test
    fun theAndroidHeadingIsShownEvenWhenItIsTheOnlySection() = runComposeUiTest {
        show(TopicBrowserUiState.Content(sections = listOf(androidSection)))

        onNodeWithText(AndroidHeading).assertIsDisplayed().assert(isHeading())
        onNodeWithText(KmpHeading).assertDoesNotExist()
        // The old single catalogue heading is gone from browsing.
        onNodeWithText("Topics").assertDoesNotExist()
    }

    @Test
    fun withKmpShownBothHeadingsAppearAndroidFirstEachAboveItsOwnRows() = runComposeUiTest {
        show(TopicBrowserUiState.Content(sections = listOf(androidSection, kmpSection)))

        val androidHeading = onNodeWithText(AndroidHeading).assertIsDisplayed().assert(isHeading())
        val kmpHeading = onNodeWithText(KmpHeading).assertIsDisplayed().assert(isHeading())
        val androidRow = onNodeWithText("Android Platform")
        val kmpRow = onNodeWithText("Shared ViewModels")

        assertTrue(androidHeading.top() < androidRow.top())
        assertTrue(androidRow.top() < kmpHeading.top())
        assertTrue(kmpHeading.top() < kmpRow.top())
    }

    @Test
    fun aKmpRowStaysAnOrdinaryTopicTarget() = runComposeUiTest {
        var clicked: String? = null
        show(
            TopicBrowserUiState.Content(sections = listOf(androidSection, kmpSection)),
            onTopicClick = { clicked = it },
        )

        onNodeWithText("Shared ViewModels").assertHasClickAction().performClick()
        assertEquals("kmp", clicked)
        onNodeWithText("Android Platform").performClick()
        assertEquals("android_platform", clicked)
    }

    /** Search stays flat: result groups keep their Topics and Subtopics headings, and no section. */
    @Test
    fun searchResultsKeepTheirTopicsAndSubtopicsGroupsWithoutSections() = runComposeUiTest {
        show(
            TopicBrowserUiState.Content(
                sections = listOf(androidSection, kmpSection),
                query = "shared",
                topicMatches = kmpSection.topics,
                subtopicMatches = listOf(
                    SubtopicSearchResult(
                        subtopicId = "kmp_shared_graph",
                        subtopicName = "Koin in a shared graph",
                        parentTopicId = "kmp",
                        parentTopicName = "Shared ViewModels",
                    ),
                ),
            ),
        )

        onNodeWithText("Topics").assertIsDisplayed()
        onNodeWithText("Subtopics").assertIsDisplayed()
        onNodeWithText(AndroidHeading).assertDoesNotExist()
        onNodeWithText(KmpHeading).assertDoesNotExist()
    }

    @Test
    fun guidanceStaysAboveTheFirstSection() = runComposeUiTest {
        show(
            TopicBrowserUiState.Content(
                sections = listOf(androidSection, kmpSection),
                recommendedNext = RecommendedNextUiModel(
                    target = LearningRecommendationTarget.MistakeReview,
                    rationale = LearningRecommendationRationale.UnresolvedMistakes(2),
                ),
            ),
        )

        assertTrue(
            onNodeWithTag(TopicBrowserRecommendedNextTag).top() <
                onNodeWithTag(TopicBrowserSavedQuestionsTag).top(),
        )
        assertTrue(onNodeWithTag(TopicBrowserSavedQuestionsTag).top() < onNodeWithText(AndroidHeading).top())
    }

    @Test
    fun aLongKmpTopicNameWrapsInsideItsSectionOnACompactScreen() = runComposeUiTest {
        val longName = "Kotlin Multiplatform shared ViewModels, host lifecycles and dependency graphs"
        show(
            TopicBrowserUiState.Content(
                sections = listOf(
                    androidSection,
                    TopicBrowserSection(
                        kind = CurriculumSection.KotlinMultiplatform,
                        topics = listOf(TopicBrowserItemUiModel("kmp", longName, learningUnitCount = 2)),
                    ),
                ),
            ),
            width = 320,
        )

        val root = onNodeWithTag(RootTag).fetchSemanticsNode().boundsInRoot
        val row = onNodeWithText(longName).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue(row.right <= root.right, "The name must wrap rather than run off the screen.")
        assertTrue(row.top > onNodeWithText(KmpHeading).top())
    }

    private fun ComposeUiTest.show(
        state: TopicBrowserUiState,
        onTopicClick: (String) -> Unit = {},
        width: Int = 400,
    ) {
        setContent {
            MaterialTheme {
                Box(Modifier.size(width.dp, 1600.dp).testTag(RootTag)) {
                    TopicBrowserScreen(state = state, onTopicClick = onTopicClick, onRetry = {})
                }
            }
        }
    }

    private fun SemanticsNodeInteraction.top(): Float = fetchSemanticsNode().boundsInRoot.top

    private companion object {
        const val RootTag = "sections_root"
        const val AndroidHeading = "Android Engineering"
        const val KmpHeading = "Kotlin Multiplatform"

        val androidSection = TopicBrowserSection(
            kind = CurriculumSection.AndroidEngineering,
            topics = listOf(TopicBrowserItemUiModel("android_platform", "Android Platform")),
        )
        val kmpSection = TopicBrowserSection(
            kind = CurriculumSection.KotlinMultiplatform,
            topics = listOf(TopicBrowserItemUiModel("kmp", "Shared ViewModels")),
        )
    }
}
