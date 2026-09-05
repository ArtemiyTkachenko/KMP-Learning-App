package org.artkachenko.kmp_learning_app

import org.artkachenko.kmp_learning_app.curriculum.learning.content.learningContentModule
import org.artkachenko.kmp_learning_app.data.local.assessment.assessmentDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDataInitializer
import org.artkachenko.kmp_learning_app.data.local.curriculum.curriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.webCurriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.saved_questions.savedQuestionDataModule
import org.artkachenko.kmp_learning_app.topic_study.topicStudyPresentationModule
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

public fun startWebLocalDataGraph() {
    if (KoinPlatform.getKoinOrNull() != null) return

    startKoin {
        modules(
            curriculumDataModule,
            learningContentModule,
            assessmentDataModule,
            savedQuestionDataModule,
            topicStudyPresentationModule,
            webCurriculumDataModule,
        )
    }
}

public suspend fun initializeWebLocalData() {
    KoinPlatform.getKoin()
        .get<CurriculumDataInitializer>()
        .initialize()
}
