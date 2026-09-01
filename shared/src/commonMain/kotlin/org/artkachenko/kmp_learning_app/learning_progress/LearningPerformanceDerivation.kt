package org.artkachenko.kmp_learning_app.learning_progress

import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

/**
 * The single derivation of all-time Topic/Subtopic performance and weak areas.
 *
 * Progress needs the complete performance models while weak-area practice needs only the weak
 * identities. Keeping both consumers on this component prevents practice eligibility from drifting
 * away from the evidence, threshold, or occurrence semantics shown on Progress.
 */
internal class LearningPerformanceDerivation(
    private val curriculumRepository: CurriculumRepository,
) {
    suspend fun derive(attempts: List<TestAttempt>): LearningPerformanceSnapshot {
        val completedAttempts = attempts.filter { it.status == AssessmentStatus.COMPLETED }
        val topicCounts = mutableMapOf<String, Counts>()
        val subtopicCounts = mutableMapOf<PerformanceSubtopicKey, Counts>()
        val questionsById = mutableMapOf<String, Question?>()

        for (attempt in completedAttempts) {
            for (questionAttempt in attempt.questionAttempts) {
                val question = questionsById.getOrLoad(questionAttempt.questionId) {
                    curriculumRepository.getQuestionById(questionAttempt.questionId)
                } ?: continue
                val isCorrect = answeredCorrectly(questionAttempt)

                topicCounts.getOrPut(question.topicId, ::Counts).add(isCorrect)
                subtopicCounts.getOrPut(
                    PerformanceSubtopicKey(
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
            .sortedWith(compareBy(PerformanceSubtopicKey::topicId, PerformanceSubtopicKey::subtopicId))
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

        return LearningPerformanceSnapshot(
            topics = topics,
            subtopics = subtopics,
            weakAreas = weakAreas,
        )
    }
}

internal data class LearningPerformanceSnapshot(
    val topics: List<TopicPerformance>,
    val subtopics: List<SubtopicPerformance>,
    val weakAreas: List<WeakArea>,
)

private fun answeredCorrectly(questionAttempt: QuestionAttempt): Boolean =
    (questionAttempt.answerState as QuestionAnswerState.Answered).isCorrect

private data class Counts(
    var answered: Int = 0,
    var correct: Int = 0,
) {
    fun add(isCorrect: Boolean) {
        answered++
        if (isCorrect) correct++
    }
}

private data class PerformanceSubtopicKey(
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
