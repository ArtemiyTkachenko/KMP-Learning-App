package org.artkachenko.kmp_learning_app.curriculum.visibility

import kotlin.time.Instant
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit
import org.artkachenko.kmp_learning_app.topic_study.FakeLearningContentRepository
import org.artkachenko.kmp_learning_app.topic_study.testLearningLesson
import org.artkachenko.kmp_learning_app.topic_study.testLearningUnit

/**
 * Learning content and attempts for the destination-guard and route-pruning tests, over the
 * [VisibilityFixture] curriculum: one Unit on each side of the boundary, each teaching its own
 * Topic's Subtopic.
 */
internal object VisibilityGuardFixture {
    val androidUnit: LearningUnit = testLearningUnit(
        id = "unit_android",
        topicId = VisibilityFixture.android.id,
        lessons = listOf(
            testLearningLesson("lesson_android_1", primarySubtopicIds = listOf("android_lifecycle")),
            testLearningLesson("lesson_android_2", primarySubtopicIds = listOf("android_lifecycle")),
        ),
    )
    val kmpUnit: LearningUnit = testLearningUnit(
        id = "unit_kmp",
        topicId = VisibilityFixture.kmp.id,
        lessons = listOf(
            testLearningLesson("lesson_kmp_1", primarySubtopicIds = listOf("kmp_expect_actual")),
            testLearningLesson("lesson_kmp_2", primarySubtopicIds = listOf("kmp_expect_actual")),
        ),
    )

    fun learningContent(): FakeLearningContentRepository =
        FakeLearningContentRepository(units = listOf(androidUnit, kmpUnit))
}

/** An in-progress attempt over [questionIds], nothing answered yet. */
internal fun inProgressAttempt(
    id: String,
    vararg questionIds: String,
    config: AssessmentConfig = AssessmentConfig.Mixed(questionCount = questionIds.size),
): TestAttempt =
    TestAttempt(
        id = id,
        config = config,
        questionAttempts = questionIds.map { QuestionAttempt(questionId = it) },
        status = AssessmentStatus.IN_PROGRESS,
        startedAt = Instant.fromEpochSeconds(1_000),
    )

/**
 * Attempts held in memory. [saves] counts every write, so a test can assert that a visibility
 * transition — a guard, a projection, a pruning pass — wrote nothing.
 */
internal class FixtureAssessmentRepository(vararg attempts: TestAttempt) : AssessmentRepository {
    private val attempts = attempts.associateBy { it.id }.toMutableMap()

    var saves: Int = 0
        private set

    val getByIdCalls = mutableListOf<String>()

    fun snapshot(): Map<String, TestAttempt> = attempts.toMap()

    override suspend fun save(attempt: TestAttempt) {
        saves += 1
        attempts[attempt.id] = attempt
    }

    override suspend fun getById(attemptId: String): TestAttempt? {
        getByIdCalls += attemptId
        return attempts[attemptId]
    }

    override suspend fun getCompletedAttempts(): List<TestAttempt> =
        attempts.values.filter { it.status == AssessmentStatus.COMPLETED }
}
