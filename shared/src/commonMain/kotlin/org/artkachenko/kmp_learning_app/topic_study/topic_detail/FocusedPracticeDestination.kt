package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.focused_practice_title
import kmp_learning_app.shared.generated.resources.topic_detail_back
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FocusedPracticeDestination(
    scopeLabel: String,
    questionCount: Int,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.focused_practice_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(text = scopeLabel, modifier = Modifier.padding(top = 12.dp))
        Text(text = questionCount.toString(), modifier = Modifier.padding(top = 4.dp))
        Button(onClick = onBack, modifier = Modifier.padding(top = 20.dp)) {
            Text(text = stringResource(Res.string.topic_detail_back))
        }
    }
}
