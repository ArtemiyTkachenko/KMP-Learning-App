package org.artkachenko.kmp_learning_app.curriculum.serialization

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.artkachenko.kmp_learning_app.curriculum.Curriculum

internal object CurriculumJsonCodec {
    private val json = Json {
        encodeDefaults = true
    }

    fun decode(value: String): Curriculum =
        json.decodeFromString(value)

    fun encode(curriculum: Curriculum): String =
        json.encodeToString(curriculum)
}
