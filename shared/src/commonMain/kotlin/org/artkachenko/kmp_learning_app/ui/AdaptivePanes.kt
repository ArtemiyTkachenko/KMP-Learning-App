package org.artkachenko.kmp_learning_app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.artkachenko.kmp_learning_app.ui.theme.AppLayout

/**
 * Two panes side by side, for a screen whose content genuinely divides at
 * [org.artkachenko.kmp_learning_app.ui.theme.AppWindowSizeClass.Expanded].
 *
 * A thin wrapper over `Row`, and it exists for the two things it states rather than for the
 * layout: the gutter between panes is [AppLayout.PaneGutter] everywhere, and [primary] is composed
 * first. The second point is the accessibility rule for every expanded layout in this app — a
 * screen reader, a keyboard tab order, and a linear text traversal all follow composition order,
 * so the pane a learner would read first on a phone has to be the pane declared first here. Where
 * a design wanted the summary on the right, the answer is to put the summary in [primary] and
 * arrange the weights, not to reorder the declarations.
 *
 * Each pane scrolls on its own. That is deliberate for a dashboard, where the panes hold different
 * kinds of thing and a learner reading a long history should not be dragging the headline figures
 * off the screen with it; it is wrong for continuous prose, which is why the Lesson reader is a
 * single centred column at every width.
 */
@Composable
internal fun AppTwoPaneRow(
    primary: @Composable RowScope.() -> Unit,
    secondary: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(AppLayout.PaneGutter),
    ) {
        primary()
        secondary()
    }
}
