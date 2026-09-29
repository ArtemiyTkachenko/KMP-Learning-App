package org.artkachenko.kmp_learning_app.topic_study.topics

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.assessment.history.asCompletedHistory
import org.artkachenko.kmp_learning_app.assessment.history.visibleHistory
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningLesson
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumSection
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibilityStateHolder
import org.artkachenko.kmp_learning_app.curriculum.visibility.VisibleCurriculumRepository
import org.artkachenko.kmp_learning_app.curriculum.visibility.VisibleLearningContentRepository
import org.artkachenko.kmp_learning_app.curriculum.visibility.completedAttempt
import org.artkachenko.kmp_learning_app.curriculum.visibility.curriculumVisibilityStateHolder
import org.artkachenko.kmp_learning_app.curriculum.visibility.focusedOn
import org.artkachenko.kmp_learning_app.curriculum.visibility.question
import org.artkachenko.kmp_learning_app.guided_learning.ContinueStudyingTarget
import org.artkachenko.kmp_learning_app.guided_learning.ContinueStudyingResolver
import org.artkachenko.kmp_learning_app.guided_learning.LearningRecommendationResolver
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.artkachenko.kmp_learning_app.lesson_study.FakeLessonStudyRepository
import org.artkachenko.kmp_learning_app.lesson_study.StudiedLesson
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressStateHolder
import org.artkachenko.kmp_learning_app.lesson_study.studyProgressStateHolder
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewService

