package org.artkachenko.kmp_learning_app.curriculum.content

internal val notificationQuestions = listOf(
    question(
        id = "notification_channels_001",
        topicId = "notifications",
        subtopicId = "notification_channels",
        text = "Why do Android notification channels matter?",
        correctAnswerIds = listOf("notification_channels_001_c"),
        explanation = "Notification channels group notifications into user-controllable categories. On supported Android versions, channel importance and settings affect how notifications appear.",
        sources = listOf(notificationsSource),
        answers = listOf(
            answer("notification_channels_001_a", "They replace PendingIntent for notification taps."),
            answer("notification_channels_001_b", "They are required only for FCM data messages."),
            answer("notification_channels_001_c", "They give users category-level control over notification behavior."),
            answer("notification_channels_001_d", "They store notification history in Room."),
        ),
    ),
    question(
        id = "notification_permission_001",
        topicId = "notifications",
        subtopicId = "notification_permission",
        text = "For Android 13 and higher, what must most apps do before posting non-exempt notifications?",
        correctAnswerIds = listOf("notification_permission_001_b"),
        explanation = "Android 13 introduced the POST_NOTIFICATIONS runtime permission for most non-exempt notifications. Apps should request it at an appropriate time.",
        sources = listOf(notificationPermissionSource),
        answers = listOf(
            answer("notification_permission_001_a", "Declare only INTERNET permission."),
            answer("notification_permission_001_b", "Request the POST_NOTIFICATIONS runtime permission."),
            answer("notification_permission_001_c", "Start a bound service."),
            answer("notification_permission_001_d", "Create a Room migration."),
        ),
    ),
    question(
        id = "fcm_data_messages_001",
        topicId = "notifications",
        subtopicId = "fcm_data_messages",
        text = "What is a practical difference between FCM notification messages and data messages?",
        correctAnswerIds = listOf("fcm_data_messages_001_a"),
        explanation = "Notification messages can be handled/displayed by the SDK depending on app state, while data messages deliver custom key-value payloads for app code to handle.",
        sources = listOf(fcmSource),
        answers = listOf(
            answer("fcm_data_messages_001_a", "Data messages carry custom payloads for app handling; notification messages can be displayed by the SDK depending on app state."),
            answer("fcm_data_messages_001_b", "Data messages never reach Android devices."),
            answer("fcm_data_messages_001_c", "Notification messages require Room."),
            answer("fcm_data_messages_001_d", "Both message types are always handled identically in every app state."),
        ),
    ),
)

