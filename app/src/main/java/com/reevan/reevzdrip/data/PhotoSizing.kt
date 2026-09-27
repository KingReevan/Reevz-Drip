package com.reevan.reevzdrip.data

import kotlin.math.roundToInt

/**
 * The arithmetic behind photo import, kept as pure functions so it can be unit-tested on the host.
 *
 * Everything that touches [android.graphics.Bitmap], the [android.content.ContentResolver] or the
 * filesystem lives in [PhotoStore]; everything that decides *what size* and *which way up* lives
 * here. Getting the sizing wrong is the difference between a wardrobe that fits on the phone and
 * one that does not, and it is invisible until the storage fills up.
 */

/** Long edge, in pixels, that an imported garment photo is reduced to. */
const val MAX_PHOTO_EDGE = 1080

/**
 * The largest power-of-two `inSampleSize` that still leaves the image at or above [maxEdge] on its
 * long side.
 *
 * Two-step downscaling is deliberate: `inSampleSize` is cheap but only halves, so it gets the
 * bitmap into roughly the right range without ever allocating the full-size original — a 12 MP
 * camera JPEG is ~48 MB as an ARGB_8888 bitmap, which is enough to kill the app on its own. The
 * precise scale to [maxEdge] then happens from the already-small bitmap, via [scaledSize].
 *
 * Deliberately stops *above* [maxEdge] rather than below it: sampling one step further would
 * undershoot and then the precise step would have to scale back up, which only throws away detail.
 */
fun sampleSizeFor(width: Int, height: Int, maxEdge: Int = MAX_PHOTO_EDGE): Int {
    if (width <= 0 || height <= 0 || maxEdge <= 0) return 1
    var sample = 1
    var longEdge = maxOf(width, height)
    while (longEdge / 2 >= maxEdge) {
        longEdge /= 2
        sample *= 2
    }
    return sample
}

/** A pixel size. */
data class PhotoSize(val width: Int, val height: Int)

/**
 * [width] x [height] scaled so its long edge is exactly [maxEdge], preserving aspect ratio.
 *
 * An image already within [maxEdge] is returned untouched — upscaling a small photo would cost
 * storage and gain nothing.
 */
fun scaledSize(width: Int, height: Int, maxEdge: Int = MAX_PHOTO_EDGE): PhotoSize {
    if (width <= 0 || height <= 0 || maxEdge <= 0) return PhotoSize(width, height)
    val longEdge = maxOf(width, height)
    if (longEdge <= maxEdge) return PhotoSize(width, height)

    val scale = maxEdge.toDouble() / longEdge
    return PhotoSize(
        width = (width * scale).roundToInt().coerceAtLeast(1),
        height = (height * scale).roundToInt().coerceAtLeast(1),
    )
}

/**
 * How an image has to be transformed to appear the right way up.
 *
 * Camera JPEGs are almost always stored in the sensor's own orientation with an EXIF tag saying
 * how to turn them. Re-encoding through [android.graphics.Bitmap.compress] drops EXIF entirely, so
 * the rotation has to be **baked into the pixels** at import — otherwise every photo taken in
 * portrait comes back sideways and there is no tag left to explain why.
 */
data class PhotoOrientation(val rotationDegrees: Int, val flipHorizontal: Boolean) {
    val isIdentity: Boolean get() = rotationDegrees == 0 && !flipHorizontal
}

// The EXIF orientation constants, repeated rather than imported so this file stays host-testable.
private const val EXIF_NORMAL = 1
private const val EXIF_FLIP_HORIZONTAL = 2
private const val EXIF_ROTATE_180 = 3
private const val EXIF_FLIP_VERTICAL = 4
private const val EXIF_TRANSPOSE = 5
private const val EXIF_ROTATE_90 = 6
private const val EXIF_TRANSVERSE = 7
private const val EXIF_ROTATE_270 = 8

/**
 * Maps an EXIF orientation tag to the transform that corrects it.
 *
 * All eight values are handled, including the four mirrored ones. They are rare from a camera but
 * turn up in images that have been through a front-facing lens or an editor, and an unhandled
 * value silently produces a mirrored garment — which looks almost right, which is worse than
 * looking obviously wrong. An unknown value is treated as [EXIF_NORMAL].
 */
fun orientationOf(exifOrientation: Int): PhotoOrientation = when (exifOrientation) {
    EXIF_FLIP_HORIZONTAL -> PhotoOrientation(0, flipHorizontal = true)
    EXIF_ROTATE_180 -> PhotoOrientation(180, flipHorizontal = false)
    EXIF_FLIP_VERTICAL -> PhotoOrientation(180, flipHorizontal = true)
    EXIF_TRANSPOSE -> PhotoOrientation(90, flipHorizontal = true)
    EXIF_ROTATE_90 -> PhotoOrientation(90, flipHorizontal = false)
    EXIF_TRANSVERSE -> PhotoOrientation(270, flipHorizontal = true)
    EXIF_ROTATE_270 -> PhotoOrientation(270, flipHorizontal = false)
    else -> PhotoOrientation(0, flipHorizontal = false)
}
