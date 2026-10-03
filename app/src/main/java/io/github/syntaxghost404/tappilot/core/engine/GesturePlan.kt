package io.github.syntaxghost404.tappilot.core.engine

/** A single gesture ready to be performed, in absolute screen pixels. */
sealed interface GesturePlan {
    val durationMs: Long
}

data class TapPlan(val x: Float, val y: Float, override val durationMs: Long) : GesturePlan

data class SwipePlan(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    override val durationMs: Long,
) : GesturePlan

enum class DispatchResult {
    /** The system performed the whole gesture. */
    Completed,

    /** The gesture started but was interrupted, for example by a real touch. */
    Cancelled,

    /** The system refused the gesture, usually because the service is no longer connected. */
    Rejected,
}

/** Performs gestures. Suspends until the system reports the outcome. */
fun interface GestureDispatcher {
    suspend fun dispatch(plan: GesturePlan): DispatchResult
}
