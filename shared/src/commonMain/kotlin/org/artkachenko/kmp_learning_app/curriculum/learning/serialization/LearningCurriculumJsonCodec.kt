package org.artkachenko.kmp_learning_app.curriculum.learning.serialization

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningCurriculum

/**
 * Codec for the authored learning document.
 *
 * It is separate from `CurriculumJsonCodec` because it needs an explicit polymorphic
 * discriminator for [org.artkachenko.kmp_learning_app.curriculum.learning.LearningBlock],
 * which the assessment curriculum has no use for. The discriminator is named here rather
 * than left to the library default so the authored JSON keeps a stated contract.
 */
internal object LearningCurriculumJsonCodec {
    private val json = Json {
        encodeDefaults = true
        classDiscriminator = "type"
    }

    fun decode(value: String): LearningCurriculum =
        json.decodeFromString(value)

    fun encode(curriculum: LearningCurriculum): String =
        json.encodeToString(curriculum)
}
