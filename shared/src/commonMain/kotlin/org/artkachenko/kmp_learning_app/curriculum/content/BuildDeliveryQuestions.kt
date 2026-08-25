package org.artkachenko.kmp_learning_app.curriculum.content

internal val buildDeliveryQuestions = listOf(
    question(
        id = "dependency_configurations_001",
        topicId = "build_delivery",
        subtopicId = "dependency_configurations",
        text = "Why does Gradle distinguish dependency configurations such as implementation, api, and runtimeOnly?",
        correctAnswerIds = listOf("dependency_configurations_001_b"),
        explanation = "Configurations define the role and scope of dependencies, such as compile classpath exposure, runtime-only use, or publication to consumers.",
        sources = listOf(gradleConfigurationsSource),
        answers = listOf(
            answer("dependency_configurations_001_a", "To choose the Android launcher icon."),
            answer("dependency_configurations_001_b", "To model how dependencies are declared, resolved, and exposed."),
            answer("dependency_configurations_001_c", "To replace source sets with XML resources."),
            answer("dependency_configurations_001_d", "To make every dependency global to all modules."),
        ),
    ),
    question(
        id = "version_catalogs_001",
        topicId = "build_delivery",
        subtopicId = "version_catalogs",
        text = "What problem do Gradle version catalogs solve in a multi-module project?",
        correctAnswerIds = listOf("version_catalogs_001_c"),
        explanation = "Version catalogs centralize dependency and plugin coordinates so modules can use consistent aliases rather than duplicating literal versions.",
        sources = listOf(gradleVersionCatalogsSource),
        answers = listOf(
            answer("version_catalogs_001_a", "They compile Kotlin source files."),
            answer("version_catalogs_001_b", "They replace dependency resolution with manual downloads."),
            answer("version_catalogs_001_c", "They centralize dependency coordinates and versions behind reusable aliases."),
            answer("version_catalogs_001_d", "They store runtime user preferences."),
        ),
    ),
    question(
        id = "apk_vs_aab_001",
        topicId = "build_delivery",
        subtopicId = "apk_vs_aab",
        text = "What is a key difference between an APK and an Android App Bundle?",
        correctAnswerIds = listOf("apk_vs_aab_001_a"),
        explanation = "An APK is directly installable on a device. An Android App Bundle is a publishing format that lets Google Play generate optimized APKs for devices.",
        sources = listOf(androidAppBundleSource),
        answers = listOf(
            answer("apk_vs_aab_001_a", "An APK is installable; an App Bundle is a publishing format used to generate optimized APKs."),
            answer("apk_vs_aab_001_b", "An App Bundle is the only format that can run on an emulator directly."),
            answer("apk_vs_aab_001_c", "An APK contains only source code and no resources."),
            answer("apk_vs_aab_001_d", "They are identical files with different extensions."),
        ),
    ),
)

