package org.artkachenko.kmp_learning_app

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.artkachenko.kmp_learning_app.product.ProductMetadata

fun main() {
    startDesktopLocalDataGraph()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            // The canonical product name, not a literal: the window title, the distribution
            // metadata and the in-app About section all name the product once.
            title = ProductMetadata.NAME,
            // The same mark the packaged distributions carry, for the window and the task bar.
            icon = painterResource("app-icon.png"),
        ) {
            DesktopAppRoot()
        }
    }
}
