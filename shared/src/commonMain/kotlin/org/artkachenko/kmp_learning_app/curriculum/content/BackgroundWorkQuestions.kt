package org.artkachenko.kmp_learning_app.curriculum.content

internal val backgroundWorkQuestions = listOf(
    question(
        id = "workmanager_constraints_001",
        topicId = "background_work",
        subtopicId = "workmanager_constraints",
        text = "A deferrable sync should survive process death and run only when network connectivity is available. Which API is the best fit?",
        correctAnswerIds = listOf("workmanager_constraints_001_c"),
        explanation = "WorkManager is intended for deferrable persistent work and supports constraints such as required network connectivity.",
        sources = listOf(workManagerSource),
        answers = listOf(
            answer("workmanager_constraints_001_a", "A raw Thread stored in a singleton."),
            answer("workmanager_constraints_001_b", "A ViewModel coroutine."),
            answer("workmanager_constraints_001_c", "WorkManager with a network constraint."),
            answer("workmanager_constraints_001_d", "A composable side effect with no persistence."),
        ),
    ),
    question(
        id = "foreground_services_001",
        topicId = "background_work",
        subtopicId = "foreground_services",
        text = "When is a foreground service generally appropriate?",
        correctAnswerIds = listOf("foreground_services_001_a"),
        explanation = "Foreground services are for user-noticeable work that must continue while the app is not in the foreground and must show an ongoing notification.",
        sources = listOf(backgroundWorkSource, foregroundServiceTypesSource),
        answers = listOf(
            answer("foreground_services_001_a", "For user-noticeable ongoing work that must continue and be visible to the user."),
            answer("foreground_services_001_b", "For any short database query from a ViewModel."),
            answer("foreground_services_001_c", "For replacing every WorkManager task."),
            answer("foreground_services_001_d", "For hiding background work from system restrictions."),
        ),
    ),
    question(
        id = "foreground_service_types_001",
        topicId = "background_work",
        subtopicId = "foreground_service_types",
        text = "For apps targeting modern Android versions, why do foreground service types matter?",
        correctAnswerIds = listOf("foreground_service_types_001_b"),
        explanation = "Foreground service types declare the category of foreground work and can carry type-specific permissions and runtime prerequisites.",
        sources = listOf(foregroundServiceTypesSource),
        answers = listOf(
            answer("foreground_service_types_001_a", "They choose the app's launcher icon."),
            answer("foreground_service_types_001_b", "They declare the foreground work category and related requirements."),
            answer("foreground_service_types_001_c", "They make a service run without a notification."),
            answer("foreground_service_types_001_d", "They replace runtime permissions."),
        ),
    ),
    question(
        id = "background_limits_001",
        topicId = "background_work",
        subtopicId = "background_limits",
        text = "Why should Android apps choose background APIs with OS limits in mind?",
        correctAnswerIds = listOf("background_limits_001_d"),
        explanation = "Android applies background execution and battery restrictions. Choosing the wrong API can make work unreliable, blocked, or wasteful.",
        sources = listOf(backgroundWorkSource),
        answers = listOf(
            answer("background_limits_001_a", "Because background restrictions apply only to debug builds."),
            answer("background_limits_001_b", "Because every background task gets unlimited CPU time."),
            answer("background_limits_001_c", "Because WorkManager cannot use constraints."),
            answer("background_limits_001_d", "Because the OS restricts background execution to protect battery and system health."),
        ),
    ),
)

