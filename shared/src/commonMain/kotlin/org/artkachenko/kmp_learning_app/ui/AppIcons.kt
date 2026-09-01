package org.artkachenko.kmp_learning_app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
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

    val Search: ImageVector by lazy {
        icon("Search") {
            moveTo(9.5f, 3f)
            curveTo(5.91f, 3f, 3f, 5.91f, 3f, 9.5f)
            curveTo(3f, 13.09f, 5.91f, 16f, 9.5f, 16f)
            curveTo(11.11f, 16f, 12.59f, 15.41f, 13.73f, 14.44f)
            lineTo(19.49f, 20.19f)
            lineTo(20.9f, 18.78f)
            lineTo(15.14f, 13.03f)
            curveTo(16.01f, 11.95f, 16.5f, 10.57f, 16.5f, 9.5f)
            curveTo(16.5f, 5.91f, 13.59f, 3f, 9.5f, 3f)
            close()
            moveTo(9.5f, 5f)
            curveTo(11.99f, 5f, 14f, 7.01f, 14f, 9.5f)
            curveTo(14f, 11.99f, 11.99f, 14f, 9.5f, 14f)
            curveTo(7.01f, 14f, 5f, 11.99f, 5f, 9.5f)
            curveTo(5f, 7.01f, 7.01f, 5f, 9.5f, 5f)
            close()
        }
    }

    val Close: ImageVector by lazy {
        icon("Close") {
            moveTo(18.3f, 5.71f)
            lineTo(16.89f, 4.29f)
            lineTo(12f, 9.17f)
            lineTo(7.11f, 4.29f)
            lineTo(5.7f, 5.71f)
            lineTo(10.59f, 10.59f)
            lineTo(5.7f, 15.48f)
            lineTo(7.11f, 16.89f)
            lineTo(12f, 12f)
            lineTo(16.89f, 16.89f)
            lineTo(18.3f, 15.48f)
            lineTo(13.41f, 10.59f)
            close()
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

    val Topics: ImageVector by lazy {
        icon("Topics") {
            moveTo(3f, 5f); horizontalLineTo(21f); verticalLineTo(7f); horizontalLineTo(3f); close()
            moveTo(3f, 11f); horizontalLineTo(21f); verticalLineTo(13f); horizontalLineTo(3f); close()
            moveTo(3f, 17f); horizontalLineTo(15f); verticalLineTo(19f); horizontalLineTo(3f); close()
        }
    }

    val Interview: ImageVector by lazy {
        icon("Interview", autoMirror = true) {
            moveTo(8f, 5f); lineTo(19f, 12f); lineTo(8f, 19f); close()
        }
    }

    val Insights: ImageVector by lazy {
        icon("Insights") {
            moveTo(21f, 8f); horizontalLineTo(19f); verticalLineTo(20f); horizontalLineTo(21f); close()
            moveTo(13f, 4f); horizontalLineTo(11f); verticalLineTo(20f); horizontalLineTo(13f); close()
            moveTo(5f, 12f); horizontalLineTo(3f); verticalLineTo(20f); horizontalLineTo(5f); close()
        }
    }

    // Topic visual identity glyphs, in curriculum order. Each one exists because exactly one
    // Topic maps to it in TopicVisualIdentity; they are deliberately plain monochrome symbols so
    // the whole set reads as one icon language rather than a mix of logos and category art.

    /** Android Platform & Application Model. */
    val Smartphone: ImageVector by lazy {
        icon("Smartphone") {
            rect(6f, 1f, 18f, 23f)
            rect(8f, 4f, 16f, 20f, cutOut = true)
        }
    }

    /** Lifecycle, State & Navigation. */
    val Route: ImageVector by lazy {
        icon("Route", autoMirror = true) {
            rect(3f, 5f, 13f, 7f)
            rect(11f, 5f, 13f, 17f)
            rect(11f, 15f, 17f, 17f)
            moveTo(16f, 12f); lineTo(21.5f, 16f); lineTo(16f, 20f); close()
        }
    }

    /** UI — Views & Jetpack Compose. */
    val Layout: ImageVector by lazy {
        icon("Layout") {
            rect(3f, 3f, 11f, 11f)
            rect(13f, 3f, 21f, 8f)
            rect(13f, 10f, 21f, 21f)
            rect(3f, 13f, 11f, 21f)
        }
    }

    /** Kotlin Language & JVM Fundamentals. */
    val Code: ImageVector by lazy {
        icon("Code") {
            moveTo(9.4f, 16.6f); lineTo(4.8f, 12f); lineTo(9.4f, 7.4f); lineTo(8f, 6f)
            lineTo(2f, 12f); lineTo(8f, 18f); close()
            moveTo(14.6f, 16.6f); lineTo(19.2f, 12f); lineTo(14.6f, 7.4f); lineTo(16f, 6f)
            lineTo(22f, 12f); lineTo(16f, 18f); close()
        }
    }

    /** Coroutines, Flow & Reactive Programming. */
    val Branch: ImageVector by lazy {
        icon("Branch") {
            moveTo(14f, 4f); lineTo(16.29f, 6.29f); lineTo(13.41f, 9.17f); lineTo(14.83f, 10.59f)
            lineTo(17.71f, 7.71f); lineTo(20f, 10f); lineTo(20f, 4f); close()
            moveTo(10f, 4f); lineTo(4f, 4f); lineTo(4f, 10f); lineTo(6.29f, 7.71f)
            lineTo(11f, 12.41f); lineTo(11f, 20f); lineTo(13f, 20f); lineTo(13f, 11.59f)
            lineTo(7.71f, 6.29f); close()
        }
    }

    /** Application Architecture & Design Principles. */
    val AccountTree: ImageVector by lazy {
        icon("AccountTree") {
            moveTo(22f, 11f); lineTo(22f, 3f); lineTo(15f, 3f); lineTo(15f, 6f); lineTo(9f, 6f)
            lineTo(9f, 3f); lineTo(2f, 3f); lineTo(2f, 11f); lineTo(9f, 11f); lineTo(9f, 8f)
            lineTo(11f, 8f); lineTo(11f, 18f); lineTo(15f, 18f); lineTo(15f, 21f); lineTo(22f, 21f)
            lineTo(22f, 13f); lineTo(15f, 13f); lineTo(15f, 16f); lineTo(13f, 16f); lineTo(13f, 8f)
            lineTo(15f, 8f); lineTo(15f, 11f); close()
        }
    }

    /** Dependency Injection. */
    val Hub: ImageVector by lazy {
        icon("Hub") {
            rect(11.25f, 3f, 12.75f, 21f)
            rect(3f, 11.25f, 21f, 12.75f)
            circle(12f, 12f, 4f)
            circle(12f, 3.5f, 2.5f)
            circle(12f, 20.5f, 2.5f)
            circle(3.5f, 12f, 2.5f)
            circle(20.5f, 12f, 2.5f)
        }
    }

    /** Local Persistence & Offline Data. */
    val Database: ImageVector by lazy {
        icon("Database") {
            moveTo(4f, 6f)
            arcTo(8f, 3f, 0f, false, true, 20f, 6f)
            lineTo(20f, 18f)
            arcTo(8f, 3f, 0f, false, true, 4f, 18f)
            close()
            rect(4f, 10f, 20f, 11.5f, cutOut = true)
            rect(4f, 14f, 20f, 15.5f, cutOut = true)
        }
    }

    /** Networking & Serialization. */
    val Globe: ImageVector by lazy {
        icon("Globe") {
            circle(12f, 12f, 9f)
            circle(12f, 12f, 7.3f, cutOut = true)
            rect(3f, 11.25f, 21f, 12.75f)
            ellipse(12f, 12f, 4.4f, 7.3f)
            ellipse(12f, 12f, 2.8f, 5.8f, cutOut = true)
        }
    }

    /** Background Work & OS Constraints. */
    val Schedule: ImageVector by lazy {
        icon("Schedule") {
            circle(12f, 12f, 9f)
            circle(12f, 12f, 7.2f, cutOut = true)
            rect(11.25f, 6.5f, 12.75f, 12.75f)
            rect(11.25f, 11.25f, 16.5f, 12.75f)
        }
    }

    /** Notifications & Push Messaging. */
    val Notifications: ImageVector by lazy {
        icon("Notifications") {
            moveTo(12f, 22f)
            curveTo(13.1f, 22f, 14f, 21.1f, 14f, 20f)
            horizontalLineTo(10f)
            curveTo(10f, 21.1f, 10.9f, 22f, 12f, 22f)
            close()
            moveTo(18f, 16f)
            verticalLineTo(11f)
            curveTo(18f, 7.93f, 16.37f, 5.36f, 13.5f, 4.68f)
            verticalLineTo(4f)
            curveTo(13.5f, 3.17f, 12.83f, 2.5f, 12f, 2.5f)
            curveTo(11.17f, 2.5f, 10.5f, 3.17f, 10.5f, 4f)
            verticalLineTo(4.68f)
            curveTo(7.64f, 5.36f, 6f, 7.92f, 6f, 11f)
            verticalLineTo(16f)
            lineTo(4f, 18f)
            verticalLineTo(19f)
            horizontalLineTo(20f)
            verticalLineTo(18f)
            close()
        }
    }

    /** Testing & Testability. */
    val Science: ImageVector by lazy {
        icon("Science") {
            moveTo(9f, 2f); lineTo(15f, 2f); lineTo(15f, 4f); lineTo(14f, 4f); lineTo(14f, 9.5f)
            lineTo(20.5f, 20.5f); lineTo(20.5f, 22f); lineTo(3.5f, 22f); lineTo(3.5f, 20.5f)
            lineTo(10f, 9.5f); lineTo(10f, 4f); lineTo(9f, 4f); close()
        }
    }

    /** Performance, Memory & Debugging. */
    val Speed: ImageVector by lazy {
        icon("Speed") {
            moveTo(3f, 16.5f)
            arcTo(9f, 9f, 0f, false, true, 21f, 16.5f)
            lineTo(18f, 16.5f)
            arcTo(6f, 6f, 0f, false, false, 6f, 16.5f)
            close()
            moveTo(10.6f, 14.6f); lineTo(16.4f, 7.9f); lineTo(17.7f, 9.1f); lineTo(12.2f, 15.6f)
            close()
        }
    }

    /** Security, Privacy & Permissions. */
    val Shield: ImageVector by lazy {
        icon("Shield") {
            moveTo(12f, 2f)
            lineTo(20f, 5f)
            verticalLineTo(11f)
            curveTo(20f, 16.5f, 16.6f, 21.3f, 12f, 22.5f)
            curveTo(7.4f, 21.3f, 4f, 16.5f, 4f, 11f)
            verticalLineTo(5f)
            close()
        }
    }

    /** Build System, Modularization & Delivery. */
    val Package: ImageVector by lazy {
        icon("Package") {
            rect(3f, 3f, 21f, 7f)
            rect(4f, 8f, 20f, 21f)
            rect(6f, 10f, 18f, 19f, cutOut = true)
            rect(9.5f, 11.5f, 14.5f, 13.5f)
        }
    }

    /** Mobile System Design. */
    val Schema: ImageVector by lazy {
        icon("Schema") {
            rect(9f, 2f, 15f, 6f)
            rect(11.25f, 6f, 12.75f, 12f)
            rect(5f, 11.25f, 19f, 12.75f)
            rect(5f, 12f, 6.5f, 17f)
            rect(17.5f, 12f, 19f, 17f)
            rect(2f, 17f, 9.5f, 21f)
            rect(14.5f, 17f, 22f, 21f)
        }
    }

    /** Kotlin Multiplatform & Compose Multiplatform. */
    val Devices: ImageVector by lazy {
        icon("Devices") {
            rect(2f, 4f, 15f, 14f)
            rect(4f, 6f, 13f, 12f, cutOut = true)
            rect(6f, 15f, 11f, 17f)
            rect(16f, 8f, 22f, 21f)
            rect(17.5f, 10f, 20.5f, 19f, cutOut = true)
        }
    }
}

/**
 * Axis-aligned rectangle.
 *
 * `cutOut` reverses the winding so the rectangle punches a hole in the shape it sits inside,
 * which is how these glyphs get outlines (device bezels, package walls) under the non-zero fill
 * rule without needing stroked paths.
 */
private fun PathBuilder.rect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    cutOut: Boolean = false,
) {
    if (cutOut) {
        moveTo(left, bottom); lineTo(right, bottom); lineTo(right, top); lineTo(left, top)
    } else {
        moveTo(left, top); lineTo(right, top); lineTo(right, bottom); lineTo(left, bottom)
    }
    close()
}

private fun PathBuilder.circle(
    centerX: Float,
    centerY: Float,
    radius: Float,
    cutOut: Boolean = false,
) = ellipse(centerX, centerY, radius, radius, cutOut)

/** Two half arcs, wound the same way as [rect] so `cutOut` behaves identically. */
private fun PathBuilder.ellipse(
    centerX: Float,
    centerY: Float,
    radiusX: Float,
    radiusY: Float,
    cutOut: Boolean = false,
) {
    val clockwise = !cutOut
    moveTo(centerX - radiusX, centerY)
    arcTo(radiusX, radiusY, 0f, false, clockwise, centerX + radiusX, centerY)
    arcTo(radiusX, radiusY, 0f, false, clockwise, centerX - radiusX, centerY)
    close()
}

private fun icon(
    name: String,
    autoMirror: Boolean = false,
    pathBuilder: PathBuilder.() -> Unit,
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
