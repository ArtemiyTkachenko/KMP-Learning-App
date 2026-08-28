package org.artkachenko.kmp_learning_app.data.local.assessment

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.data.local.assessment.entity.QuestionAttemptEntity
import org.artkachenko.kmp_learning_app.data.local.assessment.entity.QuestionAttemptSelectedAnswerEntity
import org.artkachenko.kmp_learning_app.data.local.assessment.entity.TestAttemptEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.AnswerOptionEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.QuestionEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.SubtopicEntity
import org.artkachenko.kmp_learning_app.data.local.curriculum.entity.TopicEntity

internal class AssessmentAttemptStoreTest {
    @Test
    fun inProgressFocusedTopicAttemptRoundTrips() = runTest {
        withTestDatabase { database ->
            insertAttemptFixtureCurriculum(database)
            val store = AssessmentAttemptStore(database)
            val attempt = TestAttempt(
                id = "attempt_topic",
                config = AssessmentConfig.Focused(
                    scope = AssessmentScope.Topic("topic"),
                    questionCount = 10,
                ),
                questionAttempts = listOf(
                    answeredQuestionAttempt("question_b", "question_b_a"),
                    QuestionAttempt("question_a"),
                ),
                status = AssessmentStatus.IN_PROGRESS,
                startedAt = StartedAt,
            )

            store.save(attempt)

            assertEquals(attempt, store.getById("attempt_topic"))
            assertEquals(1, database.assessmentAttemptDao().countTestAttempts())
            assertEquals(2, database.assessmentAttemptDao().countQuestionAttempts())
            assertEquals(1, database.assessmentAttemptDao().countSelectedAnswers())
        }
    }

    @Test
    fun focusedSubtopicAndMixedConfigsRoundTrip() = runTest {
        withTestDatabase { database ->
            insertAttemptFixtureCurriculum(database)
            val store = AssessmentAttemptStore(database)
            val focusedSubtopic = TestAttempt(
                id = "attempt_subtopic",
                config = AssessmentConfig.Focused(
                    scope = AssessmentScope.Subtopic("subtopic"),
                    questionCount = 2,
                ),
                questionAttempts = listOf(answeredQuestionAttempt("question_a", "question_a_a")),
                status = AssessmentStatus.IN_PROGRESS,
                startedAt = StartedAt,
            )
            val mixed = TestAttempt(
                id = "attempt_mixed",
                config = AssessmentConfig.Mixed(questionCount = 3),
                questionAttempts = listOf(answeredQuestionAttempt("question_b", "question_b_b", isCorrect = false)),
                status = AssessmentStatus.IN_PROGRESS,
                startedAt = StartedAt,
            )

            store.save(focusedSubtopic)
            store.save(mixed)

            assertEquals(focusedSubtopic, store.getById("attempt_subtopic"))
            assertEquals(mixed, store.getById("attempt_mixed"))
        }
    }

    @Test
    fun completedAttemptWithMultipleSelectedAnswersRoundTripsInAssessmentOrder() = runTest {
        withTestDatabase { database ->
            insertAttemptFixtureCurriculum(database)
            val store = AssessmentAttemptStore(database)
            val attempt = TestAttempt(
                id = "attempt_completed",
                config = AssessmentConfig.Mixed(questionCount = 10),
                questionAttempts = listOf(
                    answeredQuestionAttempt("question_b", "question_b_a", "question_b_c", isCorrect = true),
                    answeredQuestionAttempt("question_a", "question_a_b", isCorrect = false),
                ),
                status = AssessmentStatus.COMPLETED,
                startedAt = StartedAt,
                completedAt = CompletedAt,
                score = AssessmentScore(totalQuestions = 2, correctAnswers = 1),
            )

            store.save(attempt)

            val restored = store.getById("attempt_completed")
            assertEquals(attempt, restored)
            assertEquals(
                listOf("question_b", "question_a"),
                restored?.questionAttempts?.map { it.questionId },
            )
            assertEquals(50.0, restored?.score?.percentage)
        }
    }

    @Test
    fun completedHistoryIsEmptyAndExcludesPersistedInProgressAttempts() = runTest {
        withTestDatabase { database ->
            insertAttemptFixtureCurriculum(database)
            val store = AssessmentAttemptStore(database)
            assertEquals(emptyList(), store.getCompletedAttempts())

            val inProgress = TestAttempt(
                id = "attempt_in_progress",
                config = AssessmentConfig.Mixed(questionCount = 1),
                questionAttempts = listOf(QuestionAttempt("question_a")),
                status = AssessmentStatus.IN_PROGRESS,
                startedAt = StartedAt,
            )
            store.save(inProgress)

            assertEquals(emptyList(), store.getCompletedAttempts())
            assertEquals(inProgress, store.getById(inProgress.id))
        }
    }

