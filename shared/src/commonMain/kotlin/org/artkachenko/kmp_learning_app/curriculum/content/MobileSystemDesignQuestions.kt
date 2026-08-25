package org.artkachenko.kmp_learning_app.curriculum.content

internal val mobileSystemDesignQuestions = listOf(
    question(
        id = "offline_design_001",
        topicId = "mobile_system_design",
        subtopicId = "offline_design",
        text = "A news app must remain readable during poor connectivity. Which design choice best supports that requirement?",
        correctAnswerIds = listOf("offline_design_001_b"),
        explanation = "An offline-first design reads from a local source of truth and synchronizes with the network when possible, keeping the UI useful during connectivity problems.",
        sources = listOf(offlineFirstSource, androidDataLayerSource),
        answers = listOf(
            answer("offline_design_001_a", "Block the UI until every network request succeeds."),
            answer("offline_design_001_b", "Read from a local source of truth and sync remote updates in the background."),
            answer("offline_design_001_c", "Store all state only in Activity fields."),
            answer("offline_design_001_d", "Disable caching to avoid stale data."),
        ),
    ),
    question(
        id = "background_sync_001",
        topicId = "mobile_system_design",
        subtopicId = "background_sync",
        text = "When designing background sync for mobile, which constraints should be considered? Select all that apply.",
        correctAnswerIds = listOf("background_sync_001_a", "background_sync_001_b", "background_sync_001_d"),
        explanation = "Mobile sync design must account for network availability, battery, OS background limits, and retry behavior. Ignoring those constraints produces unreliable or wasteful sync.",
        sources = listOf(offlineFirstSource, workManagerSource, backgroundWorkSource),
        answers = listOf(
            answer("background_sync_001_a", "Network availability and metering."),
            answer("background_sync_001_b", "Battery and charging state."),
            answer("background_sync_001_c", "Whether the feature uses a specific button color."),
            answer("background_sync_001_d", "OS background execution limits and retry policy."),
        ),
    ),
)

