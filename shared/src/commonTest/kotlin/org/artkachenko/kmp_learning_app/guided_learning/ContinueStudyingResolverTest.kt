package org.artkachenko.kmp_learning_app.guided_learning

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

internal class ContinueStudyingResolverTest {
    @Test
    fun noCompletedHistoryOffersNothingToContinue() = runTest {
        assertNull(resolve(emptyList()))
    }

    @Test
    fun anInProgressAttemptIsNeverAContinueContext() = runTest {
        // Passed deliberately, as a lower-level input that has not been filtered: an abandoned
        // attempt describes what the learner walked away from, and Continue Studying must not send
        // them back into it — this feature is not resume.
        assertNull(resolve(listOf(inProgressFocused(AssessmentScope.Topic("kotlin")))))
    }

    @Test
    fun aCompletedTopicRunReturnsToThatTopic() = runTest {
        val context = assertNotNullContext(
            resolve(listOf(completedFocused(AssessmentScope.Topic("kotlin")))),
        )

        assertEquals(ContinueStudyingTarget.Topic("kotlin"), context.target)
        assertEquals("Kotlin", context.scopeName)
        assertNull(context.parentTopicName)
    }

    @Test
    fun aCompletedSubtopicRunReturnsToThatSubtopicWithinItsParentTopic() = runTest {
        val context = assertNotNullContext(
            resolve(listOf(completedFocused(AssessmentScope.Subtopic("coroutines")))),
        )

        // The existing Topic detail route, opened at the Subtopic. No Continue-specific screen.
        assertEquals(
            ContinueStudyingTarget.Topic(topicId = "kotlin", subtopicId = "coroutines"),
            context.target,
        )
        assertEquals("Coroutines", context.scopeName)
        assertEquals("Kotlin", context.parentTopicName)
    }

    /** A rename is picked up with no migration: history stores IDs, the curriculum stores names. */
    @Test
    fun labelsComeFromTheCurrentCurriculumRatherThanFromHistory() = runTest {
        val context = assertNotNullContext(
            resolve(
                completedAttempts = listOf(completedFocused(AssessmentScope.Topic("kotlin"))),
                topics = listOf(Topic("kotlin", "Kotlin & Coroutines")),
            ),
        )

        assertEquals(ContinueStudyingTarget.Topic("kotlin"), context.target)
        assertEquals("Kotlin & Coroutines", context.scopeName)
    }

    @Test
    fun aTargetedTopicRunReturnsToAnEditablePracticePreset() = runTest {
        val context = assertNotNullContext(
            resolve(
                listOf(
                    completedFocused(
                        scope = AssessmentScope.Topic("kotlin"),
                        source = PracticeQuestionSource.WEAK_AREAS,
                    ),
                ),
            ),
        )

        // Scope and source only: question count and levels stay the builder's defaults, because
        // continuing a practice intent is not retaking the run that expressed it.
        assertEquals(
            ContinueStudyingTarget.Practice(
                PracticePreset(
                    scope = AssessmentScope.Topic("kotlin"),
                    source = PracticeQuestionSource.WEAK_AREAS,
                ),
            ),
            context.target,
        )
        assertEquals("Kotlin", context.scopeName)
    }

    @Test
    fun aTargetedSubtopicRunKeepsItsSubtopicScopedPreset() = runTest {
        val context = assertNotNullContext(
            resolve(
                listOf(
                    completedFocused(
                        scope = AssessmentScope.Subtopic("coroutines"),
                        source = PracticeQuestionSource.UNSEEN,
                    ),
                ),
            ),
        )

        assertEquals(
            ContinueStudyingTarget.Practice(
                PracticePreset(
                    scope = AssessmentScope.Subtopic("coroutines"),
                    source = PracticeQuestionSource.UNSEEN,
                ),
            ),
            context.target,
        )
        assertEquals("Coroutines", context.scopeName)
        assertEquals("Kotlin", context.parentTopicName)
    }

