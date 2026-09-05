package org.artkachenko.kmp_learning_app.curriculum.learning.content

import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.curriculum.content.BundledCurriculumSource
import org.artkachenko.kmp_learning_app.curriculum.learning.validation.LearningCurriculumValidator
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Exercises the shipped resource rather than a fixture, so the packaged bundle itself is
 * proven to decode and to be coherent against the taxonomy it was authored against.
 *
 * The bundle is intentionally empty until E20-05 authors the first Compose Unit. That is a
 * valid document, not a defect, so these tests assert the empty state instead of expecting
 * placeholder content — and deliberately do not require the document to be non-empty.
 */
internal class BundledLearningCurriculumTest {
    @Test
    fun bundledLearningCurriculumDecodesAndCurrentlyShipsNoUnits() = runTest {
        val learningCurriculum = BundledLearningCurriculumSource.load()

        assertEquals(emptyList(), learningCurriculum.units)
    }

    @Test
    fun bundledLearningCurriculumValidatesAgainstTheBundledBaseCurriculum() = runTest {
        val errors = LearningCurriculumValidator().validate(
            learningCurriculum = BundledLearningCurriculumSource.load(),
            curriculum = BundledCurriculumSource.load(),
        )

        assertEquals(emptyList(), errors)
    }

    @Test
    fun theProductionLoadPathExposesTheBundledDocument() = runTest {
        // The default loader wires both shipped resources together; this is the path the
        // repository uses at runtime, so a resource-path or packaging mistake fails here.
        val learningCurriculum = LearningContentLoader().load()

        assertEquals(emptyList(), learningCurriculum.units)
    }

    @Test
    fun theRepositoryServesTheBundledDocument() = runTest {
        val repository = BundledLearningContentRepository()

        assertEquals(emptyList(), repository.getActiveUnitsByTopic("android_ui"))
    }
}
