package org.artkachenko.kmp_learning_app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() {
    startDesktopLocalDataGraph()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "KMP-Learning-App",
        ) {
            DesktopAppRoot()
        }
    }
}
