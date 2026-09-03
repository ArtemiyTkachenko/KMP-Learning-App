package org.artkachenko.kmp_learning_app.topic_study.topics

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.guided_learning.ContinueStudyingContext
import org.artkachenko.kmp_learning_app.guided_learning.ContinueStudyingResolver
import org.artkachenko.kmp_learning_app.guided_learning.ContinueStudyingTarget
import org.artkachenko.kmp_learning_app.guided_learning.LearningRecommendationRationale
import org.artkachenko.kmp_learning_app.guided_learning.LearningRecommendationResolver
import org.artkachenko.kmp_learning_app.guided_learning.LearningRecommendationTarget
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset
import org.artkachenko.kmp_learning_app.guided_learning.UnresolvedMistakeCounter
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewService
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader

@OptIn(ExperimentalCoroutinesApi::class)
internal class TopicBrowserViewModelTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsTopicAndSubtopicCatalogInRepositoryOrder() = runViewModelTest {
        val topics = listOf(
            Topic("topic_b", "Topic B"),
            Topic("topic_a", "Topic A"),
        )
        val repository = FakeCurriculumRepository(
            topicResults = resultsOf(topics),
            subtopicResults = mutableMapOf(
                "topic_b" to resultsOf(
                    listOf(
                        Subtopic("subtopic_b2", "topic_b", "B Two"),
                        Subtopic("subtopic_b1", "topic_b", "B One"),
                    ),
                ),
                "topic_a" to resultsOf(
                    listOf(Subtopic("subtopic_a", "topic_a", "A One")),
                ),
            ),
        )
        val viewModel = viewModel(repository)

        assertEquals(TopicBrowserUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertEquals(
            listOf("topic_b", "topic_a"),
            state.topics.map(TopicBrowserItemUiModel::topicId),
        )
        assertEquals(
            listOf("Topic B", "Topic A"),
            state.topics.map(TopicBrowserItemUiModel::topicName),
        )
        assertEquals(
            listOf("subtopic_b2", "subtopic_b1", "subtopic_a"),
            state.searchableSubtopics.map(SubtopicSearchResult::subtopicId),
        )
        assertEquals(listOf("topic_b", "topic_a"), repository.subtopicReadTopicIds)

        viewModel.onSearchQueryChange("b")
        val search = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertEquals(
            listOf("subtopic_b2", "subtopic_b1"),
            search.subtopicMatches.map(SubtopicSearchResult::subtopicId),
        )
    }

    @Test
    fun topicMatchingIsCaseInsensitiveAndPreservesTopicOrder() = runViewModelTest {
        val viewModel = loadedViewModel()

        listOf("compose", "COMPOSE", "Compose").forEach { query ->
            viewModel.onSearchQueryChange(query)
            val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
            assertEquals(
                listOf("compose", "compose_architecture"),
                state.topicMatches.map(TopicBrowserItemUiModel::topicId),
            )
        }
    }

    @Test
    fun subtopicMatchIncludesParentTopicContext() = runViewModelTest {
        val viewModel = loadedViewModel()

        viewModel.onSearchQueryChange("viewmodel")

        val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertTrue(state.topicMatches.isEmpty())
        assertEquals(
            listOf(
                SubtopicSearchResult(
                    subtopicId = "viewmodel_lifecycle",
                    subtopicName = "ViewModel lifecycle",
                    parentTopicId = "architecture",
                    parentTopicName = "Lifecycle, State & Navigation",
                ),
            ),
            state.subtopicMatches,
        )
    }

    @Test
    fun oneQueryCanReturnTopicAndSubtopicGroups() = runViewModelTest {
        val viewModel = loadedViewModel()

        viewModel.onSearchQueryChange("compose")

        val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertEquals(2, state.topicMatches.size)
        assertEquals(
            listOf("compose_runtime"),
            state.subtopicMatches.map(SubtopicSearchResult::subtopicId),
        )
    }

    @Test
    fun multiWordMatchingTrimsSplitsAndRequiresEveryToken() = runViewModelTest {
        val viewModel = loadedViewModel()

        viewModel.onSearchQueryChange("  VIEW   model  ")

        val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertEquals(
            listOf("viewmodel_lifecycle"),
            state.subtopicMatches.map(SubtopicSearchResult::subtopicId),
        )
    }

    @Test
    fun learningContextIsNeverSearchableText() = runViewModelTest {
        // Coverage, accuracy, and weakness are display metadata. Matching still reads Topic and
        // Subtopic names only, so none of these queries can find curriculum.
        val viewModel = loadedViewModel(history = historyRepository(answer("q_compose", false)))

        listOf("weak", "explored", "0%", "100%").forEach { query ->
            viewModel.onSearchQueryChange(query)
            val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
            assertTrue(state.topicMatches.isEmpty(), "\"$query\" should match no topic")
            assertTrue(state.subtopicMatches.isEmpty(), "\"$query\" should match no subtopic")
        }
    }

