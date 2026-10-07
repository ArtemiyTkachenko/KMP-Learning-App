package org.artkachenko.kmp_learning_app.progress

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.assessment.history.CompletedAssessmentHistory
import org.artkachenko.kmp_learning_app.assessment.history.visibleHistory
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.curriculum.visibility.VisibleCurriculumRepository
import org.artkachenko.kmp_learning_app.curriculum.visibility.curriculumVisibilityStateHolder
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.artkachenko.kmp_learning_app.learning_progress.ReadinessGap
import org.artkachenko.kmp_learning_app.learning_progress.ReadinessService
import org.artkachenko.kmp_learning_app.learning_progress.ReadinessStatus
import org.artkachenko.kmp_learning_app.learning_progress.ReadinessTarget
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewService

/**
 * The dashboard holder's readiness, end to end over the real history cache, its visibility
 * projection and its due-mistake re-announcement — the refreshes the section has to follow without
 * a restart.
 *
 * Time is virtual. The clock every component reads is [Start] plus the scheduler's virtual time, so
 * advancing the scheduler is the one way a mistake becomes due. `runCurrent()` rather than
 * `advanceUntilIdle()` settles each step, because the latter would run the re-announcement's waits
 * to completion and make every mistake due at once.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ProgressReadinessStateTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun withNoTargetTheSectionAsksAndReadsNoCurriculumForIt() = readinessTest {
        val harness = harness(target = null)
        runCurrent()

        assertEquals(ProgressReadinessUiModel.TargetNotChosen, content(harness).readiness)
        // Coverage's own ACTIVE read only: readiness has nothing to derive until a target exists.
        assertEquals(1, harness.curriculum.activeQuestionCalls)
    }

    @Test
    fun choosingATargetDerivesReadinessImmediatelyAndChangingItReDerives() = readinessTest {
        val harness = harness(target = null)
        runCurrent()

        harness.viewModel.chooseReadinessTarget(ReadinessTarget.JUNIOR)
        runCurrent()
        val junior = assessed(harness)
        assertEquals(ReadinessTarget.JUNIOR, junior.target)
        assertEquals(listOf("android"), junior.topics.map { it.topicId })
        assertEquals(10, junior.topics.single().targetQuestionCount)

        harness.viewModel.chooseReadinessTarget(ReadinessTarget.SENIOR)
        runCurrent()
        val senior = assessed(harness)
        assertEquals(ReadinessTarget.SENIOR, senior.target)
        // The Applied Questions now count, and the Topic has no Advanced ones to be judged on.
        assertEquals(12, senior.topics.single().targetQuestionCount)
        assertEquals(true, senior.topics.single().advancedUnavailable)
        assertEquals("senior", harness.storedTarget())
    }

    @Test
    fun completingAnAssessmentRefreshesReadiness() = readinessTest {
        val harness = harness(target = ReadinessTarget.JUNIOR, attempts = listOf(attempt("first", ago = 1.hours, "f1" to true)))
        runCurrent()
        val before = assessed(harness).topics.single()
        assertEquals(ReadinessStatus.NOT_ENOUGH_EVIDENCE, before.status)
        assertEquals(listOf<ReadinessGap>(ReadinessGap.MoreAnswersNeeded(4)), before.gaps)

        // What assessment completion does: a saved attempt, then the shared invalidation.
        harness.assessment.attempts = listOf(
            attempt("second", ago = Duration.ZERO, *correct("f2", "f3", "f4", "f5", "f6")),
        ) + harness.assessment.attempts
        harness.store.invalidate()
        runCurrent()

        assertEquals(ReadinessStatus.READY, assessed(harness).topics.single().status)
    }

    @Test
    fun hiddenTopicsAreExcludedAndReturnWhenShown() = readinessTest {
        val harness = harness(
            target = ReadinessTarget.JUNIOR,
            includeKmp = false,
            attempts = listOf(
                attempt("kmp_run", ago = 1.hours, *correct("k1", "k2", "k3", "k4", "k5")),
                attempt("android_run", ago = 2.hours, "f1" to true),
            ),
        )
        runCurrent()
        assertEquals(listOf("android"), assessed(harness).topics.map { it.topicId })
        assertEquals(0, assessed(harness).readyTopicCount)

        harness.visibility.setIncludeKmpContent(true)
        runCurrent()
        val shown = assessed(harness)
        assertEquals(listOf("android", "kmp"), shown.topics.map { it.topicId })
        assertEquals(ReadinessStatus.READY, shown.topics.single { it.topicId == "kmp" }.status)
        assertEquals(1, shown.readyTopicCount)

        harness.visibility.setIncludeKmpContent(false)
        runCurrent()
        assertEquals(listOf("android"), assessed(harness).topics.map { it.topicId })
    }

    @Test
    fun aMistakeFallingDueTakesReadyAwayWithoutNewHistory() = readinessTest {
        val harness = harness(
            target = ReadinessTarget.JUNIOR,
            attempts = listOf(
                attempt("new", ago = Duration.ZERO, *correct("f1", "f2", "f3", "f4", "f5", "f6")),
                attempt("old", ago = 1.minutes, "f7" to false),
            ),
        )
        runCurrent()
        assertEquals(ReadinessStatus.READY, assessed(harness).topics.single().status)
        val readsBefore = harness.assessment.getCompletedCalls

        // The mistake becomes due about twenty hours after it was made.
        advanceTimeBy(21.hours)
        runCurrent()

        val after = assessed(harness).topics.single()
        assertEquals(ReadinessStatus.GETTING_THERE, after.status)
        assertEquals(listOf<ReadinessGap>(ReadinessGap.DueMistakes(1)), after.gaps)
        assertEquals(readsBefore, harness.assessment.getCompletedCalls)
    }

    @Test
    fun theDrillDownSelectsItsTopicFromTheLiveDashboardState() = readinessTest {
        val harness = harness(target = ReadinessTarget.JUNIOR, attempts = listOf(attempt("first", ago = 1.hours, "f1" to true)))
        val drillDown = ProgressTopicViewModel(
            topicId = "android",
            learningProgressService = LearningProgressService(
                CompletedAssessmentHistory { harness.assessment.getCompletedAttempts() },
                harness.curriculum,
            ),
            visibilityStateHolder = harness.visibility,
            dashboardState = harness.viewModel.uiState,
        )
        runCurrent()
        assertEquals(ReadinessStatus.NOT_ENOUGH_EVIDENCE, drillDown.readiness.value?.status)

        harness.assessment.attempts = listOf(
            attempt("second", ago = Duration.ZERO, *correct("f2", "f3", "f4", "f5", "f6")),
        ) + harness.assessment.attempts
        harness.store.invalidate()
        runCurrent()
        assertEquals(ReadinessStatus.READY, drillDown.readiness.value?.status)
    }
}

private val Start = Instant.parse("2026-03-01T12:00:00Z")

private fun readinessTest(body: suspend TestScope.() -> Unit) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    body()
}

private class ReadinessHarness(
    val viewModel: ProgressViewModel,
    val store: AssessmentHistoryStore,
    val assessment: MutableAssessmentRepository,
    val curriculum: CountingCurriculum,
    val visibility: org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibilityStateHolder,
    val storedTarget: () -> String?,
)

/**
 * Android: ten Foundation and two Applied Questions. KMP: five Foundation. Every component reads one
 * clock, [Start] plus virtual time, and the curriculum is the visible decorator over the raw one.
 */
