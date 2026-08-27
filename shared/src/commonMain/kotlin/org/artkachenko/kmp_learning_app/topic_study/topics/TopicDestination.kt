package org.artkachenko.kmp_learning_app.topic_study.topics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.topic_detail_back
import kmp_learning_app.shared.generated.resources.topic_detail_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TopicDestination(
    topicId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.topic_detail_title, topicId),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Button(
            modifier = Modifier.padding(top = 24.dp),
            onClick = onBack,
        ) {
            Text(text = stringResource(Res.string.topic_detail_back))
        }
    }
}