    @Test
    fun aMixedInterviewIsSkippedRatherThanHavingATopicInventedForIt() = runTest {
        val context = assertNotNullContext(
            resolve(
                listOf(
                    // Newest, and answered entirely on Kotlin questions. A Mixed run still cannot
                    // name one Topic, and guessing one from its Questions would make Continue
                    // Studying mean "repeat your latest test". The Interview area remains its own
                    // navigation destination.
                    completedMixed(),
                    completedFocused(AssessmentScope.Topic("android_ui")),
                ),
            ),
        )

        assertEquals(ContinueStudyingTarget.Topic("android_ui"), context.target)
        assertEquals("Android UI", context.scopeName)
    }

    @Test
    fun aStaleNewestContextDoesNotHideAnOlderUsableOne() = runTest {
        val context = assertNotNullContext(
            resolve(
                completedAttempts = listOf(
                    completedFocused(AssessmentScope.Topic("retired_topic")),
                    completedFocused(AssessmentScope.Topic("kotlin")),
                ),
                topics = listOf(
                    Topic("retired_topic", "Retired Topic", ContentStatus.DEPRECATED),
                    Topic("kotlin", "Kotlin"),
                ),
            ),
        )

        assertEquals(ContinueStudyingTarget.Topic("kotlin"), context.target)
    }

    @Test
    fun aTopicTheCurriculumNoLongerKnowsIsSkipped() = runTest {
        val context = assertNotNullContext(
            resolve(
                listOf(
                    completedFocused(AssessmentScope.Topic("deleted_topic")),
                    completedFocused(AssessmentScope.Topic("kotlin")),
                ),
            ),
        )

        assertEquals(ContinueStudyingTarget.Topic("kotlin"), context.target)
    }

    @Test
    fun aDeprecatedSubtopicDegradesToItsStillActiveParentTopic() = runTest {
        val context = assertNotNullContext(
            resolve(listOf(completedFocused(AssessmentScope.Subtopic("old_coroutines")))),
        )

        // Less specific, but still where the learner was working, and still a live destination.
        assertEquals(ContinueStudyingTarget.Topic("kotlin"), context.target)
        assertEquals("Kotlin", context.scopeName)
        assertNull(context.parentTopicName)
    }

    @Test
    fun aDeprecatedSubtopicPresetWidensToItsParentTopicAndKeepsItsSource() = runTest {
        val context = assertNotNullContext(
            resolve(
                listOf(
                    completedFocused(
                        scope = AssessmentScope.Subtopic("old_coroutines"),
                        source = PracticeQuestionSource.UNRESOLVED_MISTAKES,
                    ),
                ),
            ),
        )

        assertEquals(
            ContinueStudyingTarget.Practice(
                PracticePreset(
                    scope = AssessmentScope.Topic("kotlin"),
                    source = PracticeQuestionSource.UNRESOLVED_MISTAKES,
                ),
            ),
            context.target,
        )
    }

    @Test
    fun aSubtopicUnderADeprecatedTopicIsSkipped() = runTest {
        val context = assertNotNullContext(
            resolve(
                listOf(
                    completedFocused(AssessmentScope.Subtopic("retired_topic_subtopic")),
                    completedFocused(AssessmentScope.Topic("kotlin")),
                ),
            ),
        )

        assertEquals(ContinueStudyingTarget.Topic("kotlin"), context.target)
    }

    @Test
    fun aSubtopicWhoseParentCannotBeRecoveredIsSkipped() = runTest {
        val context = assertNotNullContext(
            resolve(
                listOf(
                    // Gone from the curriculum entirely, so there is no parent to degrade to and
                    // no dead navigation target to create.
                    completedFocused(AssessmentScope.Subtopic("deleted_subtopic")),
                    completedFocused(AssessmentScope.Subtopic("coroutines")),
                ),
            ),
        )

        assertEquals(
            ContinueStudyingTarget.Topic(topicId = "kotlin", subtopicId = "coroutines"),
            context.target,
        )
    }

