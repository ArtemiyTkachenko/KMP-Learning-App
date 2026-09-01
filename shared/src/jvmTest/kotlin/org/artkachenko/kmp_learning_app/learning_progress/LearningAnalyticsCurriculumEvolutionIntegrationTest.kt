package org.artkachenko.kmp_learning_app.learning_progress

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Curriculum
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.data.local.assessment.AssessmentAttemptStore
import org.artkachenko.kmp_learning_app.data.local.assessment.repository.LocalAssessmentRepository
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImportResult
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import org.artkachenko.kmp_learning_app.data.local.curriculum.repository.LocalCurriculumRepository
import org.artkachenko.kmp_learning_app.ui.LearningContextIndex

/**
 * Derived analytics against real persistence and a curriculum that keeps changing underneath it.
 *
 * Everything below runs over a real Room database through the real importer, the real repositories,
 * and the real [LearningProgressService]; there is no fake repository in the evolution scenarios on
 * purpose. The unit-level contract is already covered in `LearningProgressServiceTest`, so what these
 * tests add is the crossing: importer writes, Room rows, repository mapping, persisted history, and
 * the derivation on top of all of it.
 *
 * The one distinction every scenario circles back to is that accuracy and coverage answer different
 * questions from the same history. Accuracy is historical and occurrence-based, so answering one
 * Question three times contributes three observations and never changes when the curriculum does.
 * Coverage is a statement about the CURRENT ACTIVE bank, so it counts each stable Question ID once
 * and legitimately moves when Questions are published or retired.
 *
 * Fixtures are deliberately tiny and purpose-built rather than the bundled question bank: these are
 * semantic assertions with exact expected counts, and they must stay stable as more interview
 * Questions are authored.
 */
internal class LearningAnalyticsCurriculumEvolutionIntegrationTest {
    @Test
    fun persistedHistoryKeepsUniqueCoverageAfterRepositoryReconstruction() = runTest {
        withRealDatabase { database ->
            database.importVersion(version1Curriculum())
            val original = analyticsOver(database)
            assertActiveQuestionIds(original, ActivityLifecycleQuestionId, FragmentLifecycleQuestionId)
            initialHistory().forEach { original.assessments.save(it) }

            val beforeReconstruction = original.progress.load()
            assertBaselineAnalytics(beforeReconstruction)

            // Nothing from the derivation above survives this: new repositories, a new service, and
            // the same real database underneath. Anything the analytics needed that was not
            // persisted would go missing here.
            val rebuilt = analyticsOver(database)
            val afterReconstruction = rebuilt.progress.load()

            assertBaselineAnalytics(afterReconstruction)
            assertEquals(beforeReconstruction, afterReconstruction)
            assertEquals(
                original.assessments.getCompletedAttempts(),
                rebuilt.assessments.getCompletedAttempts(),
            )
        }
    }

    @Test
    fun repeatedQuestionAddsAccuracyObservationsWithoutInflatingCoverage() = runTest {
        withRealDatabase { database ->
            database.importVersion(version1Curriculum())
            val analytics = analyticsOver(database)
            // The activity-lifecycle Question is answered twice and the fragment one once, so three
            // observations were recorded across two distinct Questions.
            initialHistory().forEach { analytics.assessments.save(it) }

            val snapshot = analytics.progress.load()

            assertEquals(3, snapshot.answeredQuestionCount)
            assertEquals(2, snapshot.correctAnswerCount)
            assertEquals(2, snapshot.coverage.attemptedQuestionCount)

            val topic = snapshot.topics.single()
            assertEquals(3, topic.answeredCount)
            assertEquals(2, snapshot.topicCoverage.single().attemptedQuestionCount)

            val subtopic = snapshot.subtopics.single()
            assertEquals(3, subtopic.answeredCount)
            assertEquals(2, snapshot.subtopicCoverage.single().attemptedQuestionCount)
        }
    }

