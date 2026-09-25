package org.artkachenko.kmp_learning_app.assessment_review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.artkachenko.kmp_learning_app.ui.AppTwoPaneRow
import org.artkachenko.kmp_learning_app.ui.SectionHeading
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppWindowSizeClass
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding

/**
 * The shared adaptive shell for a completed assessment's summary and review transcript.
 *
 * Both result types keep the same semantic order at every width: outcome first, transcript second.
 * Only the arrangement changes, from one scroll to two independently scrolling panes.
 */
@Composable
internal fun AssessmentResultLayout(
    modifier: Modifier,
    summaryPaneModifier: Modifier = Modifier,
    reviewPaneModifier: Modifier = Modifier,
    summary: LazyListScope.() -> Unit,
    review: LazyListScope.() -> Unit,
) {
    if (LocalAppWindowSizeClass.current.isExpanded) {
        AppTwoPaneRow(
            modifier = modifier,
            primary = {
                AssessmentResultPane(
                    modifier = Modifier.weight(1f).then(summaryPaneModifier),
                    content = summary,
                )
            },
            secondary = {
                AssessmentResultPane(
                    modifier = Modifier.weight(1f).then(reviewPaneModifier),
                    content = review,
                )
            },
        )
        return
    }
    AssessmentResultPane(modifier) {
        summary()
        review()
    }
}

/**
 * The heading that leads a result's review transcript.
 *
 * The transition out of the outcome and into the transcript is the one place the arrangement changes
 * what the spacing should be, and it was previously wrong in one of the two. Both screens passed
 * [AppSpacing.Related] unconditionally, which is correct at an expanded width — the heading is the
 * first thing in its own pane, so a section break above it would only push it away from the pane's
 * top edge. In one scroll it is the *middle* of a column, and 8dp there made the transcript read as
 * a fourth item after the actions rather than as the screen's second half.
 *
 * So: a full section break where the heading divides content, and a pane title where it leads a
 * pane. This is the whole review transition; there is deliberately no divider and no per-item
 * reveal, because twenty cards animating in one after another is theatre, and the learner came here
 * to read them.
 */
@Composable
internal fun ResultReviewHeading(text: String) {
    SectionHeading(
        text = text,
        topPadding = if (LocalAppWindowSizeClass.current.isExpanded) {
            AppSpacing.Related
        } else {
            AppSpacing.Section
        },
    )
}

@Composable
private fun AssessmentResultPane(
    modifier: Modifier,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxHeight(),
        contentPadding = appScreenContentPadding(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Comfortable),
        content = content,
    )
}
