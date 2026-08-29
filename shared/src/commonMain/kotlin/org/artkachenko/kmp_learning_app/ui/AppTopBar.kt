package org.artkachenko.kmp_learning_app.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
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
                    imageVector = BackArrowIcon,
                    contentDescription = stringResource(Res.string.app_back),
                )
            }
        },
    )
}

/**
 * The back arrow was a `Text("←")` glyph, which does not tint, size, or mirror like an icon.
 *
 * It is declared here rather than pulled from `material-icons` on purpose: the core artifact
 * would be a new dependency for a single glyph, and the extended one would add materially to the
 * JS and Wasm bundles now that the browser hosts are real. `autoMirror` gives the RTL flip that
 * `Icons.AutoMirrored` would have provided.
 */
private val BackArrowIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "AppBackArrow",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = true,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(20f, 11f)
            horizontalLineTo(7.83f)
            lineTo(13.42f, 5.41f)
            lineTo(12f, 4f)
            lineTo(4f, 12f)
            lineTo(12f, 20f)
            lineTo(13.41f, 18.59f)
            lineTo(7.83f, 13f)
            horizontalLineTo(20f)
            verticalLineTo(11f)
            close()
        }
    }.build()
}
