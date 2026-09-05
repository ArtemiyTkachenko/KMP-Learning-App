package org.artkachenko.kmp_learning_app.topic_study.practice_builder

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningUnit
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.topic_study.FakeLearningContentRepository
import org.artkachenko.kmp_learning_app.topic_study.testLearningLesson
import org.artkachenko.kmp_learning_app.topic_study.testLearningUnit

/**
 * The one crossing from learning content into assessment configuration.
 *
 * These are the authoring rules that decide what a Unit is responsible for assessing, and they are
 * asserted here rather than through the builder because they are content rules, not UI ones: a
 * regression in any of them quietly changes what learners are quizzed on without breaking a screen.
 */
internal class PracticeTargetResolverTest {
    /**
     * The core derivation, with every rule visible at once: a primary concept named by two Lessons
     * appears once, a supporting concept never appears, and a deprecated Lesson contributes
     * nothing even though the Unit itself is current.
     */
    @Test
    fun aUnitScopeIsTheDeduplicatedPrimaryConceptsOfItsActiveLessons() = runTest {
        val unit = testLearningUnit(
            id = "unit_a",
            lessons = listOf(
                testLearningLesson(
                    id = "lesson_a",
                    primarySubtopicIds = listOf("a", "b"),
                    supportingSubtopicIds = listOf("x"),
                ),
                testLearningLesson(
                    id = "lesson_b",
                    primarySubtopicIds = listOf("b", "c"),
                    supportingSubtopicIds = listOf("y"),
                ),
                testLearningLesson(
                    id = "lesson_c",
                    status = ContentStatus.DEPRECATED,
                    primarySubtopicIds = listOf("d"),
                    supportingSubtopicIds = listOf("z"),
                ),
            ),
        )

        val resolution = resolve(unit)

        assertEquals(AssessmentScope.Subtopics(setOf("a", "b", "c")), resolution.scope)
    }

    /**
     * A Unit may legitimately teach concepts owned by different Topics — that cross-Topic bridging
     * is the point of the learning content — so the Unit's home Topic must never narrow its scope.
     */
    @Test
    fun crossTopicPrimaryConceptsSurviveTheDerivation() = runTest {
        val unit = testLearningUnit(
            id = "unit_state",
            topicId = "topic_compose",
            lessons = listOf(
                testLearningLesson(
                    id = "lesson_state",
                    primarySubtopicIds = listOf("compose_state", "state_ownership"),
                ),
                testLearningLesson(
                    id = "lesson_flows",
                    // Owned by an architecture Topic rather than by the Unit's home Topic.
                    primarySubtopicIds = listOf("flow_stateflow"),
                ),
            ),
        )

        val resolution = resolve(unit)

        assertEquals(
            AssessmentScope.Subtopics(setOf("compose_state", "state_ownership", "flow_stateflow")),
            resolution.scope,
        )
    }

    /** The title comes from the document that was just read, so a re-authored Unit reads currently. */
    @Test
    fun theResolvedTitleIsTheCurrentUnitTitle() = runTest {
        val resolution = resolve(
            testLearningUnit(
                id = "unit_a",
                lessons = listOf(
                    testLearningLesson("lesson_a", primarySubtopicIds = listOf("a")),
                ),
            ),
        )

        assertEquals("Title of unit_a", resolution.name)
    }

    @Test
    fun anUnknownUnitIsUnavailableRatherThanAnEmptyScope() = runTest {
        val resolution = PracticeTargetResolver(
            curriculumRepository = FakeCurriculumRepository(),
            learningContentRepository = FakeLearningContentRepository(),
        ).resolve(PracticeBuilderTarget.LearningUnit("unit_missing"))

        assertEquals(PracticeTargetResolution.Unavailable, resolution)
    }

    /**
     * `getUnitById` resolves retired material on purpose, so the status check has to be here:
     * deprecated study material must not become newly practiceable through a stale route.
     */
    @Test
    fun aDeprecatedUnitIsUnavailableEvenThoughItStillResolvesById() = runTest {
        val unit = testLearningUnit(
            id = "unit_a",
            status = ContentStatus.DEPRECATED,
            lessons = listOf(testLearningLesson("lesson_a", primarySubtopicIds = listOf("a"))),
        )

        val resolution = PracticeTargetResolver(
            curriculumRepository = FakeCurriculumRepository(),
            learningContentRepository = FakeLearningContentRepository(units = listOf(unit)),
        ).resolve(PracticeBuilderTarget.LearningUnit("unit_a"))

        assertEquals(PracticeTargetResolution.Unavailable, resolution)
    }

