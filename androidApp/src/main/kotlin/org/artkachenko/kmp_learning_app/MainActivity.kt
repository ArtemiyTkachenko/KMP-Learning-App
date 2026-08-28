package org.artkachenko.kmp_learning_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Content is set unconditionally so a failed initialization renders AppRoot's
        // error and retry states instead of leaving the activity without any content.
        setContent {
            AppRoot { initializeAndroidLocalData() }
        }
    }
}
