package org.artkachenko.kmp_learning_app.curriculum.learning.content

import kmp_learning_app.shared.generated.resources.Res
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningCurriculum
import org.artkachenko.kmp_learning_app.curriculum.learning.serialization.LearningCurriculumJsonCodec

/**
 * The shipped learning document, read through Compose Resources so every configured host
 * gets the same bytes without a platform file API.
 *
 * It is a separate resource from `initial_curriculum.json` on purpose: the assessment
 * curriculum and the learning curriculum have different hierarchies and different
 * lifecycles, and one is imported into Room while the other stays an in-memory document.
 *
 * This source decodes and nothing more. Whether the decoded document is coherent against
 * the assessment taxonomy is [LearningContentLoader]'s concern.
 */
internal object BundledLearningCurriculumSource {
    private const val LearningCurriculumResourcePath = "files/curriculum/learning_curriculum.json"

    suspend fun load(): LearningCurriculum =
        LearningCurriculumJsonCodec.decode(
            Res.readBytes(LearningCurriculumResourcePath).decodeToString(),
        )
}
