package org.artkachenko.kmp_learning_app.assessment.selection

import org.artkachenko.kmp_learning_app.assessment.AllQuestionLevels
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class AssessmentQuestionSelectorTest {
    @Test
    fun focusedTopicSelectionUsesLevelAwareTopicPathAndTakesRequestedCount() = runSelectorTest {
        repository.topicQuestions = mapOf(
            "android_ui" to questions("ui_question_a", "ui_question_b", "ui_question_c"),
        )

        val selected = selector().selectQuestions(
            AssessmentConfig.Focused(
                scope = AssessmentScope.Topic("android_ui"),
                questionCount = 2,
            ),
        )

        assertEquals(
            listOf("topic:android_ui levels:FOUNDATION,APPLIED,ADVANCED"),
            repository.calls,
        )
        assertEquals(listOf("ui_question_a", "ui_question_b"), selected.map { it.id })
        assertUniqueQuestionIds(selected)
    }

    @Test
    fun focusedSubtopicSelectionUsesLevelAwareSubtopicPath() = runSelectorTest {
        repository.subtopicQuestions = mapOf(
            "compose_state" to questions("compose_state_001", "compose_state_002"),
        )

        val selected = selector().selectQuestions(
            AssessmentConfig.Focused(
                scope = AssessmentScope.Subtopic("compose_state"),
                questionCount = 2,
            ),
        )

        assertEquals(
            listOf("subtopic:compose_state levels:FOUNDATION,APPLIED,ADVANCED"),
            repository.calls,
        )
        assertEquals(listOf("compose_state_001", "compose_state_002"), selected.map { it.id })
    }

    @Test
    fun topicPracticeWithOneLevelSelectsOnlyThatLevel() = runSelectorTest {
        repository.topicQuestions = mapOf("android_ui" to leveledTopicFixture())

        val selected = selector().selectQuestions(
            AssessmentConfig.Focused(
                scope = AssessmentScope.Topic("android_ui"),
                questionCount = 10,
                levels = setOf(QuestionLevel.ADVANCED),
            ),
        )

        assertEquals(listOf("topic:android_ui levels:ADVANCED"), repository.calls)
        assertEquals(listOf("advanced_a", "advanced_b"), selected.map { it.id })
    }

    @Test
    fun topicPracticeWithMultipleLevelsSelectsEitherLevel() = runSelectorTest {
        repository.topicQuestions = mapOf("android_ui" to leveledTopicFixture())

        val selected = selector().selectQuestions(
            AssessmentConfig.Focused(
                scope = AssessmentScope.Topic("android_ui"),
                questionCount = 10,
                levels = setOf(QuestionLevel.FOUNDATION, QuestionLevel.ADVANCED),
            ),
        )

        assertEquals(listOf("topic:android_ui levels:FOUNDATION,ADVANCED"), repository.calls)
        assertEquals(listOf("foundation_a", "advanced_a", "advanced_b"), selected.map { it.id })
    }

    @Test
    fun subtopicPracticeWithOneLevelSelectsOnlyThatLevel() = runSelectorTest {
        repository.subtopicQuestions = mapOf("compose_state" to leveledSubtopicFixture())

        val selected = selector().selectQuestions(
            AssessmentConfig.Focused(
                scope = AssessmentScope.Subtopic("compose_state"),
                questionCount = 10,
                levels = setOf(QuestionLevel.APPLIED),
            ),
        )

        assertEquals(listOf("subtopic:compose_state levels:APPLIED"), repository.calls)
        assertEquals(listOf("applied_a"), selected.map { it.id })
    }

    @Test
    fun subtopicPracticeWithMultipleLevelsSelectsEitherLevel() = runSelectorTest {
        repository.subtopicQuestions = mapOf("compose_state" to leveledSubtopicFixture())

        val selected = selector().selectQuestions(
            AssessmentConfig.Focused(
                scope = AssessmentScope.Subtopic("compose_state"),
                questionCount = 10,
                levels = setOf(QuestionLevel.FOUNDATION, QuestionLevel.APPLIED),
            ),
        )

        assertEquals(listOf("subtopic:compose_state levels:FOUNDATION,APPLIED"), repository.calls)
        assertEquals(listOf("foundation_a", "applied_a"), selected.map { it.id })
    }

    @Test
    fun practiceRequestLargerThanEligiblePoolReturnsAvailableUniqueQuestions() = runSelectorTest {
        repository.topicQuestions = mapOf(
            "android_ui" to questions("ui_question_a", "ui_question_b"),
        )

        val selected = selector().selectQuestions(
            AssessmentConfig.Focused(
                scope = AssessmentScope.Topic("android_ui"),
                questionCount = 10,
            ),
        )

        assertEquals(listOf("ui_question_a", "ui_question_b"), selected.map { it.id })
        assertUniqueQuestionIds(selected)
    }

    @Test
    fun practiceSelectionRandomizesBeforeTakingRequestedCount() = runSelectorTest {
        repository.topicQuestions = mapOf(
            "android_ui" to questions("ui_question_a", "ui_question_b", "ui_question_c"),
        )

        val selected = selector(randomize = { it.reversed() }).selectQuestions(
            AssessmentConfig.Focused(
                scope = AssessmentScope.Topic("android_ui"),
                questionCount = 2,
            ),
        )

        assertEquals(listOf("ui_question_c", "ui_question_b"), selected.map { it.id })
    }

    @Test
    fun practiceSelectionCannotRepeatAQuestionId() = runSelectorTest {
        repository.topicQuestions = mapOf(
            "android_ui" to listOf(
                question("ui_question_a", text = "First copy"),
                question("ui_question_b"),
                question("ui_question_a", text = "Duplicate copy"),
            ),
        )

        val selected = selector(
            randomize = { listOf(it[0], it[0], it[1]) },
        ).selectQuestions(
            AssessmentConfig.Focused(
                scope = AssessmentScope.Topic("android_ui"),
                questionCount = 10,
            ),
        )

        assertEquals(listOf("ui_question_a", "ui_question_b"), selected.map { it.id })
        assertEquals("First copy?", selected.first().text)
        assertUniqueQuestionIds(selected)
    }

    /**
     * ACTIVE eligibility belongs to the repository. The selector only ever asks the `getActive*`
     * reads — every other read in the fake fails the test — so retired content cannot reach a new
     * assessment through this path.
     */
    @Test
    fun practiceSelectionReadsEligibleQuestionsThroughTheActiveRepositoryBoundary() = runSelectorTest {
        repository.topicQuestions = mapOf(
            "android_ui" to listOf(
                question("active_question"),
                question("retired_question").copy(status = ContentStatus.DEPRECATED),
            ),
        )

        val selected = selector().selectQuestions(
            AssessmentConfig.Focused(
                scope = AssessmentScope.Topic("android_ui"),
                questionCount = 10,
            ),
        )

        assertEquals(listOf("active_question"), selected.map { it.id })
    }

    @Test
    fun practiceWithoutSelectedLevelsIsExplicitlyNonRunnable() = runSelectorTest {
        repository.topicQuestions = mapOf(
            "android_ui" to questions("ui_question_a", "ui_question_b"),
        )

        val result = selector().select(
            AssessmentConfig.Focused(
                scope = AssessmentScope.Topic("android_ui"),
                questionCount = 10,
                levels = emptySet(),
            ),
        )

        assertEquals(AssessmentSelectionResult.NoContent.NoLevelsSelected, result)
        assertEquals(emptyList(), repository.calls)
    }

    @Test
    fun practiceWithNoQuestionsMatchingScopeAndLevelsReportsNoEligibleQuestions() = runSelectorTest {
        repository.topicQuestions = mapOf("android_ui" to leveledTopicFixture())

        val result = selector().select(
            AssessmentConfig.Focused(
                scope = AssessmentScope.Topic("android_ui"),
                questionCount = 10,
                levels = setOf(QuestionLevel.APPLIED),
            ),
        )

        assertEquals(AssessmentSelectionResult.NoContent.NoEligibleQuestions, result)
    }

    /**
     * The Practice Builder disables a source using [AssessmentQuestionSelector.isSourceSupported]
     * rather than by attempting a selection, so that answer has to be the same one selection would
     * give. Without this, a source could become selectable in the UI and still be refused on Start,
     * or stay disabled after its policy landed.
     */
    @Test
    fun reportedSourceSupportMatchesWhatSelectionActuallyDoes() = runSelectorTest {
        PracticeQuestionSource.entries.forEach { source ->
            repository.topicQuestions = mapOf(
                "android_ui" to listOf(question("ui_question_a")),
            )
            val selector = selector()
            val result = selector.select(
                AssessmentConfig.Focused(
                    scope = AssessmentScope.Topic("android_ui"),
                    questionCount = 1,
                    levels = AllQuestionLevels,
                    source = source,
                ),
            )

            val selectionSupportsIt = result != AssessmentSelectionResult.NoContent.SourceNotSupported
            assertEquals(
                selectionSupportsIt,
                selector.isSourceSupported(source),
                "isSourceSupported disagrees with select() for $source",
            )
        }
    }

    @Test
    fun unseenSourceIsNotSelectableYetAndDoesNotFallBackToAll() = runSelectorTest {
        assertSourceIsNotSupportedYet(PracticeQuestionSource.UNSEEN)
    }

    @Test
    fun weakAreasSourceIsNotSelectableYetAndDoesNotFallBackToAll() = runSelectorTest {
        assertSourceIsNotSupportedYet(PracticeQuestionSource.WEAK_AREAS)
    }

    @Test
    fun unresolvedMistakesSourceIsNotSelectableYetAndDoesNotFallBackToAll() = runSelectorTest {
        assertSourceIsNotSupportedYet(PracticeQuestionSource.UNRESOLVED_MISTAKES)
    }

    @Test
    fun mixedSelectionUsesAllActiveRepositoryPath() = runSelectorTest {
        repository.activeQuestions = questions(
            "android_question",
            "kotlin_question",
            "testing_question",
        )

        val selected = selector().selectQuestions(
            AssessmentConfig.Mixed(questionCount = 2),
        )

        assertEquals(listOf("all"), repository.calls)
        assertEquals(listOf("android_question", "kotlin_question"), selected.map { it.id })
    }

    @Test
    fun questionLevelMetadataDoesNotChangeMixedSelection() = runSelectorTest {
        repository.activeQuestions = listOf(
            question("foundation").copy(level = QuestionLevel.FOUNDATION),
            question("applied").copy(level = QuestionLevel.APPLIED),
            question("advanced").copy(level = QuestionLevel.ADVANCED),
        )

        val selected = selector().selectQuestions(AssessmentConfig.Mixed(questionCount = 3))

        assertEquals(listOf("foundation", "applied", "advanced"), selected.map { it.id })
        assertEquals(QuestionLevel.entries, selected.map { it.level })
    }

    @Test
    fun mixedFirstRoundCoversTopicsBeforeRepeatingOne() = runSelectorTest {
        repository.activeQuestions = mixedRoundFixture()

        val selected = selector().selectQuestions(AssessmentConfig.Mixed(questionCount = 3))

        assertEquals(listOf("A1", "B1", "C1"), selected.map { it.id })
    }

    @Test
    fun mixedSecondRoundContinuesInTopicEncounterOrder() = runSelectorTest {
        repository.activeQuestions = mixedRoundFixture()

        val selected = selector().selectQuestions(AssessmentConfig.Mixed(questionCount = 5))

        assertEquals(listOf("A1", "B1", "C1", "A2", "B2"), selected.map { it.id })
    }

    @Test
    fun mixedOversizedRequestReturnsAllUniqueQuestionsInCoverageOrder() = runSelectorTest {
        repository.activeQuestions = mixedRoundFixture()

        val selected = selector().selectQuestions(AssessmentConfig.Mixed(questionCount = 10))

        assertEquals(listOf("A1", "B1", "C1", "A2", "B2", "A3"), selected.map { it.id })
        assertUniqueQuestionIds(selected)
    }

    @Test
    fun mixedRequestSmallerThanTopicCountUsesDistinctTopics() = runSelectorTest {
        repository.activeQuestions = listOf(
            question("A1", topicId = "A"),
            question("B1", topicId = "B"),
            question("C1", topicId = "C"),
            question("D1", topicId = "D"),
        )

        val selected = selector().selectQuestions(AssessmentConfig.Mixed(questionCount = 2))

        assertEquals(listOf("A1", "B1"), selected.map { it.id })
        assertEquals(2, selected.map { it.topicId }.toSet().size)
    }

    @Test
    fun mixedSelectionSkipsExhaustedTopicsAcrossLaterRounds() = runSelectorTest {
        repository.activeQuestions = listOf(
            question("A1", topicId = "A"),
            question("B1", topicId = "B"),
            question("B2", topicId = "B"),
            question("B3", topicId = "B"),
            question("C1", topicId = "C"),
            question("C2", topicId = "C"),
        )

        val selected = selector().selectQuestions(AssessmentConfig.Mixed(questionCount = 6))

        assertEquals(listOf("A1", "B1", "C1", "B2", "C2", "B3"), selected.map { it.id })
    }

    @Test
    fun requestedCountEqualToPoolReturnsWholeUniquePool() = runSelectorTest {
        repository.activeQuestions = questions("question_a", "question_b", "question_c")

        val selected = selector().selectQuestions(AssessmentConfig.Mixed(questionCount = 3))

        assertEquals(listOf("question_a", "question_b", "question_c"), selected.map { it.id })
    }

    @Test
    fun requestedCountBelowPoolReturnsRequestedCount() = runSelectorTest {
        repository.activeQuestions = questions(
            "question_a",
            "question_b",
            "question_c",
            "question_d",
            "question_e",
        )

        val selected = selector().selectQuestions(AssessmentConfig.Mixed(questionCount = 2))

        assertEquals(listOf("question_a", "question_b"), selected.map { it.id })
    }

    @Test
    fun requestedCountAbovePoolReturnsAvailableUniqueQuestions() = runSelectorTest {
        repository.activeQuestions = questions("question_a", "question_b", "question_c", "question_d")

        val selected = selector().selectQuestions(AssessmentConfig.Mixed(questionCount = 10))

        assertEquals(listOf("question_a", "question_b", "question_c", "question_d"), selected.map { it.id })
    }

    @Test
    fun emptyEligiblePoolReportsNoEligibleQuestions() = runSelectorTest {
        repository.activeQuestions = emptyList()

        val result = selector().select(AssessmentConfig.Mixed(questionCount = 10))

        assertEquals(AssessmentSelectionResult.NoContent.NoEligibleQuestions, result)
    }

    @Test
    fun duplicateRepositoryResultsAreDeduplicatedByStableQuestionId() = runSelectorTest {
        repository.activeQuestions = listOf(
            question("question_a", text = "First copy"),
            question("question_b"),
            question("question_a", text = "Duplicate copy"),
        )

        val selected = selector().selectQuestions(AssessmentConfig.Mixed(questionCount = 10))

        assertEquals(listOf("question_a", "question_b"), selected.map { it.id })
        assertEquals("First copy?", selected.first().text)
        assertUniqueQuestionIds(selected)
    }

    @Test
    fun randomizationIsInjectable() = runSelectorTest {
        repository.activeQuestions = listOf(
            question("A1", topicId = "A"),
            question("B1", topicId = "B"),
            question("A2", topicId = "A"),
            question("C1", topicId = "C"),
        )

        val selected = selector(
            randomize = { it.reversed() },
        ).selectQuestions(AssessmentConfig.Mixed(questionCount = 4))

        assertEquals(listOf("C1", "A2", "B1", "A1"), selected.map { it.id })
    }

    @Test
    fun randomizerCannotIntroduceDuplicateSelectedQuestionIds() = runSelectorTest {
        repository.activeQuestions = questions("question_a", "question_b", "question_c")

        val selected = selector(
            randomize = { listOf(it[1], it[1], it[2], it[0]) },
        ).selectQuestions(AssessmentConfig.Mixed(questionCount = 10))

        assertEquals(listOf("question_b", "question_c", "question_a"), selected.map { it.id })
    }

    private fun runSelectorTest(
        block: suspend SelectorTestScope.() -> Unit,
    ) {
        var outcome: Result<Unit>? = null
        block.startCoroutine(
            receiver = SelectorTestScope(),
            completion = object : Continuation<Unit> {
                override val context = EmptyCoroutineContext

                override fun resumeWith(result: Result<Unit>) {
                    outcome = result
                }
            },
        )
        outcome?.getOrThrow()
            ?: error("Selector test did not complete synchronously.")
    }

    private class SelectorTestScope {
        val repository = FakeCurriculumRepository()

        fun selector(
            randomize: (List<Question>) -> List<Question> = { it },
        ): AssessmentQuestionSelector =
            AssessmentQuestionSelector(
                curriculumRepository = repository,
                randomize = randomize,
            )
    }

    private suspend fun AssessmentQuestionSelector.selectQuestions(
        config: AssessmentConfig,
    ): List<Question> {
        val result = select(config)
        assertIs<AssessmentSelectionResult.Selected>(result)
        return result.questions
    }

    private suspend fun SelectorTestScope.assertSourceIsNotSupportedYet(
        source: PracticeQuestionSource,
    ) {
        repository.topicQuestions = mapOf(
            "android_ui" to listOf(question("ui_question_a"), question("ui_question_b")),
        )

        val result = selector().select(
            AssessmentConfig.Focused(
                scope = AssessmentScope.Topic("android_ui"),
                questionCount = 2,
                levels = AllQuestionLevels,
                source = source,
            ),
        )

        assertEquals(AssessmentSelectionResult.NoContent.SourceNotSupported, result)
        // No curriculum read at all: an unimplemented source must not quietly become ALL.
        assertEquals(emptyList(), repository.calls)
    }

    /**
     * Mirrors the repository contract the selector depends on: only ACTIVE content is returned,
     * and several levels mean inclusive OR. The unfiltered scope reads fail, because targeted
     * practice must not load a whole scope and filter levels above the repository.
     */
    private class FakeCurriculumRepository : CurriculumRepository {
        var activeQuestions: List<Question> = emptyList()
        var topicQuestions: Map<String, List<Question>> = emptyMap()
        var subtopicQuestions: Map<String, List<Question>> = emptyMap()
        val calls = mutableListOf<String>()

        override suspend fun getActiveTopics(): List<Topic> =
            error("Not used by AssessmentQuestionSelector.")

        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> =
            error("Not used by AssessmentQuestionSelector.")

        override suspend fun getActiveQuestions(): List<Question> {
            calls += "all"
            return activeQuestions.filter { it.status == ContentStatus.ACTIVE }
        }

        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> =
            error("Targeted practice must use the level-aware topic read.")

        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
            error("Targeted practice must use the level-aware subtopic read.")

        override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> =
            error("Not used by AssessmentQuestionSelector.")

        override suspend fun getActiveQuestionsByTopicAndLevels(
            topicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> {
            calls += "topic:$topicId levels:${levels.describe()}"
            return topicQuestions[topicId].orEmpty().filterEligible(levels)
        }

        override suspend fun getActiveQuestionsBySubtopicAndLevels(
            subtopicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> {
            calls += "subtopic:$subtopicId levels:${levels.describe()}"
            return subtopicQuestions[subtopicId].orEmpty().filterEligible(levels)
        }

        override suspend fun getTopicById(topicId: String): Topic? =
            error("Not used by AssessmentQuestionSelector.")

        override suspend fun getSubtopicById(subtopicId: String): Subtopic? =
            error("Not used by AssessmentQuestionSelector.")

        override suspend fun getQuestionById(questionId: String): Question? =
            error("Not used by AssessmentQuestionSelector.")

        private fun List<Question>.filterEligible(levels: Set<QuestionLevel>): List<Question> =
            filter { it.status == ContentStatus.ACTIVE && it.level in levels }

        private fun Set<QuestionLevel>.describe(): String =
            QuestionLevel.entries.filter { it in this }.joinToString(",")
    }

    private fun questions(vararg ids: String): List<Question> =
        ids.map { question(it) }

    private fun leveledTopicFixture(): List<Question> =
        listOf(
            question("foundation_a").copy(level = QuestionLevel.FOUNDATION),
            question("advanced_a").copy(level = QuestionLevel.ADVANCED),
            question("advanced_b").copy(level = QuestionLevel.ADVANCED),
        )

    private fun leveledSubtopicFixture(): List<Question> =
        listOf(
            question("foundation_a").copy(level = QuestionLevel.FOUNDATION),
            question("applied_a").copy(level = QuestionLevel.APPLIED),
            question("advanced_a").copy(level = QuestionLevel.ADVANCED),
        )

    private fun mixedRoundFixture(): List<Question> =
        listOf(
            question("A1", topicId = "A"),
            question("A2", topicId = "A"),
            question("B1", topicId = "B"),
            question("C1", topicId = "C"),
            question("B2", topicId = "B"),
            question("A3", topicId = "A"),
        )

    private fun question(
        id: String,
        topicId: String = "${id}_topic",
        text: String = id,
    ): Question =
        Question(
            id = id,
            topicId = topicId,
            subtopicId = "${id}_subtopic",
            text = "$text?",
            answers = listOf(
                AnswerOption("${id}_answer_a", "Answer A"),
                AnswerOption("${id}_answer_b", "Answer B"),
            ),
            selectionMode = AnswerSelectionMode.SINGLE,
            level = QuestionLevel.FOUNDATION,
            correctAnswerIds = listOf("${id}_answer_a"),
            explanation = "$id explanation.",
            sources = listOf(
                SourceReference(
                    title = "$id source",
                    url = "https://example.com/$id",
                ),
            ),
            status = ContentStatus.ACTIVE,
        )

    private fun assertUniqueQuestionIds(questions: List<Question>) {
        assertEquals(
            questions.size,
            questions.map { it.id }.toSet().size,
        )
    }
}