    @Test
    fun newActiveQuestionsExpandCoverageWithoutChangingHistory() = runTest {
        withRealDatabase { database ->
            database.importVersion(version1Curriculum())
            val beforeGrowth = analyticsOver(database)
            initialHistory().forEach { beforeGrowth.assessments.save(it) }
            val snapshotBefore = beforeGrowth.progress.load()
            val historyBefore = beforeGrowth.assessments.getCompletedAttempts()
            assertCoverage(snapshotBefore.coverage, attempted = 2, total = 2, percentage = 100.0)

            database.importVersion(version2WithNewQuestion())
            // Started but never finished, on the Question nobody has answered yet. Coverage
            // describes completed evidence, so opening an assessment must not explore anything.
            beforeGrowth.assessments.save(inProgressAttemptOn(ProcessDeathQuestionId))

            val afterGrowth = analyticsOver(database)
            assertActiveQuestionIds(
                afterGrowth,
                ActivityLifecycleQuestionId,
                FragmentLifecycleQuestionId,
                ProcessDeathQuestionId,
            )
            val snapshotAfter = afterGrowth.progress.load()

            // A curriculum update publishes content; it must not rewrite what the learner did, and
            // the unfinished attempt is not part of that history either.
            assertEquals(historyBefore, afterGrowth.assessments.getCompletedAttempts())
            assertEquals(
                AssessmentStatus.IN_PROGRESS,
                assertNotNull(afterGrowth.assessments.getById(InProgressAttemptId)).status,
            )
            assertHistoricalAnalyticsUnchanged(snapshotBefore, snapshotAfter)

            // Only the current denominator moved: the third Question exists but nobody has met it.
            assertCoverage(snapshotAfter.coverage, attempted = 2, total = 3, percentage = 200.0 / 3.0)
            assertCoverage(
                snapshotAfter.topicCoverage.single(),
                attempted = 2,
                total = 3,
                percentage = 200.0 / 3.0,
            )
            assertCoverage(
                snapshotAfter.subtopicCoverage.single(),
                attempted = 2,
                total = 3,
                percentage = 200.0 / 3.0,
            )
        }
    }

    @Test
    fun deprecatingAQuestionChangesCoverageButNotHistoricalAccuracy() = runTest {
        withRealDatabase { database ->
            database.importVersion(version1Curriculum())
            val seeded = analyticsOver(database)
            initialHistory().forEach { seeded.assessments.save(it) }
            database.importVersion(version2WithNewQuestion())
            val beforeRetirement = analyticsOver(database)
            val snapshotBefore = beforeRetirement.progress.load()
            val historyBefore = beforeRetirement.assessments.getCompletedAttempts()
            assertCoverage(snapshotBefore.coverage, attempted = 2, total = 3, percentage = 200.0 / 3.0)

            // Retirement is an explicit DEPRECATED status on the same stable ID, not an omission:
            // the importer retains content that a later curriculum simply does not mention, so
            // leaving the Question out would model nothing at all.
            database.importVersion(version3WithDeprecatedQuestion())

            val afterRetirement = analyticsOver(database)
            assertActiveQuestionIds(
                afterRetirement,
                ActivityLifecycleQuestionId,
                ProcessDeathQuestionId,
            )
            // Lifecycle status and historical resolvability are different things: the retired
            // Question keeps its Topic and Subtopic metadata, which is what lets the observation it
            // carries stay attributed below.
            val retired = assertNotNull(
                afterRetirement.curriculum.getQuestionById(FragmentLifecycleQuestionId),
            )
            assertEquals(ContentStatus.DEPRECATED, retired.status)

            val snapshotAfter = afterRetirement.progress.load()
            assertEquals(historyBefore, afterRetirement.assessments.getCompletedAttempts())
            assertHistoricalAnalyticsUnchanged(snapshotBefore, snapshotAfter)
            // Still three historical observations, including the one answered on the retired
            // Question: evidence of what the learner knows does not expire with the content.
            assertEquals(3, snapshotAfter.topics.single().answeredCount)
            assertEquals(3, snapshotAfter.subtopics.single().answeredCount)

            assertCoverage(snapshotAfter.coverage, attempted = 1, total = 2, percentage = 50.0)
            assertCoverage(snapshotAfter.topicCoverage.single(), attempted = 1, total = 2, percentage = 50.0)
            assertCoverage(
                snapshotAfter.subtopicCoverage.single(),
                attempted = 1,
                total = 2,
                percentage = 50.0,
            )
        }
    }

