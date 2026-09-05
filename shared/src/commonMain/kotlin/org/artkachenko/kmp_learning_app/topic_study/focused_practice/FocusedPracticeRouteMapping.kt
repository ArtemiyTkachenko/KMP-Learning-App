package org.artkachenko.kmp_learning_app.topic_study.focused_practice

import org.artkachenko.kmp_learning_app.AppRoute
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope

internal fun AppRoute.FocusedTopicPractice.toAssessmentConfig(): AssessmentConfig.Focused =
    AssessmentConfig.Focused(
        scope = AssessmentScope.Topic(topicId),
        questionCount = questionCount,
        levels = levels.toSet(),
        source = source,
    )

internal fun AppRoute.FocusedSubtopicPractice.toAssessmentConfig(): AssessmentConfig.Focused =
    AssessmentConfig.Focused(
        scope = AssessmentScope.Subtopic(subtopicId),
        questionCount = questionCount,
        levels = levels.toSet(),
        source = source,
    )

/**
 * Rebuilt as a `Set`, which is the scope's own canonical form: the route sorted the IDs only so
 * that an identical configuration is an identical back-stack entry, and that ordering carries no
 * meaning for what is asked.
 */
internal fun AppRoute.FocusedSubtopicsPractice.toAssessmentConfig(): AssessmentConfig.Focused =
    AssessmentConfig.Focused(
        scope = AssessmentScope.Subtopics(subtopicIds.toSet()),
        questionCount = questionCount,
        levels = levels.toSet(),
        source = source,
    )
