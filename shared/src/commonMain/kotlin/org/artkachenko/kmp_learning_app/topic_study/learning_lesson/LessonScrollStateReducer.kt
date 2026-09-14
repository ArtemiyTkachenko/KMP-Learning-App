package org.artkachenko.kmp_learning_app.topic_study.learning_lesson

internal data class LessonScrollUiState(
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

    init {
        require(directionThresholdPx > 0) { "directionThresholdPx must be positive." }
    }

    fun update(
        position: Int,
        isScrollInProgress: Boolean,
        isAtTop: Boolean,
        isAtBottom: Boolean,
    ): LessonScrollUiState {
        val delta = lastPosition?.let { position - it } ?: 0
        lastPosition = position

        if (isAtTop) {
            showsToolbarSubtitle = true
            showsBottomNavigation = true
            accumulatedDistance = 0
        } else if (isAtBottom) {
            showsBottomNavigation = true
            accumulatedDistance = 0
        } else if (isScrollInProgress) {
            accumulate(delta)
        } else {
            // A remeasurement can move or clamp the position without a gesture or animation.
            // Keep it as the next comparison baseline, but never count it as reading direction.
            accumulatedDistance = 0
        }

        return LessonScrollUiState(
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
    showsToolbarSubtitle = true,
    showsBottomNavigation = true,
)
