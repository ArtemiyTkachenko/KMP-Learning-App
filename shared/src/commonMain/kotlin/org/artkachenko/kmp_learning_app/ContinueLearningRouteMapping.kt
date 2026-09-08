package org.artkachenko.kmp_learning_app

import org.artkachenko.kmp_learning_app.lesson_study.ContinueLearningTarget

/**
 * Continue Learning reaches the existing Lesson destination, and only that one.
 *
 * Navigation 3 stops at this file, exactly as it does for Continue Studying and Recommended Next:
 * [ContinueLearningTarget] is produced by a pure policy that knows nothing about `AppRoute`, which is
 * what lets the policy be tested without a back stack. The route it maps to is the one the Learning
 * Unit overview already pushes, carrying the same two stable IDs, so this feature adds no parallel
 * Lesson destination and no second way to open a Lesson.
 *
 * There is no mapping to any attempt, result, or practice route, and none may be added: a target
 * cannot carry an attempt ID, so "study this next" can never become "resume an assessment".
 */
internal fun ContinueLearningTarget.toAppRoute(): AppRoute =
    AppRoute.LearningLesson(unitId = unitId, lessonId = lessonId)
