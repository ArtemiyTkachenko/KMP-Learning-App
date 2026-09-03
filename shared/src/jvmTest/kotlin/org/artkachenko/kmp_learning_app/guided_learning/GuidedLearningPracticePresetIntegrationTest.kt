package org.artkachenko.kmp_learning_app.guided_learning

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.AppRoute
import org.artkachenko.kmp_learning_app.assessment.AllQuestionLevels
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewService
import org.artkachenko.kmp_learning_app.toAppRoute
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.DefaultPracticeQuestionCount
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeAvailability
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderEvent
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.PracticeBuilderViewModel
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.toAssessmentScope

/**
 * E17-05: the seam between a recommendation and the EPIC-16 Practice Builder, over real derivations.
 *
 * The layers on either side already have their own proof. `LearningRecommendationPolicyTest` and
 * `LearningRecommendationResolverTest` establish which action is chosen, `AppNavigationTest`
 * establishes the route a target maps to, and `PracticeBuilderViewModelTest` establishes what the
 * builder does with an arriving source. What none of them can show is whether a preset the *policy
 * itself produced from real history* is still a runnable, editable configuration by the time the
 * builder has preflighted it against current content — that is, whether guidance recommends
 * practice that actually exists.
 *
 * So the whole chain runs here with production components and nothing stubbed in the middle: the
 * real `LearningProgressService` and `MistakeReviewService` derive the facts, the real policy
 * chooses, the real route mapping carries the preset, and the real `PracticeBuilderViewModel`
 * re-checks it through the real `AssessmentQuestionSelector`. Only the two repositories are
 * in-memory, and only the selector's shuffle is replaced by identity, so an assertion about
 * *which* Questions are eligible stays deterministic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class GuidedLearningPracticePresetIntegrationTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun aWeakAreaRecommendationOpensAValidBuilderOnTheBuildersOwnDefaults() = runGuidedTest {
        val curriculum = FakeCurriculumRepository()
        // Every coroutines Question answered wrongly once and correctly since: nothing is
        // unresolved, and 50% over four occurrences is weak by the established policy.
        val history = MutableHistoryRepository(weakCoroutinesHistory())

        val recommendation = assertNotNull(recommend(curriculum, history))
        val preset = assertIs<LearningRecommendationTarget.Practice>(recommendation.target).preset
        assertEquals(AssessmentScope.Subtopic("coroutines"), preset.scope)
        assertEquals(PracticeQuestionSource.WEAK_AREAS, preset.source)

        val builder = builderFor(recommendation, curriculum, history)
        advanceUntilIdle()

        val state = builder.uiState.value
        // Scope and source arrive exactly as the policy chose them, through the ordinary route.
        assertEquals(PracticeQuestionSource.WEAK_AREAS, state.source)
        assertEquals("Coroutines", state.scope.name)
        // The preset carries nothing else, so the builder's own defaults still decide the run.
        assertEquals(DefaultPracticeQuestionCount, state.questionCount)
        assertEquals(AllQuestionLevels, state.levels)
        // And the recommendation is worth acting on: the weak scope still has content to ask.
        assertEquals(PracticeAvailability.Available(eligibleQuestionCount = 4), state.availability)
        assertTrue(state.isStartEnabled)
    }

    @Test
    fun aRecommendedPresetRemainsAnOrdinaryEditableConfiguration() = runGuidedTest {
        val curriculum = FakeCurriculumRepository()
        val history = MutableHistoryRepository(weakCoroutinesHistory())
        val recommendation = assertNotNull(recommend(curriculum, history))

        val builder = builderFor(recommendation, curriculum, history)
        advanceUntilIdle()

        // The learner edits the two dimensions the preset deliberately left alone.
        builder.selectQuestionCount(5)
        builder.toggleLevel(QuestionLevel.ADVANCED)
        builder.toggleLevel(QuestionLevel.APPLIED)
        advanceUntilIdle()

        // Narrowing the levels is re-preflighted rather than assumed: two of the four eligible
        // Questions are ADVANCED, so the offer shrinks with the selection.
        assertEquals(
            PracticeAvailability.Available(eligibleQuestionCount = 2),
            builder.uiState.value.availability,
        )

        val started = async { builder.events.first() }
        builder.startPractice()
        advanceUntilIdle()

        // An ordinary focused configuration, indistinguishable from a hand-made one: the
        // recommendation decided the scope and source, and the learner decided the rest.
        assertEquals(
            AssessmentConfig.Focused(
                scope = AssessmentScope.Subtopic("coroutines"),
                questionCount = 5,
                levels = setOf(QuestionLevel.FOUNDATION),
                source = PracticeQuestionSource.WEAK_AREAS,
            ),
            assertIs<PracticeBuilderEvent.StartPractice>(started.await()).config,
        )
    }

    @Test
    fun aRecommendedPresetIsPreflightedAgainstCurrentHistoryRatherThanASnapshot() = runGuidedTest {
        val curriculum = FakeCurriculumRepository()
        // Coroutines answered correctly, so nothing is unresolved or weak and the least covered
        // Topic is the one never touched.
        val history = MutableHistoryRepository(
            listOf(completedAttempt("seen", "q_coroutines_1" to true, "q_coroutines_2" to true)),
        )

        val recommendation = assertNotNull(recommend(curriculum, history))
        val preset = assertIs<LearningRecommendationTarget.Practice>(recommendation.target).preset
        assertEquals(AssessmentScope.Topic("compose"), preset.scope)
        assertEquals(PracticeQuestionSource.UNSEEN, preset.source)
        assertEquals(
            LearningRecommendationRationale.UnseenCoverage(
                topicId = "compose",
                unseenQuestionCount = 3,
            ),
            recommendation.rationale,
        )

        // Time passes between the card being drawn and being tapped, and the learner studies two of
        // the three unseen Questions in the meantime.
        history.attempts = listOf(
            completedAttempt("later", "q_compose_1" to true, "q_compose_2" to true),
        ) + history.attempts

        val builder = builderFor(recommendation, curriculum, history)
        advanceUntilIdle()

        // The builder reports what is unseen *now*. No candidate IDs travelled with the preset, so
        // there is nothing stale to reconcile — the scope and source are re-evaluated instead.
        assertEquals(
            PracticeAvailability.Available(eligibleQuestionCount = 1),
            builder.uiState.value.availability,
        )
    }

    /** The production derivation chain, exactly as `TopicBrowserViewModel` assembles it. */
    private suspend fun recommend(
        curriculum: CurriculumRepository,
        history: AssessmentRepository,
    ): LearningRecommendation? {
        val completedAttempts = history.getCompletedAttempts()
        val progress = LearningProgressService(history, curriculum).load(completedAttempts)
        val mistakeReviewService = MistakeReviewService(
            assessmentRepository = history,
            assessmentReviewLoader = AssessmentReviewLoader(curriculum),
        )
        val resolver = LearningRecommendationResolver { attempts ->
            mistakeReviewService.countUnresolved(attempts)
        }
        return resolver.resolve(completedAttempts = completedAttempts, progress = progress)
    }

    /**
     * Opens the builder the way the shell does: through the semantic target's own route, so the
     * scope and source the builder receives are the ones navigation actually carries.
     */
    private fun TestScope.builderFor(
        recommendation: LearningRecommendation,
        curriculum: CurriculumRepository,
        history: AssessmentRepository,
    ): PracticeBuilderViewModel {
        val route = recommendation.target.toAppRoute()
        val (scope, source) = when (route) {
            is AppRoute.PracticeBuilderTopic -> route.toAssessmentScope() to route.source
            is AppRoute.PracticeBuilderSubtopic -> route.toAssessmentScope() to route.source
            else -> error("Expected a practice recommendation but was $route.")
        }
        return PracticeBuilderViewModel(
            scope = scope,
            curriculumRepository = curriculum,
            questionSelector = AssessmentQuestionSelector(
                curriculumRepository = curriculum,
                completedHistory = { history.getCompletedAttempts() },
                randomize = { it },
            ),
            initialSource = source,
        )
    }

    private fun runGuidedTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }

    private class MutableHistoryRepository(
        var attempts: List<TestAttempt>,
    ) : AssessmentRepository {
        override suspend fun save(attempt: TestAttempt) = Unit
        override suspend fun getById(attemptId: String): TestAttempt? = null
        override suspend fun getCompletedAttempts(): List<TestAttempt> = attempts
    }

    /** The ACTIVE bank only: nothing here has been retired, so every read is current content. */
    private class FakeCurriculumRepository : CurriculumRepository {
        override suspend fun getActiveTopics(): List<Topic> = Topics

        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> =
            Subtopics.filter { it.topicId == topicId }

        override suspend fun getActiveQuestions(): List<Question> = Questions

        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> =
            Questions.filter { it.topicId == topicId }

        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
            Questions.filter { it.subtopicId == subtopicId }

        override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> =
            Questions.filter { it.level in levels }

        override suspend fun getActiveQuestionsByTopicAndLevels(
            topicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = getActiveQuestionsByTopic(topicId).filter { it.level in levels }

        override suspend fun getActiveQuestionsBySubtopicAndLevels(
            subtopicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = getActiveQuestionsBySubtopic(subtopicId).filter { it.level in levels }

        override suspend fun getTopicById(topicId: String): Topic? =
            Topics.firstOrNull { it.id == topicId }

        override suspend fun getSubtopicById(subtopicId: String): Subtopic? =
            Subtopics.firstOrNull { it.id == subtopicId }

        override suspend fun getQuestionById(questionId: String): Question? =
            Questions.firstOrNull { it.id == questionId }
    }

    private companion object {
        val Topics = listOf(
            Topic("compose", "Compose UI"),
            Topic("kotlin", "Kotlin Language & JVM Fundamentals"),
        )

        val Subtopics = listOf(
            Subtopic("compose_runtime", "compose", "Compose runtime"),
            Subtopic("coroutines", "kotlin", "Coroutines"),
        )

        /**
         * Three unseen Compose Questions, and four coroutines Questions split across levels so a
         * level edit has something to narrow.
         */
        val Questions = listOf(
            question("q_compose_1", "compose", "compose_runtime", QuestionLevel.FOUNDATION),
            question("q_compose_2", "compose", "compose_runtime", QuestionLevel.FOUNDATION),
            question("q_compose_3", "compose", "compose_runtime", QuestionLevel.ADVANCED),
            question("q_coroutines_1", "kotlin", "coroutines", QuestionLevel.FOUNDATION),
            question("q_coroutines_2", "kotlin", "coroutines", QuestionLevel.FOUNDATION),
            question("q_coroutines_3", "kotlin", "coroutines", QuestionLevel.ADVANCED),
            question("q_coroutines_4", "kotlin", "coroutines", QuestionLevel.ADVANCED),
        )

        /** Newest first, as the repository contract requires. */
        fun weakCoroutinesHistory(): List<TestAttempt> =
            listOf(
                completedAttempt("newer", "q_coroutines_1" to true, "q_coroutines_2" to true),
                completedAttempt("older", "q_coroutines_1" to false, "q_coroutines_2" to false),
            )

        fun completedAttempt(
            id: String,
            vararg answers: Pair<String, Boolean>,
        ): TestAttempt =
            TestAttempt(
                id = id,
                config = AssessmentConfig.Mixed(answers.size),
                questionAttempts = answers.map { (questionId, isCorrect) ->
                    QuestionAttempt(
                        questionId,
                        QuestionAnswerState.Answered(setOf("${questionId}_a"), isCorrect),
                    )
                },
                status = AssessmentStatus.COMPLETED,
                startedAt = Instant.parse("2026-09-01T00:00:00Z"),
                completedAt = Instant.parse("2026-09-01T00:15:00Z"),
                score = AssessmentScore(answers.size, answers.count { it.second }),
            )

        fun question(
            id: String,
            topicId: String,
            subtopicId: String,
            level: QuestionLevel,
        ): Question = Question(
            id = id,
            topicId = topicId,
            subtopicId = subtopicId,
            text = "Question $id",
            answers = listOf(AnswerOption("${id}_a", "Answer A")),
            selectionMode = AnswerSelectionMode.SINGLE,
            level = level,
            correctAnswerIds = listOf("${id}_a"),
            explanation = "Explanation",
            sources = listOf(SourceReference("Source", "https://example.com")),
        )
    }
}
