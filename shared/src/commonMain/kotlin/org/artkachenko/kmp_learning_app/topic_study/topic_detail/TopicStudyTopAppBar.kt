package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.topic_detail_back
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TopicStudyTopAppBar(
    title: String,
    onBack: () -> Unit,
) {
    val backDescription = stringResource(Res.string.topic_detail_back)

    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            IconButton(
                modifier = Modifier.semantics {
                    contentDescription = backDescription
                },
                onClick = onBack,
            ) {
                Text(text = "←")
            }
        },
    )
}
