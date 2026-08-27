package org.artkachenko.kmp_learning_app

import org.artkachenko.kmp_learning_app.data.local.assessment.assessmentDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDataInitializer
import org.artkachenko.kmp_learning_app.data.local.curriculum.curriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.jvmCurriculumDataModule
import org.artkachenko.kmp_learning_app.topic_study.topicStudyPresentationModule
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

public fun startDesktopLocalDataGraph() {
    if (GlobalContext.getOrNull() != null) return

    startKoin {
        modules(
            curriculumDataModule,
            assessmentDataModule,
            topicStudyPresentationModule,
            jvmCurriculumDataModule,
        )
    }
}

public suspend fun initializeDesktopLocalData() {
    GlobalContext.get()
        .get<CurriculumDataInitializer>()
        .initialize()
}
