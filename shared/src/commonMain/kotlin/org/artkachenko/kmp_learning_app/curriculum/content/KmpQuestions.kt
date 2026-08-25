package org.artkachenko.kmp_learning_app.curriculum.content

internal val kmpQuestions = listOf(
    question(
        id = "shared_vs_platform_code_001",
        topicId = "kmp",
        subtopicId = "shared_vs_platform_code",
        text = "What is a good rule of thumb when deciding what to put in Kotlin Multiplatform common code?",
        correctAnswerIds = listOf("shared_vs_platform_code_001_c"),
        explanation = "Common code should hold behavior that is genuinely platform-independent or expressed through a useful common contract. Platform-specific UI or APIs should stay in platform source sets unless there is a good abstraction.",
        sources = listOf(kmpSource),
        answers = listOf(
            answer("shared_vs_platform_code_001_a", "Put every line of code in commonMain, even platform API calls."),
            answer("shared_vs_platform_code_001_b", "Avoid common code entirely in KMP projects."),
            answer("shared_vs_platform_code_001_c", "Share platform-independent logic and use platform source sets for platform APIs."),
            answer("shared_vs_platform_code_001_d", "Use expect/actual for every class by default."),
        ),
    ),
    question(
        id = "expect_actual_001",
        topicId = "kmp",
        subtopicId = "expect_actual",
        text = "What problem do expect and actual declarations solve in Kotlin Multiplatform?",
        correctAnswerIds = listOf("expect_actual_001_a"),
        explanation = "An expect declaration defines a common API, and each platform supplies the actual implementation. This is useful when shared code needs a platform-specific capability behind a common contract.",
        sources = listOf(kmpExpectActualSource),
        answers = listOf(
            answer("expect_actual_001_a", "They let common code depend on a common declaration with platform-specific implementations."),
            answer("expect_actual_001_b", "They automatically convert Android Views into SwiftUI."),
            answer("expect_actual_001_c", "They replace Gradle source sets."),
            answer("expect_actual_001_d", "They serialize content models without annotations."),
        ),
    ),
)

