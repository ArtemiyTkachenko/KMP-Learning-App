package org.artkachenko.kmp_learning_app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme

/**
 * The reflow contract of [TrailingFigureRow], asserted on synthetic content rather than through
 * either screen that uses it.
 *
 * What is asserted is the *decision* — does the figure sit beside the text or below it — and never
 * a measurement. Both call sites draw a name beside an accuracy, and at an ordinary type scale they
 * must be unchanged; this component exists because at a doubled one the text column was being
 * squeezed below the width of its own longest word, which is the point where Compose stops wrapping
 * and starts breaking inside a word.
 *
 * The widths here are deliberately not borderline. A test sitting a pixel either side of the
 * decision would be asserting the metrics of whatever font the host happened to load.
 */
@OptIn(ExperimentalTestApi::class)
internal class TrailingFigureRowTest {

    @Test
    fun theFigureSitsBesideTheTextWhileTheTextStillFits() = runRow(
        width = 320.dp,
        text = "Kotlin",
    ) {
        val text = bounds(TextTag)
        val figure = bounds(FigureTag)

        assertTrue(
            figure.left >= text.right,
            "The figure starts at ${figure.left}, overlapping text that ends at ${text.right}.",
        )
        assertTrue(
            figure.top < text.bottom && text.top < figure.bottom,
            "The figure and the text share no line, so the row stacked when it had room not to.",
        )
    }

    /**
     * A single word wider than the space left beside the figure. No amount of wrapping rescues
     * that — the next thing Compose does is break the word itself — so the figure moves instead.
     */
    @Test
    fun theFigureDropsBelowTheTextWhenTheTextWouldBeBrokenMidWord() = runRow(
        width = 200.dp,
        text = "Internationalisation",
        fontScale = 2f,
    ) {
        val text = bounds(TextTag)
        val figure = bounds(FigureTag)

        assertTrue(
            figure.top >= text.bottom,
            "The figure is still beside the text: it spans ${figure.top}..${figure.bottom} " +
                "against text ending at ${text.bottom}.",
        )
    }

    /**
     * The figure slot composing nothing is the ordinary case for a scope with no recorded answer.
     * There is then no reflow question to ask, and the text must get the row rather than being laid
     * out as though something were sitting beside it.
     */
    @Test
    fun aRowWithNoFigureGivesTheWholeWidthToItsText() = runSkikoComposeUiTest(
        size = Size(RowWidth.value, 400f),
    ) {
        setContent {
            AppTheme {
                Box(Modifier.width(RowWidth)) {
                    TrailingFigureRow(figure = {}) {
                        Text(
                            // Long enough that it has to wrap, so the width it takes is the width
                            // it was offered rather than the width it happened to need.
                            text = "Kotlin language and tooling for multiplatform projects",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag(TextTag),
                        )
                    }
                }
            }
        }

        onNodeWithTag(FigureTag).assertDoesNotExist()
        val text = bounds(TextTag)
        assertTrue(
            text.right > RowWidth.value - FigureSlotAllowance,
            "The text stopped at ${text.right} in a ${RowWidth.value}px row, as if something " +
                "were sitting beside it.",
        )
    }

    private fun runRow(
        width: Dp,
        text: String,
        fontScale: Float = 1f,
        assertions: SkikoComposeUiTest.() -> Unit,
    ) = runSkikoComposeUiTest(
        size = Size(width.value, 400f),
        density = Density(density = 1f, fontScale = fontScale),
    ) {
        setContent {
            AppTheme {
                Box(Modifier.width(width)) {
                    TrailingFigureRow(
                        figure = {
                            Text(
                                text = "accuracy",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag(FigureTag),
                            )
                        },
                    ) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag(TextTag),
                        )
                    }
                }
            }
        }
        assertions()
    }
}

@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.bounds(tag: String): Rect =
    onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

private val RowWidth = 320.dp

/** Comfortably narrower than any figure this row would carry, so the check cannot pass by accident. */
private const val FigureSlotAllowance = 24f

private const val TextTag = "trailing_figure_row_text"
private const val FigureTag = "trailing_figure_row_figure"