    @Test
    fun historyWithNoUsableContextLeftOffersNothing() = runTest {
        assertNull(
            resolve(
                listOf(
                    completedMixed(),
                    completedFocused(AssessmentScope.Topic("deleted_topic")),
                    completedFocused(AssessmentScope.Subtopic("retired_topic_subtopic")),
                    inProgressFocused(AssessmentScope.Topic("kotlin")),
                ),
            ),
        )
    }

    private suspend fun resolve(
        completedAttempts: List<TestAttempt>,
        topics: List<Topic> = CurrentTopics,
        subtopics: List<Subtopic> = CurrentSubtopics,
    ): ContinueStudyingContext? =
        ContinueStudyingResolver(FakeCurriculumRepository(topics, subtopics))
            .resolve(completedAttempts)

    private fun assertNotNullContext(context: ContinueStudyingContext?): ContinueStudyingContext =
        context ?: error("Expected a continue-studying context.")

    /** Only the two identity lookups the resolver is allowed to make. */
    private class FakeCurriculumRepository(
        topics: List<Topic>,
        subtopics: List<Subtopic>,
    ) : CurriculumRepository {
        private val topicsById = topics.associateBy(Topic::id)
        private val subtopicsById = subtopics.associateBy(Subtopic::id)

        override suspend fun getTopicById(topicId: String): Topic? = topicsById[topicId]

        override suspend fun getSubtopicById(subtopicId: String): Subtopic? =
            subtopicsById[subtopicId]

        override suspend fun getActiveTopics(): List<Topic> = unused()
        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> = unused()
        override suspend fun getActiveQuestions(): List<Question> = unused()
        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> = unused()
        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
            unused()

        override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> =
            unused()

        override suspend fun getActiveQuestionsByTopicAndLevels(
            topicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = unused()

        override suspend fun getActiveQuestionsBySubtopicAndLevels(
            subtopicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = unused()

        override suspend fun getQuestionById(questionId: String): Question? = unused()

        private fun unused(): Nothing =
            error("ContinueStudyingResolver must resolve identity only, never content.")
    }

    private companion object {
        val CurrentTopics = listOf(
            Topic("kotlin", "Kotlin"),
            Topic("android_ui", "Android UI"),
            Topic("retired_topic", "Retired Topic", ContentStatus.DEPRECATED),
        )

        val CurrentSubtopics = listOf(
            Subtopic("coroutines", "kotlin", "Coroutines"),
            Subtopic("old_coroutines", "kotlin", "Old Coroutines", ContentStatus.DEPRECATED),
            // Still ACTIVE itself, but its parent Topic is not taught any more.
            Subtopic("retired_topic_subtopic", "retired_topic", "Retired Child"),
        )

        fun completedFocused(
            scope: AssessmentScope,
            source: PracticeQuestionSource = PracticeQuestionSource.ALL,
        ): TestAttempt =
            completedAttempt(
                AssessmentConfig.Focused(
                    scope = scope,
                    questionCount = 1,
                    source = source,
                ),
            )

        fun completedMixed(): TestAttempt = completedAttempt(AssessmentConfig.Mixed(1))

        fun completedAttempt(config: AssessmentConfig): TestAttempt =
            TestAttempt(
                id = "attempt_${config.hashCode()}",
                config = config,
                questionAttempts = listOf(
                    QuestionAttempt("q", QuestionAnswerState.Answered(setOf("a"), true)),
                ),
                status = AssessmentStatus.COMPLETED,
                startedAt = Instant.parse("2026-08-29T00:00:00Z"),
                completedAt = Instant.parse("2026-08-29T00:10:00Z"),
                score = AssessmentScore(totalQuestions = 1, correctAnswers = 1),
            )

        fun inProgressFocused(scope: AssessmentScope): TestAttempt =
            TestAttempt(
                id = "in_progress",
                config = AssessmentConfig.Focused(scope = scope, questionCount = 1),
                questionAttempts = listOf(QuestionAttempt("q", QuestionAnswerState.Unanswered)),
                status = AssessmentStatus.IN_PROGRESS,
                startedAt = Instant.parse("2026-08-30T00:00:00Z"),
            )
    }
}
