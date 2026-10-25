package ve.com.sagues.rafael.android_mouse

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

private const val DOUBLE_TAP_TIMEOUT_MS = 300L

// A two-finger gesture only becomes a pinch after the finger span changes this much.
// Parallel scrolling rarely changes the span by more than a few percent.
private const val ZOOM_INTENT_THRESHOLD = 0.12f

// Scrolling is not locked in at the first touch-slop crossing: a pinch often starts
// with a small pan, so two-finger pans must travel farther before being treated as scroll.
private const val TWO_FINGER_SCROLL_SLOP_MULTIPLIER = 2.5f

private enum class TrackpadMode { UNDECIDED, MOVE, SCROLL, ZOOM, SWIPE, DRAG }

internal enum class TwoFingerIntent { NONE, ZOOM, SCROLL }

/**
 * Decides what a two-finger gesture is trying to do.
 *
 * A pinch is only recognized when the finger span changes by more than
 * [ZOOM_INTENT_THRESHOLD], so ordinary scrolling (which keeps the span almost
 * constant) cannot be mistaken for a zoom. Scrolling also waits until the pan
 * passes a higher threshold, because pinches usually start with a small pan.
 */
internal fun classifyTwoFingerIntent(
    zoomChange: Float,
    panDistance: Float,
    touchSlop: Float
): TwoFingerIntent = when {
    abs(zoomChange) > ZOOM_INTENT_THRESHOLD -> TwoFingerIntent.ZOOM
    panDistance > touchSlop * TWO_FINGER_SCROLL_SLOP_MULTIPLIER -> TwoFingerIntent.SCROLL
    else -> TwoFingerIntent.NONE
}

/**
 * Multi-touch trackpad recognizer:
 *  - 1 finger: move cursor; tap = left click; double tap = double click;
 *    double-tap-and-drag = press-hold drag.
 *  - 2 fingers: tap = right click; drag = scroll; pinch = zoom.
 *  - 3 fingers: tap / swipe (up, down, left, right).
 *  - 4 fingers: tap / swipe (up, down, left, right).
 */
fun Modifier.trackpadGestures(
    onMove: (Float, Float) -> Unit,
    onScroll: (Float, Float) -> Unit,
    onZoom: (Float) -> Unit,
    onLeftClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onRightClick: () -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onThreeFingerSwipe: (SwipeDirection) -> Unit,
    onThreeFingerTap: () -> Unit,
    onFourFingerSwipe: (SwipeDirection) -> Unit,
    onFourFingerTap: () -> Unit,
): Modifier = pointerInput(Unit) {
    var lastTapTime = 0L

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        down.consume()

        val startTime = down.uptimeMillis
        val secondTapCandidate = startTime - lastTapTime in 1..DOUBLE_TAP_TIMEOUT_MS

        var maxPointers = 1
        var mode = TrackpadMode.UNDECIDED
        var totalPan = Offset.Zero
        var zoomAccum = 1f
        var dragStarted = false

        while (true) {
            val event = awaitPointerEvent()
            val pressed = event.changes.count { it.pressed }
            if (pressed > maxPointers) maxPointers = pressed

            val pan = event.calculatePan()
            val zoom = event.calculateZoom()
            totalPan += pan
            zoomAccum *= zoom

            // The event that locks the mode is already forwarded below as accumulated
            // movement, so it must not be forwarded a second time.
            var panConsumed = false
            var zoomConsumed = false

            if (mode == TrackpadMode.UNDECIDED) {
                val slop = viewConfiguration.touchSlop
                val panDistance = totalPan.getDistance()
                val zoomChange = abs(zoomAccum - 1f)

                when {
                    maxPointers >= 3 && panDistance > slop ->
                        mode = TrackpadMode.SWIPE

                    maxPointers == 2 -> when (
                        classifyTwoFingerIntent(zoomChange, panDistance, slop)
                    ) {
                        TwoFingerIntent.ZOOM -> {
                            mode = TrackpadMode.ZOOM
                            onZoom(zoomAccum)
                            zoomAccum = 1f
                            zoomConsumed = true
                        }

                        TwoFingerIntent.SCROLL -> {
                            mode = TrackpadMode.SCROLL
                            if (totalPan != Offset.Zero) {
                                onScroll(totalPan.x, totalPan.y)
                                panConsumed = true
                            }
                        }

                        TwoFingerIntent.NONE -> Unit
                    }

                    maxPointers == 1 && panDistance > slop -> {
                        mode = if (secondTapCandidate) TrackpadMode.DRAG else TrackpadMode.MOVE
                        if (mode == TrackpadMode.DRAG) {
                            dragStarted = true
                            onDragStart()
                        }
                    }
                }
            }

            when (mode) {
                TrackpadMode.MOVE, TrackpadMode.DRAG ->
                    if (!panConsumed && pan != Offset.Zero) onMove(pan.x, pan.y)

                TrackpadMode.SCROLL ->
                    if (!panConsumed && pan != Offset.Zero) onScroll(pan.x, pan.y)

                // Once a pinch is recognized it stays a pinch for the whole gesture.
                TrackpadMode.ZOOM ->
                    if (!zoomConsumed && abs(zoom - 1f) > 0.002f) onZoom(zoom)

                else -> Unit // SWIPE: accumulated, resolved on release
            }

            event.changes.forEach { it.consume() }
            if (event.changes.none { it.pressed }) break
        }

        when (mode) {
            TrackpadMode.UNDECIDED -> when (maxPointers) {
                1 -> {
                    if (secondTapCandidate) {
                        onDoubleClick()
                        lastTapTime = 0L
                    } else {
                        onLeftClick()
                        lastTapTime = startTime
                    }
                }
                2 -> onRightClick()
                3 -> onThreeFingerTap()
                else -> onFourFingerTap()
            }
            TrackpadMode.DRAG -> {
                if (dragStarted) onDragEnd()
                lastTapTime = 0L
            }
            TrackpadMode.SWIPE -> {
                val direction = totalPan.dominantDirection()
                if (maxPointers >= 4) onFourFingerSwipe(direction) else onThreeFingerSwipe(direction)
            }
            else -> Unit // MOVE / SCROLL / ZOOM need no discrete action
        }
    }
}

private fun Offset.dominantDirection(): SwipeDirection =
    if (abs(x) > abs(y)) {
        if (x > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
    } else {
        if (y > 0) SwipeDirection.DOWN else SwipeDirection.UP
    }
