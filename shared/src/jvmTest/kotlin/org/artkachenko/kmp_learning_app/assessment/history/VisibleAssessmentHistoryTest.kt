package org.artkachenko.kmp_learning_app.assessment.history

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibility
import org.artkachenko.kmp_learning_app.curriculum.visibility.FixtureCurriculumRepository
import org.artkachenko.kmp_learning_app.curriculum.visibility.VisibilityFixture
import org.artkachenko.kmp_learning_app.curriculum.visibility.completedAttempt

/**
 * The observable projection: it must follow both of its inputs, and keep the raw store's promise
 * that every settled refresh is announced, even an unchanged one.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class VisibleAssessmentHistoryTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val mixed = completedAttempt("mixed", 300, "a1" to true, "k1" to false)
    private val kmpOnly = completedAttempt("kmp_only", 200, "k2" to false)
    private val core = completedAttempt("core", 100, "c1" to true)
    private val raw = listOf(mixed, kmpOnly, core)

    @Test
    fun aVisibilityChangeReprojectsTheSameRawHistoryWithoutReReadingIt() = runProjectionTest {
        val repository = CountingRepository(raw)
        val visibility = VisibilityFixture.visibility(includeKmpContent = false)
        val projection = testHistoryStore(repository, testCacheScope())
            .visibleHistory(testCacheScope(), FixtureCurriculumRepository(), visibility)
        val observed = observe(projection)

        visibility.value = CurriculumVisibility.from(includeKmpContent = true)
        advanceUntilIdle()
        visibility.value = CurriculumVisibility.from(includeKmpContent = false)
        advanceUntilIdle()

        assertEquals(
            listOf(
                listOf("mixed" to listOf("a1"), "core" to listOf("c1")),
                listOf("mixed" to listOf("a1", "k1"), "kmp_only" to listOf("k2"), "core" to listOf("c1")),
                listOf("mixed" to listOf("a1"), "core" to listOf("c1")),
            ),
            observed.loaded(),
        )
        assertEquals(1, repository.reads, "A visibility change must not re-read the attempt table.")
    }

    /** The raw store's contract survives the projection: an unchanged refresh is still an event. */
    @Test
    fun anUnchangedRawRefreshIsReannouncedAndReprojected() = runProjectionTest {
        val repository = CountingRepository(raw)
        val curriculum = FixtureCurriculumRepository()
        val store = testHistoryStore(repository, testCacheScope())
        val projection = store.visibleHistory(
            testCacheScope(),
            curriculum,
            VisibilityFixture.visibility(includeKmpContent = false),
        )
        val observed = observe(projection)

        store.invalidate()
        advanceUntilIdle()

        val loaded = observed.filterIsInstance<AssessmentHistory.Loaded>()
        assertEquals(2, loaded.size, "An unchanged re-read must still reach every derived consumer.")
        assertEquals(loaded[0], loaded[1])
        assertEquals(2, curriculum.questionsByIdsCalls.size, "Each refresh is projected again.")
    }

    @Test
    fun anUnreadableHistoryPassesThroughAsFailed() = runProjectionTest {
        val repository = CountingRepository(raw, failure = IllegalStateException("Database unavailable"))
        val projection = testHistoryStore(repository, testCacheScope())
            .visibleHistory(testCacheScope(), FixtureCurriculumRepository(), VisibilityFixture.visibility(false))

        val observed = observe(projection)

        assertEquals(AssessmentHistory.Failed, observed.last())
        assertEquals(emptyList(), observed.filterIsInstance<AssessmentHistory.Loaded>())
    }

    /**
     * Unclassifiable history is published as a failure, not as the unprojected attempts, which
     * would show hidden content. Invalidating the raw store is the retry.
     */
    @Test
    fun aMetadataFailureIsPublishedAsFailedAndRetriedByInvalidation() = runProjectionTest {
        val curriculum = FlakyCurriculum()
        val projection = testHistoryStore(CountingRepository(raw), testCacheScope())
            .visibleHistory(testCacheScope(), curriculum, VisibilityFixture.visibility(false))
        val observed = observe(projection)
        assertEquals(AssessmentHistory.Failed, observed.last())

        curriculum.failing = false
        projection.invalidate()
        advanceUntilIdle()

        assertEquals(
            listOf("mixed" to listOf("a1"), "core" to listOf("c1")),
            observed.loaded().last(),
        )
    }

    @Test
    fun theOneShotReadProjectsUnderTheVisibilityCurrentAtTheCall() = runProjectionTest {
        val visibility = VisibilityFixture.visibility(includeKmpContent = false)
        val projection = testHistoryStore(CountingRepository(raw), testCacheScope())
            .visibleHistory(testCacheScope(), FixtureCurriculumRepository(), visibility)

        assertEquals(listOf("mixed", "core"), projection.completedAttempts().map { it.id })
        visibility.value = CurriculumVisibility.from(includeKmpContent = true)
        assertEquals(raw, projection.completedAttempts())
    }

    @Test
    fun theOneShotReadFailsWhenMetadataCannotBeResolved() = runProjectionTest {
        val projection = testHistoryStore(CountingRepository(raw), testCacheScope())
            .visibleHistory(testCacheScope(), FlakyCurriculum(), VisibilityFixture.visibility(false))

        assertFailsWith<IllegalStateException> { projection.completedAttempts() }
    }

    private fun TestScope.observe(projection: VisibleAssessmentHistory): List<AssessmentHistory> {
        val observed = mutableListOf<AssessmentHistory>()
        val observerScope = testCacheScope()
        observerScope.launch { projection.history.toList(observed) }
        advanceUntilIdle()
        return observed
    }

    private fun List<AssessmentHistory>.loaded(): List<List<Pair<String, List<String>>>> =
        filterIsInstance<AssessmentHistory.Loaded>().map { history ->
            history.attempts.map { attempt -> attempt.id to attempt.questionAttempts.map { it.questionId } }
        }

    private fun runProjectionTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }

    private class CountingRepository(
        private val attempts: List<TestAttempt>,
        private val failure: Exception? = null,
    ) : AssessmentRepository {
        var reads = 0

        override suspend fun save(attempt: TestAttempt) = error("The projection must never write.")

        override suspend fun getById(attemptId: String): TestAttempt? = attempts.firstOrNull { it.id == attemptId }

        override suspend fun getCompletedAttempts(): List<TestAttempt> {
            reads++
            failure?.let { throw it }
            return attempts
        }
    }

    /** The fixture curriculum, except that the historical resolver can be made to fail. */
    private class FlakyCurriculum(
        private val delegate: FixtureCurriculumRepository = FixtureCurriculumRepository(),
    ) : org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository by delegate {
        var failing = true

        override suspend fun getQuestionsByIds(questionIds: Collection<String>): Map<String, Question> {
            if (failing) throw IllegalStateException("Curriculum unavailable")
            return delegate.getQuestionsByIds(questionIds)
        }
    }
}
