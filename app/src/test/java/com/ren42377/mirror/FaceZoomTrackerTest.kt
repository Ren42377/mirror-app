package com.ren42377.mirror

import com.ren42377.mirror.camera.FaceZoomTracker
import com.ren42377.mirror.camera.sensorPointToDisplayNormalized
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceZoomTrackerTest {

    private fun newTracker(canPan: Boolean = true): FaceZoomTracker {
        return FaceZoomTracker().apply {
            configure(0, 0, 4000, 3000, 4f, canPan)
        }
    }

    private fun runSteps(tracker: FaceZoomTracker, startMs: Long, seconds: Float, face: Triple<Float, Float, Float>?) {
        var now = startMs
        val end = startMs + (seconds * 1000).toLong()
        while (now < end) {
            if (face != null) tracker.onFace(face.first, face.second, face.third, now)
            tracker.step(now)
            now += 33
        }
    }

    @Test
    fun unconfiguredTrackerReturnsNoWindow() {
        assertNull(FaceZoomTracker().step(0L))
    }

    @Test
    fun startsWithFullFrame() {
        val window = newTracker().step(0L)
        assertNotNull(window)
        assertEquals(0, window!!.left)
        assertEquals(4000, window.right)
        assertEquals(3000, window.bottom)
        assertEquals(1f, window.zoom, 0.001f)
        assertFalse(window.faceVisible)
    }

    @Test
    fun smallFaceZoomsInAndPansTowardFace() {
        val tracker = newTracker()
        runSteps(tracker, 1000L, 3f, Triple(1000f, 1000f, 400f))
        val window = tracker.step(4100L)!!
        assertTrue(window.faceVisible)
        assertTrue("zoom was ${window.zoom}", window.zoom > 2f)
        val centerX = (window.left + window.right) / 2f
        val centerY = (window.top + window.bottom) / 2f
        assertTrue(centerX < 1600f)
        assertTrue(centerY < 1300f)
        assertTrue(window.left >= 0 && window.top >= 0)
        assertTrue(window.right <= 4000 && window.bottom <= 3000)
    }

    @Test
    fun zoomIsCappedByMaxZoom() {
        val tracker = FaceZoomTracker().apply { configure(0, 0, 4000, 3000, 2f, true) }
        runSteps(tracker, 1000L, 4f, Triple(2000f, 1500f, 100f))
        val window = tracker.step(5100L)!!
        assertTrue(window.zoom <= 2.001f)
    }

    @Test
    fun largeFaceDoesNotZoom() {
        val tracker = newTracker()
        runSteps(tracker, 1000L, 2f, Triple(2000f, 1500f, 2800f))
        assertEquals(1f, tracker.step(3100L)!!.zoom, 0.01f)
    }

    @Test
    fun zoomReturnsToFullFrameAfterFaceIsLost() {
        val tracker = newTracker()
        runSteps(tracker, 1000L, 3f, Triple(2000f, 1500f, 500f))
        runSteps(tracker, 4000L, 5f, null)
        val window = tracker.step(9100L)!!
        assertFalse(window.faceVisible)
        assertEquals(1f, window.zoom, 0.001f)
        assertEquals(0, window.left)
        assertEquals(4000, window.right)
    }

    @Test
    fun centerOnlyCamerasKeepWindowCentered() {
        val tracker = newTracker(canPan = false)
        runSteps(tracker, 1000L, 3f, Triple(900f, 800f, 400f))
        val window = tracker.step(4100L)!!
        assertTrue(window.zoom > 1.5f)
        assertEquals((4000 - window.right).toFloat(), window.left.toFloat(), 1f)
        assertEquals((3000 - window.bottom).toFloat(), window.top.toFloat(), 1f)
    }

    @Test
    fun sensorPointMapsForPortraitFrontCamera() {
        val point = sensorPointToDisplayNormalized(1000f, 750f, 0, 0, 4000, 3000, 270)
        assertEquals(0.25f, point.x, 0.001f)
        assertEquals(0.75f, point.y, 0.001f)
    }

    @Test
    fun sensorPointMapsForPortraitBackCamera() {
        val point = sensorPointToDisplayNormalized(1000f, 750f, 0, 0, 4000, 3000, 90)
        assertEquals(0.75f, point.x, 0.001f)
        assertEquals(0.25f, point.y, 0.001f)
    }
}
