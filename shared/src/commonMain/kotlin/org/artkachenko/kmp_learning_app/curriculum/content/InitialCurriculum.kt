package org.artkachenko.kmp_learning_app.curriculum.content

import org.artkachenko.kmp_learning_app.curriculum.Curriculum

internal val initialCurriculum = Curriculum(
    topics = initialTopics,
    subtopics = initialSubtopics,
    questions = buildList {
        addAll(androidPlatformQuestions)
        addAll(lifecycleNavigationQuestions)
        addAll(androidUiQuestions)
        addAll(kotlinLanguageQuestions)
        addAll(asyncReactiveQuestions)
        addAll(architectureQuestions)
        addAll(dependencyInjectionQuestions)
        addAll(localDataQuestions)
        addAll(networkingQuestions)
        addAll(backgroundWorkQuestions)
        addAll(notificationQuestions)
        addAll(testingQuestions)
        addAll(performanceQuestions)
        addAll(securityQuestions)
        addAll(buildDeliveryQuestions)
        addAll(mobileSystemDesignQuestions)
        addAll(kmpQuestions)
    },
)

