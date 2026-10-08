@file:OptIn(ExperimentalTestApi::class)

package org.artkachenko.kmp_learning_app.topic_study.topics

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressPolicy
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.SubtopicPracticeItem
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicSubtopicsPage
import org.artkachenko.kmp_learning_app.ui.LearningContextUiModel
import org.artkachenko.kmp_learning_app.ui.theme.AppDarkColorScheme
import org.artkachenko.kmp_learning_app.ui.theme.AppDarkSemanticColors
import org.artkachenko.kmp_learning_app.ui.theme.AppLightColorScheme
import org.artkachenko.kmp_learning_app.ui.theme.AppLightSemanticColors
import org.artkachenko.kmp_learning_app.ui.theme.BodyTextContrast
import org.artkachenko.kmp_learning_app.ui.theme.assertContrastAtLeast

/**
 * Every Learn Topic card has the same shape whatever its Topic's state: the name, one supporting
 * line, and a trailing figure whose label describes that figure. Each card's merged text is
 * asserted whole and in order, so a pill, a third line, or a mismatched label anywhere on it fails.
 */
internal class TopicCardStatesTest {

    @Test
    fun aCardWithEvidenceReadsNameThenUnitsAndCoverageThenAWholePercentOverAccuracy() = runComposeUiTest {
        renderTopics(card("UI — Views & Jetpack Compose", units = 12, context(15, 49, 72.4)))

        assertEquals(
            listOf("UI — Views & Jetpack Compose", "12 units · 15 of 49 explored", "72%", "accuracy"),
            cardText("UI — Views & Jetpack Compose"),
        )
    }

    @Test
    fun aWeakCardCarriesItsVerdictAsTheFigureLabelAndNoPill() = runComposeUiTest {
        renderTopics(card("UI — Views & Jetpack Compose", units = 12, context(15, 49, 21.1, isWeak = true)))

        assertEquals(
            listOf("UI — Views & Jetpack Compose", "12 units · 15 of 49 explored", "21%", "weak area"),
            cardText("UI — Views & Jetpack Compose"),
        )
        onAllNodesWithText("Weak area").assertCountEqualsZero()
    }

    /**
     * The dash is drawn, and read as the reason it is there. That reason replaces the dash's own
     * text rather than adding a content description: the card merges its descendants, and on Desktop
     * a merged content description becomes the card's whole accessible name, so the Topic's name and
     * coverage would go unread. The card's spoken text is therefore asserted whole, with no
     * description at all.
     */
    @Test
    fun belowTheMinimumTheCardShowsADashOverAccuracyAndSaysWhy() = runComposeUiTest {
        renderTopics(card("Kotlin Language & JVM Fundamentals", units = 4, context(2, 34, 100.0, answered = 4)))

        assertEquals(
            listOf(
                "Kotlin Language & JVM Fundamentals",
                "4 units · 2 of 34 explored",
                "Accuracy shown after ${LearningProgressPolicy.WeakAreaMinimumAnswered} answers, 4 so far",
                "accuracy",
            ),
            cardText("Kotlin Language & JVM Fundamentals"),
        )
        assertEquals(emptyList(), cardDescriptions("Kotlin Language & JVM Fundamentals"))
    }

    @Test
    fun aNeverAnsweredCardHasNoFigureAndPutsItsUnitsBeforeNotStarted() = runComposeUiTest {
        renderTopics(card("Architecture", units = 3, context(0, 31)))

        assertEquals(
            listOf("Architecture", "3 units · Not started · 31 questions"),
            cardText("Architecture"),
        )
    }

    @Test
    fun anUnstudiedCardWithoutUnitsKeepsItsSingleNotStartedLine() = runComposeUiTest {
        renderTopics(card("Networking & Serialization", units = 0, context(0, 23)))

        assertEquals(
            listOf("Networking & Serialization", "Not started · 23 questions"),
            cardText("Networking & Serialization"),
        )
        assertEquals(emptyList(), cardDescriptions("Networking & Serialization"))
    }

    @Test
    fun aCardWhoseAnalyticsAreUnavailableStatesItsUnitsAlone() = runComposeUiTest {
        renderTopics(card("Testing", units = 1, null))

        assertEquals(listOf("Testing", "1 unit"), cardText("Testing"))
    }

    /** The weak verdict moved into the figure column, so it no longer adds a row to the card. */
    @Test
    fun aWeakCardIsNoTallerThanItsNeighbours() = runComposeUiTest {
        renderTopics(
            card("Topic A", units = 2, context(5, 20, 40.0, isWeak = true)),
            card("Topic B", units = 2, context(5, 20, 90.0)),
        )

        assertEquals(cardNode("Topic A").boundsInRoot.height, cardNode("Topic B").boundsInRoot.height)
    }

    @Test
    fun subtopicRowsFollowTheSameTrailingFigureRules() = runComposeUiTest {
        renderSubtopics(
            subtopic("Coroutines", context(6, 10, 88.0)),
            subtopic("Flows", context(6, 10, 41.0, isWeak = true)),
            subtopic("Channels", context(1, 10, 0.0, answered = 1)),
            subtopic("Dispatchers", context(0, 10)),
        )

        assertEquals(listOf("Coroutines", "6 of 10 explored", "88%", "accuracy"), cardText("Coroutines"))
        assertEquals(listOf("Flows", "6 of 10 explored", "41%", "weak area"), cardText("Flows"))
        assertEquals(
            listOf(
                "Channels",
                "1 of 10 explored",
                "Accuracy shown after ${LearningProgressPolicy.WeakAreaMinimumAnswered} answers, 1 so far",
                "accuracy",
            ),
            cardText("Channels"),
        )
        assertEquals(emptyList(), cardDescriptions("Channels"))
        assertEquals(listOf("Dispatchers", "0 of 10 explored"), cardText("Dispatchers"))
    }