    @Test
    fun missingHistoricalContentDoesNotCorruptDerivedAnalytics() = runTest {
        withRealDatabase { database ->
            database.importVersion(version1WithLaterWithdrawnQuestion())
            val persisted = analyticsOver(database)
            (initialHistory() + withdrawnQuestionAttempt()).forEach { persisted.assessments.save(it) }

            val analytics = analyticsOver(
                database,
                curriculum = CurriculumRepositoryWithoutQuestion(
                    delegate = LocalCurriculumRepository(database),
                    unavailableQuestionId = WithdrawnQuestionId,
                ),
            )
            assertActiveQuestionIds(analytics, ActivityLifecycleQuestionId, FragmentLifecycleQuestionId)
            assertNull(analytics.curriculum.getQuestionById(WithdrawnQuestionId))
            // The row itself is untouched. Room's foreign keys protect the history that references
            // it, and manufacturing an orphan would test the schema rather than the analytics.
            assertNotNull(LocalCurriculumRepository(database).getQuestionById(WithdrawnQuestionId))

            val snapshot = analytics.progress.load()

            // Persisted AssessmentScore is the historical authority, so the fourth answer counts
            // even though nothing can say any more which Topic it belonged to.
            assertEquals(4, snapshot.completedAttemptCount)
            assertEquals(4, snapshot.answeredQuestionCount)
            assertEquals(3, snapshot.correctAnswerCount)
            assertEquals(75.0, snapshot.percentage, absoluteTolerance = Tolerance)
            assertEquals(4, snapshot.recentPerformance.answeredQuestionCount)
            assertEquals(3, snapshot.recentPerformance.correctAnswerCount)

            // Scoped performance cannot invent an attribution it has no metadata for, and it never
            // borrows one from the assessment's configuration.
            assertPerformance(snapshot.topics.single(), answered = 3, correct = 2)
            assertPerformance(snapshot.subtopics.single(), answered = 3, correct = 2)

            // The unresolvable Question is not part of the current ACTIVE bank, so it neither
            // raises the numerator nor the denominator of coverage.
            assertCoverage(snapshot.coverage, attempted = 2, total = 2, percentage = 100.0)
        }
    }

    @Test
    fun recentWindowUsesRealCompletedHistoryOrdering() = runTest {
        withRealDatabase { database ->
            database.importVersion(version1Curriculum())
            val analytics = analyticsOver(database)
            // Saved in an order unrelated to their timestamps, so anything reading insertion order
            // rather than the persisted ordering contract picks a different window.
            orderingHistory().shuffledForPersistence().forEach { analytics.assessments.save(it) }

            assertEquals(
                listOf("attempt_6", "attempt_4", "attempt_5", "attempt_3", "attempt_2", "attempt_1"),
                analytics.assessments.getCompletedAttempts().map { it.id },
                "the repository returns completed history newest first, resolving equal completion" +
                    " times by start time and then by identity",
            )

            val recent = analytics.progress.load().recentPerformance

            // The same five newest attempts, exposed oldest -> newest so a trend reads
            // past -> present. The sixth is correctly outside the window.
            assertEquals(
                listOf("attempt_2", "attempt_3", "attempt_5", "attempt_4", "attempt_6"),
                recent.attemptSeries.map { it.attemptId },
            )
            assertEquals(6, recent.answeredQuestionCount)
            assertEquals(4, recent.correctAnswerCount)
            // Question-weighted across the window, not the mean of the five attempt percentages,
            // which would be 60%.
            assertEquals(200.0 / 3.0, assertNotNull(recent.percentage), absoluteTolerance = Tolerance)
            assertEquals(RecentTrendAvailability.Available, recent.trendAvailability)
        }
    }

    @Test
    fun correctingTheCurrentAnswerKeyLeavesHistoricalCorrectnessAlone() = runTest {
        withRealDatabase { database ->
            database.importVersion(version1Curriculum())
            val analytics = analyticsOver(database)
            initialHistory().forEach { analytics.assessments.save(it) }
            val before = analytics.progress.load()

            database.importVersion(version1WithCorrectedAnswerKey())

            val afterCorrection = analyticsOver(database)
            assertEquals(
                listOf(wrongAnswerId(ActivityLifecycleQuestionId)),
                assertNotNull(
                    afterCorrection.curriculum.getQuestionById(ActivityLifecycleQuestionId),
                ).correctAnswerIds,
            )

            // Correctness is read from the persisted answer state, never recomputed against the
            // current answer key, so fixing a Question does not retroactively change results the
            // learner already saw.
            assertHistoricalAnalyticsUnchanged(before, afterCorrection.progress.load())
        }
    }