    /**
     * Reported rather than constructed: `AssessmentScope.Subtopics` requires a non-empty set, and
     * a Unit that currently teaches nothing assessable is a content condition, not a crash.
     */
    @Test
    fun aUnitWithNoActivePrimaryConceptsHasNothingToPractise() = runTest {
        val unit = testLearningUnit(
            id = "unit_a",
            lessons = listOf(
                testLearningLesson("lesson_supporting", supportingSubtopicIds = listOf("a")),
                testLearningLesson(
                    id = "lesson_retired",
                    status = ContentStatus.DEPRECATED,
                    primarySubtopicIds = listOf("b"),
                ),
            ),
        )

        val resolution = PracticeTargetResolver(
            curriculumRepository = FakeCurriculumRepository(),
            learningContentRepository = FakeLearningContentRepository(units = listOf(unit)),
        ).resolve(PracticeBuilderTarget.LearningUnit("unit_a"))

        assertEquals(PracticeTargetResolution.NoPracticeableConcepts, resolution)
    }

    /**
     * A Topic or Subtopic target already knows its scope, so the learning document is not read at
     * all: the fake here fails every call, and resolution still succeeds.
     */
    @Test
    fun topicAndSubtopicTargetsNeverReadLearningContent() = runTest {
        val resolver = PracticeTargetResolver(
            curriculumRepository = FakeCurriculumRepository(),
            learningContentRepository =
                FakeLearningContentRepository(failuresRemaining = Int.MAX_VALUE),
        )

        val topic = assertIs<PracticeTargetResolution.Resolved>(
            resolver.resolve(PracticeBuilderTarget.Topic("topic_a")),
        )
        val subtopic = assertIs<PracticeTargetResolution.Resolved>(
            resolver.resolve(PracticeBuilderTarget.Subtopic("subtopic_a")),
        )

        assertEquals(AssessmentScope.Topic("topic_a"), topic.scope)
        assertEquals("Topic A", topic.name)
        assertEquals(AssessmentScope.Subtopic("subtopic_a"), subtopic.scope)
        assertEquals("Subtopic A", subtopic.name)
    }

    /**
     * The display name is decoration, and losing it must not stop practice: an unreadable
     * curriculum leaves the label absent while the scope — which is what Start depends on —
     * is unchanged. A Unit is the opposite case and is covered at the builder, where an
     * unreadable document has to stay retryable.
     */
    @Test
    fun anUnreadableCurriculumLeavesTheNameAbsentWithoutLosingTheScope() = runTest {
        val resolver = PracticeTargetResolver(
            curriculumRepository = FakeCurriculumRepository(failing = true),
            learningContentRepository = FakeLearningContentRepository(),
        )

        val resolution = assertIs<PracticeTargetResolution.Resolved>(
            resolver.resolve(PracticeBuilderTarget.Topic("topic_a")),
        )

        assertNull(resolution.name)
        assertEquals(AssessmentScope.Topic("topic_a"), resolution.scope)
    }

    private suspend fun resolve(unit: LearningUnit): PracticeTargetResolution.Resolved =
        assertIs<PracticeTargetResolution.Resolved>(
            PracticeTargetResolver(
                curriculumRepository = FakeCurriculumRepository(),
                learningContentRepository = FakeLearningContentRepository(units = listOf(unit)),
            ).resolve(PracticeBuilderTarget.LearningUnit(unit.id)),
        )

    /** Only the two name lookups are used here; nothing in this suite selects Questions. */
    private class FakeCurriculumRepository(
        private val failing: Boolean = false,
    ) : CurriculumRepository {
        override suspend fun getActiveTopics(): List<Topic> = emptyList()

        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> = emptyList()

        override suspend fun getActiveQuestions(): List<Question> = emptyList()

        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> =
            emptyList()

        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
            emptyList()

        override suspend fun getActiveQuestionsByLevels(
            levels: Set<QuestionLevel>,
        ): List<Question> = emptyList()

        override suspend fun getActiveQuestionsByTopicAndLevels(
            topicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = emptyList()

        override suspend fun getActiveQuestionsBySubtopicAndLevels(
            subtopicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = emptyList()

        override suspend fun getTopicById(topicId: String): Topic? {
            if (failing) error("curriculum unavailable")
            return Topic(topicId, "Topic A")
        }

        override suspend fun getSubtopicById(subtopicId: String): Subtopic? {
            if (failing) error("curriculum unavailable")
            return Subtopic(subtopicId, "topic_a", "Subtopic A")
        }

        override suspend fun getQuestionById(questionId: String): Question? = null
    }
}
