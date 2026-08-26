package org.artkachenko.kmp_learning_app

import android.app.Application

class KmpLearningApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startAndroidLocalDataGraph(this)
    }
}
