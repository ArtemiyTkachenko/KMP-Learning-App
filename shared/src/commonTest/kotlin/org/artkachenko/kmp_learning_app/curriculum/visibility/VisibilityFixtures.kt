package org.artkachenko.kmp_learning_app.curriculum.visibility

import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
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
 * A three-Topic curriculum for visibility tests: two core Topics and the optional `kmp` Topic.
 *
 * Six Questions in each of `android_lifecycle` and `kmp_expect_actual` give either side enough
 * answers to become a weak area (the policy needs five), and every level appears on both sides of
 * the boundary so level-scoped reads have something to hide.
 */
internal object VisibilityFixture {
    val android = Topic("android", "Android")
    val compose = Topic("compose", "Compose")
    val kmp = Topic("kmp", "Kotlin Multiplatform")

    val androidLifecycle = Subtopic("android_lifecycle", android.id, "Lifecycle")
    val composeState = Subtopic("compose_state", compose.id, "State")
    val kmpExpectActual = Subtopic("kmp_expect_actual", kmp.id, "expect / actual")
    val kmpKoin = Subtopic("kmp_koin", kmp.id, "Koin in shared code")

    val androidQuestions = List(6) { index ->
        question("a${index + 1}", androidLifecycle, levelFor(index))
    }
    val composeQuestions = List(3) { index ->
        question("c${index + 1}", composeState, levelFor(index))
    }
    val kmpQuestions = List(6) { index ->
        question("k${index + 1}", kmpExpectActual, levelFor(index))
    } + question("k7", kmpKoin, QuestionLevel.ADVANCED)

    val topics = listOf(android, compose, kmp)
    val subtopics = listOf(androidLifecycle, composeState, kmpExpectActual, kmpKoin)

    /** Authored order interleaves the Topics, so an ordering bug in a filter would show. */
    val questions: List<Question> =
        (androidQuestions.indices).flatMap { index ->
            listOfNotNull(
                androidQuestions[index],
                kmpQuestions.getOrNull(index),
                composeQuestions.getOrNull(index),
            )
        } + kmpQuestions.drop(androidQuestions.size)

    fun visibility(includeKmpContent: Boolean) =
        MutableStateFlow(CurriculumVisibility.from(includeKmpContent))

    private fun levelFor(index: Int): QuestionLevel = QuestionLevel.entries[index % QuestionLevel.entries.size]
}

internal fun question(id: String, subtopic: Subtopic, level: QuestionLevel = QuestionLevel.FOUNDATION) =
    Question(
        id = id,
        topicId = subtopic.topicId,
        subtopicId = subtopic.id,
        text = "Question $id",
        answers = listOf(AnswerOption("${id}_a", "A"), AnswerOption("${id}_b", "B")),
        selectionMode = AnswerSelectionMode.SINGLE,
        level = level,
        correctAnswerIds = listOf("${id}_a"),
        explanation = "Because.",
        sources = listOf(SourceReference("Source", "https://developer.android.com")),
    )

/**
 * The underlying repository a decorator wraps, answering from in-memory lists the way the Room
 * repository answers from tables: ACTIVE-only eligibility, unfiltered identity reads, authored order.
 */
internal class FixtureCurriculumRepository(
    private val topics: List<Topic> = VisibilityFixture.topics,
    private val subtopics: List<Subtopic> = VisibilityFixture.subtopics,
    private val questions: List<Question> = VisibilityFixture.questions,
) : CurriculumRepository {
    /** Every `getQuestionsByIds` call, so a test can assert that resolution is batched. */
    val questionsByIdsCalls = mutableListOf<Set<String>>()

    private val activeQuestions get() = questions.filter { it.status == ContentStatus.ACTIVE }

    override suspend fun getActiveTopics() = topics.filter { it.status == ContentStatus.ACTIVE }

    override suspend fun getActiveSubtopics(topicId: String) =
        subtopics.filter { it.topicId == topicId && it.status == ContentStatus.ACTIVE }

    override suspend fun getActiveQuestions() = activeQuestions

    override suspend fun getActiveQuestionsByTopic(topicId: String) =
        activeQuestions.filter { it.topicId == topicId }

    override suspend fun getActiveQuestionsBySubtopic(subtopicId: String) =
        activeQuestions.filter { it.subtopicId == subtopicId }

    override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>) =
        activeQuestions.filter { it.level in levels }

    override suspend fun getActiveQuestionsByTopicAndLevels(topicId: String, levels: Set<QuestionLevel>) =
        getActiveQuestionsByTopic(topicId).filter { it.level in levels }

    override suspend fun getActiveQuestionsBySubtopicAndLevels(subtopicId: String, levels: Set<QuestionLevel>) =
        getActiveQuestionsBySubtopic(subtopicId).filter { it.level in levels }

    override suspend fun getTopicById(topicId: String) = topics.firstOrNull { it.id == topicId }

    override suspend fun getSubtopicById(subtopicId: String) = subtopics.firstOrNull { it.id == subtopicId }

    override suspend fun getQuestionsByIdsForCurrentContent(
        questionIds: Collection<String>,
    ): Map<String, Question> = questions.filter { it.id in questionIds }.associateBy { it.id }

    override suspend fun getQuestionsByIds(questionIds: Collection<String>): Map<String, Question> {
        questionsByIdsCalls += questionIds.toSet()
        return questions.filter { it.id in questionIds }.associateBy { it.id }
    }
}

/** A completed attempt answering each Question as given, in order, with a consistent score. */
internal fun completedAttempt(
    id: String,
    completedAtSeconds: Long,
    vararg answers: Pair<String, Boolean>,
    config: AssessmentConfig = AssessmentConfig.Mixed(questionCount = answers.size),
): TestAttempt =
    TestAttempt(
        id = id,
        config = config,
        questionAttempts = answers.map { (questionId, isCorrect) ->
            QuestionAttempt(
                questionId = questionId,
                answerState = QuestionAnswerState.Answered(
                    selectedAnswerIds = setOf(if (isCorrect) "${questionId}_a" else "${questionId}_b"),
                    isCorrect = isCorrect,
                ),
            )
        },
        status = AssessmentStatus.COMPLETED,
        startedAt = Instant.fromEpochSeconds(completedAtSeconds - 60),
        completedAt = Instant.fromEpochSeconds(completedAtSeconds),
        score = AssessmentScore(
            totalQuestions = answers.size,
            correctAnswers = answers.count { it.second },
        ),
    )

internal fun focusedOn(topic: Topic, questionCount: Int): AssessmentConfig =
    AssessmentConfig.Focused(scope = AssessmentScope.Topic(topic.id), questionCount = questionCount)