private fun TestScope.harness(
    target: ReadinessTarget?,
    attempts: List<TestAttempt> = emptyList(),
    includeKmp: Boolean = false,
): ReadinessHarness {
    // The history must exist for the dashboard to leave Empty, so a harness without any still holds
    // one answer outside every Topic the tests measure.
    val history = attempts.ifEmpty { listOf(attempt("unrelated", ago = 1.hours, "elsewhere" to true)) }
    val now = { Start + testScheduler.currentTime.milliseconds }
    val assessment = MutableAssessmentRepository(history.map { it.shiftedTo(now()) })
    val raw = CountingCurriculum(
        questions = (1..10).map { question("f$it", "android", QuestionLevel.FOUNDATION) } +
            (1..2).map { question("a$it", "android", QuestionLevel.APPLIED) } +
            (1..5).map { question("k$it", "kmp", QuestionLevel.FOUNDATION) } +
            question("elsewhere", "retired_topic", QuestionLevel.ADVANCED),
        topics = listOf(Topic("android", "Android"), Topic("kmp", "Kotlin Multiplatform")),
    )
    val visibility = curriculumVisibilityStateHolder(includeKmpContent = includeKmp)
    val curriculum = VisibleCurriculumRepository(raw, visibility.visibility)
    // Cancelled with the test, which also ends the re-announcement's pending waits.
    val scope = backgroundScope
    val store = AssessmentHistoryStore(assessment, scope)
    val visibleHistory = store.visibleHistory(scope, curriculum, visibility.visibility, now)
    val storage = org.artkachenko.kmp_learning_app.settings.InMemoryAppPreferenceStorage()
    val targetHolder = ReadinessTargetStateHolder(
        ReadinessTargetPreferenceStore(storage).also { store -> target?.let(store::write) },
    )
    val completed = CompletedAssessmentHistory { visibleHistory.completedAttempts() }
    val stateHolder = ProgressStateHolder(
        learningProgressService = LearningProgressService(completed, curriculum),
        curriculumRepository = curriculum,
        mistakeReviewService = MistakeReviewService(completed, AssessmentReviewLoader(curriculum), now),
        readinessService = ReadinessService(curriculum, now),
        visibleHistory = visibleHistory,
        readinessTarget = targetHolder.target,
        scope = scope,
    )
    store.invalidate()
    return ReadinessHarness(
        viewModel = ProgressViewModel(store, stateHolder, targetHolder),
        store = store,
        assessment = assessment,
        curriculum = raw,
        visibility = visibility,
        storedTarget = { storage.stored(ReadinessTargetPreferenceStore.Key) },
    )
}

