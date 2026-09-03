package org.artkachenko.kmp_learning_app

import android.app.Application
import org.artkachenko.kmp_learning_app.data.local.assessment.assessmentDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDataInitializer
import org.artkachenko.kmp_learning_app.data.local.curriculum.androidCurriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.curriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.saved_questions.savedQuestionDataModule
import org.artkachenko.kmp_learning_app.topic_study.topicStudyPresentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

public fun startAndroidLocalDataGraph(application: Application) {
    if (GlobalContext.getOrNull() != null) return

    startKoin {
        androidContext(application.applicationContext)
        modules(
            curriculumDataModule,
            assessmentDataModule,
            savedQuestionDataModule,
            topicStudyPresentationModule,
            androidCurriculumDataModule,
        )
    }
}

public suspend fun initializeAndroidLocalData() {
    GlobalContext.get()
        .get<CurriculumDataInitializer>()
        .initialize()
}
