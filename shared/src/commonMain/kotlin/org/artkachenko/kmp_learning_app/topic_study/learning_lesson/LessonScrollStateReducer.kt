package org.artkachenko.kmp_learning_app.topic_study.learning_lesson

internal data class LessonScrollUiState(
    val isAtTop: Boolean,
    val isAtBottom: Boolean,
    val hasScrollableContent: Boolean,
    val hasContentBelow: Boolean,
    val showsToolbarSubtitle: Boolean,
    val showsBottomNavigation: Boolean,
)

/** Turns raw scroll positions into stable reading-chrome decisions. */
internal class LessonScrollStateReducer(
    private val directionThresholdPx: Int,
) {
    private var lastPosition: Int? = null
    private var accumulatedDistance = 0
    private var showsToolbarSubtitle = true
    private var showsBottomNavigation = true
    private var isEndAnchored = false

    init {
        require(directionThresholdPx > 0) { "directionThresholdPx must be positive." }
    }

    fun update(position: Int, maxPosition: Int): LessonScrollUiState {
        val isMeasured = maxPosition != Int.MAX_VALUE
        val hasScrollableContent = isMeasured && maxPosition > 0
        val isAtTop = position <= 0
        val reachedPhysicalBottom = isMeasured && position >= maxPosition
        val delta = lastPosition?.let { position - it } ?: 0
        lastPosition = position

        if (delta < 0) isEndAnchored = false
        if (reachedPhysicalBottom) isEndAnchored = true

        if (isAtTop) {
            showsToolbarSubtitle = true
            showsBottomNavigation = true
            accumulatedDistance = 0
        } else {
            accumulate(delta)
        }

        if (isEndAnchored) {
            showsBottomNavigation = true
            accumulatedDistance = 0
        }

        return LessonScrollUiState(
            isAtTop = isAtTop,
            isAtBottom = isEndAnchored,
            hasScrollableContent = hasScrollableContent,
            hasContentBelow = hasScrollableContent && position < maxPosition && !isEndAnchored,
            showsToolbarSubtitle = showsToolbarSubtitle,
            showsBottomNavigation = showsBottomNavigation,
        )
    }

    private fun accumulate(delta: Int) {
        if (delta == 0) return

        accumulatedDistance = if (
            accumulatedDistance == 0 ||
            accumulatedDistance.sign == delta.sign
        ) {
            accumulatedDistance + delta
        } else {
            delta
        }

        when {
            accumulatedDistance >= directionThresholdPx -> {
                showsToolbarSubtitle = false
                showsBottomNavigation = false
                accumulatedDistance = 0
            }
            accumulatedDistance <= -directionThresholdPx -> {
                showsToolbarSubtitle = true
                showsBottomNavigation = true
                accumulatedDistance = 0
            }
        }
    }

    private val Int.sign: Int
        get() = compareTo(0)
}

internal val InitialLessonScrollUiState = LessonScrollUiState(
    isAtTop = true,
    isAtBottom = false,
    hasScrollableContent = false,
    hasContentBelow = false,
    showsToolbarSubtitle = true,
    showsBottomNavigation = true,
)
