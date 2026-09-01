package org.artkachenko.kmp_learning_app.mistake_review

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

/**
 * The queue is derived from the LATEST completed occurrence of each stable Question ID. Attempts
 * below are listed newest first, matching the repository contract the service consumes.
 */
internal class MistakeReviewServiceTest {
    @Test
    fun emptyHistoryProducesAnEmptyQueue() = runTest {
        assertEquals(emptyList(), service(attempts = emptyList()).load())
    }

    @Test
    fun latestIncorrectOccurrenceBecomesUnresolved() = runTest {
        val service = service(attempts = listOf(attempt("a1", "2026-08-29T10:00:00Z", "q1" to false)))

        assertEquals(listOf("q1"), service.load().map { it.questionId })
    }

    @Test
    fun latestCorrectOccurrenceIsResolvedAndAbsent() = runTest {
        val service = service(attempts = listOf(attempt("a1", "2026-08-29T10:00:00Z", "q1" to true)))

        assertEquals(emptyList(), service.load())
    }

    @Test
    fun repeatedIncorrectOccurrencesProduceOneRowFromTheNewestAttempt() = runTest {
        val service = service(
            attempts = listOf(
                attempt("newest", "2026-08-29T12:00:00Z", "q1" to false),
                attempt("oldest", "2026-08-29T10:00:00Z", "q1" to false),
            ),
        )

        val queue = service.load()
        assertEquals(listOf("q1"), queue.map { it.questionId })
        assertEquals("newest", queue.single().sourceAttemptId)
    }

    @Test
    fun laterCorrectOccurrenceResolvesAnEarlierMistake() = runTest {
        val service = service(
            attempts = listOf(
                attempt("newest", "2026-08-29T12:00:00Z", "q1" to true),
                attempt("oldest", "2026-08-29T10:00:00Z", "q1" to false),
            ),
        )

        assertEquals(emptyList(), service.load())
    }

    @Test
    fun laterIncorrectOccurrenceMakesAPreviouslyCorrectQuestionUnresolvedAgain() = runTest {
        val service = service(
            attempts = listOf(
                attempt("newest", "2026-08-29T12:00:00Z", "q1" to false),
                attempt("oldest", "2026-08-29T10:00:00Z", "q1" to true),
            ),
        )

        assertEquals(listOf("q1"), service.load().map { it.questionId })
    }

    @Test
    fun incorrectThenCorrectThenIncorrectUsesTheNewestOccurrence() = runTest {
        val service = service(
            attempts = listOf(
                attempt("newest", "2026-08-29T14:00:00Z", "q1" to false),
                attempt("middle", "2026-08-29T12:00:00Z", "q1" to true),
                attempt("oldest", "2026-08-29T10:00:00Z", "q1" to false),
            ),
        )

        val queue = service.load()
        assertEquals(listOf("q1"), queue.map { it.questionId })
        assertEquals("newest", queue.single().sourceAttemptId)
    }

    @Test
    fun queueOrdersByNewestAttemptThenPersistedQuestionAttemptOrder() = runTest {
        val service = service(
            attempts = listOf(
                attempt("newest", "2026-08-29T12:00:00Z", "q3" to false, "q1" to false),
                attempt("oldest", "2026-08-29T10:00:00Z", "q2" to false),
            ),
        )

        // q3 before q1 because that is the persisted order inside the newest attempt.
        assertEquals(listOf("q3", "q1", "q2"), service.load().map { it.questionId })
    }

    @Test
    fun newestCorrectOccurrenceRemovesOnlyThatQuestion() = runTest {
        val service = service(
            attempts = listOf(
                attempt("newest", "2026-08-29T12:00:00Z", "q1" to true),
                attempt("oldest", "2026-08-29T10:00:00Z", "q1" to false, "q2" to false),
            ),
        )

        assertEquals(listOf("q2"), service.load().map { it.questionId })
    }

    @Test
    fun aLaterFocusedCorrectAnswerResolvesAnEarlierMixedMistake() = runTest {
        val service = service(
            attempts = listOf(
                attempt(
                    "focused",
                    "2026-08-29T12:00:00Z",
                    "q1" to true,
                    config = AssessmentConfig.Focused(AssessmentScope.Topic("kotlin"), 1),
                ),
                attempt("mixed", "2026-08-29T10:00:00Z", "q1" to false),
            ),
        )

        assertEquals(emptyList(), service.load())
    }

    @Test
    fun aLaterMixedMistakeReopensAQuestionAnsweredCorrectlyInFocusedPractice() = runTest {
        val service = service(
            attempts = listOf(
                attempt("mixed", "2026-08-29T12:00:00Z", "q1" to false),
                attempt(
                    "focused",
                    "2026-08-29T10:00:00Z",
                    "q1" to true,
                    config = AssessmentConfig.Focused(AssessmentScope.Subtopic("coroutines"), 1),
                ),
            ),
        )

        assertEquals(listOf("q1"), service.load().map { it.questionId })
    }