    @Test
    fun blankAndClearedQueriesRestoreNormalBrowsingWithoutExpandingSubtopics() = runViewModelTest {
        val viewModel = loadedViewModel()
        val originalTopics = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value).topics

        viewModel.onSearchQueryChange("compose")
        assertTrue(
            assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value).topicMatches.isNotEmpty(),
        )

        viewModel.onSearchQueryChange("   ")
        val whitespace = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertEquals(originalTopics, whitespace.topics)
        assertTrue(whitespace.topicMatches.isEmpty())
        assertTrue(whitespace.subtopicMatches.isEmpty())

        viewModel.onSearchQueryChange("")
        val cleared = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertEquals(originalTopics, cleared.topics)
        assertTrue(cleared.topicMatches.isEmpty())
        assertTrue(cleared.subtopicMatches.isEmpty())
    }

    @Test
    fun nonBlankQueryCanProduceExplicitlyEmptyMatches() = runViewModelTest {
        val viewModel = loadedViewModel()

        viewModel.onSearchQueryChange("not in this curriculum")

        val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertEquals("not in this curriculum", state.query)
        assertTrue(state.topicMatches.isEmpty())
        assertTrue(state.subtopicMatches.isEmpty())
    }

    @Test
    fun typingFiltersLoadedCatalogWithoutRepositoryOrQuestionReads() = runViewModelTest {
        val repository = catalogRepository()
        val history = historyRepository()
        val viewModel = viewModel(repository, history)
        advanceUntilIdle()
        // One ACTIVE question read for the whole coverage derivation, and one history read behind
        // the shared cache. Neither is per Topic card.
        val readsAfterLoad = repository.questionReadCount
        assertEquals(1, readsAfterLoad)
        assertEquals(1, history.readCount)

        viewModel.onSearchQueryChange("c")
        viewModel.onSearchQueryChange("co")
        viewModel.onSearchQueryChange("compose")
        viewModel.onSearchQueryChange("")
        advanceUntilIdle()

        assertEquals(1, repository.topicReadCount)
        assertEquals(3, repository.subtopicReadTopicIds.size)
        assertEquals(readsAfterLoad, repository.questionReadCount)
        assertEquals(1, history.readCount)
    }

    @Test
    fun emptyTopicCatalogDoesNotLoadSubtopics() = runViewModelTest {
        val repository = FakeCurriculumRepository(topicResults = resultsOf(emptyList()))
        val viewModel = viewModel(repository)

        advanceUntilIdle()

        assertEquals(TopicBrowserUiState.Empty, viewModel.uiState.value)
        assertTrue(repository.subtopicReadTopicIds.isEmpty())
    }

    @Test
    fun anyCatalogLoadFailureBecomesErrorAndRetryCanSucceed() = runViewModelTest {
        val topic = Topic("topic_a", "Topic A")
        val repository = FakeCurriculumRepository(
            topicResults = resultsOf(listOf(topic), listOf(topic)),
            subtopicResults = mutableMapOf(
                topic.id to ArrayDeque(
                    listOf(
                        Result.failure(IllegalStateException("subtopics unavailable")),
                        Result.success(listOf(Subtopic("subtopic_a", topic.id, "Subtopic A"))),
                    ),
                ),
            ),
        )
        val viewModel = viewModel(repository)

        advanceUntilIdle()
        assertEquals(TopicBrowserUiState.Error, viewModel.uiState.value)

        viewModel.retry()
        assertEquals(TopicBrowserUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertEquals(listOf("subtopic_a"), state.searchableSubtopics.map { it.subtopicId })
    }

    @Test
    fun loadedHistoryGivesEachTopicItsCoverageAndAccuracy() = runViewModelTest {
        // compose holds four ACTIVE questions; two distinct ones were attempted, one of them again
        // in a retake, so accuracy counts three occurrences while coverage counts two unique
        // questions.
        val history = historyRepository(
            listOf(
                completedAttempt(
                    "attempt_1",
                    answer("q_compose_1", true),
                    answer("q_compose_2", false),
                ),
                completedAttempt("attempt_2", answer("q_compose_1", true)),
            ),
        )
        val viewModel = loadedViewModel(history = history)

        val context = assertNotNull(topic(viewModel, "compose").learningContext)
        assertEquals(2, context.attemptedQuestionCount)
        assertEquals(4, context.totalQuestionCount)
        assertEquals(50.0, context.coveragePercentage)
        assertEquals(2.0 / 3.0 * 100.0, context.accuracyPercentage)
        assertFalse(context.isUnstudied)
    }

    @Test
    fun anUnseenTopicReportsCoverageWithoutFabricatingAccuracy() = runViewModelTest {
        val viewModel = loadedViewModel(history = historyRepository(answer("q_compose_1", true)))

        val context = assertNotNull(topic(viewModel, "architecture").learningContext)
        assertEquals(0, context.attemptedQuestionCount)
        assertEquals(2, context.totalQuestionCount)
        // Null, not 0.0: never answered and answered badly are different things to report.
        assertNull(context.accuracyPercentage)
        assertFalse(context.isWeak)
        assertTrue(context.isUnstudied)
    }

    @Test
    fun historicalAccuracySurvivesZeroCurrentCoverage() = runViewModelTest {
        // compose_architecture's questions were all retired, so its accuracy has no current
        // coverage to sit beside. That is not the same as never having been studied.
        val viewModel = loadedViewModel(
            history = historyRepository(
                answer("q_retired_1", true),
                answer("q_retired_2", false),
                answer("q_retired_3", true),
            ),
        )

        val context = assertNotNull(topic(viewModel, "compose_architecture").learningContext)
        assertEquals(0, context.attemptedQuestionCount)
        assertEquals(1, context.totalQuestionCount)
        assertEquals(2.0 / 3.0 * 100.0, context.accuracyPercentage)
        assertFalse(context.isUnstudied)
    }

    @Test
    fun weakStatusIsCopiedFromTheDomainRatherThanInferredFromThePercentage() = runViewModelTest {
        // compose: three answers at 33%, past the policy's evidence threshold, so it is weak.
        // architecture: one answer at 0%, just as low but too little evidence to be called weak.
        val viewModel = loadedViewModel(
            history = historyRepository(
                answer("q_compose_1", true),
                answer("q_compose_2", false),
                answer("q_compose_3", false),
                answer("q_architecture_1", false),
            ),
        )

        val weak = assertNotNull(topic(viewModel, "compose").learningContext)
        assertTrue(weak.isWeak)

        val sparse = assertNotNull(topic(viewModel, "architecture").learningContext)
        assertEquals(0.0, sparse.accuracyPercentage)
        assertFalse(sparse.isWeak)
        // A real 0% from a real answer is not an unstudied Topic.
        assertFalse(sparse.isUnstudied)
    }

    @Test
    fun topicsStayBrowsableWhileHistoryHasNotLoaded() = runViewModelTest {
        val repository = catalogRepository()
        // Never completes, so the shared cache stays on its Loading value.
        val viewModel = viewModel(repository, NeverReturningHistoryRepository)
        advanceUntilIdle()

        val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertEquals(3, state.topics.size)
        // No context at all, rather than an empty one: unknown history must not render as
        // "not studied yet", which is a claim about the learner.
        assertTrue(state.topics.all { it.learningContext == null })
        assertNull(state.continueStudying)
        // Search still works against the loaded catalog.
        viewModel.onSearchQueryChange("compose")
        assertEquals(
            2,
            assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value).topicMatches.size,
        )
    }

    @Test
    fun aHistoryFailureLeavesTheCatalogIntactAndInventsNoUnseenState() = runViewModelTest {
        val repository = catalogRepository()
        val viewModel = viewModel(repository, FailingHistoryRepository)
        advanceUntilIdle()

        val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        // An optional statistic must not take down curriculum browsing.
        assertEquals(3, state.topics.size)
        assertTrue(state.topics.all { it.learningContext == null })
    }

    @Test
    fun completedHistoryInvalidationRefreshesLearningContextInPlace() = runViewModelTest {
        val repository = catalogRepository()
        val history = MutableHistoryRepository()
        val store = AssessmentHistoryStore(history, CoroutineScope(currentDispatcher()))
        val viewModel = TopicBrowserViewModel(
            curriculumRepository = repository,
            learningProgressService = LearningProgressService(history, repository),
            historyStore = store,
            continueStudyingResolver = ContinueStudyingResolver(repository),
            learningRecommendationResolver = recommendationResolver(repository, history),
        )
        advanceUntilIdle()

        assertTrue(assertNotNull(topic(viewModel, "compose").learningContext).isUnstudied)

        // A learner keeps their search while a completed assessment lands underneath it.
        viewModel.onSearchQueryChange("compose")
        history.attempts = listOf(completedAttempt("attempt", answer("q_compose_1", true)))
        store.invalidate()
        advanceUntilIdle()

        val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        // The query, the search mode, and the matching are all untouched by the refresh.
        assertEquals("compose", state.query)
        assertEquals(
            listOf("compose", "compose_architecture"),
            state.topicMatches.map(TopicBrowserItemUiModel::topicId),
        )
        // And the rebuilt rows carry the new context, without a restart or a manual retry.
        val refreshed = assertNotNull(state.topicMatches.first().learningContext)
        assertEquals(1, refreshed.attemptedQuestionCount)
        assertEquals(100.0, refreshed.accuracyPercentage)
        assertEquals(refreshed, topic(viewModel, "compose").learningContext)
    }

    @Test
    fun emptyCompletedHistoryOffersNoContinueShortcut() = runViewModelTest {
        val viewModel = viewModel(continueStudyingRepository(), historyRepository())
        advanceUntilIdle()

        val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertEquals(3, state.topics.size)
        assertNull(state.continueStudying)
    }

    @Test
    fun aCompletedFocusedRunAddsAContinueShortcutNamedByCurrentCurriculum() = runViewModelTest {
        val viewModel = viewModel(
            continueStudyingRepository(),
            historyRepository(listOf(completedFocusedAttempt("attempt", "compose"))),
        )
        advanceUntilIdle()

        val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        val context = assertNotNull(state.continueStudying)
        assertEquals(ContinueStudyingTarget.Topic("compose"), context.target)
        assertEquals("Compose UI", context.scopeName)
        // Catalogue behaviour is untouched by the addition.
        assertEquals(
            listOf("compose", "compose_architecture", "architecture"),
            state.topics.map(TopicBrowserItemUiModel::topicId),
        )
    }

    @Test
    fun anUnreadableHistoryLeavesTopicsBrowsableWithNoContinueShortcut() = runViewModelTest {
        val viewModel = viewModel(continueStudyingRepository(), FailingHistoryRepository)
        advanceUntilIdle()

        val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertEquals(3, state.topics.size)
        // Unknown history is not empty history, and neither may be presented as a study context.
        assertNull(state.continueStudying)
    }

    @Test
    fun aFailedContextResolutionLeavesTopicsBrowsableWithNoContinueShortcut() = runViewModelTest {
        val repository = continueStudyingRepository(
            identityFailure = IllegalStateException("curriculum identity unavailable"),
        )
        val viewModel = viewModel(
            repository,
            historyRepository(listOf(completedFocusedAttempt("attempt", "compose"))),
        )
        advanceUntilIdle()

        // Continue Studying is enrichment: its failure must not become the screen's Error state.
        val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertEquals(3, state.topics.size)
        assertNull(state.continueStudying)
    }

    @Test
    fun theContinueShortcutIsNotOfferedAsASearchResult() = runViewModelTest {
        val viewModel = viewModel(
            continueStudyingRepository(),
            historyRepository(listOf(completedFocusedAttempt("attempt", "compose"))),
        )
        advanceUntilIdle()
        assertNotNull(
            assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value).continueStudying,
        )

        viewModel.onSearchQueryChange("compose")

        val search = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        // Searching says what the learner is looking for; the shortcut is not it, and never
        // matches the query text either way.
        assertNull(search.continueStudying)
        assertEquals(
            listOf("compose", "compose_architecture"),
            search.topicMatches.map(TopicBrowserItemUiModel::topicId),
        )

        viewModel.onSearchQueryChange("")
        assertNotNull(
            assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value).continueStudying,
        )
    }

    @Test
    fun completingANewerFocusedRunMovesTheContinueShortcutToIt() = runViewModelTest {
        val repository = continueStudyingRepository()
        val history = MutableHistoryRepository()
        history.attempts = listOf(completedFocusedAttempt("older", "compose"))
        val store = AssessmentHistoryStore(history, CoroutineScope(currentDispatcher()))
        val viewModel = TopicBrowserViewModel(
            curriculumRepository = repository,
            learningProgressService = LearningProgressService(history, repository),
            historyStore = store,
            continueStudyingResolver = ContinueStudyingResolver(repository),
            learningRecommendationResolver = recommendationResolver(repository, history),
        )
        advanceUntilIdle()

        assertEquals(
            ContinueStudyingTarget.Topic("compose"),
            assertNotNull(continueStudying(viewModel)).target,
        )

        // The normal completion path: newest first, then the shared invalidation every other
        // consumer already follows. No "last studied" value is stored anywhere.
        history.attempts = listOf(
            completedFocusedAttempt("newer", "architecture"),
            completedFocusedAttempt("older", "compose"),
        )
        store.invalidate()
        advanceUntilIdle()

        val refreshed = assertNotNull(continueStudying(viewModel))
        assertEquals(ContinueStudyingTarget.Topic("architecture"), refreshed.target)
        assertEquals("Lifecycle, State & Navigation", refreshed.scopeName)
    }

    @Test
    fun aLoadedEmptyHistoryReceivesTheDeterministicStartingRecommendation() = runViewModelTest {
        // Loaded and empty is a real statement about the learner: they have completed nothing.
        val viewModel = loadedViewModel()

        val recommendation = assertNotNull(recommendedNext(viewModel))
        // Browse Topics, and no Topic chosen for them: the list is immediately below the card.
        assertEquals(LearningRecommendationTarget.Topics, recommendation.target)
        assertEquals(LearningRecommendationRationale.NewUser, recommendation.rationale)
        assertNull(recommendation.topicName)
    }

    @Test
    fun historyThatHasNotLoadedIsNotMistakenForANewLearner() = runViewModelTest {
        val viewModel = viewModel(catalogRepository(), NeverReturningHistoryRepository)
        advanceUntilIdle()

        // Unknown history justifies nothing. Reading it as "no completed attempts" would greet a
        // returning learner as a beginner on every slow start.
        assertNull(recommendedNext(viewModel))
    }

    @Test
    fun unreadableHistoryIsNotMistakenForANewLearner() = runViewModelTest {
        val viewModel = viewModel(catalogRepository(), FailingHistoryRepository)
        advanceUntilIdle()

        assertNull(recommendedNext(viewModel))
        // And the catalogue is untouched by the failure.
        assertEquals(
            3,
            assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value).topics.size,
        )
    }

    @Test
    fun unresolvedMistakesAreRecommendedWithTheirExactCount() = runViewModelTest {
        val viewModel = loadedViewModel(
            history = historyRepository(
                answer("q_compose_1", false),
                answer("q_compose_2", false),
            ),
        )

        val recommendation = assertNotNull(recommendedNext(viewModel))
        // The existing Mistake Review capability, not targeted mistake practice.
        assertEquals(LearningRecommendationTarget.MistakeReview, recommendation.target)
        assertEquals(
            LearningRecommendationRationale.UnresolvedMistakes(count = 2),
            recommendation.rationale,
        )
    }

    @Test
    fun aWeakAreaIsRecommendedAsAnEditableWeakAreaPreset() = runViewModelTest {
        // Every Question was answered wrongly once and correctly since, so nothing is unresolved
        // while the all-time accuracy is still 50% over six occurrences — weak by the policy.
        val viewModel = loadedViewModel(
            repository = namedCatalogRepository(),
            history = historyRepository(
                listOf(
                    completedAttempt(
                        "newer",
                        answer("q_compose_1", true),
                        answer("q_compose_2", true),
                        answer("q_compose_3", true),
                    ),
                    completedAttempt(
                        "older",
                        answer("q_compose_1", false),
                        answer("q_compose_2", false),
                        answer("q_compose_3", false),
                    ),
                ),
            ),
        )

        val recommendation = assertNotNull(recommendedNext(viewModel))
        assertEquals(
            LearningRecommendationTarget.Practice(
                PracticePreset(
                    scope = AssessmentScope.Subtopic("compose_runtime"),
                    source = PracticeQuestionSource.WEAK_AREAS,
                ),
            ),
            recommendation.target,
        )
        assertEquals(
            LearningRecommendationRationale.WeakArea(
                scope = AssessmentScope.Subtopic("compose_runtime"),
                areaName = "Compose runtime",
            ),
            recommendation.rationale,
        )
    }

    @Test
    fun remainingUnseenContentIsRecommendedWithItsCurrentTopicName() = runViewModelTest {
        // One correct answer in compose, so nothing is unresolved and nothing is weak. What is
        // left is curriculum the learner has never seen.
        val viewModel = loadedViewModel(history = historyRepository(answer("q_compose_1", true)))

        val recommendation = assertNotNull(recommendedNext(viewModel))
        assertEquals(
            LearningRecommendationTarget.Practice(
                PracticePreset(
                    scope = AssessmentScope.Topic("architecture"),
                    source = PracticeQuestionSource.UNSEEN,
                ),
            ),
            recommendation.target,
        )
        assertEquals(
            LearningRecommendationRationale.UnseenCoverage(
                topicId = "architecture",
                unseenQuestionCount = 2,
            ),
            recommendation.rationale,
        )
        // Resolved from the loaded catalogue by stable ID, never from anything persisted.
        assertEquals("Lifecycle, State & Navigation", recommendation.topicName)
    }

    @Test
    fun nothingUsefulLeftToDoRendersNoRecommendation() = runViewModelTest {
        val viewModel = loadedViewModel(
            history = historyRepository(
                answer("q_compose_1", true),
                answer("q_compose_2", true),
                answer("q_compose_3", true),
                answer("q_compose_4", true),
                answer("q_compose_arch_1", true),
                answer("q_architecture_1", true),
                answer("q_architecture_2", true),
            ),
        )

        // No mistakes, no weak area, nothing unseen. Manufacturing an action here would be the
        // hidden policy this feature exists to avoid.
        assertNull(recommendedNext(viewModel))
        // Topics remain exactly as browsable as before.
        assertEquals(
            3,
            assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value).topics.size,
        )
    }

    @Test
    fun oneHistoryEmissionDerivesLearningProgressOnce() = runViewModelTest {
        val repository = catalogRepository()
        val history = historyRepository(answer("q_compose_1", true))
        val viewModel = viewModel(repository, history)
        advanceUntilIdle()

        val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertNotNull(state.topics.first().learningContext)
        assertNotNull(state.recommendedNext)
        // Topic rows and the recommendation share one LearningProgressSnapshot. The derivation
        // reads the ACTIVE bank exactly once, so a second load would show as a second read — and
        // the two surfaces could then describe the same history differently.
        assertEquals(1, repository.questionReadCount)
        assertEquals(1, history.readCount)
    }

    @Test
    fun aFailedRecommendationLeavesTheCatalogueAndTheContinueShortcutIntact() = runViewModelTest {
        val repository = continueStudyingRepository()
        val viewModel = viewModel(
            repository = repository,
            history = historyRepository(listOf(completedFocusedAttempt("attempt", "compose"))),
            learningRecommendationResolver = LearningRecommendationResolver {
                error("Recommendation unavailable")
            },
        )
        advanceUntilIdle()

        val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        // Optional enrichment, so its failure costs only itself: not the catalogue, not the
        // learning context, and not the other guided surface.
        assertNull(state.recommendedNext)
        assertEquals(3, state.topics.size)
        assertNotNull(state.topics.first().learningContext)
        assertEquals(ContinueStudyingTarget.Topic("compose"), assertNotNull(state.continueStudying).target)
    }

    @Test
    fun aFailedContinueShortcutLeavesAValidRecommendationInPlace() = runViewModelTest {
        val viewModel = viewModel(
            repository = catalogRepository(),
            history = historyRepository(listOf(completedFocusedAttempt("attempt", "compose"))),
            // Resolving the continue context fails on its own curriculum lookup, while the
            // recommendation's inputs — progress and mistakes — are unaffected.
            continueStudyingResolver = ContinueStudyingResolver(
                continueStudyingRepository(
                    identityFailure = IllegalStateException("curriculum identity unavailable"),
                ),
            ),
        )
        advanceUntilIdle()

        val state = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        assertNull(state.continueStudying)
        // The newest completed context is a Topic-scoped run, which breaks the otherwise tied
        // coverage decision towards that same Topic.
        assertEquals(
            LearningRecommendationRationale.UnseenCoverage(
                topicId = "compose",
                unseenQuestionCount = 4,
            ),
            assertNotNull(state.recommendedNext).rationale,
        )
    }

    @Test
    fun completedHistoryInvalidationMovesTheRecommendationToTheNewState() = runViewModelTest {
        val repository = catalogRepository()
        val history = MutableHistoryRepository()
        val store = AssessmentHistoryStore(history, CoroutineScope(currentDispatcher()))
        val viewModel = TopicBrowserViewModel(
            curriculumRepository = repository,
            learningProgressService = LearningProgressService(history, repository),
            historyStore = store,
            continueStudyingResolver = ContinueStudyingResolver(repository),
            learningRecommendationResolver = recommendationResolver(repository, history),
        )
        advanceUntilIdle()

        assertEquals(
            LearningRecommendationRationale.NewUser,
            assertNotNull(recommendedNext(viewModel)).rationale,
        )

        // The normal completion path, with nothing cached, persisted, or manually refreshed: the
        // current facts alone decide the current recommendation.
        history.attempts = listOf(completedAttempt("attempt", answer("q_compose_1", false)))
        store.invalidate()
        advanceUntilIdle()

        val refreshed = assertNotNull(recommendedNext(viewModel))
        assertEquals(LearningRecommendationTarget.MistakeReview, refreshed.target)
        assertEquals(
            LearningRecommendationRationale.UnresolvedMistakes(count = 1),
            refreshed.rationale,
        )
    }

    @Test
    fun theRecommendationIsNotOfferedAsASearchResult() = runViewModelTest {
        val viewModel = loadedViewModel(history = historyRepository(answer("q_compose_1", true)))
        assertNotNull(recommendedNext(viewModel))

        viewModel.onSearchQueryChange("compose")

        val search = assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
        // A learner who has started typing has already said what they are looking for, and the
        // rationale is not searchable text either way.
        assertNull(search.recommendedNext)
        assertEquals(
            listOf("compose", "compose_architecture"),
            search.topicMatches.map(TopicBrowserItemUiModel::topicId),
        )

        viewModel.onSearchQueryChange("")
        assertNotNull(recommendedNext(viewModel))
    }

    private fun recommendedNext(viewModel: TopicBrowserViewModel): RecommendedNextUiModel? =
        assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value).recommendedNext

    private fun continueStudying(viewModel: TopicBrowserViewModel): ContinueStudyingContext? =
        assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value).continueStudying

    private fun topic(
        viewModel: TopicBrowserViewModel,
        topicId: String,
    ): TopicBrowserItemUiModel =
        assertIs<TopicBrowserUiState.Content>(viewModel.uiState.value)
            .topics
            .first { it.topicId == topicId }

    private suspend fun TestScope.loadedViewModel(
        history: AssessmentRepository = historyRepository(),
        repository: CurriculumRepository = catalogRepository(),
    ): TopicBrowserViewModel = viewModel(repository, history).also { advanceUntilIdle() }

    private fun TestScope.viewModel(
        repository: CurriculumRepository,
        history: AssessmentRepository = historyRepository(),
        continueStudyingResolver: ContinueStudyingResolver = ContinueStudyingResolver(repository),
        learningRecommendationResolver: LearningRecommendationResolver =
            recommendationResolver(repository, history),
    ): TopicBrowserViewModel =
        TopicBrowserViewModel(
            curriculumRepository = repository,
            learningProgressService = LearningProgressService(history, repository),
            historyStore = AssessmentHistoryStore(history, CoroutineScope(currentDispatcher())),
            continueStudyingResolver = continueStudyingResolver,
            learningRecommendationResolver = learningRecommendationResolver,
        )

    /**
     * The production wiring: the real policy behind the real mistake semantics, counting from the
     * same history emission the ViewModel already holds.
     */
    private fun recommendationResolver(
        repository: CurriculumRepository,
        history: AssessmentRepository,
    ): LearningRecommendationResolver {
        val mistakeReviewService = MistakeReviewService(
            assessmentRepository = history,
            assessmentReviewLoader = AssessmentReviewLoader(repository),
        )
        return LearningRecommendationResolver { completedAttempts ->
            mistakeReviewService.countUnresolved(completedAttempts)
        }
    }

    private fun TestScope.currentDispatcher() = StandardTestDispatcher(testScheduler)

    private fun runViewModelTest(
        block: suspend TestScope.() -> Unit,
    ) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }

    private class FakeCurriculumRepository(
        private val topicResults: ArrayDeque<Result<List<Topic>>>,
        private val subtopicResults: MutableMap<String, ArrayDeque<Result<List<Subtopic>>>> =
            mutableMapOf(),
        private val questions: List<Question> = emptyList(),
        /**
         * Identity lookups, which Continue Studying resolves its historical scope IDs against.
         * Empty by default so tests that are only about the catalogue keep the previous behaviour
         * of resolving no metadata at all.
         */
        identityTopics: List<Topic> = emptyList(),
        identitySubtopics: List<Subtopic> = emptyList(),
        private val identityFailure: Throwable? = null,
    ) : CurriculumRepository {
        private val questionsById = questions.associateBy(Question::id)
        private val identityTopicsById = identityTopics.associateBy(Topic::id)
        private val identitySubtopicsById = identitySubtopics.associateBy(Subtopic::id)

        var topicReadCount: Int = 0
            private set
        val subtopicReadTopicIds = mutableListOf<String>()
        var questionReadCount: Int = 0
            private set

        override suspend fun getActiveTopics(): List<Topic> {
            topicReadCount += 1
            return topicResults.removeFirst().getOrThrow()
        }

        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> {
            subtopicReadTopicIds += topicId
            return subtopicResults[topicId]?.removeFirst()?.getOrThrow().orEmpty()
        }

        /** LearningProgressService reads the ACTIVE bank once per derivation, for coverage. */
        override suspend fun getActiveQuestions(): List<Question> {
            questionReadCount += 1
            return questions
        }

        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> =
            error("Not used by TopicBrowserViewModel.")

        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
            error("Not used by TopicBrowserViewModel.")

        override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> =
            error("Not used by TopicBrowserViewModel.")

        override suspend fun getActiveQuestionsByTopicAndLevels(
            topicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = error("Not used by TopicBrowserViewModel.")

        override suspend fun getActiveQuestionsBySubtopicAndLevels(
            subtopicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = error("Not used by TopicBrowserViewModel.")

        // Resolves an answered question's topic and subtopic back to curriculum metadata while
        // accuracy is derived, and a historical Continue Studying scope back to current curriculum;
        // the catalog itself is read through getActiveTopics above.
        override suspend fun getTopicById(topicId: String): Topic? {
            identityFailure?.let { throw it }
            return identityTopicsById[topicId]
        }

        override suspend fun getSubtopicById(subtopicId: String): Subtopic? {
            identityFailure?.let { throw it }
            return identitySubtopicsById[subtopicId]
        }

        // Resolves an answered question back to its Topic when accuracy is derived. Historical
        // questions that no longer exist resolve to null and contribute to neither figure.
        override suspend fun getQuestionById(questionId: String): Question? =
            questionsById[questionId] ?: RetiredQuestionsById[questionId]
    }

    private class RecordingHistoryRepository(
        private val attempts: List<TestAttempt>,
    ) : AssessmentRepository {
        var readCount: Int = 0
            private set

        override suspend fun save(attempt: TestAttempt) = Unit
        override suspend fun getById(attemptId: String): TestAttempt? = null
        override suspend fun getCompletedAttempts(): List<TestAttempt> {
            readCount += 1
            return attempts
        }
    }

    private class MutableHistoryRepository : AssessmentRepository {
        var attempts: List<TestAttempt> = emptyList()

        override suspend fun save(attempt: TestAttempt) = Unit
        override suspend fun getById(attemptId: String): TestAttempt? = null
        override suspend fun getCompletedAttempts(): List<TestAttempt> = attempts
    }

    private object FailingHistoryRepository : AssessmentRepository {
        override suspend fun save(attempt: TestAttempt) = Unit
        override suspend fun getById(attemptId: String): TestAttempt? = null
        override suspend fun getCompletedAttempts(): List<TestAttempt> =
            error("History unavailable")
    }

    /** Keeps the shared cache on its Loading value for the whole test. */
    private object NeverReturningHistoryRepository : AssessmentRepository {
        override suspend fun save(attempt: TestAttempt) = Unit
        override suspend fun getById(attemptId: String): TestAttempt? = null
        override suspend fun getCompletedAttempts(): List<TestAttempt> {
            kotlinx.coroutines.awaitCancellation()
        }
    }

    private companion object {
        val CatalogTopics = listOf(
            Topic("compose", "Compose UI"),
            Topic("compose_architecture", "Compose Architecture"),
            Topic("architecture", "Lifecycle, State & Navigation"),
        )

        fun catalogRepository() = FakeCurriculumRepository(
            topicResults = resultsOf(CatalogTopics),
            subtopicResults = mutableMapOf(
                "compose" to resultsOf(
                    listOf(Subtopic("compose_runtime", "compose", "Compose runtime")),
                ),
                "compose_architecture" to resultsOf(emptyList()),
                "architecture" to resultsOf(
                    listOf(
                        Subtopic(
                            "viewmodel_lifecycle",
                            "architecture",
                            "ViewModel lifecycle",
                        ),
                    ),
                ),
            ),
            questions = ActiveQuestions,
        )

        /** Four ACTIVE compose questions, one under compose_architecture, two under architecture. */
        val ActiveQuestions = listOf(
            question("q_compose_1", "compose", "compose_runtime"),
            question("q_compose_2", "compose", "compose_runtime"),
            question("q_compose_3", "compose", "compose_runtime"),
            question("q_compose_4", "compose", "compose_runtime"),
            question("q_compose_arch_1", "compose_architecture", "compose_arch_sub"),
            question("q_architecture_1", "architecture", "viewmodel_lifecycle"),
            question("q_architecture_2", "architecture", "viewmodel_lifecycle"),
        )

        /**
         * Answered in history and still resolvable to their Topic, but no longer in the ACTIVE
         * bank: they carry accuracy without contributing any current coverage.
         */
        val RetiredQuestionsById = listOf(
            question("q_retired_1", "compose_architecture", "compose_arch_sub"),
            question("q_retired_2", "compose_architecture", "compose_arch_sub"),
            question("q_retired_3", "compose_architecture", "compose_arch_sub"),
        ).associateBy(Question::id)

        /**
         * The same catalogue, with the identity lookups a weak-area rationale is named from. The
         * name is resolved by the existing performance derivation, not by the recommendation.
         */
        fun namedCatalogRepository() = FakeCurriculumRepository(
            topicResults = resultsOf(CatalogTopics),
            subtopicResults = mutableMapOf(
                "compose" to resultsOf(
                    listOf(Subtopic("compose_runtime", "compose", "Compose runtime")),
                ),
                "compose_architecture" to resultsOf(emptyList()),
                "architecture" to resultsOf(emptyList()),
            ),
            questions = ActiveQuestions,
            identityTopics = CatalogTopics,
            identitySubtopics = listOf(
                Subtopic("compose_runtime", "compose", "Compose runtime"),
            ),
        )

        /** The same catalogue, with the identity lookups Continue Studying resolves against. */
        fun continueStudyingRepository(identityFailure: Throwable? = null) =
            FakeCurriculumRepository(
                topicResults = resultsOf(CatalogTopics),
                subtopicResults = mutableMapOf(
                    "compose" to resultsOf(emptyList()),
                    "compose_architecture" to resultsOf(emptyList()),
                    "architecture" to resultsOf(emptyList()),
                ),
                questions = ActiveQuestions,
                identityTopics = CatalogTopics,
                identityFailure = identityFailure,
            )

        /** One completed Topic-scoped practice run, which is all a Continue context needs. */
        fun completedFocusedAttempt(id: String, topicId: String): TestAttempt =
            TestAttempt(
                id = id,
                config = AssessmentConfig.Focused(
                    scope = AssessmentScope.Topic(topicId),
                    questionCount = 1,
                ),
                questionAttempts = listOf(
                    QuestionAttempt("q_$id", QuestionAnswerState.Answered(setOf("a"), true)),
                ),
                status = AssessmentStatus.COMPLETED,
                startedAt = Instant.parse("2026-08-29T00:00:00Z"),
                completedAt = Instant.parse("2026-08-29T00:15:00Z"),
                score = AssessmentScore(1, 1),
            )

        fun historyRepository(vararg answers: Pair<String, Boolean>) =
            RecordingHistoryRepository(
                if (answers.isEmpty()) emptyList() else listOf(completedAttempt("attempt", *answers)),
            )

        fun historyRepository(attempts: List<TestAttempt>) = RecordingHistoryRepository(attempts)

        fun answer(questionId: String, isCorrect: Boolean): Pair<String, Boolean> =
            questionId to isCorrect

        /**
         * One completed attempt. A Question answered more than once therefore needs more than one
         * attempt, which is also how it happens in the app: an attempt holds each Question at most
         * once, and repeats come from retakes.
         */
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
                startedAt = Instant.parse("2026-08-29T00:00:00Z"),
                completedAt = Instant.parse("2026-08-29T00:15:00Z"),
                score = AssessmentScore(answers.size, answers.count { it.second }),
            )

        fun question(id: String, topicId: String, subtopicId: String) = Question(
            id = id,
            topicId = topicId,
            subtopicId = subtopicId,
            text = "Question $id",
            answers = listOf(AnswerOption("${id}_a", "Answer A")),
            selectionMode = AnswerSelectionMode.SINGLE,
            level = QuestionLevel.FOUNDATION,
            correctAnswerIds = listOf("${id}_a"),
            explanation = "Explanation",
            sources = listOf(SourceReference("Source", "https://example.com")),
        )

        fun <T> resultsOf(vararg values: T): ArrayDeque<Result<T>> =
            ArrayDeque(values.map(Result.Companion::success))
    }
}
