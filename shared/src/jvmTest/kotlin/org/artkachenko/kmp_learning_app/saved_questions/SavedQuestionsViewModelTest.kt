package org.artkachenko.kmp_learning_app.saved_questions

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

@OptIn(ExperimentalCoroutinesApi::class)
internal class SavedQuestionsViewModelTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun savedStateThatHasNeverLoadedIsLoadingRatherThanEmpty() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = viewModel(FakeSavedQuestionRepository())

        // Nothing has been read yet, which is not the same statement as "nothing is saved".
        assertIs<SavedQuestionsUiState.Loading>(viewModel.uiState.value)
    }

    @Test
    fun unreadableSavedStateIsAScreenErrorBecauseItIsThisScreensPrimaryData() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = viewModel(FakeSavedQuestionRepository().apply { failReads = true })
        advanceUntilIdle()

        assertIs<SavedQuestionsUiState.Error>(viewModel.uiState.value)
    }

    @Test
    fun noSavedQuestionsIsTheExplicitEmptyState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = viewModel(FakeSavedQuestionRepository())
        advanceUntilIdle()

        assertIs<SavedQuestionsUiState.Empty>(viewModel.uiState.value)
    }

    @Test
    fun contentKeepsTheRepositoryOrderAndReadsTheSavedTableOnlyThroughTheSharedHolder() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = savedRepository("q3" to 300L, "q2" to 200L, "q1" to 100L)
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        val content = assertIs<SavedQuestionsUiState.Content>(viewModel.uiState.value)
        assertEquals(listOf("q3", "q2", "q1"), content.items.map { it.questionId })
        // One read, performed by the holder's own refresh. A screen-local cache or a second
        // repository subscription would show up as more.
        assertEquals(1, repository.readCalls)
        assertEquals(0, repository.isSavedCalls)
    }

    @Test
    fun deprecatedAndMissingContentKeepTheirPlaceInTheSavedOrder() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = viewModel(
            savedRepository("q3" to 300L, "q_gone" to 200L, "q1" to 100L),
            curriculum = VmCurriculumRepository(available = setOf("q3", "q1")),
        )
        advanceUntilIdle()

        val content = assertIs<SavedQuestionsUiState.Content>(viewModel.uiState.value)
        assertEquals(listOf("q3", "q_gone", "q1"), content.items.map { it.questionId })
        assertIs<SavedQuestionItem.Available>(content.items[0])
        assertIs<SavedQuestionItem.Missing>(content.items[1])
        assertIs<SavedQuestionItem.Available>(content.items[2])
    }

    @Test
    fun aRemovalInFlightDisablesOnlyThatQuestionAndKeepsTheListOnScreen() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = savedRepository("q3" to 300L, "q2" to 200L, "q1" to 100L)
        val gate = CompletableDeferred<Unit>()
        repository.unsaveGate = gate
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        val before = assertIs<SavedQuestionsUiState.Content>(viewModel.uiState.value).items

        viewModel.removeSaved("q2")
        advanceUntilIdle()

        val pending = assertIs<SavedQuestionsUiState.Content>(viewModel.uiState.value)
        assertEquals(setOf("q2"), pending.pendingQuestionIds)
        // The screen does not blank back to Loading for a pending mutation, and the other two
        // Questions keep their own actions.
        assertEquals(before, pending.items)

        gate.complete(Unit)
        advanceUntilIdle()
        val after = assertIs<SavedQuestionsUiState.Content>(viewModel.uiState.value)
        assertEquals(listOf("q3", "q1"), after.items.map { it.questionId })
        assertTrue(after.pendingQuestionIds.isEmpty())
    }

    @Test
    fun removingAQuestionUnsavesExactlyThatIdAndTheListFollowsPersistence() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = savedRepository("q3" to 300L, "q2" to 200L, "q1" to 100L)
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.removeSaved("q2")
        advanceUntilIdle()

        assertEquals(listOf("q2"), repository.unsaveCalls)
        // Never a save: this surface can only remove what is already saved.
        assertTrue(repository.saveCalls.isEmpty())
        val content = assertIs<SavedQuestionsUiState.Content>(viewModel.uiState.value)
        assertEquals(listOf("q3", "q1"), content.items.map { it.questionId })
    }

    @Test
    fun removingTheLastSavedQuestionReachesTheEmptyState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = viewModel(savedRepository("q1" to 100L))
        advanceUntilIdle()
        assertIs<SavedQuestionsUiState.Content>(viewModel.uiState.value)

        viewModel.removeSaved("q1")
        advanceUntilIdle()

        assertIs<SavedQuestionsUiState.Empty>(viewModel.uiState.value)
    }

    /**
     * The difference from a result screen's missing placeholder: that one offers no save action
     * because there is nothing to review, while this one is already saved. Without a way to remove
     * it, a stale identity would be stuck in the learner's collection permanently.
     */
    @Test
    fun aSavedQuestionWhoseContentIsGoneCanStillBeRemoved() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = savedRepository("q_old" to 100L)
        val viewModel = viewModel(repository, curriculum = VmCurriculumRepository(available = emptySet()))
        advanceUntilIdle()
        val content = assertIs<SavedQuestionsUiState.Content>(viewModel.uiState.value)
        assertIs<SavedQuestionItem.Missing>(content.items.single())

        viewModel.removeSaved("q_old")
        advanceUntilIdle()

        assertEquals(listOf("q_old"), repository.unsaveCalls)
        assertIs<SavedQuestionsUiState.Empty>(viewModel.uiState.value)
    }

    /**
     * The saved list is identical before and after the retry, so the holder's refresh re-reads an
     * equal value that a `StateFlow` does not re-emit. Retry has to rerun content resolution
     * itself, or the button would do nothing at all after a curriculum failure.
     */
    @Test
    fun retryRerunsContentResolutionEvenWhenTheSavedListIsUnchanged() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = savedRepository("q1" to 100L)
        val curriculum = VmCurriculumRepository(available = setOf("q1"), failing = true)
        val viewModel = viewModel(repository, curriculum)
        advanceUntilIdle()
        assertIs<SavedQuestionsUiState.Error>(viewModel.uiState.value)

        curriculum.failing = false
        viewModel.retry()
        advanceUntilIdle()

        val content = assertIs<SavedQuestionsUiState.Content>(viewModel.uiState.value)
        assertEquals(listOf("q1"), content.items.map { it.questionId })
    }

    @Test
    fun aQuestionSavedOnAnotherSurfaceAppearsAtItsRepositoryPosition() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = savedRepository("q1" to 100L)
        val holder = savedQuestionStateHolder(repository)
        val viewModel = SavedQuestionsViewModel(holder, SavedQuestionContentResolver(VmCurriculumRepository()))
        advanceUntilIdle()
        assertEquals(
            listOf("q1"),
            assertIs<SavedQuestionsUiState.Content>(viewModel.uiState.value).items.map { it.questionId },
        )

        // A result screen saving through the same app-scoped holder, with this screen already open.
        holder.toggleSaved("q3")
        advanceUntilIdle()

        val content = assertIs<SavedQuestionsUiState.Content>(viewModel.uiState.value)
        assertEquals(listOf("q3", "q1"), content.items.map { it.questionId })
    }

    private fun TestScope.viewModel(
        repository: FakeSavedQuestionRepository,
        curriculum: CurriculumRepository = VmCurriculumRepository(),
    ): SavedQuestionsViewModel =
        SavedQuestionsViewModel(
            savedQuestionStateHolder = savedQuestionStateHolder(repository),
            contentResolver = SavedQuestionContentResolver(curriculum),
        )
}