    @Test
    fun aRetakeIsTreatedAsAnOrdinaryCompletedAttempt() = runTest {
        // No retake lineage is persisted or inferred; recency alone decides.
        val service = service(
            attempts = listOf(
                attempt("retake", "2026-08-29T12:00:00Z", "q1" to false),
                attempt("original", "2026-08-29T10:00:00Z", "q1" to true),
            ),
        )

        val queue = service.load()
        assertEquals(listOf("q1"), queue.map { it.questionId })
        assertEquals("retake", queue.single().sourceAttemptId)
    }

    @Test
    fun selectedAnswersComeFromTheLatestOccurrence() = runTest {
        val service = service(
            attempts = listOf(
                attempt("newest", "2026-08-29T12:00:00Z", selections = mapOf("q1" to setOf("q1_b"))),
                attempt("oldest", "2026-08-29T10:00:00Z", selections = mapOf("q1" to setOf("q1_a"))),
            ),
        )

        val available = assertIs<ReviewQuestionItem.Available>(service.load().single().reviewItem)
        assertEquals(
            listOf("q1_b"),
            available.question.answers.filter { it.wasSelected }.map { it.id },
        )
    }

    @Test
    fun persistedCorrectnessIsUsedEvenWhenAuthoredAnswersChanged() = runTest {
        // The persisted occurrence selected the currently-correct answer but was stored incorrect.
        // History must win: recalculating from correctAnswerIds would silently resolve it.
        val service = service(
            attempts = listOf(
                attempt(
                    "a1",
                    "2026-08-29T10:00:00Z",
                    selections = mapOf("q1" to setOf("q1_a")),
                    correctness = mapOf("q1" to false),
                ),
            ),
        )

        val available = assertIs<ReviewQuestionItem.Available>(service.load().single().reviewItem)
        assertEquals(false, available.question.isCorrect)
    }

    @Test
    fun aMissingHistoricalQuestionStaysInTheQueue() = runTest {
        val service = service(
            attempts = listOf(attempt("a1", "2026-08-29T10:00:00Z", "gone" to false)),
            questions = emptyList(),
        )

        val mistake = service.load().single()
        assertEquals("gone", mistake.questionId)
        assertEquals(ReviewQuestionItem.Missing("gone"), mistake.reviewItem)
    }

    @Test
    fun aDeprecatedQuestionRemainsFullyReviewable() = runTest {
        val service = service(
            attempts = listOf(attempt("a1", "2026-08-29T10:00:00Z", "q1" to false)),
            questions = listOf(question("q1", status = ContentStatus.DEPRECATED)),
        )

        val available = assertIs<ReviewQuestionItem.Available>(service.load().single().reviewItem)
        assertEquals("Question q1", available.question.text)
        assertEquals("Explanation q1", available.question.explanation)
        assertEquals(listOf("Source q1"), available.question.sources.map { it.title })
    }

    @Test
    fun reviewContentIsLoadedOnlyForUnresolvedQuestions() = runTest {
        val curriculum = RecordingCurriculumRepository(defaultQuestions())
        val service = MistakeReviewService(
            assessmentRepository = HistoryRepository(
                listOf(
                    attempt("a1", "2026-08-29T10:00:00Z", "q1" to false, "q2" to true, "q3" to true),
                ),
            ),
            assessmentReviewLoader = AssessmentReviewLoader(curriculum),
        )

        service.load()

        assertEquals(listOf("q1"), curriculum.questionLookups)
    }

    @Test
    fun attemptsThatAreNotCompletedAreIgnored() = runTest {
        // getCompletedAttempts() is contractually completed-only; the service filters defensively
        // so a violation cannot surface an unanswered occurrence in the queue.
        val inProgress = TestAttempt(
            id = "in-progress",
            config = AssessmentConfig.Mixed(1),
            questionAttempts = listOf(QuestionAttempt("q9", QuestionAnswerState.Unanswered)),
            status = AssessmentStatus.IN_PROGRESS,
            startedAt = Instant.parse("2026-08-29T13:00:00Z"),
        )
        val service = service(
            attempts = listOf(inProgress, attempt("a1", "2026-08-29T10:00:00Z", "q1" to false)),
        )

        assertEquals(listOf("q1"), service.load().map { it.questionId })
    }

    @Test
    fun countUnresolvedAgreesWithTheQueueWithoutLoadingReviewContent() = runTest {
        val curriculum = RecordingCurriculumRepository(defaultQuestions())
        val service = MistakeReviewService(
            assessmentRepository = HistoryRepository(
                listOf(
                    attempt("newest", "2026-08-29T12:00:00Z", "q1" to false, "q2" to true),
                    attempt("oldest", "2026-08-29T10:00:00Z", "q3" to false),
                ),
            ),
            assessmentReviewLoader = AssessmentReviewLoader(curriculum),
        )

        assertEquals(2, service.countUnresolved())
        // The count is a pure pass over persisted correctness: no question content is rebuilt.
        assertEquals(emptyList(), curriculum.questionLookups)
        assertEquals(service.load().size, service.countUnresolved())
    }

