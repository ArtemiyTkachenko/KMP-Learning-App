package org.artkachenko.kmp_learning_app.curriculum.visibility

/**
 * The product area a Topic belongs to, which is how the catalogue is grouped for the learner.
 *
 * Classification, not visibility: every Topic has a section whether or not it is currently shown,
 * and [CurriculumVisibility.isTopicVisible] alone decides whether it is. A section with no visible
 * Topic is simply not presented.
 *
 * Declaration order is presentation order, so Android Engineering always comes first.
 */
internal enum class CurriculumSection {
    AndroidEngineering,
    KotlinMultiplatform,
}
