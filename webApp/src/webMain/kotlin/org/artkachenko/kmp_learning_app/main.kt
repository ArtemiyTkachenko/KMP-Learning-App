package org.artkachenko.kmp_learning_app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startWebLocalDataGraph()

    ComposeViewport {
        WebAppRoot()
    }
}