    @Test
    fun countUnresolvedReusesSuppliedHistoryInsteadOfReadingItAgain() = runTest {
        val repository = HistoryRepository(
            listOf(attempt("a1", "2026-08-29T10:00:00Z", "q1" to false)),
        )
        val service = MistakeReviewService(
            assessmentRepository = repository,
            assessmentReviewLoader = AssessmentReviewLoader(
                RecordingCurriculumRepository(defaultQuestions()),
            ),
        )

        val supplied = repository.getCompletedAttempts()
        val readsAfterSupplying = repository.readCount

        assertEquals(1, service.countUnresolved(supplied))
        // The progress dashboard hands over history it already holds; re-reading it there would
        // make the dashboard rebuild the whole attempt history a third time on every resume.
        assertEquals(readsAfterSupplying, repository.readCount)
    }

    @Test
    fun historyOrderIsConsumedAsGivenWithoutReSorting() = runTest {
        val repository = HistoryRepository(
            listOf(
                attempt("newest", "2026-08-29T12:00:00Z", "q2" to false),
                attempt("oldest", "2026-08-29T10:00:00Z", "q1" to false),
            ),
        )
        val service = MistakeReviewService(repository, AssessmentReviewLoader(RecordingCurriculumRepository(defaultQuestions())))

        assertEquals(listOf("q2", "q1"), service.load().map { it.questionId })
        assertTrue(repository.readCount == 1, "History should be read once per load.")
    }
}

private fun service(
    attempts: List<TestAttempt>,
    questions: List<Question> = defaultQuestions(),
): MistakeReviewService =
    MistakeReviewService(
        assessmentRepository = HistoryRepository(attempts),
        assessmentReviewLoader = AssessmentReviewLoader(RecordingCurriculumRepository(questions)),
    )

private fun defaultQuestions(): List<Question> =
    listOf(question("q1"), question("q2"), question("q3"))

private fun question(
    id: String,
    status: ContentStatus = ContentStatus.ACTIVE,
): Question =
    Question(
        id = id,
        topicId = "kotlin",
        subtopicId = "coroutines",
        text = "Question $id",
        answers = listOf(AnswerOption("${id}_a", "Answer A"), AnswerOption("${id}_b", "Answer B")),
        selectionMode = AnswerSelectionMode.SINGLE,
        level = QuestionLevel.FOUNDATION,
        correctAnswerIds = listOf("${id}_a"),
        explanation = "Explanation $id",
        sources = listOf(SourceReference("Source $id", "https://example.com/$id")),
        status = status,
    )

/**
 * Builds a completed attempt. [answers] gives correctness per question in persisted order;
 * [selections] and [correctness] override selected answer IDs and persisted correctness.
 */
private fun attempt(
    id: String,
    completedAt: String,
    vararg answers: Pair<String, Boolean>,
    config: AssessmentConfig = AssessmentConfig.Mixed(maxOf(answers.size, 1)),
    selections: Map<String, Set<String>> = emptyMap(),
    correctness: Map<String, Boolean> = emptyMap(),
): TestAttempt {
    val questionIds = if (answers.isNotEmpty()) answers.map { it.first } else selections.keys.toList()
    val correctById = answers.toMap() + correctness
    val questionAttempts = questionIds.map { questionId ->
        QuestionAttempt(
            questionId,
            QuestionAnswerState.Answered(
                selectedAnswerIds = selections[questionId] ?: setOf("${questionId}_b"),
                isCorrect = correctById[questionId] ?: false,
            ),
        )
    }
    return TestAttempt(
        id = id,
        config = config,
        questionAttempts = questionAttempts,
        status = AssessmentStatus.COMPLETED,
        startedAt = Instant.parse("2026-08-29T09:00:00Z"),
        completedAt = Instant.parse(completedAt),
        score = AssessmentScore(
            totalQuestions = questionAttempts.size,
            correctAnswers = questionAttempts.count {
                (it.answerState as QuestionAnswerState.Answered).isCorrect
            },
        ),
    )
}

private class HistoryRepository(
    private val attempts: List<TestAttempt>,
) : AssessmentRepository {
    var readCount = 0

    override suspend fun save(attempt: TestAttempt) = Unit

    override suspend fun getById(attemptId: String): TestAttempt? =
        attempts.firstOrNull { it.id == attemptId }

    override suspend fun getCompletedAttempts(): List<TestAttempt> {
        readCount += 1
        return attempts
    }
}

private class RecordingCurriculumRepository(
    questions: List<Question>,
) : CurriculumRepository {
    private val questionsById = questions.associateBy(Question::id)
    val questionLookups = mutableListOf<String>()

    override suspend fun getActiveTopics(): List<Topic> = error("ACTIVE lookup must not be used.")
    override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestions(): List<Question> = error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getTopicById(topicId: String): Topic? = error("Topic lookup is not needed.")
    override suspend fun getSubtopicById(subtopicId: String): Subtopic? =
        error("Subtopic lookup is not needed.")

    override suspend fun getQuestionById(questionId: String): Question? {
        questionLookups += questionId
        return questionsById[questionId]
    }
}