    @Test
    fun completedHistoryReconstructsConfigsChildrenScoresAndNewestFirstOrder() = runTest {
        withTestDatabase { database ->
            insertAttemptFixtureCurriculum(database)
            val store = AssessmentAttemptStore(database)
            val oldFocusedTopic = completedAttempt(
                id = "old_focused_topic",
                config = AssessmentConfig.Focused(
                    scope = AssessmentScope.Topic("topic"),
                    questionCount = 3,
                ),
                startedAt = StartedAt,
                completedAt = instantAfter(1_000),
                questionAttempts = listOf(
                    answeredQuestionAttempt("question_c", "question_c_a"),
                    answeredQuestionAttempt("question_a", "question_a_b", isCorrect = false),
                    answeredQuestionAttempt("question_b", "question_b_a", "question_b_c"),
                ),
            )
            val newestFocusedSubtopic = completedAttempt(
                id = "new_focused_subtopic",
                config = AssessmentConfig.Focused(
                    scope = AssessmentScope.Subtopic("subtopic"),
                    questionCount = 1,
                ),
                startedAt = instantAfter(2_000),
                completedAt = instantAfter(3_000),
            )
            val middleMixed = completedAttempt(
                id = "middle_mixed",
                config = AssessmentConfig.Mixed(questionCount = 1),
                startedAt = instantAfter(1_000),
                completedAt = instantAfter(2_000),
            )
            val inProgress = TestAttempt(
                id = "in_progress_mixed",
                config = AssessmentConfig.Mixed(questionCount = 1),
                questionAttempts = listOf(QuestionAttempt("question_b")),
                status = AssessmentStatus.IN_PROGRESS,
                startedAt = instantAfter(4_000),
            )

            listOf(oldFocusedTopic, inProgress, newestFocusedSubtopic, middleMixed)
                .forEach { store.save(it) }

            assertEquals(
                listOf(newestFocusedSubtopic, middleMixed, oldFocusedTopic),
                store.getCompletedAttempts(),
            )
            assertEquals(inProgress, store.getById(inProgress.id))
        }
    }

    @Test
    fun completedHistoryUsesStartedTimeThenStableIdToBreakCompletionTies() = runTest {
        withTestDatabase { database ->
            insertAttemptFixtureCurriculum(database)
            val store = AssessmentAttemptStore(database)
            val completedAt = instantAfter(2_000)
            val earlierStarted = completedAttempt(
                id = "attempt_z",
                startedAt = StartedAt,
                completedAt = completedAt,
            )
            val laterStartedB = completedAttempt(
                id = "attempt_b",
                startedAt = instantAfter(1_000),
                completedAt = completedAt,
            )
            val laterStartedA = completedAttempt(
                id = "attempt_a",
                startedAt = instantAfter(1_000),
                completedAt = completedAt,
            )

            listOf(earlierStarted, laterStartedB, laterStartedA).forEach { store.save(it) }

            assertEquals(
                listOf("attempt_a", "attempt_b", "attempt_z"),
                store.getCompletedAttempts().map { it.id },
            )
        }
    }

    @Test
    fun savingUpdatedAttemptReplacesAttemptOwnedSnapshotOnly() = runTest {
        withTestDatabase { database ->
            insertAttemptFixtureCurriculum(database)
            val store = AssessmentAttemptStore(database)
            val original = TestAttempt(
                id = "attempt_update",
                config = AssessmentConfig.Mixed(questionCount = 2),
                questionAttempts = listOf(
                    QuestionAttempt("question_a"),
                    QuestionAttempt("question_b"),
                ),
                status = AssessmentStatus.IN_PROGRESS,
                startedAt = StartedAt,
            )
            val unrelated = TestAttempt(
                id = "attempt_unrelated",
                config = AssessmentConfig.Mixed(questionCount = 1),
                questionAttempts = listOf(answeredQuestionAttempt("question_c", "question_c_a")),
                status = AssessmentStatus.IN_PROGRESS,
                startedAt = StartedAt,
            )
            val updated = original.copy(
                questionAttempts = listOf(
                    answeredQuestionAttempt("question_a", "question_a_b", isCorrect = false),
                    QuestionAttempt("question_b"),
                ),
            )
            val changedAgain = updated.copy(
                questionAttempts = listOf(
                    answeredQuestionAttempt("question_a", "question_a_a"),
                    QuestionAttempt("question_b"),
                ),
            )

            store.save(original)
            store.save(unrelated)
            store.save(updated)
            store.save(changedAgain)

            assertEquals(changedAgain, store.getById("attempt_update"))
            assertEquals(unrelated, store.getById("attempt_unrelated"))
            assertEquals(2, database.assessmentAttemptDao().countTestAttempts())
            assertEquals(3, database.assessmentAttemptDao().countQuestionAttempts())
            assertEquals(
                setOf("question_a_a"),
                (store.getById("attempt_update")?.questionAttempts?.first()?.answerState as QuestionAnswerState.Answered)
                    .selectedAnswerIds,
            )
        }
    }

