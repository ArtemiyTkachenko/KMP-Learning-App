package org.artkachenko.kmp_learning_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.ui.theme.AppMotion
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme

/**
 * The keying contract of [ScreenStateTransition], asserted on a synthetic state rather than through
 * any of the screens that use it.
 *
 * Ten screens now switch their loading, empty, error, and content branches through this one
 * component, and all of them depend on the same property: a change of *state class* is one thing
 * becoming another and cross-fades, while a change of data *within* a class is not a screen
 * transition at all and must not run one. Getting that wrong is not a subtle regression — it fades
 * the whole collection out and back in every time a row leaves a list, over the top of the row's
 * own `animateItem`.
 *
 * What is asserted is the decision rather than the animation: after the state changes, is the
 * outgoing content still composed or was it replaced outright? No duration, easing, alpha, or frame
 * count is named, and the clock is advanced by a single frame in the cases that matter, which is far
 * inside any plausible fade. A test that asserted a value partway through a curve would be
 * asserting the curve.
 *
 * The one timing test here asserts a phase rather than a curve: inside the exit window the incoming
 * state is not drawn at all. That is the property that keeps two screens of text from showing at
 * once, and its window is read from [AppMotion.StateReplacement] rather than restated.
 */
@OptIn(ExperimentalTestApi::class)
internal class ScreenStateTransitionTest {

    /** The state shape every screen here has: some singleton branches and one that carries data. */
    private sealed interface FakeState {
        data object Loading : FakeState
        data class Content(val label: String) : FakeState
    }

    @Test
    fun crossingBetweenStateClassesRunsATransition() = runComposeUiTest {
        var state by mutableStateOf<FakeState>(FakeState.Loading)
        mainClock.autoAdvance = false
        setContent {
            AppTheme {
                ScreenStateTransition(state) { current ->
                    Text(
                        when (current) {
                            FakeState.Loading -> "loading"
                            is FakeState.Content -> current.label
                        },
                    )
                }
            }
        }
        mainClock.advanceTimeBy(SettleMillis)
        onNodeWithText("loading").assertIsDisplayed()

        state = FakeState.Content("first")
        mainClock.advanceTimeByFrame()

        // One frame in, the outgoing branch is still composed: it is fading rather than gone, which
        // is the whole purpose — a spinner one frame and a full screen the next is the hard cut this
        // component exists to remove.
        onNodeWithText("loading").assertExists()
        onNodeWithText("first").assertExists()

        mainClock.advanceTimeBy(SettleMillis)
        onNodeWithText("loading").assertDoesNotExist()
        onNodeWithText("first").assertIsDisplayed()
    }

    /**
     * The property those callers actually rely on.
     *
     * Removing a saved question, recording an answer, or resolving one more mistake all leave the
     * screen in `Content`. Those are data changes, and the row that changed owns whatever animation
     * it deserves. If the default `contentKey` ever stopped collapsing them onto one key, every one
     * of those screens would blink.
     */
    @Test
    fun aDataChangeWithinOneStateClassReplacesContentOutright() = runComposeUiTest {
        var state by mutableStateOf<FakeState>(FakeState.Content("first"))
        mainClock.autoAdvance = false
        setContent {
            AppTheme {
                ScreenStateTransition(state) { current ->
                    Text(
                        when (current) {
                            FakeState.Loading -> "loading"
                            is FakeState.Content -> current.label
                        },
                    )
                }
            }
        }
        mainClock.advanceTimeBy(SettleMillis)
        onNodeWithText("first").assertIsDisplayed()

        state = FakeState.Content("second")
        mainClock.advanceTimeByFrame()

        // No transition: the old label is gone the moment the new one arrives. Were this fading,
        // both would be composed here, exactly as they are in the test above.
        onNodeWithText("first").assertDoesNotExist()
        onNodeWithText("second").assertIsDisplayed()
    }

    /**
     * The outgoing state is a blue fill and the incoming one a red fill, over black. While the
     * incoming state is held back, no red reaches the screen however far the blue has faded; were
     * the entrance to start alongside the exit, as it once did, red would already be showing here.
     */
    @Test
    fun theIncomingStateIsNotDrawnUntilTheOutgoingOneHasGone() = runComposeUiTest {
        var state by mutableStateOf<FakeState>(FakeState.Loading)
        mainClock.autoAdvance = false
        setContent {
            Box(Modifier.size(ProbeSize).background(Color.Black)) {
                ScreenStateTransition(state) { current ->
                    val fill = if (current == FakeState.Loading) Color.Blue else Color.Red
                    Box(Modifier.fillMaxSize().background(fill))
                }
            }
        }
        mainClock.advanceTimeBy(SettleMillis)

        state = FakeState.Content("first")
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeBy(AppMotion.StateReplacement.exitMillis / 2L)

        val midExit = centrePixel()
        assertTrue(midExit.blue > 0f, "The outgoing state should still be fading out, but was gone")
        assertTrue(
            midExit.red <= ColorChannelTolerance,
            "The incoming state was already drawn (red ${midExit.red}) inside the exit window",
        )

        mainClock.advanceTimeBy(SettleMillis)
        val settled = centrePixel()
        assertTrue(settled.red >= 1f - ColorChannelTolerance, "The incoming state never arrived")
        assertTrue(settled.blue <= ColorChannelTolerance, "The outgoing state never left")
    }

    private fun androidx.compose.ui.test.ComposeUiTest.centrePixel(): Color {
        val pixels = onRoot().captureToImage().toPixelMap()
        return pixels[pixels.width / 2, pixels.height / 2]
    }
}

private val ProbeSize = 40.dp

/** One 8-bit step, so rounding in the captured image is not mistaken for a drawn colour. */
private const val ColorChannelTolerance = 1f / 255f

/** Comfortably longer than any spec on `AppMotion`, so "settled" never means "still moving". */
private const val SettleMillis = 2_000L