    @Test
    fun learningContextKeepsHistoricalAccuracyBesideCurrentCoverage() = runTest {
        withRealDatabase { database ->
            database.importVersion(version1Curriculum())
            val seeded = analyticsOver(database)
            initialHistory().forEach { seeded.assessments.save(it) }
            database.importVersion(version2WithNewQuestion())
            database.importVersion(version3WithDeprecatedQuestion())
            database.importVersion(unseenTopicCurriculum())

            val index = LearningContextIndex(analyticsOver(database).progress.load())

            // The two halves are joined but never merged: real historical accuracy beside partial
            // coverage of a bank that has since changed shape.
            val topic = index.forTopic(AndroidTopicId)
            assertEquals(200.0 / 3.0, assertNotNull(topic.accuracyPercentage), absoluteTolerance = Tolerance)
            assertEquals(1, topic.attemptedQuestionCount)
            assertEquals(2, topic.totalQuestionCount)
            assertEquals(50.0, assertNotNull(topic.coveragePercentage), absoluteTolerance = Tolerance)
            assertTrue(topic.isWeak)
            assertFalse(topic.isUnstudied)

            val subtopic = index.forSubtopic(LifecycleSubtopicId)
            assertEquals(200.0 / 3.0, assertNotNull(subtopic.accuracyPercentage), absoluteTolerance = Tolerance)
            assertEquals(1, subtopic.attemptedQuestionCount)
            assertEquals(2, subtopic.totalQuestionCount)

            // A Topic published after the history was recorded reports no accuracy at all rather
            // than a fabricated 0%.
            val unseen = index.forTopic(KotlinTopicId)
            assertNull(unseen.accuracyPercentage)
            assertEquals(0, unseen.attemptedQuestionCount)
            assertEquals(1, unseen.totalQuestionCount)
            assertTrue(unseen.isUnstudied)
            assertFalse(unseen.isWeak)
        }
    }

    /**
     * The figures the whole suite is anchored on: three observations across two distinct Questions,
     * one of them answered twice.
     */
    private fun assertBaselineAnalytics(snapshot: LearningProgressSnapshot) {
        assertEquals(3, snapshot.completedAttemptCount)
        assertEquals(3, snapshot.answeredQuestionCount)
        assertEquals(2, snapshot.correctAnswerCount)
        assertEquals(200.0 / 3.0, snapshot.percentage, absoluteTolerance = Tolerance)

        assertCoverage(snapshot.coverage, attempted = 2, total = 2, percentage = 100.0)
        assertCoverage(snapshot.topicCoverage.single(), attempted = 2, total = 2, percentage = 100.0)
        assertCoverage(snapshot.subtopicCoverage.single(), attempted = 2, total = 2, percentage = 100.0)

        assertPerformance(snapshot.topics.single(), answered = 3, correct = 2)
        assertPerformance(snapshot.subtopics.single(), answered = 3, correct = 2)

        // Weakness is a verdict about historical evidence, and coverage plays no part in it: this
        // scope is fully covered and still weak.
        assertTrue(snapshot.topics.single().isWeak)
        assertTrue(snapshot.subtopics.single().isWeak)
        assertEquals(
            listOf(LifecycleSubtopicId, AndroidTopicId),
            snapshot.weakAreas.map {
                when (it) {
                    is WeakArea.Topic -> it.performance.topicId
                    is WeakArea.Subtopic -> it.performance.subtopicId
                }
            },
        )

        assertEquals(
            listOf(FirstAttemptId, SecondAttemptId, ThirdAttemptId),
            snapshot.recentPerformance.attemptSeries.map { it.attemptId },
        )
        assertEquals(
            200.0 / 3.0,
            assertNotNull(snapshot.recentPerformance.percentage),
            absoluteTolerance = Tolerance,
        )
        assertEquals(RecentTrendAvailability.Available, snapshot.recentPerformance.trendAvailability)
    }

    /**
     * Everything a curriculum change must leave alone. Coverage is deliberately absent: it is the
     * one figure that is allowed to move.
     */
    private fun assertHistoricalAnalyticsUnchanged(
        before: LearningProgressSnapshot,
        after: LearningProgressSnapshot,
    ) {
        assertEquals(before.completedAttemptCount, after.completedAttemptCount)
        assertEquals(before.answeredQuestionCount, after.answeredQuestionCount)
        assertEquals(before.correctAnswerCount, after.correctAnswerCount)
        assertEquals(before.percentage, after.percentage, absoluteTolerance = Tolerance)
        assertEquals(before.topics, after.topics)
        assertEquals(before.subtopics, after.subtopics)
        assertEquals(before.weakAreas, after.weakAreas)
        assertEquals(before.recentPerformance, after.recentPerformance)
    }

