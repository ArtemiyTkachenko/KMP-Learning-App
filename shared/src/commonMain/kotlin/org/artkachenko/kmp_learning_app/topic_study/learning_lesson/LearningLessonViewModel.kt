package org.artkachenko.kmp_learning_app.topic_study.learning_lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningLesson
import org.artkachenko.kmp_learning_app.curriculum.learning.repository.LearningContentRepository

/**
 * One Lesson, resolved through the Unit the learner opened it from.
 *
 * The Lesson is found by walking the Unit's authored Lessons rather than by calling the global
 * `getLessonById`. That is the whole point of carrying both IDs: containment is what makes the
 * parent relationship true, so a route pairing a valid Lesson with the wrong Unit resolves to
 * nothing instead of opening another Unit's Lesson under a borrowed parent. The global lookup
 * remains the right tool for stable historical resolution elsewhere; parent-scoped navigation has
 * the stricter contract.
 *
 * All five conditions are required for normal browsing: the Unit exists, the Unit is ACTIVE, the
 * Lesson exists, the Lesson is ACTIVE, and the Lesson belongs to that Unit. Each failure is the
 * same answer — this is not current study material — so they share one state rather than being
 * distinguished in the UI, which would only leak the document's shape to the learner.
 *
 * Reading the Lesson through its Unit is also what makes previous/next derivable at all: the
 * neighbours are the Unit's other ACTIVE Lessons in authored order, which is a fact about this
 * parent and cannot be recovered from a Lesson alone.
 */
internal class LearningLessonViewModel(
    private val unitId: String,
    private val lessonId: String,
    private val learningContentRepository: LearningContentRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<LearningLessonUiState>(LearningLessonUiState.Loading)
    val uiState: StateFlow<LearningLessonUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        require(unitId.isNotBlank()) { "unitId must not be blank." }
        require(lessonId.isNotBlank()) { "lessonId must not be blank." }
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.value = LearningLessonUiState.Loading
        loadJob = viewModelScope.launch {
            try {
                _uiState.value = loadState()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                _uiState.value = LearningLessonUiState.Error
            }
        }
    }

    private suspend fun loadState(): LearningLessonUiState {
        val unit = learningContentRepository.getUnitById(unitId)
            ?.takeIf { it.status == ContentStatus.ACTIVE }
            ?: return LearningLessonUiState.NotFound

        // The reading sequence and the containment check are the same list, resolved once. A
        // deprecated Lesson is absent from it, so it can neither be opened nor be stepped through
        // on the way between two current ones — retired material must not reappear as a waypoint.
        val activeLessons = unit.lessons.filter { it.status == ContentStatus.ACTIVE }
        val index = activeLessons.indexOfFirst { it.id == lessonId }
        if (index < 0) return LearningLessonUiState.NotFound
        val lesson = activeLessons[index]

        return LearningLessonUiState.Content(
            unitId = unit.id,
            lessonId = lesson.id,
            title = lesson.title,
            summary = lesson.summary,
            sections = lesson.sections,
            sources = lesson.sources,
            previousLesson = activeLessons.getOrNull(index - 1)?.toAdjacentLesson(),
            nextLesson = activeLessons.getOrNull(index + 1)?.toAdjacentLesson(),
        )
    }
}

/**
 * Authored order is the reading order, so neither neighbour is sorted or scored: the Unit's Lessons
 * are a sequence its author wrote, and position in that list is the only thing "next" can mean.
 */
private fun LearningLesson.toAdjacentLesson(): AdjacentLessonUiModel =
    AdjacentLessonUiModel(lessonId = id, title = title)
