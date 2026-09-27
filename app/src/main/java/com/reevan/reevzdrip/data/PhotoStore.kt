package com.reevan.reevzdrip.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Directory, under `filesDir`, holding every garment photo. */
private const val PHOTO_DIR = "garments"

/** Scratch directory, under `cacheDir`, that the camera writes its full-size original into. */
private const val CAMERA_DIR = "camera"

private const val JPEG_QUALITY = 85

private const val TAG = "PhotoStore"

/** The directory garment photos live in, created if it is not there yet. */
fun garmentPhotoDir(context: Context): File =
    File(context.filesDir, PHOTO_DIR).apply { mkdirs() }

/**
 * The file backing [photoName].
 *
 * Resolved from the filename at read time rather than stored as a path, because Android relocates
 * an app's data directory on restore-to-a-new-device and a stored absolute path would not survive
 * it. The file is **not** guaranteed to exist — see [Garment.photoName].
 */
fun garmentPhotoFile(context: Context, photoName: String): File =
    File(garmentPhotoDir(context), photoName)

/**
 * Owns the bytes behind [Garment.photoName]: importing a picked or captured image into app-private
 * storage, and deleting it again.
 *
 * Imported images are **re-encoded, not copied**. A 12 MP phone camera JPEG is 4–12 MB, and a
 * wardrobe of a couple of hundred garments at that size is multiple gigabytes on a device with no
 * cloud backup. Re-encoding to a [MAX_PHOTO_EDGE] long edge costs nothing that matters at the size
 * a garment is ever displayed, and it is the difference between the app being free and being
 * expensive to keep installed.
 *
 * Re-encoding does mean EXIF is lost, so the orientation has to be baked into the pixels here —
 * see [PhotoOrientation].
 */
class PhotoStore(private val context: Context) {

    /**
     * Copies the image at [source] into app-private storage, downscaled and turned the right way
     * up, and returns its filename. Returns null if the image could not be read or decoded.
     *
     * Failure is reported as null rather than thrown: a photo is optional on a garment, so an
     * unreadable image should cost the user the photo, never the garment they were saving.
     */
    suspend fun import(source: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val bounds = readBounds(source) ?: return@withContext null
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

            val decoded = decodeDownsampled(source, bounds) ?: return@withContext null
            val corrected = correct(decoded, readOrientation(source))

            val name = "${UUID.randomUUID()}.jpg"
            val file = File(garmentPhotoDir(context), name)
            val written = try {
                file.outputStream().use { out ->
                    corrected.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                }
            } catch (e: IOException) {
                Log.w(TAG, "Could not write $name", e)
                false
            } finally {
                corrected.recycle()
            }

            if (written) {
                name
            } else {
                // A half-written file is worse than none: it would load as a corrupt image on
                // every future render rather than falling back to the placeholder.
                file.delete()
                null
            }
        } catch (e: IOException) {
            Log.w(TAG, "Could not import $source", e)
            null
        } catch (e: SecurityException) {
            // The picker grants read access for this process only; a Uri that outlived that grant
            // (restored from saved state after a process death, say) lands here.
            Log.w(TAG, "No longer permitted to read $source", e)
            null
        }
    }

    /**
     * Deletes the file behind [photoName]. A null name, or a file that is already gone, is a
     * no-op — both mean "there is no photo here", which is the state being asked for.
     */
    suspend fun delete(photoName: String?) {
        if (photoName == null) return
        withContext(Dispatchers.IO) {
            runCatching { garmentPhotoFile(context, photoName).delete() }
                .onFailure { Log.w(TAG, "Could not delete $photoName", it) }
        }
    }

    /**
     * A fresh `content://` Uri for the camera to write a full-size capture into.
     *
     * Lives in `cacheDir`, not `filesDir`: it is the untouched original, and once [import] has
     * produced the downscaled copy it is rubbish the system may reclaim whenever it likes.
     */
    fun newCameraTarget(): Uri {
        val dir = File(context.cacheDir, CAMERA_DIR).apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** Clears the camera scratch directory. Safe to call at any time. */
    suspend fun clearCameraCache() {
        withContext(Dispatchers.IO) {
            runCatching { File(context.cacheDir, CAMERA_DIR).listFiles()?.forEach { it.delete() } }
        }
    }

    private fun readBounds(source: Uri): BitmapFactory.Options? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        return context.contentResolver.openInputStream(source)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
            options
        }
    }

    private fun decodeDownsampled(source: Uri, bounds: BitmapFactory.Options): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        return context.contentResolver.openInputStream(source)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    private fun readOrientation(source: Uri): PhotoOrientation =
        try {
            context.contentResolver.openInputStream(source)?.use { stream ->
                val tag = ExifInterface(stream)
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                orientationOf(tag)
            } ?: PhotoOrientation(0, flipHorizontal = false)
        } catch (e: IOException) {
            // No EXIF at all is normal — a PNG or a screenshot has none. Upright is the right guess.
            Log.d(TAG, "No EXIF orientation on $source", e)
            PhotoOrientation(0, flipHorizontal = false)
        }

    /**
     * Scales [bitmap] to [MAX_PHOTO_EDGE] and applies [orientation], in one matrix pass so there
     * is never a second full-size intermediate in memory.
     */
    private fun correct(bitmap: Bitmap, orientation: PhotoOrientation): Bitmap {
        val target = scaledSize(bitmap.width, bitmap.height)
        val needsScale = target.width != bitmap.width || target.height != bitmap.height
        if (!needsScale && orientation.isIdentity) return bitmap

        val matrix = Matrix()
        if (needsScale) {
            matrix.postScale(
                target.width.toFloat() / bitmap.width,
                target.height.toFloat() / bitmap.height,
            )
        }
        if (orientation.flipHorizontal) matrix.postScale(-1f, 1f)
        if (orientation.rotationDegrees != 0) {
            matrix.postRotate(orientation.rotationDegrees.toFloat())
        }

        return try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                .also { if (it !== bitmap) bitmap.recycle() }
        } catch (e: OutOfMemoryError) {
            // Better a correctly-stored but oversized photo than a crash mid-save.
            Log.w(TAG, "Out of memory transforming photo; storing as decoded", e)
            bitmap
        }
    }
}
