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

private enum class TrackpadMode { UNDECIDED, MOVE, SCROLL, SWIPE, DRAG }

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

            if (mode == TrackpadMode.UNDECIDED && totalPan.getDistance() > viewConfiguration.touchSlop) {
                mode = when {
                    secondTapCandidate && maxPointers == 1 -> TrackpadMode.DRAG
                    maxPointers >= 3 -> TrackpadMode.SWIPE
                    maxPointers == 2 -> TrackpadMode.SCROLL
                    else -> TrackpadMode.MOVE
                }
                if (mode == TrackpadMode.DRAG) {
                    dragStarted = true
                    onDragStart()
                }
            }

            when (mode) {
                TrackpadMode.MOVE, TrackpadMode.DRAG -> if (pan != Offset.Zero) onMove(pan.x, pan.y)
                TrackpadMode.SCROLL ->
                    if (abs(zoomAccum - 1f) > 0.08f) onZoom(zoom) else onScroll(pan.x, pan.y)
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
            else -> Unit // MOVE / SCROLL need no discrete action
        }
    }
}

private fun Offset.dominantDirection(): SwipeDirection =
    if (abs(x) > abs(y)) {
        if (x > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
    } else {
        if (y > 0) SwipeDirection.DOWN else SwipeDirection.UP
    }
