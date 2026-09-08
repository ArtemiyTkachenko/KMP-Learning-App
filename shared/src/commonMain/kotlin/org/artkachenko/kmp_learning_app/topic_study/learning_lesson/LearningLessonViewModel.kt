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
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressState
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressStateHolder
import org.artkachenko.kmp_learning_app.lesson_study.toUiState

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
 *
 * Study state arrives from the app-scoped [studyProgressStateHolder] rather than from a read of
 * this Lesson's own record. That is what keeps the Learn stack consistent: the Unit overview and
 * Topic Detail underneath are still alive and observing the same projection, so a mark made here
 * reaches them without either being recreated. Opening this Lesson performs a read and never a
 * write — nothing about arriving at, scrolling, or leaving a Lesson persists a study fact.
 */
internal class LearningLessonViewModel(
    private val unitId: String,
    private val lessonId: String,
    private val learningContentRepository: LearningContentRepository,
    private val studyProgressStateHolder: StudyProgressStateHolder,
) : ViewModel() {
    private val _uiState = MutableStateFlow<LearningLessonUiState>(LearningLessonUiState.Loading)
    val uiState: StateFlow<LearningLessonUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    /** The document half, held so a study-state emission can re-render without reloading it. */
    private var content: LearningLessonUiState = LearningLessonUiState.Loading
    private var studyState: StudyProgressState = StudyProgressState.Loading

    init {
        require(unitId.isNotBlank()) { "unitId must not be blank." }
        require(lessonId.isNotBlank()) { "lessonId must not be blank." }
        observeStudyState()
        load()
    }

    fun retry() {
        // The document is what failed and what Retry means here. Study state is re-read too, since
        // an unavailable indicator is the other thing a learner on this page might be retrying.
        studyProgressStateHolder.refresh()
        load()
    }

    /**
     * Marks this Lesson studied, or unmarks it, through the shared holder.
     *
     * The holder persists first and republishes what it reads back, so nothing here flips the
     * visible value optimistically; it also ignores a toggle while study state is unknown or while
     * this Lesson's own write is in flight. Only the Lesson currently on screen can be toggled —
     * [lessonId] comes from the route, never from a control's payload.
     */
    fun toggleStudied() {
        studyProgressStateHolder.toggleStudied(lessonId)
    }

    private fun observeStudyState() {
        studyProgressStateHolder.refresh()
        viewModelScope.launch {
            studyProgressStateHolder.state.collect { state ->
                studyState = state
                render()
            }
        }
    }

    private fun load() {
        loadJob?.cancel()
        content = LearningLessonUiState.Loading
        render()
        loadJob = viewModelScope.launch {
            content = try {
                loadState()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                LearningLessonUiState.Error
            }
            render()
        }
    }

    /**
     * The document decides which page this is; study state only decorates the one page that has a
     * Lesson on it. A study-record failure therefore cannot produce Loading, NotFound, or Error.
     */
    private fun render() {
        _uiState.value = when (val content = content) {
            is LearningLessonUiState.Content -> content.copy(
                studyState = studyState.toUiState { loaded ->
                    LessonStudyUiModel(
                        isStudied = lessonId in loaded.studiedLessonIds,
                        isPending = lessonId in loaded.pendingLessonIds,
                    )
                },
            )
            else -> content
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
