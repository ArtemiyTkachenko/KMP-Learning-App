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
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.topic_study.FakeLearningContentRepository
import org.artkachenko.kmp_learning_app.topic_study.testLearningLesson
import org.artkachenko.kmp_learning_app.topic_study.testLearningUnit

@OptIn(ExperimentalCoroutinesApi::class)
internal class PracticeBuilderViewModelTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun opensOnTheLaunchingTopicWithAStartableDefaultSetup() = runViewModelTest {
        val viewModel =
            viewModel(PracticeBuilderTarget.Topic("topic_a"), FakeCurriculumRepository())

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
        val viewModel =
            viewModel(PracticeBuilderTarget.Subtopic("subtopic_a"), FakeCurriculumRepository())

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
        val viewModel = viewModel(PracticeBuilderTarget.Topic("topic_a"), curriculum)

        advanceUntilIdle()

        assertNull(viewModel.uiState.value.scope.name)
        assertTrue(viewModel.uiState.value.isStartEnabled)
    }

    @Test
    fun editingTheQuestionCountUpdatesStateAndTheStartedConfiguration() = runViewModelTest {
        val viewModel =
            viewModel(PracticeBuilderTarget.Topic("topic_a"), FakeCurriculumRepository())
        advanceUntilIdle()

        viewModel.selectQuestionCount(5)

        assertEquals(5, viewModel.uiState.value.questionCount)
        assertEquals(5, startedConfig(viewModel).questionCount)
    }

    /** The control offers a fixed ladder, which is what keeps the count positive without a guard. */
    @Test
    fun anUnofferedCountIsRejected() = runViewModelTest {
        val viewModel =
            viewModel(PracticeBuilderTarget.Topic("topic_a"), FakeCurriculumRepository())
        advanceUntilIdle()

        viewModel.selectQuestionCount(0)
        viewModel.selectQuestionCount(-3)
        viewModel.selectQuestionCount(999)

        assertEquals(DefaultPracticeQuestionCount, viewModel.uiState.value.questionCount)
    }

    @Test
    fun levelsCanBeDeselectedAndReselected() = runViewModelTest {
        val viewModel =
            viewModel(PracticeBuilderTarget.Topic("topic_a"), FakeCurriculumRepository())
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
        val viewModel =
            viewModel(PracticeBuilderTarget.Topic("topic_a"), FakeCurriculumRepository())
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
        val viewModel =
            viewModel(PracticeBuilderTarget.Topic("topic_a"), FakeCurriculumRepository())
        advanceUntilIdle()

        viewModel.toggleLevel(QuestionLevel.APPLIED)
        advanceUntilIdle()

        assertEquals(
            setOf(QuestionLevel.FOUNDATION, QuestionLevel.ADVANCED),
            startedConfig(viewModel).levels,
        )
    }

    @Test
    fun everySourceIsRepresentedAndSupported() = runViewModelTest {
        val viewModel =
            viewModel(PracticeBuilderTarget.Topic("topic_a"), FakeCurriculumRepository())
        advanceUntilIdle()

        val options = viewModel.uiState.value.sourceOptions
        // Every product source is listed, so the screen shows what targeted practice will offer.
        assertEquals(PracticeQuestionSource.entries, options.map { it.source })
        assertTrue(options.all { it.isAvailable })
    }

    @Test
    fun choosingMistakesRunsPreflightAndStartsWithTheMistakeSource() = runViewModelTest {
        val viewModel = viewModel(
            target = PracticeBuilderTarget.Topic("topic_a"),
            curriculum = FakeCurriculumRepository(),
            completedAttempts = listOf(
                completedHistoryOfAnswers(
                    "q_foundation" to false,
                    "q_applied" to true,
                ),
            ),
        )
        advanceUntilIdle()

        viewModel.selectSource(PracticeQuestionSource.UNRESOLVED_MISTAKES)
        advanceUntilIdle()

        assertEquals(PracticeQuestionSource.UNRESOLVED_MISTAKES, viewModel.uiState.value.source)
        assertEquals(1, availableCount(viewModel))
        assertTrue(viewModel.uiState.value.isStartEnabled)
        assertEquals(
            PracticeQuestionSource.UNRESOLVED_MISTAKES,
            startedConfig(viewModel).source,
        )
    }

    @Test
    fun mistakesWithoutEligibleHistoryStaySelectableButDisableStart() = runViewModelTest {
        val viewModel =
            viewModel(PracticeBuilderTarget.Topic("topic_a"), FakeCurriculumRepository())
        advanceUntilIdle()

        viewModel.selectSource(PracticeQuestionSource.UNRESOLVED_MISTAKES)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PracticeQuestionSource.UNRESOLVED_MISTAKES, state.source)
        assertTrue(
            state.sourceOptions
                .single { it.source == PracticeQuestionSource.UNRESOLVED_MISTAKES }
                .isAvailable,
        )
        assertEquals(PracticeAvailability.NoEligibleQuestions, state.availability)
        assertFalse(state.isStartEnabled)
    }

    @Test
    fun choosingUnseenRechecksAvailabilityAgainstTheUnseenPool() = runViewModelTest {
        // One of the three Questions in scope has already been answered in completed history.
        val viewModel = viewModel(
            target = PracticeBuilderTarget.Topic("topic_a"),
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
            target = PracticeBuilderTarget.Topic("topic_a"),
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
    fun choosingWeakAreasRunsPreflightAndStartsWithTheWeakAreaSource() = runViewModelTest {
        val viewModel = viewModel(
            target = PracticeBuilderTarget.Topic("topic_a"),
            curriculum = FakeCurriculumRepository(),
            completedAttempts = listOf(
                completedHistoryOfAnswers(
                    "q_foundation" to false,
                    "q_applied" to false,
                    "q_advanced" to true,
                ),
            ),
        )
        advanceUntilIdle()

        viewModel.selectSource(PracticeQuestionSource.WEAK_AREAS)
        advanceUntilIdle()

        assertEquals(PracticeQuestionSource.WEAK_AREAS, viewModel.uiState.value.source)
        assertEquals(3, availableCount(viewModel))
        assertTrue(viewModel.uiState.value.isStartEnabled)
        assertEquals(PracticeQuestionSource.WEAK_AREAS, startedConfig(viewModel).source)
    }

    @Test
    fun weakAreasWithoutQualifyingHistoryStaySelectableButDisableStart() = runViewModelTest {
        val viewModel =
            viewModel(PracticeBuilderTarget.Topic("topic_a"), FakeCurriculumRepository())
        advanceUntilIdle()

        viewModel.selectSource(PracticeQuestionSource.WEAK_AREAS)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PracticeQuestionSource.WEAK_AREAS, state.source)
        assertTrue(
            state.sourceOptions.single { it.source == PracticeQuestionSource.WEAK_AREAS }.isAvailable,
        )
        assertEquals(PracticeAvailability.NoEligibleQuestions, state.availability)
        assertFalse(state.isStartEnabled)
    }

    @Test
    fun anEligibleConfigurationEnablesStartAndReportsWhatIsAvailable() = runViewModelTest {
        val viewModel =
            viewModel(PracticeBuilderTarget.Topic("topic_a"), FakeCurriculumRepository())

        advanceUntilIdle()

        val availability =
            assertIs<PracticeAvailability.Available>(viewModel.uiState.value.availability)
        assertEquals(3, availability.eligibleQuestionCount)
        assertTrue(viewModel.uiState.value.isStartEnabled)
    }

    @Test
    fun noEligibleQuestionsDisablesStart() = runViewModelTest {
        val curriculum = FakeCurriculumRepository(questions = emptyList())
        val viewModel = viewModel(PracticeBuilderTarget.Topic("topic_a"), curriculum)

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
        val viewModel = viewModel(PracticeBuilderTarget.Topic("topic_a"), curriculum)
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
        val viewModel = viewModel(PracticeBuilderTarget.Topic("topic_a"), curriculum)
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
        val viewModel = viewModel(PracticeBuilderTarget.Topic("topic_a"), curriculum)

        advanceUntilIdle()

        assertEquals(listOf("topic:topic_a"), curriculum.selectionCalls)
        assertTrue(viewModel.uiState.value.isStartEnabled)
    }

    @Test
    fun startEmitsTheCompleteTypedConfiguration() = runViewModelTest {
        val viewModel =
            viewModel(PracticeBuilderTarget.Subtopic("subtopic_a"), FakeCurriculumRepository())
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
    fun anArrivingPresetSeedsTheSourceAndKeepsEveryOtherDefault() = runViewModelTest {
        val viewModel = viewModel(
            target = PracticeBuilderTarget.Topic("topic_a"),
            curriculum = FakeCurriculumRepository(),
            seenQuestionIds = listOf("q_foundation"),
            initialSource = PracticeQuestionSource.UNSEEN,
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PracticeQuestionSource.UNSEEN, state.source)
        // The preset carries scope and source only: count and levels stay the builder's defaults
        // rather than reconstructing the run the intent was remembered from.
        assertEquals(DefaultPracticeQuestionCount, state.questionCount)
        assertEquals(AllQuestionLevels, state.levels)
        // And it is preflighted against current content, not against a snapshot: one of the three
        // Questions in scope has since been seen.
        assertEquals(2, availableCount(viewModel))
    }

    @Test
    fun anArrivingPresetStartsNothingByItself() = runViewModelTest {
        val viewModel = viewModel(
            target = PracticeBuilderTarget.Subtopic("subtopic_a"),
            curriculum = FakeCurriculumRepository(),
            initialSource = PracticeQuestionSource.WEAK_AREAS,
        )

        val event = async { viewModel.events.first() }
        advanceUntilIdle()

        assertEquals(PracticeQuestionSource.WEAK_AREAS, viewModel.uiState.value.source)
        assertTrue(event.isActive, "Arriving on a preset must not start practice.")
        event.cancel()
    }

    @Test
    fun anArrivingPresetCanStillBeEditedBeforeStarting() = runViewModelTest {
        val viewModel = viewModel(
            target = PracticeBuilderTarget.Topic("topic_a"),
            curriculum = FakeCurriculumRepository(),
            initialSource = PracticeQuestionSource.UNSEEN,
        )
        advanceUntilIdle()

        viewModel.selectSource(PracticeQuestionSource.ALL)
        viewModel.selectQuestionCount(5)
        advanceUntilIdle()

        val config = startedConfig(viewModel)
        assertEquals(PracticeQuestionSource.ALL, config.source)
        assertEquals(5, config.questionCount)
    }

    @Test
    fun startIsIgnoredWhileTheConfigurationCannotRun() = runViewModelTest {
        val curriculum = FakeCurriculumRepository(questions = emptyList())
        val viewModel = viewModel(PracticeBuilderTarget.Topic("topic_a"), curriculum)
        advanceUntilIdle()

        val event = async { viewModel.events.first() }
        viewModel.startPractice()
        advanceUntilIdle()

        assertTrue(event.isActive, "Start must emit nothing while practice cannot run.")
        event.cancel()
    }

    /**
     * The builder's half of Unit practice: the route named a Unit, and what reaches the assessment
     * is the concepts its current Lessons teach. Which concepts those are is
     * [PracticeTargetResolverTest]'s subject; what matters here is that the derived scope — and
     * nothing about the Unit — becomes the configuration.
     */
    @Test
    fun aLearningUnitTargetPractisesTheConceptsItsActiveLessonsTeach() = runViewModelTest {
        val viewModel = viewModel(
            target = PracticeBuilderTarget.LearningUnit("unit_a"),
            curriculum = FakeCurriculumRepository(),
            learningContent = FakeLearningContentRepository(units = listOf(practiceableUnit())),
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PracticeScopeKind.LEARNING_UNIT, state.scope.kind)
        // The title of the Unit the repository just resolved, not a label carried in the route.
        assertEquals("Title of unit_a", state.scope.name)
        assertEquals(
            AssessmentConfig.Focused(
                scope = AssessmentScope.Subtopics(setOf("subtopic_a", "subtopic_b")),
                questionCount = DefaultPracticeQuestionCount,
                levels = AllQuestionLevels,
                source = PracticeQuestionSource.ALL,
            ),
            startedConfig(viewModel),
        )
    }

    /**
     * Unit practice adds no source semantics of its own: each choice reaches the config as the
     * ordinary `PracticeQuestionSource` the selector already implements for a multi-Subtopic scope.
     */
    @Test
    fun everySourceChoiceReachesTheUnitConfigurationUnchanged() = runViewModelTest {
        PracticeQuestionSource.entries.forEach { source ->
            val viewModel = viewModel(
                target = PracticeBuilderTarget.LearningUnit("unit_a"),
                curriculum = FakeCurriculumRepository(),
                // Enough history for every source to have something to draw from: two wrong
                // answers give weak areas and unresolved mistakes, and the untouched third
                // Question keeps unseen non-empty. Which Questions each policy picks is the
                // selector's own subject; this asserts only that the choice reaches the config.
                completedAttempts = listOf(
                    completedHistoryOfAnswers(
                        "q_foundation" to false,
                        "q_applied" to false,
                    ),
                ),
                learningContent = FakeLearningContentRepository(units = listOf(practiceableUnit())),
            )
            advanceUntilIdle()

            viewModel.selectSource(source)
            advanceUntilIdle()

            val config = startedConfig(viewModel)
            assertEquals(source, config.source, "Source ${'$'}source must survive to the config.")
            assertEquals(
                AssessmentScope.Subtopics(setOf("subtopic_a", "subtopic_b")),
                config.scope,
            )
        }
    }

    /** A stale route naming a Unit that no longer resolves fails safely and starts nothing. */
    @Test
    fun aUnitThatNoLongerResolvesIsReportedRatherThanPractised() = runViewModelTest {
        val viewModel = viewModel(
            target = PracticeBuilderTarget.LearningUnit("unit_gone"),
            curriculum = FakeCurriculumRepository(),
            learningContent = FakeLearningContentRepository(units = listOf(practiceableUnit())),
        )

        val event = async { viewModel.events.first() }
        advanceUntilIdle()
        viewModel.startPractice()
        advanceUntilIdle()

        assertEquals(PracticeAvailability.TargetUnavailable, viewModel.uiState.value.availability)
        assertFalse(viewModel.uiState.value.isStartEnabled)
        assertTrue(event.isActive, "An unresolvable Unit must not start an assessment.")
        event.cancel()
    }

    /** Retired study material must not become practiceable through a route that still names it. */
    @Test
    fun aDeprecatedUnitIsNotPractisedThroughAStaleRoute() = runViewModelTest {
        val deprecated = testLearningUnit(
            id = "unit_a",
            status = ContentStatus.DEPRECATED,
            lessons = listOf(
                testLearningLesson("lesson_a", primarySubtopicIds = listOf("subtopic_a")),
            ),
        )
        val viewModel = viewModel(
            target = PracticeBuilderTarget.LearningUnit("unit_a"),
            curriculum = FakeCurriculumRepository(),
            learningContent = FakeLearningContentRepository(units = listOf(deprecated)),
        )

        advanceUntilIdle()

        assertEquals(PracticeAvailability.TargetUnavailable, viewModel.uiState.value.availability)
    }

    /**
     * An empty derived scope is refused before `AssessmentScope.Subtopics` is constructed, so a
     * malformed Unit reads as unavailable rather than failing a domain precondition on screen.
     */
    @Test
    fun aUnitWithNoActivePrimaryConceptsIsReportedRatherThanRun() = runViewModelTest {
        val emptyUnit = testLearningUnit(
            id = "unit_a",
            lessons = listOf(
                testLearningLesson(
                    id = "lesson_supporting_only",
                    supportingSubtopicIds = listOf("subtopic_a"),
                ),
                testLearningLesson(
                    id = "lesson_retired",
                    status = ContentStatus.DEPRECATED,
                    primarySubtopicIds = listOf("subtopic_a"),
                ),
            ),
        )
        val viewModel = viewModel(
            target = PracticeBuilderTarget.LearningUnit("unit_a"),
            curriculum = FakeCurriculumRepository(),
            learningContent = FakeLearningContentRepository(units = listOf(emptyUnit)),
        )

        val event = async { viewModel.events.first() }
        advanceUntilIdle()
        viewModel.startPractice()
        advanceUntilIdle()

        assertEquals(
            PracticeAvailability.NoPracticeableConcepts,
            viewModel.uiState.value.availability,
        )
        assertFalse(viewModel.uiState.value.isStartEnabled)
        assertTrue(event.isActive, "A Unit with nothing to assess must not start an assessment.")
        event.cancel()
    }

    /**
     * An unreadable document is a failure rather than an answer, so it stays retryable — and the
     * retry has to re-resolve the Unit, because there is no scope yet to re-preflight.
     */
    @Test
    fun anUnreadableLearningDocumentIsRetryable() = runViewModelTest {
        val viewModel = viewModel(
            target = PracticeBuilderTarget.LearningUnit("unit_a"),
            curriculum = FakeCurriculumRepository(),
            learningContent = FakeLearningContentRepository(
                units = listOf(practiceableUnit()),
                failuresRemaining = 1,
            ),
        )
        advanceUntilIdle()
        assertEquals(PracticeAvailability.Error, viewModel.uiState.value.availability)

        viewModel.retryAvailability()
        advanceUntilIdle()

        assertEquals("Title of unit_a", viewModel.uiState.value.scope.name)
        assertTrue(viewModel.uiState.value.isStartEnabled)
    }

    /**
     * One Unit whose ACTIVE Lessons name `subtopic_a` twice and `subtopic_b` once, plus supporting
     * and deprecated concepts that must not reach practice.
     */
    private fun practiceableUnit() = testLearningUnit(
        id = "unit_a",
        lessons = listOf(
            testLearningLesson(
                id = "lesson_a",
                primarySubtopicIds = listOf("subtopic_a"),
                supportingSubtopicIds = listOf("subtopic_supporting"),
            ),
            testLearningLesson(
                id = "lesson_b",
                primarySubtopicIds = listOf("subtopic_a", "subtopic_b"),
            ),
            testLearningLesson(
                id = "lesson_retired",
                status = ContentStatus.DEPRECATED,
                primarySubtopicIds = listOf("subtopic_retired"),
            ),
        ),
    )

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
     * History is only input data here: which Questions qualify for a source is the selector's
     * answer, and proving its derivations belongs to the selector's own tests rather than here.
     */
    private fun viewModel(
        target: PracticeBuilderTarget,
        curriculum: CurriculumRepository,
        seenQuestionIds: List<String> = emptyList(),
        completedAttempts: List<TestAttempt>? = null,
        initialSource: PracticeQuestionSource = PracticeQuestionSource.ALL,
        // Fails on every read by default. A Topic or Subtopic target must resolve without touching
        // learning content at all, so a suite that quietly started depending on it would turn every
        // one of these cases into a resolution error instead of passing silently.
        learningContent: FakeLearningContentRepository =
            FakeLearningContentRepository(failuresRemaining = Int.MAX_VALUE),
    ): PracticeBuilderViewModel =
        PracticeBuilderViewModel(
            target = target,
            targetResolver = PracticeTargetResolver(
                curriculumRepository = curriculum,
                learningContentRepository = learningContent,
            ),
            questionSelector = AssessmentQuestionSelector(
                curriculumRepository = curriculum,
                completedHistory = {
                    completedAttempts ?: completedHistoryOf(seenQuestionIds)
                },
                randomize = { it },
            ),
            initialSource = initialSource,
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

    private fun completedHistoryOfAnswers(
        vararg answers: Pair<String, Boolean>,
    ): TestAttempt =
        TestAttempt(
            id = "completed_attempt",
            config = AssessmentConfig.Mixed(questionCount = answers.size),
            questionAttempts = answers.map { (questionId, isCorrect) ->
                QuestionAttempt(
                    questionId = questionId,
                    answerState = QuestionAnswerState.Answered(
                        selectedAnswerIds = setOf("${questionId}_a"),
                        isCorrect = isCorrect,
                    ),
                )
            },
            status = AssessmentStatus.COMPLETED,
            startedAt = Instant.fromEpochSeconds(0),
            completedAt = Instant.fromEpochSeconds(60),
            score = AssessmentScore(
                totalQuestions = answers.size,
                correctAnswers = answers.count { it.second },
            ),
        )

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
