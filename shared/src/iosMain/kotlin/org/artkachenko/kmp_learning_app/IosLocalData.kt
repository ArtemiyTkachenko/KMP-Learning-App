package org.artkachenko.kmp_learning_app

import org.artkachenko.kmp_learning_app.data.local.assessment.assessmentDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDataInitializer
import org.artkachenko.kmp_learning_app.data.local.curriculum.curriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.iosCurriculumDataModule
import org.artkachenko.kmp_learning_app.topic_study.topicStudyPresentationModule
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

public fun startIosLocalDataGraph() {
    if (KoinPlatform.getKoinOrNull() != null) return

    startKoin {
        modules(
            curriculumDataModule,
            assessmentDataModule,
            topicStudyPresentationModule,
            iosCurriculumDataModule,
        )
    }
}

public suspend fun initializeIosLocalData() {
    KoinPlatform.getKoin()
        .get<CurriculumDataInitializer>()
        .initialize()
}
