package org.artkachenko.kmp_learning_app.curriculum.learning.content

import kotlinx.serialization.SerializationException
import org.artkachenko.kmp_learning_app.curriculum.Curriculum
import org.artkachenko.kmp_learning_app.curriculum.content.BundledCurriculumSource
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningCurriculum
import org.artkachenko.kmp_learning_app.curriculum.learning.validation.LearningCurriculumValidator

/**
 * The boundary that turns bundled bytes into learning content consumers may read.
 *
 * Learning content references Topics and Subtopics by id, so it is validated against the
 * same bundled base [Curriculum] it was authored against — the document behind
 * `initial_curriculum.json`, not the imported Room copy. Reading the bundle keeps
 * publisher content out of the persistence path entirely and checks the authored
 * references against the exact taxonomy they were written for.
 *
 * Validation is all-or-nothing: [load] either returns a fully validated document or throws
 * [LearningContentLoadException]. There is no partial success, because a half-usable
 * curriculum would turn an authoring defect into a UI defect discovered much later.
 *
 * Both sources are injectable so tests can make either failure deterministic without
 * touching the shipped resources.
 */
internal class LearningContentLoader(
    private val loadLearningCurriculum: suspend () -> LearningCurriculum = {
        BundledLearningCurriculumSource.load()
    },
    private val loadCurriculum: suspend () -> Curriculum = {
        BundledCurriculumSource.load()
    },
    private val validator: LearningCurriculumValidator = LearningCurriculumValidator(),
) {
    suspend fun load(): LearningCurriculum {
        // Only a serialization failure is translated. A missing or unreadable resource is a
        // packaging fault rather than a content fault, and reporting it as malformed content
        // would send whoever reads the failure to the wrong file.
        val learningCurriculum = try {
            loadLearningCurriculum()
        } catch (cause: SerializationException) {
            throw LearningContentLoadException(LearningContentLoadFailure.Decode(cause))
        }

        val errors = validator.validate(
            learningCurriculum = learningCurriculum,
            curriculum = loadCurriculum(),
        )
        if (errors.isNotEmpty()) {
            throw LearningContentLoadException(LearningContentLoadFailure.Validation(errors))
        }

        return learningCurriculum
    }
}
