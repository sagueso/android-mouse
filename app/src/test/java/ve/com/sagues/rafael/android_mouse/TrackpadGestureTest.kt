package ve.com.sagues.rafael.android_mouse

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackpadGestureTest {

    private val touchSlop = 8f

    @Test
    fun parallelTwoFingerDragIsScroll() {
        assertEquals(
            TwoFingerIntent.SCROLL,
            classifyTwoFingerIntent(zoomChange = 0.03f, panDistance = 60f, touchSlop = touchSlop)
        )
    }

    @Test
    fun spanChangeAboveThresholdIsZoom() {
        assertEquals(
            TwoFingerIntent.ZOOM,
            classifyTwoFingerIntent(zoomChange = 0.20f, panDistance = 10f, touchSlop = touchSlop)
        )
    }

    @Test
    fun pinchWithPanIsStillZoom() {
        assertEquals(
            TwoFingerIntent.ZOOM,
            classifyTwoFingerIntent(zoomChange = 0.15f, panDistance = 200f, touchSlop = touchSlop)
        )
    }

    @Test
    fun smallInitialMovementStaysUndecided() {
        assertEquals(
            TwoFingerIntent.NONE,
            classifyTwoFingerIntent(zoomChange = 0.02f, panDistance = 12f, touchSlop = touchSlop)
        )
    }

    @Test
    fun zoomJitterBelowThresholdDoesNotZoomDuringScroll() {
        assertEquals(
            TwoFingerIntent.SCROLL,
            classifyTwoFingerIntent(zoomChange = 0.09f, panDistance = 80f, touchSlop = touchSlop)
        )
    }

    @Test
    fun pinchingInwardIsZoom() {
        assertEquals(
            TwoFingerIntent.ZOOM,
            classifyTwoFingerIntent(zoomChange = -0.18f, panDistance = 5f, touchSlop = touchSlop)
        )
    }
}
