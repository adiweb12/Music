package com.auralyx.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Extracts video frames as thumbnails for .aD17 / MP4 files.
 * Caches results to disk to avoid repeated heavy I/O.
 */
object ThumbnailUtils {

    suspend fun getVideoThumbnail(context: Context, path: String): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(path)
                val bmp = retriever.getFrameAtTime(
                    1_000_000L, // 1 second in
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                retriever.release()
                bmp
            } catch (e: Exception) {
                null
            }
        }

    /**
     * For .aD17 files: copies the file to a temp .mp4 in cache so
     * ExoPlayer and MediaMetadataRetriever can handle it natively.
     */
    suspend fun resolveAD17Path(context: Context, originalPath: String): String =
        withContext(Dispatchers.IO) {
            val src = File(originalPath)
            if (!src.exists()) return@withContext originalPath

            val cacheDir = context.cacheDir
            val dest = File(cacheDir, "ad17_temp_${src.nameWithoutExtension}.mp4")

            // Reuse cache if up to date
            if (dest.exists() && dest.lastModified() >= src.lastModified()) {
                return@withContext dest.absolutePath
            }

            // Copy bytes (symlink not reliable across volumes)
            src.copyTo(dest, overwrite = true)
            dest.absolutePath
        }
}
