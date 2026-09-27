package com.reevan.reevzdrip

import com.reevan.reevzdrip.data.MAX_PHOTO_EDGE
import com.reevan.reevzdrip.data.orientationOf
import com.reevan.reevzdrip.data.sampleSizeFor
import com.reevan.reevzdrip.data.scaledSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The arithmetic that decides how large a stored garment photo is, and which way up.
 *
 * Worth testing because both failures are quiet: an off-by-one in the sample size wastes storage
 * without ever looking wrong, and a missed EXIF case produces a garment that is mirrored — which
 * looks almost right, and so goes unnoticed.
 */
class PhotoSizingTest {

    // --- sampleSizeFor -------------------------------------------------------------------

    @Test
    fun `an image already within the limit is not sampled`() {
        assertEquals(1, sampleSizeFor(800, 600, maxEdge = 1080))
        assertEquals(1, sampleSizeFor(1080, 1080, maxEdge = 1080))
    }

    @Test
    fun `sampling halves until one more step would undershoot`() {
        // 4000 -> 2000 is still >= 1080, so sample 2. 1000 would be under, so it stops there.
        assertEquals(2, sampleSizeFor(4000, 3000, maxEdge = 1080))
        // 8000 -> 4000 -> 2000, all >= 1080; 1000 would undershoot.
        assertEquals(4, sampleSizeFor(8000, 6000, maxEdge = 1080))
    }

    @Test
    fun `sampling never undershoots the target`() {
        // The whole point: the sampled bitmap must stay at or above maxEdge so the precise scale
        // afterwards is only ever scaling down, never back up.
        val widths = listOf(1081, 1500, 2160, 2161, 4032, 6000, 12000)
        widths.forEach { width ->
            val sample = sampleSizeFor(width, width / 2, maxEdge = 1080)
            assertTrue(
                "width $width sampled by $sample fell below the target",
                width / sample >= 1080,
            )
        }
    }

    @Test
    fun `the long edge decides, whichever way round the photo is`() {
        // Portrait and landscape of the same pixels must sample identically.
        assertEquals(sampleSizeFor(4000, 3000), sampleSizeFor(3000, 4000))
    }

    @Test
    fun `degenerate sizes fall back to no sampling rather than dividing by zero`() {
        assertEquals(1, sampleSizeFor(0, 0))
        assertEquals(1, sampleSizeFor(-10, 100))
        assertEquals(1, sampleSizeFor(100, 100, maxEdge = 0))
    }

    // --- scaledSize ----------------------------------------------------------------------

    @Test
    fun `a photo within the limit is left exactly as it is`() {
        val size = scaledSize(800, 600, maxEdge = 1080)
        assertEquals(800, size.width)
        assertEquals(600, size.height)
    }

    @Test
    fun `the long edge lands exactly on the limit`() {
        assertEquals(1080, scaledSize(4000, 3000, maxEdge = 1080).width)
        assertEquals(1080, scaledSize(3000, 4000, maxEdge = 1080).height)
    }

    @Test
    fun `aspect ratio survives the scale`() {
        val size = scaledSize(4000, 3000, maxEdge = 1080)
        assertEquals(1080, size.width)
        assertEquals(810, size.height) // 4:3
        assertEquals(4000.0 / 3000.0, size.width.toDouble() / size.height, 0.01)
    }

    @Test
    fun `an extreme panorama keeps at least one pixel on its short edge`() {
        // 10000x20 scaled to a 1080 long edge rounds the short edge to 2; the guard matters for
        // anything narrower still, where the honest answer rounds to zero and Bitmap would throw.
        val size = scaledSize(20000, 5, maxEdge = 1080)
        assertTrue("short edge collapsed to ${size.height}", size.height >= 1)
    }

    @Test
    fun `the real default is the constant, not a magic number`() {
        assertEquals(MAX_PHOTO_EDGE, scaledSize(4000, 4000).width)
    }

    // --- orientationOf -------------------------------------------------------------------

    @Test
    fun `normal and undefined orientations need no transform`() {
        assertTrue(orientationOf(1).isIdentity)
        assertTrue(orientationOf(0).isIdentity)
        assertTrue(orientationOf(99).isIdentity) // unknown value, treated as upright
    }

    @Test
    fun `the three plain rotations map to their angles`() {
        assertEquals(90, orientationOf(6).rotationDegrees)
        assertEquals(180, orientationOf(3).rotationDegrees)
        assertEquals(270, orientationOf(8).rotationDegrees)
        listOf(6, 3, 8).forEach { assertTrue(!orientationOf(it).flipHorizontal) }
    }

    @Test
    fun `all four mirrored orientations are handled`() {
        // These are the ones an unhandled `else` branch silently gets wrong.
        assertEquals(orientationOf(2).rotationDegrees, 0)
        assertTrue(orientationOf(2).flipHorizontal)

        assertEquals(180, orientationOf(4).rotationDegrees)
        assertTrue(orientationOf(4).flipHorizontal)

        assertEquals(90, orientationOf(5).rotationDegrees)
        assertTrue(orientationOf(5).flipHorizontal)

        assertEquals(270, orientationOf(7).rotationDegrees)
        assertTrue(orientationOf(7).flipHorizontal)
    }

    @Test
    fun `every EXIF orientation from 1 to 8 is a known case`() {
        // 1..8 is the whole of the EXIF spec; none of them may fall through to the default and
        // quietly display wrong.
        val mirrored = setOf(2, 4, 5, 7)
        (1..8).forEach { tag ->
            val orientation = orientationOf(tag)
            assertTrue(
                "orientation $tag has an unexpected angle ${orientation.rotationDegrees}",
                orientation.rotationDegrees in setOf(0, 90, 180, 270),
            )
            assertEquals(
                "orientation $tag has the wrong mirror flag",
                tag in mirrored,
                orientation.flipHorizontal,
            )
        }
    }
}
