package org.artkachenko.kmp_learning_app.curriculum.content

internal val performanceQuestions = listOf(
    question(
        id = "anr_001",
        topicId = "performance",
        subtopicId = "anr",
        text = "What is a common cause of an ANR in an Android app?",
        correctAnswerIds = listOf("anr_001_c"),
        explanation = "ANRs occur when the app does not respond to input or lifecycle-related work in time. Blocking the main thread with expensive work is a common cause.",
        sources = listOf(androidPerformanceSource, androidProcessesThreadsSource),
        answers = listOf(
            answer("anr_001_a", "Using immutable data classes."),
            answer("anr_001_b", "Collecting a StateFlow from Compose."),
            answer("anr_001_c", "Blocking the main thread with long-running work."),
            answer("anr_001_d", "Using a version catalog."),
        ),
    ),
    question(
        id = "memory_leaks_001",
        topicId = "performance",
        subtopicId = "memory_leaks",
        text = "How can retaining an Activity reference in a singleton cause a memory leak?",
        correctAnswerIds = listOf("memory_leaks_001_a"),
        explanation = "A singleton can outlive an Activity instance. If it strongly references the Activity, the old Activity and its Views may remain reachable after they should be collected.",
        sources = listOf(androidPerformanceSource),
        answers = listOf(
            answer("memory_leaks_001_a", "The singleton can keep the Activity reachable after its lifecycle should end."),
            answer("memory_leaks_001_b", "The Activity becomes stored in Android resources."),
            answer("memory_leaks_001_c", "The garbage collector never runs in Android apps."),
            answer("memory_leaks_001_d", "The Activity is converted into a Service."),
        ),
    ),
    question(
        id = "compose_recomposition_performance_001",
        topicId = "performance",
        subtopicId = "compose_recomposition_performance",
        text = "What is a reasonable way to reduce unnecessary recomposition work in Compose?",
        correctAnswerIds = listOf("compose_recomposition_performance_001_d"),
        explanation = "Stable immutable parameters and careful state ownership help Compose skip work when inputs have not changed. The goal is not to prevent recomposition entirely.",
        sources = listOf(composeStabilitySource, composeStateHoistingSource),
        answers = listOf(
            answer("compose_recomposition_performance_001_a", "Store all UI state in global mutable variables."),
            answer("compose_recomposition_performance_001_b", "Avoid using state anywhere in the UI."),
            answer("compose_recomposition_performance_001_c", "Make every parameter nullable."),
            answer("compose_recomposition_performance_001_d", "Use stable immutable models and keep state changes scoped to where they are needed."),
        ),
    ),
    question(
        id = "startup_performance_001",
        topicId = "performance",
        subtopicId = "startup_performance",
        text = "Why should expensive initialization be treated carefully during app startup?",
        correctAnswerIds = listOf("startup_performance_001_b"),
        explanation = "Too much synchronous startup work delays first draw and can hurt launch performance. Non-critical work should be deferred or made lazy where appropriate.",
        sources = listOf(appStartupSource),
        answers = listOf(
            answer("startup_performance_001_a", "Because startup code cannot allocate objects."),
            answer("startup_performance_001_b", "Because synchronous startup work can delay first draw and user interaction."),
            answer("startup_performance_001_c", "Because startup work always runs after the app is closed."),
            answer("startup_performance_001_d", "Because Android requires every dependency to be initialized in Application.onCreate()."),
        ),
    ),
)

