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
    onBack: () -> Unit,
) {
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = AppIcons.ArrowBack,
                    contentDescription = stringResource(Res.string.app_back),
                )
            }
        },
    )
}

