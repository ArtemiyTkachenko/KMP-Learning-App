package org.artkachenko.kmp_learning_app.data.local.assessment

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.assessment.AllQuestionLevels
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
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

    /**
     * A run the Practice Builder configured has to come back as the run the learner asked for.
     *
     * These two dimensions were deliberately unpersisted while nothing could vary them; now that
     * they can be, history describing a level-narrowed attempt as an all-levels one would be
     * wrong, and retake — which re-runs the reconstructed config — would widen the repeat.
     *
     * Every source value is covered: a source read back as something else would silently turn a
     * repeat of a targeted run into a repeat over a different candidate pool. All four source
     * policies are selectable, including unresolved mistakes.
     */
    @Test
    fun practiceLevelsAndSourceRoundTripOnAFocusedAttempt() = runTest {
        withTestDatabase { database ->
            insertAttemptFixtureCurriculum(database)
            val store = AssessmentAttemptStore(database)
            PracticeQuestionSource.entries.forEach { source ->
                val config = AssessmentConfig.Focused(
                    scope = AssessmentScope.Topic("topic"),
                    questionCount = 10,
                    levels = setOf(QuestionLevel.ADVANCED, QuestionLevel.FOUNDATION),
                    source = source,
                )
                val attemptId = "attempt_targeted_${source.name}"
                val attempt = TestAttempt(
                    id = attemptId,
                    config = config,
                    questionAttempts = listOf(QuestionAttempt("question_a")),
                    status = AssessmentStatus.IN_PROGRESS,
                    startedAt = StartedAt,
                )

                store.save(attempt)

                assertEquals(config, store.getById(attemptId)?.config)
            }
        }
    }

    /**
     * Rows written before the v6 columns existed carry no selection, and every one of them was an
     * all-levels ALL run. Reconstructing them that way is what keeps historical practice readable.
     */
    @Test
    fun aLegacyFocusedRowReconstructsAsAnAllLevelsAllRequest() = runTest {
        withTestDatabase { database ->
            insertAttemptFixtureCurriculum(database)
            val dao = database.assessmentAttemptDao()
            dao.upsertTestAttempt(
                TestAttemptEntity(
                    id = "attempt_legacy",
                    configType = "FOCUSED",
                    requestedQuestionCount = 10,
                    scopeType = "TOPIC",
                    scopeId = "topic",
                    practiceLevels = null,
                    practiceSource = null,
                    status = "IN_PROGRESS",
                    scoreTotalQuestions = null,
                    scoreCorrectAnswers = null,
                    startedAtEpochMillis = StartedAt.toEpochMilliseconds(),
                    completedAtEpochMillis = null,
                ),
            )
            dao.upsertQuestionAttempts(
                listOf(QuestionAttemptEntity("attempt_legacy", "question_a", sortOrder = 0, isCorrect = null)),
            )

            assertEquals(
                AssessmentConfig.Focused(
                    scope = AssessmentScope.Topic("topic"),
                    questionCount = 10,
                    levels = AllQuestionLevels,
                    source = PracticeQuestionSource.ALL,
                ),
                AssessmentAttemptStore(database).getById("attempt_legacy")?.config,
            )
        }
    }

    /**
     * A multi-Subtopic scope is stored inside the existing scope columns: a new discriminator and
     * a JSON payload in `scope_id`. No column and no schema version was added for it, because the
     * physical schema already holds "which kind of scope" plus "which content", and only the
     * payload's shape is new.
     *
     * The payload is asserted literally because it is a durable contract: rows written today are
     * read back by later versions, so the encoding cannot drift silently.
     */
    @Test
    fun aMultiSubtopicFocusedAttemptRoundTripsThroughTheExistingScopeColumns() = runTest {
        withTestDatabase { database ->
            insertAttemptFixtureCurriculum(database)
            val store = AssessmentAttemptStore(database)
            val config = AssessmentConfig.Focused(
                scope = AssessmentScope.Subtopics(setOf("compose_udf", "compose_fundamentals")),
                questionCount = 6,
                levels = setOf(QuestionLevel.FOUNDATION, QuestionLevel.APPLIED),
                source = PracticeQuestionSource.UNSEEN,
            )
            val attempt = TestAttempt(
                id = "attempt_subtopics",
                config = config,
                questionAttempts = listOf(answeredQuestionAttempt("question_a", "question_a_a")),
                status = AssessmentStatus.IN_PROGRESS,
                startedAt = StartedAt,
            )

            store.save(attempt)

            assertEquals(attempt, store.getById("attempt_subtopics"))
            val row = requireNotNull(
                database.assessmentAttemptDao().getTestAttemptById("attempt_subtopics"),
            )
            assertEquals("SUBTOPICS", row.scopeType)
            assertEquals("""["compose_fundamentals","compose_udf"]""", row.scopeId)
        }
    }

    /**
     * The domain scope is a Set, so the order its IDs were named in must not reach the database.
     * Two learners who configured the same practice have to produce the same row, or a retake, a
     * historical comparison, or any future migration would have to know that two different strings
     * mean one scope.
     */
    @Test
    fun equivalentMultiSubtopicScopesPersistIdentically() = runTest {
        withTestDatabase { database ->
            insertAttemptFixtureCurriculum(database)
            val store = AssessmentAttemptStore(database)
            val declaredOrder = AssessmentScope.Subtopics(setOf("sub_b", "sub_a", "sub_c"))
            val otherOrder = AssessmentScope.Subtopics(setOf("sub_c", "sub_a", "sub_b"))

            listOf("attempt_declared" to declaredOrder, "attempt_other" to otherOrder)
                .forEach { (id, scope) ->
                    store.save(
                        TestAttempt(
                            id = id,
                            config = AssessmentConfig.Focused(scope = scope, questionCount = 3),
                            questionAttempts = listOf(QuestionAttempt("question_a")),
                            status = AssessmentStatus.IN_PROGRESS,
                            startedAt = StartedAt,
                        ),
                    )
                }

            val dao = database.assessmentAttemptDao()
            assertEquals(
                dao.getTestAttemptById("attempt_declared")?.scopeId,
                dao.getTestAttemptById("attempt_other")?.scopeId,
            )
            assertEquals(
                store.getById("attempt_declared")?.config,
                store.getById("attempt_other")?.config,
            )
        }
    }

    /**
     * Rows written before multi-Subtopic scopes existed store the scope ID bare, and they still
     * decode that way. The new scope is additive: a new discriminator with its own decoder rather
     * than a re-serialisation of every stored scope.
     */
    @Test
    fun preExistingTopicAndSubtopicScopeRowsStillReconstruct() = runTest {
        withTestDatabase { database ->
            insertAttemptFixtureCurriculum(database)
            val dao = database.assessmentAttemptDao()
            listOf(
                Triple("attempt_stored_topic", "TOPIC", "topic"),
                Triple("attempt_stored_subtopic", "SUBTOPIC", "subtopic"),
            ).forEach { (attemptId, scopeType, scopeId) ->
                dao.upsertTestAttempt(
                    storedFocusedAttemptRow(
                        id = attemptId,
                        scopeType = scopeType,
                        scopeId = scopeId,
                        practiceLevels = "FOUNDATION,ADVANCED",
                        practiceSource = "WEAK_AREAS",
                    ),
                )
                dao.upsertQuestionAttempts(
                    listOf(QuestionAttemptEntity(attemptId, "question_a", sortOrder = 0, isCorrect = null)),
                )
            }

            val store = AssessmentAttemptStore(database)
            assertEquals(
                AssessmentConfig.Focused(
                    scope = AssessmentScope.Topic("topic"),
                    questionCount = 10,
                    levels = setOf(QuestionLevel.FOUNDATION, QuestionLevel.ADVANCED),
                    source = PracticeQuestionSource.WEAK_AREAS,
                ),
                store.getById("attempt_stored_topic")?.config,
            )
            assertEquals(
                AssessmentConfig.Focused(
                    scope = AssessmentScope.Subtopic("subtopic"),
                    questionCount = 10,
                    levels = setOf(QuestionLevel.FOUNDATION, QuestionLevel.ADVANCED),
                    source = PracticeQuestionSource.WEAK_AREAS,
                ),
                store.getById("attempt_stored_subtopic")?.config,
            )
        }
    }

    /**
     * An unreadable scope fails the reconstruction instead of being repaired. Salvaging the IDs
     * that happen to parse, or widening to a Topic, would hand the learner a different assessment
     * than the row records — the exact outcome an authoritative stored config exists to prevent.
     */
    @Test
    fun aMalformedMultiSubtopicScopePayloadFailsReconstruction() = runTest {
        withTestDatabase { database ->
            insertAttemptFixtureCurriculum(database)
            val dao = database.assessmentAttemptDao()
            listOf(
                "attempt_not_json" to "compose_fundamentals|compose_udf",
                "attempt_empty_scope" to "[]",
            ).forEach { (attemptId, scopeId) ->
                dao.upsertTestAttempt(
                    storedFocusedAttemptRow(
                        id = attemptId,
                        scopeType = "SUBTOPICS",
                        scopeId = scopeId,
                    ),
                )
                dao.upsertQuestionAttempts(
                    listOf(QuestionAttemptEntity(attemptId, "question_a", sortOrder = 0, isCorrect = null)),
                )
            }

            val store = AssessmentAttemptStore(database)
            assertFails { store.getById("attempt_not_json") }
            assertFails { store.getById("attempt_empty_scope") }
        }
    }

    /** Mixed has no level or source dimension, so its rows keep both columns null. */
    @Test
    fun mixedAttemptsStoreNoPracticeSelection() = runTest {
        withTestDatabase { database ->
            insertAttemptFixtureCurriculum(database)
            val store = AssessmentAttemptStore(database)
            val mixed = TestAttempt(
                id = "attempt_mixed_selection",
                config = AssessmentConfig.Mixed(questionCount = 3),
                questionAttempts = listOf(QuestionAttempt("question_a")),
                status = AssessmentStatus.IN_PROGRESS,
                startedAt = StartedAt,
            )

            store.save(mixed)

            val stored = database.assessmentAttemptDao().getTestAttemptById("attempt_mixed_selection")
            assertNull(stored?.practiceLevels)
            assertNull(stored?.practiceSource)
            assertEquals(mixed, store.getById("attempt_mixed_selection"))
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
                    practiceLevels = null,
                    practiceSource = null,
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
            selectionMode = "SINGLE",
            level = "FOUNDATION",
            explanation = "$id explanation.",
            status = status,
            sortOrder = sortOrder,
        )

    private fun storedFocusedAttemptRow(
        id: String,
        scopeType: String,
        scopeId: String,
        practiceLevels: String? = null,
        practiceSource: String? = null,
    ): TestAttemptEntity =
        TestAttemptEntity(
            id = id,
            configType = "FOCUSED",
            requestedQuestionCount = 10,
            scopeType = scopeType,
            scopeId = scopeId,
            practiceLevels = practiceLevels,
            practiceSource = practiceSource,
            status = "IN_PROGRESS",
            scoreTotalQuestions = null,
            scoreCorrectAnswers = null,
            startedAtEpochMillis = StartedAt.toEpochMilliseconds(),
            completedAtEpochMillis = null,
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
