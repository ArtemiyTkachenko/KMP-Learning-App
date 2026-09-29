package org.artkachenko.kmp_learning_app.topic_study.topics

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.artkachenko.kmp_learning_app.assessment.history.AppCoroutineScope
import org.artkachenko.kmp_learning_app.curriculum.content.BundledCurriculumSource
import org.artkachenko.kmp_learning_app.curriculum.learning.content.learningContentModule
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumSection
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibilityStateHolder
import org.artkachenko.kmp_learning_app.curriculum.visibility.curriculumVisibilityModule
import org.artkachenko.kmp_learning_app.curriculum.visibility.kmpContentPreferenceTestModule
import org.artkachenko.kmp_learning_app.data.local.assessment.assessmentDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.CurriculumDatabase
import org.artkachenko.kmp_learning_app.data.local.curriculum.curriculumDataModule
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImportResult
import org.artkachenko.kmp_learning_app.data.local.curriculum.importer.CurriculumImporter
import org.artkachenko.kmp_learning_app.data.local.lesson_study.lessonStudyDataModule
import org.artkachenko.kmp_learning_app.data.local.saved_questions.savedQuestionDataModule
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressStateHolder
import org.artkachenko.kmp_learning_app.lesson_study.repository.LessonStudyRepository
import org.artkachenko.kmp_learning_app.topic_study.topicStudyPresentationModule
import org.koin.core.Koin
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * One live Topic Browser over the production graph and the bundled curriculum, following the
 * learner's Kotlin Multiplatform preference without being recreated.
 *
 * Nothing here filters a fixture by hand: the ViewModel reads through the production decorators, and
 * the only thing the test changes is the app-scoped holder the Settings switch calls.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class TopicBrowserVisibilityIntegrationTest {
    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun theLiveBrowserGainsAndLosesTheKmpSectionItsSearchAndItsUnits() = runGraphTest {
        val holder = koin.get<CurriculumVisibilityStateHolder>()
        holder.setIncludeKmpContent(true)
        val kmpSubtopic = koin.get<CurriculumRepository>().getActiveSubtopics(KmpTopicId).first()
        val kmpUnitCount = koin.get<LearningContentRepository>().getActiveUnitsByTopic(KmpTopicId).size
        holder.setIncludeKmpContent(false)

        val browser = koin.get<TopicBrowserViewModel>()
        val hidden = browser.uiState.awaitContent { it.continueLearning != null }
        assertEquals(listOf(CurriculumSection.AndroidEngineering), hidden.sections.map { it.kind })
        assertTrue(hidden.allTopics.none { it.topicId == KmpTopicId })

        // The Subtopic's own name, typed in full: nothing core is called that.
        browser.onSearchQueryChange(kmpSubtopic.name)
        assertTrue(content(browser).subtopicMatches.none { it.parentTopicId == KmpTopicId })

        holder.setIncludeKmpContent(true)
        val shown = browser.uiState.awaitContent { state ->
            state.sections.any { it.kind == CurriculumSection.KotlinMultiplatform } &&
                state.allTopics.single { it.topicId == KmpTopicId }.learningUnitCount != null
        }
        assertEquals(
            listOf(CurriculumSection.AndroidEngineering, CurriculumSection.KotlinMultiplatform),
            shown.sections.map { it.kind },
        )
        assertEquals(listOf(KmpTopicId), shown.sections.last().topics.map { it.topicId })
        assertEquals(kmpUnitCount, shown.allTopics.single { it.topicId == KmpTopicId }.learningUnitCount)
        assertEquals(kmpSubtopic.name, shown.query)
        assertTrue(shown.subtopicMatches.any { it.subtopicId == kmpSubtopic.id })

        holder.setIncludeKmpContent(false)
        val hiddenAgain = browser.uiState.awaitContent { state ->
            state.sections.none { it.kind == CurriculumSection.KotlinMultiplatform }
        }
        assertEquals(kmpSubtopic.name, hiddenAgain.query)
        assertTrue(hiddenAgain.subtopicMatches.none { it.parentTopicId == KmpTopicId })
    }

    /**
     * Continue Learning keeps the authored global sequence. KMP is reachable only once every core
     * Lesson is studied; with it hidden the same record is simply complete, and hiding it again
     * returns that answer.
     */
    @Test
    fun continueLearningReachesKmpOnlyAfterEveryCoreLesson() = runGraphTest {
        val holder = koin.get<CurriculumVisibilityStateHolder>()
        val learning = koin.get<LearningContentRepository>()
        val coreUnits = learning.getActiveUnits()
        holder.setIncludeKmpContent(true)
        val firstKmpUnit = learning.getActiveUnits().first { it.topicId == KmpTopicId }

        val browser = koin.get<TopicBrowserViewModel>()
        val start = browser.uiState.awaitContent { state ->
            state.sections.size == 2 && state.continueLearning is ContinueLearningUiModel.Next
        }
        val first = (start.continueLearning as ContinueLearningUiModel.Next).target
        assertEquals(coreUnits.first().id, first.unitId)
        assertNotEquals(firstKmpUnit.id, first.unitId)

        val study = koin.get<LessonStudyRepository>()
        coreUnits.flatMap { it.lessons }.forEach { study.markStudied(it.id) }
        koin.get<StudyProgressStateHolder>().refresh()
        browser.uiState.awaitContent { state ->
            (state.continueLearning as? ContinueLearningUiModel.Next)?.target?.unitId == firstKmpUnit.id
        }

        holder.setIncludeKmpContent(false)
        browser.uiState.awaitContent { state ->
            state.sections.size == 1 && state.continueLearning == ContinueLearningUiModel.Complete
        }

        holder.setIncludeKmpContent(true)
        val again = browser.uiState.awaitContent { state ->
            state.sections.size == 2 && state.continueLearning is ContinueLearningUiModel.Next
        }
        assertEquals(
            firstKmpUnit.lessons.first().id,
            (again.continueLearning as ContinueLearningUiModel.Next).target.lessonId,
        )
    }

    private fun content(browser: TopicBrowserViewModel): TopicBrowserUiState.Content =
        browser.uiState.value as TopicBrowserUiState.Content

    /** Real Room work runs on Room's own executor, so waiting has to leave virtual time. */
    private suspend fun StateFlow<TopicBrowserUiState>.awaitContent(
        predicate: (TopicBrowserUiState.Content) -> Boolean,
    ): TopicBrowserUiState.Content =
        withContext(Dispatchers.Default) {
            withTimeout(AwaitTimeoutMillis) {
                first { it is TopicBrowserUiState.Content && predicate(it) }
            } as TopicBrowserUiState.Content
        }

    private class Graph(val koin: Koin)

    /**
     * The production graph over an in-memory database holding the bundled curriculum, starting from
     * the production default: Kotlin Multiplatform content hidden.
     */
    private fun runGraphTest(block: suspend Graph.() -> Unit) = runTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val database = Room.inMemoryDatabaseBuilder<CurriculumDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        assertEquals(
            CurriculumImportResult.Imported,
            CurriculumImporter(database, loadCurriculum = { BundledCurriculumSource.load() })
                .importCurriculum(),
        )
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
            Graph(app.koin).block()
        } finally {
            app.koin.get<AppCoroutineScope>().cancel()
            app.close()
            database.close()
        }
    }

    private companion object {
        const val KmpTopicId = "kmp"
        const val AwaitTimeoutMillis = 10_000L
    }
}