    private fun assertCoverage(
        coverage: QuestionCoverage,
        attempted: Int,
        total: Int,
        percentage: Double,
    ) {
        assertEquals(attempted, coverage.attemptedQuestionCount)
        assertEquals(total, coverage.totalQuestionCount)
        assertEquals(percentage, assertNotNull(coverage.percentage), absoluteTolerance = Tolerance)
    }

    private fun assertPerformance(
        performance: TopicPerformance,
        answered: Int,
        correct: Int,
    ) {
        assertEquals(answered, performance.answeredCount)
        assertEquals(correct, performance.correctCount)
    }

    private fun assertPerformance(
        performance: SubtopicPerformance,
        answered: Int,
        correct: Int,
    ) {
        assertEquals(answered, performance.answeredCount)
        assertEquals(correct, performance.correctCount)
    }

    private suspend fun assertActiveQuestionIds(
        analytics: AnalyticsUnderTest,
        vararg expected: String,
    ) {
        assertEquals(
            expected.toList().sorted(),
            analytics.curriculum.getActiveQuestions().map(Question::id).sorted(),
        )
    }
}

/**
 * Repositories and a service built freshly over one real database.
 *
 * Every scenario derives through a new instance, so a snapshot can only ever come from persisted
 * rows: no coverage, accuracy, or recent summary is carried between derivations in memory, and none
 * is stored anywhere either.
 */
private class AnalyticsUnderTest(
    database: CurriculumDatabase,
    val curriculum: CurriculumRepository,
) {
    val assessments: AssessmentRepository =
        LocalAssessmentRepository(AssessmentAttemptStore(database))
    val progress = LearningProgressService(assessments, curriculum)
}

private fun analyticsOver(
    database: CurriculumDatabase,
    curriculum: CurriculumRepository = LocalCurriculumRepository(database),
): AnalyticsUnderTest = AnalyticsUnderTest(database, curriculum)

/**
 * Content that history still references but that the product can no longer resolve.
 *
 * The schema deliberately protects assessment rows with foreign keys onto the curriculum, so the
 * historical Question is imported and persisted normally and the loss is modelled one layer up, at
 * the repository contract every consumer already has to tolerate: Room keeps the stored FK-valid
 * row, while the adapter presents a Question that is neither resolvable nor part of the current
 * ACTIVE bank.
 */
private class CurriculumRepositoryWithoutQuestion(
    private val delegate: CurriculumRepository,
    private val unavailableQuestionId: String,
) : CurriculumRepository by delegate {
    override suspend fun getQuestionById(questionId: String): Question? =
        if (questionId == unavailableQuestionId) null else delegate.getQuestionById(questionId)

    override suspend fun getActiveQuestions(): List<Question> =
        delegate.getActiveQuestions().filterNot { it.id == unavailableQuestionId }
}

/**
 * Repository reconstruction is what the acceptance criterion asks for, and an in-memory database
 * shared by successive repositories proves it: the rows outlive every object that read them. A
 * file-backed reopen harness would prove the same thing at the cost of test infrastructure this
 * project does not otherwise have.
 */
private suspend fun withRealDatabase(block: suspend (CurriculumDatabase) -> Unit) {
    val database = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
    try {
        block(database)
    } finally {
        database.close()
    }
}

/** A rejected fixture would look exactly like an analytics bug, so every import is checked. */
private suspend fun CurriculumDatabase.importVersion(curriculum: Curriculum) {
    assertIs<CurriculumImportResult.Imported>(
        CurriculumImporter(this, loadCurriculum = { curriculum }).importCurriculum(),
    )
}

private fun version1Curriculum(): Curriculum = androidCurriculum(
    evolutionQuestion(ActivityLifecycleQuestionId, "What ends an Activity's visible lifetime?"),
    evolutionQuestion(FragmentLifecycleQuestionId, "When is a Fragment's view destroyed?"),
)