private fun savedRepository(vararg saved: Pair<String, Long>): FakeSavedQuestionRepository =
    FakeSavedQuestionRepository(saved.map { (id, savedAt) -> SavedQuestion(id, savedAt) })

/**
 * Resolves every ID unless it is withheld, and only through the historical stable-ID lookup: an
 * ACTIVE listing here would mean saved identity had been resolved through the current catalogue.
 */
private class VmCurriculumRepository(
    private val available: Set<String>? = null,
    var failing: Boolean = false,
) : CurriculumRepository {
    override suspend fun getQuestionById(questionId: String): Question? {
        if (failing) error("Curriculum unavailable.")
        if (available != null && questionId !in available) return null
        return Question(
            id = questionId,
            topicId = "kotlin",
            subtopicId = "coroutines",
            text = "Question $questionId",
            answers = listOf(
                AnswerOption("${questionId}_a", "Answer A"),
                AnswerOption("${questionId}_b", "Answer B"),
            ),
            selectionMode = AnswerSelectionMode.SINGLE,
            level = QuestionLevel.FOUNDATION,
            correctAnswerIds = listOf("${questionId}_a"),
            explanation = "Explanation",
            sources = listOf(SourceReference("Source", "https://example.com/$questionId")),
        )
    }

    override suspend fun getActiveTopics(): List<Topic> = error("ACTIVE lookup must not be used.")
    override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestions(): List<Question> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsByTopicAndLevels(
        topicId: String,
        levels: Set<QuestionLevel>,
    ): List<Question> = error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsBySubtopicAndLevels(
        subtopicId: String,
        levels: Set<QuestionLevel>,
    ): List<Question> = error("ACTIVE lookup must not be used.")
    override suspend fun getTopicById(topicId: String): Topic? = error("Topic lookup is not needed.")
    override suspend fun getSubtopicById(subtopicId: String): Subtopic? =
        error("Subtopic lookup is not needed.")
}