    /**
     * The regression: below the minimum an answer count used to sit over the "accuracy" label, and
     * "4 answered / accuracy" said nothing. Across every state on both surfaces, "answered" never
     * appears, and whatever sits directly over "accuracy" is a percentage or the neutral dash.
     */
    @Test
    fun noTopicCardLabelEverDescribesSomethingOtherThanTheValueAboveIt() = runComposeUiTest {
        renderTopics(*RegressionStates.mapIndexed { i, it -> card("Topic $i", units = i, it) }.toTypedArray())

        assertEveryLabelSitsUnderAnAccuracyValue(RegressionStates.indices.map { "Topic $it" })
    }

    @Test
    fun noSubtopicRowLabelEverDescribesSomethingOtherThanTheValueAboveIt() = runComposeUiTest {
        renderSubtopics(*RegressionStates.mapIndexed { i, it -> subtopic("Subtopic $i", it) }.toTypedArray())

        assertEveryLabelSitsUnderAnAccuracyValue(RegressionStates.indices.map { "Subtopic $it" })
    }

    @Test
    fun theWeakAreaLabelReadsOnTheCardInBothThemes() {
        assertContrastAtLeast(
            AppLightSemanticColors.partiallyCorrect,
            AppLightColorScheme.surfaceContainerLow,
            BodyTextContrast,
            "light weak-area label on the card",
        )
        assertContrastAtLeast(
            AppDarkSemanticColors.partiallyCorrect,
            AppDarkColorScheme.surfaceContainerLow,
            BodyTextContrast,
            "dark weak-area label on the card",
        )
    }
}

/** Every accuracy state, including both sides of the evidence minimum. */
private val RegressionStates = listOf(
    context(15, 49, 72.4),
    context(15, 49, 21.1, isWeak = true),
    context(2, 34, 100.0, answered = 1),
    context(2, 34, 50.0, answered = LearningProgressPolicy.WeakAreaMinimumAnswered - 1),
    context(2, 34, 50.0, answered = LearningProgressPolicy.WeakAreaMinimumAnswered),
    context(0, 31),
)

/**
 * The regression: below the minimum an answer count used to sit over the "accuracy" label, and
 * "4 answered / accuracy" said nothing. "answered" never appears, and whatever sits directly over a
 * figure label is a whole percentage or the neutral dash.
 */
private fun ComposeUiTest.assertEveryLabelSitsUnderAnAccuracyValue(names: List<String>) {
    onAllNodesWithText("answered", substring = true, useUnmergedTree = true).assertCountEqualsZero()
    names.forEach { name ->
        // Lazy lists compose only what is on screen, so each row is brought into view first.
        onNode(hasScrollAction()).performScrollToNode(hasText(name))
        val texts = cardText(name)
        texts.forEachIndexed { index, text ->
            if (text == "accuracy" || text == "weak area") {
                val value = texts[index - 1]
                // The dash is read as its explanation, so that is what sits over its label.
                assertTrue(
                    value.startsWith("Accuracy shown after ") || Regex("""\d+%""").matches(value),
                    "\"$text\" on $name sits under \"$value\", which is not an accuracy figure",
                )
            }
        }
    }
}

private fun ComposeUiTest.renderTopics(vararg topics: TopicBrowserItemUiModel) {
    setContent {
        MaterialTheme {
            TopicBrowserScreen(
                state = browsingContent(topics = topics.toList()),
                onTopicClick = {},
                onRetry = {},
            )
        }
    }
}

private fun ComposeUiTest.renderSubtopics(vararg items: SubtopicPracticeItem) {
    setContent {
        MaterialTheme {
            TopicSubtopicsPage(
                subtopics = items.toList(),
                onStartSubtopicPractice = {},
                onPracticePreset = {},
                onBrowsePractice = {},
                listState = rememberLazyListState(),
            )
        }
    }
}

private fun ComposeUiTest.cardNode(name: String): SemanticsNode =
    onNode(hasClickAction() and hasText(name)).fetchSemanticsNode()

/** The card's merged text, in reading order: what every platform's screen reader reads. */
private fun ComposeUiTest.cardText(name: String): List<String> =
    cardNode(name).config[SemanticsProperties.Text].map { it.text }

private fun ComposeUiTest.cardDescriptions(name: String): List<String> =
    cardNode(name).config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()

private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.assertCountEqualsZero() {
    assertEquals(0, fetchSemanticsNodes().size)
}

private fun card(name: String, units: Int?, context: LearningContextUiModel?) =
    TopicBrowserItemUiModel(name.lowercase().replace(' ', '_'), name, context, units)

private fun subtopic(name: String, context: LearningContextUiModel?) = SubtopicPracticeItem(
    subtopic = Subtopic(name.lowercase().replace(' ', '_'), "topic_a", name),
    questionCount = context?.totalQuestionCount ?: 1,
    learningContext = context,
)

private fun context(
    attempted: Int,
    total: Int,
    accuracy: Double? = null,
    isWeak: Boolean = false,
    answered: Int = if (accuracy == null) 0 else LearningProgressPolicy.WeakAreaMinimumAnswered,
) = LearningContextUiModel(
    attemptedQuestionCount = attempted,
    totalQuestionCount = total,
    coveragePercentage = if (total == 0) null else attempted.toDouble() / total * 100.0,
    accuracyPercentage = accuracy,
    answeredCount = answered,
    isWeak = isWeak,
)
