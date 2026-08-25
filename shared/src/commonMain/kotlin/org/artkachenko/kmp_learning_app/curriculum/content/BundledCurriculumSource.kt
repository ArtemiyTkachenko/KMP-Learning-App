package org.artkachenko.kmp_learning_app.curriculum.content

import kmp_learning_app.shared.generated.resources.Res
import org.artkachenko.kmp_learning_app.curriculum.Curriculum
import org.artkachenko.kmp_learning_app.curriculum.serialization.CurriculumJsonCodec

internal object BundledCurriculumSource {
    private const val InitialCurriculumResourcePath = "files/curriculum/initial_curriculum.json"

    suspend fun load(): Curriculum =
        CurriculumJsonCodec.decode(
            Res.readBytes(InitialCurriculumResourcePath).decodeToString(),
        )
}