    @Test
    fun deprecatedQuestionReferenceCanBePersistedForHistory() = runTest {
        withTestDatabase { database ->
            insertAttemptFixtureCurriculum(database)
            database.curriculumDao().upsertQuestions(
                listOf(
                    questionEntity("question_c", status = "DEPRECATED", sortOrder = 2),
                ),
            )
            val store = AssessmentAttemptStore(database)
            val attempt = TestAttempt(
                id = "attempt_deprecated",
                config = AssessmentConfig.Mixed(questionCount = 1),
                questionAttempts = listOf(answeredQuestionAttempt("question_c", "question_c_a")),
                status = AssessmentStatus.IN_PROGRESS,
                startedAt = StartedAt,
            )

            store.save(attempt)

            assertEquals(attempt, store.getById("attempt_deprecated"))
        }
    }

    @Test
    fun selectedAnswerForeignKeyRejectsAnswerOwnedByAnotherQuestion() = runTest {
        withTestDatabase { database ->
            insertAttemptFixtureCurriculum(database)
            val dao = database.assessmentAttemptDao()
            dao.upsertTestAttempt(
                TestAttemptEntity(
                    id = "attempt_invalid_answer",
                    configType = "MIXED",
                    requestedQuestionCount = 1,
                    scopeType = null,
                    scopeId = null,
                    status = "IN_PROGRESS",
                    scoreTotalQuestions = null,
                    scoreCorrectAnswers = null,
                    startedAtEpochMillis = StartedAt.toEpochMilliseconds(),
                    completedAtEpochMillis = null,
                ),
            )
            dao.upsertQuestionAttempts(
                listOf(
                    QuestionAttemptEntity(
                        testAttemptId = "attempt_invalid_answer",
                        questionId = "question_a",
                        sortOrder = 0,
                        isCorrect = true,
                    ),
                ),
            )

            assertFails {
                dao.upsertSelectedAnswers(
                    listOf(
                        QuestionAttemptSelectedAnswerEntity(
                            testAttemptId = "attempt_invalid_answer",
                            questionId = "question_a",
                            answerId = "question_b_a",
                        ),
                    ),
                )
            }
        }
    }

    private suspend fun withTestDatabase(
        block: suspend (CurriculumDatabase) -> Unit,
    ) {
        val database = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        try {
            block(database)
        } finally {
            database.close()
        }
    }

    private suspend fun insertAttemptFixtureCurriculum(database: CurriculumDatabase) {
        val dao = database.curriculumDao()
        dao.upsertTopics(listOf(TopicEntity("topic", "Topic", "ACTIVE", sortOrder = 0)))
        dao.upsertSubtopics(listOf(SubtopicEntity("subtopic", "topic", "Subtopic", "ACTIVE", sortOrder = 0)))
        dao.upsertQuestions(
            listOf(
                questionEntity("question_a", sortOrder = 1),
                questionEntity("question_b", sortOrder = 0),
                questionEntity("question_c", sortOrder = 2),
            ),
        )
        dao.upsertAnswerOptions(
            listOf(
                AnswerOptionEntity("question_a", "question_a_a", "A", sortOrder = 0),
                AnswerOptionEntity("question_a", "question_a_b", "B", sortOrder = 1),
                AnswerOptionEntity("question_b", "question_b_a", "A", sortOrder = 0),
                AnswerOptionEntity("question_b", "question_b_b", "B", sortOrder = 1),
                AnswerOptionEntity("question_b", "question_b_c", "C", sortOrder = 2),
                AnswerOptionEntity("question_c", "question_c_a", "A", sortOrder = 0),
            ),
        )
    }

    private fun questionEntity(
        id: String,
        status: String = "ACTIVE",
        sortOrder: Int,
    ): QuestionEntity =
        QuestionEntity(
            id = id,
            topicId = "topic",
            subtopicId = "subtopic",
            text = "$id?",
            explanation = "$id explanation.",
            status = status,
            sortOrder = sortOrder,
        )

    private fun answeredQuestionAttempt(
        questionId: String,
        vararg answerIds: String,
        isCorrect: Boolean = true,
    ): QuestionAttempt =
        QuestionAttempt(
            questionId = questionId,
            answerState = QuestionAnswerState.Answered(
                selectedAnswerIds = answerIds.toSet(),
                isCorrect = isCorrect,
            ),
        )

    private fun completedAttempt(
        id: String,
        config: AssessmentConfig = AssessmentConfig.Mixed(questionCount = 1),
        startedAt: Instant,
        completedAt: Instant,
        questionAttempts: List<QuestionAttempt> = listOf(
            answeredQuestionAttempt("question_a", "question_a_a"),
        ),
    ): TestAttempt =
        TestAttempt(
            id = id,
            config = config,
            questionAttempts = questionAttempts,
            status = AssessmentStatus.COMPLETED,
            startedAt = startedAt,
            completedAt = completedAt,
            score = AssessmentScore(
                totalQuestions = questionAttempts.size,
                correctAnswers = questionAttempts.count {
                    (it.answerState as QuestionAnswerState.Answered).isCorrect
                },
            ),
        )
}

private val StartedAt = Instant.fromEpochMilliseconds(1_700_000_000_000)
private val CompletedAt = Instant.fromEpochMilliseconds(1_700_000_060_000)

private fun instantAfter(milliseconds: Long): Instant =
    Instant.fromEpochMilliseconds(StartedAt.toEpochMilliseconds() + milliseconds)
