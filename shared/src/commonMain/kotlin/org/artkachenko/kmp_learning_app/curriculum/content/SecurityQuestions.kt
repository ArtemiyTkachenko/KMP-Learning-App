package org.artkachenko.kmp_learning_app.curriculum.content

internal val securityQuestions = listOf(
    question(
        id = "runtime_permissions_001",
        topicId = "security",
        subtopicId = "runtime_permissions",
        text = "Why should an app request a runtime permission close to the user action that needs it?",
        correctAnswerIds = listOf("runtime_permissions_001_c"),
        explanation = "Requesting permission in context helps users understand why access is needed and supports a better permission UX than requesting everything at startup.",
        sources = listOf(permissionsSource),
        answers = listOf(
            answer("runtime_permissions_001_a", "Because permissions requested at startup are always ignored by Android."),
            answer("runtime_permissions_001_b", "Because runtime permissions are granted automatically when requested later."),
            answer("runtime_permissions_001_c", "Because contextual requests explain the purpose and improve user trust."),
            answer("runtime_permissions_001_d", "Because permissions are stored only in a ViewModel."),
        ),
    ),
    question(
        id = "exported_components_001",
        topicId = "security",
        subtopicId = "exported_components",
        text = "Why should exported Android components be reviewed carefully?",
        correctAnswerIds = listOf("exported_components_001_a"),
        explanation = "Exported components can be invoked by other apps. They need intentional permissions, input validation, and minimal exposure.",
        sources = listOf(androidSecuritySource),
        answers = listOf(
            answer("exported_components_001_a", "Because other apps may be able to invoke them."),
            answer("exported_components_001_b", "Because exported components cannot receive Intents."),
            answer("exported_components_001_c", "Because exported components are always private to the app."),
            answer("exported_components_001_d", "Because exporting a component disables all permissions."),
        ),
    ),
    question(
        id = "sensitive_logging_001",
        topicId = "security",
        subtopicId = "sensitive_logging",
        text = "Why should access tokens, personal data, and secrets be excluded from logs?",
        correctAnswerIds = listOf("sensitive_logging_001_d"),
        explanation = "Logs can be retained, shared, or accessed during debugging and support. Sensitive data in logs increases privacy and account-compromise risk.",
        sources = listOf(androidSecuritySource),
        answers = listOf(
            answer("sensitive_logging_001_a", "Because logs cannot contain strings."),
            answer("sensitive_logging_001_b", "Because Android automatically encrypts every log line."),
            answer("sensitive_logging_001_c", "Because logging secrets improves crash triage safely."),
            answer("sensitive_logging_001_d", "Because logs can expose sensitive data beyond the intended security boundary."),
        ),
    ),
)

