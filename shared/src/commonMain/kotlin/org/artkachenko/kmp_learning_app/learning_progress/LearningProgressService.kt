package org.artkachenko.kmp_learning_app.learning_progress

import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

internal class LearningProgressService(
    private val assessmentRepository: AssessmentRepository,
    private val curriculumRepository: CurriculumRepository,
) {
    /**
     * [completedAttempts] lets a caller that already holds newest-first completed history reuse it,
     * as [MistakeReviewService] does, so the shared cache is read once per derivation rather than
     * once per consumer.
     */
    suspend fun load(completedAttempts: List<TestAttempt>? = null): LearningProgressSnapshot {
        val completedAttempts =
            (completedAttempts ?: assessmentRepository.getCompletedAttempts())
                .filter { it.status == AssessmentStatus.COMPLETED }
        val answeredQuestionCount = completedAttempts.sumOf { requireNotNull(it.score).totalQuestions }
        val correctAnswerCount = completedAttempts.sumOf { requireNotNull(it.score).correctAnswers }
        val topicCounts = mutableMapOf<String, Counts>()
        val subtopicCounts = mutableMapOf<SubtopicKey, Counts>()
        val questionsById = mutableMapOf<String, Question?>()

        for (attempt in completedAttempts) {
            for (questionAttempt in attempt.questionAttempts) {
                val question = questionsById.getOrLoad(questionAttempt.questionId) {
                    curriculumRepository.getQuestionById(questionAttempt.questionId)
                } ?: continue
                val isCorrect =
                    (questionAttempt.answerState as QuestionAnswerState.Answered).isCorrect

                topicCounts.getOrPut(question.topicId, ::Counts).add(isCorrect)
                subtopicCounts.getOrPut(
                    SubtopicKey(
                        topicId = question.topicId,
                        subtopicId = question.subtopicId,
                    ),
                    ::Counts,
                ).add(isCorrect)
            }
        }

        val topicsById = mutableMapOf<String, Topic?>()
        val topics = topicCounts.keys.sorted().map { topicId ->
            val counts = topicCounts.getValue(topicId)
            val percentage = percentage(counts.correct, counts.answered)
            val topic = curriculumRepository.getTopicById(topicId)
            topicsById[topicId] = topic
            TopicPerformance(
                topicId = topicId,
                topicName = topic?.name,
                answeredCount = counts.answered,
                correctCount = counts.correct,
                percentage = percentage,
                isWeak = LearningProgressPolicy.isWeakTopic(counts.answered, percentage),
            )
        }

        val subtopicsById = mutableMapOf<String, Subtopic?>()
        val subtopics = subtopicCounts.keys
            .sortedWith(compareBy(SubtopicKey::topicId, SubtopicKey::subtopicId))
            .map { key ->
                val counts = subtopicCounts.getValue(key)
                val percentage = percentage(counts.correct, counts.answered)
                val subtopic = subtopicsById.getOrLoad(key.subtopicId) {
                    curriculumRepository.getSubtopicById(key.subtopicId)
                }
                SubtopicPerformance(
                    subtopicId = key.subtopicId,
                    subtopicName = subtopic?.name,
                    topicId = key.topicId,
                    topicName = topicsById.getValue(key.topicId)?.name,
                    answeredCount = counts.answered,
                    correctCount = counts.correct,
                    percentage = percentage,
                    isWeak = LearningProgressPolicy.isWeakSubtopic(counts.answered, percentage),
                )
            }

        val weakAreas = buildList {
            topics.filter(TopicPerformance::isWeak).forEach { add(WeakArea.Topic(it)) }
            subtopics.filter(SubtopicPerformance::isWeak).forEach { add(WeakArea.Subtopic(it)) }
        }.sortedWith(
            compareBy<WeakArea> { it.percentage }
                .thenByDescending { it.answeredCount }
                .thenBy(::stableSortKey),
        )

        return LearningProgressSnapshot(
            completedAttemptCount = completedAttempts.size,
            answeredQuestionCount = answeredQuestionCount,
            correctAnswerCount = correctAnswerCount,
            percentage = percentage(correctAnswerCount, answeredQuestionCount),
            topics = topics,
            subtopics = subtopics,
            weakAreas = weakAreas,
        )
    }
}

private data class Counts(
    var answered: Int = 0,
    var correct: Int = 0,
) {
    fun add(isCorrect: Boolean) {
        answered++
        if (isCorrect) correct++
    }
}

private data class SubtopicKey(
    val topicId: String,
    val subtopicId: String,
)

private suspend fun <K, V> MutableMap<K, V?>.getOrLoad(
    key: K,
    load: suspend () -> V?,
): V? {
    if (containsKey(key)) return this[key]
    return load().also { this[key] = it }
}

private fun percentage(
    correctCount: Int,
    answeredCount: Int,
): Double =
    if (answeredCount == 0) {
        0.0
    } else {
        correctCount.toDouble() / answeredCount * 100.0
    }

private fun stableSortKey(area: WeakArea): String =
    when (area) {
        is WeakArea.Topic -> "topic:${area.performance.topicId}"
        is WeakArea.Subtopic ->
            "subtopic:${area.performance.topicId}:${area.performance.subtopicId}"
    }
