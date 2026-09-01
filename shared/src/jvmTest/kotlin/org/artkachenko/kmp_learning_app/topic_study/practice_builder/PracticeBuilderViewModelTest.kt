package org.artkachenko.kmp_learning_app.topic_study.practice_builder

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
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
import kotlin.time.Instant
import org.artkachenko.kmp_learning_app.assessment.AllQuestionLevels
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

@OptIn(ExperimentalCoroutinesApi::class)
internal class PracticeBuilderViewModelTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun opensOnTheLaunchingTopicWithAStartableDefaultSetup() = runViewModelTest {
        val viewModel = viewModel(AssessmentScope.Topic("topic_a"), FakeCurriculumRepository())

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PracticeScopeKind.TOPIC, state.scope.kind)
        assertEquals("Topic A", state.scope.name)
        // The default is the count one-tap focused practice always used, so arriving and pressing
        // Start reproduces the run this screen replaced.
        assertEquals(DefaultPracticeQuestionCount, state.questionCount)
        assertTrue(QuestionLevel.FOUNDATION in state.levels)
        assertTrue(QuestionLevel.APPLIED in state.levels)
        assertTrue(QuestionLevel.ADVANCED in state.levels)
        assertEquals(AllQuestionLevels, state.levels)
        assertEquals(PracticeQuestionSource.ALL, state.source)
        assertTrue(state.isStartEnabled)
    }

    @Test
    fun opensOnTheLaunchingSubtopic() = runViewModelTest {
        val viewModel = viewModel(AssessmentScope.Subtopic("subtopic_a"), FakeCurriculumRepository())

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PracticeScopeKind.SUBTOPIC, state.scope.kind)
        assertEquals("Subtopic A", state.scope.name)
        assertTrue(state.isStartEnabled)
    }

    /** The scope label is presentation only; a scope whose name will not resolve still runs. */
    @Test
    fun anUnresolvableScopeNameDoesNotBlockPractice() = runViewModelTest {
        val curriculum = FakeCurriculumRepository(topics = emptyList())
        val viewModel = viewModel(AssessmentScope.Topic("topic_a"), curriculum)

        advanceUntilIdle()

        assertNull(viewModel.uiState.value.scope.name)
        assertTrue(viewModel.uiState.value.isStartEnabled)
    }

    @Test
    fun editingTheQuestionCountUpdatesStateAndTheStartedConfiguration() = runViewModelTest {
        val viewModel = viewModel(AssessmentScope.Topic("topic_a"), FakeCurriculumRepository())
        advanceUntilIdle()

        viewModel.selectQuestionCount(5)

        assertEquals(5, viewModel.uiState.value.questionCount)
        assertEquals(5, startedConfig(viewModel).questionCount)
    }

    /** The control offers a fixed ladder, which is what keeps the count positive without a guard. */
    @Test
    fun anUnofferedCountIsRejected() = runViewModelTest {
        val viewModel = viewModel(AssessmentScope.Topic("topic_a"), FakeCurriculumRepository())
        advanceUntilIdle()

        viewModel.selectQuestionCount(0)
        viewModel.selectQuestionCount(-3)
        viewModel.selectQuestionCount(999)

        assertEquals(DefaultPracticeQuestionCount, viewModel.uiState.value.questionCount)
    }

    @Test
    fun levelsCanBeDeselectedAndReselected() = runViewModelTest {
        val viewModel = viewModel(AssessmentScope.Topic("topic_a"), FakeCurriculumRepository())
        advanceUntilIdle()

        viewModel.toggleLevel(QuestionLevel.FOUNDATION)
        assertEquals(
            setOf(QuestionLevel.APPLIED, QuestionLevel.ADVANCED),
            viewModel.uiState.value.levels,
        )

        viewModel.toggleLevel(QuestionLevel.APPLIED)
        assertEquals(setOf(QuestionLevel.ADVANCED), viewModel.uiState.value.levels)

        viewModel.toggleLevel(QuestionLevel.FOUNDATION)
        assertEquals(
            setOf(QuestionLevel.FOUNDATION, QuestionLevel.ADVANCED),
            viewModel.uiState.value.levels,
        )
    }

    /**
     * The invariant lives here rather than in the Composable: an empty selection is representable
     * in the domain and explicitly non-runnable, so a screen that could reach it would strand the
     * learner on a Start button that can never work.
     */
    @Test
    fun theFinalSelectedLevelCannotBeRemoved() = runViewModelTest {
        val viewModel = viewModel(AssessmentScope.Topic("topic_a"), FakeCurriculumRepository())
        advanceUntilIdle()

        viewModel.toggleLevel(QuestionLevel.FOUNDATION)
        viewModel.toggleLevel(QuestionLevel.APPLIED)
        viewModel.toggleLevel(QuestionLevel.ADVANCED)
        advanceUntilIdle()

        assertEquals(setOf(QuestionLevel.ADVANCED), viewModel.uiState.value.levels)
        assertTrue(viewModel.uiState.value.isStartEnabled)
    }

    @Test
    fun theStartedConfigurationContainsExactlyTheSelectedLevels() = runViewModelTest {
        val viewModel = viewModel(AssessmentScope.Topic("topic_a"), FakeCurriculumRepository())
        advanceUntilIdle()

        viewModel.toggleLevel(QuestionLevel.APPLIED)
        advanceUntilIdle()

        assertEquals(
            setOf(QuestionLevel.FOUNDATION, QuestionLevel.ADVANCED),
            startedConfig(viewModel).levels,
        )
    }

    @Test
    fun everySourceIsRepresentedButOnlyTheImplementedOnesAreAvailable() = runViewModelTest {
        val viewModel = viewModel(AssessmentScope.Topic("topic_a"), FakeCurriculumRepository())
        advanceUntilIdle()

        val options = viewModel.uiState.value.sourceOptions
        // Every product source is listed, so the screen shows what targeted practice will offer.
        assertEquals(PracticeQuestionSource.entries, options.map { it.source })
        assertEquals(
            setOf(PracticeQuestionSource.ALL, PracticeQuestionSource.UNSEEN),
            options.filter { it.isAvailable }.map { it.source }.toSet(),
        )
    }

    @Test
    fun anUnavailableSourceCannotBecomeTheActiveSource() = runViewModelTest {
        val viewModel = viewModel(AssessmentScope.Topic("topic_a"), FakeCurriculumRepository())
        advanceUntilIdle()

        viewModel.selectSource(PracticeQuestionSource.WEAK_AREAS)
        viewModel.selectSource(PracticeQuestionSource.UNRESOLVED_MISTAKES)
        advanceUntilIdle()

        assertEquals(PracticeQuestionSource.ALL, viewModel.uiState.value.source)
        // ALL is what was configured all along, not a substitution made on the way out.
        assertEquals(PracticeQuestionSource.ALL, startedConfig(viewModel).source)
        assertTrue(viewModel.uiState.value.isStartEnabled)
    }

    @Test
    fun choosingUnseenRechecksAvailabilityAgainstTheUnseenPool() = runViewModelTest {
        // One of the three Questions in scope has already been answered in completed history.
        val viewModel = viewModel(
            scope = AssessmentScope.Topic("topic_a"),
            curriculum = FakeCurriculumRepository(),
            seenQuestionIds = listOf("q_foundation"),
        )
        advanceUntilIdle()
        assertEquals(3, availableCount(viewModel))

        viewModel.selectSource(PracticeQuestionSource.UNSEEN)
        advanceUntilIdle()

        assertEquals(PracticeQuestionSource.UNSEEN, viewModel.uiState.value.source)
        assertEquals(2, availableCount(viewModel))
        assertTrue(viewModel.uiState.value.isStartEnabled)
        assertEquals(PracticeQuestionSource.UNSEEN, startedConfig(viewModel).source)
    }

    /**
     * A supported source with nothing left to ask is not an unavailable source: unseen stays
     * selectable and reports no content, rather than reverting to ALL and practising Questions the
     * learner has already answered.
     */
    @Test
    fun unseenPracticeWithNothingLeftToAskDisablesStartWithoutChangingTheSource() = runViewModelTest {
        val viewModel = viewModel(
            scope = AssessmentScope.Topic("topic_a"),
            curriculum = FakeCurriculumRepository(),
            seenQuestionIds = listOf("q_foundation", "q_applied", "q_advanced"),
        )
        advanceUntilIdle()

        viewModel.selectSource(PracticeQuestionSource.UNSEEN)
        advanceUntilIdle()

        assertEquals(PracticeQuestionSource.UNSEEN, viewModel.uiState.value.source)
        assertTrue(
            viewModel.uiState.value.sourceOptions
                .single { it.source == PracticeQuestionSource.UNSEEN }
                .isAvailable,
        )
        assertEquals(PracticeAvailability.NoEligibleQuestions, viewModel.uiState.value.availability)
        assertFalse(viewModel.uiState.value.isStartEnabled)
    }

    @Test
    fun anEligibleConfigurationEnablesStartAndReportsWhatIsAvailable() = runViewModelTest {
        val viewModel = viewModel(AssessmentScope.Topic("topic_a"), FakeCurriculumRepository())

        advanceUntilIdle()

        val availability =
            assertIs<PracticeAvailability.Available>(viewModel.uiState.value.availability)
        assertEquals(3, availability.eligibleQuestionCount)
        assertTrue(viewModel.uiState.value.isStartEnabled)
    }

    @Test
    fun noEligibleQuestionsDisablesStart() = runViewModelTest {
        val curriculum = FakeCurriculumRepository(questions = emptyList())
        val viewModel = viewModel(AssessmentScope.Topic("topic_a"), curriculum)

        advanceUntilIdle()

        assertEquals(PracticeAvailability.NoEligibleQuestions, viewModel.uiState.value.availability)
        assertFalse(viewModel.uiState.value.isStartEnabled)
    }

    @Test
    fun availabilityRefreshesWhenTheLevelSelectionChanges() = runViewModelTest {
        // Only a FOUNDATION Question exists, so narrowing to ADVANCED empties the selection.
        val curriculum = FakeCurriculumRepository(
            questions = listOf(question("q_foundation", QuestionLevel.FOUNDATION)),
        )
        val viewModel = viewModel(AssessmentScope.Topic("topic_a"), curriculum)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isStartEnabled)

        viewModel.toggleLevel(QuestionLevel.FOUNDATION)
        viewModel.toggleLevel(QuestionLevel.APPLIED)
        advanceUntilIdle()

        assertEquals(setOf(QuestionLevel.ADVANCED), viewModel.uiState.value.levels)
        assertEquals(PracticeAvailability.NoEligibleQuestions, viewModel.uiState.value.availability)
        assertFalse(viewModel.uiState.value.isStartEnabled)
    }

    @Test
    fun aFailedEligibilityCheckIsAnErrorThatRetryCanRecoverFrom() = runViewModelTest {
        val curriculum = FakeCurriculumRepository(failuresRemaining = 1)
        val viewModel = viewModel(AssessmentScope.Topic("topic_a"), curriculum)
        advanceUntilIdle()
        assertEquals(PracticeAvailability.Error, viewModel.uiState.value.availability)
        assertFalse(viewModel.uiState.value.isStartEnabled)

        viewModel.retryAvailability()
        advanceUntilIdle()

        assertIs<PracticeAvailability.Available>(viewModel.uiState.value.availability)
    }

    /**
     * Availability is read through the scoped, level-aware curriculum reads the selector makes.
     * Nothing here can create an attempt: the builder is given the selection boundary and no
     * repository or engine that could persist one.
     */
    @Test
    fun availabilityIsReadThroughScopedSelectionOnly() = runViewModelTest {
        val curriculum = FakeCurriculumRepository()
        val viewModel = viewModel(AssessmentScope.Topic("topic_a"), curriculum)

        advanceUntilIdle()

        assertEquals(listOf("topic:topic_a"), curriculum.selectionCalls)
        assertTrue(viewModel.uiState.value.isStartEnabled)
    }

    @Test
    fun startEmitsTheCompleteTypedConfiguration() = runViewModelTest {
        val viewModel = viewModel(AssessmentScope.Subtopic("subtopic_a"), FakeCurriculumRepository())
        advanceUntilIdle()
        viewModel.selectQuestionCount(15)
        viewModel.toggleLevel(QuestionLevel.APPLIED)
        advanceUntilIdle()

        assertEquals(
            AssessmentConfig.Focused(
                scope = AssessmentScope.Subtopic("subtopic_a"),
                questionCount = 15,
                levels = setOf(QuestionLevel.FOUNDATION, QuestionLevel.ADVANCED),
                source = PracticeQuestionSource.ALL,
            ),
            startedConfig(viewModel),
        )
    }

    @Test
    fun startIsIgnoredWhileTheConfigurationCannotRun() = runViewModelTest {
        val curriculum = FakeCurriculumRepository(questions = emptyList())
        val viewModel = viewModel(AssessmentScope.Topic("topic_a"), curriculum)
        advanceUntilIdle()

        val event = async { viewModel.events.first() }
        viewModel.startPractice()
        advanceUntilIdle()

        assertTrue(event.isActive, "Start must emit nothing while practice cannot run.")
        event.cancel()
    }

    private fun availableCount(viewModel: PracticeBuilderViewModel): Int =
        assertIs<PracticeAvailability.Available>(viewModel.uiState.value.availability)
            .eligibleQuestionCount

    private suspend fun TestScope.startedConfig(
        viewModel: PracticeBuilderViewModel,
    ): AssessmentConfig.Focused {
        val event = async { viewModel.events.first() }
        viewModel.startPractice()
        advanceUntilIdle()
        return assertIs<PracticeBuilderEvent.StartPractice>(event.await()).config
    }

    /**
     * [seenQuestionIds] is the only history input the builder needs: which Questions are unseen is
     * the selector's answer, and proving it belongs to the selector's own tests rather than here.
     */
    private fun viewModel(
        scope: AssessmentScope,
        curriculum: CurriculumRepository,
        seenQuestionIds: List<String> = emptyList(),
    ): PracticeBuilderViewModel =
        PracticeBuilderViewModel(
            scope = scope,
            curriculumRepository = curriculum,
            questionSelector = AssessmentQuestionSelector(
                curriculumRepository = curriculum,
                completedHistory = { completedHistoryOf(seenQuestionIds) },
                randomize = { it },
            ),
        )

    private fun completedHistoryOf(seenQuestionIds: List<String>): List<TestAttempt> {
        if (seenQuestionIds.isEmpty()) return emptyList()
        return listOf(
            TestAttempt(
                id = "completed_attempt",
                config = AssessmentConfig.Mixed(questionCount = seenQuestionIds.size),
                questionAttempts = seenQuestionIds.map { questionId ->
                    QuestionAttempt(
                        questionId = questionId,
                        answerState = QuestionAnswerState.Answered(
                            selectedAnswerIds = setOf("${questionId}_a"),
                            isCorrect = true,
                        ),
                    )
                },
                status = AssessmentStatus.COMPLETED,
                startedAt = Instant.fromEpochSeconds(0),
                completedAt = Instant.fromEpochSeconds(60),
                score = AssessmentScore(
                    totalQuestions = seenQuestionIds.size,
                    correctAnswers = seenQuestionIds.size,
                ),
            ),
        )
    }

    private fun runViewModelTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }

    /**
     * One Topic with one Subtopic and one Question per level, which is enough for every level
     * combination the builder can produce to be either populated or provably empty.
     */
    private class FakeCurriculumRepository(
        private val topics: List<Topic> = listOf(Topic("topic_a", "Topic A")),
        private val subtopics: List<Subtopic> =
            listOf(Subtopic("subtopic_a", "topic_a", "Subtopic A")),
        private val questions: List<Question> = QuestionLevel.entries.map { level ->
            question("q_${level.name.lowercase()}", level)
        },
        private var failuresRemaining: Int = 0,
    ) : CurriculumRepository {
        /** Every scoped, level-aware read the selector made, in order. */
        val selectionCalls = mutableListOf<String>()

        override suspend fun getActiveTopics(): List<Topic> = topics

        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> =
            subtopics.filter { it.topicId == topicId }

        override suspend fun getActiveQuestions(): List<Question> = questions

        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> =
            error("Targeted practice must use the level-aware read.")

        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
            error("Targeted practice must use the level-aware read.")

        override suspend fun getActiveQuestionsByLevels(
            levels: Set<QuestionLevel>,
        ): List<Question> = questions.filter { it.level in levels }

        override suspend fun getActiveQuestionsByTopicAndLevels(
            topicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> {
            selectionCalls += "topic:$topicId"
            failOnce()
            return questions.filter { it.topicId == topicId && it.level in levels }
        }

        override suspend fun getActiveQuestionsBySubtopicAndLevels(
            subtopicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> {
            selectionCalls += "subtopic:$subtopicId"
            failOnce()
            return questions.filter { it.subtopicId == subtopicId && it.level in levels }
        }

        override suspend fun getTopicById(topicId: String): Topic? =
            topics.firstOrNull { it.id == topicId }

        override suspend fun getSubtopicById(subtopicId: String): Subtopic? =
            subtopics.firstOrNull { it.id == subtopicId }

        override suspend fun getQuestionById(questionId: String): Question? =
            questions.firstOrNull { it.id == questionId }

        private fun failOnce() {
            if (failuresRemaining > 0) {
                failuresRemaining--
                error("Curriculum unavailable")
            }
        }
    }

    private companion object {
        fun question(id: String, level: QuestionLevel): Question =
            Question(
                id = id,
                topicId = "topic_a",
                subtopicId = "subtopic_a",
                text = "$id?",
                answers = listOf(
                    AnswerOption("${id}_a", "A"),
                    AnswerOption("${id}_b", "B"),
                ),
                correctAnswerIds = listOf("${id}_a"),
                selectionMode = AnswerSelectionMode.SINGLE,
                explanation = "Because.",
                level = level,
                sources = emptyList(),
            )
    }
}
