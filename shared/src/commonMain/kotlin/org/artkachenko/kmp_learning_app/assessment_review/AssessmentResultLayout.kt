package org.artkachenko.kmp_learning_app.assessment_review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.artkachenko.kmp_learning_app.ui.AppTwoPaneRow
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