private fun version2WithNewQuestion(): Curriculum = androidCurriculum(
    evolutionQuestion(ActivityLifecycleQuestionId, "What ends an Activity's visible lifetime?"),
    evolutionQuestion(FragmentLifecycleQuestionId, "When is a Fragment's view destroyed?"),
    evolutionQuestion(ProcessDeathQuestionId, "What survives process death?"),
)

private fun version3WithDeprecatedQuestion(): Curriculum = androidCurriculum(
    evolutionQuestion(ActivityLifecycleQuestionId, "What ends an Activity's visible lifetime?"),
    evolutionQuestion(
        FragmentLifecycleQuestionId,
        "When is a Fragment's view destroyed?",
        status = ContentStatus.DEPRECATED,
    ),
    evolutionQuestion(ProcessDeathQuestionId, "What survives process death?"),
)

/** Version 1 with the answer key of the repeated Question corrected after history exists. */
private fun version1WithCorrectedAnswerKey(): Curriculum = androidCurriculum(
    evolutionQuestion(
        ActivityLifecycleQuestionId,
        "What ends an Activity's visible lifetime?",
        correctAnswer = wrongAnswerId(ActivityLifecycleQuestionId),
    ),
    evolutionQuestion(FragmentLifecycleQuestionId, "When is a Fragment's view destroyed?"),
)

/** Version 1 plus the Question whose content later stops resolving. */
private fun version1WithLaterWithdrawnQuestion(): Curriculum = androidCurriculum(
    evolutionQuestion(ActivityLifecycleQuestionId, "What ends an Activity's visible lifetime?"),
    evolutionQuestion(FragmentLifecycleQuestionId, "When is a Fragment's view destroyed?"),
    evolutionQuestion(WithdrawnQuestionId, "A Question that is withdrawn after being answered."),
)

/**
 * A separate Topic published later. The importer retains unrelated existing content, so this adds a
 * scope the learner has never met without disturbing the Android history above.
 */
private fun unseenTopicCurriculum(): Curriculum = Curriculum(
    topics = listOf(Topic(KotlinTopicId, "Kotlin")),
    subtopics = listOf(Subtopic(CoroutinesSubtopicId, KotlinTopicId, "Coroutines")),
    questions = listOf(
        evolutionQuestion(
            id = StructuredConcurrencyQuestionId,
            text = "What does structured concurrency guarantee?",
            topicId = KotlinTopicId,
            subtopicId = CoroutinesSubtopicId,
        ),
    ),
)

private fun androidCurriculum(vararg questions: Question): Curriculum = Curriculum(
    topics = listOf(Topic(AndroidTopicId, "Android")),
    subtopics = listOf(Subtopic(LifecycleSubtopicId, AndroidTopicId, "Lifecycle")),
    questions = questions.toList(),
)

private fun evolutionQuestion(
    id: String,
    text: String,
    topicId: String = AndroidTopicId,
    subtopicId: String = LifecycleSubtopicId,
    correctAnswer: String = correctAnswerId(id),
    status: ContentStatus = ContentStatus.ACTIVE,
): Question = Question(
    id = id,
    topicId = topicId,
    subtopicId = subtopicId,
    text = text,
    answers = listOf(
        AnswerOption(correctAnswerId(id), "Authored answer for $id"),
        AnswerOption(wrongAnswerId(id), "Authored distractor for $id"),
    ),
    selectionMode = AnswerSelectionMode.SINGLE,
    correctAnswerIds = listOf(correctAnswer),
    explanation = "Why $id answers the way it does.",
    sources = listOf(SourceReference("Documentation for $id", "https://example.com/$id")),
    status = status,
)

/**
 * Three completed assessments in which one Question is answered twice, once right and once wrong.
 *
 * That single repetition is what separates the two concepts the rest of the suite checks: three
 * accuracy observations, two correct, but only two Questions ever encountered.
 */
private fun initialHistory(): List<TestAttempt> = listOf(
    completedAttempt(
        id = FirstAttemptId,
        completedAtMillis = 1_000,
        answers = listOf(answered(ActivityLifecycleQuestionId, correct = true)),
    ),
    completedAttempt(
        id = SecondAttemptId,
        completedAtMillis = 2_000,
        answers = listOf(answered(ActivityLifecycleQuestionId, correct = false)),
    ),
    completedAttempt(
        id = ThirdAttemptId,
        completedAtMillis = 3_000,
        answers = listOf(answered(FragmentLifecycleQuestionId, correct = true)),
    ),
)

