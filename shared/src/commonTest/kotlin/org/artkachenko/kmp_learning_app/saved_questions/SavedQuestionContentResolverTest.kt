package org.artkachenko.kmp_learning_app.saved_questions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

internal class SavedQuestionContentResolverTest {
    @Test
    fun preservesTheRepositoryOrderWhateverEachIdentityResolvesTo() = runTest {
        val saved = listOf(
            savedQuestion("q3", savedAt = 300),
            savedQuestion("q2", savedAt = 200),
            savedQuestion("q1", savedAt = 100),
        )
        val repository = FakeContentRepository(
            listOf(
                question("q1", status = ContentStatus.DEPRECATED),
                question("q3"),
            ),
        )

        val items = SavedQuestionContentResolver(repository).resolve(saved)

        // Available, missing, and deprecated in the order they were saved: nothing is grouped by
        // resolution outcome, alphabetised, or re-sorted into curriculum order.
        assertEquals(listOf("q3", "q2", "q1"), items.map { it.questionId })
        assertIs<SavedQuestionItem.Available>(items[0])
        assertIs<SavedQuestionItem.Missing>(items[1])
        assertIs<SavedQuestionItem.Available>(items[2])
    }

    @Test
    fun activeContentIsMappedWithoutAnyAttemptSemantics() = runTest {
        val repository = FakeContentRepository(listOf(question("q1")))

        val item = assertIs<SavedQuestionItem.Available>(
            SavedQuestionContentResolver(repository).resolve(listOf(savedQuestion("q1"))).single(),
        )

        assertEquals("q1", item.savedQuestion.questionId)
        assertEquals("Question q1", item.question.text)
        // Every authored option, in authored order, with the correct ones identified from the
        // curriculum's own correctAnswerIds and nothing said about a selection.
        assertEquals(listOf("q1_a", "q1_b", "q1_c"), item.question.answers.map { it.id })
        assertEquals(
            listOf(true, false, true),
            item.question.answers.map { it.isCorrectAnswer },
        )
        assertEquals("Explanation q1", item.question.explanation)
        assertEquals(listOf("Source B", "Source A"), item.question.sources.map { it.title })
        assertEquals(
            listOf("https://example.com/q1/b", "https://example.com/q1/a"),
            item.question.sources.map { it.url },
        )
    }

    @Test
    fun deprecatedContentRemainsReviewable() = runTest {
        val repository = FakeContentRepository(
            listOf(question("q2", status = ContentStatus.DEPRECATED)),
        )

        val item = assertIs<SavedQuestionItem.Available>(
            SavedQuestionContentResolver(repository).resolve(listOf(savedQuestion("q2"))).single(),
        )

        // A saved identity outlives the Question's place in the current catalogue: retired content
        // is still content the learner deliberately kept, so it is neither filtered nor downgraded
        // to a missing placeholder.
        assertEquals("Question q2", item.question.text)
        assertTrue(item.question.answers.isNotEmpty())
    }

    @Test
    fun contentTheCurriculumNoLongerHoldsBecomesMissingAndKeepsItsSavedIdentity() = runTest {
        val saved = savedQuestion("q_old", savedAt = 42)
        val repository = FakeContentRepository(emptyList())

        val item = assertIs<SavedQuestionItem.Missing>(
            SavedQuestionContentResolver(repository).resolve(listOf(saved)).single(),
        )

        assertEquals(saved, item.savedQuestion)
        assertEquals(42, item.savedQuestion.savedAtEpochMillis)
    }

    /**
     * The distinction the screen depends on: a lookup returning null means the Question is gone,
     * while a lookup that fails means the curriculum could not be read. Turning the second into a
     * list of "no longer available" placeholders would tell the learner their saved Questions had
     * been retired when the database had simply not answered.
     */
    @Test
    fun aFailingLookupIsAnErrorRatherThanMissingContent() = runTest {
        val repository = FakeContentRepository(listOf(question("q1")), failingIds = setOf("q2"))

        assertFailsWith<IllegalStateException> {
            SavedQuestionContentResolver(repository)
                .resolve(listOf(savedQuestion("q1"), savedQuestion("q2")))
        }
    }

    @Test
    fun anEmptySavedListResolvesToNoItemsWithoutTouchingTheCurriculum() = runTest {
        val repository = FakeContentRepository(emptyList())

        assertEquals(emptyList(), SavedQuestionContentResolver(repository).resolve(emptyList()))
        assertEquals(0, repository.lookups)
    }
}

private fun savedQuestion(questionId: String, savedAt: Long = 1_000): SavedQuestion =
    SavedQuestion(questionId = questionId, savedAtEpochMillis = savedAt)

private fun question(
    id: String,
    status: ContentStatus = ContentStatus.ACTIVE,
): Question =
    Question(
        id = id,
        topicId = "kotlin",
        subtopicId = "coroutines",
        text = "Question $id",
        answers = listOf(
            AnswerOption("${id}_a", "Answer A"),
            AnswerOption("${id}_b", "Answer B"),
            AnswerOption("${id}_c", "Answer C"),
        ),
        selectionMode = AnswerSelectionMode.MULTIPLE,
        level = QuestionLevel.FOUNDATION,
        correctAnswerIds = listOf("${id}_a", "${id}_c"),
        explanation = "Explanation $id",
        sources = listOf(
            SourceReference("Source B", "https://example.com/$id/b"),
            SourceReference("Source A", "https://example.com/$id/a"),
        ),
        status = status,
    )

/**
 * Only the historical resolver answers. Every ACTIVE listing fails the test, because a saved
 * identity must never be resolved through the current catalogue.
 */
private class FakeContentRepository(
    private val questions: List<Question>,
    private val failingIds: Set<String> = emptySet(),
) : CurriculumRepository {
    var lookups = 0
        private set

    override suspend fun getQuestionById(questionId: String): Question? {
        lookups += 1
        if (questionId in failingIds) error("Curriculum unavailable.")
        return questions.firstOrNull { it.id == questionId }
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
