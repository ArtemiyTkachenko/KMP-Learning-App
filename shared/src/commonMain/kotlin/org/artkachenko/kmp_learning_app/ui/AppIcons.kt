package org.artkachenko.kmp_learning_app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The small set of Material symbols this product actually uses.
 *
 * These are declared locally rather than pulled from `material-icons`: the core artifact would be
 * a new dependency for a handful of glyphs, and the extended one adds materially to the JS and
 * Wasm bundles now that the browser hosts are real. Paths are the standard Material 24dp shapes.
 * Anything directional sets `autoMirror` so right-to-left layouts stay correct.
 */
internal object AppIcons {
    val ArrowBack: ImageVector by lazy {
        icon("ArrowBack", autoMirror = true) {
            moveTo(20f, 11f); horizontalLineTo(7.83f); lineTo(13.42f, 5.41f); lineTo(12f, 4f)
            lineTo(4f, 12f); lineTo(12f, 20f); lineTo(13.41f, 18.59f); lineTo(7.83f, 13f)
            horizontalLineTo(20f); verticalLineTo(11f); close()
        }
    }

    val ChevronRight: ImageVector by lazy {
        icon("ChevronRight", autoMirror = true) {
            moveTo(10f, 6f); lineTo(8.59f, 7.41f); lineTo(13.17f, 12f); lineTo(8.59f, 16.59f)
            lineTo(10f, 18f); lineTo(16f, 12f); close()
        }
    }

    val CheckCircle: ImageVector by lazy {
        icon("CheckCircle") {
            moveTo(12f, 2f); curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
            curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
            curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f); close()
            moveTo(10f, 17f); lineTo(5f, 12f); lineTo(6.41f, 10.59f); lineTo(10f, 14.17f)
            lineTo(17.59f, 6.58f); lineTo(19f, 8f); close()
        }
    }

    val Warning: ImageVector by lazy {
        icon("Warning") {
            moveTo(1f, 21f); horizontalLineTo(23f); lineTo(12f, 2f); close()
            moveTo(13f, 18f); horizontalLineTo(11f); verticalLineTo(16f); horizontalLineTo(13f); close()
            moveTo(13f, 14f); horizontalLineTo(11f); verticalLineTo(10f); horizontalLineTo(13f); close()
        }
    }

    val OpenInNew: ImageVector by lazy {
        icon("OpenInNew") {
            moveTo(19f, 19f); horizontalLineTo(5f); verticalLineTo(5f); horizontalLineTo(12f)
            verticalLineTo(3f); horizontalLineTo(5f); curveTo(3.89f, 3f, 3f, 3.9f, 3f, 5f)
            verticalLineTo(19f); curveTo(3f, 20.1f, 3.89f, 21f, 5f, 21f); horizontalLineTo(19f)
            curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f); verticalLineTo(12f); horizontalLineTo(19f)
            close()
            moveTo(14f, 3f); verticalLineTo(5f); horizontalLineTo(17.59f); lineTo(7.76f, 14.83f)
            lineTo(9.17f, 16.24f); lineTo(19f, 6.41f); verticalLineTo(10f); horizontalLineTo(21f)
            verticalLineTo(3f); close()
        }
    }

    val Insights: ImageVector by lazy {
        icon("Insights") {
            moveTo(21f, 8f); horizontalLineTo(19f); verticalLineTo(20f); horizontalLineTo(21f); close()
            moveTo(13f, 4f); horizontalLineTo(11f); verticalLineTo(20f); horizontalLineTo(13f); close()
            moveTo(5f, 12f); horizontalLineTo(3f); verticalLineTo(20f); horizontalLineTo(5f); close()
        }
    }
}

private fun icon(
    name: String,
    autoMirror: Boolean = false,
    pathBuilder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = autoMirror,
    ).apply {
        path(fill = SolidColor(Color.Black), pathBuilder = pathBuilder)
    }.build()
