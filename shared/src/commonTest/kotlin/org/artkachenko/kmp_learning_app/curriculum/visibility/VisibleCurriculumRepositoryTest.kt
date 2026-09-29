package org.artkachenko.kmp_learning_app.curriculum.visibility

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.visibility.VisibilityFixture.android
import org.artkachenko.kmp_learning_app.curriculum.visibility.VisibilityFixture.androidLifecycle
import org.artkachenko.kmp_learning_app.curriculum.visibility.VisibilityFixture.kmp
import org.artkachenko.kmp_learning_app.curriculum.visibility.VisibilityFixture.kmpExpectActual
import org.artkachenko.kmp_learning_app.curriculum.visibility.VisibilityFixture.kmpQuestions

internal class VisibleCurriculumRepositoryTest {

    private val raw = FixtureCurriculumRepository()
    private val visibility = VisibilityFixture.visibility(includeKmpContent = false)
    private val repository = VisibleCurriculumRepository(raw, visibility)

    private val allLevels = QuestionLevel.entries.toSet()
    private val someLevels = setOf(QuestionLevel.FOUNDATION, QuestionLevel.ADVANCED)

    @Test
    fun hiddenTopicIsAbsentFromTheActiveTopicList() = runTest {
        assertEquals(listOf("android", "compose"), repository.getActiveTopics().map { it.id })
    }

    @Test
    fun hiddenTopicHasNoActiveSubtopics() = runTest {
        assertEquals(emptyList(), repository.getActiveSubtopics(kmp.id))
        assertEquals(listOf(androidLifecycle), repository.getActiveSubtopics(android.id))
    }

    /**
     * Every Question-returning eligibility read, each against the raw repository, so a method the
     * decorator forgot to filter fails here by name.
     */
    @Test
    fun everyActiveQuestionReadOmitsHiddenQuestionsAndKeepsTheRestInOrder() = runTest {
        val reads: List<Pair<String, suspend (org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository) -> List<Question>>> = listOf(
            "getActiveQuestions" to { it.getActiveQuestions() },
            "getActiveQuestionsByTopic(android)" to { it.getActiveQuestionsByTopic(android.id) },
            "getActiveQuestionsBySubtopic(android_lifecycle)" to { it.getActiveQuestionsBySubtopic(androidLifecycle.id) },
            "getActiveQuestionsByLevels" to { it.getActiveQuestionsByLevels(someLevels) },
            "getActiveQuestionsByTopicAndLevels(android)" to { it.getActiveQuestionsByTopicAndLevels(android.id, someLevels) },
            "getActiveQuestionsBySubtopicAndLevels(android_lifecycle)" to {
                it.getActiveQuestionsBySubtopicAndLevels(androidLifecycle.id, someLevels)
            },
        )
        reads.forEach { (name, read) ->
            val expected = read(raw).filterNot { it.topicId == kmp.id }
            val actual = read(repository)
            assertTrue(actual.none { it.topicId == kmp.id }, "$name returned a hidden Question")
            assertEquals(expected, actual, "$name should keep every visible Question in order")
        }
        // The global and level-only reads have something to hide in this fixture.
        assertTrue(raw.getActiveQuestions().any { it.topicId == kmp.id })
        assertTrue(raw.getActiveQuestionsByLevels(someLevels).any { it.topicId == kmp.id })
    }

    @Test
    fun readsScopedToTheHiddenTopicOrItsSubtopicsHaveNoEligibleContent() = runTest {
        assertEquals(emptyList(), repository.getActiveQuestionsByTopic(kmp.id))
        assertEquals(emptyList(), repository.getActiveQuestionsByTopicAndLevels(kmp.id, allLevels))
        assertEquals(emptyList(), repository.getActiveQuestionsBySubtopic(kmpExpectActual.id))
        assertEquals(emptyList(), repository.getActiveQuestionsBySubtopicAndLevels(kmpExpectActual.id, allLevels))
    }

    @Test
    fun turningVisibilityOnRestoresEveryActiveReadExactly() = runTest {
        visibility.value = CurriculumVisibility.from(includeKmpContent = true)

        assertEquals(raw.getActiveTopics(), repository.getActiveTopics())
        assertEquals(raw.getActiveSubtopics(kmp.id), repository.getActiveSubtopics(kmp.id))
        assertEquals(raw.getActiveQuestions(), repository.getActiveQuestions())
        assertEquals(raw.getActiveQuestionsByTopic(kmp.id), repository.getActiveQuestionsByTopic(kmp.id))
        assertEquals(
            raw.getActiveQuestionsBySubtopic(kmpExpectActual.id),
            repository.getActiveQuestionsBySubtopic(kmpExpectActual.id),
        )
        assertEquals(raw.getActiveQuestionsByLevels(someLevels), repository.getActiveQuestionsByLevels(someLevels))
        assertEquals(
            raw.getActiveQuestionsByTopicAndLevels(kmp.id, someLevels),
            repository.getActiveQuestionsByTopicAndLevels(kmp.id, someLevels),
        )
        assertEquals(
            raw.getActiveQuestionsBySubtopicAndLevels(kmpExpectActual.id, someLevels),
            repository.getActiveQuestionsBySubtopicAndLevels(kmpExpectActual.id, someLevels),
        )
    }

    /** Visibility is read per call: a change applies to the very next read, in both directions. */
    @Test
    fun aVisibilityChangeAppliesToTheNextRead() = runTest {
        assertTrue(repository.getActiveQuestions().none { it.topicId == kmp.id })
        visibility.value = CurriculumVisibility.from(includeKmpContent = true)
        assertTrue(repository.getActiveQuestions().any { it.topicId == kmp.id })
        visibility.value = CurriculumVisibility.from(includeKmpContent = false)
        assertTrue(repository.getActiveQuestions().none { it.topicId == kmp.id })
    }

    /** Hidden is not missing: stored history must still resolve hidden identities. */
    @Test
    fun identityAndHistoricalReadsStillResolveHiddenContent() = runTest {
        assertEquals(kmp, repository.getTopicById(kmp.id))
        assertEquals(kmpExpectActual, repository.getSubtopicById(kmpExpectActual.id))

        val ids = kmpQuestions.map { it.id } + "a1"
        val resolved = repository.getQuestionsByIds(ids)
        assertEquals(ids.toSet(), resolved.keys)
        assertEquals(kmpQuestions, kmpQuestions.map { assertNotNull(resolved[it.id]) })
    }

    @Test
    fun returnedCoreObjectsAreTheUnderlyingInstancesUnchanged() = runTest {
        val underlying = raw.getActiveQuestions()
        repository.getActiveQuestions().forEach { question ->
            assertSame(underlying.first { it.id == question.id }, question)
        }
        assertSame(raw.getActiveTopics().first(), repository.getActiveTopics().first())
    }
}
