package org.artkachenko.kmp_learning_app.saved_questions

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.saved_questions.repository.LocalSavedQuestionRepository

/**
 * The capture path end to end: the mutation a review card triggers, through the shared state holder,
 * into the real E18-01 table. The repository's own contract is covered by
 * `LocalSavedQuestionRepositoryTest`; what is proved here is that the UI-facing mutation boundary
 * reaches that persistence, and that what the surfaces then show is what was persisted.
 */
internal class SavedQuestionCaptureIntegrationTest {
    @Test
    fun savingAndUnsavingThroughTheSharedHolderReachesTheSavedQuestionTable() = runTest {
        val database = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val repository = LocalSavedQuestionRepository(database)
            val holder = SavedQuestionStateHolder(repository, scope)

            holder.refresh()
            assertEquals(emptyList(), holder.awaitSavedIds(emptyList()))

            holder.toggleSaved("q1")

            assertEquals(listOf("q1"), holder.awaitSavedIds(listOf("q1")))
            assertEquals(listOf("q1"), repository.getSavedQuestions().map { it.questionId })
            assertEquals(1, database.savedQuestionDao().count())

            // A save request that reaches persistence again cannot add a second row, which is why
            // the presentation layer does not check whether a Question is already saved first.
            repository.save("q1")
            assertEquals(1, database.savedQuestionDao().count())

            holder.toggleSaved("q1")

            assertEquals(emptyList(), holder.awaitSavedIds(emptyList()))
            assertEquals(0, database.savedQuestionDao().count())
        } finally {
            scope.cancel()
            database.close()
        }
    }

    /**
     * Room's work does not run on the test scheduler, so the settled state is awaited rather than
     * advanced to.
     */
    private suspend fun SavedQuestionStateHolder.awaitSavedIds(
        expected: List<String>,
    ): List<String> = withContext(Dispatchers.Default) {
        withTimeout(5_000) {
            state
                .first { current ->
                    current is SavedQuestionsState.Loaded &&
                        current.pendingQuestionIds.isEmpty() &&
                        current.savedQuestions.map { it.questionId } == expected
                }
                .let { (it as SavedQuestionsState.Loaded).savedQuestions.map { saved -> saved.questionId } }
        }
    }
}