private fun content(harness: ReadinessHarness): ProgressUiState.Content =
    assertIs<ProgressUiState.Content>(harness.viewModel.uiState.value)

private fun assessed(harness: ReadinessHarness): ProgressReadinessUiModel.Assessed =
    assertIs<ProgressReadinessUiModel.Assessed>(content(harness).readiness)

private fun correct(vararg ids: String): Array<Pair<String, Boolean>> =
    ids.map { it to true }.toTypedArray()

/**
 * An attempt [ago] before the moment it is put into history. Built against [Start] and moved to the
 * harness clock when the harness is made, so a test states ages rather than instants.
 */
private fun attempt(id: String, ago: Duration, vararg outcomes: Pair<String, Boolean>): TestAttempt {
    val completedAt = Start - ago
    return TestAttempt(
        id = id,
        config = AssessmentConfig.Mixed(outcomes.size),
        questionAttempts = outcomes.map { (questionId, isCorrect) ->
            QuestionAttempt(questionId, QuestionAnswerState.Answered(setOf("${questionId}_a"), isCorrect))
        },
        status = AssessmentStatus.COMPLETED,
        startedAt = completedAt - 10.minutes,
        completedAt = completedAt,
        score = AssessmentScore(outcomes.size, outcomes.count { it.second }),
    )
}

private fun TestAttempt.shiftedTo(now: Instant): TestAttempt {
    val offset = now - Start
    return copy(startedAt = startedAt + offset, completedAt = completedAt?.plus(offset))
}

private fun question(id: String, topicId: String, level: QuestionLevel): Question =
    Question(
        id = id,
        topicId = topicId,
        subtopicId = "${topicId}_core",
        text = "Question $id",
        answers = listOf(AnswerOption("${id}_a", "A"), AnswerOption("${id}_b", "B")),
        selectionMode = AnswerSelectionMode.SINGLE,
        level = level,
        correctAnswerIds = listOf("${id}_a"),
        explanation = "Because.",
        sources = listOf(SourceReference("Source", "https://example.com/$id")),
    )

private class MutableAssessmentRepository(var attempts: List<TestAttempt>) : AssessmentRepository {
    var getCompletedCalls = 0

    override suspend fun save(attempt: TestAttempt) = Unit

    override suspend fun getById(attemptId: String): TestAttempt? = attempts.firstOrNull { it.id == attemptId }

    override suspend fun getCompletedAttempts(): List<TestAttempt> {
        getCompletedCalls += 1
        return attempts
    }
}

private class CountingCurriculum(
    private val questions: List<Question>,
    topics: List<Topic>,
) : CurriculumRepository {
    private val questionsById = questions.associateBy(Question::id)
    private val topicsById = topics.associateBy(Topic::id)
    var activeQuestionCalls = 0

    override suspend fun getActiveTopics(): List<Topic> = topicsById.values.toList()
    override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> = emptyList()
    override suspend fun getActiveQuestions(): List<Question> {
        activeQuestionCalls += 1
        return questions.filter { it.topicId != "retired_topic" }
    }
    override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> =
        questions.filter { it.topicId == topicId }
    override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
        questions.filter { it.subtopicId == subtopicId }
    override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> =
        questions.filter { it.level in levels }
    override suspend fun getActiveQuestionsByTopicAndLevels(topicId: String, levels: Set<QuestionLevel>): List<Question> =
        questions.filter { it.topicId == topicId && it.level in levels }
    override suspend fun getActiveQuestionsBySubtopicAndLevels(subtopicId: String, levels: Set<QuestionLevel>): List<Question> =
        questions.filter { it.subtopicId == subtopicId && it.level in levels }
    override suspend fun getTopicById(topicId: String): Topic? = topicsById[topicId]
    override suspend fun getSubtopicById(subtopicId: String): Subtopic? = null
    override suspend fun getQuestionsByIdsForCurrentContent(questionIds: Collection<String>): Map<String, Question> =
        questionIds.mapNotNull(questionsById::get).associateBy(Question::id)
    override suspend fun getQuestionsByIds(questionIds: Collection<String>): Map<String, Question> =
        questionIds.mapNotNull(questionsById::get).associateBy(Question::id)
}
