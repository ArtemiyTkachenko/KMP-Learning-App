package org.artkachenko.kmp_learning_app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.app_retry
import org.artkachenko.kmp_learning_app.ui.theme.AppMotion
import org.jetbrains.compose.resources.stringResource

/**
 * Cross-fades between a screen's loading, empty, error, and content states.
 *
 * Every screen resolves its state in a `when` block, and each branch simply replaced the last, so
 * content appeared the instant a read finished — a spinner one frame and a full list the next. That
 * hard cut is what made a fast load look like a glitch and a slow one look broken.
 *
 * The fade is short and carries no movement. It exists to say that one thing became another, which
 * is the functional purpose E13-02 requires; sliding content in as well would be decoration.
 *
 * [contentKey] is what decides whether a transition runs, and it defaults to the state's class
 * rather than the state itself. Keying on the whole state would restart the fade on every data
 * change, so a list would flicker each time one answer was recorded. The state is still passed
 * through to [content] in full, which is what lets the outgoing branch keep rendering the old value
 * while it fades — keying alone would redraw both halves with the new state and defeat the fade.
 */
@Composable
internal fun <S : Any> ScreenStateTransition(
    state: S,
    modifier: Modifier = Modifier,
    contentKey: (S) -> Any = { it::class },
    content: @Composable AnimatedContentScope.(S) -> Unit,
) {
    AnimatedContent(
        targetState = state,
        modifier = modifier,
        contentKey = contentKey,
        transitionSpec = {
            fadeIn(
                tween(
                    durationMillis = AppMotion.StateChangeDurationMillis,
                    easing = AppMotion.EmphasizedDecelerateEasing,
                ),
            ) togetherWith fadeOut(
                tween(
                    durationMillis = AppMotion.StateChangeDurationMillis / 2,
                    easing = AppMotion.EmphasizedAccelerateEasing,
                ),
            )
        },
        label = "screenState",
        content = content,
    )
}

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

/**
 * An empty state that offers a way forward.
 *
 * The plain message states left the user on a dead-end screen: the progress dashboard told them to
 * complete an assessment without giving them anything to tap to get there.
 */
@Composable
internal fun ScreenAction(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = Color.Unspecified,
) {
    ScreenStatus(modifier) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = if (iconTint == Color.Unspecified) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    iconTint
                },
                modifier = Modifier.size(40.dp),
            )
        }
        ScreenStatusText(message)
        detail?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Button(onClick = onAction) { Text(actionLabel) }
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
