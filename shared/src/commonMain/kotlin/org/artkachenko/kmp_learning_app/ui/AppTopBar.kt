package org.artkachenko.kmp_learning_app.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.app_back
import org.jetbrains.compose.resources.stringResource

/**
 * Shared top bar for every screen that can be navigated back from.
 *
 * Previously this lived in `topic_study/topic_detail/` as `TopicStudyTopAppBar` while seven
 * feature packages imported it, and took its content description from `topic_detail_back`.
 */
@Composable
internal fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
) {
    TopAppBar(
        title = { Text(text = title) },
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
    )
}

