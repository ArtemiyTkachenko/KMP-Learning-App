package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningLesson
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibilityStateHolder
import org.artkachenko.kmp_learning_app.curriculum.visibility.VisibleCurriculumRepository
import org.artkachenko.kmp_learning_app.curriculum.visibility.VisibleLearningContentRepository
import org.artkachenko.kmp_learning_app.curriculum.visibility.curriculumVisibilityStateHolder
import org.artkachenko.kmp_learning_app.curriculum.visibility.question
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.artkachenko.kmp_learning_app.lesson_study.FakeLessonStudyRepository
import org.artkachenko.kmp_learning_app.lesson_study.studyProgressStateHolder

/**
 * Topic Detail's reaction to the learner's curriculum visibility, through the production decorators.
 *
 * Presentation reactivity only: a Topic that stops being visible becomes the screen's existing
 * NotFound because the repository stops returning it. Leaving the destination is route guarding,
 * which this screen does not do.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class TopicDetailVisibilityTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startingTheScreenLoadsTheTopicOnce() = runVisibilityTest {
        val fixture = fixture(topicId = "android", includeKmpContent = false)
        advanceUntilIdle()

        assertIs<TopicDetailUiState.Content>(fixture.viewModel.uiState.value)
        assertEquals(1, fixture.curriculum.topicReads)
        assertEquals(1, fixture.learning.topicReads)
        assertEquals(1, fixture.history.reads)
    }

    @Test
    fun aCoreTopicStaysLoadedAndIsReReadOnEveryVisibilityChange() = runVisibilityTest {
        val fixture = fixture(topicId = "android", includeKmpContent = false)
        advanceUntilIdle()

        fixture.holder.setIncludeKmpContent(true)
        advanceUntilIdle()
        assertCoreContent(fixture)

        fixture.holder.setIncludeKmpContent(false)
        advanceUntilIdle()
        assertCoreContent(fixture)

        assertEquals(3, fixture.curriculum.topicReads)
        assertEquals(3, fixture.learning.topicReads)
    }

    @Test
    fun aKmpTopicBecomesNotFoundWhenHiddenAndReturnsWhenShown() = runVisibilityTest {
        val fixture = fixture(topicId = "kmp", includeKmpContent = true)
        advanceUntilIdle()
        val shown = assertIs<TopicDetailUiState.Content>(fixture.viewModel.uiState.value)
        assertEquals(1, shown.topicQuestionCount)

        fixture.holder.setIncludeKmpContent(false)
        advanceUntilIdle()
        assertEquals(TopicDetailUiState.NotFound, fixture.viewModel.uiState.value)

        fixture.holder.setIncludeKmpContent(true)
        advanceUntilIdle()
        val again = assertIs<TopicDetailUiState.Content>(fixture.viewModel.uiState.value)
        assertEquals("kmp", again.topic.id)
        assertEquals(1, again.topicQuestionCount)
        val units = assertIs<TopicLearningUnitsUiState.Available>(again.learningUnits)
        assertEquals(1, units.units.size)
    }

    /**
     * The curriculum reload does not reach raw history, and the study record is refreshed rather
     * than filtered.
     */
    @Test
    fun aVisibilityChangeRefreshesStudyStateWithoutReReadingRawHistory() = runVisibilityTest {
        val fixture = fixture(topicId = "android", includeKmpContent = false)
        advanceUntilIdle()
        val studyReads = fixture.studyRepository.studiedLessonReads

        fixture.holder.setIncludeKmpContent(true)
        advanceUntilIdle()

        assertEquals(1, fixture.history.reads)
        assertEquals(studyReads + 1, fixture.studyRepository.studiedLessonReads)
    }

    @Test
    fun aSlowLoadFromTheOldVisibilityCannotOverwriteTheNewOne() = runVisibilityTest {
        val gate = CompletableDeferred<Unit>()
        val fixture = fixture(topicId = "kmp", includeKmpContent = false, firstTopicReadGate = gate)
        advanceUntilIdle()
        assertEquals(TopicDetailUiState.Loading, fixture.viewModel.uiState.value)

        fixture.holder.setIncludeKmpContent(true)
        advanceUntilIdle()
        assertIs<TopicDetailUiState.Content>(fixture.viewModel.uiState.value)

        // The first load was made while KMP was hidden, so it resolves to NotFound — and must lose.
        gate.complete(Unit)
        advanceUntilIdle()
        assertIs<TopicDetailUiState.Content>(fixture.viewModel.uiState.value)
    }

    private fun assertCoreContent(fixture: Fixture) {
        val content = assertIs<TopicDetailUiState.Content>(fixture.viewModel.uiState.value)
        assertEquals("android", content.topic.id)
        assertEquals(1, content.topicQuestionCount)
    }

    private class Fixture(
        val holder: CurriculumVisibilityStateHolder,
        val curriculum: RawCurriculum,
        val learning: RawLearningContent,
        val history: RecordingHistory,
        val studyRepository: FakeLessonStudyRepository,
        val viewModel: TopicDetailViewModel,
    )

    private fun TestScope.fixture(
        topicId: String,
        includeKmpContent: Boolean,
        firstTopicReadGate: CompletableDeferred<Unit>? = null,
    ): Fixture {
        val holder = curriculumVisibilityStateHolder(includeKmpContent)
        val rawCurriculum = RawCurriculum(firstTopicReadGate)
        val rawLearning = RawLearningContent()
        val curriculum = VisibleCurriculumRepository(rawCurriculum, holder.visibility)
        val history = RecordingHistory()
        val studyRepository = FakeLessonStudyRepository()
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val viewModel = TopicDetailViewModel(
            topicId = topicId,
            curriculumRepository = curriculum,
            learningContentRepository = VisibleLearningContentRepository(rawLearning, holder.visibility),
            learningProgressService = LearningProgressService(history.asCompletedHistory(), curriculum),
            visibleHistory = AssessmentHistoryStore(history, scope).visibleHistory(
                scope = scope,
                curriculumRepository = curriculum,
                visibility = holder.visibility,
            ),
            studyProgressStateHolder = studyProgressStateHolder(studyRepository),
            visibilityStateHolder = holder,
        )
        return Fixture(holder, rawCurriculum, rawLearning, history, studyRepository, viewModel)
    }

    private fun runVisibilityTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }

    /** One core Topic and the KMP Topic, one Subtopic and one Question each. */
    private class RawCurriculum(private var firstTopicReadGate: CompletableDeferred<Unit>?) : CurriculumRepository {
        var topicReads = 0
            private set

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
        ): List<Question> = Questions.filter { it.topicId == topicId && it.level in levels }
        override suspend fun getActiveQuestionsBySubtopicAndLevels(
            subtopicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = Questions.filter { it.subtopicId == subtopicId && it.level in levels }
        override suspend fun getTopicById(topicId: String): Topic? = Topics.firstOrNull { it.id == topicId }
        override suspend fun getSubtopicById(subtopicId: String): Subtopic? =
            Subtopics.firstOrNull { it.id == subtopicId }
        override suspend fun getQuestionsByIdsForCurrentContent(
            questionIds: Collection<String>,
        ): Map<String, Question> = getQuestionsByIds(questionIds)

        override suspend fun getQuestionsByIds(questionIds: Collection<String>): Map<String, Question> =
            Questions.filter { it.id in questionIds }.associateBy(Question::id)
    }

    private class RawLearningContent : LearningContentRepository {
        var topicReads = 0
            private set

        override suspend fun getActiveUnits(): List<LearningUnit> = Units

        override suspend fun getActiveUnitsByTopic(topicId: String): List<LearningUnit> {
            topicReads += 1
            return Units.filter { it.topicId == topicId }
        }

        override suspend fun getUnitById(unitId: String): LearningUnit? = Units.firstOrNull { it.id == unitId }

        override suspend fun getLessonById(lessonId: String): LearningLesson? =
            Units.flatMap { it.lessons }.firstOrNull { it.id == lessonId }
    }

    private class RecordingHistory : AssessmentRepository {
        var reads = 0
            private set

        override suspend fun save(attempt: TestAttempt) = Unit
        override suspend fun getById(attemptId: String): TestAttempt? = null
        override suspend fun getCompletedAttempts(): List<TestAttempt> {
            reads += 1
            return emptyList()
        }
    }

    private companion object {
        val Topics = listOf(Topic("android", "Android"), Topic("kmp", "Kotlin Multiplatform"))
        val Subtopics = listOf(
            Subtopic("android_lifecycle", "android", "Lifecycle"),
            Subtopic("kmp_expect_actual", "kmp", "expect / actual"),
        )
        val Questions = listOf(question("a1", Subtopics[0]), question("k1", Subtopics[1]))
        val Units = listOf(
            unit("unit_android", "android", "lesson_android"),
            unit("unit_kmp", "kmp", "lesson_kmp"),
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