/**
 * The Topic Browser's sections and its reaction to the learner's curriculum visibility.
 *
 * The raw fakes hold the whole curriculum, Kotlin Multiplatform included, and the ViewModel reads
 * them through the production visibility decorators bound to a production holder. Nothing in this
 * test removes a Topic by hand: a Topic appears or disappears only because the holder changed, which
 * is exactly how a Settings toggle reaches this screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class TopicBrowserVisibilityTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun visibleSectionsFollowSectionOrderAndKeepRepositoryOrderInside() = runVisibilityTest {
        val fixture = fixture(includeKmpContent = true)
        advanceUntilIdle()

        val content = content(fixture)
        assertEquals(
            listOf(CurriculumSection.AndroidEngineering, CurriculumSection.KotlinMultiplatform),
            content.sections.map(TopicBrowserSection::kind),
        )
        // The repository interleaves `kmp` between the two core Topics; sectioning moves it out
        // without reordering what remains.
        assertEquals(
            listOf(listOf("di", "compose"), listOf("kmp")),
            content.sections.map { section -> section.topics.map(TopicBrowserItemUiModel::topicId) },
        )
    }

    @Test
    fun withKmpHiddenOnlyTheAndroidSectionIsPresent() = runVisibilityTest {
        val fixture = fixture(includeKmpContent = false)
        advanceUntilIdle()

        val content = content(fixture)
        assertEquals(listOf(CurriculumSection.AndroidEngineering), content.sections.map { it.kind })
        assertEquals(listOf("di", "compose"), content.allTopics.map { it.topicId })
    }

    /**
     * A StateFlow hands its current value to a new collector. That value is the visibility the first
     * load was made under, so it must not become a second read of the catalogue.
     */
    @Test
    fun startingTheScreenReadsTheCatalogueOnce() = runVisibilityTest {
        val fixture = fixture(includeKmpContent = false)
        advanceUntilIdle()

        assertEquals(1, fixture.curriculum.topicReads)
        assertEquals(1, fixture.learning.orderedReads)
        assertEquals(1, fixture.history.reads)
    }

    @Test
    fun showingKmpAddsItsSectionToTheLiveScreen() = runVisibilityTest {
        val fixture = fixture(includeKmpContent = false)
        advanceUntilIdle()
        assertEquals(listOf(CurriculumSection.AndroidEngineering), content(fixture).sections.map { it.kind })

        fixture.holder.setIncludeKmpContent(true)
        advanceUntilIdle()

        val content = content(fixture)
        assertEquals(
            listOf(CurriculumSection.AndroidEngineering, CurriculumSection.KotlinMultiplatform),
            content.sections.map { it.kind },
        )
        // Re-read through the decorators rather than filtered from a cache, availability included.
        assertEquals(2, fixture.curriculum.topicReads)
        assertEquals(2, fixture.learning.orderedReads)
        assertEquals(2, content.allTopics.single { it.topicId == "kmp" }.learningUnitCount)
    }

    @Test
    fun hidingKmpRemovesItsSectionFromTheLiveScreen() = runVisibilityTest {
        val fixture = fixture(includeKmpContent = true)
        advanceUntilIdle()

        fixture.holder.setIncludeKmpContent(false)
        advanceUntilIdle()

        val content = content(fixture)
        assertEquals(listOf(CurriculumSection.AndroidEngineering), content.sections.map { it.kind })
        assertEquals(listOf("di", "compose"), content.allTopics.map { it.topicId })
    }

    @Test
    fun anActiveQueryIsKeptAndMatchedAgainstTheNewlyVisibleCatalogue() = runVisibilityTest {
        val fixture = fixture(includeKmpContent = false)
        advanceUntilIdle()
        fixture.viewModel.onSearchQueryChange("koin")
        assertEquals(listOf("koin_definitions"), content(fixture).subtopicMatches.map { it.subtopicId })

        fixture.holder.setIncludeKmpContent(true)
        advanceUntilIdle()

        val shown = content(fixture)
        assertEquals("koin", shown.query)
        assertEquals(
            listOf("koin_definitions", "koin_shared_graph"),
            shown.subtopicMatches.map { it.subtopicId },
        )

        fixture.holder.setIncludeKmpContent(false)
        advanceUntilIdle()

        val hidden = content(fixture)
        assertEquals("koin", hidden.query)
        assertEquals(listOf("koin_definitions"), hidden.subtopicMatches.map { it.subtopicId })
    }

    /**
     * Continue Learning walks the visible authored sequence. With every core Lesson studied the core
     * curriculum is complete; showing KMP extends the sequence with its two Units, and hiding it
     * again returns the core-only answer.
     */
    @Test
    fun continueLearningWalksOnlyTheVisibleSequence() = runVisibilityTest {
        val fixture = fixture(
            includeKmpContent = false,
            studied = listOf("lesson_di", "lesson_compose"),
        )
        advanceUntilIdle()
        assertEquals(ContinueLearningUiModel.Complete, content(fixture).continueLearning)

        fixture.holder.setIncludeKmpContent(true)
        advanceUntilIdle()
        val next = assertIs<ContinueLearningUiModel.Next>(content(fixture).continueLearning)
        assertEquals("lesson_kmp_1", next.target.lessonId)

        fixture.holder.setIncludeKmpContent(false)
        advanceUntilIdle()
        assertEquals(ContinueLearningUiModel.Complete, content(fixture).continueLearning)
    }

    /**
     * Hiding a Topic filters what is derived, never what is stored: a KMP Lesson studied while shown
     * is still studied after a hide-and-show round trip, and nothing was unmarked on the way.
     */
    @Test
    fun aStudiedKmpLessonSurvivesBeingHiddenAndShownAgain() = runVisibilityTest {
        val fixture = fixture(
            includeKmpContent = true,
            studied = listOf("lesson_di", "lesson_compose"),
        )
        advanceUntilIdle()
        fixture.studyHolder.toggleStudied("lesson_kmp_1")
        advanceUntilIdle()

        fixture.holder.setIncludeKmpContent(false)
        advanceUntilIdle()
        fixture.holder.setIncludeKmpContent(true)
        advanceUntilIdle()

        val next = assertIs<ContinueLearningUiModel.Next>(content(fixture).continueLearning)
        assertEquals("lesson_kmp_2", next.target.lessonId)
        assertTrue(fixture.studyRepository.unmarkCalls.isEmpty())
    }

    /**
     * A visibility change refreshes the shared study record and re-reads curriculum, but does not
     * touch raw assessment history: the visible projection re-emits by itself and the database has
     * not changed.
     */
    @Test
    fun aVisibilityChangeRefreshesStudyStateWithoutReReadingRawHistory() = runVisibilityTest {
        val fixture = fixture(includeKmpContent = false)
        advanceUntilIdle()
        val studyReads = fixture.studyRepository.studiedLessonReads

        fixture.holder.setIncludeKmpContent(true)
        advanceUntilIdle()
        fixture.holder.setIncludeKmpContent(false)
        advanceUntilIdle()

        assertEquals(1, fixture.history.reads)
        assertTrue(fixture.studyRepository.studiedLessonReads >= studyReads + 2)
    }

    /**
     * A load made under the old visibility that finishes after the load made under the new one is
     * discarded by the same generation contract that protects a retry.
     */
    @Test
    fun aSlowLoadFromTheOldVisibilityCannotOverwriteTheNewOne() = runVisibilityTest {
        val gate = CompletableDeferred<Unit>()
        val fixture = fixture(includeKmpContent = false, firstTopicReadGate = gate)
        advanceUntilIdle()
        assertEquals(TopicBrowserUiState.Loading, fixture.viewModel.uiState.value)

        fixture.holder.setIncludeKmpContent(true)
        advanceUntilIdle()
        assertEquals(
            listOf(CurriculumSection.AndroidEngineering, CurriculumSection.KotlinMultiplatform),
            content(fixture).sections.map { it.kind },
        )

        gate.complete(Unit)
        advanceUntilIdle()

        val content = content(fixture)
        assertEquals(
            listOf(CurriculumSection.AndroidEngineering, CurriculumSection.KotlinMultiplatform),
            content.sections.map { it.kind },
        )
        assertEquals(2, content.allTopics.single { it.topicId == "kmp" }.learningUnitCount)
    }

    /**
     * Hiding KMP reloads the catalogue and re-projects history independently. When the catalogue
     * wins, the guidance derived from the KMP-visible history must not be shown beside it — here a
     * Continue Studying shortcut back into the `kmp` Topic — until the new projection arrives.
     */
    @Test
    fun guidanceFromTheOldVisibilityIsWithheldUntilHistoryIsReprojected() = runVisibilityTest {
        val fixture = fixture(
            includeKmpContent = true,
            attempts = listOf(
                completedAttempt(
                    "kmp_run",
                    completedAtSeconds = 2_000,
                    "k1" to true,
                    config = focusedOn(Topics[1], questionCount = 1),
                ),
                completedAttempt(
                    "di_run",
                    completedAtSeconds = 1_000,
                    "d1" to true,
                    config = focusedOn(Topics[0], questionCount = 1),
                ),
            ),
        )
        advanceUntilIdle()
        assertEquals(ContinueStudyingTarget.Topic("kmp"), content(fixture).continueStudying?.target)

        // The projection's Question lookup stalls; the catalogue read does not.
        val projection = CompletableDeferred<Unit>()
        fixture.curriculum.questionReadGate = projection
        fixture.holder.setIncludeKmpContent(false)
        advanceUntilIdle()

        val reloaded = content(fixture)
        assertEquals(listOf(CurriculumSection.AndroidEngineering), reloaded.sections.map { it.kind })
        assertNull(reloaded.continueStudying)
        assertNull(reloaded.recommendedNext)
        assertTrue(reloaded.allTopics.all { it.learningContext == null })

        projection.complete(Unit)
        advanceUntilIdle()

        // The KMP run is projected away, so the shortcut falls through to the core run.
        assertEquals(ContinueStudyingTarget.Topic("di"), content(fixture).continueStudying?.target)
    }

    private fun content(fixture: Fixture): TopicBrowserUiState.Content =
        assertIs<TopicBrowserUiState.Content>(fixture.viewModel.uiState.value)

    private class Fixture(
        val holder: CurriculumVisibilityStateHolder,
        val curriculum: RawCurriculum,
        val learning: RawLearningContent,
        val history: RecordingHistory,
        val studyRepository: FakeLessonStudyRepository,
        val studyHolder: StudyProgressStateHolder,
        val viewModel: TopicBrowserViewModel,
    )

    private fun TestScope.fixture(
        includeKmpContent: Boolean,
        studied: List<String> = emptyList(),
        firstTopicReadGate: CompletableDeferred<Unit>? = null,
        attempts: List<TestAttempt> = emptyList(),
    ): Fixture {
        val holder = curriculumVisibilityStateHolder(includeKmpContent)
        val rawCurriculum = RawCurriculum(firstTopicReadGate)
        val rawLearning = RawLearningContent()
        val curriculum = VisibleCurriculumRepository(rawCurriculum, holder.visibility)
        val learning = VisibleLearningContentRepository(rawLearning, holder.visibility)
        val history = RecordingHistory(attempts)
        val studyRepository = FakeLessonStudyRepository(
            *studied.map { StudiedLesson(lessonId = it, studiedAtEpochMillis = 1_000) }.toTypedArray(),
        )
        val studyHolder = studyProgressStateHolder(studyRepository)
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val mistakeReviewService = MistakeReviewService(
            completedHistory = history.asCompletedHistory(),
            assessmentReviewLoader = AssessmentReviewLoader(curriculum),
        )
        val viewModel = TopicBrowserViewModel(
            curriculumRepository = curriculum,
            learningContentRepository = learning,
            learningProgressService = LearningProgressService(history.asCompletedHistory(), curriculum),
            visibleHistory = AssessmentHistoryStore(history, scope).visibleHistory(
                scope = scope,
                curriculumRepository = curriculum,
                visibility = holder.visibility,
            ),
            continueStudyingResolver = ContinueStudyingResolver(curriculum),
            learningRecommendationResolver = LearningRecommendationResolver { attempts ->
                mistakeReviewService.countUnresolved(attempts)
            },
            studyProgressStateHolder = studyHolder,
            visibilityStateHolder = holder,
        )
        return Fixture(holder, rawCurriculum, rawLearning, history, studyRepository, studyHolder, viewModel)
    }

    private fun runVisibilityTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }

    /** The whole curriculum, KMP included, with `kmp` authored between the two core Topics. */
    private class RawCurriculum(private var firstTopicReadGate: CompletableDeferred<Unit>?) : CurriculumRepository {
        var topicReads = 0
            private set

        /** Holds the next Question lookup — the history projection's — until completed. */
        var questionReadGate: CompletableDeferred<Unit>? = null

        override suspend fun getActiveTopics(): List<Topic> {
            topicReads += 1
            firstTopicReadGate?.let { gate ->
                firstTopicReadGate = null
                gate.await()
            }
            return Topics
        }

        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> =
            Subtopics.filter { it.topicId == topicId }

        override suspend fun getActiveQuestions(): List<Question> = emptyList()
        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> = emptyList()
        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> = emptyList()
        override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> = emptyList()
        override suspend fun getActiveQuestionsByTopicAndLevels(
            topicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = emptyList()
        override suspend fun getActiveQuestionsBySubtopicAndLevels(
            subtopicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = emptyList()
        override suspend fun getTopicById(topicId: String): Topic? = Topics.firstOrNull { it.id == topicId }
        override suspend fun getSubtopicById(subtopicId: String): Subtopic? =
            Subtopics.firstOrNull { it.id == subtopicId }
        override suspend fun getQuestionsByIds(questionIds: Collection<String>): Map<String, Question> {
            questionReadGate?.let { gate ->
                questionReadGate = null
                gate.await()
            }
            return Questions.filter { it.id in questionIds }.associateBy(Question::id)
        }
    }

    /** Two core Units, then the two KMP Units, in authored order. */
    private class RawLearningContent : LearningContentRepository {
        var orderedReads = 0
            private set

        override suspend fun getActiveUnits(): List<LearningUnit> {
            orderedReads += 1
            return Units
        }

        override suspend fun getActiveUnitsByTopic(topicId: String): List<LearningUnit> =
            Units.filter { it.topicId == topicId }

        override suspend fun getUnitById(unitId: String): LearningUnit? = Units.firstOrNull { it.id == unitId }

        override suspend fun getLessonById(lessonId: String): LearningLesson? =
            Units.flatMap { it.lessons }.firstOrNull { it.id == lessonId }
    }

    /** A fixed completed history whose raw reads are counted. */
    private class RecordingHistory(private val attempts: List<TestAttempt>) : AssessmentRepository {
        var reads = 0
            private set

        override suspend fun save(attempt: TestAttempt) = Unit
        override suspend fun getById(attemptId: String): TestAttempt? = null
        override suspend fun getCompletedAttempts(): List<TestAttempt> {
            reads += 1
            return attempts
        }
    }

    private companion object {
        val Topics = listOf(
            Topic("di", "Dependency Injection"),
            Topic("kmp", "Kotlin Multiplatform"),
            Topic("compose", "Compose UI"),
        )
        val Subtopics = listOf(
            Subtopic("koin_definitions", "di", "Koin definitions"),
            Subtopic("koin_shared_graph", "kmp", "Koin in a shared graph"),
            Subtopic("state_hoisting", "compose", "State hoisting"),
        )
        /** Resolvable only through the historical lookup, which the history projection uses. */
        val Questions = listOf(question("d1", Subtopics[0]), question("k1", Subtopics[1]))
        val Units = listOf(
            unit("unit_di", "di", "lesson_di"),
            unit("unit_compose", "compose", "lesson_compose"),
            unit("unit_kmp_1", "kmp", "lesson_kmp_1"),
            unit("unit_kmp_2", "kmp", "lesson_kmp_2"),
        )

        fun unit(id: String, topicId: String, lessonId: String) = LearningUnit(
            id = id,
            topicId = topicId,
            title = "Unit $id",
            summary = "Summary for $id",
            lessons = listOf(
                LearningLesson(
                    id = lessonId,
                    title = "Lesson $lessonId",
                    summary = "Summary for $lessonId",
                    primarySubtopicIds = emptyList(),
                    supportingSubtopicIds = emptyList(),
                    sections = emptyList(),
                    relatedLessonIds = emptyList(),
                    sources = emptyList(),
                ),
            ),
        )
    }
}
