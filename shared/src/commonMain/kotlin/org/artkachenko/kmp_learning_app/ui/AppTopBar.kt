package org.artkachenko.kmp_learning_app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.app_back
import org.artkachenko.kmp_learning_app.ui.theme.AppMotion
import org.jetbrains.compose.resources.stringResource

/**
 * Shared top bar for every screen that can be navigated back from.
 *
 * Previously this lived in `topic_study/topic_detail/` as `TopicStudyTopAppBar` while seven
 * feature packages imported it, and took its content description from `topic_detail_back`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    subtitle: String? = null,
    isSubtitleVisible: Boolean = true,
) {
    TopAppBar(
        title = {
            if (subtitle == null) {
                Text(text = title)
            } else {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    AnimatedVisibility(
                        visible = isSubtitleVisible,
                        enter = fadeIn(AppMotion.effectSpec()) +
                            expandVertically(
                                animationSpec = AppMotion.effectSpec(),
                                expandFrom = Alignment.Top,
                            ),
                        exit = fadeOut(AppMotion.effectSpec()) +
                            shrinkVertically(
                                animationSpec = AppMotion.effectSpec(),
                                shrinkTowards = Alignment.Top,
                            ),
                    ) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        navigationIcon = {
            // Null for a navigation-bar destination: there is nothing above it to go back to,
            // and offering the affordance anyway would be a dead control.
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = AppIcons.ArrowBack,
                        contentDescription = stringResource(Res.string.app_back),
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

/**
 * The scroll behaviour every screen's top bar uses, paired with [appTopBarNestedScroll].
 *
 * The bar previously painted one flat colour whatever was underneath it, so a long list slid behind
 * it with nothing marking where the bar ended and the content began. A bar that takes on its
 * container colour once content has scrolled beneath it is the standard Material cue for that
 * boundary, and its absence is a large part of why the screens read as unfinished.
 *
 * Pinned rather than `enterAlways` or `exitUntilCollapsed`: those hide or shrink the bar as the
 * learner scrolls, which would take the title and the back affordance off screen during an
 * assessment. The pinned behaviour changes only the colour, so nothing moves and no existing
 * assertion about a visible title or back button can break.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberAppTopBarScrollBehavior(): TopAppBarScrollBehavior =
    TopAppBarDefaults.pinnedScrollBehavior()
