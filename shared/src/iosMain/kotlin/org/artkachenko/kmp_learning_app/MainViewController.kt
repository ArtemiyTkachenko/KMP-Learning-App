package org.artkachenko.kmp_learning_app

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    startIosLocalDataGraph()

    return ComposeUIViewController {
        IosAppRoot()
    }
}
