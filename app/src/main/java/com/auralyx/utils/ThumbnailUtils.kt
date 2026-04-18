package com.auralyx.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Handles .aD17 file operations:
 *  - resolveAD17Path  : symlink/copy .aD17 → temp .mp4 so ExoPlayer can play it
 *  - getAD17Thumbnail : extract a video frame and cache as PNG on disk
 *  - getVideoThumbnail: generic frame extraction from any video path
 */
object ThumbnailUtils {

    private const val THUMB_DIR  = "ad17_thumbs"
    private const val THUMB_SIZE = 512

    /**
     * Returns a cached Bitmap for an .aD17 (or any video) file.
     * Reads from a disk cache keyed on filename + lastModified,
     * so extraction only happens once per file.
     */
    suspend fun getAD17Thumbnail(context: Context, originalPath: String): Bitmap? =
        withContext(Dispatchers.IO) {
            val src = File(originalPath)
            if (!src.exists()) return@withContext null

            val cacheKey = "${src.nameWithoutExtension}_${src.lastModified()}.png"
            val thumbDir = File(context.cacheDir, THUMB_DIR).also { it.mkdirs() }
            val cacheFile = File(thumbDir, cacheKey)

            // Return from disk cache if available
            if (cacheFile.exists()) {
                return@withContext BitmapFactory.decodeFile(cacheFile.absolutePath)
            }

            // Extract the first non-black frame
            val bmp = extractFrame(originalPath) ?: return@withContext null

            // Scale down to keep memory sane
            val scaled = Bitmap.createScaledBitmap(
                bmp,
                THUMB_SIZE,
                (THUMB_SIZE * bmp.height / bmp.width.toFloat()).toInt(),
                true
            )
            bmp.recycle()

            // Persist to disk cache
            try {
                FileOutputStream(cacheFile).use { out ->
                    scaled.compress(Bitmap.CompressFormat.PNG, 90, out)
                }
            } catch (_: Exception) {}

            scaled
        }

    /** Raw frame extraction — tries 1 s, then 0 s as fallback. */
    private fun extractFrame(path: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Exception) {
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    /** Legacy helper kept for compatibility. Delegates to getAD17Thumbnail. */
    suspend fun getVideoThumbnail(context: Context, path: String): Bitmap? =
        getAD17Thumbnail(context, path)

    /**
     * Copies .aD17 → temp .mp4 in cache so ExoPlayer plays it natively.
     * Re-uses existing copy when source hasn't changed (mtime check).
     */
    suspend fun resolveAD17Path(context: Context, originalPath: String): String =
        withContext(Dispatchers.IO) {
            val src = File(originalPath)
            if (!src.exists()) return@withContext originalPath

            val dest = File(context.cacheDir, "ad17_${src.nameWithoutExtension}.mp4")
            if (dest.exists() && dest.lastModified() >= src.lastModified()) {
                return@withContext dest.absolutePath
            }
            src.copyTo(dest, overwrite = true)
            dest.absolutePath
        }
}
