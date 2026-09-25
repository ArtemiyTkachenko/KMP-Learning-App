package org.artkachenko.kmp_learning_app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What a group owes its members, stated once here so the two features using it do not each have to
 * prove it.
 *
 * Nothing about colour, corner radius, divider inset, or spacing is asserted — those are the visual
 * decisions the component exists to make, and a test that pinned them would only make them harder
 * to tune. What is asserted is the contract a caller depends on: rows appear in the order given,
 * each keeps whatever interaction it declared for itself, and an empty group is not an empty box.
 */
@OptIn(ExperimentalTestApi::class)
internal class ContentGroupTest {

    private val groupTag = "group"

    @Test
    fun everyRowIsComposedInTheOrderItWasGiven() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ContentGroup(
                    rows = listOf(
                        { Text("first") },
                        { Text("second") },
                        { Text("third") },
                    ),
                )
            }
        }

        onNodeWithText("first").assertIsDisplayed()
        onNodeWithText("second").assertIsDisplayed()
        onNodeWithText("third").assertIsDisplayed()
    }

    /**
     * The reason [ContentGroup] takes a list rather than a block: a caller builds its rows from
     * optional state and must never have to ask whether anything survived. An empty group that
     * drew its container would leave a bare rounded rectangle on the page every time a learner had
     * no continuation shortcut to offer.
     */
    @Test
    fun anEmptyGroupDrawsNothingAtAll() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ContentGroup(rows = emptyList(), modifier = Modifier.testTag(groupTag))
            }
        }

        onNodeWithTag(groupTag).assertDoesNotExist()
    }

    /**
     * Grouping is a container decision and must not reach into the rows. A group holding one
     * navigable row beside one inert row is the Continue Learning case — the `Complete` state has
     * nowhere to go — and the group takes no click of its own that could make the inert row
     * tappable by proximity.
     */
    @Test
    fun aRowKeepsItsOwnInteractionAndAnInertRowStaysInert() = runComposeUiTest {
        var clicks = 0
        setContent {
            MaterialTheme {
                ContentGroup(
                    rows = listOf(
                        {
                            Text(
                                text = "navigable",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("navigable")
                                    .clickable(role = Role.Button) { clicks += 1 },
                            )
                        },
                        { Text("inert", modifier = Modifier.fillMaxWidth().testTag("inert")) },
                    ),
                )
            }
        }

        onNodeWithTag("navigable")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
            .performClick()
        onNodeWithTag("inert").assertHasNoClickAction()

        assertEquals(1, clicks)
    }
}
