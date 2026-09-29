package org.artkachenko.kmp_learning_app.curriculum.visibility

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.history.AppCoroutineScope
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentSelectionResult
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.curriculum.learning.content.learningContentModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.assessment.assessmentDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDataInitializer
import org.artkachenko.kmp_learning_app.data.local.curriculum.curriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.lesson_study.lessonStudyDataModule
import org.artkachenko.kmp_learning_app.data.local.saved_questions.savedQuestionDataModule
import org.artkachenko.kmp_learning_app.lesson_study.repository.LessonStudyRepository
import org.artkachenko.kmp_learning_app.mixed_interview.InterviewHistoryStateHolder
import org.artkachenko.kmp_learning_app.mixed_interview.InterviewHistoryUiState
import org.artkachenko.kmp_learning_app.progress.ProgressStateHolder
import org.artkachenko.kmp_learning_app.progress.ProgressUiState
import org.artkachenko.kmp_learning_app.saved_questions.repository.SavedQuestionRepository
import org.artkachenko.kmp_learning_app.topic_study.topicStudyPresentationModule
import org.koin.core.Koin
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * The production graph over the bundled curriculum: the preference, both decorators and the history
 * projection wired as every host wires them, with only the database and preference storage
 * substituted.
 */
internal class CurriculumVisibilityIntegrationTest {

    @Test
    fun theProductionDefaultHidesKmpFromMixedSelection() = withGraph { koin ->
        val selector = koin.get<AssessmentQuestionSelector>()
        val visibleCount = koin.get<CurriculumRepository>().getActiveQuestions().size

        val hidden = assertIs<AssessmentSelectionResult.Selected>(
            selector.select(AssessmentConfig.Mixed(questionCount = 10_000)),
        )
        assertTrue(hidden.questions.none { it.topicId == "kmp" })
        assertEquals(visibleCount, hidden.questions.size)
        assertTrue(koin.get<LearningContentRepository>().getActiveUnits().none { it.topicId == "kmp" })

        // Audit decision D-1: shown, `kmp` is one more Topic in the round-robin — one Question per
        // Topic in the first round, KMP's included, with no special weight.
        koin.get<CurriculumVisibilityStateHolder>().setIncludeKmpContent(true)
        val topicCount = koin.get<CurriculumRepository>().getActiveTopics().size
        val round = assertIs<AssessmentSelectionResult.Selected>(
            selector.select(AssessmentConfig.Mixed(questionCount = topicCount)),
        )
        assertEquals(1, round.questions.count { it.topicId == "kmp" })
        assertEquals(topicCount, round.questions.map { it.topicId }.distinct().size)
    }

    /**
     * A mixed interview that answered one core Question correctly and one KMP Question wrongly:
     * 1/2 stored, 1/1 while KMP is hidden. The app-scoped history surfaces follow the preference
     * without an invalidation or a write, and a round trip leaves every stored record as it was.
     */
    @Test
    fun aVisibilityRoundTripRederivesHistorySurfacesAndLeavesStoredRecordsUntouched() = withGraph { koin ->
        val curriculum = koin.get<CurriculumRepository>()
        val holder = koin.get<CurriculumVisibilityStateHolder>()
        holder.setIncludeKmpContent(true)
        val core = curriculum.getActiveQuestions().first { it.topicId != "kmp" }
        val kmp = curriculum.getActiveQuestions().first { it.topicId == "kmp" }
        val kmpLesson = koin.get<LearningContentRepository>().getActiveUnitsByTopic("kmp").first().lessons.first()
        holder.setIncludeKmpContent(false)

        val attempts = koin.get<AssessmentRepository>()
        attempts.save(completedAttempt("mixed", 1_000, core.id to true, kmp.id to false))
        koin.get<SavedQuestionRepository>().save(kmp.id)
        koin.get<LessonStudyRepository>().markStudied(kmpLesson.id)
        val storedAttempts = attempts.getCompletedAttempts()
        val storedSaved = koin.get<SavedQuestionRepository>().getSavedQuestions()
        val storedStudy = koin.get<LessonStudyRepository>().getStudiedLessons()

        // Resolved only now, so the history cache's first read sees the saved attempt.
        val progress = koin.get<ProgressStateHolder>().state
        val interviews = koin.get<InterviewHistoryStateHolder>().state

        progress.awaitContent { it.answeredQuestionCount == 1 && it.percentage == 100.0 }
        interviews.awaitLatest { it.totalQuestions == 1 && it.percentage == 100.0 }

        holder.setIncludeKmpContent(true)
        progress.awaitContent { it.answeredQuestionCount == 2 && it.percentage == 50.0 }
        interviews.awaitLatest { it.totalQuestions == 2 && it.percentage == 50.0 }

        holder.setIncludeKmpContent(false)
        progress.awaitContent { it.answeredQuestionCount == 1 && it.percentage == 100.0 }
        interviews.awaitLatest { it.totalQuestions == 1 && it.percentage == 100.0 }

        assertEquals(storedAttempts, attempts.getCompletedAttempts())
        assertEquals(storedSaved, koin.get<SavedQuestionRepository>().getSavedQuestions())
        assertEquals(storedStudy, koin.get<LessonStudyRepository>().getStudiedLessons())
    }

    private suspend fun StateFlow<ProgressUiState>.awaitContent(predicate: (ProgressUiState.Content) -> Boolean) {
        withTimeout(TimeoutMillis) { first { it is ProgressUiState.Content && predicate(it) } }
    }

    private suspend fun StateFlow<InterviewHistoryUiState>.awaitLatest(
        predicate: (org.artkachenko.kmp_learning_app.mixed_interview.InterviewAttemptUiModel) -> Boolean,
    ) {
        withTimeout(TimeoutMillis) {
            first { it is InterviewHistoryUiState.Content && predicate(it.history.latest) }
        }
    }

    /**
     * Real dispatchers throughout: Room completes on its own executors and the app-scoped holders
     * derive on the application scope, so the test waits for states rather than advancing a clock.
     */
    private fun withGraph(block: suspend (Koin) -> Unit) = runBlocking {
        val database = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        val app = koinApplication {
            modules(
                module { single<CurriculumDatabase> { database } },
                curriculumDataModule,
                learningContentModule,
                assessmentDataModule,
                savedQuestionDataModule,
                lessonStudyDataModule,
                topicStudyPresentationModule,
                curriculumVisibilityModule,
                kmpContentPreferenceTestModule(includeKmpContent = false),
            )
        }
        try {
            app.koin.get<CurriculumDataInitializer>().initialize()
            block(app.koin)
        } finally {
            // Stop the app-scoped derivations before the database they read goes away.
            app.koin.get<AppCoroutineScope>().cancel()
            app.close()
            database.close()
        }
    }

    private companion object {
        const val TimeoutMillis = 10_000L
    }
}
