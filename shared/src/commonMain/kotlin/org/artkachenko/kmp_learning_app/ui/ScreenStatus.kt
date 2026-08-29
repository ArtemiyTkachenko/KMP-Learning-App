package org.artkachenko.kmp_learning_app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.app_retry
import org.jetbrains.compose.resources.stringResource

/**
 * Loading, empty, and error presentation shared by every screen.
 *
 * Each screen used to carry its own centred wrapper — `CenteredState`, `Message`,
 * `MessageContent`, `ResultMessage`, `ProgressMessage`, `MistakeReviewMessage`, and the topic
 * browser's `LoadingState`/`MessageState`/`ErrorState`. They had drifted into three different
 * spacings, so the gap between an error message and its Retry button depended on which screen you
 * were looking at. One implementation keeps those states identical by construction.
 */
@Composable
internal fun ScreenStatus(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
internal fun ScreenLoading(
    message: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    ScreenStatus(modifier) {
        CircularProgressIndicator(Modifier.testTag(testTag))
        ScreenStatusText(message)
    }
}

@Composable
internal fun ScreenMessage(
    message: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    ScreenStatus(modifier) {
        ScreenStatusText(message)
        detail?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun ScreenError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenStatus(modifier) {
        ScreenStatusText(message)
        Button(onClick = onRetry) {
            Text(stringResource(Res.string.app_retry))
        }
    }
}

@Composable
private fun ScreenStatusText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}