private fun inProgressAttemptOn(questionId: String): TestAttempt = TestAttempt(
    id = InProgressAttemptId,
    config = AssessmentConfig.Mixed(1),
    questionAttempts = listOf(QuestionAttempt(questionId)),
    status = AssessmentStatus.IN_PROGRESS,
    startedAt = Instant.fromEpochMilliseconds(5_000),
)

private fun withdrawnQuestionAttempt(): TestAttempt = completedAttempt(
    id = WithdrawnAttemptId,
    completedAtMillis = 4_000,
    answers = listOf(answered(WithdrawnQuestionId, correct = true)),
)

/**
 * Six completed assessments whose ordering cannot be read off any single field: two share a
 * completion time and are separated by their start time, and two share both and are separated only
 * by identity.
 */
private fun orderingHistory(): List<TestAttempt> = listOf(
    completedAttempt("attempt_1", completedAtMillis = 1_000, startedAtMillis = 900, answers = listOf(answered(ActivityLifecycleQuestionId, correct = true))),
    completedAttempt("attempt_2", completedAtMillis = 2_000, startedAtMillis = 1_800, answers = listOf(answered(ActivityLifecycleQuestionId, correct = false))),
    completedAttempt("attempt_3", completedAtMillis = 2_000, startedAtMillis = 1_900, answers = listOf(answered(FragmentLifecycleQuestionId, correct = true))),
    completedAttempt(
        "attempt_4",
        completedAtMillis = 3_000,
        startedAtMillis = 2_900,
        answers = listOf(
            answered(ActivityLifecycleQuestionId, correct = true),
            answered(FragmentLifecycleQuestionId, correct = true),
        ),
    ),
    completedAttempt("attempt_5", completedAtMillis = 3_000, startedAtMillis = 2_900, answers = listOf(answered(FragmentLifecycleQuestionId, correct = true))),
    completedAttempt("attempt_6", completedAtMillis = 4_000, startedAtMillis = 3_900, answers = listOf(answered(ActivityLifecycleQuestionId, correct = false))),
)

/** A fixed persistence order that matches neither the chronology nor the identities. */
private fun List<TestAttempt>.shuffledForPersistence(): List<TestAttempt> =
    listOf(this[2], this[0], this[5], this[1], this[4], this[3])

private fun completedAttempt(
    id: String,
    completedAtMillis: Long,
    answers: List<QuestionAttempt>,
    startedAtMillis: Long = completedAtMillis - 100,
): TestAttempt = TestAttempt(
    id = id,
    config = AssessmentConfig.Mixed(answers.size),
    questionAttempts = answers,
    status = AssessmentStatus.COMPLETED,
    startedAt = Instant.fromEpochMilliseconds(startedAtMillis),
    completedAt = Instant.fromEpochMilliseconds(completedAtMillis),
    score = AssessmentScore(
        totalQuestions = answers.size,
        correctAnswers = answers.count {
            (it.answerState as QuestionAnswerState.Answered).isCorrect
        },
    ),
)

private fun answered(
    questionId: String,
    correct: Boolean,
): QuestionAttempt = QuestionAttempt(
    questionId,
    QuestionAnswerState.Answered(
        selectedAnswerIds = setOf(
            if (correct) correctAnswerId(questionId) else wrongAnswerId(questionId),
        ),
        isCorrect = correct,
    ),
)

private fun correctAnswerId(questionId: String): String = "${questionId}_a"

private fun wrongAnswerId(questionId: String): String = "${questionId}_b"

private const val AndroidTopicId = "topic_android"
private const val LifecycleSubtopicId = "subtopic_lifecycle"
private const val KotlinTopicId = "topic_kotlin"
private const val CoroutinesSubtopicId = "subtopic_coroutines"
private const val ActivityLifecycleQuestionId = "question_activity_lifecycle"
private const val FragmentLifecycleQuestionId = "question_fragment_lifecycle"
private const val ProcessDeathQuestionId = "question_process_death"
private const val WithdrawnQuestionId = "question_withdrawn"
private const val StructuredConcurrencyQuestionId = "question_structured_concurrency"
private const val FirstAttemptId = "attempt_first"
private const val SecondAttemptId = "attempt_second"
private const val ThirdAttemptId = "attempt_third"
private const val WithdrawnAttemptId = "attempt_withdrawn"
private const val InProgressAttemptId = "attempt_in_progress"
private const val Tolerance = 0.000_001
